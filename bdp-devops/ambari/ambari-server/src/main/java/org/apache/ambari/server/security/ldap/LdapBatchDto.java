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
package org.apache.ambari.server.security.ldap;

import java.util.HashSet;
import java.util.Set;

/**
 * Contains information for batch database update on LDAP synchronization.
 */
public class LdapBatchDto {
  private final Set<LdapGroupDto> groupsToBecomeLdap = new HashSet<>();
  private final Set<LdapGroupDto> groupsToBeCreated = new HashSet<>();
  private final Set<LdapGroupDto> groupsToBeRemoved = new HashSet<>();
  private final Set<LdapGroupDto> groupsProcessedInternal = new HashSet<>();
  private final Set<LdapUserDto> usersSkipped = new HashSet<>();
  private final Set<LdapUserDto> usersIgnored = new HashSet<>();
  private final Set<LdapUserDto> usersToBecomeLdap = new HashSet<>();
  private final Set<LdapUserDto> usersToBeCreated = new HashSet<>();
  private final Set<LdapUserDto> usersToBeRemoved = new HashSet<>();
  private final Set<LdapUserGroupMemberDto> membershipToAdd = new HashSet<>();
  private final Set<LdapUserGroupMemberDto> membershipToRemove = new HashSet<>();

  public Set<LdapUserDto> getUsersSkipped() {
    return usersSkipped;
  }

  public Set<LdapUserDto> getUsersIgnored() {
    return usersIgnored;
  }

  public Set<LdapGroupDto> getGroupsToBecomeLdap() {
    return groupsToBecomeLdap;
  }

  public Set<LdapGroupDto> getGroupsToBeCreated() {
    return groupsToBeCreated;
  }

  public Set<LdapUserDto> getUsersToBecomeLdap() {
    return usersToBecomeLdap;
  }

  public Set<LdapUserDto> getUsersToBeCreated() {
    return usersToBeCreated;
  }

  public Set<LdapUserGroupMemberDto> getMembershipToAdd() {
    return membershipToAdd;
  }

  public Set<LdapUserGroupMemberDto> getMembershipToRemove() {
    return membershipToRemove;
  }

  public Set<LdapGroupDto> getGroupsToBeRemoved() {
    return groupsToBeRemoved;
  }

  public Set<LdapUserDto> getUsersToBeRemoved() {
    return usersToBeRemoved;
  }

  public Set<LdapGroupDto> getGroupsProcessedInternal() {
    return groupsProcessedInternal;
  }
}
