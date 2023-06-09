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
package org.apache.drill;

import static java.lang.String.format;
import static org.apache.drill.test.TestBuilder.listOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.drill.categories.SqlTest;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.TestBuilder;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

@Category(SqlTest.class)
public class TestSelectWithOption extends ClusterTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @BeforeClass
  public static void setUp() throws Exception {
    startCluster(ClusterFixture.builder(dirTestWatcher));
  }

  private File genCSVFile(String name, String... rows) throws IOException {
    File file = new File(format("%s/%s.csv", dirTestWatcher.getRootDir(), name));
    try (FileWriter fw = new FileWriter(file)) {
      for (String row : rows) {
        fw.append(row).append("\n");
      }
    }
    return file;
  }

  private String genCSVTable(String name, String... rows) throws IOException {
    File f = genCSVFile(name, rows);
    return format("dfs.`%s`", f.getName());
  }

  private void testWithResult(String query, Object... expectedResult) throws Exception {
    TestBuilder builder = testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("columns");
    for (Object o : expectedResult) {
      builder = builder.baselineValues(o);
    }
    builder.build().run();
  }

  @Test
  public void testTextFieldDelimiter() throws Exception {
    String tableName = genCSVTable("testTextFieldDelimiter",
        "\"b\"|\"0\"",
        "\"b\"|\"1\"",
        "\"b\"|\"2\"");

    String queryTemplate = "select columns from table(%s (type => 'TeXT', fieldDelimiter => '%s'))";

    testWithResult(format(queryTemplate, tableName, ","),
        listOf("b\"|\"0"),
        listOf("b\"|\"1"),
        listOf("b\"|\"2"));

    testWithResult(format(queryTemplate, tableName, "|"),
        listOf("b", "0"),
        listOf("b", "1"),
        listOf("b", "2"));
  }

  @Test
  public void testTabFieldDelimiter() throws Exception {
    String tableName = genCSVTable("testTabFieldDelimiter",
        "1\ta",
        "2\tb");
    String fieldDelimiter = new String(new char[]{92, 116}); // represents \t
    testWithResult(format("select columns from table(%s(type=>'TeXT', fieldDelimiter => '%s'))", tableName, fieldDelimiter),
        listOf("1", "a"),
        listOf("2", "b"));
  }

  @Test
  public void testSingleTextLineDelimiter() throws Exception {
    String tableName = genCSVTable("testSingleTextLineDelimiter",
        "a|b|c");

    testWithResult(format("select columns from table(%s(type => 'TeXT', lineDelimiter => '|'))", tableName),
        listOf("a"),
        listOf("b"),
        listOf("c"));
  }

  @Test
  // '\n' is treated as standard delimiter
  // if user has indicated custom line delimiter but input file contains '\n', split will occur on both
  public void testCustomTextLineDelimiterAndNewLine() throws Exception {
    String tableName = genCSVTable("testTextLineDelimiter",
        "b|1",
        "b|2");

    testWithResult(format("select columns from table(%s(type => 'TeXT', lineDelimiter => '|'))", tableName),
        listOf("b"),
        listOf("1"),
        listOf("b"),
        listOf("2"));
  }

  @Test
  public void testTextLineDelimiterWithCarriageReturn() throws Exception {
    String tableName = genCSVTable("testTextLineDelimiterWithCarriageReturn",
        "1, a\r",
        "2, b\r");
    testWithResult(format("select columns from table(%s(type=>'TeXT', fieldDelimiter => '*', lineDelimiter => '\\r\\n'))", tableName),
        listOf("1, a"),
        listOf("2, b"));
  }

  @Test
  public void testMultiByteLineDelimiter() throws Exception {
    String tableName = genCSVTable("testMultiByteLineDelimiter",
        "1abc2abc3abc");
    testWithResult(format("select columns from table(%s(type=>'TeXT', lineDelimiter => 'abc'))", tableName),
        listOf("1"),
        listOf("2"),
        listOf("3"));
  }

  @Test
  public void testDataWithPartOfMultiByteLineDelimiter() throws Exception {
    String tableName = genCSVTable("testDataWithPartOfMultiByteLineDelimiter",
        "ab1abc2abc3abc");
    testWithResult(format("select columns from table(%s(type=>'TeXT', lineDelimiter => 'abc'))", tableName),
        listOf("ab1"),
        listOf("2"),
        listOf("3"));
  }

  @Test
  public void testTextQuote() throws Exception {
    String tableName = genCSVTable("testTextQuote",
        "\"b\"|\"0\"",
        "\"b\"|\"1\"",
        "\"b\"|\"2\"");

    testWithResult(format("select columns from table(%s(type => 'TeXT', fieldDelimiter => '|', quote => '@'))", tableName),
        listOf("\"b\"", "\"0\""),
        listOf("\"b\"", "\"1\""),
        listOf("\"b\"", "\"2\""));

    String quoteTableName = genCSVTable("testTextQuote2",
        "@b@|@0@",
        "@b$@c@|@1@");
    // It seems that a parameter can not be called "escape"
    testWithResult(format("select columns from table(%s(`escape` => '$', type => 'TeXT', fieldDelimiter => '|', quote => '@'))", quoteTableName),
        listOf("b", "0"),
        listOf("b@c", "1"));
  }

  @Test
  public void testTextComment() throws Exception {
      String commentTableName = genCSVTable("testTextComment",
          "b|0",
          "@ this is a comment",
          "b|1");
      testWithResult(format("select columns from table(%s(type => 'TeXT', fieldDelimiter => '|', comment => '@'))", commentTableName),
          listOf("b", "0"),
          listOf("b", "1"));
  }

  @Test
  public void testTextHeader() throws Exception {
    String headerTableName = genCSVTable("testTextHeader",
        "b|a",
        "b|0",
        "b|1");
    testWithResult(format("select columns from table(%s(type => 'TeXT', fieldDelimiter => '|', skipFirstLine => true))", headerTableName),
        listOf("b", "0"),
        listOf("b", "1"));

    testBuilder()
        .sqlQuery(format("select a, b from table(%s(type => 'TeXT', fieldDelimiter => '|', extractHeader => true))", headerTableName))
        .ordered()
        .baselineColumns("b", "a")
        .baselineValues("b", "0")
        .baselineValues("b", "1")
        .go();
  }

  @Test
  public void testVariationsCSV() throws Exception {
    String csvTableName = genCSVTable("testVariationsCSV",
        "a,b",
        "c|d");
    // The default field delimiter is ',', change it to something else.
    // Using the defaults in TextFormatConfig (the field delimiter is neither "," not "|")
    testWithResult(format("select columns from table(%s (type => 'TeXT', fieldDelimiter => '*'))", csvTableName),
      listOf("a,b"),
      listOf("c|d"));
    // the drill config file binds .csv to "," delimited
    testWithResult(format("select columns from %s", csvTableName),
          listOf("a", "b"),
          listOf("c|d"));
    // Default delimiter for csv
    testWithResult(format("select columns from table(%s (type => 'TeXT'))", csvTableName),
        listOf("a", "b"),
        listOf("c|d"));
    // Setting the delimiter
    testWithResult(format("select columns from table(%s (type => 'TeXT', fieldDelimiter => '|'))", csvTableName),
        listOf("a,b"),
        listOf("c", "d"));
  }

  @Test
  public void testVariationsJSON() throws Exception {
    String jsonTableName = genCSVTable("testVariationsJSON",
        "{\"columns\": [\"f\",\"g\"]}");
    // the extension is actually csv
    // Don't try to read the CSV file, however, as it does not
    // contain proper quotes for CSV.
    // File contents:
    // {"columns": ["f","g"]}
    // CSV would require:
    // "{""columns"": [""f"",""g""]}"
    // A bug in older versions appeared to have the perverse
    // effect of allowing the above to kinda-sorta work.
    String[] jsonQueries = {
        format("select columns from table(%s(type => 'JSON'))", jsonTableName),
        // we can use named format plugin configurations too!
        format("select columns from table(%s(type => 'Named', name => 'json'))", jsonTableName),
    };
    for (String jsonQuery : jsonQueries) {
      testWithResult(jsonQuery, listOf("f","g"));
    }
  }

  @Test
  public void testUse() throws Exception {
    File f = genCSVFile("testUse",
        "{\"columns\": [\"f\",\"g\"]}");
    String jsonTableName = String.format("dfs.`%s`", f.getName());
    // the extension is actually csv
    run("use dfs");
    try {
      testWithResult(format("select columns from table(%s(type => 'JSON'))", jsonTableName), listOf("f","g"));
      testWithResult(format("select length(columns[0]) as columns from table(%s (type => 'JSON'))", jsonTableName), 1L);
    } finally {
      run("use sys");
    }
  }

  @Test(expected = UserRemoteException.class)
  public void testAbsentTable() throws Exception {
    String schema = "cp.default";
    String tableName = "absent_table";
    try {
      run("select * from table(`%s`.`%s`(type=>'parquet'))", schema, tableName);
    } catch (UserRemoteException e) {
      assertThat(e.getMessage(), containsString(String.format("Unable to find table [%s]", tableName)));
      throw e;
    }
  }

  @Test
  public void testTableFunctionWithDirectoryExpansion() throws Exception {
    String tableName = "dirTable";
    String query = "select 'A' as col from (values(1))";
    run("use dfs.tmp");
    try {
      client.alterSession(ExecConstants.OUTPUT_FORMAT_OPTION, "csv");
      run("create table %s as %s", tableName, query);

      testBuilder()
        .sqlQuery("select * from table(%s(type=>'text', fieldDelimiter => ',', extractHeader => true))", tableName)
        .unOrdered()
        .sqlBaselineQuery(query)
        .go();
    } finally {
      client.resetSession(ExecConstants.OUTPUT_FORMAT_OPTION);
      run("drop table if exists %s", tableName);
    }
  }

  @Test
  public void testTableFunctionWithEmptyDirectory() throws Exception {
    String tableName = "emptyTable";
    dirTestWatcher.makeTestTmpSubDir(Paths.get(tableName));
    testBuilder()
      .sqlQuery("select * from table(dfs.tmp.`%s`(type=>'text', fieldDelimiter => ',', extractHeader => true))", tableName)
      .expectsEmptyResultSet()
      .go();
  }

  @Test
  public void testTableFunctionWithNonExistingTable() throws Exception {
    thrown.expect(UserException.class);
    thrown.expectMessage("Unable to find table");
    run("select * from table(dfs.tmp.`nonExistingTable`(schema=>'inline=(mykey int)'))");
  }

}
