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

import org.apache.drill.test.BaseTest;
import org.apache.drill.categories.EvfTest;
import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.common.exceptions.EmptyErrorContext;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.impl.scan.v3.schema.ScanSchemaTracker.ProjectionType;
import org.apache.drill.exec.physical.resultSet.impl.ProjectionFilter;
import org.apache.drill.exec.physical.rowSet.RowSetTestUtils;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.TupleSchema;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(EvfTest.class)
public class TestSchemaTrackerDefined extends BaseTest{
  private static final CustomErrorContext ERROR_CONTEXT = EmptyErrorContext.INSTANCE;

  private boolean isProjected(ProjectionFilter filter, ColumnMetadata col) {
    return filter.projection(col).isProjected;
  }

  /**
   * If a schema is defined, then the planner has combined the projection
   * list with schema information to produce the final output schema
   * at plan time.
   */
  @Test
  public void testDefinedSchema() {

    // Simulate SELECT a, b, c ...
    // With a plan-provided defined schema
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectList("a", "b", "c"));

    final TupleMetadata definedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.BIGINT)
        .add("c", MinorType.VARCHAR)
       .buildSchema();
    builder.definedSchema(definedSchema);

    final ScanSchemaTracker schemaTracker = builder.build();
    assertTrue(schemaTracker instanceof SchemaBasedTracker);
    assertTrue(schemaTracker.isResolved());
    assertSame(ProjectionType.SOME, schemaTracker.projectionType());

    // Reader input schema is fully defined
    TupleMetadata readerInputSchema = schemaTracker.readerInputSchema();
    assertEquals(definedSchema, readerInputSchema);

    // Pretend the reader dutifully provided two of the columns
    TupleMetadata readerOutputSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.BIGINT)
        .buildSchema();

    // Projection filter is schema-based
    ProjectionFilter filter = schemaTracker.projectionFilter(ERROR_CONTEXT);
    assertTrue(filter instanceof DynamicSchemaFilter);
    assertTrue(isProjected(filter, readerOutputSchema.metadata("a")));
    assertTrue(isProjected(filter, readerOutputSchema.metadata("b")));
    assertTrue(filter.isProjected("c"));
    assertFalse(filter.isProjected("d"));

    schemaTracker.applyReaderSchema(readerOutputSchema, ERROR_CONTEXT);

    // Reader schema is still fully resolved
    assertTrue(schemaTracker.isResolved());
    assertEquals(definedSchema, readerInputSchema);

    // Pretend a class fills in the the missing columns
    TupleMetadata missingColSchema = new SchemaBuilder()
        .add("c", MinorType.VARCHAR)
        .buildSchema();
    schemaTracker.applyReaderSchema(missingColSchema, ERROR_CONTEXT);
    assertEquals(definedSchema, readerInputSchema);

    // Final schema sent downstream
    final TupleMetadata outputSchema = schemaTracker.outputSchema();
    assertEquals(definedSchema, outputSchema);
  }

  /**
   * If a schema is defined, then the planner has combined the projection
   * list with schema information to produce the final output schema
   * at plan time. The planner might leave some columns as dynamic type:
   * the schema (like projection) says which columns are wanted. But, like
   * a full dynamic schema, the types may not be known.
   */
  @Test
  public void testDynamicDefinedSchema() {

    // Simulate SELECT a, b, c ...
    // With a plan-provided defined schema
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectList("a", "b", "c"));

    final TupleMetadata definedSchema = new SchemaBuilder()
        .addDynamic("a")
        .add("b", MinorType.BIGINT)
        .add("c", MinorType.VARCHAR)
       .buildSchema();
    builder.definedSchema(definedSchema);

    final ScanSchemaTracker schemaTracker = builder.build();
    assertTrue(schemaTracker instanceof ProjectionSchemaTracker);
    assertSame(ProjectionType.SOME, schemaTracker.projectionType());
    assertFalse(schemaTracker.isResolved());

    // Reader input schema is partially defined
    final TupleMetadata reader1InputSchema = schemaTracker.readerInputSchema();
    assertEquals(definedSchema, reader1InputSchema);

    ProjectionFilter filter = schemaTracker.projectionFilter(ERROR_CONTEXT);
    assertTrue(filter instanceof DynamicSchemaFilter);

    // Pretend the reader dutifully provided two of the columns
    TupleMetadata readerOutputSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.BIGINT)
        .buildSchema();
    schemaTracker.applyReaderSchema(readerOutputSchema, ERROR_CONTEXT);

    // Reader schema is now fully resolved
    assertTrue(schemaTracker.isResolved());
    final TupleMetadata reader2InputSchema = schemaTracker.readerInputSchema();
    TupleMetadata expected = new SchemaBuilder()
        .addAll(readerOutputSchema)
        .add("c", MinorType.VARCHAR)
        .build();
    assertEquals(expected, reader2InputSchema);

    // Pretend an class fills in the the missing columns
    TupleMetadata missingColSchema = new SchemaBuilder()
        .add("c", MinorType.VARCHAR)
        .buildSchema();
    schemaTracker.applyReaderSchema(missingColSchema, ERROR_CONTEXT);

    // Final schema sent downstream
    final TupleMetadata outputSchema = schemaTracker.outputSchema();
    assertEquals(expected, outputSchema);
  }

  /**
   * It is an error if the provided schema does not match the projection
   * list. (Actually, the projection list is redundant in this case. But,
   * since Drill does not actually support a defined schema yet, we have
   * to be a bit vague.
   */
  @Test
  public void testTooShortProjection() {

    // Simulate SELECT a, b ...
    // With a plan-provided defined schema
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectList("a", "b"));

    final TupleMetadata definedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.BIGINT)
        .add("c", MinorType.VARCHAR)
       .buildSchema();
    builder.definedSchema(definedSchema);

    try {
      builder.build();
      fail();
    } catch (Exception e) {
      // Expected
    }
  }

  @Test
  public void testTooLongProjection() {

    // Simulate SELECT a, b, c, d ...
    // With a plan-provided defined schema
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectList("a", "b", "c", "d"));

    final TupleMetadata definedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.BIGINT)
        .add("c", MinorType.VARCHAR)
       .buildSchema();
    builder.definedSchema(definedSchema);

    try {
      builder.build();
      fail();
    } catch (Exception e) {
      // Expected
    }
  }

  @Test
  public void testDisjointProjection() {

    // Simulate SELECT a, c, d ...
    // With a plan-provided defined schema
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectList("a", "c", "d"));

    final TupleMetadata definedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.BIGINT)
        .add("c", MinorType.VARCHAR)
       .buildSchema();
    builder.definedSchema(definedSchema);

    try {
      builder.build();
      fail();
    } catch (Exception e) {
      // Expected
    }
  }

  @Test
  public void testDefinedSchemaWildcard() {

    // Simulate SELECT * ...
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder();

    final TupleMetadata definedSchema = new SchemaBuilder()
        .addDynamic(SchemaPath.DYNAMIC_STAR)
       .buildSchema();
    builder.definedSchema(definedSchema);
    final ScanSchemaTracker schemaTracker = builder.build();
    assertTrue(schemaTracker instanceof ProjectionSchemaTracker);

    // At this point, the tracker acts just like a non-defined
    // schema.
  }

  @Test
  public void testEmptyProjectWithDefinedSchema() {

    // Simulate SELECT ...
    // That is, project nothing, as for COUNT(*)
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectNone());
    builder.definedSchema(new TupleSchema());
    final ScanSchemaTracker schemaTracker = builder.build();
    assertTrue(schemaTracker instanceof SchemaBasedTracker);
    assertTrue(schemaTracker.isResolved());
    assertSame(ProjectionType.NONE, schemaTracker.projectionType());
  }
}
