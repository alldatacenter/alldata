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

import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.impl.protocol.BatchAccessor;
import org.apache.drill.exec.physical.impl.scan.ScanOperatorExec;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.junit.Test;

/**
 * Verifies that the V2 scan framework properly pushes the LIMIT
 * into the scan by stopping the scan once any reader produces
 * the rows up to the limit. Verifies that the result set loader
 * enforces the limit at the batch level, that the reader lifecycle
 * enforces limits at the reader level, and that the scan framework
 * enforces limits at by skipping over unneeded readers.
 * <p>
 * This test is from the outside: at the scan operator level.
 */
public class TestScanLimit extends BaseScanTest {

  /**
   * Mock reader that returns two 50-row batches.
   */
  protected static class Mock50RowReader implements ManagedReader {

    private final ResultSetLoader tableLoader;

    public Mock50RowReader(SchemaNegotiator negotiator) {
      negotiator.tableSchema(new SchemaBuilder()
        .add("a", MinorType.INT)
        .build());
      tableLoader = negotiator.build();
    }

    @Override
    public boolean next() {
      if (tableLoader.batchCount() > 1) {
        return false;
      }
      RowSetLoader rowSet = tableLoader.writer();
      int base = tableLoader.batchCount() * 50 + 1;
      for (int i = 0; i < 50; i++) {
        if (rowSet.isFull()) {
          break;
        }
        rowSet.addSingleCol(base + i);
      }
      return true;
    }

    @Override
    public void close() { }
  }

  private static class TestFixture {
    ObservableCreator creator1;
    ObservableCreator creator2;
    ScanFixture scanFixture;
    ScanOperatorExec scan;

    public TestFixture(long limit) {
      creator1 = new ObservableCreator() {
        @Override
        public ManagedReader create(SchemaNegotiator negotiator) {
          return new Mock50RowReader(negotiator);
        }
      };
      creator2 = new ObservableCreator() {
        @Override
        public ManagedReader create(SchemaNegotiator negotiator) {
          return new Mock50RowReader(negotiator);
        }
      };
      BaseScanFixtureBuilder builder = simpleBuilder(creator1, creator2);
      builder.builder.limit(limit);
      scanFixture = builder.build();
      scan = scanFixture.scanOp;
    }

    public void close() { scanFixture.close(); }

    public int createCount() {
      if (creator1.reader == null) {
        return 0;
      }
      if (creator2.reader == null) {
        return 1;
      }
      return 2;
    }
  }

  /**
   * LIMIT 0, to obtain only the schema.
   */
  @Test
  public void testLimit0() {
    TestFixture fixture = new TestFixture(0);
    ScanOperatorExec scan = fixture.scan;

    assertTrue(scan.buildSchema());
    BatchAccessor batch = scan.batchAccessor();
    assertEquals(0, batch.rowCount());
    assertEquals(1, batch.schema().getFieldCount());
    batch.release();

    // No second batch or second reader
    assertFalse(scan.next());

    fixture.close();

    // Only the first of the two readers were created.
    assertEquals(1, fixture.createCount());
  }

  /**
   * LIMIT 1, simplest case
   */
  @Test
  public void testLimit1() {
    TestFixture fixture = new TestFixture(1);
    ScanOperatorExec scan = fixture.scan;

    // Reader builds schema, and stops after one row, though the reader
    // itself is happy to provide more.
    assertTrue(scan.buildSchema());
    assertTrue(scan.next());
    BatchAccessor batch = scan.batchAccessor();
    assertEquals(1, batch.rowCount());
    batch.release();

    // No second batch or second reader
    assertFalse(scan.next());

    fixture.close();

    // Only the first of the two readers were created.
    assertEquals(1, fixture.createCount());
  }

  /**
   * LIMIT 50, same as batch size, to check boundary conditions.
   */
  @Test
  public void testLimitOnBatchEnd() {
    TestFixture fixture = new TestFixture(50);
    ScanOperatorExec scan = fixture.scan;

    assertTrue(scan.buildSchema());
    assertTrue(scan.next());
    BatchAccessor batch = scan.batchAccessor();
    assertEquals(50, batch.rowCount());
    batch.release();

    // No second batch or second reader
    assertFalse(scan.next());

    fixture.close();

    // Only the first of the two readers were created.
    assertEquals(1, fixture.createCount());
  }

  /**
   * LIMIT 75, halfway through second batch.
   */
  @Test
  public void testLimitOnSecondBatch() {
    TestFixture fixture = new TestFixture(75);
    ScanOperatorExec scan = fixture.scan;

    assertTrue(scan.buildSchema());
    assertTrue(scan.next());
    BatchAccessor batch = scan.batchAccessor();
    assertEquals(50, batch.rowCount());
    batch.release();

    assertTrue(scan.next());
    batch = scan.batchAccessor();
    assertEquals(25, batch.rowCount());
    batch.release();

    // No second reader
    assertFalse(scan.next());

    fixture.close();

    // Only the first of the two readers were created.
    assertEquals(1, fixture.createCount());
  }

  /**
   * LIMIT 100, at EOF of the first reader.
   */
  @Test
  public void testLimitOnEOF() {
    TestFixture fixture = new TestFixture(100);
    ScanOperatorExec scan = fixture.scan;

    assertTrue(scan.buildSchema());
    assertTrue(scan.next());
    BatchAccessor batch = scan.batchAccessor();
    assertEquals(50, batch.rowCount());
    batch.release();

    assertTrue(scan.next());
    batch = scan.batchAccessor();
    assertEquals(50, batch.rowCount());
    batch.release();

    // No second reader
    assertFalse(scan.next());

    fixture.close();
    scan.close();

    // Only the first of the two readers were created.
    assertEquals(1, fixture.createCount());
  }

  /**
   * LIMIT 125: full first reader, limit on second
   */
  @Test
  public void testLimitOnSecondReader() {
    TestFixture fixture = new TestFixture(125);
    ScanOperatorExec scan = fixture.scan;

    assertTrue(scan.buildSchema());
    assertTrue(scan.next());
    BatchAccessor batch = scan.batchAccessor();
    assertEquals(50, batch.rowCount());
    batch.release();

    assertTrue(scan.next());
    batch = scan.batchAccessor();
    assertEquals(50, batch.rowCount());
    batch.release();

    // First batch, second reader
    assertTrue(scan.next());
    batch = scan.batchAccessor();
    assertEquals(25, batch.rowCount());
    batch.release();

    // No second batch
    assertFalse(scan.next());

    fixture.close();

    // Both readers were created.
    assertEquals(2, fixture.createCount());
  }
}
