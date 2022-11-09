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

package com.bytedance.bitsail.common.typeinfo;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class TypeInfoBridge {

  public static final Map<Types, TypeInfo<?>> TYPE_INFO_MAPPING =
      Maps.newHashMap();

  public static final Map<String, TypeInfo<?>> TYPE_INFO_NAME_MAPPING =
      Maps.newHashMap();

  static {
    TYPE_INFO_MAPPING.put(Types.VOID, TypeInfos.VOID_TYPE_INFO);
    TYPE_INFO_MAPPING.put(Types.SHORT, TypeInfos.SHORT_TYPE_INFO);
    TYPE_INFO_MAPPING.put(Types.INT, TypeInfos.INT_TYPE_INFO);
    TYPE_INFO_MAPPING.put(Types.LONG, TypeInfos.LONG_TYPE_INFO);
    TYPE_INFO_MAPPING.put(Types.BIGINT, TypeInfos.LONG_TYPE_INFO);
    TYPE_INFO_MAPPING.put(Types.BIGINTEGER, TypeInfos.BIG_INTEGER_TYPE_INFO);
    TYPE_INFO_MAPPING.put(Types.FLOAT, TypeInfos.FLOAT_TYPE_INFO);
    TYPE_INFO_MAPPING.put(Types.DOUBLE, TypeInfos.DOUBLE_TYPE_INFO);
    TYPE_INFO_MAPPING.put(Types.BIGDECIMAL, TypeInfos.BIG_DECIMAL_TYPE_INFO);
    TYPE_INFO_MAPPING.put(Types.STRING, TypeInfos.STRING_TYPE_INFO);
    TYPE_INFO_MAPPING.put(Types.BOOLEAN, TypeInfos.BOOLEAN_TYPE_INFO);
    TYPE_INFO_MAPPING.put(Types.DATE_DATE, TypeInfos.SQL_DATE_TYPE_INFO);
    TYPE_INFO_MAPPING.put(Types.DATE_TIME, TypeInfos.SQL_TIME_TYPE_INFO);
    TYPE_INFO_MAPPING.put(Types.DATE_DATE_TIME, TypeInfos.SQL_TIMESTAMP_TYPE_INFO);
    TYPE_INFO_MAPPING.put(Types.DATE, TypeInfos.LOCAL_DATE_TYPE_INFO);
    TYPE_INFO_MAPPING.put(Types.TIME, TypeInfos.LOCAL_TIME_TYPE_INFO);
    TYPE_INFO_MAPPING.put(Types.TIMESTAMP, TypeInfos.LOCAL_DATE_TIME_TYPE_INFO);
    TYPE_INFO_MAPPING.put(Types.BYTE, TypeInfos.BYTE_TYPE_INFO);
    TYPE_INFO_MAPPING.put(Types.BYTES, BasicArrayTypeInfo.BINARY_TYPE_INFO);
    TYPE_INFO_MAPPING.put(Types.BINARY, BasicArrayTypeInfo.BINARY_TYPE_INFO);

    for (Types type : TYPE_INFO_MAPPING.keySet()) {
      TYPE_INFO_NAME_MAPPING.put(StringUtils.upperCase(type.name()), TYPE_INFO_MAPPING.get(type));
      if (StringUtils.isNotEmpty(type.getTypeStringNickName())) {
        TYPE_INFO_NAME_MAPPING.put(StringUtils.upperCase(type.getTypeStringNickName()),
            TYPE_INFO_MAPPING.get(type));
      }
    }
  }

  public static TypeInfo<?> bridgeTypeInfo(String typeString) {
    return TYPE_INFO_NAME_MAPPING.get(typeString);
  }

}
