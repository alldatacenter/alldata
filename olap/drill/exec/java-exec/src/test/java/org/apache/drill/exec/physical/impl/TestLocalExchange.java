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

import org.apache.drill.exec.planner.fragment.DefaultQueryParallelizer;
import org.apache.drill.shaded.guava.com.google.common.collect.ArrayListMultimap;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

import org.apache.commons.lang3.StringUtils;
import org.apache.drill.PlanTestBase;
import org.apache.drill.test.TestBuilder;
import org.apache.drill.exec.physical.base.Exchange;
import org.apache.drill.exec.physical.config.UnorderedDeMuxExchange;
import org.apache.drill.exec.physical.config.HashToRandomExchange;
import org.apache.drill.exec.planner.PhysicalPlanReader;
import org.apache.drill.exec.planner.fragment.Fragment;
import org.apache.drill.exec.planner.fragment.Fragment.ExchangeFragmentPair;
import org.apache.drill.exec.planner.fragment.PlanningSet;
import org.apache.drill.exec.planner.fragment.SimpleParallelizer;
import org.apache.drill.exec.pop.PopUnitTestBase;
import org.apache.drill.exec.proto.BitControl.PlanFragment;
import org.apache.drill.exec.proto.BitControl.QueryContextInformation;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.rpc.user.UserSession;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.server.options.OptionList;
import org.apache.drill.exec.util.Utilities;
import org.apache.drill.exec.work.QueryWorkUnit;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.List;

import static org.apache.drill.exec.planner.physical.HashPrelUtil.HASH_EXPR_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
/**
 * This test starts a Drill cluster with CLUSTER_SIZE nodes and generates data for test tables.
 *
 * Tests queries involve HashToRandomExchange (group by and join) and test the following.
 *   1. Plan that has mux and demux exchanges inserted
 *   2. Run the query and check the output record count
 *   3. Take the plan we got in (1), use SimpleParallelizer to get PlanFragments and test that the number of
 *   partition senders in a major fragment is not more than the number of Drillbit nodes in cluster and there exists
 *   at most one partition sender per Drillbit.
 */
public class TestLocalExchange extends PlanTestBase {
  private static final int CLUSTER_SIZE = 3;
  private static final String MUX_EXCHANGE = "\"unordered-mux-exchange\"";
  private static final String DEMUX_EXCHANGE = "\"unordered-demux-exchange\"";
  private static final String MUX_EXCHANGE_CONST = "unordered-mux-exchange";
  private static final String DEMUX_EXCHANGE_CONST = "unordered-demux-exchange";
  private static final String HASH_EXCHANGE = "hash-to-random-exchange";
  private static final String EMPT_TABLE = "empTable";
  private static final String DEPT_TABLE = "deptTable";
  private static final UserSession USER_SESSION = UserSession.Builder.newBuilder()
      .withCredentials(UserBitShared.UserCredentials.newBuilder().setUserName("foo").build())
      .build();

  private static final SimpleParallelizer PARALLELIZER = new DefaultQueryParallelizer(
      true,
      1 /*parallelizationThreshold (slice_count)*/,
      6 /*maxWidthPerNode*/,
      1000 /*maxGlobalWidth*/,
      1.2 /*affinityFactor*/);

  private final static int NUM_DEPTS = 40;
  private final static int NUM_EMPLOYEES = 1000;
  private final static int NUM_MNGRS = 1;
  private final static int NUM_IDS = 1;

  private static String groupByQuery;
  private static String joinQuery;

  private static String[] joinQueryBaselineColumns;
  private static String[] groupByQueryBaselineColumns;

  private static List<Object[]> groupByQueryBaselineValues;
  private static List<Object[]> joinQueryBaselineValues;

  @BeforeClass
  public static void setupClusterSize() {
    updateTestCluster(CLUSTER_SIZE, null);
  }

  /**
   * Generate data for two tables. Each table consists of several JSON files.
   */
  @BeforeClass
  public static void generateTestDataAndQueries() throws Exception {
    // Table 1 consists of two columns "emp_id", "emp_name" and "dept_id"
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

    // Table 2 consists of two columns "dept_id" and "dept_name"
    final File deptTableLocation = dirTestWatcher.makeRootSubDir(Paths.get(DEPT_TABLE));

    // Write 4 records for each new file
    final int deptNumRecsPerFile = 4;
    for(int fileIndex=0; fileIndex<NUM_DEPTS/deptNumRecsPerFile; fileIndex++) {
      File file = new File(deptTableLocation, fileIndex + ".json");
      PrintWriter printWriter = new PrintWriter(file);
      for (int recordIndex = fileIndex*deptNumRecsPerFile; recordIndex < (fileIndex+1)*deptNumRecsPerFile; recordIndex++) {
        String record = String.format("{ \"dept_id\" : %d, \"dept_name\" : \"Department %d\" }",
            recordIndex, recordIndex);
        printWriter.println(record);
      }
      printWriter.close();
    }

    // Initialize test queries
    groupByQuery = String.format("SELECT dept_id, count(*) as numEmployees FROM dfs.`%s` GROUP BY dept_id", EMPT_TABLE);
    joinQuery = String.format("SELECT e.emp_name, d.dept_name FROM dfs.`%s` e JOIN dfs.`%s` d ON e.dept_id = d.dept_id",
        EMPT_TABLE, DEPT_TABLE);

    // Generate and store output data for test queries. Used when verifying the output of queries ran using different
    // configurations.

    groupByQueryBaselineColumns = new String[] { "dept_id", "numEmployees" };

    groupByQueryBaselineValues = Lists.newArrayList();
    // group Id is generated based on expression 'recordIndex % NUM_DEPTS' above. 'recordIndex' runs from 0 to
    // NUM_EMPLOYEES, so we expect each number of occurrance of each dept_id to be NUM_EMPLOYEES/NUM_DEPTS (1000/40 =
    // 25)
    final int numOccurrances = NUM_EMPLOYEES/NUM_DEPTS;
    for(int i = 0; i < NUM_DEPTS; i++) {
      groupByQueryBaselineValues.add(new Object[] { (long)i, (long)numOccurrances});
    }

    joinQueryBaselineColumns = new String[] { "emp_name", "dept_name" };

    joinQueryBaselineValues = Lists.newArrayList();
    for(int i = 0; i < NUM_EMPLOYEES; i++) {
      final String employee = String.format("Employee %d", i);
      final String dept = String.format("Department %d", i % NUM_DEPTS);
      joinQueryBaselineValues.add(new String[] { employee, dept });
    }
  }

  public static void setupHelper(boolean isMuxOn, boolean isDeMuxOn) throws Exception {
    // set slice count to 1, so that we can have more parallelization for testing
    test("ALTER SESSION SET `planner.slice_target`=1");
    // disable the broadcast join to produce plans with HashToRandomExchanges.
    test("ALTER SESSION SET `planner.enable_broadcast_join`=false");
    test("ALTER SESSION SET `planner.enable_mux_exchange`=" + isMuxOn);
    test("ALTER SESSION SET `planner.enable_demux_exchange`=" + isDeMuxOn);
  }

  @Test
  public void testGroupByMultiFields() throws Exception {
    // Test multifield hash generation

    test("ALTER SESSION SET `planner.slice_target`=1");
    test("ALTER SESSION SET `planner.enable_mux_exchange`=" + true);
    test("ALTER SESSION SET `planner.enable_demux_exchange`=" + false);

    final String groupByMultipleQuery = String.format("SELECT dept_id, mng_id, some_id, count(*) as numEmployees FROM dfs.`%s` e GROUP BY dept_id, mng_id, some_id", EMPT_TABLE);
    final String[] groupByMultipleQueryBaselineColumns = new String[] { "dept_id", "mng_id", "some_id", "numEmployees" };

    final int numOccurrances = NUM_EMPLOYEES/NUM_DEPTS;

    final String plan = getPlanInString("EXPLAIN PLAN FOR " + groupByMultipleQuery, JSON_FORMAT);

    jsonExchangeOrderChecker(plan, false, 1, "hash32asdouble\\(.*, hash32asdouble\\(.*, hash32asdouble\\(.*\\) \\) \\) ");

    // Run the query and verify the output
    final TestBuilder testBuilder = testBuilder()
        .sqlQuery(groupByMultipleQuery)
        .unOrdered()
        .baselineColumns(groupByMultipleQueryBaselineColumns);

    for(int i = 0; i < NUM_DEPTS; i++) {
      testBuilder.baselineValues(new Object[] { (long)i, (long)0, (long)0, (long)numOccurrances});
    }

    testBuilder.go();
  }

  @Test
  public void testGroupBy_NoMux_NoDeMux() throws Exception {
    testGroupByHelper(false, false);
  }

  @Test
  public void testJoin_NoMux_NoDeMux() throws Exception {
    testJoinHelper(false, false);
  }

  @Test
  public void testGroupBy_Mux_NoDeMux() throws Exception {
    testGroupByHelper(true, false);
  }

  @Test
  public void testJoin_Mux_NoDeMux() throws Exception {
    testJoinHelper(true, false);
  }

  @Test
  public void testGroupBy_NoMux_DeMux() throws Exception {
    testGroupByHelper(false, true);
  }

  @Test
  public void testJoin_NoMux_DeMux() throws Exception {
    testJoinHelper(false, true);
  }

  @Test
  public void testGroupBy_Mux_DeMux() throws Exception {
    testGroupByHelper(true, true);
  }

  @Test
  public void testJoin_Mux_DeMux() throws Exception {
    testJoinHelper(true, true);
  }

  private static void testGroupByHelper(boolean isMuxOn, boolean isDeMuxOn) throws Exception {
    testHelper(isMuxOn, isDeMuxOn, groupByQuery,
        isMuxOn ? 1 : 0, isDeMuxOn ? 1 : 0,
        groupByQueryBaselineColumns, groupByQueryBaselineValues);
  }

  public static void testJoinHelper(boolean isMuxOn, boolean isDeMuxOn) throws Exception {
    testHelper(isMuxOn, isDeMuxOn, joinQuery,
        isMuxOn ? 2 : 0, isDeMuxOn ? 2 : 0,
        joinQueryBaselineColumns, joinQueryBaselineValues);
  }

  private static void testHelper(boolean isMuxOn, boolean isDeMuxOn, String query,
      int expectedNumMuxes, int expectedNumDeMuxes, String[] baselineColumns, List<Object[]> baselineValues)
      throws Exception {
    setupHelper(isMuxOn, isDeMuxOn);

    String plan = getPlanInString("EXPLAIN PLAN FOR " + query, JSON_FORMAT);

    if ( isMuxOn ) {
      // # of hash exchanges should be = # of mux exchanges + # of demux exchanges
      assertEquals("HashExpr on the hash column should not happen", 2*expectedNumMuxes+expectedNumDeMuxes, StringUtils.countMatches(plan, HASH_EXPR_NAME));
      jsonExchangeOrderChecker(plan, isDeMuxOn, expectedNumMuxes, "hash32asdouble\\(.*\\) ");
    } else {
      assertEquals("HashExpr on the hash column should not happen", 0, StringUtils.countMatches(plan, HASH_EXPR_NAME));
    }

    // Make sure the plan has mux and demux exchanges (TODO: currently testing is rudimentary,
    // need to move it to sophisticated testing once we have better planning test tools are available)
    assertEquals("Wrong number of MuxExchanges are present in the plan",
        expectedNumMuxes, StringUtils.countMatches(plan, MUX_EXCHANGE));

    assertEquals("Wrong number of DeMuxExchanges are present in the plan",
        expectedNumDeMuxes, StringUtils.countMatches(plan, DEMUX_EXCHANGE));

    // Run the query and verify the output
    TestBuilder testBuilder = testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns(baselineColumns);

    for(Object[] baselineRecord : baselineValues) {
      testBuilder.baselineValues(baselineRecord);
    }

    testBuilder.go();

    testHelperVerifyPartitionSenderParallelization(plan, isMuxOn, isDeMuxOn);
  }

  private static void jsonExchangeOrderChecker(String plan, boolean isDemuxEnabled, int expectedNumMuxes, String hashExprPattern) throws Exception {
    final JSONObject planObj = (JSONObject) new JSONParser().parse(plan);
    assertNotNull("Corrupted query plan: null", planObj);
    final JSONArray graphArray = (JSONArray) planObj.get("graph");
    assertNotNull("No graph array present", graphArray);
    int i = 0;
    int k = 0;
    int prevExprsArraySize = 0;
    boolean foundExpr = false;
    int muxesCount = 0;
    for (Object object : graphArray) {
      final JSONObject popObj = (JSONObject) object;
      if ( popObj.containsKey("pop") && popObj.get("pop").equals("project")) {
        if ( popObj.containsKey("exprs")) {
          final JSONArray exprsArray = (JSONArray) popObj.get("exprs");
          for (Object exprObj : exprsArray) {
            final JSONObject expr = (JSONObject) exprObj;
            if ( expr.containsKey("ref") && expr.get("ref").equals("`"+ HASH_EXPR_NAME +"`")) {
              // found a match. Let's see if next one is the one we need
              final String hashField = (String) expr.get("expr");
              assertNotNull("HashExpr field can not be null", hashField);
              assertTrue("HashExpr field does not match pattern",hashField.matches(hashExprPattern));
              k = i;
              foundExpr = true;
              muxesCount++;
              break;
            }
          }
          if ( foundExpr ) {
            // will be reset to prevExprsArraySize-1 on the last project of the whole stanza
            prevExprsArraySize = exprsArray.size();
          }
        }
      }
      if ( !foundExpr ) {
        continue;
      }
      // next after project with hashexpr
      if ( k == i-1) {
        assertTrue("UnorderedMux should follow Project with HashExpr",
            popObj.containsKey("pop") && popObj.get("pop").equals(MUX_EXCHANGE_CONST));
      }
      if ( k == i-2) {
        assertTrue("HashToRandomExchange should follow UnorderedMux which should follow Project with HashExpr",
            popObj.containsKey("pop") && popObj.get("pop").equals(HASH_EXCHANGE));
        // is HashToRandom is using HashExpr
        assertTrue("HashToRandomExchnage should use hashExpr",
            popObj.containsKey("expr") && popObj.get("expr").equals("`"+ HASH_EXPR_NAME +"`"));
      }
      // if Demux is enabled it also should use HashExpr
      if ( isDemuxEnabled && k == i-3) {
        assertTrue("UnorderdDemuxExchange should follow HashToRandomExchange",
            popObj.containsKey("pop") && popObj.get("pop").equals(DEMUX_EXCHANGE_CONST));
        // is HashToRandom is using HashExpr
        assertTrue("UnorderdDemuxExchange should use hashExpr",
            popObj.containsKey("expr") && popObj.get("expr").equals("`"+HASH_EXPR_NAME +"`"));
      }
      if ( (isDemuxEnabled && k == i-4) || (!isDemuxEnabled && k == i-3) ) {
        // it should be a project without hashexpr, check if number of exprs is 1 less then in first project
        assertTrue("Should be project without hashexpr", popObj.containsKey("pop") && popObj.get("pop").equals("project"));
        final JSONArray exprsArray = (JSONArray) popObj.get("exprs");
        assertNotNull("Project should have some fields", exprsArray);
        assertEquals("Number of fields in closing project should be one less then in starting project",
            prevExprsArraySize, exprsArray.size());

        // Now let's reset all the counters, flags if we are going to have another batch of those exchanges
        k = 0;
        foundExpr = false;
        prevExprsArraySize = 0;
      }
      i++;
    }
    assertEquals("Number of Project/Mux/HashExchange/... ", expectedNumMuxes, muxesCount);
  }

  // Verify the number of partition senders in a major fragments is not more than the cluster size and each endpoint
  // in the cluster has at most one fragment from a given major fragment that has the partition sender.
  private static void testHelperVerifyPartitionSenderParallelization(
      String plan, boolean isMuxOn, boolean isDeMuxOn) throws Exception {

    final DrillbitContext drillbitContext = getDrillbitContext();
    final PhysicalPlanReader planReader = drillbitContext.getPlanReader();
    final Fragment rootFragment = PopUnitTestBase.getRootFragmentFromPlanString(planReader, plan);

    final List<Integer> deMuxFragments = Lists.newLinkedList();
    final List<Integer> htrFragments = Lists.newLinkedList();
    final PlanningSet planningSet = new PlanningSet();

    // Create a planningSet to get the assignment of major fragment ids to fragments.
    PARALLELIZER.initFragmentWrappers(rootFragment, planningSet);

    findFragmentsWithPartitionSender(rootFragment, planningSet, deMuxFragments, htrFragments);

    final QueryContextInformation queryContextInfo = Utilities.createQueryContextInfo("dummySchemaName", "938ea2d9-7cb9-4baf-9414-a5a0b7777e8e");
    QueryWorkUnit qwu = PARALLELIZER.generateWorkUnit(new OptionList(), drillbitContext.getEndpoint(),
        QueryId.getDefaultInstance(),
        drillbitContext.getBits(), rootFragment, USER_SESSION, queryContextInfo);
    qwu.applyPlan(planReader);

    // Make sure the number of minor fragments with HashPartitioner within a major fragment is not more than the
    // number of Drillbits in cluster
    ArrayListMultimap<Integer, DrillbitEndpoint> partitionSenderMap = ArrayListMultimap.create();
    for(PlanFragment planFragment : qwu.getFragments()) {
      if (planFragment.getFragmentJson().contains("hash-partition-sender")) {
        int majorFragmentId = planFragment.getHandle().getMajorFragmentId();
        DrillbitEndpoint assignedEndpoint = planFragment.getAssignment();
        partitionSenderMap.get(majorFragmentId).add(assignedEndpoint);
      }
    }

    if (isMuxOn) {
      verifyAssignment(htrFragments, partitionSenderMap);
    }

    if (isDeMuxOn) {
      verifyAssignment(deMuxFragments, partitionSenderMap);
    }
  }

  /**
   * Helper method to find the major fragment ids of fragments that have PartitionSender.
   * A fragment can have PartitionSender if sending exchange of the current fragment is a
   *   1. DeMux Exchange -> goes in deMuxFragments
   *   2. HashToRandomExchange -> goes into htrFragments
   */
  private static void findFragmentsWithPartitionSender(Fragment currentRootFragment, PlanningSet planningSet,
      List<Integer> deMuxFragments, List<Integer> htrFragments) {

    if (currentRootFragment != null) {
      final Exchange sendingExchange = currentRootFragment.getSendingExchange();
      if (sendingExchange != null) {
        final int majorFragmentId = planningSet.get(currentRootFragment).getMajorFragmentId();
        if (sendingExchange instanceof UnorderedDeMuxExchange) {
          deMuxFragments.add(majorFragmentId);
        } else if (sendingExchange instanceof HashToRandomExchange) {
          htrFragments.add(majorFragmentId);
        }
      }

      for(ExchangeFragmentPair e : currentRootFragment.getReceivingExchangePairs()) {
        findFragmentsWithPartitionSender(e.getNode(), planningSet, deMuxFragments, htrFragments);
      }
    }
  }

  /** Helper method to verify the number of PartitionSenders in a given fragment endpoint assignments */
  private static void verifyAssignment(List<Integer> fragmentList,
      ArrayListMultimap<Integer, DrillbitEndpoint> partitionSenderMap) {

    // We expect at least one entry the list
    assertTrue(fragmentList.size() > 0);

    for(Integer majorFragmentId : fragmentList) {
      // we expect the fragment that has DeMux/HashToRandom as sending exchange to have parallelization with not more
      // than the number of nodes in the cluster and each node in the cluster can have at most one assignment
      List<DrillbitEndpoint> assignments = partitionSenderMap.get(majorFragmentId);
      assertNotNull(assignments);
      assertTrue(assignments.size() > 0);
      assertTrue(String.format("Number of partition senders in major fragment [%d] is more than expected", majorFragmentId), CLUSTER_SIZE >= assignments.size());

      // Make sure there are no duplicates in assigned endpoints (i.e at most one partition sender per endpoint)
      assertTrue("Some endpoints have more than one fragment that has ParitionSender", ImmutableSet.copyOf(assignments).size() == assignments.size());
    }
  }
}
