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
 * WindowFramer implementation that doesn't support the FRAME clause (will
 * assume the default frame). <p>
 * According to the SQL standard, LEAD, LAG, ROW_NUMBER, NTILE and all ranking
 * functions don't support the FRAME clause. This class will handle such
 * functions.
 */
public abstract class NoFrameSupportTemplate implements WindowFramer {
  private static final Logger logger = LoggerFactory.getLogger(NoFrameSupportTemplate.class);

  private VectorContainer container;
  private VectorContainer internal;
  private boolean lagCopiedToInternal;
  private List<WindowDataBatch> batches;
  private int outputCount; // number of rows in currently/last processed batch

  private WindowDataBatch current;

  // true when at least one window function needs to process all batches of a partition before passing any batch downstream
  private boolean requireFullPartition;

  private Partition partition; // current partition being processed

  @Override
  public void setup(List<WindowDataBatch> batches, VectorContainer container, OperatorContext oContext,
                    boolean requireFullPartition, WindowPOP popConfig) throws SchemaChangeException {
    this.container = container;
    this.batches = batches;

    internal = new VectorContainer(oContext);
    allocateInternal();
    lagCopiedToInternal = false;

    outputCount = 0;
    partition = null;

    this.requireFullPartition = requireFullPartition;
  }

  private void allocateInternal() {
    for (VectorWrapper<?> w : container) {
      ValueVector vv = internal.addOrGet(w.getField());
      vv.allocateNew();
    }
  }

  /**
   * Processes all rows of the first batch.
   */
  @Override
  public void doWork() {
    int currentRow = 0;
    current = batches.get(0);
    outputCount = current.getRecordCount();

    while (currentRow < outputCount) {
      if (partition != null) {
        assert currentRow == 0 : "pending windows are only expected at the start of the batch";

        // we have a pending window we need to handle from a previous call to doWork()
        logger.trace("we have a pending partition {}", partition);

        if (!requireFullPartition) {
          // we didn't compute the whole partition length in the previous partition, we need to update the length now
          updatePartitionSize(partition, currentRow);
        }
      } else {
        newPartition(current, currentRow);
      }

      try {
        currentRow = processPartition(currentRow);
      } catch (SchemaChangeException e) {
        throw AbstractRecordBatch.schemaChangeException(e, "Window", logger);
      }
      if (partition.isDone()) {
        cleanPartition();
      }
    }
  }

  private void newPartition(WindowDataBatch current, int currentRow) {
    partition = new Partition();
    updatePartitionSize(partition, currentRow);
    try {
      setupPartition(current, container);
    } catch (SchemaChangeException e) {
      throw AbstractRecordBatch.schemaChangeException(e, "Window", logger);
    }
  }

  private void cleanPartition() {
    partition = null;
    try {
      resetValues();
    } catch (SchemaChangeException e) {
      throw new UnsupportedOperationException(e);
    }
    for (VectorWrapper<?> vw : internal) {
      if ((vw.getValueVector() instanceof BaseDataValueVector)) {
        ((BaseDataValueVector) vw.getValueVector()).reset();
      }
    }
    lagCopiedToInternal = false;
  }

  /**
   * Process all rows (computes and writes function values) of current batch
   * that are part of current partition.
   *
   * @param currentRow
   *          first unprocessed row
   * @return index of next unprocessed row
   * @throws SchemaChangeException
   */
  private int processPartition(int currentRow) throws SchemaChangeException {
    logger.trace("process partition {}, currentRow: {}, outputCount: {}", partition, currentRow, outputCount);

    setupCopyNext(current, container);
    copyPrevFromInternal();

    // copy remaining from current
    setupCopyPrev(current, container);

    int row = currentRow;

    // process all rows except the last one of the batch/partition
    while (row < outputCount && !partition.isDone()) {
      if (row != currentRow) { // this is not the first row of the partition
        copyPrev(row - 1, row);
      }

      processRow(row);

      if (row < outputCount - 1 && !partition.isDone()) {
        copyNext(row + 1, row);
      }

      row++;
    }

    // if we didn't reach the end of partition yet
    if (!partition.isDone() && batches.size() > 1) {
      // copy next value onto the current one
      setupCopyNext(batches.get(1), container);
      copyNext(0, row - 1);

      copyPrevToInternal(current, row);
    }

    return row;
  }

  private void copyPrevToInternal(VectorAccessible current, int row) {
    logger.trace("copying {} into internal", row - 1);
    try {
      setupCopyPrev(current, internal);
      copyPrev(row - 1, 0);
    } catch (SchemaChangeException e) {
      throw new UnsupportedOperationException(e);
    }
    lagCopiedToInternal = true;
  }

  private void copyPrevFromInternal() {
    if (lagCopiedToInternal) {
      try {
        setupCopyFromInternal(internal, container);
        copyFromInternal(0, 0);
      } catch (SchemaChangeException e) {
        throw new UnsupportedOperationException(e);
      }
      lagCopiedToInternal = false;
    }
  }

  private void processRow(int row) throws SchemaChangeException {
    if (partition.isFrameDone()) {
      // because all peer rows share the same frame, we only need to compute and aggregate the frame once
      long peers = countPeers(row);
      partition.newFrame(peers);
    }

    outputRow(row, partition);
    partition.rowAggregated();
  }

  /**
   * updates partition's length after computing the number of rows for the current the partition starting at the specified
   * row of the first batch. If !requiresFullPartition, this method will only count the rows in the current batch
   */
  private void updatePartitionSize(Partition partition, int start) {
    logger.trace("compute partition size starting from {} on {} batches", start, batches.size());

    long length = 0;
    boolean lastBatch = false;
    int row = start;

    // count all rows that are in the same partition of start
    // keep increasing length until we find first row of next partition or we reach the very last batch

    outer:
    for (WindowDataBatch batch : batches) {
      int recordCount = batch.getRecordCount();

      // check first container from start row, and subsequent containers from first row
      for (; row < recordCount; row++, length++) {
        try {
          if (!isSamePartition(start, current, row, batch)) {
            break outer;
          }
        } catch (SchemaChangeException e) {
          throw new UnsupportedOperationException(e);
        }
      }

      if (!requireFullPartition) {
        // we are only interested in the first batch's records
        break;
      }

      row = 0;
    }

    try {
      if (!requireFullPartition) {
        // this is the last batch of current partition if
        lastBatch = row < outputCount                           // partition ends before the end of the batch
          || batches.size() == 1                                // it's the last available batch
          || !isSamePartition(start, current, 0, batches.get(1)); // next batch contains a different partition
      }
    } catch (SchemaChangeException e) {
      throw new UnsupportedOperationException(e);
    }

    partition.updateLength(length, !(requireFullPartition || lastBatch));
  }

  /**
   * Count number of peer rows for current row
   * @param start starting row of the current frame
   * @return num peer rows for current row
   * @throws SchemaChangeException
   */
  private long countPeers(int start) throws SchemaChangeException {
    long length = 0;

    // a single frame can include rows from multiple batches
    // start processing first batch and, if necessary, move to next batches
    for (WindowDataBatch batch : batches) {
      int recordCount = batch.getRecordCount();

      // for every remaining row in the partition, count it if it's a peer row
      for (int row = (batch == current) ? start : 0; row < recordCount; row++, length++) {
        if (!isPeer(start, current, row, batch)) {
          break;
        }
      }
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
        + ", requireFullPartition=" + requireFullPartition
        + ", partition=" + partition
        + "]";
  }

  /**
   * Called once for each row after we evaluate all peer rows. Used to write a
   * value in the row
   *
   * @param outIndex
   *          index of row
   * @param partition
   *          object used by "computed" window functions
   */
  public abstract void outputRow(@Named("outIndex") int outIndex,
                                 @Named("partition") Partition partition)
                       throws SchemaChangeException;

  /**
   * Called once per partition, before processing the partition. Used to setup read/write vectors
   * @param incoming batch we will read from
   * @param outgoing batch we will be writing to
   *
   * @throws SchemaChangeException
   */
  public abstract void setupPartition(@Named("incoming") WindowDataBatch incoming,
                                      @Named("outgoing") VectorAccessible outgoing)
                       throws SchemaChangeException;

  /**
   * Copies value(s) from inIndex row to outIndex row. Mostly used by LEAD.
   * inIndex always points to the row next to outIndex
   *
   * @param inIndex
   *          source row of the copy
   * @param outIndex
   *          destination row of the copy.
   */
  public abstract void copyNext(@Named("inIndex") int inIndex,
                                @Named("outIndex") int outIndex)
                       throws SchemaChangeException;
  public abstract void setupCopyNext(@Named("incoming") VectorAccessible incoming,
                                     @Named("outgoing") VectorAccessible outgoing)
                       throws SchemaChangeException;

  /**
   * Copies value(s) from inIndex row to outIndex row. Mostly used by LAG.
   * inIndex always points to the previous row
   *
   * @param inIndex
   *          source row of the copy
   * @param outIndex
   *          destination row of the copy.
   */
  public abstract void copyPrev(@Named("inIndex") int inIndex,
                                @Named("outIndex") int outIndex)
                       throws SchemaChangeException;
  public abstract void setupCopyPrev(@Named("incoming") VectorAccessible incoming,
                                     @Named("outgoing") VectorAccessible outgoing)
                       throws SchemaChangeException;

  public abstract void copyFromInternal(@Named("inIndex") int inIndex,
                                        @Named("outIndex") int outIndex)
                       throws SchemaChangeException;
  public abstract void setupCopyFromInternal(@Named("incoming") VectorAccessible incoming,
                                             @Named("outgoing") VectorAccessible outgoing)
                       throws SchemaChangeException;

  /**
   * Reset all window functions
   */
  public abstract boolean resetValues() throws SchemaChangeException;

  /**
   * Compares two rows from different batches (can be the same), if they have the same value for the partition by
   * expression
   * @param b1Index index of first row
   * @param b1 batch for first row
   * @param b2Index index of second row
   * @param b2 batch for second row
   * @return true if the rows are in the same partition
   */
  @Override
  public abstract boolean isSamePartition(@Named("b1Index") int b1Index,
                                          @Named("b1") VectorAccessible b1,
                                          @Named("b2Index") int b2Index,
                                          @Named("b2") VectorAccessible b2)
                          throws SchemaChangeException;

  /**
   * Compares two rows from different batches (can be the same), if they have the same value for the order by
   * expression
   * @param b1Index index of first row
   * @param b1 batch for first row
   * @param b2Index index of second row
   * @param b2 batch for second row
   * @return true if the rows are in the same partition
   */
  @Override
  public abstract boolean isPeer(@Named("b1Index") int b1Index,
                                 @Named("b1") VectorAccessible b1,
                                 @Named("b2Index") int b2Index,
                                 @Named("b2") VectorAccessible b2)
                          throws SchemaChangeException;
}
