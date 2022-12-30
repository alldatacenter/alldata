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

import org.apache.hadoop.hive.common.type.HiveChar;
import org.apache.hadoop.hive.common.type.HiveDecimal;
import org.apache.hadoop.hive.common.type.HiveVarchar;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.IMetaStoreClient;
import org.apache.hadoop.hive.metastore.RetryingMetaStoreClient;
import org.apache.hadoop.hive.ql.session.SessionState;
import org.apache.hadoop.hive.serde2.io.ByteWritable;
import org.apache.hadoop.hive.serde2.io.DateWritable;
import org.apache.hadoop.hive.serde2.io.DoubleWritable;
import org.apache.hadoop.hive.serde2.io.HiveCharWritable;
import org.apache.hadoop.hive.serde2.io.HiveDecimalWritable;
import org.apache.hadoop.hive.serde2.io.HiveVarcharWritable;
import org.apache.hadoop.hive.serde2.io.ShortWritable;
import org.apache.hadoop.hive.serde2.io.TimestampWritable;
import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

import javax.annotation.Nonnull;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Optional;

public class HiveShimV120 implements HiveShim {
  @Override
  public IMetaStoreClient getHiveMetastoreClient(HiveConf hiveConf) {
    try {
      SessionState.start(hiveConf);
      Method method = RetryingMetaStoreClient.class.getMethod("getProxy", HiveConf.class);
      // getProxy is a static method
      return (IMetaStoreClient) method.invoke(null, (hiveConf));
    } catch (Exception ex) {
      throw BitSailException.asBitSailException(CommonErrorCode.CONFIG_ERROR, "Failed to create Hive Metastore client", ex);
    }
  }

  @Override
  public Writable hivePrimitiveToWritable(Object value) {
    if (value == null) {
      return null;
    }
    Optional<Writable> optional = javaToWritable(value);
    return optional.orElseThrow(() -> BitSailException.asBitSailException(CommonErrorCode.INTERNAL_ERROR,
        "Unsupported primitive java value of class " + value.getClass().getName()));
  }

  @Override
  public Class<?> getDateDataTypeClass() {
    return java.sql.Date.class;
  }

  @Override
  public Class<?> getTimestampDataTypeClass() {
    return java.sql.Timestamp.class;
  }

  Optional<Writable> javaToWritable(@Nonnull Object value) {
    Writable writable = null;
    // in case value is already a Writable
    if (value instanceof Writable) {
      writable = (Writable) value;
    } else if (value instanceof Boolean) {
      writable = new BooleanWritable((Boolean) value);
    } else if (value instanceof Byte) {
      writable = new ByteWritable((Byte) value);
    } else if (value instanceof Short) {
      writable = new ShortWritable((Short) value);
    } else if (value instanceof Integer) {
      writable = new IntWritable((Integer) value);
    } else if (value instanceof Long) {
      writable = new LongWritable((Long) value);
    } else if (value instanceof Float) {
      writable = new FloatWritable((Float) value);
    } else if (value instanceof Double) {
      writable = new DoubleWritable((Double) value);
    } else if (value instanceof String) {
      writable = new Text((String) value);
    } else if (value instanceof HiveChar) {
      writable = new HiveCharWritable((HiveChar) value);
    } else if (value instanceof HiveVarchar) {
      writable = new HiveVarcharWritable((HiveVarchar) value);
    } else if (value instanceof HiveDecimal) {
      writable = new HiveDecimalWritable((HiveDecimal) value);
    } else if (value instanceof Date) {
      writable = new DateWritable((Date) value);
    } else if (value instanceof Timestamp) {
      writable = new TimestampWritable((Timestamp) value);
    } else if (value instanceof BigDecimal) {
      HiveDecimal hiveDecimal = HiveDecimal.create((BigDecimal) value);
      writable = new HiveDecimalWritable(hiveDecimal);
    } else if (value instanceof byte[]) {
      writable = new BytesWritable((byte[]) value);
    }
    return Optional.ofNullable(writable);
  }

  @Override
  public Class<?> getMetaStoreUtilsClass() {
    try {
      return Class.forName("org.apache.hadoop.hive.metastore.MetaStoreUtils");
    } catch (ClassNotFoundException e) {
      throw BitSailException.asBitSailException(CommonErrorCode.INTERNAL_ERROR, "Failed to find class MetaStoreUtils", e);
    }
  }

  /**
   * Converts a hive date instance to  which is expected by .
   */
  @Override
  public Date toDate(Object hiveDate) {
    Preconditions.checkArgument(hiveDate instanceof Date,
        "Expecting Hive Date to be an instance of %s, but actually got %s",
        Date.class.getName(), hiveDate.getClass().getName());
    return (Date) hiveDate;
  }

  @Override
  public Object toHiveDate(Object date) {
    return date;
  }

  @Override
  public Writable toHiveDateWritable(Date value) {
    if (value == null) {
      return null;
    }
    return new DateWritable(value);
  }

  @Override
  public Object toHiveTimestamp(Object time) {
    return time;
  }

  @Override
  public Writable toHiveTimestampWritable(Timestamp time) {
    if (time == null) {
      return null;
    }
    return new TimestampWritable(time);
  }

  /**
   * Converts a hive timestamp instance to milliseconds timestamp.
   */
  @Override
  public Timestamp toTimestamp(Object hiveTimestamp) {
    Preconditions.checkArgument(hiveTimestamp instanceof Timestamp,
        "Expecting Hive timestamp to be an instance of %s, but actually got %s",
        Timestamp.class.getName(), hiveTimestamp.getClass().getName());
    return (Timestamp) hiveTimestamp;
  }

  @Override
  public ArrayWritable getArrayWritable(Writable[] writables) {
    ArrayWritable writable = new ArrayWritable(Writable.class, writables);
    return new ArrayWritable(Writable.class, new Writable[] {writable});
  }

  @Override
  public ArrayWritable getComplexValueFromArrayWritable(ArrayWritable writables) {
    return (ArrayWritable) writables.get()[0];
  }

  @Override
  public String getVersion() {
    return "1.2.0";
  }
}
