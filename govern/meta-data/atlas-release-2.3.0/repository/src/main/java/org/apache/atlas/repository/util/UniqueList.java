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
package org.apache.atlas.repository.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UniqueList<T> {
    private final List<T> list = new ArrayList<>();
    private final Set<T> set = new HashSet<>();

    public void add(T e) {
        if(set.contains(e)) {
            return;
        }

        list.add(e);
        set.add(e);
    }

    public void addAll(UniqueList<T> uniqueList) {
        for (T item : uniqueList.list) {
            if(set.contains(item)) continue;

            set.add(item);
            list.add(item);
        }
    }

    public void addAll(Collection<T> collection) {
        for (T item : collection) {
            add(item);
        }
    }

    public T remove(int index) {
        T e = list.remove(index);
        set.remove(e);
        return e;
    }

    public boolean contains(T e) {
        return set.contains(e);
    }

    public int size() {
        return list.size();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public void clear() {
        list.clear();
        set.clear();
    }

    public List<T> getList() {
        return list;
    }
}
