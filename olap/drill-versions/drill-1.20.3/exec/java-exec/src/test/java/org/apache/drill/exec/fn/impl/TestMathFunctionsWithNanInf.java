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

import org.apache.commons.io.FileUtils;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.test.BaseTestQuery;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

public class TestMathFunctionsWithNanInf extends BaseTestQuery {


    @Test
    public void testIsNulFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select isnull(nan_col) as nan_col, isnull(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {false, false};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testIsNotNulFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select isnotnull(nan_col) as nan_col, isnotnull(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {true, true};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testEqualFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select equal(nan_col, nan_col) as nan_col, equal(inf_col, inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {true, true};
      evalTest(table_name, json, query, columns, values);

    }

    @Test
    public void testNotEqualFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select not_equal(nan_col, nan_col) as nan_col, not_equal(inf_col, inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {false, false};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testLessThanFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select less_than(nan_col, 5) as nan_col, less_than(inf_col, 5) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {false, false};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void tesGreaterThanFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select greater_than(nan_col, 5) as nan_col, greater_than(inf_col, 5) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {true, true};
      evalTest(table_name, json, query, columns, values);
    }


    @Test
    @Ignore // see DRILL-6018
    public void tesGreaterThanOrEqualToFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select greater_than_or_equal_to(nan_col, 5) as nan_col, " +
              "greater_than_or_equal_to(inf_col, cast('Infinity' as float)) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {false, true};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    @Ignore // see DRILL-6018
    public void testLessThanOrEqualToFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select less_than_or_equal_to(nan_col, 5) as nan_col," +
              " less_than_or_equal_to(inf_col, cast('Infinity' as float)) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {false, true};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testHashFunctions() throws Exception {
      test("select hash(cast('NaN' as double)) from (values(1))");
      test("select hash32(cast('NaN' as double))  from (values(1))");
      test("select hash32(cast('NaN' as double), 4)  from (values(1))");
      test("select hash(cast('Infinity' as double)) from (values(1))");
      test("select hash32(cast('Infinity' as double))  from (values(1))");
      test("select hash32(cast('Infinity' as double), 4)  from (values(1))");
      test("select hash64AsDouble(cast('NaN' as float)) from (values(1))");
      test("select hash64AsDouble(cast('NaN' as float), 4)  from (values(1))");
      test("select hash64AsDouble(cast('Infinity' as float)) from (values(1))");
      test("select hash64AsDouble(cast('Infinity' as float), 4)  from (values(1))");
      test("select hash32AsDouble(cast('NaN' as float)) from (values(1))");
      test("select hash32AsDouble(cast('NaN' as float), 4)  from (values(1))");
      test("select hash32AsDouble(cast('Infinity' as float)) from (values(1))");
      test("select hash32AsDouble(cast('Infinity' as float), 4)  from (values(1))");
      test("select hash64(cast('NaN' as float)) from (values(1))");
      test("select hash64(cast('NaN' as float), 4)  from (values(1))");
      test("select hash64(cast('Infinity' as float)) from (values(1))");
      test("select hash64(cast('Infinity' as float), 4)  from (values(1))");
    }

    @Test
    public void testSignFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select sign(nan_col) as nan_col, sign(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {0, 1};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testLogFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select log(nan_col) as nan_col, log(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, Double.POSITIVE_INFINITY};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testAddFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select add(nan_col, 3) as nan_col, add(inf_col, 3) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, Double.POSITIVE_INFINITY};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testSubtractFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select subtract(nan_col, 3) as nan_col, subtract(inf_col, 3) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, Double.POSITIVE_INFINITY};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testDivideFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select divide(nan_col, 3) as nan_col, divide(inf_col, 3) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, Double.POSITIVE_INFINITY};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testMultiplyFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select multiply(nan_col, 3) as nan_col, multiply(inf_col, 3) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, Double.POSITIVE_INFINITY};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testTanhFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select tanh(nan_col) as nan_col, tanh(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, 1.0};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testTanFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select tan(nan_col) as nan_col, tan(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, Double.NaN};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testAtanFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select atan(nan_col) as nan_col, atan(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, 1.5707963267948966};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testSinFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select sin(nan_col) as nan_col, sin(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, Double.NaN};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testAsinFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select asin(nan_col) as nan_col, asin(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, Double.NaN};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testSinhFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select sinh(nan_col) as nan_col, sinh(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, Double.POSITIVE_INFINITY};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testCosFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select cos(nan_col) as nan_col, cos(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, Double.NaN};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testAcosFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select acos(nan_col) as nan_col, acos(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, Double.NaN};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testCotFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select cot(nan_col) as nan_col, cot(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, Double.NaN};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testCoshFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select cosh(nan_col) as nan_col, cosh(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, Double.POSITIVE_INFINITY};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testSqrtFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":-Infinity, \"pos_inf_col\":Infinity}";
      String query = String.format("select sqrt(nan_col) as nan_col, sqrt(inf_col) as inf_col, sqrt(pos_inf_col) as pos_inf from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col", "pos_inf"};
      Object[] values = {Double.NaN, Double.NaN, Double.POSITIVE_INFINITY};
      evalTest(table_name, json, query, columns, values);
    }

  @Test
  public void testSqrtNestedFunction() throws Exception {
    String table_name = "nan_test.json";
    String json = "{\"nan_col\":NaN, \"inf_col\":-Infinity, \"pos_inf_col\":Infinity}";
    String query = String.format("select sqrt(sin(nan_col)) as nan_col, sqrt(sin(inf_col)) as inf_col, sqrt(sin(pos_inf_col)) as pos_inf from dfs.`%s`", table_name);
    String[] columns = {"nan_col", "inf_col", "pos_inf"};
    Object[] values = {Double.NaN, Double.NaN, Double.NaN};
    evalTest(table_name, json, query, columns, values);
  }

    @Test
    public void testCeilFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select ceil(nan_col) as nan_col, ceil(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, Double.POSITIVE_INFINITY};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testNegativeFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select negative(nan_col) as nan_col, negative(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, Double.NEGATIVE_INFINITY};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testAbsFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select abs(nan_col) as nan_col, abs(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, Double.POSITIVE_INFINITY};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testFloorFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select floor(nan_col) as nan_col, floor(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, Double.POSITIVE_INFINITY};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testExpFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select exp(nan_col) as nan_col, exp(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, Double.POSITIVE_INFINITY};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testCbrtFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select cbrt(nan_col) as nan_col, cbrt(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, Double.POSITIVE_INFINITY};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testModFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select mod(nan_col,1) as nan_col, mod(inf_col,1) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, Double.NaN};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testDegreesFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select degrees(nan_col) as nan_col, degrees(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, Double.POSITIVE_INFINITY};
      evalTest(table_name, json, query, columns, values);
    }

      @Test
      public void testTruncFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select trunc(nan_col,3) as nan_col, trunc(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, Double.NaN};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testPowerFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select power(nan_col, 2) as nan_col, power(inf_col, 2) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, Double.POSITIVE_INFINITY};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testRadiansFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select radians(nan_col) as nan_col, radians(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, Double.POSITIVE_INFINITY};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testRoundFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select round(nan_col) as nan_col, round(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {0.0, Double.valueOf(Long.MAX_VALUE)};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testCasthighFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select casthigh(nan_col) as nan_col, casthigh(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, Double.POSITIVE_INFINITY};
      evalTest(table_name, json, query, columns, values);
    }
    @Test
    public void testCastfloat4Function() throws Exception {
      String table_name = "nan_test.json";
      String json = "{\"nan_col\":NaN, \"inf_col\":Infinity}";
      String query = String.format("select castfloat4(nan_col) as nan_col, castfloat4(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Float.NaN, Float.POSITIVE_INFINITY};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testVarpopFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "[{\"nan_col\":NaN, \"inf_col\":Infinity}," +
              "{\"nan_col\":5, \"inf_col\":5}]";
      String query = String.format("select var_pop(nan_col) as nan_col, var_pop(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, Double.NaN};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testStddevsampFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "[{\"nan_col\":NaN, \"inf_col\":Infinity}," +
              "{\"nan_col\":5, \"inf_col\":5}]";
      String query = String.format("select stddev_samp(nan_col) as nan_col, stddev_samp(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, Double.NaN};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testVarsampFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "[{\"nan_col\":NaN, \"inf_col\":Infinity}," +
              "{\"nan_col\":5, \"inf_col\":5}]";
      String query = String.format("select var_samp(nan_col) as nan_col, var_samp(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, Double.NaN};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testStddevpopFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "[{\"nan_col\":NaN, \"inf_col\":Infinity}," +
              "{\"nan_col\":5, \"inf_col\":5}]";
      String query = String.format("select stddev_pop(nan_col) as nan_col, stddev_pop(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, Double.NaN};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testMinFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "[{\"nan_col\":NaN, \"inf_col\":Infinity}," +
              "{\"nan_col\":5.0, \"inf_col\":5.0}]";
      String query = String.format("select min(nan_col) as nan_col, min(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {5.0, 5.0};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testMaxFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "[{\"nan_col\":NaN, \"inf_col\":Infinity}," +
              "{\"nan_col\":5.0, \"inf_col\":5.0}]";
      String query = String.format("select max(nan_col) as nan_col, max(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, Double.POSITIVE_INFINITY};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testSumFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "[{\"nan_col\":NaN, \"inf_col\":Infinity}," +
             "{\"nan_col\":5.0, \"inf_col\":5.0}]";
      String query = String.format("select sum(nan_col) as nan_col, sum(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, Double.POSITIVE_INFINITY};
      evalTest(table_name, json, query, columns, values);
    }

    @Test
    public void testAvgFunction() throws Exception {
      String table_name = "nan_test.json";
      String json = "[{\"nan_col\":NaN, \"inf_col\":Infinity}," +
              "{\"nan_col\":5.0, \"inf_col\":5.0}]";
      String query = String.format("select avg(nan_col) as nan_col, avg(inf_col) as inf_col from dfs.`%s`", table_name);
      String[] columns = {"nan_col", "inf_col"};
      Object[] values = {Double.NaN, Double.POSITIVE_INFINITY};
      evalTest(table_name, json, query, columns, values);
    }

  @Test
  public void testNanInfLiteralConversion() throws Exception {
    String query =
      "select " +
      " cast('Infinity' as float) float_inf, " +
      " cast('-Infinity' as float) float_ninf, " +
      " cast('NaN' as float) float_nan, " +
      " cast('Infinity' as double) double_inf, " +
      " cast('-Infinity' as double) double_ninf, " +
      " cast('NaN' as double) double_nan";

    String[] columns = {
      "float_inf", "float_ninf", "float_nan",
      "double_inf", "double_ninf", "double_nan"
    };

    Object[] values = {
      Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NaN,
      Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN
    };

    testBuilder()
      .sqlQuery(query)
      .ordered()
      .baselineColumns(columns)
      .baselineValues(values)
      .build()
      .run();
  }

    private void evalTest(String table_name, String json, String query, String[] columns, Object[] values) throws Exception {
      File file = new File(dirTestWatcher.getRootDir(), table_name);
      try {
          FileUtils.writeStringToFile(file, json);
          test("alter session set `%s` = true", ExecConstants.JSON_READ_NUMBERS_AS_DOUBLE);
          testBuilder()
              .sqlQuery(query)
              .ordered()
              .baselineColumns(columns)
              .baselineValues(values)
              .build()
              .run();
      } finally {
          test("alter session set `%s` = false", ExecConstants.JSON_READ_NUMBERS_AS_DOUBLE);
          FileUtils.deleteQuietly(file);
      }
    }

}
