/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.common.collections;

import java.util.Map;

import org.apache.drill.shaded.guava.com.google.common.base.Objects;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

public class ImmutableEntry<K, V> implements Map.Entry<K, V>  {
  private final K key;
  private final V value;

  public ImmutableEntry(final K key, final V value) {
    this.key = Preconditions.checkNotNull(key, "key is required");
    this.value = Preconditions.checkNotNull(value, "value is required");
  }

  @Override
  public K getKey() {
    return key;
  }

  @Override
  public V getValue() {
    return value;
  }

  @Override
  public V setValue(final V value) {
    throw new UnsupportedOperationException("entry is immutable");
  }

  @Override
  public boolean equals(final Object other) {
    if (other == this) {
      return true;
    }
    if (other == null || !(other instanceof  Map.Entry)) {
      return false;
    }
    Map.Entry<?, ?> entry = (Map.Entry<?, ?>) other;
    return Objects.equal(key, entry.getKey()) && Objects.equal(value, entry.getValue());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(key, value);
  }

  @Override
  public String toString() {
    return "(" + key.toString() + ", " +
           value.toString() + ")";
  }
}
