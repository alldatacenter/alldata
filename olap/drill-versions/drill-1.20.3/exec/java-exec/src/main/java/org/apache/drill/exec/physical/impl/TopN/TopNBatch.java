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
package org.apache.drill.exec.physical.impl.TopN;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.calcite.rel.RelFieldCollation.Direction;
import org.apache.drill.common.DrillAutoCloseables;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.ErrorCollector;
import org.apache.drill.common.expression.ErrorCollectorImpl;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.logical.data.Order.Ordering;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.compile.CodeCompiler;
import org.apache.drill.exec.compile.sig.MappingSet;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.expr.ClassGenerator;
import org.apache.drill.exec.expr.ClassGenerator.HoldingContainer;
import org.apache.drill.exec.expr.CodeGenerator;
import org.apache.drill.exec.expr.ExpressionTreeMaterializer;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.expr.fn.FunctionGenerationHelper;
import org.apache.drill.exec.expr.fn.FunctionLookupContext;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.config.TopN;
import org.apache.drill.exec.physical.impl.sort.RecordBatchData;
import org.apache.drill.exec.physical.impl.sort.SortRecordBatchBuilder;
import org.apache.drill.exec.physical.impl.svremover.Copier;
import org.apache.drill.exec.physical.impl.svremover.GenericCopierFactory;
import org.apache.drill.exec.record.AbstractRecordBatch;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.exec.record.ExpandableHyperContainer;
import org.apache.drill.exec.record.HyperVectorWrapper;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.SchemaUtil;
import org.apache.drill.exec.record.SimpleRecordBatch;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.WritableBatch;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.record.selection.SelectionVector4;
import org.apache.drill.exec.server.options.OptionSet;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.complex.AbstractContainerVector;

import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.codemodel.JConditional;
import com.sun.codemodel.JExpr;

import static org.apache.drill.exec.record.RecordBatch.IterOutcome.EMIT;
import static org.apache.drill.exec.record.RecordBatch.IterOutcome.NONE;
import static org.apache.drill.exec.record.RecordBatch.IterOutcome.OK;
import static org.apache.drill.exec.record.RecordBatch.IterOutcome.OK_NEW_SCHEMA;

/**
 * Operator Batch which implements the TopN functionality. It is more efficient
 * than (sort + limit) since unlike sort it doesn't have to store all the input
 * data to sort it first and then apply limit on the sorted data. Instead
 * internally it maintains a priority queue backed by a heap with the size being
 * same as limit value.
 */
public class TopNBatch extends AbstractRecordBatch<TopN> {
  private static final Logger logger = LoggerFactory.getLogger(TopNBatch.class);

  private final MappingSet mainMapping = createMainMappingSet();
  private final MappingSet leftMapping = createLeftMappingSet();
  private final MappingSet rightMapping = createRightMappingSet();

  private final int batchPurgeThreshold;
  private final boolean codegenDump;

  private final RecordBatch incoming;
  private BatchSchema schema;
  private boolean schemaChanged;
  private PriorityQueue priorityQueue;
  private final TopN config;
  private SelectionVector4 sv4;
  private long countSincePurge;
  private int batchCount;
  private Copier copier;
  private boolean first = true;
  private int recordCount;
  private IterOutcome lastKnownOutcome = OK;
  private boolean firstBatchForSchema = true;
  private boolean hasOutputRecords;

  public TopNBatch(TopN popConfig, FragmentContext context, RecordBatch incoming) throws OutOfMemoryException {
    super(popConfig, context);
    this.incoming = incoming;
    this.config = popConfig;
    DrillConfig drillConfig = context.getConfig();
    batchPurgeThreshold = drillConfig.getInt(ExecConstants.BATCH_PURGE_THRESHOLD);
    codegenDump = drillConfig.getBoolean(CodeCompiler.ENABLE_SAVE_CODE_FOR_DEBUG_TOPN);
  }

  @Override
  public int getRecordCount() {
    return recordCount;
  }

  @Override
  public SelectionVector2 getSelectionVector2() {
    throw new UnsupportedOperationException();
  }

  @Override
  public SelectionVector4 getSelectionVector4() {
    return sv4;
  }

  @Override
  public void close() {
    releaseResource();
    super.close();
  }

  @Override
  public void buildSchema() {
    IterOutcome outcome = next(incoming);
    switch (outcome) {
      case OK:
      case OK_NEW_SCHEMA:
        for (VectorWrapper<?> w : incoming) {
          ValueVector v = container.addOrGet(w.getField());
          if (v instanceof AbstractContainerVector) {
            w.getValueVector().makeTransferPair(v);
            v.clear();
          }
          v.allocateNew();
        }
        container.buildSchema(SelectionVectorMode.NONE);
        container.setRecordCount(0);

        return;
      case NONE:
        state = BatchState.DONE;
        return;
      case EMIT:
        throw new IllegalStateException("Unexpected EMIT outcome received in buildSchema phase");
      default:
        throw new IllegalStateException("Unexpected outcome received in buildSchema phase");
    }
  }

  @Override
  public IterOutcome innerNext() {
    recordCount = 0;
    if (state == BatchState.DONE) {
      return NONE;
    }

    // Check if anything is remaining from previous record boundary
    if (hasOutputRecords) {
      return handleRemainingOutput();
    }

    // Reset the TopN state for next iteration
    resetTopNState();

    boolean incomingHasSv2 = false;
    switch (incoming.getSchema().getSelectionVectorMode()) {
      case NONE: {
        break;
      }
      case TWO_BYTE: {
        incomingHasSv2 = true;
        break;
      }
      case FOUR_BYTE: {
        throw UserException.internalError(null)
          .message("TopN doesn't support incoming with SV4 mode")
          .build(logger);
      }
      default:
        throw new UnsupportedOperationException("Unsupported SV mode detected in TopN incoming batch");
    }

    outer: while (true) {
      Stopwatch watch = Stopwatch.createStarted();
      if (first) {
        lastKnownOutcome = IterOutcome.OK_NEW_SCHEMA;
        // Create the SV4 object upfront to be used for both empty and non-empty incoming batches at EMIT boundary
        sv4 = new SelectionVector4(context.getAllocator(), 0);
        first = false;
      } else {
        lastKnownOutcome = next(incoming);
      }
      if (lastKnownOutcome == OK && schema == null) {
        lastKnownOutcome = IterOutcome.OK_NEW_SCHEMA;
        container.clear();
      }
      logger.debug("Took {} us to get next", watch.elapsed(TimeUnit.MICROSECONDS));
      switch (lastKnownOutcome) {
      case NONE:
        break outer;
      case NOT_YET:
        throw new UnsupportedOperationException();
      case OK_NEW_SCHEMA:
        // only change in the case that the schema truly changes.  Artificial schema changes are ignored.
        // schema change handling in case when EMIT is also seen is same as without EMIT. i.e. only if union type
        // is enabled it will be handled.
        container.clear();
        firstBatchForSchema = true;
        if (!incoming.getSchema().equals(schema)) {
          if (schema != null) {
            if (!unionTypeEnabled) {
              throw new UnsupportedOperationException(String.format("TopN currently doesn't support changing " +
                "schemas with union type disabled. Please try enabling union type: %s and re-execute the query",
                ExecConstants.ENABLE_UNION_TYPE_KEY));
            } else {
              schema = SchemaUtil.mergeSchemas(this.schema, incoming.getSchema());
              purgeAndResetPriorityQueue();
              schemaChanged = true;
            }
          } else {
            schema = incoming.getSchema();
          }
        }
        // fall through.
      case OK:
      case EMIT:
        if (incoming.getRecordCount() == 0) {
          for (VectorWrapper<?> w : incoming) {
            w.clear();
          }
          // Release memory for incoming SV2 vector
          if (incomingHasSv2) {
            incoming.getSelectionVector2().clear();
          }
          break;
        }
        countSincePurge += incoming.getRecordCount();
        batchCount++;
        RecordBatchData batch;
        if (schemaChanged) {
          batch = new RecordBatchData(SchemaUtil.coerceContainer(incoming, this.schema, oContext), oContext.getAllocator());
        } else {
          batch = new RecordBatchData(incoming, oContext.getAllocator());
        }
        boolean success = false;
        try {
          if (priorityQueue == null) {
            priorityQueue = createNewPriorityQueue(new ExpandableHyperContainer(batch.getContainer()), config.getLimit());
          } else if (!priorityQueue.isInitialized()) {
            // means priority queue is cleaned up after producing output for first record boundary. We should
            // initialize it for next record boundary
            priorityQueue.init(config.getLimit(), oContext.getAllocator(),
              schema.getSelectionVectorMode() == SelectionVectorMode.TWO_BYTE);
          }
          priorityQueue.add(batch);
          // Based on static threshold of number of batches, perform purge operation to release the memory for
          // RecordBatches which are of no use or doesn't fall under TopN category
          if (countSincePurge > config.getLimit() && batchCount > batchPurgeThreshold) {
            purge();
            countSincePurge = 0;
            batchCount = 0;
          }
          success = true;
        } catch (SchemaChangeException e) {
          throw schemaChangeException(e, logger);
        } finally {
          if (!success) {
            batch.clear();
          }
        }
        break;
      default:
        throw new UnsupportedOperationException();
      }

      // If the last seen outcome is EMIT then break the loop. We do it here since we want to process the batch
      // with records and EMIT outcome in above case statements
      if (lastKnownOutcome == EMIT) {
        break;
      }
    }

    // PriorityQueue can be null here if first batch is received with OK_NEW_SCHEMA and is empty and second next()
    // call returned NONE or EMIT.
    // PriorityQueue can be uninitialized here if only empty batch is received between 2 EMIT outcome.
    if (schema == null || (priorityQueue == null || !priorityQueue.isInitialized())) {
      // builder may be null at this point if the first incoming batch is empty
      return handleEmptyBatches(lastKnownOutcome);
    }

    priorityQueue.generate();
    prepareOutputContainer(priorityQueue.getHyperBatch(), priorityQueue.getFinalSv4());

    // With EMIT outcome control will come here multiple times whereas without EMIT outcome control will only come
    // here once. In EMIT outcome case if there is schema change in any iteration then that will be handled by
    // lastKnownOutcome.
    return getFinalOutcome();
  }

  /**
   * When PriorityQueue is built up then it stores the list of limit number of
   * record indexes (in heapSv4) which falls under TopN category. But it also
   * stores all the incoming RecordBatches with all records inside a
   * HyperContainer (hyperBatch). When a certain threshold of batches are
   * reached then this method is called which copies the limit number of records
   * whose indexes are stored in heapSv4 out of HyperBatch to a new
   * VectorContainer and releases all other records and their batches. Later
   * this new VectorContainer is stored inside the HyperBatch and it's
   * corresponding indexes are stored in the heapSv4 vector. This is done to
   * avoid holding up lot's of Record Batches which can create OutOfMemory
   * condition.
   */
  private void purge() {
    Stopwatch watch = Stopwatch.createStarted();
    VectorContainer c = priorityQueue.getHyperBatch();

    // Simple VectorConatiner which stores limit number of records only. The records whose indexes are stored inside
    // selectionVector4 below are only copied from Hyper container to this simple container.
    VectorContainer newContainer = new VectorContainer(oContext);
    // SV4 storing the limit number of indexes
    SelectionVector4 selectionVector4 = priorityQueue.getSv4();
    SimpleSV4RecordBatch batch = new SimpleSV4RecordBatch(c, selectionVector4, context);
    if (copier == null) {
      copier = GenericCopierFactory.createAndSetupCopier(batch, newContainer, null);
    } else {
      for (VectorWrapper<?> i : batch) {

        ValueVector v = TypeHelper.getNewVector(i.getField(), oContext.getAllocator());
        newContainer.add(v);
      }
      copier.setup(batch, newContainer);
    }
    SortRecordBatchBuilder builder = new SortRecordBatchBuilder(oContext.getAllocator());
    try {
      // Purge all the existing batches to a new batch which only holds the selected records
      copyToPurge(newContainer, builder);
      // New VectorContainer that contains only limit number of records and is later passed to resetQueue to create a
      // HyperContainer backing the priority queue out of it
      VectorContainer newQueue = new VectorContainer();
      builder.build(newQueue);
      try {
        priorityQueue.resetQueue(newQueue, builder.getSv4().createNewWrapperCurrent());
      } catch (SchemaChangeException e) {
        throw schemaChangeException(e, logger);
      }
      builder.getSv4().clear();
    } finally {
      DrillAutoCloseables.closeNoChecked(builder);
    }
    logger.debug("Took {} us to purge", watch.elapsed(TimeUnit.MICROSECONDS));
  }

  private PriorityQueue createNewPriorityQueue(VectorAccessible batch, int limit) {
    return createNewPriorityQueue(
      mainMapping, leftMapping, rightMapping, config.getOrderings(), batch, unionTypeEnabled,
      codegenDump, limit, oContext.getAllocator(), schema.getSelectionVectorMode(), context);
  }

  public static MappingSet createMainMappingSet() {
    return new MappingSet((String) null, null, ClassGenerator.DEFAULT_SCALAR_MAP, ClassGenerator.DEFAULT_SCALAR_MAP);
  }

  public static MappingSet createLeftMappingSet() {
    return new MappingSet("leftIndex", null, ClassGenerator.DEFAULT_SCALAR_MAP, ClassGenerator.DEFAULT_SCALAR_MAP);
  }

  public static MappingSet createRightMappingSet() {
    return new MappingSet("rightIndex", null, ClassGenerator.DEFAULT_SCALAR_MAP, ClassGenerator.DEFAULT_SCALAR_MAP);
  }

  public static PriorityQueue createNewPriorityQueue(
    MappingSet mainMapping, MappingSet leftMapping, MappingSet rightMapping,
    List<Ordering> orderings, VectorAccessible batch, boolean unionTypeEnabled, boolean codegenDump,
    int limit, BufferAllocator allocator, SelectionVectorMode mode, FragmentContext context) {
    OptionSet optionSet = context.getOptions();
    FunctionLookupContext functionLookupContext = context.getFunctionRegistry();
    CodeGenerator<PriorityQueue> cg = CodeGenerator.get(PriorityQueue.TEMPLATE_DEFINITION, optionSet);
    cg.plainJavaCapable(true);
    cg.saveCodeForDebugging(codegenDump);
    // Uncomment out this line to debug the generated code.
    // cg.saveCodeForDebugging(true);
    ClassGenerator<PriorityQueue> g = cg.getRoot();
    g.setMappingSet(mainMapping);

    for (Ordering od : orderings) {
      // first, we rewrite the evaluation stack for each side of the comparison.
      ErrorCollector collector = new ErrorCollectorImpl();
      final LogicalExpression expr = ExpressionTreeMaterializer.materialize(od.getExpr(), batch, collector, functionLookupContext, unionTypeEnabled);
      collector.reportErrors(logger);
      g.setMappingSet(leftMapping);
      HoldingContainer left = g.addExpr(expr, ClassGenerator.BlkCreateMode.FALSE);
      g.setMappingSet(rightMapping);
      HoldingContainer right = g.addExpr(expr, ClassGenerator.BlkCreateMode.FALSE);
      g.setMappingSet(mainMapping);

      // next we wrap the two comparison sides and add the expression block for the comparison.
      LogicalExpression fh =
        FunctionGenerationHelper.getOrderingComparator(od.nullsSortHigh(), left, right, functionLookupContext);
      HoldingContainer out = g.addExpr(fh, ClassGenerator.BlkCreateMode.FALSE);
      JConditional jc = g.getEvalBlock()._if(out.getValue().ne(JExpr.lit(0)));

      if (od.getDirection() == Direction.ASCENDING) {
        jc._then()._return(out.getValue());
      } else {
        jc._then()._return(out.getValue().minus());
      }
      g.rotateBlock();
    }

    g.rotateBlock();
    g.getEvalBlock()._return(JExpr.lit(0));

    PriorityQueue q = context.getImplementationClass(cg);
    try {
      q.init(limit, allocator, mode == BatchSchema.SelectionVectorMode.TWO_BYTE);
    } catch (SchemaChangeException e) {
      throw TopNBatch.schemaChangeException(e, "Top N", logger);
    }
    return q;
  }

  /**
   * Handle schema changes during execution.
   * 1. Purge existing batches
   * 2. Promote newly created container for new schema.
   * 3. Recreate priority queue and reset with coerced container.
   */
  public void purgeAndResetPriorityQueue() {
    final Stopwatch watch = Stopwatch.createStarted();
    final VectorContainer c = priorityQueue.getHyperBatch();
    final VectorContainer newContainer = new VectorContainer(oContext);
    final SelectionVector4 selectionVector4 = priorityQueue.getSv4();
    final SimpleSV4RecordBatch batch = new SimpleSV4RecordBatch(c, selectionVector4, context);
    copier = GenericCopierFactory.createAndSetupCopier(batch, newContainer, null);
    SortRecordBatchBuilder builder = new SortRecordBatchBuilder(oContext.getAllocator());
    try {
      // Purge all the existing batches to a new batch which only holds the selected records
      copyToPurge(newContainer, builder);
      final VectorContainer oldSchemaContainer = new VectorContainer(oContext);
      builder.build(oldSchemaContainer);
      oldSchemaContainer.setRecordCount(builder.getSv4().getCount());
      final VectorContainer newSchemaContainer =  SchemaUtil.coerceContainer(oldSchemaContainer, this.schema, oContext);
      newSchemaContainer.buildSchema(SelectionVectorMode.FOUR_BYTE);
      priorityQueue.cleanup();
      priorityQueue = createNewPriorityQueue(newSchemaContainer, config.getLimit());
      try {
        priorityQueue.resetQueue(newSchemaContainer, builder.getSv4().createNewWrapperCurrent());
      } catch (SchemaChangeException e) {
        throw schemaChangeException(e, logger);
      }
    } finally {
      builder.clear();
      builder.close();
    }
    logger.debug("Took {} us to purge and recreate queue for new schema", watch.elapsed(TimeUnit.MICROSECONDS));
  }

  @Override
  public WritableBatch getWritableBatch() {
    throw new UnsupportedOperationException("A sort batch is not writable.");
  }

  @Override
  protected void cancelIncoming() {
    incoming.cancel();
  }

  /**
   * Resets TopNBatch state to process next incoming batches independent of
   * already seen incoming batches.
   */
  private void resetTopNState() {
    lastKnownOutcome = OK;
    countSincePurge = 0;
    batchCount = 0;
    hasOutputRecords = false;
    releaseResource();
  }

  /**
   * Cleanup resources held by TopN Batch such as sv4, priority queue and outgoing container
   */
  private void releaseResource() {
    if (sv4 != null) {
      sv4.clear();
    }

    if (priorityQueue != null) {
      priorityQueue.cleanup();
    }
    container.zeroVectors();
  }

  /**
   * Returns the final IterOutcome which TopN should return for this next call. Return OK_NEW_SCHEMA with first output
   * batch after a new schema is seen. This is indicated by firstBatchSchema flag. It is also true for very first
   * output batch after buildSchema()phase too since in buildSchema() a dummy schema was returned downstream without
   * correct SelectionVectorMode.
   * In other cases when there is no schema change then either OK or EMIT is returned with output batches depending upon
   * if EMIT is seen or not. In cases when EMIT is not seen then OK is always returned with an output batch. When all
   * the data is returned then NONE is sent in the end.
   *
   * @return - IterOutcome - outcome to send downstream
   */
  private IterOutcome getFinalOutcome() {
    IterOutcome outcomeToReturn;

    if (firstBatchForSchema) {
      outcomeToReturn = OK_NEW_SCHEMA;
      firstBatchForSchema = false;
    } else if (recordCount == 0) {
      // get the outcome to return before calling refresh since that resets the lastKnowOutcome to OK
      outcomeToReturn = lastKnownOutcome == EMIT ? EMIT : NONE;
      resetTopNState();
    } else if (lastKnownOutcome == EMIT) {
      // in case of EMIT check if this output batch returns all the data or not. If yes then return EMIT along with this
      // output batch else return OK. Remaining data will be sent downstream in subsequent next() call.
      final boolean hasMoreRecords = sv4.hasNext();
      outcomeToReturn = (hasMoreRecords) ? OK : EMIT;
      hasOutputRecords = hasMoreRecords;
    } else {
      outcomeToReturn = OK;
    }

    return outcomeToReturn;
  }

  /**
   * Copies all the selected records into the new container to purge all the incoming batches into a single batch.
   * @param newContainer - New container holding the ValueVectors with selected records
   * @param batchBuilder - Builder to build hyper vectors batches
   * @throws SchemaChangeException
   */
  private void copyToPurge(VectorContainer newContainer, SortRecordBatchBuilder batchBuilder) {
    final VectorContainer c = priorityQueue.getHyperBatch();
    final SelectionVector4 queueSv4 = priorityQueue.getSv4();
    final SimpleSV4RecordBatch newBatch = new SimpleSV4RecordBatch(newContainer, null, context);

    do {
      // count is the limit number of records required by TopN batch
      final int count = queueSv4.getCount();
      // Transfers count number of records from hyperBatch to simple container
      final int copiedRecords = copier.copyRecords(0, count);
      assert copiedRecords == count;
      newContainer.buildSchema(BatchSchema.SelectionVectorMode.NONE);
      newContainer.setValueCount(count);
      // Store all the batches containing limit number of records
      batchBuilder.add(newBatch);
    } while (queueSv4.next());
    // Release the memory stored for the priority queue heap to store indexes
    queueSv4.clear();
    // Release the memory from HyperBatch container
    c.clear();
  }

  /**
   * Prepares an output container with batches from Priority Queue for each record boundary. In case when this is the
   * first batch for the known schema (indicated by true value of firstBatchForSchema) the output container is cleared
   * and recreated with new HyperVectorWrapper objects and ValueVectors from PriorityQueue. In cases when the schema
   * has not changed then it prepares the container keeping the VectorWrapper and SV4 references as is since that is
   * what is needed by downstream operator.
   */
  private void prepareOutputContainer(VectorContainer dataContainer, SelectionVector4 dataSv4) {
    container.zeroVectors();
    hasOutputRecords = true;
    // Check if this is the first output batch for the new known schema. If yes then prepare the output container
    // with the proper vectors, otherwise re-use the previous vectors.
    if (firstBatchForSchema) {
      container.clear();
      for (VectorWrapper<?> w : dataContainer) {
        container.add(w.getValueVectors());
      }
      container.buildSchema(BatchSchema.SelectionVectorMode.FOUR_BYTE);
      sv4 = dataSv4;
    } else {
      // Schema didn't changed so we should keep the reference of HyperVectorWrapper in outgoing container intact and
      // populate the HyperVectorWrapper with new list of vectors. Here the assumption is order of ValueVectors is same
      // across multiple record boundary unless a new schema is observed
      int index = 0;
      for (VectorWrapper<?> w : dataContainer) {
        HyperVectorWrapper<?> wrapper = (HyperVectorWrapper<?>) container.getValueVector(index++);
        wrapper.updateVectorList(w.getValueVectors());
      }
      // Since the reference of SV4 is held by downstream operator and there is no schema change, so just copy the
      // underlying buffer from priority queue sv4.
      this.sv4.copy(dataSv4);
    }
    recordCount = sv4.getCount();
    container.setRecordCount(recordCount);
  }

  /**
   * Method handles returning correct outcome and setting recordCount for output container when next() is called
   * multiple time for single record boundary. It handles cases when some output was already returned at current record
   * boundary but Either there is more left to return OR proper outcome with empty batch is left to return.
   * Example: For first EMIT record boundary if all the records were returned in previous call with OK_NEW_SCHEMA
   * outcome, then this method will handle returning empty output batch with EMIT outcome in subsequent next() call.
   * @return - Outcome to return downstream
   */
  private IterOutcome handleRemainingOutput() {
    // if priority queue is not null that means the incoming batches were non-empty. And if there are more records
    // to send downstream for this record boundary
    if (priorityQueue != null && sv4.next()) {
      recordCount = sv4.getCount();
      container.setRecordCount(recordCount);
    } else { // This means that either:
      // 1) Priority Queue was not null and all records have been sent downstream for this record boundary
      // 2) Or Priority Queue is null, since all the incoming batches were empty for current record boundary (or EMIT
      // outcome). In the previous call we must have returned OK_NEW_SCHEMA along with SV4 container, so it will
      // return EMIT outcome now
      recordCount = 0;
      container.setRecordCount(0);
    }
    return getFinalOutcome();
  }

  /**
   * Method to handle preparing output container and returning proper outcome to downstream when either NONE or only
   * empty batches have been seen but with EMIT outcome. In either of the case PriorityQueue is not created yet since no
   * actual records have been received so far.
   * @param incomingOutcome - outcome received from upstream. Either NONE or EMIT
   * @return - outcome to return downstream. NONE when incomingOutcome is NONE. OK_NEW_SCHEMA/EMIT when incomingOutcome
   * is EMIT and is first/non-first empty input batch respectively.
   */
  private IterOutcome handleEmptyBatches(IterOutcome incomingOutcome) {
    IterOutcome outcomeToReturn = incomingOutcome;

    // In case of NONE it will change state to DONE and return NONE whereas in case of
    // EMIT it has to still continue working for future records.
    if (incomingOutcome == NONE) { // this means we saw NONE
      state = BatchState.DONE;
      container.clear();
      recordCount = 0;
      container.setRecordCount(recordCount);
    } else if (incomingOutcome == EMIT) {
      // since priority queue is null that means it has not seen any batch with data
      assert (countSincePurge == 0 && batchCount == 0);
      final VectorContainer hyperContainer = new ExpandableHyperContainer(incoming.getContainer());
      prepareOutputContainer(hyperContainer, sv4);

      // update the outcome to return
      outcomeToReturn = getFinalOutcome();
    }

    return outcomeToReturn;
  }

  public static class SimpleSV4RecordBatch extends SimpleRecordBatch {
    private final SelectionVector4 sv4;

    public SimpleSV4RecordBatch(VectorContainer container, SelectionVector4 sv4, FragmentContext context) {
      super(container, context);
      this.sv4 = sv4;
    }

    @Override
    public int getRecordCount() {
      if (sv4 != null) {
        return sv4.getCount();
      } else {
        return super.getRecordCount();
      }
    }

    @Override
    public SelectionVector4 getSelectionVector4() {
      return sv4;
    }
  }

  @Override
  public void dump() {
    logger.error("TopNBatch[container={}, config={}, schema={}, sv4={}, countSincePurge={}, " +
        "batchCount={}, recordCount={}]", container, config, schema, sv4, countSincePurge, batchCount, recordCount);
  }
}
