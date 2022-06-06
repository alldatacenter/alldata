/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;

public class MethodOverrideFilter implements Filter {
  private static final String HEADER_NAME = "X-Http-Method-Override";
  //limit override to GET method only (no need for others now)
  private static final List<String> ALLOWED_METHODS = new ArrayList<String>(){{
    add("GET");
  }};

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {

  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    if (request instanceof HttpServletRequest) {
      HttpServletRequest httpServletRequest = (HttpServletRequest) request;

      String method = httpServletRequest.getHeader(HEADER_NAME);
      if (method != null) {
        if (ALLOWED_METHODS.contains(method.toUpperCase())) {
          final HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase());
          HttpServletRequestWrapper requestWrapper = new HttpServletRequestWrapper(httpServletRequest){
            @Override
            public String getMethod() {
              return httpMethod.toString();
            }
          };

          chain.doFilter(requestWrapper, response);
          return;
        } else {
          HttpServletResponse httpResponse = (HttpServletResponse) response;
          httpResponse.sendError(400, "Incorrect HTTP method for override: "+ method + ". Allowed values: "+ ALLOWED_METHODS);
        }
      }
    }
    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {

  }
}
