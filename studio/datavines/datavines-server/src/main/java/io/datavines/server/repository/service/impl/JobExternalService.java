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
package io.datavines.server.repository.service.impl;

import io.datavines.common.config.DataVinesJobConfig;
import io.datavines.common.entity.JobExecutionInfo;
import io.datavines.common.entity.JobExecutionParameter;
import io.datavines.common.entity.JobExecutionRequest;
import io.datavines.common.enums.ExecutionStatus;
import io.datavines.common.utils.JSONUtils;
import io.datavines.engine.config.DataVinesConfigurationManager;
import io.datavines.common.exception.DataVinesException;
import io.datavines.server.repository.entity.Command;
import io.datavines.server.repository.entity.Job;
import io.datavines.server.repository.entity.JobExecution;
import io.datavines.server.repository.entity.JobExecutionResult;
import io.datavines.server.repository.entity.catalog.CatalogMetaDataFetchCommand;
import io.datavines.server.repository.entity.catalog.CatalogMetaDataFetchTask;
import io.datavines.server.repository.service.*;
import io.datavines.server.utils.DefaultDataSourceInfoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JobExternalService {
    
    @Autowired
    private JobExecutionService jobExecutionService;

    @Autowired
    private CommandService commandService;

    @Autowired
    private JobService jobService;

    @Autowired
    private JobExecutionResultService jobExecutionResultService;

    @Autowired
    private ActualValuesService actualValuesService;

    @Autowired
    private CatalogMetaDataFetchCommandService catalogMetaDataFetchCommandService;

    @Autowired
    private CatalogMetaDataFetchTaskService catalogMetaDataFetchTaskService;

    @Autowired
    private DataSourceService dataSourceService;

    public Job getJobById(Long id) {
        return jobService.getById(id);
    }

    public JobExecution getJobExecutionById(Long id){
        return jobExecutionService.getById(id);
    }

    public Command getCommand(){
        return commandService.getOne();
    }

    public CatalogMetaDataFetchCommand getCatalogCommand(){
        return catalogMetaDataFetchCommandService.getOne();
    }

    public int deleteCommandById(long id){
        return commandService.deleteById(id);
    }

    public int deleteCatalogCommandById(long id){
        return catalogMetaDataFetchCommandService.deleteById(id);
    }

    public JobExecution executeCommand(Command command){
        return jobExecutionService.getById(command.getJobExecutionId());
    }

    public CatalogMetaDataFetchTask executeCatalogCommand(CatalogMetaDataFetchCommand command){
        return catalogMetaDataFetchTaskService.getById(command.getTaskId());
    }

    public int updateJobExecution(JobExecution jobExecution){
        return jobExecutionService.update(jobExecution);
    }

    public Long createJobExecution(JobExecution jobExecution){
        return jobExecutionService.create(jobExecution);
    }

    public Long insertCommand(Command command){
        return commandService.insert(command);
    }

    public int updateCommand(Command command){
        return commandService.update(command);
    }

    public void updateJobExecutionStatus(Long jobExecutionId, ExecutionStatus status){
        JobExecution jobExecution = getJobExecutionById(jobExecutionId);
        jobExecution.setStatus(status);
        updateJobExecution(jobExecution);
    }

    public void updateJobExecutionRetryTimes(Long jobExecutionId, int times) {
        JobExecution jobExecution = getJobExecutionById(jobExecutionId);
        jobExecution.setRetryTimes(times);
        updateJobExecution(jobExecution);
    }

    public JobExecutionRequest buildJobExecutionRequest(JobExecution jobExecution) throws DataVinesException {
        // need to convert job parameter to other parameter
        JobExecutionRequest jobExecutionRequest = new JobExecutionRequest();
        jobExecutionRequest.setJobExecutionId(jobExecution.getId());
        jobExecutionRequest.setJobExecutionName(jobExecution.getName());
        JobExecutionParameter jobExecutionParameter = JSONUtils.parseObject(jobExecution.getParameter(),JobExecutionParameter.class);
        if (jobExecutionParameter == null) {
            throw new DataVinesException("JobExecutionParameter can not be null");
        }

        jobExecutionRequest.setExecutePlatformType(jobExecution.getExecutePlatformType());
        //读取配置文件获取环境信息
        jobExecutionRequest.setExecutePlatformParameter(jobExecution.getExecutePlatformParameter());
        jobExecutionRequest.setEngineType(jobExecution.getEngineType());
        jobExecutionRequest.setEngineParameter(jobExecution.getEngineParameter());
        Map<String,String> inputParameter = new HashMap<>();

        JobExecutionInfo jobExecutionInfo = new JobExecutionInfo(
                jobExecution.getId(), jobExecution.getName(),
                jobExecution.getEngineType(), jobExecution.getEngineParameter(),
                jobExecution.getErrorDataStorageType(), jobExecution.getErrorDataStorageParameter(), jobExecution.getErrorDataFileName(),
                "mysql", JSONUtils.toJsonString(DefaultDataSourceInfoUtils.getDefaultDataSourceConfigMap()),
                jobExecutionParameter);
        DataVinesJobConfig qualityConfig =
                DataVinesConfigurationManager.generateConfiguration(jobExecution.getJobType(), inputParameter, jobExecutionInfo);
        jobExecutionRequest.setApplicationParameter(JSONUtils.toJsonString(qualityConfig));
        jobExecutionRequest.setTenantCode(jobExecution.getTenantCode());
        jobExecutionRequest.setRetryTimes(jobExecution.getRetryTimes());
        jobExecutionRequest.setRetryInterval(jobExecution.getRetryInterval());
        jobExecutionRequest.setTimeout(jobExecution.getTimeout());
        jobExecutionRequest.setTimeoutStrategy(jobExecution.getTimeoutStrategy());
        jobExecutionRequest.setEnv(jobExecution.getEnv());
        return jobExecutionRequest;
    }

    public JobExecutionResult getJobExecutionResultByJobExecutionId(long jobExecutionId) {
        return jobExecutionResultService.getByJobExecutionId(jobExecutionId);
    }

    public List<JobExecutionResult> listJobExecutionResultByJobExecutionId(long jobExecutionId) {
        return jobExecutionResultService.listByJobExecutionId(jobExecutionId);
    }

    public int deleteJobExecutionResultByJobExecutionId(long jobExecutionId) {
        return jobExecutionResultService.deleteByJobExecutionId(jobExecutionId);
    }

    public int deleteActualValuesByJobExecutionId(long jobExecutionId) {
        return actualValuesService.deleteByJobExecutionId(jobExecutionId);
    }

    public int updateJobExecutionResult(JobExecutionResult jobExecutionResult) {
        return jobExecutionResultService.update(jobExecutionResult);
    }

    public List<JobExecution> getJobExecutionListNeedFailover(String host){
        return jobExecutionService.listNeedFailover(host);
    }

    public List<JobExecution> getJobExecutionListNeedFailover(List<String> host){
        return jobExecutionService.listJobExecutionNotInServerList(host);
    }

    public JobService getJobService() {
        return jobService;
    }

    public DataSourceService getDataSourceService() {
        return dataSourceService;
    }

    public CatalogMetaDataFetchTaskService getCatalogTaskService() {
        return catalogMetaDataFetchTaskService;
    }

    public JobExecutionResultService getJobExecutionResultService() {
        return jobExecutionResultService;
    }
}
