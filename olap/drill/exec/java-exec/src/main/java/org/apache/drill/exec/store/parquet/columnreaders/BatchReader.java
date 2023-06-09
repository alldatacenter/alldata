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

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

/**
 * Base strategy for reading a batch of Parquet records.
 */
public abstract class BatchReader {

  protected final ReadState readState;

  public BatchReader(ReadState readState) {
    this.readState = readState;
  }

  public int readBatch() throws Exception {
    ColumnReader<?> firstColumnStatus = readState.getFirstColumnReader();
    int currBatchNumRecords = readState.batchSizerMgr().getCurrentRecordsPerBatch();
    long recordsToRead = Math.min(currBatchNumRecords, readState.getRemainingValuesToRead());
    int readCount = recordsToRead > 0 ? readRecords(firstColumnStatus, recordsToRead) : 0;
    readState.fillNullVectors(readCount);

    return readCount;
  }

  protected abstract int readRecords(ColumnReader<?> firstColumnStatus, long recordsToRead) throws Exception;

  protected void readAllFixedFields(long recordsToRead) throws Exception {
    Stopwatch timer = Stopwatch.createStarted();
    if(readState.useAsyncColReader()){
      readAllFixedFieldsParallel(recordsToRead);
    } else {
      readAllFixedFieldsSerial(recordsToRead);
    }
    readState.parquetReaderStats().timeFixedColumnRead.addAndGet(timer.elapsed(TimeUnit.NANOSECONDS));
  }

  protected void readAllFixedFieldsSerial(long recordsToRead) throws IOException {
    for (ColumnReader<?> colReader : readState.getFixedLenColumnReaders()) {
      colReader.processPages(recordsToRead);
    }
  }

  protected void readAllFixedFieldsParallel(long recordsToRead) throws Exception {
    ArrayList<Future<Long>> futures = Lists.newArrayList();
    for (ColumnReader<?> colReader : readState.getFixedLenColumnReaders()) {
      Future<Long> f = colReader.processPagesAsync(recordsToRead);
      if (f != null) {
        futures.add(f);
      }
    }
    Exception exception = null;
    for(Future<Long> f: futures){
      if (exception != null) {
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
    if (exception != null) {
      throw exception;
    }
  }

  /**
   * Strategy for reading mock records. Mock records appear to occur in the case
   * in which the query has SELECT a, b, but the Parquet file has only c, d.
   * A mock scan reads dummy columns for all records to ensure that the batch
   * contains a record for each Parquet record, but with no data per record.
   * (This explanation is reverse-engineered from the code and may be wrong.
   * Caveat emptor!)
   */

  public static class MockBatchReader extends BatchReader {

    public MockBatchReader(ReadState readState) {
      super(readState);
    }

    @Override
    protected int readRecords(ColumnReader<?> firstColumnStatus, long recordsToRead) {
      readState.updateCounts((int) recordsToRead);
      return (int) recordsToRead;
    }
  }

  /**
   * Strategy for reading a record batch when all columns are
   * fixed-width.
   */

  public static class FixedWidthReader extends BatchReader {

    public FixedWidthReader(ReadState readState) {
      super(readState);
    }

    @Override
    protected int readRecords(ColumnReader<?> firstColumnStatus, long recordsToRead) throws Exception {
      readAllFixedFields(recordsToRead);

      Preconditions.checkNotNull(firstColumnStatus != null);
      readState.setValuesReadInCurrentPass(firstColumnStatus.getRecordsReadInCurrentPass()); // get the number of rows read

      readState.updateCounts((int) recordsToRead); // update the shared Reader State

      return readState.getValuesReadInCurrentPass();
    }
  }

  /**
   * Strategy for reading a record batch when at last one column is
   * variable width.
   */

  public static class VariableWidthReader extends BatchReader {

    public VariableWidthReader(ReadState readState) {
      super(readState);
    }

    @Override
    protected int readRecords(ColumnReader<?> firstColumnStatus, long recordsToRead) throws Exception {
      // We should not rely on the "firstColumnStatus.getRecordsReadInCurrentPass()" when dealing
      // with variable length columns as each might return a different number of records. The batch size
      // will be the lowest value. The variable column readers will update the "readState" object to
      // reflect the correct information.
      long fixedRecordsToRead = readState.varLengthReader().readFields(recordsToRead);
      readAllFixedFields(fixedRecordsToRead);

      // Sanity check the fixed readers read the expected number of rows
      Preconditions.checkArgument(firstColumnStatus == null
        || firstColumnStatus.getRecordsReadInCurrentPass() == readState.getValuesReadInCurrentPass());

      readState.updateCounts((int) fixedRecordsToRead);

      return readState.getValuesReadInCurrentPass();
    }
  }
}
