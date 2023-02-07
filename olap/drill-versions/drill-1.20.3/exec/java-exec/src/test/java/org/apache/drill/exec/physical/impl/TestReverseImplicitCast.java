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
package org.apache.drill.exec.physical.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;

import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.client.DrillClient;
import org.apache.drill.exec.pop.PopUnitTestBase;
import org.apache.drill.exec.record.RecordBatchLoader;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.exec.server.Drillbit;
import org.apache.drill.exec.server.RemoteServiceSet;
import org.apache.drill.exec.vector.ValueVector;
import org.junit.Test;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.io.Files;

public class TestReverseImplicitCast extends PopUnitTestBase {

  @Test
  public void twoWayCast() throws Throwable {

    // Function checks for casting from Float, Double to Decimal data types
    try (RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();
         Drillbit bit = new Drillbit(CONFIG, serviceSet);
         DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator())) {

      // run query.
      bit.run();
      client.connect();
      List<QueryDataBatch> results = client.runQuery(org.apache.drill.exec.proto.UserBitShared.QueryType.PHYSICAL,
          Files.asCharSource(DrillFileUtils.getResourceAsFile("/functions/cast/two_way_implicit_cast.json"), Charsets.UTF_8).read());

      RecordBatchLoader batchLoader = new RecordBatchLoader(bit.getContext().getAllocator());

      QueryDataBatch batch = results.get(0);
      assertTrue(batchLoader.load(batch.getHeader().getDef(), batch.getData()));

      Iterator<VectorWrapper<?>> itr = batchLoader.iterator();

      ValueVector.Accessor intAccessor1 = itr.next().getValueVector().getAccessor();
      ValueVector.Accessor varcharAccessor1 = itr.next().getValueVector().getAccessor();

      for (int i = 0; i < intAccessor1.getValueCount(); i++) {
        assertEquals(intAccessor1.getObject(i), 10);
        assertEquals(varcharAccessor1.getObject(i).toString(), "101");
      }

      batchLoader.clear();
      for (QueryDataBatch result : results) {
        result.release();
      }
    }
  }
}
