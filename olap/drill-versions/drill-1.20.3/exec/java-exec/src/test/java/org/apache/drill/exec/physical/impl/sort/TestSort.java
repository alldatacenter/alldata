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
package org.apache.drill.exec.physical.impl.sort;

import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.categories.OperatorTest;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.util.JsonStringArrayList;
import org.apache.drill.exec.util.JsonStringHashMap;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Placeholder for all sort related test. Can be used as we move
 * more tests to use the new test framework
 */
@Category(OperatorTest.class)
public class TestSort extends BaseTestQuery {

  private static final JsonStringHashMap<String, Object> x = new JsonStringHashMap<>();
  private static final JsonStringArrayList<JsonStringHashMap<String, Object>> repeated_map = new JsonStringArrayList<>();

  static {
    x.put("c", 1l);
    repeated_map.add(0, x);
  }

  @Test
  public void testSortWithComplexInput() throws Exception {
    testBuilder()
        .sqlQuery("select (t.a) as col from cp.`jsoninput/repeatedmap_sort_bug.json` t order by t.b")
        .ordered()
        .baselineColumns("col")
        .baselineValues(repeated_map)
        .go();
  }

  @Test
  public void testSortWithRepeatedMapWithExchanges() throws Exception {
    testBuilder()
        .sqlQuery("select (t.a) as col from cp.`jsoninput/repeatedmap_sort_bug.json` t order by t.b")
        .optionSettingQueriesForTestQuery("alter session set `planner.slice_target` = 1")
        .ordered()
        .baselineColumns("col")
        .baselineValues(repeated_map)
        .go();

    // reset the planner.slice_target
    test("alter session set `planner.slice_target` = " + ExecConstants.SLICE_TARGET_DEFAULT);
  }
}