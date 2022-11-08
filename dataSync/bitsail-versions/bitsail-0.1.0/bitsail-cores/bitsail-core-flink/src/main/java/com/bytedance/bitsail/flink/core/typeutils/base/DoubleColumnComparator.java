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

package com.bytedance.bitsail.flink.core.typeutils.base;

import com.bytedance.bitsail.common.column.DoubleColumn;

import org.apache.flink.api.common.typeutils.TypeComparator;
import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.MemorySegment;
import org.apache.flink.types.StringValue;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * @desc:
 */
public class DoubleColumnComparator extends ColumnTypeComparator<DoubleColumn> {

  private static final long serialVersionUID = 1L;

  public DoubleColumnComparator(boolean ascending) {
    super(ascending);
  }

  @Override
  public int compareSerialized(DataInputView firstSource, DataInputView secondSource)
      throws IOException {
    BigDecimal b1 = new BigDecimal(StringValue.readString(firstSource));
    BigDecimal b2 = new BigDecimal(StringValue.readString(secondSource));
    int comp = b1.compareTo(b2);
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
  public void putNormalizedKey(DoubleColumn record, MemorySegment target, int offset,
                               int numBytes) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TypeComparator<DoubleColumn> duplicate() {
    return new DoubleColumnComparator(ascendingComparison);
  }
}
