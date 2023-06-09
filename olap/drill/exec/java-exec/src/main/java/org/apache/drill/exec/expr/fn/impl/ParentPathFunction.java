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
import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.holders.VarCharHolder;

import javax.inject.Inject;

@FunctionTemplate(name = "parentPath",
                  scope = FunctionTemplate.FunctionScope.SIMPLE,
                  nulls = FunctionTemplate.NullHandling.NULL_IF_NULL,
                  isInternal = true)
public class ParentPathFunction implements DrillSimpleFunc {

  @Param VarCharHolder input;
  @Output VarCharHolder out;
  @Inject DrillBuf buf;

  @Override
  public void setup() {
  }

  @Override
  public void eval() {
    org.apache.hadoop.fs.Path path =
        new org.apache.hadoop.fs.Path(org.apache.drill.common.util.DrillStringUtils.toBinaryString(input.buffer, input.start, input.end));
    byte[] bytes = path.getParent().toUri().getPath().getBytes();

    buf = buf.reallocIfNeeded(bytes.length);
    buf.setBytes(0, bytes);
    out.buffer = buf;
    out.start = 0;
    out.end = bytes.length;
  }
}
