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

import java.util.Iterator;
import java.util.Map;

/**
 * An abstraction for storing, retrieving and observing transient (key, value) pairs in a distributed environment.
 *
 * This abstraction diverges from {@link org.apache.drill.exec.store.sys.PersistentStore} in that the lifetime of
 * a (key, value) tuple is bound to the lifetime of the node originally creating it. In other words, entries are evicted
 * as soon as node/s originally stored them leaves the cluster. That should explain the reason for relocating this
 * abstraction under cluster coordination package.
 *
 * Consumers of this interface can observe changes made to the store via attaching a {@link TransientStoreListener listener}.
 *
 */
public interface TransientStore<V> extends AutoCloseable {
  /**
   * Returns a value corresponding to the given look-up key if exists, null otherwise.
   * @param key  look-up key
   */
  V get(String key);

  /**
   * Stores the given (key, value) in this store overriding the existing value.
   *
   * @param key  look-up key
   * @param value  value to store
   * @return  the old value if the key exists, null otherwise
   */
  V put(String key, V value);

  /**
   * Stores the given (key, value) tuple in this store only if it does not exists.
   *
   * @param key  look-up key
   * @param value  value to store
   * @return  the old value if the key exists, null otherwise
   */
  V putIfAbsent(String key, V value);


  /**
   * Removes the (key, value) tuple from this store if the key exists.
   *
   * @param key  look-up key
   * @return  the removed value if key exists, null otherwise
   */
  V remove(String key);

  /**
   * Returns an iterator of (key, value) tuples.
   */
  Iterator<Map.Entry<String, V>> entries();

  /**
   * Returns an iterator of keys.
   */
  Iterator<String> keys();


  /**
   * Returns an iterator of values.
   */
  Iterator<V> values();

  /**
   * Returns number of entries.
   */
  int size();

  /**
   * Adds a listener that observes store {@link TransientStoreEvent events}.
   *
   * Note that
   * i) Calling this method with the same listener instance more than once has no effect.
   * ii) Listeners are not necessarily invoked from the calling thread. Consumer should consider thread safety issues.
   * iii) Subclasses might hold a strong reference to the listener. It is important that consumer
   * {@link #removeListener(TransientStoreListener) removes} its listener once it is done observing events.
   *
   * @see #removeListener(TransientStoreListener)
   */
  void addListener(TransientStoreListener listener);

  /**
   * Removes the given listener from this store if exists, has no effect otherwise.
   */
  void removeListener(TransientStoreListener listener);
}
