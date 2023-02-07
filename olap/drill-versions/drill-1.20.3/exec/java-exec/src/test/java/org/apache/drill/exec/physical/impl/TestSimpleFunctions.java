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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.drill.common.expression.ExpressionPosition;
import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.expression.TypedNullConstant;
import org.apache.drill.common.expression.ValueExpressions;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.ExecTest;
import org.apache.drill.exec.expr.fn.DrillFuncHolder;
import org.apache.drill.exec.expr.fn.FunctionImplementationRegistry;
import org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers;
import org.apache.drill.exec.expr.holders.NullableVarBinaryHolder;
import org.apache.drill.exec.expr.holders.NullableVarCharHolder;
import org.apache.drill.exec.ops.FragmentContextImpl;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.physical.base.FragmentRoot;
import org.apache.drill.exec.planner.PhysicalPlanReader;
import org.apache.drill.exec.planner.PhysicalPlanReaderTestFactory;
import org.apache.drill.exec.proto.BitControl.PlanFragment;
import org.apache.drill.exec.resolver.FunctionResolver;
import org.apache.drill.exec.resolver.FunctionResolverFactory;
import org.apache.drill.exec.rpc.UserClientConnection;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.vector.NullableVarBinaryVector;
import org.apache.drill.exec.vector.NullableVarCharVector;
import org.junit.Test;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.io.Files;
import com.sun.codemodel.JClassAlreadyExistsException;

import org.mockito.Mockito;

public class TestSimpleFunctions extends ExecTest {

  @Test
  public void testHashFunctionResolution() throws JClassAlreadyExistsException, IOException {
    final FunctionImplementationRegistry registry = new FunctionImplementationRegistry(c);
    // test required vs nullable Int input
    resolveHash(new TypedNullConstant(Types.optional(TypeProtos.MinorType.INT)),
        TypeProtos.DataMode.OPTIONAL,
        registry);

    resolveHash(new ValueExpressions.IntExpression(1, ExpressionPosition.UNKNOWN),
        TypeProtos.DataMode.REQUIRED,
        registry);

    // test required vs nullable float input
    resolveHash(new TypedNullConstant(Types.optional(TypeProtos.MinorType.FLOAT4)),
        TypeProtos.DataMode.OPTIONAL,
        registry);

    resolveHash(new ValueExpressions.FloatExpression(5.0f, ExpressionPosition.UNKNOWN),
        TypeProtos.DataMode.REQUIRED,
        registry);

    // test required vs nullable long input
    resolveHash(new TypedNullConstant(Types.optional(TypeProtos.MinorType.BIGINT)),
        TypeProtos.DataMode.OPTIONAL,
        registry);

    resolveHash(new ValueExpressions.LongExpression(100L, ExpressionPosition.UNKNOWN),
        TypeProtos.DataMode.REQUIRED,
        registry);

    // test required vs nullable double input
    resolveHash(new TypedNullConstant(Types.optional(TypeProtos.MinorType.FLOAT8)),
        TypeProtos.DataMode.OPTIONAL,
        registry);

    resolveHash(new ValueExpressions.DoubleExpression(100.0, ExpressionPosition.UNKNOWN),
        TypeProtos.DataMode.REQUIRED,
        registry);
  }

  public void resolveHash(LogicalExpression arg, TypeProtos.DataMode expectedBestInputMode,
                          FunctionImplementationRegistry registry) {
    final List<LogicalExpression> args = new ArrayList<>();
    args.add(arg);
    FunctionCall call = new FunctionCall(
        "hash",
        args,
        ExpressionPosition.UNKNOWN
    );
    final FunctionResolver resolver = FunctionResolverFactory.getResolver(call);
    final DrillFuncHolder matchedFuncHolder = registry.findDrillFunction(resolver, call);
    assertEquals(expectedBestInputMode, matchedFuncHolder.getParamMajorType(0).getMode());
  }

  @Test
  public void testSubstring() throws Throwable {
    final DrillbitContext bitContext = mockDrillbitContext();
    final UserClientConnection connection = Mockito.mock(UserClientConnection.class);

    final PhysicalPlanReader reader = PhysicalPlanReaderTestFactory.defaultPhysicalPlanReader(c);
    final PhysicalPlan plan = reader.readPhysicalPlan(Files.asCharSource(DrillFileUtils.getResourceAsFile("/functions/testSubstring.json"), Charsets.UTF_8).read());
    final FunctionImplementationRegistry registry = new FunctionImplementationRegistry(c);
    final FragmentContextImpl context = new FragmentContextImpl(bitContext, PlanFragment.getDefaultInstance(), connection, registry);
    final SimpleRootExec exec = new SimpleRootExec(ImplCreator.getExec(context, (FragmentRoot) plan.getSortedOperators(false).iterator().next()));

    while(exec.next()) {
      final NullableVarCharVector c1 = exec.getValueVectorById(new SchemaPath("col3", ExpressionPosition.UNKNOWN), NullableVarCharVector.class);
      final NullableVarCharVector.Accessor a1 = c1.getAccessor();

      int count = 0;
      for(int i = 0; i < c1.getAccessor().getValueCount(); i++) {
        if (!a1.isNull(i)) {
          final NullableVarCharHolder holder = new NullableVarCharHolder();
          a1.get(i, holder);
          assertEquals("aaaa", StringFunctionHelpers.toStringFromUTF8(holder.start,  holder.end,  holder.buffer));
          ++count;
        }
      }
      assertEquals(50, count);
    }

    if(context.getExecutorState().getFailureCause() != null) {
      throw context.getExecutorState().getFailureCause();
    }
    assertTrue(!context.getExecutorState().isFailed());
  }

  @Test
  public void testSubstringNegative() throws Throwable {
    final DrillbitContext bitContext = mockDrillbitContext();
    final UserClientConnection connection = Mockito.mock(UserClientConnection.class);

    final PhysicalPlanReader reader = PhysicalPlanReaderTestFactory.defaultPhysicalPlanReader(c);
    final PhysicalPlan plan = reader.readPhysicalPlan(Files.asCharSource(DrillFileUtils.getResourceAsFile("/functions/testSubstringNegative.json"), Charsets.UTF_8).read());
    final FunctionImplementationRegistry registry = new FunctionImplementationRegistry(c);
    final FragmentContextImpl context = new FragmentContextImpl(bitContext, PlanFragment.getDefaultInstance(), connection, registry);
    final SimpleRootExec exec = new SimpleRootExec(ImplCreator.getExec(context, (FragmentRoot) plan.getSortedOperators(false).iterator().next()));

    while(exec.next()) {
      final NullableVarCharVector c1 = exec.getValueVectorById(new SchemaPath("col3", ExpressionPosition.UNKNOWN), NullableVarCharVector.class);
      final NullableVarCharVector.Accessor a1 = c1.getAccessor();

      int count = 0;
      for(int i = 0; i < c1.getAccessor().getValueCount(); i++) {
        if (!a1.isNull(i)) {
          final NullableVarCharHolder holder = new NullableVarCharHolder();
          a1.get(i, holder);
          //when offset is negative, substring return empty string.
          assertEquals("", StringFunctionHelpers.toStringFromUTF8(holder.start,  holder.end,  holder.buffer));
          ++count;
        }
      }
      assertEquals(50, count);
    }

    if(context.getExecutorState().getFailureCause() != null) {
      throw context.getExecutorState().getFailureCause();
    }
    assertTrue(!context.getExecutorState().isFailed());
  }

  @Test
  public void testByteSubstring() throws Throwable {
    final DrillbitContext bitContext = mockDrillbitContext();
    final UserClientConnection connection = Mockito.mock(UserClientConnection.class);

    final PhysicalPlanReader reader = PhysicalPlanReaderTestFactory.defaultPhysicalPlanReader(c);
    final PhysicalPlan plan = reader.readPhysicalPlan(Files.asCharSource(DrillFileUtils.getResourceAsFile("/functions/testByteSubstring.json"), Charsets.UTF_8).read());
    final FunctionImplementationRegistry registry = new FunctionImplementationRegistry(c);
    final FragmentContextImpl context = new FragmentContextImpl(bitContext, PlanFragment.getDefaultInstance(), connection, registry);
    final SimpleRootExec exec = new SimpleRootExec(ImplCreator.getExec(context, (FragmentRoot) plan.getSortedOperators(false).iterator().next()));

    while(exec.next()) {
      final NullableVarBinaryVector c1 = exec.getValueVectorById(new SchemaPath("col3", ExpressionPosition.UNKNOWN), NullableVarBinaryVector.class);
      final NullableVarBinaryVector.Accessor a1 = c1.getAccessor();

      int count = 0;
      for(int i = 0; i < c1.getAccessor().getValueCount(); i++) {
        if (!a1.isNull(i)) {
          final NullableVarBinaryHolder holder = new NullableVarBinaryHolder();
          a1.get(i, holder);
          assertEquals("aa", StringFunctionHelpers.toStringFromUTF8(holder.start,  holder.end,  holder.buffer));
          ++count;
        }
      }
      assertEquals(50, count);
    }

    if(context.getExecutorState().getFailureCause() != null) {
      throw context.getExecutorState().getFailureCause();
    }
    assertTrue(!context.getExecutorState().isFailed());
  }
}
