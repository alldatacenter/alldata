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

import org.apache.drill.exec.store.sys.PersistentStore;
import org.apache.drill.exec.store.sys.PersistentStoreConfig;
import org.apache.drill.exec.store.sys.PersistentStoreProvider;
import org.apache.drill.exec.store.sys.VersionedPersistentStore;
import org.apache.drill.exec.store.sys.store.InMemoryStore;
import org.apache.drill.exec.store.sys.store.VersionedDelegatingStore;

public class InMemoryStoreProvider implements PersistentStoreProvider {

  private int capacity;

  public InMemoryStoreProvider(int capacity) {
    this.capacity = capacity;
  }

  @Override
  public void close() throws Exception { }

  @Override
  public <V> PersistentStore<V> getOrCreateStore(PersistentStoreConfig<V> config) {
    return new InMemoryStore<>(capacity);
  }

  @Override
  public <V> VersionedPersistentStore<V> getOrCreateVersionedStore(PersistentStoreConfig<V> config) {
    return new VersionedDelegatingStore<>(getOrCreateStore(config));
  }

  @Override
  public void start() { }
}
