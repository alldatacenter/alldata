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

import io.datavines.metric.api.ConfigItem;
import io.datavines.metric.api.MetricDimension;
import io.datavines.metric.api.MetricType;
import io.datavines.metric.plugin.base.BaseSingleTableColumn;

import java.util.Map;

public class Freshness extends BaseSingleTableColumn {

    public Freshness(){
        configMap.put("begin_time",new ConfigItem("begin_time", "开始时间", "begin_time"));
        configMap.put("deadline_time",new ConfigItem("deadline_time", "结束时间", "deadline_time"));
        configMap.put("datetime_format",new ConfigItem("datetime_format", "时间格式", "datetime_format"));

        requiredOptions.add("begin_time");
        requiredOptions.add("deadline_time");
        requiredOptions.add("datetime_format");
    }

    @Override
    public String getName() {
        return "freshness";
    }

    @Override
    public String getZhName() {
        return "及时性检查";
    }

    @Override
    public MetricDimension getDimension() {
        return MetricDimension.TIMELINESS;
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

        if (config.containsKey("column") && config.containsKey("datetime_format") && config.containsKey("deadline_time") && config.containsKey("begin_time")) {
            filters.add("  (DATE_FORMAT(${column}, '${datetime_format}') <= DATE_FORMAT('${deadline_time}', '${datetime_format}') ) AND (DATE_FORMAT(${column}, '${datetime_format}') >= DATE_FORMAT('${begin_time}', '${datetime_format}')) ");
        }
        super.prepare(config);
    }

}
