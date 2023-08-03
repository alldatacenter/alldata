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

import com.netease.arctic.flink.read.internals.KafkaFetcher;
import com.netease.arctic.flink.shuffle.LogRecordV1;
import com.netease.arctic.log.LogData;
import com.netease.arctic.log.LogDataJsonDeserialization;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.connectors.kafka.internals.ClosableBlockingQueue;
import org.apache.flink.streaming.connectors.kafka.internals.Handover;
import org.apache.flink.streaming.connectors.kafka.internals.KafkaDeserializationSchemaWrapper;
import org.apache.flink.streaming.connectors.kafka.internals.KafkaTopicPartition;
import org.apache.flink.streaming.connectors.kafka.internals.KafkaTopicPartitionState;
import org.apache.flink.streaming.runtime.tasks.ProcessingTimeService;
import org.apache.flink.table.data.RowData;
import org.apache.flink.types.RowKind;
import org.apache.flink.util.SerializedValue;
import org.apache.iceberg.Schema;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Queue;

import static com.netease.arctic.flink.table.descriptors.ArcticValidator.ARCTIC_LOG_CONSISTENCY_GUARANTEE_ENABLE;
import static com.netease.arctic.flink.table.descriptors.ArcticValidator.LOG_CONSUMER_CHANGELOG_MODE_APPEND_ONLY;
import static com.netease.arctic.log.LogData.MAGIC_NUMBER;
import static org.apache.flink.util.Preconditions.checkArgument;
import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * The fetcher runs in {@link LogKafkaConsumer} and fetches messages from kafka, and retracts message as handling a
 * Flip message that {@link LogData#getFlip()} is true.
 * <p>
 * @deprecated since 0.4.1, will be removed in 0.7.0;
 */
@Deprecated
public class LogKafkaFetcher extends KafkaFetcher<RowData> {
  private static final Logger LOG = LoggerFactory.getLogger(LogKafkaFetcher.class);
  private final LogDataJsonDeserialization<RowData> logDataJsonDeserialization;
  private final LogReadHelper logReadHelper;
  private final boolean logRetractionEnable;
  LogKafkaConsumerThread<RowData> logKafkaConsumerThread;
  private final int subtaskId;
  private final boolean logConsumerAppendOnly;

  public LogKafkaFetcher(
      SourceFunction.SourceContext<RowData> sourceContext,
      Map<KafkaTopicPartition, Long> assignedPartitionsWithInitialOffsets,
      SerializedValue<WatermarkStrategy<RowData>> watermarkStrategy,
      ProcessingTimeService processingTimeProvider,
      long autoWatermarkInterval,
      ClassLoader userCodeClassLoader,
      String taskNameWithSubtasks,
      KafkaDeserializationSchemaWrapper<RowData> deserializer,
      Properties kafkaProperties,
      long pollTimeout,
      MetricGroup subtaskMetricGroup,
      MetricGroup consumerMetricGroup,
      boolean useMetrics,
      Schema schema,
      boolean logRetractionEnable,
      LogReadHelper logReadHelper,
      Handover handover,
      ClosableBlockingQueue<KafkaTopicPartitionState<RowData, TopicPartition>>
          unassignedPartitionsQueue,
      LogKafkaConsumerThread<RowData> logKafkaConsumerThread,
      int subtaskId,
      String logConsumerChangelogMode) throws Exception {
    super(
        sourceContext,
        assignedPartitionsWithInitialOffsets,
        watermarkStrategy,
        processingTimeProvider,
        autoWatermarkInterval,
        userCodeClassLoader,
        taskNameWithSubtasks,
        deserializer,
        kafkaProperties,
        pollTimeout,
        subtaskMetricGroup,
        consumerMetricGroup,
        useMetrics,
        handover,
        logKafkaConsumerThread,
        unassignedPartitionsQueue);
    this.logDataJsonDeserialization = new LogDataJsonDeserialization<>(
        schema,
        LogRecordV1.factory,
        LogRecordV1.arrayFactory,
        LogRecordV1.mapFactory
    );
    this.logRetractionEnable = logRetractionEnable;
    this.logReadHelper = logReadHelper;
    this.logKafkaConsumerThread = logKafkaConsumerThread;
    this.subtaskId = subtaskId;
    this.logConsumerAppendOnly = LOG_CONSUMER_CHANGELOG_MODE_APPEND_ONLY.equalsIgnoreCase(logConsumerChangelogMode);
  }

  @Override
  public void partitionConsumerRecordsHandler(
      List<ConsumerRecord<byte[], byte[]>> partitionRecords,
      KafkaTopicPartitionState<RowData, TopicPartition> partitionState) throws Exception {
    for (ConsumerRecord<byte[], byte[]> record : partitionRecords) {
      byte[] value = record.value();
      boolean magicFormat = checkMagicNum(value);
      if (!magicFormat) {
        throw new UnsupportedOperationException(
            "Can't deserialize arctic log queue message due to it does not contain magic number.");
      } else {
        // new format version
        LogData<RowData> logData = logDataJsonDeserialization.deserialize(record.value());
        if (!logData.getFlip() && filterByRowKind(logData.getActualValue())) {
          LOG.info(
              "filter the rowData, because of logConsumerAppendOnly is true, and rowData={}.",
              logData.getActualValue());
          continue;
        }
        final int partition = record.partition();
        final long actualRowOffset = record.offset();
        if (filterBuffer(logData, partition, actualRowOffset)) {
          synchronized (checkpointLock) {
            partitionState.setOffset(actualRowOffset);
          }
          continue;
        }
        processMsg(record, logData, partitionState);
      }
    }
  }

  /**
   * filter the rowData only works during
   * {@link com.netease.arctic.flink.table.descriptors.ArcticValidator#ARCTIC_LOG_CONSISTENCY_GUARANTEE_ENABLE}
   * is false and
   * {@link com.netease.arctic.flink.table.descriptors.ArcticValidator#ARCTIC_LOG_CONSUMER_CHANGELOG_MODE}
   * is {@link com.netease.arctic.flink.table.descriptors.ArcticValidator#LOG_CONSUMER_CHANGELOG_MODE_APPEND_ONLY} and
   * rowData.rowKind != INSERT
   *
   * @param rowData the judged data
   * @return true means should be filtered.
   */
  boolean filterByRowKind(RowData rowData) {
    return !logRetractionEnable && logConsumerAppendOnly && !rowData.getRowKind().equals(RowKind.INSERT);
  }

  /**
   * Should filter the records that has been fetched in the buffer when the {@link LogData#getFlip()} is true.
   */
  private boolean filterBuffer(LogData<RowData> logData, int partition, long actualRowOffset) {
    final String upstreamId = logData.getUpstreamId();

    boolean cleanBufferAction = logReadHelper.getCleanBufferAction(upstreamId, partition);
    if (!cleanBufferAction) {
      return false;
    }

    boolean isPartitionRetracting = logReadHelper.isJobRetractingRightNow(upstreamId, partition);
    if (isPartitionRetracting) {
      long retractingOffset =
          logReadHelper.queryPartitionRetractingOffset(logData.getUpstreamId(), partition);
      if (actualRowOffset <= retractingOffset) {
        LOG.info("The fetcher has finished to clean buffer records.");
        logReadHelper.cleanBufferAction(upstreamId, partition, false);
        return false;
      } else {
        // actual record offset is greater than the kafka consumer seeked offset,
        // meanings that haven't finished filter buffer records.
        return true;
      }
    } else {
      // this partition is not retracting status, so unnecessary filter records.
      return false;
    }
  }

  private void processMsg(
      ConsumerRecord<byte[], byte[]> record,
      final LogData<RowData> logData,
      KafkaTopicPartitionState<RowData, TopicPartition> partitionState) throws IOException {
    if (!logData.getFlip()) {
      handleUnFlip(logData, record, partitionState);
    } else {
      LOG.info(
          "subtaskId={}, fetch a flip msg, flip offset= {} with partition= {}, data={}.",
          subtaskId, record.offset(), record.partition(), logData);
      handleFlip(logData, record, partitionState);
    }
  }

  private void handleUnFlip(
      LogData<RowData> logData,
      ConsumerRecord<byte[], byte[]> record,
      KafkaTopicPartitionState<RowData, TopicPartition> partitionState) {
    RowData actualValue = logData.getActualValue();

    if (!logRetractionEnable) {
      emitRecordWithTimestampsWithoutConsistency(actualValue, partitionState, record.offset(), record.timestamp());
    } else {
      // enable log retraction
      handleUnFlipWithRetraction(logData, record, partitionState);
    }
  }

  private void handleUnFlipWithRetraction(
      LogData<RowData> logData,
      ConsumerRecord<byte[], byte[]> record,
      KafkaTopicPartitionState<RowData, TopicPartition> partitionState) {

    RowData actualValue = logData.getActualValue();
    final String upstreamId = logData.getUpstreamId();
    final int partition = record.partition();

    boolean isRetracting = logReadHelper.isJobRetractingRightNow(upstreamId, partition);
    if (isRetracting) {
      if (logData.getEpicNo() <= logReadHelper.queryRetractingEpicNo(upstreamId, partition)) {
        //  consumer is retracting but log data does not need to be retracted, these are not duplicate data.
        LOG.info(
            "although the consumer is retracting status, however the LogData is not required to turn ChangeAction, {}.",
            actualValue);
        return;
      }
      actualValue = logReadHelper.turnRowKind(actualValue);
    }

    emitRecordWithTimestampsWithConsistency(
        logData,
        partition,
        actualValue,
        partitionState,
        record.offset(),
        record.timestamp(),
        isRetracting
    );
  }

  private void handleFlip(
      LogData<RowData> logData,
      ConsumerRecord<byte[], byte[]> record,
      KafkaTopicPartitionState<RowData, TopicPartition> partitionState) {
    final String upstreamId = logData.getUpstreamId();
    final long epicNo = logData.getEpicNo();
    final int partition = record.partition();
    if (!logRetractionEnable) {
      LOG.info("subtaskId={}, receive a flip msg, while {} is false, so ignore this flip.",
          subtaskId, ARCTIC_LOG_CONSISTENCY_GUARANTEE_ENABLE.key());
      return;
    }

    boolean isRetracting = logReadHelper.isJobRetractingRightNow(upstreamId, partition);
    if (!isRetracting) {
      // fetch a flip msg and the consumer is not retracting,
      // meanings that should it is going to retract except epic-start offset is not found.
      // 1. find out partition seek offsets
      Optional<Long> seekOffsetOpt = logReadHelper.getEpicOffset(upstreamId, epicNo, partition);
      if (!seekOffsetOpt.isPresent()) {
        LOG.warn(
            "could not find out seek offset by upstreamId={}, epicNo={}, partition={}.",
            upstreamId, epicNo, partition);
        return;
      }
      long seekOffset = seekOffsetOpt.get();

      // 2. mark upstreamId epicNo partition is retracting.
      logReadHelper.cleanBufferAction(upstreamId, partition, true);
      synchronized (checkpointLock) {
        partitionState.setOffset(seekOffset);
        logReadHelper.markEpicPartitionRetracting(upstreamId, epicNo, partition, seekOffset);
      }

      // 3. consumer seek to new offset
      String topic = record.topic();
      KafkaTopicPartitionState<RowData, TopicPartition> kafkaTopicPartitionState =
          new KafkaTopicPartitionState<>(
              new KafkaTopicPartition(topic, partition),
              new TopicPartition(topic, partition));
      kafkaTopicPartitionState.setOffset(seekOffset);
      logKafkaConsumerThread.setTopicPartitionOffset(kafkaTopicPartitionState);
    } else {
      // fetch a flip msg and the consumer is retracting right row,
      // meanings should suspend retracting.
      long flipOffset = record.offset();
      synchronized (checkpointLock) {
        partitionState.setOffset(flipOffset);
        logReadHelper.suspendRetracting(upstreamId, epicNo, partition, flipOffset);
      }
    }
  }

  public static boolean checkMagicNum(byte[] value) {
    checkNotNull(value);
    checkArgument(value.length >= 3);
    return value[0] == MAGIC_NUMBER[0] && value[1] == MAGIC_NUMBER[1] && value[2] == MAGIC_NUMBER[2];
  }

  protected void emitRecordWithTimestampsWithConsistency(
      LogData<RowData> logData,
      int partition,
      RowData record,
      KafkaTopicPartitionState<RowData, TopicPartition> partitionState,
      long offset,
      long kafkaEventTimestamp,
      boolean isRetracting) {
    final String upstreamId = logData.getUpstreamId();
    final long epicNo = logData.getEpicNo();
    synchronized (checkpointLock) {
      if (isRetracting) {
        // update retracted offset to memory by every log record.
        logReadHelper.updateRetractingEpicOffset(upstreamId, epicNo, partition, offset);
      } else {
        logReadHelper.updateEpicStartOffsetIfEmpty(upstreamId, epicNo, partition, offset);
      }
      emitRecordWithTimestamps(
          record,
          partitionState,
          offset,
          kafkaEventTimestamp
      );
    }
  }

  protected void emitRecordWithTimestampsWithoutConsistency(
      RowData record,
      KafkaTopicPartitionState<RowData, TopicPartition> partitionState,
      long offset,
      long kafkaEventTimestamp) {
    synchronized (checkpointLock) {
      emitRecordWithTimestamps(
          record,
          partitionState,
          offset,
          kafkaEventTimestamp
      );
    }
  }

  protected void emitRecordWithTimestamps(
      RowData record,
      KafkaTopicPartitionState<RowData, TopicPartition> partitionState,
      long offset,
      long kafkaEventTimestamp) {
    setKafkaLatency(kafkaEventTimestamp);
    long timestamp = partitionState.extractTimestamp(record, kafkaEventTimestamp);
    sourceContext.collectWithTimestamp(record, timestamp);

    // this might emit a watermark, so do it after emitting the record
    partitionState.onEvent(record, timestamp);
    partitionState.setOffset(offset);
  }

  /**
   * Emits a record attaching a timestamp to it.
   *
   * @param records             The records to emit
   * @param partitionState      The state of the Kafka partition from which the record was fetched
   * @param offset              The offset of the corresponding Kafka record
   * @param kafkaEventTimestamp The timestamp of the Kafka record
   */
  protected void emitRecordsWithTimestamps(
      Queue<RowData> records,
      KafkaTopicPartitionState<RowData, TopicPartition> partitionState,
      long offset,
      long kafkaEventTimestamp) {
    // emit the records, using the checkpoint lock to guarantee
    // atomicity of record emission and offset state update
    synchronized (checkpointLock) {
      setKafkaLatency(kafkaEventTimestamp);
      RowData record;
      while ((record = records.poll()) != null) {
        long timestamp = partitionState.extractTimestamp(record, kafkaEventTimestamp);
        sourceContext.collectWithTimestamp(record, timestamp);

        // this might emit a watermark, so do it after emitting the record
        partitionState.onEvent(record, timestamp);
      }
      partitionState.setOffset(offset);
    }
  }
}
