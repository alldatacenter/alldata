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

import static com.aliyun.oss.common.utils.CodingUtils.assertParameterNotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CaseInsensitiveMap<V> implements Map<String, V> {

    private Map<String, V> wrappedMap;

    public CaseInsensitiveMap() {
        this(new HashMap<String, V>());
    }

    public CaseInsensitiveMap(Map<String, V> wrappedMap) {
        assertParameterNotNull(wrappedMap, "wrappedMap");

        this.wrappedMap = wrappedMap;
    }

    public int size() {
        return wrappedMap.size();
    }

    public boolean isEmpty() {
        return wrappedMap.isEmpty();
    }

    public boolean containsKey(Object key) {
        return wrappedMap.containsKey(key.toString().toLowerCase());
    }

    public boolean containsValue(Object value) {
        return wrappedMap.containsValue(value);
    }

    public V get(Object key) {
        return wrappedMap.get(key.toString().toLowerCase());
    }

    public V put(String key, V value) {
        return wrappedMap.put(key.toLowerCase(), value);
    }

    public V remove(Object key) {
        return wrappedMap.remove(key.toString().toLowerCase());
    }

    public void putAll(Map<? extends String, ? extends V> m) {
        for (java.util.Map.Entry<? extends String, ? extends V> entry : m.entrySet()) {
            this.put(entry.getKey(), entry.getValue());
        }
    }

    public void clear() {
        wrappedMap.clear();
    }

    public Set<String> keySet() {
        return wrappedMap.keySet();
    }

    public Collection<V> values() {
        return wrappedMap.values();
    }

    public Set<java.util.Map.Entry<String, V>> entrySet() {
        return wrappedMap.entrySet();
    }

}
