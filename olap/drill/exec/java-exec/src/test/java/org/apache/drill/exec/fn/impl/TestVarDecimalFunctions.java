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

import org.apache.drill.categories.SqlFunctionTest;
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.test.BaseTestQuery;
import org.hamcrest.CoreMatchers;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Category(SqlFunctionTest.class)
public class TestVarDecimalFunctions extends BaseTestQuery {

  @BeforeClass
  public static void enableDecimalDataType() {
    setSessionOption(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY, true);
  }

  @AfterClass
  public static void disableDecimalDataType() {
    resetSessionOption(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY);
  }

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  // Tests for math functions

  @Test
  public void testDecimalAdd() throws Exception {
    String query =
        "select\n" +
            // checks trimming of scale
            "cast('999999999999999999999999999.92345678912' as DECIMAL(38, 11))\n" +
            "+ cast('0.32345678912345678912345678912345678912' as DECIMAL(38, 38)) as s1,\n" +
            // sanitary checks
            "cast('1234567891234567891234567891234567.89' as DECIMAL(36, 2))\n" +
            "+ cast('123456789123456789123456789123456.789' as DECIMAL(36, 3)) as s2,\n" +
            "cast('1234567891234567891234567891234567.89' as DECIMAL(36, 2))\n" +
            "+ cast('0' as DECIMAL(36, 3)) as s3,\n" +
            "cast('15.02' as DECIMAL(4, 2)) - cast('12.93' as DECIMAL(4, 2)) as s4,\n" +
            "cast('11.02' as DECIMAL(4, 2)) - cast('12.93' as DECIMAL(4, 2)) as s5,\n" +
            "cast('0' as DECIMAL(36, 2)) - cast('12.93' as DECIMAL(36, 2)) as s6,\n" +
            // check trimming (digits after decimal point will be trimmed from result)
            "cast('9999999999999999999999999999234567891.1' as DECIMAL(38, 1))\n" +
            "+ cast('3234567891234567891234567891234567891.1' as DECIMAL(38, 1)) as s7";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("s1", "s2", "s3", "s4", "s5", "s6", "s7")
        .baselineValues(
            new BigDecimal("999999999999999999999999999.92345678912")
                .add(new BigDecimal("0.32345678912345678912345678912345678912"))
                .round(new MathContext(38, RoundingMode.HALF_UP)),
            new BigDecimal("1358024680358024680358024680358024.679"),
            new BigDecimal("1234567891234567891234567891234567.890"),
            new BigDecimal("2.09"), new BigDecimal("-1.91"), new BigDecimal("-12.93"),
            new BigDecimal("13234567891234567891234567890469135782"))
        .go();
  }

  @Test
  public void testDecimalAddOverflow() throws Exception {
    String query =
      "select\n" +
          "cast('99999999999999999999999999992345678912' as DECIMAL(38, 0))\n" +
          "+ cast('32345678912345678912345678912345678912' as DECIMAL(38, 0)) as s7";
    expectedException.expect(UserRemoteException.class);
    expectedException.expectMessage(
        CoreMatchers.containsString("VALIDATION ERROR: Value 132345678912345678912345678904691357820 " +
            "overflows specified precision 38 with scale 0."));
    test(query);
  }

  @Test
  public void testDecimalMultiply() throws Exception {
    String query =
        "select\n" +
            // checks trimming of scale
            "cast('999999999999999999999999999.92345678912' as DECIMAL(38, 11))\n" +
            "* cast('0.32345678912345678912345678912345678912' as DECIMAL(38, 38)) as s1,\n" +
            // sanitary checks
            "cast('1234567.89' as DECIMAL(9, 2))\n" +
            "* cast('-1.789' as DECIMAL(4, 3)) as s2,\n" +
            "cast('15.02' as DECIMAL(4, 2)) * cast('0' as DECIMAL(4, 2)) as s3,\n" +
            "cast('12.93123456789' as DECIMAL(13, 11)) * cast('1' as DECIMAL(1, 0)) as s4";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("s1", "s2", "s3", "s4")
        .baselineValues(new BigDecimal("999999999999999999999999999.92345678912")
                          .multiply(new BigDecimal("0.32345678912345678912345678912345678912"))
                          .round(new MathContext(38, RoundingMode.HALF_UP)),
            new BigDecimal("-2208641.95521"),
            new BigDecimal("0.0000"), new BigDecimal("12.93123456789"))
        .go();
  }

  @Test
  public void testDecimalMultiplyOverflow() throws Exception {
    String query = "select\n" +
        "cast('999999999999999999999999999.92345678912' as DECIMAL(38, 11))\n" +
        " * cast('323456789123.45678912345678912345678912' as DECIMAL(38, 26)) as s1";
    expectedException.expect(UserRemoteException.class);
    expectedException.expectMessage(
        CoreMatchers.containsString("VALIDATION ERROR: Value 323456789123456789123456789098698367900 " +
                "overflows specified precision 38 with scale 0."));
    test(query);
  }

  @Test
  public void testDecimalDivide() throws Exception {
    String query =
        "select\n" +
            // checks trimming of scale
            "cast('1.9999999999999999999999999999234567891' as DECIMAL(38, 37))\n" +
            "/ cast('0.0000000000000000000000000000000000001' as DECIMAL(38, 37)) as s1,\n" +
            // sanitary checks
            "cast('1234567.89' as DECIMAL(9, 2))\n" +
            "/ cast('-1.789' as DECIMAL(4, 3)) as s2,\n" +
            "cast('15.02' as DECIMAL(4, 2)) / cast('15.02' as DECIMAL(4, 2)) as s3,\n" +
            "cast('12.93123456789' as DECIMAL(13, 11)) / cast('1' as DECIMAL(1, 0)) as s4,\n" +
            "cast('0' as DECIMAL(1, 0)) / cast('15.02' as DECIMAL(4, 2)) as s5";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("s1", "s2", "s3", "s4", "s5")
        .baselineValues(new BigDecimal("19999999999999999999999999999234567891"),
            new BigDecimal("-690088.2560089"),
            new BigDecimal("1.0000000"), new BigDecimal("12.9312345678900"), new BigDecimal("0.000000"))
        .go();
  }

  @Test
  public void testDecimalDivideOverflow() throws Exception {
    String query = "select\n" +
        "cast('1.9999999999999999999999999999234567891' as DECIMAL(38, 37))\n" +
        " / cast('0.00000000000000000000000000000000000001' as DECIMAL(38, 38)) as s1";
    expectedException.expect(UserRemoteException.class);
    expectedException.expectMessage(
        CoreMatchers.containsString("VALIDATION ERROR: Value 199999999999999999999999999992345678910 " +
            "overflows specified precision 38 with scale 0"));
    test(query);
  }

  @Test
  public void testDecimalMod() throws Exception {
    String query =
        "select\n" +
            "mod(cast('1111' as DECIMAL(4, 0)), cast('12' as DECIMAL(2, 0))) as s1,\n" +
            "mod(cast('1234567' as DECIMAL(7, 0)),\n" +
            "cast('-9' as DECIMAL(1, 0))) as s2,\n" +
            "mod(cast('-1502' as DECIMAL(4, 0)), cast('15' as DECIMAL(2, 0))) as s3,\n" +
            "mod(cast('-987654' as DECIMAL(6, 0)), cast('-31' as DECIMAL(2, 0))) as s4";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("s1", "s2", "s3", "s4")
        .baselineValues(new BigDecimal("7"), new BigDecimal("1"),
            new BigDecimal("-2"), new BigDecimal("-25"))
        .go();
  }

  @Test
  public void testDecimalAbs() throws Exception {
    String query =
        "select\n" +
            "abs(cast('1111' as DECIMAL(4, 0))) as s1,\n" +
            "abs(cast('-1234567.123456' as DECIMAL(13, 6))) as s2,\n" +
            "abs(cast('-1502' as DECIMAL(4, 0))) as s3,\n" +
            "abs(cast('0' as DECIMAL(4, 0))) as s4,\n" +
            "abs(cast('-987654' as DECIMAL(6, 0))) as s5";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("s1", "s2", "s3", "s4", "s5")
        .baselineValues(new BigDecimal("1111"), new BigDecimal("1234567.123456"),
            new BigDecimal("1502"), new BigDecimal("0"), new BigDecimal("987654"))
        .go();
  }

  @Test
  public void testDecimalCeil() throws Exception {
    String query =
        "select\n" +
            "ceil(cast('1111.35' as DECIMAL(6, 2))) as s1,\n" +
            "ceiling(cast('1234567.123456' as DECIMAL(13, 6))) as s2,\n" +
            "ceil(cast('-1502.5' as DECIMAL(5, 1))) as s3,\n" +
            "ceiling(cast('987654.5' as DECIMAL(7, 1))) as s4,\n" +
            "ceil(cast('987654.00' as DECIMAL(8, 2))) as s5";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("s1", "s2", "s3", "s4", "s5")
        .baselineValues(new BigDecimal("1112"), new BigDecimal("1234568"),
            new BigDecimal("-1502"), new BigDecimal("987655"), new BigDecimal("987654"))
        .go();
  }

  @Test
  public void testDecimalFloor() throws Exception {
    String query =
        "select\n" +
            "floor(cast('1111.35' as DECIMAL(6, 2))) as s1,\n" +
            "floor(cast('1234567.123456' as DECIMAL(13, 6))) as s2,\n" +
            "floor(cast('-1502.5' as DECIMAL(5, 1))) as s3,\n" +
            "floor(cast('987654.5' as DECIMAL(7, 1))) as s4,\n" +
            "floor(cast('987654.00' as DECIMAL(8, 2))) as s5";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("s1", "s2", "s3", "s4", "s5")
        .baselineValues(new BigDecimal("1111"), new BigDecimal("1234567"),
            new BigDecimal("-1503"), new BigDecimal("987654"), new BigDecimal("987654"))
        .go();
  }

  @Test
  public void testDecimalTrunc() throws Exception {
    String query =
        "select\n" +
            "trunc(cast('1111.35' as DECIMAL(6, 2))) as s1,\n" +
            "truncate(cast('1234567.123456' as DECIMAL(13, 6))) as s2,\n" +
            "trunc(cast('-1502.5' as DECIMAL(5, 1))) as s3,\n" +
            "truncate(cast('987654.5' as DECIMAL(7, 1))) as s4,\n" +
            "trunc(cast('987654.00' as DECIMAL(8, 2))) as s5";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("s1", "s2", "s3", "s4", "s5")
        .baselineValues(new BigDecimal("1111"), new BigDecimal("1234567"),
            new BigDecimal("-1502"), new BigDecimal("987654"), new BigDecimal("987654"))
        .go();
  }

  @Test
  public void testDecimalRound() throws Exception {
    String query =
        "select\n" +
            "round(cast('1111.45' as DECIMAL(6, 2))) as s1,\n" +
            "round(cast('1234567.523456' as DECIMAL(13, 6))) as s2,\n" +
            "round(cast('-1502.5' as DECIMAL(5, 1))) as s3,\n" +
            "round(cast('-987654.4' as DECIMAL(7, 1))) as s4,\n" +
            "round(cast('987654.00' as DECIMAL(8, 2))) as s5";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("s1", "s2", "s3", "s4", "s5")
        .baselineValues(new BigDecimal("1111"), new BigDecimal("1234568"),
            new BigDecimal("-1503"), new BigDecimal("-987654"), new BigDecimal("987654"))
        .go();
  }

  @Test
  public void testDecimalSign() throws Exception {
    String query =
        "select\n" +
            "sign(cast('+1111.45' as DECIMAL(6, 2))) as s1,\n" +
            "sign(cast('-1234567.523456' as DECIMAL(13, 6))) as s2,\n" +
            "sign(cast('-1502.5' as DECIMAL(5, 1))) as s3,\n" +
            "sign(cast('987654.4' as DECIMAL(7, 1))) as s4,\n" +
            "sign(cast('0' as DECIMAL(8, 2))) as s5";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("s1", "s2", "s3", "s4", "s5")
        .baselineValues(1, -1, -1, 1, 0)
        .go();
  }

  @Test
  public void testDecimalRoundWithScale() throws Exception {
    String query =
        "select\n" +
            "round(cast('1111.45' as DECIMAL(6, 2)), 1) as s1,\n" +
            "round(cast('1234567.523456' as DECIMAL(13, 6)), 5) as s2,\n" +
            "round(cast('-1502.5' as DECIMAL(5, 1)), 0) as s3,\n" +
            "round(cast('-987654.4' as DECIMAL(7, 1)), 1) as s4,\n" +
            "round(cast('987654.00' as DECIMAL(8, 2)), 2) as s5";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("s1", "s2", "s3", "s4", "s5")
        .baselineValues(new BigDecimal("1111.5"), new BigDecimal("1234567.52346"),
            new BigDecimal("-1503"), new BigDecimal("-987654.4"), new BigDecimal("987654.00"))
        .go();
  }

  @Test
  public void testDecimalTruncWithScale() throws Exception {
    String query =
        "select\n" +
            "trunc(cast('1111.45' as DECIMAL(6, 2)), 1) as s1,\n" +
            "truncate(cast('1234567.523456' as DECIMAL(13, 6)), 5) as s2,\n" +
            "trunc(cast('-1502.5' as DECIMAL(5, 1)), 0) as s3,\n" +
            "truncate(cast('-987654.4' as DECIMAL(7, 1)), 1) as s4,\n" +
            "trunc(cast('987654.00' as DECIMAL(8, 2)), 2) as s5";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("s1", "s2", "s3", "s4", "s5")
        .baselineValues(new BigDecimal("1111.4"), new BigDecimal("1234567.52345"),
            new BigDecimal("-1502"), new BigDecimal("-987654.4"), new BigDecimal("987654.00"))
        .go();
  }

  // Tests for comparison functions

  @Test
  public void testDecimalEquals() throws Exception {
    String query =
        "select\n" +
            "cast('1.9999999999999999999999999999234567891' as DECIMAL(38, 37))\n" +
            "= cast('1.9999999999999999999999999999234567891' as DECIMAL(38, 37)) as s1,\n" +
            "cast('1.9999999999999999999999999999234567891' as DECIMAL(38, 37))\n" +
            "<> cast('1.9999999999999999999999999999234567891' as DECIMAL(38, 37)) as s2,\n" +
            // the same value but different scale and precision
            "cast('1234567.89' as DECIMAL(9, 2)) = cast('1234567.890' as DECIMAL(10, 3)) as s3,\n" +
            "cast('0' as DECIMAL(4, 2)) = cast('0' as DECIMAL(1, 0)) as s4,\n" +
            "cast('0' as DECIMAL(4, 2)) <> cast('0' as DECIMAL(1, 0)) as s5,\n" +
            "cast('12.93123456789' as DECIMAL(13, 11)) = cast('12.93123456788' as DECIMAL(13, 11)) as s6";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("s1", "s2", "s3", "s4", "s5", "s6")
        .baselineValues(true, false, true, true, false, false)
        .go();
  }

  @Test
  public void testDecimalLessThan() throws Exception {
    String query =
        "select\n" +
            "cast('1.9999999999999999999999999999234567891' as DECIMAL(38, 37))\n" +
            "< cast('1.9999999999999999999999999999234567891' as DECIMAL(38, 37)) as s1,\n" +
            "cast('1.9999999999999999999999999999234567891' as DECIMAL(38, 37))\n" +
            "< cast('1.9999999999999999999999999999234567892' as DECIMAL(38, 37)) as s2,\n" +
            // the same value but different scale and precision
            "cast('1234567.89' as DECIMAL(9, 2)) < cast('1234567.890' as DECIMAL(10, 3)) as s3,\n" +
            "cast('0' as DECIMAL(4, 2)) < cast('0' as DECIMAL(1, 0)) as s4,\n" +
            "cast('12.93123456789' as DECIMAL(13, 11)) < cast('12.93123456788' as DECIMAL(13, 11)) as s5";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("s1", "s2", "s3", "s4", "s5")
        .baselineValues(false, true, false, false, false)
        .go();
  }

  @Test
  public void testDecimalLessThanEquals() throws Exception {
    String query =
        "select\n" +
            "cast('1.9999999999999999999999999999234567891' as DECIMAL(38, 37))\n" +
            "<= cast('1.9999999999999999999999999999234567891' as DECIMAL(38, 37)) as s1,\n" +
            "cast('1.9999999999999999999999999999234567891' as DECIMAL(38, 37))\n" +
            "<= cast('1.9999999999999999999999999999234567892' as DECIMAL(38, 37)) as s2,\n" +
            // the same value but different scale and precision
            "cast('1234567.89' as DECIMAL(9, 2)) <= cast('1234567.890' as DECIMAL(10, 3)) as s3,\n" +
            "cast('0' as DECIMAL(4, 2)) <= cast('0' as DECIMAL(1, 0)) as s4,\n" +
            "cast('12.93123456789' as DECIMAL(13, 11)) <= cast('12.93123456788' as DECIMAL(13, 11)) as s5";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("s1", "s2", "s3", "s4", "s5")
        .baselineValues(true, true, true, true, false)
        .go();
  }

  @Test
  public void testDecimalGreaterThan() throws Exception {
    String query =
        "select\n" +
            "cast('1.9999999999999999999999999999234567891' as DECIMAL(38, 37))\n" +
            "> cast('1.9999999999999999999999999999234567891' as DECIMAL(38, 37)) as s1,\n" +
            "cast('1.9999999999999999999999999999234567891' as DECIMAL(38, 37))\n" +
            "> cast('1.9999999999999999999999999999234567892' as DECIMAL(38, 37)) as s2,\n" +
            // the same value but different scale and precision
            "cast('1234567.89' as DECIMAL(9, 2)) > cast('1234567.890' as DECIMAL(10, 3)) as s3,\n" +
            "cast('0' as DECIMAL(4, 2)) > cast('0' as DECIMAL(1, 0)) as s4,\n" +
            "cast('12.93123456789' as DECIMAL(13, 11)) > cast('12.93123456788' as DECIMAL(13, 11)) as s5";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("s1", "s2", "s3", "s4", "s5")
        .baselineValues(false, false, false, false, true)
        .go();
  }

  @Test
  public void testDecimalGreaterThanEquals() throws Exception {
    String query =
        "select\n" +
            "cast('1.9999999999999999999999999999234567891' as DECIMAL(38, 37))\n" +
            ">= cast('1.9999999999999999999999999999234567891' as DECIMAL(38, 37)) as s1,\n" +
            "cast('1.9999999999999999999999999999234567891' as DECIMAL(38, 37))\n" +
            ">= cast('1.9999999999999999999999999999234567892' as DECIMAL(38, 37)) as s2,\n" +
            // the same value but different scale and precision
            "cast('1234567.89' as DECIMAL(9, 2)) >= cast('1234567.890' as DECIMAL(10, 3)) as s3,\n" +
            "cast('0' as DECIMAL(4, 2)) >= cast('0' as DECIMAL(1, 0)) as s4,\n" +
            "cast('12.93123456789' as DECIMAL(13, 11)) >= cast('12.93123456788' as DECIMAL(13, 11)) as s5";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("s1", "s2", "s3", "s4", "s5")
        .baselineValues(true, false, true, true, true)
        .go();
  }

  @Test
  public void testDecimalCompareToNullsHigh() throws Exception {
    String query =
        "select\n" +
            "compare_to_nulls_high(cast('1.9999999999999999999999999999234567891' as DECIMAL(38, 37)),\n" +
            "cast('1.9999999999999999999999999999234567891' as DECIMAL(38, 37))) as s1,\n" +
            "compare_to_nulls_high(cast('1.9999999999999999999999999999234567891' as DECIMAL(38, 37)),\n" +
            "cast('1.9999999999999999999999999999234567892' as DECIMAL(38, 37))) as s2,\n" +
            // the same value but different scale and precision
            "compare_to_nulls_high(cast('1234567.89' as DECIMAL(9, 2)), cast('1234567.890' as DECIMAL(10, 3))) as s3,\n" +
            "compare_to_nulls_high(cast('0' as DECIMAL(4, 2)), cast('0' as DECIMAL(1, 0))) as s4,\n" +
            "compare_to_nulls_high(cast('0' as DECIMAL(4, 2)), cast(null as DECIMAL(1, 0))) as s5,\n" +
            "compare_to_nulls_high(cast('12.93123456789' as DECIMAL(13, 11)), " +
            "cast('12.93123456788' as DECIMAL(13, 11))) as s6";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("s1", "s2", "s3", "s4", "s5", "s6")
        .baselineValues(0, -1, 0, 0, -1, 1)
        .go();
  }

  @Test
  public void testDecimalCompareToNullsLow() throws Exception {
    String query =
        "select\n" +
            "compare_to_nulls_low(cast('1.9999999999999999999999999999234567891' as DECIMAL(38, 37)),\n" +
            "cast('1.9999999999999999999999999999234567891' as DECIMAL(38, 37))) as s1,\n" +
            "compare_to_nulls_low(cast('1.9999999999999999999999999999234567891' as DECIMAL(38, 37)),\n" +
            "cast('1.9999999999999999999999999999234567892' as DECIMAL(38, 37))) as s2,\n" +
            // the same value but different scale and precision
            "compare_to_nulls_low(cast('1234567.89' as DECIMAL(9, 2)), cast('1234567.890' as DECIMAL(10, 3))) as s3,\n" +
            "compare_to_nulls_low(cast('0' as DECIMAL(4, 2)), cast('0' as DECIMAL(1, 0))) as s4,\n" +
            "compare_to_nulls_low(cast('0' as DECIMAL(4, 2)), cast(null as DECIMAL(1, 0))) as s5,\n" +
            "compare_to_nulls_low(cast('12.93123456789' as DECIMAL(13, 11)), " +
            "cast('12.93123456788' as DECIMAL(13, 11))) as s6";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("s1", "s2", "s3", "s4", "s5", "s6")
        .baselineValues(0, -1, 0, 0, 1, 1)
        .go();
  }

  // Tests for cast functions

  @Test
  public void testCastIntDecimal() throws Exception {
    String query =
        "select\n" +
            "cast(i1 as DECIMAL(4, 0)) as s1,\n" +
            "cast(i2 as DECIMAL(7, 0)) as s2,\n" +
            "cast(i3 as DECIMAL(8, 0)) as s3,\n" +
            "cast(i4 as DECIMAL(6, 0)) as s4,\n" +
            "cast(i5 as DECIMAL(6, 0)) as s5,\n" +
            "cast(i6 as DECIMAL(10, 0)) as s6,\n" +
            "cast(i7 as DECIMAL(10, 0)) as s7\n" +
        "from (" +
            "select\n" +
                "cast(0 as int) as i1,\n" +
                "cast(1234567 as int) as i2,\n" +
                "cast(-15022222 as int) as i3,\n" +
                "cast(-987654 as int) as i4,\n" +
                "cast(987654 as int) as i5,\n" +
                "cast(%s as int) as i6,\n" +
                "cast(%s as int) as i7)";
    testBuilder()
        .sqlQuery(query, Integer.MAX_VALUE, Integer.MIN_VALUE)
        .ordered()
        .baselineColumns("s1", "s2", "s3", "s4", "s5", "s6", "s7")
        .baselineValues(BigDecimal.valueOf(0), BigDecimal.valueOf(1234567),
            BigDecimal.valueOf(-15022222), BigDecimal.valueOf(-987654),BigDecimal.valueOf(987654),
            BigDecimal.valueOf(Integer.MAX_VALUE), BigDecimal.valueOf(Integer.MIN_VALUE))
        .go();
  }

  @Test
  public void testCastDecimalInt() throws Exception {
    String query =
        "select\n" +
            "cast(i1 as int) as s1,\n" +
            "cast(i2 as int) as s2,\n" +
            "cast(i3 as int) as s3,\n" +
            "cast(i4 as int) as s4,\n" +
            "cast(i5 as int) as s5,\n" +
            "cast(i6 as int) as s6,\n" +
            "cast(i7 as int) as s7\n" +
        "from (" +
            "select\n" +
                "cast('999999999999999999999999999.92345678912' as DECIMAL(38, 11)) as i1,\n" +
                "cast('0.32345678912345678912345678912345678912' as DECIMAL(38, 38)) as i2,\n" +
                "cast('-1234567891234567891234567891234567.89' as DECIMAL(36, 2)) as i3,\n" +
                "cast('0' as DECIMAL(36, 3)) as i4,\n" +
                "cast('15.02' as DECIMAL(4, 2)) as i5,\n" +
                "cast('-15.02' as DECIMAL(4, 2)) as i6,\n" +
                "cast('0.7877' as DECIMAL(4, 4)) as i7)";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("s1", "s2", "s3", "s4", "s5", "s6", "s7")
        .baselineValues(
            new BigDecimal("999999999999999999999999999.92345678912")
                .setScale(0, BigDecimal.ROUND_HALF_UP).intValue(),
            0,
            new BigDecimal("-1234567891234567891234567891234567.89")
                .setScale(0, BigDecimal.ROUND_HALF_UP).intValue(),
            0, 15, -15, 1)
        .go();
  }

  @Test
  public void testCastBigIntDecimal() throws Exception {
    String query =
        "select\n" +
            "cast(i1 as DECIMAL(4, 0)) as s1,\n" +
            "cast(i2 as DECIMAL(7, 0)) as s2,\n" +
            "cast(i3 as DECIMAL(8, 0)) as s3,\n" +
            "cast(i4 as DECIMAL(6, 0)) as s4,\n" +
            "cast(i5 as DECIMAL(6, 0)) as s5,\n" +
            "cast(i6 as DECIMAL(19, 0)) as s6,\n" +
            "cast(i7 as DECIMAL(19, 0)) as s7\n" +
        "from (" +
            "select " +
                "cast(0 as bigint) as i1,\n" +
                "cast(1234567 as bigint) as i2,\n" +
                "cast(-15022222 as bigint) as i3,\n" +
                "cast(-987654 as bigint) as i4,\n" +
                "cast(987654 as bigint) as i5,\n" +
                "cast(%s as bigint) as i6,\n" +
                "cast(%s as bigint) as i7)";
    testBuilder()
        .sqlQuery(query, Long.MAX_VALUE, Long.MIN_VALUE)
        .ordered()
        .baselineColumns("s1", "s2", "s3", "s4", "s5", "s6", "s7")
        .baselineValues(new BigDecimal("0"), new BigDecimal("1234567"),
            new BigDecimal("-15022222"), new BigDecimal("-987654"), new BigDecimal("987654"),
            BigDecimal.valueOf(Long.MAX_VALUE), BigDecimal.valueOf(Long.MIN_VALUE))
        .go();
  }

  @Test
  public void testCastDecimalBigInt() throws Exception {
    String query =
        "select\n" +
            "cast(i1 as bigint) as s1,\n" +
            "cast(i2 as bigint) as s2,\n" +
            "cast(i3 as bigint) as s3,\n" +
            "cast(i4 as bigint) as s4,\n" +
            "cast(i5 as bigint) as s5,\n" +
            "cast(i6 as bigint) as s6,\n" +
            "cast(i7 as bigint) as s7\n" +
        "from (" +
            "select\n" +
                "cast('999999999999999999999999999.92345678912' as DECIMAL(38, 11)) as i1,\n" +
                "cast('0.32345678912345678912345678912345678912' as DECIMAL(38, 38)) as i2,\n" +
                "cast('-1234567891234567891234567891234567.89' as DECIMAL(36, 2)) as i3,\n" +
                "cast('0' as DECIMAL(36, 3)) as i4,\n" +
                "cast('15.02' as DECIMAL(4, 2)) as i5,\n" +
                "cast('-15.02' as DECIMAL(4, 2)) as i6,\n" +
                "cast('0.7877' as DECIMAL(4, 4)) as i7)";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("s1", "s2", "s3", "s4", "s5", "s6", "s7")
        .baselineValues(
            new BigDecimal("999999999999999999999999999.92345678912")
                .setScale(0, BigDecimal.ROUND_HALF_UP).longValue(),
            0L,
            new BigDecimal("-1234567891234567891234567891234567.89")
                .setScale(0, BigDecimal.ROUND_HALF_UP).longValue(),
            0L, 15L, -15L, 1L)
        .go();
  }

  @Test
  public void testCastFloatDecimal() throws Exception {
    String query =
        "select\n" +
            "cast(i1 as DECIMAL(4, 0)) as s1,\n" +
            "cast(i2 as DECIMAL(7, 6)) as s2,\n" +
            "cast(i3 as DECIMAL(8, 7)) as s3,\n" +
            "cast(i4 as DECIMAL(6, 6)) as s4,\n" +
            "cast(i5 as DECIMAL(7, 0)) as s5,\n" +
            "cast(i6 as DECIMAL(38, 38)) as s6\n" +
        "from (" +
            "select\n" +
                "cast(0 as float) as i1,\n" +
                "cast(1.234567 as float) as i2,\n" +
                "cast(-1.5022222 as float) as i3,\n" +
                "cast(-0.987654 as float) as i4,\n" +
                "cast(9999999 as float) as i5,\n" +
                "cast('%s' as float) as i6)";

    testBuilder()
        .sqlQuery(query, Float.MIN_VALUE)
        .unOrdered()
        .baselineColumns("s1", "s2", "s3", "s4", "s5", "s6")
        .baselineValues(BigDecimal.valueOf(0), new BigDecimal("1.234567"),
            new BigDecimal("-1.5022222"), new BigDecimal("-0.987654"), BigDecimal.valueOf(9999999),
            new BigDecimal(Float.MIN_VALUE).setScale(38, RoundingMode.HALF_UP))
        .go();
  }

  @Test
  public void testCastDecimalFloat() throws Exception {
    String query =
        "select\n" +
            "cast(i1 as float) as s1,\n" +
            "cast(i2 as float) as s2,\n" +
            "cast(i3 as float) as s3,\n" +
            "cast(i4 as float) as s4,\n" +
            "cast(i5 as float) as s5,\n" +
            "cast(i6 as float) as s6\n" +
        "from (" +
            "select\n" +
                "cast('999999999999999999999999999.92345678912' as DECIMAL(38, 11)) as i1,\n" +
                "cast('0.32345678912345678912345678912345678912' as DECIMAL(38, 38)) as i2,\n" +
                "cast('-1234567891234567891234567891234567.89' as DECIMAL(36, 2)) as i3,\n" +
                "cast('0' as DECIMAL(36, 3)) as i4,\n" +
                "cast('15.02' as DECIMAL(4, 2)) as i5,\n" +
                "cast('%s' as DECIMAL(38, 38)) as i6)";

    testBuilder()
        .sqlQuery(query, Float.MIN_VALUE)
        .unOrdered()
        .baselineColumns("s1", "s2", "s3", "s4", "s5", "s6")
        .baselineValues(new BigDecimal("999999999999999999999999999.92345678912").floatValue(),
            new BigDecimal("0.32345678912345678912345678912345678912").floatValue(),
            new BigDecimal("-1234567891234567891234567891234567.89").floatValue(),
            0f, 15.02f, 0.0f)
        .go();
  }

  @Test
  public void testCastDoubleDecimal() throws Exception {
    String query =
        "select\n" +
            "cast(i1 as DECIMAL(4, 0)) as s1,\n" +
            "cast(i2 as DECIMAL(7, 6)) as s2,\n" +
            "cast(i3 as DECIMAL(8, 7)) as s3,\n" +
            "cast(i4 as DECIMAL(6, 6)) as s4,\n" +
            "cast(i5 as DECIMAL(7, 0)) as s5,\n" +
            "cast(i6 as DECIMAL(38, 38)) as s6\n" +
        "from (" +
            "select\n" +
                "cast(0 as double) as i1,\n" +
                "cast(1.234567 as double) as i2,\n" +
                "cast(-1.5022222 as double) as i3,\n" +
                "cast(-0.987654 as double) as i4,\n" +
                "cast(9999999 as double) as i5,\n" +
                "cast('%e' as double) as i6)";

    testBuilder()
        .sqlQuery(query, Double.MIN_VALUE)
        .unOrdered()
        .baselineColumns("s1", "s2", "s3", "s4", "s5", "s6")
        .baselineValues(BigDecimal.valueOf(0), new BigDecimal("1.234567"),
            new BigDecimal("-1.5022222"), new BigDecimal("-0.987654"), BigDecimal.valueOf(9999999),
            new BigDecimal(String.valueOf(Double.MIN_VALUE)).setScale(38, RoundingMode.HALF_UP))
        .go();
  }

  @Test
  public void testCastDecimalDouble() throws Exception {
    String query =
        "select\n" +
            "cast(i1 as double) as s1,\n" +
            "cast(i2 as double) as s2,\n" +
            "cast(i3 as double) as s3,\n" +
            "cast(i4 as double) as s4,\n" +
            "cast(i5 as double) as s5,\n" +
            "cast(i6 as double) as s6\n" +
        "from (" +
            "select\n" +
                "cast('999999999999999999999999999.92345678912' as DECIMAL(38, 11)) as i1,\n" +
                "cast('0.32345678912345678912345678912345678912' as DECIMAL(38, 38)) as i2,\n" +
                "cast('-1234567891234567891234567891234567.89' as DECIMAL(36, 2)) as i3,\n" +
                "cast('0' as DECIMAL(36, 3)) as i4,\n" +
                "cast('15.02' as DECIMAL(4, 2)) as i5,\n" +
                "cast('%e' as DECIMAL(38, 38)) as i6)";

    testBuilder()
        .sqlQuery(query, Double.MIN_VALUE)
        .unOrdered()
        .baselineColumns("s1", "s2", "s3", "s4", "s5", "s6")
        .baselineValues(new BigDecimal("999999999999999999999999999.92345678912").doubleValue(),
            new BigDecimal("0.32345678912345678912345678912345678912").doubleValue(),
            new BigDecimal("-1234567891234567891234567891234567.89").doubleValue(),
            0d, 15.02, 0.)
        .go();
  }

  @Test
  public void testCastDecimalVarchar() throws Exception {
    String query =
        "select\n" +
            "cast(i1 as varchar) as s1,\n" +
            "cast(i2 as varchar) as s2,\n" +
            "cast(i3 as varchar) as s3,\n" +
            "cast(i4 as varchar) as s4,\n" +
            "cast(i5 as varchar) as s5\n" +
        "from (" +
            "select\n" +
                "cast('999999999999999999999999999.92345678912' as DECIMAL(38, 11)) as i1,\n" +
                "cast('0.32345678912345678912345678912345678912' as DECIMAL(38, 38)) as i2,\n" +
                "cast('-1234567891234567891234567891234567.89' as DECIMAL(36, 2)) as i3,\n" +
                "cast('0' as DECIMAL(36, 3)) as i4,\n" +
                "cast('15.02' as DECIMAL(4, 2)) as i5)";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("s1", "s2", "s3", "s4", "s5")
        .baselineValues("999999999999999999999999999.92345678912",
            "0.32345678912345678912345678912345678912",
            "-1234567891234567891234567891234567.89", "0.000", "15.02")
        .go();
  }

  @Test
  public void testDecimalToChar() throws Exception {
    String query =
        "select\n" +
            "to_char(i1, '#.#') as s1,\n" +
            "to_char(i2, '#.#') as s2,\n" +
            "to_char(i3, '#.#') as s3,\n" +
            "to_char(i4, '#.#') as s4,\n" +
            "to_char(i5, '#.#') as s5\n" +
            "from (" +
            "select\n" +
            "cast('999999999999999999999999999.92345678912' as DECIMAL(38, 11)) as i1,\n" +
            "cast('0.32345678912345678912345678912345678912' as DECIMAL(38, 38)) as i2,\n" +
            "cast('-1234567891234567891234567891234567.89' as DECIMAL(36, 2)) as i3,\n" +
            "cast('0' as DECIMAL(36, 3)) as i4,\n" +
            "cast('15.02' as DECIMAL(4, 2)) as i5)";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("s1", "s2", "s3", "s4", "s5")
        .baselineValues("999999999999999999999999999.9", "0.3",
            "-1234567891234567891234567891234567.9", "0", "15")
        .go();
  }

  @Test
  public void testDecimalNegate() throws Exception {
    String query =
        "select\n" +
            "negative(i1) as s1,\n" +
            "-i2 as s2,\n" +
            "negative(i3) as s3,\n" +
            "-i4 as s4,\n" +
            "negative(i5) as s5\n" +
            "from (" +
            "select\n" +
            "cast('999999999999999999999999999.92345678912' as DECIMAL(38, 11)) as i1,\n" +
            "cast('0.32345678912345678912345678912345678912' as DECIMAL(38, 38)) as i2,\n" +
            "cast('-1234567891234567891234567891234567.89' as DECIMAL(36, 2)) as i3,\n" +
            "cast('0' as DECIMAL(36, 3)) as i4,\n" +
            "cast('15.02' as DECIMAL(4, 2)) as i5)";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("s1", "s2", "s3", "s4", "s5")
        .baselineValues(new BigDecimal("-999999999999999999999999999.92345678912"),
            new BigDecimal("-0.32345678912345678912345678912345678912"),
            new BigDecimal("1234567891234567891234567891234567.89"),
            new BigDecimal("0.000"),
            new BigDecimal("-15.02"))
        .go();
  }
}
