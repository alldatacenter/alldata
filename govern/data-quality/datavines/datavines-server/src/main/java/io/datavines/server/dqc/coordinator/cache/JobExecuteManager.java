/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.server.dqc.coordinator.cache;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.sift.SiftingAppender;
import io.datavines.common.CommonConstants;
import io.datavines.common.config.Configurations;
import io.datavines.common.config.DataVinesJobConfig;
import io.datavines.common.entity.JobExecutionParameter;
import io.datavines.common.entity.JobExecutionInfo;
import io.datavines.common.entity.JobExecutionRequest;
import io.datavines.common.enums.ExecutionStatus;
import io.datavines.common.log.JobExecutionLogDiscriminator;
import io.datavines.common.utils.*;
import io.datavines.engine.config.DataVinesConfigurationManager;
import io.datavines.engine.core.utils.JsonUtils;
import io.datavines.notification.api.entity.SlaConfigMessage;
import io.datavines.notification.api.entity.SlaNotificationMessage;
import io.datavines.notification.api.entity.SlaSenderMessage;
import io.datavines.notification.core.client.NotificationClient;
import io.datavines.server.dqc.command.CommandCode;
import io.datavines.server.dqc.command.JobExecuteAckCommand;
import io.datavines.server.dqc.command.JobExecuteResponseCommand;
import io.datavines.common.exception.DataVinesException;
import io.datavines.server.repository.entity.DataSource;
import io.datavines.server.repository.entity.Job;
import io.datavines.server.repository.entity.JobExecution;
import io.datavines.server.repository.service.DataSourceService;
import io.datavines.server.repository.service.JobService;
import io.datavines.server.repository.service.SlaNotificationService;
import io.datavines.server.repository.service.impl.JobExternalService;
import io.datavines.server.dqc.coordinator.operator.DataQualityResultOperator;
import io.datavines.server.dqc.executor.cache.JobExecutionCache;
import io.datavines.server.dqc.executor.cache.JobExecutionContext;
import io.datavines.server.dqc.executor.runner.JobRunner;
import io.datavines.server.utils.DefaultDataSourceInfoUtils;
import io.datavines.server.utils.NamedThreadFactory;
import io.datavines.server.utils.SpringApplicationContext;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

public class JobExecuteManager {

    private final Logger logger = LoggerFactory.getLogger(JobExecuteManager.class);

    private final LinkedBlockingQueue<CommandContext> jobExecutionQueue = new LinkedBlockingQueue<>();

    private final ConcurrentHashMap<Long, JobExecutionRequest> unFinishedJobExecutionMap = new ConcurrentHashMap<>();
    
    private final LinkedBlockingQueue<JobExecutionResponseContext> responseQueue = new LinkedBlockingQueue<>();

    private final ExecutorService executorService;

    private final ExecutorService jobExecuteService;

    private final JobExternalService jobExternalService;

    private final JobExecutionCache jobExecutionCache;

    private final HashedWheelTimer wheelTimer =
            new HashedWheelTimer(new NamedThreadFactory("Job-Execute-Timeout"),1,TimeUnit.SECONDS);

    private final Configurations configurations;

    private final DataQualityResultOperator dataQualityResultOperator;

    private final NotificationClient notificationClient;

    private final DataSourceService dataSourceService;

    private final SlaNotificationService slaNotificationService;

    public JobExecuteManager(){

        this.executorService = Executors.newFixedThreadPool(5, new NamedThreadFactory("Server-thread"));
        this.jobExecuteService = Executors.newFixedThreadPool(
                CommonPropertyUtils.getInt(CommonPropertyUtils.EXEC_THREADS,CommonPropertyUtils.EXEC_THREADS_DEFAULT),
                new NamedThreadFactory("Executor-execute-thread"));
        this.jobExternalService = SpringApplicationContext.getBean(JobExternalService.class);
        this.jobExecutionCache = JobExecutionCache.getInstance();
        this.configurations = new Configurations(CommonPropertyUtils.getProperties());
        this.dataQualityResultOperator = SpringApplicationContext.getBean(DataQualityResultOperator.class);
        this.notificationClient =  SpringApplicationContext.getBean(NotificationClient.class);
        this.dataSourceService =  SpringApplicationContext.getBean(DataSourceService.class);
        this.slaNotificationService = SpringApplicationContext.getBean(SlaNotificationService.class);
    }

    public void start() {
        JobExecutionExecutor jobExecutionExecutor = new JobExecutionExecutor();
        executorService.submit(jobExecutionExecutor);
        logger.info("job executor start");

        JobExecutionResponseOperator responseOperator = new JobExecutionResponseOperator();
        executorService.submit(responseOperator);
        logger.info("job execution response operator start");
    }

    public void addKillCommand(Long jobExecutionId){
        CommandContext commandContext = new CommandContext();
        commandContext.setCommandCode(CommandCode.JOB_KILL_REQUEST);
        commandContext.setJobExecutionId(jobExecutionId);
        jobExecutionQueue.offer(commandContext);
    }

    private JobExecutionRequest getExecutingJobExecution(Long jobExecutionId){
        return unFinishedJobExecutionMap.get(jobExecutionId);
    }

    class JobExecutionExecutor implements Runnable {

        @Override
        public void run() {
            while(Stopper.isRunning()) {
                try {
                    CommandContext commandContext = jobExecutionQueue.take();
                    Long jobExecutionId = commandContext.getJobExecutionId();
                    JobExecutionRequest jobExecutionRequest = commandContext.getJobExecutionRequest();
                    if (unFinishedJobExecutionMap.get(jobExecutionId) == null) {
                        continue;
                    }
                    switch(commandContext.getCommandCode()){
                        case JOB_EXECUTE_REQUEST:
                            executeJobExecution(jobExecutionRequest);
                            wheelTimer.newTimeout(
                                    new JobExecutionTimeoutTimerTask(
                                            jobExecutionId, jobExecutionRequest.getRetryTimes()),
                                            jobExecutionRequest.getTimeout()+2, TimeUnit.SECONDS);
                            break;
                        case JOB_KILL_REQUEST:
                            doKillCommand(jobExecutionId);
                            break;
                        default:
                            break;
                    }

                } catch(Exception e) {
                    logger.error("dispatcher job error",e);
                    ThreadUtils.sleep(2000);
                }
            }
        }
    }

    private void executeJobExecution(JobExecutionRequest jobExecutionRequest) {
        String execLocalPath = getExecLocalPath(jobExecutionRequest);
        try {
            FileUtils.createWorkDirAndUserIfAbsent(execLocalPath, jobExecutionRequest.getTenantCode());
        } catch (Exception ex){
            logger.error(String.format("create execLocal path : %s", execLocalPath), ex);
        }
        jobExecutionRequest.setExecuteFilePath(execLocalPath);
        Path path  = new File(execLocalPath).toPath();

        try {
            if (Files.exists(path)) {
                Files.delete(path);
            }
        } catch(IOException e) {
            logger.info("delete path error {0}",e);
        }

        jobExecutionRequest.setJobExecutionUniqueId(
                buildJobExecutionUniqueId(
                        jobExecutionRequest.getExecutePlatformType(),
                        jobExecutionRequest.getEngineType(),
                        jobExecutionRequest.getJobExecutionId()));
        jobExecutionRequest.setStartTime(LocalDateTime.now());
        jobExecutionRequest.setTimeout(jobExecutionRequest.getTimeout()*2);
        jobExecutionRequest.setExecuteHost(NetUtils.getAddr(
                CommonPropertyUtils.getInt(CommonPropertyUtils.SERVER_PORT, CommonPropertyUtils.SERVER_PORT_DEFAULT)));
        doAck(jobExecutionRequest);

        JobRunner jobRunner = new JobRunner(jobExecutionRequest, this, configurations);
        JobExecutionContext jobExecutionContext = new JobExecutionContext();
        jobExecutionContext.setJobExecutionRequest(jobExecutionRequest);
        jobExecutionContext.setJobRunner(jobRunner);
        jobExecutionCache.cache(jobExecutionContext);
        jobExecuteService.submit(jobRunner);
    }

    private String buildJobExecutionUniqueId(String executePlatformType, String engineType, long jobExecutionId){
        // LOCAL_SPARK_1521012323213]
        return String.format("%s_%s_%s",
                executePlatformType.toLowerCase(),
                engineType.toLowerCase(),
                jobExecutionId);
    }

    private void doAck(JobExecutionRequest jobExecutionRequest){
        processJobExecutionAckResponse(buildAckCommand(jobExecutionRequest));
    }

    private JobExecuteAckCommand buildAckCommand(JobExecutionRequest jobExecutionRequest) {
        JobExecuteAckCommand ackCommand = new JobExecuteAckCommand(jobExecutionRequest.getJobExecutionId());
        ackCommand.setStatus(ExecutionStatus.RUNNING_EXECUTION.getCode());
        ackCommand.setLogPath(getJobExecutionLogPath(jobExecutionRequest));
        ackCommand.setHost(jobExecutionRequest.getExecuteHost());
        ackCommand.setStartTime(LocalDateTime.now());
        ackCommand.setExecutePath(jobExecutionRequest.getExecuteFilePath());
        jobExecutionRequest.setLogPath(ackCommand.getLogPath());
        return ackCommand;
    }

    /**
     * get job log path
     * @return log path
     */
    private String getJobExecutionLogPath(JobExecutionRequest jobExecutionRequest) {
        String baseLog = ((JobExecutionLogDiscriminator) ((SiftingAppender) ((LoggerContext) LoggerFactory.getILoggerFactory())
                .getLogger("ROOT")
                .getAppender("JOB_LOG_FILE"))
                .getDiscriminator()).getLogBase();
        if (baseLog.startsWith(CommonConstants.SINGLE_SLASH)){
            return baseLog
                    + CommonConstants.SINGLE_SLASH + LoggerUtils.JOB_LOGGER_INFO_PREFIX.toLowerCase()
                    + CommonConstants.SINGLE_SLASH + DateUtils.format(LocalDateTime.now(),DateUtils.YYYYMMDD)
                    + CommonConstants.SINGLE_SLASH + jobExecutionRequest.getJobExecutionUniqueId() + ".log";
        }

        return System.getProperty("user.dir")
                + CommonConstants.SINGLE_SLASH + baseLog
                + CommonConstants.SINGLE_SLASH + LoggerUtils.JOB_LOGGER_INFO_PREFIX.toLowerCase()
                + CommonConstants.SINGLE_SLASH + DateUtils.format(LocalDateTime.now(),DateUtils.YYYYMMDD)
                + CommonConstants.SINGLE_SLASH + jobExecutionRequest.getJobExecutionUniqueId() + ".log";
    }

    /**
     * get execute local path
     * @param jobExecutionRequest executionJob
     * @return execute local path
     */
    private String getExecLocalPath(JobExecutionRequest jobExecutionRequest){
        return FileUtils.getJobExecutionExecDir(
                jobExecutionRequest.getEngineType(),
                jobExecutionRequest.getJobExecutionId());
    }

    /**
     * operate the job response
     */
    class JobExecutionResponseOperator implements Runnable {

        @Override
        public void run() {
            while (Stopper.isRunning()) {
                try {
                    JobExecutionResponseContext jobExecutionResponse = responseQueue.take();
                    JobExecutionRequest jobExecutionRequest = jobExecutionResponse.getJobExecutionRequest();
                    JobExecution jobExecution = jobExternalService.getJobExecutionById(jobExecutionRequest.getJobExecutionId());
                    switch (jobExecutionResponse.getCommandCode()) {
                        case JOB_EXECUTE_ACK:
                            if (jobExecution != null) {
                                jobExecution.setStartTime(jobExecutionRequest.getStartTime());
                                jobExecution.setStatus(ExecutionStatus.of(jobExecutionRequest.getStatus()));
                                jobExecution.setExecuteFilePath(jobExecutionRequest.getExecuteFilePath());
                                jobExecution.setLogPath(jobExecutionRequest.getLogPath());
                                jobExecution.setApplicationIdTag(jobExecutionRequest.getJobExecutionUniqueId());
                                jobExecution.setExecuteHost(jobExecutionRequest.getExecuteHost());
                                jobExternalService.updateJobExecution(jobExecution);
                            }
                            break;
                        case JOB_EXECUTE_RESPONSE:
                            logger.info("jobExecution execute response: " + JSONUtils.toJsonString(jobExecutionRequest));
                            unFinishedJobExecutionMap.put(jobExecutionRequest.getJobExecutionId(), jobExecutionRequest);
                            if (ExecutionStatus.of(jobExecutionRequest.getStatus()).typeIsSuccess()) {
                                unFinishedJobExecutionMap.remove(jobExecutionRequest.getJobExecutionId());
                                jobExecution.setApplicationId(jobExecutionRequest.getApplicationId());
                                jobExecution.setProcessId(jobExecutionRequest.getProcessId());
                                jobExecution.setExecuteHost(jobExecutionRequest.getExecuteHost());
                                jobExecution.setEndTime(jobExecutionRequest.getEndTime());
                                jobExecution.setStatus(ExecutionStatus.of(jobExecutionRequest.getStatus()));
                                jobExternalService.updateJobExecution(jobExecution);
                                dataQualityResultOperator.operateDqExecuteResult(jobExecutionRequest);
                            } else if (ExecutionStatus.of(jobExecutionRequest.getStatus()).typeIsFailure()) {
                                int retryNum = jobExecution.getRetryTimes();
                                if (jobExecution.getRetryTimes() > 0) {
                                    logger.info("retry job execution: " + JSONUtils.toJsonString(jobExecution));
                                    CommandContext commandContext = new CommandContext();
                                    commandContext.setJobExecutionRequest(jobExternalService.buildJobExecutionRequest(jobExecution));
                                    commandContext.setJobExecutionId(jobExecutionRequest.getJobExecutionId());
                                    commandContext.setCommandCode(CommandCode.JOB_EXECUTE_REQUEST);
                                    jobExecutionQueue.offer(commandContext);
                                    jobExternalService.updateJobExecutionRetryTimes(jobExecutionRequest.getJobExecutionId(), retryNum - 1);
                                    jobExternalService.deleteJobExecutionResultByJobExecutionId(jobExecutionRequest.getJobExecutionId());
                                    jobExternalService.deleteActualValuesByJobExecutionId(jobExecutionRequest.getJobExecutionId());
                                } else {
                                    sendErrorEmail(jobExecution);
                                    updateJobExecutionAndRemoveCache(jobExecutionRequest, jobExecution);
                                }
                            } else if(ExecutionStatus.of(jobExecutionRequest.getStatus()).typeIsCancel()) {
                                updateJobExecutionAndRemoveCache(jobExecutionRequest, jobExecution);
                            } else if(ExecutionStatus.of(jobExecutionRequest.getStatus()).typeIsRunning()) {
                                // do nothing
                            }

                            break;
                        default:
                            break;
                    }
                } catch(Exception e) {
                    logger.info("operate job response error {0}",e);
                }
            }
        }
    }

    private void sendErrorEmail(JobExecution jobExecution){
        LinkedList<String> messageList = new LinkedList<>();

        SlaNotificationMessage message = new SlaNotificationMessage();
        Long jobId = jobExecution.getJobId();
        JobService jobService = jobExternalService.getJobService();
        Job job = jobService.getById(jobId);
        String jobName = job.getName();
        Long dataSourceId = job.getDataSourceId();
        DataSource dataSource = dataSourceService.getDataSourceById(dataSourceId);
        String dataSourceName = dataSource.getName();
        String dataSourceType = dataSource.getType();
        messageList.add(String.format("job %s on %s datasource %s failure, please check detail", jobName, dataSourceType, dataSourceName));
        message.setSubject(String.format("datavines job %s execute failure", jobName));
        String jsonMessage = JsonUtils.toJsonString(messageList);
        message.setMessage(jsonMessage);

        Map<SlaSenderMessage, Set<SlaConfigMessage>> config = slaNotificationService.getSlasNotificationConfigurationByJobId(jobId);
        if (config.isEmpty()){
            return;
        }
        notificationClient.notify(message, config);
    }

    private void updateJobExecutionAndRemoveCache(JobExecutionRequest jobExecutionRequest, JobExecution jobExecution) {
        unFinishedJobExecutionMap.remove(jobExecutionRequest.getJobExecutionId());
        jobExternalService.deleteJobExecutionResultByJobExecutionId(jobExecutionRequest.getJobExecutionId());
        jobExternalService.deleteActualValuesByJobExecutionId(jobExecutionRequest.getJobExecutionId());
        jobExecution.setApplicationId(jobExecutionRequest.getApplicationId());
        jobExecution.setProcessId(jobExecutionRequest.getProcessId());
        jobExecution.setExecuteHost(jobExecutionRequest.getExecuteHost());
        jobExecution.setEndTime(jobExecutionRequest.getEndTime());
        jobExecution.setStatus(ExecutionStatus.of(jobExecutionRequest.getStatus()));
        jobExternalService.updateJobExecution(jobExecution);
    }

    /**
     * put the response into queue
     * @param jobExecutionResponseContext jobExecutionResponseContext
     */
    private void putResponse(JobExecutionResponseContext jobExecutionResponseContext){
        responseQueue.offer(jobExecutionResponseContext);
    }

    class JobExecutionTimeoutTimerTask implements TimerTask {

        private final long jobExecutionId;
        private final int retryTimes;

        public JobExecutionTimeoutTimerTask(long jobExecutionId, int retryTimes){
            this.jobExecutionId = jobExecutionId;
            this.retryTimes = retryTimes;
        }

        @Override
        public void run(Timeout timeout) throws Exception {
            JobExecutionRequest jobExecutionRequest = unFinishedJobExecutionMap.get(this.jobExecutionId);
            if (jobExecutionRequest == null) {
                logger.info("jobExecution {} is finished, do nothing...",jobExecutionId);
                return;
            }

            if (this.retryTimes != jobExecutionRequest.getRetryTimes()) {
                logger.info("jobExecution {} is finished, do nothing...",jobExecutionId);
                return;
            }

            logger.info("jobExecution {} is timeout, do something",jobExecutionId);

        }
    }

    private void doKillCommand(Long jobExecutionId) {
        JobExecutionContext jobExecutionContext = jobExecutionCache.getById(jobExecutionId);

        if (jobExecutionContext != null){
            JobRunner jobRunner = jobExecutionContext.getJobRunner();
            jobRunner.kill();
        } else {
            unFinishedJobExecutionMap.remove(jobExecutionId);
            JobExecution jobExecution = jobExternalService.getJobExecutionById(jobExecutionId);
            if (jobExecution != null) {
                jobExecution.setEndTime(LocalDateTime.now());
                jobExecution.setStatus(ExecutionStatus.KILL);
                jobExternalService.updateJobExecution(jobExecution);
            }
        }
    }

    public void processJobExecutionExecuteResponse(JobExecuteResponseCommand jobExecuteResponseCommand) {

        JobExecutionRequest jobExecutionRequest = getExecutingJobExecution(jobExecuteResponseCommand.getJobExecutionId());
        if(jobExecutionRequest == null){
            jobExecutionRequest =  new JobExecutionRequest();
        }

        jobExecutionRequest.setJobExecutionId(jobExecuteResponseCommand.getJobExecutionId());
        jobExecutionRequest.setEndTime(jobExecuteResponseCommand.getEndTime());
        jobExecutionRequest.setStatus(jobExecuteResponseCommand.getStatus());
        jobExecutionRequest.setApplicationId(jobExecuteResponseCommand.getApplicationIds());
        jobExecutionRequest.setProcessId(jobExecuteResponseCommand.getProcessId());

        JobExecutionResponseContext jobExecutionResponseContext = new JobExecutionResponseContext(CommandCode.JOB_EXECUTE_RESPONSE, jobExecutionRequest);

        putResponse(jobExecutionResponseContext);
    }

    private void processJobExecutionAckResponse(JobExecuteAckCommand jobExecuteAckCommand) {
        JobExecutionRequest jobExecutionRequest = getExecutingJobExecution(jobExecuteAckCommand.getJobExecutionId());
        if (jobExecutionRequest == null) {
            jobExecutionRequest =  new JobExecutionRequest();
        }

        jobExecutionRequest.setJobExecutionId(jobExecuteAckCommand.getJobExecutionId());
        jobExecutionRequest.setStartTime(jobExecuteAckCommand.getStartTime());
        jobExecutionRequest.setStatus(jobExecuteAckCommand.getStatus());
        jobExecutionRequest.setExecuteHost(jobExecuteAckCommand.getHost());
        jobExecutionRequest.setLogPath(jobExecuteAckCommand.getLogPath());
        jobExecutionRequest.setExecuteFilePath(jobExecuteAckCommand.getExecutePath());

        JobExecutionResponseContext jobExecutionResponseContext = new JobExecutionResponseContext(CommandCode.JOB_EXECUTE_ACK, jobExecutionRequest);

        putResponse(jobExecutionResponseContext);
    }

    public void close(){
        if (executorService != null) {
            executorService.shutdown();
        }

        if (jobExecuteService != null) {
            jobExecuteService.shutdown();
        }
    }

    public void addFailoverJobExecutionRequest(JobExecution jobExecution) throws DataVinesException {
        JobExecutionRequest jobExecutionRequest = getJobExecutionRequest(jobExecution);
        unFinishedJobExecutionMap.put(jobExecutionRequest.getJobExecutionId(), jobExecutionRequest);
    }

    private JobExecutionRequest getJobExecutionRequest(JobExecution jobExecution) {
        JobExecutionRequest jobExecutionRequest;
        try {
            jobExecutionRequest = buildJobExecutionRequest(jobExecution);
        } catch (Exception e) {
            logger.error("build job execution request error : {}", e);
            jobExecution.setEndTime(LocalDateTime.now());
            jobExecution.setStatus(ExecutionStatus.FAILURE);
            jobExternalService.updateJobExecution(jobExecution);
            throw e;
        }
        return jobExecutionRequest;
    }

    public void addExecuteCommand(JobExecution jobExecution){
        JobExecutionRequest jobExecutionRequest = getJobExecutionRequest(jobExecution);

        logger.info("put into wait queue to send {}", JSONUtils.toJsonString(jobExecutionRequest));
        unFinishedJobExecutionMap.put(jobExecutionRequest.getJobExecutionId(), jobExecutionRequest);
        CommandContext commandContext = new CommandContext();
        commandContext.setCommandCode(CommandCode.JOB_EXECUTE_REQUEST);
        commandContext.setJobExecutionId(jobExecutionRequest.getJobExecutionId());
        commandContext.setJobExecutionRequest(jobExecutionRequest);
        jobExecutionQueue.offer(commandContext);
    }

    private JobExecutionRequest buildJobExecutionRequest(JobExecution jobExecution) throws DataVinesException {
        // need to convert job parameter to other parameter
        JobExecutionRequest jobExecutionRequest = new JobExecutionRequest();
        jobExecutionRequest.setJobExecutionId(jobExecution.getId());
        jobExecutionRequest.setJobExecutionName(jobExecution.getName());
        JobExecutionParameter jobExecutionParameter = JSONUtils.parseObject(jobExecution.getParameter(),JobExecutionParameter.class);
        if (jobExecutionParameter == null) {
            throw new DataVinesException("JobExecutionParameter can not be null");
        }

        jobExecutionRequest.setExecutePlatformType(jobExecution.getExecutePlatformType());
        jobExecutionRequest.setExecutePlatformParameter(jobExecution.getExecutePlatformParameter());
        jobExecutionRequest.setEngineType(jobExecution.getEngineType());
        jobExecutionRequest.setEngineParameter(jobExecution.getEngineParameter());
        jobExecutionRequest.setErrorDataStorageType(jobExecution.getErrorDataStorageType());
        jobExecutionRequest.setErrorDataStorageParameter(jobExecution.getErrorDataStorageParameter());
        Map<String,String> inputParameter = new HashMap<>();

        JobExecutionInfo jobExecutionInfo = new JobExecutionInfo(
                jobExecution.getId(), jobExecution.getName(),
                jobExecution.getEngineType(), jobExecution.getEngineParameter(),
                jobExecution.getErrorDataStorageType(), jobExecution.getErrorDataStorageParameter(), jobExecution.getErrorDataFileName(),
                "mysql", JSONUtils.toJsonString(DefaultDataSourceInfoUtils.getDefaultDataSourceConfigMap()),
                jobExecutionParameter);
        DataVinesJobConfig qualityConfig =
                DataVinesConfigurationManager.generateConfiguration(jobExecution.getJobType(),inputParameter, jobExecutionInfo);

        jobExecutionRequest.setExecuteFilePath(jobExecution.getExecuteFilePath());
        jobExecutionRequest.setLogPath(jobExecution.getLogPath());
        jobExecutionRequest.setJobExecutionUniqueId(jobExecution.getApplicationIdTag());
        jobExecutionRequest.setStatus(jobExecution.getStatus().getCode());
        jobExecutionRequest.setExecuteHost(NetUtils.getAddr(CommonPropertyUtils.getInt(CommonPropertyUtils.SERVER_PORT, CommonPropertyUtils.SERVER_PORT_DEFAULT)));
        jobExecutionRequest.setApplicationParameter(JSONUtils.toJsonString(qualityConfig));
        jobExecutionRequest.setTenantCode(jobExecution.getTenantCode());
        jobExecutionRequest.setRetryTimes(jobExecution.getRetryTimes());
        jobExecutionRequest.setRetryInterval(jobExecution.getRetryInterval());
        jobExecutionRequest.setTimeout(jobExecution.getTimeout());
        jobExecutionRequest.setTimeoutStrategy(jobExecution.getTimeoutStrategy());
        jobExecutionRequest.setEnv(jobExecution.getEnv());
        jobExecutionRequest.setApplicationId(jobExecution.getApplicationId());
        jobExecutionRequest.setProcessId(jobExecution.getProcessId());
        return jobExecutionRequest;
    }
}
