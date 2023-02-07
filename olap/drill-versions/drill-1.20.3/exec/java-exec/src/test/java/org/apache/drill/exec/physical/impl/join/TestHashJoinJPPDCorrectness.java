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
package org.apache.drill.exec.physical.impl.join;

import org.apache.drill.categories.SqlTest;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import java.nio.file.Paths;

@Category(SqlTest.class)
public class TestHashJoinJPPDCorrectness extends ClusterTest {

  private static final String ALTER_RUNTIME_FILTER_OPTION_COMMAND = "ALTER SESSION SET `" +
    ExecConstants.HASHJOIN_ENABLE_RUNTIME_FILTER_KEY + "` = %s";

  private static final String ALTER_RUNTIME_FILTER_WAITING_OPTION_COMMAND = "ALTER SESSION SET `" +
    ExecConstants.HASHJOIN_RUNTIME_FILTER_WAITING_ENABLE_KEY + "` = %s";

  private static final String ALTER_RUNTIME_FILTER_WAIT_TIME_OPTION_COMMAND = "ALTER SESSION SET `" +
    ExecConstants.HASHJOIN_RUNTIME_FILTER_MAX_WAITING_TIME_KEY + "` = %d";

  private static final String ALTER_RUNTIME_FILTER_ENABLE_AND_WAIT_OPTION_COMMAND =
    String.format("%s;%s;%s", ALTER_RUNTIME_FILTER_OPTION_COMMAND, ALTER_RUNTIME_FILTER_WAITING_OPTION_COMMAND,
      ALTER_RUNTIME_FILTER_WAIT_TIME_OPTION_COMMAND);

  @BeforeClass
  public static void setUp() throws Exception {
    dirTestWatcher.copyResourceToRoot(Paths.get("tpchmulti"));
    // Reduce the slice target so that there are multiple minor fragments with exchange, otherwise RuntimeFilter
    // will not be inserted in the plan
    startCluster(ClusterFixture.builder(dirTestWatcher)
      .clusterSize(2)
      .maxParallelization(1)
      .systemOption(ExecConstants.SLICE_TARGET, 10));
  }

  @After
  public void tearDown() {
    client.resetSession(ExecConstants.HASHJOIN_ENABLE_RUNTIME_FILTER_KEY);
    client.resetSession(ExecConstants.HASHJOIN_RUNTIME_FILTER_WAITING_ENABLE_KEY);
  }

  /**
   * Test to make sure runtime filter is inserted in the plan. This is to ensure that with current cluster setup and
   * system options distributed plan is generated rather than single fragment plan. Since in later case RuntimeFilter
   * will not be inserted.
   */
  @Test
  public void testRuntimeFilterPresentInPlan() throws Exception {
    String sql = "SELECT l.n_name, r.r_name FROM dfs.`tpchmulti/nation` l, dfs.`tpchmulti/region/` r where " +
      "l.n_regionkey = r.r_regionkey";
    client.alterSession(ExecConstants.HASHJOIN_ENABLE_RUNTIME_FILTER_KEY, true);
    String queryPlan = queryBuilder().sql(sql).explainText();
    assertThat("Query plan doesn't contain RuntimeFilter. This may happen if plan is not distributed and has no " +
        "exchange operator in it.", queryPlan, containsString("RuntimeFilter"));
  }

  /**
   * Verifies that result of a query with join condition doesn't changes with and without Runtime Filter for correctness
   */
  @Test
  public void testHashJoinCorrectnessWithRuntimeFilter() throws Exception {
    String sql = "SELECT l.n_name, r.r_name FROM dfs.`tpchmulti/nation` l, dfs.`tpchmulti/region/` r where " +
      "l.n_regionkey = r.r_regionkey";

    testBuilder()
      .unOrdered()
      .sqlQuery(sql)
      .optionSettingQueriesForTestQuery(ALTER_RUNTIME_FILTER_ENABLE_AND_WAIT_OPTION_COMMAND, "true", "true", 6000)
      .sqlBaselineQuery(sql)
      .optionSettingQueriesForBaseline(ALTER_RUNTIME_FILTER_ENABLE_AND_WAIT_OPTION_COMMAND, "false", "false", 1)
      .go();
  }

  /**
   * Verifies that result of query with join condition and filter condition is same with or without RuntimeFilter. In
   * this case order of join and filter condition is same for both test and baseline query
   */
  @Test
  public void testSameOrderOfJoinAndFilterConditionProduceSameResult() throws Exception {
    String testAndBaselineQuery = "SELECT count(*) FROM dfs.`tpchmulti/nation` l, dfs.`tpchmulti/region/` r where " +
      "l.n_regionkey = r.r_regionkey and r.r_name = 'AMERICA'";

    testBuilder()
      .unOrdered()
      .sqlQuery(testAndBaselineQuery)
      .optionSettingQueriesForTestQuery(ALTER_RUNTIME_FILTER_ENABLE_AND_WAIT_OPTION_COMMAND, "true", "true", 6000)
      .sqlBaselineQuery(testAndBaselineQuery)
      .optionSettingQueriesForBaseline(ALTER_RUNTIME_FILTER_ENABLE_AND_WAIT_OPTION_COMMAND, "false", "false", 1)
      .go();
  }

  /**
   * Verifies that result of query with join condition and filter condition is same with or without RuntimeFilter. In
   * this case order of join and filter condition is swapped between test and baseline query. Also the filter
   * condition is second in case of test query
   */
  @Test
  public void testDifferentOrderOfJoinAndFilterCondition_filterConditionSecond() throws Exception {
    String testQuery = "SELECT count(*) FROM dfs.`tpchmulti/nation` l, dfs.`tpchmulti/region/` r where " +
      "l.n_regionkey = r.r_regionkey and r.r_name = 'AMERICA'";
    String baselineQuery = "SELECT count(*) FROM dfs.`tpchmulti/nation` l, dfs.`tpchmulti/region/` r where " +
      "r.r_name = 'AMERICA' and l.n_regionkey = r.r_regionkey";

    testBuilder()
      .unOrdered()
      .sqlQuery(testQuery)
      .optionSettingQueriesForTestQuery(ALTER_RUNTIME_FILTER_ENABLE_AND_WAIT_OPTION_COMMAND, "true", "true", 6000)
      .sqlBaselineQuery(baselineQuery)
      .optionSettingQueriesForBaseline(ALTER_RUNTIME_FILTER_ENABLE_AND_WAIT_OPTION_COMMAND, "false", "false", 1)
      .go();
  }

  /**
   * Verifies that result of query doesn't change if filter and join condition order is different in test query with
   * RuntimeFilter and baseline query without RuntimeFilter. In this case the filter condition is first condition in
   * test query with RuntimeFilter
   */
  @Test
  public void testDifferentOrderOfJoinAndFilterCondition_filterConditionFirst() throws Exception {
    String testQuery = "SELECT count(*) FROM dfs.`tpchmulti/nation` l, dfs.`tpchmulti/region/` r where " +
      "r.r_name = 'AMERICA' and l.n_regionkey = r.r_regionkey";
    String baselineQuery = "SELECT count(*) FROM dfs.`tpchmulti/nation` l, dfs.`tpchmulti/region/` r where " +
      "l.n_regionkey = r.r_regionkey and r.r_name = 'AMERICA'";

    testBuilder()
      .unOrdered()
      .sqlQuery(testQuery)
      .optionSettingQueriesForTestQuery(ALTER_RUNTIME_FILTER_ENABLE_AND_WAIT_OPTION_COMMAND, "true", "true", 6000)
      .sqlBaselineQuery(baselineQuery)
      .optionSettingQueriesForBaseline(ALTER_RUNTIME_FILTER_ENABLE_AND_WAIT_OPTION_COMMAND, "false", "false", 1)
      .go();
  }

  /**
   * Verifies that result of query doesn't change if filter and join condition order is different in test query with
   * RuntimeFilter and baseline query with RuntimeFilter. In this case both test and baseline query has RuntimeFilter
   * in it.
   */
  @Test
  public void testDifferentOrderOfJoinAndFilterConditionAndRTF() throws Exception {
    String testQuery = "SELECT count(*) FROM dfs.`tpchmulti/nation` l, dfs.`tpchmulti/region/` r where " +
      "l.n_regionkey = r.r_regionkey and r.r_name = 'AMERICA'";
    String baselineQuery = "SELECT count(*) FROM dfs.`tpchmulti/nation` l, dfs.`tpchmulti/region/` r where " +
      "r.r_name = 'AMERICA' and l.n_regionkey = r.r_regionkey";

    testBuilder()
      .unOrdered()
      .sqlQuery(testQuery)
      .optionSettingQueriesForTestQuery(ALTER_RUNTIME_FILTER_ENABLE_AND_WAIT_OPTION_COMMAND, "true", "true", 6000)
      .sqlBaselineQuery(baselineQuery)
      .optionSettingQueriesForBaseline(ALTER_RUNTIME_FILTER_ENABLE_AND_WAIT_OPTION_COMMAND, "true", "true", 6000)
      .go();
  }
}
