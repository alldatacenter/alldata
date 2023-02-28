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
package io.datavines.engine.local.api.utils;

import io.datavines.engine.local.api.entity.QueryColumn;
import io.datavines.engine.local.api.entity.ResultList;
import io.datavines.engine.local.api.entity.ResultListWithColumns;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

import static io.datavines.common.CommonConstants.DOT;
import static org.apache.commons.lang3.StringUtils.EMPTY;

public class SqlUtils {

    private static final Logger logger = LoggerFactory.getLogger(SqlUtils.class);

    public static ResultListWithColumns getListWithHeaderFromResultSet(ResultSet rs, Set<String> queryFromsAndJoins) throws SQLException {

        ResultListWithColumns resultListWithColumns = new ResultListWithColumns();

        ResultSetMetaData metaData = rs.getMetaData();

        List<QueryColumn> queryColumns = new ArrayList<>();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String key = getColumnLabel(queryFromsAndJoins, metaData.getColumnLabel(i));
            queryColumns.add(new QueryColumn(key, metaData.getColumnTypeName(i),""));
        }
        resultListWithColumns.setColumns(queryColumns);

        List<Map<String, Object>> resultList = new ArrayList<>();

        try {
            while (rs.next()) {
                resultList.add(getResultObjectMap(rs, metaData, queryFromsAndJoins));
            }
        } catch (Throwable e) {
            logger.error("get result set error: {0}", e);
        }

        resultListWithColumns.setResultList(resultList);
        return resultListWithColumns;
    }

    public static ResultListWithColumns getListWithHeaderFromResultSet(ResultSet rs, Set<String> queryFromsAndJoins, int start, int end) throws SQLException {

        ResultListWithColumns resultListWithColumns = new ResultListWithColumns();

        ResultSetMetaData metaData = rs.getMetaData();

        List<QueryColumn> queryColumns = new ArrayList<>();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String key = getColumnLabel(queryFromsAndJoins, metaData.getColumnLabel(i));
            queryColumns.add(new QueryColumn(key, metaData.getColumnTypeName(i),""));
        }
        resultListWithColumns.setColumns(queryColumns);
        resultListWithColumns.setResultList(getPage(rs, queryFromsAndJoins, start, end, metaData));
        return resultListWithColumns;
    }

    public static ResultList getPageFromResultSet(ResultSet rs, Set<String> queryFromsAndJoins, int start, int end) throws SQLException {

        ResultList result = new ResultList();
        ResultSetMetaData metaData = rs.getMetaData();
        result.setResultList(getPage(rs, queryFromsAndJoins, start, end, metaData));
        return result;
    }

    private static List<Map<String, Object>> getPage(ResultSet rs, Set<String> queryFromsAndJoins, int start, int end, ResultSetMetaData metaData) {

        List<Map<String, Object>> resultList = new ArrayList<>();

        try {
            if (start > 0) {
                rs.absolute(start);
            }
            int current = start;
            while (rs.next()) {
                if (current >= start && current < end ){
                    resultList.add(getResultObjectMap(rs, metaData, queryFromsAndJoins));
                    if (current == end-1){
                        break;
                    }
                    current++;
                } else {
                    break;
                }
            }
        } catch (Throwable e) {
            logger.error("get result set error: {0}", e);
        }

        return resultList;
    }

    public static ResultList getListFromResultSet(ResultSet rs, Set<String> queryFromsAndJoins) throws SQLException {

        ResultList result = new ResultList();
        ResultSetMetaData metaData = rs.getMetaData();

        List<Map<String, Object>> resultList = new ArrayList<>();

        try {
            while (rs.next()) {
                resultList.add(getResultObjectMap(rs, metaData, queryFromsAndJoins));
            }
        } catch (Throwable e) {
            logger.error("get result set error: {0}", e);
        }

        result.setResultList(resultList);
        return result;
    }

    private static Map<String, Object> getResultObjectMap(ResultSet rs, ResultSetMetaData metaData, Set<String> queryFromsAndJoins) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<>();

        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String key = metaData.getColumnLabel(i);
            String label = getColumnLabel(queryFromsAndJoins, key);

            Object value = rs.getObject(key);
            map.put(label.toLowerCase(), value instanceof byte[] ? new String((byte[]) value) : value);
        }

        return map;
    }

    private static String getColumnLabel(Set<String> columnPrefixes, String columnLabel) {
        if (!CollectionUtils.isEmpty(columnPrefixes)) {
            for (String prefix : columnPrefixes) {
                if (columnLabel.startsWith(prefix)) {
                    return columnLabel.replaceFirst(prefix, EMPTY);
                }
                if (columnLabel.startsWith(prefix.toLowerCase())) {
                    return columnLabel.replaceFirst(prefix.toLowerCase(), EMPTY);
                }
                if (columnLabel.startsWith(prefix.toUpperCase())) {
                    return columnLabel.replaceFirst(prefix.toUpperCase(), EMPTY);
                }
            }
        }

        return columnLabel;
    }

    public static Set<String> getQueryFromsAndJoins(String sql) {
        Set<String> columnPrefixes = new HashSet<>();
        try {
            net.sf.jsqlparser.statement.Statement parse = CCJSqlParserUtil.parse(sql);
            Select select = (Select) parse;
            SelectBody selectBody = select.getSelectBody();
            if (selectBody instanceof PlainSelect) {
                PlainSelect plainSelect = (PlainSelect) selectBody;
                columnPrefixExtractor(columnPrefixes, plainSelect);
            }

            if (selectBody instanceof SetOperationList) {
                SetOperationList setOperationList = (SetOperationList) selectBody;
                List<SelectBody> selects = setOperationList.getSelects();
                for (SelectBody optSelectBody : selects) {
                    PlainSelect plainSelect = (PlainSelect) optSelectBody;
                    columnPrefixExtractor(columnPrefixes, plainSelect);
                }
            }

        } catch (JSQLParserException e) {
            logger.error("get column error: ", e);
        }

        return columnPrefixes;
    }

    private static void columnPrefixExtractor(Set<String> columnPrefixes, PlainSelect plainSelect) {
        getFromItemName(columnPrefixes, plainSelect.getFromItem());
        List<Join> joins = plainSelect.getJoins();
        if (!CollectionUtils.isEmpty(joins)) {
            joins.forEach(join -> getFromItemName(columnPrefixes, join.getRightItem()));
        }
    }

    private static void getFromItemName(Set<String> columnPrefixes, FromItem fromItem) {
        if (fromItem == null) {
            return;
        }
        Alias alias = fromItem.getAlias();
        if (alias != null) {
            if (alias.isUseAs()) {
                columnPrefixes.add(alias.getName().trim() + DOT);
            } else {
                columnPrefixes.add(alias.toString().trim() + DOT);
            }
        } else {
            fromItem.accept(getFromItemTableName(columnPrefixes));
        }
    }

    private static FromItemVisitor getFromItemTableName(Set<String> set) {
        return new FromItemVisitor() {
            @Override
            public void visit(Table tableName) {
                set.add(tableName.getName() + DOT);
            }

            @Override
            public void visit(SubSelect subSelect) {
            }

            @Override
            public void visit(SubJoin subjoin) {
            }

            @Override
            public void visit(LateralSubSelect lateralSubSelect) {
            }

            @Override
            public void visit(ValuesList valuesList) {
            }

            @Override
            public void visit(TableFunction tableFunction) {
            }

            @Override
            public void visit(ParenthesisFromItem aThis) {
            }
        };
    }
}
