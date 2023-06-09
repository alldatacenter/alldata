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
package org.apache.drill.exec.physical.impl.sort;

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
import org.apache.drill.exec.proto.BitControl.PlanFragment;
import org.apache.drill.exec.rpc.UserClientConnection;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.vector.BigIntVector;
import org.apache.drill.exec.vector.IntVector;
import org.junit.Ignore;
import org.junit.Test;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.io.Files;

import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

@Ignore
@Category(OperatorTest.class)
public class TestSimpleSort extends ExecTest {
  private final DrillConfig c = DrillConfig.create();

  @Test
  public void sortOneKeyAscending() throws Throwable {
    final DrillbitContext bitContext = mockDrillbitContext();
    final UserClientConnection connection = Mockito.mock(UserClientConnection.class);

    final PhysicalPlanReader reader = PhysicalPlanReaderTestFactory.defaultPhysicalPlanReader(c);
    final PhysicalPlan plan = reader.readPhysicalPlan(Files.asCharSource(DrillFileUtils.getResourceAsFile("/sort/one_key_sort.json"), Charsets.UTF_8).read());
    final FunctionImplementationRegistry registry = new FunctionImplementationRegistry(c);
    final FragmentContextImpl context = new FragmentContextImpl(bitContext, PlanFragment.getDefaultInstance(), connection, registry);
    final SimpleRootExec exec = new SimpleRootExec(ImplCreator.getExec(context, (FragmentRoot) plan.getSortedOperators(false).iterator().next()));

    int previousInt = Integer.MIN_VALUE;
    int recordCount = 0;
    int batchCount = 0;

    while(exec.next()) {
      batchCount++;
      final IntVector c1 = exec.getValueVectorById(new SchemaPath("blue", ExpressionPosition.UNKNOWN), IntVector.class);
      final IntVector c2 = exec.getValueVectorById(new SchemaPath("green", ExpressionPosition.UNKNOWN), IntVector.class);

      final IntVector.Accessor a1 = c1.getAccessor();
      final IntVector.Accessor a2 = c2.getAccessor();

      for(int i =0; i < c1.getAccessor().getValueCount(); i++) {
        recordCount++;
        assertTrue(previousInt <= a1.get(i));
        previousInt = a1.get(i);
        assertEquals(previousInt, a2.get(i));
      }
    }

    if(context.getExecutorState().getFailureCause() != null) {
      throw context.getExecutorState().getFailureCause();
    }
    assertTrue(!context.getExecutorState().isFailed());
  }

  @Test
  public void sortTwoKeysOneAscendingOneDescending() throws Throwable {
    final DrillbitContext bitContext = mockDrillbitContext();
    final UserClientConnection connection = Mockito.mock(UserClientConnection.class);

    final PhysicalPlanReader reader = PhysicalPlanReaderTestFactory.defaultPhysicalPlanReader(c);
    final PhysicalPlan plan = reader.readPhysicalPlan(Files.asCharSource(DrillFileUtils.getResourceAsFile("/sort/two_key_sort.json"), Charsets.UTF_8).read());
    final FunctionImplementationRegistry registry = new FunctionImplementationRegistry(c);
    final FragmentContextImpl context = new FragmentContextImpl(bitContext, PlanFragment.getDefaultInstance(), connection, registry);
    final SimpleRootExec exec = new SimpleRootExec(ImplCreator.getExec(context, (FragmentRoot) plan.getSortedOperators(false).iterator().next()));

    int previousInt = Integer.MIN_VALUE;
    long previousLong = Long.MAX_VALUE;

    int recordCount = 0;
    int batchCount = 0;

    while(exec.next()) {
      batchCount++;
      final IntVector c1 = exec.getValueVectorById(new SchemaPath("blue", ExpressionPosition.UNKNOWN), IntVector.class);
      final BigIntVector c2 = exec.getValueVectorById(new SchemaPath("alt", ExpressionPosition.UNKNOWN), BigIntVector.class);

      final IntVector.Accessor a1 = c1.getAccessor();
      final BigIntVector.Accessor a2 = c2.getAccessor();

      for(int i =0; i < c1.getAccessor().getValueCount(); i++) {
        recordCount++;
        assertTrue(previousInt <= a1.get(i));

        if(previousInt != a1.get(i)) {
          previousLong = Long.MAX_VALUE;
          previousInt = a1.get(i);
        }

        assertTrue(previousLong >= a2.get(i));
      }
    }

    if(context.getExecutorState().getFailureCause() != null) {
      throw context.getExecutorState().getFailureCause();
    }
    assertTrue(!context.getExecutorState().isFailed());
  }
}
