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
package org.apache.drill.exec.physical.resultSet.impl;

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.vector.FixedWidthVector;
import org.apache.drill.exec.vector.NullableVector;
import org.apache.drill.exec.vector.UInt4Vector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.VariableWidthVector;
import org.apache.drill.exec.vector.accessor.WriterPosition;
import org.apache.drill.exec.vector.accessor.impl.HierarchicalFormatter;
import org.apache.drill.exec.vector.accessor.writer.OffsetVectorWriter;
import org.apache.drill.exec.vector.accessor.writer.WriterEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for a single vector. Handles the bulk of work for that vector.
 * Subclasses are specialized for offset vectors or values vectors.
 * (The "single vector" name contrasts with classes that manage compound
 * vectors, such as a data and offsets vector.)
 */

public abstract class SingleVectorState implements VectorState {

  public abstract static class SimpleVectorState extends SingleVectorState {

    private static final Logger logger = LoggerFactory.getLogger(SimpleVectorState.class);

    public SimpleVectorState(WriterEvents writer,
        ValueVector mainVector) {
      super(writer, mainVector);
    }

    @Override
    protected void copyOverflow(int sourceStartIndex, int sourceEndIndex) {
      int newIndex = 0;
      logger.trace("Vector {} of type {}: copy {} values from {} to {}",
        mainVector.getField().toString(),
        mainVector.getClass().getSimpleName(),
        Math.max(0, sourceEndIndex - sourceStartIndex + 1),
        sourceStartIndex, newIndex);

      // Copy overflow values from the full vector to the new
      // look-ahead vector. Uses vector-level operations for convenience.
      // These aren't very efficient, but overflow does not happen very
      // often.

      for (int src = sourceStartIndex; src <= sourceEndIndex; src++, newIndex++) {
        mainVector.copyEntry(newIndex, backupVector, src);
      }
    }
  }

  /**
   * State for a scalar value vector. The vector might be for a simple (non-array)
   * vector, or might be the payload part of a scalar array (repeated scalar)
   * vector.
   */

  public static class FixedWidthVectorState extends SimpleVectorState {

     public FixedWidthVectorState(WriterEvents writer, ValueVector mainVector) {
      super(writer, mainVector);
    }

    @Override
    public int allocateVector(ValueVector vector, int cardinality) {
      ((FixedWidthVector) vector).allocateNew(cardinality);
      return vector.getAllocatedSize();
    }
  }

  public static class IsSetVectorState extends FixedWidthVectorState {

    public IsSetVectorState(WriterEvents writer, ValueVector mainVector) {
      super(writer, mainVector);
    }

    @Override
    public int allocateVector(ValueVector vector, int cardinality) {
      int size = super.allocateVector(vector, cardinality);

      // IsSet ("bit") vectors rely on values being initialized to zero (unset.)

      ((FixedWidthVector) vector).zeroVector();
      return size;
    }
  }

  /**
   * State for a scalar value vector. The vector might be for a simple (non-array)
   * vector, or might be the payload part of a scalar array (repeated scalar)
   * vector.
   */

  public static class VariableWidthVectorState extends SimpleVectorState {

    private final ColumnMetadata schema;

    public VariableWidthVectorState(ColumnMetadata schema, WriterEvents writer, ValueVector mainVector) {
      super(writer, mainVector);
      this.schema = schema;
    }

    @Override
    public int allocateVector(ValueVector vector, int cardinality) {

      // Cap the allocated size to the maximum.

      int size = (int) Math.min(ValueVector.MAX_BUFFER_SIZE, (long) cardinality * schema.expectedWidth());
      ((VariableWidthVector) vector).allocateNew(size, cardinality);
      return vector.getAllocatedSize();
    }
  }

  /**
   * Special case for an offset vector. Offset vectors are managed like any other
   * vector with respect to overflow and allocation. This means that the loader
   * classes avoid the use of the RepeatedVector class methods, instead working
   * with the offsets vector (here) or the values vector to allow the needed
   * fine control over overflow operations.
   */

  public static class OffsetVectorState extends SingleVectorState {

    private static final Logger logger = LoggerFactory.getLogger(OffsetVectorState.class);

    /**
     * The child writer used to determine positions on overflow.
     * The repeated list vector defers creating the child until the
     * child type is know so this field cannot be final. It will,
     * however, change value only once: from null to a valid writer.
     */

    private WriterPosition childWriter;

    public OffsetVectorState(WriterEvents writer, ValueVector mainVector,
        WriterPosition childWriter) {
      super(writer, mainVector);
      this.childWriter = childWriter;
    }

    public void setChildWriter(WriterEvents childWriter) {
      this.childWriter = childWriter;
    }

    @Override
    public int allocateVector(ValueVector toAlloc, int cardinality) {
      ((UInt4Vector) toAlloc).allocateNew(cardinality);
      return toAlloc.getBufferSize();
    }

    public int rowStartOffset() {
      return ((OffsetVectorWriter) writer).rowStartOffset();
    }

    @Override
    protected void copyOverflow(int sourceStartIndex, int sourceEndIndex) {

      if (sourceStartIndex > sourceEndIndex) {
        return;
      }

      assert childWriter != null;

      // This is an offset vector. The data to copy is one greater
      // than the row index.

      sourceStartIndex++;
      sourceEndIndex++;

      // Copy overflow values from the full vector to the new
      // look-ahead vector. Since this is an offset vector, values must
      // be adjusted as they move across.
      //
      // Indexing can be confusing. Offset vectors have values offset
      // from their row by one position. The offset vector position for
      // row i has the start value for row i. The offset vector position for
      // i+1 has the start of the next value. The difference between the
      // two is the element length. As a result, the offset vector always has
      // one more value than the number of rows, and position 0 is always 0.
      //
      // The index passed in here is that of the row that overflowed. That
      // offset vector position contains the offset of the start of the data
      // for the current row. We must subtract that offset from each copied
      // value to adjust the offset for the destination.

      UInt4Vector.Accessor sourceAccessor = ((UInt4Vector) backupVector).getAccessor();
      UInt4Vector.Mutator destMutator = ((UInt4Vector) mainVector).getMutator();
      int offset = childWriter.rowStartIndex();
      int newIndex = 1;
      logger.trace("Offset vector: copy {} values from {} to {} with offset {}",
        Math.max(0, sourceEndIndex - sourceStartIndex + 1),
        sourceStartIndex, newIndex, offset);
      assert offset == sourceAccessor.get(sourceStartIndex - 1);

      // Position zero is special and will be filled in by the writer
      // later.

      for (int src = sourceStartIndex; src <= sourceEndIndex; src++, newIndex++) {
        destMutator.set(newIndex, sourceAccessor.get(src) - offset);
      }

      // Getting offsets right was a pain. If you modify this code,
      // you'll likely relive that experience. Enabling the next two
      // lines will help reveal some of the mystery around offsets and their
      // confusing off-by-one design.

//      VectorPrinter.printOffsets((UInt4Vector) backupVector, sourceStartIndex - 1, sourceEndIndex - sourceStartIndex + 3);
//      VectorPrinter.printOffsets((UInt4Vector) mainVector, 0, newIndex);
    }
  }

  protected final WriterEvents writer;
  protected final ValueVector mainVector;
  protected ValueVector backupVector;

  public SingleVectorState(WriterEvents writer, ValueVector mainVector) {
    this.writer = writer;
    this.mainVector = mainVector;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends ValueVector> T vector() { return (T) mainVector; }

  @Override
  public int allocate(int cardinality) {
    return allocateVector(mainVector, cardinality);
  }

  protected abstract int allocateVector(ValueVector vector, int cardinality);

  /**
   * A column within the row batch overflowed. Prepare to absorb the rest of
   * the in-flight row by rolling values over to a new vector, saving the
   * complete vector for later. This column could have a value for the overflow
   * row, or for some previous row, depending on exactly when and where the
   * overflow occurs.
   *
   * sourceStartIndex: the index of the row that caused the overflow, the
   * values of which should be copied to a new "look-ahead" vector. If the
   * vector is an array, then the overflowIndex is the position of the first
   * element to be moved, and multiple elements may need to move
   *
   * @param cardinality the number of unique columns in the row
   */

  @Override
  public void rollover(int cardinality) {

    int sourceStartIndex = writer.rowStartIndex();

    // Remember the last write index for the original vector.
    // This tells us the end of the set of values to move, while the
    // sourceStartIndex above tells us the start.

    int sourceEndIndex = writer.lastWriteIndex();

    // Switch buffers between the backup vector and the writer's output
    // vector. Done this way because writers are bound to vectors and
    // we wish to keep the binding.

    if (backupVector == null) {
      backupVector = TypeHelper.getNewVector(mainVector.getField(),
          parseVectorType(mainVector), mainVector.getAllocator(), null);
    }
    assert cardinality > 0;
    allocateVector(backupVector, cardinality);
    mainVector.exchange(backupVector);

    // Copy overflow values from the full vector to the new
    // look-ahead vector.

    copyOverflow(sourceStartIndex, sourceEndIndex);

    // At this point, the writer is positioned to write to the look-ahead
    // vector at the position after the copied values. The original vector
    // is saved along with a last write position that is no greater than
    // the retained values.
  }

  /**
   * The vector mechanism here relies on the vector metadata. However, if the
   * main vector is nullable, it will contain a <code>values</code> vector which
   * is required. But the <code>values</code> vector will carry metadata that
   * declares it to be nullable. While this is clearly a bug, it is a bug that has
   * become a "feature" and cannot be changed. This code works around this feature
   * by parsing out the actual type of the vector.
   *
   * @param vector the vector to clone, the type of which may not match the
   * metadata declared within that vector
   * @return the actual major type of the vector
   */

  protected static MajorType parseVectorType(ValueVector vector) {
    MajorType purportedType = vector.getField().getType();
    if (purportedType.getMode() != DataMode.OPTIONAL) {
      return purportedType;
    }

    // For nullable vectors, the purported type can be wrong. The "outer"
    // vector is nullable, but the internal "values" vector is required, though
    // it carries a nullable type -- that is, the metadata lies.

    if (vector instanceof NullableVector) {
      return purportedType;
    }
    return purportedType.toBuilder()
        .setMode(DataMode.REQUIRED)
        .build();
  }

  protected abstract void copyOverflow(int sourceStartIndex, int sourceEndIndex);

  /**
    * Exchange the data from the backup vector and the main vector, putting
    * the completed buffers back into the main vectors, and stashing the
    * overflow buffers away in the backup vector.
    * Restore the main vector's last write position.
    */

  @Override
  public void harvestWithLookAhead() {
    mainVector.exchange(backupVector);
  }

  /**
   * The previous full batch has been sent downstream and the client is
   * now ready to start writing to the next batch. Initialize that new batch
   * with the look-ahead values saved during overflow of the previous batch.
   */

  @Override
  public void startBatchWithLookAhead() {
    mainVector.exchange(backupVector);
    backupVector.clear();
  }

  @Override
  public void close() {
    mainVector.clear();
    if (backupVector != null) {
      backupVector.clear();
    }
  }

  @Override
  public boolean isProjected() { return true; }

  public static SimpleVectorState vectorState(ColumnMetadata schema, WriterEvents writer, ValueVector mainVector) {
    if (schema.isVariableWidth()) {
      return new VariableWidthVectorState(schema, writer, mainVector);
    } else {
      return new FixedWidthVectorState(writer, mainVector);
    }
  }

  @Override
  public void dump(HierarchicalFormatter format) {
    format
      .startObject(this)
      .attributeIdentity("writer", writer)
      .attributeIdentity("mainVector", mainVector)
      .attributeIdentity("backupVector", backupVector)
      .endObject();
  }
}
