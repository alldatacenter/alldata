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
package io.datavines.metric.plugin.base;

import io.datavines.common.config.CheckResult;
import io.datavines.common.config.ConfigChecker;
import io.datavines.common.entity.ExecuteSql;
import io.datavines.metric.api.ConfigItem;
import io.datavines.metric.api.MetricLevel;
import io.datavines.metric.api.SqlMetric;

import java.util.*;

import static io.datavines.common.ConfigConstants.METRIC_UNIQUE_KEY;

public abstract class BaseSingleTable implements SqlMetric {

    protected StringBuilder invalidateItemsSql = new StringBuilder("select * from ${table}");

    protected List<String> filters = new ArrayList<>();

    protected HashMap<String,ConfigItem> configMap = new HashMap<>();

    protected Set<String> requiredOptions = new HashSet<>();

    public BaseSingleTable() {
        configMap.put("table",new ConfigItem("table", "表名", "table"));
        configMap.put("filter",new ConfigItem("filter", "过滤条件", "filter"));

        requiredOptions.add("table");
    }

    @Override
    public ExecuteSql getInvalidateItems(Map<String,String> inputParameter) {
        ExecuteSql executeSql = new ExecuteSql();
        executeSql.setResultTable("invalidate_items_" + inputParameter.get(METRIC_UNIQUE_KEY));
        executeSql.setSql(invalidateItemsSql.toString());
        executeSql.setErrorOutput(isInvalidateItemsCanOutput());
        return executeSql;
    }

    @Override
    public ExecuteSql getActualValue(Map<String,String> inputParameter) {
        ExecuteSql executeSql = new ExecuteSql();
        String uniqueKey = inputParameter.get(METRIC_UNIQUE_KEY);
        executeSql.setResultTable("invalidate_count_" + uniqueKey);
        String actualValueSql = "select count(1) as actual_value_"+ uniqueKey +" from ${invalidate_items_table}";
        executeSql.setSql(actualValueSql);
        executeSql.setErrorOutput(false);
        return executeSql;
    }

    @Override
    public CheckResult validateConfig(Map<String, Object> config) {
        return ConfigChecker.checkConfig(config, requiredOptions);
    }

    @Override
    public void prepare(Map<String, String> config) {
        if (config.containsKey("filter")) {
            filters.add(config.get("filter"));
        }

        addFiltersIntoInvalidateItemsSql();
    }

    private void addFiltersIntoInvalidateItemsSql() {
        if (filters.size() > 0) {
            invalidateItemsSql.append(" where ").append(String.join(" and ", filters));
        }
    }

    @Override
    public MetricLevel getLevel() {
        return MetricLevel.TABLE;
    }
}