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
package org.apache.drill.exec.record;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.complex.AbstractMapVector;
import org.apache.drill.exec.vector.complex.FieldIdUtil;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

import java.lang.reflect.Array;


public class HyperVectorWrapper<T extends ValueVector> implements VectorWrapper<T>{
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HyperVectorWrapper.class);

  private T[] vectors;
  private MaterializedField f;
  private final boolean releasable;

  public HyperVectorWrapper(MaterializedField f, T[] v) {
    this(f, v, true);
  }

  public HyperVectorWrapper(MaterializedField f, T[] v, boolean releasable) {
    assert(v.length > 0);
    this.f = f;
    this.vectors = v;
    this.releasable = releasable;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Class<T> getVectorClass() {
    return (Class<T>) vectors.getClass().getComponentType();
  }

  @Override
  public MaterializedField getField() {
    return f;
  }

  @Override
  public T getValueVector() {
    throw new UnsupportedOperationException();
  }

  @Override
  public T[] getValueVectors() {
    return vectors;
  }

  @Override
  public boolean isHyper() {
    return true;
  }

  @Override
  public void clear() {
    if (!releasable) {
      return;
    }
    for (final T x : vectors) {
      x.clear();
    }
  }

  @Override
  public VectorWrapper<?> getChildWrapper(int[] ids) {
    if (ids.length == 1) {
      return this;
    }

    final ValueVector[] vectors = new ValueVector[this.vectors.length];
    int index = 0;

    for (final ValueVector v : this.vectors) {
      ValueVector vector = v;
      for (int i = 1; i < ids.length; i++) {
        final AbstractMapVector mapLike = AbstractMapVector.class.cast(vector);
        if (mapLike == null) {
          return null;
        }
        vector = mapLike.getChildByOrdinal(ids[i]);
      }
      vectors[index] = vector;
      index++;
    }
    return new HyperVectorWrapper<ValueVector>(vectors[0].getField(), vectors);
  }

  @Override
  public TypedFieldId getFieldIdIfMatches(int id, SchemaPath expectedPath) {
    final ValueVector v = vectors[0];
    return FieldIdUtil.getFieldId(v, id, expectedPath, true);
  }

  @Override
  public VectorWrapper<T> cloneAndTransfer(BufferAllocator allocator) {
    return new HyperVectorWrapper<T>(f, vectors, false);
//    T[] newVectors = (T[]) Array.newInstance(vectors.getClass().getComponentType(), vectors.length);
//    for(int i =0; i < newVectors.length; i++) {
//      TransferPair tp = vectors[i].getTransferPair();
//      tp.transfer();
//      newVectors[i] = (T) tp.getTo();
//    }
//    return new HyperVectorWrapper<T>(f, newVectors);
  }

  public static <T extends ValueVector> HyperVectorWrapper<T> create(MaterializedField f, T[] v, boolean releasable) {
    return new HyperVectorWrapper<T>(f, v, releasable);
  }

  @SuppressWarnings("unchecked")
  public void addVector(ValueVector v) {
    Preconditions.checkArgument(v.getClass() == this.getVectorClass(), String.format("Cannot add vector type %s to hypervector type %s for field %s",
      v.getClass(), this.getVectorClass(), v.getField()));
    vectors = (T[]) ArrayUtils.add(vectors, v);// TODO optimize this so not copying every time
  }

  @SuppressWarnings("unchecked")
  public void addVectors(ValueVector[] vv) {
    vectors = (T[]) ArrayUtils.addAll(vectors, vv);
  }

  /**
   * Transfer vectors to destination HyperVectorWrapper.
   * Both this and destination must be of same type and have same number of vectors.
   * @param destination destination HyperVectorWrapper.
   */
  @Override
  public void transfer(VectorWrapper<?> destination) {
    Preconditions.checkArgument(destination instanceof HyperVectorWrapper);
    Preconditions.checkArgument(getField().getType().equals(destination.getField().getType()));
    Preconditions.checkArgument(vectors.length == ((HyperVectorWrapper<?>)destination).vectors.length);

    final ValueVector[] destionationVectors = ((HyperVectorWrapper<?>)destination).vectors;
    for (int i = 0; i < vectors.length; ++i) {
      vectors[i].makeTransferPair(destionationVectors[i]).transfer();
    }
  }

  /**
   * Method to replace existing list of vectors with the newly provided ValueVectors list in this HyperVectorWrapper
   * @param vv - New list of ValueVectors to be stored
   */
  @SuppressWarnings("unchecked")
  public void updateVectorList(ValueVector[] vv) {
    Preconditions.checkArgument(vv.length > 0);
    Preconditions.checkArgument(getField().getType().equals(vv[0].getField().getType()));
    // vectors.length will always be > 0 since in constructor that is enforced
    Preconditions.checkArgument(vv[0].getClass().equals(vectors[0].getClass()));
    clear();

    final Class<?> clazz = vv[0].getClass();
    final ValueVector[] c = (ValueVector[]) Array.newInstance(clazz, vv.length);
    System.arraycopy(vv, 0, c, 0, vv.length);
    vectors = (T[])c;
  }
}
