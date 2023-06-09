/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.physical.impl.partitionsender;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.ErrorCollector;
import org.apache.drill.common.expression.ErrorCollectorImpl;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.expr.ClassGenerator;
import org.apache.drill.exec.expr.CodeGenerator;
import org.apache.drill.exec.expr.ExpressionTreeMaterializer;
import org.apache.drill.exec.ops.AccountingDataTunnel;
import org.apache.drill.exec.ops.ExchangeFragmentContext;
import org.apache.drill.exec.ops.MetricDef;
import org.apache.drill.exec.ops.OperatorStats;
import org.apache.drill.exec.ops.RootFragmentContext;
import org.apache.drill.exec.physical.MinorFragmentEndpoint;
import org.apache.drill.exec.physical.config.HashPartitionSender;
import org.apache.drill.exec.physical.impl.BaseRootExec;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;
import org.apache.drill.exec.record.AbstractRecordBatch;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.exec.record.CloseableRecordBatch;
import org.apache.drill.exec.record.FragmentWritableBatch;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.VectorAccessibleUtilities;
import org.apache.drill.exec.record.RecordBatch.IterOutcome;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.vector.CopyUtil;

import com.carrotsearch.hppc.IntArrayList;
import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JType;

public class PartitionSenderRootExec extends BaseRootExec {
  private static final Logger logger = LoggerFactory.getLogger(PartitionSenderRootExec.class);
  private final RecordBatch incoming;
  private final HashPartitionSender operator;
  private PartitionerDecorator partitioner;

  private final ExchangeFragmentContext context;
  private final int outGoingBatchCount;
  private final HashPartitionSender popConfig;
  private final double cost;

  private final AtomicIntegerArray remainingReceivers;
  private final AtomicInteger remaingReceiverCount;
  private boolean done;
  private boolean first = true;
  private final boolean closeIncoming;

  long minReceiverRecordCount = Long.MAX_VALUE;
  long maxReceiverRecordCount = Long.MIN_VALUE;
  protected final int numberPartitions;
  protected final int actualPartitions;

  private final IntArrayList terminations = new IntArrayList();

  public enum Metric implements MetricDef {
    BATCHES_SENT,
    RECORDS_SENT,
    MIN_RECORDS,
    MAX_RECORDS,
    N_RECEIVERS,
    BYTES_SENT,
    SENDING_THREADS_COUNT,
    COST;

    @Override
    public int metricId() {
      return ordinal();
    }
  }

  public PartitionSenderRootExec(RootFragmentContext context,
                                 RecordBatch incoming,
                                 HashPartitionSender operator) throws OutOfMemoryException {
    this(context, incoming, operator, false);
  }

  public PartitionSenderRootExec(RootFragmentContext context,
                                 RecordBatch incoming,
                                 HashPartitionSender operator,
                                 boolean closeIncoming) throws OutOfMemoryException {
    super(context, context.newOperatorContext(operator, null), operator);
    this.incoming = incoming;
    this.operator = operator;
    this.closeIncoming = closeIncoming;
    this.context = context;
    outGoingBatchCount = operator.getDestinations().size();
    popConfig = operator;
    remainingReceivers = new AtomicIntegerArray(outGoingBatchCount);
    remaingReceiverCount = new AtomicInteger(outGoingBatchCount);
    stats.setLongStat(Metric.N_RECEIVERS, outGoingBatchCount);
    // Algorithm to figure out number of threads to parallelize output
    // numberOfRows/sliceTarget/numReceivers/threadfactor
    this.cost = operator.getChild().getCost().getOutputRowCount();
    final OptionManager optMgr = context.getOptions();
    long sliceTarget = optMgr.getOption(ExecConstants.SLICE_TARGET).num_val;
    int threadFactor = optMgr.getOption(PlannerSettings.PARTITION_SENDER_THREADS_FACTOR.getOptionName()).num_val.intValue();
    int tmpParts = 1;
    if ( sliceTarget != 0 && outGoingBatchCount != 0 ) {
      tmpParts = (int) Math.round((((cost / (sliceTarget*1.0)) / (outGoingBatchCount*1.0)) / (threadFactor*1.0)));
      if ( tmpParts < 1) {
        tmpParts = 1;
      }
    }
    final int imposedThreads = optMgr.getOption(PlannerSettings.PARTITION_SENDER_SET_THREADS.getOptionName()).num_val.intValue();
    if (imposedThreads > 0 ) {
      this.numberPartitions = imposedThreads;
    } else {
      this.numberPartitions = Math.min(tmpParts, optMgr.getOption(PlannerSettings.PARTITION_SENDER_MAX_THREADS.getOptionName()).num_val.intValue());
    }
    logger.info("Preliminary number of sending threads is: " + numberPartitions);
    this.actualPartitions = outGoingBatchCount > numberPartitions ? numberPartitions : outGoingBatchCount;
    this.stats.setLongStat(Metric.SENDING_THREADS_COUNT, actualPartitions);
    this.stats.setDoubleStat(Metric.COST, this.cost);
  }

  @Override
  public boolean innerNext() {
    IterOutcome out;

    if (!done) {
      out = next(incoming);
    } else {
      incoming.cancel();
      out = IterOutcome.NONE;
    }

    logger.debug("Partitioner.next(): got next record batch with status {}", out);
    if (first && out == IterOutcome.OK) {
      out = IterOutcome.OK_NEW_SCHEMA;
    }
    switch(out){
      case NONE:
        try {
          // send any pending batches
          if(partitioner != null) {
            partitioner.flushOutgoingBatches(true, false);
          } else {
            sendEmptyBatch(true);
          }
        } catch (ExecutionException e) {
          throw UserException.dataWriteError(e)
            .addContext("Error while creating partitioning sender or flushing outgoing batches")
            .build(logger);
        }
        return false;

      case OK_NEW_SCHEMA:
        try {
          // send all existing batches
          if (partitioner != null) {
            partitioner.flushOutgoingBatches(false, true);
            partitioner.clear();
          }
          try {
            stats.startSetup();
            createPartitioner();
          } finally {
            stats.stopSetup();
          }

          if (first) {
            // Send an empty batch for fast schema
            first = false;
            sendEmptyBatch(false);
          }
        } catch (ExecutionException e) {
          throw UserException.dataWriteError(e)
            .addContext("Error while flushing outgoing batches")
            .build(logger);
        }
      case OK:
        try {
          partitioner.partitionBatch(incoming);
        } catch (ExecutionException e) {
          throw UserException.dataWriteError(e)
            .addContext("Error while partitioning outgoing batches")
            .build(logger);
        }
        VectorAccessibleUtilities.clear(incoming);
        return true;
      case NOT_YET:
      default:
        throw new IllegalStateException();
    }
  }

  @VisibleForTesting
  protected void createPartitioner() {
    createClassInstances(actualPartitions);
  }

  private List<Partitioner> createClassInstances(int actualPartitions) {
    // set up partitioning function
    final LogicalExpression expr = operator.getExpr();
    final ErrorCollector collector = new ErrorCollectorImpl();
    final ClassGenerator<Partitioner> cg;

    cg = CodeGenerator.getRoot(Partitioner.TEMPLATE_DEFINITION, context.getOptions());
    cg.getCodeGenerator().plainJavaCapable(true);
    // Uncomment out this line to debug the generated code.
    // cg.getCodeGenerator().saveCodeForDebugging(true);
    ClassGenerator<Partitioner> cgInner = cg.getInnerGenerator("OutgoingRecordBatch");

    final LogicalExpression materializedExpr = ExpressionTreeMaterializer.materialize(expr, incoming, collector, context.getFunctionRegistry());
    collector.reportErrors(logger);

    // generate code to copy from an incoming value vector to the destination partition's outgoing value vector
    JExpression bucket = JExpr.direct("bucket");

    // generate evaluate expression to determine the hash
    ClassGenerator.HoldingContainer exprHolder = cg.addExpr(materializedExpr);
    cg.getEvalBlock().decl(JType.parse(cg.getModel(), "int"), "bucket", exprHolder.getValue().mod(JExpr.lit(outGoingBatchCount)));
    cg.getEvalBlock()._return(cg.getModel().ref(Math.class).staticInvoke("abs").arg(bucket));

    CopyUtil.generateCopies(cgInner, incoming, incoming.getSchema().getSelectionVectorMode() == SelectionVectorMode.FOUR_BYTE);

     // compile and setup generated code
    List<Partitioner> subPartitioners = context.getImplementationClass(cg, actualPartitions);

    final int divisor = Math.max(1, outGoingBatchCount/actualPartitions);
    final int longTail = outGoingBatchCount % actualPartitions;
    int startIndex = 0;
    int endIndex = 0;

    boolean success = false;
    try {
      for (int i = 0; i < actualPartitions; i++) {
        startIndex = endIndex;
        endIndex = (i < actualPartitions - 1) ? startIndex + divisor : outGoingBatchCount;
        if (i < longTail) {
          endIndex++;
        }
        final OperatorStats partitionStats = new OperatorStats(stats, true);
        subPartitioners.get(i).setup(context, incoming, popConfig, partitionStats, oContext,
          cgInner, startIndex, endIndex);
      }

      partitioner = new PartitionerDecorator(subPartitioners, stats, context);
      for (int index = 0; index < terminations.size(); index++) {
        partitioner.getOutgoingBatches(terminations.buffer[index]).terminate();
      }
      terminations.clear();

      success = true;
    } catch (SchemaChangeException e) {
      throw AbstractRecordBatch.schemaChangeException(e, "Partition Sender", logger);
    } finally {
      if (!success) {
        for (Partitioner p : subPartitioners) {
          p.clear();
        }
      }
    }
    return subPartitioners;
  }

  /**
   * Find min and max record count seen across the outgoing batches and put them in stats.
   */
  private void updateAggregateStats() {
    for (Partitioner part : partitioner.getPartitioners() ) {
      for (PartitionOutgoingBatch o : part.getOutgoingBatches()) {
        long totalRecords = o.getTotalRecords();
        minReceiverRecordCount = Math.min(minReceiverRecordCount, totalRecords);
        maxReceiverRecordCount = Math.max(maxReceiverRecordCount, totalRecords);
      }
    }
    stats.setLongStat(Metric.MIN_RECORDS, minReceiverRecordCount);
    stats.setLongStat(Metric.MAX_RECORDS, maxReceiverRecordCount);
  }

  @Override
  public void receivingFragmentFinished(FragmentHandle handle) {
    final int id = handle.getMinorFragmentId();
    if (remainingReceivers.compareAndSet(id, 0, 1)) {
      if (partitioner == null) {
        terminations.add(id);
      } else {
        partitioner.getOutgoingBatches(id).terminate();
      }

      int remaining = remaingReceiverCount.decrementAndGet();
      if (remaining == 0) {
        done = true;
      }
    }
  }

  @Override
  public void close() throws Exception {
    logger.debug("Partition sender stopping.");
    super.close();

    if (partitioner != null) {
      updateAggregateStats();
      partitioner.clear();
    }

    if (closeIncoming) {
      ((CloseableRecordBatch) incoming).close();
    }
  }

  private void sendEmptyBatch(boolean isLast) {
    BatchSchema schema = incoming.getSchema();
    if (schema == null) {
      // If the incoming batch has no schema (possible when there are no input records),
      // create an empty schema to avoid NPE.
      schema = BatchSchema.newBuilder().build();
    }

    FragmentHandle handle = context.getHandle();
    for (MinorFragmentEndpoint destination : popConfig.getDestinations()) {
      AccountingDataTunnel tunnel = context.getDataTunnel(destination.getEndpoint());
      FragmentWritableBatch writableBatch = FragmentWritableBatch.getEmptyBatchWithSchema(
          isLast,
          handle.getQueryId(),
          handle.getMajorFragmentId(),
          handle.getMinorFragmentId(),
          operator.getOppositeMajorFragmentId(),
          destination.getId(),
          schema);
      stats.startWait();
      try {
        tunnel.sendRecordBatch(writableBatch);
      } finally {
        stats.stopWait();
      }
    }
    stats.addLongStat(Metric.BATCHES_SENT, 1);
  }

  @VisibleForTesting
  protected PartitionerDecorator getPartitioner() {
    return partitioner;
  }
}
