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
package org.apache.drill.exec.physical.impl.protocol;

import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.record.selection.SelectionVector4;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

/**
 * Extension of the container accessor that holds an optional selection
 * vector, presenting the batch row count as the selection vector
 * count.
 */

public class IndirectContainerAccessor extends VectorContainerAccessor {

  private SelectionVector2 sv2;
  private SelectionVector4 sv4;

  /**
   * Add a record batch, performing schema checks and picking out a
   * selection vector, if provided.
   *
   * @param batch batch of records in record batch format
   */

  public void addBatch(RecordBatch batch) {
    addBatch(batch.getContainer());
    switch (container.getSchema().getSelectionVectorMode()) {
    case TWO_BYTE:
       setSelectionVector(batch.getSelectionVector2());
       break;
    case FOUR_BYTE:
       setSelectionVector(batch.getSelectionVector4());
       break;
     default:
       break;
    }
  }

  public void setSelectionVector(SelectionVector2 sv2) {
    Preconditions.checkState(sv4 == null);
    this.sv2 = sv2;
  }

  public void setSelectionVector(SelectionVector4 sv4) {
    Preconditions.checkState(sv2 == null);
    this.sv4 = sv4;
  }

  @Override
  public SelectionVector2 selectionVector2() {
    return sv2;
  }

  @Override
  public SelectionVector4 selectionVector4() {
    return sv4;
  }

  @Override
  public int rowCount() {
    if (sv2 != null) {
      return sv2.getCount();
    } else if (sv4 != null) {
      return sv4.getCount();
    } else {
      return super.rowCount();
    }
  }

  @Override
  public void release() {
    super.release();
    if (sv2 != null) {
      sv2.clear();
      sv2 = null;
    }
    if (sv4 != null) {
      sv4.clear();
      sv4 = null;
    }
  }
}
