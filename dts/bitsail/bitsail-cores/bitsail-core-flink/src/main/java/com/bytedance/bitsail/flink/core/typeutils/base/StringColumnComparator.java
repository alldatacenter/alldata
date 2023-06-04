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

import com.bytedance.bitsail.common.column.StringColumn;

import org.apache.flink.api.common.typeutils.TypeComparator;
import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.MemorySegment;
import org.apache.flink.types.StringValue;

import java.io.IOException;

/**
 * @desc:
 */
@SuppressWarnings("checkstyle:MagicNumber")
public final class StringColumnComparator extends ColumnTypeComparator<StringColumn> {

  private static final long serialVersionUID = 1L;

  private static final int HIGH_BIT = 0x1 << 7;

  private static final int HIGH_BIT2 = 0x1 << 13;

  private static final int HIGH_BIT2_MASK = 0x3 << 6;

  public StringColumnComparator(boolean ascending) {
    super(ascending);
  }

  @Override
  public int compareSerialized(DataInputView firstSource, DataInputView secondSource)
      throws IOException {
    String s1 = StringValue.readString(firstSource);
    String s2 = StringValue.readString(secondSource);
    int comp = s1.compareTo(s2);
    return ascendingComparison ? comp : -comp;
  }

  @Override
  public boolean supportsNormalizedKey() {
    return false;
  }

  @Override
  public int getNormalizeKeyLen() {
    return Integer.MAX_VALUE;
  }

  @Override
  public boolean isNormalizedKeyPrefixOnly(int keyBytes) {
    return false;
  }

  @Override
  public void putNormalizedKey(StringColumn record, MemorySegment target, int offset,
                               int len) {
    String strVal = record.asString();

    final int limit = offset + len;
    final int end = strVal.length();
    int pos = 0;

    while (pos < end && offset < limit) {
      char c = strVal.charAt(pos++);
      if (c < HIGH_BIT) {
        target.put(offset++, (byte) c);
      } else if (c < HIGH_BIT2) {
        target.put(offset++, (byte) ((c >>> 7) | HIGH_BIT));
        if (offset < limit) {
          target.put(offset++, (byte) c);
        }
      } else {
        target.put(offset++, (byte) ((c >>> 10) | HIGH_BIT2_MASK));
        if (offset < limit) {
          target.put(offset++, (byte) (c >>> 2));
        }
        if (offset < limit) {
          target.put(offset++, (byte) c);
        }
      }
    }
    while (offset < limit) {
      target.put(offset++, (byte) 0);
    }
  }

  @Override
  public TypeComparator<StringColumn> duplicate() {
    return new StringColumnComparator(ascendingComparison);
  }
}
