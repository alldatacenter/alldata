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
import org.apache.drill.exec.expr.annotations.FunctionTemplate.FunctionScope;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.holders.BigIntHolder;

public class Alternator {

  @FunctionTemplate(name = "alternate", isRandom = true, scope = FunctionScope.SIMPLE)
  public static class Alternate2 implements DrillSimpleFunc{
    @Workspace int val;
    @Output BigIntHolder out;

    public void setup() {
      val = 0;
    }


    public void eval() {
      out.value = val;
      if(val == 0){
        val = 1;
      }else{
        val = 0;
      }
    }
  }

  @FunctionTemplate(name = "alternate3", isRandom = true, scope = FunctionScope.SIMPLE)
  public static class Alternate3 implements DrillSimpleFunc{
    @Workspace int val;
    @Output BigIntHolder out;

    public void setup() {
      val = 0;
    }


    public void eval() {
      out.value = val;
      if(val == 0){
        val = 1;
      }else if(val == 1){
        val = 2;
      }else{
        val = 0;
      }
    }
  }
}
