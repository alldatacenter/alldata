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

import java.util.Collection;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class AmbariUserAuthentication implements Authentication {

  private final String serializedToken;
  private final AmbariUserDetails userDetails;

  private boolean authenticated;

  public AmbariUserAuthentication(String token, AmbariUserDetails userDetails) {
    this(token, userDetails, false);
  }

  public AmbariUserAuthentication(String token, AmbariUserDetails userDetails, boolean authenticated) {
    this.serializedToken = token;
    this.userDetails = userDetails;
    this.authenticated = authenticated;
  }

  @Override
  @JsonIgnore
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return (userDetails == null) ? null : userDetails.getAuthorities();
  }

  @Override
  public String getCredentials() {
    return serializedToken;
  }

  @Override
  public Object getDetails() {
    return null;
  }

  @Override
  public AmbariUserDetails getPrincipal() {
    return userDetails;
  }

  @Override
  public boolean isAuthenticated() {
    return authenticated;
  }

  @Override
  public void setAuthenticated(boolean authenticated) throws IllegalArgumentException {
    this.authenticated = authenticated;
  }

  @Override
  public String getName() {
    return (userDetails == null) ? null : userDetails.getUsername();
  }

  public Integer getUserId() {
    return (userDetails == null) ? null : userDetails.getUserId();
  }
}
