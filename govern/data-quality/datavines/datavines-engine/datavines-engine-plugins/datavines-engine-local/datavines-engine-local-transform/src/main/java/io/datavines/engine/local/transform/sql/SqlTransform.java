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
package io.datavines.engine.local.transform.sql;

import io.datavines.common.config.CheckResult;
import io.datavines.common.config.Config;
import io.datavines.common.config.enums.TransformType;
import io.datavines.engine.api.env.RuntimeEnvironment;
import io.datavines.engine.local.api.LocalRuntimeEnvironment;
import io.datavines.engine.local.api.LocalTransform;
import io.datavines.engine.local.api.entity.ResultList;
import io.datavines.engine.local.api.utils.LoggerFactory;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static io.datavines.engine.api.ConfigConstants.SQL;
import static io.datavines.engine.api.EngineConstants.PLUGIN_TYPE;

public class SqlTransform implements LocalTransform {

    private Logger logger = LoggerFactory.getLogger(SqlTransform.class);

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
        List<String> requiredOptions = Arrays.asList(SQL, PLUGIN_TYPE);

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
    public ResultList process(LocalRuntimeEnvironment env) {

        ResultList resultList = null;
        try {
            String sql = config.getString(SQL);
            String pluginType = config.getString(PLUGIN_TYPE);
            logger.info("transform sql is: {}, transform_type is : {}", sql, pluginType);
            switch (TransformType.of(pluginType)){
                case INVALIDATE_ITEMS :
                    resultList = new InvalidateItemsExecutor().execute(env.getSourceConnection().getConnection(), config);
                    break;
                case ACTUAL_VALUE :
                    resultList = new ActualValueExecutor().execute(env.getSourceConnection().getConnection(), config);
                    break;
                case EXPECTED_VALUE_FROM_METADATA_SOURCE :
                    resultList = new ExpectedValueExecutor().execute(env.getMetadataConnection().getConnection(), config);
                    break;
                case EXPECTED_VALUE_FROM_SOURCE :
                    resultList = new ExpectedValueExecutor().execute(env.getSourceConnection().getConnection(), config);
                    break;
                default:
                    break;
            }

        } catch (Exception e) {
            logger.error("transform execute error: ", e);
        }

        return resultList;
    }
}
