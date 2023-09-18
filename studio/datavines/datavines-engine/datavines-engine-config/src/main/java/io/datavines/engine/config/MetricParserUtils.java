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

import io.datavines.common.entity.JobExecutionInfo;
import io.datavines.common.entity.MappingColumn;
import io.datavines.common.utils.Md5Utils;
import io.datavines.common.utils.ParameterUtils;
import io.datavines.common.utils.StringUtils;

import io.datavines.metric.api.ConfigItem;
import io.datavines.spi.PluginLoader;
import org.apache.commons.collections4.MapUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.datavines.common.config.TransformConfig;
import io.datavines.common.entity.ExecuteSql;
import io.datavines.metric.api.SqlMetric;

import static io.datavines.common.CommonConstants.AND;
import static io.datavines.common.CommonConstants.TABLE;
import static io.datavines.common.CommonConstants.TABLE2;
import static io.datavines.common.ConfigConstants.*;

public class MetricParserUtils {

    public static void operateInputParameter(Map<String, String> inputParameter,
                                             SqlMetric sqlMetric,
                                             JobExecutionInfo jobExecutionInfo) {
        DateTimeFormatter datetimeFormat = DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM_SS);
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(YYYY_MM_DD);
        LocalDateTime time = LocalDateTime.now();
        String now = datetimeFormat.format(time);

        inputParameter.put(METRIC_TYPE, StringUtils.wrapperSingleQuotes(sqlMetric.getType().getDescription()));
        inputParameter.put(METRIC_NAME, StringUtils.wrapperSingleQuotes(sqlMetric.getName()));
        inputParameter.put(METRIC_DIMENSION, StringUtils.wrapperSingleQuotes(sqlMetric.getDimension().getDescription()));
        inputParameter.put(CREATE_TIME, StringUtils.wrapperSingleQuotes(now));
        inputParameter.put(UPDATE_TIME, StringUtils.wrapperSingleQuotes(now));
        inputParameter.put(JOB_EXECUTION_ID, String.valueOf(jobExecutionInfo.getId()));

        if (StringUtils.isEmpty(inputParameter.get(DATA_TIME))) {
            inputParameter.put(DATA_TIME, StringUtils.wrapperSingleQuotes(now));
        }

        if (StringUtils.isEmpty(inputParameter.get(DATA_DATE))) {
            inputParameter.put(DATA_DATE, StringUtils.wrapperSingleQuotes(dateFormat.format(time)));
        }

        if (StringUtils.isNotEmpty(inputParameter.get(REGEXP_PATTERN))) {
            inputParameter.put(REGEXP_PATTERN, StringUtils.escapeJava(
                    StringUtils.escapeJava(inputParameter.get(REGEXP_PATTERN))));
        }

        sqlMetric.prepare(inputParameter);
    }

    public static void setTransformerConfig(Map<String, String> inputParameterValueResult,
                                            List<TransformConfig> transformerConfigList,
                                            List<ExecuteSql> executeSqlList) {
        int index = 0;
        for (ExecuteSql executeSql: executeSqlList) {
            if (StringUtils.isEmpty(executeSql.getSql())
                    || StringUtils.isEmpty(executeSql.getResultTable())) {
                continue;
            }
            Map<String,Object> config = new HashMap<>();
            config.put(INDEX,index++);
            config.put(SQL, ParameterUtils.convertParameterPlaceholders(executeSql.getSql(), inputParameterValueResult));
            config.put(OUTPUT_TABLE,executeSql.getResultTable());

            TransformConfig transformerConfig = new TransformConfig(SQL,config);
            transformerConfigList.add(transformerConfig);
        }
    }

    public static void setTransformerConfig(Map<String, String> inputParameterValueResult,
                                            List<TransformConfig> transformerConfigList,
                                            ExecuteSql executeSql,
                                            String type) {
        int index = 0;

        if (StringUtils.isEmpty(executeSql.getSql())
                || StringUtils.isEmpty(executeSql.getResultTable())) {
            return;
        }

        Map<String,Object> config = new HashMap<>();
        config.put(INDEX, index++);
        config.put(SQL, ParameterUtils.convertParameterPlaceholders(executeSql.getSql(), inputParameterValueResult));
        config.put(OUTPUT_TABLE, executeSql.getResultTable());
        config.put(INVALIDATE_ITEMS_TABLE, inputParameterValueResult.get(INVALIDATE_ITEMS_TABLE));
        TransformConfig transformerConfig = new TransformConfig(SQL, config);
        transformerConfig.setType(type);
        transformerConfigList.add(transformerConfig);
    }

    /**
     * the unique code use to get the same type and condition task statistics value
     */
    public static String generateUniqueCode(Map<String, String> inputParameterValue) {

        if (MapUtils.isEmpty(inputParameterValue)) {
            return "-1";
        }

        Map<String,String> newInputParameterValue = new HashMap<>();
        newInputParameterValue.put(METRIC_NAME, inputParameterValue.get(METRIC_NAME));
        newInputParameterValue.put(METRIC_DATABASE, inputParameterValue.get(METRIC_DATABASE));
        SqlMetric sqlMetric = PluginLoader.getPluginLoader(SqlMetric.class).getOrCreatePlugin(StringUtils.removeSingeQuotes(inputParameterValue.get(METRIC_NAME)));
        Map<String, ConfigItem> configMap = sqlMetric.getConfigMap();
        if (MapUtils.isNotEmpty(configMap)) {
            for(ConfigItem configItem : configMap.values()) {
                if (StringUtils.isNotEmpty(inputParameterValue.get(configItem.getKey()))) {
                    newInputParameterValue.put(configItem.getKey(), inputParameterValue.get(configItem.getKey()));
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        for (String value : newInputParameterValue.values()) {
            sb.append(value);
        }

        return Md5Utils.getMd5(sb.toString(),true);
    }

    public static String getOnClause(List<MappingColumn> mappingColumnList, Map<String,String> inputParameterValueResult) {
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

    public static String getWhereClause(List<MappingColumn> mappingColumnList,Map<String,String> inputParameterValueResult) {
        String columnNotNull = "( NOT (" + getColumnIsNullStr(inputParameterValueResult.get(TABLE),getColumnListInTable(mappingColumnList)) + " ))";
        String columnIsNull2 = "( " + getColumnIsNullStr(inputParameterValueResult.get(TABLE2),getColumnListInTable2(mappingColumnList)) + " )";

        return columnNotNull + AND + columnIsNull2;
    }

    public static String getCoalesceString(String table, String column) {
        return "coalesce(" + table + "." + column + ", '')";
    }

    public static String getColumnIsNullStr(String table, List<String> columns) {
        String[] columnList = new String[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            String column = columns.get(i);
            columnList[i] = table + "." + column + " IS NULL";
        }
        return  String.join(AND, columnList);
    }

    public static List<String> getColumnListInTable(List<MappingColumn> mappingColumns) {
        List<String> list = new ArrayList<>();
        mappingColumns.forEach(item ->
                list.add(item.getColumn())
        );

        return list;
    }

    public static List<String> getColumnListInTable2(List<MappingColumn> mappingColumns) {
        List<String> list = new ArrayList<>();
        mappingColumns.forEach(item ->
                list.add(item.getColumn2())
        );

        return list;
    }
}