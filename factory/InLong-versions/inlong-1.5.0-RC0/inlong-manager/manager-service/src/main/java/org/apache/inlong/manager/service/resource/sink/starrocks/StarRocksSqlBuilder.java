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

import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.pojo.sink.starrocks.StarRocksColumnInfo;
import org.apache.inlong.manager.pojo.sink.starrocks.StarRocksTableInfo;
import org.apache.inlong.manager.service.resource.sink.hive.SqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builder for SQL string
 */
public class StarRocksSqlBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlBuilder.class);

    /**
     * Build create database SQL
     */
    public static String buildCreateDbSql(String dbName) {
        // Support _ beginning with underscore
        String sql = "CREATE DATABASE IF NOT EXISTS `" + dbName + "`";
        LOGGER.info("create db sql: {}", sql);
        return sql;
    }

    /**
     * Build create table SQL
     */
    public static String buildCreateTableSql(StarRocksTableInfo table) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(table.getTableName());
        // Construct columns and partition columns
        sql.append(getColumnsAndComments(table));
        if (!StringUtils.isEmpty(table.getPrimaryKey())) {
            sql.append(", PRIMARY KEY (")
                    .append(table.getPrimaryKey())
                    .append(")");
        }
        if (!Objects.isNull(table.getReplicationNum())) {
            sql.append(" PROPERTIES ( \"replication_num\" = \"")
                    .append(table.getReplicationNum())
                    .append("\")");
        }

        LOGGER.info("create table sql: {}", sql);
        return sql.toString();
    }

    /**
     * Build query table SQL
     */
    public static String buildDescTableSql(String dbName, String tableName) {
        StringBuilder sql = new StringBuilder();
        String fullTableName = "`" + dbName + "." + tableName + "`";
        sql.append("DESC ").append(fullTableName);

        LOGGER.info("desc table sql={}", sql);
        return sql.toString();
    }

    /**
     * Build add columns SQL.
     *
     * @param tableName StarRocks table name
     * @param columnList StarRocks column list {@link List}
     * @return add column SQL string list
     */
    public static List<String> buildAddColumnsSql(String dbName, String tableName,
            List<StarRocksColumnInfo> columnList) {
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
     * Build column info by StarRocksColumnInfo list.
     *
     * @param columns StarRocks column info {@link StarRocksColumnInfo} list
     * @return the SQL list
     */
    private static List<String> getColumnsInfo(List<StarRocksColumnInfo> columns) {
        final List<String> columnList = new ArrayList<>();
        final StringBuilder columnBuilder = new StringBuilder();
        columns.forEach(columnInfo -> {
            columnBuilder.append("`")
                    .append(columnInfo.getFieldName())
                    .append("`")
                    .append(" ")
                    .append(columnInfo.getFieldType());
            if (!StringUtils.isEmpty(columnInfo.getFieldComment())) {
                columnBuilder.append(" COMMENT '")
                        .append(columnInfo.getFieldComment())
                        .append("'");
            }
            columnBuilder.append(" ");
            columnList.add(columnBuilder.toString());
            columnBuilder.delete(0, columnBuilder.length());
        });
        return columnList;
    }

    /**
     * Get columns and table comment string for create table SQL.
     *
     * For example: col_name data_type [COMMENT col_comment], col_name data_type [COMMENT col_comment]....
     */
    private static String getColumnsAndComments(StarRocksTableInfo tableInfo) {
        List<StarRocksColumnInfo> columns = tableInfo.getColumns();
        List<String> columnList = new ArrayList<>();
        List<String> sortKeyList = new ArrayList<>();
        List<String> distributeList = new ArrayList<>();
        for (StarRocksColumnInfo columnInfo : columns) {
            // Construct columns and partition columns
            StringBuilder columnStr = new StringBuilder().append("`").append(columnInfo.getFieldName()).append("` ")
                    .append(columnInfo.getFieldType());
            if (StringUtils.isNotEmpty(columnInfo.getFieldComment())) {
                columnStr.append(" COMMENT ").append("'").append(columnInfo.getFieldComment()).append("'");
            }

            if (columnInfo.getIsDistributed()) {
                distributeList.add("`" + columnInfo.getFieldName() + "` ");
            }
            if (columnInfo.getIsSortKey()) {
                sortKeyList.add("`" + columnInfo.getFieldName() + "` ");
            }
            columnList.add(columnStr.toString());
        }
        StringBuilder result = new StringBuilder().append(" (").append(StringUtils.join(columnList, ",")).append(") ");
        // set partitions
        if (sortKeyList.size() > 0) {
            result.append("DUPLICATE KEY (").append(StringUtils.join(sortKeyList, ",")).append(") ");
        }
        if (distributeList.size() <= 0 && columns.size() > 0) {
            distributeList.add("`" + columns.get(0).getFieldName() + "` ");
        }
        result.append("DISTRIBUTED BY HASH (").append(StringUtils.join(distributeList, ",")).append(") ")
                .append("BUCKETS ")
                .append(tableInfo.getBarrelSize());
        return result.toString();
    }

    /**
     * Build SQL to check whether the table exists.
     *
     * @param dbName StarRocks database name
     * @param tableName StarRocks table name
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
     * @param dbName StarRocks database name
     * @param tableName StarRocks table name
     * @param columnName StarRocks column name
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

}
