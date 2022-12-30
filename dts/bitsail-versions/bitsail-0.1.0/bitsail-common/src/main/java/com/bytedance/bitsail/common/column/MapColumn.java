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

package com.bytedance.bitsail.common.column;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.exception.CommonErrorCode;

import com.alibaba.fastjson.JSON;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MapColumn<K extends Column, V extends Column> extends Column implements Map<K, V> {

  private static final long serialVersionUID = 1L;

  // type of the map's key
  private final Class<K> keyClass;
  // type of the map's value
  private final Class<V> valueClass;
  // encapsulated map
  private final Map<K, V> map;

  /**
   * Initializes the encapsulated map with an empty HashMap.
   */
  public MapColumn(Class<K> keyClass, Class<V> valueClass) {
    super(null, 0);
    //this.keyClass = ReflectionUtil.getTemplateType1(this.getClass());
    //this.valueClass = ReflectionUtil.getTemplateType2(this.getClass());
    this.keyClass = keyClass;
    this.valueClass = valueClass;

    this.map = new HashMap<>();
  }

  /**
   * Initializes the encapsulated map with a HashMap filled with all entries of the provided map.
   *
   * @param map Map holding all entries with which the new encapsulated map is filled.
   */
  public MapColumn(Map<K, V> map, Class<K> keyClass, Class<V> valueClass) {
    super(map, 0);
    //this.keyClass = ReflectionUtil.getTemplateType1(this.getClass());
    //this.valueClass = ReflectionUtil.getTemplateType2(this.getClass());
    this.keyClass = keyClass;
    this.valueClass = valueClass;

    this.map = new HashMap<>(map);
    int size = 0;
    for (Map.Entry<K, V> entry : this.map.entrySet()) {
      size += entry.getKey().getByteSize();
      size += entry.getValue().getByteSize();
    }
    super.setByteSize(size);
  }

  @Override
  public Map<Object, Object> getRawData() {
    HashMap<Object, Object> m = new HashMap<>();
    for (Map.Entry<K, V> entry : this.map.entrySet()) {
      m.put(entry.getKey().getRawData(), entry.getValue().getRawData());
    }
    return m;
  }

  public Map<K, V> getColumnRawData() {
    return this.map;
  }

  @Override
  public int getByteSize() {
    int size = 0;
    for (Map.Entry<K, V> entry : this.map.entrySet()) {
      size += entry.getKey().getByteSize();
      size += entry.getValue().getByteSize();
    }
    return size;
  }

  @Override
  public String asString() {
    return JSON.toJSONString(this.getRawData());
  }

  @Override
  public byte[] asBytes() {
    throw BitSailException.asBitSailException(
        CommonErrorCode.CONVERT_NOT_SUPPORT, "Map can't convert to Bytes .");
  }

  @Override
  public Date asDate() {
    throw BitSailException.asBitSailException(
        CommonErrorCode.CONVERT_NOT_SUPPORT, "Map can't convert to Date .");
  }

  @Override
  public BigInteger asBigInteger() {
    throw BitSailException.asBitSailException(
        CommonErrorCode.CONVERT_NOT_SUPPORT, "Map can't convert to BigInteger .");
  }

  @Override
  public BigDecimal asBigDecimal() {
    throw BitSailException.asBitSailException(
        CommonErrorCode.CONVERT_NOT_SUPPORT, "Map can't convert to BigDecimal .");
  }

  @Override
  public Double asDouble() {
    throw BitSailException.asBitSailException(
        CommonErrorCode.CONVERT_NOT_SUPPORT, "Map can't convert to Double .");
  }

  @Override
  public Boolean asBoolean() {
    throw BitSailException.asBitSailException(
        CommonErrorCode.CONVERT_NOT_SUPPORT, "Long can't convert to Boolean .");
  }

  @Override
  public Long asLong() {
    throw BitSailException.asBitSailException(
        CommonErrorCode.CONVERT_NOT_SUPPORT, "Long can't convert to Long .");
  }

  @Override
  public int compareTo(Column o) {
    return asString().compareTo(o.asString());
  }

  /*
   * (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return JSON.toJSONString(this.map);
    //return JSON.toJSONString(this.getRawData());
    //return this.map.toString();
  }

  /*
   * (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return this.map.hashCode();
  }

  /*
   * (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (this.getClass() != obj.getClass()) {
      return false;
    }
    final MapColumn<?, ?> other = (MapColumn<?, ?>) obj;
    if (this.map == null) {
      if (other.map != null) {
        return false;
      }
    } else if (!this.map.equals(other.map)) {
      return false;
    }
    return true;
  }

  /*
   * (non-Javadoc)
   * @see java.util.Map#clear()
   */
  @Override
  public void clear() {
    this.map.clear();
  }

  /*
   * (non-Javadoc)
   * @see java.util.Map#containsKey(java.lang.Object)
   */
  @Override
  public boolean containsKey(final Object key) {
    return this.map.containsKey(key);
  }

  /*
   * (non-Javadoc)
   * @see java.util.Map#containsValue(java.lang.Object)
   */
  @Override
  public boolean containsValue(final Object value) {
    return this.map.containsValue(value);
  }

  /*
   * (non-Javadoc)
   * @see java.util.Map#entrySet()
   */
  @Override
  public Set<Entry<K, V>> entrySet() {
    return this.map.entrySet();
  }

  /*
   * (non-Javadoc)
   * @see java.util.Map#get(java.lang.Object)
   */
  @Override
  public V get(final Object key) {
    return this.map.get(key);
  }

  /*
   * (non-Javadoc)
   * @see java.util.Map#isEmpty()
   */
  @Override
  public boolean isEmpty() {
    return this.map.isEmpty();
  }

  /*
   * (non-Javadoc)
   * @see java.util.Map#keySet()
   */
  @Override
  public Set<K> keySet() {
    return this.map.keySet();
  }

  /*
   * (non-Javadoc)
   * @see java.util.Map#put(java.lang.Object, java.lang.Object)
   */
  @Override
  public V put(final K key, final V value) {
    return this.map.put(key, value);
  }

  /*
   * (non-Javadoc)
   * @see java.util.Map#putAll(java.util.Map)
   */
  @Override
  public void putAll(final Map<? extends K, ? extends V> m) {
    this.map.putAll(m);
  }

  /*
   * (non-Javadoc)
   * @see java.util.Map#remove(java.lang.Object)
   */
  @Override
  public V remove(final Object key) {
    return this.map.remove(key);
  }

  /*
   * (non-Javadoc)
   * @see java.util.Map#size()
   */
  @Override
  public int size() {
    return this.map.size();
  }

  /*
   * (non-Javadoc)
   * @see java.util.Map#values()
   */
  @Override
  public Collection<V> values() {
    return this.map.values();
  }

}
