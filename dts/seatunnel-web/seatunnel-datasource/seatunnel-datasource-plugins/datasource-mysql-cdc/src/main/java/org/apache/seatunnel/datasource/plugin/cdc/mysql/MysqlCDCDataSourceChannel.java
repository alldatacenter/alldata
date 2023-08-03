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

package org.apache.seatunnel.datasource.plugin.cdc.mysql;

import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.datasource.plugin.api.DataSourceChannel;
import org.apache.seatunnel.datasource.plugin.api.DataSourcePluginException;
import org.apache.seatunnel.datasource.plugin.api.model.TableField;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Sets;
import lombok.NonNull;

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

public class MysqlCDCDataSourceChannel implements DataSourceChannel {

    public static final Set<String> MYSQL_SYSTEM_DATABASES =
            Sets.newHashSet("information_schema", "mysql", "performance_schema", "sys");

    @Override
    public boolean canAbleGetSchema() {
        return true;
    }

    @Override
    public OptionRule getDataSourceOptions(@NonNull String pluginName) {
        return MysqlCDCOptionRule.optionRule();
    }

    @Override
    public OptionRule getDatasourceMetadataFieldsByDataSourceName(@NonNull String pluginName) {
        return MysqlCDCOptionRule.metadataRule();
    }

    @Override
    public List<String> getTables(
            String pluginName,
            Map<String, String> requestParams,
            String database,
            Map<String, String> option) {
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
        return this.checkJdbcConnectivity(requestParams);
    }

    @Override
    public List<TableField> getTableFields(
            String pluginName, Map<String, String> requestParams, String database, String table) {
        return getTableFields(requestParams, database, table);
    }

    @Override
    public Map<String, List<TableField>> getTableFields(
            String pluginName,
            Map<String, String> requestParams,
            String database,
            List<String> tables) {
        Map<String, List<TableField>> tableFields = new HashMap<>(tables.size());
        for (String table : tables) {
            tableFields.put(table, getTableFields(requestParams, database, table));
        }
        return tableFields;
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    protected boolean checkJdbcConnectivity(Map<String, String> requestParams) {
        try (Connection connection = init(requestParams);
                Statement statement = connection.createStatement()) {

            try (ResultSet resultSet = statement.executeQuery("SHOW MASTER STATUS"); ) {
                if (resultSet.next()) {
                    String binlogFile = resultSet.getString("File");
                    if (StringUtils.isBlank(binlogFile)) {
                        throw new DataSourcePluginException("binlog must be enabled");
                    }
                } else {
                    throw new DataSourcePluginException("binlog must be enabled");
                }
            }

            try (ResultSet resultSet =
                    statement.executeQuery("SHOW VARIABLES LIKE 'binlog_format'")) {
                if (resultSet.next()) {
                    String binlogFormat = resultSet.getString("Value");
                    if (!"ROW".equalsIgnoreCase(binlogFormat)) {
                        throw new DataSourcePluginException("binlog_format must be ROW");
                    }
                } else {
                    throw new DataSourcePluginException("binlog_format must be ROW");
                }
            }

            try (ResultSet resultSet =
                    statement.executeQuery("SHOW VARIABLES LIKE 'binlog_row_image'")) {
                if (resultSet.next()) {
                    String binlogRowImage = resultSet.getString("Value");
                    if (!"FULL".equalsIgnoreCase(binlogRowImage)) {
                        throw new DataSourcePluginException("binlog_row_image must be FULL");
                    }
                } else {
                    throw new DataSourcePluginException("binlog_row_image must be FULL");
                }
            }
            return true;
        } catch (Exception e) {
            throw new DataSourcePluginException(
                    "check jdbc connectivity failed, " + e.getMessage(), e);
        }
    }

    protected Connection init(Map<String, String> requestParams) throws SQLException {
        if (null == requestParams.get(MysqlCDCOptionRule.BASE_URL.key())) {
            throw new DataSourcePluginException("Jdbc url is null");
        }
        String url = requestParams.get(MysqlCDCOptionRule.BASE_URL.key());
        if (null != requestParams.get(MysqlCDCOptionRule.PASSWORD.key())
                && null != requestParams.get(MysqlCDCOptionRule.USERNAME.key())) {
            String username = requestParams.get(MysqlCDCOptionRule.USERNAME.key());
            String password = requestParams.get(MysqlCDCOptionRule.PASSWORD.key());
            return DriverManager.getConnection(url, username, password);
        }
        return DriverManager.getConnection(url);
    }

    protected List<String> getDataBaseNames(Map<String, String> requestParams) throws SQLException {
        List<String> dbNames = new ArrayList<>();
        try (Connection connection = init(requestParams);
                PreparedStatement statement = connection.prepareStatement("SHOW DATABASES;");
                ResultSet re = statement.executeQuery()) {
            // filter system databases
            while (re.next()) {
                String dbName = re.getString("database");
                if (StringUtils.isNotBlank(dbName) && isNotSystemDatabase(dbName)) {
                    dbNames.add(dbName);
                }
            }
            return dbNames;
        }
    }

    protected List<String> getTableNames(Map<String, String> requestParams, String dbName) {
        List<String> tableNames = new ArrayList<>();
        try (Connection connection = init(requestParams);
                ResultSet resultSet =
                        connection
                                .getMetaData()
                                .getTables(dbName, null, null, new String[] {"TABLE"})) {
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
}
