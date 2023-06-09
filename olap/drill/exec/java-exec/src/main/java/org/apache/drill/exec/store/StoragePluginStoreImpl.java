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

import static org.apache.drill.shaded.guava.com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.drill.common.config.LogicalPlanPersistence;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.exception.StoreException;
import org.apache.drill.exec.planner.logical.StoragePlugins;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.store.sys.CaseInsensitivePersistentStore;
import org.apache.drill.exec.store.sys.PersistentStore;
import org.apache.drill.exec.store.sys.PersistentStoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Concrete storage plugin (configuration) store based on the
 * {@link PersistentStore} abstraction.
 */
public class StoragePluginStoreImpl implements StoragePluginStore {
  private static final Logger logger = LoggerFactory.getLogger(StoragePluginStoreImpl.class);

  private final PersistentStore<StoragePluginConfig> pluginSystemTable;

  public StoragePluginStoreImpl(DrillbitContext context) {
    this.pluginSystemTable = initPluginsSystemTable(context,
        checkNotNull(context.getLpPersistence()));
  }

  /**
   * <ol>
   *   <li>Initializes persistent store for storage plugins.</li>
   *   <li>Since storage plugins names are case-insensitive in Drill, to ensure backward compatibility,
   *   re-writes those not stored in lower case with lower case names, for duplicates issues warning. </li>
   *   <li>Wraps plugin system table into case insensitive wrapper.</li>
   * </ol>
   *
   * @param context drillbit context
   * @param lpPersistence deserialization mapper provider
   * @return persistent store for storage plugins
   */
  private PersistentStore<StoragePluginConfig> initPluginsSystemTable(
      DrillbitContext context, LogicalPlanPersistence lpPersistence) {
    try {
      PersistentStore<StoragePluginConfig> pluginSystemTable = context
          .getStoreProvider()
          .getOrCreateStore(PersistentStoreConfig
              .newJacksonBuilder(lpPersistence.getMapper(), StoragePluginConfig.class)
              .name(StoragePluginRegistryImpl.PSTORE_NAME)
              .build());

      Iterator<Entry<String, StoragePluginConfig>> storedPlugins = pluginSystemTable.getAll();
      while (storedPlugins.hasNext()) {
        Entry<String, StoragePluginConfig> entry = storedPlugins.next();
        String pluginName = entry.getKey();
        if (!pluginName.equals(pluginName.toLowerCase())) {
          logger.debug("Replacing plugin name {} with its lower case equivalent.", pluginName);
          pluginSystemTable.delete(pluginName);
          if (!pluginSystemTable.putIfAbsent(pluginName.toLowerCase(), entry.getValue())) {
            logger.warn("Duplicated storage plugin name [{}] is found. Duplicate is deleted from persistent storage.", pluginName);
          }
        }
      }

      return new CaseInsensitivePersistentStore<>(pluginSystemTable);
    } catch (StoreException e) {
      throw new DrillRuntimeException(
          "Failure while reading and loading storage plugin configuration.");
    }
  }

  @Override
  public boolean isInitialized() {

    // TODO: This is not the best way to check: it will deserialize the
    // first entry. What we really want to know is: are there any
    // entries at all? (This version is better than the previous,
    // which deserialized all entries, then discarded them.)
    return pluginSystemTable.getRange(0, 1).hasNext();
  }

  @Override
  public StoragePluginConfig get(String name) {
    return pluginSystemTable.get(name);
  }

  @Override
  public void put(String name, StoragePluginConfig config) {
    pluginSystemTable.put(name, config);
  }

  @Override
  public void delete(String name) {
    pluginSystemTable.delete(name);
  }

  @Override
  public Iterator<Entry<String, StoragePluginConfig>> load() {
     return pluginSystemTable.getAll();
  }

  @Override
  public void putAll(StoragePlugins plugins) {
    for (Map.Entry<String, StoragePluginConfig> plugin : plugins) {
      put(plugin.getKey(), plugin.getValue());
    }
  }

  // TODO: Can this be removed? Avoid exposing implementation?
  @Override
  public PersistentStore<StoragePluginConfig> getStore() {
    return pluginSystemTable;
  }

  @Override
  public void close() {
    try {
      pluginSystemTable.close();
    } catch (Exception e) {
      logger.warn("Error closing the storage plugin store", e);
      // Ignore since we're shutting down the Drillbit
    }
  }
}
