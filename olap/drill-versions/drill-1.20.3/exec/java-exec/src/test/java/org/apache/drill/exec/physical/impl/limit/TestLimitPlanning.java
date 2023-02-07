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
package org.apache.drill.exec.physical.impl.limit;

import org.apache.drill.PlanTestBase;
import org.junit.Test;

public class TestLimitPlanning extends PlanTestBase {

  // DRILL-6474
  @Test
  public void dontPushdownIntoTopNWhenNoLimit() throws Exception {
    String query = "select full_name from cp.`employee.json` order by full_name offset 10";

    PlanTestBase.testPlanMatchingPatterns(query, new String[]{".*Sort\\(.*"}, new String[]{".*TopN\\(.*"});
  }

  @Test
  public void offsetMoreThanTotalRowsWithoutFetch() throws Exception {
    String query = "select full_name from cp.`employee.json` offset 1156";
    // Should not raise an assert
    PlanTestBase.testPlanMatchingPatterns(query, new String[]{".*Limit\\(offset=\\[1156\\]\\).*"});
  }
}
