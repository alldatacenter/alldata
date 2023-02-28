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
package io.datavines.server.dqc.executor.runner;

import java.time.LocalDateTime;

import io.datavines.common.config.Configurations;
import io.datavines.common.entity.JobExecutionRequest;
import io.datavines.common.enums.ExecutionStatus;
import io.datavines.common.utils.LoggerUtils;
import io.datavines.engine.api.engine.EngineExecutor;
import io.datavines.server.dqc.coordinator.cache.JobExecuteManager;
import io.datavines.server.dqc.command.JobExecuteResponseCommand;
import io.datavines.spi.PluginLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobRunner implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(JobRunner.class);

    private final JobExecutionRequest jobExecutionRequest;

    private final JobExecuteManager jobExecuteManager;

    private EngineExecutor engineExecutor;

    private final Configurations configurations;

    public JobRunner(JobExecutionRequest jobExecutionRequest, JobExecuteManager jobExecuteManager, Configurations configurations){
        this.jobExecutionRequest = jobExecutionRequest;
        this.jobExecuteManager = jobExecuteManager;
        this.configurations = configurations;
    }

    @Override
    public void run() {
        JobExecuteResponseCommand responseCommand =
                new JobExecuteResponseCommand(this.jobExecutionRequest.getJobExecutionId());
        try {
            String taskLoggerName = LoggerUtils.buildJobExecutionLoggerName(
                    LoggerUtils.JOB_LOGGER_INFO_PREFIX,
                    jobExecutionRequest.getJobExecutionUniqueId());

            // custom logger
            Logger taskLogger = LoggerFactory.getLogger(taskLoggerName);
            Thread.currentThread().setName(taskLoggerName);

            engineExecutor = PluginLoader
                    .getPluginLoader(EngineExecutor.class)
                    .getNewPlugin(jobExecutionRequest.getEngineType());

            engineExecutor.init(jobExecutionRequest, taskLogger, configurations);
            engineExecutor.execute();
            engineExecutor.after();

            if (engineExecutor.isCancel()) {
                responseCommand.setStatus(ExecutionStatus.KILL.getCode());
            } else {
                responseCommand.setStatus(engineExecutor.getProcessResult().getExitStatusCode());
            }

            responseCommand.setEndTime(LocalDateTime.now());
            responseCommand.setApplicationIds(engineExecutor.getProcessResult().getApplicationId());
            responseCommand.setProcessId(engineExecutor.getProcessResult().getProcessId());

        } catch (Exception e) {
            logger.error("task execute failure", e);
            kill();
            try {
                if (engineExecutor.isCancel()) {
                    responseCommand.setStatus(ExecutionStatus.KILL.getCode());
                } else {
                    responseCommand.setStatus(ExecutionStatus.FAILURE.getCode());
                }
            } catch (Exception ex) {
                logger.error("task execute failure", ex);
            }

            responseCommand.setEndTime(LocalDateTime.now());
            responseCommand.setApplicationIds(engineExecutor.getProcessResult().getApplicationId());
            responseCommand.setProcessId(engineExecutor.getProcessResult().getProcessId());
        } finally {
            jobExecuteManager.processJobExecutionExecuteResponse(responseCommand);
        }
    }

    /**
     *  kill job
     */
    public void kill(){
        if (engineExecutor != null) {
            try {
                engineExecutor.cancel();
                JobExecuteResponseCommand responseCommand =
                        new JobExecuteResponseCommand(this.jobExecutionRequest.getJobExecutionId());
                responseCommand.setStatus(ExecutionStatus.KILL.getCode());
                responseCommand.setEndTime(LocalDateTime.now());
                responseCommand.setApplicationIds(engineExecutor.getProcessResult().getApplicationId());
                responseCommand.setProcessId(engineExecutor.getProcessResult().getProcessId());
                jobExecuteManager.processJobExecutionExecuteResponse(responseCommand);

            } catch (Exception e) {
                logger.error(e.getMessage(),e);
            }
        }
    }
}
