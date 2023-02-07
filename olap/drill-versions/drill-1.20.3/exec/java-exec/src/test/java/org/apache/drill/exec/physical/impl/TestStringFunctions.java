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

import org.apache.drill.categories.UnlikelyTest;
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
import org.apache.drill.exec.vector.VarCharVector;
import org.junit.Test;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.io.Resources;

import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

@Category({UnlikelyTest.class})
public class TestStringFunctions extends ExecTest {
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
      if  (v instanceof VarCharVector) {
        res[i++] = new String( ((VarCharVector) v).getAccessor().get(0), Charsets.UTF_8);
      } else {
        res[i++] =  v.getAccessor().getObject(0);
      }
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
      context =  new FragmentContextImpl(bitContext, PlanFragment.getDefaultInstance(), connection, registry);
    }
    final PhysicalPlan plan = reader.readPhysicalPlan(planString);
    final SimpleRootExec exec = new SimpleRootExec(ImplCreator.getExec(context, (FragmentRoot) plan.getSortedOperators(false).iterator().next()));

    exec.next(); // skip schema batch
    while(exec.next()) {
      final Object [] res = getRunResult(exec);
      assertEquals("return count does not match", expectedResults.length, res.length);

      for (int i = 0; i<res.length; i++) {
        assertEquals(String.format("column %s does not match", i), expectedResults[i],  res[i]);
      }
    }

    if (context.getExecutorState().getFailureCause() != null) {
      throw context.getExecutorState().getFailureCause();
    }
    assertTrue(!context.getExecutorState().isFailed());
  }

  @Test
  public void testCharLength() throws Throwable {
    Object [] expected = new Object[] {Long.valueOf(8), Long.valueOf(0), Long.valueOf(5), Long.valueOf(5),
                                       Long.valueOf(8), Long.valueOf(0), Long.valueOf(5), Long.valueOf(5),
                                       Long.valueOf(8), Long.valueOf(0), Long.valueOf(5), Long.valueOf(5),};
    runTest(expected, "functions/string/testCharLength.json");
  }

  @Test
  public void testLike() throws Throwable {
    final Object [] expected = new Object[] {Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.FALSE};
    runTest(expected, "functions/string/testLike.json");
  }

  @Test
  public void testSimilar() throws Throwable {
    final Object [] expected = new Object[] {Boolean.TRUE, Boolean.FALSE, Boolean.TRUE, Boolean.FALSE};
    runTest(expected, "functions/string/testSimilar.json");
  }

  @Test
  public void testLtrim() throws Throwable {
    final Object [] expected = new Object[] {"def", "abcdef", "dabc", "", "", ""};
    runTest(expected, "functions/string/testLtrim.json");
  }

  @Test
  public void testTrim() throws Throwable {
    final Object [] expected = new Object[] {"fghI", "", "", "!", " aaa "};
    runTest(expected, "functions/string/testTrim.json");
  }

  @Test
  public void testReplace() throws Throwable {
    final Object [] expected = new Object[] {"aABABcdf", "ABABbABbcdf", "aababcdf", "acdf", "ABCD", "abc"};
    runTest(expected, "functions/string/testReplace.json");
  }

  @Test
  public void testRtrim() throws Throwable {
    final Object [] expected = new Object[] {"abc", "abcdef", "ABd", "", "", ""};
    runTest(expected, "functions/string/testRtrim.json");
  }

  @Test
  public void testConcat() throws Throwable {
    final Object [] expected = new Object[] {"abcABC", "abc", "ABC", ""};
    runTest(expected, "functions/string/testConcat.json");
  }

  @Test
  public void testLower() throws Throwable {
    final Object [] expected = new Object[] {"abcefgh", "abc", ""};
    runTest(expected, "functions/string/testLower.json");
  }

  @Test
  public void testPosition() throws Throwable {
    final Object [] expected = new Object[] {Long.valueOf(2), Long.valueOf(0), Long.valueOf(0), Long.valueOf(0),
                                       Long.valueOf(2), Long.valueOf(0), Long.valueOf(0), Long.valueOf(0)};
    runTest(expected, "functions/string/testPosition.json");
  }

  @Test
  public void testRight() throws Throwable {
    final Object [] expected = new Object[] {"ef", "abcdef", "abcdef", "cdef", "f", "", ""};
    runTest(expected, "functions/string/testRight.json");
  }


  @Test
  public void testSubstr() throws Throwable {
    final Object [] expected = new Object[] {"abc", "bcd", "bcdef", "bcdef", "", "", "", "", "भारत", "वर्ष", "वर्ष", "cdef", "", "", "", "ड्रिल"};
    runTest(expected, "functions/string/testSubstr.json");
  }

  @Test
  public void testLeft() throws Throwable {
    final Object [] expected = new Object[] {"ab", "abcdef", "abcdef", "abcd", "a", "", ""};
    runTest(expected, "functions/string/testLeft.json");
  }

  @Test
  public void testLpad() throws Throwable {
    final Object [] expected = new Object[] {"", "", "abcdef", "ab", "ab", "abcdef", "AAAAabcdef", "ABABabcdef", "ABCAabcdef", "ABCDabcdef"};
    runTest(expected, "functions/string/testLpad.json");
  }

  @Test
  public void testRegexpReplace() throws Throwable {
    final Object [] expected = new Object[] {"ThM", "Th", "Thomas"};
    runTest(expected, "functions/string/testRegexpReplace.json");
  }

  @Test
  public void testRpad() throws Throwable {
    final Object [] expected = new Object[] {"", "", "abcdef", "ab", "ab", "abcdef", "abcdefAAAA", "abcdefABAB", "abcdefABCA", "abcdefABCD"};
    runTest(expected, "functions/string/testRpad.json");
  }

  @Test
  public void testUpper() throws Throwable {
    final Object [] expected = new Object[] {"ABCEFGH", "ABC", ""};
    runTest(expected, "functions/string/testUpper.json");
  }

  @Test
  public void testNewStringFuncs() throws Throwable {
    final Object [] expected = new Object[] {97, 65, -32, "A", "btrim", "Peace Peace Peace ", "हकुना मताता हकुना मताता ", "katcit", "\u00C3\u00A2pple", "नदम"};
    runTest(expected, "functions/string/testStringFuncs.json");
  }
}
