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
import org.apache.flink.annotation.Internal;
import org.apache.flink.api.connector.source.SourceReader;
import org.apache.flink.api.connector.source.SourceReaderContext;
import org.apache.flink.connector.base.source.reader.RecordsWithSplitIds;
import org.apache.flink.connector.base.source.reader.synchronization.FutureCompletingBlockingQueue;
import org.apache.flink.connector.pulsar.common.config.PulsarClientFactory;
import org.apache.flink.connector.pulsar.source.config.SourceConfiguration;
import org.apache.flink.connector.pulsar.source.reader.message.PulsarMessage;
import org.apache.flink.connector.pulsar.source.reader.split.PulsarOrderedPartitionSplitReader;
import org.apache.flink.connector.pulsar.source.split.PulsarPartitionSplit;
import org.apache.flink.table.data.RowData;
import org.apache.iceberg.Schema;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.SubscriptionType;

import java.util.function.Supplier;

/**
 * This factory class is used for creating different types of source reader for different
 * subscription type.
 *
 * <ol>
 *   <li>Exclusive: We would create {@link LogPulsarOrderedSourceReader}. Only support it for now.
 * </ol>
 */
@Internal
public final class LogPulsarSourceReaderFactory {

  private LogPulsarSourceReaderFactory() {
    // No public constructor.
  }

  /**
   * @param readerContext
   * @param sourceConfiguration
   * @param schema              read schema, only contains the selected fields
   * @return
   */
  @SuppressWarnings("java:S2095")
  public static SourceReader<RowData, PulsarPartitionSplit> create(
      SourceReaderContext readerContext,
      SourceConfiguration sourceConfiguration,
      Schema schema,
      boolean logRetractionEnable,
      String logConsumerChangelogMode) {
    // ------ copy from org.apache.flink.connector.pulsar.source.reader.PulsarSourceReaderFactory start -------
    PulsarClient pulsarClient = PulsarClientFactory.createClient(sourceConfiguration);
    PulsarAdmin pulsarAdmin = PulsarClientFactory.createAdmin(sourceConfiguration);

    // Create a message queue with the predefined source option.
    int queueCapacity = sourceConfiguration.getMessageQueueCapacity();
    FutureCompletingBlockingQueue<RecordsWithSplitIds<PulsarMessage<RowData>>> elementsQueue =
        new FutureCompletingBlockingQueue<>(queueCapacity);

    // Create different pulsar source reader by subscription type.
    SubscriptionType subscriptionType = sourceConfiguration.getSubscriptionType();
    // ------ copy from org.apache.flink.connector.pulsar.source.reader.PulsarSourceReaderFactory end -------
    if (subscriptionType == SubscriptionType.Exclusive) {
      LogSourceHelper logReadHelper = logRetractionEnable ? new LogSourceHelper() : null;

      // Create an ordered split reader supplier.
      Supplier<PulsarOrderedPartitionSplitReader<RowData>> splitReaderSupplier =
          () ->
              new LogPulsarOrderedPartitionSplitReader(
                  pulsarClient,
                  pulsarAdmin,
                  sourceConfiguration,
                  null,
                  schema,
                  logRetractionEnable,
                  logReadHelper,
                  logConsumerChangelogMode);

      return new LogPulsarOrderedSourceReader<>(
          elementsQueue,
          splitReaderSupplier,
          readerContext,
          sourceConfiguration,
          pulsarClient,
          pulsarAdmin);
    } else {
      throw new UnsupportedOperationException(
          "This subscription type [" + subscriptionType + "] is not supported currently.");
    }
  }
}
