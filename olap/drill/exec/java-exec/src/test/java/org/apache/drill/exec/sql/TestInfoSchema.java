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
package org.apache.drill.exec.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.drill.categories.SqlTest;
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.physical.rowSet.DirectRowSet;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.exec.physical.rowSet.RowSetReader;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.exec.store.dfs.FileSystemConfig;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.QueryBuilder;
import org.apache.drill.test.TestBuilder;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.CATS_COL_CATALOG_CONNECT;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.CATS_COL_CATALOG_DESCRIPTION;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.CATS_COL_CATALOG_NAME;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Contains tests for
 * -- InformationSchema
 * -- Queries on InformationSchema such as SHOW TABLES, SHOW SCHEMAS or DESCRIBE table
 * -- USE schema
 */
@Category(SqlTest.class)
public class TestInfoSchema extends ClusterTest {
  private static final String TEST_SUB_DIR = "testSubDir";
  private static final ObjectMapper mapper = new ObjectMapper().enable(INDENT_OUTPUT);

  @BeforeClass
  public static void setupFiles() throws Exception {
    ClusterTest.startCluster(new ClusterFixtureBuilder(dirTestWatcher));
    dirTestWatcher.copyFileToRoot(Paths.get("sample-data"));
    dirTestWatcher.makeRootSubDir(Paths.get(TEST_SUB_DIR));

  }

  @Test
  public void selectFromAllTables() throws Exception {
    runAndLog("select * from INFORMATION_SCHEMA.SCHEMATA");
    runAndLog("select * from INFORMATION_SCHEMA.CATALOGS");
    runAndLog("select * from INFORMATION_SCHEMA.VIEWS");
    runAndLog("select * from INFORMATION_SCHEMA.`TABLES`");
    runAndLog("select * from INFORMATION_SCHEMA.COLUMNS");
    runAndLog("select * from INFORMATION_SCHEMA.`FILES`");
    runAndLog("select * from INFORMATION_SCHEMA.`PARTITIONS`");
  }

  @Test
  public void catalogs() throws Exception {
    testBuilder()
        .sqlQuery("SELECT * FROM INFORMATION_SCHEMA.CATALOGS")
        .unOrdered()
        .baselineColumns(CATS_COL_CATALOG_NAME, CATS_COL_CATALOG_DESCRIPTION, CATS_COL_CATALOG_CONNECT)
        .baselineValues("DRILL", "The internal metadata used by Drill", "")
        .go();
  }

  @Test
  public void showTablesFromDb() throws Exception {
    List<String[]> expected = Arrays.asList(
        new String[]{"information_schema", "VIEWS"},
        new String[]{"information_schema", "COLUMNS"},
        new String[]{"information_schema", "TABLES"},
        new String[]{"information_schema", "CATALOGS"},
        new String[]{"information_schema", "SCHEMATA"},
        new String[]{"information_schema", "FILES"},
        new String[]{"information_schema", "PARTITIONS"});

    TestBuilder t1 = testBuilder()
        .sqlQuery("SHOW TABLES FROM INFORMATION_SCHEMA")
        .unOrdered()
        .baselineColumns("TABLE_SCHEMA", "TABLE_NAME");
    for (String[] expectedRow : expected) {
      t1.baselineValues(expectedRow);
    }
    t1.go();

    TestBuilder t2 = testBuilder()
        .sqlQuery("SHOW TABLES IN INFORMATION_SCHEMA")
        .unOrdered()
        .baselineColumns("TABLE_SCHEMA", "TABLE_NAME");
    for (String[] expectedRow : expected) {
      t2.baselineValues(expectedRow);
    }
    t2.go();
  }

  @Test
  public void showTablesFromDbWhere() throws Exception {
    testBuilder()
        .sqlQuery("SHOW TABLES FROM INFORMATION_SCHEMA WHERE TABLE_NAME='VIEWS'")
        .unOrdered()
        .baselineColumns("TABLE_SCHEMA", "TABLE_NAME")
        .baselineValues("information_schema", "VIEWS")
        .go();
  }

  @Test
  public void showTablesLike() throws Exception {
    testBuilder()
        .sqlQuery("SHOW TABLES LIKE '%CH%'")
        .unOrdered()
        .optionSettingQueriesForTestQuery("USE INFORMATION_SCHEMA")
        .baselineColumns("TABLE_SCHEMA", "TABLE_NAME")
        .baselineValues("information_schema", "SCHEMATA")
        .go();
  }

  @Test
  public void showDatabases() throws Exception {
    QueryBuilder builder = client.queryBuilder().sql("SHOW DATABASES");
    DirectRowSet sets = builder.rowSet();

    try {
      TupleMetadata schema = new SchemaBuilder()
        .addNullable("SCHEMA_NAME", TypeProtos.MinorType.VARCHAR)
        .build();

      RowSet expected = new RowSetBuilder(client.allocator(), schema)
        .addRow("dfs.default")
        .addRow("dfs.root")
        .addRow("dfs.tmp")
        .addRow("cp.default")
        .addRow("sys")
        .addRow("information_schema")
        .addRow("mock")
        .build();

      new RowSetComparison(expected).unorderedVerifyAndClearAll(sets);

      builder = client.queryBuilder().sql("SHOW SCHEMAS");
      sets = builder.rowSet();
      new RowSetComparison(expected).unorderedVerifyAndClearAll(sets);
    } finally {
      sets.clear();
    }
  }

  @Test
  public void showDatabasesWhere() throws Exception {
    testBuilder()
        .sqlQuery("SHOW DATABASES WHERE SCHEMA_NAME='dfs.tmp'")
        .unOrdered()
        .baselineColumns("SCHEMA_NAME")
        .baselineValues("dfs.tmp")
        .go();
  }

  @Test
  public void showDatabasesWhereIn() throws Exception {
      testBuilder()
        .sqlQuery("SHOW DATABASES WHERE SCHEMA_NAME in ('dfs.tmp', 'dfs.root')")
        .unOrdered()
        .baselineColumns("SCHEMA_NAME")
        .baselineValues("dfs.tmp")
        .baselineValues("dfs.root")
        .go();
  }

  @Test
  public void showDatabasesLike() throws Exception {
    testBuilder()
        .sqlQuery("SHOW DATABASES LIKE '%y%'")
        .unOrdered()
        .baselineColumns("SCHEMA_NAME")
        .baselineValues("sys")
        .go();


  }

  @Test // DRILL-8281
  public void likePatternWithEscapeChar() throws Exception {
    StoragePluginRegistry pluginRegistry = cluster.drillbit().getContext().getStorage();
    pluginRegistry.validatedPut(
      "dfs_with_underscore",
      pluginRegistry.getDefinedConfig("dfs")
      );

    try {
      testBuilder()
        .sqlQuery("SHOW DATABASES WHERE schema_name LIKE 'dfs^_with^_underscore.%' escape '^'")
        .unOrdered()
        .baselineColumns("SCHEMA_NAME")
        .baselineValues("dfs_with_underscore.default")
        .baselineValues("dfs_with_underscore.tmp")
        .baselineValues("dfs_with_underscore.root")
        .go();
    } finally {
      pluginRegistry.remove("dfs_with_underscore");
    }
  }

  @Test
  public void describeTable() throws Exception {
    testBuilder()
        .sqlQuery("DESCRIBE CATALOGS")
        .unOrdered()
        .optionSettingQueriesForTestQuery("USE INFORMATION_SCHEMA")
        .baselineColumns("COLUMN_NAME", "DATA_TYPE", "IS_NULLABLE")
        .baselineValues("CATALOG_NAME", "CHARACTER VARYING", "NO")
        .baselineValues("CATALOG_DESCRIPTION", "CHARACTER VARYING", "NO")
        .baselineValues("CATALOG_CONNECT", "CHARACTER VARYING", "NO")
        .go();
  }

  @Test
  public void describeTableWithTableKeyword() throws Exception {
    runAndLog("USE INFORMATION_SCHEMA");
    testBuilder()
        .sqlQuery("DESCRIBE TABLE CATALOGS")
        .unOrdered()
        .sqlBaselineQuery("DESCRIBE CATALOGS")
        .go();
  }

  @Test
  public void describeTableWithSchema() throws Exception {
    testBuilder()
        .sqlQuery("DESCRIBE INFORMATION_SCHEMA.`TABLES`")
        .unOrdered()
        .baselineColumns("COLUMN_NAME", "DATA_TYPE", "IS_NULLABLE")
        .baselineValues("TABLE_CATALOG", "CHARACTER VARYING", "NO")
        .baselineValues("TABLE_SCHEMA", "CHARACTER VARYING", "NO")
        .baselineValues("TABLE_NAME", "CHARACTER VARYING", "NO")
        .baselineValues("TABLE_TYPE", "CHARACTER VARYING", "NO")
        .baselineValues("TABLE_SOURCE", "CHARACTER VARYING", "NO")
        .baselineValues("LOCATION", "CHARACTER VARYING", "NO")
        .baselineValues("NUM_ROWS", "BIGINT", "NO")
        .baselineValues("LAST_MODIFIED_TIME", "TIMESTAMP", "NO")
        .go();
  }

  @Test
  public void describeTableWithSchemaAndTableKeyword() throws Exception {
    testBuilder()
        .sqlQuery("DESCRIBE TABLE INFORMATION_SCHEMA.`TABLES`")
        .unOrdered()
        .sqlBaselineQuery("DESCRIBE INFORMATION_SCHEMA.`TABLES`")
        .go();
  }

  @Test
  public void describeWhenSameTableNameExistsInMultipleSchemas() throws Exception {
    try {
      runAndLog("USE dfs.tmp");
      runAndLog("CREATE OR REPLACE VIEW `TABLES` AS SELECT full_name FROM cp.`employee.json`");

      testBuilder()
          .sqlQuery("DESCRIBE `TABLES`")
          .unOrdered()
          .optionSettingQueriesForTestQuery("USE dfs.tmp")
          .baselineColumns("COLUMN_NAME", "DATA_TYPE", "IS_NULLABLE")
          .baselineValues("full_name", "ANY", "YES")
          .go();

      testBuilder()
          .sqlQuery("DESCRIBE INFORMATION_SCHEMA.`TABLES`")
          .unOrdered()
          .baselineColumns("COLUMN_NAME", "DATA_TYPE", "IS_NULLABLE")
          .baselineValues("TABLE_CATALOG", "CHARACTER VARYING", "NO")
          .baselineValues("TABLE_SCHEMA", "CHARACTER VARYING", "NO")
          .baselineValues("TABLE_NAME", "CHARACTER VARYING", "NO")
          .baselineValues("TABLE_TYPE", "CHARACTER VARYING", "NO")
          .baselineValues("TABLE_SOURCE", "CHARACTER VARYING", "NO")
          .baselineValues("LOCATION", "CHARACTER VARYING", "NO")
          .baselineValues("NUM_ROWS", "BIGINT", "NO")
          .baselineValues("LAST_MODIFIED_TIME", "TIMESTAMP", "NO")
          .go();
    } finally {
      runAndLog("DROP VIEW IF EXISTS dfs.tmp.`TABLES`");
    }
  }

  @Test
  public void describeWhenSameTableNameExistsInMultipleSchemasWithTableKeyword() throws Exception {
    try {
      runAndLog("USE dfs.tmp");
      runAndLog("CREATE OR REPLACE VIEW `TABLES` AS SELECT full_name FROM cp.`employee.json`");

      testBuilder()
          .sqlQuery("DESCRIBE TABLE `TABLES`")
          .unOrdered()
          .sqlBaselineQuery("DESCRIBE `TABLES`")
          .go();

      testBuilder()
          .sqlQuery("DESCRIBE TABLE INFORMATION_SCHEMA.`TABLES`")
          .unOrdered()
          .sqlBaselineQuery("DESCRIBE INFORMATION_SCHEMA.`TABLES`")
          .go();
    } finally {
      runAndLog("DROP VIEW IF EXISTS dfs.tmp.`TABLES`");
    }
  }

  @Test
  public void describeTableWithColumnName() throws Exception {
    testBuilder()
        .sqlQuery("DESCRIBE `TABLES` TABLE_CATALOG")
        .unOrdered()
        .optionSettingQueriesForTestQuery("USE INFORMATION_SCHEMA")
        .baselineColumns("COLUMN_NAME", "DATA_TYPE", "IS_NULLABLE")
        .baselineValues("TABLE_CATALOG", "CHARACTER VARYING", "NO")
        .go();
  }

  @Test
  public void describeTableWithColumnNameAndTableKeyword() throws Exception {
    runAndLog("USE INFORMATION_SCHEMA");
    testBuilder()
        .sqlQuery("DESCRIBE TABLE `TABLES` TABLE_CATALOG")
        .unOrdered()
        .sqlBaselineQuery("DESCRIBE `TABLES` TABLE_CATALOG")
        .go();
  }

  @Test
  public void describeTableWithSchemaAndColumnName() throws Exception {
    testBuilder()
        .sqlQuery("DESCRIBE INFORMATION_SCHEMA.`TABLES` TABLE_CATALOG")
        .unOrdered()
        .baselineColumns("COLUMN_NAME", "DATA_TYPE", "IS_NULLABLE")
        .baselineValues("TABLE_CATALOG", "CHARACTER VARYING", "NO")
        .go();
  }

  @Test
  public void describeTableWithSchemaAndColumnNameAndTableKeyword() throws Exception {
    testBuilder()
        .sqlQuery("DESCRIBE TABLE INFORMATION_SCHEMA.`TABLES` TABLE_CATALOG")
        .unOrdered()
        .sqlBaselineQuery("DESCRIBE INFORMATION_SCHEMA.`TABLES` TABLE_CATALOG")
        .go();
  }

  @Test
  public void describeTableWithColQualifier() throws Exception {
    testBuilder()
        .sqlQuery("DESCRIBE COLUMNS 'TABLE%'")
        .unOrdered()
        .optionSettingQueriesForTestQuery("USE INFORMATION_SCHEMA")
        .baselineColumns("COLUMN_NAME", "DATA_TYPE", "IS_NULLABLE")
        .baselineValues("TABLE_CATALOG", "CHARACTER VARYING", "NO")
        .baselineValues("TABLE_SCHEMA", "CHARACTER VARYING", "NO")
        .baselineValues("TABLE_NAME", "CHARACTER VARYING", "NO")
        .go();
  }

  @Test
  public void describeTableWithSchemaAndColQualifier() throws Exception {
    testBuilder()
        .sqlQuery("DESCRIBE INFORMATION_SCHEMA.SCHEMATA 'SCHEMA%'")
        .unOrdered()
        .baselineColumns("COLUMN_NAME", "DATA_TYPE", "IS_NULLABLE")
        .baselineValues("SCHEMA_NAME", "CHARACTER VARYING", "NO")
        .baselineValues("SCHEMA_OWNER", "CHARACTER VARYING", "NO")
        .go();
  }

  @Test
  public void defaultSchemaDfs() throws Exception {
    testBuilder()
        .sqlQuery("SELECT R_REGIONKEY FROM `sample-data/region.parquet` LIMIT 1")
        .unOrdered()
        .optionSettingQueriesForTestQuery("USE dfs")
        .baselineColumns("R_REGIONKEY")
        .baselineValues(0L)
        .go();
  }

  @Test
  public void defaultSchemaClasspath() throws Exception {
    testBuilder()
        .sqlQuery("SELECT full_name FROM `employee.json` LIMIT 1")
        .unOrdered()
        .optionSettingQueriesForTestQuery("USE cp")
        .baselineColumns("full_name")
        .baselineValues("Sheri Nowmer")
        .go();
  }


  @Test
  public void queryFromNonDefaultSchema() throws Exception {
    testBuilder()
        .sqlQuery("SELECT full_name FROM cp.`employee.json` LIMIT 1")
        .unOrdered()
        .optionSettingQueriesForTestQuery("USE dfs")
        .baselineColumns("full_name")
        .baselineValues("Sheri Nowmer")
        .go();
  }

  @Test
  public void useSchema() throws Exception {
    testBuilder()
        .sqlQuery("USE dfs.`default`")
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, "Default schema changed to [dfs.default]")
        .go();
  }

  @Test
  public void useSubSchemaWithinSchema() throws Exception {
    testBuilder()
        .sqlQuery("USE dfs")
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, "Default schema changed to [dfs]")
        .go();

    testBuilder()
        .sqlQuery("USE tmp")
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, "Default schema changed to [dfs.tmp]")
        .go();

    testBuilder()
        .sqlQuery("USE dfs.`default`")
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, "Default schema changed to [dfs.default]")
        .go();
  }

  @Test
  public void useSchemaNegative() throws Exception {
    try {
      queryBuilder().sql("USE invalid.schema").run();
    } catch (UserRemoteException e) {
      assertThat(
        e.getMessage(),
        containsString("Schema [invalid.schema] is not valid with respect to either root schema or current default schema.")
      );
    }
  }

  // Tests using backticks around the complete schema path
  // select * from `dfs.tmp`.`/tmp/nation.parquet`;
  @Test
  public void completeSchemaRef1() throws Exception {
    runAndLog("SELECT * FROM `cp.default`.`employee.json` limit 2");
  }

  @Test
  public void describeSchemaSyntax() throws Exception {
    runAndLog("describe schema dfs");
    runAndLog("describe schema dfs.`default`");
    runAndLog("describe database dfs.`default`");
  }

  @Test
  public void describePartialSchema() throws Exception {
    runAndLog("use dfs");
    runAndLog("describe schema tmp");
  }

  @Test
  public void describeSchemaOutput() throws Exception {
    QueryBuilder builder = queryBuilder().sql("describe schema dfs.tmp");
    DirectRowSet rows = builder.rowSet();
    try {
      assertEquals(1, rows.rowCount());

      RowSetReader reader = rows.reader();
      assertTrue(reader.next());
      String schema = (String) reader.column(0).reader().getObject();
      assertEquals("dfs.tmp", schema);

      String properties = (String) reader.column(1).reader().getObject();
      Map<?, ?> configMap = mapper.readValue(properties, Map.class);
      // check some stable properties existence
      assertTrue(configMap.containsKey("connection"));
      assertTrue(configMap.containsKey("formats"));
      assertFalse(configMap.containsKey("workspaces"));

      // check some stable properties values
      assertEquals("file", configMap.get("type"));

      DrillbitContext ctx = cluster.drillbit().getContext();
      FileSystemConfig testConfig = (FileSystemConfig) ctx.getStorage().getPlugin("dfs").getConfig();
      String tmpSchemaLocation = testConfig.getWorkspaces().get("tmp").getLocation();
      assertEquals(tmpSchemaLocation, configMap.get("location"));
    } finally {
      rows.clear();
    }
  }

  @Test
  public void describeSchemaInvalid() throws Exception {
    try {
      queryBuilder().sql("describe schema invalid.schema").run();
    } catch (UserRemoteException e) {
      assertThat(
        e.getMessage(),
        containsString("Invalid schema name [invalid.schema]")
      );
    }
  }

  @Test
  public void testDescribeAlias() throws Exception {
    runAndLog("desc schema dfs.tmp");
    runAndLog("desc information_schema.`catalogs`");
    runAndLog("desc table information_schema.`catalogs`");
  }

  @Test
  public void testSerDe() throws Exception {
    String sql = "select count() from information_schema.`tables` where table_name = 'SCHEMATA'";
    String plan = queryBuilder().sql(sql).explainJson();
    long cnt = queryBuilder().physical(plan).singletonLong();

    assertEquals("Counts should match", 1, cnt);
  }
}
