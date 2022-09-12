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

package org.apache.inlong.sdk.sort.impl.kafka;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.inlong.sdk.sort.api.ClientContext;
import org.apache.inlong.sdk.sort.api.InLongTopicFetcher;
import org.apache.inlong.sdk.sort.api.SortClientConfig.ConsumeStrategy;
import org.apache.inlong.sdk.sort.entity.InLongMessage;
import org.apache.inlong.sdk.sort.entity.InLongTopic;
import org.apache.inlong.sdk.sort.entity.MessageRecord;
import org.apache.inlong.sdk.sort.util.StringUtil;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InLongKafkaFetcherImpl extends InLongTopicFetcher {

    private final Logger logger = LoggerFactory.getLogger(InLongKafkaFetcherImpl.class);
    private final ConcurrentHashMap<TopicPartition, OffsetAndMetadata> commitOffsetMap = new ConcurrentHashMap<>();
    private final AtomicLong ackOffsets = new AtomicLong(0);
    private volatile boolean stopConsume = false;
    private String bootstrapServers;
    private KafkaConsumer<byte[], byte[]> consumer;

    public InLongKafkaFetcherImpl(InLongTopic inLongTopic, ClientContext context) {
        super(inLongTopic, context);
    }

    @Override
    public boolean init(Object object) {
        String bootstrapServers = (String) object;
        try {
            createKafkaConsumer(bootstrapServers);
            if (consumer != null) {
                consumer.subscribe(Collections.singletonList(inLongTopic.getTopic()),
                        new AckOffsetOnRebalance(consumer, commitOffsetMap));
            } else {
                return false;
            }
            this.bootstrapServers = bootstrapServers;
            String threadName = "sort_sdk_fetch_thread_" + StringUtil.formatDate(new Date());
            this.fetchThread = new Thread(new Fetcher(), threadName);
            this.fetchThread.start();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    @Override
    public void ack(String msgOffset) throws Exception {
        String[] offset = msgOffset.split(":");
        if (offset.length == 2) {
            TopicPartition topicPartition = new TopicPartition(inLongTopic.getTopic(), Integer.parseInt(offset[0]));
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
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        logger.info("closed {}", inLongTopic);
        return true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void stopConsume(boolean stopConsume) {
        this.stopConsume = stopConsume;
    }

    @Override
    public boolean isConsumeStop() {
        return this.stopConsume;
    }

    @Override
    public InLongTopic getInLongTopic() {
        return inLongTopic;
    }

    @Override
    public long getConsumedDataSize() {
        return 0;
    }

    @Override
    public long getAckedOffset() {
        return 0;
    }

    private void createKafkaConsumer(String bootstrapServers) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.CLIENT_ID_CONFIG, context.getConfig().getSortTaskId());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, context.getConfig().getSortTaskId());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                ByteArrayDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                ByteArrayDeserializer.class.getName());
        properties.put(ConsumerConfig.RECEIVE_BUFFER_CONFIG,
                context.getConfig().getKafkaSocketRecvBufferSize());
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        ConsumeStrategy offsetResetStrategy = context.getConfig().getOffsetResetStrategy();
        if (offsetResetStrategy == ConsumeStrategy.lastest
                || offsetResetStrategy == ConsumeStrategy.lastest_absolutely) {
            properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        } else if (offsetResetStrategy == ConsumeStrategy.earliest
                || offsetResetStrategy == ConsumeStrategy.earliest_absolutely) {
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
                "org.apache.kafka.clients.consumer.StickyAssignor");
        properties.put(ConsumerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 120000L);
        this.bootstrapServers = bootstrapServers;
        this.consumer = new KafkaConsumer<>(properties);
    }

    public class Fetcher implements Runnable {

        private void commitKafkaOffset() {
            if (consumer != null && commitOffsetMap.size() > 0) {
                try {
                    consumer.commitSync(commitOffsetMap);
                    commitOffsetMap.clear();
                    //TODO monitor commit succ

                } catch (Exception e) {
                    //TODO monitor commit fail
                }
            }
        }

        /**
         * put the received msg to onFinished method
         *
         * @param messageRecords {@link List < MessageRecord >}
         */
        private void handleAndCallbackMsg(List<MessageRecord> messageRecords) {
            long start = System.currentTimeMillis();
            try {
                context.getStatManager()
                        .getStatistics(context.getConfig().getSortTaskId(),
                                inLongTopic.getInLongCluster().getClusterId(), inLongTopic.getTopic())
                        .addCallbackTimes(1);
                context.getConfig().getCallback().onFinishedBatch(messageRecords);
                context.getStatManager()
                        .getStatistics(context.getConfig().getSortTaskId(),
                                inLongTopic.getInLongCluster().getClusterId(), inLongTopic.getTopic())
                        .addCallbackTimeCost(System.currentTimeMillis() - start).addCallbackDoneTimes(1);
            } catch (Exception e) {
                context.getStatManager()
                        .getStatistics(context.getConfig().getSortTaskId(),
                                inLongTopic.getInLongCluster().getClusterId(), inLongTopic.getTopic())
                        .addCallbackErrorTimes(1);
                logger.error(e.getMessage(), e);
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
                    context.getStatManager()
                            .getStatistics(context.getConfig().getSortTaskId(),
                                    inLongTopic.getInLongCluster().getClusterId(), inLongTopic.getTopic())
                            .addFetchErrorTimes(1);
                    logger.error(e.getMessage(), e);
                } finally {
                    if (hasPermit) {
                        context.releaseRequestPermit();
                    }
                }
            }
        }

        private void fetchFromKafka() throws Exception {
            context.getStatManager()
                    .getStatistics(context.getConfig().getSortTaskId(),
                            inLongTopic.getInLongCluster().getClusterId(), inLongTopic.getTopic())
                    .addMsgCount(1).addFetchTimes(1);

            long startFetchTime = System.currentTimeMillis();
            ConsumerRecords<byte[], byte[]> records = consumer
                    .poll(Duration.ofMillis(context.getConfig().getKafkaFetchWaitMs()));
            context.getStatManager()
                    .getStatistics(context.getConfig().getSortTaskId(),
                            inLongTopic.getInLongCluster().getClusterId(), inLongTopic.getTopic())
                    .addFetchTimeCost(System.currentTimeMillis() - startFetchTime);
            if (null != records && !records.isEmpty()) {

                List<MessageRecord> msgs = new ArrayList<>();
                for (ConsumerRecord<byte[], byte[]> msg : records) {
                    String offsetKey = getOffset(msg.partition(), msg.offset());
                    List<InLongMessage> inLongMessages = deserializer
                            .deserialize(context, inLongTopic, getMsgHeaders(msg.headers()), msg.value());
                    msgs.add(new MessageRecord(inLongTopic.getTopicKey(),
                            inLongMessages,
                            offsetKey, System.currentTimeMillis()));
                    context.getStatManager()
                            .getStatistics(context.getConfig().getSortTaskId(),
                                    inLongTopic.getInLongCluster().getClusterId(), inLongTopic.getTopic())
                            .addConsumeSize(msg.value().length);
                }
                context.getStatManager()
                        .getStatistics(context.getConfig().getSortTaskId(),
                                inLongTopic.getInLongCluster().getClusterId(), inLongTopic.getTopic())
                        .addMsgCount(msgs.size());
                handleAndCallbackMsg(msgs);
                sleepTime = 0L;
            } else {
                context.getStatManager()
                        .getStatistics(context.getConfig().getSortTaskId(),
                                inLongTopic.getInLongCluster().getClusterId(), inLongTopic.getTopic())
                        .addEmptyFetchTimes(1);
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
