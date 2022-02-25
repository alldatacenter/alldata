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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.OptimisticLockException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.hooks.HookContextFactory;
import org.apache.ambari.server.hooks.HookService;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.ambari.server.ldap.service.AmbariLdapConfigurationProvider;
import org.apache.ambari.server.orm.dao.GroupDAO;
import org.apache.ambari.server.orm.dao.MemberDAO;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.PrincipalDAO;
import org.apache.ambari.server.orm.dao.PrincipalTypeDAO;
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.ResourceDAO;
import org.apache.ambari.server.orm.dao.UserAuthenticationDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.GroupEntity;
import org.apache.ambari.server.orm.entities.MemberEntity;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.UserAuthenticationEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.authentication.AccountDisabledException;
import org.apache.ambari.server.security.authentication.TooManyLoginFailuresException;
import org.apache.ambari.server.security.authentication.UserNotFoundException;
import org.apache.ambari.server.security.ldap.LdapBatchDto;
import org.apache.ambari.server.security.ldap.LdapGroupDto;
import org.apache.ambari.server.security.ldap.LdapUserDto;
import org.apache.ambari.server.security.ldap.LdapUserGroupMemberDto;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * Provides high-level access to Users and Roles in database
 */
@Singleton
public class Users {

  private static final Logger LOG = LoggerFactory.getLogger(Users.class);

  /**
   * The maximum number of retries when handling OptimisticLockExceptions
   */
  private static final int MAX_RETRIES = 10;

  @Inject
  private Provider<EntityManager> entityManagerProvider;

  @Inject
  private UserDAO userDAO;

  @Inject
  private UserAuthenticationDAO userAuthenticationDAO;

  @Inject
  private GroupDAO groupDAO;

  @Inject
  private MemberDAO memberDAO;

  @Inject
  private PrincipalDAO principalDAO;

  @Inject
  private PermissionDAO permissionDAO;

  @Inject
  private PrivilegeDAO privilegeDAO;

  @Inject
  private ResourceDAO resourceDAO;

  @Inject
  private PrincipalTypeDAO principalTypeDAO;

  @Inject
  private PasswordEncoder passwordEncoder;

  @Inject
  protected AmbariLdapConfigurationProvider ldapConfigurationProvider;

  @Inject
  protected Configuration configuration;

  @Inject
  private Provider<HookService> hookServiceProvider;

  @Inject
  private HookContextFactory hookContextFactory;

  public List<User> getAllUsers() {
    List<UserEntity> userEntities = userDAO.findAll();
    List<User> users = new ArrayList<>(userEntities.size());

    for (UserEntity userEntity : userEntities) {
      users.add(new User(userEntity));
    }

    return users;
  }

  public List<UserEntity> getAllUserEntities() {
    return userDAO.findAll();
  }

  public UserEntity getUserEntity(String userName) {
    return (userName == null) ? null : userDAO.findUserByName(userName);
  }

  public UserEntity getUserEntity(Integer userId) {
    return (userId == null) ? null : userDAO.findByPK(userId);
  }

  public User getUser(UserEntity userEntity) {
    return (null == userEntity) ? null : new User(userEntity);
  }

  public User getUser(Integer userId) {
    return getUser(getUserEntity(userId));
  }

  public User getUser(String userName) {
    return getUser(getUserEntity(userName));
  }

  /**
   * Enables/disables user.
   *
   * @param userName user name
   * @param active   true if active; false if not active
   * @throws AmbariException if user does not exist
   */
  public synchronized void setUserActive(String userName, boolean active) throws AmbariException {
    UserEntity userEntity = userDAO.findUserByName(userName);
    if (userEntity != null) {
      setUserActive(userEntity, active);
    } else {
      throw new AmbariException("User " + userName + " doesn't exist");
    }
  }

  /**
   * Enables/disables user.
   *
   * @param userEntity the user
   * @param active     true if active; false if not active
   * @throws AmbariException if user does not exist
   */
  public synchronized void setUserActive(UserEntity userEntity, final boolean active) throws AmbariException {
    if (userEntity != null) {
      Command command = new Command() {
        @Override
        public void perform(UserEntity userEntity) {
          userEntity.setActive(active);
        }
      };

      safelyUpdateUserEntity(userEntity, command, MAX_RETRIES);
    }
  }

  /**
   * Validates the user account such that the user is allowed to log in.
   *
   * @param userEntity the user entity
   * @param userName   the Ambari username
   */
  public void validateLogin(UserEntity userEntity, String userName) {
    if (userEntity == null) {
      LOG.info("User not found");
      throw new UserNotFoundException(userName);
    } else {
      if (!userEntity.getActive()) {
        LOG.info("User account is disabled: {}", userName);
        throw new AccountDisabledException(userName);
      }

      int maxConsecutiveFailures = configuration.getMaxAuthenticationFailures();
      if (maxConsecutiveFailures > 0 && userEntity.getConsecutiveFailures() >= maxConsecutiveFailures) {
        LOG.info("User account is locked out due to too many authentication failures ({}/{}): {}",
            userEntity.getConsecutiveFailures(), maxConsecutiveFailures, userName);
        throw new TooManyLoginFailuresException(userName);
      }
    }
  }

  /**
   * Converts group to LDAP group.
   *
   * @param groupName group name
   * @throws AmbariException if group does not exist
   */
  public synchronized void setGroupLdap(String groupName) throws AmbariException {
    GroupEntity groupEntity = groupDAO.findGroupByName(groupName);
    if (groupEntity != null) {
      groupEntity.setGroupType(GroupType.LDAP);
      groupDAO.merge(groupEntity);
    } else {
      throw new AmbariException("Group " + groupName + " doesn't exist");
    }
  }

  /**
   * Creates new, active, user with provided userName, local username, and display name.
   *
   * @param userName      user name
   * @param localUserName the local username to use; if <code>null</code> or empty, userName will be used
   * @param displayName   the name to display for presentation; if <code>null</code> or empty, userName will be used
   * @return the new UserEntity
   * @throws AmbariException if user already exists
   */
  public UserEntity createUser(String userName, String localUserName, String displayName) throws AmbariException {
    return createUser(userName, localUserName, displayName, true);
  }

  /**
   * Creates new, user with provided userName, local username, and display name.
   *
   * @param userName      user name
   * @param localUserName the local username to use; if <code>null</code> or empty, userName will be used
   * @param displayName   the name to display for presentation; if <code>null</code> or empty, userName will be used
   * @param active        is user active
   * @return the new UserEntity
   * @throws AmbariException if user already exists
   */
  @Transactional
  public synchronized UserEntity createUser(String userName, String localUserName, String displayName, Boolean active) throws AmbariException {

    String validatedUserName = UserName.fromString(userName).toString();
    String validatedDisplayName = (StringUtils.isEmpty(displayName))
        ? validatedUserName
        : UserName.fromString(displayName).toString();
    String validatedLocalUserName = (StringUtils.isEmpty(localUserName))
        ? validatedUserName
        : UserName.fromString(localUserName).toString();

    // Ensure that the user does not already exist
    if (userDAO.findUserByName(validatedUserName) != null) {
      throw new AmbariException("User already exists");
    }

    // Create the PrincipalEntity - needed for assigning privileges/roles
    PrincipalTypeEntity principalTypeEntity = principalTypeDAO.findById(PrincipalTypeEntity.USER_PRINCIPAL_TYPE);
    if (principalTypeEntity == null) {
      principalTypeEntity = new PrincipalTypeEntity();
      principalTypeEntity.setId(PrincipalTypeEntity.USER_PRINCIPAL_TYPE);
      principalTypeEntity.setName(PrincipalTypeEntity.USER_PRINCIPAL_TYPE_NAME);
      principalTypeDAO.create(principalTypeEntity);
    }
    PrincipalEntity principalEntity = new PrincipalEntity();
    principalEntity.setPrincipalType(principalTypeEntity);
    principalDAO.create(principalEntity);

    // Create the new UserEntity Record
    UserEntity userEntity = new UserEntity();
    userEntity.setUserName(validatedUserName);
    userEntity.setDisplayName(validatedDisplayName);
    userEntity.setLocalUsername(validatedLocalUserName);

    userEntity.setPrincipal(principalEntity);
    if (active != null) {
      userEntity.setActive(active);
    }

    userDAO.create(userEntity);

    // execute user initialization hook if required ()
    executeUserHook(validatedUserName);

    return userEntity;
  }

  /**
   * Triggers the post user creation hook, if enabled
   *
   * @param username the username of the user to process
   */
  public void executeUserHook(String username) {
    hookServiceProvider.get().execute(hookContextFactory.createUserHookContext(username));
  }

  /**
   * Triggers the post user creation hook, if enabled
   *
   * @param userGroupsMap a map of user names to relevant groups
   */
  public void executeUserHook(Map<String, Set<String>> userGroupsMap) {
    hookServiceProvider.get().execute(hookContextFactory.createBatchUserHookContext(userGroupsMap));
  }

  /**
   * Removes a user from the Ambari database.
   * <p>
   * It is expected that the associated user authentication records are removed by this operation
   * as well.
   *
   * @param user the user to remove
   * @throws AmbariException
   */
  @Transactional
  public synchronized void removeUser(User user) throws AmbariException {
    UserEntity userEntity = userDAO.findByPK(user.getUserId());
    if (userEntity != null) {
      removeUser(userEntity);
    } else {
      throw new AmbariException("User " + user + " doesn't exist");
    }
  }

  /**
   * Removes a user from the Ambari database.
   * <p>
   * It is expected that the associated user authentication records are removed by this operation
   * as well.
   *
   * @param userEntity the user to remove
   * @throws AmbariException
   */
  @Transactional
  public synchronized void removeUser(UserEntity userEntity) throws AmbariException {
    if (userEntity != null) {
      if (!isUserCanBeRemoved(userEntity)) {
        throw new AmbariException("Could not remove user " + userEntity.getUserName() +
          ". System should have at least one administrator.");
      }
      userDAO.remove(userEntity);
    }
  }

  /**
   * Gets group by given name.
   *
   * @param groupName group name
   * @return group
   */
  public Group getGroup(String groupName) {
    final GroupEntity groupEntity = groupDAO.findGroupByName(groupName);
    return (null == groupEntity) ? null : new Group(groupEntity);
  }

  /**
   * Gets group by given name & type.
   *
   * @param groupName group name
   * @param groupType group type
   * @return group
   */
  public Group getGroup(String groupName, GroupType groupType) {
    final GroupEntity groupEntity = getGroupEntity(groupName, groupType);
    return (null == groupEntity) ? null : new Group(groupEntity);
  }

  /**
   * Gets a {@link GroupEntity} by name and type.
   *
   * @param groupName group name
   * @param groupType group type
   * @return group
   */
  public GroupEntity getGroupEntity(String groupName, GroupType groupType) {
    return groupDAO.findGroupByNameAndType(groupName, groupType);
  }

  /**
   * Gets group members.
   *
   * @param groupName group name
   * @return list of members
   */
  public Collection<User> getGroupMembers(String groupName) {
    final GroupEntity groupEntity = groupDAO.findGroupByName(groupName);
    if (groupEntity == null) {
      return null;
    } else {
      final Set<User> users = new HashSet<>();
      for (MemberEntity memberEntity : groupEntity.getMemberEntities()) {
        if (memberEntity.getUser() != null) {
          users.add(new User(memberEntity.getUser()));
        } else {
          LOG.error("Wrong state, not found user for member '{}' (group: '{}')",
            memberEntity.getMemberId(), memberEntity.getGroup().getGroupName());
        }
      }
      return users;
    }
  }

  /**
   * Creates new group with provided name & type
   */
  @Transactional
  public synchronized GroupEntity createGroup(String groupName, GroupType groupType) {
    // create an admin principal to represent this group
    PrincipalTypeEntity principalTypeEntity = principalTypeDAO.findById(PrincipalTypeEntity.GROUP_PRINCIPAL_TYPE);
    if (principalTypeEntity == null) {
      principalTypeEntity = new PrincipalTypeEntity();
      principalTypeEntity.setId(PrincipalTypeEntity.GROUP_PRINCIPAL_TYPE);
      principalTypeEntity.setName(PrincipalTypeEntity.GROUP_PRINCIPAL_TYPE_NAME);
      principalTypeDAO.create(principalTypeEntity);
    }
    PrincipalEntity principalEntity = new PrincipalEntity();
    principalEntity.setPrincipalType(principalTypeEntity);
    principalDAO.create(principalEntity);

    final GroupEntity groupEntity = new GroupEntity();
    groupEntity.setGroupName(groupName);
    groupEntity.setPrincipal(principalEntity);
    groupEntity.setGroupType(groupType);

    groupDAO.create(groupEntity);
    return groupEntity;
  }

  /**
   * Gets all groups.
   *
   * @return list of groups
   */
  public List<Group> getAllGroups() {
    final List<GroupEntity> groupEntities = groupDAO.findAll();
    final List<Group> groups = new ArrayList<>(groupEntities.size());

    for (GroupEntity groupEntity : groupEntities) {
      groups.add(new Group(groupEntity));
    }

    return groups;
  }

  /**
   * Gets all members of a group specified.
   *
   * @param groupName group name
   * @return list of user names
   */
  public List<String> getAllMembers(String groupName) throws AmbariException {
    final List<String> members = new ArrayList<>();
    final GroupEntity groupEntity = groupDAO.findGroupByName(groupName);
    if (groupEntity == null) {
      throw new AmbariException("Group " + groupName + " doesn't exist");
    }
    for (MemberEntity member : groupEntity.getMemberEntities()) {
      members.add(member.getUser().getUserName());
    }
    return members;
  }

  @Transactional
  public synchronized void removeGroup(Group group) throws AmbariException {
    final GroupEntity groupEntity = groupDAO.findByPK(group.getGroupId());
    if (groupEntity != null) {
      groupDAO.remove(groupEntity);
    } else {
      throw new AmbariException("Group " + group + " doesn't exist");
    }
  }

  /**
   * Test the user for Ambari Admistrator privileges.
   *
   * @param userEntity the user to test
   * @return true if the user has Ambari Administrator privileges; otherwise false
   */
  public synchronized boolean hasAdminPrivilege(UserEntity userEntity) {
    PrincipalEntity principalEntity = userEntity.getPrincipal();
    if (principalEntity != null) {
      Set<PrivilegeEntity> roles = principalEntity.getPrivileges();
      if (roles != null) {
        PermissionEntity adminPermission = permissionDAO.findAmbariAdminPermission();
        Integer adminPermissionId = (adminPermission == null) ? null : adminPermission.getId();

        if (adminPermissionId != null) {
          for (PrivilegeEntity privilegeEntity : roles) {
            PermissionEntity rolePermission = privilegeEntity.getPermission();
            if ((rolePermission != null) && (adminPermissionId.equals(rolePermission.getId()))) {
              return true;
            }
          }
        }
      }
    }

    return false;
  }

  /**
   * Grants Ambari Administrator privilege to provided user.
   *
   * @param userId user id
   */
  public synchronized void grantAdminPrivilege(Integer userId) {
    grantAdminPrivilege(userDAO.findByPK(userId));
  }

  /**
   * Grants Ambari Administrator privilege to provided user.
   *
   * @param userEntity the user
   */
  public synchronized void grantAdminPrivilege(UserEntity userEntity) {
    final PrivilegeEntity adminPrivilege = new PrivilegeEntity();
    adminPrivilege.setPermission(permissionDAO.findAmbariAdminPermission());
    adminPrivilege.setPrincipal(userEntity.getPrincipal());
    adminPrivilege.setResource(resourceDAO.findAmbariResource());
    if (!userEntity.getPrincipal().getPrivileges().contains(adminPrivilege)) {
      privilegeDAO.create(adminPrivilege);
      userEntity.getPrincipal().getPrivileges().add(adminPrivilege);
      principalDAO.merge(userEntity.getPrincipal()); //explicit merge for Derby support
      userDAO.merge(userEntity);
    }
  }

  /**
   * Grants privilege to provided group.
   *
   * @param groupId        group id
   * @param resourceId     resource id
   * @param resourceType   resource type
   * @param permissionName permission name
   */
  public synchronized void grantPrivilegeToGroup(Integer groupId, Long resourceId, ResourceType resourceType, String permissionName) {
    final GroupEntity group = groupDAO.findByPK(groupId);
    final PrivilegeEntity privilege = new PrivilegeEntity();
    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setId(resourceType.getId());
    resourceTypeEntity.setName(resourceType.name());
    privilege.setPermission(permissionDAO.findPermissionByNameAndType(permissionName, resourceTypeEntity));
    privilege.setPrincipal(group.getPrincipal());
    privilege.setResource(resourceDAO.findById(resourceId));
    if (!group.getPrincipal().getPrivileges().contains(privilege)) {
      privilegeDAO.create(privilege);
      group.getPrincipal().getPrivileges().add(privilege);
      principalDAO.merge(group.getPrincipal()); //explicit merge for Derby support
      groupDAO.merge(group);
      privilegeDAO.merge(privilege);
    }
  }

  /**
   * Revokes Ambari Administrator privileges from provided user.
   *
   * @param userId user id
   */
  public synchronized void revokeAdminPrivilege(Integer userId) {
    revokeAdminPrivilege(userDAO.findByPK(userId));
  }

  /**
   * Revokes Ambari Administrator privileges from provided user.
   *
   * @param userEntity the user
   */
  public synchronized void revokeAdminPrivilege(UserEntity userEntity) {
    for (PrivilegeEntity privilege : userEntity.getPrincipal().getPrivileges()) {
      if (privilege.getPermission().getPermissionName().equals(PermissionEntity.AMBARI_ADMINISTRATOR_PERMISSION_NAME)) {
        userEntity.getPrincipal().getPrivileges().remove(privilege);
        principalDAO.merge(userEntity.getPrincipal()); //explicit merge for Derby support
        userDAO.merge(userEntity);
        privilegeDAO.remove(privilege);
        break;
      }
    }
  }

  @Transactional
  public synchronized void addMemberToGroup(String groupName, String userName)
    throws AmbariException {

    final GroupEntity groupEntity = groupDAO.findGroupByName(groupName);
    if (groupEntity == null) {
      throw new AmbariException("Group " + groupName + " doesn't exist");
    }

    UserEntity userEntity = userDAO.findUserByName(userName);
    if (userEntity == null) {
      throw new AmbariException("User " + userName + " doesn't exist");
    }

    addMemberToGroup(groupEntity, userEntity);
  }

  @Transactional
  public synchronized void addMemberToGroup(GroupEntity groupEntity, UserEntity userEntity)
      throws AmbariException {

    if (groupEntity == null) {
      throw new NullPointerException();
    }

    if (userEntity == null) {
      throw new NullPointerException();
    }

    if (!isUserInGroup(userEntity, groupEntity)) {
      final MemberEntity memberEntity = new MemberEntity();
      memberEntity.setGroup(groupEntity);
      memberEntity.setUser(userEntity);
      userEntity.getMemberEntities().add(memberEntity);
      groupEntity.getMemberEntities().add(memberEntity);
      memberDAO.create(memberEntity);
      userDAO.merge(userEntity);
      groupDAO.merge(groupEntity);
    }
  }

  @Transactional
  public synchronized void removeMemberFromGroup(String groupName, String userName)
    throws AmbariException {

    final GroupEntity groupEntity = groupDAO.findGroupByName(groupName);
    if (groupEntity == null) {
      throw new AmbariException("Group " + groupName + " doesn't exist");
    }

    UserEntity userEntity = userDAO.findUserByName(userName);
    if (userEntity == null) {
      throw new AmbariException("User " + userName + " doesn't exist");
    }

    removeMemberFromGroup(groupEntity, userEntity);
  }

  @Transactional
  public synchronized void removeMemberFromGroup(GroupEntity groupEntity, UserEntity userEntity)
      throws AmbariException {

    if (isUserInGroup(userEntity, groupEntity)) {
      MemberEntity memberEntity = null;
      for (MemberEntity entity : userEntity.getMemberEntities()) {
        if (entity.getGroup().equals(groupEntity)) {
          memberEntity = entity;
          break;
        }
      }
      userEntity.getMemberEntities().remove(memberEntity);
      groupEntity.getMemberEntities().remove(memberEntity);
      userDAO.merge(userEntity);
      groupDAO.merge(groupEntity);
      memberDAO.remove(memberEntity);
    }
  }

  /**
   * Performs a check if the user can be removed. Do not allow removing all admins from database.
   *
   * @param userEntity user to be checked
   * @return true if user can be removed
   */
  public synchronized boolean isUserCanBeRemoved(UserEntity userEntity) {
    List<PrincipalEntity> adminPrincipals = principalDAO.findByPermissionId(PermissionEntity.AMBARI_ADMINISTRATOR_PERMISSION);
    Set<UserEntity> userEntitysSet = new HashSet<>(userDAO.findUsersByPrincipal(adminPrincipals));
    return (userEntitysSet.contains(userEntity) && userEntitysSet.size() < 2) ? false : true;
  }

  /**
   * Performs a check if given user belongs to given group.
   *
   * @param userEntity  user entity
   * @param groupEntity group entity
   * @return true if user presents in group
   */
  public boolean isUserInGroup(UserEntity userEntity, GroupEntity groupEntity) {
    for (MemberEntity memberEntity : userEntity.getMemberEntities()) {
      if (memberEntity.getGroup().equals(groupEntity)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Executes batch queries to database to insert large amounts of LDAP data.
   *
   * @param batchInfo DTO with batch information
   */
  public void processLdapSync(LdapBatchDto batchInfo) {
    final Map<String, UserEntity> allUsers = new HashMap<>();
    final Map<String, GroupEntity> allGroups = new HashMap<>();

    // prefetch all user and group data to avoid heavy queries in membership creation

    for (UserEntity userEntity : userDAO.findAll()) {
      allUsers.put(userEntity.getUserName(), userEntity);
    }

    for (GroupEntity groupEntity : groupDAO.findAll()) {
      allGroups.put(groupEntity.getGroupName(), groupEntity);
    }

    final PrincipalTypeEntity groupPrincipalType = principalTypeDAO
        .ensurePrincipalTypeCreated(PrincipalTypeEntity.GROUP_PRINCIPAL_TYPE);

    /* *****
     * Remove users
     *   First remove the relevant LDAP entries for this user.
     *   If no more user authentication items exists for the user, then remove the user.
     * ***** */
    final Set<UserEntity> usersToRemove = new HashSet<>();
    final Set<UserAuthenticationEntity> authenticationEntitiesToRemove = new HashSet<>();
    for (LdapUserDto user : batchInfo.getUsersToBeRemoved()) {
      UserEntity userEntity = userDAO.findUserByName(user.getUserName());
      if (userEntity != null) {
        List<UserAuthenticationEntity> authenticationEntities = userAuthenticationDAO.findByUser(userEntity);
        Iterator<UserAuthenticationEntity> iterator = authenticationEntities.iterator();
        while (iterator.hasNext()) {
          UserAuthenticationEntity authenticationEntity = iterator.next();

          if (authenticationEntity.getAuthenticationType() == UserAuthenticationType.LDAP) {
            String dn = user.getDn();
            String authenticationKey = authenticationEntity.getAuthenticationKey();

            // DN's are case-insensitive.
            if (StringUtils.isEmpty(dn) || StringUtils.isEmpty(authenticationKey) || dn.equalsIgnoreCase(authenticationKey)) {
              authenticationEntitiesToRemove.add(authenticationEntity);
            }
            iterator.remove();
          }
        }

        if (authenticationEntities.isEmpty()) {
          allUsers.remove(userEntity.getUserName());
          usersToRemove.add(userEntity);
        }
      }
    }
    userAuthenticationDAO.remove(authenticationEntitiesToRemove);
    userDAO.remove(usersToRemove);

    // remove groups
    final Set<GroupEntity> groupsToRemove = new HashSet<>();
    for (LdapGroupDto group : batchInfo.getGroupsToBeRemoved()) {
      final GroupEntity groupEntity = groupDAO.findGroupByName(group.getGroupName());
      allGroups.remove(groupEntity.getGroupName());
      groupsToRemove.add(groupEntity);
    }
    groupDAO.remove(groupsToRemove);

    /* *****
     * Update users
     * ***** */
    final Set<UserEntity> userEntitiesToUpdate = new HashSet<>();
    for (LdapUserDto user : batchInfo.getUsersToBecomeLdap()) {
      // Ensure the username is all lowercase
      String userName = user.getUserName();

      UserEntity userEntity = userDAO.findUserByName(userName);
      if (userEntity != null) {
        LOG.trace("Enabling LDAP authentication for the user account with the username {}.", userName);

        if (configuration.getLdapSyncCollisionHandlingBehavior() == Configuration.LdapUsernameCollisionHandlingBehavior.CONVERT) {
          // If converting the user to only an LDAP user, then remove all other authentication methods
          Collection<UserAuthenticationEntity> existingEntities = userEntity.getAuthenticationEntities();
          if(existingEntities != null) {
            Iterator<UserAuthenticationEntity> iterator = existingEntities.iterator();
            while(iterator.hasNext()) {
              UserAuthenticationEntity userAuthenticationEntity = iterator.next();
              if(userAuthenticationEntity.getAuthenticationType() != UserAuthenticationType.LDAP) {
                removeAuthentication(userEntity, userAuthenticationEntity.getUserAuthenticationId());
                iterator.remove();
              }
            }
          }
        }

        try {
          addLdapAuthentication(userEntity, user.getDn(), false);
          userEntitiesToUpdate.add(userEntity);
        } catch (AmbariException e) {
          LOG.warn(String.format("Failed to enable LDAP authentication for the user account with the username %s: %s", userName, e.getLocalizedMessage()), e);
        }
      } else {
        LOG.warn("Failed to find user account for {} while enabling LDAP authentication for the user.", userName);
      }
    }
    userDAO.merge(userEntitiesToUpdate);

    // update groups
    final Set<GroupEntity> groupsToBecomeLdap = new HashSet<>();
    for (LdapGroupDto group : batchInfo.getGroupsToBecomeLdap()) {
      final GroupEntity groupEntity = groupDAO.findGroupByName(group.getGroupName());
      groupEntity.setGroupType(GroupType.LDAP);
      allGroups.put(groupEntity.getGroupName(), groupEntity);
      groupsToBecomeLdap.add(groupEntity);
    }
    groupDAO.merge(groupsToBecomeLdap);

    // prepare create principals
    final List<PrincipalEntity> principalsToCreate = new ArrayList<>();

    // Create users
    for (LdapUserDto user : batchInfo.getUsersToBeCreated()) {
      String userName = user.getUserName();
      UserEntity userEntity;

      try {
        userEntity = createUser(userName, userName, userName, true);
      } catch (AmbariException e) {
        LOG.error(String.format("Failed to create new user: %s", userName), e);
        userEntity = null;
      }

      if (userEntity != null) {
        LOG.trace("Enabling LDAP authentication for the user account with the username {}.", userName);
        try {
          addLdapAuthentication(userEntity, user.getDn(), false);
        } catch (AmbariException e) {
          LOG.warn(String.format("Failed to enable LDAP authentication for the user account with the username %s: %s", userName, e.getLocalizedMessage()), e);
        }

        userDAO.merge(userEntity);

        // Add the new user to the allUsers map.
        allUsers.put(userEntity.getUserName(), userEntity);
      }
    }

    // prepare create groups
    final Set<GroupEntity> groupsToCreate = new HashSet<>();
    for (LdapGroupDto group: batchInfo.getGroupsToBeCreated()) {
      final PrincipalEntity principalEntity = new PrincipalEntity();
      principalEntity.setPrincipalType(groupPrincipalType);
      principalsToCreate.add(principalEntity);

      final GroupEntity groupEntity = new GroupEntity();
      groupEntity.setGroupName(group.getGroupName());
      groupEntity.setPrincipal(principalEntity);
      groupEntity.setGroupType(GroupType.LDAP);

      allGroups.put(groupEntity.getGroupName(), groupEntity);
      groupsToCreate.add(groupEntity);
    }

    // create groups
    principalDAO.create(principalsToCreate);
    groupDAO.create(groupsToCreate);

    // create membership
    final Set<MemberEntity> membersToCreate = new HashSet<>();
    final Set<GroupEntity> groupsToUpdate = new HashSet<>();
    for (LdapUserGroupMemberDto member : batchInfo.getMembershipToAdd()) {
      final MemberEntity memberEntity = new MemberEntity();
      final GroupEntity groupEntity = allGroups.get(member.getGroupName());
      memberEntity.setGroup(groupEntity);
      memberEntity.setUser(allUsers.get(member.getUserName()));
      groupEntity.getMemberEntities().add(memberEntity);
      groupsToUpdate.add(groupEntity);
      membersToCreate.add(memberEntity);
    }

    // handle adminGroupMappingRules
    processLdapAdminGroupMappingRules(membersToCreate);

    memberDAO.create(membersToCreate);
    groupDAO.merge(groupsToUpdate); // needed for Derby DB as it doesn't fetch newly added members automatically

    // remove membership
    final Set<MemberEntity> membersToRemove = new HashSet<>();
    for (LdapUserGroupMemberDto member : batchInfo.getMembershipToRemove()) {
      MemberEntity memberEntity = memberDAO.findByUserAndGroup(member.getUserName(), member.getGroupName());
      if (memberEntity != null) {
        membersToRemove.add(memberEntity);
      }
    }
    memberDAO.remove(membersToRemove);

    // clear cached entities
    entityManagerProvider.get().getEntityManagerFactory().getCache().evictAll();
  }

  private void processLdapAdminGroupMappingRules(Set<MemberEntity> membershipsToCreate) {

    if (membershipsToCreate.isEmpty()) {
      LOG.debug("There are no new memberships for which to process administrator group mapping rules.");
      return;
    }

    AmbariLdapConfiguration ldapConfiguration = ldapConfigurationProvider.get();
    if (ldapConfiguration == null) {
      LOG.warn("The LDAP configuration is not available - no administrator group mappings will be processed.");
      return;
    }

    String adminGroupMappings = ldapConfiguration.groupMappingRules();
    if (Strings.isNullOrEmpty(adminGroupMappings)) {
      LOG.debug("There are no administrator group mappings to be processed.");
      return;
    }

    LOG.info("Processing admin group mapping rules [{}]. Membership entry count: [{}]", adminGroupMappings, membershipsToCreate.size());

    // parse the comma separated list of mapping rules
    Set<String> ldapAdminGroups = Sets.newHashSet(adminGroupMappings.split(","));

    // LDAP users to become ambari administrators
    Set<UserEntity> ambariAdminProspects = Sets.newHashSet();

    // gathering all the users that need to be ambari admins
    for (MemberEntity memberEntity : membershipsToCreate) {
      if (ldapAdminGroups.contains(memberEntity.getGroup().getGroupName())) {
        LOG.debug("Ambari admin user prospect: [{}] ", memberEntity.getUser().getUserName());
        ambariAdminProspects.add(memberEntity.getUser());
      }
    }

    // granting admin privileges to the admin prospects
    for (UserEntity userEntity : ambariAdminProspects) {
      LOG.info("Granting ambari admin roles to the user: {}", userEntity.getUserName());
      grantAdminPrivilege(userEntity.getUserId());
    }

  }

  /**
   * Assembles a map where the keys are usernames and values are Lists with groups associated with users.
   *
   * @param usersToCreate a list with user entities
   * @return the populated map instance
   */
  private Map<String, Set<String>> getUsersToGroupMap(Set<UserEntity> usersToCreate) {
    Map<String, Set<String>> usersToGroups = new HashMap<>();

    for (UserEntity userEntity : usersToCreate) {

      // make sure user entities are refreshed so that membership is updated
      userEntity = userDAO.findByPK(userEntity.getUserId());

      usersToGroups.put(userEntity.getUserName(), new HashSet<>());

      for (MemberEntity memberEntity : userEntity.getMemberEntities()) {
        usersToGroups.get(userEntity.getUserName()).add(memberEntity.getGroup().getGroupName());
      }
    }

    return usersToGroups;
  }

  /**
   * Gets the explicit and implicit privileges for the given user.
   * <p>
   * The explicit privileges are the privileges that have be explicitly set by assigning roles to
   * a user.  For example the Cluster Operator role on a given cluster gives that the ability to
   * start and stop services in that cluster, among other privileges for that particular cluster.
   * <p>
   * The implicit privileges are the privileges that have been given to the roles themselves which
   * in turn are granted to the users that have been assigned those roles. For example if the
   * Cluster User role for a given cluster has been given View User access on a specified File View
   * instance, then all users who have the Cluster User role for that cluster will implicitly be
   * granted View User access on that File View instance.
   *
   * @param userEntity the relevant user
   * @return the collection of implicit and explicit privileges
   */
  public Collection<PrivilegeEntity> getUserPrivileges(UserEntity userEntity) {
    if (userEntity == null) {
      return Collections.emptyList();
    }

    // get all of the privileges for the user
    List<PrincipalEntity> principalEntities = new LinkedList<>();

    principalEntities.add(userEntity.getPrincipal());

    List<MemberEntity> memberEntities = memberDAO.findAllMembersByUser(userEntity);

    for (MemberEntity memberEntity : memberEntities) {
      principalEntities.add(memberEntity.getGroup().getPrincipal());
    }

    List<PrivilegeEntity> explicitPrivilegeEntities = privilegeDAO.findAllByPrincipal(principalEntities);
    List<PrivilegeEntity> implicitPrivilegeEntities = getImplicitPrivileges(explicitPrivilegeEntities);
    List<PrivilegeEntity> privilegeEntities;

    if (implicitPrivilegeEntities.isEmpty()) {
      privilegeEntities = explicitPrivilegeEntities;
    } else {
      privilegeEntities = new LinkedList<>();
      privilegeEntities.addAll(explicitPrivilegeEntities);
      privilegeEntities.addAll(implicitPrivilegeEntities);
    }

    return privilegeEntities;
  }

  /**
   * Gets the explicit and implicit privileges for the given group.
   * <p>
   * The explicit privileges are the privileges that have be explicitly set by assigning roles to
   * a group.  For example the Cluster Operator role on a given cluster gives that the ability to
   * start and stop services in that cluster, among other privileges for that particular cluster.
   * <p>
   * The implicit privileges are the privileges that have been given to the roles themselves which
   * in turn are granted to the groups that have been assigned those roles. For example if the
   * Cluster User role for a given cluster has been given View User access on a specified File View
   * instance, then all groups that have the Cluster User role for that cluster will implicitly be
   * granted View User access on that File View instance.
   *
   * @param groupEntity the relevant group
   * @return the collection of implicit and explicit privileges
   */
  public Collection<PrivilegeEntity> getGroupPrivileges(GroupEntity groupEntity) {
    if (groupEntity == null) {
      return Collections.emptyList();
    }

    // get all of the privileges for the group
    List<PrincipalEntity> principalEntities = new LinkedList<>();

    principalEntities.add(groupEntity.getPrincipal());

    List<PrivilegeEntity> explicitPrivilegeEntities = privilegeDAO.findAllByPrincipal(principalEntities);
    List<PrivilegeEntity> implicitPrivilegeEntities = getImplicitPrivileges(explicitPrivilegeEntities);
    List<PrivilegeEntity> privilegeEntities;

    if (implicitPrivilegeEntities.isEmpty()) {
      privilegeEntities = explicitPrivilegeEntities;
    } else {
      privilegeEntities = new LinkedList<>();
      privilegeEntities.addAll(explicitPrivilegeEntities);
      privilegeEntities.addAll(implicitPrivilegeEntities);
    }

    return privilegeEntities;
  }

  /**
   * Gets the explicit and implicit authorities for the given user.
   * <p>
   * The explicit authorities are the authorities that have be explicitly set by assigning roles to
   * a user.  For example the Cluster Operator role on a given cluster gives that the ability to
   * start and stop services in that cluster, among other privileges for that particular cluster.
   * <p>
   * The implicit authorities are the authorities that have been given to the roles themselves which
   * in turn are granted to the users that have been assigned those roles. For example if the
   * Cluster User role for a given cluster has been given View User access on a specified File View
   * instance, then all users who have the Cluster User role for that cluster will implicitly be
   * granted View User access on that File View instance.
   *
   * @param userName the username for the relevant user
   * @return the users collection of implicit and explicit granted authorities
   */
  public Collection<AmbariGrantedAuthority> getUserAuthorities(String userName) {
    return getUserAuthorities(getUserEntity(userName));
  }

  /**
   * Gets the explicit and implicit authorities for the given user.
   * <p>
   * The explicit authorities are the authorities that have be explicitly set by assigning roles to
   * a user.  For example the Cluster Operator role on a given cluster gives that the ability to
   * start and stop services in that cluster, among other privileges for that particular cluster.
   * <p>
   * The implicit authorities are the authorities that have been given to the roles themselves which
   * in turn are granted to the users that have been assigned those roles. For example if the
   * Cluster User role for a given cluster has been given View User access on a specified File View
   * instance, then all users who have the Cluster User role for that cluster will implicitly be
   * granted View User access on that File View instance.
   *
   * @param userEntity the relevant user
   * @return the users collection of implicit and explicit granted authorities
   */
  public Collection<AmbariGrantedAuthority> getUserAuthorities(UserEntity userEntity) {
    if (userEntity == null) {
      return Collections.emptyList();
    }

    Collection<PrivilegeEntity> privilegeEntities = getUserPrivileges(userEntity);

    Set<AmbariGrantedAuthority> authorities = new HashSet<>(privilegeEntities.size());

    for (PrivilegeEntity privilegeEntity : privilegeEntities) {
      authorities.add(new AmbariGrantedAuthority(privilegeEntity));
    }

    return authorities;
  }

  /**
   * Gets the implicit privileges based on the set of roles found in a collection of privileges.
   * <p>
   * The implicit privileges are the privileges that have been given to the roles themselves which
   * in turn are granted to the groups that have been assigned those roles. For example if the
   * Cluster User role for a given cluster has been given View User access on a specified File View
   * instance, then all groups that have the Cluster User role for that cluster will implicitly be
   * granted View User access on that File View instance.
   *
   * @param privilegeEntities the relevant privileges
   * @return the collection explicit privileges
   */
  private List<PrivilegeEntity> getImplicitPrivileges(List<PrivilegeEntity> privilegeEntities) {

    if ((privilegeEntities == null) || privilegeEntities.isEmpty()) {
      return Collections.emptyList();
    }

    List<PrivilegeEntity> implicitPrivileges = new LinkedList<>();

    // A list of principals representing roles/permissions. This collection of roles will be used to
    // find additional inherited privileges based on the assigned roles.
    // For example a File View instance may be set to be accessible to all authenticated user with
    // the Cluster User role.
    List<PrincipalEntity> rolePrincipals = new ArrayList<>();

    for (PrivilegeEntity privilegeEntity : privilegeEntities) {
      // Add the principal representing the role associated with this PrivilegeEntity to the collection
      // of roles.
      PrincipalEntity rolePrincipal = privilegeEntity.getPermission().getPrincipal();
      if (rolePrincipal != null) {
        rolePrincipals.add(rolePrincipal);
      }
    }

    // If the collections of assigned roles is not empty find the inherited priviliges.
    if (!rolePrincipals.isEmpty()) {
      // For each "role" see if any privileges have been granted...
      implicitPrivileges.addAll(privilegeDAO.findAllByPrincipal(rolePrincipals));
    }

    return implicitPrivileges;
  }


  /**
   * Gets the collection of {@link UserAuthenticationEntity}s for a given user.
   *
   * @param username           the username of a user; if null assumes all users
   * @param authenticationType the authentication type, if null assumes all
   * @return a collection of the requested {@link UserAuthenticationEntity}s
   */
  public Collection<UserAuthenticationEntity> getUserAuthenticationEntities(String username, UserAuthenticationType authenticationType) {
    UserEntity userEntity;

    if (!StringUtils.isEmpty(username)) {
      userEntity = userDAO.findUserByName(username);

      if (userEntity == null) {
        // The requested user was not found, return null
        return null;
      }
    } else {
      // The request is for all users
      userEntity = null;
    }

    return getUserAuthenticationEntities(userEntity, authenticationType);
  }

  /**
   * Gets the collection of {@link UserAuthenticationEntity}s for a given user.
   *
   * @param userEntity         the user; if null assumes all users
   * @param authenticationType the authentication type, if null assumes all
   * @return a collection of the requested {@link UserAuthenticationEntity}s
   */
  public Collection<UserAuthenticationEntity> getUserAuthenticationEntities(UserEntity userEntity, UserAuthenticationType authenticationType) {
    if (userEntity == null) {
      if (authenticationType == null) {
        // Get all
        return userAuthenticationDAO.findAll();
      } else {
        // Get for the specified type
        return userAuthenticationDAO.findByType(authenticationType);
      }
    } else {
      List<UserAuthenticationEntity> authenticationEntities = userAuthenticationDAO.findByUser(userEntity);

      if (authenticationType == null) {
        // Get all for the specified user
        return authenticationEntities;
      } else {
        // Get for the specified user and type
        List<UserAuthenticationEntity> pruned = new ArrayList<>();
        for (UserAuthenticationEntity authenticationEntity : authenticationEntities) {
          if (authenticationEntity.getAuthenticationType() == authenticationType) {
            pruned.add(authenticationEntity);
          }
        }

        return pruned;
      }
    }
  }

  /**
   * Find the {@link UserAuthenticationEntity} items for a specific {@link UserAuthenticationType}
   * and key value.
   *
   * @param authenticationType the authentication type
   * @param key                the key value
   * @return the found collection of {@link UserAuthenticationEntity} values
   */
  public Collection<UserAuthenticationEntity> getUserAuthenticationEntities(UserAuthenticationType authenticationType, String key) {
    return userAuthenticationDAO.findByTypeAndKey(authenticationType, key);
  }

  /**
   * Modifies authentication key of an authentication source for a user
   *
   * @throws AmbariException
   */
  @Transactional
  public synchronized void modifyAuthentication(UserAuthenticationEntity userAuthenticationEntity, String currentKey, String newKey, boolean isSelf) throws AmbariException {

    if (userAuthenticationEntity != null) {
      if (userAuthenticationEntity.getAuthenticationType() == UserAuthenticationType.LOCAL) {
        // If the authentication record represents a local password and the authenticated user is
        // changing the password for himself, ensure the old key value matches the current key value
        // If the authenticated user is can manager users and is not changing his own password, there
        // is no need to check that the authenticated user knows the current password - just update it.
        if (isSelf &&
            (StringUtils.isEmpty(currentKey) || !passwordEncoder.matches(currentKey, userAuthenticationEntity.getAuthenticationKey()))) {
          // The authenticated user is the same user as subject user and the correct current password
          // was not supplied.
          throw new AmbariException("Wrong current password provided");
        }

        validatePassword(newKey);

        // If we get here the authenticated user is authorized to change the password for the subject
        // user and the correct current password was supplied (if required).
        userAuthenticationEntity.setAuthenticationKey(passwordEncoder.encode(newKey));
      } else {
        // If we get here the authenticated user is authorized to change the key for the subject.
        userAuthenticationEntity.setAuthenticationKey(newKey);
      }

      userAuthenticationDAO.merge(userAuthenticationEntity);
    }
  }

  public void removeAuthentication(String username, Long authenticationId) {
    removeAuthentication(getUserEntity(username), authenticationId);
  }

  @Transactional
  public void removeAuthentication(UserEntity userEntity, Long authenticationId) {
    if ((userEntity != null) && (authenticationId != null)) {
      boolean changed = false;

      // Ensure we have a latest version of an attached UserEntity...
      userEntity = userDAO.findByPK(userEntity.getUserId());

      // Find the remove the specified UserAuthenticationEntity from the user's collection of
      // UserAuthenticationEntities
      List<UserAuthenticationEntity> authenticationEntities = userEntity.getAuthenticationEntities();
      Iterator<UserAuthenticationEntity> iterator = authenticationEntities.iterator();
      while (iterator.hasNext()) {
        UserAuthenticationEntity authenticationEntity = iterator.next();
        if (authenticationId.equals(authenticationEntity.getUserAuthenticationId())) {
          userAuthenticationDAO.remove(authenticationEntity);
          iterator.remove();
          changed = true;
          break;
        }
      }

      if (changed) {
        // Update the UserEntity to realize the changed set of authentication sources...
        userDAO.merge(userEntity);
      }
    }
  }


  /**
   * Adds a new authentication type for the given user.
   *
   * @param userEntity         the user
   * @param authenticationType the authentication type
   * @param key                the relevant key
   * @throws AmbariException
   * @see #addLocalAuthentication(UserEntity, String)
   * @see #addLdapAuthentication(UserEntity, String)
   * @see #addJWTAuthentication(UserEntity, String)
   * @see #addKerberosAuthentication(UserEntity, String)
   * @see #addPamAuthentication(UserEntity, String)
   */
  public void addAuthentication(UserEntity userEntity, UserAuthenticationType authenticationType, String key) throws AmbariException {
    switch (authenticationType) {
      case LOCAL:
        addLocalAuthentication(userEntity, key);
        break;
      case LDAP:
        addLdapAuthentication(userEntity, key);
        break;
      case JWT:
        addJWTAuthentication(userEntity, key);
        break;
      case PAM:
        addPamAuthentication(userEntity, key);
        break;
      case KERBEROS:
        addKerberosAuthentication(userEntity, key);
        break;
      default:
        throw new AmbariException("Unexpected user authentication type");
    }
  }


  /**
   * Adds the ability for a user to authenticate using a JWT token.
   * <p>
   * The key for this authentication mechanism is the username expected to be in the JWT token.
   * <p>
   * The created {@link UserAuthenticationEntity} and the supplied {@link UserEntity} are persisted.
   *
   * @param userEntity the user
   * @param key        the relevant key
   * @throws AmbariException
   * @see #addJWTAuthentication(UserEntity, String, boolean)
   */
  public void addJWTAuthentication(UserEntity userEntity, String key) throws AmbariException {
    addJWTAuthentication(userEntity, key, true);
  }

  /**
   * Adds the ability for a user to authenticate using a JWT token.
   * <p>
   * The key for this authentication mechanism is the username expected to be in the JWT token.
   *
   * @param userEntity the user
   * @param key        the relevant key
   * @param persist    true, to persist the created entity; false, to not persist the created entity
   * @throws AmbariException
   */
  public void addJWTAuthentication(UserEntity userEntity, String key, boolean persist) throws AmbariException {
    addAuthentication(userEntity,
        UserAuthenticationType.JWT,
        key,
            (user, authKey) -> {
              List<UserAuthenticationEntity> authenticationEntities = user.getAuthenticationEntities();

              // Ensure only one UserAuthenticationEntity exists for JWT for the user...
              for (UserAuthenticationEntity entity : authenticationEntities) {
                if ((entity.getAuthenticationType() == UserAuthenticationType.JWT) &&
                    ((authKey == null) ? (entity.getAuthenticationKey() == null) : authKey.equals(entity.getAuthenticationKey()))) {
                  throw new AmbariException("The authentication type already exists for this user");
                }
              }
            },
        persist);
  }

  /**
   * Adds the ability for a user to authenticate using a Kerberos token.
   * <p>
   * The created {@link UserAuthenticationEntity} and the supplied {@link UserEntity} are persisted.
   *
   * @param userEntity    the user
   * @param principalName the user's principal name
   * @throws AmbariException
   * @see #addKerberosAuthentication(UserEntity, String, boolean)
   */
  public void addKerberosAuthentication(UserEntity userEntity, String principalName) throws AmbariException {
    addKerberosAuthentication(userEntity, principalName, true);
  }

  /**
   * Adds the ability for a user to authenticate using a Kerberos token.
   *
   * @param userEntity    the user
   * @param principalName the user's principal name
   * @param persist       true, to persist the created entity; false, to not persist the created entity
   * @throws AmbariException
   */
  public void addKerberosAuthentication(UserEntity userEntity, String principalName, boolean persist) throws AmbariException {
    addAuthentication(userEntity,
        UserAuthenticationType.KERBEROS,
        principalName,
            (user, key) -> {
              // Ensure no other authentication entries exist for the same principal...
              if (!CollectionUtils.isEmpty(userAuthenticationDAO.findByTypeAndKey(UserAuthenticationType.KERBEROS, key))) {
                throw new AmbariException("The authentication type already exists for this principal");
              }
            },
        persist);
  }

  /**
   * Adds the ability for a user to authenticate using a password stored in Ambari's database
   * <p>
   * The supplied plaintext password will be encoded before storing.
   * <p>
   * The created {@link UserAuthenticationEntity} and the supplied {@link UserEntity} are persisted.
   *
   * @param userEntity the user
   * @param password   the user's plaintext password
   * @throws AmbariException
   * @see #addLocalAuthentication(UserEntity, String, boolean)
   */
  public void addLocalAuthentication(UserEntity userEntity, String password) throws AmbariException {
    addLocalAuthentication(userEntity, password, true);
  }

  /**
   * Adds the ability for a user to authenticate using a password stored in Ambari's database
   * <p>
   * The supplied plaintext password will be encoded before storing.
   *
   * @param userEntity the user
   * @param password   the user's plaintext password
   * @param persist    true, to persist the created entity; false, to not persist the created entity
   * @throws AmbariException
   */
  public void addLocalAuthentication(UserEntity userEntity, String password, boolean persist) throws AmbariException {

    // Ensure the password meets configured minimal requirements, if any
    validatePassword(password);

    // Encode the password..
    String encodedPassword = passwordEncoder.encode(password);

    addAuthentication(userEntity,
        UserAuthenticationType.LOCAL,
        encodedPassword,
            (user, key) -> {
              List<UserAuthenticationEntity> authenticationEntities = user.getAuthenticationEntities();

              // Ensure only one UserAuthenticationEntity exists for LOCAL for the user...
              for (UserAuthenticationEntity entity : authenticationEntities) {
                if (entity.getAuthenticationType() == UserAuthenticationType.LOCAL) {
                  throw new AmbariException("The authentication type already exists for this user");
                }
              }
            },
        persist);
  }

  /**
   * Adds the ability for a user to authenticate using Pam
   * <p>
   * The created {@link UserAuthenticationEntity} and the supplied {@link UserEntity} are persisted.
   *
   * @param userEntity the user
   * @param userName   the user's os-level username
   * @throws AmbariException
   * @see #addPamAuthentication(UserEntity, String, boolean)
   */
  public void addPamAuthentication(UserEntity userEntity, String userName) throws AmbariException {
    addPamAuthentication(userEntity, userName, true);
  }

  /**
   * Adds the ability for a user to authenticate using Pam
   *
   * @param userEntity the user
   * @param userName   the user's os-level username
   * @param persist    true, to persist the created entity; false, to not persist the created entity
   * @throws AmbariException
   */
  public void addPamAuthentication(UserEntity userEntity, String userName, boolean persist) throws AmbariException {
    addAuthentication(userEntity,
        UserAuthenticationType.PAM,
        userName,
            (user, key) -> {
              List<UserAuthenticationEntity> authenticationEntities = user.getAuthenticationEntities();

              // Ensure only one UserAuthenticationEntity exists for PAM for the user...
              for (UserAuthenticationEntity entity : authenticationEntities) {
                if (entity.getAuthenticationType() == UserAuthenticationType.PAM) {
                  throw new AmbariException("The authentication type already exists for this user");
                }
              }
            },
        persist);
  }

  /**
   * Adds the ability for a user to authenticate using a remote LDAP server
   * <p>
   * The created {@link UserAuthenticationEntity} and the supplied {@link UserEntity} are persisted.
   *
   * @param userEntity the user
   * @param dn         the user's distinguished name
   * @throws AmbariException
   * @see #addLdapAuthentication(UserEntity, String, boolean)
   */
  public void addLdapAuthentication(UserEntity userEntity, String dn) throws AmbariException {
    addLdapAuthentication(userEntity, dn, true);
  }

  /**
   * Adds the ability for a user to authenticate using a remote LDAP server
   *
   * @param userEntity the user
   * @param dn         the user's distinguished name
   * @param persist    true, to persist the created entity; false, to not persist the created entity
   * @throws AmbariException
   */
  public void addLdapAuthentication(UserEntity userEntity, String dn, boolean persist) throws AmbariException {
    addAuthentication(userEntity,
        UserAuthenticationType.LDAP,
        StringUtils.lowerCase(dn), // DNs are case-insensitive and are stored internally as the bytes of lowercase characters
            (user, key) -> {
              List<UserAuthenticationEntity> authenticationEntities = user.getAuthenticationEntities();

              // Ensure only one UserAuthenticationEntity exists for LDAP for the user...
              for (UserAuthenticationEntity entity : authenticationEntities) {
                if ((entity.getAuthenticationType() == UserAuthenticationType.LDAP) &&
                    ((key == null) ? (entity.getAuthenticationKey() == null) : key.equalsIgnoreCase(entity.getAuthenticationKey()))) {
                  throw new AmbariException("The authentication type already exists for this user");
                }
              }
            },
        persist);
  }

  /**
   * Worker to add a user authentication methods for a user.
   *
   * @param userEntity the user
   * @param type       the authentication type
   * @param key        the authentication type specific metadata
   * @param validator  the authentication type specific validator
   * @param persist    true, to persist the created entity; false, to not persist the created entity
   * @throws AmbariException
   */
  private void addAuthentication(UserEntity userEntity, UserAuthenticationType type, String key,
                                 Validator validator, boolean persist)
      throws AmbariException {

    if (userEntity == null) {
      throw new AmbariException("Missing user");
    }

    validator.validate(userEntity, key);

    List<UserAuthenticationEntity> authenticationEntities = userAuthenticationDAO.findByUser(userEntity);

    UserAuthenticationEntity authenticationEntity = new UserAuthenticationEntity();
    authenticationEntity.setUser(userEntity);
    authenticationEntity.setAuthenticationType(type);
    authenticationEntity.setAuthenticationKey(key);
    authenticationEntities.add(authenticationEntity);

    userEntity.setAuthenticationEntities(authenticationEntities);

    if (persist) {
      userDAO.merge(userEntity);
    }
  }

  /**
   * Increments the named user's consecutive authentication failure count by <code>1</code>.
   * <p>
   * This operation is safe when concurrent authentication attempts by the same username are made
   * due to {@link UserEntity#version} and optimistic locking.
   *
   * @param username the user's username
   * @return the updated number of consecutive authentication failures; or null if the user does not exist
   */
  public Integer incrementConsecutiveAuthenticationFailures(String username) {
    return incrementConsecutiveAuthenticationFailures(getUserEntity(username));
  }

  /**
   * Increments the named user's consecutive authentication failure count by <code>1</code>.
   * <p>
   * This operation is safe when concurrent authentication attempts by the same username are made
   * due to {@link UserEntity#version} and optimistic locking.
   *
   * @param userEntity the user
   * @return the updated number of consecutive authentication failures; or null if the user does not exist
   */
  public Integer incrementConsecutiveAuthenticationFailures(UserEntity userEntity) {
    if (userEntity != null) {
      Command command = new Command() {
        @Override
        public void perform(UserEntity userEntity) {
          userEntity.incrementConsecutiveFailures();
        }
      };

      userEntity = safelyUpdateUserEntity(userEntity, command, MAX_RETRIES);
    }

    return (userEntity == null) ? null : userEntity.getConsecutiveFailures();
  }

  /**
   * Resets the named user's consecutive authentication failure count to <code>0</code>.
   * <p>
   * This operation is safe when concurrent authentication attempts by the same username are made
   * due to {@link UserEntity#version} and optimistic locking.
   *
   * @param username the user's username
   */
  public void clearConsecutiveAuthenticationFailures(String username) {
    clearConsecutiveAuthenticationFailures(getUserEntity(username));
  }

  /**
   * Resets the named user's consecutive authentication failure count to <code>0</code>.
   * <p>
   * This operation is safe when concurrent authentication attempts by the same username are made
   * due to {@link UserEntity#version} and optimistic locking.
   *
   * @param userEntity the user
   */
  public void clearConsecutiveAuthenticationFailures(UserEntity userEntity) {
    if (userEntity != null) {
      if (userEntity.getConsecutiveFailures() != 0) {
        Command command = new Command() {
          @Override
          public void perform(UserEntity userEntity) {
            userEntity.setConsecutiveFailures(0);
          }
        };

        safelyUpdateUserEntity(userEntity, command, MAX_RETRIES);
      }
    }
  }

  /***
   * Attempts to update the specified {@link UserEntity} while handling {@link OptimisticLockException}s
   * by obtaining the latest version of the {@link UserEntity} and retrying the operation.
   *
   * If the maximum number of retries is exceeded (see {@link #MAX_RETRIES}), then the operation
   * will fail by rethrowing the last exception encountered.
   *
   *
   * @param userEntity the user entity
   * @param command  a command to perform on the user entity object that changes it state thus needing
   *                 to be persisted
   */
  public UserEntity safelyUpdateUserEntity(UserEntity userEntity, Command command) {
    return safelyUpdateUserEntity(userEntity, command, MAX_RETRIES);
  }

  /***
   * Attempts to update the specified {@link UserEntity} while handling {@link OptimisticLockException}s
   * by obtaining the latest version of the {@link UserEntity} and retrying the operation.
   *
   * If the maximum number of retries is exceeded, then the operation will fail by rethrowing the last
   * exception encountered.
   *
   *
   * @param userEntity the user entity
   * @param command  a command to perform on the user entity object that changes it state thus needing
   *                 to be persisted
   * @param maxRetries the maximum number of reties to peform before failing
   */
  public UserEntity safelyUpdateUserEntity(UserEntity userEntity, Command command, int maxRetries) {
    int retriesLeft = maxRetries;

    do {
      try {
        command.perform(userEntity);
        userDAO.merge(userEntity);

        // The merge was a success, break out of this loop and return
        return userEntity;
      } catch (Throwable t) {
        Throwable cause = t;
        int failSafe = 50; // counter to ensure the following do/while loop does not loop indefinitely

        do {
          if (cause instanceof OptimisticLockException) {
            // An OptimisticLockException was caught, refresh the entity and retry.
            Integer userID = userEntity.getUserId();

            // Find the userEntity record to make sure the object is managed by JPA.  The passed-in
            // object may be detached, therefore calling reset on it will fail.
            userEntity = userDAO.findByPK(userID);

            if (userEntity == null) {
              LOG.warn("Failed to find user with user id of {}.  The user may have been removed. Aborting.", userID);
              return null;  // return since this user is no longer available.
            }

            retriesLeft--;

            // The the number of attempts has been exhausted, re-throw the exception
            if (retriesLeft == 0) {
              LOG.error("Failed to update the user's ({}) consecutive failures value due to an OptimisticLockException.  Aborting.",
                  userEntity.getUserName());
              throw t;
            } else {
              LOG.warn("Failed to update the user's ({}) consecutive failures value due to an OptimisticLockException.  {} retries left, retrying...",
                  userEntity.getUserName(), retriesLeft);
            }

            break;
          } else {
            // Get the cause to see if it is an OptimisticLockException
            cause = cause.getCause();
          }

          // decrement the failsafe counter to ensure we do not get stuck in an infinite loop.
          failSafe--;
        } while ((cause != null) && (cause != t) && (failSafe > 0)); // We are out of causes

        if ((cause == null) || (cause == t) || failSafe == 0) {
          throw t;
        }
      }
    } while (retriesLeft > 0); // We are out of retries

    return userEntity;
  }

  /**
   * Validates the password meets configured requirements.
   * <p>
   * In the future this may be configurable. For now just make sure the password is not empty.
   *
   * @param password the password
   * @return true if the password is valid; false otherwise
   */
  public boolean validatePassword(String password) throws AmbariException {
    // TODO: validate the new password...
    if (StringUtils.isEmpty(password)) {
      throw new AmbariException("The new password does not meet the Ambari password requirements");
    }

    return true;
  }

  /**
   * Validator is an interface to be implemented by authentication type specific validators to ensure
   * new user authentication records meet the specific requirements for the relative authentication
   * type.
   */
  private interface Validator {
    /**
     * Validate the authentication type specific key meets the requirements for the relative user
     * authentication type.
     *
     * @param userEntity the user
     * @param key        the key (or metadata)
     * @throws AmbariException
     */
    void validate(UserEntity userEntity, String key) throws AmbariException;
  }

  /**
   * Command is an interface used to perform operations on a {@link UserEntity} while safely updating
   * a {@link UserEntity} object.
   *
   * @see #safelyUpdateUserEntity(UserEntity, Command, int)
   */
  public interface Command {
    void perform(UserEntity userEntity);
  }
}
