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

import io.netty.buffer.DrillBuf;

import javax.inject.Inject;

import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.holders.BigIntHolder;
import org.apache.drill.exec.expr.holders.BitHolder;
import org.apache.drill.exec.expr.holders.VarCharHolder;


public class SimpleCastFunctions {

  @FunctionTemplate(names = {"castBIT", "castBOOLEAN"}, scope = FunctionTemplate.FunctionScope.SIMPLE, nulls=NullHandling.NULL_IF_NULL)
  public static class CastVarCharBoolean implements DrillSimpleFunc {

    @Param VarCharHolder in;
    @Output BitHolder out;

    public void setup() {

    }

    public void eval() {
      byte[] buf = new byte[in.end - in.start];
      in.buffer.getBytes(in.start, buf, 0, in.end - in.start);
      String input = new String(buf, com.google.common.base.Charsets.UTF_8);
      out.value = org.apache.drill.common.types.BooleanType.get(input).getNumericValue();
    }
  }

  @FunctionTemplate(name = "castVARCHAR",
      scope = FunctionTemplate.FunctionScope.SIMPLE,
      returnType = FunctionTemplate.ReturnType.STRING_CAST,
      nulls = NullHandling.NULL_IF_NULL,
      outputWidthCalculatorType = FunctionTemplate.OutputWidthCalculatorType.CUSTOM_CLONE_DEFAULT)
  public static class CastBooleanVarChar implements DrillSimpleFunc {

    @Param BitHolder in;
    @Param BigIntHolder len;
    @Output VarCharHolder out;
    @Inject DrillBuf buffer;

    public void setup() {}

    public void eval() {
      byte[] outB = org.apache.drill.common.types.BooleanType.get(String.valueOf(in.value)).name().toLowerCase().getBytes();
      buffer.setBytes(0, outB);
      out.buffer = buffer;
      out.start = 0;
      out.end = Math.min((int)len.value, outB.length); // truncate if target type has length smaller than that of input's string
    }
  }
}
