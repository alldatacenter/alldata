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

import org.apache.drill.exec.exception.StoreException;
import org.apache.drill.exec.store.sys.store.UndefinedVersionDelegatingStore;

/**
 * A factory used to create {@link PersistentStore store} instances.
 */
public interface PersistentStoreProvider extends AutoCloseable {

  /**
   * Gets or creates a {@link PersistentStore persistent store} for the given configuration.
   *
   * Note that implementors have liberty to cache previous {@link PersistentStore store} instances.
   *
   * @param config store configuration
   * @param <V> store value type
   * @return persistent store instance
   * @throws StoreException in case when unable to create store
   */
  <V> PersistentStore<V> getOrCreateStore(PersistentStoreConfig<V> config) throws StoreException;

  /**
   * Override this method if store supports versioning and return versioning instance.
   * By default, undefined version wrapper will be used.
   *
   * @param config store configuration
   * @param <V> store value type
   * @return versioned persistent store instance
   * @throws StoreException in case when unable to create store
   */
  default <V> VersionedPersistentStore<V> getOrCreateVersionedStore(PersistentStoreConfig<V> config) throws StoreException {
    // for those stores that do not support versioning
    return new UndefinedVersionDelegatingStore<>(getOrCreateStore(config));
  }

  /**
   * Sets up the provider.
   */
  void start() throws Exception;
}
