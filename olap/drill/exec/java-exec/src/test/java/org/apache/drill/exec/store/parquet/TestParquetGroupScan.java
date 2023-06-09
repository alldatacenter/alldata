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

import org.apache.drill.test.BaseTestQuery;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestParquetGroupScan extends BaseTestQuery {

  private void prepareTables(final String tableName, boolean refreshMetadata) throws Exception {
    // first create some parquet subfolders
    testNoResult("CREATE TABLE dfs.tmp.`%s`      AS SELECT employee_id FROM cp.`employee.json` LIMIT 1", tableName);
    testNoResult("CREATE TABLE dfs.tmp.`%s/501`  AS SELECT employee_id FROM cp.`employee.json` LIMIT 2", tableName);
    testNoResult("CREATE TABLE dfs.tmp.`%s/502`  AS SELECT employee_id FROM cp.`employee.json` LIMIT 4", tableName);
    testNoResult("CREATE TABLE dfs.tmp.`%s/503`  AS SELECT employee_id FROM cp.`employee.json` LIMIT 8", tableName);
    testNoResult("CREATE TABLE dfs.tmp.`%s/504`  AS SELECT employee_id FROM cp.`employee.json` LIMIT 16", tableName);
    testNoResult("CREATE TABLE dfs.tmp.`%s/505`  AS SELECT employee_id FROM cp.`employee.json` LIMIT 32", tableName);
    testNoResult("CREATE TABLE dfs.tmp.`%s/60`   AS SELECT employee_id FROM cp.`employee.json` LIMIT 64", tableName);
    testNoResult("CREATE TABLE dfs.tmp.`%s/602`  AS SELECT employee_id FROM cp.`employee.json` LIMIT 128", tableName);
    testNoResult("CREATE TABLE dfs.tmp.`%s/6031` AS SELECT employee_id FROM cp.`employee.json` LIMIT 256", tableName);
    testNoResult("CREATE TABLE dfs.tmp.`%s/6032` AS SELECT employee_id FROM cp.`employee.json` LIMIT 512", tableName);
    testNoResult("CREATE TABLE dfs.tmp.`%s/6033` AS SELECT employee_id FROM cp.`employee.json` LIMIT 1024", tableName);

    // we need an empty subfolder `4376/20160401`
    // to do this we first create a table inside that subfolder
    testNoResult("CREATE TABLE dfs.tmp.`%s/6041/a` AS SELECT * FROM cp.`employee.json` LIMIT 1", tableName);
    // then we delete the table, leaving the parent subfolder empty
    testNoResult("DROP TABLE   dfs.tmp.`%s/6041/a`", tableName);

    if (refreshMetadata) {
      // build the metadata cache file
      testNoResult("REFRESH TABLE METADATA dfs.tmp.`%s`", tableName);
    }
  }

  @Test
  public void testFix4376() throws Exception {
    prepareTables("4376_1", true);

    int actualRecordCount = testSql("SELECT * FROM dfs.tmp.`4376_1/60*`");
    int expectedRecordCount = 1984;
    assertEquals(String.format("Received unexpected number of rows in output: expected = %d, received = %s",
        expectedRecordCount, actualRecordCount), expectedRecordCount, actualRecordCount);
  }

  @Test
  public void testWildCardEmptyWithCache() throws Exception {
    prepareTables("4376_2", true);

    int actualRecordCount = testSql("SELECT * FROM dfs.tmp.`4376_2/604*`");
    int expectedRecordCount = 0;
    assertEquals(String.format("Received unexpected number of rows in output: expected = %d, received = %s",
        expectedRecordCount, actualRecordCount), expectedRecordCount, actualRecordCount);
  }

  @Test
  public void testWildCardEmptyNoCache() throws Exception {
    prepareTables("4376_3", false);

    int actualRecordCount = testSql("SELECT * FROM dfs.tmp.`4376_3/604*`");
    int expectedRecordCount = 0;
    assertEquals(String.format("Received unexpected number of rows in output: expected = %d, received = %s",
        expectedRecordCount, actualRecordCount), expectedRecordCount, actualRecordCount);
  }

  @Test
  public void testSelectEmptyWithCache() throws Exception {
    prepareTables("4376_4", true);

    int actualRecordCount = testSql("SELECT * FROM dfs.tmp.`4376_4/6041`");
    int expectedRecordCount = 0;
    assertEquals(String.format("Received unexpected number of rows in output: expected = %d, received = %s",
        expectedRecordCount, actualRecordCount), expectedRecordCount, actualRecordCount);
  }

  @Test
  public void testSelectEmptyNoCache() throws Exception {
    prepareTables("4376_5", false);

    int actualRecordCount = testSql("SELECT * FROM dfs.tmp.`4376_5/6041`");
    int expectedRecordCount = 0;
    assertEquals(String.format("Received unexpected number of rows in output: expected = %d, received = %s",
        expectedRecordCount, actualRecordCount), expectedRecordCount, actualRecordCount);
  }
}
