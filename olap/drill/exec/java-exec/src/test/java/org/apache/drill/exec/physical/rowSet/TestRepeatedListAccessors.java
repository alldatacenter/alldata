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

import static org.apache.drill.test.rowSet.RowSetUtilities.objArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.singleObjArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.strArray;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.BatchSchemaBuilder;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.ColumnMetadata.StructureType;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.RepeatedVarCharVector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.accessor.ArrayReader;
import org.apache.drill.exec.vector.accessor.ArrayWriter;
import org.apache.drill.exec.vector.accessor.ObjectReader;
import org.apache.drill.exec.vector.accessor.ObjectType;
import org.apache.drill.exec.vector.accessor.ObjectWriter;
import org.apache.drill.exec.vector.accessor.ScalarReader;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.complex.BaseRepeatedValueVector;
import org.apache.drill.exec.vector.complex.RepeatedListVector;
import org.apache.drill.test.SubOperatorTest;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test the basics of repeated list support in the schema builder,
 * column writers and column readers. These tests work with a
 * single row set (batch). These tests should pass before moving
 * on to the result set loader tests.
 */

@Category(RowSetTests.class)
public class TestRepeatedListAccessors extends SubOperatorTest {

  /**
   * Test the intermediate case in which a repeated list
   * does not yet have child type.
   */

  @Test
  public void testSchemaIncompleteBatch() {
    SchemaBuilder schemaBuilder = new SchemaBuilder()
        .add("id", MinorType.INT)
          .addRepeatedList("list2")
          .resumeSchema();
    BatchSchema schema = new BatchSchemaBuilder()
        .withSchemaBuilder(schemaBuilder)
        .build();

    assertEquals(2, schema.getFieldCount());
    final MaterializedField list = schema.getColumn(1);
    assertEquals("list2", list.getName());
    assertEquals(MinorType.LIST, list.getType().getMinorType());
    assertEquals(DataMode.REPEATED, list.getType().getMode());
    assertTrue(list.getChildren().isEmpty());
  }

  @Test
  public void testSchemaIncompleteMetadata() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("id", MinorType.INT)
        .addRepeatedList("list2")
          .resumeSchema()
        .buildSchema();

    assertEquals(2, schema.size());
    final ColumnMetadata list = schema.metadata(1);
    assertEquals("list2", list.name());
    assertEquals(MinorType.LIST, list.type());
    assertEquals(DataMode.REPEATED, list.mode());
    assertNull(list.childSchema());
  }

  /**
   * Test the case of a simple 2D array. Drill represents
   * this as two levels of materialized fields.
   */

  @Test
  public void testSchema2DBatch() {
    SchemaBuilder schemaBuilder = new SchemaBuilder()
        .add("id", MinorType.INT)
        .addRepeatedList("list2")
          .addArray(MinorType.VARCHAR)
          .resumeSchema();
    BatchSchema schema = new BatchSchemaBuilder()
        .withSchemaBuilder(schemaBuilder)
        .build();

    assertEquals(2, schema.getFieldCount());
    final MaterializedField list = schema.getColumn(1);
    assertEquals("list2", list.getName());
    assertEquals(MinorType.LIST, list.getType().getMinorType());
    assertEquals(DataMode.REPEATED, list.getType().getMode());
    assertEquals(1, list.getChildren().size());

    final MaterializedField inner = list.getChildren().iterator().next();
    assertEquals("list2", inner.getName());
    assertEquals(MinorType.VARCHAR, inner.getType().getMinorType());
    assertEquals(DataMode.REPEATED, inner.getType().getMode());
  }

  /**
   * Test a 2D array using metadata. The metadata also uses
   * a column per dimension as that provides the easiest mapping
   * to the nested fields. A better design might be a single level
   * (as in repeated fields), but with a single attribute that
   * describes the number of dimensions. The <tt>dimensions()</tt>
   * method is a compromise.
   */

  @Test
  public void testSchema2DMetadata() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("id", MinorType.INT)
        .addRepeatedList("list2")
          .addArray(MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    assertEquals(2, schema.size());
    final ColumnMetadata list = schema.metadata(1);
    assertEquals("list2", list.name());
    assertEquals(MinorType.LIST, list.type());
    assertEquals(DataMode.REPEATED, list.mode());
    assertEquals(StructureType.MULTI_ARRAY, list.structureType());
    assertTrue(list.isArray());
    assertEquals(2, list.dimensions());
    assertNotNull(list.childSchema());

    final ColumnMetadata child = list.childSchema();
    assertEquals("list2", child.name());
    assertEquals(MinorType.VARCHAR, child.type());
    assertEquals(DataMode.REPEATED, child.mode());
    assertTrue(child.isArray());
    assertEquals(1, child.dimensions());
    assertNull(child.childSchema());
  }

  @Test
  public void testSchema3DBatch() {
    SchemaBuilder schemaBuilder = new SchemaBuilder()
        .add("id", MinorType.INT)
        .addRepeatedList("list2")
          .addDimension()
            .addArray(MinorType.VARCHAR)
            .resumeList()
            .resumeSchema();
    BatchSchema schema = new BatchSchemaBuilder()
        .withSchemaBuilder(schemaBuilder)
        .build();

    assertEquals(2, schema.getFieldCount());
    final MaterializedField list = schema.getColumn(1);
    assertEquals("list2", list.getName());
    assertEquals(MinorType.LIST, list.getType().getMinorType());
    assertEquals(DataMode.REPEATED, list.getType().getMode());
    assertEquals(1, list.getChildren().size());

    final MaterializedField child1 = list.getChildren().iterator().next();
    assertEquals("list2", child1.getName());
    assertEquals(MinorType.LIST, child1.getType().getMinorType());
    assertEquals(DataMode.REPEATED, child1.getType().getMode());
    assertEquals(1, child1.getChildren().size());

    final MaterializedField child2 = child1.getChildren().iterator().next();
    assertEquals("list2", child2.getName());
    assertEquals(MinorType.VARCHAR, child2.getType().getMinorType());
    assertEquals(DataMode.REPEATED, child2.getType().getMode());
    assertEquals(0, child2.getChildren().size());
  }

  @Test
  public void testSchema3DMetadata() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("id", MinorType.INT)
        .addRepeatedList("list2")
          .addDimension()
            .addArray(MinorType.VARCHAR)
            .resumeList()
          .resumeSchema()
        .buildSchema();

    assertEquals(2, schema.size());
    final ColumnMetadata list = schema.metadata(1);
    assertEquals("list2", list.name());
    assertEquals(MinorType.LIST, list.type());
    assertEquals(DataMode.REPEATED, list.mode());
    assertEquals(StructureType.MULTI_ARRAY, list.structureType());
    assertTrue(list.isArray());
    assertEquals(3, list.dimensions());
    assertNotNull(list.childSchema());

    final ColumnMetadata child1 = list.childSchema();
    assertEquals("list2", child1.name());
    assertEquals(MinorType.LIST, child1.type());
    assertEquals(DataMode.REPEATED, child1.mode());
    assertEquals(StructureType.MULTI_ARRAY, child1.structureType());
    assertTrue(child1.isArray());
    assertEquals(2, child1.dimensions());
    assertNotNull(child1.childSchema());

    final ColumnMetadata child2 = child1.childSchema();
    assertEquals("list2", child2.name());
    assertEquals(MinorType.VARCHAR, child2.type());
    assertEquals(DataMode.REPEATED, child2.mode());
    assertTrue(child2.isArray());
    assertEquals(1, child2.dimensions());
    assertNull(child2.childSchema());
  }

  @Test
  public void testIncompleteVectors() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("id", MinorType.INT)
        .addRepeatedList("list2")
          .resumeSchema()
        .buildSchema();

    final DirectRowSet rowSet = DirectRowSet.fromSchema(fixture.allocator(), schema);
    final VectorContainer container = rowSet.container();
    assertEquals(2, container.getNumberOfColumns());
    assertTrue(container.getValueVector(1).getValueVector() instanceof RepeatedListVector);
    final RepeatedListVector list = (RepeatedListVector) container.getValueVector(1).getValueVector();
    assertSame(BaseRepeatedValueVector.DEFAULT_DATA_VECTOR, list.getDataVector());
    assertTrue(list.getField().getChildren().isEmpty());
    rowSet.clear();
  }

  @Test
  public void testSchema2DVector() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("id", MinorType.INT)
        .addRepeatedList("list2")
          .addArray(MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    final DirectRowSet rowSet = DirectRowSet.fromSchema(fixture.allocator(), schema);
    final VectorContainer container = rowSet.container();
    assertEquals(2, container.getNumberOfColumns());
    assertTrue(container.getValueVector(1).getValueVector() instanceof RepeatedListVector);
    final RepeatedListVector list = (RepeatedListVector) container.getValueVector(1).getValueVector();
    assertEquals(1, list.getField().getChildren().size());

    final ValueVector child = list.getDataVector();
    assertTrue(child instanceof RepeatedVarCharVector);
    assertSame(list.getField().getChildren().iterator().next(), child.getField());
    rowSet.clear();
  }

  @Test
  public void testSchema3DVector() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("id", MinorType.INT)
        .addRepeatedList("list2")
          .addDimension()
            .addArray(MinorType.VARCHAR)
            .resumeList()
          .resumeSchema()
        .buildSchema();

    final DirectRowSet rowSet = DirectRowSet.fromSchema(fixture.allocator(), schema);
    final VectorContainer container = rowSet.container();
    assertEquals(2, container.getNumberOfColumns());
    assertTrue(container.getValueVector(1).getValueVector() instanceof RepeatedListVector);
    final RepeatedListVector list = (RepeatedListVector) container.getValueVector(1).getValueVector();
    assertEquals(1, list.getField().getChildren().size());

    assertTrue(list.getDataVector() instanceof RepeatedListVector);
    final RepeatedListVector child1 = (RepeatedListVector) list.getDataVector();
    assertEquals(1, child1.getField().getChildren().size());
    assertSame(list.getField().getChildren().iterator().next(), child1.getField());

    final ValueVector child2 = child1.getDataVector();
    assertTrue(child2 instanceof RepeatedVarCharVector);
    assertSame(child1.getField().getChildren().iterator().next(), child2.getField());
    rowSet.clear();
  }

  @Test
  public void testSchema2DWriterReader() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("id", MinorType.INT)
        .addRepeatedList("list2")
          .addArray(MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    final DirectRowSet rowSet = DirectRowSet.fromSchema(fixture.allocator(), schema);
    SingleRowSet result;
    {
      final RowSetWriter writer = rowSet.writer();
      assertEquals(2, writer.size());
      final ObjectWriter listObj = writer.column("list2");
      assertEquals(ObjectType.ARRAY, listObj.type());
      final ArrayWriter listWriter = listObj.array();
      assertEquals(ObjectType.ARRAY, listWriter.entryType());
      final ArrayWriter innerWriter = listWriter.array();
      assertEquals(ObjectType.SCALAR, innerWriter.entryType());
      final ScalarWriter strWriter = innerWriter.scalar();

      // Write one row using writers explicitly.
      //
      // (1, [["a, "b"], ["c", "d"]])
      //
      // Note auto increment of inner list on write.

      writer.scalar("id").setInt(1);
      strWriter.setString("a");
      strWriter.setString("b");
      listWriter.save();
      strWriter.setString("c");
      strWriter.setString("d");
      listWriter.save();
      writer.save();

      // Write more rows using the convenience methods.
      //
      // (2, [["e"], [], ["f", "g", "h"]])
      // (3, [])
      // (4, [[], ["i"], []])

      writer
        .addRow(2, objArray(strArray("e"), strArray(), strArray("f", "g", "h")))
        .addRow(3, objArray())
        .addRow(4, objArray(strArray(), strArray("i"), strArray()));

      result = writer.done();
    }

    // Verify one row using the individual readers.

    {
      final RowSetReader reader = result.reader();

      assertEquals(2, reader.columnCount());
      final ObjectReader listObj = reader.column("list2");
      assertEquals(ObjectType.ARRAY, listObj.type());
      final ArrayReader listReader = listObj.array();
      assertEquals(ObjectType.ARRAY, listReader.entryType());
      final ArrayReader innerReader = listReader.array();
      assertEquals(ObjectType.SCALAR, innerReader.entryType());
      final ScalarReader strReader = innerReader.scalar();

      // Write one row using writers explicitly.
      //
      // (1, [["a, "b"], ["c", "d"]])

      assertTrue(reader.next());
      assertEquals(2, listReader.size());
        assertTrue(listReader.next());
          assertEquals(2, innerReader.size());
          assertTrue(innerReader.next());
          assertEquals("a", strReader.getString());
          assertTrue(innerReader.next());
          assertEquals("b", strReader.getString());
          assertFalse(innerReader.next());
        assertTrue(listReader.next());
          assertEquals(2, innerReader.size());
          assertTrue(innerReader.next());
          assertEquals("c", strReader.getString());
          assertTrue(innerReader.next());
          assertEquals("d", strReader.getString());
          assertFalse(innerReader.next());
        assertFalse(listReader.next());
    }

    // Verify both rows by building another row set and comparing.

    final RowSet expected = fixture.rowSetBuilder(schema)
        .addRow(1, objArray(strArray("a", "b"), strArray("c", "d")))
        .addRow(2, objArray(strArray("e"), strArray(), strArray("f", "g", "h")))
        .addRow(3, objArray())
        .addRow(4, objArray(strArray(), strArray("i"), strArray()))
        .build();

    new RowSetComparison(expected).verify(result);

    // Test that the row set rebuilds its internal structure from
    // a vector container.

    RowSet wrapped = fixture.wrap(result.container());
    RowSetUtilities.verify(expected, wrapped);
  }

  @Test
  public void testSchema3DWriterReader() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("id", MinorType.INT)

        // Uses a short-hand method to avoid mucking with actual
        // nested lists.

        .addArray("cube", MinorType.VARCHAR, 3)
        .buildSchema();

    final SingleRowSet actual = fixture.rowSetBuilder(schema)
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
              strArray("i"))))
      .build();

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

    RowSetUtilities.verify(expected, actual);
  }
}
