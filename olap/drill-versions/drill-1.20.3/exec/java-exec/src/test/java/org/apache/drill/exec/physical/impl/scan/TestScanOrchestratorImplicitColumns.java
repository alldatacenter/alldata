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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.impl.protocol.SchemaTracker;
import org.apache.drill.exec.physical.impl.scan.ScanTestUtils.MockScanBuilder;
import org.apache.drill.exec.physical.impl.scan.file.ImplicitColumnManager;
import org.apache.drill.exec.physical.impl.scan.file.ImplicitColumnManager.ImplicitColumnOptions;
import org.apache.drill.exec.physical.impl.scan.project.ReaderSchemaOrchestrator;
import org.apache.drill.exec.physical.impl.scan.project.ScanSchemaOrchestrator;
import org.apache.drill.exec.physical.impl.scan.project.ScanSchemaOrchestrator.ScanOrchestratorBuilder;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.ColumnExplorer;
import org.apache.drill.exec.store.ColumnExplorer.ImplicitFileColumns;
import org.apache.drill.exec.store.ColumnExplorer.ImplicitInternalFileColumns;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.test.SubOperatorTest;
import org.apache.drill.exec.physical.rowSet.RowSetTestUtils;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

/**
 * Tests the scan orchestrator's ability to merge table schemas
 * with implicit file columns provided by the file metadata manager.
 */

@Category(RowSetTests.class)
public class TestScanOrchestratorImplicitColumns extends SubOperatorTest {

  private ImplicitColumnOptions standardOptions(Path filePath) {
    return standardOptions(Lists.newArrayList(filePath));
  }

  private ImplicitColumnOptions standardOptions(List<Path> files) {
    ImplicitColumnOptions options = new ImplicitColumnOptions();
    options.useLegacyWildcardExpansion(false); // Don't expand partition columns for wildcard
    options.setSelectionRoot(new Path(dirTestWatcher.getRootDir().toURI().getPath()));
    options.setFiles(files);
    return options;
  }

  /**
   * Resolve a selection list using SELECT *.
   */

  @Test
  public void testWildcardWithMetadata() throws IOException {
    File file = dirTestWatcher.copyResourceToRoot(
        Paths.get("multilevel", "csv", "1994", "Q1", "orders_94_q1.csv"),
        Paths.get("x", "y", "z.csv"));
    Path filePath = new Path(file.toURI().getPath());

    DrillFileSystem fileSystem = new DrillFileSystem(new Configuration());
    ImplicitColumnManager metadataManager = new ImplicitColumnManager(
        fixture.getOptionManager(),
        standardOptions(filePath),
        fileSystem);

    ScanOrchestratorBuilder builder = new MockScanBuilder();
    builder.withImplicitColumns(metadataManager);

    // SELECT *, filename, suffix ...

    builder.projection(RowSetTestUtils.projectList(
        SchemaPath.DYNAMIC_STAR,
        ScanTestUtils.FULLY_QUALIFIED_NAME_COL,
        ScanTestUtils.FILE_PATH_COL,
        ScanTestUtils.FILE_NAME_COL,
        ScanTestUtils.SUFFIX_COL,
        ScanTestUtils.LAST_MODIFIED_TIME_COL,
        ScanTestUtils.PROJECT_METADATA_COL,
        ScanTestUtils.partitionColName(0),
        ScanTestUtils.partitionColName(1)));
    ScanSchemaOrchestrator scanner = new ScanSchemaOrchestrator(fixture.allocator(), builder);

    // ... FROM file

    metadataManager.startFile(filePath);
    ReaderSchemaOrchestrator reader = scanner.startReader();

    // file schema (a, b)

    TupleMetadata tableSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.VARCHAR)
        .buildSchema();
    ResultSetLoader loader = reader.makeTableLoader(tableSchema);

    // Create a batch of data.

    reader.startBatch();
    loader.writer()
      .addRow(1, "fred")
      .addRow(2, "wilma");
    reader.endBatch();

    // Verify

    TupleMetadata expectedSchema = ScanTestUtils.expandImplicit(tableSchema, metadataManager, 2);

    String fqn = ImplicitFileColumns.FQN.getValue(filePath);
    String filePathValue = ImplicitFileColumns.FILEPATH.getValue(filePath);
    String fileName = ImplicitFileColumns.FILENAME.getValue(filePath);
    String suffix = ImplicitFileColumns.SUFFIX.getValue(filePath);
    String lastModifiedTime = ColumnExplorer.getImplicitColumnValue(ImplicitInternalFileColumns.LAST_MODIFIED_TIME, filePath, fileSystem);
    String projectMetadata = ColumnExplorer.getImplicitColumnValue(ImplicitInternalFileColumns.USE_METADATA, filePath, fileSystem);

    SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(1, "fred", fqn, filePathValue, fileName, suffix, lastModifiedTime, projectMetadata, "x", "y")
        .addRow(2, "wilma", fqn, filePathValue, fileName, suffix, lastModifiedTime, projectMetadata, "x", "y")
        .build();

    RowSetUtilities.verify(expected,
        fixture.wrap(scanner.output()));

    scanner.close();
  }

  /**
   * Test SELECT c FROM table(a, b)
   * The result set will be one null column for each record, but
   * no file data.
   */

  @Test
  public void testSelectNone() {
    ScanOrchestratorBuilder builder = new MockScanBuilder();
    File file = dirTestWatcher.copyResourceToRoot(
        Paths.get("multilevel", "csv", "1994", "Q1", "orders_94_q1.csv"),
        Paths.get("x", "y", "z.csv"));
    Path filePath = new Path(file.toURI().getPath());
    ImplicitColumnManager metadataManager = new ImplicitColumnManager(
        fixture.getOptionManager(),
        standardOptions(filePath));
    builder.withImplicitColumns(metadataManager);

    // SELECT c ...

    builder.projection(RowSetTestUtils.projectList("c"));
    ScanSchemaOrchestrator scanner = new ScanSchemaOrchestrator(fixture.allocator(), builder);

    // ... FROM file

    metadataManager.startFile(filePath);
    ReaderSchemaOrchestrator reader = scanner.startReader();

    // file schema (a, b)

    TupleMetadata tableSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.VARCHAR)
        .buildSchema();

    // Create the table loader

    ResultSetLoader loader = reader.makeTableLoader(tableSchema);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("c", MinorType.INT)
        .buildSchema();

    // Create a batch of data.

    reader.startBatch();
    loader.writer()
      .addRow(1, "fred")
      .addRow(2, "wilma");
    reader.endBatch();

    // Verify

    SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
      .addSingleCol(null)
      .addSingleCol(null)
      .build();

    RowSetUtilities.verify(expected,
        fixture.wrap(scanner.output()));

    scanner.close();
  }

  /**
   * Test SELECT a, b, dir0, suffix FROM table(a, b)
   * dir0, suffix are file metadata columns
   */

  @Test
  public void testEarlySchemaSelectAllAndMetadata() {

    // Null columns of type VARCHAR

    MajorType nullType = MajorType.newBuilder()
        .setMinorType(MinorType.VARCHAR)
        .setMode(DataMode.OPTIONAL)
        .build();

    ScanOrchestratorBuilder builder = new MockScanBuilder();
    builder.nullType(nullType);
    File file = dirTestWatcher.copyResourceToRoot(
        Paths.get("multilevel", "csv", "1994", "Q1", "orders_94_q1.csv"),
        Paths.get("x", "y", "z.csv"));
    Path filePath = new Path(file.toURI().getPath());
    ImplicitColumnManager metadataManager = new ImplicitColumnManager(
        fixture.getOptionManager(),
        standardOptions(filePath));
    builder.withImplicitColumns(metadataManager);

    // SELECT a, b, dir0, suffix ...

    builder.projection(RowSetTestUtils.projectList("a", "b", "dir0", "suffix"));
    ScanSchemaOrchestrator scanner = new ScanSchemaOrchestrator(fixture.allocator(), builder);

    // ... FROM file

    metadataManager.startFile(filePath);
    ReaderSchemaOrchestrator reader = scanner.startReader();

    // file schema (a, b)

    TupleMetadata tableSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.VARCHAR)
        .buildSchema();

    // Create the table loader

    ResultSetLoader loader = reader.makeTableLoader(tableSchema);

    // Verify empty batch.

    reader.defineSchema();
    TupleMetadata expectedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.VARCHAR)
        .addNullable("dir0", MinorType.VARCHAR)
        .add("suffix", MinorType.VARCHAR)
        .buildSchema();
    {
      SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
         .build();

      assertNotNull(scanner.output());
      RowSetUtilities.verify(expected,
          fixture.wrap(scanner.output()));
    }

    // Create a batch of data.

    reader.startBatch();
    loader.writer()
      .addRow(1, "fred")
      .addRow(2, "wilma");
    reader.endBatch();

    // Verify

    {
      SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(1, "fred", "x", "csv")
        .addRow(2, "wilma", "x", "csv")
        .build();

      RowSetUtilities.verify(expected,
          fixture.wrap(scanner.output()));
    }

    scanner.close();
  }

  /**
   * Test SELECT dir0, b, suffix, c FROM table(a, b)
   * Full combination of metadata, table and null columns
   */

  @Test
  public void testMixture() {
    ScanOrchestratorBuilder builder = new MockScanBuilder();
    File file = dirTestWatcher.copyResourceToRoot(
        Paths.get("multilevel", "csv", "1994", "Q1", "orders_94_q1.csv"),
        Paths.get("x", "y", "z.csv"));
    Path filePath = new Path(file.toURI().getPath());
    ImplicitColumnManager metadataManager = new ImplicitColumnManager(
        fixture.getOptionManager(),
        standardOptions(filePath));
    builder.withImplicitColumns(metadataManager);

    // SELECT dir0, b, suffix, c ...

    builder.projection(RowSetTestUtils.projectList("dir0", "b", "suffix", "c"));
    ScanSchemaOrchestrator scanner = new ScanSchemaOrchestrator(fixture.allocator(), builder);

    // ... FROM file

    metadataManager.startFile(filePath);
    ReaderSchemaOrchestrator reader = scanner.startReader();

    // file schema (a, b)

    TupleMetadata tableSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.VARCHAR)
        .buildSchema();

    // Create the table loader

    ResultSetLoader loader = reader.makeTableLoader(tableSchema);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("dir0", MinorType.VARCHAR)
        .add("b", MinorType.VARCHAR)
        .add("suffix", MinorType.VARCHAR)
        .addNullable("c", MinorType.INT)
        .buildSchema();

    // Create a batch of data.

    reader.startBatch();
    loader.writer()
      .addRow(1, "fred")
      .addRow(2, "wilma");
    reader.endBatch();

    // Verify

    SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
      .addRow("x", "fred", "csv", null)
      .addRow("x", "wilma", "csv", null)
      .build();

    RowSetUtilities.verify(expected,
        fixture.wrap(scanner.output()));

    scanner.close();
  }

  /**
   * Verify that metadata columns follow distinct files
   * <br>
   * SELECT dir0, filename, b FROM (a.csv, b.csv)
   */

  @Test
  public void testMetadataMulti() {
    ScanOrchestratorBuilder builder = new MockScanBuilder();
    File file = dirTestWatcher.copyResourceToRoot(
        Paths.get("multilevel", "csv", "1994", "Q1", "orders_94_q1.csv"),
        Paths.get("x", "y", "a.csv"));
    Path filePathA = new Path(file.toURI().getPath());
    File file2 = dirTestWatcher.copyResourceToRoot(
        Paths.get("multilevel", "csv", "1994", "Q2", "orders_94_q2.csv"),
        Paths.get("x", "b.csv"));
    Path filePathB = new Path(file2.toURI().getPath());
    ImplicitColumnManager metadataManager = new ImplicitColumnManager(
        fixture.getOptionManager(),
        standardOptions(Lists.newArrayList(filePathA, filePathB)));
    builder.withImplicitColumns(metadataManager);

    // SELECT dir0, dir1, filename, b ...

    builder.projection(RowSetTestUtils.projectList(
        ScanTestUtils.partitionColName(0),
        ScanTestUtils.partitionColName(1),
        ScanTestUtils.FILE_NAME_COL,
        "b"));
    ScanSchemaOrchestrator scanner = new ScanSchemaOrchestrator(fixture.allocator(), builder);

    // file schema (a, b)

    TupleMetadata tableSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addNullable("b", MinorType.VARCHAR, 10)
        .buildSchema();
    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable(ScanTestUtils.partitionColName(0), MinorType.VARCHAR)
        .addNullable(ScanTestUtils.partitionColName(1), MinorType.VARCHAR)
        .add(ScanTestUtils.FILE_NAME_COL, MinorType.VARCHAR)
        .addNullable("b", MinorType.VARCHAR, 10)
        .buildSchema();

    SchemaTracker tracker = new SchemaTracker();
    int schemaVersion;
    {
      // ... FROM file a.csv

      metadataManager.startFile(filePathA);

      ReaderSchemaOrchestrator reader = scanner.startReader();
      ResultSetLoader loader = reader.makeTableLoader(tableSchema);

      reader.startBatch();
      loader.writer()
        .addRow(10, "fred")
        .addRow(20, "wilma");
      reader.endBatch();

      tracker.trackSchema(scanner.output());
      schemaVersion = tracker.schemaVersion();

      SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
          .addRow("x", "y", "a.csv", "fred")
          .addRow("x", "y", "a.csv", "wilma")
          .build();
      RowSetUtilities.verify(expected,
          fixture.wrap(scanner.output()));

      // Do explicit close (as in real code) to avoid an implicit
      // close which will blow away the current file info...

      scanner.closeReader();
    }
    {
      // ... FROM file b.csv

      metadataManager.startFile(filePathB);
      ReaderSchemaOrchestrator reader = scanner.startReader();
      ResultSetLoader loader = reader.makeTableLoader(tableSchema);

      reader.startBatch();
      loader.writer()
          .addRow(30, "bambam")
          .addRow(40, "betty");
      reader.endBatch();

      tracker.trackSchema(scanner.output());
      assertEquals(schemaVersion, tracker.schemaVersion());

      SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
          .addRow("x", null, "b.csv", "bambam")
          .addRow("x", null, "b.csv", "betty")
          .build();
      RowSetUtilities.verify(expected,
          fixture.wrap(scanner.output()));

      scanner.closeReader();
    }

    scanner.close();
  }
}
