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
package org.apache.drill.exec.physical.impl.scan.v3.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.drill.categories.EvfTest;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.physical.impl.scan.ScanOperatorExec;
import org.apache.drill.exec.physical.impl.scan.v3.BaseMockBatchReader;
import org.apache.drill.exec.physical.impl.scan.v3.ScanFixture;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests the implicit column extensions to the file operator framework.
 * Focuses on the file implicit columns, assumes that other tests have
 * verified the underlying mechanisms.
 */
@Category(EvfTest.class)
public class TestFileScan extends BaseFileScanTest {

  /**
   * "Late schema" reader, meaning that the reader does not know the schema on
   * open, but must "discover" it when reading data.
   */
  private static class MockLateSchemaReader extends BaseMockBatchReader {

    public boolean returnDataOnFirst;

    public MockLateSchemaReader(FileSchemaNegotiator negotiator) {

      // Verify the path
      assertEquals(MOCK_FILE_SYSTEM_NAME, negotiator.file().filePath().toString());
      assertEquals(MOCK_FILE_SYSTEM_NAME, negotiator.file().fileWork().getPath().toString());
      assertEquals(MOCK_FILE_SYSTEM_NAME, negotiator.file().split().getPath().toString());

      // No schema or file, just build the table loader.
      tableLoader = negotiator.build();
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
        rowSet.addColumn(MetadataUtils.newScalar("b", Types.optional(MinorType.VARCHAR)));
        if (!returnDataOnFirst) {
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

    public MockEarlySchemaReader(FileSchemaNegotiator schemaNegotiator) {
      TupleMetadata schema = new SchemaBuilder()
          .add("a", MinorType.INT)
          .addNullable("b", MinorType.VARCHAR)
          .buildSchema();
      schemaNegotiator.tableSchema(schema, true);
      tableLoader = schemaNegotiator.build();
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
    ReaderCreator creator = negotiator -> {
      MockLateSchemaReader reader = new MockLateSchemaReader(negotiator);
      reader.batchLimit = 1;
      reader.returnDataOnFirst = true;
      return reader;
    };

    // Create the scan operator
    FileScanFixtureBuilder builder = new FileScanFixtureBuilder();
    builder.projectAllWithImplicit(3);
    builder.addReader(creator);
    ScanFixture scanFixture = builder.build();
    ScanOperatorExec scan = scanFixture.scanOp;

    // First batch: build schema. The reader helps: it returns an
    // empty first batch.
    assertTrue(scan.buildSchema());
    assertEquals(0, scan.batchAccessor().rowCount());

    // Create the expected result.
    TupleMetadata expectedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addNullable("b", MinorType.VARCHAR)
        .add(FileScanUtils.FULLY_QUALIFIED_NAME_COL, MinorType.VARCHAR)
        .add(FileScanUtils.FILE_PATH_COL, MinorType.VARCHAR)
        .add(FileScanUtils.FILE_NAME_COL, MinorType.VARCHAR)
        .add(FileScanUtils.SUFFIX_COL, MinorType.VARCHAR)
        .addNullable(FileScanUtils.partitionColName(0), MinorType.VARCHAR)
        .addNullable(FileScanUtils.partitionColName(1), MinorType.VARCHAR)
        .addNullable(FileScanUtils.partitionColName(2), MinorType.VARCHAR)
        .buildSchema();
    SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(10, "fred", MOCK_FILE_FQN, MOCK_FILE_DIR_PATH, MOCK_FILE_NAME, MOCK_SUFFIX, MOCK_DIR0, MOCK_DIR1, null)
        .addRow(20, "wilma", MOCK_FILE_FQN, MOCK_FILE_DIR_PATH, MOCK_FILE_NAME, MOCK_SUFFIX, MOCK_DIR0, MOCK_DIR1, null)
        .build();
    assertEquals(expected.batchSchema(), scan.batchAccessor().schema());

    // Next call, return with data.
    assertTrue(scan.next());
    RowSetUtilities.verify(expected,
        fixture.wrap(scan.batchAccessor().container()));

    // EOF
    assertFalse(scan.next());
    assertEquals(0, scan.batchAccessor().rowCount());
    scanFixture.close();
  }

  /**
   * Basic sanity test of a couple of implicit columns, along
   * with all table columns in table order. Full testing of implicit
   * columns is done on lower-level components.
   */
  @Test
  public void testImplicitColumns() {

    ReaderCreator creator = negotiator -> {
      MockEarlySchemaReader reader = new MockEarlySchemaReader(negotiator);
      reader.batchLimit = 1;
      return reader;
    };

    // Select table and implicit columns.
    FileScanFixtureBuilder builder = new FileScanFixtureBuilder();
    builder.setProjection("a", "b", "filename", "suffix");
    builder.addReader(creator);
    ScanFixture scanFixture = builder.build();
    ScanOperatorExec scan = scanFixture.scanOp;

    // Expect data and implicit columns
    TupleMetadata expectedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addNullable("b", MinorType.VARCHAR)
        .add("filename", MinorType.VARCHAR)
        .add("suffix", MinorType.VARCHAR)
        .build();
    SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(10, "fred", MOCK_FILE_NAME, MOCK_SUFFIX)
        .addRow(20, "wilma", MOCK_FILE_NAME, MOCK_SUFFIX)
        .build();

    // Schema should include implicit columns.
    assertTrue(scan.buildSchema());
    assertEquals(expected.container().getSchema(), scan.batchAccessor().schema());
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

    ReaderCreator creator = negotiator -> {
      MockEarlySchemaReader reader = new MockEarlySchemaReader(negotiator);
      reader.batchLimit = 1;
      return reader;
    };

    // Select table and implicit columns.
    FileScanFixtureBuilder builder = new FileScanFixtureBuilder();
    builder.setProjection("dir0", "b", "filename", "c", "suffix");
    builder.addReader(creator);
    ScanFixture scanFixture = builder.build();
    ScanOperatorExec scan = scanFixture.scanOp;

    // Expect data and implicit columns
    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("dir0", MinorType.VARCHAR)
        .addNullable("b", MinorType.VARCHAR)
        .add("filename", MinorType.VARCHAR)
        .addNullable("c", MinorType.INT)
        .add("suffix", MinorType.VARCHAR)
        .build();
    SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(MOCK_DIR0, "fred", MOCK_FILE_NAME, null, MOCK_SUFFIX)
        .addRow(MOCK_DIR0, "wilma", MOCK_FILE_NAME, null, MOCK_SUFFIX)
        .build();

    // Schema should include implicit columns.
    assertTrue(scan.buildSchema());
    assertEquals(expected.container().getSchema(), scan.batchAccessor().schema());
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

    ReaderCreator creator = negotiator -> {
      MockEarlySchemaReader reader = new MockEarlySchemaReader(negotiator);
      reader.batchLimit = 1;
      return reader;
    };

    // Select no columns
    FileScanFixtureBuilder builder = new FileScanFixtureBuilder();
    builder.setProjection();
    builder.addReader(creator);
    ScanFixture scanFixture = builder.build();
    ScanOperatorExec scan = scanFixture.scanOp;

    // Expect data and implicit columns
    TupleMetadata expectedSchema = new SchemaBuilder()
        .build();
    SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow()
        .addRow()
        .build();

    // Schema should include implicit columns.
    assertTrue(scan.buildSchema());
    assertEquals(expected.container().getSchema(), scan.batchAccessor().schema());
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

    public MockMapReader(FileSchemaNegotiator schemaNegotiator) {
      TupleMetadata schema = new SchemaBuilder()
          .addMap("m1")
            .add("a", MinorType.INT)
            .add("b", MinorType.INT)
            .resumeSchema()
          .buildSchema();
      schemaNegotiator.tableSchema(schema, true);
      tableLoader = schemaNegotiator.build();
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

    ReaderCreator creator = negotiator -> {
      MockMapReader reader = new MockMapReader(negotiator);
      reader.batchLimit = 1;
      return reader;
    };

    // Select one of the two map columns
    FileScanFixtureBuilder builder = new FileScanFixtureBuilder();
    builder.setProjection("m1.a");
    builder.addReader(creator);
    ScanFixture scanFixture = builder.build();
    ScanOperatorExec scan = scanFixture.scanOp;

    // Expect data and implicit columns
    TupleMetadata expectedSchema = new SchemaBuilder()
        .addMap("m1")
          .add("a", MinorType.INT)
          .resumeSchema()
        .build();
    SingleRowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addSingleCol(new Object[] {10})
        .addSingleCol(new Object[] {20})
        .build();
    assertTrue(scan.buildSchema());
    assertEquals(expected.container().getSchema(), scan.batchAccessor().schema());
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
