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
import org.apache.drill.exec.expr.holders.NullableVarCharHolder;
import org.apache.drill.exec.expr.holders.VarCharHolder;
import org.apache.drill.exec.vector.complex.writer.BaseWriter;

import javax.inject.Inject;

/**
 * The {@code parse_query} function splits up a query string and returns a map of the key-value pairs.
 * If input string contains one or more {@code '?'} characters the string will be
 * split by the first occurrence of the character and key-value mapping will be performed for
 * the second part of split string (the part starting after {@code '?'} character) only.
 *
 * <p>For example, {@code parse_query('url?arg1=x&arg2=y')} will return:
 * <pre>
 * {
 *   "arg1": "x",
 *   "arg2": "y"
 * }
 * </pre>
 */
public class ParseQueryFunction {

  @FunctionTemplate(name = "parse_query", scope = FunctionTemplate.FunctionScope.SIMPLE)
  public static class ParseQuery implements DrillSimpleFunc {

    @Param
    VarCharHolder in;
    @Output
    BaseWriter.ComplexWriter outWriter;
    @Inject
    DrillBuf outBuffer;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {
      org.apache.drill.exec.vector.complex.writer.BaseWriter.MapWriter mapWriter = outWriter.rootAsMap();

      String queryString =
          org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(in.start, in.end, in.buffer);
      // Check if input string contains '?' character. If it does - split the string by the first occurrence
      // of the character and preserve the part starting from the '?' exclusively so that only part with query
      // parameters is left.
      int questionMarkIndex = queryString.indexOf("?");
      if (questionMarkIndex > -1) {
        queryString = queryString.substring(questionMarkIndex + 1);
      }

      if (queryString.trim().isEmpty() || queryString.equalsIgnoreCase("null")) {
        queryString = "";
      }

      if (!queryString.isEmpty()) {
        char firstLetter = queryString.charAt(0);

        // If the first character is a &, it doesn't split properly.
        // This checks if the first character is an & and if so, removes it.
        if (firstLetter == '&') {
          queryString = queryString.substring(1);
        }
      }

      String[] queryParameters = queryString.split("&");
      mapWriter.start();
      for (String parameter : queryParameters) {
        String[] keyValue = parameter.split("=", 2);
        if (keyValue.length != 2) {
          // Ignore malformed key-value pair
          continue;
        }

        byte[] valueBytes = keyValue[1].getBytes();
        outBuffer = outBuffer.reallocIfNeeded(valueBytes.length);
        outBuffer.setBytes(0, valueBytes);

        org.apache.drill.exec.expr.holders.VarCharHolder valueHolder =
            new org.apache.drill.exec.expr.holders.VarCharHolder();
        valueHolder.start = 0;
        valueHolder.end = valueBytes.length;
        valueHolder.buffer = outBuffer;

        mapWriter.varChar(keyValue[0]).write(valueHolder);
      }
      mapWriter.end();
    }
  }

  @FunctionTemplate(name = "parse_query", scope = FunctionTemplate.FunctionScope.SIMPLE)
  public static class ParseQueryNullableInput implements DrillSimpleFunc {

    @Param
    NullableVarCharHolder in;
    @Output
    BaseWriter.ComplexWriter outWriter;
    @Inject
    DrillBuf outBuffer;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {
      org.apache.drill.exec.vector.complex.writer.BaseWriter.MapWriter mapWriter = outWriter.rootAsMap();

      if (in.isSet == 0) {
        // Return empty map
        mapWriter.start();
        mapWriter.end();
        return;
      }

      String queryString =
            org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(in.start, in.end, in.buffer);
      // Check if input string contains '?' character. If it does - split the string by the first occurrence
      // of the character and preserve the part starting from the '?' exclusively so that only part with query
      // parameters is left.
      int questionMarkIndex = queryString.indexOf("?");
      if (questionMarkIndex > -1) {
        queryString = queryString.substring(questionMarkIndex + 1);
      }

      if (queryString.trim().isEmpty() || queryString.equalsIgnoreCase("null")) {
        queryString = "";
      }

      if (!queryString.isEmpty()) {
        char firstLetter = queryString.charAt(0);

        // If the first character is a &, it doesn't split properly.
        // This checks if the first character is an & and if so, removes it.
        if (firstLetter == '&') {
          queryString = queryString.substring(1);
        }
      }

      String[] queryParameters = queryString.split("&");
      mapWriter.start();
      for (String parameter : queryParameters) {
        String[] keyValue = parameter.split("=", 2);
        if (keyValue.length != 2) {
          // Ignore malformed key-value pair
          continue;
        }

        byte[] valueBytes = keyValue[1].getBytes();
        outBuffer = outBuffer.reallocIfNeeded(valueBytes.length);
        outBuffer.setBytes(0, valueBytes);

        org.apache.drill.exec.expr.holders.VarCharHolder valueHolder =
            new org.apache.drill.exec.expr.holders.VarCharHolder();
        valueHolder.start = 0;
        valueHolder.end = valueBytes.length;
        valueHolder.buffer = outBuffer;

        mapWriter.varChar(keyValue[0]).write(valueHolder);
      }
      mapWriter.end();
    }
  }
}
