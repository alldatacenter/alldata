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
package io.datavines.server.dqc.coordinator.operator;

import io.datavines.common.entity.JobExecutionRequest;
import io.datavines.common.enums.OperatorType;
import io.datavines.common.utils.JSONUtils;
import io.datavines.common.utils.placeholder.PlaceholderUtils;
import io.datavines.core.utils.LanguageUtils;
import io.datavines.engine.core.utils.JsonUtils;
import io.datavines.metric.api.*;
import io.datavines.notification.api.entity.SlaConfigMessage;
import io.datavines.notification.api.entity.SlaNotificationMessage;
import io.datavines.notification.api.entity.SlaSenderMessage;
import io.datavines.notification.core.client.NotificationClient;
import io.datavines.server.api.dto.bo.issue.IssueCreate;
import io.datavines.server.api.dto.vo.JobExecutionResultVO;
import io.datavines.server.enums.DqJobExecutionState;
import io.datavines.server.repository.entity.DataSource;
import io.datavines.server.repository.entity.Job;
import io.datavines.server.repository.entity.JobExecution;
import io.datavines.server.repository.entity.JobExecutionResult;
import io.datavines.server.repository.service.*;
import io.datavines.server.repository.service.impl.JobExternalService;
import io.datavines.spi.PluginLoader;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DataQualityResultOperator {

    @Autowired
    private JobExternalService jobExternalService;

    @Autowired
    private NotificationClient notificationClient;

    @Autowired
    private SlaNotificationService slaNotificationService;

    @Autowired
    private IssueService issueService;
    
    /**
     * When the task type is data quality, it will get the statistics value、comparison value、
     * threshold、check type、operator and failure strategy，use the formula that
     * {result formula} {operator} {threshold} to get dqc result . If result is failure, it will alert
     * @param jobExecutionRequest jobExecutionRequest
     */
    public void operateDqExecuteResult(JobExecutionRequest jobExecutionRequest) {

        JobExecutionResult jobExecutionResult =
                jobExternalService.getJobExecutionResultByJobExecutionId(jobExecutionRequest.getJobExecutionId());
        if (jobExecutionResult != null) {
            //check the result ,if result is failure do some operator by failure strategy
            checkDqExecuteResult(jobExecutionResult);
        }
    }

    /**
     * get the data quality check result
     * and if the result is failure that will alert or block
     * @param jobExecutionResult jobExecutionResult
     */
    private void checkDqExecuteResult(JobExecutionResult jobExecutionResult) {
        MetricExecutionResult metricExecutionResult = new MetricExecutionResult();
        BeanUtils.copyProperties(jobExecutionResult, metricExecutionResult);
        if (MetricValidator.isSuccess(metricExecutionResult)) {
            jobExecutionResult.setState(DqJobExecutionState.SUCCESS.getCode());
        } else {
            jobExecutionResult.setState(DqJobExecutionState.FAILURE.getCode());
            Long jobExecutionId = jobExecutionResult.getJobExecutionId();
            sendErrorEmail(jobExecutionId);
        }

        jobExternalService.updateJobExecutionResult(jobExecutionResult);
    }

    private void sendErrorEmail(Long jobExecutionId){

        SlaNotificationMessage message = new SlaNotificationMessage();
        JobExecution jobExecution = jobExternalService.getJobExecutionById(jobExecutionId);
        Long jobId = jobExecution.getJobId();
        JobService jobService = jobExternalService.getJobService();
        Job job = jobService.getById(jobId);
        String jobName = job.getName();
        Long dataSourceId = job.getDataSourceId();
        DataSource dataSource = jobExternalService.getDataSourceService().getDataSourceById(dataSourceId);
        String dataSourceName = dataSource.getName();
        String dataSourceType = dataSource.getType();
        JobExecutionResult jobExecutionResult = jobExternalService.getJobExecutionResultByJobExecutionId(jobExecution.getId());
        boolean isEn = !LanguageUtils.isZhContext();
        if (jobExecutionResult != null) {
            MetricExecutionResult metricExecutionResult = new MetricExecutionResult();
            BeanUtils.copyProperties(jobExecutionResult, metricExecutionResult);
            List<String> messages = new ArrayList<>();
            messages.add((isEn ? "Job Name : ": "作业名称: ") + jobName);
            messages.add(String.format((isEn ? "Datasource : %s [%s] : ": "数据源 : %s [%s]: ") ,dataSourceType.toUpperCase(), dataSourceName));
            String title = buildAlertSubject(metricExecutionResult, isEn);
            String content = buildAlertMessage(messages, metricExecutionResult, jobExecution.getEngineType(), isEn);
            message.setSubject(buildAlertSubject(metricExecutionResult, isEn));
            message.setMessage(buildAlertMessage(messages, metricExecutionResult, jobExecution.getEngineType(), isEn));

            saveIssue(jobId, title, content);

            Map<SlaSenderMessage, Set<SlaConfigMessage>> config = slaNotificationService.getSlasNotificationConfigurationByJobId(jobId);
            if (config.isEmpty()){
                return;
            }

            notificationClient.notify(message, config);
        }
    }

    private String buildAlertMessage(List<String> messages, MetricExecutionResult metricExecutionResult, String engineType, boolean isEn) {
        Map<String,String> parameters = new HashMap<>();
        parameters.put("actual_value", metricExecutionResult.getActualValue()+"");
        parameters.put("expected_value", metricExecutionResult.getExpectedValue()+"");
        parameters.put("threshold", metricExecutionResult.getThreshold()+"");
        parameters.put("operator",OperatorType.of(metricExecutionResult.getOperator()).getSymbol());

        SqlMetric sqlMetric = PluginLoader.getPluginLoader(SqlMetric.class).getOrCreatePlugin(metricExecutionResult.getMetricName());
        messages.add((isEn ? "Metric" : "检查规则") + " : " + sqlMetric.getNameByLanguage(isEn));

        ResultFormula resultFormula =
                PluginLoader.getPluginLoader(ResultFormula.class).getOrCreatePlugin(metricExecutionResult.getResultFormula());

        messages.add((isEn ? "Check Subject" : "检查目标") + " : " + metricExecutionResult.getDatabaseName() + "." + metricExecutionResult.getTableName() + "." + metricExecutionResult.getColumnName());

        ExpectedValue expectedValue = PluginLoader.getPluginLoader(ExpectedValue.class).getOrCreatePlugin(engineType + "_" + metricExecutionResult.getExpectedType());
        messages.add((isEn ? "Expected Value Type" : "期望值类型") + " : " + expectedValue.getNameByLanguage(isEn));

        String resultFormulaFormat = resultFormula.getResultFormat(isEn)+" ${operator} ${threshold}";
        messages.add((isEn ? "Result Formula" : "检查公式") + " : " + PlaceholderUtils.replacePlaceholders(resultFormulaFormat, parameters, true));

        messages.add(isEn ? "Check Result : Failure" : "检查结果 : 异常" );

        return JSONUtils.toJsonString(messages);
    }

    private String buildAlertSubject(MetricExecutionResult metricExecutionResult, boolean isEn) {
        String checkSubject = metricExecutionResult.getDatabaseName() + "." + metricExecutionResult.getTableName() + "." + metricExecutionResult.getColumnName();
        SqlMetric sqlMetric = PluginLoader.getPluginLoader(SqlMetric.class).getOrCreatePlugin(metricExecutionResult.getMetricName());
        return  isEn ? (sqlMetric.getNameByLanguage(true) + "alerting on " + checkSubject) :
                checkSubject + "在" + sqlMetric.getNameByLanguage(false) + "中异常了";
    }

    private void saveIssue(Long jobId, String title, String content) {
        IssueCreate issueCreate = new IssueCreate();
        issueCreate.setTitle(title);
        issueCreate.setContent(content);
        issueCreate.setJobId(jobId);
        issueCreate.setStatus("good");

        issueService.create(issueCreate);
    }
}
