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
package org.apache.drill.exec.store.easy.json.parser;

import com.fasterxml.jackson.core.JsonToken;

/**
 * Description of a JSON value as inferred from looking ahead in
 * the JSON stream. Includes a type (which can be empty for an empty
 * array, or null), and an array size (which is 0 for simple values.)
 * <p>
 * To be clear, this is the JSON parser's best guess at a field type
 * from the input token stream. This is <i>not</i> a description of the
 * desired data type as JSON can only react to what it sees on input.
 */
public class ValueDef {

  /**
   * Description of JSON types as derived from JSON tokens.
   */
  public enum JsonType {
    OBJECT, NULL, BOOLEAN,
    INTEGER, FLOAT, STRING, EMBEDDED_OBJECT,

    /**
     * Indicates an empty array.
     */
    EMPTY,

    /**
     * Indicates an unknown array, appears when replacing the
     * value listener for an array.
     */
    UNKNOWN;

    public boolean isObject() { return this == OBJECT; }

    public boolean isUnknown() {
      return this == NULL || this == EMPTY ||
             this == UNKNOWN;
    }

    public boolean isScalar() {
      return !isObject() && !isUnknown();
    }
  }

  public static final ValueDef UNKNOWN_ARRAY = new ValueDef(JsonType.UNKNOWN, 1);
  public static final ValueDef UNKNOWN = new ValueDef(JsonType.UNKNOWN);

  private final int arrayDims;
  private final JsonType type;

  public ValueDef(JsonType type) {
    this(type, 0);
  }

  public ValueDef(JsonType type, int dims) {
    this.type = type;
    this.arrayDims = dims;
  }

  public JsonType type() { return type; }
  public int dimensions() { return arrayDims; }
  public boolean isArray() { return arrayDims > 0; }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder()
        .append(type.name());
    for (int i = 0; i < arrayDims; i++) {
      buf.append("[]");
    }
    return buf.toString();
  }

  public static JsonType jsonTypeFor(JsonToken token) {
    switch (token) {

      case VALUE_NULL:
        return JsonType.NULL;

      case VALUE_FALSE:
      case VALUE_TRUE:
        return JsonType.BOOLEAN;

      case VALUE_NUMBER_INT:
        return JsonType.INTEGER;

      case VALUE_NUMBER_FLOAT:
        return JsonType.FLOAT;

      case VALUE_STRING:
      case VALUE_EMBEDDED_OBJECT:
        return JsonType.STRING;

      default:
        throw new IllegalStateException("Not a scalar type: " + token.name());
    }
  }
}
