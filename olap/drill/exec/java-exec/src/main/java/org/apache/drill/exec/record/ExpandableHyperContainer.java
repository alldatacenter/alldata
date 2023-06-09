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

import org.apache.drill.exec.vector.ValueVector;

public class ExpandableHyperContainer extends VectorContainer {

  public ExpandableHyperContainer() {
    super();
  }

  public ExpandableHyperContainer(VectorAccessible batch) {
    super();
    build(batch);
  }

  private void build(VectorAccessible batch) {
    if (batch.getSchema().getSelectionVectorMode() == BatchSchema.SelectionVectorMode.FOUR_BYTE) {
      for (VectorWrapper<?> w : batch) {
        ValueVector[] hyperVector = w.getValueVectors();
        this.add(hyperVector, true);
      }
    } else {
      for (VectorWrapper<?> w : batch) {
        ValueVector[] hyperVector = { w.getValueVector() };
        this.add(hyperVector, true);
      }
    }

    buildSchema(BatchSchema.SelectionVectorMode.FOUR_BYTE);
  }

  public void addBatch(VectorAccessible batch) {
    if (wrappers.size() == 0) {
      build(batch);
      return;
    }
    if (batch.getSchema().getSelectionVectorMode() == BatchSchema.SelectionVectorMode.FOUR_BYTE) {
      int i = 0;
      for (VectorWrapper<?> w : batch) {
        HyperVectorWrapper<?> hyperVectorWrapper = (HyperVectorWrapper<?>) wrappers.get(i++);
        hyperVectorWrapper.addVectors(w.getValueVectors());
      }
    } else {
      int i = 0;
      for (VectorWrapper<?> w : batch) {
        HyperVectorWrapper<?> hyperVectorWrapper = (HyperVectorWrapper<?>) wrappers.get(i++);
        hyperVectorWrapper.addVector(w.getValueVector());
      }
    }

    buildSchema(BatchSchema.SelectionVectorMode.FOUR_BYTE);
  }
}
