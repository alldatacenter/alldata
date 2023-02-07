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
package org.apache.drill.exec.physical.rowSet;

import static org.apache.drill.test.rowSet.RowSetUtilities.mapArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.mapValue;
import static org.apache.drill.test.rowSet.RowSetUtilities.strArray;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.accessor.ArrayReader;
import org.apache.drill.exec.vector.accessor.ArrayWriter;
import org.apache.drill.exec.vector.accessor.ScalarReader;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.accessor.TupleReader;
import org.apache.drill.exec.vector.accessor.TupleWriter;
import org.apache.drill.exec.vector.complex.MapVector;
import org.apache.drill.exec.vector.complex.RepeatedMapVector;
import org.apache.drill.test.ClientFixture;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.SubOperatorTest;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test map support in the column readers and writers.
 * <p>
 * The tests here are a simplified form of those in
 * TestResultSetLoaderMaps -- the RowSet mechanism requires a fixed
 * schema, which makes this mechanism far simpler.
 */

@Category(RowSetTests.class)
public class TestMapAccessors extends SubOperatorTest {

  @Test
  public void testBasics() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("c", MinorType.INT)
          .add("d", MinorType.VARCHAR)
          .resumeSchema()
        .add("e", MinorType.VARCHAR)
        .buildSchema();

    RowSetBuilder builder = fixture.rowSetBuilder(schema);
    RowSetWriter rootWriter = builder.writer();

    // Verify structure and schema

    final TupleMetadata actualSchema = rootWriter.tupleSchema();
    assertEquals(3, actualSchema.size());
    assertTrue(actualSchema.metadata(1).isMap());
    assertEquals(2, actualSchema.metadata("m").tupleSchema().size());
    assertEquals(2, actualSchema.column("m").getChildren().size());

    // Write a row the way that clients will do.

    final ScalarWriter aWriter = rootWriter.scalar("a");
    final TupleWriter mWriter = rootWriter.tuple("m");
    final ScalarWriter cWriter = mWriter.scalar("c");
    final ScalarWriter dWriter = mWriter.scalar("d");
    final ScalarWriter eWriter = rootWriter.scalar("e");

    aWriter.setInt(10);
    cWriter.setInt(110);
    dWriter.setString("fred");
    eWriter.setString("pebbles");
    rootWriter.save();

    // Write another using the test-time conveniences

    rootWriter.addRow(20, mapValue(210, "barney"), "bam-bam");

    RowSet result = builder.build();
    assertEquals(2, result.rowCount());

    // Validate internal structure.

    VectorContainer container = result.container();
    assertEquals(3, container.getNumberOfColumns());
    ValueVector v = container.getValueVector(1).getValueVector();
    assertTrue(v instanceof MapVector);
    MapVector mv = (MapVector) v;
    assertEquals(2, mv.getAccessor().getValueCount());

    // Validate data. Do so using the readers to avoid verifying
    // using the very mechanisms we want to test.

    RowSetReader rootReader = result.reader();
    final ScalarReader aReader = rootReader.scalar("a");
    final TupleReader mReader = rootReader.tuple("m");
    final ScalarReader cReader = mReader.scalar("c");
    final ScalarReader dReader = mReader.scalar("d");
    final ScalarReader eReader = rootReader.scalar("e");

    rootReader.next();
    assertEquals(10, aReader.getInt());
    assertEquals(110, cReader.getInt());
    assertEquals("fred", dReader.getString());
    assertEquals("pebbles",  eReader.getString());

    rootReader.next();
    assertEquals(20, aReader.getInt());
    assertEquals(210, cReader.getInt());
    assertEquals("barney", dReader.getString());
    assertEquals("bam-bam",  eReader.getString());

    // Verify using the convenience methods.

    final SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addRow(10, mapValue(110, "fred"), "pebbles")
        .addRow(20, mapValue(210, "barney"), "bam-bam")
        .build();

    new RowSetComparison(expected).verify(result);

    // Test that the row set rebuilds its internal structure from
    // a vector container.

    RowSet wrapped = fixture.wrap(result.container());
    RowSetUtilities.verify(expected, wrapped);
  }

  /**
   * Create nested maps. Use required, variable-width columns since
   * those require the most processing and are most likely to
   * fail if anything is out of place.
   */

  @Test
  public void testNestedMapsRequired() {
    final TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m1")
          .add("b", MinorType.VARCHAR)
          .addMap("m2")
            .add("c", MinorType.VARCHAR)
            .resumeMap()
          .add("d", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    RowSetBuilder builder = fixture.rowSetBuilder(schema);
    RowSetWriter rootWriter = builder.writer();

    rootWriter.addRow(10, mapValue("b1", mapValue("c1"), "d1"));
    rootWriter.addRow(20, mapValue("b2", mapValue("c2"), "d2"));

    // Validate directly

    RowSet result = builder.build();
    RowSetReader rootReader = result.reader();
    TupleReader m1Reader = rootReader.tuple("m1");
    TupleReader m2Reader = m1Reader.tuple("m2");

    rootReader.next();
    assertEquals(10, rootReader.scalar("a").getInt());
    assertEquals("b1", m1Reader.scalar("b").getString());
    assertEquals("c1", m2Reader.scalar("c").getString());
    assertEquals("d1", m1Reader.scalar("d").getString());

    rootReader.next();
    assertEquals(20, rootReader.scalar("a").getInt());
    assertEquals("b2", m1Reader.scalar("b").getString());
    assertEquals("c2", m2Reader.scalar("c").getString());
    assertEquals("d2", m1Reader.scalar("d").getString());

    // Validate with convenience methods

    RowSet expected = fixture.rowSetBuilder(schema)
        .addRow(10, mapValue("b1", mapValue("c1"), "d1"))
        .addRow(20, mapValue("b2", mapValue("c2"), "d2"))
        .build();

    new RowSetComparison(expected).verify(result);

    // Test that the row set rebuilds its internal structure from
    // a vector container.

    RowSet wrapped = fixture.wrap(result.container());
    RowSetUtilities.verify(expected, wrapped);
  }

  @Test
  public void testBasicRepeatedMap() {
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMapArray("m")
          .add("c", MinorType.INT)
          .add("d", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    RowSetBuilder builder = fixture.rowSetBuilder(schema);
    RowSetWriter rootWriter = builder.writer();

    // Verify structure and schema

    TupleMetadata actualSchema = rootWriter.tupleSchema();
    assertEquals(2, actualSchema.size());
    assertTrue(actualSchema.metadata(1).isArray());
    assertTrue(actualSchema.metadata(1).isMap());
    assertEquals(2, actualSchema.metadata("m").tupleSchema().size());
    assertEquals(2, actualSchema.column("m").getChildren().size());
    TupleWriter mapWriter = rootWriter.array("m").tuple();
    assertSame(actualSchema.metadata("m").tupleSchema(), mapWriter.schema().tupleSchema());
    assertSame(mapWriter.tupleSchema(), mapWriter.schema().tupleSchema());
    assertSame(mapWriter.tupleSchema().metadata(0), mapWriter.scalar(0).schema());
    assertSame(mapWriter.tupleSchema().metadata(1), mapWriter.scalar(1).schema());

    // Write a couple of rows with arrays.

    rootWriter
      .addRow(10, mapArray(
          mapValue(110, "d1.1"),
          mapValue(120, "d2.2")))
      .addRow(20, mapArray())
      .addRow(30, mapArray(
          mapValue(310, "d3.1"),
          mapValue(320, "d3.2"),
          mapValue(330, "d3.3")));

    // Verify the first batch

    RowSet actual = builder.build();
    RepeatedMapVector mapVector = (RepeatedMapVector) actual.container().getValueVector(1).getValueVector();
    MaterializedField mapField = mapVector.getField();
    assertEquals(2, mapField.getChildren().size());
    Iterator<MaterializedField> iter = mapField.getChildren().iterator();
    assertTrue(mapWriter.scalar(0).schema().schema().isEquivalent(iter.next()));
    assertTrue(mapWriter.scalar(1).schema().schema().isEquivalent(iter.next()));

    SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addRow(10, mapArray(
            mapValue(110, "d1.1"),
            mapValue(120, "d2.2")))
        .addRow(20, mapArray())
        .addRow(30, mapArray(
            mapValue(310, "d3.1"),
            mapValue(320, "d3.2"),
            mapValue(330, "d3.3")))
        .build();
    new RowSetComparison(expected).verify(actual);

    // Test that the row set rebuilds its internal structure from
    // a vector container.

    RowSet wrapped = fixture.wrap(actual.container());
    RowSetUtilities.verify(expected, wrapped);
  }

  @Test
  public void testNestedArray() {
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMapArray("m")
          .add("c", MinorType.INT)
          .addArray("d", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    RowSetBuilder builder = fixture.rowSetBuilder(schema);
    RowSetWriter rootWriter = builder.writer();

    // Write a couple of rows with arrays within arrays.
    // (And, of course, the Varchar is actually an array of
    // bytes, so that's three array levels.)

    rootWriter
      .addRow(10, mapArray(
          mapValue(110, strArray("d1.1.1", "d1.1.2")),
          mapValue(120, strArray("d1.2.1", "d1.2.2"))))
      .addRow(20, mapArray())
      .addRow(30, mapArray(
          mapValue(310, strArray("d3.1.1", "d3.2.2")),
          mapValue(320, strArray()),
          mapValue(330, strArray("d3.3.1", "d1.2.2"))));

    // Verify the batch

    RowSet actual = builder.build();
    SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addRow(10, mapArray(
            mapValue(110, strArray("d1.1.1", "d1.1.2")),
            mapValue(120, strArray("d1.2.1", "d1.2.2"))))
        .addRow(20, mapArray())
        .addRow(30, mapArray(
            mapValue(310, strArray("d3.1.1", "d3.2.2")),
            mapValue(320, strArray()),
            mapValue(330, strArray("d3.3.1", "d1.2.2"))))
        .build();
    new RowSetComparison(expected).verify(actual);

    // Test that the row set rebuilds its internal structure from
    // a vector container.

    RowSet wrapped = fixture.wrap(actual.container());
    RowSetUtilities.verify(expected, wrapped);
  }

  /**
   * Test a doubly-nested array of maps.
   */

  @Test
  public void testDoubleNestedArray() {
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMapArray("m1")
          .add("b", MinorType.INT)
          .addMapArray("m2")
            .add("c", MinorType.INT)
            .addArray("d", MinorType.VARCHAR)
            .resumeMap()
          .resumeSchema()
        .buildSchema();

    RowSetBuilder builder = fixture.rowSetBuilder(schema);
    RowSetWriter rootWriter = builder.writer();

    ScalarWriter aWriter = rootWriter.scalar("a");
    ArrayWriter a1Writer = rootWriter.array("m1");
    TupleWriter m1Writer = a1Writer.tuple();
    ScalarWriter bWriter = m1Writer.scalar("b");
    ArrayWriter a2Writer = m1Writer.array("m2");
    TupleWriter m2Writer = a2Writer.tuple();
    ScalarWriter cWriter = m2Writer.scalar("c");
    ScalarWriter dWriter = m2Writer.array("d").scalar();

    for (int i = 0; i < 5; i++) {
      aWriter.setInt(i);
      for (int j = 0; j < 4; j++) {
        int a1Key = i + 10 + j;
        bWriter.setInt(a1Key);
        for (int k = 0; k < 3; k++) {
          int a2Key = a1Key * 10 + k;
          cWriter.setInt(a2Key);
          for (int l = 0; l < 2; l++) {
            dWriter.setString("d-" + (a2Key * 10 + l));
          }
          a2Writer.save();
        }
        a1Writer.save();
      }
      rootWriter.save();
    }

    RowSet results = builder.build();
    RowSetReader reader = results.reader();

    ScalarReader aReader = reader.scalar("a");
    ArrayReader a1Reader = reader.array("m1");
    TupleReader m1Reader = a1Reader.tuple();
    ScalarReader bReader = m1Reader.scalar("b");
    ArrayReader a2Reader = m1Reader.array("m2");
    TupleReader m2Reader = a2Reader.tuple();
    ScalarReader cReader = m2Reader.scalar("c");
    ArrayReader dArray = m2Reader.array("d");
    ScalarReader dReader = dArray.scalar();

    for (int i = 0; i < 5; i++) {
      assertTrue(reader.next());
      assertEquals(i, aReader.getInt());
      for (int j = 0; j < 4; j++) {
        assertTrue(a1Reader.next());
        int a1Key = i + 10 + j;
        assertEquals(a1Key, bReader.getInt());
        for (int k = 0; k < 3; k++) {
          assertTrue(a2Reader.next());
          int a2Key = a1Key * 10 + k;
          assertEquals(a2Key, cReader.getInt());
          for (int l = 0; l < 2; l++) {
            assertTrue(dArray.next());
            assertEquals("d-" + (a2Key * 10 + l), dReader.getString());
          }
        }
      }
    }
    results.clear();
  }

  /**
   * Test that the schema inference handles repeated map vectors.
   * <p>
   * It turns out that when some operators create a map array, it adds the
   * $offset$ vector to the list of children for the map's MaterializedField.
   * But, the RowSet utilities do not. This test verifies that both forms
   * work.
   */

  @Test
  public void testDrill6809() throws Exception {
    try (ClusterFixture cluster = ClusterFixture.standardCluster(dirTestWatcher);
        ClientFixture client = cluster.clientFixture()) {
      // Input: {"a" : [ {"c": 1} ], "b" : 2.1}
      String sql = "select * from `cp`.`jsoninput/repeatedmap_sort_bug.json`";
      RowSet actual = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata schema = new SchemaBuilder()
          .addMapArray("a")
            .addNullable("c", MinorType.BIGINT)
            .resumeSchema()
          .addNullable("b", MinorType.FLOAT8)
          .buildSchema();

      RowSet expected = fixture.rowSetBuilder(schema)
          .addRow(mapArray(mapValue(1L)), 2.1D)
          .build();
      RowSetUtilities.verify(expected, actual);
    }
  }
}
