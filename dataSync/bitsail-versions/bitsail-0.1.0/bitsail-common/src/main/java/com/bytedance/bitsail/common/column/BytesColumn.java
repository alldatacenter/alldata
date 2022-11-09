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

package com.bytedance.bitsail.common.column;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.exception.CommonErrorCode;

import org.apache.commons.lang3.ArrayUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

public class BytesColumn extends Column {

  public BytesColumn() {
    this(null);
  }

  public BytesColumn(byte[] bytes) {
    super(ArrayUtils.clone(bytes), null == bytes ? 0
        : bytes.length);
  }

  @Override
  public byte[] asBytes() {
    if (null == this.getRawData()) {
      return null;
    }

    return (byte[]) this.getRawData();
  }

  @Override
  public String asString() {
    if (null == this.getRawData()) {
      return null;
    }

    try {
      return ColumnCast.bytes2String(this);
    } catch (Exception e) {
      throw BitSailException.asBitSailException(
          CommonErrorCode.CONVERT_NOT_SUPPORT,
          String.format("Bytes[%s] can't convert to String .", this.toString()), e);
    }
  }

  @Override
  public Long asLong() {
    throw BitSailException.asBitSailException(
        CommonErrorCode.CONVERT_NOT_SUPPORT, "Bytes can't convert to Long .");
  }

  @Override
  public BigDecimal asBigDecimal() {
    throw BitSailException.asBitSailException(
        CommonErrorCode.CONVERT_NOT_SUPPORT, "Bytes can't convert to BigDecimal .");
  }

  @Override
  public BigInteger asBigInteger() {
    throw BitSailException.asBitSailException(
        CommonErrorCode.CONVERT_NOT_SUPPORT, "Bytes can't convert to BigInteger .");
  }

  @Override
  public Double asDouble() {
    throw BitSailException.asBitSailException(
        CommonErrorCode.CONVERT_NOT_SUPPORT, "Bytes can't convert to Long .");
  }

  @Override
  public Date asDate() {
    throw BitSailException.asBitSailException(
        CommonErrorCode.CONVERT_NOT_SUPPORT, "Bytes can't convert to Date .");
  }

  @Override
  public Boolean asBoolean() {
    throw BitSailException.asBitSailException(
        CommonErrorCode.CONVERT_NOT_SUPPORT, "Bytes can't convert to Boolean .");
  }

  @Override
  public int compareTo(Column o) {
    return asBoolean().compareTo(o.asBoolean());
  }
}
