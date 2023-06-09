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
package org.apache.drill.exec.fn.impl;

import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestTimestampAddDiffFunctions extends ClusterTest {

  private final List<String> QUALIFIERS = Arrays.asList(
      "FRAC_SECOND",
      "MICROSECOND",
      "NANOSECOND",
      "SQL_TSI_FRAC_SECOND",
      "SQL_TSI_MICROSECOND",
      "SECOND",
      "SQL_TSI_SECOND",
      "MINUTE",
      "SQL_TSI_MINUTE",
      "HOUR",
      "SQL_TSI_HOUR",
      "DAY",
      "SQL_TSI_DAY",
      "WEEK",
      "SQL_TSI_WEEK",
      "MONTH",
      "SQL_TSI_MONTH",
      "QUARTER",
      "SQL_TSI_QUARTER",
      "YEAR",
      "SQL_TSI_YEAR");

  @BeforeClass
  public static void setup() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    startCluster(builder);
  }

  @Test // DRILL-3610
  public void testTimestampAddDiffLiteralTypeInference() throws Exception {
    Map<String, String> dateTypes = new HashMap<>();
    dateTypes.put("DATE", "2013-03-31");
    dateTypes.put("TIME", "00:02:03.123");
    dateTypes.put("TIMESTAMP", "2013-03-31 00:02:03");

    for (String qualifier : QUALIFIERS) {
      for (Map.Entry<String, String> typeResultPair : dateTypes.entrySet()) {
        String dateTimeLiteral = typeResultPair.getValue();
        String type = typeResultPair.getKey();

        run("SELECT TIMESTAMPADD(%s, 0, CAST('%s' AS %s)) col1", qualifier, dateTimeLiteral, type);

        // TIMESTAMPDIFF with args of different types
        for (Map.Entry<String, String> secondArg : dateTypes.entrySet()) {
          run("SELECT TIMESTAMPDIFF(%s, CAST('%s' AS %s), CAST('%s' AS %s)) col1",
              qualifier, dateTimeLiteral, type, secondArg.getValue(), secondArg.getKey());
        }
      }
    }
  }

  @Test // DRILL-3610
  public void testTimestampAddDiffTypeInference() throws Exception {
    for (String qualifier : QUALIFIERS) {
      run("SELECT TIMESTAMPADD(%1$s, 0, `date`) col1," +
                "TIMESTAMPADD(%1$s, 0, `time`) timeReq," +
                "TIMESTAMPADD(%1$s, 0, `timestamp`) timestampReq," +
                "TIMESTAMPADD(%1$s, 0, t.time_map.`date`) dateOpt," +
                "TIMESTAMPADD(%1$s, 0, t.time_map.`time`) timeOpt," +
                "TIMESTAMPADD(%1$s, 0, t.time_map.`timestamp`) timestampOpt\n" +
          "FROM cp.`datetime.parquet` t", qualifier);

      run("SELECT TIMESTAMPDIFF(%1$s, `date`, `date`) col1," +
                "TIMESTAMPDIFF(%1$s, `time`, `time`) timeReq," +
                "TIMESTAMPDIFF(%1$s, `timestamp`, `timestamp`) timestampReq," +
                "TIMESTAMPDIFF(%1$s, `timestamp`, t.time_map.`date`) timestampReqTimestampOpt," +
                "TIMESTAMPDIFF(%1$s, `timestamp`, t.time_map.`timestamp`) timestampReqTimestampOpt," +
                "TIMESTAMPDIFF(%1$s, `date`, `time`) timeDate," +
                "TIMESTAMPDIFF(%1$s, `time`, `date`) Datetime," +
                "TIMESTAMPDIFF(%1$s, t.time_map.`date`, t.time_map.`date`) dateOpt," +
                "TIMESTAMPDIFF(%1$s, t.time_map.`time`, t.time_map.`time`) timeOpt," +
                "TIMESTAMPDIFF(%1$s, t.time_map.`timestamp`, t.time_map.`timestamp`) timestampOpt\n" +
          "FROM cp.`datetime.parquet` t", qualifier);
    }
  }

  @Test // DRILL-3610
  public void testTimestampAddParquet() throws Exception {
    String query =
        "SELECT TIMESTAMPADD(SECOND, 1, `date`) dateReq," +
              "TIMESTAMPADD(QUARTER, 1, `time`) timeReq," +
              "TIMESTAMPADD(DAY, 1, `timestamp`) timestampReq," +
              "TIMESTAMPADD(MONTH, 1, t.time_map.`date`) dateOpt," +
              "TIMESTAMPADD(HOUR, 1, t.time_map.`time`) timeOpt," +
              "TIMESTAMPADD(YEAR, 1, t.time_map.`timestamp`) timestampOpt\n" +
        "FROM cp.`datetime.parquet` t";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("dateReq", "timeReq", "timestampReq", "dateOpt", "timeOpt", "timestampOpt")
        .baselineValues(
            LocalDateTime.parse("1970-01-11T00:00:01"), LocalTime.parse("00:00:03.600"), LocalDateTime.parse("2018-03-24T17:40:52.123"),
            LocalDateTime.parse("1970-02-11T00:00"), LocalTime.parse("01:00:03.600"), LocalDateTime.parse("2019-03-23T17:40:52.123"))
        .go();
  }

  @Test // DRILL-3610
  public void testTimestampDiffParquet() throws Exception {
    String query =
        "SELECT TIMESTAMPDIFF(SECOND, DATE '1970-01-15', `date`) dateReq," +
            "TIMESTAMPDIFF(QUARTER, TIME '12:00:03.600', `time`) timeReq," +
            "TIMESTAMPDIFF(DAY, TIMESTAMP '2018-03-24 17:40:52.123', `timestamp`) timestampReq," +
            "TIMESTAMPDIFF(MONTH, DATE '1971-10-30', t.time_map.`date`) dateOpt," +
            "TIMESTAMPDIFF(HOUR, TIME '18:00:03.600', t.time_map.`time`) timeOpt," +
            "TIMESTAMPDIFF(YEAR, TIMESTAMP '2020-03-24 17:40:52.123', t.time_map.`timestamp`) timestampOpt\n" +
        "FROM cp.`datetime.parquet` t";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("dateReq", "timeReq", "timestampReq", "dateOpt", "timeOpt", "timestampOpt")
        .baselineValues(-345600L, 0L, -1L, -21L, -18L, -2L)
        .go();
  }

  @Test // DRILL-3610
  public void testTimestampAddDiffNull() throws Exception {
    String query =
        "SELECT TIMESTAMPDIFF(SECOND, DATE '1970-01-15', a) col1," +
              "TIMESTAMPDIFF(QUARTER, a, DATE '1970-01-15') col2," +
              "TIMESTAMPDIFF(DAY, a, a) col3," +
              "TIMESTAMPADD(MONTH, 1, a) col4," +
              "TIMESTAMPADD(MONTH, b, DATE '1970-01-15') col5," +
              "TIMESTAMPADD(MONTH, b, a) col6\n" +
        "FROM" +
            "(SELECT CASE WHEN FALSE THEN TIME '12:00:03.600' ELSE null END AS a," +
            "CASE WHEN FALSE THEN 2 ELSE null END AS b)";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("col1", "col2", "col3", "col4", "col5", "col6")
        .baselineValues(null, null, null, null, null, null)
        .go();
  }

  @Test // DRILL-3610
  public void testTimestampDiffTimeDateTransition() throws Exception {
    String query =
        "SELECT TIMESTAMPDIFF(SECOND, time '12:30:00.123', time '12:30:00') col1," +
              "TIMESTAMPDIFF(DAY, TIMESTAMP '1970-01-15 15:30:00', TIMESTAMP '1970-01-16 12:30:00') col2," +
              "TIMESTAMPDIFF(DAY, TIMESTAMP '1970-01-16 12:30:00', TIMESTAMP '1970-01-15 15:30:00') col3," +
              "TIMESTAMPDIFF(MONTH, TIMESTAMP '1970-01-16 12:30:00', TIMESTAMP '1970-03-15 15:30:00') col4," +
              "TIMESTAMPDIFF(MONTH, TIMESTAMP '1970-03-15 15:30:00', TIMESTAMP '1970-01-16 12:30:00') col5," +
              "TIMESTAMPDIFF(DAY, DATE '2012-01-01', DATE '2013-01-01') col6," +
              "TIMESTAMPDIFF(DAY, DATE '2013-01-01', DATE '2014-01-01') col7";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("col1", "col2", "col3", "col4", "col5", "col6", "col7")
        .baselineValues(0L, 0L, 0L, 1L, -1L, 366L, 365L)
        .go();
  }

  @Test // DRILL-6967
  public void testTimestampDiffQuarter() throws Exception {
    String query =
        "SELECT TIMESTAMPDIFF(SQL_TSI_QUARTER, date '1996-03-09', date '1998-03-09') AS col1," +
                "TIMESTAMPDIFF(QUARTER, date '2019-01-01', date '2019-01-17') AS col2," +
                "TIMESTAMPDIFF(SQL_TSI_QUARTER, date '2019-01-01', date '2019-03-31') AS col3," +
                "TIMESTAMPDIFF(QUARTER, date '2019-01-01', date '2019-04-01') AS col4," +
                "TIMESTAMPDIFF(SQL_TSI_QUARTER, date '1970-01-01', date '2019-01-11') AS col5";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("col1", "col2", "col3", "col4", "col5")
        .baselineValues(8L, 0L, 0L, 1L, 196L)
        .go();
  }
}
