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

import org.apache.commons.collections.CollectionUtils;
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
import org.apache.inlong.tubemq.client.exception.TubeClientException;
import org.apache.inlong.tubemq.client.factory.MessageSessionFactory;
import org.apache.inlong.tubemq.client.factory.TubeSingleSessionFactory;
import org.apache.pulsar.client.api.AuthenticationFactory;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Inlong manager that maintain the {@link org.apache.inlong.sdk.sort.api.MultiTopicsFetcher}.
 * It is suitable to the cases that topics share the same configurations.
 * And each consumer will consume multi topic.
 */
public class InlongMultiTopicManager extends TopicManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(InlongMultiTopicManager.class);

    private final Map<String, List<TopicFetcher>> pulsarFetchers = new ConcurrentHashMap<>();
    private final Map<String, List<TopicFetcher>> kafkaFetchers = new ConcurrentHashMap<>();
    private final Map<String, List<TopicFetcher>> tubeFetchers = new ConcurrentHashMap<>();
    private final Map<String, TopicFetcher> allFetchers = new ConcurrentHashMap<>();
    private Set<String> allTopics = new HashSet<>();
    private final PeriodicTask updateMetaDataWorker;

    private boolean stopAssign = false;
    private int consumerSize;

    public InlongMultiTopicManager(ClientContext context, QueryConsumeConfig queryConsumeConfig) {
        super(context, queryConsumeConfig);
        this.consumerSize = context.getConfig().getMaxConsumerSize();
        updateMetaDataWorker = new UpdateMetaDataThread(context.getConfig().getUpdateMetaDataIntervalSec(),
                TimeUnit.SECONDS);
        String threadName = "sortsdk_multi_topic_manager_" + context.getConfig().getSortTaskId()
                + "_" + StringUtil.formatDate(new Date(), "yyyy-MM-dd HH:mm:ss");
        updateMetaDataWorker.start(threadName);
        LOGGER.info("create InlongMultiTopicManager success");
    }

    @Override
    public boolean clean() {
        LOGGER.info("start clean {}", context.getConfig().getSortTaskId());
        close();
        offlineAllTopicsAndPartitions();
        LOGGER.info("end clean {}", context.getConfig().getSortTaskId());
        return true;
    }

    @Override
    public TopicFetcher addTopic(InLongTopic topic) {
        return null;
    }

    @Override
    public TopicFetcher removeTopic(InLongTopic topic, boolean closeFetcher) {
        return null;
    }

    @Override
    public TopicFetcher getFetcher(String fetchKey) {
        return allFetchers.get(fetchKey);
    }

    @Override
    public Collection<TopicFetcher> getAllFetchers() {
        return allFetchers.values();
    }

    @Override
    public Set<String> getManagedInLongTopics() {
        return allTopics;
    }

    @Override
    public void offlineAllTopicsAndPartitions() {
        String subscribeId = context.getConfig().getSortTaskId();
        try {
            LOGGER.info("start offline {}", subscribeId);
            stopAssign = true;
            Set<Map.Entry<String, TopicFetcher>> entries = allFetchers.entrySet();
            for (Map.Entry<String, TopicFetcher> entry : entries) {
                String fetchKey = entry.getKey();
                TopicFetcher topicFetcher = entry.getValue();
                boolean succ = false;
                if (topicFetcher != null) {
                    try {
                        succ = topicFetcher.close();
                    } catch (Exception e) {
                        LOGGER.error("got exception when close fetcher={}", topicFetcher.getTopics(), e);
                    }
                }
                LOGGER.info("close fetcher={} {}", fetchKey, succ);
            }
        } catch (Exception e) {
            LOGGER.error("got exception when offline topics and partitions, ", e);
        } finally {
            allFetchers.clear();
            kafkaFetchers.clear();
            pulsarFetchers.clear();
            tubeFetchers.clear();
            stopAssign = false;
            LOGGER.info("close finished {}", subscribeId);
        }
    }

    @Override
    public void close() {
        if (updateMetaDataWorker != null) {
            updateMetaDataWorker.stop();
        }
    }

    private void handleUpdatedConsumeConfig(List<InLongTopic> assignedTopics) {
        if (CollectionUtils.isEmpty(assignedTopics)) {
            LOGGER.warn("assignedTopics is null or empty, do nothing");
            return;
        }
        this.allTopics = assignedTopics.stream()
                .map(InLongTopic::getTopic)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());

        assignedTopics.stream()
                .filter(topic -> InlongTopicTypeEnum.KAFKA.getName().equalsIgnoreCase(topic.getTopicType()))
                .collect(Collectors.groupingBy(topic -> topic.getInLongCluster().getClusterId()))
                .forEach(this::updateKafkaFetcher);

        assignedTopics.stream()
                .filter(topic -> InlongTopicTypeEnum.PULSAR.getName().equalsIgnoreCase(topic.getTopicType()))
                .collect(Collectors.groupingBy(topic -> topic.getInLongCluster().getClusterId()))
                .forEach(this::updatePulsarFetcher);

        assignedTopics.stream()
                .filter(topic -> InlongTopicTypeEnum.TUBE.getName().equalsIgnoreCase(topic.getTopicType()))
                .collect(Collectors.groupingBy(topic -> topic.getInLongCluster().getClusterId()))
                .forEach(this::updateTubeFetcher);
    }

    private void updateKafkaFetcher(String clusterId, List<InLongTopic> topics) {
        List<TopicFetcher> fetchers = kafkaFetchers.computeIfAbsent(clusterId, k -> new ArrayList<>());
        if (CollectionUtils.isNotEmpty(fetchers)) {
            fetchers.forEach(fetcher -> fetcher.updateTopics(topics));
            return;
        }
        String bootstraps = topics.stream().findFirst().get().getInLongCluster().getBootstraps();
        TopicFetcherBuilder builder = TopicFetcherBuilder.newKafkaBuilder()
                .bootstrapServers(bootstraps)
                .topic(topics)
                .context(context);
        LOGGER.info("create new kafka multi topic consumer for bootstrap {}, size is {}", bootstraps, consumerSize);
        for (int i = 0; i < consumerSize; i++) {
            fetchers.add(builder.subscribe());
        }
        fetchers.forEach(topicFetcher -> allFetchers.put(topicFetcher.getFetchKey(), topicFetcher));
    }

    private void updatePulsarFetcher(String clusterId, List<InLongTopic> topics) {
        List<TopicFetcher> fetchers = pulsarFetchers.computeIfAbsent(clusterId, k -> new ArrayList<>());
        if (CollectionUtils.isNotEmpty(fetchers)) {
            fetchers.forEach(fetcher -> fetcher.updateTopics(topics));
            return;
        }
        InLongTopic topic = topics.stream().findFirst().get();
        LOGGER.info("create new pulsar multi topic consumer for bootstrap {}, size is {}",
                topic.getInLongCluster().getBootstraps(), consumerSize);
        for (int i = 0; i < consumerSize; i++) {
            try {
                PulsarClient pulsarClient = PulsarClient.builder()
                        .serviceUrl(topic.getInLongCluster().getBootstraps())
                        .authentication(AuthenticationFactory.token(topic.getInLongCluster().getToken()))
                        .build();
                TopicFetcher fetcher = TopicFetcherBuilder.newPulsarBuilder()
                        .pulsarClient(pulsarClient)
                        .topic(topics)
                        .context(context)
                        .subscribe();
                fetchers.add(fetcher);
                allFetchers.put(fetcher.getFetchKey(), fetcher);
            } catch (PulsarClientException e) {
                LOGGER.error("failed to create pulsar client for {}\n", topic.getInLongCluster().getBootstraps(), e);
            }
        }
    }

    private void updateTubeFetcher(String clusterId, List<InLongTopic> topics) {
        List<TopicFetcher> fetchers = tubeFetchers.computeIfAbsent(clusterId, k -> new ArrayList<>());
        if (CollectionUtils.isNotEmpty(fetchers)) {
            fetchers.forEach(fetcher -> fetcher.updateTopics(topics));
            return;
        }
        InLongTopic topic = topics.stream().findFirst().get();
        LOGGER.info("create new tube multi topic consumer for bootstrap {}, size is {}",
                topic.getInLongCluster().getBootstraps(), consumerSize);
        for (int i = 0; i < consumerSize; i++) {
            try {
                TubeClientConfig tubeConfig = new TubeClientConfig(topic.getInLongCluster().getBootstraps());
                MessageSessionFactory messageSessionFactory = new TubeSingleSessionFactory(tubeConfig);
                TubeConsumerCreator tubeConsumerCreator = new TubeConsumerCreator(messageSessionFactory,
                        tubeConfig);
                topics.forEach(tubeTopic -> {
                    TopicFetcher fetcher = TopicFetcherBuilder.newTubeBuilder()
                            .tubeConsumerCreater(tubeConsumerCreator)
                            .topic(tubeTopic)
                            .context(context)
                            .subscribe();
                    fetchers.add(fetcher);
                });
            } catch (TubeClientException e) {
                LOGGER.error("failed to create tube client for {}\n", topic.getInLongCluster().getBootstraps(), e);
            }
        }
        fetchers.forEach(topicFetcher -> allFetchers.put(topicFetcher.getFetchKey(), topicFetcher));
    }

    private class UpdateMetaDataThread extends PeriodicTask {

        public UpdateMetaDataThread(long runInterval, TimeUnit timeUnit) {
            super(runInterval, timeUnit, context.getConfig());
        }

        @Override
        protected void doWork() {
            logger.debug("InLongMultiTopicManagerImpl doWork");
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
