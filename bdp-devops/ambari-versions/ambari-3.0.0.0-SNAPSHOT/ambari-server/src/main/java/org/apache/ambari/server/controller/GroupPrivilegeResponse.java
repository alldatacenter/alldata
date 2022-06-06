/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.controller;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.services.groups.GroupPrivilegeService;
import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;

import io.swagger.annotations.ApiModelProperty;

/**
 * Response schema for endpoint {@link GroupPrivilegeService#getPrivileges(HttpHeaders, UriInfo, String)}
 */
public class GroupPrivilegeResponse extends PrivilegeResponse implements ApiModel {

  private String groupName;

  /**
   *
   * @param groupName          group name
   * @param permissionLabel    permission label
   * @param permissionName     permission name
   * @param privilegeId        privilege id
   * @param principalType      principal type
   */
  public GroupPrivilegeResponse(String groupName, String permissionLabel, String permissionName, Integer privilegeId,
                               PrincipalTypeEntity.PrincipalType principalType) {
    this.groupName = groupName;
    this.permissionLabel = permissionLabel;
    this.privilegeId = privilegeId;
    this.permissionName = permissionName;
    this.principalType = principalType;
  }

  /**
   *  Returns group name
   * @return  group name
   */
  @ApiModelProperty(name = "PrivilegeInfo/group_name", required = true)
  public String getGroupName() {
    return groupName;
  }
}
