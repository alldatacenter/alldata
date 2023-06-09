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

package com.netease.arctic.flink.read;

import com.netease.arctic.flink.read.internals.AbstractFetcher;
import com.netease.arctic.flink.read.source.log.kafka.LogKafkaSource;
import com.netease.arctic.flink.util.CompatibleFlinkPropertyUtil;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.operators.StreamingRuntimeContext;
import org.apache.flink.streaming.connectors.kafka.config.OffsetCommitMode;
import org.apache.flink.streaming.connectors.kafka.internals.ClosableBlockingQueue;
import org.apache.flink.streaming.connectors.kafka.internals.Handover;
import org.apache.flink.streaming.connectors.kafka.internals.KafkaDeserializationSchemaWrapper;
import org.apache.flink.streaming.connectors.kafka.internals.KafkaTopicPartition;
import org.apache.flink.streaming.connectors.kafka.internals.KafkaTopicPartitionState;
import org.apache.flink.table.data.RowData;
import org.apache.flink.util.SerializedValue;
import org.apache.iceberg.Schema;
import org.apache.kafka.common.TopicPartition;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import static com.netease.arctic.flink.table.descriptors.ArcticValidator.ARCTIC_LOG_CONSISTENCY_GUARANTEE_ENABLE;
import static com.netease.arctic.flink.table.descriptors.ArcticValidator.ARCTIC_LOG_CONSUMER_CHANGELOG_MODE;

/**
 * An arctic log consumer that consume arctic log data from kafka.
 * <p>
 * @deprecated since 0.4.1, will be removed in 0.7.0; use {@link LogKafkaSource} instead.
 */
@Deprecated
public class LogKafkaConsumer extends FlinkKafkaConsumer<RowData> {
  private static final long serialVersionUID = 7855676094345921722L;
  private KafkaDeserializationSchemaWrapper<RowData> logRecordDeserializationSchemaWrapper;
  private final Schema schema;
  private final boolean logRetractionEnable;
  private final LogReadHelper logReadHelper;
  private int subtaskId;
  private final String logConsumerChangelogMode;

  public LogKafkaConsumer(
      List<String> topics,
      KafkaDeserializationSchemaWrapper<RowData> deserializer,
      Properties props,
      Schema schema,
      ReadableConfig tableOptions) {
    super(topics, deserializer, props);
    this.logRecordDeserializationSchemaWrapper = deserializer;
    this.schema = schema;
    this.logRetractionEnable = CompatibleFlinkPropertyUtil.propertyAsBoolean(tableOptions,
        ARCTIC_LOG_CONSISTENCY_GUARANTEE_ENABLE);
    this.logConsumerChangelogMode = tableOptions.get(ARCTIC_LOG_CONSUMER_CHANGELOG_MODE);
    this.logReadHelper = new LogReadHelper();
  }

  public LogKafkaConsumer(
      Pattern subscriptionPattern,
      KafkaDeserializationSchemaWrapper<RowData> deserializer,
      Properties props,
      Schema schema,
      ReadableConfig tableOptions) {
    super(subscriptionPattern, deserializer, props);
    this.schema = schema;
    this.logRetractionEnable = CompatibleFlinkPropertyUtil.propertyAsBoolean(tableOptions,
        ARCTIC_LOG_CONSISTENCY_GUARANTEE_ENABLE);
    this.logConsumerChangelogMode = tableOptions.get(ARCTIC_LOG_CONSUMER_CHANGELOG_MODE);
    this.logReadHelper = new LogReadHelper();
  }

  @Override
  public final void initializeState(FunctionInitializationContext context) throws Exception {
    super.initializeState(context);
    logReadHelper.initializeState(context, getRuntimeContext());
    LOG.info(
        "initialize KafkaConsumer, subtaskId={}, {}={}",
        getRuntimeContext().getIndexOfThisSubtask(),
        ARCTIC_LOG_CONSISTENCY_GUARANTEE_ENABLE.key(),
        logRetractionEnable);
    subtaskId = getRuntimeContext().getIndexOfThisSubtask();
  }

  @Override
  public void snapshotState(FunctionSnapshotContext context) throws Exception {
    super.snapshotState(context);
    logReadHelper.snapshotState();
  }

  @Override
  protected AbstractFetcher<RowData, ?> createFetcher(
      SourceContext<RowData> sourceContext,
      Map<KafkaTopicPartition, Long> assignedPartitionsWithInitialOffsets,
      SerializedValue<WatermarkStrategy<RowData>> watermarkStrategy,
      StreamingRuntimeContext runtimeContext,
      OffsetCommitMode offsetCommitMode,
      MetricGroup consumerMetricGroup,
      boolean useMetrics) throws Exception {

    // make sure that auto commit is disabled when our offset commit mode is ON_CHECKPOINTS;
    // this overwrites whatever setting the user configured in the properties
    adjustAutoCommitConfig(properties, offsetCommitMode);

    String taskNameWithSubtasks = runtimeContext.getTaskNameWithSubtasks();
    MetricGroup subtaskMetricGroup = runtimeContext.getMetricGroup();
    Handover handover = new Handover();
    ClosableBlockingQueue<KafkaTopicPartitionState<RowData, TopicPartition>> unassignedPartitionsQueue =
        new ClosableBlockingQueue<>();

    return new LogKafkaFetcher(
        sourceContext,
        assignedPartitionsWithInitialOffsets,
        watermarkStrategy,
        runtimeContext.getProcessingTimeService(),
        runtimeContext.getExecutionConfig().getAutoWatermarkInterval(),
        runtimeContext.getUserCodeClassLoader(),
        taskNameWithSubtasks,
        logRecordDeserializationSchemaWrapper,
        properties,
        pollTimeout,
        runtimeContext.getMetricGroup(),
        consumerMetricGroup,
        useMetrics,
        schema,
        logRetractionEnable,
        logReadHelper,
        handover,
        unassignedPartitionsQueue,
        new LogKafkaConsumerThread<>(
            LOG,
            handover,
            properties,
            unassignedPartitionsQueue,
            "Kafka Fetcher for " + taskNameWithSubtasks,
            pollTimeout,
            useMetrics,
            consumerMetricGroup,
            subtaskMetricGroup
        ),
        subtaskId,
        logConsumerChangelogMode);
  }
}
