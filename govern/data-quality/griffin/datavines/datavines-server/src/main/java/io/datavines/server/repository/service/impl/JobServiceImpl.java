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
import io.datavines.common.datasource.jdbc.entity.ColumnInfo;
import io.datavines.common.entity.ConnectionInfo;
import io.datavines.common.entity.JobExecutionParameter;
import io.datavines.common.entity.job.BaseJobParameter;
import io.datavines.common.entity.job.NotificationParameter;
import io.datavines.common.entity.job.SubmitJob;
import io.datavines.server.dqc.coordinator.builder.JobExecutionParameterBuilderFactory;
import io.datavines.common.enums.DataVinesDataType;
import io.datavines.common.enums.ExecutionStatus;
import io.datavines.common.enums.JobType;
import io.datavines.common.utils.CommonPropertyUtils;
import io.datavines.common.utils.JSONUtils;
import io.datavines.common.utils.StringUtils;
import io.datavines.core.enums.Status;
import io.datavines.core.exception.DataVinesServerException;
import io.datavines.core.utils.LanguageUtils;
import io.datavines.metric.api.ResultFormula;
import io.datavines.metric.api.SqlMetric;
import io.datavines.server.api.dto.bo.job.DataProfileJobCreateOrUpdate;
import io.datavines.server.api.dto.bo.job.JobCreate;
import io.datavines.server.api.dto.bo.job.JobUpdate;
import io.datavines.server.api.dto.vo.JobVO;
import io.datavines.server.api.dto.vo.SlaConfigVO;
import io.datavines.server.api.dto.vo.SlaVO;
import io.datavines.server.enums.CommandType;
import io.datavines.server.enums.Priority;
import io.datavines.server.repository.entity.*;
import io.datavines.server.repository.entity.catalog.CatalogEntityInstance;
import io.datavines.server.repository.entity.catalog.CatalogEntityMetricJobRel;
import io.datavines.server.repository.mapper.*;
import io.datavines.server.repository.service.*;
import io.datavines.server.utils.ContextHolder;
import io.datavines.server.utils.DefaultDataSourceInfoUtils;
import io.datavines.server.utils.JobParameterUtils;
import io.datavines.spi.PluginLoader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.datavines.common.CommonConstants.LOCAL;
import static io.datavines.common.ConfigConstants.*;

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

    @Autowired
    private IssueService issueService;

    @Autowired
    private CatalogEntityInstanceService catalogEntityInstanceService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long create(JobCreate jobCreate) throws DataVinesServerException {

        String parameter = jobCreate.getParameter();
        if (StringUtils.isEmpty(parameter)) {
            throw new DataVinesServerException(Status.JOB_PARAMETER_IS_NULL_ERROR);
        }

        Job job = new Job();
        BeanUtils.copyProperties(jobCreate, job);

        List<BaseJobParameter> jobParameters = JSONUtils.toList(parameter, BaseJobParameter.class);
        jobParameters = JobParameterUtils.regenerateJobParameterList(jobParameters);

        isMetricSuitable(jobCreate.getDataSourceId(), jobCreate.getDataSourceId2(), jobCreate.getEngineType(), jobParameters);
        List<String> fqnList = setJobAttribute(job, jobParameters);
        job.setName(getJobName(jobCreate.getType(), jobCreate.getParameter()));
        if (getByKeyAttribute(job)) {
            throw new DataVinesServerException(Status.JOB_EXIST_ERROR, job.getName());
        }
        job.setType(JobType.of(jobCreate.getType()));
        job.setCreateBy(ContextHolder.getUserId());
        job.setCreateTime(LocalDateTime.now());
        job.setUpdateBy(ContextHolder.getUserId());
        job.setUpdateTime(LocalDateTime.now());

        if (!save(job)) {
            log.info("create metric jov error : {}", jobCreate);
            throw new DataVinesServerException(Status.CREATE_JOB_ERROR, job.getName());
        } else {
            saveOrUpdateMetricJobEntityRel(job, fqnList);
        }

        long jobId = job.getId();

        // whether running now
        if (jobCreate.getRunningNow() == 1) {
            executeJob(job, null);
        }

        return jobId;
    }

    private boolean getByKeyAttribute(Job job) {
         List<Job> list = baseMapper.selectList(new QueryWrapper<Job>()
                .eq("name",job.getName())
                .eq("schema_name",job.getSchemaName())
                .eq("table_name",job.getTableName())
                .eq("datasource_id",job.getDataSourceId())
                .eq(job.getDataSourceId2() != 0, "datasource_id_2", job.getDataSourceId2())
                .eq(StringUtils.isNotEmpty(job.getColumnName()), "column_name",job.getColumnName())
         );
         return CollectionUtils.isNotEmpty(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long update(JobUpdate jobUpdate) {
        Job job = getById(jobUpdate.getId());
        if (job == null) {
            throw new DataVinesServerException(Status.JOB_NOT_EXIST_ERROR, jobUpdate.getId());
        }

        BeanUtils.copyProperties(jobUpdate, job);
        List<BaseJobParameter> jobParameters = JSONUtils.toList(jobUpdate.getParameter(), BaseJobParameter.class);
        isMetricSuitable(jobUpdate.getDataSourceId(), jobUpdate.getDataSourceId2(), jobUpdate.getEngineType(), jobParameters);
        List<String> fqnList = setJobAttribute(job, jobParameters);
        job.setName(getJobName(jobUpdate.getType(), jobUpdate.getParameter()));
        job.setUpdateBy(ContextHolder.getUserId());
        job.setUpdateTime(LocalDateTime.now());

        if (!updateById(job)) {
            log.info("update metric job  error : {}", jobUpdate);
            throw new DataVinesServerException(Status.UPDATE_JOB_ERROR, job.getName());
        } else {
            saveOrUpdateMetricJobEntityRel(job, fqnList);
        }

        if (jobUpdate.getRunningNow() == 1) {
            executeJob(job, null);
        }

        return job.getId();
    }

    private void saveOrUpdateMetricJobEntityRel(Job job, List<String> fqnList) {
        List<CatalogEntityMetricJobRel>  listRel = catalogEntityMetricJobRelService.list(new QueryWrapper<CatalogEntityMetricJobRel>()
                .eq("metric_job_id", job.getId())
                .eq("metric_job_type", "DATA_QUALITY"));
        if (listRel.size() >= 1) {
            catalogEntityMetricJobRelService.remove(new QueryWrapper<CatalogEntityMetricJobRel>()
                    .eq("metric_job_id", job.getId())
                    .eq("metric_job_type", "DATA_QUALITY"));
        }

        if (CollectionUtils.isNotEmpty(fqnList)) {
            for (String fqn : fqnList) {
                CatalogEntityInstance instance =
                        catalogEntityInstanceService.getByDataSourceAndFQN(job.getDataSourceId(), fqn);
                if (instance == null) {
                    continue;
                }

                CatalogEntityMetricJobRel entityMetricJobRel = new CatalogEntityMetricJobRel();
                entityMetricJobRel.setEntityUuid(instance.getUuid());
                entityMetricJobRel.setMetricJobId(job.getId());
                entityMetricJobRel.setMetricJobType("DATA_QUALITY");
                entityMetricJobRel.setCreateBy(ContextHolder.getUserId());
                entityMetricJobRel.setCreateTime(LocalDateTime.now());
                entityMetricJobRel.setUpdateBy(ContextHolder.getUserId());
                entityMetricJobRel.setUpdateTime(LocalDateTime.now());
                catalogEntityMetricJobRelService.save(entityMetricJobRel);
            }
        }
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

    private void isMetricSuitable(long datasourceId, long datasourceId2, String engine, List<BaseJobParameter> jobParameters) {
        if (CollectionUtils.isNotEmpty(jobParameters)) {
            for (BaseJobParameter jobParameter : jobParameters) {

                if (datasourceId2 !=0 && LOCAL.equalsIgnoreCase(engine) && "multi_table_accuracy".equalsIgnoreCase(jobParameter.getMetricType())) {
                    throw new DataVinesServerException(Status.MULTI_TABLE_ACCURACY_NOT_SUPPORT_LOCAL_ENGINE);
                }

                if (StringUtils.isEmpty(getFQN(jobParameter))) {
                    throw new DataVinesServerException(Status.METRIC_JOB_RELATED_ENTITY_NOT_EXIST);
                }

                if (!isColumn(jobParameter)) {
                    return;
                }

                CatalogEntityInstance columnEntity =
                        catalogEntityInstanceService.getByDataSourceAndFQN(datasourceId, getFQN(jobParameter));
                if (columnEntity == null) {
                    return;
                }

                if (StringUtils.isEmpty(columnEntity.getProperties())) {
                    throw new DataVinesServerException(Status.ENTITY_TYPE_NOT_EXIST);
                }

                ColumnInfo columnInfo = JSONUtils.parseObject(columnEntity.getProperties(), ColumnInfo.class);
                if (columnInfo != null) {
                    String columnType = columnInfo.getType();
                    DataVinesDataType dataVinesDataType = DataVinesDataType.getType(columnType);
                    if (dataVinesDataType == null) {
                        throw new DataVinesServerException(Status.ENTITY_TYPE_NOT_EXIST);
                    }

                    SqlMetric metric = PluginLoader.getPluginLoader(SqlMetric.class).getOrCreatePlugin(jobParameter.getMetricType());
                    if (metric == null) {
                        throw new DataVinesServerException(Status.METRIC_JOB_RELATED_ENTITY_NOT_EXIST, jobParameter.getMetricType().toUpperCase());
                    }

                    List<DataVinesDataType> suitableTypeList = metric.suitableType();
                    if (!suitableTypeList.contains(dataVinesDataType)) {
                        throw new DataVinesServerException(Status.METRIC_NOT_SUITABLE_ENTITY_TYPE, metric.getNameByLanguage(!LanguageUtils.isZhContext()), dataVinesDataType.getName().toUpperCase());
                    }
                }
            }
        }
    }

    private boolean isColumn(BaseJobParameter jobParameter) {
        String column = (String)jobParameter.getMetricParameter().get("column");
        return StringUtils.isNotEmpty(column);
    }

    private String getFQN(BaseJobParameter jobParameter) {
        String fqn = "";
        String schema = (String)jobParameter.getMetricParameter().get("database");
        String table = (String)jobParameter.getMetricParameter().get("table");
        String column = (String)jobParameter.getMetricParameter().get("column");
        if (StringUtils.isEmpty(schema)) {
            return null;
        }

        if (StringUtils.isEmpty(table)) {
            return null;
        } else {
            fqn = schema + "." + table;
        }

        if (StringUtils.isEmpty(column)) {
            return fqn;
        } else {
            return fqn + "." + column;
        }
    }

    private List<String> setJobAttribute(Job job, List<BaseJobParameter> jobParameters) {
        List<String> fqnList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(jobParameters)) {
            BaseJobParameter jobParameter = jobParameters.get(0);
            job.setSchemaName((String)jobParameter.getMetricParameter().get("database"));
            job.setTableName((String)jobParameter.getMetricParameter().get("table"));
            job.setColumnName((String)jobParameter.getMetricParameter().get("column"));
            job.setMetricType(jobParameter.getMetricType());
            fqnList.add(getFQN(jobParameter));
        }

        return fqnList;
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
            catalogEntityMetricJobRelService.deleteByJobId(id);
            jobExecutionService.deleteByJobId(id);
            issueService.deleteByJobId(id);
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
            deleteById(job.getId());
            jobExecutionService.deleteByJobId(job.getId());
        });

        return 1;
    }

    @Override
    public IPage<JobVO> getJobPage(String searchVal, Long dataSourceId, Integer type, Integer pageNumber, Integer pageSize) {
        Page<JobVO> page = new Page<>(pageNumber, pageSize);
        IPage<JobVO> jobs = baseMapper.getJobPage(page, searchVal, dataSourceId, type);
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

        JobExecution jobExecution = getJobExecution(job, scheduleTime);

        jobExecutionService.save(jobExecution);

        // add a command
        Command command = new Command();
        command.setType(CommandType.START);
        command.setPriority(Priority.MEDIUM);
        command.setJobExecutionId(jobExecution.getId());
        commandService.insert(command);
    }

    private JobExecution getJobExecution(Job job, LocalDateTime scheduleTime) {
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
        return jobExecution;
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

    @Override
    public String getJobConfig(Long jobId) {
        SubmitJob submitJob = new SubmitJob();

        Job job = baseMapper.selectById(jobId);
        if  (job == null) {
            throw new DataVinesServerException(Status.JOB_NOT_EXIST_ERROR, jobId);
        }

        JobExecution jobExecution = getJobExecution(job, null);
        jobExecution.setId(System.currentTimeMillis());

        submitJob.setName(jobExecution.getName());
        submitJob.setExecutePlatformType(jobExecution.getExecutePlatformType());
        submitJob.setExecutePlatformParameter(JSONUtils.toMap(jobExecution.getExecutePlatformParameter(), String.class, Object.class));
        submitJob.setEngineType(jobExecution.getEngineType());
        submitJob.setEngineParameter(JSONUtils.toMap(jobExecution.getEngineParameter(), String.class, Object.class));
        submitJob.setParameter(JSONUtils.parseObject(jobExecution.getParameter(), JobExecutionParameter.class));
        submitJob.setErrorDataStorageType(jobExecution.getErrorDataStorageType());
        submitJob.setErrorDataStorageParameter(JSONUtils.toMap(jobExecution.getErrorDataStorageParameter(), String.class, Object.class));
        submitJob.setValidateResultDataStorageType(DefaultDataSourceInfoUtils.getDefaultConnectionInfo().getType());
        submitJob.setValidateResultDataStorageParameter(DefaultDataSourceInfoUtils.getDefaultDataSourceConfigMap());
        submitJob.setLanguageEn(!LanguageUtils.isZhContext());
        if (StringUtils.isNotEmpty(jobExecution.getEnv())) {
            submitJob.setEnv(jobExecution.getEnv());
        }

        List<SlaConfigVO> slaConfigList = slaService.getSlaConfigByJobId(jobId);
        if (CollectionUtils.isNotEmpty(slaConfigList)) {
            List<NotificationParameter> notificationParameterList = new ArrayList<>();
            slaConfigList.forEach(item->{
                NotificationParameter notificationParameter = new NotificationParameter();
                notificationParameter.setType(item.getType());
                notificationParameter.setConfig(JSONUtils.toMap(item.getConfig(), String.class, Object.class));
                notificationParameter.setReceiver(JSONUtils.toMap(item.getReceiver(), String.class, Object.class));
                notificationParameterList.add(notificationParameter);
            });
            submitJob.setNotificationParameters(notificationParameterList);
        }
        return JSONUtils.toJsonString(submitJob);
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
