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

package org.apache.seatunnel.datasource.plugin.redshift.jdbc;

import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.datasource.plugin.api.DataSourceChannel;
import org.apache.seatunnel.datasource.plugin.api.DataSourcePluginException;
import org.apache.seatunnel.datasource.plugin.api.model.TableField;
import org.apache.seatunnel.datasource.plugin.api.utils.JdbcUtils;

import org.apache.commons.lang3.StringUtils;

import lombok.NonNull;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class RedshiftDataSourceChannel implements DataSourceChannel {

    @Override
    public OptionRule getDataSourceOptions(@NonNull String pluginName) {
        return RedshiftDataSourceConfig.OPTION_RULE;
    }

    @Override
    public OptionRule getDatasourceMetadataFieldsByDataSourceName(@NonNull String pluginName) {
        return RedshiftDataSourceConfig.METADATA_RULE;
    }

    @Override
    public List<String> getTables(
            @NonNull String pluginName,
            Map<String, String> requestParams,
            String database,
            Map<String, String> option) {
        List<String> tableNames = new ArrayList<>();
        try (Connection connection = getConnection(requestParams, database);
                ResultSet resultSet =
                        connection.getMetaData().getTables(database, null, null, null); ) {
            while (resultSet.next()) {
                String schemaName = resultSet.getString("TABLE_SCHEM");
                String tableName = resultSet.getString("TABLE_NAME");
                // todo: use isNotSystemSchemaName
                if (StringUtils.isNotBlank(schemaName)
                        && !RedshiftDataSourceConfig.REDSHIFT_SYSTEM_TABLES.contains(schemaName)) {
                    tableNames.add(schemaName + "." + tableName);
                }
            }
            return tableNames;
        } catch (SQLException | ClassNotFoundException e) {
            throw new DataSourcePluginException("get table names failed", e);
        }
    }

    @Override
    public List<String> getDatabases(
            @NonNull String pluginName, @NonNull Map<String, String> requestParams) {
        List<String> dbNames = new ArrayList<>();
        try (Connection connection = getConnection(requestParams);
                PreparedStatement statement =
                        connection.prepareStatement("select datname from pg_database;");
                ResultSet re = statement.executeQuery()) {
            while (re.next()) {
                String dbName = re.getString("datname");
                if (StringUtils.isNotBlank(dbName)
                        && !RedshiftDataSourceConfig.REDSHIFT_SYSTEM_TABLES.contains(dbName)) {
                    dbNames.add(dbName);
                }
            }
            return dbNames;
        } catch (SQLException | ClassNotFoundException e) {
            throw new DataSourcePluginException("get databases failed", e);
        }
    }

    @Override
    public boolean checkDataSourceConnectivity(
            @NonNull String pluginName, @NonNull Map<String, String> requestParams) {
        try (Connection ignored = getConnection(requestParams)) {
            return true;
        } catch (Exception e) {
            throw new DataSourcePluginException("check jdbc connectivity failed", e);
        }
    }

    @Override
    public List<TableField> getTableFields(
            @NonNull String pluginName,
            @NonNull Map<String, String> requestParams,
            @NonNull String database,
            @NonNull String table) {
        List<TableField> tableFields = new ArrayList<>();
        try (Connection connection = getConnection(requestParams, database); ) {
            DatabaseMetaData metaData = connection.getMetaData();
            String primaryKey = getPrimaryKey(metaData, database, table);
            String[] split = table.split("\\.");
            if (split.length != 2) {
                throw new DataSourcePluginException(
                        "Postgresql tableName should composed by schemaName.tableName");
            }
            try (ResultSet resultSet = metaData.getColumns(database, split[0], split[1], null)) {
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
                    tableField.setNullable(Boolean.TRUE.toString().equals(nullable.toString()));
                    tableFields.add(tableField);
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            throw new DataSourcePluginException("get table fields failed", e);
        }
        return tableFields;
    }

    @Override
    public Map<String, List<TableField>> getTableFields(
            @NonNull String pluginName,
            @NonNull Map<String, String> requestParams,
            @NonNull String database,
            @NonNull List<String> tables) {
        return null;
    }

    private String getPrimaryKey(DatabaseMetaData metaData, String dbName, String tableName)
            throws SQLException {
        ResultSet primaryKeysInfo = metaData.getPrimaryKeys(dbName, "%", tableName);
        while (primaryKeysInfo.next()) {
            return primaryKeysInfo.getString("COLUMN_NAME");
        }
        return null;
    }

    private Connection getConnection(Map<String, String> requestParams)
            throws SQLException, ClassNotFoundException {
        return getConnection(requestParams, null);
    }

    private Connection getConnection(Map<String, String> requestParams, String databaseName)
            throws SQLException, ClassNotFoundException {
        checkNotNull(requestParams.get(RedshiftOptionRule.DRIVER.key()));
        checkNotNull(requestParams.get(RedshiftOptionRule.URL.key()), "Jdbc url cannot be null");
        String url =
                JdbcUtils.replaceDatabase(
                        requestParams.get(RedshiftOptionRule.URL.key()), databaseName);
        if (requestParams.containsKey(RedshiftOptionRule.USER.key())) {
            String username = requestParams.get(RedshiftOptionRule.USER.key());
            String password = requestParams.get(RedshiftOptionRule.PASSWORD.key());
            return DriverManager.getConnection(url, username, password);
        }
        return DriverManager.getConnection(url);
    }
}
