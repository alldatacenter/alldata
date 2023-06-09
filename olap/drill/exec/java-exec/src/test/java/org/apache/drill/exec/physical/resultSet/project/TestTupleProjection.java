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
package org.apache.drill.exec.physical.resultSet.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.physical.resultSet.project.RequestedTuple.TupleProjectionType;
import org.apache.drill.exec.physical.rowSet.RowSetTestUtils;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.test.BaseTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test the projection list parser: parses a list of SchemaPath
 * items into a detailed structure, handling duplicate or overlapping
 * items. Special cases the select-all (SELECT *) and select none
 * (SELECT COUNT(*)) cases.
 * <p>
 * These tests should verify everything about (runtime) projection
 * parsing; the only bits not tested here is that which is
 * inherently specific to some use case.
 */
@Category(RowSetTests.class)
public class TestTupleProjection extends BaseTest {

  private static final ColumnMetadata NORMAL_COLUMN =
      MetadataUtils.newScalar("a", Types.required(MinorType.INT));
  private static final ColumnMetadata UNPROJECTED_COLUMN =
      MetadataUtils.newScalar("bar", Types.required(MinorType.INT));
  private static final ColumnMetadata SPECIAL_COLUMN =
      MetadataUtils.newScalar("a", Types.required(MinorType.INT));
  private static final ColumnMetadata UNPROJECTED_SPECIAL_COLUMN =
      MetadataUtils.newScalar("bar", Types.required(MinorType.INT));
  private static final ColumnMetadata SPECIAL_DICT =
      MetadataUtils.newDict("a_dict");
  private static final ColumnMetadata SPECIAL_MAP =
      MetadataUtils.newMap("a_map");
  private static final ColumnMetadata SPECIAL_REP_LIST =
      MetadataUtils.newRepeatedList(
        "a_repeated_list",
        MetadataUtils.newScalar("child", Types.repeated(MinorType.INT))
      );
  private static final ColumnMetadata SPECIAL_VARIANT =
      MetadataUtils.newVariant("a_variant", DataMode.OPTIONAL);

  static {
    SPECIAL_COLUMN.setBooleanProperty(ColumnMetadata.EXCLUDE_FROM_WILDCARD, true);
    UNPROJECTED_SPECIAL_COLUMN.setBooleanProperty(ColumnMetadata.EXCLUDE_FROM_WILDCARD, true);
    SPECIAL_DICT.setBooleanProperty(ColumnMetadata.EXCLUDE_FROM_WILDCARD, true);
    SPECIAL_MAP.setBooleanProperty(ColumnMetadata.EXCLUDE_FROM_WILDCARD, true);
    SPECIAL_REP_LIST.setBooleanProperty(ColumnMetadata.EXCLUDE_FROM_WILDCARD, true);
    SPECIAL_VARIANT.setBooleanProperty(ColumnMetadata.EXCLUDE_FROM_WILDCARD, true);
  }

  /**
   * Null map means everything is projected
   */
  @Test
  public void testProjectionAll() {
    RequestedTuple projSet = Projections.parse(null);
    assertSame(TupleProjectionType.ALL, projSet.type());
    assertTrue(projSet.isProjected("foo"));
    assertTrue(projSet.isProjected(NORMAL_COLUMN));
    assertFalse(projSet.isProjected(SPECIAL_COLUMN));
    assertFalse(projSet.isProjected(SPECIAL_DICT));
    assertFalse(projSet.isProjected(SPECIAL_MAP));
    assertFalse(projSet.isProjected(SPECIAL_REP_LIST));
    assertFalse(projSet.isProjected(SPECIAL_VARIANT));
    assertTrue(projSet.projections().isEmpty());
    assertFalse(projSet.isEmpty());
  }

  /**
   * SELECT * means everything is projected
   */
  @Test
  public void testWildcard() {
    RequestedTuple projSet = Projections.parse(RowSetTestUtils.projectAll());
    assertSame(TupleProjectionType.ALL, projSet.type());
    assertTrue(projSet.isProjected("foo"));
    assertNull(projSet.get("foo"));
    assertTrue(projSet.isProjected(NORMAL_COLUMN));
    assertFalse(projSet.isProjected(SPECIAL_COLUMN));
    assertFalse(projSet.isProjected(SPECIAL_DICT));
    assertFalse(projSet.isProjected(SPECIAL_MAP));
    assertFalse(projSet.isProjected(SPECIAL_REP_LIST));
    assertFalse(projSet.isProjected(SPECIAL_VARIANT));
    assertEquals(1, projSet.projections().size());
    assertFalse(projSet.isEmpty());
  }

  /**
   * Test an empty projection which occurs in a
   * SELECT COUNT(*) query.
   * Empty list means nothing is projected.
   */
  @Test
  public void testProjectionNone() {
    RequestedTuple projSet = Projections.parse(new ArrayList<SchemaPath>());
    assertSame(TupleProjectionType.NONE, projSet.type());
    assertFalse(projSet.isProjected("foo"));
    assertFalse(projSet.isProjected(NORMAL_COLUMN));
    assertFalse(projSet.isProjected(SPECIAL_COLUMN));
    assertFalse(projSet.isProjected(SPECIAL_DICT));
    assertFalse(projSet.isProjected(SPECIAL_MAP));
    assertFalse(projSet.isProjected(SPECIAL_REP_LIST));
    assertFalse(projSet.isProjected(SPECIAL_VARIANT));
    assertTrue(projSet.projections().isEmpty());
    assertTrue(projSet.isEmpty());
  }

  /**
   * Simple non-map columns
   */
  @Test
  public void testProjectionSimple() {
    RequestedTuple projSet = Projections.parse(
        RowSetTestUtils.projectList("a", "b", "c"));
    assertSame(TupleProjectionType.SOME, projSet.type());
    assertTrue(projSet.isProjected("a"));
    assertTrue(projSet.isProjected("b"));
    assertTrue(projSet.isProjected("c"));
    assertFalse(projSet.isProjected("d"));

    assertTrue(projSet.isProjected(NORMAL_COLUMN));
    assertTrue(projSet.isProjected(SPECIAL_COLUMN));
    assertFalse(projSet.isProjected(UNPROJECTED_COLUMN));
    assertFalse(projSet.isProjected(UNPROJECTED_SPECIAL_COLUMN));
    assertFalse(projSet.isProjected(SPECIAL_MAP));

    List<RequestedColumn> cols = projSet.projections();
    assertEquals(3, cols.size());

    RequestedColumn a = cols.get(0);
    assertEquals("a", a.name());
    assertTrue(a.isSimple());
    assertFalse(a.isArray());
    assertFalse(a.isTuple());
    assertFalse(projSet.isEmpty());
  }

  /**
   * The projection set does not enforce uniqueness.
   */
  @Test
  public void testSimpleDups() {
    RequestedTuple projSet = Projections.parse(
        RowSetTestUtils.projectList("a", "b", "a"));
    assertSame(TupleProjectionType.SOME, projSet.type());
    assertEquals(2, projSet.projections().size());
    assertEquals(2, ((RequestedColumnImpl) projSet.get("a")).refCount());
  }

  /**
   * Whole-map projection (note, fully projected maps are
   * identical to projected simple columns at this level of
   * abstraction.)
   */
  @Test
  public void testProjectionWholeMap() {
    RequestedTuple projSet = Projections.parse(
        RowSetTestUtils.projectList("map"));

    assertSame(TupleProjectionType.SOME, projSet.type());
    assertTrue(projSet.isProjected("map"));
    assertFalse(projSet.isProjected("another"));

    RequestedTuple mapProj = projSet.mapProjection("map");
    assertNotNull(mapProj);
    assertSame(TupleProjectionType.ALL, mapProj.type());
    assertTrue(mapProj.isProjected("foo"));

    RequestedTuple anotherProj = projSet.mapProjection("another");
    assertNotNull(anotherProj);
    assertSame(TupleProjectionType.NONE, anotherProj.type());
    assertFalse(anotherProj.isProjected("anyCol"));
  }

  /**
   * Selected map projection, multiple levels, full projection
   * at leaf level.
   */
  @Test
  public void testProjectionMapSubset() {
    RequestedTuple projSet = Projections.parse(
        RowSetTestUtils.projectList("map.a", "map.b", "map.map2.x"));
    assertSame(TupleProjectionType.SOME, projSet.type());

    // Map itself is projected and has a map qualifier
    assertTrue(projSet.isProjected("map"));

    // Map: an explicit map at top level

    RequestedTuple mapProj = projSet.mapProjection("map");
    assertSame(TupleProjectionType.SOME, projSet.type());
    assertTrue(mapProj.isProjected("a"));
    assertTrue(mapProj.isProjected("b"));
    assertTrue(mapProj.isProjected("map2"));
    assertFalse(mapProj.isProjected("bogus"));

    // Map b: an implied nested map

    assertTrue(mapProj.get("b").isSimple());
    RequestedTuple bMapProj = mapProj.mapProjection("b");
    assertNotNull(bMapProj);
    assertSame(TupleProjectionType.ALL, bMapProj.type());
    assertTrue(bMapProj.isProjected("foo"));

    // Map2, an nested map, has an explicit projection

    RequestedTuple map2Proj = mapProj.mapProjection("map2");
    assertNotNull(map2Proj);
    assertSame(TupleProjectionType.SOME, map2Proj.type());
    assertTrue(map2Proj.isProjected("x"));
    assertFalse(map2Proj.isProjected("bogus"));
  }

  /**
   * Project both a map member and the entire map.
   */
  @Test
  public void testProjectionMapAndSimple() {
    RequestedTuple projSet = Projections.parse(
        RowSetTestUtils.projectList("map.a", "map"));

    RequestedTuple mapProj = projSet.mapProjection("map");
    assertSame(TupleProjectionType.ALL, mapProj.type());
    assertTrue(mapProj.isProjected("a"));
    assertTrue(mapProj.isProjected("b"));
  }

  /**
   * Project both an entire map and a map member.
   */
  @Test
  public void testProjectionSimpleAndMap() {
    RequestedTuple projSet = Projections.parse(
        RowSetTestUtils.projectList("map", "map.a"));

    RequestedTuple mapProj = projSet.mapProjection("map");
    assertSame(TupleProjectionType.ALL, mapProj.type());
    assertTrue(mapProj.isProjected("a"));
    assertTrue(mapProj.isProjected("b"));
  }

  /**
   * Project both a map member and the entire map.
   */
  @Test
  public void testProjectionMapAndWildcard() {

    // Built up by hand because "map.*" is not valid Drill
    // expression syntax.
    List<SchemaPath> projCols = new ArrayList<>();
    projCols.add(SchemaPath.getCompoundPath("map", "a"));
    projCols.add(SchemaPath.getCompoundPath("map", SchemaPath.DYNAMIC_STAR));

    RequestedTuple projSet = Projections.parse(projCols);
    RequestedTuple mapProj = projSet.mapProjection("map");
    assertSame(TupleProjectionType.ALL, mapProj.type());
    assertTrue(mapProj.isProjected("a"));
    assertTrue(mapProj.isProjected("b"));
  }

  /**
   * Project both an entire map and a map member.
   */
  @Test
  public void testProjectionWildcardAndMap() {

    List<SchemaPath> projCols = new ArrayList<>();
    projCols.add(SchemaPath.getCompoundPath("map", SchemaPath.DYNAMIC_STAR));
    projCols.add(SchemaPath.getCompoundPath("map", "a"));

    RequestedTuple projSet = Projections.parse(projCols);
    RequestedTuple mapProj = projSet.mapProjection("map");
    assertSame(TupleProjectionType.ALL, mapProj.type());
    assertTrue(mapProj.isProjected("a"));
    assertTrue(mapProj.isProjected("b"));
  }

  @Test
  public void testMapDetails() {
    RequestedTuple projSet = Projections.parse(
        RowSetTestUtils.projectList("a.b.c", "a.c", "d"));
    List<RequestedColumn> cols = projSet.projections();
    assertEquals(2, cols.size());

    RequestedColumn a = cols.get(0);
    assertEquals("a", a.name());
    assertFalse(a.isSimple());
    assertFalse(a.isArray());
    assertTrue(a.isTuple());

    // a{}
    assertNotNull(a.tuple());
    List<RequestedColumn> aMembers = a.tuple().projections();
    assertEquals(2, aMembers.size());

    // a.b
    RequestedColumn a_b = aMembers.get(0);
    assertEquals("b", a_b.name());
    assertTrue(a_b.isTuple());

    // a.b{}
    assertNotNull(a_b.tuple());
    List<RequestedColumn> a_bMembers = a_b.tuple().projections();
    assertEquals(1, a_bMembers.size());

    // a.b.c
    assertEquals("c", a_bMembers.get(0).name());
    assertTrue(a_bMembers.get(0).isSimple());

    // a.c
    assertEquals("c", aMembers.get(1).name());
    assertTrue(aMembers.get(1).isSimple());

    // d
    assertEquals("d", cols.get(1).name());
    assertTrue(cols.get(1).isSimple());
  }

  /**
   * Duplicate column names are merged for projection.
   */
  @Test
  public void testMapDups() {
    RequestedTuple projSet = Projections.parse(
        RowSetTestUtils.projectList("a.b", "a.c", "a.b"));

    RequestedTuple aMap = projSet.mapProjection("a");
    assertEquals(2, aMap.projections().size());
    assertEquals(2, ((RequestedColumnImpl) aMap.get("b")).refCount());
  }

  @Test
  public void testArray() {
    RequestedTuple projSet = Projections.parse(
        RowSetTestUtils.projectList("a[1]", "a[3]"));
    List<RequestedColumn> cols = projSet.projections();
    assertEquals(1, cols.size());

    RequestedColumn a = cols.get(0);
    assertEquals("a", a.name());
    assertTrue(a.isArray());
    assertEquals(1, a.arrayDims());
    assertFalse(a.isSimple());
    assertFalse(a.isTuple());
    assertTrue(a.hasIndexes());
    boolean indexes[] = a.indexes();
    assertNotNull(indexes);
    assertEquals(4, indexes.length);
    assertFalse(indexes[0]);
    assertTrue(indexes[1]);
    assertFalse(indexes[2]);
    assertTrue(indexes[3]);
  }

  @Test
  public void testMultiDimArray() {
    RequestedTuple projSet = Projections.parse(
        RowSetTestUtils.projectList("a[0][1][2]", "a[2][3]"));
    List<RequestedColumn> cols = projSet.projections();
    assertEquals(1, cols.size());

    RequestedColumn a = cols.get(0);
    assertEquals("a", a.name());
    assertTrue(a.isArray());
    // Dimension count is the maximum seen.
    assertEquals(3, a.arrayDims());
    assertFalse(a.isSimple());
    assertFalse(a.isTuple());
    boolean[] indexes = a.indexes();
    assertNotNull(indexes);
    assertEquals(3, indexes.length);
    assertTrue(indexes[0]);
    assertFalse(indexes[1]);
    assertTrue(indexes[2]);
  }

  /**
   * Duplicate array entries are allowed to handle the
   * use case of a[1], a[1].z. Each element is reported once;
   * the project operator will create copies as needed.
   */
  @Test
  public void testArrayDupsIgnored() {
    RequestedTuple projSet = Projections.parse(
        RowSetTestUtils.projectList("a[1]", "a[3]", "a[1]", "a[3].z"));

    List<RequestedColumn> cols = projSet.projections();
    assertEquals(1, cols.size());

    RequestedColumn a = cols.get(0);
    assertEquals("a", a.name());
    assertTrue(a.isArray());
    boolean indexes[] = a.indexes();
    assertNotNull(indexes);
    assertEquals(4, indexes.length);
    assertFalse(indexes[0]);
    assertTrue(indexes[1]);
    assertFalse(indexes[2]);
    assertTrue(indexes[3]);
  }

  @Test
  public void testArrayAndSimple() {
    RequestedTuple projSet = Projections.parse(
        RowSetTestUtils.projectList("a[1]", "a"));
    List<RequestedColumn> cols = projSet.projections();
    assertEquals(1, cols.size());

    RequestedColumn a = cols.get(0);
    assertEquals("a", a.name());
    assertTrue(a.isArray());
    assertNull(a.indexes());
  }

  @Test
  public void testSimpleAndArray() {
    RequestedTuple projSet = Projections.parse(
        RowSetTestUtils.projectList("a", "a[1]"));
    List<RequestedColumn> cols = projSet.projections();
    assertEquals(1, cols.size());

    RequestedColumn a = cols.get(0);
    assertEquals("a", a.name());
    assertTrue(a.isArray());
    assertFalse(a.hasIndexes());
    assertNull(a.indexes());
  }

  @Test
  // Drill syntax does not support map arrays
  public void testMapArray() {
    RequestedTuple projSet = Projections.parse(
        RowSetTestUtils.projectList("a[1].x"));
    List<RequestedColumn> cols = projSet.projections();
    assertEquals(1, cols.size());

    RequestedColumn a = cols.get(0);

    // Column acts like an array
    assertTrue(a.isArray());
    assertTrue(a.hasIndexes());
    assertEquals(1, a.arrayDims());

    // And the column acts like a map
    assertTrue(a.isTuple());
    RequestedTuple aProj = a.tuple();
    assertSame(TupleProjectionType.SOME, aProj.type());
    assertTrue(aProj.isProjected("x"));
    assertFalse(aProj.isProjected("y"));
  }

  @Test
  // Drill syntax does not support map arrays
  public void testMap2DArray() {
    RequestedTuple projSet = Projections.parse(
        RowSetTestUtils.projectList("a[1][2].x"));
    List<RequestedColumn> cols = projSet.projections();
    assertEquals(1, cols.size());

    RequestedColumn a = cols.get(0);

    // Column acts like an array
    assertTrue(a.isArray());
    assertTrue(a.hasIndexes());

    // Note that the multiple dimensions are inferred only through
    // the multiple levels of qualifiers.

    // And the column acts like a map
    assertTrue(a.isTuple());
    RequestedTuple aProj = a.tuple();
    assertSame(TupleProjectionType.SOME, aProj.type());
    assertTrue(aProj.isProjected("x"));
    assertFalse(aProj.isProjected("y"));
  }

  /**
   * Projection does not enforce semantics; it just report what it
   * sees. This allows cases such as m.a and m[0], which might mean
   * that m is a map array, m.a wants an array of a-member values, and m[0]
   * wants the first map in the array. Not clear Drill actually supports
   * these cases, however.
   */
  @Test
  public void testArrayAndMap() {
    RequestedTuple projSet = Projections.parse(
        RowSetTestUtils.projectList("m.a", "m[0]"));
    RequestedColumn m = projSet.get("m");
    assertTrue(m.isArray());
    assertEquals(1, m.arrayDims());
    assertTrue(m.isTuple());
    assertTrue(m.tuple().isProjected("a"));
    assertFalse(m.tuple().isProjected("b"));
  }

  @Test
  public void testMapAndArray() {
    RequestedTuple projSet = Projections.parse(
        RowSetTestUtils.projectList("m[0]", "m.a"));
    RequestedColumn m = projSet.get("m");
    assertTrue(m.isArray());
    assertEquals(1, m.arrayDims());
    assertTrue(m.isTuple());
    assertTrue(m.tuple().isProjected("a"));
    // m[0] requests the entire tuple
    assertTrue(m.tuple().isProjected("b"));
  }
}
