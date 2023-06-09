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
package org.apache.drill.exec.record.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.metadata.ColumnMetadata.StructureType;
import org.apache.drill.test.BaseTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test the tuple and column metadata, including extended attributes.
 */
@Category(RowSetTests.class)
public class TestTupleSchema extends BaseTest {

  /**
   * Test a fixed-width, primitive, required column. Includes basic
   * tests common to all data types. (Basic tests are not repeated for
   * other types.)
   */
  @Test
  public void testRequiredFixedWidthColumn() {

    MaterializedField field = SchemaBuilder.columnSchema("c", MinorType.INT, DataMode.REQUIRED );
    ColumnMetadata col = MetadataUtils.fromField(field);

    // Code may depend on the specific column class
    assertTrue(col instanceof PrimitiveColumnMetadata);

    // Generic checks
    assertEquals(ColumnMetadata.StructureType.PRIMITIVE, col.structureType());
    assertNull(col.tupleSchema());
    assertTrue(field.isEquivalent(col.schema()));
    assertEquals(field.getName(), col.name());
    assertEquals(field.getType().getMinorType(), col.type());
    assertEquals(field.getDataMode(), col.mode());
    assertFalse(col.isNullable());
    assertFalse(col.isArray());
    assertFalse(col.isVariableWidth());
    assertFalse(col.isMap());
    assertTrue(col.isEquivalent(col));
    assertFalse(col.isVariant());

    ColumnMetadata col2 = MetadataUtils.fromField(field);
    assertTrue(col.isEquivalent(col2));

    MaterializedField field3 = SchemaBuilder.columnSchema("d", MinorType.INT, DataMode.REQUIRED );
    ColumnMetadata col3 = MetadataUtils.fromField(field3);
    assertFalse(col.isEquivalent(col3));

    MaterializedField field4 = SchemaBuilder.columnSchema("c", MinorType.BIGINT, DataMode.REQUIRED );
    ColumnMetadata col4 = MetadataUtils.fromField(field4);
    assertFalse(col.isEquivalent(col4));

    MaterializedField field5 = SchemaBuilder.columnSchema("c", MinorType.INT, DataMode.OPTIONAL );
    ColumnMetadata col5 = MetadataUtils.fromField(field5);
    assertFalse(col.isEquivalent(col5));

    ColumnMetadata col6 = col.cloneEmpty();
    assertTrue(col.isEquivalent(col6));

    assertEquals(4, col.expectedWidth());
    col.setExpectedWidth(10);
    assertEquals(4, col.expectedWidth());

    assertEquals(1, col.expectedElementCount());
    col.setExpectedElementCount(2);
    assertEquals(1, col.expectedElementCount());
  }

  @Test
  public void testNullableFixedWidthColumn() {

    MaterializedField field = SchemaBuilder.columnSchema("c", MinorType.INT, DataMode.OPTIONAL );
    ColumnMetadata col = MetadataUtils.fromField(field);

    assertEquals(ColumnMetadata.StructureType.PRIMITIVE, col.structureType());
    assertTrue(col.isNullable());
    assertFalse(col.isArray());
    assertFalse(col.isVariableWidth());
    assertFalse(col.isMap());
    assertFalse(col.isVariant());

    assertEquals(4, col.expectedWidth());
    col.setExpectedWidth(10);
    assertEquals(4, col.expectedWidth());

    assertEquals(1, col.expectedElementCount());
    col.setExpectedElementCount(2);
    assertEquals(1, col.expectedElementCount());
  }

  @Test
  public void testRepeatedFixedWidthColumn() {

    MaterializedField field = SchemaBuilder.columnSchema("c", MinorType.INT, DataMode.REPEATED );
    ColumnMetadata col = MetadataUtils.fromField(field);

    assertFalse(col.isNullable());
    assertTrue(col.isArray());
    assertFalse(col.isVariableWidth());
    assertFalse(col.isMap());
    assertFalse(col.isVariant());

    assertEquals(4, col.expectedWidth());
    col.setExpectedWidth(10);
    assertEquals(4, col.expectedWidth());

    assertEquals(ColumnMetadata.DEFAULT_ARRAY_SIZE, col.expectedElementCount());

    col.setExpectedElementCount(2);
    assertEquals(2, col.expectedElementCount());

    col.setExpectedElementCount(0);
    assertEquals(1, col.expectedElementCount());
  }

  @Test
  public void testRequiredVariableWidthColumn() {

    MaterializedField field = SchemaBuilder.columnSchema("c", MinorType.VARCHAR, DataMode.REQUIRED );
    ColumnMetadata col = MetadataUtils.fromField(field);

    assertEquals(ColumnMetadata.StructureType.PRIMITIVE, col.structureType());
    assertNull(col.tupleSchema());
    assertFalse(col.isNullable());
    assertFalse(col.isArray());
    assertTrue(col.isVariableWidth());
    assertFalse(col.isMap());
    assertFalse(col.isVariant());

    // A different precision is a different type.
    MaterializedField field2 = new ColumnBuilder("c", MinorType.VARCHAR)
        .setMode(DataMode.REQUIRED)
        .setPrecision(10)
        .build();

    ColumnMetadata col2 = MetadataUtils.fromField(field2);
    assertFalse(col.isEquivalent(col2));

    assertEquals(50, col.expectedWidth());
    col.setExpectedWidth(10);
    assertEquals(10, col.expectedWidth());

    assertEquals(1, col.expectedElementCount());
    col.setExpectedElementCount(2);
    assertEquals(1, col.expectedElementCount());

    // If precision is provided, then that is the default width
    col = MetadataUtils.fromField(field2);
    assertEquals(10, col.expectedWidth());
  }

  @Test
  public void testNullableVariableWidthColumn() {

    MaterializedField field = SchemaBuilder.columnSchema("c", MinorType.VARCHAR, DataMode.OPTIONAL );
    ColumnMetadata col = MetadataUtils.fromField(field);

    assertTrue(col.isNullable());
    assertFalse(col.isArray());
    assertTrue(col.isVariableWidth());
    assertFalse(col.isMap());
    assertFalse(col.isVariant());

    assertEquals(50, col.expectedWidth());
    col.setExpectedWidth(10);
    assertEquals(10, col.expectedWidth());

    assertEquals(1, col.expectedElementCount());
    col.setExpectedElementCount(2);
    assertEquals(1, col.expectedElementCount());
  }

  @Test
  public void testRepeatedVariableWidthColumn() {

    MaterializedField field = SchemaBuilder.columnSchema("c", MinorType.VARCHAR, DataMode.REPEATED );
    ColumnMetadata col = MetadataUtils.fromField(field);

    assertFalse(col.isNullable());
    assertTrue(col.isArray());
    assertTrue(col.isVariableWidth());
    assertFalse(col.isMap());
    assertFalse(col.isVariant());

    assertEquals(50, col.expectedWidth());
    col.setExpectedWidth(10);
    assertEquals(10, col.expectedWidth());

    assertEquals(ColumnMetadata.DEFAULT_ARRAY_SIZE, col.expectedElementCount());

    col.setExpectedElementCount(2);
    assertEquals(2, col.expectedElementCount());
  }

  @Test
  public void testDecimalScalePrecision() {

    MaterializedField field = MaterializedField.create("d",
        MajorType.newBuilder()
          .setMinorType(MinorType.DECIMAL9)
          .setMode(DataMode.REQUIRED)
          .setPrecision(3)
          .setScale(4)
          .build());

    ColumnMetadata col = MetadataUtils.fromField(field);

    assertFalse(col.isNullable());
    assertFalse(col.isArray());
    assertFalse(col.isVariableWidth());
    assertFalse(col.isMap());
    assertFalse(col.isVariant());

    assertEquals(3, col.precision());
    assertEquals(4, col.scale());

    assertTrue(field.isEquivalent(col.schema()));
  }

  /**
   * Tests a map column. Maps can only be required or repeated, not nullable.
   * (But, the columns in the map can be nullable.)
   */
  @Test
  public void testMapColumn() {

    MaterializedField field = SchemaBuilder.columnSchema("m", MinorType.MAP, DataMode.REQUIRED );
    ColumnMetadata col = MetadataUtils.fromField(field);

    assertTrue(col instanceof MapColumnMetadata);
    assertNotNull(col.tupleSchema());
    assertEquals(0, col.tupleSchema().size());
    assertSame(col, col.tupleSchema().parent());

    MapColumnMetadata mapCol = (MapColumnMetadata) col;
    assertNull(mapCol.parentTuple());

    assertEquals(ColumnMetadata.StructureType.TUPLE, col.structureType());
    assertFalse(col.isNullable());
    assertFalse(col.isArray());
    assertFalse(col.isVariableWidth());
    assertTrue(col.isMap());
    assertFalse(col.isVariant());

    assertEquals(0, col.expectedWidth());
    col.setExpectedWidth(10);
    assertEquals(0, col.expectedWidth());

    assertEquals(1, col.expectedElementCount());
    col.setExpectedElementCount(2);
    assertEquals(1, col.expectedElementCount());
  }

  @Test
  public void testRepeatedMapColumn() {

    MaterializedField field = SchemaBuilder.columnSchema("m", MinorType.MAP, DataMode.REPEATED );
    ColumnMetadata col = MetadataUtils.fromField(field);

    assertTrue(col instanceof MapColumnMetadata);
    assertNotNull(col.tupleSchema());
    assertEquals(0, col.tupleSchema().size());

    assertFalse(col.isNullable());
    assertTrue(col.isArray());
    assertFalse(col.isVariableWidth());
    assertTrue(col.isMap());
    assertFalse(col.isVariant());

    assertEquals(0, col.expectedWidth());
    col.setExpectedWidth(10);
    assertEquals(0, col.expectedWidth());

    assertEquals(ColumnMetadata.DEFAULT_ARRAY_SIZE, col.expectedElementCount());

    col.setExpectedElementCount(2);
    assertEquals(2, col.expectedElementCount());
  }

  @Test
  public void testUnionColumn() {

    MaterializedField field = SchemaBuilder.columnSchema("u", MinorType.UNION, DataMode.OPTIONAL);
    ColumnMetadata col = MetadataUtils.fromField(field);
    assertFalse(col.isArray());
    assertTrue(col.isVariableWidth());
    doVariantTest(col);
  }

  @Test
  public void testListColumn() {

    MaterializedField field = SchemaBuilder.columnSchema("l", MinorType.LIST, DataMode.OPTIONAL);
    ColumnMetadata col = MetadataUtils.fromField(field);
    assertTrue(col.isArray());

    // List modeled as a repeated element. Implementation is a bit
    // more complex, but does not affect this abstract description.
    assertFalse(col.isVariableWidth());
    doVariantTest(col);
  }

  private void doVariantTest(ColumnMetadata col) {

    assertTrue(col instanceof VariantColumnMetadata);

    assertTrue(col.isNullable());
    assertFalse(col.isMap());
    assertTrue(col.isVariant());

    assertEquals(0, col.expectedWidth());
    col.setExpectedWidth(10);
    assertEquals(0, col.expectedWidth());

    VariantMetadata variant = col.variantSchema();
    assertNotNull(variant);
    assertEquals(0, variant.size());

    ColumnMetadata member = variant.addType(MinorType.INT);
    assertEquals(MinorType.INT, member.type());
    assertEquals(DataMode.OPTIONAL, member.mode());
    assertEquals(Types.typeKey(MinorType.INT), member.name());

    assertEquals(1, variant.size());
    assertTrue(variant.hasType(MinorType.INT));
    assertSame(member, variant.member(MinorType.INT));

    try {
      variant.addType(MinorType.INT);
      fail();
    } catch (IllegalArgumentException e) {
      // expected
    }

    assertFalse(variant.hasType(MinorType.VARCHAR));
    member = variant.addType(MinorType.VARCHAR);
    assertEquals(MinorType.VARCHAR, member.type());
    assertEquals(DataMode.OPTIONAL, member.mode());
    assertEquals(Types.typeKey(MinorType.VARCHAR), member.name());

    assertEquals(2, variant.size());
    assertTrue(variant.hasType(MinorType.VARCHAR));
    assertSame(member, variant.member(MinorType.VARCHAR));

    assertFalse(variant.hasType(MinorType.BIGINT));
  }

  // Repeated list

  /**
   * Test the basics of an empty root tuple (i.e. row) schema.
   */
  @Test
  public void testEmptyRootTuple() {

    TupleMetadata root = new TupleSchema();

    assertEquals(0, root.size());
    assertTrue(root.isEmpty());
    assertEquals(-1, root.index("foo"));

    try {
      root.metadata(0);
      fail();
    } catch (IndexOutOfBoundsException e) {
      // Expected
    }
    assertNull(root.metadata("foo"));

    try {
      root.column(0);
      fail();
    } catch (IndexOutOfBoundsException e) {
      // Expected
    }
    assertNull(root.column("foo"));

    try {
      root.fullName(0);
      fail();
    } catch (IndexOutOfBoundsException e) {
      // Expected
    }

    // The full name method does not check if the column is actually
    // in the tuple.
    MaterializedField field = SchemaBuilder.columnSchema("c", MinorType.INT, DataMode.REQUIRED );
    ColumnMetadata col = MetadataUtils.fromField(field);
    assertEquals("c", root.fullName(col));

    assertTrue(root.isEquivalent(root));
    assertNull(root.parent());
    assertTrue(root.toFieldList().isEmpty());
  }

  /**
   * Test the basics of a non-empty root tuple (i.e. a row) using a pair
   * of primitive columns.
   */
  @Test
  public void testNonEmptyRootTuple() {

    TupleSchema root = new TupleSchema();

    MaterializedField fieldA = SchemaBuilder.columnSchema("a", MinorType.INT, DataMode.REQUIRED );
    ColumnMetadata colA = root.add(fieldA);

    assertEquals(1, root.size());
    assertFalse(root.isEmpty());
    assertEquals(0, root.index("a"));
    assertEquals(-1, root.index("b"));

    assertTrue(fieldA.isEquivalent(root.column(0)));
    assertTrue(fieldA.isEquivalent(root.column("a")));
    assertTrue(fieldA.isEquivalent(root.column("A")));

    assertSame(colA, root.metadata(0));
    assertSame(colA, root.metadata("a"));

    assertEquals("a", root.fullName(0));
    assertEquals("a", root.fullName(colA));

    try {
      root.add(fieldA);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }

    MaterializedField fieldB = SchemaBuilder.columnSchema("b", MinorType.VARCHAR, DataMode.OPTIONAL );
    ColumnMetadata colB = MetadataUtils.fromField(fieldB);
    int indexB = root.addColumn(colB);

    assertEquals(1, indexB);
    assertEquals(2, root.size());
    assertFalse(root.isEmpty());
    assertEquals(indexB, root.index("b"));

    assertTrue(fieldB.isEquivalent(root.column(1)));
    assertTrue(fieldB.isEquivalent(root.column("b")));

    assertSame(colB, root.metadata(1));
    assertSame(colB, root.metadata("b"));

    assertEquals("b", root.fullName(1));
    assertEquals("b", root.fullName(colB));

    try {
      root.add(fieldB);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }

    List<MaterializedField> fieldList = root.toFieldList();
    assertTrue(fieldA.isEquivalent(fieldList.get(0)));
    assertTrue(fieldB.isEquivalent(fieldList.get(1)));

    TupleMetadata emptyRoot = new TupleSchema();
    assertFalse(emptyRoot.isEquivalent(root));

    // Same schema: the tuples are equivalent
    TupleMetadata root3 = new TupleSchema();
    root3.add(fieldA);
    root3.addColumn(colB);
    assertTrue(root3.isEquivalent(root));
    assertTrue(root.isEquivalent(root3));

    // Same columns, different order. The tuples are not equivalent.
    TupleMetadata root4 = new TupleSchema();
    root4.addColumn(colB);
    root4.add(fieldA);
    assertFalse(root4.isEquivalent(root));
    assertFalse(root.isEquivalent(root4));

    // A tuple is equivalent to its copy.
    assertTrue(root.isEquivalent(root.copy()));
  }

  /**
   * Test a complex map schema of the form:<br>
   * a.`b.x`.`c.y`.d<br>
   * in which columns "a", "b.x" and "c.y" are maps, "b.x" and "c.y" are names
   * that contains dots, and d is primitive.
   * Here we build up the schema using the metadata schema, and generate a
   * materialized field from the metadata.
   */
  @Test
  public void testMapTupleFromMetadata() {

    TupleSchema root = new TupleSchema();

    MaterializedField fieldA = SchemaBuilder.columnSchema("a", MinorType.MAP, DataMode.REQUIRED);
    ColumnMetadata colA = root.add(fieldA);
    TupleMetadata mapA = colA.tupleSchema();

    MaterializedField fieldB = SchemaBuilder.columnSchema("b.x", MinorType.MAP, DataMode.REQUIRED);
    ColumnMetadata colB = mapA.add(fieldB);
    TupleMetadata mapB = colB.tupleSchema();

    MaterializedField fieldC = SchemaBuilder.columnSchema("c.y", MinorType.MAP, DataMode.REQUIRED);
    ColumnMetadata colC = mapB.add(fieldC);
    TupleMetadata mapC = colC.tupleSchema();

    MaterializedField fieldD = SchemaBuilder.columnSchema("d", MinorType.VARCHAR, DataMode.REQUIRED);
    ColumnMetadata colD = mapC.add(fieldD);

    MaterializedField fieldE = SchemaBuilder.columnSchema("e", MinorType.INT, DataMode.REQUIRED);
    ColumnMetadata colE = mapC.add(fieldE);

    assertEquals(1, root.size());
    assertEquals(1, mapA.size());
    assertEquals(1, mapB.size());
    assertEquals(2, mapC.size());

    assertSame(colA, root.metadata("a"));
    assertSame(colB, mapA.metadata("b.x"));
    assertSame(colC, mapB.metadata("c.y"));
    assertSame(colD, mapC.metadata("d"));
    assertSame(colE, mapC.metadata("e"));

    // The full name contains quoted names if the contain dots.
    // This name is more for diagnostic than semantic purposes.
    assertEquals("a", root.fullName(0));
    assertEquals("a.`b.x`", mapA.fullName(0));
    assertEquals("a.`b.x`.`c.y`", mapB.fullName(0));
    assertEquals("a.`b.x`.`c.y`.d", mapC.fullName(0));
    assertEquals("a.`b.x`.`c.y`.e", mapC.fullName(1));

    assertEquals(1, colA.schema().getChildren().size());
    assertEquals(1, colB.schema().getChildren().size());
    assertEquals(2, colC.schema().getChildren().size());

    // Yes, it is awful that MaterializedField does not provide indexed
    // access to its children. That's one reason we have the TupleMetadata
    // classes...
    // Note that the metadata layer does not store the materialized field.
    // (Doing so causes no end of synchronization problems.) So we test
    // for equivalence, not sameness.
    Iterator<MaterializedField> iterC = colC.schema().getChildren().iterator();
    assertTrue(fieldD.isEquivalent(iterC.next()));
    assertTrue(fieldE.isEquivalent(iterC.next()));

    // Copying should be deep.
    TupleMetadata root2 = root.copy();
    assertEquals(2, root2.metadata(0).tupleSchema().metadata(0).tupleSchema().metadata(0).tupleSchema().size());
    assert(root.isEquivalent(root2));

    // Generate a materialized field and compare.
    fieldA.addChild(fieldB);
    fieldB.addChild(fieldC);
    fieldC.addChild(fieldD);
    fieldC.addChild(fieldE);
    assertTrue(colA.schema().isEquivalent(fieldA));
  }

  @Test
  public void testMapTupleFromField() {

    // Create a materialized field with the desired structure.
    MaterializedField fieldA = SchemaBuilder.columnSchema("a", MinorType.MAP, DataMode.REQUIRED);

    MaterializedField fieldB = SchemaBuilder.columnSchema("b.x", MinorType.MAP, DataMode.REQUIRED);
    fieldA.addChild(fieldB);

    MaterializedField fieldC = SchemaBuilder.columnSchema("c.y", MinorType.MAP, DataMode.REQUIRED);
    fieldB.addChild(fieldC);

    MaterializedField fieldD = SchemaBuilder.columnSchema("d", MinorType.VARCHAR, DataMode.REQUIRED);
    fieldC.addChild(fieldD);

    MaterializedField fieldE = SchemaBuilder.columnSchema("e", MinorType.INT, DataMode.REQUIRED);
    fieldC.addChild(fieldE);

    // Create a metadata schema from the field.
    TupleMetadata root = new TupleSchema();
    ColumnMetadata colA = root.add(fieldA);

    // Get the parts.
    TupleMetadata mapA = colA.tupleSchema();
    ColumnMetadata colB = mapA.metadata("b.x");
    TupleMetadata mapB = colB.tupleSchema();
    ColumnMetadata colC = mapB.metadata("c.y");
    TupleMetadata mapC = colC.tupleSchema();
    ColumnMetadata colD = mapC.metadata("d");
    ColumnMetadata colE = mapC.metadata("e");

    // Validate. Should be same as earlier test that started
    // with the metadata.
    assertEquals(1, root.size());
    assertEquals(1, mapA.size());
    assertEquals(1, mapB.size());
    assertEquals(2, mapC.size());

    assertSame(colA, root.metadata("a"));
    assertSame(colB, mapA.metadata("b.x"));
    assertSame(colC, mapB.metadata("c.y"));
    assertSame(colD, mapC.metadata("d"));
    assertSame(colE, mapC.metadata("e"));

    // The full name contains quoted names if the contain dots.
    // This name is more for diagnostic than semantic purposes.
    assertEquals("a", root.fullName(0));
    assertEquals("a.`b.x`", mapA.fullName(0));
    assertEquals("a.`b.x`.`c.y`", mapB.fullName(0));
    assertEquals("a.`b.x`.`c.y`.d", mapC.fullName(0));
    assertEquals("a.`b.x`.`c.y`.e", mapC.fullName(1));

    assertEquals(1, colA.schema().getChildren().size());
    assertEquals(1, colB.schema().getChildren().size());
    assertEquals(2, colC.schema().getChildren().size());

    assertTrue(colA.schema().isEquivalent(fieldA));
  }

  @Test
  public void testUnionSchema() {
    TupleMetadata schema = new SchemaBuilder()
        .addUnion("u")
          .addType(MinorType.BIGINT)
          .addType(MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    assertEquals(1, schema.size());
    ColumnMetadata col = schema.metadata(0);
    assertTrue(col instanceof VariantColumnMetadata);
    assertEquals(MinorType.UNION, col.type());
    assertEquals(DataMode.OPTIONAL, col.mode());
    assertTrue(col.isNullable());
    assertFalse(col.isArray());
    assertTrue(col.isVariant());
    assertEquals(StructureType.VARIANT, col.structureType());

    VariantMetadata union = col.variantSchema();
    assertNotNull(union);
    assertEquals(2, union.size());
    assertTrue(union.hasType(MinorType.BIGINT));
    assertTrue(union.hasType(MinorType.VARCHAR));
    assertFalse(union.hasType(MinorType.INT));
    Collection<MinorType> types = union.types();
    assertNotNull(types);
    assertEquals(2, types.size());
    assertTrue(types.contains(MinorType.BIGINT));
    assertTrue(types.contains(MinorType.VARCHAR));
  }

  @Test
  public void testListSchema() {
    TupleMetadata schema = new SchemaBuilder()
        .addList("list")
          .addType(MinorType.BIGINT)
          .addType(MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    assertEquals(1, schema.size());
    ColumnMetadata col = schema.metadata(0);
    assertTrue(col instanceof VariantColumnMetadata);

    // Implementation shows through here: actual major
    // type is (LIST, OPTIONAL) even though the metadata
    // lies that this is a variant array.
    assertEquals(MinorType.LIST, col.type());
    assertEquals(DataMode.OPTIONAL, col.mode());
    assertTrue(col.isNullable());
    assertTrue(col.isArray());
    assertTrue(col.isVariant());
    assertEquals(StructureType.VARIANT, col.structureType());

    VariantMetadata union = col.variantSchema();
    assertNotNull(union);
    assertEquals(2, union.size());
    assertTrue(union.hasType(MinorType.BIGINT));
    assertTrue(union.hasType(MinorType.VARCHAR));
    assertFalse(union.hasType(MinorType.INT));
    Collection<MinorType> types = union.types();
    assertNotNull(types);
    assertEquals(2, types.size());
    assertTrue(types.contains(MinorType.BIGINT));
    assertTrue(types.contains(MinorType.VARCHAR));
  }

  @Test
  public void testNestedSchema() {
    TupleMetadata schema = new SchemaBuilder()
        .addList("list")
          .addType(MinorType.BIGINT)
          .addType(MinorType.VARCHAR)
          .addMap()
            .add("a", MinorType.INT)
            .add("b", MinorType.VARCHAR)
            .resumeUnion()
          .addList()
            .addType(MinorType.FLOAT8)
            .addType(MinorType.DECIMAL18)
            .resumeUnion()
          .resumeSchema()
        .buildSchema();

    assertEquals(1, schema.size());
    ColumnMetadata col = schema.metadata(0);
    assertTrue(col.isVariant());
    VariantMetadata union = col.variantSchema();
    assertNotNull(union);
    assertEquals(4, union.size());
    assertTrue(union.hasType(MinorType.MAP));
    assertTrue(union.hasType(MinorType.LIST));

    ColumnMetadata mapCol = union.member(MinorType.MAP);
    TupleMetadata mapSchema = mapCol.tupleSchema();
    assertEquals(2, mapSchema.size());

    ColumnMetadata listCol = union.member(MinorType.LIST);
    VariantMetadata listSchema = listCol.variantSchema();
    assertEquals(2, listSchema.size());
    assertTrue(listSchema.hasType(MinorType.FLOAT8));
    assertTrue(listSchema.hasType(MinorType.DECIMAL18));
  }

  @Test
  public void testDuplicateType() {
    try {
      new SchemaBuilder()
          .addList("list")
            .addType(MinorType.BIGINT)
            .addType(MinorType.BIGINT);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

  @Test
  public void testJsonString() {
    TupleMetadata schema = new SchemaBuilder()
      .add("col_int", MinorType.INT)
      .buildSchema();
    schema.setProperty("prop", "val");

    String expected = "{\"type\":\"tuple_schema\","
      + "\"columns\":[{\"name\":\"col_int\",\"type\":\"INT\",\"mode\":\"REQUIRED\"}],"
      + "\"properties\":{\"prop\":\"val\"}}";

    assertEquals(expected, schema.jsonString());
  }

  @Test
  public void testFromJsonTyped() {
    String jsonString = "{\"type\":\"tuple_schema\","
      + "\"columns\":[{\"name\":\"col_int\",\"type\":\"INT\",\"mode\":\"REQUIRED\"}],"
      + "\"properties\":{\"prop\":\"val\"}}";

    TupleMetadata result = TupleMetadata.of(jsonString);
    assertTrue(result instanceof TupleSchema);
    assertEquals(1, result.size());
    ColumnMetadata column = result.metadata("col_int");
    assertEquals(MinorType.INT, column.type());
    assertEquals("val", result.property("prop"));
  }

  @Test
  public void testFromJsonUntyped() {
    String untyped = "{\"columns\":[{\"name\":\"col_int\",\"type\":\"INT\",\"mode\":\"REQUIRED\"}],"
      + "\"properties\":{\"prop\":\"val\"}}";
    TupleMetadata result = TupleMetadata.of(untyped);
    assertTrue(result instanceof TupleSchema);
    assertEquals(1, result.size());
    ColumnMetadata column = result.metadata("col_int");
    assertEquals(MinorType.INT, column.type());
    assertEquals("val", result.property("prop"));
  }

  @Test
  public void testNullOrEmptyJsonString() {
    assertNull(TupleMetadata.of(null));
    assertNull(TupleMetadata.of(""));
    assertNull(TupleMetadata.of("   "));
  }

  @Test
  public void testCopy() {
    TupleMetadata schema = new SchemaBuilder()
      .add("a", MinorType.BIGINT)
      .build();

    schema.setIntProperty("int_prop", 1);
    schema.setProperty("string_prop", "A");

    TupleMetadata copy = schema.copy();

    assertTrue(schema.isEquivalent(copy));
    assertEquals(schema.properties(), copy.properties());
  }

  @Test
  public void testAddNewColumn() {
    TupleMetadata schema = new SchemaBuilder()
      .add("a", MinorType.BIGINT)
      .build();

    int index = schema.addColumn(
      MetadataUtils.newScalar("b",
        MajorType.newBuilder()
          .setMinorType(MinorType.VARCHAR)
          .setMode(DataMode.OPTIONAL).build()));

    assertEquals(1, index);
    assertEquals(2, schema.size());
  }

  @Test
  public void testAddNewProperty() {
    TupleMetadata schema = new SchemaBuilder()
      .add("a", MinorType.BIGINT)
      .build();

    assertTrue(schema.properties().isEmpty());

    schema.setIntProperty("int_prop", 1);
    schema.setProperty("string_prop", "A");

    assertEquals(2, schema.properties().size());
  }

  @Test
  public void testRemoveProperty() {
    TupleMetadata schema = new SchemaBuilder()
      .add("a", MinorType.BIGINT)
      .build();

    schema.setIntProperty("int_prop", 1);
    schema.setProperty("string_prop", "A");
    assertEquals(2, schema.properties().size());

    schema.removeProperty("int_prop");
    assertEquals(1, schema.properties().size());
    assertNull(schema.property("int_prop"));
    assertEquals("A", schema.property("string_prop"));

    schema.removeProperty("missing_prop");
    assertEquals(1, schema.properties().size());
  }

  @Test
  public void testDictColumn() {
    MaterializedField field = SchemaBuilder.columnSchema("d", MinorType.DICT, DataMode.REQUIRED);
    ColumnMetadata col = MetadataUtils.fromField(field);

    assertTrue(col instanceof DictColumnMetadata);
    assertNotNull(col.tupleSchema());
    assertEquals(0, col.tupleSchema().size());
    assertSame(col, col.tupleSchema().parent());

    DictColumnMetadata dictCol = (DictColumnMetadata) col;
    assertNull(dictCol.parentTuple());

    assertEquals(ColumnMetadata.StructureType.DICT, col.structureType());
    assertFalse(col.isNullable());
    assertFalse(col.isArray());
    assertFalse(col.isVariableWidth());
    assertTrue(col.isDict());
    assertFalse(col.isVariant());

    assertEquals(0, col.expectedWidth());
    col.setExpectedWidth(10);
    assertEquals(0, col.expectedWidth());

    assertEquals(1, col.expectedElementCount());
    col.setExpectedElementCount(2);
    assertEquals(1, col.expectedElementCount());
  }

  @Test
  public void testRepeatedDictColumn() {
    MaterializedField field = SchemaBuilder.columnSchema("da", MinorType.DICT, DataMode.REPEATED);
    ColumnMetadata col = MetadataUtils.fromField(field);

    assertTrue(col instanceof DictColumnMetadata);
    assertNotNull(col.tupleSchema());
    assertEquals(0, col.tupleSchema().size());

    assertFalse(col.isNullable());
    assertTrue(col.isArray());
    assertFalse(col.isVariableWidth());
    assertTrue(col.isDict());
    assertFalse(col.isVariant());

    assertEquals(0, col.expectedWidth());
    col.setExpectedWidth(10);
    assertEquals(0, col.expectedWidth());

    assertEquals(ColumnMetadata.DEFAULT_ARRAY_SIZE, col.expectedElementCount());

    col.setExpectedElementCount(2);
    assertEquals(2, col.expectedElementCount());
  }
}
