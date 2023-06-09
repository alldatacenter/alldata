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
package org.apache.drill.exec.record.metadata.schema.parser;

import org.apache.drill.test.BaseTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

public class TestParserErrorHandling extends BaseTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testUnsupportedType() throws Exception {
    String schema = "col unk_type";
    thrown.expect(IOException.class);
    SchemaExprParser.parseSchema(schema);
  }

  @Test
  public void testVarcharWithScale() throws Exception {
    String schema = "col varchar(1, 2)";
    thrown.expect(IOException.class);
    thrown.expectMessage("missing ')' at ','");
    SchemaExprParser.parseSchema(schema);
  }

  @Test
  public void testUnquotedKeyword() throws Exception {
    String schema = "int varchar";
    thrown.expect(IOException.class);
    thrown.expectMessage("mismatched input 'int'");
    SchemaExprParser.parseSchema(schema);
  }

  @Test
  public void testUnquotedId() throws Exception {
    String schema = "id with space varchar";
    thrown.expect(IOException.class);
    SchemaExprParser.parseSchema(schema);
  }

  @Test
  public void testUnescapedBackTick() throws Exception {
    String schema = "`c`o`l` varchar";
    thrown.expect(IOException.class);
    SchemaExprParser.parseSchema(schema);
  }

  @Test
  public void testUnescapedBackSlash() throws Exception {
    String schema = "`c\\o\\l` varchar";
    thrown.expect(IOException.class);
    thrown.expectMessage("extraneous input '`'");
    SchemaExprParser.parseSchema(schema);
  }

  @Test
  public void testMissingType() throws Exception {
    String schema = "col not null";
    thrown.expect(IOException.class);
    SchemaExprParser.parseSchema(schema);
  }

  @Test
  public void testIncorrectEOF() throws Exception {
    String schema = "col int not null footer";
    thrown.expect(IOException.class);
    thrown.expectMessage("extraneous input 'footer'");
    SchemaExprParser.parseSchema(schema);
  }

  @Test
  public void testSchemaWithOneParen() throws Exception {
    String schema = "(col int not null";
    thrown.expect(IOException.class);
    thrown.expectMessage("missing ')' at '<EOF>'");
    SchemaExprParser.parseSchema(schema);
  }

  @Test
  public void testMissingAngleBracket() throws Exception {
    String schema = "col array<int not null";
    thrown.expect(IOException.class);
    thrown.expectMessage("missing '>' at 'not'");
    SchemaExprParser.parseSchema(schema);
  }

  @Test
  public void testUnclosedAngleBracket() throws Exception {
    String schema = "col struct<m array<int> not null";
    thrown.expect(IOException.class);
    thrown.expectMessage("missing '>' at '<EOF>'");
    SchemaExprParser.parseSchema(schema);
  }

  @Test
  public void testMissingColumnNameForStruct() throws Exception {
    String schema = "col struct<int> not null";
    thrown.expect(IOException.class);
    thrown.expectMessage("mismatched input 'int' expecting {ID, QUOTED_ID}");
    SchemaExprParser.parseSchema(schema);
  }

  @Test
  public void testIncorrectMapKeyType() throws Exception {
    String schema = "col map<array<int>, varchar>";
    thrown.expect(IOException.class);
    thrown.expectMessage("mismatched input 'array' expecting {'INT', 'INTEGER',");
    SchemaExprParser.parseSchema(schema);
  }

  @Test
  public void testMapKeyWithName() throws Exception {
    String schema = "col map<`key` int, `value` varchar>";
    thrown.expect(IOException.class);
    thrown.expectMessage("extraneous input '`key`' expecting {'INT', 'INTEGER',");
    SchemaExprParser.parseSchema(schema);
  }

  @Test
  public void testMapMissingComma() throws Exception {
    String schema = "col map<int varchar>";
    thrown.expect(IOException.class);
    thrown.expectMessage("missing ',' at 'varchar'");
    SchemaExprParser.parseSchema(schema);
  }

  @Test
  public void testMissingNotBeforeNull() throws Exception {
    String schema = "col int null";
    thrown.expect(IOException.class);
    thrown.expectMessage("extraneous input 'null'");
    SchemaExprParser.parseSchema(schema);
  }

  @Test
  public void testExtraComma() throws Exception {
    String schema = "id int,, name varchar";
    thrown.expect(IOException.class);
    thrown.expectMessage("extraneous input ',' expecting {ID, QUOTED_ID}");
    SchemaExprParser.parseSchema(schema);
  }

  @Test
  public void testExtraCommaEOF() throws Exception {
    String schema = "id int, name varchar,";
    thrown.expect(IOException.class);
    thrown.expectMessage("mismatched input '<EOF>' expecting {ID, QUOTED_ID}");
    SchemaExprParser.parseSchema(schema);
  }

  @Test
  public void incorrectNumber() throws Exception {
    String schema = "id decimal(5, 02)";
    thrown.expect(IOException.class);
    thrown.expectMessage("extraneous input '2' expecting ')'");
    SchemaExprParser.parseSchema(schema);
  }
}
