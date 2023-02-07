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

package org.apache.inlong.sdk.sort.impl;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.inlong.common.pojo.sdk.CacheZone;
import org.apache.inlong.common.pojo.sdk.CacheZoneConfig;
import org.apache.inlong.common.pojo.sdk.SortSourceConfigResponse;
import org.apache.inlong.common.pojo.sdk.Topic;
import org.apache.inlong.sdk.sort.api.ClientContext;
import org.apache.inlong.sdk.sort.api.QueryConsumeConfig;
import org.apache.inlong.sdk.sort.entity.CacheZoneCluster;
import org.apache.inlong.sdk.sort.entity.ConsumeConfig;
import org.apache.inlong.sdk.sort.entity.InLongTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryConsumeConfigImpl implements QueryConsumeConfig {

    private static final int NOUPDATE_VALUE = 1;
    private static final int UPDATE_VALUE = 0;
    private static final int REQ_PARAMS_ERROR = -101;
    private final Logger logger = LoggerFactory.getLogger(QueryConsumeConfigImpl.class);
    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    private ClientContext clientContext;
    private String md5 = "";

    private Map<String, List<InLongTopic>> subscribedTopic = new HashMap<>();

    public QueryConsumeConfigImpl(ClientContext clientContext) {
        this.clientContext = clientContext;
    }

    public QueryConsumeConfigImpl() {

    }

    private String getRequestUrlWithParam() {
        return clientContext.getConfig().getManagerApiUrl() + "?clusterName=" + clientContext.getConfig()
                .getSortClusterName() + "&sortTaskId=" + clientContext.getConfig().getSortTaskId() + "&md5=" + md5
                + "&apiVersion=" + clientContext.getConfig().getManagerApiVersion();
    }

    // HTTP GET
    private SortSourceConfigResponse doGetRequest(String getUrl) throws Exception {
        SortSourceConfigResponse managerResponse;
        HttpGet request = getHttpGet(getUrl);

        try (CloseableHttpResponse response = httpClient.execute(request)) {

            logger.debug("response status:{}", response.getStatusLine().toString());

            HttpEntity entity = response.getEntity();
            Header headers = entity.getContentType();
            logger.debug("response headers:{}", headers);

            String result = EntityUtils.toString(entity);
            logger.debug("response String result:{}", result);
            try {
                managerResponse = new ObjectMapper().readValue(result, SortSourceConfigResponse.class);
                return managerResponse;
            } catch (Exception e) {
                logger.error("parse json to ManagerResponse error:{}", e.getMessage(), e);
                e.printStackTrace();
            }

        }
        return null;
    }

    private HttpGet getHttpGet(String getUrl) {
        HttpGet request = new HttpGet(getUrl);
        // add request headers
        request.addHeader("custom-key", "inlong-readapi");
        request.addHeader(HttpHeaders.USER_AGENT, "Googlebot");
        return request;
    }

    /**
     * get new sortTask conf from inlong manager
     */
    public void reload() {
        logger.debug("start to reload sort task config.");
        try {
            String getUrl = getRequestUrlWithParam();
            SortSourceConfigResponse managerResponse = doGetRequest(getUrl);
            if (managerResponse == null) {
                logger.info("## reload managerResponse == null");
                return;
            }
            if (handleSortTaskConfResult(getUrl, managerResponse, managerResponse.getCode())) {
                return;
            }
        } catch (Throwable e) {
            String msg = MessageFormat
                    .format("Fail to reload atta configuration in {0} error:{1}.", getRequestUrlWithParam(),
                            e.getMessage());
            logger.error(msg, e);
        }
    }

    /**
     * handle request response
     *
     * UPDATE_VALUE = 0; conf update NOUPDATE_VALUE = 1; conf no update, md5 is same REQ_PARAMS_ERROR = -101; request
     * params error FAIL = -1; common error
     *
     * @param  getUrl
     * @param  response      ManagerResponse
     * @param  respCodeValue int
     * @return               true/false
     */
    private boolean handleSortTaskConfResult(String getUrl, SortSourceConfigResponse response, int respCodeValue)
            throws Exception {
        switch (respCodeValue) {
            case NOUPDATE_VALUE:
                logger.debug("manager conf noupdate");
                return true;
            case UPDATE_VALUE:
                logger.info("manager conf update");
                clientContext.addRequestManagerConfChange();
                this.md5 = response.getMd5();
                updateSortTaskConf(response);
                break;
            case REQ_PARAMS_ERROR:
                logger.error("return code error:{}", respCodeValue);
                clientContext.addRequestManagerParamError();
                break;
            default:
                logger.error("return code error:{},request:{},response:{}",
                        respCodeValue, getUrl, new ObjectMapper().writeValueAsString(response));
                clientContext.addRequestManagerCommonError();
                return true;
        }
        return false;
    }

    private void updateSortTaskConf(SortSourceConfigResponse response) {
        CacheZoneConfig cacheZoneConfig = response.getData();
        Map<String, List<InLongTopic>> newGroupTopicsMap = new HashMap<>();
        for (Map.Entry<String, CacheZone> entry : cacheZoneConfig.getCacheZones().entrySet()) {
            CacheZone cacheZone = entry.getValue();

            List<InLongTopic> topics = newGroupTopicsMap.computeIfAbsent(cacheZoneConfig.getSortTaskId(),
                    k -> new ArrayList<>());
            CacheZoneCluster cacheZoneCluster = new CacheZoneCluster(cacheZone.getZoneName(),
                    cacheZone.getServiceUrl(), cacheZone.getAuthentication());
            for (Topic topicInfo : cacheZone.getTopics()) {
                InLongTopic topic = new InLongTopic();
                topic.setInLongCluster(cacheZoneCluster);
                topic.setTopic(topicInfo.getTopic());
                topic.setTopicType(cacheZone.getZoneType());
                topic.setProperties(topicInfo.getTopicProperties());
                topics.add(topic);
            }
        }

        this.subscribedTopic = newGroupTopicsMap;
    }

    /**
     * query ConsumeConfig
     *
     * @param  sortTaskId String
     * @return            ConsumeConfig
     */
    @Override
    public ConsumeConfig queryCurrentConsumeConfig(String sortTaskId) {
        reload();
        return new ConsumeConfig(subscribedTopic.get(sortTaskId));
    }

    @Override
    public void configure(ClientContext context) {
        this.clientContext = context;
    }
}
