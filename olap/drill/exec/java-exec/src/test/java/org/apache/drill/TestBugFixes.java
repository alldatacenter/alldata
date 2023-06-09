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

import static org.junit.Assert.fail;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.test.BaseTestQuery;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(UnlikelyTest.class)
public class TestBugFixes extends BaseTestQuery {

  @BeforeClass
  public static void setupTestFiles() {
    dirTestWatcher.copyResourceToRoot(Paths.get("bugs", "DRILL-4192"));
  }

  @Test
  public void leak1() throws Exception {
    String select = "select count(*) \n" +
        "    from cp.`tpch/part.parquet` p1, cp.`tpch/part.parquet` p2 \n" +
        "    where p1.p_name = p2.p_name \n" +
        "  and p1.p_mfgr = p2.p_mfgr";
    test(select);
  }

  @Ignore
  @Test
  public void failingSmoke() throws Exception {
    String select = "select count(*) \n" +
        "  from (select l.l_orderkey as x, c.c_custkey as y \n" +
        "  from cp.`tpch/lineitem.parquet` l \n" +
        "    left outer join cp.`tpch/customer.parquet` c \n" +
        "      on l.l_orderkey = c.c_custkey) as foo\n" +
        "  where x < 10000";
    test(select);
  }

  @Test
  public void testSysDrillbits() throws Exception {
    test("select * from sys.drillbits");
  }

  @Test
  public void testVersionTable() throws Exception {
    test("select * from sys.version");
  }

  @Test
  public void DRILL883() throws Exception {
    test("select n1.n_regionkey from cp.`tpch/nation.parquet` n1, (select n_nationkey from cp.`tpch/nation.parquet`) as n2 where n1.n_nationkey = n2.n_nationkey");
  }

  @Test
  public void DRILL1061() throws Exception {
    String query = "select foo.mycol.x as COMPLEX_COL from (select convert_from('{ x : [1,2], y : 100 }', 'JSON') as mycol from cp.`tpch/nation.parquet`) as foo(mycol) limit 1";
    test(query);
  }

  @Test
  public void DRILL1126() throws Exception {
    try {
      test(String.format("alter session set `%s` = true", PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY));
      String query = "select sum(cast(employee_id as decimal(38, 18))), avg(cast(employee_id as decimal(38, 18))) from cp.`employee.json` group by (department_id)";
      test(query);
    } finally {
      test(String.format("alter session set `%s` = false", PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY));
    }
  }

  /**
   * This test is not checking results because the bug fixed only appears with functions taking no arguments.
   * I could alternatively use something like the now() function, but this still would be hard to write
   * result verification for. The important aspect of the test is that it verifies that the previous IOOB
   * does not occur. The various no-argument functions should be verified in other ways.
   */
  @Test
  public void Drill3484() throws Exception {
    try {
      test("alter SYSTEM set `drill.exec.functions.cast_empty_string_to_null` = true;");
      test("select random() from sys.drillbits");
    } finally {
      test("alter SYSTEM set `drill.exec.functions.cast_empty_string_to_null` = false;");
    }
  }

  @Test
  // Should be "Failure while parsing sql. Node [rel#26:Subset#6.LOGICAL.ANY([]).[]] could not be implemented;".
  // Drill will hit CanNotPlan, until we add code fix to transform the local LHS filter in left outer join properly.
  public void testDRILL1337_LocalLeftFilterLeftOutJoin() throws Exception {
    try {
      test("select count(*) from cp.`tpch/nation.parquet` n left outer join " +
           "cp.`tpch/region.parquet` r on n.n_regionkey = r.r_regionkey and n.n_nationkey > 10;");
      fail();
    } catch (UserException e) {
      // Expected;
    }
  }

  @Test
  public void testDRILL1337_LocalRightFilterLeftOutJoin() throws Exception {
    test("select * from cp.`tpch/nation.parquet` n left outer join " +
         "cp.`tpch/region.parquet` r on n.n_regionkey = r.r_regionkey and r.r_name not like '%ASIA' order by r.r_name;");
  }

  @Test
  public void testDRILL2361_AggColumnAliasWithDots() throws Exception {
    testBuilder()
      .sqlQuery("select count(*) as `test.alias` from cp.`employee.json`")
      .unOrdered()
      .baselineColumns("`test.alias`")
      .baselineValues(1155L)
      .build().run();
  }

  @Test
  public void testDRILL2361_SortColumnAliasWithDots() throws Exception {
    testBuilder()
            .sqlQuery("select o_custkey as `x.y.z` from cp.`tpch/orders.parquet` where o_orderkey < 5 order by `x.y.z`")
            .unOrdered()
            .baselineColumns("`x.y.z`")
            .baselineValues(370)
            .baselineValues(781)
            .baselineValues(1234)
            .baselineValues(1369)
            .build().run();
  }

  @Test
  public void testDRILL2361_JoinColumnAliasWithDots() throws Exception {
    testBuilder()
            .sqlQuery("select count(*) as cnt from (select o_custkey as `x.y` from cp.`tpch/orders.parquet`) o inner join cp.`tpch/customer.parquet` c on o.`x.y` = c.c_custkey")
            .unOrdered()
            .baselineColumns("cnt")
            .baselineValues(15000L)
            .build().run();
  }

  @Test
  public void testDRILL4192() throws Exception {
    testBuilder()
        .sqlQuery("select dir0, dir1 from dfs.`bugs/DRILL-4192` order by dir1")
        .unOrdered()
        .baselineColumns("dir0", "dir1")
        .baselineValues("single_top_partition", "nested_partition_1")
        .baselineValues("single_top_partition", "nested_partition_2")
        .go();

    testBuilder()
        .sqlQuery("select dir0, dir1 from dfs.`bugs/DRILL-4192/*/nested_partition_1` order by dir1")
        .unOrdered()
        .baselineColumns("dir0", "dir1")
        .baselineValues("single_top_partition", "nested_partition_1")
        .go();
  }

  @Test
  public void testDRILL4771() throws Exception {
    final String query = "select count(*) cnt, avg(distinct emp.department_id) avd\n"
        + " from cp.`employee.json` emp";
    final String[] expectedPlans = {
        ".*Agg\\(group=\\[\\{\\}\\], cnt=\\[\\$SUM0\\(\\$1\\)\\], agg#1=\\[\\$SUM0\\(\\$0\\)\\], agg#2=\\[COUNT\\(\\$0\\)\\]\\)",
        ".*Agg\\(group=\\[\\{0\\}\\], cnt=\\[COUNT\\(\\)\\]\\)"};
    final String[] excludedPlans = {".*Join\\(condition=\\[true\\], joinType=\\[inner\\]\\).*"};
    PlanTestBase.testPlanMatchingPatterns(query, expectedPlans, excludedPlans);
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("cnt", "avd")
        .baselineValues(1155L, 10.416666666666666)
        .build().run();

    final String query1 = "select emp.gender, count(*) cnt, avg(distinct emp.department_id) avd\n"
            + " from cp.`employee.json` emp\n"
            + " group by gender";
    final String[] expectedPlans1 = {
            ".*Agg\\(group=\\[\\{0\\}\\], cnt=\\[\\$SUM0\\(\\$2\\)\\], agg#1=\\[\\$SUM0\\(\\$1\\)\\], agg#2=\\[COUNT\\(\\$1\\)\\]\\)",
            ".*Agg\\(group=\\[\\{0, 1\\}\\], cnt=\\[COUNT\\(\\)\\]\\)"};
    final String[] excludedPlans1 = {".*Join\\(condition=\\[true\\], joinType=\\[inner\\]\\).*"};
    PlanTestBase.testPlanMatchingPatterns(query1, expectedPlans1, excludedPlans1);
    testBuilder()
            .sqlQuery(query1)
            .unOrdered()
            .baselineColumns("gender", "cnt", "avd")
            .baselineValues("F", 601L, 10.416666666666666)
            .baselineValues("M", 554L, 11.9)
            .build().run();
  }

  @Test
  public void testDRILL4884() throws Exception {
    int limit = 65536;
    ImmutableList.Builder<Map<String, Object>> baselineBuilder = ImmutableList.builder();
    for (int i = 0; i < limit; i++) {
      baselineBuilder.add(Collections.<String, Object>singletonMap("`id`", /*String.valueOf */ (i + 1)));
    }
    List<Map<String, Object>> baseline = baselineBuilder.build();

    testBuilder()
      .sqlQuery("select cast(id as int) as id from cp.`bugs/DRILL-4884/limit_test_parquet/test0_0_0.parquet` group by id order by 1 limit %s", limit)
      .unOrdered()
      .baselineRecords(baseline)
      .go();
  }

  @Test
  public void testDRILL5051() throws Exception {
    testBuilder()
        .sqlQuery("select count(1) as cnt from (select l_orderkey from (select l_orderkey from cp.`tpch/lineitem.parquet` limit 2) limit 1 offset 1)")
        .unOrdered()
        .baselineColumns("cnt")
        .baselineValues(1L)
        .go();
  }

  @Test // DRILL-4678
  public void testManyDateCasts() throws Exception {
    StringBuilder query = new StringBuilder("SELECT DISTINCT dt FROM (VALUES");
    for (int i = 0; i < 50; i++) {
      query.append("(CAST('1964-03-07' AS DATE)),");
    }
    query.append("(CAST('1951-05-16' AS DATE))) tbl(dt)");
    test(query.toString());
  }

  @Test // DRILL-4971
  public void testVisitBooleanOrWithoutFunctionsEvaluation() throws Exception {
    String query = "SELECT\n" +
        "CASE WHEN employee_id IN (1) THEN 1 ELSE 0 END `first`\n" +
        ", CASE WHEN employee_id IN (2) THEN 1 ELSE 0 END `second`\n" +
        ", CASE WHEN employee_id IN (1, 2) THEN 1 ELSE 0 END `any`\n" +
        "FROM cp.`employee.json` ORDER BY employee_id limit 2";

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("first", "second", "any")
        .baselineValues(1, 0, 1)
        .baselineValues(0, 1, 1)
        .go();
  }

  @Test // DRILL-4971
  public void testVisitBooleanAndWithoutFunctionsEvaluation() throws Exception {
    String query = "SELECT employee_id FROM cp.`employee.json` WHERE\n" +
        "((employee_id > 1 AND employee_id < 3) OR (employee_id > 9 AND employee_id < 11))\n" +
        "AND (employee_id > 1 AND employee_id < 3)";

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("employee_id")
        .baselineValues((long) 2)
        .go();
  }

  @Test
  public void testDRILL5269() throws Exception {
    try {
      test("ALTER SESSION SET `planner.enable_nljoin_for_scalar_only` = false");
      test("ALTER SESSION SET `planner.slice_target` = 500");
      test("\nSELECT `one` FROM (\n" +
          "  SELECT 1 `one` FROM cp.`tpch/nation.parquet`\n" +
          "  INNER JOIN (\n" +
          "    SELECT 2 `two` FROM cp.`tpch/nation.parquet`\n" +
          "  ) `t0` ON (\n" +
          "    `tpch/nation.parquet`.n_regionkey IS NOT DISTINCT FROM `t0`.`two`\n" +
          "  )\n" +
          "  GROUP BY `one`\n" +
          ") `t1`\n" +
          "  INNER JOIN (\n" +
          "    SELECT count(1) `a_count` FROM cp.`tpch/nation.parquet`\n" +
          ") `t5` ON TRUE\n");
    } finally {
      resetSessionOption("planner.enable_nljoin_for_scalar_only");
      resetSessionOption("planner.slice_target");
    }
  }

  @Test
  public void testDRILL6318() throws Exception {
    int rows = testSql("SELECT FLATTEN(data) AS d FROM cp.`jsoninput/bug6318.json`");
    Assert.assertEquals(11, rows);

    rows = testSql("SELECT FLATTEN(data) AS d FROM cp.`jsoninput/bug6318.json` LIMIT 3");
    Assert.assertEquals(3, rows);

    rows = testSql("SELECT FLATTEN(data) AS d FROM cp.`jsoninput/bug6318.json` LIMIT 3 OFFSET 5");
    Assert.assertEquals(3, rows);
  }

  @Test
  public void testDRILL6547() throws Exception {
    String str1 = StringUtils.repeat('a', Types.MAX_VARCHAR_LENGTH);
    String str2 = StringUtils.repeat('b', Types.MAX_VARCHAR_LENGTH * 2);
    testBuilder()
        .sqlQuery("select\n" +
            "concat(cast(null as varchar), EXPR$0) as c1\n" +
            "from (values('%1$s'), ('%2$s'))", str1, str2)
        .ordered()
        .baselineColumns("c1")
        .baselineValuesForSingleColumn(str1, str2)
        .build()
        .run();
  }
}
