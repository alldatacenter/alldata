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

package org.apache.seatunnel.app.thirdparty.datasource.impl;

import org.apache.seatunnel.shade.com.typesafe.config.Config;
import org.apache.seatunnel.shade.com.typesafe.config.ConfigValueFactory;

import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.app.domain.request.connector.BusinessMode;
import org.apache.seatunnel.app.domain.request.job.DataSourceOption;
import org.apache.seatunnel.app.domain.request.job.SelectTableFields;
import org.apache.seatunnel.app.domain.response.datasource.VirtualTableDetailRes;
import org.apache.seatunnel.app.dynamicforms.FormStructure;
import org.apache.seatunnel.app.thirdparty.datasource.AbstractDataSourceConfigSwitcher;
import org.apache.seatunnel.app.utils.JdbcUtils;
import org.apache.seatunnel.common.constants.PluginType;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;

public class ClickhouseDataSourceConfigSwitcher extends AbstractDataSourceConfigSwitcher {
    private static final ClickhouseDataSourceConfigSwitcher INSTANCE =
            new ClickhouseDataSourceConfigSwitcher();

    private static final String HOST = "host";
    private static final String URL = "url";
    private static final String SQL = "sql";
    private static final String DATABASE = "database";
    private static final String TABLE = "table";

    private static final Map<PluginType, List<String>> FILTER_FIELD_MAP =
            new ImmutableMap.Builder<PluginType, List<String>>()
                    .put(PluginType.SOURCE, Lists.newArrayList(SQL, HOST))
                    .put(PluginType.SINK, Lists.newArrayList(HOST, DATABASE, TABLE))
                    .build();

    @Override
    public FormStructure filterOptionRule(
            String connectorName,
            OptionRule dataSourceOptionRule,
            OptionRule virtualTableOptionRule,
            BusinessMode businessMode,
            PluginType pluginType,
            OptionRule connectorOptionRule,
            List<String> excludedKeys) {
        return super.filterOptionRule(
                connectorName,
                dataSourceOptionRule,
                virtualTableOptionRule,
                businessMode,
                pluginType,
                connectorOptionRule,
                FILTER_FIELD_MAP.get(pluginType));
    }

    @Override
    public Config mergeDatasourceConfig(
            Config dataSourceInstanceConfig,
            VirtualTableDetailRes virtualTableDetail,
            DataSourceOption dataSourceOption,
            SelectTableFields selectTableFields,
            BusinessMode businessMode,
            PluginType pluginType,
            Config connectorConfig) {
        switch (businessMode) {
            case DATA_REPLICA:
                // We only support sink in data replica mode
                if (pluginType.equals(PluginType.SINK)) {
                    connectorConfig =
                            connectorConfig.withValue(
                                    DATABASE,
                                    ConfigValueFactory.fromAnyRef(
                                            dataSourceOption.getDatabases().get(0)));
                } else {
                    throw new UnsupportedOperationException(
                            "Clickhouse DATA_REPLICA Unsupported plugin type: " + pluginType);
                }
                break;
            case DATA_INTEGRATION:
                // generate the sql by the schema
                if (pluginType.equals(PluginType.SOURCE)) {
                    List<String> tableFields = selectTableFields.getTableFields();
                    String sql =
                            String.format(
                                    "SELECT %s FROM %s",
                                    String.join(",", tableFields),
                                    String.format(
                                            "`%s`.`%s`",
                                            dataSourceOption.getDatabases().get(0),
                                            dataSourceOption.getTables().get(0)));
                    connectorConfig =
                            connectorConfig.withValue(SQL, ConfigValueFactory.fromAnyRef(sql));
                } else if (pluginType.equals(PluginType.SINK)) {
                    connectorConfig =
                            connectorConfig.withValue(
                                    DATABASE,
                                    ConfigValueFactory.fromAnyRef(
                                            dataSourceOption.getDatabases().get(0)));
                    connectorConfig =
                            connectorConfig.withValue(
                                    TABLE,
                                    ConfigValueFactory.fromAnyRef(
                                            dataSourceOption.getTables().get(0)));
                } else {
                    throw new UnsupportedOperationException(
                            "Unsupported plugin type: " + pluginType);
                }
                break;
            default:
                break;
        }
        connectorConfig =
                connectorConfig.withValue(
                        HOST,
                        ConfigValueFactory.fromAnyRef(
                                JdbcUtils.getAddressFromUrl(
                                        dataSourceInstanceConfig.getString(URL))));
        return super.mergeDatasourceConfig(
                dataSourceInstanceConfig,
                virtualTableDetail,
                dataSourceOption,
                selectTableFields,
                businessMode,
                pluginType,
                connectorConfig);
    }

    public static ClickhouseDataSourceConfigSwitcher getInstance() {
        return INSTANCE;
    }

    private ClickhouseDataSourceConfigSwitcher() {}
}
