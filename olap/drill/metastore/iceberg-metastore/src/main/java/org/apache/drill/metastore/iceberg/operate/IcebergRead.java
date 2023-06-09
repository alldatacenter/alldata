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
package org.apache.drill.metastore.iceberg.operate;

import org.apache.drill.metastore.MetastoreColumn;
import org.apache.drill.metastore.iceberg.IcebergMetastoreContext;
import org.apache.drill.metastore.iceberg.transform.FilterTransformer;
import org.apache.drill.metastore.operate.AbstractRead;
import org.apache.drill.metastore.operate.MetadataTypeValidator;
import org.apache.drill.metastore.operate.Read;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.iceberg.data.IcebergGenerics;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.expressions.Expression;
import org.apache.iceberg.types.Types;

import java.util.List;

/**
 * Implementation of {@link Read} interface based on {@link AbstractRead} parent class.
 * Reads information from Iceberg table based on given filter expression.
 * Supports reading information for specific columns.
 *
 * @param <T> Metastore component unit type
 */
public class IcebergRead<T> extends AbstractRead<T> {

  private final IcebergMetastoreContext<T> context;
  private final String[] defaultColumns;

  public IcebergRead(MetadataTypeValidator metadataTypeValidator, IcebergMetastoreContext<T> context) {
    super(metadataTypeValidator);
    this.context = context;
    this.defaultColumns = context.table().schema().columns().stream()
      .map(Types.NestedField::name)
      .toArray(String[]::new);
  }

  @Override
  protected List<T> internalExecute() {
    String[] selectedColumns = columns.isEmpty()
      ? defaultColumns
      : columns.stream()
         .map(MetastoreColumn::columnName)
         .toArray(String[]::new);

    FilterTransformer filterTransformer = context.transformer().filter();
    Expression rowFilter = filterTransformer.combine(
      filterTransformer.transform(metadataTypes), filterTransformer.transform(filter));

    Iterable<Record> records = IcebergGenerics.read(context.table())
      .select(selectedColumns)
      .where(rowFilter)
      .build();

    return context.transformer().outputData()
      .columns(selectedColumns)
      .records(Lists.newArrayList(records))
      .execute();
  }
}
