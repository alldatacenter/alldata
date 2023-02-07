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
package org.apache.drill.exec.physical.impl.mergereceiver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import org.apache.calcite.rel.RelFieldCollation.Direction;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.ErrorCollector;
import org.apache.drill.common.expression.ErrorCollectorImpl;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.logical.data.Order.Ordering;
import org.apache.drill.exec.compile.sig.GeneratorMapping;
import org.apache.drill.exec.compile.sig.MappingSet;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.expr.ClassGenerator;
import org.apache.drill.exec.expr.ClassGenerator.HoldingContainer;
import org.apache.drill.exec.expr.CodeGenerator;
import org.apache.drill.exec.expr.ExpressionTreeMaterializer;
import org.apache.drill.exec.expr.fn.FunctionGenerationHelper;
import org.apache.drill.exec.ops.ExchangeFragmentContext;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.ops.MetricDef;
import org.apache.drill.exec.ops.QueryCancelledException;
import org.apache.drill.exec.physical.MinorFragmentEndpoint;
import org.apache.drill.exec.physical.config.MergingReceiverPOP;
import org.apache.drill.exec.proto.BitControl.FinishedReceiver;
import org.apache.drill.exec.proto.BitData;
import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;
import org.apache.drill.exec.proto.GeneralRPCProtos.Ack;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.proto.UserBitShared.SerializedField;
import org.apache.drill.exec.record.AbstractRecordBatch;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.exec.record.ExpandableHyperContainer;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.RawFragmentBatch;
import org.apache.drill.exec.record.RawFragmentBatchProvider;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.RecordBatchLoader;
import org.apache.drill.exec.record.SchemaBuilder;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.exec.rpc.RpcOutcomeListener;
import org.apache.drill.exec.testing.ControlsInjector;
import org.apache.drill.exec.testing.ControlsInjectorFactory;
import org.apache.drill.exec.vector.AllocationHelper;
import org.apache.drill.exec.vector.CopyUtil;
import org.apache.drill.exec.vector.FixedWidthVector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.codemodel.JConditional;
import com.sun.codemodel.JExpr;

import io.netty.buffer.ByteBuf;

/**
 * Merges pre-sorted record batches from remote senders.
 */
public class MergingRecordBatch extends AbstractRecordBatch<MergingReceiverPOP> implements RecordBatch {
  private static final Logger logger = LoggerFactory.getLogger(MergingRecordBatch.class);
  private static final ControlsInjector injector = ControlsInjectorFactory.getInjector(MergingRecordBatch.class);

  private static final int OUTGOING_BATCH_SIZE = 32 * 1024;

  private RecordBatchLoader[] batchLoaders;
  private final RawFragmentBatchProvider[] fragProviders;
  private final ExchangeFragmentContext context;
  private MergingReceiverGeneratorBase merger;
  private final MergingReceiverPOP config;
  private boolean hasRun;
  private boolean outgoingBatchHasSpace = true;
  private boolean hasMoreIncoming = true;

  private int outgoingPosition;
  private int senderCount;
  private RawFragmentBatch[] incomingBatches;
  private int[] batchOffsets;
  private PriorityQueue <Node> pqueue;
  private RawFragmentBatch[] tempBatchHolder;
  private final long[] inputCounts;
  private final long[] outputCounts;

  public enum Metric implements MetricDef {
    BYTES_RECEIVED,
    NUM_SENDERS,
    NEXT_WAIT_NANOS;

    @Override
    public int metricId() {
      return ordinal();
    }
  }

  public MergingRecordBatch(final ExchangeFragmentContext context,
                            final MergingReceiverPOP config,
                            final RawFragmentBatchProvider[] fragProviders) throws OutOfMemoryException {
    super(config, context, true, context.newOperatorContext(config));
    this.fragProviders = fragProviders;
    this.context = context;
    this.stats.setLongStat(Metric.NUM_SENDERS, config.getNumSenders());
    this.config = config;
    this.inputCounts = new long[config.getNumSenders()];
    this.outputCounts = new long[config.getNumSenders()];

    // Register this operator's buffer allocator so that incoming buffers are owned by this allocator
    context.getBuffers().getCollector(config.getOppositeMajorFragmentId()).setAllocator(oContext.getAllocator());
  }

  private RawFragmentBatch getNext(final int providerIndex) {
    stats.startWait();
    final RawFragmentBatchProvider provider = fragProviders[providerIndex];
    try {
      injector.injectInterruptiblePause(context.getExecutionControls(), "waiting-for-data", logger);
      RawFragmentBatch b;
      try {
        b = provider.getNext();
      } catch (IOException e) {
        // TODO: Better to handle inside getNext() to provide a better error message
        throw UserException.dataReadError(e)
            .addContext("Failed to read incoming merge batch")
            .build(logger);
      }
      if (b != null) {
        stats.addLongStat(Metric.BYTES_RECEIVED, b.getByteCount());
        stats.batchReceived(0, b.getHeader().getDef().getRecordCount(), false);
        inputCounts[providerIndex] += b.getHeader().getDef().getRecordCount();
      }
      return b;
    } catch (final InterruptedException e) {
      // Preserve evidence that the interruption occurred so that code higher up on the call stack can learn of the
      // interruption and respond to it if it wants to.
      Thread.currentThread().interrupt();

      throw new QueryCancelledException();
    } finally {
      stats.stopWait();
    }
  }

  private void clearBatches(List<RawFragmentBatch> batches) {
    for (RawFragmentBatch batch : batches) {
      if (batch != null) {
        batch.release();
      }
    }
  }

  @Override
  public IterOutcome innerNext() {
    if (fragProviders.length == 0) {
      return IterOutcome.NONE;
    }
    boolean schemaChanged = false;

    if (!outgoingBatchHasSpace) {
      logger.debug("Outgoing vectors were full on last iteration");
      allocateOutgoing();
      outgoingPosition = 0;
      outgoingBatchHasSpace = true;
    }

    if (!hasMoreIncoming) {
      logger.debug("next() was called after all values have been processed");
      outgoingPosition = 0;
      return IterOutcome.NONE;
    }

    List<UserBitShared.SerializedField> fieldList = null;
    boolean createDummyBatch = false;

    // lazy initialization
    if (!hasRun) {
      schemaChanged = true; // first iteration is always a schema change

      // set up each (non-empty) incoming record batch
      final List<RawFragmentBatch> rawBatches = Lists.newArrayList();
      try {
        int p = 0;
        for (@SuppressWarnings("unused") final RawFragmentBatchProvider provider : fragProviders) {
          RawFragmentBatch rawBatch;
          // check if there is a batch in temp holder before calling getNext(), as it may have been used when building schema
          if (tempBatchHolder[p] != null) {
            rawBatch = tempBatchHolder[p];
            tempBatchHolder[p] = null;
          } else {
            rawBatch = getNext(p);
          }
          checkContinue();

          // If rawBatch is null, go ahead and add it to the list. We will create dummy batches
          // for all null batches later.
          if (rawBatch == null) {
            checkContinue();
            createDummyBatch = true;
            rawBatches.add(rawBatch);
            p++; // move to next sender
            continue;
          }

          if (fieldList == null && rawBatch.getHeader().getDef().getFieldCount() != 0) {
            // save the schema to fix up empty batches with no schema if needed.
              fieldList = rawBatch.getHeader().getDef().getFieldList();
          }

          if (rawBatch.getHeader().getDef().getRecordCount() != 0) {
            rawBatches.add(rawBatch);
          } else {
            // keep reading till we get a batch with record count > 0 or we have no more batches to read i.e. we get null
            while ((rawBatch = getNext(p)) != null && rawBatch.getHeader().getDef().getRecordCount() == 0) {
              // Do nothing
            }
            if (rawBatch == null) {
              checkContinue();
              createDummyBatch = true;
            }
            if (rawBatch == null || rawBatch.getHeader().getDef().getFieldCount() == 0) {
              createDummyBatch = true;
            }
            // Even if rawBatch is null, go ahead and add it to the list.
            // We will create dummy batches for all null batches later.
            rawBatches.add(rawBatch);
          }
          p++;
        }

        // If no batch arrived with schema from any of the providers, just return NONE.
        if (fieldList == null) {
          return IterOutcome.NONE;
        }

        // Go through and fix schema for empty batches.
        if (createDummyBatch) {
          // Create dummy record batch definition with 0 record count
          UserBitShared.RecordBatchDef dummyDef = UserBitShared.RecordBatchDef.newBuilder()
              // we cannot use/modify the original field list as that is used by
              // valid record batch.
              // create a copy of field list with valuecount = 0 for all fields.
              // This is for dummy schema generation.
              .addAllField(createDummyFieldList(fieldList))
              .setRecordCount(0)
              .build();

          // Create dummy header
          BitData.FragmentRecordBatch dummyHeader = BitData.FragmentRecordBatch.newBuilder()
              .setIsLastBatch(true)
              .setDef(dummyDef)
              .build();

          for (int i = 0; i < p; i++) {
            RawFragmentBatch rawBatch = rawBatches.get(i);
            if (rawBatch == null || rawBatch.getHeader().getDef().getFieldCount() == 0) {
              rawBatch = new RawFragmentBatch(dummyHeader, null, null);
              rawBatches.set(i, rawBatch);
            }
          }
        }
      } catch (Throwable t) {
        clearBatches(rawBatches);
        throw t;
      }

      // allocate the incoming record batch loaders
      senderCount = rawBatches.size();
      incomingBatches = new RawFragmentBatch[senderCount];
      batchOffsets = new int[senderCount];
      batchLoaders = new RecordBatchLoader[senderCount];
      for (int i = 0; i < senderCount; ++i) {
        incomingBatches[i] = rawBatches.get(i);
        batchLoaders[i] = new RecordBatchLoader(oContext.getAllocator());
      }

      // after this point all batches have moved to incomingBatches
      rawBatches.clear();

      int i = 0;
      for (final RawFragmentBatch batch : incomingBatches) {
        // initialize the incoming batchLoaders
        final UserBitShared.RecordBatchDef rbd = batch.getHeader().getDef();
        batchLoaders[i].load(rbd, batch.getBody());
        batch.release();
        ++batchOffsets[i];
        ++i;
      }

      // after this point all batches have been released and their bytebuf are in batchLoaders

      // Ensure all the incoming batches have the identical schema.
      // Note: RecordBatchLoader permutes the columns to obtain the same columns order for all batches.
      checkSameSchemaAmongBatches(batchLoaders);

      // create the outgoing schema and vector container, and allocate the initial batch
      final SchemaBuilder bldr = BatchSchema.newBuilder().setSelectionVectorMode(BatchSchema.SelectionVectorMode.NONE);
      for (final VectorWrapper<?> v : batchLoaders[0]) {

        // add field to the output schema
        bldr.addField(v.getField());

        // allocate a new value vector
        container.addOrGet(v.getField());
      }
      allocateOutgoing();

      container.buildSchema(BatchSchema.SelectionVectorMode.NONE);

      // generate code for merge operations (copy and compare)
      merger = createMerger();

      // allocate the priority queue with the generated comparator
      this.pqueue = new PriorityQueue<>(fragProviders.length, new Comparator<Node>() {
        @Override
        public int compare(final Node node1, final Node node2) {
          final int leftIndex = (node1.batchId << 16) + node1.valueIndex;
          final int rightIndex = (node2.batchId << 16) + node2.valueIndex;
          try {
            return merger.doEval(leftIndex, rightIndex);
          } catch (SchemaChangeException e) {
            throw new UnsupportedOperationException(e);
          }
        }
      });

      // populate the priority queue with initial values
      for (int b = 0; b < senderCount; ++b) {
        while (batchLoaders[b] != null && batchLoaders[b].getRecordCount() == 0) {
          final RawFragmentBatch batch = getNext(b);
          incomingBatches[b] = batch;
          if (batch != null) {
            batchLoaders[b].load(batch.getHeader().getDef(), batch.getBody());
          } else {
            batchLoaders[b].clear();
            batchLoaders[b] = null;
            checkContinue();
          }
        }
        if (batchLoaders[b] != null) {
          pqueue.add(new Node(b, 0));
        }
      }

      hasRun = true;
      // finished lazy initialization
    }

    while (outgoingBatchHasSpace) {
      // poll next value from pq and copy to outgoing batch
      final Node node = pqueue.poll();
      if (node == null) {
        break;
      }
      outgoingBatchHasSpace = copyRecordToOutgoingBatch(node);

      if (node.valueIndex == batchLoaders[node.batchId].getRecordCount() - 1) {
        // reached the end of an incoming record batch
        RawFragmentBatch nextBatch;
        nextBatch = getNext(node.batchId);

        while (nextBatch != null && nextBatch.getHeader().getDef().getRecordCount() == 0) {
          nextBatch = getNext(node.batchId);
        }

        assert nextBatch != null || inputCounts[node.batchId] == outputCounts[node.batchId]
            : String.format("Stream %d input count: %d output count %d", node.batchId, inputCounts[node.batchId], outputCounts[node.batchId]);
        if (nextBatch == null) {
          checkContinue();
        }

        incomingBatches[node.batchId] = nextBatch;

        if (nextBatch == null) {
          // batch is empty
          boolean allBatchesEmpty = true;

          for (final RawFragmentBatch batch : incomingBatches) {
            // see if all batches are empty so we can return OK_* or NONE
            if (batch != null) {
              allBatchesEmpty = false;
              break;
            }
          }

          if (allBatchesEmpty) {
            hasMoreIncoming = false;
            break;
          }

          // this batch is empty; since the pqueue no longer references this batch, it will be
          // ignored in subsequent iterations.
          continue;
        }

        final UserBitShared.RecordBatchDef rbd = incomingBatches[node.batchId].getHeader().getDef();
        batchLoaders[node.batchId].load(rbd, incomingBatches[node.batchId].getBody());
        incomingBatches[node.batchId].release();
        batchOffsets[node.batchId] = 0;

        // add front value from batch[x] to priority queue
        if (batchLoaders[node.batchId].getRecordCount() != 0) {
          node.valueIndex = 0;
          pqueue.add(node);
        }
      } else {
        node.valueIndex++;
        pqueue.add(node);
      }
    }

    // set the value counts in the outgoing vectors
    container.setValueCount(outgoingPosition);

    if (pqueue.isEmpty()) {
      state = BatchState.DONE;
    }

    if (schemaChanged) {
      return IterOutcome.OK_NEW_SCHEMA;
    }
    else {
      return IterOutcome.OK;
    }
  }

  // Create dummy field that will be used for empty batches.
  private UserBitShared.SerializedField createDummyField(UserBitShared.SerializedField field) {
    UserBitShared.SerializedField.Builder newDummyFieldBuilder = UserBitShared.SerializedField.newBuilder()
        .setVarByteLength(0)
        .setBufferLength(0)
        .setValueCount(0)
        .setNamePart(field.getNamePart())
        .setMajorType(field.getMajorType());

    int index = 0;
    for (UserBitShared.SerializedField childField : field.getChildList()) {
      // make sure we make a copy of all children, so we do not corrupt the
      // original fieldList. This will recursively call itself.
      newDummyFieldBuilder.addChild(index, createDummyField(childField));
      index++;
    }

    UserBitShared.SerializedField newDummyField = newDummyFieldBuilder.build();

    return newDummyField;
  }

  // Create a dummy field list that we can use for empty batches.
  private List<UserBitShared.SerializedField> createDummyFieldList(List<UserBitShared.SerializedField> fieldList) {
    List<UserBitShared.SerializedField> dummyFieldList = new ArrayList<UserBitShared.SerializedField>();

    for (UserBitShared.SerializedField field : fieldList) {
      dummyFieldList.add(createDummyField(field));
    }

    return dummyFieldList;
  }

  @Override
  public FragmentContext getContext() {
    return context;
  }

  @Override
  public BatchSchema getSchema() {
    if (container.hasSchema()) {
      return container.getSchema();
    }
    return null;
  }

  @Override
  public void buildSchema() {
    // find frag provider that has data to use to build schema, and put in tempBatchHolder for later use
    tempBatchHolder = new RawFragmentBatch[fragProviders.length];
    int i = 0;
    while (true) {
      if (i >= fragProviders.length) {
        state = BatchState.DONE;
        return;
      }
      final RawFragmentBatch batch = getNext(i);
      if (batch == null) {
        checkContinue();
        break;
      }
      if (batch.getHeader().getDef().getFieldCount() == 0) {
        i++;
        continue;
      }
      tempBatchHolder[i] = batch;
      for (final SerializedField field : batch.getHeader().getDef().getFieldList()) {
        final ValueVector v = container.addOrGet(MaterializedField.create(field));
        v.allocateNew();
      }
      break;
    }
    container.buildSchema(SelectionVectorMode.NONE);
    container.setEmpty();
  }

  @Override
  public int getRecordCount() {
    return outgoingPosition;
  }

  @Override
  public void cancel() {
    informSenders();

    for (final RawFragmentBatchProvider provider : fragProviders) {
      provider.kill(context);
    }
  }

  private void informSenders() {
    logger.info("Informing senders of request to terminate sending.");
    final FragmentHandle handlePrototype = FragmentHandle.newBuilder()
            .setMajorFragmentId(config.getOppositeMajorFragmentId())
            .setQueryId(context.getHandle().getQueryId())
            .build();
    for (final MinorFragmentEndpoint providingEndpoint : config.getProvidingEndpoints()) {
      final FragmentHandle sender = FragmentHandle.newBuilder(handlePrototype)
              .setMinorFragmentId(providingEndpoint.getId())
              .build();
      final FinishedReceiver finishedReceiver = FinishedReceiver.newBuilder()
              .setReceiver(context.getHandle())
              .setSender(sender)
              .build();
      context.getController()
        .getTunnel(providingEndpoint.getEndpoint())
        .informReceiverFinished(new OutcomeListener(), finishedReceiver);
    }
  }

  // TODO: Code duplication. UnorderedReceiverBatch has the same implementation.
  private class OutcomeListener implements RpcOutcomeListener<Ack> {

    @Override
    public void failed(final RpcException ex) {
      logger.warn("Failed to inform upstream that receiver is finished");
    }

    @Override
    public void success(final Ack value, final ByteBuf buffer) {
      // Do nothing
    }

    @Override
    public void interrupted(final InterruptedException e) {
      if (context.getExecutorState().shouldContinue()) {
        final String errMsg = "Received an interrupt RPC outcome while sending ReceiverFinished message";
        logger.error(errMsg, e);
        context.getExecutorState().fail(new RpcException(errMsg, e));
      }
    }
  }

  @Override
  protected void cancelIncoming() { }

  private void checkSameSchemaAmongBatches(final RecordBatchLoader[] batchLoaders) {
    Preconditions.checkArgument(batchLoaders.length > 0, "0 batch is not allowed!");

    final BatchSchema schema = batchLoaders[0].getSchema();

    for (int i = 1; i < batchLoaders.length; i++) {
      if (!schema.equals(batchLoaders[i].getSchema())) {
        throw UserException.schemaChangeError()
          .message("Incoming batches for merging receiver have different schemas!")
          .addContext("Schema 1: %s, Schema 2: %s",
              schema.toString())
          .addContext("Schema 2: %s",
              batchLoaders[i].getSchema().toString())
          .build(logger);
      }
    }
  }

  private void allocateOutgoing() {
    for (final VectorWrapper<?> w : container) {
      final ValueVector v = w.getValueVector();
      if (v instanceof FixedWidthVector) {
        AllocationHelper.allocate(v, OUTGOING_BATCH_SIZE, 1);
      } else {
        v.allocateNewSafe();
      }
    }
  }

  /**
   * Creates a generate class which implements the copy and compare methods.
   *
   * @return instance of a new merger based on generated code
   * @throws SchemaChangeException
   */
  private MergingReceiverGeneratorBase createMerger() {
    stats.startSetup();
    final CodeGenerator<MergingReceiverGeneratorBase> cg =
        CodeGenerator.get(MergingReceiverGeneratorBase.TEMPLATE_DEFINITION,
            context.getOptions());
    cg.plainJavaCapable(true);
    // Uncomment out this line to debug the generated code.
    // cg.saveCodeForDebugging(true);
    final ClassGenerator<MergingReceiverGeneratorBase> g = cg.getRoot();

    ExpandableHyperContainer batch = null;
    boolean first = true;
    for (final RecordBatchLoader loader : batchLoaders) {
      if (first) {
        batch = new ExpandableHyperContainer(loader);
        first = false;
      } else {
        batch.addBatch(loader);
      }
    }

    try {
      generateComparisons(g, batch);

      g.setMappingSet(COPIER_MAPPING_SET);
      CopyUtil.generateCopies(g, batch, true);
      g.setMappingSet(MAIN_MAPPING);
      final MergingReceiverGeneratorBase merger = context.getImplementationClass(cg);

      merger.doSetup(context, batch, container);
      return merger;
    } catch (SchemaChangeException e) {
      throw schemaChangeException(e, logger);
    } finally {
      stats.stopSetup();
    }
  }

  private final MappingSet MAIN_MAPPING = new MappingSet( (String) null, null, ClassGenerator.DEFAULT_SCALAR_MAP, ClassGenerator.DEFAULT_SCALAR_MAP);
  private final MappingSet LEFT_MAPPING = new MappingSet("leftIndex", null, ClassGenerator.DEFAULT_SCALAR_MAP, ClassGenerator.DEFAULT_SCALAR_MAP);
  private final MappingSet RIGHT_MAPPING = new MappingSet("rightIndex", null, ClassGenerator.DEFAULT_SCALAR_MAP, ClassGenerator.DEFAULT_SCALAR_MAP);
  private final GeneratorMapping COPIER_MAPPING = new GeneratorMapping("doSetup", "doCopy", null, null);
  private final MappingSet COPIER_MAPPING_SET = new MappingSet(COPIER_MAPPING, COPIER_MAPPING);

  private void generateComparisons(final ClassGenerator<?> g, final VectorAccessible batch) throws SchemaChangeException {
    g.setMappingSet(MAIN_MAPPING);

    for (final Ordering od : popConfig.getOrderings()) {
      // first, we rewrite the evaluation stack for each side of the comparison.
      final ErrorCollector collector = new ErrorCollectorImpl();
      final LogicalExpression expr = ExpressionTreeMaterializer.materialize(od.getExpr(), batch, collector,context.getFunctionRegistry());
      if (collector.hasErrors()) {
        throw new SchemaChangeException("Failure while materializing expression. " + collector.toErrorString());
      }
      g.setMappingSet(LEFT_MAPPING);
      final HoldingContainer left = g.addExpr(expr, ClassGenerator.BlkCreateMode.FALSE);
      g.setMappingSet(RIGHT_MAPPING);
      final HoldingContainer right = g.addExpr(expr, ClassGenerator.BlkCreateMode.FALSE);
      g.setMappingSet(MAIN_MAPPING);

      // next we wrap the two comparison sides and add the expression block for the comparison.
      final LogicalExpression fh =
          FunctionGenerationHelper.getOrderingComparator(od.nullsSortHigh(), left, right,
                                                         context.getFunctionRegistry());
      final HoldingContainer out = g.addExpr(fh, ClassGenerator.BlkCreateMode.FALSE);
      final JConditional jc = g.getEvalBlock()._if(out.getValue().ne(JExpr.lit(0)));

      if (od.getDirection() == Direction.ASCENDING) {
        jc._then()._return(out.getValue());
      } else {
        jc._then()._return(out.getValue().minus());
      }
    }

    g.getEvalBlock()._return(JExpr.lit(0));
  }

  /**
   * Copy the record referenced by the supplied node to the next output position.
   * Side Effect: increments outgoing position if successful
   *
   * @param node Reference to the next record to copy from the incoming batches
   */
  private boolean copyRecordToOutgoingBatch(final Node node) {
    assert outgoingPosition < OUTGOING_BATCH_SIZE
        : String.format("Outgoing position %d must be less than bath size %d", outgoingPosition, OUTGOING_BATCH_SIZE);
    assert ++outputCounts[node.batchId] <= inputCounts[node.batchId]
        : String.format("Stream %d input count: %d output count %d", node.batchId, inputCounts[node.batchId], outputCounts[node.batchId]);
    final int inIndex = (node.batchId << 16) + node.valueIndex;
    try {
      merger.doCopy(inIndex, outgoingPosition);
    } catch (SchemaChangeException e) {
      throw new UnsupportedOperationException(e);
    }
    if (++outgoingPosition == OUTGOING_BATCH_SIZE) {
      logger.debug("Outgoing vectors space is full (batch size {}).", OUTGOING_BATCH_SIZE);
      return false;
    }
    return true;
  }

  /**
   * A Node contains a reference to a single value in a specific incoming batch.  It is used
   * as a wrapper for the priority queue.
   */
  public class Node {
    public int batchId;      // incoming batch
    public int valueIndex;   // value within the batch
    Node(final int batchId, final int valueIndex) {
      this.batchId = batchId;
      this.valueIndex = valueIndex;
    }
  }

  @Override
  public void close() {
    container.clear();
    if (batchLoaders != null) {
      for (final RecordBatchLoader rbl : batchLoaders) {
        if (rbl != null) {
          rbl.clear();
        }
      }
    }
    super.close();
  }

  @Override
  public void dump() {
    logger.error("MergingRecordBatch[container={}, outgoingPosition={}, incomingBatches={}, batchOffsets={}, "
            + "tempBatchHolder={}, inputCounts={}, outputCounts={}]",
        container, outgoingPosition, Arrays.toString(incomingBatches), Arrays.toString(batchOffsets),
        Arrays.toString(tempBatchHolder), Arrays.toString(inputCounts), Arrays.toString(outputCounts));
  }
}
