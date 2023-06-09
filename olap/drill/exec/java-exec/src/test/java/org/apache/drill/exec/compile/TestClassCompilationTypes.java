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
package org.apache.drill.exec.compile;

import org.apache.drill.exec.ExecTest;
import org.codehaus.janino.ExpressionEvaluator;
import org.junit.Ignore;
import org.junit.Test;

public class TestClassCompilationTypes extends ExecTest{
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestClassCompilationTypes.class);

  @Ignore @Test
  public void comparePerfs() throws Exception {
    for(int i =0; i < 500; i++){
      int r = 0;
      long n0 = System.nanoTime();
      r += janino();
      long n1 = System.nanoTime();
      //r += jdk();
      long n2 = System.nanoTime();
      long janinoT = (n1 - n0)/1000;
      long jdkT = (n2 - n1)/1000;
      logger.info("Janino: {} micros.  JDK: {} micros. Val {}", janinoT, jdkT, r);
    }

  }

  private int janino() throws Exception{
    // Compile the expression once; relatively slow.
    org.codehaus.janino.ExpressionEvaluator ee = new org.codehaus.janino.ExpressionEvaluator("c > d ? c : d", // expression
        int.class, // expressionType
        new String[] { "c", "d" }, // parameterNames
        new Class[] { int.class, int.class } // parameterTypes
    );

    // Evaluate it with varying parameter values; very fast.
    return (Integer) ee.evaluate(new Object[] { // parameterValues
        Integer.valueOf(10), Integer.valueOf(11), });
  }

  private int jdk() throws Exception{
    // Compile the expression once; relatively slow.
    ExpressionEvaluator ee = new ExpressionEvaluator("c > d ? c : d", // expression
        int.class, // expressionType
        new String[] { "c", "d" }, // parameterNames
        new Class[] { int.class, int.class } // parameterTypes
    );

    // Evaluate it with varying parameter values; very fast.
    return  (Integer) ee.evaluate(new Object[] { // parameterValues
        Integer.valueOf(10), Integer.valueOf(11), });
  }
}
