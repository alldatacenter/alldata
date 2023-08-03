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

package com.netease.arctic.flink.read.source.log.pulsar;

import com.netease.arctic.flink.read.source.log.LogSourceHelper;
import com.netease.arctic.flink.shuffle.LogRecordV1;
import com.netease.arctic.flink.table.descriptors.ArcticValidator;
import com.netease.arctic.log.LogData;
import com.netease.arctic.log.LogDataJsonDeserialization;
import org.apache.flink.annotation.Internal;
import org.apache.flink.api.common.time.Deadline;
import org.apache.flink.connector.base.source.reader.RecordsBySplits;
import org.apache.flink.connector.base.source.reader.RecordsWithSplitIds;
import org.apache.flink.connector.pulsar.source.config.SourceConfiguration;
import org.apache.flink.connector.pulsar.source.enumerator.cursor.StopCursor;
import org.apache.flink.connector.pulsar.source.reader.deserializer.PulsarDeserializationSchema;
import org.apache.flink.connector.pulsar.source.reader.message.PulsarMessage;
import org.apache.flink.connector.pulsar.source.reader.split.PulsarOrderedPartitionSplitReader;
import org.apache.flink.connector.pulsar.source.split.PulsarPartitionSplit;
import org.apache.flink.table.data.RowData;
import org.apache.flink.types.RowKind;
import org.apache.iceberg.Schema;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static com.netease.arctic.flink.table.descriptors.ArcticValidator.LOG_CONSUMER_CHANGELOG_MODE_APPEND_ONLY;

/**
 * The split reader a given {@link PulsarPartitionSplit}, it would be closed once the {@link
 * LogPulsarOrderedSourceReader} is closed.
 */
@Internal
public class LogPulsarOrderedPartitionSplitReader extends PulsarOrderedPartitionSplitReader<RowData> {
  private static final Logger LOG = LoggerFactory.getLogger(LogPulsarOrderedPartitionSplitReader.class);

  private final LogDataJsonDeserialization<RowData> logDataJsonDeserialization;
  private final boolean logRetractionEnable;
  private final boolean logConsumerAppendOnly;

  public LogPulsarOrderedPartitionSplitReader(
      PulsarClient pulsarClient,
      PulsarAdmin pulsarAdmin,
      SourceConfiguration sourceConfiguration,
      PulsarDeserializationSchema<RowData> deserializationSchema,
      Schema schema,
      boolean logRetractionEnable,
      LogSourceHelper logReadHelper,
      String logConsumerChangelogMode) {
    super(pulsarClient, pulsarAdmin, sourceConfiguration, deserializationSchema);

    this.logDataJsonDeserialization = new LogDataJsonDeserialization<>(
        schema,
        LogRecordV1.factory,
        LogRecordV1.arrayFactory,
        LogRecordV1.mapFactory
    );
    this.logRetractionEnable = logRetractionEnable;
    this.logConsumerAppendOnly = LOG_CONSUMER_CHANGELOG_MODE_APPEND_ONLY.equalsIgnoreCase(logConsumerChangelogMode);
  }

  @Override
  public RecordsWithSplitIds<PulsarMessage<RowData>> fetch() throws IOException {
    // ---- copy from org.apache.flink.connector.pulsar.source.reader.split.PulsarPartitionSplitReaderBase start ----
    RecordsBySplits.Builder<PulsarMessage<RowData>> builder = new RecordsBySplits.Builder<>();

    // Return when no split registered to this reader.
    if (pulsarConsumer == null || registeredSplit == null) {
      return builder.build();
    }

    StopCursor stopCursor = registeredSplit.getStopCursor();
    String splitId = registeredSplit.splitId();
    Deadline deadline = Deadline.fromNow(sourceConfiguration.getMaxFetchTime());

    // Consume message from pulsar until it was woke up by flink reader.
    for (int messageNum = 0;
         messageNum < sourceConfiguration.getMaxFetchRecords() && deadline.hasTimeLeft();
         messageNum++) {
      try {
        Duration timeout = deadline.timeLeftIfAny();
        Message<byte[]> message = pollMessage(timeout);
        if (message == null) {
          break;
        }
        // ---- copy from org.apache.flink.connector.pulsar.source.reader.split.PulsarPartitionSplitReaderBase end ----
        LogData<RowData> logData = logDataJsonDeserialization.deserialize(message.getData());
        if (!logData.getFlip() && filterByRowKind(logData.getActualValue())) {
          LOG.debug(
              "filter the rowData, because of logConsumerAppendOnly is true, and rowData={}.",
              logData.getActualValue());
          continue;
        }
        StopCursor.StopCondition condition = stopCursor.shouldStop(message);

        if (condition == StopCursor.StopCondition.CONTINUE || condition == StopCursor.StopCondition.EXACTLY) {
          if (logData.getFlip()) {
            if (logRetractionEnable) {
              break;
            } else {
              // Acknowledge message if need.
              finishedPollMessage(message);
              continue;
            }
          }
          builder.add(splitId,
              LogRecordPulsarWithRetractInfo.of(message.getMessageId(), message.getEventTime(), logData));

          // Acknowledge message if need.
          finishedPollMessage(message);
        }

        if (condition == StopCursor.StopCondition.EXACTLY || condition == StopCursor.StopCondition.TERMINATE) {
          builder.addFinishedSplit(splitId);
          break;
        }
      } catch (TimeoutException e) {
        break;
      } catch (Exception e) {
        throw new IOException(e);
      }
    }

    return builder.build();
  }

  /**
   * filter the rowData only works during {@link ArcticValidator#ARCTIC_LOG_CONSISTENCY_GUARANTEE_ENABLE}
   * is false and {@link ArcticValidator#ARCTIC_LOG_CONSUMER_CHANGELOG_MODE}
   * is {@link ArcticValidator#LOG_CONSUMER_CHANGELOG_MODE_APPEND_ONLY} and rowData.rowKind != INSERT
   *
   * @param rowData the judged data
   * @return true means should be filtered.
   */
  boolean filterByRowKind(RowData rowData) {
    return !logRetractionEnable && logConsumerAppendOnly && !rowData.getRowKind().equals(RowKind.INSERT);
  }

}
