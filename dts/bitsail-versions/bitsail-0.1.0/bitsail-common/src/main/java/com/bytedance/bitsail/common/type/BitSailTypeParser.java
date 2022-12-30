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

package com.bytedance.bitsail.common.type;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.typeinfo.ListTypeInfo;
import com.bytedance.bitsail.common.typeinfo.MapTypeInfo;
import com.bytedance.bitsail.common.typeinfo.TypeInfo;
import com.bytedance.bitsail.common.typeinfo.TypeInfoBridge;
import com.bytedance.bitsail.common.typeinfo.TypeProperty;
import com.bytedance.bitsail.common.typeinfo.Types;
import com.bytedance.bitsail.common.util.Preconditions;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class BitSailTypeParser {
  private static final Logger LOG = LoggerFactory.getLogger(BitSailTypeParser.class);

  private static final String SPLIT_TOKEN = ",";

  public static String fromTypeInfo(TypeInfo<?> typeInfo) {
    throw new UnsupportedOperationException();
  }

  public static List<TypeProperty> fromTypePropertyString(String typePropertyString) {
    if (StringUtils.isEmpty(typePropertyString)) {
      return null;
    }
    LOG.debug("type property name = {}.", typePropertyString);
    typePropertyString = StringUtils.trim(StringUtils.upperCase(typePropertyString));

    String[] splits = StringUtils.split(typePropertyString, SPLIT_TOKEN);
    List<TypeProperty> typeProperties = Lists.newArrayListWithCapacity(ArrayUtils.getLength(splits));
    for (String split : splits) {
      TypeProperty property = TypeProperty.PROPERTY_MAP.get(StringUtils.trim(split));
      if (Objects.isNull(property)) {
        throw BitSailException.asBitSailException(CommonErrorCode.INTERNAL_ERROR,
            String.format("Not support type property string %s.", typePropertyString));
      }
      typeProperties.add(property);
    }

    return typeProperties;
  }

  public static TypeInfo<?> fromTypeString(String typeString) {
    Preconditions.checkNotNull(typeString,
        String.format("Type string %s can not be null.", typeString));

    LOG.debug("type string = {}.", typeString);
    typeString = StringUtils.trim(StringUtils.upperCase(typeString));

    if (StringUtils.startsWithIgnoreCase(typeString, Types.MAP.name())
        || StringUtils.startsWithIgnoreCase(typeString, Types.LIST.name())) {

      if (StringUtils.startsWithIgnoreCase(typeString, Types.MAP.name())) {

        String[] mapTypeString = parseMapTypeString(typeString);
        return new MapTypeInfo<>(fromTypeString(mapTypeString[0]), fromTypeString(mapTypeString[1]));
      } else {

        String elementTypeString = parseListTypeString(typeString);
        return new ListTypeInfo<>(fromTypeString(elementTypeString));
      }
    }
    TypeInfo<?> typeInfo = TypeInfoBridge.bridgeTypeInfo(typeString);
    if (Objects.isNull(typeInfo)) {
      throw BitSailException.asBitSailException(CommonErrorCode.INTERNAL_ERROR,
          String.format("Not support type string %s.", typeString));
    }
    return typeInfo;
  }

  private static String[] parseMapTypeString(String typeString) {
    String substring = StringUtils.substring(typeString,
        Types.MAP.name().length() + 1,
        typeString.length() - 1);
    return StringUtils.split(substring, SPLIT_TOKEN, 2);
  }

  private static String parseListTypeString(String typeString) {
    return StringUtils.substring(typeString, Types.LIST.name().length() + 1,
        typeString.length() - 1);
  }
}
