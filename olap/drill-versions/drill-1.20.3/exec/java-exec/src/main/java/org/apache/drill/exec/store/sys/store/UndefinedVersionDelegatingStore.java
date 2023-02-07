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

import org.apache.drill.exec.store.sys.PersistentStore;
import org.apache.drill.exec.store.sys.PersistentStoreMode;
import org.apache.drill.exec.store.sys.VersionedPersistentStore;

import java.util.Iterator;
import java.util.Map;

/**
 * Wrapper store that delegates operations to PersistentStore.
 * Does not keep versioning and returns {@link DataChangeVersion#UNDEFINED} when version is required.
 *
 * @param <V> store value type
 */
public class UndefinedVersionDelegatingStore<V> implements VersionedPersistentStore<V> {

  private final PersistentStore<V> store;

  public UndefinedVersionDelegatingStore(PersistentStore<V> store) {
    this.store = store;
  }

  @Override
  public boolean contains(String key, DataChangeVersion version) {
    version.setVersion(DataChangeVersion.UNDEFINED);
    return store.contains(key);
  }

  @Override
  public V get(String key, DataChangeVersion version) {
    version.setVersion(DataChangeVersion.UNDEFINED);
    return store.get(key);
  }

  @Override
  public void put(String key, V value, DataChangeVersion version) {
    store.put(key, value);
  }

  @Override
  public PersistentStoreMode getMode() {
    return store.getMode();
  }

  @Override
  public void delete(String key) {
    store.delete(key);
  }

  @Override
  public boolean putIfAbsent(String key, V value) {
    return store.putIfAbsent(key, value);
  }

  @Override
  public Iterator<Map.Entry<String, V>> getRange(int skip, int take) {
    return store.getRange(skip, take);
  }

  @Override
  public void close() throws Exception {
    store.close();
  }
}
