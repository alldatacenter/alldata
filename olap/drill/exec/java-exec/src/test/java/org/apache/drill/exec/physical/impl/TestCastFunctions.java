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

import java.util.List;

import org.apache.drill.common.expression.ExpressionPosition;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.client.DrillClient;
import org.apache.drill.exec.expr.fn.FunctionImplementationRegistry;
import org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers;
import org.apache.drill.exec.expr.holders.BigIntHolder;
import org.apache.drill.exec.expr.holders.Float4Holder;
import org.apache.drill.exec.expr.holders.Float8Holder;
import org.apache.drill.exec.expr.holders.IntHolder;
import org.apache.drill.exec.expr.holders.VarBinaryHolder;
import org.apache.drill.exec.expr.holders.VarCharHolder;
import org.apache.drill.exec.ops.FragmentContextImpl;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.physical.base.FragmentRoot;
import org.apache.drill.exec.planner.PhysicalPlanReader;
import org.apache.drill.exec.planner.PhysicalPlanReaderTestFactory;
import org.apache.drill.exec.pop.PopUnitTestBase;
import org.apache.drill.exec.proto.BitControl.PlanFragment;
import org.apache.drill.exec.record.RecordBatchLoader;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.exec.rpc.UserClientConnection;
import org.apache.drill.exec.server.Drillbit;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.server.RemoteServiceSet;
import org.apache.drill.exec.vector.BigIntVector;
import org.apache.drill.exec.vector.Float4Vector;
import org.apache.drill.exec.vector.Float8Vector;
import org.apache.drill.exec.vector.IntVector;
import org.apache.drill.exec.vector.VarBinaryVector;
import org.apache.drill.exec.vector.VarCharVector;
import org.junit.Test;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.io.Files;

import org.mockito.Mockito;

public class TestCastFunctions extends PopUnitTestBase {
  @Test
  // cast to bigint.
  public void testCastBigInt() throws Throwable {
    final DrillbitContext bitContext = mockDrillbitContext();
    final UserClientConnection connection = Mockito.mock(UserClientConnection.class);

    final PhysicalPlanReader reader = PhysicalPlanReaderTestFactory.defaultPhysicalPlanReader(CONFIG);
    final PhysicalPlan plan = reader.readPhysicalPlan(Files.asCharSource(DrillFileUtils.getResourceAsFile("/functions/cast/testCastBigInt.json"), Charsets.UTF_8).read());
    final FunctionImplementationRegistry registry = new FunctionImplementationRegistry(CONFIG);
    final FragmentContextImpl context = new FragmentContextImpl(bitContext, PlanFragment.getDefaultInstance(), connection, registry);
    final SimpleRootExec exec = new SimpleRootExec(ImplCreator.getExec(context, (FragmentRoot) plan.getSortedOperators(false).iterator().next()));

    while(exec.next()) {
      final BigIntVector c0 = exec.getValueVectorById(new SchemaPath("varchar_cast", ExpressionPosition.UNKNOWN), BigIntVector.class);
      final BigIntVector.Accessor a0 = c0.getAccessor();

      int count = 0;
      for(int i = 0; i < c0.getAccessor().getValueCount(); i++) {
          BigIntHolder holder0 = new BigIntHolder();
          a0.get(i, holder0);
          assertEquals(1256, holder0.value);
          ++count;

      }
      assertEquals(5, count);
    }

    exec.close();

    context.close();

    if (context.getExecutorState().getFailureCause() != null) {
      throw context.getExecutorState().getFailureCause();
    }
    assertTrue(!context.getExecutorState().isFailed());
  }

  @Test
  //cast to int
  public void testCastInt() throws Throwable {
    final DrillbitContext bitContext = mockDrillbitContext();
    final UserClientConnection connection = Mockito.mock(UserClientConnection.class);

    final PhysicalPlanReader reader = PhysicalPlanReaderTestFactory.defaultPhysicalPlanReader(CONFIG);
    final PhysicalPlan plan = reader.readPhysicalPlan(Files.asCharSource(DrillFileUtils.getResourceAsFile("/functions/cast/testCastInt.json"), Charsets.UTF_8).read());
    final FunctionImplementationRegistry registry = new FunctionImplementationRegistry(CONFIG);
    final FragmentContextImpl context = new FragmentContextImpl(bitContext, PlanFragment.getDefaultInstance(), connection, registry);
    final SimpleRootExec exec = new SimpleRootExec(ImplCreator.getExec(context, (FragmentRoot) plan.getSortedOperators(false).iterator().next()));

    while(exec.next()) {
      final IntVector c0 = exec.getValueVectorById(new SchemaPath("varchar_cast", ExpressionPosition.UNKNOWN), IntVector.class);
      final IntVector.Accessor a0 = c0.getAccessor();

      int count = 0;
      for(int i = 0; i < c0.getAccessor().getValueCount(); i++) {
          final IntHolder holder0 = new IntHolder();
          a0.get(i, holder0);
          assertEquals(1256, holder0.value);
          ++count;
      }
      assertEquals(5, count);
    }

    exec.close();

    context.close();

    if (context.getExecutorState().getFailureCause() != null) {
      throw context.getExecutorState().getFailureCause();
    }
    assertTrue(!context.getExecutorState().isFailed());
  }

  @Test
  //cast to float4
  public void testCastFloat4() throws Throwable {
    final DrillbitContext bitContext = mockDrillbitContext();
    final UserClientConnection connection = Mockito.mock(UserClientConnection.class);

    final PhysicalPlanReader reader = PhysicalPlanReaderTestFactory.defaultPhysicalPlanReader(CONFIG);
    final PhysicalPlan plan = reader.readPhysicalPlan(Files.asCharSource(DrillFileUtils.getResourceAsFile("/functions/cast/testCastFloat4.json"), Charsets.UTF_8).read());
    final FunctionImplementationRegistry registry = new FunctionImplementationRegistry(CONFIG);
    final FragmentContextImpl context = new FragmentContextImpl(bitContext, PlanFragment.getDefaultInstance(), connection, registry);
    final SimpleRootExec exec = new SimpleRootExec(ImplCreator.getExec(context, (FragmentRoot) plan.getSortedOperators(false).iterator().next()));

    while(exec.next()) {
      final Float4Vector c0 = exec.getValueVectorById(new SchemaPath("varchar_cast2", ExpressionPosition.UNKNOWN), Float4Vector.class);
      final Float4Vector.Accessor a0 = c0.getAccessor();

      int count = 0;
      for(int i = 0; i < c0.getAccessor().getValueCount(); i++) {
          final Float4Holder holder0 = new Float4Holder();
          a0.get(i, holder0);
          assertEquals(12.56, holder0.value, 0.001);
          ++count;

      }
      assertEquals(5, count);
    }

    exec.close();

    context.close();

    if (context.getExecutorState().getFailureCause() != null) {
      throw context.getExecutorState().getFailureCause();
    }
    assertTrue(!context.getExecutorState().isFailed());
  }

  @Test
  //cast to float8
  public void testCastFloat8() throws Throwable {
    final DrillbitContext bitContext = mockDrillbitContext();
    final UserClientConnection connection = Mockito.mock(UserClientConnection.class);

    final PhysicalPlanReader reader = PhysicalPlanReaderTestFactory.defaultPhysicalPlanReader(CONFIG);
    final PhysicalPlan plan = reader.readPhysicalPlan(Files.asCharSource(DrillFileUtils.getResourceAsFile("/functions/cast/testCastFloat8.json"), Charsets.UTF_8).read());
    final FunctionImplementationRegistry registry = new FunctionImplementationRegistry(CONFIG);
    final FragmentContextImpl context = new FragmentContextImpl(bitContext, PlanFragment.getDefaultInstance(), connection, registry);
    final SimpleRootExec exec = new SimpleRootExec(ImplCreator.getExec(context, (FragmentRoot) plan.getSortedOperators(false).iterator().next()));

    while(exec.next()) {
      final Float8Vector c0 = exec.getValueVectorById(new SchemaPath("varchar_cast2", ExpressionPosition.UNKNOWN), Float8Vector.class);
      final Float8Vector.Accessor a0 = c0.getAccessor();

      int count = 0;
      for(int i = 0; i < c0.getAccessor().getValueCount(); i++){
          final Float8Holder holder0 = new Float8Holder();
          a0.get(i, holder0);
          assertEquals(12.56, holder0.value, 0.001);
          ++count;

      }
      assertEquals(5, count);
    }

    exec.close();

    context.close();

    if (context.getExecutorState().getFailureCause() != null) {
      throw context.getExecutorState().getFailureCause();
    }
    assertTrue(!context.getExecutorState().isFailed());
  }

  @Test
  //cast to varchar(length)
  public void testCastVarChar() throws Throwable {
    final DrillbitContext bitContext = mockDrillbitContext();
    final UserClientConnection connection = Mockito.mock(UserClientConnection.class);

    final PhysicalPlanReader reader = PhysicalPlanReaderTestFactory.defaultPhysicalPlanReader(CONFIG);
    final PhysicalPlan plan = reader.readPhysicalPlan(Files.asCharSource(DrillFileUtils.getResourceAsFile("/functions/cast/testCastVarChar.json"), Charsets.UTF_8).read());
    final FunctionImplementationRegistry registry = new FunctionImplementationRegistry(CONFIG);
    final FragmentContextImpl context = new FragmentContextImpl(bitContext, PlanFragment.getDefaultInstance(), connection, registry);
    final SimpleRootExec exec = new SimpleRootExec(ImplCreator.getExec(context, (FragmentRoot) plan.getSortedOperators(false).iterator().next()));

    while(exec.next()) {
      final VarCharVector c0 = exec.getValueVectorById(new SchemaPath("int_lit_cast", ExpressionPosition.UNKNOWN), VarCharVector.class);
      final VarCharVector.Accessor a0 = c0.getAccessor();

      int count = 0;
      for(int i = 0; i < c0.getAccessor().getValueCount(); i++) {
          final VarCharHolder holder0 = new VarCharHolder();
          a0.get(i, holder0);
          assertEquals("123", StringFunctionHelpers.toStringFromUTF8(holder0.start, holder0.end, holder0.buffer));
          ++count;
      }
      assertEquals(5, count);
    }

    exec.close();

    context.close();

    if (context.getExecutorState().getFailureCause() != null) {
      throw context.getExecutorState().getFailureCause();
    }
    assertTrue(!context.getExecutorState().isFailed());
  }

  @Test
  //cast to varbinary(length)
  public void testCastVarBinary() throws Throwable {
    final DrillbitContext bitContext = mockDrillbitContext();
    final UserClientConnection connection = Mockito.mock(UserClientConnection.class);

    final PhysicalPlanReader reader = PhysicalPlanReaderTestFactory.defaultPhysicalPlanReader(CONFIG);
    final PhysicalPlan plan = reader.readPhysicalPlan(Files.asCharSource(DrillFileUtils.getResourceAsFile("/functions/cast/testCastVarBinary.json"), Charsets.UTF_8).read());
    final FunctionImplementationRegistry registry = new FunctionImplementationRegistry(CONFIG);
    final FragmentContextImpl context = new FragmentContextImpl(bitContext, PlanFragment.getDefaultInstance(), connection, registry);
    final SimpleRootExec exec = new SimpleRootExec(ImplCreator.getExec(context, (FragmentRoot) plan.getSortedOperators(false).iterator().next()));

    while(exec.next()) {
      final VarBinaryVector c0 = exec.getValueVectorById(new SchemaPath("int_lit_cast", ExpressionPosition.UNKNOWN), VarBinaryVector.class);
      final VarBinaryVector.Accessor a0 = c0.getAccessor();

      int count = 0;
      for(int i = 0; i < c0.getAccessor().getValueCount(); i++) {
          final VarBinaryHolder holder0 = new VarBinaryHolder();
          a0.get(i, holder0);
          assertEquals("123", StringFunctionHelpers.toStringFromUTF8(holder0.start, holder0.end, holder0.buffer));
          ++count;

      }
      assertEquals(5, count);
    }
    exec.close();

    context.close();

    if (context.getExecutorState().getFailureCause() != null) {
      throw context.getExecutorState().getFailureCause();
    }
    assertTrue(!context.getExecutorState().isFailed());
  }

  @Test
  //nested: cast is nested in another cast, or another function.
  public void testCastNested() throws Throwable {
    final DrillbitContext bitContext = mockDrillbitContext();
    final UserClientConnection connection = Mockito.mock(UserClientConnection.class);

    final PhysicalPlanReader reader = PhysicalPlanReaderTestFactory.defaultPhysicalPlanReader(CONFIG);
    final PhysicalPlan plan = reader.readPhysicalPlan(Files.asCharSource(DrillFileUtils.getResourceAsFile("/functions/cast/testCastNested.json"), Charsets.UTF_8).read());
    final FunctionImplementationRegistry registry = new FunctionImplementationRegistry(CONFIG);
    final FragmentContextImpl context = new FragmentContextImpl(bitContext, PlanFragment.getDefaultInstance(), connection, registry);
    final SimpleRootExec exec = new SimpleRootExec(ImplCreator.getExec(context, (FragmentRoot) plan.getSortedOperators(false).iterator().next()));

    while(exec.next()) {
      final IntVector c0 = exec.getValueVectorById(new SchemaPath("add_cast", ExpressionPosition.UNKNOWN),IntVector.class);
      final IntVector.Accessor a0 = c0.getAccessor();

      int count = 0;
      for(int i = 0; i < c0.getAccessor().getValueCount(); i++) {
          final IntHolder holder0 = new IntHolder();
          a0.get(i, holder0);
          assertEquals(300, holder0.value);
          ++count;

      }
      assertEquals(5, count);
    }
    exec.close();

    context.close();

    if (context.getExecutorState().getFailureCause() != null) {
      throw context.getExecutorState().getFailureCause();
    }


    assertTrue(!context.getExecutorState().isFailed());
  }

  @Test(expected = NumberFormatException.class)
  public void testCastNumException() throws Throwable {
    final DrillbitContext bitContext = mockDrillbitContext();
    final UserClientConnection connection = Mockito.mock(UserClientConnection.class);

    final PhysicalPlanReader reader = PhysicalPlanReaderTestFactory.defaultPhysicalPlanReader(CONFIG);
    final PhysicalPlan plan = reader.readPhysicalPlan(Files.asCharSource(DrillFileUtils.getResourceAsFile("/functions/cast/testCastNumException.json"), Charsets.UTF_8).read());
    final FunctionImplementationRegistry registry = new FunctionImplementationRegistry(CONFIG);
    final FragmentContextImpl context = new FragmentContextImpl(bitContext, PlanFragment.getDefaultInstance(), connection, registry);
    final SimpleRootExec exec = new SimpleRootExec(ImplCreator.getExec(context, (FragmentRoot) plan.getSortedOperators(false).iterator().next()));

    while(exec.next()) {
    }

    exec.close();

    context.close();

    assertTrue(context.getExecutorState().isFailed());

    if(context.getExecutorState().getFailureCause() != null) {
      throw context.getExecutorState().getFailureCause();
    }
  }

  @Test
  public void testCastFromNullablCol() throws Throwable {
    final RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();
    try(final Drillbit bit = new Drillbit(CONFIG, serviceSet);
        final DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator())) {
      bit.run();

      client.connect();
      final List<QueryDataBatch> results = client.runQuery(org.apache.drill.exec.proto.UserBitShared.QueryType.PHYSICAL,
          Files.asCharSource(DrillFileUtils.getResourceAsFile("/functions/cast/testCastVarCharNull.json"), Charsets.UTF_8).read().replace("#{TEST_FILE}", "/jsoninput/input1.json"));

      final QueryDataBatch batch = results.get(0);

      final RecordBatchLoader batchLoader = new RecordBatchLoader(bit.getContext().getAllocator());
      batchLoader.load(batch.getHeader().getDef(), batch.getData());

      final Object [][] result = getRunResult(batchLoader);

      final Object [][] expected = new Object[2][2];

      expected[0][0] = new String("2001");
      expected[0][1] = new String("1.2");

      expected[1][0] = new String("-2002");
      expected[1][1] = new String("-1.2");

      assertEquals(result.length, expected.length);
      assertEquals(result[0].length, expected[0].length);

      for (int i = 0; i<result.length; i++ ) {
        for (int j = 0; j<result[0].length; j++) {
          assertEquals(String.format("Column %s at row %s have wrong result",  j, i), result[i][j].toString(), expected[i][j]);
        }
      }
      batchLoader.clear();
      for(final QueryDataBatch b : results){
        b.release();
      }
    }
  }

  private Object[][] getRunResult(VectorAccessible va) {
    int size = 0;
    for (final VectorWrapper v : va) {
      size++;
    }

    final Object[][] res = new Object [va.getRecordCount()][size];
    for (int j = 0; j < va.getRecordCount(); j++) {
      int i = 0;
      for (final VectorWrapper v : va) {
        final Object o =  v.getValueVector().getAccessor().getObject(j);
        if (o instanceof byte[]) {
          res[j][i++] =  new String((byte[]) o);
        } else {
          res[j][i++] = o;
        }
      }
    }
    return res;
 }
}
