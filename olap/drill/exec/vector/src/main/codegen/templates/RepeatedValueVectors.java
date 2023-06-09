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

import org.apache.drill.common.types.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.vector.complex.BaseRepeatedValueVector;
import org.mortbay.jetty.servlet.Holder;

<@pp.dropOutputFile />
<#list vv.types as type>
<#list type.minor as minor>
<#assign friendlyType = (minor.friendlyType!minor.boxedType!type.boxedType) />
<#assign fields = minor.fields!type.fields />

<@pp.changeOutputFile name="/org/apache/drill/exec/vector/Repeated${minor.class}Vector.java" />
<#include "/@includes/license.ftl" />

package org.apache.drill.exec.vector;

<#include "/@includes/vv_imports.ftl" />

/**
 * Repeated${minor.class} implements a vector with multiple values per row (e.g. JSON array or
 * repeated protobuf field).  The implementation uses an additional value vectors to convert
 * the index offset to the underlying element offset. The count of values comes from subtracting
 * two successive offsets.
 *
 * NB: this class is automatically generated from ${.template_name} and ValueVectorTypes.tdd using FreeMarker.
 */

public final class Repeated${minor.class}Vector extends BaseRepeatedValueVector implements Repeated<#if type.major == "VarLen">VariableWidth<#else>FixedWidth</#if>VectorLike {
  //private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Repeated${minor.class}Vector.class);

  // we maintain local reference to concrete vector type for performance reasons.
  private ${minor.class}Vector values;
  private final FieldReader reader = new Repeated${minor.class}ReaderImpl(Repeated${minor.class}Vector.this);
  private final Mutator mutator = new Mutator();
  private final Accessor accessor = new Accessor();

  public Repeated${minor.class}Vector(MaterializedField field, BufferAllocator allocator) {
    super(field, allocator);
    MajorType majorType = field.getType();
    addOrGetVector(VectorDescriptor.create(Types.withPrecisionAndScale(
        majorType.getMinorType(), DataMode.REQUIRED,
        majorType.getPrecision(),
        majorType.getScale())));
  }

  @Override
  public Mutator getMutator() {
    return mutator;
  }

  @Override
  public Accessor getAccessor() {
    return accessor;
  }

  @Override
  public FieldReader getReader() {
    return reader;
  }

  @Override
  public ${minor.class}Vector getDataVector() {
    return values;
  }

  @Override
  public TransferPair getTransferPair(BufferAllocator allocator) {
    return new TransferImpl(getField(), allocator);
  }

  @Override
  public TransferPair getTransferPair(String ref, BufferAllocator allocator){
    return new TransferImpl(getField().withPath(ref), allocator);
  }

  @Override
  public TransferPair makeTransferPair(ValueVector to) {
    return new TransferImpl((Repeated${minor.class}Vector) to);
  }

  @Override
  public AddOrGetResult<${minor.class}Vector> addOrGetVector(VectorDescriptor descriptor) {
    final AddOrGetResult<${minor.class}Vector> result = super.addOrGetVector(descriptor);
    if (result.isCreated()) {
      values = result.getVector();
    }
    return result;
  }

  public void transferTo(Repeated${minor.class}Vector target) {
    target.clear();
    offsets.transferTo(target.offsets);
    values.transferTo(target.values);
    clear();
  }

  public void splitAndTransferTo(final int startIndex, final int groups, Repeated${minor.class}Vector to) {
    final UInt4Vector.Accessor a = offsets.getAccessor();
    final UInt4Vector.Mutator m = to.offsets.getMutator();

    final int startPos = a.get(startIndex);
    final int endPos = a.get(startIndex + groups);
    final int valuesToCopy = endPos - startPos;

    values.splitAndTransferTo(startPos, valuesToCopy, to.values);
    to.offsets.clear();
    to.offsets.allocateNew(groups + 1);
    int normalizedPos = 0;
    for (int i=0; i < groups + 1;i++ ) {
      normalizedPos = a.get(startIndex+i) - startPos;
      m.set(i, normalizedPos);
    }
    m.setValueCount(groups == 0 ? 0 : groups + 1);
  }

  private class TransferImpl implements TransferPair {
    final Repeated${minor.class}Vector to;

    public TransferImpl(MaterializedField field, BufferAllocator allocator) {
      this.to = new Repeated${minor.class}Vector(field, allocator);
    }

    public TransferImpl(Repeated${minor.class}Vector to) {
      this.to = to;
    }

    @Override
    public Repeated${minor.class}Vector getTo() {
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
      to.copyFromSafe(fromIndex, toIndex, Repeated${minor.class}Vector.this);
    }
  }

  public void copyFrom(int inIndex, int outIndex, Repeated${minor.class}Vector v) {
    final Accessor vAccessor = v.getAccessor();
    final int count = vAccessor.getInnerValueCountAt(inIndex);
    mutator.startNewValue(outIndex);
    for (int i = 0; i < count; i++) {
      mutator.add(outIndex, vAccessor.get(inIndex, i));
    }
    mutator.setValueCount(outIndex+1);
  }

  public void copyFromSafe(int inIndex, int outIndex, Repeated${minor.class}Vector v) {
    final Accessor vAccessor = v.getAccessor();
    final int count = vAccessor.getInnerValueCountAt(inIndex);
    mutator.startNewValue(outIndex);
    for (int i = 0; i < count; i++) {
      mutator.addSafe(outIndex, vAccessor.get(inIndex, i));
    }
    mutator.setValueCount(outIndex+1);
  }

  @Override
  public void copyEntry(int toIndex, ValueVector from, int fromIndex) {
    copyFromSafe(fromIndex, toIndex, (Repeated${minor.class}Vector) from);
  }

  @Override
  public boolean allocateNewSafe() {
    /* boolean to keep track if all the memory allocations were successful.
     * Used in the case of composite vectors when we need to allocate multiple
     * buffers for multiple vectors. If one of the allocations failed we need to
     * clear all the memory that we allocated.
     */
    boolean success = false;
    try {
      if(!offsets.allocateNewSafe()) return false;
      if(!values.allocateNewSafe()) return false;
      success = true;
    } finally {
      if (!success) {
        clear();
      }
    }
    offsets.zeroVector();
    mutator.reset();
    return true;
  }

  @Override
  public void allocateNew() {
    try {
      offsets.allocateNew();
      values.allocateNew();
    } catch (OutOfMemoryException e) {
      clear();
      throw e;
    }
    offsets.zeroVector();
    mutator.reset();
  }

  <#if type.major == "VarLen">
  @Override
  protected SerializedField.Builder getMetadataBuilder() {
    return super.getMetadataBuilder()
            .setVarByteLength(values.getCurrentSizeInBytes());
  }

  @Override
  public void allocateNew(int totalBytes, int valueCount, int innerValueCount) {
    try {
      offsets.allocateNew(valueCount + 1);
      values.allocateNew(totalBytes, innerValueCount);
    } catch (OutOfMemoryException e) {
      clear();
      throw e;
    }
    offsets.zeroVector();
    mutator.reset();
  }

  @Override
  public int getByteCapacity(){
    return values.getByteCapacity();
  }

  <#else>
  @Override
  public void allocateNew(int valueCount, int innerValueCount) {
    clear();
    try {
      offsets.allocateNew(valueCount + 1);
      values.allocateNew(innerValueCount);
    } catch(OutOfMemoryException e){
      clear();
      throw e;
    }
    offsets.zeroVector();
    mutator.reset();
  }

  </#if>
  // This is declared a subclass of the accessor declared inside of FixedWidthVector, this is also used for
  // variable length vectors, as they should have a consistent interface as much as possible, if they need to diverge
  // in the future, the interface shold be declared in the respective value vector superclasses for fixed and variable
  // and we should refer to each in the generation template.
  public final class Accessor extends BaseRepeatedValueVector.BaseRepeatedAccessor {
    @Override
    public List<${friendlyType}> getObject(int index) {
      final List<${friendlyType}> vals = new JsonStringArrayList<>();
      final UInt4Vector.Accessor offsetsAccessor = offsets.getAccessor();
      final int start = offsetsAccessor.get(index);
      final int end = offsetsAccessor.get(index + 1);
      final ${minor.class}Vector.Accessor valuesAccessor = values.getAccessor();
      for(int i = start; i < end; i++) {
        vals.add(valuesAccessor.getObject(i));
      }
      return vals;
    }

    public ${friendlyType} getSingleObject(int index, int arrayIndex) {
      final int start = offsets.getAccessor().get(index);
      return values.getAccessor().getObject(start + arrayIndex);
    }

    /**
     * Get a value for the given record. Each element in the repeated field is accessed by
     * the positionIndex param.
     *
     * @param  index           record containing the repeated field
     * @param  positionIndex   position within the repeated field
     * @return element at the given position in the given record
     */
    public <#if type.major == "VarLen">byte[]
           <#else>${minor.javaType!type.javaType}
           </#if> get(int index, int positionIndex) {
      return values.getAccessor().get(offsets.getAccessor().get(index) + positionIndex);
    }

    public void get(int index, Repeated${minor.class}Holder holder) {
      holder.start = offsets.getAccessor().get(index);
      holder.end =  offsets.getAccessor().get(index+1);
      holder.vector = values;
      holder.reader = reader;
    }

    public void get(int index, int positionIndex, ${minor.class}Holder holder) {
      final int offset = offsets.getAccessor().get(index);
      assert offset >= 0;
      assert positionIndex < getInnerValueCountAt(index);
      values.getAccessor().get(offset + positionIndex, holder);
    }

    public void get(int index, int positionIndex, Nullable${minor.class}Holder holder) {
      final int offset = offsets.getAccessor().get(index);
      assert offset >= 0;
      if (positionIndex >= getInnerValueCountAt(index)) {
        holder.isSet = 0;
        return;
      }
      values.getAccessor().get(offset + positionIndex, holder);
    }
  }

  public final class Mutator extends BaseRepeatedValueVector.BaseRepeatedMutator implements RepeatedMutator {
    private Mutator() {}

    /**
     * Add an element to the given record index.  This is similar to the set() method in other
     * value vectors, except that it permits setting multiple values for a single record.
     *
     * @param index   record of the element to add
     * @param value   value to add to the given row
     */
    public void add(int index, <#if type.major == "VarLen">byte[]<#elseif (type.width < 4)>int<#else>${minor.javaType!type.javaType}</#if> value) {
      final int nextOffset = offsets.getAccessor().get(index+1);
      values.getMutator().set(nextOffset, value);
      offsets.getMutator().set(index+1, nextOffset+1);
    }

    <#if type.major == "VarLen">
    public void addSafe(int index, byte[] bytes) {
      addSafe(index, bytes, 0, bytes.length);
    }

    public void addSafe(int index, byte[] bytes, int start, int length) {
      final int nextOffset = offsets.getAccessor().get(index+1);
      values.getMutator().setSafe(nextOffset, bytes, start, length);
      offsets.getMutator().setSafe(index+1, nextOffset+1);
    }

    <#else>
    public void addSafe(int index, ${minor.javaType!type.javaType} srcValue) {
      final int nextOffset = offsets.getAccessor().get(index+1);
      values.getMutator().setSafe(nextOffset, srcValue);
      offsets.getMutator().setSafe(index+1, nextOffset+1);
    }

    </#if>
    public void setSafe(int index, Repeated${minor.class}Holder h) {
      final ${minor.class}Holder ih = new ${minor.class}Holder();
      final ${minor.class}Vector.Accessor hVectorAccessor = h.vector.getAccessor();
      mutator.startNewValue(index);
      for(int i = h.start; i < h.end; i++){
        hVectorAccessor.get(i, ih);
        mutator.addSafe(index, ih);
      }
    }

    public void addSafe(int index, ${minor.class}Holder holder) {
      final int nextOffset = offsets.getAccessor().get(index+1);
      values.getMutator().setSafe(nextOffset, holder);
      offsets.getMutator().setSafe(index+1, nextOffset+1);
    }


    public void addSafe(int index, Nullable${minor.class}Holder holder) {
      final int nextOffset = offsets.getAccessor().get(index+1);
      values.getMutator().setSafe(nextOffset, holder);
      offsets.getMutator().setSafe(index+1, nextOffset+1);
    }

    /**
     * Backfill missing offsets from the given last written position to the
     * given current write position. Used by the "new" size-safe column
     * writers to allow skipping values. The <tt>set()</tt> and <tt>setSafe()</tt>
     * <b>do not</b> fill empties. See DRILL-5529.
     * @param lastWrite the position of the last valid write: the offset
     * to be copied forward
     * @param index the current write position to be initialized
     */

    public void fillEmpties(int lastWrite, int index) {
      // If last write was 2, offsets are [0, 3, 6]
      // If next write is 4, offsets must be: [0, 3, 6, 6, 6]
      // Remember the offsets are one more than row count.
      final int fillOffset = offsets.getAccessor().get(lastWrite+1);
      final UInt4Vector.Mutator offsetMutator = offsets.getMutator();
      for (int i = lastWrite; i < index; i++) {
        offsetMutator.setSafe(i + 1, fillOffset);
      }
    }

    <#if (fields?size > 1) && !(minor.class == "Decimal9" || minor.class == "Decimal18"
        || minor.class == "Decimal28Sparse" || minor.class == "Decimal38Sparse" || minor.class == "Decimal28Dense"
        || minor.class == "Decimal38Dense") || minor.class == "VarDecimal">
    public void addSafe(int rowIndex<#list fields as field><#if field.include!true>, ${field.type} ${field.name}</#if></#list>) {
      final int nextOffset = offsets.getAccessor().get(rowIndex+1);
      values.getMutator().setSafe(nextOffset<#list fields as field><#if field.include!true>, ${field.name}</#if></#list>);
      offsets.getMutator().setSafe(rowIndex + 1, nextOffset + 1);
    }

    </#if>
    <#if minor.class == "Decimal28Sparse" || minor.class == "Decimal38Sparse">
    public void addSafe(int index, BigDecimal value) {
      final int nextOffset = offsets.getAccessor().get(index+1);
      values.getMutator().setSafe(nextOffset, value);
      offsets.getMutator().setSafe(index+1, nextOffset+1);
    }

    </#if>
    protected void add(int index, ${minor.class}Holder holder) {
      final int nextOffset = offsets.getAccessor().get(index+1);
      values.getMutator().set(nextOffset, holder);
      offsets.getMutator().set(index+1, nextOffset+1);
    }

    public void add(int index, Repeated${minor.class}Holder holder) {

      final ${minor.class}Vector.Accessor accessor = holder.vector.getAccessor();
      final ${minor.class}Holder innerHolder = new ${minor.class}Holder();

      for(int i = holder.start; i < holder.end; i++) {
        accessor.get(i, innerHolder);
        add(index, innerHolder);
      }
    }

    @Override
    public void generateTestData(final int valCount) {
      final int[] sizes = {1, 2, 0, 6};
      int size = 0;
      int runningOffset = 0;
      final UInt4Vector.Mutator offsetsMutator = offsets.getMutator();
      for(int i = 1; i < valCount + 1; i++, size++) {
        runningOffset += sizes[size % sizes.length];
        offsetsMutator.set(i, runningOffset);
      }
      values.getMutator().generateTestData(valCount * 9);
      setValueCount(size);
    }

    @Override
    public void reset() {
    }
  }
}
</#list>
</#list>
