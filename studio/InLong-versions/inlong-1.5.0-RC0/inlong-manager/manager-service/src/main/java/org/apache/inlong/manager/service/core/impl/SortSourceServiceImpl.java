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
import org.apache.inlong.common.constant.ClusterSwitch;
import org.apache.inlong.common.pojo.sdk.CacheZone;
import org.apache.inlong.common.pojo.sdk.CacheZoneConfig;
import org.apache.inlong.common.pojo.sdk.SortSourceConfigResponse;
import org.apache.inlong.common.pojo.sdk.Topic;
import org.apache.inlong.common.constant.MQType;
import org.apache.inlong.manager.common.enums.ClusterType;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.dao.entity.InlongGroupExtEntity;
import org.apache.inlong.manager.dao.entity.InlongStreamExtEntity;
import org.apache.inlong.manager.pojo.sort.standalone.SortSourceClusterInfo;
import org.apache.inlong.manager.pojo.sort.standalone.SortSourceGroupInfo;
import org.apache.inlong.manager.pojo.sort.standalone.SortSourceStreamInfo;
import org.apache.inlong.manager.pojo.sort.standalone.SortSourceStreamSinkInfo;
import org.apache.inlong.manager.service.core.SortConfigLoader;
import org.apache.inlong.manager.service.core.SortSourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of {@link SortSourceService}.
 */
@Lazy
@Service
public class SortSourceServiceImpl implements SortSourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SortSourceServiceImpl.class);

    private static final Gson GSON = new Gson();
    private static final Set<String> SUPPORTED_MQ_TYPE = new HashSet<String>() {

        {
            add(MQType.KAFKA);
            add(MQType.TUBEMQ);
            add(MQType.PULSAR);
        }
    };
    private static final String KEY_AUTH = "authentication";
    private static final String KEY_TENANT = "tenant";

    private static final int RESPONSE_CODE_SUCCESS = 0;
    private static final int RESPONSE_CODE_NO_UPDATE = 1;
    private static final int RESPONSE_CODE_FAIL = -1;
    private static final int RESPONSE_CODE_REQ_PARAMS_ERROR = -101;

    /**
     * key 1: cluster name, key 2: task name, value : md5
     */
    private Map<String, Map<String, String>> sortSourceMd5Map = new ConcurrentHashMap<>();
    /**
     * key 1: cluster name, key 2: task name, value : source config
     */
    private Map<String, Map<String, CacheZoneConfig>> sortSourceConfigMap = new ConcurrentHashMap<>();

    private Map<String, List<SortSourceClusterInfo>> mqClusters;
    private Map<String, SortSourceGroupInfo> groupInfos;
    private Map<String, Map<String, SortSourceStreamInfo>> allStreams;
    private Map<String, String> backupClusterTag;
    private Map<String, String> backupGroupMqResource;
    private Map<String, Map<String, String>> backupStreamMqResource;
    private Map<String, Map<String, List<SortSourceStreamSinkInfo>>> streamSinkMap;

    @Autowired
    private SortConfigLoader configLoader;

    @PostConstruct
    public void initialize() {
        LOGGER.info("create repository for " + SortSourceServiceImpl.class.getSimpleName());
        try {
            reload();
            setReloadTimer();
        } catch (Throwable t) {
            LOGGER.error("initialize SortSourceConfigRepository error", t);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void reload() {
        LOGGER.debug("start to reload sort config.");
        try {
            reloadAllConfigs();
            parseAll();
        } catch (Throwable t) {
            LOGGER.error("fail to reload all source config", t);
        }
        LOGGER.debug("end to reload config");
    }

    @Override
    public SortSourceConfigResponse getSourceConfig(
            String cluster,
            String task,
            String md5) {

        // if cluster or task are invalid
        if (StringUtils.isBlank(cluster) || StringUtils.isBlank(task)) {
            String errMsg = "blank cluster name or task name, return nothing";
            LOGGER.error(errMsg);
            return SortSourceConfigResponse.builder()
                    .code(RESPONSE_CODE_REQ_PARAMS_ERROR)
                    .msg(errMsg)
                    .build();
        }

        // if there is no config, but still return success
        if (!sortSourceConfigMap.containsKey(cluster) || !sortSourceConfigMap.get(cluster).containsKey(task)) {
            String errMsg = String.format("there is no valid source config of cluster %s, task %s", cluster, task);
            LOGGER.error(errMsg);
            return SortSourceConfigResponse.builder()
                    .code(RESPONSE_CODE_SUCCESS)
                    .msg(errMsg)
                    .build();
        }

        // if the same md5
        if (sortSourceMd5Map.get(cluster).get(task).equals(md5)) {
            return SortSourceConfigResponse.builder()
                    .code(RESPONSE_CODE_NO_UPDATE)
                    .msg("No update")
                    .md5(md5)
                    .build();
        }

        // if there is bad config
        if (sortSourceConfigMap.get(cluster).get(task).getCacheZones().isEmpty()) {
            String errMsg = String.format("find empty cache zones of cluster %s, task %s, "
                    + "please check the manager log", cluster, task);
            LOGGER.error(errMsg);
            return SortSourceConfigResponse.builder()
                    .code(RESPONSE_CODE_FAIL)
                    .msg(errMsg)
                    .build();
        }

        return SortSourceConfigResponse.builder()
                .code(RESPONSE_CODE_SUCCESS)
                .msg("Success")
                .data(sortSourceConfigMap.get(cluster).get(task))
                .md5(sortSourceMd5Map.get(cluster).get(task))
                .build();

    }

    private void reloadAllConfigs() {

        // reload mq cluster and sort cluster
        List<SortSourceClusterInfo> allClusters = configLoader.loadAllClusters();

        // group mq clusters by cluster tag
        mqClusters = allClusters.stream()
                .filter(cluster -> SUPPORTED_MQ_TYPE.contains(cluster.getType()))
                .filter(SortSourceClusterInfo::isConsumable)
                .collect(Collectors.groupingBy(SortSourceClusterInfo::getClusterTags));

        // reload all stream sinks, to Map<clusterName, Map<taskName, List<groupId>>> format
        List<SortSourceStreamSinkInfo> allStreamSinks = configLoader.loadAllStreamSinks();
        streamSinkMap = new HashMap<>();
        allStreamSinks.stream()
                .filter(sink -> sink.getSortClusterName() != null)
                .filter(sink -> sink.getSortTaskName() != null)
                .forEach(sink -> {
                    Map<String, List<SortSourceStreamSinkInfo>> task2groupsMap =
                            streamSinkMap.computeIfAbsent(sink.getSortClusterName(), k -> new ConcurrentHashMap<>());
                    List<SortSourceStreamSinkInfo> sinkInfoList =
                            task2groupsMap.computeIfAbsent(sink.getSortTaskName(), k -> new ArrayList<>());
                    sinkInfoList.add(sink);
                });

        // reload all groups
        groupInfos = configLoader.loadAllGroup()
                .stream()
                .collect(Collectors.toMap(SortSourceGroupInfo::getGroupId, info -> info));

        // reload all back up cluster
        backupClusterTag = configLoader.loadGroupBackupInfo(ClusterSwitch.BACKUP_CLUSTER_TAG)
                .stream()
                .collect(Collectors.toMap(InlongGroupExtEntity::getInlongGroupId, InlongGroupExtEntity::getKeyValue));

        // reload all back up group mq resource
        backupGroupMqResource = configLoader.loadGroupBackupInfo(ClusterSwitch.BACKUP_MQ_RESOURCE)
                .stream()
                .collect(Collectors.toMap(InlongGroupExtEntity::getInlongGroupId, InlongGroupExtEntity::getKeyValue));

        // reload all streams
        allStreams = configLoader.loadAllStreams()
                .stream()
                .collect(Collectors.groupingBy(SortSourceStreamInfo::getInlongGroupId,
                        Collectors.toMap(SortSourceStreamInfo::getInlongStreamId, info -> info)));

        // reload all back up stream mq resource
        backupStreamMqResource = configLoader.loadStreamBackupInfo(ClusterSwitch.BACKUP_MQ_RESOURCE)
                .stream()
                .collect(Collectors.groupingBy(InlongStreamExtEntity::getInlongGroupId,
                        Collectors.toMap(InlongStreamExtEntity::getInlongStreamId,
                                InlongStreamExtEntity::getKeyValue)));
    }

    private void parseAll() {

        // Prepare CacheZones for each cluster and task
        Map<String, Map<String, String>> newMd5Map = new ConcurrentHashMap<>();
        Map<String, Map<String, CacheZoneConfig>> newConfigMap = new ConcurrentHashMap<>();

        streamSinkMap.forEach((sortClusterName, task2SinkList) -> {
            // prepare the new config and md5
            Map<String, CacheZoneConfig> task2Config = new ConcurrentHashMap<>();
            Map<String, String> task2Md5 = new ConcurrentHashMap<>();

            task2SinkList.forEach((taskName, sinkList) -> {
                try {
                    CacheZoneConfig cacheZoneConfig =
                            CacheZoneConfig.builder()
                                    .sortClusterName(sortClusterName)
                                    .sortTaskId(taskName)
                                    .build();
                    Map<String, CacheZone> cacheZoneMap =
                            this.parseCacheZones(sinkList);
                    cacheZoneConfig.setCacheZones(cacheZoneMap);

                    // prepare md5
                    String jsonStr = GSON.toJson(cacheZoneConfig);
                    String md5 = DigestUtils.md5Hex(jsonStr);
                    task2Config.put(taskName, cacheZoneConfig);
                    task2Md5.put(taskName, md5);
                } catch (Throwable t) {
                    LOGGER.error("failed to parse sort source config of sortCluster={}, task={}",
                            sortClusterName, taskName, t);
                }
            });
            newConfigMap.put(sortClusterName, task2Config);
            newMd5Map.put(sortClusterName, task2Md5);

        });
        sortSourceConfigMap = newConfigMap;
        sortSourceMd5Map = newMd5Map;
        mqClusters = null;
        groupInfos = null;
        allStreams = null;
        backupClusterTag = null;
        backupGroupMqResource = null;
        backupStreamMqResource = null;
        streamSinkMap = null;
    }

    private Map<String, CacheZone> parseCacheZones(
            List<SortSourceStreamSinkInfo> sinkList) {

        // get group infos
        List<SortSourceStreamSinkInfo> sinkInfoList = sinkList.stream()
                .filter(sinkInfo -> groupInfos.containsKey(sinkInfo.getGroupId())
                        && allStreams.containsKey(sinkInfo.getGroupId())
                        && allStreams.get(sinkInfo.getGroupId()).containsKey(sinkInfo.getStreamId()))
                .collect(Collectors.toList());

        // group them by cluster tag.
        Map<String, List<SortSourceStreamSinkInfo>> tag2SinkInfos = sinkInfoList.stream()
                .collect(Collectors.groupingBy(sink -> {
                    SortSourceGroupInfo groupInfo = groupInfos.get(sink.getGroupId());
                    return groupInfo.getClusterTag();
                }));

        // group them by second cluster tag.
        Map<String, List<SortSourceStreamSinkInfo>> backupTag2SinkInfos = sinkInfoList.stream()
                .filter(info -> backupClusterTag.containsKey(info.getGroupId()))
                .collect(Collectors.groupingBy(info -> backupClusterTag.get(info.getGroupId())));

        List<CacheZone> cacheZones = this.parseCacheZonesByTag(tag2SinkInfos, false);
        List<CacheZone> backupCacheZones = this.parseCacheZonesByTag(backupTag2SinkInfos, true);

        return Stream.of(cacheZones, backupCacheZones)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(
                        CacheZone::getZoneName,
                        cacheZone -> cacheZone,
                        (zone1, zone2) -> {
                            zone1.getTopics().addAll(zone2.getTopics());
                            return zone1;
                        }));
    }

    private List<CacheZone> parseCacheZonesByTag(
            Map<String, List<SortSourceStreamSinkInfo>> tag2Sinks,
            boolean isBackup) {

        return tag2Sinks.keySet().stream()
                .filter(mqClusters::containsKey)
                .flatMap(tag -> {
                    List<SortSourceStreamSinkInfo> sinks = tag2Sinks.get(tag);
                    List<SortSourceClusterInfo> clusters = mqClusters.get(tag);
                    return clusters.stream()
                            .map(cluster -> {
                                CacheZone zone = null;
                                try {
                                    zone = this.parseCacheZone(sinks, cluster, isBackup);
                                } catch (IllegalStateException e) {
                                    LOGGER.error("fail to init cache zone for cluster " + cluster, e);
                                }
                                return zone;
                            });
                })
                .collect(Collectors.toList());
    }

    private CacheZone parseCacheZone(
            List<SortSourceStreamSinkInfo> sinks,
            SortSourceClusterInfo cluster,
            boolean isBackupTag) {
        switch (cluster.getType()) {
            case ClusterType.PULSAR:
                return parsePulsarZone(sinks, cluster, isBackupTag);
            default:
                throw new BusinessException(String.format("do not support cluster type=%s of cluster=%s",
                        cluster.getType(), cluster));
        }
    }

    private CacheZone parsePulsarZone(
            List<SortSourceStreamSinkInfo> sinks,
            SortSourceClusterInfo cluster,
            boolean isBackupTag) {
        Map<String, String> param = cluster.getExtParamsMap();
        String tenant = param.get(KEY_TENANT);
        String auth = param.get(KEY_AUTH);
        List<Topic> sdkTopics = sinks.stream()
                .map(sink -> {
                    String groupId = sink.getGroupId();
                    String streamId = sink.getStreamId();
                    SortSourceGroupInfo groupInfo = groupInfos.get(groupId);
                    SortSourceStreamInfo streamInfo = allStreams.get(groupId).get(streamId);
                    try {
                        String namespace = groupInfo.getMqResource();
                        String topic = streamInfo.getMqResource();
                        if (isBackupTag) {
                            if (backupGroupMqResource.containsKey(groupId)) {
                                namespace = backupGroupMqResource.get(groupId);
                            }
                            if (backupStreamMqResource.containsKey(groupId)
                                    && backupStreamMqResource.get(groupId).containsKey(streamId)) {
                                topic = backupStreamMqResource.get(groupId).get(streamId);
                            }
                        }
                        String fullTopic = tenant.concat("/").concat(namespace).concat("/").concat(topic);
                        return Topic.builder()
                                .topic(fullTopic)
                                .topicProperties(sink.getExtParamsMap())
                                .build();
                    } catch (Exception e) {
                        LOGGER.error("fail to parse topic of groupId={}, streamId={}", groupId, streamId, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return CacheZone.builder()
                .zoneName(cluster.getName())
                .serviceUrl(cluster.getUrl())
                .topics(sdkTopics)
                .authentication(auth)
                .cacheZoneProperties(cluster.getExtParamsMap())
                .zoneType(ClusterType.PULSAR)
                .build();
    }

    /**
     * Set reload timer at the beginning of repository.
     */
    private void setReloadTimer() {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        long reloadInterval = 60000L;
        executorService.scheduleAtFixedRate(this::reload, reloadInterval, reloadInterval, TimeUnit.MILLISECONDS);
    }
}
