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

package org.apache.seatunnel.datasource.plugin.cdc.sqlserver;

import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.datasource.plugin.api.DataSourceChannel;
import org.apache.seatunnel.datasource.plugin.api.DataSourcePluginException;
import org.apache.seatunnel.datasource.plugin.api.model.TableField;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Sets;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class SqlServerCDCDataSourceChannel implements DataSourceChannel {

    public static final Set<String> MYSQL_SYSTEM_DATABASES =
            Sets.newHashSet("master", "tempdb", "model", "msdb");

    @Override
    public boolean canAbleGetSchema() {
        return true;
    }

    @Override
    public OptionRule getDataSourceOptions(@NonNull String pluginName) {
        return SqlServerCDCOptionRule.optionRule();
    }

    @Override
    public OptionRule getDatasourceMetadataFieldsByDataSourceName(@NonNull String pluginName) {
        return SqlServerCDCOptionRule.metadataRule();
    }

    @Override
    public List<String> getTables(
            String pluginName,
            Map<String, String> requestParams,
            String database,
            Map<String, String> options) {
        return this.getTableNames(requestParams, database);
    }

    @Override
    public List<String> getDatabases(String pluginName, Map<String, String> requestParams) {
        try {
            return this.getDataBaseNames(requestParams);
        } catch (SQLException e) {
            throw new DataSourcePluginException("get databases failed", e);
        }
    }

    @Override
    public boolean checkDataSourceConnectivity(
            String pluginName, Map<String, String> requestParams) {
        try (Connection connection = init(requestParams);
                PreparedStatement statement = connection.prepareStatement("SELECT 1");
                ResultSet rs = statement.executeQuery()) {
            return rs.next();
        } catch (SQLException e) {
            throw new DataSourcePluginException("connect datasource failed", e);
        }
    }

    @Override
    public List<TableField> getTableFields(
            String pluginName, Map<String, String> requestParams, String database, String table) {
        Pair<String, String> pair = parseSchemaAndTable(table);
        return getTableFields(requestParams, database, pair.getLeft(), pair.getRight());
    }

    @Override
    public Map<String, List<TableField>> getTableFields(
            String pluginName,
            Map<String, String> requestParams,
            String database,
            List<String> tables) {
        Map<String, List<TableField>> tableFields = new HashMap<>(tables.size());
        for (String table : tables) {
            tableFields.put(table, getTableFields(pluginName, requestParams, database, table));
        }
        return tableFields;
    }

    private Connection init(Map<String, String> requestParams) throws SQLException {
        if (null == requestParams.get(SqlServerCDCOptionRule.BASE_URL.key())) {
            throw new DataSourcePluginException("Jdbc url is null");
        }
        String url = requestParams.get(SqlServerCDCOptionRule.BASE_URL.key());
        if (null != requestParams.get(SqlServerCDCOptionRule.PASSWORD.key())
                && null != requestParams.get(SqlServerCDCOptionRule.USERNAME.key())) {
            String username = requestParams.get(SqlServerCDCOptionRule.USERNAME.key());
            String password = requestParams.get(SqlServerCDCOptionRule.PASSWORD.key());
            return DriverManager.getConnection(url, username, password);
        }
        return DriverManager.getConnection(url);
    }

    private List<String> getDataBaseNames(Map<String, String> requestParams) throws SQLException {
        List<String> dbNames = new ArrayList<>();
        try (Connection connection = init(requestParams);
                PreparedStatement statement =
                        connection.prepareStatement(
                                "SELECT NAME FROM SYS.DATABASES WHERE IS_CDC_ENABLED = 1;");
                ResultSet re = statement.executeQuery()) {
            // filter system databases
            while (re.next()) {
                String dbName = re.getString("NAME");
                if (StringUtils.isNotBlank(dbName) && isNotSystemDatabase(dbName)) {
                    dbNames.add(dbName);
                }
            }

            return dbNames;
        }
    }

    private List<String> getTableNames(Map<String, String> requestParams, String dbName) {
        final String sql =
                String.format(
                        "SELECT SCHEMAS.NAME AS SCHEMA_NAME, TABLES.NAME AS TABLE_NAME"
                                + "    FROM %s.SYS.SCHEMAS AS SCHEMAS"
                                + "        JOIN %s.SYS.TABLES AS TABLES"
                                + "            ON SCHEMAS.SCHEMA_ID = TABLES.SCHEMA_ID"
                                + "                   AND TABLES.IS_TRACKED_BY_CDC = 1",
                        dbName, dbName);

        List<String> tableNames = new ArrayList<>();
        try (Connection connection = init(requestParams);
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                String schemaName = resultSet.getString("SCHEMA_NAME");
                String tableName = resultSet.getString("TABLE_NAME");
                tableNames.add(schemaName + "." + tableName);
            }
            return tableNames;
        } catch (SQLException e) {
            throw new DataSourcePluginException("get table names failed", e);
        }
    }

    private List<TableField> getTableFields(
            Map<String, String> requestParams, String dbName, String schemaName, String tableName) {
        List<TableField> tableFields = new ArrayList<>();
        try (Connection connection = init(requestParams); ) {
            DatabaseMetaData metaData = connection.getMetaData();
            String primaryKey = getPrimaryKey(metaData, dbName, schemaName, tableName);
            ResultSet resultSet = metaData.getColumns(dbName, schemaName, tableName, null);
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

    private String getPrimaryKey(
            DatabaseMetaData metaData, String dbName, String schemaName, String tableName)
            throws SQLException {
        ResultSet primaryKeysInfo = metaData.getPrimaryKeys(dbName, schemaName, tableName);
        while (primaryKeysInfo.next()) {
            return primaryKeysInfo.getString("COLUMN_NAME");
        }
        return null;
    }

    private boolean isNotSystemDatabase(String dbName) {
        return MYSQL_SYSTEM_DATABASES.stream()
                .noneMatch(systemDatabase -> StringUtils.equalsIgnoreCase(systemDatabase, dbName));
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

    private Pair<String, String> parseSchemaAndTable(String tableName) {
        String[] schemaAndTable = tableName.split("\\.");
        if (schemaAndTable.length != 2) {
            throw new DataSourcePluginException("table name is invalid");
        }
        return Pair.of(schemaAndTable[0], schemaAndTable[1]);
    }
}
