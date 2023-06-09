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

import org.apache.drill.exec.store.sys.store.DataChangeVersion;

/**
 * Extension to the Store interface that supports versions
 * @param <V>
 */
public interface VersionedPersistentStore<V> extends Store<V> {
  /**
   * Checks if lookup key is present in store.
   * Sets data change version number.
   *
   * @param key lookup key
   * @param version version holder
   * @return true if store contains lookup key, false otherwise
   */
  boolean contains(String key, DataChangeVersion version);

  /**
   * Returns the value for the given key if exists, null otherwise.
   * Sets data change version number.
   * @param key  lookup key
   * @param version version holder
   */
  V get(String key, DataChangeVersion version);

  /**
   * Stores the (key, value) tuple in the store.
   * If tuple already exits, stores it only if versions match,
   * otherwise throws {@link org.apache.drill.exec.exception.VersionMismatchException}
   * Lifetime of the tuple depends upon store {@link #getMode mode}.
   *
   * @param key  lookup key
   * @param value  value to store
   * @param version version holder
   */
  void put(String key, V value, DataChangeVersion version);
}
