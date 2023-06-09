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
package org.apache.drill.exec.store.sys;

import java.util.Iterator;
import java.util.Map;

/**
 * A Store interface used to store and retrieve instances of given value type.
 *
 * @param <V>  value type
 */
public interface Store<V> extends AutoCloseable {
  /**
   * Returns storage {@link PersistentStoreMode mode} of this store.
   */
  PersistentStoreMode getMode();

  /**
   * Removes the value corresponding to the given key if exists, nothing happens otherwise.
   * @param key  lookup key
   */
  void delete(String key);

  /**
   * Stores the (key, value) tuple in the store only if it does not exists.
   *
   * @param key  lookup key
   * @param value  value to store
   * @return  true if put takes place, false otherwise.
   */
  boolean putIfAbsent(String key, V value);

  /**
   * Returns an iterator of desired number of entries offsetting by the skip value.
   *
   * @param skip  number of records to skip from beginning
   * @param take  max number of records to return
   */
  Iterator<Map.Entry<String, V>> getRange(int skip, int take);

}
