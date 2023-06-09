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

import java.nio.charset.Charset;

import javax.inject.Inject;

import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.holders.BigIntHolder;
import org.apache.drill.exec.expr.holders.Var16CharHolder;
import org.apache.drill.exec.expr.holders.VarCharHolder;

@SuppressWarnings("unused")
@FunctionTemplate(names = {"castVAR16CHAR", "to_var16char", "to_string"}, scope = FunctionTemplate.FunctionScope.SIMPLE, nulls= NullHandling.NULL_IF_NULL,
                  outputWidthCalculatorType = FunctionTemplate.OutputWidthCalculatorType.CUSTOM_CLONE_DEFAULT)
public class CastVarCharVar16Char implements DrillSimpleFunc {
  @Param VarCharHolder in;
  @Param BigIntHolder length;
  @Output Var16CharHolder out;
  @Workspace Charset charset;
  @Inject DrillBuf buffer;

  @Override
  public void setup() {
    charset = java.nio.charset.Charset.forName("UTF-16");
  }

  @Override
  public void eval() {
    //if input length <= target_type length, do nothing
    //else truncate based on target_type length
    byte[] buf = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(in.start, in.end, in.buffer).getBytes(charset);
    buffer.setBytes(0, buf);

    out.start = 0;
    out.end = buf.length;
    out.buffer = buffer;
  }
}

