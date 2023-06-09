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
package org.apache.drill.exec.store.sys.store;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.drill.common.AutoCloseables.Closeable;
import org.apache.drill.common.concurrent.AutoCloseableLock;
import org.apache.drill.exec.exception.VersionMismatchException;
import org.apache.drill.exec.store.sys.PersistentStore;
import org.apache.drill.exec.store.sys.PersistentStoreMode;
import org.apache.drill.exec.store.sys.VersionedPersistentStore;

/**
 * Versioned store that delegates operations to PersistentStore and keeps versioning,
 * incrementing version each time write / delete operation is triggered.
 * Once created initial version is 0. Can be used only for local versioning, not distributed.
 *
 * @param <V> store value type
 */
public class VersionedDelegatingStore<V> implements VersionedPersistentStore<V> {
  private final PersistentStore<V> store;
  private final AutoCloseableLock readLock;
  private final AutoCloseableLock writeLock;
  private int version;

  public VersionedDelegatingStore(PersistentStore<V> store) {
    this.store = store;
    ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    readLock = new AutoCloseableLock(readWriteLock.readLock());
    writeLock = new AutoCloseableLock(readWriteLock.writeLock());
    version = 0;
  }

  @Override
  public PersistentStoreMode getMode() {
    return store.getMode();
  }

  @Override
  public void delete(final String key) {
    try (@SuppressWarnings("unused") Closeable lock = writeLock.open()) {
      store.delete(key);
      version++;
    }
  }

  @Override
  public boolean contains(final String key, final DataChangeVersion dataChangeVersion) {
    try (@SuppressWarnings("unused") Closeable lock = readLock.open()) {
      boolean contains = store.contains(key);
      dataChangeVersion.setVersion(version);
      return contains;
    }
  }

  @Override
  public V get(final String key, final DataChangeVersion dataChangeVersion) {
    try (@SuppressWarnings("unused") Closeable lock = readLock.open()) {
      V value = store.get(key);
      dataChangeVersion.setVersion(version);
      return value;
    }
  }

  @Override
  public void put(final String key, final V value, final DataChangeVersion dataChangeVersion) {
    try (@SuppressWarnings("unused") Closeable lock = writeLock.open()) {
      if (dataChangeVersion.getVersion() != version) {
        throw new VersionMismatchException("Version mismatch detected", dataChangeVersion.getVersion());
      }
      store.put(key, value);
      version++;
    }
  }

  @Override
  public boolean putIfAbsent(String key, V value) {
    try (@SuppressWarnings("unused") Closeable lock = writeLock.open()) {
      if (store.putIfAbsent(key, value)) {
        version++;
        return true;
      }
      return false;
    }
  }

  @Override
  public Iterator<Map.Entry<String, V>> getRange(final int skip, final int take) {
    try (@SuppressWarnings("unused") Closeable lock = readLock.open()) {
      return store.getRange(skip, take);
    }
  }

  @Override
  public void close() throws Exception
  {
    try (@SuppressWarnings("unused") Closeable lock = writeLock.open()) {
      store.close();
      version = DataChangeVersion.NOT_AVAILABLE;
    }
  }
}
