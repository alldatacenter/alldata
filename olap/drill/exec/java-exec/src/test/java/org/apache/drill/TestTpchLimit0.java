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

import org.apache.drill.categories.SlowTest;
import org.apache.drill.test.BaseTestQuery;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({SlowTest.class})
public class TestTpchLimit0 extends BaseTestQuery {

  private void testLimitZero(String fileName) throws Exception {
    String query = getFile(fileName);
    query = "ALTER SESSION SET `planner.slice_target` = 1; select * from \n(" + query.replace(";", ")xyz limit 0;");
    test(query);
  }

  @Test
  public void tpch01() throws Exception{
    testLimitZero("queries/tpch/01.sql");
  }

  @Test
  @Ignore // DRILL-512
  public void tpch02() throws Exception{
    testLimitZero("queries/tpch/02.sql");
  }

  @Test
  public void tpch03() throws Exception{
    testLimitZero("queries/tpch/03.sql");
  }

  @Test
  public void tpch04() throws Exception{
    testLimitZero("queries/tpch/04.sql");
  }

  @Test
  public void tpch05() throws Exception{
    testLimitZero("queries/tpch/05.sql");
  }

  @Test
  public void tpch06() throws Exception{
    testLimitZero("queries/tpch/06.sql");
  }

  @Test
  public void tpch07() throws Exception{
    testLimitZero("queries/tpch/07.sql");
  }

  @Test
  public void tpch08() throws Exception{
    testLimitZero("queries/tpch/08.sql");
  }

  @Test
  public void tpch09() throws Exception{
    testLimitZero("queries/tpch/09.sql");
  }

  @Test
  public void tpch10() throws Exception{
    testLimitZero("queries/tpch/10.sql");
  }

  @Test
  @Ignore // Cartesian problem
  public void tpch11() throws Exception{
    testLimitZero("queries/tpch/11.sql");
  }

  @Test
  public void tpch12() throws Exception{
    testLimitZero("queries/tpch/12.sql");
  }

  @Test
  public void tpch13() throws Exception{
    testLimitZero("queries/tpch/13.sql");
  }

  @Test
  public void tpch14() throws Exception{
    testLimitZero("queries/tpch/14.sql");
  }

  @Test
  @Ignore //
  public void tpch15() throws Exception{
    testLimitZero("queries/tpch/15.sql");
  }

  @Test
  @Ignore // invalid plan, due to Nulls value NOT IN sub-q
  public void tpch16() throws Exception{
    testLimitZero("queries/tpch/16.sql");
  }

  @Test
  @Ignore //
  public void tpch17() throws Exception{
    testLimitZero("queries/tpch/17.sql");
  }

  @Test
  public void tpch18() throws Exception{
    testLimitZero("queries/tpch/18.sql");
  }

  @Test
  @Ignore // DRILL-519
  public void tpch19() throws Exception{
    testLimitZero("queries/tpch/19.sql");
  }

  @Test
  public void tpch20() throws Exception{
    testLimitZero("queries/tpch/20.sql");
  }

  @Test
  public void tpch21() throws Exception{
    testLimitZero("queries/tpch/21.sql");
  }

  @Test
  @Ignore // DRILL-518
  public void tpch22() throws Exception{
    testLimitZero("queries/tpch/22.sql");
  }
}
