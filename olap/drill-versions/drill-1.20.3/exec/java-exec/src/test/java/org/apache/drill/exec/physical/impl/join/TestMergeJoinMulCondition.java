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
package org.apache.drill.exec.physical.impl.join;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.apache.drill.categories.OperatorTest;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.test.TestTools;
import org.apache.drill.exec.client.DrillClient;
import org.apache.drill.exec.pop.PopUnitTestBase;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.exec.server.Drillbit;
import org.apache.drill.exec.server.RemoteServiceSet;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestRule;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.io.Files;

@Category({SlowTest.class, OperatorTest.class})
public class TestMergeJoinMulCondition extends PopUnitTestBase {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestMergeJoinMulCondition.class);

  @Rule public final TestRule TIMEOUT = TestTools.getTimeoutRule(200000);

  @Test
  //the physical plan is obtained for the following SQL query:
  //  "select l.l_partkey, l.l_suppkey, ps.ps_partkey, ps.ps_suppkey "
  //      + " from cp.`tpch/lineitem.parquet` l join "
  //      + "      cp.`tpch/partsupp.parquet` ps"
  //      + " on l.l_partkey = ps.ps_partkey and "
  //      + "    l.l_suppkey = ps.ps_suppkey";
  public void testMergeJoinMultiKeys() throws Exception {
    RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();

    try (Drillbit bit1 = new Drillbit(CONFIG, serviceSet);
        DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator())) {

      bit1.run();
      client.connect();
      List<QueryDataBatch> results = client.runQuery(org.apache.drill.exec.proto.UserBitShared.QueryType.PHYSICAL,
          Files.asCharSource(DrillFileUtils.getResourceAsFile("/join/mj_multi_condition.json"),
              Charsets.UTF_8).read());
      int count = 0;
      for (QueryDataBatch b : results) {
        if (b.getHeader().getRowCount() != 0) {
          count += b.getHeader().getRowCount();
        }
        b.release();
      }
      assertEquals(60175, count);
    }
  }

  @Test
  // The physical plan is obtained through sql:
  // alter session set `planner.enable_hashjoin`=false;
  // select * from cp.`region.json` t1, cp.`region.json` t2 where t1.non_exist = t2.non_exist2 ;
  public void testMergeJoinInnerNullKey() throws Exception {
    RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();

    try (Drillbit bit1 = new Drillbit(CONFIG, serviceSet);
        DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator())) {
      bit1.run();
      client.connect();
      List<QueryDataBatch> results = client.runQuery(org.apache.drill.exec.proto.UserBitShared.QueryType.PHYSICAL,
          Files.asCharSource(DrillFileUtils.getResourceAsFile("/join/merge_join_nullkey.json"), Charsets.UTF_8).read().replace("${JOIN_TYPE}", "INNER"));
      int count = 0;
      for (QueryDataBatch b : results) {
        if (b.getHeader().getRowCount() != 0) {
          count += b.getHeader().getRowCount();
        }
        b.release();
      }
      assertEquals(0, count);
    }
  }

  @Test
  // The physical plan is obtained through sql:
  // alter session set `planner.enable_hashjoin`=false;
  // select * from cp.`region.json` t1 left outer join cp.`region.json` t2 on  t1.non_exist = t2.non_exist2 ;
  public void testMergeJoinLeftOuterNullKey() throws Exception {
    RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();

    try (Drillbit bit1 = new Drillbit(CONFIG, serviceSet);
        DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator())) {

      bit1.run();
      client.connect();
      List<QueryDataBatch> results = client.runQuery(org.apache.drill.exec.proto.UserBitShared.QueryType.PHYSICAL,
          Files.asCharSource(DrillFileUtils.getResourceAsFile("/join/merge_join_nullkey.json"), Charsets.UTF_8).read().replace("${JOIN_TYPE}", "LEFT"));
      int count = 0;
      for (QueryDataBatch b : results) {
        if (b.getHeader().getRowCount() != 0) {
          count += b.getHeader().getRowCount();
        }
        b.release();
      }
      assertEquals(110, count);
    }
  }

}
