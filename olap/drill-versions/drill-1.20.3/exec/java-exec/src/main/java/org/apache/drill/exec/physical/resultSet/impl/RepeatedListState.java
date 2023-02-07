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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.drill.exec.physical.resultSet.ResultVectorCache;
import org.apache.drill.exec.physical.resultSet.impl.ColumnState.BaseContainerColumnState;
import org.apache.drill.exec.physical.resultSet.impl.SingleVectorState.OffsetVectorState;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.RepeatedListColumnMetadata;
import org.apache.drill.exec.vector.accessor.ArrayWriter;
import org.apache.drill.exec.vector.accessor.impl.HierarchicalFormatter;
import org.apache.drill.exec.vector.accessor.writer.AbstractObjectWriter;
import org.apache.drill.exec.vector.accessor.writer.RepeatedListWriter;
import org.apache.drill.exec.vector.complex.RepeatedListVector;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

/**
 * Represents the internal state of a RepeatedList vector. The repeated list
 * is wrapped in a repeated list "column state" that manages the column as a
 * whole. The repeated list acts as a container which the <tt>RepeatedListState<tt>
 * implements. At the vector level, we track the repeated list vector, but
 * only perform operations on its associated offset vector.
 */
public class RepeatedListState extends ContainerState implements RepeatedListWriter.ArrayListener {

  /**
   * Repeated list column state.
   */
  public static class RepeatedListColumnState extends BaseContainerColumnState {

    private final RepeatedListState listState;

    public RepeatedListColumnState(LoaderInternals loader,
        AbstractObjectWriter writer,
        RepeatedListVectorState vectorState,
        RepeatedListState listState) {
      super(loader, writer, vectorState);
      this.listState = listState;
      listState.bindColumnState(this);
    }

    /**
     * Get the output schema. For a primitive (non-structured) column,
     * the output schema is the same as the internal schema.
     */
    @Override
    public ColumnMetadata outputSchema() { return schema(); }

    @Override
    public ContainerState container() { return listState; }
  }

  /**
   * Track the repeated list vector. The vector state holds onto the repeated
   * list vector, but only performs operations on the actual storage: the
   * offset vector. The child column state manages the repeated list content
   * (which may be complex: another repeated list, a map, a union, etc.)
   */
  public static class RepeatedListVectorState implements VectorState {

    private final ArrayWriter arrayWriter;
    private final RepeatedListVector vector;
    private final OffsetVectorState offsetsState;

    public RepeatedListVectorState(AbstractObjectWriter arrayWriter, RepeatedListVector vector) {
      this.vector = vector;
      this.arrayWriter = arrayWriter.array();
      offsetsState = new OffsetVectorState(
          arrayWriter.events(), vector.getOffsetVector(),
          this.arrayWriter.entryType() == null ? null : arrayWriter.events());
    }

    /**
     * Bind the child writer once the child is created. Note: must pass
     * in the child writer because it is not yet bound to the repeated
     * list vector at the time of this call.
     *
     * @param childWriter child array writer for the inner dimension
     * of the repeated list
     */
    public void updateChildWriter(AbstractObjectWriter childWriter) {
      offsetsState.setChildWriter(childWriter.events());
    }

    @SuppressWarnings("unchecked")
    @Override
    public RepeatedListVector vector() { return vector; }

    @Override
    public int allocate(int cardinality) {
      return offsetsState.allocate(cardinality);
    }

    @Override
    public void rollover(int cardinality) {
      offsetsState.rollover(cardinality);
    }

    @Override
    public void harvestWithLookAhead() {
      offsetsState.harvestWithLookAhead();
    }

    @Override
    public void startBatchWithLookAhead() {
      offsetsState.startBatchWithLookAhead();
    }

    @Override
    public void close() {
      offsetsState.close();
    }

    @Override
    public boolean isProjected() { return true; }

    @Override
    public void dump(HierarchicalFormatter format) {
      format
        .startObject(this)
        .attribute("schema", arrayWriter.schema())
        .attributeIdentity("writer", arrayWriter)
        .attributeIdentity("vector", vector)
        .attribute("offsetsState");
      offsetsState.dump(format);
      format
        .endObject();
    }
  }

  private ColumnState childState;

  public RepeatedListState(LoaderInternals loader,
      ResultVectorCache vectorCache) {
    super(loader, vectorCache, ProjectionFilter.PROJECT_ALL);
  }

  @Override
  public int innerCardinality() {
    return parentColumn.innerCardinality();
  }

  @Override
  protected void addColumn(ColumnState colState) {

    // Remember the one and only child column.
    assert childState == null;
    childState = colState;

    // Add the new child schema to the existing repeated list
    // schema.
    ((RepeatedListColumnMetadata) parentColumn.schema()).childSchema(colState.schema());

    // Add the child vector to the existing repeated list
    // vector.
    final RepeatedListVectorState vectorState = (RepeatedListVectorState) parentColumn.vectorState();
    final RepeatedListVector listVector = vectorState.vector;
    listVector.setChildVector(childState.vector());

    // The repeated list's offset vector state needs to know the offset
    // of the inner vector. Bind that information now that we have
    // an inner writer.
    vectorState.updateChildWriter(childState.writer());
  }

  @Override
  protected Collection<ColumnState> columnStates() {

    // Turn the one and only child into a list of children for
    // the general container mechanism.
    if (childState == null) {
      return new ArrayList<>();
    } else {
      return Lists.newArrayList(childState);
    }
  }

  /**
   * The repeated list vector does not support versioning
   * of maps within the list. (That is, if a new field is
   * added in the overflow row, it will appear in the output
   * of the first batch.) The reasons for not versioning are simple:
   * 1) repeated lists are a very obscure and low-priority area of Drill,
   * and 2) given that background, the additional work of versioning is
   * not worth the effort.
   */
  @Override
  protected boolean isVersioned() { return false; }

  // Callback from the repeated list vector to add the child.
  @Override
  public AbstractObjectWriter setChild(ArrayWriter array,
      ColumnMetadata columnSchema) {

    assert childState == null;
    return addColumn(columnSchema).writer();
  }

  // Callback from the repeated list vector to add the child.
  @Override
  public AbstractObjectWriter setChild(ArrayWriter array,
      MaterializedField field) {
    return setChild(array, MetadataUtils.fromField(field));
  }
}
