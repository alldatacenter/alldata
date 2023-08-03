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
package io.datavines.engine.local.connector;

import io.datavines.common.config.CheckResult;
import io.datavines.common.config.Config;
import io.datavines.common.config.enums.SinkType;
import io.datavines.engine.api.env.RuntimeEnvironment;
import io.datavines.engine.local.api.LocalRuntimeEnvironment;
import io.datavines.engine.local.api.LocalSink;
import io.datavines.engine.local.api.entity.ResultList;
import io.datavines.engine.local.api.utils.LoggerFactory;
import io.datavines.engine.local.connector.executor.*;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static io.datavines.engine.api.EngineConstants.PLUGIN_TYPE;

public abstract class BaseJdbcSink implements LocalSink {

    private Logger log = LoggerFactory.getLogger(BaseJdbcSink.class);

    public static final String SINK_TABLE_NAME = "sink_table_name";
    public static final String CREATE_TABLE_SQL = "create_table_sql";

    protected Config config = new Config();

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
        List<String> requiredOptions = Arrays.asList("url", "user", "password");

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
    public void prepare(RuntimeEnvironment env) {

    }

    @Override
    public void output(List<ResultList> resultList, LocalRuntimeEnvironment env) {

        Map<String,String> inputParameter = new HashMap<>();
        setExceptedValue(config, resultList, inputParameter);
        ISinkExecutor sinkExecutor = null;

        switch (SinkType.of(config.getString(PLUGIN_TYPE))){
            case ERROR_DATA:
                sinkExecutor = new ErrorDataSinkExecutor(config, env);
                sinkExecutor.execute(inputParameter);
                break;
            case VALIDATE_RESULT:
                inputParameter.put(SINK_TABLE_NAME, "dv_job_execution_result");
                inputParameter.put(CREATE_TABLE_SQL, getExecutionResultTableSql());
                sinkExecutor = new ValidateResultDataSinkExecutor(config, env);
                sinkExecutor.execute(inputParameter);
                break;
            case ACTUAL_VALUE:
                inputParameter.put(SINK_TABLE_NAME, "dv_actual_values");
                inputParameter.put(CREATE_TABLE_SQL, getActualValueTableSql());
                sinkExecutor = new ActualValueDataSinkExecutor(config, env);
                sinkExecutor.execute(inputParameter);
                break;
            case PROFILE_VALUE:
                inputParameter.put(SINK_TABLE_NAME, "dv_catalog_entity_profile");
                inputParameter.put(CREATE_TABLE_SQL, getProfileValueTableSql());
                sinkExecutor = new ProfileDataSinkExecutor(config, env);
                sinkExecutor.execute(inputParameter);
                break;
            default:
                break;
        }
    }

    protected abstract String getExecutionResultTableSql();

    protected abstract String getActualValueTableSql();

    protected abstract String getProfileValueTableSql();

}
