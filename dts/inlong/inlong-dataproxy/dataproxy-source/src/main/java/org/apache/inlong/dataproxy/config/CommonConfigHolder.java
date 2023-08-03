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

import org.apache.inlong.dataproxy.sink.common.DefaultEventHandler;
import org.apache.inlong.dataproxy.sink.mq.AllCacheClusterSelector;
import org.apache.inlong.sdk.commons.protocol.ProxySdk;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.flume.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * common.properties Configure Holder
 */
public class CommonConfigHolder {

    public static final Logger LOG = LoggerFactory.getLogger(CommonConfigHolder.class);
    // configure file name
    public static final String COMMON_CONFIG_FILE_NAME = "common.properties";
    // **** allowed keys and default value, begin
    // cluster tag
    public static final String KEY_PROXY_CLUSTER_TAG = "proxy.cluster.tag";
    public static final String VAL_DEF_CLUSTER_TAG = "default_cluster";
    // cluster name
    public static final String KEY_PROXY_CLUSTER_NAME = "proxy.cluster.name";
    public static final String VAL_DEF_CLUSTER_NAME = "default_dataproxy";
    // cluster incharges
    public static final String KEY_PROXY_CLUSTER_INCHARGES = "proxy.cluster.inCharges";
    public static final String VAL_DEF_CLUSTER_INCHARGES = "admin";
    // cluster exttag,
    public static final String KEY_PROXY_CLUSTER_EXT_TAG = "proxy.cluster.extTag";
    // predefined format of ext tag: {key}={value}
    public static final String VAL_DEF_CLUSTER_EXT_TAG = "default=true";
    // manager type
    public static final String KEY_MANAGER_TYPE = "manager.type";
    public static final String VAL_DEF_MANAGER_TYPE = DefaultManagerIpListParser.class.getName();
    // manager hosts
    public static final String KEY_MANAGER_HOSTS = "manager.hosts";
    public static final String KEY_MANAGER_HOSTS_SEPARATOR = ",";
    // manager auth secret id
    public static final String KEY_MANAGER_AUTH_SECRET_ID = "manager.auth.secretId";
    // manager auth secret key
    public static final String KEY_MANAGER_AUTH_SECRET_KEY = "manager.auth.secretKey";
    // configure file check interval
    private static final String KEY_META_CONFIG_SYNC_INTERVAL_MS = "meta.config.sync.interval.ms";
    private static final String KEY_CONFIG_CHECK_INTERVAL_MS = "configCheckInterval";
    public static final long VAL_DEF_CONFIG_SYNC_INTERVAL_MS = 60000L;
    public static final long VAL_MIN_CONFIG_SYNC_INTERVAL_MS = 10000L;
    // whether to startup using the local metadata.json file without connecting to the Manager
    private static final String KEY_ENABLE_STARTUP_USING_LOCAL_META_FILE =
            "startup.using.local.meta.file.enable";
    public static final boolean VAL_DEF_ENABLE_STARTUP_USING_LOCAL_META_FILE = false;
    // whether to accept messages without mapping between groupId/streamId and topic
    public static final String KEY_ENABLE_UNCONFIGURED_TOPIC_ACCEPT = "id2topic.unconfigured.accept.enable";
    public static final boolean VAL_DEF_ENABLE_UNCONFIGURED_TOPIC_ACCEPT = false;
    // default topics configure key, multiple topic settings are separated by "\\s+".
    public static final String KEY_UNCONFIGURED_TOPIC_DEFAULT_TOPICS = "id2topic.unconfigured.default.topics";
    public static final String VAL_DEFAULT_TOPIC = "test";
    // whether enable whitelist, optional field.
    public static final String KEY_ENABLE_WHITELIST = "proxy.enable.whitelist";
    public static final boolean VAL_DEF_ENABLE_WHITELIST = false;
    // whether enable file metric, optional field.
    public static final String KEY_ENABLE_FILE_METRIC = "file.metric.enable";
    public static final boolean VAL_DEF_ENABLE_FILE_METRIC = true;
    // file metric statistic interval (second)
    public static final String KEY_FILE_METRIC_STAT_INTERVAL_SEC = "file.metric.stat.interval.sec";
    public static final int VAL_DEF_FILE_METRIC_STAT_INVL_SEC = 60;
    public static final int VAL_MIN_FILE_METRIC_STAT_INVL_SEC = 0;
    // file metric max statistic key count
    public static final String KEY_FILE_METRIC_MAX_CACHE_CNT = "file.metric.max.cache.cnt";
    public static final int VAL_DEF_FILE_METRIC_MAX_CACHE_CNT = 1000000;
    public static final int VAL_MIN_FILE_METRIC_MAX_CACHE_CNT = 0;
    // source metric statistic name
    public static final String KEY_FILE_METRIC_SOURCE_OUTPUT_NAME = "file.metric.source.output.name";
    public static final String VAL_DEF_FILE_METRIC_SOURCE_OUTPUT_NAME = "Source";
    // sink metric statistic name
    public static final String KEY_FILE_METRIC_SINK_OUTPUT_NAME = "file.metric.sink.output.name";
    public static final String VAL_DEF_FILE_METRIC_SINK_OUTPUT_NAME = "Sink";
    // event metric statistic name
    public static final String KEY_FILE_METRIC_EVENT_OUTPUT_NAME = "file.metric.event.output.name";
    public static final String VAL_DEF_FILE_METRIC_EVENT_OUTPUT_NAME = "Stats";
    // Audit fields
    public static final String KEY_ENABLE_AUDIT = "audit.enable";
    public static final boolean VAL_DEF_ENABLE_AUDIT = true;
    public static final String KEY_AUDIT_PROXYS = "audit.proxys";
    public static final String KEY_AUDIT_FILE_PATH = "audit.filePath";
    public static final String VAL_DEF_AUDIT_FILE_PATH = "/data/inlong/audit/";
    public static final String KEY_AUDIT_MAX_CACHE_ROWS = "audit.maxCacheRows";
    public static final int VAL_DEF_AUDIT_MAX_CACHE_ROWS = 2000000;
    public static final String KEY_AUDIT_FORMAT_INTERVAL_MS = "auditFormatInterval";
    public static final long VAL_DEF_AUDIT_FORMAT_INTERVAL_MS = 60000L;
    // whether to retry after the message send failure
    public static final String KEY_ENABLE_SEND_RETRY_AFTER_FAILURE = "send.retry.after.failure";
    public static final boolean VAL_DEF_ENABLE_SEND_RETRY_AFTER_FAILURE = true;
    // max retry count
    public static final String KEY_MAX_RETRIES_AFTER_FAILURE = "max.retries.after.failure";
    public static final int VAL_DEF_MAX_RETRIES_AFTER_FAILURE = -1;
    public static final String KEY_RESPONSE_AFTER_SAVE = "isResponseAfterSave";
    public static final boolean VAL_DEF_RESPONSE_AFTER_SAVE = false;
    // Same as KEY_MAX_RESPONSE_TIMEOUT_MS = "maxResponseTimeoutMs";
    public static final String KEY_MAX_RAS_TIMEOUT_MS = "maxRASTimeoutMs";
    public static final long VAL_DEF_MAX_RAS_TIMEOUT_MS = 10000L;
    // max buffer queue size in Kb
    public static final String KEY_MAX_BUFFERQUEUE_SIZE_KB = "maxBufferQueueSizeKb";
    public static final int VAL_DEF_MAX_BUFFERQUEUE_SIZE_KB = 128 * 1024;
    // event handler
    public static final String KEY_EVENT_HANDLER = "eventHandler";
    public static final String VAL_DEF_EVENT_HANDLER = DefaultEventHandler.class.getName();
    // cache cluster selector
    public static final String KEY_CACHE_CLUSTER_SELECTOR = "cacheClusterSelector";
    public static final String VAL_DEF_CACHE_CLUSTER_SELECTOR = AllCacheClusterSelector.class.getName();
    // proxy node id
    public static final String KEY_PROXY_NODE_ID = "nodeId";
    public static final String VAL_DEF_PROXY_NODE_ID = "127.0.0.1";
    // msg sent compress type
    public static final String KEY_MSG_SENT_COMPRESS_TYPE = "compressType";
    public static final String VAL_DEF_MSG_COMPRESS_TYPE = ProxySdk.INLONG_COMPRESSED_TYPE.INLONG_SNAPPY.name();
    // prometheus http port
    public static final String KEY_PROMETHEUS_HTTP_PORT = "prometheusHttpPort";
    public static final int VAL_DEF_PROMETHEUS_HTTP_PORT = 8080;
    // **** allowed keys and default value, end

    // class instance
    private static CommonConfigHolder instance = null;
    private static volatile boolean isInit = false;
    private Map<String, String> props;
    // pre-read field values
    private String clusterTag = VAL_DEF_CLUSTER_TAG;
    private String clusterName = VAL_DEF_CLUSTER_NAME;
    private String clusterIncharges = VAL_DEF_CLUSTER_INCHARGES;
    private String clusterExtTag = VAL_DEF_CLUSTER_EXT_TAG;
    private String managerType = VAL_DEF_MANAGER_TYPE;
    private IManagerIpListParser ipListParser = null;
    private String managerAuthSecretId = "";
    private String managerAuthSecretKey = "";
    private boolean enableStartupUsingLocalMetaFile = VAL_DEF_ENABLE_STARTUP_USING_LOCAL_META_FILE;
    private long metaConfigSyncInvlMs = VAL_DEF_CONFIG_SYNC_INTERVAL_MS;
    private boolean enableAudit = VAL_DEF_ENABLE_AUDIT;
    private final HashSet<String> auditProxys = new HashSet<>();
    private String auditFilePath = VAL_DEF_AUDIT_FILE_PATH;
    private int auditMaxCacheRows = VAL_DEF_AUDIT_MAX_CACHE_ROWS;
    private long auditFormatInvlMs = VAL_DEF_AUDIT_FORMAT_INTERVAL_MS;
    private boolean responseAfterSave = VAL_DEF_RESPONSE_AFTER_SAVE;
    private long maxResAfterSaveTimeout = VAL_DEF_MAX_RAS_TIMEOUT_MS;
    private boolean enableUnConfigTopicAccept = VAL_DEF_ENABLE_UNCONFIGURED_TOPIC_ACCEPT;
    private List<String> defaultTopics = Arrays.asList(VAL_DEFAULT_TOPIC);
    private boolean enableWhiteList = VAL_DEF_ENABLE_WHITELIST;
    private int maxBufferQueueSizeKb = VAL_DEF_MAX_BUFFERQUEUE_SIZE_KB;
    private String eventHandler = VAL_DEF_EVENT_HANDLER;
    private String cacheClusterSelector = VAL_DEF_CACHE_CLUSTER_SELECTOR;
    private String proxyNodeId = VAL_DEF_PROXY_NODE_ID;
    private String msgCompressType = VAL_DEF_MSG_COMPRESS_TYPE;
    private int prometheusHttpPort = VAL_DEF_PROMETHEUS_HTTP_PORT;
    private boolean enableFileMetric = VAL_DEF_ENABLE_FILE_METRIC;
    private int fileMetricStatInvlSec = VAL_DEF_FILE_METRIC_STAT_INVL_SEC;
    private int fileMetricStatCacheCnt = VAL_DEF_FILE_METRIC_MAX_CACHE_CNT;
    private String fileMetricSourceOutName = VAL_DEF_FILE_METRIC_SOURCE_OUTPUT_NAME;
    private String fileMetricSinkOutName = VAL_DEF_FILE_METRIC_SINK_OUTPUT_NAME;
    private String fileMetricEventOutName = VAL_DEF_FILE_METRIC_EVENT_OUTPUT_NAME;
    private boolean enableSendRetryAfterFailure = VAL_DEF_ENABLE_SEND_RETRY_AFTER_FAILURE;
    private int maxRetriesAfterFailure = VAL_DEF_MAX_RETRIES_AFTER_FAILURE;

    /**
     * get instance for common.properties config manager
     */
    public static CommonConfigHolder getInstance() {
        if (isInit && instance != null) {
            return instance;
        }
        synchronized (CommonConfigHolder.class) {
            if (!isInit) {
                instance = new CommonConfigHolder();
                if (instance.loadConfigFile()) {
                    instance.preReadFields();
                }
                isInit = true;
            }
        }
        return instance;
    }

    /**
     * Get the original attribute map
     *
     * Notice: only the non-pre-read fields need to be searched from the attribute map,
     *         the pre-read fields MUST be got according to the methods in the class.
     */
    public Map<String, String> getProperties() {
        return this.props;
    }

    /**
     * getStringFromContext
     *
     * @param context
     * @param key
     * @param defaultValue
     * @return
     */
    public static String getStringFromContext(Context context, String key, String defaultValue) {
        String value = context.getString(key);
        value = (value != null) ? value : getInstance().getProperties().getOrDefault(key, defaultValue);
        return value;
    }

    public String getClusterTag() {
        return clusterTag;
    }

    public String getClusterName() {
        return this.clusterName;
    }

    public String getClusterIncharges() {
        return clusterIncharges;
    }

    public String getClusterExtTag() {
        return clusterExtTag;
    }

    public long getMetaConfigSyncInvlMs() {
        return metaConfigSyncInvlMs;
    }

    public boolean isEnableUnConfigTopicAccept() {
        return enableUnConfigTopicAccept;
    }

    public List<String> getDefTopics() {
        return defaultTopics;
    }

    public String getRandDefTopics() {
        if (defaultTopics.isEmpty()) {
            return null;
        }
        SecureRandom rand = new SecureRandom();
        return defaultTopics.get(rand.nextInt(defaultTopics.size()));
    }

    public boolean isEnableWhiteList() {
        return this.enableWhiteList;
    }

    public String getManagerType() {
        return managerType;
    }

    public List<String> getManagerHosts() {
        return this.ipListParser.getIpList();
    }

    public String getManagerAuthSecretId() {
        return managerAuthSecretId;
    }

    public String getManagerAuthSecretKey() {
        return managerAuthSecretKey;
    }

    public boolean isEnableAudit() {
        return enableAudit;
    }

    public boolean isEnableFileMetric() {
        return enableFileMetric;
    }

    public int getFileMetricStatInvlSec() {
        return fileMetricStatInvlSec;
    }

    public int getFileMetricStatCacheCnt() {
        return fileMetricStatCacheCnt;
    }

    public HashSet<String> getAuditProxys() {
        return auditProxys;
    }

    public String getAuditFilePath() {
        return auditFilePath;
    }

    public int getAuditMaxCacheRows() {
        return auditMaxCacheRows;
    }

    public long getAuditFormatInvlMs() {
        return auditFormatInvlMs;
    }

    public boolean isResponseAfterSave() {
        return responseAfterSave;
    }

    public long getMaxResAfterSaveTimeout() {
        return maxResAfterSaveTimeout;
    }

    public int getMaxBufferQueueSizeKb() {
        return maxBufferQueueSizeKb;
    }

    public boolean isEnableStartupUsingLocalMetaFile() {
        return enableStartupUsingLocalMetaFile;
    }

    public String getEventHandler() {
        return eventHandler;
    }

    public String getCacheClusterSelector() {
        return cacheClusterSelector;
    }

    public int getPrometheusHttpPort() {
        return prometheusHttpPort;
    }

    public String getProxyNodeId() {
        return proxyNodeId;
    }

    public String getMsgCompressType() {
        return msgCompressType;
    }

    public String getFileMetricSourceOutName() {
        return fileMetricSourceOutName;
    }

    public String getFileMetricSinkOutName() {
        return fileMetricSinkOutName;
    }

    public String getFileMetricEventOutName() {
        return fileMetricEventOutName;
    }

    public boolean isEnableSendRetryAfterFailure() {
        return enableSendRetryAfterFailure;
    }

    public int getMaxRetriesAfterFailure() {
        return maxRetriesAfterFailure;
    }

    private void preReadFields() {
        String tmpValue;
        // read cluster tag
        tmpValue = this.props.get(KEY_PROXY_CLUSTER_TAG);
        if (StringUtils.isNotBlank(tmpValue)) {
            this.clusterTag = tmpValue.trim();
        }
        // read cluster name
        tmpValue = this.props.get(KEY_PROXY_CLUSTER_NAME);
        if (StringUtils.isNotBlank(tmpValue)) {
            this.clusterName = tmpValue.trim();
        }
        // read cluster incharges
        tmpValue = this.props.get(KEY_PROXY_CLUSTER_INCHARGES);
        if (StringUtils.isNotEmpty(tmpValue)) {
            this.clusterIncharges = tmpValue.trim();
        }
        tmpValue = this.props.get(KEY_PROXY_CLUSTER_EXT_TAG);
        if (StringUtils.isNotEmpty(tmpValue)) {
            this.clusterExtTag = tmpValue.trim();
        }
        // read configure sync interval
        tmpValue = this.props.get(KEY_META_CONFIG_SYNC_INTERVAL_MS);
        if (StringUtils.isBlank(tmpValue)) {
            tmpValue = this.props.get(KEY_CONFIG_CHECK_INTERVAL_MS);
        }
        if (StringUtils.isNotEmpty(tmpValue)) {
            long tmpSyncInvMs = NumberUtils.toLong(tmpValue.trim(), VAL_DEF_CONFIG_SYNC_INTERVAL_MS);
            if (tmpSyncInvMs >= VAL_MIN_CONFIG_SYNC_INTERVAL_MS) {
                this.metaConfigSyncInvlMs = tmpSyncInvMs;
            }
        }
        // read enable startup using local meta file
        tmpValue = this.props.get(KEY_ENABLE_STARTUP_USING_LOCAL_META_FILE);
        if (StringUtils.isNotEmpty(tmpValue)) {
            this.enableStartupUsingLocalMetaFile = "TRUE".equalsIgnoreCase(tmpValue.trim());
        }
        // read whether accept msg without id2topic configure
        tmpValue = this.props.get(KEY_ENABLE_UNCONFIGURED_TOPIC_ACCEPT);
        if (StringUtils.isNotEmpty(tmpValue)) {
            this.enableUnConfigTopicAccept = "TRUE".equalsIgnoreCase(tmpValue.trim());
        }
        // read default topics
        tmpValue = this.props.get(KEY_UNCONFIGURED_TOPIC_DEFAULT_TOPICS);
        if (StringUtils.isNotBlank(tmpValue)) {
            List<String> tmpList = new ArrayList<>();
            String[] topicItems = tmpValue.split("\\s+");
            for (String item : topicItems) {
                if (StringUtils.isBlank(item)) {
                    continue;
                }
                tmpList.add(item.trim());
            }
            if (tmpList.size() > 0) {
                defaultTopics = tmpList;
            }
        }
        // read enable whitelist
        tmpValue = this.props.get(KEY_ENABLE_WHITELIST);
        if (StringUtils.isNotEmpty(tmpValue)) {
            this.enableWhiteList = "TRUE".equalsIgnoreCase(tmpValue.trim());
        }
        // read manager type
        tmpValue = this.props.get(KEY_MANAGER_TYPE);
        if (StringUtils.isNotBlank(tmpValue)) {
            this.managerType = tmpValue.trim();
        }
        // read manager auth secret id
        tmpValue = this.props.get(KEY_MANAGER_AUTH_SECRET_ID);
        if (StringUtils.isNotBlank(tmpValue)) {
            this.managerAuthSecretId = tmpValue.trim();
        }
        // read manager auth secret key
        tmpValue = this.props.get(KEY_MANAGER_AUTH_SECRET_KEY);
        if (StringUtils.isNotBlank(tmpValue)) {
            this.managerAuthSecretKey = tmpValue.trim();
        }
        // read whether enable file metric
        tmpValue = this.props.get(KEY_ENABLE_FILE_METRIC);
        if (StringUtils.isNotEmpty(tmpValue)) {
            this.enableFileMetric = "TRUE".equalsIgnoreCase(tmpValue.trim());
        }
        // read file metric statistic interval
        tmpValue = this.props.get(KEY_FILE_METRIC_STAT_INTERVAL_SEC);
        if (StringUtils.isNotEmpty(tmpValue)) {
            int statInvl = NumberUtils.toInt(tmpValue.trim(), VAL_DEF_FILE_METRIC_STAT_INVL_SEC);
            if (statInvl >= VAL_MIN_FILE_METRIC_MAX_CACHE_CNT) {
                this.fileMetricStatInvlSec = statInvl;
            }
        }
        // read file metric statistic max cache count
        tmpValue = this.props.get(KEY_FILE_METRIC_MAX_CACHE_CNT);
        if (StringUtils.isNotEmpty(tmpValue)) {
            int maxCacheCnt = NumberUtils.toInt(tmpValue.trim(), VAL_DEF_FILE_METRIC_MAX_CACHE_CNT);
            if (maxCacheCnt >= VAL_MIN_FILE_METRIC_STAT_INVL_SEC) {
                this.fileMetricStatCacheCnt = maxCacheCnt;
            }
        }
        // read source file statistic output name
        tmpValue = this.props.get(KEY_FILE_METRIC_SOURCE_OUTPUT_NAME);
        if (StringUtils.isNotBlank(tmpValue)) {
            this.fileMetricSourceOutName = tmpValue.trim();
        }
        // read sink file statistic output name
        tmpValue = this.props.get(KEY_FILE_METRIC_SINK_OUTPUT_NAME);
        if (StringUtils.isNotBlank(tmpValue)) {
            this.fileMetricSinkOutName = tmpValue.trim();
        }
        // read event file statistic output name
        tmpValue = this.props.get(KEY_FILE_METRIC_EVENT_OUTPUT_NAME);
        if (StringUtils.isNotBlank(tmpValue)) {
            this.fileMetricEventOutName = tmpValue.trim();
        }
        // read whether enable audit
        tmpValue = this.props.get(KEY_ENABLE_AUDIT);
        if (StringUtils.isNotEmpty(tmpValue)) {
            this.enableAudit = "TRUE".equalsIgnoreCase(tmpValue.trim());
        }
        // read audit proxys
        tmpValue = this.props.get(KEY_AUDIT_PROXYS);
        if (StringUtils.isNotBlank(tmpValue)) {
            String[] ipPorts = tmpValue.split("\\s+");
            for (String tmpIPPort : ipPorts) {
                if (StringUtils.isBlank(tmpIPPort)) {
                    continue;
                }
                this.auditProxys.add(tmpIPPort.trim());
            }
        }
        // read audit file path
        tmpValue = this.props.get(KEY_AUDIT_FILE_PATH);
        if (StringUtils.isNotBlank(tmpValue)) {
            this.auditFilePath = tmpValue.trim();
        }
        // read audit max cache rows
        tmpValue = this.props.get(KEY_AUDIT_MAX_CACHE_ROWS);
        if (StringUtils.isNotEmpty(tmpValue)) {
            this.auditMaxCacheRows = NumberUtils.toInt(tmpValue.trim(), VAL_DEF_AUDIT_MAX_CACHE_ROWS);
        }
        // read audit format interval
        tmpValue = this.props.get(KEY_AUDIT_FORMAT_INTERVAL_MS);
        if (StringUtils.isNotEmpty(tmpValue)) {
            this.auditFormatInvlMs = NumberUtils.toLong(tmpValue.trim(), VAL_DEF_AUDIT_FORMAT_INTERVAL_MS);
        }
        // read whether response after save
        tmpValue = this.props.get(KEY_RESPONSE_AFTER_SAVE);
        if (StringUtils.isNotEmpty(tmpValue)) {
            this.responseAfterSave = "TRUE".equalsIgnoreCase(tmpValue.trim());
        }
        // read max response after save timeout
        tmpValue = this.props.get(KEY_MAX_RAS_TIMEOUT_MS);
        if (StringUtils.isNotEmpty(tmpValue)) {
            this.maxResAfterSaveTimeout = NumberUtils.toLong(tmpValue.trim(), VAL_DEF_MAX_RAS_TIMEOUT_MS);
        }
        // read max bufferqueue size
        tmpValue = this.props.get(KEY_MAX_BUFFERQUEUE_SIZE_KB);
        if (StringUtils.isNotEmpty(tmpValue)) {
            this.maxBufferQueueSizeKb = NumberUtils.toInt(tmpValue.trim(), VAL_DEF_MAX_BUFFERQUEUE_SIZE_KB);
        }
        // read event handler
        tmpValue = this.props.get(KEY_EVENT_HANDLER);
        if (StringUtils.isNotBlank(tmpValue)) {
            this.eventHandler = tmpValue.trim();
        }
        // read cache cluster selector
        tmpValue = this.props.get(KEY_CACHE_CLUSTER_SELECTOR);
        if (StringUtils.isNotBlank(tmpValue)) {
            this.cacheClusterSelector = tmpValue.trim();
        }
        // read proxy node id
        tmpValue = this.props.get(KEY_PROXY_NODE_ID);
        if (StringUtils.isNotBlank(tmpValue)) {
            this.proxyNodeId = tmpValue.trim();
        }
        // read msg compress type
        tmpValue = this.props.get(KEY_MSG_SENT_COMPRESS_TYPE);
        if (StringUtils.isNotBlank(tmpValue)) {
            this.msgCompressType = tmpValue.trim();
        }
        // read prometheus Http Port
        tmpValue = this.props.get(KEY_PROMETHEUS_HTTP_PORT);
        if (StringUtils.isNotEmpty(tmpValue)) {
            this.prometheusHttpPort = NumberUtils.toInt(tmpValue.trim(), VAL_DEF_PROMETHEUS_HTTP_PORT);
        }
        // read whether retry send message after sent failure
        tmpValue = this.props.get(KEY_ENABLE_SEND_RETRY_AFTER_FAILURE);
        if (StringUtils.isNotEmpty(tmpValue)) {
            this.enableSendRetryAfterFailure = "TRUE".equalsIgnoreCase(tmpValue.trim());
        }
        // read max retry count
        tmpValue = this.props.get(KEY_MAX_RETRIES_AFTER_FAILURE);
        if (StringUtils.isNotBlank(tmpValue)) {
            int retries = NumberUtils.toInt(tmpValue.trim(), VAL_DEF_MAX_RETRIES_AFTER_FAILURE);
            if (retries >= 0) {
                this.maxRetriesAfterFailure = retries;
            }
        }
        // initial ip parser
        try {
            Class<? extends IManagerIpListParser> ipListParserClass =
                    (Class<? extends IManagerIpListParser>) Class.forName(this.managerType);
            this.ipListParser = ipListParserClass.getDeclaredConstructor().newInstance();
            this.ipListParser.setCommonProperties(this.props);
        } catch (Throwable t) {
            LOG.error("Initial ipListParser Class {} failure, exit!", this.managerType, t);
            System.exit(6);
        }
    }

    private void chkRequiredFields(String requiredFieldKey) {
        String fieldVal = props.get(requiredFieldKey);
        if (fieldVal == null) {
            LOG.error("Missing mandatory field {} in {}, exit!",
                    requiredFieldKey, COMMON_CONFIG_FILE_NAME);
            System.exit(4);
        }
        if (StringUtils.isBlank(fieldVal)) {
            LOG.error("Required {} field value is blank in {}, exit!",
                    requiredFieldKey, COMMON_CONFIG_FILE_NAME);
            System.exit(5);
        }
    }

    private boolean loadConfigFile() {
        InputStream inStream = null;
        try {
            URL url = getClass().getClassLoader().getResource(COMMON_CONFIG_FILE_NAME);
            inStream = url != null ? url.openStream() : null;
            if (inStream == null) {
                LOG.error("Fail to open {} as the input stream is null, exit!",
                        COMMON_CONFIG_FILE_NAME);
                System.exit(1);
                return false;
            }
            String strKey;
            String strVal;
            Properties tmpProps = new Properties();
            tmpProps.load(inStream);
            props = new HashMap<>(tmpProps.size());
            for (Map.Entry<Object, Object> entry : tmpProps.entrySet()) {
                if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                strKey = (String) entry.getKey();
                strVal = (String) entry.getValue();
                if (StringUtils.isBlank(strKey) || StringUtils.isBlank(strVal)) {
                    continue;
                }
                props.put(strKey.trim(), strVal.trim());
            }
            LOG.info("Read success from {}, content is {}", COMMON_CONFIG_FILE_NAME, props);
        } catch (Throwable e) {
            LOG.error("Fail to load properties from {}, exit!",
                    COMMON_CONFIG_FILE_NAME, e);
            System.exit(2);
            return false;
        } finally {
            if (null != inStream) {
                try {
                    inStream.close();
                } catch (IOException e) {
                    LOG.error("Fail to InputStream.close() for file {}, exit!",
                            COMMON_CONFIG_FILE_NAME, e);
                    System.exit(3);
                }
            }
        }
        return true;
    }
}
