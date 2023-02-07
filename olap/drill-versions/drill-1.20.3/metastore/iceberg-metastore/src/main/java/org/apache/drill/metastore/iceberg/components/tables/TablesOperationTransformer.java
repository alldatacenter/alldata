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
package org.apache.drill.metastore.iceberg.components.tables;

import org.apache.drill.metastore.MetastoreColumn;
import org.apache.drill.metastore.components.tables.TableMetadataUnit;
import org.apache.drill.metastore.iceberg.IcebergMetastoreContext;
import org.apache.drill.metastore.iceberg.operate.Overwrite;
import org.apache.drill.metastore.iceberg.transform.OperationTransformer;
import org.apache.iceberg.expressions.Expression;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Metastore Tables component operations transformer that provides mechanism
 * to convert {@link TableMetadataUnit} data to Metastore overwrite / delete operations.
 */
public class TablesOperationTransformer extends OperationTransformer<TableMetadataUnit> {

  public TablesOperationTransformer(IcebergMetastoreContext<TableMetadataUnit> context) {
    super(context);
  }

  /**
   * Groups given list of {@link TableMetadataUnit} based on table key
   * (storage plugin, workspace and table name), each table key is grouped by metadata key.
   * Each group is converted into overwrite operation.
   *
   * @param units Metastore component units
   * @return list of overwrite operations
   */
  public List<Overwrite> toOverwrite(List<TableMetadataUnit> units) {
    Map<TableKey, Map<String, List<TableMetadataUnit>>> data = units.stream()
      .collect(
        Collectors.groupingBy(TableKey::of,
          Collectors.groupingBy(TableMetadataUnit::metadataKey)
        ));

    return data.entrySet().parallelStream()
      .map(dataEntry -> dataEntry.getValue().entrySet().parallelStream()
        .map(operationEntry -> {
          TableKey tableKey = dataEntry.getKey();

          String location = tableKey.toLocation(context.table().location());

          Map<MetastoreColumn, Object> filterConditions = new HashMap<>(tableKey.toFilterConditions());
          filterConditions.put(MetastoreColumn.METADATA_KEY, operationEntry.getKey());
          Expression expression = context.transformer().filter().transform(filterConditions);

          return toOverwrite(location, expression, operationEntry.getValue());
        })
        .collect(Collectors.toList()))
      .flatMap(Collection::stream)
      .collect(Collectors.toList());
  }
}
