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

package org.apache.inlong.sort.standalone.sink.kafka;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.internals.StickyPartitionCache;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Divide partitions by client.id.
 * The default partitioning strategy: if the number of producers is bigger than 1,
 * and the number of partitions bigger than producers, the producers divide partitions equally.
 * <li>If a partition is specified in the record, use it</li>
 * <li>If no partition is specified but a key is present choose a partition based on a hash of the key</li>
 * <li>If no partition or key is present choose the sticky partition that changes when the batch is full.</li>
 *
 * See KIP-480 for details about sticky partitioning.
 */
public class PartitionerSelector implements Partitioner {

    private final StickyPartitionCache stickyPartitionCache = new MyStickyPartitionCache();

    /**
     * clientId and number of producers
     */
    private static Map<String, AtomicInteger> clientIdIndex = new ConcurrentHashMap<>();

    /**
     * index of current producer.
     */
    private int index;

    /**
     * clientId
     */
    private String clientId;

    /**
     * assign index for each producer.
     *
     */
    @Override
    public void configure(Map<String, ?> configs) {
        clientId = (String) configs.get(ProducerConfig.CLIENT_ID_CONFIG);

        if (clientId != null && !clientId.isEmpty()) {
            if (clientId.contains("-")) {
                clientId = clientId.split("-")[0];
            }
        }

        AtomicInteger atomicInteger = clientIdIndex.get(clientId);
        if (atomicInteger == null) {
            clientIdIndex.putIfAbsent(clientId, new AtomicInteger(0));
            atomicInteger = clientIdIndex.get(clientId);
        }

        index = atomicInteger.getAndIncrement();
    }

    /**
     * Compute the partition for the given record.
     *
     * @param topic      The topic name
     * @param key        The key to partition on (or null if no key)
     * @param keyBytes   serialized key to partition on (or null if no key)
     * @param value      The value to partition on or null
     * @param valueBytes serialized value to partition on or null
     * @param cluster    The current cluster metadata
     */
    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
            Object value, byte[] valueBytes, Cluster cluster) {
        if (keyBytes == null) {
            return stickyPartitionCache.partition(topic, cluster);
        }
        List<PartitionInfo> partitions = cluster.availablePartitionsForTopic(topic);
        int numPartitions = partitions.size();

        if (numPartitions == 0) {
            partitions = cluster.partitionsForTopic(topic);
            numPartitions = partitions.size();
        }

        // hash the keyBytes to choose a partition
        return Utils.toPositive(Utils.murmur2(keyBytes)) % numPartitions;
    }

    /**
     * reduce the number of total producer when this one is closed.
     */
    @Override
    public void close() {
        AtomicInteger atomicInteger = clientIdIndex.get(clientId);
        if (atomicInteger == null) {
            return;
        }
        atomicInteger.decrementAndGet();
    }

    /**
     * If a batch completed for the current sticky partition, change the sticky partition. Alternately, if
     * no sticky partition has been determined, set one.
     */
    @Override
    public void onNewBatch(String topic, Cluster cluster, int prevPartition) {
        stickyPartitionCache.nextPartition(topic, cluster, prevPartition);
    }

    /**
     * Customized sticky partition selector.
     */
    class MyStickyPartitionCache extends StickyPartitionCache {

        private final ConcurrentMap<String, Integer> indexCache;

        public MyStickyPartitionCache() {
            this.indexCache = new ConcurrentHashMap<>();
        }

        @Override
        public int partition(String topic, Cluster cluster) {
            Integer part = indexCache.get(topic);
            if (part == null) {
                return nextPartition(topic, cluster, -1);
            }
            return part;
        }

        @Override
        public int nextPartition(String topic, Cluster cluster, int prevPartition) {
            Integer oldPart = indexCache.get(topic);
            boolean needChangePartition = (oldPart == null || oldPart == prevPartition);
            // no need to change partition, just return old one
            if (!needChangePartition) {
                return oldPart;
            }

            // get all available partitions
            List<PartitionInfo> availablePartitions = cluster.availablePartitionsForTopic(topic);
            // if no available partition, return 0;
            if (availablePartitions == null || availablePartitions.size() <= 0) {
                return prevPartition;
            }
            // if only one partition, return this.
            int availablePartitionsSize = availablePartitions.size();
            if (availablePartitionsSize == 1) {
                int newPart = availablePartitions.get(0).partition();
                indexCache.put(topic, newPart);
                return newPart;
            }

            // get the number of producer under this client id
            int producerCount = clientIdIndex.get(clientId).get();

            // more than 1 producer and more partitions than producers, assign them equally.
            if (producerCount > 1 && availablePartitionsSize > producerCount) {
                availablePartitions = averageAssign(availablePartitions, producerCount, index);
                availablePartitionsSize = availablePartitions.size();
            }

            // only one partition or null old partition
            if (availablePartitionsSize == 1 || oldPart == null) {
                int newPart = availablePartitions.get(0).partition();
                indexCache.put(topic, newPart);
                return newPart;
            }

            // find old partition cursor
            int indexOldPart = -1;
            for (int i = 0; i < availablePartitionsSize; i++) {
                PartitionInfo partition = availablePartitions.get(i);
                if (partition.partition() == oldPart) {
                    indexOldPart = i;
                    break;
                }
            }

            // if no old cursor, return the new partition
            if (indexOldPart < 0) {
                int newPart = availablePartitions.get(0).partition();
                indexCache.replace(topic, prevPartition, newPart);
                return newPart;
            }

            // calculate cursor of new partition
            int indexNewPart = (indexOldPart + 1) % availablePartitionsSize;
            int newPart = availablePartitions.get(indexNewPart).partition();
            indexCache.replace(topic, prevPartition, newPart);
            return newPart;
        }
    }

    /**
     * group a batch of source, and get the part of given index.
     *
     * @param  source   source to be grouped
     * @param  pageSize size of group
     * @param  index    current index
     */
    public static <T> List<T> averageAssign(List<T> source, int pageSize, int index) {

        if (source == null || source.isEmpty()) {
            return source;
        }

        int remainder = source.size() % pageSize;
        int number = source.size() / pageSize;
        int fromIndex = index * number;

        fromIndex += Math.min(index, remainder);
        boolean hasRemainder = index < remainder;
        int toIndex = fromIndex + number + (hasRemainder ? 1 : 0);

        if (fromIndex >= source.size()) {
            return new ArrayList<>();
        }

        if (toIndex > source.size()) {
            toIndex = source.size();
        }

        return source.subList(fromIndex, toIndex);
    }

}
