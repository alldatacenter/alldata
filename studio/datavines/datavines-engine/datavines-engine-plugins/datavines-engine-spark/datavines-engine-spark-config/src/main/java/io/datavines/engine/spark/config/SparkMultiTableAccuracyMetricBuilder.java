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
package io.datavines.engine.spark.config;

import io.datavines.common.config.SinkConfig;
import io.datavines.common.entity.job.BaseJobParameter;
import io.datavines.common.exception.DataVinesException;
import io.datavines.common.utils.JSONUtils;
import io.datavines.common.utils.StringUtils;
import io.datavines.common.entity.MappingColumn;
import io.datavines.engine.config.MetricParserUtils;
import io.datavines.metric.api.ExpectedValue;
import io.datavines.spi.PluginLoader;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.datavines.common.ConfigConstants.*;

/**
 * multi table accuracy metric just contain one metric parameter ,
 * the parameter contain table1 and table2 properties
 */
public class SparkMultiTableAccuracyMetricBuilder extends BaseSparkConfigurationBuilder {

    @Override
    public void buildTransformConfigs() {

        List<BaseJobParameter> metricJobParameterList = jobExecutionParameter.getMetricParameterList();
        if (CollectionUtils.isNotEmpty(metricJobParameterList)) {
            for (BaseJobParameter parameter : metricJobParameterList) {
                String metricUniqueKey = getMetricUniqueKey(parameter);
                Map<String, String> metricInputParameter = metric2InputParameter.get(metricUniqueKey);
                metricInputParameter.put(METRIC_UNIQUE_KEY, metricUniqueKey);
                List<MappingColumn> mappingColumns = JSONUtils.toList(metricInputParameter.get(MAPPING_COLUMNS),MappingColumn.class);
                metricInputParameter.put(ON_CLAUSE, MetricParserUtils.getOnClause(mappingColumns, metricInputParameter));
                metricInputParameter.put(WHERE_CLAUSE, MetricParserUtils.getWhereClause(mappingColumns, metricInputParameter));

                metric2InputParameter.put(metricUniqueKey, metricInputParameter);
            }
        }

        super.buildTransformConfigs();
    }

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

                //get the actual value storage parameter
                SinkConfig actualValueSinkConfig = getValidateResultDataSinkConfig(
                        expectedValue, SparkSinkSqlBuilder.getActualValueSql(),  "dv_actual_values", metricInputParameter);
                sinkConfigs.add(actualValueSinkConfig);

                String taskSinkSql = SparkSinkSqlBuilder.getDefaultSinkSql();
                if (StringUtils.isEmpty(expectedValue.getOutputTable())) {
                    taskSinkSql = taskSinkSql.replaceAll("full join \\$\\{expected_table}","");
                }

                //get the task data storage parameter
                SinkConfig taskResultSinkConfig = getValidateResultDataSinkConfig(
                        expectedValue, taskSinkSql, "dv_job_execution_result", metricInputParameter);
                sinkConfigs.add(taskResultSinkConfig);

                //get the error data storage parameter
                //support file(hdfs/minio/s3)/es
                SinkConfig errorDataSinkConfig = getErrorSinkConfig(metricInputParameter);
                if (errorDataSinkConfig != null) {
                    sinkConfigs.add(errorDataSinkConfig);
                }
            }
        }

        configuration.setSinkParameters(sinkConfigs);
    }

}
