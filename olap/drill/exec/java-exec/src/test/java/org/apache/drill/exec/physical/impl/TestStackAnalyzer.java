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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.apache.drill.test.BaseTest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * Test the function which finds the leaf-most operator within
 * an exception call stack. Does the tests using dummy classes
 * (which is why the stack analyzer function is parameterized.)
 */
public class TestStackAnalyzer extends BaseTest{

  private static class OperA {
    public void throwNow() {
      throw new RuntimeException();
    }

    public void throwIndirect() {
      throwNow();
    }

    public void throwViaB(OperB b) {
      b.throwIndirect();
    }

    public void throwAfterB(OperB b) {
      new RandomC().throwAfterB(b);
    }
  }

  private static class OperB {
    public void throwNow() {
      throw new RuntimeException();
    }

    public void throwIndirect() {
      throwNow();
    }

    public void throwAfterB() {
      new RandomC().throwNow();
    }
  }

  private static class RandomC {
    public void throwNow() {
      throw new RuntimeException();
    }

    public void throwAfterB(OperB b) {
      b.throwAfterB();
    }
  }

  @Test
  public void testEmptyStack() {
    try {
      throw new RuntimeException();
    } catch (RuntimeException e) {
      assertNull(BaseRootExec.findLeaf(Collections.emptyList(), e));
    }
  }

  @Test
  public void testOneLevel() {
    OperA a = new OperA();
    try {
      a.throwNow();
    } catch (RuntimeException e) {
      List<Object> ops = Collections.singletonList(a);
      assertSame(a, BaseRootExec.findLeaf(ops, e));
    }
  }

  @Test
  public void testOneLevelTwoDeep() {
    OperA a = new OperA();
    try {
      a.throwIndirect();
    } catch (RuntimeException e) {
      List<Object> ops = Collections.singletonList(a);
      assertSame(a, BaseRootExec.findLeaf(ops, e));
    }
  }

  @Test
  public void testTwoLevels() {
    OperA a = new OperA();
    OperB b = new OperB();
    try {
      a.throwViaB(b);
    } catch (RuntimeException e) {
      List<Object> ops = Arrays.asList(a, b);
      assertSame(b, BaseRootExec.findLeaf(ops, e));
    }
  }

  @Test
  public void testTwoLevelsWithExtra() {
    OperA a = new OperA();
    OperB b = new OperB();
    try {
      a.throwAfterB(b);
    } catch (RuntimeException e) {
      List<Object> ops = Arrays.asList(a, b);
      assertSame(b, BaseRootExec.findLeaf(ops, e));
    }
  }
}
