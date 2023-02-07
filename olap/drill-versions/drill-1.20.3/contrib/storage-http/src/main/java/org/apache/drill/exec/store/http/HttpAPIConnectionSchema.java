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

import org.apache.calcite.schema.Table;
import org.apache.drill.common.map.CaseInsensitiveMap;
import org.apache.drill.exec.planner.logical.DynamicDrillTable;
import org.apache.drill.exec.store.AbstractSchema;
import org.apache.drill.exec.store.http.HttpSchemaFactory.HttpSchema;

import java.util.Map;
import java.util.Set;

/**
 * In the HTTP storage plugin, users can define specific connections or APIs.
 * This class represents the database component of other storage plugins.
 */
public class HttpAPIConnectionSchema extends AbstractSchema {

  private final HttpStoragePlugin plugin;
  private final Map<String, DynamicDrillTable> activeTables = CaseInsensitiveMap.newHashMap();

  public HttpAPIConnectionSchema(HttpSchema parent,
                                 String name,
                                 HttpStoragePlugin plugin) {
    super(parent.getSchemaPath(), name);
    this.plugin = plugin;
  }

  @Override
  public String getTypeName() {
    return HttpStoragePluginConfig.NAME;
  }

  /**
   * Gets the table that is received from the query. In this case, the table actually are arguments which are passed
   * in the URL string.
   *
   * @param tableName
   *          The "tableName" actually will contain the URL arguments passed to
   *          the record reader
   * @return the selected table
   */
  @Override
  public Table getTable(String tableName) {
    DynamicDrillTable table = activeTables.get(tableName);
    if (table != null) {
      // Return the found table
      return table;
    } else {

      // Register a new table
      return registerTable(tableName, new DynamicDrillTable(plugin, plugin.getName(),
        new HttpScanSpec(plugin.getName(), name, tableName,
              plugin.getConfig().copyForPlan(name), plugin.getTokenTable(), plugin.getRegistry())));
    }
  }

  @Override
  public Set<String> getTableNames() {
    return activeTables.keySet();
  }

  private DynamicDrillTable registerTable(String name, DynamicDrillTable table) {
    activeTables.put(name, table);
    return table;
  }
}
