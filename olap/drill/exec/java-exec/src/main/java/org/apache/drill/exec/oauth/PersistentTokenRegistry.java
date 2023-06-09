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
package org.apache.drill.exec.oauth;

import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.exception.StoreException;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.store.sys.PersistentStore;
import org.apache.drill.exec.store.sys.PersistentStoreConfig;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Implementation of {@link TokenRegistry} that persists token tables
 * to the pre-configured persistent store.
 */
public class PersistentTokenRegistry implements TokenRegistry {
  private final PersistentStore<PersistentTokenTable> store;

  public PersistentTokenRegistry(DrillbitContext context, String registryPath) {
    try {
      ObjectMapper mapper = context.getLpPersistence().getMapper().copy();
      InjectableValues injectables = new InjectableValues.Std()
        .addValue(StoreProvider.class, new StoreProvider(this::getStore));

      mapper.setInjectableValues(injectables);
      this.store = context
        .getStoreProvider()
        .getOrCreateStore(PersistentStoreConfig
          .newJacksonBuilder(mapper, PersistentTokenTable.class)
          .name(registryPath)
          .build());
    } catch (StoreException e) {
      throw new DrillRuntimeException(
        "Failure while reading and loading token table.");
    }
  }

  public PersistentStore<PersistentTokenTable> getStore() {
    return store;
  }

  @Override
  public PersistentTokenTable getTokenTable(String name) {
    name = name.toLowerCase();
    if (!store.contains(name)) {
      createTokenTable(name);
    }
    return store.get(name);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public Iterator<Map.Entry<String, Tokens>> getAllTokens() {
    return (Iterator) store.getAll();
  }

  @Override
  public void createTokenTable(String pluginName) {
    // In Drill, Storage plugin names are stored in lower case. These checks make sure
    // that the tokens are associated with the correct plugin
    pluginName = pluginName.toLowerCase();
    if (!store.contains(pluginName)) {
      PersistentTokenTable tokenTable =
        new PersistentTokenTable(new HashMap<>(), pluginName, new StoreProvider(this::getStore));
      store.put(pluginName, tokenTable);
    }
  }

  @Override
  public void deleteTokenTable(String pluginName) {
    pluginName = pluginName.toLowerCase();
    if (store.contains(pluginName)) {
      store.delete(pluginName);
    }
  }

  @Override
  public void close() throws Exception {
    store.close();
  }

  public static class StoreProvider {
    private final Supplier<PersistentStore<PersistentTokenTable>> supplier;

    public StoreProvider(Supplier<PersistentStore<PersistentTokenTable>> supplier) {
      this.supplier = supplier;
    }

    public PersistentStore<PersistentTokenTable> getStore() {
      return supplier.get();
    }
  }
}
