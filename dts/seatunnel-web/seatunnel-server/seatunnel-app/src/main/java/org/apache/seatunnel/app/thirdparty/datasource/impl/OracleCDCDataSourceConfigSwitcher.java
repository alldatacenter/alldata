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
import org.apache.seatunnel.shade.com.typesafe.config.ConfigFactory;
import org.apache.seatunnel.shade.com.typesafe.config.ConfigValueFactory;

import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.app.domain.request.connector.BusinessMode;
import org.apache.seatunnel.app.domain.request.job.DataSourceOption;
import org.apache.seatunnel.app.domain.request.job.SelectTableFields;
import org.apache.seatunnel.app.domain.response.datasource.VirtualTableDetailRes;
import org.apache.seatunnel.app.domain.response.datasource.VirtualTableFieldRes;
import org.apache.seatunnel.app.dynamicforms.FormStructure;
import org.apache.seatunnel.app.thirdparty.datasource.AbstractDataSourceConfigSwitcher;
import org.apache.seatunnel.common.constants.PluginType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.apache.seatunnel.app.domain.request.connector.BusinessMode.DATA_INTEGRATION;
import static org.apache.seatunnel.app.domain.request.connector.BusinessMode.DATA_REPLICA;

public class OracleCDCDataSourceConfigSwitcher extends AbstractDataSourceConfigSwitcher {

    private OracleCDCDataSourceConfigSwitcher() {}

    public static final OracleCDCDataSourceConfigSwitcher INSTANCE =
            new OracleCDCDataSourceConfigSwitcher();

    private static final String FACTORY = "factory";

    private static final String CATALOG = "catalog";

    private static final String TABLE_NAMES = "table-names";

    private static final String DATABASE_NAMES = "database-names";

    private static final String FORMAT_KEY = "format";

    private static final String DEBEZIUM_FORMAT = "COMPATIBLE_DEBEZIUM_JSON";

    private static final String DEFAULT_FORMAT = "DEFAULT";

    private static final String SCHEMA = "schema";

    @Override
    public FormStructure filterOptionRule(
            String connectorName,
            OptionRule dataSourceOptionRule,
            OptionRule virtualTableOptionRule,
            BusinessMode businessMode,
            PluginType pluginType,
            OptionRule connectorOptionRule,
            List<String> excludedKeys) {
        if (PluginType.SOURCE.equals(pluginType)) {
            excludedKeys.add(DATABASE_NAMES);
            excludedKeys.add(TABLE_NAMES);
            if (businessMode.equals(DATA_INTEGRATION)) {
                excludedKeys.add(FORMAT_KEY);
            }
        } else {
            throw new UnsupportedOperationException("Unsupported plugin type: " + pluginType);
        }
        return super.filterOptionRule(
                connectorName,
                dataSourceOptionRule,
                virtualTableOptionRule,
                businessMode,
                pluginType,
                connectorOptionRule,
                excludedKeys);
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
        if (PluginType.SOURCE.equals(pluginType)) {
            // Add table-names
            Config config = ConfigFactory.empty();
            config = config.withValue(FACTORY, ConfigValueFactory.fromAnyRef("Oracle"));
            connectorConfig = connectorConfig.withValue(CATALOG, config.root());
            connectorConfig =
                    connectorConfig.withValue(
                            DATABASE_NAMES,
                            ConfigValueFactory.fromIterable(dataSourceOption.getDatabases()));
            connectorConfig =
                    connectorConfig.withValue(
                            TABLE_NAMES,
                            ConfigValueFactory.fromIterable(
                                    mergeDatabaseAndTables(dataSourceOption)));

            if (businessMode.equals(DATA_INTEGRATION)) {
                connectorConfig =
                        connectorConfig.withValue(
                                FORMAT_KEY, ConfigValueFactory.fromAnyRef(DEFAULT_FORMAT));
            } else if (businessMode.equals(DATA_REPLICA)
                    && connectorConfig
                            .getString(FORMAT_KEY)
                            .toUpperCase(Locale.ROOT)
                            .equals(DEBEZIUM_FORMAT)) {
                connectorConfig =
                        connectorConfig.withValue(SCHEMA, generateDebeziumFormatSchema().root());
            }
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
    }

    private Config generateDebeziumFormatSchema() {
        List<VirtualTableFieldRes> fieldResList = new ArrayList<>();
        fieldResList.add(new VirtualTableFieldRes("topic", "string", false, null, false, "", ""));
        fieldResList.add(new VirtualTableFieldRes("key", "string", false, null, false, "", ""));
        fieldResList.add(new VirtualTableFieldRes("value", "string", false, null, false, "", ""));

        Config schema = ConfigFactory.empty();
        for (VirtualTableFieldRes virtualTableFieldRes : fieldResList) {
            schema =
                    schema.withValue(
                            virtualTableFieldRes.getFieldName(),
                            ConfigValueFactory.fromAnyRef(virtualTableFieldRes.getFieldType()));
        }
        return schema.atKey("fields");
    }

    private List<String> mergeDatabaseAndTables(DataSourceOption dataSourceOption) {
        List<String> tables = new ArrayList<>();
        dataSourceOption
                .getDatabases()
                .forEach(
                        database -> {
                            dataSourceOption
                                    .getTables()
                                    .forEach(
                                            table -> {
                                                final String[] tableFragments = table.split("\\.");
                                                if (tableFragments.length == 3) {
                                                    tables.add(table);
                                                } else if (tableFragments.length == 2) {
                                                    tables.add(
                                                            getDatabaseAndTable(database, table));
                                                } else {
                                                    throw new IllegalArgumentException(
                                                            "Illegal sqlserver table-name: "
                                                                    + table);
                                                }
                                            });
                        });
        return tables;
    }

    private String getDatabaseAndTable(String database, String table) {
        return String.format("%s.%s", database, table);
    }
}
