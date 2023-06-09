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
package org.apache.drill.exec.fn.impl.testing;

import java.util.Random;

import org.apache.drill.exec.ExecTest;
import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.FunctionScope;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.holders.BigIntHolder;
import org.apache.drill.exec.expr.holders.Float8Holder;

public class GeneratorFunctions extends ExecTest {

  public static final Random random = new Random(1234L);

  @FunctionTemplate(name = "increasingBigInt", isRandom = true,
    scope = FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class IncreasingBigInt implements DrillSimpleFunc {

    @Param BigIntHolder start;
    @Workspace long current;
    @Output BigIntHolder out;

    public void setup() {
      current = 0;
    }

    public void eval() {
      out.value = start.value + current++;
    }
  }

  @FunctionTemplate(name = "randomBigInt", isRandom = true,
    scope = FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class RandomBigIntGauss implements DrillSimpleFunc {

    @Param BigIntHolder range;
    @Output BigIntHolder out;

    public void setup() {
    }

    public void eval() {
      out.value = (long)(org.apache.drill.exec.fn.impl.testing.GeneratorFunctions.random.nextGaussian() * range.value);
    }
  }

  @FunctionTemplate(name = "randomBigInt", isRandom = true,
    scope = FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class RandomBigInt implements DrillSimpleFunc {

    @Param BigIntHolder min;
    @Param BigIntHolder max;
    @Output BigIntHolder out;

    public void setup() {
    }

    public void eval() {
      out.value = (long)(org.apache.drill.exec.fn.impl.testing.GeneratorFunctions.random.nextFloat() * (max.value - min.value) + min.value);
    }
  }

  @FunctionTemplate(name = "randomFloat8", isRandom = true,
    scope = FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class RandomFloat8Gauss implements DrillSimpleFunc {

    @Param BigIntHolder range;
    @Output
    Float8Holder out;

    public void setup() {
    }

    public void eval() {
      out.value = org.apache.drill.exec.fn.impl.testing.GeneratorFunctions.random.nextGaussian() * range.value;
    }
  }

  @FunctionTemplate(name = "randomFloat8", isRandom = true,
    scope = FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class RandomFloat8 implements DrillSimpleFunc {

    @Param BigIntHolder min;
    @Param BigIntHolder max;
    @Output Float8Holder out;

    public void setup() {
    }

    public void eval() {
      out.value = org.apache.drill.exec.fn.impl.testing.GeneratorFunctions.random.nextFloat() * (max.value - min.value) + min.value;
    }
  }
}
