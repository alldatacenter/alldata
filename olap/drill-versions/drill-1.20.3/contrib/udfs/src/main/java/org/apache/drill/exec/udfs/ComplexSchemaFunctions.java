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

package org.apache.drill.exec.udfs;

import io.netty.buffer.DrillBuf;
import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.vector.complex.reader.FieldReader;
import org.apache.drill.exec.vector.complex.writer.BaseWriter;

import javax.inject.Inject;

public class ComplexSchemaFunctions {

  /**
   * This function exists to help the user understand the inner schemata of maps
   * It is NOT recursive (yet).
   */
  @FunctionTemplate(names = {"get_map_schema", "getMapSchema"},
    scope = FunctionTemplate.FunctionScope.SIMPLE,
    nulls = NullHandling.INTERNAL)
  public static class GetMapSchemaFunction implements DrillSimpleFunc {

    @Param
    FieldReader reader;

    @Output
    BaseWriter.ComplexWriter outWriter;

    @Inject
    DrillBuf outBuffer;

    @Override
    public void setup() {
      // Nothing to see here...
    }

    @Override
    public void eval() {
      if (reader.isSet()) {
        outBuffer = org.apache.drill.exec.udfs.ComplexSchemaUtils.getFields(reader, outWriter, outBuffer);
      } else {
        org.apache.drill.exec.vector.complex.writer.BaseWriter.MapWriter queryMapWriter = outWriter.rootAsMap();
        // Return empty map
        queryMapWriter.start();
        queryMapWriter.end();
      }
    }
  }
}
