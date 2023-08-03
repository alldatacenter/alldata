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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class KafkaKingbaseDataSourceConfigSwitcher extends AbstractDataSourceConfigSwitcher {

    private static final KafkaKingbaseDataSourceConfigSwitcher INSTANCE =
            new KafkaKingbaseDataSourceConfigSwitcher();

    private static final String SCHEMA = "schema";
    private static final String TOPIC = "topic";
    private static final String FORMAT = "format";
    private static final String PATTERN = "pattern";

    private static final String FACTORY = "factory";

    private static final String CATALOG = "catalog";

    private static final String TABLE_NAMES = "table-names";

    private static final String URL = "url";

    private static final String USER = "user";

    private static final String PASSWORD = "password";

    private static final String DATABASE_NAMES = "database-names";

    @Override
    public FormStructure filterOptionRule(
            String connectorName,
            OptionRule dataSourceOptionRule,
            OptionRule virtualTableOptionRule,
            BusinessMode businessMode,
            PluginType pluginType,
            OptionRule connectorOptionRule,
            List<String> excludedKeys) {
        if (pluginType == PluginType.SOURCE) {
            excludedKeys.add(SCHEMA);
            excludedKeys.add(TOPIC);
            excludedKeys.add(PATTERN);
            excludedKeys.add(FORMAT);
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
        if (pluginType == PluginType.SOURCE) {
            // Use field to generate the schema
            String topics = String.join(",", dataSourceOption.getDatabases());
            connectorConfig =
                    connectorConfig.withValue(TOPIC, ConfigValueFactory.fromAnyRef(topics));

            connectorConfig =
                    connectorConfig.withValue(
                            FORMAT, ConfigValueFactory.fromAnyRef("KINGBASE_JSON"));
            connectorConfig =
                    connectorConfig.withValue(
                            DATABASE_NAMES,
                            ConfigValueFactory.fromIterable(dataSourceOption.getDatabases()));
            connectorConfig =
                    connectorConfig.withValue(
                            TABLE_NAMES,
                            ConfigValueFactory.fromIterable(
                                    mergeDatabaseAndTables(dataSourceOption)));
            Config config = ConfigFactory.empty();
            config = config.withValue(FACTORY, ConfigValueFactory.fromAnyRef("kingbase"));
            config = config.withValue(URL, dataSourceInstanceConfig.getValue(URL));
            config = config.withValue(USER, dataSourceInstanceConfig.getValue(USER));
            config = config.withValue(PASSWORD, dataSourceInstanceConfig.getValue(PASSWORD));
            connectorConfig = connectorConfig.withValue(CATALOG, config.root());
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

    private static List<String> mergeDatabaseAndTables(DataSourceOption dataSourceOption) {
        List<String> tables = new ArrayList<>();
        dataSourceOption
                .getDatabases()
                .forEach(
                        database -> {
                            dataSourceOption
                                    .getTables()
                                    .forEach(
                                            table -> {
                                                if (StringUtils.countMatches(table, ".") > 1) {
                                                    tables.add(table);
                                                } else {
                                                    tables.add(
                                                            getDatabaseAndTable(database, table));
                                                }
                                            });
                        });
        return tables;
    }

    private static String getDatabaseAndTable(String database, String table) {
        return String.format("%s.%s", database, table);
    }

    private KafkaKingbaseDataSourceConfigSwitcher() {}

    public static KafkaKingbaseDataSourceConfigSwitcher getInstance() {
        return INSTANCE;
    }

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

    @Slf4j
    public static class SchemaGenerator {

        private SchemaGenerator() {}

        /**
         * Generate the schema of the table.
         *
         * <pre>
         * fields {
         *        name = "string"
         *        age = "int"
         *       }
         * </pre>
         *
         * @param virtualTableDetailRes virtual table detail.
         * @param selectTableFields select table fields which need to be placed in the schema.
         * @return schema.
         */
        public static Config generateSchemaBySelectTableFields(
                VirtualTableDetailRes virtualTableDetailRes, SelectTableFields selectTableFields) {
            checkNotNull(selectTableFields, "selectTableFields cannot be null");
            checkArgument(
                    CollectionUtils.isNotEmpty(selectTableFields.getTableFields()),
                    "selectTableFields.tableFields cannot be empty");

            checkNotNull(virtualTableDetailRes, "virtualTableDetailRes cannot be null");
            checkArgument(
                    CollectionUtils.isNotEmpty(virtualTableDetailRes.getFields()),
                    "virtualTableDetailRes.fields cannot be empty");

            Map<String, VirtualTableFieldRes> fieldTypeMap =
                    virtualTableDetailRes.getFields().stream()
                            .collect(
                                    Collectors.toMap(
                                            VirtualTableFieldRes::getFieldName,
                                            Function.identity()));

            Config schema = ConfigFactory.empty();
            for (String fieldName : selectTableFields.getTableFields()) {
                VirtualTableFieldRes virtualTableFieldRes =
                        checkNotNull(
                                fieldTypeMap.get(fieldName),
                                String.format(
                                        "Cannot find the field: %s from virtual table", fieldName));
                schema =
                        schema.withValue(
                                fieldName,
                                ConfigValueFactory.fromAnyRef(virtualTableFieldRes.getFieldType()));
            }
            return schema.atKey("fields");
        }
    }
}
