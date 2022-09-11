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

import org.apache.inlong.tubemq.client.common.TClientConstants;
import org.apache.inlong.tubemq.client.consumer.ConsumePosition;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.cluster.MasterInfo;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;

/**
 * Contains configuration information of a consumer.
 */
public class ConsumerConfig extends TubeClientConfig {

    private final String consumerGroup;

    /* consumeModel
     *    Set the start position of the consumer group. The value can be [-1, 0, 1]. Default value is 0.
     * -1: Start from 0 for the first time. Otherwise start from last consume position.
     *  0: Start from the latest position for the first time. Otherwise start from last consume position.
     *  1: Start from the latest consume position.
    */
    private ConsumePosition consumePosition = ConsumePosition.CONSUMER_FROM_LATEST_OFFSET;
    private int maxSubInfoReportIntvlTimes =
            TClientConstants.MAX_SUBSCRIBE_REPORT_INTERVAL_TIMES;
    private long msgNotFoundWaitPeriodMs =
            TClientConstants.CFG_DEFAULT_MSG_NOTFOUND_WAIT_PERIOD_MS;
    private long pullConsumeReadyWaitPeriodMs =
            TClientConstants.CFG_DEFAULT_CONSUME_READ_WAIT_PERIOD_MS;
    private long pullConsumeReadyChkSliceMs =
            TClientConstants.CFG_DEFAULT_CONSUME_READ_CHECK_SLICE_MS;
    private long shutDownRebalanceWaitPeriodMs =
            TClientConstants.CFG_DEFAULT_SHUTDOWN_REBALANCE_WAIT_PERIOD_MS;
    private long partMetaInfoCheckPeriodMs =
            TClientConstants.CFG_DEFAULT_META_QUERY_WAIT_PERIOD_MS;
    private int pushFetchThreadCnt =
            TClientConstants.CFG_DEFAULT_CLIENT_PUSH_FETCH_THREAD_CNT;
    private boolean pushListenerWaitTimeoutRollBack = true;
    private boolean pushListenerThrowedRollBack = false;
    private long pushListenerWaitPeriodMs =
            TClientConstants.CFG_DEFAULT_PUSH_LISTENER_WAIT_PERIOD_MS;
    private boolean pullRebConfirmTimeoutRollBack = true;
    private long pullRebConfirmWaitPeriodMs =
            TClientConstants.CFG_DEFAULT_PULL_REB_CONFIRM_WAIT_PERIOD_MS;
    private long pullProtectConfirmTimeoutMs =
            TClientConstants.CFG_DEFAULT_PULL_PROTECT_CONFIRM_WAIT_PERIOD_MS;
    private boolean pullConfirmInLocal = false;

    public ConsumerConfig(String masterAddrInfo, String consumerGroup) {
        this(new MasterInfo(masterAddrInfo), consumerGroup);
    }

    public ConsumerConfig(MasterInfo masterInfo, String consumerGroup) {
        super(masterInfo);
        validConsumerGroupParameter(consumerGroup);
        this.consumerGroup = consumerGroup.trim();
    }

    @Deprecated
    public ConsumerConfig(String localHostIP, String masterAddrInfo,
                          String consumerGroup) throws Exception {
        this(localHostIP, new MasterInfo(masterAddrInfo), consumerGroup);
    }

    @Deprecated
    public ConsumerConfig(String localHostIP, MasterInfo masterInfo,
                          String consumerGroup) throws Exception {
        super(localHostIP, masterInfo);
        validConsumerGroupParameter(consumerGroup);
        this.consumerGroup = consumerGroup.trim();
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public ConsumePosition getConsumePosition() {
        return consumePosition;
    }

    public void setConsumePosition(ConsumePosition consumePosition) {
        this.consumePosition = consumePosition;
    }

    /**
     * recommend to use getConsumePosition
     */
    @Deprecated
    public int getConsumeModel() {
        return consumePosition.getCode();
    }

    /**
     * recommend to use setConsumePosition
     */
    @Deprecated
    public void setConsumeModel(int consumeModel) {
        setConsumePosition(ConsumePosition.valueOf(consumeModel));
    }

    public long getMsgNotFoundWaitPeriodMs() {
        return msgNotFoundWaitPeriodMs;
    }

    public void setMsgNotFoundWaitPeriodMs(long msgNotFoundWaitPeriodMs) {
        this.msgNotFoundWaitPeriodMs = msgNotFoundWaitPeriodMs;
    }

    public long getPullConsumeReadyWaitPeriodMs() {
        return pullConsumeReadyWaitPeriodMs;
    }

    // setPullConsumeReadyWaitPeriodMs() use note:
    // The value range is [negative value, 0, positive value] and the value directly determines
    // the behavior of the PullMessageConsumer.GetMessage() function:
    // 1. if it is set to a negative value, it means that the GetMessage() calling thread will
    //    be blocked forever and will not return until the consumption conditions are met;
    // 2. if If it is set to 0, it means that the GetMessage() calling thread will only block
    //    the ConsumerConfig.getPullConsumeReadyChkSliceMs() interval when the consumption
    //    conditions are not met and then return;
    // 3. if it is set to a positive number, it will not meet the current user usage (including
    //    unused partitions or allocated partitions, but these partitions do not meet the usage
    //    conditions), the GetMessage() calling thread will be blocked until the total time of
    //    ConsumerConfig.getPullConsumeReadyWaitPeriodMs expires
    public void setPullConsumeReadyWaitPeriodMs(long pullConsumeReadyWaitPeriodMs) {
        this.pullConsumeReadyWaitPeriodMs = pullConsumeReadyWaitPeriodMs;
    }

    public long getPullConsumeReadyChkSliceMs() {
        return pullConsumeReadyChkSliceMs;
    }

    public void setPullConsumeReadyChkSliceMs(long pullConsumeReadyChkSliceMs) {
        if (pullConsumeReadyChkSliceMs >= 0
                && pullConsumeReadyChkSliceMs <= 1000) {
            this.pullConsumeReadyChkSliceMs = pullConsumeReadyChkSliceMs;
        }
    }

    public long getShutDownRebalanceWaitPeriodMs() {
        return shutDownRebalanceWaitPeriodMs;
    }

    public void setShutDownRebalanceWaitPeriodMs(long shutDownRebalanceWaitPeriodMs) {
        this.shutDownRebalanceWaitPeriodMs = shutDownRebalanceWaitPeriodMs;
    }

    public long getPartMetaInfoCheckPeriodMs() {
        return partMetaInfoCheckPeriodMs;
    }

    public void setPartMetaInfoCheckPeriodMs(long partMetaInfoCheckPeriodMs) {
        if (partMetaInfoCheckPeriodMs < TClientConstants.CFG_MIN_META_QUERY_WAIT_PERIOD_MS) {
            this.partMetaInfoCheckPeriodMs = TClientConstants.CFG_MIN_META_QUERY_WAIT_PERIOD_MS;
        }
        this.partMetaInfoCheckPeriodMs = partMetaInfoCheckPeriodMs;
    }

    public int getPushFetchThreadCnt() {
        return pushFetchThreadCnt;
    }

    public void setPushFetchThreadCnt(int pushFetchThreadCnt) {
        if (pushFetchThreadCnt <= 0) {
            this.pushFetchThreadCnt = TClientConstants.CFG_DEFAULT_CLIENT_PUSH_FETCH_THREAD_CNT;
        } else {
            this.pushFetchThreadCnt = pushFetchThreadCnt;
        }
    }

    public boolean isPushListenerWaitTimeoutRollBack() {
        return pushListenerWaitTimeoutRollBack;
    }

    public void setPushListenerWaitTimeoutRollBack(boolean pushListenerWaitTimeoutRollBack) {
        this.pushListenerWaitTimeoutRollBack = pushListenerWaitTimeoutRollBack;
    }

    public boolean isPushListenerThrowedRollBack() {
        return pushListenerThrowedRollBack;
    }

    public void setPushListenerThrowedRollBack(boolean pushListenerThrowedRollBack) {
        this.pushListenerThrowedRollBack = pushListenerThrowedRollBack;
    }

    public long getPushListenerWaitPeriodMs() {
        return pushListenerWaitPeriodMs;
    }

    public void setPushListenerWaitPeriodMs(long pushListenerWaitPeriodMs) {
        this.pushListenerWaitPeriodMs = pushListenerWaitPeriodMs;
    }

    public boolean isPullRebConfirmTimeoutRollBack() {
        return pullRebConfirmTimeoutRollBack;
    }

    public void setPullRebConfirmTimeoutRollBack(boolean pullRebConfirmTimeoutRollBack) {
        this.pullRebConfirmTimeoutRollBack = pullRebConfirmTimeoutRollBack;
    }

    public long getPullRebConfirmWaitPeriodMs() {
        return pullRebConfirmWaitPeriodMs;
    }

    public void setPullRebConfirmWaitPeriodMs(long pullRebConfirmWaitPeriodMs) {
        this.pullRebConfirmWaitPeriodMs = pullRebConfirmWaitPeriodMs;
    }

    public boolean isPullConfirmInLocal() {
        return pullConfirmInLocal;
    }

    public void setPullConfirmInLocal(boolean pullConfirmInLocal) {
        this.pullConfirmInLocal = pullConfirmInLocal;
    }

    public long getPullProtectConfirmTimeoutMs() {
        return pullProtectConfirmTimeoutMs;
    }

    public void setPullProtectConfirmTimeoutMs(long pullProtectConfirmTimeoutMs) {
        this.pullProtectConfirmTimeoutMs = pullProtectConfirmTimeoutMs;
    }

    public int getMaxSubInfoReportIntvlTimes() {
        return maxSubInfoReportIntvlTimes;
    }

    public void setMaxSubInfoReportIntvlTimes(int maxSubInfoReportIntvlTimes) {
        this.maxSubInfoReportIntvlTimes = maxSubInfoReportIntvlTimes;
    }

    private void validConsumerGroupParameter(String consumerGroup) {
        if (TStringUtils.isBlank(consumerGroup)) {
            throw new IllegalArgumentException("Illegal parameter: consumerGroup is Blank!");
        }
        String tmpConsumerGroup = consumerGroup.trim();
        if (tmpConsumerGroup.length() > TBaseConstants.META_MAX_GROUPNAME_LENGTH) {
            throw new IllegalArgumentException(new StringBuilder(512)
                    .append("Illegal parameter: the max length of consumerGroup is ")
                    .append(TBaseConstants.META_MAX_GROUPNAME_LENGTH)
                    .append(" characters").toString());
        }
        if (!tmpConsumerGroup.matches(TBaseConstants.META_TMP_GROUP_VALUE)) {
            throw new IllegalArgumentException(new StringBuilder(512)
                    .append("Illegal parameter: the value of consumerGroup")
                    .append(" must begin with a letter, ")
                    .append("can only contain characters,numbers,hyphen,and underscores").toString());
        }
    }

    @Override
    public String toString() {
        return new StringBuilder(512).append("\"ConsumerConfig\":{")
                .append("\"consumerGroup\":\"").append(this.consumerGroup)
                .append("\",\"maxSubInfoReportIntvlTimes\":").append(this.maxSubInfoReportIntvlTimes)
                .append(",\"consumePosition\":").append(this.consumePosition)
                .append(",\"msgNotFoundWaitPeriodMs\":").append(this.msgNotFoundWaitPeriodMs)
                .append(",\"shutDownRebalanceWaitPeriodMs\":").append(this.shutDownRebalanceWaitPeriodMs)
                .append(",\"pushFetchThreadCnt\":").append(this.pushFetchThreadCnt)
                .append(",\"pushListenerWaitTimeoutRollBack\":").append(this.pushListenerWaitTimeoutRollBack)
                .append(",\"pushListenerThrowedRollBack\":").append(this.pushListenerThrowedRollBack)
                .append(",\"pushListenerWaitPeriodMs\":").append(this.pushListenerWaitPeriodMs)
                .append(",\"pullRebConfirmTimeoutRollBack\":").append(this.pullRebConfirmTimeoutRollBack)
                .append(",\"pullConfirmWaitPeriodMs\":").append(this.pullRebConfirmWaitPeriodMs)
                .append(",\"pullProtectConfirmTimeoutPeriodMs\":").append(this.pullProtectConfirmTimeoutMs)
                .append(",\"pullConfirmInLocal\":").append(this.pullConfirmInLocal)
                .append(",\"maxSubInfoReportIntvlTimes\":").append(this.maxSubInfoReportIntvlTimes)
                .append(",\"partMetaInfoCheckPeriodMs\":").append(this.partMetaInfoCheckPeriodMs)
                .append(",\"ClientConfig\":").append(toJsonString())
                .append("}").toString();
    }
}
