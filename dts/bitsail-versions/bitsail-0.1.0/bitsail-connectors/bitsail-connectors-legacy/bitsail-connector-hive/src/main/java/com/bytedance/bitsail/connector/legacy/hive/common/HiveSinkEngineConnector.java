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

package com.bytedance.bitsail.connector.legacy.hive.common;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.ddl.sink.SinkEngineConnectorBase;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.type.TypeInfoConverter;
import com.bytedance.bitsail.common.type.filemapping.HiveTypeInfoConverter;
import com.bytedance.bitsail.common.util.JsonSerializer;
import com.bytedance.bitsail.common.util.TypeConvertUtil.StorageEngine;
import com.bytedance.bitsail.connector.legacy.hive.option.HiveWriterOptions;

import com.bytedance.bitsail.shaded.hive.client.HiveMetaClientUtil;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.hive.conf.HiveConf;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class HiveSinkEngineConnector extends SinkEngineConnectorBase {

  private static final  int DECIMAL_DEFAULT_PRECISION = 28;
  private static final  int DECIMAL_DEFAULT_SCALE = 10;
  private static final  String DECIMAL = "decimal";
  private final String database;
  private final String table;
  private final List<ColumnInfo> columnInfos;
  private final Map<String, ColumnInfo> columnMappings;
  private transient HiveConf hiveConf;

  public HiveSinkEngineConnector(BitSailConfiguration commonConfiguration,
                                 BitSailConfiguration writerConfiguration) {
    super(commonConfiguration, writerConfiguration);
    database = writerConfiguration.get(HiveWriterOptions.DB_NAME);
    table = writerConfiguration.get(HiveWriterOptions.TABLE_NAME);
    hiveConf = HiveMetaClientUtil.getHiveConf(JsonSerializer
        .parseToMap(writerConfiguration.get(HiveWriterOptions.HIVE_METASTORE_PROPERTIES)));
    try {
      columnInfos = HiveMetaClientUtil.getColumnInfo(hiveConf, database, table);
      columnMappings = columnInfos.stream()
          .collect(Collectors.toMap(ColumnInfo::getName, columnInfo -> columnInfo));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void addColumns(List<ColumnInfo> columnsToAdd) throws Exception {
    if (CollectionUtils.isEmpty(columnsToAdd)) {
      log.info("columns to add is empty, will not execute column add operation.");
      return;
    }
    log.info("start to add columns to hive..., columns to add is : {}", columnsToAdd);
    List<ColumnInfo> columnInfosToAdd = formatColumnInfos(columnsToAdd, columnMappings, true);
    HiveMetaClientUtil.addColumnInfos(hiveConf, database, table, columnInfosToAdd);
    log.info("finished add columns to hive.");
  }

  @Override
  public void updateColumns(List<ColumnInfo> columnsToUpdate) throws Exception {
    if (CollectionUtils.isEmpty(columnsToUpdate)) {
      log.info("columns to update is empty, will not execute column update operation.");
      return;
    }
    log.info("start to add columns to hive..., columns to update is : {}", columnsToUpdate);
    List<ColumnInfo> columnInfosToUpdate = formatColumnInfos(columnsToUpdate, columnMappings, false);
    HiveMetaClientUtil.updateColumnInfos(hiveConf, database, table, columnInfosToUpdate);
    log.info("finished update columns to hive.");
  }

  @Override
  public void deleteColumns(List<ColumnInfo> columnsToDelete) {
    log.info("delete operation is configured to be ignored for now! So these column delete operations will be ignored: {}", columnsToDelete);
  }

  @Override
  public boolean isTypeCompatible(String newType, String oldType) {
    //todo support the decimal(m,n)
    if (oldType.contains(DECIMAL)) {
      oldType = DECIMAL;
    }
    if (newType.contains(DECIMAL)) {
      newType = DECIMAL;
    }
    return oldType.equalsIgnoreCase(newType);
  }

  @Override
  public List<String> getExcludedColumnInfos() throws Exception {
    return new ArrayList<>(HiveMetaClientUtil
        .getPartitionKeys(hiveConf, database, table).keySet());
  }

  @Override
  public List<ColumnInfo> getExternalColumnInfos() throws Exception {
    return columnInfos;
  }

  @Override
  public String getExternalEngineName() {
    return StorageEngine.hive.name();
  }

  String addDecimalPrecision(String type) {
    if (type.equalsIgnoreCase(DECIMAL)) {
      return String.format("%s(%d,%d)", DECIMAL, DECIMAL_DEFAULT_PRECISION, DECIMAL_DEFAULT_SCALE);
    }
    return type;
  }

  public List<ColumnInfo> formatColumnInfos(List<ColumnInfo> columns,
                                            Map<String, ColumnInfo> originColumnInfo,
                                            boolean addMode) {
    if (CollectionUtils.isEmpty(columns)) {
      return new LinkedList<>();
    }
    final String addColumnComment = "BitSail ddl sync add column";
    List<ColumnInfo> columnInfos = new LinkedList<>();
    for (ColumnInfo column : columns) {
      String type = addDecimalPrecision(column.getType());
      String commentInfo;
      if (addMode) {
        commentInfo = addColumnComment;
      } else {
        ColumnInfo columnInfo = originColumnInfo.get(column.getName());
        String comment = StringUtils.isEmpty(columnInfo.getComment()) ? "Origin comment is null."
            : columnInfo.getComment();
        commentInfo = String.format("%s BitSail ddl sync update column type from %s to %s",
            comment, columnInfo.getType(), type);
      }
      columnInfos.add(new ColumnInfo(column.getName(), type, commentInfo));
    }
    return columnInfos;
  }

  @Override
  public TypeInfoConverter createTypeInfoConverter() {
    return new HiveTypeInfoConverter();
  }
}
