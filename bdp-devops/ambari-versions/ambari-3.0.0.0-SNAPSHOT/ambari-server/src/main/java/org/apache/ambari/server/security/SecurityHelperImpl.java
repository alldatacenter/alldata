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

package org.apache.ambari.server.security;

import java.util.Collection;
import java.util.Collections;

import org.apache.ambari.server.security.authorization.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Security related utilities.
 */
// TODO : combine with AuthorizationHelper.
public class SecurityHelperImpl implements SecurityHelper {

  /**
   * The singleton instance.
   */
  private static final SecurityHelper singleton = new SecurityHelperImpl();


  // ----- Constructors --------------------------------------------------

  /**
   * Hidden constructor.
   */
  private SecurityHelperImpl() {
  }


  // ----- SecurityHelperImpl --------------------------------------------

  public static SecurityHelper getInstance() {
    return singleton;
  }


  // ----- SecurityHelper ------------------------------------------------

  @Override
  public String getCurrentUserName() {
    SecurityContext ctx = SecurityContextHolder.getContext();
    Authentication authentication = ctx == null ? null : ctx.getAuthentication();
    Object principal = authentication == null ? null : authentication.getPrincipal();

    String username;
    if (principal instanceof UserDetails) {
      username = ((UserDetails) principal).getUsername();
    } else if (principal instanceof User) {
      username = ((User) principal).getUserName();
    } else {
      username = principal == null ? "" : principal.toString();
    }
    return username;
  }

  @Override
  public Collection<? extends GrantedAuthority> getCurrentAuthorities()
  {
    SecurityContext context = SecurityContextHolder.getContext();

    Authentication authentication = context.getAuthentication();

    if (context.getAuthentication() != null && context.getAuthentication().isAuthenticated()) {
      return authentication.getAuthorities();
    }
    return Collections.emptyList();
  }
}
