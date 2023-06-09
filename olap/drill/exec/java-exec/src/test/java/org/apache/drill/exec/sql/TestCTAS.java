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

import static org.apache.drill.exec.util.StoragePluginTestUtils.DFS_TMP_SCHEMA;
import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.apache.drill.categories.SqlTest;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.exec.store.StorageStrategy;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.drill.test.BaseTestQuery;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SqlTest.class)
public class TestCTAS extends BaseTestQuery {
  @Test // DRILL-2589
  @Category(UnlikelyTest.class)
  public void withDuplicateColumnsInDef1() throws Exception {
    ctasErrorTestHelper("CREATE TABLE dfs.tmp.%s AS SELECT region_id, region_id FROM cp.`region.json`",
        String.format("Duplicate column name [%s]", "region_id")
    );
  }

  @Test // DRILL-2589
  @Category(UnlikelyTest.class)
  public void withDuplicateColumnsInDef2() throws Exception {
    ctasErrorTestHelper("CREATE TABLE dfs.tmp.%s AS SELECT region_id, sales_city, sales_city FROM cp.`region.json`",
        String.format("Duplicate column name [%s]", "sales_city")
    );
  }

  @Test // DRILL-2589
  @Category(UnlikelyTest.class)
  public void withDuplicateColumnsInDef3() throws Exception {
    ctasErrorTestHelper(
        "CREATE TABLE dfs.tmp.%s(regionid, regionid) " +
            "AS SELECT region_id, sales_city FROM cp.`region.json`",
        String.format("Duplicate column name [%s]", "regionid")
    );
  }

  @Test // DRILL-2589
  @Category(UnlikelyTest.class)
  public void withDuplicateColumnsInDef4() throws Exception {
    ctasErrorTestHelper(
        "CREATE TABLE dfs.tmp.%s(regionid, salescity, salescity) " +
            "AS SELECT region_id, sales_city, sales_city FROM cp.`region.json`",
        String.format("Duplicate column name [%s]", "salescity")
    );
  }

  @Test // DRILL-2589
  @Category(UnlikelyTest.class)
  public void withDuplicateColumnsInDef5() throws Exception {
    ctasErrorTestHelper(
        "CREATE TABLE dfs.tmp.%s(regionid, salescity, SalesCity) " +
            "AS SELECT region_id, sales_city, sales_city FROM cp.`region.json`",
        String.format("Duplicate column name [%s]", "SalesCity")
    );
  }

  @Test // DRILL-2589
  public void whenInEqualColumnCountInTableDefVsInTableQuery() throws Exception {
    ctasErrorTestHelper(
        "CREATE TABLE dfs.tmp.%s(regionid, salescity) " +
            "AS SELECT region_id, sales_city, sales_region FROM cp.`region.json`",
        "table's field list and the table's query field list have different counts."
    );
  }

  @Test // DRILL-2589
  public void whenTableQueryColumnHasStarAndTableFiledListIsSpecified() throws Exception {
    ctasErrorTestHelper(
        "CREATE TABLE dfs.tmp.%s(regionid, salescity) " +
            "AS SELECT region_id, * FROM cp.`region.json`",
        "table's query field list has a '*', which is invalid when table's field list is specified."
    );
  }

  @Test // DRILL-2422
  @Category(UnlikelyTest.class)
  public void createTableWhenATableWithSameNameAlreadyExists() throws Exception{
    final String newTblName = "createTableWhenTableAlreadyExists";
    final String ctasQuery = String.format("CREATE TABLE dfs.tmp.%s AS SELECT * from cp.`region.json`", newTblName);

    test(ctasQuery);
    errorMsgTestHelper(ctasQuery, String.format("A table or view with given name [%s] already exists in schema [dfs.tmp]", newTblName));
  }

  @Test // DRILL-2422
  @Category(UnlikelyTest.class)
  public void createTableWhenAViewWithSameNameAlreadyExists() throws Exception{
    final String newTblName = "createTableWhenAViewWithSameNameAlreadyExists";

    try {
      test("CREATE VIEW dfs.tmp.%s AS SELECT * from cp.`region.json`", newTblName);

      final String ctasQuery = String.format("CREATE TABLE dfs.tmp.%s AS SELECT * FROM cp.`employee.json`", newTblName);

      errorMsgTestHelper(ctasQuery,
          String.format("A table or view with given name [%s] already exists in schema [%s]",
              newTblName, "dfs.tmp"));
    } finally {
      test("DROP VIEW dfs.tmp.%s", newTblName);
    }
  }

  @Test
  public void ctasPartitionWithEmptyList() throws Exception {
    final String newTblName = "ctasPartitionWithEmptyList";
    final String ctasQuery = String.format("CREATE TABLE dfs.tmp.%s PARTITION BY AS SELECT * from cp.`region.json`", newTblName);

    errorMsgTestHelper(ctasQuery,"PARSE ERROR: Encountered \"AS\"");
  }

  @Test // DRILL-3377
  public void partitionByCtasColList() throws Exception {
    final String newTblName = "partitionByCtasColList";

    test("CREATE TABLE dfs.tmp.%s (cnt, rkey) PARTITION BY (cnt) " +
      "AS SELECT count(*), n_regionkey from cp.`tpch/nation.parquet` group by n_regionkey", newTblName);

    testBuilder()
        .sqlQuery("select cnt, rkey from dfs.tmp.%s", newTblName)
        .unOrdered()
        .sqlBaselineQuery("select count(*) as cnt, n_regionkey as rkey from cp.`tpch/nation.parquet` group by n_regionkey")
        .build()
        .run();
  }

  @Test // DRILL-3374
  public void partitionByCtasFromView() throws Exception {
    final String newTblName = "partitionByCtasFromView";
    final String newView = "partitionByCtasColListView";

    test("create or replace view dfs.tmp.%s (col_int, col_varchar)  " +
      "AS select cast(n_nationkey as int), cast(n_name as varchar(30)) from cp.`tpch/nation.parquet`", newView);
    test("CREATE TABLE dfs.tmp.%s PARTITION BY (col_int) AS SELECT * from dfs.tmp.%s",
      newTblName, newView);

    testBuilder()
        .sqlQuery("select col_int, col_varchar from dfs.tmp.%s", newTblName)
        .unOrdered()
        .sqlBaselineQuery("select cast(n_nationkey as int) as col_int, cast(n_name as varchar(30)) as col_varchar " +
          "from cp.`tpch/nation.parquet`")
        .build()
        .run();

    test("DROP VIEW dfs.tmp.%s", newView);
  }

  @Test // DRILL-3382
  public void ctasWithQueryOrderby() throws Exception {
    final String newTblName = "ctasWithQueryOrderby";

    test("CREATE TABLE dfs.tmp.%s AS SELECT n_nationkey, n_name, n_comment from " +
      "cp.`tpch/nation.parquet` order by n_nationkey", newTblName);

    testBuilder()
        .sqlQuery("select n_nationkey, n_name, n_comment from dfs.tmp.%s", newTblName)
        .ordered()
        .sqlBaselineQuery("select n_nationkey, n_name, n_comment from cp.`tpch/nation.parquet` order by n_nationkey")
        .build()
        .run();
  }

  @Test // DRILL-4392
  public void ctasWithPartition() throws Exception {
    final String newTblName = "nation_ctas";

    test("CREATE TABLE dfs.tmp.%s partition by (n_regionkey) AS " +
      "SELECT n_nationkey, n_regionkey from cp.`tpch/nation.parquet` order by n_nationkey limit 1", newTblName);

    testBuilder()
        .sqlQuery("select * from dfs.tmp.%s", newTblName)
        .ordered()
        .sqlBaselineQuery("select n_nationkey, n_regionkey from cp.`tpch/nation.parquet` order by n_nationkey limit 1")
        .build()
        .run();
  }

  @Test
  public void testPartitionByForAllTypes() throws Exception {
    final String location = "partitioned_tables_with_nulls";
    final String ctasQuery = "create table %s partition by (%s) as %s";
    final String tablePath = "dfs.tmp.`%s/%s_%s`";

    // key - new table suffix, value - data query
    final Map<String, String> variations = Maps.newHashMap();
    variations.put("required", "select * from cp.`parquet/alltypes_required.parquet`");
    variations.put("optional", "select * from cp.`parquet/alltypes_optional.parquet`");
    variations.put("nulls_only", "select * from cp.`parquet/alltypes_optional.parquet` where %s is null");

    final QueryDataBatch result = testSqlWithResults("select * from cp.`parquet/alltypes_required.parquet` limit 0").get(0);
    for (UserBitShared.SerializedField field : result.getHeader().getDef().getFieldList()) {
      final String fieldName = field.getNamePart().getName();

      for (Map.Entry<String, String> variation : variations.entrySet()) {
        final String table = String.format(tablePath, location, fieldName, variation.getKey());
        final String dataQuery = String.format(variation.getValue(), fieldName);
        test(ctasQuery, table, fieldName, dataQuery, fieldName);
        testBuilder()
          .sqlQuery("select * from %s", table)
          .unOrdered()
          .sqlBaselineQuery(dataQuery)
          .build()
          .run();
      }
    }

    result.release();
  }

  @Test
  public void createTableWithCustomUmask() throws Exception {
    test("use dfs.tmp");
    String tableName = "with_custom_permission";
    StorageStrategy storageStrategy = new StorageStrategy("000", false);
    FileSystem fs = getLocalFileSystem();
    try {
      test("alter session set `%s` = '%s'", ExecConstants.PERSISTENT_TABLE_UMASK, storageStrategy.getUmask());
      test("create table %s as select 'A' from (values(1))", tableName);
      Path tableLocation = new Path(dirTestWatcher.getDfsTestTmpDir().getAbsolutePath(), tableName);
      assertEquals("Directory permission should match",
          storageStrategy.getFolderPermission(), fs.getFileStatus(tableLocation).getPermission());
      assertEquals("File permission should match",
          storageStrategy.getFilePermission(), fs.listLocatedStatus(tableLocation).next().getPermission());
    } finally {
      resetSessionOption(ExecConstants.PERSISTENT_TABLE_UMASK);
      test("drop table if exists %s", tableName);
    }
  }

  @Test // DRILL-5952
  public void testCreateTableIfNotExistsWhenTableWithSameNameAlreadyExists() throws Exception{
    final String newTblName = "createTableIfNotExistsWhenATableWithSameNameAlreadyExists";

    try {
      String ctasQuery = String.format("CREATE TABLE %s.%s AS SELECT * from cp.`region.json`", DFS_TMP_SCHEMA, newTblName);

      test(ctasQuery);

      ctasQuery =
        String.format("CREATE TABLE IF NOT EXISTS %s.%s AS SELECT * FROM cp.`employee.json`", DFS_TMP_SCHEMA, newTblName);

      testBuilder()
        .sqlQuery(ctasQuery)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(false, String.format("A table or view with given name [%s] already exists in schema [%s]", newTblName, DFS_TMP_SCHEMA))
        .go();
    } finally {
      test("DROP TABLE IF EXISTS %s.%s", DFS_TMP_SCHEMA, newTblName);
    }
  }

  @Test // DRILL-5952
  public void testCreateTableIfNotExistsWhenViewWithSameNameAlreadyExists() throws Exception{
    final String newTblName = "createTableIfNotExistsWhenAViewWithSameNameAlreadyExists";

    try {
      String ctasQuery = String.format("CREATE VIEW %s.%s AS SELECT * from cp.`region.json`", DFS_TMP_SCHEMA, newTblName);

      test(ctasQuery);

      ctasQuery =
        String.format("CREATE TABLE IF NOT EXISTS %s.%s AS SELECT * FROM cp.`employee.json`", DFS_TMP_SCHEMA, newTblName);

      testBuilder()
        .sqlQuery(ctasQuery)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(false, String.format("A table or view with given name [%s] already exists in schema [%s]", newTblName, DFS_TMP_SCHEMA))
        .go();
    } finally {
      test("DROP VIEW IF EXISTS %s.%s", DFS_TMP_SCHEMA, newTblName);
    }
  }

  @Test // DRILL-5952
  public void testCreateTableIfNotExistsWhenTableWithSameNameDoesNotExist() throws Exception{
    final String newTblName = "createTableIfNotExistsWhenATableWithSameNameDoesNotExist";

    try {
      String ctasQuery = String.format("CREATE TABLE IF NOT EXISTS %s.%s AS SELECT * FROM cp.`employee.json`", DFS_TMP_SCHEMA, newTblName);

      test(ctasQuery);

    } finally {
      test("DROP TABLE IF EXISTS %s.%s", DFS_TMP_SCHEMA, newTblName);
    }
  }

  @Test
  public void testCTASWithEmptyJson() throws Exception {
    final String newTblName = "tbl4444";
    try {
      test(String.format("CREATE TABLE %s.%s AS SELECT * FROM cp.`project/pushdown/empty.json`", DFS_TMP_SCHEMA, newTblName));
    } finally {
      test("DROP TABLE IF EXISTS %s.%s", DFS_TMP_SCHEMA, newTblName);
    }
  }

  @Test
  public void testTableIsCreatedWithinWorkspace() throws Exception {
    String tableName = "table_created_within_workspace";
    try {
      test("CREATE TABLE `%s`.`%s` AS SELECT * FROM cp.`region.json`", DFS_TMP_SCHEMA, "/" + tableName);
      testBuilder()
          .sqlQuery("SELECT region_id FROM `%s`.`%s` LIMIT 1", DFS_TMP_SCHEMA, tableName)
          .unOrdered()
          .baselineColumns("region_id")
          .baselineValues(0L)
          .go();
    } finally {
      test("DROP TABLE IF EXISTS `%s`.`%s`", DFS_TMP_SCHEMA, tableName);
    }
  }

  @Test
  public void testTableIsFoundWithinWorkspaceWhenNameStartsWithSlash() throws Exception {
    String tableName = "table_found_within_workspace";
    try {
      test("CREATE TABLE `%s`.`%s` AS SELECT * FROM cp.`region.json`", DFS_TMP_SCHEMA, tableName);
      testBuilder()
          .sqlQuery("SELECT region_id FROM `%s`.`%s` LIMIT 1", DFS_TMP_SCHEMA, "/" + tableName)
          .unOrdered()
          .baselineColumns("region_id")
          .baselineValues(0L)
          .go();
    } finally {
      test("DROP TABLE IF EXISTS `%s`.`%s`", DFS_TMP_SCHEMA, tableName);
    }
  }

  private static void ctasErrorTestHelper(final String ctasSql, final String expErrorMsg) throws Exception {
    final String createTableSql = String.format(ctasSql, "testTableName");
    errorMsgTestHelper(createTableSql, expErrorMsg);
  }
}
