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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.AmbariServerConfiguration;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.hooks.HookContextFactory;
import org.apache.ambari.server.hooks.HookService;
import org.apache.ambari.server.ldap.service.AmbariLdapConfigurationProvider;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.MemberDAO;
import org.apache.ambari.server.orm.dao.PrincipalDAO;
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.GroupEntity;
import org.apache.ambari.server.orm.entities.MemberEntity;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.encryption.Encryptor;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import junit.framework.Assert;

public class UsersTest extends EasyMockSupport {

  private static final String SERVICEOP_USER_NAME = "serviceopuser";
  private Injector injector;

  @Test
  public void testGetUserAuthorities() throws Exception {
    createInjector();

    PrincipalEntity userPrincipalEntity = createMock(PrincipalEntity.class);

    UserEntity userEntity = createMock(UserEntity.class);
    expect(userEntity.getPrincipal()).andReturn(userPrincipalEntity).times(1);

    UserDAO userDAO = injector.getInstance(UserDAO.class);
    expect(userDAO.findUserByName("user1")).andReturn(userEntity).times(1);

    PrincipalEntity groupPrincipalEntity = createMock(PrincipalEntity.class);

    GroupEntity groupEntity = createMock(GroupEntity.class);
    expect(groupEntity.getPrincipal()).andReturn(groupPrincipalEntity).times(1);

    MemberEntity memberEntity = createMock(MemberEntity.class);
    expect(memberEntity.getGroup()).andReturn(groupEntity).times(1);

    MemberDAO memberDAO = injector.getInstance(MemberDAO.class);
    expect(memberDAO.findAllMembersByUser(userEntity)).andReturn(Collections.singletonList(memberEntity)).times(1);

    PrincipalEntity clusterUserPrivilegePermissionPrincipalEntity = createMock(PrincipalEntity.class);

    PermissionEntity clusterUserPrivilegePermissionEntity = createMock(PermissionEntity.class);
    expect(clusterUserPrivilegePermissionEntity.getPrincipal()).andReturn(clusterUserPrivilegePermissionPrincipalEntity).times(1);

    PrivilegeEntity clusterUserPrivilegeEntity = createMock(PrivilegeEntity.class);
    expect(clusterUserPrivilegeEntity.getPermission()).andReturn(clusterUserPrivilegePermissionEntity).times(1);

    PrincipalEntity clusterOperatorPrivilegePermissionPrincipalEntity = createMock(PrincipalEntity.class);

    PermissionEntity clusterOperatorPrivilegePermissionEntity = createMock(PermissionEntity.class);
    expect(clusterOperatorPrivilegePermissionEntity.getPrincipal()).andReturn(clusterOperatorPrivilegePermissionPrincipalEntity).times(1);

    PrivilegeEntity clusterOperatorPrivilegeEntity = createMock(PrivilegeEntity.class);
    expect(clusterOperatorPrivilegeEntity.getPermission()).andReturn(clusterOperatorPrivilegePermissionEntity).times(1);

    List<PrivilegeEntity> privilegeEntities = new ArrayList<>();
    privilegeEntities.add(clusterUserPrivilegeEntity);
    privilegeEntities.add(clusterOperatorPrivilegeEntity);

    PrivilegeEntity clusterUserViewUserPrivilegeEntity = createMock(PrivilegeEntity.class);

    List<PrivilegeEntity> rolePrivilegeEntities = new ArrayList<>();
    rolePrivilegeEntities.add(clusterUserViewUserPrivilegeEntity);

    Capture<? extends List<PrincipalEntity>> principalEntitiesCapture = newCapture();
    Capture<? extends List<PrincipalEntity>> rolePrincipalEntitiesCapture = newCapture();

    PrivilegeDAO privilegeDAO = injector.getInstance(PrivilegeDAO.class);
    expect(privilegeDAO.findAllByPrincipal(capture(principalEntitiesCapture))).andReturn(privilegeEntities).times(1);
    expect(privilegeDAO.findAllByPrincipal(capture(rolePrincipalEntitiesCapture))).andReturn(rolePrivilegeEntities).times(1);


    replayAll();

    Users user = injector.getInstance(Users.class);
    Collection<AmbariGrantedAuthority> authorities = user.getUserAuthorities("user1");

    verifyAll();

    Assert.assertEquals(2, principalEntitiesCapture.getValue().size());
    Assert.assertTrue(principalEntitiesCapture.getValue().contains(userPrincipalEntity));
    Assert.assertTrue(principalEntitiesCapture.getValue().contains(groupPrincipalEntity));

    Assert.assertEquals(2, rolePrincipalEntitiesCapture.getValue().size());
    Assert.assertTrue(rolePrincipalEntitiesCapture.getValue().contains(clusterUserPrivilegePermissionPrincipalEntity));
    Assert.assertTrue(rolePrincipalEntitiesCapture.getValue().contains(clusterOperatorPrivilegePermissionPrincipalEntity));


    Assert.assertEquals(3, authorities.size());
    Assert.assertTrue(authorities.contains(new AmbariGrantedAuthority(clusterUserPrivilegeEntity)));
    Assert.assertTrue(authorities.contains(new AmbariGrantedAuthority(clusterOperatorPrivilegeEntity)));
    Assert.assertTrue(authorities.contains(new AmbariGrantedAuthority(clusterUserViewUserPrivilegeEntity)));
  }

  /**
   * User creation should complete without exception in case of unique user name
   */
  @Test
  public void testCreateUser_NoDuplicates() throws Exception {
    initForCreateUser(null);
    Users users = injector.getInstance(Users.class);
    users.createUser(SERVICEOP_USER_NAME, SERVICEOP_USER_NAME, SERVICEOP_USER_NAME);
  }

  /**
   * User creation should throw {@link AmbariException} in case another user exists with the same name but
   * different user type.
   */
  @Test(expected = AmbariException.class)
  public void testCreateUser_Duplicate() throws Exception {
    UserEntity existing = new UserEntity();
    existing.setUserName(UserName.fromString(SERVICEOP_USER_NAME).toString());
    existing.setUserId(1);
    existing.setMemberEntities(Collections.emptySet());
    PrincipalEntity principal = new PrincipalEntity();
    principal.setPrivileges(Collections.emptySet());
    existing.setPrincipal(principal);
    initForCreateUser(existing);

    Users users = injector.getInstance(Users.class);
    users.createUser(SERVICEOP_USER_NAME, SERVICEOP_USER_NAME, SERVICEOP_USER_NAME);
  }

  private void initForCreateUser(@Nullable UserEntity existingUser) {
    UserDAO userDao = createStrictMock(UserDAO.class);
    expect(userDao.findUserByName(anyString())).andReturn(existingUser);
    userDao.create(anyObject(UserEntity.class));
    expectLastCall();
    EntityManager entityManager = createNiceMock(EntityManager.class);
    expect(entityManager.find(eq(PrincipalEntity.class), EasyMock.anyObject())).andReturn(null);
    replayAll();
    createInjector(userDao, entityManager);
  }

  private void createInjector() {
    createInjector(createMock(UserDAO.class), createMock(EntityManager.class));
  }

  private void createInjector(final UserDAO mockUserDao, final EntityManager mockEntityManager) {
    injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(EntityManager.class).toInstance(mockEntityManager);
        bind(DBAccessor.class).toInstance(createMock(DBAccessor.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(UserDAO.class).toInstance(mockUserDao);
        bind(MemberDAO.class).toInstance(createMock(MemberDAO.class));
        bind(PrivilegeDAO.class).toInstance(createMock(PrivilegeDAO.class));
        bind(PasswordEncoder.class).toInstance(createMock(PasswordEncoder.class));
        bind(HookService.class).toInstance(createMock(HookService.class));
        bind(HookContextFactory.class).toInstance(createMock(HookContextFactory.class));
        bind(PrincipalDAO.class).toInstance(createMock(PrincipalDAO.class));
        bind(Configuration.class).toInstance(createNiceMock(Configuration.class));
        bind(new TypeLiteral<Encryptor<AmbariServerConfiguration>>() {}).annotatedWith(Names.named("AmbariServerConfigurationEncryptor")).toInstance(Encryptor.NONE);
        bind(AmbariLdapConfigurationProvider.class).toInstance(createMock(AmbariLdapConfigurationProvider.class));
      }
    });
  }
}