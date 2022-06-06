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

import io.swagger.annotations.ApiModelProperty;

/**
 * Represents a member maintenance request.
 */
public class MemberRequest {
  private final String groupName;
  private final String userName;

  public MemberRequest(String groupName, String userName) {
    this.groupName = groupName;
    this.userName  = userName;
  }

  @ApiModelProperty(name = "MemberInfo/group_name", required = true)
  public String getGroupName() {
    return groupName;
  }

  @ApiModelProperty(name = "MemberInfo/user_name", required = true)
  public String getUserName() {
    return userName;
  }

  @Override
  public String toString() {
    return "MemberRequest [groupName=" + groupName + ", userName=" + userName
        + "]";
  }
}
