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
package org.apache.drill.exec.store.plan.rel;

import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.schema.TranslatableTable;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.planner.logical.DrillTableSelection;
import org.apache.drill.exec.planner.logical.DynamicDrillTable;
import org.apache.drill.exec.store.StoragePlugin;
import org.apache.drill.exec.util.Utilities;

import java.io.IOException;

/**
 * Table implementation based on {@link DynamicDrillTable} to be used by Drill plugins.
 */
public class PluginDrillTable extends DynamicDrillTable implements TranslatableTable {
  private final Convention convention;

  public PluginDrillTable(StoragePlugin plugin, String storageEngineName,
      String userName, DrillTableSelection selection, Convention convention) {
    super(plugin, storageEngineName, userName, selection);
    this.convention = convention;
  }

  @Override
  public RelNode toRel(RelOptTable.ToRelContext context, RelOptTable table) {
    DrillTable drillTable = Utilities.getDrillTable(table);
    try {
      return new StoragePluginTableScan(context.getCluster(),
          context.getCluster().traitSetOf(convention),
          drillTable.getGroupScan(),
          table,
          table.getRowType());
    } catch (IOException e) {
      throw new DrillRuntimeException(e);
    }
  }
}
