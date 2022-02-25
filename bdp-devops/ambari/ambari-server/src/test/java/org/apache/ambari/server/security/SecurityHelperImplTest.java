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

import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.authentication.AmbariUserAuthentication;
import org.apache.ambari.server.security.authentication.AmbariUserDetailsImpl;
import org.apache.ambari.server.security.authorization.User;
import org.apache.ambari.server.security.authorization.UserName;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class SecurityHelperImplTest {

  private final String USER_FROM_PRINCIPAL = "user from principal";
  private final String USER_DETAILS_USER_NAME = "user details user name";

  @Test
  public void testSecurityHelperWithUser() {
    SecurityContext ctx = SecurityContextHolder.getContext();
    UserEntity userEntity = new UserEntity();
    userEntity.setPrincipal(new PrincipalEntity());
    userEntity.setUserName(UserName.fromString("userName").toString());
    userEntity.setUserId(1);
    User user = new User(userEntity);
    Authentication auth = new AmbariUserAuthentication(null, new AmbariUserDetailsImpl(user, null, null));
    ctx.setAuthentication(auth);

    // Username is expected to be lowercase
    Assert.assertEquals("username", SecurityHelperImpl.getInstance().getCurrentUserName());
  }

  @Test
  public void testSecurityHelperWithUserDetails() {
    SecurityContext ctx = SecurityContextHolder.getContext();
    TestUserDetails userDetails = new TestUserDetails();
    Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null);
    ctx.setAuthentication(auth);

    Assert.assertEquals(USER_DETAILS_USER_NAME, SecurityHelperImpl.getInstance().getCurrentUserName());
  }

  @Test
  public void testSecurityHelperWithUnknownPrincipal() {
    SecurityContext ctx = SecurityContextHolder.getContext();
    Authentication auth = new UsernamePasswordAuthenticationToken(new TestPrincipal(), null);
    ctx.setAuthentication(auth);

    Assert.assertEquals(USER_FROM_PRINCIPAL, SecurityHelperImpl.getInstance().getCurrentUserName());
  }

  class TestUserDetails implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
      return null;
    }

    @Override
    public String getPassword() {
      return null;
    }

    @Override
    public String getUsername() {
      return USER_DETAILS_USER_NAME;
    }

    @Override
    public boolean isAccountNonExpired() {
      return false;
    }

    @Override
    public boolean isAccountNonLocked() {
      return false;
    }

    @Override
    public boolean isCredentialsNonExpired() {
      return false;
    }

    @Override
    public boolean isEnabled() {
      return false;
    }
  }

  class TestPrincipal {
    @Override
    public String toString() {
      return USER_FROM_PRINCIPAL;
    }
  }
}
