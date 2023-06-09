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
package org.apache.drill.exec.physical.impl.scan.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.impl.scan.ScanTestUtils;
import org.apache.drill.exec.physical.impl.scan.project.AbstractUnresolvedColumn.UnresolvedColumn;
import org.apache.drill.exec.physical.impl.scan.project.NullColumnBuilder.NullBuilderBuilder;
import org.apache.drill.exec.physical.impl.scan.project.ResolvedTuple.ResolvedRow;
import org.apache.drill.exec.physical.rowSet.RowSetTestUtils;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.SubOperatorTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * "Schema level projection" describes one side of the projection
 * mechanism. When we project, we have the set of column the user
 * wants "the schema level" and the set of columns on offer from
 * the data source "the scan level." The projection mechanism
 * combines these to map out the actual projection.
 */

@Category(RowSetTests.class)
public class TestReaderLevelProjection extends SubOperatorTest {

  /**
   * Test wildcard projection: take all columns on offer from
   * the data source, in the order that the data source specifies.
   */

  @Test
  public void testWildcard() {
    final ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectAll(),
        ScanTestUtils.parsers());
    assertEquals(1, scanProj.columns().size());

    final TupleMetadata tableSchema = new SchemaBuilder()
        .add("a", MinorType.VARCHAR)
        .addNullable("c", MinorType.INT)
        .addArray("d", MinorType.FLOAT8)
        .buildSchema();

    final NullColumnBuilder builder = new NullBuilderBuilder().build();
    final ResolvedRow rootTuple = new ResolvedRow(builder);
    new WildcardProjection(
        scanProj, tableSchema, rootTuple,
        ScanTestUtils.resolvers());

    final List<ResolvedColumn> columns = rootTuple.columns();
    assertEquals(3, columns.size());

    assertEquals("a", columns.get(0).name());
    assertEquals(0, columns.get(0).sourceIndex());
    assertSame(rootTuple, columns.get(0).source());
    assertEquals("c", columns.get(1).name());
    assertEquals(1, columns.get(1).sourceIndex());
    assertSame(rootTuple, columns.get(1).source());
    assertEquals("d", columns.get(2).name());
    assertEquals(2, columns.get(2).sourceIndex());
    assertSame(rootTuple, columns.get(2).source());
  }

  /**
   * Test SELECT list with columns defined in a order and with
   * name case different than the early-schema table.
   */

  @Test
  public void testFullList() {

    // Simulate SELECT c, b, a ...

    final ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectList("c", "b", "a"),
        ScanTestUtils.parsers());
    assertEquals(3, scanProj.columns().size());

    // Simulate a data source, with early schema, of (a, b, c)

    final TupleMetadata tableSchema = new SchemaBuilder()
        .add("A", MinorType.VARCHAR)
        .add("B", MinorType.VARCHAR)
        .add("C", MinorType.VARCHAR)
        .buildSchema();

    final NullColumnBuilder builder = new NullBuilderBuilder().build();
    final ResolvedRow rootTuple = new ResolvedRow(builder);
    new ExplicitSchemaProjection(
        scanProj, tableSchema, rootTuple,
        ScanTestUtils.resolvers());

    final List<ResolvedColumn> columns = rootTuple.columns();
    assertEquals(3, columns.size());

    assertEquals("c", columns.get(0).name());
    assertEquals(2, columns.get(0).sourceIndex());
    assertSame(rootTuple, columns.get(0).source());

    assertEquals("b", columns.get(1).name());
    assertEquals(1, columns.get(1).sourceIndex());
    assertSame(rootTuple, columns.get(1).source());

    assertEquals("a", columns.get(2).name());
    assertEquals(0, columns.get(2).sourceIndex());
    assertSame(rootTuple, columns.get(2).source());
  }

  /**
   * Test SELECT list with columns missing from the table schema.
   */

  @Test
  public void testMissing() {

    // Simulate SELECT c, v, b, w ...

    final ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectList("c", "v", "b", "w"),
        ScanTestUtils.parsers());
    assertEquals(4, scanProj.columns().size());

    // Simulate a data source, with early schema, of (a, b, c)

    final TupleMetadata tableSchema = new SchemaBuilder()
        .add("A", MinorType.VARCHAR)
        .add("B", MinorType.VARCHAR)
        .add("C", MinorType.VARCHAR)
        .buildSchema();

    final NullColumnBuilder builder = new NullBuilderBuilder().build();
    final ResolvedRow rootTuple = new ResolvedRow(builder);
    new ExplicitSchemaProjection(
        scanProj, tableSchema, rootTuple,
        ScanTestUtils.resolvers());

    final List<ResolvedColumn> columns = rootTuple.columns();
    assertEquals(4, columns.size());
    final VectorSource nullBuilder = rootTuple.nullBuilder();

    assertEquals("c", columns.get(0).name());
    assertEquals(2, columns.get(0).sourceIndex());
    assertSame(rootTuple, columns.get(0).source());

    assertEquals("v", columns.get(1).name());
    assertEquals(0, columns.get(1).sourceIndex());
    assertSame(nullBuilder, columns.get(1).source());

    assertEquals("b", columns.get(2).name());
    assertEquals(1, columns.get(2).sourceIndex());
    assertSame(rootTuple, columns.get(2).source());

    assertEquals("w", columns.get(3).name());
    assertEquals(1, columns.get(3).sourceIndex());
    assertSame(nullBuilder, columns.get(3).source());
  }

  /**
   * Test an explicit projection (providing columns) in which the
   * names in the project lists are a different case than the data
   * source, the order of columns differs, and we ask for a
   * subset of data source columns.
   */
  @Test
  public void testSubset() {

    // Simulate SELECT c, a ...

    final ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectList("c", "a"),
        ScanTestUtils.parsers());
    assertEquals(2, scanProj.columns().size());

    // Simulate a data source, with early schema, of (a, b, c)

    final TupleMetadata tableSchema = new SchemaBuilder()
        .add("A", MinorType.VARCHAR)
        .add("B", MinorType.VARCHAR)
        .add("C", MinorType.VARCHAR)
        .buildSchema();

    final NullColumnBuilder builder = new NullBuilderBuilder().build();
    final ResolvedRow rootTuple = new ResolvedRow(builder);
    new ExplicitSchemaProjection(
        scanProj, tableSchema, rootTuple,
        ScanTestUtils.resolvers());

    final List<ResolvedColumn> columns = rootTuple.columns();
    assertEquals(2, columns.size());

    assertEquals("c", columns.get(0).name());
    assertEquals(2, columns.get(0).sourceIndex());
    assertSame(rootTuple, columns.get(0).source());

    assertEquals("a", columns.get(1).name());
    assertEquals(0, columns.get(1).sourceIndex());
    assertSame(rootTuple, columns.get(1).source());
  }

  /**
   * Drill is unique in that we can select (a, b) from a data source
   * that only offers (c, d). We get null columns as a result.
   */

  @Test
  public void testDisjoint() {

    // Simulate SELECT c, a ...

    final ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectList("b"),
        ScanTestUtils.parsers());
    assertEquals(1, scanProj.columns().size());

    // Simulate a data source, with early schema, of (a)

    final TupleMetadata tableSchema = new SchemaBuilder()
        .add("A", MinorType.VARCHAR)
        .buildSchema();

    final NullColumnBuilder builder = new NullBuilderBuilder().build();
    final ResolvedRow rootTuple = new ResolvedRow(builder);
    new ExplicitSchemaProjection(
        scanProj, tableSchema, rootTuple,
        ScanTestUtils.resolvers());

    final List<ResolvedColumn> columns = rootTuple.columns();
    assertEquals(1, columns.size());
    final VectorSource nullBuilder = rootTuple.nullBuilder();

    assertEquals("b", columns.get(0).name());
    assertEquals(0, columns.get(0).sourceIndex());
    assertSame(nullBuilder, columns.get(0).source());
  }

  /**
   * Test the obscure case that the data source contains a map, but we
   * project only one of the members of the map. The output should be a
   * map that contains only the members we request.
   */

  @Test
  public void testOmittedMap() {

    // Simulate SELECT a, b.c.d ...

    final ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectList("a", "b.c.d"),
        ScanTestUtils.parsers());
    assertEquals(2, scanProj.columns().size());
    {
      assertTrue(scanProj.columns().get(1) instanceof UnresolvedColumn);
      final UnresolvedColumn bCol = (UnresolvedColumn) (scanProj.columns().get(1));
      assertTrue(bCol.element().isTuple());
    }

    // Simulate a data source, with early schema, of (a)

    final TupleMetadata tableSchema = new SchemaBuilder()
        .add("a", MinorType.VARCHAR)
        .buildSchema();

    final NullColumnBuilder builder = new NullBuilderBuilder().build();
    final ResolvedRow rootTuple = new ResolvedRow(builder);
    new ExplicitSchemaProjection(
        scanProj, tableSchema, rootTuple,
        ScanTestUtils.resolvers());

    final List<ResolvedColumn> columns = rootTuple.columns();
    assertEquals(2, columns.size());

    // Should have resolved a to a table column, b to a missing map.

    // A is projected

    final ResolvedColumn aCol = columns.get(0);
    assertEquals("a", aCol.name());
    assertTrue(aCol instanceof ResolvedTableColumn);

    // B is not projected, is implicitly a map

    final ResolvedColumn bCol = columns.get(1);
    assertEquals("b", bCol.name());
    assertTrue(bCol instanceof ResolvedMapColumn);

    final ResolvedMapColumn bMap = (ResolvedMapColumn) bCol;
    final ResolvedTuple bMembers = bMap.members();
    assertNotNull(bMembers);
    assertEquals(1, bMembers.columns().size());

    // C is a map within b

    final ResolvedColumn cCol = bMembers.columns().get(0);
    assertTrue(cCol instanceof ResolvedMapColumn);

    final ResolvedMapColumn cMap = (ResolvedMapColumn) cCol;
    final ResolvedTuple cMembers = cMap.members();
    assertNotNull(cMembers);
    assertEquals(1, cMembers.columns().size());

    // D is an unknown column type (not a map)

    final ResolvedColumn dCol = cMembers.columns().get(0);
    assertTrue(dCol instanceof ResolvedNullColumn);
  }

  /**
   * Test of a map with missing columns.
   * table of (a{b, c}), project a.c, a.d, a.e.f
   */

  @Test
  public void testOmittedMapMembers() {

    // Simulate SELECT a.c, a.d, a.e.f ...

    final ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectList("x", "a.c", "a.d", "a.e.f", "y"),
        ScanTestUtils.parsers());
    assertEquals(3, scanProj.columns().size());

    // Simulate a data source, with early schema, of (x, y, a{b, c})

    final TupleMetadata tableSchema = new SchemaBuilder()
        .add("x", MinorType.VARCHAR)
        .add("y", MinorType.INT)
        .addMap("a")
          .add("b", MinorType.BIGINT)
          .add("c", MinorType.FLOAT8)
          .resumeSchema()
        .buildSchema();

    final NullColumnBuilder builder = new NullBuilderBuilder().build();
    final ResolvedRow rootTuple = new ResolvedRow(builder);
    new ExplicitSchemaProjection(
        scanProj, tableSchema, rootTuple,
        ScanTestUtils.resolvers());

    final List<ResolvedColumn> columns = rootTuple.columns();
    assertEquals(3, columns.size());

    // Should have resolved a.b to a map column,
    // a.d to a missing nested map, and a.e.f to a missing
    // nested map member

    // X is projected

    final ResolvedColumn xCol = columns.get(0);
    assertEquals("x", xCol.name());
    assertTrue(xCol instanceof ResolvedTableColumn);
    assertSame(rootTuple, xCol.source());
    assertEquals(0, xCol.sourceIndex());

    // Y is projected

    final ResolvedColumn yCol = columns.get(2);
    assertEquals("y", yCol.name());
    assertTrue(yCol instanceof ResolvedTableColumn);
    assertSame(rootTuple, yCol.source());
    assertEquals(1, yCol.sourceIndex());

    // A is projected

    final ResolvedColumn aCol = columns.get(1);
    assertEquals("a", aCol.name());
    assertTrue(aCol instanceof ResolvedMapColumn);

    final ResolvedMapColumn aMap = (ResolvedMapColumn) aCol;
    final ResolvedTuple aMembers = aMap.members();
    assertFalse(aMembers.isSimpleProjection());
    assertNotNull(aMembers);
    assertEquals(3, aMembers.columns().size());

    // a.c is projected

    final ResolvedColumn acCol = aMembers.columns().get(0);
    assertEquals("c", acCol.name());
    assertTrue(acCol instanceof ResolvedTableColumn);
    assertEquals(1, acCol.sourceIndex());

    // a.d is not in the table, is null

    final ResolvedColumn adCol = aMembers.columns().get(1);
    assertEquals("d", adCol.name());
    assertTrue(adCol instanceof ResolvedNullColumn);

    // a.e is not in the table, is implicitly a map

    final ResolvedColumn aeCol = aMembers.columns().get(2);
    assertEquals("e", aeCol.name());
    assertTrue(aeCol instanceof ResolvedMapColumn);

    final ResolvedMapColumn aeMap = (ResolvedMapColumn) aeCol;
    final ResolvedTuple aeMembers = aeMap.members();
    assertFalse(aeMembers.isSimpleProjection());
    assertNotNull(aeMembers);
    assertEquals(1, aeMembers.columns().size());

    // a.d.f is a null column

    final ResolvedColumn aefCol = aeMembers.columns().get(0);
    assertEquals("f", aefCol.name());
    assertTrue(aefCol instanceof ResolvedNullColumn);
  }

  /**
   * Simple map project. This is an internal case in which the
   * query asks for a set of columns inside a map, and the table
   * loader produces exactly that set. No special projection is
   * needed, the map is projected as a whole.
   */

  @Test
  public void testSimpleMapProject() {

    // Simulate SELECT a.b, a.c ...

    final ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectList("a.b", "a.c"),
        ScanTestUtils.parsers());
    assertEquals(1, scanProj.columns().size());

    // Simulate a data source, with early schema, of (a{b, c})

    final TupleMetadata tableSchema = new SchemaBuilder()
        .addMap("a")
          .add("b", MinorType.BIGINT)
          .add("c", MinorType.FLOAT8)
          .resumeSchema()
        .buildSchema();

    final NullColumnBuilder builder = new NullBuilderBuilder().build();
    final ResolvedRow rootTuple = new ResolvedRow(builder);
    new ExplicitSchemaProjection(
        scanProj, tableSchema, rootTuple,
        ScanTestUtils.resolvers());

    final List<ResolvedColumn> columns = rootTuple.columns();
    assertEquals(1, columns.size());

    // Should have resolved a.b to a map column,
    // a.d to a missing nested map, and a.e.f to a missing
    // nested map member

    // a is projected as a vector, not as a structured map

    final ResolvedColumn aCol = columns.get(0);
    assertEquals("a", aCol.name());
    assertTrue(aCol instanceof ResolvedTableColumn);
    assertSame(rootTuple, aCol.source());
    assertEquals(0, aCol.sourceIndex());
  }

  /**
   * Project of a non-map as a map
   */

  @Test
  public void testMapMismatch() {

    // Simulate SELECT a.b ...

    final ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectList("a.b"),
        ScanTestUtils.parsers());

    // Simulate a data source, with early schema, of (a)
    // where a is not a map.

    final TupleMetadata tableSchema = new SchemaBuilder()
        .add("a", MinorType.VARCHAR)
        .buildSchema();

    final NullColumnBuilder builder = new NullBuilderBuilder().build();
    final ResolvedRow rootTuple = new ResolvedRow(builder);
    try {
      new ExplicitSchemaProjection(
          scanProj, tableSchema, rootTuple,
          ScanTestUtils.resolvers());
      fail();
    } catch (final UserException e) {
      // Expected
    }
  }

  /**
   * Test project of an array. At the scan level, we just verify
   * that the requested column is, indeed, an array.
   */

  @Test
  public void testArrayProject() {

    // Simulate SELECT a[0] ...

    final ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectList("a[0]"),
        ScanTestUtils.parsers());

    // Simulate a data source, with early schema, of (a)
    // where a is not an array.

    final TupleMetadata tableSchema = new SchemaBuilder()
        .addArray("a", MinorType.VARCHAR)
        .buildSchema();

    final NullColumnBuilder builder = new NullBuilderBuilder().build();
    final ResolvedRow rootTuple = new ResolvedRow(builder);
    new ExplicitSchemaProjection(
          scanProj, tableSchema, rootTuple,
          ScanTestUtils.resolvers());

    final List<ResolvedColumn> columns = rootTuple.columns();
    assertEquals(1, columns.size());

    final ResolvedColumn aCol = columns.get(0);
    assertEquals("a", aCol.name());
    assertTrue(aCol instanceof ResolvedTableColumn);
    assertSame(rootTuple, aCol.source());
    assertEquals(0, aCol.sourceIndex());
  }

  /**
   * Project of a non-array as an array
   */

  @Test
  public void testArrayMismatch() {

    // Simulate SELECT a[0] ...

    final ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectList("a[0]"),
        ScanTestUtils.parsers());

    // Simulate a data source, with early schema, of (a)
    // where a is not an array.

    final TupleMetadata tableSchema = new SchemaBuilder()
        .add("a", MinorType.VARCHAR)
        .buildSchema();

    final NullColumnBuilder builder = new NullBuilderBuilder().build();
    final ResolvedRow rootTuple = new ResolvedRow(builder);
    try {
      new ExplicitSchemaProjection(
          scanProj, tableSchema, rootTuple,
          ScanTestUtils.resolvers());
      fail();
    } catch (final UserException e) {
      // Expected
    }
  }

  /**
   * Non-strict metadata schema. Projects all schema columns
   * followed by any "extra" reader columns.
   */
  @Test
  public void testWildcardWithSchema() {
    TupleMetadata outputSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.BIGINT)
        .buildSchema();

    // Simulate SELECT * ...

    final ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectAll(),
        ScanTestUtils.parsers(),
        outputSchema);
    assertEquals(2, scanProj.columns().size());

    // Simulate a data source, with early schema, of (b, c)

    final TupleMetadata tableSchema = new SchemaBuilder()
        .add("B", MinorType.VARCHAR)
        .add("C", MinorType.VARCHAR)
        .buildSchema();

    final NullColumnBuilder builder = new NullBuilderBuilder().build();
    final ResolvedRow rootTuple = new ResolvedRow(builder);
    new WildcardSchemaProjection(scanProj,
        tableSchema, rootTuple, ScanTestUtils.resolvers());

    final List<ResolvedColumn> columns = rootTuple.columns();
    assertEquals(3, columns.size());

    // Columns from schema preferentially use schema-defined names

    ResolvedColumn aCol = columns.get(0);
    assertEquals("a", aCol.name());
    assertTrue(aCol instanceof ResolvedNullColumn);
    assertNotNull(aCol.metadata());
    assertSame(outputSchema.metadata("a"), aCol.metadata());

    ResolvedColumn bCol = columns.get(1);
    assertEquals("b", bCol.name());
    assertTrue(bCol instanceof ResolvedTableColumn);
    assertNotNull(bCol.metadata());
    assertSame(outputSchema.metadata("b"), bCol.metadata());

    // Non-schema columns use reader names

    ResolvedColumn cCol = columns.get(2);
    assertEquals("C", cCol.name());
    assertTrue(cCol instanceof ResolvedTableColumn);
    assertNull(cCol.metadata());

    final VectorSource nullBuilder = rootTuple.nullBuilder();
    assertSame(nullBuilder, columns.get(0).source());
  }

  /**
   * Strict metadata schema. Projects output the output schema
   * columns; ignores other reader columns.
   */
  @Test
  public void testWildcardWithStrictSchema() {
    TupleMetadata outputSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.BIGINT)
        .buildSchema();
    outputSchema.setProperty(TupleMetadata.IS_STRICT_SCHEMA_PROP, Boolean.TRUE.toString());

    // Simulate SELECT * ...

    final ScanLevelProjection scanProj = ScanLevelProjection.build(
        RowSetTestUtils.projectAll(),
        ScanTestUtils.parsers(),
        outputSchema);
    assertEquals(2, scanProj.columns().size());

    // Simulate a data source, with early schema, of (b, c)

    final TupleMetadata tableSchema = new SchemaBuilder()
        .add("B", MinorType.VARCHAR)
        .add("C", MinorType.VARCHAR)
        .buildSchema();

    final NullColumnBuilder builder = new NullBuilderBuilder().build();
    final ResolvedRow rootTuple = new ResolvedRow(builder);
    new WildcardSchemaProjection(scanProj,
        tableSchema, rootTuple, ScanTestUtils.resolvers());

    final List<ResolvedColumn> columns = rootTuple.columns();
    assertEquals(2, columns.size());
    ResolvedColumn aCol = columns.get(0);
    assertEquals("a", aCol.name());
    assertTrue(aCol instanceof ResolvedNullColumn);
    assertNotNull(aCol.metadata());
    assertSame(outputSchema.metadata("a"), aCol.metadata());

    ResolvedColumn bCol = columns.get(1);
    assertEquals("b", bCol.name());
    assertTrue(bCol instanceof ResolvedTableColumn);
    assertNotNull(bCol.metadata());
    assertSame(outputSchema.metadata("b"), bCol.metadata());

    final VectorSource nullBuilder = rootTuple.nullBuilder();
    assertSame(nullBuilder, columns.get(0).source());
  }
}
