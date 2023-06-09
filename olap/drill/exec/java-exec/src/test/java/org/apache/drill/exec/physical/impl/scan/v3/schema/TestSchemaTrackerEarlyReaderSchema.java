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
package org.apache.drill.exec.physical.impl.scan.v3.schema;

import static org.apache.drill.exec.physical.impl.scan.v3.schema.BaseTestSchemaTracker.trackerFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.drill.categories.EvfTest;
import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.common.exceptions.EmptyErrorContext;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.impl.scan.ScanTestUtils;
import org.apache.drill.exec.physical.impl.scan.v3.file.ImplicitColumnResolver;
import org.apache.drill.exec.physical.impl.scan.v3.file.ImplicitColumnResolver.ImplicitColumnOptions;
import org.apache.drill.exec.physical.rowSet.RowSetTestUtils;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.SubOperatorTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests early reader schema against a provided schema.
 * Basic tests of early reader against a project list are in
 * {@link TestSchemaTrackerInputSchema}.
 */
@Category(EvfTest.class)
public class TestSchemaTrackerEarlyReaderSchema extends SubOperatorTest {
  protected static final CustomErrorContext ERROR_CONTEXT = EmptyErrorContext.INSTANCE;
  protected static final TupleMetadata SCHEMA = BaseTestSchemaTracker.SCHEMA;

  /**
   * If a reader column shadows an implicit column, then we treat the
   * reader column as unprojected and log a warning (that warning is not
   * tested here.)
   */
  @Test
  public void shadowImplicit() {
    ProjectionSchemaTracker tracker = trackerFor(
        RowSetTestUtils.projectList("a",
            ScanTestUtils.FULLY_QUALIFIED_NAME_COL));
    ImplicitColumnOptions options = new ImplicitColumnOptions()
        .optionSet(fixture.getOptionManager());
    ImplicitColumnResolver parser = new ImplicitColumnResolver(options, ERROR_CONTEXT);
    parser.parse(tracker);

    TupleMetadata readerSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addNullable(ScanTestUtils.FULLY_QUALIFIED_NAME_COL, MinorType.BIGINT)
        .build();
    tracker.applyEarlyReaderSchema(readerSchema);

    TupleMetadata expected = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add(ScanTestUtils.FULLY_QUALIFIED_NAME_COL, MinorType.VARCHAR)
        .build();
    assertEquals(expected, tracker.internalSchema().toSchema());
  }

  @Test
  public void testWildcardLenientWithSubset() {
    ProjectionSchemaTracker tracker = trackerFor(
        RowSetTestUtils.projectAll());
    tracker.applyProvidedSchema(SCHEMA);
    assertTrue(tracker.isResolved());
    TupleMetadata readerSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .build();
    tracker.applyEarlyReaderSchema(readerSchema);
    assertTrue(tracker.isResolved());
    assertEquals(SCHEMA, tracker.internalSchema().toSchema());
  }

  @Test
  public void testWildcardLenientWithSame() {
    ProjectionSchemaTracker tracker = trackerFor(
        RowSetTestUtils.projectAll());
    tracker.applyProvidedSchema(SCHEMA);
    assertTrue(tracker.isResolved());
    tracker.applyEarlyReaderSchema(SCHEMA);
    assertTrue(tracker.isResolved());
    assertEquals(SCHEMA, tracker.internalSchema().toSchema());
  }

  @Test
  public void testWildcardLenientWithSuperset() {
    ProjectionSchemaTracker tracker = trackerFor(
        RowSetTestUtils.projectAll());
    tracker.applyProvidedSchema(SCHEMA);
    assertTrue(tracker.isResolved());
    TupleMetadata readerSchema = new SchemaBuilder()
        .addAll(SCHEMA)
        .add("c", MinorType.VARCHAR)
        .build();
    tracker.applyEarlyReaderSchema(readerSchema);
    assertTrue(tracker.isResolved());
    assertEquals(readerSchema, tracker.internalSchema().toSchema());
  }

  @Test
  public void testWildcardStrictWithSuperset() {
    ProjectionSchemaTracker tracker = trackerFor(
        RowSetTestUtils.projectAll());
    TupleMetadata provided = SCHEMA.copy();
    SchemaUtils.markStrict(provided);
    tracker.applyProvidedSchema(provided);
    assertTrue(tracker.isResolved());

    TupleMetadata readerSchema = new SchemaBuilder()
        .addAll(SCHEMA)
        .add("c", MinorType.VARCHAR)
        .build();
    tracker.applyEarlyReaderSchema(readerSchema);
    assertTrue(tracker.isResolved());
    assertEquals(SCHEMA, tracker.internalSchema().toSchema());
  }

  @Test
  public void testTypeConflict() {
    ProjectionSchemaTracker tracker = trackerFor(
        RowSetTestUtils.projectAll());
    tracker.applyProvidedSchema(SCHEMA);
    assertTrue(tracker.isResolved());
    TupleMetadata readerSchema = new SchemaBuilder()
        .add("a", MinorType.BIGINT)
        .build();
    try {
      tracker.applyEarlyReaderSchema(readerSchema);
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("type conflict"));
      assertTrue(e.getMessage().contains("Scan column: `a` INT NOT NULL"));
      assertTrue(e.getMessage().contains("Reader column: `a` BIGINT NOT NULL"));
    }
  }

  @Test
  public void testModeConflict() {
    ProjectionSchemaTracker tracker = trackerFor(
        RowSetTestUtils.projectAll());
    tracker.applyProvidedSchema(SCHEMA);
    assertTrue(tracker.isResolved());
    TupleMetadata readerSchema = new SchemaBuilder()
        .addNullable("a", MinorType.INT)
        .build();
    try {
      tracker.applyEarlyReaderSchema(readerSchema);
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("type conflict"));
      assertTrue(e.getMessage().contains("Scan column: `a` INT NOT NULL"));
      assertTrue(e.getMessage().contains("Reader column: `a` INT"));
    }
  }

  @Test
  public void testWildcardWithExplicitWithEarlyReaderSchema() {

    // Simulate SELECT *, a ...
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectList(
            SchemaPath.DYNAMIC_STAR, "a"));

    final TupleMetadata readerSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.BIGINT)
        .add("c", MinorType.VARCHAR)
        .buildSchema();
    final ScanSchemaTracker schemaTracker = builder.build();
    schemaTracker.applyEarlyReaderSchema(readerSchema);
    assertTrue(schemaTracker.isResolved());
    assertEquals(readerSchema, schemaTracker.outputSchema());
  }
}
