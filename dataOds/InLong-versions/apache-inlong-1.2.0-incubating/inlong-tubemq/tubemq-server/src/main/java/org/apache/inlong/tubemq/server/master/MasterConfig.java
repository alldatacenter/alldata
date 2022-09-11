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

package org.apache.inlong.tubemq.server.master;

import java.util.Set;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.config.TLSConfig;
import org.apache.inlong.tubemq.corebase.utils.AddressUtils;
import org.apache.inlong.tubemq.corebase.utils.MixedUtils;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.corerpc.RpcConstants;
import org.apache.inlong.tubemq.server.common.TServerConstants;
import org.apache.inlong.tubemq.server.common.fileconfig.AbstractFileConfig;
import org.apache.inlong.tubemq.server.common.fileconfig.BdbMetaConfig;
import org.apache.inlong.tubemq.server.common.fileconfig.ZKMetaConfig;
import org.ini4j.Ini;
import org.ini4j.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic config for master service
 */
public class MasterConfig extends AbstractFileConfig {
    private static final Logger logger = LoggerFactory.getLogger(MasterConfig.class);

    private String hostName;
    private int port;
    private int webPort = 8080;
    private TLSConfig tlsConfig;
    private boolean useBdbStoreMetaData = false;
    private ZKMetaConfig zkMetaConfig = null;
    private BdbMetaConfig bdbMetaConfig = null;
    private int consumerBalancePeriodMs = 60 * 1000;
    private int firstBalanceDelayAfterStartMs = 30 * 1000;
    private int consumerHeartbeatTimeoutMs = 30 * 1000;
    private int producerHeartbeatTimeoutMs = 30 * 1000;
    private int brokerHeartbeatTimeoutMs = 30 * 1000;
    private long rpcReadTimeoutMs = RpcConstants.CFG_RPC_READ_TIMEOUT_DEFAULT_MS;
    private long nettyWriteBufferHighWaterMark = 10 * 1024 * 1024;
    private long nettyWriteBufferLowWaterMark = 5 * 1024 * 1024;
    private long onlineOnlyReadToRWPeriodMs = 2 * 60 * 1000;
    private long offlineOnlyReadToRWPeriodMs = 30 * 1000;
    private long stepChgWaitPeriodMs = 12 * 1000;
    private String confModAuthToken = "ASDFGHJKL";
    private String webResourcePath = "../resources";
    private int maxGroupBrokerConsumeRate = 50;
    private int maxGroupRebalanceWaitPeriod = 2;
    private int maxAutoForbiddenCnt = 5;
    private long socketSendBuffer = -1;
    private long socketRecvBuffer = -1;
    private boolean startOffsetResetCheck = false;
    private int rowLockWaitDurMs =
            TServerConstants.CFG_ROWLOCK_DEFAULT_DURATION;
    private boolean startVisitTokenCheck = false;
    private boolean startProduceAuthenticate = false;
    private boolean startProduceAuthorize = false;
    private boolean startConsumeAuthenticate = false;
    private boolean startConsumeAuthorize = false;
    private long visitTokenValidPeriodMs = 5 * 60 * 1000;
    private boolean needBrokerVisitAuth = false;
    private boolean useWebProxy = false;
    private String visitName = "";
    private String visitPassword = "";
    private long authValidTimeStampPeriodMs = TBaseConstants.CFG_DEFAULT_AUTH_TIMESTAMP_VALID_INTERVAL;
    private int rebalanceParallel = 4;
    private long maxMetaForceUpdatePeriodMs = TBaseConstants.CFG_DEF_META_FORCE_UPDATE_PERIOD;

    /**
     * getters
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * Is Transport Layer Security enabled ?
     *
     * @return true if enabled
     */
    public boolean isTlsEnable() {
        return this.tlsConfig.isTlsEnable();
    }

    public int getPort() {
        return port;
    }

    public int getWebPort() {
        return webPort;
    }

    public long getOfflineOnlyReadToRWPeriodMs() {
        return this.offlineOnlyReadToRWPeriodMs;
    }

    public String getConfModAuthToken() {
        return this.confModAuthToken;
    }

    public long getOnlineOnlyReadToRWPeriodMs() {
        return this.onlineOnlyReadToRWPeriodMs;
    }

    public long getStepChgWaitPeriodMs() {
        return this.stepChgWaitPeriodMs;
    }

    public long getRpcReadTimeoutMs() {
        return this.rpcReadTimeoutMs;
    }

    public long getNettyWriteBufferHighWaterMark() {
        return this.nettyWriteBufferHighWaterMark;
    }

    public long getNettyWriteBufferLowWaterMark() {
        return this.nettyWriteBufferLowWaterMark;
    }

    public int getConsumerBalancePeriodMs() {
        return consumerBalancePeriodMs;
    }

    public int getFirstBalanceDelayAfterStartMs() {
        return firstBalanceDelayAfterStartMs;
    }

    public String getWebResourcePath() {
        return webResourcePath;
    }

    public String getMetaDataPath() {
        if (useBdbStoreMetaData) {
            return this.bdbMetaConfig.getMetaDataPath();
        }
        return null;
    }

    /**
     * Setter
     *
     * @param webResourcePath TODO: Have no usage, could be removed?
     */
    public void setWebResourcePath(String webResourcePath) {
        this.webResourcePath = webResourcePath;
    }

    public int getConsumerHeartbeatTimeoutMs() {
        return consumerHeartbeatTimeoutMs;
    }

    public int getProducerHeartbeatTimeoutMs() {
        return producerHeartbeatTimeoutMs;
    }

    public int getBrokerHeartbeatTimeoutMs() {
        return brokerHeartbeatTimeoutMs;
    }

    public int getMaxGroupBrokerConsumeRate() {
        return maxGroupBrokerConsumeRate;
    }

    public boolean isStartOffsetResetCheck() {
        return startOffsetResetCheck;
    }

    public int getMaxGroupRebalanceWaitPeriod() {
        return maxGroupRebalanceWaitPeriod;
    }

    public int getRowLockWaitDurMs() {
        return rowLockWaitDurMs;
    }

    public int getMaxAutoForbiddenCnt() {
        return maxAutoForbiddenCnt;
    }

    public BdbMetaConfig getBdbMetaConfig() {
        return this.bdbMetaConfig;
    }

    public TLSConfig getTlsConfig() {
        return this.tlsConfig;
    }

    public ZKMetaConfig getZkMetaConfig() {
        return zkMetaConfig;
    }

    public boolean isStartVisitTokenCheck() {
        return startVisitTokenCheck;
    }

    public long getVisitTokenValidPeriodMs() {
        return visitTokenValidPeriodMs;
    }

    public boolean isStartProduceAuthenticate() {
        return startProduceAuthenticate;
    }

    public boolean isStartProduceAuthorize() {
        return startProduceAuthorize;
    }

    public boolean isNeedBrokerVisitAuth() {
        return needBrokerVisitAuth;
    }

    public boolean isStartConsumeAuthenticate() {
        return startConsumeAuthenticate;
    }

    public boolean isStartConsumeAuthorize() {
        return startConsumeAuthorize;
    }

    public boolean isUseWebProxy() {
        return useWebProxy;
    }

    public long getSocketSendBuffer() {
        return socketSendBuffer;
    }

    public long getSocketRecvBuffer() {
        return socketRecvBuffer;
    }

    public String getVisitName() {
        return visitName;
    }

    public String getVisitPassword() {
        return visitPassword;
    }

    public long getAuthValidTimeStampPeriodMs() {
        return authValidTimeStampPeriodMs;
    }

    public int getRebalanceParallel() {
        return rebalanceParallel;
    }

    public long getMaxMetaForceUpdatePeriodMs() {
        return maxMetaForceUpdatePeriodMs;
    }

    public boolean isUseBdbStoreMetaData() {
        return useBdbStoreMetaData;
    }

    /**
     * Load file section attributes
     *
     * @param iniConf  the master ini object
     */
    @Override
    protected void loadFileSectAttributes(final Ini iniConf) {
        this.loadSystemConf(iniConf);
        this.loadMetaDataSectConf(iniConf);
        this.tlsConfig = this.loadTlsSectConf(iniConf,
                TBaseConstants.META_DEFAULT_MASTER_TLS_PORT);
        if (this.port == this.webPort
                || (tlsConfig.isTlsEnable() && (this.tlsConfig.getTlsPort() == this.webPort))) {
            throw new IllegalArgumentException(new StringBuilder(512)
                    .append("Illegal field value configuration, the value of ")
                    .append("port or tlsPort cannot be the same as the value of webPort!")
                    .toString());
        }
        if (useBdbStoreMetaData) {
            if (this.port == bdbMetaConfig.getRepNodePort() || (tlsConfig.isTlsEnable()
                    && (this.tlsConfig.getTlsPort() == bdbMetaConfig.getRepNodePort()))) {
                throw new IllegalArgumentException(new StringBuilder(512)
                        .append("Illegal field value configuration, the value of ")
                        .append("port or tlsPort cannot be the same as the value of repNodePort!")
                        .toString());
            }
            if (this.webPort == bdbMetaConfig.getRepNodePort()) {
                throw new IllegalArgumentException(new StringBuilder(512)
                        .append("Illegal field value configuration, the value of ")
                        .append("webPort cannot be the same as the value of repNodePort!")
                        .toString());
            }
        }
    }

    /**
     * Load system config
     *
     * @param iniConf  the master ini object
     */
    // #lizard forgives
    private void loadSystemConf(final Ini iniConf) {
        final Profile.Section masterConf = iniConf.get(SECT_TOKEN_MASTER);
        if (masterConf == null) {
            throw new IllegalArgumentException(new StringBuilder(256)
                    .append(SECT_TOKEN_MASTER).append(" configure section is required!").toString());
        }
        Set<String> configKeySet = masterConf.keySet();
        if (configKeySet.isEmpty()) { /* Should have a least one config item */
            throw new IllegalArgumentException(new StringBuilder(256)
                    .append("Empty configure item in ").append(SECT_TOKEN_MASTER)
                    .append(" section!").toString());
        }

        // port
        this.port = this.getInt(masterConf, "port",
                TBaseConstants.META_DEFAULT_MASTER_PORT);

        // hostname
        if (TStringUtils.isNotBlank(masterConf.get("hostName"))) {
            this.hostName = masterConf.get("hostName").trim();
        } else {
            try {
                this.hostName = AddressUtils.getIPV4LocalAddress();
            } catch (Throwable e) {
                throw new IllegalArgumentException(new StringBuilder(256)
                    .append("Get default master hostName failure : ")
                    .append(e.getMessage()).toString());
            }
        }
        // web port
        if (TStringUtils.isNotBlank(masterConf.get("webPort"))) {
            this.webPort = this.getInt(masterConf, "webPort");
        }

        // web resource path
        if (TStringUtils.isBlank(masterConf.get("webResourcePath"))) {
            throw new IllegalArgumentException(new StringBuilder(256)
                    .append("webResourcePath is null or Blank in ").append(SECT_TOKEN_MASTER)
                    .append(" section!").toString());
        }
        this.webResourcePath = masterConf.get("webResourcePath").trim();

        if (TStringUtils.isNotBlank(masterConf.get("consumerBalancePeriodMs"))) {
            this.consumerBalancePeriodMs =
                    this.getInt(masterConf, "consumerBalancePeriodMs");
        }

        if (TStringUtils.isNotBlank(masterConf.get("firstBalanceDelayAfterStartMs"))) {
            this.firstBalanceDelayAfterStartMs =
                    this.getInt(masterConf, "firstBalanceDelayAfterStartMs");
        }
        if (TStringUtils.isNotBlank(masterConf.get("consumerHeartbeatTimeoutMs"))) {
            this.consumerHeartbeatTimeoutMs =
                    this.getInt(masterConf, "consumerHeartbeatTimeoutMs");
        }
        if (TStringUtils.isNotBlank(masterConf.get("producerHeartbeatTimeoutMs"))) {
            this.producerHeartbeatTimeoutMs =
                    this.getInt(masterConf, "producerHeartbeatTimeoutMs");
        }
        if (TStringUtils.isNotBlank(masterConf.get("brokerHeartbeatTimeoutMs"))) {
            this.brokerHeartbeatTimeoutMs =
                    this.getInt(masterConf, "brokerHeartbeatTimeoutMs");
        }
        if (TStringUtils.isNotBlank(masterConf.get("socketSendBuffer"))) {
            this.socketSendBuffer = this.getLong(masterConf, "socketSendBuffer");
        }
        if (TStringUtils.isNotBlank(masterConf.get("socketRecvBuffer"))) {
            this.socketRecvBuffer = this.getLong(masterConf, "socketRecvBuffer");
        }
        if (TStringUtils.isNotBlank(masterConf.get("rpcReadTimeoutMs"))) {
            this.rpcReadTimeoutMs =
                    this.getLong(masterConf, "rpcReadTimeoutMs");
        }
        if (TStringUtils.isNotBlank(masterConf.get("nettyWriteBufferHighWaterMark"))) {
            this.nettyWriteBufferHighWaterMark =
                    this.getLong(masterConf, "nettyWriteBufferHighWaterMark");
        }
        if (TStringUtils.isNotBlank(masterConf.get("nettyWriteBufferLowWaterMark"))) {
            this.nettyWriteBufferLowWaterMark =
                    this.getLong(masterConf, "nettyWriteBufferLowWaterMark");
        }
        if (TStringUtils.isNotBlank(masterConf.get("onlineOnlyReadToRWPeriodMs"))) {
            this.onlineOnlyReadToRWPeriodMs =
                    this.getLong(masterConf, "onlineOnlyReadToRWPeriodMs");
        }
        if (TStringUtils.isNotBlank(masterConf.get("stepChgWaitPeriodMs"))) {
            this.stepChgWaitPeriodMs =
                    this.getLong(masterConf, "stepChgWaitPeriodMs");
        }
        if (TStringUtils.isNotBlank(masterConf.get("offlineOnlyReadToRWPeriodMs"))) {
            this.offlineOnlyReadToRWPeriodMs =
                    this.getLong(masterConf, "offlineOnlyReadToRWPeriodMs");
        }
        if (TStringUtils.isNotBlank(masterConf.get("confModAuthToken"))) {
            String tmpAuthToken = masterConf.get("confModAuthToken").trim();
            if (tmpAuthToken.length() > TServerConstants.CFG_MODAUTHTOKEN_MAX_LENGTH) {
                throw new IllegalArgumentException(
                        "Invalid value: the length of confModAuthToken's value > "
                                + TServerConstants.CFG_MODAUTHTOKEN_MAX_LENGTH);
            }
            this.confModAuthToken = tmpAuthToken;
        }
        if (TStringUtils.isNotBlank(masterConf.get("maxGroupBrokerConsumeRate"))) {
            this.maxGroupBrokerConsumeRate =
                    this.getInt(masterConf, "maxGroupBrokerConsumeRate");
            if (this.maxGroupBrokerConsumeRate <= 0) {
                throw new IllegalArgumentException(
                        "Invalid value: maxGroupBrokerConsumeRate's value must > 0 !");
            }
        }
        if (TStringUtils.isNotBlank(masterConf.get("maxGroupRebalanceWaitPeriod"))) {
            this.maxGroupRebalanceWaitPeriod =
                    this.getInt(masterConf, "maxGroupRebalanceWaitPeriod");
        }
        if (TStringUtils.isNotBlank(masterConf.get("startOffsetResetCheck"))) {
            this.startOffsetResetCheck =
                    this.getBoolean(masterConf, "startOffsetResetCheck");
        }
        if (TStringUtils.isNotBlank(masterConf.get("rowLockWaitDurMs"))) {
            this.rowLockWaitDurMs =
                    this.getInt(masterConf, "rowLockWaitDurMs");
        }
        if (TStringUtils.isNotBlank(masterConf.get("maxAutoForbiddenCnt"))) {
            this.maxAutoForbiddenCnt =
                    this.getInt(masterConf, "maxAutoForbiddenCnt");
        }
        if (TStringUtils.isNotBlank(masterConf.get("visitTokenValidPeriodMs"))) {
            long tmpPeriodMs = this.getLong(masterConf, "visitTokenValidPeriodMs");
            if (tmpPeriodMs < 3 * 60 * 1000) { /* Min value is 3 min */
                tmpPeriodMs = 3 * 60 * 1000;
            }
            this.visitTokenValidPeriodMs = tmpPeriodMs;
        }
        if (TStringUtils.isNotBlank(masterConf.get("authValidTimeStampPeriodMs"))) {
            long tmpPeriodMs = this.getLong(masterConf, "authValidTimeStampPeriodMs");
            // must between 5,000 ms and 120,000 ms
            this.authValidTimeStampPeriodMs =
                    tmpPeriodMs < 5000 ? 5000 : tmpPeriodMs > 120000 ? 120000 : tmpPeriodMs;
        }
        if (TStringUtils.isNotBlank(masterConf.get("startVisitTokenCheck"))) {
            this.startVisitTokenCheck = this.getBoolean(masterConf, "startVisitTokenCheck");
        }
        if (TStringUtils.isNotBlank(masterConf.get("startProduceAuthenticate"))) {
            this.startProduceAuthenticate = this.getBoolean(masterConf, "startProduceAuthenticate");
        }
        if (TStringUtils.isNotBlank(masterConf.get("startProduceAuthorize"))) {
            this.startProduceAuthorize = this.getBoolean(masterConf, "startProduceAuthorize");
        }
        if (TStringUtils.isNotBlank(masterConf.get("useWebProxy"))) {
            this.useWebProxy = this.getBoolean(masterConf, "useWebProxy");
        }
        if (!this.startProduceAuthenticate && this.startProduceAuthorize) {
            throw new IllegalArgumentException(
                    "startProduceAuthenticate must set true if startProduceAuthorize is true!");
        }
        if (TStringUtils.isNotBlank(masterConf.get("startConsumeAuthenticate"))) {
            this.startConsumeAuthenticate = this.getBoolean(masterConf, "startConsumeAuthenticate");
        }
        if (TStringUtils.isNotBlank(masterConf.get("startConsumeAuthorize"))) {
            this.startConsumeAuthorize = this.getBoolean(masterConf, "startConsumeAuthorize");
        }
        if (!this.startConsumeAuthenticate && this.startConsumeAuthorize) {
            throw new IllegalArgumentException(
                    "startConsumeAuthenticate must set true if startConsumeAuthorize is true!");
        }
        if (TStringUtils.isNotBlank(masterConf.get("needBrokerVisitAuth"))) {
            this.needBrokerVisitAuth = this.getBoolean(masterConf, "needBrokerVisitAuth");
        }
        if (this.needBrokerVisitAuth) {
            if (TStringUtils.isBlank(masterConf.get("visitName"))) {
                throw new IllegalArgumentException(new StringBuilder(256)
                        .append("visitName is null or Blank in ").append(SECT_TOKEN_BROKER)
                        .append(" section!").toString());
            }
            if (TStringUtils.isBlank(masterConf.get("visitPassword"))) {
                throw new IllegalArgumentException(new StringBuilder(256)
                        .append("visitPassword is null or Blank in ").append(SECT_TOKEN_BROKER)
                        .append(" section!").toString());
            }
            this.visitName = masterConf.get("visitName").trim();
            this.visitPassword = masterConf.get("visitPassword").trim();
        }
        if (TStringUtils.isNotBlank(masterConf.get("rebalanceParallel"))) {
            int tmpParallel = this.getInt(masterConf, "rebalanceParallel");
            this.rebalanceParallel = MixedUtils.mid(tmpParallel, 1, 20);
        }
        if (TStringUtils.isNotBlank(masterConf.get("maxMetaForceUpdatePeriodMs"))) {
            long tmpPeriodMs = this.getLong(masterConf, "maxMetaForceUpdatePeriodMs");
            if (tmpPeriodMs < TBaseConstants.CFG_MIN_META_FORCE_UPDATE_PERIOD) {
                tmpPeriodMs = TBaseConstants.CFG_MIN_META_FORCE_UPDATE_PERIOD;
            }
            this.maxMetaForceUpdatePeriodMs = tmpPeriodMs;
        }
    }

    /**
     * Load meta-data section config
     *
     * @param iniConf  the master ini object
     */
    private void loadMetaDataSectConf(final Ini iniConf) {
        if (iniConf.get(SECT_TOKEN_META_BDB) != null
                && iniConf.get(SECT_TOKEN_META_ZK) != null) {
            throw new IllegalArgumentException(new StringBuilder(256)
                    .append("Cannot configure both ").append(SECT_TOKEN_META_BDB).append(" and ")
                    .append(SECT_TOKEN_META_ZK).append(" meta-data sections in the same time")
                    .append(", please confirm them and retain one first!").toString());
        }
        Profile.Section metaSect = iniConf.get(SECT_TOKEN_META_ZK);
        if (metaSect != null) {
            this.useBdbStoreMetaData = false;
            this.zkMetaConfig = loadZkMetaSectConf(iniConf);
            return;
        }
        metaSect = iniConf.get(SECT_TOKEN_META_BDB);
        if (metaSect != null) {
            this.useBdbStoreMetaData = true;
            this.bdbMetaConfig = loadBdbMetaSectConf(iniConf);
            return;
        }
        metaSect = iniConf.get(SECT_TOKEN_REPLICATION);
        if (metaSect != null) {
            this.useBdbStoreMetaData = true;
            this.bdbMetaConfig = loadReplicationSectConf(iniConf);
            return;
        }
        metaSect = iniConf.get(SECT_TOKEN_BDB);
        if (metaSect != null) {
            this.useBdbStoreMetaData = true;
            this.bdbMetaConfig = loadBdbStoreSectConf(iniConf);
            return;
        }
        throw new IllegalArgumentException(new StringBuilder(256)
                .append("Missing necessary meta-data section, please select ")
                .append(SECT_TOKEN_META_ZK).append(" or ").append(SECT_TOKEN_META_BDB)
                .append(" and configure ini again!").toString());
    }

    /**
     * Load ZooKeeper store section configure as meta-data storage
     *
     * @param iniConf  the master ini object
     * @return   the configured information
     */

    private ZKMetaConfig loadZkMetaSectConf(final Ini iniConf) {
        final Profile.Section zkeeperSect = iniConf.get(SECT_TOKEN_META_ZK);
        if (zkeeperSect == null) {
            throw new IllegalArgumentException(new StringBuilder(256)
                    .append(SECT_TOKEN_META_ZK).append(" configure section is required!").toString());
        }
        Set<String> configKeySet = zkeeperSect.keySet();
        if (configKeySet.isEmpty()) {
            throw new IllegalArgumentException(new StringBuilder(256)
                    .append("Empty configure item in ").append(SECT_TOKEN_META_ZK)
                    .append(" section!").toString());
        }
        ZKMetaConfig zkMetaConfig = new ZKMetaConfig();
        if (TStringUtils.isNotBlank(zkeeperSect.get("zkServerAddr"))) {
            zkMetaConfig.setZkServerAddr(zkeeperSect.get("zkServerAddr").trim());
        }
        if (TStringUtils.isNotBlank(zkeeperSect.get("zkNodeRoot"))) {
            zkMetaConfig.setZkNodeRoot(zkeeperSect.get("zkNodeRoot").trim());
        }
        if (TStringUtils.isNotBlank(zkeeperSect.get("zkSessionTimeoutMs"))) {
            zkMetaConfig.setZkSessionTimeoutMs(getInt(zkeeperSect, "zkSessionTimeoutMs"));
        }
        if (TStringUtils.isNotBlank(zkeeperSect.get("zkConnectionTimeoutMs"))) {
            zkMetaConfig.setZkConnectionTimeoutMs(getInt(zkeeperSect, "zkConnectionTimeoutMs"));
        }
        if (TStringUtils.isNotBlank(zkeeperSect.get("zkSyncTimeMs"))) {
            zkMetaConfig.setZkSyncTimeMs(getInt(zkeeperSect, "zkSyncTimeMs"));
        }
        if (TStringUtils.isNotBlank(zkeeperSect.get("zkCommitPeriodMs"))) {
            zkMetaConfig.setZkCommitPeriodMs(getLong(zkeeperSect, "zkCommitPeriodMs"));
        }
        if (TStringUtils.isNotBlank(zkeeperSect.get("zkCommitFailRetries"))) {
            zkMetaConfig.setZkCommitFailRetries(getInt(zkeeperSect, "zkCommitFailRetries"));
        }
        if (TStringUtils.isNotBlank(zkeeperSect.get("zkMasterCheckPeriodMs"))) {
            zkMetaConfig.setZkMasterCheckPeriodMs(getInt(zkeeperSect, "zkMasterCheckPeriodMs"));
        }
        return zkMetaConfig;
    }

    /**
     * Load Berkeley DB store section configure as meta-data storage
     *
     * @param iniConf  the master ini object
     * @return   the configured information
     */
    private BdbMetaConfig loadBdbMetaSectConf(final Ini iniConf) {
        final Profile.Section repSect = iniConf.get(SECT_TOKEN_META_BDB);
        if (repSect == null) {
            return null;
        }
        Set<String> configKeySet = repSect.keySet();
        if (configKeySet.isEmpty()) {
            throw new IllegalArgumentException(new StringBuilder(256)
                    .append("Empty configure item in ").append(SECT_TOKEN_META_BDB)
                    .append(" section!").toString());
        }
        BdbMetaConfig tmpMetaConfig = new BdbMetaConfig();
        // read configure items
        if (TStringUtils.isNotBlank(repSect.get("repGroupName"))) {
            tmpMetaConfig.setRepGroupName(repSect.get("repGroupName").trim());
        }
        if (TStringUtils.isBlank(repSect.get("repNodeName"))) {
            getSimilarConfigField(SECT_TOKEN_META_BDB, configKeySet, "repNodeName");
        } else {
            tmpMetaConfig.setRepNodeName(repSect.get("repNodeName").trim());
        }
        if (TStringUtils.isNotBlank(repSect.get("repNodePort"))) {
            tmpMetaConfig.setRepNodePort(getInt(repSect, "repNodePort"));
        }
        if (TStringUtils.isNotBlank(repSect.get("metaDataPath"))) {
            tmpMetaConfig.setMetaDataPath(repSect.get("metaDataPath").trim());
        }
        if (TStringUtils.isNotBlank(repSect.get("repHelperHost"))) {
            tmpMetaConfig.setRepHelperHost(repSect.get("repHelperHost").trim());
        }
        if (TStringUtils.isNotBlank(repSect.get("metaLocalSyncPolicy"))) {
            tmpMetaConfig.setMetaLocalSyncPolicy(getInt(repSect, "metaLocalSyncPolicy"));
        }
        if (TStringUtils.isNotBlank(repSect.get("metaReplicaSyncPolicy"))) {
            tmpMetaConfig.setMetaReplicaSyncPolicy(getInt(repSect, "metaReplicaSyncPolicy"));
        }
        if (TStringUtils.isNotBlank(repSect.get("repReplicaAckPolicy"))) {
            tmpMetaConfig.setRepReplicaAckPolicy(getInt(repSect, "repReplicaAckPolicy"));
        }
        if (TStringUtils.isNotBlank(repSect.get("repStatusCheckTimeoutMs"))) {
            tmpMetaConfig.setRepStatusCheckTimeoutMs(getLong(repSect, "repStatusCheckTimeoutMs"));
        }
        return tmpMetaConfig;
    }

    /**
     * Deprecated: Load Berkeley DB store section config
     * Just keep `loadBdbStoreSectConf` for backward compatibility
     *
     * @param iniConf  the master ini object
     * @return   the configured information
     */
    private BdbMetaConfig loadBdbStoreSectConf(final Ini iniConf) {
        final Profile.Section bdbSect = iniConf.get(SECT_TOKEN_BDB);
        if (bdbSect == null) {
            return null;
        }
        Set<String> configKeySet = bdbSect.keySet();
        if (configKeySet.isEmpty()) {
            throw new IllegalArgumentException(new StringBuilder(256)
                    .append("Empty configure item in ").append(SECT_TOKEN_BDB)
                    .append(" section!").toString());
        }
        logger.warn("[bdbStore] section is deprecated. Please config in [meta_bdb] section.");
        // read configure items
        BdbMetaConfig tmpMetaConfig = new BdbMetaConfig();
        if (TStringUtils.isBlank(bdbSect.get("bdbRepGroupName"))) {
            getSimilarConfigField(SECT_TOKEN_BDB, configKeySet, "bdbRepGroupName");
        } else {
            tmpMetaConfig.setRepGroupName(bdbSect.get("bdbRepGroupName").trim());
        }
        if (TStringUtils.isBlank(bdbSect.get("bdbNodeName"))) {
            getSimilarConfigField(SECT_TOKEN_BDB, configKeySet, "bdbNodeName");
        } else {
            tmpMetaConfig.setRepNodeName(bdbSect.get("bdbNodeName").trim());
        }
        if (TStringUtils.isNotBlank(bdbSect.get("bdbNodePort"))) {
            tmpMetaConfig.setRepNodePort(getInt(bdbSect, "bdbNodePort"));
        }
        if (TStringUtils.isBlank(bdbSect.get("bdbEnvHome"))) {
            getSimilarConfigField(SECT_TOKEN_BDB, configKeySet, "bdbEnvHome");
        } else {
            tmpMetaConfig.setMetaDataPath(bdbSect.get("bdbEnvHome").trim());
        }
        if (TStringUtils.isBlank(bdbSect.get("bdbHelperHost"))) {
            getSimilarConfigField(SECT_TOKEN_BDB, configKeySet, "bdbHelperHost");
        } else {
            tmpMetaConfig.setRepHelperHost(bdbSect.get("bdbHelperHost").trim());
        }
        if (TStringUtils.isNotBlank(bdbSect.get("bdbLocalSync"))) {
            tmpMetaConfig.setMetaLocalSyncPolicy(getInt(bdbSect, "bdbLocalSync"));
        }
        if (TStringUtils.isNotBlank(bdbSect.get("bdbReplicaSync"))) {
            tmpMetaConfig.setMetaReplicaSyncPolicy(getInt(bdbSect, "bdbReplicaSync"));
        }
        if (TStringUtils.isNotBlank(bdbSect.get("bdbReplicaAck"))) {
            tmpMetaConfig.setRepReplicaAckPolicy(getInt(bdbSect, "bdbReplicaAck"));
        }
        if (TStringUtils.isNotBlank(bdbSect.get("bdbStatusCheckTimeoutMs"))) {
            tmpMetaConfig.setRepStatusCheckTimeoutMs(getLong(bdbSect, "bdbStatusCheckTimeoutMs"));
        }
        return tmpMetaConfig;
    }

    /**
     * Deprecated: Load Berkeley DB store section config
     * Just keep `loadReplicationSectConf` for backward compatibility
     *
     * @param iniConf  the master ini object
     * @return   the configured information
     */
    private BdbMetaConfig loadReplicationSectConf(final Ini iniConf) {
        final Profile.Section repSect = iniConf.get(SECT_TOKEN_REPLICATION);
        if (repSect == null) {
            return null;
        }
        Set<String> configKeySet = repSect.keySet();
        if (configKeySet.isEmpty()) {
            throw new IllegalArgumentException(new StringBuilder(256)
                    .append("Empty configure item in ").append(SECT_TOKEN_REPLICATION)
                    .append(" section!").toString());
        }
        BdbMetaConfig tmpMetaConfig = new BdbMetaConfig();
        logger.warn("[replication] section is deprecated. Please config in [meta_bdb] section.");
        // read configure items
        if (TStringUtils.isNotBlank(repSect.get("repGroupName"))) {
            tmpMetaConfig.setRepGroupName(repSect.get("repGroupName").trim());
        }
        if (TStringUtils.isBlank(repSect.get("repNodeName"))) {
            getSimilarConfigField(SECT_TOKEN_REPLICATION, configKeySet, "repNodeName");
        } else {
            tmpMetaConfig.setRepNodeName(repSect.get("repNodeName").trim());
        }
        if (TStringUtils.isNotBlank(repSect.get("repNodePort"))) {
            tmpMetaConfig.setRepNodePort(getInt(repSect, "repNodePort"));
        }
        // meta data path
        final Profile.Section masterConf = iniConf.get(SECT_TOKEN_MASTER);
        if (TStringUtils.isNotBlank(masterConf.get("metaDataPath"))) {
            tmpMetaConfig.setMetaDataPath(masterConf.get("metaDataPath").trim());
        }
        if (TStringUtils.isNotBlank(repSect.get("repHelperHost"))) {
            tmpMetaConfig.setRepHelperHost(repSect.get("repHelperHost").trim());
        }
        if (TStringUtils.isNotBlank(repSect.get("metaLocalSyncPolicy"))) {
            tmpMetaConfig.setMetaLocalSyncPolicy(getInt(repSect, "metaLocalSyncPolicy"));
        }
        if (TStringUtils.isNotBlank(repSect.get("metaReplicaSyncPolicy"))) {
            tmpMetaConfig.setMetaReplicaSyncPolicy(getInt(repSect, "metaReplicaSyncPolicy"));
        }
        if (TStringUtils.isNotBlank(repSect.get("repReplicaAckPolicy"))) {
            tmpMetaConfig.setRepReplicaAckPolicy(getInt(repSect, "repReplicaAckPolicy"));
        }
        if (TStringUtils.isNotBlank(repSect.get("repStatusCheckTimeoutMs"))) {
            tmpMetaConfig.setRepStatusCheckTimeoutMs(getLong(repSect, "repStatusCheckTimeoutMs"));
        }
        return tmpMetaConfig;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("hostName", hostName)
                .append("port", port)
                .append("webPort", webPort)
                .append("tlsConfig", tlsConfig)
                .append("useBdbStoreMetaData", useBdbStoreMetaData)
                .append("zkMetaConfig", zkMetaConfig)
                .append("bdbMetaConfig", bdbMetaConfig)
                .append("consumerBalancePeriodMs", consumerBalancePeriodMs)
                .append("firstBalanceDelayAfterStartMs", firstBalanceDelayAfterStartMs)
                .append("consumerHeartbeatTimeoutMs", consumerHeartbeatTimeoutMs)
                .append("producerHeartbeatTimeoutMs", producerHeartbeatTimeoutMs)
                .append("brokerHeartbeatTimeoutMs", brokerHeartbeatTimeoutMs)
                .append("rpcReadTimeoutMs", rpcReadTimeoutMs)
                .append("nettyWriteBufferHighWaterMark", nettyWriteBufferHighWaterMark)
                .append("nettyWriteBufferLowWaterMark", nettyWriteBufferLowWaterMark)
                .append("onlineOnlyReadToRWPeriodMs", onlineOnlyReadToRWPeriodMs)
                .append("offlineOnlyReadToRWPeriodMs", offlineOnlyReadToRWPeriodMs)
                .append("stepChgWaitPeriodMs", stepChgWaitPeriodMs)
                .append("confModAuthToken", confModAuthToken)
                .append("webResourcePath", webResourcePath)
                .append("maxGroupBrokerConsumeRate", maxGroupBrokerConsumeRate)
                .append("maxGroupRebalanceWaitPeriod", maxGroupRebalanceWaitPeriod)
                .append("maxAutoForbiddenCnt", maxAutoForbiddenCnt)
                .append("socketSendBuffer", socketSendBuffer)
                .append("socketRecvBuffer", socketRecvBuffer)
                .append("startOffsetResetCheck", startOffsetResetCheck)
                .append("rowLockWaitDurMs", rowLockWaitDurMs)
                .append("startVisitTokenCheck", startVisitTokenCheck)
                .append("startProduceAuthenticate", startProduceAuthenticate)
                .append("startProduceAuthorize", startProduceAuthorize)
                .append("startConsumeAuthenticate", startConsumeAuthenticate)
                .append("startConsumeAuthorize", startConsumeAuthorize)
                .append("visitTokenValidPeriodMs", visitTokenValidPeriodMs)
                .append("needBrokerVisitAuth", needBrokerVisitAuth)
                .append("useWebProxy", useWebProxy)
                .append("visitName", visitName)
                .append("visitPassword", visitPassword)
                .append("authValidTimeStampPeriodMs", authValidTimeStampPeriodMs)
                .append("rebalanceParallel", rebalanceParallel)
                .append("maxMetaForceUpdatePeriodMs", maxMetaForceUpdatePeriodMs)
                .toString();
    }
}
