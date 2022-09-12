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

package org.apache.inlong.agent.plugin.sinks;

import io.netty.util.concurrent.DefaultThreadFactory;
import org.apache.inlong.agent.conf.AgentConfiguration;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.constant.CommonConstants;
import org.apache.inlong.agent.core.task.TaskPositionManager;
import org.apache.inlong.agent.plugin.message.SequentialID;
import org.apache.inlong.agent.plugin.metrics.PluginJmxMetric;
import org.apache.inlong.agent.plugin.metrics.PluginMetric;
import org.apache.inlong.agent.plugin.metrics.PluginPrometheusMetric;
import org.apache.inlong.agent.utils.AgentUtils;
import org.apache.inlong.agent.utils.ConfigUtil;
import org.apache.inlong.sdk.dataproxy.DefaultMessageSender;
import org.apache.inlong.sdk.dataproxy.ProxyClientConfig;
import org.apache.inlong.sdk.dataproxy.SendMessageCallback;
import org.apache.inlong.sdk.dataproxy.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_MANAGER_VIP_HTTP_HOST;
import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_MANAGER_VIP_HTTP_PORT;

/**
 * proxy client
 */
public class SenderManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SenderManager.class);
    private static final SequentialID SEQUENTIAL_ID = SequentialID.getInstance();
    private static final AtomicInteger SENDER_INDEX = new AtomicInteger(0);
    // cache for group and sender list, share the map cross agent lifecycle.
    private static final ConcurrentHashMap<String, List<DefaultMessageSender>> SENDER_MAP =
            new ConcurrentHashMap<>();
    private static final String SENDER_MANAGER_TAG_NAME = "AgentSenderManager";

    // sharing worker threads between sender client
    // in case of thread abusing.
    private static final ThreadFactory SHARED_FACTORY = new DefaultThreadFactory("agent-client-io",
            Thread.currentThread().isDaemon());

    private final String managerHost;
    private final int managerPort;
    private final String netTag;
    private final String localhost;
    private final boolean isLocalVisit;
    private final int totalAsyncBufSize;
    private final int aliveConnectionNum;
    private final boolean isCompress;
    private final int msgType;
    private final boolean isFile;
    private final long maxSenderTimeout;
    private final int maxSenderRetry;
    private final long retrySleepTime;
    private final String inlongGroupId;
    private TaskPositionManager taskPositionManager;
    private final int maxSenderPerGroup;
    private final String sourcePath;
    private final PluginMetric metric;

    private int ioThreadNum;
    private boolean enableBusyWait;
    private Semaphore semaphore;

    public SenderManager(JobProfile jobConf, String inlongGroupId, String sourcePath) {
        AgentConfiguration conf = AgentConfiguration.getAgentConf();
        managerHost = conf.get(AGENT_MANAGER_VIP_HTTP_HOST);
        managerPort = conf.getInt(AGENT_MANAGER_VIP_HTTP_PORT);
        localhost = jobConf.get(CommonConstants.PROXY_LOCAL_HOST, CommonConstants.DEFAULT_PROXY_LOCALHOST);
        netTag = jobConf.get(CommonConstants.PROXY_NET_TAG, CommonConstants.DEFAULT_PROXY_NET_TAG);
        isLocalVisit = jobConf.getBoolean(
                CommonConstants.PROXY_IS_LOCAL_VISIT, CommonConstants.DEFAULT_PROXY_IS_LOCAL_VISIT);
        totalAsyncBufSize = jobConf
                .getInt(
                        CommonConstants.PROXY_TOTAL_ASYNC_PROXY_SIZE,
                        CommonConstants.DEFAULT_PROXY_TOTAL_ASYNC_PROXY_SIZE);
        aliveConnectionNum = jobConf
                .getInt(
                        CommonConstants.PROXY_ALIVE_CONNECTION_NUM, CommonConstants.DEFAULT_PROXY_ALIVE_CONNECTION_NUM);
        isCompress = jobConf.getBoolean(
                CommonConstants.PROXY_IS_COMPRESS, CommonConstants.DEFAULT_PROXY_IS_COMPRESS);
        maxSenderPerGroup = jobConf.getInt(
                CommonConstants.PROXY_MAX_SENDER_PER_GROUP, CommonConstants.DEFAULT_PROXY_MAX_SENDER_PER_GROUP);
        msgType = jobConf.getInt(CommonConstants.PROXY_MSG_TYPE, CommonConstants.DEFAULT_PROXY_MSG_TYPE);
        maxSenderTimeout = jobConf.getInt(
                CommonConstants.PROXY_SENDER_MAX_TIMEOUT, CommonConstants.DEFAULT_PROXY_SENDER_MAX_TIMEOUT);
        maxSenderRetry = jobConf.getInt(
                CommonConstants.PROXY_SENDER_MAX_RETRY, CommonConstants.DEFAULT_PROXY_SENDER_MAX_RETRY);
        retrySleepTime = jobConf.getLong(
                CommonConstants.PROXY_RETRY_SLEEP, CommonConstants.DEFAULT_PROXY_RETRY_SLEEP);
        isFile = jobConf.getBoolean(CommonConstants.PROXY_IS_FILE, CommonConstants.DEFAULT_IS_FILE);
        taskPositionManager = TaskPositionManager.getTaskPositionManager();
        semaphore = new Semaphore(jobConf.getInt(CommonConstants.PROXY_MESSAGE_SEMAPHORE,
                CommonConstants.DEFAULT_PROXY_MESSAGE_SEMAPHORE));
        ioThreadNum = jobConf.getInt(CommonConstants.PROXY_CLIENT_IO_THREAD_NUM,
                CommonConstants.DEFAULT_PROXY_CLIENT_IO_THREAD_NUM);
        enableBusyWait = jobConf.getBoolean(CommonConstants.PROXY_CLIENT_ENABLE_BUSY_WAIT,
                CommonConstants.DEFAULT_PROXY_CLIENT_ENABLE_BUSY_WAIT);

        this.sourcePath = sourcePath;
        this.inlongGroupId = inlongGroupId;

        if (ConfigUtil.isPrometheusEnabled()) {
            this.metric = new PluginPrometheusMetric(SENDER_MANAGER_TAG_NAME);
        } else {
            this.metric = new PluginJmxMetric(SENDER_MANAGER_TAG_NAME);
        }
    }

    /**
     * Select by group.
     *
     * @param group inlong group id
     * @return default message sender
     */
    private DefaultMessageSender selectSender(String group) {
        List<DefaultMessageSender> senderList = SENDER_MAP.get(group);
        return senderList.get((SENDER_INDEX.getAndIncrement() & 0x7FFFFFFF) % senderList.size());
    }

    public void acquireSemaphore(int messageNum) {
        try {
            semaphore.acquire(messageNum);
        } catch (Exception e) {
            LOGGER.error("acquire messageNum {} fail, current semaphore {}",
                    messageNum, semaphore.availablePermits());
        }
    }

    /**
     * sender
     *
     * @param groupId group id
     * @return DefaultMessageSender
     */
    private DefaultMessageSender createMessageSender(String groupId) throws Exception {

        ProxyClientConfig proxyClientConfig = new ProxyClientConfig(
                localhost, isLocalVisit, managerHost, managerPort, groupId, netTag);
        proxyClientConfig.setTotalAsyncCallbackSize(totalAsyncBufSize);
        proxyClientConfig.setFile(isFile);
        proxyClientConfig.setAliveConnections(aliveConnectionNum);

        proxyClientConfig.setIoThreadNum(ioThreadNum);
        proxyClientConfig.setEnableBusyWait(enableBusyWait);

        DefaultMessageSender sender = new DefaultMessageSender(proxyClientConfig, SHARED_FACTORY);
        sender.setMsgtype(msgType);
        sender.setCompress(isCompress);
        return sender;
    }

    /**
     * Add new sender for group id if max size is not satisfied.
     */
    public void addMessageSender() throws Exception {
        List<DefaultMessageSender> tmpList = new ArrayList<>();
        List<DefaultMessageSender> senderList = SENDER_MAP.putIfAbsent(inlongGroupId, tmpList);
        if (senderList == null) {
            senderList = tmpList;
        }
        if (senderList.size() > maxSenderPerGroup) {
            return;
        }
        DefaultMessageSender sender = createMessageSender(inlongGroupId);
        senderList.add(sender);
    }

    /**
     * sender callback
     */
    private class AgentSenderCallback implements SendMessageCallback {

        private final int retry;
        private final String groupId;
        private final List<byte[]> bodyList;
        private final String streamId;
        private final long dataTime;
        private final String jobId;

        AgentSenderCallback(String jobId, String groupId, String streamId, List<byte[]> bodyList, int retry,
                long dataTime) {
            this.retry = retry;
            this.groupId = groupId;
            this.streamId = streamId;
            this.bodyList = bodyList;
            this.jobId = jobId;
            this.dataTime = dataTime;
        }

        @Override
        public void onMessageAck(SendResult result) {
            // if send result is not ok, retry again.
            if (result == null || !result.equals(SendResult.OK)) {
                LOGGER.warn("send groupId {}, streamId {}, jobId {}, dataTime {} fail with times {}, "
                        + "error {}", groupId, streamId, jobId, dataTime, retry, result);
                sendBatchAsync(jobId, groupId, streamId, bodyList, retry + 1, dataTime);
                return;
            }
            semaphore.release(bodyList.size());
            metric.incSendSuccessNum(bodyList.size());
            if (sourcePath != null) {
                taskPositionManager.updateSinkPosition(jobId, sourcePath, bodyList.size());
            }
        }

        @Override
        public void onException(Throwable e) {
            LOGGER.error("exception caught", e);
        }
    }

    /**
     * Send message to proxy by batch, use message cache.
     *
     * @param groupId groupId
     * @param streamId streamId
     * @param bodyList body list
     * @param retry retry time
     */
    public void sendBatchAsync(String jobId, String groupId, String streamId,
            List<byte[]> bodyList, int retry, long dataTime) {
        if (retry > maxSenderRetry) {
            LOGGER.warn("max retry reached, retry count is {}, sleep and send again", retry);
            AgentUtils.silenceSleepInMs(retrySleepTime);
        }
        try {
            selectSender(groupId).asyncSendMessage(
                    new AgentSenderCallback(jobId, groupId, streamId, bodyList, retry, dataTime),
                    bodyList, groupId, streamId,
                    dataTime,
                    SEQUENTIAL_ID.getNextUuid(),
                    maxSenderTimeout,
                    TimeUnit.SECONDS
            );
        } catch (Exception exception) {
            LOGGER.error("Exception caught", exception);
            // retry time
            try {
                TimeUnit.SECONDS.sleep(1);
                sendBatchAsync(jobId, groupId, streamId, bodyList, retry + 1, dataTime);
            } catch (Exception ignored) {
                // ignore it.
            }
        }
    }

    /**
     * Send message to proxy by batch, use message cache.
     *
     * @param groupId groupId
     * @param streamId streamId
     * @param bodyList body list
     * @param retry retry time
     */
    public void sendBatchSync(String groupId, String streamId,
            List<byte[]> bodyList, int retry, long dataTime, Map<String, String> extraMap) {
        if (retry > maxSenderRetry) {
            LOGGER.warn("max retry reached, retry count is {}, sleep and send again", retry);
            AgentUtils.silenceSleepInMs(retrySleepTime);
        }
        try {
            selectSender(groupId).sendMessage(
                    bodyList, groupId, streamId, dataTime, "",
                    maxSenderTimeout, TimeUnit.SECONDS, extraMap
            );
            semaphore.release(bodyList.size());
        } catch (Exception exception) {
            LOGGER.error("Exception caught", exception);
            // retry time
            try {
                TimeUnit.SECONDS.sleep(1);
                sendBatchSync(groupId, streamId, bodyList, retry + 1, dataTime, extraMap);
            } catch (Exception ignored) {
                // ignore it.
            }
        }
    }

}
