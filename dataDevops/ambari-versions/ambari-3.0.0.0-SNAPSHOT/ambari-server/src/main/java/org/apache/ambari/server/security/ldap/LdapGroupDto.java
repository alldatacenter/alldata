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
 * Pojo with information about LDAP group of users.
 */
public class LdapGroupDto {
  /**
   * Name of the group.
   */
  private String groupName;

  /**
   * Set of member attributes. Usually it's either UID or DN of users.
   */
  private Set<String> memberAttributes = new HashSet<>();

  /**
   * Determines if the LDAP group is synchronized with internal group in database.
   */
  private boolean synced;

  /**
   * Get the group name.
   *
   * @return the group name
   */
  public String getGroupName() {
    return groupName;
  }

  /**
   * Set the group name.
   *
   * @param groupName the group name
   */
  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  /**
   * Get the member attributes.
   *
   * @return the set of member attributes
   */
  public Set<String> getMemberAttributes() {
    return memberAttributes;
  }

  /**
   * Set the member attributes.
   *
   * @param memberAttributes the member attributes
   */
  public void setMemberAttributes(Set<String> memberAttributes) {
    this.memberAttributes = memberAttributes;
  }

  /**
   * Get the synced flag.
   *
   * @return the synced flag
   */
  public boolean isSynced() {
    return synced;
  }

  /**
   * Set the synced flag
   *
   * @param synced the synced flag
   */
  public void setSynced(boolean synced) {
    this.synced = synced;
  }

  @Override
  public int hashCode() {
    int result = groupName != null ? groupName.hashCode() : 0;
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    LdapGroupDto that = (LdapGroupDto) o;

    if (groupName != null ? !groupName.equals(that.getGroupName()) : that.getGroupName() != null) return false;

    return true;
  }
}
