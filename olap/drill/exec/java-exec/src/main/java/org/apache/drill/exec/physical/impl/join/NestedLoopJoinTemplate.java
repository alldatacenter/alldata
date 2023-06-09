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

import org.apache.calcite.rel.core.JoinRelType;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.record.ExpandableHyperContainer;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.util.record.RecordBatchStats;
import org.apache.drill.exec.util.record.RecordBatchStats.RecordBatchIOType;
import javax.inject.Named;
import java.util.LinkedList;
import java.util.List;

import static org.apache.drill.exec.record.JoinBatchMemoryManager.LEFT_INDEX;

/*
 * Template class that combined with the runtime generated source implements the NestedLoopJoin interface. This
 * class contains the main nested loop join logic.
 */
public abstract class NestedLoopJoinTemplate implements NestedLoopJoin {

  // Current left input batch being processed
  private RecordBatch left;

  // Record count of the left batch currently being processed
  private int leftRecordCount;

  // List of record counts per batch in the hyper container
  private List<Integer> rightCounts;

  // Output batch
  private NestedLoopJoinBatch outgoing;

  // Iteration status tracker
  private final IterationStatusTracker tracker = new IterationStatusTracker();

  private int targetOutputRecords;

  /**
   * Method initializes necessary state and invokes the doSetup() to set the
   * input and output value vector references.
   *
   * @param context Fragment context
   * @param left Current left input batch being processed
   * @param rightContainer Hyper container
   * @param rightCounts Counts for each right container
   * @param outgoing Output batch
   */
  @Override
  public void setupNestedLoopJoin(FragmentContext context,
                                  RecordBatch left,
                                  ExpandableHyperContainer rightContainer,
                                  LinkedList<Integer> rightCounts,
                                  NestedLoopJoinBatch outgoing) {
    this.left = left;
    this.leftRecordCount = left.getRecordCount();
    this.rightCounts = rightCounts;
    this.outgoing = outgoing;
    doSetup(context, rightContainer, left, outgoing);
  }

  @Override
  public void setTargetOutputCount(int targetOutputRecords) {
    this.targetOutputRecords = targetOutputRecords;
  }

  /**
   * Main entry point for producing the output records. Thin wrapper around populateOutgoingBatch(), this method
   * controls which left batch we are processing and fetches the next left input batch once we exhaust the current one.
   *
   * @param joinType join type (INNER ot LEFT)
   * @return the number of records produced in the output batch
   */
  @Override
  public int outputRecords(JoinRelType joinType) {
    int outputIndex = 0;
    while (leftRecordCount != 0) {
      outputIndex = populateOutgoingBatch(joinType, outputIndex);
      if (outputIndex >= targetOutputRecords) {
        break;
      }
      // reset state and get next left batch
      resetAndGetNextLeft(outputIndex);
    }
    return outputIndex;
  }

  /**
   * This method is the core of the nested loop join.For each left batch record looks for matching record
   * from the list of right batches. Match is checked by calling {@link #doEval(int, int, int)} method.
   * If matching record is found both left and right records are written into output batch,
   * otherwise if join type is LEFT, than only left record is written, right batch record values will be null.
   *
   * @param joinType join type (INNER or LEFT)
   * @param outputIndex index to start emitting records at
   * @return final outputIndex after producing records in the output batch
   */
  private int populateOutgoingBatch(JoinRelType joinType, int outputIndex) {
    // copy index and match counters as local variables to speed up processing
    int nextRightBatchToProcess = tracker.getNextRightBatchToProcess();
    int nextRightRecordToProcess = tracker.getNextRightRecordToProcess();
    int nextLeftRecordToProcess = tracker.getNextLeftRecordToProcess();
    boolean rightRecordMatched = tracker.isRightRecordMatched();

    outer:
    // for every record in the left batch
    for (; nextLeftRecordToProcess < leftRecordCount; nextLeftRecordToProcess++) {
      // for every batch on the right
      for (; nextRightBatchToProcess < rightCounts.size(); nextRightBatchToProcess++) {
        int rightRecordCount = rightCounts.get(nextRightBatchToProcess);
        // Since right container is a hyper container, in doEval generated code it expects the
        // batch index in the 2 MSBytes of the index variable. See DRILL-6128 for details
        final int currentRightBatchIndex = nextRightBatchToProcess << 16;
        // for every record in right batch
        for (; nextRightRecordToProcess < rightRecordCount; nextRightRecordToProcess++) {
          if (doEval(nextLeftRecordToProcess, currentRightBatchIndex, nextRightRecordToProcess)) {
            // project records from the left and right batches
            emitLeft(nextLeftRecordToProcess, outputIndex);
            emitRight(nextRightBatchToProcess, nextRightRecordToProcess, outputIndex);
            outputIndex++;
            rightRecordMatched = true;

            if (outputIndex >= targetOutputRecords) {
              nextRightRecordToProcess++;

              // no more space left in the batch, stop processing
              break outer;
            }
          }
        }
        nextRightRecordToProcess = 0;
      }
      nextRightBatchToProcess = 0;
      if (joinType == JoinRelType.LEFT && !rightRecordMatched) {
        // project records from the left side only, records from right will be null
        emitLeft(nextLeftRecordToProcess, outputIndex);
        outputIndex++;
        if (outputIndex >= targetOutputRecords) {
          nextLeftRecordToProcess++;

          // no more space left in the batch, stop processing
          break;
        }
      } else {
        // reset match indicator if matching record was found
        rightRecordMatched = false;
      }
    }

    // update iteration status tracker with actual index and match counters
    tracker.update(nextRightBatchToProcess, nextRightRecordToProcess, nextLeftRecordToProcess, rightRecordMatched);
    return outputIndex;
  }

  /**
   * Utility method to clear the memory in the left input batch once we have completed processing it.
   * Resets some internal state which indicates the next records to process in the left and right batches,
   * also fetches the next left input batch.
   */
  private void resetAndGetNextLeft(int outputIndex) {
    for (VectorWrapper<?> vw : left) {
      vw.getValueVector().clear();
    }
    tracker.reset();
    RecordBatch.IterOutcome leftOutcome = outgoing.next(NestedLoopJoinBatch.LEFT_INPUT, left);
    switch (leftOutcome) {
      case OK_NEW_SCHEMA:
        throw new DrillRuntimeException("Nested loop join does not handle schema change. Schema change" +
            " found on the left side of NLJ.");
      case NONE:
      case NOT_YET:
        leftRecordCount = 0;
        break;
      case OK:
        outgoing.getBatchMemoryManager().update(left, LEFT_INDEX,outputIndex);
        setTargetOutputCount(outgoing.getBatchMemoryManager().getCurrentOutgoingMaxRowCount()); // calculated by update()
        RecordBatchStats.logRecordBatchStats(RecordBatchIOType.INPUT_LEFT,
          outgoing.getBatchMemoryManager().getRecordBatchSizer(LEFT_INDEX),
          outgoing.getRecordBatchStatsContext());
        leftRecordCount = left.getRecordCount();
        break;
      default:
    }
  }

  @Override
  public abstract void doSetup(@Named("context") FragmentContext context,
                               @Named("rightContainer") VectorContainer rightContainer,
                               @Named("leftBatch") RecordBatch leftBatch,
                               @Named("outgoing") RecordBatch outgoing);

  @Override
  public abstract void emitRight(@Named("batchIndex") int batchIndex,
                                 @Named("recordIndexWithinBatch") int recordIndexWithinBatch,
                                 @Named("outIndex") int outIndex);

  @Override
  public abstract void emitLeft(@Named("leftIndex") int leftIndex,
                                @Named("outIndex") int outIndex);

  protected abstract boolean doEval(@Named("leftIndex") int leftIndex,
                                    @Named("rightBatchIndex") int batchIndex,
                                    @Named("rightRecordIndexWithinBatch") int recordIndexWithinBatch);

  /**
   * Helper class to track position of left and record batches during iteration
   * and match status of record from the right batch.
   */
  private static class IterationStatusTracker {
    // Next right batch to process
    private int nextRightBatchToProcess;
    // Next record in the current right batch to process
    private int nextRightRecordToProcess;
    // Next record in the left batch to process
    private int nextLeftRecordToProcess;
    // Flag to indicate if record from the left found matching record from the right, applicable during left join
    private boolean rightRecordMatched;

    int getNextRightBatchToProcess() {
      return nextRightBatchToProcess;
    }

    boolean isRightRecordMatched() {
      return rightRecordMatched;
    }

    int getNextLeftRecordToProcess() {
      return nextLeftRecordToProcess;
    }

    int getNextRightRecordToProcess() {
      return nextRightRecordToProcess;
    }

    void update(int nextRightBatchToProcess,
                int nextRightRecordToProcess,
                int nextLeftRecordToProcess,
                boolean rightRecordMatchFound) {
      this.nextRightBatchToProcess = nextRightBatchToProcess;
      this.nextRightRecordToProcess = nextRightRecordToProcess;
      this.nextLeftRecordToProcess = nextLeftRecordToProcess;
      this.rightRecordMatched = rightRecordMatchFound;
    }

    void reset() {
      nextRightBatchToProcess = nextRightRecordToProcess = nextLeftRecordToProcess = 0;
      rightRecordMatched = false;
    }

  }
}
