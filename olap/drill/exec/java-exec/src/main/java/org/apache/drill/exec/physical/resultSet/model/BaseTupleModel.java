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
package org.apache.drill.exec.physical.resultSet.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.TupleSchema;

/**
 * Base implementation for a tuple model which is common to the "single"
 * and "hyper" cases. Deals primarily with the structure of the model,
 * which is common between the two physical implementations.
 */
public abstract class BaseTupleModel implements TupleModel {

  public static abstract class BaseColumnModel implements ColumnModel {

    /**
     * Extended schema associated with a column.
     */

    protected final ColumnMetadata schema;

    public BaseColumnModel(ColumnMetadata schema) {
      this.schema = schema;
    }

    @Override
    public ColumnMetadata schema() { return schema; }

    @Override
    public TupleModel mapModel() { return null; }
  }

  /**
   * Columns within the tuple. Columns may, themselves, be represented
   * as tuples.
   */

  protected final List<ColumnModel> columns;

  /**
   * Descriptive schema associated with the columns above. Unlike a
   * {@link org.apache.drill.exec.record.VectorContainer}, this abstraction keeps the schema in sync
   * with vectors as columns are added.
   */
  protected final TupleMetadata schema;

  public BaseTupleModel() {

    // Schema starts empty and is built as columns are added.
    // This ensures that the schema stays in sync with the
    // backing vectors.

    schema = new TupleSchema();
    columns = new ArrayList<>();
  }

  public BaseTupleModel(TupleMetadata schema, List<ColumnModel> columns) {
    this.schema = schema;
    this.columns = columns;
    assert schema.size() == columns.size();
  }

  @Override
  public TupleMetadata schema() { return schema; }

  @Override
  public int size() { return schema.size(); }

  @Override
  public ColumnModel column(int index) {
    return columns.get(index);
  }

  @Override
  public ColumnModel column(String name) {
    return column(schema.index(name));
  }

  /**
   * Perform the work of keeping the list of columns and schema in-sync
   * as columns are added. This is protected because derived classes
   * must add logic to keep the new column in sync with the underlying
   * container or map vector.
   *
   * @param column column implementation to add
   */
  protected void addBaseColumn(BaseColumnModel column) {
    schema.addColumn(column.schema());
    columns.add(column);
    assert columns.size() == schema.size();
  }
}
