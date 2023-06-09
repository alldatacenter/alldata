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

import org.apache.drill.metastore.components.tables.TableMetadataUnit;
import org.apache.drill.metastore.mongo.MongoMetastoreContext;
import org.apache.drill.metastore.mongo.config.MongoConfigConstants;
import org.apache.drill.metastore.mongo.operate.Overwrite;
import org.apache.drill.metastore.mongo.transform.OperationTransformer;
import org.bson.Document;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Metastore Tables component operations transformer that provides mechanism
 * to convert {@link TableMetadataUnit} data to Metastore overwrite / delete operations.
 */
public class TablesOperationTransformer extends OperationTransformer<TableMetadataUnit> {

  public TablesOperationTransformer(MongoMetastoreContext<TableMetadataUnit> context) {
    super(context);
  }

  /**
   * Groups given list of {@link TableMetadataUnit}, convert them to list of overwrite operations
   *
   * @param units Metastore component units
   * @return list of overwrite operations
   */
  @Override
  public List<Overwrite> toOverwrite(List<TableMetadataUnit> units) {
    return context.transformer().inputData()
      .units(units)
      .execute()
      .stream()
      .map(document ->
        new Overwrite(document,
          new Document()
            .append(MongoConfigConstants.ID, document.get(MongoConfigConstants.ID))))
      .collect(Collectors.toList());
  }
}
