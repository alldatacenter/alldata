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
 * The {@code parse_url} function takes an URL and returns a map of components of the URL.
 * It acts as a wrapper for {@link java.net.URL}. If an optional URL component is absent,
 * e.g. there is no anchor (reference) element, the {@code "ref"} entry will not be added to resulting map.
 *
 * <p>For example, {@code parse_url('http://example.com/some/path?key=value#ref')} will return:
 * <pre>
 * {
 *   "protocol":"http",
 *   "authority":"example.com",
 *   "host":"example.com",
 *   "path":"/some/path",
 *   "query":"key=value",
 *   "filename":"/some/path?key=value",
 *   "ref":"ref"
 * }
 * </pre>
 */
public class ParseUrlFunction {

  @FunctionTemplate(name = "parse_url", scope = FunctionTemplate.FunctionScope.SIMPLE)
  public static class ParseUrl implements DrillSimpleFunc {

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

      String urlString =
          org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(in.start, in.end, in.buffer);
      try {
        mapWriter.start();

        java.net.URL aURL = new java.net.URL(urlString);
        // Diamond operator can not be used here because Janino does not support
        // type inference for generic instance creation.
        // LinkedHashMap is used to preserve insertion-order.
        java.util.Map<String, String> urlComponents = new java.util.LinkedHashMap<String, String>();
        urlComponents.put("protocol", aURL.getProtocol());
        urlComponents.put("authority", aURL.getAuthority());
        urlComponents.put("host", aURL.getHost());
        urlComponents.put("path", aURL.getPath());
        urlComponents.put("query", aURL.getQuery());
        urlComponents.put("filename", aURL.getFile());
        urlComponents.put("ref", aURL.getRef());

        org.apache.drill.exec.expr.holders.VarCharHolder rowHolder =
            new org.apache.drill.exec.expr.holders.VarCharHolder();
        for (java.util.Map.Entry<String, String> entry : urlComponents.entrySet()) {
          if (entry.getValue() != null) {
            // Explicit casting to String is required because of Janino's limitations regarding generics.
            byte[] protocolBytes = ((String) entry.getValue()).getBytes();
            outBuffer = outBuffer.reallocIfNeeded(protocolBytes.length);
            outBuffer.setBytes(0, protocolBytes);
            rowHolder.start = 0;
            rowHolder.end = protocolBytes.length;
            rowHolder.buffer = outBuffer;
            mapWriter.varChar((String) entry.getKey()).write(rowHolder);
          }
        }

        java.lang.Integer port = aURL.getPort();
        // If port number is not specified in URL string, it is assigned a default value of -1.
        // Include port number into resulting map only if it was specified.
        if (port != -1) {
          org.apache.drill.exec.expr.holders.IntHolder intHolder = new org.apache.drill.exec.expr.holders.IntHolder();
          intHolder.value = port;
          mapWriter.integer("port").write(intHolder);
        }

        mapWriter.end();
      } catch (Exception e) {
        // Enclose map
        mapWriter.end();
      }
    }
  }

  @FunctionTemplate(name = "parse_url", scope = FunctionTemplate.FunctionScope.SIMPLE)
  public static class ParseUrlNullableInput implements DrillSimpleFunc {

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

      String urlString =
          org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(in.start, in.end, in.buffer);
      try {
        mapWriter.start();

        java.net.URL aURL = new java.net.URL(urlString);
        // Diamond operator can not be used here because Janino does not support
        // type inference for generic instance creation.
        // LinkedHashMap is used to preserve insertion-order.
        java.util.Map<String, String> urlComponents = new java.util.LinkedHashMap<String, String>();
        urlComponents.put("protocol", aURL.getProtocol());
        urlComponents.put("authority", aURL.getAuthority());
        urlComponents.put("host", aURL.getHost());
        urlComponents.put("path", aURL.getPath());
        urlComponents.put("query", aURL.getQuery());
        urlComponents.put("filename", aURL.getFile());
        urlComponents.put("ref", aURL.getRef());

        org.apache.drill.exec.expr.holders.VarCharHolder rowHolder =
            new org.apache.drill.exec.expr.holders.VarCharHolder();
        for (java.util.Map.Entry<String, String> entry : urlComponents.entrySet()) {
          if (entry.getValue() != null) {
            // Explicit casting to String is required because of Janino's limitations regarding generics.
            byte[] protocolBytes = ((String) entry.getValue()).getBytes();
            outBuffer = outBuffer.reallocIfNeeded(protocolBytes.length);
            outBuffer.setBytes(0, protocolBytes);
            rowHolder.start = 0;
            rowHolder.end = protocolBytes.length;
            rowHolder.buffer = outBuffer;
            mapWriter.varChar((String) entry.getKey()).write(rowHolder);
          }
        }

        java.lang.Integer port = aURL.getPort();
        // If port number is not specified in URL string, it is assigned a default value of -1.
        // Include port number into resulting map only if it was specified.
        if (port != -1) {
          org.apache.drill.exec.expr.holders.IntHolder intHolder = new org.apache.drill.exec.expr.holders.IntHolder();
          intHolder.value = port;
          mapWriter.integer("port").write(intHolder);
        }

        mapWriter.end();
      } catch (Exception e) {
        // Enclose map
        mapWriter.end();
      }
    }
  }
}
