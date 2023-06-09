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
import java.lang.Override;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.memory.AllocationManager.BufferLedger;
import org.apache.drill.exec.vector.BaseDataValueVector;
import org.apache.drill.exec.vector.BaseValueVector;
import org.apache.drill.exec.vector.VariableWidthVector;

<@pp.dropOutputFile />
<#list vv.types as type>
<#list type.minor as minor>

<#assign friendlyType = (minor.friendlyType!minor.boxedType!type.boxedType) />

<#if type.major == "VarLen">
<@pp.changeOutputFile name="/org/apache/drill/exec/vector/${minor.class}Vector.java" />

<#include "/@includes/license.ftl" />

package org.apache.drill.exec.vector;

<#include "/@includes/vv_imports.ftl" />

import java.util.Iterator;
import java.nio.ByteOrder;

/**
 * ${minor.class}Vector implements a vector of variable width values.  Elements in the vector
 * are accessed by position from the logical start of the vector.  A fixed width offsetVector
 * is used to convert an element's position to it's offset from the start of the (0-based)
 * DrillBuf. Size is inferred from adjacent elements.
 * <ul>
 * <li>The width of each element is ${type.width} byte(s). Note that the actual width is
 * variable, this width is used as a guess for certain calculations.</li>
 * <li>The equivalent Java primitive is '${minor.javaType!type.javaType}'<li>
 * </ul>
 * NB: this class is automatically generated from <tt>${.template_name}</tt>
 * and <tt>ValueVectorTypes.tdd</tt> using FreeMarker.
 */

public final class ${minor.class}Vector extends BaseDataValueVector implements VariableWidthVector {

  private static final int INITIAL_BYTE_COUNT = Math.min(INITIAL_VALUE_ALLOCATION * DEFAULT_RECORD_BYTE_COUNT, MAX_BUFFER_SIZE);

  private final UInt${type.width}Vector offsetVector = new UInt${type.width}Vector(offsetsField, allocator);
  private final FieldReader reader = new ${minor.class}ReaderImpl(${minor.class}Vector.this);

  private final Accessor accessor;
  private final Mutator mutator;

  private int allocationSizeInBytes = INITIAL_BYTE_COUNT;
  private int allocationMonitor = 0;

  public ${minor.class}Vector(MaterializedField field, BufferAllocator allocator) {
    super(field, allocator);
    this.accessor = new Accessor();
    this.mutator = new Mutator();
  }

  @Override
  public FieldReader getReader(){
    return reader;
  }

  @Override
  public int getBufferSize(){
    if (getAccessor().getValueCount() == 0) {
      return 0;
    }
    return offsetVector.getBufferSize() + data.writerIndex();
  }

  @Override
  public int getAllocatedSize() {
    return offsetVector.getAllocatedSize() + data.capacity();
  }

  @Override
  public int getBufferSizeFor(int valueCount) {
    if (valueCount == 0) {
      return 0;
    }

    int idx = offsetVector.getAccessor().get(valueCount);
    return offsetVector.getBufferSizeFor(valueCount + 1) + idx;
  }

  @Override
  public int getValueCapacity(){
    return Math.max(offsetVector.getValueCapacity() - 1, 0);
  }

  @Override
  public int getByteCapacity(){
    return data.capacity();
  }

  /**
  * Return the number of bytes contained in the current var len byte vector.
  * TODO: Remove getVarByteLength with it's implementation after all client's are moved to using getCurrentSizeInBytes.
  * It's kept as is to preserve backward compatibility
  * @return
  */
  @Override
  public int getCurrentSizeInBytes() {
    return getVarByteLength();
  }

  /**
   * Return the number of bytes contained in the current var len byte vector.
   * @return
   */
  public int getVarByteLength(){
    int valueCount = getAccessor().getValueCount();
    if(valueCount == 0) {
      return 0;
    }
    return offsetVector.getAccessor().get(valueCount);
  }

  @Override
  public SerializedField getMetadata() {
    return getMetadataBuilder()
             .addChild(offsetVector.getMetadata())
             .setValueCount(getAccessor().getValueCount())
             .setBufferLength(getBufferSize())
             .build();
  }

  @Override
  public void load(SerializedField metadata, DrillBuf buffer) {
    // the bits vector is the first child (the order in which the children are added in getMetadataBuilder is significant)
    SerializedField offsetField = metadata.getChild(0);
    offsetVector.load(offsetField, buffer);

    int capacity = buffer.capacity();
    int offsetsLength = offsetField.getBufferLength();
    data = buffer.slice(offsetsLength, capacity - offsetsLength);
    data.retain();
  }

  @Override
  public void clear() {
    super.clear();
    offsetVector.clear();
  }

  @Override
  public DrillBuf[] getBuffers(boolean clear) {
    DrillBuf[] buffers = ObjectArrays.concat(offsetVector.getBuffers(false), super.getBuffers(false), DrillBuf.class);
    if (clear) {
      // does not make much sense but we have to retain buffers even when clear is set. refactor this interface.
      for (DrillBuf buffer:buffers) {
        buffer.retain(1);
      }
      clear();
    }
    return buffers;
  }

  public long getOffsetAddr(){
    return offsetVector.getBuffer().memoryAddress();
  }

  @Override
  public UInt${type.width}Vector getOffsetVector(){
    return offsetVector;
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
    return new TransferImpl((${minor.class}Vector) to);
  }

  public void transferTo(${minor.class}Vector target){
    target.clear();
    this.offsetVector.transferTo(target.offsetVector);
    target.data = data.transferOwnership(target.allocator).buffer;
    target.data.writerIndex(data.writerIndex());
    clear();
  }

  public void splitAndTransferTo(int startIndex, int length, ${minor.class}Vector target) {
    UInt${type.width}Vector.Accessor offsetVectorAccessor = this.offsetVector.getAccessor();
    int startPoint = offsetVectorAccessor.get(startIndex);
    int sliceLength = offsetVectorAccessor.get(startIndex + length) - startPoint;
    target.clear();
    target.offsetVector.allocateNew(length + 1);
    offsetVectorAccessor = this.offsetVector.getAccessor();
    UInt4Vector.Mutator targetOffsetVectorMutator = target.offsetVector.getMutator();
    for (int i = 0; i < length + 1; i++) {
      targetOffsetVectorMutator.set(i, offsetVectorAccessor.get(startIndex + i) - startPoint);
    }
    target.data = data.slice(startPoint, sliceLength).transferOwnership(target.allocator).buffer;
    target.getMutator().setValueCount(length);
}

  protected void copyFrom(int fromIndex, int thisIndex, ${minor.class}Vector from){
    UInt4Vector.Accessor fromOffsetVectorAccessor = from.offsetVector.getAccessor();
    int start = fromOffsetVectorAccessor.get(fromIndex);
    int end = fromOffsetVectorAccessor.get(fromIndex + 1);
    int len = end - start;

    int outputStart = offsetVector.data.get${(minor.javaType!type.javaType)?cap_first}(thisIndex * ${type.width});
    from.data.getBytes(start, data, outputStart, len);
    offsetVector.data.set${(minor.javaType!type.javaType)?cap_first}( (thisIndex+1) * ${type.width}, outputStart + len);
  }

  public void copyFromSafe(int fromIndex, int thisIndex, ${minor.class}Vector from){
    UInt${type.width}Vector.Accessor fromOffsetVectorAccessor = from.offsetVector.getAccessor();
    int start = fromOffsetVectorAccessor.get(fromIndex);
    int end =   fromOffsetVectorAccessor.get(fromIndex + 1);
    int len = end - start;
    int outputStart = offsetVector.getAccessor().get(thisIndex);

    while(data.capacity() < outputStart + len) {
      reAlloc();
    }

    offsetVector.getMutator().setSafe(thisIndex + 1, outputStart + len);
    from.data.getBytes(start, data, outputStart, len);
  }

  @Override
  public void copyEntry(int toIndex, ValueVector from, int fromIndex) {
    copyFromSafe(fromIndex, toIndex, (${minor.class}Vector) from);
  }

  @Override
  public void collectLedgers(Set<BufferLedger> ledgers) {
    offsetVector.collectLedgers(ledgers);
    super.collectLedgers(ledgers);
  }

  @Override
  public int getPayloadByteCount(int valueCount) {
    if (valueCount == 0) {
      return 0;
    }
    // If 1 or more values, then the last value is set to
    // the offset of the next value, which is the same as
    // the length of existing values.
    // In addition to the actual data bytes, we must also
    // include the "overhead" bytes: the offset vector entries
    // that accompany each column value. Thus, total payload
    // size is consumed text bytes + consumed offset vector
    // bytes.
    return offsetVector.getAccessor().get(valueCount) +
           offsetVector.getPayloadByteCount(valueCount);
  }

  private class TransferImpl implements TransferPair{
    private final ${minor.class}Vector to;

    public TransferImpl(MaterializedField field, BufferAllocator allocator){
      to = new ${minor.class}Vector(field, allocator);
    }

    public TransferImpl(${minor.class}Vector to){
      this.to = to;
    }

    @Override
    public ${minor.class}Vector getTo(){
      return to;
    }

    @Override
    public void transfer(){
      transferTo(to);
    }

    @Override
    public void splitAndTransfer(int startIndex, int length) {
      splitAndTransferTo(startIndex, length, to);
    }

    @Override
    public void copyValueSafe(int fromIndex, int toIndex) {
      to.copyFromSafe(fromIndex, toIndex, ${minor.class}Vector.this);
    }
  }

  @Override
  public void setInitialCapacity(int valueCount) {
    long size = 1L * valueCount * ${type.width};
    if (size > MAX_ALLOCATION_SIZE) {
      throw new OversizedAllocationException("Requested amount of memory is more than max allowed allocation size");
    }
    allocationSizeInBytes = (int) size;
    offsetVector.setInitialCapacity(valueCount + 1);
  }

  @Override
  public void allocateNew() {
    if(!allocateNewSafe()){
      throw new OutOfMemoryException("Failure while allocating buffer.");
    }
  }

  @Override
  public boolean allocateNewSafe() {
    long curAllocationSize = allocationSizeInBytes;
    if (allocationMonitor > 10) {
      curAllocationSize = Math.max(MIN_BYTE_COUNT, curAllocationSize / 2);
      allocationMonitor = 0;
    } else if (allocationMonitor < -2) {
      curAllocationSize = curAllocationSize * 2L;
      allocationMonitor = 0;
    }

    if (curAllocationSize > MAX_ALLOCATION_SIZE) {
      return false;
    }

    clear();
    /* Boolean to keep track if all the memory allocations were successful
     * Used in the case of composite vectors when we need to allocate multiple
     * buffers for multiple vectors. If one of the allocations failed we need to
     * clear all the memory that we allocated
     */
    try {
      int requestedSize = (int)curAllocationSize;
      data = allocator.buffer(requestedSize);
      allocationSizeInBytes = requestedSize;
      offsetVector.allocateNew();
    } catch (OutOfMemoryException e) {
      clear();
      return false;
    }
    data.readerIndex(0);
    offsetVector.zeroVector();
    return true;
  }

  @Override
  public void allocateNew(int totalBytes, int valueCount) {
    clear();
    assert totalBytes >= 0;
    try {
      data = allocator.buffer(totalBytes);
      offsetVector.allocateNew(valueCount + 1);
    } catch (RuntimeException e) {
      clear();
      throw e;
    }
    data.readerIndex(0);
    allocationSizeInBytes = totalBytes;
    offsetVector.zeroVector();
  }

  @Override
  public void reset() {
    allocationSizeInBytes = INITIAL_BYTE_COUNT;
    allocationMonitor = 0;
    data.readerIndex(0);
    offsetVector.zeroVector();
    super.reset();
  }

  public void reAlloc() {
    long newAllocationSize = allocationSizeInBytes*2L;

    // Some operations, such as Value Vector#exchange, can be change DrillBuf data field without corresponding allocation size changes.
    // Check that the size of the allocation is sufficient to copy the old buffer.
    while (newAllocationSize < data.capacity()) {
      newAllocationSize *= 2L;
    }

    if (newAllocationSize > MAX_ALLOCATION_SIZE)  {
      throw new OversizedAllocationException("Unable to expand the buffer. Max allowed buffer size is reached.");
    }

    reallocRaw((int) newAllocationSize);
  }

  @Override
  public DrillBuf reallocRaw(int newAllocationSize) {
    DrillBuf newBuf = allocator.buffer(newAllocationSize);
    newBuf.setBytes(0, data, 0, data.capacity());
    data.release();
    data = newBuf;
    allocationSizeInBytes = newAllocationSize;
    return data;
  }

  public void decrementAllocationMonitor() {
    if (allocationMonitor > 0) {
      allocationMonitor = 0;
    }
    --allocationMonitor;
  }

  private void incrementAllocationMonitor() {
    ++allocationMonitor;
  }

  @Override
  public Accessor getAccessor(){
    return accessor;
  }

  @Override
  public Mutator getMutator() {
    return mutator;
  }

  @Override
  public void exchange(ValueVector other) {
    super.exchange(other);
    ${minor.class}Vector target = (${minor.class}Vector) other;
    offsetVector.exchange(target.offsetVector);
  }

  @Override
  public void toNullable(ValueVector nullableVector) {
    Nullable${minor.class}Vector dest = (Nullable${minor.class}Vector) nullableVector;
    dest.getMutator().fromNotNullable(this);
  }

  public final class Accessor extends BaseValueVector.BaseAccessor implements VariableWidthAccessor {
    final UInt${type.width}Vector.Accessor oAccessor = offsetVector.getAccessor();

    public long getStartEnd(int index){
      return oAccessor.getTwoAsLong(index);
    }

    public byte[] get(int index) {
      assert index >= 0;
      int startIdx = oAccessor.get(index);
      int length = oAccessor.get(index + 1) - startIdx;
      assert length >= 0;
      byte[] dst = new byte[length];
      data.getBytes(startIdx, dst, 0, length);
      return dst;
    }

    @Override
    public int getValueLength(int index) {
      UInt${type.width}Vector.Accessor offsetVectorAccessor = offsetVector.getAccessor();
      return offsetVectorAccessor.get(index + 1) - offsetVectorAccessor.get(index);
    }

    public void get(int index, ${minor.class}Holder holder){
      holder.start = oAccessor.get(index);
      holder.end = oAccessor.get(index + 1);
      holder.buffer = data;
      <#if minor.class.contains("Decimal")>
      holder.scale = field.getScale();
      holder.precision = field.getPrecision();
      </#if>
    }

    public void get(int index, Nullable${minor.class}Holder holder){
      holder.isSet = 1;
      holder.start = oAccessor.get(index);
      holder.end = oAccessor.get(index + 1);
      holder.buffer = data;
      <#if minor.class.contains("Decimal")>
      holder.scale = field.getScale();
      holder.precision = field.getPrecision();
      </#if>
    }

    <#switch minor.class>
    <#case "VarDecimal">
    @Override
    public ${friendlyType} getObject(int index) {
      byte[] b = get(index);
      BigInteger bi = b.length == 0 ? BigInteger.ZERO : new BigInteger(b);
      BigDecimal bd = new BigDecimal(bi, getField().getScale());
      return bd;
    }
    <#break>
    <#case "VarChar">
    @Override
    public ${friendlyType} getObject(int index) {
      Text text = new Text();
      text.set(get(index));
      return text;
    }
    <#break>
    <#case "Var16Char">
    @Override
    public ${friendlyType} getObject(int index) {
      return new String(get(index), Charsets.UTF_16);
    }
    <#break>
    <#default>
    @Override
    public ${friendlyType} getObject(int index) {
      return get(index);
    }
    </#switch>

    @Override
    public int getValueCount() {
      return Math.max(offsetVector.getAccessor().getValueCount()-1, 0);
    }

    @Override
    public boolean isNull(int index){
      return false;
    }

    public UInt${type.width}Vector getOffsetVector(){
      return offsetVector;
    }
  }

  /**
   * <h4>Overview</h4>
   * <p>
   * Mutable${minor.class} implements a vector of variable width values.  Elements in the vector
   * are accessed by position from the logical start of the vector.  A fixed width offsetVector
   * is used to convert an element's position to it's offset from the start of the (0-based)
   * DrillBuf.  Size is inferred by adjacent elements.
   *   The width of each element is ${type.width} byte(s)
   *   The equivalent Java primitive is '${minor.javaType!type.javaType}'
   *
   * NB: this class is automatically generated from ValueVectorTypes.tdd using FreeMarker.
   * </p>
   * <h4>Contract</h4>
   * <p>
   *   <ol>
   *     <li>
   *       <b>Supported Writes:</b> {@link VariableWidthVector}s do not support random writes. In contrast {@link org.apache.drill.exec.vector.FixedWidthVector}s do
   *       allow random writes but special care is needed.
   *       </li>
   *     <li>
   *       <b>Writing Values:</b> All set methods must be called with a consecutive sequence of indices. With a few exceptions:
   *       <ol>
   *         <li>You can update the last index you just set.</li>
   *         <li>You can reset a previous index (call it Idx), but you must assume all the data after Idx is corrupt. Also
   *         note that the memory consumed by data that came after Idx is not released.</li>
   *       </ol>
   *     </li>
   *     <li>
   *       <b>Setting Value Count:</b> Vectors aren't explicitly aware of how many values they contain. So you must keep track of the
   *       number of values you've written to the vector and once you are done writing to the vector you must call {@link Mutator#setValueCount(int)}.
   *       It is possible to trim the vector by setting the value count to be less than the number of values currently contained in the vector. Note the extra memory consumed in
   *       the data buffer is not freed when this is done.
   *     </li>
   *     <li>
   *       <b>Memory Allocation:</b> When setting a value at an index you must do one of the following to ensure you do not get an {@link IndexOutOfBoundsException}.
   *       <ol>
   *         <li>
   *           Allocate the exact amount of memory you need when using the {@link Mutator#set(int, byte[])} methods. If you do not
   *           manually allocate sufficient memory an {@link IndexOutOfBoundsException} can be thrown when the data buffer runs out of space.
   *         </li>
   *         <li>
   *           Or you can use the {@link Mutator#setSafe(int, byte[])} methods, which will automatically grow your data buffer to
   *           fit your data.
   *         </li>
   *       </ol>
   *     </li>
   *     <li>
   *       <b>Immutability:</b> Once a vector has been populated with data and {@link #setValueCount(int)} has been called, it should be considered immutable.
   *     </li>
   *   </ol>
   * </p>
   */
  public final class Mutator extends BaseValueVector.BaseMutator implements VariableWidthVector.VariableWidthMutator {

    /**
     * Set the variable length element at the specified index to the supplied byte array.
     *
     * @param index   position of the bit to set
     * @param bytes   array of bytes to write
     */
    protected void set(int index, byte[] bytes) {
      assert index >= 0;
      int currentOffset = offsetVector.getAccessor().get(index);
      offsetVector.getMutator().set(index + 1, currentOffset + bytes.length);
      data.setBytes(currentOffset, bytes, 0, bytes.length);
    }

    public void setSafe(int index, byte[] bytes) {
      assert index >= 0;

      int currentOffset = offsetVector.getAccessor().get(index);
      while (data.capacity() < currentOffset + bytes.length) {
        reAlloc();
      }
      offsetVector.getMutator().setSafe(index + 1, currentOffset + bytes.length);
      data.setBytes(currentOffset, bytes, 0, bytes.length);
    }

    /**
     * Copies the bulk input into this value vector and extends its capacity if necessary.
     * @param input bulk input
     */
    public <T extends VarLenBulkEntry> void setSafe(VarLenBulkInput<T> input) {
      setSafe(input, null);
    }

    /**
     * Copies the bulk input into this value vector and extends its capacity if necessary. The callback
     * mechanism allows decoration as caller is invoked for each bulk entry.
     *
     * @param input bulk input
     * @param callback a bulk input callback object (optional)
     */
    public <T extends VarLenBulkEntry> void setSafe(VarLenBulkInput<T> input, VarLenBulkInput.BulkInputCallback<T> callback) {
      // Let's allocate a buffered mutator to optimize memory copy performance
      BufferedMutator bufferedMutator = new BufferedMutator(input.getStartIndex(), ${minor.class}Vector.this);

      // Let's process the input
      while (input.hasNext()) {
        T entry = input.next();

        if (entry == null || entry.getNumValues() == 0) {
          break; // this could happen when handling columnar batch sizing constraints
        }
        bufferedMutator.setSafe(entry);

        if (callback != null) {
          callback.onNewBulkEntry(entry);
        }

        DrillRuntimeException.checkInterrupted(); // Ensures fast handling of query cancellation
      }

      // Flush any data not yet copied to this VL container
      bufferedMutator.flush();

      // Inform the input object we're done reading
      input.done();

      if (callback != null) {
        callback.onEndBulkInput();
      }
    }

    /**
     * Set the variable length element at the specified index to the supplied byte array.
     *
     * @param index   position of the bit to set
     * @param bytes   array of bytes to write
     * @param start   start index of bytes to write
     * @param length  length of bytes to write
     */
    protected void set(int index, byte[] bytes, int start, int length) {
      assert index >= 0;
      int currentOffset = offsetVector.getAccessor().get(index);
      offsetVector.getMutator().set(index + 1, currentOffset + length);
      data.setBytes(currentOffset, bytes, start, length);
    }

    public void setSafe(int index, ByteBuffer bytes, int start, int length) {
      assert index >= 0;

      int currentOffset = offsetVector.getAccessor().get(index);

      while (data.capacity() < currentOffset + length) {
        reAlloc();
      }
      offsetVector.getMutator().setSafe(index + 1, currentOffset + length);
      data.setBytes(currentOffset, bytes, start, length);
    }

    public void setSafe(int index, byte[] bytes, int start, int length) {
      assert index >= 0;

      int currentOffset = offsetVector.getAccessor().get(index);

      while (data.capacity() < currentOffset + length) {
        reAlloc();
      }
      offsetVector.getMutator().setSafe(index + 1, currentOffset + length);
      data.setBytes(currentOffset, bytes, start, length);
    }

    @Override
    public void setValueLengthSafe(int index, int length) {
      int offset = offsetVector.getAccessor().get(index);
      while(data.capacity() < offset + length ) {
        reAlloc();
      }
      offsetVector.getMutator().setSafe(index + 1, offsetVector.getAccessor().get(index) + length);
    }

    public void setSafe(int index, int start, int end, DrillBuf buffer) {
      int len = end - start;
      int outputStart = offsetVector.data.get${(minor.javaType!type.javaType)?cap_first}(index * ${type.width});

      while (data.capacity() < outputStart + len) {
        reAlloc();
      }
      offsetVector.getMutator().setSafe(index + 1, outputStart + len);
      buffer.getBytes(start, data, outputStart, len);
    }

    public void setSafe(int index, Nullable${minor.class}Holder holder) {
      assert holder.isSet == 1;

      int start = holder.start;
      int end =   holder.end;
      int len = end - start;

      int outputStart = offsetVector.data.get${(minor.javaType!type.javaType)?cap_first}(index * ${type.width});

      while (data.capacity() < outputStart + len) {
        reAlloc();
      }
      holder.buffer.getBytes(start, data, outputStart, len);
      offsetVector.getMutator().setSafe(index + 1, outputStart + len);
    }

    <#if minor.class == "VarDecimal">
    public void set(int index, BigDecimal value) {
      byte[] bytes = value.unscaledValue().toByteArray();
      set(index, bytes, 0, bytes.length);
    }

    public void setSafe(int index, BigDecimal value) {
      byte[] bytes = value.unscaledValue().toByteArray();
      setSafe(index, bytes, 0, bytes.length);
    }
    </#if>

    public void setSafe(int index, ${minor.class}Holder holder) {
      int start = holder.start;
      int end =   holder.end;
      int len = end - start;
      int outputStart = offsetVector.data.get${(minor.javaType!type.javaType)?cap_first}(index * ${type.width});

      while(data.capacity() < outputStart + len) {
        reAlloc();
      }
      holder.buffer.getBytes(start, data, outputStart, len);
      offsetVector.getMutator().setSafe(index + 1, outputStart + len);
    }

    /**
     * Backfill missing offsets from the given last written position up to, but
     * not including the given current write position. Used by nullable
     * vectors to allow skipping values. The <tt>set()</tt> and
     * <tt>setSafe()</tt> <b>do not</b> fill empties. See DRILL-5529.
     * 
     * @param lastWrite
     *          the position of the last valid write: the offset to be copied
     *          forward
     * @param index
     *          the current write position filling occurs up to, but not
     *          including, this position
     */

    public void fillEmpties(int lastWrite, int index) {

      // If last write was 2, offsets are [0, 3, 6]
      // If next write is 4, offsets must be: [0, 3, 6, 6, 6]
      // Remember the offsets are one more than row count.

      int startWrite = lastWrite + 1;
      if (startWrite < index) {

        // Don't access the offset vector if nothing to fill.
        // This handles the special case of a zero-size batch
        // in which the 0th position of the offset vector does
        // not even exist.

        int fillOffset = offsetVector.getAccessor().get(startWrite);
        UInt4Vector.Mutator offsetMutator = offsetVector.getMutator();
        for (int i = startWrite; i < index; i++) {
          offsetMutator.setSafe(i + 1, fillOffset);
        }
      }
    }

    protected void set(int index, int start, int length, DrillBuf buffer){
      assert index >= 0;
      int currentOffset = offsetVector.getAccessor().get(index);
      offsetVector.getMutator().set(index + 1, currentOffset + length);
      DrillBuf bb = buffer.slice(start, length);
      data.setBytes(currentOffset, bb);
    }

    protected void set(int index, Nullable${minor.class}Holder holder){
      int length = holder.end - holder.start;
      int currentOffset = offsetVector.getAccessor().get(index);
      offsetVector.getMutator().set(index + 1, currentOffset + length);
      data.setBytes(currentOffset, holder.buffer, holder.start, length);
    }

    protected void set(int index, ${minor.class}Holder holder){
      int length = holder.end - holder.start;
      int currentOffset = offsetVector.getAccessor().get(index);
      offsetVector.getMutator().set(index + 1, currentOffset + length);
      data.setBytes(currentOffset, holder.buffer, holder.start, length);
    }

    /**
     * <h4>Notes on Usage</h4>
     * <p>
     * For {@link VariableWidthVector}s this method can be used in the following
     * cases:
     * <ul>
     * <li>Setting the actual number of elements currently contained in the
     * vector.</li>
     * <li>Trimming the vector to have fewer elements than it current does.</li>
     * </ul>
     * </p>
     * <h4>Caveats</h4>
     * <p>
     * It is important to note that for
     * {@link org.apache.drill.exec.vector.FixedWidthVector}s this method can
     * also be used to expand the vector. However, {@link VariableWidthVector}
     * do not support this usage and this method will throw an
     * {@link IndexOutOfBoundsException} if you attempt to use it in this way.
     * Expansion of valueCounts is not supported mainly because there is no
     * benefit, since you would still have to rely on the setSafe methods to
     * appropriately expand the data buffer and populate the vector anyway
     * (since by definition we do not know the width of elements). See
     * DRILL-6234 for details.
     * </p>
     * <h4>Method Documentation</h4> {@inheritDoc}
     */
    @Override
    public void setValueCount(int valueCount) {
      int currentByteCapacity = getByteCapacity();
      // Check if valueCount to be set is zero and current capacity is also zero. If yes then
      // we should not call get to read start index from offset vector at that value count.
      int idx = (valueCount == 0 && currentByteCapacity == 0)
        ? 0
        : offsetVector.getAccessor().get(valueCount);
      data.writerIndex(idx);
      if (valueCount > 0 && currentByteCapacity > idx * 2) {
        incrementAllocationMonitor();
      } else if (allocationMonitor > 0) {
        allocationMonitor = 0;
      }
      offsetVector.getMutator().setValueCount(valueCount == 0 ? 0 : valueCount+1);
    }

    @Override
    public void generateTestData(int size){
      boolean even = true;
      <#switch minor.class>
      <#case "Var16Char">
      java.nio.charset.Charset charset = Charsets.UTF_16;
      <#break>
      <#case "VarChar">
      <#default>
      java.nio.charset.Charset charset = Charsets.UTF_8;
      </#switch>
      byte[] evenValue = new String("aaaaa").getBytes(charset);
      byte[] oddValue = new String("bbbbbbbbbb").getBytes(charset);
      for(int i =0; i < size; i++, even = !even){
        set(i, even ? evenValue : oddValue);
        }
      setValueCount(size);
    }
  }

  /**
   * Helper class to buffer container mutation as a means to optimize native memory copy operations. Ideally, this
   * should be done transparently as part of the Mutator and Accessor APIs.
   *
   * NB: this class is automatically generated from ValueVectorTypes.tdd using FreeMarker.
   */
  public static final class BufferedMutator {
    /** The default buffer size */
    private static final int DEFAULT_BUFF_SZ = 1024 << 2;
    /** Byte buffer */
    private final ByteBuffer buffer;
    /** Indicator on whether to enable data buffering */
    private final boolean enableDataBuffering = false;
    /** Current offset within the data buffer */
    private int dataBuffOff;
    /** Total data length (contained within data and buffer) */
    private int totalDataLen;
    /** Parent container */
    private final ${minor.class}Vector parent;
    /** A buffered mutator to the offsets vector */
    private final UInt4Vector.BufferedMutator offsetsMutator;

    /** @see {@link #BufferedMutator(int startIdx, int buffSz, ${minor.class}Vector parent)} */
    public BufferedMutator(int startIdx, ${minor.class}Vector parent) {
      this(startIdx, DEFAULT_BUFF_SZ, parent);
    }

    /**
     * Buffered mutator to optimize bulk access to the underlying vector container
     * @param startIdx start idex of the first value to be copied
     * @param buffSz buffer length to us
     * @param parent parent container object
     */
    public BufferedMutator(int startIdx, int buffSz, ${minor.class}Vector parent) {
      if (enableDataBuffering) {
        this.buffer = ByteBuffer.allocate(buffSz);
        // set the buffer to the native byte order
        this.buffer.order(ByteOrder.nativeOrder());
      } else {
        this.buffer = null;
      }

      this.parent = parent;
      this.dataBuffOff = this.parent.offsetVector.getAccessor().get(startIdx);
      this.totalDataLen = this.dataBuffOff;
      this.offsetsMutator = new UInt4Vector.BufferedMutator(startIdx, buffSz, parent.offsetVector);

      // Forcing the offsetsMutator to operate at index+1
      this.offsetsMutator.setSafe(this.dataBuffOff);
    }

    public void setSafe(VarLenBulkEntry bulkEntry) {
      // The new entry doesn't fit in remaining space
      if (enableDataBuffering && buffer.remaining() < bulkEntry.getTotalLength()) {
        flushInternal();
      }

      // Now update the offsets vector with new information
      int[] lengths = bulkEntry.getValuesLength();
      int numValues = bulkEntry.getNumValues();

      setOffsets(lengths, numValues, bulkEntry.hasNulls());

      // Now we're able to buffer the new bulk entry
      if (enableDataBuffering && buffer.remaining() >= bulkEntry.getTotalLength() && bulkEntry.arrayBacked()) {
        buffer.put(bulkEntry.getArrayData(), bulkEntry.getDataStartOffset(), bulkEntry.getTotalLength());

      } else {
        // The new entry is larger than the buffer (note at this point we know the buffer has been flushed)
        while (parent.data.capacity() < totalDataLen) {
          parent.reAlloc();
        }

        if (bulkEntry.arrayBacked()) {
          parent.data.setBytes(dataBuffOff,
            bulkEntry.getArrayData(),
            bulkEntry.getDataStartOffset(),
            bulkEntry.getTotalLength());

        } else {
          parent.data.setBytes(dataBuffOff,
            bulkEntry.getData(),
            bulkEntry.getDataStartOffset(),
            bulkEntry.getTotalLength());
        }

        // Update the underlying DrillBuf offset
        dataBuffOff += bulkEntry.getTotalLength();
      }
    }

    public void flush() {
      flushInternal();
      offsetsMutator.flush();
    }

    private void flushInternal() {
      if (!enableDataBuffering) {
        return; // NOOP
      }
      int numElements = buffer.position();

      if (numElements == 0) {
        return; // NOOP
      }

      while (parent.data.capacity() < totalDataLen) {
        parent.reAlloc();
      }

      try {
        parent.data.setBytes(dataBuffOff, buffer.array(), 0, buffer.position());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      // Update counters
      dataBuffOff += buffer.position();
      assert dataBuffOff == totalDataLen;

      // Reset the byte buffer
      buffer.clear();
    }

    private void setOffsets(int[] lengths, int numValues, boolean hasNulls) {
      // We need to compute source offsets using the current larget offset and the value length array.
      ByteBuffer offByteBuff = offsetsMutator.getByteBuffer();
      byte[] bufferArray = offByteBuff.array();
      int remaining = numValues;
      int srcPos = 0;

      do {
        if (offByteBuff.remaining() < 4) {
          offsetsMutator.flush();
        }

        int toCopy  = Math.min(remaining, offByteBuff.remaining() / 4);
        int tgtPos       = offByteBuff.position();

        if (!hasNulls) {
          for (int idx = 0; idx < toCopy; idx++, tgtPos += 4, srcPos++) {
            totalDataLen += lengths[srcPos];
            UInt4Vector.BufferedMutator.writeInt(totalDataLen, bufferArray, tgtPos);
          }
        } else {
          for (int idx = 0; idx < toCopy; idx++, tgtPos += 4, srcPos++) {
            int curr_len = lengths[srcPos];
            totalDataLen    += (curr_len >= 0) ? curr_len : 0;
            UInt4Vector.BufferedMutator.writeInt(totalDataLen, bufferArray, tgtPos);
          }
        }

        // Update counters
        offByteBuff.position(tgtPos);
        remaining -= toCopy;

      } while (remaining > 0);

      // We need to flush as offset data can be accessed during loading to
      // figure out current payload size.
      offsetsMutator.flush();

    }
  }
}
</#if> <#-- type.major -->
</#list>
</#list>
