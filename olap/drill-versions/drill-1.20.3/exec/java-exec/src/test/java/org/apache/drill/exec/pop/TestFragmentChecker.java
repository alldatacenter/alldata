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
package org.apache.drill.exec.pop;

import java.util.List;

import org.apache.drill.categories.PlannerTest;
import org.apache.drill.exec.planner.PhysicalPlanReader;
import org.apache.drill.exec.planner.PhysicalPlanReaderTestFactory;
import org.apache.drill.exec.planner.fragment.DefaultQueryParallelizer;
import org.apache.drill.exec.planner.fragment.Fragment;
import org.apache.drill.exec.planner.fragment.SimpleParallelizer;
import org.apache.drill.exec.proto.BitControl.QueryContextInformation;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.rpc.user.UserSession;
import org.apache.drill.exec.server.options.OptionList;
import org.apache.drill.exec.util.Utilities;
import org.apache.drill.exec.work.QueryWorkUnit;
import org.junit.Test;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;

@Category(PlannerTest.class)
public class TestFragmentChecker extends PopUnitTestBase{
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestFragmentChecker.class);

  @Test
  public void checkSimpleExchangePlan() throws Exception{
    print("/physical_double_exchange.json", 2, 3);

  }

  private void print(String fragmentFile, int bitCount, int expectedFragmentCount) throws Exception {
    PhysicalPlanReader ppr = PhysicalPlanReaderTestFactory.defaultPhysicalPlanReader(CONFIG);
    Fragment fragmentRoot = getRootFragment(ppr, fragmentFile);
    SimpleParallelizer par = new DefaultQueryParallelizer(true, 1000*1000, 5, 10, 1.2);
    List<DrillbitEndpoint> endpoints = Lists.newArrayList();
    DrillbitEndpoint localBit = null;
    for(int i =0; i < bitCount; i++) {
      DrillbitEndpoint b1 = DrillbitEndpoint.newBuilder().setAddress("localhost").setControlPort(1234+i).build();
      if (i == 0) {
        localBit = b1;
      }
      endpoints.add(b1);
    }

    final QueryContextInformation queryContextInfo = Utilities.createQueryContextInfo("dummySchemaName", "938ea2d9-7cb9-4baf-9414-a5a0b7777e8e");
    QueryWorkUnit qwu = par.generateWorkUnit(new OptionList(), localBit, QueryId.getDefaultInstance(), endpoints, fragmentRoot,
        UserSession.Builder.newBuilder().withCredentials(UserBitShared.UserCredentials.newBuilder().setUserName("foo").build()).build(),
        queryContextInfo);
    qwu.applyPlan(ppr);

    assertEquals(expectedFragmentCount,
        qwu.getFragments().size() + 1 /* root fragment is not part of the getFragments() list*/);
  }

  @Test
  public void validateSingleExchangeFragment() throws Exception{
    print("/physical_single_exchange.json", 1, 2);
  }
}
