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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.expr.holders.RepeatedValueHolder;
import org.apache.drill.exec.expr.holders.DictHolder;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.util.CallBack;
import org.apache.drill.exec.util.JsonStringHashMap;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.complex.impl.SingleDictReaderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ValueVector} holding key-value pairs.
 * <p>This vector is essentially a {@link RepeatedMapVector} but with constraints:
 * it may have 2 children only, named {@link #FIELD_KEY_NAME} and {@link #FIELD_VALUE_NAME}.
 * The {@link #FIELD_KEY_NAME} can be of primitive type only and its values should not be {@code null},
 * while the other, {@link #FIELD_VALUE_NAME}, field can be either of primitive or complex type.
 * Value field can hold {@code null} values.
 *
 * <p>This vector has it's own {@link org.apache.drill.exec.vector.complex.reader.FieldReader} and
 * {@link org.apache.drill.exec.vector.complex.writer.FieldWriter} to ensure data is read and written correctly.
 * In addition, the reader is responsible for getting a value for a given key.
 *
 * <p>Additionally, {@code Object} representation is changed in {@link Accessor#getObject(int)}
 * to represent it as {@link JsonStringHashMap} with appropriate {@code key} and {@code value} types.
 *
 * <p>(The structure corresponds to Java's notion of {@link Map}).
 *
 * @see SingleDictReaderImpl reader corresponding to the vector
 * @see org.apache.drill.exec.vector.complex.impl.SingleDictWriter writer corresponding to the vector
 */
public final class DictVector extends AbstractRepeatedMapVector {

  public final static MajorType TYPE = Types.required(MinorType.DICT);

  public static final String FIELD_KEY_NAME = "key";
  public static final String FIELD_VALUE_NAME = "value";
  public static final List<String> fieldNames = Arrays.asList(FIELD_KEY_NAME, FIELD_VALUE_NAME);

  private static final Logger logger = LoggerFactory.getLogger(DictVector.class);

  private final Accessor accessor = new Accessor();
  private final Mutator mutator = new Mutator();
  private final SingleDictReaderImpl reader = new SingleDictReaderImpl(this);

  private MajorType keyType;
  private MajorType valueType;

  /**
   * Denotes if the value field is nullable. Initialized lazily on first
   * invocation of its getter method {@link #isValueNullable()}.
   */
  private Boolean valueNullable;

  public DictVector(MaterializedField field, BufferAllocator allocator, CallBack callBack) {
    super(field.clone(), allocator, callBack);
  }

  public DictVector(MaterializedField field, BufferAllocator allocator, CallBack callBack, MajorType keyType, MajorType valueType) {
    this(field, allocator, callBack);
    setKeyValueTypes(keyType, valueType);
  }

  @Override
  public SingleDictReaderImpl getReader() {
    return reader;
  }

  @Override
  protected Collection<String> getChildFieldNames() {
    return fieldNames;
  }

  public void transferTo(DictVector target) {
    makeTransferPair(target);
    target.setKeyValueTypes(keyType, valueType);
  }

  public TransferPair makeTransferPair(DictVector to) {
    return new DictTransferPair(to);
  }

  @Override
  public TransferPair getTransferPair(BufferAllocator allocator) {
    return new DictTransferPair(getField().getName(), allocator);
  }

  @Override
  public TransferPair makeTransferPair(ValueVector to) {
    return new DictTransferPair((DictVector) to);
  }

  @Override
  public TransferPair getTransferPair(String ref, BufferAllocator allocator) {
    return new DictTransferPair(ref, allocator);
  }

  private class DictTransferPair extends AbstractRepeatedMapTransferPair<DictVector> {

    DictTransferPair(String path, BufferAllocator allocator) {
      this(new DictVector(MaterializedField.create(path, TYPE), allocator, DictVector.this.callBack), false);
    }

    DictTransferPair(DictVector to) {
      this(to, true);
    }

    DictTransferPair(DictVector to, boolean allocate) {
      super(to, allocate);
      to.keyType = from.keyType;
      to.valueType = from.valueType;
    }
  }

  /**
   * Inserts the vector with the given name if it does not exist else replaces it with the new value.
   * If the vector is replaced, old and new type are expected to be equal.
   * Validates that the {@code name} is either {@link #FIELD_KEY_NAME} or {@link #FIELD_VALUE_NAME} and
   * that key is of primitive type.
   *
   * @param name field name
   * @param vector vector to be added
   * @throws DrillRuntimeException if {@code name} is not equal to {@link #FIELD_KEY_NAME} or {@link #FIELD_VALUE_NAME}
   * or if {@code name.equals(FIELD_KEY_NAME)} and vector is of repeated or complex type.
   */
  @Override
  public void putChild(String name, ValueVector vector) {
    if (!fieldNames.contains(name)) {
      throw new DrillRuntimeException(
          String.format("Unexpected field '%s' added to DictVector: the vector can have '%s' and '%s' children only",
              name, FIELD_KEY_NAME, FIELD_VALUE_NAME)
      );
    }
    MajorType fieldType = vector.getField().getType();
    if (name.equals(FIELD_KEY_NAME)) {

      if (Types.isRepeated(fieldType) || Types.isComplex(fieldType)) {
        throw new DrillRuntimeException("DictVector supports primitive key type only. Found: " + fieldType);
      }

      checkTypes(keyType, fieldType, FIELD_KEY_NAME);
      keyType = fieldType;
    } else {
      checkTypes(valueType, fieldType, FIELD_VALUE_NAME);
      valueType = fieldType;
    }
    super.putChild(name, vector);
  }

  private void checkTypes(MajorType type, MajorType newType, String fieldName) {
    assert type == null || newType.equals(type)
        : String.format("Type mismatch for %s field in DICT: expected '%s' but found '%s'", fieldName, type, newType);
  }

  /**
   * Returns a {@code ValueVector} corresponding to the given field name if exists or null.
   * Expects either {@link #FIELD_KEY_NAME} or {@link #FIELD_VALUE_NAME}.
   *
   * @param name field's name
   */
  @Override
  public ValueVector getChild(String name) {
    assert fieldNames.contains(name) : String.format(
        "DictVector has '%s' and '%s' ValueVectors only", FIELD_KEY_NAME, FIELD_VALUE_NAME);
    return super.getChild(name);
  }

  public class Accessor extends AbstractRepeatedMapVector.Accessor {

    @Override
    public Object getObject(int index) {
      int start = offsets.getAccessor().get(index);
      int end = offsets.getAccessor().get(index + 1);

      ValueVector keys = getKeys();
      ValueVector values = getValues();

      Map<Object, Object> result = new JsonStringHashMap<>();
      for (int i = start; i < end; i++) {
        Object key = keys.getAccessor().getObject(i);
        Object value = values.getAccessor().getObject(i);
        result.put(key, value);
      }
      return result;
    }

    public void get(int index, DictHolder holder) {
      int valueCapacity = getValueCapacity();
      assert index < valueCapacity :
          String.format("Attempted to access index %d when value capacity is %d", index, valueCapacity);

      holder.vector = DictVector.this;
      holder.reader = reader;
      holder.start = offsets.getAccessor().get(index);
      holder.end = offsets.getAccessor().get(index + 1);
    }
  }

  public class Mutator extends AbstractRepeatedMapVector.Mutator {
  }

  @Override
  public void exchange(ValueVector other) {
    DictVector map = (DictVector) other;
    assert this.keyType == null || this.keyType.equals(map.keyType)
        : "Cannot exchange DictVector with different key types";
    assert this.valueType == null || this.valueType.equals(map.valueType)
        : "Cannot exchange DictVector with different value types";
    super.exchange(other);
  }

  @Override
  public VectorWithOrdinal getChildVectorWithOrdinal(String name) {
    assert fieldNames.contains(name) : String.format(
        "DictVector has '%s' and '%s' children only", FIELD_KEY_NAME, FIELD_VALUE_NAME);
    ValueVector vector = getChild(name);
    switch (name) {
      case FIELD_KEY_NAME:
        return new VectorWithOrdinal(vector, 0);
      case FIELD_VALUE_NAME:
        return new VectorWithOrdinal(vector, 1);
      default:
        logger.warn("Field with name '{}' is not present in map vector.", name);
        return null;
    }
  }

  @Override
  MajorType getLastPathType() {
    if (Types.isRepeated(valueType)) {
      return valueType;
    }
    return valueType.toBuilder()
        .setMode(TypeProtos.DataMode.OPTIONAL)
        .build();
  }

  @Override
  public <T extends ValueVector> T getChild(String name, Class<T> clazz) {
    assert fieldNames.contains(name) : "No such field in DictVector: " + name;
    return super.getChild(name, clazz);
  }

  @Override
  public Accessor getAccessor() {
    return accessor;
  }

  @Override
  public Mutator getMutator() {
    return mutator;
  }

  public ValueVector getKeys() {
    return getChild(FIELD_KEY_NAME);
  }

  public ValueVector getValues() {
    return getChild(FIELD_VALUE_NAME);
  }

  public MajorType getKeyType() {
    return keyType;
  }

  public MajorType getValueType() {
    if (valueType == null) {
      valueType = getValues().getField().getType();
    }
    return valueType;
  }

  public boolean isValueNullable() {
    if (valueNullable == null) {
      valueNullable = Types.isNullable(getValueType());
    }
    return valueNullable;
  }

  @Override
  RepeatedValueHolder getValueHolder() {
    return new DictHolder();
  }

  private void setKeyValueTypes(MajorType keyType, MajorType valueType) {
    this.keyType = keyType;
    this.valueType = valueType;
  }
}
