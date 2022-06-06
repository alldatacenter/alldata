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

package org.apache.ambari.server.security.authentication.kerberos;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.util.Collection;
import java.util.Collections;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.UserAuthenticationEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.authentication.AmbariUserDetails;
import org.apache.ambari.server.security.authentication.UserNotFoundException;
import org.apache.ambari.server.security.authorization.AmbariGrantedAuthority;
import org.apache.ambari.server.security.authorization.UserAuthenticationType;
import org.apache.ambari.server.security.authorization.Users;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import junit.framework.Assert;

public class AmbariAuthToLocalUserDetailsServiceTest extends EasyMockSupport {
  @Before
  public void setup() {
    // These system properties need to be set to properly configure the KerberosName object when
    // a krb5.conf file is not available
    System.setProperty("java.security.krb5.realm", "EXAMPLE.COM");
    System.setProperty("java.security.krb5.kdc", "localhost");
  }

  @Test
  public void loadUserByUsernameSuccess() throws Exception {
    AmbariKerberosAuthenticationProperties properties = new AmbariKerberosAuthenticationProperties();

    Configuration configuration = createMock(Configuration.class);
    expect(configuration.getKerberosAuthenticationProperties()).andReturn(properties).once();

    PrincipalEntity principal = createMock(PrincipalEntity.class);
    expect(principal.getPrivileges()).andReturn(Collections.emptySet()).atLeastOnce();

    UserEntity userEntity = createMock(UserEntity.class);
    expect(userEntity.getUserName()).andReturn("user1").atLeastOnce();
    expect(userEntity.getUserId()).andReturn(1).atLeastOnce();
    expect(userEntity.getCreateTime()).andReturn(System.currentTimeMillis()).atLeastOnce();
    expect(userEntity.getActive()).andReturn(true).atLeastOnce();
    expect(userEntity.getMemberEntities()).andReturn(Collections.emptySet()).atLeastOnce();
    expect(userEntity.getAuthenticationEntities()).andReturn(Collections.emptyList()).atLeastOnce();
    expect(userEntity.getPrincipal()).andReturn(principal).atLeastOnce();

    UserAuthenticationEntity kerberosAuthenticationEntity = createMock(UserAuthenticationEntity.class);
    expect(kerberosAuthenticationEntity.getAuthenticationType()).andReturn(UserAuthenticationType.KERBEROS).anyTimes();
    expect(kerberosAuthenticationEntity.getAuthenticationKey()).andReturn("user1@EXAMPLE.COM").anyTimes();
    expect(kerberosAuthenticationEntity.getUser()).andReturn(userEntity).anyTimes();

    Collection<AmbariGrantedAuthority> userAuthorities = Collections.singletonList(createNiceMock(AmbariGrantedAuthority.class));

    Users users = createMock(Users.class);
    expect(users.getUserAuthorities(userEntity)).andReturn(userAuthorities).atLeastOnce();
    expect(users.getUserAuthenticationEntities(UserAuthenticationType.KERBEROS, "user1@EXAMPLE.COM"))
        .andReturn(Collections.singleton(kerberosAuthenticationEntity)).atLeastOnce();
    users.validateLogin(userEntity, "user1");
    expectLastCall().once();

    replayAll();

    UserDetailsService userdetailsService = new AmbariAuthToLocalUserDetailsService(configuration, users);

    UserDetails userDetails = userdetailsService.loadUserByUsername("user1@EXAMPLE.COM");

    Assert.assertNotNull(userDetails);
    Assert.assertTrue(userDetails instanceof AmbariUserDetails);

    AmbariUserDetails ambariUserDetails = (AmbariUserDetails) userDetails;
    Assert.assertEquals("user1", ambariUserDetails.getUsername());
    Assert.assertEquals(Integer.valueOf(1), ambariUserDetails.getUserId());
    Assert.assertEquals(userAuthorities.size(), ambariUserDetails.getAuthorities().size());

    verifyAll();
  }

  @Test(expected = UserNotFoundException.class)
  public void loadUserByUsernameUserNotFound() throws Exception {
    AmbariKerberosAuthenticationProperties properties = new AmbariKerberosAuthenticationProperties();

    Configuration configuration = createMock(Configuration.class);
    expect(configuration.getKerberosAuthenticationProperties()).andReturn(properties).once();

    Users users = createMock(Users.class);
    expect(users.getUserEntity("user1")).andReturn(null).times(2);
    expect(users.getUserAuthenticationEntities(UserAuthenticationType.KERBEROS, "user1@EXAMPLE.COM"))
        .andReturn(null).atLeastOnce();

    replayAll();

    UserDetailsService userdetailsService = new AmbariAuthToLocalUserDetailsService(configuration, users);

    userdetailsService.loadUserByUsername("user1@EXAMPLE.COM");

    verifyAll();

    Assert.fail("UsernameNotFoundException was not thrown");
  }

}