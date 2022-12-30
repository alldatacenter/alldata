/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.bytedance.bitsail.connector.rocketmq.source.coordinator;

import com.bytedance.bitsail.base.connector.reader.v1.Boundedness;
import com.bytedance.bitsail.base.connector.reader.v1.SourceSplitCoordinator;
import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.connector.base.source.split.SplitAssigner;
import com.bytedance.bitsail.connector.rocketmq.error.RocketMQErrorCode;
import com.bytedance.bitsail.connector.rocketmq.option.RocketMQSourceOptions;
import com.bytedance.bitsail.connector.rocketmq.source.split.RocketMQSplit;
import com.bytedance.bitsail.connector.rocketmq.source.split.RocketMQState;
import com.bytedance.bitsail.connector.rocketmq.utils.RocketMQUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.consumer.DefaultLitePullConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.apache.rocketmq.client.consumer.store.ReadOffsetType.READ_FROM_MEMORY;

public class RocketMQSourceSplitCoordinator implements
    SourceSplitCoordinator<RocketMQSplit, RocketMQState> {

  private static final String COORDINATOR_INSTANCE_NAME_TEMPLATE = "rmqCoordinator:%s_%s_%s:%s";

  private static final Logger LOG = LoggerFactory.getLogger(RocketMQSourceSplitCoordinator.class);

  private final SourceSplitCoordinator.Context<RocketMQSplit, RocketMQState> context;
  private final BitSailConfiguration jobConfiguration;
  private final Boundedness boundedness;

  private final Set<MessageQueue> discoveredPartitions;
  private final Map<MessageQueue, String> assignedPartitions;
  private final Map<Integer, Set<RocketMQSplit>> pendingRocketMQSplitAssignment;
  private final long discoveryInternal;

  private String cluster;
  private String topic;
  private String consumerGroup;
  private String consumerOffsetMode;
  private long consumerOffsetTimestamp;
  private Map<MessageQueue, Long> consumerStopOffset;

  private transient DefaultLitePullConsumer consumer;
  private transient SplitAssigner<MessageQueue> splitAssigner;

  public RocketMQSourceSplitCoordinator(
      SourceSplitCoordinator.Context<RocketMQSplit, RocketMQState> context,
      BitSailConfiguration jobConfiguration,
      Boundedness boundedness) {
    this.context = context;
    this.jobConfiguration = jobConfiguration;
    this.boundedness = boundedness;
    this.discoveryInternal = jobConfiguration.get(RocketMQSourceOptions.DISCOVERY_INTERNAL);
    this.pendingRocketMQSplitAssignment = Maps.newConcurrentMap();

    this.discoveredPartitions = new HashSet<>();
    if (context.isRestored()) {
      RocketMQState restoreState = context.getRestoreState();
      assignedPartitions = restoreState.getAssignedWithSplits();
      discoveredPartitions.addAll(assignedPartitions.keySet());
    } else {
      assignedPartitions = Maps.newHashMap();
    }

    prepareConsumerProperties();
  }

  private void prepareConsumerProperties() {
    cluster = jobConfiguration.get(RocketMQSourceOptions.CLUSTER);
    topic = jobConfiguration.get(RocketMQSourceOptions.TOPIC);
    consumerGroup = jobConfiguration.get(RocketMQSourceOptions.CONSUMER_GROUP);
    consumerOffsetMode = jobConfiguration.get(RocketMQSourceOptions.CONSUMER_OFFSET_MODE);
    if (StringUtils.equalsIgnoreCase(consumerOffsetMode,
        RocketMQSourceOptions.CONSUMER_OFFSET_TIMESTAMP_KEY)) {
      consumerOffsetTimestamp = jobConfiguration.get(RocketMQSourceOptions.CONSUMER_OFFSET_TIMESTAMP);
    }
    if (jobConfiguration.fieldExists(RocketMQSourceOptions.CONSUMER_STOP_OFFSET)) {
      consumerStopOffset = jobConfiguration.get(RocketMQSourceOptions.CONSUMER_STOP_OFFSET);
    } else {
      consumerStopOffset = Maps.newHashMap();
    }
  }

  private void prepareRocketMQConsumer() {
    try {
      consumer = RocketMQUtils.prepareRocketMQConsumer(jobConfiguration,
          String.format(COORDINATOR_INSTANCE_NAME_TEMPLATE,
              cluster, topic, consumerGroup, UUID.randomUUID()));
      consumer.start();
    } catch (Exception e) {
      throw BitSailException.asBitSailException(RocketMQErrorCode.CONSUMER_CREATE_FAILED, e);
    }
  }

  @Override
  public void start() {
    prepareRocketMQConsumer();
    splitAssigner = new FairRocketMQSplitAssigner(jobConfiguration, assignedPartitions);
    if (discoveryInternal > 0) {
      context.runAsync(
          this::fetchMessageQueues,
          this::handleMessageQueueChanged,
          0,
          discoveryInternal
      );
    } else {
      context.runAsyncOnce(
          this::fetchMessageQueues,
          this::handleMessageQueueChanged
      );
    }
  }

  private Set<RocketMQSplit> fetchMessageQueues() throws MQClientException {
    Collection<MessageQueue> fetchedMessageQueues = Sets.newHashSet(consumer
        .fetchMessageQueues(topic));
    discoveredPartitions.addAll(fetchedMessageQueues);
    consumer.assign(discoveredPartitions);

    Set<RocketMQSplit> pendingAssignedPartitions = Sets.newHashSet();
    for (MessageQueue messageQueue : fetchedMessageQueues) {
      if (assignedPartitions.containsKey(messageQueue)) {
        continue;
      }

      pendingAssignedPartitions.add(
          RocketMQSplit.builder()
              .messageQueue(messageQueue)
              .startOffset(getStartOffset(messageQueue))
              .endOffset(getEndOffset(messageQueue))
              .splitId(splitAssigner.assignSplitId(messageQueue))
              .build()
      );
    }
    return pendingAssignedPartitions;
  }

  private long getEndOffset(MessageQueue messageQueue) {
    return consumerStopOffset.getOrDefault(messageQueue,
        RocketMQSourceOptions.CONSUMER_STOPPING_OFFSET);
  }

  private long getStartOffset(MessageQueue messageQueue) throws MQClientException {
    switch (consumerOffsetMode) {
      case RocketMQSourceOptions.CONSUMER_OFFSET_EARLIEST_KEY:
        consumer.seekToEnd(messageQueue);
        return consumer.getOffsetStore()
            .readOffset(messageQueue, READ_FROM_MEMORY);
      case RocketMQSourceOptions.CONSUMER_OFFSET_LATEST_KEY:
        consumer.seekToBegin(messageQueue);
        return consumer.getOffsetStore()
            .readOffset(messageQueue, READ_FROM_MEMORY);
      case RocketMQSourceOptions.CONSUMER_OFFSET_TIMESTAMP_KEY:
        return consumer.offsetForTimestamp(messageQueue, consumerOffsetTimestamp);
      default:
        throw BitSailException.asBitSailException(
            RocketMQErrorCode.CONSUMER_FETCH_OFFSET_FAILED,
            String.format("Consumer offset mode = %s not support right now.", consumerOffsetMode));
    }
  }

  private void handleMessageQueueChanged(Set<RocketMQSplit> pendingAssignedSplits,
                                         Throwable throwable) {
    if (throwable != null) {
      throw BitSailException.asBitSailException(
          CommonErrorCode.INTERNAL_ERROR,
          String.format("Failed to fetch rocketmq offset for the topic: %s", topic), throwable);
    }

    if (CollectionUtils.isEmpty(pendingAssignedSplits)) {
      return;
    }
    addSplitChangeToPendingAssignment(pendingAssignedSplits);
    notifyReaderAssignmentResult();
  }

  private void notifyReaderAssignmentResult() {
    Map<Integer, List<RocketMQSplit>> tmpRocketMQSplitAssignments = new HashMap<>();

    for (Integer pendingAssignmentReader : pendingRocketMQSplitAssignment.keySet()) {

      if (CollectionUtils.isNotEmpty(pendingRocketMQSplitAssignment.get(pendingAssignmentReader))
          && context.registeredReaders().contains(pendingAssignmentReader)) {

        tmpRocketMQSplitAssignments.put(pendingAssignmentReader, Lists.newArrayList(pendingRocketMQSplitAssignment.get(pendingAssignmentReader)));
      }
    }

    for (Integer pendingAssignmentReader : tmpRocketMQSplitAssignments.keySet()) {

      LOG.info("Assigning splits to reader {}, splits = {}.", pendingAssignmentReader,
          tmpRocketMQSplitAssignments.get(pendingAssignmentReader));

      context.assignSplit(pendingAssignmentReader,
          tmpRocketMQSplitAssignments.get(pendingAssignmentReader));
      Set<RocketMQSplit> removes = pendingRocketMQSplitAssignment.remove(pendingAssignmentReader);
      removes.forEach(removeSplit -> {
        assignedPartitions.put(removeSplit.getMessageQueue(), removeSplit.getSplitId());
      });

      LOG.info("Assigned splits to reader {}", pendingAssignmentReader);

      if (Boundedness.BOUNDEDNESS == boundedness) {
        LOG.info("Signal reader {} no more splits assigned in future.", pendingAssignmentReader);
        context.signalNoMoreSplits(pendingAssignmentReader);
      }
    }
  }

  private synchronized void addSplitChangeToPendingAssignment(Set<RocketMQSplit> newRocketMQSplits) {
    int numReader = context.totalParallelism();
    for (RocketMQSplit split : newRocketMQSplits) {
      int readerIndex = splitAssigner.assignToReader(split.getSplitId(), numReader);
      pendingRocketMQSplitAssignment.computeIfAbsent(readerIndex, r -> new HashSet<>())
          .add(split);
    }
    LOG.debug("RocketMQ splits {} finished assignment.", newRocketMQSplits);
  }

  @Override
  public void addReader(int subtaskId) {
    LOG.info(
        "Adding reader {} to RocketMQ Split Coordinator for consumer group {}.",
        subtaskId,
        consumerGroup);
    notifyReaderAssignmentResult();
  }

  @Override
  public void addSplitsBack(List<RocketMQSplit> splits, int subtaskId) {
    LOG.info("Source reader {} return splits {}.", subtaskId, splits);
    addSplitChangeToPendingAssignment(new HashSet<>(splits));
    notifyReaderAssignmentResult();
  }

  @Override
  public void handleSplitRequest(int subtaskId, @Nullable String requesterHostname) {
    // empty
  }

  @Override
  public RocketMQState snapshotState() throws Exception {
    return new RocketMQState(assignedPartitions);
  }

  @Override
  public void close() {
    if (consumer != null) {
      consumer.shutdown();
    }
  }
}
