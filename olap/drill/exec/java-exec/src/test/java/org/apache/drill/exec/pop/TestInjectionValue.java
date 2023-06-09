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

import java.util.List;

import org.apache.drill.categories.PlannerTest;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.ExecTest;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.config.Screen;
import org.apache.drill.exec.planner.PhysicalPlanReader;
import org.apache.drill.exec.planner.PhysicalPlanReaderTestFactory;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.io.Files;
import org.junit.experimental.categories.Category;

@Category(PlannerTest.class)
public class TestInjectionValue extends ExecTest {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestInjectionValue.class);

  static DrillConfig config;

  @BeforeClass
  public static void setup(){
    config = DrillConfig.create();
  }

  @Test
  public void testInjected() throws Exception{
    PhysicalPlanReader r = PhysicalPlanReaderTestFactory.defaultPhysicalPlanReader(config);
    PhysicalPlan p = r.readPhysicalPlan(Files.asCharSource(DrillFileUtils.getResourceAsFile("/physical_screen.json"), Charsets.UTF_8).read());

    List<PhysicalOperator> o = p.getSortedOperators(false);

    PhysicalOperator op = o.iterator().next();
    assertEquals(Screen.class, op.getClass());
    Screen s = (Screen) op;
    assertEquals(DrillbitEndpoint.getDefaultInstance(), s.getEndpoint());
  }
}
