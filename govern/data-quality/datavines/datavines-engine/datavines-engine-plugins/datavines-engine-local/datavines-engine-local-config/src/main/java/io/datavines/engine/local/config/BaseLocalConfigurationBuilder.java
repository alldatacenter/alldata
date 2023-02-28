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
import io.datavines.common.entity.*;
import io.datavines.common.entity.job.BaseJobParameter;
import io.datavines.common.entity.job.DataQualityJobParameter;
import io.datavines.common.exception.DataVinesException;
import io.datavines.common.utils.StringUtils;
import io.datavines.connector.api.ConnectorFactory;
import io.datavines.engine.config.BaseJobConfigurationBuilder;
import io.datavines.metric.api.ExpectedValue;
import io.datavines.spi.PluginLoader;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;

import static io.datavines.engine.api.ConfigConstants.*;

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
        Set<String> connectorSet = new HashSet<>();
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
                    connectorParameterMap.put(OUTPUT_TABLE, outputTable);
                    connectorParameterMap.put(DRIVER, connectorFactory.getDialect().getDriver());
                    connectorParameterMap.put(SRC_CONNECTOR_TYPE, connectorParameter.getType());
                    metricInputParameter.put(REGEX_KEY, connectorFactory.getDialect().getRegexKey());
                    metricInputParameter.put(NOT_REGEX_KEY, connectorFactory.getDialect().getNotRegexKey());
                    metricInputParameter.put(STRING_TYPE, connectorFactory.getDialect().getStringType());
                    metricInputParameter.put(SRC_CONNECTOR_TYPE, connectorParameter.getType());
                    metricInputParameter.put(INVALIDATE_ITEM_CAN_OUTPUT, connectorFactory.getDialect().invalidateItemCanOutput());

                    if (connectorSet.contains(connectorUUID)) {
                        continue;
                    }
                    SourceConfig sourceConfig = new SourceConfig();
                    sourceConfig.setPlugin(connectorFactory.getCategory());
                    sourceConfig.setConfig(connectorParameterMap);
                    sourceConfig.setType(SourceType.NORMAL.getDescription());
                    sourceConfigs.add(sourceConfig);
                    connectorSet.add(connectorUUID);
                }

                if (jobExecutionParameter.getConnectorParameter2() != null && jobExecutionParameter.getConnectorParameter2().getParameters() !=null) {
                    ConnectorParameter connectorParameter2 = jobExecutionParameter.getConnectorParameter2();
                    Map<String, Object> connectorParameterMap = new HashMap<>(connectorParameter2.getParameters());
                    connectorParameterMap.putAll(metricInputParameter);

                    ConnectorFactory connectorFactory = PluginLoader
                            .getPluginLoader(ConnectorFactory.class)
                            .getNewPlugin(connectorParameter2.getType());

                    connectorParameterMap = connectorFactory.getConnectorParameterConverter().converter(connectorParameterMap);

                    String connectorUUID = connectorFactory.getConnectorParameterConverter().getConnectorUUID(connectorParameterMap);
                    String outputTable = metricInputParameter.get(TARGET_TABLE);
                    connectorParameterMap.put(OUTPUT_TABLE, outputTable);
                    connectorParameterMap.put(DRIVER, connectorFactory.getDialect().getDriver());

                    if (connectorSet.contains(connectorUUID)) {
                        continue;
                    }

                    SourceConfig sourceConfig = new SourceConfig();
                    sourceConfig.setPlugin(connectorFactory.getCategory());
                    sourceConfig.setConfig(connectorParameterMap);
                    sourceConfig.setType(SourceType.NORMAL.getDescription());
                    sourceConfigs.add(sourceConfig);
                    connectorSet.add(connectorUUID);
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
}
