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

package org.apache.inlong.dataproxy.sink.mq;

import org.apache.inlong.dataproxy.config.ConfigManager;
import org.apache.inlong.dataproxy.config.pojo.CacheClusterConfig;
import org.apache.inlong.dataproxy.consts.StatConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * MessageQueueZoneProducer
 */
public class MessageQueueZoneProducer {

    private static final Logger logger = LoggerFactory.getLogger(MessageQueueZoneProducer.class);
    private static final long MAX_RESERVED_TIME = 60 * 1000L;
    private final MessageQueueZoneSink zoneSink;
    private final MessageQueueZoneSinkContext context;
    private final CacheClusterSelector cacheClusterSelector;

    private final AtomicInteger clusterIndex = new AtomicInteger(0);
    private List<String> currentClusterNames = new ArrayList<>();
    private final ConcurrentHashMap<String, Long> usingTimeMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, MessageQueueClusterProducer> usingClusterMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, MessageQueueClusterProducer> deletingClusterMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> deletingTimeMap = new ConcurrentHashMap<>();
    private final Set<String> lastRefreshTopics = new HashSet<>();

    /**
     * Constructor
     * 
     * @param zoneSink
     * @param context
     */
    public MessageQueueZoneProducer(MessageQueueZoneSink zoneSink, MessageQueueZoneSinkContext context) {
        this.zoneSink = zoneSink;
        this.context = context;
        this.cacheClusterSelector = context.createCacheClusterSelector();
    }

    /**
     * start
     */
    public void start() {
        try {
            logger.info("{} start MessageQueueZoneProducer", zoneSink.getName());
            this.reloadMetaConfig();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * close
     */
    public void close() {
        for (MessageQueueClusterProducer clusterProducer : this.deletingClusterMap.values()) {
            if (clusterProducer == null) {
                continue;
            }
            clusterProducer.stop();
        }
        for (MessageQueueClusterProducer clusterProducer : this.usingClusterMap.values()) {
            if (clusterProducer == null) {
                continue;
            }
            clusterProducer.stop();
        }
        this.deletingClusterMap.clear();
        this.deletingTimeMap.clear();
        this.usingClusterMap.clear();
        this.usingTimeMap.clear();
    }

    /**
     * reload
     */
    public void reloadMetaConfig() {
        checkAndReloadClusterInfo();
        checkAndPublishTopics();
    }

    /**
     * clear expired producers
     */
    public void clearExpiredProducers() {
        if (deletingClusterMap.isEmpty()) {
            return;
        }
        Set<String> expired = new HashSet<>();
        synchronized (deletingClusterMap) {
            long curTime = System.currentTimeMillis();
            for (Map.Entry<String, Long> entry : deletingTimeMap.entrySet()) {
                if (entry == null
                        || entry.getKey() == null
                        || entry.getValue() == null
                        || curTime - entry.getValue() < MAX_RESERVED_TIME) {
                    continue;
                }
                expired.add(entry.getKey());
            }
            if (expired.isEmpty()) {
                return;
            }
            MessageQueueClusterProducer tmpProducer;
            for (String clusterName : expired) {
                deletingTimeMap.remove(clusterName);
                tmpProducer = deletingClusterMap.remove(clusterName);
                if (tmpProducer == null) {
                    continue;
                }
                tmpProducer.stop();
            }
        }
        logger.info("{} cleared expired cluster producer {}", zoneSink.getName(), expired);
    }

    /**
     * send
     * 
     * @param profile
     */
    public boolean send(PackProfile profile) {
        String clusterName;
        List<String> tmpClusters;
        MessageQueueClusterProducer clusterProducer;
        do {
            tmpClusters = currentClusterNames;
            if (tmpClusters == null || tmpClusters.isEmpty()) {
                context.fileMetricIncSumStats(StatConstants.EVENT_SINK_CLUSTER_EMPTY);
                sleepSomeTime(100);
                continue;
            }
            clusterName = tmpClusters.get(Math.abs(clusterIndex.getAndIncrement()) % tmpClusters.size());
            if (clusterName == null) {
                context.fileMetricIncSumStats(StatConstants.EVENT_SINK_CLUSTER_UNMATCHED);
                sleepSomeTime(100);
                continue;
            }
            clusterProducer = usingClusterMap.get(clusterName);
            if (clusterProducer == null) {
                context.fileMetricIncWithDetailStats(StatConstants.EVENT_SINK_CPRODUCER_NULL, clusterName);
                sleepSomeTime(100);
                continue;
            }
            return clusterProducer.send(profile);
        } while (true);
    }

    private void checkAndReloadClusterInfo() {
        try {
            // get new cluster list
            List<CacheClusterConfig> allConfigList = ConfigManager.getInstance().getCachedCLusterConfig();
            List<CacheClusterConfig> newConfigList = this.cacheClusterSelector.select(allConfigList);
            if (newConfigList == null || newConfigList.size() == 0) {
                return;
            }
            // check added clusters
            boolean changed = false;
            MessageQueueClusterProducer tmpProducer;
            List<String> lastClusterNames = new ArrayList<>();
            List<CacheClusterConfig> addedItems = new ArrayList<>();
            synchronized (deletingClusterMap) {
                // filter added records
                for (CacheClusterConfig clusterConfig : newConfigList) {
                    if (clusterConfig == null) {
                        continue;
                    }
                    if (usingTimeMap.containsKey(clusterConfig.getClusterName())) {
                        lastClusterNames.add(clusterConfig.getClusterName());
                        continue;
                    }
                    if (deletingTimeMap.containsKey(clusterConfig.getClusterName())) {
                        deletingTimeMap.remove(clusterConfig.getClusterName());
                        tmpProducer = deletingClusterMap.remove(clusterConfig.getClusterName());
                        if (tmpProducer == null) {
                            addedItems.add(clusterConfig);
                        } else {
                            usingClusterMap.put(clusterConfig.getClusterName(), tmpProducer);
                            usingTimeMap.put(clusterConfig.getClusterName(), System.currentTimeMillis());
                            lastClusterNames.add(clusterConfig.getClusterName());
                        }
                        continue;
                    }
                    addedItems.add(clusterConfig);
                }
            }
            if (!addedItems.isEmpty()) {
                changed = true;
                MessageQueueClusterProducer tmpCluster;
                long curTime = System.currentTimeMillis();
                for (CacheClusterConfig config : addedItems) {
                    if (config == null) {
                        continue;
                    }
                    // create
                    tmpCluster = new MessageQueueClusterProducer(zoneSink.getName(), config, context);
                    tmpCluster.start();
                    usingClusterMap.put(config.getClusterName(), tmpCluster);
                    usingTimeMap.put(config.getClusterName(), curTime);
                    lastClusterNames.add(config.getClusterName());
                }
            }
            // replace cluster names
            if (!lastClusterNames.equals(currentClusterNames)) {
                currentClusterNames = lastClusterNames;
                changed = true;
            }
            // filter removed records
            Set<String> needRmvs = new HashSet<>();
            synchronized (deletingClusterMap) {
                for (Map.Entry<String, MessageQueueClusterProducer> entry : usingClusterMap.entrySet()) {
                    if (entry == null
                            || entry.getKey() == null
                            || entry.getValue() == null
                            || lastClusterNames.contains(entry.getKey())) {
                        continue;
                    }
                    needRmvs.add(entry.getKey());
                }
                if (!needRmvs.isEmpty()) {
                    changed = true;
                    long curTime = System.currentTimeMillis();
                    for (String clusterName : needRmvs) {
                        tmpProducer = usingClusterMap.remove(clusterName);
                        usingTimeMap.remove(clusterName);
                        if (tmpProducer == null) {
                            continue;
                        }
                        deletingClusterMap.put(clusterName, tmpProducer);
                        deletingTimeMap.put(clusterName, curTime);
                    }
                }
            }
            if (!changed) {
                return;
            }
            if (zoneSink.isMqClusterStarted()) {
                logger.info("{} reload cluster info, current cluster are {}, removed {}, created {}",
                        zoneSink.getName(), lastClusterNames, needRmvs, addedItems);
            } else {
                zoneSink.setMQClusterStarted();
                ConfigManager.getInstance().setMqClusterReady();
                logger.info(
                        "{} reload cluster info, and updated sink status, current cluster are {}, removed {}, created {}",
                        zoneSink.getName(), lastClusterNames, needRmvs, addedItems);
            }
        } catch (Throwable e) {
            logger.error("{} reload cluster info failure", zoneSink.getName(), e);
        }
    }

    private void checkAndPublishTopics() {
        Set<String> curTopicSet = ConfigManager.getInstance().getAllTopicNames();
        if (curTopicSet.isEmpty() || lastRefreshTopics.equals(curTopicSet)) {
            return;
        }
        logger.info("{} reload topics changed, current topics are {}, last topics are {}",
                zoneSink.getName(), curTopicSet, lastRefreshTopics);
        lastRefreshTopics.addAll(curTopicSet);
        for (MessageQueueClusterProducer clusterProducer : this.usingClusterMap.values()) {
            if (clusterProducer == null) {
                continue;
            }
            clusterProducer.publishTopic(curTopicSet);
        }
    }

    private void sleepSomeTime(long millis) {
        try {
            Thread.sleep(millis);
        } catch (Throwable e) {
            //
        }
    }
}
