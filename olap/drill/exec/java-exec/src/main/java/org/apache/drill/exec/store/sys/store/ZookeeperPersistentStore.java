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

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.drill.shaded.guava.com.google.common.base.Function;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Iterators;
import org.apache.curator.framework.CuratorFramework;
import org.apache.drill.common.collections.ImmutableEntry;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.coord.zk.PathUtils;
import org.apache.drill.exec.coord.zk.ZookeeperClient;
import org.apache.drill.exec.exception.StoreException;
import org.apache.drill.exec.serialization.InstanceSerializer;
import org.apache.drill.exec.store.sys.BasePersistentStore;
import org.apache.drill.exec.store.sys.PersistentStoreConfig;
import org.apache.drill.exec.store.sys.PersistentStoreMode;
import org.apache.drill.exec.store.sys.VersionedPersistentStore;
import org.apache.zookeeper.CreateMode;

/**
 * Zookeeper based implementation of {@link org.apache.drill.exec.store.sys.PersistentStore}.
 */
public class ZookeeperPersistentStore<V> extends BasePersistentStore<V> implements VersionedPersistentStore<V> {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ZookeeperPersistentStore.class);

  private final PersistentStoreConfig<V> config;
  private final ZookeeperClient client;

  public ZookeeperPersistentStore(final CuratorFramework framework, final PersistentStoreConfig<V> config) throws StoreException {
    this.config = Preconditions.checkNotNull(config);
    this.client = new ZookeeperClient(framework, PathUtils.join("/", config.getName()), CreateMode.PERSISTENT);
  }

  public void start() throws Exception {
    client.start();
  }

  @Override
  public PersistentStoreMode getMode() {
    return config.getMode();
  }

  @Override
  public boolean contains(final String key) {
    return contains(key, null);
  }

  @Override
  public boolean contains(final String key, final DataChangeVersion version) {
    return client.hasPath(key, true, version);
  }

  @Override
  public V get(final String key) {
    return get(key, false, null);
  }

  @Override
  public V get(final String key, final DataChangeVersion version) {
    return get(key, true, version);
  }

  public V get(final String key, final boolean consistencyFlag, final DataChangeVersion version) {
    byte[] bytes = client.get(key, consistencyFlag, version);

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
  public void put(final String key, final V value) {
    put(key, value, null);
  }

  @Override
  public void put(final String key, final V value, final DataChangeVersion version) {
    final InstanceSerializer<V> serializer = config.getSerializer();
    try {
      final byte[] bytes = serializer.serialize(value);
      client.put(key, bytes, version);
    } catch (final IOException e) {
      throw new DrillRuntimeException(String.format("unable to de/serialize value of type %s", value.getClass()), e);
    }
  }


  @Override
  public boolean putIfAbsent(final String key, final V value) {
    try {
      final byte[] bytes = config.getSerializer().serialize(value);
      final byte[] data = client.putIfAbsent(key, bytes);
      return data == null;
    } catch (final IOException e) {
      throw new DrillRuntimeException(String.format("unable to serialize value of type %s", value.getClass()), e);
    }
  }

  @Override
  public void delete(final String key) {
    client.delete(key);
  }

  @Override
  public Iterator<Map.Entry<String, V>> getRange(final int skip, final int take) {
    final Iterator<Map.Entry<String, byte[]>> entries = client.entries();
    Iterators.advance(entries, skip);
    return Iterators.transform(Iterators.limit(entries, take), new Function<Map.Entry<String, byte[]>, Map.Entry<String, V>>() {
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
  public void close() {
    try{
      client.close();
    } catch(final Exception e) {
      logger.warn("Failure while closing out {}: {}", getClass().getSimpleName(), e);
    }
  }

}
