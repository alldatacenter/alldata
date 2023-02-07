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
package org.apache.drill.yarn.appMaster.http;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.HttpMethod;
import java.io.IOException;

/**
 * Generates and adds a CSRF token to a HTTP session. It is used to prevent CSRF attacks.
 */
public class CsrfTokenInjectFilter implements Filter {

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    if (HttpMethod.GET.equalsIgnoreCase(httpRequest.getMethod())) {
      // We don't create a session with this call as we need to check if there is a session already (i.e. if a user is logged in).
      HttpSession session = httpRequest.getSession(false);
      if (session != null) {
        String csrfToken = (String) session.getAttribute(WebConstants.CSRF_TOKEN);
        if (csrfToken == null) {
          csrfToken = WebUtils.generateCsrfToken();
          httpRequest.getSession().setAttribute(WebConstants.CSRF_TOKEN, csrfToken);
        }
      }
    }
    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
  }
}
