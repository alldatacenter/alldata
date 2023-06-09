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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.apache.drill.test.BaseTest;
import org.apache.drill.categories.EvfTest;
import org.apache.drill.common.exceptions.EmptyErrorContext;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.physical.impl.scan.v3.schema.DynamicSchemaFilter.DynamicTupleFilter;
import org.apache.drill.exec.physical.impl.scan.v3.schema.ScanProjectionParser.ProjectionParseResult;
import org.apache.drill.exec.physical.resultSet.impl.ProjectionFilter;
import org.apache.drill.exec.physical.resultSet.impl.ProjectionFilter.ProjResult;
import org.apache.drill.exec.physical.rowSet.RowSetTestUtils;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(EvfTest.class)
public class TestDynamicSchemaFilter extends BaseTest{

  private static final ColumnMetadata A_COL =
      MetadataUtils.newScalar("a", Types.required(MinorType.INT));
  private static final ColumnMetadata B_COL =
      MetadataUtils.newScalar("b", Types.optional(MinorType.VARCHAR));
  private static final ColumnMetadata SPECIAL_COLUMN =
      MetadataUtils.newScalar("special", Types.required(MinorType.INT));
  private static final ColumnMetadata SPECIAL_COLUMN2 =
      MetadataUtils.newScalar("special2", Types.required(MinorType.INT));
  private static final ColumnMetadata MAP_COL = new SchemaBuilder()
        .addMap("m")
          .add("x", MinorType.BIGINT)
          .add("y", MinorType.VARCHAR)
          .resumeSchema()
        .build()
        .metadata("m");

  static {
    SchemaUtils.markExcludeFromWildcard(SPECIAL_COLUMN);
    SchemaUtils.markExcludeFromWildcard(SPECIAL_COLUMN2);
  }

  @Test
  public void testProjectAll() {
    ProjectionFilter filter =ProjectionFilter.PROJECT_ALL;
    assertFalse(filter.isEmpty());

    assertTrue(filter.isProjected(A_COL.name()));
    ProjResult result = filter.projection(A_COL);
    assertTrue(result.isProjected);
    assertNull(result.projection);

    assertTrue(filter.isProjected(MAP_COL.name()));
    result = filter.projection(MAP_COL);
    assertTrue(result.isProjected);
    assertNull(result.projection);
    assertSame(ProjectionFilter.PROJECT_ALL, result.mapFilter);

    // "Special" columns are projected only by name, but rely on
    // a property in the column itself, so give inconsistent answers
    // from the filter.
    assertTrue(filter.isProjected(SPECIAL_COLUMN.name()));
    result = filter.projection(SPECIAL_COLUMN);
    assertFalse(result.isProjected);
  }

  @Test
  public void testEmptyProjectList() {
    ProjectionFilter filter = ProjectionFilter.PROJECT_NONE;
    assertTrue(filter.isEmpty());

    assertFalse(filter.isProjected(A_COL.name()));
    ProjResult result = filter.projection(A_COL);
    assertFalse(result.isProjected);

    assertFalse(filter.isProjected(MAP_COL.name()));
    result = filter.projection(MAP_COL);
    assertFalse(result.isProjected);
    assertSame(ProjectionFilter.PROJECT_NONE, result.mapFilter);

    assertFalse(filter.isProjected(SPECIAL_COLUMN.name()));
    result = filter.projection(SPECIAL_COLUMN);
    assertFalse(result.isProjected);
  }

  @Test
  public void testProjectList() {
    ProjectionParseResult parseResult = ScanProjectionParser.parse(
        RowSetTestUtils.projectList("a", "c", "m", "special"));
    ProjectionFilter filter = new DynamicTupleFilter(
        parseResult.dynamicSchema, EmptyErrorContext.INSTANCE);
    assertFalse(filter.isEmpty());

    assertTrue(filter.isProjected(A_COL.name()));
    ProjResult result = filter.projection(A_COL);
    assertTrue(result.isProjected);
    assertNotNull(result.projection);
    assertTrue(result.projection.isDynamic());
    assertEquals(A_COL.name(), result.projection.name());

    assertFalse(filter.isProjected(B_COL.name()));
    result = filter.projection(B_COL);
    assertFalse(result.isProjected);

    assertTrue(filter.isProjected(MAP_COL.name()));
    result = filter.projection(MAP_COL);
    assertTrue(result.isProjected);
    assertNotNull(result.projection);
    assertTrue(result.projection.isDynamic());
    assertEquals(MAP_COL.name(), result.projection.name());
    assertSame(ProjectionFilter.PROJECT_ALL, result.mapFilter);

    assertTrue(filter.isProjected(SPECIAL_COLUMN.name()));
    result = filter.projection(SPECIAL_COLUMN);
    assertTrue(result.isProjected);
    assertNotNull(result.projection);
    assertTrue(result.projection.isDynamic());
    assertEquals(SPECIAL_COLUMN.name(), result.projection.name());

    assertFalse(filter.isProjected(SPECIAL_COLUMN2.name()));
    result = filter.projection(SPECIAL_COLUMN2);
    assertFalse(result.isProjected);
  }

  @Test
  public void testMapProjectList() {
    ProjectionParseResult parseResult = ScanProjectionParser.parse(
        RowSetTestUtils.projectList("m.x"));
    ProjectionFilter filter = new DynamicTupleFilter(
        parseResult.dynamicSchema, EmptyErrorContext.INSTANCE);
    assertFalse(filter.isEmpty());

    assertTrue(filter.isProjected(MAP_COL.name()));
    ProjResult result = filter.projection(MAP_COL);
    assertTrue(result.isProjected);
    assertNotNull(result.projection);
    assertTrue(result.projection.isDynamic());
    assertEquals(MAP_COL.name(), result.projection.name());
    assertTrue(result.mapFilter instanceof DynamicTupleFilter);

    ProjectionFilter mapFilter = result.mapFilter;

    ColumnMetadata x_col = MAP_COL.tupleSchema().metadata("x");
    assertTrue(mapFilter.isProjected("x"));
    result = mapFilter.projection(x_col);
    assertTrue(result.isProjected);
    assertNotNull(result.projection);
    assertTrue(result.projection.isDynamic());
    assertEquals(x_col.name(), result.projection.name());

    ColumnMetadata y_col = MAP_COL.tupleSchema().metadata("y");
    assertFalse(mapFilter.isProjected("y"));
    result = mapFilter.projection(y_col);
    assertFalse(result.isProjected);
  }
}
