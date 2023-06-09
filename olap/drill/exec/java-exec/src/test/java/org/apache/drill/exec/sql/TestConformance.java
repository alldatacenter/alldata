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
package org.apache.drill.exec.sql;

import org.apache.drill.categories.SqlTest;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertTrue;

@Category(SqlTest.class)
public class TestConformance extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    startCluster(builder);
  }

  @Test
  public void testApply() throws Exception{
    //cross join is not support yet in Drill: DRILL-1921, so we are testing OUTER APPLY only
    String query = "SELECT c.c_nationkey, o.orderdate from " +
      "cp.`tpch/customer.parquet` c outer apply " +
      "cp.`tpch/orders.parquet` o " +
      "where c.c_custkey = o.o_custkey";

    String plan = queryBuilder().sql(query).explainText();
    assertTrue(plan.contains("Join(condition="));
  }

  @Test
  public void testGroupByWithPositionalAlias() throws Exception {
    testBuilder()
        .sqlQuery("select length(n_name), n_regionkey from cp.`tpch/nation.parquet` group by 1, 2")
        .unOrdered()
        .sqlBaselineQuery("select length(n_name), n_regionkey from cp.`tpch/nation.parquet` group by length(n_name), n_regionkey")
        .go();
  }

  @Test
  public void testGroupByWithNamedAlias() throws Exception {
    testBuilder()
        .sqlQuery("select length(n_name) as len, n_regionkey as key from cp.`tpch/nation.parquet` group by len, key")
        .unOrdered()
        .sqlBaselineQuery("select length(n_name) as len, n_regionkey as key from cp.`tpch/nation.parquet` group by length(n_name), n_regionkey")
        .go();
  }

  @Test
  public void testHavingWithNamedAlias() throws Exception {
    testBuilder()
        .sqlQuery("select length(n_name) as len, count(*) as cnt from cp.`tpch/nation.parquet` " +
            "group by length(n_name) having cnt > 1")
        .unOrdered()
        .sqlBaselineQuery("select length(n_name) as len, count(*) as cnt from cp.`tpch/nation.parquet` " +
            "group by length(n_name) having count(*) > 1")
        .go();
  }

  @Test
  public void testOrderWithPositionalAlias() throws Exception {
    testBuilder()
        .sqlQuery("select n_regionkey, n_name from cp.`tpch/nation.parquet` order by 1, 2")
        .unOrdered()
        .sqlBaselineQuery("select n_regionkey, n_name from cp.`tpch/nation.parquet` order by n_regionkey, n_name")
        .go();
  }

  @Test
  public void testOrderWithNamedAlias() throws Exception {
    testBuilder()
        .sqlQuery("select n_regionkey as r, n_name as n from cp.`tpch/nation.parquet` order by r, n")
        .unOrdered()
        .sqlBaselineQuery("select n_regionkey as r, n_name as n from cp.`tpch/nation.parquet` order by n_regionkey, n_name")
        .go();
  }

}
