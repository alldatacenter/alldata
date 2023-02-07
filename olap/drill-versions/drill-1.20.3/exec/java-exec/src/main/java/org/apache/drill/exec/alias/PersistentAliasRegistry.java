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
package org.apache.drill.exec.alias;

import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.exception.StoreException;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.store.sys.PersistentStore;
import org.apache.drill.exec.store.sys.PersistentStoreConfig;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Implementation of {@link AliasRegistry} that persists aliases tables
 * to the pre-configured persistent store.
 */
public class PersistentAliasRegistry implements AliasRegistry {
  public static final String PUBLIC_ALIASES_KEY = "$public_aliases";

  private final PersistentStore<PersistentAliasesTable> store;

  private final boolean useUserAliases;

  public PersistentAliasRegistry(DrillbitContext context, String registryPath) {
    try {
      ObjectMapper mapper = context.getLpPersistence().getMapper().copy();
      InjectableValues injectables = new InjectableValues.Std()
        .addValue(StoreProvider.class, new StoreProvider(this::getStore));

      mapper.setInjectableValues(injectables);
      this.store = context
        .getStoreProvider()
        .getOrCreateStore(PersistentStoreConfig
          .newJacksonBuilder(mapper, PersistentAliasesTable.class)
          .name(registryPath)
          .build());

      this.useUserAliases = context.getConfig().getBoolean(ExecConstants.IMPERSONATION_ENABLED);
    } catch (StoreException e) {
      throw new DrillRuntimeException(
        "Failure while reading and loading alias table.");
    }
  }

  public PersistentStore<PersistentAliasesTable> getStore() {
    return store;
  }

  private ResolvedAliases getResolvedAliases(String key) {
    // for the case when impersonation is disabled, use public aliases only
    PersistentAliasesTable userAliases = useUserAliases ? store.get(key) : null;
    return userAliases != null
      ? new ResolvedAliases(userAliases, this::getPublicAliases)
      : new ResolvedAliases(EmptyAliases.INSTANCE, this::getPublicAliases);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public Iterator<Map.Entry<String, Aliases>> getAllAliases() {
    return (Iterator) store.getAll();
  }

  @Override
  public Aliases getUserAliases(String userName) {
    return getResolvedAliases(userName);
  }

  @Override
  public void createUserAliases(String userName) {
    if (!store.contains(userName)) {
      PersistentAliasesTable aliasesTable =
        new PersistentAliasesTable(new HashMap<>(), userName, new StoreProvider(this::getStore));
      store.put(userName, aliasesTable);
    }
  }

  @Override
  public void createPublicAliases() {
    if (!store.contains(PUBLIC_ALIASES_KEY)) {
      PersistentAliasesTable publicAliases =
        new PersistentAliasesTable(new HashMap<>(), PUBLIC_ALIASES_KEY, new StoreProvider(this::getStore));
      store.put(PUBLIC_ALIASES_KEY, publicAliases);
    }
  }

  @Override
  public void deleteUserAliases(String userName) {
    store.delete(userName);
  }

  @Override
  public void deletePublicAliases() {
    store.delete(PUBLIC_ALIASES_KEY);
  }

  @Override
  public Aliases getPublicAliases() {
    return Optional.<Aliases>ofNullable(store.get(PUBLIC_ALIASES_KEY))
      .orElse(EmptyAliases.INSTANCE);
  }

  @Override
  public void close() throws Exception {
    store.close();
  }

  public static class StoreProvider {
    private final Supplier<PersistentStore<PersistentAliasesTable>> supplier;

    public StoreProvider(Supplier<PersistentStore<PersistentAliasesTable>> supplier) {
      this.supplier = supplier;
    }

    public PersistentStore<PersistentAliasesTable> getStore() {
      return supplier.get();
    }
  }
}
