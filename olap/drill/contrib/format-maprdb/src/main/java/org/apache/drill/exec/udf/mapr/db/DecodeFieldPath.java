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
package org.apache.drill.exec.udf.mapr.db;

import javax.inject.Inject;

import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.FunctionScope;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.holders.VarCharHolder;

import io.netty.buffer.DrillBuf;

@FunctionTemplate(name = "maprdb_decode_fieldpath", scope = FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL)
public class DecodeFieldPath implements DrillSimpleFunc {
  @Param  VarCharHolder input;
  @Output VarCharHolder   out;

  @Inject DrillBuf buffer;

  @Override
  public void setup() {
  }

  @Override
  public void eval() {
    String[] encodedPaths = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.
        toStringFromUTF8(input.start, input.end, input.buffer).split(",");
    String[] decodedPaths = org.apache.drill.exec.util.EncodedSchemaPathSet.decode(encodedPaths);
    java.util.Arrays.sort(decodedPaths);

    StringBuilder sb = new StringBuilder();
    for (String decodedPath : decodedPaths) {
      sb.append(", ").append(org.ojai.FieldPath.parseFrom(decodedPath).asPathString());
    }
    String outputString = "[" + sb.substring(2) + "]";
    final byte[] strBytes = outputString.getBytes(com.google.common.base.Charsets.UTF_8);
    buffer.setBytes(0, strBytes);
    buffer.setIndex(0, strBytes.length);

    out.start = 0;
    out.end = strBytes.length;
    out.buffer = buffer;
  }

}

