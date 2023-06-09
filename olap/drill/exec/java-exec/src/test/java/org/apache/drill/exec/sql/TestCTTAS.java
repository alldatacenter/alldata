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

import static org.apache.drill.exec.util.StoragePluginTestUtils.DFS_PLUGIN_NAME;
import static org.apache.drill.exec.util.StoragePluginTestUtils.DFS_TMP_SCHEMA;
import static org.hamcrest.CoreMatchers.containsString;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.drill.categories.SqlTest;
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.exec.store.dfs.FileSystemConfig;
import org.apache.drill.exec.store.dfs.WorkspaceConfig;
import org.apache.drill.common.logical.security.PlainCredentialsProvider;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.test.BaseTestQuery;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

@Category(SqlTest.class)
public class TestCTTAS extends BaseTestQuery {

  private static final String temp2_wk = "tmp2";
  private static final String temp2_schema = String.format("%s.%s", DFS_PLUGIN_NAME, temp2_wk);

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @BeforeClass
  public static void init() throws Exception {
    File tmp2 = dirTestWatcher.makeSubDir(Paths.get("tmp2"));
    StoragePluginRegistry pluginRegistry = getDrillbitContext().getStorage();
    FileSystemConfig pluginConfig = (FileSystemConfig) pluginRegistry.getPlugin(DFS_PLUGIN_NAME).getConfig();

    Map<String, WorkspaceConfig> newWorkspaces = new HashMap<>();
    Optional.ofNullable(pluginConfig.getWorkspaces())
      .ifPresent(newWorkspaces::putAll);
    newWorkspaces.put(temp2_wk, new WorkspaceConfig(tmp2.getAbsolutePath(), true, null, false));

    FileSystemConfig newPluginConfig = new FileSystemConfig(
        pluginConfig.getConnection(),
        pluginConfig.getConfig(),
        newWorkspaces,
        pluginConfig.getFormats(),
        PlainCredentialsProvider.EMPTY_CREDENTIALS_PROVIDER);
    newPluginConfig.setEnabled(pluginConfig.isEnabled());
    pluginRegistry.put(DFS_PLUGIN_NAME, newPluginConfig);
  }

  @Test
  public void testSyntax() throws Exception {
    test("create TEMPORARY table temporary_keyword as select 1 from (values(1))");
    test("create TEMPORARY table %s.temporary_keyword_with_wk as select 1 from (values(1))", DFS_TMP_SCHEMA);
  }

  @Test
  public void testCreateTableWithDifferentStorageFormats() throws Exception {
    List<String> storageFormats = Lists.newArrayList("parquet", "json", "csvh");

    try {
      for (String storageFormat : storageFormats) {
        String temporaryTableName = "temp_" + storageFormat;
        test("alter session set `store.format`='%s'", storageFormat);
        test("create TEMPORARY table %s as select 'A' as c1 from (values(1))", temporaryTableName);

        testBuilder()
            .sqlQuery("select * from %s", temporaryTableName)
            .unOrdered()
            .baselineColumns("c1")
            .baselineValues("A")
            .go();

        testBuilder()
            .sqlQuery("select * from %s", temporaryTableName)
            .unOrdered()
            .sqlBaselineQuery("select * from %s.%s", DFS_TMP_SCHEMA, temporaryTableName)
            .go();
      }
    } finally {
      resetSessionOption("store.format");
    }
  }

  @Test
  public void testTemporaryTablesCaseInsensitivity() throws Exception {
    String temporaryTableName = "tEmP_InSeNSiTiVe";
    List<String> temporaryTableNames = Lists.newArrayList(
        temporaryTableName,
        temporaryTableName.toLowerCase(),
        temporaryTableName.toUpperCase());

    test("create TEMPORARY table %s as select 'A' as c1 from (values(1))", temporaryTableName);
    for (String tableName : temporaryTableNames) {
      testBuilder()
          .sqlQuery("select * from %s", tableName)
          .unOrdered()
          .baselineColumns("c1")
          .baselineValues("A")
          .go();
    }
  }

  @Test
  public void testResolveTemporaryTableWithPartialSchema() throws Exception {
    String temporaryTableName = "temporary_table_with_partial_schema";
    test("use %s", DFS_PLUGIN_NAME);
    test("create temporary table tmp.%s as select 'A' as c1 from (values(1))", temporaryTableName);

    testBuilder()
        .sqlQuery("select * from tmp.%s", temporaryTableName)
        .unOrdered()
        .baselineColumns("c1")
        .baselineValues("A")
        .go();
  }

  @Test
  public void testPartitionByWithTemporaryTables() throws Exception {
    String temporaryTableName = "temporary_table_with_partitions";
    test("create TEMPORARY table %s partition by (c1) as select * from (" +
        "select 'A' as c1 from (values(1)) union all select 'B' as c1 from (values(1))) t", temporaryTableName);
  }

  @Test
  public void testCreationOutsideOfDefaultTemporaryWorkspace() throws Exception {
    String temporaryTableName = "temporary_table_outside_of_default_workspace";

    expectUserRemoteExceptionWithMessage(String.format(
      "VALIDATION ERROR: Temporary tables are not allowed to be created / dropped " +
        "outside of default temporary workspace [%s].", DFS_TMP_SCHEMA));

    test("create TEMPORARY table %s.%s as select 'A' as c1 from (values(1))", temp2_schema, temporaryTableName);
  }

  @Test
  public void testCreateWhenTemporaryTableExistsWithoutSchema() throws Exception {
    String temporaryTableName = "temporary_table_exists_without_schema";
    test("create TEMPORARY table %s as select 'A' as c1 from (values(1))", temporaryTableName);

    expectUserRemoteExceptionWithTableExistsMessage(temporaryTableName, DFS_TMP_SCHEMA);

    test("create TEMPORARY table %s as select 'A' as c1 from (values(1))", temporaryTableName);
  }

  @Test
  public void testCreateWhenTemporaryTableExistsCaseInsensitive() throws Exception {
    String temporaryTableName = "temporary_table_exists_case_insensitive";
    test("create TEMPORARY table %s as select 'A' as c1 from (values(1))", temporaryTableName);

    expectUserRemoteExceptionWithTableExistsMessage(temporaryTableName.toUpperCase(), DFS_TMP_SCHEMA);

    test("create TEMPORARY table %s as select 'A' as c1 from (values(1))", temporaryTableName.toUpperCase());
  }

  @Test
  public void testCreateWhenTemporaryTableExistsWithSchema() throws Exception {
    String temporaryTableName = "temporary_table_exists_with_schema";
    test("create TEMPORARY table %s.%s as select 'A' as c1 from (values(1))", DFS_TMP_SCHEMA, temporaryTableName);

    expectUserRemoteExceptionWithTableExistsMessage(temporaryTableName, DFS_TMP_SCHEMA);

    test("create TEMPORARY table %s.%s as select 'A' as c1 from (values(1))", DFS_TMP_SCHEMA, temporaryTableName);
  }

  @Test
  public void testCreateWhenPersistentTableExists() throws Exception {
    String persistentTableName = "persistent_table_exists";
    test("create table %s.%s as select 'A' as c1 from (values(1))", DFS_TMP_SCHEMA, persistentTableName);

    expectUserRemoteExceptionWithTableExistsMessage(persistentTableName, DFS_TMP_SCHEMA);

    test("create TEMPORARY table %s as select 'A' as c1 from (values(1))", persistentTableName);
  }

  @Test
  public void testCreateWhenViewExists() throws Exception {
    String viewName = "view_exists";
    test("create view %s.%s as select 'A' as c1 from (values(1))", DFS_TMP_SCHEMA, viewName);

    expectUserRemoteExceptionWithTableExistsMessage(viewName, DFS_TMP_SCHEMA);

    test("create TEMPORARY table %s as select 'A' as c1 from (values(1))", viewName);
  }

  @Test
  public void testCreatePersistentTableWhenTemporaryTableExists() throws Exception {
    String temporaryTableName = "temporary_table_exists_before_persistent";
    test("create TEMPORARY table %s as select 'A' as c1 from (values(1))", temporaryTableName);

    expectUserRemoteExceptionWithTableExistsMessage(temporaryTableName, DFS_TMP_SCHEMA);

    test("create table %s.%s as select 'A' as c1 from (values(1))", DFS_TMP_SCHEMA, temporaryTableName);
  }

  @Test
  public void testCreateViewWhenTemporaryTableExists() throws Exception {
    String temporaryTableName = "temporary_table_exists_before_view";
    test("create TEMPORARY table %s as select 'A' as c1 from (values(1))", temporaryTableName);

    expectUserRemoteExceptionWithMessage(String.format(
      "VALIDATION ERROR: A non-view table with given name [%s] already exists in schema [%s]",
      temporaryTableName, DFS_TMP_SCHEMA));

    test("create view %s.%s as select 'A' as c1 from (values(1))", DFS_TMP_SCHEMA, temporaryTableName);
  }

  @Test
  public void testSelectWithJoinOnTemporaryTables() throws Exception {
    String temporaryLeftTableName = "temporary_left_table_for_Select_with_join";
    String temporaryRightTableName = "temporary_right_table_for_Select_with_join";
    test("create TEMPORARY table %s as select 'A' as c1, 'B' as c2 from (values(1))", temporaryLeftTableName);
    test("create TEMPORARY table %s as select 'A' as c1, 'C' as c2 from (values(1))", temporaryRightTableName);

    testBuilder()
        .sqlQuery("select t1.c2 col1, t2.c2 col2 from %s t1 join %s t2 on t1.c1 = t2.c1", temporaryLeftTableName, temporaryRightTableName)
        .unOrdered()
        .baselineColumns("col1", "col2")
        .baselineValues("B", "C")
        .go();
  }

  @Test
  public void testTemporaryAndPersistentTablesPriority() throws Exception {
    String name = "temporary_and_persistent_table";
    test("use %s", temp2_schema);
    test("create TEMPORARY table %s as select 'temporary_table' as c1 from (values(1))", name);
    test("create table %s as select 'persistent_table' as c1 from (values(1))", name);

    testBuilder()
        .sqlQuery("select * from %s", name)
        .unOrdered()
        .baselineColumns("c1")
        .baselineValues("temporary_table")
        .go();

    testBuilder()
        .sqlQuery("select * from %s.%s", temp2_schema, name)
        .unOrdered()
        .baselineColumns("c1")
        .baselineValues("persistent_table")
        .go();

    test("drop table %s", name);

    testBuilder()
        .sqlQuery("select * from %s", name)
        .unOrdered()
        .baselineColumns("c1")
        .baselineValues("persistent_table")
        .go();
  }

  @Test
  public void testTemporaryTableAndViewPriority() throws Exception {
    String name = "temporary_table_and_view";
    test("use %s", temp2_schema);
    test("create TEMPORARY table %s as select 'temporary_table' as c1 from (values(1))", name);
    test("create view %s as select 'view' as c1 from (values(1))", name);

    testBuilder()
        .sqlQuery("select * from %s", name)
        .unOrdered()
        .baselineColumns("c1")
        .baselineValues("temporary_table")
        .go();

    testBuilder()
        .sqlQuery("select * from %s.%s", temp2_schema, name)
        .unOrdered()
        .baselineColumns("c1")
        .baselineValues("view")
        .go();

    test("drop table %s", name);

    testBuilder()
        .sqlQuery("select * from %s", name)
        .unOrdered()
        .baselineColumns("c1")
        .baselineValues("view")
        .go();
  }

  @Test
  public void testTemporaryTablesInViewDefinitions() throws Exception {
    String temporaryTableName = "temporary_table_for_view_definition";
    test("create TEMPORARY table %s as select 'A' as c1 from (values(1))", temporaryTableName);

    expectUserRemoteExceptionWithMessage(String.format(
      "VALIDATION ERROR: Temporary tables usage is disallowed. Used temporary table name: [%s]", temporaryTableName));

    test("create view %s.view_with_temp_table as select * from %s", DFS_TMP_SCHEMA, temporaryTableName);
  }

  @Test
  public void testTemporaryTablesInViewExpansionLogic() throws Exception {
    String tableName = "table_for_expansion_logic_test";
    String viewName = "view_for_expansion_logic_test";
    test("use %s", DFS_TMP_SCHEMA);
    test("create table %s as select 'TABLE' as c1 from (values(1))", tableName);
    test("create view %s as select * from %s", viewName, tableName);

    testBuilder()
        .sqlQuery("select * from %s", viewName)
        .unOrdered()
        .baselineColumns("c1")
        .baselineValues("TABLE")
        .go();

    test("drop table %s", tableName);
    test("create temporary table %s as select 'TEMP' as c1 from (values(1))", tableName);

    expectUserRemoteExceptionWithMessage(String.format(
      "VALIDATION ERROR: Temporary tables usage is disallowed. Used temporary table name: [%s]", tableName));

    test("select * from %s", viewName);
  }

  @Test // DRILL-5952
  public void testCreateTemporaryTableIfNotExistsWhenTableWithSameNameAlreadyExists() throws Exception{
    final String newTblName = "createTemporaryTableIfNotExistsWhenATableWithSameNameAlreadyExists";
    test("CREATE TEMPORARY TABLE %s.%s AS SELECT * from cp.`region.json`", DFS_TMP_SCHEMA, newTblName);

    testBuilder()
      .sqlQuery("CREATE TEMPORARY TABLE IF NOT EXISTS %s AS SELECT * FROM cp.`employee.json`", newTblName)
      .unOrdered()
      .baselineColumns("ok", "summary")
      .baselineValues(false, String.format("A table or view with given name [%s] already exists in schema [%s]", newTblName, DFS_TMP_SCHEMA))
      .go();

    test("DROP TABLE IF EXISTS %s.%s", DFS_TMP_SCHEMA, newTblName);
  }

  @Test // DRILL-5952
  public void testCreateTemporaryTableIfNotExistsWhenViewWithSameNameAlreadyExists() throws Exception{
    final String newTblName = "createTemporaryTableIfNotExistsWhenAViewWithSameNameAlreadyExists";
    test("CREATE VIEW %s.%s AS SELECT * from cp.`region.json`", DFS_TMP_SCHEMA, newTblName);

    testBuilder()
      .sqlQuery("CREATE TEMPORARY TABLE IF NOT EXISTS %s.%s AS SELECT * FROM cp.`employee.json`", DFS_TMP_SCHEMA, newTblName)
      .unOrdered()
      .baselineColumns("ok", "summary")
      .baselineValues(false, String.format("A table or view with given name [%s] already exists in schema [%s]", newTblName, DFS_TMP_SCHEMA))
      .go();

    test("DROP VIEW IF EXISTS %s.%s", DFS_TMP_SCHEMA, newTblName);
  }

  @Test // DRILL-5952
  public void testCreateTemporaryTableIfNotExistsWhenTableWithSameNameDoesNotExist() throws Exception{
    final String newTblName = "createTemporaryTableIfNotExistsWhenATableWithSameNameDoesNotExist";
    test("CREATE TEMPORARY TABLE IF NOT EXISTS %s.%s AS SELECT * FROM cp.`employee.json`", DFS_TMP_SCHEMA, newTblName);
    test("DROP TABLE IF EXISTS %s.%s", DFS_TMP_SCHEMA, newTblName);
  }

  @Test
  public void testManualDropWithoutSchema() throws Exception {
    String temporaryTableName = "temporary_table_to_drop_without_schema";
    test("create TEMPORARY table %s as select 'A' as c1 from (values(1))", temporaryTableName);

    testBuilder()
        .sqlQuery("drop table %s", temporaryTableName)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, String.format("Temporary table [%s] dropped", temporaryTableName))
        .go();
  }

  @Test
  public void testManualDropWithSchema() throws Exception {
    String temporaryTableName = "temporary_table_to_drop_with_schema";
    test("create TEMPORARY table %s.%s as select 'A' as c1 from (values(1))", DFS_TMP_SCHEMA, temporaryTableName);

    testBuilder()
        .sqlQuery("drop table %s.%s", DFS_TMP_SCHEMA, temporaryTableName)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, String.format("Temporary table [%s] dropped", temporaryTableName))
        .go();
  }

  @Test
  public void testDropTemporaryTableAsViewWithoutException() throws Exception {
    String temporaryTableName = "temporary_table_to_drop_like_view_without_exception";
    test("create TEMPORARY table %s as select 'A' as c1 from (values(1))", temporaryTableName);

    testBuilder()
        .sqlQuery("drop view if exists %s.%s", DFS_TMP_SCHEMA, temporaryTableName)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(false, String.format("View [%s] not found in schema [%s].",
            temporaryTableName, DFS_TMP_SCHEMA))
        .go();
  }

  @Test
  public void testDropTemporaryTableAsViewWithException() throws Exception {
    String temporaryTableName = "temporary_table_to_drop_like_view_with_exception";
    test("create TEMPORARY table %s as select 'A' as c1 from (values(1))", temporaryTableName);

    expectUserRemoteExceptionWithMessage(String.format(
      "VALIDATION ERROR: Unknown view [%s] in schema [%s]", temporaryTableName, DFS_TMP_SCHEMA));

    test("drop view %s.%s", DFS_TMP_SCHEMA, temporaryTableName);
  }

  @Test
  public void testJoinTemporaryWithPersistentTable() throws Exception {
    String temporaryTableName = "temp_tab";
    String persistentTableName = "pers_tab";
    String query = String.format("select * from `%s` a join `%s` b on a.c1 = b.c2",
        persistentTableName, temporaryTableName);

    test("use %s", temp2_schema);
    test("create TEMPORARY table %s as select '12312' as c2", temporaryTableName);
    test("create table %s as select '12312' as c1", persistentTableName);

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("c1", "c2")
        .baselineValues("12312", "12312")
        .go();
  }

  @Test
  public void testTemporaryTableWithAndWithoutLeadingSlashAreTheSame() throws Exception {
    String tablename = "table_with_and_without_slash_create";

    try {
      test("CREATE TEMPORARY TABLE %s AS SELECT * FROM cp.`region.json`", tablename);

      expectUserRemoteExceptionWithMessage(
          String.format("VALIDATION ERROR: A table or view with given name [%s] already exists in schema [%s]",
          tablename, DFS_TMP_SCHEMA));

      test(String.format("CREATE TEMPORARY TABLE `%s` AS SELECT * FROM cp.`employee.json`", "/" + tablename));
    } finally {
      test("DROP TABLE IF EXISTS %s", tablename);
    }
  }

  @Test
  public void testSelectFromTemporaryTableWithAndWithoutLeadingSlash() throws Exception {
    String tableName = "select_from_table_with_and_without_slash";

    try {
      test("CREATE TEMPORARY TABLE %s AS SELECT * FROM cp.`region.json`", tableName);

      String query = "SELECT region_id FROM `%s` LIMIT 1";

      testBuilder()
          .sqlQuery(query, tableName)
          .unOrdered()
          .baselineColumns("region_id")
          .baselineValues(0L)
          .go();

      testBuilder()
          .sqlQuery(query, "/" + tableName)
          .unOrdered()
          .baselineColumns("region_id")
          .baselineValues(0L)
          .go();
    } finally {
      test("DROP TABLE IF EXISTS %s", tableName);
    }
  }

  @Test
  public void testDropTemporaryTableNameStartsWithSlash() throws Exception {
    String tableName = "table_starts_with_slash_drop";

    try {
      test("CREATE TEMPORARY TABLE `%s` AS SELECT 1 FROM cp.`employee.json`", tableName);
      testBuilder()
          .sqlQuery("DROP TABLE `%s`", "/" + tableName)
          .unOrdered()
          .baselineColumns("ok", "summary")
          .baselineValues(true, String.format("Temporary table [%s] dropped", tableName))
          .go();
    } finally {
      test("DROP TABLE IF EXISTS %s", tableName);
    }
  }

  @Test // DRILL-7050
  public void testTemporaryTableInSubQuery() throws Exception {
    test("create temporary table source as (select 1 as id union all select 2 as id)");

    String query =
        "select t1.id as id,\n" +
            "(select count(t2.id)\n" +
            "from source t2 where t2.id = t1.id) as c\n" +
        "from source t1";

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("id", "c")
        .baselineValues(1, 1L)
        .baselineValues(2, 1L)
        .go();
  }

  private void expectUserRemoteExceptionWithMessage(String message) {
    thrown.expect(UserRemoteException.class);
    thrown.expectMessage(containsString(message));
  }

  private void expectUserRemoteExceptionWithTableExistsMessage(String tableName, String schemaName) {
    expectUserRemoteExceptionWithMessage(String.format(
      "VALIDATION ERROR: A table or view with given name [%s]" +
        " already exists in schema [%s]", tableName, schemaName));
  }

}
