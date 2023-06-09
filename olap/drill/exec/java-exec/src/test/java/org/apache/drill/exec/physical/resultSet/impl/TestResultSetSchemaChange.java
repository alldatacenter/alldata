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
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.physical.resultSet.impl.ResultSetLoaderImpl.ResultSetOptions;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.BatchSchemaBuilder;
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
public class TestResultSetSchemaChange extends SubOperatorTest {

  /**
   * Test the case where the schema changes in the first batch.
   * Schema changes before the first record are trivial and tested
   * elsewhere. Here we write some records, then add new columns, as a
   * JSON reader might do.
   */

  @Test
  public void testSchemaChangeFirstBatch() {
    ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator());
    RowSetLoader rootWriter = rsLoader.writer();
    rootWriter.addColumn(SchemaBuilder.columnSchema("a", MinorType.VARCHAR, DataMode.REQUIRED));

    // Create initial rows

    rsLoader.startBatch();
    int rowCount = 0;
    for (int i = 0; i < 2;  i++) {
      rootWriter.start();
      rowCount++;
      rootWriter.scalar(0).setString("a_" + rowCount);
      rootWriter.save();
    }

    // Add a second column: nullable.

    rootWriter.addColumn(SchemaBuilder.columnSchema("b", MinorType.INT, DataMode.OPTIONAL));
    for (int i = 0; i < 2;  i++) {
      rootWriter.start();
      rowCount++;
      rootWriter.scalar(0).setString("a_" + rowCount);
      rootWriter.scalar(1).setInt(rowCount);
      rootWriter.save();
    }

    // Add a third column. Use variable-width so that offset
    // vectors must be back-filled.

    rootWriter.addColumn(SchemaBuilder.columnSchema("c", MinorType.VARCHAR, DataMode.OPTIONAL));
    for (int i = 0; i < 2;  i++) {
      rootWriter.start();
      rowCount++;
      rootWriter.scalar(0).setString("a_" + rowCount);
      rootWriter.scalar(1).setInt(rowCount);
      rootWriter.scalar(2).setString("c_" + rowCount);
      rootWriter.save();
    }

    // Fourth: Required Varchar. Previous rows are back-filled with empty strings.
    // And a required int. Back-filled with zeros.
    // May occasionally be useful. But, does have to work to prevent
    // vector corruption if some reader decides to go this route.

    rootWriter.addColumn(SchemaBuilder.columnSchema("d", MinorType.VARCHAR, DataMode.REQUIRED));
    rootWriter.addColumn(SchemaBuilder.columnSchema("e", MinorType.INT,     DataMode.REQUIRED));
    for (int i = 0; i < 2;  i++) {
      rootWriter.start();
      rowCount++;
      rootWriter.scalar(0).setString("a_" + rowCount);
      rootWriter.scalar(1).setInt(rowCount);
      rootWriter.scalar(2).setString("c_" + rowCount);
      rootWriter.scalar(3).setString("d_" + rowCount);
      rootWriter.scalar(4).setInt(rowCount * 10);
      rootWriter.save();
    }

    // Add an array. Now two offset vectors must be back-filled.

    rootWriter.addColumn(SchemaBuilder.columnSchema("f", MinorType.VARCHAR, DataMode.REPEATED));
    for (int i = 0; i < 2;  i++) {
      rootWriter.start();
      rowCount++;
      rootWriter.scalar(0).setString("a_" + rowCount);
      rootWriter.scalar(1).setInt(rowCount);
      rootWriter.scalar(2).setString("c_" + rowCount);
      rootWriter.scalar(3).setString("d_" + rowCount);
      rootWriter.scalar(4).setInt(rowCount * 10);
      ScalarWriter arrayWriter = rootWriter.column(5).array().scalar();
      arrayWriter.setString("f_" + rowCount + "-1");
      arrayWriter.setString("f_" + rowCount + "-2");
      rootWriter.save();
    }

    // Harvest the batch and verify.

    RowSet actual = fixture.wrap(rsLoader.harvest());

    TupleMetadata expectedSchema = new SchemaBuilder()
        .add("a", MinorType.VARCHAR)
        .addNullable("b", MinorType.INT)
        .addNullable("c", MinorType.VARCHAR)
        .add("d", MinorType.VARCHAR)
        .add("e", MinorType.INT)
        .addArray("f", MinorType.VARCHAR)
        .buildSchema();
    SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow("a_1", null, null,   "",       0, strArray())
        .addRow("a_2", null, null,   "",       0, strArray())
        .addRow("a_3",    3, null,   "",       0, strArray())
        .addRow("a_4",    4, null,   "",       0, strArray())
        .addRow("a_5",    5, "c_5",  "",       0, strArray())
        .addRow("a_6",    6, "c_6",  "",       0, strArray())
        .addRow("a_7",    7, "c_7",  "d_7",   70, strArray())
        .addRow("a_8",    8, "c_8",  "d_8",   80, strArray())
        .addRow("a_9",    9, "c_9",  "d_9",   90, strArray("f_9-1",  "f_9-2"))
        .addRow("a_10",  10, "c_10", "d_10", 100, strArray("f_10-1", "f_10-2"))
        .build();

    RowSetUtilities.verify(expected, actual);
    rsLoader.close();
  }

  /**
   * Test a schema change on the row that overflows. If the
   * new column is added after overflow, it will appear as
   * a schema-change in the following batch. This is fine as
   * we are essentially time-shifting: pretending that the
   * overflow row was written in the next batch (which, in
   * fact, it is: that's what overflow means.)
   */

  @Test
  public void testSchemaChangeWithOverflow() {
    ResultSetOptions options = new ResultSetOptionBuilder()
        .rowCountLimit(ValueVector.MAX_ROW_COUNT)
        .build();
    ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    RowSetLoader rootWriter = rsLoader.writer();
    rootWriter.addColumn(SchemaBuilder.columnSchema("a", MinorType.VARCHAR, DataMode.REQUIRED));

    rsLoader.startBatch();
    byte value[] = new byte[512];
    Arrays.fill(value, (byte) 'X');
    int count = 0;
    while (! rootWriter.isFull()) {
      rootWriter.start();
      rootWriter.scalar(0).setBytes(value, value.length);

      // Relies on fact that isFull becomes true right after
      // a vector overflows; don't have to wait for saveRow().

      if (rootWriter.isFull()) {
        rootWriter.addColumn(SchemaBuilder.columnSchema("b", MinorType.INT, DataMode.OPTIONAL));
        rootWriter.scalar(1).setInt(count);

        // Add a Varchar to ensure its offset fiddling is done properly

        rootWriter.addColumn(SchemaBuilder.columnSchema("c", MinorType.VARCHAR, DataMode.OPTIONAL));
        rootWriter.scalar(2).setString("c-" + count);

        // Allow adding a required column at this point.
        // (Not intuitively obvious that this should work; we back-fill
        // with zeros.)

        rootWriter.addColumn(SchemaBuilder.columnSchema("d", MinorType.INT, DataMode.REQUIRED));
      }
      rootWriter.save();
      count++;
    }

    // Result should include only the first column.

    SchemaBuilder schemaBuilder = new SchemaBuilder()
      .add("a", MinorType.VARCHAR);
    BatchSchema expectedSchema = new BatchSchemaBuilder()
        .withSchemaBuilder(schemaBuilder)
        .build();
    RowSet result = fixture.wrap(rsLoader.harvest());
    assertTrue(result.batchSchema().isEquivalent(expectedSchema));
    assertEquals(count - 1, result.rowCount());
    result.clear();
    assertEquals(1, rsLoader.schemaVersion());

    // Double check: still can add a required column after
    // starting the next batch. (No longer in overflow state.)

    rsLoader.startBatch();
    rootWriter.addColumn(SchemaBuilder.columnSchema("e", MinorType.INT, DataMode.REQUIRED));

    // Next batch should start with the overflow row, including
    // the column added at the end of the previous batch, after
    // overflow.

    result = fixture.wrap(rsLoader.harvest());
    assertEquals(5, rsLoader.schemaVersion());
    assertEquals(1, result.rowCount());
    BatchSchemaBuilder batchSchemaBuilder = new BatchSchemaBuilder(expectedSchema);
    batchSchemaBuilder.schemaBuilder()
        .addNullable("b", MinorType.INT)
        .addNullable("c", MinorType.VARCHAR)
        .add("d", MinorType.INT)
        .add("e", MinorType.INT);

    expectedSchema = batchSchemaBuilder.build();
    assertTrue(result.batchSchema().isEquivalent(expectedSchema));
    RowSetReader reader = result.reader();
    reader.next();
    assertEquals(count - 1, reader.scalar(1).getInt());
    assertEquals("c-" + (count - 1), reader.scalar(2).getString());
    assertEquals(0, reader.scalar("d").getInt());
    assertEquals(0, reader.scalar("e").getInt());
    result.clear();

    rsLoader.close();
  }
}
