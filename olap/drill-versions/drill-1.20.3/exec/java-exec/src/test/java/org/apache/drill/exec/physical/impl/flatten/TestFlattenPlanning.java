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
package org.apache.drill.exec.physical.impl.flatten;

import org.apache.drill.PlanTestBase;
import org.apache.drill.categories.PlannerTest;
import org.apache.drill.categories.UnlikelyTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({UnlikelyTest.class, PlannerTest.class})
public class TestFlattenPlanning extends PlanTestBase {

  @Test
  public void testFlattenPlanningAvoidUnnecessaryProject() throws Exception {
    // Because of Java7 vs Java8 map ordering differences, we check for both cases
    // See DRILL-4331 for details
    testPlanMatchingPatterns("select flatten(complex), rownum from cp.`store/json/test_flatten_mappify2.json`",
        new String[]{"\\QProject(EXPR$0=[$1], rownum=[$0])\\E|\\QProject(EXPR$0=[$0], rownum=[$1])\\E"},
        new String[]{"\\QProject(EXPR$0=[$0], EXPR$1=[$1], EXPR$3=[$1])\\E|\\QProject(EXPR$0=[$1], EXPR$1=[$0], EXPR$3=[$0])\\E"});
  }

  @Test // DRILL-4121 : push partial filter past projects.
  public void testPushFilterPastProjectWithFlatten() throws Exception {
    final String query =
        " select comp, rownum " +
        " from (select flatten(complex) comp, rownum " +
        "      from cp.`store/json/test_flatten_mappify2.json`) " +
        " where comp > 1 " +   // should not be pushed down
        "   and rownum = 100"; // should be pushed down.

    final String[] expectedPlans = {"(?s)Filter.*>.*Flatten.*Filter.*=.*"};
    final String[] excludedPlans = {"Filter.*AND.*"};
    PlanTestBase.testPlanMatchingPatterns(query, expectedPlans, excludedPlans);
  }

  @Test // DRILL-4121 : push partial filter past projects : neg test case
  public void testPushFilterPastProjectWithFlattenNeg() throws Exception {
    final String query =
        " select comp, rownum " +
            " from (select flatten(complex) comp, rownum " +
            "      from cp.`store/json/test_flatten_mappify2.json`) " +
            " where comp > 1 " +   // should NOT be pushed down
            "   OR rownum = 100";  // should NOT be pushed down.

    final String[] expectedPlans = {"(?s)Filter.*OR.*Flatten"};
    final String[] excludedPlans = {"(?s)Filter.*Flatten.*Filter.*"};
    PlanTestBase.testPlanMatchingPatterns(query, expectedPlans, excludedPlans);
  }

  @Test // DRILL-6099 : push limit past flatten(project)
  public void testLimitPushdownPastFlatten() throws Exception {
    final String query = "select rownum, flatten(complex) comp from cp.`store/json/test_flatten_mappify2.json` limit 1";
    //DRILL-6318 : limit should not push past flatten(project)
    final String[] expectedPatterns = {"(?s).*Limit.*Flatten.*Project.*"};
    final String[] excludedPatterns = {"(?s).*Limit.*Flatten.*Limit.*"};
    PlanTestBase.testPlanMatchingPatterns(query, expectedPatterns, excludedPatterns);
  }

}
