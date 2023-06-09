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
package org.apache.drill.exec.physical.impl.scan.v3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.drill.categories.EvfTest;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.impl.scan.ScanOperatorExec;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.metadata.ColumnBuilder;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test "late schema" readers: those like JSON that discover their schema
 * as they read data.
 */
@Category(EvfTest.class)
public class TestScanLateSchema extends BaseScanTest {

  /**
   * "Late schema" reader, meaning that the reader does not know the schema on
   * open, but must "discover" it when reading data.
   */
  private static class MockLateSchemaReader extends BaseMockBatchReader {

    public boolean returnDataOnFirst;

    public MockLateSchemaReader(SchemaNegotiator schemaNegotiator) {

      // No schema or file, just build the table loader.
      tableLoader = schemaNegotiator.build();
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
        if (!returnDataOnFirst) {
          return true;
        }
      }

      makeBatch();
      return true;
    }
  }

  /**
   * Most basic test of a reader that discovers its schema as it goes along.
   * The purpose is to validate the most basic life-cycle steps before trying
   * more complex variations.
   */
  @Test
  public void testLateSchemaLifecycle() {

    // Create a mock reader, return two batches: one schema-only, another with data.
    ReaderCreator creator = negotiator -> {
      MockLateSchemaReader reader = new MockLateSchemaReader(negotiator);
      reader.batchLimit = 2;
      reader.returnDataOnFirst = false;
      return reader;
    };

    // Create the scan operator
    ScanFixture scanFixture = simpleFixture(creator);
    ScanOperatorExec scan = scanFixture.scanOp;

    // First batch: build schema. The reader does not help: it returns an
    // empty first batch.
    assertTrue(scan.buildSchema());
    assertEquals(0, scan.batchAccessor().rowCount());

    // Create the expected result.
    SingleRowSet expected = makeExpected(20);
    RowSetComparison verifier = new RowSetComparison(expected);
    assertEquals(expected.batchSchema(), scan.batchAccessor().schema());

    // Next call, return with data.
    assertTrue(scan.next());
    verifier.verifyAndClearAll(fixture.wrap(scan.batchAccessor().container()));

    // EOF
    assertFalse(scan.next());
    assertEquals(0, scan.batchAccessor().rowCount());
    scanFixture.close();
  }

  @Test
  public void testLateSchemaLifecycleNoSchemaBatch() {

    // Create a mock reader, return two batches: one schema-only, another with data.
    ReaderCreator creator = negotiator -> {
      MockLateSchemaReader reader = new MockLateSchemaReader(negotiator);
      reader.batchLimit = 2;
      reader.returnDataOnFirst = true;
      return reader;
    };

    BaseScanFixtureBuilder builder = simpleBuilder(creator);
    builder.enableSchemaBatch = false;
    ScanFixture scanFixture = builder.build();
    ScanOperatorExec scan = scanFixture.scanOp;

    // Create the expected result.

    // First batch with data.
    assertTrue(scan.next());
    RowSetUtilities.verify(makeExpected(0),
        fixture.wrap(scan.batchAccessor().container()));

    // Second batch.
    assertTrue(scan.next());
    RowSetUtilities.verify(makeExpected(20),
        fixture.wrap(scan.batchAccessor().container()));

     // EOF
    assertFalse(scan.next());
    assertEquals(0, scan.batchAccessor().rowCount());
    scanFixture.close();
  }

  /**
   * Test the case that a late scan operator is closed before
   * the first reader is opened.
   */
  @Test
  public void testLateSchemaEarlyClose() {

    // Create a mock reader, return two batches: one schema-only, another with data.
    ObservableCreator creator = new ObservableCreator() {
      @Override
      public ManagedReader create(SchemaNegotiator negotiator) {
        return new MockLateSchemaReader(negotiator);
      }
    };

    ScanFixture scanFixture = simpleFixture(creator);

    // Reader never opened.
    scanFixture.close();
    assertNull(creator.reader);
  }

  /**
   * Test the case that a late schema reader is closed after discovering
   * schema, before any calls to next().
   */
  @Test
  public void testLateSchemaEarlyReaderClose() {

    // Create a mock reader, return two batches: one schema-only, another with data.
    ObservableCreator creator = new ObservableCreator() {
      @Override
      public ManagedReader create(SchemaNegotiator negotiator) {
        MockLateSchemaReader reader = new MockLateSchemaReader(negotiator);
        reader.batchLimit = 2;
        reader.returnDataOnFirst = false;
        return reader;
      }
    };

    ScanFixture scanFixture = simpleFixture(creator);
    ScanOperatorExec scan = scanFixture.scanOp;

    // Get the schema as above.
    assertTrue(scan.buildSchema());

    // No lookahead batch created.
    scanFixture.close();
    MockLateSchemaReader reader = creator.reader();
    assertEquals(1, reader.batchCount);
    assertTrue(reader.closeCalled);
  }

  /**
   * Test the case that a late schema reader is closed before
   * consuming the look-ahead batch used to infer schema.
   */
  @Test
  public void testLateSchemaEarlyCloseWithData() {

    // Create a mock reader, return two batches: one schema-only, another with data.
    ObservableCreator creator = new ObservableCreator() {
      @Override
      public ManagedReader create(SchemaNegotiator negotiator) {
        MockLateSchemaReader reader = new MockLateSchemaReader(negotiator);
        reader.batchLimit = 2;
        reader.returnDataOnFirst = true;
        return reader;
      }
    };

    ScanFixture scanFixture = simpleFixture(creator);
    ScanOperatorExec scan = scanFixture.scanOp;

    // Get the schema as above.
    assertTrue(scan.buildSchema());

    // Lookahead batch created.
    scanFixture.close();
    MockLateSchemaReader reader = creator.reader();
    assertEquals(1, reader.batchCount);
    assertTrue(reader.closeCalled);
  }

  /**
   * Test a late-schema source that has no file information.
   * (Like a Hive or JDBC data source.)
   */
  @Test
  public void testLateSchemaLifecycleNoFile() {

    // Create a mock reader, return two batches: one schema-only, another with data.
    ReaderCreator creator = negotiator -> {
      MockLateSchemaReader reader = new MockLateSchemaReader(negotiator);
      reader.batchLimit = 2;
      reader.returnDataOnFirst = false;
      return reader;
    };

    ScanFixture scanFixture = simpleFixture(creator);
    ScanOperatorExec scan = scanFixture.scanOp;

    // First batch: build schema. The reader helps: it returns an
    // empty first batch.
    assertTrue(scan.buildSchema());
    assertEquals(0, scan.batchAccessor().rowCount());

    // Create the expected result.
    SingleRowSet expected = makeExpected(20);
    RowSetComparison verifier = new RowSetComparison(expected);
    assertEquals(expected.batchSchema(), scan.batchAccessor().schema());

    // Next call, return with data.
    assertTrue(scan.next());
    verifier.verifyAndClearAll(fixture.wrap(scan.batchAccessor().container()));

    // EOF
    assertFalse(scan.next());
    assertEquals(0, scan.batchAccessor().rowCount());
    scanFixture.close();
  }

  @Test
  public void testLateSchemaNoData() {

    // Create a mock reader, return two batches: one schema-only, another with data.
    ReaderCreator creator = negotiator -> {
      MockLateSchemaReader reader = new MockLateSchemaReader(negotiator);
      reader.batchLimit = 0;
      reader.returnDataOnFirst = false;
      return reader;
    };

    ScanFixture scanFixture = simpleFixture(creator);
    ScanOperatorExec scan = scanFixture.scanOp;

    // First batch: EOF.
    assertFalse(scan.buildSchema());
    scanFixture.close();
  }

  @Test
  public void testLateSchemaDataOnFirst() {

    // Create a mock reader, return two batches: one schema-only, another with data.
    ReaderCreator creator = negotiator -> {
      MockLateSchemaReader reader = new MockLateSchemaReader(negotiator);
      reader.batchLimit = 1;
      reader.returnDataOnFirst = true;
      return reader;
    };

    ScanFixture scanFixture = simpleFixture(creator);
    ScanOperatorExec scan = scanFixture.scanOp;

    // First batch: build schema. The reader helps: it returns an
    // empty first batch.
    assertTrue(scan.buildSchema());
    assertEquals(0, scan.batchAccessor().rowCount());

    SingleRowSet expected = makeExpected();
    RowSetComparison verifier = new RowSetComparison(expected);
    assertEquals(expected.batchSchema(), scan.batchAccessor().schema());

    // Next call, return with data.
    assertTrue(scan.next());
    verifier.verifyAndClearAll(fixture.wrap(scan.batchAccessor().container()));

    // EOF
    assertFalse(scan.next());
    assertEquals(0, scan.batchAccessor().rowCount());
    scanFixture.close();
  }

  /**
  * Test the case where the reader does not play the "first batch contains
  * only schema" game, and instead returns data. The Scan operator will
  * split the first batch into two: one with schema only, another with
  * data.
  */
 @Test
 public void testNonEmptyFirstBatch() {

   ReaderCreator creator = negotiator -> {
     MockLateSchemaReader reader = new MockLateSchemaReader(negotiator);
     reader.batchLimit = 2;
     reader.returnDataOnFirst = true;
     return reader;
   };

   ScanFixture scanFixture = simpleFixture(creator);
   ScanOperatorExec scan = scanFixture.scanOp;

   // First batch. The reader returns a non-empty batch. The scan
   // operator strips off the schema and returns just that.
   assertTrue(scan.buildSchema());
   SingleRowSet expected = makeExpected();
   assertEquals(expected.batchSchema(), scan.batchAccessor().schema());
   assertEquals(0, scan.batchAccessor().rowCount());
   scan.batchAccessor().release();

   // Second batch. Returns the "look-ahead" batch returned by
   // the reader earlier.
   assertTrue(scan.next());
   RowSetUtilities.verify(expected,
     fixture.wrap(scan.batchAccessor().container()));

   // Third batch, normal case.

   assertTrue(scan.next());
   RowSetUtilities.verify(makeExpected(20),
     fixture.wrap(scan.batchAccessor().container()));

   // EOF
   assertFalse(scan.next());
   assertEquals(0, scan.batchAccessor().rowCount());
   scanFixture.close();
 }
}
