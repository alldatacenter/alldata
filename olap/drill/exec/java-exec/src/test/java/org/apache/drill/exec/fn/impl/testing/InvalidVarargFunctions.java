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

import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.holders.BigIntHolder;
import org.apache.drill.exec.expr.holders.VarCharHolder;

public class InvalidVarargFunctions {

  @FunctionTemplate(name = "non_last_vararg",
                    isVarArg = true,
                    scope = FunctionTemplate.FunctionScope.SIMPLE)
  public static class FunctionWithNonLastVararg implements DrillSimpleFunc {
    @Param BigIntHolder[] inputs;
    @Param VarCharHolder lastInput;
    @Output BigIntHolder out;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {
      throw new DrillRuntimeException("This function should not be used!");
    }
  }

  @FunctionTemplate(name = "several_varargs",
                    isVarArg = true,
                    scope = FunctionTemplate.FunctionScope.SIMPLE)
  public static class FunctionWithSeveralVarargs implements DrillSimpleFunc {
    @Param VarCharHolder[] varChars;
    @Param BigIntHolder[] bigInts;
    @Output BigIntHolder out;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {
      throw new DrillRuntimeException("This function should not be used!");
    }
  }
}
