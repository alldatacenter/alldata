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
import com.bytedance.bitsail.common.typeinfo.TypeInfos;

/**
 * Created 2022/5/11
 *
 * @author ke.hao
 */
public class MongoTypeInfoConverter extends FileMappingTypeInfoConverter {

  private static final String OBJECT_TYPE = "object";
  private static final String ARRAY_TYPE = "array";

  public MongoTypeInfoConverter() {
    super("mongodb");
  }

  @Override
  public TypeInfo<?> fromTypeString(String engineType) {
    engineType = trim(engineType);
    if (isBasicType(engineType)) {
      return getBasicTypeInfoFromMongoDBType(engineType);
    } else if (isArrayType(engineType)) {
      return getListTypeInfoFromMongoDBType(engineType);
    } else if (isObjectType(engineType)) {
      return getMapTypeInfoFromMongoDBType(engineType);
    } else {
      throw BitSailException.asBitSailException(CommonErrorCode.CONVERT_NOT_SUPPORT,
          "MongDB engine, invalid MongoDB type: " + engineType);
    }
  }

  private boolean isBasicType(String mongoType) {
    return reader.getToTypeInformation().containsKey(mongoType);
  }

  private boolean isObjectType(String mongoType) {
    return mongoType.startsWith(OBJECT_TYPE);
  }

  private boolean isArrayType(String mongoType) {
    return mongoType.startsWith(ARRAY_TYPE);
  }

  public TypeInfo<?> getListTypeInfoFromMongoDBType(String type) {
    if (type.equals(ARRAY_TYPE)) {
      return new ListTypeInfo<>(TypeInfos.STRING_TYPE_INFO);
    }
    if (!type.endsWith(">")) {
      throw BitSailException.asBitSailException(CommonErrorCode.CONVERT_NOT_SUPPORT,
          "MongoDB engine, invalid MongoDB array type: " + type);
    }
    String elementType = type.substring(ARRAY_TYPE.length() + 1, type.length() - 1);
    TypeInfo<?> elementTypeInformation = getTypeInfo(elementType);
    return new ListTypeInfo<>(elementTypeInformation);

  }

  public TypeInfo<?> getMapTypeInfoFromMongoDBType(String type) {
    if (type.equals(OBJECT_TYPE)) {
      return new MapTypeInfo<>(TypeInfos.STRING_TYPE_INFO, TypeInfos.STRING_TYPE_INFO);
    }
    if (!type.endsWith(">") || !type.contains(",")) {
      throw BitSailException.asBitSailException(CommonErrorCode.CONVERT_NOT_SUPPORT,
          "MongoDB engine, invalid MongoDB map type: " + type);
    }
    String subString = type.substring(OBJECT_TYPE.length() + 1, type.length() - 1);
    String[] parts = subString.split(",", 2);
    String keyType = parts[0];
    String valueType = parts[1];

    TypeInfo<?> keyTypeInformation = getBasicTypeInfoFromMongoDBType(keyType);
    TypeInfo<?> valueTypeInformation = getTypeInfo(valueType);
    return new MapTypeInfo<>(keyTypeInformation, valueTypeInformation);
  }

  public TypeInfo<?> getTypeInfo(String type) {
    type = trim(type);
    if (isBasicType(type)) {
      return getBasicTypeInfoFromMongoDBType(type);
    } else if (isArrayType(type)) {
      return getListTypeInfoFromMongoDBType(type);
    } else if (isObjectType(type)) {
      return getMapTypeInfoFromMongoDBType(type);
    } else {
      throw BitSailException.asBitSailException(CommonErrorCode.CONVERT_NOT_SUPPORT,
          "MongoDB engine, invalid MongoDB type: " + type);
    }
  }

  public TypeInfo<?> getBasicTypeInfoFromMongoDBType(String mongoType) {
    return reader.getToTypeInformation().get(mongoType);
  }
}

