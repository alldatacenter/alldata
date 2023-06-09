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

import org.apache.drill.exec.record.BatchSchemaBuilder;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.drill.categories.OperatorTest;
import org.apache.drill.categories.SqlTest;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.work.foreman.SqlUnsupportedException;
import org.apache.drill.exec.work.foreman.UnsupportedRelOperatorException;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.List;

@Category({SqlTest.class, OperatorTest.class})
public class TestUnionAll extends ClusterTest {

  private static final String SLICE_TARGET_DEFAULT = "alter session reset `planner.slice_target`";
  private static final String EMPTY_DIR_NAME = "empty_directory";

  @BeforeClass
  public static void setupTestFiles() throws Exception {
    startCluster(ClusterFixture.builder(dirTestWatcher));
    dirTestWatcher.copyResourceToRoot(Paths.get("multilevel", "parquet"));
    dirTestWatcher.makeTestTmpSubDir(Paths.get(EMPTY_DIR_NAME));
  }

  @Test  // Simple Union-All over two scans
  public void testUnionAll1() throws Exception {
    String query = "(select n_regionkey from cp.`tpch/nation.parquet`) union all (select r_regionkey from cp.`tpch/region.parquet`)";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .csvBaselineFile("testframework/testUnionAllQueries/q1.tsv")
        .baselineTypes(TypeProtos.MinorType.INT)
        .baselineColumns("n_regionkey")
        .build().run();
  }

  @Test  // Union-All over inner joins
  public void testUnionAll2() throws Exception {
    String query =
         "select n1.n_nationkey from cp.`tpch/nation.parquet` n1 inner join cp.`tpch/region.parquet` r1 on n1.n_regionkey = r1.r_regionkey where n1.n_nationkey in (1, 2) " +
         "union all " +
         "select n2.n_nationkey from cp.`tpch/nation.parquet` n2 inner join cp.`tpch/region.parquet` r2 on n2.n_regionkey = r2.r_regionkey where n2.n_nationkey in (3, 4)";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .csvBaselineFile("testframework/testUnionAllQueries/q2.tsv")
        .baselineTypes(TypeProtos.MinorType.INT)
        .baselineColumns("n_nationkey")
        .build().run();
  }

  @Test  // Union-All over grouped aggregates
  public void testUnionAll3() throws Exception {
    String query = "select n1.n_nationkey from cp.`tpch/nation.parquet` n1 where n1.n_nationkey in (1, 2) group by n1.n_nationkey union all select r1.r_regionkey from cp.`tpch/region.parquet` r1 group by r1.r_regionkey";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .csvBaselineFile("testframework/testUnionAllQueries/q3.tsv")
        .baselineTypes(TypeProtos.MinorType.INT)
        .baselineColumns("n_nationkey")
        .build().run();
  }

  @Test    // Chain of Union-Alls
  public void testUnionAll4() throws Exception {
    String query = "select n_regionkey from cp.`tpch/nation.parquet` union all select r_regionkey from cp.`tpch/region.parquet` union all select n_nationkey from cp.`tpch/nation.parquet` union all select c_custkey from cp.`tpch/customer.parquet` where c_custkey < 5";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .csvBaselineFile("testframework/testUnionAllQueries/q4.tsv")
        .baselineTypes(TypeProtos.MinorType.INT)
        .baselineColumns("n_regionkey")
        .build().run();
  }

  @Test  // Union-All of all columns in the table
  public void testUnionAll5() throws Exception {
    String query = "select r_name, r_comment, r_regionkey from cp.`tpch/region.parquet` r1 " +
                     "union all " +
                     "select r_name, r_comment, r_regionkey from cp.`tpch/region.parquet` r2";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .csvBaselineFile("testframework/testUnionAllQueries/q5.tsv")
        .baselineTypes(TypeProtos.MinorType.VARCHAR, TypeProtos.MinorType.VARCHAR, TypeProtos.MinorType.INT)
        .baselineColumns("r_name", "r_comment", "r_regionkey")
        .build().run();
  }

  @Test // Union-All where same column is projected twice in right child
  public void testUnionAll6() throws Exception {
    String query = "select n_nationkey, n_regionkey from cp.`tpch/nation.parquet` where n_regionkey = 1 union all select r_regionkey, r_regionkey from cp.`tpch/region.parquet` where r_regionkey = 2";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .csvBaselineFile("testframework/testUnionAllQueries/q6.tsv")
        .baselineTypes(TypeProtos.MinorType.INT, TypeProtos.MinorType.INT)
        .baselineColumns("n_nationkey", "n_regionkey")
        .build().run();
  }

  @Test // Union-All where same column is projected twice in left and right child
  public void testUnionAll6_1() throws Exception {
    String query = "select n_nationkey, n_nationkey from cp.`tpch/nation.parquet` union all select r_regionkey, r_regionkey from cp.`tpch/region.parquet`";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .csvBaselineFile("testframework/testUnionAllQueries/q6_1.tsv")
        .baselineTypes(TypeProtos.MinorType.INT, TypeProtos.MinorType.INT)
        .baselineColumns("n_nationkey", "n_nationkey0")
        .build().run();
  }

  @Test  // Union-all of two string literals of different lengths
  public void testUnionAll7() throws Exception {
    String query = "select 'abc' from cp.`tpch/region.parquet` union all select 'abcdefgh' from cp.`tpch/region.parquet`";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .csvBaselineFile("testframework/testUnionAllQueries/q7.tsv")
        .baselineTypes(TypeProtos.MinorType.VARCHAR)
        .baselineColumns("EXPR$0")
        .build().run();
  }

  @Test  // Union-all of two character columns of different lengths
  public void testUnionAll8() throws Exception {
    String query = "select n_name, n_nationkey from cp.`tpch/nation.parquet` union all select r_comment, r_regionkey  from cp.`tpch/region.parquet`";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .csvBaselineFile("testframework/testUnionAllQueries/q8.tsv")
        .baselineTypes(TypeProtos.MinorType.VARCHAR, TypeProtos.MinorType.INT)
        .baselineColumns("n_name", "n_nationkey")
        .build().run();
  }

  @Test // DRILL-1905: Union-all of * column from JSON files in different directories
  @Category(UnlikelyTest.class)
  public void testUnionAll9() throws Exception {
    String file0 = "/multilevel/json/1994/Q1/orders_94_q1.json";
    String file1 = "/multilevel/json/1995/Q1/orders_95_q1.json";

    testBuilder()
        .sqlQuery("select o_custkey, o_orderstatus, o_totalprice, o_orderdate, o_orderpriority, o_clerk, o_shippriority, o_comment, o_orderkey from cp.`%s` union " +
          "all select o_custkey, o_orderstatus, o_totalprice, o_orderdate, o_orderpriority, o_clerk, o_shippriority, o_comment, o_orderkey from cp.`%s`", file0, file1)
        .unOrdered()
        .csvBaselineFile("testframework/testUnionAllQueries/q9.tsv")
        .baselineTypes(TypeProtos.MinorType.BIGINT, TypeProtos.MinorType.VARCHAR, TypeProtos.MinorType.FLOAT8, TypeProtos.MinorType.VARCHAR,
                       TypeProtos.MinorType.VARCHAR, TypeProtos.MinorType.VARCHAR, TypeProtos.MinorType.BIGINT,TypeProtos.MinorType.VARCHAR, TypeProtos.MinorType.BIGINT)
        .baselineColumns("o_custkey", "o_orderstatus", "o_totalprice", "o_orderdate",
                         "o_orderpriority", "o_clerk", "o_shippriority", "o_comment", "o_orderkey")
        .build().run();
  }

  @Test // Union All constant literals
  public void testUnionAll10() throws Exception {
    String query = "(select n_name, 'LEFT' as LiteralConstant, n_nationkey, '1' as NumberConstant from cp.`tpch/nation.parquet`) " +
              "union all " +
              "(select 'RIGHT', r_name, '2', r_regionkey from cp.`tpch/region.parquet`)";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .csvBaselineFile("testframework/testUnionAllQueries/q10.tsv")
        .baselineTypes(TypeProtos.MinorType.VARCHAR, TypeProtos.MinorType.VARCHAR, TypeProtos.MinorType.INT, TypeProtos.MinorType.INT)
        .baselineColumns("n_name", "LiteralConstant", "n_nationkey", "NumberConstant")
        .build().run();
  }

  @Test
  @Category(UnlikelyTest.class)
  public void testUnionAllViewExpandableStar() throws Exception {
    try {
      run("use dfs.tmp");
      run("create view nation_view_testunionall_expandable_star as select n_name, n_nationkey from cp.`tpch/nation.parquet`");
      run("create view region_view_testunionall_expandable_star as select r_name, r_regionkey from cp.`tpch/region.parquet`");

      String query1 = "(select * from dfs.tmp.`nation_view_testunionall_expandable_star`) " +
          "union all " +
          "(select * from dfs.tmp.`region_view_testunionall_expandable_star`) ";

      String query2 =  "(select r_name, r_regionkey from cp.`tpch/region.parquet`) " +
          "union all " +
          "(select * from dfs.tmp.`nation_view_testunionall_expandable_star`)";

      testBuilder()
          .sqlQuery(query1)
          .unOrdered()
          .csvBaselineFile("testframework/testUnionAllQueries/q11.tsv")
          .baselineTypes(TypeProtos.MinorType.VARCHAR, TypeProtos.MinorType.INT)
          .baselineColumns("n_name", "n_nationkey")
          .build().run();

      testBuilder()
          .sqlQuery(query2)
          .unOrdered()
          .csvBaselineFile("testframework/testUnionAllQueries/q12.tsv")
          .baselineTypes(TypeProtos.MinorType.VARCHAR, TypeProtos.MinorType.INT)
          .baselineColumns("r_name", "r_regionkey")
          .build().run();
    } finally {
      run("drop view if exists nation_view_testunionall_expandable_star");
      run("drop view if exists region_view_testunionall_expandable_star");
    }
  }

  @Test(expected = UnsupportedRelOperatorException.class) // see DRILL-2002
  public void testUnionAllViewUnExpandableStar() throws Exception {
    try {
      run("use dfs.tmp");
      run("create view nation_view_testunionall_expandable_star as select * from cp.`tpch/nation.parquet`");

      String query = "(select * from dfs.tmp.`nation_view_testunionall_expandable_star`) " +
                     "union all (select * from cp.`tpch/region.parquet`)";
      run(query);
    } catch(UserException ex) {
      SqlUnsupportedException.errorClassNameToException(ex.getOrCreatePBError(false).getException().getExceptionClass());
      throw ex;
    } finally {
      run("drop view if exists nation_view_testunionall_expandable_star");
    }
  }

  @Test
  public void testDiffDataTypesAndModes() throws Exception {
    try {
      run("use dfs.tmp");
      run("create view nation_view_testunionall_expandable_star as select n_name, n_nationkey from cp.`tpch/nation.parquet`");
      run("create view region_view_testunionall_expandable_star as select r_name, r_regionkey from cp.`tpch/region.parquet`");

      String t1 = "(select n_comment, n_regionkey from cp.`tpch/nation.parquet` limit 5)";
      String t2 = "(select * from nation_view_testunionall_expandable_star  limit 5)";
      String t3 = "(select full_name, store_id from cp.`employee.json` limit 5)";
      String t4 = "(select * from region_view_testunionall_expandable_star  limit 5)";

      String query1 = t1 + " union all " + t2 + " union all " + t3 + " union all " + t4;

      testBuilder()
          .sqlQuery(query1)
          .unOrdered()
          .csvBaselineFile("testframework/testUnionAllQueries/q13.tsv")
          .baselineTypes(TypeProtos.MinorType.VARCHAR, TypeProtos.MinorType.BIGINT)
          .baselineColumns("n_comment", "n_regionkey")
          .build().run();
    } finally {
      run("drop view if exists nation_view_testunionall_expandable_star");
      run("drop view if exists region_view_testunionall_expandable_star");
    }
  }

  @Test // see DRILL-2203
  @Category(UnlikelyTest.class)
  public void testDistinctOverUnionAllwithFullyQualifiedColumnNames() throws Exception {
    String query = "select distinct sq.x1, sq.x2 " +
        "from " +
        "((select n_regionkey as a1, n_name as b1 from cp.`tpch/nation.parquet`) " +
        "union all " +
        "(select r_regionkey as a2, r_name as b2 from cp.`tpch/region.parquet`)) as sq(x1,x2)";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .csvBaselineFile("testframework/testUnionAllQueries/q14.tsv")
        .baselineTypes(TypeProtos.MinorType.INT, TypeProtos.MinorType.VARCHAR)
        .baselineColumns("x1", "x2")
        .build().run();
  }

  @Test // see DRILL-1923
  @Category(UnlikelyTest.class)
  public void testUnionAllContainsColumnANumericConstant() throws Exception {
    String query = "(select n_nationkey, n_regionkey, n_name from cp.`tpch/nation.parquet`  limit 5) " +
        "union all " +
        "(select 1, n_regionkey, 'abc' from cp.`tpch/nation.parquet` limit 5)";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .csvBaselineFile("testframework/testUnionAllQueries/q15.tsv")
        .baselineTypes(TypeProtos.MinorType.INT, TypeProtos.MinorType.INT, TypeProtos.MinorType.VARCHAR)
        .baselineColumns("n_nationkey", "n_regionkey", "n_name")
        .build().run();
  }

  @Test // see DRILL-2207
  @Category(UnlikelyTest.class)
  public void testUnionAllEmptySides() throws Exception {
    String query1 = "(select n_nationkey, n_regionkey, n_name from cp.`tpch/nation.parquet`  limit 0) " +
        "union all " +
        "(select 1, n_regionkey, 'abc' from cp.`tpch/nation.parquet` limit 5)";

    String query2 = "(select n_nationkey, n_regionkey, n_name from cp.`tpch/nation.parquet`  limit 5) " +
        "union all " +
        "(select 1, n_regionkey, 'abc' from cp.`tpch/nation.parquet` limit 0)";

    testBuilder()
        .sqlQuery(query1)
        .unOrdered()
        .csvBaselineFile("testframework/testUnionAllQueries/q16.tsv")
        .baselineTypes(TypeProtos.MinorType.INT, TypeProtos.MinorType.INT, TypeProtos.MinorType.VARCHAR)
        .baselineColumns("n_nationkey", "n_regionkey", "n_name")
        .build().run();

    testBuilder()
        .sqlQuery(query2)
        .unOrdered()
        .csvBaselineFile("testframework/testUnionAllQueries/q17.tsv")
        .baselineTypes(TypeProtos.MinorType.INT, TypeProtos.MinorType.INT, TypeProtos.MinorType.VARCHAR)
        .baselineColumns("n_nationkey", "n_regionkey", "n_name")
        .build().run();
  }

  @Test // see DRILL-1977, DRILL-2376, DRILL-2377, DRILL-2378, DRILL-2379
  @Category(UnlikelyTest.class)
  public void testAggregationOnUnionAllOperator() throws Exception {
    String root = "/store/text/data/t.json";

    testBuilder()
        .sqlQuery("(select calc1, max(b1) as `max`, min(b1) as `min`, count(c1) as `count` " +
          "from (select a1 + 10 as calc1, b1, c1 from cp.`%s` " +
          "union all select a1 + 100 as diff1, b1 as diff2, c1 as diff3 from cp.`%s`) " +
          "group by calc1 order by calc1)", root, root)
        .ordered()
        .csvBaselineFile("testframework/testExampleQueries/testAggregationOnUnionAllOperator/q1.tsv")
        .baselineTypes(TypeProtos.MinorType.BIGINT, TypeProtos.MinorType.BIGINT, TypeProtos.MinorType.BIGINT, TypeProtos.MinorType.BIGINT)
        .baselineColumns("calc1", "max", "min", "count")
        .build().run();

    testBuilder()
        .sqlQuery("(select calc1, min(b1) as `min`, max(b1) as `max`, count(c1) as `count` " +
          "from (select a1 + 10 as calc1, b1, c1 from cp.`%s` " +
          "union all select a1 + 100 as diff1, b1 as diff2, c1 as diff3 from cp.`%s`) " +
          "group by calc1 order by calc1)", root, root)
        .ordered()
        .csvBaselineFile("testframework/testExampleQueries/testAggregationOnUnionAllOperator/q2.tsv")
        .baselineTypes(TypeProtos.MinorType.BIGINT, TypeProtos.MinorType.BIGINT, TypeProtos.MinorType.BIGINT, TypeProtos.MinorType.BIGINT)
        .baselineColumns("calc1", "min", "max", "count")
        .build().run();
  }

  @Test(expected = UserException.class) // see DRILL-2590
  public void testUnionAllImplicitCastingFailure() throws Exception {
    String rootInt = "/store/json/intData.json";
    String rootBoolean = "/store/json/booleanData.json";

    run("(select key from cp.`%s` " +
        "union all " +
        "select key from cp.`%s` )", rootInt, rootBoolean);
  }

  @Test // see DRILL-2591
  @Category(UnlikelyTest.class)
  public void testDateAndTimestampJson() throws Exception {
    String rootDate = "/store/json/dateData.json";
    String rootTimpStmp = "/store/json/timeStmpData.json";

    testBuilder()
        .sqlQuery("(select max(key) as key from cp.`%s` " +
          "union all select key from cp.`%s`)", rootDate, rootTimpStmp)
        .unOrdered()
        .csvBaselineFile("testframework/testUnionAllQueries/q18_1.tsv")
        .baselineTypes(TypeProtos.MinorType.VARCHAR)
        .baselineColumns("key")
        .build().run();

    testBuilder()
        .sqlQuery("select key from cp.`%s` " +
          "union all select max(key) as key from cp.`%s`", rootDate, rootTimpStmp)
        .unOrdered()
        .csvBaselineFile("testframework/testUnionAllQueries/q18_2.tsv")
        .baselineTypes(TypeProtos.MinorType.VARCHAR)
        .baselineColumns("key")
        .build().run();

    testBuilder()
        .sqlQuery("select key from cp.`%s` " +
          "union all select max(key) as key from cp.`%s`", rootDate, rootTimpStmp)
        .unOrdered()
        .csvBaselineFile("testframework/testUnionAllQueries/q18_3.tsv")
        .baselineTypes(TypeProtos.MinorType.VARCHAR)
        .baselineColumns("key")
        .build().run();
  }

  @Test // see DRILL-2637
  @Category(UnlikelyTest.class)
  public void testUnionAllOneInputContainsAggFunction() throws Exception {
    String root = "/multilevel/csv/1994/Q1/orders_94_q1.csv";

    testBuilder()
        .sqlQuery("select * from ((select count(c1) as ct from (select columns[0] c1 from cp.`%s`)) \n" +
          "union all (select columns[0] c2 from cp.`%s`)) order by ct limit 3", root, root)
        .ordered()
        .baselineColumns("ct")
        .baselineValues((long) 10)
        .baselineValues((long) 66)
        .baselineValues((long) 99)
        .build().run();

    testBuilder()
        .sqlQuery("select * from ((select columns[0] ct from cp.`%s`)\n" +
          "union all (select count(c1) as c2 from (select columns[0] c1 from cp.`%s`))) order by ct limit 3", root, root)
        .ordered()
        .baselineColumns("ct")
        .baselineValues((long) 10)
        .baselineValues((long) 66)
        .baselineValues((long) 99)
        .build().run();

    testBuilder()
        .sqlQuery("select * from ((select count(c1) as ct from (select columns[0] c1 from cp.`%s`))\n" +
          "union all (select count(c1) as c2 from (select columns[0] c1 from cp.`%s`))) order by ct", root, root)
         .ordered()
         .baselineColumns("ct")
         .baselineValues((long) 10)
         .baselineValues((long) 10)
         .build().run();
  }

  @Test // see DRILL-2717
  @Category(UnlikelyTest.class)
  public void testUnionInputsGroupByOnCSV() throws Exception {
    String root = "/multilevel/csv/1994/Q1/orders_94_q1.csv";

    testBuilder()
        .sqlQuery("select * from \n" +
            "((select columns[0] as col0 from cp.`%s` t1 \n" +
            "where t1.columns[0] = 66) \n" +
            "union all \n" +
            "(select columns[0] c2 from cp.`%s` t2 \n" +
            "where t2.columns[0] is not null \n" +
            "group by columns[0])) \n" +
            "group by col0",
            root, root)
        .unOrdered()
        .baselineColumns("col0")
        .baselineValues("290")
        .baselineValues("291")
        .baselineValues("323")
        .baselineValues("352")
        .baselineValues("389")
        .baselineValues("417")
        .baselineValues("66")
        .baselineValues("673")
        .baselineValues("833")
        .baselineValues("99")
        .build().run();
  }

  @Test // see DRILL-2639
  @Category(UnlikelyTest.class)
  public void testUnionAllDiffTypesAtPlanning() throws Exception {
    testBuilder()
        .sqlQuery("select count(c1) as ct from (select cast(r_regionkey as int) c1 from cp.`tpch/region.parquet`) " +
          "union all (select cast(r_regionkey as int) c2 from cp.`tpch/region.parquet`)")
        .ordered()
        .baselineColumns("ct")
        .baselineValues((long) 5)
        .baselineValues((long) 0)
        .baselineValues((long) 1)
        .baselineValues((long) 2)
        .baselineValues((long) 3)
        .baselineValues((long) 4)
        .build().run();
  }

  @Test // see DRILL-2612
  @Category(UnlikelyTest.class)
  public void testUnionAllRightEmptyJson() throws Exception {
    String rootEmpty = "/project/pushdown/empty.json";
    String rootSimple = "/store/json/booleanData.json";

    testBuilder()
      .sqlQuery("select key from cp.`%s` " +
          "union all " +
          "select key from cp.`%s`",
        rootSimple,
        rootEmpty)
      .unOrdered()
      .baselineColumns("key")
      .baselineValues(true)
      .baselineValues(false)
      .build().run();
  }

  @Test
  public void testUnionAllLeftEmptyJson() throws Exception {
    final String rootEmpty = "/project/pushdown/empty.json";
    final String rootSimple = "/store/json/booleanData.json";

    testBuilder()
        .sqlQuery("select key from cp.`%s` " +
            "union all " +
            "select key from cp.`%s`",
          rootEmpty,
          rootSimple)
        .unOrdered()
        .baselineColumns("key")
        .baselineValues(true)
        .baselineValues(false)
        .build()
        .run();
  }

  @Test
  public void testUnionAllBothEmptyJson() throws Exception {
    final String rootEmpty = "/project/pushdown/empty.json";

    final List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema = Lists.newArrayList();
    final TypeProtos.MajorType majorType = TypeProtos.MajorType.newBuilder()
        .setMinorType(TypeProtos.MinorType.INT)
        .setMode(TypeProtos.DataMode.OPTIONAL)
        .build();
    expectedSchema.add(Pair.of(SchemaPath.getSimplePath("key"), majorType));

    testBuilder()
        .sqlQuery("select key from cp.`%s` " +
            "union all " +
            "select key from cp.`%s`",
          rootEmpty,
          rootEmpty)
        .schemaBaseLine(expectedSchema)
        .build()
        .run();
  }

  @Test
  public void testUnionAllRightEmptyDataBatch() throws Exception {
    String rootSimple = "/store/json/booleanData.json";

    testBuilder()
        .sqlQuery("select key from cp.`%s` " +
            "union all " +
            "select key from cp.`%s` where 1 = 0",
          rootSimple,
          rootSimple)
        .unOrdered()
        .baselineColumns("key")
        .baselineValues(true)
        .baselineValues(false)
        .build().run();
  }

  @Test
  public void testUnionAllLeftEmptyDataBatch() throws Exception {
    String rootSimple = "/store/json/booleanData.json";

    testBuilder()
        .sqlQuery("select key from cp.`%s` where 1 = 0 " +
            "union all " +
            "select key from cp.`%s`",
          rootSimple,
          rootSimple)
        .unOrdered()
        .baselineColumns("key")
        .baselineValues(true)
        .baselineValues(false)
        .build()
        .run();
  }

  @Test
  public void testUnionAllBothEmptyDataBatch() throws Exception {
    String rootSimple = "/store/json/booleanData.json";

    final List<Pair<SchemaPath, TypeProtos.MajorType>> expectedSchema = Lists.newArrayList();
    final TypeProtos.MajorType majorType = TypeProtos.MajorType.newBuilder()
        .setMinorType(TypeProtos.MinorType.BIT) // field "key" is boolean type
        .setMode(TypeProtos.DataMode.OPTIONAL)
        .build();
    expectedSchema.add(Pair.of(SchemaPath.getSimplePath("key"), majorType));

    testBuilder()
        .sqlQuery("select key from cp.`%s` where 1 = 0 " +
            "union all " +
            "select key from cp.`%s` where 1 = 0",
          rootSimple,
          rootSimple)
        .schemaBaseLine(expectedSchema)
        .build()
        .run();
  }

  @Test // see DRILL-2746
  public void testFilterPushDownOverUnionAll() throws Exception {
    String query = "select n_regionkey from \n"
        + "(select n_regionkey from cp.`tpch/nation.parquet` union all select r_regionkey from cp.`tpch/region.parquet`) \n"
        + "where n_regionkey > 0 and n_regionkey < 2 \n"
        + "order by n_regionkey";

    // Validate the plan
    final String[] expectedPlan = {"Sort.*\n" +
        ".*UnionAll.*\n" +
            ".*SelectionVectorRemover.*\n" +
                ".*Filter.*\n" +
                    ".*Scan.*columns=\\[`n_regionkey`\\].*\n" +
            ".*SelectionVectorRemover.*\n" +
                ".*Filter.*\n" +
                    ".*Scan.*columns=\\[`r_regionkey`\\].*"};
    queryBuilder()
      .sql(query)
      .planMatcher()
      .include(expectedPlan)
      .match();

    // Validate the result
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("n_regionkey")
        .baselineValues(1)
        .baselineValues(1)
        .baselineValues(1)
        .baselineValues(1)
        .baselineValues(1)
        .baselineValues(1)
        .build()
        .run();
  }

  @Test // see DRILL-2746
  @Category(UnlikelyTest.class)
  public void testInListOnUnionAll() throws Exception {
    String query = "select n_nationkey \n" +
        "from (select n1.n_nationkey from cp.`tpch/nation.parquet` n1 inner join cp.`tpch/region.parquet` r1 on n1.n_regionkey = r1.r_regionkey \n" +
        "union all \n" +
        "select n2.n_nationkey from cp.`tpch/nation.parquet` n2 inner join cp.`tpch/region.parquet` r2 on n2.n_regionkey = r2.r_regionkey) \n" +
        "where n_nationkey in (1, 2)";

    // Validate the plan
    final String[] expectedPlan = {"Project.*\n" +
        ".*UnionAll.*\n" +
            ".*Project.*\n" +
                ".*HashJoin.*\n" +
                    ".*SelectionVectorRemover.*\n" +
                        ".*Filter.*\n" +
                            ".*Scan.*columns=\\[`n_regionkey`, `n_nationkey`\\].*\n" +
                        ".*Scan.*columns=\\[`r_regionkey`\\].*\n" +
            ".*Project.*\n" +
                ".*HashJoin.*\n" +
                    ".*SelectionVectorRemover.*\n" +
                        ".*Filter.*\n" +
                            ".*Scan.*columns=\\[`n_regionkey`, `n_nationkey`\\].*\n" +
                        ".*Scan.*columns=\\[`r_regionkey`\\].*"};
    queryBuilder()
      .sql(query)
      .planMatcher()
      .include(expectedPlan)
      .match();

    // Validate the result
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("n_nationkey")
        .baselineValues(1)
        .baselineValues(2)
        .baselineValues(1)
        .baselineValues(2)
        .build()
        .run();
  }

  @Test // see DRILL-2746
  public void testFilterPushDownOverUnionAllCSV() throws Exception {
    String root = "/multilevel/csv/1994/Q1/orders_94_q1.csv";

    String query = String.format("select ct \n" +
        "from ((select count(c1) as ct from (select columns[0] c1 from cp.`%s`)) \n" +
        "union all \n" +
        "(select columns[0] c2 from cp.`%s`)) \n" +
        "where ct < 100", root, root);

    // Validate the plan
    final String[] expectedPlan = {"Project.*\n" +
        ".*UnionAll.*\n" +
            ".*SelectionVectorRemover.*\n" +
                ".*Filter.*\n" +
                    ".*StreamAgg.*\n" +
                        ".*Project.*\n" +
                            ".*Scan.*columns=\\[`columns`\\[0\\]\\].*\n" +
            ".*SelectionVectorRemover.*\n" +
                ".*Filter.*\n" +
                    ".*Project.*\n" +
                        ".*Scan.*columns=\\[`columns`\\[0\\]\\].*"};

    queryBuilder()
      .sql(query)
      .planMatcher()
      .include(expectedPlan)
      .match();

    // Validate the result
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("ct")
        .baselineValues((long) 10)
        .baselineValues((long) 66)
        .baselineValues((long) 99)
        .build().run();
  }

  @Test // see DRILL-3130
  public void testProjectPushDownOverUnionAllWithProject() throws Exception {
    String query = "select n_nationkey, n_name from \n" +
        "(select n_nationkey, n_name, n_comment from cp.`tpch/nation.parquet` \n" +
        "union all select r_regionkey, r_name, r_comment  from cp.`tpch/region.parquet`)";

    // Validate the plan
    final String[] expectedPlan = {"Project\\(n_nationkey=\\[\\$0\\], n_name=\\[\\$1\\]\\).*\n" +
        ".*UnionAll.*\n" +
            ".*Project.*\n" +
                ".*Scan.*columns=\\[`n_nationkey`, `n_name`\\].*\n" +
            ".*Project.*\n" +
                ".*Scan.*columns=\\[`r_regionkey`, `r_name`\\].*"
    };

    queryBuilder()
      .sql(query)
      .planMatcher()
      .include(expectedPlan)
      .match();

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .csvBaselineFile("testframework/testUnionAllQueries/testProjectPushDownOverUnionAllWithProject.tsv")
        .baselineTypes(TypeProtos.MinorType.INT, TypeProtos.MinorType.VARCHAR)
        .baselineColumns("n_nationkey", "n_name")
        .build()
        .run();
  }

  @Test // see DRILL-3130
  public void testProjectPushDownOverUnionAllWithoutProject() throws Exception {
    String query = "select n_nationkey from \n" +
        "(select n_nationkey, n_name, n_comment from cp.`tpch/nation.parquet` \n" +
        "union all select r_regionkey, r_name, r_comment  from cp.`tpch/region.parquet`)";

    // Validate the plan
    final String[] expectedPlan = {"Project\\(n_nationkey=\\[\\$0\\]\\).*\n" +
        ".*UnionAll.*\n" +
            ".*Scan.*columns=\\[`n_nationkey`\\].*\n" +
            ".*Scan.*columns=\\[`r_regionkey`\\].*"};

    queryBuilder()
      .sql(query)
      .planMatcher()
      .include(expectedPlan)
      .match();

    // Validate the result
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .csvBaselineFile("testframework/testUnionAllQueries/testProjectPushDownOverUnionAllWithoutProject.tsv")
        .baselineTypes(TypeProtos.MinorType.INT)
        .baselineColumns("n_nationkey")
        .build()
        .run();
  }

  @Test // see DRILL-3130
  public void testProjectWithExpressionPushDownOverUnionAll() throws Exception {
    String query = "select 2 * n_nationkey as col from \n" +
        "(select n_nationkey, n_name, n_comment from cp.`tpch/nation.parquet` \n" +
        "union all select r_regionkey, r_name, r_comment  from cp.`tpch/region.parquet`)";

    // Validate the plan
    final String[] expectedPlan = {"UnionAll.*\n" +
        ".*Project\\(col=\\[\\*\\(2, \\$0\\)\\]\\).*\n" +
            ".*Scan.*columns=\\[`n_nationkey`\\].*\n" +
        ".*Project\\(col=\\[\\*\\(2, \\$0\\)\\]\\).*\n" +
            ".*Scan.*columns=\\[`r_regionkey`\\].*"};

    queryBuilder()
      .sql(query)
      .planMatcher()
      .include(expectedPlan)
      .match();

    // Validate the result
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .csvBaselineFile("testframework/testUnionAllQueries/testProjectWithExpressionPushDownOverUnionAll.tsv")
        .baselineTypes(TypeProtos.MinorType.INT)
        .baselineColumns("col")
        .build()
        .run();
  }

  @Test // see DRILL-3130
  public void testProjectDownOverUnionAllImplicitCasting() throws Exception {
    String root = "/store/text/data/nations.csv";
    String query = String.format("select 2 * n_nationkey as col from \n" +
        "(select n_nationkey, n_name, n_comment from cp.`tpch/nation.parquet` \n" +
        "union all select columns[0], columns[1], columns[2] from cp.`%s`) \n" +
        "order by col limit 10", root);

    // Validate the plan
    final String[] expectedPlan = {"UnionAll.*\n" +
        ".*Project\\(col=\\[\\*\\(2, \\$0\\)\\]\\).*\n" +
            ".*Scan.*columns=\\[`n_nationkey`\\].*\n" +
        ".*Project\\(col=\\[\\*\\(2, ITEM\\(\\$0, 0\\)\\)\\]\\).*\n" +
            ".*Scan.*columns=\\[`columns`\\[0\\]\\]"};

    queryBuilder()
      .sql(query)
      .planMatcher()
      .include(expectedPlan)
      .match();

    // Validate the result
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .csvBaselineFile("testframework/testUnionAllQueries/testProjectDownOverUnionAllImplicitCasting.tsv")
        .baselineTypes(TypeProtos.MinorType.INT)
        .baselineColumns("col")
        .build()
        .run();
  }

  @Test // see DRILL-3130
  public void testProjectPushDownProjectColumnReorderingAndAlias() throws Exception {
    String query = "select n_comment as col1, n_nationkey as col2, n_name as col3 from \n" +
        "(select n_nationkey, n_name, n_comment from cp.`tpch/nation.parquet` \n" +
        "union all select r_regionkey, r_name, r_comment  from cp.`tpch/region.parquet`)";

    // Validate the plan
    final String[] expectedPlan = {"UnionAll.*\n." +
        "*Project.*\n" +
            ".*Scan.*columns=\\[`n_comment`, `n_nationkey`, `n_name`\\].*\n" +
        ".*Project.*\n" +
            ".*Scan.*columns=\\[`r_comment`, `r_regionkey`, `r_name`\\]"};

    queryBuilder()
      .sql(query)
      .planMatcher()
      .include(expectedPlan)
      .match();

    // Validate the result
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .csvBaselineFile("testframework/testUnionAllQueries/testProjectPushDownProjectColumnReorderingAndAlias.tsv")
        .baselineTypes(TypeProtos.MinorType.VARCHAR, TypeProtos.MinorType.INT, TypeProtos.MinorType.VARCHAR)
        .baselineColumns("col1", "col2", "col3")
        .build()
        .run();
  }

  @Test // see DRILL-2746, DRILL-3130
  public void testProjectFiltertPushDownOverUnionAll() throws Exception {
    String query = "select n_nationkey from \n" +
        "(select n_nationkey, n_name, n_comment from cp.`tpch/nation.parquet` \n" +
        "union all select r_regionkey, r_name, r_comment  from cp.`tpch/region.parquet`) \n" +
        "where n_nationkey > 0 and n_nationkey < 4";

    // Validate the plan
    final String[] expectedPlan = {"Project.*\n" +
        ".*UnionAll.*\n" +
            ".*SelectionVectorRemover.*\n" +
                ".*Filter.*\n" +
                    ".*Scan.*columns=\\[`n_nationkey`\\].*\n" +
        ".*SelectionVectorRemover.*\n" +
            ".*Filter.*\n" +
                ".*Scan.*columns=\\[`r_regionkey`\\]"
    };

    queryBuilder()
      .sql(query)
      .planMatcher()
      .include(expectedPlan)
      .match();

    // Validate the result
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .csvBaselineFile("testframework/testUnionAllQueries/testProjectFiltertPushDownOverUnionAll.tsv")
        .baselineTypes(TypeProtos.MinorType.INT)
        .baselineColumns("n_nationkey")
        .build()
        .run();
  }

  @Test // DRILL-3257 (Simplified Query from TPC-DS query 74)
  @Category(UnlikelyTest.class)
  public void testUnionAllInWith() throws Exception {
    final String query1 = "WITH year_total \n" +
        "     AS (SELECT c.r_regionkey    customer_id,\n" +
        "                1 year_total\n" +
        "         FROM   cp.`tpch/region.parquet` c\n" +
        "         UNION ALL \n" +
        "         SELECT c.r_regionkey    customer_id, \n" +
        "                1 year_total\n" +
        "         FROM   cp.`tpch/region.parquet` c) \n" +
        "SELECT count(t_s_secyear.customer_id) as ct \n" +
        "FROM   year_total t_s_firstyear, \n" +
        "       year_total t_s_secyear, \n" +
        "       year_total t_w_firstyear, \n" +
        "       year_total t_w_secyear \n" +
        "WHERE  t_s_secyear.customer_id = t_s_firstyear.customer_id \n" +
        "       AND t_s_firstyear.customer_id = t_w_secyear.customer_id \n" +
        "       AND t_s_firstyear.customer_id = t_w_firstyear.customer_id \n" +
        "       AND CASE \n" +
        "             WHEN t_w_firstyear.year_total > 0 THEN t_w_secyear.year_total \n" +
        "             ELSE NULL \n" +
        "           END > -1";

    final String query2 = "WITH year_total \n" +
        "     AS (SELECT c.r_regionkey    customer_id,\n" +
        "                1 year_total\n" +
        "         FROM   cp.`tpch/region.parquet` c\n" +
        "         UNION ALL \n" +
        "         SELECT c.r_regionkey    customer_id, \n" +
        "                1 year_total\n" +
        "         FROM   cp.`tpch/region.parquet` c) \n" +
        "SELECT count(t_w_firstyear.customer_id) as ct \n" +
        "FROM   year_total t_w_firstyear, \n" +
        "       year_total t_w_secyear \n" +
        "WHERE  t_w_firstyear.year_total = t_w_secyear.year_total \n" +
        " AND t_w_firstyear.year_total > 0 and t_w_secyear.year_total > 0";

    final String query3 = "WITH year_total_1\n" +
        "             AS (SELECT c.r_regionkey    customer_id,\n" +
        "                        1 year_total\n" +
        "                 FROM   cp.`tpch/region.parquet` c\n" +
        "                 UNION ALL \n" +
        "                 SELECT c.r_regionkey    customer_id, \n" +
        "                        1 year_total\n" +
        "                 FROM   cp.`tpch/region.parquet` c) \n" +
        "             , year_total_2\n" +
        "             AS (SELECT c.n_nationkey    customer_id,\n" +
        "                        1 year_total\n" +
        "                 FROM   cp.`tpch/nation.parquet` c\n" +
        "                 UNION ALL \n" +
        "                 SELECT c.n_nationkey    customer_id, \n" +
        "                        1 year_total\n" +
        "                 FROM   cp.`tpch/nation.parquet` c) \n" +
        "        SELECT count(t_w_firstyear.customer_id) as ct\n" +
        "        FROM   year_total_1 t_w_firstyear,\n" +
        "               year_total_2 t_w_secyear\n" +
        "        WHERE  t_w_firstyear.year_total = t_w_secyear.year_total\n" +
        "           AND t_w_firstyear.year_total > 0 and t_w_secyear.year_total > 0";

    final String query4 = "WITH year_total_1\n" +
        "             AS (SELECT c.r_regionkey    customer_id,\n" +
        "                        1 year_total\n" +
        "                 FROM   cp.`tpch/region.parquet` c\n" +
        "                 UNION ALL \n" +
        "                 SELECT c.n_nationkey    customer_id, \n" +
        "                        1 year_total\n" +
        "                 FROM   cp.`tpch/nation.parquet` c), \n" +
        "             year_total_2\n" +
        "             AS (SELECT c.r_regionkey    customer_id,\n" +
        "                        1 year_total\n" +
        "                 FROM   cp.`tpch/region.parquet` c\n" +
        "                 UNION ALL \n" +
        "                 SELECT c.n_nationkey    customer_id, \n" +
        "                        1 year_total\n" +
        "                 FROM   cp.`tpch/nation.parquet` c) \n" +
        "        SELECT count(t_w_firstyear.customer_id) as ct \n" +
        "        FROM   year_total_1 t_w_firstyear,\n" +
        "               year_total_2 t_w_secyear\n" +
        "        WHERE  t_w_firstyear.year_total = t_w_secyear.year_total\n" +
        "         AND t_w_firstyear.year_total > 0 and t_w_secyear.year_total > 0";

    testBuilder()
        .sqlQuery(query1)
        .ordered()
        .baselineColumns("ct")
        .baselineValues((long) 80)
        .build()
        .run();

    testBuilder()
        .sqlQuery(query2)
        .ordered()
        .baselineColumns("ct")
        .baselineValues((long) 100)
        .build()
        .run();

    testBuilder()
        .sqlQuery(query3)
        .ordered()
        .baselineColumns("ct")
        .baselineValues((long) 500)
        .build()
        .run();

    testBuilder()
        .sqlQuery(query4)
        .ordered()
        .baselineColumns("ct")
        .baselineValues((long) 900)
        .build()
        .run();
  }

  @Test // DRILL-4147 // base case
  @Category(UnlikelyTest.class)
  public void testDrill4147_1() throws Exception {
    final String l = "/multilevel/parquet/1994";
    final String r = "/multilevel/parquet/1995";

    final String query = String.format("SELECT o_custkey FROM dfs.`%s` \n" +
        "Union All SELECT o_custkey FROM dfs.`%s`", l, r);

    // Validate the plan
    final String[] expectedPlan = {"UnionExchange.*\n",
        ".*UnionAll"};

    try {
      client.alterSession(ExecConstants.SLICE_TARGET, 1);
      queryBuilder()
        .sql(query)
        .planMatcher()
        .include(expectedPlan)
        .match();

      testBuilder()
        .optionSettingQueriesForBaseline(SLICE_TARGET_DEFAULT)
        .unOrdered()
        .sqlQuery(query)
        .sqlBaselineQuery(query)
        .build()
        .run();
    } finally {
      client.resetSession(ExecConstants.SLICE_TARGET);
    }
  }

  @Test // DRILL-4147  // group-by on top of union-all
  public void testDrill4147_2() throws Exception {
    final String l = "/multilevel/parquet/1994";
    final String r = "/multilevel/parquet/1995";

    final String query = String.format("Select o_custkey, count(*) as cnt from \n" +
        " (SELECT o_custkey FROM dfs.`%s` \n" +
        "Union All SELECT o_custkey FROM dfs.`%s`) \n" +
        "group by o_custkey", l, r);

    // Validate the plan
    final String[] expectedPlan = {"(?s)UnionExchange.*HashAgg.*HashToRandomExchange.*UnionAll.*"};

    try {
      client.alterSession(ExecConstants.SLICE_TARGET, 1);
      queryBuilder()
        .sql(query)
        .planMatcher()
        .include(expectedPlan)
        .match();

      testBuilder()
        .optionSettingQueriesForBaseline(SLICE_TARGET_DEFAULT)
        .unOrdered()
        .sqlQuery(query)
        .sqlBaselineQuery(query)
        .build()
        .run();
    } finally {
      client.resetSession(ExecConstants.SLICE_TARGET);
    }
  }

  @Test // DRILL-4147 // union-all above a hash join
  public void testDrill4147_3() throws Exception {
    final String l = "/multilevel/parquet/1994";
    final String r = "/multilevel/parquet/1995";

    final String query = String.format("SELECT o_custkey FROM \n" +
        " (select o1.o_custkey from dfs.`%s` o1 inner join dfs.`%s` o2 on o1.o_orderkey = o2.o_custkey) \n" +
        " Union All SELECT o_custkey FROM dfs.`%s` where o_custkey > 10", l, r, l);

    // Validate the plan
    final String[] expectedPlan = {"(?s)UnionExchange.*UnionAll.*HashJoin.*"};

    try {
      client.alterSession(ExecConstants.SLICE_TARGET, 1);
      queryBuilder()
        .sql(query)
        .planMatcher()
        .include(expectedPlan)
        .match();

      testBuilder()
        .optionSettingQueriesForBaseline(SLICE_TARGET_DEFAULT)
        .unOrdered()
        .sqlQuery(query)
        .sqlBaselineQuery(query)
        .build()
        .run();
    } finally {
      client.resetSession(ExecConstants.SLICE_TARGET);
    }
  }

  @Test // DRILL-4833  // limit 1 is on RHS of union-all
  @Category(UnlikelyTest.class)
  public void testDrill4833_1() throws Exception {
    final String l = "/multilevel/parquet/1994";
    final String r = "/multilevel/parquet/1995";

    final String query = String.format("SELECT o_custkey FROM \n" +
        " ((select o1.o_custkey from dfs.`%s` o1 inner join dfs.`%s` o2 on o1.o_orderkey = o2.o_custkey) \n" +
        " Union All (SELECT o_custkey FROM dfs.`%s` limit 1))", l, r, l);

    // Validate the plan
    final String[] expectedPlan = {"(?s)UnionExchange.*UnionAll.*HashJoin.*"};

    try {
      client.alterSession(ExecConstants.SLICE_TARGET, 1);
      client.alterSession(PlannerSettings.UNIONALL_DISTRIBUTE_KEY, true);
      queryBuilder()
        .sql(query)
        .planMatcher()
        .include(expectedPlan)
        .match();

      testBuilder()
        .optionSettingQueriesForBaseline(SLICE_TARGET_DEFAULT)
        .unOrdered()
        .sqlQuery(query)
        .sqlBaselineQuery(query)
        .build()
        .run();
    } finally {
      client.resetSession(ExecConstants.SLICE_TARGET);
      client.resetSession(PlannerSettings.UNIONALL_DISTRIBUTE_KEY);
    }
  }

  @Test // DRILL-4833  // limit 1 is on LHS of union-all
  @Category(UnlikelyTest.class)
  public void testDrill4833_2() throws Exception {
    final String l = "/multilevel/parquet/1994";
    final String r = "/multilevel/parquet/1995";

    final String query = String.format("SELECT o_custkey FROM \n" +
        " ((SELECT o_custkey FROM dfs.`%s` limit 1) \n" +
        " union all \n" +
        " (select o1.o_custkey from dfs.`%s` o1 inner join dfs.`%s` o2 on o1.o_orderkey = o2.o_custkey))", l, r, l);

    // Validate the plan
    final String[] expectedPlan = {"(?s)UnionExchange.*UnionAll.*HashJoin.*"};

    try {
      client.alterSession(ExecConstants.SLICE_TARGET, 1);
      client.alterSession(PlannerSettings.UNIONALL_DISTRIBUTE_KEY, true);
      queryBuilder()
        .sql(query)
        .planMatcher()
        .include(expectedPlan)
        .match();

      testBuilder()
        .optionSettingQueriesForBaseline(SLICE_TARGET_DEFAULT)
        .unOrdered()
        .sqlQuery(query)
        .sqlBaselineQuery(query)
        .build()
        .run();
    } finally {
      client.resetSession(ExecConstants.SLICE_TARGET);
      client.resetSession(PlannerSettings.UNIONALL_DISTRIBUTE_KEY);
    }
  }

  @Test // DRILL-5130
  public void testUnionAllWithValues() throws Exception {
    testBuilder()
        .sqlQuery("values('A') union all values('B')")
        .unOrdered()
        .baselineColumns("EXPR$0")
        .baselineValues("A")
        .baselineValues("B")
        .go();
  }

  @Test // DRILL-4264
  @Category(UnlikelyTest.class)
  public void testFieldWithDots() throws Exception {
    String fileName = "table.json";
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dirTestWatcher.getRootDir(), fileName)))) {
      writer.write("{\"rk.q\": \"a\", \"m\": {\"a.b\":\"1\", \"a\":{\"b\":\"2\"}, \"c\":\"3\"}}");
    }

    testBuilder()
      .sqlQuery("select * from (" +
        "(select t.m.`a.b` as a,\n" +
        "t.m.a.b as b,\n" +
        "t.m['a.b'] as c,\n" +
        "t.rk.q as d,\n" +
        "t.`rk.q` as e\n" +
        "from dfs.`%1$s` t)\n" +
        "union all\n" +
        "(select t.m.`a.b` as a,\n" +
        "t.m.a.b as b,\n" +
        "t.m['a.b'] as c,\n" +
        "t.rk.q as d,\n" +
        "t.`rk.q` as e\n" +
        "from dfs.`%1$s` t))", fileName)
      .unOrdered()
      .baselineColumns("a", "b", "c", "d", "e")
      .baselineValues("1", "2", "1", null, "a")
      .baselineValues("1", "2", "1", null, "a")
      .go();
  }

  @Test
  public void testUnionAllRightEmptyDir() throws Exception {
    String rootSimple = "/store/json/booleanData.json";

    testBuilder()
        .sqlQuery("SELECT key FROM cp.`%s` UNION ALL SELECT key FROM dfs.tmp.`%s`",
            rootSimple, EMPTY_DIR_NAME)
        .unOrdered()
        .baselineColumns("key")
        .baselineValues(true)
        .baselineValues(false)
        .build()
        .run();
  }

  @Test
  public void testUnionAllLeftEmptyDir() throws Exception {
    final String rootSimple = "/store/json/booleanData.json";

    testBuilder()
        .sqlQuery("SELECT key FROM dfs.tmp.`%s` UNION ALL SELECT key FROM cp.`%s`",
            EMPTY_DIR_NAME, rootSimple)
        .unOrdered()
        .baselineColumns("key")
        .baselineValues(true)
        .baselineValues(false)
        .build()
        .run();
  }

  @Test
  public void testUnionAllBothEmptyDirs() throws Exception {
    SchemaBuilder schemaBuilder = new SchemaBuilder()
        .addNullable("key", TypeProtos.MinorType.INT);
    BatchSchema expectedSchema = new BatchSchemaBuilder()
        .withSchemaBuilder(schemaBuilder)
        .build();

    testBuilder()
        .sqlQuery("SELECT key FROM dfs.tmp.`%1$s` UNION ALL SELECT key FROM dfs.tmp.`%1$s`", EMPTY_DIR_NAME)
        .schemaBaseLine(expectedSchema)
        .build()
        .run();
  }

  @Test
  public void testUnionAllMiddleEmptyDir() throws Exception {
    final String query = "SELECT n_regionkey FROM cp.`tpch/nation.parquet` UNION ALL " +
        "SELECT missing_key FROM dfs.tmp.`%s` UNION ALL SELECT r_regionkey FROM cp.`tpch/region.parquet`";

    testBuilder()
        .sqlQuery(query, EMPTY_DIR_NAME)
        .unOrdered()
        .csvBaselineFile("testframework/testUnionAllQueries/q1.tsv")
        .baselineTypes(TypeProtos.MinorType.INT)
        .baselineColumns("n_regionkey")
        .build()
        .run();
  }

  @Test
  public void testComplexQueryWithUnionAllAndEmptyDir() throws Exception {
    final String rootSimple = "/store/json/booleanData.json";

    testBuilder()
        .sqlQuery("SELECT key FROM dfs.tmp.`%1$s` UNION ALL SELECT key FROM " +
            "(SELECT key FROM dfs.tmp.`%1$s` UNION ALL SELECT key FROM cp.`%2$s`)",
            EMPTY_DIR_NAME, rootSimple)
        .unOrdered()
        .baselineColumns("key")
        .baselineValues(true)
        .baselineValues(false)
        .build()
        .run();
  }

  @Test // DRILL-3855
  public void testEmptyResultAfterProjectPushDownOverUnionAll() throws Exception {
    String query = "select n_nationkey from " +
        "(select n_nationkey, n_name, n_comment from cp.`tpch/nation.parquet` " +
        "union all select r_regionkey, r_name, r_comment  from cp.`tpch/region.parquet`) " +
        "where n_nationkey > 4";

    // Validate the plan
    final String[] expectedPlan = {"Project.*\n" +
        ".*UnionAll.*\n" +
            ".*SelectionVectorRemover.*\n" +
                ".*Filter.*\n" +
                    ".*Scan.*columns=\\[`n_nationkey`\\].*\n" +
            ".*SelectionVectorRemover.*\n" +
                ".*Filter.*\n" +
                    ".*Scan.*columns=\\[`r_regionkey`\\]"};

      queryBuilder()
        .sql(query)
        .planMatcher()
        .include(expectedPlan)
        .match();


    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .csvBaselineFile("testframework/testUnionAllQueries/testEmptyResultAfterProjectPushDownOverUnionAll.tsv")
        .baselineTypes(TypeProtos.MinorType.INT)
        .baselineColumns("n_nationkey")
        .build()
        .run();
  }

  @Test // DRILL-8137
  public void testUnionCancellation() throws Exception {
    String query = "WITH foo AS\n" +
      "  (SELECT 1 AS a FROM cp.`/tpch/nation.parquet`\n" +
      "   UNION ALL\n" +
      "   SELECT 1 AS a FROM cp.`/tpch/nation.parquet`\n" +
      "   WHERE n_nationkey > (SELECT 1) )\n" +
      "SELECT * FROM foo\n" +
      "LIMIT 1";

    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .baselineColumns("a")
      .baselineValues(1)
      .build()
      .run();
  }
}
