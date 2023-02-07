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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.rowSet.RowSet.ExtendableRowSet;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.NullableBigIntVector;
import org.apache.drill.exec.vector.NullableFloat8Vector;
import org.apache.drill.exec.vector.NullableIntVector;
import org.apache.drill.exec.vector.NullableVarCharVector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.accessor.ArrayReader;
import org.apache.drill.exec.vector.accessor.ArrayWriter;
import org.apache.drill.exec.vector.accessor.ObjectReader;
import org.apache.drill.exec.vector.accessor.ObjectType;
import org.apache.drill.exec.vector.accessor.ObjectWriter;
import org.apache.drill.exec.vector.accessor.ScalarReader;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.accessor.TupleReader;
import org.apache.drill.exec.vector.accessor.TupleWriter;
import org.apache.drill.exec.vector.accessor.VariantReader;
import org.apache.drill.exec.vector.accessor.VariantWriter;
import org.apache.drill.exec.vector.complex.ListVector;
import org.apache.drill.exec.vector.complex.MapVector;
import org.apache.drill.exec.vector.complex.UnionVector;
import org.apache.drill.test.SubOperatorTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests for readers and writers for union and list types.
 * <p>
 * Note that the union type is only partially supported in Drill.
 * The list type is unsupported. (However, the list type works in
 * the schema builder, row set writer, row set reader and the
 * result set builder. It does not, however, work in the Project
 * and other operators. Some assembly required for future use.)
 */

@Category(RowSetTests.class)
public class TestVariantAccessors extends SubOperatorTest {

  @Test
  public void testBuildRowSetUnion() {
    final TupleMetadata schema = new SchemaBuilder()

        // Union with simple and complex types

        .addUnion("u")
          .addType(MinorType.INT)
          .addMap()
            .addNullable("c", MinorType.BIGINT)
            .addNullable("d", MinorType.VARCHAR)
            .resumeUnion()
          .addList()
            .addType(MinorType.VARCHAR)
            .resumeUnion()
          .resumeSchema()
        .buildSchema();

    final ExtendableRowSet rowSet = fixture.rowSet(schema);
    final VectorContainer vc = rowSet.container();
    assertEquals(1, vc.getNumberOfColumns());

    // Single union

    final ValueVector vector = vc.getValueVector(0).getValueVector();
    assertTrue(vector instanceof UnionVector);
    final UnionVector union = (UnionVector) vector;

    final MapVector typeMap = union.getTypeMap();
    ValueVector member = typeMap.getChild(MinorType.INT.name());
    assertTrue(member instanceof NullableIntVector);

    // Inner map

    member = typeMap.getChild(MinorType.MAP.name());
    assertTrue(member instanceof MapVector);
    member = typeMap.getChild(MinorType.MAP.name());
    assertTrue(member instanceof MapVector);
    final MapVector childMap = (MapVector) member;
    ValueVector mapMember = childMap.getChild("c");
    assertNotNull(mapMember);
    assertTrue(mapMember instanceof NullableBigIntVector);
    mapMember = childMap.getChild("d");
    assertNotNull(mapMember);
    assertTrue(mapMember instanceof NullableVarCharVector);

    // Inner list

    member = typeMap.getChild(MinorType.LIST.name());
    assertTrue(member instanceof ListVector);
    final ListVector list = (ListVector) member;
    assertTrue(list.getDataVector() instanceof NullableVarCharVector);

    rowSet.clear();
  }

  /**
   * Test a variant (AKA "union vector") at the top level, using
   * just scalar values.
   */

  @Test
  public void testScalarVariant() {
    final TupleMetadata schema = new SchemaBuilder()
        .addUnion("u")
          .addType(MinorType.INT)
          .addType(MinorType.VARCHAR)
          .addType(MinorType.FLOAT8)
          .resumeSchema()
        .buildSchema();

    final ExtendableRowSet rs = fixture.rowSet(schema);
    final RowSetWriter writer = rs.writer();

    // Sanity check of writer structure

    final ObjectWriter wo = writer.column(0);
    assertEquals(ObjectType.VARIANT, wo.type());
    final VariantWriter vw = wo.variant();
    assertSame(vw, writer.variant(0));
    assertSame(vw, writer.variant("u"));
    assertTrue(vw.hasType(MinorType.INT));
    assertTrue(vw.hasType(MinorType.VARCHAR));
    assertTrue(vw.hasType(MinorType.FLOAT8));

    // Write values of different types

    vw.scalar(MinorType.INT).setInt(10);
    writer.save();

    vw.scalar(MinorType.VARCHAR).setString("fred");
    writer.save();

    // The entire variant is null

    vw.setNull();
    writer.save();

    vw.scalar(MinorType.FLOAT8).setDouble(123.45);
    writer.save();

    // Strange case: just the value is null, but the variant
    // is not null.

    vw.scalar(MinorType.INT).setNull();
    writer.save();

    // Marker to avoid fill-empty issues (fill-empties tested elsewhere.)

    vw.scalar(MinorType.INT).setInt(20);
    writer.save();

    final SingleRowSet result = writer.done();
    assertEquals(6, result.rowCount());

    // Read the values.

    final RowSetReader reader = result.reader();

    // Sanity check of structure

    final ObjectReader ro = reader.column(0);
    assertEquals(ObjectType.VARIANT, ro.type());
    final VariantReader vr = ro.variant();
    assertSame(vr, reader.variant(0));
    assertSame(vr, reader.variant("u"));
    for (final MinorType type : MinorType.values()) {
      if (type == MinorType.INT || type == MinorType.VARCHAR || type == MinorType.FLOAT8) {
        assertTrue(vr.hasType(type));
      } else {
        assertFalse(vr.hasType(type));
      }
    }

    // Can get readers up front

    final ScalarReader intReader = vr.scalar(MinorType.INT);
    final ScalarReader strReader = vr.scalar(MinorType.VARCHAR);
    final ScalarReader floatReader = vr.scalar(MinorType.FLOAT8);

    // Verify the data

    // Int 10

    assertTrue(reader.next());
    assertFalse(vr.isNull());
    assertSame(vr.dataType(), MinorType.INT);
    assertSame(intReader, vr.scalar());
    assertNotNull(vr.member());
    assertSame(vr.scalar(), vr.member().scalar());
    assertFalse(intReader.isNull());
    assertEquals(10, intReader.getInt());
    assertTrue(strReader.isNull());
    assertTrue(floatReader.isNull());

    // String "fred"

    assertTrue(reader.next());
    assertFalse(vr.isNull());
    assertSame(vr.dataType(), MinorType.VARCHAR);
    assertSame(strReader, vr.scalar());
    assertFalse(strReader.isNull());
    assertEquals("fred", strReader.getString());
    assertTrue(intReader.isNull());
    assertTrue(floatReader.isNull());

    // Null value

    assertTrue(reader.next());
    assertTrue(vr.isNull());
    assertNull(vr.dataType());
    assertNull(vr.scalar());
    assertTrue(intReader.isNull());
    assertTrue(strReader.isNull());
    assertTrue(floatReader.isNull());

    // Double 123.45

    assertTrue(reader.next());
    assertFalse(vr.isNull());
    assertSame(vr.dataType(), MinorType.FLOAT8);
    assertSame(floatReader, vr.scalar());
    assertFalse(floatReader.isNull());
    assertEquals(123.45, vr.scalar().getDouble(), 0.001);
    assertTrue(intReader.isNull());
    assertTrue(strReader.isNull());

    // Strange case: null int (but union is not null)

    assertTrue(reader.next());
    assertFalse(vr.isNull());
    assertSame(vr.dataType(), MinorType.INT);
    assertTrue(intReader.isNull());

    // Int 20

    assertTrue(reader.next());
    assertFalse(vr.isNull());
    assertFalse(intReader.isNull());
    assertEquals(20, intReader.getInt());

    assertFalse(reader.next());
    result.clear();
  }


  @Test
  public void testBuildRowSetScalarList() {
    final TupleMetadata schema = new SchemaBuilder()

        // Top-level single-element list

        .addList("list2")
          .addType(MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    final ExtendableRowSet rowSet = fixture.rowSet(schema);
    final VectorContainer vc = rowSet.container();
    assertEquals(1, vc.getNumberOfColumns());


    // Single-type list

    final ValueVector vector = vc.getValueVector(0).getValueVector();
    assertTrue(vector instanceof ListVector);
    final ListVector list = (ListVector) vector;
    assertTrue(list.getDataVector() instanceof NullableVarCharVector);

    rowSet.clear();
  }

  @Test
  public void testBuildRowSetUnionArray() {
    final TupleMetadata schema = new SchemaBuilder()

        // List with multiple types

        .addList("list1")
          .addType(MinorType.BIGINT)
          .addMap()
            .addNullable("a", MinorType.INT)
            .addNullable("b", MinorType.VARCHAR)
            .resumeUnion()

          // Nested single-element list

          .addList()
            .addType(MinorType.FLOAT8)
            .resumeUnion()
          .resumeSchema()
        .buildSchema();

    final ExtendableRowSet rowSet = fixture.rowSet(schema);
    final VectorContainer vc = rowSet.container();
    assertEquals(1, vc.getNumberOfColumns());

    // List with complex internal structure

    final ValueVector vector = vc.getValueVector(0).getValueVector();
    assertTrue(vector instanceof ListVector);
    final ListVector list = (ListVector) vector;
    assertTrue(list.getDataVector() instanceof UnionVector);
    final UnionVector union = (UnionVector) list.getDataVector();

    // Union inside the list

    final MajorType unionType = union.getField().getType();
    final List<MinorType> types = unionType.getSubTypeList();
    assertEquals(3, types.size());
    assertTrue(types.contains(MinorType.BIGINT));
    assertTrue(types.contains(MinorType.MAP));
    assertTrue(types.contains(MinorType.LIST));

    final MapVector typeMap = union.getTypeMap();
    ValueVector member = typeMap.getChild(MinorType.BIGINT.name());
    assertTrue(member instanceof NullableBigIntVector);

    // Map inside the list

    member = typeMap.getChild(MinorType.MAP.name());
    assertTrue(member instanceof MapVector);
    final MapVector childMap = (MapVector) member;
    ValueVector mapMember = childMap.getChild("a");
    assertNotNull(mapMember);
    assertTrue(mapMember instanceof NullableIntVector);
    mapMember = childMap.getChild("b");
    assertNotNull(mapMember);
    assertTrue(mapMember instanceof NullableVarCharVector);

    // Single-type list inside the outer list

    member = typeMap.getChild(MinorType.LIST.name());
    assertTrue(member instanceof ListVector);
    final ListVector childList = (ListVector) member;
    assertTrue(childList.getDataVector() instanceof NullableFloat8Vector);

    rowSet.clear();
  }

  /**
   * Test a variant (AKA "union vector") at the top level which
   * includes a map.
   */

  @Test
  public void testUnionWithMap() {
    final TupleMetadata schema = new SchemaBuilder()
        .addUnion("u")
          .addType(MinorType.VARCHAR)
          .addMap()
            .addNullable("a", MinorType.INT)
            .addNullable("b", MinorType.VARCHAR)
            .resumeUnion()
          .resumeSchema()
        .buildSchema();

    SingleRowSet result;

    // Write values

    {
      final ExtendableRowSet rs = fixture.rowSet(schema);
      final RowSetWriter writer = rs.writer();

      // Sanity check of writer structure

      final ObjectWriter wo = writer.column(0);
      assertEquals(ObjectType.VARIANT, wo.type());
      final VariantWriter vw = wo.variant();

      assertTrue(vw.hasType(MinorType.VARCHAR));
      final ObjectWriter strObj = vw.member(MinorType.VARCHAR);
      final ScalarWriter strWriter = strObj.scalar();
      assertSame(strWriter, vw.scalar(MinorType.VARCHAR));

      assertTrue(vw.hasType(MinorType.MAP));
      final ObjectWriter mapObj = vw.member(MinorType.MAP);
      final TupleWriter mWriter = mapObj.tuple();
      assertSame(mWriter, vw.tuple());

      final ScalarWriter aWriter = mWriter.scalar("a");
      final ScalarWriter bWriter = mWriter.scalar("b");

      // First row: string "first"

      vw.setType(MinorType.VARCHAR);
      strWriter.setString("first");
      writer.save();

      // Second row: a map

      vw.setType(MinorType.MAP);
      aWriter.setInt(20);
      bWriter.setString("fred");
      writer.save();

      // Third row: null

      vw.setNull();
      writer.save();

      // Fourth row: map with a null string

      vw.setType(MinorType.MAP);
      aWriter.setInt(40);
      bWriter.setNull();
      writer.save();

      // Fifth row: string "last"

      vw.setType(MinorType.VARCHAR);
      strWriter.setString("last");
      writer.save();

      result = writer.done();
      assertEquals(5, result.rowCount());
    }

    // Read the values.

    {
      final RowSetReader reader = result.reader();

      // Sanity check of structure

      final ObjectReader ro = reader.column(0);
      assertEquals(ObjectType.VARIANT, ro.type());
      final VariantReader vr = ro.variant();

      assertTrue(vr.hasType(MinorType.VARCHAR));
      final ObjectReader strObj = vr.member(MinorType.VARCHAR);
      final ScalarReader strReader = strObj.scalar();
      assertSame(strReader, vr.scalar(MinorType.VARCHAR));

      assertTrue(vr.hasType(MinorType.MAP));
      final ObjectReader mapObj = vr.member(MinorType.MAP);
      final TupleReader mReader = mapObj.tuple();
      assertSame(mReader, vr.tuple());

      final ScalarReader aReader = mReader.scalar("a");
      final ScalarReader bReader = mReader.scalar("b");

      // First row: string "first"

      assertTrue(reader.next());
      assertFalse(vr.isNull());
      assertEquals(MinorType.VARCHAR, vr.dataType());
      assertFalse(strReader.isNull());
      assertTrue(mReader.isNull());
      assertEquals("first", strReader.getString());

      // Second row: a map

      assertTrue(reader.next());
      assertFalse(vr.isNull());
      assertEquals(MinorType.MAP, vr.dataType());
      assertTrue(strReader.isNull());
      assertFalse(mReader.isNull());
      assertFalse(aReader.isNull());
      assertEquals(20, aReader.getInt());
      assertFalse(bReader.isNull());
      assertEquals("fred", bReader.getString());

      // Third row: null

      assertTrue(reader.next());
      assertTrue(vr.isNull());
      assertTrue(strReader.isNull());
      assertTrue(mReader.isNull());
      assertTrue(aReader.isNull());
      assertTrue(bReader.isNull());

      // Fourth row: map with a null string

      assertTrue(reader.next());
      assertEquals(MinorType.MAP, vr.dataType());
      assertEquals(40, aReader.getInt());
      assertTrue(bReader.isNull());

      // Fifth row: string "last"

      assertTrue(reader.next());
      assertEquals(MinorType.VARCHAR, vr.dataType());
      assertEquals("last", strReader.getString());

      assertFalse(reader.next());
    }

    result.clear();
  }

  /**
   * Test a scalar list. Should act just like a repeated type, with the
   * addition of allowing the list for a row to be null. But, a list
   * writer does not do auto-increment, so we must do that explicitly
   * after each write.
   */

  @Test
  public void testScalarList() {
    final TupleMetadata schema = new SchemaBuilder()
        .addList("list")
          .addType(MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    final ExtendableRowSet rowSet = fixture.rowSet(schema);
    final RowSetWriter writer = rowSet.writer();

    {
      final ObjectWriter listObj = writer.column(0);
      assertEquals(ObjectType.ARRAY, listObj.type());
      final ArrayWriter listArray = listObj.array();

      // The list contains only a scalar. But, because lists can,
      // in general, contain multiple contents, the list requires
      // an explicit save after each entry.

      final ObjectWriter itemObj = listArray.entry();
      assertEquals(ObjectType.SCALAR, itemObj.type());
      final ScalarWriter strWriter = itemObj.scalar();

      // First row: two strings and a null
      // Unlike a repeated type, a list can mark individual elements
      // as null.
      // List will automatically detect that data was written.

      strWriter.setString("fred");
      listArray.save();
      strWriter.setNull();
      listArray.save();
      strWriter.setString("wilma");
      listArray.save();
      writer.save();

      // Second row: null

      writer.save();

      // Third row: one string

      strWriter.setString("dino");
      listArray.save();
      writer.save();

      // Fourth row: empty array. Note that there is no trigger
      // to say that the column is not null, so we have to do it
      // explicitly.

      listArray.setNull(false);
      writer.save();

      // Last row: a null string and non-null

      strWriter.setNull();
      listArray.save();
      strWriter.setString("pebbles");
      listArray.save();
      writer.save();
    }

    final SingleRowSet result = writer.done();
    assertEquals(5, result.rowCount());

    {
      final RowSetReader reader = result.reader();

      final ObjectReader listObj = reader.column(0);
      assertEquals(ObjectType.ARRAY, listObj.type());
      final ArrayReader listArray = listObj.array();

      // The list is a repeated scalar

      assertEquals(ObjectType.SCALAR, listArray.entry().type());
      final ScalarReader strReader = listArray.scalar();

      // First row: two strings and a null

      assertTrue(reader.next());
      assertFalse(listArray.isNull());
      assertEquals(3, listArray.size());
      assertTrue(listArray.next());
      assertFalse(strReader.isNull());
      assertEquals("fred", strReader.getString());
      assertTrue(listArray.next());
      assertTrue(strReader.isNull());
      assertTrue(listArray.next());
      assertFalse(strReader.isNull());
      assertEquals("wilma", strReader.getString());
      assertFalse(listArray.next());

      // Second row: null

      assertTrue(reader.next());
      assertTrue(listArray.isNull());
      assertEquals(0, listArray.size());

      // Third row: one string

      assertTrue(reader.next());
      assertFalse(listArray.isNull());
      assertEquals(1, listArray.size());
      assertTrue(listArray.next());
      assertEquals("dino", strReader.getString());
      assertFalse(listArray.next());

      // Fourth row: empty array.

      assertTrue(reader.next());
      assertFalse(listArray.isNull());
      assertEquals(0, listArray.size());
      assertFalse(listArray.next());

      // Last row: a null string and non-null

      assertTrue(reader.next());
      assertFalse(listArray.isNull());
      assertEquals(2, listArray.size());
      assertTrue(listArray.next());
      assertTrue(strReader.isNull());
      assertTrue(listArray.next());
      assertFalse(strReader.isNull());
      assertEquals("pebbles", strReader.getString());
      assertFalse(listArray.next());

      assertFalse(reader.next());
    }

    result.clear();
  }

  /**
   * List of maps. Like a repeated map, but each list entry can be
   * null.
   */

  @Test
  public void testListOfMaps() {
    final TupleMetadata schema = new SchemaBuilder()
        .addList("list")
          .addMap()
            .addNullable("a", MinorType.INT)
            .addNullable("b", MinorType.VARCHAR)
            .resumeUnion()
          .resumeSchema()
        .buildSchema();

    final ExtendableRowSet rowSet = fixture.rowSet(schema);
    final RowSetWriter writer = rowSet.writer();

    {
      final ObjectWriter listObj = writer.column("list");
      assertEquals(ObjectType.ARRAY, listObj.type());
      final ArrayWriter listArray = listObj.array();
      final ObjectWriter itemObj = listArray.entry();
      assertEquals(ObjectType.TUPLE, itemObj.type());
      final TupleWriter mapWriter = itemObj.tuple();
      final ScalarWriter aWriter = mapWriter.scalar("a");
      final ScalarWriter bWriter = mapWriter.scalar("b");

      // First row:
      // {1, "fred"}, null, {3, null}

      aWriter.setInt(1);
      bWriter.setString("fred");

      listArray.save();
      // Can't mark the map as null. Instead, we simply skip
      // the map and the contained nullable members will automatically
      // back-fill each entry with a null value.
      listArray.save();

      aWriter.setInt(3);
      bWriter.setNull();
      listArray.save();
      writer.save();

      // Second row: null

      writer.save();

      // Third row: {null, "dino"}

      aWriter.setNull();
      bWriter.setString("dino");
      listArray.save();
      writer.save();

      // Fourth row: empty array. Note that there is no trigger
      // to say that the column is not null, so we have to do it
      // explicitly.

      listArray.setNull(false);
      writer.save();

      // Last row: {4, "pebbles"}

      aWriter.setInt(4);
      bWriter.setString("pebbles");
      listArray.save();
      writer.save();
    }

    final SingleRowSet result = writer.done();
    assertEquals(5, result.rowCount());

    {
      final RowSetReader reader = result.reader();

      final ObjectReader listObj = reader.column("list");
      assertEquals(ObjectType.ARRAY, listObj.type());
      final ArrayReader listArray = listObj.array();
      assertEquals(ObjectType.TUPLE, listArray.entry().type());
      final TupleReader mapReader = listArray.tuple();
      final ScalarReader aReader = mapReader.scalar("a");
      final ScalarReader bReader = mapReader.scalar("b");

      // First row:
      // {1, "fred"}, null, {3, null}

      assertTrue(reader.next());
      assertFalse(listArray.isNull());
      assertFalse(mapReader.isNull());
      assertEquals(3, listArray.size());

      assertTrue(listArray.next());
      assertFalse(aReader.isNull());
      assertEquals(1, aReader.getInt());
      assertFalse(bReader.isNull());
      assertEquals("fred", bReader.getString());

      assertTrue(listArray.next());
      // Awkward: the map has no null state, but its
      // members do.
      assertTrue(aReader.isNull());
      assertTrue(bReader.isNull());

      assertTrue(listArray.next());
      assertFalse(aReader.isNull());
      assertEquals(3, aReader.getInt());
      assertTrue(bReader.isNull());

      assertFalse(listArray.next());

      // Second row: null

      assertTrue(reader.next());
      assertTrue(listArray.isNull());
      assertEquals(0, listArray.size());

      // Third row: {null, "dino"}

      assertTrue(reader.next());
      assertFalse(listArray.isNull());
      assertEquals(1, listArray.size());
      assertTrue(listArray.next());
      assertTrue(aReader.isNull());
      assertFalse(bReader.isNull());
      assertEquals("dino", bReader.getString());
      assertFalse(listArray.next());

      // Fourth row: empty array.

      assertTrue(reader.next());
      assertFalse(listArray.isNull());
      assertEquals(0, listArray.size());
      assertFalse(listArray.next());

      // Last row: {4, "pebbles"}

      assertTrue(reader.next());
      assertFalse(listArray.isNull());
      assertEquals(1, listArray.size());
      assertTrue(listArray.next());
      assertEquals(4, aReader.getInt());
      assertEquals("pebbles", bReader.getString());
      assertFalse(listArray.next());

      assertFalse(reader.next());
    }

    result.clear();
  }

  /**
   * Test a union list.
   */

  @Test
  public void testListOfUnions() {
    final TupleMetadata schema = new SchemaBuilder()
        .addList("list")
          .addType(MinorType.INT)
          .addType(MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    final ExtendableRowSet rowSet = fixture.rowSet(schema);
    final RowSetWriter writer = rowSet.writer();

    {
      final ObjectWriter listObj = writer.column(0);
      assertEquals(ObjectType.ARRAY, listObj.type());
      final ArrayWriter listArray = listObj.array();
      final ObjectWriter itemObj = listArray.entry();
      assertEquals(ObjectType.VARIANT, itemObj.type());
      final VariantWriter variant = itemObj.variant();
      final ScalarWriter intWriter = variant.scalar(MinorType.INT);
      final ScalarWriter strWriter = variant.scalar(MinorType.VARCHAR);

      // First row: (1, "two", 3)

      variant.setType(MinorType.INT);
      intWriter.setInt(1);
      listArray.save();
      variant.setType(MinorType.VARCHAR);
      strWriter.setString("two");
      listArray.save();
      variant.setType(MinorType.INT);
      intWriter.setInt(3);
      listArray.save();
      writer.save();

      // Second row: null

      writer.save();

      // Third row: 4, null, "six", null int, null string

      variant.setType(MinorType.INT);
      intWriter.setInt(4);
      listArray.save();
      variant.setNull();
      listArray.save();
      variant.setType(MinorType.VARCHAR);
      strWriter.setString("six");
      listArray.save();
      variant.setType(MinorType.INT);
      intWriter.setNull();
      listArray.save();
      variant.setType(MinorType.VARCHAR);
      intWriter.setNull();
      listArray.save();
      writer.save();

      // Fourth row: empty array.

      listArray.setNull(false);
      writer.save();

      // Fifth row: 9

      variant.setType(MinorType.INT);
      intWriter.setInt(9);
      listArray.save();
      writer.save();
    }

    final SingleRowSet result = writer.done();
    assertEquals(5, result.rowCount());

    {
      final RowSetReader reader = result.reader();

      final ObjectReader listObj = reader.column(0);
      assertEquals(ObjectType.ARRAY, listObj.type());
      final ArrayReader listArray = listObj.array();
      assertEquals(ObjectType.VARIANT, listArray.entry().type());
      final VariantReader variant = listArray.variant();
      final ScalarReader intReader = variant.scalar(MinorType.INT);
      final ScalarReader strReader = variant.scalar(MinorType.VARCHAR);

      // First row: (1, "two", 3)

      assertTrue(reader.next());
      assertFalse(listArray.isNull());
      assertEquals(3, listArray.size());

      assertTrue(listArray.next());
      assertEquals(MinorType.INT, variant.dataType());
      assertFalse(intReader.isNull());
      assertTrue(strReader.isNull());
      assertEquals(1, intReader.getInt());
      assertEquals(1, variant.scalar().getInt());

      assertTrue(listArray.next());
      assertEquals(MinorType.VARCHAR, variant.dataType());
      assertTrue(intReader.isNull());
      assertFalse(strReader.isNull());
      assertEquals("two", strReader.getString());
      assertEquals("two", variant.scalar().getString());

      assertTrue(listArray.next());
      assertEquals(MinorType.INT, variant.dataType());
      assertEquals(3, intReader.getInt());

      assertFalse(listArray.next());

      // Second row: null

      assertTrue(reader.next());
      assertTrue(listArray.isNull());
      assertEquals(0, listArray.size());

      // Third row: 4, null, "six", null int, null string

      assertTrue(reader.next());
      assertEquals(5, listArray.size());

      assertTrue(listArray.next());
      assertEquals(4, intReader.getInt());

      assertTrue(listArray.next());
      assertTrue(variant.isNull());

      assertTrue(listArray.next());
      assertEquals("six", strReader.getString());

      assertTrue(listArray.next());
      assertEquals(MinorType.INT, variant.dataType());
      assertTrue(intReader.isNull());

      assertTrue(listArray.next());
      assertEquals(MinorType.VARCHAR, variant.dataType());
      assertTrue(strReader.isNull());

      assertFalse(listArray.next());

      // Fourth row: empty array.

      assertTrue(reader.next());
      assertFalse(listArray.isNull());
      assertEquals(0, listArray.size());
      assertFalse(listArray.next());

      // Fifth row: 9

      assertTrue(reader.next());
      assertEquals(1, listArray.size());

      assertTrue(listArray.next());
      assertEquals(9, intReader.getInt());
      assertFalse(listArray.next());

      assertFalse(reader.next());
    }

    result.clear();
  }

  /**
   * Test a variant (AKA "union vector") at the top level, using
   * just scalar values.
   */

  @Test
  public void testAddTypes() {
    final TupleMetadata batchSchema = new SchemaBuilder()
        .addNullable("v", MinorType.UNION)
        .buildSchema();

    final ExtendableRowSet rs = fixture.rowSet(batchSchema);
    final RowSetWriter writer = rs.writer();

    // Sanity check of writer structure

    final ObjectWriter wo = writer.column(0);
    assertEquals(ObjectType.VARIANT, wo.type());
    final VariantWriter vw = wo.variant();
    assertSame(vw, writer.variant(0));
    assertSame(vw, writer.variant("v"));
    for (final MinorType type : MinorType.values()) {
      assertFalse(vw.hasType(type));
    }

    // Write values of different types

    vw.scalar(MinorType.INT).setInt(10);
    assertTrue(vw.hasType(MinorType.INT));
    assertFalse(vw.hasType(MinorType.VARCHAR));
    writer.save();

    vw.scalar(MinorType.VARCHAR).setString("fred");
    assertTrue(vw.hasType(MinorType.VARCHAR));
    writer.save();

    vw.setNull();
    writer.save();

    vw.scalar(MinorType.FLOAT8).setDouble(123.45);
    assertTrue(vw.hasType(MinorType.INT));
    assertTrue(vw.hasType(MinorType.FLOAT8));
    writer.save();

    final SingleRowSet result = writer.done();

    assertEquals(4, result.rowCount());

    // Read the values.

    final RowSetReader reader = result.reader();

    // Sanity check of structure

    final ObjectReader ro = reader.column(0);
    assertEquals(ObjectType.VARIANT, ro.type());
    final VariantReader vr = ro.variant();
    assertSame(vr, reader.variant(0));
    assertSame(vr, reader.variant("v"));
    for (final MinorType type : MinorType.values()) {
      if (type == MinorType.INT || type == MinorType.VARCHAR || type == MinorType.FLOAT8) {
        assertTrue(vr.hasType(type));
      } else {
        assertFalse(vr.hasType(type));
      }
    }

    // Verify the data

    assertTrue(reader.next());
    assertFalse(vr.isNull());
    assertSame(vr.dataType(), MinorType.INT);
    assertSame(vr.scalar(MinorType.INT), vr.scalar());
    assertNotNull(vr.member());
    assertSame(vr.scalar(), vr.member().scalar());
    assertEquals(10, vr.scalar().getInt());

    assertTrue(reader.next());
    assertFalse(vr.isNull());
    assertSame(vr.dataType(), MinorType.VARCHAR);
    assertSame(vr.scalar(MinorType.VARCHAR), vr.scalar());
    assertEquals("fred", vr.scalar().getString());

    assertTrue(reader.next());
    assertTrue(vr.isNull());
    assertNull(vr.dataType());
    assertNull(vr.scalar());

    assertTrue(reader.next());
    assertFalse(vr.isNull());
    assertSame(vr.dataType(), MinorType.FLOAT8);
    assertSame(vr.scalar(MinorType.FLOAT8), vr.scalar());
    assertEquals(123.45, vr.scalar().getDouble(), 0.001);

    assertFalse(reader.next());
    result.clear();
  }

  /**
   * Test a variant (AKA "union vector") at the top level which includes
   * a list.
   */

  @Test
  public void testUnionWithList() {
    final TupleMetadata schema = new SchemaBuilder()
        .addUnion("u")
          .addType(MinorType.INT)
          .addList()
            .addType(MinorType.VARCHAR)
            .resumeUnion()
          .resumeSchema()
        .buildSchema();

    SingleRowSet result;

    // Write values

    {
      final ExtendableRowSet rs = fixture.rowSet(schema);
      final RowSetWriter writer = rs.writer();
      final VariantWriter vw = writer.variant("u");

      assertTrue(vw.hasType(MinorType.INT));
      final ScalarWriter intWriter = vw.scalar(MinorType.INT);

      assertTrue(vw.hasType(MinorType.LIST));
      final ArrayWriter aWriter = vw.array();
      final ScalarWriter strWriter = aWriter.scalar();

      // Row 1: 1, ["fred", "barney"]

      intWriter.setInt(1);
      strWriter.setString("fred");
      aWriter.save();
      strWriter.setString("barney");
      aWriter.save();
      writer.save();

      // Row 2, 2, ["wilma", "betty"]

      intWriter.setInt(2);
      strWriter.setString("wilma");
      aWriter.save();
      strWriter.setString("betty");
      aWriter.save();
      writer.save();

      result = writer.done();
      assertEquals(2, result.rowCount());
    }

    // Read the values.

    {
      final RowSetReader reader = result.reader();
      final VariantReader vr = reader.variant("u");

      assertTrue(vr.hasType(MinorType.INT));
      final ScalarReader intReader = vr.scalar(MinorType.INT);

      assertTrue(vr.hasType(MinorType.LIST));
      final ArrayReader aReader = vr.array();
      final ScalarReader strReader = aReader.scalar();

      assertTrue(reader.next());
      assertEquals(1, intReader.getInt());
      assertEquals(2, aReader.size());
      assertTrue(aReader.next());
      assertEquals("fred", strReader.getString());
      assertTrue(aReader.next());
      assertEquals("barney", strReader.getString());
      assertFalse(aReader.next());

      assertTrue(reader.next());
      assertEquals(2, intReader.getInt());
      assertEquals(2, aReader.size());
      assertTrue(aReader.next());
      assertEquals("wilma", strReader.getString());
      assertTrue(aReader.next());
      assertEquals("betty", strReader.getString());
      assertFalse(aReader.next());

      assertFalse(reader.next());
    }

    result.clear();
  }

  // TODO: Repeated list

}
