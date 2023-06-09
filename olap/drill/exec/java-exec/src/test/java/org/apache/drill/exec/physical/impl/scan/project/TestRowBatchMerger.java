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

import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.physical.impl.scan.project.NullColumnBuilder.NullBuilderBuilder;
import org.apache.drill.exec.physical.impl.scan.project.ResolvedTuple.ResolvedRow;
import org.apache.drill.exec.physical.resultSet.ResultVectorCache;
import org.apache.drill.exec.physical.resultSet.impl.NullResultVectorCacheImpl;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.test.SubOperatorTest;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.netty.buffer.DrillBuf;

import static org.apache.drill.test.rowSet.RowSetUtilities.mapValue;
import static org.apache.drill.test.rowSet.RowSetUtilities.singleMap;

import org.apache.drill.categories.RowSetTests;

import static org.apache.drill.test.rowSet.RowSetUtilities.mapArray;


/**
 * Test the row batch merger by merging two batches. Tests both the
 * "direct" and "exchange" cases. Direct means that the output container
 * contains the source vector directly: they are the same vectors.
 * Exchange means we have two vectors, but we swap the underlying
 * Drillbufs to effectively shift data from source to destination
 * vector.
 */

@Category(RowSetTests.class)
public class TestRowBatchMerger extends SubOperatorTest {

  public static class RowSetSource implements VectorSource {

    private SingleRowSet rowSet;

    public RowSetSource(SingleRowSet rowSet) {
      this.rowSet = rowSet;
    }

    public RowSet rowSet() { return rowSet; }

    public void clear() {
      rowSet.clear();
    }

    @Override
    public ValueVector vector(int index) {
      return rowSet.container().getValueVector(index).getValueVector();
    }
  }

  public static final int SLAB_SIZE = 16 * 1024 * 1024;

  @BeforeClass
  public static void setup() {
    // Party on 10 16MB blocks of memory to detect vector issues
    DrillBuf bufs[] = new DrillBuf[10];
    for (int i = 0; i < bufs.length; i++) {
      bufs[i] = fixture.allocator().buffer(SLAB_SIZE);
      for (int j = 0; j < SLAB_SIZE / 4; j++) {
        bufs[i].setInt(j * 4, 0xDEADBEEF);
      }
    }
    for (int i = 0; i < bufs.length; i++) {
      bufs[i].release();
    }
  }

  private RowSetSource makeFirst() {
    TupleMetadata firstSchema = new SchemaBuilder()
        .add("d", MinorType.VARCHAR)
        .add("a", MinorType.INT)
        .buildSchema();
    return new RowSetSource(
        fixture.rowSetBuilder(firstSchema)
          .addRow("barney", 10)
          .addRow("wilma", 20)
          .build());
  }

  private RowSetSource makeSecond() {
    TupleMetadata secondSchema = new SchemaBuilder()
        .add("b", MinorType.INT)
        .add("c", MinorType.VARCHAR)
        .buildSchema();
    return new RowSetSource(
        fixture.rowSetBuilder(secondSchema)
          .addRow(1, "foo.csv")
          .addRow(2, "foo.csv")
          .build());
  }

  public static class TestProjection extends ResolvedColumn {

    public TestProjection(VectorSource source, int sourceIndex) {
      super(source, sourceIndex);
    }

    @Override
    public String name() { return null; }

    @Override
    public MaterializedField schema() { return null; }
  }

  @Test
  public void testSimpleFlat() {

    // Create the first batch

    RowSetSource first = makeFirst();

    // Create the second batch

    RowSetSource second = makeSecond();

    ResolvedRow resolvedTuple = new ResolvedRow(null);
    resolvedTuple.add(new TestProjection(first, 1));
    resolvedTuple.add(new TestProjection(second, 0));
    resolvedTuple.add(new TestProjection(second, 1));
    resolvedTuple.add(new TestProjection(first, 0));

    // Do the merge

    VectorContainer output = new VectorContainer(fixture.allocator());
    resolvedTuple.project(null, output);
    output.setRecordCount(first.rowSet().rowCount());
    RowSet result = fixture.wrap(output);

    // Verify

    TupleMetadata expectedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.INT)
        .add("c", MinorType.VARCHAR)
        .add("d", MinorType.VARCHAR)
        .buildSchema();
    SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(10, 1, "foo.csv", "barney")
        .addRow(20, 2, "foo.csv", "wilma")
        .build();

    new RowSetComparison(expected)
      .verifyAndClearAll(result);
  }

  @Test
  public void testImplicitFlat() {

    // Create the first batch

    RowSetSource first = makeFirst();

    // Create the second batch

    RowSetSource second = makeSecond();

    ResolvedRow resolvedTuple = new ResolvedRow(null);
    resolvedTuple.add(new TestProjection(resolvedTuple, 1));
    resolvedTuple.add(new TestProjection(second, 0));
    resolvedTuple.add(new TestProjection(second, 1));
    resolvedTuple.add(new TestProjection(resolvedTuple, 0));

    // Do the merge

    VectorContainer output = new VectorContainer(fixture.allocator());
    resolvedTuple.project(first.rowSet().container(), output);
    output.setRecordCount(first.rowSet().rowCount());
    RowSet result = fixture.wrap(output);

    // Verify

    TupleMetadata expectedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.INT)
        .add("c", MinorType.VARCHAR)
        .add("d", MinorType.VARCHAR)
        .buildSchema();
    SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(10, 1, "foo.csv", "barney")
        .addRow(20, 2, "foo.csv", "wilma")
        .build();

    new RowSetComparison(expected)
      .verifyAndClearAll(result);
  }

  @Test
  public void testFlatWithNulls() {

    // Create the first batch

    RowSetSource first = makeFirst();

    // Create null columns

    NullColumnBuilder builder = new NullBuilderBuilder().build();

    ResolvedRow resolvedTuple = new ResolvedRow(builder);
    resolvedTuple.add(new TestProjection(resolvedTuple, 1));
    resolvedTuple.add(resolvedTuple.nullBuilder().add("null1"));
    resolvedTuple.add(resolvedTuple.nullBuilder().add("null2", Types.optional(MinorType.VARCHAR)));
    resolvedTuple.add(new TestProjection(resolvedTuple, 0));

    // Build the null values

    ResultVectorCache cache = new NullResultVectorCacheImpl(fixture.allocator());
    builder.build(cache);
    builder.load(first.rowSet().rowCount());

    // Do the merge

    VectorContainer output = new VectorContainer(fixture.allocator());
    resolvedTuple.project(first.rowSet().container(), output);
    output.setRecordCount(first.rowSet().rowCount());
    RowSet result = fixture.wrap(output);

    // Verify

    TupleMetadata expectedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addNullable("null1", MinorType.INT)
        .addNullable("null2", MinorType.VARCHAR)
        .add("d", MinorType.VARCHAR)
        .buildSchema();
    SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(10, null, null, "barney")
        .addRow(20, null, null, "wilma")
        .build();

    new RowSetComparison(expected)
      .verifyAndClearAll(result);
    builder.close();
  }

  /**
   * Test the ability to create maps from whole cloth if requested in
   * the projection list, and the map is not available from the data
   * source.
   */

  @Test
  public void testNullMaps() {

    // Create the first batch

    RowSetSource first = makeFirst();

    // Create null columns

    NullColumnBuilder builder = new NullBuilderBuilder().build();
    ResolvedRow resolvedTuple = new ResolvedRow(builder);
    resolvedTuple.add(new TestProjection(resolvedTuple, 1));

    ResolvedMapColumn nullMapCol = new ResolvedMapColumn(resolvedTuple, "map1");
    ResolvedTuple nullMap = nullMapCol.members();
    nullMap.add(nullMap.nullBuilder().add("null1"));
    nullMap.add(nullMap.nullBuilder().add("null2", Types.optional(MinorType.VARCHAR)));

    ResolvedMapColumn nullMapCol2 = new ResolvedMapColumn(nullMap, "map2");
    ResolvedTuple nullMap2 = nullMapCol2.members();
    nullMap2.add(nullMap2.nullBuilder().add("null3"));
    nullMap.add(nullMapCol2);

    resolvedTuple.add(nullMapCol);
    resolvedTuple.add(new TestProjection(resolvedTuple, 0));

    // Build the null values

    ResultVectorCache cache = new NullResultVectorCacheImpl(fixture.allocator());
    resolvedTuple.buildNulls(cache);

    // LoadNulls

    resolvedTuple.loadNulls(first.rowSet().rowCount());

    // Do the merge

    VectorContainer output = new VectorContainer(fixture.allocator());
    resolvedTuple.project(first.rowSet().container(), output);
    resolvedTuple.setRowCount(first.rowSet().rowCount());
    RowSet result = fixture.wrap(output);

    // Verify

    TupleMetadata expectedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("map1")
          .addNullable("null1", MinorType.INT)
          .addNullable("null2", MinorType.VARCHAR)
          .addMap("map2")
            .addNullable("null3", MinorType.INT)
            .resumeMap()
          .resumeSchema()
        .add("d", MinorType.VARCHAR)
        .buildSchema();
    SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(10, mapValue(null, null, singleMap(null)), "barney")
        .addRow(20, mapValue(null, null, singleMap(null)), "wilma")
        .build();

    new RowSetComparison(expected)
      .verifyAndClearAll(result);
    resolvedTuple.close();
  }

  /**
   * Test that the merger mechanism can rewrite a map to include
   * projected null columns.
   */

  @Test
  public void testMapRevision() {

    // Create the first batch

    TupleMetadata inputSchema = new SchemaBuilder()
        .add("b", MinorType.VARCHAR)
        .addMap("a")
          .add("c", MinorType.INT)
          .resumeSchema()
        .buildSchema();
    RowSetSource input = new RowSetSource(
        fixture.rowSetBuilder(inputSchema)
          .addRow("barney", singleMap(10))
          .addRow("wilma", singleMap(20))
          .build());

    // Create mappings

    NullColumnBuilder builder = new NullBuilderBuilder().build();
    ResolvedRow resolvedTuple = new ResolvedRow(builder);

    resolvedTuple.add(new TestProjection(resolvedTuple, 0));
    ResolvedMapColumn mapCol = new ResolvedMapColumn(resolvedTuple,
        inputSchema.column(1), 1);
    resolvedTuple.add(mapCol);
    ResolvedTuple map = mapCol.members();
    map.add(new TestProjection(map, 0));
    map.add(map.nullBuilder().add("null1"));

    // Build the null values

    ResultVectorCache cache = new NullResultVectorCacheImpl(fixture.allocator());
    resolvedTuple.buildNulls(cache);

    // LoadNulls

    resolvedTuple.loadNulls(input.rowSet().rowCount());

    // Do the merge

    VectorContainer output = new VectorContainer(fixture.allocator());
    resolvedTuple.project(input.rowSet().container(), output);
    output.setRecordCount(input.rowSet().rowCount());
    RowSet result = fixture.wrap(output);

    // Verify

    TupleMetadata expectedSchema = new SchemaBuilder()
        .add("b", MinorType.VARCHAR)
        .addMap("a")
          .add("c", MinorType.INT)
          .addNullable("null1", MinorType.INT)
          .resumeSchema()
        .buildSchema();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow("barney", mapValue(10, null))
        .addRow("wilma", mapValue(20, null))
        .build();

    new RowSetComparison(expected)
      .verifyAndClearAll(result);
  }

  /**
   * Test that the merger mechanism can rewrite a map array to include
   * projected null columns.
   */

  @Test
  public void testMapArrayRevision() {

    // Create the first batch

    TupleMetadata inputSchema = new SchemaBuilder()
        .add("b", MinorType.VARCHAR)
        .addMapArray("a")
          .add("c", MinorType.INT)
          .resumeSchema()
        .buildSchema();
    RowSetSource input = new RowSetSource(
        fixture.rowSetBuilder(inputSchema)
          .addRow("barney", mapArray(singleMap(10), singleMap(11), singleMap(12)))
          .addRow("wilma", mapArray(singleMap(20), singleMap(21)))
          .build());

    // Create mappings

    NullColumnBuilder builder = new NullBuilderBuilder().build();
    ResolvedRow resolvedTuple = new ResolvedRow(builder);

    resolvedTuple.add(new TestProjection(resolvedTuple, 0));
    ResolvedMapColumn mapCol = new ResolvedMapColumn(resolvedTuple,
        inputSchema.column(1), 1);
    resolvedTuple.add(mapCol);
    ResolvedTuple map = mapCol.members();
    map.add(new TestProjection(map, 0));
    map.add(map.nullBuilder().add("null1"));

    // Build the null values

    ResultVectorCache cache = new NullResultVectorCacheImpl(fixture.allocator());
    resolvedTuple.buildNulls(cache);

    // LoadNulls

    resolvedTuple.loadNulls(input.rowSet().rowCount());

    // Do the merge

    VectorContainer output = new VectorContainer(fixture.allocator());
    resolvedTuple.project(input.rowSet().container(), output);
    output.setRecordCount(input.rowSet().rowCount());
    RowSet result = fixture.wrap(output);

    // Verify

    TupleMetadata expectedSchema = new SchemaBuilder()
        .add("b", MinorType.VARCHAR)
        .addMapArray("a")
          .add("c", MinorType.INT)
          .addNullable("null1", MinorType.INT)
          .resumeSchema()
        .buildSchema();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow("barney", mapArray(
            mapValue(10, null), mapValue(11, null), mapValue(12, null)))
        .addRow("wilma", mapArray(
            mapValue(20, null), mapValue(21, null)))
        .build();

    new RowSetComparison(expected)
      .verifyAndClearAll(result);
  }

}
