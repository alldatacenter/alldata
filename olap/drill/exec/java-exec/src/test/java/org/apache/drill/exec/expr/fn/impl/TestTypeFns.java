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
package org.apache.drill.exec.expr.fn.impl;

import static org.junit.Assert.assertEquals;

import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestTypeFns extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    // Use the following three lines if you add a function
    // to avoid the need for a full Drill build.
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
        .configProperty("drill.classpath.scanning.cache.enabled", false);
    startCluster(builder);

    // Use the following line if a full Drill build has been
    // done since adding new functions.
//    startCluster(ClusterFixture.builder(dirTestWatcher).maxParallelization(1));
  }

  @Test
  public void testTypeOf() throws RpcException {
    // SMALLINT not supported in CAST
    //doTypeOfTest("SMALLINT");
    doTypeOfTest("INT");
    doTypeOfTest("BIGINT");
    doTypeOfTest("VARCHAR");
    doTypeOfTest("FLOAT", "FLOAT4");
    doTypeOfTest("DOUBLE", "FLOAT8");
    doTypeOfTestSpecial("a", "true", "BIT");
    doTypeOfTestSpecial("a", "CURRENT_DATE", "DATE");
    doTypeOfTestSpecial("a", "CURRENT_TIME", "TIME");
    doTypeOfTestSpecial("a", "CURRENT_TIMESTAMP", "TIMESTAMP");
    doTypeOfTestSpecial("a", "AGE(CURRENT_TIMESTAMP)", "INTERVAL");
    doTypeOfTestSpecial("BINARY_STRING(a)", "'\\xde\\xad\\xbe\\xef'", "VARBINARY");
    try {
      client.alterSession(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY, true);
      doTypeOfTestSpecial("CAST(a AS DECIMAL)", "1", "VARDECIMAL");
      doTypeOfTestSpecial("CAST(a AS DECIMAL(6, 3))", "1", "VARDECIMAL");
    } finally {
      client.resetSession(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY);
    }
  }

  private void doTypeOfTest(String type) throws RpcException {
    doTypeOfTest(type, type);
  }

  private void doTypeOfTest(String castType, String resultType) throws RpcException {

    // typeof() returns types using the internal names.

    String sql = "SELECT typeof(CAST(a AS " + castType + ")) FROM (VALUES (1)) AS T(a)";
    String result = queryBuilder().sql(sql).singletonString();
    assertEquals(resultType, result);

    // For typeof(), null values annoyingly report a type of "NULL"

    sql = "SELECT typeof(CAST(a AS " + castType + ")) FROM cp.`functions/null.json`";
    result = queryBuilder().sql(sql).singletonString();
    assertEquals(resultType, result);
  }

  private void doTypeOfTestSpecial(String expr, String value, String resultType) throws RpcException {
    String sql = "SELECT typeof(" + expr + ") FROM (VALUES (" + value + ")) AS T(a)";
    String result = queryBuilder().sql(sql).singletonString();
    assertEquals(resultType, result);
  }

  @Test
  public void testSqlTypeOf() throws RpcException {
    // SMALLINT not supported in CAST
    //doSqlTypeOfTest("SMALLINT");
    doSqlTypeOfTest("INTEGER");
    doSqlTypeOfTest("BIGINT");
    doSqlTypeOfTest("CHARACTER VARYING");
    doSqlTypeOfTest("FLOAT");
    doSqlTypeOfTest("DOUBLE");
    doSqlTypeOfTestSpecial("a", "true", "BOOLEAN");
    doSqlTypeOfTestSpecial("a", "CURRENT_DATE", "DATE");
    doSqlTypeOfTestSpecial("a", "CURRENT_TIME", "TIME");
    doSqlTypeOfTestSpecial("a", "CURRENT_TIMESTAMP", "TIMESTAMP");
    doSqlTypeOfTestSpecial("a", "AGE(CURRENT_TIMESTAMP)", "INTERVAL");
    doSqlTypeOfTestSpecial("BINARY_STRING(a)", "'\\xde\\xad\\xbe\\xef'", "BINARY VARYING");
    try {
      client.alterSession(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY, true);

      // These should include precision and scale: DECIMAL(p, s)
      // But, see DRILL-6378

      doSqlTypeOfTestSpecial("CAST(a AS DECIMAL)", "1", "DECIMAL(38, 0)");
      doSqlTypeOfTestSpecial("CAST(a AS DECIMAL(6, 3))", "1", "DECIMAL(6, 3)");
    } finally {
      client.resetSession(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY);
    }
  }

  private void doSqlTypeOfTest(String type) throws RpcException {

    // sqlTypeOf() returns SQL type names: the names used in CAST.

    String sql = "SELECT sqlTypeOf(CAST(a AS " + type + ")) FROM (VALUES (1)) AS T(a)";
    String result = queryBuilder().sql(sql).singletonString();
    assertEquals(type, result);

    // sqlTypeOf() returns SQL type names: the names used in CAST.

    sql = "SELECT sqlTypeOf(CAST(1 AS " + type + "))";
    result = queryBuilder().sql(sql).singletonString();
    assertEquals(type, result);

    // Returns same type even value is null.

    sql = "SELECT sqlTypeOf(CAST(a AS " + type + ")) FROM cp.`functions/null.json`";
    result = queryBuilder().sql(sql).singletonString();
    assertEquals(type, result);
  }

  private void doSqlTypeOfTestSpecial(String expr, String value, String resultType) throws RpcException {
    String sql = "SELECT sqlTypeof(" + expr + ") FROM (VALUES (" + value + ")) AS T(a)";
    String result = queryBuilder().sql(sql).singletonString();
    assertEquals(resultType, result);
  }

  @Test
  public void testDrillTypeOf() throws RpcException {
    // SMALLINT not supported in CAST
    //doDrillTypeOfTest("SMALLINT");
    doDrillTypeOfTest("INTEGER", "INT");
    doDrillTypeOfTest("BIGINT");
    doDrillTypeOfTest("CHARACTER VARYING", "VARCHAR");
    doDrillTypeOfTest("FLOAT", "FLOAT4");
    doDrillTypeOfTest("DOUBLE", "FLOAT8");

    // Omitting the other types. Internal code is identical to
    // typeof() except for null handling.
  }

  private void doDrillTypeOfTest(String type) throws RpcException {
    doDrillTypeOfTest(type, type);
  }

  private void doDrillTypeOfTest(String castType, String resultType) throws RpcException {

    // drillTypeOf() returns types using the internal names.

    String sql = "SELECT drillTypeOf(CAST(a AS " + castType + ")) FROM (VALUES (1)) AS T(a)";
    String result = queryBuilder().sql(sql).singletonString();
    assertEquals(resultType, result);

    sql = "SELECT drillTypeOf(CAST(1 AS " + castType + "))";
    result = queryBuilder().sql(sql).singletonString();
    assertEquals(resultType, result);

    // Returns same type even value is null.

    sql = "SELECT drillTypeOf(CAST(a AS " + castType + ")) FROM cp.`functions/null.json`";
    result = queryBuilder().sql(sql).singletonString();
    assertEquals(resultType, result);
  }

  @Test
  public void testModeOf() throws RpcException {

    // CSV files with headers use REQUIRED mode

    String sql = "SELECT modeOf(`name`) FROM cp.`store/text/data/cars.csvh`";
    String result = queryBuilder().sql(sql).singletonString();
    assertEquals("NOT NULL", result);

    // CSV files without headers use REPEATED mode

    sql = "SELECT modeOf(`columns`) FROM cp.`textinput/input2.csv`";
    result = queryBuilder().sql(sql).singletonString();
    assertEquals("ARRAY", result);

    // JSON files use OPTIONAL mode

    sql = "SELECT modeOf(`name`) FROM cp.`jsoninput/specialchar.json`";
    result = queryBuilder().sql(sql).singletonString();
    assertEquals("NULLABLE", result);
  }

  @Test
  public void testTypeOfLiteral() throws Exception {
    String sql =
        "SELECT typeOf(1) c1," +
              "typeOf('a') c2," +
              "typeOf(date '2018-01-22') c3," +
              "typeOf(time '01:00:20.123') c4," +
              "typeOf(timestamp '2018-01-22 01:00:20.123') c5," +
              "typeOf(false) c6," +
              "typeOf(12.3) c7," +
              "typeOf(1>2) c8," +
              "typeOf(cast(null as int)) c9";

    testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("c1", "c2", "c3", "c4", "c5", "c6", "c7", "c8", "c9")
        .baselineValues("INT", "VARCHAR", "DATE", "TIME", "TIMESTAMP", "BIT", "VARDECIMAL", "BIT", "INT")
        .go();
  }

  @Test
  public void testSqlTypeOfLiteral() throws Exception {
    String sql =
      "SELECT sqlTypeOf(1) c1," +
            "sqlTypeOf('a') c2," +
            "sqlTypeOf(date '2018-01-22') c3," +
            "sqlTypeOf(time '01:00:20.123') c4," +
            "sqlTypeOf(timestamp '2018-01-22 01:00:20.123') c5," +
            "sqlTypeOf(false) c6," +
            "sqlTypeOf(12.3) c7," +
            "sqlTypeOf(1>2) c8," +
            "sqlTypeOf(cast(null as int)) c9";

    testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("c1", "c2", "c3", "c4", "c5", "c6", "c7", "c8", "c9")
        .baselineValues("INTEGER", "CHARACTER VARYING", "DATE", "TIME",
            "TIMESTAMP", "BOOLEAN", "DECIMAL(3, 1)", "BOOLEAN", "INTEGER")
        .go();
  }

  @Test
  public void testDrillTypeOfLiteral() throws Exception {
    String sql =
        "SELECT drillTypeOf(1) c1," +
              "drillTypeOf('a') c2," +
              "drillTypeOf(date '2018-01-22') c3," +
              "drillTypeOf(time '01:00:20.123') c4," +
              "drillTypeOf(timestamp '2018-01-22 01:00:20.123') c5," +
              "drillTypeOf(false) c6," +
              "drillTypeOf(12.3) c7," +
              "drillTypeOf(1>2) c8," +
              "drillTypeOf(cast(null as int)) c9";

    testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("c1", "c2", "c3", "c4", "c5", "c6", "c7", "c8", "c9")
        .baselineValues("INT", "VARCHAR", "DATE", "TIME",
            "TIMESTAMP", "BIT", "VARDECIMAL", "BIT", "INT")
        .go();
  }

  @Test
  public void testModeOfLiteral() throws Exception {
    String sql =
        "SELECT modeOf(1) c1," +
              "modeOf('a') c2," +
              "modeOf(cast(null as int)) c3," +
              "modeOf(case when true then null else 'a' end) c4," +
              "modeOf(case when false then null else 'a' end) c5";

    testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("c1", "c2", "c3", "c4", "c5")
        .baselineValues("NOT NULL", "NOT NULL", "NULLABLE", "NULLABLE", "NULLABLE")
        .go();
  }

  @Test
  public void testCompareTypeLiteral() throws Exception {
    String sql =
        "SELECT compareType(1, 2) c1," +
              "compareType('a', 1) c2," +
              "compareType(1, 'a') c3," +
              "compareType(a, '01:00:20.123') c4," +
              "compareType(3, t.a) c5," +
              "compareType(t.a, 3) c6\n" +
        "from (values(1)) t(a)";

    testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("c1", "c2", "c3", "c4", "c5", "c6")
        .baselineValues(0, 1, -1, -1, 0, 0)
        .go();
  }

  @Test
  public void testTypeOfWithFile() throws Exception {
    // Column `x` does not actually appear in the file.
    String sql ="SELECT typeof(bi) AS bi_t, typeof(fl) AS fl_t, typeof(st) AS st_t,\n" +
                "       typeof(mp) AS mp_t, typeof(ar) AS ar_t, typeof(nu) AS nu_t,\n" +
                "       typeof(x) AS x_t\n" +
                "FROM cp.`jsoninput/allTypes.json`";
     testBuilder()
      .sqlQuery(sql)
      .ordered()
      .baselineColumns("bi_t",   "fl_t",   "st_t",    "mp_t", "ar_t",   "nu_t", "x_t")
      .baselineValues( "BIGINT", "FLOAT8", "VARCHAR", "MAP",  "BIGINT", "NULL", "NULL")
      .go();
  }

  @Test
  public void testUnionType() throws Exception {
    String sql ="SELECT typeof(a) AS t, modeof(a) AS m, drilltypeof(a) AS dt\n" +
                "FROM cp.`jsoninput/union/c.json`";
    try {
      testBuilder()
        .optionSettingQueriesForTestQuery("alter session set `exec.enable_union_type` = true")
        .sqlQuery(sql)
        .ordered()
        .baselineColumns("t",       "m",        "dt")
        .baselineValues( "VARCHAR", "NULLABLE", "UNION")
        .baselineValues( "BIGINT",  "NULLABLE", "UNION")
        .baselineValues( "FLOAT8",  "NULLABLE", "UNION")
        // The following should probably provide the type of the list,
        // and report cardinality as ARRAY.
        .baselineValues( "LIST",    "NULLABLE", "UNION")
        .baselineValues( "NULL",    "NULLABLE", "UNION")
        .go();
    }
    finally {
      client.resetSession(ExecConstants.ENABLE_UNION_TYPE_KEY);
    }
  }
}
