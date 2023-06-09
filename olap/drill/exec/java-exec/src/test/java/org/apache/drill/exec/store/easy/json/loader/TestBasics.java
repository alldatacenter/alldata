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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.drill.categories.JsonTest;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.resultSet.project.Projections;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetTestUtils;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(JsonTest.class)
public class TestBasics extends BaseJsonLoaderTest {

  @Test
  public void testEmpty() {
    String json = "";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.open(json);
    RowSet results = loader.next();
    assertNull(results);
    loader.close();
  }

  @Test
  public void testEmptyTuple() {
    final String json = "{} {} {}";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.open(json);
    RowSet results = loader.next();
    assertEquals(3, results.rowCount());
    assertTrue(results.schema().isEmpty());
    assertNotNull(results);
    loader.close();
  }

  @Test
  public void testRootArray() {
    final String json = "[{a: 10}, {a: 20}, {a: 30}]";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("a", MinorType.BIGINT)
         .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(10)
        .addRow(20)
        .addRow(30)
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testLeadingTrailingWhitespace() {
    final String json = "{\" a\": 10, \" b\": 20, \" c \": 30}";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("a", MinorType.BIGINT)
        .addNullable("b", MinorType.BIGINT)
        .addNullable("c", MinorType.BIGINT)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(10, 20, 30)
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testCaseInsensitive() {
    final String json = "{a: 10, Bob: 110} {A: 20, bOb: 120} {\" a \": 30, BoB: 130}";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("a", MinorType.BIGINT)
        .addNullable("Bob", MinorType.BIGINT)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(10, 110)
        .addRow(20, 120)
        .addRow(30, 130)
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testProjection() {
    String json =
        "{a: 10, b: true}\n" +
        "{a: 20, b: [\"what?\"]}\n" +
        "{a: 30, b: {c: \"oh, my!\"}}" +
        "{a: 40}" +
        "{a: 50, b: [[{x: [[{y: []}]]}]]}";

    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.rsLoaderOptions.projection(
        Projections.parse(RowSetTestUtils.projectList("a")));
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("a", MinorType.BIGINT)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(10)
        .addRow(20)
        .addRow(30)
        .addRow(40)
        .addRow(50)
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testMissingEndObject() {
    expectError("{a: 0} {a: 100", "Error parsing JSON");
  }

  @Test
  public void testMissingValue() {
    expectError("{a: 0} {a: ", "Error parsing JSON");
  }

  /**
   * When parsing an array, the Jackson JSON parser raises
   * an error for a missing close bracket.
   */
  @Test
  public void testMissingEndOuterArray() {
    expectError("[{a: 0}, {a: 100}", "Error parsing JSON");
  }

  @Test
  public void testEmptyKey() {
    expectError("{\"\": 10}", "does not allow empty keys");
  }

  @Test
  public void testBlankKey() {
    expectError("{\"  \": 10}", "does not allow empty keys");
  }

  @Test
  public void testRootArrayDisallowed() {
    final String json = "[{a: 0}, {a: 100}, {a: null}]";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.skipOuterList = false;
    try {
      loader.open(json);
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("outer array support is not enabled"));
    }
  }

  protected static void expectError(String json, String msg) {
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.open(json);
    try {
      loader.next();
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains(msg));
    }
    loader.close();
  }

  /**
   * Test syntax error recover. Recovery is not perfect. The
   * input contains six records: the second is bad. But, the parser
   * consumes records 3 and 4 trying to recover.
   */
  @Test
  public void testRecovery() {
    final String json = "{a: 1} {a: {a: 3} {a: 4} {a: 5} {a: 6}";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.skipMalformedRecords = true;
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("a", MinorType.BIGINT)
         .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(1)
        .addRow(5)
        .addRow(6)
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }
}
