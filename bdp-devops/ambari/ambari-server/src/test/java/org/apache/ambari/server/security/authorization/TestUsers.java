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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.ambari.server.ldap.service.AmbariLdapConfigurationProvider;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.GroupDAO;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.PrincipalDAO;
import org.apache.ambari.server.orm.dao.PrincipalTypeDAO;
import org.apache.ambari.server.orm.dao.ResourceDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.UserAuthenticationEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.ldap.LdapBatchDto;
import org.apache.ambari.server.security.ldap.LdapGroupDto;
import org.apache.ambari.server.security.ldap.LdapUserDto;
import org.apache.ambari.server.security.ldap.LdapUserGroupMemberDto;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import junit.framework.Assert;

public class TestUsers {
  private Injector injector;

  @Inject
  protected Users users;
  @Inject
  protected UserDAO userDAO;
  @Inject
  protected GroupDAO groupDAO;
  @Inject
  protected PermissionDAO permissionDAO;
  @Inject
  protected ResourceDAO resourceDAO;
  @Inject
  protected ResourceTypeDAO resourceTypeDAO;
  @Inject
  protected PrincipalTypeDAO principalTypeDAO;
  @Inject
  protected PrincipalDAO principalDAO;
  @Inject
  protected PasswordEncoder passwordEncoder;

  @Before
  public void setup() throws AmbariException {
    InMemoryDefaultTestModule module = new InMemoryDefaultTestModule();
    injector = Guice.createInjector(module);
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);

    // create admin permission
    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setId(ResourceType.AMBARI.getId());
    resourceTypeEntity.setName(ResourceType.AMBARI.name());
    resourceTypeDAO.create(resourceTypeEntity);

    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setId(ResourceEntity.AMBARI_RESOURCE_ID);
    resourceEntity.setResourceType(resourceTypeEntity);
    resourceDAO.create(resourceEntity);

    PrincipalTypeEntity principalTypeEntity = new PrincipalTypeEntity();
    principalTypeEntity.setName("ROLE");
    principalTypeEntity = principalTypeDAO.merge(principalTypeEntity);

    PrincipalEntity principalEntity = new PrincipalEntity();
    principalEntity.setPrincipalType(principalTypeEntity);
    principalEntity = principalDAO.merge(principalEntity);

    PermissionEntity adminPermissionEntity = new PermissionEntity();
    adminPermissionEntity.setId(PermissionEntity.AMBARI_ADMINISTRATOR_PERMISSION);
    adminPermissionEntity.setPermissionName(PermissionEntity.AMBARI_ADMINISTRATOR_PERMISSION_NAME);
    adminPermissionEntity.setPrincipal(principalEntity);
    adminPermissionEntity.setResourceType(resourceTypeEntity);
    permissionDAO.create(adminPermissionEntity);
  }

  @After
  public void tearDown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }


  @Test
  public void testIsUserCanBeRemoved() throws Exception {
    UserEntity userEntity;

    userEntity = users.createUser("admin", "admin", "admin");
    users.grantAdminPrivilege(userEntity);

    userEntity = users.createUser("admin222", "admin222", "admin22");
    users.grantAdminPrivilege(userEntity);

    Assert.assertTrue(users.isUserCanBeRemoved(userDAO.findUserByName("admin")));
    Assert.assertTrue(users.isUserCanBeRemoved(userDAO.findUserByName("admin222")));

    users.removeUser(users.getUser("admin222"));

    Assert.assertFalse(users.isUserCanBeRemoved(userDAO.findUserByName("admin")));
    users.createUser("user", "user", "user");
    Assert.assertFalse(users.isUserCanBeRemoved(userDAO.findUserByName("admin")));

    userEntity = users.createUser("admin333", "admin333", "admin333");
    users.grantAdminPrivilege(userEntity);

    Assert.assertTrue(users.isUserCanBeRemoved(userDAO.findUserByName("admin")));
    Assert.assertTrue(users.isUserCanBeRemoved(userDAO.findUserByName("admin333")));
  }

  @Test
  public void testModifyPassword_UserByAdmin() throws Exception {
    UserEntity userEntity;

    userEntity = users.createUser("admin", "admin", "admin");
    users.grantAdminPrivilege(userEntity);
    users.addLocalAuthentication(userEntity, "admin");

    userEntity = users.createUser("user", "user", "user");
    users.addLocalAuthentication(userEntity, "user");

    UserEntity foundUserEntity = userDAO.findUserByName("user");
    assertNotNull(foundUserEntity);

    UserAuthenticationEntity foundLocalAuthenticationEntity;
    foundLocalAuthenticationEntity = getAuthenticationEntity(foundUserEntity, UserAuthenticationType.LOCAL);
    assertNotNull(foundLocalAuthenticationEntity);
    assertNotSame("user", foundLocalAuthenticationEntity.getAuthenticationKey());
    assertTrue(passwordEncoder.matches("user", foundLocalAuthenticationEntity.getAuthenticationKey()));

    foundUserEntity = userDAO.findUserByName("admin");
    assertNotNull(foundUserEntity);
    users.modifyAuthentication(foundLocalAuthenticationEntity, "user", "user_new_password", false);

    foundUserEntity = userDAO.findUserByName("user");
    assertNotNull(foundUserEntity);
    foundLocalAuthenticationEntity = getAuthenticationEntity(foundUserEntity, UserAuthenticationType.LOCAL);
    assertNotNull(foundLocalAuthenticationEntity);
    assertTrue(passwordEncoder.matches("user_new_password", foundLocalAuthenticationEntity.getAuthenticationKey()));
  }

  @Test
  public void testModifyPassword_EmptyPassword() throws Exception {
    UserEntity userEntity;

    userEntity = users.createUser("user", "user", "user");
    users.addLocalAuthentication(userEntity, "user");

    UserEntity foundUserEntity = userDAO.findUserByName("user");
    assertNotNull(foundUserEntity);

    UserAuthenticationEntity foundLocalAuthenticationEntity;
    foundLocalAuthenticationEntity = getAuthenticationEntity(foundUserEntity, UserAuthenticationType.LOCAL);
    assertNotNull(foundLocalAuthenticationEntity);
    assertNotSame("user", foundLocalAuthenticationEntity.getAuthenticationKey());
    assertTrue(passwordEncoder.matches("user", foundLocalAuthenticationEntity.getAuthenticationKey()));

    try {
      users.modifyAuthentication(foundLocalAuthenticationEntity, "user", null, false);
      fail("Null password should not be allowed");
    } catch (AmbariException e) {
      assertEquals("The new password does not meet the Ambari password requirements", e.getLocalizedMessage());
    }

    try {
      users.modifyAuthentication(foundLocalAuthenticationEntity, "user", "", false);
      fail("Empty password should not be allowed");
    } catch (AmbariException e) {
      assertEquals("The new password does not meet the Ambari password requirements", e.getLocalizedMessage());
    }
  }

  @Test
  public void testRevokeAdminPrivilege() throws Exception {
    final UserEntity userEntity = users.createUser("old_admin", "old_admin", "old_admin");
    users.grantAdminPrivilege(userEntity);

    final User admin = users.getUser("old_admin");
    users.revokeAdminPrivilege(admin.getUserId());

    Assert.assertFalse(users.getUser("old_admin").isAdmin());
  }

  @Test
  public void testGrantAdminPrivilege() throws Exception {
    users.createUser("user", "user", "user");

    final User user = users.getUser("user");
    users.grantAdminPrivilege(user.getUserId());

    Assert.assertTrue(users.getUser("user").isAdmin());
  }

  @Test
  public void testCreateGetRemoveUser() throws Exception {
    users.createUser("user1", "user1", null);
    users.createUser("user", "user", null, false);

    UserEntity userEntity = users.createUser("user_ldap", "user_ldap", null);
    users.grantAdminPrivilege(userEntity);
    users.addLdapAuthentication(userEntity, "some dn");

    User createdUser = users.getUser("user");
    User createdUser1 = users.getUser("user1");
    User createdLdapUser = users.getUser("user_ldap");

    Assert.assertEquals("user1", createdUser1.getUserName());
    Assert.assertEquals(true, createdUser1.isActive());
    Assert.assertEquals(false, createdUser1.isLdapUser());
    Assert.assertEquals(false, createdUser1.isAdmin());

    Assert.assertEquals("user", createdUser.getUserName());
    Assert.assertEquals(false, createdUser.isActive());
    Assert.assertEquals(false, createdUser.isLdapUser());
    Assert.assertEquals(false, createdUser.isAdmin());

    Assert.assertEquals("user_ldap", createdLdapUser.getUserName());
    Assert.assertEquals(true, createdLdapUser.isActive());
    Assert.assertEquals(true, createdLdapUser.isLdapUser());
    Assert.assertEquals(true, createdLdapUser.isAdmin());

    assertEquals("user", users.getUser("user").getUserName());
    assertEquals("user_ldap", users.getUser("user_ldap").getUserName());
    Assert.assertNull(users.getUser("non_existing"));

    // create duplicate user
    try {
      users.createUser("user1", "user1", null);
      fail("It shouldn't be possible to create duplicate user");
    } catch (AmbariException e) {
      // This is expected
    }

    try {
      users.createUser("USER1", "user1", null);
      fail("It shouldn't be possible to create duplicate user");
    } catch (AmbariException e) {
      // This is expected
    }

    // test get all users
    List<User> userList = users.getAllUsers();

    Assert.assertEquals(3, userList.size());

    // check get any user case insensitive
    assertEquals("user", users.getUser("USER").getUserName());
    assertEquals("user_ldap", users.getUser("USER_LDAP").getUserName());
    Assert.assertNull(users.getUser("non_existing"));

    // get user by id
    User userById = users.getUser(createdUser.getUserId());

    assertNotNull(userById);
    assertEquals(createdUser.getUserId(), userById.getUserId());

    // get user by invalid id
    User userByInvalidId = users.getUser(-1);

    assertNull(userByInvalidId);

    // get user if unique
    Assert.assertNotNull(users.getUser("user"));

    //remove user
    Assert.assertEquals(3, users.getAllUsers().size());

    users.removeUser(users.getUser("user1"));

    Assert.assertNull(users.getUser("user1"));
    Assert.assertEquals(2, users.getAllUsers().size());
  }

  @Test
  public void testSetUserActive() throws Exception {
    users.createUser("user", "user", null);

    users.setUserActive("user", false);
    Assert.assertEquals(false, users.getUser("user").isActive());
    users.setUserActive("user", true);
    Assert.assertEquals(true, users.getUser("user").isActive());

    try {
      users.setUserActive("fake user", true);
      fail("It shouldn't be possible to call setUserActive() on non-existing user");
    } catch (Exception ex) {
      // This is expected
    }
  }

  @Test
  public void testSetUserLdap() throws Exception {
    UserEntity userEntity;

    users.createUser("user", "user", null);
    users.addLdapAuthentication(users.getUserEntity("user"), "some dn");

    userEntity = users.createUser("user_ldap", "user_ldap", null);
    users.addLdapAuthentication(userEntity, "some dn");

    Assert.assertEquals(true, users.getUser("user").isLdapUser());
    Assert.assertEquals(true, users.getUser("user_ldap").isLdapUser());

    try {
      users.addLdapAuthentication(users.getUserEntity("fake user"), "some other dn");
      fail("It shouldn't be possible to call setUserLdap() on non-existing user");
    } catch (AmbariException ex) {
      // This is expected
    }
  }

  @Test
  public void testSetGroupLdap() throws Exception {
    users.createGroup("group", GroupType.LOCAL);

    users.setGroupLdap("group");
    Assert.assertNotNull(users.getGroup("group"));
    Assert.assertTrue(users.getGroup("group").isLdapGroup());

    try {
      users.setGroupLdap("fake group");
      fail("It shouldn't be possible to call setGroupLdap() on non-existing group");
    } catch (AmbariException ex) {
      // This is expected
    }
  }

  @Test
  public void testCreateGetRemoveGroup() throws Exception {
    final String groupName = "engineering1";
    final String groupName2 = "engineering2";
    users.createGroup(groupName, GroupType.LOCAL);
    users.createGroup(groupName2, GroupType.LOCAL);

    final Group group = users.getGroup(groupName);
    assertNotNull(group);
    assertEquals(false, group.isLdapGroup());
    assertEquals(groupName, group.getGroupName());

    assertNotNull(groupDAO.findGroupByName(groupName));

    // get all groups
    final List<Group> groupList = users.getAllGroups();

    assertEquals(2, groupList.size());
    assertEquals(2, groupDAO.findAll().size());

    // remove group
    users.removeGroup(group);
    assertNull(users.getGroup(group.getGroupName()));
    assertEquals(1, users.getAllGroups().size());
  }

  @Test
  public void testMembers() throws Exception {
    final String groupName = "engineering";
    final String groupName2 = "engineering2";
    users.createGroup(groupName, GroupType.LOCAL);
    users.createGroup(groupName2, GroupType.LOCAL);
    users.createUser("user1", "user1", null);
    users.createUser("user2", "user2", null);
    users.createUser("user3", "user3", null);
    users.addMemberToGroup(groupName, "user1");
    users.addMemberToGroup(groupName, "user2");
    assertEquals(2, users.getAllMembers(groupName).size());
    assertEquals(0, users.getAllMembers(groupName2).size());

    try {
      users.getAllMembers("non existing");
      fail("It shouldn't be possible to call getAllMembers() on non-existing group");
    } catch (Exception ex) {
      // This is expected
    }

    // get members from not unexisting group
    assertEquals(users.getGroupMembers("unexisting"), null);

    // remove member from group
    users.removeMemberFromGroup(groupName, "user1");
    assertEquals(1, groupDAO.findGroupByName(groupName).getMemberEntities().size());
    assertEquals("user2", groupDAO.findGroupByName(groupName).getMemberEntities().iterator().next().getUser().getUserName());
  }

  @Test
  public void testModifyPassword_UserByHimselfPasswordOk() throws Exception {
    UserEntity userEntity = users.createUser("user", "user", null);
    users.addLocalAuthentication(userEntity, "user");

    userEntity = userDAO.findUserByName("user");
    UserAuthenticationEntity localAuthenticationEntity = getAuthenticationEntity(userEntity, UserAuthenticationType.LOCAL);
    assertNotNull(localAuthenticationEntity);

    assertNotSame("user", localAuthenticationEntity.getAuthenticationKey());
    assertTrue(passwordEncoder.matches("user", localAuthenticationEntity.getAuthenticationKey()));

    users.modifyAuthentication(localAuthenticationEntity, "user", "user_new_password", true);
    userEntity = userDAO.findUserByName("user");
    localAuthenticationEntity = getAuthenticationEntity(userEntity, UserAuthenticationType.LOCAL);
    assertNotNull(localAuthenticationEntity);

    assertTrue(passwordEncoder.matches("user_new_password", localAuthenticationEntity.getAuthenticationKey()));
  }

  @Test
  public void testModifyPassword_UserByHimselfPasswordNotOk() throws Exception {
    UserEntity userEntity = users.createUser("user", "user", null);
    users.addLocalAuthentication(userEntity, "user");

    userEntity = userDAO.findUserByName("user");
    UserAuthenticationEntity foundLocalAuthenticationEntity;
    foundLocalAuthenticationEntity = getAuthenticationEntity(userEntity, UserAuthenticationType.LOCAL);
    assertNotNull(foundLocalAuthenticationEntity);
    assertNotSame("user", foundLocalAuthenticationEntity.getAuthenticationKey());
    assertTrue(passwordEncoder.matches("user", foundLocalAuthenticationEntity.getAuthenticationKey()));

    try {
      users.modifyAuthentication(foundLocalAuthenticationEntity, "admin", "user_new_password", true);
      fail("Exception should be thrown here as password is incorrect");
    } catch (AmbariException ex) {
      // This is expected
    }
  }

  @Test
  public void testAddAndRemoveAuthentication() throws Exception {
    users.createUser("user", "user", "user");

    UserEntity userEntity = userDAO.findUserByName("user");
    assertNotNull(userEntity);
    assertEquals("user", userEntity.getUserName());

    UserEntity userEntity2 = userDAO.findUserByName("user");
    assertNotNull(userEntity2);
    assertEquals("user", userEntity2.getUserName());

    assertEquals(0, users.getUserAuthenticationEntities("user", null).size());

    users.addAuthentication(userEntity, UserAuthenticationType.LOCAL, "local_key");
    assertEquals(1, users.getUserAuthenticationEntities("user", null).size());
    assertEquals(1, users.getUserAuthenticationEntities("user", UserAuthenticationType.LOCAL).size());
    assertTrue(passwordEncoder.matches("local_key", users.getUserAuthenticationEntities("user", UserAuthenticationType.LOCAL).iterator().next().getAuthenticationKey()));
    assertEquals(0, users.getUserAuthenticationEntities("user", UserAuthenticationType.KERBEROS).size());

    users.addAuthentication(userEntity, UserAuthenticationType.PAM, "pam_key");
    assertEquals(2, users.getUserAuthenticationEntities("user", null).size());
    assertEquals(1, users.getUserAuthenticationEntities("user", UserAuthenticationType.PAM).size());
    assertEquals("pam_key", users.getUserAuthenticationEntities("user", UserAuthenticationType.PAM).iterator().next().getAuthenticationKey());
    assertEquals(0, users.getUserAuthenticationEntities("user", UserAuthenticationType.KERBEROS).size());

    users.addAuthentication(userEntity, UserAuthenticationType.JWT, "jwt_key");
    assertEquals(3, users.getUserAuthenticationEntities("user", null).size());
    assertEquals(1, users.getUserAuthenticationEntities("user", UserAuthenticationType.JWT).size());
    assertEquals("jwt_key", users.getUserAuthenticationEntities("user", UserAuthenticationType.JWT).iterator().next().getAuthenticationKey());
    assertEquals(0, users.getUserAuthenticationEntities("user", UserAuthenticationType.KERBEROS).size());

    users.addAuthentication(userEntity, UserAuthenticationType.LDAP, "ldap_key");
    assertEquals(4, users.getUserAuthenticationEntities("user", null).size());
    assertEquals(1, users.getUserAuthenticationEntities("user", UserAuthenticationType.LDAP).size());
    assertEquals("ldap_key", users.getUserAuthenticationEntities("user", UserAuthenticationType.LDAP).iterator().next().getAuthenticationKey());
    assertEquals(0, users.getUserAuthenticationEntities("user", UserAuthenticationType.KERBEROS).size());

    users.addAuthentication(userEntity, UserAuthenticationType.KERBEROS, "kerberos_key");
    assertEquals(5, users.getUserAuthenticationEntities("user", null).size());
    assertEquals("kerberos_key", users.getUserAuthenticationEntities("user", UserAuthenticationType.KERBEROS).iterator().next().getAuthenticationKey());
    assertEquals(1, users.getUserAuthenticationEntities("user", UserAuthenticationType.KERBEROS).size());

    // UserEntity was updated by user.addAuthentication
    assertEquals(5, userEntity.getAuthenticationEntities().size());

    // UserEntity2 needs to be refreshed...
    assertEquals(0, userEntity2.getAuthenticationEntities().size());
    userEntity2 = userDAO.findUserByName("user");
    assertEquals(5, userEntity2.getAuthenticationEntities().size());


    // Test Remove
    Long kerberosAuthenticationId = users.getUserAuthenticationEntities("user", UserAuthenticationType.KERBEROS).iterator().next().getUserAuthenticationId();
    Long pamAuthenticationId = users.getUserAuthenticationEntities("user", UserAuthenticationType.PAM).iterator().next().getUserAuthenticationId();

    users.removeAuthentication("user", kerberosAuthenticationId);
    assertEquals(4, users.getUserAuthenticationEntities("user", null).size());

    users.removeAuthentication(userEntity, kerberosAuthenticationId);
    assertEquals(4, users.getUserAuthenticationEntities("user", null).size());

    users.removeAuthentication(userEntity, pamAuthenticationId);
    assertEquals(3, users.getUserAuthenticationEntities("user", null).size());

    assertEquals(3, userEntity2.getAuthenticationEntities().size());
  }

  @Test
  public void testProcessLdapSync() {
    AmbariLdapConfiguration ambariLdapConfiguration = EasyMock.createMock(AmbariLdapConfiguration.class);
    EasyMock.expect(ambariLdapConfiguration.groupMappingRules()).andReturn("admins").anyTimes();

    AmbariLdapConfigurationProvider ambariLdapConfigurationProvider = injector.getInstance(AmbariLdapConfigurationProvider.class);
    EasyMock.expect(ambariLdapConfigurationProvider.get()).andReturn(ambariLdapConfiguration).anyTimes();

    EasyMock.replay(ambariLdapConfigurationProvider, ambariLdapConfiguration);

    LdapBatchDto batchInfo = new LdapBatchDto();
    LdapUserDto userToBeCreated;
    LdapGroupDto groupToBeCreated;

    userToBeCreated = new LdapUserDto();
    userToBeCreated.setDn("dn=user1");
    userToBeCreated.setUid("user1");
    userToBeCreated.setUserName("User1");
    batchInfo.getUsersToBeCreated().add(userToBeCreated);

    userToBeCreated = new LdapUserDto();
    userToBeCreated.setDn("dn=user2");
    userToBeCreated.setUid("user2");
    userToBeCreated.setUserName("User2");
    batchInfo.getUsersToBeCreated().add(userToBeCreated);

    groupToBeCreated = new LdapGroupDto();
    groupToBeCreated.setGroupName("admins");
    groupToBeCreated.setMemberAttributes(Collections.singleton("dn=User1"));
    batchInfo.getGroupsToBeCreated().add(groupToBeCreated);

    groupToBeCreated = new LdapGroupDto();
    groupToBeCreated.setGroupName("non-admins");
    groupToBeCreated.setMemberAttributes(Collections.singleton("dn=User2"));
    batchInfo.getGroupsToBeCreated().add(groupToBeCreated);

    batchInfo.getMembershipToAdd().add(new LdapUserGroupMemberDto("admins", "user1"));
    batchInfo.getMembershipToAdd().add(new LdapUserGroupMemberDto("non-admins", "user2"));

    users.processLdapSync(batchInfo);

    assertNotNull(users.getUser("user1"));
    assertNotNull(users.getUser("user2"));

    Collection<AmbariGrantedAuthority> authorities;

    authorities = users.getUserAuthorities("user1");
    assertNotNull(authorities);
    assertEquals(1, authorities.size());
    assertEquals("AMBARI.ADMINISTRATOR", authorities.iterator().next().getPrivilegeEntity().getPermission().getPermissionName());

    authorities = users.getUserAuthorities("user2");
    assertNotNull(authorities);
    assertEquals(0, authorities.size());
  }

  private UserAuthenticationEntity getAuthenticationEntity(UserEntity userEntity, UserAuthenticationType type) {
    assertNotNull(userEntity);
    Collection<UserAuthenticationEntity> authenticationEntities = userEntity.getAuthenticationEntities();
    assertNotNull(authenticationEntities);
    for (UserAuthenticationEntity authenticationEntity : authenticationEntities) {
      if (authenticationEntity.getAuthenticationType() == type) {
        return authenticationEntity;
      }
    }

    return null;
  }
}
