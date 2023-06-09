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
import org.apache.drill.exec.vector.complex.reader.FieldReader;
import org.apache.drill.exec.vector.complex.writer.BaseWriter;

/**
 * Implementation of Calcite's ROW(col1, col2, ..., colN) constructor function.
 * Function expects field name before every value.
 *
 * @see org.apache.drill.exec.planner.logical.DrillOptiq
 */
@FunctionTemplate(name = "row", scope = FunctionTemplate.FunctionScope.SIMPLE,
    isVarArg = true, isInternal = true)
public class RowConstructorFunction implements DrillSimpleFunc {

  @Param
  FieldReader[] in;

  @Output
  BaseWriter.ComplexWriter out;

  @Override
  public void setup() {
  }

  @Override
  public void eval() {
    org.apache.drill.exec.vector.complex.writer.BaseWriter.MapWriter writer = out.rootAsMap();
    for (int nameIdx = 0, valIdx = 1; valIdx < in.length; nameIdx += 2, valIdx += 2) {
      String fieldName = in[nameIdx].readObject().toString();
      FieldReader reader = in[valIdx];
      org.apache.drill.exec.vector.complex.MapUtility
          .writeToMapFromReader(reader, writer, fieldName, "RowConstructorFunction");
    }
  }
}
