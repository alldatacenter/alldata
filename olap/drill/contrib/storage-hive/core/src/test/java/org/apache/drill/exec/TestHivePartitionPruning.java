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
import static org.junit.Assert.assertTrue;

import org.apache.drill.categories.HiveStorageTest;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.exec.hive.HiveTestBase;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;

@Category({SlowTest.class, HiveStorageTest.class})
public class TestHivePartitionPruning extends HiveTestBase {
  // enable decimal data type
  @BeforeClass
  public static void enableDecimalDataType() throws Exception {
    test(String.format("alter session set `%s` = true", PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY));
  }

  //Currently we do not have a good way to test plans so using a crude string comparison
  @Test
  public void testSimplePartitionFilter() throws Exception {
    final String query = "explain plan for select * from hive.`default`.partition_pruning_test where c = 1";
    final String plan = getPlanInString(query, OPTIQ_FORMAT);

    // Check and make sure that Filter is not present in the plan
    assertFalse(plan.contains("Filter"));
  }

  /* Partition pruning is not supported for disjuncts that do not meet pruning criteria.
   * Will be enabled when we can do wild card comparison for partition pruning
   */
  @Test
  public void testDisjunctsPartitionFilter() throws Exception {
    final String query = "explain plan for select * from hive.`default`.partition_pruning_test where (c = 1) or (d = 1)";
    final String plan = getPlanInString(query, OPTIQ_FORMAT);

    // Check and make sure that Filter is not present in the plan
    assertFalse(plan.contains("Filter"));
  }

  @Test
  public void testConjunctsPartitionFilter() throws Exception {
    final String query = "explain plan for select * from hive.`default`.partition_pruning_test where c = 1 and d = 1";
    final String plan = getPlanInString(query, OPTIQ_FORMAT);

    // Check and make sure that Filter is not present in the plan
    assertFalse(plan.contains("Filter"));
  }

  @Test
  public void testComplexFilter() throws Exception {
    final String query = "explain plan for select * from hive.`default`.partition_pruning_test where (c = 1 and d = 1) or (c = 2 and d = 3)";
    final String plan = getPlanInString(query, OPTIQ_FORMAT);

    // Check and make sure that Filter is not present in the plan
    assertFalse(plan.contains("Filter"));
  }

  @Test
  public void testRangeFilter() throws Exception {
    final String query = "explain plan for " +
        "select * from hive.`default`.partition_pruning_test where " +
        "c > 1 and d > 1";

    final String plan = getPlanInString(query, OPTIQ_FORMAT);

    // Check and make sure that Filter is not present in the plan
    assertFalse(plan.contains("Filter"));
  }

  @Test
  public void testRangeFilterWithDisjunct() throws Exception {
    final String query = "explain plan for " +
        "select * from hive.`default`.partition_pruning_test where " +
        "(c > 1 and d > 1) or (c < 2 and d < 2)";

    final String plan = getPlanInString(query, OPTIQ_FORMAT);

    // Check and make sure that Filter is not present in the plan
    assertFalse(plan.contains("Filter"));
  }

  /**
   * Tests pruning on table that has partitions columns of supported data types. Also tests whether Hive pruning code
   * is able to deserialize the partition values in string format to appropriate type holder.
   */
  @Test
  public void pruneDataTypeSupport() throws Exception {
    final String query = "EXPLAIN PLAN FOR " +
        "SELECT * FROM hive.readtest WHERE tinyint_part = 64";

    final String plan = getPlanInString(query, OPTIQ_FORMAT);

    // Check and make sure that Filter is not present in the plan
    assertFalse(plan.contains("Filter"));
  }

  @Test // DRILL-3579
  public void selectFromPartitionedTableWithNullPartitions() throws Exception {
    final String query = "SELECT count(*) nullCount FROM hive.partition_pruning_test " +
        "WHERE c IS NULL OR d IS NULL OR e IS NULL";
    final String plan = getPlanInString("EXPLAIN PLAN FOR " + query, OPTIQ_FORMAT);

    // Check and make sure that Filter is not present in the plan
    assertFalse(plan.contains("Filter"));

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("nullCount")
        .baselineValues(95L)
        .go();
  }

  @Test // DRILL-5032
  public void testPartitionColumnsCaching() throws Exception {
    final String query = "EXPLAIN PLAN FOR SELECT * FROM hive.partition_with_few_schemas";

    List<QueryDataBatch> queryDataBatches = testSqlWithResults(query);
    String resultString = getResultString(queryDataBatches, "|");

    // different for both partitions column strings from physical plan
    String columnString = "\"name\" : \"a\"";
    String secondColumnString = "\"name\" : \"a1\"";

    int columnIndex = resultString.indexOf(columnString);
    assertTrue(columnIndex >= 0);
    columnIndex = resultString.indexOf(columnString, columnIndex + 1);
    // checks that column added to physical plan only one time
    assertEquals(-1, columnIndex);

    int secondColumnIndex = resultString.indexOf(secondColumnString);
    assertTrue(secondColumnIndex >= 0);
    secondColumnIndex = resultString.indexOf(secondColumnString, secondColumnIndex + 1);
    // checks that column added to physical plan only one time
    assertEquals(-1, secondColumnIndex);
  }

  @Test // DRILL-6173
  public void prunePartitionsBasedOnTransitivePredicates() throws Exception {
    String query = String.format("SELECT * FROM hive.partition_pruning_test t1 " +
            "JOIN hive.partition_with_few_schemas t2 ON t1.`d` = t2.`d` AND t1.`e` = t2.`e` " +
            "WHERE t2.`e` IS NOT NULL AND t1.`d` = 1");

    int actualRowCount = testSql(query);
    int expectedRowCount = 450;
    assertEquals("Expected and actual row count should match", expectedRowCount, actualRowCount);

    final String[] expectedPlan =
        {"partition_with_few_schemas.*numPartitions=6", "partition_pruning_test.*numPartitions=6"};
    testPlanMatchingPatterns(query, expectedPlan);
  }

  @AfterClass
  public static void disableDecimalDataType() throws Exception {
    test(String.format("alter session set `%s` = false", PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY));
  }
}
