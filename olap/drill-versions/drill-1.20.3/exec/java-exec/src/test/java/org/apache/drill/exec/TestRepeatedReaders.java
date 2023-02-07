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
package org.apache.drill.exec;

import org.apache.drill.test.BaseTestQuery;
import org.junit.Test;

public class TestRepeatedReaders extends BaseTestQuery {

  private void createAndQuery(String datafile) throws Exception {
    String query = String.format("select * from cp.`parquet/%s`", datafile);
    String tableName = "test_repeated_readers_"+datafile;

    test("create table dfs.tmp.`%s` as %s", tableName, query);

    testBuilder()
      .sqlQuery("select * from dfs.tmp.`%s` d", tableName)
      .ordered()
      .jsonBaselineFile("parquet/" + datafile)
      .go();
  }

  @Test //DRILL-2292
  public void testNestedRepeatedMapInsideRepeatedMap() throws Exception {
    createAndQuery("2292.rm_rm.json");
  }

  @Test //DRILL-2292
  public void testNestedRepeatedMapInsideMapInsideRepeatedMap() throws Exception {
    createAndQuery("2292.rm_m_rm.json");
  }

  @Test //DRILL-2292
  public void testNestedRepeatedListInsideRepeatedMap() throws Exception {
    runSQL("alter session set `store.format` = 'json'");

    try {
      createAndQuery("2292.rl_rm.json");
    } finally {
      runSQL("alter session set `store.format` = 'parquet'");
    }
  }

  @Test //DRILL-2292
  public void testNestedRepeatedMapInsideRepeatedList() throws Exception {
    runSQL("alter session set `store.format` = 'json'");

    try {
      createAndQuery("2292.rm_rl.json");
    } finally {
      runSQL("alter session set `store.format` = 'parquet'");
    }
  }

  @Test //DRILL-2292
  public void testNestedRepeatedListInsideRepeatedList() throws Exception {
    runSQL("alter session set `store.format` = 'json'");

    try {
      createAndQuery("2292.rl_rl.json");
    } finally {
      runSQL("alter session set `store.format` = 'parquet'");
    }
  }
}
