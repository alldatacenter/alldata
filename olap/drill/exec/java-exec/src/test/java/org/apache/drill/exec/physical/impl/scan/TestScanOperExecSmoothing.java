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

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.impl.scan.ScanTestUtils.ScanFixture;
import org.apache.drill.exec.physical.impl.scan.framework.SchemaNegotiator;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test the ability of the scan operator to "absorb" schema changes by
 * "smoothing" out data types and modes across readers. Schema smoothing
 * is helpful when there is no output schema, but only works within the
 * context of a single scan operator: it cannot help when a query has
 * multiple scans, each in its own fragment.
 */
@Category(RowSetTests.class)
public class TestScanOperExecSmoothing extends BaseScanOperatorExecTest {

  private static class MockEarlySchemaReader2 extends MockEarlySchemaReader {

    @Override
    public boolean open(SchemaNegotiator schemaNegotiator) {
      openCalled = true;
      TupleMetadata schema = new SchemaBuilder()
          .add("a", MinorType.VARCHAR)
          .addNullable("b", MinorType.VARCHAR, 10)
          .buildSchema();
      schemaNegotiator.tableSchema(schema, true);
      schemaNegotiator.build();
      tableLoader = schemaNegotiator.build();
      return true;
    }

    @Override
    protected void writeRow(RowSetLoader writer, int col1, String col2) {
      writer.start();
      if (writer.column(0) != null) {
        writer.scalar(0).setString(Integer.toString(col1));
      }
      if (writer.column(1) != null) {
        writer.scalar(1).setString(col2);
      }
      writer.save();
    }
  }

  private static class MockOneColEarlySchemaReader extends BaseMockBatchReader {

    @Override
    public boolean open(SchemaNegotiator schemaNegotiator) {
      openCalled = true;
      TupleMetadata schema = new SchemaBuilder()
          .add("a", MinorType.INT)
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

    @Override
    protected void writeRow(RowSetLoader writer, int col1, String col2) {
      writer.start();
      if (writer.column(0) != null) {
        writer.scalar(0).setInt(col1 + 1);
      }
      writer.save();
    }
  }

  /**
   * Multiple readers with a schema change between them.
   */

  @Test
  public void testSchemaChange() {
    MockEarlySchemaReader reader1 = new MockEarlySchemaReader();
    reader1.batchLimit = 2;
    MockEarlySchemaReader reader2 = new MockEarlySchemaReader2();
    reader2.batchLimit = 2;

    ScanFixture scanFixture = simpleFixture(reader1, reader2);
    ScanOperatorExec scan = scanFixture.scanOp;

    // Build schema

    assertTrue(scan.buildSchema());
    assertEquals(1, scan.batchAccessor().schemaVersion());
    scan.batchAccessor().release();

    readSchemaChangeBatches(scanFixture, reader2);
  }

  @Test
  public void testSchemaChangeNoSchemaBatch() {
    MockEarlySchemaReader reader1 = new MockEarlySchemaReader();
    reader1.batchLimit = 2;
    MockEarlySchemaReader reader2 = new MockEarlySchemaReader2();
    reader2.batchLimit = 2;

    BaseScanFixtureBuilder builder = simpleBuilder(reader1, reader2);
    builder.enableSchemaBatch = false;
    ScanFixture scanFixture = builder.build();

    readSchemaChangeBatches(scanFixture, reader2);
  }

  private void readSchemaChangeBatches(ScanFixture scanFixture, MockEarlySchemaReader reader2) {
    ScanOperatorExec scan = scanFixture.scanOp;

    // First batch

    assertTrue(scan.next());
    assertEquals(1, scan.batchAccessor().schemaVersion());
    scan.batchAccessor().release();

    // Second batch

    assertTrue(scan.next());
    assertEquals(1, scan.batchAccessor().schemaVersion());
    scan.batchAccessor().release();

    // Second reader.

    TupleMetadata expectedSchema2 = new SchemaBuilder()
        .add("a", MinorType.VARCHAR)
        .addNullable("b", MinorType.VARCHAR, 10)
        .buildSchema();

    assertTrue(scan.next());
    assertEquals(2, scan.batchAccessor().schemaVersion());
    SingleRowSet expected = fixture.rowSetBuilder(expectedSchema2)
        .addRow("10", "fred")
        .addRow("20", "wilma")
        .build();
    RowSetUtilities.verify(expected,
      fixture.wrap(scan.batchAccessor().container()));

    // Second batch from second reader.

    assertTrue(scan.next());
    assertEquals(2, scan.batchAccessor().schemaVersion());
    expected = fixture.rowSetBuilder(expectedSchema2)
        .addRow("30", "fred")
        .addRow("40", "wilma")
        .build();
    RowSetUtilities.verify(expected,
      fixture.wrap(scan.batchAccessor().container()));

    // EOF

    assertFalse(scan.next());
    assertTrue(reader2.closeCalled);
    assertEquals(0, scan.batchAccessor().rowCount());

    scanFixture.close();
  }

 /**
   * Test the ability of the scan operator to "smooth" out schema changes
   * by reusing the type from a previous reader, if known. That is,
   * given three readers:<br>
   * (a, b)<br>
   * (b)<br>
   * (a, b)<br>
   * Then the type of column a should be preserved for the second reader that
   * does not include a. This works if a is nullable. If so, a's type will
   * be used for the empty column, rather than the usual nullable int.
   * <p>
   * Full testing of smoothing is done in
   * {#link TestScanProjector}. Here we just make sure that the
   * smoothing logic is available via the scan operator.
   */

  @Test
  public void testSchemaSmoothing() {

    // Reader returns (a, b)
    MockEarlySchemaReader reader1 = new MockEarlySchemaReader();
    reader1.batchLimit = 1;

    // Reader returns (a)
    MockOneColEarlySchemaReader reader2 = new MockOneColEarlySchemaReader();
    reader2.batchLimit = 1;
    reader2.startIndex = 100;

    // Reader returns (a, b)
    MockEarlySchemaReader reader3 = new MockEarlySchemaReader();
    reader3.batchLimit = 1;
    reader3.startIndex = 200;

    BaseScanFixtureBuilder builder = new BaseScanFixtureBuilder();
    builder.setProjection(new String[]{"a", "b"});
    builder.addReader(reader1);
    builder.addReader(reader2);
    builder.addReader(reader3);
    ScanFixture scanFixture = builder.build();
    ScanOperatorExec scan = scanFixture.scanOp;

    // Schema based on (a, b)

    assertTrue(scan.buildSchema());
    assertEquals(1, scan.batchAccessor().schemaVersion());
    scan.batchAccessor().release();

    // Batch from (a, b) reader 1

    assertTrue(scan.next());
    assertEquals(1, scan.batchAccessor().schemaVersion());
    verifyBatch(0, scan.batchAccessor().container());

    // Batch from (a) reader 2
    // Due to schema smoothing, b vector type is left unchanged,
    // but is null filled.

    assertTrue(scan.next());
    assertEquals(1, scan.batchAccessor().schemaVersion());

    SingleRowSet expected = fixture.rowSetBuilder(scan.batchAccessor().schema())
        .addRow(111, null)
        .addRow(121, null)
        .build();
    RowSetUtilities.verify(expected,
        fixture.wrap(scan.batchAccessor().container()));

    // Batch from (a, b) reader 3
    // Recycles b again, back to being a table column.

    assertTrue(scan.next());
    assertEquals(1, scan.batchAccessor().schemaVersion());
    verifyBatch(200, scan.batchAccessor().container());

    assertFalse(scan.next());
    scanFixture.close();
  }


  // TODO: Schema change in late reader
}
