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
package org.apache.drill.exec.store.easy.json.loader;

import static org.apache.drill.test.rowSet.RowSetUtilities.boolArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.mapArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.mapValue;
import static org.apache.drill.test.rowSet.RowSetUtilities.strArray;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.drill.categories.JsonTest;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests the ability of the JSON reader to "wait out" a set of leading
 * null or empty array values to wait until an actual value appears before
 * deciding on the column type. Hitting the end of batch, or an array
 * that contains only null values, forces resolution to VARCHAR.
 */
@Category(JsonTest.class)
public class TestUnknowns extends BaseJsonLoaderTest {

  @Test
  public void testNullToBoolean() {
    String json =
        "{a: null} {a: true} {a: false} {a: true}";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("a", MinorType.BIT)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow((Boolean) null)
        .addRow(true)
        .addRow(false)
        .addRow(true)
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testNullToString() {
    String json =
        "{a: null} {a: \"foo\"} {a: \"bar\"}";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("a", MinorType.VARCHAR)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow((String) null)
        .addRow("foo")
        .addRow("bar")
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testNullToObject() {
    String json =
        "{a: null} {a: {b: 20, c: 220}}";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addMap("a")
          .addNullable("b", MinorType.BIGINT)
          .addNullable("c", MinorType.BIGINT)
          .resumeSchema()
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addSingleCol(mapValue(null, null))
        .addSingleCol(mapValue(20, 220))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  /**
   * Input contains all nulls. The loader will force resolve to a
   * type, and will choose VARCHAR as all scalar types which
   * may later appear can be converted to VARCHAR.
   */
  @Test
  public void testForcedNullResolve() {
    String json =
        "{a: null} {a: null}";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("a", MinorType.VARCHAR)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow((String) null)
        .addRow((String) null)
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testArrayToBooleanArray() {
    String json =
        "{a: []} {a: [true, false]} {a: true}";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("a", MinorType.BIT)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addSingleCol(boolArray())
        .addSingleCol(boolArray(true, false))
        .addSingleCol(boolArray(true))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testArrayToBooleanScalar() {
    String json =
        "{a: []} {a: true} {a: [true, false]}";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("a", MinorType.BIT)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addSingleCol(boolArray())
        .addSingleCol(boolArray(true))
        .addSingleCol(boolArray(true, false))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  /**
   * Input contains all nulls. The loader will force resolve to a
   * type, and will choose VARCHAR as all scalar types which
   * may later appear can be converted to VARCHAR.
   */
  @Test
  public void testForcedArrayResolve() {
    String json =
        "{a: []} {a: []}";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("a", MinorType.VARCHAR)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addSingleCol(strArray())
        .addSingleCol(strArray())
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testNullToArrayToBoolean() {
    String json =
        "{a: null} {a: []} {a: [true, false]} {a: true}";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("a", MinorType.BIT)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addSingleCol(boolArray())
        .addSingleCol(boolArray())
        .addSingleCol(boolArray(true, false))
        .addSingleCol(boolArray(true))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testArrayToNullJson() {
    String json =
        "{a: []} {a: [null]} {a: [\"foo\"] }\n" +
        "{a: [ 10, [20], { b: 30 } ] }";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("a", MinorType.VARCHAR)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addSingleCol(strArray())
        .addSingleCol(strArray("null"))
        .addSingleCol(strArray("\"foo\""))
        .addSingleCol(strArray("10", "[20]", "{\"b\": 30}"))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testArrayToNullVarchar() {
    String json =
        "{a: []} {a: [null]} {a: [\"foo\"]}";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.unknownsAsJson = false;
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("a", MinorType.VARCHAR)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addSingleCol(strArray())
        .addSingleCol(strArray(""))
        .addSingleCol(strArray("foo"))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testArrayToObjectArray() {
    String json =
        "{a: 1, m: []}\n" +
        "{a: 2, m: [{b: 10, c: 20}, {b: 11, c: 21}]}\n" +
        "{a: 3, m: [{b: 110, c: 120}, {b: 111, c: 121}]}";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("a", MinorType.BIGINT)
        .addMapArray("m")
          .addNullable("b", MinorType.BIGINT)
          .addNullable("c", MinorType.BIGINT)
          .resumeSchema()
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(1L, mapArray())
        .addRow(2L, mapArray(mapValue(10L, 20L), mapValue(11L, 21L)))
        .addRow(3L, mapArray(mapValue(110L, 120L), mapValue(111L, 121L)))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }
}
