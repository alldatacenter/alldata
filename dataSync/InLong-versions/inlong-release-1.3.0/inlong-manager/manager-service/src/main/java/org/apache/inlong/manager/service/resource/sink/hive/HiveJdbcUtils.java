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

package org.apache.inlong.manager.service.resource.sink.hive;

import org.apache.commons.lang3.StringUtils;
import org.apache.hive.jdbc.HiveDatabaseMetaData;
import org.apache.inlong.manager.pojo.sink.hive.HiveColumnInfo;
import org.apache.inlong.manager.pojo.sink.hive.HiveTableInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Utils for Hive JDBC.
 */
public class HiveJdbcUtils {

    private static final String HIVE_DRIVER_CLASS = "org.apache.hive.jdbc.HiveDriver";
    private static final String METADATA_TYPE = "TABLE";
    private static final String COLUMN_LABEL = "TABLE_NAME";
    private static final String HIVE_JDBC_PREFIX = "jdbc:hive2";

    private static final Logger LOGGER = LoggerFactory.getLogger(HiveJdbcUtils.class);

    /**
     * Get Hive connection from hive url and user
     */
    public static Connection getConnection(String url, String user, String password) throws Exception {
        if (StringUtils.isBlank(url) || !url.startsWith(HIVE_JDBC_PREFIX)) {
            throw new Exception("hive server url should start with " + HIVE_JDBC_PREFIX);
        }
        Connection conn;
        try {
            Class.forName(HIVE_DRIVER_CLASS);
            conn = DriverManager.getConnection(url, user, password);
            LOGGER.info("get hive connection success, url={}", url);
            return conn;
        } catch (Exception e) {
            String errMsg = "get hive connection error, please check hive jdbc url, username or password";
            LOGGER.error(errMsg, e);
            throw new Exception(errMsg + ", error: " + e.getMessage());
        }
    }

    /**
     * Execute sql on the specified Hive Server
     *
     * @param sql need to execute
     * @param url url of hive server
     * @param user user of hive server
     * @param password password of hive server
     * @throws Exception when executing error
     */
    public static void executeSql(String sql, String url, String user, String password) throws Exception {
        try (Connection conn = getConnection(url, user, password);
                Statement stmt = conn.createStatement()) {

            stmt.execute(sql);
            LOGGER.info("execute sql [{}] success for url: {}", sql, url);
        }
    }

    /**
     * Create Hive database
     */
    public static void createDb(String url, String user, String password, String dbName) throws Exception {
        String createDbSql = SqlBuilder.buildCreateDbSql(dbName);
        executeSql(createDbSql, url, user, password);
    }

    /**
     * Create Hive table
     */
    public static void createTable(String url, String user, String password, HiveTableInfo tableInfo) throws Exception {
        String createTableSql = SqlBuilder.buildCreateTableSql(tableInfo);
        HiveJdbcUtils.executeSql(createTableSql, url, user, password);
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
     * Query Hive columns
     */
    public static List<HiveColumnInfo> getColumns(String url, String user, String password, String dbName,
            String tableName) throws Exception {

        String querySql = SqlBuilder.buildDescTableSql(dbName, tableName);
        try (Connection conn = getConnection(url, user, password);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(querySql)) {
            List<HiveColumnInfo> columnList = new ArrayList<>();
            while (rs.next()) {
                HiveColumnInfo columnInfo = new HiveColumnInfo();
                columnInfo.setName(rs.getString(1));
                columnInfo.setType(rs.getString(2));
                columnInfo.setDesc(rs.getString(3));
                columnList.add(columnInfo);
            }
            return columnList;
        }
    }

    /**
     * Add columns for Hive table
     */
    public static void addColumns(String url, String user, String password, String dbName, String tableName,
            List<HiveColumnInfo> columnList) throws Exception {
        String addColumnSql = SqlBuilder.buildAddColumnSql(dbName, tableName, columnList);
        HiveJdbcUtils.executeSql(addColumnSql, url, user, password);
    }

}
