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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.impl.scan.ScanTestUtils.ScanFixture;
import org.apache.drill.exec.physical.impl.scan.ScanTestUtils.ScanFixtureBuilder;
import org.apache.drill.exec.physical.impl.scan.file.FileScanFramework.FileReaderFactory;
import org.apache.drill.exec.physical.impl.scan.file.FileScanFramework.FileScanBuilder;
import org.apache.drill.exec.physical.impl.scan.file.FileScanFramework.FileSchemaNegotiator;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedReader;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedScanFramework.ScanFrameworkBuilder;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.BatchSchemaBuilder;
import org.apache.drill.exec.record.metadata.ColumnBuilder;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.ColumnExplorer;
import org.apache.drill.exec.store.ColumnExplorer.ImplicitInternalFileColumns;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.store.dfs.easy.FileWork;
import org.apache.drill.test.SubOperatorTest;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests the file metadata extensions to the file operator framework.
 * Focuses on the file metadata itself, assumes that other tests have
 * verified the underlying mechanisms.
 */
@Category(RowSetTests.class)
public class TestFileScanFramework extends SubOperatorTest {

  private static final String MOCK_FILE_NAME = "foo.csv";
  private static final String MOCK_SUFFIX = "csv";
  private static final String MOCK_DIR0 = "x";
  private static final String MOCK_DIR1 = "y";

  private static String pathToFile;
  private static String fqn;
  private static String filePath;
  private static Path rootPath;
  private static String lastModifiedTime;

  @BeforeClass
  public static void copyFile() throws IOException {
    rootPath = new Path(dirTestWatcher.getRootDir().toURI().getPath());
    File file = dirTestWatcher.copyResourceToRoot(
        Paths.get("multilevel", "csv", "1994", "Q1", "orders_94_q1.csv"),
        Paths.get("x/y", MOCK_FILE_NAME));
    filePath = file.toURI().getPath();
    Path filePath = new Path(file.toURI().getPath());
    fqn = ColumnExplorer.ImplicitFileColumns.FQN.getValue(filePath);
    pathToFile = ColumnExplorer.ImplicitFileColumns.FILEPATH.getValue(filePath);
    lastModifiedTime = ColumnExplorer.getImplicitColumnValue(
        ImplicitInternalFileColumns.LAST_MODIFIED_TIME,
        filePath,
        new DrillFileSystem(new Configuration()));
  }

  /**
   * For schema-based testing, we only need the file path from the file work.
   */
  public static class DummyFileWork implements FileWork {

    private final Path path;

    public DummyFileWork(Path path) {
      this.path = path;
    }

    @Override
    public Path getPath() { return path; }

    @Override
    public long getStart() { return 0; }

    @Override
    public long getLength() { return 0; }
  }

  private interface MockFileReader extends ManagedReader<FileSchemaNegotiator> {
    Path filePath();
  }

  /**
   * Mock file reader that returns readers already created for specific
   * test cases. Verifies that the readers match the file splits
   * (which were obtained from the readers.)
   * <p>
   * This is not a good example of a real file reader factory, it does,
   * however, illustrate a design goal to allow a variety of implementations
   * through composition.
   */
  public static class MockFileReaderFactory extends FileReaderFactory {
    public Iterator<MockFileReader> readerIter;

    public MockFileReaderFactory(List<MockFileReader> readers) {
      readerIter = readers.iterator();
    }

    @Override
    public ManagedReader<? extends FileSchemaNegotiator> newReader() {
      MockFileReader reader = readerIter.next();
      assert reader != null;
      return reader;
    }
  }

  public static class FileScanFixtureBuilder extends ScanFixtureBuilder {

    public FileScanBuilder builder = new FileScanBuilder();
    public List<MockFileReader> readers = new ArrayList<>();

    public FileScanFixtureBuilder() {
      super(fixture);
      builder.implicitColumnOptions().setSelectionRoot(rootPath);
      builder.implicitColumnOptions().setPartitionDepth(3);
    }

    @Override
    public ScanFrameworkBuilder builder() { return builder; }

    public void addReader(MockFileReader reader) {
      readers.add(reader);
    }

    @Override
    public ScanFixture build() {

      // Bass-ackward construction of the list of files from
      // a set of text fixture readers. Normal implementations
      // create readers from file splits, not the other way around
      // as is done here.

      List<FileWork> blocks = new ArrayList<>();
      for (MockFileReader reader : readers) {
        blocks.add(new DummyFileWork(reader.filePath()));
      }
      builder.setFileSystemConfig(new Configuration());
      builder.setFiles(blocks);
      builder.setReaderFactory(new MockFileReaderFactory(readers));
      return super.build();
    }
  }

  /**
   * Base class for the "mock" readers used in this test. The mock readers
   * follow the normal (enhanced) reader API, but instead of actually reading
   * from a data source, they just generate data with a known schema.
   * They also expose internal state such as identifying which methods
   * were actually called.
   */
  private static abstract class BaseMockBatchReader implements MockFileReader {
    public boolean openCalled;
    public boolean closeCalled;
    public int startIndex;
    public int batchCount;
    public int batchLimit;
    protected ResultSetLoader tableLoader;
    protected Path filePath = new Path(TestFileScanFramework.filePath);

    @Override
    public Path filePath() { return filePath; }

    protected void makeBatch() {
      RowSetLoader writer = tableLoader.writer();
      int offset = (batchCount - 1) * 20 + startIndex;
      writeRow(writer, offset + 10, "fred");
      writeRow(writer, offset + 20, "wilma");
    }

    protected void writeRow(RowSetLoader writer, int col1, String col2) {
      writer.start();
      if (writer.column(0) != null) {
        writer.scalar(0).setInt(col1);
      }
      if (writer.column(1) != null) {
        writer.scalar(1).setString(col2);
      }
      writer.save();
    }

    @Override
    public void close() {
      closeCalled = true;
    }
  }

  /**
   * "Late schema" reader, meaning that the reader does not know the schema on
   * open, but must "discover" it when reading data.
   */
  private static class MockLateSchemaReader extends BaseMockBatchReader {

    public boolean returnDataOnFirst;

    @Override
    public boolean open(FileSchemaNegotiator schemaNegotiator) {

      // No schema or file, just build the table loader.

      tableLoader = schemaNegotiator.build();
      openCalled = true;
      return true;
    }

    @Override
    public boolean next() {
      batchCount++;
      if (batchCount > batchLimit) {
        return false;
      } else if (batchCount == 1) {

        // On first batch, pretend to discover the schema.

        RowSetLoader rowSet = tableLoader.writer();
        MaterializedField a = SchemaBuilder.columnSchema("a", MinorType.INT, DataMode.REQUIRED);
        rowSet.addColumn(a);
        MaterializedField b = new ColumnBuilder("b", MinorType.VARCHAR)
            .setMode(DataMode.OPTIONAL)
            .setWidth(10)
            .build();
        rowSet.addColumn(b);
        if ( ! returnDataOnFirst) {
          return true;
        }
      }

      makeBatch();
      return true;
    }
  }

  /**
   * Mock reader with an early schema: the schema is known before the first
   * record. Think Parquet or JDBC.
   */
  private static class MockEarlySchemaReader extends BaseMockBatchReader {

    @Override
    public boolean open(FileSchemaNegotiator schemaNegotiator) {
      openCalled = true;
      TupleMetadata schema = new SchemaBuilder()
          .add("a", MinorType.INT)
          .addNullable("b", MinorType.VARCHAR, 10)
          .buildSchema();
      schemaNegotiator.tableSchema(schema, true);
      tableLoader = schemaNegotiator.build();
      return true;
    }

    @Override
    public boolean next() {
      batchCount++;
      if (batchCount > batchLimit) {
        return false;
      }

      makeBatch();
      return true;
    }
  }

  @Test
  public void testLateSchemaFileWildcards() {

    // Create a mock reader, return two batches: one schema-only, another with data.
    MockLateSchemaReader reader = new MockLateSchemaReader();
    reader.batchLimit = 2;
    reader.returnDataOnFirst = false;

    // Create the scan operator
    FileScanFixtureBuilder builder = new FileScanFixtureBuilder();
    builder.projectAllWithAllImplicit(2);
    builder.addReader(reader);
    ScanFixture scanFixture = builder.build();
    ScanOperatorExec scan = scanFixture.scanOp;

    // Standard startup
    assertFalse(reader.openCalled);

    // First batch: build schema. The reader helps: it returns an
    // empty first batch.
    assertTrue(scan.buildSchema());
    assertTrue(reader.openCalled);
    assertEquals(1, reader.batchCount);
    assertEquals(0, scan.batchAccessor().rowCount());

    // Create the expected result.
    TupleMetadata expectedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addNullable("b", MinorType.VARCHAR, 10)
        .add(ScanTestUtils.FULLY_QUALIFIED_NAME_COL, MinorType.VARCHAR)
        .add(ScanTestUtils.FILE_PATH_COL, MinorType.VARCHAR)
        .add(ScanTestUtils.FILE_NAME_COL, MinorType.VARCHAR)
        .add(ScanTestUtils.SUFFIX_COL, MinorType.VARCHAR)
        .add(ScanTestUtils.LAST_MODIFIED_TIME_COL, MinorType.VARCHAR)
        .addNullable(ScanTestUtils.PROJECT_METADATA_COL, MinorType.VARCHAR)
        .addNullable(ScanTestUtils.partitionColName(0), MinorType.VARCHAR)
        .addNullable(ScanTestUtils.partitionColName(1), MinorType.VARCHAR)
        .addNullable(ScanTestUtils.partitionColName(2), MinorType.VARCHAR)
        .buildSchema();
    SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(30, "fred", fqn, pathToFile, MOCK_FILE_NAME, MOCK_SUFFIX, lastModifiedTime, null, MOCK_DIR0, MOCK_DIR1, null)
        .addRow(40, "wilma", fqn, pathToFile, MOCK_FILE_NAME, MOCK_SUFFIX, lastModifiedTime, null, MOCK_DIR0, MOCK_DIR1, null)
        .build();
    assertEquals(expected.batchSchema(), scan.batchAccessor().schema());

    // Next call, return with data.
    assertTrue(scan.next());
    RowSetUtilities.verify(expected,
        fixture.wrap(scan.batchAccessor().container()));

    // EOF
    assertFalse(scan.next());
    assertTrue(reader.closeCalled);
    assertEquals(0, scan.batchAccessor().rowCount());

    scanFixture.close();
  }

  /**
   * Basic sanity test of a couple of implicit columns, along
   * with all table columns in table order. Full testing of implicit
   * columns is done on lower-level components.
   */
  @Test
  public void testMetadataColumns() {

    MockEarlySchemaReader reader = new MockEarlySchemaReader();
    reader.batchLimit = 1;

    // Select table and implicit columns.
    FileScanFixtureBuilder builder = new FileScanFixtureBuilder();
    builder.setProjection("a", "b", "filename", "suffix");
    builder.addReader(reader);
    ScanFixture scanFixture = builder.build();
    ScanOperatorExec scan = scanFixture.scanOp;

    // Expect data and implicit columns
    SchemaBuilder schemaBuilder = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addNullable("b", MinorType.VARCHAR, 10)
        .add("filename", MinorType.VARCHAR)
        .add("suffix", MinorType.VARCHAR);
    BatchSchema expectedSchema = new BatchSchemaBuilder()
        .withSchemaBuilder(schemaBuilder)
        .build();
    SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(10, "fred", MOCK_FILE_NAME, MOCK_SUFFIX)
        .addRow(20, "wilma", MOCK_FILE_NAME, MOCK_SUFFIX)
        .build();

    // Schema should include implicit columns.
    assertTrue(scan.buildSchema());
    assertEquals(expectedSchema, scan.batchAccessor().schema());
    scan.batchAccessor().release();

    // Read one batch, should contain implicit columns
    assertTrue(scan.next());
    RowSetUtilities.verify(expected,
        fixture.wrap(scan.batchAccessor().container()));

    // EOF
    assertFalse(scan.next());
    assertEquals(0, scan.batchAccessor().rowCount());
    scanFixture.close();
  }

  /**
   * Exercise the major project operations: subset of table
   * columns, implicit, partition, missing columns, and output
   * order (and positions) different than table. These cases
   * are more fully test on lower level components; here we verify
   * that the components are wired up correctly.
   */
  @Test
  public void testFullProject() {

    MockEarlySchemaReader reader = new MockEarlySchemaReader();
    reader.batchLimit = 1;

    // Select table and implicit columns.
    FileScanFixtureBuilder builder = new FileScanFixtureBuilder();
    builder.setProjection("dir0", "b", "filename", "c", "suffix");
    builder.addReader(reader);
    ScanFixture scanFixture = builder.build();
    ScanOperatorExec scan = scanFixture.scanOp;

    // Expect data and implicit columns
    SchemaBuilder schemaBuilder = new SchemaBuilder()
        .addNullable("dir0", MinorType.VARCHAR)
        .addNullable("b", MinorType.VARCHAR, 10)
        .add("filename", MinorType.VARCHAR)
        .addNullable("c", MinorType.INT)
        .add("suffix", MinorType.VARCHAR);
    BatchSchema expectedSchema = new BatchSchemaBuilder()
        .withSchemaBuilder(schemaBuilder)
        .build();
    SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(MOCK_DIR0, "fred", MOCK_FILE_NAME, null, MOCK_SUFFIX)
        .addRow(MOCK_DIR0, "wilma", MOCK_FILE_NAME, null, MOCK_SUFFIX)
        .build();

    // Schema should include implicit columns.
    assertTrue(scan.buildSchema());
    assertEquals(expectedSchema, scan.batchAccessor().schema());
    scan.batchAccessor().release();

    // Read one batch, should contain implicit columns
    assertTrue(scan.next());
    RowSetUtilities.verify(expected,
        fixture.wrap(scan.batchAccessor().container()));

    // EOF
    assertFalse(scan.next());
    assertEquals(0, scan.batchAccessor().rowCount());
    scanFixture.close();
  }

  @Test
  public void testEmptyProject() {

    MockEarlySchemaReader reader = new MockEarlySchemaReader();
    reader.batchLimit = 1;

    // Select no columns
    FileScanFixtureBuilder builder = new FileScanFixtureBuilder();
    builder.setProjection();
    builder.addReader(reader);
    ScanFixture scanFixture = builder.build();
    ScanOperatorExec scan = scanFixture.scanOp;

    // Expect data and implicit columns
    BatchSchema expectedSchema = new BatchSchemaBuilder()
        .withSchemaBuilder(new SchemaBuilder())
        .build();
    SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow()
        .addRow()
        .build();

    // Schema should include implicit columns.
    assertTrue(scan.buildSchema());
    assertEquals(expectedSchema, scan.batchAccessor().schema());
    scan.batchAccessor().release();

    // Read one batch, should contain implicit columns
    assertTrue(scan.next());
    RowSetUtilities.verify(expected,
        fixture.wrap(scan.batchAccessor().container()));

    // EOF
    assertFalse(scan.next());
    assertEquals(0, scan.batchAccessor().rowCount());
    scanFixture.close();
  }

  private static class MockMapReader extends BaseMockBatchReader {

    @Override
    public boolean open(FileSchemaNegotiator schemaNegotiator) {
      TupleMetadata schema = new SchemaBuilder()
          .addMap("m1")
            .add("a", MinorType.INT)
            .add("b", MinorType.INT)
            .resumeSchema()
          .buildSchema();
      schemaNegotiator.tableSchema(schema, true);
      tableLoader = schemaNegotiator.build();
      return true;
    }

    @Override
    public boolean next() {
      batchCount++;
      if (batchCount > batchLimit) {
        return false;
      }

      tableLoader.writer()
        .addRow(new Object[] {new Object[] {10, 11}})
        .addRow(new Object[] {new Object[] {20, 21}});
      return true;
    }
  }

  @Test
  public void testMapProject() {
    MockMapReader reader = new MockMapReader();
    reader.batchLimit = 1;

    // Select one of the two map columns
    FileScanFixtureBuilder builder = new FileScanFixtureBuilder();
    builder.setProjection("m1.a");
    builder.addReader(reader);
    ScanFixture scanFixture = builder.build();
    ScanOperatorExec scan = scanFixture.scanOp;

    // Expect data and implicit columns
    SchemaBuilder schemaBuilder = new SchemaBuilder()
        .addMap("m1")
          .add("a", MinorType.INT)
          .resumeSchema();
    BatchSchema expectedSchema = new BatchSchemaBuilder()
        .withSchemaBuilder(schemaBuilder)
        .build();
    SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addSingleCol(new Object[] {10})
        .addSingleCol(new Object[] {20})
        .build();
    assertTrue(scan.buildSchema());
    assertEquals(expectedSchema, scan.batchAccessor().schema());
    scan.batchAccessor().release();

    assertTrue(scan.next());
    RowSetUtilities.verify(expected,
         fixture.wrap(scan.batchAccessor().container()));

    // EOF
    assertFalse(scan.next());
    assertEquals(0, scan.batchAccessor().rowCount());
    scanFixture.close();
  }
}
