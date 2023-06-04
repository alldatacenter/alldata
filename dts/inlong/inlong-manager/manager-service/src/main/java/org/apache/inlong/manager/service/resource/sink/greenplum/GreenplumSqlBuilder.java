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

package org.apache.inlong.manager.service.resource.sink.greenplum;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.pojo.sink.greenplum.GreenplumColumnInfo;
import org.apache.inlong.manager.pojo.sink.greenplum.GreenplumTableInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class GreenplumSqlBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(GreenplumSqlBuilder.class);

    /**
     * Build SQL to check whether the table exists.
     *
     * @param schemaName Greenplum schema name
     * @param tableName Greenplum table name
     * @return the check table SQL string
     */
    public static String getCheckTable(final String schemaName, final String tableName) {
        final StringBuilder sqlBuilder = new StringBuilder()
                .append("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES  WHERE TABLE_SCHEMA = '")
                .append(schemaName)
                .append("' AND TABLE_TYPE = 'BASE TABLE' ")
                .append(" AND TABLE_NAME = '")
                .append(tableName)
                .append("' ;");
        LOGGER.info("check table sql: {}", sqlBuilder);
        return sqlBuilder.toString();
    }

    /**
     * Build SQL to check whether the schema exists.
     *
     * @param schemaName
     * @return
     */
    public static String getCheckSchema(final String schemaName) {
        return new StringBuilder()
                .append("SELECT COUNT(1) FROM INFORMATION_SCHEMA.SCHEMATA ")
                .append(" WHERE SCHEMA_NAME = \'")
                .append(schemaName)
                .append("\';")
                .toString();
    }

    /**
     * Build create Greenplum schema SQL String
     *
     * @param schemaName schema name
     * @param user user name
     * @return SQL String
     */
    public static String buildCreateSchema(final String schemaName, final String user) {
        return new StringBuilder()
                .append(" CREATE SCHEMA \"")
                .append(schemaName)
                .append("\" AUTHORIZATION \"")
                .append(user)
                .append("\";")
                .toString();
    }

    /**
     * Build SQL to check whether the column exists.
     *
     * @param schemaName Greenplum table name
     * @param columnName Greenplum column name
     * @return the check column SQL string
     */
    public static String getCheckColumn(final String schemaName, final String tableName, final String columnName) {
        final StringBuilder sqlBuilder = new StringBuilder()
                .append("SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS  WHERE TABLE_SCHEMA = '")
                .append(schemaName)
                .append("' AND TABLE_NAME = '")
                .append(tableName)
                .append("' AND COLUMN_NAME = '")
                .append(columnName)
                .append("' ;");
        LOGGER.info("check table sql: {}", sqlBuilder);
        return sqlBuilder.toString();
    }

    /**
     * Build create table SQL by GreenplumTableInfo.
     *
     * @param table Greenplum table info {@link GreenplumTableInfo}
     * @return the create table SQL String
     */
    public static List<String> buildCreateTableSql(final GreenplumTableInfo table) {
        final List<String> sqls = Lists.newArrayList();
        final StringBuilder createSql = new StringBuilder()
                .append("CREATE TABLE ").append(table.getSchemaName())
                .append(".\"")
                .append(table.getTableName())
                .append("\"")
                .append(buildCreateColumnsSql(table));
        sqls.add(createSql.toString());

        // column comments
        sqls.addAll(getColumnsComment(table.getSchemaName(), table.getTableName(), table.getColumns()));
        // table comment
        if (StringUtils.isNotEmpty(table.getComment())) {
            sqls.add(getTableComment(table));
        }
        LOGGER.info("create table sql : {}", sqls);
        return sqls;
    }

    /**
     * Build add columns SQL.
     *
     * @param schemaName Greenplum schema name
     * @param tableName Greenplum table name
     * @param columnList Greenplum column list {@link List}
     * @return add column SQL string list
     */
    public static List<String> buildAddColumnsSql(final String schemaName, final String tableName,
            List<GreenplumColumnInfo> columnList) {
        final List<String> resultList = Lists.newArrayList();
        final StringBuilder sqlBuilder = new StringBuilder();

        columnList.forEach(columnInfo -> {
            sqlBuilder.append("ALTER TABLE \"")
                    .append(schemaName)
                    .append("\".\"")
                    .append(tableName)
                    .append("\" ADD \"")
                    .append(columnInfo.getName())
                    .append("\" ")
                    .append(columnInfo.getType())
                    .append(" ");
            resultList.add(sqlBuilder.toString());
            sqlBuilder.delete(0, sqlBuilder.length());
        });
        resultList.addAll(getColumnsComment(schemaName, tableName, columnList));
        LOGGER.info("add columns sql={}", resultList);
        return resultList;
    }

    /**
     * Build create column SQL.
     *
     * @param table Greenplum table info {@link GreenplumTableInfo}
     * @return create column SQL string
     */
    private static String buildCreateColumnsSql(final GreenplumTableInfo table) {
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
        return sql.toString();
    }

    /**
     * Build column info by GreenplumColumnInfo list.
     *
     * @param columns Greenplum column info {@link GreenplumColumnInfo} list
     * @return the SQL list
     */
    private static List<String> getColumnsInfo(final List<GreenplumColumnInfo> columns) {
        final List<String> columnList = new ArrayList<>();
        final StringBuilder columnBuilder = new StringBuilder();

        columns.forEach(columnInfo -> {
            columnBuilder.append("\"")
                    .append(columnInfo.getName())
                    .append("\" ")
                    .append(columnInfo.getType());
            columnList.add(columnBuilder.toString());
            columnBuilder.delete(0, columnBuilder.length());
        });
        return columnList;
    }

    /**
     * Build columns comment SQLs
     *
     * @param tableName Greenplum table name
     * @param columns Greenplum colum list {@link GreenplumColumnInfo}
     * @return the SQL String list
     */
    private static List<String> getColumnsComment(final String schemaName, final String tableName,
            List<GreenplumColumnInfo> columns) {
        final List<String> commentList = new ArrayList<>();
        for (GreenplumColumnInfo columnInfo : columns) {
            if (StringUtils.isNotBlank(columnInfo.getComment())) {
                StringBuilder commSql = new StringBuilder();
                commSql.append("COMMENT ON COLUMN \"")
                        .append(schemaName)
                        .append("\".\"")
                        .append(tableName)
                        .append("\".\"")
                        .append(columnInfo.getName())
                        .append("\" IS \'")
                        .append(columnInfo.getComment())
                        .append("\' ;");
                commentList.add(commSql.toString());
            }
        }
        return commentList;
    }

    /**
     * Build table comment SQL
     *
     * @param tableInfo Greenplum table info {@link GreenplumTableInfo}
     * @return the SQL String
     */
    private static String getTableComment(final GreenplumTableInfo tableInfo) {
        return new StringBuilder()
                .append("COMMENT ON TABLE \"")
                .append(tableInfo.getSchemaName())
                .append("\".\"")
                .append(tableInfo.getTableName())
                .append("\" IS \'")
                .append(tableInfo.getComment())
                .append("\';")
                .toString();
    }

    /**
     * Build query table's all cloumn SQL.
     *
     * @param schemaName Greenplum schema name
     * @param tableName Greenplum table name
     * @return desc table SQL string
     */
    public static String buildDescTableSql(final String schemaName, final String tableName) {
        StringBuilder sql = new StringBuilder().append(
                "SELECT A.COLUMN_NAME,A.UDT_NAME,C.DESCRIPTION FROM INFORMATION_SCHEMA.COLUMNS A")
                .append(" LEFT JOIN   (SELECT PC.OID AS OOID,PN.NSPNAME,PC.RELNAME")
                .append(" FROM PG_CLASS PC LEFT OUTER JOIN PG_NAMESPACE PN ON PC.RELNAMESPACE = PN.OID ")
                .append(" WHERE PN.NSPNAME ='")
                .append(schemaName)
                .append("' AND PC.RELNAME = '")
                .append(tableName)
                .append("') B   ON A.TABLE_SCHEMA = B.NSPNAME AND A.TABLE_NAME = B.RELNAME")
                .append(" LEFT JOIN PG_CATALOG.PG_DESCRIPTION C ")
                .append("ON B.OOID = C.OBJOID AND A.ORDINAL_POSITION = C.OBJSUBID")
                .append(" WHERE A.TABLE_SCHEMA = '")
                .append(schemaName)
                .append("' AND A.TABLE_NAME = '")
                .append(tableName)
                .append("'  ORDER BY  C.OBJSUBID ;");
        LOGGER.info("desc table sql={}", sql);
        return sql.toString();
    }
}
