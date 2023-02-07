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

package org.apache.inlong.dataproxy.heartbeat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.inlong.common.enums.ComponentTypeEnum;
import org.apache.inlong.common.enums.NodeSrvStatus;
import org.apache.inlong.common.heartbeat.AbstractHeartbeatManager;
import org.apache.inlong.common.heartbeat.GroupHeartbeat;
import org.apache.inlong.common.heartbeat.HeartbeatMsg;
import org.apache.inlong.common.heartbeat.StreamHeartbeat;
import org.apache.inlong.dataproxy.config.AuthUtils;
import org.apache.inlong.dataproxy.config.ConfigManager;
import org.apache.inlong.dataproxy.config.holder.SourceReportInfo;
import org.apache.inlong.dataproxy.consts.ConfigConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Heartbeat management logic.
 */
@Slf4j
public class HeartbeatManager implements AbstractHeartbeatManager {

    public static final String DEFAULT_CLUSTER_TAG = "default_cluster";
    public static final String DEFAULT_CLUSTER_NAME = "default_dataproxy";
    public static final String DEFAULT_CLUSTER_INCHARGES = "admin";
    // predefined format of ext tag: {key}={value}
    public static final String DEFAULT_CLUSTER_EXT_TAG = "default=true";

    private final CloseableHttpClient httpClient;
    private final Gson gson;

    public HeartbeatManager() {
        httpClient = constructHttpClient();
        gson = new GsonBuilder().create();
    }

    public void start() {
        Thread reportHeartbeatThread = new Thread(() -> {
            while (true) {
                reportHeartbeat(buildHeartbeat());
                try {
                    SECONDS.sleep(heartbeatInterval());
                } catch (InterruptedException e) {
                    log.error("interrupted while report heartbeat", e);
                }
            }
        });
        reportHeartbeatThread.setDaemon(true);
        reportHeartbeatThread.start();
    }

    @Override
    public void reportHeartbeat(HeartbeatMsg heartbeat) {
        if (null == heartbeat) {
            return;
        }
        ConfigManager configManager = ConfigManager.getInstance();
        final String managerHost = configManager.getCommonProperties().get(ConfigConstants.MANAGER_HOST);
        final String url =
                "http://" + managerHost + ConfigConstants.MANAGER_PATH + ConfigConstants.MANAGER_HEARTBEAT_REPORT;
        try {
            HttpPost post = new HttpPost(url);
            post.addHeader(HttpHeaders.CONNECTION, "close");
            post.addHeader(HttpHeaders.AUTHORIZATION, AuthUtils.genBasicAuth());
            String body = gson.toJson(heartbeat);
            StringEntity stringEntity = new StringEntity(body);
            stringEntity.setContentType("application/json");
            post.setEntity(stringEntity);
            CloseableHttpResponse response = httpClient.execute(post);
            String isSuccess = EntityUtils.toString(response.getEntity());
            if (StringUtils.isNotEmpty(isSuccess)
                    && response.getStatusLine().getStatusCode() == 200) {
                if (log.isDebugEnabled()) {
                    log.debug("reportHeartbeat url {}, heartbeat: {}, return str {}", url, body, isSuccess);
                }
            }
        } catch (Exception ex) {
            log.error("reportHeartbeat failed for url {}", url, ex);
        }
    }

    private synchronized CloseableHttpClient constructHttpClient() {
        long timeoutInMs = TimeUnit.MILLISECONDS.toMillis(50000);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout((int) timeoutInMs)
                .setSocketTimeout((int) timeoutInMs).build();
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        httpClientBuilder.setDefaultRequestConfig(requestConfig);
        return httpClientBuilder.build();
    }

    private HeartbeatMsg buildHeartbeat() {
        ConfigManager configManager = ConfigManager.getInstance();
        HeartbeatMsg heartbeatMsg = new HeartbeatMsg();
        SourceReportInfo reportInfo = configManager.getSourceReportInfo();
        if (!validReportInfo(reportInfo)) {
            return null;
        }
        heartbeatMsg.setNodeSrvStatus(ConfigManager.getInstance().isMqClusterReady()
                ? NodeSrvStatus.OK
                : NodeSrvStatus.SERVICE_UNREADY);
        heartbeatMsg.setIp(reportInfo.getIp());
        heartbeatMsg.setPort(reportInfo.getPort());
        heartbeatMsg.setProtocolType(reportInfo.getProtocolType());
        heartbeatMsg.setComponentType(ComponentTypeEnum.DataProxy.getType());
        heartbeatMsg.setReportTime(System.currentTimeMillis());
        heartbeatMsg.setLoad(0xffff);
        Map<String, String> commonProperties = configManager.getCommonProperties();
        heartbeatMsg.setClusterTag(commonProperties.getOrDefault(
                ConfigConstants.PROXY_CLUSTER_TAG, DEFAULT_CLUSTER_TAG));
        heartbeatMsg.setClusterName(commonProperties.getOrDefault(
                ConfigConstants.PROXY_CLUSTER_NAME, DEFAULT_CLUSTER_NAME));
        heartbeatMsg.setInCharges(commonProperties.getOrDefault(
                ConfigConstants.PROXY_CLUSTER_INCHARGES, DEFAULT_CLUSTER_INCHARGES));
        heartbeatMsg.setExtTag(commonProperties.getOrDefault(
                ConfigConstants.PROXY_CLUSTER_EXT_TAG, DEFAULT_CLUSTER_EXT_TAG));

        Map<String, String> groupIdMappings = configManager.getGroupIdMappingProperties();
        Map<String, Map<String, String>> streamIdMappings = configManager.getStreamIdMappingProperties();
        Map<String, String> groupIdEnableMappings = configManager.getGroupIdEnableMappingProperties();
        List<GroupHeartbeat> groupHeartbeats = new ArrayList<>();
        for (Entry<String, String> entry : groupIdMappings.entrySet()) {
            String groupIdNum = entry.getKey();
            String groupId = entry.getValue();
            GroupHeartbeat groupHeartbeat = new GroupHeartbeat();
            groupHeartbeat.setInlongGroupId(groupId);
            String status = groupIdEnableMappings.getOrDefault(groupIdNum, "disabled");
            status = status.equals("TRUE") ? "enabled" : "disabled";
            groupHeartbeat.setStatus(status);
            groupHeartbeats.add(groupHeartbeat);
        }
        heartbeatMsg.setGroupHeartbeats(groupHeartbeats);

        List<StreamHeartbeat> streamHeartbeats = new ArrayList<>();
        for (Entry<String, Map<String, String>> entry : streamIdMappings.entrySet()) {
            String groupIdNum = entry.getKey();
            String status = groupIdEnableMappings.getOrDefault(groupIdNum, "disabled");
            status = status.equals("TRUE") ? "enabled" : "disabled";
            String groupId = groupIdMappings.get(groupIdNum);
            for (Entry<String, String> streamEntry : entry.getValue().entrySet()) {
                String streamId = streamEntry.getValue();
                StreamHeartbeat streamHeartbeat = new StreamHeartbeat();
                streamHeartbeat.setInlongGroupId(groupId);
                streamHeartbeat.setInlongStreamId(streamId);
                streamHeartbeat.setStatus(status);
                streamHeartbeats.add(streamHeartbeat);
            }
        }
        heartbeatMsg.setStreamHeartbeats(streamHeartbeats);
        return heartbeatMsg;
    }

    private boolean validReportInfo(SourceReportInfo reportInfo) {
        return StringUtils.isNotBlank(reportInfo.getIp()) && StringUtils.isNotBlank(reportInfo.getPort());
    }
}
