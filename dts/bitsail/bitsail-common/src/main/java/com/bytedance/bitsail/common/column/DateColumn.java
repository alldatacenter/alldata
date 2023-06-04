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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Date;

@SuppressWarnings("checkstyle:MagicNumber")

public class DateColumn extends Column {

  private DateType subType = DateType.DATETIME;

  public DateColumn() {
    this((Long) null);
  }

  public DateColumn(final Long stamp) {
    super(stamp, (null == stamp ? 0 : 8));
  }

  public DateColumn(final Date date) {
    this(date == null ? null : date.getTime());
  }

  public DateColumn(final java.sql.Date date) {
    this(date == null ? null : date.getTime());
    this.setSubType(DateType.DATE);
  }

  public DateColumn(final java.sql.Time time) {
    this(time == null ? null : time.getTime());
    this.setSubType(DateType.TIME);
  }

  public DateColumn(final LocalDate localDate) {
    super(localDate, 8);
    this.setSubType(DateType.LOCAL_DATE);
  }

  public DateColumn(final LocalTime localTime) {
    super(localTime, 7);
    this.setSubType(DateType.LOCAL_TIME);
  }

  public DateColumn(final LocalDateTime localDateTime) {
    super(localDateTime, 15);
    this.setSubType(DateType.LOCAL_DATE_TIME);
  }

  @Override
  public Long asLong() {
    return (Long) this.getRawData();
  }

  @Override
  public String asString() {
    try {
      return ColumnCast.date2String(this);
    } catch (Exception e) {
      throw BitSailException.asBitSailException(
          CommonErrorCode.CONVERT_NOT_SUPPORT,
          String.format("Date[%s] can't convert to String .", this.toString()));
    }
  }

  @Override
  public Date asDate() {
    if (null == this.getRawData()) {
      return null;
    }
    if (getRawData() instanceof LocalDate) {
      LocalDate localDate = ((LocalDate) getRawData());
      return new Date(localDate.atStartOfDay().atZone(ZoneOffset.systemDefault())
          .toInstant().toEpochMilli());
    }
    if (getRawData() instanceof LocalDateTime) {
      LocalDateTime localDateTime = ((LocalDateTime) getRawData());
      return new Date(localDateTime.atZone(ZoneOffset.systemDefault())
          .toInstant().toEpochMilli());
    }
    return new Date((Long) this.getRawData());
  }

  @Override
  public byte[] asBytes() {
    throw BitSailException.asBitSailException(
        CommonErrorCode.CONVERT_NOT_SUPPORT, "Date can't convert to Bytes .");
  }

  @Override
  public Boolean asBoolean() {
    throw BitSailException.asBitSailException(
        CommonErrorCode.CONVERT_NOT_SUPPORT, "Date can't convert to Boolean .");
  }

  @Override
  public Double asDouble() {
    throw BitSailException.asBitSailException(
        CommonErrorCode.CONVERT_NOT_SUPPORT, "Date can't convert to Double .");
  }

  @Override
  public BigInteger asBigInteger() {
    throw BitSailException.asBitSailException(
        CommonErrorCode.CONVERT_NOT_SUPPORT, "Date can't convert to BigInteger .");
  }

  @Override
  public BigDecimal asBigDecimal() {
    throw BitSailException.asBitSailException(
        CommonErrorCode.CONVERT_NOT_SUPPORT, "Date can't convert to BigDecimal .");
  }

  public DateType getSubType() {
    return subType;
  }

  public void setSubType(DateType subType) {
    this.subType = subType;
  }

  @Override
  public int compareTo(Column o) {
    return asDate().compareTo(o.asDate());
  }

  public enum DateType {
    DATE,
    TIME,
    DATETIME,
    LOCAL_TIME,
    LOCAL_DATE,
    LOCAL_DATE_TIME
  }
}
