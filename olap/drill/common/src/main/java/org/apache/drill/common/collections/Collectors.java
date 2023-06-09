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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

public class Collectors {
  private Collectors() {
  }

  /**
   *
   * @param map {@code Map<K, V>} to collect elements from
   * @param mapper {@code BiFunction} that maps from (key, value) pair to type <T>
   * @param <T> elements type in {@code List}
   * @param <K> key type in {@code Map}
   * @param <V> value type in {@code Map}
   * @return new {@code List} that contains elements after applying mapper {@code BiFunction} to the input {@code Map}
   */
  public static <T, K, V> List<T> toList(Map<K, V> map, BiFunction<K, V, T> mapper) {
    return collect(new ArrayList<>(map.size()), map, mapper);
  }

  /**
   *
   * @param map {@code Map<K, V>} to collect elements from
   * @param mapper {@code BiFunction} that maps from (key, value) pair to type <T>
   * @param predicate {@code Predicate} filter to apply
   * @param <T> elements type in {@code List}
   * @param <K> keys type in {@code Map}
   * @param <V> value type in {@code Map}
   * @return new {@code List} that contains elements that satisfy {@code Predicate} after applying mapper {@code BiFunction}
   *   to the input {@code Map}
   */
  public static <T, K, V> List<T> toList(Map<K, V> map, BiFunction<K, V, T> mapper, Predicate<T> predicate) {
    return collect(new ArrayList<>(map.size()), map, mapper, predicate);
  }

  public static <T, K, V> List<T> collect(List<T> list, Map<K, V> map, BiFunction<K, V, T> mapper) {
    Preconditions.checkNotNull(list);
    Preconditions.checkNotNull(map);
    Preconditions.checkNotNull(mapper);
    map.forEach((k, v) -> list.add(mapper.apply(k, v)));
    return list;
  }

  public static <T, K, V> List<T> collect(List<T> list, Map<K, V> map, BiFunction<K, V, T> mapper, Predicate<T> predicate) {
    Preconditions.checkNotNull(list);
    Preconditions.checkNotNull(map);
    Preconditions.checkNotNull(mapper);
    Preconditions.checkNotNull(predicate);
    map.forEach((k, v) -> {
      T t = mapper.apply(k, v);
      if (predicate.test(t)) {
        list.add(t);
      }
    });
    return list;
  }

  /**
   *
   * @param collection {@code Collection<E>} of elements of type <E>
   * @param mapper {@code Function<E, T>} mapper function to apply
   * @param <T> elements type in {@code List}
   * @param <E> elements type in {@code Collection}
   * @return new {@code List} that contains elements that satisfy {@code Predicate} after applying mapper {@code Function}
   *   to the input {@code Collection}
   */
  public static <T, E> List<T> toList(Collection<E> collection, Function<E, T> mapper) {
    Preconditions.checkNotNull(collection);
    Preconditions.checkNotNull(mapper);
    ArrayList<T> list = new ArrayList<>(collection.size());
    collection.forEach(e -> list.add(mapper.apply(e)));
    return list;
  }

  /**
   *
   * @param collection {@code Collection<E>} of elements of type <E>
   * @param mapper {@code Function<E, T>} mapper function to apply
   * @param predicate {@code Predicate} filter to apply
   * @param <T>  elements type in {@code List}
   * @param <E> elements type in {@code Collection}
   * @return new {@code List} that contains elements after applying mapper {@code Function} to the input {@code Collection}
   */
  public static <T, E> List<T> toList(Collection<E> collection, Function<E, T> mapper, Predicate<T> predicate) {
    Preconditions.checkNotNull(collection);
    Preconditions.checkNotNull(mapper);
    Preconditions.checkNotNull(predicate);
    ArrayList<T> list = new ArrayList<>(collection.size());
    collection.forEach(e -> {
      T t = mapper.apply(e);
      if (predicate.test(t)) {
        list.add(t);
      }
    });
    return list;
  }
}
