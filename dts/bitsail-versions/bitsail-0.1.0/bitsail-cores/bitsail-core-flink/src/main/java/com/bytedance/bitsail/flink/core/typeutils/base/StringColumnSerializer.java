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

import org.apache.flink.annotation.Internal;
import org.apache.flink.api.common.typeutils.SimpleTypeSerializerSnapshot;
import org.apache.flink.api.common.typeutils.TypeSerializerSnapshot;
import org.apache.flink.api.common.typeutils.base.TypeSerializerSingleton;
import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.DataOutputView;
import org.apache.flink.types.StringValue;

import java.io.IOException;

/**
 * @desc:
 */
@Internal
public final class StringColumnSerializer extends TypeSerializerSingleton<StringColumn> {

  public static final StringColumnSerializer INSTANCE = new StringColumnSerializer();
  public static final StringColumn EMPTY = new StringColumn("");
  private static final long serialVersionUID = 1L;

  @Override
  public boolean isImmutableType() {
    return false;
  }

  @Override
  public StringColumn createInstance() {
    return EMPTY;
  }

  @Override
  public StringColumn copy(StringColumn from) {
    return new StringColumn(from.asString());
  }

  @Override
  public StringColumn copy(StringColumn from, StringColumn reuse) {
    return from;
  }

  @Override
  public void copy(DataInputView source, DataOutputView target) throws IOException {
    StringValue.copyString(source, target);
  }

  @Override
  public int getLength() {
    return -1;
  }

  @Override
  public void serialize(StringColumn record, DataOutputView target) throws IOException {
    boolean isNull = record.getRawData() == null;
    target.writeBoolean(isNull);
    if (!isNull) {
      StringValue.writeString(record.asString(), target);
    }
  }

  @Override
  public StringColumn deserialize(DataInputView source) throws IOException {
    boolean isNull = source.readBoolean();
    String data = null;
    if (!isNull) {
      data = StringValue.readString(source);
    }
    return new StringColumn(data);
  }

  @Override
  public StringColumn deserialize(StringColumn reuse, DataInputView source) throws IOException {
    return deserialize(source);
  }

  @Override
  public TypeSerializerSnapshot<StringColumn> snapshotConfiguration() {
    return new StringColumnSerializerSnapshot();
  }

  // ------------------------------------------------------------------------

  /**
   * Serializer configuration snapshot for compatibility and format evolution.
   */
  @SuppressWarnings("WeakerAccess")
  public static final class StringColumnSerializerSnapshot extends SimpleTypeSerializerSnapshot<StringColumn> {

    public StringColumnSerializerSnapshot() {
      super(() -> INSTANCE);
    }
  }
}
