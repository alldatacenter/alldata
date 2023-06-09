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
import org.apache.drill.exec.physical.rowSet.TestFixedWidthWriter.TestIndex;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.vector.UInt4Vector;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.accessor.ValueType;
import org.apache.drill.exec.vector.accessor.writer.OffsetVectorWriterImpl;
import org.apache.drill.exec.vector.accessor.writer.WriterEvents.ColumnWriterListener;
import org.apache.drill.test.SubOperatorTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * The offset vector writer is unique: it follows the same API as
 * the other writers, but has a unique twist because offsets are written
 * into the slot one after the other vectors. That is, if we are writing
 * row 5, the offset vector writer writes to position 6. This is done to
 * write the end offset of row 5 as the start offset of row 6. (It does,
 * however, waste space as we need twice the number of elements in the
 * offset vector as other vectors when writing power-of-two record
 * counts.)
 */

@Category(RowSetTests.class)
public class TestOffsetVectorWriter extends SubOperatorTest {

  /**
   * Party over enough memory that the uninitialized nature of
   * vectors under the new writers will cause test to fail if
   * the writer's don't correctly fill in all values.
   */

  @BeforeClass
  public static void setup() {
    fixture.dirtyMemory(100);
  }

  /**
   * Basic test to write a contiguous set of offsets, enough to cause
   * the vector to double in size twice, then read back the values.
   */

  @Test
  public void testWrite() {
    try (UInt4Vector vector = allocVector(1000)) {

      TestIndex index = new TestIndex();
      OffsetVectorWriterImpl writer = makeWriter(vector, index);

      // Start write sets initial position to 0.

      writer.startWrite();
      assertEquals(0, vector.getAccessor().get(0));

      // Pretend to write offsets for values of width 10. We write
      // the end position of each field.
      // Write enough that the vector is resized.

      long origAddr = vector.getBuffer().addr();
      for (int i = 0; i < 3000; i++) {
        index.index = i;
        writer.startRow();
        assertEquals(i * 10, writer.nextOffset());
        writer.setNextOffset((i+1) * 10);
        assertEquals((i+1) * 10, writer.nextOffset());
        writer.saveRow();
      }
      writer.endWrite();

      // Should have been reallocated.

      assertNotEquals(origAddr, vector.getBuffer().addr());

      // Verify values

      for (int i = 0; i < 3001; i++) {
        assertEquals(i * 10, vector.getAccessor().get(i));
      }
    }
  }

  @Test
  public void testRestartRow() {
    try (UInt4Vector vector = allocVector(1000)) {

      TestIndex index = new TestIndex();
      OffsetVectorWriterImpl writer = makeWriter(vector, index);
      writer.startWrite();

      // Write rows, rewriting every other row.

      writer.startRow();
      index.index = 0;
      for (int i = 0; i < 50; i++) {
        if (i % 2 == 0) {
          assertEquals(i == 0 ? 0 : (i - 1) * 10, writer.nextOffset());
          writer.setNextOffset((i + 1) * 10);
          writer.saveRow();
          writer.startRow();
          index.index++;
        } else {
          writer.setNextOffset((i + 1) * 10);
          writer.restartRow();
        }
      }
      writer.endWrite();

      // Verify values

      assertEquals(0, vector.getAccessor().get(0));
      for (int i = 1; i < 25; i++) {
        assertEquals((2 * i - 1) * 10, vector.getAccessor().get(i));
      }
    }
  }


  /**
   * Offset vectors have specific behavior when back-filling missing values:
   * the last offset must be carried forward into the missing slots. The
   * slots cannot be zero-filled, or entries will end up with a negative
   * length.
   */

  @Test
  public void testFillEmpties() {
    try (UInt4Vector vector = allocVector(1000)) {
      TestIndex index = new TestIndex();
      OffsetVectorWriterImpl writer = makeWriter(vector, index);
      writer.startWrite();

      // Pretend to write offsets for values of width 10, but
      // skip four out of five values, forcing backfill.
      // The loop will cause the vector to double in size.
      // The number of values is odd, forcing the writer to
      // back-fill at the end as well as between values.

      long origAddr = vector.getBuffer().addr();
      for (int i = 5; i < 3001; i += 5) {
        index.index = i;
        writer.startRow();
        int startOffset = writer.nextOffset();
        assertEquals((i/5 - 1) * 10, startOffset);
        writer.setNextOffset(startOffset + 10);
        writer.saveRow();
      }
      index.index = 3003;
      writer.endWrite();

      // Should have been reallocated.

      assertNotEquals(origAddr, vector.getBuffer().addr());

      // Verify values

      for (int i = 0; i < 3004; i++) {
        assertEquals(((i-1)/5) * 10, vector.getAccessor().get(i));
      }
    }
  }

  /**
   * The rollover method is used during vector overflow.
   */

  @Test
  public void testRollover() {
    try (UInt4Vector vector = allocVector(1000)) {
      TestIndex index = new TestIndex();
      OffsetVectorWriterImpl writer = makeWriter(vector, index);
      writer.startWrite();

      // Simulate doing an overflow of ten values.

      for (int i = 0; i < 10; i++) {
        index.index = i;
        writer.startRow();
        writer.setNextOffset((i+1) * 10);
        writer.saveRow();
      }

      // Overflow occurs after writing the 11th row

      index.index = 10;
      writer.startRow();
      writer.setNextOffset(110);

      // Overflow occurs

      writer.preRollover();

      // Simulate rollover

      for (int i = 0; i < 15; i++) {
        vector.getMutator().set(i, 0xdeadbeef);
      }

      // Simulate shifting the last value down (which changes
      // the offset.)

      vector.getMutator().set(1, 10);

      // Post rollover, slot 0 should be initialized

      writer.postRollover();
      index.index = 0;
      writer.saveRow();

      // Simulate resuming with a few more values.

      for (int i = 1; i < 5; i++) {
        index.index = i;
        writer.startRow();
        writer.setNextOffset((i + 1) * 10);
        writer.saveRow();
      }
      writer.endWrite();

      // Verify the results

      for (int i = 0; i < 6; i++) {
        assertEquals(i * 10, vector.getAccessor().get(i));
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
    try (UInt4Vector vector = allocVector(1000)) {
      TestIndex index = new TestIndex();
      OffsetVectorWriterImpl writer = makeWriter(vector, index);
      writer.startWrite();

      // Simulate doing an overflow of 15 values,
      // of which 5 are empty.

      for (int i = 0; i < 10; i++) {
        index.index = i;
        writer.startRow();
        writer.setNextOffset((i+1) * 10);
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

      for (int i = 0; i < 11; i++) {
        assertEquals("i = " + i, i * 10, vector.getAccessor().get(i));
      }
      for (int i = 11; i < 16; i++) {
        assertEquals("i = " + i, 100, vector.getAccessor().get(i));
      }

      // Simulate rollover

      for (int i = 0; i < 20; i++) {
        vector.getMutator().set(i, 0xdeadbeef);
      }

      // Simulate finishing the overflow row.

      index.index++;

      // Post rollover, slot 0 should be initialized.
      // This is a rollover. This row must set the value
      // for the new row 0 (which was presumably set/filled
      // after the overflow.)

      writer.postRollover();
      index.index = 0;
      writer.setNextOffset(0);
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
        writer.setNextOffset((i - 4) * 10);
        writer.saveRow();
      }
      writer.endWrite();

      // Verify the results

      for (int i = 0; i < 6; i++) {
        assertEquals("Index + " + i, 0, vector.getAccessor().get(i));
      }
      for (int i = 6; i < 11; i++) {
        assertEquals("Index + " + i, (i - 5) * 10, vector.getAccessor().get(i));
      }
    }
  }

  /**
   * Test resize monitoring. Add a listener to an offsets writer,
   * capture each resize, and refuse a resize when the number
   * of ints exceeds 8K values. This will trigger an overflow,
   * which will throw an exception which we then check for.
   */

  @Test
  public void testSizeLimit() {
    try (UInt4Vector vector = allocVector(1000)) {
      TestIndex index = new TestIndex();
      OffsetVectorWriterImpl writer = makeWriter(vector, index);
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
          writer.setNextOffset(i);
          writer.saveRow();
        }
      }
      catch(IllegalStateException e) {
        assertTrue(e.getMessage().contains("overflow called"));
      }

      // Should have failed on 8191, which doubled vector
      // to 16K, which was rejected. Note the 8191 value,
      // because offsets are one ahead of the index.

      assertEquals(8191, index.index);
    }
  }

  private UInt4Vector allocVector(int size) {
    MaterializedField field = SchemaBuilder.columnSchema("x", MinorType.UINT4,
        DataMode.REQUIRED);
    UInt4Vector vector = new UInt4Vector(field, fixture.allocator());
    vector.allocateNew(size);

    // Party on the bytes of the vector so we start dirty

    for (int i = 0; i < size; i++) {
      vector.getMutator().set(i, 0xdeadbeef);
    }
    assertNotEquals(0, vector.getAccessor().get(0));
    return vector;
  }

  private OffsetVectorWriterImpl makeWriter(UInt4Vector vector, TestIndex index) {
    OffsetVectorWriterImpl writer = new OffsetVectorWriterImpl(vector);
    writer.bindIndex(index);

    assertEquals(ValueType.INTEGER, writer.valueType());
    return writer;
  }
}
