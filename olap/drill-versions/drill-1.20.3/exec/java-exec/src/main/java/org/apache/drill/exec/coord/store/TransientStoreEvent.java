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
package org.apache.drill.exec.coord.store;

import org.apache.drill.shaded.guava.com.google.common.base.Objects;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

/**
 * Represents an event created as a result of an operation over a particular (key, value) entry in a
 * {@link TransientStore store} instance.
 *
 * Types of operations are enumerated in {@link TransientStoreEventType}
 *
 * @param <V>  value type
 */
public class TransientStoreEvent<V> {
  private final TransientStoreEventType type;
  private final String key;
  private final V value;

  public TransientStoreEvent(final TransientStoreEventType type, final String key, final V value) {
    this.type = Preconditions.checkNotNull(type);
    this.key = Preconditions.checkNotNull(key);
    this.value = Preconditions.checkNotNull(value);
  }

  public String getKey() {
    return key;
  }

  public TransientStoreEventType getType() {
    return type;
  }

  public V getValue() {
    return value;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof TransientStoreEvent && obj.getClass().equals(getClass())) {
      @SuppressWarnings("unchecked")
      final TransientStoreEvent<V> other = (TransientStoreEvent<V>) obj;
      return Objects.equal(type, other.type) && Objects.equal(key, other.key) && Objects.equal(value, other.value);
    }
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(type, key, value);
  }

  public static <T> TransientStoreEvent<T>of(final TransientStoreEventType type, final String key, final T value) {
    return new TransientStoreEvent<>(type, key, value);
  }
}
