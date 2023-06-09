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

import java.util.ArrayList;
import java.util.List;

import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.physical.resultSet.model.hyper.HyperReaderBuilder;
import org.apache.drill.exec.physical.resultSet.model.hyper.HyperSchemaInference;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.ExpandableHyperContainer;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.selection.SelectionVector4;
import org.apache.drill.exec.physical.rowSet.RowSet.HyperRowSet;

/**
 * Implements a row set wrapper around a collection of "hyper vectors."
 * A hyper-vector is a logical vector formed by a series of physical vectors
 * stacked on top of one another. To make a row set, we have a hyper-vector
 * for each column. Another way to visualize this is as a "hyper row set":
 * a stacked collection of single row sets: each column is represented by a
 * vector per row set, with each vector in a row set having the same number
 * of rows. An SV4 then provides a uniform index into the rows in the
 * hyper set. A hyper row set is read-only.
 */

public class HyperRowSetImpl extends AbstractRowSet implements HyperRowSet {

  public static class HyperRowSetBuilderImpl implements HyperRowSetBuilder {

    private final BufferAllocator allocator;
    private final List<VectorContainer> batches = new ArrayList<>();
    private int totalRowCount;

    public HyperRowSetBuilderImpl(BufferAllocator allocator) {
      this.allocator = allocator;
    }

    @Override
    public void addBatch(SingleRowSet rowSet) {
      if (rowSet.rowCount() == 0) {
        return;
      }
      if (rowSet.indirectionType() != SelectionVectorMode.NONE) {
        throw new IllegalArgumentException("Batches must not have a selection vector.");
      }
      batches.add(rowSet.container());
      totalRowCount += rowSet.rowCount();
    }

    @Override
    public void addBatch(VectorContainer container) {
      if (container.getRecordCount() == 0) {
        return;
      }
      if (container.getSchema().getSelectionVectorMode() != SelectionVectorMode.NONE) {
        throw new IllegalArgumentException("Batches must not have a selection vector.");
      }
      batches.add(container);
      totalRowCount += container.getRecordCount();
    }

    @Override
    public HyperRowSet build() throws SchemaChangeException {
      SelectionVector4 sv4 = new SelectionVector4(allocator, totalRowCount);
      ExpandableHyperContainer hyperContainer = new ExpandableHyperContainer();
      for (VectorContainer container : batches) {
        hyperContainer.addBatch(container);
      }
      hyperContainer.setRecordCount(totalRowCount);

      // TODO: This has a bug. If the hyperset has two batches with unions,
      // and the first union contains only VARCHAR, while the second contains
      // only INT, the combined schema should be (VARCHAR, INT). Same is true
      // of lists. But, this code looks at only the first container.
      //
      // This is only a theoretical bug as Drill does not support unions
      // completely, but must be fixed if we want complete union support.
      //
      // Actually, the problem is more fundamental. The extendable hyper
      // container, which creates the metadata schema, does not handle the
      // case either.

      TupleMetadata schema = new HyperSchemaInference().infer(hyperContainer);
      return new HyperRowSetImpl(schema, hyperContainer, sv4);
    }
  }

  /**
   * Selection vector that indexes into the hyper vectors.
   */

  private final SelectionVector4 sv4;

  public HyperRowSetImpl(TupleMetadata schema, VectorContainer container, SelectionVector4 sv4) {
    super(container, schema);
    this.sv4 = sv4;
  }

  public HyperRowSetImpl(VectorContainer container, SelectionVector4 sv4) throws SchemaChangeException {
    this(new HyperSchemaInference().infer(container), container, sv4);
  }

  public static HyperRowSetBuilder builder(BufferAllocator allocator) {
    return new HyperRowSetBuilderImpl(allocator);
  }

  public static HyperRowSet fromContainer(VectorContainer container, SelectionVector4 sv4) {
    try {
      return new HyperRowSetImpl(container, sv4);
    } catch (SchemaChangeException e) {
      throw new UnsupportedOperationException(e);
    }
  }

  public static HyperRowSet fromRowSets(BufferAllocator allocator, SingleRowSet...rowSets) {
    HyperRowSetBuilder builder = builder(allocator);
    for (SingleRowSet rowSet : rowSets) {
      builder.addBatch(rowSet);
    }
    try {
      return builder.build();
    } catch (SchemaChangeException e) {
      throw new IllegalArgumentException("Incompatible schemas", e);
    }
  }

  @Override
  public boolean isExtendable() { return false; }

  @Override
  public boolean isWritable() { return false; }

  @Override
  public RowSetReader reader() {
    return HyperReaderBuilder.build(container(), schema, sv4);
  }

  @Override
  public SelectionVectorMode indirectionType() { return SelectionVectorMode.FOUR_BYTE; }

  @Override
  public SelectionVector4 getSv4() { return sv4; }

  @Override
  public int rowCount() { return sv4.getCount(); }

  @Override
  public void clear() {
    super.clear();
    sv4.clear();
  }
}
