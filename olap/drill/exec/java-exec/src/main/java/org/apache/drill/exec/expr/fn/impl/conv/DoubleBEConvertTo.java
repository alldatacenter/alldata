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
package org.apache.drill.exec.expr.fn.impl.conv;

import io.netty.buffer.DrillBuf;
import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.FunctionScope;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.holders.Float8Holder;
import org.apache.drill.exec.expr.holders.VarBinaryHolder;
import org.apache.drill.exec.physical.impl.project.OutputSizeEstimateConstants;

import javax.inject.Inject;

@FunctionTemplate(name = "convert_toDOUBLE_BE", scope = FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL,
                  outputSizeEstimate = OutputSizeEstimateConstants.CONVERT_TO_DOUBLE_LENGTH)
public class DoubleBEConvertTo implements DrillSimpleFunc {

  @Param Float8Holder in;
  @Output VarBinaryHolder out;
  @Inject DrillBuf buffer;


  @Override
  public void setup() {
    buffer = buffer.reallocIfNeeded(8);
  }

  @Override
  public void eval() {
    buffer.clear();
    buffer.writeLong(Long.reverseBytes(Double.doubleToLongBits(in.value)));
    out.buffer = buffer;
    out.start = 0;
    out.end = 8;
  }
}
