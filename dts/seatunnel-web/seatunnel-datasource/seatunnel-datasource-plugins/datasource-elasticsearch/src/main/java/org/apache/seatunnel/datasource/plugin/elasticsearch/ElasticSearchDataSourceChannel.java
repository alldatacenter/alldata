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

package org.apache.seatunnel.datasource.plugin.elasticsearch;

import org.apache.seatunnel.shade.com.typesafe.config.ConfigFactory;

import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.datasource.plugin.api.DataSourceChannel;
import org.apache.seatunnel.datasource.plugin.api.DataSourcePluginException;
import org.apache.seatunnel.datasource.plugin.api.model.TableField;
import org.apache.seatunnel.datasource.plugin.elasticsearch.client.EsRestClient;

import org.apache.commons.lang3.StringUtils;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElasticSearchDataSourceChannel implements DataSourceChannel {

    private static final String DATABASE = "default";

    @Override
    public boolean canAbleGetSchema() {
        return true;
    }

    @Override
    public OptionRule getDataSourceOptions(@NonNull String pluginName) {
        return ElasticSearchOptionRule.optionRule();
    }

    @Override
    public OptionRule getDatasourceMetadataFieldsByDataSourceName(@NonNull String pluginName) {
        return ElasticSearchOptionRule.metadataRule();
    }

    @Override
    public List<String> getTables(
            @NonNull String pluginName,
            Map<String, String> requestParams,
            String database,
            Map<String, String> option) {
        databaseCheck(database);
        try (EsRestClient client =
                EsRestClient.createInstance(ConfigFactory.parseMap(requestParams))) {
            return client.listIndex();
        }
    }

    @Override
    public List<String> getDatabases(
            @NonNull String pluginName, @NonNull Map<String, String> requestParams) {
        return DEFAULT_DATABASES;
    }

    @Override
    public boolean checkDataSourceConnectivity(
            @NonNull String pluginName, @NonNull Map<String, String> requestParams) {
        try (EsRestClient client =
                EsRestClient.createInstance(ConfigFactory.parseMap(requestParams))) {
            client.getClusterInfo();
            return true;
        } catch (Throwable e) {
            throw new DataSourcePluginException(
                    "check ElasticSearch connectivity failed, " + e.getMessage(), e);
        }
    }

    @Override
    public List<TableField> getTableFields(
            @NonNull String pluginName,
            @NonNull Map<String, String> requestParams,
            @NonNull String database,
            @NonNull String table) {
        databaseCheck(database);
        try (EsRestClient client =
                EsRestClient.createInstance(ConfigFactory.parseMap(requestParams))) {
            Map<String, String> fieldTypeMapping = client.getFieldTypeMapping(table);
            List<TableField> fields = new ArrayList<>();
            fieldTypeMapping.forEach(
                    (fieldName, fieldType) ->
                            fields.add(convertToTableField(fieldName, fieldType)));
            return fields;
        } catch (Exception ex) {
            throw new DataSourcePluginException("Get table fields failed", ex);
        }
    }

    @Override
    public Map<String, List<TableField>> getTableFields(
            @NonNull String pluginName,
            @NonNull Map<String, String> requestParams,
            @NonNull String database,
            @NonNull List<String> tables) {
        databaseCheck(database);
        Map<String, List<TableField>> tableFields = new HashMap<>();
        tables.forEach(
                table ->
                        tableFields.put(
                                table, getTableFields(pluginName, requestParams, database, table)));
        return tableFields;
    }

    private static void databaseCheck(@NonNull String database) {
        if (!StringUtils.equalsIgnoreCase(database, DATABASE)) {
            throw new IllegalArgumentException("database not found: " + database);
        }
    }

    private TableField convertToTableField(String fieldName, String fieldType) {
        TableField tableField = new TableField();
        tableField.setName(fieldName);
        tableField.setType(fieldType);
        tableField.setComment(null);
        tableField.setNullable(true);
        tableField.setPrimaryKey(fieldName.equals("_id"));
        tableField.setDefaultValue(null);
        return tableField;
    }
}
