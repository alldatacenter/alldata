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
package org.apache.drill.exec.fn.impl.testing;

import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.categories.SqlFunctionTest;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.common.exceptions.UserException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

@Category({UnlikelyTest.class, SqlFunctionTest.class})
public class TestDateConversions extends BaseTestQuery {

  private static final String ENABLE_CAST_EMPTY_STRING_AS_NULL_QUERY =
      "alter system set `drill.exec.functions.cast_empty_string_to_null` = true;";

  @BeforeClass
  public static void generateTestFiles() throws IOException {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dirTestWatcher.getRootDir(), "joda_postgres_date.json")))) {
      writer.write("{\"date1\" : \"1970-01-02\",\n \"date2\" : \"01021970\",\n \"date3\" : \"32/1970\"\n}\n"
        + "{\"date1\" : \"2010-05-03\",\n \"date2\" : \"01021970\",\n \"date3\" : \"64/2010\"\n}");
    }

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dirTestWatcher.getRootDir(), "joda_postgres_time.json")))) {
      writer.write("{\"time1\" : \"23:11:59\",\n \"time2\" : \"11:11:59pm\",\n \"time3\" : \"591111pm\"\n}\n"
        + "{\"time1\" : \"17:33:41\",\n \"time2\" : \"5:33:41am\",\n \"time3\" : \"413305pm\"\n}");
    }

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dirTestWatcher.getRootDir(), "joda_postgres_date_time.json")))) {
      writer.write("{ \"time1\" : \"1970-01-0223:11:59\",\n \"time2\" : \"0102197011:11:59pm\",\n"
        + "  \"time3\" : \"32/1970591111pm\"\n}\n"
        + "{\"time1\" : \"2010-05-0317:33:41\",\n \"time2\" : \"0102197005:33:41am\",\n"
        + "  \"time3\" : \"64/2010413305pm\"\n}");
    }
  }

  @Test
  public void testJodaDate() throws Exception {
    testBuilder()
      .sqlQuery("SELECT to_date(date1, 'yyyy-dd-MM') = "
        + "to_date(date2, 'ddMMyyyy') as col1, " + "to_date(date1, 'yyyy-dd-MM') = "
        + "to_date(date3, 'D/yyyy') as col2 "
        + "from dfs.`joda_postgres_date.json`")
      .unOrdered()
      .baselineColumns("col1", "col2")
      .baselineValues(true, true)
      .baselineValues(false, true)
      .go();
  }

  @Test
  public void testPostgresDate() throws Exception {
    testBuilder()
      .sqlQuery("SELECT sql_to_date(date1, 'yyyy-DD-MM') = "
        + "sql_to_date(date2, 'DDMMyyyy') as col1, "
        + "sql_to_date(date1, 'yyyy-DD-MM') = "
        + "sql_to_date(date3, 'DDD/yyyy') as col2 "
        + "from dfs.`joda_postgres_date.json`")
      .unOrdered()
      .baselineColumns("col1", "col2")
      .baselineValues(true, true)
      .baselineValues(false, true)
      .go();
  }

  @Test
  public void testJodaTime() throws Exception {
    mockUsDateFormatSymbols();

    testBuilder()
      .sqlQuery("SELECT to_time(time1, 'H:m:ss') = "
        + "to_time(time2, 'h:m:ssa') as col1, "
        + "to_time(time1, 'H:m:ss') = "
        + "to_time(time3, 'ssmha') as col2 "
        + "from dfs.`joda_postgres_time.json`")
      .unOrdered()
      .baselineColumns("col1", "col2")
      .baselineValues(true, true)
      .baselineValues(false, true)
      .go();
  }

  @Test
  public void testToTimeWithDateTimePatternFormat() throws Exception {
    mockUsDateFormatSymbols();

    testBuilder()
        .sqlQuery("select TO_TIME('2016-03-03 00:00', 'yyyy-MM-dd HH:mm') as `result` from (values(1))")
        .unOrdered()
        .baselineColumns("result")
        .baselineValues(LocalTime.of(0,0,0))
        .go();
  }

  @Test
  public void testPostgresTime() throws Exception {
    mockUsDateFormatSymbols();

    testBuilder()
      .sqlQuery("SELECT sql_to_time(time1, 'HH24:MI:SS') = "
        + "sql_to_time(time2, 'HH12:MI:SSam') as col1, "
        + "sql_to_time(time1, 'HH24:MI:SS') = "
        + "sql_to_time(time3, 'SSMIHH12am') as col2 "
        + "from dfs.`joda_postgres_time.json`")
      .unOrdered()
      .baselineColumns("col1", "col2")
      .baselineValues(true, true)
      .baselineValues(false, true)
      .go();
  }

  @Test
  public void testPostgresDateTime() throws Exception {
    mockUsDateFormatSymbols();

    testBuilder()
      .sqlQuery("SELECT sql_to_timestamp(time1, 'yyyy-DD-MMHH24:MI:SS') = "
        + "sql_to_timestamp(time2, 'DDMMyyyyHH12:MI:SSam') as col1, "
        + "sql_to_timestamp(time1, 'yyyy-DD-MMHH24:MI:SS') = "
        + "sql_to_timestamp(time3, 'DDD/yyyySSMIHH12am') as col2 "
        + "from dfs.`joda_postgres_date_time.json`")
      .unOrdered()
      .baselineColumns("col1", "col2")
      .baselineValues(true, true)
      .baselineValues(false, true)
      .go();
  }

  @Test
  public void testJodaDateTime() throws Exception {
    mockUsDateFormatSymbols();

    testBuilder()
      .sqlQuery("SELECT to_timestamp(time1, 'yyyy-dd-MMH:m:ss') = "
        + "to_timestamp(time2, 'ddMMyyyyh:m:ssa') as col1, "
        + "to_timestamp(time1, 'yyyy-dd-MMH:m:ss') = "
        + "to_timestamp(time3, 'DDD/yyyyssmha') as col2 "
        + "from dfs.`joda_postgres_date_time.json`")
      .unOrdered()
      .baselineColumns("col1", "col2")
      .baselineValues(true, true)
      .baselineValues(false, true)
      .go();
  }

  @Test
  public void testJodaDateTimeNested() throws Exception {
    mockUsDateFormatSymbols();

    testBuilder()
      .sqlQuery("SELECT date_add(to_date(time1, concat('yyyy-dd-MM','H:m:ss')), 22)= "
        + "date_add(to_date(time2, concat('ddMMyyyy', 'h:m:ssa')), 22) as col1, "
        + "date_add(to_date(time1, concat('yyyy-dd-MM', 'H:m:ss')), 22) = "
        + "date_add(to_date(time3, concat('DDD/yyyy', 'ssmha')), 22) as col2 "
        + "from dfs.`joda_postgres_date_time.json`")
      .unOrdered()
      .baselineColumns("col1", "col2")
      .baselineValues(true, true)
      .baselineValues(false, true)
      .go();
  }

  @Test
  public void testPostgresDateTimeNested() throws Exception {
    mockUsDateFormatSymbols();

    testBuilder()
      .sqlQuery("SELECT date_add(sql_to_date(time1, concat('yyyy-DD-MM', 'HH24:MI:SS')), 22) = "
        + "date_add(sql_to_date(time2, concat('DDMMyyyy', 'HH12:MI:SSam')), 22) as col1, "
        + "date_add(sql_to_date(time1, concat('yyyy-DD-MM', 'HH24:MI:SS')), 10) = "
        + "date_add(sql_to_date(time3, concat('DDD/yyyySSMI', 'HH12am')), 10) as col2 "
        + "from dfs.`joda_postgres_date_time.json`")
      .unOrdered()
      .baselineColumns("col1", "col2")
      .baselineValues(true, true)
      .baselineValues(false, true)
      .go();
  }

  @Test(expected = UserException.class)
  public void testPostgresPatternFormatError() throws Exception {
    try {
      test("SELECT sql_to_date('1970-01-02', 'yyyy-QQ-MM') from (values(1))");
    } catch (UserException e) {
      assertThat("No expected current \"FUNCTION ERROR\"", e.getMessage(), startsWith("FUNCTION ERROR"));
      throw e;
    }
  }

  @Test(expected = UserException.class)
  public void testPostgresDateFormatError() throws Exception {
    try {
      test("SELECT sql_to_date('1970/01/02', 'yyyy-DD-MM') from (values(1))");
    } catch (UserException e) {
      assertThat("No expected current \"FUNCTION ERROR\"", e.getMessage(), startsWith("FUNCTION ERROR"));
      throw e;
    }
  }

  @Test
  public void testToDateWithEmptyString() throws Exception {
    String query = "SELECT to_date(dateCol, 'yyyy-MM-dd') d from cp.`dateWithEmptyStrings.json`";
    testToDateTimeFunctionWithEmptyStringsAsNull(query, "d", null, null, LocalDate.of(1997, 12, 10));
  }

  @Test
  public void testToTimeWithEmptyString() throws Exception {
    String query = "SELECT to_time(timeCol, 'hh:mm:ss') t from cp.`dateWithEmptyStrings.json`";
    testToDateTimeFunctionWithEmptyStringsAsNull(query, "t", null, null, LocalTime.of(7, 21, 39));
  }

  @Test
  public void testToTimeStampWithEmptyString() throws Exception {
    String query = "SELECT to_timestamp(timestampCol, 'yyyy-MM-dd hh:mm:ss') t from cp.`dateWithEmptyStrings.json`";
    testToDateTimeFunctionWithEmptyStringsAsNull(query, "t", null, null, LocalDateTime.of(2003, 9, 11, 10, 1, 37));
  }

  @Test
  public void testToDateWithLiteralEmptyString() throws Exception {
    Object[] nullObj = new Object[] {null};
    testToDateTimeFunctionWithEmptyStringsAsNull("SELECT to_date('', 'yyyy-MM-dd') d from (values(1))", "d", nullObj);
  }

  @Test
  public void testToTimeWithLiteralEmptyString() throws Exception {
    Object[] nullObj = new Object[] {null};
    testToDateTimeFunctionWithEmptyStringsAsNull("SELECT to_time('', 'hh:mm:ss') d from (values(1))", "d", nullObj);
  }

  @Test
  public void testToTimeStampWithLiteralEmptyString() throws Exception {
    Object[] nullObj = new Object[] {null};
    testToDateTimeFunctionWithEmptyStringsAsNull(
        "SELECT to_timestamp('', 'yyyy-MM-dd hh:mm:ss') d from (values(1))", "d", nullObj);
  }

  @Test
  public void testSqlToDateWithEmptyString() throws Exception {
    String query = "SELECT sql_to_date(dateCol, 'yyyy-MM-dd') d from cp.`dateWithEmptyStrings.json`";
    testToDateTimeFunctionWithEmptyStringsAsNull(query, "d", null, null, LocalDate.of(1997, 12, 10));
  }

  @Test
  public void testSqlToTimeWithEmptyString() throws Exception {
    String query = "SELECT sql_to_time(timeCol, 'HH24:MI:SS') t from cp.`dateWithEmptyStrings.json`";
    testToDateTimeFunctionWithEmptyStringsAsNull(query, "t", null, null, LocalTime.of(7, 21, 39));
  }

  @Test
  public void testSqlToTimeStampWithEmptyString() throws Exception {
    String query = "SELECT sql_to_timestamp(timestampCol, 'yyyy-MM-dd HH24:MI:SS') t from cp.`dateWithEmptyStrings.json`";
    testToDateTimeFunctionWithEmptyStringsAsNull(query, "t", null, null, LocalDateTime.of(2003, 9, 11, 10, 1, 37));
  }

  @Test
  public void testSqlToDateWithLiteralEmptyString() throws Exception {
    Object[] nullObj = new Object[] {null};
    testToDateTimeFunctionWithEmptyStringsAsNull(
        "SELECT sql_to_date('', 'yyyy-MM-dd') d from (values(1))", "d", nullObj);
  }

  @Test
  public void testSqlToTimeWithLiteralEmptyString() throws Exception {
    Object[] nullObj = new Object[] {null};
    testToDateTimeFunctionWithEmptyStringsAsNull(
        "SELECT sql_to_time('', 'HH24:MI:SS') d from (values(1))", "d", nullObj);
  }

  @Test
  public void testSqlToTimeStampWithLiteralEmptyString() throws Exception {
    Object[] nullObj = new Object[] {null};
    testToDateTimeFunctionWithEmptyStringsAsNull(
        "SELECT sql_to_timestamp('', 'yyyy-MM-dd HH24:MI:SS') d from (values(1))", "d", nullObj);
  }

  private void testToDateTimeFunctionWithEmptyStringsAsNull(
      String query, String baselineColumn, Object... baselineValues) throws Exception {
    testBuilder()
        .optionSettingQueriesForTestQuery(ENABLE_CAST_EMPTY_STRING_AS_NULL_QUERY)
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns(baselineColumn)
        .baselineValuesForSingleColumn(baselineValues)
        .go();
  }
}
