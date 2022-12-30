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

package com.bytedance.bitsail.connector.hbase.source;

import com.bytedance.bitsail.base.extension.ParallelismComputable;
import com.bytedance.bitsail.base.format.DeserializationFormat;
import com.bytedance.bitsail.base.format.DeserializationSchema;
import com.bytedance.bitsail.base.parallelism.ParallelismAdvice;
import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.constants.Constants;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.option.ReaderOptions;
import com.bytedance.bitsail.common.type.filemapping.FileMappingTypeInfoConverter;
import com.bytedance.bitsail.common.typeinfo.TypeInfo;
import com.bytedance.bitsail.common.typeinfo.TypeInfoUtils;
import com.bytedance.bitsail.common.util.Preconditions;
import com.bytedance.bitsail.component.format.hbase.HBaseDeserializationFormat;
import com.bytedance.bitsail.connector.hadoop.source.HadoopInputFormatCommonBasePlugin;
import com.bytedance.bitsail.connector.hbase.HBaseHelper;
import com.bytedance.bitsail.connector.hbase.error.HBasePluginErrorCode;
import com.bytedance.bitsail.connector.hbase.option.HBaseReaderOptions;
import com.bytedance.bitsail.connector.hbase.source.split.RegionSplit;

import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.io.DefaultInputSplitAssigner;
import org.apache.flink.api.common.io.statistics.BaseStatistics;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.io.InputSplit;
import org.apache.flink.core.io.InputSplitAssigner;
import org.apache.flink.types.Row;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableInputFormat;
import org.apache.hadoop.hbase.mapreduce.TableSplit;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.JobContext;
import org.apache.hadoop.mapred.JobContextImpl;
import org.apache.hadoop.mapred.JobID;
import org.apache.hadoop.mapred.TaskAttemptContext;
import org.apache.hadoop.mapred.TaskAttemptContextImpl;
import org.apache.hadoop.mapred.TaskAttemptID;
import org.apache.hadoop.mapreduce.RecordReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class HBaseInputFormat extends HadoopInputFormatCommonBasePlugin<Row, InputSplit>
    implements ResultTypeQueryable<Row>, ParallelismComputable {
  private static final Logger LOG = LoggerFactory.getLogger(HBaseInputFormat.class);
  private static final String ROW_KEY = "rowkey";

  /**
   * Mutexes for avoiding concurrent operations on Hadoop InputFormats.
   * Hadoop runs tasks across JVMs, so JVM isolation ensure there is no concurrency issue.
   * In contrast, Flink runs tasks in the same JVM, so multiple Hadoop InputFormat instances
   * might be used in the same JVM, and we need to manually avoid concurrency conflict.
   */
  private static final Object OPEN_MUTEX = new Object();
  private static final Object CONFIGURE_MUTEX = new Object();
  private static final Object CLOSE_MUTEX = new Object();

  private static final Retryer<Object> RETRYER = RetryerBuilder.newBuilder()
      .retryIfException()
      .withWaitStrategy(WaitStrategies.fixedWait(Constants.RETRY_DELAY, TimeUnit.MILLISECONDS))
      .withStopStrategy(StopStrategies.stopAfterAttempt(Constants.RETRY_TIMES))
      .build();

  /**
   * InputFormat natively supported by HBase.
   */
  private transient TableInputFormat tableInputFormat;

  /**
   * Scanner for {@link org.apache.hadoop.mapreduce.InputSplit}.
   */
  private transient RecordReader<ImmutableBytesWritable, Result> tableReader;

  /**
   * Fake JobContext for constructing {@link TableInputFormat}.
   */
  private transient JobContext jobContext;

  /**
   * Used to de duplicate user-defined fields with the same name.
   */
  private transient Map<String, byte[][]> namesMap;

  /**
   * Schema Settings.
   */
  private String tableName;

  private transient Result value;

  private TypeInfo<?>[] typeInfos;
  private RowTypeInfo rowTypeInfo;
  private List<String> columnNames;
  private Set<String> columnFamilies;
  private transient DeserializationFormat<byte[][], Row> deserializationFormat;
  private transient DeserializationSchema<byte[][], Row> deserializationSchema;

  /**
   * Parameters for Hbase/TableInputFormat.
   */
  private Map<String, Object> hbaseConfig;

  /**
   * Number of regions, used for computing parallelism.
   */
  private int regionCount;

  public HBaseInputFormat() {
    super(new JobConf().getCredentials());
  }

  @Override
  public void initPlugin() throws Exception {
    this.tableName = inputSliceConfig.get(HBaseReaderOptions.TABLE);
    this.hbaseConfig = inputSliceConfig.get(HBaseReaderOptions.HBASE_CONF);

    this.columnFamilies = new LinkedHashSet<>();
    List<ColumnInfo> columnInfos = inputSliceConfig.getNecessaryOption(
        HBaseReaderOptions.COLUMNS, HBasePluginErrorCode.REQUIRED_VALUE);
    typeInfos =
        TypeInfoUtils.getTypeInfos(new FileMappingTypeInfoConverter(StringUtils.lowerCase(getType())), columnInfos);

    columnNames = columnInfos.stream().map(ColumnInfo::getName)
        .collect(Collectors.toList());

    // Get region numbers in hbase.
    regionCount = (int) RETRYER.call(() -> {
      Connection hbaseClient = HBaseHelper.getHbaseConnection(hbaseConfig);
      return hbaseClient.getAdmin().getRegions(TableName.valueOf(tableName)).size();
    });
    LOG.info("Got HBase region count {} to set Flink parallelism", regionCount);

    // Check if input column names are in format: [ columnFamily:column ].
    columnNames.stream().peek(column -> Preconditions.checkArgument(
            (column.contains(":") && column.split(":").length == 2) || ROW_KEY.equalsIgnoreCase(column),
            "Invalid column names, it should be [ColumnFamily:Column] format"))
        .forEach(column -> columnFamilies.add(column.split(":")[0]));
  }

  /**
   * Open {@link TableInputFormat} in each task.
   */
  @Override
  public void openInputFormat() throws IOException {
    super.openInputFormat();
    tableInputFormat = new TableInputFormat();
    tableInputFormat.setConf(getConf());
    namesMap = Maps.newConcurrentMap();
    deserializationFormat = new HBaseDeserializationFormat(inputSliceConfig);
    deserializationSchema = deserializationFormat.createRuntimeDeserializationSchema(typeInfos);
    LOG.info("Starting config HBase input format, maybe so slow........");
  }

  @Override
  public Row buildRow(Row reuse, String mandatoryEncoding) throws BitSailException {
    byte[][] rawRow = new byte[columnNames.size()][];
    for (int i = 0; i < columnNames.size(); ++i) {
      String columnName = columnNames.get(i);
      byte[] bytes;
      try {
        // If it is rowkey defined by users, directly use it.
        if (ROW_KEY.equals(columnName)) {
          bytes = value.getRow();
        } else {
          byte[][] arr = namesMap.get(columnName);
          // Deduplicate
          if (Objects.isNull(arr)) {
            arr = new byte[2][];
            String[] arr1 = columnName.split(":");
            arr[0] = arr1[0].trim().getBytes(StandardCharsets.UTF_8);
            arr[1] = arr1[1].trim().getBytes(StandardCharsets.UTF_8);
            namesMap.put(columnName, arr);
          }
          bytes = value.getValue(arr[0], arr[1]);
        }
        rawRow[i] = bytes;
      } catch (Exception e) {
        LOG.error("Cannot read data from {}, reason: \n", tableName, e);
      }
    }
    reuse = deserializationSchema.deserialize(rawRow);
    return reuse;
  }

  @Override
  public boolean isSplitEnd() throws IOException {
    try {
      value = tableReader.nextKeyValue() ? tableReader.getCurrentValue() : null;
      return !(hasNext = !(value == null));
    } catch (InterruptedException e) {
      hasNext = false;
      LOG.error("Got hbase next value failed, prev value is {},  maybe cause data loss", value);
      return true;
    }
  }

  @Override
  public InputSplit[] createSplits(int minNumSplits) throws IOException {
    LOG.info("begin to get split, may take some time ...");
    List<org.apache.hadoop.mapreduce.InputSplit> splits = this.tableInputFormat.getSplits(jobContext);
    LOG.info("Finally get {} splits from hbase", splits.size());
    return splits.stream().map(split -> new RegionSplit((TableSplit) split)).toArray(InputSplit[]::new);
  }

  /**
   * Firstly initialize {@link TableInputFormat} to create splits.
   *
   * @param parameters Parameters for connection.
   */
  @Override
  public void configure(Configuration parameters) {
    super.configure(parameters);
    // enforce sequential configuration() calls
    synchronized (CONFIGURE_MUTEX) {
      // configure MR InputFormat if necessary
      JobConf jobConf = getConf();
      tableInputFormat = new TableInputFormat();
      tableInputFormat.setConf(jobConf);
      jobContext = new JobContextImpl(jobConf, new JobID());
    }
  }

  @Override
  public String getType() {
    return "HBase";
  }

  @Override
  public ParallelismAdvice getParallelismAdvice(BitSailConfiguration commonConf,
                                                BitSailConfiguration selfConf,
                                                ParallelismAdvice upstreamAdvice) throws Exception {
    int parallelism = inputSliceConfig.getUnNecessaryOption(ReaderOptions.BaseReaderOptions.READER_PARALLELISM_NUM, -1);
    if (parallelism > regionCount || parallelism < 0) {
      LOG.info("Reader parallelism is amended to shard count: {}, origin parallelism is: {}", regionCount, parallelism);
      parallelism = regionCount;
    }
    return ParallelismAdvice.builder()
        .adviceParallelism(parallelism)
        .enforceDownStreamChain(false)
        .build();
  }

  @Override
  public BaseStatistics getStatistics(BaseStatistics baseStatistics) {
    return new BaseStatistics() {
      @Override
      public long getTotalInputSize() {
        return SIZE_UNKNOWN;
      }

      @Override
      public long getNumberOfRecords() {
        return SIZE_UNKNOWN;
      }

      @Override
      public float getAverageRecordWidth() {
        return AVG_RECORD_BYTES_UNKNOWN;
      }
    };
  }

  @Override
  public InputSplitAssigner getInputSplitAssigner(InputSplit[] inputSplits) {
    return new DefaultInputSplitAssigner(inputSplits);
  }

  @Override
  public void open(InputSplit inputSplit) throws IOException {
    // InputSplit uses preemptive acquisition.
    // So if a region has too many splits, it will be acquired by too much tasks, leading to high overload.
    try {
      synchronized (OPEN_MUTEX) {
        JobConf conf = getConf();
        TaskAttemptContext taskContext = new TaskAttemptContextImpl(conf, new TaskAttemptID());
        TableSplit tableSplit = toTableSplit(inputSplit, columnFamilies);
        this.tableReader = tableInputFormat.createRecordReader(tableSplit, taskContext);
        this.tableReader.initialize(tableSplit, taskContext);
        this.value = tableReader.getCurrentValue();
      }
    } catch (InterruptedException iee) {
      LOG.error("Open HBase table reader failed, reason: {}", iee.toString());
      throw BitSailException.asBitSailException(HBasePluginErrorCode.EXTERNAL_ERROR, "Open HBase Connection Failed", iee);
    }
  }

  /**
   * Transform {@link RegionSplit} into splits that can be accepted by {@link TableInputFormat}.
   *
   * @param inputSplit {@link RegionSplit}.
   * @param cfs        Column families.
   * @return Splits read by {@link TableInputFormat}.
   */
  private TableSplit toTableSplit(InputSplit inputSplit, Set<String> cfs) {
    RegionSplit split = (RegionSplit) inputSplit;
    Scan scan = new Scan();
    cfs.forEach(cf -> scan.addFamily(Bytes.toBytes(cf)));
    return new TableSplit(TableName.valueOf(split.getTableName()), scan,
        split.getStartKey(), split.getEndKey(),
        split.getRegionLocation(), split.getRegionName(), split.getLength());
  }

  @Override
  public void close() throws IOException {
    if (this.tableReader != null) {
      // enforce sequential close() calls
      synchronized (CLOSE_MUTEX) {
        this.tableReader.close();
      }
    }
  }

  @Override
  public TypeInformation<Row> getProducedType() {
    return rowTypeInfo;
  }

  public JobConf getConf() {
    JobConf jobConf = new JobConf(false);
    jobConf.set(TableInputFormat.INPUT_TABLE, tableName);
    hbaseConfig.forEach((key, value) -> jobConf.set(key, value.toString()));
    return jobConf;
  }
}
