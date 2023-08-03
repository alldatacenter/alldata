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

import io.datavines.common.config.*;
import io.datavines.common.config.enums.SourceType;
import io.datavines.common.config.enums.TransformType;
import io.datavines.common.entity.*;
import io.datavines.common.entity.job.BaseJobParameter;
import io.datavines.common.exception.DataVinesException;
import io.datavines.common.utils.StringUtils;
import io.datavines.connector.api.ConnectorFactory;
import io.datavines.engine.config.BaseJobConfigurationBuilder;
import io.datavines.engine.config.MetricParserUtils;
import io.datavines.metric.api.ExpectedValue;
import io.datavines.metric.api.SqlMetric;
import io.datavines.spi.PluginLoader;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;

import static io.datavines.common.CommonConstants.DATABASE2;
import static io.datavines.common.CommonConstants.TABLE2;
import static io.datavines.common.ConfigConstants.*;
import static io.datavines.engine.config.MetricParserUtils.generateUniqueCode;

public abstract class BaseLocalConfigurationBuilder extends BaseJobConfigurationBuilder {

    @Override
    protected EnvConfig getEnvConfig() {
        EnvConfig envConfig = new EnvConfig();
        envConfig.setEngine(jobExecutionInfo.getEngineType());
        return envConfig;
    }

    @Override
    protected List<SourceConfig> getSourceConfigs() throws DataVinesException {
        List<SourceConfig> sourceConfigs = new ArrayList<>();
        boolean isAddValidateResultDataSource = false;
        Set<String> sourceConnectorSet = new HashSet<>();
        Set<String> targetConnectorSet = new HashSet<>();
        List<BaseJobParameter> metricJobParameterList = jobExecutionParameter.getMetricParameterList();
        if (CollectionUtils.isNotEmpty(metricJobParameterList)) {
            for (BaseJobParameter parameter : metricJobParameterList) {
                String metricUniqueKey = getMetricUniqueKey(parameter);
                Map<String, String> metricInputParameter = metric2InputParameter.get(metricUniqueKey);
                if (jobExecutionParameter.getConnectorParameter() != null) {
                    ConnectorParameter connectorParameter = jobExecutionParameter.getConnectorParameter();
                    ConnectorFactory connectorFactory = PluginLoader
                            .getPluginLoader(ConnectorFactory.class)
                            .getNewPlugin(connectorParameter.getType());
                    Map<String, Object> connectorParameterMap = new HashMap<>(connectorParameter.getParameters());
                    connectorParameterMap.putAll(metricInputParameter);
                    connectorParameterMap = connectorFactory.getConnectorParameterConverter().converter(connectorParameterMap);
                    String connectorUUID = connectorFactory.getConnectorParameterConverter().getConnectorUUID(connectorParameterMap);

                    String outputTable = metricInputParameter.get(TABLE);
                    connectorParameterMap.put(DATABASE, metricInputParameter.get(METRIC_DATABASE));
                    connectorParameterMap.put(OUTPUT_TABLE, outputTable);
                    connectorParameterMap.put(DRIVER, connectorFactory.getDialect().getDriver());
                    connectorParameterMap.put(SRC_CONNECTOR_TYPE, connectorParameter.getType());
                    metricInputParameter.putAll(connectorFactory.getDialect().getDialectKeyMap());
                    metricInputParameter.put(SRC_CONNECTOR_TYPE, connectorParameter.getType());
                    invalidateItemCanOutput &= Boolean.parseBoolean(connectorFactory.getDialect().invalidateItemCanOutput());
                    metricInputParameter.put(INVALIDATE_ITEM_CAN_OUTPUT, String.valueOf(invalidateItemCanOutput));

                    if (sourceConnectorSet.contains(connectorUUID)) {
                        continue;
                    }

                    SourceConfig sourceConfig = new SourceConfig();
                    sourceConfig.setPlugin(connectorFactory.getCategory());
                    sourceConfig.setConfig(connectorParameterMap);
                    sourceConfig.setType(SourceType.SOURCE.getDescription());
                    sourceConfigs.add(sourceConfig);
                    sourceConnectorSet.add(connectorUUID);
                }

                if (jobExecutionParameter.getConnectorParameter2() != null && jobExecutionParameter.getConnectorParameter2().getParameters() !=null) {
                    ConnectorParameter connectorParameter2 = jobExecutionParameter.getConnectorParameter2();
                    Map<String, Object> connectorParameterMap = new HashMap<>(connectorParameter2.getParameters());
                    connectorParameterMap.putAll(metricInputParameter);
                    connectorParameterMap.put(TABLE,metricInputParameter.get(TABLE2));
                    ConnectorFactory connectorFactory = PluginLoader
                            .getPluginLoader(ConnectorFactory.class)
                            .getNewPlugin(connectorParameter2.getType());

                    connectorParameterMap = connectorFactory.getConnectorParameterConverter().converter(connectorParameterMap);
                    String connectorUUID = connectorFactory.getConnectorParameterConverter().getConnectorUUID(connectorParameterMap);

                    String outputTable = metricInputParameter.get(TABLE2);
                    connectorParameterMap.put(OUTPUT_TABLE, outputTable);
                    connectorParameterMap.put(DRIVER, connectorFactory.getDialect().getDriver());
                    connectorParameterMap.put(SRC_CONNECTOR_TYPE, connectorParameter2.getType());
                    metricInputParameter.putAll(connectorFactory.getDialect().getDialectKeyMap());
                    metricInputParameter.put(SRC_CONNECTOR_TYPE, connectorParameter2.getType());
                    invalidateItemCanOutput &= Boolean.parseBoolean(connectorFactory.getDialect().invalidateItemCanOutput());
                    metricInputParameter.put(INVALIDATE_ITEM_CAN_OUTPUT, String.valueOf(invalidateItemCanOutput));
                    if (targetConnectorSet.contains(connectorUUID)) {
                        continue;
                    }

                    SourceConfig sourceConfig = new SourceConfig();
                    sourceConfig.setPlugin(connectorFactory.getCategory());
                    sourceConfig.setConfig(connectorParameterMap);
                    sourceConfig.setType(SourceType.TARGET.getDescription());
                    sourceConfigs.add(sourceConfig);
                    targetConnectorSet.add(connectorUUID);
                }

                String expectedType = jobExecutionInfo.getEngineType() + "_" + parameter.getExpectedType();

                ExpectedValue expectedValue = PluginLoader
                        .getPluginLoader(ExpectedValue.class)
                        .getNewPlugin(expectedType);

                if (expectedValue.isNeedDefaultDatasource() && !isAddValidateResultDataSource) {
                    sourceConfigs.add(getValidateResultDataSourceConfig());
                    isAddValidateResultDataSource = true;
                }

                metric2InputParameter.put(metricUniqueKey, metricInputParameter);
            }
        }

        return sourceConfigs;
    }

    @Override
    public void buildTransformConfigs() {
        List<TransformConfig> transformConfigs = new ArrayList<>();
        List<BaseJobParameter> metricJobParameterList = jobExecutionParameter.getMetricParameterList();
        if (CollectionUtils.isNotEmpty(metricJobParameterList)) {
            for (BaseJobParameter parameter : metricJobParameterList) {
                String metricUniqueKey = getMetricUniqueKey(parameter);
                Map<String, String> metricInputParameter = metric2InputParameter.get(metricUniqueKey);
                metricInputParameter.put(TABLE, metricInputParameter.get(METRIC_DATABASE)+"."+metricInputParameter.get(TABLE));
                metricInputParameter.put(TABLE2, metricInputParameter.get(DATABASE2)+"."+metricInputParameter.get(TABLE2));
                String metricType = parameter.getMetricType();
                SqlMetric sqlMetric = PluginLoader
                        .getPluginLoader(SqlMetric.class)
                        .getNewPlugin(metricType);

                MetricParserUtils.operateInputParameter(metricInputParameter, sqlMetric, jobExecutionInfo);
                invalidateItemCanOutput &= sqlMetric.isInvalidateItemsCanOutput();
                metricInputParameter.put(INVALIDATE_ITEM_CAN_OUTPUT, String.valueOf(invalidateItemCanOutput));

                // generate invalidate item execute sql
                if (sqlMetric.getInvalidateItems(metricInputParameter) != null) {
                    ExecuteSql invalidateItemExecuteSql = sqlMetric.getInvalidateItems(metricInputParameter);
                    metricInputParameter.put(INVALIDATE_ITEMS_TABLE, invalidateItemExecuteSql.getResultTable());
                    invalidateItemExecuteSql.setResultTable(invalidateItemExecuteSql.getResultTable());
                    MetricParserUtils.setTransformerConfig(
                            metricInputParameter,
                            transformConfigs,
                            invalidateItemExecuteSql,
                            TransformType.INVALIDATE_ITEMS.getDescription());
                }

                // generate actual value execute sql
                ExecuteSql actualValueExecuteSql = sqlMetric.getActualValue(metricInputParameter);
                if (actualValueExecuteSql != null) {
                    actualValueExecuteSql.setResultTable(sqlMetric.getActualValue(metricInputParameter).getResultTable());
                    MetricParserUtils.setTransformerConfig(
                            metricInputParameter,
                            transformConfigs,
                            actualValueExecuteSql,
                            TransformType.ACTUAL_VALUE.getDescription());
                    metricInputParameter.put(ACTUAL_TABLE, sqlMetric.getActualValue(metricInputParameter).getResultTable());
                }

                // generate expected value transform sql
                String expectedType = jobExecutionInfo.getEngineType() + "_" + parameter.getExpectedType();
                ExpectedValue expectedValue = PluginLoader
                        .getPluginLoader(ExpectedValue.class)
                        .getNewPlugin(expectedType);

                ExecuteSql expectedValueExecuteSql =
                        new ExecuteSql(expectedValue.getExecuteSql(), expectedValue.getOutputTable());

                if (StringUtils.isNotEmpty(expectedValueExecuteSql.getResultTable())) {
                    metricInputParameter.put(EXPECTED_TABLE, expectedValueExecuteSql.getResultTable());
                }

                metricInputParameter.put(UNIQUE_CODE, StringUtils.wrapperSingleQuotes(generateUniqueCode(metricInputParameter)));

                if (expectedValue.isNeedDefaultDatasource()) {
                    MetricParserUtils.setTransformerConfig(metricInputParameter, transformConfigs,
                            expectedValueExecuteSql, TransformType.EXPECTED_VALUE_FROM_METADATA_SOURCE.getDescription());
                } else {
                    MetricParserUtils.setTransformerConfig(metricInputParameter, transformConfigs,
                            expectedValueExecuteSql, TransformType.EXPECTED_VALUE_FROM_SOURCE.getDescription());
                }
                metric2InputParameter.put(metricUniqueKey, metricInputParameter);
            }

            configuration.setTransformParameters(transformConfigs);
        }
    }
}
