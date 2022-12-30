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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

public class LongColumn extends Column {

  public LongColumn(final String data) {
    super(null, 0);
    if (StringUtils.isBlank(data)) {
      return;
    }

    try {
      BigInteger rawData = NumberUtils.createBigDecimal(data)
          .toBigInteger();
      super.setRawData(rawData);

      super.setByteSize(data.length());
    } catch (Exception e) {
      throw BitSailException.asBitSailException(
          CommonErrorCode.CONVERT_NOT_SUPPORT,
          String.format("String[%s] can't convert to Long .", data));
    }
  }

  public LongColumn(Long data) {
    this(null == data ? null : BigInteger.valueOf(data));
  }

  public LongColumn(Integer data) {
    this(null == data ? null : BigInteger.valueOf(data));
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  public LongColumn(BigInteger data) {
    this(data, null == data ? 0 : 8);
  }

  private LongColumn(BigInteger data, int byteSize) {
    super(data, byteSize);
  }

  public LongColumn() {
    this((BigInteger) null);
  }

  @Override
  public BigInteger asBigInteger() {
    if (null == this.getRawData()) {
      return null;
    }

    return (BigInteger) this.getRawData();
  }

  @Override
  public Long asLong() {
    BigInteger rawData = (BigInteger) this.getRawData();
    if (null == rawData) {
      return null;
    }

    OverFlowUtil.validateLongNotOverFlow(rawData);

    return rawData.longValue();
  }

  @Override
  public Double asDouble() {
    if (null == this.getRawData()) {
      return null;
    }

    BigDecimal decimal = this.asBigDecimal();
    OverFlowUtil.validateDoubleNotOverFlow(decimal);

    return decimal.doubleValue();
  }

  @Override
  public Boolean asBoolean() {
    if (null == this.getRawData()) {
      return null;
    }

    return this.asBigInteger().compareTo(BigInteger.ZERO) != 0;
  }

  @Override
  public BigDecimal asBigDecimal() {
    if (null == this.getRawData()) {
      return null;
    }

    return new BigDecimal(this.asBigInteger());
  }

  @Override
  public String asString() {
    if (null == this.getRawData()) {
      return null;
    }
    return this.getRawData().toString();
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  @Override
  public Date asDate() {
    if (null == this.getRawData()) {
      return null;
    }

    //Only support seconds
    return new Date(this.asLong() * 1000);
  }

  @Override
  public byte[] asBytes() {
    throw BitSailException.asBitSailException(
        CommonErrorCode.CONVERT_NOT_SUPPORT, "Long can't convert to Bytes .");
  }

  @Override
  public int compareTo(Column o) {
    return asLong().compareTo(o.asLong());
  }
}
