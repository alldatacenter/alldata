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
 *
 * Original Files: apache/flink(https://github.com/apache/flink)
 * Copyright: Copyright 2014-2022 The Apache Software Foundation
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.bytedance.bitsail.connector.legacy.jdbc.utils.upsert;

import com.bytedance.bitsail.connector.legacy.jdbc.sink.JDBCOutputFormat;

import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.Map;

public class OracleUpsertUtil extends JDBCUpsertUtil {
  public OracleUpsertUtil(JDBCOutputFormat jdbcOutputFormat, String[] shardKeys, Map<String, List<String>> upsertKeys) {
    super(jdbcOutputFormat, shardKeys, upsertKeys);
  }

  @Override
  public String genUpsertTemplate(String table, List<String> columns, String targetUniqueKey) {
    if (upsertKeys == null || upsertKeys.isEmpty()) {
      return getInsertStatement(columns, table);
    }

    List<String> updateColumns = getUpdateColumns(columns, upsertKeys);
    if (CollectionUtils.isEmpty(updateColumns)) {
      // If there's a UNIQUE constraint conflict, no column needs to be updated.
      return "MERGE INTO " + quoteTable(table) + " T1 USING "
              + "(" + makeValues(columns) + ") T2 ON ("
              + equalUpsertKeysSql(upsertKeys) + ") WHEN NOT MATCHED THEN "
              + "INSERT (" + quoteColumns(columns) + ") VALUES ("
              + quoteColumns(columns, "T2") + ")";
    } else {
      return "MERGE INTO " + quoteTable(table) + " T1 USING "
              + "(" + makeValues(columns) + ") T2 ON ("
              + equalUpsertKeysSql(upsertKeys) + ") WHEN MATCHED THEN UPDATE SET "
              + getUpdateSql(updateColumns, "T1", "T2") + " WHEN NOT MATCHED THEN "
              + "INSERT (" + quoteColumns(columns) + ") VALUES ("
              + quoteColumns(columns, "T2") + ")";
    }
  }

  @Override
  protected String makeValues(List<String> column) {
    StringBuilder sb = new StringBuilder("SELECT ");
    for (int i = 0; i < column.size(); ++i) {
      if (i != 0) {
        sb.append(",");
      }
      sb.append("? " + quoteColumn(column.get(i)));
    }
    sb.append(" FROM DUAL");
    return sb.toString();
  }
}
