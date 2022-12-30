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

import com.bytedance.bitsail.common.column.Column;
import com.bytedance.bitsail.common.column.MapColumn;

import org.apache.flink.api.common.typeutils.CompositeTypeSerializerSnapshot;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.common.typeutils.TypeSerializerSnapshot;

/**
 * @class: MapColumnSerializerSnapshot
 * @desc: Snapshot class for the {@link MapColumnSerializer}.
 **/

public class MapColumnSerializerSnapshot<K extends Column, V extends Column> extends CompositeTypeSerializerSnapshot<MapColumn<K, V>, MapColumnSerializer<K, V>> {

  private static final int CURRENT_VERSION = 1;

  // type of the map's key
  private final Class<K> keyClass;

  // type of the map's value
  private final Class<V> valueClass;

  /**
   * Constructor for read instantiation.
   */
  public MapColumnSerializerSnapshot() {
    super(MapColumnSerializer.class);
    this.keyClass = null;
    this.valueClass = null;
  }

  /**
   * Constructor to create the snapshot for writing.
   */
  public MapColumnSerializerSnapshot(MapColumnSerializer<K, V> mapColumnSerializer, Class<K> keyClass, Class<V> valueClass) {
    super(mapColumnSerializer);
    this.keyClass = keyClass;
    this.valueClass = valueClass;
  }

  @Override
  public int getCurrentOuterSnapshotVersion() {
    return CURRENT_VERSION;
  }

  @Override
  protected MapColumnSerializer<K, V> createOuterSerializerWithNestedSerializers(TypeSerializer<?>[] nestedSerializers) {
    @SuppressWarnings("unchecked")
    TypeSerializer<K> keySerializer = (TypeSerializer<K>) nestedSerializers[0];

    @SuppressWarnings("unchecked")
    TypeSerializer<V> valueSerializer = (TypeSerializer<V>) nestedSerializers[1];

    return new MapColumnSerializer<>(keySerializer, valueSerializer, keyClass, valueClass);
  }

  @Override
  protected TypeSerializer<?>[] getNestedSerializers(MapColumnSerializer<K, V> outerSerializer) {
    return new TypeSerializer<?>[] {outerSerializer.getKeySerializer(), outerSerializer.getValueSerializer()};
  }

  @SuppressWarnings("unchecked")
  public TypeSerializerSnapshot<K> getKeySerializerSnapshot() {
    return (TypeSerializerSnapshot<K>) getNestedSerializerSnapshots()[0];
  }
}

