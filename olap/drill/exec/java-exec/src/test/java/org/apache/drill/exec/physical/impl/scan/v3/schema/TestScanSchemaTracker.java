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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.drill.categories.EvfTest;
import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.common.exceptions.EmptyErrorContext;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.physical.impl.scan.v3.schema.ScanSchemaTracker.ProjectionType;
import org.apache.drill.exec.physical.resultSet.impl.ProjectionFilter;
import org.apache.drill.exec.physical.rowSet.RowSetTestUtils;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.BaseTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test the scan operator schema tracker which computes the final
 * output schema from a variety of sources.
 */
@Category(EvfTest.class)
public class TestScanSchemaTracker extends BaseTest {
  private static final CustomErrorContext ERROR_CONTEXT = EmptyErrorContext.INSTANCE;

  /**
   * Basic test: select a set of columns (a, b, c) when the
   * data source has an early schema of (a, b).
   */
  @Test
  public void testBasics() {

    // Simulate SELECT a, b, c ...
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectList("a", "b", "c"));
    final ScanSchemaTracker schemaTracker = builder.build();

    assertSame(ProjectionType.SOME, schemaTracker.projectionType());
    assertFalse(schemaTracker.isResolved());
    final int initVersion = schemaTracker.schemaVersion();
    assertTrue(initVersion > 0);

    // Reader input schema is dynamic
    final TupleMetadata readerInputSchema = schemaTracker.readerInputSchema();
    final TupleMetadata expected = new SchemaBuilder()
        .addDynamic("a")
        .addDynamic("b")
        .addDynamic("c")
        .build();
    assertEquals(expected, readerInputSchema);

    // Pretend the reader discovered two of the columns.
    final TupleMetadata readerOutputSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.BIGINT)
        .buildSchema();

    // Projection filter is list-based
    ProjectionFilter filter = schemaTracker.projectionFilter(ERROR_CONTEXT);
    assertTrue(filter instanceof DynamicSchemaFilter);
    assertFalse(filter.isEmpty());
    assertTrue(filter.projection(readerOutputSchema.metadata("a")).isProjected);
    assertTrue(filter.projection(readerOutputSchema.metadata("b")).isProjected);
    assertTrue(filter.isProjected("c"));
    assertFalse(filter.isProjected("d"));
    assertFalse(filter.projection(MetadataUtils.newScalar(
        "d", Types.optional(MinorType.INT))).isProjected);

    schemaTracker.applyReaderSchema(readerOutputSchema, ERROR_CONTEXT);
    assertFalse(schemaTracker.isResolved());
    final int reader1Version = schemaTracker.schemaVersion();
    assertTrue(reader1Version > initVersion);

    // Make up a type for the missing column
    final TupleMetadata missingCols = schemaTracker.missingColumns(readerOutputSchema);
    assertEquals(1, missingCols.size());
    assertEquals(MinorType.LATE, missingCols.metadata("c").type());

    TupleMetadata missingColSchema = new SchemaBuilder()
        .add("c", MinorType.VARCHAR)
        .buildSchema();
    schemaTracker.resolveMissingCols(missingColSchema);
    assertTrue(schemaTracker.isResolved());
    final int missing1Version = schemaTracker.schemaVersion();
    assertTrue(missing1Version > reader1Version);

    // Second reader finds all columns
    TupleMetadata reader2OutputSchema = new SchemaBuilder()
        .addAll(readerOutputSchema)
        .addAll(missingColSchema)
        .build();
    schemaTracker.applyReaderSchema(reader2OutputSchema, ERROR_CONTEXT);
    assertEquals(missing1Version, schemaTracker.schemaVersion());

    // Third reader finds two columns, treats "c" as missing again.
    schemaTracker.resolveMissingCols(missingColSchema);

    // Final schema sent downstream
    final TupleMetadata outputSchema = schemaTracker.outputSchema();
    final TupleMetadata expectedOutput = new SchemaBuilder()
        .addAll(readerOutputSchema)
        .addAll(missingColSchema)
        .buildSchema();
    assertEquals(expectedOutput, outputSchema);
  }

  /**
   * Wildcard projection, schema change allowed.
   */
  @Test
  public void testWildcard() {

    // Simulate SELECT * ...
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectAll());
    final ScanSchemaTracker schemaTracker = builder.build();

    assertSame(ProjectionType.ALL, schemaTracker.projectionType());
    assertFalse(schemaTracker.isResolved());

    // Reader input schema is dynamic
    final TupleMetadata reader1InputSchema = schemaTracker.readerInputSchema();
    assertTrue(reader1InputSchema.isEmpty());

    ProjectionFilter filter1 = schemaTracker.projectionFilter(ERROR_CONTEXT);
    assertSame(ProjectionFilter.PROJECT_ALL, filter1);

    // Pretend the reader discovers two columns.
    final TupleMetadata reader1OutputSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.BIGINT)
        .buildSchema();

    schemaTracker.applyReaderSchema(reader1OutputSchema, ERROR_CONTEXT);
    assertTrue(schemaTracker.isResolved());

    // Schema sent downstream after first batch
    final TupleMetadata outputSchema1 = schemaTracker.outputSchema();
    assertEquals(reader1OutputSchema, outputSchema1);

    // Next reader schema is partially defined
    final TupleMetadata reader2InputSchema = schemaTracker.readerInputSchema();
    assertEquals(reader1OutputSchema, reader2InputSchema);

    ProjectionFilter filter2 = schemaTracker.projectionFilter(ERROR_CONTEXT);
    assertTrue(filter2 instanceof DynamicSchemaFilter);
    assertTrue(filter2.projection(reader1OutputSchema.metadata("a")).isProjected);
    assertTrue(filter2.projection(reader1OutputSchema.metadata("b")).isProjected);
    assertTrue(filter2.isProjected("c"));
    try {
      filter2.projection(MetadataUtils.newScalar("a", Types.required(MinorType.VARCHAR)));
      fail();
    } catch (UserException e) {
      // Expected;
    }

    // The next reader defines another column.
    // This triggers a schema change in output.
    final TupleMetadata reader2OutputSchema = new SchemaBuilder()
        .add("c", MinorType.VARCHAR)
        .buildSchema();
    schemaTracker.applyReaderSchema(reader2OutputSchema, ERROR_CONTEXT);

    // Schema sent downstream after second reader
    final TupleMetadata outputSchema2 = schemaTracker.outputSchema();
    final TupleMetadata expectedOutput = new SchemaBuilder()
        .addAll(reader1OutputSchema)
        .addAll(reader2OutputSchema)
        .buildSchema();
    assertEquals(expectedOutput, outputSchema2);
  }

  /**
   * The provided schema is a bit like a defined schema, but it is more
   * of an advisory. The provided schema is completely independent of the
   * project list: the user is responsible for choosing the project list
   * wisely.
   */
  @Test
  public void testProvidedSchema() {

    // Simulate SELECT a, b, c ...
    // With a plan-provided defined schema
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectList("a", "b", "c"));

    final TupleMetadata providedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.BIGINT)
       .buildSchema();
    builder.providedSchema(providedSchema);

    final ScanSchemaTracker schemaTracker = builder.build();
    assertSame(ProjectionType.SOME, schemaTracker.projectionType());
    assertFalse(schemaTracker.isResolved());

    // Reader input schema is partially defined
    TupleMetadata readerInputSchema = schemaTracker.readerInputSchema();
    final TupleMetadata expected = new SchemaBuilder()
       .addAll(providedSchema)
       .addDynamic("c")
       .buildSchema();
    assertEquals(expected, readerInputSchema);

    // Pretend the reader dutifully provided two of the columns
    TupleMetadata readerOutputSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("c", MinorType.VARCHAR)
        .buildSchema();

    schemaTracker.applyReaderSchema(readerOutputSchema, ERROR_CONTEXT);
    assertTrue(schemaTracker.isResolved());

    // Final schema sent downstream
    final TupleMetadata outputSchema = schemaTracker.outputSchema();
    final TupleMetadata expectedOutput = new SchemaBuilder()
        .addAll(providedSchema)
        .add("c", MinorType.VARCHAR)
        .buildSchema();
    assertEquals(expectedOutput, outputSchema);
  }

  @Test
  public void testProvidedSchemaWithWildcard() {

    // Simulate SELECT * ...
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder();

    final TupleMetadata providedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.BIGINT)
        .buildSchema();
    builder.providedSchema(providedSchema);
    final ScanSchemaTracker schemaTracker = builder.build();

    assertSame(ProjectionType.ALL, schemaTracker.projectionType());
    assertTrue(schemaTracker.isResolved());

    // Reader input schema is dynamic
    final TupleMetadata reader1InputSchema = schemaTracker.readerInputSchema();
    assertEquals(providedSchema, reader1InputSchema);

    ProjectionFilter filter1 = schemaTracker.projectionFilter(ERROR_CONTEXT);
    assertTrue(filter1 instanceof DynamicSchemaFilter);

    // Pretend the reader discovers two columns.
    final TupleMetadata reader1OutputSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("c", MinorType.VARCHAR)
        .buildSchema();
    assertTrue(filter1.projection(reader1OutputSchema.metadata("a")).isProjected);
    assertTrue(filter1.projection(reader1OutputSchema.metadata("c")).isProjected);
    assertTrue(filter1.isProjected("d"));

    schemaTracker.applyReaderSchema(reader1OutputSchema, ERROR_CONTEXT);
    assertTrue(schemaTracker.isResolved());

    // Pretend something fills in the the missing columns
    TupleMetadata missingColSchema = new SchemaBuilder()
        .add("b", MinorType.BIGINT)
        .buildSchema();
    schemaTracker.applyReaderSchema(missingColSchema, ERROR_CONTEXT);

    // Schema sent downstream after first batch
    final TupleMetadata outputSchema = schemaTracker.outputSchema();
    final TupleMetadata expected = new SchemaBuilder()
        .addAll(providedSchema)
        .add(reader1OutputSchema.metadata("c"))
        .buildSchema();
    assertEquals(expected, outputSchema);
  }

  /**
   * Test for a query of the form:<br>
   * {code SELECT * FROM t ORDER BY a}<br>
   * in which we get a projection list of the form<br<
   * {@code [`**`, `a`]<br>
   * If we are given a provided schema of {@code (a, b, c)},
   * the "natural" expansion will be @{code (b, c, a)}, but we
   * add a hack to get what the user expects: @{code (a, b, c)}.
   * The "natural" expansion occurs because the projection list says
   * "all all columns to the wildcard position except those mentioned
   * elsewhere". We essentially redefine it as "add or move all columns
   * in provided schema order."
   */
  @Test
  public void testWildcardWithExplicitWithProvided() {

    // Simulate SELECT *, a ...
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectList(
            SchemaPath.DYNAMIC_STAR, "a"));

    final TupleMetadata providedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.BIGINT)
        .add("c", MinorType.VARCHAR)
        .buildSchema();
    builder.providedSchema(providedSchema);
    final ScanSchemaTracker schemaTracker = builder.build();
    assertTrue(schemaTracker.isResolved());
    assertEquals(providedSchema, schemaTracker.outputSchema());
  }

  @Test
  public void testStrictProvidedSchemaWithWildcard() {

    // Simulate SELECT * ...
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder();

    final TupleMetadata providedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.BIGINT)
        .buildSchema();
    providedSchema.setBooleanProperty(TupleMetadata.IS_STRICT_SCHEMA_PROP, true);
    builder.providedSchema(providedSchema);
    final ScanSchemaTracker schemaTracker = builder.build();

    // Even though the project list is a wildcard, the presence
    // of a strict provided schema makes it fully defined
    assertSame(ProjectionType.SOME, schemaTracker.projectionType());
    assertTrue(schemaTracker.isResolved());

    // Reader input schema is fixed
    final TupleMetadata reader1InputSchema = schemaTracker.readerInputSchema();
    assertEquals(providedSchema, reader1InputSchema);

    ProjectionFilter filter1 = schemaTracker.projectionFilter(ERROR_CONTEXT);
    assertTrue(filter1 instanceof DynamicSchemaFilter);

    // Pretend the reader discovers two columns.
    final TupleMetadata reader1OutputSchema = new SchemaBuilder()
        .addAll(providedSchema)
        .buildSchema();
    assertTrue(filter1.projection(reader1OutputSchema.metadata("a")).isProjected);
    assertFalse(filter1.isProjected("c"));

    schemaTracker.applyReaderSchema(reader1OutputSchema, ERROR_CONTEXT);
    assertTrue(schemaTracker.isResolved());

    // Schema sent downstream after first batch
    final TupleMetadata outputSchema = schemaTracker.outputSchema();
    assertEquals(providedSchema, outputSchema);
  }

  /**
   * Wildcard projection, schema change not allowed; first batch
   * defines the schema and projection for later readers.
   */
  @Test
  public void testWildcardWithoutSchemaChange() {

    // Simulate SELECT * ...
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectAll());
    builder.allowSchemaChange(false);
    final ScanSchemaTracker schemaTracker = builder.build();

    assertSame(ProjectionType.ALL, schemaTracker.projectionType());
    assertFalse(schemaTracker.isResolved());

    // Reader input schema is dynamic
    ProjectionFilter filter1 = schemaTracker.projectionFilter(ERROR_CONTEXT);
    assertSame(ProjectionFilter.PROJECT_ALL, filter1);
    assertTrue(filter1.isProjected("z"));

    TupleMetadata readerInputSchema = schemaTracker.readerInputSchema();
    assertTrue(readerInputSchema.isEmpty());

    // Pretend the reader discovered two columns.
    final TupleMetadata reader1OutputSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.BIGINT)
        .buildSchema();

    schemaTracker.applyReaderSchema(reader1OutputSchema, ERROR_CONTEXT);
    assertTrue(schemaTracker.isResolved());

    // Next reader schema is completely defined
    assertSame(ProjectionType.SOME, schemaTracker.projectionType());
    readerInputSchema = schemaTracker.readerInputSchema();
    assertEquals(reader1OutputSchema, readerInputSchema);

    // Projection list changes to what we've seen.
    ProjectionFilter filter2 = schemaTracker.projectionFilter(ERROR_CONTEXT);
    assertTrue(filter2 instanceof DynamicSchemaFilter);
    assertTrue(filter2.isProjected("a"));
    assertTrue(filter2.isProjected("b"));
    assertFalse(filter2.isProjected("c"));

    schemaTracker.applyReaderSchema(reader1OutputSchema, ERROR_CONTEXT);
    // Schema sent downstream after first batch
    TupleMetadata outputSchema = schemaTracker.outputSchema();
    assertEquals(reader1OutputSchema, outputSchema);

    // The next reader defines another column: "c". The projection filter ignores
    // the column.

    // The schema tracker ignores this column.
    final TupleMetadata reader2OutputSchema = new SchemaBuilder()
        .buildSchema();
    schemaTracker.applyReaderSchema(reader2OutputSchema, ERROR_CONTEXT);

    // If the reader were to bypass the projection filter and define the
    // column anyway, the query will fail because the scan framework does
    // not know what to do with the unwanted (materialized) column.
    final TupleMetadata reader3OutputSchema = new SchemaBuilder()
        .add("c", MinorType.VARCHAR)
        .buildSchema();
    try {
      schemaTracker.applyReaderSchema(reader3OutputSchema, ERROR_CONTEXT);
      fail();
    } catch (IllegalStateException e) {
      // Expected;
    }

    // Schema sent downstream after second reader
    outputSchema = schemaTracker.outputSchema();
    assertEquals(reader1OutputSchema, outputSchema);
  }

  @Test
  public void testEmptyProject() {

    // Simulate SELECT ...
    // That is, project nothing, as for COUNT(*)
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectNone());
    doTestEmptyProject(builder);
  }

  @Test
  public void testEmptyProjectWithProvidedSchema() {

    // Simulate SELECT ...
    // That is, project nothing, as for COUNT(*)
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectNone());
    final TupleMetadata providedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.BIGINT)
       .buildSchema();
    builder.providedSchema(providedSchema);
    doTestEmptyProject(builder);
  }

  private void doTestEmptyProject(ScanSchemaConfigBuilder builder) {
    final ScanSchemaTracker schemaTracker = builder.build();

    assertSame(ProjectionType.NONE, schemaTracker.projectionType());
    assertTrue(schemaTracker.isResolved());

    // Sanity check of projection; detailed testing done elsewhere
    ProjectionFilter filter1 = schemaTracker.projectionFilter(ERROR_CONTEXT);
    assertSame(ProjectionFilter.PROJECT_NONE, filter1);
    assertFalse(filter1.isProjected("a"));

    // Reader input schema is empty
    TupleMetadata readerInputSchema = schemaTracker.readerInputSchema();
    assertTrue(readerInputSchema.isEmpty());

    // Projection is empty
    ProjectionFilter filter = schemaTracker.projectionFilter(ERROR_CONTEXT);
    assertTrue(filter.isEmpty());
    assertFalse(filter.isProjected("a"));

    // Reader produces a empty schema
    final TupleMetadata readerOutputSchema = new SchemaBuilder()
        .buildSchema();

    schemaTracker.applyReaderSchema(readerOutputSchema, ERROR_CONTEXT);
    assertTrue(schemaTracker.isResolved());

    // Schema sent downstream after first batch
    final TupleMetadata outputSchema = schemaTracker.outputSchema();
    assertTrue(outputSchema.isEmpty());
  }

  /**
   * Test for a query of the form:<br>
   * {code SELECT * FROM t ORDER BY a}<br>
   * in which we get a projection list of the form<br<
   * {@code [`**`, `a`]<br>
   * If we are given a reader schema of {@code (a, b, c)},
   * the "natural" expansion will be @{code (b, c, a)}, but we
   * add a hack to get what the user expects: @{code (a, b, c)}.
   * The "natural" expansion occurs because the projection list says
   * "all all columns to the wildcard position except those mentioned
   * elsewhere". We essentially redefine it as "add or move all columns
   * in provided schema order."
   */
  @Test
  public void testWildcardWithExplicitWithReaderSchema() {

    // Simulate SELECT *, a ...
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectList(
            SchemaPath.DYNAMIC_STAR, "a"));

    final TupleMetadata readerOutputSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.BIGINT)
        .add("c", MinorType.VARCHAR)
        .buildSchema();
    final ScanSchemaTracker schemaTracker = builder.build();
    schemaTracker.applyReaderSchema(readerOutputSchema, ERROR_CONTEXT);
    assertTrue(schemaTracker.isResolved());
    assertEquals(readerOutputSchema, schemaTracker.outputSchema());
  }

  @Test
  public void testWildcardWithExplicitWithProvidedAndReaderSchema() {

    // Simulate SELECT *, a ...
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectList(
            SchemaPath.DYNAMIC_STAR, "a"));

    final TupleMetadata providedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.BIGINT)
        .add("c", MinorType.VARCHAR)
        .buildSchema();
    builder.providedSchema(providedSchema);
    final ScanSchemaTracker schemaTracker = builder.build();
    schemaTracker.applyReaderSchema(providedSchema, ERROR_CONTEXT);
    assertTrue(schemaTracker.isResolved());
    assertEquals(providedSchema, schemaTracker.outputSchema());
  }

}
