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

public class MemberResponse implements ApiModel{
  private final String groupName;
  private final String userName;

  public MemberResponse(String groupName, String userName) {
    this.groupName = groupName;
    this.userName = userName;
  }

  @ApiModelProperty(name = "MemberInfo/group_name")
  public String getGroupName() {
    return groupName;
  }

  @ApiModelProperty(name = "MemberInfo/user_name")
  public String getUserName() {
    return userName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    MemberResponse that = (MemberResponse) o;

    if (groupName != null ? !groupName.equals(that.groupName)
        : that.groupName != null) {
      return false;
    }
    if (userName != null ? !userName.equals(that.userName)
        : that.userName != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = groupName != null ? groupName.hashCode() : 0;
    result = 31 * result + (userName != null ? userName.hashCode() : 0);
    return result;
  }
}
