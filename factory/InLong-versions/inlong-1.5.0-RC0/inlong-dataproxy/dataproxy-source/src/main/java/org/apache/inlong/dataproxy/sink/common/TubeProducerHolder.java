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

package org.apache.inlong.dataproxy.sink.common;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.lang3.StringUtils;
import org.apache.flume.FlumeException;
import org.apache.inlong.dataproxy.config.pojo.MQClusterConfig;
import org.apache.inlong.tubemq.client.config.TubeClientConfig;
import org.apache.inlong.tubemq.client.exception.TubeClientException;
import org.apache.inlong.tubemq.client.factory.TubeMultiSessionFactory;
import org.apache.inlong.tubemq.client.producer.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TubeProducerHolder {

    private static final Logger logger =
            LoggerFactory.getLogger(TubeProducerHolder.class);
    private static final long SEND_FAILURE_WAIT = 30000L;
    private static final long PUBLISH_FAILURE_WAIT = 60000L;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final String sinkName;
    private final String clusterAddr;
    private final MQClusterConfig clusterConfig;
    private TubeMultiSessionFactory sessionFactory = null;
    private final Map<String, MessageProducer> producerMap = new ConcurrentHashMap<>();
    private MessageProducer lastProducer = null;
    private final AtomicInteger lastPubTopicCnt = new AtomicInteger(0);
    private static final ConcurrentHashMap<String, AtomicLong> FROZEN_TOPIC_MAP = new ConcurrentHashMap<>();

    public TubeProducerHolder(String sinkName, String clusterAddr, MQClusterConfig tubeConfig) {
        Preconditions.checkState(StringUtils.isNotBlank(clusterAddr),
                "No TubeMQ's cluster address list specified");
        this.sinkName = sinkName;
        this.clusterAddr = clusterAddr;
        this.clusterConfig = tubeConfig;
    }

    public void start(Set<String> configTopicSet) {
        if (!this.started.compareAndSet(false, true)) {
            logger.info("ProducerHolder for " + sinkName + " has started!");
            return;
        }
        logger.info("ProducerHolder for " + sinkName + " begin to start!");
        // create session factory
        try {
            TubeClientConfig clientConfig = TubeUtils.buildClientConfig(clusterAddr, this.clusterConfig);
            this.sessionFactory = new TubeMultiSessionFactory(clientConfig);
            createProducersByTopicSet(configTopicSet);
        } catch (Throwable e) {
            stop();
            String errInfo = "Build session factory  to " + clusterAddr
                    + " for " + sinkName + " failure, please re-check";
            logger.error(errInfo, e);
            throw new FlumeException(errInfo);
        }
        logger.info("ProducerHolder for " + sinkName + " started!");
    }

    public void stop() {
        if (this.started.get()) {
            return;
        }
        // change start flag
        if (!this.started.compareAndSet(true, false)) {
            logger.info("ProducerHolder for " + sinkName + " has stopped!");
            return;
        }
        logger.info("ProducerHolder for " + sinkName + " begin to stop!");
        for (Map.Entry<String, MessageProducer> entry : producerMap.entrySet()) {
            if (entry == null || entry.getValue() == null) {
                continue;
            }
            try {
                entry.getValue().shutdown();
            } catch (Throwable e) {
                // ignore log
            }
        }
        producerMap.clear();
        lastProducer = null;
        lastPubTopicCnt.set(0);
        FROZEN_TOPIC_MAP.clear();
        if (sessionFactory != null) {
            try {
                sessionFactory.shutdown();
            } catch (Throwable e) {
                // ignore log
            }
            sessionFactory = null;
        }
        logger.info("ProducerHolder for " + sinkName + " finished stop!");
    }

    /**
     * Get producer by topic name:
     *   i. if the topic is judged to be an illegal topic, return null;
     *   ii. if it is not an illegal topic or the status has expired, check:
     *    a. if the topic has been published before, return the corresponding producer directly;
     *    b. if the topic is not in the published list, perform the topic's publish action.
     *  If the topic is thrown exception during the publishing process,
     *     set the topic to an illegal topic
     *
     * @param topicName  the topic name
     *
     * @return  the producer
     *          if topic is illegal, return null
     * @throws  TubeClientException
     */
    public MessageProducer getProducer(String topicName) throws TubeClientException {
        AtomicLong fbdTime = FROZEN_TOPIC_MAP.get(topicName);
        if (fbdTime != null && fbdTime.get() > System.currentTimeMillis()) {
            return null;
        }
        MessageProducer tmpProducer = producerMap.get(topicName);
        if (tmpProducer != null) {
            if (fbdTime != null) {
                FROZEN_TOPIC_MAP.remove(topicName);
            }
            return tmpProducer;
        }
        synchronized (lastPubTopicCnt) {
            fbdTime = FROZEN_TOPIC_MAP.get(topicName);
            if (fbdTime != null && fbdTime.get() > System.currentTimeMillis()) {
                return null;
            }
            if (lastProducer == null
                    || lastPubTopicCnt.get() >= clusterConfig.getMaxTopicsEachProducerHold()) {
                lastProducer = sessionFactory.createProducer();
                lastPubTopicCnt.set(0);
            }
            try {
                lastProducer.publish(topicName);
            } catch (Throwable e) {
                fbdTime = FROZEN_TOPIC_MAP.get(topicName);
                if (fbdTime == null) {
                    AtomicLong tmpFbdTime = new AtomicLong();
                    fbdTime = FROZEN_TOPIC_MAP.putIfAbsent(topicName, tmpFbdTime);
                    if (fbdTime == null) {
                        fbdTime = tmpFbdTime;
                    }
                }
                fbdTime.set(System.currentTimeMillis() + PUBLISH_FAILURE_WAIT);
                logger.warn("Throw exception while publish topic="
                        + topicName + ", exception is " + e.getMessage());
                return null;
            }
            producerMap.put(topicName, lastProducer);
            lastPubTopicCnt.incrementAndGet();
            return lastProducer;
        }
    }

    /**
     * Whether frozen production according to the exceptions returned by message sending
     *
     * @param topicName  the topic name sent message
     * @param throwable  the exception information thrown when sending a message
     *
     * @return  whether illegal topic
     */
    public boolean needFrozenSent(String topicName, Throwable throwable) {
        if (throwable instanceof TubeClientException) {
            String message = throwable.getMessage();
            if (message != null && (message.contains("No available partition for topic")
                    || message.contains("The brokers of topic are all forbidden"))) {
                AtomicLong fbdTime = FROZEN_TOPIC_MAP.get(topicName);
                if (fbdTime == null) {
                    AtomicLong tmpFbdTime = new AtomicLong(0);
                    fbdTime = FROZEN_TOPIC_MAP.putIfAbsent(topicName, tmpFbdTime);
                    if (fbdTime == null) {
                        fbdTime = tmpFbdTime;
                    }
                }
                fbdTime.set(System.currentTimeMillis() + SEND_FAILURE_WAIT);
                return true;
            }
        }
        return false;
    }

    /**
     * Create sink producers by configured topic set
     * group topicSet to different group, each group is associated with a producer
     *
     * @param cfgTopicSet  the configured topic set
     */
    public synchronized void createProducersByTopicSet(Set<String> cfgTopicSet) throws Exception {
        if (cfgTopicSet == null || cfgTopicSet.isEmpty()) {
            return;
        }
        // filter published topics
        List<String> filteredTopics = new ArrayList<>(cfgTopicSet.size());
        for (String topicName : cfgTopicSet) {
            if (StringUtils.isBlank(topicName)
                    || producerMap.get(topicName) != null) {
                continue;
            }
            filteredTopics.add(topicName);
        }
        if (filteredTopics.isEmpty()) {
            return;
        }
        // alloc topic count
        Collections.sort(filteredTopics);
        long startTime = System.currentTimeMillis();
        int maxPublishTopicCnt = clusterConfig.getMaxTopicsEachProducerHold();
        int allocTotalCnt = filteredTopics.size();
        List<Integer> topicGroupCnt = new ArrayList<>();
        int paddingCnt = (lastPubTopicCnt.get() <= 0)
                ? 0
                : (maxPublishTopicCnt - lastPubTopicCnt.get());
        while (allocTotalCnt > 0) {
            if (paddingCnt > 0) {
                topicGroupCnt.add(Math.min(allocTotalCnt, paddingCnt));
                allocTotalCnt -= paddingCnt;
                paddingCnt = 0;
            } else {
                topicGroupCnt.add(Math.min(allocTotalCnt, maxPublishTopicCnt));
                allocTotalCnt -= maxPublishTopicCnt;
            }
        }
        // create producer
        int startPos = 0;
        int endPos = 0;
        Set<String> subTopicSet = new HashSet<>();
        for (Integer dltCnt : topicGroupCnt) {
            // allocate topic items
            subTopicSet.clear();
            endPos = startPos + dltCnt;
            for (int index = startPos; index < endPos; index++) {
                subTopicSet.add(filteredTopics.get(index));
            }
            startPos = endPos;
            // create producer
            if (lastProducer == null
                    || lastPubTopicCnt.get() == maxPublishTopicCnt) {
                lastProducer = sessionFactory.createProducer();
                lastPubTopicCnt.set(0);
            }
            try {
                lastProducer.publish(subTopicSet);
            } catch (Throwable e) {
                logger.info(sinkName + " meta sink publish fail.", e);
            }
            lastPubTopicCnt.addAndGet(subTopicSet.size());
            for (String topicItem : subTopicSet) {
                producerMap.put(topicItem, lastProducer);
            }
        }
        logger.info(sinkName + " initializes producers for topics:"
                + producerMap.keySet() + ", cost: " + (System.currentTimeMillis() - startTime)
                + "ms");
    }

}
