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

import org.apache.drill.common.AutoCloseables;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.server.DrillbitContext;

/**
 * Class for obtaining and managing storage and table alias registries.
 */
public class AliasRegistryProvider implements AutoCloseable {
  private static final String STORAGE_REGISTRY_PATH = "storage_aliases";

  private static final String TABLE_REGISTRY_PATH = "table_aliases";

  private final DrillbitContext context;

  private AliasRegistry storageAliasesRegistry;

  private AliasRegistry tableAliasesRegistry;

  public AliasRegistryProvider(DrillbitContext context) {
    this.context = context;
  }

  public AliasRegistry getStorageAliasesRegistry() {
    if (context.getOptionManager().getBoolean(ExecConstants.ENABLE_ALIASES)) {
      if (storageAliasesRegistry == null) {
        initRemoteRegistries();
      }
      return storageAliasesRegistry;
    }
    return NoopAliasRegistry.INSTANCE;
  }

  public AliasRegistry getTableAliasesRegistry() {
    if (context.getOptionManager().getBoolean(ExecConstants.ENABLE_ALIASES)) {
      if (tableAliasesRegistry == null) {
        initRemoteRegistries();
      }
      return tableAliasesRegistry;
    }
    return NoopAliasRegistry.INSTANCE;
  }

  private synchronized void initRemoteRegistries() {
    if (storageAliasesRegistry == null) {
      storageAliasesRegistry = new PersistentAliasRegistry(context, STORAGE_REGISTRY_PATH);
      tableAliasesRegistry = new PersistentAliasRegistry(context, TABLE_REGISTRY_PATH);
    }
  }

  @Override
  public void close() throws Exception {
    AutoCloseables.closeSilently(storageAliasesRegistry, tableAliasesRegistry);
  }
}
