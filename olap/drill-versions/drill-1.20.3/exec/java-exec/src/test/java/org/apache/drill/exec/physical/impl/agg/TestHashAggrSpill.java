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
package org.apache.drill.exec.physical.impl.agg;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.drill.categories.OperatorTest;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.physical.config.HashAggregate;
import org.apache.drill.exec.physical.impl.aggregate.HashAggTemplate;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.test.BaseDirTestWatcher;
import org.apache.drill.test.ClientFixture;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.DrillTest;
import org.apache.drill.test.ProfileParser;
import org.apache.drill.test.QueryBuilder;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test spilling for the Hash Aggr operator (using the mock reader)
 */
@Category({SlowTest.class, OperatorTest.class})
public class TestHashAggrSpill extends DrillTest {

  // Matches the "2400K" in the table name.
  public static final int DEFAULT_ROW_COUNT = 2_400_000;

  @Rule
  public final BaseDirTestWatcher dirTestWatcher = new BaseDirTestWatcher();

  /**
   *  A template for Hash Aggr spilling tests
   */
  private void testSpill(long maxMem, long numPartitions, long minBatches, int maxParallel, boolean fallback, boolean predict,
                         String sql, long expectedRows, int cycle, int fromPart, int toPart) throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
      .sessionOption(ExecConstants.HASHAGG_MAX_MEMORY_KEY,maxMem)
      .sessionOption(ExecConstants.HASHAGG_NUM_PARTITIONS_KEY,numPartitions)
      .sessionOption(ExecConstants.HASHAGG_MIN_BATCHES_PER_PARTITION_KEY,minBatches)
      .configProperty(ExecConstants.SYS_STORE_PROVIDER_LOCAL_ENABLE_WRITE, false)
      .sessionOption(PlannerSettings.FORCE_2PHASE_AGGR_KEY,true)
      .sessionOption(ExecConstants.HASHAGG_FALLBACK_ENABLED_KEY, fallback)
      .sessionOption(ExecConstants.HASHAGG_USE_MEMORY_PREDICTION_KEY,predict)
      .maxParallelization(maxParallel)
      .saveProfiles();
    String sqlStr = sql != null ? sql :  // if null then use this default query
      "SELECT empid_s17, dept_i, branch_i, AVG(salary_i) FROM `mock`.`employee_2400K` GROUP BY empid_s17, dept_i, branch_i";

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      runAndDump(client, sqlStr, expectedRows, cycle, fromPart, toPart);
    }
  }

  /**
   * Test "normal" spilling: Only 2 (or 3) partitions (out of 4) would require spilling
   * ("normal spill" means spill-cycle = 1 )
   */
  @Test
  public void testSimpleHashAggrSpill() throws Exception {
    testSpill(68_000_000, 16, 2, 2, false, true, null,
        DEFAULT_ROW_COUNT, 1,2, 3);
  }

  /**
   * Test with "needed memory" prediction turned off
   * (i.e., exercise code paths that catch OOMs from the Hash Table and recover)
   */
  @Test
  @Ignore("DRILL-7301")
  public void testNoPredictHashAggrSpill() throws Exception {
    testSpill(135_000_000, 16, 2, 2, false, false /* no prediction */, null,
        DEFAULT_ROW_COUNT, 1, 1, 1);
  }

  private void runAndDump(ClientFixture client, String sql, long expectedRows, long spillCycle, long fromSpilledPartitions, long toSpilledPartitions) throws Exception {
    QueryBuilder.QuerySummary summary = client.queryBuilder().sql(sql).run();
    if (expectedRows > 0) {
      assertEquals(expectedRows, summary.recordCount());
    }

    ProfileParser profile = client.parseProfile(summary.queryIdString());
    List<ProfileParser.OperatorProfile> ops = profile.getOpsOfType(HashAggregate.OPERATOR_TYPE);

    assertFalse(ops.isEmpty());
    // check for the first op only
    ProfileParser.OperatorProfile hag0 = ops.get(0);
    long opCycle = hag0.getMetric(HashAggTemplate.Metric.SPILL_CYCLE.ordinal());
    assertEquals(spillCycle, opCycle);
    long op_spilled_partitions = hag0.getMetric(HashAggTemplate.Metric.SPILLED_PARTITIONS.ordinal());
    assertTrue(op_spilled_partitions >= fromSpilledPartitions);
    assertTrue(op_spilled_partitions <= toSpilledPartitions);
  }

  /**
   * Test Secondary and Tertiary spill cycles - Happens when some of the spilled
   * partitions cause more spilling as they are read back
   */
  @Test
  public void testHashAggrSecondaryTertiarySpill() throws Exception {

    testSpill(58_000_000, 16, 3, 1, false, true,
        "SELECT empid_s44, dept_i, branch_i, AVG(salary_i) FROM `mock`.`employee_1100K` GROUP BY empid_s44, dept_i, branch_i",
        1_100_000, 3, 2, 2);
  }

  /**
   * Test with the "fallback" option disabled: When not enough memory available
   * to allow spilling, then fail (Resource error) !!
   */
  @Test
  public void testHashAggrFailWithFallbackDisabed() throws Exception {

    try {
      testSpill(34_000_000, 4, 5, 2, false /* no fallback */, true, null,
          DEFAULT_ROW_COUNT, 0 /* no spill due to fallback to pre-1.11 */, 0, 0);
      fail(); // in case the above test did not throw
    } catch (Exception ex) {
      assertTrue(ex instanceof UserRemoteException);
      assertTrue(((UserRemoteException) ex).getErrorType() == UserBitShared.DrillPBError.ErrorType.RESOURCE);
      // must get here for the test to succeed ...
    }
  }

  /**
   * Test with the "fallback" option ON: When not enough memory is available to
   * allow spilling (internally need enough memory to create multiple
   * partitions), then behave like the pre-1.11 Hash Aggregate: Allocate
   * unlimited memory, no spill.
   */
  @Test
  public void testHashAggrSuccessWithFallbackEnabled() throws Exception {
    testSpill(34_000_000, 4, 5, 2, true /* do fallback */, true, null,
        DEFAULT_ROW_COUNT, 0 /* no spill due to fallback to pre-1.11 */, 0, 0);
  }
}
