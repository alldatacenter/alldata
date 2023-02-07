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

import org.apache.drill.exec.physical.impl.protocol.BatchAccessor;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.VectorContainer;

public class RowSets {

  public static RowSet wrap(RecordBatch batch) {
    VectorContainer container = batch.getContainer();
    switch (container.getSchema().getSelectionVectorMode()) {
    case FOUR_BYTE:
      return HyperRowSetImpl.fromContainer(container, batch.getSelectionVector4());
    case NONE:
      return DirectRowSet.fromContainer(container);
    case TWO_BYTE:
      return IndirectRowSet.fromSv2(container, batch.getSelectionVector2());
    default:
      throw new IllegalStateException("Invalid selection mode");
    }
  }

  public static RowSet wrap(VectorContainer container) {
    switch (container.getSchema().getSelectionVectorMode()) {
    case FOUR_BYTE:
      throw new IllegalArgumentException("Build from a batch for SV4");
    case NONE:
      return DirectRowSet.fromContainer(container);
    case TWO_BYTE:
      throw new IllegalArgumentException("Build from a batch for SV2");
    default:
      throw new IllegalStateException("Invalid selection mode");
    }
  }

  public static RowSet wrap(BatchAccessor batch) {
    VectorContainer container = batch.container();
    switch (container.getSchema().getSelectionVectorMode()) {
    case FOUR_BYTE:
      return HyperRowSetImpl.fromContainer(container, batch.selectionVector4());
    case NONE:
      return DirectRowSet.fromContainer(container);
    case TWO_BYTE:
      return IndirectRowSet.fromSv2(container, batch.selectionVector2());
    default:
      throw new IllegalStateException("Invalid selection mode");
    }
  }
}
