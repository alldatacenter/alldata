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

package org.apache.atlas.kafka;

import org.apache.kafka.common.TopicPartition;

public class AtlasKafkaMessage<T> {
    private final T              message;
    private final long           offset;
    private final TopicPartition topicPartition;
    private final boolean        spooled;
    private final long           msgCreated;

    public AtlasKafkaMessage(T message, long offset, String topic, int partition, long msgCreated, boolean spooled) {
        this.message        = message;
        this.offset         = offset;
        this.topicPartition = new TopicPartition(topic, partition);
        this.msgCreated     = msgCreated;
        this.spooled        = spooled;
    }

    public AtlasKafkaMessage(T message, long offset, String topic, int partition) {
        this(message, offset, topic, partition, 0, false);
    }

    public T getMessage() {
        return message;
    }

    public long getOffset() {
        return offset;
    }

    public TopicPartition getTopicPartition() {
        return topicPartition;
    }

    public String getTopic() {
        return topicPartition.topic();
    }

    public int getPartition() {
        return topicPartition.partition();
    }

    public boolean getSpooled() {
        return this.spooled;
    }

    public long getMsgCreated() {
        return this.msgCreated;
    }
}
