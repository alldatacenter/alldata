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

package com.bytedance.bitsail.conversion.hive;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.column.ArrayMapColumn;
import com.bytedance.bitsail.common.column.Column;
import com.bytedance.bitsail.common.column.ListColumn;
import com.bytedance.bitsail.common.column.MapColumn;
import com.bytedance.bitsail.common.exception.CommonErrorCode;

import com.bytedance.bitsail.shaded.hive.shim.HiveShim;

import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector.PrimitiveCategory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BitSailColumnConversion {
  public static HiveShim hiveShim;
  public static ConvertToHiveObjectOptions options;

  public static synchronized void init(HiveShim convertHiveShim, ConvertToHiveObjectOptions convertOptions) {
    hiveShim = convertHiveShim;
    options = convertOptions;
  }

  public static Object toHivePrimitiveObject(Object o, PrimitiveCategory primitiveType) {
    try {
      switch (primitiveType) {
        case STRING:
          return ConvertToHiveObjectOptions.convertToString((Column) o, options);
        default:
          if (((Column) o).getRawData() == null) {
            return null;
          }
      }

      switch (primitiveType) {
        case BOOLEAN:
          return ((Column) o).asBoolean();
        case LONG:
          return toIntOrBigintHiveObject((Column) o, false);
        case INT:
          return toIntOrBigintHiveObject((Column) o, true);
        case SHORT:
          return ((Column) o).asLong().shortValue();
        case FLOAT:
          return ((Column) o).asDouble().floatValue();
        case DOUBLE:
          return ((Column) o).asDouble();
        case BINARY:
          return ((Column) o).asBytes();
        case DECIMAL:
          return ((Column) o).asBigDecimal();
        case DATE:
          return hiveShim.toHiveDate(new java.sql.Date(((Column) o).asDate().getTime()));
        case TIMESTAMP:
          return hiveShim.toHiveTimestamp(new java.sql.Timestamp(((Column) o).asDate().getTime()));
        default:
          throw BitSailException.asBitSailException(CommonErrorCode.UNSUPPORTED_ENCODING, "Column type is illegal:" + primitiveType.name());
      }
    } catch (Exception e) {
      if (options.isConvertErrorColumnAsNull()) {
        return toHiveNull(o);
      } else {
        throw BitSailException.asBitSailException(CommonErrorCode.RUNTIME_ERROR, "Error while creating basic writable value", e);
      }
    }
  }

  public static Object toIntOrBigintHiveObject(Column column, boolean intOrBigInt) {
    return ConvertToHiveObjectOptions.toIntOrBigintHiveObject(column, intOrBigInt, options);
  }

  public static Object toHiveString(Object o) {
    return toHivePrimitiveObject(o, PrimitiveCategory.STRING);
  }

  public static Object toHiveBoolean(Object o) {
    return toHivePrimitiveObject(o, PrimitiveCategory.BOOLEAN);
  }

  public static Object toHiveLong(Object o) {
    return toHivePrimitiveObject(o, PrimitiveCategory.LONG);
  }

  public static Object toHiveInt(Object o) {
    return toHivePrimitiveObject(o, PrimitiveCategory.INT);
  }

  public static Object toHiveShort(Object o) {
    return toHivePrimitiveObject(o, PrimitiveCategory.SHORT);
  }

  public static Object toHiveFloat(Object o) {
    return toHivePrimitiveObject(o, PrimitiveCategory.FLOAT);
  }

  public static Object toHiveDouble(Object o) {
    return toHivePrimitiveObject(o, PrimitiveCategory.DOUBLE);
  }

  public static Object toHiveBytes(Object o) {
    return toHivePrimitiveObject(o, PrimitiveCategory.BINARY);
  }

  public static Object toHiveBigDecimal(Object o) {
    return toHivePrimitiveObject(o, PrimitiveCategory.DECIMAL);
  }

  public static Object toHiveMap(Object o) {
    if (o instanceof ArrayMapColumn) {
      ArrayMapColumn mapColumn = (ArrayMapColumn) o;
      List<Column> keys = mapColumn.getKeys();
      List<Column> values = mapColumn.getValues();
      int len = keys.size();
      Map<Column, Column> map = new HashMap<>(len);
      for (int i = 0; i < len; ++i) {
        map.put(keys.get(i), values.get(i));
      }
      return map;
    }
    return ((MapColumn) o).getColumnRawData();
  }

  public static Object toHiveArray(Object o) {
    return ((ListColumn) o).getColumnRawData();
  }

  public static Object toHiveNull(Object o) {
    return null;
  }

  public static Object toHiveDate(Object o) {
    return toHivePrimitiveObject(o, PrimitiveCategory.DATE);
  }

  public static Object toHiveTimestamp(Object o) {
    return toHivePrimitiveObject(o, PrimitiveCategory.TIMESTAMP);
  }
}
