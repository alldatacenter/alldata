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
import org.apache.drill.test.BaseTestQuery;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(PlannerTest.class)
public class TestSelectivity extends BaseTestQuery {
    static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestSelectivity.class);

    @Test
    public void testFilterSelectivityOptions() throws Exception {

        /* Tests to check setting options works as expected */
        test(String.format("alter system set `planner.filter.min_selectivity_estimate_factor` = %f", 0.25));
        test(String.format("alter system set `planner.filter.max_selectivity_estimate_factor` = %f", 0.75));

        String query1 = String.format("alter session set `planner.filter.min_selectivity_estimate_factor` = %f", -0.5);
        String errMsg1 = "Option planner.filter.min_selectivity_estimate_factor must be between 0.000000 and 1.000000";
        BaseTestQuery.errorMsgTestHelper(query1, errMsg1);

        String query2 = String.format("alter session set `planner.filter.min_selectivity_estimate_factor` = %f", 0.85);
        String errMsg2 = "Option planner.filter.min_selectivity_estimate_factor must be less than or equal to"
                + " Option planner.filter.max_selectivity_estimate_factor";
        BaseTestQuery.errorMsgTestHelper(query2, errMsg2);

        String query3 = String.format("alter session set `planner.filter.max_selectivity_estimate_factor` = %f", 1.5);
        String errMsg3 = "Option planner.filter.max_selectivity_estimate_factor must be between 0.000000 and 1.000000";
        BaseTestQuery.errorMsgTestHelper(query3, errMsg3);

        String query4 = String.format("alter session set `planner.filter.max_selectivity_estimate_factor` = %f", 0.15);
        String errMsg4 = "Option planner.filter.max_selectivity_estimate_factor must be greater than or equal to"
                + " Option planner.filter.min_selectivity_estimate_factor";
        BaseTestQuery.errorMsgTestHelper(query4, errMsg4);

        test(String.format("alter session set `planner.filter.max_selectivity_estimate_factor` = %f", 1.0));
        test(String.format("alter session set `planner.filter.min_selectivity_estimate_factor` = %f", 0.9));
        /* End of tests to check setting options */

        /* Capping the selectivity prevents underestimation of filtered rows */
        String query = " select employee_id from cp.`employee.json` where employee_id < 10 and department_id > 5";

        test(String.format("alter session set `planner.filter.min_selectivity_estimate_factor` = %f", 0.1));
        final String[] expectedPlan1 = {"Filter\\(condition.*\\).*rowcount = 115.75,.*",
                            "Scan.*columns=\\[`employee_id`, `department_id`\\].*rowcount = 463.0.*"};
        PlanTestBase.testPlanWithAttributesMatchingPatterns(query, expectedPlan1, new String[]{});

        test(String.format("alter session set `planner.filter.min_selectivity_estimate_factor` = %f", 0.7));
        final String[] expectedPlan2 = {"Filter\\(condition.*\\).*rowcount = 324.0.*",
                            "Scan.*columns=\\[`employee_id`, `department_id`\\].*rowcount = 463.0.*"};
        PlanTestBase.testPlanWithAttributesMatchingPatterns(query, expectedPlan2, new String[]{});
    }
}
