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

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for Postgres SQL string
 */
public class PostgresSqlBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresSqlBuilder.class);

    /**
     * Build check database exists SQL
     */
    public static String getCheckDatabase(String dbName) {
        String sql = "SELECT datname FROM from pg_catalog.pg_database WHERE datname = '" + dbName + "'";
        LOGGER.info("check database sql: {}", sql);
        return sql;
    }

    /**
     * Build create database SQL
     */
    public static String buildCreateDbSql(String dbName) {
        String sql = "CREATE DATABASE " + dbName;
        LOGGER.info("create db sql: {}", sql);
        return sql;
    }

    /**
     * Build create table SQL
     */
    public static String buildCreateTableSql(PostgresTableInfo table) {
        StringBuilder sql = new StringBuilder();
        // Support _ beginning with underscore
        String dbTableName = table.getTableName();
        sql.append("CREATE TABLE ").append(dbTableName);

        // Construct columns and partition columns
        sql.append(buildCreateColumnsSql(table.getColumns()));

        LOGGER.info("create table sql: {}", sql);
        return sql.toString();
    }

    /**
     * Build add column SQL
     */
    public static List<String> buildAddColumnsSql(String tableName, List<PostgresColumnInfo> columnList) {
        List<String> columnInfoList = getColumnsInfo(columnList);
        List<String> resultList = new ArrayList<>();
        for (String columnInfo : columnInfoList) {
            String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnInfo;
            resultList.add(sql);
        }

        LOGGER.info("add columns sql={}", resultList);
        return resultList;
    }

    /**
     * Build create column SQL
     */
    private static String buildCreateColumnsSql(List<PostgresColumnInfo> columns) {
        List<String> columnList = getColumnsInfo(columns);
        String sql = " (" + StringUtils.join(columnList, ",") + ") ";
        LOGGER.info("create columns sql={}", sql);
        return sql;
    }

    /**
     * Build column info
     */
    private static List<String> getColumnsInfo(List<PostgresColumnInfo> columns) {
        List<String> columnList = new ArrayList<>();
        for (PostgresColumnInfo columnInfo : columns) {
            // Construct columns and partition columns
            String columnStr = columnInfo.getName() + " " + columnInfo.getType();
            columnList.add(columnStr);
        }
        return columnList;
    }

    /**
     * Build query table SQL
     */
    public static String buildDescTableSql(String tableName) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT att.attname as filedName, format_type(att.atttypid, att.atttypmod) as filedType"
                        + " FROM pg_attribute as att, pg_class as clz"
                        + " WHERE att.attrelid = clz.oid and att.attnum > 0 and clz.relname = '")
                .append(tableName)
                .append("';");

        LOGGER.info("desc table sql={}", sql);
        return sql.toString();
    }

}
