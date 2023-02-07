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

import org.apache.drill.categories.OperatorTest;
import org.apache.drill.categories.SqlTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({SqlTest.class, OperatorTest.class})
public class TestCorrelation extends PlanTestBase {

  @Test  // DRILL-2962
  public void testScalarAggCorrelatedSubquery() throws Exception {
    String query = "select count(*) as cnt from cp.`tpch/nation.parquet` n1 "
      + " where n1.n_nationkey  > (select avg(n2.n_regionkey) * 4 from cp.`tpch/nation.parquet` n2 "
      + " where n1.n_regionkey = n2.n_nationkey)";

    testBuilder()
      .sqlQuery(query)
      .ordered()
      .baselineColumns("cnt")
      .baselineValues((long) 17)
      .build()
      .run();
  }

  @Test  // DRILL-2949
  public void testScalarAggAndFilterCorrelatedSubquery() throws Exception {
    String query = "select count(*) as cnt from cp.`tpch/nation.parquet` n1, "
      + " cp.`tpch/region.parquet` r1 where n1.n_regionkey = r1.r_regionkey and "
      + " r1.r_regionkey < 3 and "
      + " n1.n_nationkey  > (select avg(n2.n_regionkey) * 4 from cp.`tpch/nation.parquet` n2 "
      + " where n1.n_regionkey = n2.n_nationkey)";

    testBuilder()
      .sqlQuery(query)
      .ordered()
      .baselineColumns("cnt")
      .baselineValues((long) 11)
      .build()
      .run();
  }

  @Test
  public void testExistsScalarSubquery() throws Exception {
    String query =
        "SELECT employee_id\n" +
        "FROM cp.`employee.json`\n" +
        "WHERE EXISTS\n" +
        "    (SELECT *\n" +
        "     FROM cp.`employee.json` cs2\n" +
        "     )\n" +
        "limit 1";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("employee_id")
        .baselineValues(1L)
        .go();
  }

  @Test
  public void testSeveralExistsCorrelateSubquery() throws Exception {
    String query =
        "SELECT cs1.employee_id\n" +
        "FROM cp.`employee.json` cs1,\n" +
        "     cp.`employee.json` cs3\n" +
        "WHERE cs1.hire_date = cs3.hire_date\n" +
        "  AND EXISTS\n" +
        "    (SELECT *\n" +
        "     FROM cp.`employee.json` cs2\n" +
        "     WHERE " +
        "       cs1.position_id > cs2.position_id\n" +
        "       AND" +
        "       cs1.epmloyee_id = cs2.epmloyee_id" +
        "       )\n" +
        "  AND EXISTS\n" +
        "    (SELECT *\n" +
        "     FROM cp.`employee.json` cr1\n" +
        "     WHERE cs1.position_id = cr1.position_id)\n" +
        "LIMIT 1";

    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .expectsEmptyResultSet()
      .go();
  }

  @Test // DRILL-7050
  public void testCorrelatedSubQueryInSelect() throws Exception {
    String tableName = "dfs.tmp.source";
    String query =
        "select t1.id,\n" +
            "(select count(t2.id)\n" +
            "from %1$s t2 where t2.id = t1.id) as c\n" +
        "from %1$s t1";
    try {
      test("create table %s as (select 1 as id union all select 2 as id)", tableName);

      testBuilder()
        .sqlQuery(query, tableName)
        .unOrdered()
        .baselineColumns("id", "c")
        .baselineValues(1, 1L)
        .baselineValues(2, 1L)
        .go();
    } finally {
      test("drop table if exists %s", tableName);
    }
  }
}
