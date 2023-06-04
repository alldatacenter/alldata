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

package com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.format.hive;

import com.bytedance.bitsail.base.metrics.MetricManager;
import com.bytedance.bitsail.base.metrics.manager.CallTracer;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.util.JsonSerializer;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.common.PartitionInfo;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemSinkOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.tools.MetricsFactory;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.tools.PartitionUtils;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.schema.HiveFileSystemMetaManager;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.schema.HiveMeta;
import com.bytedance.bitsail.conversion.hive.ConvertToHiveObjectOptions;
import com.bytedance.bitsail.conversion.hive.extractor.GeneralWritableExtractor;
import com.bytedance.bitsail.conversion.hive.extractor.HiveWritableExtractor;
import com.bytedance.bitsail.conversion.hive.extractor.ParquetWritableExtractor;
import com.bytedance.bitsail.conversion.hive.option.HiveConversionOptions;

import org.apache.flink.api.java.hadoop.common.HadoopOutputFormatCommonBase;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.fs.Path;
import org.apache.flink.types.Row;
import org.apache.hadoop.hive.ql.exec.FileSinkOperator.RecordWriter;
import org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat;
import org.apache.hadoop.hive.ql.io.HiveOutputFormat;
import org.apache.hadoop.hive.serde2.Deserializer;
import org.apache.hadoop.hive.serde2.SerDeException;
import org.apache.hadoop.hive.serde2.SerDeUtils;
import org.apache.hadoop.hive.serde2.Serializer;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.StructField;
import org.apache.hadoop.hive.serde2.objectinspector.StructObjectInspector;
import org.apache.hadoop.mapred.JobConf;
import org.apache.parquet.hadoop.ParquetOutputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.bytedance.bitsail.connector.legacy.streamingfile.common.constants.StreamingFileSystemMetricsNames.HDFS_FILE_WRITE_LATENCY;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.constants.StreamingFileSystemMetricsNames.INVALID_TASK_ID;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.constants.StreamingFileSystemMetricsNames.SUB_TASK_ID;

/**
 * @class: HiveWriteableOutputFormat
 * @desc:
 **/
public class HiveWritableOutputFormat<IN extends Row> extends HadoopOutputFormatCommonBase<IN> {
  public static final boolean CONVERT_ERROR_COLUMN_AS_NULL = false;
  public static final boolean DATE_TYPE_TO_STRING_AS_LONG = false;
  public static final ConvertToHiveObjectOptions.DatePrecision DATE_PRECISION = ConvertToHiveObjectOptions.DatePrecision.SECOND;
  private static final Logger LOG = LoggerFactory.getLogger(HiveWritableOutputFormat.class);
  private static final long serialVersionUID = 3893398155359367104L;
  private static final String HIVE_DEFAULT_CODEC = "gzip";
  /**
   * Construct Parameters
   */
  private final transient boolean useStringText;
  private final transient boolean nullStringAsNull;
  private final transient Path outputPath;
  private final transient BitSailConfiguration jobConf;
  private final transient HiveFileSystemMetaManager fileSystemMetaManager;
  private final transient Map<String, String> hiveOutputFormatPropertyMap;
  /**
   * Columns Parameters
   */
  private final transient List<String> partitionColumns;
  /**
   * RecordWriter Parameters
   */
  private transient RecordWriter recordWriter;
  private transient Serializer recordSerDe;
  private transient String[] fieldNames;
  private transient StructObjectInspector rowObjectInspector;
  private transient List<ObjectInspector> objectInspectors;
  private transient Map<String, Integer> columnMapping;

  /**
   * Open Parameters
   */
  private transient int taskId;
  private transient MetricManager metrics;
  private transient HiveWritableExtractor hiveWritableExtractor;

  public HiveWritableOutputFormat(final BitSailConfiguration jobConf, Path outputPath,
                                  HiveFileSystemMetaManager fileSystemMetaManager) {
    super(new JobConf().getCredentials());

    this.jobConf = jobConf;
    this.outputPath = outputPath;
    this.partitionColumns = PartitionUtils
        .getPartitionInfo(jobConf)
        .stream()
        .map(PartitionInfo::getName)
        .collect(Collectors.toList());

    this.fileSystemMetaManager = fileSystemMetaManager;
    this.hiveOutputFormatPropertyMap = getHiveOutputFormatPropertyMap(jobConf);

    this.useStringText = jobConf.get(HiveConversionOptions.USE_STRING_TEXT);
    this.nullStringAsNull = jobConf.get(HiveConversionOptions.NULL_STRING_AS_NULL);
  }

  @Override
  public void configure(Configuration parameters) {
    taskId = parameters.getInteger(SUB_TASK_ID, INVALID_TASK_ID);
  }

  @Override
  public void open(int taskNumber, int numTasks) throws IOException {
    initHiveWritableExtractor(fileSystemMetaManager.getHiveMeta());
    initColumnParameters(fileSystemMetaManager.getSinkColumnInfos(), fileSystemMetaManager.getHiveMeta());
    initHiveRecordWriter(fileSystemMetaManager.getHiveMeta());
    initObjectInspector(fileSystemMetaManager.getHiveMeta());

    metrics = MetricsFactory.getInstanceMetricsManager(jobConf, taskId);
    this.hiveWritableExtractor.setFieldNames(fieldNames);
    this.hiveWritableExtractor.setColumnMapping(columnMapping);
  }

  @Override
  public void writeRecord(IN record) throws IOException {
    try (CallTracer ignored = metrics.recordTimer(HDFS_FILE_WRITE_LATENCY).get()) {
      try {
        Object hiveRow = hiveWritableExtractor.createRowObject(record);
        this.recordWriter.write(this.recordSerDe.serialize(hiveRow, rowObjectInspector));
      } catch (IOException | SerDeException e) {
        throw new IOException("Could not write Record.", e);
      }
    }
  }

  @Override
  public void close() throws IOException {
    if (recordWriter != null) {
      recordWriter.close(false);
      recordWriter = null;
      recordSerDe = null;
    }
  }

  private Map<String, Integer> getColumnMapping(HiveMeta hiveMeta) {
    String[] columnNames = hiveMeta.getColumns().split(",");
    Map<String, Integer> columnMapping = new HashMap<>();
    for (int i = 0; i < columnNames.length; i++) {
      columnMapping.put(columnNames[i].toUpperCase(), i);
    }
    return columnMapping;
  }

  private Map<String, String> getHiveOutputFormatPropertyMap(final BitSailConfiguration jobConf) {
    Map<String, String> properties = new HashMap<>();
    if (jobConf.fieldExists(FileSystemSinkOptions.HIVE_OUTPUTFORMAT_PROPERTIES)) {
      properties = JsonSerializer.parseToMap(jobConf.get(FileSystemSinkOptions.HIVE_OUTPUTFORMAT_PROPERTIES));
    }
    return properties;
  }

  private void initColumnParameters(List<ColumnInfo> columnInfos, HiveMeta hiveMeta) {
    this.columnMapping = getColumnMapping(hiveMeta);

    this.fieldNames = columnInfos
        .stream()
        .map(ColumnInfo::getName).filter(c -> !partitionColumns.contains(c)).toArray(String[]::new);
  }

  private void initObjectInspector(HiveMeta hiveMeta) {
    this.rowObjectInspector = hiveWritableExtractor.createObjectInspector(hiveMeta.getColumns(), hiveMeta.getColumnTypes());
    this.objectInspectors = new ArrayList<>(rowObjectInspector.getAllStructFieldRefs().size());
    List<? extends StructField> structFields = rowObjectInspector.getAllStructFieldRefs();
    for (StructField structField : structFields) {
      objectInspectors.add(structField.getFieldObjectInspector());
    }
    hiveWritableExtractor.setObjectInspectors(objectInspectors);
  }

  private void initHiveRecordWriter(HiveMeta hiveMeta) throws IOException {
    HiveOutputFormat outputFormat;
    Properties tableProps = new Properties();
    hiveMeta.getSerdeParameters().forEach(tableProps::setProperty);
    tableProps.setProperty("columns", hiveMeta.getColumns());
    tableProps.setProperty("columns.types", hiveMeta.getColumnTypes());
    if (this.jobConf.fieldExists(FileSystemSinkOptions.PARQUET_COMPRESSION)) {
      tableProps.setProperty(ParquetOutputFormat.COMPRESSION, jobConf.get(FileSystemSinkOptions.PARQUET_COMPRESSION));
    } else {
      tableProps.setProperty(ParquetOutputFormat.COMPRESSION, HIVE_DEFAULT_CODEC);
    }

    try {
      outputFormat = (HiveOutputFormat) Class.forName(hiveMeta.getOutputFormat()).newInstance();
      recordSerDe = (Serializer) Class.forName(hiveMeta.getSerializationLib()).newInstance();
      SerDeUtils.initializeSerDe((Deserializer) recordSerDe, new org.apache.hadoop.conf.Configuration(), tableProps, null);
    } catch (Exception e) {
      throw new RuntimeException("Unable to init hive output format", e);
    }

    // Translate to hadoop path class
    org.apache.hadoop.fs.Path taskOutputPath = new org.apache.hadoop.fs.Path(this.outputPath.toString());
    boolean isCompressed = !(outputFormat instanceof HiveIgnoreKeyTextOutputFormat);

    org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration();
    JobConf writerConf;
    if (this.hiveOutputFormatPropertyMap.size() > 0) {
      LOG.info("set record writer conf: " + JsonSerializer.serialize(this.hiveOutputFormatPropertyMap));
      for (String key : hiveOutputFormatPropertyMap.keySet()) {
        String value = hiveOutputFormatPropertyMap.get(key);
        conf.set(key, value);
      }
      writerConf = new JobConf(conf);
    } else {
      writerConf = new JobConf();
    }

    // for support emr ha cluster
    if (Objects.nonNull(jobConf.get(FileSystemSinkOptions.HIVE_METASTORE_PROPERTIES))) {
      LOG.info("set hadoop config to writerConf");
      Map<String, String> hadoopConf =
          JsonSerializer.parseToMap(jobConf.get(FileSystemSinkOptions.HIVE_METASTORE_PROPERTIES));
      for (String key : hadoopConf.keySet()) {
        String value = hadoopConf.get(key);
        writerConf.set(key, value);
      }
    }

    this.recordWriter = outputFormat.getHiveRecordWriter(writerConf, taskOutputPath, org.apache.hadoop.io.Text.class, isCompressed, tableProps, null);
  }

  private void initHiveWritableExtractor(HiveMeta hiveMeta) {
    String extractorType = jobConf.getUnnecessaryValue(HiveConversionOptions.WRITABLE_EXTRACTOR_TYPE.key(),
        HiveConversionOptions.WRITABLE_EXTRACTOR_TYPE.defaultValue());
    if ("parquet".equalsIgnoreCase(extractorType)) {
      this.hiveWritableExtractor = new ParquetWritableExtractor();
    } else {
      this.hiveWritableExtractor = new GeneralWritableExtractor();
    }

    ConvertToHiveObjectOptions convertOptions = ConvertToHiveObjectOptions.builder()
        .jobType(ConvertToHiveObjectOptions.JobType.DUMP)
        .convertErrorColumnAsNull(CONVERT_ERROR_COLUMN_AS_NULL)
        .dateTypeToStringAsLong(DATE_TYPE_TO_STRING_AS_LONG)
        .datePrecision(DATE_PRECISION)
        .nullStringAsNull(nullStringAsNull)
        .useStringText(useStringText)
        .build();
    hiveWritableExtractor.initConvertOptions(convertOptions);
  }

  @Override
  public String toString() {
    return "HiveWritableOutputFormat";
  }
}
