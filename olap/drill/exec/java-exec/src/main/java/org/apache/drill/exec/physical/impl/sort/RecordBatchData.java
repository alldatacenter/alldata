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
package org.apache.drill.exec.physical.impl.sort;

import java.util.List;

import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.vector.ValueVector;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

/**
 * Holds the data for a particular record batch for later manipulation.
 */
public class RecordBatchData {

  private SelectionVector2 sv2;
  private int recordCount;
  VectorContainer container = new VectorContainer();

  public RecordBatchData(VectorAccessible batch, BufferAllocator allocator) {
    List<ValueVector> vectors = Lists.newArrayList();
    recordCount = batch.getRecordCount();

    if (batch instanceof RecordBatch && batch.getSchema().getSelectionVectorMode() == SelectionVectorMode.TWO_BYTE) {
      this.sv2 = ((RecordBatch)batch).getSelectionVector2().clone();
    } else {
      this.sv2 = null;
    }

    for (VectorWrapper<?> v : batch) {
      if (v.isHyper()) {
        throw new UnsupportedOperationException("Record batch data can't be created based on a hyper batch.");
      }
      TransferPair tp = v.getValueVector().getTransferPair(allocator);
      // Transfer make sure of releasing memory for value vector in source container.
      tp.transfer();
      vectors.add(tp.getTo());
    }

    container.addCollection(vectors);
    container.setRecordCount(recordCount);
    container.buildSchema(batch.getSchema().getSelectionVectorMode());
  }

  public int getRecordCount() {
    return recordCount;
  }

  public List<ValueVector> getVectors() {
    List<ValueVector> vectors = Lists.newArrayList();
    for (VectorWrapper<?> w : container) {
      vectors.add(w.getValueVector());
    }
    return vectors;
  }

  public void setSv2(SelectionVector2 sv2) {
    this.sv2 = sv2;
    this.recordCount = sv2.getCount();
    this.container.buildSchema(SelectionVectorMode.TWO_BYTE);
  }

  public SelectionVector2 getSv2() {
    return sv2;
  }

  public VectorContainer getContainer() {
    return container;
  }

  public void clear() {
    if (sv2 != null) {
      sv2.clear();
    }
    container.clear();
  }
}
