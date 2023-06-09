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
package org.apache.drill.exec.physical.impl.scan.v3.lifecycle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.impl.scan.RowBatchReader;
import org.apache.drill.exec.physical.impl.scan.v3.ManagedReader;
import org.apache.drill.exec.physical.impl.scan.v3.ScanLifecycleBuilder;
import org.apache.drill.exec.physical.impl.scan.v3.SchemaNegotiator;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetTestUtils;
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
 * This test is at the level of the scan framework, stripping away
 * the outer scan operator level.
 */
public class TestScanLifecycleLimit extends BaseTestScanLifecycle {

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

  private Pair<TwoReaderFactory, ScanLifecycle> setupScan(long limit) {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.projection(RowSetTestUtils.projectList("a"));
    TwoReaderFactory factory = new TwoReaderFactory() {
      @Override
      public ManagedReader firstReader(SchemaNegotiator negotiator) {
        return new Mock50RowReader(negotiator);
      }

      @Override
      public ManagedReader secondReader(SchemaNegotiator negotiator) {
        return new Mock50RowReader(negotiator);
      }
    };
    builder.readerFactory(factory);

    builder.limit(limit);
    return Pair.of(factory, buildScan(builder));
  }

  /**
   * LIMIT 0, to obtain only the schema.
   */
  @Test
  public void testLimit0() {
    Pair<TwoReaderFactory, ScanLifecycle> pair = setupScan(0);
    TwoReaderFactory factory = pair.getLeft();
    ScanLifecycle scan = pair.getRight();

    // Reader builds schema, but returns no data, though the reader
    // itself is happy to provide data.
    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());
    assertTrue(reader.next());
    RowSet result = fixture.wrap(reader.output());
    assertEquals(0, result.rowCount());
    assertEquals(1, result.schema().size());
    result.clear();

    // No second batch
    assertFalse(reader.next());
    reader.close();

    // No next reader, despite there being two, since we hit the limit.
    assertNull(scan.nextReader());

    scan.close();

    // Only the first of the two readers were created.
    assertEquals(1, factory.count());
  }

  /**
   * LIMIT 1, simplest case
   */
  @Test
  public void testLimit1() {
    Pair<TwoReaderFactory, ScanLifecycle> pair = setupScan(1);
    TwoReaderFactory factory = pair.getLeft();
    ScanLifecycle scan = pair.getRight();

    // Reader builds schema, and stops after one row, though the reader
    // itself is happy to provide more.
    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());
    assertTrue(reader.next());
    RowSet result = fixture.wrap(reader.output());
    assertEquals(1, result.rowCount());
    assertEquals(1, result.schema().size());
    result.clear();

    // No second batch
    assertFalse(reader.next());
    reader.close();

    // No next reader, despite there being two, since we hit the limit.
    assertNull(scan.nextReader());

    scan.close();

    // Only the first of the two readers were created.
    assertEquals(1, factory.count());
  }

  /**
   * LIMIT 50, same as batch size, to check boundary conditions.
   */
  @Test
  public void testLimitOnBatchEnd() {
    Pair<TwoReaderFactory, ScanLifecycle> pair = setupScan(50);
    TwoReaderFactory factory = pair.getLeft();
    ScanLifecycle scan = pair.getRight();

    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());
    assertTrue(reader.next());
    RowSet result = fixture.wrap(reader.output());
    assertEquals(50, result.rowCount());
    result.clear();

    // No second batch
    assertFalse(reader.next());
    reader.close();

    // No next reader, despite there being two, since we hit the limit.
    assertNull(scan.nextReader());

    scan.close();

    // Only the first of the two readers were created.
    assertEquals(1, factory.count());
  }

  /**
   * LIMIT 75, halfway through second batch.
   */
  @Test
  public void testLimitOnSecondBatch() {
    Pair<TwoReaderFactory, ScanLifecycle> pair = setupScan(75);
    TwoReaderFactory factory = pair.getLeft();
    ScanLifecycle scan = pair.getRight();

    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());

    // First batch
    assertTrue(reader.next());
    RowSet result = fixture.wrap(reader.output());
    assertEquals(50, result.rowCount());
    result.clear();

    // Second batch
    assertTrue(reader.next());
    result = fixture.wrap(reader.output());
    assertEquals(25, result.rowCount());
    result.clear();

    // No third batch
    assertFalse(reader.next());
    reader.close();

    // No next reader, despite there being two, since we hit the limit.
    assertNull(scan.nextReader());

    scan.close();

    // Only the first of the two readers were created.
    assertEquals(1, factory.count());
  }

  /**
   * LIMIT 100, at EOF of the first reader.
   */
  @Test
  public void testLimitOnEOF() {
    Pair<TwoReaderFactory, ScanLifecycle> pair = setupScan(100);
    TwoReaderFactory factory = pair.getLeft();
    ScanLifecycle scan = pair.getRight();

    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());

    // First batch
    assertTrue(reader.next());
    RowSet result = fixture.wrap(reader.output());
    assertEquals(50, result.rowCount());
    result.clear();

    // Second batch
    assertTrue(reader.next());
    result = fixture.wrap(reader.output());
    assertEquals(50, result.rowCount());
    result.clear();

    // No third batch
    assertFalse(reader.next());
    reader.close();

    // No next reader, despite there being two, since we hit the limit.
    assertNull(scan.nextReader());

    scan.close();

    // Only the first of the two readers were created.
    assertEquals(1, factory.count());
  }

  /**
   * LIMIT 125: full first reader, limit on second
   */
  @Test
  public void testLimitOnSecondReader() {
    Pair<TwoReaderFactory, ScanLifecycle> pair = setupScan(125);
    TwoReaderFactory factory = pair.getLeft();
    ScanLifecycle scan = pair.getRight();

    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());

    // First batch
    assertTrue(reader.next());
    RowSet result = fixture.wrap(reader.output());
    assertEquals(50, result.rowCount());
    result.clear();

    // Second batch
    assertTrue(reader.next());
    result = fixture.wrap(reader.output());
    assertEquals(50, result.rowCount());
    result.clear();

    // No third batch
    assertFalse(reader.next());
    reader.close();

    // Move to second reader.
    reader = scan.nextReader();
    assertNotNull(reader);
    assertTrue(reader.open());

    // First is limited
    assertTrue(reader.next());
    result = fixture.wrap(reader.output());
    assertEquals(25, result.rowCount());
    result.clear();

    // No second batch
    assertFalse(reader.next());
    reader.close();

    scan.close();

    // Both readers were created.
    assertEquals(2, factory.count());
  }
}
