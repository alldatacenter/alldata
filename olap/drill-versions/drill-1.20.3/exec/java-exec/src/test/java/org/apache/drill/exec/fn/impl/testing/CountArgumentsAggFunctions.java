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

import org.apache.drill.exec.expr.DrillAggFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.holders.BigIntHolder;
import org.apache.drill.exec.expr.holders.MapHolder;
import org.apache.drill.exec.expr.holders.NullableBigIntHolder;

public class CountArgumentsAggFunctions {

  @FunctionTemplate(name = "test_count_agg",
                    isVarArg = true,
                    scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class CountBigInt implements DrillAggFunc {
    @Param BigIntHolder[] inputs;
    @Workspace BigIntHolder value;
    @Output BigIntHolder out;

    public void setup() {
      value = new BigIntHolder();
      value.value = 0;
    }

    @Override
    public void add() {
      for (BigIntHolder input : inputs) {
        // dummy loop, just to check that type is correct
        value.value++;
      }
    }

    @Override
    public void output() {
      out.value = value.value;
    }

    @Override
    public void reset() {
      value.value = 0;
    }
  }

  @FunctionTemplate(name = "test_count_agg",
                    isVarArg = true,
                    scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class CountNullableBigInt implements DrillAggFunc {
    @Param NullableBigIntHolder[] inputs;
    @Workspace BigIntHolder value;
    @Output BigIntHolder out;

    public void setup() {
      value = new BigIntHolder();
      value.value = 0;
    }

    @Override
    public void add() {
      for (NullableBigIntHolder input : inputs) {
        // dummy loop, just to check that type is correct
        value.value++;
      }
    }

    @Override
    public void output() {
      out.value = value.value;
    }

    @Override
    public void reset() {
      value.value = 0;
    }
  }

  @FunctionTemplate(name = "test_count_agg",
                    isVarArg = true,
                    scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
  public static class CountMapHolder implements DrillAggFunc {
    @Param MapHolder[] inputs;
    @Workspace BigIntHolder value;
    @Output BigIntHolder out;

    public void setup() {
      value = new BigIntHolder();
      value.value = 0;
    }

    @Override
    public void add() {
      for (MapHolder input : inputs) {
        // dummy loop, just to check that type is correct
        value.value++;
      }
    }

    @Override
    public void output() {
      out.value = value.value;
    }

    @Override
    public void reset() {
      value.value = 0;
    }
  }
}
