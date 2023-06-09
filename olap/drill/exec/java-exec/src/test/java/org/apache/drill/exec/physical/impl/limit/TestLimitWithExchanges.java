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
package org.apache.drill.exec.physical.impl.limit;

import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.categories.OperatorTest;
import org.apache.drill.PlanTestBase;
import org.apache.drill.exec.ExecConstants;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

@Category(OperatorTest.class)
public class TestLimitWithExchanges extends BaseTestQuery {
  @BeforeClass
  public static void setupTestFiles() {
    dirTestWatcher.copyResourceToRoot(Paths.get("multilevel", "json"));
    dirTestWatcher.copyResourceToRoot(Paths.get("tpchmulti", "region"));
    dirTestWatcher.copyResourceToRoot(Paths.get("tpchmulti", "nation"));
  }

  @Test
  public void testLimitWithExchanges() throws Exception{
    testPhysicalFromFile("limit/limit_exchanges.json");
  }

  @Test
  public void testPushLimitPastUnionExchange() throws Exception {
    // Push limit past through UnionExchange.
    try {
      test("alter session set `planner.slice_target` = 1");
      final String[] excludedPlan = {};

      // case 1. single table query.
      final String sql = "select * from dfs.`multilevel/json` limit 1 offset 2";
      final String[] expectedPlan ={"(?s)Limit\\(offset=\\[2\\], fetch=\\[1\\].*UnionExchange.*Limit\\(fetch=\\[3\\]\\).*Scan"};
      testLimitHelper(sql, expectedPlan, excludedPlan, 1);

      final String sql2 = "select * from dfs.`multilevel/json` limit 1 offset 0";
      final String[] expectedPlan2 = {"(?s)Limit\\(offset=\\[0\\], fetch=\\[1\\].*UnionExchange.*Limit\\(fetch=\\[1\\]\\).*Scan"};
      testLimitHelper(sql2, expectedPlan2, excludedPlan, 1);

      final String sql3 = "select * from dfs.`multilevel/json` limit 1";
      final String[] expectedPlan3 = {"(?s)Limit\\(fetch=\\[1\\].*UnionExchange.*Limit\\(fetch=\\[1\\]\\).*Scan"};
      testLimitHelper(sql3, expectedPlan3, excludedPlan, 1);

      // case 2: join query.
      final String sql4 = "select * from dfs.`tpchmulti/region` r, dfs.`tpchmulti/nation` n " +
          "where r.r_regionkey = n.n_regionkey limit 1 offset 2";

      final String[] expectedPlan4 = {"(?s)Limit\\(offset=\\[2\\], fetch=\\[1\\].*UnionExchange.*Limit\\(fetch=\\[3\\]\\).*Join"};

      testLimitHelper(sql4, expectedPlan4, excludedPlan, 1);

      final String sql5 = "select * from dfs.`tpchmulti/region` r,  dfs.`tpchmulti/nation` n " +
              "where r.r_regionkey = n.n_regionkey limit 1";

      final String[] expectedPlan5 = {"(?s)Limit\\(fetch=\\[1\\].*UnionExchange.*Limit\\(fetch=\\[1\\]\\).*Join"};
      testLimitHelper(sql5, expectedPlan5, excludedPlan, 1);
    } finally {
      resetSessionOption(ExecConstants.SLICE_TARGET);
    }
  }

  @Test
  public void testNegPushLimitPastUnionExchange() throws Exception {
    // Negative case: should not push limit past through UnionExchange.
    try {
      test("alter session set `planner.slice_target` = 1");
      final String[] expectedPlan ={};

      // case 1. Only "offset", but no "limit" : should not push "limit" down.
      // case 2. "limit" is higher than # of rowcount in table : should not push "limit" down.
      final String sql2 = "select * from dfs.`tpchmulti/region` limit 100";
      final String[] excludedPlan2 = {"(?s)Limit\\(fetch=\\[100\\].*UnionExchange.*Limit.*Scan"};

      testLimitHelper(sql2, expectedPlan, excludedPlan2, 5);
    } finally {
      resetSessionOption(ExecConstants.SLICE_TARGET);
    }
  }

  @Test
  public void testLimitImpactExchange() throws Exception {
    try {
      test("alter session set `planner.slice_target` = 5" );

      // nation has 3 files, total 25 rows.
      // Given slice_target = 5, if # of rows to fetch is < 5 : do NOT insert Exchange, and the query should run in single fragment.
      //                         if # of row to fetch is >= 5:  do insert exchange, and query should run in multiple fragments.
      final String sql = "select * from dfs.`tpchmulti/nation` limit 2";  // Test Limit_On_Scan rule.
      final String sql2 = "select n_nationkey + 1000 from dfs.`tpchmulti/nation` limit 2"; // Test Limit_On_Project rule.
      final String [] expectedPlan = {};
      final String [] excludedPlan = {"UnionExchange"};

      testLimitHelper(sql, expectedPlan, excludedPlan, 2);
      testLimitHelper(sql2, expectedPlan, excludedPlan, 2);

      final String sql3 = "select * from dfs.`tpchmulti/nation` limit 10"; // Test Limit_On_Scan rule.
      final String sql4 = "select n_nationkey + 1000 from dfs.`tpchmulti/nation` limit 10"; // Test Limit_On_Project rule.

      final String [] expectedPlan2 = {"UnionExchange"};
      final String [] excludedPlan2 = {};

      testLimitHelper(sql3, expectedPlan2, excludedPlan2, 10);
      testLimitHelper(sql4, expectedPlan2, excludedPlan2, 10);
    } finally {
      resetSessionOption(ExecConstants.SLICE_TARGET);
    }
  }

  @Test
  public void TestLimitAllOnParquet() throws Exception {
    final String query = "select t.n_nationkey from cp.`tpch/nation.parquet` t limit all offset 5";
    final String [] expectedPlan = {};
    final String [] excludedPlan = {"UnionExchange"};

    testLimitHelper(query, expectedPlan, excludedPlan, 20);
  }

  private void testLimitHelper(final String sql, final String[] expectedPlan, final String[] excludedPattern, int expectedRecordCount) throws Exception {
    // Validate the plan
    PlanTestBase.testPlanMatchingPatterns(sql, expectedPlan, excludedPattern);

    // Validate row count
    final int actualRecordCount = testSql(sql);
    assertEquals(String.format("Received unexpected number of rows in output: expected=%d, received=%s", expectedRecordCount, actualRecordCount), expectedRecordCount, actualRecordCount);
  }
}
