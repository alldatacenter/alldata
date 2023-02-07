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
package org.apache.drill.exec.nested;

import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.test.BaseTestQuery;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class TestFastComplexSchema extends BaseTestQuery {

  @Test
  public void test() throws Exception {
    test("SELECT r.r_name, \n" +
            "       t1.f \n" +
            "FROM   cp.`tpch/region.parquet` r \n" +
            "       JOIN (SELECT flatten(x) AS f \n" +
            "             FROM   (SELECT Convert_from('[0, 1]', 'json') AS x \n" +
            "                     FROM   cp.`tpch/region.parquet`)) t1 \n" +
            "         ON t1.f = cast(r.r_regionkey as bigint)");
  }

  @Test
  public void test2() throws Exception {
    test("alter session set `planner.enable_hashjoin` = false");
    test("alter session set `planner.slice_target` = 1");
    try {
      test("SELECT r.r_name, \n" +
              "       t1.f \n" +
              "FROM   cp.`tpch/region.parquet` r \n" +
              "       JOIN (SELECT Flatten(x) AS f \n" +
              "             FROM   (SELECT Convert_from('[0, 1]', 'json') AS x \n" +
              "                     FROM   cp.`tpch/region.parquet`)) t1 \n" +
              "         ON t1.f = cast(r.r_regionkey as bigint) \n" +
              "ORDER  BY r.r_name");
    } finally {
      test("alter session reset `planner.enable_hashjoin`");
      test("alter session reset `planner.slice_target`");
    }
  }

  @Test
  public void test3() throws Exception {
    test("alter session set `planner.enable_hashjoin` = false");
    test("alter session set `planner.slice_target` = 1");
    try {
      test("select f from\n" +
              "(select convert_from(nation, 'json') as f from\n" +
              "(select concat('{\"name\": \"', n.n_name, '\", ', '\"regionkey\": ', r.r_regionkey, '}') as nation\n" +
              "       from cp.`tpch/nation.parquet` n,\n" +
              "            cp.`tpch/region.parquet` r\n" +
              "        where \n" +
              "        n.n_regionkey = r.r_regionkey)) t\n" +
              "order by t.f.name");
    } finally {
      test("alter session reset `planner.enable_hashjoin`");
      test("alter session reset `planner.slice_target`");
    }
  }

  @Test
  public void test4() throws Exception {
    test("alter session set `planner.enable_hashjoin` = false");
    test("alter session set `planner.slice_target` = 1");
    try {
      test("SELECT f \n" +
              "FROM   (SELECT Convert_from(nation, 'json') AS f \n" +
              "        FROM   (SELECT Concat('{\"name\": \"', n.n_name, '\", ', '\"regionkey\": ', \n" +
              "                       r.r_regionkey, \n" +
              "                               '}') AS \n" +
              "                       nation \n" +
              "                FROM   cp.`tpch/nation.parquet` n, \n" +
              "                       cp.`tpch/region.parquet` r \n" +
              "                WHERE  n.n_regionkey = r.r_regionkey \n" +
              "                       AND r.r_regionkey = 4)) t \n" +
              "ORDER  BY t.f.name");
    } finally {
      test("alter session reset `planner.enable_hashjoin`");
      test("alter session reset `planner.slice_target`");
    }
  }

  @Test //DRILL-4783 when resultset is empty, don't throw exception.
  @Category(UnlikelyTest.class)
  public void test5() throws Exception {

    // when there is no incoming record, flatten won't throw exception
    testBuilder().sqlQuery("select flatten(j) from \n" +
           " (select convert_from(names, 'json') j \n" +
           " from (select concat('[\"', first_name, '\", ', '\"', last_name, '\"]') names \n" +
           " from cp.`employee.json` where store_id=9999))")
        .expectsEmptyResultSet()
        .build().run();

    // result is not empty and is list type,
    testBuilder().sqlQuery("select flatten(j) n from \n" +
        " (select convert_from(names, 'json') j \n" +
        " from (select concat('[\"', first_name, '\", ', '\"', last_name, '\"]') names \n" +
        " from cp.`employee.json` where first_name='Sheri'))")
        .unOrdered()
        .baselineColumns("n")
        .baselineValues("Sheri")
        .baselineValues("Nowmer")
        .build().run();

    // result is not empty, and flatten got incompatible (non-list) incoming records. got exception thrown
    errorMsgTestHelper("select flatten(first_name) from \n" +
        "(select first_name from cp.`employee.json` where first_name='Sheri')",
        "Flatten does not support inputs of non-list values");
  }
}
