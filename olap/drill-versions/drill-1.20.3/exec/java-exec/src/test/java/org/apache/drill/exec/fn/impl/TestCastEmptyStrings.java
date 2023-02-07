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

import org.apache.drill.exec.ExecConstants;
import org.apache.drill.categories.SqlFunctionTest;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Category({UnlikelyTest.class, SqlFunctionTest.class})
public class TestCastEmptyStrings extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
        // enable decimal data type
        .sessionOption(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY, true)
        // Enable the new cast functions (cast empty string "" to null)
        .systemOption(ExecConstants.CAST_EMPTY_STRING_TO_NULL, true);
    startCluster(builder);
  }

  @Test // see DRILL-1874
  public void testCastOptionalVarCharToNumeric() throws Exception {
    testCastOptionalString("columns[0]", "int", "cp.`emptyStrings.csv`", null, 1, 2);
    testCastOptionalString("columns[0]", "bigint", "cp.`emptyStrings.csv`", null, 1L, 2L);
    testCastOptionalString("columns[0]", "float", "cp.`emptyStrings.csv`", null, 1.0f, 2.0f);
    testCastOptionalString("columns[0]", "double", "cp.`emptyStrings.csv`", null, 1.0, 2.0);
  }

  @Test // see DRILL-1874
  public void testCastRequiredVarCharToNumeric() throws Exception {
    testCastEmptyString("int");
    testCastEmptyString("bigint");
    testCastEmptyString("float");
    testCastEmptyString("double");
  }

  @Test // see DRILL-1874
  public void testCastOptionalVarCharToDecimal() throws Exception {
    BigDecimal one = BigDecimal.valueOf(1L);
    BigDecimal two = BigDecimal.valueOf(2L);
    testCastOptionalString("columns[0]", "decimal", "cp.`emptyStrings.csv`", null, one, two);
    testCastOptionalString("columns[0]", "decimal(9)", "cp.`emptyStrings.csv`", null, one, two);
    testCastOptionalString("columns[0]", "decimal(18)", "cp.`emptyStrings.csv`", null, one, two);
    testCastOptionalString("columns[0]", "decimal(28)", "cp.`emptyStrings.csv`", null, one, two);
    testCastOptionalString("columns[0]", "decimal(38)", "cp.`emptyStrings.csv`", null, one, two);
  }

  @Test // see DRILL-1874
  public void testCastRequiredVarCharToDecimal() throws Exception {
    testCastEmptyString("decimal");
    testCastEmptyString("decimal(18)");
    testCastEmptyString("decimal(28)");
    testCastEmptyString("decimal(38)");
  }

  @Test
  public void testCastRequiredVarCharToDateTime() throws Exception {
    testCastEmptyString("date");
    testCastEmptyString("time");
    testCastEmptyString("timestamp");
  }

  @Test
  public void testCastOptionalVarCharToDateTime() throws Exception {
    testCastOptionalString("dateCol", "date", "cp.`dateWithEmptyStrings.json`",
        null, null, LocalDate.of(1997, 12, 10));
    testCastOptionalString("timeCol", "time", "cp.`dateWithEmptyStrings.json`",
        null, null, LocalTime.of(7, 21, 39));
    testCastOptionalString("timestampCol", "timestamp", "cp.`dateWithEmptyStrings.json`",
        null, null, LocalDateTime.of(2003, 9, 11, 10, 1, 37));
  }

  @Test
  public void testCastRequiredVarCharToInterval() throws Exception {
    testCastEmptyString("interval year");
    testCastEmptyString("interval day");
    testCastEmptyString("interval month");
  }

  private void testCastOptionalString(String column, String asType, String table,
                                      Object... baselineValues) throws Exception {
    String query = String.format("select cast(%s as %s) c from %s", column, asType, table);
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("c")
        .baselineValuesForSingleColumn(baselineValues)
        .go();
  }

  private void testCastEmptyString(String asType) throws Exception {
    Object[] nullObj = new Object[] {null};
    String query = String.format("select cast('' as %s) c from (values(1))", asType);
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("c")
        .baselineValues(nullObj)
        .go();
  }

  @Test
  public void testCastOptionalVarCharToNumber() throws Exception {
    testBuilder()
        .sqlQuery("select to_number(columns[0], '#,##0.0') n from cp.`emptyStrings.csv`")
        .unOrdered()
        .baselineColumns("n")
        .baselineValuesForSingleColumn(null, 1.0, 2.0)
        .go();
  }

  @Test
  public void testCastRequiredVarCharToNumber() throws Exception {
    Object[] nullObj = new Object[] {null};
    testBuilder()
        .sqlQuery("select to_number('', '#,##0.0') n from (values(1))")
        .unOrdered()
        .baselineColumns("n")
        .baselineValues(nullObj)
        .go();
  }
}
