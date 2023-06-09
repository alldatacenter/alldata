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
package org.apache.drill.exec.vector;

import io.netty.buffer.DrillBuf;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.expr.holders.BigIntHolder;
import org.apache.drill.exec.expr.holders.BitHolder;
import org.apache.drill.exec.expr.holders.DateHolder;
import org.apache.drill.exec.expr.holders.Decimal18Holder;
import org.apache.drill.exec.expr.holders.Decimal28SparseHolder;
import org.apache.drill.exec.expr.holders.Decimal38SparseHolder;
import org.apache.drill.exec.expr.holders.Decimal9Holder;
import org.apache.drill.exec.expr.holders.Float4Holder;
import org.apache.drill.exec.expr.holders.Float8Holder;
import org.apache.drill.exec.expr.holders.IntHolder;
import org.apache.drill.exec.expr.holders.IntervalDayHolder;
import org.apache.drill.exec.expr.holders.IntervalYearHolder;
import org.apache.drill.exec.expr.holders.NullableBitHolder;
import org.apache.drill.exec.expr.holders.TimeHolder;
import org.apache.drill.exec.expr.holders.TimeStampHolder;
import org.apache.drill.exec.expr.holders.VarCharHolder;
import org.apache.drill.exec.expr.holders.VarDecimalHolder;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.util.DecimalUtility;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;

public class ValueHolderHelper {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ValueHolderHelper.class);

  public static IntHolder getIntHolder(int value) {
    IntHolder holder = new IntHolder();
    holder.value = value;

    return holder;
  }

  public static BigIntHolder getBigIntHolder(long value) {
    BigIntHolder holder = new BigIntHolder();
    holder.value = value;

    return holder;
  }

  public static Float4Holder getFloat4Holder(float value) {
    Float4Holder holder = new Float4Holder();
    holder.value = value;

    return holder;
  }

  public static Float8Holder getFloat8Holder(double value) {
    Float8Holder holder = new Float8Holder();
    holder.value = value;

    return holder;
  }

  public static DateHolder getDateHolder(long value) {
    DateHolder holder = new DateHolder();
    holder.value = value;
    return holder;
  }

  public static TimeHolder getTimeHolder(int value) {
    TimeHolder holder = new TimeHolder();
    holder.value = value;
    return holder;
  }

  public static TimeStampHolder getTimeStampHolder(long value) {
    TimeStampHolder holder = new TimeStampHolder();
    holder.value = value;
    return holder;
  }

  public static BitHolder getBitHolder(int value) {
    BitHolder holder = new BitHolder();
    holder.value = value;

    return holder;
  }

  public static NullableBitHolder getNullableBitHolder(boolean isNull, int value) {
    NullableBitHolder holder = new NullableBitHolder();
    holder.isSet = isNull? 0 : 1;
    if (! isNull) {
      holder.value = value;
    }

    return holder;
  }

  public static VarCharHolder getVarCharHolder(DrillBuf buf, String s){
    VarCharHolder vch = new VarCharHolder();

    byte[] b = s.getBytes(Charsets.UTF_8);
    vch.start = 0;
    vch.end = b.length;
    vch.buffer = buf.reallocIfNeeded(b.length);
    vch.buffer.setBytes(0, b);
    return vch;
  }

  public static VarCharHolder getVarCharHolder(BufferAllocator a, String s){
    VarCharHolder vch = new VarCharHolder();

    byte[] b = s.getBytes(Charsets.UTF_8);
    vch.start = 0;
    vch.end = b.length;
    vch.buffer = a.buffer(b.length);
    vch.buffer.setBytes(0, b);
    return vch;
  }


  public static IntervalYearHolder getIntervalYearHolder(int intervalYear) {
    IntervalYearHolder holder = new IntervalYearHolder();

    holder.value = intervalYear;
    return holder;
  }

  public static IntervalDayHolder getIntervalDayHolder(int days, int millis) {
      IntervalDayHolder dch = new IntervalDayHolder();

      dch.days = days;
      dch.milliseconds = millis;
      return dch;
  }

  public static Decimal9Holder getDecimal9Holder(int decimal, int precision, int scale) {
    Decimal9Holder dch = new Decimal9Holder();

    dch.scale = scale;
    dch.precision = precision;
    dch.value = decimal;

    return dch;
  }

  public static Decimal18Holder getDecimal18Holder(long decimal, int precision, int scale) {
    Decimal18Holder dch = new Decimal18Holder();

    dch.scale = scale;
    dch.precision = precision;
    dch.value = decimal;

    return dch;
  }

  public static Decimal28SparseHolder getDecimal28Holder(DrillBuf buf, String decimal) {
    BigDecimal bigDecimal = new BigDecimal(decimal);

    return getDecimal28Holder(buf, bigDecimal);
  }

  public static Decimal28SparseHolder getDecimal28Holder(DrillBuf buf, BigDecimal bigDecimal) {
    Decimal28SparseHolder dch = new Decimal28SparseHolder();

    dch.scale = bigDecimal.scale();
    dch.precision = bigDecimal.precision();
    Decimal28SparseHolder.setSign(bigDecimal.signum() == -1, dch.start, dch.buffer);
    dch.start = 0;
    dch.buffer = buf.reallocIfNeeded(5 * DecimalUtility.INTEGER_SIZE);
    DecimalUtility
      .getSparseFromBigDecimal(bigDecimal, dch.buffer, dch.start, dch.scale, Decimal28SparseHolder.nDecimalDigits);

    return dch;
  }

  public static Decimal38SparseHolder getDecimal38Holder(DrillBuf buf, String decimal) {
      BigDecimal bigDecimal = new BigDecimal(decimal);

      return getDecimal38Holder(buf, bigDecimal);
  }

  public static Decimal38SparseHolder getDecimal38Holder(DrillBuf buf, BigDecimal bigDecimal) {
    Decimal38SparseHolder dch = new Decimal38SparseHolder();

    dch.scale = bigDecimal.scale();
    dch.precision = bigDecimal.precision();
    Decimal38SparseHolder.setSign(bigDecimal.signum() == -1, dch.start, dch.buffer);
    dch.start = 0;
    dch.buffer = buf.reallocIfNeeded(Decimal38SparseHolder.maxPrecision * DecimalUtility.INTEGER_SIZE);
    DecimalUtility
      .getSparseFromBigDecimal(bigDecimal, dch.buffer, dch.start, dch.scale, Decimal38SparseHolder.nDecimalDigits);

    return dch;
  }

  public static VarDecimalHolder getVarDecimalHolder(DrillBuf buf, String decimal) {
    BigDecimal bigDecimal = new BigDecimal(decimal);

    return getVarDecimalHolder(buf, bigDecimal);
  }

  public static VarDecimalHolder getVarDecimalHolder(DrillBuf buf, BigDecimal bigDecimal) {
    VarDecimalHolder dch = new VarDecimalHolder();

    byte[] bytes = bigDecimal.unscaledValue().toByteArray();
    int length = bytes.length;

    dch.scale = bigDecimal.scale();
    dch.precision = bigDecimal.precision();
    dch.start = 0;
    dch.end = length;
    dch.buffer = buf.reallocIfNeeded(length);
    dch.buffer.setBytes(0, bytes);

    return dch;
  }

  /**
   * Returns list of field names which belong to holder corresponding to the specified {@code TypeProtos.MajorType type}.
   *
   * @param type type of holder whose fields should be returned
   * @return list of field names which belong to holder corresponding to the specified {@code TypeProtos.MajorType type}.
   */
  public static List<String> getHolderParams(TypeProtos.MajorType type) {
    ArrayList<String> result = new ArrayList<>();
    switch (type.getMode()) {
      case OPTIONAL:
        result.add("isSet");
        // fall through
      case REQUIRED:
        switch (type.getMinorType()) {
          case BIGINT:
          case FLOAT4:
          case FLOAT8:
          case INT:
          case MONEY:
          case SMALLINT:
          case TINYINT:
          case UINT1:
          case UINT2:
          case UINT4:
          case UINT8:
          case INTERVALYEAR:
          case DATE:
          case TIME:
          case TIMESTAMP:
          case BIT:
          case DECIMAL9:
          case DECIMAL18:
            result.add("value");
            return result;
          case DECIMAL28DENSE:
          case DECIMAL28SPARSE:
          case DECIMAL38DENSE:
          case DECIMAL38SPARSE:
            result.add("start");
            result.add("buffer");
            result.add("scale");
            result.add("precision");
            return result;
          case INTERVAL: {
            result.add("months");
            result.add("days");
            result.add("milliseconds");
            return result;
          }
          case INTERVALDAY: {
            result.add("days");
            result.add("milliseconds");
            return result;
          }
          case VARDECIMAL:
            result.add("scale");
            result.add("precision");
            // fall through
          case VAR16CHAR:
          case VARBINARY:
          case VARCHAR:
            result.add("start");
            result.add("end");
            result.add("buffer");
            return result;
          case UNION:
            result.add("reader");
            return result;
        }
    }
    return result;
  }
}
