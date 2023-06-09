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

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.expr.holders.RepeatedDictHolder;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.util.CallBack;
import org.apache.drill.exec.util.JsonStringArrayList;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.complex.impl.RepeatedDictReaderImpl;
import org.apache.drill.exec.vector.complex.reader.FieldReader;

import java.util.List;

public class RepeatedDictVector extends BaseRepeatedValueVector {

  public final static TypeProtos.MajorType TYPE = Types.repeated(TypeProtos.MinorType.DICT);

  private final static String DICT_VECTOR_NAME = "$inner$";
  private  final static MaterializedField DICT_VECTOR_FIELD =
      MaterializedField.create(DICT_VECTOR_NAME, DictVector.TYPE);

  private final Accessor accessor = new Accessor();
  private final Mutator mutator = new Mutator();
  private final FieldReader reader = new RepeatedDictReaderImpl(this);
  private final EmptyValuePopulator emptyPopulator;

  public RepeatedDictVector(String path, BufferAllocator allocator) {
    this(MaterializedField.create(path, TYPE), allocator, null);
  }

  public RepeatedDictVector(MaterializedField field, BufferAllocator allocator, CallBack callback) {
    super(field, allocator, new DictVector(DICT_VECTOR_FIELD, allocator, callback));
    emptyPopulator = new EmptyValuePopulator(getOffsetVector());
  }

  @Override
  public void allocateNew() throws OutOfMemoryException {
    if (!allocateNewSafe()) {
      throw new OutOfMemoryException();
    }
  }

  @Override
  public TransferPair getTransferPair(String ref, BufferAllocator allocator) {
    return makeTransferPair(new RepeatedDictVector(ref, allocator));
  }

  @Override
  public TransferPair makeTransferPair(ValueVector target) {
    return new RepeatedDictTransferPair((RepeatedDictVector) target);
  }

  public class RepeatedDictTransferPair extends BaseRepeatedValueVectorTransferPair<RepeatedDictVector> {

    public RepeatedDictTransferPair(RepeatedDictVector target) {
      super(target);
    }

    @Override
    public void copyValueSafe(int srcIndex, int destIndex) {
      final RepeatedDictHolder holder = new RepeatedDictHolder();
      getAccessor().get(srcIndex, holder);
      target.emptyPopulator.populate(destIndex+1);
      copyValueSafe(destIndex, holder.start, holder.end);
    }
  }

  @Override
  public MaterializedField getField() {
    MaterializedField field = this.field.clone();
    field.addChild(vector.getField());
    return field;
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
  public void copyEntry(int toIndex, ValueVector from, int fromIndex) {
    RepeatedDictTransferPair pair = (RepeatedDictTransferPair) from.makeTransferPair(this);
    pair.copyValueSafe(fromIndex, toIndex);
  }

  public class Accessor extends BaseRepeatedValueVector.BaseRepeatedAccessor {

    @Override
    public Object getObject(int index) {

      List<Object> list = new JsonStringArrayList<>();
      int start = offsets.getAccessor().get(index);
      int end = offsets.getAccessor().get(index + 1);
      for (int i = start; i < end; i++) {
        list.add(vector.getAccessor().getObject(i));
      }
      return list;
    }

    public void get(int index, RepeatedDictHolder holder) {
      int valueCapacity = getValueCapacity();
      assert index < valueCapacity :
        String.format("Attempted to access index %d when value capacity is %d", index, valueCapacity);

      holder.vector = RepeatedDictVector.this;
      holder.reader = reader;
      holder.start = getOffsetVector().getAccessor().get(index);
      holder.end =  getOffsetVector().getAccessor().get(index + 1);
    }
  }

  public class Mutator extends BaseRepeatedValueVector.BaseRepeatedMutator {

    @Override
    public void startNewValue(int index) {
      emptyPopulator.populate(index + 1);
      offsets.getMutator().setSafe(index + 1, offsets.getAccessor().get(index));
    }

    @Override
    public void setValueCount(int topLevelValueCount) {
      emptyPopulator.populate(topLevelValueCount);
      offsets.getMutator().setValueCount(topLevelValueCount == 0 ? 0 : topLevelValueCount + 1);
      int childValueCount = offsets.getAccessor().get(topLevelValueCount);
      vector.getMutator().setValueCount(childValueCount);
    }

    public int add(int index) {
      int prevEnd = offsets.getAccessor().get(index + 1);
      offsets.getMutator().setSafe(index + 1, prevEnd + 1);
      return prevEnd;
    }
  }
}
