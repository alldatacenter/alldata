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

import org.apache.drill.exec.store.CommonParquetRecordReader;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.impl.OutputMutator;
import org.apache.drill.exec.store.parquet.ParquetReaderUtility;
import org.apache.drill.exec.store.parquet.columnreaders.batchsizing.RecordBatchSizerManager;
import org.apache.drill.exec.util.record.RecordBatchStats.RecordBatchStatsContext;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.compression.CompressionCodecFactory;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParquetRecordReader extends CommonParquetRecordReader {

  private static final Logger logger = LoggerFactory.getLogger(ParquetRecordReader.class);

  // When no column is required by the downstream operator, ask SCAN to return a DEFAULT column. If such column does not exist,
  // it will return as a nullable-int column. If that column happens to exist, return that column.
  private static final List<SchemaPath> DEFAULT_COLS_TO_READ = ImmutableList.of(SchemaPath.getSimplePath("_DEFAULT_COL_TO_READ_"));

  private final FileSystem fileSystem;
  private final long numRecordsToRead; // number of records to read
  private final Path hadoopPath;
  private final CompressionCodecFactory codecFactory;
  private final int rowGroupIndex;
  private final ParquetReaderUtility.DateCorruptionStatus dateCorruptionStatus;

  /** Container object for holding Parquet columnar readers state */
  private ReadState readState;
  /** Responsible for managing record batch size constraints */
  private RecordBatchSizerManager batchSizerMgr;
  private BatchReader batchReader;

  final boolean useAsyncColReader;
  final boolean useAsyncPageReader;
  final boolean useBufferedReader;
  final int bufferedReadSize;
  final boolean useFadvise;
  final boolean enforceTotalSize;
  final long readQueueSize;

  private final boolean useBulkReader;

  public ParquetRecordReader(FragmentContext fragmentContext,
      Path path,
      int rowGroupIndex,
      long numRecordsToRead,
      FileSystem fs,
      CompressionCodecFactory codecFactory,
      ParquetMetadata footer,
      List<SchemaPath> columns,
      ParquetReaderUtility.DateCorruptionStatus dateCorruptionStatus) {
    this(fragmentContext, numRecordsToRead, path, rowGroupIndex, fs, codecFactory, footer, columns, dateCorruptionStatus);
  }

  public ParquetRecordReader(FragmentContext fragmentContext,
      Path path,
      int rowGroupIndex,
      FileSystem fs,
      CompressionCodecFactory codecFactory,
      ParquetMetadata footer,
      List<SchemaPath> columns,
      ParquetReaderUtility.DateCorruptionStatus dateCorruptionStatus) {
    this(fragmentContext, footer.getBlocks().get(rowGroupIndex).getRowCount(), path, rowGroupIndex, fs, codecFactory,
        footer, columns, dateCorruptionStatus);
  }

  public ParquetRecordReader(
      FragmentContext fragmentContext,
      long numRecordsToRead,
      Path path,
      int rowGroupIndex,
      FileSystem fs,
      CompressionCodecFactory codecFactory,
      ParquetMetadata footer,
      List<SchemaPath> columns,
      ParquetReaderUtility.DateCorruptionStatus dateCorruptionStatus) {
    super(footer, fragmentContext);
    this.hadoopPath = path;
    this.fileSystem = fs;
    this.codecFactory = codecFactory;
    this.rowGroupIndex = rowGroupIndex;
    this.dateCorruptionStatus = dateCorruptionStatus;
    this.numRecordsToRead = initNumRecordsToRead(numRecordsToRead, rowGroupIndex, footer);
    this.useAsyncColReader = fragmentContext.getOptions().getOption(ExecConstants.PARQUET_COLUMNREADER_ASYNC).bool_val;
    this.useAsyncPageReader = fragmentContext.getOptions().getOption(ExecConstants.PARQUET_PAGEREADER_ASYNC).bool_val;
    this.useBufferedReader = fragmentContext.getOptions().getOption(ExecConstants.PARQUET_PAGEREADER_USE_BUFFERED_READ).bool_val;
    this.bufferedReadSize = fragmentContext.getOptions().getOption(ExecConstants.PARQUET_PAGEREADER_BUFFER_SIZE).num_val.intValue();
    this.useFadvise = fragmentContext.getOptions().getOption(ExecConstants.PARQUET_PAGEREADER_USE_FADVISE).bool_val;
    this.readQueueSize = fragmentContext.getOptions().getOption(ExecConstants.PARQUET_PAGEREADER_QUEUE_SIZE).num_val;
    this.enforceTotalSize = fragmentContext.getOptions().getOption(ExecConstants.PARQUET_PAGEREADER_ENFORCETOTALSIZE).bool_val;
    this.useBulkReader = fragmentContext.getOptions().getOption(ExecConstants.PARQUET_FLAT_READER_BULK).bool_val;

    setColumns(columns);
  }

  /**
   * Flag indicating if the old non-standard data format appears
   * in this file, see DRILL-4203.
   *
   * @return true if the dates are corrupted and need to be corrected
   */
  public ParquetReaderUtility.DateCorruptionStatus getDateCorruptionStatus() {
    return dateCorruptionStatus;
  }

  public CompressionCodecFactory getCodecFactory() {
    return codecFactory;
  }

  public Path getHadoopPath() {
    return hadoopPath;
  }

  public FileSystem getFileSystem() {
    return fileSystem;
  }

  public int getRowGroupIndex() {
    return rowGroupIndex;
  }

  public RecordBatchSizerManager getBatchSizesMgr() {
    return batchSizerMgr;
  }

  public OperatorContext getOperatorContext() {
    return operatorContext;
  }

  public FragmentContext getFragmentContext() {
    return fragmentContext;
  }

  /**
   * @return true if Parquet reader Bulk processing is enabled; false otherwise
   */
  public boolean useBulkReader() {
    return useBulkReader;
  }

  public ReadState getReadState() {
    return readState;
  }

  /**
   * Prepare the Parquet reader. First determine the set of columns to read (the schema
   * for this read.) Then, create a state object to track the read across calls to
   * the reader <tt>next()</tt> method. Finally, create one of three readers to
   * read batches depending on whether this scan is for only fixed-width fields,
   * contains at least one variable-width field, or is a "mock" scan consisting
   * only of null fields (fields in the SELECT clause but not in the Parquet file.)
   */
  @Override
  public void setup(OperatorContext operatorContext, OutputMutator output) throws ExecutionSetupException {
    this.operatorContext = operatorContext;
    ParquetSchema schema = new ParquetSchema(fragmentContext.getOptions(), rowGroupIndex, footer, isStarQuery() ? null : getColumns());
    batchSizerMgr = new RecordBatchSizerManager(fragmentContext.getOptions(), schema, numRecordsToRead, new RecordBatchStatsContext(fragmentContext, operatorContext));

    logger.debug("Reading {} records from row group({}) in file {}.", numRecordsToRead, rowGroupIndex,
        hadoopPath.toUri().getPath());

    try {
      schema.buildSchema();
      batchSizerMgr.setup();
      readState = new ReadState(schema, batchSizerMgr, parquetReaderStats, numRecordsToRead, useAsyncColReader);
      readState.buildReader(this, output);
    } catch (Exception e) {
      throw handleAndRaise("Failure in setting up reader", e);
    }

    ColumnReader<?> firstColumnStatus = readState.getFirstColumnReader();
    if (firstColumnStatus == null) {
      batchReader = new BatchReader.MockBatchReader(readState);
    } else if (schema.allFieldsFixedLength()) {
      batchReader = new BatchReader.FixedWidthReader(readState);
    } else {
      batchReader = new BatchReader.VariableWidthReader(readState);
    }
  }

  @Override
  public void allocate(Map<String, ValueVector> vectorMap) throws OutOfMemoryException {
    batchSizerMgr.allocate(vectorMap);
  }

  /**
   * Read the next record batch from the file using the reader and read state
   * created previously.
   */
  @Override
  public int next() {
    readState.resetBatch();
    Stopwatch timer = Stopwatch.createStarted();
    try {
      return batchReader.readBatch();
    } catch (Exception e) {
      throw handleAndRaise("\nHadoop path: " + hadoopPath.toUri().getPath() +
        "\nTotal records read: " + readState.recordsRead() +
        "\nRow group index: " + rowGroupIndex +
        "\nRecords to read: " + numRecordsToRead, e);
    } finally {
      parquetReaderStats.timeProcess.addAndGet(timer.elapsed(TimeUnit.NANOSECONDS));
    }
  }

  @Override
  public void close() {
    long recordsRead = (readState == null) ? 0 : readState.recordsRead();
    logger.debug("Read {} records out of row group({}) in file '{}'",
        recordsRead, rowGroupIndex,
        hadoopPath.toUri().getPath());

    // enable this for debugging when it is know that a whole file will be read
    // limit kills upstream operators once it has enough records, so this assert will fail
    //    assert totalRecordsRead == footer.getBlocks().get(rowGroupIndex).getRowCount();

    // NOTE - We check whether the parquet reader data structure is not null before calling close();
    //        this is because close() can be invoked before setup() has executed (probably because of
    //        spurious failures).

    if (readState != null) {
      readState.close();
      readState = null;
    }

    if (batchSizerMgr != null) {
      batchSizerMgr.close();
      batchSizerMgr = null;
    }

    codecFactory.release();

    closeStats(logger, hadoopPath);
  }

  @Override
  protected List<SchemaPath> getDefaultColumnsToRead() {
    return DEFAULT_COLS_TO_READ;
  }

  @Override
  public String toString() {
    return "ParquetRecordReader[File=" + hadoopPath.toUri()
        + ", Row group index=" + rowGroupIndex
        + ", Records to read=" + numRecordsToRead
        + ", Total records read=" + (readState != null ? readState.recordsRead() : -1)
        + ", Metadata" + footer
        + "]";
  }
}
