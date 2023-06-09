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
package org.apache.drill.exec.planner.logical;

import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.categories.SqlTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.math.BigDecimal;

/**
 * Tests for handling nullable types in CASE function
 */
@Category(SqlTest.class)
public class TestCaseNullableTypes extends BaseTestQuery {

  @Test
  public void testCaseNullableTypesInt() throws Exception {
    testBuilder()
        .sqlQuery("select (case when (false) then null else 1 end) res1 from (values(1))")
        .unOrdered()
        .baselineColumns("res1")
        .baselineValues(1)
        .go();
  }

  @Test
  public void testCaseNullableTypesVarchar() throws Exception {
    testBuilder()
        .sqlQuery("select (res1 = 'qwe') res2 from (select (case when (false) then null else 'qwe' end) res1 from (values(1)))")
        .unOrdered()
        .baselineColumns("res2")
        .baselineValues(true)
        .go();
  }

  @Test
  public void testCaseNullableTypesBigint() throws Exception {
    testBuilder()
        .sqlQuery("select (case when (false) then null else " + Long.MAX_VALUE + " end) res1 from (values(1))")
        .unOrdered()
        .baselineColumns("res1")
        .baselineValues(Long.MAX_VALUE)
        .go();
  }

  @Test
  public void testCaseNullableTypesFloat() throws Exception {
    testBuilder()
        .sqlQuery("select (case when (false) then null else cast(0.1 as float) end) res1 from (values(1))")
        .unOrdered()
        .baselineColumns("res1")
        .baselineValues(0.1F)
        .go();
  }

  @Test
  public void testCaseNullableTypesDouble() throws Exception {
    testBuilder()
        .sqlQuery("select (case when (false) then null else cast(0.1 as double) end) res1 from (values(1))")
        .unOrdered()
        .baselineColumns("res1")
        .baselineValues(0.1)
        .go();
  }

  @Test
  public void testCaseNullableTypesBoolean() throws Exception {
    testBuilder()
        .sqlQuery("select (case when (false) then null else true end) res1 from (values(1))")
        .unOrdered()
        .baselineColumns("res1")
        .baselineValues(true)
        .go();
  }

  @Test
  public void testCaseNullableTypesDate() throws Exception {
    testBuilder()
        .sqlQuery("select (res1 = 22/09/2016) res2 from (select (case when (false) then null else 22/09/2016 end) res1 from (values(1)))")
        .unOrdered()
        .baselineColumns("res2")
        .baselineValues(true)
        .go();
  }

  @Test
  public void testCaseNullableTypesTimestamp() throws Exception {
    testBuilder()
        .sqlQuery("select (res1 = current_timestamp) res2 from (select (case when (false) then null else current_timestamp end) res1 from (values(1)))")
        .unOrdered()
        .baselineColumns("res2")
        .baselineValues(true)
        .go();
  }

  @Test
  public void testNestedCaseNullableTypes() throws Exception {
    testBuilder()
      .sqlQuery("select (case when (false) then null else (case when (false) then null else cast(0.1 as float) end) end) res1 from (values(1))")
      .unOrdered()
      .baselineColumns("res1")
      .baselineValues(0.1f)
      .go();
  }

  @Test
  public void testMultipleCasesNullableTypes() throws Exception {
    testBuilder()
      .sqlQuery("select (case when (false) then null else 1 end) res1, (case when (false) then null else cast(0.1 as float) end) res2 from (values(1))")
      .unOrdered()
      .baselineColumns("res1", "res2")
      .baselineValues(1, 0.1f)
      .go();
  }

  @Test //DRILL-5048
  public void testCaseNullableTimestamp() throws Exception {
    java.time.LocalDateTime date = java.time.LocalDateTime.parse("2016-11-17 14:43:23", java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    testBuilder()
      .sqlQuery("SELECT (CASE WHEN (false) THEN null ELSE CAST('2016-11-17 14:43:23' AS TIMESTAMP) END) res FROM (values(1)) foo")
      .unOrdered()
      .baselineColumns("res")
      .baselineValues(date)
      .go();
  }

  @Test
  public void testCaseNullableTypesDecimal() throws Exception {
    try {
      alterSession(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY, true);
      testBuilder()
          .sqlQuery("select (case when (false) then null else cast(0.1 as decimal(2, 1)) end) res1")
          .ordered()
          .baselineColumns("res1")
          .baselineValues(new BigDecimal("0.1"))
          .go();
    } finally {
      resetSessionOption(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY);
    }
  }

  // Coalesce is being transformed to if-else cases
  @Test
  public void testCoalesceWithUntypedNullValues() throws Exception {
    testBuilder()
        .sqlQuery("select coalesce(coalesce(n_name1, n_name2, n_name), coalesce(n_name3, n_name4), n_name3) res from cp.`tpch/nation.parquet` limit 1")
        .ordered()
        .baselineColumns("res")
        .baselineValues("ALGERIA")
        .go();
  }

  @Test
  public void testCoalesceWithUntypedNullValues_2() throws Exception {
    testBuilder()
        .sqlQuery("select coalesce(coalesce(n_name1, n_name2), n_name) res from cp.`tpch/nation.parquet` limit 1")
        .ordered()
        .baselineColumns("res")
        .baselineValues("ALGERIA")
        .go();
  }
}
