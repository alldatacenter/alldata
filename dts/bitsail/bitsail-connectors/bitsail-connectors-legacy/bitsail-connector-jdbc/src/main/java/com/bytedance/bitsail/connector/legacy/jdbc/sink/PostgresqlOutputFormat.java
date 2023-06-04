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

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.util.TypeConvertUtil.StorageEngine;
import com.bytedance.bitsail.connector.legacy.jdbc.constants.WriteModeProxy;
import com.bytedance.bitsail.connector.legacy.jdbc.options.PostgresWriterOptions;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.PostgresqlUtil;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.upsert.JDBCUpsertUtil;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.upsert.PostgreSqlUpsertUtil;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class PostgresqlOutputFormat extends JDBCOutputFormat {

  protected String primaryKey;
  protected Boolean truncateMode;
  protected String dbName;
  protected String tableSchema;
  protected String upsertIndex;
  private Boolean deleteThresholdEnabled;

  @Override
  public void initPlugin() throws IOException {
    truncateMode = outputSliceConfig.get(PostgresWriterOptions.IS_TRUNCATE_MODE);
    dbName = outputSliceConfig.get(PostgresWriterOptions.DB_NAME);
    tableSchema = outputSliceConfig.get(PostgresWriterOptions.TABLE_SCHEMA);
    deleteThresholdEnabled = outputSliceConfig.get(PostgresWriterOptions.DELETE_THRESHOLD_ENABLED);
    upsertIndex = outputSliceConfig.get(PostgresWriterOptions.UPSERT_KEY);
    if (!truncateMode && deleteThresholdEnabled) {
      primaryKey = outputSliceConfig.getNecessaryOption(PostgresWriterOptions.PRIMARY_KEY, CommonErrorCode.CONFIG_ERROR);
    }

    super.initPlugin();
    if (truncateMode) {
      if (!StringUtils.isEmpty(partitionName) || !extraPartitions.getFieldNames().isEmpty()) {
        throw BitSailException.asBitSailException(CommonErrorCode.CONFIG_ERROR,
            "Truncate mode can't configure partitions!");
      }
    }
  }

  @Override
  protected Map<String, List<String>> initUniqueIndexColumnsMap() throws IOException {
    PostgresqlUtil psqlUtil = new PostgresqlUtil();
    Map<String, List<String>> indexColumnsMap = new HashMap<>();
    try {
      indexColumnsMap = psqlUtil.getIndexColumnsMap(dbURL, username, password, null, null, table, true);
      initUpsertIndex(indexColumnsMap);
    } catch (Exception e) {
      throw new IOException("unable to get unique indexes info, Error: " + e.toString());
    }
    return indexColumnsMap;
  }

  protected void initUpsertIndex(Map<String, List<String>> indexColumnsMap) throws IOException {
    List<String> uniqueIndexes;
    try {
      uniqueIndexes = getUniqueColumns(indexColumnsMap);
    } catch (Exception e) {
      throw new IOException("unable to get unique indexes info");
    }

    if (upsertIndex == null || upsertIndex.isEmpty()) {
      if (uniqueIndexes.size() > 1) {
        throw new IOException("overwrite write_mode can not be used on the table with several unique indexes!");
      }
      upsertIndex = uniqueIndexes.size() == 1 ? uniqueIndexes.get(0) : "";
    } else if (!uniqueIndexes.contains(upsertIndex)) {
      throw new IOException("Upsert key is not unique key!");
    }
  }

  public List<String> getUniqueColumns(Map<String, List<String>> indexColumns) {

    List<String> uniqueColumns = new ArrayList<>();

    indexColumns.forEach((index, columns) -> {
      uniqueColumns.add(String.join(",", columns));
    });

    return uniqueColumns;
  }

  @Override
  protected String genUpsertTemplate(String table, List<String> columnNames) {
    return jdbcUpsertUtil.genUpsertTemplate(table, columnNames, upsertIndex);
  }

  @Override
  protected JDBCUpsertUtil initUpsertUtils() {
    return new PostgreSqlUpsertUtil(this, shardKeys, upsertKeys);
  }

  @Override
  protected List<String> addPartitionColumns(List<String> columnNames, String partitionName, JDBCOutputExtraPartitions extraPartitions) {
    if (truncateMode) {
      return columnNames;
    }
    return super.addPartitionColumns(columnNames, partitionName, extraPartitions);
  }

  @Override
  public String getDriverName() {
    return PostgresqlUtil.DRIVER_NAME;
  }

  @Override
  public StorageEngine getStorageEngine() {
    return StorageEngine.postgresql;
  }

  @Override
  public String getFieldQuote() {
    return PostgresqlUtil.DB_QUOTE;
  }

  @Override
  public String getValueQuote() {
    return PostgresqlUtil.VALUE_QUOTE;
  }

  @Override
  public String getType() {
    return "Postgresql";
  }

  @Override
  protected String genClearQuery(String partitionValue, String compare, String extraPartitionsSql) {
    if (truncateMode) {
      return genClearQueryWithTruncateMode();
    }
    if (!deleteThresholdEnabled) {
      return genClearQueryWithoutThreshold(partitionValue, compare, extraPartitionsSql);
    }
    return genClearQueryWithThreshold(partitionValue, compare, extraPartitionsSql);
  }

  String genClearQueryWithThreshold(String partitionValue, String compare, String extraPartitionsSql) {
    final String tableWithQuote = getQuoteTable(table);
    final String partitionNameWithQuote = getQuoteColumn(partitionName);
    final String primaryKeyWithQuote = getQuoteColumn(primaryKey);
    String selectQuery;
    if (partitionPatternFormat.equals("yyyyMMdd")) {
      selectQuery = "select " + primaryKeyWithQuote + " from " + tableWithQuote + " where " + partitionNameWithQuote +
          compare + wrapPartitionValueWithQuota(partitionValue) + extraPartitionsSql + " limit " + deleteThreshold;
    } else {
      selectQuery = "select " + primaryKeyWithQuote + " from " + tableWithQuote + " where " + partitionNameWithQuote +
          compare + getQuoteValue(partitionValue) + extraPartitionsSql + " limit " + deleteThreshold;
    }
    return "delete from " + tableWithQuote + " where " + primaryKeyWithQuote + " in (" + selectQuery + ")";
  }

  String genClearQueryWithoutThreshold(String partitionValue, String compare, String extraPartitionsSql) {
    if (partitionPatternFormat.equals("yyyyMMdd")) {
      return "delete from " + getQuoteTable(table) + " where " + getQuoteColumn(partitionName) +
          compare + wrapPartitionValueWithQuota(partitionValue) + extraPartitionsSql;
    } else {
      return "delete from " + getQuoteTable(table) + " where " + getQuoteColumn(partitionName) +
          compare + getQuoteValue(partitionValue) + extraPartitionsSql;
    }
  }

  String genClearQueryWithTruncateMode() {
    return "TRUNCATE TABLE " + getQuoteTable(table);
  }

  @Override
  protected void addPartitionValue(int index, String partitionKey, String columnType) throws BitSailException {
    if (truncateMode) {
      return;
    }
    super.addPartitionValue(index, partitionKey, columnType);
  }

  @Override
  protected WriteModeProxy buildWriteModeProxy(WriteModeProxy.WriteMode writeMode) {
    switch (writeMode) {
      case insert:
        return new PostgresqlInsertProxy();
      case overwrite:
        return new OverwriteProxy();
      default:
        throw BitSailException.asBitSailException(CommonErrorCode.CONFIG_ERROR, "unsupported write mode: " + writeMode);
    }
  }

  public class PostgresqlInsertProxy extends InsertProxy {
    @Override
    protected void validatePartitionValue(String partitionValue) {
      if (truncateMode) {
        return;
      }
      super.validatePartitionValue(partitionValue);
    }
  }
}
