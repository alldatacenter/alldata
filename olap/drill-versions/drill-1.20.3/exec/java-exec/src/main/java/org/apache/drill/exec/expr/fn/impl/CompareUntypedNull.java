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
package org.apache.drill.exec.expr.fn.impl;

import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.fn.FunctionGenerationHelper;
import org.apache.drill.exec.expr.holders.IntHolder;
import org.apache.drill.exec.vector.UntypedNullHolder;

public class CompareUntypedNull {

  @FunctionTemplate(name = FunctionGenerationHelper.COMPARE_TO_NULLS_HIGH,
    scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.INTERNAL)
  public static class CompareUntypedNullHigh implements DrillSimpleFunc {

    @Param
    UntypedNullHolder left;

    @Param
    UntypedNullHolder right;

    @Output
    IntHolder out;

    public void setup() {
    }

    public void eval() {
      out.value = 0;
    }
  }

  @FunctionTemplate(name = FunctionGenerationHelper.COMPARE_TO_NULLS_LOW,
    scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.INTERNAL)
  public static class CompareUntypedNullLow implements DrillSimpleFunc {

    @Param
    UntypedNullHolder left;

    @Param
    UntypedNullHolder right;

    @Output
    IntHolder out;

    public void setup() {
    }

    public void eval() {
      out.value = 0;
    }
  }

}

