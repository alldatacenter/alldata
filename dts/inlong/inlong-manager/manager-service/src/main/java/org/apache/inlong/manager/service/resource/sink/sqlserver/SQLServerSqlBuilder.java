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

package org.apache.inlong.manager.service.resource.sink.sqlserver;

import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.pojo.sink.sqlserver.SQLServerColumnInfo;
import org.apache.inlong.manager.pojo.sink.sqlserver.SQLServerTableInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Builder the SQL string for SQLServer.
 */
public class SQLServerSqlBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(SQLServerSqlBuilder.class);

    /**
     * Build SQL to check whether the table exists.
     *
     * @param schemaName SQLServer schema name
     * @param tableName SQLServer table name
     * @return the check table SQL string
     */
    public static String getCheckTable(final String schemaName, final String tableName) {
        final StringBuilder sqlBuilder = new StringBuilder()
                .append("SELECT COUNT(1) ")
                .append(" FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '")
                .append(schemaName)
                .append("' AND TABLE_NAME = '")
                .append(tableName)
                .append("' ;");
        LOGGER.info("check table sql: {}", sqlBuilder);
        return sqlBuilder.toString();
    }

    /**
     * Build SQL to check whether the column exists.
     *
     * @param schemaName SQLServer schema name
     * @param tableName SQLServer table name
     * @param columnName SQLServer column name
     * @return the check column SQL string
     */
    public static String getCheckColumn(final String schemaName, final String tableName, final String columnName) {
        final StringBuilder sqlBuilder = new StringBuilder()
                .append("SELECT COUNT(1) ")
                .append(" FROM  INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='")
                .append(schemaName)
                .append("' AND TABLE_NAME = '")
                .append(tableName)
                .append("' AND COLUMN_NAME = '")
                .append(columnName)
                .append("';");
        LOGGER.info("check table sql: {}", sqlBuilder);
        return sqlBuilder.toString();
    }

    /**
     * Build SQL to check whether the schema exists.
     *
     * @param schemaName
     * @return
     */
    public static String getCheckSchema(String schemaName) {
        final StringBuilder sqlBuilder = new StringBuilder()
                .append("SELECT COUNT(1) FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME ='")
                .append(schemaName)
                .append("';");
        LOGGER.info("check schema sql: {}", sqlBuilder);
        return sqlBuilder.toString();
    }

    /**
     * Build create schema SQL.
     *
     * @param schemaName SQLServer schema name
     * @return
     */
    public static String buildCreateSchemaSql(String schemaName) {
        return new StringBuilder()
                .append("CREATE SCHEMA \"")
                .append(schemaName)
                .append("\" AUTHORIZATION dbo ;")
                .toString();
    }

    /**
     * Build create table SQL by SQLServerTableInfo.
     *
     * @param table SQLServer table info {@link SQLServerTableInfo}
     * @return the create table SQL String
     */
    public static List<String> buildCreateTableSql(SQLServerTableInfo table) {
        final List<String> sqls = Lists.newArrayList();
        final StringBuilder sql = new StringBuilder()
                .append("CREATE TABLE ").append(table.getSchemaName())
                .append(".")
                .append(table.getTableName())
                .append(buildCreateColumnsSql(table));
        sqls.add(sql.toString());
        table.getColumns().stream()
                .filter(column -> StringUtils.isNotEmpty(column.getComment()))
                .forEach(column -> {
                    sqls.add(
                            buildAddColumnComment(table.getSchemaName(), table.getTableName(), column.getName(),
                                    column.getComment()));
                });
        LOGGER.info("create table sql: {}", sqls);
        return sqls;
    }

    /**
     * Build add columns SQL.
     *
     * @param schemaName SQLServer schema name
     * @param columnList SQLServer column list {@link List}
     * @return add column SQL string list
     */
    public static List<String> buildAddColumnsSql(final String schemaName, final String tableName,
            List<SQLServerColumnInfo> columnList) {
        final List<String> sqls = Lists.newArrayList();
        final List<String> columnInfoList = getColumnsInfo(columnList, null);
        final StringBuilder sqlBuilder = new StringBuilder();
        if (CollectionUtils.isNotEmpty(columnList)) {
            sqlBuilder.append("ALTER TABLE ")
                    .append(schemaName)
                    .append(".")
                    .append(tableName)
                    .append(" ADD ")
                    .append(String.join(",", columnInfoList))
                    .append(" ;");
        }
        sqls.add(sqlBuilder.toString());
        columnList.stream()
                .filter(column -> StringUtils.isNotEmpty(column.getComment()))
                .forEach(column -> {
                    sqls.add(
                            buildAddColumnComment(schemaName, tableName, column.getName(), column.getComment()));
                });
        LOGGER.info("add columns sql={}", sqls);
        return sqls;
    }

    /**
     * Build alter table add column comment SQL.
     *
     * @param schemaName SQLServer schema name
     * @param tableName SQLServer table name
     * @param columnName SQLServer column name
     * @param comment SQLServer column comment
     * @return SQL string
     */
    private static String buildAddColumnComment(final String schemaName, final String tableName,
            final String columnName,
            String comment) {
        return new StringBuilder()
                .append("EXEC sys.sp_addextendedproperty @name=N'MS_Description',")
                .append(" @value=N'")
                .append(columnName)
                .append("' , @level0type=N'SCHEMA',@level0name=N'")
                .append(schemaName)
                .append("', @level1type=N'TABLE',@level1name=N'")
                .append(tableName)
                .append("', @level2type=N'COLUMN',@level2name=N'")
                .append(comment)
                .append("'")
                .toString();
    }

    /**
     * Build create column SQL.
     *
     * @param table SQLServer table info {@link SQLServerTableInfo}
     * @return create column SQL string
     */
    private static String buildCreateColumnsSql(final SQLServerTableInfo table) {
        final StringBuilder sqlBuilder = new StringBuilder()
                .append(" (")
                .append(String.join(",", getColumnsInfo(table.getColumns(), table.getPrimaryKey())))
                .append(") ");
        LOGGER.info("create columns sql={}", sqlBuilder);
        return sqlBuilder.toString();
    }

    /**
     * Build column info by SQLServerColumnInfo list.
     *
     * @param columns SQLServer column info {@link SQLServerColumnInfo} list
     * @return the SQL list
     */
    private static List<String> getColumnsInfo(final List<SQLServerColumnInfo> columns, final String primaryKey) {
        final List<String> columnList = Lists.newArrayList();
        final StringBuilder columnBuilder = new StringBuilder();
        columns.forEach(columnInfo -> {
            columnBuilder.append("\"")
                    .append(columnInfo.getName())
                    .append("\"")
                    .append(" ")
                    .append(columnInfo.getType())
                    .append(" ");
            if (StringUtils.isNotEmpty(primaryKey) && columnInfo.getName().equals(primaryKey)) {
                columnBuilder.append("PRIMARY KEY ");
            }
            columnList.add(columnBuilder.toString());
            columnBuilder.delete(0, columnBuilder.length());
        });
        return columnList;
    }

    /**
     * Build query table SQL.
     *
     * @param schemaName SQLServer schema name
     * @param tableName SQLServer table name
     * @return desc table SQL string
     */
    public static String buildDescTableSql(final String schemaName, final String tableName) {
        final StringBuilder sql = new StringBuilder()
                .append(" SELECT C.COLUMN_NAME AS NAME,C.DATA_TYPE AS TYPE,CAST(D.VALUE AS VARCHAR) AS COMMENT FROM ")
                .append("(SELECT B.OBJECT_ID,A.TABLE_NAME,A.COLUMN_NAME,A.DATA_TYPE,A.ORDINAL_POSITION")
                .append(" FROM INFORMATION_SCHEMA.COLUMNS A LEFT JOIN SYS.TABLES B")
                .append(" ON A.TABLE_NAME = B.NAME")
                .append("  WHERE A.TABLE_NAME = '")
                .append(tableName)
                .append("'  AND A.TABLE_SCHEMA = '")
                .append(schemaName)
                .append("') C  LEFT JOIN SYS.EXTENDED_PROPERTIES D")
                .append(" ON C.OBJECT_ID = D.MAJOR_ID ")
                .append(" AND C.ORDINAL_POSITION = D.MINOR_ID ;");
        LOGGER.info("desc table sql={}", sql);
        return sql.toString();
    }
}
