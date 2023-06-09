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
package org.apache.drill.exec.vector;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import io.netty.buffer.DrillBuf;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.proto.UserBitShared.SerializedField;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.vector.complex.reader.FieldReader;

/** UntypedNullVector is to represent a value vector with {@link org.apache.drill.common.types.TypeProtos.MinorType#NULL}
 *  All values in the vector represent two semantic implications: 1) the value is unknown, 2) the type is unknown.
 *  Because of this, we only have to keep track of the number of values in value vector,
 *  and there is no allocated buffer to back up this value vector. Therefore, the majority of
 *  methods in this class is either no-op, or throws {@link UnsupportedOperationException}.
 *
 */
public final class UntypedNullVector extends BaseDataValueVector implements FixedWidthVector {

  /**
   * Width of each fixed-width value.
   */
  public static final int VALUE_WIDTH = 0;

  private final Accessor accessor = new Accessor();
  private final Mutator mutator = new Mutator();
  private int valueCount;

  public UntypedNullVector(MaterializedField field, BufferAllocator allocator) {
    super(field, allocator);
  }

  @Override
  public FieldReader getReader() {
    return new UntypedReaderImpl();
  }

  @Override
  public int getBufferSizeFor(final int valueCount) { return 0; }

  @Override
  public int getValueCapacity() { return ValueVector.MAX_ROW_COUNT; }

  @Override
  public Accessor getAccessor() { return accessor; }

  @Override
  public Mutator getMutator() { return mutator; }

  @Override
  public void setInitialCapacity(final int valueCount) { }

  @Override
  public void allocateNew() { }

  @Override
  public boolean allocateNewSafe() { return true; }

  @Override
  public void allocateNew(final int valueCount) {
    this.valueCount = valueCount;
  }

  @Override
  public void reset() { }

  /**
   * {@inheritDoc}
   */
  @Override
  public void zeroVector() { }

  @Override
  public DrillBuf reallocRaw(int newAllocationSize) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getValueWidth() {
    return VALUE_WIDTH;
  }

  @Override
  public void load(SerializedField metadata, DrillBuf buffer) {
    Preconditions.checkArgument(this.field.getName().equals(metadata.getNamePart().getName()),
        "The field %s doesn't match the provided metadata %s.", this.field, metadata);
    final int actualLength = metadata.getBufferLength();
    final int valueCount = metadata.getValueCount();
    final int expectedLength = valueCount * VALUE_WIDTH;
    assert actualLength == expectedLength : String.format("Expected to load %d bytes but actually loaded %d bytes", expectedLength, actualLength);

    this.valueCount = valueCount;
  }

  @Override
  public TransferPair getTransferPair(BufferAllocator allocator){
    return new TransferImpl(getField(), allocator);
  }

  @Override
  public TransferPair getTransferPair(String ref, BufferAllocator allocator){
    return new TransferImpl(getField().withPath(ref), allocator);
  }

  @Override
  public TransferPair makeTransferPair(ValueVector to) {
    return new TransferImpl((UntypedNullVector) to);
  }

  public void transferTo(UntypedNullVector target) {
    target.valueCount = valueCount;
    clear();
  }

  public void splitAndTransferTo(int startIndex, int length, UntypedNullVector target) { }

  @Override
  public int getPayloadByteCount(int valueCount) { return 0; }

  private class TransferImpl implements TransferPair{
    private final UntypedNullVector to;

    public TransferImpl(MaterializedField field, BufferAllocator allocator){
      to = new UntypedNullVector(field, allocator);
    }

    public TransferImpl(UntypedNullVector to) {
      this.to = to;
    }

    @Override
    public UntypedNullVector getTo() { return to; }

    @Override
    public void transfer(){
      transferTo(to);
    }

    @Override
    public void splitAndTransfer(int startIndex, int length) {
      Preconditions.checkPositionIndexes(startIndex, startIndex + length, valueCount);
      splitAndTransferTo(startIndex, length, to);
    }

    @Override
    public void copyValueSafe(int fromIndex, int toIndex) {
      Preconditions.checkElementIndex(fromIndex, valueCount);
      to.copyFromSafe(fromIndex, toIndex, UntypedNullVector.this);
    }
  }

  public void copyFrom(int fromIndex, int thisIndex, UntypedNullVector from) { }

  public void copyFromSafe(int fromIndex, int thisIndex, UntypedNullVector from) { }

  @Override
  public void copyEntry(int toIndex, ValueVector from, int fromIndex) {
  }

  @Override
  public void clear() {
    valueCount = 0;
  }

  public final class Accessor extends BaseAccessor {
    @Override
    public int getValueCount() {
      return valueCount;
    }

    @Override
    public boolean isNull(int index){
      Preconditions.checkElementIndex(index, valueCount);
      return true;
    }

    public int isSet(int index) {
      Preconditions.checkElementIndex(index, valueCount);
      return 0;
    }

    @Override
    public Object getObject(int index) {
      Preconditions.checkElementIndex(index, valueCount);
      return null;
    }

    public void get(int index, UntypedNullHolder holder) {
      Preconditions.checkElementIndex(index, valueCount);
    }
  }

  /**
   * UntypedNullVector.Mutator throws Exception for most of its mutate operations, except for the ones that set
   * value counts.
   *
   */
  public final class Mutator extends BaseMutator {

    private Mutator() {}

    public void set(int index, UntypedNullHolder holder) {
      throw new UnsupportedOperationException("UntypedNullVector does not support set");
    }

    public void set(int index, int isSet, UntypedNullHolder holder) {
      throw new UnsupportedOperationException("UntypedNullVector does not support set");
    }

    public void setSafe(int index, UntypedNullHolder holder) {
      throw new UnsupportedOperationException("UntypedNullVector does not support setSafe");
    }

    public void setSafe(int index, int isSet, UntypedNullHolder holder) {
      throw new UnsupportedOperationException("UntypedNullVector does not support setSafe");
    }

    public void setScalar(int index, UntypedNullHolder holder) throws VectorOverflowException {
      throw new UnsupportedOperationException("UntypedNullVector does not support setScalar");
    }

    public void setArrayItem(int index, UntypedNullHolder holder) throws VectorOverflowException {
      throw new UnsupportedOperationException("UntypedNullVector does not support setArrayItem");
    }

    @Override
    public void generateTestData(int size) {
      setValueCount(size);
    }

    public void generateTestDataAlt(int size) {
      setValueCount(size);
    }

    @Override
    public void setValueCount(int valueCount) {
      UntypedNullVector.this.valueCount = valueCount;
    }
  }
}
