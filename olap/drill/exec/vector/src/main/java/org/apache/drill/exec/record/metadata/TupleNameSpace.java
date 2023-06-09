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
package org.apache.drill.exec.record.metadata;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.drill.common.map.CaseInsensitiveMap;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;

/**
 * Implementation of a tuple name space. Tuples allow both indexed and
 * named access to their members.
 *
 * @param <T> the type of object representing each column
 */

public class TupleNameSpace<T> implements Iterable<T> {
  private final Map<String,Integer> nameSpace = CaseInsensitiveMap.newHashMap();
  private final List<T> entries = new ArrayList<>();

  public int add(String key, T value) {
    if (indexOf(key) != -1) {
      throw new IllegalArgumentException("Duplicate entry: " + key);
    }
    int index = entries.size();
    nameSpace.put(key, index);
    entries.add(value);
    return index;
  }

  public T get(int index) {
    return entries.get(index);
  }

  public T get(String key) {
    int index = indexOf(key);
    if (index == -1) {
      return null;
    }
    return get(index);
  }

  public int indexOf(String key) {
    Integer index = nameSpace.get(key);
    if (index == null) {
      return -1;
    }
    return index;
  }

  public int count() { return entries.size(); }

  @Override
  public Iterator<T> iterator() {
    return entries.iterator();
  }

  public boolean isEmpty() {
    return entries.isEmpty();
  }

  public List<T> entries() {
    return ImmutableList.copyOf(entries);
  }

  public void replace(String key, T replaceWith) {
    int index = indexOf(key);
    Preconditions.checkArgument(index != -1);
    entries.set(index, replaceWith);
  }

  @Override
  public String toString() {
    return entries.toString();
  }
}
