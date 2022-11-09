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

import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.type.TypeInfoConverter;
import com.bytedance.bitsail.common.typeinfo.TypeInfo;
import com.bytedance.bitsail.flink.core.typeinfo.ListColumnTypeInfo;
import com.bytedance.bitsail.flink.core.typeinfo.MapColumnTypeInfo;

import org.apache.commons.lang.StringUtils;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.RowTypeInfo;

import java.util.List;

/**
 * flink type information helper
 *
 * @desc: type system upgrade, after Plugin improvement, FlinkTypeUtil will be deprecated, please use {@link ColumnFlinkTypeInfoUtil}
 */

public class ColumnFlinkTypeInfoUtil {

  public static RowTypeInfo getRowTypeInformation(TypeInfoConverter converter,
                                                  List<ColumnInfo> columnInfos) {

    String[] fieldNames = new String[columnInfos.size()];
    TypeInformation<?>[] fieldTypes = new TypeInformation[columnInfos.size()];
    for (int index = 0; index < columnInfos.size(); index++) {
      String type = StringUtils.lowerCase(columnInfos.get(index).getType());
      String name = columnInfos.get(index).getName();

      TypeInfo<?> typeInfo = converter.fromTypeString(type);
      fieldNames[index] = name;
      fieldTypes[index] = toColumnFlinkTypeInformation(typeInfo);
    }

    return new RowTypeInfo(fieldTypes, fieldNames);
  }

  public static RowTypeInfo getRowTypeInformation(TypeInfo<?>[] typeInfos) {

    TypeInformation<?>[] fieldTypes = new TypeInformation[typeInfos.length];
    for (int index = 0; index < typeInfos.length; index++) {
      fieldTypes[index] = toColumnFlinkTypeInformation(typeInfos[index]);
    }
    return new RowTypeInfo(fieldTypes);
  }

  private static TypeInformation<?> toColumnFlinkTypeInformation(TypeInfo<?> typeInfo) {
    if (typeInfo instanceof com.bytedance.bitsail.common.typeinfo.MapTypeInfo) {
      com.bytedance.bitsail.common.typeinfo.MapTypeInfo<?, ?> mapTypeInfo = (com.bytedance.bitsail.common.typeinfo.MapTypeInfo<?, ?>) typeInfo;
      TypeInfo<?> keyTypeInfo = mapTypeInfo.getKeyTypeInfo();
      TypeInfo<?> valueTypeInfo = mapTypeInfo.getValueTypeInfo();
      return new MapColumnTypeInfo(toColumnFlinkTypeInformation(keyTypeInfo),
          toColumnFlinkTypeInformation(valueTypeInfo));
    }

    if (typeInfo instanceof com.bytedance.bitsail.common.typeinfo.ListTypeInfo) {
      com.bytedance.bitsail.common.typeinfo.ListTypeInfo<?> listTypeInfo = (com.bytedance.bitsail.common.typeinfo.ListTypeInfo<?>) typeInfo;
      TypeInfo<?> elementTypeInfo = listTypeInfo.getElementTypeInfo();
      return new ListColumnTypeInfo(toColumnFlinkTypeInformation(elementTypeInfo));
    }

    return TypeInfoColumnBridge.bridgeTypeInfo(typeInfo);
  }
}
