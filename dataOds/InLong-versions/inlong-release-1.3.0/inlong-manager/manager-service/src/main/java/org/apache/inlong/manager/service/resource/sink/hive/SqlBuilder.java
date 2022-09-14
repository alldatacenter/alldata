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
import org.apache.inlong.manager.pojo.sink.hive.HiveColumnInfo;
import org.apache.inlong.manager.pojo.sink.hive.HiveTableInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for SQL string
 */
public class SqlBuilder {

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
    public static String buildCreateTableSql(HiveTableInfo table) {
        StringBuilder sql = new StringBuilder();
        // Support _ beginning with underscore
        String dbTableName = "`" + table.getDbName() + "." + table.getTableName() + "`";
        sql.append("CREATE TABLE ").append(dbTableName);

        // Construct columns and partition columns
        sql.append(getColumnsAndComments(table.getColumns(), table.getTableDesc()));
        if (table.getFieldTerSymbol() != null) {
            sql.append(" ROW FORMAT DELIMITED FIELDS TERMINATED BY '").append(table.getFieldTerSymbol()).append("'");
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
     * Build add column SQL
     */
    public static String buildAddColumnSql(String dbName, String tableName, List<HiveColumnInfo> columnList) {
        StringBuilder sql = new StringBuilder();
        // Support _ beginning with underscore
        String dbTableName = "`" + dbName + "." + tableName + "`";
        sql.append("ALTER TABLE ").append(dbTableName);

        // add columns
        List<String> columnInfoList = new ArrayList<>();
        for (HiveColumnInfo columnInfo : columnList) {
            if (columnInfo.isPartition()) {
                continue;
            }
            // Support _ beginning with underscore
            StringBuilder columnStr = new StringBuilder().append("`").append(columnInfo.getName()).append("` ")
                    .append(columnInfo.getType());
            if (StringUtils.isNotEmpty(columnInfo.getDesc())) { // comment is not empty
                columnStr.append(" COMMENT ").append("'").append(columnInfo.getDesc()).append("'");
            }
            columnInfoList.add(columnStr.toString());
        }
        sql.append(" ADD COLUMNS (").append(StringUtils.join(columnInfoList, ",")).append(") ");

        LOGGER.info("add columns sql={}", sql);
        return sql.toString();
    }

    /**
     * Get columns and table comment string for create table SQL.
     *
     * For example: col_name data_type [COMMENT col_comment], col_name data_type [COMMENT col_comment]....
     */
    private static String getColumnsAndComments(List<HiveColumnInfo> columns, String tableComment) {
        List<String> columnList = new ArrayList<>();
        List<String> partitionList = new ArrayList<>();
        for (HiveColumnInfo columnInfo : columns) {
            // Construct columns and partition columns
            StringBuilder columnStr = new StringBuilder().append("`").append(columnInfo.getName()).append("` ")
                    .append(columnInfo.getType());
            if (StringUtils.isNotEmpty(columnInfo.getDesc())) {
                columnStr.append(" COMMENT ").append("'").append(columnInfo.getDesc()).append("'");
            }

            if (columnInfo.isPartition()) {
                partitionList.add(columnStr.toString());
            } else {
                columnList.add(columnStr.toString());
            }
        }
        StringBuilder result = new StringBuilder().append(" (").append(StringUtils.join(columnList, ",")).append(") ");

        // set table comment
        if (StringUtils.isNotEmpty(tableComment)) {
            result.append("COMMENT ").append("'").append(tableComment).append("' ");
        }
        // set partitions
        if (partitionList.size() > 0) {
            result.append("PARTITIONED BY (").append(StringUtils.join(partitionList, ",")).append(") ");
        }

        return result.toString();
    }

}
