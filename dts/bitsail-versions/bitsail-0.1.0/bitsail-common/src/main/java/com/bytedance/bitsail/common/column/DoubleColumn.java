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
 * Original Files: alibaba/DataX (https://github.com/alibaba/DataX)
 * Copyright: Copyright 1999-2022 Alibaba Group Holding Ltd.
 * SPDX-License-Identifier: Apache License 2.0
 *
 * This file may have been modified by ByteDance Ltd. and/or its affiliates.
 */

package com.bytedance.bitsail.common.column;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.util.OverFlowUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

public class DoubleColumn extends Column {
  private static final String NAN = "NaN";
  private static final String POSITIVE_INFINITY = "+Infinity";
  private static final String NAVIGATE_INFINITY = "-Infinity";
  private static final String INFINITY = "Infinity";

  public DoubleColumn(final String data) {
    this(data, null == data ? 0 : data.length());
    this.validate(data);
  }

  public DoubleColumn(Long data) {
    this(data == null ? (String) null : String.valueOf(data));
  }

  public DoubleColumn(Integer data) {
    this(data == null ? (String) null : String.valueOf(data));
  }

  public DoubleColumn(final Double data) {
    this(convertToString(data));
  }

  public DoubleColumn(final Float data) {
    this(convertToString(data));
  }

  public DoubleColumn(final BigDecimal data) {
    this(null == data ? (String) null : data.toPlainString());
  }

  public DoubleColumn(final BigInteger data) {
    this(null == data ? (String) null : data.toString());
  }

  public DoubleColumn() {
    this((String) null);
  }

  private DoubleColumn(final String data, int byteSize) {
    super(data, byteSize);
  }

  public static String convertToString(final Object data) {
    if (data == null) {
      return null;
    }
    return String.valueOf(data);
  }

  @Override
  public BigDecimal asBigDecimal() {
    if (null == this.getRawData()) {
      return null;
    }

    try {
      return new BigDecimal((String) this.getRawData());
    } catch (NumberFormatException e) {
      throw BitSailException.asBitSailException(
          CommonErrorCode.CONVERT_NOT_SUPPORT,
          String.format("String[%s] can't convert to BigDecimal.",
              this.getRawData()));
    }
  }

  @Override
  public Double asDouble() {
    if (null == this.getRawData()) {
      return null;
    }

    String doubleStr = (String) this.getRawData();

    if (NAN.equals(doubleStr)
        || NAVIGATE_INFINITY.equals(doubleStr)
        || POSITIVE_INFINITY.equals(doubleStr)
        || INFINITY.equals(doubleStr)) {
      return Double.valueOf(doubleStr);
    }

    BigDecimal result = this.asBigDecimal();
    OverFlowUtil.validateDoubleNotOverFlow(result);

    return result.doubleValue();
  }

  @Override
  public Long asLong() {
    if (null == this.getRawData()) {
      return null;
    }

    BigDecimal result = this.asBigDecimal();
    OverFlowUtil.validateLongNotOverFlow(result.toBigInteger());

    return result.longValue();
  }

  @Override
  public BigInteger asBigInteger() {
    if (null == this.getRawData()) {
      return null;
    }

    return this.asBigDecimal().toBigInteger();
  }

  @Override
  public String asString() {
    if (null == this.getRawData()) {
      return null;
    }
    return (String) this.getRawData();
  }

  @Override
  public Boolean asBoolean() {
    throw BitSailException.asBitSailException(
        CommonErrorCode.CONVERT_NOT_SUPPORT, "Double can't convert to Bool .");
  }

  @Override
  public Date asDate() {
    throw BitSailException.asBitSailException(
        CommonErrorCode.CONVERT_NOT_SUPPORT, "Double can't convert to Date.");
  }

  @Override
  public byte[] asBytes() {
    throw BitSailException.asBitSailException(
        CommonErrorCode.CONVERT_NOT_SUPPORT, "Double can't convert to Bytes.");
  }

  private void validate(final String data) {
    if (null == data) {
      return;
    }

    if (data.equalsIgnoreCase("NaN") || data.equalsIgnoreCase("-Infinity")
        || data.equalsIgnoreCase("Infinity")) {
      return;
    }

    try {
      new BigDecimal(data);
    } catch (Exception e) {
      throw BitSailException.asBitSailException(
          CommonErrorCode.CONVERT_NOT_SUPPORT,
          String.format("String[%s] can't convert to Double.", data));
    }
  }

  @Override
  public int compareTo(Column o) {
    return asDouble().compareTo(o.asDouble());
  }
}
