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

package com.bytedance.bitsail.connector.doris.converter;

import com.bytedance.bitsail.common.row.Row;
import com.bytedance.bitsail.connector.doris.typeinfo.DorisDataType;

import java.io.Serializable;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.TimeZone;

import static org.apache.flink.util.Preconditions.checkNotNull;

public class DorisRowConverter implements Serializable {

  private static final long serialVersionUID = 1L;
  private final SerializationConverter[] serializationConverters;

  public DorisRowConverter(DorisDataType[] dataTypes) {
    checkNotNull(dataTypes);
    this.serializationConverters = new SerializationConverter[dataTypes.length];
    for (int i = 0; i < dataTypes.length; i++) {
      DorisDataType dorisDataType = dataTypes[i];
      serializationConverters[i] = createNullableExternalConverter(dorisDataType);
    }
  }

  /**
   * Convert data from {@link Row}
   * @param row record from bitsail row
   * @param index the field index
   * @return java type value.
   */
  public Object convertExternal(Row row, int index) {
    return serializationConverters[index].serialize(index, row);
  }

  protected SerializationConverter createNullableExternalConverter(DorisDataType type) {
    return wrapIntoNullableExternalConverter(createExternalConverter(type));
  }

  protected SerializationConverter wrapIntoNullableExternalConverter(SerializationConverter serializationConverter) {
    return (index, val) -> {
      if (val == null || val.isNullAt(index)) {
        return null;
      } else {
        return serializationConverter.serialize(index, val);
      }
    };
  }

  /**
   * Runtime converter to convert {@link Row} type object to doris field.
   */
  @FunctionalInterface
  interface SerializationConverter extends Serializable {
    Object serialize(int index, Row field);
  }

  protected SerializationConverter createExternalConverter(DorisDataType type) {
    switch (type) {
      case NULL:
        return ((index, val) -> null);
      case CHAR:
      case VARCHAR:
      case TEXT:
        return (index, val) -> val.getString(index);
      case BOOLEAN:
        return (index, val) -> val.getBoolean(index);
      case BINARY:
      case VARBINARY:
        return (index, val) -> val.getBinary(index);
      case DECIMAL:
      case DECIMALV2:
        final int decimalPrecision = type.getPrecision();
        final int decimalScale = type.getScale();
        return (index, val) -> val.getDecimal(index, decimalPrecision, decimalScale);
      case TINYINT:
      case SMALLINT:
        return (index, val) -> {
          Object value = val.getField(index);
          if (value instanceof Long) {
            return ((Long) value).shortValue();
          }

          if (value instanceof Integer) {
            return ((Integer) value).shortValue();
          }
          return val.getShort(index);
        };
      case INT:
      case INTEGER:
      case INTERVAL_YEAR_MONTH:
      case INTERVAL_DAY_TIME:
        return (index, val) -> {
          Object value = val.getField(index);
          if (value instanceof Long) {
            return ((Long) value).intValue();
          }
          return val.getInt(index);
        };
      case BIGINT:
      case LARGEINT:
        return (index, val) -> {
          Object value = val.getField(index);
          if (value instanceof Integer) {
            return new BigInteger(String.valueOf((int) value));
          }
          if (value instanceof Long) {
            return new BigInteger(String.valueOf((long) value));
          }
          return val.getBigInteger(index);
        };
      case FLOAT:
        return (index, val) -> {
          Object value = val.getField(index);
          if (value instanceof Double) {
            return ((Double) value).floatValue();
          }
          return val.getFloat(index);
        };
      case DOUBLE:
        return (index, val) -> val.getDouble(index);
      case DATE:
        return (index, val) -> {
          Object value = val.getField(index);
          if (value instanceof Integer) {
            return Date.valueOf(LocalDate.ofEpochDay((Integer) value));
          }
          if (value instanceof Long) {
            return Date.valueOf(LocalDate.ofEpochDay((Long) value));
          }
          return val.getDate(index);
        };
      case DATETIME:
      case TIMESTAMP_WITHOUT_TIME_ZONE:
      case TIMESTAMP_WITH_LOCAL_TIME_ZONE:
      case TIMESTAMP_WITH_TIME_ZONE:
        final int timestampPrecision = type.getPrecision();
        return (index, val) -> {
          Object value = val.getField(index);
          if (value instanceof Timestamp) {
            return value;
          }
          if (value instanceof Long) {
            return Timestamp.valueOf(LocalDateTime.ofInstant(Instant.ofEpochSecond((long) value),
                TimeZone.getDefault().toZoneId()));
          }
          return val.getTimestamp(index, timestampPrecision);
        };
      default:
        throw new UnsupportedOperationException("Unsupported type:" + type);
    }
  }

}
