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
package org.apache.drill.exec.physical.impl.lateraljoin;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.drill.PlanTestBase;
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.test.ClientFixture;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestLateralPlans extends BaseTestQuery {
  private static final String regularTestFile_1 = "cust_order_10_1.json";
  private static final String regularTestFile_2 = "cust_order_10_2.json";

  @BeforeClass
  public static void enableUnnestLateral() throws Exception {
    dirTestWatcher.copyResourceToRoot(Paths.get("lateraljoin", "multipleFiles", regularTestFile_1));
    dirTestWatcher.copyResourceToRoot(Paths.get("lateraljoin", "multipleFiles", regularTestFile_2));
    test("alter session set `planner.enable_unnest_lateral`=true");
  }

  @Test
  public void testLateralSql() throws Exception {
    String sql = "select t.c_name, t2.ord.o_shop as o_shop from cp.`lateraljoin/nested-customer.json` t," +
        " unnest(t.orders) t2(ord) limit 1";

    PlanTestBase.testPlanMatchingPatterns(sql, new String[]{"column excluded from output: =\\[\\`orders\\`\\]"},
      new String[]{});

    testBuilder()
        .unOrdered()
        .sqlQuery(sql)
        .baselineColumns("c_name", "o_shop")
        .baselineValues("customer1", "Meno Park 1st")
        .go();
  }

  @Test
  public void testExplainLateralSql() throws Exception {
    String explainSql = "explain plan without implementation for select t.c_name, t2.ord.o_shop as o_shop from cp.`lateraljoin/nested-customer.json` t," +
        " unnest(t.orders) t2(ord) limit 1";

    String sql = "select t.c_name, t2.ord.o_shop as o_shop from cp.`lateraljoin/nested-customer.json` t," +
      " unnest(t.orders) t2(ord) limit 1";

    PlanTestBase.testPlanMatchingPatterns(sql, new String[]{"column excluded from output: =\\[\\`orders\\`\\]"},
      new String[]{});

    test(explainSql);
  }

  @Test
  public void testFilterPushCorrelate() throws Exception {
    test("alter session set `planner.slice_target`=1");
    String sql = "select t.c_name, t2.ord.o_shop as o_shop from cp.`lateraljoin/nested-customer.json` t,"
        + " unnest(t.orders) t2(ord) where t.c_name='customer1' AND t2.ord.o_shop='Meno Park 1st' ";

    PlanTestBase.testPlanMatchingPatterns(sql, new String[]{"LateralJoin(.*[\n\r])+.*Filter(.*[\n\r])+.*Scan(.*[\n\r])+.*Filter"},
        new String[]{});

    PlanTestBase.testPlanMatchingPatterns(sql, new String[]{"column excluded from output: =\\[\\`orders\\`\\]"},
      new String[]{});

    testBuilder()
        .unOrdered()
        .sqlQuery(sql)
        .baselineColumns("c_name", "o_shop")
        .baselineValues("customer1", "Meno Park 1st")
        .go();
  }

  @Test
  public void testLateralSqlPlainCol() throws Exception {
    String sql = "select t.c_name, t2.phone as c_phone from cp.`lateraljoin/nested-customer.json` t,"
        + " unnest(t.c_phone) t2(phone) limit 1";

    PlanTestBase.testPlanMatchingPatterns(sql, new String[]{"column excluded from output: =\\[\\`c_phone\\`\\]"},
      new String[]{});

    testBuilder()
        .unOrdered()
        .sqlQuery(sql)
        .baselineColumns("c_name", "c_phone")
        .baselineValues("customer1", "6505200001")
        .go();
  }

  @Test
  public void testLateralSqlStar() throws Exception {
    String sql = "select * from cp.`lateraljoin/nested-customer.json` t, unnest(t.orders) Orders(ord) limit 0";

    PlanTestBase.testPlanMatchingPatterns(sql, new String[]{},
      new String[]{"column excluded from output: =\\[\\`orders\\`\\]"});

    testBuilder()
        .unOrdered()
        .sqlQuery(sql)
        .baselineColumns("c_name", "c_id", "c_phone", "orders", "c_address", "ord")
        .expectsEmptyResultSet()
        .go();
  }

  @Test
  public void testLateralSqlStar2() throws Exception {
    String Sql = "select c.* from cp.`lateraljoin/nested-customer.json` c, unnest(c.orders) Orders(ord) limit 0";

    PlanTestBase.testPlanMatchingPatterns(Sql, new String[]{},
      new String[]{"column excluded from output: =\\[\\`orders\\`\\]"});

    testBuilder()
        .unOrdered()
        .sqlQuery(Sql)
        .baselineColumns("c_name", "c_id", "c_phone", "orders", "c_address")
        .expectsEmptyResultSet()
        .go();
  }

  @Test
  public void testLateralSqlStar3() throws Exception {
    String sql = "select Orders.*, c.* from cp.`lateraljoin/nested-customer.json` c, unnest(c.orders) Orders(ord) limit 0";

    PlanTestBase.testPlanMatchingPatterns(sql, new String[]{},
      new String[]{"column excluded from output: =\\[\\`orders\\`\\]"});

    testBuilder()
        .unOrdered()
        .sqlQuery(sql)
        .baselineColumns("ord","c_name", "c_id", "c_phone", "orders", "c_address")
        .expectsEmptyResultSet()
        .go();
  }

  @Test
  public void testLateralSqlStar4() throws Exception {
    String sql = "select Orders.* from cp.`lateraljoin/nested-customer.json` c, unnest(c.orders) Orders(ord) limit 0";

    PlanTestBase.testPlanMatchingPatterns(sql, new String[]{"column excluded from output: =\\[\\`orders\\`\\]"}, new String[]{});

    testBuilder()
        .unOrdered()
        .sqlQuery(sql)
        .baselineColumns("ord")
        .expectsEmptyResultSet()
        .go();
  }

  @Test
  public void testLateralSqlWithAS() throws Exception {
    String sql = "select t.c_name, t2.orders from cp.`lateraljoin/nested-customer.parquet` t,"
        + " unnest(t.orders) as t2(orders)";
    String baselineQuery = "select t.c_name, t2.orders from cp.`lateraljoin/nested-customer.parquet` t inner join" +
        " (select c_name, flatten(orders) from cp" +
        ".`lateraljoin/nested-customer.parquet` ) as t2(name, orders) on t.c_name = t2.name";

    PlanTestBase.testPlanMatchingPatterns(sql, new String[]{"column excluded from output: =\\[\\`orders\\`\\]"}, new String[]{});

    testBuilder()
        .unOrdered()
        .sqlQuery(sql)
        .sqlBaselineQuery(baselineQuery)
        .go();
  }

  @Test
  public void testMultiUnnestLateralAtSameLevel() throws Exception {
    String sql = "select t.c_name, t2.orders, t3.orders from cp.`lateraljoin/nested-customer.parquet` t," +
        " LATERAL ( select t2.orders from unnest(t.orders) as t2(orders)) as t2, LATERAL " +
        "(select t3.orders from unnest(t.orders) as t3(orders)) as t3";
    String baselineQuery = "select t.c_name, t2.orders, t3.orders from cp.`lateraljoin/nested-customer.parquet` t inner join" +
        " (select c_name, flatten(orders) from cp.`lateraljoin/nested-customer.parquet` ) as t2 (name, orders) on t.c_name = t2.name " +
        " inner join (select c_name, flatten(orders) from cp.`lateraljoin/nested-customer.parquet` ) as t3(name, orders) on t.c_name = t3.name";

    PlanTestBase.testPlanMatchingPatterns(sql, new String[]{"column excluded from output: =\\[\\`orders\\`\\]"}, new String[]{});

    testBuilder()
        .unOrdered()
        .sqlQuery(sql)
        .sqlBaselineQuery(baselineQuery)
        .go();
  }

  @Test
  public void testSubQuerySql() throws Exception {
    String sql = "select t.c_name, d1.items as items0 , t3.items as items1 from cp.`lateraljoin/nested-customer.parquet` t," +
        " lateral (select t2.ord.items as items from unnest(t.orders) t2(ord)) d1," +
        " unnest(d1.items) t3(items)";

    String baselineQuery = "select t.c_name, t3.orders.items as items0, t3.items as items1 from cp.`lateraljoin/nested-customer.parquet` t " +
        " inner join (select c_name, f, flatten(t1.f.items) from (select c_name, flatten(orders) as f from cp.`lateraljoin/nested-customer.parquet`) as t1 ) " +
        "t3(name, orders, items) on t.c_name = t3.name ";

    PlanTestBase.testPlanMatchingPatterns(sql, new String[]{"column excluded from output: =\\[\\`orders\\`\\]"}, new String[]{"column excluded from output: =\\[\\`items\\`\\]"});

    testBuilder()
        .unOrdered()
        .sqlQuery(sql)
        .sqlBaselineQuery(baselineQuery)
        .go();
  }

  @Test
  public void testUnnestWithFilter() throws Exception {
    String sql = "select t.c_name, d1.items as items0, t3.items as items1 from cp.`lateraljoin/nested-customer.parquet` t," +
        " lateral (select t2.ord.items as items from unnest(t.orders) t2(ord)) d1," +
        " unnest(d1.items) t3(items) where t.c_id > 1";

    String baselineQuery = "select t.c_name, t3.orders.items as items0, t3.items as items1 from cp.`lateraljoin/nested-customer.parquet` t " +
        " inner join (select c_name, f, flatten(t1.f.items) from (select c_name, flatten(orders) as f from cp.`lateraljoin/nested-customer.parquet`) as t1 ) " +
        "t3(name, orders, items) on t.c_name = t3.name where t.c_id > 1";

    PlanTestBase.testPlanMatchingPatterns(sql, new String[]{"column excluded from output: =\\[\\`orders\\`\\]"}, new String[]{"column excluded from output: =\\[\\`items\\`\\]"});

    testBuilder()
        .unOrdered()
        .sqlQuery(sql)
        .sqlBaselineQuery(baselineQuery)
        .go();
  }

  @Test
  public void testUnnestWithAggInSubquery() throws Exception {
    String sql = "select t.c_name, sum(t4.items) from cp.`lateraljoin/nested-customer.parquet` t," +
        " lateral (select t2.ord.items as items from unnest(t.orders) t2(ord)) d1," +
        " lateral (select sum(t3.items.i_number) from unnest(d1.items) t3(items)) t4(items) where t.c_id > 1 group by t.c_name";

    String baselineQuery = "select t.c_name, sum(t3.items.i_number) from cp.`lateraljoin/nested-customer.parquet` t " +
        " inner join (select c_name, f, flatten(t1.f.items) from (select c_name, flatten(orders) as f from cp.`lateraljoin/nested-customer.parquet`) as t1 ) " +
        "t3(name, orders, items) on t.c_name = t3.name where t.c_id > 1 group by t.c_name";

    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
        .setOptionDefault(PlannerSettings.ENABLE_UNNEST_LATERAL_KEY, true);

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      client
          .testBuilder()
          .ordered()
          .sqlBaselineQuery(baselineQuery)
          .sqlQuery(sql)
          .go();
    }
  }

  @Test
  public void testUnnestWithAggOnOuterTable() throws Exception {
    String sql = "select avg(d2.inum) from cp.`lateraljoin/nested-customer.parquet` t," +
        " lateral (select t2.ord.items as items from unnest(t.orders) t2(ord)) d1," +
        " lateral (select t3.items.i_number as inum from unnest(d1.items) t3(items)) d2 where t.c_id > 1 group by t.c_id";

    String baselineQuery = "select avg(t3.items.i_number) from cp.`lateraljoin/nested-customer.parquet` t " +
        " inner join (select c_name, f, flatten(t1.f.items) from (select c_name, flatten(orders) as f from cp.`lateraljoin/nested-customer.parquet`) as t1 ) " +
        "t3(name, orders, items) on t.c_name = t3.name where t.c_id > 1 group by t.c_id";

    PlanTestBase.testPlanMatchingPatterns(sql, new String[]{"column excluded from output: =\\[\\`orders\\`\\]", "column excluded from output: =\\[\\`items\\`\\]"}, new String[]{});

    testBuilder()
        .unOrdered()
        .sqlQuery(sql)
        .sqlBaselineQuery(baselineQuery)
        .go();
  }

  @Test
  public void testUnnestTableAndColumnAlias() throws Exception {
    String sql = "select t.c_name from cp.`lateraljoin/nested-customer.json` t, unnest(t.orders) ";
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
        .setOptionDefault(PlannerSettings.ENABLE_UNNEST_LATERAL_KEY, true);

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      client
          .queryBuilder()
          .sql(sql)
          .run();
    } catch (UserRemoteException ex) {
      assertTrue(ex.getMessage().contains("Alias table and column name are required for UNNEST"));
    }
  }

  @Test
  public void testUnnestColumnAlias() throws Exception {
    String sql = "select t.c_name, t2.orders from cp.`lateraljoin/nested-customer.json` t, unnest(t.orders) t2";
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
        .setOptionDefault(PlannerSettings.ENABLE_UNNEST_LATERAL_KEY, true);

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      client
          .queryBuilder()
          .sql(sql)
          .run();
    } catch (UserRemoteException ex) {
      assertTrue(ex.getMessage().contains("Alias table and column name are required for UNNEST"));
    }
  }

  /***********************************************************************************************
   Following test cases are introduced to make sure no exchanges are present on right side of
   Lateral join.
   **********************************************************************************************/

  @Test
  public void testNoExchangeWithAggWithoutGrpBy() throws Exception {
    String sql = "select d1.totalprice from dfs.`lateraljoin/multipleFiles` t," +
            " lateral ( select sum(t2.ord.o_totalprice) as totalprice from unnest(t.c_orders) t2(ord)) d1";
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
            .setOptionDefault(PlannerSettings.ENABLE_UNNEST_LATERAL_KEY, true)
            .setOptionDefault(ExecConstants.SLICE_TARGET, 1);

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      String explain = client.queryBuilder().sql(sql).explainText();
      String rightChild = getRightChildOfLateral(explain);
      assertFalse(rightChild.contains("Exchange"));
    }
  }

  @Test
  public void testNoExchangeWithStreamAggWithGrpBy() throws Exception {
    String sql = "select d1.totalprice from dfs.`lateraljoin/multipleFiles` t," +
            " lateral ( select sum(t2.ord.o_totalprice) as totalprice from unnest(t.c_orders) t2(ord) group by t2.ord.o_orderkey) d1";

    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
            .setOptionDefault(PlannerSettings.ENABLE_UNNEST_LATERAL_KEY, true)
            .setOptionDefault(ExecConstants.SLICE_TARGET, 1)
            .setOptionDefault(PlannerSettings.HASHAGG.getOptionName(), false)
            .setOptionDefault(PlannerSettings.STREAMAGG.getOptionName(), true);

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      String explain = client.queryBuilder().sql(sql).explainText();
      String rightChild = getRightChildOfLateral(explain);
      assertFalse(rightChild.contains("Exchange"));
    }
  }

  @Test
  public void testNoExchangeWithHashAggWithGrpBy() throws Exception {
    String sql = "select d1.totalprice from dfs.`lateraljoin/multipleFiles` t," +
            " lateral ( select sum(t2.ord.o_totalprice) as totalprice from unnest(t.c_orders) t2(ord) group by t2.ord.o_orderkey) d1";
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
            .setOptionDefault(PlannerSettings.ENABLE_UNNEST_LATERAL_KEY, true)
            .setOptionDefault(ExecConstants.SLICE_TARGET, 1)
            .setOptionDefault(PlannerSettings.HASHAGG.getOptionName(), true)
            .setOptionDefault(PlannerSettings.STREAMAGG.getOptionName(), false);

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      String explain = client.queryBuilder().sql(sql).explainText();
      String rightChild = getRightChildOfLateral(explain);
      assertFalse(rightChild.contains("Exchange"));
    }
  }

  @Test
  public void testNoExchangeWithOrderByWithoutLimit() throws Exception {
    String Sql = "select d1.totalprice from dfs.`lateraljoin/multipleFiles` t," +
            " lateral ( select t2.ord.o_totalprice as totalprice from unnest(t.c_orders) t2(ord) order by t2.ord.o_orderkey) d1";
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
            .setOptionDefault(PlannerSettings.ENABLE_UNNEST_LATERAL_KEY, true)
            .setOptionDefault(ExecConstants.SLICE_TARGET, 1);

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      String explain = client.queryBuilder().sql(Sql).explainText();
      String rightChild = getRightChildOfLateral(explain);
      assertFalse(rightChild.contains("Exchange"));
    }
  }

  @Test
  public void testNoExchangeWithOrderByLimit() throws Exception {
    String sql = "select d1.totalprice from dfs.`lateraljoin/multipleFiles` t," +
            " lateral ( select t2.ord.o_totalprice as totalprice from unnest(t.c_orders) t2(ord) order by t2.ord.o_orderkey limit 10) d1";
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
            .setOptionDefault(PlannerSettings.ENABLE_UNNEST_LATERAL_KEY, true)
            .setOptionDefault(ExecConstants.SLICE_TARGET, 1);

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      String explain = client.queryBuilder().sql(sql).explainText();
      String rightChild = getRightChildOfLateral(explain);
      assertFalse(rightChild.contains("Exchange"));
    }
  }

  @Test
  public void testNoExchangeWithLateralsDownStreamJoin() throws Exception {
    String sql = "select d1.totalprice from dfs.`lateraljoin/multipleFiles` t, dfs.`lateraljoin/multipleFiles` t2, " +
            " lateral ( select t2.ord.o_totalprice as totalprice from unnest(t.c_orders) t2(ord) order by t2.ord.o_orderkey limit 10) d1" +
            " where t.c_name = t2.c_name";
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
            .setOptionDefault(PlannerSettings.ENABLE_UNNEST_LATERAL_KEY, true)
            .setOptionDefault(ExecConstants.SLICE_TARGET, 1);

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      String explain = client.queryBuilder().sql(sql).explainText();
      String rightChild = getRightChildOfLateral(explain);
      assertFalse(rightChild.contains("Exchange"));
    }
  }

  @Test
  public void testNoExchangeWithLateralsDownStreamUnion() throws Exception {
    String sql = "select t.c_name from dfs.`lateraljoin/multipleFiles` t union all " +
            " select t.c_name from dfs.`lateraljoin/multipleFiles` t, " +
                    " lateral ( select t2.ord.o_totalprice as totalprice from unnest(t.c_orders) t2(ord) order by t2.ord.o_orderkey limit 10) d1";
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
            .setOptionDefault(PlannerSettings.ENABLE_UNNEST_LATERAL_KEY, true)
            .setOptionDefault(ExecConstants.SLICE_TARGET, 1);

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      String explain = client.queryBuilder().sql(sql).explainText();
      String rightChild = getRightChildOfLateral(explain);
      assertFalse(rightChild.contains("Exchange"));
    }
  }

  @Test
  public void testNoExchangeWithLateralsDownStreamAgg() throws Exception {
    String sql = "select sum(d1.totalprice) from dfs.`lateraljoin/multipleFiles` t, " +
            " lateral ( select t2.ord.o_totalprice as totalprice from unnest(t.c_orders) t2(ord) order by t2.ord.o_orderkey limit 10) d1 group by t.c_custkey";
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
            .setOptionDefault(PlannerSettings.ENABLE_UNNEST_LATERAL_KEY, true)
            .setOptionDefault(ExecConstants.SLICE_TARGET, 1)
            .setOptionDefault(PlannerSettings.HASHAGG.getOptionName(), false)
            .setOptionDefault(PlannerSettings.STREAMAGG.getOptionName(), true);

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      String explain = client.queryBuilder().sql(sql).explainText();
      String rightChild = getRightChildOfLateral(explain);
      assertFalse(rightChild.contains("Exchange"));
    }
  }

  private String getRightChildOfLateral(String explain) throws Exception {
    Matcher matcher = Pattern.compile("LateralJoin.*Unnest", Pattern.MULTILINE | Pattern.DOTALL).matcher(explain);
    assertTrue (matcher.find());
    String CorrelateUnnest = matcher.group(0);
    return CorrelateUnnest.substring(CorrelateUnnest.lastIndexOf("Scan"));
  }

  //The following test is for testing the explain plan contains relation between lateral and corresponding unnest.
  @Test
  public void testLateralAndUnnestExplainPlan() throws Exception {
    String sql = "select c.* from cp.`lateraljoin/nested-customer.json` c, unnest(c.orders) Orders(ord)";
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
            .setOptionDefault(PlannerSettings.ENABLE_UNNEST_LATERAL_KEY, true)
            .setOptionDefault(ExecConstants.SLICE_TARGET, 1);

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      String explain = client.queryBuilder().sql(sql).explainText();
      String srcOp = explain.substring(explain.indexOf("srcOp"));
      assertTrue(srcOp != null && srcOp.length() > 0);
      String correlateFragmentPattern = srcOp.substring(srcOp.indexOf("=")+1, srcOp.indexOf("]"));
      assertTrue(correlateFragmentPattern != null && correlateFragmentPattern.length() > 0);
      Matcher matcher = Pattern.compile(correlateFragmentPattern + ".*LateralJoin", Pattern.MULTILINE | Pattern.DOTALL).matcher(explain);
      assertTrue(matcher.find());
    }
  }

  @Test
  public void testUnnestTopN() throws Exception {
    String sql =
        "select customer.c_custkey," +
                "customer.c_name," +
                "t.o.o_orderkey," +
                "t.o.o_totalprice\n" +
        "from dfs.`lateraljoin/multipleFiles` customer," +
              "unnest(customer.c_orders) t(o)\n" +
        "order by customer.c_custkey," +
                  "t.o.o_orderkey," +
                  "t.o.o_totalprice\n" +
        "limit 50";

    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
        .setOptionDefault(PlannerSettings.ENABLE_UNNEST_LATERAL_KEY, true);

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      String plan = client.queryBuilder()
          .sql(sql)
          .explainText();

      assertThat("Query plan doesn't contain TopN operator",
          plan, containsString("TopN(limit=[50])"));
      assertThat("Query plan shouldn't contain Sort operator",
          plan, not(containsString("Sort")));
    }
  }

  @Test
  public void testMultiUnnestQuery() throws Exception {
    String sql = "SELECT t5.l_quantity FROM dfs.`lateraljoin/multipleFiles` t, " +
            "LATERAL (SELECT t2.ordrs.o_lineitems FROM UNNEST(t.c_orders) t2(ordrs)) t3(lineitems), " +
            "LATERAL (SELECT t4.lineitems.l_quantity FROM UNNEST(t3.lineitems) t4(lineitems)) t5(l_quantity) order by 1";

    String baselineQuery = "select dt.lineitems.l_quantity as l_quantity from (select flatten(dt.orders.o_lineitems) as lineitems " +
            "from (select flatten(c_orders) as orders from dfs.`lateraljoin/multipleFiles` t) dt)dt order by 1";

    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
      .setOptionDefault(PlannerSettings.ENABLE_UNNEST_LATERAL_KEY, true)
      .setOptionDefault(ExecConstants.SLICE_TARGET, 1);

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      client.testBuilder()
              .ordered()
              .sqlBaselineQuery(baselineQuery)
              .sqlQuery(sql)
              .go();
    }
  }

  @Test
  public void testNestedColumnQuery() throws Exception {
    String sql = "select dt.area_code as area_code, dt.ph as ph from cp.`lateraljoin/nested-customer-map.json` t," +
                 " lateral (select t2.ord.area_code as area_code , t2.ord.phone as ph from unnest(t.c_address.c_phone) t2(ord)) dt";

    String baselineQuery = "select dt.c_ph.area_code as area_code, dt.c_ph.phone as ph from (select flatten(t.c_address.c_phone) as c_ph from cp.`lateraljoin/nested-customer-map.json` t) dt";

    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
            .setOptionDefault(PlannerSettings.ENABLE_UNNEST_LATERAL_KEY, true);

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      client.testBuilder()
              .ordered()
              .sqlBaselineQuery(baselineQuery)
              .sqlQuery(sql)
              .go();
    }
  }
}
