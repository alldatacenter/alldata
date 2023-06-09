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
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category(OperatorTest.class)
public class TestJoinEmptyDirTable extends JoinTestBase {

  private static final String EMPTY_DIRECTORY = "empty_directory";

  @BeforeClass
  public static void setupTestFiles() {
    dirTestWatcher.makeTestTmpSubDir(Paths.get(EMPTY_DIRECTORY));
  }

  @Test
  public void testHashInnerJoinWithLeftEmptyDirTable() throws Exception {
    try {
      String query = String.format("select t1.`employee_id`, t1.`full_name`, t2.`employee_id`, t2.`full_name` " +
          "from dfs.tmp.`%s` t1 inner join cp.`employee.json` t2 on t1.`full_name` = t2.`full_name`", EMPTY_DIRECTORY);
      final int expectedRecordCount = 0;

      enableJoin(true, false, false);
      testPlanMatchingPatterns(query, new String[]{HJ_PATTERN, INNER_JOIN_TYPE}, new String[]{});
      final int actualRecordCount = testSql(query);
      assertEquals("Number of output rows", expectedRecordCount, actualRecordCount);
    } finally {
      resetJoinOptions();
    }
  }

  @Test
  public void testHashInnerJoinWithRightEmptyDirTable() throws Exception {
    try {
      String query = String.format("select t1.`employee_id`, t1.`full_name`, t2.`employee_id`, t2.`full_name` " +
          "from cp.`employee.json` t1 inner join dfs.tmp.`%s` t2 on t1.`full_name` = t2.`full_name`", EMPTY_DIRECTORY);
      final int expectedRecordCount = 0;

      enableJoin(true, false, false);
      testPlanMatchingPatterns(query, new String[]{HJ_PATTERN, INNER_JOIN_TYPE}, new String[]{});
      final int actualRecordCount = testSql(query);
      assertEquals("Number of output rows", expectedRecordCount, actualRecordCount);
    } finally {
      resetJoinOptions();
    }
  }

  @Test
  public void testHashInnerJoinWithBothEmptyDirTables() throws Exception {
    try {
      String query = String.format("select t1.`employee_id`, t1.`full_name`, t2.`employee_id`, t2.`full_name` " +
          "from dfs.tmp.`%1$s` t1 inner join dfs.tmp.`%1$s` t2 on t1.`full_name` = t2.`full_name`", EMPTY_DIRECTORY);
      final int expectedRecordCount = 0;

      enableJoin(true, false, false);
      testPlanMatchingPatterns(query, new String[]{HJ_PATTERN, INNER_JOIN_TYPE}, new String[]{});
      final int actualRecordCount = testSql(query);
      assertEquals("Number of output rows", expectedRecordCount, actualRecordCount);
    } finally {
      resetJoinOptions();
    }
  }

  @Test
  public void testHashLeftJoinWithRightEmptyDirTable() throws Exception {
    try {
      String query = String.format("select t1.`employee_id`, t1.`full_name`, t2.`employee_id`, t2.`full_name` " +
          "from cp.`employee.json` t1 left join dfs.tmp.`%s` t2 on t1.`full_name` = t2.`full_name`", EMPTY_DIRECTORY);
      final int expectedRecordCount = 1155;

      enableJoin(true, false, false);
      testPlanMatchingPatterns(query, new String[]{HJ_PATTERN, LEFT_JOIN_TYPE}, new String[]{});
      final int actualRecordCount = testSql(query);
      assertEquals("Number of output rows", expectedRecordCount, actualRecordCount);
    } finally {
      resetJoinOptions();
    }
  }

  @Test
  public void testHashRightJoinWithRightEmptyDirTable() throws Exception {
    try {
      String query = String.format("select t1.`employee_id`, t1.`full_name`, t2.`employee_id`, t2.`full_name` " +
          "from cp.`employee.json` t1 right join dfs.tmp.`%s` t2 on t1.`full_name` = t2.`full_name`", EMPTY_DIRECTORY);
      final int expectedRecordCount = 0;

      enableJoin(true, false, false);
      testPlanMatchingPatterns(query, new String[]{HJ_PATTERN, RIGHT_JOIN_TYPE}, new String[]{});
      final int actualRecordCount = testSql(query);
      assertEquals("Number of output rows", expectedRecordCount, actualRecordCount);
    } finally {
      resetJoinOptions();
    }
  }

  @Test
  public void testMergeInnerJoinWithLeftEmptyDirTable() throws Exception {
    try {
      String query = String.format("select t1.`employee_id`, t1.`full_name`, t2.`employee_id`, t2.`full_name` " +
          "from dfs.tmp.`%s` t1 inner join cp.`employee.json` t2 on t1.`full_name` = t2.`full_name`", EMPTY_DIRECTORY);
      final int expectedRecordCount = 0;

      enableJoin(false, true, false);
      testPlanMatchingPatterns(query, new String[]{MJ_PATTERN, INNER_JOIN_TYPE}, new String[]{});
      final int actualRecordCount = testSql(query);
      assertEquals("Number of output rows", expectedRecordCount, actualRecordCount);
    } finally {
      resetJoinOptions();
    }
  }

  @Test
  public void testMergeInnerJoinWithRightEmptyDirTable() throws Exception {
    try {
      String query = String.format("select t1.`employee_id`, t1.`full_name`, t2.`employee_id`, t2.`full_name` " +
          "from cp.`employee.json` t1 inner join dfs.tmp.`%s` t2 on t1.`full_name` = t2.`full_name`", EMPTY_DIRECTORY);
      final int expectedRecordCount = 0;

      enableJoin(false, true, false);
      testPlanMatchingPatterns(query, new String[]{MJ_PATTERN, INNER_JOIN_TYPE}, new String[]{});
      final int actualRecordCount = testSql(query);
      assertEquals("Number of output rows", expectedRecordCount, actualRecordCount);
    } finally {
      resetJoinOptions();
    }
  }

  @Test
  public void testMergeInnerJoinWithBothEmptyDirTables() throws Exception {
    try {
      String query = String.format("select t1.`employee_id`, t1.`full_name`, t2.`employee_id`, t2.`full_name` " +
          "from dfs.tmp.`%1$s` t1 inner join dfs.tmp.`%1$s` t2 on t1.`full_name` = t2.`full_name`", EMPTY_DIRECTORY);
      final int expectedRecordCount = 0;

      enableJoin(false, true, false);
      testPlanMatchingPatterns(query, new String[]{MJ_PATTERN, INNER_JOIN_TYPE}, new String[]{});
      final int actualRecordCount = testSql(query);
      assertEquals("Number of output rows", expectedRecordCount, actualRecordCount);
    } finally {
      resetJoinOptions();
    }
  }

  @Test
  public void testMergeLeftJoinWithRightEmptyDirTable() throws Exception {
    try {
      String query = String.format("select t1.`employee_id`, t1.`full_name`, t2.`employee_id`, t2.`full_name` " +
          "from cp.`employee.json` t1 left join dfs.tmp.`%s` t2 on t1.`full_name` = t2.`full_name`", EMPTY_DIRECTORY);
      final int expectedRecordCount = 1155;

      enableJoin(false, true, false);
      testPlanMatchingPatterns(query, new String[]{MJ_PATTERN, LEFT_JOIN_TYPE}, new String[]{});
      final int actualRecordCount = testSql(query);
      assertEquals("Number of output rows", expectedRecordCount, actualRecordCount);
    } finally {
      resetJoinOptions();
    }
  }

  @Test
  public void testMergeRightJoinWithRightEmptyDirTable() throws Exception {
    try {
      String query = String.format("select t1.`employee_id`, t1.`full_name`, t2.`employee_id`, t2.`full_name` " +
          "from cp.`employee.json` t1 right join dfs.tmp.`%s` t2 on t1.`full_name` = t2.`full_name`", EMPTY_DIRECTORY);
      final int expectedRecordCount = 0;

      enableJoin(false, true, false);
      testPlanMatchingPatterns(query, new String[]{MJ_PATTERN, RIGHT_JOIN_TYPE}, new String[]{});
      final int actualRecordCount = testSql(query);
      assertEquals("Number of output rows", expectedRecordCount, actualRecordCount);
    } finally {
      resetJoinOptions();
    }
  }

  @Test
  public void testNestedLoopInnerJoinWithLeftEmptyDirTable() throws Exception {
    try {
      String query = String.format("select t1.`employee_id`, t1.`full_name`, t2.`employee_id`, t2.`full_name` " +
          "from dfs.tmp.`%s` t1 inner join cp.`employee.json` t2 on t1.`full_name` = t2.`full_name`", EMPTY_DIRECTORY);
      final int expectedRecordCount = 0;

      enableJoin(false, false, true);
      testPlanMatchingPatterns(query, new String[]{NLJ_PATTERN, INNER_JOIN_TYPE}, new String[]{});
      final int actualRecordCount = testSql(query);
      assertEquals("Number of output rows", expectedRecordCount, actualRecordCount);
    } finally {
      resetJoinOptions();
    }
  }

  @Test
  public void testNestedLoopInnerJoinWithRightEmptyDirTable() throws Exception {
    try {
      String query = String.format("select t1.`employee_id`, t1.`full_name`, t2.`employee_id`, t2.`full_name` " +
          "from cp.`employee.json` t1 inner join dfs.tmp.`%s` t2 on t1.`full_name` = t2.`full_name`", EMPTY_DIRECTORY);
      final int expectedRecordCount = 0;

      enableJoin(false, false, true);
      testPlanMatchingPatterns(query, new String[]{NLJ_PATTERN, INNER_JOIN_TYPE}, new String[]{});
      final int actualRecordCount = testSql(query);
      assertEquals("Number of output rows", expectedRecordCount, actualRecordCount);
    } finally {
      resetJoinOptions();
    }
  }

  @Test
  public void testNestedLoopInnerJoinWithBothEmptyDirTables() throws Exception {
    try {
      String query = String.format("select t1.`employee_id`, t1.`full_name`, t2.`employee_id`, t2.`full_name` " +
          "from dfs.tmp.`%1$s` t1 inner join dfs.tmp.`%1$s` t2 on t1.`full_name` = t2.`full_name`", EMPTY_DIRECTORY);
      final int expectedRecordCount = 0;

      enableJoin(false, false, true);
      testPlanMatchingPatterns(query, new String[]{NLJ_PATTERN, INNER_JOIN_TYPE}, new String[]{});
      final int actualRecordCount = testSql(query);
      assertEquals("Number of output rows", expectedRecordCount, actualRecordCount);
    } finally {
      resetJoinOptions();
    }
  }

  @Test
  public void testNestedLoopLeftJoinWithLeftEmptyDirTable() throws Exception {
    try {
      String query = String.format("select t1.`employee_id`, t1.`full_name`, t2.`employee_id`, t2.`full_name` " +
          "from dfs.tmp.`%s` t1 left join cp.`employee.json` t2 on t1.`full_name` = t2.`full_name`", EMPTY_DIRECTORY);
      final int expectedRecordCount = 0;

      enableJoin(false, false, true);
      // See details in description for PlannerSettings.JOIN_OPTIMIZATION
      setSessionOption((PlannerSettings.JOIN_OPTIMIZATION.getOptionName()), false);
      testPlanMatchingPatterns(query, new String[]{NLJ_PATTERN, LEFT_JOIN_TYPE}, new String[]{});
      final int actualRecordCount = testSql(query);
      assertEquals("Number of output rows", expectedRecordCount, actualRecordCount);
    } finally {
      resetJoinOptions();
      resetSessionOption((PlannerSettings.JOIN_OPTIMIZATION.getOptionName()));

    }
  }

  @Test
  public void testNestedLoopLeftJoinWithRightEmptyDirTable() throws Exception {
    try {
      String query = String.format("select t1.`employee_id`, t1.`full_name`, t2.`employee_id`, t2.`full_name` " +
          "from cp.`employee.json` t1 left join dfs.tmp.`%s` t2 on t1.`full_name` = t2.`full_name`", EMPTY_DIRECTORY);
      final int expectedRecordCount = 1155;

      enableJoin(false, false, true);
      testPlanMatchingPatterns(query, new String[]{NLJ_PATTERN, LEFT_JOIN_TYPE}, new String[]{});
      final int actualRecordCount = testSql(query);
      assertEquals("Number of output rows", expectedRecordCount, actualRecordCount);
    } finally {
      resetJoinOptions();
    }
  }

  @Test
  public void testNestedLoopRightJoinWithLeftEmptyDirTable() throws Exception {
    try {
      String query = String.format("select t1.`employee_id`, t1.`full_name`, t2.`employee_id`, t2.`full_name` " +
          "from dfs.tmp.`%s` t1 right join cp.`employee.json` t2 on t1.`full_name` = t2.`full_name`", EMPTY_DIRECTORY);
      final int expectedRecordCount = 1155;

      enableJoin(false, false, true);
      // The left output is less than right one. Therefore during optimization phase the RIGHT JOIN is converted to LEFT JOIN
      testPlanMatchingPatterns(query, new String[]{NLJ_PATTERN, LEFT_JOIN_TYPE}, new String[]{});
      final int actualRecordCount = testSql(query);
      assertEquals("Number of output rows", expectedRecordCount, actualRecordCount);
    } finally {
      resetJoinOptions();
    }
  }

  @Test(expected = UserRemoteException.class)
  public void testNestedLoopRightJoinWithRightEmptyDirTable() throws Exception {
    try {
      String query = String.format("select t1.`employee_id`, t1.`full_name`, t2.`employee_id`, t2.`full_name` " +
          "from cp.`employee.json` t1 right join dfs.tmp.`%s` t2 on t1.`full_name` = t2.`full_name`", EMPTY_DIRECTORY);

      enableJoin(false, false, true);
      // The nested loops join does not support the "RIGHT OUTER JOIN" logical join operator.
      test(query);
    } catch (UserRemoteException e) {
      assertTrue("Not expected exception is obtained while performing the query with RIGHT JOIN logical operator " +
              "by using nested loop join physical operator",
          e.getMessage().contains("SYSTEM ERROR: CannotPlanException"));
      throw e;
    } finally {
      resetJoinOptions();
    }
  }
}
