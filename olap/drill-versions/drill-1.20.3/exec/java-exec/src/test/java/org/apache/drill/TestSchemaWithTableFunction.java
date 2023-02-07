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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.util.StoragePluginTestUtils;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestSchemaWithTableFunction extends ClusterTest {

  private static final String DATA_PATH = "store/text/data";
  private static final String TABLE_PLACEHOLDER = "[TABLE]";
  private static final String TABLE_NAME = String.format("%s.`%s/%s`", StoragePluginTestUtils.DFS_PLUGIN_NAME, DATA_PATH, TABLE_PLACEHOLDER);

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @BeforeClass
  public static void setup() throws Exception {
    dirTestWatcher.copyResourceToRoot(Paths.get(DATA_PATH));
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    startCluster(builder);
  }

  @Test
  public void testSchemaInline() throws Exception {
    String table = TABLE_NAME.replace(TABLE_PLACEHOLDER, "cars.csvh");
    String query = "select Year from table(%s(schema=>'inline=(`Year` int)')) where Make = 'Ford'";

    testBuilder()
      .sqlQuery(query, table)
      .unOrdered()
      .baselineColumns("Year")
      .baselineValues(1997)
      .go();

    String plan = queryBuilder().sql(query, table).explainText();
    assertTrue(plan.contains("schema=[TupleSchema [PrimitiveColumnMetadata [`Year` (INT:OPTIONAL)]]]"));
  }

  @Test
  public void testSchemaInlineWithProperties() throws Exception {
    String table = TABLE_NAME.replace(TABLE_PLACEHOLDER, "cars.csvh");
    String query = "select * from table(%s(schema=>'inline=(`Year` int, `Make` varchar) " +
      "properties {`drill.strict` = `true`}')) where Make = 'Ford'";

    testBuilder()
      .sqlQuery(query, table)
      .unOrdered()
      .baselineColumns("Year", "Make")
      .baselineValues(1997, "Ford")
      .go();

    String plan = queryBuilder().sql(query, table).explainText();
    assertFalse(plan.contains("schema=null"));
  }

  @Test
  public void testSchemaInlineWithoutColumns() throws Exception {
    String table = TABLE_NAME.replace(TABLE_PLACEHOLDER, "cars.csvh");
    String query = "select * from table(%s(schema=>'inline=() " +
      "properties {`drill.strict` = `true`}')) where Make = 'Ford'";

    String plan = queryBuilder().sql(query, table).explainText();
    assertFalse(plan.contains("schema=null"));
  }

  @Test
  public void testSchemaInlineWithTableProperties() throws Exception {
    String table = TABLE_NAME.replace(TABLE_PLACEHOLDER, "cars.csvh-test");
    String query = "select Year from table(%s(type=>'text', fieldDelimiter=>',', extractHeader=>true, " +
      "schema=>'inline=(`Year` int)')) where Make = 'Ford'";

    testBuilder()
      .sqlQuery(query, table)
      .unOrdered()
      .baselineColumns("Year")
      .baselineValues(1997)
      .go();

    String plan = queryBuilder().sql(query, table).explainText();
    assertFalse(plan.contains("schema=null"));
  }

  @Test
  public void testSchemaInlineWithPropertiesInDifferentOrder() throws Exception {
    String table = TABLE_NAME.replace(TABLE_PLACEHOLDER, "cars.csvh-test");

    String sqlQuery = "select Year from table(%s(schema=>'inline=(`Year` int)', fieldDelimiter=>',', extractHeader=>true, " +
      "type=>'text'))";

    String sqlBaselineQuery = "select Year from table(%s(type=>'text', fieldDelimiter=>',', schema=>'inline=(`Year` int)', " +
      "extractHeader=>true))";

    testBuilder()
      .sqlQuery(sqlQuery, table)
      .unOrdered()
      .sqlBaselineQuery(sqlBaselineQuery, table)
      .go();

    String plan = queryBuilder().sql(sqlQuery, table).explainText();
    assertFalse(plan.contains("schema=null"));
  }

  @Test
  public void testSchemaInlineForFolder() throws Exception {
    run("use dfs.tmp");

    String table = "text_table";
    String sourceTable = TABLE_NAME.replace(TABLE_PLACEHOLDER, "regions.csv");
    try {
      client.alterSession(ExecConstants.OUTPUT_FORMAT_OPTION, "csv");
      run("create table %s as select columns[0] as id, columns[1] as name from %s", table, sourceTable);

      // Inherits other properties from CSV
      String query = "select * from table(%s(type=>'text', extractHeader=>true " +
        ",schema=>'inline=(`id` int)')) where id = 1";

      testBuilder()
        .sqlQuery(query, table)
        .unOrdered()
        .baselineColumns("id", "name")
        .baselineValues(1, "AMERICA")
        .go();

      String plan = queryBuilder().sql(query, table).explainText();
      assertTrue(plan.contains("schema=[TupleSchema [PrimitiveColumnMetadata [`id` (INT:OPTIONAL)]]]"));
    } finally {
      client.resetSession(ExecConstants.OUTPUT_FORMAT_OPTION);
      run("drop table if exists %s", table);
    }
  }

  @Test
  public void testInvalidSchemaParameter() throws Exception {
    String table = TABLE_NAME.replace(TABLE_PLACEHOLDER, "cars.csvh");

    thrown.expect(UserRemoteException.class);
    thrown.expectMessage("VALIDATION ERROR");

    run("select Year from table(%s(schema=>'(`Year` int)'))", table);
  }

  @Test
  public void testInvalidSchemaProviderType() throws Exception {
    String table = TABLE_NAME.replace(TABLE_PLACEHOLDER, "cars.csvh");

    thrown.expect(UserRemoteException.class);
    thrown.expectMessage("VALIDATION ERROR");

    run("select Year from table(%s(schema=>'line=(`Year` int)'))", table);
  }

  @Test
  public void testSchemaInlineInvalidSchemaSyntax() throws Exception {
    String table = TABLE_NAME.replace(TABLE_PLACEHOLDER, "cars.csvh");

    thrown.expect(UserRemoteException.class);
    thrown.expectMessage("VALIDATION ERROR");

    run("select Year from table(%s(schema=>'inline=(int)')) where Make = 'Ford'", table);
  }

  @Test
  public void testSchemaPath() throws Exception {
    File tmpDir = dirTestWatcher.getTmpDir();
    File schemaFile = new File(tmpDir, "schema_for_path.schema");
    assertFalse(schemaFile.exists());

    try {
      String path = schemaFile.getPath();
      testBuilder()
        .sqlQuery("create schema (`Year` int) path '%s'", path)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, String.format("Created schema for [%s]", path))
        .go();

      String table = TABLE_NAME.replace(TABLE_PLACEHOLDER, "cars.csvh");

      List<String> queries = Arrays.asList(
        "select Year from table(%s(schema=>'path=%s')) where Make = 'Ford'",
        "select Year from table(%s(schema=>'path=`%s`')) where Make = 'Ford'");

      for (String query : queries) {
        testBuilder()
          .sqlQuery(query, table, path)
          .unOrdered()
          .baselineColumns("Year")
          .baselineValues(1997)
          .go();

        String plan = queryBuilder().sql(query, table, path).explainText();
        assertFalse(plan.contains("schema=null"));
      }

    } finally {
      if (schemaFile.exists()) {
        assertTrue(schemaFile.delete());
      }
    }
  }

  @Test
  public void testSchemaPathInvalid() throws Exception {
    String table = TABLE_NAME.replace(TABLE_PLACEHOLDER, "cars.csvh");

    thrown.expect(UserRemoteException.class);
    thrown.expectMessage("VALIDATION ERROR");

    run("select Year from table(%s(schema=>'path=(int)')) where Make = 'Ford'", table);
  }
}
