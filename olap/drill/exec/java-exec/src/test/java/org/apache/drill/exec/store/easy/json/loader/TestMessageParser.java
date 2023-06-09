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
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.easy.json.parser.MessageParser;
import org.apache.drill.exec.store.easy.json.parser.TokenIterator;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fasterxml.jackson.core.JsonToken;

@Category(JsonTest.class)
public class TestMessageParser extends BaseJsonLoaderTest {

  /**
   * Example message parser. A real parser would provide much better
   * error messages for badly-formed JSON or error codes.
   */
  private static class MessageParserFixture implements MessageParser {

    @Override
    public boolean parsePrefix(TokenIterator tokenizer) {
      assertEquals(JsonToken.START_OBJECT, tokenizer.requireNext());
      assertEquals(JsonToken.FIELD_NAME, tokenizer.requireNext());
      assertEquals(JsonToken.VALUE_STRING, tokenizer.requireNext());
      if (!"ok".equals(tokenizer.stringValue())) {
        return false;
      }
      assertEquals(JsonToken.FIELD_NAME, tokenizer.requireNext());
      JsonToken token = tokenizer.requireNext();
      assertEquals(JsonToken.START_ARRAY, token);
      tokenizer.unget(token);
      return true;
    }

    @Override
    public void parseSuffix(TokenIterator tokenizer) {
      assertEquals(JsonToken.END_OBJECT, tokenizer.requireNext());
    }
  }

  /**
   * Test the ability to wrap the data objects with a custom message
   * structure, typical of a REST call.
   */
  @Test
  public void testMessageParser() {
    final String json =
        "{ status: \"ok\", data: [{a: 0}, {a: 100}, {a: null}]}";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.builder.messageParser(new MessageParserFixture());
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("a", MinorType.BIGINT)
         .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(0)
        .addRow(100)
        .addSingleCol(null)
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  /**
   * Test the ability to cancel the data load if a message header
   * indicates that there is no data.
   */
  @Test
  public void testMessageParserEOF() {
    final String json =
        "{ status: \"fail\", data: [{a: 0}, {a: 100}, {a: null}]}";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.builder.messageParser(new MessageParserFixture());
    loader.open(json);
    assertNull(loader.next());
    loader.close();
  }

  /**
   * Test the case where the returned message has a single data
   * object: <code>{ data: { ... } }</code>.
   */
  @Test
  public void testDataPathObject() {
    final String json =
        "{ status: \"ok\", data: [{a: 0}, {a: 100}, {a: null}]}";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.builder.dataPath("data");
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("a", MinorType.BIGINT)
         .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(0)
        .addRow(100)
        .addSingleCol(null)
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  /**
   * Test the case where the returned message has an array
   * objects: <code>{ data: [ { ... }, { ... } ... ] }</code>.
   */
  @Test
  public void testDataPathArray() {
    final String json =
        "{ status: \"ok\", data: [{a: 0}, {a: 100}, {a: null}]}";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.builder.dataPath("data");
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("a", MinorType.BIGINT)
         .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(0)
        .addRow(100)
        .addSingleCol(null)
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testComplexDataPath() {
    final String json =
        "{ status: {result : \"ok\", runtime: 123},\n" +
        "  response: { rowCount: 1,\n" +
        "    data: [{a: 0}, {a: 100}, {a: null}]},\n" +
        "  footer: \"some stuff\"}";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.builder.dataPath("response/data");
    loader.open(json);
    RowSet results = loader.next();
    assertNotNull(results);

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("a", MinorType.BIGINT)
         .build();
    RowSet expected = fixture.rowSetBuilder(expectedSchema)
        .addRow(0)
        .addRow(100)
        .addSingleCol(null)
        .build();
    RowSetUtilities.verify(expected, results);
    assertNull(loader.next());
    loader.close();
  }

  /**
   * Test the case where the returned message has a null in place
   * of the data: <code>{ data: null }</code>. This is harmlessly
   * treated as no data and is needed for the case where the
   * message normally returns a single object.
   */
  @Test
  public void testDataPathNull() {
    final String json =
        "{ status: \"fail\", data: null}";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.builder.messageParser(new MessageParserFixture());
    loader.open(json);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testDataPathMissing() {
    final String json =
        "{ status: \"fail\"}";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.builder.messageParser(new MessageParserFixture());
    loader.open(json);
    assertNull(loader.next());
    loader.close();
  }

  @Test
  public void testDataPathErrorRoot() {
    final String json = "\"Bogus!\"";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.builder.dataPath("data");
    try {
      loader.open(json);
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("Syntax error"));
      assertTrue(e.getCause() instanceof MessageParser.MessageContextException);
    }
  }

  @Test
  public void testDataPathErrorLeaf() {
    final String json =
        "{ status: \"bogus\", data: \"must be array or object\" }";
    JsonLoaderFixture loader = new JsonLoaderFixture();
    loader.builder.dataPath("data");
    try {
      loader.open(json);
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("Syntax error"));
      assertTrue(e.getCause() instanceof MessageParser.MessageContextException);
    }
  }
}
