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

package org.apache.inlong.sdk.dataproxy.pb.config;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.apache.flume.Context;
import org.apache.inlong.sdk.dataproxy.pb.config.pojo.GetProxyConfigBySdkResponse;
import org.apache.inlong.sdk.dataproxy.pb.config.pojo.InlongStreamConfig;
import org.apache.inlong.sdk.dataproxy.pb.config.pojo.ProxyClusterResult;
import org.apache.inlong.sdk.dataproxy.pb.config.pojo.ProxyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Data Proxy cluster config loader
 */
public class ContextProxyClusterConfigLoader implements ProxyClusterConfigLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ContextProxyClusterConfigLoader.class);
    private Context context;

    /**
     * configure the context
     */
    @Override
    public void configure(Context context) {
        this.context = context;
    }

    /**
     * Load data proxy cluster info by inlong group and inlong stream
     */
    @Override
    public ProxyClusterResult loadByStream(String inlongGroupId, String inlongStreamId) {
        try {
            String jsonString = this.context.getString(KEY_LOADER_TYPE_CONTEXT_KEY);
            LOG.info("get data proxy cluster from inlong group and stream result: {}", jsonString);

            GetProxyConfigBySdkResponse configResponse = JSON.parseObject(jsonString,
                    GetProxyConfigBySdkResponse.class);
            // result
            for (Entry<String, ProxyClusterResult> entry : configResponse.getData().entrySet()) {
                for (InlongStreamConfig stream : entry.getValue().getConfig().getInlongStreamList()) {
                    if (StringUtils.equals(inlongGroupId, stream.getInlongGroupId())
                            && StringUtils.equals(inlongStreamId, stream.getInlongStreamId())) {
                        return entry.getValue();
                    }
                }
            }
            return null;
        } catch (Exception e) {
            LOG.error("get data proxy cluster from inlong group and stream failed: ", e);
            return null;
        }
    }

    /**
     * Load data proxy cluster info by cluster IDs
     */
    @Override
    public Map<String, ProxyClusterResult> loadByClusterIds(List<ProxyInfo> proxyInfos) {
        try {
            String jsonString = this.context.getString(KEY_LOADER_TYPE_CONTEXT_KEY);
            LOG.info("get data proxy cluster from cluster result: {}", jsonString);

            GetProxyConfigBySdkResponse configResponse = JSON.parseObject(jsonString,
                    GetProxyConfigBySdkResponse.class);
            return configResponse.getData();
        } catch (Exception e) {
            LOG.error("get data proxy cluster from cluster failed: ", e);
            return null;
        }
    }
}
