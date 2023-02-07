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
import static org.apache.drill.test.rowSet.RowSetUtilities.map;
import static org.apache.drill.test.rowSet.RowSetUtilities.mapValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.impl.validate.BatchValidator;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.DictColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.accessor.DictWriter;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.accessor.TupleWriter;
import org.apache.drill.exec.vector.complex.DictVector;
import org.apache.drill.test.SubOperatorTest;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;

/**
 * Test (non-array) dict support in the result set loader and related classes.
 */
@Category(RowSetTests.class)
public class TestResultSetLoaderDicts extends SubOperatorTest {

  @Test
  public void testBasics() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addDict("d", MinorType.INT)
          .value(MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();
    final ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .readerSchema(schema)
        .build();
    final ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    assertFalse(rsLoader.isProjectionEmpty());
    final RowSetLoader rootWriter = rsLoader.writer();

    // Verify structure and schema
    assertEquals(4, rsLoader.schemaVersion());
    final TupleMetadata actualSchema = rootWriter.tupleSchema();
    assertEquals(2, actualSchema.size());
    assertTrue(actualSchema.metadata(1).isDict());
    assertEquals(2, actualSchema.metadata("d").tupleSchema().size());
    assertEquals(2, actualSchema.column("d").getChildren().size());

    rsLoader.startBatch();

    // Write a row the way that clients will do.
    final ScalarWriter aWriter = rootWriter.scalar("a");
    final DictWriter dictWriter = rootWriter.dict("d");
    final ScalarWriter keyWriter = dictWriter.keyWriter();
    final ScalarWriter valueWriter = dictWriter.valueWriter().scalar();

    rootWriter.start();
    aWriter.setInt(10);

    keyWriter.setInt(110);
    valueWriter.setString("fred");
    dictWriter.save();
    keyWriter.setInt(111);
    valueWriter.setString("george");
    dictWriter.save();

    rootWriter.save();

    // Write another using the test-time conveniences
    rootWriter.addRow(20, map(210, "barney", 211, "bart", 212, "jerry"));

    // Harvest the batch
    final RowSet actual = fixture.wrap(rsLoader.harvest());
    assertEquals(4, rsLoader.schemaVersion());
    assertEquals(2, actual.rowCount());
    final DictVector dictVector = (DictVector) actual.container().getValueVector(1).getValueVector();
    assertEquals(2, dictVector.getAccessor().getValueCount());

    // Validate data
    final SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addRow(10, map(110, "fred", 111, "george"))
        .addRow(20, map(210, "barney", 211, "bart", 212, "jerry"))
        .build();

    RowSetUtilities.verify(expected, actual);
    rsLoader.close();
  }

  /**
   * Test adding a dict to a loader after writing the first row.
   */
  @Test
  public void testDictAddition() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .buildSchema();
    final ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .readerSchema(schema)
        .build();
    final ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    assertEquals(1, rsLoader.schemaVersion());
    final RowSetLoader rootWriter = rsLoader.writer();

    // Start without the dict. Add then add a dict after the first row.
    rsLoader.startBatch();
    rootWriter.addRow(10);

    MaterializedField dictField = SchemaBuilder.columnSchema("d", MinorType.DICT, DataMode.REQUIRED);
    dictField.addChild(SchemaBuilder.columnSchema(DictVector.FIELD_KEY_NAME, MinorType.VARCHAR, DataMode.REQUIRED));
    dictField.addChild(SchemaBuilder.columnSchema(DictVector.FIELD_VALUE_NAME, MinorType.VARCHAR, DataMode.REQUIRED));
    DictColumnMetadata dictMetadata = MetadataUtils.newDict(dictField);

    final int dictIndex = rootWriter.addColumn(dictMetadata);
    final DictWriter dictWriter = rootWriter.dict(dictIndex);

    // Ensure metadata was added
    final TupleMetadata actualSchema = rootWriter.tupleSchema();
    assertTrue(actualSchema.metadata(1).isDict());
    assertEquals(2, actualSchema.metadata("d").tupleSchema().size());
    assertEquals(2, actualSchema.column("d").getChildren().size());
    assertEquals(2, actualSchema.size());
    assertEquals(2, dictWriter.schema().tupleSchema().size());

    rootWriter.addRow(20, map("name", "fred", "lastname", "smith"))
        .addRow(30, map("name", "barney", "lastname", "johnson"));

    final RowSet actual = fixture.wrap(rsLoader.harvest());
    assertEquals(4, rsLoader.schemaVersion());
    assertEquals(3, actual.rowCount());

    final DictVector dictVector = (DictVector) actual.container().getValueVector(1).getValueVector();
    assertEquals(2, dictVector.getField().getChildren().size());

    // Validate first batch
    final TupleMetadata expectedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addDict("d", MinorType.VARCHAR)
          .value(MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();
    final SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(10, map())
        .addRow(20, map("name", "fred", "lastname", "smith"))
        .addRow(30, map("name", "barney", "lastname", "johnson"))
        .build();

    RowSetUtilities.verify(expected, actual);
    rsLoader.close();
  }

  /**
   * Create dict with map value. Then, add columns to the map
   * on the fly. Use required, variable-width columns since
   * those require the most processing and are most likely to
   * fail if anything is out of place.
   */
  @Test
  public void testMapValueRequiredFields() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addDict("d", MinorType.VARCHAR)
          .mapValue()
            .add("b", MinorType.VARCHAR)
            .resumeDict()
          .resumeSchema()
        .buildSchema();
    final ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .readerSchema(schema)
        .build();
    final ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    assertEquals(5, rsLoader.schemaVersion());
    final RowSetLoader rootWriter = rsLoader.writer();

    rsLoader.startBatch();
    rootWriter.addRow(10, map("a", mapValue("c1"), "b", mapValue("c2")));

    // Validate first batch
    RowSet actual = fixture.wrap(rsLoader.harvest());
    assertEquals(5, rsLoader.schemaVersion());
    SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addRow(10, map("a", mapValue("c1"), "b", mapValue("c2")))
        .build();

    RowSetUtilities.verify(expected, actual);

    // Now add columns in the second batch.
    rsLoader.startBatch();
    rootWriter.addRow(20, map("a2", mapValue("c11"), "b2", mapValue("c12"), "c2", mapValue("c13")));

    final DictWriter dictWriter = rootWriter.dict("d");
    final TupleWriter nestedMapWriter = dictWriter.valueWriter().tuple();
    nestedMapWriter.addColumn(SchemaBuilder.columnSchema("c", MinorType.VARCHAR, DataMode.REQUIRED));

    rootWriter.addRow(30, map("a3", mapValue("c21", "d21")));

    // And another set while the write proceeds.
    nestedMapWriter.addColumn(SchemaBuilder.columnSchema("d", MinorType.VARCHAR, DataMode.REQUIRED));

    rootWriter.addRow(40, map("a4", mapValue("c31", "d31", "e31"), "b4", mapValue("c32", "d32", "e32")));

    // Validate second batch
    actual = fixture.wrap(rsLoader.harvest());
    assertEquals(7, rsLoader.schemaVersion());

    final TupleMetadata expectedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addDict("d", MinorType.VARCHAR)
          .mapValue()
            .add("b", MinorType.VARCHAR)
            .add("c", MinorType.VARCHAR)
            .add("d", MinorType.VARCHAR)
            .resumeDict()
          .resumeSchema()
        .buildSchema();
    expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(20, map("a2", mapValue("c11", "", ""), "b2", mapValue("c12", "", ""), "c2", mapValue("c13", "", "")))
        .addRow(30, map("a3", mapValue("c21", "d21", "")))
        .addRow(40, map("a4", mapValue("c31", "d31", "e31"), "b4", mapValue("c32", "d32", "e32")))
        .build();

    RowSetUtilities.verify(expected, actual);
    rsLoader.close();
  }

  /**
   * Create dict with map value. Then, add columns to the map
   * on the fly. Use required, variable-width columns since
   * those require the most processing and are most likely to
   * fail if anything is out of place.
   */
  @Test
  public void testMapValueNullableFields() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addDict("d", MinorType.VARCHAR)
          .mapValue()
          .addNullable("b", MinorType.VARCHAR)
          .resumeDict()
          .resumeSchema()
        .buildSchema();
    final ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .readerSchema(schema)
        .build();
    final ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    assertEquals(5, rsLoader.schemaVersion());
    final RowSetLoader rootWriter = rsLoader.writer();

    rsLoader.startBatch();
    rootWriter.addRow(10, map("a", mapValue("c1"), "b", mapValue("c2")));

    // Validate first batch
    RowSet actual = fixture.wrap(rsLoader.harvest());
    assertEquals(5, rsLoader.schemaVersion());
    SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addRow(10, map("a", mapValue("c1"), "b", mapValue("c2")))
        .build();

    RowSetUtilities.verify(expected, actual);

    // Now add columns in the second batch.
    rsLoader.startBatch();
    rootWriter.addRow(20, map("a2", mapValue("c11"), "b2", mapValue("c12"), "c2", mapValue("c13")));

    final DictWriter dictWriter = rootWriter.dict("d");
    final TupleWriter nestedMapWriter = dictWriter.valueWriter().tuple();
    nestedMapWriter.addColumn(SchemaBuilder.columnSchema("c", MinorType.VARCHAR, DataMode.OPTIONAL));

    rootWriter.addRow(30, map("a3", mapValue("c21", "d21")));

    // And another set while the write proceeds.
    nestedMapWriter.addColumn(SchemaBuilder.columnSchema("d", MinorType.VARCHAR, DataMode.OPTIONAL));

    rootWriter.addRow(40, map("a4", mapValue("c31", "d31", "e31"), "b4", mapValue("c32", "d32", "e32")));

    // Validate second batch
    actual = fixture.wrap(rsLoader.harvest());
    assertEquals(7, rsLoader.schemaVersion());

    final TupleMetadata expectedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
          .addDict("d", MinorType.VARCHAR)
          .mapValue()
            .addNullable("b", MinorType.VARCHAR)
            .addNullable("c", MinorType.VARCHAR)
            .addNullable("d", MinorType.VARCHAR)
            .resumeDict()
          .resumeSchema()
        .buildSchema();
    expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(20, map("a2", mapValue("c11", null, null), "b2", mapValue("c12", null, null), "c2", mapValue("c13", null, null)))
        .addRow(30, map("a3", mapValue("c21", "d21", null)))
        .addRow(40, map("a4", mapValue("c31", "d31", "e31"), "b4", mapValue("c32", "d32", "e32")))
        .build();

    RowSetUtilities.verify(expected, actual);
    rsLoader.close();
  }

  @Test
  public void testArrayValue() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addDict("d", MinorType.INT)
          .repeatedValue(MinorType.INT)
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
        .addRow(10, map(
            1, intArray(110, 120, 130),
            2, intArray(111, 121)
        ))
        .addRow(20, map())
        .addRow(30, map(
            1, intArray(),
            2, intArray(310, 320),
            3, intArray(311, 321, 331, 341),
            4, intArray(312, 322, 332)
        ));

    // Validate first batch
    RowSet actual = fixture.wrap(rsLoader.harvest());
    SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addRow(10, map(
            1, intArray(110, 120, 130),
            2, intArray(111, 121)
        ))
        .addRow(20, map())
        .addRow(30, map(
            1, intArray(),
            2, intArray(310, 320),
            3, intArray(311, 321, 331, 341),
            4, intArray(312, 322, 332)
        ))
        .build();

    RowSetUtilities.verify(expected, actual);

    // Add another rows in the second batch.
    rsLoader.startBatch();
    rootWriter
        .addRow(40, map(1, intArray(410, 420)))
        .addRow(50, map(1, intArray(510), 2, intArray(511, 531)));

    // Validate first batch. The new array should have been back-filled with
    // empty offsets for the missing rows.
    actual = fixture.wrap(rsLoader.harvest());
    expected = fixture.rowSetBuilder(actual.schema())
        .addRow(40, map(1, intArray(410, 420)))
        .addRow(50, map(1, intArray(510), 2, intArray(511, 531)))
        .build();

    RowSetUtilities.verify(expected, actual);
    rsLoader.close();
  }

  @Test
  public void testDictValue() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addDict("d", MinorType.INT)
          .dictValue()
            .key(MinorType.INT)
            .nullableValue(MinorType.VARCHAR)
            .resumeDict()
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
        .addRow(10, map(
            1, map(1, "a", 2, "b", 4, "c"),
            2, map(2, "a2", 1, "c2")
        ))
        .addRow(20, map())
        .addRow(30, map(
            1, map(),
            2, map(1, "a3"),
            3, map(2, "b4", 4, "n4", 1, null),
            4, map(3, "m5", 1, "a5", 2, "c5", 8, "m5", 21, "h5")
        ));

    // Validate first batch
    RowSet actual = fixture.wrap(rsLoader.harvest());
    SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addRow(10, map(
            1, map(1, "a", 2, "b", 4, "c"),
            2, map(2, "a2", 1, "c2")
        ))
        .addRow(20, map())
        .addRow(30, map(
            1, map(),
            2, map(1, "a3"),
            3, map(2, "b4", 4, "n4", 1, null),
            4, map(3, "m5", 1, "a5", 2, "c5", 8, "m5", 21, "h5")
        ))
        .build();

    RowSetUtilities.verify(expected, actual);

    // Add another rows in the second batch.
    rsLoader.startBatch();
    rootWriter
        .addRow(40, map(
            1, map(1, "j6", 0, "k6"))
        )
        .addRow(50, map(
            1, map(2, "l7"),
            2, map(1, "o8", 5, "p8", 7, "u8")
        ));

    // Validate first batch. The new dict should have been back-filled with
    // empty offsets for the missing rows.
    actual = fixture.wrap(rsLoader.harvest());
    expected = fixture.rowSetBuilder(actual.schema())
        .addRow(40, map(
            1, map(1, "j6", 0, "k6"))
        )
        .addRow(50, map(
            1, map(2, "l7"),
            2, map(1, "o8", 5, "p8", 7, "u8")
        ))
        .build();

    RowSetUtilities.verify(expected, actual);
    rsLoader.close();
  }

  @Test
  public void testKeyOverflow() {
    TupleMetadata schema = new SchemaBuilder()
        .addDict("d", MinorType.VARCHAR)
          .value(MinorType.INT)
          .resumeSchema()
        .buildSchema();
    ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .rowCountLimit(ValueVector.MAX_ROW_COUNT)
        .readerSchema(schema)
        .build();
    ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    RowSetLoader rootWriter = rsLoader.writer();

    rsLoader.startBatch();
    byte[] key = new byte[523];
    Arrays.fill(key, (byte) 'X');

    int dictSize = 4; // number of entries in each dict

    // Number of rows should be driven by vector size.
    // Our row count should include the overflow row
    DictWriter dictWriter = rootWriter.dict(0);
    ScalarWriter keyWriter = dictWriter.keyWriter();
    ScalarWriter valueWriter = dictWriter.valueWriter().scalar();

    int expectedCount = ValueVector.MAX_BUFFER_SIZE / (key.length * dictSize);
    {
      int count = 0;
      while (! rootWriter.isFull()) {
        rootWriter.start();
        for (int i = 0; i < dictSize; i++) {
          keyWriter.setBytes(key, key.length);
          valueWriter.setInt(0); // acts as a placeholder, the actual value is not important
          dictWriter.save(); // not necessary for scalars, just for completeness
        }
        rootWriter.save();
        count++;
      }

      assertEquals(expectedCount + 1, count);

      // Loader's row count should include only "visible" rows
      assertEquals(expectedCount, rootWriter.rowCount());

      // Total count should include invisible and look-ahead rows.
      assertEquals(expectedCount + 1, rsLoader.totalRowCount());

      // Result should exclude the overflow row
      VectorContainer container = rsLoader.harvest();
      BatchValidator.validate(container);
      RowSet result = fixture.wrap(container);
      assertEquals(expectedCount, result.rowCount());
      result.clear();
    }

    // Next batch should start with the overflow row
    {
      rsLoader.startBatch();
      assertEquals(1, rootWriter.rowCount());
      assertEquals(expectedCount + 1, rsLoader.totalRowCount());
      VectorContainer container = rsLoader.harvest();
      BatchValidator.validate(container);
      RowSet result = fixture.wrap(container);
      assertEquals(1, result.rowCount());
      result.clear();
    }

    rsLoader.close();
  }

  @Test
  public void testValueOverflow() {
    TupleMetadata schema = new SchemaBuilder()
        .addDict("d", MinorType.INT)
          .value(MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();
    ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .rowCountLimit(ValueVector.MAX_ROW_COUNT)
        .readerSchema(schema)
        .build();
    ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    RowSetLoader rootWriter = rsLoader.writer();

    rsLoader.startBatch();
    byte[] value = new byte[523];
    Arrays.fill(value, (byte) 'X');

    int dictSize = 4; // number of entries in each dict

    // Number of rows should be driven by vector size.
    // Our row count should include the overflow row
    DictWriter dictWriter = rootWriter.dict(0);
    ScalarWriter keyWriter = dictWriter.keyWriter();
    ScalarWriter valueWriter = dictWriter.valueWriter().scalar();

    int expectedCount = ValueVector.MAX_BUFFER_SIZE / (value.length * dictSize);
    {
      int count = 0;
      while (! rootWriter.isFull()) {
        rootWriter.start();
        for (int i = 0; i < dictSize; i++) {
          keyWriter.setInt(0); // acts as a placeholder, the actual value is not important
          valueWriter.setBytes(value, value.length);
          dictWriter.save(); // not necessary for scalars, just for completeness
        }
        rootWriter.save();
        count++;
      }

      assertEquals(expectedCount + 1, count);

      // Loader's row count should include only "visible" rows
      assertEquals(expectedCount, rootWriter.rowCount());

      // Total count should include invisible and look-ahead rows.
      assertEquals(expectedCount + 1, rsLoader.totalRowCount());

      // Result should exclude the overflow row
      VectorContainer container = rsLoader.harvest();
      BatchValidator.validate(container);
      RowSet result = fixture.wrap(container);
      assertEquals(expectedCount, result.rowCount());
      result.clear();
    }

    // Next batch should start with the overflow row
    {
      rsLoader.startBatch();
      assertEquals(1, rootWriter.rowCount());
      assertEquals(expectedCount + 1, rsLoader.totalRowCount());
      VectorContainer container = rsLoader.harvest();
      BatchValidator.validate(container);
      RowSet result = fixture.wrap(container);
      assertEquals(1, result.rowCount());
      result.clear();
    }

    rsLoader.close();
  }
}
