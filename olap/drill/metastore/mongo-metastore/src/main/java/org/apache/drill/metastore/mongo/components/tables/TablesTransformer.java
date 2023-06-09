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
package org.apache.drill.metastore.mongo.components.tables;

import org.apache.drill.metastore.mongo.MongoMetastoreContext;
import org.apache.drill.metastore.mongo.transform.InputDataTransformer;
import org.apache.drill.metastore.mongo.transform.OperationTransformer;
import org.apache.drill.metastore.mongo.transform.OutputDataTransformer;
import org.apache.drill.metastore.mongo.transform.Transformer;
import org.apache.drill.metastore.components.tables.TableMetadataUnit;

/**
 * Metastore Tables component filter, data and operations transformer.
 * Provides needed transformations when reading / writing {@link TableMetadataUnit}
 * from / into Mongo collection.
 */
public class TablesTransformer implements Transformer<TableMetadataUnit> {

  private final MongoMetastoreContext<TableMetadataUnit> context;

  public TablesTransformer(MongoMetastoreContext<TableMetadataUnit> context) {
    this.context = context;
  }

  @Override
  public InputDataTransformer<TableMetadataUnit> inputData() {
    return new InputDataTransformer<>(TableMetadataUnit.SCHEMA.unitGetters());
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
