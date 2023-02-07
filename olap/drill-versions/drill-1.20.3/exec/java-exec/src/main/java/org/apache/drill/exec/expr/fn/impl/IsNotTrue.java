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
import org.apache.drill.exec.expr.holders.BitHolder;
import org.apache.drill.exec.expr.holders.NullableBitHolder;

public class IsNotTrue {

  @FunctionTemplate(names = {"isnottrue", "is not true"}, scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.INTERNAL)
  public static class Optional implements DrillSimpleFunc {

    @Param NullableBitHolder in;
    @Output BitHolder out;

    public void setup() { }

    public void eval() {
      if (in.isSet == 0) {
        out.value = 1;
      } else {
        out.value = (in.value == 0 ? 1 : 0);
      }
    }
  }

  @FunctionTemplate(names = {"isnottrue", "is not true"}, scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.INTERNAL)
  public static class Required implements DrillSimpleFunc {

    @Param BitHolder in;
    @Output BitHolder out;

    public void setup() { }

    public void eval() {
      out.value = in.value == 0 ? 1 : 0;
    }
  }

}
