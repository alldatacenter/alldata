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
package org.apache.drill.exec.store.http;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.map.CaseInsensitiveMap;
import org.apache.drill.exec.planner.logical.DynamicDrillTable;
import org.apache.drill.exec.store.AbstractSchema;
import org.apache.drill.exec.store.AbstractSchemaFactory;
import org.apache.drill.exec.store.SchemaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpSchemaFactory extends AbstractSchemaFactory {
  private static final Logger logger = LoggerFactory.getLogger(HttpSchemaFactory.class);

  private final HttpStoragePlugin plugin;

  public HttpSchemaFactory(HttpStoragePlugin plugin) {
    super(plugin.getName());
    this.plugin = plugin;
  }

  @Override
  public void registerSchemas(SchemaConfig schemaConfig, SchemaPlus parent) {
    HttpSchema schema = new HttpSchema(plugin);
    logger.debug("Registering {} {}", schema.getName(), schema.toString());

    SchemaPlus schemaPlus = parent.add(getName(), schema);
    schema.setHolder(schemaPlus);
  }

  protected static class HttpSchema extends AbstractSchema {

    private final HttpStoragePlugin plugin;
    private final Map<String, HttpAPIConnectionSchema> subSchemas = CaseInsensitiveMap.newHashMap();
    private final Map<String, HttpApiConfig> tables = CaseInsensitiveMap.newHashMap();
    private final Map<String, DynamicDrillTable> activeTables = CaseInsensitiveMap.newHashMap();

    public HttpSchema(HttpStoragePlugin plugin) {
      super(Collections.emptyList(), plugin.getName());
      this.plugin = plugin;
      for (Entry<String, HttpApiConfig> entry : plugin.getConfig().connections().entrySet()) {
        String configName = entry.getKey();
        HttpApiConfig config = entry.getValue();
        if (config.requireTail()) {
          subSchemas.put(configName, new HttpAPIConnectionSchema(this, configName, plugin));
        } else {
          tables.put(configName, config);
        }
      }
    }

    void setHolder(SchemaPlus plusOfThis) {
      for (Entry<String, HttpAPIConnectionSchema> entry : subSchemas.entrySet()) {
        plusOfThis.add(entry.getKey(), entry.getValue());
      }
    }

    @Override
    public AbstractSchema getSubSchema(String name) {
      HttpAPIConnectionSchema subSchema = subSchemas.get(name);
      if (subSchema != null) {
        return subSchema;
      } else if (tables.containsKey(name)) {
        return null;
      } else {
        throw UserException
          .connectionError()
          .message("API '%s' does not exist in HTTP storage plugin '%s'", name, getName())
          .build(logger);
      }
    }

    @Override
    public Table getTable(String name) {
      DynamicDrillTable table = activeTables.get(name);
      if (table != null) {
        return table;
      }
      HttpApiConfig config = tables.get(name);
      if (config != null) {
        // Register a new table
        return registerTable(name, new DynamicDrillTable(plugin, plugin.getName(),
            new HttpScanSpec(plugin.getName(), name, null,
                plugin.getConfig().copyForPlan(name), plugin.getTokenTable(), plugin.getRegistry())));
      } else {
        return null; // Unknown table
      }
    }

    @Override
    public String getTypeName() {
      return HttpStoragePluginConfig.NAME;
    }

    private DynamicDrillTable registerTable(String name, DynamicDrillTable table) {
      activeTables.put(name, table);
      return table;
    }
  }
}
