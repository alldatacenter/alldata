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

import java.nio.file.Paths;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.physical.rowSet.DirectRowSet;
import org.apache.drill.exec.physical.rowSet.RowSetReader;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import static org.junit.Assert.assertEquals;

import org.apache.drill.exec.vector.accessor.ObjectReader;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class TestAnalyze extends ClusterTest {

  @BeforeClass
  public static void copyData() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    startCluster(builder);
    dirTestWatcher.copyResourceToRoot(Paths.get("multilevel", "parquet"));
  }

  // Analyze for all columns
  @Test
  public void basic1() throws Exception {
    try {
      client.alterSession(ExecConstants.SLICE_TARGET, 1);
      client.alterSession(ExecConstants.OUTPUT_FORMAT_OPTION, "parquet");
      run("CREATE TABLE dfs.tmp.region_basic1 AS SELECT * from cp.`region.json`");
      run("ANALYZE TABLE dfs.tmp.region_basic1 COMPUTE STATISTICS");
      run("SELECT * FROM dfs.tmp.`region_basic1/.stats.drill`");
      run("create table dfs.tmp.flatstats1 as select flatten(`directories`[0].`columns`) as `columns`"
              + " from dfs.tmp.`region_basic1/.stats.drill`");

      testBuilder()
          .sqlQuery("SELECT tbl.`columns`.`column` as `column`, tbl.`columns`.rowcount as rowcount,"
              + " tbl.`columns`.nonnullrowcount as nonnullrowcount, tbl.`columns`.ndv as ndv,"
              + " tbl.`columns`.avgwidth as avgwidth"
              + " FROM dfs.tmp.flatstats1 tbl")
          .unOrdered()
          .baselineColumns("column", "rowcount", "nonnullrowcount", "ndv", "avgwidth")
          .baselineValues("`region_id`", 110.0, 110.0, 110L, 8.0)
          .baselineValues("`sales_city`", 110.0, 110.0, 109L, 8.663636363636364)
          .baselineValues("`sales_state_province`", 110.0, 110.0, 13L, 2.4272727272727272)
          .baselineValues("`sales_district`", 110.0, 110.0, 23L, 9.318181818181818)
          .baselineValues("`sales_region`", 110.0, 110.0, 8L, 10.8)
          .baselineValues("`sales_country`", 110.0, 110.0, 4L, 3.909090909090909)
          .baselineValues("`sales_district_id`", 110.0, 110.0, 23L, 8.0)
          .go();
    } finally {
      client.resetSession(ExecConstants.SLICE_TARGET);
      client.resetSession(ExecConstants.OUTPUT_FORMAT_OPTION);
    }
  }

  // Analyze for only a subset of the columns in table
  @Test
  public void basic2() throws Exception {
    try {
      client.alterSession(ExecConstants.SLICE_TARGET, 1);
      client.alterSession(ExecConstants.OUTPUT_FORMAT_OPTION, "parquet");
      run("CREATE TABLE dfs.tmp.employee_basic2 AS SELECT * from cp.`employee.json`");
      run("ANALYZE TABLE dfs.tmp.employee_basic2 COMPUTE STATISTICS (employee_id, birth_date)");
      run("SELECT * FROM dfs.tmp.`employee_basic2/.stats.drill`");
      run("create table dfs.tmp.flatstats2 as select flatten(`directories`[0].`columns`) as `columns`"
          + " from dfs.tmp.`employee_basic2/.stats.drill`");

      testBuilder()
          .sqlQuery("SELECT tbl.`columns`.`column` as `column`, tbl.`columns`.rowcount as rowcount,"
              + " tbl.`columns`.nonnullrowcount as nonnullrowcount, tbl.`columns`.ndv as ndv,"
              + " tbl.`columns`.avgwidth as avgwidth"
              + " FROM dfs.tmp.flatstats2 tbl")
          .unOrdered()
          .baselineColumns("column", "rowcount", "nonnullrowcount", "ndv", "avgwidth")
          .baselineValues("`employee_id`", 1155.0, 1155.0, 1155L, 8.0)
          .baselineValues("`birth_date`", 1155.0, 1155.0, 52L, 10.0)
          .go();
    } finally {
      client.resetSession(ExecConstants.SLICE_TARGET);
      client.resetSession(ExecConstants.OUTPUT_FORMAT_OPTION);
    }
  }

  // Analyze with sampling percentage
  @Test
  public void basic3() throws Exception {
    try {
      client.alterSession(ExecConstants.SLICE_TARGET, 1);
      client.alterSession(ExecConstants.OUTPUT_FORMAT_OPTION, "parquet");
      client.alterSession(ExecConstants.DETERMINISTIC_SAMPLING, true);
      run("CREATE TABLE dfs.tmp.employee_basic3 AS SELECT * from cp.`employee.json`");
      run("ANALYZE TABLE table(dfs.tmp.employee_basic3 (type => 'parquet')) COMPUTE STATISTICS (employee_id, birth_date) SAMPLE 55 PERCENT");

      testBuilder()
          .sqlQuery("SELECT tbl.`columns`.`column` as `column`, tbl.`columns`.rowcount is not null as has_rowcount,"
              + " tbl.`columns`.nonnullrowcount is not null as has_nonnullrowcount, tbl.`columns`.ndv is not null as has_ndv,"
              + " tbl.`columns`.avgwidth is not null as has_avgwidth"
              + " FROM (select flatten(`directories`[0].`columns`) as `columns` from dfs.tmp.`employee_basic3/.stats.drill`) tbl")
          .unOrdered()
          .baselineColumns("column", "has_rowcount", "has_nonnullrowcount", "has_ndv", "has_avgwidth")
          .baselineValues("`employee_id`", true, true, true, true)
          .baselineValues("`birth_date`", true, true, true, true)
          .go();
    } finally {
      client.resetSession(ExecConstants.DETERMINISTIC_SAMPLING);
      client.resetSession(ExecConstants.SLICE_TARGET);
      client.resetSession(ExecConstants.OUTPUT_FORMAT_OPTION);
      run("drop table if exists dfs.tmp.employee_basic3");
    }
  }

  @Test
  public void join() throws Exception {
    try {
      client.alterSession(ExecConstants.SLICE_TARGET, 1);
      client.alterSession(ExecConstants.OUTPUT_FORMAT_OPTION, "parquet");
      run("CREATE TABLE dfs.tmp.lineitem AS SELECT * FROM cp.`tpch/lineitem.parquet`");
      run("CREATE TABLE dfs.tmp.orders AS select * FROM cp.`tpch/orders.parquet`");
      run("ANALYZE TABLE dfs.tmp.lineitem COMPUTE STATISTICS");
      run("ANALYZE TABLE dfs.tmp.orders COMPUTE STATISTICS");
      run("SELECT * FROM dfs.tmp.`lineitem/.stats.drill`");
      run("SELECT * FROM dfs.tmp.`orders/.stats.drill`");
      client.alterSession(PlannerSettings.STATISTICS_USE.getOptionName(), true);
      run("SELECT * FROM dfs.tmp.`lineitem` l JOIN dfs.tmp.`orders` o ON l.l_orderkey = o.o_orderkey");
    } finally {
      client.resetSession(ExecConstants.SLICE_TARGET);
      client.resetSession(ExecConstants.OUTPUT_FORMAT_OPTION);
      client.resetSession(PlannerSettings.STATISTICS_USE.getOptionName());
    }
  }

  @Test
  public void testAnalyzeSupportedFormats() throws Exception {
    //Only allow computing statistics on PARQUET files.
    try {
      client.alterSession(ExecConstants.SLICE_TARGET, 1);
      client.alterSession(ExecConstants.OUTPUT_FORMAT_OPTION, "json");
      run("CREATE TABLE dfs.tmp.employee_basic4 AS SELECT * from cp.`employee.json`");
      //Should display not supported
      verifyAnalyzeOutput("ANALYZE TABLE dfs.tmp.employee_basic4 COMPUTE STATISTICS",
          "Table employee_basic4 is not supported by ANALYZE. "
          + "Support is currently limited to directory-based Parquet tables.");

      run("DROP TABLE dfs.tmp.employee_basic4");
      client.alterSession(ExecConstants.OUTPUT_FORMAT_OPTION, "parquet");
      run("CREATE TABLE dfs.tmp.employee_basic4 AS SELECT * from cp.`employee.json`");
      //Should complete successfully (16 columns in employee.json)
      verifyAnalyzeOutput("ANALYZE TABLE dfs.tmp.employee_basic4 COMPUTE STATISTICS",
          "16");
    } finally {
      client.resetSession(ExecConstants.SLICE_TARGET);
      client.resetSession(ExecConstants.OUTPUT_FORMAT_OPTION);
    }
  }

  @Ignore("For 1.16.0, we do not plan to support statistics on dir columns")
  @Test
  public void testAnalyzePartitionedTables() throws Exception {
    //Computing statistics on columns, dir0, dir1
    try {
      String tmpLocation = "/multilevel/parquet";
      client.alterSession(ExecConstants.SLICE_TARGET, 1);
      client.alterSession(ExecConstants.OUTPUT_FORMAT_OPTION, "parquet");
      run("CREATE TABLE dfs.tmp.parquet1 AS SELECT * from dfs.`%s`", tmpLocation);
      verifyAnalyzeOutput("ANALYZE TABLE dfs.tmp.parquet1 COMPUTE STATISTICS", "11");
      run("SELECT * FROM dfs.tmp.`parquet1/.stats.drill`");
      run("create table dfs.tmp.flatstats4 as select flatten(`directories`[0].`columns`) as `columns` " +
           "from dfs.tmp.`parquet1/.stats.drill`");
      //Verify statistics
      testBuilder()
          .sqlQuery("SELECT tbl.`columns`.`column` as `column`, tbl.`columns`.rowcount as rowcount,"
              + " tbl.`columns`.nonnullrowcount as nonnullrowcount, tbl.`columns`.ndv as ndv,"
              + " tbl.`columns`.avgwidth as avgwidth"
              + " FROM dfs.tmp.flatstats4 tbl")
          .unOrdered()
          .baselineColumns("column", "rowcount", "nonnullrowcount", "ndv", "avgwidth")
          .baselineValues("`o_orderkey`", 120.0, 120.0, 119L, 4.0)
          .baselineValues("`o_custkey`", 120.0, 120.0, 113L, 4.0)
          .baselineValues("`o_orderstatus`", 120.0, 120.0, 3L, 1.0)
          .baselineValues("`o_totalprice`", 120.0, 120.0, 120L, 8.0)
          .baselineValues("`o_orderdate`", 120.0, 120.0, 111L, 4.0)
          .baselineValues("`o_orderpriority`", 120.0, 120.0, 5L, 8.458333333333334)
          .baselineValues("`o_clerk`", 120.0, 120.0, 114L, 15.0)
          .baselineValues("`o_shippriority`", 120.0, 120.0, 1L, 4.0)
          .baselineValues("`o_comment`", 120.0, 120.0, 120L, 46.333333333333336)
          .baselineValues("`dir0`", 120.0, 120.0, 3L, 4.0)
          .baselineValues("`dir1`", 120.0, 120.0, 4L, 2.0)
          .go();
    } finally {
      client.resetSession(ExecConstants.SLICE_TARGET);
      client.resetSession(ExecConstants.OUTPUT_FORMAT_OPTION);
    }
  }

  @Test
  public void testStaleness() throws Exception {
    // copy the data into the temporary location
    String tmpLocation = "/multilevel/parquet";
    client.alterSession(ExecConstants.SLICE_TARGET, 1);
    client.alterSession(ExecConstants.OUTPUT_FORMAT_OPTION, "parquet");
    try {
      run("CREATE TABLE dfs.tmp.parquetStale AS SELECT o_orderkey, o_custkey, o_orderstatus, " +
           "o_totalprice, o_orderdate, o_orderpriority, o_clerk, o_shippriority, o_comment from dfs.`%s`", tmpLocation);
      verifyAnalyzeOutput("ANALYZE TABLE dfs.tmp.parquetStale COMPUTE STATISTICS", "9");
      verifyAnalyzeOutput("ANALYZE TABLE dfs.tmp.parquetStale COMPUTE STATISTICS",
          "Table parquetStale has not changed since last ANALYZE!");
      // Verify we recompute statistics once a new file/directory is added. Update the directory some
      // time after ANALYZE so that the timestamps are different.
      Thread.sleep(1000);
      final String Q4 = "/multilevel/parquet/1996/Q4";
      run("CREATE TABLE dfs.tmp.`parquetStale/1996/Q5` AS SELECT o_orderkey, o_custkey, o_orderstatus, " +
           "o_totalprice, o_orderdate, o_orderpriority, o_clerk, o_shippriority, o_comment from dfs.`%s`", Q4);
      verifyAnalyzeOutput("ANALYZE TABLE dfs.tmp.parquetStale COMPUTE STATISTICS", "9");
      Thread.sleep(1000);
      run("DROP TABLE dfs.tmp.`parquetStale/1996/Q5`");
      verifyAnalyzeOutput("ANALYZE TABLE dfs.tmp.parquetStale COMPUTE STATISTICS", "9");
    } finally {
      client.resetSession(ExecConstants.SLICE_TARGET);
      client.resetSession(ExecConstants.OUTPUT_FORMAT_OPTION);
    }
  }

  @Test
  public void testUseStatistics() throws Exception {
    //Test ndv/rowcount for scan
    client.alterSession(ExecConstants.SLICE_TARGET, 1);
    client.alterSession(ExecConstants.OUTPUT_FORMAT_OPTION, "parquet");
    try {
      run("CREATE TABLE dfs.tmp.employeeUseStat AS SELECT * from cp.`employee.json`");
      run("CREATE TABLE dfs.tmp.departmentUseStat AS SELECT * from cp.`department.json`");
      run("ANALYZE TABLE dfs.tmp.employeeUseStat COMPUTE STATISTICS");
      run("ANALYZE TABLE dfs.tmp.departmentUseStat COMPUTE STATISTICS");
      client.alterSession(PlannerSettings.STATISTICS_USE.getOptionName(), true);
      String query = "select employee_id from dfs.tmp.employeeUseStat where department_id = 2";
      String[] expectedPlan1 = {"Filter\\(condition.*\\).*rowcount = 96.25,.*",
              "Scan.*columns=\\[`department_id`, `employee_id`\\].*rowcount = 1155.0.*"};
      queryBuilder()
          .sql(query)
          .detailedPlanMatcher()
          .include(expectedPlan1)
          .match();

      query = "select employee_id from dfs.tmp.employeeUseStat where department_id IN (2, 5)";
      String[] expectedPlan2 = {"Filter\\(condition.*\\).*rowcount = 192.5,.*",
              "Scan.*columns=\\[`department_id`, `employee_id`\\].*rowcount = 1155.0.*"};
      queryBuilder()
          .sql(query)
          .detailedPlanMatcher()
          .include(expectedPlan2)
          .match();

      query = "select employee_id from dfs.tmp.employeeUseStat where department_id IN (2, 5) and employee_id = 5";
      String[] expectedPlan3 = {"Filter\\(condition.*\\).*rowcount = 1.0,.*",
              "Scan.*columns=\\[`department_id`, `employee_id`\\].*rowcount = 1155.0.*"};
      queryBuilder()
          .sql(query)
          .detailedPlanMatcher()
          .include(expectedPlan3)
          .match();

      query = " select emp.employee_id from dfs.tmp.employeeUseStat emp join dfs.tmp.departmentUseStat dept"
          + " on emp.department_id = dept.department_id";
      String[] expectedPlan4 = {"HashJoin\\(condition.*\\).*rowcount = 1155.0,.*",
              "Scan.*columns=\\[`department_id`, `employee_id`\\].*rowcount = 1155.0.*",
              "Scan.*columns=\\[`department_id`\\].*rowcount = 12.0.*"};
      queryBuilder()
          .sql(query)
          .detailedPlanMatcher()
          .include(expectedPlan4)
          .match();

      query = " select emp.employee_id from dfs.tmp.employeeUseStat emp join dfs.tmp.departmentUseStat dept"
              + " on emp.department_id = dept.department_id where dept.department_id = 5";
      String[] expectedPlan5 = {"HashJoin\\(condition.*\\).*rowcount = 96.25,.*",
              "Scan.*columns=\\[`department_id`, `employee_id`\\].*rowcount = 1155.0.*",
              "Scan.*columns=\\[`department_id`\\].*rowcount = 12.0.*"};
      queryBuilder()
          .sql(query)
          .detailedPlanMatcher()
          .include(expectedPlan5)
          .match();

      query = " select emp.employee_id from dfs.tmp.employeeUseStat emp join dfs.tmp.departmentUseStat dept"
              + " on emp.department_id = dept.department_id"
              + " where dept.department_id = 5 and emp.employee_id = 10";
      String[] expectedPlan6 = {"MergeJoin\\(condition.*\\).*rowcount = 1.0,.*",
              "Filter\\(condition=\\[AND\\(=\\(\\$1, 10\\), =\\(\\$0, 5\\)\\)\\]\\).*rowcount = 1.0,.*",
              "Scan.*columns=\\[`department_id`, `employee_id`\\].*rowcount = 1155.0.*",
              "Filter\\(condition=\\[=\\(\\$0, 5\\)\\]\\).*rowcount = 1.0,.*",
              "Scan.*columns=\\[`department_id`\\].*rowcount = 12.0.*"};
      queryBuilder()
          .sql(query)
          .detailedPlanMatcher()
          .include(expectedPlan6)
          .match();

      query = " select emp.employee_id, count(*)"
              + " from dfs.tmp.employeeUseStat emp"
              + " group by emp.employee_id";
      String[] expectedPlan7 = {"HashAgg\\(group=\\[\\{0\\}\\], EXPR\\$1=\\[COUNT\\(\\)\\]\\).*rowcount = 1155.0,.*",
              "Scan.*columns=\\[`employee_id`\\].*rowcount = 1155.0.*"};
      queryBuilder()
          .sql(query)
          .detailedPlanMatcher()
          .include(expectedPlan7)
          .match();

      query = " select emp.employee_id from dfs.tmp.employeeUseStat emp join dfs.tmp.departmentUseStat dept"
              + " on emp.department_id = dept.department_id "
              + " group by emp.employee_id";
      String[] expectedPlan8 = {"HashAgg\\(group=\\[\\{0\\}\\]\\).*rowcount = 730.0992454469841,.*",
              "HashJoin\\(condition.*\\).*rowcount = 1155.0,.*",
              "Scan.*columns=\\[`department_id`, `employee_id`\\].*rowcount = 1155.0.*",
              "Scan.*columns=\\[`department_id`\\].*rowcount = 12.0.*"};
      queryBuilder()
          .sql(query)
          .detailedPlanMatcher()
          .include(expectedPlan8)
          .match();

      query = "select emp.employee_id, dept.department_description"
              + " from dfs.tmp.employeeUseStat emp join dfs.tmp.departmentUseStat dept"
              + " on emp.department_id = dept.department_id "
              + " group by emp.employee_id, emp.store_id, dept.department_description "
              + " having dept.department_description = 'FINANCE'";
      String[] expectedPlan9 = {"HashAgg\\(group=\\[\\{0, 1, 2\\}\\]\\).*rowcount = 60.84160378724867.*",
              "HashJoin\\(condition.*\\).*rowcount = 96.25,.*",
              "Scan.*columns=\\[`department_id`, `employee_id`, `store_id`\\].*rowcount = 1155.0.*",
              "Filter\\(condition=\\[=\\(\\$1, 'FINANCE'\\)\\]\\).*rowcount = 1.0,.*",
              "Scan.*columns=\\[`department_id`, `department_description`\\].*rowcount = 12.0.*"};
      queryBuilder()
          .sql(query)
          .detailedPlanMatcher()
          .include(expectedPlan9)
          .match();

      query = " select emp.employee_id from dfs.tmp.employeeUseStat emp join dfs.tmp.departmentUseStat dept\n"
              + " on emp.department_id = dept.department_id "
              + " group by emp.employee_id, emp.store_id "
              + " having emp.store_id = 7";
      String[] expectedPlan10 = {"HashAgg\\(group=\\[\\{0, 1\\}\\]\\).*rowcount = 29.203969817879365.*",
              "HashJoin\\(condition.*\\).*rowcount = 46.2,.*",
              "Filter\\(condition=\\[=\\(\\$2, 7\\)\\]\\).*rowcount = 46.2,.*",
              "Scan.*columns=\\[`department_id`, `employee_id`, `store_id`\\].*rowcount = 1155.0.*",
              "Scan.*columns=\\[`department_id`\\].*rowcount = 12.0.*"};
      queryBuilder()
          .sql(query)
          .detailedPlanMatcher()
          .include(expectedPlan10)
          .match();

      query = " select emp.employee_id from dfs.tmp.employeeUseStat emp join dfs.tmp.departmentUseStat dept\n"
              + " on emp.department_id = dept.department_id "
              + " group by emp.employee_id "
              + " having emp.employee_id = 7";
      String[] expectedPlan11 = {"StreamAgg\\(group=\\[\\{0\\}\\]\\).*rowcount = 1.0.*",
              "HashJoin\\(condition.*\\).*rowcount = 1.0,.*",
              "Filter\\(condition=\\[=\\(\\$1, 7\\)\\]\\).*rowcount = 1.0.*",
              "Scan.*columns=\\[`department_id`\\].*rowcount = 12.0.*",
              "Scan.*columns=\\[`department_id`, `employee_id`\\].*rowcount = 1155.0.*"};
      queryBuilder()
          .sql(query)
          .detailedPlanMatcher()
          .include(expectedPlan11)
          .match();
    } finally {
      client.resetSession(ExecConstants.SLICE_TARGET);
      client.resetSession(ExecConstants.OUTPUT_FORMAT_OPTION);
    }
  }

  @Test
  public void testWithMetadataCaching() throws Exception {
    client.alterSession(ExecConstants.SLICE_TARGET, 1);
    client.alterSession(ExecConstants.OUTPUT_FORMAT_OPTION, "parquet");
    client.alterSession(PlannerSettings.STATISTICS_USE.getOptionName(), true);
    String tmpLocation = "/multilevel/parquet";
    try {
      // copy the data into the temporary location
      run("DROP TABLE dfs.tmp.parquetStale");
      run("CREATE TABLE dfs.tmp.parquetStale AS SELECT o_orderkey, o_custkey, o_orderstatus, " +
              "o_totalprice, o_orderdate, o_orderpriority, o_clerk, o_shippriority, o_comment from dfs.`%s`", tmpLocation);
      String query = "select count(distinct o_orderkey) from dfs.tmp.parquetStale";
      verifyAnalyzeOutput("ANALYZE TABLE dfs.tmp.parquetStale COMPUTE STATISTICS", "9");
      run("REFRESH TABLE METADATA dfs.tmp.parquetStale");
      // Verify we recompute statistics once a new file/directory is added. Update the directory some
      // time after ANALYZE so that the timestamps are different.
      Thread.sleep(1000);
      String Q4 = "/multilevel/parquet/1996/Q4";
      run("CREATE TABLE dfs.tmp.`parquetStale/1996/Q5` AS SELECT o_orderkey, o_custkey, o_orderstatus, " +
              "o_totalprice, o_orderdate, o_orderpriority, o_clerk, o_shippriority, o_comment from dfs.`%s`", Q4);
      // query should use STALE statistics
      String[] expectedStalePlan = {"StreamAgg\\(group=\\[\\{0\\}\\]\\).*rowcount = 119.0.*",
          "Scan.*rowcount = 130.0.*"};
      queryBuilder()
          .sql(query)
          .detailedPlanMatcher()
          .include(expectedStalePlan)
          .match();
      // Query should use Parquet Metadata, since statistics not available. In this case, NDV is computed as
      // 1/10*rowcount (Calcite default). Hence, NDV is 13.0 instead of the correct 119.0
      run("DROP TABLE dfs.tmp.`parquetStale/.stats.drill`");
      String[] expectedPlan1 = {"HashAgg\\(group=\\[\\{0\\}\\]\\).*rowcount = 13.0.*",
          "Scan.*rowcount = 130.0.*"};
      queryBuilder()
          .sql(query)
          .detailedPlanMatcher()
          .include(expectedPlan1)
          .match();
      // query should use the new statistics. NDV remains unaffected since we copy the Q4 into Q5
      verifyAnalyzeOutput("ANALYZE TABLE dfs.tmp.parquetStale COMPUTE STATISTICS", "9");
      String[] expectedPlan2 = {"StreamAgg\\(group=\\[\\{0\\}\\]\\).*rowcount = 119.0.*",
          "Scan.*rowcount = 130.0.*"};
      queryBuilder()
          .sql(query)
          .detailedPlanMatcher()
          .include(expectedPlan2)
          .match();
    } finally {
      run("DROP TABLE dfs.tmp.`parquetStale/1996/Q5`");
      client.resetSession(ExecConstants.SLICE_TARGET);
      client.resetSession(ExecConstants.OUTPUT_FORMAT_OPTION);
      client.resetSession(PlannerSettings.STATISTICS_USE.getOptionName());
    }
  }

  // Test basic histogram creation functionality for int, bigint, double, date, timestamp and boolean data types.
  // Test that varchar column does not fail the query but generates empty buckets.
  // Use Repeated_Count for checking number of entries, but currently we don't check actual contents of the
  // buckets since that requires enforcing a repeatable t-digest quantile that is used by histogram and is future work.
  @Test
  public void testHistogramWithDataTypes1() throws Exception {
    try {
      client.alterSession(ExecConstants.SLICE_TARGET, 1);
      client.alterSession(ExecConstants.OUTPUT_FORMAT_OPTION, "parquet");
      run("CREATE TABLE dfs.tmp.employee1 AS SELECT  employee_id, full_name, "
              + "case when gender = 'M' then cast(1 as boolean) else cast(0 as boolean) end as is_male, "
              + " cast(store_id as int) as store_id, cast(department_id as bigint) as department_id, "
              + " cast(birth_date as date) as birth_date, cast(hire_date as timestamp) as hire_date_and_time, "
              + " cast(salary as double) as salary from cp.`employee.json` where department_id > 10");
      run("ANALYZE TABLE dfs.tmp.employee1 COMPUTE STATISTICS");

      testBuilder()
              .sqlQuery("SELECT tbl.`columns`.`column` as `column`, "
                      + " repeated_count(tbl.`columns`.`histogram`.`buckets`) as num_bucket_entries "
                      + " from (select flatten(`directories`[0].`columns`) as `columns` "
                      + "  from dfs.tmp.`employee1/.stats.drill`) as tbl")
              .unOrdered()
              .baselineColumns("column", "num_bucket_entries")
              .baselineValues("`employee_id`", 11)
              .baselineValues("`full_name`", 0)
              .baselineValues("`is_male`", 3)
              .baselineValues("`store_id`", 11)
              .baselineValues("`department_id`", 8)
              .baselineValues("`birth_date`", 11)
              .baselineValues("`hire_date_and_time`", 7)
              .baselineValues("`salary`", 11)
              .go();

      // test the use of the just created histogram
      client.alterSession(PlannerSettings.STATISTICS_USE.getOptionName(), true);

      // check boundary conditions: last bucket
      String query = "select 1 from dfs.tmp.employee1 where store_id > 21";
      String[] expectedPlan1 = {"Filter\\(condition.*\\).*rowcount = 112.*,.*",
              "Scan.*columns=\\[`store_id`\\].*rowcount = 1128.0.*"};
      queryBuilder()
          .sql(query)
          .detailedPlanMatcher()
          .include(expectedPlan1)
          .match();

      query = "select 1 from dfs.tmp.employee1 where store_id < 15";
      String[] expectedPlan2 = {"Filter\\(condition.*\\).*rowcount = 699.*,.*",
              "Scan.*columns=\\[`store_id`\\].*rowcount = 1128.0.*"};
      queryBuilder()
          .sql(query)
          .detailedPlanMatcher()
          .include(expectedPlan2)
          .match();

      query = "select 1 from dfs.tmp.employee1 where store_id between 1 and 23";
      String[] expectedPlan3 = {"Filter\\(condition.*\\).*rowcount = 1090.*,.*",
        "Scan.*columns=\\[`store_id`\\].*rowcount = 1128.0.*"};
      queryBuilder()
          .sql(query)
          .detailedPlanMatcher()
          .include(expectedPlan3)
          .match();

      query = "select count(*) from dfs.tmp.employee1 where store_id between 10 and 20";
      String[] expectedPlan4 = {"Filter\\(condition.*\\).*rowcount = 5??.*,.*",
        "Scan.*columns=\\[`store_id`\\].*rowcount = 1128.0.*"};
      queryBuilder()
          .sql(query)
          .detailedPlanMatcher()
          .include(expectedPlan4)
          .match();

      // col > end_point of last bucket
      query = "select 1 from dfs.tmp.employee1 where store_id > 24";
      String[] expectedPlan5 = {"Filter\\(condition.*\\).*rowcount = 1.0,.*",
        "Scan.*columns=\\[`store_id`\\].*rowcount = 1128.0.*"};
      queryBuilder()
          .sql(query)
          .detailedPlanMatcher()
          .include(expectedPlan5)
          .match();

      // col < start_point of first bucket
      query = "select 1 from dfs.tmp.employee1 where store_id < 1";
      String[] expectedPlan6 = {"Filter\\(condition.*\\).*rowcount = 1.0,.*",
        "Scan.*columns=\\[`store_id`\\].*rowcount = 1128.0.*"};
      queryBuilder()
          .sql(query)
          .detailedPlanMatcher()
          .include(expectedPlan6)
          .match();
    } finally {
      client.resetSession(ExecConstants.SLICE_TARGET);
      client.resetSession(ExecConstants.OUTPUT_FORMAT_OPTION);
      client.resetSession(PlannerSettings.STATISTICS_USE.getOptionName());
    }
  }

  @Test
  public void testHistogramWithSubsetColumnsAndSampling() throws Exception {
    try {
      client.alterSession(ExecConstants.SLICE_TARGET, 1);
      client.alterSession(ExecConstants.OUTPUT_FORMAT_OPTION, "parquet");
      run("CREATE TABLE dfs.tmp.customer1 AS SELECT  * from cp.`tpch/customer.parquet`");
      run("ANALYZE TABLE dfs.tmp.customer1 COMPUTE STATISTICS (c_custkey, c_nationkey, c_acctbal) SAMPLE 55 PERCENT");

      testBuilder()
              .sqlQuery("SELECT tbl.`columns`.`column` as `column`, "
                      + " repeated_count(tbl.`columns`.`histogram`.`buckets`) as num_bucket_entries "
                      + " from (select flatten(`directories`[0].`columns`) as `columns` "
                      + "  from dfs.tmp.`customer1/.stats.drill`) as tbl")
              .unOrdered()
              .baselineColumns("column", "num_bucket_entries")
              .baselineValues("`c_custkey`", 11)
              .baselineValues("`c_nationkey`", 11)
              .baselineValues("`c_acctbal`", 11)
              .go();
    } finally {
      client.resetSession(ExecConstants.SLICE_TARGET);
      client.resetSession(ExecConstants.OUTPUT_FORMAT_OPTION);
    }
  }

  @Test
  public void testHistogramWithColumnsWithAllNulls() throws Exception {
    try {
      client.alterSession(ExecConstants.SLICE_TARGET, 1);
      client.alterSession(ExecConstants.OUTPUT_FORMAT_OPTION, "parquet");
      run("CREATE TABLE dfs.tmp.all_nulls AS SELECT employee_id, cast(null as int) as null_int_col, "
              + "cast(null as bigint) as null_bigint_col, cast(null as float) as null_float_col, "
              + "cast(null as double) as null_double_col, cast(null as date) as null_date_col, "
              + "cast(null as timestamp) as null_timestamp_col, cast(null as time) as null_time_col, "
              + "cast(null as boolean) as null_boolean_col "
              + "from cp.`employee.json` ");
      run("ANALYZE TABLE dfs.tmp.all_nulls COMPUTE STATISTICS ");

      testBuilder()
              .sqlQuery("SELECT tbl.`columns`.`column` as `column`, "
                      + " repeated_count(tbl.`columns`.`histogram`.`buckets`) as num_bucket_entries "
                      + " from (select flatten(`directories`[0].`columns`) as `columns` "
                      + "  from dfs.tmp.`all_nulls/.stats.drill`) as tbl")
              .unOrdered()
              .baselineColumns("column", "num_bucket_entries")
              .baselineValues("`employee_id`", 11)
              .baselineValues("`null_int_col`", 0)
              .baselineValues("`null_bigint_col`", 0)
              .baselineValues("`null_float_col`", 0)
              .baselineValues("`null_double_col`", 0)
              .baselineValues("`null_date_col`", 0)
              .baselineValues("`null_timestamp_col`", 0)
              .baselineValues("`null_time_col`", 0)
              .baselineValues("`null_boolean_col`", 0)
              .go();

    } finally {
      client.resetSession(ExecConstants.SLICE_TARGET);
      client.resetSession(ExecConstants.OUTPUT_FORMAT_OPTION);
    }
  }

  @Test
  public void testHistogramWithIntervalPredicate() throws Exception {
    try {
      client.alterSession(ExecConstants.SLICE_TARGET, 1);
      client.alterSession(ExecConstants.OUTPUT_FORMAT_OPTION, "parquet");
      run("create table dfs.tmp.orders2 as select * from cp.`tpch/orders.parquet`");
      run("analyze table dfs.tmp.orders2 compute statistics");
      client.alterSession(PlannerSettings.STATISTICS_USE.getOptionName(), true);

      String query = "select 1 from dfs.tmp.orders2 o where o.o_orderdate >= date '1996-10-01' and o.o_orderdate < date '1996-10-01' + interval '3' month";
      String[] expectedPlan1 = {"Filter\\(condition.*\\).*rowcount = 59?.*,.*", "Scan.*columns=\\[`o_orderdate`\\].*rowcount = 15000.0.*"};
      queryBuilder()
          .sql(query)
          .detailedPlanMatcher()
          .include(expectedPlan1)
          .match();
    } finally {
      client.resetSession(ExecConstants.SLICE_TARGET);
      client.resetSession(ExecConstants.OUTPUT_FORMAT_OPTION);
    }
  }

  //Helper function to verify output of ANALYZE statement
  private void verifyAnalyzeOutput(String query, String message) throws Exception {
    DirectRowSet rowSet = queryBuilder().sql(query).rowSet();
    try {
      assertEquals(1, rowSet.rowCount());

      RowSetReader reader = rowSet.reader();
      assertEquals(2, reader.columnCount());
      while (reader.next()) {
        ObjectReader column = reader.column(1);
        assertEquals(message, column.isNull() ? null : column.getObject().toString());
      }
    } finally {
      rowSet.clear();
    }
  }
}
