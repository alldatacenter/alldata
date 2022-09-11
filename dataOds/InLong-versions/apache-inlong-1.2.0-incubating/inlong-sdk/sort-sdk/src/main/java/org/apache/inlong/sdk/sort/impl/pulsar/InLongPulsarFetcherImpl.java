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

package org.apache.inlong.sdk.sort.impl.pulsar;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.inlong.sdk.sort.api.ClientContext;
import org.apache.inlong.sdk.sort.api.InLongTopicFetcher;
import org.apache.inlong.sdk.sort.entity.InLongMessage;
import org.apache.inlong.sdk.sort.entity.InLongTopic;
import org.apache.inlong.sdk.sort.entity.MessageRecord;
import org.apache.inlong.sdk.sort.util.StringUtil;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.Messages;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.SubscriptionType;
import org.apache.pulsar.shade.org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InLongPulsarFetcherImpl extends InLongTopicFetcher {

    private final Logger logger = LoggerFactory.getLogger(InLongPulsarFetcherImpl.class);
    private final ReentrantReadWriteLock mainLock = new ReentrantReadWriteLock(true);
    private final ConcurrentHashMap<String, MessageId> offsetCache = new ConcurrentHashMap<>();
    private Consumer<byte[]> consumer;

    public InLongPulsarFetcherImpl(InLongTopic inLongTopic,
            ClientContext context) {
        super(inLongTopic, context);
    }

    @Override
    public void stopConsume(boolean stopConsume) {
        this.isStopConsume = stopConsume;
    }

    @Override
    public boolean isConsumeStop() {
        return isStopConsume;
    }

    @Override
    public InLongTopic getInLongTopic() {
        return inLongTopic;
    }

    @Override
    public long getConsumedDataSize() {
        return 0L;
    }

    @Override
    public long getAckedOffset() {
        return 0L;
    }

    private void ackSucc(String offset) {
        offsetCache.remove(offset);
        context.getStatManager().getStatistics(context.getConfig().getSortTaskId(),
                inLongTopic.getInLongCluster().getClusterId(), inLongTopic.getTopic()).addAckSuccTimes(1L);
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
                    context.getStatManager().getStatistics(context.getConfig().getSortTaskId(),
                            inLongTopic.getInLongCluster().getClusterId(), inLongTopic.getTopic())
                            .addAckFailTimes(1L);
                    logger.error("consumer == null {}", inLongTopic);
                    return;
                }
                MessageId messageId = offsetCache.get(msgOffset);
                if (messageId == null) {
                    context.getStatManager().getStatistics(context.getConfig().getSortTaskId(),
                            inLongTopic.getInLongCluster().getClusterId(), inLongTopic.getTopic())
                            .addAckFailTimes(1L);
                    logger.error("messageId == null {}", inLongTopic);
                    return;
                }
                consumer.acknowledgeAsync(messageId)
                        .thenAccept(consumer -> ackSucc(msgOffset))
                        .exceptionally(exception -> {
                            logger.error("ack fail:{} {},error:{}", 
                                    inLongTopic, msgOffset, exception.getMessage(), exception);
                            context.getStatManager().getStatistics(context.getConfig().getSortTaskId(),
                                    inLongTopic.getInLongCluster().getClusterId(), inLongTopic.getTopic())
                                    .addAckFailTimes(1L);
                            return null;
                        });
            } catch (Exception e) {
                context.getStatManager().getStatistics(context.getConfig().getSortTaskId(),
                        inLongTopic.getInLongCluster().getClusterId(), inLongTopic.getTopic()).addAckFailTimes(1L);
                logger.error(e.getMessage(), e);
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
    public boolean init(Object object) {
        PulsarClient pulsarClient = (PulsarClient) object;
        return createConsumer(pulsarClient);
    }

    private boolean createConsumer(PulsarClient client) {
        if (null == client) {
            return false;
        }
        try {
            consumer = client.newConsumer(Schema.BYTES)
                    .topic(inLongTopic.getTopic())
                    .subscriptionName(context.getConfig().getSortTaskId())
                    .subscriptionType(SubscriptionType.Shared)
                    .startMessageIdInclusive()
                    .ackTimeout(context.getConfig().getAckTimeoutSec(), TimeUnit.SECONDS)
                    .receiverQueueSize(context.getConfig().getPulsarReceiveQueueSize())
                    .subscribe();

            String threadName = "sort_sdk_fetch_thread_" + StringUtil.formatDate(new Date());
            this.fetchThread = new Thread(new Fetcher(), threadName);
            this.fetchThread.start();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
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
                if (fetchThread != null) {
                    fetchThread.interrupt();
                }
            } catch (PulsarClientException e) {
                e.printStackTrace();
            }
            logger.info("closed {}", inLongTopic);
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
                context.getStatManager()
                        .getStatistics(context.getConfig().getSortTaskId(),
                                inLongTopic.getInLongCluster().getClusterId(), inLongTopic.getTopic())
                        .addCallbackTimes(1L);
                context.getConfig().getCallback().onFinishedBatch(messageRecords);
                context.getStatManager()
                        .getStatistics(context.getConfig().getSortTaskId(),
                                inLongTopic.getInLongCluster().getClusterId(), inLongTopic.getTopic())
                        .addCallbackTimeCost(System.currentTimeMillis() - start).addCallbackDoneTimes(1L);
            } catch (Exception e) {
                context.getStatManager()
                        .getStatistics(context.getConfig().getSortTaskId(),
                                inLongTopic.getInLongCluster().getClusterId(), inLongTopic.getTopic())
                        .addCallbackErrorTimes(1L);
                e.printStackTrace();
            }
        }

        private String getOffset(MessageId msgId) {
            return Base64.getEncoder().encodeToString(msgId.toByteArray());
        }

        @Override
        public void run() {
            boolean hasPermit;
            while (true) {
                hasPermit = false;
                try {
                    if (context.getConfig().isStopConsume() || isStopConsume) {
                        TimeUnit.MILLISECONDS.sleep(50);
                        continue;
                    }

                    if (sleepTime > 0) {
                        TimeUnit.MILLISECONDS.sleep(sleepTime);
                    }

                    context.acquireRequestPermit();
                    hasPermit = true;
                    context.getStatManager()
                            .getStatistics(context.getConfig().getSortTaskId(),
                                    inLongTopic.getInLongCluster().getClusterId(), inLongTopic.getTopic())
                            .addMsgCount(1L).addFetchTimes(1L);

                    long startFetchTime = System.currentTimeMillis();
                    Messages<byte[]> messages = consumer.batchReceive();
                    context.getStatManager()
                            .getStatistics(context.getConfig().getSortTaskId(),
                                    inLongTopic.getInLongCluster().getClusterId(), inLongTopic.getTopic())
                            .addFetchTimeCost(System.currentTimeMillis() - startFetchTime);
                    if (null != messages && messages.size() != 0) {
                        List<MessageRecord> msgs = new ArrayList<>();
                        for (Message<byte[]> msg : messages) {
                            String offsetKey = getOffset(msg.getMessageId());
                            offsetCache.put(offsetKey, msg.getMessageId());

                            List<InLongMessage> inLongMessages = deserializer
                                    .deserialize(context, inLongTopic, msg.getProperties(), msg.getData());

                            msgs.add(new MessageRecord(inLongTopic.getTopicKey(),
                                    inLongMessages,
                                    offsetKey, System.currentTimeMillis()));
                            context.getStatManager()
                                    .getStatistics(context.getConfig().getSortTaskId(),
                                            inLongTopic.getInLongCluster().getClusterId(), inLongTopic.getTopic())
                                    .addConsumeSize(msg.getData().length);
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
                                .addEmptyFetchTimes(1L);
                        emptyFetchTimes++;
                        if (emptyFetchTimes >= context.getConfig().getEmptyPollTimes()) {
                            sleepTime = Math.min((sleepTime += context.getConfig().getEmptyPollSleepStepMs()),
                                    context.getConfig().getMaxEmptyPollSleepMs());
                            emptyFetchTimes = 0;
                        }
                    }
                } catch (Exception e) {
                    context.getStatManager()
                            .getStatistics(context.getConfig().getSortTaskId(),
                                    inLongTopic.getInLongCluster().getClusterId(), inLongTopic.getTopic())
                            .addFetchErrorTimes(1L);
                    logger.error(e.getMessage(), e);
                } finally {
                    if (hasPermit) {
                        context.releaseRequestPermit();
                    }
                }

                if (closed) {
                    break;
                }
            }
        }
    }
}
