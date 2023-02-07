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

import org.apache.drill.categories.OperatorTest;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.expression.ExpressionPosition;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.ExecTest;
import org.apache.drill.exec.expr.fn.FunctionImplementationRegistry;
import org.apache.drill.exec.ops.FragmentContextImpl;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.physical.base.FragmentRoot;
import org.apache.drill.exec.physical.impl.ImplCreator;
import org.apache.drill.exec.physical.impl.SimpleRootExec;
import org.apache.drill.exec.planner.PhysicalPlanReader;
import org.apache.drill.exec.planner.PhysicalPlanReaderTestFactory;
import org.apache.drill.exec.proto.BitControl;
import org.apache.drill.exec.rpc.UserClientConnection;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.vector.Float8Vector;
import org.apache.drill.exec.vector.IntVector;
import org.junit.Test;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.io.Files;

import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

@Category(OperatorTest.class)
public class TestMathFunctions extends ExecTest {
  private final DrillConfig c = DrillConfig.create();

  @Test
  public void testBasicMathFunctions() throws Throwable
  {
    final DrillbitContext bitContext = mockDrillbitContext();
    final UserClientConnection connection = Mockito.mock(UserClientConnection.class);

    final PhysicalPlanReader reader = PhysicalPlanReaderTestFactory.defaultPhysicalPlanReader(c);
    final PhysicalPlan plan = reader.readPhysicalPlan(Files.asCharSource(DrillFileUtils.getResourceAsFile("/functions/simple_math_functions.json"), Charsets.UTF_8).read());
    final FunctionImplementationRegistry registry = new FunctionImplementationRegistry(c);
    final FragmentContextImpl context = new FragmentContextImpl(bitContext, BitControl.PlanFragment.getDefaultInstance(), connection, registry);
    final SimpleRootExec exec = new SimpleRootExec(ImplCreator.getExec(context, (FragmentRoot) plan.getSortedOperators(false).iterator().next()));

    while(exec.next()) {
      final IntVector intMulVector = exec.getValueVectorById(new SchemaPath("INTMUL", ExpressionPosition.UNKNOWN), IntVector.class);
      final Float8Vector floatMulVector = exec.getValueVectorById(new SchemaPath("FLOATMUL", ExpressionPosition.UNKNOWN), Float8Vector.class);
      final IntVector intAddVector = exec.getValueVectorById(new SchemaPath("INTADD", ExpressionPosition.UNKNOWN), IntVector.class);
      final Float8Vector floatAddVector = exec.getValueVectorById(new SchemaPath("FLOATADD", ExpressionPosition.UNKNOWN), Float8Vector.class);
      assertEquals(exec.getRecordCount(), 1);
      assertEquals(intMulVector.getAccessor().get(0), 2);
      assertEquals(floatMulVector.getAccessor().get(0), (1.1 * 2.2), 0);
      assertEquals(intAddVector.getAccessor().get(0), 3);
      assertEquals(floatAddVector.getAccessor().get(0), (1.1 + 2.2), 0);
    }

    if(context.getExecutorState().getFailureCause() != null) {
      throw context.getExecutorState().getFailureCause();
    }
    assertTrue(!context.getExecutorState().isFailed());
  }
}
