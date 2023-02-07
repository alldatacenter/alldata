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

import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.FunctionScope;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.holders.Float8Holder;
import org.apache.drill.exec.expr.holders.VarBinaryHolder;

@FunctionTemplate(name = "convert_fromDOUBLE_BE", scope = FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL)
public class DoubleBEConvertFrom implements DrillSimpleFunc {

  @Param VarBinaryHolder in;
  @Output Float8Holder out;

  @Override
  public void setup() { }

  @Override
  public void eval() {
    org.apache.drill.exec.util.ByteBufUtil.checkBufferLength(in.buffer, in.start, in.end, 8);

    in.buffer.readerIndex(in.start);
    out.value = Double.longBitsToDouble(Long.reverseBytes(in.buffer.readLong()));
  }
}
