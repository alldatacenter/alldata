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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.ColumnMetadata.StructureType;
import org.apache.drill.exec.record.metadata.MapBuilder;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.RepeatedListBuilder;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.UnionBuilder;
import org.apache.drill.exec.record.metadata.VariantMetadata;
import org.apache.drill.test.DrillTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * The schema builder for tests has grown complex to handle maps, unions,
 * lists and repeated lists. This test verifies that it assembles the various
 * pieces correctly for the various nesting combinations.
 */
@Category(RowSetTests.class)
public class TestSchemaBuilder extends DrillTest {

  @Test
  public void testRowBasics() {
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.VARCHAR, DataMode.OPTIONAL) // Generic
        .add("b", MinorType.INT) // Required
        .addNullable("c", MinorType.FLOAT8) // Convenience
        .addArray("d", MinorType.BIGINT) // Convenience
        .buildSchema();

    assertEquals(4, schema.size());

    ColumnMetadata a = schema.metadata(0);
    assertEquals("a", a.name());
    assertEquals(MinorType.VARCHAR, a.type());
    assertEquals(DataMode.OPTIONAL, a.mode());

    ColumnMetadata b = schema.metadata(1);
    assertEquals("b", b.name());
    assertEquals(MinorType.INT, b.type());
    assertEquals(DataMode.REQUIRED, b.mode());

    ColumnMetadata c = schema.metadata(2);
    assertEquals("c", c.name());
    assertEquals(MinorType.FLOAT8, c.type());
    assertEquals(DataMode.OPTIONAL, c.mode());

    ColumnMetadata d = schema.metadata(3);
    assertEquals("d", d.name());
    assertEquals(MinorType.BIGINT, d.type());
    assertEquals(DataMode.REPEATED, d.mode());
  }

  @Test
  public void testRowPreBuilt() {

    MaterializedField aField = MaterializedField.create("a",
        Types.optional(MinorType.VARCHAR));
    ColumnMetadata bCol = MetadataUtils.newScalar("b",
        MinorType.INT, DataMode.REQUIRED);

    SchemaBuilder builder = new SchemaBuilder()
        .add(aField);

    // Internal method, does not return builder itself.
    builder.addColumn(bCol);

    TupleMetadata schema = builder.buildSchema();

    assertEquals(2, schema.size());

    ColumnMetadata a = schema.metadata(0);
    assertEquals("a", a.name());
    assertEquals(MinorType.VARCHAR, a.type());
    assertEquals(DataMode.OPTIONAL, a.mode());

    ColumnMetadata b = schema.metadata(1);
    assertEquals("b", b.name());
    assertEquals(MinorType.INT, b.type());
    assertEquals(DataMode.REQUIRED, b.mode());
  }

  /**
   * Tests creating a map within a row.
   * Also the basic map add column methods.
   */
  @Test
  public void testMapInRow() {
    TupleMetadata schema = new SchemaBuilder()
        .addMap("m")
          .add("a", MinorType.VARCHAR, DataMode.OPTIONAL) // Generic
          .add("b", MinorType.INT) // Required
          .addNullable("c", MinorType.FLOAT8) // Convenience
          .addArray("d", MinorType.BIGINT) // Convenience
          .resumeSchema()
        .buildSchema();

    assertEquals(1, schema.size());

    ColumnMetadata m = schema.metadata(0);
    assertEquals("m", m.name());
    assertTrue(m.isMap());
    assertEquals(DataMode.REQUIRED, m.mode());

    TupleMetadata mapSchema = m.tupleSchema();
    assertNotNull(mapSchema);
    assertEquals(4, mapSchema.size());

    ColumnMetadata a = mapSchema.metadata(0);
    assertEquals("a", a.name());
    assertEquals(MinorType.VARCHAR, a.type());
    assertEquals(DataMode.OPTIONAL, a.mode());

    ColumnMetadata b = mapSchema.metadata(1);
    assertEquals("b", b.name());
    assertEquals(MinorType.INT, b.type());
    assertEquals(DataMode.REQUIRED, b.mode());

    ColumnMetadata c = mapSchema.metadata(2);
    assertEquals("c", c.name());
    assertEquals(MinorType.FLOAT8, c.type());
    assertEquals(DataMode.OPTIONAL, c.mode());

    ColumnMetadata d = mapSchema.metadata(3);
    assertEquals("d", d.name());
    assertEquals(MinorType.BIGINT, d.type());
    assertEquals(DataMode.REPEATED, d.mode());
  }

  /**
   * Test building a union in the top-level schema.
   * Also tests the basic union add type methods.
   */
  @Test
  public void testUnionInRow() {
    TupleMetadata schema = new SchemaBuilder()
        .addUnion("u")
          .addType(MinorType.VARCHAR)
          .addType(MinorType.INT)
          .resumeSchema()
        .buildSchema();

    assertEquals(1, schema.size());

    ColumnMetadata u = schema.metadata(0);
    assertEquals("u", u.name());
    assertEquals(StructureType.VARIANT, u.structureType());
    assertTrue(u.isVariant());
    assertEquals(MinorType.UNION, u.type());
    assertEquals(DataMode.OPTIONAL, u.mode());

    VariantMetadata variant = u.variantSchema();
    assertNotNull(variant);
    assertEquals(2, variant.size());

    assertTrue(variant.hasType(MinorType.VARCHAR));
    ColumnMetadata vMember = variant.member(MinorType.VARCHAR);
    assertNotNull(vMember);
    assertEquals(Types.typeKey(MinorType.VARCHAR), vMember.name());
    assertEquals(MinorType.VARCHAR, vMember.type());
    assertEquals(DataMode.OPTIONAL, vMember.mode());

    assertTrue(variant.hasType(MinorType.INT));
    ColumnMetadata iMember = variant.member(MinorType.INT);
    assertNotNull(iMember);
    assertEquals(Types.typeKey(MinorType.INT), iMember.name());
    assertEquals(MinorType.INT, iMember.type());
    assertEquals(DataMode.OPTIONAL, iMember.mode());
  }

  /**
   * Test building a list (of unions) in the top-level schema.
   */
  @Test
  public void testListInRow() {
    TupleMetadata schema = new SchemaBuilder()
        .addList("list")
          .addType(MinorType.VARCHAR)
          .addType(MinorType.INT)
          .resumeSchema()
        .buildSchema();

    assertEquals(1, schema.size());

    ColumnMetadata list = schema.metadata(0);
    assertEquals("list", list.name());
    assertEquals(StructureType.VARIANT, list.structureType());
    assertTrue(list.isVariant());
    assertEquals(MinorType.LIST, list.type());

    // Yes, strange. Though a list is, essentially, an array, an
    // optional list has one set of semantics (in ListVector, not
    // really supported), while a repeated list has entirely different
    // semantics (in the RepeatedListVector) and is supported.
    assertEquals(DataMode.OPTIONAL, list.mode());

    VariantMetadata variant = list.variantSchema();
    assertNotNull(variant);
    assertEquals(2, variant.size());

    assertTrue(variant.hasType(MinorType.VARCHAR));
    ColumnMetadata vMember = variant.member(MinorType.VARCHAR);
    assertNotNull(vMember);
    assertEquals(Types.typeKey(MinorType.VARCHAR), vMember.name());
    assertEquals(MinorType.VARCHAR, vMember.type());
    assertEquals(DataMode.OPTIONAL, vMember.mode());

    assertTrue(variant.hasType(MinorType.INT));
    ColumnMetadata iMember = variant.member(MinorType.INT);
    assertNotNull(iMember);
    assertEquals(Types.typeKey(MinorType.INT), iMember.name());
    assertEquals(MinorType.INT, iMember.type());
    assertEquals(DataMode.OPTIONAL, iMember.mode());
  }

  /**
   * Test building a repeated list in the top-level schema.
   */
  @Test
  public void testRepeatedListInRow() {
    TupleMetadata schema = new SchemaBuilder()
        .addRepeatedList("list")
          .addArray(MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    assertEquals(1, schema.size());

    ColumnMetadata list = schema.metadata(0);
    assertEquals("list", list.name());
    assertFalse(list.isVariant());
    assertEquals(StructureType.MULTI_ARRAY, list.structureType());
    assertEquals(MinorType.LIST, list.type());

    // See note above for the (non-repeated) list.

    assertEquals(DataMode.REPEATED, list.mode());

    ColumnMetadata child = list.childSchema();
    assertNotNull(child);
    assertEquals(list.name(), child.name());
    assertEquals(MinorType.VARCHAR, child.type());
    assertEquals(DataMode.REPEATED, child.mode());
  }

  /**
   * Tests creating a dict within a row.
   * Also the basic dict add key and value columns methods.
   */
  @Test
  public void testDictInRow() {
    TupleMetadata schema = new SchemaBuilder()
        .addDict("d", MinorType.VARCHAR)
          .nullableValue(MinorType.FLOAT8)
          .resumeSchema()
        .buildSchema();

    assertEquals(1, schema.size());

    ColumnMetadata d = schema.metadata(0);
    assertEquals("d", d.name());
    assertTrue(d.isDict());
    assertEquals(DataMode.REQUIRED, d.mode());

    TupleMetadata dictSchema = d.tupleSchema();
    assertNotNull(dictSchema);
    assertEquals(2, dictSchema.size());

    ColumnMetadata keyMetadata = dictSchema.metadata(0);
    assertEquals("key", keyMetadata.name());
    assertEquals(MinorType.VARCHAR, keyMetadata.type());
    assertEquals(DataMode.REQUIRED, keyMetadata.mode());

    ColumnMetadata valueMetadata = dictSchema.metadata(1);
    assertEquals("value", valueMetadata.name());
    assertEquals(MinorType.FLOAT8, valueMetadata.type());
    assertEquals(DataMode.OPTIONAL, valueMetadata.mode());
  }

  /**
   * Test methods to provide a width (precision) for VarChar
   * columns. The schema builder does not provide shortcuts for
   * VarChar in lists, unions or repeated lists because these
   * cases are obscure and seldom (never?) used.
   */
  @Test
  public void testVarCharPrecision() {
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.VARCHAR, 21)
        .addNullable("b", MinorType.VARCHAR, 22)
        .addMap("m")
          .add("c", MinorType.VARCHAR, 23)
          .addNullable("d", MinorType.VARCHAR, 24)
          .resumeSchema()
        .buildSchema();

    assertEquals(3, schema.size());

    // Use name methods, just for variety

    assertEquals(21, schema.metadata("a").precision());
    assertEquals(22, schema.metadata("b").precision());
    TupleMetadata mapSchema = schema.metadata("m").tupleSchema();
    assertEquals(23, mapSchema.metadata("c").precision());
    assertEquals(24, mapSchema.metadata("d").precision());
  }

  /**
   * Test the ability to specify decimal precision and scale. Decimal is
   * broken in Drill, so we don't bother about decimals in unions,
   * lists or repeated lists, though those methods could be added.
   */
  @Test
  public void testDecimal() {
    TupleMetadata schema = new SchemaBuilder()
        .addNullable("a", MinorType.DECIMAL18, 5, 2)
        .add("b", MinorType.DECIMAL18, 6, 3)
        .addArray("c", MinorType.DECIMAL18, 7, 4)
        .addMap("m")
          .addNullable("d", MinorType.DECIMAL18, 8, 1)
          .resumeSchema()
        .buildSchema();

    // Use name methods, just for variety

    ColumnMetadata a = schema.metadata("a");
    assertEquals(DataMode.OPTIONAL, a.mode());
    assertEquals(5, a.precision());
    assertEquals(2, a.scale());

    ColumnMetadata b = schema.metadata("b");
    assertEquals(DataMode.REQUIRED, b.mode());
    assertEquals(6, b.precision());
    assertEquals(3, b.scale());

    ColumnMetadata c = schema.metadata("c");
    assertEquals(DataMode.REPEATED, c.mode());
    assertEquals(7, c.precision());
    assertEquals(4, c.scale());

    ColumnMetadata d = schema.metadata("m").tupleSchema().metadata("d");
    assertEquals(DataMode.OPTIONAL, d.mode());
    assertEquals(8, d.precision());
    assertEquals(1, d.scale());
  }

  @Test
  public void testVarDecimal() {
    TupleMetadata schema = new SchemaBuilder()
        .addNullable("a", MinorType.VARDECIMAL, 5, 2)
        .add("b", MinorType.VARDECIMAL, 6, 3)
        .addArray("c", MinorType.VARDECIMAL, 7, 4)
        .add("e", MinorType.VARDECIMAL)
        .add("g", MinorType.VARDECIMAL, 38, 4)
        .addMap("m")
          .addNullable("d", MinorType.VARDECIMAL, 8, 1)
          .add("f", MinorType.VARDECIMAL)
          .resumeSchema()
        .buildSchema();

    // Use name methods, just for variety
    ColumnMetadata a = schema.metadata("a");
    assertEquals(MinorType.VARDECIMAL, a.type());
    assertEquals(DataMode.OPTIONAL, a.mode());
    assertEquals(5, a.precision());
    assertEquals(2, a.scale());

    ColumnMetadata b = schema.metadata("b");
    assertEquals(MinorType.VARDECIMAL, b.type());
    assertEquals(DataMode.REQUIRED, b.mode());
    assertEquals(6, b.precision());
    assertEquals(3, b.scale());

    ColumnMetadata c = schema.metadata("c");
    assertEquals(MinorType.VARDECIMAL, c.type());
    assertEquals(DataMode.REPEATED, c.mode());
    assertEquals(7, c.precision());
    assertEquals(4, c.scale());

    ColumnMetadata e = schema.metadata("e");
    assertEquals(MinorType.VARDECIMAL, e.type());
    assertEquals(DataMode.REQUIRED, e.mode());
    assertEquals(38, e.precision());
    assertEquals(0, e.scale());

    ColumnMetadata g = schema.metadata("g");
    assertEquals(MinorType.VARDECIMAL, g.type());
    assertEquals(DataMode.REQUIRED, g.mode());
    assertEquals(38, g.precision());
    assertEquals(4, g.scale());

    ColumnMetadata d = schema.metadata("m").tupleSchema().metadata("d");
    assertEquals(MinorType.VARDECIMAL, d.type());
    assertEquals(DataMode.OPTIONAL, d.mode());
    assertEquals(8, d.precision());
    assertEquals(1, d.scale());

    ColumnMetadata f = schema.metadata("m").tupleSchema().metadata("f");
    assertEquals(MinorType.VARDECIMAL, f.type());
    assertEquals(DataMode.REQUIRED, f.mode());
    assertEquals(38, f.precision());
    assertEquals(0, f.scale());
  }

  @Test
  public void testVarDecimalOverflow() {

    try {
      new SchemaBuilder()
        .add("a", MinorType.VARDECIMAL, 39, 0)
        .buildSchema();
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      new SchemaBuilder()
        .add("a", MinorType.VARDECIMAL, -1, 0)
        .buildSchema();
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      new SchemaBuilder()
        .add("a", MinorType.VARDECIMAL, 38, -1)
        .buildSchema();
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      new SchemaBuilder()
        .add("a", MinorType.VARDECIMAL, 5, 6)
        .buildSchema();
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

  /**
   * Verify that the map-in-map plumbing works.
   */
  @Test
  public void testMapInMap() {
    TupleMetadata schema = new SchemaBuilder()
        .addMap("m1")
          .addMap("m2")
            .add("a", MinorType.INT)
            .resumeMap()
          .add("b", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    TupleMetadata m1Schema = schema.metadata("m1").tupleSchema();
    TupleMetadata m2Schema = m1Schema.metadata("m2").tupleSchema();

    ColumnMetadata a = m2Schema.metadata(0);
    assertEquals("a", a.name());
    assertEquals(MinorType.INT, a.type());

    ColumnMetadata b = m1Schema.metadata(1);
    assertEquals("b", b.name());
    assertEquals(MinorType.VARCHAR, b.type());
  }

  /**
   * Verify that the union-in-map plumbing works.
   */
  @Test
  public void testUnionInMap() {
    TupleMetadata schema = new SchemaBuilder()
        .addMap("m1")
          .addUnion("u")
            .addType(MinorType.INT)
            .resumeMap()
          .add("b", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    TupleMetadata m1Schema = schema.metadata("m1").tupleSchema();
    VariantMetadata uSchema = m1Schema.metadata("u").variantSchema();

    assertTrue(uSchema.hasType(MinorType.INT));
    assertFalse(uSchema.hasType(MinorType.VARCHAR));

    ColumnMetadata b = m1Schema.metadata(1);
    assertEquals("b", b.name());
    assertEquals(MinorType.VARCHAR, b.type());
  }

  /**
   * Verify that the repeated list-in-map plumbing works.
   */
  @Test
  public void testRepeatedListInMap() {
    TupleMetadata schema = new SchemaBuilder()
        .addMap("m1")
          .addRepeatedList("r")
            .addArray(MinorType.INT)
            .resumeMap()
          .add("b", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    TupleMetadata m1Schema = schema.metadata("m1").tupleSchema();

    ColumnMetadata r = m1Schema.metadata(0);
    assertEquals("r", r.name());
    assertEquals(MinorType.LIST, r.type());
    assertEquals(DataMode.REPEATED, r.mode());

    ColumnMetadata child = r.childSchema();
    assertEquals(r.name(), child.name());
    assertEquals(MinorType.INT, child.type());

    ColumnMetadata b = m1Schema.metadata(1);
    assertEquals("b", b.name());
    assertEquals(MinorType.VARCHAR, b.type());
  }

  @Test
  public void testMapInUnion() {
    TupleMetadata schema = new SchemaBuilder()
        .addUnion("u")
          .addMap()
            .add("a", MinorType.INT)
            .add("b", MinorType.VARCHAR)
            .resumeUnion()
          .addType(MinorType.FLOAT8)
          .resumeSchema()
        .buildSchema();

    ColumnMetadata u = schema.metadata("u");
    VariantMetadata variant = u.variantSchema();

    ColumnMetadata mapType = variant.member(MinorType.MAP);
    assertNotNull(mapType);

    TupleMetadata mapSchema = mapType.tupleSchema();
    assertEquals(2, mapSchema.size());

    assertTrue(variant.hasType(MinorType.FLOAT8));
    assertFalse(variant.hasType(MinorType.VARCHAR));
  }

  @Test
  public void testRepeatedListInUnion() {
    TupleMetadata schema = new SchemaBuilder()
        .addUnion("u")
          .addRepeatedList()
            .addArray(MinorType.INT)
            .resumeUnion()
          .addType(MinorType.FLOAT8)
          .resumeSchema()
        .buildSchema();

    ColumnMetadata u = schema.metadata("u");
    VariantMetadata variant = u.variantSchema();

    ColumnMetadata listType = variant.member(MinorType.LIST);
    assertNotNull(listType);

    ColumnMetadata child = listType.childSchema();
    assertEquals(MinorType.INT, child.type());

    assertTrue(variant.hasType(MinorType.FLOAT8));
    assertFalse(variant.hasType(MinorType.VARCHAR));
  }

  // Note: list-in-union may be supported, but this area of the code is obscure
  // and not a priority to maintain. The problem will be that both lists
  // and repeated lists key off of the same type code: LIST, so it is
  // ambiguous which is supported. The schema builder muddles through this
  // case, but the rest of the code might not.
  @Test
  public void testListInUnion() {
    TupleMetadata schema = new SchemaBuilder()
        .addUnion("u")
          .addList()
            .addType(MinorType.INT)
            .resumeUnion()
          .addType(MinorType.FLOAT8)
          .resumeSchema()
        .buildSchema();

    ColumnMetadata u = schema.metadata("u");
    VariantMetadata variant = u.variantSchema();

    ColumnMetadata listType = variant.member(MinorType.LIST);
    assertNotNull(listType);
    VariantMetadata listSchema = listType.variantSchema();
    assertTrue(listSchema.hasType(MinorType.INT));

    assertTrue(variant.hasType(MinorType.FLOAT8));
    assertFalse(variant.hasType(MinorType.VARCHAR));
  }

  // Note: union-in-union not supported in Drill
  @Test
  public void testMapInRepeatedList() {
    TupleMetadata schema = new SchemaBuilder()
        .addRepeatedList("x")
          .addMapArray()
            .add("a", MinorType.INT)
            .addNullable("b", MinorType.VARCHAR)
            .resumeList()
          .resumeSchema()
        .buildSchema();

    ColumnMetadata list = schema.metadata("x");
    ColumnMetadata mapCol = list.childSchema();
    assertTrue(mapCol.isMap());
    TupleMetadata mapSchema = mapCol.tupleSchema();

    ColumnMetadata a = mapSchema.metadata("a");
    assertEquals(MinorType.INT, a.type());
    assertEquals(DataMode.REQUIRED, a.mode());

    ColumnMetadata b = mapSchema.metadata("b");
    assertEquals(MinorType.VARCHAR, b.type());
    assertEquals(DataMode.OPTIONAL, b.mode());
  }

  /**
   * Test that repeated lists can be nested to provide 3D or
   * higher dimensions.
   */
  @Test
  public void testRepeatedListInRepeatedList() {
    TupleMetadata schema = new SchemaBuilder()
        .addRepeatedList("x")
          .addDimension()
            .addArray(MinorType.VARCHAR)
            .resumeList()
          .resumeSchema()
        .buildSchema();

    assertEquals(1, schema.size());

    ColumnMetadata outerList = schema.metadata(0);
    assertEquals("x", outerList.name());
    assertEquals(StructureType.MULTI_ARRAY, outerList.structureType());
    assertEquals(MinorType.LIST, outerList.type());
    assertEquals(DataMode.REPEATED, outerList.mode());

    ColumnMetadata innerList = outerList.childSchema();
    assertNotNull(innerList);
    assertEquals(outerList.name(), innerList.name());
    assertEquals(StructureType.MULTI_ARRAY, innerList.structureType());
    assertEquals(MinorType.LIST, innerList.type());
    assertEquals(DataMode.REPEATED, innerList.mode());

    ColumnMetadata child = innerList.childSchema();
    assertNotNull(child);
    assertEquals(outerList.name(), child.name());
    assertEquals(MinorType.VARCHAR, child.type());
    assertEquals(DataMode.REPEATED, child.mode());
  }

  @Test
  public void testRepeatedListShortcut() {
    TupleMetadata schema = new SchemaBuilder()
        .addArray("x", MinorType.VARCHAR, 3)
        .buildSchema();

    assertEquals(1, schema.size());

    ColumnMetadata outerList = schema.metadata(0);
    assertEquals("x", outerList.name());
    assertEquals(StructureType.MULTI_ARRAY, outerList.structureType());
    assertEquals(MinorType.LIST, outerList.type());
    assertEquals(DataMode.REPEATED, outerList.mode());

    ColumnMetadata innerList = outerList.childSchema();
    assertNotNull(innerList);
    assertEquals(outerList.name(), innerList.name());
    assertEquals(StructureType.MULTI_ARRAY, innerList.structureType());
    assertEquals(MinorType.LIST, innerList.type());
    assertEquals(DataMode.REPEATED, innerList.mode());

    ColumnMetadata child = innerList.childSchema();
    assertNotNull(child);
    assertEquals(outerList.name(), child.name());
    assertEquals(MinorType.VARCHAR, child.type());
    assertEquals(DataMode.REPEATED, child.mode());
  }

  @Test
  public void testStandaloneMapBuilder() {
    ColumnMetadata columnMetadata= new MapBuilder("m1", DataMode.OPTIONAL)
      .addNullable("b", MinorType.BIGINT)
      .addMap("m2")
      .addNullable("v", MinorType.VARCHAR)
      .resumeMap()
      .buildColumn();

    assertTrue(columnMetadata.isMap());
    assertTrue(columnMetadata.isNullable());
    assertEquals("m1", columnMetadata.name());

    TupleMetadata schema = columnMetadata.tupleSchema();

    ColumnMetadata col0 = schema.metadata(0);
    assertEquals("b", col0.name());
    assertEquals(MinorType.BIGINT, col0.type());
    assertTrue(col0.isNullable());

    ColumnMetadata col1 = schema.metadata(1);
    assertEquals("m2", col1.name());
    assertTrue(col1.isMap());
    assertFalse(col1.isNullable());

    ColumnMetadata child = col1.tupleSchema().metadata(0);
    assertEquals("v", child.name());
    assertEquals(MinorType.VARCHAR, child.type());
    assertTrue(child.isNullable());
  }

  @Test
  public void testStandaloneRepeatedListBuilder() {
    ColumnMetadata columnMetadata = new RepeatedListBuilder("l")
      .addMapArray()
      .addNullable("v", MinorType.VARCHAR)
      .add("i", MinorType.INT)
      .resumeList()
      .buildColumn();

    assertTrue(columnMetadata.isArray());
    assertEquals("l", columnMetadata.name());
    assertEquals(MinorType.LIST, columnMetadata.type());

    ColumnMetadata child = columnMetadata.childSchema();
    assertEquals("l", child.name());
    assertTrue(child.isArray());
    assertTrue(child.isMap());

    TupleMetadata mapSchema = child.tupleSchema();

    ColumnMetadata col0 = mapSchema.metadata(0);
    assertEquals("v", col0.name());
    assertEquals(MinorType.VARCHAR, col0.type());
    assertTrue(col0.isNullable());

    ColumnMetadata col1 = mapSchema.metadata(1);
    assertEquals("i", col1.name());
    assertEquals(MinorType.INT, col1.type());
    assertFalse(col1.isNullable());
  }

  @Test
  public void testStandaloneUnionBuilder() {
    ColumnMetadata columnMetadata = new UnionBuilder("u", MinorType.UNION)
      .addType(MinorType.INT)
      .addType(MinorType.VARCHAR)
      .buildColumn();

    assertEquals("u", columnMetadata.name());
    assertTrue(columnMetadata.isVariant());

    VariantMetadata variantMetadata = columnMetadata.variantSchema();
    assertTrue(variantMetadata.hasType(MinorType.INT));
    assertTrue(variantMetadata.hasType(MinorType.VARCHAR));
  }
}
