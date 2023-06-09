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
package org.apache.drill.exec.coord.zk;

import java.util.Map;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.drill.exec.coord.store.TransientStoreEvent;
import org.apache.drill.exec.coord.store.TransientStoreEventType;

/**
 * An abstraction used for dispatching store {@link TransientStoreEvent events}.
 *
 * @param <V>  value type
 */
public class EventDispatcher<V> implements PathChildrenCacheListener {
  public final static Map<PathChildrenCacheEvent.Type, TransientStoreEventType> MAPPINGS = ImmutableMap
      .<PathChildrenCacheEvent.Type, TransientStoreEventType>builder()
      .put(PathChildrenCacheEvent.Type.CHILD_ADDED, TransientStoreEventType.CREATE)
      .put(PathChildrenCacheEvent.Type.CHILD_REMOVED, TransientStoreEventType.DELETE)
      .put(PathChildrenCacheEvent.Type.CHILD_UPDATED, TransientStoreEventType.UPDATE)
      .build();

  private final ZkEphemeralStore<V> store;

  protected EventDispatcher(final ZkEphemeralStore<V> store) {
    this.store = Preconditions.checkNotNull(store, "store is required");
  }

  @Override
  public void childEvent(final CuratorFramework client, final PathChildrenCacheEvent event) throws Exception {
    final PathChildrenCacheEvent.Type original = event.getType();
    final TransientStoreEventType mapped = MAPPINGS.get(original);
    if (mapped != null) { // dispatch the event to listeners only if it can be mapped
      final String path = event.getData().getPath();
      final byte[] bytes = event.getData().getData();
      final V value = store.getConfig().getSerializer().deserialize(bytes);
      store.fireListeners(TransientStoreEvent.of(mapped, path, value));
    }
  }
}
