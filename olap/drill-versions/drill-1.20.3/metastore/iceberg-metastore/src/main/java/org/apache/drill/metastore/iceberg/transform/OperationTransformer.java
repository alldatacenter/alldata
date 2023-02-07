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
package org.apache.drill.metastore.iceberg.transform;

import org.apache.drill.metastore.expressions.FilterExpression;
import org.apache.drill.metastore.iceberg.IcebergMetastoreContext;
import org.apache.drill.metastore.iceberg.operate.Delete;
import org.apache.drill.metastore.iceberg.operate.Overwrite;
import org.apache.drill.metastore.iceberg.write.File;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DataFiles;
import org.apache.iceberg.expressions.Expression;

import java.util.List;
import java.util.UUID;

/**
 * Base class to transforms given input into
 * {@link org.apache.drill.metastore.iceberg.operate.IcebergOperation} implementations.
 *
 * @param <T> Metastore component unit type
 */
public abstract class OperationTransformer<T> {

  protected final IcebergMetastoreContext<T> context;

  protected OperationTransformer(IcebergMetastoreContext<T> context) {
    this.context = context;
  }

  public Overwrite toOverwrite(String location, Expression expression, List<T> units) {
    WriteData writeData = context.transformer().inputData()
      .units(units)
      .execute();

    File file = context.fileWriter()
      .records(writeData.records())
      .location(location)
      .name(UUID.randomUUID().toString())
      .write();

    DataFile dataFile = DataFiles.builder(context.table().spec())
      .withInputFile(file.input())
      .withMetrics(file.metrics())
      .withPartition(writeData.partition())
      .build();

    return new Overwrite(dataFile, expression);
  }

  public Delete toDelete(FilterExpression filter) {
    return new Delete(context.transformer().filter().transform(filter));
  }

  public Delete toDelete(org.apache.drill.metastore.operate.Delete delete) {
    // metadata types are ignored during delete since they are not part of the partition key
    FilterTransformer filterTransformer = context.transformer().filter();
    return new Delete(filterTransformer.transform(delete.filter()));
  }

  /**
   * Converts given list of Metastore components units into list of overwrite operations.
   * Specific for each Metastore component.
   *
   * @param units Metastore component units
   * @return list of overwrite operations
   */
  public abstract List<Overwrite> toOverwrite(List<T> units);
}
