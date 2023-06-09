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
package org.apache.drill.exec.coord.local;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.drill.exec.coord.store.BaseTransientStore;
import org.apache.drill.exec.coord.store.TransientStoreConfig;
import org.apache.drill.exec.coord.store.TransientStoreEvent;
import org.apache.drill.exec.coord.store.TransientStoreEventType;

public class MapBackedStore<V> extends BaseTransientStore<V> {
  private final ConcurrentMap<String, V> delegate = Maps.newConcurrentMap();

  public MapBackedStore(final TransientStoreConfig<V> config) {
    super(config);
  }

  @Override
  public V get(final String key) {
    return delegate.get(key);
  }

  @Override
  public V put(final String key, final V value) {
    final boolean hasKey = delegate.containsKey(key);
    final V old = delegate.put(key, value);
    if (old != value) {
      final TransientStoreEventType type = hasKey ? TransientStoreEventType.UPDATE : TransientStoreEventType.CREATE;
      fireListeners(TransientStoreEvent.of(type, key, value));
    }
    return old;
  }

  @Override
  public V putIfAbsent(final String key, final V value) {
    final V existing = delegate.putIfAbsent(key, value);
    if (existing == null) {
      fireListeners(TransientStoreEvent.of(TransientStoreEventType.CREATE, key, value));
    }
    return existing;
  }

  @Override
  public V remove(final String key) {
    final V existing = delegate.remove(key);
    if (existing != null) {
      fireListeners(TransientStoreEvent.of(TransientStoreEventType.DELETE, key, existing));
    }
    return existing;
  }

  @Override
  public Iterator<Map.Entry<String, V>> entries() {
    return delegate.entrySet().iterator();
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public void close() throws Exception {
    delegate.clear();
  }
}
