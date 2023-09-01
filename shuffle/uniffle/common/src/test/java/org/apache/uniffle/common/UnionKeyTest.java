/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.uniffle.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UnionKeyTest {

  @Test
  public void test() {
    Object[] elements = new Object[]{
        "appId",
        1,
        1
    };

    String key = UnionKey.buildKey(elements);
    assertEquals(key, "appId_1_1");

    assertTrue(UnionKey.sameWith(key, elements));
    assertFalse(UnionKey.sameWith(null, elements));

    assertFalse(UnionKey.startsWith(null, elements));
    assertFalse(UnionKey.startsWith(key, new Object[]{"appId", "app"}));
    assertTrue(UnionKey.startsWith(key, elements));
    assertTrue(UnionKey.startsWith(key, new Object[]{"appId"}));
    assertTrue(UnionKey.startsWith(key, new Object[]{"appId", 1}));
    assertTrue(UnionKey.startsWith(key, new Object[]{"appId", 1, 1}));
  }
}
