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
package org.apache.drill.exec.impersonation;

import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.drill.categories.SecurityTest;
import org.apache.drill.exec.store.dfs.WorkspaceConfig;
import org.apache.drill.categories.SlowTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Note to future devs, please do not put random tests here. Make sure that they actually require
 * access to a DFS instead of the local filesystem implementation used by default in the rest of
 * the tests. Running this mini cluster is slow and it is best for these tests to only cover
 * necessary cases.
 */
@Category({SlowTest.class, SecurityTest.class})
public class TestImpersonationDisabledWithMiniDFS extends BaseTestImpersonation {

  @BeforeClass
  public static void setup() throws Exception {
    startMiniDfsCluster(TestImpersonationDisabledWithMiniDFS.class.getSimpleName(), false);
    startDrillCluster(false);
    addMiniDfsBasedStorage(Maps.<String, WorkspaceConfig>newHashMap());
    createTestData();
  }

  private static void createTestData() throws Exception {
    // Create test table in minidfs.tmp schema for use in test queries
    test(String.format("CREATE TABLE %s.tmp.dfsRegion AS SELECT * FROM cp.`region.json`", MINI_DFS_STORAGE_PLUGIN_NAME));

    // generate a large enough file that the DFS will not fulfill requests to read a
    // page of data all at once, see notes above testReadLargeParquetFileFromDFS()
    test(String.format(
        "CREATE TABLE %s.tmp.large_employee AS " +
            "(SELECT employee_id, full_name FROM cp.`employee.json`) " +
            "UNION ALL (SELECT employee_id, full_name FROM cp.`employee.json`)" +
            "UNION ALL (SELECT employee_id, full_name FROM cp.`employee.json`)" +
            "UNION ALL (SELECT employee_id, full_name FROM cp.`employee.json`)" +
            "UNION ALL (SELECT employee_id, full_name FROM cp.`employee.json`)" +
            "UNION ALL (SELECT employee_id, full_name FROM cp.`employee.json`)" +
            "UNION ALL (SELECT employee_id, full_name FROM cp.`employee.json`)" +
        "UNION ALL (SELECT employee_id, full_name FROM cp.`employee.json`)", MINI_DFS_STORAGE_PLUGIN_NAME));
  }

  /**
   * When working on merging the Drill fork of parquet a bug was found that only manifested when
   * run on a cluster. It appears that the local implementation of the Hadoop FileSystem API
   * never fails to provide all of the bytes that are requested in a single read. The API is
   * designed to allow for a subset of the requested bytes be returned, and a client can decide
   * if they want to do processing on teh subset that are available now before requesting the rest.
   *
   * For parquet's block compression of page data, we need all of the bytes. This test is here as
   * a sanitycheck  to make sure we don't accidentally introduce an issue where a subset of the bytes
   * are read and would otherwise require testing on a cluster for the full contract of the read method
   * we are using to be exercised.
   */
  @Test
  public void testReadLargeParquetFileFromDFS() throws Exception {
    test(String.format("USE %s", MINI_DFS_STORAGE_PLUGIN_NAME));
    test("SELECT * FROM tmp.`large_employee`");
  }

  @Test // DRILL-3037
  public void testSimpleQuery() throws Exception {
    final String query =
        String.format("SELECT sales_city, sales_country FROM tmp.dfsRegion ORDER BY region_id DESC LIMIT 2");

    testBuilder()
        .optionSettingQueriesForTestQuery(String.format("USE %s", MINI_DFS_STORAGE_PLUGIN_NAME))
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("sales_city", "sales_country")
        .baselineValues("Santa Fe", "Mexico")
        .baselineValues("Santa Anita", "Mexico")
        .go();
  }

  @AfterClass
  public static void removeMiniDfsBasedStorage() throws Exception {
    getDrillbitContext().getStorage().remove(MINI_DFS_STORAGE_PLUGIN_NAME);
    stopMiniDfsCluster();
  }
}
