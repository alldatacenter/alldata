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

import org.apache.flink.api.common.typeutils.TypeComparator;
import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.MemorySegment;

import java.io.IOException;

/**
 * @desc:
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class DateColumnComparator extends ColumnTypeComparator<DateColumn> {

  private static final long serialVersionUID = 1L;

  public DateColumnComparator(boolean ascending) {
    super(ascending);
  }

  public static int compareSerializedDate(DataInputView firstSource, DataInputView secondSource,
                                          boolean ascendingComparison) throws IOException {
    final byte left = firstSource.readByte();
    final byte right = secondSource.readByte();
    int order = 0;
    if (left != right) {
      order = left < right ? -1 : 1;
    } else {
      if (left == Byte.MAX_VALUE) {
        short leftOrdinal = firstSource.readShort();
        short rightOrdinal = secondSource.readShort();
        DateColumn.DateType leftDataType = DateColumn.DateType.values()[leftOrdinal];
        DateColumn.DateType rightDataType = DateColumn.DateType.values()[rightOrdinal];
        if (leftDataType == rightDataType) {
          if (leftDataType == DateColumn.DateType.LOCAL_DATE) {
            order = compareSerializedLocalDate(firstSource, secondSource);
          } else if (leftDataType == DateColumn.DateType.LOCAL_TIME) {
            order = compareSerializedLocalTime(firstSource, secondSource);
          } else if (leftDataType == DateColumn.DateType.LOCAL_DATE_TIME) {
            order = compareSerializedLocalDate(firstSource, secondSource);
            if (order == 0) {
              order = compareSerializedLocalTime(firstSource, secondSource);
            }
          } else {
            return Long.compare(firstSource.readLong(), secondSource.readLong());
          }
        } else {
          order = Short.compare(leftOrdinal, rightOrdinal);
        }
      }
    }
    return ascendingComparison ? order : -order;
  }

  private static int compareSerializedLocalDate(DataInputView firstSource,
                                                DataInputView secondSource) throws IOException {
    int cmp = firstSource.readInt() - secondSource.readInt();
    if (cmp == 0) {
      cmp = firstSource.readByte() - secondSource.readByte();
      if (cmp == 0) {
        cmp = firstSource.readByte() - secondSource.readByte();
      }
    }
    return cmp;
  }

  private static int compareSerializedLocalTime(DataInputView firstSource,
                                                DataInputView secondSource) throws IOException {
    int cmp = firstSource.readByte() - secondSource.readByte();
    if (cmp == 0) {
      cmp = firstSource.readByte() - secondSource.readByte();
      if (cmp == 0) {
        cmp = firstSource.readByte() - secondSource.readByte();
        if (cmp == 0) {
          cmp = firstSource.readInt() - secondSource.readInt();
        }
      }
    }
    return cmp;
  }

  @Override
  public int compareSerialized(DataInputView firstSource, DataInputView secondSource)
      throws IOException {
    return compareSerializedDate(firstSource, secondSource, ascendingComparison);
  }

  @Override
  public boolean supportsNormalizedKey() {
    return true;
  }

  @Override
  public int getNormalizeKeyLen() {
    return 8;
  }

  @Override
  public boolean isNormalizedKeyPrefixOnly(int keyBytes) {
    return keyBytes < 8;
  }

  @Override
  public void putNormalizedKey(DateColumn record, MemorySegment target, int offset, int numBytes) {
    final long value = record.asDate().getTime() - Long.MIN_VALUE;

    // see IntValue for an explanation of the logic
    if (numBytes == 8) {
      // default case, full normalized key
      target.putLongBigEndian(offset, value);
    } else if (numBytes < 8) {
      for (int i = 0; numBytes > 0; numBytes--, i++) {
        target.put(offset + i, (byte) (value >>> ((7 - i) << 3)));
      }
    } else {
      target.putLongBigEndian(offset, value);
      for (int i = 8; i < numBytes; i++) {
        target.put(offset + i, (byte) 0);
      }
    }
  }

  @Override
  public TypeComparator<DateColumn> duplicate() {
    return new DateColumnComparator(ascendingComparison);
  }
}
