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

import org.apache.drill.categories.PlannerTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.nio.file.Paths;

@Category({PlannerTest.class})
public class TestDirScanToValuesConversion extends PlanTestBase {

  private static final String TABLE_WITH_METADATA = "parquetTable1";

  @BeforeClass
  public static void setUp() {
    dirTestWatcher.copyResourceToRoot(Paths.get("multilevel"));
    dirTestWatcher.copyResourceToRoot(Paths.get("multilevel/parquet"), Paths.get(TABLE_WITH_METADATA));
  }

  @Test
  public void testDirScanToValuesConversion() throws Exception {
    String[] tableNames = {"multilevel/parquet", "multilevel/json", "multilevel/csv"};
    String[] queries = {
        "select distinct dir0, dir1 from dfs.`%s`",
        "select dir0, dir1 from dfs.`%s` group by 1, 2"
    };
    for (String tableName : tableNames) {
      for (String query : queries) {
        testPlanMatchingPatterns(String.format(query, tableName), new String[]{"Values\\(tuples="}, new String[]{"Scan\\(table="});
      }
    }
  }

  @Test
  public void testDirScanToValuesConversionWithMetadataCache() throws Exception {
    test("refresh table metadata dfs.`%s`", TABLE_WITH_METADATA);
    checkForMetadataFile(TABLE_WITH_METADATA);
    String query = String.format("select distinct dir0, dir1 from dfs.`%s`", TABLE_WITH_METADATA);
    PlanTestBase.testPlanMatchingPatterns(query, new String[]{"Values\\(tuples="}, null);
  }

  @Test
  public void testDirScanToValuesConversionIsNotApplied() throws Exception {
    String[] tableNames = {"multilevel/parquet", "multilevel/json", "multilevel/csv"};
    String[] queries = {
        "select dir0, dir1 from dfs.`%s`", // no aggregation
        "select dir0, dir1, o_custkey from dfs.`%s` group by 1, 2, 3" // not only partition columns present
    };
    for (String tableName : tableNames) {
      for (String query : queries) {
        testPlanMatchingPatterns(String.format(query, tableName), new String[]{"Scan\\(table="}, new String[]{"Values\\(tuples="});
      }
    }
  }
}
