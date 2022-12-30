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

package com.bytedance.bitsail.connector.legacy.jdbc.utils.upsert;

import com.bytedance.bitsail.connector.legacy.jdbc.sink.JDBCOutputFormat;

import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class JDBCUpsertUtil implements Serializable {
  public String[] shardKeys;
  public Map<String, List<String>> upsertKeys;
  protected JDBCOutputFormat jdbcOutputFormat;

  public JDBCUpsertUtil(JDBCOutputFormat jdbcOutputFormat, String[] shardKeys, Map<String, List<String>> upsertKeys) {
    this.jdbcOutputFormat = jdbcOutputFormat;
    this.shardKeys = shardKeys;
    this.upsertKeys = upsertKeys;
  }

  public abstract String genUpsertTemplate(String table, List<String> columns, String targetUniqueKey);

  protected String getInsertStatement(List<String> columns, String table) {
    return "INSERT INTO " + quoteTable(table)
        + " (" + quoteColumns(columns) + ") values ("
        + StringUtils.repeat("?", ",", columns.size()) + ")";
  }

  protected String equalUpsertKeysSql(Map<String, List<String>> upsertKeys) {
    List<String> exprList = new ArrayList<>();
    for (Map.Entry<String, List<String>> entry : upsertKeys.entrySet()) {
      List<String> colList = new ArrayList<>();
      for (String col : entry.getValue()) {
        colList.add("T1." + quoteColumn(col) + "=T2." + quoteColumn(col));
      }
      exprList.add(StringUtils.join(colList, " AND "));
    }
    return StringUtils.join(exprList, " OR ");
  }

  protected List<String> getUpdateColumns(List<String> columns, Map<String, List<String>> upsertKeys) {
    Set<String> indexColumns = new HashSet<>();
    Set<String> shardColumns = new HashSet<>();
    for (List<String> value : upsertKeys.values()) {
      indexColumns.addAll(value);
    }

    for (String shardKey : shardKeys) {
      shardColumns.add(shardKey);
    }

    List<String> updateColumns = new ArrayList<>();
    for (String col : columns) {
      if (!indexColumns.contains(col) && !shardColumns.contains(col)) {
        updateColumns.add(col);
      }
    }

    return updateColumns;
  }

  protected String getUpdateSql(List<String> column, String leftTable, String rightTable) {
    String prefixLeft = StringUtils.isBlank(leftTable) ? "" : quoteTable(leftTable) + ".";
    String prefixRight = StringUtils.isBlank(rightTable) ? "" : quoteTable(rightTable) + ".";
    List<String> list = new ArrayList<>();
    for (String col : column) {
      list.add(prefixLeft + col + "=" + prefixRight + col);
    }
    return StringUtils.join(list, ",");
  }

  protected String quoteColumns(List<String> column) {
    return quoteColumns(column, null);
  }

  protected String quoteColumns(List<String> column, String table) {
    String prefix = StringUtils.isBlank(table) ? "" : quoteTable(table) + ".";
    List<String> list = new ArrayList<>();
    for (String col : column) {
      list.add(prefix + quoteColumn(col));
    }
    return StringUtils.join(list, ",");
  }

  protected String makeValues(List<String> column) {
    StringBuilder sb = new StringBuilder("SELECT ");
    for (int i = 0; i < column.size(); ++i) {
      if (i != 0) {
        sb.append(",");
      }
      sb.append("? " + quoteColumn(column.get(i)));
    }
    return sb.toString();
  }

  protected String quoteColumn(String col) {
    return this.jdbcOutputFormat.getQuoteColumn(col);
  }

  protected String quoteTable(String col) {
    return this.jdbcOutputFormat.getQuoteTable(col);
  }
}
