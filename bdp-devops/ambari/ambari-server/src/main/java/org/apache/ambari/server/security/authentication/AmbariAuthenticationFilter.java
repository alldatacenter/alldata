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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * AmbariAuthenticationFilter is a {@link Filter} interface to be implemented
 * by authentication filters used by the Ambari server. More specifically, implementations
 * of this interface may be used in the {@link AmbariDelegatingAuthenticationFilter}.
 *
 * @see AmbariDelegatingAuthenticationFilter
 */
public interface AmbariAuthenticationFilter extends Filter {

  /**
   * Tests this AmbariAuthenticationFilter to see if it should be applied to the filter chain
   * - meaning its {@link Filter#doFilter(ServletRequest, ServletResponse, FilterChain)} method
   * should be called.
   * <p>
   * Each implementation will have its own requirements on whether it should be applied and care must
   * be taken such that <code>true</code> is returned on if warranted, else other implementations may
   * not get a chance to execute
   *
   * @param httpServletRequest the HttpServletRequest
   * @return true if this AmbariAuthenticationFilter should be applied to the filter chain; otherwise false.
   */
  boolean shouldApply(HttpServletRequest httpServletRequest);

  /**
   * Tests this AmbariAuthenticationFilter to see if authentication failures should count towards
   * the consecutive authentication failure count.
   * <p>
   * This should typically be false for remote authentication sources such as LDAP or JWT.
   *
   * @return true if authentication failure should be counted; false, otherwise
   */
  boolean shouldIncrementFailureCount();
}
