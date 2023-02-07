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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.drill.categories.EvfTest;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.physical.impl.scan.RowBatchReader;
import org.apache.drill.exec.physical.impl.scan.v3.ManagedReader;
import org.apache.drill.exec.physical.impl.scan.v3.ScanLifecycleBuilder;
import org.apache.drill.exec.physical.impl.scan.v3.SchemaNegotiator;
import org.apache.drill.exec.physical.impl.scan.v3.schema.ScanSchemaTracker.ProjectionType;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(EvfTest.class)
public class TestScanLifecycleSchema extends BaseTestScanLifecycle {

  /**
   * Simplest defined schema case: the defined schema agrees
   * with the the schema the reader will produce. The defined schema
   * implies the project list, which is not visible here.
   */
  @Test
  public void testDefinedSchemaSimple() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.definedSchema(SCHEMA);
    builder.readerFactory(new SingleReaderFactory() {
      @Override
      public ManagedReader next(SchemaNegotiator negotiator) {
        return new MockEarlySchemaReader(negotiator, 1);
      }
    });
    ScanLifecycle scan = buildScan(builder);

    // Projection defaults to SELECT *, but defined schema
    // overrides that setting.
    assertSame(ProjectionType.SOME, scan.schemaTracker().projectionType());

    verifyStandardReader(scan, 0);
    scan.close();
  }

  /**
   * The defined schema is a subset of the reader's schema; the
   * defined schema acts as a project list.
   */
  @Test
  public void testDefinedSchemaSubset() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.definedSchema(SCHEMA);
    builder.readerFactory(new SingleReaderFactory() {
      @Override
      public ManagedReader next(SchemaNegotiator negotiator) {
        return new MockThreeColReader(negotiator);
      }
    });
    ScanLifecycle scan = buildScan(builder);

    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());
    assertEquals(SCHEMA, scan.outputSchema());
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
   * The defined schema is a superset of the reader's schema; the
   * defined schema defines the missing column type.
   */
  @Test
  public void testDefinedSchemaSupersset() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.definedSchema(SCHEMA);
    builder.readerFactory(new SingleReaderFactory() {
      @Override
      public ManagedReader next(SchemaNegotiator negotiator) {
        return new MockSingleColReader(negotiator);
      }
    });
    ScanLifecycle scan = buildScan(builder);

    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());
    assertEquals(SCHEMA, scan.outputSchema());
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
   * Reader produces a schema which conflicts with the defined schema.
   * The defined schema should be something the reader can implement; so
   * it is an error if the reader does not do so.
   */
  @Test
  public void testDefinedSchemaConflict() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.definedSchema(SCHEMA);
    builder.readerFactory(new SingleReaderFactory() {
      @Override
      public ManagedReader next(SchemaNegotiator negotiator) {
        return new MockEarlySchemaTypeConflictReader(negotiator);
      }
    });
    ScanLifecycle scan = buildScan(builder);

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
   * Simplest provided schema case: the defined schema agrees
   * with the the schema the reader will produce. The provided schema
   * is separate from the project list.
   */
  @Test
  public void testProvidedSchemaSimple() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.providedSchema(SCHEMA);
    builder.readerFactory(new SingleReaderFactory() {
      @Override
      public ManagedReader next(SchemaNegotiator negotiator) {
        return new MockEarlySchemaReader(negotiator, 1);
      }
    });
    ScanLifecycle scan = buildScan(builder);
    assertSame(ProjectionType.ALL, scan.schemaTracker().projectionType());

    verifyStandardReader(scan, 0);
    scan.close();
  }

  /**
   * Lenient provided schema which is a subset of the reader's schema; the
   * provided schema agrees with the reader types
   */
  @Test
  public void testLenientProvidedSchemaSubset() {
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.providedSchema(SCHEMA);
    builder.readerFactory(new SingleReaderFactory() {
      @Override
      public ManagedReader next(SchemaNegotiator negotiator) {
        return new MockThreeColReader(negotiator);
      }
    });
    ScanLifecycle scan = buildScan(builder);
    assertSame(ProjectionType.ALL, scan.schemaTracker().projectionType());

    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());
    assertEquals(MockThreeColReader.READER_SCHEMA, scan.outputSchema());
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
   * Lenient provided schema which is a subset of the reader's schema; the
   * provided schema agrees with the reader types
   */
  @Test
  public void testStrictProvidedSchemaSubset() {
    TupleMetadata schema = new SchemaBuilder()
        .addAll(SCHEMA)
        .build();
    schema.setBooleanProperty(TupleMetadata.IS_STRICT_SCHEMA_PROP, true);
    ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    builder.providedSchema(schema);
    builder.readerFactory(new SingleReaderFactory() {
      @Override
      public ManagedReader next(SchemaNegotiator negotiator) {
        return new MockThreeColReader(negotiator);
      }
    });
    ScanLifecycle scan = buildScan(builder);

    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());
    assertEquals(SCHEMA, scan.outputSchema());
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
}
