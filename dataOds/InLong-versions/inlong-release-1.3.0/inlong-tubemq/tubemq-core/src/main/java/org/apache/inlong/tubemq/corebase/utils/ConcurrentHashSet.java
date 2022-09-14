/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.corebase.utils;

import java.util.AbstractSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ConcurrentHashSet, construct the set collection through ConcurrentHashMap
 *  to complete the operation management of the concurrent set
 */
public class ConcurrentHashSet<E> extends AbstractSet<E> {

    private final ConcurrentHashMap<E, Long> keyValMap
            = new ConcurrentHashMap<>();

    public ConcurrentHashSet() {

    }

    @Override
    public boolean add(E item) {
        Long value =
                keyValMap.putIfAbsent(item, System.currentTimeMillis());
        return (value == null);
    }

    @Override
    public boolean contains(Object item) {
        return keyValMap.containsKey(item);
    }

    @Override
    public boolean remove(Object item) {
        return keyValMap.remove(item) != null;
    }

    @Override
    public void clear() {
        keyValMap.clear();
    }

    @Override
    public int size() {
        return keyValMap.size();
    }

    @Override
    public boolean isEmpty() {
        return keyValMap.isEmpty();
    }

    @Override
    public Iterator<E> iterator() {
        return new HashSet<>(keyValMap.keySet()).iterator();
    }

    public Long getItemAddTime(E item) {
        return keyValMap.get(item);
    }
}
