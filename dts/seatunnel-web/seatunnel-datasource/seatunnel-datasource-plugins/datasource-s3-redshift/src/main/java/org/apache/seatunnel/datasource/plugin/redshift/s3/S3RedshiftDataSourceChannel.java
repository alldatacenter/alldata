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

package org.apache.seatunnel.datasource.plugin.redshift.s3;

import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.datasource.plugin.api.DataSourceChannel;
import org.apache.seatunnel.datasource.plugin.api.DataSourcePluginException;
import org.apache.seatunnel.datasource.plugin.api.model.TableField;
import org.apache.seatunnel.datasource.plugin.api.utils.JdbcUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;

import com.google.common.collect.Sets;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class S3RedshiftDataSourceChannel implements DataSourceChannel {

    @Override
    public OptionRule getDataSourceOptions(@NonNull String pluginName) {
        return S3RedshiftOptionRule.optionRule();
    }

    @Override
    public OptionRule getDatasourceMetadataFieldsByDataSourceName(@NonNull String pluginName) {
        return S3RedshiftOptionRule.metadataRule();
    }

    @Override
    public List<String> getTables(
            @NonNull String pluginName, Map<String, String> requestParams, String database) {
        return getTableNames(requestParams, database);
    }

    @Override
    public List<String> getDatabases(
            @NonNull String pluginName, @NonNull Map<String, String> requestParams) {
        try {
            return getDataBaseNames(pluginName, requestParams);
        } catch (SQLException e) {
            throw new DataSourcePluginException("Query redshift databases failed", e);
        }
    }

    @Override
    public boolean checkDataSourceConnectivity(
            @NonNull String pluginName, @NonNull Map<String, String> requestParams) {
        checkHdfsS3Connection(requestParams);
        checkJdbcConnection(requestParams);
        return true;
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
        // not need this method
        return null;
    }

    private void checkJdbcConnection(Map<String, String> requestParams) {
        String jdbcUrl = requestParams.get(S3RedshiftOptionRule.JDBC_URL.key());
        String username = requestParams.get(S3RedshiftOptionRule.JDBC_USER.key());
        String password = requestParams.get(S3RedshiftOptionRule.JDBC_PASSWORD.key());
        if (StringUtils.isBlank(jdbcUrl)) {
            throw new DataSourcePluginException("Redshift Jdbc url is empty");
        }
        if (StringUtils.isBlank(username) && StringUtils.isBlank(password)) {
            try (Connection ignored = DriverManager.getConnection(jdbcUrl)) {
                log.info("Redshift jdbc connection is valid");
                return;
            } catch (SQLException e) {
                throw new DataSourcePluginException(
                        "Check Redshift jdbc connection failed,please check your config", e);
            }
        }
        try (Connection ignored = DriverManager.getConnection(jdbcUrl, username, password)) {
            log.info("Redshift jdbc connection is valid");
        } catch (SQLException e) {
            throw new DataSourcePluginException(
                    "Check Redshift jdbc connection failed,please check your config", e);
        }
    }

    private void checkHdfsS3Connection(Map<String, String> requestParams) {
        Configuration s3Conf = HadoopS3AConfiguration.getConfiguration(requestParams);
        try (FileSystem fs = FileSystem.get(s3Conf)) {
            fs.getFileStatus(new org.apache.hadoop.fs.Path("/"));
        } catch (IOException e) {
            throw new DataSourcePluginException(
                    "S3 configuration is invalid, please check your config", e);
        }
    }

    protected Connection init(Map<String, String> requestParams, String databaseName)
            throws SQLException {
        if (null == requestParams.get(S3RedshiftOptionRule.JDBC_URL.key())) {
            throw new DataSourcePluginException("Jdbc url is null");
        }
        String url =
                JdbcUtils.replaceDatabase(
                        requestParams.get(S3RedshiftOptionRule.JDBC_URL.key()), databaseName);
        if (null != requestParams.get(S3RedshiftOptionRule.JDBC_PASSWORD.key())
                && null != requestParams.get(S3RedshiftOptionRule.JDBC_USER.key())) {
            String username = requestParams.get(S3RedshiftOptionRule.JDBC_USER.key());
            String password = requestParams.get(S3RedshiftOptionRule.JDBC_PASSWORD.key());
            return DriverManager.getConnection(url, username, password);
        }
        return DriverManager.getConnection(url);
    }

    protected List<String> getDataBaseNames(String pluginName, Map<String, String> requestParams)
            throws SQLException {
        List<String> dbNames = new ArrayList<>();
        try (Connection connection = init(requestParams, null);
                PreparedStatement statement =
                        connection.prepareStatement("select datname from pg_database;");
                ResultSet re = statement.executeQuery()) {
            while (re.next()) {
                String dbName = re.getString("datname");
                if (StringUtils.isNotBlank(dbName) && isNotSystemDatabase(dbName)) {
                    dbNames.add(dbName);
                }
            }
            return dbNames;
        } catch (SQLException e) {
            throw new DataSourcePluginException("get databases failed", e);
        }
    }

    protected List<String> getTableNames(Map<String, String> requestParams, String dbName) {
        List<String> tableNames = new ArrayList<>();
        try (Connection connection = init(requestParams, dbName); ) {
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
        try (Connection connection = init(requestParams, dbName); ) {
            DatabaseMetaData metaData = connection.getMetaData();
            String primaryKey = getPrimaryKey(metaData, dbName, tableName);
            String[] split = tableName.split("\\.");
            if (split.length != 2) {
                throw new DataSourcePluginException(
                        "Postgresql tableName should composed by schemaName.tableName");
            }
            ResultSet resultSet = metaData.getColumns(dbName, split[0], split[1], null);
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

            throw new DataSourcePluginException("check host connectable failed", e);
        }
    }

    private boolean isNotSystemDatabase(String dbName) {
        return !POSTGRESQL_SYSTEM_DATABASES.contains(dbName.toLowerCase());
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

    public static final Set<String> POSTGRESQL_SYSTEM_DATABASES =
            Sets.newHashSet(
                    "information_schema",
                    "pg_catalog",
                    "root",
                    "pg_toast",
                    "pg_temp_1",
                    "pg_toast_temp_1",
                    "postgres",
                    "template0",
                    "template1");
}
