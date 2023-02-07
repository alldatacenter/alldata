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

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.apache.drill.shaded.guava.com.google.common.base.Function;
import org.apache.drill.shaded.guava.com.google.common.collect.Iterators;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.drill.common.collections.ImmutableEntry;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.coord.store.BaseTransientStore;
import org.apache.drill.exec.coord.store.TransientStoreConfig;
import org.apache.drill.exec.coord.store.TransientStoreEvent;
import org.apache.drill.exec.serialization.InstanceSerializer;
import org.apache.zookeeper.CreateMode;

public class ZkEphemeralStore<V> extends BaseTransientStore<V> {

  @VisibleForTesting
  protected final PathChildrenCacheListener dispatcher = new EventDispatcher<>(this);
  private final ZookeeperClient client;

  public ZkEphemeralStore(final TransientStoreConfig<V> config, final CuratorFramework curator) {
    super(config);
    this.client = new ZookeeperClient(curator, PathUtils.join("/", config.getName()), CreateMode.EPHEMERAL);
  }

  public void start() throws Exception {
    getClient().getCache().getListenable().addListener(dispatcher);
    getClient().start();
  }

  protected ZookeeperClient getClient() {
    return client;
  }

  @Override
  public V get(final String key) {
    final byte[] bytes = getClient().get(key);
    if (bytes == null) {
      return null;
    }
    try {
      return config.getSerializer().deserialize(bytes);
    } catch (final IOException e) {
      throw new DrillRuntimeException(String.format("unable to deserialize value at %s", key), e);
    }
  }

  @Override
  public V put(final String key, final V value) {
    final InstanceSerializer<V> serializer = config.getSerializer();
    try {
      final byte[] old = getClient().get(key);
      final byte[] bytes = serializer.serialize(value);
      getClient().put(key, bytes);
      if (old == null) {
        return null;
      }
      return serializer.deserialize(old);
    } catch (final IOException e) {
      throw new DrillRuntimeException(String.format("unable to de/serialize value of type %s", value.getClass()), e);
    }
  }

  @Override
  public V putIfAbsent(final String key, final V value) {
    try {
      final InstanceSerializer<V> serializer = config.getSerializer();
      final byte[] bytes = serializer.serialize(value);
      final byte[] data = getClient().putIfAbsent(key, bytes);
      if (data == null) {
        return null;
      }
      return serializer.deserialize(data);
    } catch (final IOException e) {
      throw new DrillRuntimeException(String.format("unable to serialize value of type %s", value.getClass()), e);
    }
  }

  @Override
  public V remove(final String key) {
    final V existing = get(key);
    if (existing != null) {
      getClient().delete(key);
    }
    return existing;
  }

  @Override
  public Iterator<Map.Entry<String, V>> entries() {
    return Iterators.transform(getClient().entries(), new Function<Map.Entry<String, byte[]>, Map.Entry<String, V>>() {
      @Nullable
      @Override
      public Map.Entry<String, V> apply(@Nullable Map.Entry<String, byte[]> input) {
        try {
          final V value = config.getSerializer().deserialize(input.getValue());
          return new ImmutableEntry<>(input.getKey(), value);
        } catch (final IOException e) {
          throw new DrillRuntimeException(String.format("unable to deserialize value at key %s", input.getKey()), e);
        }
      }
    });
  }

  @Override
  public int size() {
    return getClient().getCache().getCurrentData().size();
  }

  @Override
  public void close() throws Exception {
    getClient().close();
  }

  /**
   * This method override ensures package level method visibility.
   */
  @Override
  protected void fireListeners(TransientStoreEvent event) {
    super.fireListeners(event);
  }
}
