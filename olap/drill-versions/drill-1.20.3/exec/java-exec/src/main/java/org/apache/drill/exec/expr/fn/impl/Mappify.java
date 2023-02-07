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
import org.apache.drill.exec.vector.complex.reader.FieldReader;
import org.apache.drill.exec.vector.complex.writer.BaseWriter.ComplexWriter;

import javax.inject.Inject;

public class Mappify {

  /*
   * The following function can be invoked when we want to convert a map to a repeated map where every
   * with two fields in each entry:
   * key: the name of the field in the original map and
   * value: value of the field
   *
   * For eg, consider the following json file:
   *
   * {"foo": {"obj":1, "bar":10}}
   * {"foo": {"obj":2, "bar":20}}
   *
   * Invoking mappify(foo) would result in the following output
   *
   * [{"key":"obj", "value":1}, {"key":"bar", "value":10}]
   * [{"key":"obj", "value":2}, {"key":"bar", "value":20}]
   *
   * Currently this function only allows
   * simple maps as input
   * scalar value fields
   * value fields need to be of the same data type
   */
  @FunctionTemplate(names = {"mappify", "kvgen"}, scope = FunctionTemplate.FunctionScope.SIMPLE, isRandom = true)
  public static class ConvertMapToKeyValuePairs implements DrillSimpleFunc {

    @Param  FieldReader reader;
    @Inject DrillBuf buffer;
    @Output ComplexWriter writer;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {
      buffer = org.apache.drill.exec.expr.fn.impl.MappifyUtility.mappify(reader, writer, buffer, "Mappify/kvgen");
    }
  }
}
