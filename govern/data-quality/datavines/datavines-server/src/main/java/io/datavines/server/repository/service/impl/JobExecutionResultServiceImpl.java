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

import io.datavines.common.entity.JobExecutionParameter;
import io.datavines.common.entity.job.BaseJobParameter;
import io.datavines.common.entity.job.DataQualityJobParameter;
import io.datavines.common.utils.JSONUtils;
import io.datavines.common.utils.placeholder.PlaceholderUtils;
import io.datavines.core.utils.LanguageUtils;
import io.datavines.metric.api.ConfigItem;
import io.datavines.metric.api.ExpectedValue;
import io.datavines.metric.api.ResultFormula;
import io.datavines.metric.api.SqlMetric;
import io.datavines.server.api.dto.vo.JobExecutionResultVO;
import io.datavines.server.repository.entity.Job;
import io.datavines.server.repository.entity.JobExecution;
import io.datavines.server.repository.entity.JobExecutionResult;
import io.datavines.server.repository.service.JobService;
import io.datavines.server.repository.service.JobExecutionService;
import io.datavines.server.enums.DqJobExecutionState;
import io.datavines.common.enums.OperatorType;
import io.datavines.server.repository.mapper.JobExecutionResultMapper;
import io.datavines.server.repository.service.JobExecutionResultService;
import io.datavines.spi.PluginLoader;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.*;

@Service("jobExecutionResultService")
public class JobExecutionResultServiceImpl extends ServiceImpl<JobExecutionResultMapper, JobExecutionResult>  implements JobExecutionResultService {

    @Autowired
    private JobExecutionService jobExecutionService;

    @Autowired
    private JobService jobService;

    @Override
    public long insert(JobExecutionResult jobExecutionResult) {
        baseMapper.insert(jobExecutionResult);
        return jobExecutionResult.getId();
    }

    @Override
    public int update(JobExecutionResult jobExecutionResult) {
        return baseMapper.updateById(jobExecutionResult);
    }

    @Override
    public int deleteByJobExecutionId(long taskId) {
        return baseMapper.delete(new QueryWrapper<JobExecutionResult>().eq("job_execution_id",taskId));
    }

    @Override
    public JobExecutionResult getById(long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public JobExecutionResult getByJobExecutionId(long taskId) {
        List<JobExecutionResult> list = baseMapper.selectList(new QueryWrapper<JobExecutionResult>().eq("job_execution_id", taskId).orderByDesc("update_time"));
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        return list.get(0);
    }

    @Override
    public JobExecutionResultVO getResultVOByJobExecutionId(long taskId) {
        JobExecutionResultVO jobExecutionResultVO = new JobExecutionResultVO();
        Map<String,String> parameters = new HashMap<>();
        JobExecutionResult jobExecutionResult = baseMapper.getOne(taskId);
        parameters.put("actual_value", jobExecutionResult.getActualValue()+"");
        parameters.put("expected_value", jobExecutionResult.getExpectedValue()+"");
        parameters.put("threshold", jobExecutionResult.getThreshold()+"");
        parameters.put("operator",OperatorType.of(jobExecutionResult.getOperator()).getSymbol());

        JobExecution jobExecution = jobExecutionService.getById(taskId);
        if (!Objects.isNull(jobExecution)) {
            Job job = jobService.getById(jobExecution.getJobId());
            List<BaseJobParameter> jobParameterList = JSONUtils.toList(job.getParameter(),BaseJobParameter.class);
            for (BaseJobParameter jobParameter : jobParameterList) {
                if (jobParameter != null) {
                    SqlMetric sqlMetric = PluginLoader.getPluginLoader(SqlMetric.class).getOrCreatePlugin(jobParameter.getMetricType());
                    Map<String,ConfigItem> configMap = sqlMetric.getConfigMap();
                    Map<String,Object> paramMap = new HashMap<>();
                    String uniqueName = jobParameter.getMetricType() + "."
                            + jobParameter.getMetricParameter().get("database")+ "."
                            + jobParameter.getMetricParameter().get("table")+ "."
                            + jobParameter.getMetricParameter().get("column");

                    String taskResultUniqueName = jobExecutionResult.getMetricName()+ "."
                            + jobExecutionResult.getDatabaseName() + "."
                            + jobExecutionResult.getTableName() + "."
                            + jobExecutionResult.getColumnName();

                    if (uniqueName.equalsIgnoreCase(taskResultUniqueName)) {
                        configMap.entrySet().stream().filter(x->{
                            return !("column".equalsIgnoreCase(x.getKey()) || "table".equalsIgnoreCase(x.getKey()) || "filter".equalsIgnoreCase(x.getKey()));
                        }).forEach(config -> {
                            paramMap.put(config.getValue().getLabel(!LanguageUtils.isZhContext()), jobParameter.getMetricParameter().get(config.getKey()));
                        });
                        jobExecutionResultVO.setMetricParameter(paramMap);
                    }
                }
            }
        }

        ResultFormula resultFormula =
                PluginLoader.getPluginLoader(ResultFormula.class).getOrCreatePlugin(jobExecutionResult.getResultFormula());
        String resultFormulaFormat = resultFormula.getResultFormat(!LanguageUtils.isZhContext())+" ${operator} ${threshold}";

        jobExecutionResultVO.setCheckSubject(jobExecutionResult.getDatabaseName() + "." + jobExecutionResult.getTableName() + "." + jobExecutionResult.getColumnName());
        jobExecutionResultVO.setCheckResult(DqJobExecutionState.of(jobExecutionResult.getState()).getDescription(!LanguageUtils.isZhContext()));
        ExpectedValue expectedValue = PluginLoader.getPluginLoader(ExpectedValue.class).getOrCreatePlugin(jobExecution.getEngineType() + "_" + jobExecutionResult.getExpectedType());

        jobExecutionResultVO.setExpectedType(expectedValue.getNameByLanguage(!LanguageUtils.isZhContext()));
        SqlMetric sqlMetric = PluginLoader.getPluginLoader(SqlMetric.class).getOrCreatePlugin(jobExecutionResult.getMetricName());
        jobExecutionResultVO.setMetricName(sqlMetric.getNameByLanguage(!LanguageUtils.isZhContext()));
        jobExecutionResultVO.setResultFormulaFormat(PlaceholderUtils.replacePlaceholders(resultFormulaFormat, parameters, true));

        return jobExecutionResultVO;
    }

    @Override
    public List<JobExecutionResult> listByJobIdAndTimeRange(Long jobId, String startTime, String endTime) {
        return baseMapper.listByJobIdAndTimeRange(jobId, startTime, endTime);
    }
}
