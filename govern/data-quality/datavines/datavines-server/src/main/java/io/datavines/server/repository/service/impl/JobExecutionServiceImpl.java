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

import java.time.LocalDateTime;
import java.util.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.datavines.common.entity.JobExecutionParameter;
import io.datavines.common.exception.DataVinesException;
import io.datavines.common.param.ExecuteRequestParam;
import io.datavines.core.enums.Status;
import io.datavines.metric.api.ResultFormula;
import io.datavines.common.entity.job.SubmitJob;
import io.datavines.server.api.dto.vo.JobExecutionVO;
import io.datavines.core.exception.DataVinesServerException;
import io.datavines.server.api.dto.vo.MetricExecutionDashBoard;
import io.datavines.server.repository.entity.JobExecution;
import io.datavines.server.repository.entity.JobExecutionResult;
import io.datavines.server.repository.service.ActualValuesService;
import io.datavines.server.repository.service.CommandService;
import io.datavines.server.repository.service.JobExecutionResultService;
import io.datavines.server.repository.entity.Command;
import io.datavines.server.repository.mapper.JobExecutionMapper;
import io.datavines.server.repository.service.JobExecutionService;
import io.datavines.spi.PluginLoader;
import io.datavines.storage.api.StorageFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import io.datavines.common.enums.ExecutionStatus;
import io.datavines.common.utils.JSONUtils;
import io.datavines.server.enums.CommandType;
import io.datavines.server.enums.Priority;
import org.springframework.transaction.annotation.Transactional;

import static io.datavines.core.constant.DataVinesConstants.SPARK;

@Service("jobExecutionService")
public class JobExecutionServiceImpl extends ServiceImpl<JobExecutionMapper, JobExecution>  implements JobExecutionService {

    @Autowired
    private CommandService commandService;

    @Autowired
    private JobExecutionResultService jobExecutionResultService;

    @Autowired
    private ActualValuesService actualValuesService;

    @Override
    public long create(JobExecution jobExecution) {
        baseMapper.insert(jobExecution);
        return jobExecution.getId();
    }

    @Override
    public int update(JobExecution jobExecution) {
        return baseMapper.updateById(jobExecution);
    }

    @Override
    public JobExecution getById(long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public List<JobExecution> listByJobId(long jobId) {
        return baseMapper.listByJobId(jobId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByJobId(long jobId) {
        List<JobExecution> jobExecutionList = listByJobId(jobId);
        if (CollectionUtils.isEmpty(jobExecutionList)) {
            return 0;
        }

        jobExecutionList.forEach(task -> {
            baseMapper.deleteById(task.getId());
            jobExecutionResultService.deleteByJobExecutionId(task.getId());
            actualValuesService.deleteByJobExecutionId(task.getId());
        });

        return 0;
    }

    @Override
    public IPage<JobExecutionVO> getJobExecutionPage(String searchVal, Long jobId, Integer pageNumber, Integer pageSize) {
        Page<JobExecutionVO> page = new Page<>(pageNumber, pageSize);
        IPage<JobExecutionVO> jobs = baseMapper.getJobExecutionPage(page, searchVal, jobId);
        return jobs;
    }

    @Override
    public Long submitJob(SubmitJob submitJob) throws DataVinesServerException {

        checkJobExecutionParameter(submitJob.getParameter(), submitJob.getEngineType());

        JobExecution jobExecution = new JobExecution();
        BeanUtils.copyProperties(submitJob, jobExecution);
        jobExecution.setParameter(JSONUtils.toJsonString(submitJob.getParameter()));
        if (submitJob.getExecutePlatformParameter() != null) {
            jobExecution.setExecutePlatformParameter(JSONUtils.toJsonString(submitJob.getExecutePlatformParameter()));
        }

        if(SPARK.equals(jobExecution.getEngineType())) {
            Map<String,Object> defaultEngineParameter = new HashMap<>();
            defaultEngineParameter.put("programType", "JAVA");
            defaultEngineParameter.put("deployMode", "cluster");
            defaultEngineParameter.put("driverCores", 1);
            defaultEngineParameter.put("driverMemory", "512M");
            defaultEngineParameter.put("numExecutors", 2);
            defaultEngineParameter.put("executorMemory", "2G");
            defaultEngineParameter.put("executorCores", 2);
            defaultEngineParameter.put("others", "--conf spark.yarn.maxAppAttempts=1");

            if (submitJob.getEngineParameter() != null) {
                defaultEngineParameter.putAll(submitJob.getEngineParameter());
            }
            submitJob.setEngineParameter(defaultEngineParameter);
            jobExecution.setEngineParameter(JSONUtils.toJsonString(submitJob.getEngineParameter()));
        }

        jobExecution.setSubmitTime(LocalDateTime.now());
        jobExecution.setStatus(ExecutionStatus.SUBMITTED_SUCCESS);

        return executeJob(jobExecution);
    }

    @Override
    public Long executeJob(JobExecution jobExecution) throws DataVinesServerException {
        Long jobExecutionId = create(jobExecution);

        Command command = new Command();
        command.setType(CommandType.START);
        command.setPriority(Priority.MEDIUM);
        command.setJobExecutionId(jobExecutionId);
        commandService.insert(command);

        return jobExecutionId;
    }

    @Override
    public Long killJob(Long jobExecutionId) {
        Command command = new Command();
        command.setType(CommandType.STOP);
        command.setPriority(Priority.MEDIUM);
        command.setJobExecutionId(jobExecutionId);
        commandService.insert(command);

        return jobExecutionId;
    }

    @Override
    public List<JobExecution> listNeedFailover(String host) {
        return baseMapper.selectList(new QueryWrapper<JobExecution>()
                .eq("execute_host", host)
                .in("status", ExecutionStatus.RUNNING_EXECUTION.getCode(), ExecutionStatus.SUBMITTED_SUCCESS.getCode()));
    }

    @Override
    public List<JobExecution> listJobExecutionNotInServerList(List<String> hostList) {
        return baseMapper.selectList(new QueryWrapper<JobExecution>()
                .notIn("execute_host", hostList)
                .in("status",ExecutionStatus.RUNNING_EXECUTION.getCode(), ExecutionStatus.SUBMITTED_SUCCESS.getCode()));
    }

    private void checkJobExecutionParameter(JobExecutionParameter jobExecutionParameter, String engineType) throws DataVinesServerException {
//        String metricType = jobExecutionParameter.getMetricType();
//        Set<String> metricPluginSet = PluginLoader.getPluginLoader(SqlMetric.class).getSupportedPlugins();
//        if (!metricPluginSet.contains(metricType)) {
//            throw new DataVinesServerException(String.format("%s metric does not supported", metricType));
//        }
//
//        SqlMetric sqlMetric = PluginLoader.getPluginLoader(SqlMetric.class).getOrCreatePlugin(metricType);
//        CheckResult checkResult = sqlMetric.validateConfig(jobExecutionParameter.getMetricParameter());
//        if (checkResult== null || !checkResult.isSuccess()) {
//            throw new DataVinesServerException(checkResult== null? "check error": checkResult.getMsg());
//        }
//
//        String configBuilder = engineType + "_" + sqlMetric.getType().getDescription();
//        Set<String> configBuilderPluginSet = PluginLoader.getPluginLoader(JobConfigurationBuilder.class).getSupportedPlugins();
//        if (!configBuilderPluginSet.contains(configBuilder)) {
//            throw new DataVinesServerException(String.format("%s engine does not supported %s metric", engineType, metricType));
//        }
//
//        ConnectorParameter connectorParameter = jobExecutionParameter.getConnectorParameter();
//        if (connectorParameter != null) {
//            String connectorType = connectorParameter.getType();
//            Set<String> connectorFactoryPluginSet =
//                    PluginLoader.getPluginLoader(ConnectorFactory.class).getSupportedPlugins();
//            if (!connectorFactoryPluginSet.contains(connectorType)) {
//                throw new DataVinesServerException(String.format("%s connector does not supported", connectorType));
//            }
//
//            if (LOCAL.equals(engineType)) {
//                ConnectorFactory connectorFactory = PluginLoader.getPluginLoader(ConnectorFactory.class).getOrCreatePlugin(connectorType);
//                if (!JDBC.equals(connectorFactory.getCategory())) {
//                    throw new DataVinesServerException(String.format("jdbc engine does not supported %s connector", connectorType));
//                }
//            }
//        } else {
//            throw new DataVinesServerException("connector parameter should not be null");
//        }
//
//        String expectedMetric = jobExecutionParameter.getExpectedType();
//        Set<String> expectedValuePluginSet = PluginLoader.getPluginLoader(ExpectedValue.class).getSupportedPlugins();
//        if (!expectedValuePluginSet.contains(expectedMetric)) {
//            throw new DataVinesServerException(String.format("%s expected value does not supported", metricType));
//        }
//
//        String resultFormula = jobExecutionParameter.getResultFormula();
//        Set<String> resultFormulaPluginSet = PluginLoader.getPluginLoader(ResultFormula.class).getSupportedPlugins();
//        if (!resultFormulaPluginSet.contains(resultFormula)) {
//            throw new DataVinesServerException(String.format("%s result formula does not supported", metricType));
//        }
    }

    @Override
    public Object readErrorDataPage(Long taskId, Integer pageNumber, Integer pageSize)  {

        JobExecution jobExecution = getById(taskId);
        if (jobExecution == null) {
            throw new DataVinesServerException(Status.TASK_NOT_EXIST_ERROR, taskId);
        }

        String errorDataStorageType = jobExecution.getErrorDataStorageType();
        String errorDataStorageParameter = jobExecution.getErrorDataStorageParameter();
        String errorDataFileName = jobExecution.getErrorDataFileName();

        StorageFactory storageFactory =
                PluginLoader.getPluginLoader(StorageFactory.class).getOrCreatePlugin(errorDataStorageType);

        ExecuteRequestParam param = new ExecuteRequestParam();
        param.setType(errorDataStorageType);
        param.setDataSourceParam(errorDataStorageParameter);

        Map<String,String> scriptConfigMap = new HashMap<>();
        scriptConfigMap.put("error_data_file_name", errorDataFileName);
        param.setScript(storageFactory.getErrorDataScript(scriptConfigMap));
        param.setPageNumber(pageNumber);
        param.setPageSize(pageSize);

        Object result = null;
        try {
            result = storageFactory.getStorageExecutor().executeSyncQuery(param).getResult();
        } catch (Exception exception) {
            throw new DataVinesException(exception);
        }

        return result;
    }

    /**
     * get task host from taskId
     * @param taskId
     * @return
     * @throws DataVinesServerException
     */
    @Override
    public String getJobExecutionHost(Long taskId) {
        JobExecution jobExecution = baseMapper.selectById(taskId);
        if(null == jobExecution){
            throw new DataVinesServerException(Status.TASK_NOT_EXIST_ERROR, taskId);
        }
        String executeHost = jobExecution.getExecuteHost();
        if(StringUtils.isEmpty(executeHost)){
            throw new DataVinesServerException(Status.TASK_EXECUTE_HOST_NOT_EXIST_ERROR, taskId);
        }
        return executeHost;
    }

    @Override
    public List<MetricExecutionDashBoard> getMetricExecutionDashBoard(Long jobId, String startTime, String endTime) {

        List<MetricExecutionDashBoard> resultList = new ArrayList<>();

        List<JobExecutionResult> executionResults = jobExecutionResultService.listByJobIdAndTimeRange(jobId, startTime, endTime);
        if (CollectionUtils.isEmpty(executionResults)) {
            return resultList;
        }

        executionResults.forEach(result -> {
            ResultFormula resultFormula =
                    PluginLoader.getPluginLoader(ResultFormula.class).getOrCreatePlugin(result.getResultFormula());
            MetricExecutionDashBoard executionDashBoard = new MetricExecutionDashBoard();
            executionDashBoard.setValue(resultFormula.getResult(result.getActualValue(), Objects.isNull(result.getExpectedValue()) ? 0 : result.getExpectedValue()));
            executionDashBoard.setType(resultFormula.getType().getDescription());
            executionDashBoard.setDatetime(result.getCreateTime().toString());

            resultList.add(executionDashBoard);
        });

        return resultList;
    }
}
