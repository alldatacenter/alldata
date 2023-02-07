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

package org.apache.inlong.agent.plugin.fetcher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.inlong.agent.cache.LocalFileCache;
import org.apache.inlong.agent.common.AbstractDaemon;
import org.apache.inlong.agent.conf.AgentConfiguration;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.conf.ProfileFetcher;
import org.apache.inlong.agent.conf.TriggerProfile;
import org.apache.inlong.agent.core.AgentManager;
import org.apache.inlong.agent.db.CommandDb;
import org.apache.inlong.agent.plugin.Trigger;
import org.apache.inlong.agent.plugin.utils.PluginUtils;
import org.apache.inlong.agent.pojo.ConfirmAgentIpRequest;
import org.apache.inlong.agent.pojo.DbCollectorTaskRequestDto;
import org.apache.inlong.agent.pojo.DbCollectorTaskResult;
import org.apache.inlong.agent.utils.AgentUtils;
import org.apache.inlong.agent.utils.HttpManager;
import org.apache.inlong.agent.utils.ThreadUtils;
import org.apache.inlong.common.db.CommandEntity;
import org.apache.inlong.common.enums.ManagerOpEnum;
import org.apache.inlong.common.enums.PullJobTypeEnum;
import org.apache.inlong.common.pojo.agent.CmdConfig;
import org.apache.inlong.common.pojo.agent.TaskRequest;
import org.apache.inlong.common.pojo.agent.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.apache.inlong.agent.constant.AgentConstants.AGENT_CLUSTER_NAME;
import static org.apache.inlong.agent.constant.AgentConstants.AGENT_HOME;
import static org.apache.inlong.agent.constant.AgentConstants.AGENT_LOCAL_CACHE;
import static org.apache.inlong.agent.constant.AgentConstants.AGENT_LOCAL_CACHE_TIMEOUT;
import static org.apache.inlong.agent.constant.AgentConstants.AGENT_UNIQ_ID;
import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_AGENT_HOME;
import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_AGENT_LOCAL_CACHE;
import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_AGENT_LOCAL_CACHE_TIMEOUT;
import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_AGENT_UNIQ_ID;
import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_FETCHER_INTERVAL;
import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_MANAGER_DBCOLLECT_GETTASK_HTTP_PATH;
import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_MANAGER_IP_CHECK_HTTP_PATH;
import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_MANAGER_RETURN_PARAM_DATA;
import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_MANAGER_RETURN_PARAM_IP;
import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_MANAGER_TASK_HTTP_PATH;
import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_MANAGER_VIP_HTTP_HOST;
import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_MANAGER_VIP_HTTP_PATH;
import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_MANAGER_VIP_HTTP_PORT;
import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_MANAGER_VIP_HTTP_PREFIX_PATH;
import static org.apache.inlong.agent.constant.FetcherConstants.DEFAULT_AGENT_FETCHER_INTERVAL;
import static org.apache.inlong.agent.constant.FetcherConstants.DEFAULT_AGENT_MANAGER_DBCOLLECTOR_GETTASK_HTTP_PATH;
import static org.apache.inlong.agent.constant.FetcherConstants.DEFAULT_AGENT_MANAGER_TASK_HTTP_PATH;
import static org.apache.inlong.agent.constant.FetcherConstants.DEFAULT_AGENT_MANAGER_VIP_HTTP_PREFIX_PATH;
import static org.apache.inlong.agent.constant.FetcherConstants.DEFAULT_AGENT_TDM_IP_CHECK_HTTP_PATH;
import static org.apache.inlong.agent.constant.FetcherConstants.DEFAULT_AGENT_TDM_VIP_HTTP_PATH;
import static org.apache.inlong.agent.constant.FetcherConstants.VERSION;
import static org.apache.inlong.agent.constant.JobConstants.JOB_OP;
import static org.apache.inlong.agent.constant.JobConstants.JOB_RETRY_TIME;
import static org.apache.inlong.agent.constant.JobConstants.JOB_TRIGGER;
import static org.apache.inlong.agent.plugin.fetcher.ManagerResultFormatter.getResultData;
import static org.apache.inlong.agent.plugin.utils.PluginUtils.copyJobProfile;
import static org.apache.inlong.agent.utils.AgentUtils.fetchLocalIp;
import static org.apache.inlong.agent.utils.AgentUtils.fetchLocalUuid;

/**
 * Fetch command from Inlong-Manager
 */
public class ManagerFetcher extends AbstractDaemon implements ProfileFetcher {

    public static final String AGENT = "agent";
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerFetcher.class);
    private static final GsonBuilder gsonBuilder = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final Gson GSON = gsonBuilder.create();
    private static final int MAX_RETRY = 2;
    private final String managerVipUrl;
    private final String baseManagerUrl;
    private final String managerTaskUrl;
    private final String managerIpsCheckUrl;
    private final String managerDbCollectorTaskUrl;
    private final AgentConfiguration conf;
    private final LocalFileCache localFileCache;
    private final String uniqId;
    private final AgentManager agentManager;
    private final HttpManager httpManager;
    private List<String> managerList;
    private String localIp;
    private String uuid;
    private String clusterName;

    private CommandDb commandDb;

    public ManagerFetcher(AgentManager agentManager) {
        this.agentManager = agentManager;
        this.conf = AgentConfiguration.getAgentConf();
        if (requiredKeys(conf)) {
            httpManager = new HttpManager(conf);
            baseManagerUrl = buildBaseUrl();
            managerVipUrl = buildVipUrl(baseManagerUrl);
            managerTaskUrl = buildFileCollectTaskUrl(baseManagerUrl);
            managerIpsCheckUrl = buildIpCheckUrl(baseManagerUrl);
            managerDbCollectorTaskUrl = buildDbCollectorGetTaskUrl(baseManagerUrl);
            localFileCache = getLocalFileCache();
            uniqId = conf.get(AGENT_UNIQ_ID, DEFAULT_AGENT_UNIQ_ID);
            clusterName = conf.get(AGENT_CLUSTER_NAME);
            this.commandDb = agentManager.getCommandDb();
        } else {
            throw new RuntimeException("init manager error, cannot find required key");
        }
    }

    private boolean requiredKeys(AgentConfiguration conf) {
        return conf.hasKey(AGENT_MANAGER_VIP_HTTP_HOST) && conf.hasKey(AGENT_MANAGER_VIP_HTTP_PORT);
    }

    /**
     * build base url for manager according to config
     *
     * example - http://127.0.0.1:8080/inlong/manager/openapi
     */
    private String buildBaseUrl() {
        return "http://" + conf.get(AGENT_MANAGER_VIP_HTTP_HOST)
                + ":" + conf.get(AGENT_MANAGER_VIP_HTTP_PORT) + conf.get(
                        AGENT_MANAGER_VIP_HTTP_PREFIX_PATH, DEFAULT_AGENT_MANAGER_VIP_HTTP_PREFIX_PATH);
    }

    /**
     * build vip url for manager according to config
     *
     * example - http://127.0.0.1:8080/inlong/manager/openapi/agent/getManagerIpList
     */
    private String buildVipUrl(String baseUrl) {
        return baseUrl + conf.get(AGENT_MANAGER_VIP_HTTP_PATH, DEFAULT_AGENT_TDM_VIP_HTTP_PATH);
    }

    /**
     * build file collect task url for manager according to config
     *
     * example - http://127.0.0.1:8080/inlong/manager/openapi/fileAgent/getTaskConf
     */
    private String buildFileCollectTaskUrl(String baseUrl) {
        return baseUrl + conf.get(AGENT_MANAGER_TASK_HTTP_PATH, DEFAULT_AGENT_MANAGER_TASK_HTTP_PATH);
    }

    /**
     * build ip check url for manager according to config
     *
     * example - http://127.0.0.1:8080/inlong/manager/openapi/fileAgent/confirmAgentIp
     */
    private String buildIpCheckUrl(String baseUrl) {
        return baseUrl + conf.get(AGENT_MANAGER_IP_CHECK_HTTP_PATH, DEFAULT_AGENT_TDM_IP_CHECK_HTTP_PATH);
    }

    /**
     * build db collector get task url for manager according to config
     *
     * example - http://127.0.0.1:8080/inlong/manager/openapi/dbcollector/getTask
     */
    private String buildDbCollectorGetTaskUrl(String baseUrl) {
        return baseUrl + conf
                .get(AGENT_MANAGER_DBCOLLECT_GETTASK_HTTP_PATH, DEFAULT_AGENT_MANAGER_DBCOLLECTOR_GETTASK_HTTP_PATH);
    }

    /**
     * get localFileCache according to config
     */
    private LocalFileCache getLocalFileCache() {
        Path localStorage = Paths.get(conf.get(AGENT_HOME, DEFAULT_AGENT_HOME),
                conf.get(AGENT_LOCAL_CACHE, DEFAULT_AGENT_LOCAL_CACHE), "managerList.txt");
        long timeout = TimeUnit.MINUTES.toMillis(conf.getInt(AGENT_LOCAL_CACHE_TIMEOUT,
                DEFAULT_AGENT_LOCAL_CACHE_TIMEOUT));
        return new LocalFileCache(localStorage.toFile(), timeout);
    }

    /**
     * for manager to get job profiles
     *
     * @return job profile list
     */
    @Override
    public List<JobProfile> getJobProfiles() {
        getTriggerProfiles();
        return null;
    }

    /**
     * request manager to get manager vipUrl list, and store it to local file
     */
    public void requestTdmList() {
        JsonObject result = getResultData(httpManager.doSendPost(managerVipUrl));
        JsonArray data = result.get(AGENT_MANAGER_RETURN_PARAM_DATA).getAsJsonArray();
        List<String> managerIpList = new ArrayList<>();
        for (JsonElement datum : data) {
            JsonObject asJsonObject = datum.getAsJsonObject();
            managerIpList.add(asJsonObject.get(AGENT_MANAGER_RETURN_PARAM_IP).getAsString());
        }
        if (managerIpList.isEmpty()) {
            return;
        }
        localFileCache.writeToCache(String.join(",", managerIpList));
    }

    /**
     * request manager to get commands, make sure it is not throwing exceptions
     */
    public void fetchCommand() {
        List<CommandEntity> unackedCommands = commandDb.getUnackedCommands();
        String resultStr = httpManager.doSentPost(managerTaskUrl, getFetchRequest(unackedCommands));
        JsonObject resultData = getResultData(resultStr);
        JsonElement element = resultData.get(AGENT_MANAGER_RETURN_PARAM_DATA);
        if (element != null) {
            dealWithFetchResult(GSON.fromJson(element.getAsJsonObject(), TaskResult.class));
        }
        ackCommands(unackedCommands);
    }

    private void ackCommands(List<CommandEntity> unackedCommands) {
        for (CommandEntity command : unackedCommands) {
            command.setAcked(true);
            commandDb.storeCommand(command);
        }
    }

    /**
     * request manager to get db collect task, make sure it is not throwing exceptions
     */
    public void fetchDbCollectTask() {
        if (agentManager.getJobManager().sqlJobExist()) {
            return;
        }
        JsonObject resultData = getResultData(httpManager.doSentPost(managerDbCollectorTaskUrl, getSqlTaskRequest()));
        dealWithSqlTaskResult(GSON.fromJson(resultData.get(AGENT_MANAGER_RETURN_PARAM_DATA).getAsJsonObject(),
                DbCollectorTaskResult.class));
    }

    private void dealWithSqlTaskResult(DbCollectorTaskResult taskResult) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("deal with sql task result {}", taskResult);
        }
        if (!taskResult.getVersion().equals(VERSION)) {
            LOGGER.error("invalid version {} != {}", taskResult.getVersion(), VERSION);
            return;
        }
        JobProfile profile = taskResult.getJobProfile();
        if (profile == null) {
            return;
        }
        agentManager.getJobManager().submitJobProfile(profile, true);
    }

    /**
     * the fetch file command can be normal or special
     */
    private void dealWithFetchResult(TaskResult taskResult) {
        if (!taskResult.getCmdConfigs().isEmpty() || !taskResult.getDataConfigs().isEmpty()) {
            LOGGER.info("deal with fetch result {}", taskResult);
        }
        taskResult.getDataConfigs().stream()
                .map(TriggerProfile::getTriggerProfiles)
                .forEach(profile -> {
                    LOGGER.info("the triggerProfile: {}", profile.toJsonStr());
                    if (profile.hasKey(JOB_TRIGGER)) {
                        dealWithTdmTriggerProfile(profile);
                    } else {
                        dealWithJobProfile(profile);
                    }
                });
        // todo: delete this statement,cmd would never be issued
        taskResult.getCmdConfigs().forEach(this::dealWithTdmCmd);
    }

    /**
     * form file command fetch request
     */
    public TaskRequest getFetchRequest(List<CommandEntity> unackedCommands) {
        TaskRequest request = new TaskRequest();
        request.setAgentIp(localIp);
        request.setUuid(uuid);
        request.setClusterName(clusterName);
        // when job size is over limit, require no new job
        if (agentManager.getJobManager().isJobOverLimit()) {
            request.setPullJobType(PullJobTypeEnum.NEVER.getType());
        } else {
            request.setPullJobType(PullJobTypeEnum.NEW.getType());
        }
        request.setCommandInfo(unackedCommands);
        return request;
    }

    /**
     * form db collector task fetch request
     */
    public DbCollectorTaskRequestDto getSqlTaskRequest() {
        DbCollectorTaskRequestDto request = new DbCollectorTaskRequestDto();
        request.setVersion(VERSION);
        request.setMd5("123456");
        return request;
    }

    /**
     * get command db
     */
    public CommandDb getCommandDb() {
        return commandDb;
    }

    /**
     * deal with special command retry\backtrack
     */
    public void dealWithTdmCmd(CmdConfig cmdConfig) {
        Trigger trigger = agentManager.getTriggerManager().getTrigger(
                cmdConfig.getTaskId().toString());
        if (trigger == null) {
            LOGGER.error("trigger {} doesn't exist, cmd is {}",
                    cmdConfig.getTaskId(), cmdConfig);
            commandDb.saveSpecialCmds(cmdConfig.getId(),
                    cmdConfig.getTaskId(), false);
            return;
        }
        TriggerProfile copiedProfile =
                TriggerProfile.parseJsonStr(trigger.getTriggerProfile().toJsonStr());
        String dataTime = cmdConfig.getDataTime();
        // set job retry time
        copiedProfile.set(JOB_RETRY_TIME, dataTime);
        boolean cmdResult = executeCmd(copiedProfile,
                ManagerOpEnum.getOpType(cmdConfig.getOp()), dataTime);
        commandDb.saveSpecialCmds(cmdConfig.getId(),
                cmdConfig.getTaskId(), cmdResult);
    }

    /**
     * execute commands
     */
    private boolean executeCmd(TriggerProfile triggerProfile,
            ManagerOpEnum opType, String dataTime) {
        switch (opType) {
            case RETRY:
            case BACKTRACK:
                return agentManager.getJobManager().submitFileJobProfile(triggerProfile);
            case MAKEUP:
                return makeUpFiles(triggerProfile, dataTime);
            case CHECK:
                return !PluginUtils.findSuitFiles(triggerProfile).isEmpty();
            default:
        }
        LOGGER.error("do not support such opType {}", opType);
        return false;
    }

    /**
     * when execute make up command, files scanned before should not be executed.
     */
    private boolean makeUpFiles(TriggerProfile triggerProfile, String dataTime) {
        LOGGER.info("start to make up files with trigger {}, dataTime {}",
                triggerProfile, dataTime);
        Collection<File> suitFiles = PluginUtils.findSuitFiles(triggerProfile);
        // filter files exited before
        List<File> pendingFiles =
                suitFiles.stream().filter(file -> !agentManager.getJobManager().checkJobExist(file.getAbsolutePath()))
                        .collect(Collectors.toList());
        for (File pendingFile : pendingFiles) {
            JobProfile copiedProfile = copyJobProfile(triggerProfile, pendingFile);
            LOGGER.info("ready to make up file with job {}", copiedProfile.toJsonStr());
            agentManager.getJobManager().submitFileJobProfile(copiedProfile);
        }
        return true;
    }

    /**
     * the trigger profile returned from manager should be parsed
     */
    public void dealWithTdmTriggerProfile(TriggerProfile triggerProfile) {
        ManagerOpEnum opType = ManagerOpEnum.getOpType(triggerProfile.getInt(JOB_OP));
        boolean success = true;
        try {
            switch (requireNonNull(opType)) {
                case ACTIVE:
                case ADD:
                    agentManager.getTriggerManager().submitTrigger(triggerProfile);
                    break;
                case DEL:
                case FROZEN:
                    agentManager.getTriggerManager().deleteTrigger(triggerProfile.getTriggerId());
                    break;
                default:
            }
        } catch (Exception e) {
            LOGGER.error("Deal with trigger profile err.", e);
            success = false;
        }
        commandDb.saveNormalCmds(triggerProfile, success);
    }

    /**
     * Handle tasks according to the trigger profile
     */
    public void dealWithJobProfile(TriggerProfile triggerProfile) {
        ManagerOpEnum opType = ManagerOpEnum.getOpType(triggerProfile.getInt(JOB_OP));
        boolean success = true;
        try {
            switch (requireNonNull(opType)) {
                case ACTIVE:
                case ADD:
                    success = agentManager.getJobManager().submitJobProfile(triggerProfile, true);
                    break;
                case DEL:
                case FROZEN:
                    success = agentManager.getJobManager().deleteJob(triggerProfile.getTriggerId());
                    break;
                default:
            }
        } catch (Exception e) {
            LOGGER.error("Deal with job profile err.", e);
            success = false;
        }
        commandDb.saveNormalCmds(triggerProfile, success);
    }

    /**
     * confirm local ips from manager
     */
    private String confirmLocalIps(List<String> localIps) {
        ConfirmAgentIpRequest request = new ConfirmAgentIpRequest(AGENT, localIps);
        JsonObject resultData = getResultData(httpManager.doSentPost(managerIpsCheckUrl, request)).get(
                AGENT_MANAGER_RETURN_PARAM_DATA).getAsJsonObject();
        if (!resultData.has(AGENT_MANAGER_RETURN_PARAM_IP)) {
            throw new IllegalArgumentException("cannot get ip from data " + resultData.getAsString());
        }
        return resultData.get(AGENT_MANAGER_RETURN_PARAM_IP).getAsString();
    }

    /**
     * fetch manager list, make sure it's not throwing exceptions
     *
     * @param isInitial is initial
     * @param retryTime retry time
     */
    private void fetchTdmList(boolean isInitial, int retryTime) {
        if (retryTime > MAX_RETRY) {
            return;
        }
        try {
            // check local cache time, make sure cache not timeout
            if (!isInitial && !localFileCache.cacheIsExpired()) {
                String result = localFileCache.getCacheInfo();
                managerList = Arrays.stream(result.split(","))
                        .map(String::trim)
                        .collect(Collectors.toList());
            } else {
                requestTdmList();
            }
        } catch (Exception ex) {
            fetchTdmList(false, retryTime + 1);
        }
    }

    /**
     * thread for profile fetcher.
     *
     * @return runnable profile.
     */
    private Runnable profileFetchThread() {
        return () -> {
            Thread.currentThread().setName("ManagerFetcher");
            while (isRunnable()) {
                try {
                    int configSleepTime = conf.getInt(AGENT_FETCHER_INTERVAL,
                            DEFAULT_AGENT_FETCHER_INTERVAL);
                    TimeUnit.SECONDS.sleep(AgentUtils.getRandomBySeed(configSleepTime));
                    // fetch commands from manager
                    fetchCommand();

                    // fetch manager list from vip
                    fetchTdmList(false, 0);

                    // fetch db collector task from manager
                    fetchDbCollectTask();
                } catch (Throwable ex) {
                    LOGGER.warn("exception caught", ex);
                    ThreadUtils.threadThrowableHandler(Thread.currentThread(), ex);
                }
            }
        };
    }

    /**
     * request manager to get trigger profiles.
     *
     * @return trigger profile list
     */
    @Override
    public List<TriggerProfile> getTriggerProfiles() {
        return null;
    }

    @Override
    public void start() throws Exception {
        // when agent start, check local ip and fetch manager ip list;
        localIp = fetchLocalIp();
        uuid = fetchLocalUuid();
        fetchTdmList(true, 0);
        submitWorker(profileFetchThread());
    }

    @Override
    public void stop() {
        waitForTerminate();
    }
}
