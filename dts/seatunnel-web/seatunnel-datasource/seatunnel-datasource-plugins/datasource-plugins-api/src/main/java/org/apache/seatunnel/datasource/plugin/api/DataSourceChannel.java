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

package org.apache.seatunnel.datasource.plugin.api;

import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.datasource.plugin.api.model.TableField;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableList;
import lombok.NonNull;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

public interface DataSourceChannel {

    List<String> DEFAULT_DATABASES = ImmutableList.of("default");

    /**
     * get datasource metadata fields by datasource name
     *
     * @param pluginName plugin name
     * @return datasource metadata fields
     */
    OptionRule getDataSourceOptions(@NonNull String pluginName);

    /**
     * get datasource metadata fields by datasource name
     *
     * @param pluginName plugin name
     * @return datasource metadata fields
     */
    OptionRule getDatasourceMetadataFieldsByDataSourceName(@NonNull String pluginName);

    List<String> getTables(
            @NonNull String pluginName,
            Map<String, String> requestParams,
            String database,
            Map<String, String> options);

    List<String> getDatabases(
            @NonNull String pluginName, @NonNull Map<String, String> requestParams);

    boolean checkDataSourceConnectivity(
            @NonNull String pluginName, @NonNull Map<String, String> requestParams);

    default boolean canAbleGetSchema() {
        return false;
    }

    List<TableField> getTableFields(
            @NonNull String pluginName,
            @NonNull Map<String, String> requestParams,
            @NonNull String database,
            @NonNull String table);

    Map<String, List<TableField>> getTableFields(
            @NonNull String pluginName,
            @NonNull Map<String, String> requestParams,
            @NonNull String database,
            @NonNull List<String> tables);

    /**
     * just check metadata field is right and used by virtual table
     *
     * @param requestParams request param(connector params)
     * @return true if right
     */
    default Boolean checkMetadataFieldIsRight(Map<String, String> requestParams) {
        return true;
    }

    default Pair<String, String> getTableSyncMaxValue(
            String pluginName,
            Map<String, String> requestParams,
            String databaseName,
            String tableName,
            String updateFieldType) {
        return null;
    }

    default Connection getConnection(String pluginName, Map<String, String> requestParams) {
        return null;
    }
}
