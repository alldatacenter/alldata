/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.connector.legacy.hive.sink;

import com.bytedance.bitsail.base.execution.ProcessResult;
import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.ddl.sink.SinkEngineConnector;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.common.option.WriterOptions;
import com.bytedance.bitsail.common.util.JsonSerializer;
import com.bytedance.bitsail.common.util.Pair;
import com.bytedance.bitsail.connector.hadoop.sink.FileOutputFormatPlugin;
import com.bytedance.bitsail.connector.hadoop.util.HdfsUtils;
import com.bytedance.bitsail.connector.legacy.hive.common.HiveParqueFormatErrorCode;
import com.bytedance.bitsail.connector.legacy.hive.common.HiveSinkEngineConnector;
import com.bytedance.bitsail.connector.legacy.hive.option.HiveWriterOptions;
import com.bytedance.bitsail.conversion.hive.ConvertToHiveObjectOptions;
import com.bytedance.bitsail.conversion.hive.extractor.GeneralWritableExtractor;
import com.bytedance.bitsail.conversion.hive.extractor.HiveWritableExtractor;
import com.bytedance.bitsail.conversion.hive.extractor.ParquetWritableExtractor;
import com.bytedance.bitsail.conversion.hive.option.HiveConversionOptions;

import com.bytedance.bitsail.shaded.hive.client.HiveMetaClientUtil;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.flink.api.common.io.FileOutputFormat;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.FileSystem.WriteMode;
import org.apache.flink.core.fs.Path;
import org.apache.flink.types.Row;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.ql.exec.FileSinkOperator.RecordWriter;
import org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat;
import org.apache.hadoop.hive.serde2.Deserializer;
import org.apache.hadoop.hive.serde2.SerDeException;
import org.apache.hadoop.hive.serde2.SerDeUtils;
import org.apache.hadoop.hive.serde2.Serializer;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.JobConf;
import org.apache.parquet.hadoop.ParquetOutputFormat;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * @param <E>
 * @desc: {@link FileOutputFormat} for Hive Parquet records
 */
public class HiveOutputFormat<E extends Row> extends FileOutputFormatPlugin<E> {

  public static final Logger LOG = LoggerFactory.getLogger(HiveOutputFormat.class);
  private static final long serialVersionUID = 1L;
  /**
   * the column info to write, defined in task conf
   */
  protected List<ColumnInfo> columns = null;

  /**
   * compression type, special parameter for parquet
   */
  protected String parquetCompression;

  protected String db;
  protected String table;
  protected HiveWritableExtractorType hiveWritableExtractorType;
  protected String partition;
  protected RecordWriter writer;
  protected Serializer serDe;
  protected long instanceId;
  protected transient HiveConf hiveConf;

  /**
   * the intermediate path to store data, only when job was successfully finished, this path will be renamed to final place
   */
  protected Path tmpOutputFilePath;
  protected HiveMeta hiveMeta;
  /**
   * the real table column info
   */
  protected Map<String, Integer> columnMapping;
  /**
   * whether write null value when doing type converting
   */
  protected ConvertToHiveObjectOptions convertToHiveObjectOptions;
  @Getter(AccessLevel.PACKAGE)
  protected HiveWritableExtractor hiveWritableExtractor;
  private Path committedTmpFilePath;

  public HiveOutputFormat() {
    // We should always create a parent dir in hive's data path
    setOutputDirectoryMode(OutputDirectoryMode.ALWAYS);
  }

  @Override
  public void initializeGlobal(int parallelism) throws IOException {
    if (getOutputDirectoryMode() != OutputDirectoryMode.ALWAYS) {
      LOG.error("OutputDirectoryMode must be ALWAYS!");
      throw new IOException("OutputDirectoryMode must be ALWAYS!");
    }
  }

  protected void dropPartition() throws IOException {
    // If OVERWRITE we should delete the target path if existed. Do we need to use drop partition?
    try {
      if (getWriteMode() == WriteMode.OVERWRITE) {
        if (HiveMetaClientUtil.hasPartition(hiveConf, db, table, partition)) {
          LOG.info("Target hive partition exist. " + db + "." + table + " " + partition);
          HiveMetaClientUtil.dropPartition(hiveConf, db, table, partition);
        }

        if (HdfsUtils.checkExists(super.outputFilePath)) {
          LOG.info("Target output path " + super.outputFilePath + " already exist. The program will delete it.");
          HdfsUtils.deletePath(super.outputFilePath);
        }
      }
    } catch (IOException e) {
      LOG.error("Can't delete output path " + super.outputFilePath + "! This will cause the whole job exit!");
      throw new IOException("Can't delete output path " + super.outputFilePath + "! This will cause the whole job exit!", e);
    } catch (TException e) {
      LOG.error("Check hive partition failed. Due to " + e.getMessage(), e);
      throw new IOException("Check hive partition failed. Due to " + e.getMessage(), e);
    }
  }

  protected void validateParameters() {
    this.instanceId = commonConfig.getUnNecessaryOption(CommonOptions.INSTANCE_ID, -1L);

    // Hive table related
    db = outputSliceConfig.getNecessaryOption(HiveWriterOptions.DB_NAME, HiveParqueFormatErrorCode.REQUIRED_VALUE);
    table = outputSliceConfig.getNecessaryOption(HiveWriterOptions.TABLE_NAME, HiveParqueFormatErrorCode.REQUIRED_VALUE);
    hiveWritableExtractorType =
        HiveWritableExtractorType.valueOf(outputSliceConfig.get(HiveConversionOptions.WRITABLE_EXTRACTOR_TYPE).toUpperCase());
    partition = outputSliceConfig.getNecessaryOption(HiveWriterOptions.PARTITION, HiveParqueFormatErrorCode.REQUIRED_VALUE);
    columns = outputSliceConfig.get(WriterOptions.BaseWriterOptions.COLUMNS);

    boolean dateTypeToStringAsLong = outputSliceConfig.get(HiveWriterOptions.DATE_TO_STRING_AS_LONG);

    boolean nullStringAsNull = outputSliceConfig.get(HiveConversionOptions.NULL_STRING_AS_NULL);

    ConvertToHiveObjectOptions.DatePrecision datePrecision =
        ConvertToHiveObjectOptions.DatePrecision.valueOf(outputSliceConfig.get(HiveWriterOptions.DATE_PRECISION).toUpperCase().trim());

    // Convert related
    boolean convertErrorColumnAsNull = outputSliceConfig.get(HiveWriterOptions.CONVERT_ERROR_COLUMN_AS_NULL);

    ConvertToHiveObjectOptions.ConvertToHiveObjectOptionsBuilder optionsBuilder = ConvertToHiveObjectOptions.builder();
    convertToHiveObjectOptions = optionsBuilder
        .convertErrorColumnAsNull(convertErrorColumnAsNull)
        .dateTypeToStringAsLong(dateTypeToStringAsLong)
        .datePrecision(datePrecision)
        .nullStringAsNull(nullStringAsNull)
        .useStringText(false)
        .build();

    // Output dir related
    String writeMode = outputSliceConfig.get(HiveWriterOptions.WRITE_MODE);
    setWriteMode(WriteMode.valueOf(writeMode.toUpperCase()));

    parquetCompression = outputSliceConfig.get(HiveWriterOptions.HIVE_PARQUET_COMPRESSION);

    if (null == hiveConf) {
      hiveConf = getHiveConf();
    }
  }

  @Override
  public void open(int taskNumber, int numTasks) throws IOException {
    super.open(taskNumber, numTasks);
    if (taskNumber < 0 || numTasks < 1) {
      throw new IllegalArgumentException("TaskNumber: " + taskNumber + ", numTasks: " + numTasks);
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Opening stream for output (" + (taskNumber + 1) + "/" + numTasks + "). WriteMode=" + getWriteMode() +
          ", OutputDirectoryMode=" + getOutputDirectoryMode());
    }

    Path p = this.tmpOutputFilePath;
    if (p == null) {
      throw new IOException("The file path is null.");
    }

    // Suffix the path with the parallel instance index, if needed
    this.actualFilePath = getAttemptFileName();
    this.committedTmpFilePath = getCommitFileName(taskNumber);
    this.fileCreated = true;
    LOG.info("actualFilePath: " + actualFilePath);

    // delete task file
    tryDeleteFilePath(actualFilePath);
    tryDeleteFilePath(committedTmpFilePath);

    Properties tableProps = new Properties();

    hiveMeta.serdeParameters.forEach(tableProps::setProperty);

    tableProps.setProperty("columns", hiveMeta.columns);
    tableProps.setProperty("columns.types", hiveMeta.columnTypes);

    tableProps.setProperty(ParquetOutputFormat.COMPRESSION, parquetCompression);

    LOG.info(db + "." + table + " column names: " + hiveMeta.columns);
    LOG.info(db + "." + table + " column types: " + hiveMeta.columnTypes);
    LOG.info("inputFormat: " + hiveMeta.inputFormat + " outputFormat: " + hiveMeta.outputFormat + " serDe: " + hiveMeta.serializationLib);

    org.apache.hadoop.hive.ql.io.HiveOutputFormat outputFormat;
    try {
      outputFormat = (org.apache.hadoop.hive.ql.io.HiveOutputFormat) Class.forName(hiveMeta.outputFormat).newInstance();
      serDe = (Serializer) Class.forName(hiveMeta.serializationLib).newInstance();
      SerDeUtils.initializeSerDe((Deserializer) serDe, new Configuration(), tableProps, null);
    } catch (Exception e) {
      LOG.error("Init HiveOutputFormat or HiveSerDe failed!", e);
      throw new IOException(e);
    }

    // Translate to hadoop path class
    org.apache.hadoop.fs.Path currentTaskOutputPath = new org.apache.hadoop.fs.Path(actualFilePath.toString());
    LOG.info("Current task output path: " + currentTaskOutputPath);

    // RecordWriter is based on native hadoop file system, so need to init a hadoop JobConf
    // the better way is to unify file system to single hadoop or single flink. The single one can simplify using cost and
    // avoid package conflicts.
    JobConf jobConf = initHadoopJobConf();
    boolean isCompressed = true;
    if (outputFormat instanceof HiveIgnoreKeyTextOutputFormat) {
      isCompressed = false;
    }
    writer = outputFormat.getHiveRecordWriter(jobConf, currentTaskOutputPath, org.apache.hadoop.io.Text.class, isCompressed, tableProps, null);
    buildWritableExtractor();
  }

  private void tryDeleteFilePath(Path filePath) throws IOException {
    if (HdfsUtils.checkExists(filePath)) {
      LOG.info("Delete task tmp output file: " + filePath);
      boolean isDeleted = HdfsUtils.deletePath(filePath);
      if (!isDeleted) {
        String errMsg = "Failed to delete task tmp output file: " + filePath;
        LOG.info(errMsg);
        throw new IOException(errMsg);
      }
    }
  }

  public HiveConf getHiveConf() {
    Map<String, String> hiveProperties =
        JsonSerializer.parseToMap(outputSliceConfig.getNecessaryOption(HiveWriterOptions.HIVE_METASTORE_PROPERTIES, CommonErrorCode.CONFIG_ERROR));
    return HiveMetaClientUtil.getHiveConf(hiveProperties);
  }

  public JobConf initHadoopJobConf() {
    JobConf jobConf = new JobConf();
    if (Objects.nonNull(outputSliceConfig.getUnNecessaryOption(HiveWriterOptions.CUSTOMIZED_HADOOP_CONF, null))) {
      LOG.info("Init user hadoop jobConf.");
      Map<String, String> hadoopConf = outputSliceConfig.getNecessaryOption(HiveWriterOptions.CUSTOMIZED_HADOOP_CONF, null);
      hadoopConf.forEach(jobConf::set);
    }
    return jobConf;
  }

  private void buildWritableExtractor() {
    String[] fieldNames = this.columns.stream().map(ColumnInfo::getName).toArray(String[]::new);

    switch (hiveWritableExtractorType) {
      case PARQUET:
        hiveWritableExtractor = new ParquetWritableExtractor();
        break;
      default:
        hiveWritableExtractor = new GeneralWritableExtractor();
    }
    hiveWritableExtractor.setColumnMapping(this.columnMapping);
    hiveWritableExtractor.setFieldNames(fieldNames);
    hiveWritableExtractor.initConvertOptions(convertToHiveObjectOptions);
    hiveWritableExtractor.createObjectInspector(hiveMeta.columns, hiveMeta.columnTypes);
  }

  private synchronized void closeWriter() throws IOException {
    if (null != writer) {
      writer.close(false);
      writer = null;
    }
  }

  @SneakyThrows
  @Override
  public void close() throws IOException {
    closeWriter();
    FileSystem fileSystem = actualFilePath.getFileSystem();
    if (fileSystem.exists(actualFilePath)) {
      LOG.info("Subtask {} commit temporary file {}.", taskNumber, actualFilePath.toString());
      commitAttempt(taskNumber, getRuntimeContext().getAttemptNumber());
    } else {
      LOG.info("Subtask {} temporary file {} already cleanup.", taskNumber, actualFilePath);
    }
    super.close();

  }

  @Override
  public Path getCommitFolder() {
    return tmpOutputFilePath;
  }

  @Override
  public Path getAttemptFolder() {
    return getCommitFolder().suffix("/tmp");
  }

  @Override
  public String getFileName() {
    return String.format("/bitsail-hive-%s-%s", this.jobId, this.instanceId);
  }

  @Override
  public void tryCleanupOnError() {
    if (this.fileCreated) {
      this.fileCreated = false;

      try {
        LOG.info("Closing actual file path: " + actualFilePath);
        closeWriter();
        LOG.info("Closed actual file path: " + actualFilePath);
      } catch (Exception e) {
        LOG.error("Could not properly close FileOutputFormat.", e);
      }

      try {
        LOG.info("Deleting actual file path: " + actualFilePath);
        tryDeleteFilePath(actualFilePath);
        LOG.info("Deleted actual file path: " + actualFilePath);
        LOG.info("Deleting committed tmp file path: " + committedTmpFilePath);
        tryDeleteFilePath(committedTmpFilePath);
        LOG.info("Deleted committed tmp file path: " + committedTmpFilePath);
      } catch (FileNotFoundException e) {
        // ignore, may not be visible yet or may be already removed
      } catch (Throwable t) {
        LOG.error("Could not remove the incomplete file " + actualFilePath + '.', t);
      }
    }
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  @Override
  public void initPlugin() throws TException, IOException {
    validateParameters();
    super.outputFilePath = new Path(HiveMetaClientUtil.getTablePath(hiveConf, db, table, partition));

    // Temp path
    long currentTimestamp = System.currentTimeMillis() / 1000;
    tmpOutputFilePath = super.outputFilePath.suffix("-bitsail-tmp-" + currentTimestamp);
    if (FileSystem.get(tmpOutputFilePath.toUri()).exists(tmpOutputFilePath)) {
      LOG.info("tmp output path " + tmpOutputFilePath + " already exist. The program will delete it.");
      HdfsUtils.deletePath(tmpOutputFilePath);
    } else {
      LOG.info("tmp output path " + tmpOutputFilePath + " not exist.");
    }

    Pair<String, String> hiveTableSchema = null;
    StorageDescriptor storageDescriptor = null;
    Map<String, String> hiveSerdeParameter;
    try {
      hiveTableSchema = HiveMetaClientUtil.getTableSchema(hiveConf, db, table);
      storageDescriptor = HiveMetaClientUtil.getTableFormat(hiveConf, db, table);
      hiveSerdeParameter = HiveMetaClientUtil.getSerdeParameters(hiveConf, db, table);
      columnMapping = HiveMetaClientUtil.getColumnMapping(hiveTableSchema);
      LOG.info("fetch column mapping from hive metastore:\n {}", columnMapping);
    } catch (TException e) {
      LOG.error("Get hive table " + db + "." + table + " schema failed.", e);
      throw new IOException("Get hive table " + db + "." + table + " schema failed.", e);
    }

    hiveMeta = new HiveMeta(
        hiveTableSchema.getFirst(),
        hiveTableSchema.getSecond(),
        storageDescriptor.getInputFormat(),
        storageDescriptor.getOutputFormat(),
        storageDescriptor.getSerdeInfo().getSerializationLib(),
        hiveSerdeParameter);
  }

  @Override
  public SinkEngineConnector initSinkSchemaManager(BitSailConfiguration commonConf, BitSailConfiguration writerConf) throws Exception {
    return new HiveSinkEngineConnector(commonConf, writerConf);
  }

  /**
   * When add partition in Default HiveOutputFormat, we used the partition location path as the location.
   * for example, the partition whole location is "hdfs://test-db/test_table/date=20220101", we only use
   * the path of this uri, which is "/test_db/test_table/date=20220101". This is working well in HiveOutputFormat.
   * But In DLFOutputFormat, our target table is s3 external table, HMS need whole partition location uri, not path.
   */
  @Override
  public void onSuccessComplete(ProcessResult result) throws Exception {
    super.onSuccessComplete(result);
    long numRows = result.getJobSuccessOutputRecordCount();
    LOG.info("will write {} records to hive partition", numRows);

    Path p = this.tmpOutputFilePath;
    Path successFlagPath = p.suffix("/_SUCCESS");

    try {
      dropPartition();
      LOG.info("Touch successFlagPath " + successFlagPath.getPath());
      HdfsUtils.touchEmptyFile(successFlagPath);
      LOG.info("Rename tmpOutputFilePath to outputFilePath");
      HdfsUtils.rename(this.tmpOutputFilePath, this.outputFilePath, true);

      // means it is normal hive, only use uri path.
      HiveMetaClientUtil.addPartition(hiveConf, db, table, partition, outputFilePath.getPath(), numRows);
    } catch (IOException e) {
      LOG.error("Touch successFlagPath " + successFlagPath.getPath() + " Failed! Due to: " + e.getMessage());
      throw BitSailException.asBitSailException(HiveParqueFormatErrorCode.HDFS_EXCEPTION,
          "Touch successFlagPath " + successFlagPath.getPath() + " Failed! Due to: " + e.getMessage());
    } catch (TException e) {
      LOG.error("Add partitions " + partition + " to " + db + "." + table + " Failed! Due to: " + e.getMessage());
      throw BitSailException.asBitSailException(HiveParqueFormatErrorCode.HIVE_METASTORE_EXCEPTION,
          "Add partitions " + partition + " to " + db + "." + table + " Failed! Due to: " + e.getMessage());
    }
  }

  @Override
  public void onDestroy() throws Exception {
    /**
     *     http://foo:bar@example.com:8042/over/there?name=ferret#nose
     *     \__/   \______________________/\_________/ \_________/ \__/
     *       |               |                |            |        |
     *    scheme         authority           path        query   fragment
     */
    if (tmpOutputFilePath != null && FileSystem.get(tmpOutputFilePath.toUri()).exists(tmpOutputFilePath)) {
      LOG.info("Target temp output path " + tmpOutputFilePath + " already exist. The program will delete it.");
      HdfsUtils.deletePath(tmpOutputFilePath);
    }
    super.onDestroy();
  }

  @Override
  public String getType() {
    return "Hive";
  }

  @Override
  public int getMaxParallelism() {
    return Integer.MAX_VALUE;
  }

  @Override
  public void writeRecordInternal(Row record) throws BitSailException, IOException {
    Object hiveRow = hiveWritableExtractor.createRowObject(record);
    try {
      Writable writable = serDe.serialize(hiveRow, hiveWritableExtractor.getInspector());
      synchronized (actualFilePath) {
        writer.write(writable);
      }
    } catch (SerDeException e) {
      throw new IOException("Deserialize Row to hiveRecord failed!", e);
    } catch (IOException e) {
      throw new IOException("Write one record failed - " + e.getMessage(), e);
    }
  }

  /**
   * generate for hive
   */

  public static enum HiveWritableExtractorType {
    PARQUET, GENERAL
  }

  @AllArgsConstructor
  private class HiveMeta implements Serializable {
    String columns;
    String columnTypes;
    String inputFormat;
    String outputFormat;
    String serializationLib;
    Map<String, String> serdeParameters;
  }

}
