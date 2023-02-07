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
package org.apache.drill.common.expression.fn;

import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.drill.test.BaseTest;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import java.util.Locale;
import java.util.Map;
import static org.apache.drill.common.expression.fn.JodaDateValidator.toJodaFormat;
import static org.joda.time.DateTime.parse;
import static org.joda.time.format.DateTimeFormat.forPattern;

public class JodaDateValidatorTest extends BaseTest {

  private static final Map<String, String> TEST_CASES = Maps.newHashMap();

  @BeforeClass
  public static void fillTestCases() {
    TEST_CASES.put("ddd-mm-yyyy", "D-MM-yyyy");
    TEST_CASES.put("DDD-MM-YYYY", "D-MM-yyyy");
    TEST_CASES.put("ddd/yyyy", "D/yyyy");
    TEST_CASES.put("DDD/YYYY", "D/yyyy");
    TEST_CASES.put("yyyy-Mon-dd", "yyyy-MMM-d");
    TEST_CASES.put("YYYY-mon-DD", "yyyy-MMM-d");
    TEST_CASES.put("yyyy-mon-dd", "yyyy-MMM-d");
    TEST_CASES.put("YYYY-MON-DD", "yyyy-MMM-d");
    TEST_CASES.put("YYYY-MON-DD-D", "yyyy-MMM-d-e");
    TEST_CASES.put("YYYY-MONTH-DD", "yyyy-MMMM-d");
    TEST_CASES.put("dayyyy", "EEEEyyy");
    TEST_CASES.put("dayy", "EEEEy");
    TEST_CASES.put("dyy", "Ey");
    TEST_CASES.put("ddd\"D\"mm\"D\"yyyy", "D'D'MM'D'yyyy");
    TEST_CASES.put("ddd\"ddd-mm-yyyy\"mm-yyyy", "D'ddd-mm-yyyy'MM-yyyy");
    TEST_CASES.put("ddd\"ddd-mm-yyyy\"mm\"ddd-mm-yyyy\"yyyy", "D'ddd-mm-yyyy'MM'ddd-mm-yyyy'yyyy");
    TEST_CASES.put("DD-mm-yyyy", "d-MM-yyyy");
    TEST_CASES.put("DddDDD", "edD");
    TEST_CASES.put("dddddd", "DD");
    TEST_CASES.put("mmmmyyyyddd", "MMMMyyyyD");
    TEST_CASES.put("wweeiyyy", "wGxxxx");
    TEST_CASES.put("iweeiyy", "wGxxx");
    TEST_CASES.put("wweei", "wGx");
    TEST_CASES.put("hhmissmsam", "hmssSSSaa");
    TEST_CASES.put("HHMISSMSAM", "hmssSSSaa");
    TEST_CASES.put("HHmiSSmsPM", "hmssSSSaa");
    TEST_CASES.put("hh12missmsam", "hmssSSSaa");
    TEST_CASES.put("hh24missmsam", "HmssSSSaa");
    TEST_CASES.put("hh24mifmssfxmsam", "HmssSSSaa");
  }

  @Test
  public void testDateCases() {
    for (Map.Entry<String, String> testEntry : TEST_CASES.entrySet()) {
      Assert.assertEquals(testEntry.getValue(), toJodaFormat(testEntry.getKey()));
    }
  }

  @Test
  public void testDateMonthDayYearFormat() {
    int day = 1;
    int month = 8;
    int year = 2011;
    DateTime date = parseDateFromPostgres(month + "/" + day + "/" + year, "MM/DD/YYYY");
    Assert.assertTrue(date.getDayOfMonth() == day &&
                        date.getMonthOfYear() == month &&
                        date.getYear() == year);
  }

  @Test
  public void testDateYearMonthDayFormat() {
    String day = "05";
    String month = "Dec";
    int year = 2000;
    DateTime date = parseDateFromPostgres(day + " " + month + " " + year, "DD Mon YYYY");
    Assert.assertTrue(date.getDayOfMonth() == Integer.parseInt(day) &&
                        date.getMonthOfYear() == 12 &&
                        date.getYear() == year);
  }

  @Test
  public void testDateDayMonthYearFormat() {
    String day = "01";
    String month = "08";
    int year = 2011;
    DateTime date = parseDateFromPostgres(year + "-" + month + "-" + day, "YYYY-MM-DD");
    Assert.assertTrue(date.getDayOfMonth() == Integer.parseInt(day) &&
                        date.getMonthOfYear() == Integer.parseInt(month) &&
                        date.getYear() == year);
  }

  @Test
  public void testDateDayOfYearYearFormat() {
    String day = "01";
    int year = 2011;
    DateTime date = parseDateFromPostgres(day + "/" + year, "ddd/YYYY");
    Assert.assertTrue(date.getDayOfMonth() == 1 &&
                        date.getMonthOfYear() == 1 &&
                        date.getYear() == year);
  }

  @Test
  public void testTimeHoursMinutesSecondsFormat() {
    int hours = 11;
    int minutes = 50;
    String seconds = "05";
    DateTime date = parseDateFromPostgres(hours + ":" + minutes + ":" + seconds + " am", "hh12:mi:ss am");
    Assert.assertTrue(date.getHourOfDay() == hours &&
                        date.getMinuteOfHour() == minutes &&
                          date.getSecondOfMinute() == Integer.parseInt(seconds));
  }

  @Test
  public void testTimeHours24MinutesSecondsFormat() {
    int hours = 15;
    int minutes = 50;
    int seconds = 5;
    DateTime date = parseDateFromPostgres(hours + ":" + minutes + ":" + seconds, "hh24:mi:ss");
    Assert.assertTrue(date.getHourOfDay() == hours &&
                        date.getMinuteOfHour() == minutes &&
                        date.getSecondOfMinute() == seconds);
  }

  @Test
  public void testDateYearMonthNameFormat() {
    String month = "JUN";
    int year = 2000;
    DateTime date = parseDateFromPostgres(year + " " + month, "YYYY MON");
    Assert.assertTrue(date.getMonthOfYear() == 6 && date.getYear() == year);
  }

  @Test
  public void testYearMonthDayFormat() {
    String day = "01";
    String month = "08";
    int year = 2011;
    DateTime date = parseDateFromPostgres(year + "" + month + day, "YYYYMMDD");
    Assert.assertTrue(date.getDayOfMonth() == Integer.parseInt(day) &&
                        date.getMonthOfYear() == Integer.parseInt(month) &&
                        date.getYear() == year);
  }

  @Test
  public void testYearAndMonthDayFormat() {
    String day = "01";
    String month = "08";
    int year = 2011;
    DateTime date = parseDateFromPostgres(year + "-" + month + day, "YYYY-MMDD");
    Assert.assertTrue(date.getDayOfMonth() == Integer.parseInt(day) &&
                        date.getMonthOfYear() == Integer.parseInt(month) &&
                        date.getYear() == year);
  }

  @Test
  public void testYearMonthNameDayFormat() {
    String day = "30";
    String month = "Nov";
    int year = 2000;
    DateTime date = parseDateFromPostgres(year + "" + month + day, "YYYYMonDD");
    Assert.assertTrue(date.getDayOfMonth() == Integer.parseInt(day) &&
                        date.getMonthOfYear() == 11 &&
                        date.getYear() == year);
  }

  @Test
  public void testDateTimeHoursMinutesSecondsFormat() {
    String day = "24";
    String month = "June";
    int year = 2010;
    int hours = 10;
    int minutes = 12;
    DateTime date = parseDateFromPostgres(year + "" + day + month + hours + ":" + minutes + "am", "YYYYDDFMMonthHH12:MIam");
    Assert.assertTrue(date.getDayOfMonth() == Integer.parseInt(day) &&
                        date.getMonthOfYear() == 6 &&
                        date.getYear() == year &&
                        date.getHourOfDay() == hours &&
                        date.getMinuteOfHour() == minutes);
  }

  private DateTime parseDateFromPostgres(String date, String pattern) {
    String jodaFormat = toJodaFormat(pattern);
    DateTimeFormatter format = forPattern(jodaFormat).withLocale(Locale.US);
    return parse(date, format).withZoneRetainFields(DateTimeZone.UTC);
  }
}
