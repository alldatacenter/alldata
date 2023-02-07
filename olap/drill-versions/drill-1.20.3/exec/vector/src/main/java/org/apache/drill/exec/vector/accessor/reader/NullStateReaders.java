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
package org.apache.drill.exec.vector.accessor.reader;

import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.vector.accessor.ColumnAccessors.UInt1ColumnReader;
import org.apache.drill.exec.vector.accessor.ColumnReaderIndex;
import org.apache.drill.exec.vector.complex.UnionVector;

public class NullStateReaders {

  public static final RequiredStateReader REQUIRED_STATE_READER = new RequiredStateReader();
  public static final NullStateReader NULL_STATE_READER = new AlwaysNullStateReader();

  private NullStateReaders() { }

  /**
   * Dummy implementation of a null state reader for cases in which the
   * value is never null. Use the {@link NullStateReaders#REQUIRED_STATE_READER} instance
   * for this case.
   */

  protected static class RequiredStateReader implements NullStateReader {

    @Override
    public void bindIndex(ColumnReaderIndex rowIndex) { }

    @Override
    public boolean isNull() { return false; }

    @Override
    public void bindBuffer() {
    }
  }

  /**
   * Holder for the NullableVector wrapper around a bits vector and a
   * data vector. Manages the bits vector to extract the nullability
   * value.
   * <p>
   * This class allows the same reader to handle both the required and
   * nullable cases; the only difference is how nulls are handled.
   */

  protected static class NullableIsSetVectorStateReader implements NullStateReader {

    private final VectorAccessor nullableAccessor;
    private final UInt1ColumnReader isSetReader;

    public NullableIsSetVectorStateReader(VectorAccessor nullableAccessor) {
      this.nullableAccessor = nullableAccessor;
      isSetReader = new UInt1ColumnReader();
      isSetReader.bindVector(null,
          VectorAccessors.nullableBitsAccessor(nullableAccessor));
      isSetReader.bindNullState(REQUIRED_STATE_READER);
    }

    @Override
    public void bindIndex(ColumnReaderIndex rowIndex) {
      nullableAccessor.bind(rowIndex);
      isSetReader.bindIndex(rowIndex);
    }

    @Override
    public boolean isNull() {
      return isSetReader.getInt() == 0;
    }

    @Override
    public void bindBuffer() {
      isSetReader.bindBuffer();
    }
  }

  /**
   * Holder for the NullableVector wrapper around a bits vector and a
   * data vector. Manages the bits vector to extract the nullability
   * value.
   * <p>
   * This class allows the same reader to handle both the required and
   * nullable cases; the only difference is how nulls are handled.
   */

  protected static class ListIsSetVectorStateReader implements NullStateReader {

    private final VectorAccessor bitsAccessor;
    private final UInt1ColumnReader isSetReader;

    public ListIsSetVectorStateReader(VectorAccessor bitsAccessor) {
      this.bitsAccessor = bitsAccessor;
      isSetReader = new UInt1ColumnReader();
      isSetReader.bindVector(null, bitsAccessor);
      isSetReader.bindNullState(REQUIRED_STATE_READER);
    }

    @Override
    public void bindIndex(ColumnReaderIndex rowIndex) {
      bitsAccessor.bind(rowIndex);
      isSetReader.bindIndex(rowIndex);
    }

    @Override
    public boolean isNull() {
      return isSetReader.getInt() == 0;
    }

    @Override
    public void bindBuffer() {
      isSetReader.bindBuffer();
    }
  }

  /**
   * Null state that handles the strange union semantics that both
   * the union and the values can be null. A value is null if either
   * the union or the value is null. (Though, presumably, in the normal
   * case either the union is null or one of the associated values is
   * null.)
   */

  protected static class MemberNullStateReader implements NullStateReader {

    private final NullStateReader unionNullState;
    private final NullStateReader memberNullState;

    public MemberNullStateReader(NullStateReader unionNullState, NullStateReader memberNullState) {
      this.unionNullState = unionNullState;
      this.memberNullState = memberNullState;
    }

    @Override
    public void bindIndex(ColumnReaderIndex rowIndex) {
      memberNullState.bindIndex(rowIndex);
    }

    @Override
    public boolean isNull() {
      return unionNullState.isNull() || memberNullState.isNull();
    }

    @Override
    public void bindBuffer() {
      memberNullState.bindBuffer();
    }
  }

  /**
   * Handle the awkward situation with complex types. They don't carry their own
   * bits (null state) vector. Instead, we define them as null if the type of
   * the union is other than the type of the map or list. (Since the same vector
   * that holds state also holds the is-null value, this check includes the
   * check if the entire union is null.)
   */

  protected static class ComplexMemberStateReader implements NullStateReader {

    private final UInt1ColumnReader typeReader;
    private final MinorType type;

    public ComplexMemberStateReader(UInt1ColumnReader typeReader, MinorType type) {
      this.typeReader = typeReader;
      this.type = type;
    }

    @Override
    public void bindIndex(ColumnReaderIndex rowIndex) { }

    @Override
    public boolean isNull() {
      return typeReader.getInt() != type.getNumber();
    }

    @Override
    public void bindBuffer() {
    }
  }

  /**
   * Extract null state from the union vector's type vector. The union reader
   * manages the type reader, so no binding is done here.
   */

  protected static class TypeVectorStateReader implements NullStateReader {

    public final UInt1ColumnReader typeReader;

    public TypeVectorStateReader(UInt1ColumnReader typeReader) {
      this.typeReader = typeReader;
    }

    @Override
    public void bindIndex(ColumnReaderIndex rowIndex) {
      typeReader.bindIndex(rowIndex);
    }

    @Override
    public boolean isNull() {
      return typeReader.getInt() == UnionVector.NULL_MARKER;
    }

    @Override
    public void bindBuffer() {
      typeReader.bindBuffer();
    }
  }

  protected static class AlwaysNullStateReader implements NullStateReader {

    @Override
    public void bindIndex(ColumnReaderIndex rowIndex) {
    }

    @Override
    public boolean isNull() {
      return true;
    }

    @Override
    public void bindBuffer() {
    }
  }
}
