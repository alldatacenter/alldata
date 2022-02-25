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

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * AmbariProxiedUserDetailsImpl is an {@link AmbariUserDetails} implementation that contains details
 * about the proxied user as well as the proxy user.
 * <p>
 * This data can be helpful with logging information about the user performing an action.
 */
public class AmbariProxiedUserDetailsImpl implements AmbariUserDetails {

  /**
   * Details about the acting user.
   * <p>
   * This user was specified as the doAs or proxied user during a trusted proxy authentication attempt
   */
  private final UserDetails proxiedUserDetails;

  /**
   * Details about the proxy user that was authenticated.
   * <p>
   * This user that authenticated wth Ambari but specified a doAs or proxied user to be used as the acting user for operations.
   */
  private final AmbariProxyUserDetails proxyUserDetails;

  public AmbariProxiedUserDetailsImpl(UserDetails proxiedUserDetails, AmbariProxyUserDetails proxyUserDetails) {
    this.proxiedUserDetails = proxiedUserDetails;
    this.proxyUserDetails = proxyUserDetails;
  }

  @Override
  public Integer getUserId() {
    return (proxiedUserDetails instanceof AmbariUserDetails) ? ((AmbariUserDetails) proxiedUserDetails).getUserId() : null;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return (proxiedUserDetails == null) ? null : proxiedUserDetails.getAuthorities();
  }

  @Override
  public String getPassword() {
    return (proxiedUserDetails == null) ? null : proxiedUserDetails.getPassword();
  }

  @Override
  public String getUsername() {
    return (proxiedUserDetails == null) ? null : proxiedUserDetails.getUsername();
  }

  @Override
  public boolean isAccountNonExpired() {
    return (proxiedUserDetails != null) && proxiedUserDetails.isAccountNonExpired();
  }

  @Override
  public boolean isAccountNonLocked() {
    return (proxiedUserDetails != null) && proxiedUserDetails.isAccountNonLocked();
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return (proxiedUserDetails != null) && proxiedUserDetails.isCredentialsNonExpired();
  }

  @Override
  public boolean isEnabled() {
    return (proxiedUserDetails != null) && proxiedUserDetails.isEnabled();
  }

  public AmbariProxyUserDetails getProxyUserDetails() {
    return proxyUserDetails;
  }
}
