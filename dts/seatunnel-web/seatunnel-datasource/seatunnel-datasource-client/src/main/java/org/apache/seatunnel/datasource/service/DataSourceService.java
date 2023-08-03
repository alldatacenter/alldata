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

package org.apache.seatunnel.datasource.service;

import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.datasource.plugin.api.DataSourcePluginInfo;
import org.apache.seatunnel.datasource.plugin.api.model.TableField;

import org.apache.commons.lang3.tuple.Pair;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

public interface DataSourceService {

    /**
     * get all data source plugins
     *
     * @return data source plugins info
     */
    List<DataSourcePluginInfo> listAllDataSources();

    /**
     * get data source plugin fields
     *
     * @param pluginName data source name
     * @return data source plugin fields
     */
    OptionRule queryDataSourceFieldByName(String pluginName);

    /**
     * get data source metadata fields
     *
     * @param pluginName data source name
     * @return data source metadata fields
     */
    OptionRule queryMetadataFieldByName(String pluginName);

    /**
     * check data source params is valid and connectable
     *
     * @param parameters data source params eg mysql plugin key: url // jdbc url key: username key:
     *     password other key...
     * @return true if valid, false if invalid
     */
    /**
     * we can use this method to check data source connectivity
     *
     * @param pluginName source params
     * @return check result
     */
    Boolean checkDataSourceConnectivity(String pluginName, Map<String, String> datasourceParams);

    /**
     * get data source table names by database name
     *
     * @param pluginName plugin name
     * @param databaseName database name
     * @param requestParams connection params
     * @return table names
     */
    List<String> getTables(
            String pluginName,
            String databaseName,
            Map<String, String> requestParams,
            Map<String, String> options);
    /**
     * get data source database names
     *
     * @param pluginName plugin name
     * @param requestParams connection params
     * @return database names
     */
    List<String> getDatabases(String pluginName, Map<String, String> requestParams);

    /**
     * get data source table fields
     *
     * @param pluginName plugin name
     * @param requestParams connection params
     * @param databaseName database name
     * @param tableName table name
     * @return table fields
     */
    List<TableField> getTableFields(
            String pluginName,
            Map<String, String> requestParams,
            String databaseName,
            String tableName);

    /**
     * get data source table fields
     *
     * @param pluginName plugin name
     * @param requestParams connection params
     * @param databaseName database name
     * @param tableNames table names
     * @return table fields
     */
    Map<String, List<TableField>> getTableFields(
            String pluginName,
            Map<String, String> requestParams,
            String databaseName,
            List<String> tableNames);

    Pair<String, String> getTableSyncMaxValue(
            String pluginName,
            Map<String, String> requestParams,
            String databaseName,
            String tableName,
            String updateFieldType);

    Connection getConnection(String pluginName, Map<String, String> requestParams);
}
