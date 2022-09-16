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

package org.apache.inlong.agent.core;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.agent.common.AbstractDaemon;
import org.apache.inlong.agent.conf.AgentConfiguration;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.core.job.Job;
import org.apache.inlong.agent.core.job.JobManager;
import org.apache.inlong.agent.core.job.JobWrapper;
import org.apache.inlong.agent.state.State;
import org.apache.inlong.agent.utils.AgentUtils;
import org.apache.inlong.agent.utils.HttpManager;
import org.apache.inlong.agent.utils.ThreadUtils;
import org.apache.inlong.common.enums.ComponentTypeEnum;
import org.apache.inlong.common.heartbeat.AbstractHeartbeatManager;
import org.apache.inlong.common.heartbeat.GroupHeartbeat;
import org.apache.inlong.common.heartbeat.HeartbeatMsg;
import org.apache.inlong.common.heartbeat.StreamHeartbeat;
import org.apache.inlong.common.pojo.agent.TaskSnapshotMessage;
import org.apache.inlong.common.pojo.agent.TaskSnapshotRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.inlong.agent.constant.AgentConstants.AGENT_CLUSTER_IN_CHARGES;
import static org.apache.inlong.agent.constant.AgentConstants.AGENT_CLUSTER_NAME;
import static org.apache.inlong.agent.constant.AgentConstants.AGENT_CLUSTER_TAG;
import static org.apache.inlong.agent.constant.AgentConstants.AGENT_HTTP_PORT;
import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_AGENT_HTTP_PORT;
import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_HEARTBEAT_INTERVAL;
import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_MANAGER_HEARTBEAT_HTTP_PATH;
import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_MANAGER_REPORTSNAPSHOT_HTTP_PATH;
import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_MANAGER_VIP_HTTP_HOST;
import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_MANAGER_VIP_HTTP_PORT;
import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_MANAGER_VIP_HTTP_PREFIX_PATH;
import static org.apache.inlong.agent.constant.FetcherConstants.DEFAULT_AGENT_HEARTBEAT_INTERVAL;
import static org.apache.inlong.agent.constant.FetcherConstants.DEFAULT_AGENT_MANAGER_HEARTBEAT_HTTP_PATH;
import static org.apache.inlong.agent.constant.FetcherConstants.DEFAULT_AGENT_MANAGER_REPORTSNAPSHOT_HTTP_PATH;
import static org.apache.inlong.agent.constant.FetcherConstants.DEFAULT_AGENT_MANAGER_VIP_HTTP_PREFIX_PATH;
import static org.apache.inlong.agent.constant.JobConstants.JOB_GROUP_ID;
import static org.apache.inlong.agent.constant.JobConstants.JOB_STREAM_ID;

/**
 * report heartbeat to inlong-manager
 */
public class HeartbeatManager extends AbstractDaemon implements AbstractHeartbeatManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeartbeatManager.class);

    private final AgentManager agentManager;
    private final JobManager jobmanager;
    private final AgentConfiguration conf;
    private final HttpManager httpManager;
    private final String baseManagerUrl;
    private final String reportSnapshotUrl;
    private final String reportHeartbeatUrl;
    private final Pattern numberPattern = Pattern.compile("^[-+]?[\\d]*$");

    /**
     * Init heartbeat manager.
     */
    public HeartbeatManager(AgentManager agentManager) {
        this.conf = AgentConfiguration.getAgentConf();
        this.agentManager = agentManager;
        jobmanager = agentManager.getJobManager();
        httpManager = new HttpManager(conf);
        baseManagerUrl = buildBaseUrl();
        reportSnapshotUrl = buildReportSnapShotUrl(baseManagerUrl);
        reportHeartbeatUrl = buildReportHeartbeatUrl(baseManagerUrl);
    }

    @Override
    public void start() throws Exception {
        submitWorker(snapshotReportThread());
        submitWorker(heartbeatReportThread());
    }

    private Runnable snapshotReportThread() {
        return () -> {
            while (isRunnable()) {
                try {
                    TaskSnapshotRequest taskSnapshotRequest = buildTaskSnapshotRequest();
                    httpManager.doSentPost(reportSnapshotUrl, taskSnapshotRequest);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(" {} report to manager", taskSnapshotRequest);
                    }
                    SECONDS.sleep(conf.getInt(AGENT_HEARTBEAT_INTERVAL, DEFAULT_AGENT_HEARTBEAT_INTERVAL));
                } catch (Throwable e) {
                    LOGGER.error("interrupted while report snapshot", e);
                    ThreadUtils.threadThrowableHandler(Thread.currentThread(), e);
                }
            }
        };
    }

    private Runnable heartbeatReportThread() {
        return () -> {
            while (isRunnable()) {
                try {
                    reportHeartbeat(buildHeartbeatMsg());
                    SECONDS.sleep(heartbeatInterval());
                } catch (Throwable e) {
                    LOGGER.error("interrupted while report heartbeat", e);
                    ThreadUtils.threadThrowableHandler(Thread.currentThread(), e);
                }
            }
        };
    }

    @Override
    public void stop() throws Exception {
        waitForTerminate();
    }

    @Override
    public void reportHeartbeat(HeartbeatMsg heartbeat) {
        httpManager.doSentPost(reportHeartbeatUrl, heartbeat);
    }

    /**
     * build task snapshot request of job
     */
    private TaskSnapshotRequest buildTaskSnapshotRequest() {
        Map<String, JobWrapper> jobWrapperMap = jobmanager.getJobs();
        List<TaskSnapshotMessage> taskSnapshotMessageList = new ArrayList<>();
        TaskSnapshotRequest taskSnapshotRequest = new TaskSnapshotRequest();

        Date date = new Date(System.currentTimeMillis());
        for (Map.Entry<String, JobWrapper> entry : jobWrapperMap.entrySet()) {
            if (StringUtils.isBlank(entry.getKey()) || entry.getValue() == null) {
                LOGGER.info("key: {} or value: {} is null", entry.getKey(), entry.getValue());
                continue;
            }
            String offset = entry.getValue().getSnapshot();
            String jobId = entry.getKey();
            TaskSnapshotMessage snapshotMessage = new TaskSnapshotMessage();
            snapshotMessage.setSnapshot(offset);

            // TODO Need to make sure the jobId is an Integer
            if (!numberPattern.matcher(jobId).matches()) {
                continue;
            }
            snapshotMessage.setJobId(Integer.valueOf(jobId));
            taskSnapshotMessageList.add(snapshotMessage);
        }
        taskSnapshotRequest.setSnapshotList(taskSnapshotMessageList);
        taskSnapshotRequest.setReportTime(date);
        taskSnapshotRequest.setAgentIp(AgentUtils.fetchLocalIp());
        taskSnapshotRequest.setUuid(AgentUtils.fetchLocalUuid());
        return taskSnapshotRequest;
    }

    /**
     * build heartbeat message of agent
     */
    private HeartbeatMsg buildHeartbeatMsg() {
        final String agentIp = AgentUtils.fetchLocalIp();
        final int agentPort = conf.getInt(AGENT_HTTP_PORT, DEFAULT_AGENT_HTTP_PORT);
        final String clusterName = conf.get(AGENT_CLUSTER_NAME);
        final String clusterTag = conf.get(AGENT_CLUSTER_TAG);
        final String inCharges = conf.get(AGENT_CLUSTER_IN_CHARGES);

        HeartbeatMsg heartbeatMsg = new HeartbeatMsg();
        heartbeatMsg.setIp(agentIp);
        heartbeatMsg.setPort(agentPort);
        heartbeatMsg.setComponentType(ComponentTypeEnum.Agent.getName());
        heartbeatMsg.setReportTime(System.currentTimeMillis());
        if (StringUtils.isNotBlank(clusterName)) {
            heartbeatMsg.setClusterName(clusterName);
        }
        if (StringUtils.isNotBlank(clusterTag)) {
            heartbeatMsg.setClusterTag(clusterTag);
        }
        if (StringUtils.isNotBlank(inCharges)) {
            heartbeatMsg.setInCharges(inCharges);
        }

        Map<String, JobWrapper> jobWrapperMap = jobmanager.getJobs();
        List<GroupHeartbeat> groupHeartbeats = Lists.newArrayList();
        List<StreamHeartbeat> streamHeartbeats = Lists.newArrayList();
        jobWrapperMap.values().forEach(jobWrapper -> {
            Job job = jobWrapper.getJob();
            JobProfile jobProfile = job.getJobConf();
            final String groupId = jobProfile.get(JOB_GROUP_ID);
            final String streamId = jobProfile.get(JOB_STREAM_ID);
            State currentState = jobWrapper.getCurrentState();
            String status = currentState.name();
            GroupHeartbeat groupHeartbeat = new GroupHeartbeat();
            groupHeartbeat.setInlongGroupId(groupId);
            groupHeartbeat.setStatus(status);
            groupHeartbeats.add(groupHeartbeat);

            StreamHeartbeat streamHeartbeat = new StreamHeartbeat();
            streamHeartbeat.setInlongGroupId(groupId);
            streamHeartbeat.setInlongStreamId(streamId);
            streamHeartbeat.setStatus(status);
            streamHeartbeats.add(streamHeartbeat);
        });

        heartbeatMsg.setGroupHeartbeats(groupHeartbeats);
        heartbeatMsg.setStreamHeartbeats(streamHeartbeats);
        return heartbeatMsg;
    }

    /**
     * build base url for manager according to config
     *
     * example - http://127.0.0.1:8080/inlong/manager/openapi
     */
    private String buildBaseUrl() {
        return "http://" + conf.get(AGENT_MANAGER_VIP_HTTP_HOST)
                + ":" + conf.get(AGENT_MANAGER_VIP_HTTP_PORT)
                + conf.get(AGENT_MANAGER_VIP_HTTP_PREFIX_PATH, DEFAULT_AGENT_MANAGER_VIP_HTTP_PREFIX_PATH);
    }

    private String buildReportSnapShotUrl(String baseUrl) {
        return baseUrl
                + conf.get(AGENT_MANAGER_REPORTSNAPSHOT_HTTP_PATH, DEFAULT_AGENT_MANAGER_REPORTSNAPSHOT_HTTP_PATH);
    }

    private String buildReportHeartbeatUrl(String baseUrl) {
        return baseUrl + conf.get(AGENT_MANAGER_HEARTBEAT_HTTP_PATH, DEFAULT_AGENT_MANAGER_HEARTBEAT_HTTP_PATH);
    }
}