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
package io.datavines.engine.local.config;

import io.datavines.common.config.SinkConfig;
import io.datavines.common.config.TransformConfig;
import io.datavines.common.config.enums.SinkType;
import io.datavines.common.config.enums.TransformType;
import io.datavines.common.entity.ExecuteSql;
import io.datavines.common.entity.job.BaseJobParameter;
import io.datavines.common.exception.DataVinesException;
import io.datavines.common.utils.JSONUtils;
import io.datavines.common.utils.StringUtils;
import io.datavines.engine.config.MetricParserUtils;
import io.datavines.metric.api.ExpectedValue;
import io.datavines.metric.api.SqlMetric;
import io.datavines.spi.PluginLoader;
import io.datavines.storage.api.StorageFactory;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.datavines.common.CommonConstants.TABLE2;
import static io.datavines.common.ConfigConstants.*;
import static io.datavines.engine.config.MetricParserUtils.generateUniqueCode;

public class LocalMultiTableValueComparisonMetricBuilder extends BaseLocalConfigurationBuilder {

    @Override
    public void buildTransformConfigs() {
        List<BaseJobParameter> metricJobParameterList = jobExecutionParameter.getMetricParameterList();
        if (CollectionUtils.isNotEmpty(metricJobParameterList)) {
            for (BaseJobParameter parameter : metricJobParameterList) {
                String metricUniqueKey = getMetricUniqueKey(parameter);
                Map<String, String> metricInputParameter = metric2InputParameter.get(metricUniqueKey);
                metricInputParameter.put(METRIC_UNIQUE_KEY, metricUniqueKey);
                String metricType = parameter.getMetricType();
                SqlMetric sqlMetric = PluginLoader
                        .getPluginLoader(SqlMetric.class)
                        .getNewPlugin(metricType);

                MetricParserUtils.operateInputParameter(metricInputParameter, sqlMetric, jobExecutionInfo);

                List<TransformConfig> transformConfigs = new ArrayList<>();
                //get actual value execute sql
                MetricParserUtils.setTransformerConfig(
                        metricInputParameter,
                        transformConfigs,
                        getActualValueExecuteSql(metricInputParameter),
                        TransformType.ACTUAL_VALUE.getDescription());

                // get expected value execute sql
                MetricParserUtils.setTransformerConfig(
                        metricInputParameter,
                        transformConfigs,
                        getExceptedValueExecuteSql(metricInputParameter),
                        TransformType.EXPECTED_VALUE_FROM_TARGET_SOURCE.getDescription());

                metricInputParameter.put(UNIQUE_CODE, StringUtils.wrapperSingleQuotes(generateUniqueCode(metricInputParameter)));

                configuration.setTransformParameters(transformConfigs);

                metric2InputParameter.put(metricUniqueKey, metricInputParameter);
            }
        }
    }

    private ExecuteSql getActualValueExecuteSql(Map<String, String> inputParameter) {
        inputParameter.put(ACTUAL_TABLE, inputParameter.get(TABLE));
        String actualExecuteSql = inputParameter.get(ACTUAL_EXECUTE_SQL);
        if (StringUtils.isNotEmpty(actualExecuteSql)) {
            actualExecuteSql = actualExecuteSql.replace("as actual_value", "as actual_value_" + inputParameter.get(METRIC_UNIQUE_KEY));
        }
        return new ExecuteSql(actualExecuteSql, inputParameter.get(TABLE));
    }

    private ExecuteSql getExceptedValueExecuteSql(Map<String, String> inputParameter) {
        inputParameter.put(EXPECTED_TABLE, inputParameter.get(TABLE2));
        String expectedExecuteSql = inputParameter.get(EXPECTED_EXECUTE_SQL);
        if (StringUtils.isNotEmpty(expectedExecuteSql)) {
            expectedExecuteSql = expectedExecuteSql.replace("as expected_value", "as expected_value_" + inputParameter.get(METRIC_UNIQUE_KEY));
        }
        return new ExecuteSql(expectedExecuteSql, inputParameter.get(TABLE));
    }

    @Override
    public void buildSinkConfigs() throws DataVinesException {
        List<SinkConfig> sinkConfigs = new ArrayList<>();

        List<BaseJobParameter> metricJobParameterList = jobExecutionParameter.getMetricParameterList();
        if (CollectionUtils.isNotEmpty(metricJobParameterList)) {
            for (BaseJobParameter parameter : metricJobParameterList) {
                String metricUniqueKey = getMetricUniqueKey(parameter);
                Map<String, String> metricInputParameter = metric2InputParameter.get(metricUniqueKey);
                metricInputParameter.put(METRIC_UNIQUE_KEY, metricUniqueKey);
                String expectedType = jobExecutionInfo.getEngineType() + "_" + parameter.getExpectedType();
                ExpectedValue expectedValue = PluginLoader.getPluginLoader(ExpectedValue.class)
                        .getNewPlugin(expectedType);

                String validateResultSinkSql = SinkSqlBuilder.getJobExecutionResultSql()
                        .replace("${actual_value}", "${actual_value_" + metricUniqueKey + "}")
                        .replace("${expected_value}", "${expected_value_" + metricUniqueKey + "}");
                //get the task data storage parameter
                SinkConfig taskResultSinkConfig = getValidateResultDataSinkConfig(
                        expectedValue, validateResultSinkSql,  "dv_job_execution_result", metricInputParameter);
                taskResultSinkConfig.setType(SinkType.VALIDATE_RESULT.getDescription());
                sinkConfigs.add(taskResultSinkConfig);

                String actualValueSinkSql = SinkSqlBuilder.getActualValueSql()
                        .replace("${actual_value}", "${actual_value_" + metricUniqueKey + "}")
                        .replace("${expected_value}", "${expected_value_" + metricUniqueKey + "}");
                //get the actual value storage parameter
                SinkConfig actualValueSinkConfig = getValidateResultDataSinkConfig(
                        expectedValue, actualValueSinkSql, "dv_actual_values", metricInputParameter);
                actualValueSinkConfig.setType(SinkType.ACTUAL_VALUE.getDescription());
                sinkConfigs.add(actualValueSinkConfig);

                //get the error data storage parameter
                if (StringUtils.isNotEmpty(jobExecutionInfo.getErrorDataStorageType())
                        &&StringUtils.isNotEmpty(jobExecutionInfo.getErrorDataStorageParameter())) {
                    SinkConfig errorDataSinkConfig = new SinkConfig();
                    errorDataSinkConfig.setType(SinkType.ERROR_DATA.getDescription());

                    Map<String, Object> connectorParameterMap = new HashMap<>(JSONUtils.toMap(jobExecutionInfo.getErrorDataStorageParameter(),String.class, Object.class));
                    connectorParameterMap.putAll(metricInputParameter);
                    StorageFactory storageFactory = PluginLoader
                            .getPluginLoader(StorageFactory.class)
                            .getNewPlugin(jobExecutionInfo.getErrorDataStorageType());

                    if (storageFactory != null) {
                        connectorParameterMap = storageFactory.getStorageConnector().getParamMap(connectorParameterMap);
                        errorDataSinkConfig.setPlugin(jobExecutionInfo.getErrorDataStorageType());
                        connectorParameterMap.put(ERROR_DATA_FILE_NAME, jobExecutionInfo.getErrorDataFileName());
                        connectorParameterMap.put(ERROR_DATA_DIR, metricInputParameter.get(ERROR_DATA_DIR));
                        connectorParameterMap.put(METRIC_NAME, metricInputParameter.get(METRIC_NAME));
                        connectorParameterMap.put(INVALIDATE_ITEMS_TABLE, metricInputParameter.get(INVALIDATE_ITEMS_TABLE));
                        connectorParameterMap.put(INVALIDATE_ITEM_CAN_OUTPUT, metricInputParameter.get(INVALIDATE_ITEM_CAN_OUTPUT));
                        // use to get source type converter in sink
                        connectorParameterMap.put(SRC_CONNECTOR_TYPE, metricInputParameter.get(SRC_CONNECTOR_TYPE));
                        connectorParameterMap.put(JOB_EXECUTION_ID, metricInputParameter.get(JOB_EXECUTION_ID));
                        errorDataSinkConfig.setConfig(connectorParameterMap);

                        sinkConfigs.add(errorDataSinkConfig);
                    }
                }
            }
        }

        configuration.setSinkParameters(sinkConfigs);
    }
}
