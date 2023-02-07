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
package org.apache.drill.exec.physical.impl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.drill.categories.FlakyTest;
import org.apache.drill.exec.expr.fn.impl.DateUtility;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.test.TestBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * For DRILL-6242, output for Date, Time, Timestamp should use different classes
 */
@Category(FlakyTest.class)
public class TestNestedDateTimeTimestamp extends BaseTestQuery {
  private static final String              DATAFILE       = "cp.`datetime.parquet`";
  private static final Map<String, Object> expectedRecord = new TreeMap<String, Object>();

  static {
    /**
     * Data in the parquet file represents this equivalent JSON, but with typed
     * data, time, and timestamps: { "date" : "1970-01-11", "time" :
     * "00:00:03.600", "timestamp" : "2018-03-23T17:40:52.123Z", "date_list" : [
     * "1970-01-11" ], "time_list" : [ "00:00:03.600" ], "timestamp_list" : [
     * "2018-03-23T17:40:52.123Z" ], "time_map" : { "date" : "1970-01-11",
     * "time" : "00:00:03.600", "timestamp" : "2018-03-23T17:40:52.123Z" } }
     *
     * Note that when the above data is read in to Drill, Drill modifies the
     * timestamp to local time zone, and preserving the <date> and <time>
     * values. This effectively changes the timestamp, if the time zone is not
     * UTC.
     */

    LocalDate date = DateUtility.parseLocalDate("1970-01-11");
    LocalTime time = DateUtility.parseLocalTime("00:00:03.600");
    LocalDateTime timestamp = DateUtility.parseLocalDateTime("2018-03-23 17:40:52.123");
    expectedRecord.put("`date`", date);
    expectedRecord.put("`time`", time);
    expectedRecord.put("`timestamp`", timestamp);
    expectedRecord.put("`date_list`", Arrays.asList(date));
    expectedRecord.put("`time_list`", Arrays.asList(time));
    expectedRecord.put("`timestamp_list`", Arrays.asList(timestamp));
    Map<String, Object> nestedMap = new TreeMap<String, Object>();
    nestedMap.put("date", date);
    nestedMap.put("time", time);
    nestedMap.put("timestamp", timestamp);

    expectedRecord.put("`time_map`", nestedMap);
  }

  /**
   * Test reading of from the parquet file that contains nested time, date, and
   * timestamp
   */
  @Test
  public void testNested() throws Exception {
    String query = String.format("select * from %s limit 1", DATAFILE);
    testBuilder().sqlQuery(query).ordered().baselineRecords(Arrays.asList(expectedRecord)).build().run();
  }

  /**
   * Test timeofday() function.
   */
  @Test
  public void testTimeOfDay() throws Exception {
    ZonedDateTime now = ZonedDateTime.now();
    // check the time zone
    testBuilder().sqlQuery("select substr(timeofday(),25) as n from (values(1))").ordered().baselineColumns("n")
        .baselineValues(DateUtility.formatTimeStampTZ.format(now).substring(24)).build().run();
  }

  /**
   * Test the textual display to make sure it is consistent with actual JSON
   * output
   */
  @Test
  public void testNestedDateTimePrint() throws Exception {
    List<QueryDataBatch> resultList = testSqlWithResults(String.format("select * from %s limit 1", DATAFILE));
    String actual = getResultString(resultList, " | ");

    final String expected = "date | time | timestamp | date_list | time_list | timestamp_list | time_map\n"
        + "1970-01-11 | 00:00:03.600 | 2018-03-23 17:40:52.123 | [\"1970-01-11\"] | [\"00:00:03.600\"] | [\"2018-03-23 17:40:52.123\"] | {\"date\":\"1970-01-11\",\"time\":\"00:00:03.600\",\"timestamp\":\"2018-03-23 17:40:52.123\"}";

    Assert.assertEquals(expected.trim(), actual.trim());
  }

  /**
   * Test the json output is consistent as before
   */
  @Test
  public void testNestedDateTimeCTASJson() throws Exception {
    String query = String.format("select * from %s limit 1", DATAFILE);
    String testName = "ctas_nested_datetime";
    try {
      test("alter session set `store.format` = 'json'");
      test("alter session set store.json.extended_types = false");
      test("use dfs.tmp");
      test("create table " + testName + "_json as " + query);

      final String readQuery = "select * from `" + testName + "_json` t1 ";

      testBuilder().sqlQuery(readQuery).ordered().jsonBaselineFile("baseline_nested_datetime.json").build().run();
    } finally {
      test("drop table " + testName + "_json");
      resetSessionOption("store.format");
      resetSessionOption("store.json.extended_types");
    }
  }

  /**
   * Test the extended json output is consistent as before
   */
  @Test
  public void testNestedDateTimeCTASExtendedJson() throws Exception {
    String query = String.format("select * from %s limit 1", DATAFILE);
    String testName = "ctas_nested_datetime_extended";
    try {
      test("alter session set `store.format` = 'json'");
      test("alter session set store.json.extended_types = true");
      test("use dfs.tmp");
      test("create table " + testName + "_json as " + query);

      final String readQuery = "select * from `" + testName + "_json` t1 ";

      testBuilder().sqlQuery(readQuery).ordered().jsonBaselineFile("datetime.parquet").build().run();
    } finally {
      test("drop table " + testName + "_json");
      resetSessionOption("store.format");
      resetSessionOption("store.json.extended_types");
    }
  }

  /**
   * Test parquet output is consistent as before
   */
  @Test
  public void testNestedDateTimeCTASParquet() throws Exception {
    String query = String.format("select * from %s limit 1", DATAFILE);
    String testName = "ctas_nested_datetime_extended";
    try {
      test("alter session set `store.format` = 'parquet'");
      test("use dfs.tmp");
      test("create table " + testName + "_parquet as " + query);

      final String readQuery = "select * from `" + testName + "_parquet` t1 ";

      testBuilder().sqlQuery(readQuery).ordered().jsonBaselineFile("datetime.parquet").build().run();
    } finally {
      test("drop table " + testName + "_parquet");
      resetSessionOption("store.format");
    }
  }

  /**
   * Testing time zone change and revert
   */
  @Test
  public void testTimeZoneChangeAndReverse() throws Exception {
    long timeMillis[] = new long[] { 864000000L, 3600L, 1521826852123L };

    for (int i = 0; i < timeMillis.length; i++) {
      OffsetDateTime time1 = OffsetDateTime.ofInstant(Instant.ofEpochMilli(timeMillis[i]), ZoneOffset.UTC);
      OffsetDateTime time2 = time1.toLocalDateTime().atZone(ZoneOffset.systemDefault()).toOffsetDateTime();
      OffsetDateTime time3 = time2.toLocalDateTime().atOffset(ZoneOffset.UTC);

      Assert.assertEquals(time1.toString(), time3.toString());
      Assert.assertEquals(time1.toString().substring(0, 16), time2.toString().substring(0, 16));
    }
  }

  @Test
  public void testDateUtilityParser() {
    LocalDateTime timestamp3 = TestBuilder.convertToLocalDateTime("1970-01-01 00:00:00.000");
    Assert.assertEquals(timestamp3,
        ZonedDateTime.ofInstant(Instant.parse("1970-01-01T00:00:00Z"), ZoneOffset.systemDefault()).toLocalDateTime());

    String in[] = new String[] {
        "1970-01-01",
        "1970-01-01 20:12:32",
        "1970-01-01 20:12:32.32",
        "1970-01-01 20:12:32.032",
        "1970-01-01 20:12:32.32 +0800",
        "1970-1-01",
        "1970-01-1 2:12:32",
        "1970-01-01 20:12:3.32",
        "1970-01-01 20:12:32.032",
        "1970-01-01 20:2:32.32 +0800" };
    for (String i : in) {
      LocalDateTime parsed = DateUtility.parseBest(i);
      Assert.assertNotNull(parsed);
    }

    // parse iso parser
    String isoTimestamp[] = new String[] {
        "2015-03-12T21:54:31.809+0530",
        "2015-03-12T21:54:31.809Z",
        "2015-03-12T21:54:31.809-0000",
        "2015-03-12T21:54:31.809-0800"
    };

    for (String s : isoTimestamp) {
      OffsetDateTime t = OffsetDateTime.parse(s, DateUtility.isoFormatTimeStamp);
      Assert.assertNotNull(t);
      Assert.assertNotNull(DateUtility.isoFormatTimeStamp.format(t));
    }
  }
}
