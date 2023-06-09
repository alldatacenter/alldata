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
package org.apache.drill.hbase;

import org.apache.drill.categories.HbaseStorageTest;
import org.apache.drill.categories.SlowTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({SlowTest.class, HbaseStorageTest.class})
public class TestHBaseConnectionManager extends BaseHBaseTest {

  @Test
  public void testHBaseConnectionManager() throws Exception{
    setColumnWidth(8);
    runHBaseSQLVerifyCount("SELECT\n"
        + "row_key\n"
        + "FROM\n"
        + "  hbase.`[TABLE_NAME]` tableName",
        8);

    /*
     * Simulate HBase connection close and ensure that the connection
     * will be reestablished automatically.
     */
    storagePlugin.getConnection().close();
    runHBaseSQLVerifyCount("SELECT\n"
        + "row_key\n"
        + "FROM\n"
        + "  hbase.`[TABLE_NAME]` tableName",
        8);

    /*
     * Simulate HBase cluster restart and ensure that running query against
     * HBase does not require Drill cluster restart.
     */
    HBaseTestsSuite.getHBaseTestingUtility().shutdownMiniHBaseCluster();
    HBaseTestsSuite.getHBaseTestingUtility().restartHBaseCluster(1);
    runHBaseSQLVerifyCount("SELECT\n"
        + "row_key\n"
        + "FROM\n"
        + "  hbase.`[TABLE_NAME]` tableName",
        8);

  }

}
