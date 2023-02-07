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
package org.apache.drill.exec.physical.impl.join;

import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.base.LateralContract;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.config.LateralJoinPOP;
import org.apache.drill.exec.record.AbstractBinaryRecordBatch;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.JoinBatchMemoryManager;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.RecordBatchSizer;
import org.apache.drill.exec.record.SchemaBuilder;
import org.apache.drill.exec.record.VectorAccessibleUtilities;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.util.record.RecordBatchStats;
import org.apache.drill.exec.util.record.RecordBatchStats.RecordBatchIOType;
import org.apache.drill.exec.vector.IntVector;
import org.apache.drill.exec.vector.ValueVector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.apache.drill.exec.record.RecordBatch.IterOutcome.EMIT;
import static org.apache.drill.exec.record.RecordBatch.IterOutcome.NONE;
import static org.apache.drill.exec.record.RecordBatch.IterOutcome.OK;
import static org.apache.drill.exec.record.RecordBatch.IterOutcome.OK_NEW_SCHEMA;

/**
 * RecordBatch implementation for the lateral join operator. Currently it's
 * expected LATERAL to co-exists with UNNEST operator. Both LATERAL and UNNEST
 * will share a contract with each other defined at {@link LateralContract}.
 */
public class LateralJoinBatch extends AbstractBinaryRecordBatch<LateralJoinPOP> implements LateralContract {
  private static final Logger logger = LoggerFactory.getLogger(LateralJoinBatch.class);

  // Maximum number records in the outgoing batch
  private int maxOutputRowCount;

  // Schema on the left side
  private BatchSchema leftSchema;

  // Schema on the right side
  private BatchSchema rightSchema;

  // Index in output batch to populate next row
  private int outputIndex;

  // Current index of record in left incoming which is being processed
  private int leftJoinIndex = -1;

  // Current index of record in right incoming which is being processed
  private int rightJoinIndex = -1;

  // flag to keep track if current left batch needs to be processed in future next call
  private boolean processLeftBatchInFuture;

  // Keep track if any matching right record was found for current left index record
  private boolean matchedRecordFound;

  // Used only for testing
  private boolean useMemoryManager = true;

  // Flag to keep track of new left batch so that update on memory manager is called only once per left batch
  private boolean isNewLeftBatch;

  private final HashSet<String> excludedFieldNames = new HashSet<>();

  private final String implicitColumn;

  private boolean hasRemainderForLeftJoin;

  private ValueVector implicitVector;

  // Map to cache reference of input and corresponding output vectors for left and right batches
  private final Map<ValueVector, ValueVector> leftInputOutputVector = new HashMap<>();

  private final Map<ValueVector, ValueVector> rightInputOutputVector = new HashMap<>();

  /* ****************************************************************************************************************
   * Public Methods
   * ****************************************************************************************************************/
  public LateralJoinBatch(LateralJoinPOP popConfig, FragmentContext context,
                          RecordBatch left, RecordBatch right) throws OutOfMemoryException {
    super(popConfig, context, left, right);
    Preconditions.checkNotNull(left);
    Preconditions.checkNotNull(right);
    final int configOutputBatchSize = (int) context.getOptions().getOption(ExecConstants.OUTPUT_BATCH_SIZE_VALIDATOR);
    RecordBatchStats.printConfiguredBatchSize(getRecordBatchStatsContext(), configOutputBatchSize);
    implicitColumn = popConfig.getImplicitRIDColumn();

    populateExcludedField(popConfig);
    batchMemoryManager = new JoinBatchMemoryManager(configOutputBatchSize, left, right, excludedFieldNames);

    // Initially it's set to default value of 64K and later for each new output row it will be set to the computed
    // row count
    maxOutputRowCount = batchMemoryManager.getOutputRowCount();
  }

  /**
   * Handles cases where previous output batch got full after processing all the
   * batches from right side for a left side batch. But there are still few
   * unprocessed rows in left batch which cannot be ignored because JoinType is
   * {@code LeftJoin}.
   *
   * @return - true if all the rows in left batch is produced in output
   *         container false if there is still some rows pending in left
   *         incoming container
   */
  private boolean handleRemainingLeftRows() {
    Preconditions.checkState(popConfig.getJoinType() == JoinRelType.LEFT,
      "Unexpected leftover rows from previous left batch when join type is not left join");

    while(leftJoinIndex < left.getRecordCount() && !isOutgoingBatchFull()) {
      emitLeft(leftJoinIndex, outputIndex, 1);
      ++outputIndex;
      ++leftJoinIndex;
    }

    // Check if there is still pending left rows
    return leftJoinIndex >= left.getRecordCount();
  }

  /**
   * Gets the left and right incoming batch and produce the output batch. If the
   * left incoming batch is empty then next on right branch is not called and
   * empty batch with correct outcome is returned. If non empty left incoming
   * batch is received then it call's next on right branch to get an incoming
   * and finally produces output.
   *
   * @return IterOutcome state of the lateral join batch
   */
  @Override
  public IterOutcome innerNext() {

    if (hasRemainderForLeftJoin) { // if set that means there is spill over from previous left batch and no
      // corresponding right rows and it is left join scenario
      allocateVectors();

      boolean hasMoreRows = !handleRemainingLeftRows();
      if (leftUpstream == EMIT || hasMoreRows) {
        logger.debug("Sending current output batch with EMIT outcome since left is received with EMIT and is fully " +
          "consumed now in output batch");
        hasRemainderForLeftJoin = hasMoreRows;
        finalizeOutputContainer();
        return (leftUpstream == EMIT) ? EMIT : OK;
      } else {
        // release memory for previous left batch
        leftJoinIndex = -1;
        VectorAccessibleUtilities.clear(left);
      }
    }

    // We don't do anything special on FIRST state. Process left batch first and then right batch if need be
    IterOutcome childOutcome = processLeftBatch();
    logger.debug("Received left batch with outcome {}", childOutcome);

    if (processLeftBatchInFuture && hasRemainderForLeftJoin) {
      finalizeOutputContainer();
      hasRemainderForLeftJoin = false;
      return OK;
    }

    // reset this state after calling processLeftBatch above.
    processLeftBatchInFuture = false;
    hasRemainderForLeftJoin = false;

    // If the left batch doesn't have any record in the incoming batch (with OK_NEW_SCHEMA/EMIT) or the state returned
    // from left side is terminal state then just return the IterOutcome and don't call next() on right branch
    if (isTerminalOutcome(childOutcome) || left.getRecordCount() == 0) {
      container.setRecordCount(0);
      return childOutcome;
    }

    // Left side has some records in the batch so let's process right batch
    childOutcome = processRightBatch();
    logger.debug("Received right batch with outcome {}", childOutcome);

    // reset the left & right outcomes to OK here and send the empty batch downstream. Non-Empty right batch with
    // OK_NEW_SCHEMA will be handled in subsequent next call
    if (childOutcome == OK_NEW_SCHEMA) {
      leftUpstream = (leftUpstream != EMIT) ? OK : leftUpstream;
      rightUpstream = OK;
      return childOutcome;
    }

    if (isTerminalOutcome(childOutcome)) {
      return childOutcome;
    }

    // If OK_NEW_SCHEMA is seen only on non empty left batch but not on right batch, then we should setup schema in
    // output container based on new left schema and old right schema. If schema change failed then return STOP
    // downstream
    if (leftUpstream == OK_NEW_SCHEMA) {
      handleSchemaChange();
    }

    // Setup the references of left, right and outgoing container in generated operator
    state = BatchState.NOT_FIRST;

    // Update the memory manager only if its a brand new incoming i.e. leftJoinIndex and rightJoinIndex is 0
    // Otherwise there will be a case where while filling last output batch, some records from previous left or
    // right batch are still left to be sent in output for which we will count this batch twice. The actual checks
    // are done in updateMemoryManager
    updateMemoryManager(LEFT_INDEX);

    // We have to call update on memory manager for empty batches (rightJoinIndex = -1) as well since other wise while
    // allocating memory for vectors below it can fail. Since in that case colSize will not have any info on right side
    // vectors and throws NPE. The actual checks are done in updateMemoryManager
    updateMemoryManager(RIGHT_INDEX);

    if (outputIndex > 0) {
      // this means batch is already allocated but because of new incoming the width and output row count might have
      // changed. So update the maxOutputRowCount with new value
      if (useMemoryManager) {
        setMaxOutputRowCount(batchMemoryManager.getCurrentOutgoingMaxRowCount());
      }
    }
    // if output is not allocated then maxRowCount will be set correctly below
    // allocate space for the outgoing batch
    allocateVectors();

    return produceOutputBatch();
  }

  @Override
  public void close() {
    updateBatchMemoryManagerStats();

    RecordBatchStats.logRecordBatchStats(getRecordBatchStatsContext(),
      "incoming aggregate left: batch count : %d, avg bytes : %d,  avg row bytes : %d, " +
      "record count : %d", batchMemoryManager.getNumIncomingBatches(JoinBatchMemoryManager.LEFT_INDEX),
      batchMemoryManager.getAvgInputBatchSize(JoinBatchMemoryManager.LEFT_INDEX),
      batchMemoryManager.getAvgInputRowWidth(JoinBatchMemoryManager.LEFT_INDEX),
      batchMemoryManager.getTotalInputRecords(JoinBatchMemoryManager.LEFT_INDEX));

    RecordBatchStats.logRecordBatchStats(getRecordBatchStatsContext(),
      "incoming aggregate right: batch count : %d, avg bytes : %d,  avg row bytes : %d, " +
      "record count : %d", batchMemoryManager.getNumIncomingBatches(JoinBatchMemoryManager.RIGHT_INDEX),
      batchMemoryManager.getAvgInputBatchSize(JoinBatchMemoryManager.RIGHT_INDEX),
      batchMemoryManager.getAvgInputRowWidth(JoinBatchMemoryManager.RIGHT_INDEX),
      batchMemoryManager.getTotalInputRecords(JoinBatchMemoryManager.RIGHT_INDEX));

    RecordBatchStats.logRecordBatchStats(getRecordBatchStatsContext(),
      "outgoing aggregate: batch count : %d, avg bytes : %d,  avg row bytes : %d, " +
      "record count : %d", batchMemoryManager.getNumOutgoingBatches(),
      batchMemoryManager.getAvgOutputBatchSize(),
      batchMemoryManager.getAvgOutputRowWidth(),
      batchMemoryManager.getTotalOutputRecords());

    super.close();
  }

  @Override
  public int getRecordCount() {
    return container.getRecordCount();
  }

  /**
   * Returns the left side incoming for the Lateral Join. Used by right branch
   * leaf operator of Lateral to process the records at {@code leftJoinIndex}.
   *
   * @return - RecordBatch received as left side incoming
   */
  @Override
  public RecordBatch getIncoming() {
    Preconditions.checkState (left != null,
      "Retuning null left batch. It's unexpected since right side will only be called iff " +
        "there is any valid left batch");
    return left;
  }

  /**
   * Returns the current row index which the calling operator should process in
   * current incoming left record batch. LATERAL should never return it as -1
   * since that indicated current left batch is empty and LATERAL will never
   * call next on right side with empty left batch
   *
   * @return - int - index of row to process.
   */
  @Override
  public int getRecordIndex() {
    Preconditions.checkState (leftJoinIndex < left.getRecordCount(),
      "Left join index: %s is out of bounds: %s", leftJoinIndex, left.getRecordCount());
    return leftJoinIndex;
  }

  /**
   * Returns the current {@link org.apache.drill.exec.record.RecordBatch.IterOutcome} for the left incoming batch
   */
  @Override
  public IterOutcome getLeftOutcome() {
    return leftUpstream;
  }

  /* ****************************************************************************************************************
   * Protected Methods
   * ****************************************************************************************************************/

  /**
   * Get the left and right batch during build schema phase for
   * {@link LateralJoinBatch}. If left batch sees a failure outcome then we
   * don't even call next on right branch, since there is no left incoming.
   *
   * @return true if both the left/right batch was received without failure
   *         outcome. false if either of batch is received with failure outcome.
   */
  @Override
  protected boolean prefetchFirstBatchFromBothSides() {
    // Left can get batch with zero or more records with OK_NEW_SCHEMA outcome as first batch
    leftUpstream = next(0, left);

    boolean validBatch = setBatchState(leftUpstream);

    if (validBatch) {
      isNewLeftBatch = true;
      rightUpstream = next(1, right);
      validBatch = setBatchState(rightUpstream);
    }

    // EMIT outcome is not expected as part of first batch from either side
    if (leftUpstream == EMIT || rightUpstream == EMIT) {
      throw new IllegalStateException("Unexpected IterOutcome.EMIT received either from left or right side in " +
        "buildSchema phase");
    }
    return validBatch;
  }

  /**
   * Prefetch a batch from left and right branch to know about the schema of each side. Then adds value vector in
   * output container based on those schemas. For this phase LATERAL always expect's an empty batch from right side
   * which UNNEST should abide by.
   *
   * @throws SchemaChangeException if batch schema was changed during execution
   */
  @Override
  protected void buildSchema() {
    // Prefetch a RecordBatch from both left and right branch
    if (!prefetchFirstBatchFromBothSides()) {
      return;
    }
    Preconditions.checkState(right.getRecordCount() == 0,
      "Unexpected non-empty first right batch received");

    // Setup output container schema based on known left and right schema
    setupNewSchema();

    // Release the vectors received from right side
    VectorAccessibleUtilities.clear(right);

    // Set join index as invalid (-1) if the left side is empty, else set it to 0
    leftJoinIndex = (left.getRecordCount() <= 0) ? -1 : 0;
    rightJoinIndex = -1;

    // Reset the left side of the IterOutcome since for this call, OK_NEW_SCHEMA will be returned correctly
    // by buildSchema caller and we should treat the batch as received with OK outcome.
    leftUpstream = OK;
    rightUpstream = OK;
  }

  @Override
  protected void cancelIncoming() {
    left.cancel();
    right.cancel();
  }

  /* ****************************************************************************************************************
   * Private Methods
   * ****************************************************************************************************************/

  private void handleSchemaChange() {
    try {
      stats.startSetup();
      logger.debug("Setting up new schema based on incoming batch. Old output schema: {}", container.getSchema());
      setupNewSchema();
    } finally {
      stats.stopSetup();
    }
  }

  private boolean isTerminalOutcome(IterOutcome outcome) {
    return outcome == NONE;
  }

  /**
   * Process left incoming batch with different
   * {@link org.apache.drill.exec.record.RecordBatch.IterOutcome}. It is called
   * from main {@link LateralJoinBatch#innerNext()} block with each
   * {@code next()} call from upstream operator. Also when we populate the
   * outgoing container then this method is called to get next left batch if
   * current one is fully processed. It calls {@code next()} on left side until
   * we get a non-empty RecordBatch. OR we get either of
   * {@code OK_NEW_SCHEMA/EMIT/NONE/STOP/OOM/NOT_YET} outcome.
   *
   * @return IterOutcome after processing current left batch
   */
  private IterOutcome processLeftBatch() {

    boolean needLeftBatch = leftJoinIndex == -1;

    // If left batch is empty
    while (needLeftBatch) {

      if (!processLeftBatchInFuture) {
        leftUpstream = next(LEFT_INDEX, left);
        isNewLeftBatch = true;
      }

      final boolean emptyLeftBatch = left.getRecordCount() <=0;
      logger.trace("Received a left batch and isEmpty: {}", emptyLeftBatch);

      switch (leftUpstream) {
        case OK_NEW_SCHEMA:
          // This OK_NEW_SCHEMA is received post build schema phase and from left side
          if (outputIndex > 0) { // can only reach here from produceOutputBatch
            // This means there is already some records from previous join inside left batch
            // So we need to pass that downstream and then handle the OK_NEW_SCHEMA in subsequent next call
            processLeftBatchInFuture = true;
            return OK_NEW_SCHEMA;
          }

          // If left batch is empty with actual schema change then just rebuild the output container and send empty
          // batch downstream
          if (emptyLeftBatch) {
            handleSchemaChange();
            leftJoinIndex = -1;
            return OK_NEW_SCHEMA;
          } // else - setup the new schema information after getting it from right side too.
        case OK:
          // With OK outcome we will keep calling next until we get a batch with >0 records
          if (emptyLeftBatch) {
            leftJoinIndex = -1;
            continue;
          } else {
            leftJoinIndex = 0;
          }
          break;
        case EMIT:
          // don't call next on right batch
          if (emptyLeftBatch) {
            leftJoinIndex = -1;
            return EMIT;
          } else {
            leftJoinIndex = 0;
          }
          break;
        case NONE:
          // Not using =0 since if outgoing container is empty then no point returning anything
          if (outputIndex > 0) { // can only reach here from produceOutputBatch
            processLeftBatchInFuture = true;
          }
          return leftUpstream;
        case NOT_YET:
          try {
            Thread.sleep(5);
          } catch (InterruptedException ex) {
            logger.debug("Thread interrupted while sleeping to call next on left branch of LATERAL since it " +
              "received NOT_YET");
          }
          break;
        default:
          throw new IllegalStateException("Unexpected iter outcome: " + leftUpstream.name());
      }
      needLeftBatch = leftJoinIndex == -1;
    }
    return leftUpstream;
  }

  /**
   * Process right incoming batch with different
   * {@link org.apache.drill.exec.record.RecordBatch.IterOutcome}. It is called
   * from main {@link LateralJoinBatch#innerNext()} block with each next() call
   * from upstream operator and if left batch has some records in it. Also when
   * we populate the outgoing container then this method is called to get next
   * right batch if current one is fully processed.
   *
   * @return IterOutcome after processing current left batch
   */
  private IterOutcome processRightBatch() {
    // Check if we still have records left to process in left incoming from new batch or previously half processed
    // batch based on indexes. We are making sure to update leftJoinIndex and rightJoinIndex correctly. Like for new
    // batch leftJoinIndex will always be set to zero and once leftSide batch is fully processed then it will be set
    // to -1.
    // Whereas rightJoinIndex is to keep track of record in right batch being joined with record in left batch.
    // So when there are cases such that all records in right batch is not consumed by the output, then rightJoinIndex
    // will be a valid index. When all records are consumed it will be set to -1.
    boolean needNewRightBatch = (leftJoinIndex >= 0) && (rightJoinIndex == -1);
    while (needNewRightBatch) {
      rightUpstream = next(RIGHT_INDEX, right);
      switch (rightUpstream) {
        case OK_NEW_SCHEMA:

          // If there is some records in the output batch that means left batch didn't came with OK_NEW_SCHEMA,
          // otherwise it would have been marked for processInFuture and output will be returned. This means for
          // current non processed left or new left non-empty batch there is unexpected right batch schema change
          if (outputIndex > 0) {
            throw new IllegalStateException("SchemaChange on right batch is not expected in between the rows of " +
              "current left batch or a new non-empty left batch with no schema change");
          }
          // We should not get OK_NEW_SCHEMA multiple times for the same left incoming batch. So there won't be a
          // case where we get OK_NEW_SCHEMA --> OK (with batch) ---> OK_NEW_SCHEMA --> OK/EMIT fall through
          //
          // Right batch with OK_NEW_SCHEMA can be non-empty so update the rightJoinIndex correctly and pass the
          // new schema downstream with empty batch and later with subsequent next() call the join output will be
          // produced
          handleSchemaChange();
          container.setEmpty();
          rightJoinIndex = (right.getRecordCount() > 0) ? 0 : -1;
          return OK_NEW_SCHEMA;
        case OK:
        case EMIT:
          // Even if there are no records we should not call next() again because in case of LEFT join empty batch is
          // of importance too
          rightJoinIndex = (right.getRecordCount() > 0) ? 0 : -1;
          needNewRightBatch = false;
          break;
        case NONE:
          needNewRightBatch = false;
          break;
        case NOT_YET:
          try {
            Thread.sleep(10);
          } catch (InterruptedException ex) {
            logger.debug("Thread interrupted while sleeping to call next on left branch of LATERAL since it " +
              "received NOT_YET");
          }
          break;
        default:
          throw new IllegalStateException("Unexpected iter outcome: " + leftUpstream.name());
      }
    }
    return rightUpstream;
  }

  /**
   * Gets the current left and right incoming batch and does the cross join to
   * fill the output batch. If all the records in the either or both the batches
   * are consumed then it get's next batch from that branch depending upon if
   * output batch still has some space left. If output batch is full then the
   * output is finalized to be sent downstream. Subsequent call's knows how to
   * consume previously half consumed (if any) batches and producing the output
   * using that.
   *
   * @return - IterOutcome to be send along with output batch to downstream
   *         operator
   */
  private IterOutcome produceOutputBatch() {

    boolean isLeftProcessed = false;

    // Try to fully pack the outgoing container
    while (!isOutgoingBatchFull()) {
      // perform the cross join between records in left and right batch and populate the output container
      crossJoinAndOutputRecords();

      // rightJoinIndex should move by number of records in output batch for current right batch only. For cases when
      // right batch is fully consumed rightJoinIndex will be equal to record count. For cases when only part of it is
      // consumed in current output batch rightJoinIndex will point to next row to be consumed
      final boolean isRightProcessed = rightJoinIndex == -1 || rightJoinIndex >= right.getRecordCount();

      // Check if above join to produce output resulted in fully consuming right side batch
      if (isRightProcessed) {
        // Release vectors of right batch. This will happen for both rightUpstream = EMIT/OK
        VectorAccessibleUtilities.clear(right);
        rightJoinIndex = -1;
      }

      // Check if all rows in right batch is processed and there was a match for last rowId and this is last
      // right batch for this left batch, then increment the leftJoinIndex. If this is not the last right batch we
      // cannot increase the leftJoinIndex even though a match is found because next right batch can contain more
      // records for the same implicit rowId
      if (isRightProcessed && rightUpstream == EMIT && matchedRecordFound) {
        ++leftJoinIndex;
        matchedRecordFound = false;
      }

      // left is only declared as processed if this is last right batch for current left batch and we have processed
      // all the rows in it.
      isLeftProcessed = (rightUpstream == EMIT) && leftJoinIndex >= left.getRecordCount();

      // Even though if left batch is not fully processed but we have received EMIT outcome from right side.
      // In this case if left batch has some unprocessed rows and it's left join emit left side for these rows.
      // If it's inner join then just set treat left batch as processed.
      if (!isLeftProcessed && rightUpstream == EMIT && isRightProcessed) {
        if (popConfig.getJoinType() == JoinRelType.LEFT) {
          // If outgoing batch got full that means we still have some leftJoinIndex to output but right side is done
          // producing the batches. So mark hasRemainderForLeftJoin=true and we will take care of it in future next call.
          isLeftProcessed = handleRemainingLeftRows();
          hasRemainderForLeftJoin = !isLeftProcessed;
        } else {
          // not left join hence ignore rows pending in left batch since right side is done producing the output
          isLeftProcessed = true;
        }
      }

      if (isLeftProcessed) {
        leftJoinIndex = -1;
        VectorAccessibleUtilities.clear(left);
        matchedRecordFound = false;
      }

      // Check if output batch still has some space
      if (!isOutgoingBatchFull()) {
        // Check if left side still has records or not
        if (isLeftProcessed) {
          // The current left batch was with EMIT/OK_NEW_SCHEMA outcome, then return output to downstream layer before
          // getting next batch
          if (leftUpstream == EMIT || leftUpstream == OK_NEW_SCHEMA) {
            break;
          } else {
            logger.debug("Output batch still has some space left, getting new batches from left and right. OutIndex: {}",
              outputIndex);
            // Get both left batch and the right batch and make sure indexes are properly set
            leftUpstream = processLeftBatch();

            logger.debug("Received left batch with outcome {}", leftUpstream);

            // output batch is not empty and we have new left batch with OK_NEW_SCHEMA or terminal outcome
            if (processLeftBatchInFuture) {
              logger.debug("Received left batch such that we have to return the current outgoing batch and process " +
                "the new batch in subsequent next call");
              // We should return the current output batch with OK outcome and don't reset the leftUpstream
              finalizeOutputContainer();
              return OK;
            }

            // If left batch received a terminal outcome then don't call right batch
            if (isTerminalOutcome(leftUpstream)) {
              finalizeOutputContainer();
              return leftUpstream;
            }

            // If we have received the left batch with EMIT outcome and is empty then we should return previous output
            // batch with EMIT outcome
            if ((leftUpstream == EMIT || leftUpstream == OK_NEW_SCHEMA) && left.getRecordCount() == 0) {
              isLeftProcessed = true;
              break;
            }

            // Update the batch memory manager to use new left incoming batch
            updateMemoryManager(LEFT_INDEX);
          }
        }

        // If we are here it means one of the below:
        // 1) Either previous left batch was not fully processed and it came with OK outcome. There is still some space
        // left in outgoing batch so let's get next right batch.
        // 2) OR previous left & right batch was fully processed and it came with OK outcome. There is space in outgoing
        // batch. Now we have got new left batch with OK outcome. Let's get next right batch
        // 3) OR previous left & right batch was fully processed and left came with OK outcome. Outgoing batch is
        // empty since all right batches were empty for all left rows. Now we got another non-empty left batch with
        // OK_NEW_SCHEMA.
        rightUpstream = processRightBatch();

        logger.debug("Received right batch with outcome {}", rightUpstream);

        if (rightUpstream == OK_NEW_SCHEMA) {
          leftUpstream = (leftUpstream != EMIT) ? OK : leftUpstream;
          rightUpstream = OK;
          finalizeOutputContainer();
          return OK_NEW_SCHEMA;
        }

        if (isTerminalOutcome(rightUpstream)) {
          finalizeOutputContainer();
          return rightUpstream;
        }

        // Update the batch memory manager to use new right incoming batch
        updateMemoryManager(RIGHT_INDEX);

        // If previous left batch is fully processed and it didn't produced any output rows and later we got a new
        // non-empty left batch with OK_NEW_SCHEMA with schema change only on left side vectors, then setup schema
        // in output container based on new left schema and old right schema. If schema change failed then return STOP
        // downstream
        if (leftUpstream == OK_NEW_SCHEMA && outputIndex == 0) {
          handleSchemaChange();
          // Since schema has change so we have new empty vectors in output container hence allocateMemory for them
          allocateVectors();
        } else {
          // means we are using already allocated output batch so row count may have changed based on new incoming
          // batch hence update it
          if (useMemoryManager) {
            setMaxOutputRowCount(batchMemoryManager.getCurrentOutgoingMaxRowCount());
          }
        }
      }
    } // output batch is full to its max capacity

    finalizeOutputContainer();

    // Check if output batch was full and left was fully consumed or not. Since if left is not consumed entirely
    // but output batch is full, then if the left batch came with EMIT outcome we should send this output batch along
    // with OK outcome not with EMIT. Whereas if output is full and left is also fully consumed then we should send
    // EMIT outcome.
    if (leftUpstream == EMIT && isLeftProcessed) {
      logger.debug("Sending current output batch with EMIT outcome since left is received with EMIT and is fully " +
        "consumed in output batch");
      return EMIT;
    }

    if (leftUpstream == OK_NEW_SCHEMA) {
      // return output batch with OK_NEW_SCHEMA and reset the state to OK
      logger.debug("Sending current output batch with OK_NEW_SCHEMA and resetting the left outcome to OK for next set" +
        " of batches");
      leftUpstream = OK;
      return OK_NEW_SCHEMA;
    }
    return OK;
  }
  /**
   * Finalizes the current output container with the records produced so far before sending it downstream
   */
  private void finalizeOutputContainer() {
    VectorAccessibleUtilities.setValueCount(container, outputIndex);

    // Set the record count in the container
    container.setRecordCount(outputIndex);

    batchMemoryManager.updateOutgoingStats(outputIndex);

    RecordBatchStats.logRecordBatchStats(RecordBatchIOType.OUTPUT, this, getRecordBatchStatsContext());
    RecordBatchStats.logRecordBatchStats(getRecordBatchStatsContext(),
      "Number of records emitted: %d and Allocator Stats: [AllocatedMem: %d, PeakMem: %d]",
        outputIndex, container.getAllocator().getAllocatedMemory(), container.getAllocator().getPeakMemoryAllocation());

    // Update the output index for next output batch to zero
    outputIndex = 0;
  }

  /**
   * Check if the schema changed between provided newSchema and oldSchema. It relies on
   * {@link BatchSchema#isEquivalent(BatchSchema)}.
   * @param newSchema - New Schema information
   * @param oldSchema -  - New Schema information to compare with
   *
   * @return - true - if newSchema is not same as oldSchema
   *         - false - if newSchema is same as oldSchema
   */
  @SuppressWarnings("unused")
  private boolean isSchemaChanged(BatchSchema newSchema, BatchSchema oldSchema) {
    return (newSchema == null || oldSchema == null) || !newSchema.isEquivalent(oldSchema);
  }

  /**
   * Validate if the input schema is not null and doesn't contain any Selection Vector.
   * @param schema - input schema to verify
   * @return - true: valid input schema
   *           false: invalid input schema
   */
  private boolean verifyInputSchema(BatchSchema schema) {

    boolean isValid = true;
    if (schema == null) {
      logger.error("Null schema found for the incoming batch");
      isValid = false;
    } else {
      final BatchSchema.SelectionVectorMode svMode = schema.getSelectionVectorMode();
      if (svMode != BatchSchema.SelectionVectorMode.NONE) {
        logger.error("Incoming batch schema found with selection vector which is not supported. SVMode: {}",
          svMode.toString());
        isValid = false;
      }
    }
    return isValid;
  }

  private BatchSchema batchSchemaWithNoExcludedCols(BatchSchema originSchema, boolean isRightBatch) {
    if (excludedFieldNames.size() == 0) {
      return originSchema;
    }

    final SchemaBuilder newSchemaBuilder =
      BatchSchema.newBuilder().setSelectionVectorMode(originSchema.getSelectionVectorMode());
    for (MaterializedField field : originSchema) {
      // Don't ignore implicit column from left side in multilevel case where plan is generated such that lower lateral
      // is on the right side of upper lateral.
      if (!excludedFieldNames.contains(field.getName()) ||
        (field.getName().equals(implicitColumn) && !isRightBatch)) {
        newSchemaBuilder.addField(field);
      }
    }
    return newSchemaBuilder.build();
  }

  /**
   * Helps to create the outgoing container vectors based on known left and right batch schemas
   * @throws SchemaChangeException
   */
  private void setupNewSchema() {

    logger.debug("Setting up new schema based on incoming batch. New left schema: {} and New right schema: {}",
      left.getSchema(), right.getSchema());

    // Clear up the container
    container.clear();
    leftInputOutputVector.clear();
    rightInputOutputVector.clear();

    leftSchema = batchSchemaWithNoExcludedCols(left.getSchema(), false);
    rightSchema = batchSchemaWithNoExcludedCols(right.getSchema(), true);

    if (!verifyInputSchema(leftSchema)) {
      throw UserException.schemaChangeError()
        .message("Invalid Schema found for left incoming batch of lateral join")
        .build(logger);
    }

    if (!verifyInputSchema(rightSchema)) {
      throw UserException.schemaChangeError()
        .message("Invalid Schema found for right incoming batch of lateral join")
        .build(logger);
    }

    // Setup LeftSchema in outgoing container and also include implicit column if present in left side for multilevel
    // case if plan is generated such that lower lateral is right child of upper lateral
    for (final VectorWrapper<?> vectorWrapper : left) {
      final MaterializedField leftField = vectorWrapper.getField();
      if (excludedFieldNames.contains(leftField.getName()) && !(leftField.getName().equals(implicitColumn))) {
        continue;
      }
      container.addOrGet(leftField);
    }

    // Setup RightSchema in the outgoing container
    for (final VectorWrapper<?> vectorWrapper : right) {
      MaterializedField rightField = vectorWrapper.getField();
      if (excludedFieldNames.contains(rightField.getName())) {
        if (rightField.getName().equals(implicitColumn)) {
          implicitVector = vectorWrapper.getValueVector();
        }
        continue;
      }

      TypeProtos.MajorType rightFieldType = vectorWrapper.getField().getType();

      // make right input schema optional if we have LEFT join
      if (popConfig.getJoinType() == JoinRelType.LEFT &&
        rightFieldType.getMode() == TypeProtos.DataMode.REQUIRED) {
        final TypeProtos.MajorType outputType =
          Types.overrideMode(rightField.getType(), TypeProtos.DataMode.OPTIONAL);

        // Create the right field with optional type. This will also take care of creating
        // children fields in case of ValueVectors of map type
        rightField = rightField.withType(outputType);
      }
      container.addOrGet(rightField);
    }

    Preconditions.checkState(implicitVector != null,
      "Implicit column vector %s not found in right incoming batch", implicitColumn);

    // Let's build schema for the container
    outputIndex = 0;
    container.setRecordCount(outputIndex);
    container.buildSchema(BatchSchema.SelectionVectorMode.NONE);

    // Setup left vectors
    setupInputOutputVectors(left, 0, leftSchema.getFieldCount(), 0, false);

    // Setup right vectors
    setupInputOutputVectors(right, 0, rightSchema.getFieldCount(),
      leftSchema.getFieldCount(), true);

    logger.debug("Output Schema created {} based on input left schema {} and right schema {}", container.getSchema(),
      leftSchema, rightSchema);
  }

  /**
   * Allocates space for all the vectors in the container.
   */
  private void allocateVectors() {
    // This check is here and will be true only in case of left join where the pending rows from previous left batch is
    // copied to the new output batch. Then same output batch is used to fill remaining memory using new left & right
    // batches.
    if (outputIndex > 0) {
      logger.trace("Allocation is already done for output container vectors since it already holds some record");
      return;
    }

    // Set this as max output rows to be filled in output batch since memory for that many rows are allocated
    if (useMemoryManager) {
      setMaxOutputRowCount(batchMemoryManager.getOutputRowCount());
    }

    for (VectorWrapper<?> w : container) {
      RecordBatchSizer.ColumnSize colSize = batchMemoryManager.getColumnSize(w.getField().getName());
      colSize.allocateVector(w.getValueVector(), maxOutputRowCount);
    }

    logger.debug("Allocator Stats: [AllocatedMem: {}, PeakMem: {}]", container.getAllocator().getAllocatedMemory(),
      container.getAllocator().getPeakMemoryAllocation());
  }

  private boolean setBatchState(IterOutcome outcome) {
    switch(outcome) {
      case EMIT:
      case NONE:
      case NOT_YET:
        state = BatchState.DONE;
        return false;
      default:
    }
    return true;
  }

  /**
   * Creates a map of rowId to number of rows with that rowId in the right
   * incoming batch of Lateral Join. It is expected from UnnestRecordBatch to
   * add an implicit column of IntVectorType with each output row. All the array
   * records belonging to same row in left incoming will have same rowId in the
   * Unnest output batch.
   *
   * @return - map of rowId to rowCount in right batch
   */
  @SuppressWarnings("unused")
  private Map<Integer, Integer> getRowIdToRowCountMapping() {
    final Map<Integer, Integer> indexToFreq = new HashMap<>();
    final IntVector rowIdVector = (IntVector) implicitVector;
    int prevRowId = rowIdVector.getAccessor().get(rightJoinIndex);
    int countRows = 1;
    for (int i=rightJoinIndex + 1; i < right.getRecordCount(); ++i) {
      int currentRowId = rowIdVector.getAccessor().get(i);
      if (prevRowId == currentRowId) {
        ++countRows;
      } else {
        indexToFreq.put(prevRowId, countRows);
        prevRowId = currentRowId;
        countRows = 1;
      }
    }
    indexToFreq.put(prevRowId, countRows);
    return indexToFreq;
  }

  /**
   * Main entry point for producing the output records. This method populates
   * the output batch after cross join of the record in a given left batch at
   * left index and all the corresponding rows in right batches produced by
   * Unnest for current left batch. For each call to this function number of
   * records copied in output batch is limited to maximum rows output batch can
   * hold or the number of rows in right incoming batch
   */
  private void crossJoinAndOutputRecords() {
    final int rightRecordCount = right.getRecordCount();

    // If there is no record in right batch just return current index in output batch
    if (rightRecordCount <= 0) {
      return;
    }

    // Check if right batch is empty since we have to handle left join case
    Preconditions.checkState(rightJoinIndex != -1, "Right batch record count is >0 but index is -1");

    int currentOutIndex = outputIndex;
    // Number of rows that can be copied in output batch
    int maxAvailableRowSlot = maxOutputRowCount - currentOutIndex;

    if (logger.isDebugEnabled()) {
      logger.debug("Producing output for leftIndex: {}, rightIndex: {}, rightRecordCount: {}, outputIndex: {} and " +
        "availableSlotInOutput: {}", leftJoinIndex, rightJoinIndex, rightRecordCount, outputIndex, maxAvailableRowSlot);
      logger.debug("Output Batch stats before copying new data: {}", new RecordBatchSizer(this));
    }

    // Assuming that first vector in right batch is for implicitColumn.
    // get a mapping of number of rows for each rowId present in current right side batch
    //final Map<Integer, Integer> indexToFreq = getRowIdToRowCountMapping();
    final IntVector rowIdVector = (IntVector) implicitVector;
    final int leftRecordCount = left.getRecordCount();

    // we need to have both conditions because in left join case we can exceed the maxAvailableRowSlot before reaching
    // rightBatch end or vice-versa
    while(maxAvailableRowSlot > 0 && rightJoinIndex < rightRecordCount) {
      // Get rowId from current right row
      int currentRowId = rowIdVector.getAccessor().get(rightJoinIndex);
      int leftRowId = leftJoinIndex + 1;
      int numRowsCopied = 0;

      if (currentRowId > leftRecordCount || leftJoinIndex > leftRecordCount) {
        // Not using Preconditions.checkState here since along with condition evaluation there will be cost of boxing
        // the arguments.
        throw new IllegalStateException(String.format("Either RowId in right batch is greater than total records in " +
          "left batch or all rows in left batch is processed but there are still rows in right batch. " +
          "Details[RightRowId: %s, LeftRecordCount: %s, LeftJoinIndex: %s, RightJoinIndex: %s]",
          currentRowId, leftRecordCount, leftJoinIndex, rightJoinIndex));
      }

      if (logger.isTraceEnabled()) {
        // Inside the if condition to eliminate parameter boxing cost
        logger.trace("leftRowId and currentRowId are: {}, {}", leftRowId, currentRowId);
      }

      // If leftRowId matches the rowId in right row then emit left and right row. Increment outputIndex, rightJoinIndex
      // and numRowsCopied. Also set leftMatchFound to true to indicate when to increase leftJoinIndex.
      if (leftRowId == currentRowId) {
        // there is a match
        matchedRecordFound = true;
        numRowsCopied = 1;
        //numRowsCopied = Math.min(indexToFreq.get(currentRowId), maxAvailableRowSlot);
        emitRight(rightJoinIndex, outputIndex, numRowsCopied);
        emitLeft(leftJoinIndex, outputIndex, numRowsCopied);
        outputIndex += numRowsCopied;
        rightJoinIndex += numRowsCopied;
      } else if (leftRowId < currentRowId) {
        // If a matching record for leftRowId was found in right batch in previous iteration, increase the leftJoinIndex
        // and reset the matchedRecordFound flag
        if (matchedRecordFound) {
          matchedRecordFound = false;
          ++leftJoinIndex;
          continue;
        } else { // If no matching row was found in right batch then in case of left join produce left row in output
          // and increase the indexes properly to reflect that
          if (JoinRelType.LEFT == popConfig.getJoinType()) {
            numRowsCopied = 1;
            emitLeft(leftJoinIndex, outputIndex, numRowsCopied);
            ++outputIndex;
          }
          ++leftJoinIndex;
        }
      } else {
        Preconditions.checkState(leftRowId <= currentRowId, "Unexpected case where rowId " +
          "%s in right batch of lateral is smaller than rowId %s in left batch being processed",
          currentRowId, leftRowId);
      }
      // Update the max available rows slot in output batch
      maxAvailableRowSlot -= numRowsCopied;
    }
  }

  /**
   * Gets references of vector's from input and output vector container and create the mapping between them in
   * respective maps. Example: for right incoming batch the references of input vector to corresponding output
   * vector will be stored in {@link LateralJoinBatch#rightInputOutputVector}. This is done here such that during
   * copy we don't have to figure out this mapping everytime for each input and output vector and then do actual copy.
   * There was overhead seen with functions {@link MaterializedField#getValueClass()} and
   * {@link RecordBatch#getValueAccessorById(Class, int...)} since it will be called for each row copy.
   * @param batch - Incoming RecordBatch
   * @param startVectorIndex StartIndex of output vector container
   * @param endVectorIndex endIndex of output vector container
   * @param baseVectorIndex delta to add in startIndex for getting vectors in output container
   * @param isRightBatch is batch passed left or right child
   */
  private void setupInputOutputVectors(RecordBatch batch, int startVectorIndex, int endVectorIndex,
                                       int baseVectorIndex, boolean isRightBatch) {
    // Get the vectors using field index rather than Materialized field since input batch field can be different from
    // output container field in case of Left Join. As we rebuild the right Schema field to be optional for output
    // container.
    int inputIndex = 0;
    final Map<ValueVector, ValueVector> mappingToUse = (isRightBatch) ? rightInputOutputVector : leftInputOutputVector;

    for (int i = startVectorIndex; i < endVectorIndex; ++i) {
      // Get output vector
      final int outputVectorIndex = i + baseVectorIndex;
      final Class<?> outputValueClass = this.getSchema().getColumn(outputVectorIndex).getValueClass();
      final ValueVector outputVector = this.getValueAccessorById(outputValueClass, outputVectorIndex).getValueVector();
      final String outputFieldName = outputVector.getField().getName();

      ValueVector inputVector;
      Class<?> inputValueClass;
      String inputFieldName;
      do {
        // Get input vector
        inputValueClass = batch.getSchema().getColumn(inputIndex).getValueClass();
        inputVector = batch.getValueAccessorById(inputValueClass, inputIndex).getValueVector();
        inputFieldName = inputVector.getField().getName();

        // If implicit column is in left batch then preserve it
        if (inputFieldName.equals(implicitColumn) && !isRightBatch) {
          ++inputIndex;
          break;
        }

        ++inputIndex;
      } while (excludedFieldNames.contains(inputFieldName));

      Preconditions.checkState(outputFieldName.equals(inputFieldName),
        "Non-excluded Input and output container fields are not in same order. " +
          "[Output Schema: %s and Input Schema:%s]", this.getSchema(), batch.getSchema());

      mappingToUse.put(inputVector, outputVector);
    }
  }

  /**
   * Given a vector reference mapping between source and destination vector,
   * copies data from all the source vectors at {@code fromRowIndex} to all the
   * destination vectors in output batch at {@code toRowIndex}.
   *
   * @param fromRowIndex row index of all the vectors in batch to copy data from
   * @param toRowIndex row index of all the vectors in outgoing batch to copy data to
   * @param mapping source record batch holding vectors with data
   * @param numRowsToCopy Number of rows to copy into output batch
   * @param isRightBatch indicates if the fromIndex should also be increased or
   *          not. Since in case of copying data from left vector fromIndex is
   *          constant whereas in case of copying data from right vector
   *          fromIndex will move along with output index.
   */
  private void copyDataToOutputVectors(int fromRowIndex, int toRowIndex, Map<ValueVector, ValueVector> mapping,
                                       int numRowsToCopy, boolean isRightBatch) {
    for (Map.Entry<ValueVector, ValueVector> entry : mapping.entrySet()) {

      if (logger.isTraceEnabled()) {
        // Inside the if condition to eliminate parameter boxing cost
        logger.trace("Copying data from incoming batch vector to outgoing batch vector. [IncomingBatch: " +
          "(RowIndex: {}, ColumnName: {}), OutputBatch: (RowIndex: {}, ColumnName: {}) and Other: (TimeEachValue: {})]",
          fromRowIndex, entry.getKey().getField().getName(), toRowIndex, entry.getValue().getField().getName(),
          numRowsToCopy);
      }

      // Copy data from input vector to output vector for numRowsToCopy times.
      for (int j = 0; j < numRowsToCopy; ++j) {
        entry.getValue().copyEntry(toRowIndex + j, entry.getKey(), (isRightBatch) ? fromRowIndex + j : fromRowIndex);
      }
    }
  }

  /**
   * Copies data at leftIndex from each of vector's in left incoming batch to outIndex at corresponding vectors in
   * outgoing record batch
   * @param leftIndex - index to copy data from left incoming batch vectors
   * @param outIndex - index to copy data to in outgoing batch vectors
   * @param numRowsToCopy - number of rows to copy from source vector to destination vectors
   */
  private void emitLeft(int leftIndex, int outIndex, int numRowsToCopy) {
    if (logger.isTraceEnabled()) {
      // Inside the if condition to eliminate parameter boxing cost
      logger.trace("Copying the left batch data. Details: [leftIndex: {}, outputIndex: {}, numsCopy: {}]",
        leftIndex, outIndex, numRowsToCopy);
    }
    copyDataToOutputVectors(leftIndex, outIndex, leftInputOutputVector, numRowsToCopy, false);
  }

  /**
   * Copies data at rightIndex from each of vector's in right incoming batch to outIndex at corresponding vectors in
   * outgoing record batch
   * @param rightIndex - index to copy data from right incoming batch vectors
   * @param outIndex - index to copy data to in outgoing batch vectors
   * @param numRowsToCopy - number of rows to copy from source vector to destination vectors
   */
  private void emitRight(int rightIndex, int outIndex, int numRowsToCopy) {
    if (logger.isTraceEnabled()) {
      // Inside the if condition to eliminate parameter boxing cost
      logger.trace("Copying the right batch data. Details: [rightIndex: {}, outputIndex: {}, numsCopy: {}]",
        rightIndex, outIndex, numRowsToCopy);
    }
    copyDataToOutputVectors(rightIndex, outIndex, rightInputOutputVector, numRowsToCopy, true);
  }

  /**
   * Used only for testing for cases when multiple output batches are produced for same input set
   * @param outputRowCount - Max rows that output batch can hold
   */
  @VisibleForTesting
  public void setMaxOutputRowCount(int outputRowCount) {
    if (isRecordBatchStatsLoggingEnabled()) {
      RecordBatchStats.logRecordBatchStats(getRecordBatchStatsContext(),
        "Previous OutputRowCount: %d, New OutputRowCount: %d", maxOutputRowCount, outputRowCount);
    }
    maxOutputRowCount = outputRowCount;
  }

  /**
   * Used only for testing to disable output batch calculation using memory manager and instead use the static max
   * value set by {@link LateralJoinBatch#setMaxOutputRowCount(int)}
   * @param useMemoryManager - false - disable memory manager update to take effect, true enable memory manager update
   */
  @VisibleForTesting
  public void setUseMemoryManager(boolean useMemoryManager) {
    this.useMemoryManager = useMemoryManager;
  }

  private boolean isOutgoingBatchFull() {
    return outputIndex >= maxOutputRowCount;
  }

  private void updateMemoryManager(int inputIndex) {
    if (inputIndex == LEFT_INDEX && isNewLeftBatch) {
      // reset state and continue to update
      isNewLeftBatch = false;
    } else if (inputIndex == RIGHT_INDEX && (rightJoinIndex == 0 || rightJoinIndex == -1)) {
      // continue to update
    } else {
      return;
    }

    // For cases where all the previous input were consumed and send with previous output batch. But now we are building
    // a new output batch with new incoming then it will not cause any problem since outputIndex will be 0
    batchMemoryManager.update(inputIndex, outputIndex);

    if (isRecordBatchStatsLoggingEnabled()) {
      RecordBatchIOType type = inputIndex == LEFT_INDEX ? RecordBatchIOType.INPUT_LEFT : RecordBatchIOType.INPUT_RIGHT;
      RecordBatchStats.logRecordBatchStats(type, batchMemoryManager.getRecordBatchSizer(inputIndex),
        getRecordBatchStatsContext());
    }
  }

  private void populateExcludedField(PhysicalOperator lateralPop) {
    excludedFieldNames.add(implicitColumn);
    final List<SchemaPath> excludedCols = ((LateralJoinPOP)lateralPop).getExcludedColumns();
    if (excludedCols != null) {
      for (SchemaPath currentPath : excludedCols) {
        excludedFieldNames.add(currentPath.rootName());
      }
    }
  }

  @Override
  public void dump() {
    logger.error("LateralJoinBatch[container={}, left={}, right={}, leftOutcome={}, rightOutcome={}, leftSchema={}, " +
            "rightSchema={}, outputIndex={}, leftJoinIndex={}, rightJoinIndex={}, hasRemainderForLeftJoin={}]",
        container, left, right, leftUpstream, rightUpstream, leftSchema, rightSchema, outputIndex,
        leftJoinIndex, rightJoinIndex, hasRemainderForLeftJoin);
  }
}
