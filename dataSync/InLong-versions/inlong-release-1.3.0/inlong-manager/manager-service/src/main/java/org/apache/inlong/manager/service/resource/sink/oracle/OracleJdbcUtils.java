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

package org.apache.inlong.manager.service.resource.sink.oracle;

import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.pojo.sink.oracle.OracleColumnInfo;
import org.apache.inlong.manager.pojo.sink.oracle.OracleTableInfo;
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
 * Utils for Oracle JDBC.
 */
public class OracleJdbcUtils {

    private static final String ORACLE_JDBC_PREFIX = "jdbc:oracle";

    private static final String ORACLE_DRIVER_CLASS = "oracle.jdbc.driver.OracleDriver";

    private static final Logger LOG = LoggerFactory.getLogger(OracleJdbcUtils.class);

    /**
     * Get Oracle connection from the url and user.
     *
     * @param url jdbc url,such as jdbc:oracle:thin@host:port:sid or jdbc:oracle:thin@host:port/service_name
     * @param user Username for JDBC URL
     * @param password User password
     * @return {@link Connection}
     * @throws Exception on get connection error
     */
    public static Connection getConnection(final String url, final String user, final String password)
            throws Exception {
        if (StringUtils.isBlank(url) || !url.startsWith(ORACLE_JDBC_PREFIX)) {
            throw new Exception("Oracle server URL was invalid, it should start with jdbc:oracle");
        }
        Connection conn;
        try {
            Class.forName(ORACLE_DRIVER_CLASS);
            conn = DriverManager.getConnection(url, user, password);
        } catch (Exception e) {
            final String errorMsg = "get Oracle connection error, please check Oracle JDBC url, username or password!";
            LOG.error(errorMsg, e);
            throw new Exception(errorMsg + " other error msg: " + e.getMessage());
        }
        if (Objects.isNull(conn)) {
            throw new Exception("get Oracle connection failed, please contact administrator.");
        }
        LOG.info("get Oracle connection success, url={}", url);
        return conn;
    }

    /**
     * Execute SQL command on Oracle.
     *
     * @param conn JDBC Connection  {@link Connection}
     * @param sql SQL string to be executed
     * @throws Exception on execute SQL error
     */
    public static void executeSql(final Connection conn, final String sql) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
        LOG.info("execute sql [{}] success", sql);
    }

    /**
     * Execute batch query SQL on Oracle.
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
            LOG.info("execute batch sql [{}] success", sqls);
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /**
     * Create Oracle table by OracleTableInfo
     *
     * @param conn JDBC Connection  {@link Connection}
     * @param tableInfo Oracle table info  {@link OracleTableInfo}
     * @throws Exception on create table error
     */
    public static void createTable(final Connection conn, final OracleTableInfo tableInfo) throws Exception {
        if (checkTablesExist(conn, tableInfo.getUserName(), tableInfo.getTableName())) {
            LOG.info("The table [{}] are exists", tableInfo.getTableName());
        } else {
            List<String> createTableSqls = OracleSqlBuilder.buildCreateTableSql(tableInfo);
            executeSqlBatch(conn, createTableSqls);
            LOG.info("execute sql [{}] success", createTableSqls);
        }
    }

    /**
     * Check tables from the Oracle information_schema.
     *
     * @param conn JDBC Connection  {@link Connection}
     * @param userName Oracle database name
     * @param tableName Oracle table name
     * @return true if table exist, otherwise false
     * @throws Exception on check table exist error
     */
    public static boolean checkTablesExist(final Connection conn, final String userName, final String tableName)
            throws Exception {
        boolean result = false;
        final String checkTableSql = OracleSqlBuilder.getCheckTable(userName, tableName);
        try (Statement statement = conn.createStatement();
                ResultSet resultSet = statement.executeQuery(checkTableSql)) {
            if (Objects.nonNull(resultSet) && resultSet.next()) {
                int size = resultSet.getInt(1);
                if (size > 0) {
                    result = true;
                }
            }
        }
        LOG.info("check table exist for username={} table={}, result={}", userName, tableName, result);
        return result;
    }

    /**
     * Check whether the column exists in the table.
     *
     * @param conn JDBC Connection  {@link Connection}
     * @param tableName Oracle table name
     * @param column Oracle table column name
     * @return true if column exist in the table, otherwise false
     * @throws Exception on check column exist error
     */
    public static boolean checkColumnExist(final Connection conn, final String tableName, final String column)
            throws Exception {
        boolean result = false;
        final String checkColumnSql = OracleSqlBuilder.getCheckColumn(tableName, column);
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
     * Query all columns of the tableName.
     *
     * @param conn JDBC Connection  {@link Connection}
     * @param tableName Oracle table name
     * @return {@link List}
     * @throws Exception on get columns error
     */
    public static List<OracleColumnInfo> getColumns(final Connection conn, final String tableName) throws Exception {
        String querySql = OracleSqlBuilder.buildDescTableSql(tableName);
        List<OracleColumnInfo> columnList = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(querySql)) {
            while (rs.next()) {
                OracleColumnInfo columnInfo = new OracleColumnInfo(rs.getString(1),
                        rs.getString(2), rs.getString(3));
                columnList.add(columnInfo);
            }
        }
        return columnList;
    }

    /**
     * Add columns for Oracle table.
     *
     * @param conn JDBC Connection  {@link Connection}
     * @param tableName Oracle table name
     * @param columns Oracle columns to be added
     * @throws Exception on add columns error
     */
    public static void addColumns(final Connection conn, final String tableName, final List<OracleColumnInfo> columns)
            throws Exception {
        List<OracleColumnInfo> columnInfos = new ArrayList<>();

        for (OracleColumnInfo columnInfo : columns) {
            if (!checkColumnExist(conn, tableName, columnInfo.getName())) {
                columnInfos.add(columnInfo);
            }
        }
        List<String> addColumnSql = OracleSqlBuilder.buildAddColumnsSql(tableName, columnInfos);
        executeSqlBatch(conn, addColumnSql);
    }
}
