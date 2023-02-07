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

package org.apache.inlong.sort.standalone.config.loader;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.StringUtils;
import org.apache.flume.Context;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.inlong.common.pojo.sortstandalone.SortClusterConfig;
import org.apache.inlong.common.pojo.sortstandalone.SortClusterResponse;
import org.apache.inlong.sort.standalone.config.holder.CommonPropertiesHolder;
import org.apache.inlong.sort.standalone.config.holder.ManagerUrlHandler;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

/**
 * 
 * ManagerSortClusterConfigLoader
 */
public class ManagerSortClusterConfigLoader implements SortClusterConfigLoader {

    public static final Logger LOG = InlongLoggerFactory.getLogger(ClassResourceSortClusterConfigLoader.class);

    private Context context;
    private CloseableHttpClient httpClient;
    private ObjectMapper objectMapper = new ObjectMapper();
    private String md5;

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
     * load
     * 
     * @return
     */
    @Override
    public SortClusterConfig load() {
        HttpGet httpGet = null;
        try {
            String clusterName = this.context.getString(CommonPropertiesHolder.KEY_CLUSTER_ID);
            String url = ManagerUrlHandler.getSortClusterConfigUrl() + "?apiVersion=1.0&clusterName="
                    + clusterName + "&md5=";
            if (StringUtils.isNotBlank(this.md5)) {
                url += this.md5;
            }
            LOG.info("start to request {} to get config info", url);
            httpGet = new HttpGet(url);
            httpGet.addHeader(HttpHeaders.CONNECTION, "close");

            // request with get
            CloseableHttpResponse response = httpClient.execute(httpGet);
            String returnStr = EntityUtils.toString(response.getEntity());
            LOG.info("end to request {},result:{}", url, returnStr);
            // get groupId <-> topic and m value.

            SortClusterResponse clusterResponse = objectMapper.readValue(returnStr, SortClusterResponse.class);
            int errCode = clusterResponse.getCode();
            if (errCode != SortClusterResponse.SUCC && errCode != SortClusterResponse.NOUPDATE) {
                LOG.info("Fail to get config info from url:{}, error code is {}, msg is {}",
                        url, clusterResponse.getCode(), clusterResponse.getMsg());
                return null;
            }

            this.md5 = clusterResponse.getMd5();
            return clusterResponse.getData();
        } catch (Exception ex) {
            LOG.error("exception caught", ex);
            return null;
        } finally {
            if (httpGet != null) {
                httpGet.releaseConnection();
            }
        }
    }
}
