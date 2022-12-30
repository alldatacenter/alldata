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

package com.bytedance.bitsail.flink.core.typeutils;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.type.BitSailTypeInfoConverter;
import com.bytedance.bitsail.common.type.TypeInfoConverter;
import com.bytedance.bitsail.common.typeinfo.ListTypeInfo;
import com.bytedance.bitsail.common.typeinfo.MapTypeInfo;
import com.bytedance.bitsail.common.typeinfo.TypeInfo;

import org.apache.commons.lang.StringUtils;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;

import java.util.List;

public class NativeFlinkTypeInfoUtil {

  public static RowTypeInfo getRowTypeInformation(List<ColumnInfo> columnInfos) {
    return getRowTypeInformation(columnInfos, new BitSailTypeInfoConverter());
  }

  public static RowTypeInfo getRowTypeInformation(List<ColumnInfo> columnInfos,
                                                  TypeInfoConverter typeInfoConverter) {

    String[] fieldNames = new String[columnInfos.size()];
    TypeInformation<?>[] fieldTypes = new TypeInformation[columnInfos.size()];

    for (int index = 0; index < columnInfos.size(); index++) {
      String type = StringUtils.lowerCase(columnInfos.get(index).getType());
      String name = columnInfos.get(index).getName();

      TypeInfo<?> typeInfo = typeInfoConverter.fromTypeString(type);

      fieldNames[index] = name;
      fieldTypes[index] = toNativeFlinkTypeInformation(typeInfo);
    }

    return new RowTypeInfo(fieldTypes, fieldNames);
  }

  public static TypeInformation<Row> getRowTypeInformation(TypeInfo<?>[] typeInfos) {

    TypeInformation<?>[] fieldTypes = new TypeInformation[typeInfos.length];

    for (int index = 0; index < typeInfos.length; index++) {
      fieldTypes[index] = toNativeFlinkTypeInformation(typeInfos[index]);
    }

    return new RowTypeInfo(fieldTypes);
  }

  private static TypeInformation<?> toNativeFlinkTypeInformation(TypeInfo<?> typeInfo) {
    if (typeInfo instanceof MapTypeInfo) {
      MapTypeInfo<?, ?> mapTypeInfo = (MapTypeInfo<?, ?>) typeInfo;
      TypeInfo<?> keyTypeInfo = mapTypeInfo.getKeyTypeInfo();
      TypeInfo<?> valueTypeInfo = mapTypeInfo.getValueTypeInfo();
      return new org.apache.flink.api.java.typeutils.MapTypeInfo<>(toNativeFlinkTypeInformation(keyTypeInfo),
          toNativeFlinkTypeInformation(valueTypeInfo));
    }

    if (typeInfo instanceof ListTypeInfo) {
      ListTypeInfo<?> listTypeInfo = (ListTypeInfo<?>) typeInfo;
      TypeInfo<?> elementTypeInfo = listTypeInfo.getElementTypeInfo();
      return new org.apache.flink.api.java.typeutils.ListTypeInfo<>(toNativeFlinkTypeInformation(elementTypeInfo));
    }

    return TypeInfoNativeBridge.bridgeTypeInformation(typeInfo);
  }

  public static TypeInfo<?>[] toTypeInfos(TypeInformation<?> typeInformation) {
    if (typeInformation instanceof RowTypeInfo) {
      RowTypeInfo rowTypeInfo = (RowTypeInfo) typeInformation;
      TypeInformation<?>[] fieldTypes = rowTypeInfo.getFieldTypes();
      TypeInfo<?>[] typeInfos = new TypeInfo<?>[fieldTypes.length];
      for (int index = 0; index < fieldTypes.length; index++) {
        typeInfos[index] = toTypeInfo(fieldTypes[index]);
      }
      return typeInfos;
    }

    throw BitSailException.asBitSailException(CommonErrorCode.INTERNAL_ERROR,
        "Only support row type info, code should not enter here.");
  }

  public static TypeInfo<?> toTypeInfo(TypeInformation<?> typeInformation) {
    if (typeInformation instanceof org.apache.flink.api.java.typeutils.MapTypeInfo) {
      org.apache.flink.api.java.typeutils.MapTypeInfo<?, ?> mapTypeInfo =
          (org.apache.flink.api.java.typeutils.MapTypeInfo<?, ?>) typeInformation;
      return new MapTypeInfo<>(
          TypeInfoNativeBridge.bridgeTypeInfo(mapTypeInfo.getKeyTypeInfo()),
          TypeInfoNativeBridge.bridgeTypeInfo(mapTypeInfo.getValueTypeInfo())
      );

    } else if (typeInformation instanceof org.apache.flink.api.java.typeutils.ListTypeInfo) {
      org.apache.flink.api.java.typeutils.ListTypeInfo<?> listTypeInfo =
          (org.apache.flink.api.java.typeutils.ListTypeInfo<?>) typeInformation;
      return new ListTypeInfo<>(
          TypeInfoNativeBridge.bridgeTypeInfo(listTypeInfo.getElementTypeInfo())
      );

    } else {
      return TypeInfoNativeBridge.bridgeTypeInfo(typeInformation);
    }
  }

}
