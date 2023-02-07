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

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.record.selection.SelectionVector4;
import org.apache.drill.exec.vector.AllocationHelper;
import org.apache.drill.exec.vector.SchemaChangeCallBack;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;

public class VectorContainer implements VectorAccessible {

  private final BufferAllocator allocator;
  protected final List<VectorWrapper<?>> wrappers = Lists.newArrayList();
  private BatchSchema schema;
  private int recordCount;
  private boolean initialized;
  // private BufferAllocator allocator;
  // Schema has changed since last built. Must rebuild schema
  private boolean schemaChanged = true;

  public VectorContainer() {
    allocator = null;
  }

  public VectorContainer(OperatorContext oContext) {
    this(oContext.getAllocator());
  }

  public VectorContainer(BufferAllocator allocator) {
    this.allocator = allocator;
  }

  /**
   * Create a new vector container given a pre-defined schema. Creates the
   * corresponding vectors, but does not allocate memory for them. Call
   * {@link #allocateNew()} or {@link #allocateNewSafe()} to allocate
   * memory.
   * <p>
   * Note that this method does the equivalent of {@link #buildSchema(SelectionVectorMode)}
   * using the schema provided.
   *
   * @param allocator allocator to be used to allocate memory later
   * @param schema the schema that defines the vectors to create
   */

  public VectorContainer(BufferAllocator allocator, BatchSchema schema) {
    this.allocator = allocator;
    for (MaterializedField field : schema) {
      addOrGet(field, null);
    }
    this.schema = schema;
    schemaChanged = false;
  }

  @Override
  public String toString() {
    return super.toString()
        + "[recordCount = " + recordCount
        + ", schemaChanged = " + schemaChanged
        + ", schema = " + schema
        + ", wrappers = " + wrappers
        + ", ...]";
  }

  public BufferAllocator getAllocator() { return allocator; }

  public boolean isSchemaChanged() { return schemaChanged; }

  /**
   * Indicate the schema changed. Normally set by mutating this container.
   * If schemas are built externally, call this if the schema contained
   * here is different than the one provided in a previous batch. (Some
   * operators don't trust OK_NEW_SCHEMA, and use the schema changed
   * flag for the "real" truth.
   */

  public void schemaChanged() {
    schemaChanged = true;
  }

  public void addHyperList(List<ValueVector> vectors) {
    addHyperList(vectors, true);
  }

  public void addHyperList(List<ValueVector> vectors, boolean releasable) {
    schema = null;
    ValueVector[] vv = new ValueVector[vectors.size()];
    for (int i = 0; i < vv.length; i++) {
      vv[i] = vectors.get(i);
    }
    add(vv, releasable);
  }

  /**
   * Transfer vectors from containerIn to this.
   */
  public void transferIn(VectorContainer containerIn) {
    rawTransferIn(containerIn);
    setRecordCount(containerIn.getRecordCount());
  }

  @VisibleForTesting
  public void rawTransferIn(VectorContainer containerIn) {
    Preconditions.checkArgument(wrappers.size() == containerIn.wrappers.size());
    for (int i = 0; i < wrappers.size(); ++i) {
      containerIn.wrappers.get(i).transfer(wrappers.get(i));
    }
  }

  /**
   * Transfer vectors from this to containerOut
   */
  public void transferOut(VectorContainer containerOut) {
    Preconditions.checkArgument(this.wrappers.size() == containerOut.wrappers.size());
    for (int i = 0; i < this.wrappers.size(); ++i) {
      this.wrappers.get(i).transfer(containerOut.wrappers.get(i));
    }
  }

  public <T extends ValueVector> T addOrGet(MaterializedField field) {
    return addOrGet(field, null);
  }

  /**
   * This method should be called with MaterializedField which also has correct children field list specially when
   * the field type is MAP. Otherwise after calling this method if caller is not creating TransferPair on the
   * ValueVector, then the new ValueVector will not have information about it's list of children MaterializedField.
   * @param field
   * @param callBack
   * @param <T>
   * @return
   */
  @SuppressWarnings("unchecked")
  public <T extends ValueVector> T addOrGet(final MaterializedField field, final SchemaChangeCallBack callBack) {
    final TypedFieldId id = getValueVectorId(SchemaPath.getSimplePath(field.getName()));
    final ValueVector vector;
    if (id != null) {
      vector = getValueAccessorById(id.getFieldIds()).getValueVector();
      if (id.getFieldIds().length == 1 && !vector.getField().getType().equals(field.getType())) {
        final ValueVector newVector = TypeHelper.getNewVector(field, this.getAllocator(), callBack);
        replace(vector, newVector);
        return (T) newVector;
      }
    } else {
      vector = TypeHelper.getNewVector(field, this.getAllocator(), callBack);
      add(vector);
    }
    return (T) vector;
  }

  public <T extends ValueVector> T addOrGet(String name, MajorType type, Class<T> clazz) {
    MaterializedField field = MaterializedField.create(name, type);
    return addOrGet(field);
  }

  /**
   * Get a set of transferred clones of this container. Note that this guarantees that the vectors in the cloned
   * container have the same TypedFieldIds as the existing container, allowing interchangeability in generated code. In
   * the case of hyper vectors, this container actually doesn't do a full transfer, rather creating a clone vector
   * wrapper only.
   *
   * @param incoming
   *          The RecordBatch iterator the contains the batch we should take over.
   * @return A cloned vector container.
   */
  public static VectorContainer getTransferClone(VectorAccessible incoming, OperatorContext oContext) {
    VectorContainer vc = new VectorContainer(oContext);
    for (VectorWrapper<?> w : incoming) {
      vc.cloneAndTransfer(w);
    }
    return vc;
  }

  public static VectorContainer getTransferClone(VectorAccessible incoming, BufferAllocator allocator) {
    VectorContainer vc = new VectorContainer(allocator);
    for (VectorWrapper<?> w : incoming) {
      vc.cloneAndTransfer(w);
    }
    return vc;
  }

  public static VectorContainer getTransferClone(VectorAccessible incoming, VectorWrapper<?>[] ignoreWrappers, OperatorContext oContext) {
    Iterable<VectorWrapper<?>> wrappers = incoming;
    if (ignoreWrappers != null) {
      final List<VectorWrapper<?>> ignored = Lists.newArrayList(ignoreWrappers);
      final Set<VectorWrapper<?>> resultant = Sets.newLinkedHashSet(incoming);
      resultant.removeAll(ignored);
      wrappers = resultant;
    }

    final VectorContainer vc = new VectorContainer(oContext);
    for (VectorWrapper<?> w : wrappers) {
      vc.cloneAndTransfer(w);
    }

    return vc;
  }

  private void cloneAndTransfer(VectorWrapper<?> wrapper) {
    wrappers.add(wrapper.cloneAndTransfer(getAllocator()));
  }

  public void addCollection(Iterable<ValueVector> vectors) {
    schema = null;
    for (ValueVector vv : vectors) {
      wrappers.add(SimpleVectorWrapper.create(vv));
    }
  }

  /**
    * This works with non-hyper {@link VectorContainer}s which have no selection vectors.
    * Appends a row taken from a source {@link VectorContainer} to this {@link VectorContainer}.
    * @param srcContainer The {@link VectorContainer} to copy a row from.
    * @param srcIndex The index of the row to copy from the source {@link VectorContainer}.
    * @return Position one above where the row was appended
    */
  public int appendRow(VectorContainer srcContainer, int srcIndex) {
    for (int vectorIndex = 0; vectorIndex < wrappers.size(); vectorIndex++) {
      ValueVector destVector = wrappers.get(vectorIndex).getValueVector();
      ValueVector srcVector = srcContainer.wrappers.get(vectorIndex).getValueVector();
      destVector.copyEntry(recordCount, srcVector, srcIndex);
    }
    return incRecordCount();
  }

  public TypedFieldId add(ValueVector vv) {
    schemaChanged();
    schema = null;
    int i = wrappers.size();
    wrappers.add(SimpleVectorWrapper.create(vv));
    return new TypedFieldId.Builder().finalType(vv.getField().getType())
        .addId(i)
        .build();
  }

  public ValueVector getLast() {
    int sz = wrappers.size();
    if ( sz == 0 ) { return null; }
    return wrappers.get(sz - 1).getValueVector();
  }
  public void add(ValueVector[] hyperVector) {
    add(hyperVector, true);
  }

  public void add(ValueVector[] hyperVector, boolean releasable) {
    assert hyperVector.length != 0;
    schemaChanged();
    schema = null;
    Class<?> clazz = hyperVector[0].getClass();
    ValueVector[] c = (ValueVector[]) Array.newInstance(clazz, hyperVector.length);
    System.arraycopy(hyperVector, 0, c, 0, hyperVector.length);
    // todo: work with a merged schema.
    wrappers.add(HyperVectorWrapper.create(hyperVector[0].getField(), c, releasable));
  }

  public void remove(ValueVector v) {
    schema = null;
    schemaChanged();
    for (Iterator<VectorWrapper<?>> iter = wrappers.iterator(); iter.hasNext();) {
      VectorWrapper<?> w = iter.next();
      if (!w.isHyper() && v == w.getValueVector()) {
        w.clear();
        iter.remove();
        return;
      }
    }
    throw new IllegalStateException("You attempted to remove a vector that didn't exist.");
  }

  private void replace(ValueVector old, ValueVector newVector) {
    schema = null;
    schemaChanged();
    int i = 0;
    for (VectorWrapper<?> w : wrappers){
      if (!w.isHyper() && old == w.getValueVector()) {
        w.clear();
        wrappers.set(i, new SimpleVectorWrapper<>(newVector));
        return;
      }
      i++;
    }
    throw new IllegalStateException("You attempted to remove a vector that didn't exist.");
  }

  @Override
  public TypedFieldId getValueVectorId(SchemaPath path) {
    for (int i = 0; i < wrappers.size(); i++) {
      VectorWrapper<?> va = wrappers.get(i);
      TypedFieldId id = va.getFieldIdIfMatches(i, path);
      if (id != null) {
        return id;
      }
    }

    return null;
  }

  public VectorWrapper<?> getValueVector(int index) {
    return wrappers.get(index);
  }

  @Override
  public VectorWrapper<?> getValueAccessorById(Class<?> clazz, int... fieldIds) {
    Preconditions.checkArgument(fieldIds.length >= 1);
    VectorWrapper<?> va = wrappers.get(fieldIds[0]);

    if (va == null) {
      return null;
    }

    if (fieldIds.length == 1 && clazz != null && !clazz.isAssignableFrom(va.getVectorClass())) {
      throw new IllegalStateException(String.format(
          "Failure while reading vector.  Expected vector class of %s but was holding vector class %s, field= %s ",
          clazz.getCanonicalName(), va.getVectorClass().getCanonicalName(), va.getField()));
    }

    return va.getChildWrapper(fieldIds);
  }

  private VectorWrapper<?> getValueAccessorById(int... fieldIds) {
    Preconditions.checkArgument(fieldIds.length >= 1);
    VectorWrapper<?> va = wrappers.get(fieldIds[0]);

    if (va == null) {
      return null;
    }
    return va.getChildWrapper(fieldIds);
  }

  public boolean hasSchema() {
    return schema != null;
  }

  @Override
  public BatchSchema getSchema() {
    Preconditions
        .checkNotNull(schema,
            "Schema is currently null.  You must call buildSchema(SelectionVectorMode) before this container can return a schema.");
    return schema;
  }

  public void buildSchema(SelectionVectorMode mode) {
    SchemaBuilder bldr = BatchSchema.newBuilder().setSelectionVectorMode(mode);
    for (VectorWrapper<?> v : wrappers) {
      bldr.addField(v.getField());
    }
    schema = bldr.build();
    schemaChanged = false;
  }

  @Override
  public Iterator<VectorWrapper<?>> iterator() {
    return wrappers.iterator();
  }

  public void clear() {
    zeroVectors();
    removeAll();
  }

  public void removeAll() {
    wrappers.clear();
    schema = null;
  }

  public void setRecordCount(int recordCount) {
    this.recordCount = recordCount;
    this.initialized = true;
  }

  /**
   * Increment the record count
   * @return the new record count
   */
  public int incRecordCount() {
    initialized = true;
    return ++recordCount;
  }

  @Override
  public int getRecordCount() {
    Preconditions.checkState(hasRecordCount(), "Record count not set for this vector container");
    return recordCount;
  }

  public boolean hasRecordCount() { return initialized; }

  @Override
  public SelectionVector2 getSelectionVector2() {
    throw new UnsupportedOperationException();
  }

  @Override
  public SelectionVector4 getSelectionVector4() {
    throw new UnsupportedOperationException();
  }

  /**
   * Clears the contained vectors.  (See {@link ValueVector#clear}).
   * Note that the name <tt>zeroVector()</tt> in a value vector is
   * used for the action to set all vectors to zero. Here it means
   * to free the vector's memory. Sigh...
   */

  public void zeroVectors() {
    VectorAccessibleUtilities.clear(this);
  }

  public int getNumberOfColumns() {
    return wrappers.size();
  }

  public void allocate(int recordCount) {
    for (VectorWrapper<?> w : wrappers) {
      ValueVector v = w.getValueVector();
      v.setInitialCapacity(recordCount);
      v.allocateNew();
    }
  }

  public void allocateNew() {
    for (VectorWrapper<?> w : wrappers) {
      w.getValueVector().allocateNew();
    }
  }

  public boolean allocateNewSafe() {
    for (VectorWrapper<?> w : wrappers) {
      if (!w.getValueVector().allocateNewSafe()) {
        return false;
      }
    }
    return true;
  }

  public void setValueCount(int valueCount) {
    VectorAccessibleUtilities.setValueCount(this, valueCount);
    setRecordCount(valueCount);
  }

  /**
   * Merge two batches to create a single, combined, batch. Vectors
   * appear in the order defined by {@link BatchSchema#merge(BatchSchema)}.
   * The two batches must have identical row counts. The pattern is that
   * this container is the main part of the record batch, the other
   * represents new columns to merge.
   * <p>
   * Reference counts on the underlying buffers are <b>unchanged</b>.
   * The client code is assumed to abandon the two input containers in
   * favor of the merged container.
   *
   * @param otherContainer the container to merge with this one
   * @return a new, merged, container
   */
  public VectorContainer merge(VectorContainer otherContainer) {
    if (recordCount != otherContainer.recordCount) {
      throw new IllegalArgumentException();
    }
    VectorContainer merged = new VectorContainer(allocator);
    merged.schema = schema.merge(otherContainer.schema);
    merged.recordCount = recordCount;
    merged.wrappers.addAll(wrappers);
    merged.wrappers.addAll(otherContainer.wrappers);
    merged.schemaChanged = false;
    merged.initialized = true;
    return merged;
  }

  /**
   * Exchange buffers between two identical vector containers.
   * The schemas must be identical in both column schemas and
   * order. That is, after this call, data is exchanged between
   * the containers. Requires that both containers be owned
   * by the same allocator.
   *
   * @param other the target container with buffers to swap
   */

  public void exchange(VectorContainer other) {
    assert schema.isEquivalent(other.schema);
    assert wrappers.size() == other.wrappers.size();
    assert allocator != null  &&  allocator == other.allocator;
    for (int i = 0; i < wrappers.size(); i++) {
      wrappers.get(i).getValueVector().exchange(
          other.wrappers.get(i).getValueVector());
    }
    int temp = recordCount;
    recordCount = other.recordCount;
    other.recordCount = temp;
    boolean temp2 = schemaChanged;
    schemaChanged = other.schemaChanged;
    other.schemaChanged = temp2;
  }

  /**
   * This method create a pretty string for a record in the {@link VectorContainer}.
   * @param index The index of the record of interest.
   * @return The string representation of a record.
   */
  public String prettyPrintRecord(int index) {
    final StringBuilder sb = new StringBuilder();
    String separator = "";
    sb.append("[");

    for (VectorWrapper<?> vectorWrapper: wrappers) {
      sb.append(separator);
      separator = ", ";
      final String columnName = vectorWrapper.getField().getName();
      final Object value = vectorWrapper.getValueVector().getAccessor().getObject(index);

      // "columnName" = 11
      sb.append("\"").append(columnName).append("\" = ").append(value);
    }

    sb.append("]");
    return sb.toString();
  }

  /**
   * Safely set this container to an empty batch. An empty batch is not
   * fully empty: offset vectors must contain a single 0 entry in their
   * first position.
   */
  public void setEmpty() {
    // May not be needed; retaining for safety.
    zeroVectors();
    // Better to only allocate minimum-size offset vectors,
    // but no good way to do that presently.
    allocateNew();
    // The "fill empties" logic will set the zero
    // in the offset vectors that need it.
    setValueCount(0);
  }

  public void copySchemaFrom(VectorAccessible other) {
    for (VectorWrapper<?> wrapper : other) {
      addOrGet(wrapper.getField());
    }
  }

  public void buildFrom(BatchSchema sourceSchema) {
    for (MaterializedField field : sourceSchema) {
      add(TypeHelper.getNewVector(field, allocator));
    }
  }

  public void allocatePrecomputedChildCount(int valueCount,
      int bytesPerValue, int childValCount) {
    for (VectorWrapper<?> w : wrappers) {
      AllocationHelper.allocatePrecomputedChildCount(w.getValueVector(),
          valueCount, bytesPerValue, childValCount);
    }
    setEmpty();
  }
}
