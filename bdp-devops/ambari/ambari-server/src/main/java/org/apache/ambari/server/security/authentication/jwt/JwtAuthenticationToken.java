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

package org.apache.ambari.server.security.authentication.jwt;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * {@link AbstractAuthenticationToken} implementation for JWT authentication tokens.
 */
public class JwtAuthenticationToken extends AbstractAuthenticationToken {
  private final String username;
  private final String token;

  /**
   * Constructor.
   *
   * @param username           the principal's username
   * @param token              the JWT token (or credential)
   * @param grantedAuthorities the granted authorities
   */
  public JwtAuthenticationToken(String username, String token, Collection<? extends GrantedAuthority> grantedAuthorities) {
    super(grantedAuthorities);
    this.username = username;
    this.token = token;
  }

  @Override
  public Object getCredentials() {
    return token;
  }

  @Override
  public Object getPrincipal() {
    return username;
  }
}
