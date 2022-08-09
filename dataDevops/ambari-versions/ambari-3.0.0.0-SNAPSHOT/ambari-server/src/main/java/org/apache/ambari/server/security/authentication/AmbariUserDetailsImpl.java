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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.ambari.server.security.authorization.User;
import org.springframework.security.core.GrantedAuthority;

/**
 * AmbariUserDetails is an implementation of {@link AmbariUserDetails}
 * <p>
 * Ideally instances of this class are used as the value returned by {@link org.springframework.security.core.Authentication#getPrincipal()}
 */
public class AmbariUserDetailsImpl implements AmbariUserDetails {

  private final User user;
  private final String password;
  private final Collection<? extends GrantedAuthority> grantedAuthorities;

  public AmbariUserDetailsImpl(User user, String password, Collection<? extends GrantedAuthority> grantedAuthorities) {
    this.user = user;
    this.password = password;
    this.grantedAuthorities = (grantedAuthorities == null)
        ? Collections.emptyList()
        : Collections.unmodifiableCollection(new ArrayList<>(grantedAuthorities));
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return grantedAuthorities;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return (user == null) ? null : user.getUserName();
  }

  @Override
  public Integer getUserId() {
    return (user == null) ? null : user.getUserId();
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return (user != null) && user.isActive();
  }
}
