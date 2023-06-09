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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.apache.drill.categories.PlannerTest;
import org.apache.drill.exec.exception.FragmentSetupException;
import org.apache.drill.exec.planner.PhysicalPlanReader;
import org.apache.drill.exec.planner.PhysicalPlanReaderTestFactory;
import org.apache.drill.exec.planner.fragment.Fragment;
import org.apache.drill.exec.work.foreman.ForemanSetupException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(PlannerTest.class)
public class TestFragmenter extends PopUnitTestBase {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestFragmenter.class);


  @Test
  public void ensureOneFragment() throws FragmentSetupException, IOException, ForemanSetupException {
    PhysicalPlanReader ppr = PhysicalPlanReaderTestFactory.defaultPhysicalPlanReader(CONFIG);
    Fragment b = getRootFragment(ppr, "/physical_test1.json");
    assertEquals(1, getFragmentCount(b));
    assertEquals(0, b.getReceivingExchangePairs().size());
    assertNull(b.getSendingExchange());
  }

  @Test
  public void ensureThreeFragments() throws FragmentSetupException, IOException, ForemanSetupException {
    PhysicalPlanReader ppr = PhysicalPlanReaderTestFactory.defaultPhysicalPlanReader(CONFIG);
    Fragment b = getRootFragment(ppr, "/physical_double_exchange.json");
    logger.debug("Fragment Node {}", b);
    assertEquals(3, getFragmentCount(b));
    assertEquals(1, b.getReceivingExchangePairs().size());
    assertNull(b.getSendingExchange());

    // get first child.
    b = b.iterator().next().getNode();
    assertEquals(1, b.getReceivingExchangePairs().size());
    assertNotNull(b.getSendingExchange());

    b = b.iterator().next().getNode();
    assertEquals(0, b.getReceivingExchangePairs().size());
    assertNotNull(b.getSendingExchange());
  }








}
