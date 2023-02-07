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
package org.apache.drill.exec.physical.impl.scan;

import static org.junit.Assert.assertFalse;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.impl.scan.ScanTestUtils.MockScanBuilder;
import org.apache.drill.exec.physical.impl.scan.project.ReaderSchemaOrchestrator;
import org.apache.drill.exec.physical.impl.scan.project.ScanSchemaOrchestrator;
import org.apache.drill.exec.physical.impl.scan.project.ScanSchemaOrchestrator.ScanOrchestratorBuilder;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.SubOperatorTest;
import org.apache.drill.exec.physical.rowSet.RowSetTestUtils;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test the late-schema support in the scan orchestrator. "Late schema" is the case
 * in which no table schema is provided up front, instead, the reader "discovers"
 * the schema as it reads data. For example, the JSON reader learns the schema
 * only as it sees each column.
 * <p>
 * The tests here focus on the scan orchestrator itself; the tests assume
 * that tests for lower-level components have already passed.
 */

@Category(RowSetTests.class)
public class TestScanOrchestratorLateSchema extends SubOperatorTest {

  /**
   * Test SELECT * from an early-schema table of (a, b)
   */

  @Test
  public void testLateSchemaWildcard() {
    ScanOrchestratorBuilder builder = new MockScanBuilder();

    // SELECT * ...

    builder.projection(RowSetTestUtils.projectAll());
    ScanSchemaOrchestrator orchestrator = new ScanSchemaOrchestrator(fixture.allocator(), builder);

    // ... FROM table

    ReaderSchemaOrchestrator reader = orchestrator.startReader();

    // Create the table loader

    ResultSetLoader loader = reader.makeTableLoader(null);

    // Late schema: no batch provided up front.

    assertFalse(reader.hasSchema());

    // Start a batch and discover a schema: (a, b)

    reader.startBatch();
    RowSetLoader writer = loader.writer();
    writer.addColumn(SchemaBuilder.columnSchema("a", MinorType.INT, DataMode.REQUIRED));
    writer.addColumn(SchemaBuilder.columnSchema("b", MinorType.VARCHAR, DataMode.REQUIRED));

    // Create a batch of data using the discovered schema

    writer
      .addRow(1, "fred")
      .addRow(2, "wilma");
    reader.endBatch();

    // Verify

    TupleMetadata tableSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.VARCHAR)
        .buildSchema();
    SingleRowSet expected = fixture.rowSetBuilder(tableSchema)
        .addRow(1, "fred")
        .addRow(2, "wilma")
        .build();

    new RowSetComparison(expected)
        .verifyAndClearAll(fixture.wrap(orchestrator.output()));

    orchestrator.close();
  }

  /**
   * Test SELECT a, c FROM table(a, b)
   */

  @Test
  public void testLateSchemaSelectDisjoint() {
    ScanOrchestratorBuilder builder = new MockScanBuilder();

    // SELECT a, c ...

    builder.projection(RowSetTestUtils.projectList("a", "c"));
    ScanSchemaOrchestrator orchestrator = new ScanSchemaOrchestrator(fixture.allocator(), builder);

    // ... FROM file

    ReaderSchemaOrchestrator reader = orchestrator.startReader();

    // Create the table loader

    ResultSetLoader loader = reader.makeTableLoader(null);

    // file schema (a, b)

    reader.startBatch();
    RowSetLoader writer = loader.writer();
    writer.addColumn(SchemaBuilder.columnSchema("a", MinorType.INT, DataMode.REQUIRED));
    writer.addColumn(SchemaBuilder.columnSchema("b", MinorType.VARCHAR, DataMode.REQUIRED));

    // Create a batch of data.

    writer
      .addRow(1, "fred")
      .addRow(2, "wilma");
    reader.endBatch();

    // Verify

    TupleMetadata expectedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addNullable("c", MinorType.INT)
        .buildSchema();
    SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(1, null)
        .addRow(2, null)
        .build();

    new RowSetComparison(expected)
        .verifyAndClearAll(fixture.wrap(orchestrator.output()));

    orchestrator.close();
  }

  // TODO: Type persistence across late schema changes
}
