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

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for ClickHouse SQL string
 */
public class ClickHouseSqlBuilder {

    private static final int FIRST_COLUMN_INDEX = 0;

    private static final Logger LOGGER = LoggerFactory.getLogger(ClickHouseSqlBuilder.class);

    /**
     * Build create database SQL
     */
    public static String buildCreateDbSql(String dbName) {
        // Support _ beginning with underscore
        String sql = "CREATE DATABASE IF NOT EXISTS " + dbName;
        LOGGER.info("create db sql: {}", sql);
        return sql;
    }

    /**
     * Build create table SQL
     */
    public static String buildCreateTableSql(ClickHouseTableInfo table) {
        StringBuilder sql = new StringBuilder();
        // Support _ beginning with underscore
        String dbTableName = table.getDbName() + "." + table.getTableName();
        sql.append("CREATE TABLE ").append(dbTableName);

        // Construct columns and partition columns
        sql.append(buildCreateColumnsSql(table.getColumns()));
        if (StringUtils.isNotEmpty(table.getEngine())) {
            sql.append(" ENGINE = ").append(table.getEngine());
        } else {
            sql.append(" ENGINE = MergeTree()");
        }
        if (StringUtils.isNotEmpty(table.getOrderBy())) {
            sql.append(" ORDER BY ").append(table.getOrderBy());
        } else if (StringUtils.isEmpty(table.getEngine())) {
            sql.append(" ORDER BY ").append(table.getColumns()
                    .get(FIRST_COLUMN_INDEX).getName());
        }
        if (StringUtils.isNotEmpty(table.getPartitionBy())) {
            sql.append(" PARTITION BY ").append(table.getPartitionBy());
        }
        if (StringUtils.isNotEmpty(table.getPrimaryKey())) {
            sql.append(" PRIMARY KEY ").append(table.getPrimaryKey());
        }
        if (StringUtils.isNotEmpty(table.getTableDesc())) {
            sql.append(" COMMENT '").append(table.getTableDesc()).append("'");
        }

        LOGGER.info("create table sql: {}", sql);
        return sql.toString();
    }

    /**
     * Build add column SQL
     */
    public static List<String> buildAddColumnsSql(String dbName, String tableName,
            List<ClickHouseColumnInfo> columnList) {
        String dbTableName = dbName + "." + tableName;
        List<String> columnInfoList = getColumnsInfo(columnList);
        List<String> resultList = new ArrayList<>();
        for (String columnInfo : columnInfoList) {
            StringBuilder sql = new StringBuilder().append("ALTER TABLE ")
                    .append(dbTableName).append(" ADD COLUMN ").append(columnInfo);
            resultList.add(sql.toString());
        }
        return resultList;
    }

    /**
     * Build create column SQL
     */
    private static String buildCreateColumnsSql(List<ClickHouseColumnInfo> columns) {
        List<String> columnList = getColumnsInfo(columns);
        StringBuilder result = new StringBuilder().append(" (")
                .append(StringUtils.join(columnList, ",")).append(") ");
        return result.toString();
    }

    /**
     * Build column info
     */
    private static List<String> getColumnsInfo(List<ClickHouseColumnInfo> columns) {
        List<String> columnList = new ArrayList<>();
        for (ClickHouseColumnInfo columnInfo : columns) {
            // Construct columns and partition columns
            StringBuilder columnStr = new StringBuilder().append(columnInfo.getName())
                    .append(" ").append(columnInfo.getType());
            if (StringUtils.isNotEmpty(columnInfo.getDefaultType())) {
                columnStr.append(" ").append(columnInfo.getDefaultType())
                        .append(" ").append(columnInfo.getDefaultExpr());
            }
            if (StringUtils.isNotEmpty(columnInfo.getCompressionCode())) {
                columnStr.append(" CODEC(").append(columnInfo.getDesc()).append(")");
            }
            if (StringUtils.isNotEmpty(columnInfo.getTtlExpr())) {
                columnStr.append(" TTL ").append(columnInfo.getTtlExpr());
            }
            if (StringUtils.isNotEmpty(columnInfo.getDesc())) {
                columnStr.append(" COMMENT '").append(columnInfo.getDesc()).append("'");
            }
            columnList.add(columnStr.toString());
        }
        return columnList;
    }

    /**
     * Build query table SQL
     */
    public static String buildDescTableSql(String dbName, String tableName) {
        StringBuilder sql = new StringBuilder();
        String fullTableName = dbName + "." + tableName;
        sql.append("DESC ").append(fullTableName);

        LOGGER.info("desc table sql={}", sql);
        return sql.toString();
    }
}
