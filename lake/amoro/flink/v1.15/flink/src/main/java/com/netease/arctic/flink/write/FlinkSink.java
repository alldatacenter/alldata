/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.flink.write;

import com.netease.arctic.flink.metric.MetricsGenerator;
import com.netease.arctic.flink.shuffle.RoundRobinShuffleRulePolicy;
import com.netease.arctic.flink.shuffle.ShuffleHelper;
import com.netease.arctic.flink.shuffle.ShuffleKey;
import com.netease.arctic.flink.shuffle.ShuffleRulePolicy;
import com.netease.arctic.flink.table.ArcticTableLoader;
import com.netease.arctic.flink.table.descriptors.ArcticValidator;
import com.netease.arctic.flink.util.ArcticUtils;
import com.netease.arctic.flink.util.CompatibleFlinkPropertyUtil;
import com.netease.arctic.flink.util.IcebergClassUtil;
import com.netease.arctic.flink.util.ProxyUtil;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.DistributionHashMode;
import com.netease.arctic.table.TableProperties;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.typeutils.TypeExtractor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.functions.sink.DiscardingSink;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.connector.ProviderContext;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.util.Preconditions;
import org.apache.iceberg.DistributionMode;
import org.apache.iceberg.Schema;
import org.apache.iceberg.flink.FlinkSchemaUtil;
import org.apache.iceberg.flink.sink.TaskWriterFactory;
import org.apache.iceberg.io.WriteResult;
import org.apache.iceberg.types.TypeUtil;
import org.apache.iceberg.util.PropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Properties;

import static com.netease.arctic.flink.table.descriptors.ArcticValidator.ARCTIC_EMIT_FILE;
import static com.netease.arctic.flink.table.descriptors.ArcticValidator.ARCTIC_EMIT_MODE;
import static com.netease.arctic.flink.table.descriptors.ArcticValidator.ARCTIC_THROUGHPUT_METRIC_ENABLE;
import static com.netease.arctic.flink.table.descriptors.ArcticValidator.ARCTIC_THROUGHPUT_METRIC_ENABLE_DEFAULT;
import static com.netease.arctic.flink.table.descriptors.ArcticValidator.ARCTIC_WRITE_MAX_OPEN_FILE_SIZE;
import static com.netease.arctic.flink.table.descriptors.ArcticValidator.ARCTIC_WRITE_MAX_OPEN_FILE_SIZE_DEFAULT;
import static com.netease.arctic.flink.table.descriptors.ArcticValidator.AUTO_EMIT_LOGSTORE_WATERMARK_GAP;
import static com.netease.arctic.flink.table.descriptors.ArcticValidator.SUBMIT_EMPTY_SNAPSHOTS;
import static com.netease.arctic.table.TableProperties.WRITE_DISTRIBUTION_HASH_MODE;
import static com.netease.arctic.table.TableProperties.WRITE_DISTRIBUTION_HASH_MODE_DEFAULT;
import static com.netease.arctic.table.TableProperties.WRITE_DISTRIBUTION_MODE;
import static com.netease.arctic.table.TableProperties.WRITE_DISTRIBUTION_MODE_DEFAULT;
import static org.apache.flink.table.factories.FactoryUtil.SINK_PARALLELISM;

/**
 * An util generates arctic sink operator including log writer, file writer and file committer operators.
 */
public class FlinkSink {
  private static final Logger LOG = LoggerFactory.getLogger(FlinkSink.class);

  public static final String FILES_COMMITTER_NAME = "FilesCommitter";

  public static Builder forRowData(DataStream<RowData> input) {
    return new Builder().forRowData(input);
  }

  public static class Builder {
    private DataStream<RowData> rowDataInput = null;
    private ProviderContext context;
    private ArcticTable table;
    private ArcticTableLoader tableLoader;
    private TableSchema flinkSchema;
    private Properties producerConfig;
    private String topic;
    private boolean overwrite = false;
    private DistributionHashMode distributionMode = null;

    private Builder() {
    }

    private Builder forRowData(DataStream<RowData> newRowDataInput) {
      this.rowDataInput = newRowDataInput;
      return this;
    }

    public Builder context(ProviderContext context) {
      this.context = context;
      return this;
    }

    public Builder table(ArcticTable table) {
      this.table = table;
      return this;
    }

    public Builder flinkSchema(TableSchema flinkSchema) {
      this.flinkSchema = flinkSchema;
      return this;
    }

    public Builder producerConfig(Properties producerConfig) {
      this.producerConfig = producerConfig;
      return this;
    }

    public Builder topic(String topic) {
      this.topic = topic;
      return this;
    }

    public Builder tableLoader(ArcticTableLoader tableLoader) {
      this.tableLoader = tableLoader;
      return this;
    }

    public Builder overwrite(boolean overwrite) {
      this.overwrite = overwrite;
      return this;
    }

    public Builder distribute(DistributionHashMode distributionMode) {
      this.distributionMode = distributionMode;
      return this;
    }

    DataStreamSink<?> withEmit(
        DataStream<RowData> input,
        ArcticLogWriter logWriter,
        ArcticFileWriter fileWriter,
        OneInputStreamOperator<WriteResult, Void> committer,
        int writeOperatorParallelism,
        MetricsGenerator metricsGenerator,
        String arcticEmitMode) {
      SingleOutputStreamOperator writerStream = input
          .transform(ArcticWriter.class.getName(), TypeExtractor.createTypeInfo(WriteResult.class),
              new ArcticWriter<>(logWriter, fileWriter, metricsGenerator))
          .name(String.format("ArcticWriter %s(%s)", table.name(), arcticEmitMode))
          .setParallelism(writeOperatorParallelism);

      if (committer != null) {
        writerStream = writerStream.transform(FILES_COMMITTER_NAME, Types.VOID, committer)
            .setParallelism(1)
            .setMaxParallelism(1);
      }

      return writerStream.addSink(new DiscardingSink<>())
          .name(String.format("ArcticSink %s", table.name()))
          .setParallelism(1);
    }

    public DataStreamSink<?> build() {
      Preconditions.checkNotNull(tableLoader, "table loader can not be null");
      initTableIfNeeded();

      Configuration config = new Configuration();
      table.properties().forEach(config::setString);

      RowType flinkSchemaRowType = (RowType) flinkSchema.toRowDataType().getLogicalType();
      Schema writeSchema = TypeUtil.reassignIds(FlinkSchemaUtil.convert(flinkSchema), table.schema());

      int writeOperatorParallelism = PropertyUtil.propertyAsInt(table.properties(), SINK_PARALLELISM.key(),
          rowDataInput.getExecutionEnvironment().getParallelism());

      DistributionHashMode distributionMode = getDistributionHashMode();
      LOG.info("take effect distribute mode: {}", distributionMode);
      ShuffleHelper helper = ShuffleHelper.build(table, writeSchema, flinkSchemaRowType);

      ShuffleRulePolicy<RowData, ShuffleKey>
          shufflePolicy = buildShuffleRulePolicy(helper, writeOperatorParallelism, distributionMode, overwrite, table);
      LOG.info("shuffle policy config={}, actual={}", distributionMode,
          shufflePolicy == null ? DistributionMode.NONE : distributionMode.getDesc());

      String arcticEmitMode = table.properties().getOrDefault(ARCTIC_EMIT_MODE.key(), ARCTIC_EMIT_MODE.defaultValue());
      final boolean metricsEventLatency = CompatibleFlinkPropertyUtil
          .propertyAsBoolean(table.properties(), ArcticValidator.ARCTIC_LATENCY_METRIC_ENABLE,
              ArcticValidator.ARCTIC_LATENCY_METRIC_ENABLE_DEFAULT);

      final boolean metricsEnable = CompatibleFlinkPropertyUtil
          .propertyAsBoolean(table.properties(), ARCTIC_THROUGHPUT_METRIC_ENABLE,
              ARCTIC_THROUGHPUT_METRIC_ENABLE_DEFAULT);

      final Duration watermarkWriteGap = config.get(AUTO_EMIT_LOGSTORE_WATERMARK_GAP);

      ArcticFileWriter fileWriter = createFileWriter(table, shufflePolicy, overwrite, flinkSchemaRowType,
          arcticEmitMode, tableLoader);

      ArcticLogWriter logWriter = ArcticUtils.buildArcticLogWriter(table.properties(),
          producerConfig, topic, flinkSchema, arcticEmitMode, helper, tableLoader, watermarkWriteGap);

      MetricsGenerator metricsGenerator = ArcticUtils.getMetricsGenerator(metricsEventLatency,
          metricsEnable, table, flinkSchemaRowType, writeSchema);

      if (shufflePolicy != null) {
        rowDataInput = rowDataInput.partitionCustom(
            shufflePolicy.generatePartitioner(),
            shufflePolicy.generateKeySelector());
      }

      return withEmit(
          rowDataInput,
          logWriter,
          fileWriter,
          createFileCommitter(table, tableLoader, overwrite, arcticEmitMode),
          writeOperatorParallelism,
          metricsGenerator,
          arcticEmitMode);
    }

    private void initTableIfNeeded() {
      if (table == null) {
        table = ArcticUtils.loadArcticTable(tableLoader);
      }
    }

    /**
     * Transform {@link org.apache.iceberg.TableProperties#WRITE_DISTRIBUTION_MODE} to ShufflePolicyType
     */
    private DistributionHashMode getDistributionHashMode() {
      if (distributionMode != null) {
        return distributionMode;
      }

      String modeName = PropertyUtil.propertyAsString(
          table.properties(),
          WRITE_DISTRIBUTION_MODE,
          WRITE_DISTRIBUTION_MODE_DEFAULT);

      DistributionMode mode = DistributionMode.fromName(modeName);
      switch (mode) {
        case NONE:
          return DistributionHashMode.NONE;
        case HASH:
          String hashMode = PropertyUtil.propertyAsString(
              table.properties(), WRITE_DISTRIBUTION_HASH_MODE, WRITE_DISTRIBUTION_HASH_MODE_DEFAULT);
          return DistributionHashMode.valueOfDesc(hashMode);
        case RANGE:
          LOG.warn("Fallback to use 'none' distribution mode, because {}={} is not supported in flink now",
              WRITE_DISTRIBUTION_MODE, DistributionMode.RANGE.modeName());
          return DistributionHashMode.NONE;
        default:
          return DistributionHashMode.AUTO;
      }
    }

    @Nullable
    public static ShuffleRulePolicy<RowData, ShuffleKey> buildShuffleRulePolicy(
        ShuffleHelper helper,
        int writeOperatorParallelism,
        DistributionHashMode distributionHashMode,
        boolean overwrite,
        ArcticTable table) {
      if (distributionHashMode == DistributionHashMode.AUTO) {
        distributionHashMode = DistributionHashMode.autoSelect(
            helper.isPrimaryKeyExist(), helper.isPartitionKeyExist());
      }
      if (distributionHashMode == DistributionHashMode.NONE) {
        return null;
      } else {
        if (distributionHashMode.mustByPrimaryKey() && !helper.isPrimaryKeyExist()) {
          throw new IllegalArgumentException(
              "illegal shuffle policy " + distributionHashMode.getDesc() + " for table without primary key");
        }
        if (distributionHashMode.mustByPartition() && !helper.isPartitionKeyExist()) {
          throw new IllegalArgumentException(
              "illegal shuffle policy " + distributionHashMode.getDesc() + " for table without partition");
        }
        int writeFileSplit;
        if (ArcticUtils.isToBase(overwrite)) {
          writeFileSplit = PropertyUtil.propertyAsInt(
              table.properties(),
              TableProperties.BASE_FILE_INDEX_HASH_BUCKET,
              TableProperties.BASE_FILE_INDEX_HASH_BUCKET_DEFAULT);
        } else {
          writeFileSplit = PropertyUtil.propertyAsInt(
              table.properties(),
              TableProperties.CHANGE_FILE_INDEX_HASH_BUCKET,
              TableProperties.CHANGE_FILE_INDEX_HASH_BUCKET_DEFAULT);
        }

        return new RoundRobinShuffleRulePolicy(helper, writeOperatorParallelism,
            writeFileSplit, distributionHashMode);
      }
    }
  }

  public static ArcticFileWriter createFileWriter(
      ArcticTable arcticTable,
      ShuffleRulePolicy shufflePolicy,
      boolean overwrite,
      RowType flinkSchema,
      ArcticTableLoader tableLoader) {
    return createFileWriter(arcticTable, shufflePolicy, overwrite, flinkSchema, ARCTIC_EMIT_FILE,
        tableLoader);
  }

  public static ArcticFileWriter createFileWriter(
      ArcticTable arcticTable,
      ShuffleRulePolicy shufflePolicy,
      boolean overwrite,
      RowType flinkSchema,
      String emitMode,
      ArcticTableLoader tableLoader) {
    if (!ArcticUtils.arcticFileWriterEnable(emitMode)) {
      return null;
    }
    long maxOpenFilesSizeBytes = PropertyUtil
        .propertyAsLong(arcticTable.properties(), ARCTIC_WRITE_MAX_OPEN_FILE_SIZE,
            ARCTIC_WRITE_MAX_OPEN_FILE_SIZE_DEFAULT);
    LOG.info(
        "with maxOpenFilesSizeBytes = {}MB, close biggest/earliest file to avoid OOM",
        maxOpenFilesSizeBytes >> 20);

    int minFileSplitCount = PropertyUtil
        .propertyAsInt(arcticTable.properties(), TableProperties.CHANGE_FILE_INDEX_HASH_BUCKET,
            TableProperties.CHANGE_FILE_INDEX_HASH_BUCKET_DEFAULT);

    boolean upsert = arcticTable.isKeyedTable() && PropertyUtil.propertyAsBoolean(arcticTable.properties(),
        TableProperties.UPSERT_ENABLED, TableProperties.UPSERT_ENABLED_DEFAULT);
    boolean submitEmptySnapshot = PropertyUtil.propertyAsBoolean(
        arcticTable.properties(), SUBMIT_EMPTY_SNAPSHOTS.key(), SUBMIT_EMPTY_SNAPSHOTS.defaultValue());

    return new ArcticFileWriter(
        shufflePolicy,
        createTaskWriterFactory(arcticTable, overwrite, flinkSchema),
        minFileSplitCount,
        tableLoader,
        upsert,
        submitEmptySnapshot);
  }

  private static TaskWriterFactory<RowData> createTaskWriterFactory(
      ArcticTable arcticTable,
      boolean overwrite,
      RowType flinkSchema) {
    return new ArcticRowDataTaskWriterFactory(arcticTable, flinkSchema, overwrite);
  }

  public static OneInputStreamOperator<WriteResult, Void> createFileCommitter(
      ArcticTable arcticTable,
      ArcticTableLoader tableLoader,
      boolean overwrite) {
    return createFileCommitter(arcticTable, tableLoader, overwrite, ARCTIC_EMIT_FILE);
  }

  public static OneInputStreamOperator<WriteResult, Void> createFileCommitter(
      ArcticTable arcticTable,
      ArcticTableLoader tableLoader,
      boolean overwrite,
      String emitMode) {
    if (!ArcticUtils.arcticFileWriterEnable(emitMode)) {
      return null;
    }
    tableLoader.switchLoadInternalTableForKeyedTable(ArcticUtils.isToBase(overwrite));
    return (OneInputStreamOperator) ProxyUtil.getProxy(
        IcebergClassUtil.newIcebergFilesCommitter(tableLoader, overwrite, arcticTable.io()),
        arcticTable.io());
  }
}
