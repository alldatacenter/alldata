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
package com.mapr.drill.maprdb.tests.index;

import com.mapr.tests.annotations.ClusterTest;
import org.apache.drill.PlanTestBase;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;


@Category(ClusterTest.class)
public class StatisticsTest extends IndexPlanTest {
  /**
   *  A sample row of this 10K table:
   ------------------+-----------------------------+--------+
   | 1012  | {"city":"pfrrs","state":"pc"}  | {"email":"KfFzKUZwNk@gmail.com","phone":"6500005471"}  |
   {"ssn":"100007423"}  | {"fname":"KfFzK","lname":"UZwNk"}  | {"age":53.0,"income":45.0}  | 1012   |
   *
   * This test suite generate random content to fill all the rows, since the random function always start from
   * the same seed for different runs, when the row count is not changed, the data in table will always be the same,
   * thus the query result could be predicted and verified.
   */

  @Test
  @Ignore("Currently untested; re-enable after stats/costing integration complete")
  public void testFilters() throws Exception {
    String query;
    String explain = "explain plan including all attributes for ";

    // Top-level ANDs - Leading columns (personal.age), (address.state)
    query = "select * from hbase.`index_test_primary` t "
        + " where (t.personal.age < 30 or t.personal.age > 100)"
        + " and (t.address.state = 'mo' or t.address.state = 'ca')";
    PlanTestBase.testPlanMatchingPatterns(explain+query,
        new String[] {".*JsonTableGroupScan.*tableName=.*index_test_primary.*rows=10000"}
    );

    // Top-level ORs - Cannot split top-level ORs so use defaults
    query = "select * from hbase.`index_test_primary` t "
        + " where (t.personal.age > 30 and t.personal.age < 100)"
        + " or (t.address.state = 'mo')";
    PlanTestBase.testPlanMatchingPatterns(explain+query,
        new String[] {".*JsonTableGroupScan.*tableName=.*index_test_primary.*rows=10000"}
    );

    // ANDed condition - Leading index column(personal.age) and non-leading column(address.city)
    query = "select * from hbase.`index_test_primary` t "
        + " where (t.personal.age < 30 or t.personal.age > 100)"
        + " and `address.city` = 'sf'";
    PlanTestBase.testPlanMatchingPatterns(explain+query,
        new String[] {".*JsonTableGroupScan.*tableName=.*index_test_primary.*rows=10000"}
    );

    // ANDed condition - Leading index columns (address.state) and (address.city)
    query = "select * from hbase.`index_test_primary` t "
        + " where (`address.state` = 'mo' or `address.state` = 'ca') " // Leading index column
        + " and `address.city` = 'sf'";                                // Non leading index column
    PlanTestBase.testPlanMatchingPatterns(explain+query,
        new String[] {".*JsonTableGroupScan.*tableName=.*index_test_primary.*rows=10000"}
    );

    // ANDed condition - Leading index columns (address.state) and non-index column (name.fname)
    query = "select * from hbase.`index_test_primary` t "
        + " where (`address.state` = 'mo' or `address.state` = 'ca') " // Leading index column
        + " and `name.fname` = 'VcFahj'";                              // Non index column
    PlanTestBase.testPlanMatchingPatterns(explain+query,
        new String[] {".*JsonTableGroupScan.*tableName=.*index_test_primary.*rows=10000"}
    );

    // Simple condition - LIKE predicate
    query = "select t._id as rowid from hbase.`index_test_primary` as t "
        + "where t.driverlicense like '100007423%'";
    PlanTestBase.testPlanMatchingPatterns(explain+query,
        new String[] {".*JsonTableGroupScan.*tableName=.*index_test_primary.*rows=10000"}
    );

    // Simple condition - LIKE predicate with ESCAPE clause
    query = "select t._id as rowid from hbase.`index_test_primary` as t "
        + "where t.driverlicense like '100007423%' ESCAPE '/'";
    PlanTestBase.testPlanMatchingPatterns(explain+query,
        new String[] {".*JsonTableGroupScan.*tableName=.*index_test_primary.*rows=10000"}
    );
  }
}
