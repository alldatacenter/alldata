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

import javax.inject.Inject;

import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.FunctionScope;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.holders.VarBinaryHolder;
import org.apache.drill.exec.vector.complex.reader.FieldReader;

/**
 * The two functions defined here convert_toJSON and convert_toEXTENDEDJSON are almost
 * identical. For now, the default behavior is to use simple JSON (see DRILL-2976). Until the issues with
 * extended JSON types are resolved, the convert_toJSON/convert_toSIMPLEJSON is consider the default. The default
 * will possibly change in the future, as the extended types can accurately serialize more types supported
 * by Drill.
 * TODO(DRILL-2906) - review the default once issues with extended JSON are resolved
 */
public class JsonConvertTo {

 static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JsonConvertTo.class);

  private JsonConvertTo(){}

  @FunctionTemplate(names = { "convert_toJSON", "convert_toSIMPLEJSON" },
                    scope = FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL,
                    outputWidthCalculatorType = FunctionTemplate.OutputWidthCalculatorType.CUSTOM_FIXED_WIDTH_DEFAULT)
  public static class ConvertToJson implements DrillSimpleFunc{

    @Param FieldReader input;
    @Output VarBinaryHolder out;
    @Inject DrillBuf buffer;

    public void setup(){
    }

    public void eval(){
      out.start = 0;

      java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
      try {
        org.apache.drill.exec.vector.complex.fn.JsonWriter jsonWriter = new org.apache.drill.exec.vector.complex.fn.JsonWriter(stream, true, false);

        jsonWriter.write(input);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      byte [] bytea = stream.toByteArray();

      out.buffer = buffer = buffer.reallocIfNeeded(bytea.length);
      out.buffer.setBytes(0, bytea);
      out.end = bytea.length;
    }
  }

  @FunctionTemplate(name = "convert_toEXTENDEDJSON", scope = FunctionScope.SIMPLE, nulls = NullHandling.NULL_IF_NULL,
                    outputWidthCalculatorType = FunctionTemplate.OutputWidthCalculatorType.CUSTOM_FIXED_WIDTH_DEFAULT)
  public static class ConvertToExtendedJson implements DrillSimpleFunc{

    @Param FieldReader input;
    @Output VarBinaryHolder out;
    @Inject DrillBuf buffer;

    public void setup(){
    }

    public void eval(){
      out.start = 0;

      java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
      try {
        org.apache.drill.exec.vector.complex.fn.JsonWriter jsonWriter = new org.apache.drill.exec.vector.complex.fn.JsonWriter(stream, true, true);

        jsonWriter.write(input);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      byte [] bytea = stream.toByteArray();

      out.buffer = buffer = buffer.reallocIfNeeded(bytea.length);
      out.buffer.setBytes(0, bytea);
      out.end = bytea.length;
    }
  }

}
