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
package org.apache.drill.exec.physical.impl;

import org.apache.drill.PlanTestBase;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClientFixture;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class TestOrderedMuxExchange extends PlanTestBase {

  private final static String ORDERED_MUX_EXCHANGE = "OrderedMuxExchange";
  private final static String TOPN = "TopN";
  private final static int NUM_DEPTS = 40;
  private final static int NUM_EMPLOYEES = 1000;
  private final static int NUM_MNGRS = 1;
  private final static int NUM_IDS = 1;
  private final static String MATCH_PATTERN_ACROSS_LINES = "((?s).*[\\n\\r].*)";
  private static final String EMPT_TABLE = "empTable";

  /**
   * Generate data for two tables. Each table consists of several JSON files.
   */
  @BeforeClass
  public static void generateTestDataAndQueries() throws Exception {
    // Table consists of two columns "emp_id", "emp_name" and "dept_id"
    final File empTableLocation = dirTestWatcher.makeRootSubDir(Paths.get(EMPT_TABLE));

    // Write 100 records for each new file
    final int empNumRecsPerFile = 100;
    for(int fileIndex=0; fileIndex<NUM_EMPLOYEES/empNumRecsPerFile; fileIndex++) {
      File file = new File(empTableLocation, fileIndex + ".json");
      PrintWriter printWriter = new PrintWriter(file);
      for (int recordIndex = fileIndex*empNumRecsPerFile; recordIndex < (fileIndex+1)*empNumRecsPerFile; recordIndex++) {
        String record = String.format("{ \"emp_id\" : %d, \"emp_name\" : \"Employee %d\", \"dept_id\" : %d, \"mng_id\" : %d, \"some_id\" : %d }",
                recordIndex, recordIndex, recordIndex % NUM_DEPTS, recordIndex % NUM_MNGRS, recordIndex % NUM_IDS);
        printWriter.println(record);
      }
      printWriter.close();
    }
  }

  /**
   * Test case to verify the OrderedMuxExchange created for order by clause.
   * It checks by forcing the plan to create OrderedMuxExchange and also verifies the
   * output column is ordered.
   *
   * @throws Exception if anything goes wrong
   */

  @Test
  public void testOrderedMuxForOrderBy() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
            .maxParallelization(1)
            .configProperty(ExecConstants.SYS_STORE_PROVIDER_LOCAL_ENABLE_WRITE, true);

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      client.alterSession(ExecConstants.SLICE_TARGET, 10);
      String sql = "SELECT emp_id, emp_name FROM dfs.`empTable` e order BY emp_name, emp_id";
      client.testBuilder()
            .unOrdered()
            .optionSettingQueriesForTestQuery("alter session set `planner.slice_target` = 10;")
            .sqlQuery(sql)
            .optionSettingQueriesForBaseline("alter session set `planner.enable_ordered_mux_exchange` = false") // Use default option setting.
            .sqlBaselineQuery(sql)
            .build()
            .run();
      client.alterSession(ExecConstants.ORDERED_MUX_EXCHANGE, true);
      String explainText = client.queryBuilder().sql(sql).explainText();
      assertTrue(explainText.contains(ORDERED_MUX_EXCHANGE));
    }
  }

  /**
   * Test case to verify the OrderedMuxExchange created for window functions.
   * It checks by forcing the plan to create OrderedMuxExchange and also verifies the
   * output column is ordered.
   *
   * @throws Exception if anything goes wrong
   */

  @Test
  public void testOrderedMuxForWindowAgg() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
            .maxParallelization(1)
            .configProperty(ExecConstants.SYS_STORE_PROVIDER_LOCAL_ENABLE_WRITE, true);

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      client.alterSession(ExecConstants.SLICE_TARGET, 10);
      String sql = "SELECT emp_name, max(emp_id) over (order by emp_name) FROM dfs.`empTable` e order BY emp_name";
      client.testBuilder()
            .unOrdered()
            .optionSettingQueriesForTestQuery("alter session set `planner.slice_target` = 10;")
            .sqlQuery(sql)
            .optionSettingQueriesForBaseline("alter session set `planner.enable_ordered_mux_exchange` = false") // Use default option setting.
            .sqlBaselineQuery(sql)
            .build()
            .run();
      client.alterSession(ExecConstants.ORDERED_MUX_EXCHANGE, true);
      String explainText = client.queryBuilder().sql(sql).explainText();
      assertTrue(explainText.contains(ORDERED_MUX_EXCHANGE));
    }
  }


  /**
   * Test case to verify the OrderedMuxExchange created for order by with limit.
   * It checks that the limit is pushed down the OrderedMuxExchange.
   *
   * @throws Exception if anything goes wrong
   */
  @Test
  public void testLimitOnOrderedMux() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
            .maxParallelization(1)
            .configProperty(ExecConstants.SYS_STORE_PROVIDER_LOCAL_ENABLE_WRITE, true);

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      client.alterSession(ExecConstants.SLICE_TARGET, 10);
      String sql = "SELECT emp_id, emp_name FROM dfs.`empTable` e order BY emp_name, emp_id limit 10";
      client.testBuilder()
            .unOrdered()
            .optionSettingQueriesForTestQuery("alter session set `planner.slice_target` = 10;")
            .sqlQuery(sql)
            .optionSettingQueriesForBaseline("alter session set `planner.enable_ordered_mux_exchange` = false") // Use default option setting.
            .sqlBaselineQuery(sql)
            .build()
            .run();
      client.alterSession(ExecConstants.ORDERED_MUX_EXCHANGE, true);
      String explainText = client.queryBuilder().sql(sql).explainText();
      assertTrue(explainText.matches(String.format(MATCH_PATTERN_ACROSS_LINES + "%s"+
              MATCH_PATTERN_ACROSS_LINES +"%s"+MATCH_PATTERN_ACROSS_LINES,ORDERED_MUX_EXCHANGE, TOPN)));
    }
  }

  /**
   * Test case to verify the OrderedMuxExchange created for order by with limit and window agg.
   * It checks that the limit is pushed down the OrderedMuxExchange.
   *
   * @throws Exception if anything goes wrong
   */
  @Test
  public void testLimitOnOrderedMuxWindow() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
            .maxParallelization(1)
            .configProperty(ExecConstants.SYS_STORE_PROVIDER_LOCAL_ENABLE_WRITE, true);

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      client.alterSession(ExecConstants.SLICE_TARGET, 10);
      String sql = "SELECT emp_name, max(emp_id) over (order by emp_name) FROM dfs.`empTable` e order BY emp_name limit 10";
      client.testBuilder()
              .unOrdered()
              .optionSettingQueriesForTestQuery("alter session set `planner.slice_target` = 10;")
              .sqlQuery(sql)
              .optionSettingQueriesForBaseline("alter session set `planner.enable_ordered_mux_exchange` = false") // Use default option setting.
              .sqlBaselineQuery(sql)
              .build()
              .run();
      client.alterSession(ExecConstants.ORDERED_MUX_EXCHANGE, true);
      String explainText = client.queryBuilder().sql(sql).explainText();
      assertTrue(explainText.matches(String.format(MATCH_PATTERN_ACROSS_LINES + "%s"+
              MATCH_PATTERN_ACROSS_LINES +"%s"+MATCH_PATTERN_ACROSS_LINES,ORDERED_MUX_EXCHANGE, TOPN)));
    }
  }
}
