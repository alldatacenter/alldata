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
package org.apache.drill.exec.store.sys.store;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.drill.exec.store.sys.BasePersistentStore;
import org.apache.drill.exec.store.sys.PersistentStoreMode;

import org.apache.drill.shaded.guava.com.google.common.collect.Iterables;

public class InMemoryStore<V> extends BasePersistentStore<V> {

  private final ConcurrentNavigableMap<String, V> store;
  private final int capacity;
  private final AtomicInteger currentSize = new AtomicInteger();

  public InMemoryStore(int capacity) {
    this.capacity = capacity;
    //Allows us to trim out the oldest elements to maintain finite max size
    this.store = new ConcurrentSkipListMap<>();
  }

  @Override
  public void delete(final String key) {
    store.remove(key);
  }

  @Override
  public PersistentStoreMode getMode() {
    return PersistentStoreMode.BLOB_PERSISTENT;
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
    if (currentSize.incrementAndGet() > capacity) {
      //Pop Out Oldest
      store.pollLastEntry();
      currentSize.decrementAndGet();
    }
  }

  @Override
  public boolean putIfAbsent(final String key, final V value) {
    return (value != store.putIfAbsent(key, value));
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
