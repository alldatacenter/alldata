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
package org.apache.drill.exec.physical.impl.limit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.drill.PlanTestBase;
import org.apache.drill.categories.PlannerTest;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.expr.fn.impl.DateUtility;
import org.apache.drill.test.BaseTestQuery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

@Category(PlannerTest.class)
public class TestEarlyLimit0Optimization extends BaseTestQuery {

  private static final String viewName = "limitZeroEmployeeView";

  private static String wrapLimit0(final String query) {
    return "SELECT * FROM (" + query + ") LZT LIMIT 0";
  }

  @BeforeClass
  public static void createView() throws Exception {
    test("USE dfs.tmp");
    test("CREATE OR REPLACE VIEW %s AS SELECT " +
        "CAST(employee_id AS INT) AS employee_id, " +
        "CAST(full_name AS VARCHAR(25)) AS full_name, " +
        "CAST(position_id AS INTEGER) AS position_id, " +
        "CAST(department_id AS BIGINT) AS department_id," +
        "CAST(birth_date AS DATE) AS birth_date, " +
        "CAST(hire_date AS TIMESTAMP) AS hire_date, " +
        "CAST(salary AS DOUBLE) AS salary, " +
        "CAST(salary AS FLOAT) AS fsalary, " +
        "CAST((CASE WHEN marital_status = 'S' THEN true ELSE false END) AS BOOLEAN) AS single, " +
        "CAST(education_level AS VARCHAR(60)) AS education_level," +
        "CAST(gender AS CHAR(1)) AS gender " +
        "FROM cp.`employee.json` " +
        "ORDER BY employee_id " +
        "LIMIT 1", viewName);
  }

  @AfterClass
  public static void tearDownView() throws Exception {
    test("DROP VIEW " + viewName);
  }

  @Before
  public void setOption() throws Exception {
    test("SET `%s` = true", ExecConstants.EARLY_LIMIT0_OPT_KEY);
  }

  @After
  public void resetOption() throws Exception {
    test("RESET `%s`", ExecConstants.EARLY_LIMIT0_OPT_KEY);
  }

  // -------------------- SIMPLE QUERIES --------------------

  @Test
  public void infoSchema() throws Exception {
    testBuilder()
        .sqlQuery("DESCRIBE %s", viewName)
        .unOrdered()
        .baselineColumns("COLUMN_NAME", "DATA_TYPE", "IS_NULLABLE")
        .baselineValues("employee_id", "INTEGER", "YES")
        .baselineValues("full_name", "CHARACTER VARYING", "YES")
        .baselineValues("position_id", "INTEGER", "YES")
        .baselineValues("department_id", "BIGINT", "YES")
        .baselineValues("birth_date", "DATE", "YES")
        .baselineValues("hire_date", "TIMESTAMP", "YES")
        .baselineValues("salary", "DOUBLE", "YES")
        .baselineValues("fsalary", "FLOAT", "YES")
        .baselineValues("single", "BOOLEAN", "NO")
        .baselineValues("education_level", "CHARACTER VARYING", "YES")
        .baselineValues("gender", "CHARACTER", "YES")
        .go();
  }

  @Test
  public void simpleSelect() throws Exception {
    testBuilder()
        .sqlQuery("SELECT * FROM %s", viewName)
        .ordered()
        .baselineColumns("employee_id", "full_name", "position_id", "department_id", "birth_date", "hire_date",
            "salary", "fsalary", "single", "education_level", "gender")
        .baselineValues(1, "Sheri Nowmer", 1, 1L, LocalDate.parse("1961-08-26"),
            LocalDateTime.parse("1994-12-01 00:00:00", DateUtility.getDateTimeFormatter()), 80000.0D, 80000.0F, true, "Graduate Degree", "F")
        .go();
  }

  @Test
  public void simpleSelectLimit0() throws Exception {
    @SuppressWarnings("unchecked")
    final List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema = Lists.newArrayList(
        Pair.of(SchemaPath.getSimplePath("employee_id"), Types.optional(TypeProtos.MinorType.INT)),
        Pair.of(SchemaPath.getSimplePath("full_name"), Types.withPrecision(TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL, 25)),
        Pair.of(SchemaPath.getSimplePath("position_id"), Types.optional(TypeProtos.MinorType.INT)),
        Pair.of(SchemaPath.getSimplePath("department_id"), Types.optional(TypeProtos.MinorType.BIGINT)),
        Pair.of(SchemaPath.getSimplePath("birth_date"), Types.optional(TypeProtos.MinorType.DATE)),
        Pair.of(SchemaPath.getSimplePath("hire_date"), Types.optional(TypeProtos.MinorType.TIMESTAMP)),
        Pair.of(SchemaPath.getSimplePath("salary"), Types.optional(TypeProtos.MinorType.FLOAT8)),
        Pair.of(SchemaPath.getSimplePath("fsalary"), Types.optional(TypeProtos.MinorType.FLOAT4)),
        Pair.of(SchemaPath.getSimplePath("single"), Types.required(TypeProtos.MinorType.BIT)),
        Pair.of(SchemaPath.getSimplePath("education_level"), Types.withPrecision(TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL, 60)),
        Pair.of(SchemaPath.getSimplePath("gender"), Types.withPrecision(TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL, 1)));

    testBuilder()
        .sqlQuery(wrapLimit0(String.format("SELECT * FROM %s", viewName)))
        .schemaBaseLine(expectedSchema)
        .go();

    checkThatQueryPlanIsOptimized("SELECT * FROM " + viewName);
  }

  private static void checkThatQueryPlanIsOptimized(final String query) throws Exception {
    PlanTestBase.testPlanMatchingPatterns(
        wrapLimit0(query),
        new String[]{
            ".*Project.*\n" +
                ".*Scan.*RelDataTypeReader.*"
        },
        new String[]{});
  }

  // -------------------- AGGREGATE FUNC. QUERIES --------------------

  private static String getAggQuery(final String functionName) {
    return "SELECT " +
        functionName + "(employee_id) AS e, " +
        functionName + "(position_id) AS p, " +
        functionName + "(department_id) AS d, " +
        functionName + "(salary) AS s, " +
        functionName + "(fsalary) AS f " +
        "FROM " + viewName;
  }

  @Test
  public void sums() throws Exception {
    final String query = getAggQuery("SUM");

    @SuppressWarnings("unchecked")
    final List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema = Lists.newArrayList(
        Pair.of(SchemaPath.getSimplePath("e"), Types.optional(TypeProtos.MinorType.BIGINT)),
        Pair.of(SchemaPath.getSimplePath("p"), Types.optional(TypeProtos.MinorType.BIGINT)),
        Pair.of(SchemaPath.getSimplePath("d"), Types.optional(TypeProtos.MinorType.BIGINT)),
        Pair.of(SchemaPath.getSimplePath("s"), Types.optional(TypeProtos.MinorType.FLOAT8)),
        Pair.of(SchemaPath.getSimplePath("f"), Types.optional(TypeProtos.MinorType.FLOAT8)));

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("e", "p", "d", "s", "f")
        .baselineValues(1L, 1L, 1L, 80000D, 80000D)
        .go();

    testBuilder()
        .sqlQuery(wrapLimit0(query))
        .schemaBaseLine(expectedSchema)
        .go();

    checkThatQueryPlanIsOptimized(query);
  }

  @Test
  public void counts() throws Exception {
    final String query = getAggQuery("COUNT");

    @SuppressWarnings("unchecked")
    final List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema = Lists.newArrayList(
        Pair.of(SchemaPath.getSimplePath("e"), Types.required(TypeProtos.MinorType.BIGINT)),
        Pair.of(SchemaPath.getSimplePath("p"), Types.required(TypeProtos.MinorType.BIGINT)),
        Pair.of(SchemaPath.getSimplePath("d"), Types.required(TypeProtos.MinorType.BIGINT)),
        Pair.of(SchemaPath.getSimplePath("s"), Types.required(TypeProtos.MinorType.BIGINT)),
        Pair.of(SchemaPath.getSimplePath("f"), Types.required(TypeProtos.MinorType.BIGINT)));

    testBuilder()
        .sqlQuery(query)
        .baselineColumns("e", "p", "d", "s", "f")
        .ordered()
        .baselineValues(1L, 1L, 1L, 1L, 1L)
        .go();

    testBuilder()
        .sqlQuery(wrapLimit0(query))
        .schemaBaseLine(expectedSchema)
        .go();

    checkThatQueryPlanIsOptimized(query);
  }

  private void minAndMaxTest(final String functionName) throws Exception {
    final String query = getAggQuery(functionName);

    @SuppressWarnings("unchecked")
    final List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema = Lists.newArrayList(
        Pair.of(SchemaPath.getSimplePath("e"), Types.optional(TypeProtos.MinorType.INT)),
        Pair.of(SchemaPath.getSimplePath("p"), Types.optional(TypeProtos.MinorType.INT)),
        Pair.of(SchemaPath.getSimplePath("d"), Types.optional(TypeProtos.MinorType.BIGINT)),
        Pair.of(SchemaPath.getSimplePath("s"), Types.optional(TypeProtos.MinorType.FLOAT8)),
        Pair.of(SchemaPath.getSimplePath("f"), Types.optional(TypeProtos.MinorType.FLOAT4)));

    testBuilder()
        .sqlQuery(query)
        .baselineColumns("e", "p", "d", "s", "f")
        .ordered()
        .baselineValues(1, 1, 1L, 80_000D, 80_000F)
        .go();

    testBuilder()
        .sqlQuery(wrapLimit0(query))
        .schemaBaseLine(expectedSchema)
        .go();

    checkThatQueryPlanIsOptimized(query);
  }

  @Test
  public void mins() throws Exception {
    minAndMaxTest("MIN");
  }

  @Test
  public void maxs() throws Exception {
    minAndMaxTest("MAX");
  }

  @Test
  public void avgs() throws Exception {
    final String query = getAggQuery("AVG");

    @SuppressWarnings("unchecked")
    final List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema = Lists.newArrayList(
        Pair.of(SchemaPath.getSimplePath("e"), Types.optional(TypeProtos.MinorType.FLOAT8)),
        Pair.of(SchemaPath.getSimplePath("p"), Types.optional(TypeProtos.MinorType.FLOAT8)),
        Pair.of(SchemaPath.getSimplePath("d"), Types.optional(TypeProtos.MinorType.FLOAT8)),
        Pair.of(SchemaPath.getSimplePath("s"), Types.optional(TypeProtos.MinorType.FLOAT8)),
        Pair.of(SchemaPath.getSimplePath("f"), Types.optional(TypeProtos.MinorType.FLOAT8)));

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("e", "p", "d", "s", "f")
        .baselineValues(1D, 1D, 1D, 80_000D, 80_000D)
        .go();

    testBuilder()
        .sqlQuery(wrapLimit0(query))
        .schemaBaseLine(expectedSchema)
        .go();

    checkThatQueryPlanIsOptimized(query);
  }

  @Test
  public void measures() throws Exception {
    final String query = "SELECT " +
        "STDDEV_SAMP(employee_id) AS s, " +
        "STDDEV_POP(position_id) AS p, " +
        "AVG(position_id) AS a, " +
        "COUNT(position_id) AS c " +
        "FROM " + viewName;

    @SuppressWarnings("unchecked")
    final List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema = Lists.newArrayList(
        Pair.of(SchemaPath.getSimplePath("s"), Types.optional(TypeProtos.MinorType.FLOAT8)),
        Pair.of(SchemaPath.getSimplePath("p"), Types.optional(TypeProtos.MinorType.FLOAT8)),
        Pair.of(SchemaPath.getSimplePath("a"), Types.optional(TypeProtos.MinorType.FLOAT8)),
        Pair.of(SchemaPath.getSimplePath("c"), Types.required(TypeProtos.MinorType.BIGINT)));

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("s", "p", "a", "c")
        .baselineValues(null, 0.0D, 1.0D, 1L)
         .go();

    testBuilder()
        .sqlQuery(wrapLimit0(query))
        .schemaBaseLine(expectedSchema)
        .go();

    checkThatQueryPlanIsOptimized(query);
  }

  @Test
  public void nullableCount() throws Exception {
    final String query = "SELECT " +
        "COUNT(CASE WHEN position_id = 1 THEN NULL ELSE position_id END) AS c FROM " + viewName;

    @SuppressWarnings("unchecked")
    final List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema = Lists.newArrayList(
        Pair.of(SchemaPath.getSimplePath("c"), Types.required(TypeProtos.MinorType.BIGINT)));

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("c")
        .baselineValues(0L)
        .go();

    testBuilder()
        .sqlQuery(wrapLimit0(query))
        .schemaBaseLine(expectedSchema)
        .go();

    checkThatQueryPlanIsOptimized(query);
  }

  @Test
  public void nullableSumAndCount() throws Exception {
    final String query = String.format("SELECT " +
        "COUNT(position_id) AS c, " +
        "SUM(CAST((CASE WHEN position_id = 1 THEN NULL ELSE position_id END) AS INT)) AS p " +
        "FROM %s", viewName);

    @SuppressWarnings("unchecked")
    final List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema = Lists.newArrayList(
        Pair.of(SchemaPath.getSimplePath("c"), Types.required(TypeProtos.MinorType.BIGINT)),
        Pair.of(SchemaPath.getSimplePath("p"), Types.optional(TypeProtos.MinorType.BIGINT)));

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("c", "p")
        .baselineValues(1L, null)
        .go();

    testBuilder()
        .sqlQuery(wrapLimit0(query))
        .schemaBaseLine(expectedSchema)
        .go();

    checkThatQueryPlanIsOptimized(query);
  }

  @Test
  public void castSum() throws Exception {
    final String query = "SELECT CAST(SUM(position_id) AS INT) AS s FROM cp.`employee.json`";

    @SuppressWarnings("unchecked")
    final List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema = Lists.newArrayList(
        Pair.of(SchemaPath.getSimplePath("s"), Types.optional(TypeProtos.MinorType.INT)));

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("s")
        .baselineValues(18422)
        .go();

    testBuilder()
        .sqlQuery(wrapLimit0(query))
        .schemaBaseLine(expectedSchema)
        .go();

    checkThatQueryPlanIsOptimized(query);
  }

  @Test
  public void sumCast() throws Exception {
    final String query = "SELECT SUM(CAST(position_id AS INT)) AS s FROM cp.`employee.json`";

    @SuppressWarnings("unchecked")
    final List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema = Lists.newArrayList(
        Pair.of(SchemaPath.getSimplePath("s"), Types.optional(TypeProtos.MinorType.BIGINT)));

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("s")
        .baselineValues(18422L)
        .go();

    testBuilder()
        .sqlQuery(wrapLimit0(query))
        .schemaBaseLine(expectedSchema)
        .go();

    checkThatQueryPlanIsOptimized(query);
  }

  @Test
  public void sumsAndCounts1() throws Exception {
    final String query = String.format("SELECT " +
        "COUNT(*) as cs, " +
        "COUNT(1) as c1, " +
        "COUNT(employee_id) as cc, " +
        "SUM(1) as s1," +
        "department_id " +
        " FROM %s GROUP BY department_id", viewName);

    @SuppressWarnings("unchecked")
    final List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema = Lists.newArrayList(
        Pair.of(SchemaPath.getSimplePath("cs"), Types.required(TypeProtos.MinorType.BIGINT)),
        Pair.of(SchemaPath.getSimplePath("c1"), Types.required(TypeProtos.MinorType.BIGINT)),
        Pair.of(SchemaPath.getSimplePath("cc"), Types.required(TypeProtos.MinorType.BIGINT)),
        Pair.of(SchemaPath.getSimplePath("s1"), Types.required(TypeProtos.MinorType.BIGINT)),
        Pair.of(SchemaPath.getSimplePath("department_id"), Types.optional(TypeProtos.MinorType.BIGINT)));

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("cs", "c1", "cc", "s1", "department_id")
        .baselineValues(1L, 1L, 1L, 1L, 1L)
        .go();

    testBuilder()
        .sqlQuery(wrapLimit0(query))
        .schemaBaseLine(expectedSchema)
        .go();

    checkThatQueryPlanIsOptimized(query);
  }

  @Test
  public void sumsAndCounts2() throws Exception {
    final String query = "SELECT " +
        "SUM(1) as s1, " +
        "COUNT(1) as c1, " +
        "COUNT(*) as cs, " +
        "COUNT(CAST(n_regionkey AS INT)) as cc " +
        "FROM cp.`tpch/nation.parquet` " +
        "GROUP BY CAST(n_regionkey AS INT)";

    @SuppressWarnings("unchecked")
    final List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema = Lists.newArrayList(
        Pair.of(SchemaPath.getSimplePath("s1"), Types.required(TypeProtos.MinorType.BIGINT)),
        Pair.of(SchemaPath.getSimplePath("c1"), Types.required(TypeProtos.MinorType.BIGINT)),
        Pair.of(SchemaPath.getSimplePath("cs"), Types.required(TypeProtos.MinorType.BIGINT)),
        Pair.of(SchemaPath.getSimplePath("cc"), Types.required(TypeProtos.MinorType.BIGINT)));

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("s1", "c1", "cs", "cc")
        .baselineValues(5L, 5L, 5L, 5L)
        .baselineValues(5L, 5L, 5L, 5L)
        .baselineValues(5L, 5L, 5L, 5L)
        .baselineValues(5L, 5L, 5L, 5L)
        .baselineValues(5L, 5L, 5L, 5L)
        .go();

    testBuilder()
        .sqlQuery(wrapLimit0(query))
        .schemaBaseLine(expectedSchema)
        .go();

    checkThatQueryPlanIsOptimized(query);

  }

  @Test
  public void rank() throws Exception {
    final String query = "SELECT RANK() OVER(PARTITION BY employee_id ORDER BY employee_id) AS r FROM " + viewName;

    @SuppressWarnings("unchecked")
    final List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema = Lists.newArrayList(
        Pair.of(SchemaPath.getSimplePath("r"), Types.required(TypeProtos.MinorType.BIGINT)));

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("r")
        .baselineValues(1L)
        .go();

    testBuilder()
        .sqlQuery(wrapLimit0(query))
        .schemaBaseLine(expectedSchema)
        .go();

    checkThatQueryPlanIsOptimized(query);
  }

  // -------------------- SCALAR FUNC. QUERIES --------------------

  @Test
  public void cast() throws Exception {
    final String query = String.format("SELECT CAST(fsalary AS DOUBLE) AS d," +
        "CAST(employee_id AS BIGINT) AS e FROM %s", viewName);

    @SuppressWarnings("unchecked")
    final List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema = Lists.newArrayList(
        Pair.of(SchemaPath.getSimplePath("d"), Types.optional(TypeProtos.MinorType.FLOAT8)),
        Pair.of(SchemaPath.getSimplePath("e"), Types.optional(TypeProtos.MinorType.BIGINT)));

    testBuilder()
        .sqlQuery(query)
        .baselineColumns("d", "e")
        .ordered()
        .baselineValues(80_000D, 1L)
        .go();

    testBuilder()
        .sqlQuery(wrapLimit0(query))
        .schemaBaseLine(expectedSchema)
        .go();

    checkThatQueryPlanIsOptimized(query);
  }

  public void concatTest(final String query, int precision, boolean isNullable) throws Exception {
    @SuppressWarnings("unchecked")
    final List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema =
        Lists.newArrayList(Pair.of(SchemaPath.getSimplePath("c"), Types.withPrecision(TypeProtos.MinorType.VARCHAR,
            isNullable ? TypeProtos.DataMode.OPTIONAL : TypeProtos.DataMode.REQUIRED, precision)));

    testBuilder()
        .sqlQuery(query)
        .baselineColumns("c")
        .ordered()
        .baselineValues("Sheri NowmerGraduate Degree")
        .go();

    testBuilder()
        .sqlQuery(wrapLimit0(query))
        .schemaBaseLine(expectedSchema)
        .go();

    checkThatQueryPlanIsOptimized(query);
  }

  @Test
  public void concat() throws Exception {
    concatTest("SELECT CONCAT(full_name, education_level) AS c FROM " + viewName, 85, false);
  }

  @Test
  public void concatOp() throws Exception {
    concatTest("SELECT full_name || education_level AS c FROM " + viewName, 85, true);
  }

  @Test
  public void extract() throws Exception {
    final String query = "SELECT EXTRACT(YEAR FROM hire_date) AS e FROM " + viewName;

    @SuppressWarnings("unchecked")
    final List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema = Lists.newArrayList(
        Pair.of(SchemaPath.getSimplePath("e"), Types.optional(TypeProtos.MinorType.BIGINT)));

    testBuilder()
        .sqlQuery(query)
        .baselineColumns("e")
        .ordered()
        .baselineValues(1994L)
        .go();

    testBuilder()
        .sqlQuery(wrapLimit0(query))
        .schemaBaseLine(expectedSchema)
        .go();

    checkThatQueryPlanIsOptimized(query);
  }

  @Test
  public void binary() throws Exception {
    final String query = "SELECT " +
        "single AND true AS b, " +
        "full_name || education_level AS c, " +
        "position_id / position_id AS d, " +
        "position_id = position_id AS e, " +
        "position_id > position_id AS g, " +
        "position_id >= position_id AS ge, " +
        "position_id IN (0, 1) AS i, +" +
        "position_id < position_id AS l, " +
        "position_id <= position_id AS le, " +
        "position_id - position_id AS m, " +
        "position_id * position_id AS mu, " +
        "position_id <> position_id AS n, " +
        "single OR false AS o, " +
        "position_id + position_id AS p FROM " + viewName;

    @SuppressWarnings("unchecked")
    final List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema = Lists.newArrayList(
        Pair.of(SchemaPath.getSimplePath("b"), Types.required(TypeProtos.MinorType.BIT)),
        Pair.of(SchemaPath.getSimplePath("c"), Types.withPrecision(TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL, 85)),
        Pair.of(SchemaPath.getSimplePath("d"), Types.optional(TypeProtos.MinorType.INT)),
        Pair.of(SchemaPath.getSimplePath("e"), Types.optional(TypeProtos.MinorType.BIT)),
        Pair.of(SchemaPath.getSimplePath("g"), Types.optional(TypeProtos.MinorType.BIT)),
        Pair.of(SchemaPath.getSimplePath("ge"), Types.optional(TypeProtos.MinorType.BIT)),
        Pair.of(SchemaPath.getSimplePath("i"), Types.optional(TypeProtos.MinorType.BIT)),
        Pair.of(SchemaPath.getSimplePath("l"), Types.optional(TypeProtos.MinorType.BIT)),
        Pair.of(SchemaPath.getSimplePath("le"), Types.optional(TypeProtos.MinorType.BIT)),
        Pair.of(SchemaPath.getSimplePath("m"), Types.optional(TypeProtos.MinorType.INT)),
        Pair.of(SchemaPath.getSimplePath("mu"), Types.optional(TypeProtos.MinorType.INT)),
        Pair.of(SchemaPath.getSimplePath("n"), Types.optional(TypeProtos.MinorType.BIT)),
        Pair.of(SchemaPath.getSimplePath("o"), Types.required(TypeProtos.MinorType.BIT)),
        Pair.of(SchemaPath.getSimplePath("p"), Types.optional(TypeProtos.MinorType.INT)));

    testBuilder()
        .sqlQuery(query)
        .baselineColumns("b", "c", "d", "e", "g", "ge", "i", "l", "le", "m", "mu", "n", "o", "p")
        .ordered()
        .baselineValues(true, "Sheri NowmerGraduate Degree", 1, true, false, true, true, false, true,
            0, 1, false, true, 2)
        .go();

    testBuilder()
        .sqlQuery(wrapLimit0(query))
        .schemaBaseLine(expectedSchema)
        .go();

    checkThatQueryPlanIsOptimized(query);
  }

  public void substringTest(final String query, int precision) throws Exception {
    @SuppressWarnings("unchecked")
    final List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema = Lists.newArrayList(
        Pair.of(SchemaPath.getSimplePath("s"), Types.withPrecision(TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL, precision)));

    testBuilder()
        .sqlQuery(query)
        .baselineColumns("s")
        .ordered()
        .baselineValues("Sheri")
        .go();

    testBuilder()
        .sqlQuery(wrapLimit0(query))
        .schemaBaseLine(expectedSchema)
        .go();

    checkThatQueryPlanIsOptimized(query);
  }

  @Test
  public void substring() throws Exception {
    substringTest("SELECT SUBSTRING(full_name, 1, 5) AS s FROM " + viewName, Types.MAX_VARCHAR_LENGTH);
  }

  @Test
  public void substr() throws Exception {
    substringTest("SELECT SUBSTR(full_name, 1, 5) AS s FROM " + viewName, Types.MAX_VARCHAR_LENGTH);
  }
}
