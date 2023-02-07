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
package org.apache.drill.exec.store.log;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.test.BaseDirTestWatcher;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.QueryRowSetIterator;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.apache.drill.test.rowSet.RowSetUtilities.strArray;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category(RowSetTests.class)
public class TestLogReader extends ClusterTest {

  public static final String DATE_ONLY_PATTERN = "(\\d\\d\\d\\d)-(\\d\\d)-(\\d\\d) .*";

  @ClassRule
  public static final BaseDirTestWatcher dirTestWatcher = new BaseDirTestWatcher();

  protected static File schemaAndConfigDir;
  protected static File schemaOnlyDir;

  private static File tableFuncDir;

  @BeforeClass
  public static void setup() throws Exception {
    ClusterTest.startCluster(ClusterFixture.builder(dirTestWatcher));

    // Define a regex format config for testing.
    defineRegexPlugin();
  }

  private static void defineRegexPlugin() throws ExecutionSetupException {

    // Create an instance of the regex config.
    // Note: we can't use the ".log" extension; the Drill .gitignore
    // file ignores such files, so they'll never get committed. Instead,
    // make up a fake suffix.
    Map<String, FormatPluginConfig> formats = new HashMap<>();

    formats.put("sample", dateOnlyConfig());
    formats.put("drill-log", drillLogConfig());
    formats.put("date-log", dateTimeConfig());
    formats.put("mysql-log", mySqlConfig());
    formats.put("ssdlog", firewallConfig());

    // Define a temporary format plugin for the "cp" storage plugin.
    cluster.defineFormats("cp", formats);

    // Create a test directory we can write to.
    schemaAndConfigDir = cluster.makeDataDir("sAndC", "logu", untypedDateOnlyConfig());

    // Empty configuration: regex and columns defined in the
    // provided schema
    LogFormatConfig emptyConfig = new LogFormatConfig(
        null, "loge", null, null);
    schemaOnlyDir = cluster.makeDataDir("SOnly", "loge", emptyConfig);
    tableFuncDir = cluster.makeDataDir("tf", "logf", emptyConfig);
  }

  private static LogFormatConfig dateOnlyConfig() {
    List<LogFormatField> schema = Lists.newArrayList(
        new LogFormatField("year", "INT"),
        new LogFormatField("month", "INT"),
        new LogFormatField("day", "INT"));
    return new LogFormatConfig(
        DATE_ONLY_PATTERN, "log1", null, schema);
  }

  // Config similar to the above, but with no type info. Types
  // will be provided via the provided schema mechanism. Column names
  // are required so that the format and provided schemas match up.
  private static LogFormatConfig untypedDateOnlyConfig() {
    List<LogFormatField> schema = Lists.newArrayList(
        new LogFormatField("year"),
        new LogFormatField("month"),
        new LogFormatField("day"));
    return new LogFormatConfig(
        DATE_ONLY_PATTERN, "logu", null, schema);
  }

  // Full Drill log parser definition.
  private static LogFormatConfig drillLogConfig() {
    String regex = "(\\d\\d\\d\\d)-(\\d\\d)-(\\d\\d) " +
        "(\\d\\d):(\\d\\d):(\\d\\d),\\d+ " +
        "\\[([^]]*)] (\\w+)\\s+(\\S+) - (.*)";
    List<LogFormatField> schema = Lists.newArrayList(
        new LogFormatField("year", "INT"),
        new LogFormatField("month", "INT"),
        new LogFormatField("day", "INT"),
        new LogFormatField("hour", "INT"),
        new LogFormatField("minute", "INT"),
        new LogFormatField("second", "INT"),
        new LogFormatField("thread"),
        new LogFormatField("level"),
        new LogFormatField("module"),
        new LogFormatField("message"));
    return new LogFormatConfig(
        regex, "log1", null, schema);
  }

  //Set up additional configs to check the time/date formats
  private static LogFormatConfig dateTimeConfig() {
    String regex = "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}),(\\d+)\\s\\[(\\w+)\\]\\s([A-Z]+)\\s(.+)";
    List<LogFormatField> schema = Lists.newArrayList(
        new LogFormatField("entry_date", "TIMESTAMP", "yyyy-MM-dd HH:mm:ss"),
        new LogFormatField("pid", "INT"),
        new LogFormatField("location"),
        new LogFormatField("message_type"),
        new LogFormatField("message"));
    return new LogFormatConfig(
        regex, "log2", 3, schema);
  }

  private static LogFormatConfig mySqlConfig() {
    String regex = "(\\d{6})\\s(\\d{2}:\\d{2}:\\d{2})\\s+(\\d+)\\s(\\w+)\\s+(.+)";
    return new LogFormatConfig(
        regex, "sqllog", null, null);
  }

  // Firewall log file that requires date parsing
  private static LogFormatConfig firewallConfig() {
    String regex =
        "(\\w{3}\\s\\d{1,2}\\s\\d{4}\\s\\d{2}:\\d{2}:\\d{2})\\s+(\\w+)" +
        "\\[(\\d+)\\]:\\s(.*?(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}).*?)";
    List<LogFormatField> schema = Lists.newArrayList(
        new LogFormatField("eventDate", "TIMESTAMP", "MMM dd yyyy HH:mm:ss"),
        new LogFormatField("process_name"),
        new LogFormatField("pid", "INT"),
        new LogFormatField("message"),
        new LogFormatField("src_ip"));
    return new LogFormatConfig(
        regex, "ssdlog", null, schema);
  }

  @Test
  public void testWildcard() throws RpcException {
    String sql = "SELECT * FROM cp.`regex/simple.log1`";
    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("year", MinorType.INT)
        .addNullable("month", MinorType.INT)
        .addNullable("day", MinorType.INT)
        .buildSchema();

    RowSet expected = client.rowSetBuilder(expectedSchema)
        .addRow(2017, 12, 17)
        .addRow(2017, 12, 18)
        .addRow(2017, 12, 19)
        .build();

    RowSetUtilities.verify(expected, results);
  }

  /**
   * Tests for no crashes; does not validate results, unfortunately.
   */
  @Test
  public void testWildcardLargeFile() throws RpcException {
    String sql = "SELECT * FROM cp.`regex/large.log1`";
    QueryRowSetIterator iter = client.queryBuilder().sql(sql).rowSetIterator();

    for (RowSet rowSet : iter) {
      rowSet.clear();
    }
  }

  @Test
  public void testExplicitProject() throws RpcException {
    String sql = "SELECT `day`, `month` FROM cp.`regex/simple.log1`";
    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("day", MinorType.INT)
        .addNullable("month", MinorType.INT)
        .buildSchema();

    RowSet expected = client.rowSetBuilder(expectedSchema)
        .addRow(17, 12)
        .addRow(18, 12)
        .addRow(19, 12)
        .build();

    RowSetUtilities.verify(expected, results);
  }

  @Test
  public void testMissingColumns() throws RpcException {
    String sql = "SELECT `day`, `missing`, `month` FROM cp.`regex/simple.log1`";
    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("day", MinorType.INT)
        .addNullable("missing", MinorType.VARCHAR)
        .addNullable("month", MinorType.INT)
        .buildSchema();

    RowSet expected = client.rowSetBuilder(expectedSchema)
        .addRow(17, null, 12)
        .addRow(18, null, 12)
        .addRow(19, null, 12)
        .build();

    RowSetUtilities.verify(expected, results);
  }

  @Test
  public void testRaw() throws RpcException {
    String sql = "SELECT `_raw` FROM cp.`regex/simple.log1`";
    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("_raw", MinorType.VARCHAR)
        .buildSchema();

    RowSet expected = client.rowSetBuilder(expectedSchema)
        .addRow("2017-12-17 10:52:41,820 [main] INFO  o.a.d.e.e.f.FunctionImplementationRegistry - Function registry loaded.  459 functions loaded in 1396 ms.")
        .addRow("2017-12-18 10:52:37,652 [main] INFO  o.a.drill.common.config.DrillConfig - Configuration and plugin file(s) identified in 115ms.")
        .addRow("2017-12-19 11:12:27,278 [main] ERROR o.apache.drill.exec.server.Drillbit - Failure during initial startup of Drillbit.")
        .build();
    RowSetUtilities.verify(expected, results);
  }

  @Test
  public void testDate() throws RpcException {
    String sql = "SELECT TYPEOF(`entry_date`) AS entry_date FROM cp.`regex/simple.log2` LIMIT 1";
    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
        .add("entry_date", MinorType.VARCHAR)
        .buildSchema();

    RowSet expected = client.rowSetBuilder(expectedSchema)
        .addRow("TIMESTAMP")
        .build();

    RowSetUtilities.verify(expected, results);
  }

  @Test
  public void testCount() throws RpcException {
    String sql = "SELECT COUNT(*) FROM cp.`regex/simple.log1`";
    long result = client.queryBuilder().sql(sql).singletonLong();
    assertEquals(3, result);
  }

  @Test
  public void testFull() throws RpcException {
    String sql = "SELECT * FROM cp.`regex/simple.log1`";
    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("year", MinorType.INT)
        .addNullable("month", MinorType.INT)
        .addNullable("day", MinorType.INT)
        .buildSchema();

    RowSet expected = client.rowSetBuilder(expectedSchema)
        .addRow(2017, 12, 17)
        .addRow(2017, 12, 18)
        .addRow(2017, 12, 19)
        .build();

    RowSetUtilities.verify(expected, results);
  }

  /**
   * Test log queries without a defined schema using select *
   */
  @Test
  public void testStarQueryNoSchema() throws RpcException {
    String sql = "SELECT * FROM cp.`regex/mysql.sqllog`";
    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("columns", MinorType.VARCHAR)
        .buildSchema();

    RowSet expected = client.rowSetBuilder(expectedSchema)
        .addSingleCol(strArray("070823", "21:00:32", "1", "Connect", "root@localhost on test1"))
        .addSingleCol(strArray("070823", "21:00:48", "1", "Query", "show tables"))
        .addSingleCol(strArray("070823", "21:00:56", "1", "Query", "select * from category" ))
        .addSingleCol(strArray("070917", "16:29:01", "21", "Query","select * from location" ))
        .addSingleCol(strArray("070917", "16:29:12", "21", "Query","select * from location where id = 1 LIMIT 1" ))
        .build();

    RowSetUtilities.verify(expected, results);
  }

  @Test
  public void testSomeFieldsQueryNoSchema() throws RpcException {
    String sql = "SELECT columns[0], columns[4] FROM cp.`regex/mysql.sqllog`";
    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("EXPR$0", MinorType.VARCHAR)
        .addNullable("EXPR$1", MinorType.VARCHAR)
        .buildSchema();

    RowSet expected = client.rowSetBuilder(expectedSchema)
        .addRow("070823", "root@localhost on test1")
        .addRow("070823",  "show tables")
        .addRow("070823",  "select * from category" )
        .addRow("070917",  "select * from location" )
        .addRow("070917", "select * from location where id = 1 LIMIT 1" )
        .build();

    RowSetUtilities.verify(expected, results);
  }

  @Test
  public void testRawNoSchema() throws RpcException {
    String sql = "SELECT _raw FROM cp.`regex/mysql.sqllog`";
    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("_raw", MinorType.VARCHAR)
        .buildSchema();

    RowSet expected = client.rowSetBuilder(expectedSchema)
        .addRow("070823 21:00:32       1 Connect     root@localhost on test1")
        .addRow("070823 21:00:48       1 Query       show tables")
        .addRow("070823 21:00:56       1 Query       select * from category" )
        .addRow("070917 16:29:01      21 Query       select * from location" )
        .addRow("070917 16:29:12      21 Query       select * from location where id = 1 LIMIT 1" )
        .build();

    RowSetUtilities.verify(expected, results);
  }

  @Test
  public void testUMNoSchema() throws RpcException {
    String sql = "SELECT _unmatched_rows FROM cp.`regex/mysql.sqllog`";
    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("_unmatched_rows", MinorType.VARCHAR)
        .buildSchema();

    RowSet expected = client.rowSetBuilder(expectedSchema)
        .addRow("dfadkfjaldkjafsdfjlksdjflksjdlkfjsldkfjslkjl")
        .build();

    RowSetUtilities.verify(expected, results);
  }

  @Test
  public void testRawUMNoSchema() throws RpcException {
    String sql = "SELECT _raw, _unmatched_rows FROM cp.`regex/mysql.sqllog`";
    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("_raw", MinorType.VARCHAR)
        .addNullable("_unmatched_rows", MinorType.VARCHAR)
        .buildSchema();

    RowSet expected = client.rowSetBuilder(expectedSchema)
        .addRow("070823 21:00:32       1 Connect     root@localhost on test1", null)
        .addRow("070823 21:00:48       1 Query       show tables", null)
        .addRow("070823 21:00:56       1 Query       select * from category", null )
        .addRow("070917 16:29:01      21 Query       select * from location", null )
        .addRow("070917 16:29:12      21 Query       select * from location where id = 1 LIMIT 1", null )
        .addRow(null, "dfadkfjaldkjafsdfjlksdjflksjdlkfjsldkfjslkjl")
        .build();

    RowSetUtilities.verify(expected, results);
  }

  /**
   * Build a table, temporary for this test, using the table name and resource
   * provided.
   *
   * @param tableName name of the table within the test-temporary dfs.data
   * workspace
   * @param fileName name of the one and only file in the table (allows using
   * plugin-specific extensions)
   * @param resource path to an existing resource file to copy into the
   * table as the given file name
   * @return the SQL path for the table
   * @throws IOException if the file operations fail
   */
  private String buildTable(File dir, String ws, String tableName,
      String fileName, String resource) throws IOException {

    // We need to create a schema file. Create a temporary test
    // table.
    File tableDir = new File(dir, tableName);
    tableDir.mkdirs();

    // Copy the "simple.log1" data file. Use a distinct extension
    // configured above to provide field names but no types.
    File dest = new File(tableDir, fileName);
    URL url = getClass().getResource(resource);
    FileUtils.copyURLToFile(url, dest);
    return "dfs." + ws + "." + tableName;
  }

  @Test
  public void testProvidedSchema() throws Exception {
    String tablePath = buildTable(schemaAndConfigDir, "sAndC", "schema",
        "sample.logu", "/regex/simple.log1");
    try {

      // Define the provided table schema
      client.alterSession(ExecConstants.STORE_TABLE_USE_SCHEMA_FILE, true);
      String schemaSql = "create schema (`year` int not null, `month` int not null, " +
          "`day` int not null) " +
          "for table " + tablePath;
      run(schemaSql);

      // Run a query using the provided schema.
      String sql = "SELECT * FROM %s";
      RowSet results = client.queryBuilder().sql(sql, tablePath).rowSet();

      // Verify that the returned data used the schema.
      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("year", MinorType.INT)
          .add("month", MinorType.INT)
          .add("day", MinorType.INT)
          .buildSchema();

      RowSet expected = client.rowSetBuilder(expectedSchema)
          .addRow(2017, 12, 17)
          .addRow(2017, 12, 18)
          .addRow(2017, 12, 19)
          .build();

      RowSetUtilities.verify(expected, results);
    } finally {
      client.resetSession(ExecConstants.STORE_TABLE_USE_SCHEMA_FILE);
    }
  }

  /**
   * Test the case in which the plugin config contains no information
   * other than the file extensions. The regex is provided by the provided
   * schema, but no columns are defined, so we use the columns[] array.
   */
  @Test
  public void testSchemaOnlyNoCols() throws Exception {
    String tablePath = buildTable(schemaOnlyDir, "sOnly", "noCols", "sample.loge", "/regex/simple.log1");
    try {
      client.alterSession(ExecConstants.STORE_TABLE_USE_SCHEMA_FILE, true);
      String schemaSql = "create schema ()" +
          " for table %s properties ('%s'='%s')";
      run(schemaSql, tablePath, LogFormatPlugin.REGEX_PROP, DATE_ONLY_PATTERN);

      String sql = "SELECT * FROM %s";
      RowSet results = client.queryBuilder().sql(sql, tablePath).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
          .addArray("columns", MinorType.VARCHAR)
          .buildSchema();

      RowSet expected = client.rowSetBuilder(expectedSchema)
          .addSingleCol(strArray("2017", "12", "17"))
          .addSingleCol(strArray("2017", "12", "18"))
          .addSingleCol(strArray("2017", "12", "19"))
          .build();

      RowSetUtilities.verify(expected, results);
    } finally {
      client.resetSession(ExecConstants.STORE_TABLE_USE_SCHEMA_FILE);
    }
  }

  /**
   * Test the case in which the plugin config contains no information
   * other than the file extensions. The provided schema includes both
   * the regex and the set of columns and types.
   */
  @Test
  public void testSchemaOnlyWithCols() throws Exception {
    String tablePath = buildTable(schemaOnlyDir, "sOnly", "withCols", "sample.loge", "/regex/simple.log1");
    try {
      client.alterSession(ExecConstants.STORE_TABLE_USE_SCHEMA_FILE, true);
      String schemaSql = "create schema (`year` int not null, `month` int not null, " +
          "`day` int not null) " +
          " for table " + tablePath +
          " properties ('" + LogFormatPlugin.REGEX_PROP +
          "'='" + DATE_ONLY_PATTERN + "')";
      run(schemaSql);

      String sql = "SELECT * FROM %s";
      RowSet results = client.queryBuilder().sql(sql, tablePath).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("year", MinorType.INT)
          .add("month", MinorType.INT)
          .add("day", MinorType.INT)
          .buildSchema();

      RowSet expected = client.rowSetBuilder(expectedSchema)
          .addRow(2017, 12, 17)
          .addRow(2017, 12, 18)
          .addRow(2017, 12, 19)
          .build();

      RowSetUtilities.verify(expected, results);
    } finally {
      client.resetSession(ExecConstants.STORE_TABLE_USE_SCHEMA_FILE);
    }
  }

  /**
   * Corner case: provided schema has the regex and two of the three
   * columns, the third takes a default name and type.
   */
  @Test
  public void testSchemaOnlyWithMissingCols() throws Exception {
    String tablePath = buildTable(schemaOnlyDir, "sOnly", "missingCols", "sample.loge", "/regex/simple.log1");
    try {
      client.alterSession(ExecConstants.STORE_TABLE_USE_SCHEMA_FILE, true);
      String schemaSql = "create schema (`year` int not null, `month` int not null) " +
          " for table " + tablePath +
          " properties ('" + LogFormatPlugin.REGEX_PROP +
          "'='" + DATE_ONLY_PATTERN + "')";
      run(schemaSql);

      String sql = "SELECT * FROM %s";
      RowSet results = client.queryBuilder().sql(sql, tablePath).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("year", MinorType.INT)
          .add("month", MinorType.INT)
          .addNullable("field_2", MinorType.VARCHAR)
          .buildSchema();

      RowSet expected = client.rowSetBuilder(expectedSchema)
          .addRow(2017, 12, "17")
          .addRow(2017, 12, "18")
          .addRow(2017, 12, "19")
          .build();

      RowSetUtilities.verify(expected, results);
    } finally {
      client.resetSession(ExecConstants.STORE_TABLE_USE_SCHEMA_FILE);
    }
  }

  /**
   * Verify that an error is thrown if no pattern is provided in
   * the plugin config, table function or provided schema.
   */
  @Test
  public void testEmptyPattern() throws Exception {
    String tablePath = buildTable(tableFuncDir, "tf", "emptyRegex",
        "sample.logf", "/regex/simple.log1");
    try {
     String sql = "SELECT * FROM %s";
     client.queryBuilder().sql(sql, tablePath).run();
     fail();
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("Regex property is required"));
    }
  }

  /**
   * Test the ability to use table functions to specify the regex.
   */
  @Test
  public void testTableFunction() throws Exception {
    String tablePath = buildTable(tableFuncDir, "tf", "table1",
        "sample.logf", "/regex/simple.log1");

    // Run a query using a table function.
    String escaped = DATE_ONLY_PATTERN.replace("\\", "\\\\");
    String sql = "SELECT * FROM table(%s(type => '%s', regex => '%s', maxErrors => 10))";
    RowSet results = client.queryBuilder().sql(sql, tablePath,
        LogFormatPlugin.PLUGIN_NAME, escaped).rowSet();

    // Verify that the returned data used the schema.
    TupleMetadata expectedSchema = new SchemaBuilder()
        .addArray("columns", MinorType.VARCHAR)
        .buildSchema();

    RowSet expected = client.rowSetBuilder(expectedSchema)
        .addSingleCol(strArray("2017", "12", "17"))
        .addSingleCol(strArray("2017", "12", "18"))
        .addSingleCol(strArray("2017", "12", "19"))
        .build();

    RowSetUtilities.verify(expected, results);
  }

  /**
   * Test the use of a table function to provide the regex. Verify
   * that the plugin throws an error if no groups are defined.
   */
  @Test
  public void testTableFunctionNoGroups() throws Exception {
    String tablePath = buildTable(tableFuncDir, "tf", "noGroups",
        "sample.logf", "/regex/simple.log1");

    // Use a table function to pass in a regex without a group.

    try {
      String sql = "SELECT * FROM table(%s(type => '%s', regex => '''foo'''))";
      client.queryBuilder().sql(sql, tablePath, LogFormatPlugin.PLUGIN_NAME).run();
      fail();
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("Regex property has no groups"));
    }
  }

  /**
   * Test the use of the schema table function to provide a schema
   * including types. In this form, the regex must be provided by the
   * plugin config or (as in this test), as table properties.
   */
  @Test
  public void testTableFunctionWithSchema() throws Exception {
    String tablePath = buildTable(tableFuncDir, "tf", "table2",
        "sample.logf", "/regex/simple.log1");
    try {
      client.alterSession(ExecConstants.STORE_TABLE_USE_SCHEMA_FILE, true);

      // Run a query using a table function.

      String escaped = DATE_ONLY_PATTERN.replace("\\", "\\\\");
      String sql = "SELECT * FROM table(%s(" +
         "schema=>'inline=(`year` int, `month` int, `day` int) properties {`%s`=`%s`}'))";
      RowSet results = client.queryBuilder().sql(sql, tablePath,
         LogFormatPlugin.REGEX_PROP, escaped).rowSet();

      // Verify that the returned data used the schema.
      TupleMetadata expectedSchema = new SchemaBuilder()
          .addNullable("year", MinorType.INT)
          .addNullable("month", MinorType.INT)
          .addNullable("day", MinorType.INT)
          .buildSchema();

      RowSet expected = client.rowSetBuilder(expectedSchema)
          .addRow(2017, 12, 17)
          .addRow(2017, 12, 18)
          .addRow(2017, 12, 19)
          .build();

      RowSetUtilities.verify(expected, results);
    } finally {
      client.resetSession(ExecConstants.STORE_TABLE_USE_SCHEMA_FILE);
    }
  }

  /**
   * Test that a schema can be provided in a table function that also includes
   * plugin config. This case fails for the log format plugin because, unfortunately,
   * the log format plugin has a config field called "schema" which is not a string
   * and is found by the code before trying to treat "schema" as a schema.
   * So, this test is disabled.
   */
  @Test
  @Ignore("Use of schema conflicts with plugin field")
  public void testTableFunctionWithConfigAndSchema() throws Exception {
    String tablePath = buildTable(tableFuncDir, "tf", "table2",
        "sample.logf", "/regex/simple.log1");
    try {
      client.alterSession(ExecConstants.STORE_TABLE_USE_SCHEMA_FILE, true);

      // Run a query using a table function.
      String escaped = DATE_ONLY_PATTERN.replace("\\", "\\\\");
      String sql = "SELECT * FROM table(%s(type => '%s', regex => '%s', " +
         "schema=>'inline=(`year` int, `month` int, `day` int)'))";
      RowSet results = client.queryBuilder().sql(sql, tablePath,
          LogFormatPlugin.PLUGIN_NAME, escaped).rowSet();

      // Verify that the returned data used the schema.
      TupleMetadata expectedSchema = new SchemaBuilder()
          .addNullable("year", MinorType.INT)
          .addNullable("month", MinorType.INT)
          .addNullable("day", MinorType.INT)
          .buildSchema();

      RowSet expected = client.rowSetBuilder(expectedSchema)
          .addRow(2017, 12, 17)
          .addRow(2017, 12, 18)
          .addRow(2017, 12, 19)
          .build();

      RowSetUtilities.verify(expected, results);
    } finally {
      client.resetSession(ExecConstants.STORE_TABLE_USE_SCHEMA_FILE);
    }
  }

  /**
   * The config classes for this plugin have been trick to get right.
   * The following ensures that the plugin config works correctly
   * to serialize/deserialize values.
   */
  @Test
  public void testPluginSerialization() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    assertTrue(mapper.canSerialize(LogFormatPlugin.class));

    LogFormatConfig sampleConfig = dateOnlyConfig();

    String json = mapper.writeValueAsString(sampleConfig);
    LogFormatConfig result = mapper.readValue(json, LogFormatConfig.class);

    assertEquals(sampleConfig.getRegex(), result.getRegex());
    assertEquals(sampleConfig.getMaxErrors(), result.getMaxErrors());
    assertEquals(sampleConfig.getSchema(), result.getSchema());
  }

  @Test
  public void testFirewallSchema() throws RpcException {
    String sql = "SELECT * FROM cp.`regex/firewall.ssdlog` limit 0";
    RowSet result = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("eventDate", MinorType.TIMESTAMP)
        .addNullable("process_name", MinorType.VARCHAR)
        .addNullable("pid", MinorType.INT)
        .addNullable("message", MinorType.VARCHAR)
        .addNullable("src_ip", MinorType.VARCHAR)
        .buildSchema();

    RowSet.SingleRowSet expected = client.rowSetBuilder(expectedSchema).build();

    RowSetUtilities.verify(expected, result);
    result.clear();
  }
}
