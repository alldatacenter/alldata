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
package org.apache.drill.exec.server.rest.auth;

import org.apache.drill.exec.server.rest.WebServerConstants;
import org.eclipse.jetty.server.handler.ErrorHandler;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Writer;

/**
 * Custom ErrorHandler class for Drill's WebServer to have better error message in case when SPNEGO login failed and
 * what to do next. In all other cases this would use the generic error page.
 */
public class DrillErrorHandler extends ErrorHandler {

  @Override
  protected void writeErrorPageMessage(HttpServletRequest request, Writer writer,
                                       int code, String message, String uri) throws IOException {

    super.writeErrorPageMessage(request, writer, code, message, uri);

    if (uri.equals(WebServerConstants.SPENGO_LOGIN_RESOURCE_PATH)) {
      writer.write("<p>SPNEGO Login Failed</p>");
      writer.write("<p>Please check the requirements or use below link to use Form Authentication instead</p>");
      writer.write("<a href='/login'> login </a>");
    }
  }
}