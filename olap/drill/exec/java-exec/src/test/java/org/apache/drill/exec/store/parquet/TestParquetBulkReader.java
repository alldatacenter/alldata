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

import org.apache.drill.categories.ParquetTest;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/** Tests the Parquet bulk reader */
@Category({ParquetTest.class, UnlikelyTest.class})
public class TestParquetBulkReader extends ClusterTest {


  @BeforeClass
  public static void setup() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    startCluster(builder);
  }

  private static final String DATAFILE = "cp.`parquet/fourvarchar_asc_nulls.parquet`";

  /** Load variable length data which has nulls */
  @Test
  public void testNullCount() throws Exception {
    try {
      alterSession();

      testBuilder()
        .sqlQuery("select count(*) as c from %s where VarbinaryValue3 is null", DATAFILE)
        .unOrdered()
        .baselineColumns("c")
        .baselineValues(71L)
        .go();

      testBuilder()
        .sqlQuery("select count(*) as c from %s where VarbinaryValue1 is null", DATAFILE)
        .unOrdered()
        .baselineColumns("c")
        .baselineValues(44L)
        .go();
    } finally {
      resetSession();
    }
  }

  /** Load variable length data which has non-nulls data */
  @Test
  public void testNotNullCount() throws Exception {
    try {
      alterSession();

      testBuilder()
        .sqlQuery("select count(*) as c from %s where VarbinaryValue3 is not null", DATAFILE)
        .unOrdered()
        .baselineColumns("c")
        .baselineValues(0L)
        .go();

      testBuilder()
        .sqlQuery("select count(*) as c from %s where VarbinaryValue1 is not null", DATAFILE)
        .unOrdered()
        .baselineColumns("c")
        .baselineValues(27L)
        .go();
    } finally {
      resetSession();
    }
  }

  /** Load variable columns with fixed length data with large precision and null values */
  @Test
  public void testFixedLengthWithLargePrecisionAndNulls() throws Exception {
    try {
      alterSession();

      testBuilder()
        .sqlQuery("select count(*) as c from %s where index < 50 and length(VarbinaryValue1) = 400", DATAFILE)
        .unOrdered()
        .baselineColumns("c")
        .baselineValues(25L)
        .go();
    } finally {
      resetSession();
    }
  }

  /** Load variable length data which was originally fixed length and then became variable length */
  @Test
  public void testFixedLengthToVarlen() throws Exception {
    try {
      alterSession();

      testBuilder()
        .sqlQuery("select count(*) as c from %s where index < 60 and length(VarbinaryValue1) <= 800", DATAFILE)
        .unOrdered()
        .baselineColumns("c")
        .baselineValues(27L)
        .go();
    } finally {
      resetSession();
    }
  }

  /** Load variable length data with values larger than chunk size (4k) */
  @Test
  public void testLargeVarlen() throws Exception {
    try {
      alterSession();

      testBuilder()
        .sqlQuery("select count(*) as c from %s where length(VarbinaryValue2) = 4500", DATAFILE)
        .unOrdered()
        .baselineColumns("c")
        .baselineValues(19L)
        .go();
    } finally {
      resetSession();
    }
  }

  private void alterSession() {
    client.alterSession(ExecConstants.PARQUET_FLAT_READER_BULK, true);
    client.alterSession(PlannerSettings.PARQUET_ROWGROUP_FILTER_PUSHDOWN_PLANNING_KEY, false);
  }

  private void resetSession() {
    client.resetSession(ExecConstants.PARQUET_FLAT_READER_BULK);
    client.resetSession(PlannerSettings.PARQUET_ROWGROUP_FILTER_PUSHDOWN_PLANNING_KEY);
  }

}
