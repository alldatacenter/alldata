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
package org.apache.drill.exec.physical.resultSet.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.drill.test.BaseTest;
import org.apache.drill.common.exceptions.EmptyErrorContext;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.physical.resultSet.impl.ProjectionFilter.CompoundProjectionFilter;
import org.apache.drill.exec.physical.resultSet.impl.ProjectionFilter.DirectProjectionFilter;
import org.apache.drill.exec.physical.resultSet.impl.ProjectionFilter.ProjResult;
import org.apache.drill.exec.physical.resultSet.impl.ProjectionFilter.SchemaProjectionFilter;
import org.apache.drill.exec.physical.resultSet.impl.ProjectionFilter.TypeProjectionFilter;
import org.apache.drill.exec.physical.resultSet.project.Projections;
import org.apache.drill.exec.physical.resultSet.project.RequestedTuple;
import org.apache.drill.exec.physical.rowSet.RowSetTestUtils;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.TupleSchema;
import org.junit.Test;

public class TestProjectionFilter extends BaseTest{
  private static final ColumnMetadata A_COL = MetadataUtils.newScalar("a", Types.required(MinorType.INT));
  private static final ColumnMetadata B_COL = MetadataUtils.newScalar("b", Types.optional(MinorType.VARCHAR));
  private static final ColumnMetadata MAP_COL = MetadataUtils.newMap("m", new TupleSchema());
  private static final ColumnMetadata MAP_COL2 = MetadataUtils.newMap("m2", new TupleSchema());

  @Test
  public void testImplicitAll() {
    ProjectionFilter filter = ProjectionFilter.PROJECT_ALL;
    assertTrue(filter.isProjected("a"));
    assertTrue(filter.projection(A_COL).isProjected);
    ColumnMetadata specialCol = MetadataUtils.newScalar("special", Types.optional(MinorType.BIGINT));
    specialCol.setBooleanProperty(ColumnMetadata.EXCLUDE_FROM_WILDCARD, true);
    assertFalse(filter.projection(specialCol).isProjected);
    assertFalse(filter.isEmpty());
    ProjResult result = filter.projection(MAP_COL);
    assertTrue(result.isProjected);
    assertSame(ProjectionFilter.PROJECT_ALL, result.mapFilter);
  }

  @Test
  public void testImplicitNone() {
    ProjectionFilter filter = ProjectionFilter.PROJECT_NONE;
    assertFalse(filter.isProjected("a"));
    assertFalse(filter.projection(A_COL).isProjected);
    assertTrue(filter.isEmpty());
    ProjResult result = filter.projection(MAP_COL);
    assertFalse(result.isProjected);
    assertSame(ProjectionFilter.PROJECT_NONE, result.mapFilter);
  }

  @Test
  public void testProjectList() {
    RequestedTuple projSet = Projections.parse(
        RowSetTestUtils.projectList("a", "c", "m.a"));
    ProjectionFilter filter = new DirectProjectionFilter(projSet, EmptyErrorContext.INSTANCE);
    assertTrue(filter.isProjected("a"));
    assertTrue(filter.projection(A_COL).isProjected);
    assertFalse(filter.isProjected("b"));
    assertFalse(filter.projection(B_COL).isProjected);
    assertFalse(filter.isEmpty());

    ProjResult result = filter.projection(MAP_COL);
    assertTrue(result.isProjected);
    ProjectionFilter child = result.mapFilter;
    assertTrue(child.isProjected("a"));
    assertFalse(child.isProjected("b"));

    result = filter.projection(MAP_COL2);
    assertFalse(result.isProjected);
    assertSame(ProjectionFilter.PROJECT_NONE, result.mapFilter);
  }
  @Test
  public void testGenericMap() {
    RequestedTuple projSet = Projections.parse(
        RowSetTestUtils.projectList("a", "m"));
    ProjectionFilter filter = new DirectProjectionFilter(projSet, EmptyErrorContext.INSTANCE);
    assertTrue(filter.isProjected("a"));

    ProjResult result = filter.projection(MAP_COL);
    assertTrue(result.isProjected);
    assertSame(ProjectionFilter.PROJECT_ALL, result.mapFilter);
  }

  @Test
  public void testEmptyProjectList() {
    ProjectionFilter filter = new DirectProjectionFilter(Projections.projectNone(), EmptyErrorContext.INSTANCE);
    assertFalse(filter.isProjected("a"));
    assertFalse(filter.projection(A_COL).isProjected);
    assertTrue(filter.isEmpty());
    ProjResult result = filter.projection(MAP_COL);
    assertFalse(result.isProjected);
    assertSame(ProjectionFilter.PROJECT_NONE, result.mapFilter);
  }

  @Test
  public void testTypeFilter() {
    TupleMetadata schema = new SchemaBuilder()
        .add(A_COL.copy())
        .add(B_COL.copy())
        .addMap("m")
          .add("a", MinorType.INT)
          .resumeSchema()
        .build();
    ProjectionFilter filter = new TypeProjectionFilter(schema, EmptyErrorContext.INSTANCE);
    assertFalse(filter.isEmpty());
    assertTrue(filter.isProjected("a"));
    assertTrue(filter.projection(A_COL).isProjected);
    assertTrue(filter.isProjected("b"));
    assertTrue(filter.projection(B_COL).isProjected);
    assertTrue(filter.isProjected("c"));
    assertTrue(filter.projection(
        MetadataUtils.newScalar("c", Types.required(MinorType.BIGINT))).isProjected);

    ColumnMetadata typeConflict = MetadataUtils.newScalar("a", Types.required(MinorType.BIGINT));
    try {
      filter.projection(typeConflict);
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("conflict"));
    }

    ColumnMetadata modeConflict = MetadataUtils.newScalar("a", Types.optional(MinorType.INT));
    try {
      filter.projection(modeConflict);
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("conflict"));
    }

    ProjResult result = filter.projection(MAP_COL);
    assertTrue(result.isProjected);
    ProjectionFilter child = result.mapFilter;
    assertTrue(child.isProjected("a"));
    assertTrue(child.isProjected("b"));

    result = filter.projection(MAP_COL2);
    assertTrue(result.isProjected);
    assertSame(ProjectionFilter.PROJECT_ALL, result.mapFilter);

    try {
      ColumnMetadata aMap = MetadataUtils.newMap("a", new TupleSchema());
      filter.projection(aMap);
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("type conflict"));
    }
  }

  @Test
  public void testSchemaFilter() {
    TupleMetadata schema = new SchemaBuilder()
        .add(A_COL.copy())
        .add(B_COL.copy())
        .addMap("m")
          .add("a", MinorType.INT)
          .resumeSchema()
        .build();
    ProjectionFilter filter = new SchemaProjectionFilter(schema, EmptyErrorContext.INSTANCE);
    assertFalse(filter.isEmpty());
    assertTrue(filter.isProjected("a"));
    assertTrue(filter.projection(A_COL).isProjected);
    assertTrue(filter.isProjected("b"));
    assertTrue(filter.projection(B_COL).isProjected);
    assertFalse(filter.isProjected("c"));
    assertFalse(filter.projection(MetadataUtils.newScalar("c", Types.required(MinorType.BIGINT))).isProjected);

    ColumnMetadata typeConflict = MetadataUtils.newScalar("a", Types.required(MinorType.BIGINT));
    try {
      filter.projection(typeConflict);
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("conflict"));
    }

    ColumnMetadata modeConflict = MetadataUtils.newScalar("a", Types.optional(MinorType.INT));
    try {
      filter.projection(modeConflict);
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("conflict"));
    }

    try {
      ColumnMetadata aMap = MetadataUtils.newMap("a", new TupleSchema());
      filter.projection(aMap);
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("type conflict"));
    }

    ProjResult result = filter.projection(MAP_COL);
    assertTrue(result.isProjected);
    ProjectionFilter child = result.mapFilter;
    assertTrue(child.isProjected("a"));
    assertFalse(child.isProjected("b"));
  }

  @Test
  public void testCompoundFilterMixed1() {
    ProjectionFilter filter = new CompoundProjectionFilter(
        ProjectionFilter.PROJECT_ALL, ProjectionFilter.PROJECT_NONE);
    assertFalse(filter.isProjected("a"));
    assertFalse(filter.projection(A_COL).isProjected);
    assertTrue(filter.isEmpty());
    ProjResult result = filter.projection(MAP_COL);
    assertFalse(result.isProjected);
    assertSame(ProjectionFilter.PROJECT_NONE, result.mapFilter);
  }

  @Test
  public void testCompoundFilterMixed2() {
    ProjectionFilter filter = new CompoundProjectionFilter(
        ProjectionFilter.PROJECT_NONE, ProjectionFilter.PROJECT_ALL);
    assertFalse(filter.isProjected("a"));
    assertFalse(filter.projection(A_COL).isProjected);
    assertTrue(filter.isEmpty());
    ProjResult result = filter.projection(MAP_COL);
    assertFalse(result.isProjected);
    assertSame(ProjectionFilter.PROJECT_NONE, result.mapFilter);
  }

  @Test
  public void testCompoundPermissive() {
    ProjectionFilter filter = new CompoundProjectionFilter(
        ProjectionFilter.PROJECT_ALL, ProjectionFilter.PROJECT_ALL);
    assertTrue(filter.isProjected("a"));
    assertTrue(filter.projection(A_COL).isProjected);
    ColumnMetadata specialCol = MetadataUtils.newScalar("special", Types.optional(MinorType.BIGINT));
    specialCol.setBooleanProperty(ColumnMetadata.EXCLUDE_FROM_WILDCARD, true);
    assertFalse(filter.projection(specialCol).isProjected);
    assertFalse(filter.isEmpty());
    ProjResult result = filter.projection(MAP_COL);
    assertTrue(result.isProjected);
    assertSame(ProjectionFilter.PROJECT_ALL, result.mapFilter);
  }

 @Test
  public void testCompoundMap() {
    TupleMetadata schema = new SchemaBuilder()
       .add(A_COL.copy())
       .add(B_COL.copy())
       .addMap("m")
         .add("a", MinorType.INT)
         .add("b", MinorType.INT)
         .resumeSchema()
       .build();
    ProjectionFilter filter = new CompoundProjectionFilter(
        new DirectProjectionFilter(Projections.parse(
            RowSetTestUtils.projectList("a", "c", "m.a")), EmptyErrorContext.INSTANCE),
        new SchemaProjectionFilter(schema, EmptyErrorContext.INSTANCE));

    ProjResult result = filter.projection(MAP_COL);
    assertTrue(result.isProjected);
    assertTrue(result.mapFilter.isProjected("a"));
 }

 @Test
 public void testBuilders() {
    assertSame(ProjectionFilter.PROJECT_ALL,
        ProjectionFilter.projectionFilter(Projections.projectAll(), EmptyErrorContext.INSTANCE));
    assertSame(ProjectionFilter.PROJECT_NONE,
        ProjectionFilter.projectionFilter(Projections.projectNone(), EmptyErrorContext.INSTANCE));
    assertTrue(
        ProjectionFilter.projectionFilter(Projections.parse(
            RowSetTestUtils.projectList("a")), EmptyErrorContext.INSTANCE)
        instanceof DirectProjectionFilter);

    TupleMetadata schema = new SchemaBuilder()
        .add(A_COL.copy())
        .add(B_COL.copy())
        .build();
     assertSame(ProjectionFilter.PROJECT_NONE,
        ProjectionFilter.definedSchemaFilter(new TupleSchema(), EmptyErrorContext.INSTANCE));
    assertTrue(
        ProjectionFilter.definedSchemaFilter(schema, EmptyErrorContext.INSTANCE)
        instanceof SchemaProjectionFilter);

    assertTrue(
        ProjectionFilter.providedSchemaFilter(Projections.projectAll(), schema,
            EmptyErrorContext.INSTANCE) instanceof CompoundProjectionFilter);
    assertSame(ProjectionFilter.PROJECT_NONE,
        ProjectionFilter.providedSchemaFilter(Projections.projectNone(), schema,
            EmptyErrorContext.INSTANCE));
    assertSame(ProjectionFilter.PROJECT_ALL,
        ProjectionFilter.providedSchemaFilter(Projections.projectAll(), new TupleSchema(),
            EmptyErrorContext.INSTANCE));
    TupleMetadata strictEmpty = new TupleSchema();
    strictEmpty.setBooleanProperty(TupleMetadata.IS_STRICT_SCHEMA_PROP, true);
    assertSame(ProjectionFilter.PROJECT_NONE,
        ProjectionFilter.providedSchemaFilter(Projections.projectAll(), strictEmpty,
            EmptyErrorContext.INSTANCE));
    assertTrue(
        ProjectionFilter.providedSchemaFilter(Projections.parse(
            RowSetTestUtils.projectList("a")),
            schema, EmptyErrorContext.INSTANCE)
        instanceof CompoundProjectionFilter);
  }
}
