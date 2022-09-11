/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.dataproxy;

import org.apache.commons.lang.StringUtils;
import org.apache.inlong.sdk.dataproxy.network.ProxysdkException;
import org.apache.inlong.sdk.dataproxy.network.Utils;

public class ProxyClientConfig {

    private int aliveConnections;
    private int syncThreadPoolSize;
    private int asyncCallbackSize;
    private int managerPort = 8099;
    private String managerIP = "";

    private String managerIpLocalPath = System.getProperty("user.dir") + "/.inlong/.managerIps";
    private String proxyIPServiceURL = "";
    private int proxyUpdateIntervalMinutes;
    private int proxyUpdateMaxRetry;
    private String netTag;
    private String groupId;
    private boolean isFile = false;
    private boolean isLocalVisit = true;
    private boolean isNeedDataEncry = false;
    private boolean needAuthentication = false;
    private String userName = "";
    private String secretKey = "";
    private String rsaPubKeyUrl = "";
    private String confStoreBasePath = System.getProperty("user.dir") + "/.inlong/";
    private boolean needVerServer = false;
    private String tlsServerCertFilePathAndName;
    private String tlsServerKey;
    private int maxTimeoutCnt = ConfigConstants.MAX_TIMEOUT_CNT;

    private boolean enableSaveManagerVIps = true;

    private boolean enableSlaMetric = false;

    private int managerConnectionTimeout = 10000;
    private boolean readProxyIPFromLocal = false;
    /**
     * Default connection, handshake, and initial request timeout in milliseconds.
     */
    private long connectTimeoutMillis;
    private long requestTimeoutMillis;

    private int managerSocketTimeout = 30 * 1000;

    // configuration for http client
    // whether discard old metric when cache is full.
    private boolean discardOldMessage = false;
    private int proxyHttpUpdateIntervalMinutes;
    // thread number for async sending data.
    private int asyncWorkerNumber = 3;
    // interval for async worker in microseconds.
    private int asyncWorkerInterval = 500;
    private boolean cleanHttpCacheWhenClosing = false;

    // config for metric collector
    // whether use groupId as key for metric, default is true
    private boolean useGroupIdAsKey = true;
    // whether use StreamId as key for metric, default is true
    private boolean useStreamIdAsKey = true;
    // whether use localIp as key for metric, default is true
    private boolean useLocalIpAsKey = true;
    // metric collection interval, default is 1 mins in milliseconds.
    private int metricIntervalInMs = 60 * 1000;
    // max cache time for proxy config.
    private long maxProxyCacheTimeInMs = 30 * 60 * 1000;

    // metric groupId
    private String metricGroupId = "inlong_sla_metric";

    private int ioThreadNum = Runtime.getRuntime().availableProcessors();
    private boolean enableBusyWait = false;

    /*pay attention to the last url parameter ip*/
    public ProxyClientConfig(String localHost, boolean isLocalVisit, String managerIp,
            int managerPort, String groupId, String netTag) throws ProxysdkException {
        if (Utils.isBlank(localHost)) {
            throw new ProxysdkException("localHost is blank!");
        }
        if (Utils.isBlank(managerIp)) {
            throw new IllegalArgumentException("managerIp is Blank!");
        }
        this.proxyIPServiceURL =
                "http://" + managerIp + ":" + managerPort + "/api/inlong/manager/openapi/dataproxy/getIpList";
        this.groupId = groupId;
        this.netTag = netTag;
        this.isLocalVisit = isLocalVisit;
        this.managerPort = managerPort;
        this.managerIP = managerIp;
        Utils.validLocalIp(localHost);
        this.aliveConnections = ConfigConstants.ALIVE_CONNECTIONS;
        this.syncThreadPoolSize = ConfigConstants.SYNC_THREAD_POOL_SIZE;
        this.asyncCallbackSize = ConfigConstants.ASYNC_CALLBACK_SIZE;
        this.proxyUpdateIntervalMinutes = ConfigConstants.PROXY_UPDATE_INTERVAL_MINUTES;
        this.proxyHttpUpdateIntervalMinutes = ConfigConstants.PROXY_HTTP_UPDATE_INTERVAL_MINUTES;
        this.proxyUpdateMaxRetry = ConfigConstants.PROXY_UPDATE_MAX_RETRY;
        this.connectTimeoutMillis = ConfigConstants.DEFAULT_CONNECT_TIMEOUT_MILLIS;
        this.setRequestTimeoutMillis(ConfigConstants.DEFAULT_SEND_BUFFER_SIZE);
    }

    public String getTlsServerCertFilePathAndName() {
        return tlsServerCertFilePathAndName;
    }

    public String getTlsServerKey() {
        return tlsServerKey;
    }

    public boolean isLocalVisit() {
        return isLocalVisit;
    }

    public boolean isFile() {
        return isFile;
    }

    public void setFile(boolean file) {
        isFile = file;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public int getManagerPort() {
        return managerPort;
    }

    public String getManagerIP() {
        return managerIP;
    }

    public String getManagerIpLocalPath() {
        return managerIpLocalPath;
    }

    public void setManagerIpLocalPath(String managerIpLocalPath) throws ProxysdkException {
        if (StringUtils.isEmpty(managerIpLocalPath)) {
            throw new ProxysdkException("managerIpLocalPath is empty.");
        }
        if (managerIpLocalPath.charAt(managerIpLocalPath.length() - 1) == '/') {
            managerIpLocalPath = managerIpLocalPath.substring(0, managerIpLocalPath.length() - 1);
        }
        this.managerIpLocalPath = managerIpLocalPath + "/.managerIps";
    }

    public boolean isEnableSaveManagerVIps() {
        return enableSaveManagerVIps;
    }

    public void setEnableSaveManagerVIps(boolean enable) {
        this.enableSaveManagerVIps = enable;
    }

    public String getConfStoreBasePath() {
        return confStoreBasePath;
    }

    public void setConfStoreBasePath(String confStoreBasePath) {
        this.confStoreBasePath = confStoreBasePath;
    }

    public int getAliveConnections() {
        return this.aliveConnections;
    }

    public void setAliveConnections(int aliveConnections) {
        this.aliveConnections = aliveConnections;
    }

    public int getSyncThreadPoolSize() {
        return syncThreadPoolSize;
    }

    public void setSyncThreadPoolSize(int syncThreadPoolSize) {
        if (syncThreadPoolSize > ConfigConstants.MAX_SYNC_THREAD_POOL_SIZE) {
            throw new IllegalArgumentException("");
        }
        this.syncThreadPoolSize = syncThreadPoolSize;
    }

    public int getTotalAsyncCallbackSize() {
        return asyncCallbackSize;
    }

    public void setTotalAsyncCallbackSize(int asyncCallbackSize) {
        this.asyncCallbackSize = asyncCallbackSize;
    }

    public String getProxyIPServiceURL() {
        return proxyIPServiceURL;
    }

    public int getMaxTimeoutCnt() {
        return maxTimeoutCnt;
    }

    public void setMaxTimeoutCnt(int maxTimeoutCnt) {
        if (maxTimeoutCnt < 0) {
            throw new IllegalArgumentException("maxTimeoutCnt must bigger than 0");
        }
        this.maxTimeoutCnt = maxTimeoutCnt;
    }

    public int getProxyUpdateIntervalMinutes() {
        return proxyUpdateIntervalMinutes;
    }

    public void setProxyUpdateIntervalMinutes(int proxyUpdateIntervalMinutes) {
        this.proxyUpdateIntervalMinutes = proxyUpdateIntervalMinutes;
    }

    public int getProxyUpdateMaxRetry() {
        return proxyUpdateMaxRetry;
    }

    public void setProxyUpdateMaxRetry(int proxyUpdateMaxRetry) {
        this.proxyUpdateMaxRetry = proxyUpdateMaxRetry;
    }

    public long getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(long connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public boolean isNeedVerServer() {
        return needVerServer;
    }

    public long getRequestTimeoutMillis() {
        return requestTimeoutMillis;
    }

    public void setRequestTimeoutMillis(long requestTimeoutMillis) {
        this.requestTimeoutMillis = requestTimeoutMillis;
    }

    public String getNetTag() {
        return netTag;
    }

    public String getRsaPubKeyUrl() {
        return rsaPubKeyUrl;
    }

    public boolean isNeedDataEncry() {
        return isNeedDataEncry;
    }

    public boolean isNeedAuthentication() {
        return this.needAuthentication;
    }

    public void setAuthenticationInfo(boolean needAuthentication, boolean needDataEncry,
            final String userName, final String secretKey) {
        this.needAuthentication = needAuthentication;
        this.isNeedDataEncry = needDataEncry;
        if (this.needAuthentication || this.isNeedDataEncry) {
            if (Utils.isBlank(userName)) {
                throw new IllegalArgumentException("userName is Blank!");
            }
            if (Utils.isBlank(secretKey)) {
                throw new IllegalArgumentException("secretKey is Blank!");
            }
        }
        this.userName = userName.trim();
        this.secretKey = secretKey.trim();
    }

    public void setHttpsInfo(String tlsServerCertFilePathAndName, String tlsServerKey) {
        if (Utils.isBlank(tlsServerCertFilePathAndName)) {
            throw new IllegalArgumentException("tlsServerCertFilePathAndName is Blank!");
        }
        if (Utils.isBlank(tlsServerKey)) {
            throw new IllegalArgumentException("tlsServerKey is Blank!");
        }
        this.needVerServer = true;
        this.tlsServerKey = tlsServerKey;
        this.tlsServerCertFilePathAndName = tlsServerCertFilePathAndName;
    }

    public String getUserName() {
        return userName;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public boolean isReadProxyIPFromLocal() {
        return readProxyIPFromLocal;
    }

    public void setReadProxyIPFromLocal(boolean readProxyIPFromLocal) {
        this.readProxyIPFromLocal = readProxyIPFromLocal;
    }

    public int getProxyHttpUpdateIntervalMinutes() {
        return proxyHttpUpdateIntervalMinutes;
    }

    public void setProxyHttpUpdateIntervalMinutes(int proxyHttpUpdateIntervalMinutes) {
        this.proxyHttpUpdateIntervalMinutes = proxyHttpUpdateIntervalMinutes;
    }

    public boolean isDiscardOldMessage() {
        return discardOldMessage;
    }

    public void setDiscardOldMessage(boolean discardOldMessage) {
        this.discardOldMessage = discardOldMessage;
    }

    public int getAsyncWorkerNumber() {
        return asyncWorkerNumber;
    }

    public void setAsyncWorkerNumber(int asyncWorkerNumber) {
        this.asyncWorkerNumber = asyncWorkerNumber;
    }

    public int getAsyncWorkerInterval() {
        return asyncWorkerInterval;
    }

    public void setAsyncWorkerInterval(int asyncWorkerInterval) {
        this.asyncWorkerInterval = asyncWorkerInterval;
    }

    public int getManagerSocketTimeout() {
        return managerSocketTimeout;
    }

    public void setManagerSocketTimeout(int managerSocketTimeout) {
        this.managerSocketTimeout = managerSocketTimeout;
    }

    public boolean isCleanHttpCacheWhenClosing() {
        return cleanHttpCacheWhenClosing;
    }

    public void setCleanHttpCacheWhenClosing(boolean cleanHttpCacheWhenClosing) {
        this.cleanHttpCacheWhenClosing = cleanHttpCacheWhenClosing;
    }

    public boolean isUseGroupIdAsKey() {
        return useGroupIdAsKey;
    }

    public void setUseGroupIdAsKey(boolean useGroupIdAsKey) {
        this.useGroupIdAsKey = useGroupIdAsKey;
    }

    public boolean isUseStreamIdAsKey() {
        return useStreamIdAsKey;
    }

    public void setUseStreamIdAsKey(boolean useStreamIdAsKey) {
        this.useStreamIdAsKey = useStreamIdAsKey;
    }

    public boolean isUseLocalIpAsKey() {
        return useLocalIpAsKey;
    }

    public void setUseLocalIpAsKey(boolean useLocalIpAsKey) {
        this.useLocalIpAsKey = useLocalIpAsKey;
    }

    public int getMetricIntervalInMs() {
        return metricIntervalInMs;
    }

    public void setMetricIntervalInMs(int metricIntervalInMs) {
        this.metricIntervalInMs = metricIntervalInMs;
    }

    public String getMetricGroupId() {
        return metricGroupId;
    }

    public void setMetricGroupId(String metricGroupId) {
        this.metricGroupId = metricGroupId;
    }

    public long getMaxProxyCacheTimeInMs() {
        return maxProxyCacheTimeInMs;
    }

    public void setMaxProxyCacheTimeInMs(long maxProxyCacheTimeInMs) {
        this.maxProxyCacheTimeInMs = maxProxyCacheTimeInMs;
    }

    public int getManagerConnectionTimeout() {
        return managerConnectionTimeout;
    }

    public void setManagerConnectionTimeout(int managerConnectionTimeout) {
        this.managerConnectionTimeout = managerConnectionTimeout;
    }

    public boolean isEnableSlaMetric() {
        return enableSlaMetric;
    }

    public void setEnableSlaMetric(boolean enableSlaMetric) {
        this.enableSlaMetric = enableSlaMetric;
    }

    public int getIoThreadNum() {
        return ioThreadNum;
    }

    public void setIoThreadNum(int ioThreadNum) {
        this.ioThreadNum = ioThreadNum;
    }

    public boolean isEnableBusyWait() {
        return enableBusyWait;
    }

    public void setEnableBusyWait(boolean enableBusyWait) {
        this.enableBusyWait = enableBusyWait;
    }
}
