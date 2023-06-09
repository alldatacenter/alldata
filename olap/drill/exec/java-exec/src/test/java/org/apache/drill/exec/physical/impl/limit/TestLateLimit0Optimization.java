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

import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.PlanTestBase;
import org.junit.Test;

import static org.apache.drill.exec.ExecConstants.LATE_LIMIT0_OPT_KEY;

public class TestLateLimit0Optimization extends BaseTestQuery {

  @Test
  public void convertFromJson() throws Exception {
    checkThatQueryIsNotOptimized("SELECT CONVERT_FROM('{x:100, y:215.6}' ,'JSON') AS MYCOL FROM (VALUES(1))");
  }

  private static void checkThatQueryIsNotOptimized(final String query) throws Exception {
    PlanTestBase.testPlanMatchingPatterns(wrapLimit0(query),
        null,
        new String[] {".*Limit\\(offset=\\[0\\], fetch=\\[0\\]\\)(.*[\n\r])+.*Scan.*"});
  }

  private static String wrapLimit0(final String query) {
    return "SELECT * FROM (" + query + ") LZT LIMIT 0";
  }

  @Test
  public void convertToIntBE() throws Exception {
    checkThatQueryIsOptimized("SELECT CONVERT_TO(r_regionkey, 'INT_BE') FROM cp.`tpch/region.parquet`");
  }

  private static void checkThatQueryIsOptimized(final String query) throws Exception {
    PlanTestBase.testPlanMatchingPatterns(wrapLimit0(query),
        new String[] {".*Limit\\(offset=\\[0\\], fetch=\\[0\\]\\)(.*[\n\r])+.*Scan.*"});
  }

  @Test
  public void convertToOthers() throws Exception {
    checkThatQueryIsOptimized("SELECT r_regionkey,\n" +
        "  STRING_BINARY(CONVERT_TO(r_regionkey, 'INT')) as i,\n" +
        "  STRING_BINARY(CONVERT_TO(r_regionkey, 'INT_BE')) as i_be,\n" +
        "  STRING_BINARY(CONVERT_TO(r_regionkey, 'BIGINT')) as l,\n" +
        "  STRING_BINARY(CONVERT_TO(r_regionkey, 'BIGINT')) as l_be,\n" +
        "  STRING_BINARY(CONVERT_TO(r_name, 'UTF8')) u8,\n" +
        "  STRING_BINARY(CONVERT_TO(r_name, 'UTF16')) u16,\n" +
        "  STRING_BINARY(CONVERT_TO(r_regionkey, 'INT_HADOOPV')) as i_ha\n" +
        "FROM cp.`tpch/region.parquet`");
  }

  @Test
  public void union() throws Exception {
    checkThatQueryIsNotOptimized("(select n_regionkey from cp.`tpch/nation.parquet`) union " +
        "(select r_regionname from cp.`tpch/region.parquet`)");
  }

  @Test
  public void unionAll() throws Exception {
    checkThatQueryIsNotOptimized("(select n_regionkey from cp.`tpch/nation.parquet`) union all " +
        "(select r_regionname from cp.`tpch/region.parquet`)");
  }

  @Test
  public void flatten() throws Exception {
    checkThatQueryIsNotOptimized("select flatten(arr) as a from cp.`/flatten/drill-3370.json`");
  }

  @Test
  public void flatten2() throws Exception {
    checkThatQueryIsNotOptimized("select uid, lst_lst, d.lst_lst[1], flatten(d.lst_lst) lst " +
        "from cp.`tpch/region.parquet` d order by d.lst_lst[1][2]"); // table is just for validation
  }

  @Test
  public void flatten3() throws Exception {
    checkThatQueryIsNotOptimized("select s.evnts.evnt_id from (select d.type type, flatten(d.events) evnts from " +
        "cp.`tpch/region.parquet` d where d.type='web' order by d.uid) s " +
        "where s.evnts.type = 'cmpgn4' and s.type='web'"); // table is just for validation
  }

  @Test
  public void flatten4() throws Exception {
    checkThatQueryIsNotOptimized("select flatten(lst) from (select uid, flatten(d.lst_lst) lst from " +
        "cp.`tpch/region.parquet` d) s1 order by s1.lst[3]"); // table is just for validation
  }

  @Test
  public void countDistinct() throws Exception {
    checkThatQueryIsOptimized("SELECT COUNT(employee_id), " +
            "SUM(employee_id), " +
            "COUNT(DISTINCT employee_id) " +
            "FROM cp.`employee.json`");
  }

  @Test
  public void testLimit0IsAbsentWhenDisabled() throws Exception {
    String query = "SELECT CONVERT_TO(r_regionkey, 'INT_BE') FROM cp.`tpch/region.parquet`";
    try {
      setSessionOption(LATE_LIMIT0_OPT_KEY, false);
      PlanTestBase.testPlanMatchingPatterns(wrapLimit0(query), null, new String[] {".*Limit\\(offset=\\[0\\], fetch=\\[0\\]\\)(.*[\n\r])+.*Scan.*"});
    } finally {
      resetSessionOption(LATE_LIMIT0_OPT_KEY);
    }
  }
}
