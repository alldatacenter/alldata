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

import static org.apache.drill.test.rowSet.RowSetUtilities.listValue;
import static org.apache.drill.test.rowSet.RowSetUtilities.mapValue;
import static org.apache.drill.test.rowSet.RowSetUtilities.objArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.strArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.variantArray;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.NullableIntVector;
import org.apache.drill.exec.vector.NullableVarCharVector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.accessor.ArrayWriter;
import org.apache.drill.exec.vector.accessor.ObjectType;
import org.apache.drill.exec.vector.accessor.ObjectWriter;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.accessor.TupleWriter;
import org.apache.drill.exec.vector.accessor.ValueType;
import org.apache.drill.exec.vector.accessor.VariantWriter;
import org.apache.drill.exec.vector.accessor.writer.EmptyListShim;
import org.apache.drill.exec.vector.accessor.writer.ListWriterImpl;
import org.apache.drill.exec.vector.accessor.writer.SimpleListShim;
import org.apache.drill.exec.vector.accessor.writer.UnionVectorShim;
import org.apache.drill.exec.vector.accessor.writer.UnionWriterImpl;
import org.apache.drill.exec.vector.complex.ListVector;
import org.apache.drill.exec.vector.complex.UnionVector;
import org.apache.drill.test.SubOperatorTest;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.exec.physical.rowSet.RowSetReader;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.apache.drill.shaded.guava.com.google.common.base.Charsets;

/**
 * Tests the result set loader support for union vectors. Union vectors
 * are only lightly supported in Apache Drill and not supported at all
 * in commercial versions. They have may problems: both in code and in theory.
 * Most operators do not support them. But, JSON uses them, so they must
 * be made to work in the result set loader layer.
 */
@Category(RowSetTests.class)
public class TestResultSetLoaderUnions extends SubOperatorTest {

  @Test
  public void testUnionBasics() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("id", MinorType.INT)
        .addUnion("u")
          .addType(MinorType.VARCHAR)
          .addMap()
            .addNullable("a", MinorType.INT)
            .addNullable("b", MinorType.VARCHAR)
            .resumeUnion()
          .resumeSchema()
        .buildSchema();

    final ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .readerSchema(schema)
        .build();
    final ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    final RowSetLoader writer = rsLoader.writer();

    // Sanity check of writer structure
    final ObjectWriter wo = writer.column(1);
    assertEquals(ObjectType.VARIANT, wo.type());
    final VariantWriter vw = wo.variant();

    assertTrue(vw.hasType(MinorType.VARCHAR));
    assertNotNull(vw.memberWriter(MinorType.VARCHAR));

    assertTrue(vw.hasType(MinorType.MAP));
    assertNotNull(vw.memberWriter(MinorType.MAP));

    // Write values
    rsLoader.startBatch();
    writer
      .addRow(1, "first")
      .addRow(2, mapValue(20, "fred"))
      .addRow(3, null)
      .addRow(4, mapValue(40, null))
      .addRow(5, "last");

    // Verify the values.
    // (Relies on the row set level union tests having passed.)
    final SingleRowSet expected = fixture.rowSetBuilder(schema)
      .addRow(1, "first")
      .addRow(2, mapValue(20, "fred"))
      .addRow(3, null)
      .addRow(4, mapValue(40, null))
      .addRow(5, "last")
      .build();

    RowSetUtilities.verify(expected, fixture.wrap(rsLoader.harvest()));
  }

  @Test
  public void testUnionAddTypes() {
    final ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator());
    final RowSetLoader writer = rsLoader.writer();

    rsLoader.startBatch();

    // First row, (1, "first"), create types as we go.
    writer.start();
    writer.addColumn(SchemaBuilder.columnSchema("id", MinorType.INT, DataMode.REQUIRED));
    writer.scalar("id").setInt(1);
    writer.addColumn(SchemaBuilder.columnSchema("u", MinorType.UNION, DataMode.OPTIONAL));
    final VariantWriter variant = writer.column("u").variant();
    variant.member(MinorType.VARCHAR).scalar().setString("first");
    writer.save();

    // Second row, (2, {20, "fred"}), create types as we go.
    writer.start();
    writer.scalar("id").setInt(2);
    final TupleWriter innerMap = variant.member(MinorType.MAP).tuple();
    innerMap.addColumn(SchemaBuilder.columnSchema("a", MinorType.INT, DataMode.OPTIONAL));
    innerMap.scalar("a").setInt(20);
    innerMap.addColumn(SchemaBuilder.columnSchema("b", MinorType.VARCHAR, DataMode.OPTIONAL));
    innerMap.scalar("b").setString("fred");
    writer.save();

    // Write remaining rows using convenient methods, using
    // schema defined above.
    writer
      .addRow(3, null)
      .addRow(4, mapValue(40, null))
      .addRow(5, "last");

    // Verify the values.
    // (Relies on the row set level union tests having passed.)
    final TupleMetadata schema = new SchemaBuilder()
        .add("id", MinorType.INT)
        .addUnion("u")
          .addType(MinorType.VARCHAR)
          .addMap()
            .addNullable("a", MinorType.INT)
            .addNullable("b", MinorType.VARCHAR)
            .resumeUnion()
          .resumeSchema()
        .buildSchema();

    final SingleRowSet expected = fixture.rowSetBuilder(schema)
      .addRow(1, "first")
      .addRow(2, mapValue(20, "fred"))
      .addRow(3, null)
      .addRow(4, mapValue(40, null))
      .addRow(5, "last")
      .build();

    final RowSet result = fixture.wrap(rsLoader.harvest());
    RowSetUtilities.verify(expected, result);
  }

  @Test
  public void testUnionOverflow() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("id", MinorType.INT)
        .addUnion("u")
          .addType(MinorType.INT)
          .addType(MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    final ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .rowCountLimit(ValueVector.MAX_ROW_COUNT)
        .readerSchema(schema)
        .build();
    final ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    final RowSetLoader writer = rsLoader.writer();

    // Fill the batch with enough data to cause overflow.
    // Fill even rows with a Varchar, odd rows with an int.
    // Data must be large enough to cause overflow before 32K rows
    // (the half that get strings.
    // 16 MB / 32 K = 512 bytes
    // Make a bit bigger to overflow early.
    final int strLength = 600;
    final byte[] value = new byte[strLength - 6];
    Arrays.fill(value, (byte) 'X');
    final String strValue = new String(value, Charsets.UTF_8);
    int count = 0;

    rsLoader.startBatch();
    while (! writer.isFull()) {
      if (count % 2 == 0) {
        writer.addRow(count, String.format("%s%06d", strValue, count));
      } else {
        writer.addRow(count, count * 10);
      }
      count++;
    }

    // Number of rows should be driven by vector size.
    // Our row count should include the overflow row
    final int expectedCount = ValueVector.MAX_BUFFER_SIZE / strLength * 2;
    assertEquals(expectedCount + 1, count);

    // Loader's row count should include only "visible" rows
    assertEquals(expectedCount, writer.rowCount());

    // Total count should include invisible and look-ahead rows.
    assertEquals(expectedCount + 1, rsLoader.totalRowCount());

    // Result should exclude the overflow row
    RowSet result = fixture.wrap(rsLoader.harvest());
    assertEquals(expectedCount, result.rowCount());

    // Verify the data.
    RowSetReader reader = result.reader();
    int readCount = 0;
    while (reader.next()) {
      assertEquals(readCount, reader.scalar(0).getInt());
      if (readCount % 2 == 0) {
        assertEquals(String.format("%s%06d", strValue, readCount),
            reader.variant(1).scalar().getString());
      } else {
        assertEquals(readCount * 10, reader.variant(1).scalar().getInt());
      }
      readCount++;
    }
    assertEquals(readCount, result.rowCount());
    result.clear();

    // Write a few more rows to verify the overflow row.
    rsLoader.startBatch();
    for (int i = 0; i < 1000; i++) {
      if (count % 2 == 0) {
        writer.addRow(count, String.format("%s%06d", strValue, count));
      } else {
        writer.addRow(count, count * 10);
      }
      count++;
    }

    result = fixture.wrap(rsLoader.harvest());
    assertEquals(1001, result.rowCount());

    final int startCount = readCount;
    reader = result.reader();
    while (reader.next()) {
      assertEquals(readCount, reader.scalar(0).getInt());
      if (readCount % 2 == 0) {
        assertEquals(String.format("%s%06d", strValue, readCount),
            reader.variant(1).scalar().getString());
      } else {
        assertEquals(readCount * 10, reader.variant(1).scalar().getInt());
      }
      readCount++;
    }
    assertEquals(readCount - startCount, result.rowCount());
    result.clear();
    rsLoader.close();
  }

  /**
   * Test for the case of a list defined to contain exactly one type.
   * Relies on the row set tests to verify that the single type model
   * works for lists. Here we test that the ResultSetLoader put the
   * pieces together correctly.
   */
  @Test
  public void testSimpleList() {

    // Schema with a list declared with one type, not expandable
    final TupleMetadata schema = new SchemaBuilder()
        .add("id", MinorType.INT)
        .addList("list")
          .addType(MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    schema.metadata("list").variantSchema().becomeSimple();

    final ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .readerSchema(schema)
        .build();
    final ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
    final RowSetLoader writer = rsLoader.writer();

    // Sanity check: should be an array of Varchar because we said the
    // types within the list is not expandable.
    final ArrayWriter arrWriter = writer.array("list");
    assertEquals(ObjectType.SCALAR, arrWriter.entryType());
    final ScalarWriter strWriter = arrWriter.scalar();
    assertEquals(ValueType.STRING, strWriter.valueType());

    // Can write a batch as if this was a repeated Varchar, except
    // that any value can also be null.
    rsLoader.startBatch();
    writer
      .addRow(1, strArray("fred", "barney"))
      .addRow(2, null)
      .addRow(3, strArray("wilma", "betty", "pebbles"));

    // Verify
    final SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addRow(1, strArray("fred", "barney"))
        .addRow(2, null)
        .addRow(3, strArray("wilma", "betty", "pebbles"))
        .build();

    RowSetUtilities.verify(expected, fixture.wrap(rsLoader.harvest()));
  }

  /**
   * Test a simple list created dynamically at load time.
   * The list must include a single type member.
   */
  @Test
  public void testSimpleListDynamic() {

    final ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator());
    final RowSetLoader writer = rsLoader.writer();

    // Can write a batch as if this was a repeated Varchar, except
    // that any value can also be null.
    rsLoader.startBatch();

    writer.addColumn(MaterializedField.create("id", Types.required(MinorType.INT)));

    final ColumnMetadata colSchema = MetadataUtils.newVariant("list", DataMode.REPEATED);
    colSchema.variantSchema().addType(MinorType.VARCHAR);
    colSchema.variantSchema().becomeSimple();
    writer.addColumn(colSchema);

    // Sanity check: should be an array of Varchar because we said the
    // types within the list is not expandable.
    final ArrayWriter arrWriter = writer.array("list");
    assertEquals(ObjectType.SCALAR, arrWriter.entryType());
    final ScalarWriter strWriter = arrWriter.scalar();
    assertEquals(ValueType.STRING, strWriter.valueType());

    writer
      .addRow(1, strArray("fred", "barney"))
      .addRow(2, null)
      .addRow(3, strArray("wilma", "betty", "pebbles"));

    // Verify
    final TupleMetadata schema = new SchemaBuilder()
        .add("id", MinorType.INT)
        .addList("list")
          .addType(MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();
    final SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addRow(1, strArray("fred", "barney"))
        .addRow(2, null)
        .addRow(3, strArray("wilma", "betty", "pebbles"))
        .build();

    RowSetUtilities.verify(expected, fixture.wrap(rsLoader.harvest()));
  }

  /**
   * Try to create a simple (non-expandable) list without
   * giving a member type. Expected to fail.
   */
  @Test
  public void testSimpleListNoTypes() {

    // Schema with a list declared with one type, not expandable
    final TupleMetadata schema = new SchemaBuilder()
        .add("id", MinorType.INT)
        .addList("list")
          .resumeSchema()
        .buildSchema();

    try {
      schema.metadata("list").variantSchema().becomeSimple();
      fail();
    } catch (final IllegalStateException e) {
      // expected
    }
  }

  /**
   * Try to create a simple (non-expandable) list while specifying
   * two types. Expected to fail.
   */
  @Test
  public void testSimpleListMultiTypes() {

    // Schema with a list declared with one type, not expandable

    final TupleMetadata schema = new SchemaBuilder()
        .add("id", MinorType.INT)
        .addList("list")
          .addType(MinorType.VARCHAR)
          .addType(MinorType.INT)
          .resumeSchema()
        .buildSchema();

    try {
      schema.metadata("list").variantSchema().becomeSimple();
      fail();
    } catch (final IllegalStateException e) {
      // expected
    }
  }

  /**
   * Test a variant list created dynamically at load time.
   * The list starts with no type, at which time it can hold
   * only null values. Then we add a Varchar, and finally an
   * Int.
   * <p>
   * This test is superficial. There are many odd cases to consider.
   * <ul>
   * <li>Write nulls to a list with no type. (This test ensures that
   * adding a (nullable) scalar "does the right thing."</li>
   * <li>Add a map to the list. Maps carry no "bits" vector, so null
   * list entries to that point are lost. (For maps, we could go straight
   * to a union, with just a map, to preserve the null states. This whole
   * area is a huge mess...)</li>
   * <li>Do the type transitions when writing to a row. (The tests here
   * do the transition between rows.)</li>
   * </ul>
   *
   * The reason for the sparse coverage is that Drill barely supports lists
   * and unions; most code is just plain broken. Our goal here is not to fix
   * all those problems, just to leave things no more broken than before.
   */
  @Test
  public void testVariantListDynamic() {

    final ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator());
    final RowSetLoader writer = rsLoader.writer();

    // Can write a batch as if this was a repeated Varchar, except
    // that any value can also be null.
    rsLoader.startBatch();

    writer.addColumn(MaterializedField.create("id", Types.required(MinorType.INT)));
    writer.addColumn(MaterializedField.create("list", Types.optional(MinorType.LIST)));

    // Sanity check: should be an array of variants because we said the
    // types within the list are expandable (which is the default.)
    final ArrayWriter arrWriter = writer.array("list");
    assertEquals(ObjectType.VARIANT, arrWriter.entryType());
    final VariantWriter variant = arrWriter.variant();

    // We need to verify that the internal state is what we expect, so
    // the next assertion peeks inside the private bits of the union
    // writer. No client code should ever need to do this, of course.
    assertTrue(((UnionWriterImpl) variant).shim() instanceof EmptyListShim);

    // No types, so all we can do is add a null list, or a list of nulls.
    writer
      .addRow(1, null)
      .addRow(2, variantArray())
      .addRow(3, variantArray(null, null));

    // Add a String. Now we can create a list of strings and/or nulls.
    variant.addMember(MinorType.VARCHAR);
    assertTrue(variant.hasType(MinorType.VARCHAR));

    // Sanity check: sniff inside to ensure that the list contains a single
    // type.
    assertTrue(((UnionWriterImpl) variant).shim() instanceof SimpleListShim);
    assertTrue(((ListWriterImpl) arrWriter).vector().getDataVector() instanceof NullableVarCharVector);

    writer
      .addRow(4, variantArray("fred", null, "barney"));

    // Add an integer. The list vector should be promoted to union.
    // Now we can add both types.
    variant.addMember(MinorType.INT);

    // Sanity check: sniff inside to ensure promotion to union occurred
    assertTrue(((UnionWriterImpl) variant).shim() instanceof UnionVectorShim);
    assertTrue(((ListWriterImpl) arrWriter).vector().getDataVector() instanceof UnionVector);

    writer
      .addRow(5, variantArray("wilma", null, 30));

    // Verify
    final RowSet result = fixture.wrap(rsLoader.harvest());

    final TupleMetadata schema = new SchemaBuilder()
        .add("id", MinorType.INT)
        .addList("list")
          .addType(MinorType.VARCHAR)
          .addType(MinorType.INT)
          .resumeSchema()
        .buildSchema();
    final SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addRow(1, null)
        .addRow(2, variantArray())
        .addRow(3, variantArray(null, null))
        .addRow(4, variantArray("fred", null, "barney"))
        .addRow(5, variantArray("wilma", null, 30))
        .build();

    RowSetUtilities.verify(expected, result);
  }

  /**
   * Dynamically add a map to a list that also contains scalars.
   * Assumes that {@link #testVariantListDynamic()} passed.
   */
  @Test
  public void testVariantListWithMap() {

    final ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator());
    final RowSetLoader writer = rsLoader.writer();

    rsLoader.startBatch();
    writer.addColumn(MaterializedField.create("id", Types.required(MinorType.INT)));
    writer.addColumn(MaterializedField.create("list", Types.optional(MinorType.LIST)));

    final ArrayWriter arrWriter = writer.array("list");
    final VariantWriter variant = arrWriter.variant();

    // Add a null list, or a list of nulls.
    writer
      .addRow(1, null)
      .addRow(2, variantArray())
      .addRow(3, variantArray(null, null));

    // Add a String. Now we can create a list of strings and/or nulls.
    variant.addMember(MinorType.VARCHAR);
    writer
      .addRow(4, variantArray("fred", null, "barney"));

    // Add a map
    final TupleWriter mapWriter = variant.addMember(MinorType.MAP).tuple();
    mapWriter.addColumn(MetadataUtils.newScalar("first", Types.optional(MinorType.VARCHAR)));
    mapWriter.addColumn(MetadataUtils.newScalar("last", Types.optional(MinorType.VARCHAR)));

    // Add a map-based record
    writer
      .addRow(5, variantArray(
          mapValue("wilma", "flintstone"), mapValue("betty", "rubble")));

    // Verify
    final RowSet result = fixture.wrap(rsLoader.harvest());

    final TupleMetadata schema = new SchemaBuilder()
        .add("id", MinorType.INT)
        .addList("list")
          .addType(MinorType.VARCHAR)
          .addMap()
            .addNullable("first", MinorType.VARCHAR)
            .addNullable("last", MinorType.VARCHAR)
            .resumeUnion()
          .resumeSchema()
        .buildSchema();
    final SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addRow(1, null)
        .addRow(2, variantArray())
        .addRow(3, variantArray(null, null))
        .addRow(4, variantArray("fred", null, "barney"))
        .addRow(5, variantArray(
            mapValue("wilma", "flintstone"), mapValue("betty", "rubble")))
        .build();

    RowSetUtilities.verify(expected, result);
  }

  /**
   * The semantics of the ListVector are such that it allows
   * multi-dimensional lists. In this way, it is like a (slightly
   * more normalized) version of the repeated list vector. This form
   * allows arrays to be null.
   * <p>
   * This test verifies that the (non-repeated) list vector can
   * be used to create multi-dimensional arrays in the result set
   * loader layer. However, the rest of Drill does not support this
   * functionality at present, so this test is more of a proof-of-
   * concept than a necessity.
   */
  @Test
  public void testListofListofScalar() {

    // JSON equivalent: {a: [[1, 2], [3, 4]]}
    final ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator());
    final RowSetLoader writer = rsLoader.writer();

    // Can write a batch as if this was a repeated Varchar, except
    // that any value can also be null.
    rsLoader.startBatch();

    writer.addColumn(MaterializedField.create("a", Types.optional(MinorType.LIST)));
    final ArrayWriter outerArray = writer.array("a");
    final VariantWriter outerVariant = outerArray.variant();
    outerVariant.addMember(MinorType.LIST);
    final ArrayWriter innerArray = outerVariant.array();
    final VariantWriter innerVariant = innerArray.variant();
    innerVariant.addMember(MinorType.INT);

    writer.addSingleCol(listValue(listValue(1, 2), listValue(3, 4)));
    final RowSet results = fixture.wrap(rsLoader.harvest());

    // Verify metadata
    final ListVector outer = (ListVector) results.container().getValueVector(0).getValueVector();
    final MajorType outerType = outer.getField().getType();
    assertEquals(1, outerType.getSubTypeCount());
    assertEquals(MinorType.LIST, outerType.getSubType(0));
    assertEquals(1, outer.getField().getChildren().size());

    final ListVector inner = (ListVector) outer.getDataVector();
    assertSame(inner.getField(), outer.getField().getChildren().iterator().next());
    final MajorType innerType = inner.getField().getType();
    assertEquals(1, innerType.getSubTypeCount());
    assertEquals(MinorType.INT, innerType.getSubType(0));
    assertEquals(1, inner.getField().getChildren().size());

    final ValueVector data = inner.getDataVector();
    assertSame(data.getField(), inner.getField().getChildren().iterator().next());
    assertEquals(MinorType.INT, data.getField().getType().getMinorType());
    assertEquals(DataMode.OPTIONAL, data.getField().getType().getMode());
    assertTrue(data instanceof NullableIntVector);

    // Note use of TupleMetadata: BatchSchema can't hold the
    // structure of a list.
    final TupleMetadata expectedSchema = new SchemaBuilder()
        .addList("a")
          .addList()
            .addType(MinorType.INT)
            .resumeUnion()
          .resumeSchema()
        .buildSchema();
    final RowSet expected = new RowSetBuilder(fixture.allocator(), expectedSchema)
        .addSingleCol(listValue(listValue(1, 2), listValue(3, 4)))
        .build();
    RowSetUtilities.verify(expected, results);
  }

  /**
   * The repeated list type is way off in the weeds in Drill. It is not fully
   * supported and the semantics are very murky as a result. It is not clear
   * how such a structure fits into SQL or into an xDBC client. Still, it is
   * part of Drill at present and must be supported in the result set loader.
   * <p>
   * This test models using the repeated list as a 2D array of UNION.
   */
  @Test
  public void testRepeatedListOfUnion() {

    final ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator());
    final RowSetLoader writer = rsLoader.writer();

    // Can write a batch as if this was a repeated Varchar, except
    // that any value can also be null.
    rsLoader.startBatch();

    writer.addColumn(MaterializedField.create("id", Types.required(MinorType.INT)));

    // A union requires a structured column. The only tool to build that a present
    // is the schema builder, so we use that and grab a single column.
    final TupleMetadata dummySchema = new SchemaBuilder()
        .addRepeatedList("list")
          .addArray(MinorType.UNION)
          .resumeSchema()
        .buildSchema();
    writer.addColumn(dummySchema.metadata(0));

    // Sanity check: should be an array of array of variants.
    final ArrayWriter outerArrWriter  = writer.array("list");
    assertEquals(ObjectType.ARRAY, outerArrWriter.entryType());
    final ArrayWriter innerArrWriter = outerArrWriter.array();
    assertEquals(ObjectType.VARIANT, innerArrWriter.entryType());
    final VariantWriter variant = innerArrWriter.variant();

    // No types, so all we can do is add a null list, or a list of nulls.
    writer
      .addRow(1, null)
      .addRow(2, objArray())
      .addRow(3, objArray(null, null))
      .addRow(4, objArray(variantArray(), variantArray()))
      .addRow(5, objArray(variantArray(null, null), variantArray(null, null)));

    // Add a String. Now we can create a list of strings and/or nulls.
    variant.addMember(MinorType.VARCHAR);
    assertTrue(variant.hasType(MinorType.VARCHAR));

    writer
      .addRow(6, objArray(
          variantArray("fred", "wilma", null),
          variantArray("barney", "betty", null)));

    // Add a map
    final TupleWriter mapWriter = variant.addMember(MinorType.MAP).tuple();
    mapWriter.addColumn(MetadataUtils.newScalar("first", Types.optional(MinorType.VARCHAR)));
    mapWriter.addColumn(MetadataUtils.newScalar("last", Types.optional(MinorType.VARCHAR)));

    // Add a map-based record
    writer
      .addRow(7, objArray(
          variantArray(mapValue("fred", "flintstone"), mapValue("wilma", "flintstone")),
          variantArray(mapValue("barney", "rubble"), mapValue("betty", "rubble"))));

    // Verify
    final RowSet result = fixture.wrap(rsLoader.harvest());

    final TupleMetadata schema = new SchemaBuilder()
        .add("id", MinorType.INT)
        .addRepeatedList("list")
          .addList()
            .addType(MinorType.VARCHAR)
            .addMap()
              .addNullable("first", MinorType.VARCHAR)
              .addNullable("last", MinorType.VARCHAR)
              .resumeUnion()
            .resumeRepeatedList()
          .resumeSchema()
        .buildSchema();
    final SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addRow(1, null)
        .addRow(2, objArray())
        .addRow(3, objArray(null, null))
        .addRow(4, objArray(variantArray(), variantArray()))
        .addRow(5, objArray(variantArray(null, null), variantArray(null, null)))
        .addRow(6, objArray(
            variantArray("fred", "wilma", null),
            variantArray("barney", "betty", null)))
        .addRow(7, objArray(
            variantArray(mapValue("fred", "flintstone"), mapValue("wilma", "flintstone")),
            variantArray(mapValue("barney", "rubble"), mapValue("betty", "rubble"))))
        .build();

    RowSetUtilities.verify(expected, result);
  }
}
