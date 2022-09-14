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

package org.apache.inlong.manager.service.resource.sink.mysql;

import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.pojo.sink.mysql.MySQLColumnInfo;
import org.apache.inlong.manager.pojo.sink.mysql.MySQLTableInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder the SQL string for MySQL
 */
public class MySQLSqlBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(MySQLSqlBuilder.class);

    /**
     * Build SQL to check whether the database exists.
     *
     * @param dbName MySQL database name
     * @return the check database SQL string
     */
    public static String getCheckDatabase(String dbName) {
        final StringBuilder sqlBuilder = new StringBuilder()
                .append("SELECT schema_name ")
                .append(" FROM information_schema.schemata ")
                .append("WHERE schema_name = '")
                .append(dbName)
                .append("';");
        LOGGER.info("check database sql: {}", sqlBuilder);
        return sqlBuilder.toString();
    }

    /**
     * Build SQL to check whether the table exists.
     *
     * @param dbName MySQL database name
     * @param tableName MySQL table name
     * @return the check table SQL string
     */
    public static String getCheckTable(String dbName, String tableName) {
        final StringBuilder sqlBuilder = new StringBuilder()
                .append("select table_schema,table_name ")
                .append(" from information_schema.tables where table_schema = '")
                .append(dbName)
                .append("' and table_name = '")
                .append(tableName)
                .append("' ;");
        LOGGER.info("check table sql: {}", sqlBuilder);
        return sqlBuilder.toString();
    }

    /**
     * Build SQL to check whether the column exists.
     *
     * @param dbName MySQL database name
     * @param tableName MySQL table name
     * @param columnName MySQL column name
     * @return the check column SQL string
     */
    public static String getCheckColumn(String dbName, String tableName, String columnName) {
        final StringBuilder sqlBuilder = new StringBuilder()
                .append("SELECT COLUMN_NAME,COLUMN_TYPE,COLUMN_COMMENT ")
                .append(" from  information_schema.COLUMNS where table_schema='")
                .append(dbName)
                .append("' and table_name = '")
                .append(tableName)
                .append("' and column_name = '")
                .append(columnName)
                .append("';");
        LOGGER.info("check table sql: {}", sqlBuilder);
        return sqlBuilder.toString();
    }

    /**
     * Build create database SQL.
     *
     * @param dbName MySQL database name
     * @return the create database SQL string
     */
    public static String buildCreateDbSql(String dbName) {
        final String sql = "CREATE DATABASE " + dbName;
        LOGGER.info("create db sql: {}", sql);
        return sql;
    }

    /**
     * Build create table SQL by MySQLTableInfo.
     *
     * @param table MySQL table info {@link MySQLTableInfo}
     * @return the create table SQL String
     */
    public static String buildCreateTableSql(MySQLTableInfo table) {
        final StringBuilder sql = new StringBuilder()
                .append("CREATE TABLE ").append(table.getDbName())
                .append(".")
                .append(table.getTableName())
                .append(buildCreateColumnsSql(table));

        if (StringUtils.isEmpty(table.getEngine())) {
            sql.append(" ENGINE=InnoDB ");
        } else {
            sql.append(" ENGINE=")
                    .append(table.getEngine())
                    .append(" ");
        }

        if (!StringUtils.isEmpty(table.getCharset())) {
            sql.append(" DEFAULT CHARSET=")
                    .append(table.getCharset())
                    .append(" ");
        }

        LOGGER.info("create table sql: {}", sql);
        return sql.toString();
    }

    /**
     * Build add columns SQL.
     *
     * @param tableName MySQL table name
     * @param columnList MySQL column list {@link List}
     * @return add column SQL string list
     */
    public static List<String> buildAddColumnsSql(String dbName, String tableName,
            List<MySQLColumnInfo> columnList) {
        final List<String> columnInfoList = getColumnsInfo(columnList);
        final List<String> resultList = new ArrayList<>();
        final StringBuilder sqlBuilder = new StringBuilder();
        columnInfoList.forEach(columnInfo -> {
            sqlBuilder.append("ALTER TABLE ")
                    .append(dbName)
                    .append(".")
                    .append(tableName)
                    .append(" ADD COLUMN ")
                    .append(columnInfo)
                    .append(";");
            resultList.add(sqlBuilder.toString());
            sqlBuilder.delete(0, sqlBuilder.length());
        });
        return resultList;
    }

    /**
     * Build create column SQL.
     *
     * @param table MySQL table info {@link MySQLTableInfo}
     * @return create column SQL string
     */
    private static String buildCreateColumnsSql(MySQLTableInfo table) {
        final List<String> columnList = getColumnsInfo(table.getColumns());
        final StringBuilder sql = new StringBuilder()
                .append(" (")
                .append(StringUtils.join(columnList, ","));
        if (!StringUtils.isEmpty(table.getPrimaryKey())) {
            sql.append(", PRIMARY KEY (")
                    .append(table.getPrimaryKey())
                    .append(")");
        }
        sql.append(") ");
        LOGGER.info("create columns sql={}", sql);
        return sql.toString();
    }

    /**
     * Build column info by MySQLColumnInfo list.
     *
     * @param columns MySQL column info {@link MySQLColumnInfo} list
     * @return the SQL list
     */
    private static List<String> getColumnsInfo(List<MySQLColumnInfo> columns) {
        final List<String> columnList = new ArrayList<>();
        final StringBuilder columnBuilder = new StringBuilder();
        columns.forEach(columnInfo -> {
            columnBuilder.append("`")
                    .append(columnInfo.getName())
                    .append("`")
                    .append(" ")
                    .append(columnInfo.getType());
            if (!StringUtils.isEmpty(columnInfo.getComment())) {
                columnBuilder.append(" COMMENT '")
                        .append(columnInfo.getComment())
                        .append("'");
            }
            columnBuilder.append(" ");
            columnList.add(columnBuilder.toString());
            columnBuilder.delete(0, columnBuilder.length());
        });
        return columnList;
    }

    /**
     * Build query table SQL.
     *
     * @param dbName MySQL database name
     * @param tableName MySQL table name
     * @return desc table SQL string
     */
    public static String buildDescTableSql(String dbName, String tableName) {
        final StringBuilder sql = new StringBuilder()
                .append("SELECT COLUMN_NAME,COLUMN_TYPE,COLUMN_COMMENT ")
                .append(" from  information_schema.COLUMNS where table_schema='")
                .append(dbName)
                .append("' and table_name = '")
                .append(tableName)
                .append("';");
        LOGGER.info("desc table sql={}", sql);
        return sql.toString();
    }

}
