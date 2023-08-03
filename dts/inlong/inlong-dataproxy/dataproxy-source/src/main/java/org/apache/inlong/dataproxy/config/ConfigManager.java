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

package org.apache.inlong.dataproxy.config;

import org.apache.inlong.common.pojo.dataproxy.DataProxyCluster;
import org.apache.inlong.common.pojo.dataproxy.DataProxyConfigRequest;
import org.apache.inlong.common.pojo.dataproxy.DataProxyConfigResponse;
import org.apache.inlong.dataproxy.config.holder.BlackListConfigHolder;
import org.apache.inlong.dataproxy.config.holder.ConfigUpdateCallback;
import org.apache.inlong.dataproxy.config.holder.GroupIdNumConfigHolder;
import org.apache.inlong.dataproxy.config.holder.MetaConfigHolder;
import org.apache.inlong.dataproxy.config.holder.SourceReportConfigHolder;
import org.apache.inlong.dataproxy.config.holder.SourceReportInfo;
import org.apache.inlong.dataproxy.config.holder.WeightConfigHolder;
import org.apache.inlong.dataproxy.config.holder.WhiteListConfigHolder;
import org.apache.inlong.dataproxy.config.pojo.CacheClusterConfig;
import org.apache.inlong.dataproxy.config.pojo.IdTopicConfig;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.apache.inlong.dataproxy.utils.HttpUtils;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Config manager class.
 */
public class ConfigManager {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigManager.class);

    public static final List<ConfigHolder> CONFIG_HOLDER_LIST = new ArrayList<>();
    // whether handshake manager ok
    public static final AtomicBoolean handshakeManagerOk = new AtomicBoolean(false);
    private static volatile boolean isInit = false;
    private static ConfigManager instance = null;
    // node weight configure
    private final WeightConfigHolder weightConfigHolder = new WeightConfigHolder();
    // black list configure
    private final BlackListConfigHolder blacklistConfigHolder = new BlackListConfigHolder();
    // whitelist configure
    private final WhiteListConfigHolder whitelistConfigHolder = new WhiteListConfigHolder();
    // group id num 2 name configure
    private final GroupIdNumConfigHolder groupIdConfig = new GroupIdNumConfigHolder();
    // mq configure and topic configure holder
    private final MetaConfigHolder metaConfigHolder = new MetaConfigHolder();
    // source report configure holder
    private final SourceReportConfigHolder sourceReportConfigHolder = new SourceReportConfigHolder();
    // mq clusters ready
    private volatile boolean mqClusterReady = false;

    /**
     * get instance for config manager
     */
    public static ConfigManager getInstance() {
        if (isInit && instance != null) {
            return instance;
        }
        synchronized (ConfigManager.class) {
            if (!isInit) {
                instance = new ConfigManager();
                for (ConfigHolder holder : CONFIG_HOLDER_LIST) {
                    holder.loadFromFileToHolder();
                }
                ReloadConfigWorker reloadProperties = ReloadConfigWorker.create(instance);
                reloadProperties.setDaemon(true);
                reloadProperties.start();
                isInit = true;
            }
        }
        return instance;
    }

    // get node weight configure
    public double getCpuWeight() {
        return weightConfigHolder.getCachedCpuWeight();
    }

    public double getNetInWeight() {
        return weightConfigHolder.getCachedNetInWeight();
    }

    public double getNetOutWeight() {
        return weightConfigHolder.getCachedNetOutWeight();
    }

    public double getTcpWeight() {
        return weightConfigHolder.getCachedTcpWeight();
    }

    public double getCpuThresholdWeight() {
        return weightConfigHolder.getCachedCpuThreshold();
    }

    /**
     * get topic by groupId and streamId
     */
    public String getTopicName(String groupId, String streamId) {
        return metaConfigHolder.getBaseTopicName(groupId, streamId);
    }

    public IdTopicConfig getIdTopicConfig(String groupId, String streamId) {
        return metaConfigHolder.getIdTopicConfig(groupId, streamId);
    }

    public String getMetaConfigMD5() {
        return metaConfigHolder.getConfigMd5();
    }

    public boolean updateMetaConfigInfo(String inDataMd5, String inDataJsonStr) {
        return metaConfigHolder.updateConfigMap(inDataMd5, inDataJsonStr);
    }

    // register meta-config callback
    public void regMetaConfigChgCallback(ConfigUpdateCallback callback) {
        metaConfigHolder.addUpdateCallback(callback);
    }

    public List<CacheClusterConfig> getCachedCLusterConfig() {
        return metaConfigHolder.forkCachedCLusterConfig();
    }

    public Set<String> getAllTopicNames() {
        return metaConfigHolder.getAllTopicName();
    }

    // get groupId num 2 name info
    public boolean isEnableNum2NameTrans(String groupIdNum) {
        return groupIdConfig.isEnableNum2NameTrans(groupIdNum);
    }

    public boolean isGroupIdNumConfigEmpty() {
        return groupIdConfig.isGroupIdNumConfigEmpty();
    }

    public boolean isStreamIdNumConfigEmpty() {
        return groupIdConfig.isStreamIdNumConfigEmpty();
    }

    public String getGroupIdNameByNum(String groupIdNum) {
        return groupIdConfig.getGroupIdNameByNum(groupIdNum);
    }

    public String getStreamIdNameByIdNum(String groupIdNum, String streamIdNum) {
        return groupIdConfig.getStreamIdNameByIdNum(groupIdNum, streamIdNum);
    }

    public ConcurrentHashMap<String, String> getGroupIdNumMap() {
        return groupIdConfig.getGroupIdNumMap();
    }

    public ConcurrentHashMap<String, ConcurrentHashMap<String, String>> getStreamIdNumMap() {
        return groupIdConfig.getStreamIdNumMap();
    }

    // get blacklist whitelist configure info
    public void regIPVisitConfigChgCallback(ConfigUpdateCallback callback) {
        blacklistConfigHolder.addUpdateCallback(callback);
        whitelistConfigHolder.addUpdateCallback(callback);
    }

    public boolean needChkIllegalIP() {
        return (blacklistConfigHolder.needCheckBlacklist()
                || whitelistConfigHolder.needCheckWhitelist());
    }

    public boolean isIllegalIP(String strRemoteIP) {
        return strRemoteIP == null
                || blacklistConfigHolder.isIllegalIP(strRemoteIP)
                || whitelistConfigHolder.isIllegalIP(strRemoteIP);
    }

    public void addSourceReportInfo(String sourceIp, String sourcePort, String protocolType) {
        sourceReportConfigHolder.addSourceInfo(sourceIp, sourcePort, protocolType);
    }

    public SourceReportInfo getSourceReportInfo() {
        return sourceReportConfigHolder.getSourceReportInfo();
    }

    public boolean isMqClusterReady() {
        return mqClusterReady;
    }

    public void setMqClusterReady() {
        mqClusterReady = true;
    }

    /**
     * load worker
     */
    public static class ReloadConfigWorker extends Thread {

        private final ConfigManager configManager;
        private final CloseableHttpClient httpClient;
        private final Gson gson = new Gson();
        private boolean isRunning = true;
        private final AtomicInteger managerIpListIndex = new AtomicInteger(0);

        private ReloadConfigWorker(ConfigManager managerInstance) {
            this.configManager = managerInstance;
            this.httpClient = constructHttpClient();
            SecureRandom random = new SecureRandom(String.valueOf(System.currentTimeMillis()).getBytes());
            managerIpListIndex.set(random.nextInt());
        }

        public static ReloadConfigWorker create(ConfigManager managerInstance) {
            return new ReloadConfigWorker(managerInstance);
        }

        @Override
        public void run() {
            long count = 0;
            long startTime;
            long wstTime;
            boolean fisrtCheck = true;
            LOG.info("Reload-Config Worker started!");
            while (isRunning) {
                count += 1;
                startTime = System.currentTimeMillis();
                try {
                    // check and load local configure files
                    for (ConfigHolder holder : CONFIG_HOLDER_LIST) {
                        if (holder.checkAndUpdateHolder()) {
                            holder.executeCallbacks();
                        }
                    }
                    // connect to manager
                    if (fisrtCheck) {
                        fisrtCheck = false;
                        checkRemoteConfig();
                        count = 0;
                    } else {
                        // wait for 3 * check-time to update remote config
                        if (count % 3 == 0) {
                            checkRemoteConfig();
                            count = 0;
                        }
                    }
                    // check processing time
                    wstTime = System.currentTimeMillis() - startTime;
                    if (wstTime > 60000L) {
                        LOG.warn("Reload-Config Worker process wast({}) over 60000 millis", wstTime);
                    }
                    // sleep for next check
                    TimeUnit.MILLISECONDS.sleep(
                            CommonConfigHolder.getInstance().getMetaConfigSyncInvlMs() + getRandom(0, 5000));
                } catch (InterruptedException ex1) {
                    LOG.error("Reload-Config Worker encounters an interrupt exception, break processing", ex1);
                    break;
                } catch (Throwable ex2) {
                    LOG.error("Reload-Config Worker encounters exception, continue process", ex2);
                }
            }
            LOG.info("Reload-Config Worker existed!");
        }

        public void close() {
            isRunning = false;
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

        private int getRandom(int min, int max) {
            return (int) (Math.random() * (max + 1 - min)) + min;
        }

        private void checkRemoteConfig() {
            String proxyClusterName = CommonConfigHolder.getInstance().getClusterName();
            String proxyClusterTag = CommonConfigHolder.getInstance().getClusterTag();
            if (StringUtils.isBlank(proxyClusterName) || StringUtils.isBlank(proxyClusterTag)) {
                LOG.error("Found {} or {} is blank in {}, can't quest remote configure!",
                        CommonConfigHolder.KEY_PROXY_CLUSTER_NAME,
                        CommonConfigHolder.KEY_PROXY_CLUSTER_TAG,
                        CommonConfigHolder.COMMON_CONFIG_FILE_NAME);
                return;
            }
            List<String> managerIpList = CommonConfigHolder.getInstance().getManagerHosts();
            if (managerIpList == null || managerIpList.size() == 0) {
                LOG.error("Found manager ip list are empty, can't quest remote configure!");
                return;
            }
            int managerIpSize = managerIpList.size();
            for (int i = 0; i < managerIpList.size(); i++) {
                String host = managerIpList.get(Math.abs(managerIpListIndex.getAndIncrement()) % managerIpSize);
                if (this.reloadDataProxyConfig(proxyClusterName, proxyClusterTag, host)) {
                    break;
                }
            }
        }

        /**
         * reloadDataProxyConfig
         */
        private boolean reloadDataProxyConfig(String clusterName, String clusterTag, String host) {
            String url = null;
            HttpPost httpPost = null;
            try {
                url = "http://" + host + ConfigConstants.MANAGER_PATH
                        + ConfigConstants.MANAGER_GET_ALL_CONFIG_PATH;
                httpPost = HttpUtils.getHttPost(url);
                // request body
                DataProxyConfigRequest request = new DataProxyConfigRequest();
                request.setClusterName(clusterName);
                request.setClusterTag(clusterTag);
                if (StringUtils.isNotBlank(configManager.getMetaConfigMD5())) {
                    request.setMd5(configManager.getMetaConfigMD5());
                }
                httpPost.setEntity(HttpUtils.getEntity(request));
                // request with post
                LOG.info("Start to request {} to get config info, with params: {}, headers: {}",
                        url, request, httpPost.getAllHeaders());
                CloseableHttpResponse response = httpClient.execute(httpPost);
                String returnStr = EntityUtils.toString(response.getEntity());
                if (response.getStatusLine().getStatusCode() != 200) {
                    LOG.warn("Failed to request {}, with params: {}, headers: {}, the response is {}",
                            url, request, httpPost.getAllHeaders(), returnStr);
                    return false;
                }
                LOG.info("End to request {} to get config info:{}", url, returnStr);
                // get groupId <-> topic and m value.
                DataProxyConfigResponse proxyResponse =
                        gson.fromJson(returnStr, DataProxyConfigResponse.class);
                if (!proxyResponse.isResult()) {
                    LOG.warn("Fail to get config from url {}, with params {}, error code is {}",
                            url, request, proxyResponse.getErrCode());
                    return false;
                }
                if (proxyResponse.getErrCode() != DataProxyConfigResponse.SUCC) {
                    if (proxyResponse.getErrCode() != DataProxyConfigResponse.NOUPDATE) {
                        LOG.warn("Get config failure from url:{}, with params {}, error code is {}",
                                url, request, proxyResponse.getErrCode());
                    }
                    return true;
                }
                DataProxyCluster dataProxyCluster = proxyResponse.getData();
                if (dataProxyCluster == null
                        || dataProxyCluster.getCacheClusterSet() == null
                        || dataProxyCluster.getCacheClusterSet().getCacheClusters().isEmpty()) {
                    LOG.warn("Get config empty from url:{}, with params {}, return:{}, cluster is empty!",
                            url, request, returnStr);
                    return true;
                }
                // update meta configure
                if (configManager.updateMetaConfigInfo(proxyResponse.getMd5(), returnStr)) {
                    ConfigManager.handshakeManagerOk.set(true);
                    LOG.info("Get config success from manager and updated, set handshake status is ok!");
                }
                return true;
            } catch (Throwable ex) {
                LOG.error("Request manager {} failure, throw exception", url, ex);
                return false;
            } finally {
                if (httpPost != null) {
                    httpPost.releaseConnection();
                }
            }
        }
    }
}
