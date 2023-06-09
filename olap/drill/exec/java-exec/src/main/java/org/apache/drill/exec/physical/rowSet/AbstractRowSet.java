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
package org.apache.drill.exec.physical.rowSet;

import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.TupleMetadata;

/**
 * Basic implementation of a row set for both the single and multiple
 * (hyper) varieties, both the fixed and extensible varieties.
 */
public abstract class AbstractRowSet implements RowSet {

  protected VectorContainer container;
  protected TupleMetadata schema;

  public AbstractRowSet(VectorContainer container, TupleMetadata schema) {
    this.container = container;
    this.schema = schema;
  }

  @Override
  public VectorAccessible vectorAccessible() { return container(); }

  @Override
  public VectorContainer container() { return container; }

  @Override
  public int rowCount() { return container().getRecordCount(); }

  @Override
  public void clear() {
    VectorContainer container = container();
    container.zeroVectors();
    container.setRecordCount(0);
  }

  @Override
  public TupleMetadata schema() { return schema; }

  @Override
  public BufferAllocator allocator() { return container.getAllocator(); }

  @Override
  public String toString() {
    return RowSetFormatter.toString(this);
  }

  @Override
  public void print() {
    RowSetFormatter.print(this);
  }

  @Override
  public long size() {
    throw new UnsupportedOperationException("Current row set implementation does not support providing size information");
  }

  @Override
  public BatchSchema batchSchema() { return container().getSchema(); }
}
