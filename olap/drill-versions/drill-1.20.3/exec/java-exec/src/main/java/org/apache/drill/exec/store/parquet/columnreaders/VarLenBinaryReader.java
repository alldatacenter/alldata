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
package org.apache.drill.exec.store.parquet.columnreaders;

import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.store.parquet.columnreaders.batchsizing.RecordBatchOverflow;
import org.apache.drill.exec.store.parquet.columnreaders.batchsizing.RecordBatchOverflow.FieldOverflowDefinition;
import org.apache.drill.exec.store.parquet.columnreaders.batchsizing.RecordBatchSizerManager;
import org.apache.drill.exec.store.parquet.columnreaders.batchsizing.RecordBatchSizerManager.FieldOverflowState;
import org.apache.drill.exec.store.parquet.columnreaders.batchsizing.RecordBatchSizerManager.FieldOverflowStateContainer;
import org.apache.drill.exec.store.parquet.columnreaders.batchsizing.RecordBatchSizerManager.VarLenColumnBatchStats;
import org.apache.drill.exec.util.record.RecordBatchStats;
import org.apache.drill.exec.vector.ValueVector;

/** Class which handles reading a batch of rows from a set of variable columns */
public class VarLenBinaryReader {

  final ParquetRecordReader parentReader;
  final RecordBatchSizerManager batchSizer;
  final List<VarLengthColumn<? extends ValueVector>> columns;
  /** Sorting columns to minimize overflow */
  final List<VLColumnContainer> orderedColumns;
  private final Comparator<VLColumnContainer> comparator = new VLColumnComparator();
  final boolean useAsyncTasks;
  private final boolean useBulkReader;

  public VarLenBinaryReader(ParquetRecordReader parentReader, List<VarLengthColumn<? extends ValueVector>> columns) {
    this.parentReader = parentReader;
    this.batchSizer = parentReader.getBatchSizesMgr();
    this.columns = columns;
    this.orderedColumns = populateOrderedColumns();
    this.useAsyncTasks = parentReader.useAsyncColReader;
    this.useBulkReader = parentReader.useBulkReader();
  }

  /**
   * Reads as many variable length values as possible.
   *
   * @param recordsToReadInThisPass - the number of records recommended for reading form the reader
   * @return - the number of fixed length fields that will fit in the batch

   */
  public long readFields(long recordsToReadInThisPass) throws IOException {

    // write the first 0 offset
    for (VarLengthColumn<?> columnReader : columns) {
      columnReader.reset();
    }
    Stopwatch timer = Stopwatch.createStarted();

    // Ensure we do not read more than batch record count
    recordsToReadInThisPass = Math.min(recordsToReadInThisPass, batchSizer.getCurrentRecordsPerBatch());

    long recordsReadInCurrentPass = 0;

    if (!useBulkReader) {
      recordsReadInCurrentPass = determineSizesSerial(recordsToReadInThisPass);

      if(useAsyncTasks) {
        readRecordsParallel(recordsReadInCurrentPass);
      } else {
        readRecordsSerial(recordsReadInCurrentPass);
      }
    } else {
      recordsReadInCurrentPass = readRecordsInBulk((int) recordsToReadInThisPass);
    }

    // Publish this information
    parentReader.getReadState().setValuesReadInCurrentPass((int) recordsReadInCurrentPass);

    // Update the stats
    parentReader.parquetReaderStats.timeVarColumnRead.addAndGet(timer.elapsed(TimeUnit.NANOSECONDS));

    return recordsReadInCurrentPass;
  }

  private int readRecordsInBulk(int recordsToReadInThisPass) throws IOException {
    int batchNumRecords = recordsToReadInThisPass;
    List<VarLenColumnBatchStats> columnStats = new ArrayList<VarLenColumnBatchStats>(columns.size());
    int prevReadColumns = -1;
    boolean overflowCondition = false;

    for (VLColumnContainer columnContainer : orderedColumns) {
      VarLengthColumn<?> columnReader = columnContainer.column;

      // Read the column data
      int readColumns = columnReader.readRecordsInBulk(batchNumRecords);
      Preconditions.checkState(readColumns <= batchNumRecords, "Reader cannot return more values than requested..");

      if (!overflowCondition) {
        if (prevReadColumns >= 0 && prevReadColumns != readColumns) {
          overflowCondition = true;
        } else {
          prevReadColumns = readColumns;
        }
      }

      // Enqueue this column entry information to handle overflow conditions; we will not know
      // whether an overflow happened till all variable length columns have been processed
      columnStats.add(new VarLenColumnBatchStats(columnReader.valueVec, readColumns));

      // Decrease the number of records to read when a column returns less records (minimize overflow)
      if (batchNumRecords > readColumns) {
        batchNumRecords = readColumns;
        // it seems this column caused an overflow (higher layer will not ask for more values than remaining)
        ++columnContainer.numCausedOverflows;
      }
    }

    // Set the value-count for each column
    for (VarLengthColumn<?> columnReader : columns) {
      columnReader.valuesReadInCurrentPass = batchNumRecords;
    }

    // Publish this batch statistics
    publishBatchStats(columnStats, batchNumRecords);

    // Handle column(s) overflow if any
    if (overflowCondition) {
      handleColumnOverflow(columnStats, batchNumRecords);
    }

    return batchNumRecords;
  }

  private void handleColumnOverflow(List<VarLenColumnBatchStats> columnStats, int batchNumRecords) {
    // Overflow would happen if a column returned more values than "batchValueCount"; this can happen
    // when a column Ci is called first, returns num-values-i, and then another column cj is called which
    // returns less values than num-values-i.
    RecordBatchOverflow.Builder builder = null;

    // We need to collect all columns which are subject to an overflow (except for the ones which are already
    // returning values from previous batch overflow)
    for (VarLenColumnBatchStats columnStat : columnStats) {
      if (columnStat.numValuesRead > batchNumRecords) {
        // We need to figure out whether this column was already returning values from a previous batch
        // overflow; if it is, then this is a NOOP (as the overflow data is still available to be replayed)
        if (fieldHasAlreadyOverflowData(columnStat.vector.getField().getName())) {
          continue;
        }

        // We need to set the value-count as otherwise some size related vector APIs won't work
        columnStat.vector.getMutator().setValueCount(batchNumRecords);

        // Lazy initialization
        if (builder == null) {
          builder = RecordBatchOverflow.newBuilder(parentReader.getOperatorContext().getAllocator(),
            batchSizer.getBatchStatsContext());
        }

        final int numOverflowValues = columnStat.numValuesRead - batchNumRecords;
        builder.addFieldOverflow(columnStat.vector, batchNumRecords, numOverflowValues);
      }
    }

    // Register batch overflow data with the record batch sizer manager (if any)
    if (builder != null) {
      Map<String, FieldOverflowStateContainer> overflowContainerMap = parentReader.getBatchSizesMgr().getFieldOverflowMap();
      Map<String, FieldOverflowDefinition> overflowDefMap = builder.build().getRecordOverflowDefinition().getFieldOverflowDefs();

      for (Map.Entry<String, FieldOverflowDefinition> entry : overflowDefMap.entrySet()) {
        FieldOverflowStateContainer overflowStateContainer = new FieldOverflowStateContainer(entry.getValue(), null);
        // Register this overflow condition
        overflowContainerMap.put(entry.getKey(), overflowStateContainer);
      }
    }

    reorderVLColumns();
  }

  private void reorderVLColumns() {
    // Finally, re-order the variable length columns since an overflow occurred
    Collections.sort(orderedColumns, comparator);

    if (batchSizer.getBatchStatsContext().isEnableBatchSzLogging()) {
      boolean isFirstValue = true;
      final StringBuilder msg = new StringBuilder();
      msg.append(": Dumping the variable length columns read order: ");

      for (VLColumnContainer container : orderedColumns) {
        if (!isFirstValue) {
          msg.append(", ");
        } else {
          isFirstValue = false;
        }
        msg.append(container.column.valueVec.getField().getName());
      }
      msg.append('.');

      RecordBatchStats.logRecordBatchStats(msg.toString(), batchSizer.getBatchStatsContext());
    }
  }

  private boolean fieldHasAlreadyOverflowData(String field) {
    FieldOverflowStateContainer container = parentReader.getBatchSizesMgr().getFieldOverflowContainer(field);

    if (container == null) {
      return false;
    }

    if (container.overflowState == null || container.overflowState.isOverflowDataFullyConsumed()) {
      parentReader.getBatchSizesMgr().releaseFieldOverflowContainer(field);
      return false;
    }
    return true;
  }

  private void publishBatchStats(List<VarLenColumnBatchStats> stats, int batchNumRecords) {
    // First, let us inform the variable columns of the number of records returned by this batch; this
    // is for managing overflow data state.
    Map<String, FieldOverflowStateContainer> overflowMap =
      parentReader.getBatchSizesMgr().getFieldOverflowMap();

    for (FieldOverflowStateContainer container : overflowMap.values()) {
      FieldOverflowState overflowState = container.overflowState;

      if (overflowState != null) {
        overflowState.onNewBatchValuesConsumed(batchNumRecords);
      }
    }

    // Now publish the same to the record batch sizer manager
    parentReader.getBatchSizesMgr().onEndOfBatch(batchNumRecords, stats);
  }

  private long determineSizesSerial(long recordsToReadInThisPass) throws IOException {

    int recordsReadInCurrentPass = 0;
    top: do {
      for (VarLengthColumn<?> columnReader : columns) {
        // Return status is "done reading", meaning stop if true.
        if (columnReader.determineSize(recordsReadInCurrentPass)) {
          break top;
        }
      }
      for (VarLengthColumn<?> columnReader : columns) {
        columnReader.updateReadyToReadPosition();
        columnReader.currDefLevel = -1;
      }
      recordsReadInCurrentPass++;
    } while (recordsReadInCurrentPass < recordsToReadInThisPass);

    return recordsReadInCurrentPass;
  }

  private void readRecordsSerial(long recordsReadInCurrentPass) {
    for (VarLengthColumn<?> columnReader : columns) {
      columnReader.readRecords(columnReader.pageReader.valuesReadyToRead);
    }
    for (VarLengthColumn<?> columnReader : columns) {
      columnReader.valueVec.getMutator().setValueCount((int)recordsReadInCurrentPass);
    }
  }

  private void readRecordsParallel(long recordsReadInCurrentPass){
    ArrayList<Future<Integer>> futures = Lists.newArrayList();
    for (VarLengthColumn<?> columnReader : columns) {
      Future<Integer> f = columnReader.readRecordsAsync(columnReader.pageReader.valuesReadyToRead);
      if (f != null) {
        futures.add(f);
      }
    }
    Exception exception = null;
    for(Future<Integer> f: futures){
      if(exception != null) {
        f.cancel(true);
      } else {
        try {
          f.get();
        } catch (Exception e) {
          f.cancel(true);
          exception = e;
        }
      }
    }
    for (VarLengthColumn<?> columnReader : columns) {
      columnReader.valueVec.getMutator().setValueCount((int)recordsReadInCurrentPass);
    }
  }

  protected void handleAndRaise(String s, Exception e) {
    String message = "Error in parquet record reader.\nMessage: " + s;
    throw new DrillRuntimeException(message, e);
  }

  private List<VLColumnContainer> populateOrderedColumns() {
    List<VLColumnContainer> result = new ArrayList<VLColumnContainer>(columns.size());

    // first, we need to populate this list
    for (VarLengthColumn<? extends ValueVector> column : columns) {
      result.add(new VLColumnContainer(column));
    }

    // now perform the sorting
    Collections.sort(result, comparator);

    return result;
  }

// ----------------------------------------------------------------------------
// Internal Data Structure
// ----------------------------------------------------------------------------

  /** Container class which will will allow us to implement ordering so to minimize overflow */
  private static final class VLColumnContainer {
    /** Variable length column */
    private final VarLengthColumn<? extends ValueVector> column;
    /** Number of times this method caused overflow */
    private int numCausedOverflows;

    /** Constructor */
    private VLColumnContainer(VarLengthColumn<? extends ValueVector> column) {
      this.column = column;
    }
  }

  /** Comparator class to minimize overflow; columns with highest chance of causing overflow
   * should be iterated first (have smallest value).
   */
  private static final class VLColumnComparator implements Comparator<VLColumnContainer> {
    // columns which caused overflows, should execute earlier (lowest order)
    @Override
    public int compare(VLColumnContainer o1, VLColumnContainer o2) {
      assert o1 != null && o2 != null;

      if (o1.numCausedOverflows == o2.numCausedOverflows) { return 0; }
      if (o1.numCausedOverflows < o2.numCausedOverflows)  { return 1; }

      return -1;
    }
  }

}
