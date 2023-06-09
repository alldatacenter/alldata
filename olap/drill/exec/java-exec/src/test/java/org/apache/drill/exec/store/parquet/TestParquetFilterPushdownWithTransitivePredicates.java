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
import org.apache.drill.categories.ParquetTest;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.exec.util.StoragePluginTestUtils;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

@Category({ParquetTest.class, SlowTest.class})
public class TestParquetFilterPushdownWithTransitivePredicates extends PlanTestBase {

  private static final String TABLE_PATH = "parquetFilterPush/transitiveClosure";
  private static final String FIRST_TABLE_NAME = String.format("%s.`%s/%s`",
      StoragePluginTestUtils.DFS_PLUGIN_NAME, TABLE_PATH, "first");
  private static final String SECOND_TABLE_NAME = String.format("%s.`%s/%s`",
      StoragePluginTestUtils.DFS_PLUGIN_NAME, TABLE_PATH, "second");
  private static final String THIRD_TABLE_NAME = String.format("%s.`%s/%s`",
      StoragePluginTestUtils.DFS_PLUGIN_NAME, TABLE_PATH, "third");

  @BeforeClass
  public static void copyData() {
    dirTestWatcher.copyResourceToRoot(Paths.get(TABLE_PATH));
  }

  @Test
  public void testForSeveralInnerJoins() throws Exception {
    String query = String.format("SELECT * FROM %s t1 JOIN %s t2 ON t1.`month` = t2.`month` " +
            "JOIN %s t3 ON t1.`period` = t3.`period` WHERE t2.`month` = 7 AND t1.`period` = 2",
        FIRST_TABLE_NAME, SECOND_TABLE_NAME, THIRD_TABLE_NAME);

    int actualRowCount = testSql(query);
    int expectedRowCount = 24;
    assertEquals("Expected and actual row count should match", expectedRowCount, actualRowCount);

    final String[] expectedPlan = {"first.*numRowGroups=1", "second.*numRowGroups=1", "third.*numRowGroups=3"};
    testPlanMatchingPatterns(query, expectedPlan);
  }

  @Test
  public void testForFilterInJoinOperator() throws Exception {
    String query = String.format("SELECT * FROM %s t1 JOIN %s t2 ON t1.`month` = t2.`month` AND t2.`month` = 7 " +
            "JOIN %s t3 ON t1.`period` = t3.`period` AND t1.`period` = 2",
        FIRST_TABLE_NAME, SECOND_TABLE_NAME, THIRD_TABLE_NAME);

    int actualRowCount = testSql(query);
    int expectedRowCount = 24;
    assertEquals("Expected and actual row count should match", expectedRowCount, actualRowCount);

    final String[] expectedPlan = {"first.*numRowGroups=1", "second.*numRowGroups=1", "third.*numRowGroups=3"};
    testPlanMatchingPatterns(query, expectedPlan);
  }

  @Test
  public void testForLeftAndRightJoins() throws Exception {
    String query = String.format("SELECT * FROM %s t1 RIGHT JOIN %s t2 ON t1.`year` = t2.`year` " +
            "LEFT JOIN %s t3 ON t1.`period` = t3.`period` WHERE t2.`year` = 1987 AND t1.`period` = 1",
        FIRST_TABLE_NAME, SECOND_TABLE_NAME, THIRD_TABLE_NAME);

    int actualRowCount = testSql(query);
    int expectedRowCount = 54;
    assertEquals("Expected and actual row count should match", expectedRowCount, actualRowCount);

    final String[] expectedPlan = {"first.*numRowGroups=2", "second.*numRowGroups=2", "third.*numRowGroups=3"};
    testPlanMatchingPatterns(query, expectedPlan);
  }

  @Test
  public void testForCommaSeparatedJoins() throws Exception {
    String query = String.format("SELECT * FROM %s t1, %s t2, %s t3 WHERE t1.`year` = t2.`year` " +
            "AND t1.`period` = t3.`period` AND t2.`year` = 1990 AND t3.`period` = 1",
        FIRST_TABLE_NAME, SECOND_TABLE_NAME, THIRD_TABLE_NAME);

    int actualRowCount = testSql(query);
    int expectedRowCount = 24;
    assertEquals("Expected and actual row count should match", expectedRowCount, actualRowCount);

    final String[] expectedPlan = {"first.*numRowGroups=2", "second.*numRowGroups=2", "third.*numRowGroups=3"};
    testPlanMatchingPatterns(query, expectedPlan);
  }

  @Test
  public void testForInAndNotOperators() throws Exception {
    String query = String.format("SELECT * FROM %s t1 JOIN %s t2 " +
            "ON t1.`year` = t2.`year` JOIN %s t3 ON t1.`period` = t3.`period` " +
            "WHERE t2.`year` NOT IN (1987, 1988) AND t3.`period` IN (1, 2)",
        FIRST_TABLE_NAME, SECOND_TABLE_NAME, THIRD_TABLE_NAME);


    int actualRowCount = testSql(query);
    int expectedRowCount = 72;
    assertEquals("Expected and actual row count should match", expectedRowCount, actualRowCount);

    final String[] expectedPlan = {"first.*numRowGroups=6", "second.*numRowGroups=3", "third.*numRowGroups=4"};
    testPlanMatchingPatterns(query, expectedPlan);
  }

  @Test
  public void testForBetweenOperator() throws Exception {
    String query = String.format("SELECT * FROM %s t1 JOIN %s t2 " +
            "ON t1.`year` = t2.`year` JOIN %s t3 ON t1.`period` = t3.`period` " +
            "WHERE t2.`year` BETWEEN 1988 AND 1991 AND t3.`period` BETWEEN 2 AND 4 ",
        FIRST_TABLE_NAME, SECOND_TABLE_NAME, THIRD_TABLE_NAME);

    int actualRowCount = testSql(query);
    int expectedRowCount = 96;
    assertEquals("Expected and actual row count should match", expectedRowCount, actualRowCount);

    final String[] expectedPlan = {"first.*numRowGroups=7", "second.*numRowGroups=5", "third.*numRowGroups=6"};
    testPlanMatchingPatterns(query, expectedPlan);
  }

  @Test
  public void testForGreaterThanAndLessThanOperators() throws Exception {
    String query = String.format("SELECT * FROM %s t1 JOIN %s t2 " +
            "ON t1.`year` = t2.`year` JOIN %s t3 ON t1.`period` = t3.`period` " +
            "WHERE t2.`year` >= 1990 AND t3.`period` < 2",
        FIRST_TABLE_NAME, SECOND_TABLE_NAME, THIRD_TABLE_NAME);

    int actualRowCount = testSql(query);
    int expectedRowCount = 36;
    assertEquals("Expected and actual row count should match", expectedRowCount, actualRowCount);

    final String[] expectedPlan = {"first.*numRowGroups=3", "second.*numRowGroups=3", "third.*numRowGroups=3"};
    testPlanMatchingPatterns(query, expectedPlan);
  }


  @Test
  public void testForSubQuery() throws Exception {
    String query = String.format("SELECT * FROM %s t1 JOIN " +
            "(SELECT `year`, `month` FROM %s WHERE `year` = 1987 AND `month` = 5) t2 " +
            "ON t1.`year` = t2.`year` AND t1.`month` = t2.`month`",
        FIRST_TABLE_NAME, SECOND_TABLE_NAME);

    int actualRowCount = testSql(query);
    int expectedRowCount = 4;
    assertEquals("Expected and actual row count should match", expectedRowCount, actualRowCount);

    final String[] expectedPlan = {"first.*numRowGroups=2", "second.*numRowGroups=1"};
    testPlanMatchingPatterns(query, expectedPlan);
  }

  @Test
  public void testForWithStatement() throws Exception {
    String query = String.format("WITH `first_date` AS (SELECT `year`, `month` FROM %s WHERE `year` = 1987 and `month` = 5) " +
            "SELECT t2.`year`, t2.`month` FROM %s t2 JOIN `first_date` ON t2.`year` = `first_date`.`year` AND t2.`month` = `first_date`.`month`",
        FIRST_TABLE_NAME, SECOND_TABLE_NAME);

    int actualRowCount = testSql(query);
    int expectedRowCount = 4;
    assertEquals("Expected and actual row count should match", expectedRowCount, actualRowCount);

    final String[] expectedPlan = {"first.*numRowGroups=2", "second.*numRowGroups=1"};
    testPlanMatchingPatterns(query, expectedPlan);
  }

  @Test // TODO: CALCITE-1048
  @Ignore // For now plan has "first.*numRowGroups=7". Replacing left join to inner should be made earlier.
  public void testForTwoExists() throws Exception {
    String query = String.format("SELECT * from %s t1 " +
        " WHERE EXISTS (SELECT * FROM %s t2 WHERE t1.`year` = t2.`year` AND t2.`year` = 1988) " +
        " AND EXISTS (SELECT * FROM %s t3 WHERE t1.`period` = t3.`period` AND t3.`period` = 2)",
        FIRST_TABLE_NAME, SECOND_TABLE_NAME, THIRD_TABLE_NAME);

    int actualRowCount = testSql(query);
    int expectedRowCount = 2;
    assertEquals("Expected and actual row count should match", expectedRowCount, actualRowCount);

    final String[] expectedPlan = {"first.*numRowGroups=2", "second.*numRowGroups=2", "third.*numRowGroups=3"};
    testPlanMatchingPatterns(query, expectedPlan);
  }

  @Test
  public void testForFilterInHaving() throws Exception {
    String query = String.format("SELECT t1.`year`, t2.`year`, t1.`period`, t3.`period` FROM %s t1 " +
        "JOIN %s t2 ON t1.`year` = t2.`year` " +
        "JOIN %s t3 ON t1.`period` = t3.`period` " +
        "GROUP BY t1.`year`, t2.`year`, t1.`period`, t3.`period` " +
        "HAVING t2.`year` = 1987 AND t3.`period` = 1",
        FIRST_TABLE_NAME, SECOND_TABLE_NAME, THIRD_TABLE_NAME);

    int actualRowCount = testSql(query);
    int expectedRowCount = 1;
    assertEquals("Expected and actual row count should match", expectedRowCount, actualRowCount);

    final String[] expectedPlan = {"first.*numRowGroups=2", "second.*numRowGroups=2", "third.*numRowGroups=3"};
    testPlanMatchingPatterns(query, expectedPlan);
  }

  @Test // TODO: CALCITE-2241
  @Ignore // For now plan has "first.*numRowGroups=16", "second.*numRowGroups=7"
  public void testForOrOperator() throws Exception {
    String query = String.format("SELECT * FROM %s t1 " +
            "JOIN %s t2 ON t1.`month` = t2.`month` " +
            "WHERE t2.`month` = 4 OR t1.`month` = 11",
        FIRST_TABLE_NAME, SECOND_TABLE_NAME);

    int actualRowCount = testSql(query);
    int expectedRowCount = 13;
    assertEquals("Expected and actual row count should match", expectedRowCount, actualRowCount);

    final String[] expectedPlan = {"first.*numRowGroups=4", "second.*numRowGroups=2"};
    testPlanMatchingPatterns(query, expectedPlan);
  }

  @Test // TODO: CALCITE-2275
  @Ignore // For now plan has "first.*numRowGroups=14""
  public void testForInAndNotOperatorsInJoinCondition() throws Exception {
    String query = String.format("SELECT * FROM %s t1 JOIN %s t2 " +
            "ON t1.`year` = t2.`year` AND t2.`year` NOT IN (1987, 1988) JOIN %s t3 ON t1.`period` = t3.`period` " +
            "WHERE t3.`period` IN (1, 2)",
        FIRST_TABLE_NAME, SECOND_TABLE_NAME, THIRD_TABLE_NAME);


    int actualRowCount = testSql(query);
    int expectedRowCount = 72;
    assertEquals("Expected and actual row count should match", expectedRowCount, actualRowCount);

    final String[] expectedPlan = {"first.*numRowGroups=6", "second.*numRowGroups=3", "third.*numRowGroups=4"};
    testPlanMatchingPatterns(query, expectedPlan);
  }

  @Test // TODO: CALCITE-2274
  @Ignore // For now plan has "first.*numRowGroups=16""
  public void testForSubQueryAndDynamicStar() throws Exception {
    String query = String.format("SELECT * FROM %s t1 JOIN " +
            "(SELECT * FROM %s WHERE `year` = 1987 AND `month` = 5) t2 ON t1.`year` = t2.`year` AND t1.`month` = t2.`month`",
        FIRST_TABLE_NAME, SECOND_TABLE_NAME);

    int actualRowCount = testSql(query);
    int expectedRowCount = 4;
    assertEquals("Expected and actual row count should match", expectedRowCount, actualRowCount);

    final String[] expectedPlan = {"first.*numRowGroups=2", "second.*numRowGroups=1"};
    testPlanMatchingPatterns(query, expectedPlan);
  }

  @Test // TODO: CALCITE-2274
  @Ignore // For now plan has "second.*numRowGroups=7"
  public void testForWithStatementAndDynamicStar() throws Exception {
    String query = String.format("WITH `first_date` AS (SELECT * FROM %s t1 WHERE t1.`year` = 1987 and t1.`month` = 5) " +
            "SELECT * FROM %s t2 JOIN `first_date` ON t2.`year` = `first_date`.`year` AND t2.`month` = `first_date`.`month`",
        FIRST_TABLE_NAME, SECOND_TABLE_NAME);

    int actualRowCount = testSql(query);
    int expectedRowCount = 4;
    assertEquals("Expected and actual row count should match", expectedRowCount, actualRowCount);

    final String[] expectedPlan = {"first.*numRowGroups=2", "second.*numRowGroups=1"};
    testPlanMatchingPatterns(query, expectedPlan);
  }

  @Test
  public void testForTransitiveFilterPushPastAgg() throws Exception {
    String query = String.format("SELECT t1.`year` FROM %s t1 WHERE t1.`month` = 7 AND t1.`period` = 2 AND t1.`month` IN " +
        "(SELECT t2.`month` FROM %s t2)", FIRST_TABLE_NAME, SECOND_TABLE_NAME);

    // Validate the plan
    int actualRowCount = testSql(query);
    int expectedRowCount = 2;
    assertEquals("Expected and actual row count should match", expectedRowCount, actualRowCount);

    final String[] expectedPlan = {"first.*numRowGroups=1", "second.*numRowGroups=1"};
    testPlanMatchingPatterns(query, expectedPlan);
  }
}

