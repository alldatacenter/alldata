/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.connector.legacy.hudi.dag;

import com.bytedance.bitsail.base.execution.ExecutionEnviron;
import com.bytedance.bitsail.base.parallelism.ParallelismAdvice;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.ReaderOptions;
import com.bytedance.bitsail.connector.legacy.hudi.configuration.FlinkOptions;
import com.bytedance.bitsail.connector.legacy.hudi.configuration.HadoopConfigurations;
import com.bytedance.bitsail.connector.legacy.hudi.format.RowDataToRowFunction;
import com.bytedance.bitsail.connector.legacy.hudi.source.FileIndex;
import com.bytedance.bitsail.connector.legacy.hudi.source.IncrementalInputSplits;
import com.bytedance.bitsail.connector.legacy.hudi.source.format.FilePathUtils;
import com.bytedance.bitsail.connector.legacy.hudi.source.format.cow.CopyOnWriteInputFormat;
import com.bytedance.bitsail.connector.legacy.hudi.source.format.mor.MergeOnReadInputFormat;
import com.bytedance.bitsail.connector.legacy.hudi.source.format.mor.MergeOnReadInputSplit;
import com.bytedance.bitsail.connector.legacy.hudi.source.format.mor.MergeOnReadTableState;
import com.bytedance.bitsail.connector.legacy.hudi.util.AvroSchemaConverter;
import com.bytedance.bitsail.connector.legacy.hudi.util.InputFormats;
import com.bytedance.bitsail.connector.legacy.hudi.util.StreamerUtil;
import com.bytedance.bitsail.flink.core.execution.FlinkExecutionEnviron;
import com.bytedance.bitsail.flink.core.reader.FlinkDataReaderDAGBuilder;

import lombok.Getter;
import org.apache.avro.Schema;
import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.api.common.io.FileInputFormat;
import org.apache.flink.api.common.io.FilePathFilter;
import org.apache.flink.api.common.io.InputFormat;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.source.InputFormatSourceFunction;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.runtime.types.TypeInfoDataTypeConverter;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.types.Row;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hudi.common.model.BaseFile;
import org.apache.hudi.common.model.HoodieLogFile;
import org.apache.hudi.common.model.HoodieTableType;
import org.apache.hudi.common.table.HoodieTableMetaClient;
import org.apache.hudi.common.table.TableSchemaResolver;
import org.apache.hudi.common.table.view.HoodieTableFileSystemView;
import org.apache.hudi.common.util.Option;
import org.apache.hudi.exception.HoodieException;
import org.apache.hudi.hadoop.HoodieROTablePathFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.bytedance.bitsail.connector.legacy.hudi.configuration.HadoopConfigurations.getParquetConf;

public class HudiSourceFunctionDAGBuilder<T extends Row> extends FlinkDataReaderDAGBuilder<T> {

  private static final Logger LOG = LoggerFactory.getLogger(HudiSourceFunctionDAGBuilder.class);

  @Getter
  private Configuration conf;

  @Getter
  private BitSailConfiguration jobConf;

  /**
   * Meta Client.
   */
  private HoodieTableMetaClient metaClient;

  private Path path;

  private FileIndex fileIndex;

  private long maxCompactionMemoryInBytes;

  private transient org.apache.hadoop.conf.Configuration hadoopConf;

  private DataType rowDataType;

  private Schema avroSchema;

  private List<String> fields;

  private List<String> partitionKeys;

  private List<Map<String, String>> requiredPartitions;

  private int[] requiredPos;

  @Override
  public void configure(ExecutionEnviron execution, BitSailConfiguration readerConfiguration) throws Exception {
    this.jobConf = execution.getGlobalConfiguration();
    Map<String, String> properties = jobConf.getFlattenMap(ReaderOptions.READER_PREFIX);
    this.conf = FlinkOptions.fromMap(properties);
    this.path = new Path(conf.get(FlinkOptions.PATH));
    this.maxCompactionMemoryInBytes = StreamerUtil.getMaxCompactionMemoryInBytes(conf);
    this.fileIndex = FileIndex.instance(this.path, this.conf);
    this.hadoopConf = HadoopConfigurations.getHadoopConf(conf);
    this.metaClient = StreamerUtil.metaClientForReader(conf, hadoopConf);
    TableSchemaResolver schemaResolver = new TableSchemaResolver(metaClient);
    this.avroSchema = schemaResolver.getTableAvroSchema();
    this.fields = avroSchema.getFields().stream().map(Schema.Field::name).collect(Collectors.toList());
    this.rowDataType = AvroSchemaConverter.convertToDataType(avroSchema);
    // TODO: pass partition key
    this.partitionKeys = new ArrayList<>();
    partitionKeys.add("partition");
    this.requiredPartitions = null;
    // TODO: support column pruning
    this.requiredPos = IntStream.range(0, fields.size()).toArray();
  }

  @Override
  public boolean validate() throws Exception {
    return true;
  }

  @Override
  public String getReaderName() {
    return "hudi_source";
  }

  @Override
  public ParallelismAdvice getParallelismAdvice(BitSailConfiguration commonConf, BitSailConfiguration readerConf, ParallelismAdvice upstreamAdvice) throws Exception {
    //TODO: Advice parallelism based on file stats
    Integer userConfigReaderParallelism = readerConf.getUnNecessaryOption(ReaderOptions.BaseReaderOptions.READER_PARALLELISM_NUM, 1);

    return ParallelismAdvice.builder()
      .adviceParallelism(userConfigReaderParallelism)
      .enforceDownStreamChain(false)
      .build();
  }

  @Override
  public DataStream<T> addSource(FlinkExecutionEnviron executionEnviron, int readerParallelism) throws Exception {
    TypeInformation<RowData> typeInfo =
        (TypeInformation<RowData>) TypeInfoDataTypeConverter.fromDataTypeToTypeInfo(getProducedDataType());
    org.apache.flink.streaming.api.functions.source.InputFormatSourceFunction<RowData> func =
        new InputFormatSourceFunction<>(getBatchInputFormat(), typeInfo);

    DataStream<Row> source = executionEnviron.getExecutionEnvironment()
        .addSource(func, typeInfo)
        .name(getReaderName())
        .setParallelism(readerParallelism)
        .map(new RowDataToRowFunction<>(this.rowDataType), TypeInformation.of(Row.class))
        .setParallelism(readerParallelism);
    return (DataStream<T>) source;
  }

  private DataType getProducedDataType() {
    String[] schemaFieldNames = this.fields.toArray(new String[0]);
    DataType[] schemaTypes = this.rowDataType.getChildren().toArray(new DataType[0]);

    return DataTypes.ROW(Arrays.stream(this.requiredPos)
            .mapToObj(i -> DataTypes.FIELD(schemaFieldNames[i], schemaTypes[i]))
            .toArray(DataTypes.Field[]::new))
            .bridgedTo(RowData.class);
  }

  private InputFormat<RowData, ?> getBatchInputFormat() throws Exception {
    final RowType rowType = (RowType) rowDataType.getLogicalType();
    final RowType requiredRowType = rowType; // placeholder for column pruning

    final String queryType = this.conf.getString(FlinkOptions.QUERY_TYPE);
    switch (queryType) {
      case FlinkOptions.QUERY_TYPE_SNAPSHOT:
        final HoodieTableType tableType = HoodieTableType.valueOf(this.conf.getString(FlinkOptions.TABLE_TYPE));
        switch (tableType) {
          case MERGE_ON_READ:
            final List<MergeOnReadInputSplit> inputSplits = buildFileIndex();
            if (inputSplits.size() == 0) {
              // When there is no input splits, just return an empty source.
              LOG.warn("No input splits generate for MERGE_ON_READ input format, returns empty collection instead");
              return InputFormats.EMPTY_INPUT_FORMAT;
            }
            return mergeOnReadInputFormat(rowType, requiredRowType, avroSchema,
              rowDataType, inputSplits, false);
          case COPY_ON_WRITE:
            return baseFileOnlyInputFormat();
          default:
            throw new HoodieException("Unexpected table type: " + this.conf.getString(FlinkOptions.TABLE_TYPE));
        }
      case FlinkOptions.QUERY_TYPE_READ_OPTIMIZED:
        return baseFileOnlyInputFormat();
      case FlinkOptions.QUERY_TYPE_INCREMENTAL:
        IncrementalInputSplits incrementalInputSplits = IncrementalInputSplits.builder()
            .conf(conf).path(FilePathUtils.toFlinkPath(path))
            .maxCompactionMemoryInBytes(maxCompactionMemoryInBytes)
            .requiredPartitions(getRequiredPartitionPaths()).build();
        final IncrementalInputSplits.Result result = incrementalInputSplits.inputSplits(metaClient, hadoopConf);
        if (result.isEmpty()) {
          // When there is no input splits, just return an empty source.
          LOG.warn("No input splits generate for incremental read, returns empty collection instead");
          return InputFormats.EMPTY_INPUT_FORMAT;
        }
        return mergeOnReadInputFormat(rowType, requiredRowType, avroSchema,
            rowDataType, result.getInputSplits(), false);
      default:
        String errMsg = String.format("Invalid query type : '%s', options ['%s', '%s', '%s'] are supported now", queryType,
            FlinkOptions.QUERY_TYPE_SNAPSHOT, FlinkOptions.QUERY_TYPE_READ_OPTIMIZED, FlinkOptions.QUERY_TYPE_INCREMENTAL);
        throw new HoodieException(errMsg);
    }
  }

  @Nullable
  private Set<String> getRequiredPartitionPaths() {
    if (this.requiredPartitions == null) {
      // returns null for non partition pruning
      return null;
    }
    return FilePathUtils.toRelativePartitionPaths(this.partitionKeys, this.requiredPartitions,
        conf.getBoolean(FlinkOptions.HIVE_STYLE_PARTITIONING));
  }

  private List<MergeOnReadInputSplit> buildFileIndex() {
    Set<String> requiredPartitionPaths = getRequiredPartitionPaths();
    fileIndex.setPartitionPaths(requiredPartitionPaths);
    List<String> relPartitionPaths = fileIndex.getOrBuildPartitionPaths();
    if (relPartitionPaths.size() == 0) {
      return Collections.emptyList();
    }
    FileStatus[] fileStatuses = fileIndex.getFilesInPartitions();
    if (fileStatuses.length == 0) {
      throw new HoodieException("No files found for reading in user provided path.");
    }

    // file-slice after pending compaction-requested instant-time is also considered valid
    HoodieTableFileSystemView fsView = new HoodieTableFileSystemView(metaClient,
        metaClient.getCommitsAndCompactionTimeline().filterCompletedAndCompactionInstants(), fileStatuses);
    String latestCommit = fsView.getLastInstant().get().getTimestamp();
    final String mergeType = this.conf.getString(FlinkOptions.MERGE_TYPE);
    final AtomicInteger cnt = new AtomicInteger(0);
    // generates one input split for each file group
    return relPartitionPaths.stream()
        .map(relPartitionPath -> fsView.getLatestMergedFileSlicesBeforeOrOn(relPartitionPath, latestCommit)
            .map(fileSlice -> {
              String basePath = fileSlice.getBaseFile().map(BaseFile::getPath).orElse(null);
              Option<List<String>> logPaths = Option.ofNullable(fileSlice.getLogFiles()
                  .sorted(HoodieLogFile.getLogFileComparator())
                  .map(logFile -> logFile.getPath().toString())
                  .collect(Collectors.toList()));
              return new MergeOnReadInputSplit(cnt.getAndAdd(1), basePath, logPaths, latestCommit,
                  metaClient.getBasePath(), maxCompactionMemoryInBytes, mergeType, null, fileSlice.getFileId());
            }).collect(Collectors.toList()))
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  private MergeOnReadInputFormat mergeOnReadInputFormat(
      RowType rowType,
      RowType requiredRowType,
      Schema tableAvroSchema,
      DataType rowDataType,
      List<MergeOnReadInputSplit> inputSplits,
      boolean emitDelete) {
    final MergeOnReadTableState hoodieTableState = new MergeOnReadTableState(
        rowType,
        requiredRowType,
        tableAvroSchema.toString(),
        AvroSchemaConverter.convertToSchema(requiredRowType).toString(),
        inputSplits,
        conf.getString(FlinkOptions.RECORD_KEY_FIELD).split(","));
    return MergeOnReadInputFormat.builder()
        .config(this.conf)
        .tableState(hoodieTableState)
        // use the explicit fields' data type because the AvroSchemaConverter
        // is not very stable.
        .fieldTypes(rowDataType.getChildren())
        .defaultPartName(conf.getString(FlinkOptions.PARTITION_DEFAULT_NAME))
        //.limit(this.limit)
        .emitDelete(emitDelete)
        .build();
  }

  private InputFormat<RowData, ?> baseFileOnlyInputFormat() {
    final Path[] paths = getReadPaths();
    if (paths.length == 0) {
      return InputFormats.EMPTY_INPUT_FORMAT;
    }
    FileInputFormat<RowData> format = new CopyOnWriteInputFormat(
        FilePathUtils.toFlinkPaths(paths),
        this.fields.toArray(new String[0]),
        this.rowDataType.getChildren().toArray(new DataType[0]),
        this.requiredPos,
        this.conf.getString(FlinkOptions.PARTITION_DEFAULT_NAME),
        Long.MAX_VALUE, // TODO: Support user defined limit
        getParquetConf(this.conf, this.hadoopConf),
        this.conf.getBoolean(FlinkOptions.UTC_TIMEZONE)
    );
    format.setFilesFilter(new LatestFileFilter(this.hadoopConf));
    return format;
  }

  /**
   * Get the reader paths with partition path expanded.
   */
  @VisibleForTesting
  public Path[] getReadPaths() {
    return partitionKeys.isEmpty() ? new Path[] {path}
        : FilePathUtils.partitionPath2ReadPath(path, partitionKeys, getOrFetchPartitions(),
        conf.getBoolean(FlinkOptions.HIVE_STYLE_PARTITIONING));
  }

  private List<Map<String, String>> getOrFetchPartitions() {
    if (requiredPartitions == null) {
      requiredPartitions = listPartitions().orElse(Collections.emptyList());
    }
    return requiredPartitions;
  }

  public Optional<List<Map<String, String>>> listPartitions() {
    List<Map<String, String>> partitions = this.fileIndex.getPartitions(
        this.partitionKeys, conf.getString(FlinkOptions.PARTITION_DEFAULT_NAME), conf.getBoolean(FlinkOptions.HIVE_STYLE_PARTITIONING));
    return Optional.of(partitions);
  }

  private static class LatestFileFilter extends FilePathFilter {
    private final HoodieROTablePathFilter hoodieFilter;

    public LatestFileFilter(org.apache.hadoop.conf.Configuration hadoopConf) {
      this.hoodieFilter = new HoodieROTablePathFilter(hadoopConf);
    }

    @Override
    public boolean filterPath(org.apache.flink.core.fs.Path filePath) {
      return !this.hoodieFilter.accept(new Path(filePath.toUri()));
    }
  }
}
