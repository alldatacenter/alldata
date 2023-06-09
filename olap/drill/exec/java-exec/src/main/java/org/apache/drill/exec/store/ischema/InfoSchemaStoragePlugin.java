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
package org.apache.drill.exec.store.ischema;

import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.drill.common.JSONOptions;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.common.map.CaseInsensitiveMap;
import org.apache.drill.exec.ops.OptimizerRulesContext;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.store.AbstractSchema;
import org.apache.drill.exec.store.AbstractStoragePlugin;
import org.apache.drill.exec.store.SchemaConfig;
import org.apache.drill.exec.store.StoragePluginOptimizerRule;
import org.apache.drill.exec.store.SystemPlugin;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.IS_SCHEMA_NAME;

@SystemPlugin
public class InfoSchemaStoragePlugin extends AbstractStoragePlugin {

  private final InfoSchemaConfig config;

  @SuppressWarnings("unused") // used in StoragePluginRegistryImpl to dynamically init system plugins
  public InfoSchemaStoragePlugin(DrillbitContext context) {
    this(InfoSchemaConfig.INSTANCE, context, InfoSchemaConstants.IS_SCHEMA_NAME);
  }

  public InfoSchemaStoragePlugin(InfoSchemaConfig config, DrillbitContext context, String name) {
    super(context, name);
    this.config = config;
  }

  @Override
  public boolean supportsRead() {
    return true;
  }

  @Override
  public InfoSchemaGroupScan getPhysicalScan(String userName, JSONOptions selection, List<SchemaPath> columns) {
    InfoSchemaTableType table = selection.getWith(getContext().getLpPersistence().getMapper(),  InfoSchemaTableType.class);
    return new InfoSchemaGroupScan(table);
  }

  @Override
  public StoragePluginConfig getConfig() {
    return this.config;
  }

  @Override
  public void registerSchemas(SchemaConfig schemaConfig, SchemaPlus parent) {
    ISchema s = new ISchema(this);
    parent.add(s.getName(), s);
  }

  /**
   * Representation of the INFORMATION_SCHEMA schema.
   */
  private class ISchema extends AbstractSchema {

    private final Map<String, InfoSchemaDrillTable> tables;
    // for backward compatibility keep IS schema table names in upper case
    // the way they used to appear in INFORMATION_SCHEMA.TABLES table
    // though user can query them in any case
    private final Set<String> originalTableNames;

    ISchema(InfoSchemaStoragePlugin plugin) {

      super(Collections.emptyList(), IS_SCHEMA_NAME);

      this.tables = CaseInsensitiveMap.newHashMap();
      this.originalTableNames = new HashSet<>();

      Arrays.stream(InfoSchemaTableType.values()).forEach(
          table -> {
            tables.put(table.name(), new InfoSchemaDrillTable(plugin, getName(), table, config));
            originalTableNames.add(table.name());
          }
      );
    }

    @Override
    public Table getTable(String name) {
      return tables.get(name);
    }

    @Override
    public Set<String> getTableNames() {
      return originalTableNames;
    }

    @Override
    public String getTypeName() {
      return InfoSchemaConfig.NAME;
    }

    @Override
    public boolean areTableNamesCaseSensitive() {
      return false;
    }
  }

  @Override
  public Set<StoragePluginOptimizerRule> getPhysicalOptimizerRules(OptimizerRulesContext optimizerRulesContext) {
    return ImmutableSet.of(
        InfoSchemaPushFilterIntoRecordGenerator.IS_FILTER_ON_PROJECT,
        InfoSchemaPushFilterIntoRecordGenerator.IS_FILTER_ON_SCAN);
  }
}
