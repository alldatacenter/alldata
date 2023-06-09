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

import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.impl.protocol.BatchAccessor;
import org.apache.drill.exec.physical.impl.scan.ScanTestUtils.ScanFixture;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedReader;
import org.apache.drill.exec.physical.impl.scan.framework.SchemaNegotiator;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.junit.Test;

/**
 * Verifies that the V1 scan framework properly pushes the LIMIT
 * into the scan by stopping the scan once any reader produces
 * the rows up to the limit. Verifies that the result set loader
 * enforces the limit at the batch level, that the reader lifecycle
 * enforces limits at the reader level, and that the scan framework
 * enforces limits at by skipping over unneeded readers.
 */
public class TestScanOperExecLimit extends BaseScanOperatorExecTest {

  /**
   * Mock reader that returns two 50-row batches.
   */
  protected static class Mock50RowReader implements ManagedReader<SchemaNegotiator> {
    protected boolean openCalled;
    protected ResultSetLoader tableLoader;

    @Override
    public boolean open(SchemaNegotiator negotiator) {
      openCalled = true;
      negotiator.tableSchema(new SchemaBuilder()
          .add("a", MinorType.INT)
          .build(), true);
        tableLoader = negotiator.build();
      return true;
    }

    @Override
    public boolean next() {
      if (tableLoader.batchCount() > 2) {
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

  /**
   * LIMIT 0, to obtain only the schema.
   */
  @Test
  public void testLimit0() {
    Mock50RowReader reader1 = new Mock50RowReader();
    Mock50RowReader reader2 = new Mock50RowReader();

    BaseScanFixtureBuilder builder = simpleBuilder(reader1, reader2);
    builder.builder.limit(0);
    ScanFixture scanFixture = builder.build();
    ScanOperatorExec scan = scanFixture.scanOp;

    assertTrue(scan.buildSchema());
    assertTrue(scan.next());
    BatchAccessor batch = scan.batchAccessor();
    assertEquals(0, batch.rowCount());
    assertEquals(1, batch.schema().getFieldCount());
    batch.release();

    // No second batch or second reader
    assertFalse(scan.next());

    scanFixture.close();

    assertTrue(reader1.openCalled);
    assertFalse(reader2.openCalled);
  }

  /**
   * LIMIT 1, simplest case
   */
  @Test
  public void testLimit1() {
    Mock50RowReader reader1 = new Mock50RowReader();
    Mock50RowReader reader2 = new Mock50RowReader();

    BaseScanFixtureBuilder builder = simpleBuilder(reader1, reader2);
    builder.builder.limit(1);
    ScanFixture scanFixture = builder.build();
    ScanOperatorExec scan = scanFixture.scanOp;

    assertTrue(scan.buildSchema());
    assertTrue(scan.next());
    BatchAccessor batch = scan.batchAccessor();
    assertEquals(1, batch.rowCount());
    batch.release();

    // No second batch or second reader
    assertFalse(scan.next());

    scanFixture.close();

    assertTrue(reader1.openCalled);
    assertFalse(reader2.openCalled);
  }

  /**
   * LIMIT 50, same as batch size, to check boundary conditions.
   */
  @Test
  public void testLimitOnBatchEnd() {
    Mock50RowReader reader1 = new Mock50RowReader();
    Mock50RowReader reader2 = new Mock50RowReader();

    BaseScanFixtureBuilder builder = simpleBuilder(reader1, reader2);
    builder.builder.limit(50);
    ScanFixture scanFixture = builder.build();
    ScanOperatorExec scan = scanFixture.scanOp;

    assertTrue(scan.buildSchema());
    assertTrue(scan.next());
    BatchAccessor batch = scan.batchAccessor();
    assertEquals(50, batch.rowCount());
    batch.release();

    // No second batch or second reader
    assertFalse(scan.next());

    scanFixture.close();

    assertTrue(reader1.openCalled);
    assertFalse(reader2.openCalled);
  }

  /**
   * LIMIT 75, halfway through second batch.
   */
  @Test
  public void testLimitOnScondBatch() {
    Mock50RowReader reader1 = new Mock50RowReader();
    Mock50RowReader reader2 = new Mock50RowReader();

    BaseScanFixtureBuilder builder = simpleBuilder(reader1, reader2);
    builder.builder.limit(75);
    ScanFixture scanFixture = builder.build();
    ScanOperatorExec scan = scanFixture.scanOp;

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

    scanFixture.close();

    assertTrue(reader1.openCalled);
    assertFalse(reader2.openCalled);
  }

  /**
   * LIMIT 100, at EOF of the first reader.
   */
  @Test
  public void testLimitOnEOF() {
    Mock50RowReader reader1 = new Mock50RowReader();
    Mock50RowReader reader2 = new Mock50RowReader();

    BaseScanFixtureBuilder builder = simpleBuilder(reader1, reader2);
    builder.builder.limit(100);
    ScanFixture scanFixture = builder.build();
    ScanOperatorExec scan = scanFixture.scanOp;

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

    scanFixture.close();

    assertTrue(reader1.openCalled);
    assertFalse(reader2.openCalled);
  }

  /**
   * LIMIT 125: full first reader, limit on second
   */
  @Test
  public void testLimitOnSecondReader() {
    Mock50RowReader reader1 = new Mock50RowReader();
    Mock50RowReader reader2 = new Mock50RowReader();

    BaseScanFixtureBuilder builder = simpleBuilder(reader1, reader2);
    builder.builder.limit(125);
    ScanFixture scanFixture = builder.build();
    ScanOperatorExec scan = scanFixture.scanOp;

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

    scanFixture.close();

    assertTrue(reader1.openCalled);
    assertTrue(reader2.openCalled);
  }
}
