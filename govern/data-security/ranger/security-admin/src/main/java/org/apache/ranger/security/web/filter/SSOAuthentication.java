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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.security.web.filter;

import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * Internal token which describes JWT authentication
 */
public class SSOAuthentication implements Authentication {

  private SignedJWT token;
  private boolean authenticated = false;

  public SSOAuthentication(SignedJWT token) {
    this.token = token;
  }

  @Override
  public SignedJWT getCredentials() {
    return token;
  }

  @Override
  public Object getDetails() {
    return null;
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
	  return null;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
	  return null;
  }

  @Override
  public Object getPrincipal() {
	  return null;
  }
}
