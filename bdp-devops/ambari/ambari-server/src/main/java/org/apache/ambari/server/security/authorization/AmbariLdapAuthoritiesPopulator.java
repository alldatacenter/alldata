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

import java.util.Collection;
import java.util.Collections;

import org.apache.ambari.server.orm.dao.MemberDAO;
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.authentication.InvalidUsernamePasswordCombinationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

import com.google.inject.Inject;

/**
 * Provides authorities population for LDAP user from LDAP catalog
 */
public class AmbariLdapAuthoritiesPopulator implements LdapAuthoritiesPopulator {
  private static final Logger log = LoggerFactory.getLogger(AmbariLdapAuthoritiesPopulator.class);

  private AuthorizationHelper authorizationHelper;
  UserDAO userDAO;
  MemberDAO memberDAO;
  PrivilegeDAO privilegeDAO;
  Users users;

  @Inject
  public AmbariLdapAuthoritiesPopulator(AuthorizationHelper authorizationHelper,
                                        UserDAO userDAO, MemberDAO memberDAO, PrivilegeDAO privilegeDAO,
                                        Users users) {
    this.authorizationHelper = authorizationHelper;
    this.userDAO = userDAO;
    this.memberDAO = memberDAO;
    this.privilegeDAO = privilegeDAO;
    this.users = users;
  }

  @Override
  public Collection<? extends GrantedAuthority> getGrantedAuthorities(DirContextOperations userData, String username) {
    username = AuthorizationHelper.resolveLoginAliasToUserName(username);

    log.info("Get authorities for user " + username + " from local DB");

    UserEntity user;

    user = userDAO.findUserByName(username);
    
    if (user == null) {
      log.error("Can't get authorities for user " + username + ", he is not present in local DB");
      return Collections.emptyList();
    }
    if(!user.getActive()){
      throw new InvalidUsernamePasswordCombinationException(username);
    }

    Collection<PrivilegeEntity> privilegeEntities = users.getUserPrivileges(user);

    return authorizationHelper.convertPrivilegesToAuthorities(privilegeEntities);
  }
}
