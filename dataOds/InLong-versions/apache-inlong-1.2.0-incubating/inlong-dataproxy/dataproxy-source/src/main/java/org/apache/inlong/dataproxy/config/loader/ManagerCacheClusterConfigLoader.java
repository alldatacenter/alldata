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
import java.util.List;

import org.apache.flume.Context;
import org.apache.inlong.common.pojo.dataproxy.CacheClusterObject;
import org.apache.inlong.common.pojo.dataproxy.DataProxyCluster;
import org.apache.inlong.dataproxy.config.RemoteConfigManager;
import org.apache.inlong.dataproxy.config.pojo.CacheClusterConfig;

/**
 * 
 * ManagerCacheClusterConfigLoader
 */
public class ManagerCacheClusterConfigLoader implements CacheClusterConfigLoader {

    /**
     * load
     * 
     * @return
     */
    @Override
    public List<CacheClusterConfig> load() {
        List<CacheClusterConfig> configList = new ArrayList<>();
        DataProxyCluster dataProxyCluster = RemoteConfigManager.getInstance().getCurrentClusterConfig();
        if (dataProxyCluster == null) {
            return configList;
        }
        for (CacheClusterObject obj : dataProxyCluster.getCacheClusterSet().getCacheClusters()) {
            CacheClusterConfig config = new CacheClusterConfig();
            config.setClusterName(obj.getName());
            config.getParams().putAll(obj.getParams());
            configList.add(config);
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
    }

}
