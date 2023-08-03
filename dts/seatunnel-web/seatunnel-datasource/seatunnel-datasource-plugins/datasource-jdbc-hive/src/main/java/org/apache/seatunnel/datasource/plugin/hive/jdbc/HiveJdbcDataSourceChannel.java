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

package org.apache.seatunnel.datasource.plugin.hive.jdbc;

import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.datasource.plugin.api.DataSourceChannel;
import org.apache.seatunnel.datasource.plugin.api.DataSourcePluginException;
import org.apache.seatunnel.datasource.plugin.api.model.TableField;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class HiveJdbcDataSourceChannel implements DataSourceChannel {

    @Override
    public OptionRule getDataSourceOptions(@NonNull String pluginName) {
        return HiveJdbcOptionRule.optionRule();
    }

    @Override
    public OptionRule getDatasourceMetadataFieldsByDataSourceName(@NonNull String pluginName) {
        return HiveJdbcOptionRule.metadataRule();
    }

    @Override
    public List<String> getTables(
            @NonNull String pluginName,
            Map<String, String> requestParams,
            String database,
            Map<String, String> option) {
        return getTables(pluginName, requestParams, database, option);
    }

    @Override
    public List<String> getDatabases(
            @NonNull String pluginName, @NonNull Map<String, String> requestParams) {
        try {
            return getDataBaseNames(pluginName, requestParams);
        } catch (SQLException e) {
            log.error("Query Hive databases error, request params is {}", requestParams, e);
            throw new DataSourcePluginException("Query Hive databases error,", e);
        }
    }

    @Override
    public boolean checkDataSourceConnectivity(
            @NonNull String pluginName, @NonNull Map<String, String> requestParams) {
        return checkJdbcConnectivity(requestParams);
    }

    @Override
    public List<TableField> getTableFields(
            @NonNull String pluginName,
            @NonNull Map<String, String> requestParams,
            @NonNull String database,
            @NonNull String table) {
        return getTableFields(requestParams, database, table);
    }

    @Override
    public Map<String, List<TableField>> getTableFields(
            @NonNull String pluginName,
            @NonNull Map<String, String> requestParams,
            @NonNull String database,
            @NonNull List<String> tables) {
        Map<String, List<TableField>> tableFields = new HashMap<>(tables.size());
        for (String table : tables) {
            tableFields.put(table, getTableFields(requestParams, database, table));
        }
        return tableFields;
    }

    protected boolean checkJdbcConnectivity(Map<String, String> requestParams) {
        try (Connection ignored = init(requestParams)) {
            return true;
        } catch (Exception e) {
            throw new DataSourcePluginException(
                    "check jdbc connectivity failed, " + e.getMessage(), e);
        }
    }

    protected Connection init(Map<String, String> requestParams) throws SQLException {
        if (MapUtils.isEmpty(requestParams)) {
            throw new DataSourcePluginException(
                    "Hive jdbc request params is null, please check your config");
        }
        String url = requestParams.get(HiveJdbcOptionRule.URL.key());
        return DriverManager.getConnection(url);
    }

    protected List<String> getDataBaseNames(String pluginName, Map<String, String> requestParams)
            throws SQLException {
        List<String> dbNames = new ArrayList<>();
        try (Connection connection = init(requestParams);
                Statement statement = connection.createStatement(); ) {
            ResultSet re = statement.executeQuery("SHOW DATABASES;");
            // filter system databases
            while (re.next()) {
                String dbName = re.getString("database");
                if (StringUtils.isNotBlank(dbName) && isNotSystemDatabase(pluginName, dbName)) {
                    dbNames.add(dbName);
                }
            }
            return dbNames;
        }
    }

    protected List<String> getTableNames(Map<String, String> requestParams, String dbName) {
        List<String> tableNames = new ArrayList<>();
        try (Connection connection = init(requestParams); ) {
            ResultSet resultSet =
                    connection.getMetaData().getTables(dbName, null, null, new String[] {"TABLE"});
            while (resultSet.next()) {
                String tableName = resultSet.getString("TABLE_NAME");
                if (StringUtils.isNotBlank(tableName)) {
                    tableNames.add(tableName);
                }
            }
            return tableNames;
        } catch (SQLException e) {
            throw new DataSourcePluginException("get table names failed", e);
        }
    }

    protected List<TableField> getTableFields(
            Map<String, String> requestParams, String dbName, String tableName) {
        List<TableField> tableFields = new ArrayList<>();
        try (Connection connection = init(requestParams); ) {
            DatabaseMetaData metaData = connection.getMetaData();
            String primaryKey = getPrimaryKey(metaData, dbName, tableName);
            ResultSet resultSet = metaData.getColumns(dbName, null, tableName, null);
            while (resultSet.next()) {
                TableField tableField = new TableField();
                String columnName = resultSet.getString("COLUMN_NAME");
                tableField.setPrimaryKey(false);
                if (StringUtils.isNotBlank(primaryKey) && primaryKey.equals(columnName)) {
                    tableField.setPrimaryKey(true);
                }
                tableField.setName(columnName);
                tableField.setType(resultSet.getString("TYPE_NAME"));
                tableField.setComment(resultSet.getString("REMARKS"));
                Object nullable = resultSet.getObject("IS_NULLABLE");
                boolean isNullable = convertToBoolean(nullable);
                tableField.setNullable(isNullable);
                tableFields.add(tableField);
            }
        } catch (SQLException e) {
            throw new DataSourcePluginException("get table fields failed", e);
        }
        return tableFields;
    }

    private String getPrimaryKey(DatabaseMetaData metaData, String dbName, String tableName)
            throws SQLException {
        ResultSet primaryKeysInfo = metaData.getPrimaryKeys(dbName, "%", tableName);
        while (primaryKeysInfo.next()) {
            return primaryKeysInfo.getString("COLUMN_NAME");
        }
        return null;
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    private static boolean checkHostConnectable(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean isNotSystemDatabase(String pluginName, String dbName) {
        // FIXME,filters system databases
        return true;
    }

    private boolean convertToBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return value.equals("TRUE");
        }
        return false;
    }
}
