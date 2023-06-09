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

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.Schema.TableType;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.util.ImpersonationUtil;

public class InfoSchemaDrillTable extends DrillTable {

  private final InfoSchemaTableType table;

  public InfoSchemaDrillTable(InfoSchemaStoragePlugin plugin, String storageEngineName, InfoSchemaTableType selection, StoragePluginConfig storageEngineConfig) {
    super(storageEngineName, plugin, TableType.SYSTEM_TABLE, ImpersonationUtil.getProcessUserName(), selection);
    this.table = selection;
  }

  @Override
  public RelDataType getRowType(RelDataTypeFactory typeFactory) {
    return table.getRowType(typeFactory);
  }
}
