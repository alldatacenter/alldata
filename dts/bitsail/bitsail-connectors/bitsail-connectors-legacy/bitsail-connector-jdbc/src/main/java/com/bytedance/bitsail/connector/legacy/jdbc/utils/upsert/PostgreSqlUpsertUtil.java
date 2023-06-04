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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PostgreSqlUpsertUtil extends JDBCUpsertUtil {
  public PostgreSqlUpsertUtil(JDBCOutputFormat jdbcOutputFormat, String[] shardKeys, Map<String, List<String>> upsertKeys) {
    super(jdbcOutputFormat, shardKeys, upsertKeys);
  }

  @Override
  public String genUpsertTemplate(String table, List<String> columns, String targetUniqueKey) {
    return getInsertStatement(columns, table) + onDuplicateKeyUpdateString(columns, targetUniqueKey);
  }

  public String onDuplicateKeyUpdateString(List<String> columnsHolder, String targetUniqueKey) {
    if (targetUniqueKey.isEmpty() || columnsHolder == null || columnsHolder.size() < 1) {
      return "";
    }

    return columnsHolder.parallelStream()
        .filter(col -> {
          for (String shardKey : shardKeys) {
            if (shardKey.equalsIgnoreCase(col)) {
              return false;
            }
          }
          return true;
        })
        .map(col -> quoteColumn(col) + "=excluded." + quoteColumn(col))
        .collect(Collectors.joining(",", " ON CONFLICT (" + targetUniqueKey + ") DO UPDATE SET ", " "));
  }
}
