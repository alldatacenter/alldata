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
import io.datavines.connector.api.ConnectorFactory;
import io.datavines.connector.plugin.entity.JdbcOptions;
import io.datavines.connector.plugin.utils.JdbcUtils;
import io.datavines.engine.api.env.RuntimeEnvironment;
import io.datavines.engine.local.api.LocalRuntimeEnvironment;
import io.datavines.engine.local.api.LocalSource;
import io.datavines.engine.local.api.entity.ConnectionItem;
import io.datavines.engine.local.api.utils.LoggerFactory;
import io.datavines.spi.PluginLoader;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static io.datavines.common.ConfigConstants.*;

public class BaseJdbcSource implements LocalSource {

    private final Logger log = LoggerFactory.getLogger(BaseJdbcSource.class);

    private Config config = new Config();

    private ConnectionItem connectionItem;

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
        List<String> requiredOptions = Arrays.asList("url", "user");

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
    public ConnectionItem getConnectionItem(LocalRuntimeEnvironment env) {
        connectionItem = new ConnectionItem(config);
        return connectionItem;
    }

    @Override
    public boolean checkTableExist() {

        if (connectionItem != null) {
            ConnectorFactory connectorFactory = PluginLoader.getPluginLoader(ConnectorFactory.class)
                    .getOrCreatePlugin(config.getString(SRC_CONNECTOR_TYPE));
            JdbcOptions jdbcOptions = new JdbcOptions();
            jdbcOptions.setDatabaseName(config.getString(DATABASE));
            jdbcOptions.setTableName(config.getString(TABLE));
            jdbcOptions.setQueryTimeout(10000);
            try {
                 return JdbcUtils.tableExists(connectionItem.getConnection(),jdbcOptions, connectorFactory.getDialect());
            } catch (Exception e) {
                log.error("check table {} exists error :", config.getString(TABLE), e);
                return false;
            }
        }

        return false;
    }
}
