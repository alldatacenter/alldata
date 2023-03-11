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

import com.netease.arctic.flink.read.internals.KafkaSourceFetcherManager;
import com.netease.arctic.flink.read.internals.KafkaSourceReader;
import com.netease.arctic.flink.read.source.log.LogSourceHelper;
import org.apache.flink.api.connector.source.SourceReaderContext;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.base.source.reader.RecordEmitter;
import org.apache.flink.connector.base.source.reader.RecordsWithSplitIds;
import org.apache.flink.connector.base.source.reader.synchronization.FutureCompletingBlockingQueue;
import org.apache.flink.connector.kafka.source.split.KafkaPartitionSplit;
import org.apache.flink.connector.kafka.source.split.KafkaPartitionSplitState;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * The source reader for Kafka partitions.
 */
public class LogKafkaSourceReader<T> extends KafkaSourceReader<T> {

  private static final Logger LOG = LoggerFactory.getLogger(LogKafkaSourceReader.class);

  // These maps need to be concurrent because it will be accessed by both the main thread
  // and the split fetcher thread in the callback.
  private final SortedMap<Long, Map<TopicPartition, OffsetAndMetadata>> offsetsToCommit;
  private final ConcurrentMap<TopicPartition, OffsetAndMetadata> offsetsOfFinishedSplits;
  @Nullable
  private final LogSourceHelper logReadHelper;

  public LogKafkaSourceReader(
      FutureCompletingBlockingQueue<RecordsWithSplitIds<ConsumerRecord<byte[], byte[]>>> elementsQueue,
      Supplier<LogKafkaPartitionSplitReader> splitReaderSupplier,
      RecordEmitter<ConsumerRecord<byte[], byte[]>, T, KafkaPartitionSplitState> recordEmitter,
      Configuration config,
      SourceReaderContext context,
      @Nullable LogSourceHelper logReadHelper) {
    super(
        elementsQueue,
        new KafkaSourceFetcherManager(elementsQueue, splitReaderSupplier::get),
        recordEmitter,
        config,
        context);

    this.logReadHelper = logReadHelper;
    this.offsetsToCommit = Collections.synchronizedSortedMap(new TreeMap<>());
    this.offsetsOfFinishedSplits = new ConcurrentHashMap<>();
  }

  @Override
  protected void onSplitFinished(Map<String, KafkaPartitionSplitState> finishedSplitIds) {
    finishedSplitIds.forEach(
        (ignored, splitState) -> {
          if (splitState.getCurrentOffset() >= 0) {
            offsetsOfFinishedSplits.put(
                splitState.getTopicPartition(),
                new OffsetAndMetadata(splitState.getCurrentOffset()));
          }
        });
  }

  @Override
  public List<KafkaPartitionSplit> snapshotState(long checkpointId) {
    List<KafkaPartitionSplit> splits = super.snapshotState(checkpointId);
    if (splits.isEmpty() && offsetsOfFinishedSplits.isEmpty()) {
      offsetsToCommit.put(checkpointId, Collections.emptyMap());
    } else {
      Map<TopicPartition, OffsetAndMetadata> offsetsMap =
          offsetsToCommit.computeIfAbsent(checkpointId, id -> new HashMap<>());
      // Put the offsets of the active splits.
      for (KafkaPartitionSplit split : splits) {
        // If the checkpoint is triggered before the partition starting offsets
        // is retrieved, do not commit the offsets for those partitions.
        if (split.getStartingOffset() >= 0) {
          offsetsMap.put(
              split.getTopicPartition(),
              new OffsetAndMetadata(split.getStartingOffset()));
        }
      }
      // Put offsets of all the finished splits.
      offsetsMap.putAll(offsetsOfFinishedSplits);
    }
    return splits;
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
