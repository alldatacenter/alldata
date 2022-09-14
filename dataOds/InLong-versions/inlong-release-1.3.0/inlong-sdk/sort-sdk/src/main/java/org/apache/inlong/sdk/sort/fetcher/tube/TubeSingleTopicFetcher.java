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
 *
 */

package org.apache.inlong.sdk.sort.fetcher.tube;

import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.sdk.sort.api.ClientContext;
import org.apache.inlong.sdk.sort.api.Deserializer;
import org.apache.inlong.sdk.sort.api.SingleTopicFetcher;
import org.apache.inlong.sdk.sort.api.SysConstants;
import org.apache.inlong.sdk.sort.entity.InLongMessage;
import org.apache.inlong.sdk.sort.entity.InLongTopic;
import org.apache.inlong.sdk.sort.entity.MessageRecord;
import org.apache.inlong.sdk.sort.api.Interceptor;
import org.apache.inlong.tubemq.client.config.ConsumerConfig;
import org.apache.inlong.tubemq.client.config.TubeClientConfig;
import org.apache.inlong.tubemq.client.consumer.ConsumerResult;
import org.apache.inlong.tubemq.client.consumer.PullMessageConsumer;
import org.apache.inlong.tubemq.corebase.Message;
import org.apache.inlong.tubemq.corebase.TErrCodeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

/**
 * Tube single topic fetcher
 */
public class TubeSingleTopicFetcher extends SingleTopicFetcher {
    private static final Logger LOG = LoggerFactory.getLogger(TubeSingleTopicFetcher.class);
    private PullMessageConsumer messageConsumer;
    private volatile Thread fetchThread;
    private TubeConsumerCreator tubeConsumerCreator;

    public TubeSingleTopicFetcher(
            InLongTopic inLongTopic,
            ClientContext context,
            Interceptor interceptor,
            Deserializer deserializer,
            TubeConsumerCreator tubeConsumerCreator) {
        super(inLongTopic, context, interceptor, deserializer);
        this.tubeConsumerCreator = tubeConsumerCreator;
    }

    @Override
    public boolean init() {
        TubeClientConfig tubeClientConfig = tubeConsumerCreator.getTubeClientConfig();
        try {
            ConsumerConfig consumerConfig = new ConsumerConfig(tubeClientConfig.getMasterInfo(),
                    context.getConfig().getSortTaskId());

            messageConsumer = tubeConsumerCreator.getMessageSessionFactory().createPullConsumer(consumerConfig);
            if (messageConsumer != null) {
                TreeSet<String> filters = null;
                if (topic.getProperties() != null && topic.getProperties().containsKey(
                        SysConstants.TUBE_TOPIC_FILTER_KEY)) {
                    String filterStr = topic.getProperties().get(SysConstants.TUBE_TOPIC_FILTER_KEY);
                    String[] filterArray = filterStr.split(" ");
                    filters = new TreeSet<>(Arrays.asList(filterArray));
                }
                messageConsumer.subscribe(topic.getTopic(), filters);
                messageConsumer.completeSubscribe();

                String threadName = String.format("sort_sdk_tube_single_topic_fetch_thread_%s_%s_%d",
                        this.topic.getInLongCluster().getClusterId(), topic.getTopic(), this.hashCode());
                this.fetchThread = new Thread(new TubeSingleTopicFetcher.Fetcher(), threadName);
                this.fetchThread.start();
            } else {
                return false;
            }
        } catch (Exception e) {
            LOG.error("failed to init tube single topic fetcher");
            return false;
        }
        return true;
    }

    @Override
    public void ack(String msgOffset) throws Exception {
        if (!StringUtils.isEmpty(msgOffset)) {
            if (messageConsumer == null) {
                context.getStateCounterByTopic(topic).addAckFailTimes(1L);
                LOG.warn("consumer == null");
                return;
            }

            try {
                ConsumerResult consumerResult = messageConsumer.confirmConsume(msgOffset, true);
                int errCode = consumerResult.getErrCode();
                if (TErrCodeConstants.SUCCESS != errCode) {
                    context.getStateCounterByTopic(topic).addAckFailTimes(1L);
                } else {
                    context.getStateCounterByTopic(topic).addAckSuccTimes(1L);
                }
            } catch (Exception e) {
                context.getStateCounterByTopic(topic).addAckFailTimes(1L);
                LOG.error("failed to ack topic {}, msg is {}", topic.getTopic(), e.getMessage(), e);
                throw e;
            }
        }
    }

    @Override
    public void pause() {
        this.closed = true;
    }

    @Override
    public void resume() {
        this.closed = false;
    }

    @Override
    public boolean close() {
        try {
            if (fetchThread != null) {
                fetchThread.interrupt();
            }
            if (messageConsumer != null) {
                messageConsumer.shutdown();
            }
        } catch (Throwable throwable) {
            LOG.warn(throwable.getMessage(), throwable);
        } finally {
            this.closed = true;
        }
        LOG.info("closed {}", topic);
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
        return Collections.singletonList(topic);
    }

    public class Fetcher implements Runnable {

        /**
         * put the received msg to onFinished method
         *
         * @param messageRecord {@link MessageRecord}
         */
        private void handleAndCallbackMsg(MessageRecord messageRecord) {
            long start = System.currentTimeMillis();
            try {
                context.getStateCounterByTopic(topic).addCallbackTimes(1L);
                context.getConfig().getCallback().onFinishedBatch(Collections.singletonList(messageRecord));
                context.getStateCounterByTopic(topic)
                        .addCallbackTimeCost(System.currentTimeMillis() - start).addCallbackDoneTimes(1L);
            } catch (Exception e) {
                context.getStateCounterByTopic(topic).addCallbackErrorTimes(1L);
                LOG.error("failed to callback {}", e.getMessage(), e);
            }
        }

        /**
         * parseAttr from k1=v1&k2=v2 to kv map
         *
         * @param splitter {@link Splitter}
         * @param attr String
         * @param entrySplitterStr String
         * @return {@link Map}
         */
        private Map<String, String> parseAttr(Splitter splitter, String attr, String entrySplitterStr) {
            Map<String, String> map = new HashMap<>();
            for (String s : splitter.split(attr)) {
                int idx = s.indexOf(entrySplitterStr);
                String k = s;
                String v = null;
                if (idx > 0) {
                    k = s.substring(0, idx);
                    v = s.substring(idx + 1);
                }
                map.put(k, v);
            }
            return map;
        }

        private Map<String, String> getAttributeMap(String attribute) {
            final Splitter splitter = Splitter.on("&");
            return parseAttr(splitter, attribute, "=");
        }

        @Override
        public void run() {
            boolean hasPermit;
            while (true) {
                hasPermit = false;
                try {
                    if (context.getConfig().isStopConsume() || stopConsume) {
                        TimeUnit.MILLISECONDS.sleep(50L);
                        continue;
                    }

                    if (sleepTime > 0) {
                        TimeUnit.MILLISECONDS.sleep(sleepTime);
                    }

                    context.acquireRequestPermit();
                    hasPermit = true;
                    context.getStateCounterByTopic(topic).addMsgCount(1L).addFetchTimes(1L);

                    long startFetchTime = System.currentTimeMillis();
                    ConsumerResult message = messageConsumer.getMessage();
                    context.getStateCounterByTopic(topic).addFetchTimeCost(System.currentTimeMillis() - startFetchTime);
                    if (null != message && TErrCodeConstants.SUCCESS == message.getErrCode()) {
                        List<InLongMessage> msgs = new ArrayList<>();
                        for (Message msg : message.getMessageList()) {
                            List<InLongMessage> deserialize = deserializer
                                    .deserialize(context, topic, getAttributeMap(msg.getAttribute()),
                                            msg.getData());
                            deserialize = interceptor.intercept(deserialize);
                            if (deserialize.isEmpty()) {
                                continue;
                            }
                            msgs.addAll(deserialize);
                            context.getStateCounterByTopic(topic)
                                    .addMsgCount(deserialize.size())
                                    .addConsumeSize(msg.getData().length);
                        }

                        handleAndCallbackMsg(new MessageRecord(topic.getTopicKey(), msgs,
                                message.getConfirmContext(), System.currentTimeMillis()));
                        sleepTime = 0L;
                    } else {
                        context.getStateCounterByTopic(topic).addEmptyFetchTimes(1L);
                        emptyFetchTimes++;
                        if (emptyFetchTimes >= context.getConfig().getEmptyPollTimes()) {
                            sleepTime = Math.min((sleepTime += context.getConfig().getEmptyPollSleepStepMs()),
                                    context.getConfig().getMaxEmptyPollSleepMs());
                            emptyFetchTimes = 0;
                        }
                    }
                } catch (Exception e) {
                    context.getStateCounterByTopic(topic).addFetchErrorTimes(1L);
                    LOG.error(e.getMessage(), e);
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
