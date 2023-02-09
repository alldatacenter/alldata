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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.common.utils;


import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class CaseInsensitiveMapTest {
    @Test
    public void testCaseInsensitiveMap() {

        CaseInsensitiveMap<String> map = new CaseInsensitiveMap<String>();

        assertEquals(0, map.size());
        assertEquals(true, map.isEmpty());

        map.put("key", "value");
        assertEquals("value", map.get("key"));
        assertEquals("value", map.get("KEY"));
        assertEquals(true, map.containsKey("KEY"));
        assertEquals(true, map.containsValue("value"));
        assertEquals(false, map.containsValue("Value"));
        assertEquals(1, map.size());

        Map<String, String> map2 = new HashMap<String, String>();
        map2.put("key", "value");
        map2.put("Key", "Value");
        assertEquals(2, map2.size());
        map.clear();
        assertEquals(0, map.size());
        map.putAll(map2);
        assertEquals(1, map.size());

        CaseInsensitiveMap<String> map3 = new CaseInsensitiveMap<String>(map);
        assertEquals(1, map3.size());

        assertEquals(1, map.keySet().size());
        assertEquals(1, map.values().size());
        assertEquals(1, map.entrySet().size());

        map.remove("key");
        assertEquals(0, map.size());
    }

}
