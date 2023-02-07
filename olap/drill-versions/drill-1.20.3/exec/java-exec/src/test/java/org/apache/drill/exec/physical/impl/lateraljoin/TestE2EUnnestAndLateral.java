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

import org.apache.drill.categories.OperatorTest;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.TestBuilder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.nio.file.Paths;

import static junit.framework.TestCase.fail;

@Category(OperatorTest.class)
public class TestE2EUnnestAndLateral extends ClusterTest {

  private static final String regularTestFile_1 = "cust_order_10_1.json";
  private static final String regularTestFile_2 = "cust_order_10_2.json";
  private static final String schemaChangeFile_1 = "cust_order_10_2_stringNationKey.json";
  private static final String schemaChangeFile_2 = "cust_order_10_2_stringOrderShipPriority.json";
  private static final String schemaChangeFile_3 = "cust_order_10_2_stringNationKey_ShipPriority.json";

  @BeforeClass
  public static void setupTestFiles() throws Exception {
    dirTestWatcher.copyResourceToRoot(Paths.get("lateraljoin", "multipleFiles", regularTestFile_1));
    dirTestWatcher.copyResourceToRoot(Paths.get("lateraljoin", "multipleFiles", regularTestFile_2));
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
        .sessionOption(PlannerSettings.ENABLE_UNNEST_LATERAL_KEY, true)
        .maxParallelization(1);
    startCluster(builder);
  }

  /***********************************************************************************************
   Test with single batch but using different keyword for Lateral and it has limit/filter operator
   within the subquery of Lateral and Unnest
   **********************************************************************************************/

  @Test
  public void testLateral_WithLimitInSubQuery() throws Exception {
    String sql = "SELECT customer.c_name, customer.c_address, orders.o_id, orders.o_amount " +
      "FROM cp.`lateraljoin/nested-customer.parquet` customer, LATERAL " +
      "(SELECT t.ord.o_id as o_id, t.ord.o_amount as o_amount FROM UNNEST(customer.orders) t(ord) LIMIT 1) orders";
    runAndLog(sql);
  }

  @Test
  public void testLateral_WithFilterInSubQuery() throws Exception {
    String sql = "SELECT customer.c_name, customer.c_address, orders.o_id, orders.o_amount " +
      "FROM cp.`lateraljoin/nested-customer.parquet` customer, LATERAL " +
      "(SELECT t.ord.o_id as o_id, t.ord.o_amount as o_amount FROM UNNEST(customer.orders) t(ord) WHERE t.ord.o_amount > 10) orders";
    runAndLog(sql);
  }

  @Test
  public void testLateral_WithFilterAndLimitInSubQuery() throws Exception {
    String sql = "SELECT customer.c_name, customer.c_address, orders.o_id, orders.o_amount " +
      "FROM cp.`lateraljoin/nested-customer.parquet` customer, LATERAL " +
      "(SELECT t.ord.o_id as o_id, t.ord.o_amount as o_amount FROM UNNEST(customer.orders) t(ord) WHERE t.ord.o_amount > 10 LIMIT 1) orders";
    runAndLog(sql);
  }

  @Test
  public void testLateral_WithTopNInSubQuery() throws Exception {
    runAndLog("alter session set `planner.enable_topn`=false");

    String sql = "SELECT customer.c_name, orders.o_id, orders.o_amount " +
      "FROM cp.`lateraljoin/nested-customer.parquet` customer, LATERAL " +
      "(SELECT t.ord.o_id as o_id, t.ord.o_amount as o_amount FROM UNNEST(customer.orders) t(ord) ORDER BY " +
      "o_amount DESC LIMIT 1) orders";

    try {
      testBuilder()
         .sqlQuery(sql)
         .unOrdered()
         .baselineColumns("c_name", "o_id", "o_amount")
         .baselineValues("customer1", 3.0,  294.5)
         .baselineValues("customer2", 10.0,  724.5)
         .baselineValues("customer3", 23.0,  772.2)
         .baselineValues("customer4", 32.0,  1030.1)
         .go();
    } finally {
      runAndLog("alter session set `planner.enable_topn`=true");
    }
  }

  /**
   * Test which disables the TopN operator from planner settintestLateral_WithTopNInSubQuerygs before running query using SORT and LIMIT in
   * subquery. The same query as in above test is executed and same result is expected.
   */
  @Test
  public void testLateral_WithSortAndLimitInSubQuery() throws Exception {

    runAndLog("alter session set `planner.enable_topn`=false");

    String sql = "SELECT customer.c_name, orders.o_id, orders.o_amount " +
      "FROM cp.`lateraljoin/nested-customer.parquet` customer, LATERAL " +
      "(SELECT t.ord.o_id as o_id, t.ord.o_amount as o_amount FROM UNNEST(customer.orders) t(ord) ORDER BY " +
      "o_amount DESC LIMIT 1) orders";

    try {
      testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("c_name", "o_id", "o_amount")
        .baselineValues("customer1", 3.0,  294.5)
        .baselineValues("customer2", 10.0,  724.5)
        .baselineValues("customer3", 23.0,  772.2)
        .baselineValues("customer4", 32.0,  1030.1)
        .go();
    } finally {
      runAndLog("alter session set `planner.enable_topn`=true");
    }
  }

  @Test
  public void testLateral_WithSortInSubQuery() throws Exception {
    String sql = "SELECT customer.c_name, orders.o_id, orders.o_amount " +
      "FROM cp.`lateraljoin/nested-customer.parquet` customer, LATERAL " +
      "(SELECT t.ord.o_id as o_id, t.ord.o_amount as o_amount FROM UNNEST(customer.orders) t(ord) ORDER BY " +
      "o_amount DESC) orders WHERE customer.c_id = 1.0";

    testBuilder()
      .sqlQuery(sql)
      .ordered()
      .baselineColumns("c_name", "o_id", "o_amount")
      .baselineValues("customer1", 3.0,  294.5)
      .baselineValues("customer1", 2.0,  104.5)
      .baselineValues("customer1", 1.0,  4.5)
      .go();
  }

  @Test
  public void testOuterApply_WithFilterAndLimitInSubQuery() throws Exception {
    String sql = "SELECT customer.c_name, customer.c_address, orders.o_id, orders.o_amount " +
      "FROM cp.`lateraljoin/nested-customer.parquet` customer OUTER APPLY " +
      "(SELECT t.ord.o_id as o_id , t.ord.o_amount as o_amount FROM UNNEST(customer.orders) t(ord) WHERE t.ord.o_amount > 10 LIMIT 1) orders";
    runAndLog(sql);
  }

  @Test
  public void testLeftLateral_WithFilterAndLimitInSubQuery() throws Exception {
    String sql = "SELECT customer.c_name, customer.c_address, orders.o_id, orders.o_amount " +
      "FROM cp.`lateraljoin/nested-customer.parquet` customer LEFT JOIN LATERAL " +
      "(SELECT t.ord.o_id as o_id, t.ord.o_amount as o_amount FROM UNNEST(customer.orders) t(ord) WHERE t.ord.o_amount > 10 LIMIT 1) orders ON TRUE";
    runAndLog(sql);
  }

  @Test
  public void testMultiUnnestAtSameLevel() throws Exception {
    String sql = "EXPLAIN PLAN FOR SELECT customer.c_name, customer.c_address, U1.order_id, U1.order_amt," +
            " U1.itemName, U1.itemNum" + " FROM cp.`lateraljoin/nested-customer.parquet` customer, LATERAL" +
            " (SELECT t.ord.o_id AS order_id, t.ord.o_amount AS order_amt, U2.item_name AS itemName, U2.item_num AS " +
            "itemNum FROM UNNEST(customer.orders) t(ord) , LATERAL" +
            " (SELECT t1.ord.i_name AS item_name, t1.ord.i_number AS item_num FROM UNNEST(t.ord) AS t1(ord)) AS U2) AS U1";
    runAndLog(sql);
  }

  @Test
  public void testMultiUnnestAtSameLevelExec() throws Exception {
    String sql = "SELECT customer.c_name, customer.c_address, U1.order_id, U1.order_amt," +
      " U1.itemName, U1.itemNum FROM cp.`lateraljoin/nested-customer.parquet` customer, LATERAL" +
      " (SELECT dt.order_id, dt.order_amt, U2.item_name AS itemName, U2.item_num AS itemNum from" +
            "(select t.ord.items as items, t.ord.o_id AS order_id, t.ord.o_amount AS order_amt FROM UNNEST(customer.orders) t(ord)) dt , LATERAL" +
      " (SELECT t1.items.i_name AS item_name, t1.items.i_number AS item_num FROM UNNEST(dt.items) AS t1(items)) AS U2) AS U1";
    String baseline = "SELECT customer.c_name, customer.c_address, U1.order_id, U1.order_amount as order_amt, U2.item_name as itemName, U2.item_num as itemNum" +
            " FROM cp.`lateraljoin/nested-customer.parquet` customer, LATERAL " +
            "(SELECT t.ord.items as items, t.ord.o_id as order_id, t.ord.o_amount as order_amount from UNNEST(customer.orders) t(ord)) U1, LATERAL" +
            "(SELECT t1.items.i_name as item_name, t1.items.i_number as item_num from UNNEST(U1.items) t1(items)) U2";
    testBuilder()
            .unOrdered()
            .sqlQuery(sql)
            .sqlBaselineQuery(baseline)
            .go();
  }

  @Test
  public void testMultiUnnestAtSameLevelExecExplicitResult() throws Exception {
    String sql = "SELECT customer.c_name, customer.c_address, U1.order_id, U1.order_amt," +
            " U1.itemName, U1.itemNum FROM cp.`lateraljoin/nested-customer.parquet` customer, LATERAL" +
            " (SELECT dt.order_id, dt.order_amt, U2.item_name AS itemName, U2.item_num AS itemNum from" +
            "(select t.ord.items as items, t.ord.o_id AS order_id, t.ord.o_amount AS order_amt FROM UNNEST(customer.orders) t(ord)) dt , LATERAL" +
            " (SELECT t1.items.i_name AS item_name, t1.items.i_number AS item_num FROM UNNEST(dt.items) AS t1(items)) AS U2) AS U1 order by 1,2,3,4,5,6 limit 1";
    testBuilder()
            .unOrdered()
            .sqlQuery(sql)
            .baselineColumns("c_name", "c_address", "order_id", "order_amt", "itemName", "itemNum")
            .baselineValues("customer1","bay area, CA",1.0,4.5,"cheese",9.0)
            .go();
  }

  @Test
  public void testUnnestWithItem() throws Exception {
    String sql = "select u.item from\n" +
        "cp.`lateraljoin/nested-customer.parquet` c," +
        "unnest(c.orders[0]['items']) as u(item)\n" +
        "limit 1";

    testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("item")
        .baselineValues(
            TestBuilder.mapOf("i_name", "paper towel",
                "i_number", 2.0,
                "i_supplier", "oregan"))
        .go();
  }

  @Test
  public void testUnnestWithFunctionCall() throws Exception {
    String sql = "select u.ord.o_amount o_amount from\n" +
        "cp.`lateraljoin/nested-customer.parquet` c," +
        "unnest(convert_fromjson(convert_tojson(c.orders))) as u(ord)\n" +
        "limit 1";

    testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("o_amount")
        .baselineValues(4.5)
        .go();
  }

  @Test
  public void testUnnestWithMap() throws Exception {
    String sql = "select u.item from\n" +
        "cp.`lateraljoin/nested-customer.parquet` c," +
        "unnest(c.orders[0].items) as u(item)\n" +
        "limit 1";

    testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("item")
        .baselineValues(
            TestBuilder.mapOf("i_name", "paper towel",
                "i_number", 2.0,
                "i_supplier", "oregan"))
        .go();
  }

  @Test
  public void testMultiUnnestWithMap() throws Exception {
    String sql = "select u.item from\n" +
        "cp.`lateraljoin/nested-customer.parquet` c," +
        "unnest(c.orders[0].items) as u(item)," +
        "unnest(c.orders[0].items) as u1(item1)\n" +
        "limit 1";

    testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("item")
        .baselineValues(
            TestBuilder.mapOf("i_name", "paper towel",
                "i_number", 2.0,
                "i_supplier", "oregan"))
        .go();
  }

  @Test
  public void testSingleUnnestCol() throws Exception {
    String sql =
      "select t.orders.o_id as id " +
      "from (select u.orders from\n" +
            "cp.`lateraljoin/nested-customer.parquet` c," +
            "unnest(c.orders) as u(orders)\n" +
            "limit 1) t";

    testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("id")
        .baselineValues(1.0)
        .go();
  }

  @Test
  public void testNestedUnnest() throws Exception {
    String sql = "select * from (select customer.orders as orders from cp.`lateraljoin/nested-customer.parquet` customer ) t1," +
        " lateral ( select t.ord.items as items from unnest(t1.orders) t(ord) ) t2, unnest(t2.items) t3(item) ";
    runAndLog(sql);
  }

  /***********************************************************************************************
   Test where multiple files are used to trigger multiple batch scenario. And it has limit/filter
   within the subquery of Lateral and Unnest
   **********************************************************************************************/
  @Test
  public void testMultipleBatchesLateralQuery() throws Exception {
    String sql = "SELECT customer.c_name, customer.c_address, orders.o_orderkey, orders.o_totalprice " +
      "FROM dfs.`lateraljoin/multipleFiles` customer, LATERAL " +
      "(SELECT t.ord.o_orderkey as o_orderkey, t.ord.o_totalprice as o_totalprice FROM UNNEST(customer.c_orders) t(ord)) orders";
    runAndLog(sql);
  }

  @Test
  public void testMultipleBatchesLateral_WithLimitInSubQuery() throws Exception {
    String sql = "SELECT customer.c_name, customer.c_address, orders.o_orderkey, orders.o_totalprice " +
      "FROM dfs.`lateraljoin/multipleFiles` customer, LATERAL " +
      "(SELECT t.ord.o_orderkey as o_orderkey, t.ord.o_totalprice as o_totalprice FROM UNNEST(customer.c_orders) t(ord) LIMIT 10) orders";
    runAndLog(sql);
  }

  @Test
  public void testMultipleBatchesLateral_WithTopNInSubQuery() throws Exception {
    runAndLog("alter session set `planner.enable_topn`=false");

    String sql = "SELECT customer.c_name, orders.o_orderkey, orders.o_totalprice " +
      "FROM dfs.`lateraljoin/multipleFiles` customer, LATERAL " +
      "(SELECT t.ord.o_orderkey as o_orderkey, t.ord.o_totalprice as o_totalprice FROM UNNEST(customer.c_orders) t(ord)" +
      " ORDER BY o_totalprice DESC LIMIT 1) orders";

    testBuilder()
      .sqlQuery(sql)
      .unOrdered()
      .baselineColumns("c_name", "o_orderkey", "o_totalprice")
      .baselineValues("Customer#000951313", (long)47035683, 306996.2)
      .baselineValues("Customer#000007180", (long)54646821, 367189.55)
      .go();
  }

  @Test
  public void testMultipleBatchesLateral_WithSortAndLimitInSubQuery() throws Exception {

    runAndLog("alter session set `planner.enable_topn`=false");

    String sql = "SELECT customer.c_name, orders.o_orderkey, orders.o_totalprice " +
      "FROM dfs.`lateraljoin/multipleFiles` customer, LATERAL " +
      "(SELECT t.ord.o_orderkey as o_orderkey, t.ord.o_totalprice as o_totalprice FROM UNNEST(customer.c_orders) t(ord)" +
      " ORDER BY o_totalprice DESC LIMIT 1) orders";

    try {
      testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("c_name", "o_orderkey", "o_totalprice")
        .baselineValues("Customer#000951313", (long)47035683, 306996.2)
        .baselineValues("Customer#000007180", (long)54646821, 367189.55)
        .go();
    } finally {
      runAndLog("alter session set `planner.enable_topn`=true");
    }
  }

  @Test
  public void testMultipleBatchesLateral_WithSortInSubQuery() throws Exception {

    String sql = "SELECT customer.c_name, customer.c_custkey, orders.o_orderkey, orders.o_totalprice " +
      "FROM dfs.`lateraljoin/multipleFiles` customer, LATERAL " +
      "(SELECT t.ord.o_orderkey as o_orderkey, t.ord.o_totalprice as o_totalprice FROM UNNEST(customer.c_orders) t(ord)" +
      " ORDER BY o_totalprice DESC) orders WHERE customer.c_custkey = '7180' LIMIT 1";

    testBuilder()
      .sqlQuery(sql)
      .ordered()
      .baselineColumns("c_name", "c_custkey", "o_orderkey", "o_totalprice")
      .baselineValues("Customer#000007180", "7180", (long) 54646821, 367189.55)
      .go();

  }

  @Test
  public void testMultipleBatchesLateral_WithLimitFilterInSubQuery() throws Exception {
    String sql = "SELECT customer.c_name, customer.c_address, orders.o_orderkey, orders.o_totalprice " +
      "FROM dfs.`lateraljoin/multipleFiles` customer, LATERAL " +
      "(SELECT t.ord.o_orderkey as o_orderkey, t.ord.o_totalprice as o_totalprice FROM UNNEST(customer.c_orders) t(ord) WHERE t.ord.o_totalprice > 100000 LIMIT 2) " +
      "orders";
    runAndLog(sql);
  }

  /***********************************************************************************************
   Test where multiple files are used to trigger schema change for Lateral and Unnest
   **********************************************************************************************/

  @Test
  public void testSchemaChangeOnNonUnnestColumn() throws Exception {

    try {
      dirTestWatcher.copyResourceToRoot(Paths.get("lateraljoin", "multipleFiles", schemaChangeFile_1));

      String sql = "SELECT customer.c_name, customer.c_address, orders.o_orderkey, orders.o_totalprice " +
        "FROM dfs.`lateraljoin/multipleFiles` customer, LATERAL " +
        "(SELECT t.ord.o_orderkey as o_orderkey, t.ord.o_totalprice as o_totalprice FROM UNNEST (customer.c_orders) t(ord)) orders";
      runAndLog(sql);
    } catch (Exception ex) {
      fail();
    } finally {
      dirTestWatcher.removeFileFromRoot(Paths.get("lateraljoin", "multipleFiles", schemaChangeFile_1));
    }
  }

  /**
   * This test is different than {@link TestE2EUnnestAndLateral#testSchemaChangeOnNonUnnestColumn()} because with
   * multilevel when the first Lateral see's a schema change it creates a new batch with new vector references. Hence
   * the second lateral will receive a new incoming with new vector references with OK_NEW_SCHEMA outcome. Now even
   * though there is schema change for non-unnest column the second Unnest has to again setup it's transfer pairs since
   * vector reference for unnest field has changed for second Unnest.
   * Whereas in other test since there is only 1 Lateral followed by Scan, the incoming for lateral which has
   * schema change will be handled by Scan in such a way that it only updates vector of affected column. Hence in this
   * case vector corresponding to unnest field will not be affected and it will work fine.
   * @throws Exception
   */
  @Test
  public void testSchemaChangeOnNonUnnestColumn_InMultilevelCase() throws Exception {

    try {
      dirTestWatcher.copyResourceToRoot(Paths.get("lateraljoin", "multipleFiles", schemaChangeFile_1));
      String sql = "SELECT customer.c_custkey, customer.c_name, customer.c_nationkey, orders.orderkey, " +
        "orders.totalprice, olineitems.l_partkey, olineitems.l_linenumber, olineitems.l_quantity " +
        "FROM dfs.`lateraljoin/multipleFiles` customer, " +
        "LATERAL (SELECT t1.o.o_orderkey as orderkey, t1.o.o_totalprice as totalprice, t1.o.o_lineitems as lineitems " +
        "FROM UNNEST(customer.c_orders) t1(o)) orders, " +
        "LATERAL (SELECT t2.l.l_partkey as l_partkey, t2.l.l_linenumber as l_linenumber, t2.l.l_quantity as l_quantity " +
        "FROM UNNEST(orders.lineitems) t2(l)) olineitems";
      runAndLog(sql);
    } catch (Exception ex) {
      fail();
    } finally {
      dirTestWatcher.removeFileFromRoot(Paths.get("lateraljoin", "multipleFiles", schemaChangeFile_1));
    }
  }

  @Test
  public void testSchemaChangeOnUnnestColumn() throws Exception {
    try {
      dirTestWatcher.copyResourceToRoot(Paths.get("lateraljoin", "multipleFiles", schemaChangeFile_2));

      String sql = "SELECT customer.c_name, customer.c_address, orders.o_orderkey, orders.o_totalprice " +
        "FROM dfs.`lateraljoin/multipleFiles` customer, LATERAL " +
        "(SELECT t.ord.o_orderkey as o_orderkey, t.ord.o_totalprice as o_totalprice FROM UNNEST(customer.c_orders) t(ord)) orders";
      runAndLog(sql);
    } catch (Exception ex) {
      fail();
    } finally {
      dirTestWatcher.removeFileFromRoot(Paths.get("lateraljoin", "multipleFiles", schemaChangeFile_2));
    }
  }

  @Test
  public void testSchemaChangeOnUnnestColumn_InMultilevelCase() throws Exception {
    try {
      dirTestWatcher.copyResourceToRoot(Paths.get("lateraljoin", "multipleFiles", schemaChangeFile_2));

      String sql = "SELECT customer.c_custkey, customer.c_name, customer.c_nationkey, orders.orderkey, " +
        "orders.totalprice, orders.spriority, olineitems.l_partkey, olineitems.l_linenumber, olineitems.l_quantity " +
        "FROM dfs.`lateraljoin/multipleFiles` customer, " +
        "LATERAL (SELECT t1.o.o_orderkey as orderkey, t1.o.o_totalprice as totalprice, t1.o.o_lineitems as lineitems," +
        " t1.o.o_shippriority as spriority FROM UNNEST(customer.c_orders) t1(o)) orders, " +
        "LATERAL (SELECT t2.l.l_partkey as l_partkey, t2.l.l_linenumber as l_linenumber, t2.l.l_quantity as l_quantity " +
        "FROM UNNEST(orders.lineitems) t2(l)) olineitems";
      runAndLog(sql);
    } catch (Exception ex) {
      fail();
    } finally {
      dirTestWatcher.removeFileFromRoot(Paths.get("lateraljoin", "multipleFiles", schemaChangeFile_2));
    }
  }

  @Test
  public void testSchemaChangeOnMultipleColumns() throws Exception {
    try {
      dirTestWatcher.copyResourceToRoot(Paths.get("lateraljoin", "multipleFiles", schemaChangeFile_3));

      String sql = "SELECT customer.c_name, customer.c_address, customer.c_nationkey, orders.o_orderkey, " +
        "orders.o_totalprice FROM dfs.`lateraljoin/multipleFiles` customer, LATERAL " +
        "(SELECT t.ord.o_orderkey as o_orderkey, t.ord.o_totalprice as o_totalprice, t.ord.o_shippriority o_shippriority FROM UNNEST(customer.c_orders) t(ord)) orders";

      runAndLog(sql);
    } catch (Exception ex) {
      fail();
    } finally {
      dirTestWatcher.removeFileFromRoot(Paths.get("lateraljoin", "multipleFiles", schemaChangeFile_3));
    }
  }

  /*****************************************************************************************
   Test where Limit/Filter/Agg/Sort operator are used in parent query
   *****************************************************************************************/

  @Test
  public void testMultipleBatchesLateral_WithLimitInParent() throws Exception {
    String sql = "SELECT customer.c_name, customer.c_address, orders.o_orderkey, orders.o_totalprice " +
      "FROM dfs.`lateraljoin/multipleFiles` customer, LATERAL " +
      "(SELECT t.ord.o_orderkey as o_orderkey, t.ord.o_totalprice  as o_totalprice FROM UNNEST(customer.c_orders) t(ord) WHERE t.ord.o_totalprice > 100000 LIMIT 2) " +
      "orders LIMIT 1";
    runAndLog(sql);
  }

  @Test
  public void testMultipleBatchesLateral_WithFilterInParent() throws Exception {
    String sql = "SELECT customer.c_name, customer.c_address, orders.o_orderkey, orders.o_totalprice " +
      "FROM dfs.`lateraljoin/multipleFiles` customer, LATERAL " +
      "(SELECT t.ord.o_orderkey as o_orderkey, t.ord.o_totalprice as o_totalprice FROM UNNEST(customer.c_orders) t(ord) WHERE t.ord.o_totalprice > 100000 LIMIT 2) " +
      "orders WHERE orders.o_totalprice > 240000";
    runAndLog(sql);
  }

  @Test
  public void testMultipleBatchesLateral_WithGroupByInParent() throws Exception {
    String sql = "SELECT customer.c_name, avg(orders.o_totalprice) AS avgPrice " +
      "FROM dfs.`lateraljoin/multipleFiles` customer, LATERAL " +
      "(SELECT t.ord.o_totalprice as o_totalprice FROM UNNEST(customer.c_orders) t(ord) WHERE t.ord.o_totalprice > 100000 LIMIT 2) " +
      "orders GROUP BY customer.c_name";
    runAndLog(sql);
  }

  @Test
  public void testMultipleBatchesLateral_WithOrderByInParent() throws Exception {
    String sql = "SELECT customer.c_name, customer.c_address, orders.o_orderkey, orders.o_totalprice " +
      "FROM dfs.`lateraljoin/multipleFiles` customer, LATERAL " +
      "(SELECT t.ord.o_orderkey as o_orderkey, t.ord.o_totalprice as o_totalprice FROM UNNEST(customer.c_orders) t(ord)) orders " +
      "ORDER BY orders.o_orderkey";
    runAndLog(sql);
  }

  @Test
  public void testMultipleBatchesLateral_WithHashAgg() throws Exception {
    String sql = "SELECT t2.maxprice FROM (SELECT customer.c_orders AS c_orders FROM "
      + "dfs.`lateraljoin/multipleFiles/` customer) t1, LATERAL (SELECT CAST(MAX(t.ord.o_totalprice)"
      + " AS int) AS maxprice FROM UNNEST(t1.c_orders) t(ord) GROUP BY t.ord.o_orderstatus) t2";

    try {
    testBuilder()
      .optionSettingQueriesForTestQuery("alter session set `%s` = false",
        PlannerSettings.STREAMAGG.getOptionName())
      .sqlQuery(sql)
      .unOrdered()
      .baselineColumns("maxprice")
      .baselineValues(367190)
      .baselineValues(316347)
      .baselineValues(146610)
      .baselineValues(306996)
      .baselineValues(235695)
      .baselineValues(177819)
      .build().run();
    } finally {
      runAndLog("alter session set `" + PlannerSettings.STREAMAGG.getOptionName() + "` = true");
    }
  }

  @Test
  public void testLateral_HashAgg_with_nulls() throws Exception {
    String sql = "SELECT key, t3.dsls FROM cp.`lateraljoin/with_nulls.json` t LEFT OUTER "
    + "JOIN LATERAL (SELECT DISTINCT t2.sls AS dsls FROM UNNEST(t.sales) t2(sls)) t3 ON TRUE";

    try {
    testBuilder()
      .optionSettingQueriesForTestQuery("alter session set `%s` = false",
        PlannerSettings.STREAMAGG.getOptionName())
      .sqlQuery(sql)
      .unOrdered()
      .baselineColumns("key","dsls")
      .baselineValues("aa",null)
      .baselineValues("bb",100L)
      .baselineValues("bb",200L)
      .baselineValues("bb",300L)
      .baselineValues("bb",400L)
      .baselineValues("cc",null)
      .baselineValues("dd",111L)
      .baselineValues("dd",222L)
      .build().run();
    } finally {
      runAndLog("alter session set `" + PlannerSettings.STREAMAGG.getOptionName() + "` = true");
    }
  }

  @Test
  public void testMultipleBatchesLateral_WithStreamingAgg() throws Exception {
    String sql = "SELECT t2.maxprice FROM (SELECT customer.c_orders AS c_orders FROM "
        + "dfs.`lateraljoin/multipleFiles/` customer) t1, LATERAL (SELECT CAST(MAX(t.ord.o_totalprice)"
        + " AS int) AS maxprice FROM UNNEST(t1.c_orders) t(ord) GROUP BY t.ord.o_orderstatus) t2";

    testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("maxprice")
        .baselineValues(367190)
        .baselineValues(316347)
        .baselineValues(146610)
        .baselineValues(306996)
        .baselineValues(235695)
        .baselineValues(177819)
        .build().run();
  }

  @Test
  public void testLateral_StreamingAgg_with_nulls() throws Exception {
    String sql = "SELECT key, t3.dsls FROM cp.`lateraljoin/with_nulls.json` t LEFT OUTER "
        + "JOIN LATERAL (SELECT DISTINCT t2.sls AS dsls FROM UNNEST(t.sales) t2(sls)) t3 ON TRUE";

    testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("key","dsls")
        .baselineValues("aa",null)
        .baselineValues("bb",100L)
        .baselineValues("bb",200L)
        .baselineValues("bb",300L)
        .baselineValues("bb",400L)
        .baselineValues("cc",null)
        .baselineValues("dd",111L)
        .baselineValues("dd",222L)
        .build().run();
  }

  @Test
  public void testMultipleBatchesLateral_WithStreamingAggNoGroup() throws Exception {
    String sql = "SELECT t2.maxprice FROM (SELECT customer.c_orders AS c_orders FROM "
        + "dfs.`lateraljoin/multipleFiles/` customer) t1, LATERAL (SELECT CAST(MAX(t.ord.o_totalprice)"
        + " AS int) AS maxprice FROM UNNEST(t1.c_orders) t(ord) ) t2";

    testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("maxprice")
        .baselineValues(367190)
        .baselineValues(306996)
        .build().run();
  }

  @Test
  public void testUnnestNestedStarSubquery() throws Exception {
    String sql = "select t2.o.o_id o_id\n" +
        "from (select * from cp.`lateraljoin/nested-customer.json` limit 1) t,\n" +
        "unnest(t.orders) t2(o)";

    testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("o_id")
        .baselineValues(1L)
        .baselineValues(2L)
        .baselineValues(3L)
        .go();
  }

  @Test
  public void testLateralWithComplexProject() throws Exception {
    String sql = "select l.name from cp.`lateraljoin/nested-customer.parquet` c,\n" +
        "lateral (select u.item.i_name as name from unnest(c.orders[0].items) as u(item)) l limit 1";

    testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("name")
        .baselineValues("paper towel")
        .go();
  }

  @Test
  public void testLateralWithAgg() throws Exception {
    String sql = "select l.name from cp.`lateraljoin/nested-customer.parquet` c,\n" +
        "lateral (select max(u.item.i_name) as name from unnest(c.orders[0].items) as u(item)) l limit 1";

    testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("name")
        .baselineValues("paper towel")
        .go();
  }

  @Test
  public void testMultiLateralWithComplexProject() throws Exception {
    String sql = "select l1.name, l2.name as name2 from cp.`lateraljoin/nested-customer.parquet` c,\n" +
      "lateral (select u.item.i_name as name from unnest(c.orders[0].items) as u(item)) l1," +
      "lateral (select u.item.i_name as name from unnest(c.orders[0].items) as u(item)) l2 limit 1";

    testBuilder()
      .sqlQuery(sql)
      .unOrdered()
      .baselineColumns("name", "name2")
      .baselineValues("paper towel", "paper towel")
      .go();
  }
}
