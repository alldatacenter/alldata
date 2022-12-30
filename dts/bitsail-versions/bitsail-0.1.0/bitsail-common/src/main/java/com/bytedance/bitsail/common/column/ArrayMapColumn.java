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

package com.bytedance.bitsail.common.column;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.exception.CommonErrorCode;

import com.alibaba.fastjson.JSON;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Use list to store map column, replace use map to store map column.
public class ArrayMapColumn<K extends Column, V extends Column> extends Column {

  private static final long serialVersionUID = 1L;

  // type of the map's key
  private final Class<K> keyClass;
  // type of the map's value
  private final Class<V> valueClass;

  @Getter
  private final List<K> keys;
  @Getter
  private final List<V> values;

  public ArrayMapColumn(List<K> keys, List<V> values, Class<K> keyClass, Class<V> valueClass) {
    super(null, 0);
    this.keyClass = keyClass;
    this.valueClass = valueClass;
    this.keys = keys;
    this.values = values;
    if (this.keys.size() != this.values.size()) {
      throw BitSailException.asBitSailException(
          CommonErrorCode.RUNTIME_ERROR, "keys size != values size");
    }
  }

  public int size() {
    return keys.size();
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
  public String asString() {
    return JSON.toJSONString(this.getRawData());
  }

  @Override
  public Map<Object, Object> getRawData() {
    Map<Object, Object> m = new HashMap<Object, Object>(this.keys.size());
    int len = this.keys.size();
    for (int i = 0; i < len; i++) {
      m.put(keys.get(i).getRawData(), values.get(i).getRawData());
    }
    return m;
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

  @Override
  public String toString() {
    return JSON.toJSONString(this.keys) + JSON.toJSONString(this.values);
  }

  @Override
  public int getByteSize() {
    int size = 0;
    int len = keys.size();
    for (int i = 0; i < len; i++) {
      size += keys.get(i).getByteSize();
      size += values.get(i).getByteSize();
    }
    return size;
  }
}
