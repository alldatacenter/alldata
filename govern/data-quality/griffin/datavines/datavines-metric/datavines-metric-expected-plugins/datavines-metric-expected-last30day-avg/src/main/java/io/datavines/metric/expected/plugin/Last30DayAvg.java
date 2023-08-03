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
package io.datavines.metric.expected.plugin;

import io.datavines.metric.api.ExpectedValue;

import java.util.Map;

public class Last30DayAvg implements ExpectedValue {

    @Override
    public String getName() {
        return "last_30d_avg";
    }

    @Override
    public String getZhName() {
        return "最近30天均值";
    }

    @Override
    public String getType() {
        return "last_30d_avg";
    }

    @Override
    public String getExecuteSql() {
        return "select round(avg(actual_value),2) as last_30d_avg from dv_actual_values where data_time >= date_sub(date_format(${data_time},'%Y-%m-%d'),interval 30 DAY) and  data_time < date_add(date_format(${data_time},'%Y-%m-%d'),interval 1 DAY) and unique_code = ${unique_code}";
    }

    @Override
    public String getOutputTable() {
        return "last_30d";
    }

    @Override
    public boolean isNeedDefaultDatasource() {
        return true;
    }

    @Override
    public void prepare(Map<String, String> config) {

    }
}
