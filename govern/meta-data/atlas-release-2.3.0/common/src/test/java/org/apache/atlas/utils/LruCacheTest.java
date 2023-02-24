/**
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
package org.apache.atlas.utils;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
import org.testng.annotations.Test;

/**
 * Tests the LruCache.
 */
@Test
public class LruCacheTest {

    /**
     * Tests the basic operations on the cache.
     */
    @Test
    public void testBasicOps() throws Exception {

        LruCache<String, String> cache = new LruCache<>(1000, 0);
        // Get the static cache and populate it. Its size and other
        // characteristics depend on the bootstrap properties that are hard to
        // control in a test. So it is hard to see that if we add more entries
        // than the size of the cache one is evicted, or that it gets reaped at
        // the right time. However, a lot of this type of functionality is
        // tested by the underlying LruCache's test.

        // Note that query handle IDs are of the form sessionID::queryID
        String h1 = createHandle("s1::", "1::");
        String q1 = createQuery();

        String h2 = createHandle("s1::", "2::");
        String q2 = createQuery();

        String h3 = createHandle("s2::", "1::");
        String q3 = createQuery();

        String h4 = createHandle("s1::", "3::");
        String q4 = createQuery();

        String h5 = createHandle("s3::", null);
        String q5 = createQuery();

        String h5b = createHandle("s3::", null);
        String q5b = createQuery();

        String h6 = createHandle(null, "3::");
        String q6 = createQuery();

        String h6b = createHandle(null, "3::");
        String q6b = createQuery();

        // Test put and get.
        cache.put(h1, q1);
        cache.put(h2, q2);
        cache.put(h3, q3);
        cache.put(h4, q4);
        cache.put(h5, q5);
        cache.put(h6, q6);

        assertEquals(cache.get(h1), q1);
        assertEquals(cache.get(h2), q2);
        assertEquals(cache.get(h3), q3);
        assertEquals(cache.get(h4), q4);
        assertEquals(cache.get(h5), q5);

        assertEquals(cache.remove(h1), q1);
        assertEquals(cache.remove(h2), q2);
        assertEquals(cache.remove(h3), q3);
        assertEquals(cache.remove(h4), q4);
        assertEquals(cache.remove(h5), q5);
        assertNull(cache.remove(h5b));
        assertEquals(q6, cache.remove(h6));
        assertNull(cache.remove(h6b));

        cache.put(h5b, q5b);
        cache.put(h6b, q6b);

        assertEquals(q5b, cache.remove(h5));
        assertNull(cache.remove(h5b));
        assertEquals(q6b, cache.remove(h6));
        assertNull(cache.remove(h6b));
    }

    @Test
    public void testMapOperations() {

        Map<String, String> reference = new HashMap<>();
        reference.put("name", "Fred");
        reference.put("occupation", "student");
        reference.put("height", "5'11");
        reference.put("City", "Littleton");
        reference.put("State", "MA");

        LruCache<String, String> map = new LruCache<>(10, 10);
        map.putAll(reference);

        assertEquals(map.size(), reference.size());
        assertEquals(map.keySet().size(), reference.keySet().size());
        assertTrue(map.keySet().containsAll(reference.keySet()));
        assertTrue(reference.keySet().containsAll(map.keySet()));

        assertEquals(reference.entrySet().size(), map.entrySet().size());
        for(Map.Entry<String, String> entry : map.entrySet()) {
            assertTrue(reference.containsKey(entry.getKey()));
            assertEquals(entry.getValue(), reference.get(entry.getKey()));
            assertTrue(map.containsKey(entry.getKey()));
            assertTrue(map.containsValue(entry.getValue()));
            assertTrue(map.values().contains(entry.getValue()));
        }
        assertTrue(reference.equals(map));
        assertTrue(map.equals(reference));

    }

    @Test
    public void testReplaceValueInMap() {
        LruCache<String, String> map = new LruCache<>(10, 10);
        map.put("name", "Fred");
        map.put("name", "George");

        assertEquals(map.get("name"), "George");
        assertEquals(map.size(), 1);
    }



    @Test
    public void testOrderUpdatedWhenAddExisting() {
        LruCache<String, String> map = new LruCache<>(2, 10);
        map.put("name", "Fred");
        map.put("age", "15");
        map.put("name", "George");

        //age should be evicted
        map.put("height", "5'3\"");
        //age is now least recently used
        assertFalse(map.containsKey("age"));
    }

    @Test
    public void testMapRemove() {
        LruCache<String, String> map = new LruCache<>(10, 10);
        map.put("name", "Fred");
        map.put("occupation", "student");
        map.put("height", "5'11");
        map.put("City", "Littleton");
        map.put("State", "MA");
        assertMapHasSize(map, 5);
        assertTrue(map.containsKey("State"));
        map.remove("State");
        assertMapHasSize(map, 4);
        assertFalse(map.containsKey("State"));

    }

    private void assertMapHasSize(LruCache<String, String> map, int size) {
        assertEquals(map.size(), size);
        assertEquals(map.keySet().size(), size);
        assertEquals(map.values().size(), size);
        assertEquals(map.entrySet().size(), size);
    }

    @Test
    public void testEvict() {
        LruCache<String, String> map = new LruCache<>(5, 10);
        map.put("name", "Fred");
        map.put("occupation", "student");
        map.put("height", "5'11");
        map.put("City", "Littleton");
        map.put("State", "MA");
        assertMapHasSize(map, 5);

        //name should be evicted next
        assertTrue(map.containsKey("name"));
        map.put("zip", "01460");
        assertFalse(map.containsKey("name"));
        assertMapHasSize(map, 5);

        map.get("occupation");
        //height should be evicted next
        assertTrue(map.containsKey("height"));
        map.put("country", "USA");
        assertFalse(map.containsKey("height"));
        assertMapHasSize(map, 5);
    }

    /**
     * Create a fake query handle for testing.
     *
     * @param queryPrefix
     * @param pkgPrefix
     * @return a new query handle.
     */
    private String createHandle(String s1, String s2) {
        return s1 + ": " + s2 + ":select x from x in y";
    }

    /**
     * Create a mock IInternalQuery.
     *
     * @return a mock IInternalQuery.
     * @throws QueryException
     */
    private String createQuery() {
        return RandomStringUtils.randomAlphabetic(10);
    }


}
