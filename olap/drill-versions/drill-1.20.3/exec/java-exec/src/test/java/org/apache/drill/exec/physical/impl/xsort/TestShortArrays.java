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
package org.apache.drill.exec.physical.impl.xsort;

import static org.apache.drill.test.rowSet.RowSetUtilities.intArray;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.record.RecordBatchSizer;
import org.apache.drill.exec.record.RecordBatchSizer.ColumnSize;
import org.apache.drill.exec.record.VectorInitializer;
import org.apache.drill.exec.record.VectorInitializer.AllocationHint;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.IntVector;
import org.apache.drill.exec.vector.RepeatedIntVector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.test.SubOperatorTest;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.junit.Test;

/**
 * DRILL-5804.
 * Code had a bug that if an array had low cardinality, the average,
 * expressed as an int, was zero. We then allocated a zero length
 * buffer, tried to double it, got another zero length buffer, and
 * looped forever. This test verifies the fixes to avoid that case.
 * @throws Exception
 */


public class TestShortArrays extends SubOperatorTest {

  @Test
  public void testSizer() {

    // Create a row set with less than one item, on
    // average, per array.

    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addArray("b", MinorType.INT)
        .buildSchema();
    RowSetBuilder builder = fixture.rowSetBuilder(schema)
        .addRow(1, intArray(10));
    for (int i = 2; i <= 10; i++) {
      builder.addRow(i, intArray());
    }
    RowSet rows = builder.build();

    // Run the record batch sizer on the resulting batch.

    RecordBatchSizer sizer = new RecordBatchSizer(rows.container());
    assertEquals(2, sizer.columns().size());
    ColumnSize bCol = sizer.columns().get("b");
    assertEquals(0.1, bCol.getCardinality(), 0.01);
    assertEquals(1, bCol.getElementCount());

    // Create a vector initializer using the sizer info.

    VectorInitializer vi = sizer.buildVectorInitializer();
    AllocationHint bHint = vi.hint("b");
    assertNotNull(bHint);
    assertEquals(bHint.elementCount, bCol.getCardinality(), 0.001);

    // Create a new batch, and new vector, using the sizer and
    // initializer inferred from the previous batch.

    SingleRowSet empty = fixture.rowSet(schema);
    vi.allocateBatch(empty.container(), 100);
    assertEquals(2, empty.container().getNumberOfColumns());
    ValueVector bVector = empty.container().getValueVector(1).getValueVector();
    assertTrue(bVector instanceof RepeatedIntVector);
    assertEquals(16, ((RepeatedIntVector) bVector).getDataVector().getValueCapacity());

    rows.clear();
    empty.clear();
  }

  /**
   * Test that a zero-length vector, on reAlloc, will default
   * to 256 bytes. (Previously the code just doubled zero
   * forever.)
   */

  @Test
  public void testReAllocZeroSize() {
    try (IntVector vector = new IntVector(
            SchemaBuilder.columnSchema("a", MinorType.INT, DataMode.REQUIRED),
            fixture.allocator())) {
      vector.allocateNew(0);
      vector.reAlloc();
      assertEquals(256 / 4, vector.getValueCapacity());
    }
  }
}
