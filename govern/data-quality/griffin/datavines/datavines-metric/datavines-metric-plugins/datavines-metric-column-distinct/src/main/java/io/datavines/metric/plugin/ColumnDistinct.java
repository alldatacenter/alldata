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
package io.datavines.metric.plugin;

import io.datavines.common.entity.ExecuteSql;
import io.datavines.common.enums.DataVinesDataType;
import io.datavines.metric.api.MetricDimension;
import io.datavines.metric.api.MetricType;
import io.datavines.metric.plugin.base.BaseSingleTableColumn;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.datavines.common.ConfigConstants.METRIC_UNIQUE_KEY;

public class ColumnDistinct extends BaseSingleTableColumn {

    public ColumnDistinct(){
        super();
    }

    @Override
    public String getName() {
        return "column_distinct";
    }

    @Override
    public String getZhName() {
        return "Distinct检查";
    }

    @Override
    public MetricDimension getDimension() {
        return MetricDimension.UNIQUENESS;
    }

    @Override
    public MetricType getType() {
        return MetricType.SINGLE_TABLE;
    }

    @Override
    public ExecuteSql getInvalidateItems(Map<String,String> inputParameter) {
        return null;
    }

    @Override
    public ExecuteSql getActualValue(Map<String,String> inputParameter) {
        String uniqueKey = inputParameter.get(METRIC_UNIQUE_KEY);
        ExecuteSql executeSql = new ExecuteSql();
        executeSql.setResultTable("invalidate_count_" + uniqueKey);
        StringBuilder actualValueSql = new StringBuilder();
        actualValueSql.append("select count(distinct(${column})) as actual_value_").append(uniqueKey).append(" from ${table}");
        if (filters.size() > 0) {
            actualValueSql.append(" where ").append(String.join(" and ", filters));
        }

        executeSql.setSql(actualValueSql.toString());
        executeSql.setErrorOutput(false);
        return executeSql;
    }

    @Override
    public List<DataVinesDataType> suitableType() {
        return Arrays.asList(DataVinesDataType.NUMERIC_TYPE, DataVinesDataType.STRING_TYPE, DataVinesDataType.DATE_TIME_TYPE);
    }
}
