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

package org.apache.inlong.manager.service.resource.sink.oracle;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.pojo.sink.oracle.OracleColumnInfo;
import org.apache.inlong.manager.pojo.sink.oracle.OracleTableInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class OracleSqlBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(OracleSqlBuilder.class);

    /**
     * Build SQL to check whether the table exists.
     *
     * @param userName Oracle database name
     * @param tableName Oracle table name
     * @return the check table SQL string
     */
    public static String getCheckTable(final String userName, final String tableName) {
        final StringBuilder sqlBuilder = new StringBuilder()
                .append("SELECT COUNT(*) FROM ALL_TABLES WHERE OWNER = UPPER('")
                .append(userName)
                .append("') ")
                .append("AND TABLE_NAME = '")
                .append(tableName)
                .append("' ");
        LOGGER.info("check table sql: {}", sqlBuilder);
        return sqlBuilder.toString();
    }

    /**
     * Build SQL to check whether the column exists.
     *
     * @param tableName Oracle table name
     * @param columnName Oracle column name
     * @return the check column SQL string
     */
    public static String getCheckColumn(final String tableName, final String columnName) {
        final StringBuilder sqlBuilder = new StringBuilder().append("SELECT count(1) ")
                .append(" from  USER_TAB_COLUMNS where TABLE_NAME= '")
                .append(tableName)
                .append("' and COLUMN_NAME = '")
                .append(columnName)
                .append("' ");
        LOGGER.info("check table sql: {}", sqlBuilder);
        return sqlBuilder.toString();
    }

    /**
     * Build create table SQL by OracleTableInfo.
     *
     * @param table Oracle table info {@link OracleTableInfo}
     * @return the create table SQL String
     */
    public static List<String> buildCreateTableSql(final OracleTableInfo table) {
        final StringBuilder createSql = new StringBuilder()
                .append("CREATE TABLE ").append(table.getUserName())
                .append(".\"")
                .append(table.getTableName())
                .append("\"")
                .append(buildCreateColumnsSql(table));
        final List<String> sqls = Lists.newArrayList(createSql.toString());

        sqls.addAll(getColumnsComment(table.getTableName(), table.getColumns()));
        LOGGER.info("create table sql size: {}", sqls.size());
        return sqls;
    }

    /**
     * Build add columns SQL.
     *
     * @param tableName Oracle table name
     * @param columnList Oracle column list {@link List}
     * @return add column SQL string list
     */
    public static List<String> buildAddColumnsSql(final String tableName, final List<OracleColumnInfo> columnList) {
        final List<String> resultList = new ArrayList<>();
        final StringBuilder sqlBuilder = new StringBuilder();
        columnList.forEach(columnInfo -> {
            resultList.add(sqlBuilder.append("ALTER TABLE \"")
                    .append(tableName)
                    .append("\" ADD \"")
                    .append(columnInfo.getName())
                    .append("\" ")
                    .append(columnInfo.getType())
                    .append(" ")
                    .toString());
            sqlBuilder.delete(0, sqlBuilder.length());
        });

        resultList.addAll(getColumnsComment(tableName, columnList));
        LOGGER.info("add columns sql={}", resultList);
        return resultList;
    }

    /**
     * Build create column SQL.
     *
     * @param table Oracle table info {@link OracleColumnInfo}
     * @return create column SQL string
     */
    private static String buildCreateColumnsSql(final OracleTableInfo table) {
        final List<String> columnList = getColumnsInfo(table.getColumns());
        final StringBuilder sql = new StringBuilder()
                .append(" (")
                .append(StringUtils.join(columnList, ","))
                .append(") ");
        LOGGER.info("create columns sql={}", sql);
        return sql.toString();
    }

    /**
     * Build column info by OracleColumnInfo list.
     *
     * @param columns Oracle column info {@link OracleColumnInfo} list
     * @return the SQL list
     */
    private static List<String> getColumnsInfo(final List<OracleColumnInfo> columns) {
        final List<String> columnList = new ArrayList<>();
        final StringBuilder sqlBuilder = new StringBuilder();
        columns.forEach(columnInfo -> {
            columnList.add(sqlBuilder.append("\"")
                    .append(columnInfo.getName())
                    .append("\" ")
                    .append(columnInfo.getType())
                    .toString());
            sqlBuilder.delete(0, sqlBuilder.length());
        });
        return columnList;
    }

    /**
     * Build column comment by  tableName and OracleColumnInfo list.
     *
     * @param tableName Oracle table name
     * @param columns Oracle column info {@link OracleColumnInfo} list
     * @return the comment SQL list
     */
    private static List<String> getColumnsComment(final String tableName, final List<OracleColumnInfo> columns) {
        final List<String> commentList = new ArrayList<>();
        final StringBuilder sqlBuilder = new StringBuilder();
        columns.stream()
                .filter(columnInfo -> StringUtils.isNotBlank(columnInfo.getComment()))
                .forEach(columnInfo -> {
                    sqlBuilder.append("COMMENT ON COLUMN \"")
                            .append(tableName)
                            .append("\".\"")
                            .append(columnInfo.getName())
                            .append("\" IS '")
                            .append(columnInfo.getComment())
                            .append("' ");
                    commentList.add(sqlBuilder.toString());
                    sqlBuilder.delete(0, sqlBuilder.length());
                });
        return commentList;
    }

    /**
     * Build query table SQL.
     *
     * @param tableName Oracle table name
     * @return desc table SQL string
     */
    public static String buildDescTableSql(final String tableName) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT A.COLUMN_NAME,A.DATA_TYPE,B.COMMENTS ")
                .append(" FROM USER_TAB_COLUMNS A LEFT JOIN USER_COL_COMMENTS B ")
                .append("  ON A.TABLE_NAME=B.TABLE_NAME AND A.COLUMN_NAME=B.COLUMN_NAME ")
                .append("WHERE  A.TABLE_NAME = '")
                .append(tableName)
                .append("'  ORDER  BY A.COLUMN_ID ");
        LOGGER.info("desc table sql={}", sql);
        return sql.toString();
    }

}
