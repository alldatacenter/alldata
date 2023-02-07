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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.apache.drill.categories.EvfTest;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.physical.impl.scan.RowBatchReader;
import org.apache.drill.exec.physical.impl.scan.v3.ManagedReader;
import org.apache.drill.exec.physical.impl.scan.v3.ManagedReader.EarlyEofException;
import org.apache.drill.exec.physical.impl.scan.v3.ScanLifecycleBuilder;
import org.apache.drill.exec.physical.impl.scan.v3.SchemaNegotiator;
import org.apache.drill.exec.physical.impl.scan.v3.schema.ScanSchemaTracker.ProjectionType;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetTestUtils;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(EvfTest.class)
public class TestScanLifecycleBasics extends BaseTestScanLifecycle {

  /**
   * Simplest possible do-nothing, default config case.
   * Scan produces no valid schema.
   */
  @Test
  public void testNoReaders() {
    ScanLifecycle scan = buildScan(new ScanLifecycleBuilder());
    assertNull(scan.nextReader());
    assertFalse(scan.hasOutputSchema());
    scan.close();
  }

  /**
   * Single reader, late schema, early EOF.
   * The scan never has a valid schema in this case.
   */
  @Test
  public void testEarlyEOF() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.readerFactory(new SingleReaderFactory() {
      @Override
      public ManagedReader next(SchemaNegotiator negotiator)
          throws EarlyEofException {
        return new NoDataReader(negotiator);
      }
    });
    ScanLifecycle scan = buildScan(builder);
    RowBatchReader reader = scan.nextReader();
    assertFalse(reader.open());
    reader.close();
    assertFalse(scan.hasOutputSchema());
    scan.close();
  }

  /**
   * Single reader empty schema, EOF on first next().
   * Since both no schema and no rows, we assume this to be a
   * null (not empty) result set, so no output schema.
   */
  @Test
  public void testNullReader() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.readerFactory(new SingleReaderFactory() {
      @Override
      public ManagedReader next(SchemaNegotiator negotiator) {
        return new MockLateSchemaReader(negotiator, 0);
      }
    });
    ScanLifecycle scan = buildScan(builder);
    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());
    assertFalse(reader.next());
    assertFalse(scan.hasOutputSchema());
    reader.close();
    scan.close();
  }

  /**
   * Single reader early schema, EOF on first next().
   * Since this is an early-schema reader, there is an output
   * schema even without any rows.
   */
  @Test
  public void testNullReaderWithSchema() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.readerFactory(new SingleReaderFactory() {
      @Override
      public ManagedReader next(SchemaNegotiator negotiator) {
        return new MockEarlySchemaReader(negotiator, 0);
      }
    });
    ScanLifecycle scan = buildScan(builder);
    assertSame(ProjectionType.ALL, scan.schemaTracker().projectionType());
    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());

    // Early schema: so output schema is available after open
    assertTrue(scan.hasOutputSchema());
    assertEquals(SCHEMA, scan.outputSchema());
    assertTrue(reader.next());
    VectorContainer result = reader.output();
    assertEquals(0, result.getRecordCount());
    result.zeroVectors();

    // Early schema with no additional columns discovered
    assertEquals(SCHEMA, scan.outputSchema());

    // But, no second batch.
    assertFalse(reader.next());
    reader.close();
    scan.close();
  }

  /**
   * Test SELECT * from an early-schema table of (a, b)
   */
  @Test
  public void testEarlySchemaOneBatch() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.readerFactory(new SingleReaderFactory() {
      @Override
      public ManagedReader next(SchemaNegotiator negotiator) {
        return new MockEarlySchemaReader(negotiator, 1);
      }
    });
    ScanLifecycle scan = buildScan(builder);
    assertSame(ProjectionType.ALL, scan.schemaTracker().projectionType());
    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());

    // Early schema: so output schema is available after open
    assertEquals(SCHEMA, scan.outputSchema());
    assertTrue(reader.next());

    RowSetUtilities.verify(simpleExpected(0), fixture.wrap(reader.output()));

    assertFalse(reader.next());
    reader.close();

    scan.close();
  }

  @Test
  public void testEarlySchemaTwoBatches() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.readerFactory(new SingleReaderFactory() {
      @Override
      public ManagedReader next(SchemaNegotiator negotiator) {
        return new MockEarlySchemaReader(negotiator, 2);
      }
    });
    ScanLifecycle scan = buildScan(builder);
    assertSame(ProjectionType.ALL, scan.schemaTracker().projectionType());
    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());
    assertEquals(SCHEMA, scan.outputSchema());

    assertTrue(reader.next());
    RowSetUtilities.verify(simpleExpected(0), fixture.wrap(reader.output()));

    assertTrue(reader.next());
    RowSetUtilities.verify(simpleExpected(1), fixture.wrap(reader.output()));

    assertFalse(reader.next());
    reader.close();

    scan.close();
  }

  /**
   * Single reader late schema, one batch.
   */
  @Test
  public void testLateSchemaOneBatch() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.readerFactory(new SingleReaderFactory() {
      @Override
      public ManagedReader next(SchemaNegotiator negotiator) {
        return new MockLateSchemaReader(negotiator, 1);
      }
    });
    ScanLifecycle scan = buildScan(builder);
    assertSame(ProjectionType.ALL, scan.schemaTracker().projectionType());
    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());

    // Late schema: so no output schema is available after open
    assertFalse(scan.hasOutputSchema());
    assertTrue(reader.next());

    // Late schema, output schema available after first batch
    assertTrue(scan.hasOutputSchema());
    assertEquals(SCHEMA, scan.outputSchema());
    RowSetUtilities.verify(simpleExpected(0), fixture.wrap(reader.output()));

    assertFalse(reader.next());
    reader.close();

    scan.close();
  }

  @Test
  public void testLateSchemaTwoBatches() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
     builder.readerFactory(new SingleReaderFactory() {
      @Override
      public ManagedReader next(SchemaNegotiator negotiator) {
        return new MockLateSchemaReader(negotiator, 2);
      }
    });
    ScanLifecycle scan = buildScan(builder);
    assertSame(ProjectionType.ALL, scan.schemaTracker().projectionType());
    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());

    // Late schema: so no output schema is available after open
    assertFalse(scan.hasOutputSchema());
    assertTrue(reader.next());

    // Late schema, output schema available after first batch
    assertEquals(SCHEMA, scan.outputSchema());
    RowSetUtilities.verify(simpleExpected(0), fixture.wrap(reader.output()));

    assertTrue(reader.next());
    RowSetUtilities.verify(simpleExpected(1), fixture.wrap(reader.output()));

    assertFalse(reader.next());
    reader.close();

    scan.close();
  }

  /**
   * Test SELECT a, c FROM table(a, b)
   */
  @Test
  public void testEarlySchemaWithProject() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.projection(RowSetTestUtils.projectList("a", "c"));
    builder.readerFactory(new SingleReaderFactory() {
      @Override
      public ManagedReader next(SchemaNegotiator negotiator) {
        return new MockEarlySchemaReader(negotiator, 1);
      }
    });
    ScanLifecycle scan = buildScan(builder);
    assertSame(ProjectionType.SOME, scan.schemaTracker().projectionType());
    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());

    // Early schema: so output schema is available after open
    TupleMetadata expectedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addNullable("c", MinorType.INT)
        .build();
    assertEquals(expectedSchema, scan.outputSchema());

    assertTrue(reader.next());
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(10, null)
        .addRow(20, null)
        .build();
    RowSetUtilities.verify(expected, fixture.wrap(reader.output()));

    assertFalse(reader.next());
    reader.close();
    scan.close();
  }

  /**
   * Test SELECT a, c FROM table(a, b)
   * c will be null
   */
  @Test
  public void testLateSchemaWithProject() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.projection(RowSetTestUtils.projectList("a", "c"));
    builder.readerFactory(new SingleReaderFactory() {
      @Override
      public ManagedReader next(SchemaNegotiator negotiator) {
        return new MockLateSchemaReader(negotiator, 1);
      }
    });
    ScanLifecycle scan = buildScan(builder);
    assertSame(ProjectionType.SOME, scan.schemaTracker().projectionType());
    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());
    assertTrue(reader.next());

    // Late schema: so output schema is available after next()
    TupleMetadata expectedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addNullable("c", MinorType.INT)
        .build();
    assertEquals(expectedSchema, scan.outputSchema());

    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(10, null)
        .addRow(20, null)
        .build();
    RowSetUtilities.verify(expected, fixture.wrap(reader.output()));

    assertFalse(reader.next());
    reader.close();
    scan.close();
  }

  /**
   * Test SELECT b, a FROM table(a, b)
   */
  @Test
  public void testEarlySchemaReorder() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.projection(RowSetTestUtils.projectList("b", "a"));
    builder.readerFactory(new SingleReaderFactory() {
      @Override
      public ManagedReader next(SchemaNegotiator negotiator) {
        return new MockEarlySchemaReader(negotiator, 1);
      }
    });
    ScanLifecycle scan = buildScan(builder);
    assertSame(ProjectionType.SOME, scan.schemaTracker().projectionType());
    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());

    // Early schema: so output schema is available after open
    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("b", MinorType.VARCHAR)
        .add("a", MinorType.INT)
        .build();
    assertEquals(expectedSchema, scan.outputSchema());

    assertTrue(reader.next());
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow("fred", 10)
        .addRow("wilma", 20)
        .build();
    RowSetUtilities.verify(expected, fixture.wrap(reader.output()));

    assertFalse(reader.next());
    reader.close();
    scan.close();
  }

  @Test
  public void testEarlySchemaWithProjectNone() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.projection(RowSetTestUtils.projectNone());
    builder.readerFactory(new SingleReaderFactory() {
      @Override
      public ManagedReader next(SchemaNegotiator negotiator) {
        return new MockEarlySchemaReader(negotiator, 1);
      }
    });
    ScanLifecycle scan = buildScan(builder);
    assertSame(ProjectionType.NONE, scan.schemaTracker().projectionType());
    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());

    // Early schema: so output schema is available after open
    TupleMetadata expectedSchema = new SchemaBuilder()
         .build();
    assertEquals(expectedSchema, scan.outputSchema());

    assertTrue(reader.next());
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow()
        .addRow()
        .build();
    RowSetUtilities.verify(expected, fixture.wrap(reader.output()));

    assertFalse(reader.next());
    reader.close();
    scan.close();
  }

  /**
   * Test SELECT * from an early-schema table of () (that is,
   * a schema that consists of zero columns.
   */
  @Test
  public void testLateSchemaWithCustomType() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.projection(RowSetTestUtils.projectList("a", "c"));
    builder.nullType(Types.optional(MinorType.VARCHAR));
    builder.readerFactory(new SingleReaderFactory() {
      @Override
      public ManagedReader next(SchemaNegotiator negotiator) {
        return new MockLateSchemaReader(negotiator, 1);
      }
    });
    ScanLifecycle scan = buildScan(builder);
    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());
    assertTrue(reader.next());

    // Late schema: so output schema is available after next()
    TupleMetadata expectedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addNullable("c", MinorType.VARCHAR)
        .build();
    assertEquals(expectedSchema, scan.outputSchema());

    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(10, null)
        .addRow(20, null)
        .build();
    RowSetUtilities.verify(expected, fixture.wrap(reader.output()));

    assertFalse(reader.next());
    reader.close();
    scan.close();
  }

  /**
   * Test SELECT a FROM table(a, b)
   */
  @Test
  public void testEarlySchemaSubset() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.projection(RowSetTestUtils.projectList("a"));
    builder.readerFactory(new SingleReaderFactory() {
      @Override
      public ManagedReader next(SchemaNegotiator negotiator) {
        return new MockEarlySchemaReader(negotiator, 1);
      }
    });
    ScanLifecycle scan = buildScan(builder);
    assertSame(ProjectionType.SOME, scan.schemaTracker().projectionType());
    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());

    // Early schema: so output schema is available after open
    TupleMetadata expectedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .build();
    assertEquals(expectedSchema, scan.outputSchema());

    assertTrue(reader.next());
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(10)
        .addRow(20)
        .build();
    RowSetUtilities.verify(expected, fixture.wrap(reader.output()));

    assertFalse(reader.next());
    reader.close();
    scan.close();
  }

  /**
   * Test SELECT * from an early-schema table of () (that is,
   * a schema that consists of zero columns.
   */
  @Test
  public void testEarlySchemaEmpty() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.readerFactory(new SingleReaderFactory() {
      @Override
      public ManagedReader next(SchemaNegotiator negotiator) {
        return new MockEmptySchemaReader(negotiator);
      }
    });
    ScanLifecycle scan = buildScan(builder);
    assertSame(ProjectionType.ALL, scan.schemaTracker().projectionType());
    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());

    // Early schema: so output schema is available after open
    TupleMetadata expectedSchema = new SchemaBuilder()
         .build();
    assertEquals(expectedSchema, scan.outputSchema());

    assertTrue(reader.next());
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow()
        .addRow()
        .build();
    RowSetUtilities.verify(expected, fixture.wrap(reader.output()));

    assertFalse(reader.next());
    reader.close();
    scan.close();
  }

  /**
   * Test SELECT a from an early-schema table of () (that is,
   * a schema that consists of zero columns.
   */
  @Test
  public void testEarlySchemaEmptyWithProject() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.projection(RowSetTestUtils.projectList("a"));
    builder.readerFactory(new SingleReaderFactory() {
      @Override
      public ManagedReader next(SchemaNegotiator negotiator) {
        return new MockEmptySchemaReader(negotiator);
      }
    });
    ScanLifecycle scan = buildScan(builder);
    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());

    // Early schema: so output schema is available after open
    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("a", MinorType.INT)
        .build();
    assertEquals(expectedSchema, scan.outputSchema());

    assertTrue(reader.next());
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addSingleCol(null)
        .addSingleCol(null)
        .build();
    RowSetUtilities.verify(expected, fixture.wrap(reader.output()));

    assertFalse(reader.next());
    reader.close();
    scan.close();
  }
}
