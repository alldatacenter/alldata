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
package org.apache.drill.exec.fn.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.drill.categories.OperatorTest;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.client.DrillClient;
import org.apache.drill.exec.pop.PopUnitTestBase;
import org.apache.drill.exec.proto.UserBitShared.QueryType;
import org.apache.drill.exec.record.RecordBatchLoader;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.exec.server.Drillbit;
import org.apache.drill.exec.server.RemoteServiceSet;
import org.apache.drill.exec.vector.ValueVector;
import org.junit.Test;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.io.Files;
import org.junit.experimental.categories.Category;

@Category(OperatorTest.class)
public class TestAggregateFunction extends PopUnitTestBase {

  public void runTest(Object[] values, String planPath, String dataPath) throws Throwable {

    try (RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();
         Drillbit bit = new Drillbit(CONFIG, serviceSet);
         DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator())) {

      // run query.
      bit.run();
      client.connect();
      List<QueryDataBatch> results = client.runQuery(QueryType.PHYSICAL,
          Files.asCharSource(DrillFileUtils.getResourceAsFile(planPath), Charsets.UTF_8).read().replace("#{TEST_FILE}", dataPath));

      RecordBatchLoader batchLoader = new RecordBatchLoader(bit.getContext().getAllocator());

      QueryDataBatch batch = results.get(1);
      assertTrue(batchLoader.load(batch.getHeader().getDef(), batch.getData()));

      int i = 0;
      for (VectorWrapper<?> v : batchLoader) {
        ValueVector.Accessor accessor = v.getValueVector().getAccessor();
        assertEquals(values[i++], (accessor.getObject(0)));
      }

      batchLoader.clear();
      for(QueryDataBatch b : results) {
        b.release();
      }
    }
  }

  @Test
  public void testSortDate() throws Throwable {
    String planPath = "/functions/test_stddev_variance.json";
    String dataPath = "/simple_stddev_variance_input.json";
    Double expectedValues[] = {2.0d, 2.138089935299395d, 2.138089935299395d, 4.0d, 4.571428571428571d, 4.571428571428571d};

    runTest(expectedValues, planPath, dataPath);
  }

  @Test
  public void testCovarianceCorrelation() throws Throwable {
    String planPath = "/functions/test_covariance.json";
    String dataPath = "/covariance_input.json";
    Double[] expectedValues = {4.571428571428571d, 4.857142857142857d, -6.000000000000002d, 4.0d, 4.25d, -5.250000000000002d, 1.0d, 0.9274260335029677d, -1.0000000000000004d};

    runTest(expectedValues, planPath, dataPath);
  }

}
