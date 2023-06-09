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

import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.vector.BaseDataValueVector;
import org.apache.drill.exec.vector.accessor.ColumnReaderIndex;

import io.netty.buffer.DrillBuf;

/**
 * Column reader implementation that acts as the basis for the
 * generated, vector-specific implementations. All set methods
 * throw an exception; subclasses simply override the supported
 * method(s).
 */
public abstract class BaseScalarReader extends AbstractScalarReader {

  public abstract static class BaseFixedWidthReader extends BaseScalarReader {

    public abstract int width();

    public final int offsetIndex() {
      return vectorIndex.offset();
    }
  }

  public abstract static class BaseVarWidthReader extends BaseScalarReader {

    protected OffsetVectorReader offsetsReader;

    @Override
    public void bindVector(ColumnMetadata schema, VectorAccessor va) {
      super.bindVector(schema, va);
      offsetsReader = new OffsetVectorReader(
          VectorAccessors.varWidthOffsetVectorAccessor(va));
    }

    @Override
    public void bindIndex(ColumnReaderIndex index) {
      super.bindIndex(index);
      offsetsReader.bindIndex(index);
    }

    @Override
    public void bindBuffer() {
      super.bindBuffer();
      offsetsReader.bindBuffer();
    }

    public final long getEntry( ) {
      return offsetsReader.getEntry();
    }
  }

  /**
   * Provide access to the DrillBuf for the data vector.
   */
  public interface BufferAccessor {
    DrillBuf buffer();
  }

  private static class VectorBufferAccessor implements BufferAccessor {
    private final VectorAccessor va;

    public VectorBufferAccessor(VectorAccessor va) {
      this.va = va;
    }

    @Override
    public DrillBuf buffer() {
      BaseDataValueVector vector = va.vector();
      return vector.getBuffer();
    }
  }

  protected ColumnMetadata schema;
  protected VectorAccessor vectorAccessor;
  protected BufferAccessor bufferAccessor;

  public static ScalarObjectReader buildOptional(ColumnMetadata schema,
      VectorAccessor va, BaseScalarReader reader) {

    // Reader is bound to the values vector inside the nullable vector.
    reader.bindVector(schema, VectorAccessors.nullableValuesAccessor(va));

    // The nullability of each value depends on the "bits" vector
    // in the nullable vector.
    reader.bindNullState(new NullStateReaders.NullableIsSetVectorStateReader(va));

    // Wrap the reader in an object reader.
    return new ScalarObjectReader(reader);
  }

  public static ScalarObjectReader buildRequired(ColumnMetadata schema,
      VectorAccessor va, BaseScalarReader reader) {

    // Reader is bound directly to the required vector.
    reader.bindVector(schema, va);

    // The reader is required, values can't be null.
    reader.bindNullState(NullStateReaders.REQUIRED_STATE_READER);

    // Wrap the reader in an object reader.
    return new ScalarObjectReader(reader);
  }

  public void bindVector(ColumnMetadata schema, VectorAccessor va) {
    this.schema = schema;
    vectorAccessor = va;
    bufferAccessor = bufferAccessor(va);
  }

  protected BufferAccessor bufferAccessor(VectorAccessor va) {
    return new VectorBufferAccessor(va);
  }

  @Override
  public void bindIndex(ColumnReaderIndex rowIndex) {
    super.bindIndex(rowIndex);
    vectorAccessor.bind(rowIndex);
  }

  @Override
  public ColumnMetadata schema() { return schema; }

  @Override
  public void bindBuffer() {
    nullStateReader.bindBuffer();
  }

  public final DrillBuf buffer() {
    return bufferAccessor.buffer();
  }
}
