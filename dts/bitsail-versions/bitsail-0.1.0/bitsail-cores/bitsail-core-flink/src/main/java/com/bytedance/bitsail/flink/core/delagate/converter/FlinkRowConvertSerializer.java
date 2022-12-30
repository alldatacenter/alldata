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

package com.bytedance.bitsail.flink.core.delagate.converter;

import com.bytedance.bitsail.base.serializer.RowSerializer;
import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.column.Column;
import com.bytedance.bitsail.common.column.ListColumn;
import com.bytedance.bitsail.common.column.MapColumn;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.typeinfo.BasicArrayTypeInfo;
import com.bytedance.bitsail.common.typeinfo.ListTypeInfo;
import com.bytedance.bitsail.common.typeinfo.MapTypeInfo;
import com.bytedance.bitsail.common.typeinfo.TypeInfo;
import com.bytedance.bitsail.common.typeinfo.TypeInfos;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.flink.types.Row;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.bytedance.bitsail.common.typeinfo.TypeInfos.STRING_TYPE_INFO;

/**
 * Created 2022/6/21
 */
public class FlinkRowConvertSerializer implements RowSerializer<Row> {

  private List<ColumnInfo> columns;

  private TypeInfo<?>[] typeInfos;

  private BitSailConfiguration commonConfiguration;

  public FlinkRowConvertSerializer(TypeInfo<?>[] typeInfos,
                                   List<ColumnInfo> columns,
                                   BitSailConfiguration commonConfiguration) {
    this.columns = columns;
    this.typeInfos = typeInfos;
    this.commonConfiguration = commonConfiguration;
  }

  @Override
  public Row serialize(com.bytedance.bitsail.common.row.Row row) throws IOException {
    Object[] fields = row.getFields();
    int arity = ArrayUtils.getLength(fields);
    Row flinkRow = new Row(arity);
    for (int index = 0; index < arity; index++) {
      TypeInfo<?> typeInfo = typeInfos[index];
      Object field = row.getField(index);
      if (field instanceof Column) {
        field = deserialize((Column) field, typeInfo, columns.get(index).getName());
      }
      flinkRow.setField(index, field);
    }
    return flinkRow;
  }

  @Override
  public com.bytedance.bitsail.common.row.Row deserialize(Row serialized) throws IOException {
    int arity = serialized.getArity();
    Object[] fields = new Object[arity];
    for (int index = 0; index < arity; index++) {
      TypeInfo<?> typeInfo = typeInfos[index];
      Object field = serialized.getField(index);
      String name = columns.get(index).getName();
      if (field instanceof Column) {
        fields[index] = deserialize((Column) field, typeInfo, name);
      } else {
        fields[index] = field;
      }
    }
    return new com.bytedance.bitsail.common.row.Row(
        serialized.getKind().toByteValue(),
        fields);
  }

  private Object deserialize(Column object, TypeInfo<?> typeInfo, String name) throws BitSailException {
    if (Objects.isNull(object)) {
      return null;
    }

    Class<?> typeInfoTypeClass = typeInfo.getTypeClass();
    if (List.class.isAssignableFrom(typeInfoTypeClass)) {
      if (!(object instanceof ListColumn)) {
        throw BitSailException.asBitSailException(CommonErrorCode.CONVERT_NOT_SUPPORT,
            String.format("Column %s is not list type, value: %s", name, object));
      }
      return getListValue((List<Column>) object, (ListTypeInfo<?>) typeInfo, name);
    }

    if (Map.class.isAssignableFrom(typeInfoTypeClass)) {
      if (!(object instanceof MapColumn)) {
        throw BitSailException.asBitSailException(CommonErrorCode.CONVERT_NOT_SUPPORT,
            String.format("Column %s is not map type, value: %s", name, object));
      }
      return getMapValue((MapColumn<Column, Column>) object, (MapTypeInfo<?, ?>) typeInfo, name);
    }

    return getBasicTypeValue((Column) object, typeInfo);
  }

  private List<?> getListValue(List<Column> columns, ListTypeInfo<?> listTypeInfo, String name) {
    TypeInfo<?> elementTypeInfo = listTypeInfo.getElementTypeInfo();
    List<Object> objects = new ArrayList<>();
    if (Objects.nonNull(columns)) {
      for (Column column : columns) {
        objects.add(deserialize(column, elementTypeInfo, name));
      }
    }
    return objects;
  }

  private Map<?, ?> getMapValue(Map<Column, Column> columnMap, MapTypeInfo<?, ?> mapTypeInfo, String name) {
    TypeInfo<?> keyTypeInfo = mapTypeInfo.getKeyTypeInfo();
    TypeInfo<?> valueTypeInfo = mapTypeInfo.getValueTypeInfo();

    Map<Object, Object> maps = new HashMap<>();
    if (Objects.nonNull(columnMap)) {
      columnMap.forEach((key, value) -> {
        Object keyValue = deserialize(key, keyTypeInfo, name);
        if (Objects.isNull(keyValue)) {
          throw new BitSailException(CommonErrorCode.CONVERT_NOT_SUPPORT, "");
        }
        Object mapValue = deserialize(value, valueTypeInfo, name);
        maps.put(keyValue, mapValue);
      });
    }
    return maps;
  }

  private Object getBasicTypeValue(Column column, TypeInfo<?> typeInfo) {
    Class<?> typeInfoTypeClass = typeInfo.getTypeClass();
    if (null == column.getRawData()) {
      return null;
    }

    if (STRING_TYPE_INFO.getTypeClass() == typeInfoTypeClass) {
      return column.asString();
    } else if (TypeInfos.BOOLEAN_TYPE_INFO.getTypeClass() == typeInfoTypeClass) {
      return column.asBoolean();
    } else if (TypeInfos.BYTE_TYPE_INFO.getTypeClass() == typeInfoTypeClass) {
      return column.asLong().byteValue();
    } else if (TypeInfos.INT_TYPE_INFO.getTypeClass() == typeInfoTypeClass) {
      return column.asLong().intValue();
    } else if (TypeInfos.SHORT_TYPE_INFO.getTypeClass() == typeInfoTypeClass) {
      return column.asLong().shortValue();
    } else if (TypeInfos.LONG_TYPE_INFO.getTypeClass() == typeInfoTypeClass) {
      return column.asLong();
    } else if (TypeInfos.BIG_INTEGER_TYPE_INFO.getTypeClass() == typeInfoTypeClass) {
      return column.asBigInteger();
    } else if (TypeInfos.FLOAT_TYPE_INFO.getTypeClass() == typeInfoTypeClass) {
      return column.asDouble().floatValue();
    } else if (TypeInfos.DOUBLE_TYPE_INFO.getTypeClass() == typeInfoTypeClass) {
      return column.asDouble();
    } else if (TypeInfos.BIG_DECIMAL_TYPE_INFO.getTypeClass() == typeInfoTypeClass) {
      return column.asBigDecimal();
    } else if (TypeInfos.SQL_DATE_TYPE_INFO.getTypeClass() == typeInfoTypeClass) {
      return new java.sql.Date(column.asDate().getTime());
    } else if (TypeInfos.SQL_TIME_TYPE_INFO.getTypeClass() == typeInfoTypeClass) {
      return new java.sql.Time(column.asDate().getTime());
    } else if (TypeInfos.SQL_TIMESTAMP_TYPE_INFO.getTypeClass() == typeInfoTypeClass) {
      return new java.sql.Timestamp(column.asDate().getTime());
    } else if (BasicArrayTypeInfo.BINARY_TYPE_INFO.getTypeClass() == typeInfoTypeClass) {
      return column.asBytes();
    } else {
      throw BitSailException.asBitSailException(CommonErrorCode.CONVERT_NOT_SUPPORT,
          "Flink basic data type " + typeInfoTypeClass + " is not supported!");
    }
  }

}
