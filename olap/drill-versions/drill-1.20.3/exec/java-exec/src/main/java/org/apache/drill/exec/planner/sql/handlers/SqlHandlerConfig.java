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
package org.apache.drill.exec.planner.sql.handlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelShuttleImpl;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.tools.RuleSet;
import org.apache.drill.common.util.function.CheckedSupplier;
import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.planner.PlannerPhase;
import org.apache.drill.exec.planner.common.DrillRelOptUtil;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.planner.sql.SchemaUtilites;
import org.apache.drill.exec.planner.sql.conversion.SqlConverter;
import org.apache.drill.exec.store.StoragePlugin;
import org.apache.drill.exec.store.StoragePluginRegistry;

public class SqlHandlerConfig {

  private final QueryContext context;
  private final SqlConverter converter;

  public SqlHandlerConfig(QueryContext context, SqlConverter converter) {
    this.context = context;
    this.converter = converter;
  }

  public QueryContext getContext() {
    return context;
  }

  public RuleSet getRules(PlannerPhase phase, RelNode input) {
    PluginsCollector pluginsCollector = new PluginsCollector(context.getStorage());
    input.accept(pluginsCollector);

    Collection<StoragePlugin> plugins = pluginsCollector.getPlugins();
    return phase.getRules(context, plugins);
  }

  public SqlConverter getConverter() {
    return converter;
  }

  public static class PluginsCollector extends RelShuttleImpl {
    private final List<StoragePlugin> plugins = new ArrayList<>();
    private final StoragePluginRegistry storagePlugins;

    public PluginsCollector(StoragePluginRegistry storagePlugins) {
      this.storagePlugins = storagePlugins;
    }

    @Override
    public RelNode visit(TableScan scan) {
      String pluginName = SchemaUtilites.getSchemaPathAsList(
        scan.getTable().getQualifiedName().iterator().next()).iterator().next();
      CheckedSupplier<StoragePlugin, StoragePluginRegistry.PluginException> pluginsProvider =
        () -> storagePlugins.getPlugin(pluginName);

      StoragePlugin storagePlugin = Optional.ofNullable(DrillRelOptUtil.getDrillTable(scan))
        .map(DrillTable::getPlugin)
        .orElseGet(pluginsProvider);
      plugins.add(storagePlugin);
      return scan;
    }

    public List<StoragePlugin> getPlugins() {
      return plugins;
    }
  }
}
