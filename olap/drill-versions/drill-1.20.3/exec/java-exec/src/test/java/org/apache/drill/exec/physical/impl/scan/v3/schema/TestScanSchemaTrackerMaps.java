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
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.resultSet.impl.ProjectionFilter;
import org.apache.drill.exec.physical.resultSet.impl.ProjectionFilter.ProjResult;
import org.apache.drill.exec.physical.rowSet.RowSetTestUtils;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.BaseTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Maps add considerable complexity to the scan schema tracker, turning
 * a list of columns into a tree. The projection list, provided schema,
 * defined schema, scan schema, reader schema and missing columns schemas
 * must all be trees, and must all be kept in sync.
 */
@Category(EvfTest.class)
public class TestScanSchemaTrackerMaps extends BaseTest {
  private static final CustomErrorContext ERROR_CONTEXT = EmptyErrorContext.INSTANCE;

  private boolean isProjected(ProjectionFilter filter, ColumnMetadata col) {
    return filter.projection(col).isProjected;
  }

  private ProjResult mapProjection(ProjectionFilter filter, String mapName) {
    return filter.projection(MetadataUtils.newMap(mapName));
  }

  /**
   * Test a map projected with just the map name: {@code `m`}.
   */
  @Test
  public void testGenericMap() {
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectList("a", "m"));
    doTestGenericMap(builder, true);
  }

  /**
   * Test a map projected with the map name and a specific
   * column: {@code `m`.`x`, `m`}. The map is generic: the entire
   * map is projected.
   */
  @Test
  public void testGenericAndSpecificMap() {
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectList("a", "m.x", "m"));
    doTestGenericMap(builder, false);
  }

  private void doTestGenericMap(ScanSchemaConfigBuilder builder, boolean isAll) {
    final ScanSchemaTracker schemaTracker = builder.build();

    // Pretend the reader discovers that m is a map.
    TupleMetadata reader1InputSchema = schemaTracker.readerInputSchema();
    assertEquals(2, reader1InputSchema.size());

    final TupleMetadata readerOutputSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("x", MinorType.BIGINT)
          .add("y", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    {
      ProjectionFilter filter = schemaTracker.projectionFilter(ERROR_CONTEXT);
      assertTrue(filter instanceof DynamicSchemaFilter);
      assertTrue(isProjected(filter, readerOutputSchema.metadata("a")));
      assertTrue(isProjected(filter, readerOutputSchema.metadata("m")));
      assertFalse(filter.isProjected("b"));

      final ColumnMetadata mapCol = readerOutputSchema.metadata("m");
      ProjResult mapResult = filter.projection(mapCol);
      assertTrue(mapResult.isProjected);
      ProjectionFilter mapFilter = mapResult.mapFilter;
      if (isAll) {
        assertSame(ProjectionFilter.PROJECT_ALL, mapFilter);
      } else {
        assertTrue(mapFilter instanceof DynamicSchemaFilter);
      }
      final TupleMetadata mapSchema = mapCol.tupleSchema();
      assertTrue(isProjected(mapFilter, mapSchema.metadata("x")));
      assertTrue(isProjected(mapFilter, mapSchema.metadata("y")));
      assertTrue(mapFilter.isProjected("z"));

      assertSame(ProjectionFilter.PROJECT_ALL,
          mapProjection(mapFilter, "w").mapFilter);
    }

    schemaTracker.applyReaderSchema(readerOutputSchema, ERROR_CONTEXT);

    // Scan output schema is now resolved.
    assertTrue(schemaTracker.isResolved());
    final TupleMetadata outputSchema = schemaTracker.outputSchema();
    assertEquals(readerOutputSchema, outputSchema);

    // A second reader gets a strict filter for the row, but a
    // project-all filter for the map.
    TupleMetadata reader2InputSchema = schemaTracker.readerInputSchema();
    assertEquals(outputSchema, reader2InputSchema);

    {
      final ProjectionFilter filter = schemaTracker.projectionFilter(ERROR_CONTEXT);
      assertTrue(filter instanceof DynamicSchemaFilter);
      assertTrue(isProjected(filter, readerOutputSchema.metadata("a")));
      assertTrue(isProjected(filter, readerOutputSchema.metadata("m")));
      assertFalse(filter.isProjected("b"));

      final TupleMetadata mapSchema = readerOutputSchema.metadata("m").tupleSchema();
      ProjResult mapResult = filter.projection(readerOutputSchema.metadata("m"));
      assertTrue(mapResult.isProjected);
      ProjectionFilter mapFilter = mapResult.mapFilter;
      assertTrue(mapFilter instanceof DynamicSchemaFilter);
      assertTrue(isProjected(mapFilter, mapSchema.metadata("x")));
      assertTrue(isProjected(mapFilter, mapSchema.metadata("y")));
      assertTrue(mapFilter.isProjected("z"));

      assertSame(ProjectionFilter.PROJECT_ALL, mapFilter.projection(
          MetadataUtils.newMap("w")).mapFilter);
    }
  }

  @Test
  public void testTwoLevelGenericMap() {
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectList("a", "m"));
    final ScanSchemaTracker schemaTracker = builder.build();

    // Pretend the reader discovers that m is a map.
    final TupleMetadata reader1InputSchema = schemaTracker.readerInputSchema();
    assertEquals(2, reader1InputSchema.size());

    final TupleMetadata readerOutputSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("x", MinorType.BIGINT)
          .add("y", MinorType.VARCHAR)
          .addMap("m2")
            .add("p", MinorType.BIGINT)
            .add("q", MinorType.VARCHAR)
            .resumeMap()
          .resumeSchema()
        .buildSchema();

    {
      final ProjectionFilter filter = schemaTracker.projectionFilter(ERROR_CONTEXT);

      ProjResult result = filter.projection(MetadataUtils.newMap("m"));
      assertTrue(result.isProjected);
      final ProjectionFilter map1Filter = result.mapFilter;
      assertSame(ProjectionFilter.PROJECT_ALL, map1Filter);

      result = map1Filter.projection(MetadataUtils.newMap("m2"));
      assertTrue(result.isProjected);
      assertSame(ProjectionFilter.PROJECT_ALL, result.mapFilter);
    }

    schemaTracker.applyReaderSchema(readerOutputSchema, ERROR_CONTEXT);

    // Scan output schema is now resolved.
    assertTrue(schemaTracker.isResolved());
    assertEquals(readerOutputSchema, schemaTracker.outputSchema());

    // A second reader gets a strict filter for the row, but a
    // project-all filter for the map.
    TupleMetadata reader2InputSchema = schemaTracker.readerInputSchema();
    assertEquals(readerOutputSchema, reader2InputSchema);

    {
      final ProjectionFilter filter = schemaTracker.projectionFilter(ERROR_CONTEXT);
      assertTrue(filter instanceof DynamicSchemaFilter);

      final ProjectionFilter map1Filter = mapProjection(filter, "m").mapFilter;
      assertTrue(map1Filter instanceof DynamicSchemaFilter);
      assertTrue(map1Filter.isProjected("z"));

      final ProjectionFilter map2Filter = mapProjection(map1Filter, "m2").mapFilter;
      assertTrue(map2Filter instanceof DynamicSchemaFilter);
      assertTrue(map2Filter.isProjected("r"));
    }

    final TupleMetadata reader2OutputSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("x", MinorType.BIGINT)
          .add("y", MinorType.VARCHAR)
          .addMap("m2")
            .add("p", MinorType.BIGINT)
            .add("q", MinorType.VARCHAR)
            .add("r", MinorType.INT)
            .resumeMap()
          .add("z", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();
    schemaTracker.applyReaderSchema(reader2OutputSchema, ERROR_CONTEXT);
    assertEquals(reader2OutputSchema, schemaTracker.outputSchema());
  }

  @Test
  public void testMapWithWildcard() {
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder();
    final ScanSchemaTracker schemaTracker = builder.build();
    assertFalse(schemaTracker.isResolved());

    // Pretend the reader discovers that m is a map.
    TupleMetadata reader1InputSchema = schemaTracker.readerInputSchema();
    assertTrue(reader1InputSchema.isEmpty());

    final TupleMetadata readerOutputSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("x", MinorType.BIGINT)
          .add("y", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    {
      final ProjectionFilter filter = schemaTracker.projectionFilter(ERROR_CONTEXT);
      assertSame(ProjectionFilter.PROJECT_ALL, filter);
      assertFalse(filter.isEmpty());
    }

    schemaTracker.applyReaderSchema(readerOutputSchema, ERROR_CONTEXT);

    // Scan output schema is now resolved.
    assertTrue(schemaTracker.isResolved());
    final TupleMetadata outputSchema = schemaTracker.outputSchema();
    assertEquals(readerOutputSchema, outputSchema);

    // A second reader gets a strict filter for the row, but a
    // project-all filter for the map.
    TupleMetadata reader2InputSchema = schemaTracker.readerInputSchema();
    assertEquals(outputSchema, reader2InputSchema);

    {
      final ProjectionFilter filter = schemaTracker.projectionFilter(ERROR_CONTEXT);
      assertTrue(filter instanceof DynamicSchemaFilter);
      assertFalse(filter.isEmpty());
      assertTrue(isProjected(filter, readerOutputSchema.metadata("a")));
      assertTrue(isProjected(filter, readerOutputSchema.metadata("m")));
      assertTrue(filter.isProjected("b"));

      assertSame(ProjectionFilter.PROJECT_ALL, mapProjection(filter, "w").mapFilter);

      final ColumnMetadata mapCol = readerOutputSchema.metadata("m");
      final ProjectionFilter mapFilter = filter.projection(mapCol).mapFilter;
      assertTrue(mapFilter instanceof DynamicSchemaFilter);
      final TupleMetadata mapSchema = mapCol.tupleSchema();
      assertTrue(isProjected(mapFilter, mapSchema.metadata("x")));
      assertTrue(isProjected(mapFilter, mapSchema.metadata("y")));
      assertTrue(mapFilter.isProjected("z"));

      assertSame(ProjectionFilter.PROJECT_ALL,
          mapProjection(mapFilter, "w").mapFilter);
    }
  }

  @Test
  public void testMapWithWildcardNoSchemaChange() {
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder();
    builder.allowSchemaChange(false);
    final ScanSchemaTracker schemaTracker = builder.build();
    assertFalse(schemaTracker.isResolved());

    // Pretend the reader discovers that m is a map.
    TupleMetadata reader1InputSchema = schemaTracker.readerInputSchema();
    assertTrue(reader1InputSchema.isEmpty());

    final TupleMetadata readerOutputSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("x", MinorType.BIGINT)
          .add("y", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    {
      final ProjectionFilter filter = schemaTracker.projectionFilter(ERROR_CONTEXT);
      assertSame(ProjectionFilter.PROJECT_ALL, filter);
      assertFalse(filter.isEmpty());
    }

    schemaTracker.applyReaderSchema(readerOutputSchema, ERROR_CONTEXT);

    // Scan output schema is now resolved.
    assertTrue(schemaTracker.isResolved());
    assertEquals(readerOutputSchema, schemaTracker.outputSchema());

    // A second reader gets a strict filter for the row, but a
    // project-all filter for the map.
    TupleMetadata reader2InputSchema = schemaTracker.readerInputSchema();
    assertEquals(readerOutputSchema, reader2InputSchema);

    {
      final ProjectionFilter filter = schemaTracker.projectionFilter(ERROR_CONTEXT);
      assertTrue(filter instanceof DynamicSchemaFilter);
      assertFalse(filter.isEmpty());
      assertTrue(isProjected(filter, readerOutputSchema.metadata("a")));
      assertTrue(isProjected(filter, readerOutputSchema.metadata("m")));
      assertFalse(filter.isProjected("b"));

      assertSame(ProjectionFilter.PROJECT_NONE,
          mapProjection(filter, "w").mapFilter);

      final ColumnMetadata mapCol = readerOutputSchema.metadata("m");
      final ProjectionFilter mapFilter = filter.projection(mapCol).mapFilter;
      assertTrue(mapFilter instanceof DynamicSchemaFilter);
      final TupleMetadata mapSchema = mapCol.tupleSchema();
      assertTrue(isProjected(mapFilter, mapSchema.metadata("x")));
      assertTrue(isProjected(mapFilter, mapSchema.metadata("y")));
      assertFalse(mapFilter.isProjected("z"));

      assertSame(ProjectionFilter.PROJECT_NONE,
          mapProjection(mapFilter, "w").mapFilter);
    }
  }

  /**
   * Test a specific map column, {@code `m`.`x`}, with no schema.
   */
  @Test
  public void testSpecificMap() {
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectList("a", "m.x"));
    final ScanSchemaTracker schemaTracker = builder.build();

    // Pretend the reader discovers that m is a map.
    TupleMetadata reader1InputSchema = schemaTracker.readerInputSchema();
    assertEquals(2, reader1InputSchema.size());

    final TupleMetadata readerOutputSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("x", MinorType.BIGINT)
          .resumeSchema()
        .buildSchema();

    {
      ProjectionFilter filter = schemaTracker.projectionFilter(ERROR_CONTEXT);
      assertTrue(filter instanceof DynamicSchemaFilter);
      assertTrue(isProjected(filter, readerOutputSchema.metadata("a")));
      assertTrue(isProjected(filter, readerOutputSchema.metadata("m")));
      assertFalse(filter.isProjected("b"));

      final ColumnMetadata mapCol = readerOutputSchema.metadata("m");
      ProjResult mapResult = filter.projection(mapCol);
      assertTrue(mapResult.isProjected);
      ProjectionFilter mapFilter = mapResult.mapFilter;
      assertTrue(mapFilter instanceof DynamicSchemaFilter);
      final TupleMetadata mapSchema = readerOutputSchema.metadata("m").tupleSchema();
      assertTrue(isProjected(mapFilter, mapSchema.metadata("x")));
      assertFalse(mapFilter.isProjected("y"));

      assertSame(ProjectionFilter.PROJECT_NONE,
          mapProjection(mapFilter, "w").mapFilter);
    }

    schemaTracker.applyReaderSchema(readerOutputSchema, ERROR_CONTEXT);

    // Scan output schema is now resolved.
    assertTrue(schemaTracker.isResolved());
    final TupleMetadata outputSchema = schemaTracker.outputSchema();
    assertEquals(readerOutputSchema, outputSchema);

    // A second reader gets a strict filter for the row, but a
    // project-all filter for the map.
    TupleMetadata reader2InputSchema = schemaTracker.readerInputSchema();
    assertEquals(outputSchema, reader2InputSchema);

    {
      ProjectionFilter filter = schemaTracker.projectionFilter(ERROR_CONTEXT);
      assertTrue(filter instanceof DynamicSchemaFilter);
      assertTrue(isProjected(filter, readerOutputSchema.metadata("a")));
      assertTrue(isProjected(filter, readerOutputSchema.metadata("m")));
      assertFalse(filter.isProjected("b"));

      final ColumnMetadata mapCol = readerOutputSchema.metadata("m");
      ProjResult mapResult = filter.projection(mapCol);
      assertTrue(mapResult.isProjected);
      ProjectionFilter mapFilter = mapResult.mapFilter;
      assertTrue(mapFilter instanceof DynamicSchemaFilter);
      final TupleMetadata mapSchema = readerOutputSchema.metadata("m").tupleSchema();
      assertTrue(isProjected(mapFilter, mapSchema.metadata("x")));
      assertFalse(mapFilter.isProjected("y"));

      assertSame(ProjectionFilter.PROJECT_NONE,
          mapProjection(mapFilter, "w").mapFilter);
    }
  }

  @Test
  public void testSpecificMapSubset() {
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectList("a", "m.x", "m.y"));
    final ScanSchemaTracker schemaTracker = builder.build();

    // Pretend the reader discovers that m is a map.
    TupleMetadata reader1InputSchema = schemaTracker.readerInputSchema();
    assertEquals(2, reader1InputSchema.size());

    final TupleMetadata readerOutputSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("x", MinorType.BIGINT)
          .resumeSchema()
        .buildSchema();

    {
      final ProjectionFilter filter = schemaTracker.projectionFilter(ERROR_CONTEXT);
      assertTrue(filter instanceof DynamicSchemaFilter);

      final ColumnMetadata mapCol = readerOutputSchema.metadata("m");
      final ProjectionFilter mapFilter = filter.projection(mapCol).mapFilter;
      assertTrue(mapFilter instanceof DynamicSchemaFilter);
    }

    schemaTracker.applyReaderSchema(readerOutputSchema, ERROR_CONTEXT);

    // Scan output schema is not yet resolved.
    assertFalse(schemaTracker.isResolved());

    // Missing columns provided
    final TupleMetadata missingCols = schemaTracker.missingColumns(readerOutputSchema);
    final TupleMetadata expectedMissingCols = new SchemaBuilder()
        .addMap("m")
          .addDynamic("y")
          .resumeSchema()
        .buildSchema();
    assertEquals(expectedMissingCols, missingCols);

    final TupleMetadata missingColsOutput = new SchemaBuilder()
        .addMap("m")
          .addNullable("y", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();
    schemaTracker.resolveMissingCols(missingColsOutput);

    // Schema is now resolved
    assertTrue(schemaTracker.isResolved());

    final TupleMetadata expected = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("x", MinorType.BIGINT)
          .addNullable("y", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();
    assertEquals(expected, schemaTracker.outputSchema());
  }

  /**
   * Test a generic map with a lenient provided schema. The schema
   * defines those columns which do exist, but allow other map members
   * to be added.
   */
  @Test
  public void testGenericMapWithLenientProvidedSchema() {
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectList("a", "m"));
    final TupleMetadata providedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("x", MinorType.BIGINT)
          .add("y", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();
    builder.providedSchema(providedSchema);
    final ScanSchemaTracker schemaTracker = builder.build();
    assertTrue(schemaTracker.isResolved());

    // Pretend the reader reads one of the map columns and discovers a new one.
    TupleMetadata reader1InputSchema = schemaTracker.readerInputSchema();
    assertEquals(2, reader1InputSchema.size());

    final TupleMetadata readerOutputSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("x", MinorType.BIGINT)
          .add("z", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    {
      ProjectionFilter filter = schemaTracker.projectionFilter(ERROR_CONTEXT);
      assertTrue(filter instanceof DynamicSchemaFilter);
      assertTrue(isProjected(filter, readerOutputSchema.metadata("a")));
      assertTrue(isProjected(filter, readerOutputSchema.metadata("m")));
      assertFalse(filter.isProjected("b"));

      final ColumnMetadata mapCol = readerOutputSchema.metadata("m");
      ProjectionFilter mapFilter = filter.projection(mapCol).mapFilter;
      assertTrue(mapFilter instanceof DynamicSchemaFilter);
      final TupleMetadata mapSchema = mapCol.tupleSchema();
      assertTrue(isProjected(mapFilter, mapSchema.metadata("x")));
      assertTrue(mapFilter.isProjected("y"));
      assertTrue(isProjected(mapFilter, mapSchema.metadata("z")));

      assertSame(ProjectionFilter.PROJECT_ALL,
          mapProjection(mapFilter, "w").mapFilter);
    }

    schemaTracker.applyReaderSchema(readerOutputSchema, ERROR_CONTEXT);

    // Scan output schema is now resolved.
    assertTrue(schemaTracker.isResolved());
    final TupleMetadata expectedOutput = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("x", MinorType.BIGINT)
          .add("y", MinorType.VARCHAR)
          .add("z", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();
    assertEquals(expectedOutput, schemaTracker.outputSchema());

    // A second reader gets a strict filter for the row, but a
    // project-all filter for the map.
    TupleMetadata reader2InputSchema = schemaTracker.readerInputSchema();
    assertEquals(expectedOutput, reader2InputSchema);

    {
      ProjectionFilter filter = schemaTracker.projectionFilter(ERROR_CONTEXT);
      assertTrue(filter instanceof DynamicSchemaFilter);
      assertTrue(isProjected(filter, readerOutputSchema.metadata("a")));
      assertTrue(isProjected(filter, readerOutputSchema.metadata("m")));
      assertFalse(filter.isProjected("b"));

      final ColumnMetadata mapCol = readerOutputSchema.metadata("m");
      ProjectionFilter mapFilter = filter.projection(mapCol).mapFilter;
      assertTrue(mapFilter instanceof DynamicSchemaFilter);
      final TupleMetadata mapSchema = mapCol.tupleSchema();
      assertTrue(isProjected(mapFilter, mapSchema.metadata("x")));
      assertTrue(mapFilter.isProjected("w"));

      assertSame(ProjectionFilter.PROJECT_ALL,
          mapProjection(mapFilter, "w").mapFilter);
    }
  }

  /**
   * Test a generic map with a strict provided schema. The schema
   * defines those columns which exist, and forbids adding other map
   * members.
   */
  @Test
  public void testGenericMapWithStrictProvidedSchema() {
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectList("a", "m"));
    final TupleMetadata providedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("x", MinorType.BIGINT)
          .add("y", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();
    providedSchema.setBooleanProperty(TupleMetadata.IS_STRICT_SCHEMA_PROP, true);
    builder.providedSchema(providedSchema);
    doTestMapDefinedSchema(builder, providedSchema);
  }

  /**
   * Test a generic map with a defined schema. The schema
   * exactly defines the map. Since the map projection is generic,
   * it matches whatever columns the defined schema defines.
   */
  @Test
  public void testGenericMapWithDefinedSchema() {
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectList("a", "m"));
    final TupleMetadata definedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("x", MinorType.BIGINT)
          .add("y", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();
    builder.definedSchema(definedSchema);
    doTestMapDefinedSchema(builder, definedSchema);
  }

  private void doTestMapDefinedSchema(ScanSchemaConfigBuilder builder, TupleMetadata targetSchema) {
    final ScanSchemaTracker schemaTracker = builder.build();
    assertTrue(schemaTracker.isResolved());

    // Pretend the reader reads one of the map columns and discovers a new one.
    TupleMetadata reader1InputSchema = schemaTracker.readerInputSchema();
    assertEquals(targetSchema, reader1InputSchema);

    final TupleMetadata readerOutputSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("x", MinorType.BIGINT)
          .resumeSchema()
        .buildSchema();

    {
      ProjectionFilter filter = schemaTracker.projectionFilter(ERROR_CONTEXT);
      assertTrue(filter instanceof DynamicSchemaFilter);
      assertFalse(filter.isEmpty());
      assertTrue(isProjected(filter, readerOutputSchema.metadata("a")));
      assertTrue(isProjected(filter, readerOutputSchema.metadata("m")));
      assertFalse(filter.isProjected("b"));

      assertSame(ProjectionFilter.PROJECT_NONE,
          mapProjection(filter, "w").mapFilter);

      final ColumnMetadata mapCol = readerOutputSchema.metadata("m");
      final ProjectionFilter mapFilter = filter.projection(mapCol).mapFilter;
      assertTrue(mapFilter instanceof DynamicSchemaFilter);
      final TupleMetadata mapSchema = mapCol.tupleSchema();
      assertTrue(isProjected(mapFilter, mapSchema.metadata("x")));
      assertTrue(mapFilter.isProjected("y"));
      assertFalse(mapFilter.isProjected("z"));

      assertSame(ProjectionFilter.PROJECT_NONE,
          mapProjection(mapFilter, "w").mapFilter);
    }

    schemaTracker.applyReaderSchema(readerOutputSchema, ERROR_CONTEXT);

    // Scan output schema is now resolved.
    assertTrue(schemaTracker.isResolved());
    assertEquals(targetSchema, schemaTracker.outputSchema());

    // A second reader gets a strict filter for the row and map.
    TupleMetadata reader2InputSchema = schemaTracker.readerInputSchema();
    assertEquals(targetSchema, reader2InputSchema);

    {
      ProjectionFilter filter = schemaTracker.projectionFilter(ERROR_CONTEXT);
      assertTrue(filter instanceof DynamicSchemaFilter);
      assertTrue(isProjected(filter, readerOutputSchema.metadata("a")));
      assertTrue(isProjected(filter, readerOutputSchema.metadata("m")));
      assertFalse(filter.isProjected("b"));

      final ColumnMetadata mapCol = readerOutputSchema.metadata("m");
      final ProjectionFilter mapFilter = filter.projection(mapCol).mapFilter;
      assertTrue(mapFilter instanceof DynamicSchemaFilter);
      final TupleMetadata mapSchema = mapCol.tupleSchema();
      assertTrue(isProjected(mapFilter, mapSchema.metadata("x")));
      assertFalse(mapFilter.isProjected("w"));

      assertSame(ProjectionFilter.PROJECT_NONE,
          mapProjection(mapFilter, "w").mapFilter);
    }
  }

  /**
   * Test a specific map column ({@code m.x}) with a lenient
   * provided schema. The schema can include columns other than
   * the specific one, but only the specific one will be projected.
   * Note that the projection list, not the provided schema, constrains
   * projection in this case.
   */
  @Test
  public void testSpecificMapWithLenientProvidedSchema() {
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectList("a", "m.x"));
    final TupleMetadata providedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("x", MinorType.BIGINT)
          .add("y", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();
    builder.providedSchema(providedSchema);
    final ScanSchemaTracker schemaTracker = builder.build();
    assertTrue(schemaTracker.isResolved());
    final int initVersion = schemaTracker.schemaVersion();

    // Pretend the reader reads one of the map columns and discovers a new one.
    TupleMetadata reader1InputSchema = schemaTracker.readerInputSchema();
    assertEquals(2, reader1InputSchema.size());

    final TupleMetadata readerOutputSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("x", MinorType.BIGINT)
          .resumeSchema()
        .buildSchema();

    {
      ProjectionFilter filter = schemaTracker.projectionFilter(ERROR_CONTEXT);
      assertTrue(filter instanceof DynamicSchemaFilter);
      assertTrue(isProjected(filter, readerOutputSchema.metadata("a")));
      assertTrue(isProjected(filter, readerOutputSchema.metadata("m")));
      assertFalse(filter.isProjected("b"));

      assertSame(ProjectionFilter.PROJECT_NONE,
          mapProjection(filter, "w").mapFilter);

      final ColumnMetadata mapCol = readerOutputSchema.metadata("m");
      final ProjectionFilter mapFilter = filter.projection(mapCol).mapFilter;
      assertTrue(mapFilter instanceof DynamicSchemaFilter);
      final TupleMetadata mapSchema = mapCol.tupleSchema();
      assertTrue(isProjected(mapFilter, mapSchema.metadata("x")));
      assertFalse(mapFilter.isProjected("y"));

      assertSame(ProjectionFilter.PROJECT_NONE,
          mapProjection(mapFilter, "w").mapFilter);
    }

    schemaTracker.applyReaderSchema(readerOutputSchema, ERROR_CONTEXT);
    assertEquals(initVersion, schemaTracker.schemaVersion());

    // Scan output sent downstream
    assertTrue(schemaTracker.isResolved());
    assertEquals(readerOutputSchema, schemaTracker.outputSchema());
  }

  /**
   * Test a specific map column ({@code m.x}) with a strict
   * provided schema. The schema can include columns other than
   * the specific one, but only the specific one will be projected.
   */
  @Test
  public void testSpecificMapWithStrictProvidedSchema() {
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectList("a", "m.x"));
    final TupleMetadata providedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("x", MinorType.BIGINT)
          .add("y", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();
    providedSchema.setBooleanProperty(TupleMetadata.IS_STRICT_SCHEMA_PROP, true);
    builder.providedSchema(providedSchema);
    final TupleMetadata expected = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("x", MinorType.BIGINT)
          .resumeSchema()
        .buildSchema();
    doTestSpecificMapWithSchema(builder, expected);
  }

  /**
   * Test a specific map column ({@code m.x}) with a defined schema.
   * The defined schema must exactly match the projection list (because
   * it should have been computed from that list.)
   */
  @Test
  public void testSpecificMapWithDefinedSchema() {
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectList("a", "m.x"));
    final TupleMetadata definedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("x", MinorType.BIGINT)
          .resumeSchema()
        .buildSchema();
    definedSchema.setBooleanProperty(TupleMetadata.IS_STRICT_SCHEMA_PROP, true);
    builder.definedSchema(definedSchema);
    doTestSpecificMapWithSchema(builder, definedSchema);
  }

  private void doTestSpecificMapWithSchema(ScanSchemaConfigBuilder builder, TupleMetadata targetSchema) {
    final ScanSchemaTracker schemaTracker = builder.build();
    assertTrue(schemaTracker.isResolved());
    final int initVersion = schemaTracker.schemaVersion();

    // Pretend the reader reads one of the map columns and discovers a new one.
    TupleMetadata reader1InputSchema = schemaTracker.readerInputSchema();
    assertEquals(2, reader1InputSchema.size());

    final TupleMetadata readerOutputSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("x", MinorType.BIGINT)
          .resumeSchema()
        .buildSchema();

    {
      ProjectionFilter filter = schemaTracker.projectionFilter(ERROR_CONTEXT);
      assertTrue(filter instanceof DynamicSchemaFilter);
      assertTrue(isProjected(filter, readerOutputSchema.metadata("a")));
      assertTrue(isProjected(filter, readerOutputSchema.metadata("m")));
      assertFalse(filter.isProjected("b"));

      final ColumnMetadata mapCol = readerOutputSchema.metadata("m");
      final ProjectionFilter mapFilter = filter.projection(mapCol).mapFilter;
      assertTrue(mapFilter instanceof DynamicSchemaFilter);
      final TupleMetadata mapSchema = mapCol.tupleSchema();
      assertTrue(isProjected(mapFilter, mapSchema.metadata("x")));
      assertFalse(mapFilter.isProjected("y"));

      assertSame(ProjectionFilter.PROJECT_NONE,
          mapProjection(mapFilter, "w").mapFilter);
    }

    schemaTracker.applyReaderSchema(readerOutputSchema, ERROR_CONTEXT);
    assertEquals(initVersion, schemaTracker.schemaVersion());

    // Scan output sent downstream
    assertTrue(schemaTracker.isResolved());
    assertEquals(targetSchema, schemaTracker.outputSchema());
  }

  @Test
  public void testDynamicMapWithDefinedSchema() {
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectList("a", "m"));
    final TupleMetadata definedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addDynamic("m")
        .buildSchema();
    definedSchema.setBooleanProperty(TupleMetadata.IS_STRICT_SCHEMA_PROP, true);
    builder.definedSchema(definedSchema);
    final ScanSchemaTracker schemaTracker = builder.build();
    assertFalse(schemaTracker.isResolved());

    // Pretend the reader reads one of the map columns and discovers a new one.
    TupleMetadata reader1InputSchema = schemaTracker.readerInputSchema();
    assertEquals(2, reader1InputSchema.size());

    final TupleMetadata readerOutputSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("x", MinorType.BIGINT)
          .resumeSchema()
        .buildSchema();

    {
      ProjectionFilter filter = schemaTracker.projectionFilter(ERROR_CONTEXT);
      assertTrue(filter instanceof DynamicSchemaFilter);
      assertTrue(isProjected(filter, readerOutputSchema.metadata("a")));
      assertTrue(isProjected(filter, readerOutputSchema.metadata("m")));
      assertFalse(filter.isProjected("b"));

      final ColumnMetadata mapCol = readerOutputSchema.metadata("m");
      final ProjectionFilter mapFilter = filter.projection(mapCol).mapFilter;
      assertSame(ProjectionFilter.PROJECT_ALL, mapFilter);
      final TupleMetadata mapSchema = mapCol.tupleSchema();
      assertTrue(isProjected(mapFilter, mapSchema.metadata("x")));
      assertTrue(mapFilter.isProjected("y"));

      assertSame(ProjectionFilter.PROJECT_ALL,
          mapProjection(mapFilter, "w").mapFilter);
    }

    schemaTracker.applyReaderSchema(readerOutputSchema, ERROR_CONTEXT);

    // Scan output sent downstream
    assertTrue(schemaTracker.isResolved());
    assertEquals(readerOutputSchema, schemaTracker.outputSchema());
  }

  /**
   * Test a generic map column ({@code m.x}) with a defined schema.
   * The defined schema must exactly match the projection list (because
   * it should have been computed from that list.)
   */
  @Test
  public void testGenericMapWithStrictSchema() {
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectList("a", "m"));
    final TupleMetadata definedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("x", MinorType.BIGINT)
          .resumeSchema()
        .buildSchema();
    definedSchema.setBooleanProperty(TupleMetadata.IS_STRICT_SCHEMA_PROP, true);
    builder.providedSchema(definedSchema);
    doTestGenericMapWithSchema(builder, definedSchema);
  }

  @Test
  public void testDynamicMapWithStrictSchema() {
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectList("a", "m"));
    final TupleMetadata definedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("x", MinorType.BIGINT)
          .resumeSchema()
        .buildSchema();
    definedSchema.setBooleanProperty(TupleMetadata.IS_STRICT_SCHEMA_PROP, true);
    builder.providedSchema(definedSchema);
    doTestGenericMapWithSchema(builder, definedSchema);
  }

  private void doTestGenericMapWithSchema(ScanSchemaConfigBuilder builder, TupleMetadata targetSchema) {
    final ScanSchemaTracker schemaTracker = builder.build();
    assertTrue(schemaTracker.isResolved());
    final int initVersion = schemaTracker.schemaVersion();

    // Pretend the reader reads one of the map columns and discovers a new one.
    TupleMetadata reader1InputSchema = schemaTracker.readerInputSchema();
    assertEquals(2, reader1InputSchema.size());

    final TupleMetadata readerOutputSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("x", MinorType.BIGINT)
          .resumeSchema()
        .buildSchema();

    {
      ProjectionFilter filter = schemaTracker.projectionFilter(ERROR_CONTEXT);
      assertTrue(filter instanceof DynamicSchemaFilter);
      assertTrue(isProjected(filter, readerOutputSchema.metadata("a")));
      assertTrue(isProjected(filter, readerOutputSchema.metadata("m")));
      assertFalse(filter.isProjected("b"));

      final ColumnMetadata mapCol = readerOutputSchema.metadata("m");
      final ProjectionFilter mapFilter = filter.projection(mapCol).mapFilter;
      assertTrue(mapFilter instanceof DynamicSchemaFilter);
      final TupleMetadata mapSchema = mapCol.tupleSchema();
      assertTrue(isProjected(mapFilter, mapSchema.metadata("x")));
      assertFalse(mapFilter.isProjected("y"));

      assertSame(ProjectionFilter.PROJECT_NONE,
          mapProjection(mapFilter, "w").mapFilter);
    }

    schemaTracker.applyReaderSchema(readerOutputSchema, ERROR_CONTEXT);
    assertEquals(initVersion, schemaTracker.schemaVersion());

    // Scan output sent downstream
    assertTrue(schemaTracker.isResolved());
    assertEquals(targetSchema, schemaTracker.outputSchema());
  }

  @Test
  public void testGenericMapWithDynamicSchema() {
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectList("a", "m"));
    final TupleMetadata definedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addDynamic("m")
        .buildSchema();
    definedSchema.setBooleanProperty(TupleMetadata.IS_STRICT_SCHEMA_PROP, true);
    builder.definedSchema(definedSchema);
    final ScanSchemaTracker schemaTracker = builder.build();
    assertFalse(schemaTracker.isResolved());
    final int initVersion = schemaTracker.schemaVersion();

    // Pretend the reader reads one of the map columns and discovers a new one.
    TupleMetadata reader1InputSchema = schemaTracker.readerInputSchema();
    assertEquals(2, reader1InputSchema.size());

    final TupleMetadata readerOutputSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("x", MinorType.BIGINT)
          .resumeSchema()
        .buildSchema();

    {
      ProjectionFilter filter = schemaTracker.projectionFilter(ERROR_CONTEXT);
      assertTrue(filter instanceof DynamicSchemaFilter);
      assertTrue(isProjected(filter, readerOutputSchema.metadata("a")));
      assertTrue(isProjected(filter, readerOutputSchema.metadata("m")));
      assertFalse(filter.isProjected("b"));

      final ColumnMetadata mapCol = readerOutputSchema.metadata("m");
      final ProjectionFilter mapFilter = filter.projection(mapCol).mapFilter;
      assertSame(ProjectionFilter.PROJECT_ALL, mapFilter);
    }

    schemaTracker.applyReaderSchema(readerOutputSchema, ERROR_CONTEXT);
    assertTrue(initVersion < schemaTracker.schemaVersion());

    // Scan output sent downstream
    assertTrue(schemaTracker.isResolved());
    assertEquals(readerOutputSchema, schemaTracker.outputSchema());
  }

  /**
   * When a defined schema is given, the map projection list
   * must match the defined schema. Unlike a provided schema,
   * extra columns are not allowed.
   */
  @Test
  public void testMapProjectionMismatchLength() {
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectList("a", "m.x"));
    final TupleMetadata definedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("x", MinorType.BIGINT)
          .add("y", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();
    definedSchema.setBooleanProperty(TupleMetadata.IS_STRICT_SCHEMA_PROP, true);
    builder.definedSchema(definedSchema);
    try {
      builder.build();
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

  /**
   * When a defined schema is given, the map projection list
   * must match the defined schema. Unlike a provided schema,
   * disjoint columns are not allowed.
   */
  @Test
  public void testMapProjectionMismatchMembers() {
    final ScanSchemaConfigBuilder builder = new ScanSchemaConfigBuilder()
        .projection(RowSetTestUtils.projectList("a", "m.x"));
    final TupleMetadata definedSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("y", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();
    definedSchema.setBooleanProperty(TupleMetadata.IS_STRICT_SCHEMA_PROP, true);
    builder.definedSchema(definedSchema);
    try {
      builder.build();
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }
}
