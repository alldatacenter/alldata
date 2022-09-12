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
import java.util.concurrent.TimeUnit;

import org.apache.flume.Context;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.inlong.sdk.dataproxy.pb.config.pojo.GetProxyConfigBySdkRequest;
import org.apache.inlong.sdk.dataproxy.pb.config.pojo.GetProxyConfigBySdkResponse;
import org.apache.inlong.sdk.dataproxy.pb.config.pojo.GetProxyConfigByStreamResponse;
import org.apache.inlong.sdk.dataproxy.pb.config.pojo.ProxyClusterConfig;
import org.apache.inlong.sdk.dataproxy.pb.config.pojo.ProxyClusterResult;
import org.apache.inlong.sdk.dataproxy.pb.config.pojo.ProxyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

/**
 * 
 * ManagerProxyClusterConfigLoader
 */
public class ManagerProxyClusterConfigLoader implements ProxyClusterConfigLoader {

    private static Logger LOG = LoggerFactory.getLogger(ManagerProxyClusterConfigLoader.class);
    private Context context;
    private CloseableHttpClient httpClient;

    /**
     * constructHttpClient
     * 
     * @return
     */
    private static synchronized CloseableHttpClient constructHttpClient() {
        long timeoutInMs = TimeUnit.MILLISECONDS.toMillis(50000);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout((int) timeoutInMs)
                .setSocketTimeout((int) timeoutInMs).build();
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        httpClientBuilder.setDefaultRequestConfig(requestConfig);
        return httpClientBuilder.build();
    }

    /**
     * configure
     * 
     * @param context
     */
    @Override
    public void configure(Context context) {
        this.context = context;
        this.httpClient = constructHttpClient();
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
        HttpGet httpGet = null;
        try {
            String url = this.context.getString(KEY_LOADER_TYPE_MANAGER_STREAMURL)
                    + "?inlongGroupId=" + inlongGroupId
                    + "&inlongStreamId=" + inlongStreamId;
            LOG.info("start to ProxyClusterConfigLoader request {} to get config info", url);
            httpGet = new HttpGet(url);
            httpGet.addHeader(HttpHeaders.CONNECTION, "close");

            // request with get
            CloseableHttpResponse response = httpClient.execute(httpGet);
            String jsonString = EntityUtils.toString(response.getEntity());
            LOG.info("end to ProxyClusterConfigLoader request {},result:{}", url, jsonString);

            GetProxyConfigByStreamResponse configResponse = JSON.parseObject(jsonString,
                    GetProxyConfigByStreamResponse.class);
            if (!configResponse.isResult()) {
                LOG.info("Fail to get config info from url:{}, error code is {}", url, configResponse.getErrCode());
                return null;
            }

            // result
            ProxyClusterConfig config = configResponse.getData();
            ProxyClusterResult result = new ProxyClusterResult();
            result.setClusterId(config.getClusterId());
            result.setHasUpdated(true);
            result.setMd5(configResponse.getMd5());
            result.setConfig(config);
            return result;
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
            return null;
        } finally {
            if (httpGet != null) {
                httpGet.releaseConnection();
            }
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
            String url = this.context.getString(KEY_LOADER_TYPE_MANAGER_SDKURL);
            LOG.info("start to ProxyClusterConfigLoader request {} to get config info", url);
            httpPost = new HttpPost(url);
            httpPost.addHeader(HttpHeaders.CONNECTION, "close");
            httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

            // body
            GetProxyConfigBySdkRequest request = new GetProxyConfigBySdkRequest();
            request.setProxys(proxys);
            String requestJson = JSON.toJSONString(request);
            httpPost.setEntity(new StringEntity(requestJson));

            // request with post
            CloseableHttpResponse response = httpClient.execute(httpPost);
            String jsonString = EntityUtils.toString(response.getEntity());
            LOG.info("end to ProxyClusterConfigLoader request {},body:{},result:{}", url, requestJson, jsonString);

            GetProxyConfigBySdkResponse configResponse = JSON.parseObject(jsonString,
                    GetProxyConfigBySdkResponse.class);
            if (!configResponse.isResult()) {
                LOG.info("Fail to get config info from url:{}, error code is {}", url, configResponse.getErrCode());
                return null;
            }

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
