/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.metastore.rdbms.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.drill.metastore.rdbms.exception.RdbmsMetastoreException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Converter utility class which helps to convert Metastore metadata objects from / to string value.
 */
public class ConverterUtil {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final TypeReference<List<String>> LIST_STRING_TYPE_REF = new TypeReference<List<String>>() {
  };
  private static final TypeReference<Map<String, String>> MAP_STRING_STRING_TYPE_REF = new TypeReference<Map<String, String>>() {
  };
  private static final TypeReference<Map<String, Float>> MAP_STRING_FLOAT_TYPE_REF = new TypeReference<Map<String, Float>>() {
  };

  public static <T> String convertToString(T value) {
    try {
      return MAPPER.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new RdbmsMetastoreException("Unable to convert value to String: " + value);
    }
  }

  public static <T> T convertTo(String value, TypeReference<T> typeReference) {
    if (value == null) {
      return null;
    }
    try {
      return MAPPER.readValue(value, typeReference);
    } catch (IOException e) {
      throw new RdbmsMetastoreException(String.format("Unable to convert to %s value: %s",
        typeReference.getType().getTypeName(), value));
    }
  }

  public static List<String> convertToListString(String value) {
    return convertTo(value, LIST_STRING_TYPE_REF);
  }

  public static Map<String, String> convertToMapStringString(String value) {
    return convertTo(value, MAP_STRING_STRING_TYPE_REF);
  }

  public static Map<String, Float> convertToMapStringFloat(String value) {
    return convertTo(value, MAP_STRING_FLOAT_TYPE_REF);
  }
}
