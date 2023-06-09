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

import static org.junit.Assert.assertEquals;

import org.apache.drill.categories.OperatorTest;
import org.apache.drill.test.BaseTestQuery;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(OperatorTest.class)
public class TestAggNullable extends BaseTestQuery {

  private static void enableAggr(boolean ha, boolean sa) throws Exception {

    test("alter session set `planner.enable_hashagg` = %s", ha);
    test("alter session set `planner.enable_streamagg` = %s", sa);
    test("alter session set `planner.slice_target` = 1");
  }

  @Test  // HashAgg on nullable columns
  public void testHashAggNullableColumns() throws Exception {
    String query1 = "select t2.b2 from cp.`jsoninput/nullable2.json` t2 group by t2.b2";
    String query2 = "select t2.a2, t2.b2 from cp.`jsoninput/nullable2.json` t2 group by t2.a2, t2.b2";

    int actualRecordCount;
    int expectedRecordCount = 2;

    enableAggr(true, false);
    actualRecordCount = testSql(query1);
    assertEquals(String.format("Received unexpected number of rows in output: expected=%d, received=%s",
        expectedRecordCount, actualRecordCount), expectedRecordCount, actualRecordCount);

    expectedRecordCount = 4;
    actualRecordCount = testSql(query2);
    assertEquals(String.format("Received unexpected number of rows in output: expected=%d, received=%s",
        expectedRecordCount, actualRecordCount), expectedRecordCount, actualRecordCount);
  }

  @Test  // StreamingAgg on nullable columns
  public void testStreamAggNullableColumns() throws Exception {
    String query1 = "select t2.b2 from cp.`jsoninput/nullable2.json` t2 group by t2.b2";
    String query2 = "select t2.a2, t2.b2 from cp.`jsoninput/nullable2.json` t2 group by t2.a2, t2.b2";

    int actualRecordCount;
    int expectedRecordCount = 2;

    enableAggr(false, true);
    actualRecordCount = testSql(query1);
    assertEquals(String.format("Received unexpected number of rows in output: expected=%d, received=%s",
        expectedRecordCount, actualRecordCount), expectedRecordCount, actualRecordCount);

    expectedRecordCount = 4;
    actualRecordCount = testSql(query2);
    assertEquals(String.format("Received unexpected number of rows in output: expected=%d, received=%s",
        expectedRecordCount, actualRecordCount), expectedRecordCount, actualRecordCount);
  }
}
