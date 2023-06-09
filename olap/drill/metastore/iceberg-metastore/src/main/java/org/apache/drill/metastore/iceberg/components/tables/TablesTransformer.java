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

import org.apache.drill.metastore.components.tables.TableMetadataUnit;
import org.apache.drill.metastore.iceberg.IcebergMetastoreContext;
import org.apache.drill.metastore.iceberg.transform.InputDataTransformer;
import org.apache.drill.metastore.iceberg.transform.OperationTransformer;
import org.apache.drill.metastore.iceberg.transform.OutputDataTransformer;
import org.apache.drill.metastore.iceberg.transform.Transformer;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Table;

/**
 * Metastore Tables component filter, data and operations transformer.
 * Provides needed transformations when reading / writing {@link TableMetadataUnit}
 * from / into Iceberg table.
 */
public class TablesTransformer implements Transformer<TableMetadataUnit> {

  private final IcebergMetastoreContext<TableMetadataUnit> context;

  public TablesTransformer(IcebergMetastoreContext<TableMetadataUnit> context) {
    this.context = context;
  }

  @Override
  public InputDataTransformer<TableMetadataUnit> inputData() {
    Table table = context.table();
    return new InputDataTransformer<>(table.schema(), new Schema(table.spec().partitionType().fields()),
      TableMetadataUnit.SCHEMA.unitGetters());
  }

  @Override
  public OutputDataTransformer<TableMetadataUnit> outputData() {
    return new TablesOutputDataTransformer(TableMetadataUnit.SCHEMA.unitBuilderSetters());
  }

  @Override
  public OperationTransformer<TableMetadataUnit> operation() {
    return new TablesOperationTransformer(context);
  }
}
