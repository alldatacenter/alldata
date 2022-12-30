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

package com.bytedance.bitsail.common.type.filemapping;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.typeinfo.ListTypeInfo;
import com.bytedance.bitsail.common.typeinfo.MapTypeInfo;
import com.bytedance.bitsail.common.typeinfo.TypeInfo;

/**
 * Created 2022/5/11
 */
public class JdbcTypeInfoConverter extends FileMappingTypeInfoConverter {
  private static final String LIST_TYPE_NAME = "list";
  private static final String MAP_TYPE_NAME = "map";

  public JdbcTypeInfoConverter(String engineName) {
    super(engineName);
  }

  private static boolean isArrayType(String engineType) {
    return engineType.startsWith(LIST_TYPE_NAME);
  }

  public static boolean isMapType(String engineType) {
    return engineType.startsWith(MAP_TYPE_NAME);
  }

  @Override
  public TypeInfo<?> fromTypeString(String typeString) {
    typeString = getBaseName(typeString);
    if (isArrayType(typeString)) {
      return getArrayTypeInfo(typeString);

    } else if (isMapType(typeString)) {
      return getMapTypeInfo(typeString);

    } else {
      return reader.getToTypeInformation().get(typeString);
    }
  }

  public TypeInfo<?> getArrayTypeInfo(String engineType) {
    if (!engineType.endsWith(">")) {
      throw BitSailException.asBitSailException(CommonErrorCode.CONVERT_NOT_SUPPORT,
          "Source engine " + engineName + ", invalid engine list type: " + engineType);
    }
    String elementType = engineType.substring(LIST_TYPE_NAME.length() + 1, engineType.length() - 1);
    TypeInfo<?> elementTypeInfo = fromTypeString(elementType);
    return new ListTypeInfo<>(elementTypeInfo);
  }

  public TypeInfo<?> getMapTypeInfo(String engineType) {
    if (!engineType.endsWith(">") || !engineType.contains(",")) {
      throw BitSailException.asBitSailException(CommonErrorCode.CONVERT_NOT_SUPPORT,
          "Source engine " + engineName + ", invalid engine map type: " + engineType);
    }
    String subString = engineType.substring(MAP_TYPE_NAME.length() + 1, engineType.length() - 1);
    String[] parts = subString.split(",", 2);
    String keyType = parts[0];
    String valueType = parts[1];

    TypeInfo<?> keyTypeInformation = fromTypeString(keyType);
    TypeInfo<?> valueTypeInformation = fromTypeString(valueType);
    return new MapTypeInfo<>(keyTypeInformation, valueTypeInformation);

  }
}
