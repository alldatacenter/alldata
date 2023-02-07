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

import static org.apache.drill.test.rowSet.RowSetUtilities.mapValue;
import static org.apache.drill.test.rowSet.RowSetUtilities.objArray;
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

@Category(JsonTest.class)
public class TestVariant extends BaseJsonLoaderTest {

  @Test
  public void testScalars() {
    String json =
        "{a: null} {a: true} {a: 10} {a: 10.5} {a: \"foo\"}";
    TupleMetadata schema = new SchemaBuilder()
        .addNullable("a", MinorType.UNION)
        .build();

    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.builder.providedSchema(schema);
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    RowSet expected = fixture.rowSetBuilder(schema)
        .addSingleCol(null)
        .addSingleCol(true)
        .addSingleCol(10L)
        .addSingleCol(10.5D)
        .addSingleCol("foo")
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testMap() {
    String json =
        "{a: null} {a: 10}\n" +
        "{a: {b: 10, c: \"foo\"}}";
    TupleMetadata schema = new SchemaBuilder()
        .addNullable("a", MinorType.UNION)
        .build();

    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.builder.providedSchema(schema);
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    // The RowSetBuilder can add scalar types to a union,
    // but not structured types. So, we have to declare the
    // structured types up front.
    TupleMetadata expectedchema = new SchemaBuilder()
        .addUnion("a")
          .addType(MinorType.BIGINT)
          .addMap()
            .addNullable("b", MinorType.BIGINT)
            .addNullable("c", MinorType.VARCHAR)
            .resumeUnion()
          .resumeSchema()
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedchema)
        .addSingleCol(null)
        .addSingleCol(10L)
        .addSingleCol(mapValue(10L, "foo"))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testScalarList() {
    String json =
        "{a: null} {a: []}\n" +
        // All scalar types
        "{a: [null, true, 10, 10.5, \"foo\"]}\n" +
        // One more to ensure that indexes are synced
        "{a: [false, 20]}";
    TupleMetadata schema = new SchemaBuilder()
        .addNullable("a", MinorType.LIST)
        .build();

    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.builder.providedSchema(schema);
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedchema = new SchemaBuilder()
        .addList("a")
          .addType(MinorType.BIT)
          .addType(MinorType.BIGINT)
          .addType(MinorType.FLOAT8)
          .addType(MinorType.VARCHAR)
          .resumeSchema()
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedchema)
        .addSingleCol(null)
        .addSingleCol(objArray())
        .addSingleCol(objArray(null, true, 10L, 10.5D, "foo"))
        .addSingleCol(objArray(false, 20L))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testObjectList() {
    String json =
        "{a: null} {a: []} {a: [null, 10]}\n" +
        "{a: [{b:  10, c:  20}, {b: 110, c: 120}]}\n" +
        "{a: [{b: 210, c: 220}, {b: 310, c: 320}]}";
    TupleMetadata schema = new SchemaBuilder()
        .addNullable("a", MinorType.LIST)
        .build();

    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.builder.providedSchema(schema);
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedchema = new SchemaBuilder()
        .addList("a")
          .addType(MinorType.BIGINT)
          .addMap()
            .addNullable("b", MinorType.BIGINT)
            .addNullable("c", MinorType.BIGINT)
          .  resumeUnion()
          .resumeSchema()
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedchema)
        .addSingleCol(null)
        .addSingleCol(objArray())
        .addSingleCol(objArray(null, 10L))
        .addSingleCol(objArray(
            objArray(10L, 20L), objArray(110L, 120L)))
        .addSingleCol(objArray(
            objArray(210L, 220L), objArray(310L, 320L)))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }
}
