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
import java.time.LocalDateTime;

import org.apache.drill.categories.SqlFunctionTest;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.exec.expr.fn.impl.DateUtility;
import org.apache.drill.test.BaseTestQuery;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({UnlikelyTest.class, SqlFunctionTest.class})
public class TestDateAddFunctions extends BaseTestQuery {

  @Test
  public void testDateAddIntervalDay() throws Exception {
    String query = "select date_add(timestamp '2015-01-24 07:27:05.0', interval '3' day) as col1,\n" +
                          "date_add(timestamp '2015-01-24 07:27:05.0', interval '5' day) as col2,\n" +
                          "date_add(timestamp '2015-01-24 07:27:05.0', interval '5' hour) as col3,\n" +
                          "date_add(timestamp '2015-01-24 07:27:05.0', interval '5' minute) as col4,\n" +
                          "date_add(timestamp '2015-01-24 07:27:05.0', interval '5' second) as col5,\n" +
                          "date_add(timestamp '2015-01-24 07:27:05.0', interval '5 10:20:30' day to second) as col6\n" +
                   "from (values(1))";

    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .baselineColumns("col1", "col2", "col3", "col4", "col5", "col6")
      .baselineValues(LocalDateTime.parse("2015-01-27T07:27:05.0"),
                      LocalDateTime.parse("2015-01-29T07:27:05.0"),
                      LocalDateTime.parse("2015-01-24T12:27:05.0"),
                      LocalDateTime.parse("2015-01-24T07:32:05.0"),
                      LocalDateTime.parse("2015-01-24T07:27:10.0"),
                      LocalDateTime.parse("2015-01-29T17:47:35.0"))
      .go();
  }

  @Test
  public void testDateAddIntervalYear() throws Exception {
    String query = "select date_add(date '2015-01-24', interval '3' month) as col1,\n" +
                          "date_add(date '2015-01-24', interval '5' month) as col2,\n" +
                          "date_add(date '2015-01-24', interval '5' year) as col3\n" +
                   "from (values(1))";

    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .baselineColumns("col1", "col2", "col3")
      .baselineValues(DateUtility.parseBest("2015-04-24"),
                      DateUtility.parseBest("2015-06-24"),
                      DateUtility.parseBest("2020-01-24"))
      .go();
  }

  @Test
  public void testDateAddIntegerAsDay() throws Exception {
    String query = "select date_add(date '2015-01-24', 3) as col1,\n" +
                          "date_add(date '2015-01-24', 5) as col2\n" +
                   "from (values(1))";

    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .baselineColumns("col1", "col2")
      .baselineValues(LocalDate.parse("2015-01-27"),
                      LocalDate.parse("2015-01-29"))
      .go();
  }
}
