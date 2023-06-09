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
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.holders.NullableVarCharHolder;
import org.apache.drill.exec.expr.holders.VarCharHolder;
import org.apache.drill.exec.vector.complex.writer.BaseWriter;

import javax.inject.Inject;

public class UserAgentFunctions {

  @FunctionTemplate(name = "parse_user_agent",
    scope = FunctionTemplate.FunctionScope.SIMPLE
  )
  public static class UserAgentFunction implements DrillSimpleFunc {
    @Param
    VarCharHolder input;

    @Output
    BaseWriter.ComplexWriter outWriter;

    @Inject
    DrillBuf outBuffer;

    @Workspace
    nl.basjes.parse.useragent.UserAgentAnalyzer uaa;

    @Workspace
    java.util.List<String> allFields;

    public void setup() {
      uaa = org.apache.drill.exec.udfs.UserAgentAnalyzerProvider.getInstance();
      allFields = uaa.getAllPossibleFieldNamesSorted();
    }

    public void eval() {
      org.apache.drill.exec.vector.complex.writer.BaseWriter.MapWriter queryMapWriter = outWriter.rootAsMap();

      String userAgentString = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.getStringFromVarCharHolder(input);

      nl.basjes.parse.useragent.UserAgent agent = uaa.parse(userAgentString);

      for (String fieldName: allFields) {

        org.apache.drill.exec.expr.holders.VarCharHolder rowHolder = new org.apache.drill.exec.expr.holders.VarCharHolder();
        String field = agent.getValue(fieldName);

        byte[] rowStringBytes = field.getBytes();
        outBuffer = outBuffer.reallocIfNeeded(rowStringBytes.length);
        outBuffer.setBytes(0, rowStringBytes);

        rowHolder.start = 0;
        rowHolder.end = rowStringBytes.length;
        rowHolder.buffer = outBuffer;

        queryMapWriter.varChar(fieldName).write(rowHolder);
      }
    }
  }

  @FunctionTemplate(name = "parse_user_agent",
    scope = FunctionTemplate.FunctionScope.SIMPLE
  )
  public static class NullableUserAgentFunction implements DrillSimpleFunc {
    @Param
    NullableVarCharHolder input;

    @Output
    BaseWriter.ComplexWriter outWriter;

    @Inject
    DrillBuf outBuffer;

    @Workspace
    nl.basjes.parse.useragent.UserAgentAnalyzer uaa;

    @Workspace
    java.util.List<String> allFields;

    public void setup() {
      uaa = org.apache.drill.exec.udfs.UserAgentAnalyzerProvider.getInstance();
      allFields = uaa.getAllPossibleFieldNamesSorted();
    }

    public void eval() {
      org.apache.drill.exec.vector.complex.writer.BaseWriter.MapWriter queryMapWriter = outWriter.rootAsMap();
      if (input.isSet == 0) {
        // Return empty map
        queryMapWriter.start();
        queryMapWriter.end();
        return;
      }
      String userAgentString = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.getStringFromVarCharHolder(input);

      nl.basjes.parse.useragent.UserAgent agent = uaa.parse(userAgentString);

      for (String fieldName: allFields) {

        org.apache.drill.exec.expr.holders.VarCharHolder rowHolder = new org.apache.drill.exec.expr.holders.VarCharHolder();
        String field = agent.getValue(fieldName);

        byte[] rowStringBytes = field.getBytes();
        outBuffer = outBuffer.reallocIfNeeded(rowStringBytes.length);
        outBuffer.setBytes(0, rowStringBytes);

        rowHolder.start = 0;
        rowHolder.end = rowStringBytes.length;
        rowHolder.buffer = outBuffer;

        queryMapWriter.varChar(fieldName).write(rowHolder);
      }
    }
  }

  @FunctionTemplate(name = "parse_user_agent",
    scope = FunctionTemplate.FunctionScope.SIMPLE, nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)

  public static class UserAgentFieldFunction implements DrillSimpleFunc {
    @Param
    VarCharHolder input;

    @Param
    VarCharHolder desiredField;

    @Output
    VarCharHolder out;

    @Inject
    DrillBuf outBuffer;

    @Workspace
    nl.basjes.parse.useragent.UserAgentAnalyzer uaa;

    public void setup() {
      uaa = org.apache.drill.exec.udfs.UserAgentAnalyzerProvider.getInstance();
    }

    public void eval() {
      String userAgentString = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.getStringFromVarCharHolder(input);
      String requestedField = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.getStringFromVarCharHolder(desiredField);

      nl.basjes.parse.useragent.UserAgent agent = uaa.parse(userAgentString);
      String field = agent.getValue(requestedField);

      byte[] rowStringBytes = field.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      outBuffer = outBuffer.reallocIfNeeded(rowStringBytes.length);
      outBuffer.setBytes(0, rowStringBytes);

      out.start = 0;
      out.end = rowStringBytes.length;
      out.buffer = outBuffer;
    }
  }
}
