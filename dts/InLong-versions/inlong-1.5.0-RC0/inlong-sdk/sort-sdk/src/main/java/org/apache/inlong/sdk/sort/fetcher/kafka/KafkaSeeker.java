/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.sort.fetcher.kafka;

import org.apache.inlong.sdk.sort.api.Seeker;
import org.apache.inlong.sdk.sort.entity.InLongTopic;
import org.apache.inlong.sdk.sort.util.TimeUtil;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Kafka seeker.
 * Only seek those topics and partitions assigned by server.
 * The whole process can be divided into three steps:
 * 1. Find all topics and partitions of this consumer;
 * 2. Calculate the offsets of the excepted seek time of each topic and partition respectively;
 * 3. Reset these topics and partitions to the corresponding offset
 */
public class KafkaSeeker implements Seeker {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaSeeker.class);
    private long seekTime = -1;
    private String topic;
    private KafkaConsumer<byte[], byte[]> consumer;

    public KafkaSeeker(KafkaConsumer<byte[], byte[]> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void configure(InLongTopic inLongTopic) {
        seekTime = TimeUtil.parseStartTime(inLongTopic);
        topic = inLongTopic.getTopic();
        LOGGER.info("start to config kafka seeker, topic is {}, seek time is {}", topic, seekTime);
    }

    @Override
    public void seek() {
        if (seekTime < 0) {
            return;
        }
        LOGGER.info("start to seek kafka topic {}, seek time is {}", topic, seekTime);
        try {
            Set<TopicPartition> assignedTopicPartitions = consumer.assignment();
            if (assignedTopicPartitions.isEmpty()) {
                LOGGER.error("haven't assigned any topic partitions, do nothing");
                return;
            }
            Map<TopicPartition, Long> timestampsToSearch = assignedTopicPartitions.stream()
                    .collect(Collectors.toMap(tp -> tp, tp -> seekTime));
            Map<TopicPartition, OffsetAndTimestamp> offsetMap = consumer.offsetsForTimes(timestampsToSearch);
            List<TopicPartition> endOffsetsTopicPartitions = new ArrayList<>();
            offsetMap.forEach(
                    (tp, offsetAndTimestamp) -> resetOffset(tp, offsetAndTimestamp, endOffsetsTopicPartitions));
            LOGGER.info("topic partition {} should be seek to end", endOffsetsTopicPartitions);
            if (!endOffsetsTopicPartitions.isEmpty()) {
                consumer.seekToEnd(endOffsetsTopicPartitions);
            }
            LOGGER.info("finish to seek kafka topic {}", topic);
        } catch (Throwable t) {
            LOGGER.error("failed to seek kafka topic, ex is {}", t.getMessage(), t);
        }
    }

    private void resetOffset(
            TopicPartition tp,
            OffsetAndTimestamp offsetAndTimestamp,
            List<TopicPartition> endOffsetsTopicPartitions) {
        // if offsetAndTimestamp = null, means the time you seek is later than the last offset
        if (offsetAndTimestamp == null) {
            LOGGER.info("tp {} has null offsetAndTimestamp, reset to end", tp);
            endOffsetsTopicPartitions.add(tp);
        }
        long expected = offsetAndTimestamp.offset();
        long last = consumer.position(tp);
        LOGGER.info("for tp {}, expected offset is {}, last offset is {}", tp, expected, last);
        // only reset if last consume offset earlier than the expected one
        if (last < expected) {
            LOGGER.info("do seek for tp {}", tp);
            consumer.seek(tp, offsetAndTimestamp.offset());
            long afterSeek = consumer.position(tp);
            LOGGER.info("after seek, the offset for tp {} is {}", tp, afterSeek);
        }

    }

    @Override
    public long getSeekTime() {
        return seekTime;
    }
}
