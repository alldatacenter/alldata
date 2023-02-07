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
package org.apache.drill.exec.store;

import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.planner.logical.StoragePlugins;
import org.apache.drill.exec.store.sys.PersistentStore;

/**
 * Interface to the storage mechanism used to store storage plugin
 * configurations, typically in JSON format.
 */
public interface StoragePluginStore {
  boolean isInitialized();
  void delete(String name);
  Iterator<Entry<String, StoragePluginConfig>> load();
  void put(String name, StoragePluginConfig config);
  void putAll(StoragePlugins plugins);
  StoragePluginConfig get(String name);
  PersistentStore<StoragePluginConfig> getStore();
  void close();
}
