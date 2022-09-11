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
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.common.pojo.dataproxy.CacheClusterObject;
import org.apache.inlong.common.pojo.dataproxy.CacheClusterSetObject;
import org.apache.inlong.common.pojo.dataproxy.DataProxyCluster;
import org.apache.inlong.common.pojo.dataproxy.DataProxyConfigResponse;
import org.apache.inlong.common.pojo.dataproxy.IRepository;
import org.apache.inlong.common.pojo.dataproxy.InLongIdObject;
import org.apache.inlong.common.pojo.dataproxy.ProxyClusterObject;
import org.apache.inlong.common.pojo.dataproxy.RepositoryTimerTask;
import org.apache.inlong.manager.common.pojo.dataproxy.CacheCluster;
import org.apache.inlong.manager.common.pojo.dataproxy.InlongGroupId;
import org.apache.inlong.manager.common.pojo.dataproxy.InlongStreamId;
import org.apache.inlong.manager.common.pojo.dataproxy.ProxyCluster;
import org.apache.inlong.manager.dao.mapper.ClusterSetMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DataProxyConfigRepository
 */
@Repository(value = "dataProxyConfigRepository")
public class DataProxyConfigRepository implements IRepository {

    public static final Splitter.MapSplitter MAP_SPLITTER = Splitter.on(SEPARATOR).trimResults()
            .withKeyValueSeparator(KEY_VALUE_SEPARATOR);
    public static final String CACHE_CLUSTER_PRODUCER_TAG = "producer";
    public static final String CACHE_CLUSTER_CONSUMER_TAG = "consumer";
    private static final Logger LOGGER = LoggerFactory.getLogger(DataProxyConfigRepository.class);
    private static final Gson gson = new Gson();

    // key: proxyClusterName, value: jsonString
    private Map<String, String> proxyConfigJson = new ConcurrentHashMap<>();
    // key: proxyClusterName, value: md5
    private Map<String, String> proxyMd5Map = new ConcurrentHashMap<>();

    private long reloadInterval;

    @Autowired
    private ClusterSetMapper clusterSetMapper;

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
     * reload
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reload() {
        LOGGER.info("start to reload config.");
        Map<String, ProxyClusterObject> proxyClusterMap = this.reloadProxyCluster();
        if (proxyClusterMap.size() == 0) {
            return;
        }
        Map<String, Map<String, List<CacheCluster>>> cacheClusterMap = this.reloadCacheCluster();
        Map<String, List<InLongIdObject>> inlongIdMap = this.reloadInlongId();
        // mapping inlongIdMap
        for (Entry<String, ProxyClusterObject> entry : proxyClusterMap.entrySet()) {
            String clusterTag = entry.getValue().getSetName();
            List<InLongIdObject> inlongIds = inlongIdMap.get(clusterTag);
            if (inlongIds != null) {
                entry.getValue().setInlongIds(inlongIds);
            }
        }

        // generateClusterJson
        this.generateClusterJson(proxyClusterMap, cacheClusterMap);

        LOGGER.info("end to reload config.");
    }

    /**
     * reloadProxyCluster
     */
    private Map<String, ProxyClusterObject> reloadProxyCluster() {
        Map<String, ProxyClusterObject> proxyClusterMap = new HashMap<>();
        for (ProxyCluster proxyCluster : clusterSetMapper.selectProxyCluster()) {
            ProxyClusterObject obj = new ProxyClusterObject();
            obj.setName(proxyCluster.getClusterName());
            obj.setSetName(proxyCluster.getClusterTag());
            obj.setZone(proxyCluster.getExtTag());
            proxyClusterMap.put(obj.getName(), obj);
        }
        return proxyClusterMap;
    }

    /**
     * reloadCacheCluster
     */
    private Map<String, Map<String, List<CacheCluster>>> reloadCacheCluster() {
        Map<String, Map<String, List<CacheCluster>>> cacheClusterMap = new HashMap<>();
        for (CacheCluster cacheCluster : clusterSetMapper.selectCacheCluster()) {
            if (StringUtils.isEmpty(cacheCluster.getExtTag())) {
                continue;
            }
            Map<String, String> tagMap = MAP_SPLITTER.split(cacheCluster.getExtTag());
            String producerTag = tagMap.getOrDefault(CACHE_CLUSTER_PRODUCER_TAG, Boolean.TRUE.toString());
            if (StringUtils.equalsIgnoreCase(producerTag, Boolean.TRUE.toString())) {
                cacheClusterMap.computeIfAbsent(cacheCluster.getClusterTag(), k -> new HashMap<>())
                        .computeIfAbsent(cacheCluster.getExtTag(), k -> new ArrayList<>()).add(cacheCluster);
            }
        }
        return cacheClusterMap;
    }

    /**
     * reloadInlongId
     */
    @SuppressWarnings("unchecked")
    private Map<String, List<InLongIdObject>> reloadInlongId() {
        // parse group
        Map<String, InlongGroupId> groupIdMap = new HashMap<>();
        clusterSetMapper.selectInlongGroupId().forEach(value -> groupIdMap.put(value.getInlongGroupId(), value));
        // parse stream
        Map<String, List<InLongIdObject>> inlongIdMap = new HashMap<>();
        for (InlongStreamId streamIdObj : clusterSetMapper.selectInlongStreamId()) {
            String groupId = streamIdObj.getInlongGroupId();
            InlongGroupId groupIdObj = groupIdMap.get(groupId);
            if (groupId == null) {
                continue;
            }
            // choose topic
            String groupTopic = groupIdObj.getTopic();
            String streamTopic = streamIdObj.getTopic();
            String finalTopic = null;
            if (StringUtils.isEmpty(groupTopic)) {
                // both empty then ignore
                if (StringUtils.isEmpty(streamTopic)) {
                    continue;
                } else {
                    finalTopic = streamTopic;
                }
            } else {
                if (StringUtils.isEmpty(streamTopic)) {
                    finalTopic = groupTopic;
                } else {
                    // Pulsar: namespace+topic
                    finalTopic = groupTopic + "/" + streamTopic;
                }
            }
            // concat id
            InLongIdObject obj = new InLongIdObject();
            String inlongId = groupId + "." + streamIdObj.getInlongStreamId();
            obj.setInlongId(inlongId);
            obj.setTopic(finalTopic);
            Map<String, String> params = new HashMap<>();
            obj.setParams(params);
            // parse group extparams
            if (!StringUtils.isEmpty(groupIdObj.getExtParams())) {
                try {
                    Map<String, String> groupParams = gson.fromJson(groupIdObj.getExtParams(), Map.class);
                    params.putAll(groupParams);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
            // parse stream extparams
            if (!StringUtils.isEmpty(streamIdObj.getExtParams())) {
                try {
                    Map<String, String> streamParams = gson.fromJson(streamIdObj.getExtParams(), Map.class);
                    params.putAll(streamParams);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
            inlongIdMap.computeIfAbsent(groupIdObj.getClusterTag(), k -> new ArrayList<>()).add(obj);
        }
        return inlongIdMap;
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
    @SuppressWarnings("unchecked")
    private void generateClusterJson(Map<String, ProxyClusterObject> proxyClusterMap,
            Map<String, Map<String, List<CacheCluster>>> cacheClusterMap) {
        Map<String, String> newProxyConfigJson = new ConcurrentHashMap<>();
        Map<String, String> newProxyMd5Map = new ConcurrentHashMap<>();
        Map<String, Map<String, String>> tagCache = new HashMap<>();
        for (Entry<String, ProxyClusterObject> entry : proxyClusterMap.entrySet()) {
            ProxyClusterObject proxyObj = entry.getValue();
            // proxy
            DataProxyCluster clusterObj = new DataProxyCluster();
            clusterObj.setProxyCluster(proxyObj);
            // cache
            String clusterTag = proxyObj.getSetName();
            String extTag = proxyObj.getZone();
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
                        List<CacheClusterObject> cacheClusters = new ArrayList<>(cacheClusterList.size());
                        cacheSet.setCacheClusters(cacheClusters);
                        for (CacheCluster cacheCluster : cacheClusterList) {
                            CacheClusterObject obj = new CacheClusterObject();
                            obj.setName(cacheCluster.getClusterName());
                            obj.setZone(cacheCluster.getExtTag());
                            try {
                                Map<String, String> params = gson.fromJson(cacheCluster.getExtParams(), Map.class);
                                obj.setParams(params);
                            } catch (Exception e) {
                                LOGGER.error(e.getMessage(), e);
                            }
                            cacheClusters.add(obj);
                        }
                    }
                }
            }
            // json
            String jsonDataProxyCluster = gson.toJson(clusterObj);
            String md5 = DigestUtils.md5Hex(jsonDataProxyCluster);
            DataProxyConfigResponse response = new DataProxyConfigResponse();
            response.setResult(true);
            response.setErrCode(DataProxyConfigResponse.SUCC);
            response.setMd5(md5);
            response.setData(clusterObj);
            String jsonResponse = gson.toJson(response);
            newProxyConfigJson.put(proxyObj.getName(), jsonResponse);
            newProxyMd5Map.put(proxyObj.getName(), md5);
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
}
