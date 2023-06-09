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
package org.apache.drill.exec.physical.impl.agg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.drill.categories.OperatorTest;
import org.apache.drill.common.config.DrillConfig;
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
import org.apache.drill.exec.vector.NullableBigIntVector;
import org.junit.Test;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.io.Files;

import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

@Category(OperatorTest.class)
public class TestAgg extends ExecTest {
  private final DrillConfig c = DrillConfig.create();

  private SimpleRootExec doTest(String file) throws Exception {
    final DrillbitContext bitContext = mockDrillbitContext();
    final UserClientConnection connection = Mockito.mock(UserClientConnection.class);

    final PhysicalPlanReader reader = PhysicalPlanReaderTestFactory.defaultPhysicalPlanReader(c);
    final PhysicalPlan plan = reader.readPhysicalPlan(Files.asCharSource(DrillFileUtils.getResourceAsFile(file), Charsets.UTF_8).read());
    final FunctionImplementationRegistry registry = new FunctionImplementationRegistry(c);
    final FragmentContextImpl context = new FragmentContextImpl(bitContext, PlanFragment.getDefaultInstance(), connection, registry);
    final SimpleRootExec exec = new SimpleRootExec(ImplCreator.getExec(context, (FragmentRoot) plan.getSortedOperators(false).iterator().next()));
    return exec;
  }

  @Test
  public void oneKeyAgg() throws Throwable {
    final SimpleRootExec exec = doTest("/agg/test1.json");

    while(exec.next()) {
      final BigIntVector cnt = exec.getValueVectorById(SchemaPath.getSimplePath("cnt"), BigIntVector.class);
      final IntVector key = exec.getValueVectorById(SchemaPath.getSimplePath("blue"), IntVector.class);
      final long[] cntArr = {10001, 9999};
      final int[] keyArr = {Integer.MIN_VALUE, Integer.MAX_VALUE};

      for(int i = 0; i < exec.getRecordCount(); i++) {
        assertEquals((Long) cntArr[i], cnt.getAccessor().getObject(i));
        assertEquals((Integer) keyArr[i], key.getAccessor().getObject(i));
      }
    }

    if(exec.getContext().getExecutorState().getFailureCause() != null) {
      throw exec.getContext().getExecutorState().getFailureCause();
    }

    assertTrue(!exec.getContext().getExecutorState().isFailed());
  }

  @Test
  public void twoKeyAgg() throws Throwable {
    SimpleRootExec exec = doTest("/agg/twokey.json");

    while(exec.next()) {
      final IntVector key1 = exec.getValueVectorById(SchemaPath.getSimplePath("key1"), IntVector.class);
      final BigIntVector key2 = exec.getValueVectorById(SchemaPath.getSimplePath("key2"), BigIntVector.class);
      final BigIntVector cnt = exec.getValueVectorById(SchemaPath.getSimplePath("cnt"), BigIntVector.class);
      final NullableBigIntVector total = exec.getValueVectorById(SchemaPath.getSimplePath("total"), NullableBigIntVector.class);
      final Integer[] keyArr1 = {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
      final long[] keyArr2 = {0,1,2,0,1,2};
      final long[] cntArr = {34,34,34,34,34,34};
      final long[] totalArr = {0,34,68,0,34,68};

      for(int i = 0; i < exec.getRecordCount(); i++) {
        assertEquals((Long) cntArr[i], cnt.getAccessor().getObject(i));
        assertEquals(keyArr1[i], key1.getAccessor().getObject(i));
        assertEquals((Long) keyArr2[i], key2.getAccessor().getObject(i));
        assertEquals((Long) totalArr[i], total.getAccessor().getObject(i));
      }
    }

    if(exec.getContext().getExecutorState().getFailureCause() != null){
      throw exec.getContext().getExecutorState().getFailureCause();
    }
    assertTrue(!exec.getContext().getExecutorState().isFailed());
  }
}
