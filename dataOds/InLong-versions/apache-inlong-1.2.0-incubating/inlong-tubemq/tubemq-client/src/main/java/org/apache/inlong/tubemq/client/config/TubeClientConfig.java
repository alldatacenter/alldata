/*
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

package org.apache.inlong.tubemq.client.config;

import org.apache.inlong.tubemq.client.common.StatsConfig;
import org.apache.inlong.tubemq.client.common.StatsLevel;
import org.apache.inlong.tubemq.client.common.TClientConstants;
import org.apache.inlong.tubemq.corebase.cluster.MasterInfo;
import org.apache.inlong.tubemq.corebase.config.TLSConfig;
import org.apache.inlong.tubemq.corebase.utils.AddressUtils;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.corerpc.RpcConstants;

/**
 * Configuration of the Tube client.
 */
public class TubeClientConfig {
    // Master information.
    private final MasterInfo masterInfo;
    // Rpc read time out.
    private long rpcReadTimeoutMs = RpcConstants.CFG_RPC_READ_TIMEOUT_DEFAULT_MS;
    // Rpc connection processor number.
    private int rpcConnProcessorCnt = RpcConstants.CFG_DEFAULT_CLIENT_WORKER_COUNT;
    // Netty memory size.
    private int rpcNettyWorkMemorySize = RpcConstants.CFG_DEFAULT_TOTAL_MEM_SIZE;
    // The size of the thread pool, which handles the call back response.
    private int rpcRspCallBackThreadCnt = RpcConstants.CFG_DEFAULT_RSP_CALLBACK_WORKER_COUNT;
    // High watermark of the netty write buffer.
    private long nettyWriteBufferHighWaterMark = RpcConstants.CFG_DEFAULT_NETTY_WRITEBUFFER_HIGH_MARK;
    // Low watermark of the netty write buffer.
    private long nettyWriteBufferLowWaterMark = RpcConstants.CFG_DEFAULT_NETTY_WRITEBUFFER_LOW_MARK;
    // Max register retry times.
    private int maxRegisterRetryTimes = TClientConstants.CFG_DEFAULT_REGISTER_RETRY_TIMES;
    // Wait time threshold to consider a registration as failed.
    private long regFailWaitPeriodMs = TClientConstants.CFG_DEFAULT_REGFAIL_WAIT_PERIOD_MS;
    // Max heartbeat retry number.
    private int maxHeartBeatRetryTimes = TClientConstants.CFG_DEFAULT_HEARTBEAT_RETRY_TIMES;
    // Heartbeat period in ms.
    private long heartbeatPeriodMs = TClientConstants.CFG_DEFAULT_HEARTBEAT_PERIOD_MS;
    // Heartbeat period after failure happened.
    private long heartbeatPeriodAfterFail = TClientConstants.CFG_DEFAULT_HEARTBEAT_PERIOD_AFTER_RETRY_FAIL;
    // Link statistic check duration in ms.
    private long linkStatsDurationMs = RpcConstants.CFG_LQ_STATS_DURATION_MS;
    // statistics setting
    private final StatsConfig statsConfig = new StatsConfig();

    // The following 5 configuration parameters are used in broker exception process.
    //
    // If the failure count of a broker meets linkStatsMaxAllowedFailTimes in a
    // linkStatsForbiddenDurationMs duration. The client will check the current total connection
    // number. If the blocking connection number / total connection number is lower than
    // linkStatsMaxForbiddenRate, the client will block the broker.
    //
    // After established the connection, the client will check the connection quality based on the
    // broker response status. If the client confirm the connection is in bad quality during
    // maxForbiddenCheckDuration, and the exception connection / total connection is lower than
    // maxSentForbiddenRate, the client will blocking the connection.
    private long linkStatsForbiddenDurationMs = RpcConstants.CFG_LQ_FORBIDDEN_DURATION_MS;
    private int linkStatsMaxAllowedFailTimes = RpcConstants.CFG_LQ_MAX_ALLOWED_FAIL_COUNT;
    private double linkStatsMaxForbiddenRate = RpcConstants.CFG_LQ_MAX_FAIL_FORBIDDEN_RATE;
    private long unAvailableFbdDurationMs = RpcConstants.CFG_UNAVAILABLE_FORBIDDEN_DURATION_MS;
    private double maxSentForbiddenRate = 0.15;
    private long maxForbiddenCheckDuration = 30000;

    // Check duration of session statistic.
    private long sessionStatisticCheckDuration = 1000;
    // Warn when the forbidden rate of a session meet this threshold.
    private double sessionWarnForbiddenRate = 0.20;
    // Warn when the number of delayed messages in a session meets this count.
    private long sessionWarnDelayedMsgCount = 300000;
    // Max allowed delayed message number in a link.
    private long linkMaxAllowedDelayedMsgCount = 50000;
    // Max allowed delayed message number in a session.
    private long sessionMaxAllowedDelayedMsgCount = 500000;
    // Enable user auth.
    private boolean enableUserAuthentic = false;
    // User name.
    private String usrName = "";
    // User password.
    private String usrPassWord = "";
    // TLS configuration.
    private TLSConfig tlsConfig = new TLSConfig();

    public TubeClientConfig(String masterAddrInfo) {
        this(new MasterInfo(masterAddrInfo));
    }

    public TubeClientConfig(MasterInfo masterInfo) {
        if (masterInfo == null) {
            throw new IllegalArgumentException("Illegal parameter: masterAddrInfo is null!");
        }
        this.masterInfo = masterInfo.clone();
        AddressUtils.getIPV4LocalAddress();
    }

    @Deprecated
    public TubeClientConfig(final String localHostIP, final String masterAddrInfo) {
        this(localHostIP, new MasterInfo(masterAddrInfo));
    }

    @Deprecated
    public TubeClientConfig(final String localHostIP, final MasterInfo masterInfo) {
        if (TStringUtils.isBlank(localHostIP)) {
            throw new IllegalArgumentException("Illegal parameter: localHostIP is blank!");
        }
        if ("127.0.0.1".equals(localHostIP)) {
            throw new IllegalArgumentException("Illegal parameter: localHostIP can't set to 127.0.0.1");
        }
        if (masterInfo == null) {
            throw new IllegalArgumentException("Illegal parameter: masterAddrInfo is null!");
        }
        this.masterInfo = masterInfo.clone();
        AddressUtils.validLocalIp(localHostIP.trim());
    }

    public MasterInfo getMasterInfo() {
        return masterInfo;
    }

    public long getRpcTimeoutMs() {
        return this.rpcReadTimeoutMs;
    }

    /**
     * Set RPC read timeout. Please notice that the value should be between
     * RpcConstants.CFG_RPC_READ_TIMEOUT_MAX_MS and RpcConstants.CFG_RPC_READ_TIMEOUT_MIN_MS.
     *
     * @param rpcReadTimeoutMs rpc read timeout in ms.
     */
    public void setRpcTimeoutMs(long rpcReadTimeoutMs) {
        if (rpcReadTimeoutMs >= RpcConstants.CFG_RPC_READ_TIMEOUT_MAX_MS) {
            this.rpcReadTimeoutMs = RpcConstants.CFG_RPC_READ_TIMEOUT_MAX_MS;
        } else if (rpcReadTimeoutMs <= RpcConstants.CFG_RPC_READ_TIMEOUT_MIN_MS) {
            this.rpcReadTimeoutMs = RpcConstants.CFG_RPC_READ_TIMEOUT_MIN_MS;
        } else {
            this.rpcReadTimeoutMs = rpcReadTimeoutMs;
        }
    }

    public long getHeartbeatPeriodMs() {
        return heartbeatPeriodMs;
    }

    public void setHeartbeatPeriodMs(long heartbeatPeriodMs) {
        this.heartbeatPeriodMs = heartbeatPeriodMs;
    }

    public int getRpcConnProcessorCnt() {
        return this.rpcConnProcessorCnt;
    }

    public void setRpcConnProcessorCnt(int rpcConnProcessorCnt) {
        if (rpcConnProcessorCnt <= 0) {
            this.rpcConnProcessorCnt = RpcConstants.CFG_DEFAULT_CLIENT_WORKER_COUNT;
        } else {
            this.rpcConnProcessorCnt = rpcConnProcessorCnt;
        }
    }

    public long getUnAvailableFbdDurationMs() {
        return unAvailableFbdDurationMs;
    }

    public void setUnAvailableFbdDurationMs(long unAvailableFbdDurationMs) {
        this.unAvailableFbdDurationMs = unAvailableFbdDurationMs;
    }

    public int getRpcNettyWorkMemorySize() {
        return rpcNettyWorkMemorySize;
    }

    /**
     * Set the netty work memory size. Please notice that the value must be larger than 0.
     *
     * @param rpcNettyWorkMemorySize netty work memory size.
     */
    public void setRpcNettyWorkMemorySize(int rpcNettyWorkMemorySize) {
        if (rpcNettyWorkMemorySize <= 0) {
            this.rpcNettyWorkMemorySize = RpcConstants.CFG_DEFAULT_TOTAL_MEM_SIZE;
        } else {
            this.rpcNettyWorkMemorySize = rpcNettyWorkMemorySize;
        }
    }

    public int getRpcRspCallBackThreadCnt() {
        return rpcRspCallBackThreadCnt;
    }

    public void setRpcRspCallBackThreadCnt(int rpcRspCallBackThreadCnt) {
        if (rpcRspCallBackThreadCnt <= 0) {
            this.rpcRspCallBackThreadCnt = RpcConstants.CFG_DEFAULT_RSP_CALLBACK_WORKER_COUNT;
        } else {
            this.rpcRspCallBackThreadCnt = rpcRspCallBackThreadCnt;
        }
    }

    public long getNettyWriteBufferHighWaterMark() {
        return this.nettyWriteBufferHighWaterMark;
    }

    public void setNettyWriteBufferHighWaterMark(long nettyWriteBufferHighWaterMark) {
        if (nettyWriteBufferHighWaterMark >= Integer.MAX_VALUE) {
            this.nettyWriteBufferHighWaterMark = Integer.MAX_VALUE - 1;
        } else if (nettyWriteBufferHighWaterMark <= 0) {
            this.nettyWriteBufferHighWaterMark =
                    RpcConstants.CFG_DEFAULT_NETTY_WRITEBUFFER_HIGH_MARK;
        } else {
            this.nettyWriteBufferHighWaterMark =
                    nettyWriteBufferHighWaterMark;
        }
    }

    public long getNettyWriteBufferLowWaterMark() {
        return this.nettyWriteBufferLowWaterMark;
    }

    /**
     * Set netty write buffer low water mark. Please notice this value must be between
     * 0 and Integer.MAX_VALUE.
     *
     * @param nettyWriteBufferLowWaterMark netty write buffer water mark.
     */
    public void setNettyWriteBufferLowWaterMark(long nettyWriteBufferLowWaterMark) {
        if (nettyWriteBufferLowWaterMark >= Integer.MAX_VALUE) {
            this.nettyWriteBufferLowWaterMark =
                    Integer.MAX_VALUE - 1;
        } else if (nettyWriteBufferLowWaterMark <= 0) {
            this.nettyWriteBufferLowWaterMark =
                    RpcConstants.CFG_DEFAULT_NETTY_WRITEBUFFER_LOW_MARK;
        } else {
            this.nettyWriteBufferLowWaterMark =
                    nettyWriteBufferLowWaterMark;
        }
    }

    public int getMaxRegisterRetryTimes() {
        return maxRegisterRetryTimes;
    }

    public void setMaxRegisterRetryTimes(int maxRegisterRetryTimes) {
        this.maxRegisterRetryTimes = maxRegisterRetryTimes;
    }

    public long getRegFailWaitPeriodMs() {
        return regFailWaitPeriodMs;
    }

    public void setRegFailWaitPeriodMs(long regFailWaitPeriodMs) {
        this.regFailWaitPeriodMs = regFailWaitPeriodMs;
    }

    public int getMaxHeartBeatRetryTimes() {
        return maxHeartBeatRetryTimes;
    }

    public void setMaxHeartBeatRetryTimes(int maxHeartBeatRetryTimes) {
        this.maxHeartBeatRetryTimes = maxHeartBeatRetryTimes;
    }

    public long getHeartbeatPeriodAfterFail() {
        return heartbeatPeriodAfterFail;
    }

    public void setHeartbeatPeriodAfterFail(long heartbeatPeriodAfterFail) {
        this.heartbeatPeriodAfterFail = heartbeatPeriodAfterFail;
    }

    public long getLinkStatsDurationMs() {
        return linkStatsDurationMs;
    }

    public void setLinkStatsDurationMs(long linkStatsDurationMs) {
        this.linkStatsDurationMs = linkStatsDurationMs;
    }

    public long getLinkStatsForbiddenDurationMs() {
        return linkStatsForbiddenDurationMs;
    }

    public void setLinkStatsForbiddenDurationMs(long linkStatsForbiddenDurationMs) {
        this.linkStatsForbiddenDurationMs = linkStatsForbiddenDurationMs;
    }

    public int getLinkStatsMaxAllowedFailTimes() {
        return linkStatsMaxAllowedFailTimes;
    }

    public void setLinkStatsMaxAllowedFailTimes(int linkStatsMaxAllowedFailTimes) {
        this.linkStatsMaxAllowedFailTimes = linkStatsMaxAllowedFailTimes;
    }

    public double getLinkStatsMaxForbiddenRate() {
        return linkStatsMaxForbiddenRate;
    }

    public void setLinkStatsMaxForbiddenRate(double linkStatsMaxForbiddenRate) {
        this.linkStatsMaxForbiddenRate = linkStatsMaxForbiddenRate;
    }

    public double getMaxSentForbiddenRate() {
        return this.maxSentForbiddenRate;
    }

    public void setMaxSentForbiddenRate(double maxSentForbiddenRate) {
        this.maxSentForbiddenRate = maxSentForbiddenRate;
    }

    public long getMaxForbiddenCheckDuration() {
        return this.maxForbiddenCheckDuration;
    }

    public void setMaxForbiddenCheckDuration(long maxForbiddenCheckDuration) {
        this.maxForbiddenCheckDuration = maxForbiddenCheckDuration;
    }

    public long getSessionStatisticCheckDuration() {
        return sessionStatisticCheckDuration;
    }

    public void setSessionStatisticCheckDuration(long sessionStatisticCheckDuration) {
        this.sessionStatisticCheckDuration = sessionStatisticCheckDuration;
    }

    public double getSessionWarnForbiddenRate() {
        return sessionWarnForbiddenRate;
    }

    public void setSessionWarnForbiddenRate(double sessionWarnForbiddenRate) {
        this.sessionWarnForbiddenRate = sessionWarnForbiddenRate;
    }

    public long getSessionWarnDelayedMsgCount() {
        return sessionWarnDelayedMsgCount;
    }

    public void setSessionWarnDelayedMsgCount(long sessionWarnDelayedMsgCount) {
        this.sessionWarnDelayedMsgCount = sessionWarnDelayedMsgCount;
    }

    public long getLinkMaxAllowedDelayedMsgCount() {
        return linkMaxAllowedDelayedMsgCount;
    }

    public void setLinkMaxAllowedDelayedMsgCount(long linkMaxAllowedDelayedMsgCount) {
        this.linkMaxAllowedDelayedMsgCount = linkMaxAllowedDelayedMsgCount;
    }

    public long getSessionMaxAllowedDelayedMsgCount() {
        return sessionMaxAllowedDelayedMsgCount;
    }

    public void setSessionMaxAllowedDelayedMsgCount(long sessionMaxAllowedDelayedMsgCount) {
        this.sessionMaxAllowedDelayedMsgCount = sessionMaxAllowedDelayedMsgCount;
    }

    /**
     * Set authenticate information
     *
     * @param needAuthentic   enable or disable authentication
     * @param usrName         the user name
     * @param usrPassWord     the password
     */
    public void setAuthenticInfo(boolean needAuthentic,
                                 String usrName,
                                 String usrPassWord) {
        if (needAuthentic) {
            if (TStringUtils.isBlank(usrName)) {
                throw new IllegalArgumentException("Illegal parameter: usrName is Blank!");
            }
            if (TStringUtils.isBlank(usrPassWord)) {
                throw new IllegalArgumentException("Illegal parameter: usrPassWord is Blank!");
            }
        }
        this.enableUserAuthentic = needAuthentic;
        if (this.enableUserAuthentic) {
            this.usrName = usrName;
            this.usrPassWord = usrPassWord;
        } else {
            this.usrName = "";
            this.usrPassWord = "";
        }
    }

    /**
     * Set TLS information
     *
     * @param trustStorePath        the trusted store path
     * @param trustStorePassword    the trusted store password
     */
    public void setTLSEnableInfo(String trustStorePath, String trustStorePassword) {
        // public void setTLSEnableInfo(String trustStorePath, String trustStorePassword,
        // boolean tlsTwoWayAuthEnable,String keyStorePath, String keyStorePassword) throws Exception {
        if (TStringUtils.isBlank(trustStorePath)) {
            throw new IllegalArgumentException("Illegal parameter: trustStorePath is Blank!");
        }
        if (TStringUtils.isBlank(trustStorePassword)) {
            throw new IllegalArgumentException("Illegal parameter: trustStorePassword is Blank!");
        }
        this.tlsConfig.setTlsEnable(true);
        this.tlsConfig.setTlsTrustStorePath(trustStorePath);
        this.tlsConfig.setTlsTrustStorePassword(trustStorePassword);
        this.tlsConfig.setTlsTwoWayAuthEnable(false);
        /*
        if (tlsTwoWayAuthEnable) {
            if (TStringUtils.isBlank(keyStorePath)) {
                throw new Exception("Illegal parameter: keyStorePath is Blank!");
            }
            if (TStringUtils.isBlank(keyStorePassword)) {
                throw new Exception("Illegal parameter: keyStorePassword is Blank!");
            }
            this.tlsConfig.setTlsTwoWayAuthEnable(tlsTwoWayAuthEnable);
            this.tlsConfig.setTlsKeyStorePath(keyStorePath);
            this.tlsConfig.setTlsKeyStorePassword(keyStorePassword);
        }
        */
    }

    public boolean isTlsEnable() {
        return tlsConfig.isTlsEnable();
    }

    public String getTrustStorePath() {
        return tlsConfig.getTlsTrustStorePath();
    }

    public String getTrustStorePassword() {
        return tlsConfig.getTlsTrustStorePassword();
    }

    public boolean isEnableTLSTwoWayAuthentic() {
        return tlsConfig.isTlsTwoWayAuthEnable();
    }

    public String getKeyStorePath() {
        return tlsConfig.getTlsKeyStorePath();
    }

    public String getKeyStorePassword() {
        return tlsConfig.getTlsKeyStorePassword();
    }

    public boolean isEnableUserAuthentic() {
        return enableUserAuthentic;
    }

    public String getUsrName() {
        return usrName;
    }

    public String getUsrPassWord() {
        return usrPassWord;
    }

    public StatsConfig getStatsConfig() {
        return this.statsConfig;
    }

    public void setStatsConfig(StatsLevel statsLevel, boolean enableSelfPrint,
                               long selfPrintPeriodMs, long forcedResetPeriodMs) {
        this.statsConfig.updateStatsConfig(statsLevel,
                enableSelfPrint, selfPrintPeriodMs, forcedResetPeriodMs);
    }

    @Override
    public boolean equals(Object o) {
        // #lizard forgives
        if (this == o) {
            return true;
        }
        if (!(o instanceof TubeClientConfig)) {
            return false;
        }

        TubeClientConfig that = (TubeClientConfig) o;
        if (rpcReadTimeoutMs != that.rpcReadTimeoutMs) {
            return false;
        }
        if (rpcConnProcessorCnt != that.rpcConnProcessorCnt) {
            return false;
        }
        if (rpcNettyWorkMemorySize != that.rpcNettyWorkMemorySize) {
            return false;
        }
        if (rpcRspCallBackThreadCnt != that.rpcRspCallBackThreadCnt) {
            return false;
        }
        if (nettyWriteBufferHighWaterMark != that.nettyWriteBufferHighWaterMark) {
            return false;
        }
        if (nettyWriteBufferLowWaterMark != that.nettyWriteBufferLowWaterMark) {
            return false;
        }
        if (maxRegisterRetryTimes != that.maxRegisterRetryTimes) {
            return false;
        }
        if (regFailWaitPeriodMs != that.regFailWaitPeriodMs) {
            return false;
        }
        if (maxHeartBeatRetryTimes != that.maxHeartBeatRetryTimes) {
            return false;
        }
        if (heartbeatPeriodMs != that.heartbeatPeriodMs) {
            return false;
        }
        if (heartbeatPeriodAfterFail != that.heartbeatPeriodAfterFail) {
            return false;
        }
        if (linkStatsDurationMs != that.linkStatsDurationMs) {
            return false;
        }
        if (linkStatsForbiddenDurationMs != that.linkStatsForbiddenDurationMs) {
            return false;
        }
        if (unAvailableFbdDurationMs != that.unAvailableFbdDurationMs) {
            return false;
        }
        if (linkStatsMaxAllowedFailTimes != that.linkStatsMaxAllowedFailTimes) {
            return false;
        }
        if (Double.compare(that.linkStatsMaxForbiddenRate, linkStatsMaxForbiddenRate) != 0) {
            return false;
        }
        if (Double.compare(that.maxSentForbiddenRate, maxSentForbiddenRate) != 0) {
            return false;
        }
        if (maxForbiddenCheckDuration != that.maxForbiddenCheckDuration) {
            return false;
        }
        if (sessionStatisticCheckDuration != that.sessionStatisticCheckDuration) {
            return false;
        }
        if (Double.compare(that.sessionWarnForbiddenRate, sessionWarnForbiddenRate) != 0) {
            return false;
        }
        if (sessionWarnDelayedMsgCount != that.sessionWarnDelayedMsgCount) {
            return false;
        }
        if (linkMaxAllowedDelayedMsgCount != that.linkMaxAllowedDelayedMsgCount) {
            return false;
        }
        if (sessionMaxAllowedDelayedMsgCount != that.sessionMaxAllowedDelayedMsgCount) {
            return false;
        }
        if (enableUserAuthentic != that.enableUserAuthentic) {
            return false;
        }
        if (!usrName.equals(that.usrName)) {
            return false;
        }
        if (!usrPassWord.equals(that.usrPassWord)) {
            return false;
        }
        if (!this.tlsConfig.equals(that.tlsConfig)) {
            return false;
        }
        if (!this.statsConfig.equals(that.statsConfig)) {
            return false;
        }
        return masterInfo.equals(that.masterInfo);
    }

    /**
     * Get the configured Json string information
     *
     * @return    the configured Json string information
     */
    public String toJsonString() {
        int num = 0;
        String localAddress = null;
        try {
            localAddress = AddressUtils.getLocalAddress();
        } catch (Throwable e) {
            //
        }
        StringBuilder sBuilder = new StringBuilder(512);
        sBuilder.append("{\"masterInfo\":[");
        for (String item : this.masterInfo.getAddrMap4Failover().keySet()) {
            if (num++ > 0) {
                sBuilder.append(",");
            }
            sBuilder.append("\"").append(item).append("\"");
        }
        return sBuilder.append("],\"rpcReadTimeoutMs\":").append(this.rpcReadTimeoutMs)
                .append(",\"rpcConnProcessorCnt\":").append(this.rpcConnProcessorCnt)
                .append(",\"rpcNettyWorkMemorySize\":").append(this.rpcNettyWorkMemorySize)
                .append(",\"rpcRspCallBackThreadCnt\":").append(this.rpcRspCallBackThreadCnt)
                .append(",\"nettyWriteBufferHighWaterMark\":").append(this.nettyWriteBufferHighWaterMark)
                .append(",\"nettyWriteBufferLowWaterMark\":").append(this.nettyWriteBufferLowWaterMark)
                .append(",\"maxRegisterRetryTimes\":").append(this.maxRegisterRetryTimes)
                .append(",\"regFailWaitPeriodMs\":").append(this.regFailWaitPeriodMs)
                .append(",\"maxHeartBeatRetryTimes\":").append(this.maxHeartBeatRetryTimes)
                .append(",\"heartbeatPeriodMs\":").append(this.heartbeatPeriodMs)
                .append(",\"heartbeatPeriodAfterFail\":").append(this.heartbeatPeriodAfterFail)
                .append(",\"linkStatsDurationMs\":").append(this.linkStatsDurationMs)
                .append(",\"linkStatsForbiddenDurationMs\":").append(this.linkStatsForbiddenDurationMs)
                .append(",\"linkStatsMaxAllowedFailTimes\":").append(this.linkStatsMaxAllowedFailTimes)
                .append(",\"linkStatsMaxForbiddenRate\":").append(this.linkStatsMaxForbiddenRate)
                .append(",\"maxSentForbiddenRate\":").append(this.maxSentForbiddenRate)
                .append(",\"maxForbiddenCheckDuration\":").append(this.maxForbiddenCheckDuration)
                .append(",\"sessionStatisticCheckDuration\":").append(this.sessionStatisticCheckDuration)
                .append(",\"sessionWarnForbiddenRate\":").append(this.sessionWarnForbiddenRate)
                .append(",\"sessionWarnDelayedMsgCount\":").append(this.sessionWarnDelayedMsgCount)
                .append(",\"linkMaxAllowedDelayedMsgCount\":").append(this.linkMaxAllowedDelayedMsgCount)
                .append(",\"sessionMaxAllowedDelayedMsgCount\":").append(this.sessionMaxAllowedDelayedMsgCount)
                .append(",\"unAvailableFbdDurationMs\":").append(this.unAvailableFbdDurationMs)
                .append(",\"enableUserAuthentic\":").append(this.enableUserAuthentic)
                .append(",").append(this.statsConfig.toString())
                .append(",\"usrName\":\"").append(this.usrName)
                .append("\",\"usrPassWord\":\"").append(this.usrPassWord)
                .append("\",\"localAddress\":\"").append(localAddress)
                .append("\",").append(this.tlsConfig.toString())
                .append("}").toString();
    }
}
