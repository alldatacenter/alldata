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
package org.apache.drill.common.map;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A special type of {@link Map} with {@link String}s as keys, and the case of a key is ignored for operations involving
 * keys like {@link #put}, {@link #get}, etc. The keys are stored and retrieved in lower case. Use the static factory
 * methods to create instances of this class (e.g. {@link #newConcurrentMap}).
 *
 * @param <VALUE> the type of values to be stored in the map
 */
public class CaseInsensitiveMap<VALUE> implements Map<String, VALUE> {

  /**
   * Returns a new instance of {@link java.util.concurrent.ConcurrentMap} with key case-insensitivity. See
   * {@link java.util.concurrent.ConcurrentMap}.
   *
   * @param <VALUE> type of values to be stored in the map
   * @return key case-insensitive concurrent map
   */
  public static <VALUE> CaseInsensitiveMap<VALUE> newConcurrentMap() {
    return new CaseInsensitiveMap<>(Maps.newConcurrentMap());
  }

  /**
   * Returns a new instance of {@link java.util.HashMap} with key case-insensitivity. See {@link java.util.HashMap}.
   *
   * @param <VALUE> type of values to be stored in the map
   * @return key case-insensitive hash map
   */
  public static <VALUE> CaseInsensitiveMap<VALUE> newHashMap() {
    return new CaseInsensitiveMap<>(Maps.newHashMap());
  }

  /**
   * Returns a new instance of {@link java.util.HashMap}, with key case-insensitivity, of expected size.
   * See {@link java.util.HashMap}.
   *
   * @param expectedSize expected size
   * @param <VALUE> type of values to be stored in the map
   * @return key case-insensitive hash map
   */
  public static <VALUE> CaseInsensitiveMap<VALUE> newHashMapWithExpectedSize(final int expectedSize) {
    return new CaseInsensitiveMap<>(Maps.newHashMapWithExpectedSize(expectedSize));
  }

  /**
   * Returns a new instance of {@link ImmutableMap} with key case-insensitivity. This map is built from the given
   * map. See {@link ImmutableMap}.
   *
   * @param map map to copy from
   * @param <VALUE> type of values to be stored in the map
   * @return key case-insensitive immutable map
   */
  public static <VALUE> CaseInsensitiveMap<VALUE> newImmutableMap(final Map<? extends String, ? extends VALUE> map) {
    final ImmutableMap.Builder<String, VALUE> builder = ImmutableMap.builder();
    for (final Entry<? extends String, ? extends VALUE> entry : map.entrySet()) {
      builder.put(entry.getKey().toLowerCase(), entry.getValue());
    }
    return new CaseInsensitiveMap<>(builder.build());
  }

  private final Map<String, VALUE> underlyingMap;

  /**
   * Use the static factory methods to create instances of this class (e.g. {@link #newConcurrentMap}).
   *
   * @param underlyingMap the underlying map
   */
  private CaseInsensitiveMap(final Map<String, VALUE> underlyingMap) {
    this.underlyingMap = underlyingMap;
  }

  @Override
  public int size() {
    return underlyingMap.size();
  }

  @Override
  public boolean isEmpty() {
    return underlyingMap.isEmpty();
  }

  @Override
  public boolean containsKey(final Object key) {
    return key instanceof String && underlyingMap.containsKey(((String) key).toLowerCase());
  }

  @Override
  public boolean containsValue(final Object value) {
    return underlyingMap.containsValue(value);
  }

  @Override
  public VALUE get(final Object key) {
    return key instanceof String ? underlyingMap.get(((String) key).toLowerCase()) : null;
  }

  @Override
  public VALUE put(final String key, final VALUE value) {
    return underlyingMap.put(key.toLowerCase(), value);
  }

  @Override
  public VALUE remove(final Object key) {
    return key instanceof String ? underlyingMap.remove(((String) key).toLowerCase()) : null;
  }

  @Override
  public void putAll(final Map<? extends String, ? extends VALUE> map) {
    for (final Entry<? extends String, ? extends VALUE> entry : map.entrySet()) {
      underlyingMap.put(entry.getKey().toLowerCase(), entry.getValue());
    }
  }

  @Override
  public void clear() {
    underlyingMap.clear();
  }

  @Override
  public Set<String> keySet() {
    return underlyingMap.keySet();
  }

  @Override
  public Collection<VALUE> values() {
    return underlyingMap.values();
  }

  @Override
  public Set<Entry<String, VALUE>> entrySet() {
    return underlyingMap.entrySet();
  }

  @Override
  public int hashCode() {
    return Objects.hash(underlyingMap);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CaseInsensitiveMap)) {
      return false;
    }
    CaseInsensitiveMap<?> that = (CaseInsensitiveMap<?>) o;
    return Objects.equals(underlyingMap, that.underlyingMap);
  }

  @Override
  public String toString() {
    return underlyingMap.toString();
  }
}
