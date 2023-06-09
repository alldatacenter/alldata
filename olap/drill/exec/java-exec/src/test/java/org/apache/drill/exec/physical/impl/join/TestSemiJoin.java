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

import org.apache.drill.categories.OperatorTest;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.test.ClientFixture;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import org.junit.experimental.categories.Category;

@Category({SlowTest.class, OperatorTest.class})
public class TestSemiJoin extends BaseTestQuery {
  @Test
  public void testInClauseToSemiJoin() throws Exception {
    String sql = "select employee_id, full_name from cp.`employee.json` where employee_id in (select employee_id from cp.`employee.json` )";

    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
            .setOptionDefault(PlannerSettings.SEMIJOIN.getOptionName(), true);

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      String queryPlan = client.queryBuilder().sql(sql).explainText();
      assertTrue(queryPlan.contains("semi-join: =[true]"));
    }
  }

  @Test
  public void testInClauseWithSemiJoinDisabled() throws Exception {
    String sql = "select employee_id, full_name from cp.`employee.json` where employee_id in (select employee_id from cp.`employee.json` )";

    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
            .setOptionDefault(PlannerSettings.SEMIJOIN.getOptionName(), false);

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      String queryPlan = client.queryBuilder().sql(sql).explainText();
      assertTrue(!queryPlan.contains("semi-join: =[true]"));
    }
  }

  @Test
  public void testSmallInClauseToSemiJoin() throws Exception {
    String sql = "select employee_id, full_name from cp.`employee.json` " +
            "where employee_id in (351, 352, 353, 451, 452, 453)";

    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
            .setOptionDefault(PlannerSettings.SEMIJOIN.getOptionName(), true);

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      String queryPlan = client.queryBuilder().sql(sql).explainText();
      assertTrue(!queryPlan.contains("semi-join: =[true]"));
    }
  }

  @Test
  public void testLargeInClauseToSemiJoin() throws Exception {
    String sql = "select employee_id, full_name from cp.`employee.json` " +
            "where employee_id in (351, 352, 353, 451, 452, 453, 551, 552, 553, 651, 652, 653, 751, 752, 753, 851, 852, 853, 951, 952, 953)";

    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
            .setOptionDefault(PlannerSettings.SEMIJOIN.getOptionName(), true);

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      String queryPlan = client.queryBuilder().sql(sql).explainText();
      assertTrue(queryPlan.contains("semi-join: =[true]"));
    }
  }

  @Test
  public void testStarWithInClauseToSemiJoin() throws Exception {
    String sql = "select * from cp.`employee.json` where employee_id in (select employee_id from cp.`employee.json` )";

    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
            .setOptionDefault(PlannerSettings.SEMIJOIN.getOptionName(), true);

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      String queryPlan = client.queryBuilder().sql(sql).explainText();
      assertTrue(queryPlan.contains("semi-join: =[true]"));
    }
  }

  @Test
  public void testMultiColumnInClauseWithSemiJoin() throws Exception {
    String sql = "select * from cp.`employee.json` where (employee_id, full_name) in (select employee_id, full_name from cp.`employee.json` )";

    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
            .setOptionDefault(PlannerSettings.SEMIJOIN.getOptionName(), true);

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      String queryPlan = client.queryBuilder().sql(sql).explainText();
      assertTrue(queryPlan.contains("semi-join: =[true]"));
    }
  }
}
