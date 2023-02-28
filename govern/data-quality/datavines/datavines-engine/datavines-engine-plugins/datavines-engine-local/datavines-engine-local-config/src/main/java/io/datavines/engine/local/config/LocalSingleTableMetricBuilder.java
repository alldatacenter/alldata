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
import io.datavines.common.config.enums.SinkType;
import io.datavines.common.entity.job.BaseJobParameter;
import io.datavines.common.exception.DataVinesException;
import io.datavines.common.utils.JSONUtils;
import io.datavines.common.utils.StringUtils;
import io.datavines.metric.api.ExpectedValue;
import io.datavines.spi.PluginLoader;
import io.datavines.storage.api.StorageFactory;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.datavines.engine.api.ConfigConstants.*;

public class LocalSingleTableMetricBuilder extends BaseLocalConfigurationBuilder {

    @Override
    public void buildSinkConfigs() throws DataVinesException {

        List<SinkConfig> sinkConfigs = new ArrayList<>();

        List<BaseJobParameter> metricJobParameterList = jobExecutionParameter.getMetricParameterList();
        if (CollectionUtils.isNotEmpty(metricJobParameterList)) {
            for (BaseJobParameter parameter : metricJobParameterList) {
                String metricUniqueKey = getMetricUniqueKey(parameter);
                Map<String, String> metricInputParameter = metric2InputParameter.get(metricUniqueKey);
                String expectedType = jobExecutionInfo.getEngineType() + "_" + parameter.getExpectedType();
                ExpectedValue expectedValue = PluginLoader.getPluginLoader(ExpectedValue.class)
                        .getNewPlugin(expectedType);

                String validateResultSinkSql = SinkSqlBuilder.getTaskResultSql().replace("${actual_value}", "${actual_value_" + metricUniqueKey + "}");
                //get the task data storage parameter
                SinkConfig taskResultSinkConfig = getValidateResultDataSinkConfig(
                        expectedValue, validateResultSinkSql,  "dv_job_execution_result", metricInputParameter);
                taskResultSinkConfig.setType(SinkType.VALIDATE_RESULT.getDescription());
                sinkConfigs.add(taskResultSinkConfig);

                String actualValueSinkSql = SinkSqlBuilder.getActualValueSql().replace("${actual_value}", "${actual_value_" + metricUniqueKey + "}");
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
