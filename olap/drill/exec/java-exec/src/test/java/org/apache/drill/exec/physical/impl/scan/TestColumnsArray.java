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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.impl.scan.ScanTestUtils.MockScanBuilder;
import org.apache.drill.exec.physical.impl.scan.columns.ColumnsArrayManager;
import org.apache.drill.exec.physical.impl.scan.columns.ColumnsScanFramework.ColumnsScanBuilder;
import org.apache.drill.exec.physical.impl.scan.columns.ColumnsScanFramework;
import org.apache.drill.exec.physical.impl.scan.file.ImplicitColumnManager;
import org.apache.drill.exec.physical.impl.scan.file.ImplicitColumnManager.ImplicitColumnOptions;
import org.apache.drill.exec.physical.impl.scan.project.ReaderSchemaOrchestrator;
import org.apache.drill.exec.physical.impl.scan.project.ScanSchemaOrchestrator;
import org.apache.drill.exec.physical.impl.scan.project.ScanSchemaOrchestrator.ScanOrchestratorBuilder;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.test.SubOperatorTest;
import org.apache.drill.exec.physical.rowSet.RowSetTestUtils;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.apache.hadoop.fs.Path;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test the "columns" array mechanism integrated with the scan schema
 * orchestrator including simulating reading data.
 */
@Category(RowSetTests.class)
public class TestColumnsArray extends SubOperatorTest {

  private static class MockScanner {
    ScanSchemaOrchestrator scanner;
    ReaderSchemaOrchestrator reader;
    ResultSetLoader loader;
  }

  private ImplicitColumnOptions standardOptions(Path filePath) {
    ImplicitColumnOptions options = new ImplicitColumnOptions();
    options.useLegacyWildcardExpansion(false); // Don't expand partition columns for wildcard
    options.setSelectionRoot(new Path("hdfs:///w"));
    options.setFiles(Lists.newArrayList(filePath));
    return options;
  }

  private MockScanner buildScanner(List<SchemaPath> projList) {

    MockScanner mock = new MockScanner();

    // Set up the file metadata manager

    Path filePath = new Path("hdfs:///w/x/y/z.csv");
    ImplicitColumnManager metadataManager = new ImplicitColumnManager(
        fixture.getOptionManager(),
        standardOptions(filePath));

    // ...and the columns array manager

    ColumnsArrayManager colsManager = new ColumnsArrayManager(false);

    // Configure the schema orchestrator

    ScanOrchestratorBuilder builder = new MockScanBuilder();
    builder.withImplicitColumns(metadataManager);
    builder.addParser(colsManager.projectionParser());
    builder.addResolver(colsManager.resolver());

    // SELECT <proj list> ...

    builder.projection(projList);
    mock.scanner = new ScanSchemaOrchestrator(fixture.allocator(), builder);

    // FROM z.csv

    metadataManager.startFile(filePath);
    mock.reader = mock.scanner.startReader();

    // Table schema (columns: VARCHAR[])

    TupleMetadata tableSchema = new SchemaBuilder()
        .addArray(ColumnsScanFramework.COLUMNS_COL, MinorType.VARCHAR)
        .buildSchema();

    mock.loader = mock.reader.makeTableLoader(tableSchema);

    // First empty batch

    mock.reader.defineSchema();
    return mock;
  }

  /**
   * Test columns array. The table must be able to support it by having a
   * matching column.
   */

  @Test
  public void testColumnsArray() {

    MockScanner mock = buildScanner(RowSetTestUtils.projectList(ScanTestUtils.FILE_NAME_COL,
        ColumnsScanFramework.COLUMNS_COL,
        ScanTestUtils.partitionColName(0)));

    // Verify empty batch.

    TupleMetadata expectedSchema = new SchemaBuilder()
        .add("filename", MinorType.VARCHAR)
        .addArray("columns", MinorType.VARCHAR)
        .addNullable("dir0", MinorType.VARCHAR)
        .buildSchema();
    {
      SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
         .build();

      assertNotNull(mock.scanner.output());
      RowSetUtilities.verify(expected,
         fixture.wrap(mock.scanner.output()));
    }

    // Create a batch of data.

    mock.reader.startBatch();
    mock.loader.writer()
      .addRow(new Object[] {new String[] {"fred", "flintstone"}})
      .addRow(new Object[] {new String[] {"barney", "rubble"}});
    mock. reader.endBatch();

    // Verify

    {
      SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow("z.csv", new String[] {"fred", "flintstone"}, "x")
        .addRow("z.csv", new String[] {"barney", "rubble"}, "x")
        .build();

      RowSetUtilities.verify(expected,
          fixture.wrap(mock.scanner.output()));
    }

    mock.scanner.close();
  }

  @Test
  public void testWildcard() {

    MockScanner mock = buildScanner(RowSetTestUtils.projectAll());

    // Verify empty batch.

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("columns", MinorType.VARCHAR)
        .buildSchema();
    {
      SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
         .build();

      assertNotNull(mock.scanner.output());
      RowSetUtilities.verify(expected,
         fixture.wrap(mock.scanner.output()));
    }

    // Create a batch of data.

    mock.reader.startBatch();
    mock.loader.writer()
      .addRow(new Object[] {new String[] {"fred", "flintstone"}})
      .addRow(new Object[] {new String[] {"barney", "rubble"}});
    mock. reader.endBatch();

    // Verify

    {
      SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addSingleCol(new String[] {"fred", "flintstone"})
        .addSingleCol(new String[] {"barney", "rubble"})
        .build();

      RowSetUtilities.verify(expected,
          fixture.wrap(mock.scanner.output()));
    }

    mock.scanner.close();
  }

  @Test
  public void testWildcardAndFileMetadata() {

    MockScanner mock = buildScanner(RowSetTestUtils.projectList(
        ScanTestUtils.FILE_NAME_COL,
        SchemaPath.DYNAMIC_STAR,
        ScanTestUtils.partitionColName(0)));

    // Verify empty batch.

    TupleMetadata expectedSchema = new SchemaBuilder()
        .add("filename", MinorType.VARCHAR)
        .addArray("columns", MinorType.VARCHAR)
        .addNullable("dir0", MinorType.VARCHAR)
        .buildSchema();
    {
      SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
         .build();

      assertNotNull(mock.scanner.output());
      RowSetUtilities.verify(expected,
         fixture.wrap(mock.scanner.output()));
    }

    // Create a batch of data.

    mock.reader.startBatch();
    mock.loader.writer()
      .addRow(new Object[] {new String[] {"fred", "flintstone"}})
      .addRow(new Object[] {new String[] {"barney", "rubble"}});
    mock. reader.endBatch();

    // Verify

    {
      SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow("z.csv", new String[] {"fred", "flintstone"}, "x")
        .addRow("z.csv", new String[] {"barney", "rubble"}, "x")
        .build();

      RowSetUtilities.verify(expected,
          fixture.wrap(mock.scanner.output()));
    }

    mock.scanner.close();
  }

  private ScanSchemaOrchestrator buildScan(boolean requireColumns, List<SchemaPath> cols) {

    // Set up the columns array manager

    ColumnsArrayManager colsManager = new ColumnsArrayManager(requireColumns);

    // Configure the schema orchestrator

    ScanOrchestratorBuilder builder = new ColumnsScanBuilder();
    builder.addParser(colsManager.projectionParser());
    builder.addResolver(colsManager.resolver());
    builder.projection(cols);
    return new ScanSchemaOrchestrator(fixture.allocator(), builder);
  }

  /**
   * Test attempting to use the columns array with an early schema with
   * column types not compatible with a varchar array.
   */

  @Test
  public void testMissingColumnsColumn() {
    ScanSchemaOrchestrator scanner = buildScan(true,
        RowSetTestUtils.projectList(ColumnsScanFramework.COLUMNS_COL));

    TupleMetadata tableSchema = new SchemaBuilder()
        .add("a", MinorType.VARCHAR)
        .buildSchema();

    try {
      ReaderSchemaOrchestrator reader = scanner.startReader();
      reader.makeTableLoader(tableSchema);
      reader.defineSchema();
      fail();
    } catch (IllegalStateException e) {
      // Expected
    }

    scanner.close();
  }

  @Test
  public void testNotRepeated() {
    ScanSchemaOrchestrator scanner = buildScan(true,
        RowSetTestUtils.projectList(ColumnsScanFramework.COLUMNS_COL));

    TupleMetadata tableSchema = new SchemaBuilder()
        .add(ColumnsScanFramework.COLUMNS_COL, MinorType.VARCHAR)
        .buildSchema();

    try {
      ReaderSchemaOrchestrator reader = scanner.startReader();
      reader.makeTableLoader(tableSchema);
      reader.defineSchema();
      fail();
    } catch (IllegalStateException e) {
      // Expected
    }

    scanner.close();
  }

  /**
   * Verify that if the columns column is not required, that `columns`
   * is treated like any other column.
   */
  @Test
  public void testReqularCol() {
    ScanSchemaOrchestrator scanner = buildScan(false,
        RowSetTestUtils.projectList(ColumnsScanFramework.COLUMNS_COL));

    TupleMetadata tableSchema = new SchemaBuilder()
        .add(ColumnsScanFramework.COLUMNS_COL, MinorType.VARCHAR)
        .buildSchema();

    ReaderSchemaOrchestrator reader = scanner.startReader();
    ResultSetLoader rsLoader = reader.makeTableLoader(tableSchema);
    reader.defineSchema();

    reader.startBatch();
    rsLoader.writer()
      .addRow("fred");
    reader.endBatch();

    SingleRowSet expected = fixture.rowSetBuilder(tableSchema)
      .addRow("fred")
      .build();

    RowSetUtilities.verify(expected,
        fixture.wrap(scanner.output()));
    scanner.close();
  }
}
