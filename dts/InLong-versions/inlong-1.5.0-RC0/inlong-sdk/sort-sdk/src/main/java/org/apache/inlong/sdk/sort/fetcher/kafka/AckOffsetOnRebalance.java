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

import org.apache.commons.collections.CollectionUtils;
import org.apache.inlong.sdk.sort.api.Seeker;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class AckOffsetOnRebalance implements ConsumerRebalanceListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AckOffsetOnRebalance.class);
    private static final long DEFAULT_MAX_WAIT_FOR_ACK_TIME = 15000L;
    private final long maxWaitForAckTime;
    private final String clusterId;
    private final Seeker seeker;
    private final ConcurrentHashMap<TopicPartition, OffsetAndMetadata> commitOffsetMap;
    private final ConcurrentHashMap<TopicPartition, ConcurrentSkipListMap<Long, Boolean>> ackOffsetMap;
    private final KafkaConsumer<byte[], byte[]> consumer;
    private final AtomicLong revokedNum = new AtomicLong(0);
    private final AtomicLong assignedNum = new AtomicLong(0);

    public AckOffsetOnRebalance(
            String clusterId,
            Seeker seeker,
            ConcurrentHashMap<TopicPartition, OffsetAndMetadata> commitOffsetMap,
            KafkaConsumer<byte[], byte[]> consumer) {
        this(clusterId, seeker, commitOffsetMap, null, consumer);
    }

    public AckOffsetOnRebalance(
            String clusterId,
            Seeker seeker,
            ConcurrentHashMap<TopicPartition, OffsetAndMetadata> commitOffsetMap,
            ConcurrentHashMap<TopicPartition, ConcurrentSkipListMap<Long, Boolean>> ackOffsetMap,
            KafkaConsumer<byte[], byte[]> consumer) {
        this(clusterId, seeker, commitOffsetMap, ackOffsetMap, consumer, DEFAULT_MAX_WAIT_FOR_ACK_TIME);
    }

    public AckOffsetOnRebalance(
            String clusterId,
            Seeker seeker,
            ConcurrentHashMap<TopicPartition, OffsetAndMetadata> commitOffsetMap,
            ConcurrentHashMap<TopicPartition, ConcurrentSkipListMap<Long, Boolean>> ackOffsetMap,
            KafkaConsumer<byte[], byte[]> consumer,
            long maxWaitForAckTime) {
        this.clusterId = clusterId;
        this.seeker = seeker;
        this.commitOffsetMap = commitOffsetMap;
        this.ackOffsetMap = ackOffsetMap;
        this.consumer = consumer;
        this.maxWaitForAckTime = maxWaitForAckTime;
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> collection) {
        LOGGER.info("*- in re-balance:onPartitionsRevoked, it's the {} time", revokedNum.incrementAndGet());
        collection.forEach((v) -> {
            LOGGER.debug("clusterId:{},onPartitionsRevoked:{}, position is {}",
                    clusterId, v.toString(), consumer.position(v));
        });

        try {
            if (Objects.nonNull(ackOffsetMap) && Objects.nonNull(commitOffsetMap)) {
                // sleep 15s to wait un-ack messages
                long startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < maxWaitForAckTime && !ackReady(collection)) {
                    TimeUnit.MILLISECONDS.sleep(1000L);
                }
                ackRemovedTopicPartitions(collection);
            }
        } catch (Throwable t) {
            LOGGER.warn("got exception in onPartitionsRevoked : ", t);
        }
    }

    private boolean ackReady(Collection<TopicPartition> revoked) {
        for (TopicPartition tp : revoked) {
            ConcurrentSkipListMap<Long, Boolean> tpMap = ackOffsetMap.get(tp);
            if (Objects.isNull(tpMap)) {
                continue;
            }
            for (Map.Entry<Long, Boolean> entry : tpMap.entrySet()) {
                if (!entry.getValue()) {
                    LOGGER.info("tp {}, offset {} has not been ack, wait", tp, entry.getKey());
                    return false;
                }
            }
        }
        LOGGER.info("all revoked tp have been ack, re-balance right now.");
        return true;
    }

    private void ackRemovedTopicPartitions(Collection<TopicPartition> revoked) {
        LOGGER.info("ack revoked topic partitions");
        prepareCommit();
        consumer.commitSync(commitOffsetMap);
        // remove revoked topic partitions
        Set<TopicPartition> keySet = ackOffsetMap.keySet();
        revoked.stream()
                .filter(keySet::contains)
                .forEach(ackOffsetMap::remove);
    }

    private void prepareCommit() {
        List<Long> removeOffsets = new ArrayList<>();
        ackOffsetMap.forEach((topicPartition, tpOffsetMap) -> {
            // get the remove list
            long commitOffset = -1;
            for (Long ackOffset : tpOffsetMap.keySet()) {
                if (!tpOffsetMap.get(ackOffset)) {
                    break;
                }
                removeOffsets.add(ackOffset);
                commitOffset = ackOffset;
            }
            // the first haven't ack, do nothing
            if (CollectionUtils.isEmpty(removeOffsets)) {
                return;
            }

            // remove offset and commit offset
            removeOffsets.forEach(tpOffsetMap::remove);
            removeOffsets.clear();
            commitOffsetMap.put(topicPartition, new OffsetAndMetadata(commitOffset));
        });
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> collection) {
        LOGGER.info("*- in re-balance:onPartitionsAssigned, it is the {} time", assignedNum.incrementAndGet());
        collection.forEach((v) -> {
            long position = consumer.position(v);
            LOGGER.debug("clusterId:{},onPartitionsAssigned:{}, position is {}",
                    clusterId, v.toString(), position);
            consumer.seek(v, position + 1);
        });
        seeker.seek();
    }
}
