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
package org.apache.drill.exec.physical.impl.join;

import org.junit.BeforeClass;
import org.junit.Test;
import java.nio.file.Paths;


public class TestHashJoinJPPDPlan extends JoinTestBase {

  @BeforeClass
  public static void setupTestFiles() throws Exception {
    dirTestWatcher.copyFileToRoot(Paths.get("sample-data", "nation.parquet"));
    dirTestWatcher.copyFileToRoot(Paths.get("sample-data", "region.parquet"));
    dirTestWatcher.copyResourceToRoot(Paths.get("multilevel", "parquet"));
    dirTestWatcher.copyResourceToRoot(Paths.get("join", "multiple"));
    test("alter session set `exec.hashjoin.enable.runtime_filter` = true");
  }

  @Test
  public void testLeftHashJoin() throws Exception {
    String sql = "SELECT nations.N_NAME, count(*)"
      + "FROM\n"
      + " dfs.`sample-data/nation.parquet` nations\n"
      + "LEFT JOIN\n"
      + "  dfs.`sample-data/region.parquet` regions\n"
      + "  on nations.N_REGIONKEY = regions.R_REGIONKEY "
      + "group by nations.N_NAME";
    String excludedColNames1 =  "\"runtimeFilterDef\"";
    String excludedColNames2 =  "\"bloomFilterDefs\"";
    String excludedColNames3 =  "\"runtime-filter\"";
    testPlanWithAttributesMatchingPatterns(sql, null, new String[]{excludedColNames1, excludedColNames2, excludedColNames3});
  }


  @Test
  public void testHashJoinWithFuncJoinCondition() throws Exception {
    String sql = "SELECT nations.N_NAME, count(*)"
      + "FROM\n"
      + " dfs.`sample-data/nation.parquet` nations\n"
      + "JOIN\n"
      + "  dfs.`sample-data/region.parquet` regions\n"
      + "  on (nations.N_REGIONKEY + 1) = regions.R_REGIONKEY "
      + "group by nations.N_NAME";
    String excludedColNames1 =  "\"runtimeFilterDef\"";
    String excludedColNames2 =  "\"bloomFilterDefs\"";
    String excludedColNames3 =  "\"runtime-filter\"";
    testPlanWithAttributesMatchingPatterns(sql, null, new String[]{excludedColNames1, excludedColNames2, excludedColNames3});
  }
}
