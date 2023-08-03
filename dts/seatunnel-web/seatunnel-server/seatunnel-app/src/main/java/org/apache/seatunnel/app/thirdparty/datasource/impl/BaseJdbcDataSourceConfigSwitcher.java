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
import org.apache.seatunnel.common.constants.PluginType;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.seatunnel.app.domain.request.connector.BusinessMode.DATA_INTEGRATION;
import static org.apache.seatunnel.app.domain.request.connector.BusinessMode.DATA_REPLICA;

public abstract class BaseJdbcDataSourceConfigSwitcher extends AbstractDataSourceConfigSwitcher {
    private static final String TABLE_KEY = "table";
    private static final String DATABASE_KEY = "database";

    private static final String QUERY_KEY = "query";

    private static final String GENERATE_SINK_SQL = "generate_sink_sql";

    private static final String URL_KEY = "url";

    @Override
    public FormStructure filterOptionRule(
            String connectorName,
            OptionRule dataSourceOptionRule,
            OptionRule virtualTableOptionRule,
            BusinessMode businessMode,
            PluginType pluginType,
            OptionRule connectorOptionRule,
            List<String> excludedKeys) {
        Map<PluginType, List<String>> filterFieldMap = new HashMap<>();

        filterFieldMap.put(
                PluginType.SINK,
                Arrays.asList(QUERY_KEY, TABLE_KEY, DATABASE_KEY, GENERATE_SINK_SQL));
        filterFieldMap.put(PluginType.SOURCE, Collections.singletonList(QUERY_KEY));

        return super.filterOptionRule(
                connectorName,
                dataSourceOptionRule,
                virtualTableOptionRule,
                businessMode,
                pluginType,
                connectorOptionRule,
                filterFieldMap.get(pluginType));
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

        // 替换url中的database
        if (dataSourceOption.getDatabases().size() == 1) {
            String databaseName = dataSourceOption.getDatabases().get(0);
            String url = dataSourceInstanceConfig.getString(URL_KEY);
            String newUrl = replaceDatabaseNameInUrl(url, databaseName);
            dataSourceInstanceConfig =
                    dataSourceInstanceConfig.withValue(
                            URL_KEY, ConfigValueFactory.fromAnyRef(newUrl));
        }
        if (pluginType.equals(PluginType.SINK)) {
            connectorConfig =
                    connectorConfig.withValue(
                            GENERATE_SINK_SQL, ConfigValueFactory.fromAnyRef(true));
        }
        if (businessMode.equals(DATA_INTEGRATION)) {

            String databaseName = dataSourceOption.getDatabases().get(0);

            String tableName = dataSourceOption.getTables().get(0);

            // 将schema转换成sql
            if (pluginType.equals(PluginType.SOURCE)) {

                List<String> tableFields = selectTableFields.getTableFields();

                String sql = tableFieldsToSql(tableFields, databaseName, tableName);

                connectorConfig =
                        connectorConfig.withValue(QUERY_KEY, ConfigValueFactory.fromAnyRef(sql));
            } else if (pluginType.equals(PluginType.SINK)) {
                connectorConfig =
                        connectorConfig.withValue(
                                DATABASE_KEY, ConfigValueFactory.fromAnyRef(databaseName));
                connectorConfig =
                        connectorConfig.withValue(
                                TABLE_KEY, ConfigValueFactory.fromAnyRef(tableName));
            } else {
                throw new UnsupportedOperationException("Unsupported plugin type: " + pluginType);
            }

            return super.mergeDatasourceConfig(
                    dataSourceInstanceConfig,
                    virtualTableDetail,
                    dataSourceOption,
                    selectTableFields,
                    businessMode,
                    pluginType,
                    connectorConfig);
        } else if (businessMode.equals(DATA_REPLICA)) {
            String databaseName = dataSourceOption.getDatabases().get(0);
            if (pluginType.equals(PluginType.SINK)) {
                connectorConfig =
                        connectorConfig.withValue(
                                DATABASE_KEY, ConfigValueFactory.fromAnyRef(databaseName));
                return super.mergeDatasourceConfig(
                        dataSourceInstanceConfig,
                        virtualTableDetail,
                        dataSourceOption,
                        selectTableFields,
                        businessMode,
                        pluginType,
                        connectorConfig);
            } else {
                throw new UnsupportedOperationException(
                        "JDBC DATA_REPLICA Unsupported plugin type: " + pluginType);
            }

        } else {
            throw new UnsupportedOperationException("Unsupported businessMode : " + businessMode);
        }
    }

    protected String generateSql(
            List<String> tableFields, String database, String schema, String table) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ");
        for (int i = 0; i < tableFields.size(); i++) {
            sb.append(quoteIdentifier(tableFields.get(i)));
            if (i < tableFields.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(" FROM ").append(quoteIdentifier(database));
        if (schema != null && !schema.isEmpty()) {
            sb.append(".").append(quoteIdentifier(schema));
        }
        sb.append(".").append(quoteIdentifier(table));

        return sb.toString();
    }

    protected String tableFieldsToSql(List<String> tableFields, String database, String table) {
        return generateSql(tableFields, database, null, table);
    }

    protected String quoteIdentifier(String identifier) {
        return "`" + identifier + "`";
    }

    protected String replaceDatabaseNameInUrl(String url, String databaseName) {
        return url;
    }
}
