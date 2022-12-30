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

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.connector.legacy.jdbc.model.TableInfo;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.lowerCase;

@Slf4j
public class OracleUtil extends AbstractJdbcUtil {

  public static final String DRIVER_NAME = "oracle.jdbc.OracleDriver";
  public static final String DB_QUOTE = "\"";
  public static final String VALUE_QUOTE = "'";

  private static final String COLUMN_NAME = "COLUMN_NAME";
  private static final String DISTINCT_KEYS = "DISTINCT_KEYS";
  private static final String COLUMN_POSITION = "COLUMN_POSITION";
  private static final String INDEX_NAME = "INDEX_NAME";

  private static final Set<String> ORACLE_SUPPORTED_FLINK_NUMBER_TYPE = ImmutableSet.of(
      "short", "int", "long", "bigint", "bigdecimal");

  public OracleUtil() {
    super(DISTINCT_KEYS, COLUMN_NAME, INDEX_NAME, COLUMN_POSITION, DRIVER_NAME, ORACLE_SUPPORTED_FLINK_NUMBER_TYPE);
  }

  public TableInfo getTableInfo(String url, String user, String password, String schemaName, String tableName, String initSql) throws Exception {
    schemaName = schemaName.toUpperCase();
    // fixme: oracle table name is case sensitive, this method can not work for lowercase table name
    tableName = tableName.toUpperCase();
    List<ColumnInfo> columnInfoList = new ArrayList<>();
    Connection conn = null;

    try {
      conn = getConnection(url, user, password, initSql);
      Statement statement = conn.createStatement();
      String sql = String.format("select t1.column_name,data_type,data_scale,comments from (" +
          "    select column_name,data_type,data_precision,data_scale from all_tab_cols where table_Name='%s' AND owner='%s' ORDER BY column_id" +
          ")t1 RIGHT JOIN (" +
          "    select column_name,comments from all_col_comments where table_Name='%s' and owner='%s'" +
          ")t2 on t1.column_name=t2.column_name", tableName, schemaName, tableName, schemaName);
      ResultSet rs = statement.executeQuery(sql);
      while (rs.next()) {
        String colName = rs.getString("column_name");
        String remarks = rs.getString("comments");
        if (remarks == null || remarks.equals("")) {
          remarks = colName;
        }
        String columnType = lowerCase(rs.getString("data_type"));
        String dataScale = rs.getString("data_scale");
        String parsedColumnType = parseColumnType(columnType, dataScale);
        ColumnInfo oneColumn = new ColumnInfo(colName, parsedColumnType, remarks);
        columnInfoList.add(oneColumn);
      }
    } finally {
      try {
        if (null != conn) {
          conn.close();
        }
      } catch (SQLException e) {
        log.error("Get table info got exception!", e);
      }
    }
    return new TableInfo(schemaName, tableName, columnInfoList);

  }

  @Override
  public TableInfo getTableInfo(String url, String user, String password, String dbName, String schema, String tableName,
                                String initSql, String selectColumns) throws Exception {
    return getTableInfo(url, user, password, schema, tableName, initSql);
  }

  public TableInfo getTableInfo(String url, String user, String password, String tableName, String initSql) throws Exception {
    String[] splitTableAndDbName = splitTableName(tableName);
    String db = splitTableAndDbName[0];
    String table = splitTableAndDbName[1];
    return getTableInfo(url, user, password, db, table, initSql);
  }

  String parseColumnType(String columnType, String dataScale) {
    int scale = dataScale == null ? Integer.MAX_VALUE : Integer.parseInt(dataScale);

    if (scale > 0 && "number".equals(columnType)) {
      return "numeric";
    }

    if (columnType.contains("(")) {
      return columnType.split("\\(")[0];
    }

    return columnType;
  }

  @Override
  public Connection getConnection(String url, String user, String pwd) throws ClassNotFoundException, SQLException {
    return this.getConnection(url, user, pwd, "");
  }

  private Connection getConnection(String url, String user, String pwd, String initSql)
      throws ClassNotFoundException, SQLException {
    Class.forName(DRIVER_NAME);
    Connection connection = DriverManager.getConnection(url, user, pwd);
    if (!Strings.isNullOrEmpty(initSql)) {
      Statement statement = connection.createStatement();
      statement.executeQuery(initSql);
      log.info("Init sql " + initSql + " is configured!");
    }
    return connection;
  }

  String[] splitTableName(String tableName) throws BitSailException {
    // Oracle table name type is: db.table
    String[] splitTableAndDbName = tableName.toUpperCase().split("\\.");
    final int count = 2;
    if (splitTableAndDbName.length != count) {
      throw BitSailException.asBitSailException(CommonErrorCode.CONFIG_ERROR,
          "The table name should include db name, the table name is " + tableName);
    }
    return splitTableAndDbName;
  }
}
