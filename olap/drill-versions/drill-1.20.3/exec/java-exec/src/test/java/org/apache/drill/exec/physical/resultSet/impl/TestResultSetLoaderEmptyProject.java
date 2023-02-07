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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.impl.ResultSetLoaderImpl.ResultSetOptions;
import org.apache.drill.exec.physical.resultSet.project.Projections;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.test.SubOperatorTest;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(RowSetTests.class)
public class TestResultSetLoaderEmptyProject extends SubOperatorTest {

  /**
   * Verify that empty projection works: allows skipping rows and
   * reporting those rows as a batch with no vectors but with the
   * desired row count.
   */
  @Test
  public void testEmptyTopSchema() {
    List<SchemaPath> selection = Lists.newArrayList();
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.INT)
        .buildSchema();
    ResultSetOptions options = new ResultSetOptionBuilder()
        .projection(Projections.parse(selection))
        .readerSchema(schema)
        .build();
    ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);

    assertTrue(rsLoader.isProjectionEmpty());

    // Can't skip rows if batch not started.
    int rowCount = 100_000;
    try {
      rsLoader.skipRows(10);
      fail();
    } catch (IllegalStateException e) {
      // Expected
    }

    // Loop to skip 100,000 rows. Should occur in two batches.
    rsLoader.startBatch();
    int skipped = rsLoader.skipRows(rowCount);
    assertEquals(skipped, ValueVector.MAX_ROW_COUNT);

    VectorContainer output = rsLoader.harvest();
    assertEquals(skipped, output.getRecordCount());
    assertEquals(0, output.getNumberOfColumns());
    output.zeroVectors();

    // Second batch

    rowCount -= skipped;
    rsLoader.startBatch();
    skipped = rsLoader.skipRows(rowCount);
    assertEquals(skipped, rowCount);

    output = rsLoader.harvest();
    assertEquals(skipped, output.getRecordCount());
    assertEquals(0, output.getNumberOfColumns());
    output.zeroVectors();

    rsLoader.close();
  }

  /**
   * Verify that a disjoint schema (projection does not overlap with
   * table schema) is treated the same as an empty projection.
   */
  @Test
  public void testDisjointSchema() {
    List<SchemaPath> selection = Lists.newArrayList(
        SchemaPath.getSimplePath("a"),
        SchemaPath.getSimplePath("b"));
    TupleMetadata schema = new SchemaBuilder()
        .add("c", MinorType.INT)
        .add("d", MinorType.INT)
        .buildSchema();
    ResultSetOptions options = new ResultSetOptionBuilder()
        .projection(Projections.parse(selection))
        .readerSchema(schema)
        .build();
    ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);

    assertTrue(rsLoader.isProjectionEmpty());
    rsLoader.close();
  }

  /**
   * Verify that skip rows works even if the the projection is non-empty.
   */
  @Test
  public void testNonEmptySchema() {
    List<SchemaPath> selection = Lists.newArrayList(
        SchemaPath.getSimplePath("a"),
        SchemaPath.getSimplePath("b"));
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.INT)
        .buildSchema();
    ResultSetOptions options = new ResultSetOptionBuilder()
        .projection(Projections.parse(selection))
        .readerSchema(schema)
        .build();
    ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);

    assertFalse(rsLoader.isProjectionEmpty());

    // Skip 10 rows. Columns are of required types, so are filled
    // with zeros.
    rsLoader.startBatch();
    int rowCount = 10;
    rsLoader.skipRows(rowCount);

    // Verify
    RowSetBuilder builder = fixture.rowSetBuilder(schema);
    for (int i = 0; i < rowCount; i++) {
      builder.addRow(0, 0);
    }
    RowSetUtilities.verify(builder.build(), fixture.wrap(rsLoader.harvest()));

    rsLoader.close();
  }

  @Test
  public void testEmptyMapProjection() {
    List<SchemaPath> selection = Lists.newArrayList();
    TupleMetadata schema = new SchemaBuilder()
        .addMap("map")
          .add("a", MinorType.INT)
          .add("b", MinorType.INT)
          .resumeSchema()
        .buildSchema();
    ResultSetOptions options = new ResultSetOptionBuilder()
        .projection(Projections.parse(selection))
        .readerSchema(schema)
        .build();
    ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);

    assertTrue(rsLoader.isProjectionEmpty());

    // Sanity test to verify row skipping with maps
    int rowCount = 5000;
    rsLoader.startBatch();
    int skipped = rsLoader.skipRows(rowCount);
    assertEquals(skipped, rowCount);

    VectorContainer output = rsLoader.harvest();
    assertEquals(rowCount, output.getRecordCount());
    assertEquals(0, output.getNumberOfColumns());
    output.zeroVectors();

    rsLoader.close();
  }

  /**
   * Test disjoint projection, but with maps. Project top-level columns
   * a, b, when those columns actually appear in a map which is not
   * projected.
   */
  @Test
  public void testDisjointMapProjection() {
    List<SchemaPath> selection = Lists.newArrayList(
        SchemaPath.getSimplePath("a"),
        SchemaPath.getSimplePath("b"));
    TupleMetadata schema = new SchemaBuilder()
        .addMap("map")
          .add("a", MinorType.INT)
          .add("b", MinorType.INT)
          .resumeSchema()
        .buildSchema();
    ResultSetOptions options = new ResultSetOptionBuilder()
        .projection(Projections.parse(selection))
        .readerSchema(schema)
        .build();
    ResultSetLoader rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);

    assertTrue(rsLoader.isProjectionEmpty());

    rsLoader.close();
  }
}
