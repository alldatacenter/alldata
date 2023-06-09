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

import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.exception.OversizedAllocationException;
import org.apache.drill.exec.expr.holders.BitHolder;
import org.apache.drill.exec.expr.holders.NullableBitHolder;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.proto.UserBitShared.SerializedField;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.vector.complex.impl.BitReaderImpl;
import org.apache.drill.exec.vector.complex.reader.FieldReader;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.DrillBuf;

/**
 * Bit implements a vector of bit-width values. Elements in the vector are accessed by position from the logical start
 * of the vector. The width of each element is 1 bit. The equivalent Java primitive is an int containing the value '0'
 * or '1'.
 */
public final class BitVector extends BaseDataValueVector implements FixedWidthVector {
  static final Logger logger = LoggerFactory.getLogger(BitVector.class);

  /**
   * Width of each fixed-width value.
   */

  public static final int VALUE_WIDTH = 1;

  /**
   * Maximum number of values that this fixed-width vector can hold
   * and stay below the maximum vector size limit. This is the limit
   * enforced when the vector is used to hold values in a repeated
   * vector.
   */

  public static final int MAX_CAPACITY = MAX_BUFFER_SIZE / VALUE_WIDTH;

  /**
   * Maximum number of values that this fixed-width vector can hold
   * and stay below the maximum vector size limit and/or stay below
   * the maximum item count. This is the limit enforced when the
   * vector is used to hold required or nullable values.
   */

  public static final int MAX_COUNT = Math.min(MAX_ROW_COUNT, MAX_CAPACITY);

  /**
   * Actual maximum vector size, in bytes, given the number of fixed-width
   * values that either fit in the maximum overall vector size, or that
   * is no larger than the maximum vector item count.
   */

  public static final int NET_MAX_SIZE = VALUE_WIDTH * MAX_COUNT;

  private final FieldReader reader = new BitReaderImpl(BitVector.this);
  private final Accessor accessor = new Accessor();
  private final Mutator mutator = new Mutator();

  private int valueCount;
  private int allocationSizeInBytes = INITIAL_VALUE_ALLOCATION;
  private int allocationMonitor;

  public BitVector(MaterializedField field, BufferAllocator allocator) {
    super(field, allocator);
  }

  @Override
  public FieldReader getReader() {
    return reader;
  }

  @Override
  public int getBufferSize() {
    return getSizeFromCount(valueCount);
  }

  @Override
  public int getBufferSizeFor(final int valueCount) {
    return getSizeFromCount(valueCount);
  }

  public static int getSizeFromCount(int valueCount) {
    return (valueCount + 7) / 8;
  }

  @Override
  public int getValueCapacity() {
    return (int) Math.min(Integer.MAX_VALUE, data.capacity() * 8L);
  }

  private int getByteIndex(int index) {
    return index / 8;
  }

  @Override
  public void setInitialCapacity(final int valueCount) {
    allocationSizeInBytes = getSizeFromCount(valueCount);
  }

  @Override
  public void allocateNew() {
    if (!allocateNewSafe()) {
      throw new OutOfMemoryException();
    }
  }

  @Override
  public boolean allocateNewSafe() {
    long curAllocationSize = allocationSizeInBytes;
    if (allocationMonitor > 10) {
      curAllocationSize = Math.max(8, allocationSizeInBytes / 2);
      allocationMonitor = 0;
    } else if (allocationMonitor < -2) {
      curAllocationSize = allocationSizeInBytes * 2L;
      allocationMonitor = 0;
    }

    try {
      allocateBytes(curAllocationSize);
    } catch (OutOfMemoryException ex) {
      return false;
    }
    return true;
  }

  @Override
  public void reset() {
    valueCount = 0;
    allocationSizeInBytes = INITIAL_VALUE_ALLOCATION;
    allocationMonitor = 0;
    zeroVector();
    super.reset();
  }

  /**
   * Allocate a new memory space for this vector. Must be called prior to using the ValueVector.
   *
   * @param valueCount
   *          The number of values which can be contained within this vector.
   */
  @Override
  public void allocateNew(int valueCount) {
    final int size = getSizeFromCount(valueCount);
    allocateBytes(size);
  }

  private void allocateBytes(final long size) {
    if (size > MAX_ALLOCATION_SIZE) {
      throw new OversizedAllocationException("Requested amount of memory is more than max allowed allocation size");
    }

    final int curSize = (int) size;
    clear();
    data = allocator.buffer(curSize);
    zeroVector();
    allocationSizeInBytes = curSize;
  }

  /**
   * Allocate new buffer with double capacity, and copy data into the new buffer. Replace vector's buffer with new buffer, and release old one
   */
  public void reAlloc() {
    long newAllocationSize = allocationSizeInBytes * 2L;

    // Some operations, such as Value Vector#exchange, can change DrillBuf data field without corresponding allocation size changes.
    // Check that the size of the allocation is sufficient to copy the old buffer.
    while (newAllocationSize < data.capacity()) {
      newAllocationSize *= 2L;
    }

    if (newAllocationSize > MAX_ALLOCATION_SIZE) {
      throw new OversizedAllocationException("Requested amount of memory is more than max allowed allocation size");
    }

    final int curSize = (int)newAllocationSize;
    final DrillBuf newBuf = allocator.buffer(curSize);
    newBuf.setZero(0, newBuf.capacity());
    newBuf.setBytes(0, data, 0, data.capacity());
    data.release();
    data = newBuf;
    allocationSizeInBytes = curSize;
  }

  // This version uses the base version because this vector appears to not be
  // used, so not worth the effort to avoid zero-fill.

  @Override
  public DrillBuf reallocRaw(int newAllocationSize) {
    while (allocationSizeInBytes < newAllocationSize) {
      reAlloc();
    }
    return data;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void zeroVector() {
    data.setZero(0, data.capacity());
  }

  @Override
  public int getValueWidth() {
    return VALUE_WIDTH;
  }

  public void copyFrom(int inIndex, int outIndex, BitVector from) {
    this.mutator.set(outIndex, from.accessor.get(inIndex));
  }

  public void copyFromSafe(int inIndex, int outIndex, BitVector from) {
    while (outIndex >= this.getValueCapacity()) {
      reAlloc();
    }
    copyFrom(inIndex, outIndex, from);
  }

  @Override
  public void copyEntry(int toIndex, ValueVector from, int fromIndex) {
    copyFrom(fromIndex, toIndex, (BitVector) from);
  }

  @Override
  public void load(SerializedField metadata, DrillBuf buffer) {
    Preconditions.checkArgument(field.getName().equals(metadata.getNamePart().getName()),
                                "The field %s doesn't match the provided metadata %s.", field, metadata);
    final int valueCount = metadata.getValueCount();
    final int expectedLength = getSizeFromCount(valueCount);
    final int actualLength = metadata.getBufferLength();
    assert expectedLength == actualLength: "expected and actual buffer sizes do not match";

    clear();
    data = buffer.slice(0, actualLength);
    data.retain();
    this.valueCount = valueCount;
  }

  @Override
  public Mutator getMutator() {
    return new Mutator();
  }

  @Override
  public Accessor getAccessor() {
    return new Accessor();
  }

  @Override
  public TransferPair getTransferPair(BufferAllocator allocator) {
    return new TransferImpl(getField(), allocator);
  }

  @Override
  public TransferPair getTransferPair(String ref, BufferAllocator allocator) {
    return new TransferImpl(getField().withPath(ref), allocator);
  }

  @Override
  public TransferPair makeTransferPair(ValueVector to) {
    return new TransferImpl((BitVector) to);
  }

  public void transferTo(BitVector target) {
    target.clear();
    if (target.data != null) {
      target.data.release();
    }
    target.data = data;
    target.data.retain(1);
    target.valueCount = valueCount;
    clear();
  }

  public void splitAndTransferTo(int startIndex, int length, BitVector target) {
    assert startIndex + length <= valueCount;
    int firstByteIndex = getByteIndex(startIndex);//byte offset of the first src byte
    int numBytesHoldingSourceBits = getSizeFromCount(length); //src bytes to read (including start/end bytes that might not be fully copied)
    int firstBitOffset = startIndex % 8; //Offset of first src bit within the first src byte
    if (firstBitOffset == 0) {
      target.clear();
      // slice
      if (target.data != null) {
        target.data.release();
      }
      target.data = data.slice(firstByteIndex, numBytesHoldingSourceBits);
      target.data.retain(1);
    } else {
      // Copy data
      // When the first bit starts from the middle of a byte (firstBitOffset != 0), copy data from src BitVector.
      // Each byte in the target is composed by a part in i-th byte, another part in (i+1)-th byte.
      // The last byte copied to target is a bit tricky :
      //   1) if length requires partly byte (length % 8 !=0), copy the remaining bits only.
      //   2) otherwise, copy the last byte in the same way as to the prior bytes.
      target.clear();
      target.allocateNew(length);
      // TODO maybe do this one word at a time, rather than byte?

      byte byteI, byteIPlus1 = 0;
      for (int i = 0; i < numBytesHoldingSourceBits - 1; i++) {
        byteI = this.data.getByte(firstByteIndex + i);
        byteIPlus1 = this.data.getByte(firstByteIndex + i + 1);
        // Extract higher-X bits from first byte i and lower-Y bits from byte (i + 1), where X + Y = 8 bits
        // Lower-Y  bits are the MS bits of the byte to be written (target byte) and Higher-X are the LS bits.
        // The target bytes are assembled in accordance to little-endian ordering (byte[0] = LS bit, byte[7] = MS bit)
        target.data.setByte(i, (((byteI & 0xFF) >>> firstBitOffset) + (byteIPlus1 <<  (8 - firstBitOffset))));
      }

      //Copying the last n bits

      //Copy length is not a byte-multiple
      if (length % 8 != 0) {
        // start is not byte aligned so we have to copy some bits from the last full byte read in the
        // previous loop
        // if numBytesHoldingSourceBits == 1, lastButOneByte is the first byte, but we have not read it yet, so read it
        byte lastButOneByte = (numBytesHoldingSourceBits == 1) ? this.data.getByte(firstByteIndex) : byteIPlus1;
        byte bitsFromLastButOneByte = (byte)((lastButOneByte & 0xFF) >>> firstBitOffset);
        // if last bit to be copied is before the end of the first byte, then mask of the trailing extra bits
        if (8 > (length + firstBitOffset)) {
          byte mask = (byte)((0x1 << length) - 1);
          bitsFromLastButOneByte = (byte)((bitsFromLastButOneByte & mask));
        }

        // If we have to read more bits than what we have already read, read it into lastByte otherwise set lastByte to 0.
        // (length % 8) is num of remaining bits to be read.
        // (8 - firstBitOffset) is the number of bits already read into lastButOneByte but not used in the previous write.
        // We do not have to read more bits if (8 - firstBitOffset >= length % 8)
        final int lastByte = (8 - firstBitOffset >= length % 8) ?
                0 : this.data.getByte(firstByteIndex + numBytesHoldingSourceBits);
        target.data.setByte(numBytesHoldingSourceBits - 1, bitsFromLastButOneByte + (lastByte << (8 - firstBitOffset)));
      } else {
        target.data.setByte(numBytesHoldingSourceBits - 1,
            (((this.data.getByte(firstByteIndex + numBytesHoldingSourceBits - 1) & 0xFF) >>> firstBitOffset) +
                     (this.data.getByte(firstByteIndex + numBytesHoldingSourceBits) <<  (8 - firstBitOffset))));
      }
    }
    target.getMutator().setValueCount(length);
  }

  @Override
  public void exchange(ValueVector other) {
    super.exchange(other);
    int temp = valueCount;
    valueCount = ((BitVector) other).valueCount;
    ((BitVector) other).valueCount = temp;
  }

  private class TransferImpl implements TransferPair {
    BitVector to;

    public TransferImpl(MaterializedField field, BufferAllocator allocator) {
      this.to = new BitVector(field, allocator);
    }

    public TransferImpl(BitVector to) {
      this.to = to;
    }

    @Override
    public BitVector getTo() {
      return to;
    }

    @Override
    public void transfer() {
      transferTo(to);
    }

    @Override
    public void splitAndTransfer(int startIndex, int length) {
      splitAndTransferTo(startIndex, length, to);
    }

    @Override
    public void copyValueSafe(int fromIndex, int toIndex) {
      to.copyFromSafe(fromIndex, toIndex, BitVector.this);
    }
  }

  private void incrementAllocationMonitor() {
    ++allocationMonitor;
  }

  public class Accessor extends BaseAccessor {

    /**
     * Get the byte holding the desired bit, then mask all other bits. Iff the result is 0, the bit was not set.
     *
     * @param index
     *          position of the bit in the vector
     * @return 1 if set, otherwise 0
     */
    public final int get(int index) {
      int byteIndex = index >> 3;
      byte b = data.getByte(byteIndex);
      int bitIndex = index & 7;
      return Long.bitCount(b &  (1L << bitIndex));
    }

    @Override
    public boolean isNull(int index) {
      return false;
    }

    @Override
    public final Boolean getObject(int index) {
      return Boolean.valueOf(get(index) != 0);
    }

    @Override
    public final int getValueCount() {
      return valueCount;
    }

    public final void get(int index, BitHolder holder) {
      holder.value = get(index);
    }

    public final void get(int index, NullableBitHolder holder) {
      holder.isSet = 1;
      holder.value = get(index);
    }
  }

  /**
   * MutableBit implements a vector of bit-width values. Elements in the vector are accessed by position from the
   * logical start of the vector. Values should be pushed onto the vector sequentially, but may be randomly accessed.
   *
   * NB: this class is automatically generated from ValueVectorTypes.tdd using FreeMarker.
   */
  public class Mutator extends BaseMutator {

    private Mutator() { }

    /**
     * Set the bit at the given index to the specified value.
     *
     * @param index
     *          position of the bit to set
     * @param value
     *          value to set (either 1 or 0)
     */
    public final void set(int index, int value) {
      int byteIndex = index >> 3;
      int bitIndex = index & 7;
      byte currentByte = data.getByte(byteIndex);
      byte bitMask = (byte) (1L << bitIndex);
      if (value != 0) {
        currentByte |= bitMask;
      } else {
        currentByte -= (bitMask & currentByte);
      }

      data.setByte(byteIndex, currentByte);
    }

    public final void set(int index, BitHolder holder) {
      set(index, holder.value);
    }

    final void set(int index, NullableBitHolder holder) {
      set(index, holder.value);
    }

    public void setSafe(int index, int value) {
      while (index >= getValueCapacity()) {
        reAlloc();
      }
      set(index, value);
    }

    public void setSafe(int index, BitHolder holder) {
      while (index >= getValueCapacity()) {
        reAlloc();
      }
      set(index, holder.value);
    }

    public void setSafe(int index, NullableBitHolder holder) {
      while (index >= getValueCapacity()) {
        reAlloc();
      }
      set(index, holder.value);
    }

    @Override
    public final void setValueCount(int valueCount) {
      int currentValueCapacity = getValueCapacity();
      BitVector.this.valueCount = valueCount;
      int idx = getSizeFromCount(valueCount);
      while (valueCount > getValueCapacity()) {
        reAlloc();
      }
      if (valueCount > 0 && currentValueCapacity > valueCount * 2) {
        incrementAllocationMonitor();
      } else if (allocationMonitor > 0) {
        allocationMonitor = 0;
      }
      VectorTrimmer.trim(data, idx);
    }

    @Override
    public final void generateTestData(int values) {
      boolean even = true;
      for (int i = 0; i < values; i++, even = !even) {
        if (even) {
          set(i, 1);
        }
      }
      setValueCount(values);
    }
  }

  @Override
  public void clear() {
    valueCount = 0;
    super.clear();
  }

  @Override
  public int getPayloadByteCount(int valueCount) {
    return getSizeFromCount(valueCount);
  }

  @Override
  public void toNullable(ValueVector nullableVector) {
    NullableBitVector dest = (NullableBitVector) nullableVector;
    dest.getMutator().fromNotNullable(this);
  }
}
