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
package org.apache.drill.exec.physical.impl.writer;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.drill.exec.store.easy.text.TextFormatPlugin.TextFormatConfig;
import static org.junit.Assert.assertEquals;

public class TestTextWriter extends ClusterTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static final List<String> tablesToDrop = new ArrayList<>();

  @BeforeClass
  public static void setup() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    startCluster(builder);

    Map<String, FormatPluginConfig> formats = new HashMap<>();

    formats.put("csv", new TextFormatConfig(
        Collections.singletonList("csv"),
        "\n",  // line delimiter
        ",",   // field delimiter
        "\"",  // quote
        "\"",  // escape
        null,  // comment
        false, // skip first line
        true   // extract header
        ));

    formats.put("tsv", new TextFormatConfig(
        Collections.singletonList("tsv"),
        "\n",  // line delimiter
        "\t",  // field delimiter
        "\"",  // quote
        "\"",  // escape
        null,  // comment
        false, // skip first line
        true   // extract header
        ));

    formats.put("custom", new TextFormatConfig(
        Collections.singletonList("custom"),
        "!",   // line delimiter
        "_",   // field delimiter
        "$",   // quote
        "^",   // escape
        null,  // comment
        false, // skip first line
        true   // extract header
        ));

    cluster.defineFormats("dfs", formats);
  }

  @After
  public void cleanUp() {
    client.resetSession(ExecConstants.OUTPUT_FORMAT_OPTION);
    client.resetSession(ExecConstants.TEXT_WRITER_ADD_HEADER);
    client.resetSession(ExecConstants.TEXT_WRITER_FORCE_QUOTES);

    tablesToDrop.forEach(
      table -> client.runSqlSilently(String.format("drop table if exists %s", table)));
  }

  @Test
  public void testWithHeaders() throws Exception {
    client.alterSession(ExecConstants.OUTPUT_FORMAT_OPTION, "csv");

    String tableName = "csv_with_headers_table";
    String fullTableName = String.format("dfs.tmp.`%s`", tableName);
    tablesToDrop.add(fullTableName);

    queryBuilder().sql("create table %s as select 'a' as col1, 'b' as col2 from (values(1))", fullTableName).run();

    Path path = Paths.get(dirTestWatcher.getDfsTestTmpDir().getAbsolutePath(), tableName, "0_0_0.csv");
    List<String> lines = Files.readAllLines(path);
    assertEquals(Arrays.asList("col1,col2", "a,b"), lines);
  }

  @Test
  public void testWithoutHeaders() throws Exception {
    client.alterSession(ExecConstants.OUTPUT_FORMAT_OPTION, "csv");
    client.alterSession(ExecConstants.TEXT_WRITER_ADD_HEADER, false);

    String tableName = "csv_without_headers_table";
    String fullTableName = String.format("dfs.tmp.`%s`", tableName);
    tablesToDrop.add(fullTableName);

    queryBuilder().sql("create table %s as select 'a' as col1, 'b' as col2 from (values(1))", fullTableName).run();

    Path path = Paths.get(dirTestWatcher.getDfsTestTmpDir().getAbsolutePath(), tableName, "0_0_0.csv");
    List<String> lines = Files.readAllLines(path);
    assertEquals(Collections.singletonList("a,b"), lines);
  }

  @Test
  public void testNoQuotes() throws Exception {
    client.alterSession(ExecConstants.OUTPUT_FORMAT_OPTION, "csv");

    String tableName = "csv_no_quotes_table";
    String fullTableName = String.format("dfs.tmp.`%s`", tableName);
    tablesToDrop.add(fullTableName);

    queryBuilder().sql("create table %s as " +
      "select 1 as id, 'Bob' as name, 'A B C' as desc from (values(1))", fullTableName).run();

    testBuilder()
      .sqlQuery("select * from %s", fullTableName)
      .unOrdered()
      .baselineColumns("id", "name", "desc")
      .baselineValues("1", "Bob", "A B C")
      .go();

    Path path = Paths.get(dirTestWatcher.getDfsTestTmpDir().getAbsolutePath(), tableName, "0_0_0.csv");
    List<String> lines = Files.readAllLines(path);
    assertEquals(Arrays.asList("id,name,desc", "1,Bob,A B C"), lines);
  }

  @Test
  public void testQuotesOnDemand() throws Exception {
    client.alterSession(ExecConstants.OUTPUT_FORMAT_OPTION, "csv");

    String tableName = "csv_quotes_on_demand_table";
    String fullTableName = String.format("dfs.tmp.`%s`", tableName);
    tablesToDrop.add(fullTableName);

    queryBuilder().sql("create table %s as " +
      "select 1 as id, 'Bob\nSmith' as name, 'A,B,C' as desc from (values(1))", fullTableName).run();

    testBuilder()
      .sqlQuery("select * from %s", fullTableName)
      .unOrdered()
      .baselineColumns("id", "name", "desc")
      .baselineValues("1", "Bob\nSmith", "A,B,C")
      .go();

    Path path = Paths.get(dirTestWatcher.getDfsTestTmpDir().getAbsolutePath(), tableName, "0_0_0.csv");
    List<String> lines = Files.readAllLines(path);
    assertEquals(Arrays.asList("id,name,desc", "1,\"Bob", "Smith\",\"A,B,C\""), lines);
  }

  @Test
  public void testForceQuotes() throws Exception {
    client.alterSession(ExecConstants.OUTPUT_FORMAT_OPTION, "csv");
    client.alterSession(ExecConstants.TEXT_WRITER_FORCE_QUOTES, true);

    String tableName = "csv_force_quotes_table";
    String fullTableName = String.format("dfs.tmp.`%s`", tableName);
    tablesToDrop.add(fullTableName);

    queryBuilder().sql("create table %s as " +
      "select 1 as id, 'Bob' as name, 'A,B,C' as desc from (values(1))", fullTableName).run();

    testBuilder()
      .sqlQuery("select * from %s", fullTableName)
      .unOrdered()
      .baselineColumns("id", "name", "desc")
      .baselineValues("1", "Bob", "A,B,C")
      .go();

    Path path = Paths.get(dirTestWatcher.getDfsTestTmpDir().getAbsolutePath(), tableName, "0_0_0.csv");
    List<String> lines = Files.readAllLines(path);
    assertEquals(Arrays.asList("\"id\",\"name\",\"desc\"", "\"1\",\"Bob\",\"A,B,C\""), lines);
  }

  @Test
  public void testTsv() throws Exception {
    client.alterSession(ExecConstants.OUTPUT_FORMAT_OPTION, "tsv");

    String tableName = "tsv_table";
    String fullTableName = String.format("dfs.tmp.`%s`", tableName);
    tablesToDrop.add(fullTableName);

    queryBuilder().sql("create table %s as " +
      "select 1 as id, 'Bob\tSmith' as name, 'A\"B\"C' as desc from (values(1))", fullTableName).run();

    testBuilder()
      .sqlQuery("select * from %s", fullTableName)
      .unOrdered()
      .baselineColumns("id", "name", "desc")
      .baselineValues("1", "Bob\tSmith", "A\"B\"C")
      .go();

    Path path = Paths.get(dirTestWatcher.getDfsTestTmpDir().getAbsolutePath(), tableName, "0_0_0.tsv");
    List<String> lines = Files.readAllLines(path);
    assertEquals(Arrays.asList("id\tname\tdesc", "1\t\"Bob\tSmith\"\tA\"B\"C"), lines);
  }

  @Test
  public void testCustomFormat() throws Exception {
    client.alterSession(ExecConstants.OUTPUT_FORMAT_OPTION, "custom");

    String tableName = "custom_format_table";
    String fullTableName = String.format("dfs.tmp.`%s`", tableName);
    tablesToDrop.add(fullTableName);

    queryBuilder().sql("create table %s as " +
      "select 1 as `id_`, 'Bob$Smith' as name, 'A^B!C' as desc from (values(1))", fullTableName).run();

    testBuilder()
      .sqlQuery("select * from %s", fullTableName)
      .unOrdered()
      .baselineColumns("id_", "name", "desc")
      .baselineValues("1", "Bob$Smith", "A^B!C")
      .go();

    Path path = Paths.get(dirTestWatcher.getDfsTestTmpDir().getAbsolutePath(), tableName, "0_0_0.custom");
    List<String> lines = Files.readAllLines(path);
    assertEquals(Collections.singletonList("$id_$_name_desc!1_Bob$Smith_$A^B!C$!"), lines);
  }

  @Test
  public void testLineDelimiterLengthLimit() throws Exception {
    TextFormatConfig incorrect = new TextFormatConfig(
        null,
        "end", // line delimiter
        null,  // field delimiter
        null,  // quote
        null,  // escape
        null,  // comment
        false, // skip first line
        false  // extract header
        );
    cluster.defineFormat("dfs", "incorrect", incorrect);

    client.alterSession(ExecConstants.OUTPUT_FORMAT_OPTION, "incorrect");

    String tableName = "incorrect_line_delimiter_table";
    String fullTableName = String.format("dfs.tmp.`%s`", tableName);
    tablesToDrop.add(fullTableName);

    // univocity-parsers allow only 1 - 2 characters line separators
    thrown.expect(UserException.class);
    thrown.expectMessage("Invalid line separator");

    queryBuilder().sql("create table %s as select 1 as id from (values(1))", fullTableName).run();
  }
}
