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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.inlong.sdk.sort.entity.InLongTopic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class SortClientConfig implements Serializable {

    public static final String MONITOR_NAME = "SortSdk";

    private static final long serialVersionUID = -7531960714809683830L;

    private final String sortTaskId;
    private final String sortClusterName;
    private InLongTopicChangeListener assignmentsListener;
    private ReadCallback callback;
    private int callbackQueueSize = 100;
    private int pulsarReceiveQueueSize = 2000;
    private long statsIntervalSeconds = -1;
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
    private TopicType topicType;
    private int reportStatisticIntervalSec = 60;
    private int updateMetaDataIntervalSec = 10;
    private int ackTimeoutSec = 0;
    private volatile boolean stopConsume = false;
    private boolean isPrometheusEnabled = true;
    private int emptyPollSleepStepMs = 10;
    private int maxEmptyPollSleepMs = 500;
    private int emptyPollTimes = 10;
    private int cleanOldConsumerIntervalSec = 60;
    private int maxConsumerSize = 5;

    private ConsumerSubsetType consumerSubsetType = ConsumerSubsetType.ALL;
    private int consumerSubsetSize = 1;

    private boolean topicStaticsEnabled = true;
    private boolean partitionStaticsEnabled = true;

    public SortClientConfig(
            String sortTaskId,
            String sortClusterName,
            InLongTopicChangeListener assignmentsListener,
            ConsumeStrategy consumeStrategy,
            String localIp) {
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

    public String getSortClusterName() {
        return sortClusterName;
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
     * get the type of topic manager
     * @return
     */
    public TopicType getTopicType() {
        return topicType;
    }

    /**
     * Set type of topic manager
     * @param topicType
     */
    public void setTopicManagerType(TopicType topicType) {
        this.topicType = topicType;
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

    public long getStatsIntervalSeconds() {
        return statsIntervalSeconds;
    }

    public void setStatsIntervalSeconds(long statsIntervalSeconds) {
        this.statsIntervalSeconds = statsIntervalSeconds;
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

    public int getCleanOldConsumerIntervalSec() {
        return cleanOldConsumerIntervalSec;
    }

    public void setCleanOldConsumerIntervalSec(int cleanOldConsumerIntervalSec) {
        this.cleanOldConsumerIntervalSec = cleanOldConsumerIntervalSec;
    }

    public int getMaxConsumerSize() {
        return maxConsumerSize;
    }

    public void setMaxConsumerSize(int maxConsumerSize) {
        this.maxConsumerSize = maxConsumerSize;
    }

    public ConsumerSubsetType getConsumerSubsetType() {
        return consumerSubsetType;
    }

    public void setConsumerSubsetSize(ConsumerSubsetType consumerSubsetType) {
        this.consumerSubsetType = consumerSubsetType;
    }

    public int getConsumerSubsetSize() {
        return consumerSubsetSize;
    }

    public void setConsumerSubsetSize(int consumerSubsetSize) {
        this.consumerSubsetSize = consumerSubsetSize;
    }

    public boolean isTopicStaticsEnabled() {
        return topicStaticsEnabled;
    }

    public boolean isPartitionStaticsEnabled() {
        return partitionStaticsEnabled;
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

    public enum TopicType {
        // single topic manager and fetcher
        SINGLE_TOPIC,
        // multi topic manager and fetcher
        MULTI_TOPIC
    }

    /**
     * setParameters
     * @param sortSdkParams
     */
    public void setParameters(Map<String, String> sortSdkParams) {
        this.callbackQueueSize = NumberUtils.toInt(sortSdkParams.get(ConfigConstants.CALLBACK_QUEUE_SIZE),
                callbackQueueSize);
        this.pulsarReceiveQueueSize = NumberUtils.toInt(sortSdkParams.get(ConfigConstants.PULSAR_RECEIVE_QUEUE_SIZE),
                pulsarReceiveQueueSize);
        this.statsIntervalSeconds = NumberUtils.toLong(sortSdkParams.get(ConfigConstants.STATS_INTERVAL_SECONDS),
                statsIntervalSeconds);
        this.kafkaFetchWaitMs = NumberUtils.toInt(sortSdkParams.get(ConfigConstants.KAFKA_FETCH_WAIT_MS),
                kafkaFetchWaitMs);
        this.kafkaFetchSizeBytes = NumberUtils.toInt(sortSdkParams.get(ConfigConstants.KAFKA_FETCH_SIZE_BYTES),
                kafkaFetchSizeBytes);
        this.kafkaSocketRecvBufferSize = NumberUtils.toInt(
                sortSdkParams.get(ConfigConstants.KAFKA_SOCKET_RECV_BUFFER_SIZE),
                kafkaSocketRecvBufferSize);

        this.localIp = sortSdkParams.getOrDefault(ConfigConstants.LOCAL_IP, localIp);
        this.appName = sortSdkParams.getOrDefault(ConfigConstants.APP_NAME, appName);
        this.serverName = sortSdkParams.getOrDefault(ConfigConstants.SERVER_NAME, serverName);
        this.containerId = sortSdkParams.getOrDefault(ConfigConstants.CONTAINER_ID, containerId);
        this.instanceName = sortSdkParams.getOrDefault(ConfigConstants.INSTANCE_NAME, instanceName);
        this.env = sortSdkParams.getOrDefault(ConfigConstants.ENV, env);
        this.managerApiUrl = sortSdkParams.getOrDefault(ConfigConstants.MANAGER_API_URL, managerApiUrl);
        this.managerApiVersion = sortSdkParams.getOrDefault(ConfigConstants.MANAGER_API_VERSION, managerApiVersion);
        String strConsumeStrategy = sortSdkParams.getOrDefault(ConfigConstants.CONSUME_STRATEGY,
                consumeStrategy.name());
        String strManagerType = sortSdkParams.getOrDefault(ConfigConstants.TOPIC_MANAGER_TYPE,
                TopicType.MULTI_TOPIC.toString());
        this.consumeStrategy = ConsumeStrategy.valueOf(strConsumeStrategy);
        this.topicType = TopicType.valueOf(strManagerType);

        this.reportStatisticIntervalSec = NumberUtils.toInt(
                sortSdkParams.get(ConfigConstants.REPORT_STATISTIC_INTERVAL_SEC),
                reportStatisticIntervalSec);
        this.updateMetaDataIntervalSec = NumberUtils.toInt(
                sortSdkParams.get(ConfigConstants.UPDATE_META_DATA_INTERVAL_SEC),
                updateMetaDataIntervalSec);
        this.ackTimeoutSec = NumberUtils.toInt(sortSdkParams.get(ConfigConstants.ACK_TIMEOUT_SEC), ackTimeoutSec);
        this.cleanOldConsumerIntervalSec = NumberUtils.toInt(
                sortSdkParams.get(ConfigConstants.CLEAN_OLD_CONSUMER_INTERVAL_SEC),
                cleanOldConsumerIntervalSec);

        String strPrometheusEnabled = sortSdkParams.getOrDefault(ConfigConstants.IS_PROMETHEUS_ENABLED,
                Boolean.TRUE.toString());
        this.isPrometheusEnabled = StringUtils.equalsIgnoreCase(strPrometheusEnabled, Boolean.TRUE.toString());

        this.emptyPollSleepStepMs = NumberUtils.toInt(sortSdkParams.get(ConfigConstants.EMPTY_POLL_SLEEP_STEP_MS),
                emptyPollSleepStepMs);
        this.maxEmptyPollSleepMs = NumberUtils.toInt(sortSdkParams.get(ConfigConstants.MAX_EMPTY_POLL_SLEEP_MS),
                maxEmptyPollSleepMs);
        this.emptyPollTimes = NumberUtils.toInt(sortSdkParams.get(ConfigConstants.EMPTY_POLL_TIMES), emptyPollTimes);

        this.maxConsumerSize = NumberUtils.toInt(sortSdkParams.get(ConfigConstants.MAX_CONSUMER_SIZE),
                maxConsumerSize);
        this.consumerSubsetType = ConsumerSubsetType.convert(
                sortSdkParams.getOrDefault(ConfigConstants.CONSUMER_SUBSET_TYPE, ConsumerSubsetType.CLUSTER.name()));
        this.consumerSubsetSize = NumberUtils.toInt(sortSdkParams.get(ConfigConstants.CONSUMER_SUBSET_SIZE),
                consumerSubsetSize);

        String strTopicStaticsEnabled = sortSdkParams.getOrDefault(ConfigConstants.IS_TOPIC_STATICS_ENABLED,
                Boolean.TRUE.toString());
        this.topicStaticsEnabled = StringUtils.equalsIgnoreCase(strTopicStaticsEnabled, Boolean.TRUE.toString());
        String strPartitionStaticsEnabled = sortSdkParams.getOrDefault(ConfigConstants.IS_PARTITION_STATICS_ENABLED,
                Boolean.TRUE.toString());
        this.partitionStaticsEnabled = StringUtils.equalsIgnoreCase(strPartitionStaticsEnabled,
                Boolean.TRUE.toString());
    }

    public List<InLongTopic> getConsumerSubset(List<InLongTopic> totalTopics) {
        if (this.consumerSubsetSize <= 1
                || this.containerId == null
                || this.consumerSubsetType == ConsumerSubsetType.ALL) {
            return totalTopics;
        }
        List<InLongTopic> subset = new ArrayList<>(totalTopics.size());
        int containerHashId = Math.abs(this.containerId.hashCode()) % this.consumerSubsetSize;
        for (InLongTopic topic : totalTopics) {
            int topicHashId = 0;
            if (this.consumerSubsetType == ConsumerSubsetType.CLUSTER) {
                String hashString = topic.getInLongCluster().getClusterId();
                topicHashId = Math.abs(hashString.hashCode()) % this.consumerSubsetSize;
            } else {
                String hashString = topic.getTopicKey();
                topicHashId = Math.abs(hashString.hashCode()) % this.consumerSubsetSize;
            }
            if (containerHashId == topicHashId) {
                subset.add(topic);
            }
        }
        return subset;
    }

}
