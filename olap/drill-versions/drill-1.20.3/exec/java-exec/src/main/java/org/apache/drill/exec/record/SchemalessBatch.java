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
package org.apache.drill.exec.record;


import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.record.selection.SelectionVector4;

import java.util.Iterator;

/**
 * Empty batch without schema and data.
 */
public class SchemalessBatch implements CloseableRecordBatch {

  public SchemalessBatch() { }

  @Override
  public FragmentContext getContext() {
    return null;
  }

  @Override
  public BatchSchema getSchema() {
    return null;
  }

  @Override
  public int getRecordCount() {
    return 0;
  }

  @Override
  public SelectionVector2 getSelectionVector2() {
    throw new UnsupportedOperationException(String.format("You should not call getSelectionVector2() for class %s",
        this.getClass().getCanonicalName()));
  }

  @Override
  public SelectionVector4 getSelectionVector4() {
    throw new UnsupportedOperationException(String.format("You should not call getSelectionVector4() for class %s",
        this.getClass().getCanonicalName()));
  }

  @Override
  public void cancel() { }

  @Override
  public VectorContainer getOutgoingContainer() {
    throw new UnsupportedOperationException(String.format("You should not call getOutgoingContainer() for class %s",
        this.getClass().getCanonicalName()));
  }

  @Override
  public TypedFieldId getValueVectorId(SchemaPath path) {
    throw new UnsupportedOperationException(String.format("You should not call getValueVectorId() for class %s",
        this.getClass().getCanonicalName()));
  }

  @Override
  public VectorWrapper<?> getValueAccessorById(Class<?> clazz, int... ids) {
    throw new UnsupportedOperationException(String.format("You should not call getValueAccessorById() for class %s",
        this.getClass().getCanonicalName()));
  }

  @Override
  public IterOutcome next() {
    return IterOutcome.NONE;
  }

  @Override
  public WritableBatch getWritableBatch() {
    throw new UnsupportedOperationException(String.format("You should not call getWritableBatch() for class %s",
        this.getClass().getCanonicalName()));
  }

  @Override
  public Iterator<VectorWrapper<?>> iterator() {
    return null;
  }

  @Override
  public void close() throws Exception {
    // This is present to match BatchCreator#getBatch() returning type.
  }

  @Override
  public VectorContainer getContainer() { return null; }

  @Override
  public void dump() { }
}
