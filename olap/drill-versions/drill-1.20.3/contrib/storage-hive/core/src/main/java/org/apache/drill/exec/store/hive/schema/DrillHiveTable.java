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
package org.apache.drill.exec.store.hive.schema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.planner.types.HiveToRelDataTypeConverter;
import org.apache.drill.exec.store.hive.HiveReadEntry;
import org.apache.drill.exec.store.hive.HiveStoragePlugin;
import org.apache.drill.exec.store.hive.HiveTableWithColumnCache;

public class DrillHiveTable extends DrillTable {

  protected final HiveTableWithColumnCache hiveTable;

  public DrillHiveTable(String storageEngineName, HiveStoragePlugin plugin, String userName, HiveReadEntry readEntry) {
    super(storageEngineName, plugin, userName, readEntry);
    this.hiveTable = new HiveTableWithColumnCache(readEntry.getTable());
  }

  @Override
  public RelDataType getRowType(RelDataTypeFactory typeFactory) {
    HiveToRelDataTypeConverter dataTypeConverter = new HiveToRelDataTypeConverter(typeFactory);
    final List<String> fieldNames = new ArrayList<>();
    final List<RelDataType> fieldTypes = Stream.of(hiveTable.getColumnListsCache().getTableSchemaColumns(), hiveTable.getPartitionKeys())
            .flatMap(Collection::stream)
            .peek(hiveField -> fieldNames.add(hiveField.getName()))
            .map(dataTypeConverter::convertToNullableRelDataType)
            .collect(Collectors.toList());
    return typeFactory.createStructType(fieldTypes, fieldNames);
  }

}
