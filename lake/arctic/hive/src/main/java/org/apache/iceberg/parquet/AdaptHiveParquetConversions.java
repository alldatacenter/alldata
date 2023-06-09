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

package org.apache.iceberg.parquet;

import org.apache.iceberg.expressions.Literal;
import org.apache.iceberg.types.Type;
import org.apache.iceberg.types.Types;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.schema.PrimitiveType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static java.time.Instant.EPOCH;

/**
 * Copy from iceberg {@link org.apache.iceberg.parquet.ParquetConversions} to resolve int96 type.
 */
class AdaptHiveParquetConversions {
  private AdaptHiveParquetConversions() {
  }

  @SuppressWarnings("unchecked")
  static <T> Literal<T> fromParquetPrimitive(Type type, PrimitiveType parquetType, Object value) {
    switch (type.typeId()) {
      case BOOLEAN:
        return (Literal<T>) Literal.of((Boolean) value);
      case INTEGER:
      case DATE:
        return (Literal<T>) Literal.of((Integer) value);
      case LONG:
      case TIME:
      case TIMESTAMP:
        return (Literal<T>) Literal.of((Long) value);
      case FLOAT:
        return (Literal<T>) Literal.of((Float) value);
      case DOUBLE:
        return (Literal<T>) Literal.of((Double) value);
      case STRING:
        Function<Object, Object> stringConversion = converterFromParquet(parquetType);
        return (Literal<T>) Literal.of((CharSequence) stringConversion.apply(value));
      case UUID:
        Function<Object, Object> uuidConversion = converterFromParquet(parquetType);
        return (Literal<T>) Literal.of((UUID) uuidConversion.apply(value));
      case FIXED:
      case BINARY:
        Function<Object, Object> binaryConversion = converterFromParquet(parquetType);
        return (Literal<T>) Literal.of((ByteBuffer) binaryConversion.apply(value));
      case DECIMAL:
        Function<Object, Object> decimalConversion = converterFromParquet(parquetType);
        return (Literal<T>) Literal.of((BigDecimal) decimalConversion.apply(value));
      default:
        throw new IllegalArgumentException("Unsupported primitive type: " + type);
    }
  }

  static Function<Object, Object> converterFromParquet(PrimitiveType parquetType, Type icebergType) {
    Function<Object, Object> fromParquet = converterFromParquet(parquetType);

    //Change For Arctic:Adapt int 96
    if (parquetType.getPrimitiveTypeName() == PrimitiveType.PrimitiveTypeName.INT96) {
      return binary -> {
        final ByteBuffer byteBuffer = ByteBuffer.wrap(((Binary) binary).getBytes()).order(ByteOrder.LITTLE_ENDIAN);
        final long timeOfDayNanos = byteBuffer.getLong();
        final int julianDay = byteBuffer.getInt();
        Instant instant = Instant
            .ofEpochMilli(TimeUnit.DAYS.toMillis(julianDay - 2_440_588L))
            .plusNanos(timeOfDayNanos);

        if (!((Types.TimestampType) icebergType).shouldAdjustToUTC()) {
          //iceberg org.apache.iceberg.expressions.Literals resolve timestamp without tz use UTC, but in fact it is
          // local time zone
          instant = instant.atZone(ZoneId.systemDefault()).toLocalDateTime().toInstant(
              ZoneOffset.UTC);
        }
        return ChronoUnit.MICROS.between(EPOCH, instant);
      };
    }
    //Change For Arctic

    if (icebergType != null) {
      if (icebergType.typeId() == Type.TypeID.LONG &&
          parquetType.getPrimitiveTypeName() == PrimitiveType.PrimitiveTypeName.INT32) {
        return value -> ((Integer) fromParquet.apply(value)).longValue();
      } else if (icebergType.typeId() == Type.TypeID.DOUBLE &&
          parquetType.getPrimitiveTypeName() == PrimitiveType.PrimitiveTypeName.FLOAT) {
        return value -> ((Float) fromParquet.apply(value)).doubleValue();
      }
    }

    return fromParquet;
  }

  static Function<Object, Object> converterFromParquet(PrimitiveType type) {
    if (type.getOriginalType() != null) {
      switch (type.getOriginalType()) {
        case UTF8:
          // decode to CharSequence to avoid copying into a new String
          return binary -> StandardCharsets.UTF_8.decode(((Binary) binary).toByteBuffer());
        case DECIMAL:
          int scale = type.getDecimalMetadata().getScale();
          switch (type.getPrimitiveTypeName()) {
            case INT32:
            case INT64:
              return num -> BigDecimal.valueOf(((Number) num).longValue(), scale);
            case FIXED_LEN_BYTE_ARRAY:
            case BINARY:
              return bin -> new BigDecimal(new BigInteger(((Binary) bin).getBytes()), scale);
            default:
              throw new IllegalArgumentException(
                  "Unsupported primitive type for decimal: " + type.getPrimitiveTypeName());
          }
        default:
      }
    }

    switch (type.getPrimitiveTypeName()) {
      case FIXED_LEN_BYTE_ARRAY:
      case BINARY:
        return binary -> ByteBuffer.wrap(((Binary) binary).getBytes());
      default:
    }

    return obj -> obj;
  }
}
