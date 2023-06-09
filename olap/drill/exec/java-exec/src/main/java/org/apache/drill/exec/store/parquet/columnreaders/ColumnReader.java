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

import io.netty.buffer.DrillBuf;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.vector.BaseDataValueVector;
import org.apache.drill.exec.vector.ValueVector;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.column.Encoding;
import org.apache.parquet.format.SchemaElement;
import org.apache.parquet.hadoop.metadata.ColumnChunkMetaData;
import org.apache.parquet.schema.PrimitiveType;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;

public abstract class ColumnReader<V extends ValueVector> {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ColumnReader.class);

  public static final Set<Encoding> DICTIONARY_ENCODINGS = ImmutableSet.of(
    Encoding.PLAIN_DICTIONARY,
    Encoding.RLE_DICTIONARY
  );
  public static final Set<Encoding> VALUE_ENCODINGS = ImmutableSet.<Encoding>builder()
    .addAll(DICTIONARY_ENCODINGS)
    .add(Encoding.DELTA_BINARY_PACKED)
    .add(Encoding.DELTA_BYTE_ARRAY)
    .add(Encoding.DELTA_LENGTH_BYTE_ARRAY)
    .build();

  final ParquetRecordReader parentReader;

  // Value Vector for this column
  final V valueVec;

  ColumnDescriptor getColumnDescriptor() {
    return columnDescriptor;
  }

  // column description from the parquet library
  final ColumnDescriptor columnDescriptor;
  // metadata of the column, from the parquet library
  final ColumnChunkMetaData columnChunkMetaData;
  // status information on the current page
  PageReader pageReader;

  final SchemaElement schemaElement;
  boolean usingDictionary;

  // quick reference to see if the field is fixed length (as this requires an instanceof)
  final boolean isFixedLength;

  // counter for the total number of values read from one or more pages
  // when a batch is filled all of these values should be the same for all of the columns
  int totalValuesRead;

  // counter for the values that have been read in this pass (a single call to the next() method)
  int valuesReadInCurrentPass;

  // length of single data value in bits, if the length is fixed
  int dataTypeLengthInBits;
  int bytesReadInCurrentPass;

  protected DrillBuf vectorData;
  // when reading definition levels for nullable columns, it is a one-way stream of integers
  // when reading var length data, where we don't know if all of the records will fit until we've read all of them
  // we must store the last definition level and use it at the start of the next batch
  int currDefLevel;

  // variables for a single read pass
  long readStartInBytes = 0;
  long readLength = 0;
  long readLengthInBits = 0;
  long recordsReadInThisIteration = 0;
  private ExecutorService threadPool;

  volatile boolean isShuttingDown; //Indicate to not submit any new AsyncPageReader Tasks during clear()

  protected ColumnReader(ParquetRecordReader parentReader, ColumnDescriptor descriptor,
      ColumnChunkMetaData columnChunkMetaData, boolean fixedLength, V v, SchemaElement schemaElement) throws ExecutionSetupException {
    this.parentReader = parentReader;
    this.columnDescriptor = descriptor;
    this.columnChunkMetaData = columnChunkMetaData;
    this.isFixedLength = fixedLength;
    this.schemaElement = schemaElement;
    this.valueVec =  v;
    this.pageReader = parentReader.useAsyncPageReader
      ? new AsyncPageReader(this, parentReader.getFileSystem(), parentReader.getHadoopPath())
      : new PageReader(this, parentReader.getFileSystem(), parentReader.getHadoopPath());

    try {
      pageReader.init();
    } catch (IOException e) {
      UserException ex = UserException.dataReadError(e)
          .message("Error initializing page reader for Parquet file")
          .pushContext("Row Group Start: ", this.columnChunkMetaData.getStartingPos())
          .pushContext("Column: ", this.schemaElement.getName())
          .pushContext("File: ", this.parentReader.getHadoopPath().toString() )
          .build(logger);
      throw ex;
    }
    if (columnDescriptor.getType() != PrimitiveType.PrimitiveTypeName.BINARY) {
      if (columnDescriptor.getType() == PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY) {
        dataTypeLengthInBits = columnDescriptor.getTypeLength() * 8;
      } else {
        dataTypeLengthInBits = ParquetColumnMetadata.getTypeLengthInBits(columnDescriptor.getType());
      }
    }
    threadPool = parentReader.getOperatorContext().getScanDecodeExecutor();
  }

  public int getRecordsReadInCurrentPass() {
    return valuesReadInCurrentPass;
  }

  public Future<Long> processPagesAsync(long recordsToReadInThisPass){
    Future<Long> r = (isShuttingDown ? null : threadPool.submit(new ColumnReaderProcessPagesTask(recordsToReadInThisPass)));
    return r;
  }

  public void processPages(long recordsToReadInThisPass) throws IOException {
    reset();
    if(recordsToReadInThisPass>0) {
      do {
        determineSize(recordsToReadInThisPass);

      } while (valuesReadInCurrentPass < recordsToReadInThisPass && pageReader.hasPage());
    }
    logger.trace("Column Reader: {} - Values read in this pass: {} - ",
        this.getColumnDescriptor().toString(), valuesReadInCurrentPass);
    valueVec.getMutator().setValueCount(valuesReadInCurrentPass);
  }

  public void clear() {
    //State to indicate no more tasks to be scheduled
    isShuttingDown = true;

    valueVec.clear();
    pageReader.clear();
  }

  public void readValues(long recordsToRead) {
    try {
      readField(recordsToRead);

      valuesReadInCurrentPass += (int)recordsReadInThisIteration;
      pageReader.valuesRead += (int)recordsReadInThisIteration;
      pageReader.readPosInBytes = readStartInBytes + readLength;
    } catch (Exception e) {
      UserException ex = UserException.dataReadError(e)
          .message("Error reading from Parquet file")
          .pushContext("Row Group Start: ", this.columnChunkMetaData.getStartingPos())
          .pushContext("Column: ", this.schemaElement.getName())
          .pushContext("File: ", this.parentReader.getHadoopPath().toString() )
          .build(logger);
      throw ex;
    }
  }

  protected abstract void readField(long recordsToRead);

  /**
   * Determines the size of a single value in a variable column.
   *
   * Return value indicates if we have finished a row group and should stop reading
   *
   * @param recordsReadInCurrentPass records read in current pass
   * @return true if we should stop reading
   * @throws IOException
   */
  public boolean determineSize(long recordsReadInCurrentPass) throws IOException {

    if (readPage()) {
      return true;
    }

    if (processPageData((int) recordsReadInCurrentPass)) {
      return true;
    }

    return checkVectorCapacityReached();
  }

  protected Future<Integer> readRecordsAsync(int recordsToRead) {
    Future<Integer> r = (isShuttingDown ? null : threadPool.submit(new ColumnReaderReadRecordsTask(recordsToRead)));
    return r;
  }

  protected void readRecords(int recordsToRead) {
    for (int i = 0; i < recordsToRead; i++) {
      readField(i);
    }
    pageReader.valuesRead += recordsToRead;
  }

  protected int readRecordsInBulk(int recordsToReadInThisPass) throws IOException {
      throw new UnsupportedOperationException();
  }

  protected boolean recordsRequireDecoding() {
    return usingDictionary || !Collections.disjoint(VALUE_ENCODINGS, columnChunkMetaData.getEncodings());
  }

  protected boolean processPageData(int recordsToReadInThisPass) throws IOException {
    readValues(recordsToReadInThisPass);
    return true;
  }

  public void updatePosition() {}

  public void updateReadyToReadPosition() {}

  public void reset() {
    readStartInBytes = 0;
    readLength = 0;
    readLengthInBits = 0;
    recordsReadInThisIteration = 0;
    bytesReadInCurrentPass = 0;
    vectorData = ((BaseDataValueVector) valueVec).getBuffer();
  }

  public int capacity() {
    return (int) (valueVec.getValueCapacity() * dataTypeLengthInBits / 8.0);
  }

  public Future<Boolean> readPageAsync() {
    Future<Boolean> f = threadPool.submit(new Callable<Boolean>() {
      @Override public Boolean call() throws Exception {
        return Boolean.valueOf(readPage());
      }
    });
    return f;
  }

  /**
   * Read a page. If we need more data, exit the read loop and return true.
   *
   * @return true if we need more data and page is not read successfully
   * @throws IOException
   */
  public boolean readPage() throws IOException {
    if (!pageReader.hasPage()
        || totalValuesReadAndReadyToReadInPage() == pageReader.pageValueCount) {
      readRecords(pageReader.valuesReadyToRead);
      if (pageReader.hasPage()) {
        totalValuesRead += pageReader.pageValueCount;
      }
      if (!pageReader.next()) {
        hitRowGroupEnd();
        return true;
      }
      postPageRead();
    }
    return false;
  }

  protected int totalValuesReadAndReadyToReadInPage() {
    return pageReader.valuesRead + pageReader.valuesReadyToRead;
  }

  protected void postPageRead() {
    pageReader.valuesReadyToRead = 0;
  }

  protected void hitRowGroupEnd() {}

  protected boolean checkVectorCapacityReached() {
    // Here "bits" means "bytes"
    // But, inside "capacity", "bits" sometimes means "bits".
    // Note that bytesReadInCurrentPass is never updated, so this next
    // line is a no-op.
    if (bytesReadInCurrentPass + dataTypeLengthInBits > capacity()) {
      logger.debug("Reached the capacity of the data vector in a variable length value vector.");
      return true;
    }
    // No op: already checked this earlier and would not be here if this
    // condition is true.
    return valuesReadInCurrentPass > valueVec.getValueCapacity();
  }

  /**
   * This is copied out of Parquet library, didn't want to deal with the
   * unnecessary throws statement they had declared
   *
   * @param in incoming data
   * @param offset offset
   * @return little endian integer
   */
  public static int readIntLittleEndian(DrillBuf in, int offset) {
    int ch4 = in.getByte(offset) & 0xff;
    int ch3 = in.getByte(offset + 1) & 0xff;
    int ch2 = in.getByte(offset + 2) & 0xff;
    int ch1 = in.getByte(offset + 3) & 0xff;
    return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
  }

  private class ColumnReaderProcessPagesTask implements Callable<Long> {

    private final ColumnReader<V> parent = ColumnReader.this;
    private final long recordsToReadInThisPass;

    public ColumnReaderProcessPagesTask(long recordsToReadInThisPass){
      this.recordsToReadInThisPass = recordsToReadInThisPass;
    }

    /**
     * This method calls the column reader.
     *
     * @return records to read
     * @throws IOException
     */
    @Override public Long call() throws IOException{

      String oldname = Thread.currentThread().getName();
      try {
        Thread.currentThread().setName(oldname + "Decode-" + this.parent.columnChunkMetaData.toString());

        this.parent.processPages(recordsToReadInThisPass);
        return recordsToReadInThisPass;

      } finally {
        Thread.currentThread().setName(oldname);
      }
    }
  }

  private class ColumnReaderReadRecordsTask implements Callable<Integer> {

    private final ColumnReader<V> parent = ColumnReader.this;
    private final int recordsToRead;

    public ColumnReaderReadRecordsTask(int recordsToRead){
      this.recordsToRead = recordsToRead;
    }

    /**
     * This method calls the column reader.
     *
     * @return records to read
     * @throws IOException
     */
    @Override public Integer call() throws IOException{

      String oldname = Thread.currentThread().getName();
      try {
        Thread.currentThread().setName("Decode-" + this.parent.columnChunkMetaData.toString());

        this.parent.readRecords(recordsToRead);
        return recordsToRead;

      } finally {
        Thread.currentThread().setName(oldname);
      }
    }

  }

}
