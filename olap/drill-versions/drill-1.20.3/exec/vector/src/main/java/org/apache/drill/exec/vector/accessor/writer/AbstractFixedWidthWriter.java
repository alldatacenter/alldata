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

import java.math.BigDecimal;

import org.apache.drill.exec.memory.BaseAllocator;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.accessor.InvalidConversionError;
import org.apache.drill.exec.vector.accessor.impl.HierarchicalFormatter;

/**
 * Base class for writers for fixed-width vectors. Handles common
 * tasks, leaving the generated code to handle only type-specific
 * operations.
 * <p>
 * Fixed-width writers provide default values for empty (unused) slots.
 * Suppose a client writes to slot 0, skips a few rows, then writes to slot
 * 5. We could leave the intermediate values unwritten. But, since Drill
 * reuses buffers, the slots may contain garbage. Instead, we implement
 * "fill empties" logic. When we write to slot 5, we notice that the last
 * slot written was 0, and we fill in slots 1, 2, 3 and 4 with values.
 * <p>
 * The fill value defaults to 0, and is defined as a block of zero-bytes
 * (at least) the same length as each data value. Derived classes also
 * allow setting a default value. In this case, the default value is encoded
 * into a byte array, and that array is copied to each slot as the fill value.
 */
public abstract class AbstractFixedWidthWriter extends BaseScalarWriter {

  public static abstract class BaseFixedWidthWriter extends AbstractFixedWidthWriter {

    /**
     * Buffer of zeros used to back-fill vector buffers with
     * zeros.
     */
    private static final byte ZERO_BUF[] = new byte[256];

    public BaseFixedWidthWriter() {
      emptyValue = ZERO_BUF;
    }

    /**
     * Determine the write index, growing, overflowing and back-filling
     * the vector as needed.
     * <p>
     * This is a bit tricky. This method has side effects, by design.
     * The current vector buffer, and buffer address, will change in
     * this method when a vector grows or overflows. So, don't use this
     * method in inline calls of the form<br><code>
     * vector.getBuffer().doSomething(prepareWrite());</code></br>
     * The buffer obtained by <tt>getBuffer()</tt> can be different than
     * the current buffer after <tt>prepareWrite()</tt>.
     *
     * @return the index at which to write the current value
     */
    protected final int prepareWrite() {

      // "Fast path" for the normal case of no fills, no overflow.
      // This is the only bounds check we want to do for the entire
      // set operation.

      // This is performance critical code; every operation counts.
      // Please be thoughtful when changing the code.

      int writeIndex = vectorIndex.vectorIndex();
      if (lastWriteIndex + 1 < writeIndex || writeIndex >= capacity) {
        writeIndex = prepareWrite(writeIndex);
      }

      // Track the last write location for zero-fill use next time around.
      lastWriteIndex = writeIndex;
      return writeIndex;
    }

    protected final int prepareWrite(int writeIndex) {

      // Either empties must be filed or the vector is full.
      writeIndex = resize(writeIndex);

      // Fill empties to the write position.
      fillEmpties(writeIndex);
      return writeIndex;
    }

    /**
     * Fill empties. This is required because the allocated memory is not
     * zero-filled. Also allows filling a non-nullable vector with a defined
     * default value.
     */
    @Override
    protected final void fillEmpties(final int writeIndex) {
      final int width = width();
      final int stride = emptyValue.length / width;
      int dest = lastWriteIndex + 1;
      while (dest < writeIndex) {
        final int length = Math.min(writeIndex - dest, stride);
        drillBuf.setBytes(dest * width, emptyValue, 0, length * width);
        dest += length;
      }
    }
  }

  /**
   * Base class for writers that use the Java int type as their native
   * type. Handles common implicit conversions from other types to int.
   */
  public static abstract class BaseIntWriter extends BaseFixedWidthWriter {

    @Override
    public final void setBoolean(final boolean value) {
      setInt(value ? 1 : 0);
    }

    @Override
    public final void setLong(final long value) {
      try {
        // Catches int overflow. Does not catch overflow for smaller types.
        setInt(Math.toIntExact(value));
      } catch (final ArithmeticException e) {
        throw InvalidConversionError.writeError(schema(), value, e);
      }
    }

    @Override
    public final void setFloat(final float value) {
      try {
        // Catches int overflow. Does not catch overflow from
        // double. See Math.round for details.
        setInt(Math.toIntExact(Math.round(value)));
      } catch (final ArithmeticException e) {
        throw InvalidConversionError.writeError(schema(), value, e);
      }
    }

    @Override
    public final void setDouble(final double value) {
      try {
        // Catches int overflow. Does not catch overflow from
        // double. See Math.round for details.
        setInt(Math.toIntExact(Math.round(value)));
      } catch (final ArithmeticException e) {
        throw InvalidConversionError.writeError(schema(), value, e);
      }
    }

    @Override
    public final void setDecimal(final BigDecimal value) {
      try {
        // Catches int overflow.
        setInt(value.intValueExact());
      } catch (final ArithmeticException e) {
        throw InvalidConversionError.writeError(schema(), value, e);
      }
    }

    @Override
    public final void setValue(final Object value) {
      if (value != null) {
        setInt((int) value);
      }
    }
  }

  /**
   * The largest position to which the writer has written data. Used to allow
   * "fill-empties" (AKA "back-fill") of missing values one each value write
   * and at the end of a batch. Note that this is the position of the last
   * write, not the next write position. Starts at -1 (no last write).
   */
  protected int lastWriteIndex;

  @Override
  public void startWrite() {
    setBuffer();
    lastWriteIndex = -1;
  }

  public abstract int width();

  @Override
  protected final void setBuffer() {
    drillBuf = vector().getBuffer();
    capacity = drillBuf.capacity() / width();
  }

  protected final void mandatoryResize(final int writeIndex) {
    if (writeIndex < capacity) {
      return;
    }

    // Since some vectors start off as 0 length, set a
    // minimum size to avoid silly thrashing on early rows.
    final int size = BaseAllocator.nextPowerOfTwo(
        Math.max((writeIndex + 1) * width(), MIN_BUFFER_SIZE));
    realloc(size);
  }

  protected final int resize(final int writeIndex) {
    if (writeIndex < capacity) {
      return writeIndex;
    }
    final int width = width();

    // Since some vectors start off as 0 length, set a
    // minimum size to avoid silly thrashing on early rows.
    final int size = BaseAllocator.nextPowerOfTwo(
        Math.max((writeIndex + 1) * width, MIN_BUFFER_SIZE));

    // Two cases: grow this vector or allocate a new one.

    // Grow the vector -- or overflow if the growth would make the batch
    // consume too much memory. The idea is that we grow vectors as they
    // fit the available memory budget, then we fill those vectors until
    // one of them needs more space. At that point we trigger overflow to
    // a new set of vectors. Internal fragmentation will result, but this
    // approach (along with proper initial vector sizing), minimizes that
    // fragmentation.
    if (size <= ValueVector.MAX_BUFFER_SIZE &&
        canExpand(size - capacity * width)) {

      // Optimized form of reAlloc() which does not zero memory, does not do
      // bounds checks (since they were already done above). The write index
      // and offset remain unchanged.
      realloc(size);
    } else {

      // Allocate a new vector, or throw an exception if overflow is not
      // supported. If overflow is supported, the callback will call
      // endWrite(), which will fill empties, so no need to do that here.
      // The call to endWrite() will also set the final writer index for the
      // current vector. Then, bindVector() will be called to provide the new
      // vector. The write index changes with the new vector.
      overflowed();
    }

    // Call to resize may cause rollover, so reset write index
    // afterwards.
    return vectorIndex.vectorIndex();
  }

  @Override
  public int lastWriteIndex() { return lastWriteIndex; }

  /**
   * For internal use only to update the write position on those
   * very rare occasions in which the vector is written to outside
   * of this writer framework. Not to be called by application code!
   *
   * @param index new last write index
   */
  public void setLastWriteIndex(int index) {
    lastWriteIndex = index;
  }

  @Override
  public void skipNulls() {

    // Pretend we've written up to the previous value.
    // This will leave null values (as specified by the
    // caller) uninitialized.
    lastWriteIndex = vectorIndex.vectorIndex() - 1;
  }

  @Override
  public void restartRow() {
    lastWriteIndex = Math.min(lastWriteIndex, vectorIndex.vectorIndex() - 1);
  }

  @Override
  public void preRollover() {
    setValueCount(vectorIndex.rowStartIndex());
  }

  @Override
  public void postRollover() {
    final int newIndex = Math.max(lastWriteIndex - vectorIndex.rowStartIndex(), -1);
    startWrite();
    lastWriteIndex = newIndex;
  }

  @Override
  public void endWrite() {
    setValueCount(vectorIndex.vectorIndex());
  }

  protected abstract void fillEmpties(int writeIndex);

  public void setValueCount(int valueCount) {

    // Done this way to avoid another drill buf access in value set path.
    // Though this calls writeOffset(), which handles vector overflow,
    // such overflow should never occur because here we are simply
    // finalizing a position already set. However, the vector size may
    // grow and the "missing" values may be zero-filled. Note that, in
    // odd cases, the call to writeOffset() might cause the vector to
    // resize (as part of filling empties), so grab the buffer AFTER
    // the call to writeOffset().
    mandatoryResize(valueCount - 1);
    fillEmpties(valueCount);
    vector().getBuffer().writerIndex(valueCount * width());

    // Last write index is either the last value we just filled,
    // or it is the last actual write, if this is an overflow
    // situation.
    lastWriteIndex = Math.max(lastWriteIndex, valueCount - 1);
  }

  @Override
  public void dump(HierarchicalFormatter format) {
    format.extend();
    super.dump(format);
    format
      .attribute("lastWriteIndex", lastWriteIndex)
      .endObject();
  }
}
