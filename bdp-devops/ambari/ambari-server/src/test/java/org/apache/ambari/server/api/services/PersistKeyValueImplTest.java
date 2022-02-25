/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.api.services;

import java.util.Map;

import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;


public class PersistKeyValueImplTest extends Assert {

  public static final int NUMB_THREADS = 1000;

  private Injector injector;

  @Before
  public void setUp() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
  }

  @After
  public void tearDown() throws Exception {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Test
  public void testStore() throws Exception {
    PersistKeyValueImpl impl = injector.getInstance(PersistKeyValueImpl.class);
    Map<String, String> map = impl.getAllKeyValues();
    assertEquals(0, map.size());

    impl.put("key1", "value1");
    impl.put("key2", "value2");

    map = impl.getAllKeyValues();
    assertEquals(2, map.size());
    assertEquals("value1", impl.getValue("key1"));
    assertEquals("value2", impl.getValue("key2"));
    assertEquals(map.get("key1"), impl.getValue("key1"));

    impl.put("key1", "value1-2");
    assertEquals("value1-2", impl.getValue("key1"));
    assertEquals(2, map.size());

    StringBuilder largeValueBuilder = new StringBuilder();
    for (int i = 0; i < 320; i++) {
      largeValueBuilder.append("0123456789");
    }
    String largeValue = largeValueBuilder.toString();

    impl.put("key3", largeValue);

    assertEquals(largeValue, impl.getValue("key3"));

  }

  @Test
  public void testMultiThreaded() throws Exception {
    final PersistKeyValueImpl impl = injector.getInstance(PersistKeyValueImpl.class);
    Thread[] threads = new Thread[NUMB_THREADS];

    for ( int i = 0; i < NUMB_THREADS; ++i ) {
      threads[i] = new Thread() {
        @Override
        public void run() {

          for (int i = 0; i < 100; ++i) {
            impl.put("key1", "value1");
            impl.put("key2", "value2");
            impl.put("key3", "value3");
            impl.put("key4", "value4");
          }
        }
      };
    }

    for ( int i = 0; i < NUMB_THREADS; ++i ) {
      threads[i].start();
    }

    for ( int i = 0; i < NUMB_THREADS; ++i ) {
      threads[i].join();
    }
  }
}
