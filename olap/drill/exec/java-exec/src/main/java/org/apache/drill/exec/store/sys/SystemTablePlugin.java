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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.calcite.schema.SchemaPlus;
import org.apache.drill.common.JSONOptions;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.common.map.CaseInsensitiveMap;
import org.apache.drill.exec.physical.base.AbstractGroupScan;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.store.AbstractSchema;
import org.apache.drill.exec.store.AbstractStoragePlugin;
import org.apache.drill.exec.store.SchemaConfig;
import org.apache.drill.exec.store.SystemPlugin;
import org.apache.drill.exec.store.pojo.PojoDataType;

/**
 * A "storage" plugin for system tables.
 */
@SystemPlugin
public class SystemTablePlugin extends AbstractStoragePlugin {

  public static final String SYS_SCHEMA_NAME = "sys";

  private final SystemTablePluginConfig config;
  private final SystemSchema schema;

  @SuppressWarnings("unused") // used in StoragePluginRegistryImpl to dynamically init system plugins
  public SystemTablePlugin(DrillbitContext context) {
    this(SystemTablePluginConfig.INSTANCE, context, SYS_SCHEMA_NAME);
  }

  public SystemTablePlugin(SystemTablePluginConfig config, DrillbitContext context, String name) {
    super(context, name);
    this.config = config;
    this.schema = new SystemSchema(this);
  }

  @Override
  public StoragePluginConfig getConfig() {
    return config;
  }

  @Override
  public void registerSchemas(SchemaConfig schemaConfig, SchemaPlus parent) {
    parent.add(schema.getName(), schema);
  }

  @Override
  public AbstractGroupScan getPhysicalScan(String userName, JSONOptions selection, List<SchemaPath> columns) {
    SystemTable table = selection.getWith(getContext().getLpPersistence().getMapper(), SystemTable.class);
    return new SystemTableScan(table, this);
  }

  /**
   * This class defines a namespace for {@link org.apache.drill.exec.store.sys.SystemTable}s
   */
  private class SystemSchema extends AbstractSchema {

    private final Map<String, StaticDrillTable> tables;

    SystemSchema(SystemTablePlugin plugin) {

      super(Collections.emptyList(), SYS_SCHEMA_NAME);

      this.tables = Arrays.stream(SystemTable.values())
          .collect(
              Collectors.toMap(
                  SystemTable::getTableName,
                  table -> new StaticDrillTable(getName(), plugin, TableType.SYSTEM_TABLE, table, new PojoDataType(table.getPojoClass())),
                  (o, n) -> n,
                  CaseInsensitiveMap::newHashMap));
    }

    @Override
    public Set<String> getTableNames() {
      return tables.keySet();
    }

    @Override
    public DrillTable getTable(String name) {
      return tables.get(name);
    }

    @Override
    public String getTypeName() {
      return SystemTablePluginConfig.NAME;
    }

    @Override
    public boolean areTableNamesCaseSensitive() {
      return false;
    }
  }
}
