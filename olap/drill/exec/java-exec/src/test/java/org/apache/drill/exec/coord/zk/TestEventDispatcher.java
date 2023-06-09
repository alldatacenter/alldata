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

import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.drill.exec.coord.store.TransientStoreConfig;
import org.apache.drill.exec.coord.store.TransientStoreEvent;
import org.apache.drill.exec.serialization.InstanceSerializer;
import org.apache.drill.test.BaseTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestEventDispatcher extends BaseTest {

  private final static String key = "some-key";
  private final static String value = "some-data";
  private final static byte[] data = "some-data".getBytes();

  private ZkEphemeralStore<String> store;
  private EventDispatcher<String> dispatcher;
  private ChildData child;

  @Before
  public void setUp() {
    store = Mockito.mock(ZkEphemeralStore.class);
    final TransientStoreConfig<String> config = Mockito.mock(TransientStoreConfig.class);
    Mockito
        .when(store.getConfig())
        .thenReturn(config);

    Mockito
        .when(config.getSerializer())
        .thenReturn(new InstanceSerializer<String>() {
          @Override
          public byte[] serialize(String instance) throws IOException {
            return instance.getBytes();
          }

          @Override
          public String deserialize(byte[] raw) throws IOException {
            return new String(raw);
          }
        });

    dispatcher = new EventDispatcher<>(store);
    child = Mockito.mock(ChildData.class);
    Mockito
        .when(child.getPath())
        .thenReturn(key);

    Mockito
        .when(child.getData())
        .thenReturn(data);
  }

  @Test
  public void testDispatcherPropagatesEvents() throws Exception {
    final PathChildrenCacheEvent.Type[] types = new PathChildrenCacheEvent.Type[] {
        PathChildrenCacheEvent.Type.CHILD_ADDED,
        PathChildrenCacheEvent.Type.CHILD_REMOVED,
        PathChildrenCacheEvent.Type.CHILD_UPDATED
    };

    for (final PathChildrenCacheEvent.Type type:types) {
      dispatcher.childEvent(null, new PathChildrenCacheEvent(type, child));

      final TransientStoreEvent event = TransientStoreEvent.of(EventDispatcher.MAPPINGS.get(type), key, value);
      Mockito
          .verify(store)
          .fireListeners(event);
    }

    Assert.assertEquals("Number of event types that dispatcher can handle is different", types.length, EventDispatcher.MAPPINGS.size());
  }
}
