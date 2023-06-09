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

import static org.apache.drill.test.rowSet.RowSetUtilities.intArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.mapValue;
import static org.apache.drill.test.rowSet.RowSetUtilities.strArray;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.accessor.TupleReader;
import org.apache.drill.exec.vector.accessor.TupleWriter;
import org.apache.drill.exec.vector.complex.MapVector;
import org.apache.drill.test.SubOperatorTest;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.exec.physical.rowSet.RowSetReader;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;
import org.junit.experimental.categories.Category;


/**
 * Test (non-array) map support in the result set loader and related classes.
 */
@Category(RowSetTests.class)
public class TestResultSetLoaderMaps extends SubOperatorTest {

  @Test
  public void testBasics() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("c", MinorType.INT)
          .add("d", MinorType.VARCHAR)
          .resumeSchema()
        .add("e", MinorType.VARCHAR)
        .buildSchema();
    final ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .readerSchema(schema)
        .build();
    final ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    assertFalse(rsLoader.isProjectionEmpty());
    final RowSetLoader rootWriter = rsLoader.writer();

    // Verify structure and schema
    assertEquals(5, rsLoader.schemaVersion());
    final TupleMetadata actualSchema = rootWriter.tupleSchema();
    assertEquals(3, actualSchema.size());
    assertTrue(actualSchema.metadata(1).isMap());
    assertEquals(2, actualSchema.metadata("m").tupleSchema().size());
    assertEquals(2, actualSchema.column("m").getChildren().size());

    rsLoader.startBatch();

    // Write a row the way that clients will do.
    final ScalarWriter aWriter = rootWriter.scalar("a");
    final TupleWriter mWriter = rootWriter.tuple("m");
    final ScalarWriter cWriter = mWriter.scalar("c");
    final ScalarWriter dWriter = mWriter.scalar("d");
    final ScalarWriter eWriter = rootWriter.scalar("e");

    rootWriter.start();
    aWriter.setInt(10);
    cWriter.setInt(110);
    dWriter.setString("fred");
    eWriter.setString("pebbles");
    rootWriter.save();

    // Try adding a duplicate column.
    try {
      mWriter.addColumn(SchemaBuilder.columnSchema("c", MinorType.INT, DataMode.OPTIONAL));
      fail();
    } catch (final UserException e) {
      // Expected
    }

    // Write another using the test-time conveniences
    rootWriter.addRow(20, mapValue(210, "barney"), "bam-bam");

    // Harvest the batch
    final RowSet actual = fixture.wrap(rsLoader.harvest());
    assertEquals(5, rsLoader.schemaVersion());
    assertEquals(2, actual.rowCount());
    final MapVector mapVector = (MapVector) actual.container().getValueVector(1).getValueVector();
    assertEquals(2, mapVector.getAccessor().getValueCount());

    // Validate data
    final SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addRow(10, mapValue(110, "fred"), "pebbles")
        .addRow(20, mapValue(210, "barney"), "bam-bam")
        .build();

    RowSetUtilities.verify(expected, actual);
    rsLoader.close();
  }

  /**
   * Create schema with a map, then add columns to the map
   * after delivering the first batch. The new columns should appear
   * in the second-batch output.
   */
  @Test
  public void testMapEvolution() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("b", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();
    final ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .readerSchema(schema)
        .build();
    final ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    assertEquals(3, rsLoader.schemaVersion());
    final RowSetLoader rootWriter = rsLoader.writer();

    rsLoader.startBatch();
    rootWriter
      .addRow(10, mapValue("fred"))
      .addRow(20, mapValue("barney"));

    RowSet actual = fixture.wrap(rsLoader.harvest());
    assertEquals(3, rsLoader.schemaVersion());
    assertEquals(2, actual.rowCount());

    // Validate first batch
    SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addRow(10, mapValue("fred"))
        .addRow(20, mapValue("barney"))
        .build();

    RowSetUtilities.verify(expected, actual);

    // Add three columns in the second batch. One before
    // the batch starts, one before the first row, and one after
    // the first row.
    final TupleWriter mapWriter = rootWriter.tuple("m");
    mapWriter.addColumn(SchemaBuilder.columnSchema("c", MinorType.INT, DataMode.REQUIRED));

    rsLoader.startBatch();
    mapWriter.addColumn(SchemaBuilder.columnSchema("d", MinorType.BIGINT, DataMode.REQUIRED));

    rootWriter.addRow(30, mapValue("wilma", 130, 130_000L));

    mapWriter.addColumn(SchemaBuilder.columnSchema("e", MinorType.VARCHAR, DataMode.REQUIRED));
    rootWriter.addRow(40, mapValue("betty", 140, 140_000L, "bam-bam"));

    actual = fixture.wrap(rsLoader.harvest());
    assertEquals(6, rsLoader.schemaVersion());
    assertEquals(2, actual.rowCount());

    // Validate first batch
    final TupleMetadata expectedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("b", MinorType.VARCHAR)
          .add("c", MinorType.INT)
          .add("d", MinorType.BIGINT)
          .add("e", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();
    expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(30, mapValue("wilma", 130, 130_000L, ""))
        .addRow(40, mapValue("betty", 140, 140_000L, "bam-bam"))
        .build();

    RowSetUtilities.verify(expected, actual);
    rsLoader.close();
  }

  /**
   * Test adding a map to a loader after writing the first row.
   */
  @Test
  public void testMapAddition() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .buildSchema();
    final ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .readerSchema(schema)
        .build();
    final ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    assertEquals(1, rsLoader.schemaVersion());
    final RowSetLoader rootWriter = rsLoader.writer();

    // Start without the map. Add a map after the first row.
    rsLoader.startBatch();
    rootWriter.addRow(10);

    final int mapIndex = rootWriter.addColumn(SchemaBuilder.columnSchema("m", MinorType.MAP, DataMode.REQUIRED));
    final TupleWriter mapWriter = rootWriter.tuple(mapIndex);

    // Add a column to the map with the same name as the top-level column.
    // Verifies that the name spaces are independent.
    final int colIndex = mapWriter.addColumn(SchemaBuilder.columnSchema("a", MinorType.VARCHAR, DataMode.REQUIRED));
    assertEquals(0, colIndex);

    // Ensure metadata was added
    assertTrue(mapWriter.tupleSchema().size() == 1);
    assertSame(mapWriter.tupleSchema(), mapWriter.schema().tupleSchema());
    assertSame(mapWriter.tupleSchema().metadata(colIndex), mapWriter.scalar(colIndex).schema());

    rootWriter
      .addRow(20, mapValue("fred"))
      .addRow(30, mapValue("barney"));

    final RowSet actual = fixture.wrap(rsLoader.harvest());
    assertEquals(3, rsLoader.schemaVersion());
    assertEquals(3, actual.rowCount());

    final MapVector mapVector = (MapVector) actual.container().getValueVector(1).getValueVector();
    final MaterializedField mapField = mapVector.getField();
    assertEquals(1, mapField.getChildren().size());
    assertTrue(mapWriter.scalar(colIndex).schema().schema().isEquivalent(
        mapField.getChildren().iterator().next()));

    // Validate first batch
    final TupleMetadata expectedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("a", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();
    final SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(10, mapValue(""))
        .addRow(20, mapValue("fred"))
        .addRow(30, mapValue("barney"))
        .build();

    RowSetUtilities.verify(expected, actual);
    rsLoader.close();
  }

  /**
   * Test adding an empty map to a loader after writing the first row.
   * Then add columns in another batch. Yes, this is a bizarre condition,
   * but we must check it anyway for robustness.
   */
  @Test
  public void testEmptyMapAddition() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .buildSchema();
    final ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .readerSchema(schema)
        .build();
    final ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    assertEquals(1, rsLoader.schemaVersion());
    final RowSetLoader rootWriter = rsLoader.writer();

    // Start without the map. Add a map after the first row.
    rsLoader.startBatch();
    rootWriter.addRow(10);

    final int mapIndex = rootWriter.addColumn(SchemaBuilder.columnSchema("m", MinorType.MAP, DataMode.REQUIRED));
    final TupleWriter mapWriter = rootWriter.tuple(mapIndex);

    rootWriter
      .addRow(20, mapValue())
      .addRow(30, mapValue());

    RowSet actual = fixture.wrap(rsLoader.harvest());
    assertEquals(2, rsLoader.schemaVersion());
    assertEquals(3, actual.rowCount());

    // Validate first batch
    TupleMetadata expectedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .resumeSchema()
        .buildSchema();
    SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(10, mapValue())
        .addRow(20, mapValue())
        .addRow(30, mapValue())
        .build();

    RowSetUtilities.verify(expected, actual);

    // Now add another column to the map
    rsLoader.startBatch();
    mapWriter.addColumn(SchemaBuilder.columnSchema("a", MinorType.VARCHAR, DataMode.REQUIRED));

    rootWriter
      .addRow(40, mapValue("fred"))
      .addRow(50, mapValue("barney"));

    actual = fixture.wrap(rsLoader.harvest());
    assertEquals(3, rsLoader.schemaVersion());
    assertEquals(2, actual.rowCount());

    // Validate first batch
    expectedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("a", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();
    expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(40, mapValue("fred"))
        .addRow(50, mapValue("barney"))
        .build();

    RowSetUtilities.verify(expected, actual);
    rsLoader.close();
  }

  /**
   * Create nested maps. Then, add columns to each map
   * on the fly. Use required, variable-width columns since
   * those require the most processing and are most likely to
   * fail if anything is out of place.
   */
  @Test
  public void testNestedMapsRequired() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m1")
          .add("b", MinorType.VARCHAR)
          .addMap("m2")
            .add("c", MinorType.VARCHAR)
            .resumeMap()
          .resumeSchema()
        .buildSchema();
    final ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .readerSchema(schema)
        .build();
    final ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    assertEquals(5, rsLoader.schemaVersion());
    final RowSetLoader rootWriter = rsLoader.writer();

    rsLoader.startBatch();
    rootWriter.addRow(10, mapValue("b1", mapValue("c1")));

    // Validate first batch
    RowSet actual = fixture.wrap(rsLoader.harvest());
    assertEquals(5, rsLoader.schemaVersion());
    SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addRow(10, mapValue("b1", mapValue("c1")))
        .build();

    RowSetUtilities.verify(expected, actual);

    // Now add columns in the second batch.
    rsLoader.startBatch();
    rootWriter.addRow(20, mapValue("b2", mapValue("c2")));

    final TupleWriter m1Writer = rootWriter.tuple("m1");
    m1Writer.addColumn(SchemaBuilder.columnSchema("d", MinorType.VARCHAR, DataMode.REQUIRED));
    final TupleWriter m2Writer = m1Writer.tuple("m2");
    m2Writer.addColumn(SchemaBuilder.columnSchema("e", MinorType.VARCHAR, DataMode.REQUIRED));

    rootWriter.addRow(30, mapValue("b3", mapValue("c3", "e3"), "d3"));

    // And another set while the write proceeds.
    m1Writer.addColumn(SchemaBuilder.columnSchema("f", MinorType.VARCHAR, DataMode.REQUIRED));
    m2Writer.addColumn(SchemaBuilder.columnSchema("g", MinorType.VARCHAR, DataMode.REQUIRED));

    rootWriter.addRow(40, mapValue("b4", mapValue("c4", "e4", "g4"), "d4", "e4"));

    // Validate second batch
    actual = fixture.wrap(rsLoader.harvest());
    assertEquals(9, rsLoader.schemaVersion());

    final TupleMetadata expectedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m1")
          .add("b", MinorType.VARCHAR)
          .addMap("m2")
            .add("c", MinorType.VARCHAR)
            .add("e", MinorType.VARCHAR)
            .add("g", MinorType.VARCHAR)
            .resumeMap()
          .add("d", MinorType.VARCHAR)
          .add("f", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();
    expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(20, mapValue("b2", mapValue("c2", "",   ""  ), "",    "" ))
        .addRow(30, mapValue("b3", mapValue("c3", "e3", ""  ), "d3",  "" ))
        .addRow(40, mapValue("b4", mapValue("c4", "e4", "g4"), "d4", "e4"))
        .build();

    RowSetUtilities.verify(expected, actual);
    rsLoader.close();
  }

  /**
   * Create nested maps. Then, add columns to each map
   * on the fly. This time, with nullable types.
   */
  @Test
  public void testNestedMapsNullable() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m1")
          .addNullable("b", MinorType.VARCHAR)
          .addMap("m2")
            .addNullable("c", MinorType.VARCHAR)
            .resumeMap()
          .resumeSchema()
        .buildSchema();
    final ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .readerSchema(schema)
        .build();
    final ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    final RowSetLoader rootWriter = rsLoader.writer();

    rsLoader.startBatch();
    rootWriter.addRow(10, mapValue("b1", mapValue("c1")));

    // Validate first batch
    RowSet actual = fixture.wrap(rsLoader.harvest());
    SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addRow(10, mapValue("b1", mapValue("c1")))
        .build();

    RowSetUtilities.verify(expected, actual);

    // Now add columns in the second batch.
    rsLoader.startBatch();
    rootWriter.addRow(20, mapValue("b2", mapValue("c2")));

    final TupleWriter m1Writer = rootWriter.tuple("m1");
    m1Writer.addColumn(SchemaBuilder.columnSchema("d", MinorType.VARCHAR, DataMode.OPTIONAL));
    final TupleWriter m2Writer = m1Writer.tuple("m2");
    m2Writer.addColumn(SchemaBuilder.columnSchema("e", MinorType.VARCHAR, DataMode.OPTIONAL));

    rootWriter.addRow(30, mapValue("b3", mapValue("c3", "e3"), "d3"));

    // And another set while the write proceeds.
    m1Writer.addColumn(SchemaBuilder.columnSchema("f", MinorType.VARCHAR, DataMode.OPTIONAL));
    m2Writer.addColumn(SchemaBuilder.columnSchema("g", MinorType.VARCHAR, DataMode.OPTIONAL));

    rootWriter.addRow(40, mapValue("b4", mapValue("c4", "e4", "g4"), "d4", "e4"));

    // Validate second batch
    actual = fixture.wrap(rsLoader.harvest());
    final TupleMetadata expectedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m1")
          .addNullable("b", MinorType.VARCHAR)
          .addMap("m2")
            .addNullable("c", MinorType.VARCHAR)
            .addNullable("e", MinorType.VARCHAR)
            .addNullable("g", MinorType.VARCHAR)
            .resumeMap()
          .addNullable("d", MinorType.VARCHAR)
          .addNullable("f", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();
    expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(20, mapValue("b2", mapValue("c2", null, null), null, null))
        .addRow(30, mapValue("b3", mapValue("c3", "e3", null), "d3", null))
        .addRow(40, mapValue("b4", mapValue("c4", "e4", "g4"), "d4", "e4"))
        .build();

    RowSetUtilities.verify(expected, actual);
    rsLoader.close();
  }

  /**
   * Test a map that contains a scalar array. No reason to suspect that this
   * will have problem as the array writer is fully tested in the accessor
   * subsystem. Still, need to test the cardinality methods of the loader
   * layer.
   */
  @Test
  public void testMapWithArray() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .addArray("c", MinorType.INT)
          .addArray("d", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();
    final ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .readerSchema(schema)
        .build();
    final ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    final RowSetLoader rootWriter = rsLoader.writer();

    // Write some rows
    rsLoader.startBatch();
    rootWriter
      .addRow(10, mapValue(intArray(110, 120, 130),
                           strArray("d1.1", "d1.2", "d1.3", "d1.4")))
      .addRow(20, mapValue(intArray(210), strArray()))
      .addRow(30, mapValue(intArray(), strArray("d3.1")));

    // Validate first batch
    RowSet actual = fixture.wrap(rsLoader.harvest());
    SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addRow(10, mapValue(intArray(110, 120, 130),
                             strArray("d1.1", "d1.2", "d1.3", "d1.4")))
        .addRow(20, mapValue(intArray(210), strArray()))
        .addRow(30, mapValue(intArray(), strArray("d3.1")))
        .build();

    RowSetUtilities.verify(expected, actual);

    // Add another array after the first row in the second batch.
    rsLoader.startBatch();
    rootWriter
      .addRow(40, mapValue(intArray(410, 420), strArray("d4.1", "d4.2")))
      .addRow(50, mapValue(intArray(510), strArray("d5.1")));

    final TupleWriter mapWriter = rootWriter.tuple("m");
    mapWriter.addColumn(SchemaBuilder.columnSchema("e", MinorType.VARCHAR, DataMode.REPEATED));
    rootWriter
      .addRow(60, mapValue(intArray(610, 620), strArray("d6.1", "d6.2"), strArray("e6.1", "e6.2")))
      .addRow(70, mapValue(intArray(710), strArray(), strArray("e7.1", "e7.2")));

    // Validate first batch. The new array should have been back-filled with
    // empty offsets for the missing rows.
    actual = fixture.wrap(rsLoader.harvest());
    expected = fixture.rowSetBuilder(actual.schema())
        .addRow(40, mapValue(intArray(410, 420), strArray("d4.1", "d4.2"), strArray()))
        .addRow(50, mapValue(intArray(510), strArray("d5.1"), strArray()))
        .addRow(60, mapValue(intArray(610, 620), strArray("d6.1", "d6.2"), strArray("e6.1", "e6.2")))
        .addRow(70, mapValue(intArray(710), strArray(), strArray("e7.1", "e7.2")))
        .build();

    RowSetUtilities.verify(expected, actual);
    rsLoader.close();
  }

  /**
   * Create a schema with a map, then trigger an overflow on one of the columns
   * in the map. Proper overflow handling should occur regardless of nesting
   * depth.
   */
  @Test
  public void testMapWithOverflow() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m1")
          .add("b", MinorType.INT)
          .addMap("m2")
            .add("c", MinorType.INT) // Before overflow, written
            .add("d", MinorType.VARCHAR)
            .add("e", MinorType.INT) // After overflow, not yet written
            .resumeMap()
          .resumeSchema()
        .buildSchema();
    final ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .readerSchema(schema)
        .rowCountLimit(ValueVector.MAX_ROW_COUNT)
        .build();
    final ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    final RowSetLoader rootWriter = rsLoader.writer();

    final byte value[] = new byte[512];
    Arrays.fill(value, (byte) 'X');
    int count = 0;
    rsLoader.startBatch();
    while (! rootWriter.isFull()) {
      rootWriter.addRow(count, mapValue(count * 10, mapValue(count * 100, value, count * 1000)));
      count++;
    }

    // Our row count should include the overflow row
    final int expectedCount = ValueVector.MAX_BUFFER_SIZE / value.length;
    assertEquals(expectedCount + 1, count);

    // Loader's row count should include only "visible" rows
    assertEquals(expectedCount, rootWriter.rowCount());

    // Total count should include invisible and look-ahead rows.
    assertEquals(expectedCount + 1, rsLoader.totalRowCount());

    // Result should exclude the overflow row
    RowSet result = fixture.wrap(rsLoader.harvest());
    assertEquals(expectedCount, result.rowCount());

    // Ensure the odd map vector value count variable is set correctly.
    final MapVector m1Vector = (MapVector) result.container().getValueVector(1).getValueVector();
    assertEquals(expectedCount, m1Vector.getAccessor().getValueCount());
    final MapVector m2Vector = (MapVector) m1Vector.getChildByOrdinal(1);
    assertEquals(expectedCount, m2Vector.getAccessor().getValueCount());

    result.clear();

    // Next batch should start with the overflow row
    rsLoader.startBatch();
    assertEquals(1, rootWriter.rowCount());
    assertEquals(expectedCount + 1, rsLoader.totalRowCount());
    result = fixture.wrap(rsLoader.harvest());
    assertEquals(1, result.rowCount());
    result.clear();

    rsLoader.close();
  }

  /**
   * Test the case in which a new column is added during the overflow row. Unlike
   * the top-level schema case, internally we must create a copy of the map, and
   * move vectors across only when the result is to include the schema version
   * of the target column. For overflow, the new column is added after the
   * first batch; it is added in the second batch that contains the overflow
   * row in which the column was added.
   */
  @Test
  public void testMapOverflowWithNewColumn() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("b", MinorType.INT)
          .add("c", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();
    final ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .readerSchema(schema)
        .rowCountLimit(ValueVector.MAX_ROW_COUNT)
        .build();
    final ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    assertEquals(4, rsLoader.schemaVersion());
    final RowSetLoader rootWriter = rsLoader.writer();

    // Can't use the shortcut to populate rows when doing a schema
    // change.
    final ScalarWriter aWriter = rootWriter.scalar("a");
    final TupleWriter mWriter = rootWriter.tuple("m");
    final ScalarWriter bWriter = mWriter.scalar("b");
    final ScalarWriter cWriter = mWriter.scalar("c");

    final byte value[] = new byte[512];
    Arrays.fill(value, (byte) 'X');
    int count = 0;
    rsLoader.startBatch();
    while (! rootWriter.isFull()) {
      rootWriter.start();
      aWriter.setInt(count);
      bWriter.setInt(count * 10);
      cWriter.setBytes(value, value.length);
      if (rootWriter.isFull()) {

        // Overflow just occurred. Add another column.
        mWriter.addColumn(SchemaBuilder.columnSchema("d", MinorType.INT, DataMode.OPTIONAL));
        mWriter.scalar("d").setInt(count * 100);
      }
      rootWriter.save();
      count++;
    }

    // Result set should include the original columns, but not d.
    RowSet result = fixture.wrap(rsLoader.harvest());

    assertEquals(4, rsLoader.schemaVersion());
    assertTrue(schema.isEquivalent(result.schema()));
    final BatchSchema expectedSchema = new BatchSchema(SelectionVectorMode.NONE, schema.toFieldList());
    assertTrue(expectedSchema.isEquivalent(result.batchSchema()));

    // Use a reader to validate row-by-row. Too large to create an expected
    // result set.
    RowSetReader reader = result.reader();
    TupleReader mapReader = reader.tuple("m");
    int rowId = 0;
    while (reader.next()) {
      assertEquals(rowId, reader.scalar("a").getInt());
      assertEquals(rowId * 10, mapReader.scalar("b").getInt());
      assertTrue(Arrays.equals(value, mapReader.scalar("c").getBytes()));
      rowId++;
    }
    result.clear();

    // Next batch should start with the overflow row
    rsLoader.startBatch();
    assertEquals(1, rootWriter.rowCount());
    result = fixture.wrap(rsLoader.harvest());
    assertEquals(1, result.rowCount());

    reader = result.reader();
    mapReader = reader.tuple("m");
    while (reader.next()) {
      assertEquals(rowId, reader.scalar("a").getInt());
      assertEquals(rowId * 10, mapReader.scalar("b").getInt());
      assertTrue(Arrays.equals(value, mapReader.scalar("c").getBytes()));
      assertEquals(rowId * 100, mapReader.scalar("d").getInt());
    }
    result.clear();

    rsLoader.close();
  }

  /**
   * Version of the {#link TestResultSetLoaderProtocol#testOverwriteRow()} test
   * that uses nested columns.
   */
  @Test
  public void testOverwriteRow() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("b", MinorType.INT)
          .add("c", MinorType.VARCHAR)
        .resumeSchema()
      .buildSchema();
    final ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .readerSchema(schema)
        .rowCountLimit(ValueVector.MAX_ROW_COUNT)
        .build();
    final ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    final RowSetLoader rootWriter = rsLoader.writer();

    // Can't use the shortcut to populate rows when doing overwrites.
    final ScalarWriter aWriter = rootWriter.scalar("a");
    final TupleWriter mWriter = rootWriter.tuple("m");
    final ScalarWriter bWriter = mWriter.scalar("b");
    final ScalarWriter cWriter = mWriter.scalar("c");

    // Write 100,000 rows, overwriting 99% of them. This will cause vector
    // overflow and data corruption if overwrite does not work; but will happily
    // produce the correct result if everything works as it should.
    final byte value[] = new byte[512];
    Arrays.fill(value, (byte) 'X');
    int count = 0;
    rsLoader.startBatch();
    while (count < 100_000) {
      rootWriter.start();
      count++;
      aWriter.setInt(count);
      bWriter.setInt(count * 10);
      cWriter.setBytes(value, value.length);
      if (count % 100 == 0) {
        rootWriter.save();
      }
    }

    // Verify using a reader.
    final RowSet result = fixture.wrap(rsLoader.harvest());
    assertEquals(count / 100, result.rowCount());
    final RowSetReader reader = result.reader();
    final TupleReader mReader = reader.tuple("m");
    int rowId = 1;
    while (reader.next()) {
      assertEquals(rowId * 100, reader.scalar("a").getInt());
      assertEquals(rowId * 1000, mReader.scalar("b").getInt());
      assertTrue(Arrays.equals(value, mReader.scalar("c").getBytes()));
      rowId++;
    }

    result.clear();
    rsLoader.close();
  }

  /**
   * Verify that map name spaces (and implementations) are
   * independent.
   */
  @Test
  public void testNameSpace() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("a", MinorType.INT)
          .addMap("m")
            .add("a", MinorType.INT)
            .resumeMap()
          .resumeSchema()
        .buildSchema();
    final ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .readerSchema(schema)
        .build();
    final ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    assertFalse(rsLoader.isProjectionEmpty());
    final RowSetLoader rootWriter = rsLoader.writer();

    rsLoader.startBatch();

    // Write a row the way that clients will do.
    final ScalarWriter a1Writer = rootWriter.scalar("a");
    final TupleWriter m1Writer = rootWriter.tuple("m");
    final ScalarWriter a2Writer = m1Writer.scalar("a");
    final TupleWriter m2Writer = m1Writer.tuple("m");
    final ScalarWriter a3Writer = m2Writer.scalar("a");

    rootWriter.start();
    a1Writer.setInt(11);
    a2Writer.setInt(12);
    a3Writer.setInt(13);
    rootWriter.save();

    rootWriter.start();
    a1Writer.setInt(21);
    a2Writer.setInt(22);
    a3Writer.setInt(23);
    rootWriter.save();

    // Try simplified test format
    rootWriter.addRow(31,
        mapValue(32,
            mapValue(33)));

    // Verify
    final RowSet actual = fixture.wrap(rsLoader.harvest());

    final SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addRow(11, mapValue(12, mapValue(13)))
        .addRow(21, mapValue(22, mapValue(23)))
        .addRow(31, mapValue(32, mapValue(33)))
        .build();

    RowSetUtilities.verify(expected, actual);
    rsLoader.close();
  }
}
