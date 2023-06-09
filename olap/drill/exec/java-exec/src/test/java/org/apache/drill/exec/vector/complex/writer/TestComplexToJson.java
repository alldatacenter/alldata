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
package org.apache.drill.exec.vector.complex.writer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.exec.client.DrillClient;
import org.apache.drill.exec.proto.UserBitShared.RecordBatchDef;
import org.apache.drill.exec.record.RecordBatchLoader;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.junit.Test;

public class TestComplexToJson extends BaseTestQuery {

  @Test
  public void test() throws Exception {
    DrillClient parent_client = client;

    List<QueryDataBatch> results;
    RecordBatchLoader loader = new RecordBatchLoader(getAllocator());

    client = new DrillClient(config, serviceSet.getCoordinator());
    client.setSupportComplexTypes(false);
    client.connect();
    results = testSqlWithResults("select * from cp.`store/text/data/regions.csv`");
    loader.load(results.get(0).getHeader().getDef(), results.get(0).getData());
    RecordBatchDef def = results.get(0).getHeader().getDef();
    // the entire row is returned as a single column
    assertEquals(1, def.getFieldCount());
    // with setSupportComplexTypes == false, the column mode should be REQUIRED
    assertTrue(def.getField(0).getMajorType().getMode() == DataMode.REQUIRED);
    loader.clear();
    for(QueryDataBatch result : results) {
      result.release();
    }
    client.close();

    client = new DrillClient(config, serviceSet.getCoordinator());
    client.setSupportComplexTypes(true);
    client.connect();
    results = testSqlWithResults("select * from cp.`store/text/data/regions.csv`");
    loader.load(results.get(0).getHeader().getDef(), results.get(0).getData());
    def = results.get(0).getHeader().getDef();
    // the entire row is returned as a single column
    assertEquals(1, def.getFieldCount());
    // with setSupportComplexTypes == true, the column mode should be REPEATED
    assertTrue(def.getField(0).getMajorType().getMode() == DataMode.REPEATED);
    loader.clear();
    for(QueryDataBatch result : results) {
      result.release();
    }
    client.close();

    client = parent_client;
  }
}
