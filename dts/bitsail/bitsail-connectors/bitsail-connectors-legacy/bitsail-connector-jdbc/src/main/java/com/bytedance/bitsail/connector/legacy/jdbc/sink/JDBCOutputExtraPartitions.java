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
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.jdbc.constants.Key;
import com.bytedance.bitsail.connector.legacy.jdbc.constants.WriteModeProxy;
import com.bytedance.bitsail.connector.legacy.jdbc.exception.DBUtilErrorCode;
import com.bytedance.bitsail.connector.legacy.jdbc.model.SqlType;
import com.bytedance.bitsail.connector.legacy.jdbc.options.JdbcWriterOptions;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class JDBCOutputExtraPartitions implements Serializable {
  private static final long serialVersionUID = 103L;
  private final String driverName;
  private final String dbQuota;
  private final String valueQuota;
  @Getter
  private List<String> fieldNames;
  private Set<String> dynamicFieldNames;
  private List<String> fieldTypes;
  private List<String> fieldValues;

  @Getter
  private int size;

  JDBCOutputExtraPartitions(final String driverName, final String dbQuota, final String valueQuota) {
    size = 0;
    fieldNames = new ArrayList<>();
    fieldTypes = new ArrayList<>();
    fieldValues = new ArrayList<>();
    dynamicFieldNames = new HashSet<>();
    this.driverName = driverName;
    this.dbQuota = dbQuota;
    this.valueQuota = valueQuota;
  }

  void initExtraPartitions(BitSailConfiguration outputSliceConfig, WriteModeProxy.WriteMode writeMode) {
    List<Map<String, Object>> extraPartitions = outputSliceConfig.get(JdbcWriterOptions.EXTRA_PARTITIONS);
    if (extraPartitions != null) {
      log.info("Extra partition info:");
      size = extraPartitions.size();
      for (val extraPartition : extraPartitions) {
        final String name = getInfo(extraPartition, Key.PARTITION_NAME).toLowerCase();
        final String type = getInfo(extraPartition, Key.PARTITION_TYPE).toLowerCase();
        fieldNames.add(name);
        fieldTypes.add(type);
        log.info("Partition Name:" + name + "Type:" + type);
        if (writeMode == WriteModeProxy.WriteMode.insert) {
          final String value = getInfo(extraPartition, Key.PARTITION_VALUE);
          log.info("Value:" + value);
          fieldValues.add(value);
        }
      }
    }
  }

  String getFieldType(int index) {
    return fieldTypes.get(index);
  }

  Object getFieldValue(int index) {
    return fieldValues.get(index);
  }

  boolean containsColumn(final String column) {
    return dynamicFieldNames.contains(column);
  }

  private String getInfo(Object extraPartition, String key) {
    return ((JSONObject) extraPartition).getString(key);
  }

  String genSqlString() {
    List<String> filters = new ArrayList<>(fieldValues.size());
    for (int i = 0; i < fieldValues.size(); i++) {
      filters.add(getExtraPartitionSqlFilter(
          fieldNames.get(i),
          fieldTypes.get(i),
          fieldValues.get(i)));
    }
    return String.join("", filters);
  }

  String getExtraPartitionSqlFilter(final String name, final String type, Object value) throws BitSailException {
    SqlType.SqlTypes sqlType = SqlType.getSqlType(type, driverName);
    switch (sqlType) {
      case Boolean:
      case Short:
      case Year:
      case Long:
      case Double:
      case Float:
      case BigInt:
      case BigDecimal:
        return " AND " + dbQuota + name + dbQuota + "=" + value;
      case String:
        return " AND " + dbQuota + name + dbQuota + "=" + valueQuota + value + valueQuota;
      case Bytes:
      case Timestamp:
      case Time:
      case Date:
      default:
        throw BitSailException.asBitSailException(DBUtilErrorCode.UNSUPPORTED_TYPE,
            String.format("The extraPartition type [%s] in your configuration is not support." +
                " Please try to change the extraPartition type or don't add this partition.", type)
        );
    }
  }
}