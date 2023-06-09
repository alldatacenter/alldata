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

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.inject.Named;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.compile.sig.RuntimeOverridden;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.expr.ClassGenerator;
import org.apache.drill.exec.memory.BaseAllocator;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.ops.AccountingDataTunnel;
import org.apache.drill.exec.ops.ExchangeFragmentContext;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.ops.OperatorStats;
import org.apache.drill.exec.physical.MinorFragmentEndpoint;
import org.apache.drill.exec.physical.config.HashPartitionSender;
import org.apache.drill.exec.physical.impl.common.CodeGenMemberInjector;
import org.apache.drill.exec.physical.impl.partitionsender.PartitionSenderRootExec.Metric;
import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.exec.record.FragmentWritableBatch;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.TypedFieldId;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.WritableBatch;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.record.selection.SelectionVector4;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PartitionerTemplate implements Partitioner {
  static final Logger logger = LoggerFactory.getLogger(PartitionerTemplate.class);

  // Always keep the recordCount as (2^x) - 1 to better utilize the memory
  // allocation in ValueVectors
  private static final int DEFAULT_RECORD_BATCH_SIZE = (1 << 10) - 1;

  private SelectionVector2 sv2;
  private SelectionVector4 sv4;
  private RecordBatch incoming;
  private OperatorStats stats;
  protected ClassGenerator<?> cg;
  protected FragmentContext context;
  private int start;
  private int end;
  private final List<OutgoingRecordBatch> outgoingBatches = Lists.newArrayList();

  private int outgoingRecordBatchSize = DEFAULT_RECORD_BATCH_SIZE;

  @Override
  public List<? extends PartitionOutgoingBatch> getOutgoingBatches() {
    return outgoingBatches;
  }

  @Override
  public PartitionOutgoingBatch getOutgoingBatch(int index) {
    if ( index >= start && index < end) {
      return outgoingBatches.get(index - start);
    }
    return null;
  }

  @Override
  public final void setup(ExchangeFragmentContext context,
                          RecordBatch incoming,
                          HashPartitionSender popConfig,
                          OperatorStats stats,
                          OperatorContext oContext,
                          ClassGenerator<?> cg,
                          int start, int end) throws SchemaChangeException {

    this.incoming = incoming;
    this.stats = stats;
    this.context = context;
    this.cg = cg;
    this.start = start;
    this.end = end;
    doSetup(context, incoming, null);

    // Consider the system/session option to allow the buffer size to shrink
    // linearly with the increase in slice count, over some limit:
    // exec.partition.mem_throttle:
    // The default is 0, which leaves the current logic unchanged.
    // If set to a positive value, then when the slice count exceeds that
    // amount, the buffer size per sender is reduced.
    // The reduction factor is 1 / (slice count - threshold), with a minimum
    // batch size of 256 records.
    //
    // So, if we set the threshold at 2, and run 10 slices, each slice will
    // get 1024 / 8 = 256 records.
    //
    // This option controls memory, but at an obvious cost of increasing overhead.
    // One could argue that this is a good thing. As the number of senders
    // increases, the number of records going to each sender decreases, which
    // increases the time that batches must accumulate before they are sent.
    //
    // If the option is enabled, and buffer size reduction kicks in, you'll
    // find an info-level log message which details the reduction:
    // exec.partition.mem_throttle is set to 2: 10 receivers,
    //       reduced send buffer size from 1024 to 256 rows
    //
    // See  DRILL-7675, DRILL-7686.
    int destinationCount = popConfig.getDestinations().size();
    int reductionCutoff = oContext.getFragmentContext().getOptions().getInt(
        ExecConstants.PARTITIONER_MEMORY_REDUCTION_THRESHOLD_KEY);
    if (reductionCutoff > 0 && destinationCount >= reductionCutoff) {
      int reducedBatchSize = Math.max(256,
          (DEFAULT_RECORD_BATCH_SIZE + 1) / (destinationCount - reductionCutoff));
      outgoingRecordBatchSize = BaseAllocator.nextPowerOfTwo(reducedBatchSize) - 1;
      logger.info("{} is set to {}: {} receivers, reduced send buffer size from {} to {} rows",
          ExecConstants.PARTITIONER_MEMORY_REDUCTION_THRESHOLD_KEY,
          reductionCutoff, destinationCount,
          DEFAULT_RECORD_BATCH_SIZE, outgoingRecordBatchSize);
    } else if (destinationCount > 1000) {
      // Half the outgoing record batch size if the number of senders exceeds 1000 to reduce the total amount of memory
      // allocated.

      // Always keep the recordCount as (2^x) - 1 to better utilize the memory allocation in ValueVectors
      outgoingRecordBatchSize = (DEFAULT_RECORD_BATCH_SIZE + 1)/2 - 1;
    }

    int fieldId = 0;
    for (MinorFragmentEndpoint destination : popConfig.getDestinations()) {
      // create outgoingBatches only for subset of Destination Points
      if (fieldId >= start && fieldId < end) {
        logger.debug("start: {}, count: {}, fieldId: {}", start, end, fieldId);
        outgoingBatches.add(newOutgoingRecordBatch(stats, popConfig,
          context.getDataTunnel(destination.getEndpoint()), context, oContext.getAllocator(), destination.getId()));
      }
      fieldId++;
    }

    for (OutgoingRecordBatch outgoingRecordBatch : outgoingBatches) {
      outgoingRecordBatch.initializeBatch();
    }

    SelectionVectorMode svMode = incoming.getSchema().getSelectionVectorMode();
    switch(svMode){
      case FOUR_BYTE:
        this.sv4 = incoming.getSelectionVector4();
        break;

      case TWO_BYTE:
        this.sv2 = incoming.getSelectionVector2();
        break;

      case NONE:
        break;

      default:
        throw new UnsupportedOperationException("Unknown selection vector mode: " + svMode.toString());
    }
  }

  /**
   * Shim method to be overridden in plain-old Java mode by the subclass to instantiate the
   * generated inner class. Byte-code manipulation appears to fix up the byte codes
   * directly. The name is special, it must be "new" + inner class name.
   */
  protected OutgoingRecordBatch newOutgoingRecordBatch(
                               OperatorStats stats, HashPartitionSender operator, AccountingDataTunnel tunnel,
                               FragmentContext context, BufferAllocator allocator, int oppositeMinorFragmentId) {
    return this.injectMembers(new OutgoingRecordBatch(stats, operator, tunnel, context, allocator, oppositeMinorFragmentId));
  }

  protected OutgoingRecordBatch injectMembers(OutgoingRecordBatch outgoingRecordBatch) {
    CodeGenMemberInjector.injectMembers(cg, outgoingRecordBatch, context);
    return outgoingRecordBatch;
  }

  @Override
  public OperatorStats getStats() {
    return stats;
  }

  /**
   * Flush each outgoing record batch, and optionally reset the state of each outgoing record
   * batch (on schema change).  Note that the schema is updated based on incoming at the time
   * this function is invoked.
   *
   * @param isLastBatch    true if this is the last incoming batch
   * @param schemaChanged  true if the schema has changed
   */
  @Override
  public void flushOutgoingBatches(boolean isLastBatch, boolean schemaChanged) throws IOException {
    for (OutgoingRecordBatch batch : outgoingBatches) {
      logger.debug("Attempting to flush all outgoing batches");
      if (isLastBatch) {
        batch.setIsLast();
      }
      batch.flush(schemaChanged);
      if (schemaChanged) {
        batch.resetBatch();
        batch.initializeBatch();
      }
    }
  }

  @Override
  public void partitionBatch(RecordBatch incoming) throws IOException {
    SelectionVectorMode svMode = incoming.getSchema().getSelectionVectorMode();

    // Keeping the for loop inside the case to avoid case evaluation for each record.
    switch(svMode) {
      case NONE:
        for (int recordId = 0; recordId < incoming.getRecordCount(); ++recordId) {
          doCopy(recordId);
        }
        break;

      case TWO_BYTE:
        for (int recordId = 0; recordId < incoming.getRecordCount(); ++recordId) {
          int svIndex = sv2.getIndex(recordId);
          doCopy(svIndex);
        }
        break;

      case FOUR_BYTE:
        for (int recordId = 0; recordId < incoming.getRecordCount(); ++recordId) {
          int svIndex = sv4.get(recordId);
          doCopy(svIndex);
        }
        break;

      default:
        throw new UnsupportedOperationException("Unknown selection vector mode: " + svMode.toString());
    }
  }

  /**
   * Helper method to copy data based on partition
   * @param svIndex
   * @throws IOException
   */
  private void doCopy(int svIndex) throws IOException {
    int index;
    try {
      index = doEval(svIndex);
    } catch (SchemaChangeException e) {
      throw new UnsupportedOperationException(e);
    }
    if ( index >= start && index < end) {
      OutgoingRecordBatch outgoingBatch = outgoingBatches.get(index - start);
      outgoingBatch.copy(svIndex);
    }
  }

  @Override
  public void initialize() { }

  @Override
  public void clear() {
    for (OutgoingRecordBatch outgoingRecordBatch : outgoingBatches) {
      outgoingRecordBatch.clear();
    }
  }

  public abstract void doSetup(@Named("context") FragmentContext context,
                               @Named("incoming") RecordBatch incoming,
                               @Named("outgoing") OutgoingRecordBatch[] outgoing)
                       throws SchemaChangeException;
  public abstract int doEval(@Named("inIndex") int inIndex) throws SchemaChangeException;

  public class OutgoingRecordBatch implements PartitionOutgoingBatch, VectorAccessible {

    private final AccountingDataTunnel tunnel;
    private final HashPartitionSender operator;
    private final FragmentContext context;
    private final VectorContainer vectorContainer;
    private final int oppositeMinorFragmentId;
    private final OperatorStats stats;

    private boolean isLast;
    private boolean dropAll;
    private int recordCount;
    private int totalRecords;

    public OutgoingRecordBatch(OperatorStats stats, HashPartitionSender operator, AccountingDataTunnel tunnel,
                               FragmentContext context, BufferAllocator allocator, int oppositeMinorFragmentId) {
      this.context = context;
      this.operator = operator;
      this.tunnel = tunnel;
      this.stats = stats;
      this.oppositeMinorFragmentId = oppositeMinorFragmentId;
      this.vectorContainer = new VectorContainer(allocator);
    }

    protected void copy(int inIndex) throws IOException {
      try {
        doEval(inIndex, recordCount);
      } catch (SchemaChangeException e) {
        throw new UnsupportedOperationException(e);
      }
      recordCount++;
      totalRecords++;
      if (recordCount == outgoingRecordBatchSize) {
        flush(false);
      }
    }

    @Override
    public void terminate() {
      // receiver already terminated, don't send anything to it from now on
      dropAll = true;
    }

    @RuntimeOverridden
    protected void doSetup(@Named("incoming") RecordBatch incoming,
                           @Named("outgoing") VectorAccessible outgoing) throws SchemaChangeException { };

    @RuntimeOverridden
    protected void doEval(@Named("inIndex") int inIndex,
                          @Named("outIndex") int outIndex) throws SchemaChangeException { };

    public void flush(boolean schemaChanged) throws IOException {
      if (dropAll) {
        // If we are in dropAll mode, we still want to copy the data, because we
        // can't stop copying a single outgoing
        // batch with out stopping all outgoing batches. Other option is check
        // for status of dropAll before copying
        // every single record in copy method which has the overhead for every
        // record all the time. Resetting the output
        // count, reusing the same buffers and copying has overhead only for
        // outgoing batches whose receiver has
        // terminated.

        // Reset the count to 0 and use existing buffers for exhausting input where receiver of this batch is terminated
        recordCount = 0;
        return;
      }
      final FragmentHandle handle = context.getHandle();

      // We need to send the last batch when
      //   1. we are actually done processing the incoming RecordBatches and no more input available
      //   2. receiver wants to terminate (possible in case of queries involving limit clause). Even when receiver wants
      //      to terminate we need to send at least one batch with "isLastBatch" set to true, so that receiver knows
      //      sender has acknowledged the terminate request. After sending the last batch, all further batches are
      //      dropped.
      //   3. Partitioner thread is interrupted due to cancellation of fragment.
      final boolean isLastBatch = isLast || Thread.currentThread().isInterrupted();

      // if the batch is not the last batch and the current recordCount is zero, then no need to send any RecordBatches
      if (!isLastBatch && recordCount == 0) {
        return;
      }

      vectorContainer.setValueCount(recordCount);

      FragmentWritableBatch writableBatch = new FragmentWritableBatch(isLastBatch,
          handle.getQueryId(),
          handle.getMajorFragmentId(),
          handle.getMinorFragmentId(),
          operator.getOppositeMajorFragmentId(),
          oppositeMinorFragmentId,
          getWritableBatch());

      updateStats(writableBatch);
      stats.startWait();
      try {
        tunnel.sendRecordBatch(writableBatch);
      } finally {
        stats.stopWait();
      }

      // If the current batch is the last batch, then set a flag to ignore any
      // requests to flush the data
      // This is possible when the receiver is terminated, but we still get data
      // from input operator
      if (isLastBatch) {
        dropAll = true;
      }

      // If this flush is not due to schema change, allocate space for existing vectors.
      if (!schemaChanged) {
        // reset values and reallocate the buffer for each value vector based on the incoming batch.
        // NOTE: the value vector is directly referenced by generated code; therefore references
        // must remain valid.
        recordCount = 0;
        vectorContainer.zeroVectors();
        allocateOutgoingRecordBatch();
      }
    }

    private void allocateOutgoingRecordBatch() {
      vectorContainer.allocate(outgoingRecordBatchSize);
    }

    public void updateStats(FragmentWritableBatch writableBatch) {
      stats.addLongStat(Metric.BYTES_SENT, writableBatch.getByteCount());
      stats.addLongStat(Metric.BATCHES_SENT, 1);
      stats.addLongStat(Metric.RECORDS_SENT, writableBatch.getHeader().getDef().getRecordCount());
    }

    /**
     * Initialize the OutgoingBatch based on the current schema in incoming RecordBatch
     */
    public void initializeBatch() {
      vectorContainer.buildFrom(incoming.getSchema());
      allocateOutgoingRecordBatch();
      try {
        doSetup(incoming, vectorContainer);
      } catch (SchemaChangeException e) {
        throw new UnsupportedOperationException(e);
      }
    }

    public void resetBatch() {
      isLast = false;
      recordCount = 0;
      vectorContainer.clear();
    }

    public void setIsLast() {
      isLast = true;
    }

    @Override
    public BatchSchema getSchema() {
      return incoming.getSchema();
    }

    @Override
    public int getRecordCount() {
      return recordCount;
    }

    @Override
    public long getTotalRecords() {
      return totalRecords;
    }

    @Override
    public TypedFieldId getValueVectorId(SchemaPath path) {
      return vectorContainer.getValueVectorId(path);
    }

    @Override
    public VectorWrapper<?> getValueAccessorById(Class<?> clazz, int... fieldIds) {
      return vectorContainer.getValueAccessorById(clazz, fieldIds);
    }

    @Override
    public Iterator<VectorWrapper<?>> iterator() {
      return vectorContainer.iterator();
    }

    @Override
    public SelectionVector2 getSelectionVector2() {
      throw new UnsupportedOperationException();
    }

    @Override
    public SelectionVector4 getSelectionVector4() {
      throw new UnsupportedOperationException();
    }

    public WritableBatch getWritableBatch() {
      return WritableBatch.getBatchNoHVWrap(recordCount, this, false);
    }

    public void clear(){
      vectorContainer.clear();
    }
  }
}
