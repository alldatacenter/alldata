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
import org.apache.inlong.sdk.sort.api.ClientContext;
import org.apache.inlong.sdk.sort.api.Deserializer;
import org.apache.inlong.sdk.sort.api.Interceptor;
import org.apache.inlong.sdk.sort.api.MultiTopicsFetcher;
import org.apache.inlong.sdk.sort.api.SeekerFactory;
import org.apache.inlong.sdk.sort.api.SortClientConfig;
import org.apache.inlong.sdk.sort.entity.InLongMessage;
import org.apache.inlong.sdk.sort.entity.InLongTopic;
import org.apache.inlong.sdk.sort.entity.MessageRecord;
import org.apache.inlong.sdk.sort.fetcher.pulsar.PulsarMultiTopicsFetcher;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.RangeAssignor;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Kafka multi topics fetcher
 */
public class KafkaMultiTopicsFetcher extends MultiTopicsFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(PulsarMultiTopicsFetcher.class);
    private final ConcurrentHashMap<TopicPartition, OffsetAndMetadata> commitOffsetMap;
    private final ConcurrentHashMap<TopicPartition, ConcurrentSkipListMap<Long, Boolean>> ackOffsetMap;
    private final String bootstrapServers;
    private ConsumerRebalanceListener listener;
    private KafkaConsumer<byte[], byte[]> consumer;

    public KafkaMultiTopicsFetcher(
            List<InLongTopic> topics,
            ClientContext context,
            Interceptor interceptor,
            Deserializer deserializer,
            String bootstrapServers,
            String fetchKey) {
        super(topics, context, interceptor, deserializer, fetchKey);
        this.bootstrapServers = bootstrapServers;
        this.commitOffsetMap = new ConcurrentHashMap<>();
        this.ackOffsetMap = new ConcurrentHashMap<>();
    }

    @Override
    public boolean init() {
        try {
            this.consumer = createKafkaConsumer();
            InLongTopic topic = onlineTopics.values().stream().findFirst().get();
            this.seeker = SeekerFactory.createKafkaSeeker(consumer, topic);
            this.listener = new AckOffsetOnRebalance(topic.getInLongCluster().getClusterId(), seeker,
                    commitOffsetMap, ackOffsetMap, consumer);
            consumer.subscribe(onlineTopics.keySet(), listener);
            LOGGER.info("init kafka multi topic fetcher success, bootstrap is {}, fetchKey is {}",
                    bootstrapServers, fetchKey);
            String threadName = String.format("sort_sdk_kafka_multi_topic_fetch_thread_%s_%s",
                    topic.getInLongCluster().getClusterId(), this.fetchKey);
            this.fetchThread = new Thread(new KafkaMultiTopicsFetcher.Fetcher(), threadName);
            fetchThread.start();
            return true;
        } catch (Throwable t) {
            LOGGER.error("failed to init kafka consumer: ", t);
            return false;
        }
    }

    private KafkaConsumer<byte[], byte[]> createKafkaConsumer() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, context.getConfig().getSortTaskId());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                ByteArrayDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                ByteArrayDeserializer.class.getName());
        properties.put(ConsumerConfig.RECEIVE_BUFFER_CONFIG,
                context.getConfig().getKafkaSocketRecvBufferSize());
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        SortClientConfig.ConsumeStrategy offsetResetStrategy = context.getConfig().getOffsetResetStrategy();
        if (offsetResetStrategy == SortClientConfig.ConsumeStrategy.lastest
                || offsetResetStrategy == SortClientConfig.ConsumeStrategy.lastest_absolutely) {
            properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        } else if (offsetResetStrategy == SortClientConfig.ConsumeStrategy.earliest
                || offsetResetStrategy == SortClientConfig.ConsumeStrategy.earliest_absolutely) {
            properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        } else {
            properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "none");
        }
        properties.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG,
                context.getConfig().getKafkaFetchSizeBytes());
        properties.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG,
                context.getConfig().getKafkaFetchWaitMs());
        properties.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG,
                RangeAssignor.class.getName());
        properties.put(ConsumerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 120000L);
        LOGGER.info("start to create kafka consumer:{}", properties);
        return new KafkaConsumer<>(properties);
    }

    @Override
    public void ack(String msgOffset) throws Exception {
        LOGGER.info("ack {}", msgOffset);
        // the format of multi topic kafka fetcher msg offset is topic:partitionId:offset, such as topic1:20:1746839
        String[] offset = msgOffset.split(":");
        if (offset.length != 3) {
            throw new Exception("offset is illegal, the correct format is topic:partitionId:offset, "
                    + "the error offset is:" + msgOffset);
        }

        // parse topic partition offset
        TopicPartition topicPartition = new TopicPartition(offset[0], Integer.parseInt(offset[1]));
        long ackOffset = Long.parseLong(offset[2]);

        // ack
        if (!ackOffsetMap.containsKey(topicPartition) || !ackOffsetMap.get(topicPartition).containsKey(ackOffset)) {
            LOGGER.warn("did not find offsetMap to ack offset of {}, offset {}, just ignore it",
                    topicPartition, ackOffset);
            return;
        }

        // mark this offset has been ack.
        ConcurrentSkipListMap<Long, Boolean> tpOffsetMap = ackOffsetMap.get(topicPartition);
        // to prevent race condition in AckOffsetOnRebalance::onPartitionsRevoked
        if (Objects.nonNull(tpOffsetMap)) {
            tpOffsetMap.put(ackOffset, true);
        }
    }

    @Override
    public void pause() {
        consumer.pause(consumer.assignment());
    }

    @Override
    public void resume() {
        consumer.resume(consumer.assignment());
    }

    @Override
    public boolean close() {
        this.closed = true;
        try {
            if (fetchThread != null) {
                fetchThread.interrupt();
            }
            if (consumer != null) {
                prepareCommit();
                consumer.commitSync(commitOffsetMap);
                consumer.close();
            }
            ackOffsetMap.clear();
            commitOffsetMap.clear();
        } catch (Throwable t) {
            LOGGER.warn("got exception in multi topic fetcher close: ", t);
        }
        LOGGER.info("closed kafka multi topic fetcher");
        return true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void setStopConsume(boolean stopConsume) {
        this.stopConsume = stopConsume;
    }

    @Override
    public boolean isStopConsume() {
        return stopConsume;
    }

    @Override
    public List<InLongTopic> getTopics() {
        return new ArrayList<>(onlineTopics.values());
    }

    @Override
    public boolean updateTopics(List<InLongTopic> topics) {
        if (needUpdate(topics)) {
            LOGGER.info("need to update topic");
            newTopics = topics;
            return true;
        }
        LOGGER.info("no need to update topics");
        return false;
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

    public class Fetcher implements Runnable {

        private boolean subscribeNew() {
            if (CollectionUtils.isEmpty(newTopics)) {
                return false;
            }
            LOGGER.info("start to update topics");
            try {
                // update
                onlineTopics = newTopics.stream().collect(Collectors.toMap(InLongTopic::getTopic, t -> t));
                InLongTopic topic = onlineTopics.values().stream().findFirst().get();
                seeker = SeekerFactory.createKafkaSeeker(consumer, topic);
                Optional.ofNullable(interceptor).ifPresent(i -> i.configure(topic));
                LOGGER.info("new subscribe topic is {}", onlineTopics.keySet());
                // subscribe new
                consumer.subscribe(onlineTopics.keySet(), listener);
                return true;
            } finally {
                newTopics = null;
            }
        }

        private void commitKafkaOffset() {
            prepareCommit();
            if (consumer != null) {
                try {
                    LOGGER.info("commit {}", commitOffsetMap);
                    consumer.commitSync(commitOffsetMap);
                    commitOffsetMap.clear();
                } catch (Exception e) {
                    LOGGER.error("commit kafka offset failed: ", e);
                }
            }
        }

        /**
         * put the received msg to onFinished method
         *
         * @param messageRecords {@link List < MessageRecord >}
         */
        private void handleAndCallbackMsg(List<MessageRecord> messageRecords, InLongTopic topic, int partition) {
            long start = System.currentTimeMillis();
            try {
                context.addCallBack(topic, partition);
                context.getConfig().getCallback().onFinishedBatch(messageRecords);
                context.addCallBackSuccess(topic, partition, messageRecords.size(),
                        System.currentTimeMillis() - start);
            } catch (Exception e) {
                context.addCallBackFail(topic, partition, messageRecords.size(),
                        System.currentTimeMillis() - start);
                LOGGER.error("failed to callback: ", e);
            }
        }

        private String getOffset(String topic, int partitionId, long offset) {
            TopicPartition topicPartition = new TopicPartition(topic, partitionId);
            ackOffsetMap.computeIfAbsent(topicPartition, k -> new ConcurrentSkipListMap<>()).put(offset, false);
            return topic + ":" + partitionId + ":" + offset;
        }

        private Map<String, String> getMsgHeaders(Headers headers) {
            Map<String, String> headerMap = new HashMap<>();
            for (Header header : headers) {
                headerMap.put(header.key(), new String(header.value()));
            }
            return headerMap;
        }

        @Override
        public void run() {
            boolean hasPermit;
            while (true) {
                hasPermit = false;
                try {
                    if (context.getConfig().isStopConsume() || stopConsume) {
                        TimeUnit.MILLISECONDS.sleep(50);
                        continue;
                    }

                    if (sleepTime > 0) {
                        TimeUnit.MILLISECONDS.sleep(sleepTime);
                    }

                    context.acquireRequestPermit();
                    hasPermit = true;
                    // update topics
                    subscribeNew();
                    // commit
                    commitKafkaOffset();
                    // fetch from kafka
                    fetchFromKafka();
                } catch (Exception e) {
                    context.addConsumeError(null, -1, -1);
                    LOGGER.error("failed in kafka multi topic fetcher: ", e);
                } finally {
                    if (hasPermit) {
                        context.releaseRequestPermit();
                    }
                }
            }
        }

        private void fetchFromKafka() throws Exception {
            context.addConsumeTime(null, -1);

            long startFetchTime = System.currentTimeMillis();
            ConsumerRecords<byte[], byte[]> records = consumer
                    .poll(Duration.ofMillis(context.getConfig().getKafkaFetchWaitMs()));
            long fetchTimeCost = System.currentTimeMillis() - startFetchTime;

            if (null != records && !records.isEmpty()) {
                for (ConsumerRecord<byte[], byte[]> msg : records) {
                    List<MessageRecord> msgs = new ArrayList<>();
                    String topicName = msg.topic();
                    InLongTopic topic = onlineTopics.get(topicName);
                    String offsetKey = getOffset(topicName, msg.partition(), msg.offset());
                    List<InLongMessage> inLongMessages = deserializer
                            .deserialize(context, topic, getMsgHeaders(msg.headers()), msg.value());
                    context.addConsumeSuccess(topic, msg.partition(), inLongMessages.size(), msg.value().length,
                            fetchTimeCost);
                    int originSize = inLongMessages.size();
                    inLongMessages = interceptor.intercept(inLongMessages);
                    if (inLongMessages.isEmpty()) {
                        ack(offsetKey);
                        continue;
                    }
                    int filterSize = originSize - inLongMessages.size();
                    context.addConsumeFilter(topic, msg.partition(), filterSize);
                    msgs.add(new MessageRecord(fetchKey,
                            inLongMessages,
                            offsetKey, System.currentTimeMillis()));
                    handleAndCallbackMsg(msgs, topic, msg.partition());
                }
                sleepTime = 0L;
            } else {
                context.addConsumeEmpty(null, -1, fetchTimeCost);
                emptyFetchTimes++;
                if (emptyFetchTimes >= context.getConfig().getEmptyPollTimes()) {
                    sleepTime = Math.min((sleepTime += context.getConfig().getEmptyPollSleepStepMs()),
                            context.getConfig().getMaxEmptyPollSleepMs());
                    emptyFetchTimes = 0;
                }
            }
        }
    }
}
