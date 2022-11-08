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

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.exception.CommonErrorCode;

import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

public final class TypeInfos {

  public static final TypeInfo<Void> VOID_TYPE_INFO =
      new BasicTypeInfo<>(Void.class);
  public static final TypeInfo<Short> SHORT_TYPE_INFO =
      new BasicTypeInfo<>(Short.class);
  public static final TypeInfo<Integer> INT_TYPE_INFO =
      new BasicTypeInfo<>(Integer.class);
  public static final TypeInfo<Long> LONG_TYPE_INFO =
      new BasicTypeInfo<>(Long.class);
  public static final TypeInfo<Float> FLOAT_TYPE_INFO =
      new BasicTypeInfo<>(Float.class);
  public static final TypeInfo<Double> DOUBLE_TYPE_INFO =
      new BasicTypeInfo<>(Double.class);
  public static final TypeInfo<Boolean> BOOLEAN_TYPE_INFO =
      new BasicTypeInfo<>(Boolean.class);
  public static final TypeInfo<Byte> BYTE_TYPE_INFO =
      new BasicTypeInfo<>(Byte.class);
  public static final TypeInfo<BigInteger> BIG_INTEGER_TYPE_INFO =
      new BasicTypeInfo<>(BigInteger.class);
  public static final TypeInfo<BigDecimal> BIG_DECIMAL_TYPE_INFO =
      new BasicTypeInfo<>(BigDecimal.class);
  public static final TypeInfo<String> STRING_TYPE_INFO =
      new BasicTypeInfo<>(String.class);
  public static final TypeInfo<LocalDate> LOCAL_DATE_TYPE_INFO =
      new BasicTypeInfo<>(LocalDate.class);
  public static final TypeInfo<LocalTime> LOCAL_TIME_TYPE_INFO =
      new BasicTypeInfo<>(LocalTime.class);
  public static final TypeInfo<LocalDateTime> LOCAL_DATE_TIME_TYPE_INFO =
      new BasicTypeInfo<>(LocalDateTime.class);
  @Deprecated
  public static final TypeInfo<Date> SQL_DATE_TYPE_INFO =
      new BasicTypeInfo<>(Date.class);
  @Deprecated
  public static final TypeInfo<Time> SQL_TIME_TYPE_INFO =
      new BasicTypeInfo<>(Time.class);
  @Deprecated
  public static final TypeInfo<Timestamp> SQL_TIMESTAMP_TYPE_INFO =
      new BasicTypeInfo<>(Timestamp.class);

  private static final Map<Class<?>, TypeInfo<?>> SUPPORTED_CLASS_MAP = new HashMap<>();

  static {
    try {
      Field[] fields = FieldUtils.getAllFields(TypeInfos.class);
      for (Field field : fields) {
        if (Modifier.isStatic(field.getModifiers())) {
          Object fieldValue = FieldUtils.readStaticField(TypeInfos.class, field.getName(), true);
          if (fieldValue instanceof TypeInfo) {
            TypeInfo<?> supportedTypeInfo = (TypeInfo<?>) fieldValue;
            SUPPORTED_CLASS_MAP.put(supportedTypeInfo.getTypeClass(), supportedTypeInfo);
          }
        }
      }
    } catch (Exception e) {
      throw BitSailException.asBitSailException(CommonErrorCode.INTERNAL_ERROR,
          String.format("initialize TypeFactory failed, maybe caused by reflect option. the reason is: %s",
              e.getMessage()));
    }
  }
}
