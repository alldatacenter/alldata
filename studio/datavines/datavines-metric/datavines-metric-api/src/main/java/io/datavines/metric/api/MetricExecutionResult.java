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
package io.datavines.metric.api;

import com.baomidou.mybatisplus.annotation.TableField;
import io.datavines.common.exception.DataVinesException;
import lombok.Data;
import org.apache.commons.collections4.MapUtils;

import java.io.Serializable;
import java.util.Map;

@Data
public class MetricExecutionResult implements Serializable {

    private static final long serialVersionUID = -1L;

    private Double actualValue;

    private Double expectedValue;

    private String resultFormula;

    private String operator;

    private Double threshold;

    private String expectedType;

    private String metricName;

    private String metricDimension;

    private String metricType;

    private String databaseName;

    private String tableName;

    private String columnName;

    public MetricExecutionResult() {
    }

    public MetricExecutionResult(Map<String, Object> dataMap) {
        if (MapUtils.isEmpty(dataMap)) {
            throw new DataVinesException("data map is empty");
        }

        actualValue = Double.valueOf(String.valueOf(dataMap.get("actual_value")));
        expectedValue = Double.valueOf(String.valueOf(dataMap.get("expected_value")));
        resultFormula = String.valueOf(dataMap.get("result_formula"));
        operator = String.valueOf(dataMap.get("operator"));
        threshold = Double.valueOf(String.valueOf(dataMap.get("threshold")));
        metricName = String.valueOf(dataMap.get("metric_name"));
        metricDimension = String.valueOf(dataMap.get("metric_dimension"));
        metricType = String.valueOf(dataMap.get("metric_type"));
        databaseName = String.valueOf(dataMap.get("database_name"));
        tableName = String.valueOf(dataMap.get("table_name"));
        columnName = String.valueOf(dataMap.get("column_name"));
        expectedType = String.valueOf(dataMap.get("expected_type"));
    }
}