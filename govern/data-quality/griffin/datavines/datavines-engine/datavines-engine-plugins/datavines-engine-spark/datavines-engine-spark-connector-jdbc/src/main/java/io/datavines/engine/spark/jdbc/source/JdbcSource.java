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
package io.datavines.engine.spark.jdbc.source;

import io.datavines.common.utils.StringUtils;
import org.apache.spark.sql.DataFrameReader;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.util.*;
import java.util.stream.Collectors;

import io.datavines.common.config.CheckResult;
import io.datavines.common.config.Config;
import io.datavines.common.utils.TypesafeConfigUtils;
import io.datavines.engine.api.env.RuntimeEnvironment;
import io.datavines.engine.spark.api.SparkRuntimeEnvironment;
import io.datavines.engine.spark.api.batch.SparkBatchSource;

public class JdbcSource implements SparkBatchSource {

    private Config config = new Config();

    @Override
    public void setConfig(Config config) {
        if(config != null) {
            this.config = config;
        }
    }

    @Override
    public Config getConfig() {
        return config;
    }

    @Override
    public CheckResult checkConfig() {
        List<String> requiredOptions = Arrays.asList("url", "table", "user");

        List<String> nonExistsOptions = new ArrayList<>();
        requiredOptions.forEach(x->{
            if(!config.has(x)){
                nonExistsOptions.add(x);
            }
        });

        if (!nonExistsOptions.isEmpty()) {
            return new CheckResult(
                    false,
                    "please specify " + nonExistsOptions.stream().map(option ->
                            "[" + option + "]").collect(Collectors.joining(",")) + " as non-empty string");
        } else {
            return new CheckResult(true, "");
        }
    }

    @Override
    public void prepare(RuntimeEnvironment prepareEnv) {

    }

    @Override
    public Dataset<Row> getData(SparkRuntimeEnvironment env) {
        Properties properties = new Properties();
        properties.setProperty("user", config.getString("user"));
        properties.setProperty("driver", config.getString("driver"));
        if (StringUtils.isNotEmpty(config.getString("password")) && !"null".equalsIgnoreCase(config.getString("password"))) {
            properties.put("password", config.getString("password"));
        }

        Config jdbcConfig = TypesafeConfigUtils.extractSubConfigThrowable(config, "jdbc.", false);

        if (!jdbcConfig.isEmpty()) {
            jdbcConfig.entrySet().forEach(x -> {
                properties.put(x.getKey(),String.valueOf(x.getValue()));
            });
        }

        DataFrameReader reader = new DataFrameReader(env.sparkSession());
        return reader.jdbc(config.getString("url"), config.getString("table"),properties);
    }
}
