/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.log.data;

import org.apache.iceberg.types.Type;
import org.apache.iceberg.types.Types;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * An internal data structure representing data of array.
 */
public interface LogArrayData {

  boolean getBoolean(int pos);

  int getInt(int pos);

  long getLong(int pos);

  float getFloat(int pos);

  double getDouble(int pos);

  LocalDateTime getTimestamp(int pos);

  Instant getInstant(int pos);

  String getString(int pos);

  byte[] getBinary(int pos);

  BigDecimal getDecimal(int pos);

  Object getStruct(int pos);

  LogArrayData getArray(int pos);

  LogMapData getMap(int pos);

  boolean isNullAt(int pos);

  int size();

  /**
   * Creates an accessor for getting elements in an internal array data structure at the given
   * position.
   *
   * @param field the element field of the array
   */
  static ElementGetter createElementGetter(Types.NestedField field) {
    final ElementGetter elementGetter;
    Type elementType = field.type();
    switch (elementType.typeId()) {
      case BOOLEAN:
        elementGetter = LogArrayData::getBoolean;
        break;
      case INTEGER:
      case DATE:
        elementGetter = LogArrayData::getInt;
        break;
      case LONG:
      case TIME:
        elementGetter = LogArrayData::getLong;
        break;
      case FLOAT:
        elementGetter = LogArrayData::getFloat;
        break;
      case DOUBLE:
        elementGetter = LogArrayData::getDouble;
        break;
      case TIMESTAMP:
        Types.TimestampType timestamp = (Types.TimestampType) elementType;
        if (timestamp.shouldAdjustToUTC()) {
          elementGetter = LogArrayData::getInstant;
        } else {
          elementGetter = LogArrayData::getTimestamp;
        }
        break;
      case STRING:
        elementGetter = LogArrayData::getString;
        break;
      case UUID:
      case FIXED:
      case BINARY:
        elementGetter = LogArrayData::getBinary;
        break;
      case DECIMAL:
        elementGetter = LogArrayData::getDecimal;
        break;
      case LIST:
        elementGetter = LogArrayData::getArray;
        break;
      case MAP:
        elementGetter = LogArrayData::getMap;
        break;
      case STRUCT:
        elementGetter = LogArrayData::getStruct;
        break;
      default:
        throw new UnsupportedOperationException("Not Support to parse type: " + elementType);
    }
    if (field.isRequired()) {
      return elementGetter;
    }
    return (array, pos) -> {
      if (array.isNullAt(pos)) {
        return null;
      }
      return elementGetter.getElementOrNull(array, pos);
    };
  }

  /**
   * Accessor for getting the elements of an array during runtime.
   *
   * @see #createElementGetter(Types.NestedField)
   */
  interface ElementGetter extends Serializable {
    @Nullable
    Object getElementOrNull(LogArrayData array, int pos);
  }

  /**
   * used by log deserialization
   */
  interface Factory extends Serializable {
    LogArrayData create(Object[] array);
  }
}
