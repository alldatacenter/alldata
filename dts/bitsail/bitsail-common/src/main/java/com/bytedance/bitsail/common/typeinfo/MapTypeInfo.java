/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.bytedance.bitsail.common.typeinfo;

import com.bytedance.bitsail.common.util.Preconditions;

import java.util.Map;

public class MapTypeInfo<K, V> extends TypeInfo<Map<K, V>> {

  /* The type information for the keys in the map*/
  private final TypeInfo<K> keyTypeInfo;

  /* The type information for the values in the map */
  private final TypeInfo<V> valueTypeInfo;

  public MapTypeInfo(TypeInfo<K> keyTypeInfo, TypeInfo<V> valueTypeInfo) {
    Preconditions.checkNotNull(keyTypeInfo, "The key type information cannot be null.");
    Preconditions.checkNotNull(valueTypeInfo, "The value type information cannot be null.");
    this.keyTypeInfo = keyTypeInfo;
    this.valueTypeInfo = valueTypeInfo;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Class<Map<K, V>> getTypeClass() {
    return (Class<Map<K, V>>) (Class<?>) Map.class;
  }

  @Override
  public String toString() {
    return "Map<" + keyTypeInfo + ", " + valueTypeInfo + ">";
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof MapTypeInfo) {
      @SuppressWarnings("unchecked")
      MapTypeInfo<K, V> other = (MapTypeInfo<K, V>) obj;

      return keyTypeInfo.equals(other.keyTypeInfo)
          && valueTypeInfo.equals(other.valueTypeInfo);
    } else {
      return false;
    }
  }

  public TypeInfo<K> getKeyTypeInfo() {
    return this.keyTypeInfo;
  }

  public TypeInfo<V> getValueTypeInfo() {
    return this.valueTypeInfo;
  }

  @Override
  public int hashCode() {
    return 31 * keyTypeInfo.hashCode() + valueTypeInfo.hashCode();
  }
}
