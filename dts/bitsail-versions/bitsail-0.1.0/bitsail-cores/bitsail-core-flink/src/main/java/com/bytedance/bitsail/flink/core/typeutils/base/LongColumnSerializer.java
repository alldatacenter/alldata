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

import com.bytedance.bitsail.common.column.LongColumn;

import org.apache.flink.api.common.typeutils.SimpleTypeSerializerSnapshot;
import org.apache.flink.api.common.typeutils.TypeSerializerSnapshot;
import org.apache.flink.api.common.typeutils.base.TypeSerializerSingleton;
import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.DataOutputView;

import java.io.IOException;
import java.math.BigInteger;

/**
 * @desc:
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class LongColumnSerializer extends TypeSerializerSingleton<LongColumn> {

  public static final LongColumnSerializer INSTANCE = new LongColumnSerializer();
  public static final LongColumn ZERO = new LongColumn(0L);
  private static final long serialVersionUID = 1L;

  public static void writeBigInteger(BigInteger record, DataOutputView target) throws IOException {
    // null value support
    if (record == null) {
      target.writeInt(0);
      return;
    } else if (record.equals(BigInteger.ZERO)) {
      // fast paths for 0, 1, 10
      // only reference equality is checked because equals would be too expensive
      target.writeInt(1);
      return;
    } else if (record.equals(BigInteger.ONE)) {
      target.writeInt(2);
      return;
    } else if (record.equals(BigInteger.TEN)) {
      target.writeInt(3);
      return;
    }
    // default
    final byte[] bytes = record.toByteArray();
    // the length we write is offset by four, because null and short-paths for ZERO, ONE, and TEN
    target.writeInt(bytes.length + 4);
    target.write(bytes);
  }

  public static BigInteger readBigInteger(DataInputView source) throws IOException {
    final int len = source.readInt();
    if (len < 4) {
      switch (len) {
        case 0:
          return null;
        case 1:
          return BigInteger.ZERO;
        case 2:
          return BigInteger.ONE;
        case 3:
          return BigInteger.TEN;
        default:
          // continue
      }
    }
    final byte[] bytes = new byte[len - 4];
    source.readFully(bytes);
    return new BigInteger(bytes);
  }

  public static boolean copyBigInteger(DataInputView source, DataOutputView target) throws IOException {
    final int len = source.readInt();
    target.writeInt(len);
    if (len > 4) {
      target.write(source, len - 4);
    }
    return len == 0; // returns true if the copied record was null
  }

  @Override
  public boolean isImmutableType() {
    return false;
  }

  @Override
  public LongColumn createInstance() {
    return ZERO;
  }

  @Override
  public LongColumn copy(LongColumn from) {
    return new LongColumn(from.asLong());
  }

  @Override
  public LongColumn copy(LongColumn from, LongColumn reuse) {
    return from;
  }

  @Override
  public void copy(DataInputView source, DataOutputView target) throws IOException {
    copyBigInteger(source, target);
  }

  @Override
  public int getLength() {
    return -1;
  }

  @Override
  public void serialize(LongColumn record, DataOutputView target) throws IOException {
    boolean isNull = record.getRawData() == null;
    target.writeBoolean(isNull);
    if (!isNull) {
      writeBigInteger(record.asBigInteger(), target);
    }
  }

  @Override
  public LongColumn deserialize(DataInputView source) throws IOException {
    boolean isNull = source.readBoolean();
    BigInteger data = null;
    if (!isNull) {
      data = readBigInteger(source);
    }
    return new LongColumn(data);
  }

  // ------------------------------------------------------------------------

  @Override
  public LongColumn deserialize(LongColumn reuse, DataInputView source) throws IOException {
    return deserialize(source);
  }

  @Override
  public TypeSerializerSnapshot<LongColumn> snapshotConfiguration() {
    return new LongColumnSerializerSnapshot();
  }

  /**
   * Serializer configuration snapshot for compatibility and format evolution.
   */
  @SuppressWarnings("WeakerAccess")
  public static final class LongColumnSerializerSnapshot extends SimpleTypeSerializerSnapshot<LongColumn> {

    public LongColumnSerializerSnapshot() {
      super(() -> INSTANCE);
    }
  }
}
