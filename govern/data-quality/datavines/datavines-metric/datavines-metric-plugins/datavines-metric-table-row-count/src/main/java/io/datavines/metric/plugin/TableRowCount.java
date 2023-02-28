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
import io.datavines.metric.api.ConfigItem;
import io.datavines.metric.api.MetricDimension;
import io.datavines.metric.api.MetricType;
import io.datavines.metric.plugin.base.BaseSingleTable;

import java.util.Map;
import java.util.Set;

public class TableRowCount extends BaseSingleTable {

    public TableRowCount() {
        super();
    }

    @Override
    public String getName() {
        return "table_row_count";
    }

    @Override
    public String getZhName() {
        return "表行数检查";
    }

    @Override
    public MetricDimension getDimension() {
        return MetricDimension.COMPLETENESS;
    }

    @Override
    public MetricType getType() {
        return MetricType.SINGLE_TABLE;
    }

    @Override
    public boolean isInvalidateItemsCanOutput() {
        return true;
    }

    @Override
    public void prepare(Map<String, String> config) {
        super.prepare(config);
    }

    @Override
    public Map<String, ConfigItem> getConfigMap() {
        return configMap;
    }

    @Override
    public ExecuteSql getInvalidateItems() {
        return null;
    }

    @Override
    public ExecuteSql getActualValue(String uniqueKey) {
        ExecuteSql executeSql = new ExecuteSql();
        executeSql.setResultTable("invalidate_count_" + uniqueKey);
        StringBuilder actualValueSql = new StringBuilder();
        actualValueSql.append("select count(1) as actual_value_").append(uniqueKey).append(" from ${table}");
        if (filters.size() > 0) {
            actualValueSql.append(" where ").append(String.join(" and ", filters));
        }

        executeSql.setSql(actualValueSql.toString());
        executeSql.setErrorOutput(false);
        return executeSql;
    }
}
