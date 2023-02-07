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

import static org.junit.Assert.assertTrue;

import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.categories.VectorTest;
import org.apache.drill.common.DrillAutoCloseables;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.ExecTest;
import org.apache.drill.exec.expr.fn.impl.ByteFunctionHelpers;
import org.apache.drill.exec.expr.holders.VarCharHolder;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.memory.RootAllocatorFactory;
import org.apache.drill.exec.vector.ValueHolderHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({UnlikelyTest.class, VectorTest.class})
public class TestByteComparisonFunctions extends ExecTest {

  private static BufferAllocator allocator;
  private static VarCharHolder hello;
  private static VarCharHolder goodbye;
  private static VarCharHolder helloLong;
  private static VarCharHolder goodbyeLong;

  @BeforeClass
  public static void setup() {
    DrillConfig c= DrillConfig.create();
    allocator = RootAllocatorFactory.newRoot(c);
    hello = ValueHolderHelper.getVarCharHolder(allocator, "hello");
    goodbye = ValueHolderHelper.getVarCharHolder(allocator, "goodbye");
    helloLong = ValueHolderHelper.getVarCharHolder(allocator, "hellomyfriend");
    goodbyeLong = ValueHolderHelper.getVarCharHolder(allocator, "goodbyemyenemy");
  }

  @AfterClass
  public static void teardown() {
    hello.buffer.release();
    helloLong.buffer.release();
    goodbye.buffer.release();
    goodbyeLong.buffer.release();
    DrillAutoCloseables.closeNoChecked(allocator);
  }

  @Test
  public void testAfter() {
    final VarCharHolder left = hello;
    final VarCharHolder right = goodbye;
    assertTrue(ByteFunctionHelpers.compare(left.buffer, left.start, left.end, right.buffer, right.start, right.end) == 1);
  }

  @Test
  public void testBefore() {
    final VarCharHolder left = goodbye;
    final VarCharHolder right = hello;
    assertTrue(ByteFunctionHelpers.compare(left.buffer, left.start, left.end, right.buffer, right.start, right.end) == -1);
  }

  @Test
  public void testEqualCompare() {
    final VarCharHolder left = hello;
    final VarCharHolder right = hello;
    assertTrue(ByteFunctionHelpers.compare(left.buffer, left.start, left.end, right.buffer, right.start, right.end) == 0);
  }

  @Test
  public void testEqual() {
    final VarCharHolder left = hello;
    final VarCharHolder right = hello;
    assertTrue(ByteFunctionHelpers.equal(left.buffer, left.start, left.end, right.buffer, right.start, right.end) == 1);
  }

  @Test
  public void testNotEqual() {
    final VarCharHolder left = hello;
    final VarCharHolder right = goodbye;
    assertTrue(ByteFunctionHelpers.equal(left.buffer, left.start, left.end, right.buffer, right.start, right.end) == 0);
  }

  @Test
  public void testAfterLong() {
    final VarCharHolder left = helloLong;
    final VarCharHolder right = goodbyeLong;
    assertTrue(ByteFunctionHelpers.compare(left.buffer, left.start, left.end, right.buffer, right.start, right.end) == 1);
  }

  @Test
  public void testBeforeLong() {
    final VarCharHolder left = goodbyeLong;
    final VarCharHolder right = helloLong;
    assertTrue(ByteFunctionHelpers.compare(left.buffer, left.start, left.end, right.buffer, right.start, right.end) == -1);
  }

  @Test
  public void testEqualCompareLong() {
    final VarCharHolder left = helloLong;
    final VarCharHolder right = helloLong;
    assertTrue(ByteFunctionHelpers.compare(left.buffer, left.start, left.end, right.buffer, right.start, right.end) == 0);
  }

  @Test
  public void testEqualLong() {
    final VarCharHolder left = helloLong;
    final VarCharHolder right = helloLong;
    assertTrue(ByteFunctionHelpers.equal(left.buffer, left.start, left.end, right.buffer, right.start, right.end) == 1);
  }

  @Test
  public void testNotEqualLong() {
    final VarCharHolder left = helloLong;
    final VarCharHolder right = goodbyeLong;
    assertTrue(ByteFunctionHelpers.equal(left.buffer, left.start, left.end, right.buffer, right.start, right.end) == 0);
  }
}
