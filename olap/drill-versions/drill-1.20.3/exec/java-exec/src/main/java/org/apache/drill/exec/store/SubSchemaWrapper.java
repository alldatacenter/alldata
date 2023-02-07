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
package org.apache.drill.exec.store;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.calcite.schema.Function;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.Table;
import org.apache.drill.exec.planner.logical.CreateTableEntry;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;

public class SubSchemaWrapper extends AbstractSchema {

  private final AbstractSchema innerSchema;

  public SubSchemaWrapper(AbstractSchema innerSchema) {
    super(ImmutableList.<String>of(), innerSchema.getFullSchemaName());
    this.innerSchema = innerSchema;
  }

  @Override
  public boolean showInInformationSchema() {
    return false;
  }

  @Override
  public Iterable<String> getSubPartitions(String table,
                                           List<String> partitionColumns,
                                           List<String> partitionValues
  ) throws PartitionNotFoundException {
    Schema defaultSchema = getDefaultSchema();
    if (defaultSchema instanceof AbstractSchema) {
      return ((AbstractSchema) defaultSchema).getSubPartitions(table, partitionColumns, partitionValues);
    } else {
      return Collections.emptyList();
    }
  }

  @Override
  public Schema getDefaultSchema() {
    return innerSchema.getDefaultSchema();
  }

  @Override
  public CreateTableEntry createNewTable(String tableName, List<String> partitionColumns, StorageStrategy storageStrategy) {
    return innerSchema.createNewTable(tableName, partitionColumns, storageStrategy);
  }

  @Override
  public CreateTableEntry createStatsTable(String tableName) {
    return innerSchema.createStatsTable(tableName);
  }

  @Override
  public CreateTableEntry appendToStatsTable(String tableName) {
    return innerSchema.appendToStatsTable(tableName);
  }

  @Override
  public Table getStatsTable(String tableName) {
    return innerSchema.getStatsTable(tableName);
  }

  @Override
  public Collection<Function> getFunctions(String name) {
    return innerSchema.getFunctions(name);
  }

  @Override
  public Set<String> getFunctionNames() {
    return innerSchema.getFunctionNames();
  }

  @Override
  public Schema getSubSchema(String name) {
    return innerSchema.getSubSchema(name);
  }

  @Override
  public Set<String> getSubSchemaNames() {
    return innerSchema.getSubSchemaNames();
  }

  @Override
  public boolean isMutable() {
    return innerSchema.isMutable();
  }

  @Override
  public Table getTable(String name) {
    return innerSchema.getTable(name);
  }

  @Override
  public Set<String> getTableNames() {
    return innerSchema.getTableNames();
  }

  @Override
  public String getTypeName() {
    return innerSchema.getTypeName();
  }
}
