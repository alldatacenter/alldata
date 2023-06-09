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

package com.netease.arctic.flink.table;

import com.netease.arctic.flink.read.source.log.kafka.LogKafkaSource;
import com.netease.arctic.flink.read.source.log.kafka.LogKafkaSourceBuilder;
import com.netease.arctic.flink.util.CompatibleFlinkPropertyUtil;
import com.netease.arctic.table.ArcticTable;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.connector.source.Boundedness;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.source.DataStreamScanProvider;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.connector.source.ScanTableSource;
import org.apache.flink.table.connector.source.abilities.SupportsProjectionPushDown;
import org.apache.flink.table.connector.source.abilities.SupportsWatermarkPushDown;
import org.apache.flink.table.data.RowData;
import org.apache.flink.types.RowKind;
import org.apache.flink.util.Preconditions;
import org.apache.iceberg.Schema;
import org.apache.iceberg.types.Types.NestedField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.netease.arctic.flink.table.descriptors.ArcticValidator.ARCTIC_LOG_CONSISTENCY_GUARANTEE_ENABLE;
import static com.netease.arctic.flink.table.descriptors.ArcticValidator.ARCTIC_LOG_CONSUMER_CHANGELOG_MODE;
import static com.netease.arctic.flink.table.descriptors.ArcticValidator.LOG_CONSUMER_CHANGELOG_MODE_ALL_KINDS;
import static com.netease.arctic.flink.table.descriptors.ArcticValidator.LOG_CONSUMER_CHANGELOG_MODE_APPEND_ONLY;
import static org.apache.flink.table.connector.ChangelogMode.insertOnly;

/**
 * This is a log source table api, create log queue consumer e.g. {@link LogKafkaSource}
 */
public class LogDynamicSource implements ScanTableSource, SupportsWatermarkPushDown, SupportsProjectionPushDown {

  private static final Logger LOG = LoggerFactory.getLogger(LogDynamicSource.class);

  private final ArcticTable arcticTable;
  private final Schema schema;
  private final ReadableConfig tableOptions;
  private final Optional<String> consumerChangelogMode;
  private final boolean logRetractionEnable;

  /**
   * Watermark strategy that is used to generate per-partition watermark.
   */
  protected @Nullable WatermarkStrategy<RowData> watermarkStrategy;

  /** Data type to configure the formats. */

  /**
   * Indices that determine the value fields and the target position in the produced row.
   */
  protected int[] projectedFields;

  /**
   * Properties for the logStore consumer.
   */
  protected final Properties properties;

  private static final ChangelogMode ALL_KINDS = ChangelogMode.newBuilder()
      .addContainedKind(RowKind.INSERT)
      .addContainedKind(RowKind.UPDATE_BEFORE)
      .addContainedKind(RowKind.UPDATE_AFTER)
      .addContainedKind(RowKind.DELETE)
      .build();

  public LogDynamicSource(
      Properties properties,
      Schema schema,
      ReadableConfig tableOptions,
      ArcticTable arcticTable) {
    this.schema = schema;
    this.tableOptions = tableOptions;
    this.consumerChangelogMode = tableOptions.getOptional(ARCTIC_LOG_CONSUMER_CHANGELOG_MODE);
    this.logRetractionEnable = CompatibleFlinkPropertyUtil.propertyAsBoolean(arcticTable.properties(),
        ARCTIC_LOG_CONSISTENCY_GUARANTEE_ENABLE.key(), ARCTIC_LOG_CONSISTENCY_GUARANTEE_ENABLE.defaultValue());
    this.arcticTable = arcticTable;
    this.properties = properties;
  }

  public LogDynamicSource(
      Properties properties,
      Schema schema,
      ReadableConfig tableOptions,
      ArcticTable arcticTable,
      boolean logRetractionEnable,
      Optional<String> consumerChangelogMode) {
    this.schema = schema;
    this.tableOptions = tableOptions;
    this.consumerChangelogMode = consumerChangelogMode;
    this.logRetractionEnable = logRetractionEnable;
    this.arcticTable = arcticTable;
    this.properties = properties;
  }

  protected LogKafkaSource createKafkaSource() {
    Schema projectedSchema = getProjectSchema(schema);
    LOG.info("Schema used for create KafkaSource is: {}", projectedSchema);

    LogKafkaSourceBuilder kafkaSourceBuilder = LogKafkaSource.builder(projectedSchema, arcticTable.properties());
    kafkaSourceBuilder.setProperties(properties);

    LOG.info("build log kafka source");
    return kafkaSourceBuilder.build();
  }

  @Override
  public ChangelogMode getChangelogMode() {
    String changeLogMode = consumerChangelogMode.orElse(
        arcticTable.isKeyedTable() ? LOG_CONSUMER_CHANGELOG_MODE_ALL_KINDS : LOG_CONSUMER_CHANGELOG_MODE_APPEND_ONLY);
    switch (changeLogMode) {
      case LOG_CONSUMER_CHANGELOG_MODE_APPEND_ONLY:
        if (logRetractionEnable) {
          throw new IllegalArgumentException(
              String.format(
                  "Only %s is false when %s is %s",
                  ARCTIC_LOG_CONSISTENCY_GUARANTEE_ENABLE.key(),
                  ARCTIC_LOG_CONSUMER_CHANGELOG_MODE.key(),
                  LOG_CONSUMER_CHANGELOG_MODE_APPEND_ONLY));
        }
        return insertOnly();
      case LOG_CONSUMER_CHANGELOG_MODE_ALL_KINDS:
        return ALL_KINDS;
      default:
        throw new UnsupportedOperationException(
            String.format(
                "As of now, %s can't support this option %s.",
                ARCTIC_LOG_CONSUMER_CHANGELOG_MODE.key(),
                consumerChangelogMode
            ));
    }
  }

  @Override
  public ScanRuntimeProvider getScanRuntimeProvider(ScanContext scanContext) {
    final LogKafkaSource kafkaSource = createKafkaSource();

    return new DataStreamScanProvider() {
      @Override
      public DataStream<RowData> produceDataStream(StreamExecutionEnvironment execEnv) {
        if (watermarkStrategy == null) {
          watermarkStrategy = WatermarkStrategy.noWatermarks();
        }
        return execEnv.fromSource(
            kafkaSource, watermarkStrategy, "LogStoreSource-" + arcticTable.name());
      }

      @Override
      public boolean isBounded() {
        return kafkaSource.getBoundedness() == Boundedness.BOUNDED;
      }
    };
  }

  @Override
  public DynamicTableSource copy() {
    return new LogDynamicSource(
        this.properties,
        this.schema,
        this.tableOptions,
        this.arcticTable,
        this.logRetractionEnable,
        this.consumerChangelogMode);
  }

  @Override
  public String asSummaryString() {
    return "Arctic Log: " + arcticTable.name();
  }

  @Override
  public void applyWatermark(WatermarkStrategy<RowData> watermarkStrategy) {
    this.watermarkStrategy = watermarkStrategy;
  }

  @Override
  public boolean supportsNestedProjection() {
    return false;
  }

  @Override
  public void applyProjection(int[][] projectFields) {
    this.projectedFields = new int[projectFields.length];
    for (int i = 0; i < projectFields.length; i++) {
      Preconditions.checkArgument(projectFields[i].length == 1,
          "Don't support nested projection now.");
      this.projectedFields[i] = projectFields[i][0];
    }
  }

  private Schema getProjectSchema(Schema projectedSchema) {
    if (projectedFields != null) {
      List<NestedField> projectedSchemaColumns = projectedSchema.columns();
      projectedSchema = new Schema(Arrays.stream(projectedFields)
        .mapToObj(projectedSchemaColumns::get)
        .collect(Collectors.toList()));
    }
    return projectedSchema;
  }
}