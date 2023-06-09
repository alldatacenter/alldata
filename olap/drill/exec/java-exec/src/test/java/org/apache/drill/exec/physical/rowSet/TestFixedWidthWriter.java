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
package org.apache.drill.exec.physical.rowSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.vector.IntVector;
import org.apache.drill.exec.vector.accessor.ColumnAccessors.IntColumnWriter;
import org.apache.drill.exec.vector.accessor.ColumnWriterIndex;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.accessor.ValueType;
import org.apache.drill.exec.vector.accessor.writer.WriterEvents.ColumnWriterListener;
import org.apache.drill.test.SubOperatorTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test the int writer as a typical example of a fixed-width
 * writer. Exercises normal writing, writing after a (simulated)
 * overflow, and filling in empty values.
 */

@Category(RowSetTests.class)
public class TestFixedWidthWriter extends SubOperatorTest {

  public static class TestIndex implements ColumnWriterIndex {

    public int index;

    @Override
    public int vectorIndex() { return index; }

    @Override
    public void nextElement() { }

    @Override
    public void prevElement() { }

    @Override
    public void rollover() { }

    @Override
    public int rowStartIndex() { return index; }

    @Override
    public ColumnWriterIndex outerIndex() { return null; }
  }

  /**
   * Basic test to write a contiguous set of values, enough to cause
   * the vector to double in size twice, then read back the values.
   */

  @Test
  public void testWrite() {
    try (IntVector vector = allocVector(1000)) {
      TestIndex index = new TestIndex();
      IntColumnWriter writer = makeWriter(vector, index);

      writer.startWrite();

      // Write integers.
      // Write enough that the vector is resized.

      long origAddr = vector.getBuffer().addr();
      for (int i = 0; i < 3000; i++) {
        index.index = i;
        writer.setInt(i * 10);
      }
      writer.endWrite();

      // Should have been reallocated.

      assertNotEquals(origAddr, vector.getBuffer().addr());

      // Verify values

      for (int i = 0; i < 3000; i++) {
        assertEquals(i * 10, vector.getAccessor().get(i));
      }
    }
  }

  @Test
  public void testRestartRow() {
    try (IntVector vector = allocVector(1000)) {
      TestIndex index = new TestIndex();
      IntColumnWriter writer = makeWriter(vector, index);
      writer.startWrite();

      // Write rows, rewriting every other row.

      writer.startRow();
      index.index = 0;
      for (int i = 0; i < 50; i++) {
        writer.setInt(i);
        if (i % 2 == 0) {
          writer.saveRow();
          writer.startRow();
          index.index++;
        } else {
          writer.restartRow();
        }
      }
      writer.endWrite();

      // Verify values

      for (int i = 0; i < 25; i++) {
        assertEquals(2 * i, vector.getAccessor().get(i));
      }
    }
  }

  /**
   * Required, fixed-width vectors are back-filling with 0 to fill in missing
   * values. While using zero is not strictly SQL compliant, it is better
   * than failing. (The SQL solution would be to fill with nulls, but a
   * required vector does not support nulls...)
   */

  @Test
  public void testFillEmpties() {
    try (IntVector vector = allocVector(1000)) {
      TestIndex index = new TestIndex();
      IntColumnWriter writer = makeWriter(vector, index);
      writer.startWrite();

      // Write values, skipping four out of five positions,
      // forcing backfill.
      // The number of values is odd, forcing the writer to
      // back-fill at the end as well as between values.
      // Keep the number of values below the allocation so
      // that we know all values were initially garbage-filled.

      for (int i = 0; i < 501; i += 5) {
        index.index = i;
        writer.startRow();
        writer.setInt(i);
        writer.saveRow();
      }
      // At end, vector index defined to point one past the
      // last row. That is, the vector index gives the row count.

      index.index = 504;
      writer.endWrite();

      // Verify values

      for (int i = 0; i < 504; i++) {
        assertEquals("Mismatch on " + i,
            (i%5) == 0 ? i : 0,
            vector.getAccessor().get(i));
      }
    }
  }

  /**
   * The rollover method is used during vector overflow.
   */

  @Test
  public void testRollover() {
    try (IntVector vector = allocVector(1000)) {
      TestIndex index = new TestIndex();
      IntColumnWriter writer = makeWriter(vector, index);
      writer.startWrite();

      // Simulate doing an overflow of ten values.

      for (int i = 0; i < 10; i++) {
        index.index = i;
        writer.startRow();
        writer.setInt(i);
        writer.saveRow();
      }

      // Overflow occurs after writing the 11th row

      index.index = 10;
      writer.startRow();
      writer.setInt(10);

      // Overflow occurs

      writer.preRollover();

      // Simulate rollover

      for (int i = 0; i < 15; i++) {
        vector.getMutator().set(i, 0xdeadbeef);
      }
      vector.getMutator().set(0, 10);

      writer.postRollover();
      index.index = 0;
      writer.saveRow();

      // Simulate resuming with a few more values.

      for (int i = 1; i < 5; i++) {
        index.index = i;
        writer.startRow();
        writer.setInt(10 + i);
        writer.saveRow();
      }
      writer.endWrite();

      // Verify the results

      for (int i = 0; i < 5; i++) {
        assertEquals(10 + i, vector.getAccessor().get(i));
      }
    }
  }

  /**
   * Simulate the case in which the tail end of an overflow
   * batch has empties. <tt>preRollover()</tt> should back-fill
   * them with the next offset prior to rollover.
   */

  @Test
  public void testRolloverWithEmpties() {
    try (IntVector vector = allocVector(1000)) {
      TestIndex index = new TestIndex();
      IntColumnWriter writer = makeWriter(vector, index);
      writer.startWrite();

      // Simulate doing an overflow of 15 values,
      // of which 5 are empty.

      for (int i = 0; i < 10; i++) {
        index.index = i;
        writer.startRow();
        writer.setInt(i);
        writer.saveRow();
      }

      for (int i = 10; i < 15; i++) {
        index.index = i;
        writer.startRow();
        writer.saveRow();
      }

      // Overflow occurs before writing the 16th row

      index.index = 15;
      writer.startRow();

      // Overflow occurs. This should fill empty offsets.

      writer.preRollover();

      // Verify the first "batch" results

      for (int i = 0; i < 10; i++) {
        assertEquals(i, vector.getAccessor().get(i));
      }
      for (int i = 10; i < 15; i++) {
        assertEquals(0, vector.getAccessor().get(i));
      }

      // Simulate rollover

      for (int i = 0; i < 20; i++) {
        vector.getMutator().set(i, 0xdeadbeef);
      }

      writer.postRollover();
      index.index = 0;
      writer.saveRow();

      // Skip more values.

      for (int i = 1; i < 5; i++) {
        index.index = i;
        writer.startRow();
        writer.saveRow();
      }

      // Simulate resuming with a few more values.

      for (int i = 5; i < 10; i++) {
        index.index = i;
        writer.startRow();
        writer.setInt(i + 20);
        writer.saveRow();
      }
      writer.endWrite();

      // Verify the results

      for (int i = 0; i < 5; i++) {
        assertEquals(0, vector.getAccessor().get(i));
      }
      for (int i = 5; i < 10; i++) {
        assertEquals(i + 20, vector.getAccessor().get(i));
      }
    }
  }

  /**
   * Test the case in which a scalar vector is used in conjunction
   * with a nullable bits vector. The nullable vector will call the
   * <tt>skipNulls()</tt> method to avoid writing values for null
   * entries. (Without the call, the scalar writer will fill the
   * empty values with zeros.)
   */

  @Test
  public void testSkipNulls() {
    try (IntVector vector = allocVector(1000)) {
      TestIndex index = new TestIndex();
      IntColumnWriter writer = makeWriter(vector, index);
      writer.startWrite();

      // Write values, skipping four out of five positions,
      // skipping nulls.
      // The loop will cause the vector to double in size.
      // The number of values is odd, forcing the writer to
      // skip nulls at the end as well as between values.

      long origAddr = vector.getBuffer().addr();
      for (int i = 0; i < 3000; i += 5) {
        index.index = i;
        writer.startRow();
        writer.skipNulls();
        writer.setInt(i);
        writer.saveRow();
      }
      index.index = 3003;
      writer.startRow();
      writer.skipNulls();
      writer.saveRow();
      writer.endWrite();

      // Should have been reallocated.

      assertNotEquals(origAddr, vector.getBuffer().addr());

      // Verify values. First 1000 were filled with known
      // garbage values.

      for (int i = 0; i < 1000; i++) {
        assertEquals("Mismatch at " + i,
            (i%5) == 0 ? i : 0xdeadbeef,
            vector.getAccessor().get(i));
      }

      // Next values are filled with unknown values:
      // whatever was left in the buffer allocated by Netty.

      for (int i = 1005; i < 3000; i+= 5) {
        assertEquals(i, vector.getAccessor().get(i));
      }
    }
  }

  /**
   * Test resize monitoring. Add a listener to an int writer,
   * capture each resize, and refuse a resize when the number
   * of ints exceeds 8K values. This will trigger an overflow,
   * which will throw an exception which we then check for.
   */

  @Test
  public void testSizeLimit() {
    try (IntVector vector = allocVector(1000)) {
      TestIndex index = new TestIndex();
      IntColumnWriter writer = makeWriter(vector, index);
      writer.bindListener(new ColumnWriterListener() {
        int totalAlloc = 4096;

        @Override
        public void overflowed(ScalarWriter writer) {
          throw new IllegalStateException("overflow called");
        }

        @Override
        public boolean canExpand(ScalarWriter writer, int delta) {
          totalAlloc += delta;
          return totalAlloc < 16_384 * 4;
        }
      });
      writer.startWrite();
      try {
        for (int i = 0;; i++ ) {
          index.index = i;
          writer.startRow();
          writer.setInt(i);
          writer.saveRow();
        }
      }
      catch(IllegalStateException e) {
        assertTrue(e.getMessage().contains("overflow called"));
      }

      // Should have failed on 8192, which doubled vector
      // to 16K, which was rejected.

      assertEquals(8192, index.index);
    }
  }

  private IntVector allocVector(int size) {
    MaterializedField field =
        SchemaBuilder.columnSchema("x", MinorType.INT, DataMode.REQUIRED);
    IntVector vector = new IntVector(field, fixture.allocator());
    vector.allocateNew(size);

    // Party on the bytes of the vector so we start dirty

    for (int i = 0; i < size; i++) {
      vector.getMutator().set(i, 0xdeadbeef);
    }
    return vector;
  }

  private IntColumnWriter makeWriter(IntVector vector, TestIndex index) {
    IntColumnWriter writer = new IntColumnWriter(vector);
    writer.bindIndex(index);

    assertEquals(ValueType.INTEGER, writer.valueType());
    return writer;
  }
}
