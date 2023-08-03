/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.common.datasource.jdbc.utils;

import com.alibaba.druid.sql.SQLUtils;
import io.datavines.common.entity.ListWithQueryColumn;
import io.datavines.common.entity.QueryColumn;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

public class HiveSqlUtils {

    private static final Logger logger = LoggerFactory.getLogger(HiveSqlUtils.class);

    private static final int DEFAULT_LIMIT = 1000;

    public static ListWithQueryColumn query(JdbcTemplate jdbcTemplate, String sql, int limit) {
        long before = System.currentTimeMillis();
        ListWithQueryColumn listWithQueryColumn = new ListWithQueryColumn();
        if (limit <= 0) {
            jdbcTemplate.setMaxRows(DEFAULT_LIMIT);
        } else {
            jdbcTemplate.setMaxRows(Math.min(limit, DEFAULT_LIMIT));
        }

        jdbcTemplate.query(sql, rs -> {

            ResultSetMetaData metaData = rs.getMetaData();
            List<QueryColumn> queryColumns = new ArrayList<>();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                queryColumns.add(new QueryColumn(metaData.getColumnLabel(i), metaData.getColumnTypeName(i)));
            }
            listWithQueryColumn.setColumns(queryColumns);

            List<Map<String, Object>> resultList = new ArrayList<>();

            try {
                while (rs.next()) {
                    resultList.add(getResultObjectMap(rs, metaData));
                }
            } catch (Throwable e) {
                logger.error("get data error", e);
            }

            listWithQueryColumn.setResultList(resultList);

            logger.info("query for {} ms, total count:{} sql:{}",
                    System.currentTimeMillis() - before,
                    listWithQueryColumn.getResultList().size(),
                    formatSql(sql));
        });

        return listWithQueryColumn;
    }

    public static ListWithQueryColumn queryForPage(JdbcTemplate jdbcTemplate,
                                                   String sql, int limit, int pageNumber, int pageSize) {

        if (pageNumber < 1 && pageSize < 1) {
             return query(jdbcTemplate,sql,limit);
        }

        if (pageNumber < 1) {
            pageNumber = 1;
        }

        if (pageSize < 1) {
            pageSize = 10;
        }

        int startRow = (pageNumber - 1) * pageSize;

        int totalCount = 0;
        Object countResult = jdbcTemplate.queryForList("select count(1) from ("+ sql+") tmp", Object.class);
        totalCount = Integer.parseInt(String.valueOf(((List) countResult).get(0)));

        sql = sql + " LIMIT " + startRow + ", " + pageSize;
        ListWithQueryColumn result = query(jdbcTemplate,sql,limit);
        result.setPageNumber(pageNumber);
        result.setPageSize(pageSize);
        result.setTotalCount(totalCount);
        return result;
    }

    public static String formatSql(String sql) {
        try {
            return SQLUtils.formatMySql(sql);
        } catch (Exception e) {
            logger.warn("format sql error ", e);
        }
        return sql;
    }

    private static Map<String, Object> getResultObjectMap(ResultSet rs, ResultSetMetaData metaData) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<>();

        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String key = metaData.getColumnLabel(i);
            Object value = rs.getObject(key);
            map.put(key, value instanceof byte[] ? new String((byte[]) value) : value);
        }

        return map;
    }
}
