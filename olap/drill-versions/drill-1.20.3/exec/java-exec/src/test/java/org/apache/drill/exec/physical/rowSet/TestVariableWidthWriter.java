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
import org.apache.drill.exec.vector.VarCharVector;
import org.apache.drill.exec.vector.accessor.ColumnAccessors.VarCharColumnWriter;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.accessor.ValueType;
import org.apache.drill.exec.vector.accessor.writer.WriterEvents.ColumnWriterListener;
import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.test.SubOperatorTest;
import org.bouncycastle.util.Arrays;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(RowSetTests.class)
public class TestVariableWidthWriter extends SubOperatorTest {

  /**
   * Basic test to write a contiguous set of values, enough to cause
   * the vector to double in size twice, then read back the values.
   */

  @Test
  public void testWrite() {
    try (VarCharVector vector = allocVector(1000)) {
      TestIndex index = new TestIndex();
      VarCharColumnWriter writer = makeWriter(vector, index);

      writer.startWrite();

      // Write integers.
      // Write enough that the vector is resized.

      long origAddr = vector.getBuffer().addr();
      String base = "sample-value";
      for (int i = 0; i < 3000; i++) {
        index.index = i;
        writer.setString(base + i);
      }
      writer.endWrite();

      // Should have been reallocated.

      assertNotEquals(origAddr, vector.getBuffer().addr());

      // Verify values

      for (int i = 0; i < 3000; i++) {
        assertEquals(base + i, stringAt(vector, i));
      }
    }
  }

  @Test
  public void testRestartRow() {
    try (VarCharVector vector = allocVector(1000)) {
      TestIndex index = new TestIndex();
      VarCharColumnWriter writer = makeWriter(vector, index);
      writer.startWrite();

      // Write rows, rewriting every other row.

      String base = "sample-value";
      writer.startRow();
      index.index = 0;
      for (int i = 0; i < 50; i++) {
        writer.setString(base + i);
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
        assertEquals(base + (2 * i), stringAt(vector, i));
      }
    }
  }

  /**
   * Filling empties in a variable-width row means carrying forward
   * offsets (as tested elsewhere), leaving zero-length values.
   */

  @Test
  public void testFillEmpties() {
    try (VarCharVector vector = allocVector(1000)) {
      TestIndex index = new TestIndex();
      VarCharColumnWriter writer = makeWriter(vector, index);
      writer.startWrite();

      // Write values, skipping four out of five positions,
      // forcing backfill.
      // The number of values is odd, forcing the writer to
      // back-fill at the end as well as between values.

      String base = "sample-value";
      for (int i = 0; i < 501; i += 5) {
        index.index = i;
        writer.startRow();
        writer.setString(base + i);
        writer.saveRow();
      }
      // At end, vector index defined to point one past the
      // last row. That is, the vector index gives the row count.

      index.index = 504;
      writer.endWrite();

      // Verify values

      for (int i = 0; i < 504; i++) {
        assertEquals("Mismatch on " + i,
            (i%5) == 0 ? base + i : "", stringAt(vector, i));
      }
    }
  }

  /**
   * The rollover method is used during vector overflow.
   */

  @Test
  public void testRollover() {
    try (VarCharVector vector = allocVector(1000)) {
      TestIndex index = new TestIndex();
      VarCharColumnWriter writer = makeWriter(vector, index);
      writer.startWrite();

      // Simulate doing an overflow of ten values.

      String base = "sample-value";
      for (int i = 0; i < 10; i++) {
        index.index = i;
        writer.startRow();
        writer.setString(base + i);
        writer.saveRow();
      }

      // Overflow occurs after writing the 11th row

      index.index = 10;
      writer.startRow();
      String overflowValue = base + 10;
      writer.setString(overflowValue);

      // Overflow occurs

      writer.preRollover();

      // Simulate rollover

      byte dummy[] = new byte[] { (byte) 0x55 };
      for (int i = 0; i < 500; i++) {
        vector.getMutator().setSafe(i, dummy);
      }
      for (int i = 1; i < 15; i++) {
        vector.getOffsetVector().getMutator().set(i, 0xdeadbeef);
      }
      vector.getMutator().setSafe(0, overflowValue.getBytes(Charsets.UTF_8));

      writer.postRollover();
      index.index = 0;
      writer.saveRow();

      // Simulate resuming with a few more values.

      for (int i = 1; i < 5; i++) {
        index.index = i;
        writer.startRow();
        writer.setString(base + (i + 10));
        writer.saveRow();
      }
      writer.endWrite();

      // Verify the results

      for (int i = 0; i < 5; i++) {
        assertEquals(base + (10 + i), stringAt(vector, i));
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
    try (VarCharVector vector = allocVector(1000)) {
      TestIndex index = new TestIndex();
      VarCharColumnWriter writer = makeWriter(vector, index);
      writer.startWrite();

      // Simulate doing an overflow of 15 values,
      // of which 5 are empty.

      String base = "sample-value";
      for (int i = 0; i < 10; i++) {
        index.index = i;
        writer.startRow();
        writer.setString(base + i);
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
        assertEquals(base + i, stringAt(vector, i));
      }
      for (int i = 10; i < 15; i++) {
        assertEquals("", stringAt(vector, i));
      }

      // Simulate rollover

      byte dummy[] = new byte[] { (byte) 0x55 };
      for (int i = 0; i < 500; i++) {
        vector.getMutator().setSafe(i, dummy);
      }
      for (int i = 1; i < 15; i++) {
        vector.getOffsetVector().getMutator().set(i, 0xdeadbeef);
      }
      vector.getMutator().setSafe(0, new byte[] {});

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
        writer.setString(base + (i + 20));
        writer.saveRow();
      }
      writer.endWrite();

      // Verify the results

      for (int i = 0; i < 5; i++) {
        assertEquals("", stringAt(vector, i));
      }
      for (int i = 5; i < 10; i++) {
        assertEquals(base + (i + 20), stringAt(vector, i));
      }
    }
  }


  /**
   * Test the case in which a scalar vector is used in conjunction
   * with a nullable bits vector. The nullable vector will call the
   * <tt>skipNulls()</tt> method to avoid writing values for null
   * entries. For variable-width, there is no difference between
   * filling empties and skipping nulls: both result in zero-sized
   * entries.
   */

  @Test
  public void testSkipNulls() {
    try (VarCharVector vector = allocVector(1000)) {
      TestIndex index = new TestIndex();
      VarCharColumnWriter writer = makeWriter(vector, index);
      writer.startWrite();

      // Write values, skipping four out of five positions,
      // skipping nulls.
      // The number of values is odd, forcing the writer to
      // skip nulls at the end as well as between values.

      String base = "sample-value";
      for (int i = 0; i < 3000; i += 5) {
        index.index = i;
        writer.startRow();
        writer.skipNulls();
        writer.setString(base + i);
        writer.saveRow();
      }
      index.index = 3003;
      writer.startRow();
      writer.skipNulls();
      writer.saveRow();
      writer.endWrite();

      // Verify values. Skipping nulls should back-fill
      // offsets, resulting in zero-length strings.

      for (int i = 0; i < 3000; i++) {
        assertEquals("Mismatch at " + i,
            (i%5) == 0 ? base + i : "", stringAt(vector, i));
      }
    }
  }

  /**
   * Test resize monitoring. Add a listener to an Varchar writer,
   * capture each resize, and refuse a resize when the s
   * of the vector exceeds 1 MB. This will trigger an overflow,
   * which will throw an exception which we then check for.
   */

  @Test
  public void testSizeLimit() {
    try (VarCharVector vector = allocVector(1000)) {
      TestIndex index = new TestIndex();
      VarCharColumnWriter writer = makeWriter(vector, index);
      writer.bindListener(new ColumnWriterListener() {
        // Because assumed array size is 10, so 10 * 1000 = 10,000
        // rounded to 16K
        int totalAlloc = 16384;

        @Override
        public void overflowed(ScalarWriter writer) {
          throw new IllegalStateException("overflow called");
        }

        @Override
        public boolean canExpand(ScalarWriter writer, int delta) {
          totalAlloc += delta;
          return totalAlloc < 1024 * 1024;
        }
      });
      writer.startWrite();

      byte value[] = new byte[423];
      Arrays.fill(value, (byte) 'X');
      try {
        for (int i = 0;; i++ ) {
          index.index = i;
          writer.startRow();
          writer.setBytes(value, value.length);
          writer.saveRow();
        }
      }
      catch(IllegalStateException e) {
        assertTrue(e.getMessage().contains("overflow called"));
      }
    }
  }

  private String stringAt(VarCharVector vector, int i) {
    return new String(vector.getAccessor().get(i), Charsets.UTF_8);
  }

  private VarCharVector allocVector(int size) {
    MaterializedField field =
        SchemaBuilder.columnSchema("x", MinorType.VARCHAR, DataMode.REQUIRED);
    VarCharVector vector = new VarCharVector(field, fixture.allocator());
    vector.allocateNew(size * 10, size);
    return vector;
  }

  private VarCharColumnWriter makeWriter(VarCharVector vector, TestIndex index) {
    VarCharColumnWriter writer = new VarCharColumnWriter(vector);
    writer.bindIndex(index);

    assertEquals(ValueType.STRING, writer.valueType());
    return writer;
  }
}
