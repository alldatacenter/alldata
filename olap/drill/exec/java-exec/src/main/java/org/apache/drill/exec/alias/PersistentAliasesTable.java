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

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.drill.exec.store.sys.PersistentStore;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Implementation of aliases table that updates its version in persistent store after modifications.
 */
public class PersistentAliasesTable implements Aliases {
  private final Map<String, String> aliases;

  private final String key;

  private final PersistentStore<PersistentAliasesTable> store;

  @JsonCreator
  public PersistentAliasesTable(
    @JsonProperty("aliases") Map<String, String> aliases,
    @JsonProperty("key") String key,
    @JacksonInject PersistentAliasRegistry.StoreProvider storeProvider) {
    this.aliases = aliases != null ? aliases : new HashMap<>();
    this.key = key;
    this.store = storeProvider.getStore();
  }

  @Override
  @JsonProperty("key")
  public String getKey() {
    return key;
  }

  @Override
  public String get(String alias) {
    return aliases.get(alias);
  }

  @Override
  public boolean put(String alias, String value, boolean replace) {
    if (replace || !aliases.containsKey(alias)) {
      aliases.put(alias, value);
      store.put(key, this);
      return true;
    }
    return false;
  }

  @Override
  public boolean remove(String alias) {
    boolean isRemoved = aliases.remove(alias) != null;
    store.put(key, this);
    return isRemoved;
  }

  @Override
  @JsonIgnore
  public Iterator<Map.Entry<String, String>> getAllAliases() {
    return aliases.entrySet().iterator();
  }

  @JsonProperty("aliases")
  public Map<String, String> getAliases() {
    return aliases;
  }
}
