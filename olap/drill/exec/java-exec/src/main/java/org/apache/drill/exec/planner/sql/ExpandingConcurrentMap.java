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
package org.apache.drill.exec.planner.sql;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.drill.shaded.guava.com.google.common.collect.Maps;

/**
 * A special type of concurrent map which attempts to create an object before returning that it does not exist.  It will also provide the same functionality on it's keyset.
 * @param <KEY> The key in the map.
 * @param <VALUE> The value in the map.
 */
public class ExpandingConcurrentMap<KEY, VALUE> implements ConcurrentMap<KEY, VALUE> {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ExpandingConcurrentMap.class);

  private final ConcurrentMap<KEY, VALUE> internalMap = Maps.newConcurrentMap();
  private final DelegatingKeySet keySet = new DelegatingKeySet();
  private final MapValueFactory<KEY, VALUE> fac;

  /**
   * Create a new ExpandingConcurrentMap.
   * @param fac The object factory responsible for attempting to generate object instances.
   */
  public ExpandingConcurrentMap(MapValueFactory<KEY, VALUE> fac) {
    super();
    this.fac = fac;
  }

  @Override
  public int size() {
    return internalMap.size();
  }

  @Override
  public boolean isEmpty() {
    return internalMap.isEmpty();
  }

  public boolean alreadyContainsKey(Object k) {
    @SuppressWarnings("unchecked") KEY key = (KEY) k;

    if (internalMap.containsKey(key)) {
      return true;
    }
    return false;
  }

  @Override
  public boolean containsKey(Object k) {
    @SuppressWarnings("unchecked") KEY key = (KEY) k;

    if (internalMap.containsKey(key)) {
      return true;
    }
    VALUE v = getNewEntry(k);
    return v != null;
  }

  @Override
  public boolean containsValue(Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public VALUE get(Object key) {
    VALUE out = internalMap.get(key);
    if (out != null) {
      return out;
    }
    return getNewEntry(key);
  }

  private VALUE getNewEntry(Object k) {
    @SuppressWarnings("unchecked")
    KEY key = (KEY) k;
    VALUE v = this.fac.create(key);
    if (v == null) {
      return null;
    }
    VALUE old = internalMap.putIfAbsent(key, v);
    if (old == null) {
      return v;
    }
    fac.destroy(v);
    return old;
  }

  @Override
  public VALUE put(KEY key, VALUE value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public VALUE remove(Object key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void putAll(Map<? extends KEY, ? extends VALUE> m) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<KEY> keySet() {
    return this.keySet;
  }

  @Override
  public Collection<VALUE> values() {
    return internalMap.values();
  }

  @Override
  public Set<java.util.Map.Entry<KEY, VALUE>> entrySet() {
    return internalMap.entrySet();
  }

  @Override
  public VALUE putIfAbsent(KEY key, VALUE value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean remove(Object key, Object value) {
    return false;
  }

  @Override
  public boolean replace(KEY key, VALUE oldValue, VALUE newValue) {
    return false;
  }

  @Override
  public VALUE replace(KEY key, VALUE value) {
    return null;
  }

  private class DelegatingKeySet implements Set<KEY>{

    @Override
    public int size() {
      return ExpandingConcurrentMap.this.size();
    }

    @Override
    public boolean isEmpty() {
      return ExpandingConcurrentMap.this.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
      return ExpandingConcurrentMap.this.containsKey(o);
    }

    @Override
    public Iterator<KEY> iterator() {
      return internalMap.keySet().iterator();
    }

    @Override
    public Object[] toArray() {
      return internalMap.keySet().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
      return internalMap.keySet().toArray(a);
    }

    @Override
    public boolean add(KEY e) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      for (Object o : c) {
        if (this.contains(o)) {
          return false;
        }
      }
      return true;
    }

    @Override
    public boolean addAll(Collection<? extends KEY> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException();
    }

  }

  public interface MapValueFactory<KEY, VALUE> {

    public VALUE create(KEY key);
    public void destroy(VALUE value);
  }

}
