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

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.expr.BasicTypeHelper;
import org.apache.drill.exec.expr.holders.RepeatedValueHolder;
import org.apache.drill.exec.memory.AllocationManager.BufferLedger;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.proto.UserBitShared.SerializedField;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.util.CallBack;
import org.apache.drill.exec.vector.AddOrGetResult;
import org.apache.drill.exec.vector.AllocationHelper;
import org.apache.drill.exec.vector.SchemaChangeCallBack;
import org.apache.drill.exec.vector.UInt4Vector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.VectorDescriptor;

import io.netty.buffer.DrillBuf;

public abstract class AbstractRepeatedMapVector extends AbstractMapVector implements RepeatedValueVector {

  protected final UInt4Vector offsets; // offsets to start of each record (considering record indices are 0-indexed)
  protected final EmptyValuePopulator emptyPopulator;

  protected AbstractRepeatedMapVector(MaterializedField field, BufferAllocator allocator, CallBack callBack) {
    this(field, new UInt4Vector(BaseRepeatedValueVector.OFFSETS_FIELD, allocator), callBack);
  }

  protected AbstractRepeatedMapVector(MaterializedField field, UInt4Vector offsets, CallBack callBack) {
    super(field, offsets.getAllocator(), callBack);
    this.offsets = offsets;
    this.emptyPopulator = new EmptyValuePopulator(offsets);
  }

  @Override
  public UInt4Vector getOffsetVector() { return offsets; }

  @Override
  public ValueVector getDataVector() {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T extends ValueVector> AddOrGetResult<T> addOrGetVector(VectorDescriptor descriptor) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setInitialCapacity(int numRecords) {
    offsets.setInitialCapacity(numRecords + 1);
    for (ValueVector v : this) {
      v.setInitialCapacity(numRecords * RepeatedValueVector.DEFAULT_REPEAT_PER_RECORD);
    }
  }

  public void allocateNew(int groupCount, int innerValueCount) {
    clear();
    try {
      allocateOffsetsNew(groupCount);
      for (ValueVector v : getChildren()) {
        AllocationHelper.allocatePrecomputedChildCount(v, groupCount, 50, innerValueCount);
      }
    } catch (OutOfMemoryException e) {
      clear();
      throw e;
    }
    getMutator().reset();
  }

  public void allocateOffsetsNew(int groupCount) {
    offsets.allocateNew(groupCount + 1);
    offsets.zeroVector();
  }

  public Iterator<String> fieldNameIterator() {
    return getChildFieldNames().iterator();
  }

  @Override
  public List<ValueVector> getPrimitiveVectors() {
    List<ValueVector> primitiveVectors = super.getPrimitiveVectors();
    primitiveVectors.add(offsets);
    return primitiveVectors;
  }

  @Override
  public int getBufferSize() {
    if (getAccessor().getValueCount() == 0) {
      return 0;
    }
    return offsets.getBufferSize() + super.getBufferSize();
  }

  @Override
  public int getAllocatedSize() {
    return offsets.getAllocatedSize() + super.getAllocatedSize();
  }

  @Override
  public int getBufferSizeFor(int valueCount) {
    if (valueCount == 0) {
      return 0;
    }

    long bufferSize = offsets.getBufferSizeFor(valueCount);
    for (ValueVector v : this) {
      bufferSize += v.getBufferSizeFor(valueCount);
    }

    return (int) bufferSize;
  }

  @Override
  public void close() {
    offsets.close();
    super.close();
  }

  public TransferPair getTransferPairToSingleMap(String reference, BufferAllocator allocator) {
    return new SingleMapTransferPair(this, reference, allocator);
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
      if (!offsets.allocateNewSafe()) {
        return false;
      }
      success =  super.allocateNewSafe();
    } finally {
      if (!success) {
        clear();
      }
    }
    offsets.zeroVector();
    return success;
  }

  abstract class AbstractRepeatedMapTransferPair<T extends AbstractRepeatedMapVector> implements TransferPair {

    protected final T to;
    protected final T from;
    private final TransferPair[] pairs;

    public AbstractRepeatedMapTransferPair(T to) {
      this(to, true);
    }

    @SuppressWarnings("unchecked")
    public AbstractRepeatedMapTransferPair(T to, boolean allocate) {
      this.from = (T) AbstractRepeatedMapVector.this;
      this.to = to;
      this.pairs = new TransferPair[from.size()];

      int i = 0;
      ValueVector vector;
      for (String child : from.getChildFieldNames()) {
        int preSize = to.size();
        vector = from.getChild(child);
        if (vector == null) {
          continue;
        }

        ValueVector newVector = to.addOrGet(child, vector.getField().getType(), vector.getClass());
        if (allocate && to.size() != preSize) {
          newVector.allocateNew();
        }

        pairs[i++] = vector.makeTransferPair(newVector);
      }
    }

    @Override
    public void transfer() {
      from.offsets.transferTo(to.offsets);
      for (TransferPair p : pairs) {
        p.transfer();
      }
      from.clear();
    }

    @Override
    public ValueVector getTo() {
      return to;
    }

    @Override
    public void copyValueSafe(int srcIndex, int destIndex) {
      RepeatedValueHolder holder = getValueHolder();
      from.getAccessor().get(srcIndex, holder);
      to.emptyPopulator.populate(destIndex + 1);
      int newIndex = to.offsets.getAccessor().get(destIndex);
      for (int i = holder.start; i < holder.end; i++, newIndex++) {
        for (TransferPair p : pairs) {
          p.copyValueSafe(i, newIndex);
        }
      }
      to.offsets.getMutator().setSafe(destIndex + 1, newIndex);
    }

    @Override
    public void splitAndTransfer(int groupStart, int groups) {
      UInt4Vector.Accessor a = from.offsets.getAccessor();
      UInt4Vector.Mutator m = to.offsets.getMutator();

      int startPos = a.get(groupStart);
      int endPos = a.get(groupStart + groups);
      int valuesToCopy = endPos - startPos;

      to.offsets.clear();
      to.offsets.allocateNew(groups + 1);

      int normalizedPos;
      for (int i = 0; i < groups + 1; i++) {
        normalizedPos = a.get(groupStart + i) - startPos;
        m.set(i, normalizedPos);
      }

      m.setValueCount(groups + 1);
      to.emptyPopulator.populate(groups);

      for (TransferPair p : pairs) {
        p.splitAndTransfer(startPos, valuesToCopy);
      }
    }
  }

  static class SingleMapTransferPair implements TransferPair {

    private static final TypeProtos.MajorType MAP_TYPE = Types.required(TypeProtos.MinorType.MAP);

    private final TransferPair[] pairs;
    private final AbstractRepeatedMapVector from;
    private final MapVector to;

    public SingleMapTransferPair(AbstractRepeatedMapVector from, String path, BufferAllocator allocator) {
      this(from, new MapVector(MaterializedField.create(path, MAP_TYPE), allocator, new SchemaChangeCallBack()), false);
    }

    public SingleMapTransferPair(AbstractRepeatedMapVector from, MapVector to, boolean allocate) {
      this.from = from;
      this.to = to;
      this.pairs = new TransferPair[from.size()];
      int i = 0;
      ValueVector vector;
      for (String child : from.getChildFieldNames()) {
        int preSize = to.size();
        vector = from.getChild(child);
        if (vector == null) {
          continue;
        }
        ValueVector newVector = to.addOrGet(child, vector.getField().getType(), vector.getClass());
        if (allocate && to.size() != preSize) {
          newVector.allocateNew();
        }
        pairs[i++] = vector.makeTransferPair(newVector);
      }
    }

    @Override
    public void transfer() {
      for (TransferPair p : pairs) {
        p.transfer();
      }
      to.getMutator().setValueCount(from.getAccessor().getValueCount());
      from.clear();
    }

    @Override
    public ValueVector getTo() {
      return to;
    }

    @Override
    public void copyValueSafe(int from, int to) {
      for (TransferPair p : pairs) {
        p.copyValueSafe(from, to);
      }
    }

    @Override
    public void splitAndTransfer(int startIndex, int length) {
      for (TransferPair p : pairs) {
        p.splitAndTransfer(startIndex, length);
      }
      to.getMutator().setValueCount(length);
    }
  }

  transient private AbstractRepeatedMapTransferPair<?> ephPair;

  public void copyFromSafe(int fromIndex, int thisIndex, AbstractRepeatedMapVector from) {
    if (ephPair == null || ephPair.from != from) {
      ephPair = (AbstractRepeatedMapTransferPair<?>) from.makeTransferPair(this);
    }
    ephPair.copyValueSafe(fromIndex, thisIndex);
  }

  @Override
  public void copyEntry(int toIndex, ValueVector from, int fromIndex) {
    copyFromSafe(fromIndex, toIndex, (AbstractRepeatedMapVector) from);
  }

  @Override
  public int getValueCapacity() {
    return Math.max(offsets.getValueCapacity() - 1, 0);
  }

  @Override
  public void exchange(ValueVector other) {
    super.exchange(other);
    offsets.exchange(((AbstractRepeatedMapVector) other).offsets);
  }

  @Override
  public DrillBuf[] getBuffers(boolean clear) {
    return ArrayUtils.addAll(offsets.getBuffers(clear), super.getBuffers(clear));
  }

  @Override
  public void load(SerializedField metadata, DrillBuf buffer) {
    List<SerializedField> children = metadata.getChildList();

    SerializedField offsetField = children.get(0);
    offsets.load(offsetField, buffer);
    int bufOffset = offsetField.getBufferLength();

    for (int i = 1; i < children.size(); i++) {
      SerializedField child = children.get(i);
      MaterializedField fieldDef = MaterializedField.create(child);
      ValueVector vector = getChild(fieldDef.getName());
      if (vector == null) {
        // if we arrive here, we didn't have a matching vector.
        vector = BasicTypeHelper.getNewVector(fieldDef, allocator);
        putChild(fieldDef.getName(), vector);
      }
      int vectorLength = child.getBufferLength();
      vector.load(child, buffer.slice(bufOffset, vectorLength));
      bufOffset += vectorLength;
    }

    assert bufOffset == buffer.writerIndex();
  }

  @Override
  public SerializedField getMetadata() {
    SerializedField.Builder builder = getField()
        .getAsBuilder()
        .setBufferLength(getBufferSize())
        // while we don't need to actually read this on load, we need it to
        // make sure we don't skip deserialization of this vector
        .setValueCount(getAccessor().getValueCount());
    builder.addChild(offsets.getMetadata());
    for (ValueVector child : getChildren()) {
      builder.addChild(child.getMetadata());
    }
    return builder.build();
  }

  public abstract class Accessor implements RepeatedAccessor {

    public void get(int index, RepeatedValueHolder holder) {
      assert index < getValueCapacity() :
          String.format("Attempted to access index %d when value capacity is %d",
              index, getValueCapacity());
      UInt4Vector.Accessor offsetsAccessor = offsets.getAccessor();
      holder.start = offsetsAccessor.get(index);
      holder.end = offsetsAccessor.get(index + 1);
    }

    @Override
    public int getValueCount() {
      return Math.max(offsets.getAccessor().getValueCount() - 1, 0);
    }

    @Override
    public int getInnerValueCount() {
      int valueCount = getValueCount();
      if (valueCount == 0) {
        return 0;
      }
      return offsets.getAccessor().get(valueCount);
    }

    @Override
    public int getInnerValueCountAt(int index) {
      return offsets.getAccessor().get(index+1) - offsets.getAccessor().get(index);
    }

    @Override
    public boolean isEmpty(int index) {
      return false;
    }

    @Override
    public boolean isNull(int index) {
      return false;
    }
  }

  public abstract class Mutator implements RepeatedMutator {

    @Override
    public void startNewValue(int index) {
      emptyPopulator.populate(index + 1);
    }

    @Override
    public void setValueCount(int topLevelValueCount) {
      int childValueCount;
      if (topLevelValueCount == 0) {
        childValueCount = 0;
        offsets.getMutator().setValueCount(0);
      } else {
        emptyPopulator.populate(topLevelValueCount);
        childValueCount = offsets.getAccessor().get(topLevelValueCount);
      }
      for (ValueVector v : getChildren()) {
        v.getMutator().setValueCount(childValueCount);
      }
    }

    @Override
    public void reset() { }

    @Override
    public void generateTestData(int values) { }

    public int add(int index) {
      int prevEnd = offsets.getAccessor().get(index + 1);
      offsets.getMutator().setSafe(index + 1, prevEnd + 1);
      return prevEnd;
    }

    @Override
    public void exchange(ValueVector.Mutator other) { }
  }

  @Override
  public void clear() {
    getMutator().reset();

    offsets.clear();
    for (ValueVector vector : getChildren()) {
      vector.clear();
    }
  }

  @Override
  public void collectLedgers(Set<BufferLedger> ledgers) {
    super.collectLedgers(ledgers);
    offsets.collectLedgers(ledgers);
  }

  @Override
  public void toNullable(ValueVector nullableVector) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getPayloadByteCount(int valueCount) {
    if (valueCount == 0) {
      return 0;
    }

    int entryCount = offsets.getAccessor().get(valueCount);
    int count = offsets.getPayloadByteCount(valueCount);

    for (ValueVector v : getChildren()) {
      count += v.getPayloadByteCount(entryCount);
    }
    return count;
  }

  @Override
  public abstract Accessor getAccessor();

  /**
   * Creates an instance of value holder corresponding to the vector.
   *
   * @return value holder for the vector
   */
  abstract RepeatedValueHolder getValueHolder();
}
