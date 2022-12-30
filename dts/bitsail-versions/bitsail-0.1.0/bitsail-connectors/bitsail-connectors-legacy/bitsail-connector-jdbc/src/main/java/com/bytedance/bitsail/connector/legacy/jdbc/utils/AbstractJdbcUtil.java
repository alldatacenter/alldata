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

package com.bytedance.bitsail.connector.legacy.jdbc.utils;

import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.connector.legacy.jdbc.model.IndexInfo;
import com.bytedance.bitsail.connector.legacy.jdbc.model.TableInfo;

import com.google.common.collect.Multimap;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
public abstract class AbstractJdbcUtil {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractJdbcUtil.class);

  final String distinctKeyName;
  final String indexColumnName;
  final String indexName;
  final String indexPosition;
  private final String driverName;

  AbstractJdbcUtil(String distinctKeyName, String columnName, String indexName, String indexPosition, String driverName) {
    this.distinctKeyName = distinctKeyName;
    this.indexColumnName = columnName;
    this.indexName = indexName;
    this.indexPosition = indexPosition;
    this.driverName = driverName;
  }

  AbstractJdbcUtil(String distinctKeyName, String columnName, String indexName, String indexPosition, String driverName,
                   Set<String> supportedFlinkNumberType) {
    this(distinctKeyName, columnName, indexName, indexPosition, driverName);
  }

  public TableInfo getTableInfo(String url,
                                String user,
                                String password,
                                String db,
                                String schema,
                                String table,
                                String initSql,
                                String selectColumns) throws Exception {
    boolean isFullColumns = (selectColumns == null);
    List<String> selectColumnList = (selectColumns != null) ? Arrays.asList(selectColumns.toLowerCase().split(",")) : null;
    List<ColumnInfo> columnInfoList = new ArrayList<>();
    Set<String> columnSet = new HashSet<>();

    try (Connection conn = getConnection(url, user, password);
         ResultSet rs = conn.getMetaData().getColumns(null, schema, table, "%")) {
      while (rs.next()) {
        String colName = rs.getString("COLUMN_NAME").toLowerCase();
        columnSet.add(colName);

        if (!isFullColumns && !selectColumnList.contains(colName)) {
          continue;
        }

        String remarks = rs.getString("REMARKS");
        if (remarks == null || remarks.equals("")) {
          remarks = colName;
        }

        String columnType = rs.getString("TYPE_NAME").toLowerCase();

        Object defaultValue = rs.getString("COLUMN_DEF");

        ColumnInfo oneColumn = new ColumnInfo(colName, columnType, remarks, defaultValue);

        columnInfoList.add(oneColumn);
      }
      if (!isFullColumns && !columnSet.containsAll(selectColumnList)) {
        throw new IllegalArgumentException("selected list contains the column which is not belong to the table");
      }
    }

    return new TableInfo(db, table, columnInfoList);
  }

  public Map<String, List<String>> getIndexColumnsMap(String url,
                                                      String user,
                                                      String password,
                                                      String dbName,
                                                      String schema,
                                                      String tableName,
                                                      Boolean unique)
      throws SQLException, ClassNotFoundException {
    Map<String, List<String>> indexColumnsMap = new HashMap<>();
    try (Connection conn = getConnection(url, user, password);
         ResultSet resultSet = conn.getMetaData()
             .getIndexInfo(dbName, schema, tableName, unique, true)) {

      while (resultSet.next()) {
        String columnName = resultSet.getString(indexColumnName);
        if (columnName != null && !columnName.isEmpty()) {
          String index = resultSet.getString(indexName);
          if (!indexColumnsMap.containsKey(index)) {
            indexColumnsMap.put(index, new ArrayList<>());
          }
          indexColumnsMap.get(index).add(columnName);
        }
      }
    }

    return indexColumnsMap;
  }

  Map<String, IndexInfo> getFirstOrderIndex(Multimap<String, IndexInfo> columnIndicesMap) {
    Map<String, IndexInfo> result = new HashMap<>();

    columnIndicesMap.asMap().forEach((columnName, columnIndices) -> {
      final Optional<IndexInfo> columnIndex = columnIndices.stream()
          .max(Comparator.comparingLong(IndexInfo::getCardinality));

      columnIndex.ifPresent(index ->
          result.put(columnName, index)
      );
    });

    return result;
  }

  public TableInfo getCustomizedSQLTableInfo(String url, String userName, String password, String database, String customizedSql) throws Exception {
    Connection conn = null;
    try {
      conn = getConnection(url, userName, password);
      List<ColumnInfo> columnInfoList = new ArrayList<>();
      PreparedStatement statement = conn.prepareStatement(customizedSql);
      ResultSetMetaData metaData = statement.getMetaData();
      int columnNum = metaData.getColumnCount();
      for (int i = 1; i <= columnNum; i++) {
        ColumnInfo oneColumn = new ColumnInfo(
            metaData.getColumnName(i).toLowerCase(),
            metaData.getColumnTypeName(i).toLowerCase(),
            metaData.getColumnLabel(i).toLowerCase());
        columnInfoList.add(oneColumn);
      }
      return new TableInfo(database, "_customized_sql", columnInfoList);
    } finally {
      try {
        if (null != conn) {
          conn.close();
        }
      } catch (SQLException e) {
        log.error("Get table info got exception!", e);
      }
    }
  }

  public Connection getConnection(String url, String user, String pwd)
      throws ClassNotFoundException, SQLException {
    Class.forName(driverName);
    return DriverManager.getConnection(url, user, pwd);
  }
}
