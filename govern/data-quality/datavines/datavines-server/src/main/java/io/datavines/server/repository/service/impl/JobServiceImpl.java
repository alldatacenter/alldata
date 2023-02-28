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

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.datavines.common.entity.ConnectionInfo;
import io.datavines.common.entity.job.BaseJobParameter;
import io.datavines.common.entity.job.builder.JobExecutionParameterBuilderFactory;
import io.datavines.common.enums.ExecutionStatus;
import io.datavines.common.enums.JobType;
import io.datavines.common.utils.CommonPropertyUtils;
import io.datavines.common.utils.JSONUtils;
import io.datavines.common.utils.StringUtils;
import io.datavines.core.enums.Status;
import io.datavines.core.exception.DataVinesServerException;
import io.datavines.metric.api.ResultFormula;
import io.datavines.server.api.dto.bo.job.DataProfileJobCreateOrUpdate;
import io.datavines.server.api.dto.bo.job.JobCreate;
import io.datavines.server.api.dto.bo.job.JobUpdate;
import io.datavines.server.api.dto.vo.JobVO;
import io.datavines.server.api.dto.vo.SlaVO;
import io.datavines.server.enums.CommandType;
import io.datavines.server.enums.Priority;
import io.datavines.server.repository.entity.*;
import io.datavines.server.repository.entity.catalog.CatalogEntityMetricJobRel;
import io.datavines.server.repository.mapper.*;
import io.datavines.server.repository.service.*;
import io.datavines.server.utils.ContextHolder;
import io.datavines.spi.PluginLoader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.datavines.engine.api.ConfigConstants.*;

@Slf4j
@Service("jobService")
public class JobServiceImpl extends ServiceImpl<JobMapper, Job> implements JobService {

    @Autowired
    private JobExecutionService jobExecutionService;

    @Autowired
    private CommandService commandService;

    @Autowired
    private DataSourceService dataSourceService;

    @Autowired
    private EnvService envService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private ErrorDataStorageService errorDataStorageService;

    @Autowired
    private SlaService slaService;

    @Autowired
    private CatalogEntityMetricJobRelService catalogEntityMetricJobRelService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long create(JobCreate jobCreate) throws DataVinesServerException {

        // 需要对参数进行校验，判断插件类型是否存在
        String parameter = jobCreate.getParameter();
        if (StringUtils.isEmpty(parameter)) {
            throw new DataVinesServerException(Status.JOB_PARAMETER_IS_NULL_ERROR);
        }

        Job job = new Job();
        BeanUtils.copyProperties(jobCreate, job);

        List<BaseJobParameter> jobParameters = JSONUtils.toList(parameter, BaseJobParameter.class);
        setJobAttribute(job, jobParameters);
        job.setName(getJobName(jobCreate.getType(), jobCreate.getParameter()));
        if (getByKeyAttribute(job)) {
            throw new DataVinesServerException(Status.JOB_EXIST_ERROR, job.getName());
        }
        job.setType(JobType.of(jobCreate.getType()));
        job.setCreateBy(ContextHolder.getUserId());
        job.setCreateTime(LocalDateTime.now());
        job.setUpdateBy(ContextHolder.getUserId());
        job.setUpdateTime(LocalDateTime.now());

        // add a job
        baseMapper.insert(job);
        long jobId = job.getId();

        // whether running now
        if(jobCreate.getRunningNow() == 1) {
            executeJob(job, null);
        }

        return jobId;
    }

    private boolean getByKeyAttribute(Job job) {
         List<Job> list = baseMapper.selectList(new QueryWrapper<Job>()
                .eq("name",job.getName())
                .eq("schema_name",job.getSchemaName())
                .eq("table_name",job.getTableName())
                .eq(job.getColumnName()!=null, "column_name",job.getColumnName())
         );
         return CollectionUtils.isNotEmpty(list);
    }

    @Override
    public long update(JobUpdate jobUpdate) {
        Job job = getById(jobUpdate.getId());
        if (job == null) {
            throw new DataVinesServerException(Status.JOB_NOT_EXIST_ERROR, jobUpdate.getId());
        }

        BeanUtils.copyProperties(jobUpdate, job);
        List<BaseJobParameter> jobParameters = JSONUtils.toList(jobUpdate.getParameter(), BaseJobParameter.class);
        setJobAttribute(job, jobParameters);
        job.setName(getJobName(jobUpdate.getType(), jobUpdate.getParameter()));
        job.setUpdateBy(ContextHolder.getUserId());
        job.setUpdateTime(LocalDateTime.now());

        if (baseMapper.updateById(job) <= 0) {
            log.info("update workspace fail : {}", jobUpdate);
            throw new DataVinesServerException(Status.UPDATE_JOB_ERROR, job.getName());
        }

        if (jobUpdate.getRunningNow() == 1) {
            executeJob(job, null);
        }

        return job.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long createOrUpdateDataProfileJob(DataProfileJobCreateOrUpdate dataProfileJobCreateOrUpdate) throws DataVinesServerException {
        // 需要对参数进行校验，判断插件类型是否存在
        String parameter = dataProfileJobCreateOrUpdate.getParameter();
        if (StringUtils.isEmpty(parameter)) {
            throw new DataVinesServerException(Status.JOB_PARAMETER_IS_NULL_ERROR);
        }

        Job job = new Job();
        BeanUtils.copyProperties(dataProfileJobCreateOrUpdate, job);
        job.setName(String.format("%s(%s.%s)", "DATA_PROFILE", job.getSchemaName(), job.getTableName()));

        List<Job> list = baseMapper.selectList(new QueryWrapper<Job>()
                .eq("name",job.getName())
                .eq("datasource_id",job.getDataSourceId())
        );

        if (CollectionUtils.isNotEmpty(list)) {
            job.setId(list.get(0).getId());
            job.setUpdateBy(ContextHolder.getUserId());
            job.setUpdateTime(LocalDateTime.now());
            job.setType(JobType.DATA_PROFILE);

            if (baseMapper.updateById(job) <= 0) {
                log.info("update data profile job fail : {}", dataProfileJobCreateOrUpdate);
                throw new DataVinesServerException(Status.UPDATE_JOB_ERROR, job.getName());
            }
        } else {
            job.setType(JobType.of(dataProfileJobCreateOrUpdate.getType()));
            job.setCreateBy(ContextHolder.getUserId());
            job.setCreateTime(LocalDateTime.now());
            job.setUpdateBy(ContextHolder.getUserId());
            job.setUpdateTime(LocalDateTime.now());
            if (baseMapper.insert(job) <= 0) {
                log.info("create data profile job fail : {}", dataProfileJobCreateOrUpdate);
                throw new DataVinesServerException(Status.CREATE_JOB_ERROR, job.getName());
            }
        }
        // whether running now
        if (dataProfileJobCreateOrUpdate.getRunningNow() == 1) {
            executeJob(job, null);
        }

        return job.getId();
    }

    private void setJobAttribute(Job job, List<BaseJobParameter> jobParameters) {
        if (CollectionUtils.isNotEmpty(jobParameters)) {
            BaseJobParameter jobParameter = jobParameters.get(0);
            job.setSchemaName((String)jobParameter.getMetricParameter().get("database"));
            job.setTableName((String)jobParameter.getMetricParameter().get("table"));
            job.setColumnName((String)jobParameter.getMetricParameter().get("column"));
            job.setMetricType(jobParameter.getMetricType());
        }
    }

    @Override
    public Job getById(long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public List<Job> listByDataSourceId(Long dataSourceId) {
        return baseMapper.listByDataSourceId(dataSourceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteById(long id) {
        if (baseMapper.deleteById(id) > 0) {
            catalogEntityMetricJobRelService.remove(new QueryWrapper<CatalogEntityMetricJobRel>().eq("metric_job_id", id));
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByDataSourceId(long dataSourceId) {
        List<Job> jobList = listByDataSourceId(dataSourceId);
        if (CollectionUtils.isEmpty(jobList)) {
            return 0;
        }

        jobList.forEach(job -> {
            baseMapper.deleteById(job.getId());
            jobExecutionService.deleteByJobId(job.getId());
        });

        return 1;
    }

    @Override
    public IPage<JobVO> getJobPage(String searchVal, Long dataSourceId, Integer pageNumber, Integer pageSize) {
        Page<JobVO> page = new Page<>(pageNumber, pageSize);
        IPage<JobVO> jobs = baseMapper.getJobPage(page, searchVal, dataSourceId);
        List<JobVO> jobList = jobs.getRecords();
        if (CollectionUtils.isNotEmpty(jobList)) {
            for(JobVO jobVO: jobList) {
                List<SlaVO> slaList = slaService.getSlaByJobId(jobVO.getId());
                jobVO.setSlaList(slaList);
            }
        }
        return jobs;
    }

    @Override
    public boolean execute(Long jobId, LocalDateTime scheduleTime) throws DataVinesServerException {
        Job job = baseMapper.selectById(jobId);
        if  (job == null) {
            throw new DataVinesServerException(Status.JOB_NOT_EXIST_ERROR, jobId);
        }

        executeJob(job, scheduleTime);

        return true;
    }

    private void executeJob(Job job, LocalDateTime scheduleTime) {

        String executionParameter = buildJobExecutionParameter(job);

        long jobId = job.getId();
        Env env = envService.getById(job.getEnv());
        String envStr = "";
        if (env != null) {
            envStr = env.getEnv();
        }

        Tenant tenant = tenantService.getById(job.getTenantCode());
        String tenantStr = "";
        if (tenant != null) {
            tenantStr = tenant.getTenant();
        }

        ErrorDataStorage errorDataStorage = errorDataStorageService.getById(job.getErrorDataStorageId());
        String errorDataStorageType = "";
        String errorDataStorageParameter = "";
        if (errorDataStorage != null) {
            errorDataStorageType = errorDataStorage.getType();
            errorDataStorageParameter  = errorDataStorage.getParam();
        } else {
            if ("local".equalsIgnoreCase(job.getEngineType())) {
                errorDataStorageType = "file";
                Map<String,String> errorDataStorageParameterMap = new HashMap<>();
                errorDataStorageParameterMap.put(DATA_DIR, CommonPropertyUtils.getString(CommonPropertyUtils.ERROR_DATA_DIR, CommonPropertyUtils.ERROR_DATA_DIR_DEFAULT));
                errorDataStorageParameterMap.put(COLUMN_SEPARATOR, CommonPropertyUtils.getString(CommonPropertyUtils.COLUMN_SEPARATOR, CommonPropertyUtils.COLUMN_SEPARATOR_DEFAULT));
                errorDataStorageParameterMap.put(LINE_SEPERATOR, CommonPropertyUtils.getString(CommonPropertyUtils.LINE_SEPARATOR, CommonPropertyUtils.LINE_SEPARATOR_DEFAULT));

                errorDataStorageParameter  = JSONUtils.toJsonString(errorDataStorageParameterMap);
            }
        }

        // add a jobExecution
        JobExecution jobExecution = new JobExecution();
        BeanUtils.copyProperties(job, jobExecution);
        jobExecution.setId(null);
        jobExecution.setJobId(jobId);
        jobExecution.setParameter(executionParameter);
        jobExecution.setName(job.getName() + "_task_" + System.currentTimeMillis());
        jobExecution.setJobType(job.getType());
        jobExecution.setErrorDataStorageType(errorDataStorageType);
        jobExecution.setErrorDataStorageParameter(errorDataStorageParameter);
        jobExecution.setErrorDataFileName(getErrorDataFileName(job.getParameter()));
        jobExecution.setStatus(ExecutionStatus.SUBMITTED_SUCCESS);
        jobExecution.setTenantCode(tenantStr);
        jobExecution.setEnv(envStr);
        jobExecution.setSubmitTime(LocalDateTime.now());
        jobExecution.setScheduleTime(scheduleTime);
        jobExecution.setCreateTime(LocalDateTime.now());
        jobExecution.setUpdateTime(LocalDateTime.now());

        jobExecutionService.save(jobExecution);

        // add a command
        Command command = new Command();
        command.setType(CommandType.START);
        command.setPriority(Priority.MEDIUM);
        command.setJobExecutionId(jobExecution.getId());
        commandService.insert(command);
    }

    @Override
    public String getJobName(String jobType, String parameter) {
        List<BaseJobParameter> jobParameters = JSONUtils.toList(parameter, BaseJobParameter.class);

        if (CollectionUtils.isEmpty(jobParameters)) {
            throw new DataVinesServerException(Status.JOB_PARAMETER_IS_NULL_ERROR);
        }

        BaseJobParameter baseJobParameter = jobParameters.get(0);
        Map<String,Object> metricParameter = baseJobParameter.getMetricParameter();
        if (MapUtils.isEmpty(metricParameter)) {
            throw new DataVinesServerException(Status.JOB_PARAMETER_IS_NULL_ERROR);
        }

        ResultFormula resultFormula = PluginLoader.getPluginLoader(ResultFormula.class).getOrCreatePlugin(baseJobParameter.getResultFormula());

        String database = (String)metricParameter.get("database");
        String table = (String)metricParameter.get("table");
        String column = (String)metricParameter.get("column");
        String metric = baseJobParameter.getMetricType();

        switch (JobType.of(jobType)) {
            case DATA_QUALITY:
                return String.format("%s(%s)", metric.toUpperCase(), resultFormula.getSymbol());
            case DATA_PROFILE:
                return String.format("%s(%s.%s)", "DATA_PROFILE", database, table);
            case DATA_RECONCILIATION:
                return String.format("%s(%s)", metric.toUpperCase(), resultFormula.getSymbol());
            default:
                return String.format("%s[%s.%s.%s]%s", "JOB", database, table, column, System.currentTimeMillis());
        }
    }

    private String getErrorDataFileName(String parameter) {
        List<BaseJobParameter> jobParameters = JSONUtils.toList(parameter, BaseJobParameter.class);

        if (CollectionUtils.isEmpty(jobParameters)) {
            throw new DataVinesServerException(Status.JOB_PARAMETER_IS_NULL_ERROR);
        }

        BaseJobParameter baseJobParameter = jobParameters.get(0);
        Map<String,Object> metricParameter = baseJobParameter.getMetricParameter();
        if (MapUtils.isEmpty(metricParameter)) {
            throw new DataVinesServerException(Status.JOB_PARAMETER_IS_NULL_ERROR);
        }

        String column = (String)metricParameter.get("column");
        String metric = baseJobParameter.getMetricType();
        if (StringUtils.isEmpty(column)) {
            return String.format("%s_%s", metric.toLowerCase(), System.currentTimeMillis());
        } else {
            return String.format("%s_%s_%s", metric.toLowerCase(), column, System.currentTimeMillis());
        }
    }

    private String buildJobExecutionParameter(Job job) {
        DataSource dataSource = dataSourceService.getDataSourceById(job.getDataSourceId());
        Map<String, Object> srcSourceConfigMap = JSONUtils.toMap(dataSource.getParam(), String.class, Object.class);
        ConnectionInfo srcConnectionInfo = new ConnectionInfo();
        srcConnectionInfo.setType(dataSource.getType());
        srcConnectionInfo.setConfig(srcSourceConfigMap);

        ConnectionInfo targetConnectionInfo = new ConnectionInfo();
        DataSource dataSource2 = dataSourceService.getDataSourceById(job.getDataSourceId2());
        if (dataSource2 != null) {
            Map<String, Object> targetSourceConfigMap = JSONUtils.toMap(dataSource2.getParam(), String.class, Object.class);
            targetConnectionInfo.setType(dataSource2.getType());
            targetConnectionInfo.setConfig(targetSourceConfigMap);
        }

        return JobExecutionParameterBuilderFactory.builder(job.getType())
                .buildJobExecutionParameter(job.getParameter(), srcConnectionInfo, targetConnectionInfo);
    }
}
