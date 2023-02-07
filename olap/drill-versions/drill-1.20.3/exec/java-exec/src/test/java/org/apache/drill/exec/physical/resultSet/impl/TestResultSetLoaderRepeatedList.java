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

import java.util.Arrays;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.exec.physical.rowSet.RowSetReader;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.ColumnMetadata.StructureType;
import org.apache.drill.exec.record.metadata.RepeatedListColumnMetadata;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.accessor.ArrayReader;
import org.apache.drill.exec.vector.accessor.ArrayWriter;
import org.apache.drill.exec.vector.accessor.ObjectType;
import org.apache.drill.exec.vector.accessor.ObjectWriter;
import org.apache.drill.exec.vector.accessor.ScalarReader;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.accessor.ValueType;
import org.apache.drill.exec.vector.accessor.writer.RepeatedListWriter;
import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.test.SubOperatorTest;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.apache.drill.test.rowSet.RowSetUtilities.objArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.singleObjArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.strArray;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests repeated list support. Repeated lists add another layer of dimensionality
 * on top of other repeated types. Since a repeated list can wrap another repeated
 * list, the repeated list allows creating 2D, 3D or higher dimensional arrays (lists,
 * actually, since the different "slices" need not have the same length...)
 * Repeated lists appear to be used only by JSON.
 */
@Category(RowSetTests.class)
public class TestResultSetLoaderRepeatedList extends SubOperatorTest {

  @Test
  public void test2DEarlySchema() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("id", MinorType.INT)
        .addRepeatedList("list2")
          .addArray(MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    final ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .readerSchema(schema)
        .build();
    final ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);

    do2DTest(schema, rsLoader);
    rsLoader.close();
  }

  private void do2DTest(TupleMetadata schema, ResultSetLoader rsLoader) {
    final RowSetLoader writer = rsLoader.writer();

    // Sanity check of writer structure
    assertEquals(2, writer.size());
    final ObjectWriter listObj = writer.column("list2");
    assertEquals(ObjectType.ARRAY, listObj.type());
    final ArrayWriter listWriter = listObj.array();
    assertEquals(ObjectType.ARRAY, listWriter.entryType());
    final ArrayWriter innerWriter = listWriter.array();
    assertEquals(ObjectType.SCALAR, innerWriter.entryType());
    final ScalarWriter strWriter = innerWriter.scalar();
    assertEquals(ValueType.STRING, strWriter.valueType());

    // Sanity test of schema
    final TupleMetadata rowSchema = writer.tupleSchema();
    assertEquals(2, rowSchema.size());
    final ColumnMetadata listSchema = rowSchema.metadata(1);
    assertEquals(MinorType.LIST, listSchema.type());
    assertEquals(DataMode.REPEATED, listSchema.mode());
    assertTrue(listSchema instanceof RepeatedListColumnMetadata);
    assertEquals(StructureType.MULTI_ARRAY, listSchema.structureType());
    assertNotNull(listSchema.childSchema());

    final ColumnMetadata elementSchema = listSchema.childSchema();
    assertEquals(listSchema.name(), elementSchema.name());
    assertEquals(MinorType.VARCHAR, elementSchema.type());
    assertEquals(DataMode.REPEATED, elementSchema.mode());

    // Write values
    rsLoader.startBatch();
    writer
        .addRow(1, objArray(strArray("a", "b"), strArray("c", "d")))
        .addRow(2, objArray(strArray("e"), strArray(), strArray("f", "g", "h")))
        .addRow(3, objArray())
        .addRow(4, objArray(strArray(), strArray("i"), strArray()));

    // Verify the values.
    // (Relies on the row set level repeated list tests having passed.)
    final RowSet expected = fixture.rowSetBuilder(schema)
        .addRow(1, objArray(strArray("a", "b"), strArray("c", "d")))
        .addRow(2, objArray(strArray("e"), strArray(), strArray("f", "g", "h")))
        .addRow(3, objArray())
        .addRow(4, objArray(strArray(), strArray("i"), strArray()))
        .build();

    RowSetUtilities.verify(expected, fixture.wrap(rsLoader.harvest()));
  }

  @Test
  public void test2DLateSchema() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("id", MinorType.INT)
        .addRepeatedList("list2")
          .addArray(MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    final ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .build();
    final ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    final RowSetLoader writer = rsLoader.writer();

    // Add columns dynamically
    writer.addColumn(schema.metadata(0));
    writer.addColumn(schema.metadata(1).cloneEmpty());

    // Yes, this is ugly. The whole repeated array idea is awkward.
    // The only place it is used at present is in JSON where the
    // awkwardness is mixed in with JSON complexity.
    // Consider improving this API in the future.
    ((RepeatedListWriter) writer.array(1)).defineElement(schema.metadata(1).childSchema());

    do2DTest(schema, rsLoader);
    rsLoader.close();
  }

  @Test
  public void test2DLateSchemaIncremental() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("id", MinorType.INT)
        .addRepeatedList("list1")
          .addArray(MinorType.VARCHAR)
          .resumeSchema()
        .addRepeatedList("list2")
          .addArray(MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    final ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .build();
    final ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    final RowSetLoader writer = rsLoader.writer();

    // Add columns dynamically
    writer.addColumn(schema.metadata(0));

    // Write a row without the array.
    rsLoader.startBatch();
    writer.addRow(1);

    // Add the repeated list, but without contents.
    writer.addColumn(schema.metadata(1).cloneEmpty());

    // Sanity check of writer structure
    assertEquals(2, writer.size());
    final ObjectWriter listObj = writer.column("list1");
    assertEquals(ObjectType.ARRAY, listObj.type());
    final ArrayWriter listWriter = listObj.array();

    // No child defined yet. A dummy child is inserted instead.
    assertEquals(MinorType.NULL, listWriter.entry().schema().type());
    assertEquals(ObjectType.ARRAY, listWriter.entryType());
    assertEquals(ObjectType.SCALAR, listWriter.array().entryType());
    assertEquals(ValueType.NULL, listWriter.array().scalar().valueType());

    // Although we don't know the type of the inner, we can still
    // create null (empty) elements in the outer array.
    writer
      .addRow(2, null)
      .addRow(3, objArray())
      .addRow(4, objArray(objArray(), null));

    // Define the inner type.
    final RepeatedListWriter listWriterImpl = (RepeatedListWriter) listWriter;
    listWriterImpl.defineElement(MaterializedField.create("list1", Types.repeated(MinorType.VARCHAR)));

    // Sanity check of completed structure
    assertEquals(ObjectType.ARRAY, listWriter.entryType());
    final ArrayWriter innerWriter = listWriter.array();
    assertEquals(ObjectType.SCALAR, innerWriter.entryType());
    final ScalarWriter strWriter = innerWriter.scalar();
    assertEquals(ValueType.STRING, strWriter.valueType());

    // Write values
    writer
        .addRow(5, objArray(strArray("a1", "b1"), strArray("c1", "d1")));

    // Add the second list, with a complete type
    writer.addColumn(schema.metadata(2));

    // Sanity check of writer structure
    assertEquals(3, writer.size());
    final ObjectWriter list2Obj = writer.column("list2");
    assertEquals(ObjectType.ARRAY, list2Obj.type());
    final ArrayWriter list2Writer = list2Obj.array();
    assertEquals(ObjectType.ARRAY, list2Writer.entryType());
    final ArrayWriter inner2Writer = list2Writer.array();
    assertEquals(ObjectType.SCALAR, inner2Writer.entryType());
    final ScalarWriter str2Writer = inner2Writer.scalar();
    assertEquals(ValueType.STRING, str2Writer.valueType());

    // Write values
    writer
        .addRow(6,
            objArray(strArray("a2", "b2"), strArray("c2", "d2")),
            objArray(strArray("w2", "x2"), strArray("y2", "z2")));

    // Add the second list, with a complete type

    // Verify the values.
    // (Relies on the row set level repeated list tests having passed.)
    final RowSet expected = fixture.rowSetBuilder(schema)
        .addRow(1, objArray(), objArray())
        .addRow(2, objArray(), objArray())
        .addRow(3, objArray(), objArray())
        .addRow(4, objArray(objArray(), null), objArray())
        .addRow(5, objArray(strArray("a1", "b1"), strArray("c1", "d1")), objArray())
        .addRow(6,
            objArray(strArray("a2", "b2"), strArray("c2", "d2")),
            objArray(strArray("w2", "x2"), strArray("y2", "z2")))
        .build();

    RowSetUtilities.verify(expected, fixture.wrap(rsLoader.harvest()));
    rsLoader.close();
  }

  @Test
  public void test2DOverflow() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("id", MinorType.INT)
        .addRepeatedList("list2")
          .addArray(MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    final ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .rowCountLimit(ValueVector.MAX_ROW_COUNT)
        .readerSchema(schema)
        .build();
    final ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    final RowSetLoader writer = rsLoader.writer();

    // Fill the batch with enough data to cause overflow.
    // Data must be large enough to cause overflow before 64K rows
    // Make a bit bigger to overflow early.
    final int outerSize = 7;
    final int innerSize = 5;
    final int strLength = ValueVector.MAX_BUFFER_SIZE / ValueVector.MAX_ROW_COUNT / outerSize / innerSize + 20;
    final byte value[] = new byte[strLength - 6];
    Arrays.fill(value, (byte) 'X');
    final String strValue = new String(value, Charsets.UTF_8);
    int rowCount = 0;
    int elementCount = 0;

    final ArrayWriter outerWriter = writer.array(1);
    final ArrayWriter innerWriter = outerWriter.array();
    final ScalarWriter elementWriter = innerWriter.scalar();
    rsLoader.startBatch();
    while (! writer.isFull()) {
      writer.start();
      writer.scalar(0).setInt(rowCount);
      for (int j = 0; j < outerSize; j++) {
        for (int k = 0; k < innerSize; k++) {
          elementWriter.setString(String.format("%s%06d", strValue, elementCount));
          elementCount++;
        }
        outerWriter.save();
      }
      writer.save();
      rowCount++;
    }

    // Number of rows should be driven by vector size.
    // Our row count should include the overflow row
    final int expectedCount = ValueVector.MAX_BUFFER_SIZE / (strLength * innerSize * outerSize);
    assertEquals(expectedCount + 1, rowCount);

    // Loader's row count should include only "visible" rows
    assertEquals(expectedCount, writer.rowCount());

    // Total count should include invisible and look-ahead rows.
    assertEquals(expectedCount + 1, rsLoader.totalRowCount());

    // Result should exclude the overflow row
    RowSet result = fixture.wrap(rsLoader.harvest());
    assertEquals(expectedCount, result.rowCount());

    // Verify the data.
    RowSetReader reader = result.reader();
    ArrayReader outerReader = reader.array(1);
    ArrayReader innerReader = outerReader.array();
    ScalarReader strReader = innerReader.scalar();
    int readRowCount = 0;
    int readElementCount = 0;
    while (reader.next()) {
      assertEquals(readRowCount, reader.scalar(0).getInt());
      for (int i = 0; i < outerSize; i++) {
        assertTrue(outerReader.next());
        for (int j = 0; j < innerSize; j++) {
          assertTrue(innerReader.next());
          assertEquals(String.format("%s%06d", strValue, readElementCount),
              strReader.getString());
          readElementCount++;
        }
        assertFalse(innerReader.next());
      }
      assertFalse(outerReader.next());
      readRowCount++;
    }
    assertEquals(readRowCount, result.rowCount());
    result.clear();

    // Write a few more rows to verify the overflow row.
    rsLoader.startBatch();
    for (int i = 0; i < 1000; i++) {
      writer.start();
      writer.scalar(0).setInt(rowCount);
      for (int j = 0; j < outerSize; j++) {
        for (int k = 0; k < innerSize; k++) {
          elementWriter.setString(String.format("%s%06d", strValue, elementCount));
          elementCount++;
        }
        outerWriter.save();
      }
      writer.save();
      rowCount++;
    }

    result = fixture.wrap(rsLoader.harvest());
    assertEquals(1001, result.rowCount());

    final int startCount = readRowCount;
    reader = result.reader();
    outerReader = reader.array(1);
    innerReader = outerReader.array();
    strReader = innerReader.scalar();
    while (reader.next()) {
      assertEquals(readRowCount, reader.scalar(0).getInt());
      for (int i = 0; i < outerSize; i++) {
        assertTrue(outerReader.next());
        for (int j = 0; j < innerSize; j++) {
          assertTrue(innerReader.next());
          elementWriter.setString(String.format("%s%06d", strValue, readElementCount));
          assertEquals(String.format("%s%06d", strValue, readElementCount),
              strReader.getString());
          readElementCount++;
        }
        assertFalse(innerReader.next());
      }
      assertFalse(outerReader.next());
      readRowCount++;
    }
    assertEquals(readRowCount - startCount, result.rowCount());
    result.clear();
    rsLoader.close();
  }

  // Adapted from TestRepeatedListAccessors.testSchema3DWriterReader
  // That test exercises the low-level schema and writer mechanisms.
  // Here we simply ensure that the 3D case continues to work when
  // wrapped in the Result Set Loader
  @Test
  public void test3DEarlySchema() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("id", MinorType.INT)

        // Uses a short-hand method to avoid mucking with actual
        // nested lists.
        .addArray("cube", MinorType.VARCHAR, 3)
        .buildSchema();

    final ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .readerSchema(schema)
        .build();
    final ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);

    rsLoader.startBatch();
    final RowSetLoader writer = rsLoader.writer();
    writer
      .addRow(1,
          objArray(
              objArray(
                  strArray("a", "b"),
                  strArray("c")),
              objArray(
                  strArray("d", "e", "f"),
                  null),
              null,
              objArray()))
      .addRow(2, null)
      .addRow(3, objArray())
      .addRow(4, objArray(objArray()))
      .addRow(5, singleObjArray(
          objArray(
              strArray("g", "h"),
              strArray("i"))));

    final SingleRowSet expected = fixture.rowSetBuilder(schema)
      .addRow(1,
          objArray(
              objArray(
                  strArray("a", "b"),
                  strArray("c")),
              objArray(
                  strArray("d", "e", "f"),
                  strArray()),
              objArray(),
              objArray()))
      .addRow(2, objArray())
      .addRow(3, objArray())
      .addRow(4, objArray(objArray()))
      .addRow(5, singleObjArray(
          objArray(
              strArray("g", "h"),
              strArray("i"))))
      .build();

    RowSetUtilities.verify(expected, fixture.wrap(rsLoader.harvest()));
  }

  // TODO: Test union list as inner
  // TODO: Test repeated map as inner
}
