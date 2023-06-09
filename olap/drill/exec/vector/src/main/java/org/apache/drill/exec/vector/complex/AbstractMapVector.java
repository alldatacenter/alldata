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

import io.netty.buffer.DrillBuf;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.drill.common.collections.MapWithOrdinal;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.expr.BasicTypeHelper;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.memory.AllocationManager.BufferLedger;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.util.CallBack;
import org.apache.drill.exec.vector.ValueVector;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for MapVectors. Currently used by AbstractRepeatedMapVector and MapVector
 */
public abstract class AbstractMapVector extends AbstractContainerVector {
  private static final Logger logger = LoggerFactory.getLogger(AbstractContainerVector.class);

  // Maintains a map with key as field name and value is the vector itself
  private final MapWithOrdinal<String, ValueVector> vectors = new MapWithOrdinal<>();

  protected AbstractMapVector(MaterializedField field, BufferAllocator allocator, CallBack callBack) {
    super(field.clone(), allocator, callBack);
    // create the hierarchy of the child vectors based on the materialized field
    for (MaterializedField child : field.getChildren()) {
      if (child.getName().equals(BaseRepeatedValueVector.OFFSETS_FIELD.getName())) {
        continue;
      }
      final String fieldName = child.getName();
      final ValueVector v = BasicTypeHelper.getNewVector(child, allocator, callBack);
      putVector(fieldName, v);
    }
  }

  @Override
  public void close() {
    for (final ValueVector valueVector : vectors.values()) {
      valueVector.close();
    }
    vectors.clear();
    super.close();
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
      for (final ValueVector v : vectors.values()) {
        if (! v.allocateNewSafe()) {
          return false;
        }
      }
      success = true;
    } finally {
      if (! success) {
        clear();
      }
    }
    return true;
  }

  /**
   * Adds a new field with the given parameters or replaces the existing one and consequently returns the resultant
   * {@link org.apache.drill.exec.vector.ValueVector}.
   *
   * Execution takes place in the following order:
   * <ul>
   *   <li>
   *     if field is new, create and insert a new vector of desired type.
   *   </li>
   *   <li>
   *     if field exists and existing vector is of desired vector type, return the vector.
   *   </li>
   *   <li>
   *     if field exists and null filled, clear the existing vector; create and insert a new vector of desired type.
   *   </li>
   *   <li>
   *     otherwise, throw an {@link java.lang.IllegalStateException}
   *   </li>
   * </ul>
   *
   * @param name name of the field
   * @param type type of the field
   * @param clazz class of expected vector type
   * @param <T> class type of expected vector type
   * @throws java.lang.IllegalStateException raised if there is a hard schema change
   *
   * @return resultant {@link org.apache.drill.exec.vector.ValueVector}
   */
  @SuppressWarnings("unchecked")
  @Override
  public <T extends ValueVector> T addOrGet(String name, TypeProtos.MajorType type, Class<T> clazz) {
    final ValueVector existing = getChild(name);
    boolean create = false;
    if (existing == null) {
      create = true;
    } else if (clazz.isAssignableFrom(existing.getClass())) {
      return (T) existing;
    } else if (nullFilled(existing)) {
      existing.clear();
      // Since it's removing old vector and adding new one based on new type, it should do same for Materialized field,
      // Otherwise there will be duplicate of same field with same name but different type.
      field.removeChild(existing.getField());
      create = true;
    }
    if (create) {
      final T vector = (T) BasicTypeHelper.getNewVector(name, allocator, type, callBack);
      putChild(name, vector);
      if (callBack != null) {
        callBack.doWork();
      }
      return vector;
    }
    final String message = "Drill does not support schema change yet. Existing[%s] and desired[%s] vector types mismatch";
    throw new IllegalStateException(String.format(message, existing.getClass().getSimpleName(), clazz.getSimpleName()));
  }

  private boolean nullFilled(ValueVector vector) {
    for (int r = 0; r < vector.getAccessor().getValueCount(); r++) {
      if (! vector.getAccessor().isNull(r)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns a {@link org.apache.drill.exec.vector.ValueVector} corresponding to the given ordinal identifier.
   */
  public ValueVector getChildByOrdinal(int id) {
    return vectors.getByOrdinal(id);
  }

  /**
   * Returns a {@link org.apache.drill.exec.vector.ValueVector} instance of subtype of <T> corresponding to the given
   * field name if exists or null.
   */
  @Override
  public <T extends ValueVector> T getChild(String name, Class<T> clazz) {
    final ValueVector v = vectors.get(name.toLowerCase());
    if (v == null) {
      return null;
    }
    return typeify(v, clazz);
  }

  /**
   * Inserts the vector with the given name if it does not exist else replaces it with the new value.
   *
   * Note that this method does not enforce any vector type check nor throws a schema change exception.
   */
  public void putChild(String name, ValueVector vector) {
    putVector(name, vector);
    field.addChild(vector.getField());
  }

  /**
   * Inserts the input vector into the map if it does not exist, replaces if it exists already
   * @param name  field name
   * @param vector  vector to be inserted
   */
  protected void putVector(String name, ValueVector vector) {
    final ValueVector old = vectors.put(
        Preconditions.checkNotNull(name, "field name cannot be null").toLowerCase(),
        Preconditions.checkNotNull(vector, "vector cannot be null")
    );
    if (old != null && old != vector) {
      logger.debug("Field [{}] mutated from [{}] to [{}]", name, old.getClass().getSimpleName(),
                   vector.getClass().getSimpleName());
    }
  }

  /**
   * Returns a sequence of underlying child vectors.
   */
  protected Collection<ValueVector> getChildren() {
    return vectors.values();
  }

  /**
   * Returns the number of underlying child vectors.
   */
  @Override
  public int size() {
    return vectors.size();
  }

  @Override
  public Iterator<ValueVector> iterator() {
    return vectors.values().iterator();
  }

  /**
   * Returns a list of scalar child vectors recursing the entire vector hierarchy.
   */
  public List<ValueVector> getPrimitiveVectors() {
    final List<ValueVector> primitiveVectors = Lists.newArrayList();
    for (final ValueVector v : vectors.values()) {
      if (v instanceof AbstractMapVector) {
        AbstractMapVector mapVector = (AbstractMapVector) v;
        primitiveVectors.addAll(mapVector.getPrimitiveVectors());
      } else {
        primitiveVectors.add(v);
      }
    }
    return primitiveVectors;
  }

  /**
   * Returns a vector with its corresponding ordinal mapping if field exists or null.
   */
  @Override
  public VectorWithOrdinal getChildVectorWithOrdinal(String name) {
    final int ordinal = vectors.getOrdinal(name.toLowerCase());
    if (ordinal < 0) {
      return null;
    }
    final ValueVector vector = vectors.getByOrdinal(ordinal);
    return new VectorWithOrdinal(vector, ordinal);
  }

  @Override
  public DrillBuf[] getBuffers(boolean clear) {
    final List<DrillBuf> buffers = Lists.newArrayList();

    for (final ValueVector vector : vectors.values()) {
      for (final DrillBuf buf : vector.getBuffers(false)) {
        buffers.add(buf);
        if (clear) {
          buf.retain(1);
        }
      }
      if (clear) {
        vector.clear();
      }
    }

    return buffers.toArray(new DrillBuf[buffers.size()]);
  }

  @Override
  public int getBufferSize() {
    int actualBufSize = 0;

    for (final ValueVector v : vectors.values()) {
      for (final DrillBuf buf : v.getBuffers(false)) {
        actualBufSize += buf.writerIndex();
      }
    }
    return actualBufSize;
  }

  @Override
  public int getAllocatedSize() {
    int size = 0;

    for (final ValueVector v : vectors.values()) {
      size += v.getAllocatedSize();
    }
    return size;
  }

  @Override
  public void collectLedgers(Set<BufferLedger> ledgers) {
    for (final ValueVector v : vectors.values()) {
      v.collectLedgers(ledgers);
    }
  }

  @Override
  public int getPayloadByteCount(int valueCount) {
    if (valueCount == 0) {
      return 0;
    }

    int count = 0;

    for (final ValueVector v : vectors.values()) {
      count += v.getPayloadByteCount(valueCount);
    }
    return count;
  }

  @Override
  public void exchange(ValueVector other) {
    AbstractMapVector otherMap = (AbstractMapVector) other;
    if (vectors.size() != otherMap.vectors.size()) {
      throw new IllegalStateException("Maps have different column counts");
    }
    for (int i = 0; i < vectors.size(); i++) {
      assert vectors.getByOrdinal(i).getField().isEquivalent(
          otherMap.vectors.getByOrdinal(i).getField());
      vectors.getByOrdinal(i).exchange(otherMap.vectors.getByOrdinal(i));
    }
  }
}
