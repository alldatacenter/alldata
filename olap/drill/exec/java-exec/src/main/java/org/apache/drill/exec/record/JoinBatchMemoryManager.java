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
package org.apache.drill.exec.record;

import java.util.Set;

public class JoinBatchMemoryManager extends RecordBatchMemoryManager {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JoinBatchMemoryManager.class);

  private int rowWidth[];
  private RecordBatch recordBatch[];
  private Set<String> columnsToExclude;

  private static final int numInputs = 2;
  public static final int LEFT_INDEX = 0;
  public static final int RIGHT_INDEX = 1;

  public JoinBatchMemoryManager(int outputBatchSize, RecordBatch leftBatch,
                                RecordBatch rightBatch, Set<String> excludedColumns) {
    super(numInputs, outputBatchSize);
    recordBatch = new RecordBatch[numInputs];
    recordBatch[LEFT_INDEX] = leftBatch;
    recordBatch[RIGHT_INDEX] = rightBatch;
    rowWidth = new int[numInputs];
    this.columnsToExclude = excludedColumns;
  }

  /**
   * Update the memory manager parameters based on the new incoming batch
   *
   * Notice three (possibly) different "row counts" for the outgoing batches:
   *
   *  1. The rowCount that the current outgoing batch was allocated with (always a power of 2; e.g. 8192)
   *  2. The new rowCount computed based on the newly seen input rows (always a power of 2); may be bigger than (1) if the
   *     new input rows are much smaller than before (e.g. 16384), or smaller (e.g. 4096) if the new rows are much wider.
   *     Subsequent outgoing batches would be allocated based on this (2) new rowCount.
   *  3. The target rowCount for the current outgoing batch. While initially (1), it may be resized down if the new rows
   *     are getting bigger. In any case it won't be resized above (1) (to avoid IOOB) or below the current number of rows
   *     in that batch (i.e., outputPosition). (Need not be a power of two; e.g., 7983).
   *
   *  After every call to update() while the outgoing batch is active, the current target should be updated with (3) by
   *  calling getCurrentOutgoingMaxRowCount() .
   *
   *  Comment: The "power of 2" in the above (1) and (2) is actually "power of 2 minus 1" (e.g. 65535, or 8191) in order
   *  to avoid memory waste in case offset vectors are used (see DRILL-5446)
   *
   * @param inputIndex  Left (0) or Right (1)
   * @param outputPosition  Position (i.e. number of inserted rows) in the current output batch
   * @param useAggregate If true, compute using average row width (else based on allocated sizes)
   */
  private void updateInternal(int inputIndex, int outputPosition,  boolean useAggregate) {
    updateIncomingStats(inputIndex);
    rowWidth[inputIndex] = useAggregate ? (int) getAvgInputRowWidth(inputIndex) : getRecordBatchSizer(inputIndex).getRowAllocWidth();

    // Reduce the width of excluded columns from actual rowWidth
    for (String columnName : columnsToExclude) {
      final RecordBatchSizer.ColumnSize currentColSizer = getColumnSize(inputIndex, columnName);
      if (currentColSizer == null) {
        continue;
      }
      rowWidth[inputIndex] -= currentColSizer.getAllocSizePerEntry();
    }

    // Get final net outgoing row width after reducing the excluded columns width
    int newOutgoingRowWidth = rowWidth[LEFT_INDEX] + rowWidth[RIGHT_INDEX];

    // If outgoing row width is 0 or there is no change in outgoing row width, just return.
    // This is possible for empty batches or
    // when first set of batches come with OK_NEW_SCHEMA and no data.
    if (newOutgoingRowWidth == 0 || newOutgoingRowWidth == getOutgoingRowWidth()) {
      return;
    }

    // Adjust for the current batch.
    // calculate memory used so far based on previous outgoing row width and how many rows we already processed.
    final int previousOutgoingWidth = getOutgoingRowWidth();
    final long memoryUsed = outputPosition * previousOutgoingWidth;

    final int configOutputBatchSize = getOutputBatchSize();
    // This is the remaining memory.
    final long remainingMemory = Math.max(configOutputBatchSize - memoryUsed, 0);

    // These are number of rows we can fit in remaining memory based on new outgoing row width.
    final int numOutputRowsRemaining = RecordBatchSizer.safeDivide(remainingMemory, newOutgoingRowWidth);

    final int currentOutputBatchRowCount = getOutputRowCount();

    // update the value to be used for next batch(es)
    setOutputRowCount(configOutputBatchSize, newOutgoingRowWidth);

    // set the new row width
    setOutgoingRowWidth(newOutgoingRowWidth);

    int newOutputRowCount = getOutputRowCount();

    if ( currentOutputBatchRowCount != newOutputRowCount ) {
      logger.debug("Memory manager update changed the output row count from {} to {}",currentOutputBatchRowCount,newOutputRowCount);
    }

    // The current outgoing batch target count (i.e., max number of rows to put there) is modified to be the current number of rows there
    // plus as many of the future new rows that would fit in the remaining memory (e.g., if the new rows are wider, fewer would fit), but
    // in any case no larger than the size the batch was allocated for (to avoid IOOB on the allocated vectors)
    setCurrentOutgoingMaxRowCount(Math.min(currentOutputBatchRowCount, outputPosition + numOutputRowsRemaining ));
  }

  /**
   * Update the memory manager parameters based on the new incoming batch
   *
   * @param inputIndex Left (0) or Right (1)
   * @param outputPosition Position (i.e. number of inserted rows) in the output batch
   * @param useAggregate Compute using average row width (else based on allocated sizes)
   */
  @Override
  public void update(int inputIndex, int outputPosition, boolean useAggregate) {
    setRecordBatchSizer(inputIndex, new RecordBatchSizer(recordBatch[inputIndex]));
    updateInternal(inputIndex, outputPosition, useAggregate);
  }

  /**
   * Update the memory manager parameters based on the new incoming batch (based on allocated sizes, not average row size)
   *
   * @param inputIndex Left (0) or Right (1)
   * @param outputPosition Position (i.e. number of inserted rows) in the output batch
   */
  @Override
  public void update(int inputIndex, int outputPosition) {
    update(inputIndex, outputPosition, false);
  }

  /**
   * Update the memory manager parameters based on the given (incoming) batch
   *
   * @param batch Update based on the data in this batch
   * @param inputIndex Left (0) or Right (1)
   * @param outputPosition Position (i.e. number of inserted rows) in the output batch
   * @param useAggregate Compute using average row width (else based on allocated sizes)
   */
  @Override
  public void update(RecordBatch batch, int inputIndex, int outputPosition, boolean useAggregate) {
    setRecordBatchSizer(inputIndex, new RecordBatchSizer(batch));
    updateInternal(inputIndex, outputPosition, useAggregate);
  }

  /**
   * Update the memory manager parameters based on the given (incoming) batch (based on allocated sizes, not average row size)
   *
   * @param batch Update based on the data in this batch
   * @param inputIndex Left (0) or Right (1)
   * @param outputPosition Position (i.e. number of inserted rows) in the output batch
   */
  @Override
  public void update(RecordBatch batch, int inputIndex, int outputPosition) {
    update(batch, inputIndex, outputPosition, false);
  }
}
