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

package org.apache.inlong.sdk.sort.impl.tube;

import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import org.apache.inlong.sdk.sort.api.ClientContext;
import org.apache.inlong.sdk.sort.api.InLongTopicFetcher;
import org.apache.inlong.sdk.sort.api.SysConstants;
import org.apache.inlong.sdk.sort.entity.InLongMessage;
import org.apache.inlong.sdk.sort.entity.InLongTopic;
import org.apache.inlong.sdk.sort.entity.MessageRecord;
import org.apache.inlong.sdk.sort.util.StringUtil;
import org.apache.inlong.tubemq.client.config.ConsumerConfig;
import org.apache.inlong.tubemq.client.config.TubeClientConfig;
import org.apache.inlong.tubemq.client.consumer.ConsumerResult;
import org.apache.inlong.tubemq.client.consumer.PullMessageConsumer;
import org.apache.inlong.tubemq.corebase.Message;
import org.apache.inlong.tubemq.corebase.TErrCodeConstants;
import org.apache.pulsar.shade.org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InLongTubeFetcherImpl extends InLongTopicFetcher {

    private static final Logger LOG = LoggerFactory.getLogger(InLongTubeFetcherImpl.class);
    private PullMessageConsumer messageConsumer;
    private volatile Thread fetchThread;

    public InLongTubeFetcherImpl(InLongTopic inLongTopic, ClientContext context) {
        super(inLongTopic, context);
    }

    @Override
    public boolean init(Object object) {
        TubeConsumerCreater tubeConsumerCreater = (TubeConsumerCreater) object;
        TubeClientConfig tubeClientConfig = tubeConsumerCreater.getTubeClientConfig();
        try {
            ConsumerConfig consumerConfig = new ConsumerConfig(tubeClientConfig.getMasterInfo(),
                    context.getConfig().getSortTaskId());

            messageConsumer = tubeConsumerCreater.getMessageSessionFactory().createPullConsumer(consumerConfig);
            if (messageConsumer != null) {
                TreeSet<String> filters = null;
                if (inLongTopic.getProperties() != null && inLongTopic.getProperties().containsKey(
                        SysConstants.TUBE_TOPIC_FILTER_KEY)) {
                    filters = (TreeSet<String>) inLongTopic.getProperties().get(SysConstants.TUBE_TOPIC_FILTER_KEY);
                }
                messageConsumer.subscribe(inLongTopic.getTopic(), filters);
                messageConsumer.completeSubscribe();

                String threadName = "sort_sdk_fetch_thread_" + StringUtil.formatDate(new Date());
                this.fetchThread = new Thread(new Fetcher(), threadName);
                this.fetchThread.start();
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void ack(String msgOffset) throws Exception {
        if (!StringUtils.isEmpty(msgOffset)) {
            if (messageConsumer == null) {
                context.getStatManager()
                        .getStatistics(context.getConfig().getSortTaskId(),
                                inLongTopic.getInLongCluster().getClusterId(), inLongTopic.getTopic())
                        .addAckFailTimes(1L);
                LOG.warn("consumer == null");
                return;
            }

            try {
                ConsumerResult consumerResult = messageConsumer.confirmConsume(msgOffset, true);
                int errCode = consumerResult.getErrCode();
                if (TErrCodeConstants.SUCCESS != errCode) {
                    context.getStatManager()
                            .getStatistics(context.getConfig().getSortTaskId(),
                                    inLongTopic.getInLongCluster().getClusterId(), inLongTopic.getTopic())
                            .addAckFailTimes(1L);
                } else {
                    context.getStatManager()
                            .getStatistics(context.getConfig().getSortTaskId(),
                                    inLongTopic.getInLongCluster().getClusterId(), inLongTopic.getTopic())
                            .addAckSuccTimes(1L);
                }
            } catch (Exception e) {
                context.getStatManager().getStatistics(context.getConfig().getSortTaskId(),
                        inLongTopic.getInLongCluster().getClusterId(), inLongTopic.getTopic()).addAckFailTimes(1L);
                LOG.error(e.getMessage(), e);
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
            throwable.printStackTrace();
        } finally {
            this.closed = true;
        }
        LOG.info("closed {}", inLongTopic);
        return true;
    }

    @Override
    public boolean isClosed() {
        return closed;
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

    public class Fetcher implements Runnable {

        /**
         * put the received msg to onFinished method
         *
         * @param messageRecord {@link MessageRecord}
         */
        private void handleAndCallbackMsg(MessageRecord messageRecord) {
            long start = System.currentTimeMillis();
            try {
                context.getStatManager()
                        .getStatistics(context.getConfig().getSortTaskId(),
                                inLongTopic.getInLongCluster().getClusterId(), inLongTopic.getTopic())
                        .addCallbackTimes(1L);
                context.getConfig().getCallback().onFinishedBatch(Collections.singletonList(messageRecord));
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
                    if (context.getConfig().isStopConsume() || isStopConsume) {
                        TimeUnit.MILLISECONDS.sleep(50L);
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
                    ConsumerResult message = messageConsumer.getMessage();
                    context.getStatManager()
                            .getStatistics(context.getConfig().getSortTaskId(),
                                    inLongTopic.getInLongCluster().getClusterId(), inLongTopic.getTopic())
                            .addFetchTimeCost(System.currentTimeMillis() - startFetchTime);
                    if (null != message && TErrCodeConstants.SUCCESS == message.getErrCode()) {
                        List<InLongMessage> msgs = new ArrayList<>();
                        for (Message msg : message.getMessageList()) {
                            List<InLongMessage> deserialize = deserializer
                                    .deserialize(context, inLongTopic, getAttributeMap(msg.getAttribute()),
                                            msg.getData());
                            msgs.addAll(deserialize);
                            context.getStatManager()
                                    .getStatistics(context.getConfig().getSortTaskId(),
                                            inLongTopic.getInLongCluster().getClusterId(), inLongTopic.getTopic())
                                    .addMsgCount(deserialize.size()).addConsumeSize(msg.getData().length);
                        }

                        handleAndCallbackMsg(new MessageRecord(inLongTopic.getTopicKey(), msgs,
                                message.getConfirmContext(), System.currentTimeMillis()));
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
