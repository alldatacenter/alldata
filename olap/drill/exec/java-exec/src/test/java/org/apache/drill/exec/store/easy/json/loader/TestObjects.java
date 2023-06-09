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

import static org.apache.drill.test.rowSet.RowSetUtilities.mapArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.mapValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.drill.categories.JsonTest;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(JsonTest.class)
public class TestObjects extends BaseJsonLoaderTest {

  @Test
  public void testMap() {
    String json =
        "{a: 1, m: {b: 10, c: 20}}\n" +
        "{a: 2, m: {b: 110}}\n" +
        "{a: 3, m: {c: 220}}\n" +
        "{a: 4, m: {}}\n" +
        "{a: 5, m: null}\n" +
        "{a: 6}\n" +
        "{a: 7, m: {b: 710, c: 720}}";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("a", MinorType.BIGINT)
        .addMap("m")
          .addNullable("b", MinorType.BIGINT)
          .addNullable("c", MinorType.BIGINT)
          .resumeSchema()
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(1L, mapValue(10L, 20L))
        .addRow(2L, mapValue(110L, null))
        .addRow(3L, mapValue(null, 220L))
        .addRow(4L, mapValue(null, null))
        .addRow(5L, mapValue(null, null))
        .addRow(6L, mapValue(null, null))
        .addRow(7L, mapValue(710L, 720L))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  /**
   * Without a schema, leading nulls or empty maps can be ambiguous.
   * With a schema, the meaning is clear.
   */
  @Test
  public void testMapWithSchema() {
    String json =
        "{a: 6}\n" +
        "{a: 5, m: null}\n" +
        "{a: 4, m: {}}\n" +
        "{a: 2, m: {b: 110}}\n" +
        "{a: 3, m: {c: 220}}\n" +
        "{a: 1, m: {b: 10, c: 20}}\n" +
        "{a: 7, m: {b: 710, c: 720}}";

    TupleMetadata schema = new SchemaBuilder()
        .addNullable("a", MinorType.BIGINT)
        .addMap("m")
          .addNullable("b", MinorType.BIGINT)
          .addNullable("c", MinorType.BIGINT)
          .resumeSchema()
        .build();

    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.builder.providedSchema(schema);
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    RowSet expected = fixture.rowSetBuilder(schema)
        .addRow(6L, mapValue(null, null))
        .addRow(5L, mapValue(null, null))
        .addRow(4L, mapValue(null, null))
        .addRow(2L, mapValue(110L, null))
        .addRow(3L, mapValue(null, 220L))
        .addRow(1L, mapValue(10L, 20L))
        .addRow(7L, mapValue(710L, 720L))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testMapAsJson() {
    String json =
        "{a: 6}\n" +
        "{a: 5, m: null}\n" +
        "{a: 4, m: {}}\n" +
        "{a: 2, m: {b: 110}}\n" +
        "{a: 3, m: {c: 220}}\n" +
        "{a: 1, m: {b: 10, c: 20}}\n" +
        "{a: 7, m: {b: 710, c: 720}}";

    TupleMetadata schema = new SchemaBuilder()
        .addNullable("a", MinorType.BIGINT)
        .addNullable("m", MinorType.VARCHAR)
        .build();
    ColumnMetadata m = schema.metadata("m");
    m.setProperty(JsonLoader.JSON_MODE, JsonLoader.JSON_LITERAL_MODE);

    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.builder.providedSchema(schema);
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    RowSet expected = fixture.rowSetBuilder(schema)
        .addRow(6L, null)
        .addRow(5L, "null")
        .addRow(4L, "{}")
        .addRow(2L, "{\"b\": 110}")
        .addRow(3L, "{\"c\": 220}")
        .addRow(1L, "{\"b\": 10, \"c\": 20}")
        .addRow(7L, "{\"b\": 710, \"c\": 720}")
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testMapArray() {
    String json =
        "{a: 1, m: [{b: 10, c: 20}, {b: 11, c: 21}]}\n" +
        "{a: 2, m: [{b: 110}]}\n" +
        "{a: 3, m: [{c: 220}]}\n" +
        "{a: 4, m: [{}]}\n" +
        "{a: 5, m: [null]}\n" +
        "{a: 6, m: []}\n" +
        "{a: 7, m: null}\n" +
        "{a: 8}\n" +
        "{a: 9, m: [{b: 710, c: 720}, {b: 711, c: 721}]}";

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
        .addRow(1L, mapArray(mapValue(10L, 20L), mapValue(11L, 21L)))
        .addRow(2L, mapArray(mapValue(110L, null)))
        .addRow(3L, mapArray(mapValue(null, 220L)))
        .addRow(4L, mapArray(mapValue(null, null)))
        .addRow(5L, mapArray(mapValue(null, null)))
        .addRow(6L, mapArray())
        .addRow(7L, mapArray())
        .addRow(8L, mapArray())
        .addRow(9L, mapArray(mapValue(710L, 720L), mapValue(711L, 721L)))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  /**
   * With a schema we don't have to infer the type of the map or its members.
   * Instead, we can tolerate extreme ambiguity in both.
   */
  @Test
  public void testMapArrayWithSchema() {
    String json =
        "{a:  8}\n" +
        "{a:  7, m: null}\n" +
        "{a:  6, m: []}\n" +
        "{a:  5, m: [null]}\n" +
        "{a:  4, m: [{}]}\n" +
        "{a: 10, m: [{b: null}]}\n" +
        "{a: 11, m: [{c: null}]}\n" +
        "{a: 12, m: [{b: null}, {c: null}]}\n" +
        "{a:  2, m: [{b: 110}]}\n" +
        "{a:  3, m: [{c: 220}]}\n" +
        "{a:  1, m: [{b: 10, c: 20}, {b: 11, c: 21}]}\n" +
        "{a:  9, m: [{b: 710, c: 720}, {b: 711, c: 721}]}";

    TupleMetadata schema = new SchemaBuilder()
        .addNullable("a", MinorType.BIGINT)
        .addMapArray("m")
          .addNullable("b", MinorType.BIGINT)
          .addNullable("c", MinorType.BIGINT)
          .resumeSchema()
        .build();

    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.builder.providedSchema(schema);
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    RowSet expected = fixture.rowSetBuilder(schema)
        .addRow( 8L, mapArray())
        .addRow( 7L, mapArray())
        .addRow( 6L, mapArray())
        .addRow( 5L, mapArray(mapValue(null, null)))
        .addRow( 4L, mapArray(mapValue(null, null)))
        .addRow(10L, mapArray(mapValue(null, null)))
        .addRow(11L, mapArray(mapValue(null, null)))
        .addRow(12L, mapArray(mapValue(null, null), mapValue(null, null)))
        .addRow( 2L, mapArray(mapValue(110L, null)))
        .addRow( 3L, mapArray(mapValue(null, 220L)))
        .addRow( 1L, mapArray(mapValue(10L, 20L), mapValue(11L, 21L)))
        .addRow( 9L, mapArray(mapValue(710L, 720L), mapValue(711L, 721L)))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  /**
   * The structure parser feels its way along looking ahead at tokens
   * to guess types. Test the case where the member is an array containing
   * null (so the parser does not know the type). Given a schema, we know
   * it is a map.
   */
  @Test
  public void testMapArrayWithSchemaInitialNullMember() {
    String json =
        "{a:  5, m: [null]}\n" +
        "{a:  1, m: [{b: 10, c: 20}, {b: 11, c: 21}]}\n";

    TupleMetadata schema = new SchemaBuilder()
        .addNullable("a", MinorType.BIGINT)
        .addMapArray("m")
          .addNullable("b", MinorType.BIGINT)
          .addNullable("c", MinorType.BIGINT)
          .resumeSchema()
        .build();

    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.builder.providedSchema(schema);
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    RowSet expected = fixture.rowSetBuilder(schema)
        .addRow( 5L, mapArray(mapValue(null, null)))
        .addRow( 1L, mapArray(mapValue(10L, 20L), mapValue(11L, 21L)))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testObjectToScalar() {
    String json =
        "{a: {b: 10}} {a: 10}";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.open(json);
    try {
      loader.next();
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("JSON object expected"));
    }
    loader.close();
  }

  @Test
  public void testObjectToArray() {
    String json =
        "{a: {b: 10}} {a: [10]}";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.open(json);
    try {
      loader.next();
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("JSON object expected"));
    }
    loader.close();
  }
}
