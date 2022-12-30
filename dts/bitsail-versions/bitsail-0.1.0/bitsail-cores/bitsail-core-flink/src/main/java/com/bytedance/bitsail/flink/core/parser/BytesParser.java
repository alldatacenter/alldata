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

package com.bytedance.bitsail.flink.core.parser;

import com.bytedance.bitsail.common.BitSailException;
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
import com.bytedance.bitsail.flink.core.typeinfo.ListColumnTypeInfo;
import com.bytedance.bitsail.flink.core.typeinfo.MapColumnTypeInfo;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @desc:
 */
public abstract class BytesParser implements Serializable {

  /**
   * convert dirty data to Null,
   * close in default, child class need to imply in need
   */
  protected boolean convertErrorColumnAsNull = false;

  protected SerializerFeature[] serializerFeatures = new SerializerFeature[0];

  public Column createColumn(TypeInformation<?> typeInformation, Object fieldVal) {
    Class<?> columnTypeClass = typeInformation.getTypeClass();
    // attention to that there is List and Map
    if (columnTypeClass == List.class) {
      if (fieldVal != null && fieldVal.getClass().isArray()) {
        fieldVal = convertArrayToList(fieldVal);
      }
      return createListColumn(typeInformation, (List<?>) fieldVal);
    } else if (columnTypeClass == Map.class) {
      return createMapColumn((MapColumnTypeInfo<?, ?>) typeInformation, (Map<?, ?>) fieldVal);
    } else {
      return createBasicColumn(typeInformation, fieldVal);
    }
  }

  protected List<Object> convertArrayToList(Object fieldVal) {
    Class<?> componentType = fieldVal.getClass().getComponentType();
    if (componentType.isPrimitive()) {
      if (componentType == boolean.class) {
        boolean[] arr = (boolean[]) fieldVal;
        List<Object> ret = new ArrayList<>(arr.length);
        for (boolean e : arr) {
          ret.add(e);
        }
        return ret;
      }
      if (componentType == byte.class) {
        byte[] arr = (byte[]) fieldVal;
        List<Object> ret = new ArrayList<>(arr.length);
        for (byte e : arr) {
          ret.add(e);
        }
        return ret;
      }
      if (componentType == char.class) {
        char[] arr = (char[]) fieldVal;
        List<Object> ret = new ArrayList<>(arr.length);
        for (char e : arr) {
          ret.add(e);
        }
        return ret;
      }
      if (componentType == short.class) {
        short[] arr = (short[]) fieldVal;
        List<Object> ret = new ArrayList<>(arr.length);
        for (short e : arr) {
          ret.add(e);
        }
        return ret;
      }
      if (componentType == int.class) {
        int[] arr = (int[]) fieldVal;
        List<Object> ret = new ArrayList<>(arr.length);
        for (int e : arr) {
          ret.add(e);
        }
        return ret;
      }
      if (componentType == long.class) {
        long[] arr = (long[]) fieldVal;
        List<Object> ret = new ArrayList<>(arr.length);
        for (long e : arr) {
          ret.add(e);
        }
        return ret;
      }
      if (componentType == float.class) {
        float[] arr = (float[]) fieldVal;
        List<Object> ret = new ArrayList<>(arr.length);
        for (float e : arr) {
          ret.add(e);
        }
        return ret;
      }
      if (componentType == double.class) {
        double[] arr = (double[]) fieldVal;
        List<Object> ret = new ArrayList<>(arr.length);
        for (double e : arr) {
          ret.add(e);
        }
        return ret;
      }
      throw new RuntimeException("Unsupported type: " + fieldVal.getClass());
    } else {
      Object[] fieldVal2 = (Object[]) fieldVal;
      return Arrays.asList(fieldVal2);
    }
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  public Column createBasicColumn(TypeInformation<?> typeInformation, Object fieldVal) {
    Class<?> columnTypeClass = typeInformation.getTypeClass();
    boolean columnTypeSupport = true;
    try {
      if (columnTypeClass == StringColumn.class) {
        return getStringColumnValue(fieldVal);
      } else if (columnTypeClass == BooleanColumn.class) {
        return getBooleanColumnValue(fieldVal);
      } else if (columnTypeClass == BytesColumn.class) {
        return getBytesColumnValue(fieldVal);
      } else if (columnTypeClass == LongColumn.class) {
        if (fieldVal instanceof LocalDateTime) {
          return new LongColumn(Timestamp.valueOf((LocalDateTime) fieldVal).getTime() / 1000);
        }
        return new LongColumn(fieldVal == null ? null : fieldVal.toString());
      } else if (columnTypeClass == DateColumn.class) {
        return getDateColumnValue(fieldVal);
      } else if (columnTypeClass == DoubleColumn.class) {
        return getDoubleColumnValue(fieldVal);
      } else {
        columnTypeSupport = false;
        throw BitSailException.asBitSailException(CommonErrorCode.UNSUPPORTED_COLUMN_TYPE, columnTypeClass + " is not supported yet!");
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
   * create empty Column
   *
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

    final ListColumnTypeInfo listTypeInfo = (ListColumnTypeInfo) typeInfo;
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

  protected MapColumn<Column, Column> createMapColumn(MapColumnTypeInfo typeInfo, Map<?, ?> fieldVal) {
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

    // Because DoubleColumn serialization will check if double is overflow, there will convert to double in normal
    // for overflow value, there will convert to dirty data.
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
    /**
     * support java.util.Date,java.sql.Date,java.sql.Time,java.sql.Timestamp.
     */
    if (fieldVal instanceof java.sql.Date) {
      return new DateColumn((java.sql.Date) fieldVal);
    } else if (fieldVal instanceof java.sql.Time) {
      return new DateColumn((java.sql.Time) fieldVal);
    } else if (fieldVal instanceof java.sql.Timestamp) {
      return new DateColumn((java.sql.Timestamp) fieldVal);
    } else if (fieldVal instanceof Date) {
      return new DateColumn(((Date) fieldVal).getTime());
    } else if (fieldVal instanceof LocalDate) {
      return new DateColumn((LocalDate) fieldVal);
    } else if (fieldVal instanceof LocalDateTime) {
      return new DateColumn(((LocalDateTime) fieldVal));
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
   * Parse a byte array into a Row
   *
   * @param row
   * @param bytes
   * @param charsetName
   * @return
   */
  public abstract Row parse(Row row, byte[] bytes, int offset, int numBytes, String charsetName, RowTypeInfo rowTypeInfo)
      throws Exception;

  public abstract Row parse(Row row, Object line, RowTypeInfo rowTypeInfo) throws Exception;

  /**
   * key,value use different parse ways according different encode ways
   *
   * @param row
   * @param rowTypeInfo
   * @param keyBytes
   * @param valueBytes
   * @return
   */
  public Row parseKvData(Row row, RowTypeInfo rowTypeInfo, byte[] keyBytes, byte[] valueBytes) throws Exception {
    throw new UnsupportedOperationException("this method show be accomplished");
  }
}
