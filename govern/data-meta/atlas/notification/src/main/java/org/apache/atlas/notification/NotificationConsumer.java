/**
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
package org.apache.atlas.notification;

import java.util.List;
import java.util.Map;

import org.apache.kafka.common.TopicPartition;
import org.apache.atlas.kafka.AtlasKafkaMessage;

/**
 * Atlas notification consumer.  This consumer blocks until a notification can be read.
 *
 * @param <T>  the class type of notifications returned by this consumer
 */
public interface NotificationConsumer<T> {

    /**
     * Commit the offset of messages that have been successfully processed.
     *
     * This API should be called when messages read with {@link #next()} have been successfully processed and
     * the consumer is ready to handle the next message, which could happen even after a normal or an abnormal
     * restart.
     */
    void commit(TopicPartition partition, long offset);

    void close();

    void wakeup();

    /**
     * Fetch data for the topics from Kafka
     * @return List containing kafka message and partionId and offset.
     */
    List<AtlasKafkaMessage<T>> receive();

    /**
     * Fetch data for the topics from Kafka
     * @param timeoutMilliSeconds poll timeout
     * @return List containing kafka message and partionId and offset.
     */
    List<AtlasKafkaMessage<T>> receive(long timeoutMilliSeconds);


    /**
     * Fetch data for the topics from Kafka, if lastCommittedOffset same as message
     * received offset, it will proceed with commit.
     * @return List containing kafka message and partionId and offset.
     */
    List<AtlasKafkaMessage<T>> receiveWithCheckedCommit(Map<TopicPartition, Long> lastCommittedPartitionOffset);
}
