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

package org.apache.ambari.server.orm.entities;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Id;

import org.apache.ambari.server.Role;

@SuppressWarnings("serial")
public class RoleSuccessCriteriaEntityPK implements Serializable {
  private Long requestId;

  @Id
  @Column(name = "request_id")
  public Long getRequestId() {
    return requestId;
  }

  public void setRequestId(Long requestId) {
    this.requestId = requestId;
  }

  private Long stageId;

  @Id
  @Column(name = "stage_id")
  public Long getStageId() {
    return stageId;
  }

  public void setStageId(Long stageId) {
    this.stageId = stageId;
  }

  private String role;

  @Column(name = "role")
  @Id
  public Role getRole() {
    return Role.valueOf(role);
  }

  public void setRole(Role role) {
    this.role = role.name();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RoleSuccessCriteriaEntityPK that = (RoleSuccessCriteriaEntityPK) o;

    if (requestId != null ? !requestId.equals(that.requestId) : that.requestId != null) return false;
    if (role != null ? !role.equals(that.role) : that.role != null) return false;
    if (stageId != null ? !stageId.equals(that.stageId) : that.stageId != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = requestId != null ? requestId.hashCode() : 0;
    result = 31 * result + (stageId != null ? stageId.hashCode() : 0);
    result = 31 * result + (role != null ? role.hashCode() : 0);
    return result;
  }
}
