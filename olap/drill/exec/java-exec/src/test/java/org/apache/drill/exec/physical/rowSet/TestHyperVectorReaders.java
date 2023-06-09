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
import static org.apache.drill.test.rowSet.RowSetUtilities.variantArray;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.rowSet.RowSet.ExtendableRowSet;
import org.apache.drill.exec.physical.rowSet.RowSet.HyperRowSet;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.selection.SelectionVector4;
import org.apache.drill.test.SubOperatorTest;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test the reader mechanism that reads rows indexed via an SV4.
 * SV4's introduce an additional level of indexing: each row may
 * come from a different batch. The readers use the SV4 to find
 * the root batch and vector, then must navigate downward from that
 * vector for maps, repeated maps, lists, unions, repeated lists,
 * nullable vectors and variable-length vectors.
 * <p>
 * This test does not cover repeated vectors; those tests should be added.
 */
@Category(RowSetTests.class)
public class TestHyperVectorReaders extends SubOperatorTest {

  /**
   * Test the simplest case: a top-level required vector. Has no contained vectors.
   * This test focuses on the SV4 indirection mechanism itself.
   */
  @Test
  public void testRequired() {
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .buildSchema();

    SingleRowSet rowSet1;
    {
      ExtendableRowSet rowSet = fixture.rowSet(schema);
      RowSetWriter writer = rowSet.writer();
      for (int i = 0; i < 10; i++) {
        writer.scalar(0).setInt(i * 10);
        writer.save();
      }
      rowSet1 = writer.done();
    }

    SingleRowSet rowSet2;
    {
      ExtendableRowSet rowSet = fixture.rowSet(schema);
      RowSetWriter writer = rowSet.writer();
      for (int i = 10; i < 20; i++) {
        writer.scalar(0).setInt(i * 10);
        writer.save();
      }
      rowSet2 = writer.done();
    }

    // Build the hyper batch
    // [0, 10, 20, ... 190]
    HyperRowSet hyperSet = HyperRowSetImpl.fromRowSets(fixture.allocator(), rowSet1, rowSet2);
    assertEquals(20, hyperSet.rowCount());

    // Populate the indirection vector:
    // (1, 9), (0, 9), (1, 8), (0, 8), ... (0, 0)
    SelectionVector4 sv4 = hyperSet.getSv4();
    for (int i = 0; i < 20; i++) {
      int batch = i % 2;
      int offset = 9 - i / 2;
      sv4.set(i, batch, offset);
    }

    // Sanity check.
    for (int i = 0; i < 20; i++) {
      int batch = i % 2;
      int offset = 9 - i / 2;
      int encoded = sv4.get(i);
      assertEquals(batch, SelectionVector4.getBatchIndex(encoded));
      assertEquals(offset, SelectionVector4.getRecordIndex(encoded));
    }

    // Verify reader
    // Expected: [190, 90, 180, 80, ... 0]
    RowSetReader reader = hyperSet.reader();
    for (int i = 0; i < 20; i++) {
      assertTrue(reader.next());
      int batch = i % 2;
      int offset = 9 - i / 2;
      int expected = batch * 100 + offset * 10;
      assertEquals(expected, reader.scalar(0).getInt());
    }
    assertFalse(reader.next());

    // Validate using an expected result set.
    RowSetBuilder rsBuilder = fixture.rowSetBuilder(schema);
    for (int i = 0; i < 20; i++) {
      int batch = i % 2;
      int offset = 9 - i / 2;
      int expected = batch * 100 + offset * 10;
      rsBuilder.addRow(expected);
    }

    RowSetUtilities.verify(rsBuilder.build(), hyperSet);
  }

  @Test
  public void testVarWidth() {
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.VARCHAR)
        .buildSchema();

    SingleRowSet rowSet1 = fixture.rowSetBuilder(schema)
        .addSingleCol("second")
        .addSingleCol("fourth")
        .build();

    SingleRowSet rowSet2 = fixture.rowSetBuilder(schema)
        .addSingleCol("first")
        .addSingleCol("third")
        .build();

    // Build the hyper batch
    HyperRowSet hyperSet = HyperRowSetImpl.fromRowSets(fixture.allocator(), rowSet1, rowSet2);
    assertEquals(4, hyperSet.rowCount());
    SelectionVector4 sv4 = hyperSet.getSv4();
    sv4.set(0, 1, 0);
    sv4.set(1, 0, 0);
    sv4.set(2, 1, 1);
    sv4.set(3, 0, 1);

    SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addRow("first")
        .addRow("second")
        .addRow("third")
        .addRow("fourth")
        .build();

    RowSetUtilities.verify(expected, hyperSet);
  }

  /**
   * Test a nullable varchar. Requires multiple indirections:
   * <ul>
   * <li>From the SV4 to the nullable vector.</li>
   * <li>From the nullable vector to the bits vector.</li>
   * <li>From the nullable vector to the data vector.</li>
   * <li>From the data vector to the offset vector.</li>
   * <li>From the data vector to the values vector.</li>
   * </ul>
   * All are coordinated by the vector index and vector accessors.
   * This test verifies that each of the indirections does, in fact,
   * work as expected.
   */
  @Test
  public void testOptional() {
    TupleMetadata schema = new SchemaBuilder()
        .addNullable("a", MinorType.VARCHAR)
        .buildSchema();

    SingleRowSet rowSet1 = fixture.rowSetBuilder(schema)
        .addSingleCol("sixth")
        .addSingleCol(null)
        .addSingleCol("fourth")
        .build();

    SingleRowSet rowSet2 = fixture.rowSetBuilder(schema)
        .addSingleCol(null)
        .addSingleCol("first")
        .addSingleCol("third")
        .build();

    // Build the hyper batch
    HyperRowSet hyperSet = HyperRowSetImpl.fromRowSets(fixture.allocator(), rowSet1, rowSet2);
    assertEquals(6, hyperSet.rowCount());
    SelectionVector4 sv4 = hyperSet.getSv4();
    sv4.set(0, 1, 1);
    sv4.set(1, 0, 1);
    sv4.set(2, 1, 2);
    sv4.set(3, 0, 2);
    sv4.set(4, 1, 0);
    sv4.set(5, 0, 0);

    SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addSingleCol("first")
        .addSingleCol(null)
        .addSingleCol("third")
        .addSingleCol("fourth")
        .addSingleCol(null)
        .addSingleCol("sixth")
        .build();

    RowSetUtilities.verify(expected, hyperSet);
  }

  /**
   * Test an array to test the indirection from the repeated vector
   * to the array offsets vector and the array values vector. (Uses
   * varchar to add another level of indirection to the data offset
   * and data values vectors.)
   */
  @Test
  public void testRepeated() {
    TupleMetadata schema = new SchemaBuilder()
        .addArray("a", MinorType.VARCHAR)
        .buildSchema();

    SingleRowSet rowSet1 = fixture.rowSetBuilder(schema)
        .addSingleCol(strArray("sixth", "6.1", "6.2"))
        .addSingleCol(strArray("second", "2.1", "2.2", "2.3"))
        .addSingleCol(strArray("fourth", "4.1"))
        .build();

    SingleRowSet rowSet2 = fixture.rowSetBuilder(schema)
        .addSingleCol(strArray("fifth", "51", "5.2"))
        .addSingleCol(strArray("first", "1.1", "1.2", "1.3"))
        .addSingleCol(strArray("third", "3.1"))
        .build();

    // Build the hyper batch
    HyperRowSet hyperSet = HyperRowSetImpl.fromRowSets(fixture.allocator(), rowSet1, rowSet2);
    assertEquals(6, hyperSet.rowCount());
    SelectionVector4 sv4 = hyperSet.getSv4();
    sv4.set(0, 1, 1);
    sv4.set(1, 0, 1);
    sv4.set(2, 1, 2);
    sv4.set(3, 0, 2);
    sv4.set(4, 1, 0);
    sv4.set(5, 0, 0);

    SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addSingleCol(strArray("first", "1.1", "1.2", "1.3"))
        .addSingleCol(strArray("second", "2.1", "2.2", "2.3"))
        .addSingleCol(strArray("third", "3.1"))
        .addSingleCol(strArray("fourth", "4.1"))
        .addSingleCol(strArray("fifth", "51", "5.2"))
        .addSingleCol(strArray("sixth", "6.1", "6.2"))
        .build();

    RowSetUtilities.verify(expected, hyperSet);
  }

  /**
   * Maps are an interesting case. The hyper-vector wrapper holds a mirror-image of the
   * map members. So, we can reach the map members either via the vector wrappers or
   * the original map vector.
   */
  @Test
  public void testMap() {
    TupleMetadata schema = new SchemaBuilder()
        .addMap("m")
          .add("a", MinorType.INT)
          .add("b", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    SingleRowSet rowSet1 = fixture.rowSetBuilder(schema)
        .addSingleCol(mapValue(2, "second"))
        .addSingleCol(mapValue(4, "fourth"))
        .build();

    SingleRowSet rowSet2 = fixture.rowSetBuilder(schema)
        .addSingleCol(mapValue(2, "first"))
        .addSingleCol(mapValue(4, "third"))
        .build();

    // Build the hyper batch
    HyperRowSet hyperSet = HyperRowSetImpl.fromRowSets(fixture.allocator(), rowSet1, rowSet2);
    assertEquals(4, hyperSet.rowCount());
    SelectionVector4 sv4 = hyperSet.getSv4();
    sv4.set(0, 1, 0);
    sv4.set(1, 0, 0);
    sv4.set(2, 1, 1);
    sv4.set(3, 0, 1);

    SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addSingleCol(mapValue(2, "first"))
        .addSingleCol(mapValue(2, "second"))
        .addSingleCol(mapValue(4, "third"))
        .addSingleCol(mapValue(4, "fourth"))
        .build();

    RowSetUtilities.verify(expected, hyperSet);
  }

  @Test
  public void testRepeatedMap() {
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMapArray("ma")
          .add("b", MinorType.INT)
          .add("c", MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    SingleRowSet rowSet1 = fixture.rowSetBuilder(schema)
        .addRow(2, mapArray(mapValue(21, "second.1"), mapValue(22, "second.2")))
        .addRow(4, mapArray(mapValue(41, "fourth.1")))
        .build();

    SingleRowSet rowSet2 = fixture.rowSetBuilder(schema)
        .addRow(1, mapArray(mapValue(11, "first.1"), mapValue(12, "first.2")))
        .addRow(3, mapArray(mapValue(31, "third.1"), mapValue(32, "third.2"), mapValue(33, "third.3")))
        .build();

    // Build the hyper batch
    HyperRowSet hyperSet = HyperRowSetImpl.fromRowSets(fixture.allocator(), rowSet1, rowSet2);
    assertEquals(4, hyperSet.rowCount());
    SelectionVector4 sv4 = hyperSet.getSv4();
    sv4.set(0, 1, 0);
    sv4.set(1, 0, 0);
    sv4.set(2, 1, 1);
    sv4.set(3, 0, 1);

    SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addRow(1, mapArray(mapValue(11, "first.1"),  mapValue(12, "first.2")))
        .addRow(2, mapArray(mapValue(21, "second.1"), mapValue(22, "second.2")))
        .addRow(3, mapArray(mapValue(31, "third.1"),  mapValue(32, "third.2"), mapValue(33, "third.3")))
        .addRow(4, mapArray(mapValue(41, "fourth.1")))
        .build();

    RowSetUtilities.verify(expected, hyperSet);
  }

  @Test
  public void testUnion() {
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addUnion("u")
          .addType(MinorType.INT)
          .addType(MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    SingleRowSet rowSet1 = fixture.rowSetBuilder(schema)
        .addRow(2, 20)
        .addRow(4, "fourth")
        .build();

    SingleRowSet rowSet2 = fixture.rowSetBuilder(schema)
        .addRow(1, "first")
        .addRow(3, 30)
        .build();

    // Build the hyper batch
    HyperRowSet hyperSet = HyperRowSetImpl.fromRowSets(fixture.allocator(), rowSet1, rowSet2);
    assertEquals(4, hyperSet.rowCount());
    SelectionVector4 sv4 = hyperSet.getSv4();
    sv4.set(0, 1, 0);
    sv4.set(1, 0, 0);
    sv4.set(2, 1, 1);
    sv4.set(3, 0, 1);

    SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addRow(1, "first")
        .addRow(2, 20)
        .addRow(3, 30)
        .addRow(4, "fourth")
        .build();

    RowSetUtilities.verify(expected, hyperSet);
  }

  @Test
  public void testScalarList() {
    TupleMetadata schema = new SchemaBuilder()
        .addList("a")
          .addType(MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    schema.metadata("a").variantSchema().becomeSimple();

    SingleRowSet rowSet1 = fixture.rowSetBuilder(schema)
        .addSingleCol(strArray("sixth", "6.1", "6.2"))
        .addSingleCol(null)
        .addSingleCol(strArray("fourth", "4.1"))
        .build();

    SingleRowSet rowSet2 = fixture.rowSetBuilder(schema)
        .addSingleCol(strArray("fifth", "51", "5.2"))
        .addSingleCol(strArray("first", "1.1", "1.2", "1.3"))
        .addSingleCol(strArray("third", "3.1"))
        .build();

    // Build the hyper batch
    HyperRowSet hyperSet = HyperRowSetImpl.fromRowSets(fixture.allocator(), rowSet1, rowSet2);
    assertEquals(6, hyperSet.rowCount());
    SelectionVector4 sv4 = hyperSet.getSv4();
    sv4.set(0, 1, 1);
    sv4.set(1, 0, 1);
    sv4.set(2, 1, 2);
    sv4.set(3, 0, 2);
    sv4.set(4, 1, 0);
    sv4.set(5, 0, 0);

    SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addSingleCol(strArray("first", "1.1", "1.2", "1.3"))
        .addSingleCol(null)
        .addSingleCol(strArray("third", "3.1"))
        .addSingleCol(strArray("fourth", "4.1"))
        .addSingleCol(strArray("fifth", "51", "5.2"))
        .addSingleCol(strArray("sixth", "6.1", "6.2"))
        .build();

    RowSetUtilities.verify(expected, hyperSet);
  }

  @Test
  public void testUnionList() {
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addList("list")
          .addType(MinorType.INT)
          .addType(MinorType.VARCHAR)
          .resumeSchema()
        .buildSchema();

    SingleRowSet rowSet1 = fixture.rowSetBuilder(schema)
        .addRow(6, variantArray("sixth", 61, "6.2"))
        .addRow(2, variantArray("second", "2.1", 22, "2.3"))
        .addRow(4, variantArray("fourth", 41))
        .build();

    SingleRowSet rowSet2 = fixture.rowSetBuilder(schema)
        .addRow(5, variantArray("fifth", "5.1", 52))
        .addRow(1, variantArray("first", 11, "1.2", 13))
        .addRow(3, variantArray("third", 31))
        .build();

    // Build the hyper batch
    HyperRowSet hyperSet = HyperRowSetImpl.fromRowSets(fixture.allocator(), rowSet1, rowSet2);
    assertEquals(6, hyperSet.rowCount());
    SelectionVector4 sv4 = hyperSet.getSv4();
    sv4.set(0, 1, 1);
    sv4.set(1, 0, 1);
    sv4.set(2, 1, 2);
    sv4.set(3, 0, 2);
    sv4.set(4, 1, 0);
    sv4.set(5, 0, 0);

    SingleRowSet expected = fixture.rowSetBuilder(schema)
        .addRow(1, variantArray("first", 11, "1.2", 13))
        .addRow(2, variantArray("second", "2.1", 22, "2.3"))
        .addRow(3, variantArray("third", 31))
        .addRow(4, variantArray("fourth", 41))
        .addRow(5, variantArray("fifth", "5.1", 52))
        .addRow(6, variantArray("sixth", 61, "6.2"))
        .build();

    RowSetUtilities.verify(expected, hyperSet);
  }
}
