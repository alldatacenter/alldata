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

import java.time.LocalDate;

import org.apache.drill.categories.SqlFunctionTest;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.test.BaseTestQuery;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({UnlikelyTest.class, SqlFunctionTest.class})
public class TestNewDateFunctions extends BaseTestQuery {
  DateTime date;
  DateTimeFormatter formatter;
  long unixTimeStamp = -1;

  @Test
  public void testIsDate() throws Exception {
    final String dateValues = "(values('1900-01-01'), ('3500-01-01'), ('2000-12-31'), ('2005-12-32'), ('2015-02-29'), (cast(null as varchar))) as t(date1)";
    testBuilder()
        .sqlQuery("select isDate(date1) res1 " +
            "from " + dateValues)
        .unOrdered()
        .baselineColumns("res1")
        .baselineValues(true)
        .baselineValues(true)
        .baselineValues(true)
        .baselineValues(false)
        .baselineValues(false)
        .baselineValues(false)
        .build()
        .run();

    testBuilder()
        .sqlQuery("select case when isdate(date1) then cast(date1 as date) else null end res1 from " + dateValues)
        .unOrdered()
        .baselineColumns("res1")
        .baselineValues(LocalDate.of(1900, 1, 1))
        .baselineValues(LocalDate.of(3500, 1, 1))
        .baselineValues(LocalDate.of(2000, 12, 31))
        .baselineValues(new Object[] {null})
        .baselineValues(new Object[] {null})
        .baselineValues(new Object[] {null})
        .build()
        .run();
  }

  @Test
  public void testUnixTimeStampForDate() throws Exception {
    formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
    date = formatter.parseDateTime("2009-03-20 11:30:01");
    unixTimeStamp = date.getMillis() / 1000;
    testBuilder()
        .sqlQuery("select unix_timestamp('2009-03-20 11:30:01') from cp.`employee.json` limit 1")
        .ordered()
        .baselineColumns("EXPR$0")
        .baselineValues(unixTimeStamp)
        .build().run();

    date = formatter.parseDateTime("2014-08-09 05:15:06");
    unixTimeStamp = date.getMillis() / 1000;
    testBuilder()
        .sqlQuery("select unix_timestamp('2014-08-09 05:15:06') from cp.`employee.json` limit 1")
        .ordered()
        .baselineColumns("EXPR$0")
        .baselineValues(unixTimeStamp)
        .build().run();

    date = formatter.parseDateTime("1970-01-01 00:00:00");
    unixTimeStamp = date.getMillis() / 1000;
    testBuilder()
        .sqlQuery("select unix_timestamp('1970-01-01 00:00:00') from cp.`employee.json` limit 1")
        .ordered()
        .baselineColumns("EXPR$0")
        .baselineValues(unixTimeStamp)
        .build().run();
  }

  @Test
  public void testUnixTimeStampForDateWithPattern() throws Exception {
    formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");
    date = formatter.parseDateTime("2009-03-20 11:30:01.0");
    unixTimeStamp = date.getMillis() / 1000;

    testBuilder()
        .sqlQuery("select unix_timestamp('2009-03-20 11:30:01.0', 'yyyy-MM-dd HH:mm:ss.SSS') from cp.`employee.json` limit 1")
        .ordered()
        .baselineColumns("EXPR$0")
        .baselineValues(unixTimeStamp)
        .build().run();

    formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
    date = formatter.parseDateTime("2009-03-20");
    unixTimeStamp = date.getMillis() / 1000;

    testBuilder()
        .sqlQuery("select unix_timestamp('2009-03-20', 'yyyy-MM-dd') from cp.`employee.json` limit 1")
        .ordered()
        .baselineColumns("EXPR$0")
        .baselineValues(unixTimeStamp)
        .build().run();
  }

  @Test
  public void testCurrentDate() throws Exception {
    testBuilder()
        .sqlQuery("select (extract(hour from current_date) = 0) as col from cp.`employee.json` limit 1")
        .unOrdered()
        .baselineColumns("col")
        .baselineValues(true)
        .go();
  }

  @Test
  public void testExtractWeek() throws Exception {
    testBuilder()
      .sqlQuery("SELECT EXTRACT(WEEK FROM hire_date) AS col FROM cp.`employee.json` WHERE last_name='Nowmer'")
      .unOrdered()
      .baselineColumns("col")
      .baselineValues(48L)
      .go();
  }

  @Test
  public void testLocalTimestamp() throws Exception {
    testBuilder()
        .sqlQuery("select extract(day from localtimestamp) = extract(day from current_date) as col from cp.`employee.json` limit 1")
        .unOrdered()
        .baselineColumns("col")
        .baselineValues(true)
        .go();
  }
}
