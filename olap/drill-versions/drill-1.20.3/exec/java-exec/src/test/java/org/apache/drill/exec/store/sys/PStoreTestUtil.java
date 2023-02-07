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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.ObjectMapper;

public class PStoreTestUtil {

  public static void test(PersistentStoreProvider provider) throws Exception {
    PersistentStore<String> store = provider.getOrCreateStore(
      PersistentStoreConfig.newJacksonBuilder(new ObjectMapper(), String.class)
        .name("sys.test")
        .build()
    );
    try {
      Map<String, String> expectedMap = new HashMap<>();
      expectedMap.put("first", "value1");
      expectedMap.put("second", "value2");
      expectedMap.forEach(store::put);
      waitForNumProps(store, expectedMap.size());

      Iterator<Map.Entry<String, String>> iter = store.getAll();
      for(int i =0; i < expectedMap.size(); i++) {
        Entry<String, String> storeEntry = iter.next();

        assertTrue(String.format("This element wasn't added to PStore, storeEntry: %s", storeEntry),
          expectedMap.containsKey(storeEntry.getKey()));

        String expectedValue = expectedMap.get(storeEntry.getKey());
        assertEquals(String.format("The value is different in PStore for this key: %s. Expected value: %s, Actual: %s",
          storeEntry.getKey(), expectedValue, storeEntry.getValue()), expectedValue, storeEntry.getValue());
      }

      assertFalse(iter.hasNext());
    } finally {
      Iterator<Map.Entry<String, String>> iter = store.getAll();
      while(iter.hasNext()) {
        final String key = iter.next().getKey();
        store.delete(key);
      }

      waitForNumProps(store, 0);
      assertFalse(store.getAll().hasNext());
    }
  }

  /**
   * Wait for store caches to update, this is necessary because ZookeeperClient caches can update asynchronously
   * in some cases.
   *
   * @param store PersistentStore
   * @param expected num props
   * @throws InterruptedException will fail this test once arises
   */
  private static void waitForNumProps(PersistentStore store, int expected) throws InterruptedException {
    while(getNumProps(store) < expected) {
      Thread.sleep(100L);
    }
  }

  private static int getNumProps(PersistentStore store) {
    Iterator<Map.Entry<String, String>> iter = store.getAll();

    int numProps = 0;

    while (iter.hasNext()) {
      iter.next();
      numProps++;
    }

    return numProps;
  }
}
