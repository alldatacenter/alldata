/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.util;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements memoization semantics.
 * <p>
 * Conceptually a function from K -> V that computes values lazily and remembers the results.
 * Often used to implement a data store per key.
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 * @since 1.281
 */
public abstract class Memoizer<K, V> {

    private final ConcurrentHashMap<K, V> store = new ConcurrentHashMap<K, V>();

    public V get(K key) {
        V v = store.get(key);
        if (v != null)
            return v;
        // that represents "the value is being computed". FingerprintMap does this.
        synchronized (this) {
            v = store.get(key);
            if (v != null) {
                return v;
            }
            v = compute(key);
            store.put(key, v);
            return v;
        }
    }

    /**
     * Creates a new instance.
     */
    public abstract V compute(K key);

    /**
     * Clears all the computed values.
     */
    public void clear() {
        store.clear();
    }

    public void clear(K key) {
        store.remove(key);
    }

    /**
     * Provides a snapshot view of all {@code V}s.
     */
    public Iterable<V> values() {
        return store.values();
    }

    public Set<Map.Entry<K, V>> getEntries() {
        return store.entrySet();
    }

    public Map<K, V> snapshot() {
        return Collections.unmodifiableMap(this.store);
    }
}
