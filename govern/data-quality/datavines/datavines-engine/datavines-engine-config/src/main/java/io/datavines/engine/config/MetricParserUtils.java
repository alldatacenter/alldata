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
import io.datavines.common.utils.Md5Utils;
import io.datavines.common.utils.StringUtils;

import org.apache.commons.collections4.MapUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.datavines.common.config.TransformConfig;
import io.datavines.common.entity.ExecuteSql;

import io.datavines.common.utils.placeholder.PlaceholderUtils;
import io.datavines.metric.api.SqlMetric;

import static io.datavines.engine.api.ConfigConstants.*;

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
            config.put(SQL, PlaceholderUtils.replacePlaceholders(executeSql.getSql(), inputParameterValueResult, true));
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
        config.put(SQL, PlaceholderUtils.replacePlaceholders(executeSql.getSql(), inputParameterValueResult,true));
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

        Map<String,String> newInputParameterValue = new HashMap<>(inputParameterValue);

        newInputParameterValue.remove(METRIC_TYPE);
        newInputParameterValue.remove(METRIC_DIMENSION);
        newInputParameterValue.remove(CREATE_TIME);
        newInputParameterValue.remove(UPDATE_TIME);
        newInputParameterValue.remove(JOB_EXECUTION_ID);
        newInputParameterValue.remove(RESULT_FORMULA);
        newInputParameterValue.remove(OPERATOR);
        newInputParameterValue.remove(THRESHOLD);
        newInputParameterValue.remove(DATA_TIME);
        newInputParameterValue.remove(ERROR_DATA_FILE_NAME);
        newInputParameterValue.remove(ERROR_DATA_DIR);
        newInputParameterValue.remove(EXPECTED_TYPE);
        newInputParameterValue.remove(EXPECTED_NAME);
        newInputParameterValue.remove(EXPECTED_VALUE);
        newInputParameterValue.remove(EXPECTED_TABLE);
        newInputParameterValue.remove(INVALIDATE_ITEMS_TABLE);
        newInputParameterValue.remove(SRC_CONNECTOR_TYPE);
        newInputParameterValue.remove(ACTUAL_TABLE);
        newInputParameterValue.remove(REGEX_KEY);
        newInputParameterValue.remove(NOT_REGEX_KEY);
        newInputParameterValue.remove(VALIDATE_RESULT_DATA_DIR);
        newInputParameterValue.remove(METRIC_UNIQUE_KEY);

        StringBuilder sb = new StringBuilder();
        for (String value : newInputParameterValue.values()) {
            sb.append(value);
        }

        return Md5Utils.getMd5(sb.toString(),true);
    }
}