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
package org.apache.drill.exec.physical.resultSet.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.resultSet.ResultVectorCache;
import org.apache.drill.exec.physical.resultSet.impl.ColumnState.BaseContainerColumnState;
import org.apache.drill.exec.physical.resultSet.impl.SingleVectorState.FixedWidthVectorState;
import org.apache.drill.exec.physical.resultSet.impl.SingleVectorState.SimpleVectorState;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.VariantMetadata;
import org.apache.drill.exec.record.metadata.VariantSchema;
import org.apache.drill.exec.vector.accessor.ObjectWriter;
import org.apache.drill.exec.vector.accessor.VariantWriter;
import org.apache.drill.exec.vector.accessor.impl.HierarchicalFormatter;
import org.apache.drill.exec.vector.accessor.writer.AbstractObjectWriter;
import org.apache.drill.exec.vector.accessor.writer.UnionVectorShim;
import org.apache.drill.exec.vector.accessor.writer.UnionWriterImpl;
import org.apache.drill.exec.vector.complex.UnionVector;

/**
 * Represents the contents of a union vector (or a pseudo-union for lists).
 * Holds the column states for the "columns" that make up the type members
 * of the union, and implements the writer callbacks to add members to
 * a union. This class is used when a column is a (single, non-repeated)
 * UNION. See the {@link ListState} for when the union is used inside
 * a LIST (repeated union) type.
 */

public class UnionState extends ContainerState
  implements VariantWriter.VariantWriterListener {

  /**
   * Union or list (repeated union) column state.
   */

  public static class UnionColumnState extends BaseContainerColumnState {

    private final ContainerState unionState;

    public UnionColumnState(LoaderInternals loader,
        AbstractObjectWriter writer,
        VectorState vectorState,
        ContainerState unionState) {
      super(loader, writer, vectorState);
      this.unionState = unionState;
      unionState.bindColumnState(this);
    }

    @Override
    public boolean isProjected() {
      // Unions and lists are always projected
      return true;
    }

    /**
     * Get the output schema. For a primitive (non-structured) column,
     * the output schema is the same as the internal schema.
     */
    @Override
    public ColumnMetadata outputSchema() { return schema(); }

    @Override
    public ContainerState container() { return unionState; }
  }

  /**
   * Vector wrapper for a union vector. Union vectors contain a types
   * vector (which indicates the type of each row) along with member
   * vectors for each supported type. This class manages the types
   * vector. The member vectors are managed as children of the
   * {@link UnionState} class. The union vector itself is just a
   * holder; it has no state to be managed.
   */

  public static class UnionVectorState implements VectorState {

    private final UnionVector vector;
    private final SimpleVectorState typesVectorState;

    public UnionVectorState(UnionVector vector, UnionWriterImpl unionWriter) {
      this.vector = vector;
      typesVectorState = new FixedWidthVectorState(
          ((UnionVectorShim) unionWriter.shim()).typeWriter(), vector.getTypeVector());
    }

    @Override
    public int allocate(int cardinality) {
      return typesVectorState.allocate(cardinality);
    }

    @Override
    public void rollover(int cardinality) {
      typesVectorState.rollover(cardinality);
    }

    @Override
    public void harvestWithLookAhead() {
      typesVectorState.harvestWithLookAhead();
    }

    @Override
    public void startBatchWithLookAhead() {
      typesVectorState.startBatchWithLookAhead();
    }

    @Override
    public void close() {
      typesVectorState.close();
    }

    @SuppressWarnings("unchecked")
    @Override
    public UnionVector vector() { return vector; }

    @Override
    public boolean isProjected() { return true; }

    @Override
    public void dump(HierarchicalFormatter format) {
      // TODO Auto-generated method stub
    }
  }

  /**
   * Map of types to member columns, used to track the set of child
   * column states for this union. This map mimics the actual set of
   * vectors in the union,
   * and matches the set of child writers in the union writer.
   */

  private final Map<MinorType, ColumnState> columns = new HashMap<>();

  public UnionState(LoaderInternals events, ResultVectorCache vectorCache) {
    super(events, vectorCache);
  }

  public UnionWriterImpl writer() {
    return (UnionWriterImpl) parentColumn.writer.variant();
  }

  public VariantMetadata variantSchema() {
    return writer().variantSchema();
  }

  public UnionVector vector() {
    return parentColumn.vector();
  }

  @Override
  public ObjectWriter addType(MinorType type) {
    return addMember(VariantSchema.memberMetadata(type));
  }

  @Override
  public ObjectWriter addMember(ColumnMetadata member) {
    if (variantSchema().hasType(member.type())) {
      throw new IllegalArgumentException("Duplicate type: " + member.type().toString());
    }
    return addColumn(member).writer();
  }

  @Override
  protected void addColumn(ColumnState colState) {
    assert ! columns.containsKey(colState.schema().type());
    columns.put(colState.schema().type(), colState);
    vector().addType(colState.vector());
  }

  @Override
  protected Collection<ColumnState> columnStates() {
    return columns.values();
  }

  @Override
  public int innerCardinality() {
    return parentColumn.innerCardinality();
  }

  @Override
  protected boolean isVersioned() { return false; }
}
