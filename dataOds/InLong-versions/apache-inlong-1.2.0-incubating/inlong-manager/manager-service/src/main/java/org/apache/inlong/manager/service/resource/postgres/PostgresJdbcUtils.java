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

package org.apache.inlong.manager.service.resource.postgres;

import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.pojo.sink.postgres.PostgresColumnInfo;
import org.apache.inlong.manager.common.pojo.sink.postgres.PostgresTableInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Utils for Postgres JDBC.
 */
public class PostgresJdbcUtils {

    private static final String POSTGRES_DRIVER_CLASS = "org.postgresql.Driver";
    private static final String SCHEMA_PATTERN = "public";
    private static final String TABLE_TYPE = "TABLE";
    private static final String COLUMN_LABEL_TABLE = "TABLE_NAME";
    private static final String COLUMN_LABEL_COUNT = "count";
    private static final String POSTGRES_JDBC_PREFIX = "jdbc:postgresql";

    private static final Logger LOG = LoggerFactory.getLogger(PostgresJdbcUtils.class);

    /**
     * Get Postgres connection from Postgres url and user
     */
    public static Connection getConnection(String url, String user, String password)
            throws Exception {
        if (StringUtils.isBlank(url) || !url.startsWith(POSTGRES_JDBC_PREFIX)) {
            throw new Exception("Postgres server URL was invalid, it should start with jdbc:postgresql");
        }
        Connection conn;
        try {
            Class.forName(POSTGRES_DRIVER_CLASS);
            conn = DriverManager.getConnection(url, user, password);
        } catch (Exception e) {
            String errorMsg = "get postgres connection error, please check postgres jdbc url, username or password!";
            LOG.error(errorMsg, e);
            throw new Exception(errorMsg + " other error msg: " + e.getMessage());
        }
        if (conn == null) {
            throw new Exception("get postgres connection failed, please contact administrator");
        }
        LOG.info("get postgres connection success, url={}", url);
        return conn;
    }

    /**
     * Execute One Postgres Sql command
     *
     * @return true if execute successfully
     */
    public static boolean executeSql(String sql, String url, String user, String password) throws Exception {
        try (Connection conn = getConnection(url, user, password)) {
            Statement stmt = conn.createStatement();
            LOG.info("execute sql [{}] success for url: {}", sql, url);
            return stmt.execute(sql);
        }
    }

    /**
     * Execute One query Postgres Sql command
     *
     * @return the query result set
     */
    public static ResultSet executeQuerySql(String sql, String url, String user, String password)
            throws Exception {
        try (Connection conn = getConnection(url, user, password)) {
            Statement stmt = conn.createStatement();
            LOG.info("execute sql [{}] success for url: {}", sql, url);
            return stmt.executeQuery(sql);
        }
    }

    /**
     * Execute Batch Postgres Sql commands
     */
    public static void executeSqlBatch(List<String> sql, String url, String user, String password)
            throws Exception {
        try (Connection conn = getConnection(url, user, password)) {
            Statement stmt = conn.createStatement();
            for (String entry : sql) {
                stmt.execute(entry);
            }
            LOG.info("execute sql [{}] success for url: {}", sql, url);
        }
    }

    /**
     * Create Postgres database
     */
    public static void createDb(String url, String user, String password, String dbName) throws Exception {
        String checkDbSql = PostgresSqlBuilder.getCheckDatabase(dbName);
        ResultSet resultSet = executeQuerySql(checkDbSql, url, user, password);
        if (resultSet != null) {
            resultSet.next();
            if (resultSet.getInt(COLUMN_LABEL_COUNT) == 0) {
                String createDbSql = PostgresSqlBuilder.buildCreateDbSql(dbName);
                executeSql(createDbSql, url, user, password);
            }
        }
    }

    /**
     * Create Postgres table
     */
    public static void createTable(String url, String user, String password, PostgresTableInfo tableInfo)
            throws Exception {
        String createTableSql = PostgresSqlBuilder.buildCreateTableSql(tableInfo);
        PostgresJdbcUtils.executeSql(createTableSql, url, user, password);
    }

    /**
     * Get Postgres tables from the Postgres metadata
     */
    public static boolean checkTablesExist(String url, String user, String password, String dbName, String tableName)
            throws Exception {
        boolean result = false;
        try (Connection conn = getConnection(url, user, password)) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getTables(conn.getCatalog(), SCHEMA_PATTERN, tableName, new String[]{TABLE_TYPE});
            if (rs != null) {
                rs.next();
                result = rs.getRow() > 0 && tableName.equals(rs.getString(COLUMN_LABEL_TABLE));
                LOG.info("check table exist for db={} table={}, result={}", dbName, tableName, result);
            }
        }
        return result;
    }

    /**
     * Query Postgres columns
     */
    public static List<PostgresColumnInfo> getColumns(String url, String user, String password, String tableName)
            throws Exception {
        String querySql = PostgresSqlBuilder.buildDescTableSql(tableName);
        try (Connection conn = getConnection(url, user, password);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(querySql)) {
            List<PostgresColumnInfo> columnList = new ArrayList<>();

            while (rs.next()) {
                PostgresColumnInfo columnInfo = new PostgresColumnInfo();
                columnInfo.setName(rs.getString(1));
                columnInfo.setType(rs.getString(2));
                columnList.add(columnInfo);
            }
            return columnList;
        }
    }

    /**
     * Add columns for Postgres table
     */
    public static void addColumns(String url, String user, String password, String tableName,
            List<PostgresColumnInfo> columnList) throws Exception {
        List<String> addColumnSql = PostgresSqlBuilder.buildAddColumnsSql(tableName, columnList);
        PostgresJdbcUtils.executeSqlBatch(addColumnSql, url, user, password);
    }

}
