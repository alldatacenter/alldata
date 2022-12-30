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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SqlServerUpsertUtil extends JDBCUpsertUtil {

  public SqlServerUpsertUtil(JDBCOutputFormat jdbcOutputFormat, String[] shardKeys, Map<String, List<String>> upsertKeys) {
    super(jdbcOutputFormat, shardKeys, upsertKeys);
  }

  @Override
  public String genUpsertTemplate(String table, List<String> columns, String targetUniqueKey) {
    if (upsertKeys == null || upsertKeys.isEmpty()) {
      return getInsertStatement(columns, table);
    }

    List<String> updateColumns = getUpdateColumns(columns, upsertKeys);
    if (CollectionUtils.isEmpty(updateColumns)) {
      return "MERGE INTO " + jdbcOutputFormat.getQuoteTable(table) + " T1 USING "
          + "(" + makeValues(columns) + ") T2 ON ("
          + equalUpsertKeysSql(upsertKeys) + ") WHEN NOT MATCHED THEN "
          + "INSERT (" + quoteColumns(columns) + ") VALUES ("
          + quoteColumns(columns, "T2") + ");";
    } else {
      return "MERGE INTO " + jdbcOutputFormat.getQuoteTable(table) + " T1 USING "
          + "(" + makeValues(columns) + ") T2 ON ("
          + equalUpsertKeysSql(upsertKeys) + ") WHEN MATCHED THEN UPDATE SET "
          + getSqlServerUpdateSql(updateColumns, upsertKeys, shardKeys, "T1", "T2") + " WHEN NOT MATCHED THEN "
          + "INSERT (" + quoteColumns(columns) + ") VALUES ("
          + quoteColumns(columns, "T2") + ");";
    }
  }

  protected String getSqlServerUpdateSql(List<String> columns, Map<String, List<String>> upsertKeys, String[] shardKeys, String targetTable, String sourceTable) {
    List<String> pkCols = new ArrayList<>();
    for (Map.Entry<String, List<String>> entry : upsertKeys.entrySet()) {
      pkCols.addAll(entry.getValue());
    }

    String prefixLeft = StringUtils.isBlank(targetTable) ? "" : quoteTable(targetTable) + ".";
    String prefixRight = StringUtils.isBlank(sourceTable) ? "" : quoteTable(sourceTable) + ".";
    List<String> list = new ArrayList<>();

    boolean isPk = false;
    boolean isShardKey = false;
    for (String col : columns) {
      for (String pkCol : pkCols) {
        if (pkCol.equalsIgnoreCase(col)) {
          isPk = true;
          break;
        }
      }

      for (String shardKey : shardKeys) {
        if (shardKey.equalsIgnoreCase(col)) {
          isShardKey = true;
          break;
        }
      }

      if (isPk || isShardKey) {
        isPk = false;
        isShardKey = false;
        continue;
      }

      list.add(prefixLeft + quoteColumn(col) + "=" + prefixRight + quoteColumn(col));
      isPk = false;
    }
    return StringUtils.join(list, ",");
  }
}
