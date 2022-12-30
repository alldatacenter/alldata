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

package com.bytedance.bitsail.connector.legacy.hudi.util;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.model.ColumnInfo;

import org.apache.avro.Schema;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.types.AtomicDataType;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.table.types.logical.utils.LogicalTypeParser;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("checkstyle:MagicNumber")
public class SchemaUtils {
  public static Schema getAvroSchema(String schemaStr) {
    return new Schema.Parser().parse(schemaStr);
  }

  public static RowType getRowTypeFromColumnInfo(List<ColumnInfo> sourceColumns) {
    List<RowType.RowField> fields = getRowFields(sourceColumns);
    return new RowType(false, fields);
  }

  /**
   * Convert ColumnInfo to RowField.
   */
  private static List<RowType.RowField> getRowFields(List<ColumnInfo> columns) {
    List<RowType.RowField> rowFields = new ArrayList<>();
    for (ColumnInfo column : columns) {
      String name = column.getName();
      String type = column.getType().toLowerCase();
      LogicalType logicalType = getLogicalType(type);
      rowFields.add(new RowType.RowField(name, logicalType));
    }
    return rowFields;
  }

  private static LogicalType getLogicalType(String type) {
    if (isArrayType(type)) {
      return arrayType2DataType(type).getLogicalType();
    } else if (isMapType(type)) {
      return mapType2DataType(type).getLogicalType();
    } else {
      return baseType2DataType(type).getLogicalType();
    }
  }

  private static boolean isArrayType(String type) {
    return type.startsWith("array<");
  }

  private static boolean isMapType(String type) {
    return type.startsWith("map<");
  }

  private static DataType arrayType2DataType(String type) {
    String elementType = type.substring("array<".length(), type.length() - 1);
    DataType elementDataType = baseType2DataType(elementType);
    return DataTypes.ARRAY(elementDataType);
  }

  private static DataType mapType2DataType(String type) {
    String subString = type.substring("map<".length(), type.length() - 1);
    String[] parts = subString.split(",", 2);
    String keyType = parts[0];
    String valueType = parts[1];
    DataType keyLogicalType = baseType2DataType(keyType).notNull();
    DataType valueLogicalType = baseType2DataType(valueType);
    return DataTypes.MAP(keyLogicalType, valueLogicalType);
  }

  /**
   * @param type
   * @return
   */
  private static DataType baseType2DataType(String type) {
    if (type.startsWith("decimal")) {
      return new AtomicDataType(LogicalTypeParser.parse(type));
    }

    switch (type) {
      case "string":
        return DataTypes.STRING();
      case "tinyint":
      case "int":
        return DataTypes.INT();
      case "long":
      case "bigint":
        return DataTypes.BIGINT();
      case "double":
        return DataTypes.DOUBLE();
      case "boolean":
        return DataTypes.BOOLEAN();
      case "timestamp":
        return DataTypes.TIMESTAMP(0);
      case "date":
        return DataTypes.DATE();
      case "time":
        return DataTypes.TIME(0);
      case "float":
        return DataTypes.FLOAT();
      case "binary":
        return DataTypes.BYTES();
      default:
        throw BitSailException.asBitSailException(CommonErrorCode.CONVERT_NOT_SUPPORT,
            "Unsupported column type: " + type);
    }
  }
}
