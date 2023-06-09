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
package org.apache.drill.exec.store.easy.text.compliant;

import org.apache.drill.TestSelectWithOption;
import org.apache.drill.categories.EvfTest;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.easy.text.TextFormatPlugin;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import static org.apache.drill.test.rowSet.RowSetUtilities.strArray;
import static org.junit.Assert.assertTrue;

/**
 * Test table properties with the compliant text reader. The
 * table properties override selected properties in the format
 * plugin config. The tests here start with a "stock" CSV
 * format plugin config without headers. We then use table
 * properties to vary the table format: without headers, skip
 * first row, with headers.
 * <p>
 * The tests also verify that, without headers, if a schema
 * is provided, the text format plugin will create columns
 * using that schema rather than using the "columns" array
 * column.
 *
 * @see TestSelectWithOption for similar tests using table
 * properties within SQL
 */
@Category(EvfTest.class)
public class TestCsvTableProperties extends BaseCsvTest {

  @BeforeClass
  public static void setup() throws Exception {
    BaseCsvTest.setup(false, false);
  }

  private static final String COL_SCHEMA = "id int not null, name varchar not null";

  private static final String SCHEMA_SQL =
    "create schema (%s) " +
    "for table %s PROPERTIES ('" + TextFormatPlugin.HAS_HEADERS_PROP +
    "'='%s', '" + TextFormatPlugin.SKIP_FIRST_LINE_PROP + "'='%s')";

  private RowSet expectedSchemaRows() {
    TupleMetadata expectedSchema = new SchemaBuilder()
        .add("id", MinorType.INT)
        .add("name", MinorType.VARCHAR)
        .buildSchema();
    return new RowSetBuilder(client.allocator(), expectedSchema)
        .addRow(10, "fred")
        .addRow(20, "wilma")
        .build();
  }

  private RowSet expectedArrayRows() {
    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("columns", MinorType.VARCHAR)
        .buildSchema();
    return new RowSetBuilder(client.allocator(), expectedSchema)
        .addSingleCol(strArray("10", "fred"))
        .addSingleCol(strArray("20", "wilma"))
        .build();
  }

  private static final String SELECT_ALL = "SELECT * FROM %s";

  private static final String[] noHeaders = {
      "10,fred",
      "20,wilma"
  };

  @Test
  public void testNoHeadersWithSchema() throws Exception {
    try {
      enableSchemaSupport();
      String tablePath = buildTable("noHwS", noHeaders);
      run(SCHEMA_SQL, COL_SCHEMA, tablePath, false, false);
      RowSet actual = client.queryBuilder().sql(SELECT_ALL, tablePath).rowSet();
      RowSetUtilities.verify(expectedSchemaRows(), actual);
    } finally {
      resetSchemaSupport();
    }
  }

  @Test
  public void testNoHeadersWithoutSchema() throws Exception {
    try {
      enableSchemaSupport();
      String tablePath = buildTable("noHnoS", noHeaders);
      run(SCHEMA_SQL, "", tablePath, false, false);
      RowSet actual = client.queryBuilder().sql(SELECT_ALL, tablePath).rowSet();
      RowSetUtilities.verify(expectedArrayRows(), actual);
    } finally {
      resetSchemaSupport();
    }
  }

  private static final String[] extraCols = {
      "10,fred,23.45",
      "20,wilma,1234.56,vip"
  };

  @Test
  public void testNoHeadersWithSchemaExtraCols() throws Exception {
    try {
      enableSchemaSupport();
      String tablePath = buildTable("extraCols", extraCols);
      run(SCHEMA_SQL, COL_SCHEMA, tablePath, false, false);
      RowSet actual = client.queryBuilder().sql(SELECT_ALL, tablePath).rowSet();
      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("id", MinorType.INT)
          .add("name", MinorType.VARCHAR)
          .buildSchema();
      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addRow(10, "fred")
          .addRow(20, "wilma")
          .build();
      RowSetUtilities.verify(expected, actual);
    } finally {
      resetSchemaSupport();
    }
  }

  private static final String[] skipHeaders = {
      "ignore,me",
      "10,fred",
      "20,wilma"
  };

  @Test
  public void testSkipHeadersWithSchema() throws Exception {
    try {
      enableSchemaSupport();
      String tablePath = buildTable("skipHwS", skipHeaders);
      run(SCHEMA_SQL, COL_SCHEMA, tablePath, false, true);
      RowSet actual = client.queryBuilder().sql(SELECT_ALL, tablePath).rowSet();
      RowSetUtilities.verify(expectedSchemaRows(), actual);
    } finally {
      resetSchemaSupport();
    }
  }

  @Test
  public void testSkipHeadersWithoutSchema() throws Exception {
    try {
      enableSchemaSupport();
      String tablePath = buildTable("skipHnoS", skipHeaders);
      run(SCHEMA_SQL, "", tablePath, false, true);
      RowSet actual = client.queryBuilder().sql(SELECT_ALL, tablePath).rowSet();
      RowSetUtilities.verify(expectedArrayRows(), actual);
    } finally {
      resetSchemaSupport();
    }
  }

  private static final String[] withHeaders = {
      "id, name",
      "10,fred",
      "20,wilma"
  };

  @Test
  public void testHeadersWithSchema() throws Exception {
    try {
      enableSchemaSupport();
      String tablePath = buildTable("headwS", withHeaders);
      run(SCHEMA_SQL, COL_SCHEMA, tablePath, true, false);
      RowSet actual = client.queryBuilder().sql(SELECT_ALL, tablePath).rowSet();
      RowSetUtilities.verify(expectedSchemaRows(), actual);
    } finally {
      resetSchemaSupport();
    }
  }

  @Test
  public void testHeadersWithoutSchema() throws Exception {
    try {
      enableSchemaSupport();
      String tablePath = buildTable("headnoS", withHeaders);
      run(SCHEMA_SQL, "", tablePath, true, false);
      RowSet actual = client.queryBuilder().sql(SELECT_ALL, tablePath).rowSet();
      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("id", MinorType.VARCHAR)
          .add("name", MinorType.VARCHAR)
          .buildSchema();
      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addRow("10", "fred")
          .addRow("20", "wilma")
          .build();
      RowSetUtilities.verify(expected, actual);
    } finally {
      resetSchemaSupport();
    }
  }

  private static final String[] barDelim = {
      "10|fred",
      "20|wilma"
  };

  @Test
  public void testDelimiter() throws Exception {
    try {
      enableSchemaSupport();
      String tablePath = buildTable("barDelim", barDelim);
      String sql = String.format(SCHEMA_SQL, COL_SCHEMA, tablePath, false, false);
      sql = sql.substring(0, sql.length() - 1) +
          ", '" + TextFormatPlugin.DELIMITER_PROP + "'='|')";
      run(sql);
      RowSet actual = client.queryBuilder().sql(SELECT_ALL, tablePath).rowSet();
      RowSetUtilities.verify(expectedSchemaRows(), actual);
    } finally {
      resetSchemaSupport();
    }
  }

  private static final String[] customCommentChar = {
      "@Comment",
      "#10,fred",
      "#20,wilma"
  };

  private RowSet expectedCommentRows() {
    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("columns", MinorType.VARCHAR)
        .buildSchema();
    return new RowSetBuilder(client.allocator(), expectedSchema)
        .addSingleCol(strArray("#10", "fred"))
        .addSingleCol(strArray("#20", "wilma"))
        .build();
  }

  @Test
  public void testComment() throws Exception {
    try {
      enableSchemaSupport();
      String tablePath = buildTable("customCommentChar", customCommentChar);
      String sql = String.format(SCHEMA_SQL, "", tablePath, false, false);
      sql = sql.substring(0, sql.length() - 1) +
          ", '" + TextFormatPlugin.COMMENT_CHAR_PROP + "'='@')";
      run(sql);
      RowSet actual = client.queryBuilder().sql(SELECT_ALL, tablePath).rowSet();
      RowSetUtilities.verify(expectedCommentRows(), actual);
    } finally {
      resetSchemaSupport();
    }
  }

  private static final String[] noCommentChar = {
      "#10,fred",
      "#20,wilma"
  };

  /**
   * Users have complained about the comment character. We usually
   * suggest they change it to some other character. This test verifies
   * that the plugin will choose the ASCII NUL (0) character if the
   * comment property is set to a blank string. Since NUL never occurs
   * in the input, the result is to essentially disable comment support.
   */
  @Test
  public void testNoComment() throws Exception {
    try {
      enableSchemaSupport();
      String tablePath = buildTable("noCommentChar", noCommentChar);
      String sql = String.format(SCHEMA_SQL, "", tablePath, false, false);
      sql = sql.substring(0, sql.length() - 1) +
          ", '" + TextFormatPlugin.COMMENT_CHAR_PROP + "'='')";
      run(sql);
      RowSet actual = client.queryBuilder().sql(SELECT_ALL, tablePath).rowSet();
      RowSetUtilities.verify(expectedCommentRows(), actual);
    } finally {
      resetSchemaSupport();
    }
  }

  private static final String[] quotesData = {
    "1,@foo@",
    "2,@foo~@bar@",

    // Test proper handling of escapes.
    "3,@foo~bar@",
    "4,@foo~~bar@"
  };

  /**
   * Test quote and quote escape
   */
  @Test
  public void testQuoteChars() throws Exception {
    try {
      enableSchemaSupport();
      String tablePath = buildTable("customQuotes", quotesData);
      String sql = "create schema () " +
          "for table " + tablePath + " PROPERTIES ('" +
          TextFormatPlugin.HAS_HEADERS_PROP + "'='false', '" +
          TextFormatPlugin.SKIP_FIRST_LINE_PROP + "'='false', '" +
          TextFormatPlugin.QUOTE_PROP + "'='@', '" +
          TextFormatPlugin.QUOTE_ESCAPE_PROP + "'='~')";
      run(sql);
      RowSet actual = client.queryBuilder().sql(SELECT_ALL, tablePath).rowSet();
      TupleMetadata expectedSchema = new SchemaBuilder()
          .addArray("columns", MinorType.VARCHAR)
          .buildSchema();
      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addSingleCol(strArray("1", "foo"))
          .addSingleCol(strArray("2", "foo@bar"))
          .addSingleCol(strArray("3", "foo~bar"))
          .addSingleCol(strArray("4", "foo~~bar"))
          .build();
      RowSetUtilities.verify(expected, actual);
    } finally {
      resetSchemaSupport();
    }
  }

  private static final String[] doubleQuotesData = {
      "1,@foo@",
      "2,@foo@@bar@",
    };

  /**
   * Test that the quote escape can be the quote character
   * itself. In this case, &lt;escape>&<lt;escape> is the
   * same as &lt;quote>&lt;quote> and is considered to
   * be an escaped quote. There is no "orphan" escape
   * case.
   */
  @Test
  public void testDoubleQuoteChars() throws Exception {
    try {
      enableSchemaSupport();
      String tablePath = buildTable("doubleQuotes", doubleQuotesData);
      String sql = "create schema () " +
          "for table " + tablePath + " PROPERTIES ('" +
          TextFormatPlugin.HAS_HEADERS_PROP + "'='false', '" +
          TextFormatPlugin.SKIP_FIRST_LINE_PROP + "'='false', '" +
          TextFormatPlugin.QUOTE_PROP + "'='@', '" +
          TextFormatPlugin.QUOTE_ESCAPE_PROP + "'='@')";
      run(sql);
      RowSet actual = client.queryBuilder().sql(SELECT_ALL, tablePath).rowSet();
      TupleMetadata expectedSchema = new SchemaBuilder()
          .addArray("columns", MinorType.VARCHAR)
          .buildSchema();
      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addSingleCol(strArray("1", "foo"))
          .addSingleCol(strArray("2", "foo@bar"))
           .build();
      RowSetUtilities.verify(expected, actual);
    } finally {
      resetSchemaSupport();
    }
  }

  private static final String[] quotesAndCustomNewLineData = {
    "1,@foo@!2,@foo@@bar@!",
  };

  @Test
  public void testQuotesAndCustomNewLine() throws Exception {
    try {
      enableSchemaSupport();
      String tablePath = buildTable("quotesAndCustomNewLine", quotesAndCustomNewLineData);
      String sql = "create schema () " +
        "for table " + tablePath + " PROPERTIES ('" +
        TextFormatPlugin.HAS_HEADERS_PROP + "'='false', '" +
        TextFormatPlugin.SKIP_FIRST_LINE_PROP + "'='false', '" +
        TextFormatPlugin.LINE_DELIM_PROP + "'='!', '" +
        TextFormatPlugin.QUOTE_PROP + "'='@', '" +
        TextFormatPlugin.QUOTE_ESCAPE_PROP + "'='@')";
      run(sql);
      RowSet actual = client.queryBuilder().sql(SELECT_ALL, tablePath).rowSet();
      TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("columns", MinorType.VARCHAR)
        .buildSchema();
      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
        .addSingleCol(strArray("1", "foo"))
        .addSingleCol(strArray("2", "foo@bar"))
        .build();
      RowSetUtilities.verify(expected, actual);
    } finally {
      resetSchemaSupport();
    }
  }

  private static final String[] specialCharsData = {
      "10\u0001'fred'",
      "20\u0001'wilma'"
    };

 /**
   * End-to-end test of special characters for delimiter (a control
   * character, ASCII 0x01) and quote (same as the SQL quote.)
   */
  @Test
  public void testSpecialChars() throws Exception {
    try {
      enableSchemaSupport();
      String tablePath = buildTable("specialChars", specialCharsData);
      String sql = String.format("create schema (%s) " +
          "for table %s PROPERTIES ('" +
          TextFormatPlugin.HAS_HEADERS_PROP + "'='false', '" +
          TextFormatPlugin.SKIP_FIRST_LINE_PROP + "'='false', '" +
          // Obscure Calcite parsing feature. See
          // SqlParserUtil.checkUnicodeEscapeChar()
          // See also https://issues.apache.org/jira/browse/CALCITE-2273
          // \U0001 also seems to work.
          TextFormatPlugin.DELIMITER_PROP + "'='\01', '" +
          // Looks like the lexer accepts Java escapes: \n, \r,
          // presumably \t, though not tested here.
          TextFormatPlugin.LINE_DELIM_PROP + "'='\n', '" +
          // See: http://drill.apache.org/docs/lexical-structure/#string
          TextFormatPlugin.QUOTE_PROP + "'='''')",
          COL_SCHEMA, tablePath);
      run(sql);
      RowSet actual = client.queryBuilder().sql(SELECT_ALL, tablePath).rowSet();
      RowSetUtilities.verify(expectedSchemaRows(), actual);
    } finally {
      resetSchemaSupport();
    }
  }

  /**
   * Verify that a custom newline character works, and that the symbol
   * '\n' can be used in SQL and is stored properly in the schema file.
   */
  @Test
  public void testNewlineProp() throws Exception {
    try {
      enableSchemaSupport();
      String tableName = "newline";
      File rootDir = new File(testDir, tableName);
      assertTrue(rootDir.mkdir());
      try(PrintWriter out = new PrintWriter(new FileWriter(new File(rootDir, ROOT_FILE)))) {
        out.print("1,fred\r2,wilma\r");
      }
      String tablePath = "`dfs.data`.`" + tableName + "`";
      String sql = "create schema () " +
          "for table " + tablePath + " PROPERTIES ('" +
          TextFormatPlugin.HAS_HEADERS_PROP + "'='false', '" +
          TextFormatPlugin.SKIP_FIRST_LINE_PROP + "'='false', '" +
          TextFormatPlugin.LINE_DELIM_PROP + "'='\r')";
      run(sql);
      RowSet actual = client.queryBuilder().sql(SELECT_ALL, tablePath).rowSet();
      TupleMetadata expectedSchema = new SchemaBuilder()
          .addArray("columns", MinorType.VARCHAR)
          .buildSchema();
      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addSingleCol(strArray("1", "fred"))
          .addSingleCol(strArray("2", "wilma"))
          .build();
      RowSetUtilities.verify(expected, actual);
    } finally {
      resetSchemaSupport();
    }
  }

  private static final String[] messyQuotesData = {
      "first\"field\"here,another \"field",
      "end quote\",another\"",
      "many\"\"\"\",more\"\"",
      "\"not\"end\",\"\"wtf\" \"",
      "\"newline\nhere\",\"and here\"\"\n\""
    };

  /**
   * The legacy "V2" text reader had special handling for quotes
   * that appear inside fields. Example:<pre><tt>
   * first"field"here,another "field</tt></pre>
   * <p>
   * Since behavior in this case is ill-defined, the reader
   * apparently treated quotes as normal characters unless the
   * field started with a quote. There is an option in the UniVocity
   * code to set this behavior, but it is not exposed in Drill.
   * So, this test verifies the non-customizable messy quote handling
   * logic.
   * <p>
   * If a field starts with a quote, quoting rules kick in, including
   * the quote escape, which is, by default, itself a quote. So
   * <br><code>"foo""bar"</code><br>
   * is read as
   * <br><code>foo"bar</code><br>
   * But, for fields not starting with a quote, the quote escape
   * is ignored, so:
   * <br><code>foo""bar</code><br>
   * is read as
   * <br><code>foo""bar</code><br>
   * This seems more like a bug than a feature, but it does appear to be
   * how the "new" text reader always worked, so the behavior is preserved.
   * <p>
   * Also, seems that the text reader supported embedded newlines, even
   * though such behavior <i><b>will not work</b></i> if the embedded
   * newline occurs near a split. In this case, the reader will scan
   * forward to find a record delimiter (a newline by default), will
   * find the embedded newline, and will read a partial first record.
   * Again, this appears to be legacy behavior, and so is preserved,
   * even if broken.
   * <p>
   * The key thing is that if the CSV is well-formed (no messy quotes,
   * properly quoted fields with proper escapes, no embedded newlines)
   * then things will work OK.
   */
  @Test
  public void testMessyQuotes() throws Exception {
   String tablePath = buildTable("messyQuotes", messyQuotesData);
    RowSet actual = client.queryBuilder().sql(SELECT_ALL, tablePath).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("columns", MinorType.VARCHAR)
        .buildSchema();
    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
        .addSingleCol(strArray("first\"field\"here", "another \"field"))
        .addSingleCol(strArray("end quote\"", "another\""))
        .addSingleCol(strArray("many\"\"\"\"", "more\"\""))
        .addSingleCol(strArray("not\"end", "\"wtf\" "))
        .addSingleCol(strArray("newline\nhere", "and here\"\n"))
        .build();
    RowSetUtilities.verify(expected, actual);
  }

  private static final String[] trimData = {
      " 10 , fred ",
      " 20, wilma "
    };

  /**
   * Trim leading and trailing whitespace. This setting is currently
   * only available via table properties.
   */
  @Test
  public void testKeepWitespace() throws Exception {
    try {
      enableSchemaSupport();
      String tablePath = buildTable("noTrim", trimData);
      String sql = String.format("create schema (%s) " +
          "for table %s PROPERTIES ('" +
          TextFormatPlugin.HAS_HEADERS_PROP + "'='false', '" +
          TextFormatPlugin.SKIP_FIRST_LINE_PROP + "'='false')",
          COL_SCHEMA, tablePath);
      run(sql);
      RowSet actual = client.queryBuilder().sql(SELECT_ALL, tablePath).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("id", MinorType.INT)
          .add("name", MinorType.VARCHAR)
          .buildSchema();

      // String-to-number conversion trims strings automatically
      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addRow(10, " fred ")
          .addRow(20, " wilma ")
          .build();
      RowSetUtilities.verify(expected, actual);
    } finally {
      resetSchemaSupport();
    }
  }

 /**
   * Trim leading and trailing whitespace. This setting is currently
   * only available via table properties.
   */
  @Test
  public void testTrimWitespace() throws Exception {
    try {
      enableSchemaSupport();
      String tablePath = buildTable("trim", trimData);
      String sql = String.format("create schema (%s) " +
          "for table %s PROPERTIES ('" +
          TextFormatPlugin.HAS_HEADERS_PROP + "'='false', '" +
          TextFormatPlugin.SKIP_FIRST_LINE_PROP + "'='false', '" +
          TextFormatPlugin.TRIM_WHITESPACE_PROP + "'='true')",
          COL_SCHEMA, tablePath);
      run(sql);
      RowSet actual = client.queryBuilder().sql(SELECT_ALL, tablePath).rowSet();
      RowSetUtilities.verify(expectedSchemaRows(), actual);
    } finally {
      resetSchemaSupport();
    }
  }
}
