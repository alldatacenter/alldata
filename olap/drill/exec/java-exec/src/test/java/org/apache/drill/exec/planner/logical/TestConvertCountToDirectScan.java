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
package org.apache.drill.exec.planner.logical;

import org.apache.drill.categories.PlannerTest;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

@Category({PlannerTest.class, UnlikelyTest.class})
public class TestConvertCountToDirectScan extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    dirTestWatcher.copyResourceToRoot(Paths.get("directcount.parquet"));
    startCluster(builder);
  }

  @Test
  public void testCaseDoesNotConvertToDirectScan() throws Exception {
    queryBuilder()
      .sql("select " +
      "count(case when n_name = 'ALGERIA' and n_regionkey = 2 then n_nationkey else null end) as cnt " +
      "from dfs.`directcount.parquet`")
      .planMatcher()
      .include("CASE")
      .match();
  }

  @Test
  public void testConvertSimpleCountToDirectScan() throws Exception {
    String sql = "select count(*) as cnt from cp.`tpch/nation.parquet`";

    queryBuilder()
      .sql(sql)
      .planMatcher()
      .include("DynamicPojoRecordReader")
      .match();

    testBuilder()
      .sqlQuery(sql)
      .unOrdered()
      .baselineColumns("cnt")
      .baselineValues(25L)
      .go();
  }

  @Test
  public void testConvertSimpleCountConstToDirectScan() throws Exception {
    String sql = "select count(100) as cnt from cp.`tpch/nation.parquet`";

    queryBuilder()
      .sql(sql)
      .planMatcher()
      .include("DynamicPojoRecordReader")
      .match();

    testBuilder()
      .sqlQuery(sql)
      .unOrdered()
      .baselineColumns("cnt")
      .baselineValues(25L)
      .go();
  }

  @Test
  public void testConvertSimpleCountConstExprToDirectScan() throws Exception {
    String sql = "select count(1 + 2) as cnt from cp.`tpch/nation.parquet`";

    queryBuilder()
      .sql(sql)
      .planMatcher()
      .include("DynamicPojoRecordReader")
      .match();

    testBuilder()
      .sqlQuery(sql)
      .unOrdered()
      .baselineColumns("cnt")
      .baselineValues(25L)
      .go();
  }

  @Test
  public void testDoesNotConvertForDirectoryColumns() throws Exception {
    String sql = "select count(dir0) as cnt from cp.`tpch/nation.parquet`";

    queryBuilder()
      .sql(sql)
      .planMatcher()
      .include("ParquetGroupScan")
      .match();

    testBuilder()
      .sqlQuery(sql)
      .unOrdered()
      .baselineColumns("cnt")
      .baselineValues(0L)
      .go();
  }

  @Test
  public void testConvertForImplicitColumns() throws Exception {
    String sql = "select count(fqn) as cnt from cp.`tpch/nation.parquet`";

    queryBuilder()
      .sql(sql)
      .planMatcher()
      .include("DynamicPojoRecordReader")
      .match();

    testBuilder()
      .sqlQuery(sql)
      .unOrdered()
      .baselineColumns("cnt")
      .baselineValues(25L)
      .go();
  }

  @Test
  public void ensureConvertForSeveralColumns() throws Exception {
    run("use dfs.tmp");
    String tableName = "parquet_table_counts";

    try {
      String newFqnColumnName = "new_fqn";
      client.alterSession(ExecConstants.IMPLICIT_FQN_COLUMN_LABEL, newFqnColumnName);
      run("create table %s as select * from cp.`parquet/alltypes_optional.parquet`", tableName);
      run("refresh table metadata %s", tableName);

      String sql = String.format("select\n" +
        "count(%s) as implicit_count,\n" +
        "count(*) as star_count,\n" +
        "count(col_int) as int_column_count,\n" +
        "count(col_vrchr) as vrchr_column_count\n" +
        "from %s", newFqnColumnName, tableName);

      queryBuilder()
        .sql(sql)
        .planMatcher()
        .include("DynamicPojoRecordReader")
        .match();

      testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("implicit_count", "star_count", "int_column_count", "vrchr_column_count")
        .baselineValues(6L, 6L, 2L, 3L)
        .go();

    } finally {
      client.resetSession(ExecConstants.IMPLICIT_FQN_COLUMN_LABEL);
      run("drop table if exists %s", tableName);
    }
  }

  @Test
  public void testCorrectCountWithMissingStatistics() throws Exception {
    run("use dfs.tmp");
    String tableName = "wide_str_table";
    try {
      // table will contain two partitions: one - with null value, second - with non null value
      run("create table %s partition by (col_str) as select * from cp.`parquet/wide_string.parquet`", tableName);

      String sql = String.format("select count(col_str) as cnt_str, count(*) as cnt_total from %s", tableName);

      // direct scan should not be applied since we don't have statistics
      queryBuilder()
        .sql(sql)
        .planMatcher()
        .exclude("DynamicPojoRecordReader")
        .match();

      testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("cnt_str", "cnt_total")
        .baselineValues(1L, 2L)
        .go();
    } finally {
      run("drop table if exists %s", tableName);
    }
  }

  @Test
  public void testCountsWithMetadataCacheSummary() throws Exception {
    run("use dfs.tmp");

    String tableName = "parquet_table_counts";

    try {
      run("create table `%s/1` as select * from cp.`parquet/alltypes_optional.parquet`", tableName);
      run("create table `%s/2` as select * from cp.`parquet/alltypes_optional.parquet`", tableName);
      run("create table `%s/3` as select * from cp.`parquet/alltypes_optional.parquet`", tableName);
      run("create table `%s/4` as select * from cp.`parquet/alltypes_optional.parquet`", tableName);

      run("refresh table metadata %s", tableName);

      String sql = String.format("select\n" +
        "count(*) as star_count,\n" +
        "count(col_int) as int_column_count,\n" +
        "count(col_vrchr) as vrchr_column_count\n" +
        "from %s", tableName);

      queryBuilder()
        .sql(sql)
        .planMatcher()
        .include("numFiles = 1")
        .include("usedMetadataSummaryFile = true")
        .include("DynamicPojoRecordReader")
        .match();

      testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("star_count", "int_column_count", "vrchr_column_count")
        .baselineValues(24L, 8L, 12L)
        .go();

    } finally {
      run("drop table if exists %s", tableName);
    }
  }

  @Test
  public void testCountsWithMetadataCacheSummaryAndDirPruning() throws Exception {
    run("use dfs.tmp");
    String tableName = "parquet_table_counts";

    try {
      run("create table `%s/1` as select * from cp.`parquet/alltypes_optional.parquet`", tableName);
      run("create table `%s/2` as select * from cp.`parquet/alltypes_optional.parquet`", tableName);
      run("create table `%s/3` as select * from cp.`parquet/alltypes_optional.parquet`", tableName);
      run("create table `%s/4` as select * from cp.`parquet/alltypes_optional.parquet`", tableName);

      run("refresh table metadata %s", tableName);

      String sql = String.format("select\n" +
        "count(*) as star_count,\n" +
        "count(col_int) as int_column_count,\n" +
        "count(col_vrchr) as vrchr_column_count\n" +
        "from %s where dir0 = 1 ", tableName);

      queryBuilder()
        .sql(sql)
        .planMatcher()
        .include("numFiles = 1")
        .include("usedMetadataSummaryFile = true")
        .include("DynamicPojoRecordReader")
        .match();

      testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("star_count", "int_column_count", "vrchr_column_count")
        .baselineValues(6L, 2L, 3L)
        .go();

    } finally {
      run("drop table if exists %s", tableName);
    }
  }

  @Test
  public void textConvertAbsentColumn() throws Exception {
    String sql = "select count(abc) as cnt from cp.`tpch/nation.parquet`";

    queryBuilder()
        .sql(sql)
        .planMatcher()
        .include("DynamicPojoRecordReader")
        .match();

    testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("cnt")
        .baselineValues(0L)
        .go();
  }

  @Test
  public void testCountsWithWildCard() throws Exception {
    run("use dfs.tmp");
    String tableName = "parquet_table_counts";

    try {
      for (int i = 0; i < 10; i++) {
        run("create table `%s/12/%s` as select * from cp.`tpch/nation.parquet`", tableName, i);
      }
      run("create table `%s/2` as select * from cp.`tpch/nation.parquet`", tableName);
      run("create table `%s/2/11` as select * from cp.`tpch/nation.parquet`", tableName);
      run("create table `%s/2/12` as select * from cp.`tpch/nation.parquet`", tableName);

      run("refresh table metadata %s", tableName);

      String sql = String.format("select\n" +
        "count(*) as star_count\n" +
        "from `%s/1*`", tableName);

      queryBuilder()
        .sql(sql)
        .planMatcher()
        .include("usedMetadataSummaryFile = false")
        .include("DynamicPojoRecordReader")
        .match();

      testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("star_count")
        .baselineValues(250L)
        .go();

    } finally {
      run("drop table if exists %s", tableName);
    }
  }

  @Test
  public void testCountsForLeafDirectories() throws Exception {
    run("use dfs.tmp");
    String tableName = "parquet_table_counts";

    try {
      run("create table `%s/1` as select * from cp.`tpch/nation.parquet`", tableName);
      run("create table `%s/2` as select * from cp.`tpch/nation.parquet`", tableName);
      run("create table `%s/3` as select * from cp.`tpch/nation.parquet`", tableName);
      run("refresh table metadata %s", tableName);

      String sql = String.format("select\n" +
        "count(*) as star_count\n" +
        "from `%s/1`", tableName);

      queryBuilder()
        .sql(sql)
        .planMatcher()
        .include("numFiles = 1")
        .include("usedMetadataSummaryFile = true")
        .include("DynamicPojoRecordReader")
        .match();

      testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("star_count")
        .baselineValues(25L)
        .go();

    } finally {
      run("drop table if exists %s", tableName);
    }
  }

  @Test
  public void testCountsForDirWithFilesAndDir() throws Exception {
    run("use dfs.tmp");
    String tableName = "parquet_table_counts";

    try {
      run("create table `%s/1` as select * from cp.`tpch/nation.parquet`", tableName);
      run("create table `%s/1/2` as select * from cp.`tpch/nation.parquet`", tableName);
      run("create table `%s/1/3` as select * from cp.`tpch/nation.parquet`", tableName);
      run("refresh table metadata %s", tableName);

      String sql = String.format("select count(*) as star_count from `%s/1`", tableName);

      queryBuilder()
        .sql(sql)
        .planMatcher()
        .include("numFiles = 1")
        .include("usedMetadataSummaryFile = true")
        .include("DynamicPojoRecordReader")
        .match();

      testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("star_count")
        .baselineValues(75L)
        .go();

    } finally {
      run("drop table if exists %s", tableName);
    }
  }

  @Test
  public void testCountsWithNonExistingColumn() throws Exception {
    run("use dfs.tmp");
    String tableName = "parquet_table_counts_nonex";

    try {
      run("create table `%s/1` as select * from cp.`parquet/alltypes_optional.parquet`", tableName);
      run("create table `%s/2` as select * from cp.`parquet/alltypes_optional.parquet`", tableName);
      run("create table `%s/3` as select * from cp.`parquet/alltypes_optional.parquet`", tableName);
      run("create table `%s/4` as select * from cp.`parquet/alltypes_optional.parquet`", tableName);

      run("refresh table metadata %s", tableName);

      String sql = String.format("select\n" +
        "count(*) as star_count,\n" +
        "count(col_int) as int_column_count,\n" +
        "count(col_vrchr) as vrchr_column_count,\n" +
        "count(non_existent) as non_existent\n" +
        "from %s", tableName);

      queryBuilder()
        .sql(sql)
        .planMatcher()
        .include("numFiles = 1")
        .include("usedMetadataSummaryFile = true")
        .include("DynamicPojoRecordReader")
        .match();

      testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("star_count", "int_column_count", "vrchr_column_count", "non_existent" )
        .baselineValues(24L, 8L, 12L, 0L)
        .go();

    } finally {
      run("drop table if exists %s", tableName);
    }
  }

  @Test
  public void testSerDe() throws Exception {
    String sql = "select count(*) as cnt from cp.`tpch/nation.parquet`";
    String plan = queryBuilder().sql(sql).explainJson();
    long cnt = queryBuilder().physical(plan).singletonLong();
    assertEquals("Counts should match", 25L, cnt);
  }
}
