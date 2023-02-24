/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.web.servlets;

import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.exception.AtlasBaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import java.io.PrintWriter;

public class AtlasLoginServlet extends AtlasHttpServlet {
    public static final Logger LOG = LoggerFactory.getLogger(AtlasLoginServlet.class);

    public static final String LOGIN_HTML_TEMPLATE = "/login.html.template";

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) {
        try {
            if (!request.getMethod().equals(HttpMethod.GET)) {
                response.setContentType(TEXT_HTML);
                response.setHeader(ALLOW, HttpMethod.GET);
                response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);

                String errorMessage = AtlasErrorCode.METHOD_NOT_ALLOWED.getFormattedErrorMessage(request.getMethod(), request.getRequestURI());
                PrintWriter out     = response.getWriter();
                out.println(errorMessage);

                throw new AtlasBaseException(errorMessage);
            }

            includeResponse(request, response, LOGIN_HTML_TEMPLATE);

        } catch (Exception e) {
            LOG.error("Error in AtlasLoginServlet", LOGIN_HTML_TEMPLATE, e);
        }
    }
}