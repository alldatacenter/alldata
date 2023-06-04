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

package org.apache.inlong.manager.service.resource.sink.greenplum;

import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.pojo.sink.greenplum.GreenplumColumnInfo;
import org.apache.inlong.manager.pojo.sink.greenplum.GreenplumTableInfo;
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
 * Utils for Greenplum JDBC.
 */
public class GreenplumJdbcUtils {

    private static final String GREENPLUM_JDBC_PREFIX = "jdbc:postgresql";

    private static final String GREENPLUM_DRIVER_CLASS = "org.postgresql.Driver";

    public static final String GREENPLUM_DEFAULT_SCHEMA = "public";

    private static final Logger LOG = LoggerFactory.getLogger(GreenplumJdbcUtils.class);

    /**
     * Get Greenplum connection from the url and user
     */
    public static Connection getConnection(final String url, final String user, final String password)
            throws Exception {
        if (StringUtils.isBlank(url) || !url.startsWith(GREENPLUM_JDBC_PREFIX)) {
            throw new Exception("Greenplum server URL was invalid, it should start with jdbc:postgresql");
        }
        final Connection conn;
        try {
            Class.forName(GREENPLUM_DRIVER_CLASS);
            conn = DriverManager.getConnection(url, user, password);
        } catch (Exception e) {
            final String errorMsg = "get Greenplum connection error, please check postgres jdbc url,"
                    + " username or password!";
            LOG.error(errorMsg, e);
            throw new Exception(errorMsg + " other error msg: " + e.getMessage());
        }
        if (conn == null) {
            throw new Exception("get Greenplum connection failed, please contact administrator");
        }
        LOG.info("get Greenplum connection success, url={}", url);
        return conn;
    }

    /**
     * Execute SQL command on Greenplum.
     *
     * @param conn JDBC Connection  {@link Connection}
     * @param sql SQL String to be executed
     * @throws Exception on execute SQL error
     */
    public static void executeSql(final Connection conn, final String sql) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            LOG.info("execute sql [{}] success", sql);
        }
    }

    /**
     * Execute batch query SQL on Greenplum.
     *
     * @param conn JDBC Connection  {@link Connection}
     * @param sqls SQL String to be executed
     * @throws Exception on get execute SQL batch error
     */
    public static void executeSqlBatch(final Connection conn, final List<String> sqls)
            throws Exception {
        conn.setAutoCommit(false);
        try (Statement stmt = conn.createStatement()) {
            for (String entry : sqls) {
                stmt.execute(entry);
            }
            conn.commit();
        } finally {
            conn.setAutoCommit(true);
        }
        LOG.info("execute batch sql [{}] success", sqls);
    }

    /**
     * Create Greenplum table by GreenplumTableInfo
     *
     * @param conn JDBC Connection  {@link Connection}
     * @param tableInfo Greenplum table info  {@link GreenplumTableInfo}
     * @throws Exception on create table error
     */
    public static void createTable(final Connection conn, final GreenplumTableInfo tableInfo)
            throws Exception {
        if (checkTablesExist(conn, tableInfo.getSchemaName(), tableInfo.getTableName())) {
            LOG.info("the table [{}] are exists", tableInfo.getTableName());
        } else {
            final List<String> createTableSqls = GreenplumSqlBuilder.buildCreateTableSql(tableInfo);
            executeSqlBatch(conn, createTableSqls);
            LOG.info("execute sql [{}] success", createTableSqls);
        }
    }

    /**
     * Create Greenplum schema by schemaNama
     *
     * @param conn JDBC Connection  {@link Connection}
     * @param schemaName Greenplum schema name
     * @param userName Greenplum user name
     * @throws Exception on create schema error
     */
    public static void createSchema(final Connection conn, final String schemaName, final String userName)
            throws Exception {
        if (checkSchemaExist(conn, schemaName)) {
            LOG.info("the schema [{}] are exists", schemaName);
        } else {
            final String sql = GreenplumSqlBuilder.buildCreateSchema(schemaName, userName);
            executeSql(conn, sql);
            LOG.info("execute create schema sql [{}] success", sql);
        }
    }

    /**
     * Check tables from the Greenplum information_schema.
     *
     * @param conn JDBC Connection  {@link Connection}
     * @param schemaName Greenplum database name
     * @param tableName Greenplum table name
     * @return true if table exist, otherwise false
     * @throws Exception on check table exist error
     */
    public static boolean checkTablesExist(final Connection conn, final String schemaName, final String tableName)
            throws Exception {
        boolean result = false;
        final String checkTableSql = GreenplumSqlBuilder.getCheckTable(schemaName, tableName);
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
     * Check whether the column exists in the table.
     *
     * @param conn JDBC Connection  {@link Connection}
     * @param schemaName Greenplum schema name
     * @param tableName Greenplum table name
     * @param column Greenplum table column name
     * @return true if column exist in the table, otherwise false
     * @throws Exception on check column exist error
     */
    public static boolean checkColumnExist(final Connection conn, final String schemaName, final String tableName,
            final String column) throws Exception {
        boolean result = false;
        final String checkColumnSql = GreenplumSqlBuilder.getCheckColumn(schemaName, tableName, column);
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
     * Check whether the schema exists.
     *
     * @param conn JDBC Connection  {@link Connection}
     * @param schemaName Greenplum schema name
     * @return true if schema exist in the table, otherwise false
     * @throws Exception on check column exist error
     */
    public static boolean checkSchemaExist(final Connection conn, final String schemaName) throws Exception {
        boolean result = false;
        if (GREENPLUM_DEFAULT_SCHEMA.equals(schemaName)) {
            result = true;
        } else {
            final String checkColumnSql = GreenplumSqlBuilder.getCheckSchema(schemaName);
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
     * Query all columns of the tableName.
     *
     * @param conn JDBC Connection  {@link Connection}
     * @param schemaName Greenplum schema name
     * @param tableName Greenplum table name
     * @return {@link List}
     * @throws Exception on get columns error
     */
    public static List<GreenplumColumnInfo> getColumns(final Connection conn, final String schemaName,
            final String tableName) throws Exception {
        final List<GreenplumColumnInfo> columnList = new ArrayList<>();
        final String querySql = GreenplumSqlBuilder.buildDescTableSql(schemaName, tableName);

        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(querySql)) {
            while (rs.next()) {
                columnList.add(new GreenplumColumnInfo(rs.getString(1), rs.getString(2),
                        rs.getString(3)));
            }
        }
        return columnList;
    }

    /**
     * Add columns for Greenpluum table.
     *
     * @param conn JDBC Connection  {@link Connection}
     * @param schemaName Greenpluum schema name
     * @param tableName Greenpluum table name
     * @param columns Greenpluum columns to be added
     * @throws Exception on add columns error
     */
    public static void addColumns(final Connection conn, final String schemaName, final String tableName,
            final List<GreenplumColumnInfo> columns) throws Exception {
        final List<GreenplumColumnInfo> columnInfos = new ArrayList<>();

        for (GreenplumColumnInfo columnInfo : columns) {
            if (!checkColumnExist(conn, schemaName, tableName, columnInfo.getName())) {
                columnInfos.add(columnInfo);
            }
        }
        final List<String> addColumnSql = GreenplumSqlBuilder.buildAddColumnsSql(schemaName, tableName, columnInfos);
        executeSqlBatch(conn, addColumnSql);
        LOG.info("execute add columns sql [{}] success", addColumnSql);
    }
}
