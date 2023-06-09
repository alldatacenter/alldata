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
package org.apache.drill.exec.physical.base;

import java.util.List;

import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.calcite.rel.RelNode;
import org.apache.drill.exec.planner.index.IndexCollection;
import org.apache.drill.exec.planner.cost.PluginCost;
import org.apache.drill.exec.planner.physical.PartitionFunction;
import org.apache.drill.exec.store.AbstractStoragePlugin;

public abstract class AbstractDbGroupScan extends AbstractGroupScan implements DbGroupScan {

  private static final String ROW_KEY = "_id";
  private static final SchemaPath ROW_KEY_PATH = SchemaPath.getSimplePath(ROW_KEY);

  public AbstractDbGroupScan(String userName) {
    super(userName);
  }

  public AbstractDbGroupScan(AbstractDbGroupScan that) {
    super(that);
  }

  public abstract AbstractStoragePlugin getStoragePlugin();

  public abstract StoragePluginConfig getStorageConfig();

  @Override
  public boolean supportsSecondaryIndex() {
    return false;
  }

  @Override
  public IndexCollection getSecondaryIndexCollection(RelNode scanrel) {
    return null;
  }

  @Override
  public boolean supportsRestrictedScan() {
    return false;
  }

  @Override
  public boolean isRestrictedScan() {
    return false;
  }

  @Override
  public DbGroupScan getRestrictedScan(List<SchemaPath> columns) {
    return null;
  }

  @Override
  public String getRowKeyName() {
    return ROW_KEY;
  }

  @Override
  public SchemaPath getRowKeyPath() {
    return ROW_KEY_PATH;
  }

  @Override
  public PartitionFunction getRangePartitionFunction(List<FieldReference> refList) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PluginCost getPluginCostModel() {
    return null;
  }
}
