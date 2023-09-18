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
package io.datavines.runner;

import io.datavines.common.config.Configurations;
import io.datavines.common.entity.JobExecutionRequest;
import io.datavines.common.entity.ListWithQueryColumn;
import io.datavines.common.entity.ProcessResult;
import io.datavines.common.entity.job.NotificationParameter;
import io.datavines.common.enums.ExecutionStatus;
import io.datavines.common.enums.OperatorType;
import io.datavines.common.param.ConnectorResponse;
import io.datavines.common.param.ExecuteRequestParam;
import io.datavines.common.utils.JSONUtils;
import io.datavines.common.utils.LoggerUtils;
import io.datavines.common.utils.ParameterUtils;
import io.datavines.common.utils.StringUtils;
import io.datavines.common.ConfigConstants;
import io.datavines.engine.api.engine.EngineExecutor;
import io.datavines.metric.api.*;
import io.datavines.notification.api.entity.SlaConfigMessage;
import io.datavines.notification.api.entity.SlaNotificationMessage;
import io.datavines.notification.api.entity.SlaSenderMessage;
import io.datavines.notification.core.NotificationManager;
import io.datavines.spi.PluginLoader;
import io.datavines.storage.api.StorageFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Slf4j
public class JobRunner {

    private final JobExecutionRequest jobExecutionRequest;

    private EngineExecutor engineExecutor;

    private final Configurations configurations;

    public JobRunner(JobExecutionRequest jobExecutionRequest, Configurations configurations){
        this.jobExecutionRequest = jobExecutionRequest;
        this.configurations = configurations;
    }

    public void run() {

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

            ProcessResult processResult = engineExecutor.getProcessResult();
            if (ExecutionStatus.FAILURE.getCode() == processResult.getExitStatusCode()) {
                //TODO task failure notification
            } else {
                Long jobExecutionId = jobExecutionRequest.getJobExecutionId();
                String validateResultStorageType = jobExecutionRequest.getValidateResultDataStorageType();
                StorageFactory validateResultStorageFactory =
                        PluginLoader.getPluginLoader(StorageFactory.class).getOrCreatePlugin(validateResultStorageType);
                if (validateResultStorageFactory == null) {
                    log.error("validate result storage type {} is not supported", validateResultStorageType);
                    return;
                }

                ExecuteRequestParam executeRequestParam = new ExecuteRequestParam();
                String validateResultDataStorageParameter = jobExecutionRequest.getValidateResultDataStorageParameter();
                executeRequestParam.setDataSourceParam(validateResultDataStorageParameter);

                Map<String,String> scriptConfigMap = new HashMap<>();
                scriptConfigMap.put("execution_id", String.valueOf(jobExecutionId));
                executeRequestParam.setScript(validateResultStorageFactory.getValidateResultDataScript(scriptConfigMap));
                ConnectorResponse response = validateResultStorageFactory.getStorageExecutor().queryForOne(executeRequestParam);
                if (response != null && response.getResult()!= null) {
                    ListWithQueryColumn validateResultDataList = (ListWithQueryColumn)response.getResult();
                    Map<String, Object> validateResultData = validateResultDataList.getResultList().get(0);
                    MetricExecutionResult metricExecutionResult = new MetricExecutionResult(validateResultData);
                    if (!MetricValidator.isSuccess(metricExecutionResult)) {
                        if (StringUtils.isEmpty(jobExecutionRequest.getNotificationParameters())) {
                            log.warn("notification parameter is null");
                            return;
                        }

                        List<NotificationParameter> notificationParameters =
                                JSONUtils.toList(jobExecutionRequest.getNotificationParameters(), NotificationParameter.class);

                        if (CollectionUtils.isEmpty(notificationParameters)) {
                            log.error("parse notification parameter error");
                            return;
                        }

                        SlaNotificationMessage notificationMessage = new SlaNotificationMessage();
                        notificationMessage.setMessage(buildAlertMessage(metricExecutionResult, jobExecutionRequest.getEngineType(), jobExecutionRequest.isEn()));
                        notificationMessage.setSubject(buildAlertSubject(metricExecutionResult, jobExecutionRequest.isEn()));
                        Map<SlaSenderMessage, Set<SlaConfigMessage>> configMap = new HashMap<>();
                        for (NotificationParameter notificationParameter : notificationParameters) {
                            SlaSenderMessage slaSenderMessage = new SlaSenderMessage();
                            slaSenderMessage.setType(notificationParameter.getType());
                            slaSenderMessage.setConfig(JSONUtils.toJsonString(notificationParameter.getConfig()));

                            Set<SlaConfigMessage> set = new HashSet<>();
                            if (MapUtils.isNotEmpty(notificationParameter.getReceiver())) {
                                SlaConfigMessage slaConfigMessage = new SlaConfigMessage();
                                slaConfigMessage.setType(notificationParameter.getType());
                                slaConfigMessage.setConfig(JSONUtils.toJsonString(notificationParameter.getReceiver()));
                                set.add(slaConfigMessage);
                            }
                            configMap.put(slaSenderMessage, set);
                        }

                        NotificationManager notificationManager = new NotificationManager();
                        notificationManager.notify(notificationMessage, configMap);
                    }
                }
            }
        } catch (Exception e) {
            log.error("validate job execute failure", e);
        }
    }

    private String buildAlertMessage(MetricExecutionResult metricExecutionResult, String engineType, boolean isEn) {
        List<String> messages = new ArrayList<>();
        Map<String,String> parameters = new HashMap<>();
        parameters.put(ConfigConstants.ACTUAL_VALUE, metricExecutionResult.getActualValue()+"");
        parameters.put(ConfigConstants.EXPECTED_VALUE, metricExecutionResult.getExpectedValue()+"");
        parameters.put(ConfigConstants.THRESHOLD, metricExecutionResult.getThreshold()+"");
        parameters.put(ConfigConstants.OPERATOR, OperatorType.of(metricExecutionResult.getOperator()).getSymbol());

        SqlMetric sqlMetric = PluginLoader.getPluginLoader(SqlMetric.class).getOrCreatePlugin(metricExecutionResult.getMetricName());
        messages.add((isEn ? "Metric" : "检查规则") + " : " + sqlMetric.getNameByLanguage(isEn));

        ResultFormula resultFormula =
                PluginLoader.getPluginLoader(ResultFormula.class).getOrCreatePlugin(metricExecutionResult.getResultFormula());

        messages.add((isEn ? "Check Subject" : "检查目标") + " : " + metricExecutionResult.getDatabaseName() + "." + metricExecutionResult.getTableName() + "." + metricExecutionResult.getColumnName());

        ExpectedValue expectedValue = PluginLoader.getPluginLoader(ExpectedValue.class).getOrCreatePlugin(engineType + "_" + metricExecutionResult.getExpectedType());
        messages.add((isEn ? "Expected Value Type" : "期望值类型") + " : " + expectedValue.getNameByLanguage(isEn));

        String resultFormulaFormat = resultFormula.getResultFormat(isEn)+" ${operator} ${threshold}";
        messages.add((isEn ? "Result Formula" : "检查公式") + " : " + ParameterUtils.convertParameterPlaceholders(resultFormulaFormat, parameters));

        messages.add(isEn ? "Check Result : Failure" : "检查结果 : 异常" );

        return JSONUtils.toJsonString(messages);
    }

    private String buildAlertSubject(MetricExecutionResult metricExecutionResult, boolean isEn) {
        String checkSubject = metricExecutionResult.getDatabaseName() + "." + metricExecutionResult.getTableName() + "." + metricExecutionResult.getColumnName();
        SqlMetric sqlMetric = PluginLoader.getPluginLoader(SqlMetric.class).getOrCreatePlugin(metricExecutionResult.getMetricName());
        return  isEn ? (sqlMetric.getNameByLanguage(true) + " alerting on " + checkSubject) :
                checkSubject + "在" + sqlMetric.getNameByLanguage(false) + "中异常";
    }
}
