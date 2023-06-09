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
package org.apache.drill.exec.vector.complex;

import org.apache.drill.shaded.guava.com.google.common.collect.ObjectArrays;
import io.netty.buffer.DrillBuf;
import java.util.List;
import java.util.Set;

import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.memory.AllocationManager.BufferLedger;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.util.CallBack;
import org.apache.drill.exec.util.JsonStringArrayList;
import org.apache.drill.exec.vector.AddOrGetResult;
import org.apache.drill.exec.vector.NullableVector;
import org.apache.drill.exec.vector.UInt1Vector;
import org.apache.drill.exec.vector.UInt4Vector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.VectorDescriptor;
import org.apache.drill.exec.vector.ZeroVector;
import org.apache.drill.exec.vector.complex.impl.ComplexCopier;
import org.apache.drill.exec.vector.complex.impl.UnionListReader;
import org.apache.drill.exec.vector.complex.impl.UnionListWriter;
import org.apache.drill.exec.vector.complex.reader.FieldReader;
import org.apache.drill.exec.vector.complex.writer.FieldWriter;

/**
 * "Non-repeated" LIST vector. This vector holds some other vector as
 * its data element. Unlike a repeated vector, the child element can
 * change dynamically. It starts as nothing (the LATE type). It can then
 * change to a single type (typically a map but can be anything.) If
 * another type is needed, the list morphs again, this time to a list
 * of unions. The prior single type becomes a member of the new union
 * (which requires back-filling is-set values.)
 * <p>
 * Why this odd behavior? The LIST type apparently attempts to model
 * certain JSON types. In JSON, we can have lists like this:
 * <pre><code>
 * {a: [null, null]}
 * {a: [10, "foo"]}
 * {a: [{name: "fred", balance: 10}, null]
 * {a: null}
 * </code></pre>
 * Compared with Drill, JSON has a number of additional list-related
 * abilities:
 * <ul>
 * <li>A list can be null. (In Drill, an array can be empty, but not
 * null.)</li>
 * <li>A list element can be null. (In Drill, a repeated type is an
 * array of non-nullable elements, so list elements can't be
 * null.</li>
 * <li>A list can contain heterogeneous types. (In Drill, repeated
 * types are arrays of a single type.</li>
 * </ul>
 * The LIST vector is an attempt to implement full JSON behavior. The
 * list:
 * <ul>
 * <li>Allows the list value for a row to be null. (To handle the
 * <code>{list: null}</code> case.</li>
 * <li>Allows list elements to be null. (To handle the
 * <code>{list: [10, null 30]}</code case.)</li>
 * <li>Allows the list to be a single type. (To handle the list
 * of nullable ints above.</li>
 * <li>Allows the list to be of multiple types, by creating a list
 * of UNIONs. (To handle the
 * <code>{list: ["fred", 10]}</code> case.</li>
 * </ul>
 *
 * <h4>Background</h4>
 *
 * The above is the theory. The problem is, the goals are very difficult
 * to achieve, and the code here does not quite do so.
 * The code here is difficult to maintain and understand.
 * The first thing to understand is that union vectors are
 * broken in most operators, and so major bugs remain in union
 * and list vectors that
 * have not had to be fixed. Recent revisions attempt to
 * fix or works around some of the bugs, but many remain.
 * <p>
 * Unions have a null bit for the union itself. That is, a union can be an
 * int, say, or. a Varchar, or null. Oddly, the Int and Varchar can also be
 * null (we use nullable vectors so we can mark the unused values as null.)
 * So, we have a two-level null bit. The most logical way to interpret it is
 * that a union value can be:
 * <ul>
 * <li>Untyped null (if the type is not set and the null bit (really, the isSet
 * bit) is unset.) Typed null if the type is set and EITHER the union's isSet
 * bit is unset OR the union's isSet bit is set, but the data vector's isSet
 * bit is not set. It is not clear in the code which convention is assumed, or
 * if different code does it differently.</li>
 * <li>Now, add all that to a list. A list can be a list of something (ints, say,
 * or maps.) When the list is a list of maps, the entire value for a row can
 * be null. But individual maps can't be null. In a list, however, individual
 * ints can be null (because we use a nullable int vector.)</li>
 * </ul>
 * So, when a list of (non-nullable maps) converts to a list of unions (one of
 * which is a map), we suddenly now have the list null bit and the union null
 * bit to worry about. We have to go and back-patch the isSet vector for all
 * the existing map entries in the new union so that we don't end up with all
 * previous entries becoming null by default.
 * <p>
 * Another issue is that the metadata for a list should reflect the structure
 * of the list. The {@link MaterializedField} contains a child field, which
 * points to the element of the list. If that child is a UNION, then the UNION's
 * <code>MaterializedField</code> contains subtypes for each type in the
 * union. Now, note that the LIST's metadata contains the child, so we need
 * to update the LIST's <code>MaterializedField</code> each time we add a
 * type to the UNION. And, since the LIST is part of a row or map, then we
 * have to update the metadata in those objects to propagate the change.
 * <p>
 * The problem is that the original design assumed that
 * <code>MaterializedField</code> is immutable. The above shows that it
 * clearly is not. So, we have a tension between the original immutable
 * design and the necessity of mutating the <code>MaterializedField</code>
 * to keep everything in sync.
 * <p>
 * Of course, there is another solution: don't include subtypes and children
 * in the <code>MaterializedField</code>, then we don't have the propagation
 * problem.
 * <p>
 * The code for this class kind of punts on the issue: the metadata is not
 * maintained and can get out of sync. THis makes the metadata useless: one must
 * recover actual structure by traversing vectors. There was an attempt to fix
 * this, but doing so changes the metadata structure, which broke clients. So,
 * we have to live with broken metadata and work around the issues. The metadata
 * sync issue exists in many places, but is most obvious in the LIST vector
 * because of the sheer complexity in this class.
 * <p>
 * This is why the code notes say that this is a mess.
 * <p>
 * It is hard to simply fix the bugs because this is a design problem. If the list
 * and union vectors don't need to work (they barely work today), then any
 * design is fine. See the list of JIRA tickets below for more information.
 * <p>
 * Fundamental issue: should Drill support unions and lists? Is the current
 * approach compatible with SQL? Is there a better approach? If such changes
 * are made, they are breaking changes, and so must be done as part of a major
 * version, such as the much-discussed "Drill 2.0". Or, perhaps as part of
 * a conversion to use Apache Arrow, which also would be a major breaking
 * change.
 *
 * @see DRILL-5955, DRILL-6037, DRILL-5958, DRILL-6046, DRILL-6062, DRILL-3806
 *      to get a flavor for the issues.
 */

public class ListVector extends BaseRepeatedValueVector {

  public static final String UNION_VECTOR_NAME = "$union$";

  private final UInt1Vector bits;
  private final Mutator mutator = new Mutator();
  private final Accessor accessor = new Accessor();
  private final UnionListWriter writer;
  private UnionListReader reader;

  public ListVector(MaterializedField field, BufferAllocator allocator, CallBack callBack) {
    super(field, allocator);
    // Can't do this. See below.
//    super(field.cloneEmpty(), allocator);
    this.bits = new UInt1Vector(MaterializedField.create(BITS_VECTOR_NAME, Types.required(MinorType.UINT1)), allocator);
    this.field.addChild(getDataVector().getField());
    this.writer = new UnionListWriter(this);
    this.reader = new UnionListReader(this);
//
//    // To be consistent with the map vector, create the child if a child is
//    // given in the field. This is a mess. See DRILL-6046.
//    But, can't do this because the deserialization mechanism passes in a
//    field with children filled in, but with no expectation that the vector will
//    match its schema until this vector itself is loaded. Indeed a mess.
//
//    assert field.getChildren().size() <= 1;
//    for (MaterializedField child : field.getChildren()) {
//      if (child.getName().equals(DATA_VECTOR_NAME)) {
//        continue;
//      }
//      setChildVector(BasicTypeHelper.getNewVector(child, allocator, callBack));
//    }
  }

  public UnionListWriter getWriter() {
    return writer;
  }

  @Override
  public void allocateNew() throws OutOfMemoryException {
    super.allocateNewSafe();
    bits.allocateNewSafe();
  }

  public void transferTo(ListVector target) {
    offsets.makeTransferPair(target.offsets).transfer();
    bits.makeTransferPair(target.bits).transfer();
    if (target.getDataVector() instanceof ZeroVector) {
      target.addOrGetVector(new VectorDescriptor(vector.getField().getType()));
    }
    getDataVector().makeTransferPair(target.getDataVector()).transfer();
  }

  public void copyFromSafe(int inIndex, int outIndex, ListVector from) {
    copyFrom(inIndex, outIndex, from);
  }

  public void copyFrom(int inIndex, int outIndex, ListVector from) {
    final FieldReader in = from.getReader();
    in.setPosition(inIndex);
    final FieldWriter out = getWriter();
    out.setPosition(outIndex);
    ComplexCopier.copy(in, out);
  }

  @Override
  public void copyEntry(int toIndex, ValueVector from, int fromIndex) {
    copyFromSafe(fromIndex, toIndex, (ListVector) from);
  }

  @Override
  public ValueVector getDataVector() { return vector; }

  public ValueVector getBitsVector() { return bits; }

  @Override
  public TransferPair getTransferPair(String ref, BufferAllocator allocator) {
    return new TransferImpl(field.withPath(ref), allocator);
  }

  @Override
  public TransferPair makeTransferPair(ValueVector target) {
    return new TransferImpl((ListVector) target);
  }

  private class TransferImpl implements TransferPair {

    private final ListVector to;

    public TransferImpl(MaterializedField field, BufferAllocator allocator) {
      to = new ListVector(field, allocator, null);
      to.addOrGetVector(new VectorDescriptor(vector.getField().getType()));
    }

    public TransferImpl(ListVector to) {
      this.to = to;
      to.addOrGetVector(new VectorDescriptor(vector.getField().getType()));
    }

    @Override
    public void transfer() {
      transferTo(to);
    }

    @Override
    public void splitAndTransfer(int startIndex, int length) {
      to.allocateNew();
      for (int i = 0; i < length; i++) {
        copyValueSafe(startIndex + i, i);
      }
    }

    @Override
    public ValueVector getTo() {
      return to;
    }

    @Override
    public void copyValueSafe(int from, int to) {
      this.to.copyFrom(from, to, ListVector.this);
    }
  }

  @Override
  public Accessor getAccessor() {
    return accessor;
  }

  @Override
  public Mutator getMutator() {
    return mutator;
  }

  @Override
  public FieldReader getReader() {
    return reader;
  }

  @Override
  public boolean allocateNewSafe() {
    /* boolean to keep track if all the memory allocation were successful
     * Used in the case of composite vectors when we need to allocate multiple
     * buffers for multiple vectors. If one of the allocations failed we need to
     * clear all the memory that we allocated
     */
    boolean success = false;
    try {
      if (! offsets.allocateNewSafe()) {
        return false;
      }
      success = vector.allocateNewSafe();
      success = success && bits.allocateNewSafe();
    } finally {
      if (! success) {
        clear();
      }
    }
    if (success) {
      offsets.zeroVector();
      bits.zeroVector();
    }
    return success;
  }

  @Override
  protected UserBitShared.SerializedField.Builder getMetadataBuilder() {
    return getField().getAsBuilder()
            .setValueCount(getAccessor().getValueCount())
            .setBufferLength(getBufferSize())
            .addChild(offsets.getMetadata())
            .addChild(bits.getMetadata())
            .addChild(vector.getMetadata());
  }

  @Override
  public <T extends ValueVector> AddOrGetResult<T> addOrGetVector(VectorDescriptor descriptor) {
    final AddOrGetResult<T> result = super.addOrGetVector(descriptor);
    reader = new UnionListReader(this);
    return result;
  }

  @Override
  public int getBufferSize() {
    if (getAccessor().getValueCount() == 0) {
      return 0;
    }
    return offsets.getBufferSize() + bits.getBufferSize() + vector.getBufferSize();
  }

  @Override
  public void clear() {
    bits.clear();
    lastSet = 0;
    super.clear();
  }

  @Override
  public DrillBuf[] getBuffers(boolean clear) {
    final DrillBuf[] buffers = ObjectArrays.concat(
        offsets.getBuffers(false),
        ObjectArrays.concat(bits.getBuffers(false),
            vector.getBuffers(false), DrillBuf.class),
        DrillBuf.class);
    if (clear) {
      for (final DrillBuf buffer : buffers) {
        buffer.retain();
      }
      clear();
    }
    return buffers;
  }

  @Override
  public void load(UserBitShared.SerializedField metadata, DrillBuf buffer) {
    final UserBitShared.SerializedField offsetMetadata = metadata.getChild(0);
    offsets.load(offsetMetadata, buffer);

    final int offsetLength = offsetMetadata.getBufferLength();
    final UserBitShared.SerializedField bitMetadata = metadata.getChild(1);
    final int bitLength = bitMetadata.getBufferLength();
    bits.load(bitMetadata, buffer.slice(offsetLength, bitLength));

    final UserBitShared.SerializedField vectorMetadata = metadata.getChild(2);
    if (isEmptyType()) {
      addOrGetVector(VectorDescriptor.create(vectorMetadata.getMajorType()));
    }

    final int vectorLength = vectorMetadata.getBufferLength();
    vector.load(vectorMetadata, buffer.slice(offsetLength + bitLength, vectorLength));
  }

  public boolean isEmptyType() {
    return getDataVector() == DEFAULT_DATA_VECTOR;
  }

  @Override
  public void setChildVector(ValueVector childVector) {

    // Unlike the repeated list vector, the (plain) list vector
    // adds the dummy vector as a child type.

    assert field.getChildren().size() == 1;
    assert field.getChildren().iterator().next().getType().getMinorType() == MinorType.LATE;
    field.removeChild(vector.getField());

    super.setChildVector(childVector);

    // Initial LATE type vector not added as a subtype initially.
    // So, no need to remove it, just add the new subtype. Since the
    // MajorType is immutable, must build a new one and replace the type
    // in the materialized field. (We replace the type, rather than creating
    // a new materialized field, to preserve the link to this field from
    // a parent map, list or union.)

    assert field.getType().getSubTypeCount() == 0;
    field.replaceType(
        field.getType().toBuilder()
          .addSubType(childVector.getField().getType().getMinorType())
          .build());
  }

  /**
   * Promote the list to a union. Called from old-style writers. This implementation
   * relies on the caller to set the types vector for any existing values.
   * This method simply clears the existing vector.
   *
   * @return the new union vector
   */

  public UnionVector promoteToUnion() {
    final UnionVector vector = createUnion();

    // Replace the current vector, clearing its data. (This is the
    // old behavior.)

    replaceDataVector(vector);
    reader = new UnionListReader(this);
    return vector;
  }

  /**
   * Revised form of promote to union that correctly fixes up the list
   * field metadata to match the new union type. Since this form handles
   * both the vector and metadata revisions, it is a "full" promotion.
   *
   * @return the new union vector
   */

  public UnionVector fullPromoteToUnion() {
    final UnionVector unionVector = createUnion();

    // Set the child vector, replacing the union vector's child
    // metadata with the union vector's metadata. (This is the
    // new, correct, behavior.)

    setChildVector(unionVector);
    return unionVector;
  }

  /**
   * Promote to a union, preserving the existing data vector as a member of the
   * new union. Back-fill the types vector with the proper type value for
   * existing rows.
   * @return the new union vector
   */

  public UnionVector convertToUnion(int allocValueCount, int valueCount) {
    assert allocValueCount >= valueCount;
    final UnionVector unionVector = createUnion();
    unionVector.allocateNew(allocValueCount);

    // Preserve the current vector (and its data) if it is other than
    // the default. (New behavior used by column writers.)

    if (! isEmptyType()) {
      unionVector.addType(vector);
      final int prevType = vector.getField().getType().getMinorType().getNumber();
      final UInt1Vector.Mutator typeMutator = unionVector.getTypeVector().getMutator();

      // If the previous vector was nullable, then promote the nullable state
      // to the type vector by setting either the null marker or the type
      // marker depending on the original nullable values.

      if (vector instanceof NullableVector) {
        final UInt1Vector.Accessor bitsAccessor =
            ((UInt1Vector) ((NullableVector) vector).getBitsVector()).getAccessor();
        for (int i = 0; i < valueCount; i++) {
          typeMutator.setSafe(i, (bitsAccessor.get(i) == 0)
              ? UnionVector.NULL_MARKER
              : prevType);
        }
      } else {

        // The value is not nullable. (Perhaps it is a map.)
        // Note that the original design of lists have a flaw: if the sole member
        // is a map, then map entries can't be nullable when the only type, but
        // become nullable when in a union. What a mess...

        for (int i = 0; i < valueCount; i++) {
          typeMutator.setSafe(i, prevType);
        }
      }
    }
    vector = unionVector;
    return unionVector;
  }

  private UnionVector createUnion() {
    final MaterializedField newField = MaterializedField.create(UNION_VECTOR_NAME, Types.optional(MinorType.UNION));
    final UnionVector unionVector = new UnionVector(newField, allocator, null);

    // For efficiency, should not create a reader that will never be used.
    // Keeping for backward compatibility.
    //
    // This vector already creates a reader for every vector. The reader isn't used for
    // the ResultSetVector, but may be used by older code. Someone can perhaps
    // track down uses, I kept this code. The Union vector is normally created in the
    // older "complex writer" which may use this reader.
    //
    // Moving forward, we should revisit union vectors (and the older complex writer).
    // At that time, we can determine if the vector needs to own a reader, or if
    // the reader can be created as needed.
    // If we create a "result set reader" that works cross-batch in parallel with the
    // result set loader (for writing), then vectors would not carry their own readers.
    //
    // Yes indeed, union vectors are a mess.

    reader = new UnionListReader(this);
    return unionVector;
  }

  private int lastSet;

  public class Accessor extends BaseRepeatedAccessor {

    @Override
    public Object getObject(int index) {
      if (isNull(index)) {
        return null;
      }
      final List<Object> vals = new JsonStringArrayList<>();
      final UInt4Vector.Accessor offsetsAccessor = offsets.getAccessor();
      final int start = offsetsAccessor.get(index);
      final int end = offsetsAccessor.get(index + 1);
      final ValueVector.Accessor valuesAccessor = getDataVector().getAccessor();
      for(int i = start; i < end; i++) {
        vals.add(valuesAccessor.getObject(i));
      }
      return vals;
    }

    @Override
    public boolean isNull(int index) {
      return bits.getAccessor().get(index) == 0;
    }
  }

  public class Mutator extends BaseRepeatedMutator {
    public void setNotNull(int index) {
      bits.getMutator().setSafe(index, 1);
      lastSet = index + 1;
    }

    @Override
    public void startNewValue(int index) {
      for (int i = lastSet; i <= index; i++) {
        offsets.getMutator().setSafe(i + 1, offsets.getAccessor().get(i));
      }
      setNotNull(index);
      lastSet = index + 1;
    }

    @Override
    public void setValueCount(int valueCount) {
      // TODO: populate offset end points
      if (valueCount == 0) {
        offsets.getMutator().setValueCount(0);
      } else {
        for (int i = lastSet; i < valueCount; i++) {
          offsets.getMutator().setSafe(i + 1, offsets.getAccessor().get(i));
        }
        offsets.getMutator().setValueCount(valueCount + 1);
      }
      final int childValueCount = valueCount == 0 ? 0 : offsets.getAccessor().get(valueCount);
      vector.getMutator().setValueCount(childValueCount);
      bits.getMutator().setValueCount(valueCount);
    }
  }

  @Override
  public void collectLedgers(Set<BufferLedger> ledgers) {
    offsets.collectLedgers(ledgers);
    bits.collectLedgers(ledgers);
    super.collectLedgers(ledgers);
  }

  @Override
  public int getPayloadByteCount(int valueCount) {
    if (valueCount == 0) {
      return 0;
    }

    return offsets.getPayloadByteCount(valueCount) + bits.getPayloadByteCount(valueCount) +
           super.getPayloadByteCount(valueCount);
  }
}
