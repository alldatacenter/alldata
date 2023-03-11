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

package com.netease.arctic.flink.read.source.log.kafka;

import com.netease.arctic.flink.read.internals.KafkaSourceReader;
import com.netease.arctic.flink.read.source.log.LogSourceHelper;
import org.apache.flink.api.connector.source.SourceReaderContext;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.base.source.reader.RecordEmitter;
import org.apache.flink.connector.base.source.reader.RecordsWithSplitIds;
import org.apache.flink.connector.base.source.reader.synchronization.FutureCompletingBlockingQueue;
import org.apache.flink.connector.kafka.source.metrics.KafkaSourceReaderMetrics;
import org.apache.flink.connector.kafka.source.reader.fetcher.KafkaSourceFetcherManager;
import org.apache.flink.connector.kafka.source.split.KafkaPartitionSplit;
import org.apache.flink.connector.kafka.source.split.KafkaPartitionSplitState;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

/**
 * The source reader for Kafka partitions.
 */
public class LogKafkaSourceReader<T> extends KafkaSourceReader<T> {

  private static final Logger LOG = LoggerFactory.getLogger(LogKafkaSourceReader.class);

  @Nullable
  private final LogSourceHelper logReadHelper;

  public LogKafkaSourceReader(
      FutureCompletingBlockingQueue<RecordsWithSplitIds<ConsumerRecord<byte[], byte[]>>> elementsQueue,
      KafkaSourceFetcherManager kafkaSourceFetcherManager,
      RecordEmitter<ConsumerRecord<byte[], byte[]>, T, KafkaPartitionSplitState> recordEmitter,
      Configuration config,
      SourceReaderContext context,
      KafkaSourceReaderMetrics kafkaSourceReaderMetrics,
      @Nullable LogSourceHelper logReadHelper) {
    super(
        elementsQueue,
        kafkaSourceFetcherManager,
        recordEmitter,
        config,
        context,
        kafkaSourceReaderMetrics);

    this.logReadHelper = logReadHelper;
  }

  @Override
  protected KafkaPartitionSplitState initializedState(KafkaPartitionSplit split) {
    if (logReadHelper != null) {
      logReadHelper.initializedState(split);
    }
    return new LogKafkaPartitionSplitState(split);
  }

  @Override
  protected KafkaPartitionSplit toSplitType(String splitId, KafkaPartitionSplitState splitState) {
    return ((LogKafkaPartitionSplitState) splitState).toLogKafkaPartitionSplit();
  }

}
