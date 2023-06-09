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
package org.apache.drill.exec.physical.impl.mergereceiver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.drill.categories.OperatorTest;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.client.DrillClient;
import org.apache.drill.exec.pop.PopUnitTestBase;
import org.apache.drill.exec.proto.UserBitShared.QueryData;
import org.apache.drill.exec.record.MaterializedField;
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

@Category({OperatorTest.class, SlowTest.class})
public class TestMergingReceiver extends PopUnitTestBase {
  // private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestMergingReceiver.class);

  @Test
  public void twoBitTwoExchange() throws Exception {
    final RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();

    try (final Drillbit bit1 = new Drillbit(CONFIG, serviceSet);
        final Drillbit bit2 = new Drillbit(CONFIG, serviceSet);
        final DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator())) {
      bit1.run();
      bit2.run();
      client.connect();
      final List<QueryDataBatch> results = client.runQuery(org.apache.drill.exec.proto.UserBitShared.QueryType.PHYSICAL,
        Files.asCharSource(DrillFileUtils.getResourceAsFile("/mergerecv/merging_receiver.json"),
          Charsets.UTF_8).read());
      int count = 0;
      final RecordBatchLoader batchLoader = new RecordBatchLoader(client.getAllocator());
      // print the results
      for (final QueryDataBatch b : results) {
        final QueryData queryData = b.getHeader();
        final int rowCount = queryData.getRowCount();
        count += rowCount;
        batchLoader.load(queryData.getDef(), b.getData()); // loaded but not used, just to test
        b.release();
        batchLoader.clear();
      }
      assertEquals(200000, count);
    }
  }

  @Test
  public void testMultipleProvidersMixedSizes() throws Exception {
    final RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();

    try (final Drillbit bit1 = new Drillbit(CONFIG, serviceSet);
        final Drillbit bit2 = new Drillbit(CONFIG, serviceSet);
        final DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator())) {

      bit1.run();
      bit2.run();
      client.connect();
      final List<QueryDataBatch> results =
          client.runQuery(org.apache.drill.exec.proto.UserBitShared.QueryType.PHYSICAL,
              Files.asCharSource(DrillFileUtils.getResourceAsFile("/mergerecv/multiple_providers.json"),
                  Charsets.UTF_8).read());
      int count = 0;
      final RecordBatchLoader batchLoader = new RecordBatchLoader(client.getAllocator());
      // print the results
      Long lastBlueValue = null;
      for (final QueryDataBatch b : results) {
        final QueryData queryData = b.getHeader();
        final int batchRowCount = queryData.getRowCount();
        count += batchRowCount;
        batchLoader.load(queryData.getDef(), b.getData());
        for (final VectorWrapper<?> vw : batchLoader) {
          final ValueVector vv = vw.getValueVector();
          final ValueVector.Accessor va = vv.getAccessor();
          final MaterializedField materializedField = vv.getField();
          final int numValues = va.getValueCount();
          for (int valueIdx = 0; valueIdx < numValues; ++valueIdx) {
            if (materializedField.getName().equals("blue")) {
              final long longValue = (Long) va.getObject(valueIdx);
              // check that order is ascending
              if (lastBlueValue != null) {
                assertTrue(longValue >= lastBlueValue);
              }
              lastBlueValue = longValue;
            }
          }
        }
        b.release();
        batchLoader.clear();
      }
      assertEquals(400000, count);
    }
  }

  @Test
  public void handleEmptyBatch() throws Exception {
    final RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();

    try (final Drillbit bit1 = new Drillbit(CONFIG, serviceSet);
        final Drillbit bit2 = new Drillbit(CONFIG, serviceSet);
        final DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator())) {

      bit1.run();
      bit2.run();
      client.connect();
      final List<QueryDataBatch> results =
          client.runQuery(org.apache.drill.exec.proto.UserBitShared.QueryType.PHYSICAL,
              Files.asCharSource(DrillFileUtils.getResourceAsFile("/mergerecv/empty_batch.json"),
                  Charsets.UTF_8).read());
      int count = 0;
      final RecordBatchLoader batchLoader = new RecordBatchLoader(client.getAllocator());
      // print the results
      for (final QueryDataBatch b : results) {
        final QueryData queryData = b.getHeader();
        batchLoader.load(queryData.getDef(), b.getData()); // loaded but not used, for testing
        count += queryData.getRowCount();
        b.release();
        batchLoader.clear();
      }
      assertEquals(100000, count);
    }
  }

  @Test
  public void handleEmptyBatchNoSchema() throws Exception {
    final RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();

    try (final Drillbit bit1 = new Drillbit(CONFIG, serviceSet);
         final Drillbit bit2 = new Drillbit(CONFIG, serviceSet);
         final DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator());) {

      bit1.run();
      bit2.run();
      client.connect();
      final List<QueryDataBatch> results =
          client.runQuery(org.apache.drill.exec.proto.UserBitShared.QueryType.PHYSICAL,
              Files.asCharSource(DrillFileUtils.getResourceAsFile("/mergerecv/empty_batch_noschema.json"),
                  Charsets.UTF_8).read());
      int count = 0;
      final RecordBatchLoader batchLoader = new RecordBatchLoader(client.getAllocator());
      // print the results
      for (final QueryDataBatch b : results) {
        final QueryData queryData = b.getHeader();
        batchLoader.load(queryData.getDef(), b.getData()); // loaded but not used, for testing
        count += queryData.getRowCount();
        b.release();
        batchLoader.clear();
      }
      assertEquals(100000, count);
    }
  }

  @Test
  public void testMultipleProvidersEmptyBatches() throws Exception {
    final RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();

    try (final Drillbit bit1 = new Drillbit(CONFIG, serviceSet);
         final Drillbit bit2 = new Drillbit(CONFIG, serviceSet);
         final DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator())) {

      bit1.run();
      bit2.run();
      client.connect();
      final List<QueryDataBatch> results =
          client.runQuery(org.apache.drill.exec.proto.UserBitShared.QueryType.PHYSICAL,
              Files.asCharSource(DrillFileUtils.getResourceAsFile("/mergerecv/multiple_providers_empty_batches.json"),
                  Charsets.UTF_8).read());
      int count = 0;
      final RecordBatchLoader batchLoader = new RecordBatchLoader(client.getAllocator());
      // print the results
      Long lastBlueValue = null;
      for (final QueryDataBatch b : results) {
        final QueryData queryData = b.getHeader();
        final int batchRowCount = queryData.getRowCount();
        count += batchRowCount;
        batchLoader.load(queryData.getDef(), b.getData());
        for (final VectorWrapper<?> vw : batchLoader) {
          final ValueVector vv = vw.getValueVector();
          final ValueVector.Accessor va = vv.getAccessor();
          final MaterializedField materializedField = vv.getField();
          final int numValues = va.getValueCount();
          for (int valueIdx = 0; valueIdx < numValues; ++valueIdx) {
            if (materializedField.getName().equals("blue")) {
              final long longValue = (Long) va.getObject(valueIdx);
              // check that order is ascending
              if (lastBlueValue != null) {
                assertTrue(longValue >= lastBlueValue);
              }
              lastBlueValue = longValue;
            }
          }
        }
        b.release();
        batchLoader.clear();
      }
      assertEquals(300000, count);
    }
  }
}
