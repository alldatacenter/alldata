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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.drill.test.BaseTest;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.physical.rowSet.RowSetTestUtils;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.junit.Test;

/**
 * Projection creates a pattern which we match against a particular type
 * to see if the projection path is consistent with the type. Tests here
 * verify the consistency checks.
 */
public class TestProjectedPath extends BaseTest{

  // INT is a proxy for all scalar columns.
  private static final ColumnMetadata INT_COLUMN = intSchema().metadata("a");
  private static final ColumnMetadata INT_ARRAY_COLUMN = intArraySchema().metadata("a");
  private static final ColumnMetadata MAP_COLUMN = mapSchema().metadata("a");
  private static final ColumnMetadata MAP_ARRAY_COLUMN = mapArraySchema().metadata("a");
  private static final ColumnMetadata UNION_COLUMN = unionSchema().metadata("a");
  private static final ColumnMetadata LIST_COLUMN = listSchema().metadata("a");
  private static final ColumnMetadata DICT_INT_INT_COLUMN = dictSchema(MinorType.INT).metadata("a");
  private static final ColumnMetadata DICT_ARRAY_INT_INT_COLUMN = dictArraySchema(MinorType.INT).metadata("a");

  // Commented out for now as the projected path does not (yet) have sufficient
  // context to verify projection against a DICT. For other types, the rules are
  // based purely on the type. (a[0] is for arrays, a.b is for maps.) But, for
  // DICT, we must consider the type of the keys. So, (dict[0]) is valid if
  // either the DICT is repeated, or it has an integer key. This logic was attempted,
  // but turned out to be too complex and brittle, so was backed out for now. The tests
  // are left here as a record of what we should eventually do.
//  private static final ColumnMetadata DICT_BIGINT_INT_COLUMN = dictSchema(MinorType.BIGINT).metadata("a");
//  private static final ColumnMetadata DICT_ARRAY_BIGINT_INT_COLUMN = dictArraySchema(MinorType.BIGINT).metadata("a");
//  private static final ColumnMetadata DICT_VARCHAR_INT_COLUMN = dictSchema(MinorType.VARCHAR).metadata("a");
//  private static final ColumnMetadata DICT_ARRAY_VARCHAR_INT_COLUMN = dictArraySchema(MinorType.VARCHAR).metadata("a");
//  private static final ColumnMetadata DICT_DOUBLE_INT_COLUMN = dictSchema(MinorType.FLOAT8).metadata("a");
//  private static final ColumnMetadata DICT_ARRAY_DOUBLE_INT_COLUMN = dictArraySchema(MinorType.FLOAT8).metadata("a");
//  private static final ColumnMetadata DICT_ARRAY_INT_INT_ARRAY_COLUMN = dictArrayArraySchema(MinorType.INT).metadata("a");
//  private static final ColumnMetadata DICT_ARRAY_VARCHAR_INT_ARRAY_COLUMN = dictArrayArraySchema(MinorType.VARCHAR).metadata("a");

  private static TupleMetadata intSchema() {
    return new SchemaBuilder()
        .add("a", MinorType.INT)
        .build();
  }

  private static TupleMetadata intArraySchema() {
    return new SchemaBuilder()
        .addArray("a", MinorType.INT)
        .build();
  }

  private static TupleMetadata mapSchema() {
    return new SchemaBuilder()
        .addMap("a")
          .add("i", MinorType.INT)
          .addMap("m")
            .add("mi", MinorType.INT)
            .resumeMap()
          .resumeSchema()
        .build();
  }

  private static TupleMetadata mapArraySchema() {
    return new SchemaBuilder()
        .addMapArray("a")
          .add("i", MinorType.INT)
          .addMap("m")
            .add("mi", MinorType.INT)
            .resumeMap()
          .resumeSchema()
        .build();
  }

  private static TupleMetadata dictSchema(MinorType keyType) {
    return new SchemaBuilder()
        .addDict("a", keyType)
          .value(MinorType.INT)
          .resumeSchema()
        .build();
  }

  private static TupleMetadata dictArraySchema(MinorType keyType) {
    return new SchemaBuilder()
        .addDictArray("a", keyType)
          .value(MinorType.INT)
          .resumeSchema()
        .build();
  }

  @SuppressWarnings("unused")
  private static TupleMetadata dictArrayArraySchema(MinorType keyType) {
    return new SchemaBuilder()
        .addDictArray("a", keyType)
          .value(Types.repeated(MinorType.INT))
          .resumeSchema()
        .build();
  }

  private static TupleMetadata unionSchema() {
    return new SchemaBuilder()
        .addUnion("a")
          .addType(MinorType.INT)
          .resumeSchema()
        .build();
  }

  private static TupleMetadata listSchema() {
    return new SchemaBuilder()
        .addList("a")
          .addType(MinorType.INT)
          .resumeSchema()
        .build();
  }

  private void assertConsistent(RequestedTuple projSet, ColumnMetadata col) {
    assertTrue(ProjectionChecker.isConsistent(projSet, col));
  }

  private void assertNotConsistent(RequestedTuple projSet, ColumnMetadata col) {
    assertFalse(ProjectionChecker.isConsistent(projSet, col));
  }

  private void assertAllConsistent(RequestedTuple projSet) {
    assertConsistent(projSet, INT_COLUMN);
    assertConsistent(projSet, INT_ARRAY_COLUMN);
    assertConsistent(projSet, MAP_COLUMN);
    assertConsistent(projSet, MAP_ARRAY_COLUMN);
    assertConsistent(projSet, DICT_INT_INT_COLUMN);
    assertConsistent(projSet, DICT_ARRAY_INT_INT_COLUMN);
    assertConsistent(projSet, UNION_COLUMN);
    assertConsistent(projSet, LIST_COLUMN);
  }

  @Test
  public void testSimplePath() {
    RequestedTuple projSet = Projections.parse(
        RowSetTestUtils.projectList("a"));

    assertAllConsistent(projSet);

    // No constraints on an unprojected column.

    assertTrue(ProjectionChecker.isConsistent(projSet,
        MetadataUtils.newScalar("b", Types.required(MinorType.INT))));
  }

  @Test
  public void testProjectAll() {
    RequestedTuple projSet = Projections.parse(
        RowSetTestUtils.projectAll());

    // No constraints on wildcard projection
    assertAllConsistent(projSet);
  }

  @Test
  public void testProjectNone() {
    RequestedTuple projSet = Projections.parse(
        RowSetTestUtils.projectNone());

    // No constraints on empty projection
    assertAllConsistent(projSet);
  }

  @Test
  public void test1DArray() {
    RequestedTuple projSet = Projections.parse(
        RowSetTestUtils.projectList("a[0]"));

    assertNotConsistent(projSet, INT_COLUMN);
    assertConsistent(projSet, INT_ARRAY_COLUMN);
    assertNotConsistent(projSet, MAP_COLUMN);
    assertConsistent(projSet, MAP_ARRAY_COLUMN);
    assertConsistent(projSet, UNION_COLUMN);
    assertConsistent(projSet, LIST_COLUMN);

    assertConsistent(projSet, DICT_INT_INT_COLUMN);

    // TODO: Enforce specific DICT keys, if needed.
//    assertDictConsistent(projSet, DICT_INT_INT_COLUMN);
//    assertDictConsistent(projSet, DICT_ARRAY_INT_INT_COLUMN);
//    assertDictConsistent(projSet, DICT_ARRAY_INT_INT_ARRAY_COLUMN);
//    assertDictConsistent(projSet, DICT_BIGINT_INT_COLUMN);
//    assertDictConsistent(projSet, DICT_ARRAY_BIGINT_INT_COLUMN);
//    assertDictNotConsistent(projSet, DICT_VARCHAR_INT_COLUMN);
//    assertDictConsistent(projSet, DICT_ARRAY_VARCHAR_INT_COLUMN);
//    assertDictConsistent(projSet, DICT_ARRAY_VARCHAR_INT_ARRAY_COLUMN);
//    assertDictNotConsistent(projSet, DICT_DOUBLE_INT_COLUMN);
//    assertDictConsistent(projSet, DICT_ARRAY_DOUBLE_INT_COLUMN);
  }

  @Test
  public void test2DArray() {
    RequestedTuple projSet = Projections.parse(
        RowSetTestUtils.projectList("a[0][1]"));

    assertNotConsistent(projSet, INT_COLUMN);
    assertNotConsistent(projSet, INT_ARRAY_COLUMN);
    assertNotConsistent(projSet, MAP_COLUMN);
    assertNotConsistent(projSet, MAP_ARRAY_COLUMN);
    assertConsistent(projSet, UNION_COLUMN);
    assertConsistent(projSet, LIST_COLUMN);

    assertConsistent(projSet, DICT_INT_INT_COLUMN);

    // TODO: Enforce specific DICT keys, if needed.
//    assertDictNotConsistent(projSet, DICT_INT_INT_COLUMN);
//    assertDictConsistent(projSet, DICT_ARRAY_INT_INT_COLUMN);
//    assertDictConsistent(projSet, DICT_ARRAY_INT_INT_ARRAY_COLUMN);
//    assertDictNotConsistent(projSet, DICT_BIGINT_INT_COLUMN);
//    assertDictConsistent(projSet, DICT_ARRAY_BIGINT_INT_COLUMN);
//    assertDictNotConsistent(projSet, DICT_VARCHAR_INT_COLUMN);
//    assertDictNotConsistent(projSet, DICT_ARRAY_VARCHAR_INT_COLUMN);
//    assertDictNotConsistent(projSet, DICT_ARRAY_VARCHAR_INT_ARRAY_COLUMN);
//    assertDictNotConsistent(projSet, DICT_DOUBLE_INT_COLUMN);
//    assertDictNotConsistent(projSet, DICT_ARRAY_DOUBLE_INT_COLUMN);
  }

  @Test
  public void test3DArray() {
    RequestedTuple projSet = Projections.parse(
        RowSetTestUtils.projectList("a[0][1][2]"));

    assertNotConsistent(projSet, INT_COLUMN);
    assertNotConsistent(projSet, INT_ARRAY_COLUMN);
    assertNotConsistent(projSet, MAP_COLUMN);
    assertNotConsistent(projSet, MAP_ARRAY_COLUMN);
    assertConsistent(projSet, UNION_COLUMN);
    assertConsistent(projSet, LIST_COLUMN);

    assertConsistent(projSet, DICT_INT_INT_COLUMN);

    // TODO: Enforce specific DICT keys, if needed.
//    assertDictNotConsistent(projSet, DICT_INT_INT_COLUMN);
//    assertDictNotConsistent(projSet, DICT_ARRAY_INT_INT_COLUMN);
//    assertDictConsistent(projSet, DICT_ARRAY_INT_INT_ARRAY_COLUMN);
//    assertDictNotConsistent(projSet, DICT_BIGINT_INT_COLUMN);
//    assertDictNotConsistent(projSet, DICT_ARRAY_BIGINT_INT_COLUMN);
//    assertDictNotConsistent(projSet, DICT_VARCHAR_INT_COLUMN);
//    assertDictNotConsistent(projSet, DICT_ARRAY_VARCHAR_INT_COLUMN);
//    assertDictNotConsistent(projSet, DICT_ARRAY_VARCHAR_INT_ARRAY_COLUMN);
//    assertDictNotConsistent(projSet, DICT_DOUBLE_INT_COLUMN);
//    assertDictNotConsistent(projSet, DICT_ARRAY_DOUBLE_INT_COLUMN);
  }

  @Test
  public void testMap() {
    RequestedTuple projSet = Projections.parse(
        RowSetTestUtils.projectList("a.b"));

    assertNotConsistent(projSet, INT_COLUMN);
    assertNotConsistent(projSet, INT_ARRAY_COLUMN);
    assertConsistent(projSet, MAP_COLUMN);
    assertConsistent(projSet, MAP_ARRAY_COLUMN);

    // A UNION could contain a map, which would allow the
    // a.b path to be valid.
    assertConsistent(projSet, UNION_COLUMN);
    // A LIST could be a list of MAPs, so a.b could mean
    // to pick out the b column in all array entries.
    assertConsistent(projSet, LIST_COLUMN);

    assertConsistent(projSet, DICT_INT_INT_COLUMN);

    // TODO: Enforce specific DICT keys, if needed.
//    assertDictNotConsistent(projSet, DICT_INT_INT_COLUMN);
//    assertDictNotConsistent(projSet, DICT_ARRAY_INT_INT_COLUMN);
//    assertDictNotConsistent(projSet, DICT_ARRAY_INT_INT_ARRAY_COLUMN);
//    assertDictNotConsistent(projSet, DICT_BIGINT_INT_COLUMN);
//    assertDictNotConsistent(projSet, DICT_ARRAY_BIGINT_INT_COLUMN);
//    assertDictConsistent(projSet, DICT_VARCHAR_INT_COLUMN);
//    assertDictConsistent(projSet, DICT_ARRAY_VARCHAR_INT_COLUMN);
//    assertDictConsistent(projSet, DICT_ARRAY_VARCHAR_INT_ARRAY_COLUMN);
//    assertDictNotConsistent(projSet, DICT_DOUBLE_INT_COLUMN);
//    assertDictNotConsistent(projSet, DICT_ARRAY_DOUBLE_INT_COLUMN);
  }

  @Test
  public void testMapArray() {
    RequestedTuple projSet = Projections.parse(
        RowSetTestUtils.projectList("a[0].b"));

    assertNotConsistent(projSet, INT_COLUMN);
    assertNotConsistent(projSet, INT_ARRAY_COLUMN);
    assertNotConsistent(projSet, MAP_COLUMN);
    assertConsistent(projSet, MAP_ARRAY_COLUMN);

    // A UNION could contain a repeated map, which would allow the
    // a.b path to be valid.
    assertConsistent(projSet, UNION_COLUMN);
    // A LIST could contain MAPs.
    assertConsistent(projSet, LIST_COLUMN);

    assertConsistent(projSet, DICT_INT_INT_COLUMN);

    // TODO: Enforce specific DICT keys, if needed.
//    assertDictNotConsistent(projSet, DICT_INT_INT_COLUMN);
//    assertDictNotConsistent(projSet, DICT_ARRAY_INT_INT_COLUMN);
//    assertDictNotConsistent(projSet, DICT_ARRAY_INT_INT_ARRAY_COLUMN);
//    assertDictNotConsistent(projSet, DICT_BIGINT_INT_COLUMN);
//    assertDictNotConsistent(projSet, DICT_ARRAY_BIGINT_INT_COLUMN);
//    assertDictNotConsistent(projSet, DICT_VARCHAR_INT_COLUMN);
//    assertDictConsistent(projSet, DICT_ARRAY_VARCHAR_INT_COLUMN);
//    assertDictConsistent(projSet, DICT_ARRAY_VARCHAR_INT_ARRAY_COLUMN);
//    assertDictNotConsistent(projSet, DICT_DOUBLE_INT_COLUMN);
//    assertDictNotConsistent(projSet, DICT_ARRAY_DOUBLE_INT_COLUMN);
  }
}
