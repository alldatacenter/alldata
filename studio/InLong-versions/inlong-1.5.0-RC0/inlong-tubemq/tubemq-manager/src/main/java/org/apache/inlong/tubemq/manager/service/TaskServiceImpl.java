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

package org.apache.inlong.tubemq.manager.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.transaction.Transactional;

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.tubemq.manager.controller.TubeMQResult;
import org.apache.inlong.tubemq.manager.entry.ClusterEntry;
import org.apache.inlong.tubemq.manager.entry.MasterEntry;
import org.apache.inlong.tubemq.manager.entry.TopicStatus;
import org.apache.inlong.tubemq.manager.entry.TopicTaskEntry;
import org.apache.inlong.tubemq.manager.enums.ErrorCode;
import org.apache.inlong.tubemq.manager.enums.TaskStatusEnum;
import org.apache.inlong.tubemq.manager.executors.AddTopicExecutor;
import org.apache.inlong.tubemq.manager.repository.MasterRepository;
import org.apache.inlong.tubemq.manager.repository.TopicTaskRepository;
import org.apache.inlong.tubemq.manager.service.interfaces.ClusterService;
import org.apache.inlong.tubemq.manager.service.interfaces.MasterService;
import org.apache.inlong.tubemq.manager.service.interfaces.NodeService;
import org.apache.inlong.tubemq.manager.service.interfaces.TaskService;
import org.apache.inlong.tubemq.manager.service.interfaces.TopicService;
import org.apache.inlong.tubemq.manager.service.tube.TopicView;
import org.apache.inlong.tubemq.manager.service.tube.TubeHttpBrokerInfoList;
import org.apache.inlong.tubemq.manager.utils.ValidateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TaskServiceImpl implements TaskService {

    public static final Integer MAX_RELOAD_TIMES = 500;

    @Autowired
    TopicTaskRepository topicTaskRepository;

    @Autowired
    ClusterService clusterService;

    @Autowired
    AddTopicExecutor addTopicExecutor;

    @Autowired
    NodeService nodeService;

    @Autowired
    MasterRepository masterRepository;

    @Autowired
    TopicService topicService;

    @Autowired
    MasterService masterService;

    @Override
    @Transactional
    public TubeMQResult addTopicCreateTask(Long clusterId, Set<String> topicNames) {
        try {
            List<TopicTaskEntry> topicTaskEntries = new ArrayList<>(64);
            Set<String> existTopicNames = findExistTopics(clusterId, topicNames);
            if (!existTopicNames.isEmpty()) {
                return TubeMQResult.errorResult("There are topic tasks "
                        + existTopicNames + " already in adding status", ErrorCode.TASK_EXIST.getCode());
            }
            topicNames.forEach(
                    topicName -> {
                        TopicTaskEntry entry = new TopicTaskEntry();
                        entry.setClusterId(clusterId);
                        entry.setTopicName(topicName);
                        topicTaskEntries.add(entry);
                    });
            topicTaskRepository.saveAll(topicTaskEntries);
        } catch (Exception e) {
            log.error("save topic tasks to db fail, topics : {}", topicNames, e);
            return TubeMQResult.errorResult(TubeMQErrorConst.MYSQL_ERROR);
        }
        return TubeMQResult.successResult();
    }

    private Set<String> findExistTopics(Long clusterId, Set<String> topicNames) {
        Set<String> existTopicName = topicNames.stream()
                .filter(topicName -> hasAlreadyExistTopicTask(clusterId, topicName, TaskStatusEnum.ADDING
                        .getCode()))
                .collect(Collectors.toSet());
        return existTopicName;
    }

    public boolean hasAlreadyExistTopicTask(Long clusterId, String topicName, Integer status) {
        TopicTaskEntry taskEntry = topicTaskRepository
                .findTopicTaskEntryByClusterIdAndStatusAndTopicName(clusterId, status, topicName);
        return taskEntry != null;
    }

    @Scheduled(cron = "${topic.config.schedule}")
    public void executeTopicConfigTasks() {
        List<ClusterEntry> allClusters = clusterService.getAllClusters();
        for (ClusterEntry cluster : allClusters) {
            long clusterId = cluster.getClusterId();
            List<TopicTaskEntry> topicTasks =
                    topicTaskRepository
                            .findTopicTaskEntriesByClusterIdAndStatus(clusterId, TaskStatusEnum.ADDING.getCode());
            addTopicExecutor.addTopicConfig(clusterId, topicTasks);
        }
    }

    @Scheduled(cron = "${broker.reload.schedule}")
    public void reloadBrokers() {
        List<ClusterEntry> allClusters = clusterService.getAllClusters();
        for (ClusterEntry cluster : allClusters) {
            long clusterId = cluster.getClusterId();
            MasterEntry masterEntry = masterService.getMasterNode(clusterId);
            if (ValidateUtils.isNull(masterEntry)) {
                continue;
            }
            TubeHttpBrokerInfoList brokerInfoList = nodeService
                    .requestBrokerStatus(masterEntry);
            if (ValidateUtils.isNull(brokerInfoList)) {
                continue;
            }
            doReloadBrokers(clusterId, masterEntry, brokerInfoList, cluster);
        }
    }

    @Async("asyncExecutor")
    public void doReloadBrokers(long clusterId, MasterEntry masterEntry,
            TubeHttpBrokerInfoList brokerInfoList, ClusterEntry clusterEntry) {
        nodeService.handleReloadBroker(masterEntry, brokerInfoList.getNeedReloadList(), clusterEntry);
        updateCreateTopicTaskStatus(clusterId);
    }

    /**
     * Modify the status of creating a topic task
     *
     * @param clusterId this is the cluster id
     */
    @Transactional(rollbackOn = Exception.class)
    public void updateCreateTopicTaskStatus(long clusterId) {
        List<TopicTaskEntry> topicTasks = topicTaskRepository
                .findTopicTaskEntriesByClusterIdAndStatus(clusterId,
                        TaskStatusEnum.ADDING.getCode());
        TopicView topicView = topicService
                .requestTopicViewInfo(clusterId, null);
        if (topicView == null || topicView.getData() == null) {
            return;
        }
        List<TopicView.TopicViewInfo> topicViews = topicView.getData();
        Map<String, TopicView.TopicViewInfo> topicViewMap = topicViews.stream()
                .collect(Collectors.toMap(TopicView.TopicViewInfo::getTopicName, topicViewInfo -> topicViewInfo));
        MasterEntry masterNode = masterService.getMasterNode(clusterId);
        TubeHttpBrokerInfoList brokerInfoList = nodeService.requestBrokerStatus(masterNode);
        if (ValidateUtils.isNull(brokerInfoList)) {
            return;
        }
        updateTaskRepo(topicTasks, topicViewMap, brokerInfoList);
    }

    private void updateTaskRepo(List<TopicTaskEntry> topicTasks,
            Map<String, TopicView.TopicViewInfo> topicViewMap,
            TubeHttpBrokerInfoList brokerInfoList) {
        int size = brokerInfoList.getAllBrokerIdList().size();
        for (TopicTaskEntry topicTask : topicTasks) {
            TopicView.TopicViewInfo topicViewInfo = topicViewMap.get(topicTask.getTopicName());
            if (topicViewInfo == null) {
                continue;
            }
            if (size == topicViewInfo.getTotalCfgBrokerCnt()
                    && topicViewInfo.getTotalCfgNumPart() == topicViewInfo.getTotalRunNumPartCount()) {
                topicTask.setStatus(TaskStatusEnum.SUCCESS.getCode());
            } else {
                Integer reloadRetryTimes = topicTask.getReloadRetryTimes();
                if (reloadRetryTimes >= MAX_RELOAD_TIMES) {
                    topicTask.setStatus(TopicStatus.FAILED.value());
                }
                topicTask.setReloadRetryTimes(reloadRetryTimes + 1);
            }
        }
        topicTaskRepository.saveAll(topicTasks);
    }

}
