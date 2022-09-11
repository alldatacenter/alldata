/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.dataproxy.config.loader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.flume.Context;
import org.apache.inlong.dataproxy.config.pojo.CacheClusterConfig;
import org.apache.pulsar.shade.org.apache.commons.lang3.StringUtils;

/**
 * 
 * ContextCacheClusterConfigLoader
 */
public class ContextCacheClusterConfigLoader implements CacheClusterConfigLoader {

    private Context context;

    /**
     * load
     * 
     * @return
     */
    @Override
    public List<CacheClusterConfig> load() {
        String clusterNames = context.getString(CACHE_CLUSTER_CONFIG);
        if (StringUtils.isBlank(clusterNames)) {
            return new ArrayList<>();
        }
        //
        String[] clusterNameArray = StringUtils.split(clusterNames);
        Set<String> clusterNameSet = new HashSet<>();
        clusterNameSet.addAll(Arrays.asList(clusterNameArray));
        //
        List<CacheClusterConfig> configList = new ArrayList<>(clusterNameSet.size());
        for (String clusterName : clusterNameSet) {
            CacheClusterConfig config = new CacheClusterConfig();
            configList.add(config);
            config.setClusterName(clusterName);
            Map<String, String> params = context.getSubProperties("cacheClusterConfig." + clusterName + ".");
            config.setParams(params);
        }
        return configList;
    }

    /**
     * configure
     * 
     * @param context
     */
    @Override
    public void configure(Context context) {
        this.context = context;
    }

}
