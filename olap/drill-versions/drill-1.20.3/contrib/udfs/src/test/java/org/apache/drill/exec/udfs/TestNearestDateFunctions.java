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

package org.apache.drill.exec.udfs;

import org.apache.drill.categories.SqlFunctionTest;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Month;
import java.util.Arrays;

import java.time.LocalDateTime;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category({UnlikelyTest.class, SqlFunctionTest.class})
public class TestNearestDateFunctions extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    startCluster(builder);
  }

  @Test
  public void testNearestDate() throws Exception {
    String query = "SELECT nearestDate( TO_TIMESTAMP('2019-02-01 07:22:00', 'yyyy-MM-dd HH:mm:ss'), 'YEAR') AS nearest_year, " +
            "nearestDate( TO_TIMESTAMP('2019-02-01 07:22:00', 'yyyy-MM-dd HH:mm:ss'), 'QUARTER') AS nearest_quarter, " +
            "nearestDate( TO_TIMESTAMP('2019-02-15 07:22:00', 'yyyy-MM-dd HH:mm:ss'), 'MONTH') AS nearest_month, " +
            "nearestDate( TO_TIMESTAMP('2019-02-15 07:22:00', 'yyyy-MM-dd HH:mm:ss'), 'DAY') AS nearest_day, " +
            "nearestDate( TO_TIMESTAMP('2019-02-15 07:22:00', 'yyyy-MM-dd HH:mm:ss'), 'WEEK_SUNDAY') AS nearest_week_sunday, " +
            "nearestDate( TO_TIMESTAMP('2019-02-15 07:22:00', 'yyyy-MM-dd HH:mm:ss'), 'WEEK_MONDAY') AS nearest_week_monday, " +
            "nearestDate( TO_TIMESTAMP('2019-02-15 07:22:00', 'yyyy-MM-dd HH:mm:ss'), 'HOUR') AS nearest_hour, " +
            "nearestDate( TO_TIMESTAMP('2019-02-15 07:42:00', 'yyyy-MM-dd HH:mm:ss'), 'HALF_HOUR') AS nearest_half_hour, " +
            "nearestDate( TO_TIMESTAMP('2019-02-15 07:48:00', 'yyyy-MM-dd HH:mm:ss'), 'QUARTER_HOUR') AS nearest_quarter_hour, " +
            "nearestDate( TO_TIMESTAMP('2019-02-15 07:22:00', 'yyyy-MM-dd HH:mm:ss'), 'MINUTE') AS nearest_minute, " +
            "nearestDate( TO_TIMESTAMP('2019-02-15 07:22:22', 'yyyy-MM-dd HH:mm:ss'), 'HALF_MINUTE') AS nearest_30second, " +
            "nearestDate( TO_TIMESTAMP('2019-02-15 07:22:22', 'yyyy-MM-dd HH:mm:ss'), 'QUARTER_MINUTE') AS nearest_15second, " +
            "nearestDate( TO_TIMESTAMP('2019-02-15 07:22:31', 'yyyy-MM-dd HH:mm:ss'), 'SECOND') AS nearest_second " +
            "FROM (VALUES(1))";
    testBuilder()
            .sqlQuery(query)
            .unOrdered()
            .baselineColumns("nearest_year",
                    "nearest_quarter",
                    "nearest_month",
                    "nearest_day",
                    "nearest_week_sunday",
                    "nearest_week_monday",
                    "nearest_hour",
                    "nearest_half_hour",
                    "nearest_quarter_hour",
                    "nearest_minute",
                    "nearest_30second",
                    "nearest_15second",
                    "nearest_second")
            .baselineValues(LocalDateTime.of(2019, 1, 1, 0, 0, 0),  //Year
                    LocalDateTime.of(2019, 1, 1, 0, 0, 0),  //Quarter
                    LocalDateTime.of(2019, 2, 1, 0, 0, 0), //Month
                    LocalDateTime.of(2019, 2, 15, 0, 0, 0), //Day
                    LocalDateTime.of(2019, 2, 10, 0, 0, 0), //Week Sunday
                    LocalDateTime.of(2019, 2, 11, 0, 0, 0), //Week Monday
                    LocalDateTime.of(2019, 2, 15, 7, 0, 0), //Hour
                    LocalDateTime.of(2019, 2, 15, 7, 30, 0), //Half Hour
                    LocalDateTime.of(2019, 2, 15, 7, 45, 0), //Quarter Hour
                    LocalDateTime.of(2019, 2, 15, 7, 22, 0), //Minute
                    LocalDateTime.of(2019, 2, 15, 7, 22, 0), //30Second
                    LocalDateTime.of(2019, 2, 15, 7, 22, 15), //15Second
                    LocalDateTime.of(2019, 2, 15, 7, 22, 31)) //Second
            .go();
  }

  @Test
  public void testNearestDateWithTimestamp() throws Exception {
    String query = "SELECT nearestDate( '2019-02-01 07:22:00', 'yyyy-MM-dd HH:mm:ss', 'YEAR') AS nearest_year, " +
            "nearestDate( '2019-02-01 07:22:00', 'yyyy-MM-dd HH:mm:ss', 'QUARTER') AS nearest_quarter, " +
            "nearestDate( '2019-02-15 07:22:00', 'yyyy-MM-dd HH:mm:ss', 'MONTH') AS nearest_month, " +
            "nearestDate( '2019-02-15 07:22:00', 'yyyy-MM-dd HH:mm:ss', 'DAY') AS nearest_day, " +
            "nearestDate( '2019-02-15 07:22:00', 'yyyy-MM-dd HH:mm:ss', 'WEEK_SUNDAY') AS nearest_week_sunday, " +
            "nearestDate( '2019-02-15 07:22:00', 'yyyy-MM-dd HH:mm:ss', 'WEEK_MONDAY') AS nearest_week_monday, " +
            "nearestDate( '2019-02-15 07:22:00', 'yyyy-MM-dd HH:mm:ss', 'HOUR') AS nearest_hour, " +
            "nearestDate( '2019-02-15 07:42:00', 'yyyy-MM-dd HH:mm:ss', 'HALF_HOUR') AS nearest_half_hour, " +
            "nearestDate( '2019-02-15 07:48:00', 'yyyy-MM-dd HH:mm:ss', 'QUARTER_HOUR') AS nearest_quarter_hour, " +
            "nearestDate( '2019-02-15 07:22:00', 'yyyy-MM-dd HH:mm:ss', 'MINUTE') AS nearest_minute, " +
            "nearestDate( '2019-02-15 07:22:22', 'yyyy-MM-dd HH:mm:ss', 'HALF_MINUTE') AS nearest_30second, " +
            "nearestDate( '2019-02-15 07:22:22', 'yyyy-MM-dd HH:mm:ss', 'QUARTER_MINUTE') AS nearest_15second, " +
            "nearestDate( '2019-02-15 07:22:31', 'yyyy-MM-dd HH:mm:ss', 'SECOND') AS nearest_second " +
            "FROM (VALUES(1))";
    testBuilder()
            .sqlQuery(query)
            .unOrdered()
            .baselineColumns("nearest_year",
                    "nearest_quarter",
                    "nearest_month",
                    "nearest_day",
                    "nearest_week_sunday",
                    "nearest_week_monday",
                    "nearest_hour",
                    "nearest_half_hour",
                    "nearest_quarter_hour",
                    "nearest_minute",
                    "nearest_30second",
                    "nearest_15second",
                    "nearest_second")
            .baselineValues(LocalDateTime.of(2019, 1, 1, 0, 0, 0),  //Year
                    LocalDateTime.of(2019, 1, 1, 0, 0, 0),  //Quarter
                    LocalDateTime.of(2019, 2, 1, 0, 0, 0), //Month
                    LocalDateTime.of(2019, 2, 15, 0, 0, 0), //Day
                    LocalDateTime.of(2019, 2, 10, 0, 0, 0), //Week Sunday
                    LocalDateTime.of(2019, 2, 11, 0, 0, 0), //Week Monday
                    LocalDateTime.of(2019, 2, 15, 7, 0, 0), //Hour
                    LocalDateTime.of(2019, 2, 15, 7, 30, 0), //Half Hour
                    LocalDateTime.of(2019, 2, 15, 7, 45, 0), //Quarter Hour
                    LocalDateTime.of(2019, 2, 15, 7, 22, 0), //Minute
                    LocalDateTime.of(2019, 2, 15, 7, 22, 0), //30Second
                    LocalDateTime.of(2019, 2, 15, 7, 22, 15), //15Second
                    LocalDateTime.of(2019, 2, 15, 7, 22, 31)) //Second
            .go();
  }

  @Test
  public void testReadException() throws Exception {
    String query = "SELECT nearestDate( TO_TIMESTAMP('2019-02-01 07:22:00', 'yyyy-MM-dd HH:mm:ss'), 'BAD_DATE') AS nearest_year " +
            "FROM (VALUES(1))";
    try {
      run(query);
      fail();
    } catch (DrillRuntimeException e) {
      assertTrue(e.getMessage().contains("[BAD_DATE] is not a valid time statement. Expecting: " + Arrays.asList(NearestDateUtils.TimeInterval.values())));
    }
  }

  @Test
  public void testDRILL7781() throws Exception {
    LocalDateTime q1 = LocalDateTime.of(2019, Month.JANUARY, 1, 0, 0, 0);
    LocalDateTime q2 = LocalDateTime.of(2019, Month.APRIL, 1, 0, 0, 0);
    LocalDateTime q3 = LocalDateTime.of(2019, Month.JULY, 1, 0, 0, 0);
    LocalDateTime q4 = LocalDateTime.of(2019, Month.OCTOBER, 1, 0, 0, 0);

    String query = "SELECT nearestDate( TO_TIMESTAMP('2019-01-02 07:22:00', 'yyyy-MM-dd HH:mm:ss'), 'QUARTER') AS january_quarter, " +
        "nearestDate( TO_TIMESTAMP('2019-02-02 07:22:00', 'yyyy-MM-dd HH:mm:ss'), 'QUARTER') AS february_quarter, " +
        "nearestDate( TO_TIMESTAMP('2019-03-02 07:22:00', 'yyyy-MM-dd HH:mm:ss'), 'QUARTER') AS march_quarter, " +
        "nearestDate( TO_TIMESTAMP('2019-04-02 07:22:00', 'yyyy-MM-dd HH:mm:ss'), 'QUARTER') AS april_quarter, " +
        "nearestDate( TO_TIMESTAMP('2019-05-02 07:22:00', 'yyyy-MM-dd HH:mm:ss'), 'QUARTER') AS may_quarter, " +
        "nearestDate( TO_TIMESTAMP('2019-06-02 07:22:00', 'yyyy-MM-dd HH:mm:ss'), 'QUARTER') AS june_quarter, " +
        "nearestDate( TO_TIMESTAMP('2019-07-02 07:22:00', 'yyyy-MM-dd HH:mm:ss'), 'QUARTER') AS july_quarter, " +
        "nearestDate( TO_TIMESTAMP('2019-08-02 07:22:00', 'yyyy-MM-dd HH:mm:ss'), 'QUARTER') AS august_quarter, " +
        "nearestDate( TO_TIMESTAMP('2019-09-02 07:22:00', 'yyyy-MM-dd HH:mm:ss'), 'QUARTER') AS september_quarter, " +
        "nearestDate( TO_TIMESTAMP('2019-10-02 07:22:00', 'yyyy-MM-dd HH:mm:ss'), 'QUARTER') AS october_quarter, " +
        "nearestDate( TO_TIMESTAMP('2019-11-02 07:22:00', 'yyyy-MM-dd HH:mm:ss'), 'QUARTER') AS november_quarter, " +
        "nearestDate( TO_TIMESTAMP('2019-12-02 07:22:00', 'yyyy-MM-dd HH:mm:ss'), 'QUARTER') AS december_quarter " +
        "FROM (VALUES(1))";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("january_quarter",
            "february_quarter",
            "march_quarter",
            "april_quarter",
            "may_quarter",
            "june_quarter",
            "july_quarter",
            "august_quarter",
            "september_quarter",
            "october_quarter",
            "november_quarter",
            "december_quarter")
        .baselineValues(q1, q1, q1,
            q2, q2, q2,
            q3, q3, q3,
            q4, q4, q4)
        .go();
  }
}
