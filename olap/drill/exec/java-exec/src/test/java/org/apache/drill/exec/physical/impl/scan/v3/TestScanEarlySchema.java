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
import static org.junit.Assert.assertTrue;

import org.apache.drill.categories.EvfTest;
import org.apache.drill.exec.physical.impl.scan.ScanOperatorExec;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test "early schema" readers: those that can declare a schema at
 * open time.
 */
@Category(EvfTest.class)
public class TestScanEarlySchema extends BaseScanTest {

  @Test
  public void testEarlySchemaLifecycle() {

    // Create a mock reader, return two batches: one schema-only, another with data.
    ObservableCreator creator = new ObservableCreator() {
      @Override
      public ManagedReader create(SchemaNegotiator negotiator) {
        MockEarlySchemaReader reader = new MockEarlySchemaReader(negotiator);
        reader.batchLimit = 1;
        return reader;
      }
    };

    ScanFixture scanFixture = simpleFixture(creator);
    ScanOperatorExec scan = scanFixture.scanOp;

    SingleRowSet expected = makeExpected();
    RowSetComparison verifier = new RowSetComparison(expected);

    // First batch: return schema.
    assertTrue(scan.buildSchema());
    MockEarlySchemaReader reader = creator.reader();
    assertEquals(0, reader.batchCount);
    assertEquals(expected.batchSchema(), scan.batchAccessor().schema());
    assertEquals(0, scan.batchAccessor().rowCount());

    // Next call, return with data.
    assertTrue(scan.next());
    verifier.verifyAndClearAll(fixture.wrap(scan.batchAccessor().container()));

    // EOF
    assertFalse(scan.next());
    assertEquals(0, scan.batchAccessor().rowCount());

    // Next again: no-op
    assertFalse(scan.next());
    scanFixture.close();

    // Close again: no-op
    scan.close();
  }

  @Test
  public void testEarlySchemaLifecycleNoSchemaBatch() {

    // Create a mock reader, return one batch with data.
    ReaderCreator creator = negotiator -> {
      MockEarlySchemaReader reader = new MockEarlySchemaReader(negotiator);
      reader.batchLimit = 1;
      return reader;
    };

    // Create the scan operator
    BaseScanFixtureBuilder builder = simpleBuilder(creator);
    builder.enableSchemaBatch = false;
    ScanFixture scanFixture = builder.build();
    ScanOperatorExec scan = scanFixture.scanOp;

    SingleRowSet expected = makeExpected();
    RowSetComparison verifier = new RowSetComparison(expected);

    // First batch: return with data.
    assertTrue(scan.next());
    verifier.verifyAndClearAll(fixture.wrap(scan.batchAccessor().container()));

    // EOF
    assertFalse(scan.next());
    assertEquals(0, scan.batchAccessor().rowCount());

    // Next again: no-op
    assertFalse(scan.next());
    scanFixture.close();

    // Close again: no-op
    scan.close();
  }

  private static class MockEarlySchemaReader3 extends MockEarlySchemaReader {

    public MockEarlySchemaReader3(SchemaNegotiator schemaNegotiator) {
      super(schemaNegotiator);
    }

    @Override
    public boolean next() {
      if (batchCount >= batchLimit) {
        return false;
      }
      batchCount++;

      makeBatch();
      return batchCount < batchLimit;
    }
  }

  @Test
  public void testEarlySchemaDataWithEof() {

    // Create a mock reader, return two batches: one schema-only, another with data.
    ReaderCreator creator = negotiator -> {
      MockEarlySchemaReader reader = new MockEarlySchemaReader3(negotiator);
      reader.batchLimit = 1;
      return reader;
    };

    // Create the scan operator
    ScanFixture scanFixture = simpleFixture(creator);
    ScanOperatorExec scan = scanFixture.scanOp;

    SingleRowSet expected = makeExpected();
    RowSetComparison verifier = new RowSetComparison(expected);

    // First batch: return schema.
    assertTrue(scan.buildSchema());
    assertEquals(0, scan.batchAccessor().rowCount());

    // Next call, return with data.
    assertTrue(scan.next());
    verifier.verifyAndClearAll(fixture.wrap(scan.batchAccessor().container()));

    // EOF
    assertFalse(scan.next());
    assertEquals(0, scan.batchAccessor().rowCount());

    // Next again: no-op
    assertFalse(scan.next());
    scanFixture.close();

    // Close again: no-op
    scan.close();
  }

  /**
   * Test EOF on the first batch. Is allowed, but will result in the scan operator
   * passing a null batch to the parent.
   */
  @Test
  public void testEOFOnSchema() {

    // Create a mock reader, return two batches: one schema-only, another with data.
    ReaderCreator creator = negotiator ->
      new EofOnOpenReader(negotiator);

    ScanFixture scanFixture = simpleFixture(creator);
    ScanOperatorExec scan = scanFixture.scanOp;

    // EOF
    assertFalse(scan.buildSchema());
    assertEquals(0, scan.batchAccessor().rowCount());

    scanFixture.close();
  }

  @Test
  public void testEOFOnFirstBatch() {
    ReaderCreator creator = negotiator -> {
      MockEarlySchemaReader reader = new MockEarlySchemaReader(negotiator);
      reader.batchLimit = 0;
      return reader;
    };

    ScanFixture scanFixture = simpleFixture(creator);
    ScanOperatorExec scan = scanFixture.scanOp;
    assertTrue(scan.buildSchema());

    // EOF. Returns a single empty batch with early schema
    // in order to provide an empty result set.

    assertTrue(scan.next());
    assertEquals(0, scan.batchAccessor().rowCount());

    RowSetUtilities.verify(
        RowSetBuilder.emptyBatch(fixture.allocator(), expectedSchema()),
        fixture.wrap(scan.batchAccessor().container()));

    assertFalse(scan.next());
    scanFixture.close();
  }

  /**
   * Test normal case with multiple readers. These return
   * the same schema, so no schema change.
   */

  @Test
  public void testMultipleReaders() {
    ReaderCreator creator1 = negotiator ->
      new EofOnOpenReader(negotiator);

    ReaderCreator creator2 = negotiator -> {
      MockEarlySchemaReader reader = new MockEarlySchemaReader(negotiator);
      reader.batchLimit = 2;
      return reader;
    };
    ReaderCreator creator3 = negotiator -> {
      MockEarlySchemaReader reader = new MockEarlySchemaReader(negotiator);
      reader.batchLimit = 2;
      reader.startIndex = 100;
      return reader;
    };

    ScanFixture scanFixture = simpleFixture(creator1, creator2, creator3);
    ScanOperatorExec scan = scanFixture.scanOp;

    // First batch, schema only.
    assertTrue(scan.buildSchema());
    assertEquals(1, scan.batchAccessor().schemaVersion());
    scan.batchAccessor().release();

    // Second batch.
    assertTrue(scan.next());
    assertEquals(1, scan.batchAccessor().schemaVersion());
    verifyBatch(0, scan.batchAccessor().container());

    // Third batch.
    assertTrue(scan.next());
    assertEquals(1, scan.batchAccessor().schemaVersion());
    verifyBatch(20, scan.batchAccessor().container());

    // Second reader. First batch includes data, no special first-batch
    // handling for the second reader.
    assertTrue(scan.next());
    assertEquals(1, scan.batchAccessor().schemaVersion());
    verifyBatch(100, scan.batchAccessor().container());

    // Second batch from second reader.
    assertTrue(scan.next());
    assertEquals(1, scan.batchAccessor().schemaVersion());
    verifyBatch(120, scan.batchAccessor().container());

    // EOF
    assertFalse(scan.next());
    assertEquals(0, scan.batchAccessor().rowCount());
    scanFixture.close();
  }
}
