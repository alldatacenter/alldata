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
package org.apache.drill.exec.physical.impl.aggregate;

import java.util.Iterator;

import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.record.selection.SelectionVector4;

public class InternalBatch implements Iterable<VectorWrapper<?>>{
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(InternalBatch.class);

  private final VectorContainer container;
  private final FragmentContext context;
  private final BatchSchema schema;
  private final SelectionVector2 sv2;
  private final SelectionVector4 sv4;

  public InternalBatch(RecordBatch incoming, OperatorContext oContext) {
    this(incoming, null, oContext);
  }

  public InternalBatch(RecordBatch incoming, VectorWrapper<?>[] ignoreWrappers, OperatorContext oContext){
    switch(incoming.getSchema().getSelectionVectorMode()){
    case FOUR_BYTE:
      this.sv4 = incoming.getSelectionVector4().createNewWrapperCurrent();
      this.sv2 = null;
      break;
    case TWO_BYTE:
      this.sv4 = null;
      this.sv2 = incoming.getSelectionVector2().clone();
      break;
    default:
      this.sv4 = null;
      this.sv2 = null;
    }
    this.schema = incoming.getSchema();
    this.context = incoming.getContext();
    this.container = VectorContainer.getTransferClone(incoming, ignoreWrappers, oContext);
  }

  public BatchSchema getSchema() {
    return schema;
  }

  public SelectionVector2 getSv2() {
    return sv2;
  }

  public SelectionVector4 getSv4() {
    return sv4;
  }

  @Override
  public Iterator<VectorWrapper<?>> iterator() {
    return container.iterator();
  }

  public void clear() {
    if (sv2 != null) {
      sv2.clear();
    }
    if (sv4 != null) {
      sv4.clear();
    }
    container.clear();
  }

  public VectorWrapper<?> getValueAccessorById(Class<?> clazz, int[] fieldIds) {
    return container.getValueAccessorById(clazz, fieldIds);
  }

  public FragmentContext getContext() {
    return context;
  }
}
