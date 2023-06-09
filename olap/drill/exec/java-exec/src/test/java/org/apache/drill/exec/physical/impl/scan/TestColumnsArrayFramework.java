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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.impl.scan.ScanTestUtils.ScanFixture;
import org.apache.drill.exec.physical.impl.scan.ScanTestUtils.ScanFixtureBuilder;
import org.apache.drill.exec.physical.impl.scan.TestFileScanFramework.DummyFileWork;
import org.apache.drill.exec.physical.impl.scan.columns.ColumnsScanFramework;
import org.apache.drill.exec.physical.impl.scan.columns.ColumnsScanFramework.ColumnsScanBuilder;
import org.apache.drill.exec.physical.impl.scan.columns.ColumnsSchemaNegotiator;
import org.apache.drill.exec.physical.impl.scan.file.FileScanFramework.FileReaderFactory;
import org.apache.drill.exec.physical.impl.scan.file.FileScanFramework.FileSchemaNegotiator;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedReader;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedScanFramework.ScanFrameworkBuilder;
import org.apache.drill.exec.physical.rowSet.RowSetTestUtils;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.dfs.easy.FileWork;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.test.SubOperatorTest;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * Test the columns-array specific behavior in the columns scan framework.
 */
@Category(RowSetTests.class)
public class TestColumnsArrayFramework extends SubOperatorTest {

  private static final Path MOCK_FILE_PATH = new Path("file:/w/x/y/z.csv");

  public static class MockFileReaderFactory extends FileReaderFactory {
    public Iterator<DummyColumnsReader> readerIter;

    public MockFileReaderFactory(List<DummyColumnsReader> readers) {
      readerIter = readers.iterator();
    }

    @Override
    public ManagedReader<? extends FileSchemaNegotiator> newReader() {
      DummyColumnsReader reader = readerIter.next();
      assert reader != null;
      return reader;
    }
  }

  public static class ColumnsScanFixtureBuilder extends ScanFixtureBuilder {

    public ColumnsScanBuilder builder = new ColumnsScanBuilder();
    public List<DummyColumnsReader> readers = new ArrayList<>();

    public ColumnsScanFixtureBuilder() {
      super(fixture);
    }

    @Override
    public ScanFrameworkBuilder builder() { return builder; }

    public void addReader(DummyColumnsReader reader) {
      readers.add(reader);
    }

    @Override
    public ScanFixture build() {

      // Bass-ackward construction of the list of files from
      // a set of text fixture readers. Normal implementations
      // create readers from file splits, not the other way around
      // as is done here.

      List<FileWork> blocks = new ArrayList<>();
      for (DummyColumnsReader reader : readers) {
        blocks.add(new DummyFileWork(reader.filePath()));
      }
      builder.setFileSystemConfig(new Configuration());
      builder.setFiles(blocks);
      builder.setReaderFactory(new MockFileReaderFactory(readers));
      return super.build();
    }
  }

  public static class DummyColumnsReader implements ManagedReader<ColumnsSchemaNegotiator> {

    public TupleMetadata schema;
    public ColumnsSchemaNegotiator negotiator;
    int nextCount;
    protected Path filePath = MOCK_FILE_PATH;

    public DummyColumnsReader(TupleMetadata schema) {
      this.schema = schema;
    }

    public Path filePath() { return filePath; }

    @Override
    public boolean open(ColumnsSchemaNegotiator negotiator) {
      this.negotiator = negotiator;
      negotiator.tableSchema(schema, true);
      negotiator.build();
      return true;
    }

    @Override
    public boolean next() {
      return nextCount++ == 0;
    }

    @Override
    public void close() { }
  }

  /**
   * Test including a column other than "columns". Occurs when
   * using implicit columns.
   */
  @Test
  public void testNonColumnsProjection() {

    // Create a mock reader, return two batches: one schema-only, another with data.

    DummyColumnsReader reader = new DummyColumnsReader(
        new SchemaBuilder()
          .add("a", MinorType.VARCHAR)
          .buildSchema());

    // Create the scan operator

    ColumnsScanFixtureBuilder builder = new ColumnsScanFixtureBuilder();
    builder.projectAll();
    builder.addReader(reader);
    ScanFixture scanFixture = builder.build();
    ScanOperatorExec scan = scanFixture.scanOp;

    // Start the one and only reader, and check the columns
    // schema info.

    assertTrue(scan.buildSchema());
    assertNotNull(reader.negotiator);
    assertFalse(reader.negotiator.columnsArrayProjected());
    assertNull(reader.negotiator.projectedIndexes());

    scanFixture.close();
  }

  /**
   * Test projecting just the `columns` column.
   */
  @Test
  public void testColumnsProjection() {

    // Create a mock reader, return two batches: one schema-only, another with data.

    DummyColumnsReader reader = new DummyColumnsReader(
        new SchemaBuilder()
        .addArray(ColumnsScanFramework.COLUMNS_COL, MinorType.VARCHAR)
        .buildSchema());

    // Create the scan operator

    ColumnsScanFixtureBuilder builder = new ColumnsScanFixtureBuilder();
    builder.setProjection(RowSetTestUtils.projectList(ColumnsScanFramework.COLUMNS_COL));
    builder.addReader(reader);
    builder.builder.requireColumnsArray(true);
    ScanFixture scanFixture = builder.build();
    ScanOperatorExec scan = scanFixture.scanOp;

    // Start the one and only reader, and check the columns
    // schema info.

    assertTrue(scan.buildSchema());
    assertNotNull(reader.negotiator);
    assertTrue(reader.negotiator.columnsArrayProjected());
    assertNull(reader.negotiator.projectedIndexes());

    scanFixture.close();
  }

  /**
   * Test including a specific index of `columns` such as
   * `columns`[1].
   */
  @Test
  public void testColumnsIndexProjection() {

    // Create a mock reader, return two batches: one schema-only, another with data.

    DummyColumnsReader reader = new DummyColumnsReader(
        new SchemaBuilder()
        .addArray(ColumnsScanFramework.COLUMNS_COL, MinorType.VARCHAR)
        .buildSchema());

    // Create the scan operator

    ColumnsScanFixtureBuilder builder = new ColumnsScanFixtureBuilder();
    builder.setProjection(Lists.newArrayList(
        SchemaPath.parseFromString(ColumnsScanFramework.COLUMNS_COL + "[1]"),
        SchemaPath.parseFromString(ColumnsScanFramework.COLUMNS_COL + "[3]")));
    builder.addReader(reader);
    builder.builder.requireColumnsArray(true);
    ScanFixture scanFixture = builder.build();
    ScanOperatorExec scan = scanFixture.scanOp;

    // Start the one and only reader, and check the columns
    // schema info.

    assertTrue(scan.buildSchema());
    assertNotNull(reader.negotiator);
    assertTrue(reader.negotiator.columnsArrayProjected());
    boolean projIndexes[] = reader.negotiator.projectedIndexes();
    assertNotNull(projIndexes);
    assertEquals(4, projIndexes.length);
    assertFalse(projIndexes[0]);
    assertTrue(projIndexes[1]);
    assertFalse(projIndexes[2]);
    assertTrue(projIndexes[3]);

    scanFixture.close();
  }
}
