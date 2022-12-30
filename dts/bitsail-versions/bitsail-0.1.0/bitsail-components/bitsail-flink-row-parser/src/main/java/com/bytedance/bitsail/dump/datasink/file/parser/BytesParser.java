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

package com.bytedance.bitsail.dump.datasink.file.parser;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.column.ArrayMapColumn;
import com.bytedance.bitsail.common.column.BooleanColumn;
import com.bytedance.bitsail.common.column.BytesColumn;
import com.bytedance.bitsail.common.column.Column;
import com.bytedance.bitsail.common.column.ColumnCast;
import com.bytedance.bitsail.common.column.DateColumn;
import com.bytedance.bitsail.common.column.DoubleColumn;
import com.bytedance.bitsail.common.column.ListColumn;
import com.bytedance.bitsail.common.column.LongColumn;
import com.bytedance.bitsail.common.column.MapColumn;
import com.bytedance.bitsail.common.column.StringColumn;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.util.FieldPathUtils;
import com.bytedance.bitsail.flink.core.typeinfo.ListColumnTypeInfo;
import com.bytedance.bitsail.flink.core.typeinfo.MapColumnTypeInfo;
import com.bytedance.bitsail.parser.error.ParserErrorCode;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @class: BytesParser
 * @desc:
 **/
public abstract class BytesParser implements Serializable {
  /**
   * If convert failed, set column as null.
   */
  protected boolean convertErrorColumnAsNull = false;
  protected boolean useArrayMapColumn = false;
  protected int rawColumnIndex = -1;
  protected SerializerFeature[] serializerFeatures = new SerializerFeature[0];

  protected Column createColumn(TypeInformation<?> typeInformation, Object fieldVal) {
    Class<?> columnTypeClass = typeInformation.getTypeClass();
    if (columnTypeClass == List.class) {
      return createListColumn(typeInformation, (List) fieldVal);
    } else if (columnTypeClass == Map.class) {
      if (useArrayMapColumn) {
        return createArrayMapColumn((MapColumnTypeInfo) typeInformation, (Map) fieldVal);
      }
      return createMapColumn((MapColumnTypeInfo) typeInformation, (Map) fieldVal);
    } else {
      return createBasicColumn(typeInformation, fieldVal);
    }
  }

  protected Column createBasicColumn(TypeInformation typeInformation, Object fieldVal) {
    Class columnTypeClass = typeInformation.getTypeClass();
    //Todo: Try to use a better way to instead of {if else}?

    boolean columnTypeSupport = true;
    try {
      if (columnTypeClass == StringColumn.class) {
        return getStringColumnValue(fieldVal);
      } else if (columnTypeClass == BooleanColumn.class) {
        return getBooleanColumnValue(fieldVal);
      } else if (columnTypeClass == BytesColumn.class) {
        return getBytesColumnValue(fieldVal);
      } else if (columnTypeClass == LongColumn.class) {
        return new LongColumn(fieldVal == null ? null : fieldVal.toString());
      } else if (columnTypeClass == DateColumn.class) {
        return getDateColumnValue(fieldVal);
      } else if (columnTypeClass == DoubleColumn.class) {
        return getDoubleColumnValue(fieldVal);
      } else {
        columnTypeSupport = false;
        throw BitSailException.asBitSailException(ParserErrorCode.UNSUPPORTED_COLUMN_TYPE, columnTypeClass + " is not supported yet!");
      }
    } catch (BitSailException e) {
      if (columnTypeSupport && this.convertErrorColumnAsNull) {
        return createBasicColumnNull(columnTypeClass);
      } else {
        throw e;
      }
    }
  }

  private StringColumn getStringColumnValue(Object fieldVal) {
    if (fieldVal == null) {
      return new StringColumn();
    }
    String stringVal;
    if (fieldVal instanceof JSONObject) {
      stringVal = JSONObject.toJSONString(fieldVal, serializerFeatures);
    } else {
      stringVal = String.valueOf(fieldVal);
    }
    return new StringColumn(stringVal);
  }

  /**
   * @param columnTypeClass
   * @return
   */
  protected Column createBasicColumnNull(Class columnTypeClass) {
    try {
      return (Column) columnTypeClass.newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param typeInfo
   * @param fieldVal
   * @return
   */
  protected ListColumn<Column> createListColumn(TypeInformation typeInfo, List<?> fieldVal) {

    final ListColumnTypeInfo<?> listTypeInfo = (ListColumnTypeInfo) typeInfo;
    TypeInformation<?> valueTypeInfo = listTypeInfo.getElementTypeInfo();
    ListColumn<Column> lc = new ListColumn<Column>(Column.class);
    if (fieldVal != null) {
      for (int j = 0; j < fieldVal.size(); j++) {
        Column temp = createColumn(valueTypeInfo, fieldVal.get(j));
        lc.add(temp);
      }
    }
    return lc;
  }

  protected MapColumn<Column, Column> createMapColumn(MapColumnTypeInfo<?, ?> typeInfo, Map<?, ?> fieldVal) {
    TypeInformation<?> keyTypeInfo = typeInfo.getKeyTypeInfo();
    TypeInformation<?> valueTypeInfo = typeInfo.getValueTypeInfo();

    MapColumn<Column, Column> mc = new MapColumn<Column, Column>(Column.class, Column.class);
    if (fieldVal != null) {
      fieldVal.forEach((key, value) -> {
        Column keyColumn = createBasicColumn(keyTypeInfo, key);
        Column valueColumn = createColumn(valueTypeInfo, value);
        mc.put(keyColumn, valueColumn);
      });
    }
    return mc;
  }

  protected ArrayMapColumn<Column, Column> createArrayMapColumn(MapColumnTypeInfo typeInfo, Map<?, ?> fieldVal) {
    TypeInformation<?> keyTypeInfo = typeInfo.getKeyTypeInfo();
    TypeInformation<?> valueTypeInfo = typeInfo.getValueTypeInfo();
    List<Column> keys = new ArrayList<>();
    List<Column> values = new ArrayList<>();
    if (fieldVal != null) {
      fieldVal.forEach((key, value) -> {
        Column keyColumn = createBasicColumn(keyTypeInfo, key);
        Column valueColumn = createColumn(valueTypeInfo, value);
        keys.add(keyColumn);
        values.add(valueColumn);
      });
    }
    ArrayMapColumn<Column, Column> mc = new ArrayMapColumn<Column, Column>(keys, values, Column.class, Column.class);
    return mc;
  }

  protected BooleanColumn getBooleanColumnValue(Object fieldVal) {
    if (fieldVal == null) {
      return new BooleanColumn(false);
    }

    if (fieldVal instanceof Boolean) {
      return new BooleanColumn((Boolean) fieldVal);
    }

    return new BooleanColumn(fieldVal.toString());
  }

  protected DoubleColumn getDoubleColumnValue(Object fieldVal) {
    if (fieldVal == null) {
      return new DoubleColumn();
    }

    if (fieldVal instanceof BigDecimal) {
      return new DoubleColumn((BigDecimal) fieldVal);
    }

    return new DoubleColumn(Double.parseDouble(fieldVal.toString()));
  }

  protected BytesColumn getBytesColumnValue(Object fieldVal) {
    if (fieldVal == null) {
      return new BytesColumn();
    }
    if (fieldVal instanceof byte[]) {
      return new BytesColumn((byte[]) fieldVal);
    }

    if (fieldVal instanceof Byte[]) {
      return new BytesColumn(ArrayUtils.toPrimitive((Byte[]) fieldVal));
    }

    return new BytesColumn(fieldVal.toString().getBytes());
  }

  protected DateColumn getDateColumnValue(Object fieldVal) {
    if (fieldVal == null) {
      return new DateColumn();
    }

    if (fieldVal instanceof java.sql.Date) {
      return new DateColumn((java.sql.Date) fieldVal);
    } else if (fieldVal instanceof java.sql.Time) {
      return new DateColumn((java.sql.Time) fieldVal);
    } else if (fieldVal instanceof java.sql.Timestamp) {
      return new DateColumn((java.sql.Timestamp) fieldVal);
    } else if (fieldVal instanceof Date) {
      return new DateColumn(((Date) fieldVal).getTime());
    }

    try {
      StringColumn strColumn = new StringColumn(fieldVal.toString());
      return new DateColumn(ColumnCast.string2Date(strColumn).getTime());
    } catch (Exception e) {
      throw BitSailException.asBitSailException(
          CommonErrorCode.CONVERT_NOT_SUPPORT,
          String.format("String[%s] can't convert to Date.", fieldVal));
    }
  }

  /**
   * Set up raw data column, if index is -1, do nothing.
   *
   * @param row             {@link Row}.
   * @param typeInformation {@link TypeInformation} for {@link Column}.
   * @param index           raw column index.
   * @param bytes           raw data.
   */
  protected void setRawColumn(Row row, TypeInformation typeInformation, int index, byte[] bytes) {
    if (index != -1) {
      Column column = createColumn(typeInformation, bytes);
      row.setField(index, column);
    }
  }

  /**
   * Find index based on column name
   *
   * @param columnInfos column info list.
   * @param columnName  raw column name.
   * @return index for raw column.
   */
  protected int getRawColumnIndexByName(List<ColumnInfo> columnInfos, String columnName) {
    if (StringUtils.isNotBlank(columnName) && CollectionUtils.isNotEmpty(columnInfos)) {
      for (int i = 0; i < columnInfos.size(); i++) {
        if (columnName.equals(columnInfos.get(i).getName())) {
          return i;
        }
      }
    }
    return -1;
  }

  public abstract Row parse(Row row, byte[] bytes, RowTypeInfo rowTypeInfo) throws Exception;

  public abstract Tuple2<Row, Object> parse(
      Row row, byte[] bytes, RowTypeInfo rowTypeInfo, List<FieldPathUtils.PathInfo> nestFieldNames) throws Exception;
}
