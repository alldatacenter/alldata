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

package org.apache.ambari.server.view;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.MDC;

import com.google.inject.Singleton;

/**
 * Inserts view name, view version and view instance name into MDC info for views REST for logging in views.
 */
@Singleton
public class AmbariViewsMDCLoggingFilter implements Filter {

  private final static String patternStr = "/api/v1/views/(.*)/versions/(.*)/instances/([^/]+).*";
  private final static Pattern pattern = Pattern.compile(patternStr);

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // do nothing
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

    buildMDC(servletRequest);
    try {
      filterChain.doFilter(servletRequest, servletResponse);
    } finally {
      clear();
    }
  }

  private void buildMDC(ServletRequest request) {
    if ((request instanceof HttpServletRequest) && MDC.getMDCAdapter() != null) {
      String url = ((HttpServletRequest) request).getRequestURI();
      Matcher matcher = pattern.matcher(url);
      if (matcher.find()) {
        MDC.put("viewName", matcher.group(1));
        MDC.put("viewVersion", matcher.group(2));
        MDC.put("viewInstanceName", matcher.group(3));
      }
    }
  }

  private void clear() {
    MDC.clear();
  }

  @Override
  public void destroy() {
    //do nothing
  }
}
