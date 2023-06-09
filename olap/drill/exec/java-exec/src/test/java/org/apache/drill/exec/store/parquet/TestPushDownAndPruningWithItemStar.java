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
package org.apache.drill.exec.store.parquet;

import org.apache.drill.PlanTestBase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.apache.drill.exec.util.StoragePluginTestUtils.DFS_TMP_SCHEMA;

public class TestPushDownAndPruningWithItemStar extends PlanTestBase {

  private static final String TABLE_NAME = "order_ctas";

  @BeforeClass
  public static void setup() throws Exception {
    test("create table `%s`.`%s/t1` as select o_orderkey, o_custkey, cast(o_orderdate as date) as o_orderdate from cp.`tpch/orders.parquet` " +
        "where o_orderdate between date '1992-01-01' and date '1992-01-03'", DFS_TMP_SCHEMA, TABLE_NAME);
    test("create table `%s`.`%s/t2` as select o_orderkey, o_custkey, cast(o_orderdate as date) as o_orderdate from cp.`tpch/orders.parquet` " +
        "where o_orderdate between date '1992-01-04' and date '1992-01-06'", DFS_TMP_SCHEMA, TABLE_NAME);
    test("create table `%s`.`%s/t3` as select o_orderkey, o_custkey, cast(o_orderdate as date) as o_orderdate from cp.`tpch/orders.parquet` " +
        "where o_orderdate between date '1992-01-07' and date '1992-01-09'", DFS_TMP_SCHEMA, TABLE_NAME);
  }

  @AfterClass
  public static void tearDown() throws Exception {
    test("drop table if exists `%s`.`%s`", DFS_TMP_SCHEMA, TABLE_NAME);
  }

  @Test
  public void testPushProjectIntoScanWithGroupByClause() throws Exception {
    String query = String.format("select o_orderdate, count(*) from (select * from `%s`.`%s`) group by o_orderdate", DFS_TMP_SCHEMA, TABLE_NAME);

    String[] expectedPlan = {"numFiles=3", "numRowGroups=3", "usedMetadataFile=false", "columns=\\[`o_orderdate`\\]"};
    String[] excludedPlan = {};

    PlanTestBase.testPlanMatchingPatterns(query, expectedPlan, excludedPlan);

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .sqlBaselineQuery("select o_orderdate, count(*) from `%s`.`%s` group by o_orderdate", DFS_TMP_SCHEMA, TABLE_NAME)
        .go();
  }

  @Test
  public void testPushProjectIntoScanWithExpressionInProject() throws Exception {
    String query = String.format("select o_custkey + o_orderkey from (select * from `%s`.`%s`)", DFS_TMP_SCHEMA, TABLE_NAME);

    String[] expectedPlan = {"numFiles=3", "numRowGroups=3", "usedMetadataFile=false", "columns=\\[`o_custkey`, `o_orderkey`\\]"};
    String[] excludedPlan = {};

    PlanTestBase.testPlanMatchingPatterns(query, expectedPlan, excludedPlan);

    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .sqlBaselineQuery("select o_custkey + o_orderkey from `%s`.`%s`", DFS_TMP_SCHEMA, TABLE_NAME)
      .go();
  }

  @Test
  public void testPushProjectIntoScanWithExpressionInFilter() throws Exception {
    String query = String.format("select o_orderdate from (select * from `%s`.`%s`) where o_custkey + o_orderkey < 5", DFS_TMP_SCHEMA, TABLE_NAME);

    String[] expectedPlan = {"numFiles=3", "numRowGroups=3", "usedMetadataFile=false",
        "columns=\\[`o_orderdate`, `o_custkey`, `o_orderkey`\\]"};
    String[] excludedPlan = {};

    PlanTestBase.testPlanMatchingPatterns(query, expectedPlan, excludedPlan);

    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .sqlBaselineQuery("select o_orderdate from `%s`.`%s` where o_custkey + o_orderkey < 5", DFS_TMP_SCHEMA, TABLE_NAME)
      .go();
  }

  @Test
  public void testPushProjectIntoScanWithComplexInProject() throws Exception {
    String query = "select t.user_info.cust_id, t.user_info.device, t.marketing_info.camp_id, t.marketing_info.keywords[2] " +
      "from (select * from cp.`store/parquet/complex/complex.parquet`) t";

    String[] expectedPlan = {"numFiles=1", "numRowGroups=1", "usedMetadataFile=false",
      "columns=\\[`user_info`.`cust_id`, `user_info`.`device`, `marketing_info`.`camp_id`, `marketing_info`.`keywords`\\[2\\]\\]"};
    String[] excludedPlan = {};

    PlanTestBase.testPlanMatchingPatterns(query, expectedPlan, excludedPlan);

    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .sqlBaselineQuery("select t.user_info.cust_id, t.user_info.device, t.marketing_info.camp_id, t.marketing_info.keywords[2] " +
        "from cp.`store/parquet/complex/complex.parquet` t", DFS_TMP_SCHEMA, TABLE_NAME)
      .go();
  }

  @Test
  public void testPushProjectIntoScanWithComplexInFilter() throws Exception {
    String query = "select t.trans_id from (select * from cp.`store/parquet/complex/complex.parquet`) t " +
      "where t.user_info.cust_id > 28 and t.user_info.device = 'IOS5' and t.marketing_info.camp_id > 5 and t.marketing_info.keywords[2] is not null";

    String[] expectedPlan = {"numFiles=1", "numRowGroups=1", "usedMetadataFile=false",
      "columns=\\[`trans_id`, `user_info`.`cust_id`, `user_info`.`device`, `marketing_info`.`camp_id`, `marketing_info`.`keywords`\\[2\\]\\]"};
    String[] excludedPlan = {};

    PlanTestBase.testPlanMatchingPatterns(query, expectedPlan, excludedPlan);

    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .sqlBaselineQuery("select t.trans_id from cp.`store/parquet/complex/complex.parquet` t " +
        "where t.user_info.cust_id > 28 and t.user_info.device = 'IOS5' and t.marketing_info.camp_id > 5 and t.marketing_info.keywords[2] is not null",
        DFS_TMP_SCHEMA, TABLE_NAME)
      .go();
  }

  @Test
  public void testProjectIntoScanWithNestedStarSubQuery() throws Exception {
    String query = String.format("select *, o_orderdate from (select * from `%s`.`%s`)", DFS_TMP_SCHEMA, TABLE_NAME);

    String[] expectedPlan = {"numFiles=3", "numRowGroups=3", "usedMetadataFile=false", "columns=\\[`\\*\\*`, `o_orderdate`\\]"};
    String[] excludedPlan = {};

    PlanTestBase.testPlanMatchingPatterns(query, expectedPlan, excludedPlan);

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .sqlBaselineQuery("select *, o_orderdate from `%s`.`%s`", DFS_TMP_SCHEMA, TABLE_NAME)
        .go();
  }

  @Test
  public void testProjectIntoScanWithSeveralNestedStarSubQueries() throws Exception {
    String subQuery = String.format("select * from `%s`.`%s`", DFS_TMP_SCHEMA, TABLE_NAME);
    String query = String.format("select o_custkey + o_orderkey from (select * from (select * from (%s)))", subQuery);

    String[] expectedPlan = {"numFiles=3", "numRowGroups=3", "usedMetadataFile=false", "columns=\\[`o_custkey`, `o_orderkey`\\]"};
    String[] excludedPlan = {};

    PlanTestBase.testPlanMatchingPatterns(query, expectedPlan, excludedPlan);

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .sqlBaselineQuery("select o_custkey + o_orderkey from `%s`.`%s`", DFS_TMP_SCHEMA, TABLE_NAME)
        .go();
  }

  @Test
  public void testDirectoryPruning() throws Exception {
    String query = String.format("select * from (select * from `%s`.`%s`) where dir0 = 't1'", DFS_TMP_SCHEMA, TABLE_NAME);

    String[] expectedPlan = {"numFiles=1", "numRowGroups=1", "usedMetadataFile=false", "columns=\\[`\\*\\*`, `dir0`\\]"};
    String[] excludedPlan = {};

    PlanTestBase.testPlanMatchingPatterns(query, expectedPlan, excludedPlan);

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .sqlBaselineQuery("select * from `%s`.`%s` where dir0 = 't1'", DFS_TMP_SCHEMA, TABLE_NAME)
        .go();
  }

  @Test
  public void testDirectoryPruningWithNestedStarSubQuery() throws Exception {
    String subQuery = String.format("select * from `%s`.`%s`", DFS_TMP_SCHEMA, TABLE_NAME);
    String query = String.format("select * from (select * from (select * from (%s))) where dir0 = 't1'", subQuery);

    String[] expectedPlan = {"numFiles=1", "numRowGroups=1", "usedMetadataFile=false", "columns=\\[`\\*\\*`, `dir0`\\]"};
    String[] excludedPlan = {};

    PlanTestBase.testPlanMatchingPatterns(query, expectedPlan, excludedPlan);

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .sqlBaselineQuery("select * from `%s`.`%s` where dir0 = 't1'", DFS_TMP_SCHEMA, TABLE_NAME)
        .go();
  }

  @Test
  public void testDirectoryPruningWithNestedStarSubQueryAndAdditionalColumns() throws Exception {
    String subQuery = String.format("select * from `%s`.`%s`", DFS_TMP_SCHEMA, TABLE_NAME);
    String query = String.format("select * from (select * from (select *, `o_orderdate` from (%s))) where dir0 = 't1'", subQuery);

    String[] expectedPlan = {"numFiles=1", "numRowGroups=1", "usedMetadataFile=false", "columns=\\[`\\*\\*`, `o_orderdate`, `dir0`\\]"};
    String[] excludedPlan = {};

    PlanTestBase.testPlanMatchingPatterns(query, expectedPlan, excludedPlan);

    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .sqlBaselineQuery("select *, `o_orderdate` from `%s`.`%s` where dir0 = 't1'", DFS_TMP_SCHEMA, TABLE_NAME)
      .go();
  }

  @Test
  public void testFilterPushDownSingleCondition() throws Exception {
    String query = String.format("select * from (select * from `%s`.`%s`) where o_orderdate = date '1992-01-01'", DFS_TMP_SCHEMA, TABLE_NAME);

    String[] expectedPlan = {"numFiles=1", "numRowGroups=1", "usedMetadataFile=false",
        "columns=\\[`\\*\\*`, `o_orderdate`\\]"};
    String[] excludedPlan = {};

    PlanTestBase.testPlanMatchingPatterns(query, expectedPlan, excludedPlan);

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .sqlBaselineQuery("select * from `%s`.`%s` where o_orderdate = date '1992-01-01'", DFS_TMP_SCHEMA, TABLE_NAME)
        .go();
  }

  @Test
  public void testFilterPushDownMultipleConditions() throws Exception {
    String query = String.format("select * from (select * from `%s`.`%s`) where o_orderdate = date '1992-01-01' or o_orderdate = date '1992-01-09'",
        DFS_TMP_SCHEMA, TABLE_NAME);

    String[] expectedPlan = {"numFiles=2", "numRowGroups=2", "usedMetadataFile=false", "columns=\\[`\\*\\*`, `o_orderdate`\\]"};
    String[] excludedPlan = {};

    PlanTestBase.testPlanMatchingPatterns(query, expectedPlan, excludedPlan);

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .sqlBaselineQuery("select * from `%s`.`%s` where o_orderdate = date '1992-01-01' or o_orderdate = date '1992-01-09'",
            DFS_TMP_SCHEMA, TABLE_NAME)
        .go();
  }

  @Test
  public void testFilterPushDownWithSeveralNestedStarSubQueries() throws Exception {
    String subQuery = String.format("select * from `%s`.`%s`", DFS_TMP_SCHEMA, TABLE_NAME);
    String query = String.format("select * from (select * from (select * from (%s))) where o_orderdate = date '1992-01-01'", subQuery);

    String[] expectedPlan = {"numFiles=1", "numRowGroups=1", "usedMetadataFile=false",
        "columns=\\[`\\*\\*`, `o_orderdate`\\]"};
    String[] excludedPlan = {};

    PlanTestBase.testPlanMatchingPatterns(query, expectedPlan, excludedPlan);

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .sqlBaselineQuery("select * from `%s`.`%s` where o_orderdate = date '1992-01-01'", DFS_TMP_SCHEMA, TABLE_NAME)
        .go();
  }

  @Test
  public void testFilterPushDownWithSeveralNestedStarSubQueriesWithAdditionalColumns() throws Exception {
    String subQuery = String.format("select * from `%s`.`%s`", DFS_TMP_SCHEMA, TABLE_NAME);
    String query = String.format("select * from (select * from (select *, o_custkey from (%s))) where o_orderdate = date '1992-01-01'", subQuery);

    String[] expectedPlan = {"numFiles=1", "numRowGroups=1", "usedMetadataFile=false",
        "columns=\\[`\\*\\*`, `o_custkey`, `o_orderdate`\\]"};
    String[] excludedPlan = {};

    PlanTestBase.testPlanMatchingPatterns(query, expectedPlan, excludedPlan);

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .sqlBaselineQuery("select *, o_custkey from `%s`.`%s` where o_orderdate = date '1992-01-01'", DFS_TMP_SCHEMA, TABLE_NAME)
        .go();
  }
}
