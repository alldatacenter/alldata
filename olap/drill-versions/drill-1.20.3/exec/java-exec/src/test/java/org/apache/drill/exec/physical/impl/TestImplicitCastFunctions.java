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

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.ExecTest;
import org.apache.drill.exec.expr.fn.FunctionImplementationRegistry;
import org.apache.drill.exec.ops.FragmentContextImpl;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.physical.base.FragmentRoot;
import org.apache.drill.exec.planner.PhysicalPlanReader;
import org.apache.drill.exec.planner.PhysicalPlanReaderTestFactory;
import org.apache.drill.exec.proto.BitControl.PlanFragment;
import org.apache.drill.exec.rpc.UserClientConnection;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.vector.ValueVector;
import org.junit.Test;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.io.Resources;

import org.mockito.Mockito;

public class TestImplicitCastFunctions extends ExecTest {
  private final DrillConfig c = DrillConfig.create();
  private PhysicalPlanReader reader;
  private FunctionImplementationRegistry registry;
  private FragmentContextImpl context;

  public Object[] getRunResult(SimpleRootExec exec) {
    int size = 0;
    for (final ValueVector v : exec) {
      size++;
    }

    final Object[] res = new Object [size];
    int i = 0;
    for (final ValueVector v : exec) {
      res[i++] = v.getAccessor().getObject(0);
    }
    return res;
 }

  public void runTest(Object[] expectedResults, String planPath) throws Throwable {
    final DrillbitContext bitContext = mockDrillbitContext();
    final UserClientConnection connection = Mockito.mock(UserClientConnection.class);

    final String planString = Resources.toString(Resources.getResource(planPath), Charsets.UTF_8);
    if (reader == null) {
      reader = PhysicalPlanReaderTestFactory.defaultPhysicalPlanReader(c);
    }
    if (registry == null) {
      registry = new FunctionImplementationRegistry(c);
    }
    if (context == null) {
      context = new FragmentContextImpl(bitContext, PlanFragment.getDefaultInstance(), connection, registry);
    }
    final PhysicalPlan plan = reader.readPhysicalPlan(planString);
    final SimpleRootExec exec = new SimpleRootExec(ImplCreator.getExec(context, (FragmentRoot) plan.getSortedOperators(false).iterator().next()));


    exec.next(); // skip schema batch
    while (exec.next()) {
      final Object [] res = getRunResult(exec);
      assertEquals("return count does not match", res.length, expectedResults.length);

      for (int i = 0; i < res.length; i++) {
        assertEquals(String.format("column %s does not match", i),  res[i], expectedResults[i]);
      }
    }

    if (context.getExecutorState().getFailureCause() != null) {
      throw context.getExecutorState().getFailureCause();
    }

    assertTrue(!context.getExecutorState().isFailed());
  }

  @Test
  public void testImplicitCastWithConstant() throws Throwable{
    final Object [] expected = new Object[21];
    expected [0] = new Double (30.1);
    expected [1] = new Double (30.1);
    expected [2] = new Double (30.1);
    expected [3] = new Double (30.1);
    expected [4] = new Long (30);
    expected [5] = new Long (30);

    expected [6] = new Double (30.1);
    expected [7] = new Double (30.1);
    expected [8] = new Float (30.1);
    expected [9] = new Float (30.1);
    expected [10] = new Double (30.1);
    expected [11] = new Double (30.1);

    expected [12] = new Float (30.1);
    expected [13] = new Double (30.1);
    expected [14] = new Float (30.1);
    expected [15] = new Double (30.1);

    expected [16] = Boolean.TRUE;
    expected [17] = Boolean.TRUE;
    expected [18] = Boolean.TRUE;
    expected [19] = Boolean.TRUE;
    expected [20] = Boolean.TRUE;

    runTest(expected, "functions/cast/testICastConstant.json");
  }

  @Test
  public void testImplicitCastWithMockColumn() throws Throwable{
    final Object [] expected = new Object[5];
    expected [0] = new Integer (0);
    expected [1] = new Integer (0);
    expected [2] = new Float (-2.14748365E9);
    expected [3] = new Float (-2.14748365E9);
    expected [4] = new Double (-9.223372036854776E18);

    runTest(expected, "functions/cast/testICastMockCol.json");
  }

  @Test
  public void testImplicitCastWithNullExpression() throws Throwable{
    final Object [] expected = new Object[10];

    expected [0] = Boolean.TRUE;
    expected [1] = Boolean.FALSE;
    expected [2] = Boolean.FALSE;
    expected [3] = Boolean.TRUE;

    expected [4] = null;
    expected [5] = null;
    expected [6] = null;
    expected [7] = null;
    expected [8] = null;
    expected [9] = null;

    runTest(expected, "functions/cast/testICastNullExp.json");
  }
}
