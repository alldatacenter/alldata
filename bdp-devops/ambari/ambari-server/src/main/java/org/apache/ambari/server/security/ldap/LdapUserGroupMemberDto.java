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

/**
 * Pojo with information about LDAP membership.
 */
public class LdapUserGroupMemberDto {
  /**
   * Name of the group.
   */
  private final String groupName;

  /**
   * Name of the user.
   */
  private final String userName;

  /**
   * Constructor.
   *
   * @param groupName group name
   * @param userName user name
   */
  public LdapUserGroupMemberDto(String groupName, String userName) {
    this.groupName = groupName;
    this.userName = userName;
  }

  /**
   * Get the group name.
   *
   * @return the group name
   */
  public String getGroupName() {
    return groupName;
  }

  /**
   * Get the user name.
   *
   * @return the user name
   */
  public String getUserName() {
    return userName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    LdapUserGroupMemberDto that = (LdapUserGroupMemberDto) o;

    if (userName != null ? !userName.equals(that.userName) : that.userName != null) return false;
    if (groupName != null ? !groupName.equals(that.groupName) : that.groupName != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = userName != null ? userName.hashCode() : 0;
    result = 31 * result + (groupName != null ? groupName.hashCode() : 0);
    return result;
  }
}
