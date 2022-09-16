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

package org.apache.inlong.manager.service.resource.sink.ck;

import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.pojo.sink.ck.ClickHouseColumnInfo;
import org.apache.inlong.manager.pojo.sink.ck.ClickHouseTableInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.clickhouse.ClickHouseDatabaseMetadata;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Utils for ClickHouse JDBC.
 */
public class ClickHouseJdbcUtils {

    private static final String CLICKHOUSE_DRIVER_CLASS = "ru.yandex.clickhouse.ClickHouseDriver";
    private static final String METADATA_TYPE = "TABLE";
    private static final String COLUMN_LABEL = "TABLE_NAME";
    private static final String CLICKHOUSE_JDBC_PREFIX = "jdbc:clickhouse";

    private static final Logger LOG = LoggerFactory.getLogger(ClickHouseJdbcUtils.class);

    /**
     * Get ClickHouse connection from clickhouse url and user
     */
    public static Connection getConnection(String url, String user, String password) throws Exception {
        if (StringUtils.isBlank(url) || !url.startsWith(CLICKHOUSE_JDBC_PREFIX)) {
            throw new Exception("ClickHouse server URL was invalid, it should start with jdbc:clickhouse");
        }
        Connection conn;
        try {
            Class.forName(CLICKHOUSE_DRIVER_CLASS);
            conn = DriverManager.getConnection(url, user, password);
        } catch (Exception e) {
            LOG.error("get clickhouse connection error, please check clickhouse jdbc url, username or password", e);
            throw new Exception("get clickhouse connection error, please check jdbc url, username or password. "
                    + "other error msg: " + e.getMessage());
        }

        if (conn == null) {
            throw new Exception("get clickhouse connection failed, please contact administrator");
        }

        LOG.info("get clickhouse connection success, url={}", url);
        return conn;
    }

    /**
     * Execute One ClickHouse Sql command
     */
    public static void executeSql(String sql, String url, String user, String password) throws Exception {
        try (Connection conn = getConnection(url, user, password)) {
            Statement stmt = conn.createStatement();
            stmt.execute(sql);
            LOG.info("execute sql [{}] success for url: {}", sql, url);
        }
    }

    /**
     * Execute Batch ClickHouse Sql commands
     */
    public static void executeSqlBatch(List<String> sql, String url, String user, String password) throws Exception {
        try (Connection conn = getConnection(url, user, password)) {
            Statement stmt = conn.createStatement();
            for (String entry : sql) {
                stmt.execute(entry);
            }
            LOG.info("execute sql [{}] success for url: {}", sql, url);
        }
    }

    /**
     * Create ClickHouse database
     */
    public static void createDb(String url, String user, String password, String dbName) throws Exception {
        String createDbSql = ClickHouseSqlBuilder.buildCreateDbSql(dbName);
        executeSql(createDbSql, url, user, password);
    }

    /**
     * Create ClickHouse table
     */
    public static void createTable(String url, String user, String password,
            ClickHouseTableInfo tableInfo) throws Exception {
        String createTableSql = ClickHouseSqlBuilder.buildCreateTableSql(tableInfo);
        ClickHouseJdbcUtils.executeSql(createTableSql, url, user, password);
    }

    /**
     * Get ClickHouse tables from the ClickHouse metadata
     */
    public static List<String> getTables(String url, String user, String password, String dbname) throws Exception {
        List<String> tables = new ArrayList<>();
        try (Connection conn = getConnection(url, user, password)) {
            ClickHouseDatabaseMetadata metaData = (ClickHouseDatabaseMetadata) conn.getMetaData();
            LOG.info("dbname is {}", dbname);
            ResultSet rs = metaData.getTables(dbname, dbname, null, new String[]{"TABLE"});
            while (rs.next()) {
                String tableName = rs.getString(COLUMN_LABEL);
                tables.add(tableName);
            }
            rs.close();
        }
        return tables;
    }

    /**
     * Query ClickHouse columns
     */
    public static List<ClickHouseColumnInfo> getColumns(String url, String user, String password, String dbName,
            String tableName) throws Exception {

        String querySql = ClickHouseSqlBuilder.buildDescTableSql(dbName, tableName);
        try (Connection conn = getConnection(url, user, password);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(querySql)) {
            List<ClickHouseColumnInfo> columnList = new ArrayList<>();
            while (rs.next()) {
                ClickHouseColumnInfo columnInfo = new ClickHouseColumnInfo();
                columnInfo.setName(rs.getString(1));
                columnInfo.setType(rs.getString(2));
                columnInfo.setDefaultType(rs.getString(3));
                columnInfo.setDefaultExpr(rs.getString(4));
                columnInfo.setDesc(rs.getString(5));
                columnInfo.setCompressionCode(rs.getString(6));
                columnInfo.setTtlExpr(rs.getString(7));
                columnList.add(columnInfo);
            }
            return columnList;
        }
    }

    /**
     * Add columns for ClickHouse table
     */
    public static void addColumns(String url, String user, String password, String dbName, String tableName,
            List<ClickHouseColumnInfo> columnList) throws Exception {
        List<String> addColumnSql = ClickHouseSqlBuilder.buildAddColumnsSql(dbName, tableName, columnList);
        ClickHouseJdbcUtils.executeSqlBatch(addColumnSql, url, user, password);
    }

}
