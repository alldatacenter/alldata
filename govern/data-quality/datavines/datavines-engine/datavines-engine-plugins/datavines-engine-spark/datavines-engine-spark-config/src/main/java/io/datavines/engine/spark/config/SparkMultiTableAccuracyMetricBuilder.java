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
import io.datavines.common.config.SourceConfig;
import io.datavines.common.entity.job.BaseJobParameter;
import io.datavines.common.entity.job.DataQualityJobParameter;
import io.datavines.common.exception.DataVinesException;
import io.datavines.common.utils.JSONUtils;
import io.datavines.common.utils.StringUtils;
import io.datavines.common.entity.MappingColumn;
import io.datavines.metric.api.ExpectedValue;
import io.datavines.spi.PluginLoader;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.datavines.common.CommonConstants.AND;
import static io.datavines.common.CommonConstants.TABLE;
import static io.datavines.common.CommonConstants.TABLE2;
import static io.datavines.engine.api.ConfigConstants.EXPECTED_VALUE;

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
                List<MappingColumn> mappingColumns = JSONUtils.toList(metricInputParameter.get("mappingColumns"),MappingColumn.class);
                metricInputParameter.put("on_clause", getOnClause(mappingColumns, metricInputParameter));
                metricInputParameter.put("where_clause", getWhereClause(mappingColumns, metricInputParameter));

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
                SinkConfig errorDataSinkConfig = getErrorSinkConfig();
                if (errorDataSinkConfig != null) {
                    sinkConfigs.add(errorDataSinkConfig);
                }
            }
        }

        configuration.setSinkParameters(sinkConfigs);
    }

    private String getOnClause(List<MappingColumn> mappingColumnList, Map<String,String> inputParameterValueResult) {
        //get on clause
        String[] columnList = new String[mappingColumnList.size()];
        for (int i = 0; i < mappingColumnList.size(); i++) {
            MappingColumn column = mappingColumnList.get(i);
            columnList[i] = getCoalesceString(inputParameterValueResult.get(TABLE),column.getColumn())
                    + column.getOperator()
                    + getCoalesceString(inputParameterValueResult.get(TABLE2),column.getColumn2());
        }

        return String.join(AND,columnList);
    }

    private String getWhereClause(List<MappingColumn> mappingColumnList,Map<String,String> inputParameterValueResult) {
        String columnNotNull = "( NOT (" + getColumnIsNullStr(inputParameterValueResult.get(TABLE),getColumnListInTable(mappingColumnList)) + " ))";
        String columnIsNull2 = "( " + getColumnIsNullStr(inputParameterValueResult.get(TABLE2),getColumnListInTable2(mappingColumnList)) + " )";

        return columnNotNull + AND + columnIsNull2;
    }

    private String getCoalesceString(String table, String column) {
        return "coalesce(" + table + "." + column + ", '')";
    }

    private String getColumnIsNullStr(String table, List<String> columns) {
        String[] columnList = new String[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            String column = columns.get(i);
            columnList[i] = table + "." + column + " IS NULL";
        }
        return  String.join(AND, columnList);
    }

    private List<String> getColumnListInTable(List<MappingColumn> mappingColumns) {
        List<String> list = new ArrayList<>();
        mappingColumns.forEach(item ->
                list.add(item.getColumn())
        );

        return list;
    }

    private List<String> getColumnListInTable2(List<MappingColumn> mappingColumns) {
        List<String> list = new ArrayList<>();
        mappingColumns.forEach(item ->
                list.add(item.getColumn2())
        );

        return list;
    }
}
