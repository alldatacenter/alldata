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
package org.apache.drill.exec.store.enumerable.plan;

import org.apache.calcite.schema.Table;
import org.apache.drill.exec.store.AbstractSchema;
import org.apache.drill.exec.store.StoragePlugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class EnumMockSchema extends AbstractSchema {
  private final StoragePlugin plugin;
  private final Map<String, EnumMockTable> tables;

  public EnumMockSchema(String name, StoragePlugin plugin) {
    super(Collections.emptyList(), name);
    this.plugin = plugin;
    this.tables = new HashMap<>();
  }

  @Override
  public String getTypeName() {
    return EnumMockPlugin.EnumMockStoragePluginConfig.NAME;
  }

  @Override
  public Table getTable(String name) {
    return tables.computeIfAbsent(name, key -> new EnumMockTable(plugin, key, null, null));
  }

  @Override
  public Set<String> getTableNames() {
    return Collections.singleton("mock_enum_table");
  }
}
