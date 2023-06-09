/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.planner.types;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.sql.SqlCollation;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.util.Util;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector.Category;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector;
import org.apache.hadoop.hive.serde2.typeinfo.DecimalTypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.ListTypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.MapTypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.PrimitiveTypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.StructTypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for data type conversions
 * from {@link org.apache.hadoop.hive.metastore.api.FieldSchema} instances
 * to {@link  org.apache.calcite.rel.type.RelDataType} instances
 */
public class HiveToRelDataTypeConverter {

  private static final Logger logger = LoggerFactory.getLogger(HiveToRelDataTypeConverter.class);

  private static final String UNSUPPORTED_HIVE_DATA_TYPE_ERROR_MSG = "Unsupported Hive data type %s. %n" +
      "Following Hive data types are supported in Drill INFORMATION_SCHEMA: " +
      "BOOLEAN, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, DATE, TIMESTAMP, BINARY, DECIMAL, STRING, " +
      "VARCHAR, CHAR, LIST, MAP, STRUCT and UNION";


  private final RelDataTypeFactory typeFactory;

  public HiveToRelDataTypeConverter(RelDataTypeFactory typeFactory) {
    this.typeFactory = typeFactory;
  }

  /**
   * Performs conversion from Hive field to nullable RelDataType
   *
   * @param field - representation of data type in Hive Metastore
   * @return appropriate nullable RelDataType for using with Calcite
   * @throws RuntimeException for unsupported data types, check
   *         {@link HiveToRelDataTypeConverter#UNSUPPORTED_HIVE_DATA_TYPE_ERROR_MSG}
   *         for details about supported hive types
   */
  public RelDataType convertToNullableRelDataType(FieldSchema field) {
    TypeInfo fieldTypeInfo = TypeInfoUtils.getTypeInfoFromTypeString(field.getType());
    return convertToRelDataType(fieldTypeInfo, true);
  }

  private RelDataType convertToRelDataType(TypeInfo typeInfo, boolean nullable) {
    final Category typeCategory = typeInfo.getCategory();
    switch (typeCategory) {
      case PRIMITIVE:
        return typeFactory.createTypeWithNullability(getRelDataType((PrimitiveTypeInfo) typeInfo), nullable);
      case LIST:
        return typeFactory.createTypeWithNullability(getRelDataType((ListTypeInfo) typeInfo, nullable), nullable);
      case MAP:
        return typeFactory.createTypeWithNullability(getRelDataType((MapTypeInfo) typeInfo, nullable), nullable);
      case STRUCT:
        return typeFactory.createTypeWithNullability(getRelDataType((StructTypeInfo) typeInfo, nullable), nullable);
      case UNION:
        logger.warn("There is no UNION data type in SQL. Converting it to Sql type OTHER to avoid " +
            "breaking INFORMATION_SCHEMA queries");
        return typeFactory.createTypeWithNullability(typeFactory.createSqlType(SqlTypeName.OTHER), nullable);
    }
    throw new RuntimeException(String.format(UNSUPPORTED_HIVE_DATA_TYPE_ERROR_MSG, typeCategory));
  }

  private RelDataType getRelDataType(StructTypeInfo structTypeInfo, boolean nullable) {
    final List<String> fieldNames = structTypeInfo.getAllStructFieldNames();
    final List<RelDataType> relDataTypes = structTypeInfo.getAllStructFieldTypeInfos().stream()
        .map(typeInfo -> convertToRelDataType(typeInfo, nullable))
        .collect(Collectors.toList());
    return typeFactory.createStructType(relDataTypes, fieldNames);
  }

  private RelDataType getRelDataType(MapTypeInfo mapTypeInfo, boolean nullable) {
    RelDataType keyType = convertToRelDataType(mapTypeInfo.getMapKeyTypeInfo(), nullable);
    RelDataType valueType = convertToRelDataType(mapTypeInfo.getMapValueTypeInfo(), nullable);
    return typeFactory.createMapType(keyType, valueType);
  }

  private RelDataType getRelDataType(ListTypeInfo listTypeInfo, boolean nullable) {
    RelDataType listElemTypeInfo = convertToRelDataType(listTypeInfo.getListElementTypeInfo(), nullable);
    return typeFactory.createArrayType(listElemTypeInfo, -1);
  }

  private RelDataType getRelDataType(PrimitiveTypeInfo primitiveTypeInfo) {
    final PrimitiveObjectInspector.PrimitiveCategory primitiveCategory = primitiveTypeInfo.getPrimitiveCategory();
    switch (primitiveCategory) {
      case STRING:
      case VARCHAR:
        return getRelDataType(primitiveTypeInfo, SqlTypeName.VARCHAR);
      case CHAR:
        return getRelDataType(primitiveTypeInfo, SqlTypeName.CHAR);
      case BYTE:
      case SHORT:
      case INT:
        return typeFactory.createSqlType(SqlTypeName.INTEGER);
      case DECIMAL:
        return getRelDataType((DecimalTypeInfo) primitiveTypeInfo);
      case BOOLEAN:
        return typeFactory.createSqlType(SqlTypeName.BOOLEAN);
      case LONG:
        return typeFactory.createSqlType(SqlTypeName.BIGINT);
      case FLOAT:
        return typeFactory.createSqlType(SqlTypeName.FLOAT);
      case DOUBLE:
        return typeFactory.createSqlType(SqlTypeName.DOUBLE);
      case DATE:
        return typeFactory.createSqlType(SqlTypeName.DATE);
      case TIMESTAMP:
        return typeFactory.createSqlType(SqlTypeName.TIMESTAMP);
      case BINARY:
        return typeFactory.createSqlType(SqlTypeName.VARBINARY);
    }
    throw new RuntimeException(String.format(UNSUPPORTED_HIVE_DATA_TYPE_ERROR_MSG, primitiveCategory));
  }

  private RelDataType getRelDataType(PrimitiveTypeInfo pTypeInfo, SqlTypeName typeName) {
    int maxLen = TypeInfoUtils.getCharacterLengthForType(pTypeInfo);
    RelDataType relDataType = typeFactory.createSqlType(typeName, maxLen);
    return typeFactory.createTypeWithCharsetAndCollation(relDataType, Util.getDefaultCharset(),
        SqlCollation.IMPLICIT);
  }

  private RelDataType getRelDataType(DecimalTypeInfo decimalTypeInfo) {
    return typeFactory.createSqlType(SqlTypeName.DECIMAL, decimalTypeInfo.precision(), decimalTypeInfo.scale());
  }

}
