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

import java.util.List;

import org.apache.drill.categories.OperatorTest;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.client.DrillClient;
import org.apache.drill.exec.pop.PopUnitTestBase;
import org.apache.drill.exec.proto.UserBitShared.QueryType;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.exec.server.Drillbit;
import org.apache.drill.exec.server.RemoteServiceSet;
import org.junit.Test;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.io.Files;
import org.junit.experimental.categories.Category;

@Category({OperatorTest.class, SlowTest.class})
public class TestBroadcastExchange extends PopUnitTestBase {
  @Test
  public void TestSingleBroadcastExchangeWithTwoScans() throws Exception {
    RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();

    try (Drillbit bit1 = new Drillbit(CONFIG, serviceSet);
        Drillbit bit2 = new Drillbit(CONFIG, serviceSet);
        DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator())) {

      bit1.run();
      bit2.run();
      client.connect();

      String physicalPlan = Files.asCharSource(
              DrillFileUtils.getResourceAsFile("/sender/broadcast_exchange.json"), Charsets.UTF_8).read()
              .replace("#{LEFT_FILE}", DrillFileUtils.getResourceAsFile("/join/merge_single_batch.left.json").toURI().toString())
              .replace("#{RIGHT_FILE}", DrillFileUtils.getResourceAsFile("/join/merge_single_batch.right.json").toURI().toString());
      List<QueryDataBatch> results = client.runQuery(QueryType.PHYSICAL, physicalPlan);
      int count = 0;
      for (QueryDataBatch b : results) {
        if (b.getHeader().getRowCount() != 0) {
          count += b.getHeader().getRowCount();
        }
        b.release();
      }
      assertEquals(25, count);
    }
  }

  @Test
  public void TestMultipleSendLocationBroadcastExchange() throws Exception {
    RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();

    try (Drillbit bit1 = new Drillbit(CONFIG, serviceSet);
        Drillbit bit2 = new Drillbit(CONFIG, serviceSet);
        DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator())) {

      bit1.run();
      bit2.run();
      client.connect();

      String physicalPlan = Files.asCharSource(
          DrillFileUtils.getResourceAsFile("/sender/broadcast_exchange_long_run.json"), Charsets.UTF_8).read();
      List<QueryDataBatch> results = client.runQuery(QueryType.PHYSICAL, physicalPlan);
      int count = 0;
      for (QueryDataBatch b : results) {
        if (b.getHeader().getRowCount() != 0) {
          count += b.getHeader().getRowCount();
        }
        b.release();
      }
      // Nothing done with count?
    }
  }

}
