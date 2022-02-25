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

import static org.easymock.EasyMock.expect;

import java.util.Collections;

import org.apache.ambari.server.orm.dao.MemberDAO;
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.ldap.core.DirContextOperations;

@RunWith(PowerMockRunner.class)               // Allow mocking static methods
@PrepareForTest(AuthorizationHelper.class)    // This class has a static method that will be mocked
public class TestAmbariLdapAuthoritiesPopulator extends EasyMockSupport {

  AuthorizationHelper helper = new AuthorizationHelper();
  UserDAO userDAO = createMock(UserDAO.class);
  Users users = createMock(Users.class);
  MemberDAO memberDAO = createMock(MemberDAO.class);
  PrivilegeDAO privilegeDAO = createMock(PrivilegeDAO.class);
  DirContextOperations userData = createMock(DirContextOperations.class);
  UserEntity userEntity = createMock(UserEntity.class);
  PrivilegeEntity privilegeEntity = createMock(PrivilegeEntity.class);

  @Before
  public void setUp() throws Exception {
    resetAll();
    PowerMock.resetAll();
  }

  @Test
  public void testGetGrantedAuthorities() throws Exception {
    String username = "user";

    AmbariLdapAuthoritiesPopulator populator = createMockBuilder(AmbariLdapAuthoritiesPopulator.class)
        .withConstructor(helper, userDAO, memberDAO, privilegeDAO, users).createMock();

    expect(userEntity.getActive()).andReturn(true);
    expect(users.getUserPrivileges(userEntity)).andReturn(Collections.singletonList(privilegeEntity));

    expect(userDAO.findUserByName(username)).andReturn(userEntity);
    replayAll();

    populator.getGrantedAuthorities(userData, username);

    verifyAll();

  }

  @Test
  public void testGetGrantedAuthoritiesWithLoginAlias() throws Exception {
    // Given
    String loginAlias = "testLoginAlias@testdomain.com";
    String ambariUserName = "user";

    PowerMock.mockStatic(AuthorizationHelper.class);
    expect(AuthorizationHelper.resolveLoginAliasToUserName(loginAlias)).andReturn(ambariUserName);

    PowerMock.replay(AuthorizationHelper.class);

    AmbariLdapAuthoritiesPopulator populator = createMockBuilder(AmbariLdapAuthoritiesPopulator.class)
      .withConstructor(helper, userDAO, memberDAO, privilegeDAO, users).createMock();

    expect(userEntity.getActive()).andReturn(true);
    expect(users.getUserPrivileges(userEntity)).andReturn(Collections.singletonList(privilegeEntity));

    expect(userDAO.findUserByName(ambariUserName)).andReturn(userEntity); // user should be looked up by user name instead of login alias

    replayAll();

    // When
    populator.getGrantedAuthorities(userData, loginAlias);

    PowerMock.verify(AuthorizationHelper.class);
    verifyAll();
  }

}
