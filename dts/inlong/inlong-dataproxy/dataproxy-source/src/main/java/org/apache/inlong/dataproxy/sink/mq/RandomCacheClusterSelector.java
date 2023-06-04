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

import org.apache.flume.Context;
import org.apache.flume.conf.Configurable;
import org.apache.inlong.dataproxy.config.pojo.CacheClusterConfig;
import org.apache.pulsar.shade.org.apache.commons.lang.math.NumberUtils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * RandomCacheClusterSelector
 */
public class RandomCacheClusterSelector implements CacheClusterSelector, Configurable {

    public static final String KEY_CACHE_CLUSTER_PRODUCER_SIZE = "cacheClusterProducerSize";
    public static final int DEFAULT_CACHE_CLUSTER_PRODUCER_SIZE = 1;
    public static final String KEY_CACHE_CLUSTER_RESELECT_INTERVAL_MINUTES = "cacheClusterReselectIntervalMinutes";
    public static final int DEFAULT_CACHE_CLUSTER_RESELECT_INTERVAL_MINUTES = 10;

    private Context context;
    private long lastCacheClusterSelectTime = 0;
    private HashSet<String> currentClusterNames = new HashSet<>();

    /**
     * select
     * @param allClusterList
     * @return
     */
    @Override
    public List<CacheClusterConfig> select(List<CacheClusterConfig> allConfigList) {
        List<CacheClusterConfig> newConfigList = new ArrayList<>();
        HashSet<String> newClusterNames = new HashSet<>();
        allConfigList.forEach(v -> newClusterNames.add(v.getClusterName()));
        int cacheClusterReselectIntervalMinutes = NumberUtils.toInt(
                context.getString(KEY_CACHE_CLUSTER_RESELECT_INTERVAL_MINUTES),
                DEFAULT_CACHE_CLUSTER_RESELECT_INTERVAL_MINUTES);
        long cacheClusterReselectIntervalMs = cacheClusterReselectIntervalMinutes * 60 * 1000;
        boolean needReselectCacheCluster = (System.currentTimeMillis()
                - lastCacheClusterSelectTime) > cacheClusterReselectIntervalMs;
        if (newClusterNames.equals(currentClusterNames)
                && !needReselectCacheCluster) {
            return newConfigList;
        }
        this.lastCacheClusterSelectTime = System.currentTimeMillis();
        this.currentClusterNames = newClusterNames;

        // update cluster list
        SecureRandom rand = new SecureRandom();
        int cacheClusterProducerSize = NumberUtils.toInt(
                context.getString(KEY_CACHE_CLUSTER_PRODUCER_SIZE),
                DEFAULT_CACHE_CLUSTER_PRODUCER_SIZE);
        int maxClusterSize = Math.min(cacheClusterProducerSize, allConfigList.size());
        List<CacheClusterConfig> randomList = new ArrayList<>(allConfigList);
        for (int i = 0; i < maxClusterSize; i++) {
            // random select
            int index = rand.nextInt(randomList.size());
            CacheClusterConfig config = randomList.remove(index);
            newConfigList.add(config);
        }
        return newConfigList;
    }

    @Override
    public void configure(Context context) {
        this.context = context;
    }

}
