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

package org.apache.inlong.manager.service.core.impl;

import com.google.gson.Gson;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.common.pojo.sortstandalone.SortClusterConfig;
import org.apache.inlong.common.pojo.sortstandalone.SortClusterResponse;
import org.apache.inlong.common.pojo.sortstandalone.SortTaskConfig;
import org.apache.inlong.manager.dao.entity.DataNodeEntity;
import org.apache.inlong.manager.dao.entity.StreamSinkEntity;
import org.apache.inlong.manager.pojo.node.DataNodeInfo;
import org.apache.inlong.manager.pojo.sort.standalone.SortFieldInfo;
import org.apache.inlong.manager.pojo.sort.standalone.SortTaskInfo;
import org.apache.inlong.manager.service.core.SortClusterService;
import org.apache.inlong.manager.service.core.SortConfigLoader;
import org.apache.inlong.manager.service.node.DataNodeOperator;
import org.apache.inlong.manager.service.node.DataNodeOperatorFactory;
import org.apache.inlong.manager.service.sink.SinkOperatorFactory;
import org.apache.inlong.manager.service.sink.StreamSinkOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Used to cache the sort cluster config and reduce the number of query to database.
 */
@Lazy
@Service
public class SortClusterServiceImpl implements SortClusterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SortClusterServiceImpl.class);

    private static final Gson GSON = new Gson();

    private static final long DEFAULT_HEARTBEAT_INTERVAL_MS = 60000;

    private static final int RESPONSE_CODE_SUCCESS = 0;
    private static final int RESPONSE_CODE_NO_UPDATE = 1;
    private static final int RESPONSE_CODE_FAIL = -1;
    private static final int RESPONSE_CODE_REQ_PARAMS_ERROR = -101;

    private static final String KEY_GROUP_ID = "inlongGroupId";
    private static final String KEY_STREAM_ID = "inlongStreamId";
    private Map<String, List<String>> fieldMap;

    // key : sort cluster name, value : md5
    private Map<String, String> sortClusterMd5Map = new ConcurrentHashMap<>();
    // key : sort cluster name, value : cluster config
    private Map<String, SortClusterConfig> sortClusterConfigMap = new ConcurrentHashMap<>();
    // key : sort cluster name, value : error log
    private Map<String, String> sortClusterErrorLogMap = new ConcurrentHashMap<>();

    private long reloadInterval;

    @Autowired
    private SortConfigLoader sortConfigLoader;
    @Autowired
    private SinkOperatorFactory sinkOperatorFactory;
    @Autowired
    private DataNodeOperatorFactory dataNodeOperatorFactory;

    @PostConstruct
    public void initialize() {
        LOGGER.info("create repository for " + SortClusterServiceImpl.class.getSimpleName());
        try {
            this.reloadInterval = DEFAULT_HEARTBEAT_INTERVAL_MS;
            reload();
            setReloadTimer();
        } catch (Throwable t) {
            LOGGER.error("Initialize SortClusterConfigRepository error", t);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void reload() {
        LOGGER.debug("start to reload sort config");
        try {
            reloadAllClusterConfig();
        } catch (Throwable t) {
            LOGGER.error("fail to reload cluster config", t);
        }
        LOGGER.debug("end to reload config");
    }

    @Override
    public SortClusterResponse getClusterConfig(String clusterName, String md5) {
        // check if cluster name is valid or not.
        if (StringUtils.isBlank(clusterName)) {
            String errMsg = "Blank cluster name, return nothing";
            LOGGER.debug(errMsg);
            return SortClusterResponse.builder()
                    .msg(errMsg)
                    .code(RESPONSE_CODE_REQ_PARAMS_ERROR)
                    .build();
        }

        // if there is an error
        if (sortClusterErrorLogMap.get(clusterName) != null) {
            return SortClusterResponse.builder()
                    .msg(sortClusterErrorLogMap.get(clusterName))
                    .code(RESPONSE_CODE_FAIL)
                    .build();
        }

        // there is no config, but still return success.
        if (sortClusterConfigMap.get(clusterName) == null) {
            String errMsg = "There is not config for cluster " + clusterName;
            LOGGER.debug(errMsg);
            return SortClusterResponse.builder()
                    .msg(errMsg)
                    .code(RESPONSE_CODE_SUCCESS)
                    .build();
        }

        // if the same md5
        if (sortClusterMd5Map.get(clusterName).equals(md5)) {
            return SortClusterResponse.builder()
                    .msg("No update")
                    .code(RESPONSE_CODE_NO_UPDATE)
                    .md5(md5)
                    .build();
        }

        return SortClusterResponse.builder()
                .msg("Success")
                .code(RESPONSE_CODE_SUCCESS)
                .data(sortClusterConfigMap.get(clusterName))
                .md5(sortClusterMd5Map.get(clusterName))
                .build();
    }

    private void reloadAllClusterConfig() {
        // load all fields info
        List<SortFieldInfo> fieldInfos = sortConfigLoader.loadAllFields();
        fieldMap = new HashMap<>();
        fieldInfos.forEach(info -> {
            List<String> fields = fieldMap.computeIfAbsent(info.getInlongGroupId(), k -> new ArrayList<>());
            fields.add(info.getFieldName());
        });

        List<StreamSinkEntity> sinkEntities = sortConfigLoader.loadAllStreamSinkEntity();
        // get all task under a given cluster, has been reduced into cluster and task.
        List<SortTaskInfo> tasks = sortConfigLoader.loadAllTask();
        Map<String, List<SortTaskInfo>> clusterTaskMap = tasks.stream()
                .filter(dto -> StringUtils.isNotBlank(dto.getSortClusterName())
                        && StringUtils.isNotBlank(dto.getSortTaskName())
                        && StringUtils.isNotBlank(dto.getDataNodeName())
                        && StringUtils.isNotBlank(dto.getSinkType()))
                .collect(Collectors.groupingBy(SortTaskInfo::getSortClusterName));

        // get all stream sinks
        Map<String, List<StreamSinkEntity>> task2AllStreams = sinkEntities.stream()
                .filter(entity -> StringUtils.isNotBlank(entity.getInlongClusterName()))
                .collect(Collectors.groupingBy(StreamSinkEntity::getSortTaskName));

        // get all data nodes and group by node name
        List<DataNodeEntity> dataNodeEntities = sortConfigLoader.loadAllDataNodeEntity();
        Map<String, DataNodeInfo> task2DataNodeMap = dataNodeEntities.stream()
                .filter(entity -> StringUtils.isNotBlank(entity.getName()))
                .map(entity -> {
                    DataNodeOperator operator = dataNodeOperatorFactory.getInstance(entity.getType());
                    return operator.getFromEntity(entity);
                })
                .collect(Collectors.toMap(DataNodeInfo::getName, info -> info));

        // re-org all SortClusterConfigs
        Map<String, SortClusterConfig> newConfigMap = new ConcurrentHashMap<>();
        Map<String, String> newMd5Map = new ConcurrentHashMap<>();
        Map<String, String> newErrorLogMap = new ConcurrentHashMap<>();

        clusterTaskMap.forEach((clusterName, taskList) -> {
            try {
                SortClusterConfig config = this.getConfigByClusterName(clusterName,
                        taskList, task2AllStreams, task2DataNodeMap);
                String jsonStr = GSON.toJson(config);
                String md5 = DigestUtils.md5Hex(jsonStr);
                newConfigMap.put(clusterName, config);
                newMd5Map.put(clusterName, md5);
            } catch (Throwable e) {
                // if get config failed, update the err log.
                String errMsg = Optional.ofNullable(e.getMessage()).orElse("Unknown error, please check logs");
                newErrorLogMap.put(clusterName, errMsg);
                LOGGER.error("Failed to update cluster config={}", clusterName, e);
            }
        });

        sortClusterErrorLogMap = newErrorLogMap;
        sortClusterConfigMap = newConfigMap;
        sortClusterMd5Map = newMd5Map;
    }

    private SortClusterConfig getConfigByClusterName(
            String clusterName,
            List<SortTaskInfo> tasks,
            Map<String, List<StreamSinkEntity>> task2AllStreams,
            Map<String, DataNodeInfo> task2DataNodeMap) {

        List<SortTaskConfig> taskConfigs = tasks.stream()
                .map(task -> {
                    try {
                        String taskName = task.getSortTaskName();
                        String type = task.getSinkType();
                        String dataNodeName = task.getDataNodeName();
                        DataNodeInfo nodeInfo = task2DataNodeMap.get(dataNodeName);
                        List<StreamSinkEntity> streams = task2AllStreams.get(taskName);
                        return SortTaskConfig.builder()
                                .name(taskName)
                                .type(type)
                                .idParams(this.parseIdParams(streams))
                                .sinkParams(this.parseSinkParams(nodeInfo))
                                .build();
                    } catch (Exception e) {
                        LOGGER.error("fail to parse sort task config of cluster={}", clusterName, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return SortClusterConfig.builder()
                .clusterName(clusterName)
                .sortTasks(taskConfigs)
                .build();
    }

    private List<Map<String, String>> parseIdParams(List<StreamSinkEntity> streams) {
        return streams.stream()
                .map(streamSink -> {
                    try {
                        StreamSinkOperator operator = sinkOperatorFactory.getInstance(streamSink.getSinkType());
                        List<String> fields = fieldMap.get(streamSink.getInlongGroupId());
                        return operator.parse2IdParams(streamSink, fields);
                    } catch (Exception e) {
                        LOGGER.error("fail to parse id params of groupId={}, streamId={} name={}, type={}}",
                                streamSink.getInlongGroupId(), streamSink.getInlongStreamId(),
                                streamSink.getSinkName(), streamSink.getSinkType(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Map<String, String> parseSinkParams(DataNodeInfo nodeInfo) {
        DataNodeOperator operator = dataNodeOperatorFactory.getInstance(nodeInfo.getType());
        return operator.parse2SinkParams(nodeInfo);
    }

    /**
     * Set reload timer at the beginning of repository.
     */
    private void setReloadTimer() {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(this::reload, reloadInterval, reloadInterval, TimeUnit.MILLISECONDS);
    }
}
