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

package org.apache.ambari.server.security.authorization;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.scheduler.ExecutionScheduleManager;
import org.apache.ambari.server.security.authentication.AmbariUserAuthentication;
import org.apache.ambari.server.security.authentication.AmbariUserDetails;
import org.apache.ambari.server.security.authentication.AmbariUserDetailsImpl;
import org.apache.ambari.server.security.authorization.internal.InternalTokenClientFilter;
import org.apache.ambari.server.security.authorization.internal.InternalTokenStorage;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.Inject;

public class AmbariUserAuthorizationFilter implements Filter {

  private final InternalTokenStorage internalTokenStorage;
  private final Users users;

  @Inject
  public AmbariUserAuthorizationFilter(InternalTokenStorage internalTokenStorage, Users users) {
    this.internalTokenStorage = internalTokenStorage;
    this.users = users;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // do nothing
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    String token = httpRequest.getHeader(InternalTokenClientFilter.INTERNAL_TOKEN_HEADER);
    if (token != null) {
      if (internalTokenStorage.isValidInternalToken(token)) {
        String userToken = httpRequest.getHeader(ExecutionScheduleManager.USER_ID_HEADER);
        if (userToken != null) {
          if (!NumberUtils.isDigits(userToken)) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid user ID");
            httpResponse.flushBuffer();
            return;
          }
          Integer userId = Integer.parseInt(userToken);
          UserEntity userEntity = users.getUserEntity(userId);
          if (userEntity == null) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Authentication required");
            httpResponse.flushBuffer();
            return;
          }
          if (!userEntity.getActive()) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "User is not active");
            httpResponse.flushBuffer();
            return;
          } else {
            AmbariUserDetails userDetails = new AmbariUserDetailsImpl(users.getUser(userEntity), null, users.getUserAuthorities(userEntity));
            AmbariUserAuthentication authentication = new AmbariUserAuthentication(token, userDetails, true);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            httpResponse.setHeader("User", AuthorizationHelper.getAuthenticatedName());
          }
        }
      }
    }
    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
    // do nothing
  }
}
