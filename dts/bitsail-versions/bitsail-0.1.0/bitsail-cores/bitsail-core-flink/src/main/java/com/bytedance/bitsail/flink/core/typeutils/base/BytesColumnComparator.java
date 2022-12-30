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

import com.bytedance.bitsail.common.column.BytesColumn;

import org.apache.flink.api.common.typeutils.TypeComparator;
import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.MemorySegment;

import java.io.IOException;

/**
 * @desc:
 */
public class BytesColumnComparator extends ColumnTypeComparator<BytesColumn> {

  private static final long serialVersionUID = 1L;

  public BytesColumnComparator(boolean ascending) {
    super(ascending);
  }

  @Override
  public int compareSerialized(DataInputView firstSource, DataInputView secondSource)
      throws IOException {
    int firstLen = firstSource.readInt();
    byte[] firstBytes = new byte[firstLen];
    firstSource.read(firstBytes);

    int secondLen = secondSource.readInt();
    byte[] secondBytes = new byte[secondLen];
    secondSource.read(secondBytes);

    int i = 0;
    int j = 0;
    int comp = 0;
    while (i < firstLen && j < secondLen) {
      byte b1 = firstBytes[i++];
      byte b2 = secondBytes[j++];
      comp = (b1 < b2 ? -1 : (b1 == b2 ? 0 : 1));
      if (comp != 0) {
        break;
      }
    }

    if (comp == 0 && (firstLen != secondLen)) {
      if (firstLen < secondLen) {
        comp = -1;
      } else {
        comp = 1;
      }
    }

    return ascendingComparison ? comp : -comp;
  }

  @Override
  public boolean supportsNormalizedKey() {
    return false;
  }

  @Override
  public int getNormalizeKeyLen() {
    return 0;
  }

  @Override
  public boolean isNormalizedKeyPrefixOnly(int keyBytes) {
    return true;
  }

  @Override
  public void putNormalizedKey(BytesColumn record, MemorySegment target, int offset, int numBytes) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TypeComparator<BytesColumn> duplicate() {
    return new BytesColumnComparator(ascendingComparison);
  }
}
