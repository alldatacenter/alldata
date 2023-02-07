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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestParquetFilterPushDownForDateTimeCasts extends PlanTestBase {

  private static final String TABLE_NAME = "dateTimeCasts";

  @BeforeClass
  public static void init() throws Exception {
    test("use dfs.tmp");
    test("create table `%s/p1` as\n" +
        "select timestamp '2017-01-01 00:00:00' as col_timestamp, date '2017-01-01' as col_date, time '00:00:00' as col_time from (values(1)) union all\n" +
        "select timestamp '2017-01-02 00:00:00' as col_timestamp, date '2017-01-02' as col_date, time '00:00:00' as col_time from (values(1)) union all\n" +
        "select timestamp '2017-01-02 21:01:15' as col_timestamp, date '2017-01-02' as col_date, time '21:01:15' as col_time from (values(1))", TABLE_NAME);

    test("create table `%s/p2` as\n" +
        "select timestamp '2017-01-03 08:50:00' as col_timestamp, date '2017-01-03' as col_date, time '08:50:00' as col_time from (values(1)) union all\n" +
        "select timestamp '2017-01-04 15:25:00' as col_timestamp, date '2017-01-04' as col_date, time '15:25:00' as col_time from (values(1)) union all\n" +
        "select timestamp '2017-01-04 22:14:29' as col_timestamp, date '2017-01-04' as col_date, time '22:14:29' as col_time from (values(1))", TABLE_NAME);

    test("create table `%s/p3` as\n" +
        "select timestamp '2017-01-05 05:46:11' as col_timestamp, date '2017-01-05' as col_date, time '05:46:11' as col_time from (values(1)) union all\n" +
        "select timestamp '2017-01-06 06:17:59' as col_timestamp, date '2017-01-06' as col_date, time '06:17:59' as col_time from (values(1)) union all\n" +
        "select timestamp '2017-01-06 06:17:59' as col_timestamp, date '2017-01-06' as col_date, time '06:17:59' as col_time from (values(1)) union all\n" +
        "select cast(null as timestamp) as col_timestamp, cast(null as date) as col_date, cast(null as time) as col_time from (values(1))", TABLE_NAME);
  }

  @AfterClass
  public static void tearDown() throws Exception {
    test("drop table if exists `%s`", TABLE_NAME);
  }

  @Test
  public void testCastTimestampVarchar() throws Exception {
    testParquetFilterPushDown("col_timestamp = '2017-01-05 05:46:11'", 1, 1);
    testParquetFilterPushDown("col_timestamp = cast('2017-01-05 05:46:11' as varchar)", 1, 1);
    testParquetFilterPushDown("col_timestamp = cast('2017-01-05 05:46:11' as timestamp)", 1, 1);
    testParquetFilterPushDown("col_timestamp > '2017-01-02 00:00:00'", 7, 3);
    testParquetFilterPushDown("col_timestamp between '2017-01-03 21:01:15' and '2017-01-06 05:46:11'", 3, 2);
    testParquetFilterPushDown("col_timestamp between '2017-01-03' and '2017-01-06'", 4, 2);
  }

  @Test
  public void testCastTimestampDate() throws Exception {
    testParquetFilterPushDown("col_timestamp = date '2017-01-02'", 1, 1);
    testParquetFilterPushDown("col_timestamp = cast(date '2017-01-02' as timestamp)", 1, 1);
    testParquetFilterPushDown("col_timestamp > date '2017-01-02'", 7, 3);
    testParquetFilterPushDown("col_timestamp between date '2017-01-03' and date '2017-01-06'", 4, 2);
  }

  @Test
  public void testCastDateVarchar() throws Exception {
    testParquetFilterPushDown("col_date = '2017-01-02'", 2, 1);
    testParquetFilterPushDown("col_date = cast('2017-01-02' as varchar)", 2, 1);
    testParquetFilterPushDown("col_date = cast('2017-01-02' as date)", 2, 1);
    testParquetFilterPushDown("col_date > '2017-01-02'", 6, 2);
    testParquetFilterPushDown("col_date between '2017-01-02' and '2017-01-04'", 5, 2);
  }

  @Test
  public void testCastDateTimestamp() throws Exception {
    testParquetFilterPushDown("col_date = timestamp '2017-01-02 00:00:00'", 2, 1);
    testParquetFilterPushDown("col_date = cast(timestamp '2017-01-02 00:00:00' as date)", 2, 1);
    testParquetFilterPushDown("col_date > timestamp '2017-01-02 21:01:15'", 6, 2);
    testParquetFilterPushDown("col_date between timestamp '2017-01-03 08:50:00' and timestamp '2017-01-06 06:17:59'", 5, 2);
  }

  @Test
  public void testCastTimeVarchar() throws Exception {
    testParquetFilterPushDown("col_time = '00:00:00'", 2, 1);
    testParquetFilterPushDown("col_time = cast('00:00:00' as varchar)", 2, 1);
    testParquetFilterPushDown("col_time = cast('00:00:00' as time)", 2, 1);
    testParquetFilterPushDown("col_time > '15:25:00'", 2, 2);
    testParquetFilterPushDown("col_time between '08:00:00' and '23:00:00'", 4, 2);
  }

  @Test
  public void testCastTimeTimestamp() throws Exception {
    testParquetFilterPushDown("col_time = timestamp '2017-01-01 05:46:11'", 1, 2);
    testParquetFilterPushDown("col_time = cast(timestamp '2017-01-01 05:46:11' as time)", 1, 2);
    testParquetFilterPushDown("col_time = timestamp '2017-01-01 00:00:00'", 2, 1);
    testParquetFilterPushDown("col_time > timestamp '2017-01-01 15:25:00'", 2, 2);
    testParquetFilterPushDown("col_time between timestamp '2017-01-01 08:00:00' and timestamp '2017-01-01 23:00:00'", 4, 2);
  }

  @Test
  public void testCastTimeDate() throws Exception {
    testParquetFilterPushDown("col_time = date '2017-01-01'", 2, 1);
    testParquetFilterPushDown("col_time = cast(date '2017-01-01' as time)", 2, 1);
    testParquetFilterPushDown("col_time > date '2017-01-01'", 7, 3);
    testParquetFilterPushDown("col_time between date '2017-01-01' and date '2017-01-02'", 2, 1);
  }

  private void testParquetFilterPushDown(String predicate, int expectedRowCount, int expectedFilesNumber) throws Exception {
    String query = String.format("select * from `%s` where %s", TABLE_NAME, predicate);

    int actualRowCount = testSql(query);
    assertEquals("Expected and actual row count should match", expectedRowCount, actualRowCount);

    String numFilesPattern = "numFiles=" + expectedFilesNumber;
    testPlanMatchingPatterns(query, new String[] {numFilesPattern}, new String[] {});
  }

}