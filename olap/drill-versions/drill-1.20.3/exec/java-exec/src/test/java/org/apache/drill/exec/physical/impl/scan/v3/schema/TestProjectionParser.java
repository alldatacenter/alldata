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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.drill.categories.EvfTest;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.impl.scan.v3.schema.ScanProjectionParser.ProjectionParseResult;
import org.apache.drill.exec.physical.rowSet.RowSetTestUtils;
import org.apache.drill.exec.record.metadata.TupleMetadata;
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
@Category(EvfTest.class)
public class TestProjectionParser extends BaseTest {

  /**
   * Null map means everything is projected
   */
  @Test
  public void testProjectionAll() {
    ProjectionParseResult result = ScanProjectionParser.parse(null);
    assertEquals(0, result.wildcardPosn);
    TupleMetadata projSet = result.dynamicSchema;
    assertTrue(SchemaUtils.isProjectAll(projSet));
    assertFalse(SchemaUtils.isProjectNone(projSet));
    assertTrue(projSet.isEmpty());
  }

  /**
   * SELECT * means everything is projected
   */
  @Test
  public void testWildcard() {
    ProjectionParseResult result = ScanProjectionParser.parse(
        RowSetTestUtils.projectAll());
    assertEquals(0, result.wildcardPosn);
    TupleMetadata projSet = result.dynamicSchema;
    assertTrue(SchemaUtils.isProjectAll(projSet));
    assertFalse(SchemaUtils.isProjectNone(projSet));
    assertTrue(projSet.isEmpty());
  }

  /**
   * Test an empty projection which occurs in a
   * SELECT COUNT(*) query.
   * Empty list means nothing is projected.
   */
  @Test
  public void testProjectionNone() {
    ProjectionParseResult result = ScanProjectionParser.parse(
        Collections.emptyList());
    assertEquals(-1, result.wildcardPosn);
    TupleMetadata projSet = result.dynamicSchema;
    assertFalse(SchemaUtils.isProjectAll(projSet));
    assertTrue(SchemaUtils.isProjectNone(projSet));
    assertTrue(projSet.isEmpty());
  }

  /**
   * Simple non-map columns
   */
  @Test
  public void testProjectionSimple() {
    ProjectionParseResult result = ScanProjectionParser.parse(
        RowSetTestUtils.projectList("a", "b", "c"));
    assertEquals(-1, result.wildcardPosn);
    TupleMetadata projSet = result.dynamicSchema;
    assertFalse(SchemaUtils.isProjectAll(projSet));
    assertFalse(SchemaUtils.isProjectNone(projSet));
    assertNotNull(projSet.metadata("a"));
    assertNotNull(projSet.metadata("b"));
    assertNotNull(projSet.metadata("c"));
    assertNull(projSet.metadata("d"));
    assertEquals(3, projSet.size());

    ProjectedColumn a = (ProjectedColumn) projSet.metadata(0);
    assertEquals("a", a.name());
    assertTrue(a.isSimple());
    assertFalse(a.isArray());
    assertFalse(a.isMap());
    assertNull(a.tupleSchema());
  }

  /**
   * The projection set does not enforce uniqueness.
   */
  @Test
  public void testSimpleDups() {
    ProjectionParseResult result = ScanProjectionParser.parse(
        RowSetTestUtils.projectList("a", "b", "a"));
    assertEquals(-1, result.wildcardPosn);
    TupleMetadata projSet = result.dynamicSchema;
    assertEquals(2, projSet.size());
    assertEquals(2, ((ProjectedColumn) projSet.metadata("a")).refCount());
  }

  /**
   * Selected map projection, multiple levels, full projection
   * at leaf level.
   */
  @Test
  public void testProjectionMapSubset() {
    ProjectionParseResult result = ScanProjectionParser.parse(
        RowSetTestUtils.projectList("map.a", "map.b", "map.map2.x"));
    TupleMetadata projSet = result.dynamicSchema;

    // Map itself is projected and has a map qualifier
    ProjectedColumn map = (ProjectedColumn) projSet.metadata("map");
    assertNotNull(map);

    // Map: an explicit map-like at top level
    assertTrue(map.isMap());
    TupleMetadata mapProj = map.tupleSchema();
    assertNotNull(mapProj);
    assertFalse(SchemaUtils.isProjectAll(mapProj));
    assertFalse(SchemaUtils.isProjectNone(mapProj));

    assertNotNull(mapProj.metadata("a"));
    assertNotNull(mapProj.metadata("b"));
    assertNotNull(mapProj.metadata("map2"));
    assertNull(mapProj.metadata("bogus"));

    // Map b: an implied nested map
    ProjectedColumn b = (ProjectedColumn) mapProj.metadata("b");
    assertTrue(b.isSimple());

    // Map2, an nested map, has an explicit projection
    ProjectedColumn map2 = (ProjectedColumn) mapProj.metadata("map2");
    TupleMetadata map2Proj = map2.tupleSchema();
    assertNotNull(map2Proj);
    assertFalse(SchemaUtils.isProjectAll(map2Proj));
    assertNotNull(map2Proj.metadata("x"));
    assertNull(map2Proj.metadata("bogus"));
  }

  /**
   * Project both a map member and the entire map.
   */
  @Test
  public void testProjectionMapAndSimple() {
    ProjectionParseResult result = ScanProjectionParser.parse(
        RowSetTestUtils.projectList("map.a", "map"));
    doTestMapAndSimple(result.dynamicSchema);
  }

  /**
   * Project both an entire map and a map member.
   */
  @Test
  public void testProjectionSimpleAndMap() {
    ProjectionParseResult result = ScanProjectionParser.parse(
        RowSetTestUtils.projectList("map", "map.a"));
    doTestMapAndSimple(result.dynamicSchema);
  }

  private void doTestMapAndSimple(TupleMetadata projSet) {
    ProjectedColumn map = (ProjectedColumn) projSet.metadata("map");
    assertNotNull(map);
    assertTrue(map.isMap());
    TupleMetadata mapProj = map.tupleSchema();
    assertNotNull(mapProj);
    assertTrue(SchemaUtils.isProjectAll(mapProj));
    assertNotNull(mapProj.metadata("a"));
    assertNull(mapProj.metadata("b"));
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

    ProjectionParseResult result = ScanProjectionParser.parse(projCols);
    assertEquals(-1, result.wildcardPosn);
    TupleMetadata projSet = result.dynamicSchema;
    doTestMapAndSimple(projSet);
  }

  /**
   * Project both an entire map and a map member.
   */
  @Test
  public void testProjectionWildcardAndMap() {

    List<SchemaPath> projCols = new ArrayList<>();
    projCols.add(SchemaPath.getCompoundPath("map", SchemaPath.DYNAMIC_STAR));
    projCols.add(SchemaPath.getCompoundPath("map", "a"));

    ProjectionParseResult result = ScanProjectionParser.parse(projCols);
    assertEquals(-1, result.wildcardPosn);
    TupleMetadata projSet = result.dynamicSchema;
    doTestMapAndSimple(projSet);
  }

  @Test
  public void testMapDetails() {
    ProjectionParseResult result = ScanProjectionParser.parse(
        RowSetTestUtils.projectList("a.b.c", "a.c", "d"));
    TupleMetadata projSet = result.dynamicSchema;
    assertEquals(2, projSet.size());

    ProjectedColumn a = (ProjectedColumn) projSet.metadata(0);
    assertEquals("a", a.name());
    assertFalse(a.isSimple());
    assertFalse(a.isArray());
    assertTrue(a.isMap());

    // a{}
    TupleMetadata aMembers = a.tupleSchema();
    assertNotNull(aMembers);
    assertEquals(2, aMembers.size());

    // a.b
    ProjectedColumn a_b = (ProjectedColumn) aMembers.metadata(0);
    assertEquals("b", a_b.name());
    assertTrue(a_b.isMap());

    // a.b{}
    TupleMetadata a_bMembers = a_b.tupleSchema();
    assertNotNull(a_bMembers);
    assertEquals(1, a_bMembers.size());

    // a.b.c
    ProjectedColumn a_b_c = (ProjectedColumn) a_bMembers.metadata(0);
    assertNotNull(a_b_c);
    assertEquals("c", a_b_c.name());
    assertTrue(a_b_c.isSimple());

    // a.c
    ProjectedColumn a_c = (ProjectedColumn) aMembers.metadata(1);
    assertNotNull(a_c);
    assertEquals("c", a_c.name());
    assertTrue(a_c.isSimple());

    // d
    ProjectedColumn d = (ProjectedColumn) projSet.metadata(1);
    assertNotNull(d);
    assertEquals("d", d.name());
    assertTrue(d.isSimple());
  }

  /**
   * Duplicate column names are merged for projection.
   */
  @Test
  public void testMapDups() {
    ProjectionParseResult result = ScanProjectionParser.parse(
        RowSetTestUtils.projectList("a.b", "a.c", "a.b"));
    TupleMetadata projSet = result.dynamicSchema;

    ProjectedColumn a = (ProjectedColumn) projSet.metadata("a");
    TupleMetadata aMap = a.tupleSchema();
    assertEquals(2, aMap.size());
    assertEquals(2, ((ProjectedColumn) aMap.metadata("b")).refCount());
  }

  @Test
  public void testArray() {
    ProjectionParseResult result = ScanProjectionParser.parse(
        RowSetTestUtils.projectList("a[1]", "a[3]"));
    TupleMetadata projSet = result.dynamicSchema;
    assertEquals(1, projSet.size());

    ProjectedColumn a = (ProjectedColumn) projSet.metadata(0);
    assertEquals("a", a.name());
    assertTrue(a.isArray());
    assertEquals(1, a.arrayDims());
    assertFalse(a.isSimple());
    assertFalse(a.isMap());
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
    ProjectionParseResult result = ScanProjectionParser.parse(
        RowSetTestUtils.projectList("a[0][1][2]", "a[2][3]"));
    TupleMetadata projSet = result.dynamicSchema;
    assertEquals(1, projSet.size());

    ProjectedColumn a = (ProjectedColumn) projSet.metadata(0);
    assertEquals("a", a.name());
    assertTrue(a.isArray());

    // Dimension count is the maximum seen.
    assertEquals(3, a.arrayDims());
    assertFalse(a.isSimple());
    assertFalse(a.isMap());

    // Indexes only at the first dimension
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
    ProjectionParseResult result = ScanProjectionParser.parse(
        RowSetTestUtils.projectList("a[1]", "a[3]", "a[1]", "a[3].z"));
    TupleMetadata projSet = result.dynamicSchema;
    assertEquals(1, projSet.size());

    ProjectedColumn a = (ProjectedColumn) projSet.metadata(0);
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
    ProjectionParseResult result = ScanProjectionParser.parse(
        RowSetTestUtils.projectList("a[1]", "a"));
    doTestArrayAndSimple(result.dynamicSchema);
  }

  @Test
  public void testSimpleAndArray() {
    ProjectionParseResult result = ScanProjectionParser.parse(
        RowSetTestUtils.projectList("a", "a[1]"));
    doTestArrayAndSimple(result.dynamicSchema);
  }

  private void doTestArrayAndSimple(TupleMetadata projSet) {
    assertEquals(1, projSet.size());
    ProjectedColumn a = (ProjectedColumn) projSet.metadata(0);
    assertEquals("a", a.name());
    assertTrue(a.isArray());
    assertNull(a.indexes());
  }

  @Test
  // Drill syntax does not support map arrays
  public void testMapArray() {
    ProjectionParseResult result = ScanProjectionParser.parse(
        RowSetTestUtils.projectList("a[1].x"));
    TupleMetadata projSet = result.dynamicSchema;
    assertEquals(1, projSet.size());

    ProjectedColumn a = (ProjectedColumn) projSet.metadata(0);

    // Column acts like an array
    assertTrue(a.isArray());
    assertTrue(a.hasIndexes());
    assertEquals(1, a.arrayDims());

    // And the column acts like a map
    assertTrue(a.isMap());
    TupleMetadata aProj = a.tupleSchema();
    assertFalse(SchemaUtils.isProjectAll(aProj));
    assertFalse(SchemaUtils.isProjectNone(aProj));
    assertNotNull(aProj.metadata("x"));
    assertNull(aProj.metadata("y"));
  }

  @Test
  // Drill syntax does not support map arrays
  public void testMap2DArray() {
    ProjectionParseResult result = ScanProjectionParser.parse(
        RowSetTestUtils.projectList("a[1][2].x"));
    TupleMetadata projSet = result.dynamicSchema;
    assertEquals(1, projSet.size());

    ProjectedColumn a = (ProjectedColumn) projSet.metadata(0);

    // Column acts like an array
    assertTrue(a.isArray());
    assertTrue(a.hasIndexes());

    // Note that the multiple dimensions are inferred only through
    // the multiple levels of qualifiers.

    // And the column acts like a map
    assertTrue(a.isMap());
    TupleMetadata aProj = a.tupleSchema();
    assertNotNull(aProj.metadata("x"));
    assertNull(aProj.metadata("y"));
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
    ProjectionParseResult result = ScanProjectionParser.parse(
        RowSetTestUtils.projectList("m.a", "m[0]"));
    doTestArrayAndMap(result.dynamicSchema);
  }

  @Test
  public void testMapAndArray() {
    ProjectionParseResult result = ScanProjectionParser.parse(
        RowSetTestUtils.projectList("m[0]", "m.a"));
    doTestArrayAndMap(result.dynamicSchema);
  }

  private void doTestArrayAndMap(TupleMetadata projSet) {
    assertEquals(1, projSet.size());
    ProjectedColumn m = (ProjectedColumn) projSet.metadata("m");
    assertTrue(m.isArray());
    assertEquals(1, m.arrayDims());
    assertTrue(m.isMap());
    TupleMetadata mProj = m.tupleSchema();
    assertNotNull(mProj.metadata("a"));
    assertNull(mProj.metadata("b"));
  }
}
