/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.service.resource.sink.postgresql;

import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.pojo.sink.postgresql.PostgreSQLColumnInfo;
import org.apache.inlong.manager.pojo.sink.postgresql.PostgreSQLTableInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Utils for PostgreSQL JDBC.
 */
public class PostgreSQLJdbcUtils {

    private static final String POSTGRES_DRIVER_CLASS = "org.postgresql.Driver";

    private static final String POSTGRES_JDBC_PREFIX = "jdbc:postgresql";

    private static final String POSTGRESQL_DEFAULT_SCHEMA = "public";

    private static final Logger LOG = LoggerFactory.getLogger(PostgreSQLJdbcUtils.class);

    /**
     * Get PostgreSQL connection from the url and user.
     *
     * @param url jdbc url, such as jdbc:mysql://host:port/database
     * @param user Username for JDBC URL
     * @param password User password
     * @return {@link Connection}
     * @throws Exception on get connection error
     */
    public static Connection getConnection(String url, String user, String password)
            throws Exception {
        if (StringUtils.isBlank(url) || !url.startsWith(POSTGRES_JDBC_PREFIX)) {
            throw new Exception("PostgreSQL server URL was invalid, it should start with jdbc:postgresql");
        }
        Connection conn;
        try {
            Class.forName(POSTGRES_DRIVER_CLASS);
            conn = DriverManager.getConnection(url, user, password);
        } catch (Exception e) {
            String errorMsg = "get PostgreSQL connection error, please check postgresql jdbc url, username or password";
            LOG.error(errorMsg, e);
            throw new Exception(errorMsg + ": " + e.getMessage());
        }
        if (conn == null) {
            throw new Exception("get PostgreSQL connection failed, please contact administrator");
        }
        LOG.info("get PostgreSQL connection success, url={}", url);
        return conn;
    }

    /**
     * Execute SQL command on PostgreSQL.
     *
     * @param conn JDBC Connection  {@link Connection}
     * @param sql SQL string to be executed
     * @throws Exception on execute SQL error
     */
    public static void executeSql(final Connection conn, final String sql) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            LOG.info("execute sql [{}] success", sql);
        }
    }

    /**
     * Execute batch query SQL on PostgreSQL.
     *
     * @param conn JDBC Connection  {@link Connection}
     * @param sqls SQL string to be executed
     * @throws Exception on get execute SQL batch error
     */
    public static void executeSqlBatch(final Connection conn, final List<String> sqls) throws Exception {
        conn.setAutoCommit(false);
        try (Statement stmt = conn.createStatement()) {
            for (String entry : sqls) {
                stmt.execute(entry);
            }
            conn.commit();
            LOG.info("execute sql [{}] success", sqls);
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /**
     * Create PostgreSQL schema by schemaNama
     *
     * @param conn JDBC Connection  {@link Connection}
     * @param schemaName PostgreSQL schema name
     * @param userName PostgreSQL user name
     * @throws Exception on create schema error
     */
    public static void createSchema(final Connection conn, final String schemaName, final String userName)
            throws Exception {
        if (checkSchemaExist(conn, schemaName)) {
            LOG.info("the schema [{}] are exists", schemaName);
        } else {
            final String sql = PostgreSQLSqlBuilder.buildCreateSchema(schemaName, userName);
            executeSql(conn, sql);
            LOG.info("execute create schema sql [{}] success", sql);
        }
    }

    /**
     * Check whether the schema exists.
     *
     * @param conn JDBC Connection  {@link Connection}
     * @param schemaName PostgreSQL schema name
     * @return true if schema exist in the table, otherwise false
     * @throws Exception on check column exist error
     */
    public static boolean checkSchemaExist(final Connection conn, final String schemaName) throws Exception {
        boolean result = false;
        if (POSTGRESQL_DEFAULT_SCHEMA.equals(schemaName)) {
            result = true;
        } else {
            final String checkColumnSql = PostgreSQLSqlBuilder.getCheckSchema(schemaName);
            try (Statement statement = conn.createStatement();
                    ResultSet resultSet = statement.executeQuery(checkColumnSql)) {
                if (Objects.nonNull(resultSet) && resultSet.next()) {
                    int count = resultSet.getInt(1);
                    if (count > 0) {
                        result = true;
                    }
                }
            }
        }
        LOG.info("check schema exist for schema={}, result={}", schemaName, result);
        return result;
    }

    /**
     * Check whether the column exists in the table.
     *
     * @param conn JDBC Connection  {@link Connection}
     * @param schemaName PostgreSQL schema name
     * @param tableName PostgreSQL table name
     * @param column PostgreSQL table column name
     * @return true if column exist in the table, otherwise false
     * @throws Exception on check column exist error
     */
    public static boolean checkColumnExist(final Connection conn, final String schemaName, final String tableName,
            final String column) throws Exception {
        boolean result = false;
        final String checkColumnSql = PostgreSQLSqlBuilder.getCheckColumn(schemaName, tableName, column);
        try (Statement statement = conn.createStatement();
                ResultSet resultSet = statement.executeQuery(checkColumnSql)) {
            if (Objects.nonNull(resultSet) && resultSet.next()) {
                int count = resultSet.getInt(1);
                if (count > 0) {
                    result = true;
                }
            }
        }
        LOG.info("check column exist for table={}, column={}, result={}", tableName, column, result);
        return result;
    }

    /**
     * Create Greenplum table by GreenplumTableInfo
     *
     * @param conn JDBC Connection  {@link Connection}
     * @param tableInfo Greenplum table info  {@link PostgreSQLTableInfo}
     * @throws Exception on create table error
     */
    public static void createTable(final Connection conn, final PostgreSQLTableInfo tableInfo)
            throws Exception {
        if (checkTablesExist(conn, tableInfo.getSchemaName(), tableInfo.getTableName())) {
            LOG.info("the table [{}] are exists", tableInfo.getTableName());
        } else {
            final List<String> createTableSqls = PostgreSQLSqlBuilder.buildCreateTableSql(tableInfo);
            executeSqlBatch(conn, createTableSqls);
            LOG.info("execute sql [{}] success", createTableSqls);
        }
    }

    /**
     * Check tables from the Greenplum information_schema.
     *
     * @param conn JDBC Connection  {@link Connection}
     * @param schemaName PostgreSQL database name
     * @param tableName PostgreSQL table name
     * @return true if table exist, otherwise false
     * @throws Exception on check table exist error
     */
    public static boolean checkTablesExist(final Connection conn, final String schemaName, final String tableName)
            throws Exception {
        boolean result = false;
        final String checkTableSql = PostgreSQLSqlBuilder.getCheckTable(schemaName, tableName);
        try (Statement statement = conn.createStatement();
                ResultSet resultSet = statement.executeQuery(checkTableSql)) {
            if (null != resultSet && resultSet.next()) {
                int size = resultSet.getInt(1);
                if (size > 0) {
                    result = true;
                }
            }
        }
        LOG.info("check table exist for username={} table={}, result={}", schemaName, tableName, result);
        return result;
    }

    /**
     * Query all columns of the tableName.
     *
     * @param conn JDBC Connection  {@link Connection}
     * @param schemaName PostgreSQL schema name
     * @param tableName PostgreSQL table name
     * @return {@link List}
     * @throws Exception on get columns error
     */
    public static List<PostgreSQLColumnInfo> getColumns(final Connection conn, final String schemaName,
            final String tableName) throws Exception {
        final List<PostgreSQLColumnInfo> columnList = new ArrayList<>();
        final String querySql = PostgreSQLSqlBuilder.buildDescTableSql(schemaName, tableName);

        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(querySql)) {
            while (rs.next()) {
                columnList.add(new PostgreSQLColumnInfo(rs.getString(1), rs.getString(2),
                        rs.getString(3)));
            }
        }
        return columnList;
    }

    /**
     * Add columns for Greenpluum table.
     *
     * @param conn JDBC Connection  {@link Connection}
     * @param schemaName PostgreSQL schema name
     * @param tableName PostgreSQL table name
     * @param columns PostgreSQL columns to be added
     * @throws Exception on add columns error
     */
    public static void addColumns(final Connection conn, final String schemaName, final String tableName,
            final List<PostgreSQLColumnInfo> columns) throws Exception {
        final List<PostgreSQLColumnInfo> columnInfos = new ArrayList<>();

        for (PostgreSQLColumnInfo columnInfo : columns) {
            if (!checkColumnExist(conn, schemaName, tableName, columnInfo.getName())) {
                columnInfos.add(columnInfo);
            }
        }
        final List<String> addColumnSql = PostgreSQLSqlBuilder.buildAddColumnsSql(schemaName, tableName, columnInfos);
        executeSqlBatch(conn, addColumnSql);
        LOG.info("execute add columns sql [{}] success", addColumnSql);
    }
}
