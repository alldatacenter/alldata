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
import static org.junit.Assert.assertTrue;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.rowSet.RowSet.ExtendableRowSet;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.vector.accessor.ArrayReader;
import org.apache.drill.exec.vector.accessor.ArrayWriter;
import org.apache.drill.exec.vector.accessor.ScalarReader;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.test.SubOperatorTest;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test reading with an indirection vector (sv2.) This form of
 * indirection vector reorders values within a single batch.
 * Since the indirection occurs only in the reader index, only
 * light testing is done; all readers go through the same index class,
 * so if the index works for one reader, it will for for all.
 */

@Category(RowSetTests.class)
public class TestIndirectReaders extends SubOperatorTest {

  /**
   * Simplest case: required reader, uses the index
   * directly.
   */

  @Test
  public void testRequired() {
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .buildSchema();
    ExtendableRowSet rowSet = fixture.rowSet(schema);
    RowSetWriter writer = rowSet.writer();

    // 10 rows with value 0 .. 9

    for (int i = 0; i < 10; i++) {
      writer.scalar(0).setInt(i);
      writer.save();
    }

    SingleRowSet result = writer.done().toIndirect();

    // Use the SV2 to reverse the row order.

    SelectionVector2 sv2 = result.getSv2();
    for (int i = 0; i < 10; i++) {
      sv2.setIndex(i, 9 - i);
    }

    // Values should be read back in the reverse order.

    RowSetReader reader = result.reader();
    for (int i = 9; i >= 0; i--) {
      assertTrue(reader.next());
      assertEquals(i, reader.scalar(0).getInt());
    }

    // The row set comparison should read using the
    // indirection.

    SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addRow(9)
        .addRow(8)
        .addRow(7)
        .addRow(6)
        .addRow(5)
        .addRow(4)
        .addRow(3)
        .addRow(2)
        .addRow(1)
        .addRow(0)
        .build();

    RowSetUtilities.verify(expected, result);
  }

  /**
   * More complex case with two levels of offset vector (one for the
   * array, another for the Varchar values.) Only the top level goes
   * through the indirection.
   */

  @Test
  public void testArray() {
    TupleMetadata schema = new SchemaBuilder()
        .addArray("a", MinorType.VARCHAR)
        .buildSchema();
    ExtendableRowSet rowSet = fixture.rowSet(schema);
    RowSetWriter writer = rowSet.writer();
    ArrayWriter aWriter = writer.array(0);
    ScalarWriter strWriter = aWriter.scalar();

    for (int i = 0; i < 10; i++) {
      for (int j = 0; j < 5; j++) {
        strWriter.setString("value" + i + "." + j);
      }
      writer.save();
    }

    SingleRowSet result = writer.done().toIndirect();

    SelectionVector2 sv2 = result.getSv2();
    for (int i = 0; i < 10; i++) {
      sv2.setIndex(i, 9 - i);
    }

    RowSetReader reader = result.reader();
    ArrayReader aReader = reader.array(0);
    ScalarReader strReader = aReader.scalar();

    for (int i = 9; i >= 0; i--) {
      assertTrue(reader.next());
      for (int j = 0; j < 5; j++) {
        assertTrue(aReader.next());
        assertEquals("value" + i + "." + j, strReader.getString());
      }
    }

    result.clear();
  }
}
