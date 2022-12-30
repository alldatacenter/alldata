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

package com.bytedance.bitsail.connector.legacy.jdbc.sink;

import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.util.TypeConvertUtil.StorageEngine;
import com.bytedance.bitsail.connector.legacy.jdbc.constants.WriteModeProxy;
import com.bytedance.bitsail.connector.legacy.jdbc.exception.JDBCPluginErrorCode;
import com.bytedance.bitsail.connector.legacy.jdbc.options.SqlServerWriterOptions;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.SqlServerUtil;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.upsert.JDBCUpsertUtil;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.upsert.SqlServerUpsertUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SqlServerOutputFormat extends JDBCOutputFormat {
  protected String tableSchema;
  protected String dbName;
  protected String tableWithSchema;

  @Override
  public void initPlugin() throws IOException {
    tableSchema = outputSliceConfig.get(SqlServerWriterOptions.TABLE_SCHEMA);
    table = outputSliceConfig.getNecessaryOption(SqlServerWriterOptions.TABLE_NAME, JDBCPluginErrorCode.REQUIRED_VALUE);
    tableWithSchema = tableSchema + "." + table;
    dbName = outputSliceConfig.get(SqlServerWriterOptions.DB_NAME);
    super.initPlugin();
  }

  @Override
  protected Map<String, List<String>> initUniqueIndexColumnsMap() throws IOException {
    SqlServerUtil sqlServerUtil = new SqlServerUtil();
    try {
      return sqlServerUtil.getIndexColumnsMap(dbURL, username, password, dbName, tableSchema, table, true);
    } catch (Exception e) {
      throw new IOException("unable to get unique indexes info");
    }
  }

  @Override
  protected JDBCUpsertUtil initUpsertUtils() {
    return new SqlServerUpsertUtil(this, shardKeys, upsertKeys);
  }

  @Override
  public String getDriverName() {
    return SqlServerUtil.DRIVER_NAME;
  }

  @Override
  public StorageEngine getStorageEngine() {
    return StorageEngine.sqlserver;
  }

  @Override
  public String getFieldQuote() {
    return SqlServerUtil.DB_QUOTE;
  }

  @Override
  public String getValueQuote() {
    return SqlServerUtil.VALUE_QUOTE;
  }

  @Override
  public String getType() {
    return "SqlServer";
  }

  @Override
  public String genClearQuery(String partitionValue, String compare, String extraPartitionsSql) {
    if (partitionPatternFormat.equals("yyyyMMdd")) {
      return "delete top (" + deleteThreshold + ") from " + getQuoteTable(tableWithSchema) + " where " + getQuoteColumn(partitionName) + compare +
          wrapPartitionValueWithQuota(partitionValue) + extraPartitionsSql;
    }
    return "delete top (" + deleteThreshold + ") from " + getQuoteTable(tableWithSchema) + " where " + getQuoteColumn(partitionName) + compare +
        getQuoteValue(partitionValue) + extraPartitionsSql;
  }

  @Override
  String genInsertQuery(String table, List<ColumnInfo> columns, WriteModeProxy.WriteMode writeMode) {
    return super.genInsertQuery(tableWithSchema, columns, writeMode);
  }
}
