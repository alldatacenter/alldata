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

import org.apache.drill.categories.PlannerTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(PlannerTest.class)
public class TestMergeFilterPlan extends PlanTestBase {

  @Test
  public void testDRILL_FilterMerge() throws Exception {
    String expectedPattern1 = "Filter(condition=[AND(OR(=($0, 1), =($0, 2), =($0, 3)), =($4, 'GRADUATE DEGREE'))])";
    String expectedPattern2 = "Filter(condition=[AND(OR(=($0, 1), =($0, 2), =($0, 3)), LIKE($1, '%VP%'))])";
    String excludedPattern = "Filter(condition=[OR(=($0, 1), =($0, 2), =($0, 3))])";

    test("use dfs.tmp");

    test("create or replace view MyViewWithFilter as " +
      " SELECT  first_name, " +
      "         last_name, " +
      "         full_name, " +
      "         salary, " +
      "         employee_id, " +
      "         store_id, " +
      "         position_id, " +
      "         position_title, " +
      "         education_level " +
      " FROM cp.`employee.json` " +
      " WHERE position_id in (1, 2, 3 )");

    testPlanSubstrPatterns(" select dat.store_id " +
        "      , sum(dat.store_cost) as total_cost " +
        " from ( " +
        "  select store_id, position_id " +
        "            , sum(salary) as store_cost " +
        "       from MyViewWithFilter " +
        " where full_name in ( select n_name " +
        "                      from cp.`tpch/nation.parquet`) " +
        "  and  education_level = 'GRADUATE DEGREE' " +
        "  and position_id in ( select position_id " +
        "                       from MyViewWithFilter " +
        "                        where position_title like '%VP%' " +
        "                      ) " +
        "  group by store_id, position_id " +
        ") dat " +
        "group by dat.store_id " +
        "order by dat.store_id",
        new String[]{expectedPattern1, expectedPattern2}, new String[]{excludedPattern});

    test("drop view MyViewWithFilter ");
  }


}
