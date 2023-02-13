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

package org.apache.inlong.sdk.sort.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.sdk.sort.api.ClientContext;
import org.apache.inlong.sdk.sort.api.InlongTopicTypeEnum;
import org.apache.inlong.sdk.sort.api.QueryConsumeConfig;
import org.apache.inlong.sdk.sort.api.TopicFetcher;
import org.apache.inlong.sdk.sort.api.TopicFetcherBuilder;
import org.apache.inlong.sdk.sort.api.TopicManager;
import org.apache.inlong.sdk.sort.entity.ConsumeConfig;
import org.apache.inlong.sdk.sort.entity.InLongTopic;
import org.apache.inlong.sdk.sort.fetcher.tube.TubeConsumerCreator;
import org.apache.inlong.sdk.sort.util.PeriodicTask;
import org.apache.inlong.sdk.sort.util.StringUtil;
import org.apache.inlong.tubemq.client.config.TubeClientConfig;
import org.apache.inlong.tubemq.client.factory.MessageSessionFactory;
import org.apache.inlong.tubemq.client.factory.TubeSingleSessionFactory;
import org.apache.pulsar.client.api.AuthenticationFactory;
import org.apache.pulsar.client.api.PulsarClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inlong manager that maintain the single topic fetchers.
 * It is suitable to the cases that each topic has its own configurations.
 * And each consumer only consume the very one topic.
 */
public class InlongSingleTopicManager extends TopicManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(InlongSingleTopicManager.class);

    private final ConcurrentHashMap<String, TopicFetcher> fetchers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PulsarClient> pulsarClients = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TubeConsumerCreator> tubeFactories = new ConcurrentHashMap<>();

    private final PeriodicTask updateMetaDataWorker;
    private boolean stopAssign = false;

    public InlongSingleTopicManager(ClientContext context, QueryConsumeConfig queryConsumeConfig) {
        super(context, queryConsumeConfig);
        updateMetaDataWorker = new UpdateMetaDataThread(context.getConfig().getUpdateMetaDataIntervalSec(),
                TimeUnit.SECONDS);
        String threadName = "sortsdk_single_topic_manager_" + context.getConfig().getSortTaskId()
                + "_" + StringUtil.formatDate(new Date(), "yyyy-MM-dd HH:mm:ss");
        updateMetaDataWorker.start(threadName);
    }

    @Override
    public TopicFetcher addTopic(InLongTopic topic) {

        try {
            TopicFetcher result = fetchers.get(topic.getTopicKey());
            if (result == null) {
                // create fetcher (pulsar,tube,kafka)
                TopicFetcher topicFetcher = createInLongTopicFetcher(topic);
                TopicFetcher preValue = fetchers.putIfAbsent(topic.getTopicKey(), topicFetcher);
                LOGGER.info("addFetcher :{}", topic.getTopicKey());
                result = topicFetcher;
                if (preValue != null) {
                    result = preValue;
                    if (topicFetcher != null) {
                        topicFetcher.close();
                    }
                    LOGGER.info("addFetcher create same fetcher {}", topic);
                }
            }
            return result;
        } catch (Throwable t) {
            LOGGER.error("got error when add fetcher: {}", t.getMessage(), t);
            return null;
        }
    }

    /**
     * create fetcher (pulsar,tube,kafka)
     *
     * @param topic {@link InLongTopic}
     * @return {@link TopicFetcher}
     */
    private TopicFetcher createInLongTopicFetcher(InLongTopic topic) {
        if (InlongTopicTypeEnum.PULSAR.getName().equalsIgnoreCase(topic.getTopicType())) {
            LOGGER.info("the topic is pulsar {}", topic);
            return TopicFetcherBuilder.newPulsarBuilder()
                    .pulsarClient(pulsarClients.get(topic.getInLongCluster().getClusterId()))
                    .topic(topic)
                    .context(context)
                    .subscribe();
        } else if (InlongTopicTypeEnum.KAFKA.getName().equalsIgnoreCase(topic.getTopicType())) {
            LOGGER.info("the topic is kafka {}", topic);
            return TopicFetcherBuilder.newKafkaBuilder()
                    .bootstrapServers(topic.getInLongCluster().getBootstraps())
                    .topic(topic)
                    .context(context)
                    .subscribe();
        } else if (InlongTopicTypeEnum.TUBE.getName().equalsIgnoreCase(topic.getTopicType())) {
            LOGGER.info("the topic is tube {}", topic);
            return TopicFetcherBuilder.newTubeBuilder()
                    .tubeConsumerCreater(tubeFactories.get(topic.getInLongCluster().getClusterId()))
                    .topic(topic)
                    .context(context)
                    .subscribe();
        } else {
            LOGGER.error("topic type not support " + topic.getTopicType());
            return null;
        }
    }

    @Override
    public TopicFetcher removeTopic(InLongTopic topic, boolean closeFetcher) {
        TopicFetcher result = fetchers.remove(topic.getTopicKey());
        if (result != null && closeFetcher) {
            result.close();
        }
        return result;
    }

    @Override
    public TopicFetcher getFetcher(String fetchKey) {
        return fetchers.get(fetchKey);
    }

    @Override
    public Set<String> getManagedInLongTopics() {
        return new HashSet<>(fetchers.keySet());
    }

    @Override
    public Collection<TopicFetcher> getAllFetchers() {
        return fetchers.values();
    }

    /**
     * offline all inlong topic
     */
    @Override
    public void offlineAllTopicsAndPartitions() {
        String subscribeId = context.getConfig().getSortTaskId();
        try {
            LOGGER.info("start offline {}", subscribeId);
            stopAssign = true;
            closeAllFetcher();
            LOGGER.info("close finished {}", subscribeId);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        if (updateMetaDataWorker != null) {
            updateMetaDataWorker.stop();
        }
    }

    @Override
    public boolean clean() {
        String sortTaskId = context.getConfig().getSortTaskId();
        try {
            LOGGER.info("start close {}", sortTaskId);

            if (updateMetaDataWorker != null) {
                updateMetaDataWorker.stop();
            }

            closeFetcher();
            closePulsarClient();
            closeTubeSessionFactory();
            LOGGER.info("close finished {}", sortTaskId);
            return true;
        } catch (Throwable th) {
            LOGGER.error("close error " + sortTaskId, th);
        }
        return false;
    }

    private void closeAllFetcher() {
        closeFetcher();
    }

    private void closeFetcher() {
        Set<Entry<String, TopicFetcher>> entries = fetchers.entrySet();
        for (Entry<String, TopicFetcher> entry : entries) {
            String fetchKey = entry.getKey();
            TopicFetcher topicFetcher = entry.getValue();
            boolean succ = false;
            if (topicFetcher != null) {
                try {
                    succ = topicFetcher.close();
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
            LOGGER.info("close fetcher{} {}", fetchKey, succ);
        }
    }

    private void closePulsarClient() {
        for (Map.Entry<String, PulsarClient> entry : pulsarClients.entrySet()) {
            PulsarClient pulsarClient = entry.getValue();
            String key = entry.getKey();
            try {
                if (pulsarClient != null) {
                    pulsarClient.close();
                }
            } catch (Exception e) {
                LOGGER.error("close PulsarClient" + key + " error.", e);
            }
        }
        pulsarClients.clear();
    }

    private void closeTubeSessionFactory() {
        for (Map.Entry<String, TubeConsumerCreator> entry : tubeFactories.entrySet()) {
            MessageSessionFactory tubeMessageSessionFactory = entry.getValue().getMessageSessionFactory();
            String key = entry.getKey();
            try {
                if (tubeMessageSessionFactory != null) {
                    tubeMessageSessionFactory.shutdown();
                }
            } catch (Exception e) {
                LOGGER.error("close MessageSessionFactory" + key + " error.", e);
            }
        }
        tubeFactories.clear();
    }

    private void handleUpdatedConsumeConfig(List<InLongTopic> assignedTopics) {
        if (null == assignedTopics) {
            LOGGER.warn("assignedTopics is null, do nothing");
            return;
        }

        List<String> newTopics = assignedTopics.stream()
                .filter(inlongTopic -> StringUtils.isNotBlank(inlongTopic.getTopic()))
                .map(InLongTopic::getTopicKey)
                .collect(Collectors.toList());
        LOGGER.debug("assignedTopics name: {}", Arrays.toString(newTopics.toArray()));

        List<String> oldTopics = new ArrayList<>(fetchers.keySet());
        LOGGER.debug("oldTopics :{}", Arrays.toString(oldTopics.toArray()));
        // get need be offline topics
        oldTopics.removeAll(newTopics);
        LOGGER.debug("removed oldTopics: {}", Arrays.toString(oldTopics.toArray()));

        // get new topics
        newTopics.removeAll(new ArrayList<>(fetchers.keySet()));
        LOGGER.debug("really new topics :{}", Arrays.toString(newTopics.toArray()));
        // offline need be offlined topics
        offlineRemovedTopic(oldTopics);
        // online new topics
        onlineNewTopic(assignedTopics, newTopics);
        // update remain topics
        updateRemainTopics(assignedTopics);
    }

    private void updateRemainTopics(List<InLongTopic> topics) {
        topics.forEach(topic -> {
            TopicFetcher fetcher = fetchers.get(topic.getTopicKey());
            if (!Objects.isNull(fetcher)) {
                fetcher.updateTopics(Collections.singletonList(topic));
            }
        });
    }

    /**
     * offline inlong topic which not belong the sortTaskId
     *
     * @param oldTopics {@link List}
     */
    private void offlineRemovedTopic(List<String> oldTopics) {
        for (String fetchKey : oldTopics) {
            LOGGER.info("offlineRemovedTopic {}", fetchKey);
            InLongTopic topic = fetchers.get(fetchKey).getTopics().get(0);
            TopicFetcher topicFetcher = fetchers.get(fetchKey);
            if (topicFetcher != null) {
                topicFetcher.close();
            }
            fetchers.remove(fetchKey);
            if (context != null && topic != null) {
                context.addTopicOfflineCount(1);
            } else {
                LOGGER.error("context == null or context.getStatManager() == null or inLongTopic == null :{}",
                        topic);
            }
        }
    }

    /**
     * online new inlong topic
     *
     * @param newSubscribedInLongTopics List
     * @param reallyNewTopic List
     */
    private void onlineNewTopic(List<InLongTopic> newSubscribedInLongTopics, List<String> reallyNewTopic) {
        for (InLongTopic topic : newSubscribedInLongTopics) {
            if (!reallyNewTopic.contains(topic.getTopicKey())) {
                LOGGER.debug("!reallyNewTopic.contains(inLongTopic.getTopicKey())");
                continue;
            }
            onlineTopic(topic);
        }
    }

    private void onlineTopic(InLongTopic topic) {
        if (InlongTopicTypeEnum.PULSAR.getName().equalsIgnoreCase(topic.getTopicType())) {
            LOGGER.info("the topic is pulsar:{}", topic);
            onlinePulsarTopic(topic);
        } else if (InlongTopicTypeEnum.KAFKA.getName().equalsIgnoreCase(topic.getTopicType())) {
            LOGGER.info("the topic is kafka:{}", topic);
            onlineKafkaTopic(topic);
        } else if (InlongTopicTypeEnum.TUBE.getName().equalsIgnoreCase(topic.getTopicType())) {
            LOGGER.info("the topic is tube:{}", topic);
            onlineTubeTopic(topic);
        } else {
            LOGGER.error("topic type:{} not support", topic.getTopicType());
        }
    }

    private void onlinePulsarTopic(InLongTopic topic) {
        if (!checkAndCreateNewPulsarClient(topic)) {
            LOGGER.error("checkAndCreateNewPulsarClient error:{}", topic);
            return;
        }
        createNewFetcher(topic);
    }

    private boolean checkAndCreateNewPulsarClient(InLongTopic topic) {
        if (!pulsarClients.containsKey(topic.getInLongCluster().getClusterId())) {
            if (topic.getInLongCluster().getBootstraps() != null) {
                try {
                    PulsarClient pulsarClient = PulsarClient.builder()
                            .serviceUrl(topic.getInLongCluster().getBootstraps())
                            .authentication(AuthenticationFactory.token(topic.getInLongCluster().getToken()))
                            .build();
                    pulsarClients.put(topic.getInLongCluster().getClusterId(), pulsarClient);
                    LOGGER.debug("create pulsar client succ {}",
                            new String[]{topic.getInLongCluster().getClusterId(),
                                    topic.getInLongCluster().getBootstraps(),
                                    topic.getInLongCluster().getToken()});
                } catch (Exception e) {
                    LOGGER.error("create pulsar client error {}", topic);
                    LOGGER.error(e.getMessage(), e);
                    return false;
                }
            } else {
                LOGGER.error("bootstrap is null {}", topic.getInLongCluster());
                return false;
            }
        }
        LOGGER.info("create pulsar client true {}", topic);
        return true;
    }

    private boolean checkAndCreateNewTubeSessionFactory(InLongTopic inLongTopic) {
        if (!tubeFactories.containsKey(inLongTopic.getInLongCluster().getClusterId())) {
            if (inLongTopic.getInLongCluster().getBootstraps() != null) {
                try {
                    // create MessageSessionFactory
                    TubeClientConfig tubeConfig = new TubeClientConfig(inLongTopic.getInLongCluster().getBootstraps());
                    MessageSessionFactory messageSessionFactory = new TubeSingleSessionFactory(tubeConfig);
                    TubeConsumerCreator tubeConsumerCreator = new TubeConsumerCreator(messageSessionFactory,
                            tubeConfig);
                    tubeFactories.put(inLongTopic.getInLongCluster().getClusterId(), tubeConsumerCreator);
                    LOGGER.debug("create tube client succ {} {} {}",
                            new String[]{inLongTopic.getInLongCluster().getClusterId(),
                                    inLongTopic.getInLongCluster().getBootstraps(),
                                    inLongTopic.getInLongCluster().getToken()});
                } catch (Exception e) {
                    LOGGER.error("create tube client error {}", inLongTopic);
                    LOGGER.error(e.getMessage(), e);
                    return false;
                }
            } else {
                LOGGER.info("bootstrap is null {}", inLongTopic.getInLongCluster());
                return false;
            }
        }
        LOGGER.info("create pulsar client true {}", inLongTopic);
        return true;
    }

    private void onlineKafkaTopic(InLongTopic topic) {
        createNewFetcher(topic);
    }

    private void onlineTubeTopic(InLongTopic topic) {
        if (!checkAndCreateNewTubeSessionFactory(topic)) {
            LOGGER.error("checkAndCreateNewPulsarClient error:{}", topic);
            return;
        }
        createNewFetcher(topic);
    }

    private void createNewFetcher(InLongTopic topic) {
        if (!fetchers.containsKey(topic.getTopicKey())) {
            LOGGER.info("begin add Fetcher:{}", topic.getTopicKey());
            if (context != null) {
                context.addTopicOnlineCount(1);
                TopicFetcher fetcher = addTopic(topic);
                if (fetcher == null) {
                    fetchers.remove(topic.getTopicKey());
                    LOGGER.error("add fetcher error:{}", topic.getTopicKey());
                }
            } else {
                LOGGER.error("context == null or context.getStatManager() == null");
            }
        }
    }

    private class UpdateMetaDataThread extends PeriodicTask {

        public UpdateMetaDataThread(long runInterval, TimeUnit timeUnit) {
            super(runInterval, timeUnit, context.getConfig());
        }

        @Override
        protected void doWork() {
            logger.debug("InLongSingleTopicManagerImpl doWork");
            if (stopAssign) {
                logger.warn("assign is stopped");
                return;
            }
            // get sortTask conf from manager
            if (queryConsumeConfig != null) {
                long start = System.currentTimeMillis();
                context.addRequestManager();
                ConsumeConfig consumeConfig = queryConsumeConfig
                        .queryCurrentConsumeConfig(context.getConfig().getSortTaskId());
                if (consumeConfig != null) {
                    List<InLongTopic> topicSubset = context.getConfig().getConsumerSubset(consumeConfig.getTopics());
                    handleUpdatedConsumeConfig(topicSubset);
                } else {
                    logger.warn("subscribedInfo is null");
                    context.addRequestManagerFail(System.currentTimeMillis() - start);
                }
            } else {
                logger.error("subscribedMetaDataInfo is null");
            }
        }
    }
}
