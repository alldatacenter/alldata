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
package org.apache.drill.exec.physical.impl.scan.project;

import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.physical.impl.scan.project.NullColumnBuilder.NullBuilderBuilder;
import org.apache.drill.exec.physical.resultSet.ResultVectorCache;
import org.apache.drill.exec.physical.resultSet.impl.NullResultVectorCacheImpl;
import org.apache.drill.exec.physical.resultSet.impl.ResultVectorCacheImpl;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.test.SubOperatorTest;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test the mechanism that handles all-null columns during projection.
 * An all-null column is one projected in the query, but which does
 * not actually exist in the underlying data source (or input
 * operator.)
 * <p>
 * In anticipation of having type information, this mechanism
 * can create the classic nullable Int null column, or one of
 * any other type and mode.
 */
@Category(RowSetTests.class)
public class TestNullColumnLoader extends SubOperatorTest {

  private ResolvedNullColumn makeNullCol(String name, MajorType nullType) {
    return makeNullCol(name, nullType, null);
  }

  private ResolvedNullColumn makeNullCol(String name) {
    return makeNullCol(name, null, null);
  }

  private ResolvedNullColumn makeNullCol(String name, MajorType nullType, String defaultValue) {

    // For this test, we don't need the projection, so just
    // set it to null.
    return new ResolvedNullColumn(name, nullType, defaultValue, null, 0);
  }

  /**
   * Test the simplest case: default null type, nothing in the vector
   * cache. Specify no column type, the special NULL type, or a
   * predefined type. Output types should be set accordingly.
   */
  @Test
  public void testBasics() {

    final List<ResolvedNullColumn> defns = new ArrayList<>();
    defns.add(makeNullCol("unspecified", null));
    defns.add(makeNullCol("nullType", Types.optional(MinorType.NULL)));
    defns.add(makeNullCol("specifiedOpt", Types.optional(MinorType.VARCHAR)));
    defns.add(makeNullCol("specifiedReq", Types.required(MinorType.VARCHAR)));
    defns.add(makeNullCol("specifiedArray", Types.repeated(MinorType.VARCHAR)));

    final ResultVectorCache cache = new NullResultVectorCacheImpl(fixture.allocator());
    final NullColumnLoader staticLoader = new NullColumnLoader(cache, defns, null, false);

    // Create a batch
    final VectorContainer output = staticLoader.load(2);

    // Verify values and types
    final TupleMetadata expectedSchema = new SchemaBuilder()
        .add("unspecified", NullColumnLoader.DEFAULT_NULL_TYPE)
        .add("nullType", NullColumnLoader.DEFAULT_NULL_TYPE)
        .addNullable("specifiedOpt", MinorType.VARCHAR)
        .addNullable("specifiedReq", MinorType.VARCHAR)
        .addArray("specifiedArray", MinorType.VARCHAR)
        .buildSchema();
    final SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(null, null, null, null, new String[] {})
        .addRow(null, null, null, null, new String[] {})
        .build();

    RowSetUtilities.verify(expected, fixture.wrap(output));
    staticLoader.close();
  }

  /**
   * Test the ability to use a type other than nullable INT for null
   * columns. This occurs, for example, in the CSV reader where no
   * column is ever INT (nullable or otherwise) and we want our null
   * columns to be (non-nullable) VARCHAR.
   */
  @Test
  public void testCustomNullType() {

    final List<ResolvedNullColumn> defns = new ArrayList<>();
    defns.add(makeNullCol("unspecified", null));
    defns.add(makeNullCol("nullType", MajorType.newBuilder()
        .setMinorType(MinorType.NULL)
        .setMode(DataMode.OPTIONAL)
        .build()));

    // Null required is an oxymoron, so is not tested.
    // Null type array does not make sense, so is not tested.
    final ResultVectorCache cache = new NullResultVectorCacheImpl(fixture.allocator());
    final MajorType nullType = MajorType.newBuilder()
        .setMinorType(MinorType.VARCHAR)
        .setMode(DataMode.OPTIONAL)
        .build();
    final NullColumnLoader staticLoader = new NullColumnLoader(cache, defns, nullType, false);

    // Create a batch
    final VectorContainer output = staticLoader.load(2);

    // Verify values and types
    final TupleMetadata expectedSchema = new SchemaBuilder()
        .add("unspecified", nullType)
        .add("nullType", nullType)
        .buildSchema();
    final SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(null, null)
        .addRow(null, null)
        .build();

    RowSetUtilities.verify(expected, fixture.wrap(output));
    staticLoader.close();
  }

  /**
   * Test the ability to provide a default value for a "null" column.
   * Default values are only allowed for required "null" columns. For
   * nullable columns, NULL is already the default.
   */
  @Test
  public void testDefaultValue() {

    final List<ResolvedNullColumn> defns = new ArrayList<>();
    defns.add(makeNullCol("int", Types.required(MinorType.INT), "10"));
    defns.add(makeNullCol("str", Types.required(MinorType.VARCHAR), "foo"));
    defns.add(makeNullCol("dub", Types.required(MinorType.FLOAT8), "20.0"));

    final ResultVectorCache cache = new NullResultVectorCacheImpl(fixture.allocator());
    final MajorType nullType = Types.optional(MinorType.VARCHAR);
    final NullColumnLoader staticLoader = new NullColumnLoader(cache, defns, nullType, false);

    // Create a batch
    final VectorContainer output = staticLoader.load(2);

    // Verify values and types
    final TupleMetadata expectedSchema = new SchemaBuilder()
        .add("int", MinorType.INT)
        .add("str", MinorType.VARCHAR)
        .add("dub", MinorType.FLOAT8)
        .buildSchema();
    final SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(10, "foo", 20.0D)
        .addRow(10, "foo", 20.0D)
        .build();

    RowSetUtilities.verify(expected, fixture.wrap(output));
    staticLoader.close();
  }

  /**
   * Drill requires "schema persistence": if a scan operator
   * reads two files, F1 and F2, then the scan operator must
   * provide the same vectors from both readers. Not just the
   * same types, the same value vector instances (but, of course,
   * populated with different data.)
   * <p>
   * Test the case in which the reader for F1 found columns
   * (a, b, c) but, F2 found only (a, b), requiring that we
   * fill in column c, filled with nulls, but of the same type that it
   * was in file F1. We use a vector cache to pull off this trick.
   * This test ensures that the null column mechanism looks in that
   * vector cache when asked to create a nullable column.
   */
  @Test
  public void testCachedTypesMapToNullable() {

    final List<ResolvedNullColumn> defns = new ArrayList<>();
    defns.add(makeNullCol("req"));
    defns.add(makeNullCol("opt"));
    defns.add(makeNullCol("rep"));
    defns.add(makeNullCol("unk"));

    // Populate the cache with a column of each mode.
    final ResultVectorCacheImpl cache = new ResultVectorCacheImpl(fixture.allocator());
    cache.vectorFor(SchemaBuilder.columnSchema("req", MinorType.FLOAT8, DataMode.REQUIRED));
    final ValueVector opt = cache.vectorFor(SchemaBuilder.columnSchema("opt", MinorType.FLOAT8, DataMode.OPTIONAL));
    final ValueVector rep = cache.vectorFor(SchemaBuilder.columnSchema("rep", MinorType.FLOAT8, DataMode.REPEATED));

    // Use nullable Varchar for unknown null columns.
    final MajorType nullType = Types.optional(MinorType.VARCHAR);
    final NullColumnLoader staticLoader = new NullColumnLoader(cache, defns, nullType, false);

    // Create a batch
    final VectorContainer output = staticLoader.load(2);

    // Verify vectors are reused
    assertSame(opt, output.getValueVector(1).getValueVector());
    assertSame(rep, output.getValueVector(2).getValueVector());

    // Verify values and types
    final TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("req", MinorType.FLOAT8)
        .addNullable("opt", MinorType.FLOAT8)
        .addArray("rep", MinorType.FLOAT8)
        .addNullable("unk", MinorType.VARCHAR)
        .buildSchema();
    final SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(null, null, new int[] { }, null)
        .addRow(null, null, new int[] { }, null)
        .build();

    RowSetUtilities.verify(expected, fixture.wrap(output));
    staticLoader.close();
  }

  /**
   * Suppose, in the previous test, that one of the columns that
   * goes missing is a required column. The null-column mechanism can
   * create the "null" column as a required column, then fill it with
   * empty values (zero or "") -- if the scan operator feels doing so would
   * be helpful.
   */
  @Test
  public void testCachedTypesAllowRequired() {

    final List<ResolvedNullColumn> defns = new ArrayList<>();
    defns.add(makeNullCol("req"));
    defns.add(makeNullCol("opt"));
    defns.add(makeNullCol("rep"));
    defns.add(makeNullCol("unk"));

    // Populate the cache with a column of each mode.
    final ResultVectorCacheImpl cache = new ResultVectorCacheImpl(fixture.allocator());
    cache.vectorFor(SchemaBuilder.columnSchema("req", MinorType.FLOAT8, DataMode.REQUIRED));
    final ValueVector opt = cache.vectorFor(SchemaBuilder.columnSchema("opt", MinorType.FLOAT8, DataMode.OPTIONAL));
    final ValueVector rep = cache.vectorFor(SchemaBuilder.columnSchema("rep", MinorType.FLOAT8, DataMode.REPEATED));

    // Use nullable Varchar for unknown null columns.
    final MajorType nullType = Types.optional(MinorType.VARCHAR);
    final NullColumnLoader staticLoader = new NullColumnLoader(cache, defns, nullType, true);

    // Create a batch
    final VectorContainer output = staticLoader.load(2);

    // Verify vectors are reused
    assertSame(opt, output.getValueVector(1).getValueVector());
    assertSame(rep, output.getValueVector(2).getValueVector());

    // Verify values and types
    final TupleMetadata expectedSchema = new SchemaBuilder()
        .add("req", MinorType.FLOAT8)
        .addNullable("opt", MinorType.FLOAT8)
        .addArray("rep", MinorType.FLOAT8)
        .addNullable("unk", MinorType.VARCHAR)
        .buildSchema();
    final SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(0.0, null, new int[] { }, null)
        .addRow(0.0, null, new int[] { }, null)
        .build();

    RowSetUtilities.verify(expected, fixture.wrap(output));
    staticLoader.close();
  }

  /**
   * Test the shim class that adapts between the null column loader
   * and the projection mechanism. The projection mechanism uses this
   * to pull in the null columns which the null column loader has
   * created.
   */
  @Test
  public void testNullColumnBuilder() {

    final ResultVectorCache cache = new NullResultVectorCacheImpl(fixture.allocator());
    final NullColumnBuilder builder = new NullBuilderBuilder().build();

    builder.add("unspecified");
    builder.add("nullType", Types.optional(MinorType.NULL));
    builder.add("specifiedOpt", Types.optional(MinorType.VARCHAR));
    builder.add("specifiedReq", Types.required(MinorType.VARCHAR));
    builder.add("specifiedArray", Types.repeated(MinorType.VARCHAR));
    builder.build(cache);

    // Create a batch
    builder.load(2);

    // Verify values and types
    final TupleMetadata expectedSchema = new SchemaBuilder()
        .add("unspecified", NullColumnLoader.DEFAULT_NULL_TYPE)
        .add("nullType", NullColumnLoader.DEFAULT_NULL_TYPE)
        .addNullable("specifiedOpt", MinorType.VARCHAR)
        .addNullable("specifiedReq", MinorType.VARCHAR)
        .addArray("specifiedArray", MinorType.VARCHAR)
        .buildSchema();
    final SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(null, null, null, null, new String[] {})
        .addRow(null, null, null, null, new String[] {})
        .build();

    RowSetUtilities.verify(expected, fixture.wrap(builder.output()));
    builder.close();
  }

  /**
   * Test using an output schema, along with a default value property,
   * to define a default value for missing columns.
   */
  @Test
  public void testNullColumnBuilderWithSchema() {

    // Note: upper case names in schema, lower case in "projection" list
    final TupleMetadata outputSchema = new SchemaBuilder()
        .add("IntReq", MinorType.INT)
        .add("StrReq", MinorType.VARCHAR)
        .addNullable("IntOpt", MinorType.INT)
        .addNullable("StrOpt", MinorType.VARCHAR)
        .addNullable("DubOpt", MinorType.FLOAT8) // No default
        .buildSchema();
    outputSchema.metadata("intReq").setDefaultValue("10");
    outputSchema.metadata("strReq").setDefaultValue("foo");
    outputSchema.metadata("intOpt").setDefaultValue("20");
    outputSchema.metadata("strOpt").setDefaultValue("bar");

    final ResultVectorCache cache = new NullResultVectorCacheImpl(fixture.allocator());
    final NullColumnBuilder builder = new NullBuilderBuilder()
        .setNullType(Types.optional(MinorType.VARCHAR))
        .setOutputSchema(outputSchema).build();

    builder.add("strReq");
    builder.add("strOpt");
    builder.add("dubOpt");
    builder.add("intReq");
    builder.add("intOpt");
    builder.add("extra");
    builder.build(cache);

    // Create a batch
    builder.load(2);

    // Verify values and types
    final TupleMetadata expectedSchema = new SchemaBuilder()
        .add("strReq", MinorType.VARCHAR)
        .addNullable("strOpt", MinorType.VARCHAR)
        .addNullable("dubOpt", MinorType.FLOAT8)
        .add("intReq", MinorType.INT)
        .addNullable("intOpt", MinorType.INT)
        .addNullable("extra", MinorType.VARCHAR)
        .buildSchema();
    final SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow("foo", null, null, 10, null, null)
        .addRow("foo", null, null, 10, null, null)
        .build();

    RowSetUtilities.verify(expected, fixture.wrap(builder.output()));
    builder.close();
  }

  /**
   * Test the various conflicts that can occur:
   * <ul>
   * <li>Schema is required, but no default value for null column.</li>
   * <li>Query wants a different type than that in the schema.</li>
   * <li>Query wants a different mode than that in the schema.</li>
   * <ul>
   *
   * The type and mode provided to the builder is that which would result from
   * schema smoothing. The types and modes should usually match, but verify
   * the rules when they don't.
   * <p>
   * Defaults for null columns are ignored: null columns use NULL as the
   * null value.
   */
  @Test
  public void testSchemaWithConflicts() {

    // Note: upper case names in schema, lower case in "projection" list
    final TupleMetadata outputSchema = new SchemaBuilder()
        .add("IntReq", MinorType.INT)
        .add("StrReq", MinorType.VARCHAR) // No default
        .addNullable("IntOpt", MinorType.INT)
        .addNullable("StrOpt", MinorType.VARCHAR)
        .buildSchema();
    outputSchema.metadata("intReq").setDefaultValue("10");
    outputSchema.metadata("intOpt").setDefaultValue("20");
    outputSchema.metadata("strOpt").setDefaultValue("bar");

    final ResultVectorCache cache = new NullResultVectorCacheImpl(fixture.allocator());
    final NullColumnBuilder builder = new NullBuilderBuilder()
        .setNullType(Types.optional(MinorType.VARCHAR))
        .setOutputSchema(outputSchema).build();

    // Defined, required, no default so --> optional
    builder.add("strReq");
    builder.add("strOpt");
    // Defined, has default, but conflicting type, so default --> null, so --> optional
    builder.add("intReq", Types.required(MinorType.BIGINT));
    // Defined, has default, conflicting mode, so keep default
    builder.add("intOpt", Types.required(MinorType.INT));
    builder.build(cache);

    // Create a batch
    builder.load(2);

    // Verify values and types
    final TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("strReq", MinorType.VARCHAR)
        .addNullable("strOpt", MinorType.VARCHAR)
        .addNullable("intReq", MinorType.BIGINT)
        .add("intOpt", MinorType.INT)
        .buildSchema();
    final SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(null, null, null, 20)
        .addRow(null, null, null, 20)
        .build();

    RowSetUtilities.verify(expected, fixture.wrap(builder.output()));
    builder.close();
  }
}
