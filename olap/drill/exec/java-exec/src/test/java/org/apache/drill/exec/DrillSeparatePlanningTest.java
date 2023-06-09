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
package org.apache.drill.exec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;
import java.util.List;

import org.apache.drill.test.ClientFixture;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.QueryBuilder.QuerySummary;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetReader;
import org.apache.drill.categories.PlannerTest;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.exec.proto.BitControl.PlanFragment;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.proto.UserBitShared.QueryType;
import org.apache.drill.exec.proto.UserProtos.QueryPlanFragments;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

/**
 * Class to test different planning use cases (separate from query execution)
 * (Though, despite the above, this test does execute queries.)
 */
@Category({SlowTest.class, PlannerTest.class})
public class DrillSeparatePlanningTest extends ClusterTest {

  @BeforeClass
  public static void setupFiles() {
    dirTestWatcher.copyResourceToRoot(Paths.get("multilevel", "json"));
    dirTestWatcher.copyResourceToRoot(Paths.get("multilevel", "csv"));
  }

  @Before
  public void testSetup() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
        .clusterSize(2);
    startCluster(builder);
  }

  @Test(timeout=60_000)
  public void testSingleFragmentQuery() throws Exception {
    final String query = "SELECT * FROM cp.`employee.json` where employee_id > 1 and employee_id < 1000";

    QueryPlanFragments planFragments = getFragmentsHelper(query);

    assertNotNull(planFragments);

    assertEquals(1, planFragments.getFragmentsCount());
    assertTrue(planFragments.getFragments(0).getLeafFragment());

    QuerySummary summary = client.queryBuilder().plan(planFragments.getFragmentsList()).run();
    assertEquals(997, summary.recordCount());
  }

  @Test(timeout=60_000)
  public void testMultiMinorFragmentSimpleQuery() throws Exception {
    final String query = "SELECT o_orderkey FROM dfs.`multilevel/json`";

    QueryPlanFragments planFragments = getFragmentsHelper(query);

    assertNotNull(planFragments);
    assertTrue((planFragments.getFragmentsCount() > 1));

    for (PlanFragment planFragment : planFragments.getFragmentsList()) {
      assertTrue(planFragment.getLeafFragment());
    }

    int rowCount = getResultsHelper(planFragments);
    assertEquals(120, rowCount);
  }

  @Test(timeout=60_000)
  public void testMultiMinorFragmentComplexQuery() throws Exception {
    final String query = "SELECT dir0, sum(o_totalprice) FROM dfs.`multilevel/json` group by dir0 order by dir0";

    QueryPlanFragments planFragments = getFragmentsHelper(query);

    assertNotNull(planFragments);
    assertTrue((planFragments.getFragmentsCount() > 1));

    for ( PlanFragment planFragment : planFragments.getFragmentsList()) {
      assertTrue(planFragment.getLeafFragment());
    }

    int rowCount = getResultsHelper(planFragments);
    assertEquals(8, rowCount);
  }

  @Test(timeout=60_000)
  public void testPlanningNoSplit() throws Exception {
    final String query = "SELECT dir0, sum(o_totalprice) FROM dfs.`multilevel/json` group by dir0 order by dir0";

    client.alterSession("planner.slice_target", 1);
    try {
      final QueryPlanFragments planFragments = client.planQuery(query);

      assertNotNull(planFragments);
      assertTrue((planFragments.getFragmentsCount() > 1));

      PlanFragment rootFragment = planFragments.getFragments(0);
      assertFalse(rootFragment.getLeafFragment());

      QuerySummary summary = client.queryBuilder().plan(planFragments.getFragmentsList()).run();
      assertEquals(3, summary.recordCount());
    }
    finally {
      client.resetSession("planner.slice_target");
    }
  }

  @Test(timeout=60_000)
  public void testPlanningNegative() throws Exception {
    final String query = "SELECT dir0, sum(o_totalprice) FROM dfs.`multilevel/json` group by dir0 order by dir0";

    // LOGICAL is not supported
    final QueryPlanFragments planFragments = client.planQuery(QueryType.LOGICAL, query, false);

    assertNotNull(planFragments);
    assertNotNull(planFragments.getError());
    assertTrue(planFragments.getFragmentsCount()==0);
  }

  @Test(timeout=60_000)
  public void testPlanning() throws Exception {
    final String query = "SELECT dir0, columns[3] FROM dfs.`multilevel/csv` order by dir0";

    client.alterSession("planner.slice_target", 1);
    try {
      // Original version, but no reason to dump output to test results.
//      long rows = client.queryBuilder().sql(query).print(Format.TSV, VectorUtil.DEFAULT_COLUMN_WIDTH);
      QuerySummary summary = client.queryBuilder().sql(query).run();
      assertEquals(120, summary.recordCount());
    }
    finally {
      client.resetSession("planner.slice_target");
    }
  }

  private QueryPlanFragments getFragmentsHelper(final String query) {
    client.alterSession("planner.slice_target", 1);
    try {
      QueryPlanFragments planFragments = client.planQuery(QueryType.SQL, query, true);
      return planFragments;
    }
    finally {
      client.resetSession("planner.slice_target");
    }
  }

  private int getResultsHelper(final QueryPlanFragments planFragments) throws Exception {
    long totalRows = 0;
    for (PlanFragment fragment : planFragments.getFragmentsList()) {
      DrillbitEndpoint assignedNode = fragment.getAssignment();
      ClientFixture fragmentClient = cluster.client(assignedNode.getAddress(), assignedNode.getUserPort());

      RowSet rowSet = fragmentClient.queryBuilder().sql("select hostname, user_port from sys.drillbits where `current`=true").rowSet();
      assertEquals(1, rowSet.rowCount());
      RowSetReader reader = rowSet.reader();
      assertTrue(reader.next());
      String host = reader.scalar("hostname").getString();
      int port = reader.scalar("user_port").getInt();
      rowSet.clear();

      assertEquals(assignedNode.getAddress(), host);
      assertEquals(assignedNode.getUserPort(), port);

      List<PlanFragment> fragmentList = Lists.newArrayList();
      fragmentList.add(fragment);
      QuerySummary summary = fragmentClient.queryBuilder().plan(fragmentList).run();
      totalRows += summary.recordCount();
      fragmentClient.close();
    }
    return Math.toIntExact(totalRows);
  }
}
