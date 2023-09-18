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
package io.datavines.server.dqc.coordinator.failover;

import io.datavines.common.enums.ExecutionStatus;
import io.datavines.common.utils.CommonPropertyUtils;
import io.datavines.common.utils.NetUtils;
import io.datavines.common.utils.Stopper;
import io.datavines.common.utils.YarnUtils;
import io.datavines.server.dqc.command.JobExecuteResponseCommand;
import io.datavines.common.exception.DataVinesException;
import io.datavines.server.dqc.coordinator.cache.JobExecuteManager;
import io.datavines.server.repository.entity.JobExecution;
import io.datavines.server.repository.service.impl.JobExternalService;
import io.datavines.server.utils.SpringApplicationContext;
import io.datavines.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class JobExecutionFailover {

    private final JobExternalService jobExternalService;

    private final ConcurrentHashMap<Long,JobExecution> needCheckStatusJobExecutionMap = new ConcurrentHashMap<>();

    private final JobExecuteManager jobExecuteManager;

    private final ScheduledExecutorService executorService;

    private final Integer SERVER_PORT =
            CommonPropertyUtils.getInt(CommonPropertyUtils.SERVER_PORT, CommonPropertyUtils.SERVER_PORT_DEFAULT);

    public JobExecutionFailover(JobExecuteManager jobExecuteManager){
        this.jobExternalService = SpringApplicationContext.getBean(JobExternalService.class);
        this.jobExecuteManager = jobExecuteManager;
        executorService = Executors.newScheduledThreadPool(2);
        executorService.scheduleAtFixedRate(new YarnJobExecutionStatusChecker(),0,4, TimeUnit.SECONDS);
    }

    public void handleJobExecutionFailover(String host) {
        List<JobExecution> jobExecutionList = jobExternalService.getJobExecutionListNeedFailover(host);
        if (CollectionUtils.isEmpty(jobExecutionList)) {
            return;
        }

        innerHandleJobExecutionFailover(jobExecutionList);
    }

    public void handleJobExecutionFailover(List<String> hostList) {
        List<JobExecution> jobExecutionList = jobExternalService.getJobExecutionListNeedFailover(hostList);
        if (CollectionUtils.isEmpty(jobExecutionList)) {
            return;
        }

        innerHandleJobExecutionFailover(jobExecutionList);
    }

    private void innerHandleJobExecutionFailover(List<JobExecution> jobExecutionList) {
        List<JobExecution> needRerunJobExecutionList = new ArrayList<>();

        jobExecutionList.forEach(jobExecution -> {
            if (StringUtils.isNotEmpty(jobExecution.getApplicationId())) {
                try {
                    jobExecuteManager.addFailoverJobExecutionRequest(jobExecution);
                } catch (DataVinesException e) {
                    log.error("add failover job execution error : ", e);
                }
                jobExecution.setExecuteHost(NetUtils.getAddr(SERVER_PORT));
                jobExternalService.updateJobExecution(jobExecution);
                needCheckStatusJobExecutionMap.put(jobExecution.getId(), jobExecution);
            } else {
                String appId = YarnUtils.getYarnAppId(jobExecution.getTenantCode(), jobExecution.getApplicationIdTag());
                if (StringUtils.isNotEmpty(appId)) {
                    try {
                        jobExecuteManager.addFailoverJobExecutionRequest(jobExecution);
                    } catch (DataVinesException e) {
                        log.error("add failover job execution error : ", e);
                    }
                    jobExecution.setApplicationId(appId);
                    jobExecution.setExecuteHost(NetUtils.getAddr(SERVER_PORT));
                    jobExternalService.updateJobExecution(jobExecution);
                    needCheckStatusJobExecutionMap.put(jobExecution.getId(), jobExecution);
                } else {
                    needRerunJobExecutionList.add(jobExecution);
                }
            }
        });

        handleRerunJobExecution(needRerunJobExecutionList);
    }

    private void handleRerunJobExecution(List<JobExecution> needRerunJobExecutionList) {
        needRerunJobExecutionList.forEach(jobExecution -> jobExecuteManager.addExecuteCommand(jobExecution));
    }

    class YarnJobExecutionStatusChecker implements Runnable {

        @Override
        public void run() {

            if (Stopper.isRunning() && needCheckStatusJobExecutionMap.size() > 0) {
                needCheckStatusJobExecutionMap.forEach((k,v) ->{
                    JobExecuteResponseCommand responseCommand =
                            new JobExecuteResponseCommand(v.getId());
                    responseCommand.setEndTime(LocalDateTime.now());
                    ExecutionStatus applicationStatus = YarnUtils.getApplicationStatus(v.getApplicationId());
                    if (applicationStatus != null) {
                        log.info("appId:{}, final state:{}", v.getApplicationId(), applicationStatus.name());
                        if (applicationStatus.equals(ExecutionStatus.FAILURE) ||
                                applicationStatus.equals(ExecutionStatus.KILL) ||
                                applicationStatus.equals(ExecutionStatus.SUCCESS)) {
                            responseCommand.setStatus(applicationStatus.getCode());
                            responseCommand.setApplicationIds(v.getApplicationId());
                            jobExecuteManager.processJobExecutionExecuteResponse(responseCommand);
                            needCheckStatusJobExecutionMap.remove(k);
                        }
                    }
                });
            }
        }
    }

    public void close() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
