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

public class StringColumn extends Column {

  public StringColumn() {
    this((String) null);
  }

  public StringColumn(final String rawData) {
    super(rawData, (null == rawData ? 0 : rawData
        .length()));
  }

  public StringColumn(final Integer rawData) {
    this(String.valueOf(rawData));
  }

  @Override
  public Long asLong() {
    if (null == this.getRawData()) {
      return null;
    }

    this.validateDoubleSpecific((String) this.getRawData());

    try {
      BigInteger integer = this.asBigInteger();
      OverFlowUtil.validateLongNotOverFlow(integer);
      return integer.longValue();
    } catch (Exception e) {
      throw BitSailException.asBitSailException(
          CommonErrorCode.CONVERT_NOT_SUPPORT,
          String.format("String[\"%s\"] can't convert to Long .", this.asString()));
    }
  }

  @Override
  public Double asDouble() {
    if (null == this.getRawData()) {
      return null;
    }

    String data = (String) this.getRawData();
    if ("NaN".equals(data)) {
      return Double.NaN;
    }

    if ("Infinity".equals(data)) {
      return Double.POSITIVE_INFINITY;
    }

    if ("-Infinity".equals(data)) {
      return Double.NEGATIVE_INFINITY;
    }

    BigDecimal decimal = this.asBigDecimal();
    OverFlowUtil.validateDoubleNotOverFlow(decimal);

    return decimal.doubleValue();
  }

  @Override
  public String asString() {
    if (null == this.getRawData()) {
      return null;
    }

    return (String) this.getRawData();
  }

  @Override
  public Date asDate() {
    try {
      return ColumnCast.string2Date(this);
    } catch (Exception e) {
      throw BitSailException.asBitSailException(
          CommonErrorCode.CONVERT_NOT_SUPPORT,
          String.format("String[%s] can't convert to Date.", this.toString()));
    }
  }

  @Override
  public byte[] asBytes() {
    if (null == this.getRawData()) {
      return new byte[0];
    }
    return this.getRawData().toString().getBytes();
  }

  @Override
  public Boolean asBoolean() {
    if (null == this.getRawData()) {
      return null;
    }

    if ("true".equalsIgnoreCase(this.asString())) {
      return true;
    }

    if ("false".equalsIgnoreCase(this.asString())) {
      return false;
    }

    throw BitSailException.asBitSailException(
        CommonErrorCode.CONVERT_NOT_SUPPORT,
        String.format("String[\"%s\"] can't convert to Bool .", this.asString()));
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
          String.format("String[%s] can't convert to Double.",
              (String) this.getRawData()));
    }
  }

  @Override
  public BigInteger asBigInteger() {
    if (null == this.getRawData()) {
      return null;
    }

    this.validateDoubleSpecific((String) this.getRawData());

    try {
      return this.asBigDecimal().toBigInteger();
    } catch (Exception e) {
      throw BitSailException.asBitSailException(
          CommonErrorCode.CONVERT_NOT_SUPPORT, String.format(
              "String[\"%s\"] can't convert to BigInteger .", this.asString()));
    }
  }

  private void validateDoubleSpecific(final String data) {
    if ("NaN".equals(data) || "Infinity".equals(data)
        || "-Infinity".equals(data)) {
      throw BitSailException.asBitSailException(
          CommonErrorCode.CONVERT_NOT_SUPPORT,
          String.format("String[\"%s\"] is a special type of Double which can't convert to other type.", data));
    }

    return;
  }

  @Override
  public int compareTo(Column o) {
    return asString().compareTo(o.asString());
  }
}
