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

import java.util.*;

import io.datavines.common.config.CheckResult;
import io.datavines.common.config.ConfigChecker;
import io.datavines.common.entity.ExecuteSql;
import io.datavines.common.enums.DataVinesDataType;
import io.datavines.common.utils.StringUtils;
import io.datavines.metric.api.ConfigItem;
import io.datavines.metric.api.MetricDimension;
import io.datavines.metric.api.MetricType;
import io.datavines.metric.api.SqlMetric;

import static io.datavines.common.CommonConstants.TABLE;
import static io.datavines.common.ConfigConstants.*;
import static io.datavines.common.ConfigConstants.METRIC_UNIQUE_KEY;

public class CustomAggregateSql implements SqlMetric {

    private final Set<String> requiredOptions = new HashSet<>();

    private final HashMap<String,ConfigItem> configMap = new HashMap<>();

    public CustomAggregateSql() {
        configMap.put("table",new ConfigItem("table", "表名", "table"));
        configMap.put("actual_aggregate_sql", new ConfigItem("actual_aggregate_sql","自定义聚合SQL","actual_aggregate_sql"));

        requiredOptions.add("actual_aggregate_sql");
        requiredOptions.add("table");
    }

    @Override
    public String getName() {
        return "custom_aggregate_sql";
    }

    @Override
    public String getZhName() {
        return "自定义聚合SQL";
    }

    @Override
    public MetricDimension getDimension() {
        return MetricDimension.ACCURACY;
    }

    @Override
    public MetricType getType() {
        return MetricType.SINGLE_TABLE;
    }

    @Override
    public boolean isInvalidateItemsCanOutput() {
        return false;
    }

    @Override
    public CheckResult validateConfig(Map<String, Object> config) {
        return ConfigChecker.checkConfig(config, requiredOptions);
    }

    @Override
    public void prepare(Map<String, String> config) {

    }

    @Override
    public Map<String, ConfigItem> getConfigMap() {
        return configMap;
    }

    @Override
    public ExecuteSql getInvalidateItems(Map<String,String> inputParameter) {
        return null;
    }

    @Override
    public ExecuteSql getActualValue(Map<String,String> inputParameter) {
        inputParameter.put(ACTUAL_TABLE, inputParameter.get(TABLE));
        String actualAggregateSql = inputParameter.get(ACTUAL_AGGREGATE_SQL);
        if (StringUtils.isNotEmpty(actualAggregateSql)) {
            actualAggregateSql = actualAggregateSql.replace("as actual_value", "as actual_value_" + inputParameter.get(METRIC_UNIQUE_KEY));
        }
        return new ExecuteSql(actualAggregateSql, inputParameter.get(TABLE));
    }

    @Override
    public List<DataVinesDataType> suitableType() {
        return Collections.emptyList();
    }
}
