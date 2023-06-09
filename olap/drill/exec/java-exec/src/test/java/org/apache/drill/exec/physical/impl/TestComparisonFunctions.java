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
import org.junit.Test;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.io.Resources;

import org.mockito.Mockito;

public class TestComparisonFunctions extends ExecTest {
  private final DrillConfig c = DrillConfig.create();
  private final String COMPARISON_TEST_PHYSICAL_PLAN = "functions/comparisonTest.json";
  private PhysicalPlanReader reader;
  private FunctionImplementationRegistry registry;

  public void runTest(String expression, int expectedResults) throws Throwable {
    final DrillbitContext bitContext = mockDrillbitContext();
    final UserClientConnection connection = Mockito.mock(UserClientConnection.class);

    final String planString = Resources.toString(Resources.getResource(COMPARISON_TEST_PHYSICAL_PLAN), Charsets.UTF_8).replaceAll("EXPRESSION", expression);
    if (reader == null) {
      reader = PhysicalPlanReaderTestFactory.defaultPhysicalPlanReader(c);
    }
    if (registry == null) {
      registry = new FunctionImplementationRegistry(c);
    }
    final FragmentContextImpl context =
        new FragmentContextImpl(bitContext, PlanFragment.getDefaultInstance(), connection, registry);
    final PhysicalPlan plan = reader.readPhysicalPlan(planString);
    final SimpleRootExec exec = new SimpleRootExec(ImplCreator.getExec(context, (FragmentRoot) plan.getSortedOperators(false).iterator().next()));

    while(exec.next()) {
      assertEquals(String.format("Expression: %s;", expression), expectedResults,
          exec.getSelectionVector2().getCount());
    }

    exec.close();
    context.close();

    if (context.getExecutorState().getFailureCause() != null) {
      throw context.getExecutorState().getFailureCause();
    }

    assertTrue(!context.getExecutorState().isFailed());
  }

  @Test
  public void testInt() throws Throwable {
    runTest("intColumn == intColumn", 100);
    runTest("intColumn != intColumn", 0);
    runTest("intColumn > intColumn", 0);
    runTest("intColumn < intColumn", 0);
    runTest("intColumn >= intColumn", 100);
    runTest("intColumn <= intColumn", 100);
  }

  @Test
  public void testBigInt() throws Throwable {
    runTest("bigIntColumn == bigIntColumn", 100);
    runTest("bigIntColumn != bigIntColumn", 0);
    runTest("bigIntColumn > bigIntColumn", 0);
    runTest("bigIntColumn < bigIntColumn", 0);
    runTest("bigIntColumn >= bigIntColumn", 100);
    runTest("bigIntColumn <= bigIntColumn", 100);
  }

  @Test
  public void testFloat4() throws Throwable {
    runTest("float4Column == float4Column", 100);
    runTest("float4Column != float4Column", 0);
    runTest("float4Column > float4Column", 0);
    runTest("float4Column < float4Column", 0);
    runTest("float4Column >= float4Column", 100);
    runTest("float4Column <= float4Column", 100);
  }

  @Test
  public void testFloat8() throws Throwable {
    runTest("float8Column == float8Column", 100);
    runTest("float8Column != float8Column", 0);
    runTest("float8Column > float8Column", 0);
    runTest("float8Column < float8Column", 0);
    runTest("float8Column >= float8Column", 100);
    runTest("float8Column <= float8Column", 100);
  }

  @Test
  public void testIntNullable() throws Throwable {
    runTest("intNullableColumn == intNullableColumn", 50);
    runTest("intNullableColumn != intNullableColumn", 0);
    runTest("intNullableColumn > intNullableColumn", 0);
    runTest("intNullableColumn < intNullableColumn", 0);
    runTest("intNullableColumn >= intNullableColumn", 50);
    runTest("intNullableColumn <= intNullableColumn", 50);
  }

  @Test
  public void testBigIntNullable() throws Throwable {
    runTest("bigIntNullableColumn == bigIntNullableColumn", 50);
    runTest("bigIntNullableColumn != bigIntNullableColumn", 0);
    runTest("bigIntNullableColumn > bigIntNullableColumn", 0);
    runTest("bigIntNullableColumn < bigIntNullableColumn", 0);
    runTest("bigIntNullableColumn >= bigIntNullableColumn", 50);
    runTest("bigIntNullableColumn <= bigIntNullableColumn", 50);
  }
}
