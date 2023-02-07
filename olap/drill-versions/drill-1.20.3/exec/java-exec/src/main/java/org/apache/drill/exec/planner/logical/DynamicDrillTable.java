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
package org.apache.drill.exec.planner.logical;

import org.apache.calcite.schema.Schema;
import org.apache.drill.exec.metastore.MetadataProviderManager;
import org.apache.drill.exec.planner.types.RelDataTypeDrillImpl;
import org.apache.drill.exec.planner.types.RelDataTypeHolder;
import org.apache.drill.exec.store.StoragePlugin;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.drill.exec.util.ImpersonationUtil;

public class DynamicDrillTable extends DrillTable {

  private final RelDataTypeHolder holder;

  public DynamicDrillTable(StoragePlugin plugin, String storageEngineName, String userName, DrillTableSelection selection) {
    this(plugin, storageEngineName, userName, selection, null);
  }

  public DynamicDrillTable(StoragePlugin plugin, String storageEngineName, String userName,
    DrillTableSelection selection, MetadataProviderManager metadataProviderManager) {
    super(storageEngineName, plugin, Schema.TableType.TABLE, userName, selection, metadataProviderManager);
    this.holder = new RelDataTypeHolder();
  }

  /**
   * TODO: Same purpose as other constructor except the impersonation user is
   * the user who is running the Drillbit process. Once we add impersonation to
   * non-FileSystem storage plugins such as Hive, HBase etc, we can remove this
   * constructor.
   */
  public DynamicDrillTable(StoragePlugin plugin, String storageEngineName, DrillTableSelection selection) {
    this(plugin, storageEngineName, ImpersonationUtil.getProcessUserName(), selection, null);
  }

  @Override
  public RelDataType getRowType(RelDataTypeFactory typeFactory) {
    return new RelDataTypeDrillImpl(holder, typeFactory);
  }
}
