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
package org.apache.drill.exec.coord.store;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.drill.shaded.guava.com.google.common.base.Function;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Iterators;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;

public abstract class BaseTransientStore<V> implements TransientStore<V> {
  private final Set<TransientStoreListener> listeners = Collections.newSetFromMap(
      Maps.<TransientStoreListener, Boolean>newConcurrentMap());

  protected final TransientStoreConfig<V> config;

  protected BaseTransientStore(final TransientStoreConfig<V> config) {
    this.config = Preconditions.checkNotNull(config, "config cannot be null");
  }

  public TransientStoreConfig<V> getConfig() {
    return config;
  }

  @Override
  public Iterator<String> keys() {
    return Iterators.transform(entries(), new Function<Map.Entry<String, V>, String>() {
      @Nullable
      @Override
      public String apply(@Nullable Map.Entry<String, V> input) {
        return input.getKey();
      }
    });
  }

  @Override
  public Iterator<V> values() {
    return Iterators.transform(entries(), new Function<Map.Entry<String, V>, V>() {
      @Nullable
      @Override
      public V apply(final Map.Entry<String, V> entry) {
        return entry.getValue();
      }
    });
  }

  protected void fireListeners(final TransientStoreEvent<?> event) {
    for (final TransientStoreListener listener:listeners) {
      listener.onChange(event);
    }
  }

  @Override
  public void addListener(final TransientStoreListener listener) {
    listeners.add(Preconditions.checkNotNull(listener, "listener cannot be null"));
  }

  @Override
  public void removeListener(final TransientStoreListener listener) {
    listeners.remove(Preconditions.checkNotNull(listener, "listener cannot be null"));
  }
}
