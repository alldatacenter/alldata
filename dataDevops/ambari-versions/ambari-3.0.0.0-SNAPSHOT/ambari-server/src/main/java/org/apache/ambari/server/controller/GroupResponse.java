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
package org.apache.ambari.server.controller;

import org.apache.ambari.server.security.authorization.GroupType;

import io.swagger.annotations.ApiModelProperty;

/**
 * Represents a user group maintenance response.
 */
public class GroupResponse implements ApiModel{
  private final String groupName;
  private final boolean ldapGroup;
  private final GroupType groupType;

  public GroupResponse(String groupName, boolean ldapGroup, GroupType groupType) {
    this.groupName = groupName;
    this.ldapGroup = ldapGroup;
    this.groupType = groupType;
  }

  public GroupResponse(String groupName, boolean ldapGroup) {
    this.groupName = groupName;
    this.ldapGroup = ldapGroup;
    this.groupType = GroupType.LOCAL;
  }

  @ApiModelProperty(name = "Groups/group_name")
  public String getGroupName() {
    return groupName;
  }

  @ApiModelProperty(name = "Groups/ldap_group")
  public boolean isLdapGroup() {
    return ldapGroup;
  }

  @ApiModelProperty(name = "Groups/group_type")
  public GroupType getGroupType() {
    return groupType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    GroupResponse that = (GroupResponse) o;

    if (groupName != null ? !groupName.equals(that.groupName)
        : that.groupName != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = groupName != null ? groupName.hashCode() : 0;
    return result;
  }
}
