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

import com.netease.arctic.flink.read.internals.KafkaConsumerThread;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.streaming.connectors.kafka.internals.ClosableBlockingQueue;
import org.apache.flink.streaming.connectors.kafka.internals.Handover;
import org.apache.flink.streaming.connectors.kafka.internals.KafkaTopicPartitionState;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * This thread is an extent of {@link KafkaConsumerThread} added an abstract method
 * {@link KafkaConsumerThread#reSeekPartitionOffsets()} to reSeek the offset of kafka topic partitions.
 * <p>
 * @deprecated since 0.4.1, will be removed in 0.7.0;
 */
@Deprecated
public class LogKafkaConsumerThread<T> extends KafkaConsumerThread<T> {
  protected final ClosableBlockingQueue<KafkaTopicPartitionState<T, TopicPartition>>
      unReSeekPartitionsQueue;

  public LogKafkaConsumerThread(
      Logger log,
      Handover handover,
      Properties kafkaProperties,
      ClosableBlockingQueue<KafkaTopicPartitionState<T, TopicPartition>> unassignedPartitionsQueue,
      String threadName,
      long pollTimeout,
      boolean useMetrics,
      MetricGroup consumerMetricGroup,
      MetricGroup subtaskMetricGroup) {
    super(
        log,
        handover,
        kafkaProperties,
        unassignedPartitionsQueue,
        threadName,
        pollTimeout,
        useMetrics,
        consumerMetricGroup,
        subtaskMetricGroup
    );
    this.unReSeekPartitionsQueue = new ClosableBlockingQueue<>();
  }

  /**
   * setting the consumer and seeking topic partition offsets to make sure log consumer consistency guarantee.
   *
   * @throws Exception
   */
  public void reSeekPartitionOffsets() throws Exception {
    //
    // attention lock.
    List<KafkaTopicPartitionState<T, TopicPartition>> kafkaTopicPartitionStates =
        unReSeekPartitionsQueue.pollBatch();
    if (kafkaTopicPartitionStates == null) {
      return;
    }

    boolean reassignmentStarted = false;

    // since the reassignment may introduce several Kafka blocking calls that cannot be
    // interrupted,
    // the consumer needs to be isolated from external wakeup calls in setOffsetsToCommit() and
    // shutdown()
    // until the reassignment is complete.
    final KafkaConsumer<byte[], byte[]> consumerTmp;
    synchronized (consumerReassignmentLock) {
      consumerTmp = this.consumer;
      this.consumer = null;
    }

    final Map<TopicPartition, Long> oldPartitionAssignmentsToPosition = new HashMap<>();
    try {
      for (TopicPartition oldPartition : consumerTmp.assignment()) {
        oldPartitionAssignmentsToPosition.put(
            oldPartition, consumerTmp.position(oldPartition));
      }

      // reassign with the new partitions
      reassignmentStarted = true;

      // old partitions should be seeked to their previous position
      for (KafkaTopicPartitionState<T, TopicPartition> kafkaTopicPartitionState : kafkaTopicPartitionStates) {
        TopicPartition topicPartition = kafkaTopicPartitionState.getKafkaPartitionHandle();
        long seekOffset = kafkaTopicPartitionState.getOffset();

        consumerTmp.seek(topicPartition, seekOffset);

      }

    } catch (WakeupException e) {
      // a WakeupException may be thrown if the consumer was invoked wakeup()
      // before it was isolated for the reassignment. In this case, we abort the
      // reassignment and just re-expose the original consumer.

      synchronized (consumerReassignmentLock) {
        this.consumer = consumerTmp;

        // if reassignment had already started and affected the consumer,
        // we do a full roll back so that it is as if it was left untouched
        if (reassignmentStarted) {
          for (KafkaTopicPartitionState<T, TopicPartition> kafkaTopicPartitionState : kafkaTopicPartitionStates) {
            TopicPartition topicPartition = kafkaTopicPartitionState.getKafkaPartitionHandle();
            long oldOffset = oldPartitionAssignmentsToPosition.get(topicPartition);
            this.consumer.seek(topicPartition, oldOffset);
          }
        }

        // no need to restore the wakeup state in this case,
        // since only the last wakeup call is effective anyways
        hasBufferedWakeup = false;


        // this signals the main fetch loop to continue through the loop
        throw new AbortedReassignmentException();
      }
    }

    // reassignment complete; expose the reassigned consumer
    synchronized (consumerReassignmentLock) {
      this.consumer = consumerTmp;

      // restore wakeup state for the consumer if necessary
      if (hasBufferedWakeup) {
        this.consumer.wakeup();
        hasBufferedWakeup = false;
      }
    }
  }

  public void setTopicPartitionOffset(KafkaTopicPartitionState<T, TopicPartition> partitionState) {
    unReSeekPartitionsQueue.add(partitionState);
  }
}
