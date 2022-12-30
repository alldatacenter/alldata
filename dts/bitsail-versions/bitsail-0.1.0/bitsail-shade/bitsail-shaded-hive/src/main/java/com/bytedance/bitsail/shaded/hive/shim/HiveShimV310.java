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
 *
 * Original Files: apache/flink(https://github.com/apache/flink)
 * Copyright: Copyright 2014-2022 The Apache Software Foundation
 * SPDX-License-Identifier: Apache License 2.0
 *
 * This file may have been modified by ByteDance Ltd. and/or its affiliates.
 */

package com.bytedance.bitsail.shaded.hive.shim;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.util.Preconditions;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.IMetaStoreClient;
import org.apache.hadoop.hive.metastore.RetryingMetaStoreClient;
import org.apache.hadoop.hive.ql.session.SessionState;
import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.io.Writable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Shim for Hive version 3.1.2.
 */
public class HiveShimV310 extends HiveShimV211 {
  // timestamp classes
  private static Class hiveTimestampClz;
  private static Constructor hiveTimestampConstructor;
  private static Field hiveTimestampLocalDateTime;
  private static Constructor timestampWritableConstructor;

  // date classes
  private static Class hiveDateClz;
  private static Constructor hiveDateConstructor;
  private static Field hiveDateLocalDate;
  private static Constructor dateWritableConstructor;

  private static boolean hiveClassesInited;

  private static void initDateTimeClasses() {
    if (!hiveClassesInited) {
      synchronized (HiveShimV310.class) {
        if (!hiveClassesInited) {
          try {
            hiveTimestampClz = Class.forName("org.apache.hadoop.hive.common.type.Timestamp");
            hiveTimestampConstructor = hiveTimestampClz.getDeclaredConstructor(LocalDateTime.class);
            hiveTimestampConstructor.setAccessible(true);
            hiveTimestampLocalDateTime = hiveTimestampClz.getDeclaredField("localDateTime");
            hiveTimestampLocalDateTime.setAccessible(true);
            timestampWritableConstructor = Class.forName("org.apache.hadoop.hive.serde2.io.TimestampWritableV2")
                .getDeclaredConstructor(hiveTimestampClz);

            hiveDateClz = Class.forName("org.apache.hadoop.hive.common.type.Date");
            hiveDateConstructor = hiveDateClz.getDeclaredConstructor(LocalDate.class);
            hiveDateConstructor.setAccessible(true);
            hiveDateLocalDate = hiveDateClz.getDeclaredField("localDate");
            hiveDateLocalDate.setAccessible(true);
            dateWritableConstructor = Class.forName("org.apache.hadoop.hive.serde2.io.DateWritableV2")
                .getDeclaredConstructor(hiveDateClz);
          } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException e) {
            throw BitSailException.asBitSailException(CommonErrorCode.INTERNAL_ERROR,
                "Failed to get Hive timestamp class and constructor", e);
          }
          hiveClassesInited = true;
        }
      }
    }
  }

  @Override
  public IMetaStoreClient getHiveMetastoreClient(HiveConf hiveConf) {
    try {
      SessionState.start(hiveConf);
      Method method = RetryingMetaStoreClient.class.getMethod("getProxy", Configuration.class, Boolean.TYPE);
      // getProxy is a static method
      return (IMetaStoreClient) method.invoke(null, hiveConf, true);
    } catch (Exception ex) {
      throw BitSailException.asBitSailException(CommonErrorCode.INTERNAL_ERROR, "Failed to create Hive Metastore client", ex);
    }
  }

  @Override
  public Writable hivePrimitiveToWritable(Object value) {
    if (value == null) {
      return null;
    }
    Optional<Writable> optional = javaToWritable(value);
    if (optional.isPresent()) {
      return optional.get();
    }
    try {
      if (getDateDataTypeClass().isInstance(value)) {
        return (Writable) dateWritableConstructor.newInstance(value);
      }
      if (getTimestampDataTypeClass().isInstance(value)) {
        return (Writable) timestampWritableConstructor.newInstance(value);
      }
    } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
      throw BitSailException.asBitSailException(CommonErrorCode.INTERNAL_ERROR, "Failed to create writable objects", e);
    }
    throw BitSailException.asBitSailException(CommonErrorCode.INTERNAL_ERROR, "Unsupported primitive java value of class " + value.getClass().getName());
  }

  @Override
  public Class<?> getDateDataTypeClass() {
    initDateTimeClasses();
    return hiveDateClz;
  }

  @Override
  public Class<?> getTimestampDataTypeClass() {
    initDateTimeClasses();
    return hiveTimestampClz;
  }

  @Override
  public Class<?> getMetaStoreUtilsClass() {
    try {
      return Class.forName("org.apache.hadoop.hive.metastore.utils.MetaStoreUtils");
    } catch (ClassNotFoundException e) {
      throw BitSailException.asBitSailException(CommonErrorCode.INTERNAL_ERROR, "Failed to find class MetaStoreUtils", e);
    }
  }

  /**
   * Converts a hive date instance to  which is expected by .
   */
  @Override
  public Date toDate(Object hiveDate) {
    initDateTimeClasses();
    Preconditions.checkArgument(hiveDateClz.isAssignableFrom(hiveDate.getClass()),
        "Expecting Hive date to be an instance of %s, but actually got %s",
        hiveDateClz.getName(), hiveDate.getClass().getName());
    try {
      LocalDate localDate = (LocalDate) hiveDateLocalDate.get(hiveDate);
      return Date.valueOf(localDate);
    } catch (IllegalAccessException e) {
      throw BitSailException.asBitSailException(CommonErrorCode.RUNTIME_ERROR, "Failed to convert to Flink date", e);
    }
  }

  @Override
  public Object toHiveDate(Object date) {
    if (date == null) {
      return null;
    }
    initDateTimeClasses();
    try {
      LocalDate localDateTime = ((Date) date).toLocalDate();
      return hiveDateConstructor.newInstance(localDateTime);
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw BitSailException.asBitSailException(CommonErrorCode.RUNTIME_ERROR, "Failed to convert to Hive date", e);
    }
  }

  @Override
  public Writable toHiveDateWritable(Date value) {
    if (value == null) {
      return null;
    }
    initDateTimeClasses();
    try {
      LocalDate localDateTime = value.toLocalDate();
      Object hiveDate = hiveDateConstructor.newInstance(localDateTime);
      return (Writable) dateWritableConstructor.newInstance(hiveDate);
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw BitSailException.asBitSailException(CommonErrorCode.RUNTIME_ERROR, "Failed to convert to Hive date", e);
    }
  }

  @Override
  public Object toHiveTimestamp(Object time) {
    if (time == null) {
      return null;
    }
    initDateTimeClasses();
    try {
      LocalDateTime localDateTime = ((Timestamp) time).toLocalDateTime();
      return hiveTimestampConstructor.newInstance(localDateTime);
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw BitSailException.asBitSailException(CommonErrorCode.RUNTIME_ERROR, "Failed to convert to Hive timestamp", e);
    }
  }

  @Override
  public Writable toHiveTimestampWritable(Timestamp time) {
    if (time == null) {
      return null;
    }
    initDateTimeClasses();
    try {
      LocalDateTime localDateTime = time.toLocalDateTime();
      Object hiveTimestamp = hiveTimestampConstructor.newInstance(localDateTime);
      return (Writable) timestampWritableConstructor.newInstance(hiveTimestamp);
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw BitSailException.asBitSailException(CommonErrorCode.RUNTIME_ERROR, "Failed to convert to Hive timestamp", e);
    }
  }

  /**
   * Converts a hive timestamp instance to milliseconds timestamp.
   */
  @Override
  public Timestamp toTimestamp(Object hiveTimestamp) {
    initDateTimeClasses();
    Preconditions.checkArgument(hiveTimestampClz.isAssignableFrom(hiveTimestamp.getClass()),
        "Expecting Hive timestamp to be an instance of %s, but actually got %s",
        hiveTimestampClz.getName(), hiveTimestamp.getClass().getName());
    try {
      LocalDateTime localDateTime = (LocalDateTime) hiveTimestampLocalDateTime.get(hiveTimestamp);
      return Timestamp.valueOf(localDateTime);
    } catch (IllegalAccessException e) {
      throw BitSailException.asBitSailException(CommonErrorCode.RUNTIME_ERROR, "Failed to convert to Flink timestamp", e);
    }
  }

  @Override
  public ArrayWritable getArrayWritable(Writable[] writables) {
    return new ArrayWritable(Writable.class, writables);
  }

  @Override
  public ArrayWritable getComplexValueFromArrayWritable(ArrayWritable writables) {
    return writables;
  }

  @Override
  public String getVersion() {
    return "3.1.0";
  }
}
