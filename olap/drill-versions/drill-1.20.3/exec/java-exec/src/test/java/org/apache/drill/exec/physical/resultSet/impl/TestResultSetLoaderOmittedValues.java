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

import static org.apache.drill.test.rowSet.RowSetUtilities.strArray;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.test.SubOperatorTest;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.exec.physical.rowSet.RowSetReader;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;
import org.junit.experimental.categories.Category;


@Category(RowSetTests.class)
public class TestResultSetLoaderOmittedValues extends SubOperatorTest {

  /**
   * Test "holes" in the middle of a batch, and unset columns at
   * the end. Ending the batch should fill in missing values.
   */

  @Test
  public void testOmittedValuesAtEnd() {

    // Create columns up front

    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.VARCHAR)
        .addNullable("c", MinorType.VARCHAR)
        .add("d", MinorType.INT)
        .addNullable("e", MinorType.INT)
        .addArray("f", MinorType.VARCHAR)
        .buildSchema();
    ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .readerSchema(schema)
        .build();
    ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    RowSetLoader rootWriter = rsLoader.writer();

    rsLoader.startBatch();
    int rowCount = 0;
    ScalarWriter arrayWriter;
    for (int i = 0; i < 2;  i++) { // Row 0, 1
      rootWriter.start();
      rowCount++;
      rootWriter.scalar(0).setInt(rowCount);
      rootWriter.scalar(1).setString("b_" + rowCount);
      rootWriter.scalar(2).setString("c_" + rowCount);
      rootWriter.scalar(3).setInt(rowCount * 10);
      rootWriter.scalar(4).setInt(rowCount * 100);
      arrayWriter = rootWriter.column(5).array().scalar();
      arrayWriter.setString("f_" + rowCount + "-1");
      arrayWriter.setString("f_" + rowCount + "-2");
      rootWriter.save();
    }

    // Holes in half the columns

    for (int i = 0; i < 2;  i++) { // Rows 2, 3
      rootWriter.start();
      rowCount++;
      rootWriter.scalar(0).setInt(rowCount);
      rootWriter.scalar(1).setString("b_" + rowCount);
      rootWriter.scalar(3).setInt(rowCount * 10);
      arrayWriter = rootWriter.column(5).array().scalar();
      arrayWriter.setString("f_" + rowCount + "-1");
      arrayWriter.setString("f_" + rowCount + "-2");
      rootWriter.save();
    }

    // Holes in the other half

    for (int i = 0; i < 2;  i++) { // Rows 4, 5
      rootWriter.start();
      rowCount++;
      rootWriter.scalar(0).setInt(rowCount);
      rootWriter.scalar(2).setString("c_" + rowCount);
      rootWriter.scalar(4).setInt(rowCount * 100);
      rootWriter.save();
    }

    // All columns again.

    for (int i = 0; i < 2;  i++) { // Rows 6, 7
      rootWriter.start();
      rowCount++;
      rootWriter.scalar(0).setInt(rowCount);
      rootWriter.scalar(1).setString("b_" + rowCount);
      rootWriter.scalar(2).setString("c_" + rowCount);
      rootWriter.scalar(3).setInt(rowCount * 10);
      rootWriter.scalar(4).setInt(rowCount * 100);
      arrayWriter = rootWriter.column(5).array().scalar();
      arrayWriter.setString("f_" + rowCount + "-1");
      arrayWriter.setString("f_" + rowCount + "-2");
      rootWriter.save();
    }

    // Omit all but the key column at end

    for (int i = 0; i < 2;  i++) { // Rows 8, 9
      rootWriter.start();
      rowCount++;
      rootWriter.scalar(0).setInt(rowCount);
      rootWriter.save();
    }

    // Harvest the row and verify.

    RowSet actual = fixture.wrap(rsLoader.harvest());
//    actual.print();

    TupleMetadata expectedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.VARCHAR)
        .addNullable("c", MinorType.VARCHAR)
        .add("d", MinorType.INT)
        .addNullable("e", MinorType.INT)
        .addArray("f", MinorType.VARCHAR)
        .buildSchema();
    SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(  1, "b_1", "c_1",  10,  100, strArray("f_1-1",  "f_1-2"))
        .addRow(  2, "b_2", "c_2",  20,  200, strArray("f_2-1",  "f_2-2"))
        .addRow(  3, "b_3", null,   30, null, strArray("f_3-1",  "f_3-2"))
        .addRow(  4, "b_4", null,   40, null, strArray("f_4-1",  "f_4-2"))
        .addRow(  5, "",    "c_5",   0,  500, strArray())
        .addRow(  6, "",    "c_6",   0,  600, strArray())
        .addRow(  7, "b_7", "c_7",  70,  700, strArray("f_7-1",  "f_7-2"))
        .addRow(  8, "b_8", "c_8",  80,  800, strArray("f_8-1",  "f_8-2"))
        .addRow(  9, "",    null,    0, null, strArray())
        .addRow( 10, "",    null,    0, null, strArray())
        .build();

    RowSetUtilities.verify(expected, actual);
    rsLoader.close();
  }

  /**
   * Test "holes" at the end of a batch when batch overflows. Completed
   * batch must be finalized correctly, new batch initialized correct,
   * for the missing values.
   */

  @Test
  public void testOmittedValuesAtEndWithOverflow() {
    TupleMetadata schema = new SchemaBuilder()
        // Row index
        .add("a", MinorType.INT)
        // Column that forces overflow
        .add("b", MinorType.VARCHAR)
        // Column with all holes
        .addNullable("c", MinorType.VARCHAR)
        // Column with some holes
        .addNullable("d", MinorType.VARCHAR)
        .buildSchema();
    ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .rowCountLimit(ValueVector.MAX_ROW_COUNT)
        .readerSchema(schema)
        .build();
    ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    RowSetLoader rootWriter = rsLoader.writer();

    // Fill the batch. Column d has some values. Column c is worst case: no values.

    rsLoader.startBatch();
    byte value[] = new byte[533];
    Arrays.fill(value, (byte) 'X');
    int rowNumber = 0;
    while (! rootWriter.isFull()) {
      rootWriter.start();
      rowNumber++;
      rootWriter.scalar(0).setInt(rowNumber);
      rootWriter.scalar(1).setBytes(value, value.length);
      if (rowNumber < 10_000) {
        rootWriter.scalar(3).setString("d-" + rowNumber);
      }
      rootWriter.save();
      assertEquals(rowNumber, rsLoader.totalRowCount());
    }

    // Harvest and verify

    RowSet result = fixture.wrap(rsLoader.harvest());
    assertEquals(rowNumber - 1, result.rowCount());
    RowSetReader reader = result.reader();
    int rowIndex = 0;
    while (reader.next()) {
      int expectedRowNumber = 1 + rowIndex;
      assertEquals(expectedRowNumber, reader.scalar(0).getInt());
      assertTrue(reader.scalar(2).isNull());
      if (expectedRowNumber < 10_000) {
        assertEquals("d-" + expectedRowNumber, reader.scalar(3).getString());
      } else {
        assertTrue(reader.scalar(3).isNull());
      }
      rowIndex++;
    }

    // Start count for this batch is one less than current
    // count, because of the overflow row.

    int startRowNumber = rowNumber;

    // Write a few more rows to the next batch

    rsLoader.startBatch();
    for (int i = 0; i < 10; i++) {
      rootWriter.start();
      rowNumber++;
      rootWriter.scalar(0).setInt(rowNumber);
      rootWriter.scalar(1).setBytes(value, value.length);
      if (i > 5) {
        rootWriter.scalar(3).setString("d-" + rowNumber);
      }
      rootWriter.save();
      assertEquals(rowNumber, rsLoader.totalRowCount());
    }

    // Verify that holes were preserved.

    result = fixture.wrap(rsLoader.harvest());
    assertEquals(rowNumber, rsLoader.totalRowCount());
    assertEquals(rowNumber - startRowNumber + 1, result.rowCount());
//    result.print();
    reader = result.reader();
    rowIndex = 0;
    while (reader.next()) {
      int expectedRowNumber = startRowNumber + rowIndex;
      assertEquals(expectedRowNumber, reader.scalar(0).getInt());
      assertTrue(reader.scalar(2).isNull());
      if (rowIndex > 6) {
        assertEquals("d-" + expectedRowNumber, reader.scalar(3).getString());
      } else {
        assertTrue("Row " + rowIndex + " col d should be null", reader.scalar(3).isNull());
      }
      rowIndex++;
    }
    assertEquals(rowIndex, 11);

    rsLoader.close();
  }

  /**
   * Test that omitting the call to saveRow() effectively discards
   * the row. Note that the vectors still contain values in the
   * discarded position; just the various pointers are unset. If
   * the batch ends before the discarded values are overwritten, the
   * discarded values just exist at the end of the vector. Since vectors
   * start with garbage contents, the discarded values are simply a different
   * kind of garbage. But, if the client writes a new row, then the new
   * row overwrites the discarded row. This works because we only change
   * the tail part of a vector; never the internals.
   */

  @Test
  public void testSkipRows() {
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addNullable("b", MinorType.VARCHAR)
        .buildSchema();
    ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .rowCountLimit(ValueVector.MAX_ROW_COUNT)
        .readerSchema(schema)
        .build();
    ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    RowSetLoader rootWriter = rsLoader.writer();

    rsLoader.startBatch();
    int rowNumber = 0;
    for (int i = 0; i < 14; i++) {
      rootWriter.start();
      rowNumber++;
      rootWriter.scalar(0).setInt(rowNumber);
      if (i % 3 == 0) {
        rootWriter.scalar(1).setNull();
      } else {
        rootWriter.scalar(1).setString("b-" + rowNumber);
      }
      if (i % 2 == 0) {
        rootWriter.save();
      }
    }

    RowSet result = fixture.wrap(rsLoader.harvest());
//    result.print();
    SingleRowSet expected = fixture.rowSetBuilder(result.batchSchema())
        .addRow( 1, null)
        .addRow( 3, "b-3")
        .addRow( 5, "b-5")
        .addRow( 7, null)
        .addRow( 9, "b-9")
        .addRow(11, "b-11")
        .addRow(13, null)
        .build();
//    expected.print();
    RowSetUtilities.verify(expected, result);

    rsLoader.close();
  }

  /**
   * Test that discarding a row works even if that row happens to be an
   * overflow row.
   */

  @Test
  public void testSkipOverflowRow() {
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addNullable("b", MinorType.VARCHAR)
        .buildSchema();
    ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .rowCountLimit(ValueVector.MAX_ROW_COUNT)
        .readerSchema(schema)
        .build();
    ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    RowSetLoader rootWriter = rsLoader.writer();

    rsLoader.startBatch();
    byte value[] = new byte[512];
    Arrays.fill(value, (byte) 'X');
    int count = 0;
    while (! rootWriter.isFull()) {
      rootWriter.start();
      rootWriter.scalar(0).setInt(count);
      rootWriter.scalar(1).setBytes(value, value.length);

      // Relies on fact that isFull becomes true right after
      // a vector overflows; don't have to wait for saveRow().
      // Keep all rows, but discard the overflow row.

      if (! rootWriter.isFull()) {
        rootWriter.save();
      }
      count++;
    }

    // Discard the results.

    rsLoader.harvest().zeroVectors();

    // Harvest the next batch. Will be empty (because overflow row
    // was discarded.)

    rsLoader.startBatch();
    RowSet result = fixture.wrap(rsLoader.harvest());
    assertEquals(0, result.rowCount());
    result.clear();

    rsLoader.close();
  }

  /**
   * Verify that a default value set on the schema is used to fill missing
   * required columns.
   */
  @Test
  public void testDefaultValues() {
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.VARCHAR)
        .buildSchema();
    schema.metadata("b").setDefaultValue("Foo");
    ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .rowCountLimit(ValueVector.MAX_ROW_COUNT)
        .readerSchema(schema)
        .build();
    ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    RowSetLoader rootWriter = rsLoader.writer();

    rsLoader.startBatch();
    for (int i = 0; i < 7; i++) {
      rootWriter.start();
      rootWriter.scalar(0).setInt(i + 1);
      if (i % 3 != 0) {
        rootWriter.scalar(1).setString("b-" + (i + 1));
      }
      rootWriter.save();
    }

    RowSet result = fixture.wrap(rsLoader.harvest());
    SingleRowSet expected = fixture.rowSetBuilder(result.batchSchema())
        .addRow( 1, "Foo")
        .addRow( 2, "b-2")
        .addRow( 3, "b-3")
        .addRow( 4, "Foo")
        .addRow( 5, "b-5")
        .addRow( 6, "b-6")
        .addRow( 7, "Foo")
        .build();
    RowSetUtilities.verify(expected, result);

    rsLoader.close();
  }
}
