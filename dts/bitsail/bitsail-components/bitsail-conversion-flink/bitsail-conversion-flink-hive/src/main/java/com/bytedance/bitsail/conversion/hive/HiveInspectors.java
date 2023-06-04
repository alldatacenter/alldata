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
 *
 * Original Files: apache/flink(https://github.com/apache/flink)
 * Copyright: Copyright 2014-2022 The Apache Software Foundation
 * SPDX-License-Identifier: Apache License 2.0
 *
 * This file may have been modified by ByteDance Ltd. and/or its affiliates.
 */

package com.bytedance.bitsail.conversion.hive;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.exception.CommonErrorCode;

import com.bytedance.bitsail.shaded.hive.shim.HiveShim;

import org.apache.flink.types.Row;
import org.apache.hadoop.hive.common.type.HiveChar;
import org.apache.hadoop.hive.common.type.HiveDecimal;
import org.apache.hadoop.hive.common.type.HiveVarchar;
import org.apache.hadoop.hive.serde2.objectinspector.ListObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.MapObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.StructField;
import org.apache.hadoop.hive.serde2.objectinspector.StructObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.BinaryObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.BooleanObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.ByteObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.DateObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.DoubleObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.FloatObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.HiveCharObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.HiveDecimalObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.HiveVarcharObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.IntObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.LongObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.ShortObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.StringObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.TimestampObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.VoidObjectInspector;
import org.apache.hadoop.hive.serde2.typeinfo.CharTypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.ListTypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.MapTypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.PrimitiveTypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.StructTypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.VarcharTypeInfo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HiveInspectors {

  /**
   * Get conversion for converting Flink object to Hive object from an ObjectInspector and the
   * corresponding Flink DataType.
   */
  public static HiveObjectConversion getConversion(
      ObjectInspector inspector, TypeInfo dataType, HiveShim hiveShim) {
    if (inspector instanceof PrimitiveObjectInspector) {
      HiveObjectConversion conversion;
      if (inspector instanceof BooleanObjectInspector) {
        conversion = BitSailColumnConversion::toHiveBoolean;
      } else if (inspector instanceof StringObjectInspector) {
        conversion = BitSailColumnConversion::toHiveString;
      } else if (inspector instanceof ByteObjectInspector
          || inspector instanceof BinaryObjectInspector) {
        conversion = BitSailColumnConversion::toHiveBytes;
      } else if (inspector instanceof LongObjectInspector) {
        conversion = BitSailColumnConversion::toHiveLong;
      } else if (inspector instanceof IntObjectInspector) {
        conversion = BitSailColumnConversion::toHiveInt;
      } else if (inspector instanceof ShortObjectInspector) {
        conversion = BitSailColumnConversion::toHiveShort;
      } else if (inspector instanceof DoubleObjectInspector) {
        conversion = BitSailColumnConversion::toHiveDouble;
      } else if (inspector instanceof FloatObjectInspector) {
        conversion = BitSailColumnConversion::toHiveFloat;
      } else if (inspector instanceof VoidObjectInspector) {
        conversion = BitSailColumnConversion::toHiveNull;
      } else if (inspector instanceof DateObjectInspector) {
        conversion = BitSailColumnConversion::toHiveDate;
      } else if (inspector instanceof TimestampObjectInspector) {
        conversion = BitSailColumnConversion::toHiveTimestamp;
      } else if (inspector instanceof HiveCharObjectInspector) {
        conversion =
            o ->
                o == null ? null
                    : new HiveChar(
                    (String) BitSailColumnConversion.toHiveString(o), ((CharTypeInfo) dataType).getLength());
      } else if (inspector instanceof HiveVarcharObjectInspector) {
        conversion =
            o ->
                o == null ? null
                    : new HiveVarchar(
                    (String) BitSailColumnConversion.toHiveString(o), ((VarcharTypeInfo) dataType).getLength());
      } else if (inspector instanceof HiveDecimalObjectInspector) {
        conversion = o -> o == null ? null : HiveDecimal.create((BigDecimal) BitSailColumnConversion.toHiveBigDecimal(o));
      } else {
        throw new BitSailException(CommonErrorCode.CONVERT_NOT_SUPPORT,
            "Unsupported primitive object inspector " + inspector.getClass().getName());
      }
      // if the object inspector prefers Writable objects, we should add an extra conversion
      // for that
      // currently this happens for constant arguments for UDFs
      if (((PrimitiveObjectInspector) inspector).preferWritable()) {
        conversion = new WritableHiveObjectConversion(conversion, hiveShim);
      }
      return conversion;
    }

    if (inspector instanceof ListObjectInspector) {
      HiveObjectConversion eleConvert =
          getConversion(
              ((ListObjectInspector) inspector).getListElementObjectInspector(),
              ((ListTypeInfo) dataType).getListElementTypeInfo(),
              hiveShim);
      return o -> {
        if (o == null) {
          return null;
        }
        List<Object> list = (List) BitSailColumnConversion.toHiveArray(o);
        List<Object> result = new ArrayList<>();

        for (Object ele : list) {
          result.add(eleConvert.toHiveObject(ele));
        }
        return result;
      };
    }

    if (inspector instanceof MapObjectInspector) {
      MapObjectInspector mapInspector = (MapObjectInspector) inspector;
      MapTypeInfo kvType = (MapTypeInfo) dataType;

      HiveObjectConversion keyConversion =
          getConversion(
              mapInspector.getMapKeyObjectInspector(), kvType.getMapKeyTypeInfo(), hiveShim);
      HiveObjectConversion valueConversion =
          getConversion(
              mapInspector.getMapValueObjectInspector(),
              kvType.getMapValueTypeInfo(),
              hiveShim);

      return o -> {
        if (o == null) {
          return null;
        }
        Map<Object, Object> map = (Map) BitSailColumnConversion.toHiveMap(o);
        Map<Object, Object> result = new HashMap<>(map.size());

        for (Map.Entry<Object, Object> entry : map.entrySet()) {
          Object key = keyConversion.toHiveObject(entry.getKey());
          if (key == null) {
            throw BitSailException.asBitSailException(CommonErrorCode.CONVERT_NOT_SUPPORT, "Key in Hive MAP is null.");
          }
          result.put(key, valueConversion.toHiveObject(entry.getValue()));
        }
        return result;
      };
    }

    if (inspector instanceof StructObjectInspector) {
      StructObjectInspector structInspector = (StructObjectInspector) inspector;

      List<? extends StructField> structFields = structInspector.getAllStructFieldRefs();

      List<TypeInfo> rowFields = ((StructTypeInfo) dataType).getAllStructFieldTypeInfos();

      HiveObjectConversion[] conversions = new HiveObjectConversion[structFields.size()];
      for (int i = 0; i < structFields.size(); i++) {
        conversions[i] =
            getConversion(
                structFields.get(i).getFieldObjectInspector(),
                rowFields.get(i),
                hiveShim);
      }

      return o -> {
        if (o == null) {
          return null;
        }
        Row row = (Row) o;
        List<Object> result = new ArrayList<>(row.getArity());
        for (int i = 0; i < row.getArity(); i++) {
          result.add(conversions[i].toHiveObject(row.getField(i)));
        }
        return result;
      };
    }

    throw new BitSailException(CommonErrorCode.CONVERT_NOT_SUPPORT,
        String.format(
            "Flink doesn't support convert object conversion for %s yet", inspector));
  }

  public static ObjectInspector getObjectInspector(TypeInfo type) {
    switch (type.getCategory()) {
      case PRIMITIVE:
        PrimitiveTypeInfo primitiveType = (PrimitiveTypeInfo) type;
        return PrimitiveObjectInspectorFactory.getPrimitiveJavaObjectInspector(
            primitiveType);
      case LIST:
        ListTypeInfo listType = (ListTypeInfo) type;
        return ObjectInspectorFactory.getStandardListObjectInspector(
            getObjectInspector(listType.getListElementTypeInfo()));
      case MAP:
        MapTypeInfo mapType = (MapTypeInfo) type;
        return ObjectInspectorFactory.getStandardMapObjectInspector(
            getObjectInspector(mapType.getMapKeyTypeInfo()),
            getObjectInspector(mapType.getMapValueTypeInfo()));
      case STRUCT:
        StructTypeInfo structType = (StructTypeInfo) type;
        List<TypeInfo> fieldTypes = structType.getAllStructFieldTypeInfos();

        List<ObjectInspector> fieldInspectors = new ArrayList<ObjectInspector>();
        for (TypeInfo fieldType : fieldTypes) {
          fieldInspectors.add(getObjectInspector(fieldType));
        }

        return ObjectInspectorFactory.getStandardStructObjectInspector(
            structType.getAllStructFieldNames(), fieldInspectors);
      default:
        throw new BitSailException(CommonErrorCode.CONVERT_NOT_SUPPORT, "Unsupported Hive type category " + type.getCategory());
    }
  }
}
