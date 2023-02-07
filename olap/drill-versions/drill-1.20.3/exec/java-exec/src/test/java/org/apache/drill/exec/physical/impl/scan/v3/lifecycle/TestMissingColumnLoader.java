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
package org.apache.drill.exec.physical.impl.scan.v3.lifecycle;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.apache.drill.categories.EvfTest;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.physical.resultSet.ResultVectorCache;
import org.apache.drill.exec.physical.resultSet.impl.NullResultVectorCacheImpl;
import org.apache.drill.exec.physical.resultSet.impl.ResultVectorCacheImpl;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.test.SubOperatorTest;
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
@Category(EvfTest.class)
public class TestMissingColumnLoader extends SubOperatorTest {

  /**
   * Test the simplest case: default null type, nothing in the vector
   * cache. Specify no column type, the special NULL type, or a
   * predefined type. Output types should be set accordingly.
   */
  @Test
  public void testBasics() {

    TupleMetadata missingCols = new SchemaBuilder()
        .addDynamic("unspecified")
        .addNullable("specifiedOpt", MinorType.VARCHAR)
        .add("specifiedReq", MinorType.VARCHAR)
        .addArray("specifiedArray", MinorType.VARCHAR)
        .build();

    final ResultVectorCache cache = new NullResultVectorCacheImpl(fixture.allocator());
    StaticBatchBuilder handler = new MissingColumnHandlerBuilder()
        .inputSchema(missingCols)
        .vectorCache(cache)
        .build();
    assertNotNull(handler);

    // Create a batch
    handler.load(2);

    // Verify values and types
    final TupleMetadata expectedSchema = new SchemaBuilder()
        .add("unspecified", MissingColumnHandlerBuilder.DEFAULT_NULL_TYPE)
        .addNullable("specifiedOpt", MinorType.VARCHAR)
        .add("specifiedReq", MinorType.VARCHAR)
        .addArray("specifiedArray", MinorType.VARCHAR)
        .buildSchema();
    final SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(null, null, "", new String[] {})
        .addRow(null, null, "", new String[] {})
        .build();

    RowSetUtilities.verify(expected, fixture.wrap(handler.outputContainer()));
    handler.close();
  }

  @Test
  public void testEmpty() {

    TupleMetadata missingCols = new SchemaBuilder()
        .build();

    final ResultVectorCache cache = new NullResultVectorCacheImpl(fixture.allocator());
    StaticBatchBuilder handler = new MissingColumnHandlerBuilder()
        .inputSchema(missingCols)
        .vectorCache(cache)
        .build();
    assertNull(handler);
  }

  /**
   * Test the ability to use a type other than nullable INT for null
   * columns. This occurs, for example, in the CSV reader where no
   * column is ever INT (nullable or otherwise) and we want our null
   * columns to be (non-nullable) VARCHAR.
   */
  @Test
  public void testCustomNullType() {

    TupleMetadata missingCols = new SchemaBuilder()
        .addDynamic("unspecified")
        .build();

    // Null required is an oxymoron, so is not tested.
    // Null type array does not make sense, so is not tested.

    final ResultVectorCache cache = new NullResultVectorCacheImpl(fixture.allocator());
    final MajorType nullType = MajorType.newBuilder()
        .setMinorType(MinorType.VARCHAR)
        .setMode(DataMode.OPTIONAL)
        .build();
    StaticBatchBuilder handler = new MissingColumnHandlerBuilder()
        .inputSchema(missingCols)
        .vectorCache(cache)
        .nullType(nullType)
        .build();
    assertNotNull(handler);

    // Create a batch
    handler.load(2);

    // Verify values and types

    final TupleMetadata expectedSchema = new SchemaBuilder()
        .add("unspecified", nullType)
        .buildSchema();
    final SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addSingleCol(null)
        .addSingleCol(null)
        .build();

    RowSetUtilities.verify(expected, fixture.wrap(handler.outputContainer()));
    handler.close();
  }

  /**
   * Test the ability to provide a default value for a "null" column.
   * Default values are only allowed for required "null" columns. For
   * nullable columns, NULL is already the default.
   */
  @Test
  public void testDefaultValue() {

    TupleMetadata missingCols = new SchemaBuilder()
        .add("int", MinorType.INT)
        .add("str", MinorType.VARCHAR)
        .add("dub", MinorType.FLOAT8)
        .build();
    missingCols.metadata("int").setDefaultValue("10");
    missingCols.metadata("str").setDefaultValue("foo");
    missingCols.metadata("dub").setDefaultValue("20.0");

    final ResultVectorCache cache = new NullResultVectorCacheImpl(fixture.allocator());
    StaticBatchBuilder handler = new MissingColumnHandlerBuilder()
        .inputSchema(missingCols)
        .vectorCache(cache)
        .build();
    assertNotNull(handler);

    // Create a batch
    handler.load(2);

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

    RowSetUtilities.verify(expected, fixture.wrap(handler.outputContainer()));
    handler.close();
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
  public void testVectorCache() {

    TupleMetadata missingCols = new SchemaBuilder()
        .addNullable("req", MinorType.FLOAT8)
        .addNullable("opt", MinorType.FLOAT8)
        .addArray("rep", MinorType.FLOAT8)
        .addDynamic("unk")
        .build();

    // Populate the cache with a column of each mode.
    final ResultVectorCacheImpl cache = new ResultVectorCacheImpl(fixture.allocator());
    cache.vectorFor(SchemaBuilder.columnSchema("req", MinorType.FLOAT8, DataMode.REQUIRED));
    final ValueVector opt = cache.vectorFor(SchemaBuilder.columnSchema("opt", MinorType.FLOAT8, DataMode.OPTIONAL));
    final ValueVector rep = cache.vectorFor(SchemaBuilder.columnSchema("rep", MinorType.FLOAT8, DataMode.REPEATED));

    // Use nullable Varchar for unknown null columns.
    final MajorType nullType = Types.optional(MinorType.VARCHAR);
    StaticBatchBuilder handler = new MissingColumnHandlerBuilder()
        .inputSchema(missingCols)
        .vectorCache(cache)
        .nullType(nullType)
        .build();
    assertNotNull(handler);

    // Create a batch
    handler.load(2);
    final VectorContainer output = handler.outputContainer();

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
    handler.close();
  }

  /**
   * More extensive schema test.
   */
  @Test
  public void testAllModes() {

    final TupleMetadata missingCols = new SchemaBuilder()
        .add("intReq", MinorType.INT)
        .add("strReq", MinorType.VARCHAR)
        .add("dubReq", MinorType.FLOAT8) // No default
        .addNullable("intOpt", MinorType.INT)
        .addNullable("strOpt", MinorType.VARCHAR)
        .addNullable("dubOpt", MinorType.FLOAT8)
        .buildSchema();
    missingCols.metadata("intReq").setDefaultValue("10");
    missingCols.metadata("strReq").setDefaultValue("foo");
    missingCols.metadata("intOpt").setDefaultValue("20");
    missingCols.metadata("strOpt").setDefaultValue("bar");

    final ResultVectorCache cache = new NullResultVectorCacheImpl(fixture.allocator());
    StaticBatchBuilder handler = new MissingColumnHandlerBuilder()
        .inputSchema(missingCols)
        .vectorCache(cache)
        .nullType(Types.optional(MinorType.VARCHAR))
        .build();
    assertNotNull(handler);

    handler.load(2);

    final SingleRowSet expected = fixture.rowSetBuilder(missingCols)
        .addRow(10, "foo", 0.0D, null, null, null)
        .addRow(10, "foo", 0.0D, null, null, null)
        .build();

    RowSetUtilities.verify(expected, fixture.wrap(handler.outputContainer()));
    handler.close();
  }
}
