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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.drill.categories.EvfTest;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.physical.impl.scan.RowBatchReader;
import org.apache.drill.exec.physical.impl.scan.v3.ManagedReader;
import org.apache.drill.exec.physical.impl.scan.v3.ScanLifecycleBuilder;
import org.apache.drill.exec.physical.impl.scan.v3.SchemaNegotiator;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetTestUtils;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test two readers in succession in various cases: empty readers, normal readers,
 * type conflicts, etc.
 */
@Category(EvfTest.class)
public class TestScanLifecycleTwoReaders extends BaseTestScanLifecycle {

  /**
   * Two null readers: neither provides a valid scan schema.
   */
  @Test
  public void testTwoNullReaders() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.readerFactory(new TwoReaderFactory() {
      @Override
      public ManagedReader firstReader(SchemaNegotiator negotiator) {
        return new MockLateSchemaReader(negotiator, 0);
      }

      @Override
      public ManagedReader secondReader(SchemaNegotiator negotiator) {
        return new MockLateSchemaReader(negotiator, 0);
       }
    });
    ScanLifecycle scan = buildScan(builder);

    verifyEmptyReader(scan);
    verifyEmptyReader(scan);
    scan.close();
  }

  @Test
  public void testNullThenValidReader() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.readerFactory(new TwoReaderFactory() {
      @Override
      public ManagedReader firstReader(SchemaNegotiator negotiator) {
        return new MockLateSchemaReader(negotiator, 0);
      }

      @Override
      public ManagedReader secondReader(SchemaNegotiator negotiator) {
        return new MockLateSchemaReader(negotiator, 1);
       }
    });
    ScanLifecycle scan = buildScan(builder);

    verifyEmptyReader(scan);
    verifyStandardReader(scan, 0);
    scan.close();
  }

  @Test
  public void testValidThenNullReader() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.readerFactory(new TwoReaderFactory() {
      @Override
      public ManagedReader firstReader(SchemaNegotiator negotiator) {
        return new MockLateSchemaReader(negotiator, 1);
      }

      @Override
      public ManagedReader secondReader(SchemaNegotiator negotiator) {
        return new MockLateSchemaReader(negotiator, 0);
      }
    });
    ScanLifecycle scan = buildScan(builder);

    verifyStandardReader(scan, 0);

    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());
    assertFalse(reader.next());
    assertTrue(scan.hasOutputSchema());
    reader.close();
    scan.close();
  }

  @Test
  public void testTwoValidReaders() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.readerFactory(new TwoReaderFactory() {
      @Override
      public ManagedReader firstReader(SchemaNegotiator negotiator) {
        return new MockLateSchemaReader(negotiator, 1);
      }

      @Override
      public ManagedReader secondReader(SchemaNegotiator negotiator) {
        return new MockLateSchemaReader(negotiator, 1);
      }
    });
    ScanLifecycle scan = buildScan(builder);

    verifyStandardReader(scan, 0);
    verifyStandardReader(scan, 0);
    scan.close();
  }

  /**
   * SELECT * FROM two readers, one with (a, b), the other with just (a).
   * The scan will reuse the type of b from the first reader when filling
   * the missing column in the second reader. Since the schema shrinks,
   * the result is the same whether schema change is allowed or not.
   */
  @Test
  public void testShrinkingSchema() {
    doTestShrinkingSchema(true);
    doTestShrinkingSchema(false);
  }

  private void doTestShrinkingSchema(boolean allowSchemaChange) {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
     builder.allowSchemaChange(allowSchemaChange);
    builder.readerFactory(new TwoReaderFactory() {
      @Override
      public ManagedReader firstReader(SchemaNegotiator negotiator) {
        return new MockLateSchemaReader(negotiator, 1);
      }

      @Override
      public ManagedReader secondReader(SchemaNegotiator negotiator) {
        return new MockSingleColReader(negotiator);
      }
    });
    ScanLifecycle scan = buildScan(builder);

    verifyStandardReader(scan, 0);

    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());
    assertTrue(reader.next());
    RowSet expected = fixture.rowSetBuilder(SCHEMA)
        .addRow(101, null)
        .addRow(102, null)
        .build();
    RowSetUtilities.verify(expected, fixture.wrap(reader.output()));
    assertFalse(reader.next());
    reader.close();

    scan.close();
  }

  /**
   * Shrinking schema, as above. Explicit projection:<pre><code>
   * SELECT a, b FROM (a) then (a,b)
   * </code></pre><p>
   * But choose a missing column type (the default
   * Nullable INT) in the first reader that will conflict with the actual column type
   * (VARCHAR) in the second.
   */
  @Test
  public void testShrinkingSchemaWithConflict() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.projection(RowSetTestUtils.projectList("a", "b"));
    builder.readerFactory(new TwoReaderFactory() {
      @Override
      public ManagedReader firstReader(SchemaNegotiator negotiator) {
        return new MockSingleColReader(negotiator);
      }

      @Override
      public ManagedReader secondReader(SchemaNegotiator negotiator) {
        return new MockEarlySchemaReader(negotiator, 1);
      }
    });
    ScanLifecycle scan = buildScan(builder);

    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());
    assertTrue(reader.next());
    reader.output().clear();
    assertFalse(reader.next());
    reader.close();

    reader = scan.nextReader();
    try {
      reader.open();
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("conflict"));
    }
    reader.close();
    scan.close();
  }

  /**
   * SELECT * FROM two readers, one (a, b), the other (a, b, c).
   * With schema change enabled, the third column shows up only
   * in the second batch, forcing a schema change downstream.
   */
  @Test
  public void testExpandingSchemaAllowingSchemaChange() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.allowSchemaChange(true);
    builder.readerFactory(new TwoReaderFactory() {
      @Override
      public ManagedReader firstReader(SchemaNegotiator negotiator) {
        return new MockLateSchemaReader(negotiator, 1);
      }

      @Override
      public ManagedReader secondReader(SchemaNegotiator negotiator) {
        return new MockThreeColReader(negotiator);
      }
    });
    ScanLifecycle scan = buildScan(builder);

    verifyStandardReader(scan, 0);

    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());
    assertTrue(reader.next());
    RowSet expected = fixture.rowSetBuilder(MockThreeColReader.READER_SCHEMA)
        .addRow(101, "wilma", 1001)
        .addRow(102, "betty", 1002)
        .build();
    RowSetUtilities.verify(expected, fixture.wrap(reader.output()));
    assertFalse(reader.next());
    reader.close();

    scan.close();
  }

  /**
   * SELECT * FROM two readers, one (a, b), the other (a, b, c).
   * With schema change disabled, the third column is ignored in the
   * second reader: the first reader "fixes" projection to just (a, b)
   * even though projection started as wildcard.
   * <p>
   * We can argue if this behavior is correct. We know most operators,
   * and no ODBC or JDBC client can handle schema change. So, our other
   * options are do the schema change anyway (the above test), fail, or
   * this choice, which is to try to muddle though. The correct solution
   * is to provide a schema (tested elsewhere.)
   */
  @Test
  public void testExpandingSchemaDisallowingSchemaChange() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.allowSchemaChange(false);
    builder.readerFactory(new TwoReaderFactory() {
      @Override
      public ManagedReader firstReader(SchemaNegotiator negotiator) {
        return new MockLateSchemaReader(negotiator, 1);
      }

      @Override
      public ManagedReader secondReader(SchemaNegotiator negotiator) {
        return new MockThreeColReader(negotiator);
      }
    });
    ScanLifecycle scan = buildScan(builder);

    verifyStandardReader(scan, 0);

    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());
    assertTrue(reader.next());
    RowSet expected = fixture.rowSetBuilder(SCHEMA)
        .addRow(101, "wilma")
        .addRow(102, "betty")
        .build();
    RowSetUtilities.verify(expected, fixture.wrap(reader.output()));
    assertFalse(reader.next());
    reader.close();

    scan.close();
  }

  /**
   * SELECT * from two readers: both (a, b), but with a conflicting
   * type for the column b. The reader fail will with a type conflict.
   */
  @Test
  public void testEarlySchemaTypeConflict() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.allowSchemaChange(true);
    builder.readerFactory(new TwoReaderFactory() {
      @Override
      public ManagedReader firstReader(SchemaNegotiator negotiator) {
        return new MockLateSchemaReader(negotiator, 1);
      }

      @Override
      public ManagedReader secondReader(SchemaNegotiator negotiator) {
        return new MockEarlySchemaTypeConflictReader(negotiator);
      }
    });
    ScanLifecycle scan = buildScan(builder);

    verifyStandardReader(scan, 0);

    RowBatchReader reader = scan.nextReader();
    try {
      reader.open();
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("conflict"));
    }
    reader.close();
    scan.close();
  }

  /**
   * SELECT * from two readers: both (a, b), but with a conflicting
   * type for the column b. The reader fail will with a type conflict.
   */
  @Test
  public void testLateSchemaTypeConflict() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.allowSchemaChange(true);
    builder.readerFactory(new TwoReaderFactory() {
      @Override
      public ManagedReader firstReader(SchemaNegotiator negotiator) {
        return new MockLateSchemaReader(negotiator, 1);
      }

      @Override
      public ManagedReader secondReader(SchemaNegotiator negotiator) {
        return new MockLateSchemaTypeConflictReader(negotiator);
      }
    });
    ScanLifecycle scan = buildScan(builder);

    verifyStandardReader(scan, 0);

    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());
    try {
      reader.next();
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("conflict"));
    }
    reader.close();
    scan.close();
  }

  /**
   * SELECT * from two readers: both (a, b), but with a conflicting
   * type for the column b. The reader fail will with a type conflict.
   * We could possibly try to handle a NULLABLE --> NON NULLABLE conflict,
   * but there does not yet appear to be a compelling case for doing so.
   */
  @Test
  public void testModeConflict() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.allowSchemaChange(true);
    builder.readerFactory(new TwoReaderFactory() {
      @Override
      public ManagedReader firstReader(SchemaNegotiator negotiator) {
        return new MockLateSchemaReader(negotiator, 1);
      }

      @Override
      public ManagedReader secondReader(SchemaNegotiator negotiator) {
        return new MockModeConflictReader(negotiator);
      }
    });
    ScanLifecycle scan = buildScan(builder);

    verifyStandardReader(scan, 0);

    RowBatchReader reader = scan.nextReader();
    try {
      reader.open();
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("conflict"));
    }
    reader.close();
    scan.close();
  }

  /**
   * {@code SELECT * FROM} (a, b) then (b,a). The order of the first
   * table drives the order of the results.
   */
  @Test
  public void testColumnReorderingAB() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
     builder.readerFactory(new TwoReaderFactory() {
      @Override
      public ManagedReader firstReader(SchemaNegotiator negotiator) {
        return new MockLateSchemaReader(negotiator, 1);
      }

      @Override
      public ManagedReader secondReader(SchemaNegotiator negotiator) {
        return new MockReorderedReader(negotiator);
      }
    });
     ScanLifecycle scan = buildScan(builder);

    verifyStandardReader(scan, 0);

    RowBatchReader reader = scan.nextReader();
    reader = scan.nextReader();
    assertTrue(reader.open());
    assertTrue(reader.next());
    RowSet expected = fixture.rowSetBuilder(SCHEMA)
      .addRow(30, "barney")
      .addRow(40, "betty")
      .build();
    RowSetUtilities.verify(expected, fixture.wrap(reader.output()));
    assertFalse(reader.next());
    reader.close();
    scan.close();
  }

  /**
   * {@code SELECT * FROM} (b,a) then (a, b). The order of the first
   * table drives the order of the results.
   */
  @Test
  public void testColumnReorderingBA() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.readerFactory(new TwoReaderFactory() {
      @Override
      public ManagedReader firstReader(SchemaNegotiator negotiator) {
        return new MockReorderedReader(negotiator);
      }

      @Override
      public ManagedReader secondReader(SchemaNegotiator negotiator) {
        return new MockLateSchemaReader(negotiator, 1);
      }
    });
    ScanLifecycle scan = buildScan(builder);

    RowBatchReader reader = scan.nextReader();
    reader = scan.nextReader();
    assertTrue(reader.open());
    assertTrue(reader.next());
    RowSet expected = fixture.rowSetBuilder(MockReorderedReader.READER_SCHEMA)
      .addRow("barney", 30)
      .addRow("betty", 40)
      .build();
    RowSetUtilities.verify(expected, fixture.wrap(reader.output()));
    assertFalse(reader.next());
    reader.close();

    reader = scan.nextReader();
    reader = scan.nextReader();
    assertTrue(reader.open());
    assertTrue(reader.next());
    expected = fixture.rowSetBuilder(MockReorderedReader.READER_SCHEMA)
      .addRow("fred", 10)
      .addRow("wilma", 20)
      .build();
    RowSetUtilities.verify(expected, fixture.wrap(reader.output()));
    assertFalse(reader.next());
    reader.close();
    scan.close();
  }

  /**
   * {@code SELECT , b FROM} (b,a) then (a, b). The order of the project list
   * drives the order of the results.
   */
  @Test
  public void testSpecifiedColumnOrder() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.projection(RowSetTestUtils.projectList("a", "b"));
    builder.readerFactory(new TwoReaderFactory() {
      @Override
      public ManagedReader firstReader(SchemaNegotiator negotiator) {
        return new MockReorderedReader(negotiator);
      }

      @Override
      public ManagedReader secondReader(SchemaNegotiator negotiator) {
        return new MockLateSchemaReader(negotiator, 1);
      }
    });
    ScanLifecycle scan = buildScan(builder);

    RowBatchReader reader = scan.nextReader();
    reader = scan.nextReader();
    assertTrue(reader.open());
    assertTrue(reader.next());
    RowSet expected = fixture.rowSetBuilder(SCHEMA)
      .addRow(30, "barney")
      .addRow(40, "betty")
      .build();
    RowSetUtilities.verify(expected, fixture.wrap(reader.output()));
    assertFalse(reader.next());
    reader.close();

    verifyStandardReader(scan, 0);
    scan.close();
  }
}
