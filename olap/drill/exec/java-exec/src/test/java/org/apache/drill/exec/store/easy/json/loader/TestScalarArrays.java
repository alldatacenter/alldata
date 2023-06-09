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
import static org.apache.drill.test.rowSet.RowSetUtilities.doubleArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.longArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.strArray;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.drill.categories.JsonTest;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test scalar arrays. Without a schema, the first array token
 * sets the type. With a schema, the type is known independent of
 * the first token (if any).
 * <p>
 * Verifies that a single scalar is harmlessly treated as an
 * array of one element. Verifies default type conversions.
 * Verifies that null array elements are converted to a default
 * value for the type (false, 0 or empty string.)
 */
@Category(JsonTest.class)
public class TestScalarArrays extends BaseJsonLoaderTest {

  @Test
  public void testBoolean() {
    String json =
        "{a: [true, false, null]} {a: []} {a: null} " +
        "{a: true} {a: false} " +
        "{a: [0, 1.0, \"true\", \"\"]}";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("a", MinorType.BIT)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addSingleCol(boolArray(true, false, false))
        .addSingleCol(boolArray())
        .addSingleCol(boolArray())
        .addSingleCol(boolArray(true))
        .addSingleCol(boolArray(false))
        .addSingleCol(boolArray(false, true, true, false))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testAllTextBoolean() {
    String json =
        "{a: [true, false, null]} {a: []} {a: null}";

    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.allTextMode = true;
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("a", MinorType.VARCHAR)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addSingleCol(strArray("true", "false", ""))
        .addSingleCol(strArray())
        .addSingleCol(strArray())
         .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testBooleanWithSchema() {
    String json =
        "{a: []} {a: null} {a: [true, false]} " +
        "{a: true} {a: false} " +
        "{a: [0, 1.0, \"true\", \"\"]}";
    TupleMetadata schema = new SchemaBuilder()
        .addArray("a", MinorType.BIT)
        .build();
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.builder.providedSchema(schema);
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    RowSet expected = fixture.rowSetBuilder(schema)
        .addSingleCol(boolArray())
        .addSingleCol(boolArray())
        .addSingleCol(boolArray(true, false))
        .addSingleCol(boolArray(true))
        .addSingleCol(boolArray(false))
        .addSingleCol(boolArray(false, true, true, false))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testInt() {
    String json =
        "{a: [2, 4, null]} {a: []} {a: null} " +
        "{a: 10} " +
        "{a: [3, 2.3, true, false, \"5\", \"\"]}";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("a", MinorType.BIGINT)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addSingleCol(longArray(2L, 4L, 0L))
        .addSingleCol(longArray())
        .addSingleCol(longArray())
        .addSingleCol(longArray(10L))
        .addSingleCol(longArray(3L, 2L, 1L, 0L, 5L, 0L))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testIntAsDouble() {
    String json =
        "{a: [2, 4, null]} {a: []} {a: null} " +
        "{a: 10} " +
        "{a: [3, 2.25, true, false, \"5\", \"\"]}";

    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.readNumbersAsDouble = true;
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("a", MinorType.FLOAT8)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addSingleCol(doubleArray(2D, 4D, 0D))
        .addSingleCol(doubleArray())
        .addSingleCol(doubleArray())
        .addSingleCol(doubleArray(10D))
        .addSingleCol(doubleArray(3D, 2.25D, 1D, 0D, 5D, 0D))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testAllTextInt() {
    String json =
        "{a: [2, 4, null]} {a: []} {a: null}";

    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.allTextMode = true;
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("a", MinorType.VARCHAR)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addSingleCol(strArray("2", "4", ""))
        .addSingleCol(strArray())
        .addSingleCol(strArray())
         .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testIntWithSchema() {
    String json =
        "{a: []} {a: null} {a: [2, 4, null]} " +
        "{a: 10} " +
        "{a: [3, 2.3, true, false, \"5\", \"\"]}";
    TupleMetadata schema = new SchemaBuilder()
        .addArray("a", MinorType.BIGINT)
        .build();

    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.builder.providedSchema(schema);
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    RowSet expected = fixture.rowSetBuilder(schema)
        .addSingleCol(longArray())
        .addSingleCol(longArray())
        .addSingleCol(longArray(2L, 4L, 0L))
        .addSingleCol(longArray(10L))
        .addSingleCol(longArray(3L, 2L, 1L, 0L, 5L, 0L))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testDouble() {
    String json =
        "{a: [2.25, 4.5, null]} {a: []} {a: null} " +
        "{a: 10.125} " +
        "{a: [3, 2.75, true, false, \"5.25\", \"\"]}";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("a", MinorType.FLOAT8)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addSingleCol(doubleArray(2.25D, 4.5D, 0D))
        .addSingleCol(doubleArray())
        .addSingleCol(doubleArray())
        .addSingleCol(doubleArray(10.126D))
        .addSingleCol(doubleArray(3D, 2.75D, 1.0D, 0.0D, 5.25D, 0D))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testAllTextDouble() {
    String json =
        "{a: [2.25, 4.5, null]} {a: []} {a: null}";

    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.allTextMode = true;
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("a", MinorType.VARCHAR)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addSingleCol(strArray("2.25", "4.5", ""))
        .addSingleCol(strArray())
        .addSingleCol(strArray())
         .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testDoubleWithSchema() {
    String json =
        "{a: []} {a: null} {a: [2.25, 4.5, null]} " +
        "{a: 10.125} " +
        "{a: [3, 2.75, true, false, \"5.25\", \"\"]}";
    TupleMetadata schema = new SchemaBuilder()
        .addArray("a", MinorType.FLOAT8)
        .build();

    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.builder.providedSchema(schema);
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    RowSet expected = fixture.rowSetBuilder(schema)
        .addSingleCol(doubleArray())
        .addSingleCol(doubleArray())
        .addSingleCol(doubleArray(2.25D, 4.5D, 0D))
        .addSingleCol(doubleArray(10.126D))
        .addSingleCol(doubleArray(3D, 2.75D, 1.0D, 0.0D, 5.25D, 0D))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testString() {
    String json =
        "{a: [\"foo\", \"\", null]} {a: []} {a: null} " +
        "{a: \"bar\"} " +
        "{a: [3, 2.75, true, false]}";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("a", MinorType.VARCHAR)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addSingleCol(strArray("foo", "", ""))
        .addSingleCol(strArray())
        .addSingleCol(strArray())
        .addSingleCol(strArray("bar"))
        .addSingleCol(strArray("3", "2.75", "true", "false"))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testStringClassicNulls() {
    String json =
        "{a: [\"foo\", \"\", null]} {a: []} {a: null}";

    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.jsonOptions.classicArrayNulls = true;
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("a", MinorType.VARCHAR)
        .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addSingleCol(strArray("foo", "", "null"))
        .addSingleCol(strArray())
        .addSingleCol(strArray())
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testStringWithSchema() {
    String json =
        "{a: []} {a: null} {a: [\"foo\", \"\", null]} " +
        "{a: \"bar\"} " +
        "{a: [3, 2.75, true, false]}";
    TupleMetadata schema = new SchemaBuilder()
        .addArray("a", MinorType.VARCHAR)
        .build();

    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.builder.providedSchema(schema);
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    RowSet expected = fixture.rowSetBuilder(schema)
        .addSingleCol(strArray())
        .addSingleCol(strArray())
        .addSingleCol(strArray("foo", "", ""))
        .addSingleCol(strArray("bar"))
        .addSingleCol(strArray("3", "2.75", "true", "false"))
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  /**
   * Cannot shift to a nested array from a repeated scalar.
   */
  @Test
  public void testNestedArray() {
    String json =
        "{a: [2, 4]} {a: [[10, 20]]}";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.open(json);
    try {
      loader.next();
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("Structure value found where scalar expected"));
    } finally {
      loader.close();
    }
  }

  /**
   * Cannot shift to an object from a repeated scalar.
   */
  @Test
  public void testArrayWithObject() {
    String json =
        "{a: [2, 4]} {a: {b: 10}}";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.open(json);
    try {
      loader.next();
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("JSON array expected"));
    } finally {
      loader.close();
    }
  }
}
