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
package org.apache.drill.exec.store.sys.store.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.drill.exec.store.sys.VersionedPersistentStore;
import org.apache.drill.common.AutoCloseables;
import org.apache.drill.exec.exception.StoreException;
import org.apache.drill.exec.store.sys.PersistentStore;
import org.apache.drill.exec.store.sys.PersistentStoreConfig;
import org.apache.drill.exec.store.sys.PersistentStoreProvider;
import org.apache.drill.common.util.function.CheckedFunction;

public class CachingPersistentStoreProvider extends BasePersistentStoreProvider {

  private final Map<PersistentStoreConfig<?>, PersistentStore<?>> storeCache = new ConcurrentHashMap<>();
  private final Map<PersistentStoreConfig<?>, VersionedPersistentStore<?>> versionedStoreCache = new ConcurrentHashMap<>();
  private final PersistentStoreProvider provider;

  public CachingPersistentStoreProvider(PersistentStoreProvider provider) {
    this.provider = provider;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> PersistentStore<V> getOrCreateStore(final PersistentStoreConfig<V> config) throws StoreException {
    CheckedFunction<PersistentStoreConfig<?>, PersistentStore<?>, StoreException> function = provider::getOrCreateStore;
    return (PersistentStore<V>) storeCache.computeIfAbsent(config, function);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> VersionedPersistentStore<V> getOrCreateVersionedStore(PersistentStoreConfig<V> config) throws StoreException {
    CheckedFunction<PersistentStoreConfig<?>, VersionedPersistentStore<?>, StoreException> function = provider::getOrCreateVersionedStore;
    return (VersionedPersistentStore<V>) versionedStoreCache.computeIfAbsent(config, function);
  }

  @Override
  public void start() throws Exception {
    provider.start();
  }

  @Override
  public void close() throws Exception {
    List<AutoCloseable> closeables = new ArrayList<>();

    // add un-versioned stores
    closeables.addAll(storeCache.values());
    storeCache.clear();

    // add versioned stores
    closeables.addAll(versionedStoreCache.values());
    versionedStoreCache.clear();

    // add provider
    closeables.add(provider);

    AutoCloseables.close(closeables);
  }

}
