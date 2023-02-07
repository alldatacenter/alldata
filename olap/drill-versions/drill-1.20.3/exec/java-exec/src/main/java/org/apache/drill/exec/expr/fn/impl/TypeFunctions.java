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

import javax.inject.Inject;

import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import org.apache.drill.exec.expr.holders.VarCharHolder;
import org.apache.drill.exec.vector.complex.reader.FieldReader;

import io.netty.buffer.DrillBuf;

/**
 * Type functions for all types. See UnionFunctions for type functions
 * specifically for the UNION type.
 */

public class TypeFunctions {

  @FunctionTemplate(name = "sqlTypeOf",
          scope = FunctionTemplate.FunctionScope.SIMPLE,
          nulls = NullHandling.INTERNAL)
  public static class GetSqlType implements DrillSimpleFunc {

    @Param
    FieldReader input;
    @Output
    VarCharHolder out;
    @Inject
    DrillBuf buf;

    @Override
    public void setup() {}

    @Override
    public void eval() {

      String typeName = org.apache.drill.common.types.Types.getExtendedSqlTypeName(input.getType());
      byte[] type = typeName.getBytes();
      buf = buf.reallocIfNeeded(type.length);
      buf.setBytes(0, type);
      out.buffer = buf;
      out.start = 0;
      out.end = type.length;
    }
  }

  @FunctionTemplate(name = "drillTypeOf",
          scope = FunctionTemplate.FunctionScope.SIMPLE,
          nulls = NullHandling.INTERNAL)
  public static class GetDrillType implements DrillSimpleFunc {

    @Param
    FieldReader input;
    @Output
    VarCharHolder out;
    @Inject
    DrillBuf buf;

    @Override
    public void setup() {}

    @Override
    public void eval() {
      String typeName = input.getVectorType().name();
      byte[] type = typeName.getBytes();
      buf = buf.reallocIfNeeded(type.length);
      buf.setBytes(0, type);
      out.buffer = buf;
      out.start = 0;
      out.end = type.length;
    }
  }

  @FunctionTemplate(name = "modeOf",
          scope = FunctionTemplate.FunctionScope.SIMPLE,
          nulls = NullHandling.INTERNAL)
  public static class GetMode implements DrillSimpleFunc {

    @Param
    FieldReader input;
    @Output
    VarCharHolder out;
    @Inject
    DrillBuf buf;

    @Override
    public void setup() {}

    @Override
    public void eval() {
      String typeName = org.apache.drill.common.types.Types.getSqlModeName(
          input.getType());
      byte[] type = typeName.getBytes();
      buf = buf.reallocIfNeeded(type.length);
      buf.setBytes(0, type);
      out.buffer = buf;
      out.start = 0;
      out.end = type.length;
    }
  }
}
