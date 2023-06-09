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
package org.apache.drill.exec.store.sys;

import org.apache.drill.exec.alias.AliasRegistry;
import org.apache.drill.exec.alias.AliasTarget;
import org.apache.drill.exec.alias.Aliases;
import org.apache.drill.exec.alias.PersistentAliasRegistry;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.store.pojo.NonNullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.StreamSupport;

/**
 * List aliases as a System Table
 */
public class AliasesIterator implements Iterator<Object> {

  private final Iterator<AliasInfo> iterator;

  public AliasesIterator(FragmentContext context, AliasTarget aliasTarget, int maxRecords) {
    AliasRegistry storageAliasesRegistry;
    switch (aliasTarget) {
      case STORAGE:
        storageAliasesRegistry = context.getAliasRegistryProvider().getStorageAliasesRegistry();
        break;
      case TABLE:
        storageAliasesRegistry = context.getAliasRegistryProvider().getTableAliasesRegistry();
        break;
      default:
        iterator = Collections.emptyIterator();
        return;
    }
    Iterable<Map.Entry<String, Aliases>> allAliases = storageAliasesRegistry::getAllAliases;

    iterator = StreamSupport.stream(allAliases.spliterator(), false)
      .flatMap(aliasesTable -> StreamSupport.stream(getAllAliases(aliasesTable).spliterator(), false)
        .map(entry -> getAliasInfo(entry.getKey(), entry.getValue(), aliasesTable.getKey())))
      .limit(maxRecords)
      .iterator();
  }

  private AliasInfo getAliasInfo(String alias, String value, String key) {
    boolean isPublic = key.equals(PersistentAliasRegistry.PUBLIC_ALIASES_KEY);
    return new AliasInfo(alias, value, isPublic ? null : key, isPublic);
  }

  private Iterable<Map.Entry<String, String>> getAllAliases(Map.Entry<String, Aliases> aliasesTable) {
    return () -> aliasesTable.getValue().getAllAliases();
  }

  @Override
  public boolean hasNext() {
    return iterator.hasNext();
  }

  @Override
  public AliasInfo next() {
    return iterator.next();
  }

  /**
   * Representation of an entry in the System table - Aliases
   */
  public static class AliasInfo {
    @NonNullable
    public final String alias;

    @NonNullable
    public final String name;

    @NonNullable
    public final String user;

    @NonNullable
    public final boolean isPublic;

    public AliasInfo(String alias, String name, String user, boolean isPublic) {
      this.name = name;
      this.alias = alias;
      this.user = user;
      this.isPublic = isPublic;
    }
  }
}
