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

import org.apache.inlong.manager.pojo.sink.ck.ClickHouseFieldInfo;
import org.apache.inlong.manager.pojo.sink.ck.ClickHouseTableInfo;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        if (StringUtils.isNotBlank(table.getCluster())) {
            sql.append(" ON CLUSTER ").append(table.getCluster());
        }
        // add ttl columns
        if (table.getTtl() != null && StringUtils.isNotBlank(table.getTtlUnit())) {
            ClickHouseFieldInfo clickHouseFieldInfo = new ClickHouseFieldInfo();
            clickHouseFieldInfo.setFieldName("inlong_ttl_date_time");
            clickHouseFieldInfo.setFieldType("DateTime");
            clickHouseFieldInfo.setFieldComment("inlong ttl date time");
            clickHouseFieldInfo.setDefaultType("DEFAULT");
            clickHouseFieldInfo.setDefaultExpr("now()");
            table.getFieldInfoList().add(clickHouseFieldInfo);
        }
        // Construct columns and partition columns
        sql.append(buildCreateColumnsSql(table.getFieldInfoList()));
        if (StringUtils.isNotBlank(table.getEngine()) && Objects.equals("ReplicatedMergeTree", table.getEngine())) {
            sql.append(
                    " ENGINE = ReplicatedMergeTree('/clickhouse/tables/{shard}/{database}/{table}/data', '{replica}')");
        } else if (StringUtils.isNotBlank(table.getEngine())) {
            sql.append(" ENGINE = ").append(table.getEngine());
        } else {
            sql.append(" ENGINE = MergeTree()");
        }
        if (StringUtils.isNotEmpty(table.getOrderBy())) {
            sql.append(" ORDER BY ").append(table.getOrderBy());
        } else if (StringUtils.isEmpty(table.getEngine())) {
            sql.append(" ORDER BY ").append(table.getFieldInfoList()
                    .get(FIRST_COLUMN_INDEX).getFieldName());
        }
        if (table.getTtl() != null && StringUtils.isNotBlank(table.getTtlUnit())) {
            sql.append(" TTL ").append("inlong_ttl_date_time").append(" + INTERVAL ").append(table.getTtl()).append(" ")
                    .append(table.getTtlUnit());
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
            List<ClickHouseFieldInfo> columnList) {
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
    private static String buildCreateColumnsSql(List<ClickHouseFieldInfo> columns) {
        List<String> columnList = getColumnsInfo(columns);
        StringBuilder result = new StringBuilder().append(" (")
                .append(StringUtils.join(columnList, ",")).append(") ");
        return result.toString();
    }

    /**
     * Build column info
     */
    private static List<String> getColumnsInfo(List<ClickHouseFieldInfo> columns) {
        List<String> columnList = new ArrayList<>();
        for (ClickHouseFieldInfo columnInfo : columns) {
            // Construct columns and partition columns
            StringBuilder columnStr = new StringBuilder().append(columnInfo.getFieldName())
                    .append(" ").append(columnInfo.getFieldType());
            if (StringUtils.isNotEmpty(columnInfo.getDefaultType())) {
                columnStr.append(" ").append(columnInfo.getDefaultType())
                        .append(" ").append(columnInfo.getDefaultExpr());
            }
            if (StringUtils.isNotEmpty(columnInfo.getCompressionCode())) {
                columnStr.append(" CODEC(").append(columnInfo.getCompressionCode()).append(")");
            }
            if (StringUtils.isNotEmpty(columnInfo.getTtlExpr())) {
                columnStr.append(" TTL ").append(columnInfo.getTtlExpr());
            }
            if (StringUtils.isNotEmpty(columnInfo.getFieldComment())) {
                columnStr.append(" COMMENT '").append(columnInfo.getFieldComment()).append("'");
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
