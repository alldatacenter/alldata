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

package com.bytedance.bitsail.flink.core.typeutils.base;

import com.bytedance.bitsail.common.column.DateColumn;

import org.apache.flink.annotation.Internal;
import org.apache.flink.api.common.typeutils.SimpleTypeSerializerSnapshot;
import org.apache.flink.api.common.typeutils.TypeSerializerSnapshot;
import org.apache.flink.api.common.typeutils.base.TypeSerializerSingleton;
import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.DataOutputView;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * @desc:
 */
@Internal
public class DateColumnSerializer extends TypeSerializerSingleton<DateColumn> {

  public static final DateColumnSerializer INSTANCE = new DateColumnSerializer();
  private static final long serialVersionUID = 1L;

  @Override
  public boolean isImmutableType() {
    return false;
  }

  @Override
  public DateColumn createInstance() {
    return new DateColumn();
  }

  @Override
  public DateColumn copy(DateColumn from) {
    if (null == from) {
      return null;
    }
    DateColumn dc = new DateColumn(from.asLong());
    dc.setSubType(from.getSubType());
    return dc;
  }

  @Override
  public DateColumn copy(DateColumn from, DateColumn reuse) {
    return from;
  }

  @Override
  public void copy(DataInputView source, DataOutputView target) throws IOException {
    target.writeLong(source.readLong());
    target.writeLong(source.readInt());
  }

  @Override
  public int getLength() {
    return -1;
  }

  @Override
  public void serialize(DateColumn record, DataOutputView target) throws IOException {
    if (null == record || null == record.getRawData()) {
      target.writeByte(Byte.MIN_VALUE);
    } else {
      target.writeByte(Byte.MAX_VALUE);
      DateColumn.DateType subType = record.getSubType();
      target.writeShort(subType.ordinal());
      if (subType == DateColumn.DateType.LOCAL_DATE) {
        LocalDate rawData = (LocalDate) record.getRawData();
        serializeLocalDate(rawData, target);
      } else if (subType == DateColumn.DateType.LOCAL_TIME) {
        LocalTime rawData = (LocalTime) record.getRawData();
        serializeLocalTime(rawData, target);
      } else if (subType == DateColumn.DateType.LOCAL_DATE_TIME) {
        LocalDateTime rawData = (LocalDateTime) record.getRawData();
        serializeLocalDate(rawData.toLocalDate(), target);
        serializeLocalTime(rawData.toLocalTime(), target);
      } else {
        target.writeLong(record.asLong());
      }
    }
  }

  private static void serializeLocalTime(LocalTime localTime, DataOutputView target) throws IOException {
    target.writeByte(localTime.getHour());
    target.writeByte(localTime.getMinute());
    target.writeByte(localTime.getSecond());
    target.writeInt(localTime.getNano());
  }

  private static void serializeLocalDate(LocalDate localDate, DataOutputView target) throws IOException {
    target.writeInt(localDate.getYear());
    target.writeByte(localDate.getMonthValue());
    target.writeByte(localDate.getDayOfMonth());
  }

  @Override
  public DateColumn deserialize(DateColumn reuse, DataInputView source) throws IOException {
    return deserialize(source);
  }

  @Override
  public DateColumn deserialize(DataInputView source) throws IOException {
    byte symbol = source.readByte();
    if (symbol == Byte.MIN_VALUE) {
      return new DateColumn((Long) null);
    } else {
      DateColumn.DateType dateType = DateColumn.DateType.values()[source.readShort()];
      if (dateType == DateColumn.DateType.LOCAL_DATE) {
        return new DateColumn(deserializeLocalDate(source));
      } else if (dateType == DateColumn.DateType.LOCAL_TIME) {
        return new DateColumn(deserializeLocalTime(source));
      } else if (dateType == DateColumn.DateType.LOCAL_DATE_TIME) {
        return new DateColumn(LocalDateTime.of(deserializeLocalDate(source),
            deserializeLocalTime(source)));
      } else {
        DateColumn dateColumn = new DateColumn(source.readLong());
        dateColumn.setSubType(dateType);
        return dateColumn;
      }
    }
  }

  private static LocalDate deserializeLocalDate(DataInputView source) throws IOException {
    return LocalDate.of(source.readInt(), source.readByte(), source.readByte());
  }

  private static LocalTime deserializeLocalTime(DataInputView source) throws IOException {
    return LocalTime
        .of(source.readByte(), source.readByte(), source.readByte(), source.readInt());
  }

  @Override
  public TypeSerializerSnapshot<DateColumn> snapshotConfiguration() {
    return new DateColumnSerializerSnapshot();
  }

  // ------------------------------------------------------------------------

  /**
   * Serializer configuration snapshot for compatibility and format evolution.
   */
  @SuppressWarnings("WeakerAccess")
  public static final class DateColumnSerializerSnapshot extends SimpleTypeSerializerSnapshot<DateColumn> {

    public DateColumnSerializerSnapshot() {
      super(() -> INSTANCE);
    }
  }
}
