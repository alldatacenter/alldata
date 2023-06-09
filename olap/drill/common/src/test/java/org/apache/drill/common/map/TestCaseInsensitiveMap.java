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
package org.apache.drill.common.map;

import org.apache.drill.test.BaseTest;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TestCaseInsensitiveMap extends BaseTest {

  @Test
  public void putAndGet() {
    final CaseInsensitiveMap<Integer> map = CaseInsensitiveMap.newConcurrentMap();
    assertTrue(map.isEmpty());
    map.put("DRILL", 1);
    map.put("bABy", 2);
    map.put("drill!", 3);

    assertTrue(!map.isEmpty());
    assertEquals(3, map.size());
    assertTrue(map.containsKey("drill"));
    assertEquals(2, (long) map.get("BabY"));
    assertNull(map.get(2));

    assertEquals(3, (long) map.remove("DriLl!"));
    assertEquals(2, map.size());

    assertTrue(map.containsValue(1));
  }

  @Test
  public void addAllFromAnother() {
    final Map<String, Integer> another = new HashMap<>();
    another.put("JuSt", 1);
    another.put("DRILL", 2);
    another.put("it", 3);

    final Map<String, Integer> map = CaseInsensitiveMap.newConcurrentMap();
    map.putAll(another);
    assertEquals(3, map.size());
    assertTrue(map.containsKey("just"));
    assertEquals(2, (long) map.get("drill"));
    assertEquals(3, (long) map.remove("IT"));

    final Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
    assertEquals(2, entrySet.size());
  }

  @Test
  public void checkEquals() {
    Map<String, String> map1 = CaseInsensitiveMap.newHashMap();
    map1.put("key_1", "value_1");

    Map<String, String> map2 = CaseInsensitiveMap.newHashMap();
    map2.put("KEY_1", "value_1");
    assertEquals(map1, map2);

    map2.put("key_2", "value_2");
    assertNotEquals(map1, map2);
  }

}
