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

import static org.apache.drill.test.TestBuilder.listOf;
import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.nio.file.Paths;

import org.apache.drill.categories.OperatorTest;
import org.apache.drill.categories.PlannerTest;
import org.apache.drill.categories.SqlFunctionTest;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.test.BaseTestQuery;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({SqlFunctionTest.class, OperatorTest.class, PlannerTest.class, UnlikelyTest.class})
public class TestExampleQueries extends BaseTestQuery {

  @BeforeClass
  public static void setupTestFiles() {
    dirTestWatcher.copyResourceToRoot(Paths.get("tpchmulti"));
    dirTestWatcher.copyResourceToRoot(Paths.get("store","text"));
  }

  @Test // see DRILL-2328
  @Category(UnlikelyTest.class)
  public void testConcatOnNull() throws Exception {
    try {
      test("use dfs.tmp");
      test("create view concatNull as (select * from cp.`customer.json` where customer_id < 5);");

      // Test Left Null
      testBuilder()
          .sqlQuery("select (mi || lname) as CONCATOperator, mi, lname, concat(mi, lname) as CONCAT from concatNull")
          .ordered()
          .baselineColumns("CONCATOperator", "mi", "lname", "CONCAT")
          .baselineValues("A.Nowmer", "A.", "Nowmer", "A.Nowmer")
          .baselineValues("I.Whelply", "I.", "Whelply", "I.Whelply")
          .baselineValues(null, null, "Derry", "Derry")
          .baselineValues("J.Spence", "J.", "Spence", "J.Spence")
          .build().run();

      // Test Right Null
      testBuilder()
          .sqlQuery("select (lname || mi) as CONCATOperator, lname, mi, concat(lname, mi) as CONCAT from concatNull")
          .ordered()
          .baselineColumns("CONCATOperator", "lname", "mi", "CONCAT")
          .baselineValues("NowmerA.", "Nowmer", "A.", "NowmerA.")
          .baselineValues("WhelplyI.", "Whelply", "I.", "WhelplyI.")
          .baselineValues(null, "Derry", null, "Derry")
          .baselineValues("SpenceJ.", "Spence", "J.", "SpenceJ.")
          .build().run();

      // Test Two Sides
      testBuilder()
          .sqlQuery("select (mi || mi) as CONCATOperator, mi, mi, concat(mi, mi) as CONCAT from concatNull")
          .ordered()
          .baselineColumns("CONCATOperator", "mi", "mi0", "CONCAT")
          .baselineValues("A.A.", "A.", "A.", "A.A.")
          .baselineValues("I.I.", "I.", "I.", "I.I.")
          .baselineValues(null, null, null, "")
          .baselineValues("J.J.", "J.", "J.", "J.J.")
          .build().run();

      testBuilder()
          .sqlQuery("select (cast(null as varchar(10)) || lname) as CONCATOperator, " +
              "cast(null as varchar(10)) as NullColumn, lname, concat(cast(null as varchar(10)), lname) as CONCAT from concatNull")
          .ordered()
          .baselineColumns("CONCATOperator", "NullColumn", "lname", "CONCAT")
          .baselineValues(null, null, "Nowmer", "Nowmer")
          .baselineValues(null, null, "Whelply", "Whelply")
          .baselineValues(null, null, "Derry", "Derry")
          .baselineValues(null, null, "Spence", "Spence")
          .build().run();
    } finally {
      test("drop view concatNull;");
    }
  }

  @Test // see DRILL-2054
  public void testConcatOperator() throws Exception {
    testBuilder()
        .sqlQuery("select n_nationkey || '+' || n_name || '=' as CONCAT, n_nationkey, '+' as PLUS, n_name from cp.`tpch/nation.parquet`")
        .ordered()
        .csvBaselineFile("testframework/testExampleQueries/testConcatOperator.tsv")
        .baselineTypes(TypeProtos.MinorType.VARCHAR, TypeProtos.MinorType.INT, TypeProtos.MinorType.VARCHAR, TypeProtos.MinorType.VARCHAR)
        .baselineColumns("CONCAT", "n_nationkey", "PLUS", "n_name")
        .build().run();

    testBuilder()
        .sqlQuery("select (n_nationkey || n_name) as CONCAT from cp.`tpch/nation.parquet`")
        .ordered()
        .csvBaselineFile("testframework/testExampleQueries/testConcatOperatorInputTypeCombination.tsv")
        .baselineTypes(TypeProtos.MinorType.VARCHAR)
        .baselineColumns("CONCAT")
        .build().run();


    testBuilder()
        .sqlQuery("select (n_nationkey || cast(n_name as varchar(30))) as CONCAT from cp.`tpch/nation.parquet`")
        .ordered()
        .csvBaselineFile("testframework/testExampleQueries/testConcatOperatorInputTypeCombination.tsv")
        .baselineTypes(TypeProtos.MinorType.VARCHAR)
        .baselineColumns("CONCAT")
        .build().run();

    testBuilder()
        .sqlQuery("select (cast(n_nationkey as varchar(30)) || n_name) as CONCAT from cp.`tpch/nation.parquet`")
        .ordered()
        .csvBaselineFile("testframework/testExampleQueries/testConcatOperatorInputTypeCombination.tsv")
        .baselineTypes(TypeProtos.MinorType.VARCHAR)
        .baselineColumns("CONCAT")
        .build().run();
  }

  @Test // see DRILL-985
  public void testViewFileName() throws Exception {
    test("use dfs.tmp");
    test("create view nation_view_testexamplequeries as select * from cp.`tpch/nation.parquet`;");
    test("select * from dfs.tmp.`nation_view_testexamplequeries.view.drill`");
    test("drop view nation_view_testexamplequeries");
  }

  @Test
  public void testTextInClasspathStorage() throws Exception {
    test("select * from cp.`store/text/classpath_storage_csv_test.csv`");
  }

  @Test
  public void testParquetComplex() throws Exception {
    test("select recipe from cp.`parquet/complex.parquet`");
    test("select * from cp.`parquet/complex.parquet`");
    test("select recipe, c.inventor.name as name, c.inventor.age as age from cp.`parquet/complex.parquet` c");
  }

  @Test // see DRILL-553
  public void testQueryWithNullValues() throws Exception {
    test("select count(*) from cp.`customer.json` limit 1");
    test("select count(*) from cp.`customer.json` limit 1");
    test("select count(*) from cp.`customer.json` limit 1");
    test("select count(*) from cp.`customer.json` limit 1");
    test("select count(*) from cp.`customer.json` limit 1");
    test("select count(*) from cp.`customer.json` limit 1");
    test("select count(*) from cp.`customer.json` limit 1");
    test("select count(*) from cp.`customer.json` limit 1");
    test("select count(*) from cp.`customer.json` limit 1");
    test("select count(*) from cp.`customer.json` limit 1");
    test("select count(*) from cp.`customer.json` limit 1");
  }

  @Test
  public void testJoinMerge() throws Exception {
    alterSession(PlannerSettings.ENABLE_HASH_JOIN_OPTION, false);
    try {
      test("select count(*) \n" +
          "  from (select l.l_orderkey as x, c.c_custkey as y \n" +
          "  from cp.`tpch/lineitem.parquet` l \n" +
          "    left outer join cp.`tpch/customer.parquet` c \n" +
          "      on l.l_orderkey = c.c_custkey) as foo\n" +
          "  where x < 10000\n" +
          "");
    } finally {
      resetSessionOption(PlannerSettings.ENABLE_HASH_JOIN_OPTION);
    }
  }

  @Test
  public void testJoinExpOn() throws Exception {
    test("select a.n_nationkey from cp.`tpch/nation.parquet` a join cp.`tpch/region.parquet` b on a.n_regionkey + 1 = b.r_regionkey and a.n_regionkey + 1 = b.r_regionkey;");
  }

  @Test
  public void testJoinExpWhere() throws Exception {
    test("select a.n_nationkey from cp.`tpch/nation.parquet` a , cp.`tpch/region.parquet` b where a.n_regionkey + 1 = b.r_regionkey and a.n_regionkey + 1 = b.r_regionkey;");
  }

  @Test
  public void testPushExpInJoinConditionInnerJoin() throws Exception {
    test("select a.n_nationkey from cp.`tpch/nation.parquet` a join cp.`tpch/region.parquet` b " + "" +
      " on a.n_regionkey + 100  = b.r_regionkey + 200" +      // expressions in both sides of equal join filter
      "   and (substr(a.n_name,1,3)= 'L1' or substr(a.n_name,2,2) = 'L2') " +  // left filter
      "   and (substr(b.r_name,1,3)= 'R1' or substr(b.r_name,2,2) = 'R2') " +  // right filter
      "   and (substr(a.n_name,2,3)= 'L3' or substr(b.r_name,3,2) = 'R3');");  // non-equal join filter
  }

  @Test
  public void testPushExpInJoinConditionWhere() throws Exception {
    test("select a.n_nationkey from cp.`tpch/nation.parquet` a , cp.`tpch/region.parquet` b " + "" +
        " where a.n_regionkey + 100  = b.r_regionkey + 200" +      // expressions in both sides of equal join filter
        "   and (substr(a.n_name,1,3)= 'L1' or substr(a.n_name,2,2) = 'L2') " +  // left filter
        "   and (substr(b.r_name,1,3)= 'R1' or substr(b.r_name,2,2) = 'R2') " +  // right filter
        "   and (substr(a.n_name,2,3)= 'L3' or substr(b.r_name,3,2) = 'R3');");  // non-equal join filter
  }

  @Test
  public void testPushExpInJoinConditionLeftJoin() throws Exception {
    test("select a.n_nationkey, b.r_regionkey from cp.`tpch/nation.parquet` a left join cp.`tpch/region.parquet` b " +
        " on a.n_regionkey +100 = b.r_regionkey +200 " +        // expressions in both sides of equal join filter // left filter
        " and (substr(b.r_name,1,3)= 'R1' or substr(b.r_name,2,2) = 'R2') ");   // right filter // non-equal join filter
  }

  @Test
  public void testPushExpInJoinConditionRightJoin() throws Exception {
    test("select a.n_nationkey, b.r_regionkey from cp.`tpch/nation.parquet` a right join cp.`tpch/region.parquet` b " + "" +
        " on a.n_regionkey +100 = b.r_regionkey +200 " +        // expressions in both sides of equal join filter
        "   and (substr(a.n_name,1,3)= 'L1' or substr(a.n_name,2,2) = 'L2') ");  // left filter
  }

  @Test
  @Category(UnlikelyTest.class)
  public void testCaseReturnValueVarChar() throws Exception {
    test("select case when employee_id < 1000 then 'ABC' else 'DEF' end from cp.`employee.json` limit 5");
  }

  @Test
  @Category(UnlikelyTest.class)
  public void testCaseReturnValueBigInt() throws Exception {
    test("select case when employee_id < 1000 then 1000 else 2000 end from cp.`employee.json` limit 5");
  }

  @Test
  public void testHashPartitionSV2() throws Exception {
    test("select count(n_nationkey) from cp.`tpch/nation.parquet` where n_nationkey > 8 group by n_regionkey");
  }

  @Test
  public void testHashPartitionSV4() throws Exception {
    test("select count(n_nationkey) as cnt from cp.`tpch/nation.parquet` group by n_regionkey order by cnt");
  }

  @Test
  public void testSelectWithLimit() throws Exception {
    test("select employee_id,  first_name, last_name from cp.`employee.json` limit 5 ");
  }

  @Test
  public void testSelectWithLimit2() throws Exception {
    test("select l_comment, l_orderkey from cp.`tpch/lineitem.parquet` limit 10000 ");
  }

  @Test
  public void testSVRV4() throws Exception {
    test("select employee_id,  first_name from cp.`employee.json` order by employee_id ");
  }

  @Test
  public void testSVRV4MultBatch() throws Exception {
    test("select l_orderkey from cp.`tpch/lineitem.parquet` order by l_orderkey limit 10000 ");
  }

  @Test
  public void testSVRV4Join() throws Exception {
    test("select count(*) from cp.`tpch/lineitem.parquet` l, cp.`tpch/partsupp.parquet` ps \n" +
        " where l.l_partkey = ps.ps_partkey and l.l_suppkey = ps.ps_suppkey ;");
  }

  @Test
  public void testText() throws Exception {
    test("select * from cp.`store/text/data/regions.csv`");
  }

  @Test
  public void testFilterOnArrayTypes() throws Exception {
    test("select columns[0] from cp.`store/text/data/regions.csv` " +
      " where cast(columns[0] as int) > 1 and cast(columns[1] as varchar(20))='ASIA'");
  }

  @Test
  @Ignore("DRILL-3774")
  public void testTextPartitions() throws Exception {
    test("select * from cp.`store/text/data/`");
  }

  @Test
  @Ignore("DRILL-3004")
  public void testJoin() throws Exception {
    try {
      alterSession(PlannerSettings.ENABLE_HASH_JOIN_OPTION, false);
      test("SELECT\n" +
          "  nations.N_NAME,\n" +
          "  regions.R_NAME\n" +
          "FROM\n" +
          "  cp.`tpch/nation.parquet` nations\n" +
          "JOIN\n" +
          "  cp.`tpch/region.parquet` regions\n" +
          "  on nations.N_REGIONKEY = regions.R_REGIONKEY where 1 = 0");
    } finally {
      resetSessionOption(PlannerSettings.ENABLE_HASH_JOIN_OPTION);
    }
  }


  @Test
  public void testWhere() throws Exception {
    test("select * from cp.`employee.json` ");
  }

  @Test
  public void testGroupBy() throws Exception {
    test("select marital_status, COUNT(1) as cnt from cp.`employee.json` group by marital_status");
  }

  @Test
  public void testExplainPhysical() throws Exception {
    test("explain plan for select marital_status, COUNT(1) as cnt from cp.`employee.json` group by marital_status");
  }

  @Test
  public void testExplainLogical() throws Exception {
    test("explain plan without implementation for select marital_status, COUNT(1) as cnt from cp.`employee.json` group by marital_status");
  }

  @Test
  public void testGroupScanRowCountExp1() throws Exception {
    test("EXPLAIN plan for select count(n_nationkey) as mycnt, count(*) + 2 * count(*) as mycnt2 from cp.`tpch/nation.parquet` ");
  }

  @Test
  public void testGroupScanRowCount1() throws Exception {
    test("select count(n_nationkey) as mycnt, count(*) + 2 * count(*) as mycnt2 from cp.`tpch/nation.parquet` ");
  }

  @Test
  public void testColunValueCnt() throws Exception {
    test("select count( 1 + 2) from cp.`tpch/nation.parquet` ");
  }

  @Test
  public void testGroupScanRowCountExp2() throws Exception {
    test("EXPLAIN plan for select count(*) as mycnt, count(*) + 2 * count(*) as mycnt2 from cp.`tpch/nation.parquet` ");
  }

  @Test
  public void testGroupScanRowCount2() throws Exception {
    test("select count(*) as mycnt, count(*) + 2 * count(*) as mycnt2 from cp.`tpch/nation.parquet` where 1 < 2");
  }

  @Test
  @Category(UnlikelyTest.class)
  // cast non-exist column from json file. Should return null value.
  public void testDrill428() throws Exception {
    test("select cast(NON_EXIST_COL as varchar(10)) from cp.`employee.json` limit 2; ");
  }

  @Test  // Bugs DRILL-727, DRILL-940
  public void testOrderByDiffColumn() throws Exception {
    test("select r_name from cp.`tpch/region.parquet` order by r_regionkey");
    test("select r_name from cp.`tpch/region.parquet` order by r_name, r_regionkey");
    test("select cast(r_name as varchar(20)) from cp.`tpch/region.parquet` order by r_name");
  }

  @Test  // tests with LIMIT 0
  @Ignore("DRILL-1866")
  public void testLimit0_1() throws Exception {
    test("select n_nationkey, n_name from cp.`tpch/nation.parquet` limit 0");
    test("select n_nationkey, n_name from cp.`tpch/nation.parquet` limit 0 offset 5");
    test("select n_nationkey, n_name from cp.`tpch/nation.parquet` order by n_nationkey limit 0");
    test("select * from cp.`tpch/nation.parquet` limit 0");
    test("select n.n_nationkey from cp.`tpch/nation.parquet` n, cp.`tpch/region.parquet` r where n.n_regionkey = r.r_regionkey limit 0");
    test("select n_regionkey, count(*) from cp.`tpch/nation.parquet` group by n_regionkey limit 0");
  }

  @Test
  public void testTextJoin() throws Exception {
    test("select t1.columns[1] from cp.`store/text/data/nations.csv` t1, cp.`store/text/data/regions.csv` t2 where t1.columns[0] = t2.columns[0]");
  }

  @Test // DRILL-811
  public void testDRILL_811View() throws Exception {
    test("use dfs.tmp");
    test("create view nation_view_testexamplequeries as select * from cp.`tpch/nation.parquet`;");
    test("select n.n_nationkey, n.n_name, n.n_regionkey from nation_view_testexamplequeries n where n.n_nationkey > 8 order by n.n_regionkey");
    test("select n.n_regionkey, count(*) as cnt from nation_view_testexamplequeries n where n.n_nationkey > 8 group by n.n_regionkey order by n.n_regionkey");
    test("drop view nation_view_testexamplequeries ");
  }

  @Test  // DRILL-811
  public void testDRILL_811ViewJoin() throws Exception {
    test("use dfs.tmp");
    test("create view nation_view_testexamplequeries as select * from cp.`tpch/nation.parquet`;");
    test("create view region_view_testexamplequeries as select * from cp.`tpch/region.parquet`;");
    test("select n.n_nationkey, n.n_regionkey, r.r_name from region_view_testexamplequeries r , nation_view_testexamplequeries n where r.r_regionkey = n.n_regionkey ");
    test("select n.n_regionkey, count(*) as cnt from region_view_testexamplequeries r , nation_view_testexamplequeries n where r.r_regionkey = n.n_regionkey and n.n_nationkey > 8 group by n.n_regionkey order by n.n_regionkey");
    test("select n.n_regionkey, count(*) as cnt from region_view_testexamplequeries r join nation_view_testexamplequeries n on r.r_regionkey = n.n_regionkey and n.n_nationkey > 8 group by n.n_regionkey order by n.n_regionkey");
    test("drop view region_view_testexamplequeries ");
    test("drop view nation_view_testexamplequeries ");
  }

  @Test  // DRILL-811
  public void testDRILL_811Json() throws Exception {
    test("use dfs.tmp");
    test("create view region_view_testexamplequeries as select * from cp.`region.json`;");
    test("select sales_city, sales_region from region_view_testexamplequeries where region_id > 50 order by sales_country; ");
    test("drop view region_view_testexamplequeries ");
  }

  @Test
  @Category(UnlikelyTest.class)
  public void testCase() throws Exception {
    test("select case when n_nationkey > 0 and n_nationkey < 2 then concat(n_name, '_abc') when n_nationkey >=2 and n_nationkey < 4 then '_EFG' else concat(n_name,'_XYZ') end, n_comment from cp.`tpch/nation.parquet` ;");
  }

  @Test // tests join condition that has different input types
  public void testJoinCondWithDifferentTypes() throws Exception {
    test("select t1.department_description from cp.`department.json` t1, cp.`employee.json` t2 where (cast(t1.department_id as double)) = t2.department_id");
    test("select t1.full_name from cp.`employee.json` t1, cp.`department.json` t2 where cast(t1.department_id as double) = t2.department_id and cast(t1.position_id as bigint) = t2.department_id");

    // See DRILL-3995. Re-enable this once fixed.
    // test("select t1.full_name from cp.`employee.json` t1, cp.`department.json` t2 where t1.department_id = t2.department_id and t1.position_id = t2.department_id");
  }

  @Test
  public void testTopNWithSV2() throws Exception {
    int actualRecordCount = testSql("select N_NATIONKEY from cp.`tpch/nation.parquet` where N_NATIONKEY < 10 order by N_NATIONKEY limit 5");
    int expectedRecordCount = 5;
    assertEquals(String.format("Received unexpected number of rows in output: expected=%d, received=%s",
        expectedRecordCount, actualRecordCount), expectedRecordCount, actualRecordCount);
  }

  @Test
  public void testTextQueries() throws Exception {
    test("select cast('285572516' as int) from cp.`tpch/nation.parquet` limit 1");
  }

  @Test // DRILL-1544
  @Category(UnlikelyTest.class)
  public void testLikeEscape() throws Exception {
    int actualRecordCount = testSql("select id, name from cp.`jsoninput/specialchar.json` where name like '%#_%' ESCAPE '#'");
    int expectedRecordCount = 1;
    assertEquals(String.format("Received unexpected number of rows in output: expected=%d, received=%s",
        expectedRecordCount, actualRecordCount), expectedRecordCount, actualRecordCount);

  }

  @Test
  @Category(UnlikelyTest.class)
  public void testSimilarEscape() throws Exception {
    int actualRecordCount = testSql("select id, name from cp.`jsoninput/specialchar.json` where name similar to '(N|S)%#_%' ESCAPE '#'");
    int expectedRecordCount = 1;
    assertEquals(String.format("Received unexpected number of rows in output: expected=%d, received=%s",
        expectedRecordCount, actualRecordCount), expectedRecordCount, actualRecordCount);
  }

  @Test
  @Category(UnlikelyTest.class)
  public void testImplicitDownwardCast() throws Exception {
    int actualRecordCount = testSql("select o_totalprice from cp.`tpch/orders.parquet` where o_orderkey=60000 and o_totalprice=299402");
    int expectedRecordCount = 0;
    assertEquals(String.format("Received unexpected number of rows in output: expected=%d, received=%s",
        expectedRecordCount, actualRecordCount), expectedRecordCount, actualRecordCount);
  }

  @Test // DRILL-1470
  @Category(UnlikelyTest.class)
  public void testCastToVarcharWithLength() throws Exception {
    // cast from varchar with unknown length to a fixed length.
    int actualRecordCount = testSql("select first_name from cp.`employee.json` where cast(first_name as varchar(2)) = 'Sh'");
    int expectedRecordCount = 27;
    assertEquals(String.format("Received unexpected number of rows in output: expected=%d, received=%s",
        expectedRecordCount, actualRecordCount), expectedRecordCount, actualRecordCount);

    // cast from varchar with unknown length to varchar(5), then to varchar(10), then to varchar(2). Should produce the same result as the first query.
    actualRecordCount = testSql("select first_name from cp.`employee.json` where cast(cast(cast(first_name as varchar(5)) as varchar(10)) as varchar(2)) = 'Sh'");
    expectedRecordCount = 27;
    assertEquals(String.format("Received unexpected number of rows in output: expected=%d, received=%s",
        expectedRecordCount, actualRecordCount), expectedRecordCount, actualRecordCount);


    // this long nested cast expression should be essentially equal to substr(), meaning the query should return every row in the table.
    actualRecordCount = testSql("select first_name from cp.`employee.json` where cast(cast(cast(first_name as varchar(5)) as varchar(10)) as varchar(2)) = substr(first_name, 1, 2)");
    expectedRecordCount = 1155;
    assertEquals(String.format("Received unexpected number of rows in output: expected=%d, received=%s",
        expectedRecordCount, actualRecordCount), expectedRecordCount, actualRecordCount);

    // cast is applied to a column from parquet file.
    actualRecordCount = testSql("select n_name from cp.`tpch/nation.parquet` where cast(n_name as varchar(2)) = 'UN'");
    expectedRecordCount = 2;
    assertEquals(String.format("Received unexpected number of rows in output: expected=%d, received=%s",
        expectedRecordCount, actualRecordCount), expectedRecordCount, actualRecordCount);
  }

  @Test // DRILL-1488
  @Category(UnlikelyTest.class)
  public void testIdentifierMaxLength() throws Exception {
    // use long column alias name (approx 160 chars)
    test("select employee_id as  aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa from cp.`employee.json` limit 1");

    // use long table alias name  (approx 160 chars)
    test("select employee_id from cp.`employee.json` as aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa limit 1");

  }

  @Test // DRILL-1846  (this tests issue with SimpleMergeExchange)
  public void testOrderByDiffColumnsInSubqAndOuter() throws Exception {
    String query = "select n.n_nationkey from  (select n_nationkey, n_regionkey from cp.`tpch/nation.parquet` order by n_regionkey) n  order by n.n_nationkey";
    // set slice_target = 1 to force exchanges
    try {
      alterSession(ExecConstants.SLICE_TARGET, 1);
      test(query);
    } finally {
      resetSessionOption(ExecConstants.SLICE_TARGET);
    }
  }

  @Test // DRILL-1846  (this tests issue with UnionExchange)
  @Ignore("DRILL-1866")
  public void testLimitInSubqAndOrderByOuter() throws Exception {
    String query = "select t2.n_nationkey from (select n_nationkey, n_regionkey from cp.`tpch/nation.parquet` t1 group by n_nationkey, n_regionkey limit 10) t2 order by t2.n_nationkey";
    // set slice_target = 1 to force exchanges
    try {
      alterSession(ExecConstants.SLICE_TARGET, 1);
      test(query);
    } finally {
      resetSessionOption(ExecConstants.SLICE_TARGET);
    }
  }

  @Test // DRILL-1788
  @Category(UnlikelyTest.class)
  public void testCaseInsensitiveJoin() throws Exception {
    test("select n3.n_name from (select n2.n_name from cp.`tpch/nation.parquet` n1, cp.`tpch/nation.parquet` n2 where n1.N_name = n2.n_name) n3 " +
        " join cp.`tpch/nation.parquet` n4 on n3.n_name = n4.n_name");
  }

  @Test // DRILL-1561
  public void test2PhaseAggAfterOrderBy() throws Exception {
    String query = "select count(*) from (select o_custkey from cp.`tpch/orders.parquet` order by o_custkey)";
    // set slice_target = 1 to force exchanges and 2-phase aggregation
    try {
      alterSession(ExecConstants.SLICE_TARGET, 1);
      test(query);
    } finally {
      resetSessionOption(ExecConstants.SLICE_TARGET);
    }
  }

  @Test // DRILL-1867
  public void testCaseInsensitiveSubQuery() throws Exception {
    int actualRecordCount = 0, expectedRecordCount = 0;

    // source is JSON
    actualRecordCount = testSql("select EMPID from ( select employee_id as empid from cp.`employee.json` limit 2)");
    expectedRecordCount = 2;
    assertEquals(String.format("Received unexpected number of rows in output: expected=%d, received=%s",
        expectedRecordCount, actualRecordCount), expectedRecordCount, actualRecordCount);

    actualRecordCount = testSql("select EMPLOYEE_ID from ( select employee_id from cp.`employee.json` where Employee_id is not null limit 2)");
    expectedRecordCount = 2;
    assertEquals(String.format("Received unexpected number of rows in output: expected=%d, received=%s",
        expectedRecordCount, actualRecordCount), expectedRecordCount, actualRecordCount);

    actualRecordCount = testSql("select x.EMPLOYEE_ID from ( select employee_id from cp.`employee.json` limit 2) X");
    expectedRecordCount = 2;
    assertEquals(String.format("Received unexpected number of rows in output: expected=%d, received=%s",
        expectedRecordCount, actualRecordCount), expectedRecordCount, actualRecordCount);

    // source is PARQUET
    actualRecordCount = testSql("select NID from ( select n_nationkey as nid from cp.`tpch/nation.parquet`) where NID = 3");
    expectedRecordCount = 1;
    assertEquals(String.format("Received unexpected number of rows in output: expected=%d, received=%s",
        expectedRecordCount, actualRecordCount), expectedRecordCount, actualRecordCount);

    actualRecordCount = testSql("select x.N_nationkey from ( select n_nationkey from cp.`tpch/nation.parquet`) X where N_NATIONKEY = 3");
    expectedRecordCount = 1;
    assertEquals(String.format("Received unexpected number of rows in output: expected=%d, received=%s",
        expectedRecordCount, actualRecordCount), expectedRecordCount, actualRecordCount);

    // source is CSV
    String root = "store/text/data/regions.csv";
    String query = String.format("select rid, x.name from (select columns[0] as RID, columns[1] as NAME from cp.`%s`) X where X.rid = 2", root);
    actualRecordCount = testSql(query);
    expectedRecordCount = 1;
    assertEquals(String.format("Received unexpected number of rows in output: expected=%d, received=%s",
        expectedRecordCount, actualRecordCount), expectedRecordCount, actualRecordCount);

  }

  @Test
  public void testMultipleCountDistinctWithGroupBy() throws Exception {
    String query = "select n_regionkey, count(distinct n_nationkey), count(distinct n_name) from cp.`tpch/nation.parquet` group by n_regionkey;";

    // hash agg and streaming agg with default slice target (single phase aggregate)
    // hash agg and streaming agg with lower slice target (multiphase aggregate)
    try {
      alterSession(PlannerSettings.ENABLE_HASH_AGG_OPTION, true);
      alterSession(PlannerSettings.ENABLE_STREAM_AGG_OPTION, false);
      test(query);
      alterSession(ExecConstants.SLICE_TARGET, 1);
      test(query);
      resetSessionOption(ExecConstants.SLICE_TARGET);

      alterSession(PlannerSettings.ENABLE_HASH_AGG_OPTION, false);
      alterSession(PlannerSettings.ENABLE_STREAM_AGG_OPTION, true);
      test(query);
      alterSession(ExecConstants.SLICE_TARGET, 1);
      test(query);
    } finally {
      resetSessionOption(PlannerSettings.ENABLE_HASH_AGG_OPTION);
      resetSessionOption(PlannerSettings.ENABLE_STREAM_AGG_OPTION);
      resetSessionOption(ExecConstants.SLICE_TARGET);
    }
  }

  @Test // DRILL-2019
  public void testFilterInSubqueryAndOutside() throws Exception {
    String query1 = "select r_regionkey from (select r_regionkey from cp.`tpch/region.parquet` o where r_regionkey < 2) where r_regionkey > 2";
    String query2 = "select r_regionkey from (select r_regionkey from cp.`tpch/region.parquet` o where r_regionkey < 4) where r_regionkey > 1";
    int actualRecordCount = 0;
    int expectedRecordCount = 0;

    actualRecordCount = testSql(query1);
    assertEquals(expectedRecordCount, actualRecordCount);

    expectedRecordCount = 2;
    actualRecordCount = testSql(query2);
    assertEquals(expectedRecordCount, actualRecordCount);
  }

  @Test // DRILL-1973
  public void testLimit0SubqueryWithFilter() throws Exception {
    String query1 = "select * from (select sum(1) as x from  cp.`tpch/region.parquet` limit 0) WHERE x < 10";
    String query2 = "select * from (select sum(1) as x from  cp.`tpch/region.parquet` limit 0) WHERE (0 = 1)";
    int actualRecordCount = 0;
    int expectedRecordCount = 0;

    actualRecordCount = testSql(query1);
    assertEquals(expectedRecordCount, actualRecordCount);

    actualRecordCount = testSql(query2);
    assertEquals(expectedRecordCount, actualRecordCount);
  }

  @Test // DRILL-2063
  public void testAggExpressionWithGroupBy() throws Exception {
    String query = "select l_suppkey, sum(l_extendedprice)/sum(l_quantity) as avg_price \n" +
        " from cp.`tpch/lineitem.parquet` where l_orderkey in \n" +
        " (select o_orderkey from cp.`tpch/orders.parquet` where o_custkey = 2) \n" +
        " and l_suppkey = 4 group by l_suppkey";

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("l_suppkey", "avg_price")
        .baselineValues(4, 1374.47)
        .build().run();

  }

  @Test // DRILL-1888
  public void testAggExpressionWithGroupByHaving() throws Exception {
    String query = "select l_suppkey, sum(l_extendedprice)/sum(l_quantity) as avg_price \n" +
        " from cp.`tpch/lineitem.parquet` where l_orderkey in \n" +
        " (select o_orderkey from cp.`tpch/orders.parquet` where o_custkey = 2) \n" +
        " group by l_suppkey having sum(l_extendedprice)/sum(l_quantity) > 1850.0";

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("l_suppkey", "avg_price")
        .baselineValues(98, 1854.95)
        .build().run();
  }

  @Test
  public void testExchangeRemoveForJoinPlan() throws Exception {
    String sql = "select t2.n_nationkey from dfs.`tpchmulti/region` t1 join dfs.`tpchmulti/nation` t2 on t2.n_regionkey = t1.r_regionkey";

    testBuilder()
        .unOrdered()
        .optionSettingQueriesForTestQuery("alter session set `planner.slice_target` = 10; alter session set `planner.join.row_count_estimate_factor` = 0.1")  // Enforce exchange will be inserted.
        .sqlQuery(sql)
        .optionSettingQueriesForBaseline("alter session set `planner.slice_target` = 100000; alter session set `planner.join.row_count_estimate_factor` = 1.0") // Use default option setting.
        .sqlBaselineQuery(sql)
        .build()
        .run();
  }

  @Test //DRILL-2163
  public void testNestedTypesPastJoinReportsValidResult() throws Exception {
    testBuilder()
        .sqlQuery("select t1.uid, t1.events, t1.events[0].evnt_id as event_id, t2.transactions, " +
          "t2.transactions[0] as trans, t1.odd, t2.even from cp.`project/complex/a.json` t1, " +
          "cp.`project/complex/b.json` t2 where t1.uid = t2.uid")
        .ordered()
        .jsonBaselineFile("project/complex/drill-2163-result.json")
        .build()
        .run();
  }

  @Test
  @Category(UnlikelyTest.class)
  public void testSimilar() throws Exception {
    testBuilder()
        .sqlQuery("select n_nationkey " +
          "from cp.`tpch/nation.parquet` " +
          "where n_name similar to 'CHINA' " +
          "order by n_regionkey")
        .unOrdered()
        .optionSettingQueriesForTestQuery("alter session set `planner.slice_target` = 1")
        .baselineColumns("n_nationkey")
        .baselineValues(18)
        .go();

    test("alter session set `planner.slice_target` = " + ExecConstants.SLICE_TARGET_DEFAULT);
  }

  @Test // DRILL-2311
  @Ignore("Move to TestParquetWriter. Have to ensure same file name does not exist on filesystem.")
  public void testCreateTableSameColumnNames() throws Exception {
    String creatTable = "CREATE TABLE CaseInsensitiveColumnNames as " +
        "select cast(r_regionkey as BIGINT) BIGINT_col, cast(r_regionkey as DECIMAL) bigint_col \n" +
        "FROM cp.`tpch/region.parquet`;\n";

    test("USE dfs.tmp");
    test(creatTable);

    testBuilder()
        .sqlQuery("select * from `CaseInsensitiveColumnNames`")
        .unOrdered()
        .baselineColumns("BIGINT_col", "bigint_col0\n")
        .baselineValues((long) 0, new BigDecimal(0))
        .baselineValues((long) 1, new BigDecimal(1))
        .baselineValues((long) 2, new BigDecimal(2))
        .baselineValues((long) 3, new BigDecimal(3))
        .baselineValues((long) 4, new BigDecimal(4))
        .build().run();
  }

  @Test // DRILL-1943, DRILL-1911
  @Category(UnlikelyTest.class)
  public void testColumnNamesDifferInCaseOnly() throws Exception {
    testBuilder()
        .sqlQuery("select r_regionkey a, r_regionkey A FROM cp.`tpch/region.parquet`")
        .unOrdered()
        .baselineColumns("a", "A0")
        .baselineValues(0, 0)
        .baselineValues(1, 1)
        .baselineValues(2, 2)
        .baselineValues(3, 3)
        .baselineValues(4, 4)
        .build().run();

    testBuilder()
        .sqlQuery("select employee_id, Employee_id from cp.`employee.json` limit 2")
        .unOrdered()
        .baselineColumns("employee_id", "Employee_id0")
        .baselineValues((long) 1, (long) 1)
        .baselineValues((long) 2, (long) 2)
        .build().run();
  }

  @Test // DRILL-2094
  public void testOrderbyArrayElement() throws Exception {
    testBuilder()
        .sqlQuery("select t.id, t.list[0] as SortingElem " +
          "from cp.`store/json/orderByArrayElement.json` t " +
          "order by t.list[0]")
        .ordered()
        .baselineColumns("id", "SortingElem")
        .baselineValues((long) 1, (long) 1)
        .baselineValues((long) 5, (long) 2)
        .baselineValues((long) 4, (long) 3)
        .baselineValues((long) 2, (long) 5)
        .baselineValues((long) 3, (long) 6)
        .build().run();
  }

  @Test // DRILL-2479
  public void testCorrelatedExistsWithInSubq() throws Exception {
    testBuilder()
        .sqlQuery("select count(*) as cnt from cp.`tpch/lineitem.parquet` l where exists "
          + " (select ps.ps_suppkey from cp.`tpch/partsupp.parquet` ps where ps.ps_suppkey = l.l_suppkey and ps.ps_partkey "
          + " in (select p.p_partkey from cp.`tpch/part.parquet` p where p.p_type like '%NICKEL'))")
        .unOrdered()
        .baselineColumns("cnt")
        .baselineValues(60175l)
        .go();
  }

  @Test // DRILL-2094
  public void testOrderbyArrayElementInSubquery() throws Exception {
    testBuilder()
        .sqlQuery("select s.id from " +
          "(select id " +
          "from cp.`store/json/orderByArrayElement.json` " +
          "order by list[0]) s")
        .ordered()
        .baselineColumns("id")
        .baselineValues((long) 1)
        .baselineValues((long) 5)
        .baselineValues((long) 4)
        .baselineValues((long) 2)
        .baselineValues((long) 3)
        .build().run();
  }

  @Test // DRILL-1978
  public void testCTASOrderByCoumnNotInSelectClause() throws Exception {
    String queryCTAS1 = "CREATE TABLE TestExampleQueries_testCTASOrderByCoumnNotInSelectClause1 as " +
        "select r_name from cp.`tpch/region.parquet` order by r_regionkey;";

    String queryCTAS2 = "CREATE TABLE TestExampleQueries_testCTASOrderByCoumnNotInSelectClause2 as " +
        "SELECT columns[1] as col FROM cp.`store/text/data/regions.csv` ORDER BY cast(columns[0] as double)";

    String query1 = "select * from TestExampleQueries_testCTASOrderByCoumnNotInSelectClause1";
    String query2 = "select * from TestExampleQueries_testCTASOrderByCoumnNotInSelectClause2";

    test("use dfs.tmp");
    test(queryCTAS1);
    test(queryCTAS2);


    testBuilder()
        .sqlQuery(query1)
        .ordered()
        .baselineColumns("r_name")
        .baselineValues("AFRICA")
        .baselineValues("AMERICA")
        .baselineValues("ASIA")
        .baselineValues("EUROPE")
        .baselineValues("MIDDLE EAST")
        .build().run();

    testBuilder()
        .sqlQuery(query2)
        .ordered()
        .baselineColumns("col")
        .baselineValues("AFRICA")
        .baselineValues("AMERICA")
        .baselineValues("ASIA")
        .baselineValues("EUROPE")
        .baselineValues("MIDDLE EAST")
        .build().run();
  }

  @Test // DRILL-2221
  public void createJsonWithEmptyList() throws Exception {
    try {
      final String tableName = "jsonWithEmptyList";
      test("USE dfs.tmp");
      alterSession(ExecConstants.OUTPUT_FORMAT_OPTION, "json");
      test("CREATE TABLE %s AS SELECT * FROM cp.`store/json/record_with_empty_list.json`", tableName);
      test("SELECT COUNT(*) FROM %s", tableName);
    } finally {
      resetSessionOption(ExecConstants.OUTPUT_FORMAT_OPTION);
    }
  }

  @Test // DRILL-2914
  public void testGroupByStarSchemaless() throws Exception {
    String query = "SELECT n.n_nationkey AS col \n" +
        "FROM (SELECT * FROM cp.`tpch/nation.parquet`) AS n \n" +
        "GROUP BY n.n_nationkey \n" +
        "ORDER BY n.n_nationkey";

    testBuilder()
        .sqlQuery(query)
        .ordered()
        .csvBaselineFile("testframework/testExampleQueries/testGroupByStarSchemaless.tsv")
        .baselineTypes(TypeProtos.MinorType.INT)
        .baselineColumns("col")
        .build()
        .run();
  }

  @Test // DRILL-1927
  public void testGroupByCaseInSubquery() throws Exception {
    String query1 = "select (case when t.r_regionkey in (3) then 0 else 1 end) as col \n" +
        "from cp.`tpch/region.parquet` t \n" +
        "group by (case when t.r_regionkey in (3) then 0 else 1 end)";

    String query2 = "select sum(case when t.r_regionkey in (3) then 0 else 1 end) as col \n" +
        "from cp.`tpch/region.parquet` t";

    String query3 = "select (case when (r_regionkey IN (0, 2, 3, 4)) then 0 else r_regionkey end) as col1, min(r_regionkey) as col2 \n" +
        "from cp.`tpch/region.parquet` \n" +
        "group by (case when (r_regionkey IN (0, 2, 3, 4)) then 0 else r_regionkey end)";

    testBuilder()
        .sqlQuery(query1)
        .unOrdered()
        .baselineColumns("col")
        .baselineValues(0)
        .baselineValues(1)
        .build()
        .run();

    testBuilder()
        .sqlQuery(query2)
        .unOrdered()
        .baselineColumns("col")
        .baselineValues((long) 4)
        .build()
        .run();

    testBuilder()
        .sqlQuery(query3)
        .unOrdered()
        .baselineColumns("col1", "col2")
        .baselineValues(0, 0)
        .baselineValues(1, 1)
        .build()
        .run();
  }

  @Test  // DRILL-2966
  public void testHavingAggFunction() throws Exception {
    String query1 = "select n_nationkey as col \n" +
        "from cp.`tpch/nation.parquet` \n" +
        "group by n_nationkey \n" +
        "having sum(case when n_regionkey in (1, 2) then 1 else 0 end) + \n" +
        "sum(case when n_regionkey in (2, 3) then 1 else 0 end) > 1";

    String query2 = "select n_nationkey as col \n"
        + "from cp.`tpch/nation.parquet` \n"
        + "group by n_nationkey \n"
        + "having n_nationkey in \n"
        + "(select r_regionkey \n"
        + "from cp.`tpch/region.parquet` \n"
        + "group by r_regionkey \n"
        + "having sum(r_regionkey) > 0)";

    String query3 = "select n_nationkey as col \n"
        + "from cp.`tpch/nation.parquet` \n"
        + "group by n_nationkey \n"
        + "having max(n_regionkey) > ((select min(r_regionkey) from cp.`tpch/region.parquet`) + 3)";

    testBuilder()
        .sqlQuery(query1)
        .unOrdered()
        .csvBaselineFile("testframework/testExampleQueries/testHavingAggFunction/q1.tsv")
        .baselineTypes(TypeProtos.MinorType.INT)
        .baselineColumns("col")
        .build()
        .run();

    testBuilder()
        .sqlQuery(query2)
        .unOrdered()
        .csvBaselineFile("testframework/testExampleQueries/testHavingAggFunction/q2.tsv")
        .baselineTypes(TypeProtos.MinorType.INT)
        .baselineColumns("col")
        .build()
        .run();

    testBuilder()
        .sqlQuery(query3)
        .unOrdered()
        .csvBaselineFile("testframework/testExampleQueries/testHavingAggFunction/q3.tsv")
        .baselineTypes(TypeProtos.MinorType.INT)
        .baselineColumns("col")
        .build()
        .run();
  }

  @Test  //DRILL-3018
  public void testNestLoopJoinScalarSubQ() throws Exception {
    testBuilder()
        .sqlQuery("select n_nationkey from cp.`tpch/nation.parquet` where n_nationkey >= (select min(c_nationkey) from cp.`tpch/customer.parquet`)")
        .unOrdered()
        .sqlBaselineQuery("select n_nationkey from cp.`tpch/nation.parquet`")
        .build()
        .run();
  }

  @Test //DRILL-2953
  public void testGbAndObDifferentExp() throws Exception {
    testBuilder()
        .sqlQuery("select cast(columns[0] as int) as nation_key " +
          " from cp.`store/text/data/nations.csv` " +
          " group by columns[0] " +
          " order by cast(columns[0] as int)")
        .ordered()
        .csvBaselineFile("testframework/testExampleQueries/testGroupByStarSchemaless.tsv")
        .baselineTypes(TypeProtos.MinorType.INT)
        .baselineColumns("nation_key")
        .build()
        .run();

    testBuilder()
        .sqlQuery("select cast(columns[0] as int) as nation_key " +
          " from cp.`store/text/data/nations.csv` " +
          " group by cast(columns[0] as int) " +
          " order by cast(columns[0] as int)")
        .ordered()
        .csvBaselineFile("testframework/testExampleQueries/testGroupByStarSchemaless.tsv")
        .baselineTypes(TypeProtos.MinorType.INT)
        .baselineColumns("nation_key")
        .build()
        .run();

  }

  @Test  //DRILL_3004
  public void testDRILL_3004() throws Exception {
    final String query =
        "SELECT\n" +
        "  nations.N_NAME,\n" +
        "  regions.R_NAME\n" +
        "FROM\n" +
        "  cp.`tpch/nation.parquet` nations\n" +
        "JOIN\n" +
        "  cp.`tpch/region.parquet` regions\n" +
        "on nations.N_REGIONKEY = regions.R_REGIONKEY " +
        "where 1 = 0";


    try {
      setSessionOption(PlannerSettings.DISABLE_EXCHANGE_OPTION, true);
      setSessionOption(PlannerSettings.ENABLE_HASH_JOIN_OPTION, false);
      testBuilder()
          .sqlQuery(query)
          .expectsEmptyResultSet()
          .build()
          .run();
    } finally {
      resetSessionOption(PlannerSettings.DISABLE_EXCHANGE_OPTION);
      resetSessionOption(PlannerSettings.ENABLE_HASH_JOIN_OPTION);
    }
  }

  @Test
  public void testRepeatedListProjectionPastJoin() throws Exception {
    final String query = "select * from cp.`join/join-left-drill-3032.json` f1 inner join cp.`join/join-right-drill-3032.json` f2 on f1.id = f2.id";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("id", "id0", "aaa")
        .baselineValues(1L, 1L, listOf(listOf(listOf("val1"), listOf("val2"))))
        .go();
  }

  @Test
  @Ignore
  public void testPartitionCTAS() throws  Exception {
    test("use dfs.tmp; " +
        "create table mytable1  partition by (r_regionkey, r_comment) as select r_regionkey, r_name, r_comment from cp.`tpch/region.parquet`");

    test("use dfs.tmp; " +
        "create table mytable2  partition by (r_regionkey, r_comment) as select * from cp.`tpch/region.parquet` where r_name = 'abc' ");

    test("use dfs.tmp; " +
        "create table mytable3  partition by (r_regionkey, n_nationkey) as " +
        "  select r.r_regionkey, r.r_name, n.n_nationkey, n.n_name from cp.`tpch/nation.parquet` n, cp.`tpch/region.parquet` r " +
        "  where n.n_regionkey = r.r_regionkey");

    test("use dfs.tmp; " +
        "create table mytable4  partition by (r_regionkey, r_comment) as " +
        "  select  r.* from cp.`tpch/nation.parquet` n, cp.`tpch/region.parquet` r " +
        "  where n.n_regionkey = r.r_regionkey");
  }

  @Test // DRILL-3210
  public void testWindowFunAndStarCol() throws Exception {
    // SingleTableQuery : star + window function
    final String query =
        " select * , sum(n_nationkey) over (partition by n_regionkey) as sumwin " +
        " from cp.`tpch/nation.parquet`";
    final String baseQuery =
        " select n_nationkey, n_name, n_regionkey, n_comment, " +
        "   sum(n_nationkey) over (partition by n_regionkey) as sumwin " +
        " from cp.`tpch/nation.parquet`";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .sqlBaselineQuery(baseQuery)
        .build()
        .run();

    // JoinQuery: star + window function
    final String joinQuery =
        " select *, sum(n.n_nationkey) over (partition by r.r_regionkey order by r.r_name) as sumwin" +
        " from cp.`tpch/nation.parquet` n, cp.`tpch/region.parquet` r " +
        " where n.n_regionkey = r.r_regionkey";
    final String joinBaseQuery =
        " select n.n_nationkey, n.n_name, n.n_regionkey, n.n_comment, r.r_regionkey, r.r_name, r.r_comment, " +
        "   sum(n.n_nationkey) over (partition by r.r_regionkey order by r.r_name) as sumwin " +
        " from cp.`tpch/nation.parquet` n, cp.`tpch/region.parquet` r " +
        " where n.n_regionkey = r.r_regionkey";

    testBuilder()
        .sqlQuery(joinQuery)
        .unOrdered()
        .sqlBaselineQuery(joinBaseQuery)
        .build()
        .run();
  }

  @Test // see DRILL-3557
  @Category(UnlikelyTest.class)
  public void testEmptyCSVinDirectory() throws Exception {
    test("explain plan for select * from dfs.`store/text/directoryWithEmptyCSV`");
    test("explain plan for select * from cp.`store/text/directoryWithEmptyCSV/empty.csv`");
  }

  @Test
  @Category(UnlikelyTest.class)
  public void testNegativeExtractOperator() throws Exception {
    testBuilder()
        .sqlQuery("select -EXTRACT(DAY FROM birth_date) as col \n" +
          "from cp.`employee.json` \n" +
          "order by col \n" +
          "limit 5")
        .ordered()
        .baselineColumns("col")
        .baselineValues(-27l)
        .baselineValues(-27l)
        .baselineValues(-27l)
        .baselineValues(-26l)
        .baselineValues(-26l)
        .build()
        .run();
  }

  @Test // see DRILL-2313
  public void testDistinctOverAggFunctionWithGroupBy() throws Exception {
    String query1 = "select distinct count(distinct n_nationkey) as col from cp.`tpch/nation.parquet` group by n_regionkey order by 1";
    String query2 = "select distinct count(distinct n_nationkey) as col from cp.`tpch/nation.parquet` group by n_regionkey order by count(distinct n_nationkey)";
    String query3 = "select distinct sum(n_nationkey) as col from cp.`tpch/nation.parquet` group by n_regionkey order by 1";
    String query4 = "select distinct sum(n_nationkey) as col from cp.`tpch/nation.parquet` group by n_regionkey order by col";

    testBuilder()
        .sqlQuery(query1)
        .unOrdered()
        .baselineColumns("col")
        .baselineValues((long) 5)
        .build()
        .run();

    testBuilder()
        .sqlQuery(query2)
        .unOrdered()
        .baselineColumns("col")
        .baselineValues((long) 5)
        .build()
        .run();

    testBuilder()
        .sqlQuery(query3)
        .ordered()
        .baselineColumns("col")
        .baselineValues((long) 47)
        .baselineValues((long) 50)
        .baselineValues((long) 58)
        .baselineValues((long) 68)
        .baselineValues((long) 77)
        .build()
        .run();

    testBuilder()
        .sqlQuery(query4)
        .ordered()
        .baselineColumns("col")
        .baselineValues((long) 47)
        .baselineValues((long) 50)
        .baselineValues((long) 58)
        .baselineValues((long) 68)
        .baselineValues((long) 77)
        .build()
        .run();
  }

  @Test // DRILL-2190
  @Category(UnlikelyTest.class)
  public void testDateImplicitCasting() throws Exception {
    String query = "SELECT birth_date \n" +
        "FROM cp.`employee.json` \n" +
        "WHERE birth_date BETWEEN '1920-01-01' AND cast('1931-01-01' AS DATE) \n" +
        "order by birth_date";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("birth_date")
        .baselineValues("1920-04-17")
        .baselineValues("1921-12-04")
        .baselineValues("1922-08-10")
        .baselineValues("1926-10-27")
        .baselineValues("1928-03-20")
        .baselineValues("1930-01-08")
        .build()
        .run();
  }

  @Test
  public void testComparisonWithSingleValueSubQuery() throws Exception {
    String query = "select n_name from cp.`tpch/nation.parquet` " +
        "where n_nationkey = " +
        "(select r_regionkey from cp.`tpch/region.parquet` " +
        "where r_regionkey = 1)";
    PlanTestBase.testPlanMatchingPatterns(query,
        new String[]{"agg.*SINGLE_VALUE", "Filter.*=\\(\\$0, 1\\)"});

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("n_name")
        .baselineValues("ARGENTINA")
        .go();
  }

  @Test
  public void testMultipleComparisonWithSingleValueSubQuery() throws Exception {
    String query = "select a.last_name, b.n_name " +
        "from cp.`employee.json` a, cp.`tpch/nation.parquet` b " +
        "where b.n_nationkey = " +
        "(select r_regionkey from cp.`tpch/region.parquet` " +
        "where r_regionkey = 1) limit 1";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("last_name", "n_name")
        .baselineValues("Nowmer", "ARGENTINA")
        .go();
  }

  @Test
  public void testSubQueryInProjectWithVarChar() throws Exception {
    String query = "select n_name," +
        "(select r.r_name from cp.`tpch/region.parquet` r where r.r_regionkey = n.n_regionkey) as r_name\n" +
        "from cp.`tpch/nation.parquet` n order by n.n_name limit 1";
    PlanTestBase.testPlanMatchingPatterns(query, "agg.*SINGLE_VALUE");

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("n_name", "r_name")
        .baselineValues("ALGERIA", "AFRICA")
        .go();
  }

  @Test  // DRILL-7391
  public void testItemPushdownPastLeftOuterJoin() throws Exception {
    String query = "select t1.columns[0] as a, t2.columns[0] as b from cp.`store/text/data/regions.csv` t1 "
        + " left outer join cp.`store/text/data/regions.csv` t2 on t1.columns[0] = t2.columns[0]";

    PlanTestBase.testPlanMatchingPatterns(query,
        new String[]{},
        // exclude pattern where Project is projecting the 'columns' field
        new String[]{"Project.*columns"});

  }

  @Test // DRILL-6905
  public void testCombineFilterWithNumericAndVarcharLiteral() throws Exception {
    String query = "select n_nationkey from cp.`tpch/nation.parquet` where n_nationkey < 2 or n_nationkey = '10'";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("n_nationkey")
        .baselineValues(0)
        .baselineValues(1)
        .baselineValues(10)
        .go();
  }
}
