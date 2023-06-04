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

package org.apache.inlong.manager.service.repository;

import com.google.common.base.Splitter;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.common.constant.ClusterSwitch;
import org.apache.inlong.common.pojo.dataproxy.CacheClusterObject;
import org.apache.inlong.common.pojo.dataproxy.CacheClusterSetObject;
import org.apache.inlong.common.pojo.dataproxy.DataProxyCluster;
import org.apache.inlong.common.pojo.dataproxy.DataProxyConfigResponse;
import org.apache.inlong.common.pojo.dataproxy.IRepository;
import org.apache.inlong.common.pojo.dataproxy.InLongIdObject;
import org.apache.inlong.common.pojo.dataproxy.ProxyClusterObject;
import org.apache.inlong.common.pojo.dataproxy.RepositoryTimerTask;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.dao.entity.InlongClusterEntity;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.dao.entity.InlongGroupExtEntity;
import org.apache.inlong.manager.dao.entity.InlongStreamExtEntity;
import org.apache.inlong.manager.dao.entity.StreamSinkEntity;
import org.apache.inlong.manager.dao.mapper.ClusterSetMapper;
import org.apache.inlong.manager.dao.mapper.InlongClusterEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongGroupEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamSinkEntityMapper;
import org.apache.inlong.manager.pojo.cluster.ClusterPageRequest;
import org.apache.inlong.manager.pojo.dataproxy.CacheCluster;
import org.apache.inlong.manager.pojo.dataproxy.InlongGroupId;
import org.apache.inlong.manager.pojo.dataproxy.InlongStreamId;
import org.apache.inlong.manager.pojo.dataproxy.ProxyCluster;
import org.apache.inlong.manager.pojo.sink.SinkPageRequest;
import org.apache.inlong.manager.service.core.SortConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DataProxyConfigRepository
 */
@Lazy
@Repository(value = "dataProxyConfigRepository")
public class DataProxyConfigRepository implements IRepository {

    public static final Logger LOGGER = LoggerFactory.getLogger(DataProxyConfigRepository.class);

    public static final String KEY_NAMESPACE = "namespace";
    public static final String KEY_BACKUP_CLUSTER_TAG = "backup_cluster_tag";
    public static final String KEY_BACKUP_TOPIC = "backup_topic";
    public static final String KEY_SORT_TASK_NAME = "defaultSortTaskName";
    public static final String KEY_DATA_NODE_NAME = "defaultDataNodeName";
    public static final String KEY_SORT_CONSUMER_GROUP = "defaultSortConsumerGroup";
    public static final String KEY_SINK_NAME = "defaultSinkName";

    public static final Splitter.MapSplitter MAP_SPLITTER = Splitter.on(SEPARATOR).trimResults()
            .withKeyValueSeparator(KEY_VALUE_SEPARATOR);
    public static final String CACHE_CLUSTER_PRODUCER_TAG = "producer";
    public static final String CACHE_CLUSTER_CONSUMER_TAG = "consumer";
    private static final Gson GSON = new Gson();

    // key: proxyClusterName, value: jsonString
    private Map<String, String> proxyConfigJson = new ConcurrentHashMap<>();
    // key: proxyClusterName, value: md5
    private Map<String, String> proxyMd5Map = new ConcurrentHashMap<>();

    private long reloadInterval;

    @Autowired
    private ClusterSetMapper clusterSetMapper;
    @Autowired
    private InlongClusterEntityMapper clusterMapper;
    @Autowired
    private InlongGroupEntityMapper inlongGroupMapper;
    @Autowired
    private StreamSinkEntityMapper streamSinkMapper;
    @Autowired
    private SortConfigLoader sortConfigLoader;

    @PostConstruct
    public void initialize() {
        LOGGER.info("create repository for " + DataProxyConfigRepository.class.getSimpleName());
        try {
            this.reloadInterval = DEFAULT_HEARTBEAT_INTERVAL_MS;
            reload();
            setReloadTimer();
        } catch (Throwable t) {
            LOGGER.error("Initialize DataProxyConfigRepository error", t);
        }
    }

    /**
     * get clusterSetMapper
     *
     * @return the clusterSetMapper
     */
    public ClusterSetMapper getClusterSetMapper() {
        return clusterSetMapper;
    }

    /**
     * set clusterSetMapper
     *
     * @param clusterSetMapper the clusterSetMapper to set
     */
    public void setClusterSetMapper(ClusterSetMapper clusterSetMapper) {
        this.clusterSetMapper = clusterSetMapper;
    }

    /**
     * get clusterMapper
     *
     * @return the clusterMapper
     */
    public InlongClusterEntityMapper getClusterMapper() {
        return clusterMapper;
    }

    /**
     * set clusterMapper
     *
     * @param clusterMapper the clusterMapper to set
     */
    public void setClusterMapper(InlongClusterEntityMapper clusterMapper) {
        this.clusterMapper = clusterMapper;
    }

    /**
     * get inlongGroupMapper
     *
     * @return the inlongGroupMapper
     */
    public InlongGroupEntityMapper getInlongGroupMapper() {
        return inlongGroupMapper;
    }

    /**
     * set inlongGroupMapper
     *
     * @param inlongGroupMapper the inlongGroupMapper to set
     */
    public void setInlongGroupMapper(InlongGroupEntityMapper inlongGroupMapper) {
        this.inlongGroupMapper = inlongGroupMapper;
    }

    /**
     * get streamSinkMapper
     *
     * @return the streamSinkMapper
     */
    public StreamSinkEntityMapper getStreamSinkMapper() {
        return streamSinkMapper;
    }

    /**
     * set streamSinkMapper
     *
     * @param streamSinkMapper the streamSinkMapper to set
     */
    public void setStreamSinkMapper(StreamSinkEntityMapper streamSinkMapper) {
        this.streamSinkMapper = streamSinkMapper;
    }

    /**
     * reload
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reload() {
        LOGGER.info("start to reload config:" + this.getClass().getSimpleName());
        // reload proxy cluster
        Map<String, DataProxyCluster> proxyClusterMap = new HashMap<>();
        this.reloadProxyCluster(proxyClusterMap);
        if (proxyClusterMap.size() == 0) {
            return;
        }
        // reload cache cluster
        this.reloadCacheCluster(proxyClusterMap);
        // reload inlong group id and inlong stream id
        this.reloadInlongId(proxyClusterMap);

        // generateClusterJson
        this.generateClusterJson(proxyClusterMap);

        LOGGER.info("end to reload config:" + this.getClass().getSimpleName());
    }

    /**
     * reloadProxyCluster
     */
    private void reloadProxyCluster(Map<String, DataProxyCluster> proxyClusterMap) {
        for (ProxyCluster proxyCluster : clusterSetMapper.selectProxyCluster()) {
            ProxyClusterObject obj = new ProxyClusterObject();
            obj.setName(proxyCluster.getClusterName());
            obj.setSetName(proxyCluster.getClusterTag());
            obj.setZone(proxyCluster.getExtTag());
            DataProxyCluster clusterObj = new DataProxyCluster();
            clusterObj.setProxyCluster(obj);
            proxyClusterMap.put(obj.getName(), clusterObj);
        }
    }

    /**
     * reloadCacheCluster
     */
    private void reloadCacheCluster(Map<String, DataProxyCluster> proxyClusterMap) {
        // reload cache cluster
        Map<String, Map<String, List<CacheCluster>>> cacheClusterMap = new HashMap<>();
        for (CacheCluster cacheCluster : clusterSetMapper.selectCacheCluster()) {
            if (StringUtils.isEmpty(cacheCluster.getExtTag())) {
                continue;
            }
            Map<String, String> tagMap = MAP_SPLITTER.split(cacheCluster.getExtTag());
            String producerTag = tagMap.getOrDefault(CACHE_CLUSTER_PRODUCER_TAG, Boolean.TRUE.toString());
            if (StringUtils.equalsIgnoreCase(producerTag, Boolean.TRUE.toString())) {
                cacheClusterMap.computeIfAbsent(cacheCluster.getClusterTags(), k -> new HashMap<>())
                        .computeIfAbsent(cacheCluster.getExtTag(), k -> new ArrayList<>()).add(cacheCluster);
            }
        }
        // mark cache cluster to proxy cluster
        Map<String, Map<String, String>> tagCache = new HashMap<>();
        for (Entry<String, DataProxyCluster> entry : proxyClusterMap.entrySet()) {
            DataProxyCluster clusterObj = entry.getValue();
            ProxyClusterObject proxyObj = clusterObj.getProxyCluster();
            // cache
            String clusterTag = proxyObj.getSetName();
            String extTag = proxyObj.getZone();
            if (StringUtils.isEmpty(extTag)) {
                continue;
            }
            Map<String, List<CacheCluster>> cacheClusterZoneMap = cacheClusterMap.get(clusterTag);
            if (cacheClusterZoneMap != null) {
                Map<String, String> subTagMap = tagCache.computeIfAbsent(extTag, k -> MAP_SPLITTER.split(extTag));
                for (Entry<String, List<CacheCluster>> cacheEntry : cacheClusterZoneMap.entrySet()) {
                    if (cacheEntry.getValue().size() == 0) {
                        continue;
                    }
                    Map<String, String> wholeTagMap = tagCache.computeIfAbsent(cacheEntry.getKey(),
                            k -> MAP_SPLITTER.split(cacheEntry.getKey()));
                    if (isSubTag(wholeTagMap, subTagMap)) {
                        CacheClusterSetObject cacheSet = clusterObj.getCacheClusterSet();
                        cacheSet.setSetName(clusterTag);
                        List<CacheCluster> cacheClusterList = cacheEntry.getValue();
                        cacheSet.setType(cacheClusterList.get(0).getType());
                        List<CacheClusterObject> cacheClusters = cacheSet.getCacheClusters();
                        for (CacheCluster cacheCluster : cacheClusterList) {
                            CacheClusterObject obj = new CacheClusterObject();
                            obj.setName(cacheCluster.getClusterName());
                            obj.setZone(cacheCluster.getExtTag());
                            obj.setToken(cacheCluster.getToken());
                            obj.setParams(fromJsonToMap(cacheCluster.getExtParams()));
                            cacheClusters.add(obj);
                        }
                    }
                }
            }
        }
    }

    /**
     * Parse Json string to Java Map
     */
    private Map<String, String> fromJsonToMap(String jsonString) {
        Map<String, String> mapObj = new HashMap<>();
        if (StringUtils.isBlank(jsonString)) {
            return mapObj;
        }
        try {
            JsonObject obj = GSON.fromJson(jsonString, JsonObject.class);
            for (String key : obj.keySet()) {
                JsonElement child = obj.get(key);
                if (child.isJsonPrimitive()) {
                    mapObj.put(key, child.getAsString());
                } else {
                    mapObj.put(key, child.toString());
                }
            }
        } catch (Exception e) {
            LOGGER.error("parse json string to map error", e);
        }
        return mapObj;
    }

    /**
     * Parse Json string to JsonObject
     */
    private JsonObject fromJsonToJson(String jsonString) {
        if (StringUtils.isBlank(jsonString)) {
            return new JsonObject();
        }
        try {
            return GSON.fromJson(jsonString, JsonObject.class);
        } catch (Exception e) {
            LOGGER.error("parse json string to json object error", e);
            return new JsonObject();
        }
    }

    /**
     * reloadInlongId
     */
    private void reloadInlongId(Map<String, DataProxyCluster> proxyClusterMap) {
        // reload inlong group id
        Map<String, InlongGroupId> groupIdMap = new HashMap<>();
        clusterSetMapper.selectInlongGroupId().forEach(value -> groupIdMap.put(value.getInlongGroupId(), value));
        // reload inlong group ext params
        Map<String, Map<String, String>> groupParams = new HashMap<>();
        groupIdMap.forEach((k, v) -> groupParams.put(k, fromJsonToMap(v.getExtParams())));
        // reload inlong group ext
        List<InlongGroupExtEntity> groupExtCursor = sortConfigLoader
                .loadGroupBackupInfo(ClusterSwitch.BACKUP_CLUSTER_TAG);
        groupExtCursor.forEach(v -> groupParams.computeIfAbsent(v.getInlongGroupId(), k -> new HashMap<>())
                .put(ClusterSwitch.BACKUP_CLUSTER_TAG, v.getKeyValue()));
        // reload inlong stream id
        Map<String, InlongStreamId> streamIdMap = new HashMap<>();
        clusterSetMapper.selectInlongStreamId()
                .forEach(v -> streamIdMap.put(getInlongId(v.getInlongGroupId(), v.getInlongStreamId()), v));
        // reload inlong stream ext params
        Map<String, Map<String, String>> streamParams = new HashMap<>();
        streamIdMap.forEach((k, v) -> streamParams.put(k, fromJsonToMap(v.getExtParams())));
        // reload inlong stream ext
        List<InlongStreamExtEntity> streamExtCursor = sortConfigLoader
                .loadStreamBackupInfo(ClusterSwitch.BACKUP_MQ_RESOURCE);
        streamExtCursor.forEach(v -> streamParams
                .computeIfAbsent(getInlongId(v.getInlongGroupId(), v.getInlongStreamId()), k -> new HashMap<>())
                .put(ClusterSwitch.BACKUP_MQ_RESOURCE, v.getKeyValue()));

        // build Map<clusterTag, List<InlongIdObject>>
        Map<String, List<InLongIdObject>> inlongIdMap = this.parseInlongId(groupIdMap, groupParams, streamIdMap,
                streamParams);
        // mark inlong id to proxy cluster
        for (Entry<String, DataProxyCluster> entry : proxyClusterMap.entrySet()) {
            String clusterTag = entry.getValue().getProxyCluster().getSetName();
            List<InLongIdObject> inlongIds = inlongIdMap.get(clusterTag);
            if (inlongIds != null) {
                entry.getValue().getProxyCluster().getInlongIds().addAll(inlongIds);
            }
        }
    }

    /**
     * parseInlongId
     */
    private Map<String, List<InLongIdObject>> parseInlongId(Map<String, InlongGroupId> groupIdMap,
            Map<String, Map<String, String>> groupParams, Map<String, InlongStreamId> streamIdMap,
            Map<String, Map<String, String>> streamParams) {
        Map<String, List<InLongIdObject>> inlongIdMap = new HashMap<>();
        for (Entry<String, InlongStreamId> entry : streamIdMap.entrySet()) {
            InlongStreamId streamIdObj = entry.getValue();
            String groupId = streamIdObj.getInlongGroupId();
            InlongGroupId groupIdObj = groupIdMap.get(groupId);
            if (groupId == null || groupIdObj == null) {
                LOGGER.debug("groupId {} or groupIdObj {} is null, ignored", groupId, groupIdObj);
                continue;
            }
            // master
            InLongIdObject obj = new InLongIdObject();
            String inlongId = entry.getKey();
            obj.setInlongId(inlongId);
            Optional.ofNullable(groupParams.get(groupId)).ifPresent(v -> obj.getParams().putAll(v));
            Optional.ofNullable(streamParams.get(inlongId)).ifPresent(v -> obj.getParams().putAll(v));
            if (StringUtils.isBlank(streamIdObj.getTopic())) {
                obj.setTopic(groupIdObj.getTopic());
            } else {
                obj.setTopic(streamIdObj.getTopic());
                obj.getParams().put(KEY_NAMESPACE, groupIdObj.getTopic());
            }
            inlongIdMap.computeIfAbsent(groupIdObj.getClusterTag(), k -> new ArrayList<>()).add(obj);
            // backup
            InLongIdObject backupObj = new InLongIdObject();
            backupObj.setInlongId(inlongId);
            backupObj.getParams().putAll(obj.getParams());
            Map<String, String> groupParam = groupParams.get(groupId);
            if (groupParam != null && groupParam.containsKey(ClusterSwitch.BACKUP_CLUSTER_TAG)
                    && groupParam.containsKey(ClusterSwitch.BACKUP_MQ_RESOURCE)) {
                String clusterTag = groupParam.get(ClusterSwitch.BACKUP_CLUSTER_TAG);
                String groupMqResource = groupParam.get(ClusterSwitch.BACKUP_MQ_RESOURCE);

                Map<String, String> streamParam = streamParams.get(inlongId);
                if (streamParam != null && !StringUtils.isBlank(streamParam.get(ClusterSwitch.BACKUP_MQ_RESOURCE))) {
                    backupObj.setTopic(streamParam.get(ClusterSwitch.BACKUP_MQ_RESOURCE));
                    backupObj.getParams().put(KEY_NAMESPACE, groupMqResource);
                } else {
                    backupObj.setTopic(groupMqResource);
                }
                inlongIdMap.computeIfAbsent(clusterTag, k -> new ArrayList<>()).add(backupObj);
            }
        }
        return inlongIdMap;
    }

    /**
     * getInlongId
     */
    private String getInlongId(String inlongGroupId, String inlongStreamId) {
        return inlongGroupId + "." + inlongStreamId;
    }

    /**
     * setReloadTimer
     */
    private void setReloadTimer() {
        Timer reloadTimer = new Timer(true);
        TimerTask task = new RepositoryTimerTask<DataProxyConfigRepository>(this);
        reloadTimer.scheduleAtFixedRate(task, reloadInterval, reloadInterval);
    }

    /**
     * generateClusterJson
     */
    private void generateClusterJson(Map<String, DataProxyCluster> proxyClusterMap) {
        Map<String, String> newProxyConfigJson = new ConcurrentHashMap<>();
        Map<String, String> newProxyMd5Map = new ConcurrentHashMap<>();
        for (Entry<String, DataProxyCluster> entry : proxyClusterMap.entrySet()) {
            DataProxyCluster proxyObj = entry.getValue();
            // json
            String jsonDataProxyCluster = GSON.toJson(proxyObj);
            String md5 = DigestUtils.md5Hex(jsonDataProxyCluster);
            DataProxyConfigResponse response = new DataProxyConfigResponse();
            response.setResult(true);
            response.setErrCode(DataProxyConfigResponse.SUCC);
            response.setMd5(md5);
            response.setData(proxyObj);
            String jsonResponse = GSON.toJson(response);
            newProxyConfigJson.put(entry.getKey(), jsonResponse);
            newProxyMd5Map.put(entry.getKey(), md5);
        }

        // replace
        this.proxyConfigJson = newProxyConfigJson;
        this.proxyMd5Map = newProxyMd5Map;
    }

    /**
     * isSubTag
     */
    private boolean isSubTag(Map<String, String> wholeTagMap, Map<String, String> subTagMap) {
        for (Entry<String, String> entry : subTagMap.entrySet()) {
            String value = wholeTagMap.get(entry.getKey());
            if (value == null || !value.equals(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    /**
     * getProxyMd5
     */
    public String getProxyMd5(String clusterName) {
        return this.proxyMd5Map.get(clusterName);
    }

    /**
     * getProxyConfigJson
     */
    public String getProxyConfigJson(String clusterName) {
        return this.proxyConfigJson.get(clusterName);
    }

    /**
     * changeClusterTag
     */
    public String changeClusterTag(String inlongGroupId, String clusterTag,
            String topic) {
        try {
            // select
            InlongGroupEntity oldGroup = inlongGroupMapper.selectByGroupId(inlongGroupId);
            if (oldGroup == null) {
                throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND);
            }
            String oldClusterTag = oldGroup.getInlongClusterTag();
            if (StringUtils.equals(oldClusterTag, clusterTag)) {
                return "Cluster tag is same.";
            }
            // prepare group
            final InlongGroupEntity newGroup = this.prepareClusterTagGroup(oldGroup, clusterTag, topic);
            // load cluster
            Map<String, InlongClusterEntity> clusterMap = new HashMap<>();
            ClusterPageRequest clusterRequest = new ClusterPageRequest();
            List<InlongClusterEntity> clusters = clusterMapper.selectByCondition(clusterRequest);
            clusters.forEach((v) -> {
                clusterMap.put(v.getName(), v);
            });
            // prepare stream sink
            SinkPageRequest request = new SinkPageRequest();
            request.setInlongGroupId(inlongGroupId);
            List<StreamSinkEntity> streamSinks = streamSinkMapper.selectByCondition(request);
            List<StreamSinkEntity> newStreamSinks = new ArrayList<>();
            for (StreamSinkEntity streamSink : streamSinks) {
                String clusterName = streamSink.getInlongClusterName();
                InlongClusterEntity cluster = clusterMap.get(clusterName);
                if (cluster == null) {
                    continue;
                }
                if (!StringUtils.equals(oldClusterTag, cluster.getClusterTags())) {
                    continue;
                }
                String clusterType = cluster.getType();
                // find the cluster of same cluster tag and sink type, and add new stream sink
                StreamSinkEntity newStreamSink = this.createNewStreamSink(clusters, clusterType, clusterTag,
                        streamSink);
                if (newStreamSink != null) {
                    newStreamSinks.add(newStreamSink);
                }
            }
            // update
            newStreamSinks.forEach((v) -> {
                streamSinkMapper.insert(v);
            });
            int rowCount = inlongGroupMapper.updateByIdentifierSelective(newGroup);
            if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
                LOGGER.error("inlong group has already updated with group id={}, curVersion={}",
                        newGroup.getInlongGroupId(), newGroup.getVersion());
                throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
            }
            return inlongGroupId;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return e.getMessage();
        }
    }

    /**
     * createNewStreamSink
     */
    private StreamSinkEntity createNewStreamSink(List<InlongClusterEntity> clusters, String clusterType,
            String clusterTag, StreamSinkEntity srcStreamSink) {
        for (InlongClusterEntity v : clusters) {
            if (StringUtils.equals(clusterType, v.getType())
                    && StringUtils.equals(clusterTag, v.getClusterTags())) {
                String newExtParams = v.getExtParams();
                JsonObject extParams = fromJsonToJson(newExtParams);
                if (extParams.has(KEY_SINK_NAME) && extParams.has(KEY_SORT_TASK_NAME)
                        && extParams.has(KEY_DATA_NODE_NAME) && extParams.has(KEY_SORT_CONSUMER_GROUP)) {
                    final String sinkName = extParams.get(KEY_SINK_NAME).getAsString();
                    final String sortTaskName = extParams.get(KEY_SORT_TASK_NAME).getAsString();
                    final String dataNodeName = extParams.get(KEY_DATA_NODE_NAME).getAsString();
                    final String sortConsumerGroup = extParams.get(KEY_SORT_CONSUMER_GROUP).getAsString();
                    StreamSinkEntity newStreamSink = copyStreamSink(srcStreamSink);
                    newStreamSink.setInlongClusterName(v.getName());
                    newStreamSink.setSinkName(sinkName);
                    newStreamSink.setSortTaskName(sortTaskName);
                    newStreamSink.setDataNodeName(dataNodeName);
                    newStreamSink.setSortConsumerGroup(sortConsumerGroup);
                    return newStreamSink;
                }
                return null;
            }
        }
        return null;
    }

    /**
     * copyStreamSink
     */
    private StreamSinkEntity copyStreamSink(StreamSinkEntity streamSink) {
        StreamSinkEntity streamSinkDest = new StreamSinkEntity();
        CommonBeanUtils.copyProperties(streamSink, streamSinkDest);
        streamSinkDest.setId(null);
        streamSinkDest.setModifyTime(new Date());
        return streamSinkDest;
    }

    /**
     * prepareClusterTagGroup
     */
    private InlongGroupEntity prepareClusterTagGroup(InlongGroupEntity oldGroup, String clusterTag, String topic)
            throws IllegalAccessException, InvocationTargetException {
        // parse ext_params
        String extParams = oldGroup.getExtParams();
        if (StringUtils.isEmpty(extParams)) {
            extParams = "{}";
        }
        // parse json
        JsonObject extParamsObj = fromJsonToJson(extParams);
        // change cluster tag
        extParamsObj.addProperty(KEY_BACKUP_CLUSTER_TAG, oldGroup.getInlongClusterTag());
        extParamsObj.addProperty(KEY_BACKUP_TOPIC, oldGroup.getMqResource());
        // copy properties
        InlongGroupEntity newGroup = new InlongGroupEntity();
        BeanUtils.copyProperties(newGroup, oldGroup);
        newGroup.setId(null);
        // change properties
        newGroup.setInlongClusterTag(clusterTag);
        newGroup.setMqResource(topic);
        String newExtParams = extParamsObj.toString();
        newGroup.setExtParams(newExtParams);
        return newGroup;
    }

    /**
     * removeBackupClusterTag
     */
    public String removeBackupClusterTag(String inlongGroupId) {
        // select
        InlongGroupEntity oldGroup = inlongGroupMapper.selectByGroupId(inlongGroupId);
        if (oldGroup == null) {
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND);
        }
        // parse ext_params
        String extParams = oldGroup.getExtParams();
        if (StringUtils.isEmpty(extParams)) {
            return inlongGroupId;
        }
        // parse json
        JsonObject extParamsObj = fromJsonToJson(extParams);
        if (!extParamsObj.has(KEY_BACKUP_CLUSTER_TAG)) {
            return inlongGroupId;
        }
        final String oldClusterTag = extParamsObj.get(KEY_BACKUP_CLUSTER_TAG).getAsString();
        extParamsObj.remove(KEY_BACKUP_CLUSTER_TAG);
        extParamsObj.remove(KEY_BACKUP_TOPIC);
        String newExtParams = extParamsObj.toString();
        oldGroup.setExtParams(newExtParams);
        // update group
        int rowCount = inlongGroupMapper.updateByIdentifierSelective(oldGroup);
        if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
            LOGGER.error("inlong group has already updated with group id={}, curVersion={}",
                    oldGroup.getInlongGroupId(), oldGroup.getVersion());
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }
        // load cluster
        Map<String, InlongClusterEntity> clusterMap = new HashMap<>();
        ClusterPageRequest clusterRequest = new ClusterPageRequest();
        List<InlongClusterEntity> clusters = clusterMapper.selectByCondition(clusterRequest);
        clusters.forEach((v) -> {
            clusterMap.put(v.getName(), v);
        });
        // prepare stream sink
        SinkPageRequest request = new SinkPageRequest();
        request.setInlongGroupId(inlongGroupId);
        List<StreamSinkEntity> streamSinks = streamSinkMapper.selectByCondition(request);
        List<StreamSinkEntity> deleteStreamSinks = new ArrayList<>();
        for (StreamSinkEntity streamSink : streamSinks) {
            String clusterName = streamSink.getInlongClusterName();
            InlongClusterEntity cluster = clusterMap.get(clusterName);
            if (cluster == null) {
                continue;
            }
            if (StringUtils.equals(oldClusterTag, cluster.getClusterTags())) {
                deleteStreamSinks.add(streamSink);
            }
        }
        // delete old stream sink
        deleteStreamSinks.forEach((v) -> {
            streamSinkMapper.deleteById(v.getId());
        });
        return inlongGroupId;
    }
}
