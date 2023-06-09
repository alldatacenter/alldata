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
import org.apache.drill.exec.physical.resultSet.impl.SingleVectorState.IsSetVectorState;
import org.apache.drill.exec.physical.resultSet.impl.SingleVectorState.OffsetVectorState;
import org.apache.drill.exec.physical.resultSet.impl.UnionState.UnionVectorState;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.VariantMetadata;
import org.apache.drill.exec.record.metadata.VariantSchema;
import org.apache.drill.exec.vector.accessor.ObjectWriter;
import org.apache.drill.exec.vector.accessor.VariantWriter;
import org.apache.drill.exec.vector.accessor.impl.HierarchicalFormatter;
import org.apache.drill.exec.vector.accessor.writer.ListWriterImpl;
import org.apache.drill.exec.vector.accessor.writer.SimpleListShim;
import org.apache.drill.exec.vector.accessor.writer.UnionVectorShim;
import org.apache.drill.exec.vector.accessor.writer.UnionWriterImpl;
import org.apache.drill.exec.vector.accessor.writer.WriterEvents;
import org.apache.drill.exec.vector.complex.ListVector;
import org.apache.drill.exec.vector.complex.UnionVector;
import org.apache.drill.exec.vector.complex.impl.UnionWriter;

/**
 * Represents the contents of a list vector. A list vector is an odd creature.
 * It starts as a list of nothing, evolves to be a nullable array of a single
 * type, then becomes a nullable array of nullable unions holding nullable
 * types.
 * <p>
 * At the writer level, the list consists of two parts: an array writer and
 * a union writer. The union writer is needed because, unless the client tells
 * us otherwise, we must be prepared for the list to become a union.
 * <p>
 * Holds the column states for the "columns" that make up the type members
 * of the union, and implements the writer callbacks to add members to
 * a list (disguised as a union), creating the actual union with the
 * number of member types becomes two or more.
 * <p>
 * This class is similar to the {@link UnionState}, except that this
 * version must handle the list transitions from no members to single
 * member to union, and so this class is a bit more complex than the
 * simple union case.
 * <p>
 * This implementation is based on a desired invariant: that once a client
 * obtains a writer for the list, that writer never becomes invalid. This
 * means we must carefully consider the list lifecycle. The list is
 * represented as an array writer. When the list has
 * no members, there would be no child for the array writer, a call to
 * <tt>listArray.entry()</tt> would have to return null, which would be
 * awkward and unlike any other writer use case. Once the list has a single
 * type, the call to <tt>listArray.entry()</tt> might return a writer for
 * that type. But, once the list becomes a repeated union, then
 * <tt>listArray.entry()</tt> would have to return a union writer. This is
 * the kind of muddy semantics we wish to avoid.
 * <p>
 * Instead, we model the list as a repeated union at all times. When the
 * list has no type, then the list is a repeated union with no members.
 * Once the list has a member, we have a repeated union of one member type.
 * Finally, when adding another type, we have a repeated union of two
 * types. The key is, in all cases, <tt>listArray.entry()</tt> returns
 * a {@link UnionWriter}, so the client gets a consistent view.
 * <p>
 * Since the list itself changes form (no type, single type, then
 * union), we hide that lifecycle internal to the writer and to this
 * list state. The result is that the client need not care about the
 * odd list lifecycle. But, on the flip side, this class, and the union
 * writer, must go out of their way to hide these details.
 * <p>
 * At the writer level, the union writer uses "shims" to map from the union
 * view to the actual list representation (no type, single type or union.)
 * <p>
 * At this level, this class must handle those cases as well, creating the
 * union (by promoting the list) when needed. The result is a bit complex
 * (for the code here), but simple for the client.
 */

public class ListState extends ContainerState
  implements VariantWriter.VariantWriterListener {

  /**
   * Wrapper around the list vector (and its optional contained union).
   * Manages the state of the "overhead" vectors such as the bits and
   * offset vectors for the list, and (via the union vector state) the
   * types vector for the union. The union vector state starts of as
   * a dummy state (before the list has been "promoted" to a union)
   * then becomes populated with the union state once the list is
   * promoted.
   */

  protected static class ListVectorState implements VectorState {

    private final ColumnMetadata schema;
    private final ListVector vector;
    private final VectorState bitsVectorState;
    private final VectorState offsetVectorState;
    private VectorState memberVectorState;

    public ListVectorState(UnionWriterImpl writer, ListVector vector) {
      this.schema = writer.schema();
      this.vector = vector;
      bitsVectorState = new IsSetVectorState(writer, vector.getBitsVector());
      offsetVectorState = new OffsetVectorState(writer, vector.getOffsetVector(), writer.elementPosition());
      memberVectorState = new NullVectorState();
    }

    public ListVectorState(ListWriterImpl writer, WriterEvents elementWriter, ListVector vector) {
      this.schema = writer.schema();
      this.vector = vector;
      bitsVectorState = new IsSetVectorState(writer, vector.getBitsVector());
      offsetVectorState = new OffsetVectorState(writer, vector.getOffsetVector(), elementWriter);
      memberVectorState = new NullVectorState();
    }

    private void replaceMember(VectorState memberState) {
      memberVectorState = memberState;
    }

    private VectorState memberVectorState() { return memberVectorState; }

    @Override
    public int allocate(int cardinality) {
      return bitsVectorState.allocate(cardinality) +
          offsetVectorState.allocate(cardinality + 1) +
          memberVectorState.allocate(childCardinality(cardinality));
    }

    @Override
    public void rollover(int cardinality) {
      bitsVectorState.rollover(cardinality);
      offsetVectorState.rollover(cardinality);
      memberVectorState.rollover(childCardinality(cardinality));
    }

    private int childCardinality(int cardinality) {
      return cardinality * schema.expectedElementCount();
    }

    @Override
    public void harvestWithLookAhead() {
      bitsVectorState.harvestWithLookAhead();
      offsetVectorState.harvestWithLookAhead();
      memberVectorState.harvestWithLookAhead();
    }

    @Override
    public void startBatchWithLookAhead() {
      bitsVectorState.startBatchWithLookAhead();
      offsetVectorState.startBatchWithLookAhead();
      memberVectorState.startBatchWithLookAhead();
    }

    @Override
    public void close() {
      bitsVectorState.close();
      offsetVectorState.close();
      memberVectorState.close();
    }

    @SuppressWarnings("unchecked")
    @Override
    public ListVector vector() {
      return vector;
    }

    @Override
    public boolean isProjected() {
      return true;
    }

    @Override
    public void dump(HierarchicalFormatter format) {
      // TODO Auto-generated method stub
    }
  }

  /**
   * Map of types to member columns, used to track the set of child
   * column states for this list. This map mimics the actual set of
   * vectors in the union (or list, before a list becomes a union),
   * and matches the set of child writers in the union writer.
   */

  private final Map<MinorType, ColumnState> columns = new HashMap<>();

  public ListState(LoaderInternals loader, ResultVectorCache vectorCache) {
    super(loader, vectorCache);
  }

  public VariantMetadata variantSchema() {
    return parentColumn.schema().variantSchema();
  }

  public ListWriterImpl listWriter() {
    return (ListWriterImpl) parentColumn.writer.array();
  }

  private UnionWriterImpl unionWriter() {
    return (UnionWriterImpl) listWriter().variant();
  }

  private ListVector listVector() {
    return parentColumn.vector();
  }

  private UnionVector unionVector() {
    return (UnionVector) listVectorState().memberVectorState().vector();
  }

  private ListVectorState listVectorState() {
    return (ListVectorState) parentColumn.vectorState();
  }

  private boolean isSingleType() {
    return variantSchema().isSimple();
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
    if (isSingleType()) {
      throw new IllegalStateException("List is defined to contains a single type.");
    }
    return addColumn(member).writer();
  }

  /**
   * Add a new column representing a type within the list. This is where the list
   * strangeness occurs. The list starts with no type, then evolves to have a single
   * type (held by the list vector). Upon the second type, the list vector is modified
   * to hold a union vector, which then holds the existing type and the new type.
   * After that, the third and later types simply are added to the union.
   * Very, very ugly, but it is how the list vector works until we improve it...
   * <p>
   * We must make three parallel changes:
   * <ul>
   * <li>Modify the list vector structure.</li>
   * <li>Modify the union writer structure. (If a list type can evolve, then the
   * writer structure is an array of unions. But, since the union itself does
   * not exist in the 0 and 1 type cases, we use "shims" to model these odd
   * cases.</li>
   * <li>Modify the vector state for the list. If the list is "promoted" to a
   * union, then add the union to the list vector's state for management in
   * vector events.</li>
   * </ul>
   */

  @Override
  protected void addColumn(ColumnState colState) {
    final MinorType type = colState.schema().type();
    assert ! columns.containsKey(type);

    // Add the new column (type) to metadata.

    final int prevColCount = columns.size();
    columns.put(type, colState);

    if (prevColCount == 0) {
      addFirstType(colState);
    } else if (prevColCount == 1) {
      addSecondType(colState);
    } else {

      // Already have a union; just add another type.

      unionVector().addType(colState.vector());
    }
  }

  private void addFirstType(ColumnState colState) {

    // Going from no types to one type.

    // Add the member to the list as its data vector.

    listVector().setChildVector(colState.vector());

    // Don't add the type to the vector state; we manage it as part
    // of the collection of member columns.

    // Note that we may have written 1 or more nulls to the array
    // before we make this 0-to-1 type transition. If the new type is
    // a scalar, it is nullable. Automatic back-fill will fill prior values
    // with null, so there is nothing to do here.
    //
    // Note, however, that if this first type is a map, there is no way
    // to mark that map as null; we loose the nullability state. However,
    // if we later promote the single-type map to a union, we can't
    // recover the null states and so the previously-null values won't
    // be null. We could fix this, for a single batch, by keeping track
    // of the null positions. But, there is no way to mark later maps
    // as null. Another choice would be to force a transition directly
    // to a union if the first type is a map. None of this has ever worked
    // and so is left as an exercise for later once we work out what we
    // actually want to support.

    // Create the single type shim.

    unionWriter().bindShim(new SimpleListShim());
  }

  /**
   * Perform the delicate dance of promoting a list vector from a single type to
   * a union, while leaving the writer client blissfully ignorant that the underlying
   * vector representation just did a radical change. Key tasks:
   * <ul>
   * <li>Create the new column (type member) requested by the client.</li>
   * <li>The List vector currently has a single type. Promote the list to
   * a union, adding the existing type (column) as the first union member.</li>
   * <li>Initialize the union's type vector with either the type of the existing
   * column, or null, depending on the setting of the is-set bits in the existing
   * column vector.</li>
   * <li>Since we've written values into the union's type vector, mark the
   * last-write position in the union vector's type vector writer to reflect
   * these writes. (Otherwise, the writer will helpfully zero-fill the previous
   * positions as part of it's back-fill handling.</li>
   * <li>Replace the single-type shim in the union vector with a full union
   * shim.</li>
   * <li>Move the existing column writer or the member column across from the
   * single-writer shim to the new union shim.</li>
   * <li>Augment the list vector's vector state to include a vector state for
   * the newly created union vector.</li>
   * </ul>
   * <p>
   * Here, yet again, an editorial comment might be useful. List vectors are
   * very strange and not at all well designed for high-speed writing. They are
   * too complex; too much can go wrong and there are too many states to handle.
   * Not only that, variant types don't play well with a relational model like
   * SQL. This code works, but the overall list concept really needs rethinking.
   *
   * @param colState the column state for the newly added type column; the
   * one causing the list to change from single-type to a union
   */

  private void addSecondType(ColumnState colState) {
    final UnionWriterImpl unionWriter = unionWriter();
    final ListVector listVector = listVector();

    // Going from one type to a union

    // Convert the list from single type to a union,
    // moving across the previous type vector.

    final int typeFillCount = unionWriter.elementPosition().writeIndex();
    final UnionVector unionVector = listVector.convertToUnion(
        innerCardinality(), typeFillCount);
    unionVector.addType(colState.vector());

    // Replace the single-type shim with a union shim, copying
    // across the existing writer.

    final SimpleListShim oldShim = (SimpleListShim) unionWriter.shim();
    final UnionVectorShim newShim = new UnionVectorShim(unionVector);
    unionWriter.bindShim(newShim);
    newShim.addMemberWriter(oldShim.memberWriter());
    newShim.initTypeIndex(typeFillCount);

    // The union vector will be managed within the list vector state.
    // (Do this last because the union vector state expects the union
    // writer to be operating in "union mode".

    listVectorState().replaceMember(new UnionVectorState(unionVector, unionWriter));
  }

  /**
   * Set the one and only type when building a single-type list.
   *
   * @param memberState the column state for the list elements
   */

  public void setSubColumn(ColumnState memberState) {
    assert columns.isEmpty();
    columns.put(memberState.schema().type(), memberState);
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
