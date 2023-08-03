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
package io.datavines.engine.config;

import io.datavines.common.config.*;
import io.datavines.common.config.enums.SourceType;
import io.datavines.common.config.enums.TransformType;
import io.datavines.common.entity.*;
import io.datavines.common.entity.job.BaseJobParameter;
import io.datavines.common.exception.DataVinesException;
import io.datavines.common.utils.CommonPropertyUtils;
import io.datavines.common.utils.JSONUtils;
import io.datavines.common.utils.ParameterUtils;
import io.datavines.common.utils.StringUtils;
import io.datavines.common.utils.placeholder.PlaceholderUtils;
import io.datavines.metric.api.ExpectedValue;
import io.datavines.metric.api.SqlMetric;
import io.datavines.spi.PluginLoader;
import io.datavines.storage.api.StorageFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.datavines.common.ConfigConstants.*;
import static io.datavines.engine.config.MetricParserUtils.generateUniqueCode;

public abstract class BaseJobConfigurationBuilder implements JobConfigurationBuilder {

    protected final DataVinesJobConfig configuration = new DataVinesJobConfig();

    protected Map<String, String> inputParameter;

    protected JobExecutionParameter jobExecutionParameter;

    protected JobExecutionInfo jobExecutionInfo;

    protected Map<String, Map<String, String>> metric2InputParameter = new HashMap<>();

    protected boolean invalidateItemCanOutput = true;

    @Override
    public void init(Map<String, String> inputParameter, JobExecutionInfo jobExecutionInfo) {
        this.inputParameter = inputParameter;
        this.inputParameter.put(COLUMN, "");
        this.jobExecutionInfo = jobExecutionInfo;
        this.jobExecutionParameter = jobExecutionInfo.getJobExecutionParameter();

        this.inputParameter.put(ERROR_DATA_FILE_NAME, jobExecutionInfo.getErrorDataFileName());

        if ("file".equalsIgnoreCase(jobExecutionInfo.getErrorDataStorageType())) {
            Map<String,String> errorDataParameterMap = JSONUtils.toMap(jobExecutionInfo.getErrorDataStorageParameter(),String.class, String.class);
            this.inputParameter.put(ERROR_DATA_DIR, errorDataParameterMap.get("data_dir"));
            this.inputParameter.put(COLUMN_SEPARATOR,
                    errorDataParameterMap.get(CommonPropertyUtils.COLUMN_SEPARATOR) == null ?
                            CommonPropertyUtils.COLUMN_SEPARATOR_DEFAULT : errorDataParameterMap.get(CommonPropertyUtils.COLUMN_SEPARATOR));
            this.inputParameter.put(LINE_SEPERATOR,
                    errorDataParameterMap.get(CommonPropertyUtils.LINE_SEPARATOR) == null ?
                            CommonPropertyUtils.LINE_SEPARATOR_DEFAULT : errorDataParameterMap.get(CommonPropertyUtils.LINE_SEPARATOR));
        } else {
            this.inputParameter.put(ERROR_DATA_DIR, CommonPropertyUtils.getString(CommonPropertyUtils.ERROR_DATA_DIR, CommonPropertyUtils.ERROR_DATA_DIR_DEFAULT));
            this.inputParameter.put(COLUMN_SEPARATOR, CommonPropertyUtils.getString(CommonPropertyUtils.COLUMN_SEPARATOR, CommonPropertyUtils.COLUMN_SEPARATOR_DEFAULT));
            this.inputParameter.put(LINE_SEPERATOR, CommonPropertyUtils.getString(CommonPropertyUtils.LINE_SEPARATOR, CommonPropertyUtils.LINE_SEPARATOR_DEFAULT));
        }

        if ("file".equalsIgnoreCase(jobExecutionInfo.getValidateResultDataStorageType())) {
            Map<String,String> validateResultDataParameterMap = JSONUtils.toMap(jobExecutionInfo.getValidateResultDataStorageParameter(),String.class, String.class);
            this.inputParameter.put(VALIDATE_RESULT_DATA_DIR, validateResultDataParameterMap.get("data_dir"));
        } else {
            this.inputParameter.put(VALIDATE_RESULT_DATA_DIR, CommonPropertyUtils.getString(CommonPropertyUtils.VALIDATE_RESULT_DATA_DIR, CommonPropertyUtils.VALIDATE_RESULT_DATA_DIR_DEFAULT));
        }

        List<BaseJobParameter> metricJobParameterList = jobExecutionParameter.getMetricParameterList();
        if (CollectionUtils.isNotEmpty(metricJobParameterList)) {
            for (BaseJobParameter parameter : metricJobParameterList) {
                String metricUniqueKey = getMetricUniqueKey(parameter);
                Map<String, String> metricInputParameter = new HashMap<>(this.inputParameter);
                metricInputParameter.put(METRIC_UNIQUE_KEY, metricUniqueKey);
                if (parameter.getMetricParameter() != null) {
                    parameter.getMetricParameter().forEach((k, v) -> {
                        metricInputParameter.put(k, String.valueOf(v));
                    });
                }

                if (parameter.getExpectedParameter() != null) {
                    parameter.getExpectedParameter().forEach((k, v) -> {
                        metricInputParameter.put(k, String.valueOf(v));
                    });
                }

                metricInputParameter.put(RESULT_FORMULA, String.valueOf(parameter.getResultFormula()));
                metricInputParameter.put(OPERATOR, String.valueOf(parameter.getOperator()));
                metricInputParameter.put(THRESHOLD, String.valueOf(parameter.getThreshold()));
                metricInputParameter.put(EXPECTED_TYPE, StringUtils.wrapperSingleQuotes(parameter.getExpectedType()));

                metric2InputParameter.put(metricUniqueKey, metricInputParameter);
            }
        }
    }

    @Override
    public void buildName() {
        configuration.setName(jobExecutionInfo.getName());
    }

    @Override
    public void buildEnvConfig() {
        configuration.setEnvConfig(getEnvConfig());
    }

    @Override
    public void buildSourceConfigs() throws DataVinesException {
        configuration.setSourceParameters(getSourceConfigs());
    }

    @Override
    public void buildTransformConfigs() {
        List<TransformConfig> transformConfigs = new ArrayList<>();
        List<BaseJobParameter> metricJobParameterList = jobExecutionParameter.getMetricParameterList();
        if (CollectionUtils.isNotEmpty(metricJobParameterList)) {
            for (BaseJobParameter parameter : metricJobParameterList) {
                String metricUniqueKey = getMetricUniqueKey(parameter);
                Map<String, String> metricInputParameter = metric2InputParameter.get(metricUniqueKey);

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

    @Override
    public DataVinesJobConfig build() {
        return configuration;
    }

    protected abstract EnvConfig getEnvConfig();

    protected abstract List<SourceConfig> getSourceConfigs() throws DataVinesException;

    protected SourceConfig getValidateResultDataSourceConfig() throws DataVinesException {

        SourceConfig actualValueSourceConfig = new SourceConfig();
        StorageFactory storageFactory =
                PluginLoader.getPluginLoader(StorageFactory.class)
                        .getOrCreatePlugin(jobExecutionInfo.getValidateResultDataStorageType());

        actualValueSourceConfig.setPlugin(storageFactory.getCategory());
        actualValueSourceConfig.setType(SourceType.METADATA.getDescription());
        actualValueSourceConfig.setConfig(getValidateResultSourceConfigMap(null,"dv_actual_values"));
        return actualValueSourceConfig;
    }

    protected SinkConfig getValidateResultDataSinkConfig(ExpectedValue expectedValue, String sql, String dbTable, Map<String, String> inputParameter) throws DataVinesException {

        SinkConfig validateResultDataStorageConfig = new SinkConfig();
        validateResultDataStorageConfig.setPlugin(jobExecutionInfo.getValidateResultDataStorageType());
        Map<String, Object> configMap = getValidateResultSourceConfigMap(
                ParameterUtils.convertParameterPlaceholders(sql, inputParameter),dbTable);
        configMap.put(JOB_EXECUTION_ID, jobExecutionInfo.getId());
        configMap.put(INVALIDATE_ITEMS_TABLE, inputParameter.get(INVALIDATE_ITEMS_TABLE));

        if (expectedValue != null && StringUtils.isNotEmpty(expectedValue.getOutputTable())) {
            inputParameter.put(EXPECTED_VALUE, expectedValue.getName());
            configMap.put(EXPECTED_VALUE, expectedValue.getName());
        }

        validateResultDataStorageConfig.setConfig(configMap);

        return validateResultDataStorageConfig;
    }

    private Map<String,Object> getValidateResultSourceConfigMap(String sql, String dbTable) {
        Map<String, Object> configMap = new HashMap<>();
        StorageFactory storageFactory =
                PluginLoader.getPluginLoader(StorageFactory.class)
                        .getOrCreatePlugin(jobExecutionInfo.getValidateResultDataStorageType());
        if (storageFactory != null) {
            if (StringUtils.isNotEmpty(jobExecutionInfo.getValidateResultDataStorageParameter())) {
                configMap = storageFactory.getStorageConnector().getParamMap(JSONUtils.toMap(jobExecutionInfo.getValidateResultDataStorageParameter(), String.class, Object.class));
            }
        }

        configMap.put(TABLE, dbTable);
        configMap.put(OUTPUT_TABLE, dbTable);
        if (StringUtils.isNotEmpty(sql)) {
            configMap.put(SQL, sql);
        }

        return configMap;
    }

    protected String getMetricUniqueKey(BaseJobParameter parameter) {
        return DigestUtils.md5Hex(String.format("%s_%s_%s_%s_%s",
                parameter.getMetricType(),
                parameter.getMetricParameter().get("metric_database"),
                parameter.getMetricParameter().get("table"),
                parameter.getMetricParameter().get("column"),
                jobExecutionInfo.getId()));
    }
}
