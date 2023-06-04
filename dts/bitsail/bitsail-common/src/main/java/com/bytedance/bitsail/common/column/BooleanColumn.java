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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

public class BooleanColumn extends Column {
  public BooleanColumn(Boolean bool) {
    super(bool, 1);
  }

  public BooleanColumn(final String data) {
    this(true);
    this.validate(data);
    if (null == data) {
      this.setRawData(null);
      this.setByteSize(0);
    } else {
      if ("0".equals(data)) {
        this.setRawData(false);
      } else if ("1".equals(data)) {
        this.setRawData(true);
      } else {
        this.setRawData(Boolean.valueOf(data));
      }
      this.setByteSize(1);
    }
    return;
  }

  public BooleanColumn() {
    super(null, 1);
  }

  @Override
  public Boolean asBoolean() {
    if (null == super.getRawData()) {
      return null;
    }

    return (Boolean) super.getRawData();
  }

  @Override
  public Long asLong() {
    if (null == this.getRawData()) {
      return null;
    }

    return this.asBoolean() ? 1L : 0L;
  }

  @Override
  public Double asDouble() {
    if (null == this.getRawData()) {
      return null;
    }

    return this.asBoolean() ? 1.0d : 0.0d;
  }

  @Override
  public String asString() {
    if (null == super.getRawData()) {
      return null;
    }

    return this.asBoolean().toString();
  }

  @Override
  public BigInteger asBigInteger() {
    if (null == this.getRawData()) {
      return null;
    }

    return BigInteger.valueOf(this.asLong());
  }

  @Override
  public BigDecimal asBigDecimal() {
    if (null == this.getRawData()) {
      return null;
    }

    return BigDecimal.valueOf(this.asLong());
  }

  @Override
  public Date asDate() {
    throw BitSailException.asBitSailException(
        CommonErrorCode.CONVERT_NOT_SUPPORT, "Bool can't convert to Date .");
  }

  @Override
  public byte[] asBytes() {
    throw BitSailException.asBitSailException(
        CommonErrorCode.CONVERT_NOT_SUPPORT, "Boolean can't convert to Bytes .");
  }

  private void validate(final String data) {
    if (null == data) {
      return;
    }

    if ("true".equalsIgnoreCase(data) || "false".equalsIgnoreCase(data)) {
      return;
    }

    if ("0".equals(data) || "1".equals(data)) {
      return;
    }

    throw BitSailException.asBitSailException(
        CommonErrorCode.CONVERT_NOT_SUPPORT,
        String.format("String[%s] can't convert to Bool .", data));
  }

  @Override
  public int compareTo(Column o) {
    return asBoolean().compareTo(o.asBoolean());
  }
}
