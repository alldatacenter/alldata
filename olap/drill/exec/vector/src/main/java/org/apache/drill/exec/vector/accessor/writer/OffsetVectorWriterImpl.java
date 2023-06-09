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
package org.apache.drill.exec.vector.accessor.writer;

import org.apache.drill.exec.vector.BaseDataValueVector;
import org.apache.drill.exec.vector.UInt4Vector;
import org.apache.drill.exec.vector.accessor.ColumnReader;
import org.apache.drill.exec.vector.accessor.InvalidConversionError;
import org.apache.drill.exec.vector.accessor.ValueType;
import org.apache.drill.exec.vector.accessor.impl.HierarchicalFormatter;

/**
 * Specialized column writer for the (hidden) offset vector used
 * with variable-length or repeated vectors. See comments in the
 * <tt>ColumnAccessors.java</tt> template file for more details.
 * <p>
 * Note that the <tt>lastWriteIndex</tt> tracked here corresponds
 * to the data values; it is one less than the actual offset vector
 * last write index due to the nature of offset vector layouts. The selection
 * of last write index basis makes roll-over processing easier as only this
 * writer need know about the +1 translation required for writing.
 * <p>
 * The states illustrated in the base class apply here as well,
 * remembering that the end offset for a row (or array position)
 * is written one ahead of the vector index.
 * <p>
 * The vector index does create an interesting dynamic for the child
 * writers. From the child writer's perspective, the states described in
 * the super class are the only states of interest. Here we want to
 * take the perspective of the parent.
 * <p>
 * The offset vector is an implementation of a repeat level. A repeat
 * level can occur for a single array, or for a collection of columns
 * within a repeated map. (A repeat level also occurs for variable-width
 * fields, but this is a bit harder to see, so let's ignore that for
 * now.)
 * <p>
 * The key point to realize is that each repeat level introduces an
 * isolation level in terms of indexing. That is, empty values in the
 * outer level have no affect on indexing in the inner level. In fact,
 * the nature of a repeated outer level means that there are no empties
 * in the inner level.
 * <p>
 * To illustrate:<pre><code>
 *       Offset Vector          Data Vector   Indexes
 *  lw, v > | 10 |   - - - - - >   | X |        10
 *          | 12 |   - - +         | X | < lw'  11
 *          |    |       + - - >   |   | < v'   12
 * </code></pre>
 * In the above, the client has just written an array of two elements
 * at the current write position. The data starts at offset 10 in
 * the data vector, and the next write will be at 12. The end offset
 * is written one ahead of the vector index.
 * <p>
 * From the data vector's perspective, its last-write (lw') reflects
 * the last element written. If this is an array of scalars, then the
 * write index is automatically incremented, as illustrated by v'.
 * (For map arrays, the index must be incremented by calling
 * <tt>save()</tt> on the map array writer.)
 * <p>
 * Suppose the client now skips some arrays:<pre><code>
 *       Offset Vector          Data Vector
 *     lw > | 10 |   - - - - - >   | X |        10
 *          | 12 |   - - +         | X | < lw'  11
 *          |    |       + - - >   |   | < v'   12
 *          |    |                 |   |        13
 *      v > |    |                 |   |        14
 * </code></pre>
 * The last write position does not move and there are gaps in the
 * offset vector. The vector index points to the current row. Note
 * that the data vector last write and vector indexes do not change,
 * this reflects the fact that the the data vector's vector index
 * (v') matches the tail offset
 * <p>
 * The
 * client now writes a three-element vector:<pre><code>
 *       Offset Vector          Data Vector
 *          | 10 |   - - - - - >   | X |        10
 *          | 12 |   - - +         | X |        11
 *          | 12 |   - - + - - >   | Y |        12
 *          | 12 |   - - +         | Y |        13
 *  lw, v > | 12 |   - - +         | Y | < lw'  14
 *          | 15 |   - - - - - >   |   | < v'   15
 * </code></pre>
 * Quite a bit just happened. The empty offset slots were back-filled
 * with the last write offset in the data vector. The client wrote
 * three values, which advanced the last write and vector indexes
 * in the data vector. And, the last write index in the offset
 * vector also moved to reflect the update of the offset vector.
 * Note that as a result, multiple positions in the offset vector
 * point to the same location in the data vector. This is fine; we
 * compute the number of entries as the difference between two successive
 * offset vector positions, so the empty positions have become 0-length
 * arrays.
 * <p>
 * Note that, for an array of scalars, when overflow occurs,
 * we need only worry about two
 * states in the data vector. Either data has been written for the
 * row (as in the third example above), and so must be moved to the
 * roll-over vector, or no data has been written and no move is
 * needed. We never have to worry about missing values because the
 * cannot occur in the data vector.
 * <p>
 * See {@link ObjectArrayWriter} for information about arrays of
 * maps (arrays of multiple columns.)
 *
 * <h4>Empty Slots</h4>
 *
 * The offset vector writer handles empty slots in two distinct ways.
 * First, the writer handles its own empties. Suppose that this is the offset
 * vector for a VarChar column. Suppose we write "Foo" in the first slot. Now
 * we have an offset vector with the values <tt>[ 0 3 ]</tt>. Suppose the client
 * skips several rows and next writes at slot 5. We must copy the latest
 * offset (3) into all the skipped slots: <tt>[ 0 3 3 3 3 3 ]</tt>. The result
 * is a set of four empty VarChars in positions 1, 2, 3 and 4. (Here, remember
 * that the offset vector always has one more value than the the number of rows.)
 * <p>
 * The second way to fill empties is in the data vector. The data vector may choose
 * to fill the four "empty" slots with a value, say "X". In this case, it is up to
 * the data vector to fill in the values, calling into this vector to set each
 * offset. Note that when doing this, the calls are a bit different than for writing
 * a regular value because we want to write at the "last write position", not the
 * current row position. See {@link BaseVarWidthWriter} for an example.
 */

public class OffsetVectorWriterImpl extends AbstractFixedWidthWriter implements OffsetVectorWriter {

  private static final int VALUE_WIDTH = UInt4Vector.VALUE_WIDTH;

  private final UInt4Vector vector;

  /**
   * Offset of the first value for the current row. Used during
   * overflow or if the row is restarted.
   */

  private int rowStartOffset;

  /**
   * Cached value of the end offset for the current value. Used
   * primarily for variable-width columns to allow the column to be
   * rewritten multiple times within the same row. The start offset
   * value is updated with the end offset only when the value is
   * committed in {@link @endValue()}.
   */

  protected int nextOffset;

  public OffsetVectorWriterImpl(UInt4Vector vector) {
    this.vector = vector;
  }

  @Override public BaseDataValueVector vector() { return vector; }
  @Override public int width() { return VALUE_WIDTH; }

  @Override
  protected void realloc(int size) {
    vector.reallocRaw(size);
    setBuffer();
  }

  @Override
  public ValueType valueType() { return ValueType.INTEGER; }

  @Override
  public void startWrite() {
    super.startWrite();
    nextOffset = 0;
    rowStartOffset = 0;

    // Special handling for first value. Alloc vector if needed.
    // Offset vectors require a 0 at position 0. The (end) offset
    // for row 0 starts at position 1, which is handled in
    // writeOffset() below.

    if (capacity * VALUE_WIDTH < MIN_BUFFER_SIZE) {
      realloc(MIN_BUFFER_SIZE);
    }
    drillBuf.setInt(0, 0);
  }

  @Override
  public int nextOffset() {return nextOffset; }

  @Override
  public int rowStartOffset() { return rowStartOffset; }

  @Override
  public void startRow() { rowStartOffset = nextOffset; }

  /**
   * Return the write offset, which is one greater than the index reported
   * by the vector index.
   *
   * @return the offset in which to write the current offset of the end
   * of the current data value
   */

  protected final int prepareWrite() {

    // This is performance critical code; every operation counts.
    // Please be thoughtful when changing the code.

    int valueIndex = prepareFill();
    int fillCount = valueIndex - lastWriteIndex - 1;
    if (fillCount > 0) {
      fillEmpties(fillCount);
    }

    // Track the last write location for zero-fill use next time around.

    lastWriteIndex = valueIndex;
    return valueIndex + 1;
  }

  public final int prepareFill() {
    int valueIndex = vectorIndex.vectorIndex();
    if (valueIndex + 1 < capacity) {
      return valueIndex;
    }
    resize(valueIndex + 1);

    // Call to resize may cause rollover, so get new write index afterwards.

    return vectorIndex.vectorIndex();
  }

  @Override
  protected final void fillEmpties(int fillCount) {
    for (int i = 0; i < fillCount; i++) {
      fillOffset(nextOffset);
    }
  }

  @Override
  public final void setNextOffset(int newOffset) {
    int writeIndex = prepareWrite();
    drillBuf.setInt(writeIndex * VALUE_WIDTH, newOffset);
    nextOffset = newOffset;
  }

  public final void reviseOffset(int newOffset) {
    int writeIndex = vectorIndex.vectorIndex() + 1;
    drillBuf.setInt(writeIndex * VALUE_WIDTH, newOffset);
    nextOffset = newOffset;
  }

  public final void fillOffset(int newOffset) {
    drillBuf.setInt((++lastWriteIndex + 1) * VALUE_WIDTH, newOffset);
    nextOffset = newOffset;
  }

  @Override
  public final void setValue(Object value) {
    throw new InvalidConversionError(
        "setValue() not supported for the offset vector writer: " + value);
  }

  @Override
  public void skipNulls() {

    // Nothing to do. Fill empties logic will fill in missing offsets.
  }

  @Override
  public void restartRow() {
    nextOffset = rowStartOffset;
    super.restartRow();
  }

  @Override
  public void preRollover() {

    // Rollover is occurring. This means the current row is not complete.
    // We want to keep 0..(row index - 1) which gives us (row index)
    // rows. But, this being an offset vector, we add one to account
    // for the extra 0 value at the start. That is, we want to set
    // the value count to the current row start index, which already
    // is set to one past the index of the last zero-based index.
    // (Offset vector indexes are confusing.)

    setValueCount(vectorIndex.rowStartIndex());
  }

  @Override
  public void postRollover() {
    int newNext = nextOffset - rowStartOffset;
    super.postRollover();
    nextOffset = newNext;
  }

  @Override
  public void setValueCount(int valueCount) {

    if (valueCount == 0) {

      // Special case: if the total number of values is zero,
      // then the offset vector should have 0 (not 1) values.
      // Serialization code relies on this fact.

      vector().getBuffer().writerIndex(0);
    } else {

      // Value count is in row positions, not index
      // positions. (There are one more index positions
      // than row positions.)

      int offsetCount = valueCount + 1;
      mandatoryResize(offsetCount);
      fillEmpties(valueCount - lastWriteIndex - 1);
      vector().getBuffer().writerIndex(offsetCount * VALUE_WIDTH);
    }
  }

  @Override
  public void dump(HierarchicalFormatter format) {
    format.extend();
    super.dump(format);
    format
      .attribute("lastWriteIndex", lastWriteIndex)
      .attribute("nextOffset", nextOffset)
      .endObject();
  }

  @Override
  public void setDefaultValue(Object value) {
    throw new UnsupportedOperationException("Encoding not supported for offset vectors");
  }

  @Override
  public void copy(ColumnReader from) {
    throw new UnsupportedOperationException("Copying of offset vectors not supported");
  }
}
