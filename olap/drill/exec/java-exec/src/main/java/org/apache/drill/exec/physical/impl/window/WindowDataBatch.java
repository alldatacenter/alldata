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
package org.apache.drill.exec.physical.impl.window;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.record.TypedFieldId;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.record.selection.SelectionVector4;
import org.apache.drill.exec.vector.ValueVector;

import java.util.Iterator;
import java.util.List;

public class WindowDataBatch implements VectorAccessible {

  private final OperatorContext oContext;
  private final VectorContainer container;
  private final int recordCount;

  public WindowDataBatch(final VectorAccessible batch, final OperatorContext oContext) {
    this.oContext = oContext;
    recordCount = batch.getRecordCount();

    List<ValueVector> vectors = Lists.newArrayList();

    for (VectorWrapper<?> v : batch) {
      if (v.isHyper()) {
        throw new UnsupportedOperationException("Record batch data can't be created based on a hyper batch.");
      }
      TransferPair tp = v.getValueVector().getTransferPair(oContext.getAllocator());
      tp.transfer();
      vectors.add(tp.getTo());
    }

    container = new VectorContainer(oContext);
    container.addCollection(vectors);
    container.setRecordCount(recordCount);
    container.buildSchema(batch.getSchema().getSelectionVectorMode());
  }

  public OperatorContext getContext() {
    return oContext;
  }

  public VectorContainer getContainer() {
    return container;
  }

  @Override
  public int getRecordCount() {
    return recordCount;
  }

  @Override
  public VectorWrapper<?> getValueAccessorById(Class<?> clazz, int... fieldIds) {
    return container.getValueAccessorById(clazz, fieldIds);
  }

  @Override
  public TypedFieldId getValueVectorId(SchemaPath path) {
    return container.getValueVectorId(path);
  }

  @Override
  public BatchSchema getSchema() {
    return container.getSchema();
  }

  @Override
  public Iterator<VectorWrapper<?>> iterator() {
    return container.iterator();
  }

  @Override
  public SelectionVector2 getSelectionVector2() {
    throw new UnsupportedOperationException();
  }

  @Override
  public SelectionVector4 getSelectionVector4() {
    throw new UnsupportedOperationException();
  }

  public void clear() {
    container.clear();
  }

  @Override
  public String toString() {
    return "WindowDataBatch[container=" + container + ", recordCount=" + recordCount + "]";
  }
}
