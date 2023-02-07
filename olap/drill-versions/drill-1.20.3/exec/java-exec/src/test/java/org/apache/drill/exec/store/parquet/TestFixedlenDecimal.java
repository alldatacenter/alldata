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
package org.apache.drill.exec.store.parquet;

import org.apache.drill.categories.ParquetTest;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({ParquetTest.class, UnlikelyTest.class})
public class TestFixedlenDecimal extends BaseTestQuery {


  @BeforeClass
  public static void enableDecimalDataType() {
    setSessionOption(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY, true);
  }

  @AfterClass
  public static void disableDecimalDataType() {
    resetSessionOption(PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY);
  }

  private static final String DATAFILE = "cp.`parquet/fixedlenDecimal.parquet`";

  @Test
  public void testNullCount() throws Exception {
    testBuilder()
        .sqlQuery("select count(*) as c from %s where department_id is null", DATAFILE)
        .unOrdered()
        .baselineColumns("c")
        .baselineValues(1L)
        .go();
  }

  @Test
  public void testNotNullCount() throws Exception {
    testBuilder()
        .sqlQuery("select count(*) as c from %s where department_id is not null", DATAFILE)
        .unOrdered()
        .baselineColumns("c")
        .baselineValues(106L)
        .go();
  }

  @Test
  public void testSimpleQueryWithCast() throws Exception {
    testBuilder()
        .sqlQuery("select cast(department_id as bigint) as c from %s where cast(employee_id as decimal) = 170", DATAFILE)
        .unOrdered()
        .baselineColumns("c")
        .baselineValues(80L)
        .go();
  }

  @Test
  public void testSimpleQueryDrill4704Fix() throws Exception {
    testBuilder()
        .sqlQuery("select cast(department_id as bigint) as c from %s where employee_id = 170", DATAFILE)
        .unOrdered()
        .baselineColumns("c")
        .baselineValues(80L)
        .go();
  }
}
