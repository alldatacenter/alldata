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
package org.apache.drill.exec.store.mongo;

import org.apache.drill.categories.MongoStorageTest;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.exec.ExecConstants;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;

@Category({SlowTest.class, MongoStorageTest.class})
public class TestMongoQueries extends MongoTestBase {

  @Test
  public void testBooleanFilter() throws Exception {
    testBuilder()
        .sqlQuery(String.format(TEST_BOOLEAN_FILTER_QUERY_TEMPLATE1, EMPLOYEE_DB, EMPINFO_COLLECTION))
        .unOrdered()
        .expectsNumRecords(11)
        .go();

    testBuilder()
        .sqlQuery(String.format(TEST_BOOLEAN_FILTER_QUERY_TEMPLATE2, EMPLOYEE_DB, EMPINFO_COLLECTION))
        .unOrdered()
        .expectsNumRecords(8)
        .go();
  }

  @Test
  public void testSerDe() throws Exception {
    String plan = queryBuilder()
      .sql(String.format(TEST_BOOLEAN_FILTER_QUERY_TEMPLATE1, EMPLOYEE_DB, EMPINFO_COLLECTION))
      .explainJson();

    assertEquals(queryBuilder().physical(plan).run().recordCount(), 11);
  }

  @Test
  public void testFragmentSerDe() throws Exception {
    client.alterSession(ExecConstants.SLICE_TARGET, 1);
    try {
      String plan = queryBuilder()
        .sql(String.format("select t1.id as id, t1.name from mongo.%1$s.`%2$s` t1 where t1.name = 'Cake' union " +
          "select t2.id as id, t2.name from mongo.%1$s.`%2$s` t2 ", DONUTS_DB, DONUTS_COLLECTION))
        .explainJson();

      assertEquals(queryBuilder().physical(plan).run().recordCount(), 5);
    } finally {
      client.resetSession(ExecConstants.SLICE_TARGET);
    }
  }

  @Test
  public void testWithANDOperator() throws Exception {
    testBuilder()
        .sqlQuery(String.format(TEST_BOOLEAN_FILTER_QUERY_TEMPLATE3, EMPLOYEE_DB, EMPINFO_COLLECTION))
        .unOrdered()
        .expectsNumRecords(4)
        .go();
  }

  @Test
  public void testWithOROperator() throws Exception {
    testBuilder()
        .sqlQuery(String.format(TEST_BOOLEAN_FILTER_QUERY_TEMPLATE3, EMPLOYEE_DB, EMPINFO_COLLECTION))
        .unOrdered()
        .expectsNumRecords(4)
        .go();
  }

  @Test
  public void testResultCount() throws Exception {
    testBuilder()
        .sqlQuery(String.format(TEST_BOOLEAN_FILTER_QUERY_TEMPLATE4, EMPLOYEE_DB, EMPINFO_COLLECTION))
        .unOrdered()
        .expectsNumRecords(5)
        .go();
  }

  @Test
  public void testUnShardedDBInShardedCluster() throws Exception {
    testBuilder()
        .sqlQuery(String.format(TEST_STAR_QUERY_UNSHARDED_DB, DONUTS_DB, DONUTS_COLLECTION))
        .unOrdered()
        .expectsNumRecords(5)
        .go();
  }

  @Test
  public void testEmptyCollection() throws Exception {
    testBuilder()
        .sqlQuery(String.format(TEST_STAR_QUERY_UNSHARDED_DB, EMPLOYEE_DB, EMPTY_COLLECTION))
        .unOrdered()
        .expectsNumRecords(0)
        .go();
  }

  @Test
  @Ignore("DRILL-7428") // Query is invalid, Drill bug allows it.
  public void testUnShardedDBInShardedClusterWithProjectionAndFilter() throws Exception {
    testBuilder()
        .sqlQuery(String.format(TEST_STAR_QUERY_UNSHARDED_DB_PROJECT_FILTER, DONUTS_DB, DONUTS_COLLECTION))
        .unOrdered()
        .expectsNumRecords(2)
        .go();
  }

  @Test
  @Ignore("DRILL-7428") // Query is invalid, Drill bug allows it.
  public void testUnShardedDBInShardedClusterWithGroupByProjectionAndFilter() throws Exception {
    testBuilder()
        .sqlQuery(String.format(TEST_STAR_QUERY_UNSHARDED_DB_GROUP_PROJECT_FILTER, DONUTS_DB, DONUTS_COLLECTION))
        .unOrdered()
        .expectsNumRecords(5)
        .go();
  }

  @Test
  public void testCountColumnPushDown() throws Exception {
    String query = "select count(t.name) as c from mongo.%s.`%s` t";

    queryBuilder()
        .sql(query, DONUTS_DB, DONUTS_COLLECTION)
        .planMatcher()
        .exclude("Agg\\(")
        .include("MongoGroupScan.*group")
        .match();

    testBuilder()
        .sqlQuery(query, DONUTS_DB, DONUTS_COLLECTION)
        .unOrdered()
        .baselineColumns("c")
        .baselineValues(5)
        .go();
  }

  @Test
  public void testSumColumnPushDown() throws Exception {
    String query = "select sum(t.sales) as s from mongo.%s.`%s` t";

    queryBuilder()
        .sql(query, DONUTS_DB, DONUTS_COLLECTION)
        .planMatcher()
        .exclude("Agg\\(")
        .include("MongoGroupScan.*group")
        .match();

    testBuilder()
        .sqlQuery(query, DONUTS_DB, DONUTS_COLLECTION)
        .unOrdered()
        .baselineColumns("s")
        .baselineValues(1194)
        .go();
  }

  @Test
  public void testCountGroupByPushDown() throws Exception {
    String query = "select count(t.id) as c, t.type from mongo.%s.`%s` t group by t.type";

    queryBuilder()
        .sql(query, DONUTS_DB, DONUTS_COLLECTION)
        .planMatcher()
        .exclude("Agg\\(")
        .include("MongoGroupScan.*group")
        .match();

    testBuilder()
        .sqlQuery(query, DONUTS_DB, DONUTS_COLLECTION)
        .unOrdered()
        .baselineColumns("c", "type")
        .baselineValues(5, "donut")
        .go();
  }

  @Test
  public void testSumGroupByPushDown() throws Exception {
    String query = "select sum(t.sales) s, t.type from mongo.%s.`%s` t group by t.type";

    queryBuilder()
        .sql(query, DONUTS_DB, DONUTS_COLLECTION)
        .planMatcher()
        .exclude("Agg\\(")
        .include("MongoGroupScan.*group")
        .match();

    testBuilder()
        .sqlQuery(query, DONUTS_DB, DONUTS_COLLECTION)
        .unOrdered()
        .baselineColumns("s", "type")
        .baselineValues(1194, "donut")
        .go();
  }

  @Test
  public void testCountColumnPushDownWithFilter() throws Exception {
    String query = "select count(t.id) as c from mongo.%s.`%s` t where t.name = 'Cake'";

    queryBuilder()
        .sql(query, DONUTS_DB, DONUTS_COLLECTION)
        .planMatcher()
        .exclude("Agg\\(", "Filter")
        .include("MongoGroupScan.*group")
        .match();

    testBuilder()
        .sqlQuery(query, DONUTS_DB, DONUTS_COLLECTION)
        .unOrdered()
        .baselineColumns("c")
        .baselineValues(1)
        .go();
  }

  @Test
  public void testUnionAll() throws Exception {
    String query = "select t1.id as id, t1.name from mongo.%1$s.`%2$s` t1 where t1.name = 'Cake' union all " +
        "select t2.id as id, t2.name from mongo.%1$s.`%2$s` t2";

    queryBuilder()
        .sql(query, DONUTS_DB, DONUTS_COLLECTION)
        .planMatcher()
        .exclude("UnionAll\\(")
        .include("MongoGroupScan.*\\$unionWith")
        .match();

    testBuilder()
        .sqlQuery(query, DONUTS_DB, DONUTS_COLLECTION)
        .unOrdered()
        .baselineColumns("id", "name")
        .baselineValues("0001", "Cake")
        .baselineValues("0001", "Cake")
        .baselineValues("0002", "Raised")
        .baselineValues("0003", "Old Fashioned")
        .baselineValues("0004", "Filled")
        .baselineValues("0005", "Apple Fritter")
        .go();
  }

  @Test
  public void testUnionDistinct() throws Exception {
    String query = "select t1.id as id, t1.name from mongo.%1$s.`%2$s` t1 where t1.name = 'Cake' union " +
        "select t2.id as id, t2.name from mongo.%1$s.`%2$s` t2 ";

    queryBuilder()
        .sql(query, DONUTS_DB, DONUTS_COLLECTION)
        .planMatcher()
        .exclude("UnionAll\\(", "Agg\\(")
        .include("MongoGroupScan.*\\$unionWith")
        .match();

    testBuilder()
        .sqlQuery(query, DONUTS_DB, DONUTS_COLLECTION)
        .unOrdered()
        .baselineColumns("id", "name")
        .baselineValues("0001", "Cake")
        .baselineValues("0002", "Raised")
        .baselineValues("0003", "Old Fashioned")
        .baselineValues("0004", "Filled")
        .baselineValues("0005", "Apple Fritter")
        .go();
  }

  @Test
  public void testProjectPushDown() throws Exception {
    String query = "select t.sales * t.sales as c, t.name from mongo.%s.`%s` t";

    queryBuilder()
        .sql(query, DONUTS_DB, DONUTS_COLLECTION)
        .planMatcher()
        .include("MongoGroupScan.*project.*multiply")
        .match();

    testBuilder()
        .sqlQuery(query, DONUTS_DB, DONUTS_COLLECTION)
        .unOrdered()
        .baselineColumns("c", "name")
        .baselineValues(196, "Filled")
        .baselineValues(1225, "Cake")
        .baselineValues(21025, "Raised")
        .baselineValues(90000, "Old Fashioned")
        .baselineValues(490000, "Apple Fritter")
        .go();
  }

  @Test
  public void testProjectPushDownWithCase() throws Exception {
    String query = "select case when t.sales >= 700 then 2 when t.sales > 145 then 1 else 0 end as c, t.name from mongo.%s.`%s` t";

    queryBuilder()
      .sql(query, DONUTS_DB, DONUTS_COLLECTION)
      .planMatcher()
      .include("MongoGroupScan.*project.*cond.*\\$gt")
      .match();

    testBuilder()
      .sqlQuery(query, DONUTS_DB, DONUTS_COLLECTION)
      .unOrdered()
      .baselineColumns("c", "name")
      .baselineValues(0, "Filled")
      .baselineValues(0, "Cake")
      .baselineValues(0, "Raised")
      .baselineValues(1, "Old Fashioned")
      .baselineValues(2, "Apple Fritter")
      .go();
  }
}
