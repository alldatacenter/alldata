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

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

import org.apache.drill.categories.EvfTest;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.rowSet.RowSetTestUtils;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test resolving various projection lists against a provided schema.
 * Things get complex when resolving maps.
 * <p>
 * In most cases, an early reader schema, by itself, works the same
 * as a provided schema: both are a way of providing type information
 * before any data is seen. There are subtle differences explored
 * here and in {@link TestSchemaTrackerEarlyReaderSchema}.
 */
@Category(EvfTest.class)
public class TestSchemaTrackerInputSchema extends BaseTestSchemaTracker {

  private void testBoth(Collection<SchemaPath> projList, TupleMetadata schema,
      Consumer<ProjectionSchemaTracker> test) {

    // Test the schema as provided
    ProjectionSchemaTracker tracker1 = trackerFor(projList);
    tracker1.applyProvidedSchema(schema);
    test.accept(tracker1);

    // Test the schema as an early reader schema
    ProjectionSchemaTracker tracker2 = trackerFor(projList);
    tracker2.applyEarlyReaderSchema(schema);
    test.accept(tracker2);
  }

  @Test
  public void testEmpty() {
    testBoth(RowSetTestUtils.projectNone(), SCHEMA, tracker -> {
      assertTrue(tracker.isResolved());
      assertSame(ScanSchemaTracker.ProjectionType.NONE, tracker.projectionType());
      assertTrue(tracker.internalSchema().toSchema().isEmpty());
    });
  }

  @Test
  public void testWithWildcard() {
    testBoth(RowSetTestUtils.projectAll(), SCHEMA, tracker -> {
      assertTrue(tracker.isResolved());
      assertSame(ScanSchemaTracker.ProjectionType.ALL, tracker.projectionType());
      TupleMetadata schema = tracker.internalSchema().toSchema();
      assertEquals(SCHEMA, schema);

      // Verify column properties are merged
      assertEquals(MOCK_VALUE, schema.metadata("a").property(MOCK_PROP));
    });
  }

  @Test
  public void testStrictWithWildcard() {
    ProjectionSchemaTracker tracker = trackerFor(
        RowSetTestUtils.projectAll());
    TupleMetadata provided = SCHEMA.copy();
    SchemaUtils.markStrict(provided);
    tracker.applyProvidedSchema(provided);
    assertTrue(tracker.isResolved());

    // Strict setting with wildcard only expands the strict
    // schema; the reader can add no new columns.
    assertSame(ScanSchemaTracker.ProjectionType.SOME, tracker.projectionType());
    TupleMetadata schema = tracker.internalSchema().toSchema();
    assertEquals(SCHEMA, schema);
  }

  @Test
  public void testSpecialWithWildcard() {
    TupleMetadata input = SCHEMA.copy();
    SchemaUtils.markExcludeFromWildcard(input.metadata("b"));
    testBoth(RowSetTestUtils.projectAll(), input, tracker -> {
      assertTrue(tracker.isResolved());
      assertSame(ScanSchemaTracker.ProjectionType.ALL, tracker.projectionType());

      TupleMetadata expected = new SchemaBuilder()
          .add("a", MinorType.INT)
          .build();
      TupleMetadata schema = tracker.internalSchema().toSchema();
      assertEquals(expected, schema);
    });
  }

  /**
   * Drill will provide a project list that includes both a wildcard
   * and column names if the columns are implicit. Not applicable
   * to a reader schema.
   */
  @Test
  public void testWithWildcardAndCols() {
    ProjectionSchemaTracker tracker = trackerFor(
        RowSetTestUtils.projectList("x", SchemaPath.DYNAMIC_STAR, "y"));
    tracker.applyProvidedSchema(SCHEMA);
    assertFalse(tracker.isResolved());
    assertSame(ScanSchemaTracker.ProjectionType.ALL, tracker.projectionType());

    // Schema is partially resolved: some columns are dynamic
    // (to be resolved by reader)
    TupleMetadata expected = new SchemaBuilder()
        .addDynamic("x")
        .addAll(SCHEMA)
        .addDynamic("y")
        .build();
    assertEquals(expected, tracker.internalSchema().toSchema());
  }

  @Test
  public void testWithExplicit() {
    testBoth(
        RowSetTestUtils.projectList("b", "c"),
        SCHEMA, tracker -> {
      assertSame(ScanSchemaTracker.ProjectionType.SOME, tracker.projectionType());
      assertFalse(tracker.isResolved());
      TupleMetadata expected = new SchemaBuilder()
          .addNullable("b", MinorType.VARCHAR)
          .addDynamic("c")
          .build();
      assertEquals(expected, tracker.internalSchema().toSchema());
    });
  }

  @Test
  public void testWithExplicitReorder() {
    testBoth(
        RowSetTestUtils.projectList("b", "a"),
        SCHEMA, tracker -> {
      assertSame(ScanSchemaTracker.ProjectionType.SOME, tracker.projectionType());
      assertTrue(tracker.isResolved());
      TupleMetadata schema = tracker.internalSchema().toSchema();
      TupleMetadata expected = new SchemaBuilder()
          .addNullable("b", MinorType.VARCHAR)
          .add("a", MinorType.INT)
          .build();
      assertEquals(expected, schema);

      // Verify column properties are merged
      assertEquals(MOCK_VALUE, schema.metadata("a").property(MOCK_PROP));
    });
  }

  @Test
  public void testProvidedMapProjectConflict() {
    ProjectionSchemaTracker tracker = trackerFor(
        RowSetTestUtils.projectList("a.x"));
    try {
      tracker.applyProvidedSchema(SCHEMA);
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("not compatible"));
      assertTrue(e.getMessage().contains("Projected column: a {x}"));
      assertTrue(e.getMessage().contains("Provided column: `a` INT NOT NULL"));
    }
  }

  @Test
  public void testReaderMapProjectConflict() {
    ProjectionSchemaTracker tracker = trackerFor(
        RowSetTestUtils.projectList("a.x"));
    try {
      tracker.applyEarlyReaderSchema(SCHEMA);
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("not compatible"));
      assertTrue(e.getMessage().contains("Projected column: a {x}"));
      assertTrue(e.getMessage().contains("Reader column: `a` INT NOT NULL"));
    }
  }

  @Test
  public void testProvidedArrayProjectConflict() {
    ProjectionSchemaTracker tracker = trackerFor(
        RowSetTestUtils.projectList("a[2]"));
    try {
      tracker.applyProvidedSchema(SCHEMA);
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("not compatible"));
      assertTrue(e.getMessage().contains("Projected column: a[2]"));
      assertTrue(e.getMessage().contains("Provided column: `a` INT NOT NULL"));
    }
  }

  @Test
  public void testReaderArrayProjectConflict() {
    ProjectionSchemaTracker tracker = trackerFor(
        RowSetTestUtils.projectList("a[2]"));
    try {
      tracker.applyEarlyReaderSchema(SCHEMA);
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("not compatible"));
      assertTrue(e.getMessage().contains("Projected column: a[2]"));
      assertTrue(e.getMessage().contains("Reader column: `a` INT NOT NULL"));
    }
  }

  @Test
  public void testEmptyWithMap() {
    testBoth(
        Collections.emptyList(),
        MAP_SCHEMA, tracker -> {
      assertSame(ScanSchemaTracker.ProjectionType.NONE, tracker.projectionType());
      assertTrue(tracker.isResolved());
      assertTrue(tracker.internalSchema().toSchema().isEmpty());
    });
  }

  /**
   * Lenient schema with a map and a wildcard: both the top level
   * and the map allow the reader to add members.
   */
  @Test
  public void testWithWildcardWithMap() {
    testBoth(
        RowSetTestUtils.projectAll(),
        MAP_SCHEMA, tracker -> {
      doTestGenericMap(tracker, true);
      assertSame(ScanSchemaTracker.ProjectionType.ALL, tracker.projectionType());
    });
  }

  /**
   * Strict schema with a map and a wildcard: neither the top level
   * nor the map allow the reader to add members. Not applicable for
   * a reader schema.
   */
  @Test
  public void testStrictWithWildcardWithMap() {
    ProjectionSchemaTracker tracker = trackerFor(
        RowSetTestUtils.projectAll());

    // Mark schema as strict
    TupleMetadata provided = MAP_SCHEMA.copy();
    SchemaUtils.markStrict(provided);
    tracker.applyProvidedSchema(provided);
    assertTrue(tracker.isResolved());

    // Projection as without strict, except the map is "closed",
    // won't accept new members from the reader.
    TupleMetadata schema = tracker.internalSchema().toSchema();
    assertEquals(MAP_SCHEMA, schema);
    ColumnMetadata mapCol = schema.metadata("m");
    assertFalse(SchemaUtils.isProjectAll(mapCol.tupleSchema()));
  }

  /**
   * Test a map projected with just the map name: {@code `m`}.
   * Expands the the full map.
   */
  @Test
  public void testGenericMap() {
    testBoth(
        RowSetTestUtils.projectList("a", "m"),
        MAP_SCHEMA, tracker -> {
      doTestGenericMap(tracker, true);
      assertSame(ScanSchemaTracker.ProjectionType.SOME, tracker.projectionType());
    });
  }

  /**
   * Test a map with members projected in the same order as the
   * provided schema.
   */
  @Test
  public void testSpecificMap() {
    testBoth(
        RowSetTestUtils.projectList("a", "m.x", "m.y"),
        MAP_SCHEMA, tracker -> {
      doTestGenericMap(tracker, false);
      assertSame(ScanSchemaTracker.ProjectionType.SOME, tracker.projectionType());
    });
  }

  /**
   * Projection of whole map and one column, the map
   * column accepts new reader columns.
   */
  @Test
  public void testGenericAndSpecificMap() {
    testBoth(
        RowSetTestUtils.projectList("a", "m.x", "m"),
        MAP_SCHEMA, tracker -> {
      doTestGenericMap(tracker, true);
      assertSame(ScanSchemaTracker.ProjectionType.SOME, tracker.projectionType());
    });
  }

  private void doTestGenericMap(ProjectionSchemaTracker tracker, boolean mapAll) {
    assertTrue(tracker.isResolved());
    TupleMetadata schema = tracker.internalSchema().toSchema();
    assertEquals(MAP_SCHEMA, schema);
    ColumnMetadata mapCol = schema.metadata("m");
    assertEquals(mapAll, SchemaUtils.isProjectAll(mapCol.tupleSchema()));
    assertEquals(MOCK_VALUE, schema.metadata("m").property(MOCK_PROP));
  }

  /**
   * Lenient schema projecting the whole map: the map allows the
   * reader to add members.
   */
  @Test
  public void testSubsetWithMap() {
    TupleMetadata expected = new SchemaBuilder()
        .addMap("m")
          .add("x", MinorType.BIGINT)
          .add("y", MinorType.VARCHAR)
          .resumeSchema()
        .build();
    testBoth(
        RowSetTestUtils.projectList("m"),
        MAP_SCHEMA, tracker -> {
      assertTrue(tracker.isResolved());
      TupleMetadata schema = tracker.internalSchema().toSchema();
      assertEquals(expected, schema);
      assertTrue(SchemaUtils.isProjectAll(
          schema.metadata("m").tupleSchema()));
    });
  }

  /**
   * Strict schema projecting the whole map: the map does not allow the
   * reader to add members. Not applicable to a reader schema.
   */
  @Test
  public void testStrictSubsetWithMap() {
    ProjectionSchemaTracker tracker = trackerFor(
        RowSetTestUtils.projectList("m"));
    TupleMetadata provided = MAP_SCHEMA.copy();
    SchemaUtils.markStrict(provided);
    tracker.applyProvidedSchema(provided);
    TupleMetadata expected = new SchemaBuilder()
        .addMap("m")
          .add("x", MinorType.BIGINT)
          .add("y", MinorType.VARCHAR)
          .resumeSchema()
        .build();
    assertTrue(tracker.isResolved());
    TupleMetadata schema = tracker.internalSchema().toSchema();
    assertEquals(expected, schema);
    assertTrue(!SchemaUtils.isProjectAll(
        schema.metadata("m").tupleSchema()));
  }

  @Test
  public void testMapSubset() {
    TupleMetadata expected = new SchemaBuilder()
        .addMap("m")
          .add("x", MinorType.BIGINT)
          .resumeSchema()
        .build();
    testBoth(
        RowSetTestUtils.projectList("m.x"),
        MAP_SCHEMA, tracker -> {
      assertTrue(tracker.isResolved());
      assertEquals(expected, tracker.internalSchema().toSchema());
    });
  }

  @Test
  public void testMapDisjointSet() {
    TupleMetadata expected = new SchemaBuilder()
        .addMap("m")
          .addDynamic("w")
          .add("x", MinorType.BIGINT)
          .addDynamic("z")
          .resumeSchema()
        .build();
    testBoth(
        RowSetTestUtils.projectList("m.w", "m.x", "m.z"),
        MAP_SCHEMA, tracker -> {
      assertFalse(tracker.isResolved());
      assertEquals(expected, tracker.internalSchema().toSchema());
    });
  }

  @Test
  public void testGenericAndSpecificMapReorder() {
    TupleMetadata expected = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("y", MinorType.VARCHAR)
          .add("x", MinorType.BIGINT)
          .resumeSchema()
        .build();
    testBoth(
        RowSetTestUtils.projectList("a", "m.y", "m"),
        MAP_SCHEMA, tracker -> {
      assertTrue(tracker.isResolved());
      assertEquals(expected, tracker.internalSchema().toSchema());
    });
  }

  @Test
  public void testProvidedMapProjectConflictInMap() {
    ProjectionSchemaTracker tracker = trackerFor(
        RowSetTestUtils.projectList("m.x.u"));
    try {
      tracker.applyProvidedSchema(MAP_SCHEMA);
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("not compatible"));
      assertTrue(e.getMessage().contains("Projected column: x {u}"));
      assertTrue(e.getMessage().contains("Provided column: `x` BIGINT NOT NULL"));
    }
  }

  @Test
  public void testReaderMapProjectConflictInMap() {
    ProjectionSchemaTracker tracker = trackerFor(
        RowSetTestUtils.projectList("m.x.u"));
    try {
      tracker.applyEarlyReaderSchema(MAP_SCHEMA);
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("not compatible"));
      assertTrue(e.getMessage().contains("Projected column: x {u}"));
      assertTrue(e.getMessage().contains("Reader column: `x` BIGINT NOT NULL"));
    }
  }

  @Test
  public void testProvidedArrayProjectConflictInMap() {
    ProjectionSchemaTracker tracker = trackerFor(
        RowSetTestUtils.projectList("m.x[2]"));
    try {
      tracker.applyProvidedSchema(MAP_SCHEMA);
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("not compatible"));
      assertTrue(e.getMessage().contains("Projected column: x[]"));
      assertTrue(e.getMessage().contains("Provided column: `x` BIGINT NOT NULL"));
    }
  }

  @Test
  public void testReaderArrayProjectConflictInMap() {
    ProjectionSchemaTracker tracker = trackerFor(
        RowSetTestUtils.projectList("m.x[2]"));
    try {
      tracker.applyEarlyReaderSchema(MAP_SCHEMA);
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("not compatible"));
      assertTrue(e.getMessage().contains("Projected column: x[]"));
      assertTrue(e.getMessage().contains("Reader column: `x` BIGINT NOT NULL"));
    }
  }

  @Test
  public void testWithWildcardWithNestedMap() {
    testBoth(
        RowSetTestUtils.projectAll(),
        NESTED_MAP_SCHEMA, tracker -> {
      doTestGenericNestedMap(tracker);
    });
  }

  @Test
  public void testGenericNestedMap() {
    testBoth(
        RowSetTestUtils.projectList("a", "m"),
        NESTED_MAP_SCHEMA, tracker -> {
      doTestGenericNestedMap(tracker);
    });
  }

  @Test
  public void testSpecificNestedMap1() {
     testBoth(
        RowSetTestUtils.projectList("a", "m.x", "m.y", "m.m2"),
        NESTED_MAP_SCHEMA, tracker -> {
      doTestGenericNestedMap(tracker);
    });
  }

  @Test
  public void testSpecificNestedMap2() {
    testBoth(
        RowSetTestUtils.projectList("a", "m.x", "m.y", "m.m2.p", "m.m2.q"),
        NESTED_MAP_SCHEMA, tracker -> {
      doTestGenericNestedMap(tracker);
    });
  }

  @Test
  public void testGenericAndSpecificNestedMap() {
    testBoth(
        RowSetTestUtils.projectList("a", "m.x", "m.y", "m.m2.p", "m.m2"),
        NESTED_MAP_SCHEMA, tracker -> {
      doTestGenericNestedMap(tracker);
    });
  }

  private void doTestGenericNestedMap(ProjectionSchemaTracker tracker) {
    TupleMetadata schema = tracker.internalSchema().toSchema();
    assertTrue(tracker.isResolved());
    assertEquals(NESTED_MAP_SCHEMA, schema);
  }

  @Test
  public void testSubsetWithNestedMap() {
    TupleMetadata expected = new SchemaBuilder()
        .addMap("m")
          .addMap("m2")
            .add("p", MinorType.BIGINT)
            .add("q", MinorType.VARCHAR)
            .resumeMap()
          .resumeSchema()
        .buildSchema();
    testBoth(
        RowSetTestUtils.projectList("m.m2"),
        NESTED_MAP_SCHEMA, tracker -> {
      assertTrue(tracker.isResolved());
      assertEquals(expected, tracker.internalSchema().toSchema());
    });
  }

  @Test
  public void testNestedMapSubset() {
    TupleMetadata expected = new SchemaBuilder()
        .addMap("m")
          .addMap("m2")
            .add("p", MinorType.BIGINT)
            .resumeMap()
          .resumeSchema()
        .buildSchema();
    testBoth(
        RowSetTestUtils.projectList("m.m2.p"),
        NESTED_MAP_SCHEMA, tracker -> {
      assertTrue(tracker.isResolved());
      assertEquals(expected, tracker.internalSchema().toSchema());
    });
  }

  @Test
  public void testNestedMapDisjointSet() {
    TupleMetadata expected = new SchemaBuilder()
        .addMap("m")
          .addMap("m2")
            .addDynamic("o")
            .add("p", MinorType.BIGINT)
            .addDynamic("r")
            .resumeMap()
          .resumeSchema()
        .buildSchema();
    testBoth(
        RowSetTestUtils.projectList("m.m2.o", "m.m2.p", "m.m2.r"),
        NESTED_MAP_SCHEMA, tracker -> {
      assertFalse(tracker.isResolved());
      assertEquals(expected, tracker.internalSchema().toSchema());
    });
  }
}
