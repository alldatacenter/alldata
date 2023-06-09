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

import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.schema.TranslatableTable;
import org.apache.drill.exec.planner.logical.DrillTableSelection;
import org.apache.drill.exec.planner.logical.DynamicDrillTable;
import org.apache.drill.exec.store.StoragePlugin;

public class EnumMockTable extends DynamicDrillTable implements TranslatableTable {

  public EnumMockTable(StoragePlugin plugin, String storageEngineName, String userName, DrillTableSelection selection) {
    super(plugin, storageEngineName, userName, selection);
  }

  @Override
  public RelNode toRel(RelOptTable.ToRelContext context, RelOptTable relOptTable) {
    EnumMockPlugin plugin = (EnumMockPlugin) getPlugin();
    return new EnumMockRel.EnumMockTableScan(context.getCluster(),
        context.getCluster().traitSetOf(plugin.getConvention()), relOptTable);
  }
}
