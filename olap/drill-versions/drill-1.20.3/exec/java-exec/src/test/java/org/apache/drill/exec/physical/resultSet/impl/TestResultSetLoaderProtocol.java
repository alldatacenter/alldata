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

import static org.apache.drill.test.rowSet.RowSetUtilities.boolArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.byteArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.doubleArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.floatArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.intArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.longArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.shortArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.strArray;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.exec.physical.rowSet.RowSetReader;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.accessor.TupleWriter.UndefinedColumnException;
import org.apache.drill.test.SubOperatorTest;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests of the overall result set loader protocol focusing on which operations
 * are valid in each state, basics of column lookup, basics of adding columns
 * and so on. Uses the simplest possible type: a required int.
 * <p>
 * Run this test first to do a sanity check of the result set loader after making
 * changes.
 * <p>
 * You will find that the result set loader creates a very complex tree of
 * objects that can be quite hard to understand and debug. Please read the
 * material in the various subsystems to see how the classes fit together
 * to implement Drill's rich JSON-like data model.
 * <p>
 * To aid in debugging, you can also dump the result set loader, and all its
 * child objects as follows:<pre><code>
 * ((ResultSetLoaderImpl) rsLoader).dump(new HierarchicalPrinter());
 * </code></pre>
 * Simply insert that line into these tests anywhere you want to visualize
 * the structure. The object tree will show all the components and their
 * current state.
 */
@Category(RowSetTests.class)
public class TestResultSetLoaderProtocol extends SubOperatorTest {

  @Test
  public void testBasics() {
    ResultSetLoaderImpl rsLoaderImpl = new ResultSetLoaderImpl(fixture.allocator());
    ResultSetLoader rsLoader = rsLoaderImpl;
    assertEquals(0, rsLoader.schemaVersion());
    assertEquals(ResultSetLoader.DEFAULT_ROW_COUNT, rsLoader.targetRowCount());
    assertEquals(ValueVector.MAX_BUFFER_SIZE, rsLoader.targetVectorSize());
    assertEquals(0, rsLoader.writer().rowCount());
    assertEquals(0, rsLoader.batchCount());
    assertEquals(0, rsLoader.totalRowCount());
    assertTrue(rsLoader.isProjectionEmpty());

    // Failures due to wrong state (Start)

    try {
      rsLoader.harvest();
      fail();
    } catch (IllegalStateException e) {
      // Expected
    }

    // Can define schema before starting the first batch.

    RowSetLoader rootWriter = rsLoader.writer();
    TupleMetadata schema = rootWriter.tupleSchema();
    assertEquals(0, schema.size());

    MaterializedField fieldA = SchemaBuilder.columnSchema("a", MinorType.INT, DataMode.REQUIRED);
    rootWriter.addColumn(fieldA);
    assertFalse(rsLoader.isProjectionEmpty());

    assertEquals(1, schema.size());
    assertTrue(fieldA.isEquivalent(schema.column(0)));
    assertSame(schema.metadata(0), schema.metadata("a"));

    // Error to start a row before the first batch.

    try {
      rootWriter.start();
      fail();
    } catch (IllegalStateException e) {
      // Expected
    }

    // Error to end a row before the first batch.

    try {
      rootWriter.save();
      fail();
    } catch (IllegalStateException e) {
      // Expected
    }

    // Because writing is an inner loop; no checks are
    // done to ensure that writing occurs only in the proper
    // state. So, can't test setInt() in the wrong state.

    rsLoader.startBatch();
    try {
      rsLoader.startBatch();
      fail();
    } catch (IllegalStateException e) {
      // Expected
    }
    assertFalse(rootWriter.isFull());

    rootWriter.start();
    rootWriter.scalar(0).setInt(100);
    assertEquals(0, rootWriter.rowCount());
    assertEquals(0, rsLoader.batchCount());
    rootWriter.save();
    assertEquals(1, rootWriter.rowCount());
    assertEquals(1, rsLoader.batchCount());
    assertEquals(1, rsLoader.totalRowCount());

    // Can add a field after first row, prior rows are
    // "back-filled".

    MaterializedField fieldB = SchemaBuilder.columnSchema("b", MinorType.INT, DataMode.OPTIONAL);
    rootWriter.addColumn(fieldB);

    assertEquals(2, schema.size());
    assertTrue(fieldB.isEquivalent(schema.column(1)));
    assertSame(schema.metadata(1), schema.metadata("b"));

    rootWriter.start();
    rootWriter.scalar(0).setInt(200);
    rootWriter.scalar(1).setInt(210);
    rootWriter.save();
    assertEquals(2, rootWriter.rowCount());
    assertEquals(1, rsLoader.batchCount());
    assertEquals(2, rsLoader.totalRowCount());

    // Harvest the first batch. Version number is the number
    // of columns added.

    assertFalse(rootWriter.isFull());
    RowSet result = fixture.wrap(rsLoader.harvest());
    assertEquals(2, rsLoader.schemaVersion());
    assertEquals(0, rootWriter.rowCount());
    assertEquals(1, rsLoader.batchCount());
    assertEquals(2, rsLoader.totalRowCount());

    SingleRowSet expected = fixture.rowSetBuilder(result.batchSchema())
        .addRow(100, null)
        .addRow(200, 210)
        .build();
    RowSetUtilities.verify(expected, result);

    // Between batches: batch-based operations fail

    try {
      rootWriter.start();
      fail();
    } catch (IllegalStateException e) {
      // Expected
    }
    try {
      rsLoader.harvest();
      fail();
    } catch (IllegalStateException e) {
      // Expected
    }
    try {
      rootWriter.save();
      fail();
    } catch (IllegalStateException e) {
      // Expected
    }

    // Create a second batch

    rsLoader.startBatch();
    assertEquals(0, rootWriter.rowCount());
    assertEquals(1, rsLoader.batchCount());
    assertEquals(2, rsLoader.totalRowCount());
    rootWriter.start();
    rootWriter.scalar(0).setInt(300);
    rootWriter.scalar(1).setInt(310);
    rootWriter.save();
    assertEquals(1, rootWriter.rowCount());
    assertEquals(2, rsLoader.batchCount());
    assertEquals(3, rsLoader.totalRowCount());
    rootWriter.start();
    rootWriter.scalar(0).setInt(400);
    rootWriter.scalar(1).setInt(410);
    rootWriter.save();

    // Harvest. Schema has not changed.

    result = fixture.wrap(rsLoader.harvest());
    assertEquals(2, rsLoader.schemaVersion());
    assertEquals(0, rootWriter.rowCount());
    assertEquals(2, rsLoader.batchCount());
    assertEquals(4, rsLoader.totalRowCount());

    expected = fixture.rowSetBuilder(result.batchSchema())
        .addRow(300, 310)
        .addRow(400, 410)
        .build();
    RowSetUtilities.verify(expected, result);

    // Next batch. Schema has changed.

    rsLoader.startBatch();
    rootWriter.start();
    rootWriter.scalar(0).setInt(500);
    rootWriter.scalar(1).setInt(510);
    rootWriter.addColumn(SchemaBuilder.columnSchema("c", MinorType.INT, DataMode.OPTIONAL));
    rootWriter.scalar(2).setInt(520);
    rootWriter.save();
    rootWriter.start();
    rootWriter.scalar(0).setInt(600);
    rootWriter.scalar(1).setInt(610);
    rootWriter.scalar(2).setInt(620);
    rootWriter.save();

    result = fixture.wrap(rsLoader.harvest());
    assertEquals(3, rsLoader.schemaVersion());
    expected = fixture.rowSetBuilder(result.batchSchema())
        .addRow(500, 510, 520)
        .addRow(600, 610, 620)
        .build();
    RowSetUtilities.verify(expected, result);

    rsLoader.close();

    // Key operations fail after close.

    try {
      rootWriter.start();
      fail();
    } catch (IllegalStateException e) {
      // Expected
    }
    try {
      rsLoader.writer();
      fail();
    } catch (IllegalStateException e) {
      // Expected
    }
    try {
      rsLoader.startBatch();
      fail();
    } catch (IllegalStateException e) {
      // Expected
    }
    try {
      rsLoader.harvest();
      fail();
    } catch (IllegalStateException e) {
      // Expected
    }
    try {
      rootWriter.save();
      fail();
    } catch (IllegalStateException e) {
      // Expected
    }

    // Benign to close twice

    rsLoader.close();
  }

  /**
   * Schemas are case insensitive by default. Verify that
   * the schema mechanism works, with emphasis on the
   * case insensitive case.
   * <p>
   * The tests here and elsewhere build columns from a
   * <tt>MaterializedField</tt>. Doing so is rather old-school;
   * better to use the newer <tt>ColumnMetadata</tt> which provides
   * additional information. The code here simply uses the <tt>MaterializedField</tt>
   * to create a <tt>ColumnMetadata</tt> implicitly.
   */
  @Test
  public void testCaseInsensitiveSchema() {
    ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator());
    RowSetLoader rootWriter = rsLoader.writer();
    TupleMetadata schema = rootWriter.tupleSchema();
    assertEquals(0, rsLoader.schemaVersion());

    // No columns defined in schema

    assertNull(schema.metadata("a"));
    try {
      schema.column(0);
      fail();
    } catch (IndexOutOfBoundsException e) {
      // Expected
    }

    // No columns defined in writer

    try {
      rootWriter.column("a");
      fail();
    } catch (UndefinedColumnException e) {
      // Expected
    }
    try {
      rootWriter.column(0);
      fail();
    } catch (IndexOutOfBoundsException e) {
      // Expected
    }

    // Define a column

    assertEquals(0, rsLoader.schemaVersion());
    MaterializedField colSchema = SchemaBuilder.columnSchema("a", MinorType.VARCHAR, DataMode.REQUIRED);
    rootWriter.addColumn(colSchema);
    assertEquals(1, rsLoader.schemaVersion());

    // Can now be found, case insensitive

    assertTrue(colSchema.isEquivalent(schema.column(0)));
    ColumnMetadata colMetadata = schema.metadata(0);
    assertSame(colMetadata, schema.metadata("a"));
    assertSame(colMetadata, schema.metadata("A"));
    assertNotNull(rootWriter.column(0));
    assertNotNull(rootWriter.column("a"));
    assertNotNull(rootWriter.column("A"));
    assertEquals(1, schema.size());
    assertEquals(0, schema.index("a"));
    assertEquals(0, schema.index("A"));

    // Reject a duplicate name, case insensitive

    try {
      rootWriter.addColumn(colSchema);
      fail();
    } catch(UserException e) {
      // Expected
    }
    try {
      MaterializedField testCol = SchemaBuilder.columnSchema("A", MinorType.VARCHAR, DataMode.REQUIRED);
      rootWriter.addColumn(testCol);
      fail();
    } catch (UserException e) {
      // Expected
      assertTrue(e.getMessage().contains("Duplicate"));
    }

    // Can still add required fields while writing the first row.

    rsLoader.startBatch();
    rootWriter.start();
    rootWriter.scalar(0).setString("foo");

    MaterializedField col2 = SchemaBuilder.columnSchema("b", MinorType.VARCHAR, DataMode.REQUIRED);
    rootWriter.addColumn(col2);
    assertEquals(2, rsLoader.schemaVersion());
    assertTrue(col2.isEquivalent(schema.column(1)));
    ColumnMetadata col2Metadata = schema.metadata(1);
    assertSame(col2Metadata, schema.metadata("b"));
    assertSame(col2Metadata, schema.metadata("B"));
    assertEquals(2, schema.size());
    assertEquals(1, schema.index("b"));
    assertEquals(1, schema.index("B"));
    rootWriter.scalar(1).setString("second");

    // After first row, can add an optional or repeated.
    // Also allows a required field: values will be back-filled.

    rootWriter.save();
    rootWriter.start();
    rootWriter.scalar(0).setString("bar");
    rootWriter.scalar(1).setString("");

    MaterializedField col3 = SchemaBuilder.columnSchema("c", MinorType.VARCHAR, DataMode.REQUIRED);
    rootWriter.addColumn(col3);
    assertEquals(3, rsLoader.schemaVersion());
    assertTrue(col3.isEquivalent(schema.column(2)));
    ColumnMetadata col3Metadata = schema.metadata(2);
    assertSame(col3Metadata, schema.metadata("c"));
    assertSame(col3Metadata, schema.metadata("C"));
    assertEquals(3, schema.size());
    assertEquals(2, schema.index("c"));
    assertEquals(2, schema.index("C"));
    rootWriter.scalar("c").setString("c.2");

    MaterializedField col4 = SchemaBuilder.columnSchema("d", MinorType.VARCHAR, DataMode.OPTIONAL);
    rootWriter.addColumn(col4);
    assertEquals(4, rsLoader.schemaVersion());
    assertTrue(col4.isEquivalent(schema.column(3)));
    ColumnMetadata col4Metadata = schema.metadata(3);
    assertSame(col4Metadata, schema.metadata("d"));
    assertSame(col4Metadata, schema.metadata("D"));
    assertEquals(4, schema.size());
    assertEquals(3, schema.index("d"));
    assertEquals(3, schema.index("D"));
    rootWriter.scalar("d").setString("d.2");

    MaterializedField col5 = SchemaBuilder.columnSchema("e", MinorType.VARCHAR, DataMode.REPEATED);
    rootWriter.addColumn(col5);
    assertEquals(5, rsLoader.schemaVersion());
    assertTrue(col5.isEquivalent(schema.column(4)));
    ColumnMetadata col5Metadata = schema.metadata(4);
    assertSame(col5Metadata, schema.metadata("e"));
    assertSame(col5Metadata, schema.metadata("E"));
    assertEquals(5, schema.size());
    assertEquals(4, schema.index("e"));
    assertEquals(4, schema.index("E"));
    rootWriter.array(4).setObject(strArray("e1", "e2", "e3"));

    MaterializedField col6 = SchemaBuilder.columnSchema("f", MinorType.BIGINT, DataMode.REPEATED);
    rootWriter.addColumn(col6);
    assertEquals(6, rsLoader.schemaVersion());
    assertTrue(col6.isEquivalent(schema.column(5)));
    ColumnMetadata col6Metadata = schema.metadata(5);
    assertSame(col6Metadata, schema.metadata("f"));
    assertSame(col6Metadata, schema.metadata("F"));
    assertEquals(6, schema.size());
    assertEquals(5, schema.index("f"));
    assertEquals(5, schema.index("F"));
    rootWriter.array(5).setObject(new Long[] { Long.MIN_VALUE, Long.MAX_VALUE });

    MaterializedField col7 = SchemaBuilder.columnSchema("g", MinorType.INT, DataMode.REPEATED);
    rootWriter.addColumn(col7);
    assertEquals(7, rsLoader.schemaVersion());
    assertTrue(col7.isEquivalent(schema.column(6)));
    ColumnMetadata col7Metadata = schema.metadata(6);
    assertSame(col7Metadata, schema.metadata("g"));
    assertSame(col7Metadata, schema.metadata("G"));
    assertEquals(7, schema.size());
    assertEquals(6, schema.index("g"));
    assertEquals(6, schema.index("G"));
    rootWriter.array(6).setObject(new Integer[] { Integer.MIN_VALUE, Integer.MAX_VALUE });

    MaterializedField col8 = SchemaBuilder.columnSchema("h", MinorType.INT, DataMode.REPEATED);
    rootWriter.addColumn(col8);
    assertEquals(8, rsLoader.schemaVersion());
    assertTrue(col8.isEquivalent(schema.column(7)));
    ColumnMetadata col8Metadata = schema.metadata(7);
    assertSame(col8Metadata, schema.metadata("h"));
    assertSame(col8Metadata, schema.metadata("H"));
    assertEquals(8, schema.size());
    assertEquals(7, schema.index("h"));
    assertEquals(7, schema.index("H"));
    rootWriter.array(7).setObject(new Short[] { Short.MIN_VALUE, Short.MAX_VALUE });

    MaterializedField col9 = SchemaBuilder.columnSchema("i", MinorType.INT, DataMode.REPEATED);
    rootWriter.addColumn(col9);
    assertEquals(9, rsLoader.schemaVersion());
    assertTrue(col9.isEquivalent(schema.column(8)));
    ColumnMetadata col9Metadata = schema.metadata(8);
    assertSame(col9Metadata, schema.metadata("i"));
    assertSame(col9Metadata, schema.metadata("I"));
    assertEquals(9, schema.size());
    assertEquals(8, schema.index("i"));
    assertEquals(8, schema.index("I"));
    rootWriter.array(8).setObject(new Byte[] { Byte.MIN_VALUE, Byte.MAX_VALUE });

    MaterializedField col10 = SchemaBuilder.columnSchema("j", MinorType.FLOAT8, DataMode.REPEATED);
    rootWriter.addColumn(col10);
    assertEquals(10, rsLoader.schemaVersion());
    assertTrue(col10.isEquivalent(schema.column(9)));
    ColumnMetadata col10Metadata = schema.metadata(9);
    assertSame(col10Metadata, schema.metadata("j"));
    assertSame(col10Metadata, schema.metadata("J"));
    assertEquals(10, schema.size());
    assertEquals(9, schema.index("j"));
    assertEquals(9, schema.index("J"));
    rootWriter.array(9).setObject(new Double[] { Double.MIN_VALUE, Double.MAX_VALUE });

    MaterializedField col11 = SchemaBuilder.columnSchema("k", MinorType.FLOAT4, DataMode.REPEATED);
    rootWriter.addColumn(col11);
    assertEquals(11, rsLoader.schemaVersion());
    assertTrue(col11.isEquivalent(schema.column(10)));
    ColumnMetadata col11Metadata = schema.metadata(10);
    assertSame(col11Metadata, schema.metadata("k"));
    assertSame(col11Metadata, schema.metadata("K"));
    assertEquals(11, schema.size());
    assertEquals(10, schema.index("k"));
    assertEquals(10, schema.index("K"));
    rootWriter.array(10).setObject(new Float[] { Float.MIN_VALUE, Float.MAX_VALUE });

    MaterializedField col12 = SchemaBuilder.columnSchema("l", MinorType.BIT, DataMode.REPEATED);
    rootWriter.addColumn(col12);
    assertEquals(12, rsLoader.schemaVersion());
    assertTrue(col12.isEquivalent(schema.column(11)));
    ColumnMetadata col12Metadata = schema.metadata(11);
    assertSame(col12Metadata, schema.metadata("l"));
    assertSame(col12Metadata, schema.metadata("L"));
    assertEquals(12, schema.size());
    assertEquals(11, schema.index("l"));
    assertEquals(11, schema.index("L"));
    rootWriter.array(11).setObject(new Boolean[] { Boolean.TRUE, Boolean.FALSE });
    rootWriter.save();

    // Verify. No reason to expect problems, but might as well check.

    RowSet result = fixture.wrap(rsLoader.harvest());
    assertEquals(12, rsLoader.schemaVersion());
    SingleRowSet expected = fixture.rowSetBuilder(result.batchSchema())
        .addRow("foo", "second", "", null,
            strArray(),
            longArray(),
            intArray(),
            shortArray(),
            byteArray(),
            doubleArray(),
            floatArray(),
            boolArray())
        .addRow("bar", "", "c.2", "d.2",
            strArray("e1", "e2", "e3"),
            longArray(Long.MIN_VALUE, Long.MAX_VALUE),
            intArray(Integer.MIN_VALUE, Integer.MAX_VALUE),
            shortArray(Short.MIN_VALUE, Short.MAX_VALUE),
            byteArray((int) Byte.MIN_VALUE, (int) Byte.MAX_VALUE),
            doubleArray(Double.MIN_VALUE, Double.MAX_VALUE),
            floatArray(Float.MIN_VALUE, Float.MAX_VALUE),
            boolArray(Boolean.TRUE, Boolean.FALSE))
        .build();
    RowSetUtilities.verify(expected, result);

    // Handy way to test that close works to abort an in-flight batch
    // and clean up.

    rsLoader.close();
  }

  /**
   * Provide a schema up front to the loader; schema is built before
   * the first row.
   * <p>
   * Also verifies the test-time method to set a row of values using
   * a single method.
   */
  @Test
  public void testInitialSchema() {
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addNullable("b", MinorType.INT)
        .add("c", MinorType.VARCHAR)
        .buildSchema();
    ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .readerSchema(schema)
        .build();
    ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    RowSetLoader rootWriter = rsLoader.writer();

    rsLoader.startBatch();
    rootWriter
        .addRow(10, 100, "fred")
        .addRow(20, null, "barney")
        .addRow(30, 300, "wilma");
    RowSet actual = fixture.wrap(rsLoader.harvest());

    RowSet expected = fixture.rowSetBuilder(schema)
        .addRow(10, 100, "fred")
        .addRow(20, null, "barney")
        .addRow(30, 300, "wilma")
        .build();

    RowSetUtilities.verify(expected, actual);
    rsLoader.close();
  }

  /**
   * The writer protocol allows a client to write to a row any number of times
   * before invoking {@code save()}. In this case, each new value simply
   * overwrites the previous value. Here, we test the most basic case: a simple,
   * flat tuple with no arrays. We use a very large Varchar that would, if
   * overwrite were not working, cause vector overflow.
   * <p>
   * The ability to overwrite rows is seldom needed except in one future use
   * case: writing a row, then applying a filter "in-place" to discard unwanted
   * rows, without having to send the row downstream.
   * <p>
   * Because of this use case, specific rules apply when discarding row or
   * overwriting values.
   * <ul>
   * <li>Values can be written once per row. Fixed-width columns actually allow
   * multiple writes. But, because of the way variable-width columns work,
   * multiple writes will cause undefined results.</li>
   * <li>To overwrite a row, call <tt>start()</tt> without calling
   * <tt>save()</tt> on the previous row. Doing so ignores data for the
   * previous row and starts a new row in place of the old one.</li>
   * </ul>
   * Note that there is no explicit method to discard a row. Instead,
   * the rule is that a row is not saved until <tt>save()</tt> is called.
   */
  @Test
  public void testOverwriteRow() {
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.VARCHAR)
        .buildSchema();
    ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .readerSchema(schema)
        .rowCountLimit(ValueVector.MAX_ROW_COUNT)
        .build();
    ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    RowSetLoader rootWriter = rsLoader.writer();

    // Can't use the shortcut to populate rows when doing overwrites.

    ScalarWriter aWriter = rootWriter.scalar("a");
    ScalarWriter bWriter = rootWriter.scalar("b");

    // Write 100,000 rows, overwriting 99% of them. This will cause vector
    // overflow and data corruption if overwrite does not work; but will happily
    // produce the correct result if everything works as it should.

    byte value[] = new byte[512];
    Arrays.fill(value, (byte) 'X');
    int count = 0;
    rsLoader.startBatch();
    while (count < 100_000) {
      rootWriter.start();
      count++;
      aWriter.setInt(count);
      bWriter.setBytes(value, value.length);
      if (count % 100 == 0) {
        rootWriter.save();
      }
    }

    // Verify using a reader.

    RowSet result = fixture.wrap(rsLoader.harvest());
    assertEquals(count / 100, result.rowCount());
    RowSetReader reader = result.reader();
    int rowId = 1;
    while (reader.next()) {
      assertEquals(rowId * 100, reader.scalar("a").getInt());
      assertTrue(Arrays.equals(value, reader.scalar("b").getBytes()));
      rowId++;
    }

    result.clear();
    rsLoader.close();
  }

  /**
   * Test that memory is released if the loader is closed with an active
   * batch (that is, before the batch is harvested.)
   */
  @Test
  public void testCloseWithoutHarvest() {
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.VARCHAR)
        .buildSchema();
    ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .readerSchema(schema)
        .rowCountLimit(ValueVector.MAX_ROW_COUNT)
        .build();
    ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    RowSetLoader rootWriter = rsLoader.writer();

    rsLoader.startBatch();
    for (int i = 0; i < 100; i++) {
      rootWriter.start();
      rootWriter.scalar("a").setInt(i);
      rootWriter.scalar("b").setString("b-" + i);
      rootWriter.save();
    }

    // Don't harvest the batch. Allocator will complain if the
    // loader does not release memory.

    rsLoader.close();
  }
}
