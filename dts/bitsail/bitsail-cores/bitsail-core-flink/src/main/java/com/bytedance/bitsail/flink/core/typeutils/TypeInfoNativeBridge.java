/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.bytedance.bitsail.flink.core.typeutils;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.typeinfo.BasicArrayTypeInfo;
import com.bytedance.bitsail.common.typeinfo.TypeInfo;
import com.bytedance.bitsail.common.typeinfo.TypeInfos;

import com.google.common.collect.Maps;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.LocalTimeTypeInfo;
import org.apache.flink.api.common.typeinfo.PrimitiveArrayTypeInfo;
import org.apache.flink.api.common.typeinfo.SqlTimeTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import java.util.Map;
import java.util.Objects;

/**
 * Bridge class for {@link TypeInfo} and {@link BasicTypeInfo}
 */
public class TypeInfoNativeBridge {

  public static final Map<TypeInfo<?>, TypeInformation<?>> NATIVE_BRIDGE_TYPE_INFO_MAPPING =
      Maps.newHashMap();

  public static final Map<Class<?>, TypeInformation<?>> NATIVE_BRIDGE_CLASS_MAPPING =
      Maps.newHashMap();

  public static final Map<Class<?>, TypeInfo<?>> TYPE_INFO_CLASS_MAPPING =
      Maps.newHashMap();

  static {
    NATIVE_BRIDGE_TYPE_INFO_MAPPING.put(TypeInfos.VOID_TYPE_INFO,
        BasicTypeInfo.VOID_TYPE_INFO);
    NATIVE_BRIDGE_TYPE_INFO_MAPPING.put(TypeInfos.SHORT_TYPE_INFO,
        BasicTypeInfo.SHORT_TYPE_INFO);
    NATIVE_BRIDGE_TYPE_INFO_MAPPING.put(TypeInfos.INT_TYPE_INFO,
        BasicTypeInfo.INT_TYPE_INFO);
    NATIVE_BRIDGE_TYPE_INFO_MAPPING.put(TypeInfos.LONG_TYPE_INFO,
        BasicTypeInfo.LONG_TYPE_INFO);
    NATIVE_BRIDGE_TYPE_INFO_MAPPING.put(TypeInfos.BIG_INTEGER_TYPE_INFO,
        BasicTypeInfo.BIG_INT_TYPE_INFO);
    NATIVE_BRIDGE_TYPE_INFO_MAPPING.put(TypeInfos.FLOAT_TYPE_INFO,
        BasicTypeInfo.FLOAT_TYPE_INFO);
    NATIVE_BRIDGE_TYPE_INFO_MAPPING.put(TypeInfos.DOUBLE_TYPE_INFO,
        BasicTypeInfo.DOUBLE_TYPE_INFO);
    NATIVE_BRIDGE_TYPE_INFO_MAPPING.put(TypeInfos.BIG_DECIMAL_TYPE_INFO,
        BasicTypeInfo.BIG_DEC_TYPE_INFO);
    NATIVE_BRIDGE_TYPE_INFO_MAPPING.put(TypeInfos.STRING_TYPE_INFO,
        BasicTypeInfo.STRING_TYPE_INFO);
    NATIVE_BRIDGE_TYPE_INFO_MAPPING.put(TypeInfos.BOOLEAN_TYPE_INFO,
        BasicTypeInfo.BOOLEAN_TYPE_INFO);
    NATIVE_BRIDGE_TYPE_INFO_MAPPING.put(TypeInfos.BYTE_TYPE_INFO,
        BasicTypeInfo.BYTE_TYPE_INFO);
    NATIVE_BRIDGE_TYPE_INFO_MAPPING.put(TypeInfos.SQL_DATE_TYPE_INFO,
        SqlTimeTypeInfo.DATE);
    NATIVE_BRIDGE_TYPE_INFO_MAPPING.put(TypeInfos.SQL_TIME_TYPE_INFO,
        SqlTimeTypeInfo.TIME);
    NATIVE_BRIDGE_TYPE_INFO_MAPPING.put(TypeInfos.SQL_TIMESTAMP_TYPE_INFO,
        SqlTimeTypeInfo.TIMESTAMP);
    NATIVE_BRIDGE_TYPE_INFO_MAPPING.put(TypeInfos.LOCAL_DATE_TYPE_INFO,
        LocalTimeTypeInfo.LOCAL_DATE);
    NATIVE_BRIDGE_TYPE_INFO_MAPPING.put(TypeInfos.LOCAL_TIME_TYPE_INFO,
        LocalTimeTypeInfo.LOCAL_TIME);
    NATIVE_BRIDGE_TYPE_INFO_MAPPING.put(TypeInfos.LOCAL_DATE_TIME_TYPE_INFO,
        LocalTimeTypeInfo.LOCAL_DATE_TIME);
    NATIVE_BRIDGE_TYPE_INFO_MAPPING.put(BasicArrayTypeInfo.BINARY_TYPE_INFO,
        PrimitiveArrayTypeInfo.BYTE_PRIMITIVE_ARRAY_TYPE_INFO);

    for (TypeInfo<?> typeInfo : NATIVE_BRIDGE_TYPE_INFO_MAPPING.keySet()) {
      NATIVE_BRIDGE_CLASS_MAPPING.put(typeInfo.getTypeClass(),
          NATIVE_BRIDGE_TYPE_INFO_MAPPING.get(typeInfo));
    }

    for (TypeInfo<?> typeInfo : NATIVE_BRIDGE_TYPE_INFO_MAPPING.keySet()) {
      TYPE_INFO_CLASS_MAPPING.put(typeInfo.getTypeClass(), typeInfo);
    }
  }

  public static TypeInformation<?> bridgeTypeInformation(TypeInfo<?> bridge) {
    TypeInformation<?> typeInformation = NATIVE_BRIDGE_CLASS_MAPPING.get(bridge.getTypeClass());
    if (Objects.isNull(typeInformation)) {
      throw BitSailException.asBitSailException(CommonErrorCode.CONVERT_NOT_SUPPORT, String
          .format("Primitive type info %s has no flink bridge type information.", bridge));
    }
    return typeInformation;
  }

  public static TypeInfo<?> bridgeTypeInfo(TypeInformation<?> typeInformation) {
    TypeInfo<?> bridge = TYPE_INFO_CLASS_MAPPING.get(typeInformation.getTypeClass());
    if (Objects.isNull(bridge)) {
      throw BitSailException.asBitSailException(CommonErrorCode.CONVERT_NOT_SUPPORT, String
          .format("Primitive type info %s has no flink bridge type info.", typeInformation));
    }
    return bridge;
  }
}
