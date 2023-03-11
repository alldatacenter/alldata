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

import java.io.IOException;
import java.util.List;

import org.apache.drill.categories.HbaseStorageTest;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.exec.client.DrillClient;
import org.apache.drill.exec.rpc.user.QueryDataBatch;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({SlowTest.class, HbaseStorageTest.class})
public class TestHBaseCFAsJSONString extends BaseHBaseTest {

  private static DrillClient parent_client;

  @BeforeClass
  public static void openMyClient() throws Exception {
    parent_client = client;
    client = new DrillClient(config, serviceSet.getCoordinator());
    client.setSupportComplexTypes(false);
    client.connect();
  }

  @AfterClass
  public static void closeMyClient() throws IOException {
    if (client != null) {
      client.close();
    }
    client = parent_client;
  }

  @Test
  public void testColumnFamiliesAsJSONString() throws Exception {
    setColumnWidths(new int[] {112, 12});
    List<QueryDataBatch> resultList = runHBaseSQLlWithResults("SELECT f, f2 FROM hbase.`[TABLE_NAME]` tableName LIMIT 1");
    logResult(resultList);
  }

}
