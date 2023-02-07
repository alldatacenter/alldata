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

package org.apache.inlong.tubemq.manager.executors;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.inlong.tubemq.manager.entry.MasterEntry;
import org.apache.inlong.tubemq.manager.entry.TopicTaskEntry;
import org.apache.inlong.tubemq.manager.repository.MasterRepository;
import org.apache.inlong.tubemq.manager.repository.TopicTaskRepository;
import org.apache.inlong.tubemq.manager.service.interfaces.MasterService;
import org.apache.inlong.tubemq.manager.service.interfaces.NodeService;
import org.apache.inlong.tubemq.manager.service.interfaces.TopicService;
import org.apache.inlong.tubemq.manager.service.tube.TubeHttpBrokerInfoList;
import org.apache.inlong.tubemq.manager.service.tube.TubeHttpTopicInfoList;
import org.apache.inlong.tubemq.manager.utils.ValidateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AddTopicExecutor {

    public static final int MAX_CONFIG_TOPIC_NUM = 100;
    @Autowired
    NodeService nodeService;

    @Autowired
    MasterRepository masterRepository;

    @Value("${manager.max.configurable.broker.size:50}")
    private int maxConfigurableBrokerSize;

    @Value("${manager.max.config.topic.retry.time:50}")
    private int maxRetryTimes = 5000;

    @Autowired
    TopicTaskRepository topicTaskRepository;

    @Autowired
    TopicService topicService;

    @Autowired
    MasterService masterService;

    /**
     * Add topic info
     *
     * @param clusterId this is the cluster id
     * @param topicTasks topic info
     */
    @Async("asyncExecutor")
    public void addTopicConfig(Long clusterId, List<TopicTaskEntry> topicTasks) {
        if (CollectionUtils.isEmpty(topicTasks)) {
            return;
        }
        List<List<TopicTaskEntry>> taskParts = ListUtils.partition(topicTasks, MAX_CONFIG_TOPIC_NUM);

        MasterEntry masterNode = masterService.getMasterNode(clusterId);
        TubeHttpBrokerInfoList brokerInfoList = nodeService.requestBrokerStatus(masterNode);
        if (ValidateUtils.isNull(brokerInfoList)) {
            return;
        }

        for (List<TopicTaskEntry> taskPart : taskParts) {
            Map<String, TopicTaskEntry> topicEntryMap = taskPart.stream().collect(
                    Collectors.toMap(TopicTaskEntry::getTopicName, topicTaskEntry -> topicTaskEntry, (v1, v2) -> v2));
            doConfigTopics(topicEntryMap, masterNode, brokerInfoList);
        }

    }

    private void doConfigTopics(Map<String, TopicTaskEntry> topicEntryMap,
            MasterEntry masterNode, TubeHttpBrokerInfoList brokerInfoList) {
        handleAddingTopic(masterNode, brokerInfoList, topicEntryMap);
        updateConfigResult(masterNode, topicEntryMap);
    }

    private void updateConfigResult(MasterEntry masterEntry, Map<String, TopicTaskEntry> pendingTopic) {
        TubeHttpBrokerInfoList brokerInfoList = nodeService.requestBrokerStatus(masterEntry);
        if (ValidateUtils.isNull(brokerInfoList)) {
            return;
        }
        for (String topic : pendingTopic.keySet()) {
            TubeHttpTopicInfoList topicInfoList = topicService.requestTopicConfigInfo(masterEntry, topic);
            if (ValidateUtils.isNull(topicInfoList)) {
                continue;
            }
            // get broker list by topic request
            Set<Integer> topicBrokerSet =
                    new HashSet<>(topicInfoList.getTopicBrokerIdList());
            List<Integer> allBrokerIdList = brokerInfoList.getAllBrokerIdList();

            // determine if all topics has been added
            TopicTaskEntry topicTask = pendingTopic.get(topic);
            updateTopicRepo(topicBrokerSet, allBrokerIdList, topicTask);
        }
    }

    private void updateTopicRepo(Set<Integer> topicBrokerSet, List<Integer> allBrokerIdList,
            TopicTaskEntry topicTask) {
        if (!topicBrokerSet.containsAll(allBrokerIdList)) {
            Integer retryTimes = topicTask.getConfigRetryTimes() + 1;
            topicTask.setConfigRetryTimes(retryTimes);
            topicTaskRepository.save(topicTask);
        }
    }

    private void handleAddingTopic(MasterEntry masterEntry,
            TubeHttpBrokerInfoList brokerInfoList,
            Map<String, TopicTaskEntry> pendingTopic) {
        // check tubemq cluster by topic name, remove pending topic if has added.
        Set<String> brandNewTopics = new HashSet<>();
        for (String topic : pendingTopic.keySet()) {
            TubeHttpTopicInfoList topicInfoList = topicService.requestTopicConfigInfo(masterEntry, topic);
            if (ValidateUtils.isNull(topicInfoList)) {
                return;
            }
            // get broker list by topic request
            List<Integer> topicBrokerList = topicInfoList.getTopicBrokerIdList();
            if (topicBrokerList.isEmpty()) {
                brandNewTopics.add(topic);
            } else {
                handleAddingExistTopics(masterEntry, brokerInfoList, topic, topicBrokerList);
            }
        }
        handleAddingNewTopics(masterEntry, brokerInfoList, brandNewTopics);
    }

    private void handleAddingExistTopics(MasterEntry masterEntry, TubeHttpBrokerInfoList brokerInfoList,
            String topic, List<Integer> topicBrokerList) {
        // remove brokers which have been added.
        List<Integer> configurableBrokerIdList =
                brokerInfoList.getConfigurableBrokerIdList();
        configurableBrokerIdList.removeAll(topicBrokerList);
        // add topic to satisfy max broker number.
        Set<String> singleTopic = new HashSet<>();
        singleTopic.add(topic);
        int maxBrokers = Math.min(maxConfigurableBrokerSize, configurableBrokerIdList.size());
        nodeService.configBrokersForTopics(masterEntry, singleTopic,
                configurableBrokerIdList, maxBrokers);
    }

    private void handleAddingNewTopics(MasterEntry masterEntry, TubeHttpBrokerInfoList brokerInfoList,
            Set<String> brandNewTopics) {
        if (CollectionUtils.isEmpty(brandNewTopics)) {
            return;
        }
        List<Integer> configurableBrokerIdList = brokerInfoList.getConfigurableBrokerIdList();
        int maxBrokers = Math.min(maxConfigurableBrokerSize, configurableBrokerIdList.size());
        nodeService.configBrokersForTopics(masterEntry, brandNewTopics,
                configurableBrokerIdList, maxBrokers);
    }

}
