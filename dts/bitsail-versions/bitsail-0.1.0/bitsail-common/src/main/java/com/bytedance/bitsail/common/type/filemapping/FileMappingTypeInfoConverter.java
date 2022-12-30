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

import com.bytedance.bitsail.common.type.TypeInfoConverter;
import com.bytedance.bitsail.common.typeinfo.TypeInfo;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

public class FileMappingTypeInfoConverter implements TypeInfoConverter {
  @Getter
  protected FileMappingTypeInfoReader reader;

  protected String engineName;

  public FileMappingTypeInfoConverter(String engineName) {
    this(engineName, new FileMappingTypeInfoReader(engineName));
  }

  public FileMappingTypeInfoConverter(String engineName, FileMappingTypeInfoReader reader) {
    this.engineName = engineName;
    this.reader = reader;
  }

  protected static String getBaseName(String typeName) {
    int idx = typeName.indexOf('(');
    if (idx == -1) {
      return typeName;
    } else {
      return typeName.substring(0, idx);
    }
  }

  protected static String trim(String typeName) {
    return StringUtils.replace(StringUtils.trim(typeName), " ", "");
  }

  @Override
  public TypeInfo<?> fromTypeString(String typeString) {
    return reader.getToTypeInformation().get(typeString);
  }

  @Override
  public String fromTypeInfo(TypeInfo<?> typeInfo) {
    return reader.getFromTypeInformation().get(typeInfo);
  }
}
