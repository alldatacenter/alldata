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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.vector.accessor.UnsupportedConversionError;
import org.apache.drill.exec.vector.accessor.impl.HierarchicalFormatter;
import org.joda.time.Period;

import io.netty.buffer.DrillBuf;

/**
 * Column writer implementation that acts as the basis for the
 * generated, vector-specific implementations. All set methods
 * throw an exception; subclasses simply override the supported
 * method(s).
 * <p>
 * The only tricky part to this class is understanding the
 * state of the write indexes as the write proceeds. There are
 * two pointers to consider:
 * <ul>
 * <li>lastWriteIndex: The position in the vector at which the
 * client last asked us to write data. This index is maintained
 * in this class because it depends only on the actions of this
 * class.</li>
 * <li>vectorIndex: The position in the vector at which we will
 * write if the client chooses to write a value at this time.
 * The vector index is shared by all columns at the same repeat
 * level. It is incremented as the client steps through the write
 * and is observed in this class each time a write occurs.</i>
 * </ul>
 * A repeat level is defined as any of the following:
 * <ul>
 * <li>The set of top-level scalar columns, or those within a
 * top-level, non-repeated map, or nested to any depth within
 * non-repeated maps rooted at the top level.</li>
 * <li>The values for a single scalar array.</li>
 * <li>The set of scalar columns within a repeated map, or
 * nested within non-repeated maps within a repeated map.</li>
 * </ul>
 * Items at a repeat level index together and share a vector
 * index. However, the columns within a repeat level
 * <i>do not</i> share a last write index: some can lag further
 * behind than others.
 * <p>
 * Let's illustrate the states. Let's focus on one column and
 * illustrate the three states that can occur during write:
 * <ul>
 * <li><b>Behind</b>: the last write index is more than one position behind
 * the vector index. Zero-filling will be needed to catch up to
 * the vector index.</li>
 * <li><b>Written</b>: the last write index is the same as the vector
 * index because the client wrote data at this position (and previous
 * values were back-filled with nulls, empties or zeros.)</li>
 * <li><b>Unwritten</b>: the last write index is one behind the vector
 * index. This occurs when the column was written, then the client
 * moved to the next row or array position.</li>
 * <li><b>Restarted</b>: The current row is abandoned (perhaps filtered
 * out) and is to be rewritten. The last write position moves
 * back one position. Note that, the Restarted state is
 * indistinguishable from the unwritten state: the only real
 * difference is that the current slot (pointed to by the
 * vector index) contains the previous written value that must
 * be overwritten or back-filled. But, this is fine, because we
 * assume that unwritten values are garbage anyway.</li>
 * </ul>
 * To illustrate:<pre><code>
 *      Behind      Written    Unwritten    Restarted
 *       |X|          |X|         |X|          |X|
 *   lw >|X|          |X|         |X|          |X|
 *       | |          |0|         |0|     lw > |0|
 *    v >| |  lw, v > |X|    lw > |X|      v > |X|
 *                            v > | |
 * </code></pre>
 * The illustrated state transitions are:
 * <ul>
 * <li>Suppose the state starts in Behind.<ul>
 *   <li>If the client writes a value, then the empty slot is
 *       back-filled and the state moves to Written.</li>
 *   <li>If the client does not write a value, the state stays
 *       at Behind, and the gap of unfilled values grows.</li></ul></li>
 * <li>When in the Written state:<ul>
 *   <li>If the client saves the current row or array position,
 *       the vector index increments and we move to the Unwritten
 *       state.</li>
 *   <li>If the client abandons the row, the last write position
 *       moves back one to recreate the unwritten state. We've
 *       shown this state separately above just to illustrate
 *       the two transitions from Written.</li></ul></li>
 * <li>When in the Unwritten (or Restarted) states:<ul>
 *   <li>If the client writes a value, then the writer moves back to the
 *       Written state.</li>
 *   <li>If the client skips the value, then the vector index increments
 *       again, leaving a gap, and the writer moves to the
 *       Behind state.</li></ul>
 * </ul>
 * <p>
 * We've already noted that the Restarted state is identical to
 * the Unwritten state (and was discussed just to make the flow a bit
 * clearer.) The astute reader will have noticed that the Behind state is
 * the same as the Unwritten state if we define the combined state as
 * when the last write position is behind the vector index.
 * <p>
 * Further, if
 * one simply treats the gap between last write and the vector indexes
 * as the amount (which may be zero) to back-fill, then there is just
 * one state. This is, in fact, how the code works: it always writes
 * to the vector index (and can do so multiple times for a single row),
 * back-filling as necessary.
 * <p>
 * The states, then, are more for our use in understanding the algorithm.
 * They are also very useful when working through the logic of performing
 * a roll-over when a vector overflows.
 */
public abstract class BaseScalarWriter extends AbstractScalarWriterImpl {

  public static final int MIN_BUFFER_SIZE = 256;

  /**
   * Listener invoked if the vector overflows. If not provided, then the writer
   * does not support vector overflow.
   */
  protected ColumnWriterListener listener;

  /**
   * Value to use to fill empties. Must be at least as wide as each
   * value.
   */
  protected byte emptyValue[];

  protected DrillBuf drillBuf;

  /**
   * Capacity, in values, of the currently allocated buffer that backs
   * the vector. Updated each time the buffer changes. The capacity is in
   * values (rather than bytes) to streamline the per-write logic.
   */
  protected int capacity;

  @Override
  public void bindListener(ColumnWriterListener listener) {
    this.listener = listener;
  }

  @Override
  public void bindSchema(ColumnMetadata schema) {
    super.bindSchema(schema);

    // Set the default value, if any, from the schema.
    final Object defaultValue = schema.decodeDefaultValue();
    if (defaultValue != null) {
      setDefaultValue(defaultValue);
    }
  }

  /**
   * All change of buffer comes through this function to allow capturing
   * the buffer address and capacity. Only two ways to set the buffer:
   * by binding to a vector in bindVector(), or by resizing the vector
   * in prepareWrite().
   */
  protected abstract void setBuffer();

  protected void realloc(int size) {
    vector().reallocRaw(size);
    setBuffer();
  }

  /**
   * The vector is about to grow. Give the listener a chance to
   * veto the growth and opt for overflow instead.
   *
   * @param delta the new amount of memory to allocate
   * @return true if the vector can be grown, false if an
   * overflow should be triggered
   */
  protected boolean canExpand(int delta) {
    if (listener == null) {
      return true;
    } else {
      return listener.canExpand(this, delta);
    }
  }

  /**
   * Handle vector overflow. If this is an array, then there is a slim chance
   * we may need to grow the vector immediately after overflow. Since a double
   * overflow is not allowed, this recursive call won't continue forever.
   */
  protected void overflowed() {
    if (listener == null) {
      throw new UnsupportedOperationException("Overflow not supported");
    } else {
      listener.overflowed(this);
    }
  }

  public abstract void skipNulls();

  @Override
  public boolean nullable() { return false; }

  @Override
  public void setNull() {
    throw UnsupportedConversionError.nullError(schema());
  }

  @Override
  public void setBoolean(boolean value) {
    throw conversionError("boolean");
  }

  @Override
  public void setInt(int value) {
    throw conversionError("int");
  }

  @Override
  public void setLong(long value) {
    throw conversionError("long");
  }

  @Override
  public void setFloat(float value) {
    throw conversionError("float");
  }

  @Override
  public void setDouble(double value) {
    throw conversionError("double");
  }

  @Override
  public void setString(String value) {
    throw conversionError("String");
  }

  @Override
  public void setBytes(byte[] value, int len) {
    throw conversionError("bytes");
  }

  @Override
  public void appendBytes(byte[] value, int len) {
    throw conversionError("bytes");
  }

  @Override
  public void setDecimal(BigDecimal value) {
    throw conversionError("Decimal");
  }

  @Override
  public void setPeriod(Period value) {
    throw conversionError("Period");
  }

  @Override
  public void setDate(LocalDate value) {
    throw conversionError("LocalDate");
  }

  @Override
  public void setTime(LocalTime value) {
    throw conversionError("LocalTime");
  }

  @Override
  public void setTimestamp(Instant value) {
    throw conversionError("Instant");
  }

  @Override
  public void dump(HierarchicalFormatter format) {
    format.extend();
    super.dump(format);
    format
      .attribute("vectorIndex", vectorIndex)
      .attributeIdentity("listener", listener)
      .attribute("capacity", capacity)
      .endObject();
  }
}
