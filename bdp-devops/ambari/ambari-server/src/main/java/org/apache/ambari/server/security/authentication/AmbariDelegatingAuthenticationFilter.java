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
package org.apache.ambari.server.security.authentication;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The AmbariDelegatingAuthenticationFilter is an authentication filter that holds zero or more
 * {@link org.apache.ambari.server.security.authorization.AmbariAuthorizationFilter}s.
 * <p>
 * This container iterates though the contained Filters to delegate {@link Filter} operations.
 * For {@link Filter#init(FilterConfig)} and {@link Filter#destroy()} operations, all contained filters
 * will be called.  For {@link Filter#doFilter(ServletRequest, ServletResponse, FilterChain)}, each
 * filter will be accessed in order to test whether is should be used or skipped.  Once a filter
 * claims it is to be used for the operation, interation stops ensuring at most only one of the contained
 * filters is invoked.
 */
@Component
public class AmbariDelegatingAuthenticationFilter implements Filter {
  private static final Logger LOG = LoggerFactory.getLogger(AmbariDelegatingAuthenticationFilter.class);

  /**
   * The ordered collections of {@link org.apache.ambari.server.security.authorization.AmbariAuthorizationFilter}s
   */
  private final Collection<AmbariAuthenticationFilter> filters;

  /**
   * Constructor.
   *
   * @param filters an ordered collections of {@link org.apache.ambari.server.security.authorization.AmbariAuthorizationFilter}s
   */
  public AmbariDelegatingAuthenticationFilter(Collection<AmbariAuthenticationFilter> filters) {

    this.filters = (filters == null) ? Collections.emptyList() : filters;

    if (this.filters.isEmpty()) {
      LOG.warn("The delegated filters list is empty. No authentication tests will be performed by this " +
          "authentication filter.");
    } else if (LOG.isDebugEnabled()) {
      StringBuffer filterNames = new StringBuffer();

      for (AmbariAuthenticationFilter filter : this.filters) {
        filterNames.append("\n\t");
        filterNames.append(filter.getClass().getName());
      }

      LOG.debug("This authentication filter will attempt to authenticate a user using one of the " +
              "following delegated authentication filters: {}",
          filterNames);
    }
  }

  /**
   * Iterates over the contained {@link org.apache.ambari.server.security.authorization.AmbariAuthorizationFilter}s
   * to invoke each's init method.
   *
   * @param filterConfig a filter configuration
   * @throws ServletException
   */
  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    for (AmbariAuthenticationFilter filter : filters) {
      filter.init(filterConfig);
    }
  }

  /**
   * Iterates over the contained {@link org.apache.ambari.server.security.authorization.AmbariAuthorizationFilter}s
   * to test each to see if they should be invoked. If one tests positive, its doFilter method will be
   * invoked and processing will stop ensuring only at most one of the contained filter is invoked.
   *
   * @param servletRequest  the request
   * @param servletResponse the response
   * @param chain           the Spring filter change
   * @throws IOException
   * @throws ServletException
   */
  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
      throws IOException, ServletException {
    boolean handled = false;
    HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

    for (AmbariAuthenticationFilter filter : filters) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Attempting to apply authentication filter {}", filter.getClass().getName());
      }

      if (filter.shouldApply(httpServletRequest)) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Using authentication filter {} since it applies", filter.getClass().getName());
        }

        filter.doFilter(servletRequest, servletResponse, chain);
        handled = true;
        break;
      } else {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Filter {} does not apply skipping", filter.getClass().getName());
        }
      }
    }

    if (!handled) {
      LOG.debug("No delegated filters applied while attempting to authenticate a user, continuing with the filter chain.");
      chain.doFilter(servletRequest, servletResponse);
    }
  }

  /**
   * Iterates over the contained {@link org.apache.ambari.server.security.authorization.AmbariAuthorizationFilter}s
   * to invoke each's destroy method.
   */
  @Override
  public void destroy() {
    for (AmbariAuthenticationFilter filter : filters) {
      filter.destroy();
    }
  }
}
