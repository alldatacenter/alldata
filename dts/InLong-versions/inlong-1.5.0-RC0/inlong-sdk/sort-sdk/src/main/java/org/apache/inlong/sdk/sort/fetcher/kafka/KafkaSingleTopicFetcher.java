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

import com.google.gson.Gson;
import org.apache.inlong.sdk.sort.api.ClientContext;
import org.apache.inlong.sdk.sort.api.Deserializer;
import org.apache.inlong.sdk.sort.api.SeekerFactory;
import org.apache.inlong.sdk.sort.api.SingleTopicFetcher;
import org.apache.inlong.sdk.sort.api.SortClientConfig;
import org.apache.inlong.sdk.sort.entity.InLongMessage;
import org.apache.inlong.sdk.sort.entity.InLongTopic;
import org.apache.inlong.sdk.sort.entity.MessageRecord;
import org.apache.inlong.sdk.sort.api.Interceptor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Kafka single topic fetcher.
 */
public class KafkaSingleTopicFetcher extends SingleTopicFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaSingleTopicFetcher.class);
    private final ConcurrentHashMap<TopicPartition, OffsetAndMetadata> commitOffsetMap = new ConcurrentHashMap<>();
    private String bootstrapServers;
    private KafkaConsumer<byte[], byte[]> consumer;

    public KafkaSingleTopicFetcher(
            InLongTopic inLongTopic,
            ClientContext context,
            Interceptor interceptor,
            Deserializer deserializer,
            String bootstrapServers) {
        super(inLongTopic, context, interceptor, deserializer);
        this.bootstrapServers = bootstrapServers;
    }

    @Override
    public boolean init() {
        try {
            createKafkaConsumer(bootstrapServers);
            if (consumer != null) {
                LOGGER.info("start to subscribe topic:{}", new Gson().toJson(topic));
                this.seeker = SeekerFactory.createKafkaSeeker(consumer, topic);
                consumer.subscribe(Collections.singletonList(topic.getTopic()),
                        new AckOffsetOnRebalance(this.topic.getInLongCluster().getClusterId(), seeker,
                                commitOffsetMap, consumer));
            } else {
                LOGGER.info("consumer is null");
                return false;
            }
            String threadName = String.format("sort_sdk_kafka_single_topic_fetch_thread_%s_%s_%d",
                    this.topic.getInLongCluster().getClusterId(), topic.getTopic(), this.hashCode());
            this.fetchThread = new Thread(new KafkaSingleTopicFetcher.Fetcher(), threadName);
            fetchThread.start();
            LOGGER.info("start to start thread:{}", threadName);
        } catch (Exception e) {
            LOGGER.error("fail to init kafka single topic fetcher: {}", e.getMessage(), e);
            return false;
        }
        return true;
    }

    @Override
    public void ack(String msgOffset) throws Exception {
        // the format of kafka msg offset is partitionId:offset, such as 20:1746839
        String[] offset = msgOffset.split(":");
        if (offset.length == 2) {
            TopicPartition topicPartition = new TopicPartition(topic.getTopic(), Integer.parseInt(offset[0]));
            OffsetAndMetadata offsetAndMetadata = new OffsetAndMetadata(Long.parseLong(offset[1]));
            commitOffsetMap.put(topicPartition, offsetAndMetadata);
        } else {
            throw new Exception("offset is illegal, the correct format is int:long ,the error offset is:" + msgOffset);
        }
    }

    @Override
    public void pause() {
        this.stopConsume = true;
    }

    @Override
    public void resume() {
        this.stopConsume = false;
    }

    @Override
    public boolean close() {
        this.closed = true;
        try {
            if (fetchThread != null) {
                fetchThread.interrupt();
            }
            if (consumer != null) {
                consumer.close();
            }
        } catch (Throwable t) {
            LOGGER.warn(t.getMessage(), t);
        }
        LOGGER.info("closed {}", topic);
        return true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void setStopConsume(boolean isStopConsume) {
        this.stopConsume = isStopConsume;
    }

    @Override
    public boolean isStopConsume() {
        return this.stopConsume;
    }

    @Override
    public List<InLongTopic> getTopics() {
        return Collections.singletonList(topic);
    }

    private void createKafkaConsumer(String bootstrapServers) {
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
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG,
                RangeAssignor.class.getName());
        properties.put(ConsumerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 120000L);
        this.bootstrapServers = bootstrapServers;
        LOGGER.info("start to create kafka consumer:{}", properties);
        this.consumer = new KafkaConsumer<>(properties);
        LOGGER.info("end to create kafka consumer:{}", consumer);
    }

    public class Fetcher implements Runnable {

        private void commitKafkaOffset() {
            if (consumer != null && commitOffsetMap.size() > 0) {
                try {
                    consumer.commitSync(commitOffsetMap);
                    commitOffsetMap.clear();
                } catch (Exception e) {
                    LOGGER.error("commit kafka offset failed: {}", e.getMessage(), e);
                }
            }
        }

        /**
         * put the received msg to onFinished method
         *
         * @param messageRecords {@link List < MessageRecord >}
         */
        private void handleAndCallbackMsg(List<MessageRecord> messageRecords, int partition) {
            long start = System.currentTimeMillis();
            try {
                context.addCallBack(topic, partition);
                context.getConfig().getCallback().onFinishedBatch(messageRecords);
                context.addCallBackSuccess(topic, partition, messageRecords.size(),
                        System.currentTimeMillis() - start);
            } catch (Exception e) {
                context.addCallBackFail(topic, partition, messageRecords.size(),
                        System.currentTimeMillis() - start);
                LOGGER.error("failed to callback: {}", e.getMessage(), e);
            }
        }

        private String getOffset(int partitionId, long offset) {
            return partitionId + ":" + offset;
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
                    // fetch from kafka
                    fetchFromKafka();
                    // commit
                    commitKafkaOffset();
                } catch (Exception e) {
                    context.addConsumeError(topic, -1, -1);
                    LOGGER.error(e.getMessage(), e);
                } finally {
                    if (hasPermit) {
                        context.releaseRequestPermit();
                    }
                }
            }
        }

        private void fetchFromKafka() throws Exception {
            context.addConsumeTime(topic, -1);

            long startFetchTime = System.currentTimeMillis();
            ConsumerRecords<byte[], byte[]> records = consumer
                    .poll(Duration.ofMillis(context.getConfig().getKafkaFetchWaitMs()));
            long fetchTimeCost = System.currentTimeMillis() - startFetchTime;
            if (null != records && !records.isEmpty()) {

                for (ConsumerRecord<byte[], byte[]> msg : records) {
                    List<MessageRecord> msgs = new ArrayList<>();
                    String offsetKey = getOffset(msg.partition(), msg.offset());
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

                    msgs.add(new MessageRecord(topic.getTopicKey(),
                            inLongMessages,
                            offsetKey, System.currentTimeMillis()));
                    handleAndCallbackMsg(msgs, msg.partition());
                }
                sleepTime = 0L;
            } else {
                context.addConsumeEmpty(topic, -1, fetchTimeCost);
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
