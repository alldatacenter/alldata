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

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.listen.MappingListenerManager;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.test.TestingServer;
import org.apache.drill.exec.ZookeeperTestUtil;
import org.apache.drill.exec.coord.store.TransientStoreConfig;
import org.apache.drill.exec.serialization.InstanceSerializer;
import org.apache.drill.test.BaseTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestEphemeralStore extends BaseTest {
  private final static String root = "/test";
  private final static String path = "test-key";
  private final static String value = "testing";

  private TestingServer server;
  private CuratorFramework curator;
  private TransientStoreConfig<String> config;
  private ZkEphemeralStore<String> store;

  static class StoreWithMockClient<V> extends ZkEphemeralStore<V> {
    private final ZookeeperClient client = Mockito.mock(ZookeeperClient.class);

    public StoreWithMockClient(final TransientStoreConfig<V> config, final CuratorFramework curator) {
      super(config, curator);
    }

    @Override
    protected ZookeeperClient getClient() {
      return client;
    }
  }

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    ZookeeperTestUtil.setZookeeperSaslTestConfigProps();

    server = new TestingServer();
    final RetryPolicy policy = new RetryNTimes(2, 1000);
    curator = CuratorFrameworkFactory.newClient(server.getConnectString(), policy);

    config = Mockito.mock(TransientStoreConfig.class);
    Mockito
        .when(config.getName())
        .thenReturn(root);

    Mockito
        .when(config.getSerializer())
        .thenReturn(new InstanceSerializer<String>() {
          @Override
          public byte[] serialize(final String instance) throws IOException {
            if (instance == null) {
              return null;
            }
            return instance.getBytes();
          }

          @Override
          public String deserialize(final byte[] raw) throws IOException {
            if (raw == null) {
              return null;
            }
            return new String(raw);
          }
        });

    store = new ZkEphemeralStore<>(config, curator);

    server.start();
    curator.start();
    store.start();
  }

  /**
   * This test ensures store subscribes to receive events from underlying client. Dispatcher tests ensures listeners
   * are fired on incoming events. These two sets of tests ensure observer pattern in {@code TransientStore} works fine.
   */
  @Test
  public void testStoreRegistersDispatcherAndStartsItsClient() throws Exception {
    @SuppressWarnings("resource")
    final StoreWithMockClient<String> store = new StoreWithMockClient<>(config, curator);

    final PathChildrenCache cache = Mockito.mock(PathChildrenCache.class);
    final ZookeeperClient client = store.getClient();
    Mockito
        .when(client.getCache())
        .thenReturn(cache);

    @SuppressWarnings("unchecked")
    final MappingListenerManager<PathChildrenCacheListener,PathChildrenCacheListener> container = Mockito.mock(MappingListenerManager.class);
    Mockito
        .when(cache.getListenable())
        .thenReturn(container);

    store.start();

    Mockito
        .verify(container)
        .addListener(store.dispatcher);

    Mockito
        .verify(client)
        .start();
  }

  @After
  public void tearDown() throws Exception {
    store.close();
    curator.close();
    server.close();
  }

  @Test
  public void testPutAndGetWorksAntagonistacally() {
    store.put(path, value);
    final String actual = store.get(path);
    Assert.assertEquals("value mismatch", value, actual);
  }
}
