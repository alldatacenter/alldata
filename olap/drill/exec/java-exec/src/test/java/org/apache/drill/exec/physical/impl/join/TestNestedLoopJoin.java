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
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.test.TestBuilder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

@Category(OperatorTest.class)
public class TestNestedLoopJoin extends JoinTestBase {

  // Test queries used by planning and execution tests
  private static final String testNlJoinExists_1 = "select r_regionkey from cp.`tpch/region.parquet` "
      + " where exists (select n_regionkey from cp.`tpch/nation.parquet` "
      + " where n_nationkey < 10)";

  private static final String testNlJoinNotIn_1 = "select r_regionkey from cp.`tpch/region.parquet` "
      + " where r_regionkey not in (select n_regionkey from cp.`tpch/nation.parquet` "
      + "                            where n_nationkey < 4)";

  // not-in subquery produces empty set
  private static final String testNlJoinNotIn_2 = "select r_regionkey from cp.`tpch/region.parquet` "
      + " where r_regionkey not in (select n_regionkey from cp.`tpch/nation.parquet` "
      + "                            where 1=0)";

  private static final String testNlJoinInequality_1 = "select r_regionkey from cp.`tpch/region.parquet` "
      + " where r_regionkey > (select min(n_regionkey) from cp.`tpch/nation.parquet` "
      + "                        where n_nationkey < 4)";

  private static final String testNlJoinInequality_2 = "select r.r_regionkey, n.n_nationkey from cp.`tpch/nation.parquet` n "
      + " inner join cp.`tpch/region.parquet` r on n.n_regionkey < r.r_regionkey where n.n_nationkey < 3";

  private static final String testNlJoinInequality_3 = "select r_regionkey from cp.`tpch/region.parquet` "
      + " where r_regionkey > (select min(n_regionkey) * 2 from cp.`tpch/nation.parquet` )";

  private static final String testNlJoinBetween = "select " +
      "n.n_nationkey, length(r.r_name) r_name_len, length(r.r_comment) r_comment_len " +
      "from (select * from cp.`tpch/nation.parquet` where n_regionkey = 1) n " +
      "%s join (select * from cp.`tpch/region.parquet` where r_regionkey = 1) r " +
      "on n.n_nationkey between length(r.r_name) and length(r.r_comment) " +
      "order by n.n_nationkey";

  private static final String testNlJoinWithLargeRightInput = "select * from cp.`tpch/region.parquet`r " +
      "left join cp.`tpch/nation.parquet` n on r.r_regionkey <> n.n_regionkey";

  @BeforeClass
  public static void setupTestFiles() {
    dirTestWatcher.copyResourceToRoot(Paths.get("multilevel", "parquet"));
    dirTestWatcher.copyResourceToRoot(Paths.get("join", "multiple"));
  }

  @Test
  public void testNlJoinExists_1_planning() throws Exception {
    testPlanMatchingPatterns(testNlJoinExists_1, new String[]{NLJ_PATTERN}, new String[]{});
  }

  @Test
  public void testNlJoinNotIn_1_planning() throws Exception {
    testPlanMatchingPatterns(testNlJoinNotIn_1, new String[]{NLJ_PATTERN}, new String[]{});
  }

  @Test
  public void testNlJoinInequality_1() throws Exception {
    testPlanMatchingPatterns(testNlJoinInequality_1, new String[]{NLJ_PATTERN}, new String[]{});
  }

  @Test
  public void testNlJoinInequality_2() throws Exception {
    test(DISABLE_NLJ_SCALAR);
    testPlanMatchingPatterns(testNlJoinInequality_2, new String[]{NLJ_PATTERN}, new String[]{});
    test(ENABLE_NLJ_SCALAR);
  }

  @Test
  public void testNlJoinInequality_3() throws Exception {
    test(DISABLE_NLJ_SCALAR);
    testPlanMatchingPatterns(testNlJoinInequality_3, new String[]{NLJ_PATTERN}, new String[]{});
    test(ENABLE_NLJ_SCALAR);
  }

  @Test
  public void testNlJoinAggrs_1_planning() throws Exception {
    String query = "select total1, total2 from "
        + "(select sum(l_quantity) as total1 from cp.`tpch/lineitem.parquet` where l_suppkey between 100 and 200), "
        + "(select sum(l_quantity) as total2 from cp.`tpch/lineitem.parquet` where l_suppkey between 200 and 300)  ";
    testPlanMatchingPatterns(query, new String[]{NLJ_PATTERN}, new String[]{});
  }

  @Test // equality join and scalar right input, hj and mj disabled
  public void testNlJoinEqualityScalar_1_planning() throws Exception {
    String query = "select r_regionkey from cp.`tpch/region.parquet` "
        + " where r_regionkey = (select min(n_regionkey) from cp.`tpch/nation.parquet` "
        + "                        where n_nationkey < 10)";
    test(DISABLE_HJ);
    test(DISABLE_MJ);
    testPlanMatchingPatterns(query, new String[]{NLJ_PATTERN}, new String[]{});
    test(ENABLE_HJ);
    test(ENABLE_MJ);
  }

  @Test // equality join and scalar right input, hj and mj disabled, enforce exchanges
  public void testNlJoinEqualityScalar_2_planning() throws Exception {
    String query = "select r_regionkey from cp.`tpch/region.parquet` "
        + " where r_regionkey = (select min(n_regionkey) from cp.`tpch/nation.parquet` "
        + "                        where n_nationkey < 10)";
    test("alter session set `planner.slice_target` = 1");
    test(DISABLE_HJ);
    test(DISABLE_MJ);
    testPlanMatchingPatterns(query, new String[]{NLJ_PATTERN, "BroadcastExchange"}, new String[]{});
    test(ENABLE_HJ);
    test(ENABLE_MJ);
    test("alter session set `planner.slice_target` = 100000");
  }

  @Test // equality join and non-scalar right input, hj and mj disabled
  public void testNlJoinEqualityNonScalar_1_planning() throws Exception {
    String query = "select r.r_regionkey from cp.`tpch/region.parquet` r inner join cp.`tpch/nation.parquet` n"
        + " on r.r_regionkey = n.n_regionkey where n.n_nationkey < 10";
    test(DISABLE_HJ);
    test(DISABLE_MJ);
    test(DISABLE_NLJ_SCALAR);
    testPlanMatchingPatterns(query, new String[]{NLJ_PATTERN}, new String[]{});
    test(ENABLE_HJ);
    test(ENABLE_MJ);
    test(ENABLE_NLJ_SCALAR);
  }

  @Test // equality join and non-scalar right input, hj and mj disabled, enforce exchanges
  public void testNlJoinEqualityNonScalar_2_planning() throws Exception {
    String query = "select n.n_nationkey from cp.`tpch/nation.parquet` n, "
        + " dfs.`multilevel/parquet` o "
        + " where n.n_regionkey = o.o_orderkey and o.o_custkey > 5";
    test("alter session set `planner.slice_target` = 1");
    test(DISABLE_HJ);
    test(DISABLE_MJ);
    test(DISABLE_NLJ_SCALAR);
    testPlanMatchingPatterns(query, new String[]{NLJ_PATTERN, "BroadcastExchange"}, new String[]{});
    test(ENABLE_HJ);
    test(ENABLE_MJ);
    test(ENABLE_NLJ_SCALAR);
    test("alter session set `planner.slice_target` = 100000");
  }

  // EXECUTION TESTS

  @Test
  public void testNlJoinExists_1_exec() throws Exception {
    testBuilder()
        .sqlQuery(testNlJoinExists_1)
        .unOrdered()
        .baselineColumns("r_regionkey")
        .baselineValues(0)
        .baselineValues(1)
        .baselineValues(2)
        .baselineValues(3)
        .baselineValues(4)
        .go();
  }

  @Test
  public void testNlJoinNotIn_1_exec() throws Exception {
    testBuilder()
        .sqlQuery(testNlJoinNotIn_1)
        .unOrdered()
        .baselineColumns("r_regionkey")
        .baselineValues(2)
        .baselineValues(3)
        .baselineValues(4)
        .go();
  }

  @Test
  public void testNlJoinNotIn_2_exec() throws Exception {
    testBuilder()
        .sqlQuery(testNlJoinNotIn_2)
        .unOrdered()
        .baselineColumns("r_regionkey")
        .baselineValues(0)
        .baselineValues(1)
        .baselineValues(2)
        .baselineValues(3)
        .baselineValues(4)
        .go();
  }

  @Test
  public void testNLJWithEmptyBatch() throws Exception {
    long result = 0L;

    test(DISABLE_NLJ_SCALAR);
    test(DISABLE_HJ);
    test(DISABLE_MJ);

    // We have a false filter causing empty left batch
    String query = "select count(*) col from (select a.lastname " +
        "from cp.`employee.json` a " +
        "where exists (select n_name from cp.`tpch/nation.parquet` b) AND 1 = 0)";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("col")
        .baselineValues(result)
        .go();

    // Below tests use NLJ in a general case (non-scalar subqueries, followed by filter) with empty batches
    query = "select count(*) col from " +
        "(select t1.department_id " +
        "from cp.`employee.json` t1 inner join cp.`department.json` t2 " +
        "on t1.department_id = t2.department_id where t1.department_id = -1)";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("col")
        .baselineValues(result)
        .go();

    query = "select count(*) col from " +
        "(select t1.department_id " +
        "from cp.`employee.json` t1 inner join cp.`department.json` t2 " +
        "on t1.department_id = t2.department_id where t2.department_id = -1)";


    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("col")
        .baselineValues(result)
        .go();

    test(ENABLE_NLJ_SCALAR);
    test(ENABLE_HJ);
    test(ENABLE_MJ);
  }

  @Test
  public void testNlJoinInnerBetween() throws Exception {
    try {
      test(DISABLE_NLJ_SCALAR);
      String query = String.format(testNlJoinBetween, "INNER");
      testPlanMatchingPatterns(query, new String[]{NLJ_PATTERN}, new String[]{});
      testBuilder()
          .sqlQuery(query)
          .ordered()
          .baselineColumns("n_nationkey", "r_name_length", "r_comment_length")
          .baselineValues(17, 7, 31)
          .baselineValues(24, 7, 31)
          .build();
    } finally {
      test(ENABLE_NLJ_SCALAR);
      test(RESET_HJ);
    }
  }

  @Test
  public void testNlJoinLeftBetween() throws Exception {
    try {
      test(DISABLE_NLJ_SCALAR);
      String query = String.format(testNlJoinBetween, "LEFT");
      testPlanMatchingPatterns(query, new String[]{NLJ_PATTERN}, new String[]{});
      testBuilder()
          .sqlQuery(query)
          .ordered()
          .baselineColumns("n_nationkey", "r_name_length", "r_comment_length")
          .baselineValues(1, null, null)
          .baselineValues(2, null, null)
          .baselineValues(3, null, null)
          .baselineValues(17, 7, 31)
          .baselineValues(24, 7, 31)
          .build();
    } finally {
      test(ENABLE_NLJ_SCALAR);
      test(RESET_HJ);
    }
  }

  @Test(expected = UserRemoteException.class)
  public void testNlJoinWithLargeRightInputFailure() throws Exception {
    try {
      test(DISABLE_NLJ_SCALAR);
      test(testNlJoinWithLargeRightInput);
    } catch (UserRemoteException e) {
      assertThat(e.getMessage(), containsString("UNSUPPORTED_OPERATION ERROR: This query cannot be planned " +
          "possibly due to either a cartesian join or an inequality join"));
      throw e;
    } finally {
      test(ENABLE_NLJ_SCALAR);
      test(RESET_HJ);
    }
  }

  @Test
  public void testNlJoinWithLargeRightInputSuccess() throws Exception {
    try {
      test(DISABLE_NLJ_SCALAR);
      test(DISABLE_JOIN_OPTIMIZATION);
      testPlanMatchingPatterns(testNlJoinWithLargeRightInput, new String[]{NLJ_PATTERN}, new String[]{});
    } finally {
      test(ENABLE_NLJ_SCALAR);
      test(RESET_HJ);
      test(RESET_JOIN_OPTIMIZATION);
    }
  }

  @Test
  public void testNestedLeftJoinWithEmptyTable() throws Exception {
    try {
      enableJoin(false, false, true);
      testJoinWithEmptyFile(dirTestWatcher.getRootDir(), "left outer", new String[] {NLJ_PATTERN, LEFT_JOIN_TYPE}, 1155L);
    } finally {
      resetJoinOptions();
    }
  }

  @Test
  public void testNestedInnerJoinWithEmptyTable() throws Exception {
    try {
      enableJoin(false, false, true);
      testJoinWithEmptyFile(dirTestWatcher.getRootDir(), "inner", new String[] {NLJ_PATTERN, INNER_JOIN_TYPE}, 0L);
    } finally {
      resetJoinOptions();
    }
  }

  @Test(expected = RpcException.class)
  public void testNestedRightJoinWithEmptyTable() throws Exception {
    try {
      enableJoin(false, false, true);
      testJoinWithEmptyFile(dirTestWatcher.getRootDir(), "right outer", new String[] {NLJ_PATTERN, RIGHT_JOIN_TYPE}, 0L);
    } catch (RpcException e) {
      assertTrue("Not expected exception is obtained while performing the query with RIGHT JOIN logical operator " +
          "by using nested loop join physical operator",
          e.getMessage().contains("SYSTEM ERROR: CannotPlanException"));
      throw e;
    } finally {
      resetJoinOptions();
    }
  }

  /**
   * Validates correctness of NestedLoopJoin when right side has multiple batches.
   * See DRILL-6128 for details
   * @throws Exception
   */
  @Test
  public void testNLJoinCorrectnessRightMultipleBatches() throws Exception {
    try {
      test(DISABLE_NLJ_SCALAR);
      test(DISABLE_JOIN_OPTIMIZATION);
      setSessionOption(ExecConstants.SLICE_TARGET, 1);
      test(DISABLE_HJ);
      test(DISABLE_MJ);

      final String query = "SELECT l.id_left AS id_left, r.id_right AS id_right FROM dfs.`join/multiple/left` l left " +
        "join dfs.`join/multiple/right` r on l.id_left = r.id_right";

      Map<SchemaPath, TypeProtos.MajorType> typeMap = new HashMap<>();
      typeMap.put(TestBuilder.parsePath("id_left"), Types.optional(TypeProtos.MinorType.BIGINT));
      typeMap.put(TestBuilder.parsePath("id_right"), Types.optional(TypeProtos.MinorType.BIGINT));

      testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .csvBaselineFile("join/expected/nestedLoopJoinBaseline.csv")
        .baselineColumns("id_left", "id_right")
        .baselineTypes(typeMap)
        .go();

    } catch (Exception e) {
      fail();
    } finally {
      test(ENABLE_NLJ_SCALAR);
      test(RESET_JOIN_OPTIMIZATION);
      test(ENABLE_HJ);
      test(ENABLE_MJ);
      setSessionOption(ExecConstants.SLICE_TARGET, 100000);
    }
  }

  @Test
  public void testNlJoinWithStringsInCondition() throws Exception {
    try {
      test(DISABLE_NLJ_SCALAR);
      test(DISABLE_JOIN_OPTIMIZATION);

      final String query =
          "select v.employee_id\n" +
          "from cp.`employee.json` v\n" +
          "left outer join cp.`employee.json` s\n" +
          "on v.employee_id <> s.employee_id\n" +
          "and (v.position_id <= '-1' or s.department_id > '5000')\n" +
          "order by v.employee_id limit 1";

      testBuilder()
          .sqlQuery(query)
          .unOrdered()
          .baselineColumns("employee_id")
          .baselineValues(1L)
          .go();
    } finally {
      resetJoinOptions();
      test(RESET_JOIN_OPTIMIZATION);
    }
  }
}
