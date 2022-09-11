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

package org.apache.inlong.sdk.dataproxy.pb.config;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.flume.Context;
import org.apache.http.client.methods.HttpPost;
import org.apache.inlong.sdk.dataproxy.pb.config.pojo.GetProxyConfigBySdkResponse;
import org.apache.inlong.sdk.dataproxy.pb.config.pojo.InlongStreamConfig;
import org.apache.inlong.sdk.dataproxy.pb.config.pojo.ProxyClusterResult;
import org.apache.inlong.sdk.dataproxy.pb.config.pojo.ProxyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

/**
 * 
 * ContextProxyClusterConfigLoader
 */
public class ContextProxyClusterConfigLoader implements ProxyClusterConfigLoader {

    private static Logger LOG = LoggerFactory.getLogger(ContextProxyClusterConfigLoader.class);
    private Context context;

    /**
     * configure
     * 
     * @param context
     */
    @Override
    public void configure(Context context) {
        this.context = context;
    }

    /**
     * loadByStream
     * 
     * @param  inlongGroupId
     * @param  inlongStreamId
     * @return
     */
    @Override
    public ProxyClusterResult loadByStream(String inlongGroupId, String inlongStreamId) {
        try {
            String jsonString = this.context.getString(KEY_LOADER_TYPE_CONTEXT_KEY);
            LOG.info("Get ProxyClusterConfigLoader result:{}", jsonString);

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
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * loadByClusterIds
     * 
     * @param  proxys
     * @return
     */
    @Override
    public Map<String, ProxyClusterResult> loadByClusterIds(List<ProxyInfo> proxys) {
        HttpPost httpPost = null;
        try {
            String jsonString = this.context.getString(KEY_LOADER_TYPE_CONTEXT_KEY);
            LOG.info("Get ProxyClusterConfigLoader result:{}", jsonString);

            GetProxyConfigBySdkResponse configResponse = JSON.parseObject(jsonString,
                    GetProxyConfigBySdkResponse.class);

            // result
            return configResponse.getData();
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
            return null;
        } finally {
            if (httpPost != null) {
                httpPost.releaseConnection();
            }
        }
    }
}
