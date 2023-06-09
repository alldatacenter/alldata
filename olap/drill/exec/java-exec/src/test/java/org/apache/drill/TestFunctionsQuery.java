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
package org.apache.drill;

import static org.apache.drill.exec.expr.fn.impl.DateUtility.formatTimeStamp;
import static org.hamcrest.CoreMatchers.containsString;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import org.apache.drill.categories.SqlFunctionTest;
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.BatchSchemaBuilder;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.test.BaseTestQuery;
import org.hamcrest.CoreMatchers;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

@Category(SqlFunctionTest.class)
public class TestFunctionsQuery extends BaseTestQuery {

  // enable decimal data type
  @BeforeClass
  public static void enableDecimalDataType() throws Exception {
    test(String.format("alter session set `%s` = true", PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY));
  }

  @AfterClass
  public static void disableDecimalDataType() {
    resetSessionOption(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY);
  }

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testAbsDecimalFunction() throws Exception{
    String query = "SELECT " +
        "abs(cast('1234.4567' as decimal(9, 5))) as DEC9_ABS_1, " +
        "abs(cast('-1234.4567' as decimal(9, 5))) DEC9_ABS_2, " +
        "abs(cast('99999912399.4567' as decimal(18, 5))) DEC18_ABS_1, " +
        "abs(cast('-99999912399.4567' as decimal(18, 5))) DEC18_ABS_2, " +
        "abs(cast('12345678912345678912.4567' as decimal(28, 5))) DEC28_ABS_1, " +
        "abs(cast('-12345678912345678912.4567' as decimal(28, 5))) DEC28_ABS_2, " +
        "abs(cast('1234567891234567891234567891234567891.4' as decimal(38, 1))) DEC38_ABS_1, " +
        "abs(cast('-1234567891234567891234567891234567891.4' as decimal(38, 1))) DEC38_ABS_2";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("DEC9_ABS_1", "DEC9_ABS_2", "DEC18_ABS_1", "DEC18_ABS_2", "DEC28_ABS_1", "DEC28_ABS_2",
          "DEC38_ABS_1", "DEC38_ABS_2")
        .baselineValues(new BigDecimal("1234.45670"), new BigDecimal("1234.45670"),
          new BigDecimal("99999912399.45670"), new BigDecimal("99999912399.45670"),
          new BigDecimal("12345678912345678912.45670"), new BigDecimal("12345678912345678912.45670"),
          new BigDecimal("1234567891234567891234567891234567891.4"),
          new BigDecimal("1234567891234567891234567891234567891.4"))
        .go();
  }


  @Test
  public void testCeilDecimalFunction() throws Exception {
    String query = "SELECT " +
        "ceil(cast('1234.4567' as decimal(9, 5))) as DEC9_1, " +
        "ceil(cast('1234.0000' as decimal(9, 5))) as DEC9_2, " +
        "ceil(cast('-1234.4567' as decimal(9, 5))) as DEC9_3, " +
        "ceil(cast('-1234.000' as decimal(9, 5))) as DEC9_4, " +
        "ceil(cast('99999912399.4567' as decimal(18, 5))) DEC18_1, " +
        "ceil(cast('99999912399.0000' as decimal(18, 5))) DEC18_2, " +
        "ceil(cast('-99999912399.4567' as decimal(18, 5))) DEC18_3, " +
        "ceil(cast('-99999912399.0000' as decimal(18, 5))) DEC18_4, " +
        "ceil(cast('12345678912345678912.4567' as decimal(28, 5))) DEC28_1, " +
        "ceil(cast('999999999999999999.4567' as decimal(28, 5))) DEC28_2, " +
        "ceil(cast('12345678912345678912.0000' as decimal(28, 5))) DEC28_3, " +
        "ceil(cast('-12345678912345678912.4567' as decimal(28, 5))) DEC28_4, " +
        "ceil(cast('-12345678912345678912.0000' as decimal(28, 5))) DEC28_5, " +
        "ceil(cast('1234567891234567891234567891234567891.4' as decimal(38, 1))) DEC38_1, " +
        "ceil(cast('999999999999999999999999999999999999.4' as decimal(38, 1))) DEC38_2, " +
        "ceil(cast('1234567891234567891234567891234567891.0' as decimal(38, 1))) DEC38_3, " +
        "ceil(cast('-1234567891234567891234567891234567891.4' as decimal(38, 1))) DEC38_4, " +
        "ceil(cast('-1234567891234567891234567891234567891.0' as decimal(38, 1))) DEC38_5";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("DEC9_1", "DEC9_2", "DEC9_3", "DEC9_4", "DEC18_1", "DEC18_2", "DEC18_3", "DEC18_4", "DEC28_1",
            "DEC28_2", "DEC28_3", "DEC28_4", "DEC28_5", "DEC38_1", "DEC38_2", "DEC38_3", "DEC38_4", "DEC38_5")
        .baselineValues(new BigDecimal("1235"), new BigDecimal("1234"), new BigDecimal("-1234"),
          new BigDecimal("-1234"), new BigDecimal("99999912400"), new BigDecimal("99999912399"),
          new BigDecimal("-99999912399"), new BigDecimal("-99999912399"),
          new BigDecimal("12345678912345678913"), new BigDecimal("1000000000000000000"),
          new BigDecimal("12345678912345678912"), new BigDecimal("-12345678912345678912"),
          new BigDecimal("-12345678912345678912"), new BigDecimal("1234567891234567891234567891234567892"),
          new BigDecimal("1000000000000000000000000000000000000"),
          new BigDecimal("1234567891234567891234567891234567891"),
          new BigDecimal("-1234567891234567891234567891234567891"),
          new BigDecimal("-1234567891234567891234567891234567891"))
        .go();
  }

  @Test
  public void testFloorDecimalFunction() throws Exception {
    String query = "SELECT " +
        "floor(cast('1234.4567' as decimal(9, 5))) as DEC9_1, " +
        "floor(cast('1234.0000' as decimal(9, 5))) as DEC9_2, " +
        "floor(cast('-1234.4567' as decimal(9, 5))) as DEC9_3, " +
        "floor(cast('-1234.000' as decimal(9, 5))) as DEC9_4, " +
        "floor(cast('99999912399.4567' as decimal(18, 5))) DEC18_1, " +
        "floor(cast('99999912399.0000' as decimal(18, 5))) DEC18_2, " +
        "floor(cast('-99999912399.4567' as decimal(18, 5))) DEC18_3, " +
        "floor(cast('-99999912399.0000' as decimal(18, 5))) DEC18_4, " +
        "floor(cast('12345678912345678912.4567' as decimal(28, 5))) DEC28_1, " +
        "floor(cast('999999999999999999.4567' as decimal(28, 5))) DEC28_2, " +
        "floor(cast('12345678912345678912.0000' as decimal(28, 5))) DEC28_3, " +
        "floor(cast('-12345678912345678912.4567' as decimal(28, 5))) DEC28_4, " +
        "floor(cast('-12345678912345678912.0000' as decimal(28, 5))) DEC28_5, " +
        "floor(cast('1234567891234567891234567891234567891.4' as decimal(38, 1))) DEC38_1, " +
        "floor(cast('999999999999999999999999999999999999.4' as decimal(38, 1))) DEC38_2, " +
        "floor(cast('1234567891234567891234567891234567891.0' as decimal(38, 1))) DEC38_3, " +
        "floor(cast('-1234567891234567891234567891234567891.4' as decimal(38, 1))) DEC38_4, " +
        "floor(cast('-999999999999999999999999999999999999.4' as decimal(38, 1))) DEC38_5";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("DEC9_1", "DEC9_2", "DEC9_3", "DEC9_4", "DEC18_1", "DEC18_2", "DEC18_3", "DEC18_4", "DEC28_1",
            "DEC28_2", "DEC28_3", "DEC28_4", "DEC28_5", "DEC38_1", "DEC38_2", "DEC38_3", "DEC38_4", "DEC38_5")
        .baselineValues(new BigDecimal("1234"), new BigDecimal("1234"), new BigDecimal("-1235"),
          new BigDecimal("-1234"), new BigDecimal("99999912399"), new BigDecimal("99999912399"),
          new BigDecimal("-99999912400"), new BigDecimal("-99999912399"),
          new BigDecimal("12345678912345678912"), new BigDecimal("999999999999999999"),
          new BigDecimal("12345678912345678912"), new BigDecimal("-12345678912345678913"),
          new BigDecimal("-12345678912345678912"), new BigDecimal("1234567891234567891234567891234567891"),
          new BigDecimal("999999999999999999999999999999999999"),
          new BigDecimal("1234567891234567891234567891234567891"),
          new BigDecimal("-1234567891234567891234567891234567892"),
          new BigDecimal("-1000000000000000000000000000000000000"))
        .go();
  }

  @Test
  public void testTruncateDecimalFunction() throws Exception {
    String query = "SELECT " +
        "trunc(cast('1234.4567' as decimal(9, 5))) as DEC9_1, " +
        "trunc(cast('1234.0000' as decimal(9, 5))) as DEC9_2, " +
        "trunc(cast('-1234.4567' as decimal(9, 5))) as DEC9_3, " +
        "trunc(cast('0.111' as decimal(9, 5))) as DEC9_4, " +
        "trunc(cast('99999912399.4567' as decimal(18, 5))) DEC18_1, " +
        "trunc(cast('99999912399.0000' as decimal(18, 5))) DEC18_2, " +
        "trunc(cast('-99999912399.4567' as decimal(18, 5))) DEC18_3, " +
        "trunc(cast('-99999912399.0000' as decimal(18, 5))) DEC18_4, " +
        "trunc(cast('12345678912345678912.4567' as decimal(28, 5))) DEC28_1, " +
        "trunc(cast('999999999999999999.4567' as decimal(28, 5))) DEC28_2, " +
        "trunc(cast('12345678912345678912.0000' as decimal(28, 5))) DEC28_3, " +
        "trunc(cast('-12345678912345678912.4567' as decimal(28, 5))) DEC28_4, " +
        "trunc(cast('-12345678912345678912.0000' as decimal(28, 5))) DEC28_5, " +
        "trunc(cast('1234567891234567891234567891234567891.4' as decimal(38, 1))) DEC38_1, " +
        "trunc(cast('999999999999999999999999999999999999.4' as decimal(38, 1))) DEC38_2, " +
        "trunc(cast('1234567891234567891234567891234567891.0' as decimal(38, 1))) DEC38_3, " +
        "trunc(cast('-1234567891234567891234567891234567891.4' as decimal(38, 1))) DEC38_4, " +
        "trunc(cast('-999999999999999999999999999999999999.4' as decimal(38, 1))) DEC38_5";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("DEC9_1", "DEC9_2", "DEC9_3", "DEC9_4", "DEC18_1", "DEC18_2", "DEC18_3", "DEC18_4", "DEC28_1",
            "DEC28_2", "DEC28_3", "DEC28_4", "DEC28_5", "DEC38_1", "DEC38_2", "DEC38_3", "DEC38_4", "DEC38_5")
        .baselineValues(new BigDecimal("1234"), new BigDecimal("1234"), new BigDecimal("-1234"),
          new BigDecimal("0"), new BigDecimal("99999912399"), new BigDecimal("99999912399"),
          new BigDecimal("-99999912399"), new BigDecimal("-99999912399"),
          new BigDecimal("12345678912345678912"), new BigDecimal("999999999999999999"),
          new BigDecimal("12345678912345678912"), new BigDecimal("-12345678912345678912"),
          new BigDecimal("-12345678912345678912"), new BigDecimal("1234567891234567891234567891234567891"),
          new BigDecimal("999999999999999999999999999999999999"),
          new BigDecimal("1234567891234567891234567891234567891"),
          new BigDecimal("-1234567891234567891234567891234567891"),
          new BigDecimal("-999999999999999999999999999999999999"))
        .go();
  }

  @Test
  public void testTruncateWithParamDecimalFunction() throws Exception {
    String query = "SELECT " +
        "trunc(cast('1234.4567' as decimal(9, 5)), 2) as DEC9_1, " +
        "trunc(cast('1234.45' as decimal(9, 2)), 4) as DEC9_2, " +
        "trunc(cast('-1234.4567' as decimal(9, 5)), 0) as DEC9_3, " +
        "trunc(cast('0.111' as decimal(9, 5)), 2) as DEC9_4, " +
        "trunc(cast('99999912399.4567' as decimal(18, 5)), 2) DEC18_1, " +
        "trunc(cast('99999912399.0000' as decimal(18, 5)), 2) DEC18_2, " +
        "trunc(cast('-99999912399.45' as decimal(18, 2)), 6) DEC18_3, " +
        "trunc(cast('-99999912399.0000' as decimal(18, 5)), 4) DEC18_4, " +
        "trunc(cast('12345678912345678912.4567' as decimal(28, 5)), 1) DEC28_1, " +
        "trunc(cast('999999999999999999.456' as decimal(28, 3)), 6) DEC28_2, " +
        "trunc(cast('12345678912345678912.0000' as decimal(28, 5)), 2) DEC28_3, " +
        "trunc(cast('-12345678912345678912.45' as decimal(28, 2)), 0) DEC28_4, " +
        "trunc(cast('-12345678912345678912.0000' as decimal(28, 5)), 1) DEC28_5, " +
        "trunc(cast('999999999.123456789' as decimal(38, 9)), 7) DEC38_1, " +
        "trunc(cast('999999999.4' as decimal(38, 1)), 8) DEC38_2, " +
        "trunc(cast('999999999.1234' as decimal(38, 4)), 12) DEC38_3, " +
        "trunc(cast('-123456789123456789.4' as decimal(38, 1)), 10) DEC38_4, " +
        "trunc(cast('-999999999999999999999999999999999999.4' as decimal(38, 1)), 1) DEC38_5";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("DEC9_1", "DEC9_2", "DEC9_3", "DEC9_4", "DEC18_1", "DEC18_2", "DEC18_3", "DEC18_4", "DEC28_1",
            "DEC28_2", "DEC28_3", "DEC28_4", "DEC28_5", "DEC38_1", "DEC38_2", "DEC38_3", "DEC38_4", "DEC38_5")
        .baselineValues(new BigDecimal("1234.45"), new BigDecimal("1234.4500"), new BigDecimal("-1234"),
          new BigDecimal("0.11"), new BigDecimal("99999912399.45"), new BigDecimal("99999912399.00"),
          new BigDecimal("-99999912399.450000"), new BigDecimal("-99999912399.0000"),
          new BigDecimal("12345678912345678912.4"), new BigDecimal("999999999999999999.456000"),
          new BigDecimal("12345678912345678912.00"), new BigDecimal("-12345678912345678912"),
          new BigDecimal("-12345678912345678912.0"), new BigDecimal("999999999.1234567"),
          new BigDecimal("999999999.40000000"), new BigDecimal("999999999.123400000000"),
          new BigDecimal("-123456789123456789.4000000000"),
          new BigDecimal("-999999999999999999999999999999999999.4"))
        .go();
  }

  @Test
  public void testRoundDecimalFunction() throws Exception {
    String query = "SELECT " +
        "round(cast('1234.5567' as decimal(9, 5))) as DEC9_1, " +
        "round(cast('1234.1000' as decimal(9, 5))) as DEC9_2, " +
        "round(cast('-1234.5567' as decimal(9, 5))) as DEC9_3, " +
        "round(cast('-1234.1234' as decimal(9, 5))) as DEC9_4, " +
        "round(cast('99999912399.9567' as decimal(18, 5))) DEC18_1, " +
        "round(cast('99999912399.0000' as decimal(18, 5))) DEC18_2, " +
        "round(cast('-99999912399.5567' as decimal(18, 5))) DEC18_3, " +
        "round(cast('-99999912399.0000' as decimal(18, 5))) DEC18_4, " +
        "round(cast('12345678912345678912.5567' as decimal(28, 5))) DEC28_1, " +
        "round(cast('999999999999999999.5567' as decimal(28, 5))) DEC28_2, " +
        "round(cast('12345678912345678912.0000' as decimal(28, 5))) DEC28_3, " +
        "round(cast('-12345678912345678912.5567' as decimal(28, 5))) DEC28_4, " +
        "round(cast('-12345678912345678912.0000' as decimal(28, 5))) DEC28_5, " +
        "round(cast('999999999999999999999999999.5' as decimal(38, 1))) DEC38_1, " +
        "round(cast('99999999.512345678123456789' as decimal(38, 18))) DEC38_2, " +
        "round(cast('999999999999999999999999999999999999.5' as decimal(38, 1))) DEC38_3, " +
        "round(cast('1234567891234567891234567891234567891.2' as decimal(38, 1))) DEC38_4, " +
        "round(cast('-1234567891234567891234567891234567891.4' as decimal(38, 1))) DEC38_5, " +
        "round(cast('-999999999999999999999999999999999999.9' as decimal(38, 1))) DEC38_6";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("DEC9_1", "DEC9_2", "DEC9_3", "DEC9_4", "DEC18_1", "DEC18_2", "DEC18_3", "DEC18_4", "DEC28_1",
            "DEC28_2", "DEC28_3", "DEC28_4", "DEC28_5", "DEC38_1", "DEC38_2", "DEC38_3", "DEC38_4", "DEC38_5", "DEC38_6")
        .baselineValues(new BigDecimal("1235"), new BigDecimal("1234"), new BigDecimal("-1235"),
          new BigDecimal("-1234"), new BigDecimal("99999912400"), new BigDecimal("99999912399"),
          new BigDecimal("-99999912400"), new BigDecimal("-99999912399"),
          new BigDecimal("12345678912345678913"), new BigDecimal("1000000000000000000"),
          new BigDecimal("12345678912345678912"), new BigDecimal("-12345678912345678913"),
          new BigDecimal("-12345678912345678912"), new BigDecimal("1000000000000000000000000000"),
          new BigDecimal("100000000"), new BigDecimal("1000000000000000000000000000000000000"),
          new BigDecimal("1234567891234567891234567891234567891"),
          new BigDecimal("-1234567891234567891234567891234567891"),
          new BigDecimal("-1000000000000000000000000000000000000"))
        .go();
  }

  @Ignore("DRILL-3909")
  @Test
  public void testRoundWithScaleDecimalFunction() throws Exception {
    String query = "SELECT " +
        "round(cast('1234.5567' as decimal(9, 5)), 3) as DEC9_1, " +
        "round(cast('1234.1000' as decimal(9, 5)), 2) as DEC9_2, " +
        "round(cast('-1234.5567' as decimal(9, 5)), 4) as DEC9_3, " +
        "round(cast('-1234.1234' as decimal(9, 5)), 3) as DEC9_4, " +
        "round(cast('-1234.1234' as decimal(9, 2)), 4) as DEC9_5, " +
        "round(cast('99999912399.9567' as decimal(18, 5)), 3) DEC18_1, " +
        "round(cast('99999912399.0000' as decimal(18, 5)), 2) DEC18_2, " +
        "round(cast('-99999912399.5567' as decimal(18, 5)), 2) DEC18_3, " +
        "round(cast('-99999912399.0000' as decimal(18, 5)), 0) DEC18_4, " +
        "round(cast('12345678912345678912.5567' as decimal(28, 5)), 2) DEC28_1, " +
        "round(cast('999999999999999999.5567' as decimal(28, 5)), 1) DEC28_2, " +
        "round(cast('12345678912345678912.0000' as decimal(28, 5)), 8) DEC28_3, " +
        "round(cast('-12345678912345678912.5567' as decimal(28, 5)), 3) DEC28_4, " +
        "round(cast('-12345678912345678912.0000' as decimal(28, 5)), 0) DEC28_5, " +
        "round(cast('999999999999999999999999999.5' as decimal(38, 1)), 1) DEC38_1, " +
        "round(cast('99999999.512345678923456789' as decimal(38, 18)), 9) DEC38_2, " +
        "round(cast('999999999.9999999995678' as decimal(38, 18)), 9) DEC38_3, " +
        "round(cast('999999999.9999999995678' as decimal(38, 18)), 11) DEC38_4, " +
        "round(cast('999999999.9999999995678' as decimal(38, 18)), 21) DEC38_5, " +
        "round(cast('-1234567891234567891234567891234567891.4' as decimal(38, 1)), 1) DEC38_6, " +
        "round(cast('-999999999999999999999999999999999999.9' as decimal(38, 1)), 0) DEC38_7";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("DEC9_1", "DEC9_2", "DEC9_3", "DEC9_4", "DEC9_5", "DEC18_1", "DEC18_2", "DEC18_3", "DEC18_4",
          "DEC28_1", "DEC28_2", "DEC28_3", "DEC28_4", "DEC28_5", "DEC38_1", "DEC38_2", "DEC38_3", "DEC38_4", "DEC38_5",
          "DEC38_6", "DEC38_7")
        .baselineValues(new BigDecimal("1234.557"), new BigDecimal("1234.10"), new BigDecimal("-1234.5567"),
          new BigDecimal("-1234.123"), new BigDecimal("-1234.1200"), new BigDecimal("99999912399.957"),
          new BigDecimal("99999912399.00"), new BigDecimal("-99999912399.56"),
          new BigDecimal("-99999912399"), new BigDecimal("12345678912345678912.56"),
          new BigDecimal("999999999999999999.6"), new BigDecimal("12345678912345678912.00000000"),
          new BigDecimal("-12345678912345678912.557"), new BigDecimal("-12345678912345678912"),
          new BigDecimal("999999999999999999999999999.5"), new BigDecimal("99999999.512345679"),
          new BigDecimal("1000000000.000000000"), new BigDecimal("999999999.99999999957"),
          new BigDecimal("999999999.999999999567800000000"),
          new BigDecimal("-1234567891234567891234567891234567891.4"),
          new BigDecimal("-1000000000000000000000000000000000000"))
        .go();
  }

  @Ignore("we don't have decimal division")
  @Test
  public void testCastDecimalDivide() throws Exception {
    String query = "select  (cast('9' as decimal(9, 1)) / cast('2' as decimal(4, 1))) as DEC9_DIV, " +
        "cast('999999999' as decimal(9,0)) / cast('0.000000000000000000000000001' as decimal(28,28)) as DEC38_DIV, " +
        "cast('123456789.123456789' as decimal(18, 9)) * cast('123456789.123456789' as decimal(18, 9)) as DEC18_MUL";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("DEC9_DIV", "DEC38_DIV", "DEC18_MUL")
        .baselineValues(new BigDecimal("4.500000000"), new BigDecimal("999999999000000000000000000000000000.0"),
            new BigDecimal("15241578780673678.515622620750190521"))
        .go();
  }


  // From DRILL-2668:  "CAST ( 1.1 AS FLOAT )" yielded TYPE DOUBLE:

  /**
   * Test for DRILL-2668, that "CAST ( 1.5 AS FLOAT )" really yields type FLOAT
   * (rather than type DOUBLE).
   */
  @Test
  public void testLiteralCastToFLOATYieldsFLOAT() throws Exception {
    testBuilder()
    .sqlQuery( "SELECT CAST( 1.5 AS FLOAT ) AS ShouldBeFLOAT")
    .unOrdered()
    .baselineColumns("ShouldBeFLOAT")
    .baselineValues(1.5f)
    .go();
  }

  @Test
  public void testLiteralCastToDOUBLEYieldsDOUBLE() throws Exception {
    testBuilder()
    .sqlQuery( "SELECT CAST( 1.25 AS DOUBLE PRECISION ) AS ShouldBeDOUBLE")
    .unOrdered()
    .baselineColumns("ShouldBeDOUBLE")
    .baselineValues(1.25)
    .go();
  }

  @Test
  public void testLiteralCastToBIGINTYieldsBIGINT() throws Exception {
    testBuilder()
    .sqlQuery( "SELECT CAST( 64 AS BIGINT ) AS ShouldBeBIGINT")
    .unOrdered()
    .baselineColumns("ShouldBeBIGINT")
    .baselineValues(64L)
    .go();
  }

  @Test
  public void testLiteralCastToINTEGERYieldsINTEGER() throws Exception {
    testBuilder()
    .sqlQuery( "SELECT CAST( 32 AS INTEGER ) AS ShouldBeINTEGER")
    .unOrdered()
    .baselineColumns("ShouldBeINTEGER")
    .baselineValues(32)
    .go();
  }

  @Ignore( "until SMALLINT is supported (DRILL-2470)" )
  @Test
  public void testLiteralCastToSMALLINTYieldsSMALLINT() throws Exception {
    testBuilder()
    .sqlQuery( "SELECT CAST( 16 AS SMALLINT ) AS ShouldBeSMALLINT")
    .unOrdered()
    .baselineColumns("ShouldBeSMALLINT")
    .baselineValues((short) 16)
    .go();
  }

  @Ignore( "until TINYINT is supported (~DRILL-2470)" )
  @Test
  public void testLiteralCastToTINYINTYieldsTINYINT() throws Exception {
    testBuilder()
    .sqlQuery( "SELECT CAST( 8 AS TINYINT ) AS ShouldBeTINYINT")
    .unOrdered()
    .baselineColumns("ShouldBeTINYINT")
    .baselineValues((byte) 8)
    .go();
  }

  @Test
  public void testDecimalMultiplicationOverflowNegativeScale() throws Exception {
    String query = "select cast('1000000000000000001.000000000000000000' as decimal(38, 18)) * " +
      "cast('99999999999999999999.999999999999999999' as decimal(38, 18)) as DEC38_1";
    expectedException.expect(UserRemoteException.class);
    expectedException.expectMessage(CoreMatchers.containsString("VALIDATION ERROR: Value 100000000000000000100000000000000000000 overflows specified precision 38 with scale 0."));
    test(query);
  }

  @Test
  public void testDecimalMultiplicationOverflowHandling() throws Exception {
    String query = "select cast('1' as decimal(9, 5)) * cast ('999999999999999999999999999.999999999' as decimal(38, 9)) as DEC38_1, " +
        "cast('1000000000000000001.000000000000000000' as decimal(38, 18)) * cast('0.999999999999999999' as decimal(38, 18)) as DEC38_2, " +
        "cast('3' as decimal(9, 8)) * cast ('333333333.3333333333333333333' as decimal(38, 19)) as DEC38_3 " +
        "from cp.`employee.json` where employee_id = 1";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("DEC38_1", "DEC38_2", "DEC38_3")
        .baselineValues(new BigDecimal("1000000000000000000000000000.00000"), new BigDecimal("1000000000000000000"), new BigDecimal("1000000000.000000000000000000"))
        .go();
  }

  @Test
  public void testDecimalRoundUp() throws Exception {
    String query = "select cast('999999999999999999.9999999999999999995' as decimal(38, 18)) as DEC38_1, " +
        "cast('999999999999999999.9999999999999999994' as decimal(38, 18)) as DEC38_2, " +
        "cast('999999999999999999.1234567895' as decimal(38, 9)) as DEC38_3, " +
        "cast('99999.12345' as decimal(18, 4)) as DEC18_1, " +
        "cast('99999.99995' as decimal(18, 4)) as DEC18_2";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("DEC38_1", "DEC38_2", "DEC38_3", "DEC18_1", "DEC18_2")
        .baselineValues(new BigDecimal("1000000000000000000.000000000000000000"),
          new BigDecimal("999999999999999999.999999999999999999"),
          new BigDecimal("999999999999999999.123456790"), new BigDecimal("99999.1235"),
          new BigDecimal("100000.0000"))
        .go();
  }

  @Test
  public void testDecimalDownwardCast() throws Exception {
    String query = "select cast((cast('12345.6789' as decimal(18, 4))) as decimal(9, 4)) as DEC18_DEC9_1, " +
        "cast((cast('12345.6789' as decimal(18, 4))) as decimal(9, 2)) as DEC18_DEC9_2, " +
        "cast((cast('-12345.6789' as decimal(18, 4))) as decimal(9, 0)) as DEC18_DEC9_3, " +
        "cast((cast('99999999.6789' as decimal(38, 4))) as decimal(9, 0)) as DEC38_DEC19_1, " +
        "cast((cast('-999999999999999.6789' as decimal(38, 4))) as decimal(18, 2)) as DEC38_DEC18_1, " +
        "cast((cast('-999999999999999.6789' as decimal(38, 4))) as decimal(18, 0)) as DEC38_DEC18_2, " +
        "cast((cast('100000000999999999.6789' as decimal(38, 4))) as decimal(28, 0)) as DEC38_DEC28_1";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("DEC18_DEC9_1", "DEC18_DEC9_2", "DEC18_DEC9_3", "DEC38_DEC19_1", "DEC38_DEC18_1",
          "DEC38_DEC18_2", "DEC38_DEC28_1")
        .baselineValues(new BigDecimal("12345.6789"), new BigDecimal("12345.68"), new BigDecimal("-12346"),
          new BigDecimal("100000000"), new BigDecimal("-999999999999999.68"),
          new BigDecimal("-1000000000000000"), new BigDecimal("100000001000000000"))
        .go();
  }

  @Test
  public void testTruncateWithParamFunction() throws Exception {
    String query =
        "SELECT\n" +
            "trunc(cast('1234.4567' as double), 2) as T_1,\n" +
            "trunc(cast('-1234.4567' as double), 2) as T_2,\n" +
            "trunc(cast('1234.4567' as double), -2) as T_3,\n" +
            "trunc(cast('-1234.4567' as double), -2) as T_4,\n" +
            "trunc(cast('1234' as double), 4) as T_5,\n" +
            "trunc(cast('-1234' as double), 4) as T_6,\n" +
            "trunc(cast('1234' as double), -4) as T_7,\n" +
            "trunc(cast('-1234' as double), -4) as T_8,\n" +
            "trunc(cast('8124674407369523212' as double), 0) as T_9,\n" +
            "trunc(cast('81246744073695.395' as double), 1) as T_10\n";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("T_1", "T_2", "T_3", "T_4", "T_5", "T_6", "T_7", "T_8", "T_9", "T_10")
        .baselineValues(Double.valueOf("1234.45"), Double.valueOf("-1234.45"), Double.valueOf("1200.0"),
          Double.valueOf("-1200.0"), Double.valueOf("1234.0"), Double.valueOf("-1234.0"), Double.valueOf("0.0"),
          Double.valueOf("0.0"), Double.valueOf("8.1246744073695232E18"), Double.valueOf("8.12467440736953E13"))
        .go();
  }

  @Test
  public void testRoundWithParamFunction() throws Exception {
    String query =
        "SELECT\n" +
            "round(cast('1234.4567' as double), 2) as T_1,\n" +
            "round(cast('-1234.4567' as double), 2) as T_2,\n" +
            "round(cast('1234.4567' as double), -2) as T_3,\n" +
            "round(cast('-1234.4567' as double), -2) as T_4,\n" +
            "round(cast('1234' as double), 4) as T_5,\n" +
            "round(cast('-1234' as double), 4) as T_6,\n" +
            "round(cast('1234' as double), -4) as T_7,\n" +
            "round(cast('-1234' as double), -4) as T_8,\n" +
            "round(cast('8124674407369523212' as double), -4) as T_9,\n" +
            "round(cast('81246744073695.395' as double), 1) as T_10\n";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("T_1", "T_2", "T_3", "T_4", "T_5", "T_6", "T_7", "T_8", "T_9", "T_10")
        .baselineValues(Double.valueOf("1234.46"), Double.valueOf("-1234.46"), Double.valueOf("1200.0"),
          Double.valueOf("-1200.0"), Double.valueOf("1234.0"), Double.valueOf("-1234.0"), Double.valueOf("0.0"),
          Double.valueOf("0.0"), Double.valueOf("8.1246744073695201E18"), Double.valueOf("8.12467440736954E13"))
        .go();

  }

  @Test
  public void testRoundWithOneParam() throws Exception {
    String query =
        "select\n" +
            "round(8124674407369523212) round_bigint,\n" +
            "round(9999999) round_int,\n" +
            "round(cast('23.45' as float)) round_float_1,\n" +
            "round(cast('23.55' as float)) round_float_2,\n" +
            "round(cast('8124674407369.2345' as double)) round_double_1,\n" +
            "round(cast('8124674407369.589' as double)) round_double_2\n";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("round_bigint", "round_int", "round_float_1", "round_float_2", "round_double_1", "round_double_2")
        .baselineValues(8124674407369523212L, 9999999, 23.0f, 24.0f, 8124674407369.0d, 8124674407370.0d)
        .go();
  }

  @Test
  public void testToCharFunction() throws Exception {
    String query = "SELECT " +
        "to_char(1234.5567, '#,###.##') as FLOAT8_1, " +
        "to_char(1234.5, '$#,###.00') as FLOAT8_2, " +
        "to_char(cast('1234.5567' as decimal(9, 5)), '#,###.##') as DEC9_1, " +
        "to_char(cast('99999912399.9567' as decimal(18, 5)), '#.#####') DEC18_1, " +
        "to_char(cast('12345678912345678912.5567' as decimal(28, 5)), '#,###.#####') DEC28_1, " +
        "to_char(cast('999999999999999999999999999.5' as decimal(38, 1)), '#.#') DEC38_1";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("FLOAT8_1", "FLOAT8_2", "DEC9_1", "DEC18_1", "DEC28_1", "DEC38_1")
        .baselineValues("1,234.56", "$1,234.50", "1,234.56", "99999912399.9567", "12,345,678,912,345,678,912.5567",
          "999999999999999999999999999.5")
        .go();
  }

  @Test
  public void testConcatFunction() throws Exception {
    String query = "SELECT " +
        "concat('1234', ' COL_VALUE ', R_REGIONKEY, ' - STRING') as STR_1 " +
        "FROM cp.`tpch/region.parquet` limit 1";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("STR_1")
        .baselineValues("1234 COL_VALUE 0 - STRING")
        .go();
  }

  @Test
  public void testTimeStampConstant() throws Exception {
    String query = "SELECT " +
        "timestamp '2008-2-23 12:23:23' as TS";

    LocalDateTime date = LocalDateTime.parse("2008-02-23 12:23:23.0", formatTimeStamp);
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("TS")
        .baselineValues(date)
        .go();
  }

  @Test
  public void testNullConstantsTimeTimeStampAndDate() throws Exception {
    String query = "SELECT " +
        "CAST(NULL AS TIME) AS t, " +
        "CAST(NULL AS TIMESTAMP) AS ts, " +
        "CAST(NULL AS DATE) AS d";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("t", "ts", "d")
        .baselineValues(null, null, null)
        .go();
  }

  @Test
  public void testIntMinToDecimal() throws Exception {
    String query = "select cast((employee_id - employee_id + -2147483648) as decimal(28, 2)) as DEC_28," +
        "cast((employee_id - employee_id + -2147483648) as decimal(18, 2)) as DEC_18 from " +
        "cp.`employee.json` limit 1";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("DEC_28", "DEC_18")
        .baselineValues(new BigDecimal("-2147483648.00"), new BigDecimal("-2147483648.00"))
        .go();
  }

  @Test
  public void testDecimalAddConstant() throws Exception {
    String query = "select (cast('-1' as decimal(37, 3)) + cast (employee_id as decimal(37, 3))) as CNT " +
        "from cp.`employee.json` where employee_id <= 4";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("CNT")
        .baselineValues(new BigDecimal("0.000"))
        .baselineValues(new BigDecimal("1.000"))
        .baselineValues(new BigDecimal("3.000"))
        .go();
  }

  @Test
  public void testDecimalAddIntConstant() throws Exception {
    String query = "select 1 + cast(employee_id as decimal(9, 3)) as DEC_9 , 1 + cast(employee_id as decimal(37, 5)) as DEC_38 " +
        "from cp.`employee.json` where employee_id <= 2";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("DEC_9", "DEC_38")
        .baselineValues(new BigDecimal("2.000"), new BigDecimal("2.00000"))
        .baselineValues(new BigDecimal("3.000"), new BigDecimal("3.00000"))
        .go();
  }

  @Test
  public void testSignFunction() throws Exception {
    String query = "select sign(cast('1.23' as float)) as SIGN_FLOAT, sign(-1234.4567) as SIGN_DOUBLE, " +
      "sign(23) as SIGN_INT";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("SIGN_FLOAT", "SIGN_DOUBLE", "SIGN_INT")
        .baselineValues(1, -1, 1)
        .go();
  }


  @Test
  public void testPadFunctions() throws Exception {
    String query = "select rpad(first_name, 10) as RPAD_DEF, rpad(first_name, 10, '*') as RPAD_STAR, " +
      "lpad(first_name, 10) as LPAD_DEF, lpad(first_name, 10, '*') as LPAD_STAR, lpad(first_name, 2) as LPAD_TRUNC, " +
      "rpad(first_name, 2) as RPAD_TRUNC from cp.`employee.json` where employee_id = 1";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("RPAD_DEF", "RPAD_STAR", "LPAD_DEF", "LPAD_STAR", "LPAD_TRUNC", "RPAD_TRUNC")
        .baselineValues("Sheri     ", "Sheri*****", "     Sheri", "*****Sheri", "Sh", "Sh")
        .go();

  }

  @Test
  public void testExtractSecond() throws Exception {
    String query = "select extract(second from date '2008-2-23') as DATE_EXT, " +
      "extract(second from timestamp '2008-2-23 10:00:20.123') as TS_EXT, " +
        "extract(second from time '10:20:30.303') as TM_EXT";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("DATE_EXT", "TS_EXT", "TM_EXT")
        .baselineValues(0.0d, 20.123d, 30.303d)
        .go();
  }

  @Test
  public void testCastDecimalDouble() throws Exception {
    String query = "select cast((cast('1.0001' as decimal(18, 9))) as double) DECIMAL_DOUBLE_CAST";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("DECIMAL_DOUBLE_CAST")
        .baselineValues(1.0001d)
        .go();
  }

  @Test
  public void testExtractSecondFromInterval() throws Exception {
    String query = "select extract (second from interval '1 2:30:45.100' day to second) as EXT_INTDAY";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("EXT_INTDAY")
        .baselineValues(45.1d)
        .go();
  }

  @Test
  public void testFunctionCaseInsensitiveNames() throws Exception {
    String query = "SELECT to_date('2003/07/09', 'yyyy/MM/dd') as col1, " +
        "TO_DATE('2003/07/09', 'yyyy/MM/dd') as col2, " +
        "To_DaTe('2003/07/09', 'yyyy/MM/dd') as col3";

    LocalDate date = LocalDate.parse("2003-07-09");

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("col1", "col2", "col3")
        .baselineValues(date, date, date)
        .go();
  }

  @Test
  public void testDecimal18Decimal38Comparison() throws Exception {
    String query = "select cast('-999999999.999999999' as decimal(18, 9)) = cast('-999999999.999999999' as " +
      "decimal(38, 18)) as CMP";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("CMP")
        .baselineValues(true)
        .go();
  }

  @Test
  public void testOptiqDecimalCapping() throws Exception {
    String query = "select  cast('12345.678900000' as decimal(18, 9))=cast('12345.678900000' as decimal(38, 9)) as CMP";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("CMP")
        .baselineValues(true)
        .go();
  }

  @Test
  public void testNegative() throws Exception {
    String query = "select  negative(cast(2 as bigint)) as NEG\n";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("NEG")
        .baselineValues(-2L)
        .go();
  }

  @Test
  public void testOptiqValidationFunctions() throws Exception {
    String query = "select trim(first_name) as TRIM_STR, substring(first_name, 2) as SUB_STR " +
        "from cp.`employee.json` where employee_id = 1";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("TRIM_STR", "SUB_STR")
        .baselineValues("Sheri", "heri")
        .go();
  }

  @Test
  public void testToTimeStamp() throws Exception {
    String query = "select to_timestamp(cast('800120400.12312' as decimal(38, 5))) as DEC38_TS, " +
      "to_timestamp(200120400) as INT_TS\n";

    LocalDateTime result1 = Instant.ofEpochMilli(800120400123L).atZone(ZoneOffset.systemDefault()).toLocalDateTime();
    LocalDateTime result2 = Instant.ofEpochMilli(200120400000L).atZone(ZoneOffset.systemDefault()).toLocalDateTime();

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("DEC38_TS", "INT_TS")
        .baselineValues(result1, result2)
        .go();
  }

  @Test
  public void testCaseWithDecimalExpressions() throws Exception {
    String query = "select " +
        "case when true then cast(employee_id as decimal(15, 5)) else cast('0.0' as decimal(2, 1)) end as col1 " +
        "from cp.`employee.json` where employee_id = 1";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("col1")
        .baselineValues(new BigDecimal("1.00000"))
        .go();
  }


  /*
   * We may apply implicit casts in Hash Join while dealing with different numeric data types
   * For this to work we need to distribute the data based on a common key, below method
   * makes sure the hash value for different numeric types is the same for the same key
   */
  @Test
  public void testHash64() throws Exception {
    String query = "select " +
        "hash64AsDouble(cast(employee_id as int)) = hash64AsDouble(cast(employee_id as bigint)) col1, " +
        "hash64AsDouble(cast(employee_id as bigint)) = hash64AsDouble(cast(employee_id as float)) col2, " +
        "hash64AsDouble(cast(employee_id as float)) = hash64AsDouble(cast(employee_id as double)) col3, " +
        "hash64AsDouble(cast(employee_id as double)) = hash64AsDouble(cast(employee_id as decimal(9, 0))) col4, " +
        "hash64AsDouble(cast(employee_id as decimal(9, 0))) = hash64AsDouble(cast(employee_id as decimal(18, 0))) col5, " +
        "hash64AsDouble(cast(employee_id as decimal(18, 0))) = hash64AsDouble(cast(employee_id as decimal(28, 0))) col6, " +
        "hash64AsDouble(cast(employee_id as decimal(28, 0))) = hash64AsDouble(cast(employee_id as decimal(38, 0))) col7 " +
        "from cp.`employee.json`  where employee_id = 1";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("col1", "col2", "col3", "col4", "col5", "col6", "col7")
        .baselineValues(true, true, true, true, true, true, true)
        .go();

    java.util.Random seedGen = new java.util.Random();
    seedGen.setSeed(System.currentTimeMillis());
    int seed = seedGen.nextInt();

    String querytemplate = "select " +
            "hash64AsDouble(cast(employee_id as int), #RAND_SEED#) = hash64AsDouble(cast(employee_id as bigint), #RAND_SEED#) col1, " +
            "hash64AsDouble(cast(employee_id as bigint), #RAND_SEED#) = hash64AsDouble(cast(employee_id as float), #RAND_SEED#) col2, " +
            "hash64AsDouble(cast(employee_id as float), #RAND_SEED#) = hash64AsDouble(cast(employee_id as double), #RAND_SEED#) col3, " +
            "hash64AsDouble(cast(employee_id as double), #RAND_SEED#) = hash64AsDouble(cast(employee_id as decimal(9, 0)), #RAND_SEED#) col4, " +
            "hash64AsDouble(cast(employee_id as decimal(9, 0)), #RAND_SEED#) = hash64AsDouble(cast(employee_id as decimal(18, 0)), #RAND_SEED#) col5, " +
            "hash64AsDouble(cast(employee_id as decimal(18, 0)), #RAND_SEED#) = hash64AsDouble(cast(employee_id as decimal(28, 0)), #RAND_SEED#) col6, " +
            "hash64AsDouble(cast(employee_id as decimal(28, 0)), #RAND_SEED#) = hash64AsDouble(cast(employee_id as decimal(38, 0)), #RAND_SEED#) col7 " +
            "from cp.`employee.json` where employee_id = 1";

    String queryWithSeed = querytemplate.replaceAll("#RAND_SEED#", String.format("%d",seed));
    testBuilder()
            .sqlQuery(queryWithSeed)
            .unOrdered()
            .baselineColumns("col1", "col2", "col3", "col4", "col5", "col6", "col7")
            .baselineValues(true, true, true, true, true, true, true)
            .go();

  }


  @Test
  public void testHash32() throws Exception {
    String query = "select " +
        "hash32AsDouble(cast(employee_id as int)) = hash32AsDouble(cast(employee_id as bigint)) col1, " +
        "hash32AsDouble(cast(employee_id as bigint)) = hash32AsDouble(cast(employee_id as float)) col2, " +
        "hash32AsDouble(cast(employee_id as float)) = hash32AsDouble(cast(employee_id as double)) col3, " +
        "hash32AsDouble(cast(employee_id as double)) = hash32AsDouble(cast(employee_id as decimal(9, 0))) col4, " +
        "hash32AsDouble(cast(employee_id as decimal(9, 0))) = hash32AsDouble(cast(employee_id as decimal(18, 0))) col5, " +
        "hash32AsDouble(cast(employee_id as decimal(18, 0))) = hash32AsDouble(cast(employee_id as decimal(28, 0))) col6, " +
        "hash32AsDouble(cast(employee_id as decimal(28, 0))) = hash32AsDouble(cast(employee_id as decimal(38, 0))) col7 " +
        "from cp.`employee.json` where employee_id = 1";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("col1", "col2", "col3", "col4", "col5", "col6", "col7")
        .baselineValues(true, true, true, true, true, true, true)
        .go();

    java.util.Random seedGen = new java.util.Random();
    seedGen.setSeed(System.currentTimeMillis());
    int seed = seedGen.nextInt();

    String querytemplate = "select " +
            "hash32AsDouble(cast(employee_id as int), #RAND_SEED#) = hash32AsDouble(cast(employee_id as bigint), #RAND_SEED#) col1, " +
            "hash32AsDouble(cast(employee_id as bigint), #RAND_SEED#) = hash32AsDouble(cast(employee_id as float), #RAND_SEED#) col2, " +
            "hash32AsDouble(cast(employee_id as float),  #RAND_SEED#) = hash32AsDouble(cast(employee_id as double), #RAND_SEED#) col3, " +
            "hash32AsDouble(cast(employee_id as double), #RAND_SEED#) = hash32AsDouble(cast(employee_id as decimal(9, 0)), #RAND_SEED#) col4, " +
            "hash32AsDouble(cast(employee_id as decimal(9, 0)), #RAND_SEED#) = hash32AsDouble(cast(employee_id as decimal(18, 0)), #RAND_SEED#) col5, " +
            "hash32AsDouble(cast(employee_id as decimal(18, 0)), #RAND_SEED#) = hash32AsDouble(cast(employee_id as decimal(28, 0)), #RAND_SEED#) col6, " +
            "hash32AsDouble(cast(employee_id as decimal(28, 0)), #RAND_SEED#) = hash32AsDouble(cast(employee_id as decimal(38, 0)), #RAND_SEED#) col7 " +
            "from cp.`employee.json` where employee_id = 1";

    String queryWithSeed = querytemplate.replaceAll("#RAND_SEED#", String.format("%d",seed));
    testBuilder()
            .sqlQuery(queryWithSeed)
            .unOrdered()
            .baselineColumns("col1", "col2", "col3", "col4", "col5", "col6", "col7")
            .baselineValues(true, true, true, true, true, true, true)
            .go();

  }

  @Test
  public void testImplicitCastVarcharToDouble() throws Exception {
    // tests implicit cast from varchar to double
    testBuilder()
        .sqlQuery("select `integer` i, `float` f from cp.`jsoninput/input1.json` where `float` = '1.2'")
        .unOrdered()
        .baselineColumns("i", "f")
        .baselineValues(2001L, 1.2d)
        .go();
  }

  @Test
  public void testConcatSingleInput() throws Exception {
    String query = "select concat(employee_id) as col1 from cp.`employee.json` where employee_id = 1";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("col1")
        .baselineValues("1")
        .go();

    query = "select concat(null_column) as col1 from cp.`employee.json` where employee_id = 1";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("col1")
        .baselineValues("")
        .go();

    query = "select concat('foo') as col1 from cp.`employee.json` where employee_id = 1";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("col1")
        .baselineValues("foo")
        .go();
  }

  @Test
  public void testRandom() throws Exception {
    String query = "select 2*random()=2*random() as col1 from (values (1))";
    testBuilder()
            .sqlQuery(query)
            .unOrdered()
            .baselineColumns("col1")
            .baselineValues(false)
            .go();
  }

  /**
  * Test for DRILL-5645, where negation of expressions that do not contain
  * a RelNode input results in a NullPointerException
  */
  @Test
  public void testNegate() throws Exception {
    String query = "select -(2 * 2) as col1 from ( values ( 1 ) ) T ( C1 )";
    testBuilder()
            .sqlQuery(query)
            .unOrdered()
            .baselineColumns("col1")
            .baselineValues(-4)
            .go();

    // Test float
    query = "select -(1.1 * 1) as col1 from ( values ( 1 ) ) T ( C1 )";
    testBuilder()
            .sqlQuery(query)
            .unOrdered()
            .baselineColumns("col1")
            .baselineValues(new BigDecimal("-1.1"))
            .go();
  }

  @Test
  public void testBooleanConditionsMode() throws Exception {
    List<String> conditions = Arrays.asList(
        "employee_id IS NULL",
        "employee_id IS NOT NULL",
        "employee_id > 0 IS TRUE",
        "employee_id > 0 IS NOT TRUE",
        "employee_id > 0 IS FALSE",
        "employee_id > 0 IS NOT FALSE",
        "employee_id IS NULL OR position_id IS NULL",
        "employee_id IS NULL AND position_id IS NULL",
        "isdate(employee_id)",
        "NOT (employee_id IS NULL)");

    SchemaBuilder schemaBuilder = new SchemaBuilder()
        .add("col1", TypeProtos.MinorType.BIT);
    BatchSchema expectedSchema = new BatchSchemaBuilder()
        .withSchemaBuilder(schemaBuilder)
        .build();

    for (String condition : conditions) {
      testBuilder()
          .sqlQuery("SELECT %s AS col1 FROM cp.`employee.json` LIMIT 0", condition)
          .schemaBaseLine(expectedSchema)
          .go();
    }
  }

  @Test // DRILL-7297
  public void testErrorInUdf() throws Exception {
    expectedException.expect(UserRemoteException.class);
    expectedException.expectMessage(containsString("Error from UDF"));
    test("select error_function()");
  }

  @Test
  public void testParentPathFunction() throws Exception {
    testBuilder()
        .sqlQuery("select parentPath('/a/b/cde/f') as col1," +
            "parentPath('/a/b/cde/f/') as col2," +
            "parentPath('/a') as col3")
        .unOrdered()
        .baselineColumns("col1", "col2", "col3")
        .baselineValues("/a/b/cde", "/a/b/cde", "/")
        .go();
  }
}
