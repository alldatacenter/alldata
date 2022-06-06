/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.testutils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Utilities for collections used in unit tests.
 */
public class TestCollectionUtils {

  /**
   * A simple (but not production quality) way to create mutable hashmaps for unit tests
   * @param firstKey the first key in the map
   * @param firstValue the first value in the map
   * @param others further keys and values
   * @param <K> key type
   * @param <V> value type
   * @return the map
   */
  @SuppressWarnings("unchecked")
  public static <K, V> Map<K, V> map(K firstKey, V firstValue, Object... others) {
    Map<K, V> map = new HashMap<>();
    map.put(firstKey, firstValue);
    Iterator iterator = Arrays.asList(others).iterator();
    while (iterator.hasNext()) {
      map.put((K)iterator.next(), (V)iterator.next());
    }
    return map;
  }

}
