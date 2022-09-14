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

package org.apache.inlong.manager.service.resource.sink.sqlserver;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.pojo.sink.sqlserver.SQLServerColumnInfo;
import org.apache.inlong.manager.pojo.sink.sqlserver.SQLServerTableInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Utils for SQLServer JDBC.
 */
public class SQLServerJdbcUtils {

    private static final String SQLSERVER_JDBC_PREFIX = "jdbc:sqlserver";

    private static final String SQLSERVER_DRIVER_CLASS = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

    private static final Logger LOG = LoggerFactory.getLogger(SQLServerJdbcUtils.class);

    /**
     * Get SQLServer connection from the url and user.
     *
     * @param url jdbc url, such as jdbc:sqlserver://host:port
     * @param user Username for JDBC URL
     * @param password User password
     * @return {@link Connection}
     * @throws Exception on get connection error
     */
    public static Connection getConnection(final String url, final String user, final String password)
            throws Exception {
        if (StringUtils.isBlank(url) || !url.startsWith(SQLSERVER_JDBC_PREFIX)) {
            throw new Exception("SQLServer server URL was invalid, it should start with jdbc:sqlserver");
        }
        Connection conn;
        try {
            Class.forName(SQLSERVER_DRIVER_CLASS);
            conn = DriverManager.getConnection(url, user, password);
        } catch (Exception e) {
            final String errorMsg = "get SQLServer connection error, please check SQLServer JDBC url, "
                    + "username or password!";
            LOG.error(errorMsg, e);
            throw new Exception(errorMsg + " other error msg: " + e.getMessage());
        }
        if (conn == null) {
            throw new Exception("get SQLServer connection failed, please contact administrator.");
        }
        LOG.info("get SQLServer connection success, url={}", url);
        return conn;
    }

    /**
     * Execute SQL command on SQLServer.
     *
     * @param conn JDBC Connection  {@link Connection}
     * @param sql SQL string to be executed
     * @throws Exception on execute SQL error
     */
    public static void executeSql(final Connection conn, final String sql) throws Exception {
        conn.setAutoCommit(true);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            LOG.info("execute sql [{}] success", sql);
        }
    }

    /**
     * Execute batch query SQL on SQLServer.
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
        } finally {
            conn.setAutoCommit(true);
        }
        LOG.info("execute sql [{}] success", sqls);
    }

    /**
     * create SQLServer schema
     *
     * @param conn JDBC Connection  {@link Connection}
     * @param schemaName SQLServer schema name
     * @throws Exception on create schema error
     */
    public static void createSchema(final Connection conn, final String schemaName) throws Exception {
        if (!checkSchemaExist(conn, schemaName)) {
            final String createSql = SQLServerSqlBuilder.buildCreateSchemaSql(schemaName);
            executeSql(conn, createSql);
            LOG.info("execute sql [{}] success", createSql);
        } else {
            LOG.info("The schemaName [{}] are exists", schemaName);
        }
    }

    /**
     * Create SQLServer table by SQLServerTableInfo
     *
     * @param conn JDBC Connection  {@link Connection}
     * @param tableInfo SQLServer table info  {@link SQLServerTableInfo}
     * @throws Exception on create table error
     */
    public static void createTable(final Connection conn, final SQLServerTableInfo tableInfo)
            throws Exception {
        if (checkTablesExist(conn, tableInfo.getSchemaName(), tableInfo.getTableName())) {
            LOG.info("The table [{}] are exists", tableInfo.getTableName());
        } else {
            final List<String> createTableSqls = SQLServerSqlBuilder.buildCreateTableSql(tableInfo);
            executeSqlBatch(conn, createTableSqls);
            LOG.info("execute sql [{}] success", createTableSqls);
        }
    }

    /**
     * Check tables from the SQLServer information_schema.
     *
     * @param conn JDBC Connection  {@link Connection}
     * @param schemaName SQLServer schema name
     * @param tableName SQLServer table name
     * @return true if table exist, otherwise false
     * @throws Exception on check table exist error
     */
    public static boolean checkTablesExist(final Connection conn, final String schemaName, final String tableName)
            throws Exception {
        boolean result = false;
        final String checkTableSql = SQLServerSqlBuilder.getCheckTable(schemaName, tableName);
        try (Statement statement = conn.createStatement();
                ResultSet resultSet = statement.executeQuery(checkTableSql)) {
            if (null != resultSet && resultSet.next()) {
                int count = resultSet.getInt(1);
                if (count > 0) {
                    result = true;
                }
            }
        }
        LOG.info("check table exist for schema={} table={}, result={}", schemaName, tableName, result);
        return result;
    }

    /**
     * Check schema from the SQLServer information_schema.schemata.
     *
     * @param conn JDBC Connection  {@link Connection}
     * @param schemaName SQLServer schema
     * @return true if schema exist, otherwise false
     * @throws Exception on check schema exist error
     */
    public static boolean checkSchemaExist(final Connection conn, final String schemaName) throws Exception {
        boolean result = false;
        final String checkSchemaSql = SQLServerSqlBuilder.getCheckSchema(schemaName);
        try (Statement statement = conn.createStatement();
                ResultSet resultSet = statement.executeQuery(checkSchemaSql)) {
            if (null != resultSet && resultSet.next()) {
                int count = resultSet.getInt(1);
                if (count > 0) {
                    result = true;
                }
            }
        }
        LOG.info("check schema exist for schemaName={}, result={}", schemaName, result);
        return result;
    }

    /**
     * Check whether the column exists in the table.
     *
     * @param conn JDBC Connection  {@link Connection}
     * @param schemaName SQLServer schema name
     * @param tableName SQLServer table name
     * @param column SQLServer table column name
     * @return true if column exist in the table, otherwise false
     * @throws Exception on check column exist error
     */
    public static boolean checkColumnExist(final Connection conn, final String schemaName, final String tableName,
            final String column) throws Exception {
        boolean result = false;
        final String checkTableSql = SQLServerSqlBuilder.getCheckColumn(schemaName, tableName, column);
        try (Statement statement = conn.createStatement();
                ResultSet resultSet = statement.executeQuery(checkTableSql)) {
            if (null != resultSet && resultSet.next()) {
                int count = resultSet.getInt(1);
                if (count > 0) {
                    result = true;
                }
            }
        }
        LOG.info("check column exist for schema={} table={}, result={} column={}", schemaName, tableName, result,
                column);
        return result;
    }

    /**
     * Query all columns of the tableName.
     *
     * @param conn JDBC Connection  {@link Connection}
     * @param schemaName SQLServer schema name
     * @param tableName SQLServer table name
     * @return {@link List}
     * @throws Exception on get columns error
     */
    public static List<SQLServerColumnInfo> getColumns(final Connection conn, final String schemaName,
            final String tableName) throws Exception {
        final String querySql = SQLServerSqlBuilder.buildDescTableSql(schemaName, tableName);
        final List<SQLServerColumnInfo> columnList = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(querySql)) {
            while (rs.next()) {
                SQLServerColumnInfo columnInfo = new SQLServerColumnInfo();
                columnInfo.setName(rs.getString(1));
                columnInfo.setType(rs.getString(2));
                columnInfo.setComment(rs.getString(3));
                columnList.add(columnInfo);
            }
        }
        return columnList;
    }

    /**
     * Add columns for SQLServer table.
     *
     * @param conn JDBC Connection  {@link Connection}
     * @param schemaName SQLServer schema name
     * @param tableName SQLServer table name
     * @param columns SQLServer columns to be added
     * @throws Exception on add columns error
     */
    public static void addColumns(final Connection conn, final String schemaName, final String tableName,
            List<SQLServerColumnInfo> columns) throws Exception {
        final List<SQLServerColumnInfo> columnInfos = Lists.newArrayList();

        for (SQLServerColumnInfo columnInfo : columns) {
            if (!checkColumnExist(conn, schemaName, tableName, columnInfo.getName())) {
                columnInfos.add(columnInfo);
            }
        }
        executeSqlBatch(conn, SQLServerSqlBuilder.buildAddColumnsSql(schemaName, tableName, columnInfos));
    }
}
