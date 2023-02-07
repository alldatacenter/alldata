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
import org.apache.drill.PlanTestBase;
import org.junit.experimental.categories.Category;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.drill.exec.planner.physical.PlannerSettings;


@Category(OperatorTest.class)
public class JoinTestBase extends PlanTestBase {

  public static final String HJ_PATTERN = "HashJoin";
  public static final String MJ_PATTERN = "MergeJoin";
  public static final String NLJ_PATTERN = "NestedLoopJoin";
  public static final String INNER_JOIN_TYPE = "joinType=\\[inner\\]";
  public static final String LEFT_JOIN_TYPE = "joinType=\\[left\\]";
  public static final String RIGHT_JOIN_TYPE = "joinType=\\[right\\]";

  public static final String DISABLE_HJ =
      String.format("alter session set `%s` = false", PlannerSettings.HASHJOIN.getOptionName());
  public static final String ENABLE_HJ =
      String.format("alter session set `%s` = true", PlannerSettings.HASHJOIN.getOptionName());
  public static final String RESET_HJ =
      String.format("alter session reset `%s`", PlannerSettings.HASHJOIN.getOptionName());
  public static final String DISABLE_MJ =
      String.format("alter session set `%s` = false", PlannerSettings.MERGEJOIN.getOptionName());
  public static final String ENABLE_MJ =
      String.format("alter session set `%s` = true", PlannerSettings.MERGEJOIN.getOptionName());
  public static final String DISABLE_NLJ_SCALAR =
      String.format("alter session set `%s` = false", PlannerSettings.NLJOIN_FOR_SCALAR.getOptionName());
  public static final String ENABLE_NLJ_SCALAR =
      String.format("alter session set `%s` = true", PlannerSettings.NLJOIN_FOR_SCALAR.getOptionName());
  public static final String DISABLE_JOIN_OPTIMIZATION =
      String.format("alter session set `%s` = false", PlannerSettings.JOIN_OPTIMIZATION.getOptionName());
  public static final String RESET_JOIN_OPTIMIZATION =
      String.format("alter session reset `%s`", PlannerSettings.JOIN_OPTIMIZATION.getOptionName());


  private static final String TEST_EMPTY_JOIN = "select count(*) as cnt from cp.`employee.json` emp %s join dfs.`dept.json` " +
          "as dept on dept.manager = emp.`last_name`";

  /**
   * This method runs a join query with one of the table generated as an
   * empty json file.
   * @param testDir in which the empty json file is generated.
   * @param joinType to be executed.
   * @param joinPattern to look for the pattern in the successful run.
   * @param result number of the output rows.
   */
  public void testJoinWithEmptyFile(File testDir, String joinType,
                         String[] joinPattern, long result) throws Exception {
    buildFile("dept.json", new String[0], testDir);
    String query = String.format(TEST_EMPTY_JOIN, joinType);
    testPlanMatchingPatterns(query, joinPattern, new String[]{});
    testBuilder()
            .sqlQuery(query)
            .unOrdered()
            .baselineColumns("cnt")
            .baselineValues(result)
            .build().run();
  }

  private void buildFile(String fileName, String[] data, File testDir) throws IOException {
    try(PrintWriter out = new PrintWriter(new FileWriter(new File(testDir, fileName)))) {
      for (String line : data) {
        out.println(line);
      }
    }
  }

  /**
   * Allows to enable necessary join operator.
   * @param hj hash join operator
   * @param mj merge join operator
   * @param nlj nested-loop join operator
   * @throws Exception If any exception is obtained, all set options should be reset
   */
  public static void enableJoin(boolean hj, boolean mj, boolean nlj) throws Exception {
    setSessionOption((PlannerSettings.HASHJOIN.getOptionName()), hj);
    setSessionOption((PlannerSettings.MERGEJOIN.getOptionName()), mj);
    setSessionOption((PlannerSettings.NESTEDLOOPJOIN.getOptionName()), nlj);
    setSessionOption((PlannerSettings.NLJOIN_FOR_SCALAR.getOptionName()), !(nlj));
  }

  /**
   * Allows to reset session options of custom join operators
   */
  public static void resetJoinOptions() {
    resetSessionOption((PlannerSettings.HASHJOIN.getOptionName()));
    resetSessionOption((PlannerSettings.MERGEJOIN.getOptionName()));
    resetSessionOption((PlannerSettings.NESTEDLOOPJOIN.getOptionName()));
    resetSessionOption((PlannerSettings.NLJOIN_FOR_SCALAR.getOptionName()));
  }
}
