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

package org.apache.inlong.sdk.sort.fetcher.pulsar;

import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.sdk.sort.api.ClientContext;
import org.apache.inlong.sdk.sort.api.Deserializer;
import org.apache.inlong.sdk.sort.api.SeekerFactory;
import org.apache.inlong.sdk.sort.api.SingleTopicFetcher;
import org.apache.inlong.sdk.sort.api.SortClientConfig;
import org.apache.inlong.sdk.sort.entity.InLongMessage;
import org.apache.inlong.sdk.sort.entity.InLongTopic;
import org.apache.inlong.sdk.sort.entity.MessageRecord;
import org.apache.inlong.sdk.sort.api.Interceptor;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.Messages;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.apache.pulsar.client.api.SubscriptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Pulsar single topic fetcher.
 */
public class PulsarSingleTopicFetcher extends SingleTopicFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(PulsarSingleTopicFetcher.class);
    private final ReentrantReadWriteLock mainLock = new ReentrantReadWriteLock(true);
    private final ConcurrentHashMap<String, MessageId> offsetCache = new ConcurrentHashMap<>();
    private Consumer<byte[]> consumer;
    private PulsarClient pulsarClient;

    public PulsarSingleTopicFetcher(
            InLongTopic inLongTopic,
            ClientContext context,
            Interceptor interceptor,
            Deserializer deserializer,
            PulsarClient pulsarClient) {
        super(inLongTopic, context, interceptor, deserializer);
        this.pulsarClient = pulsarClient;
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
        return Collections.singletonList(topic);
    }

    private void ackSucc(String offset) {
        offsetCache.remove(offset);
        context.addAckSuccess(topic, -1);
    }

    /**
     * ack Offset
     *
     * @param msgOffset String
     */
    @Override
    public void ack(String msgOffset) throws Exception {
        if (!StringUtils.isEmpty(msgOffset)) {
            try {
                if (consumer == null) {
                    context.addAckFail(topic, -1);
                    LOGGER.error("consumer == null {}", topic);
                    return;
                }
                MessageId messageId = offsetCache.get(msgOffset);
                if (messageId == null) {
                    context.addAckFail(topic, -1);
                    LOGGER.error("messageId == null {}", topic);
                    return;
                }
                consumer.acknowledgeAsync(messageId)
                        .thenAccept(consumer -> ackSucc(msgOffset))
                        .exceptionally(exception -> {
                            LOGGER.error("ack fail:{} {}",
                                    topic, msgOffset, exception);
                            context.addAckFail(topic, -1);
                            return null;
                        });
            } catch (Exception e) {
                context.addAckFail(topic, -1);
                LOGGER.error(e.getMessage(), e);
                throw e;
            }
        }
    }

    /**
     * create Consumer and fetch thread
     *
     * @return boolean
     */
    @Override
    public boolean init() {
        return createConsumer(pulsarClient);
    }

    private boolean createConsumer(PulsarClient client) {
        if (null == client) {
            LOGGER.error("pulsar client is null");
            return false;
        }
        try {
            SubscriptionInitialPosition position = SubscriptionInitialPosition.Latest;
            SortClientConfig.ConsumeStrategy offsetResetStrategy = context.getConfig().getOffsetResetStrategy();
            if (offsetResetStrategy == SortClientConfig.ConsumeStrategy.earliest
                    || offsetResetStrategy == SortClientConfig.ConsumeStrategy.earliest_absolutely) {
                LOGGER.info("the subscription initial position is earliest!");
                position = SubscriptionInitialPosition.Earliest;
            }

            consumer = client.newConsumer(Schema.BYTES)
                    .topic(topic.getTopic())
                    .subscriptionName(context.getConfig().getSortTaskId())
                    .subscriptionType(SubscriptionType.Shared)
                    .startMessageIdInclusive()
                    .subscriptionInitialPosition(position)
                    .ackTimeout(context.getConfig().getAckTimeoutSec(), TimeUnit.SECONDS)
                    .receiverQueueSize(context.getConfig().getPulsarReceiveQueueSize())
                    .subscribe();

            this.seeker = SeekerFactory.createPulsarSeeker(consumer, topic);
            String threadName = String.format("sort_sdk_pulsar_single_topic_fetch_thread_%s_%s_%d",
                    this.topic.getInLongCluster().getClusterId(), topic.getTopic(), this.hashCode());
            this.fetchThread = new Thread(new PulsarSingleTopicFetcher.Fetcher(), threadName);
            this.fetchThread.setDaemon(true);
            this.fetchThread.start();
        } catch (Exception e) {
            LOGGER.error("fail to create consumer", e);
            return false;
        }
        return true;
    }

    /**
     * pause
     */
    @Override
    public void pause() {
        if (consumer != null) {
            consumer.pause();
        }
    }

    /**
     * resume
     */
    @Override
    public void resume() {
        if (consumer != null) {
            consumer.resume();
        }
    }

    /**
     * close
     *
     * @return true/false
     */
    @Override
    public boolean close() {
        mainLock.writeLock().lock();
        try {
            try {
                if (consumer != null) {
                    consumer.close();
                }
            } catch (PulsarClientException e) {
                LOGGER.warn(e.getMessage(), e);
            }
            LOGGER.info("closed {}", topic);
            return true;
        } finally {
            this.closed = true;
            mainLock.writeLock().unlock();
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    public class Fetcher implements Runnable {

        /**
         * put the received msg to onFinished method
         *
         * @param messageRecords {@link List}
         */
        private void handleAndCallbackMsg(List<MessageRecord> messageRecords) {
            long start = System.currentTimeMillis();
            try {
                context.addCallBack(topic, -1);
                context.getConfig().getCallback().onFinishedBatch(messageRecords);
                context.addCallBackSuccess(topic, -1, messageRecords.size(),
                        System.currentTimeMillis() - start);
            } catch (Exception e) {
                context.addCallBackFail(topic, -1, messageRecords.size(),
                        System.currentTimeMillis() - start);
                LOGGER.error("failed to callback", e);
            }
        }

        private String getOffset(MessageId msgId) {
            return Base64.getEncoder().encodeToString(msgId.toByteArray());
        }

        @Override
        public void run() {
            boolean hasPermit;
            while (true) {
                try {
                    hasPermit = false;
                    long fetchTimeCost = -1;
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
                        context.addConsumeTime(topic, -1);

                        long startFetchTime = System.currentTimeMillis();
                        Messages<byte[]> messages = consumer.batchReceive();
                        fetchTimeCost = System.currentTimeMillis() - startFetchTime;
                        if (null != messages && messages.size() != 0) {
                            for (Message<byte[]> msg : messages) {
                                // if need seek
                                if (msg.getPublishTime() < seeker.getSeekTime()) {
                                    seeker.seek();
                                    break;
                                }

                                String offsetKey = getOffset(msg.getMessageId());
                                offsetCache.put(offsetKey, msg.getMessageId());

                                // deserialize
                                List<InLongMessage> inLongMessages = deserializer
                                        .deserialize(context, topic, msg.getProperties(), msg.getData());
                                context.addConsumeSuccess(topic, -1, inLongMessages.size(), msg.getData().length,
                                        fetchTimeCost);
                                int originSize = inLongMessages.size();
                                // intercept
                                inLongMessages = interceptor.intercept(inLongMessages);
                                if (inLongMessages.isEmpty()) {
                                    ack(offsetKey);
                                    continue;
                                }
                                int filterSize = originSize - inLongMessages.size();
                                context.addConsumeFilter(topic, -1, filterSize);

                                List<MessageRecord> msgs = new ArrayList<>();
                                msgs.add(new MessageRecord(topic.getTopicKey(),
                                        inLongMessages,
                                        offsetKey, System.currentTimeMillis()));
                                handleAndCallbackMsg(msgs);
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
                    } catch (Exception e) {
                        context.addConsumeError(topic, -1, fetchTimeCost);
                        LOGGER.error("failed to fetch msg", e);
                    } finally {
                        if (hasPermit) {
                            context.releaseRequestPermit();
                        }
                    }

                    if (closed) {
                        break;
                    }
                } catch (Throwable t) {
                    LOGGER.error("got exception while process fetching", t);
                }
            }
        }
    }
}
