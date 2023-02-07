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
package org.apache.drill.exec.testing.store;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.apache.drill.shaded.guava.com.google.common.collect.Iterables;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;

import org.apache.drill.exec.store.sys.BasePersistentStore;
import org.apache.drill.exec.store.sys.PersistentStoreMode;

public class NoWriteLocalStore<V> extends BasePersistentStore<V> {
  private final ConcurrentMap<String, V> store = Maps.newConcurrentMap();

  @Override
  public void delete(final String key) {
    store.remove(key);
  }

  @Override
  public PersistentStoreMode getMode() {
    return PersistentStoreMode.PERSISTENT;
  }

  @Override
  public boolean contains(final String key) {
    return store.containsKey(key);
  }

  @Override
  public V get(final String key) {
    return store.get(key);
  }

  @Override
  public void put(final String key, final V value) {
    store.put(key, value);
  }

  @Override
  public boolean putIfAbsent(final String key, final V value) {
    final V old = store.putIfAbsent(key, value);
    if (old == null) {
      return true;
    }
    return false;
  }

  @Override
  public Iterator<Map.Entry<String, V>> getRange(final int skip, final int take) {
    return Iterables.limit(Iterables.skip(store.entrySet(), skip), take).iterator();
  }

  @Override
  public void close() {
    store.clear();
  }
}
