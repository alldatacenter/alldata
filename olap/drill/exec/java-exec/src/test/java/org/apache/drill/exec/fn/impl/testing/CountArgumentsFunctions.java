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

import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.holders.BigIntHolder;
import org.apache.drill.exec.expr.holders.RepeatedListHolder;
import org.apache.drill.exec.expr.holders.RepeatedMapHolder;
import org.apache.drill.exec.expr.holders.RepeatedVarCharHolder;

public class CountArgumentsFunctions {

  @FunctionTemplate(name = "test_count",
                    isVarArg = true,
                    scope = FunctionTemplate.FunctionScope.SIMPLE)
  public static class CountRepeatedVarchar implements DrillSimpleFunc {
    @Param RepeatedVarCharHolder[] inputs;
    @Output BigIntHolder out;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {
      for (RepeatedVarCharHolder input : inputs) {
        // dummy loop, just to check that type is correct
        out.value++;
      }
    }
  }

  @FunctionTemplate(name = "test_count",
                    isVarArg = true,
                    scope = FunctionTemplate.FunctionScope.SIMPLE)
  public static class CountRepeatedMap implements DrillSimpleFunc {
    @Param RepeatedMapHolder[] inputs;
    @Output BigIntHolder out;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {
      for (RepeatedMapHolder input : inputs) {
        // dummy loop, just to check that type is correct
        out.value++;
      }
    }
  }

  @FunctionTemplate(name = "test_count",
                    isVarArg = true,
                    scope = FunctionTemplate.FunctionScope.SIMPLE)
  public static class CountListHolder implements DrillSimpleFunc {
    @Param RepeatedListHolder[] inputs;
    @Output BigIntHolder out;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {
      for (RepeatedListHolder input : inputs) {
        // dummy loop, just to check that type is correct
        out.value++;
      }
    }
  }
}
