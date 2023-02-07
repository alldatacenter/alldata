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
package org.apache.drill.exec.physical.impl.window;

import org.apache.drill.common.exceptions.DrillException;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.config.WindowPOP;
import org.apache.drill.exec.record.AbstractRecordBatch;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.vector.BaseDataValueVector;
import org.apache.drill.exec.vector.ValueVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.List;


/**
 * WindowFramer implementation that supports the FRAME clause. <br>
 * According to the SQL specification, FIRST_VALUE, LAST_VALUE and all aggregate
 * functions support the FRAME clause. This class will handle such functions
 * even if the FRAME clause is not present.
 */
public abstract class FrameSupportTemplate implements WindowFramer {

  private static final Logger logger = LoggerFactory.getLogger(FrameSupportTemplate.class);

  private VectorContainer container;
  private VectorContainer internal;
  private List<WindowDataBatch> batches;
  private int outputCount; // number of rows in currently/last processed batch

  private WindowDataBatch current;

  private int frameLastRow;

  // true when at least one window function needs to process all batches of a partition before passing any batch downstream
  private boolean requireFullPartition;

  private long remainingRows; // num unprocessed rows in current partition
  private long remainingPeers; // num unprocessed peer rows in current frame
  private boolean partialPartition; // true if we remainingRows only account for the current batch and more batches are expected for the current partition

  private WindowPOP popConfig;

  @Override
  public void setup(final List<WindowDataBatch> batches, final VectorContainer container, final OperatorContext oContext,
                    final boolean requireFullPartition, final WindowPOP popConfig) throws SchemaChangeException {
    this.container = container;
    this.batches = batches;

    internal = new VectorContainer(oContext);
    allocateInternal();

    outputCount = 0;

    this.requireFullPartition = requireFullPartition;
    this.popConfig = popConfig;
  }

  private void allocateInternal() {
    for (VectorWrapper<?> w : container) {
      ValueVector vv = internal.addOrGet(w.getField());
      vv.allocateNew();
    }
  }

  private boolean isPartitionDone() {
    return !partialPartition && remainingRows == 0;
  }

  /**
   * processes all rows of the first batch.
   */
  @Override
  public void doWork() throws SchemaChangeException {
    int currentRow = 0;

    this.current = batches.get(0);

    setupSaveFirstValue(current, internal);

    outputCount = current.getRecordCount();

    while (currentRow < outputCount) {
      if (!isPartitionDone()) {
        // we have a pending partition we need to handle from a previous call to doWork()
        assert currentRow == 0 : "pending partitions are only expected at the start of the batch";
        logger.trace("we have a pending partition {}", remainingRows);

        if (!requireFullPartition) {
          // we didn't compute the whole partition length in the previous partition, we need to update the length now
          updatePartitionSize(currentRow);
        }
      } else {
        newPartition(current, currentRow);
      }

      currentRow = processPartition(currentRow);
      if (isPartitionDone()) {
        reset();
      }
    }
  }

  private void newPartition(final WindowDataBatch current, final int currentRow) throws SchemaChangeException {
    remainingRows = 0;
    remainingPeers = 0;
    updatePartitionSize(currentRow);

    setupPartition(current, container);
    saveFirstValue(currentRow);
  }

  private void reset() {
    resetValues();
    for (VectorWrapper<?> vw : internal) {
      if ((vw.getValueVector() instanceof BaseDataValueVector)) {
        ((BaseDataValueVector) vw.getValueVector()).reset();
      }
    }
  }

  /**
   * process all rows (computes and writes aggregation values) of current batch that are part of current partition.
   * @param currentRow first unprocessed row
   * @return index of next unprocessed row
   * @throws DrillException if it can't write into the container
   */
  private int processPartition(final int currentRow) throws SchemaChangeException {
    logger.trace("{} rows remaining to process, currentRow: {}, outputCount: {}", remainingRows, currentRow, outputCount);

    setupWriteFirstValue(internal, container);

    if (popConfig.isFrameUnitsRows()) {
      return processROWS(currentRow);
    } else {
      return processRANGE(currentRow);
    }
  }

  private int processROWS(int row) throws SchemaChangeException {
    //TODO (DRILL-4413) we only need to call these once per batch
    setupEvaluatePeer(current, container);
    setupReadLastValue(current, container);

    while (row < outputCount && !isPartitionDone()) {
      logger.trace("aggregating row {}", row);
      evaluatePeer(row);

      outputRow(row);
      writeLastValue(row, row);

      remainingRows--;
      row++;
    }

    return row;
  }

  private int processRANGE(int row) throws SchemaChangeException {
    while (row < outputCount && !isPartitionDone()) {
      if (remainingPeers == 0) {
        // because all peer rows share the same frame, we only need to compute and aggregate the frame once
        if (popConfig.getStart().isCurrent()) {
          reset();
          saveFirstValue(row);
        }

        remainingPeers = aggregatePeers(row);
      }

      outputRow(row);
      writeLastValue(frameLastRow, row);

      remainingRows--;
      remainingPeers--;
      row++;
    }

    return row;
  }

  /**
   * Updates partition's length after computing the number of rows for the
   * current the partition starting at the specified row of the first batch. If
   * !requiresFullPartition, this method will only count the rows in the current
   * batch
   */
  private void updatePartitionSize(final int start) {
    logger.trace("compute partition size starting from {} on {} batches", start, batches.size());

    long length = 0;
    int row = start;

    // count all rows that are in the same partition of start
    // keep increasing length until we find first row of next partition or we reach the very last batch

    outer:
    for (WindowDataBatch batch : batches) {
      final int recordCount = batch.getRecordCount();

      // check first container from start row, and subsequent containers from first row
      for (; row < recordCount; row++, length++) {
        if (!isSamePartition(start, current, row, batch)) {
          break outer;
        }
      }

      if (!requireFullPartition) {
        // we are only interested in the first batch's records
        break;
      }

      row = 0;
    }

    if (!requireFullPartition) {
      // this is the last batch of current partition if
      boolean lastBatch = row < outputCount                     // partition ends before the end of the batch
        || batches.size() == 1                                  // it's the last available batch
        || !isSamePartition(start, current, 0, batches.get(1)); // next batch contains a different partition

      partialPartition = !lastBatch;
    } else {
      partialPartition = false;
    }

    remainingRows += length;
  }

  /**
   * Aggregates all peer rows of current row
   * @param start starting row of the current frame
   * @return num peer rows for current row
   */
  private long aggregatePeers(final int start) {
    logger.trace("aggregating rows starting from {}", start);

    final boolean unboundedFollowing = popConfig.getEnd().isUnbounded();
    VectorAccessible last = current;
    long length = 0;

    // a single frame can include rows from multiple batches
    // start processing first batch and, if necessary, move to next batches
    for (WindowDataBatch batch : batches) {
      try {
        setupEvaluatePeer(batch, container);
      } catch (SchemaChangeException e) {
        throw AbstractRecordBatch.schemaChangeException(e, "Window", logger);
      }
      final int recordCount = batch.getRecordCount();

      // for every remaining row in the partition, count it if it's a peer row
      for (int row = (batch == current) ? start : 0; row < recordCount; row++, length++) {
        if (unboundedFollowing) {
          if (length >= remainingRows) {
            break;
          }
        } else {
          if (!isPeer(start, current, row, batch)) {
            break;
          }
        }

        evaluatePeer(row);
        last = batch;
        frameLastRow = row;
      }
    }

    try {
      setupReadLastValue(last, container);
    } catch (SchemaChangeException e) {
      throw AbstractRecordBatch.schemaChangeException(e, "Window", logger);
    }

    return length;
  }

  @Override
  public int getOutputCount() {
    return outputCount;
  }

  // we need this abstract method for code generation
  @Override
  public void cleanup() {
    logger.trace("clearing internal");
    internal.clear();
  }

  @Override
  public String toString() {
    return "FrameSupportTemplate[internal=" + internal
        + ", outputCount=" + outputCount
        + ", current=" + current
        + ", frameLastRow=" + frameLastRow
        + ", remainingRows=" + remainingRows
        + ", partialPartition=" + partialPartition
        + "]";
  }

  /**
   * called once for each peer row of the current frame.
   * @param index of row to aggregate
   */
  public abstract void evaluatePeer(@Named("index") int index);
  public abstract void setupEvaluatePeer(@Named("incoming") VectorAccessible incoming, @Named("outgoing") VectorAccessible outgoing) throws SchemaChangeException;

  public abstract void setupReadLastValue(@Named("incoming") VectorAccessible incoming, @Named("outgoing") VectorAccessible outgoing) throws SchemaChangeException;
  public abstract void writeLastValue(@Named("index") int index, @Named("outIndex") int outIndex);

  public abstract void setupSaveFirstValue(@Named("incoming") VectorAccessible incoming, @Named("outgoing") VectorAccessible outgoing) throws SchemaChangeException;
  public abstract void saveFirstValue(@Named("index") int index);
  public abstract void setupWriteFirstValue(@Named("incoming") VectorAccessible incoming, @Named("outgoing") VectorAccessible outgoing);

  /**
   * called once for each row after we evaluate all peer rows. Used to write a value in the row
   *
   * @param outIndex index of row
   */
  public abstract void outputRow(@Named("outIndex") int outIndex);

  /**
   * Called once per partition, before processing the partition. Used to setup read/write vectors
   * @param incoming batch we will read from
   * @param outgoing batch we will be writing to
   *
   * @throws SchemaChangeException
   */
  public abstract void setupPartition(@Named("incoming") WindowDataBatch incoming,
                                      @Named("outgoing") VectorAccessible outgoing) throws SchemaChangeException;

  /**
   * reset all window functions
   */
  public abstract boolean resetValues();

  /**
   * compares two rows from different batches (can be the same), if they have the same value for the partition by
   * expression
   * @param b1Index index of first row
   * @param b1 batch for first row
   * @param b2Index index of second row
   * @param b2 batch for second row
   * @return true if the rows are in the same partition
   */
  @Override
  public abstract boolean isSamePartition(@Named("b1Index") int b1Index, @Named("b1") VectorAccessible b1,
                                          @Named("b2Index") int b2Index, @Named("b2") VectorAccessible b2);

  /**
   * compares two rows from different batches (can be the same), if they have the same value for the order by
   * expression
   * @param b1Index index of first row
   * @param b1 batch for first row
   * @param b2Index index of second row
   * @param b2 batch for second row
   * @return true if the rows are in the same partition
   */
  @Override
  public abstract boolean isPeer(@Named("b1Index") int b1Index, @Named("b1") VectorAccessible b1,
                                 @Named("b2Index") int b2Index, @Named("b2") VectorAccessible b2);
}
