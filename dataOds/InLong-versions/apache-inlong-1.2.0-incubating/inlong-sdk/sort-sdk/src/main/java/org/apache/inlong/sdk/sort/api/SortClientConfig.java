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

package org.apache.inlong.sdk.sort.api;

import java.io.Serializable;
import java.util.concurrent.Semaphore;

public class SortClientConfig implements Serializable {

    public static final String MONITOR_NAME = "read_stat";

    private static final long serialVersionUID = -7531960714809683830L;

    private String sortTaskId;
    private String sortClusterName = "default";
    private InLongTopicChangeListener assignmentsListener;
    private ReadCallback callback;
    private int callbackQueueSize = 100;
    private int pulsarReceiveQueueSize = 2000;
    private int kafkaFetchWaitMs = 5000;
    private int kafkaFetchSizeBytes = 3 * 1024 * 1024;
    private int kafkaSocketRecvBufferSize = 5 * 1024 * 1024;
    private Semaphore globalInProgressRequest = new Semaphore(Short.MAX_VALUE, true);
    private String localIp;
    private String appName;
    private String serverName;
    private String containerId;
    private String instanceName;
    private String env;
    private String managerApiUrl;
    private String managerApiVersion;
    private ConsumeStrategy consumeStrategy;
    private int reportStatisticIntervalSec = 60;
    private int updateMetaDataIntervalSec = 10;
    private int ackTimeoutSec = 120;
    private volatile boolean stopConsume = false;
    private boolean isPrometheusEnabled = true;
    private int emptyPollSleepStepMs = 50;
    private int maxEmptyPollSleepMs = 500;
    private int emptyPollTimes = 10;

    public SortClientConfig(String sortTaskId, String sortClusterName, InLongTopicChangeListener assignmentsListener,
            ConsumeStrategy consumeStrategy, String localIp) {
        this.sortTaskId = sortTaskId;
        this.sortClusterName = sortClusterName;
        this.assignmentsListener = assignmentsListener;
        this.consumeStrategy = consumeStrategy;
        this.localIp = localIp;
    }

    public boolean isStopConsume() {
        return stopConsume;
    }

    public void setStopConsume(boolean stopConsume) {
        this.stopConsume = stopConsume;
    }

    public String getSortTaskId() {
        return sortTaskId;
    }

    public void setSortTaskId(String sortTaskId) {
        this.sortTaskId = sortTaskId;
    }

    public String getSortClusterName() {
        return sortClusterName;
    }

    public void setSortClusterName(String sortClusterName) {
        this.sortClusterName = sortClusterName;
    }

    public InLongTopicChangeListener getAssignmentsListener() {
        return (assignmentsListener == null
                ? EmptyListener.EMPTY_LISTENER
                : assignmentsListener);
    }

    public void setAssignmentsListener(InLongTopicChangeListener assignmentsListener) {
        this.assignmentsListener = assignmentsListener;
    }

    public ConsumeStrategy getOffsetResetStrategy() {
        return consumeStrategy;
    }

    public void setOffsetResetStrategy(ConsumeStrategy consumeStrategy) {
        this.consumeStrategy = consumeStrategy;
    }

    /**
     * get fetchCallback
     *
     * @return the callback
     */
    public ReadCallback getCallback() {
        return callback;
    }

    /**
     * set fetchCallback
     *
     * @param callback the callback to set
     */
    public void setCallback(ReadCallback callback) {
        this.callback = callback;
    }

    public int getCallbackQueueSize() {
        return callbackQueueSize;
    }

    public void setCallbackQueueSize(int callbackQueueSize) {
        this.callbackQueueSize = callbackQueueSize;
    }

    public int getReportStatisticIntervalSec() {
        return reportStatisticIntervalSec;
    }

    public void setReportStatisticIntervalSec(int reportStatisticIntervalSec) {
        this.reportStatisticIntervalSec = reportStatisticIntervalSec;
    }

    public int getUpdateMetaDataIntervalSec() {
        return updateMetaDataIntervalSec;
    }

    public void setUpdateMetaDataIntervalSec(int updateMetaDataIntervalSec) {
        this.updateMetaDataIntervalSec = updateMetaDataIntervalSec;
    }

    public int getAckTimeoutSec() {
        return ackTimeoutSec;
    }

    public void setAckTimeoutSec(int ackTimeoutSec) {
        this.ackTimeoutSec = ackTimeoutSec;
    }

    public int getPulsarReceiveQueueSize() {
        return pulsarReceiveQueueSize;
    }

    public void setPulsarReceiveQueueSize(int pulsarReceiveQueueSize) {
        this.pulsarReceiveQueueSize = pulsarReceiveQueueSize;
    }

    public int getKafkaFetchWaitMs() {
        return kafkaFetchWaitMs;
    }

    public void setKafkaFetchWaitMs(int kafkaFetchWaitMs) {
        this.kafkaFetchWaitMs = kafkaFetchWaitMs;
    }

    public int getKafkaFetchSizeBytes() {
        return kafkaFetchSizeBytes;
    }

    public void setKafkaFetchSizeBytes(int kafkaFetchSizeBytes) {
        this.kafkaFetchSizeBytes = kafkaFetchSizeBytes;
    }

    public int getKafkaSocketRecvBufferSize() {
        return kafkaSocketRecvBufferSize;
    }

    public void setKafkaSocketRecvBufferSize(int kafkaSocketRecvBufferSize) {
        this.kafkaSocketRecvBufferSize = kafkaSocketRecvBufferSize;
    }

    public Semaphore getGlobalInProgressRequest() {
        return globalInProgressRequest;
    }

    public void setGlobalInProgressRequest(Semaphore globalInProgressRequest) {
        this.globalInProgressRequest = globalInProgressRequest;
    }

    /**
     * get localIp
     *
     * @return the localIp
     */
    public String getLocalIp() {
        return localIp;
    }

    /**
     * set localIp
     *
     * @param localIp the localIp to set
     */
    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getManagerApiUrl() {
        return managerApiUrl;
    }

    public void setManagerApiUrl(String managerApiUrl) {
        this.managerApiUrl = managerApiUrl;
    }

    public String getManagerApiVersion() {
        return managerApiVersion;
    }

    public void setManagerApiVersion(String managerApiVersion) {
        this.managerApiVersion = managerApiVersion;
    }

    public boolean isPrometheusEnabled() {
        return isPrometheusEnabled;
    }

    public void setPrometheusEnabled(boolean prometheusEnabled) {
        isPrometheusEnabled = prometheusEnabled;
    }

    public int getEmptyPollSleepStepMs() {
        return emptyPollSleepStepMs;
    }

    public void setEmptyPollSleepStepMs(int emptyPollSleepStepMs) {
        this.emptyPollSleepStepMs = emptyPollSleepStepMs;
    }

    public int getMaxEmptyPollSleepMs() {
        return maxEmptyPollSleepMs;
    }

    public void setMaxEmptyPollSleepMs(int maxEmptyPollSleepMs) {
        this.maxEmptyPollSleepMs = maxEmptyPollSleepMs;
    }

    public int getEmptyPollTimes() {
        return emptyPollTimes;
    }

    public void setEmptyPollTimes(int emptyPollTimes) {
        this.emptyPollTimes = emptyPollTimes;
    }

    /**
     * ConsumeStrategy
     */
    public enum ConsumeStrategy {
        // consume from last ack position,if not exists then consume from earliest
        earliest,
        // consume from last ack position,if not exists then consume from lastest
        lastest,
        // consume from smallest position
        earliest_absolutely,
        // consume from largest position
        lastest_absolutely
    }
}
