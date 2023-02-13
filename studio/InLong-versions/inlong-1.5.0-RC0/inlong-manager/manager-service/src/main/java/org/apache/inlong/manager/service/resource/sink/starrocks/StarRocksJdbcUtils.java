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

package org.apache.inlong.manager.service.resource.sink.starrocks;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.hive.jdbc.HiveDatabaseMetaData;
import org.apache.inlong.manager.pojo.sink.starrocks.StarRocksColumnInfo;
import org.apache.inlong.manager.pojo.sink.starrocks.StarRocksTableInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StarRocksJdbcUtils {

    private static final String STAR_ROCKS_DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";
    private static final String METADATA_TYPE = "TABLE";
    private static final String COLUMN_LABEL = "TABLE_NAME";
    private static final String STAR_ROCKS_JDBC_PREFIX = "jdbc:mysql";

    private static final Logger LOGGER = LoggerFactory.getLogger(StarRocksJdbcUtils.class);

    /**
     * Get starRocks connection from starRocks url and user
     */
    public static Connection getConnection(String url, String user, String password) throws Exception {
        if (StringUtils.isBlank(url) || !url.startsWith(STAR_ROCKS_JDBC_PREFIX)) {
            throw new Exception("starRocks server url should start with " + STAR_ROCKS_JDBC_PREFIX);
        }
        Connection conn;
        try {
            Class.forName(STAR_ROCKS_DRIVER_CLASS);
            conn = DriverManager.getConnection(url, user, password);
            LOGGER.info("get star rocks connection success, url={}", url);
            return conn;
        } catch (Exception e) {
            String errMsg = "get star rocks connection error, please check starRocks jdbc url, username or password";
            LOGGER.error(errMsg, e);
            throw new Exception(errMsg + ", error: " + e.getMessage());
        }
    }

    /**
     * Execute sql on the specified starRocks Server
     *
     * @param sql need to execute
     * @param url url of starRocks server
     * @param user user of starRocks server
     * @param password password of starRocks server
     * @throws Exception when executing error
     */
    public static void executeSql(String sql, String url, String user, String password) throws Exception {
        try (Connection conn = getConnection(url, user, password);
                Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            LOGGER.info("execute sql [{}] success", sql);
        }
    }

    /**
     * Create StarRocks database
     */
    public static void createDb(String url, String username, String password, String dbName) throws Exception {
        String createDbSql = StarRocksSqlBuilder.buildCreateDbSql(dbName);
        executeSql(createDbSql, url, username, password);
    }

    /**
     * Create StarRocks table
     */
    public static void createTable(String url, String username, String password, StarRocksTableInfo tableInfo)
            throws Exception {
        if (checkTablesExist(url, username, password, tableInfo.getDbName(), tableInfo.getTableName())) {
            LOGGER.info("The table [{}] are exists", tableInfo.getTableName());
        } else {
            String createTableSql = StarRocksSqlBuilder.buildCreateTableSql(tableInfo);
            StarRocksJdbcUtils.executeSql(createTableSql, url, username, password);
            LOGGER.info("execute sql [{}] success", createTableSql);
        }
    }

    /**
     * Get Hive tables from the Hive metadata
     */
    public static List<String> getTables(String url, String user, String password, String dbName) throws Exception {
        try (Connection conn = getConnection(url, user, password)) {
            HiveDatabaseMetaData metaData = (HiveDatabaseMetaData) conn.getMetaData();
            ResultSet rs = metaData.getTables(dbName, dbName, null, new String[]{METADATA_TYPE});
            List<String> tables = new ArrayList<>();
            while (rs.next()) {
                String tableName = rs.getString(COLUMN_LABEL);
                tables.add(tableName);
            }
            rs.close();

            return tables;
        }
    }

    /**
     * Add columns for StarRocks table
     */
    public static void addColumns(String url, String user, String password, String dbName, String tableName,
            List<StarRocksColumnInfo> columnList) throws Exception {
        final List<StarRocksColumnInfo> columnInfos = Lists.newArrayList();

        for (StarRocksColumnInfo columnInfo : columnList) {
            if (!checkColumnExist(url, user, password, dbName, tableName, columnInfo.getFieldName())) {
                columnInfos.add(columnInfo);
            }
        }
        List<String> addColumnSql = StarRocksSqlBuilder.buildAddColumnsSql(dbName, tableName, columnInfos);
        try (Connection conn = getConnection(url, user, password)) {
            executeSqlBatch(conn, addColumnSql);
        }
    }

    /**
     * Check tables from the StarRocks information_schema.
     *
     * @param url jdbcUrl
     * @param user username
     * @param password password
     * @param dbName database name
     * @param tableName table name
     * @return true if table exist, otherwise false
     * @throws Exception on check table exist error
     */
    public static boolean checkTablesExist(String url, String user, String password, String dbName,
            String tableName) throws Exception {
        boolean result = false;
        final String checkTableSql = StarRocksSqlBuilder.getCheckTable(dbName, tableName);
        try (Connection conn = getConnection(url, user, password);
                Statement stmt = conn.createStatement()) {
            ResultSet resultSet = stmt.executeQuery(checkTableSql);
            if (Objects.nonNull(resultSet)) {
                if (resultSet.next()) {
                    result = true;
                }
            }
        }
        LOGGER.info("check table exist for db={} table={}, result={}", dbName, tableName, result);
        return result;
    }

    /**
     * Check whether the column exists in the StarRocks table.
     *
     * @param url jdbcUrl
     * @param user username
     * @param password password
     * @param dbName database name
     * @param tableName table name
     * @param column table column name
     * @return true if column exist in the table, otherwise false
     * @throws Exception on check column exist error
     */
    public static boolean checkColumnExist(String url, String user, String password, String dbName,
            final String tableName, final String column) throws Exception {
        boolean result = false;
        final String checkTableSql = StarRocksSqlBuilder.getCheckColumn(dbName, tableName, column);
        try (Connection conn = getConnection(url, user, password);
                Statement stmt = conn.createStatement();
                ResultSet resultSet = stmt.executeQuery(checkTableSql)) {
            if (Objects.nonNull(resultSet)) {
                if (resultSet.next()) {
                    result = true;
                }
            }
        }
        LOGGER.info("check column exist for db={} table={}, result={} column={}", dbName, tableName, result, column);
        return result;
    }

    /**
     * Execute batch query SQL on StarRocks.
     *
     * @param conn JDBC {@link Connection}
     * @param sqls SQL to be executed
     * @throws Exception on get execute SQL batch error
     */
    public static void executeSqlBatch(final Connection conn, final List<String> sqls) throws Exception {
        conn.setAutoCommit(false);
        try (Statement stmt = conn.createStatement()) {
            for (String entry : sqls) {
                stmt.execute(entry);
            }
            conn.commit();
            LOGGER.info("execute sql [{}] success", sqls);
        } finally {
            conn.setAutoCommit(true);
        }
    }

}
