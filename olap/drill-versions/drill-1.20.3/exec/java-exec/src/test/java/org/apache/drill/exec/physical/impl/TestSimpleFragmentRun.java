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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.client.DrillClient;
import org.apache.drill.exec.memory.RootAllocatorFactory;
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

public class TestSimpleFragmentRun extends PopUnitTestBase {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestSimpleFragmentRun.class);

  @Test
  public void runNoExchangeFragment() throws Exception {
    try (final RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();
        final Drillbit bit = new Drillbit(CONFIG, serviceSet);
        final DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator())) {

    // run query.
    bit.run();
    client.connect();
    final String path = "/physical_test2.json";
    final List<QueryDataBatch> results = client.runQuery(QueryType.PHYSICAL, Files.asCharSource(DrillFileUtils.getResourceAsFile(path), Charsets.UTF_8).read());

    // look at records
    final RecordBatchLoader batchLoader = new RecordBatchLoader(client.getAllocator());
    final StringBuilder sb = new StringBuilder();

    int recordCount = 0;
    for (final QueryDataBatch batch : results) {
      final boolean schemaChanged = batchLoader.load(batch.getHeader().getDef(), batch.getData());
      boolean firstColumn = true;

      // print headers.
      if (schemaChanged) {
        sb.append("\n\n========NEW SCHEMA=========\n\n");
        for (final VectorWrapper<?> value : batchLoader) {

          if (firstColumn) {
            firstColumn = false;
          } else {
            sb.append("\t");
          }
          sb.append(value.getField().getName());
          sb.append("[");
          sb.append(value.getField().getType().getMinorType());
          sb.append("]");
        }
        sb.append('\n');
      }

      for (int i = 0; i < batchLoader.getRecordCount(); i++) {
        boolean first = true;
        recordCount++;
        for (final VectorWrapper<?> value : batchLoader) {
          if (first) {
            first = false;
          } else {
            sb.append("\t");
          }
          sb.append(value.getValueVector().getAccessor().getObject(i));
        }
        if (!first) {
          sb.append('\n');
        }
      }
      batchLoader.clear();
      batch.release();
    }

    logger.debug(sb.toString());
    logger.debug("Received results {}", results);
    assertEquals(recordCount, 200);
    }
  }

  @Test
  public void runJSONScanPopFragment() throws Exception {
    try (final RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();
         final Drillbit bit = new Drillbit(CONFIG, serviceSet);
         final DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator())) {

      // run query.
      bit.run();
      client.connect();
      final List<QueryDataBatch> results = client.runQuery(QueryType.PHYSICAL,
          Files.asCharSource(DrillFileUtils.getResourceAsFile("/physical_json_scan_test1.json"), Charsets.UTF_8).read()
              .replace("#{TEST_FILE}", DrillFileUtils.getResourceAsFile("/scan_json_test_1.json").toURI().toString())
      );

      // look at records
      final RecordBatchLoader batchLoader = new RecordBatchLoader(RootAllocatorFactory.newRoot(CONFIG));
      int recordCount = 0;

      for (int i = 0; i < results.size(); ++i) {
        final QueryDataBatch batch = results.get(i);
        if (i == 0) {
          assertTrue(batch.hasData());
        } else {
          assertFalse(batch.hasData());
          batch.release();
          continue;
        }

        assertTrue(batchLoader.load(batch.getHeader().getDef(), batch.getData()));
        boolean firstColumn = true;

        // print headers.
        final StringBuilder sb = new StringBuilder();

        sb.append("\n\n========NEW SCHEMA=========\n\n");
        for (final VectorWrapper<?> v : batchLoader) {

          if (firstColumn) {
            firstColumn = false;
          } else {
            sb.append("\t");
          }
          sb.append(v.getField().getName());
          sb.append("[");
          sb.append(v.getField().getType().getMinorType());
          sb.append("]");
        }

        sb.append('\n');

        for (int r = 0; r < batchLoader.getRecordCount(); r++) {
          boolean first = true;
          recordCount++;
          for (final VectorWrapper<?> v : batchLoader) {
            if (first) {
              first = false;
            } else {
              sb.append("\t");
            }

            final ValueVector.Accessor accessor = v.getValueVector().getAccessor();
            sb.append(accessor.getObject(r));
          }
          if (!first) {
            sb.append('\n');
          }
        }
        batchLoader.clear();
        batch.release();
      }

      assertEquals(2, recordCount);
    }
  }
}
