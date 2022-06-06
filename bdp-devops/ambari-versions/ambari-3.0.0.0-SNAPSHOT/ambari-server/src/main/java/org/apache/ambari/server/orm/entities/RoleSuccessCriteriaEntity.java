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

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.apache.ambari.server.Role;

@IdClass(org.apache.ambari.server.orm.entities.RoleSuccessCriteriaEntityPK.class)
@Table(name = "role_success_criteria")
@Entity
@NamedQueries({
  @NamedQuery(name = "RoleSuccessCriteriaEntity.removeByRequestStageIds", query = "DELETE FROM RoleSuccessCriteriaEntity criteria WHERE criteria.stageId = :stageId AND criteria.requestId = :requestId")
})
public class RoleSuccessCriteriaEntity {

  @Id
  @Column(name = "request_id", insertable = false, updatable = false, nullable = false)
  private Long requestId;

  @Id
  @Column(name = "stage_id", insertable = false, updatable = false, nullable = false)
  private Long stageId;

  @Id
  @Column(name = "role")
  private String role;

  @Basic
  @Column(name = "success_factor", nullable = false)
  private Double successFactor = 1d;

  @ManyToOne
  @JoinColumns({@JoinColumn(name = "request_id", referencedColumnName = "request_id", nullable = false), @JoinColumn(name = "stage_id", referencedColumnName = "stage_id", nullable = false)})
  private StageEntity stage;

  public Long getRequestId() {
    return requestId;
  }

  public void setRequestId(Long requestId) {
    this.requestId = requestId;
  }

  public Long getStageId() {
    return stageId;
  }

  public void setStageId(Long stageId) {
    this.stageId = stageId;
  }

  public Role getRole() {
    return Role.valueOf(role);
  }

  public void setRole(Role role) {
    this.role = role.name();
  }

  public Double getSuccessFactor() {
    return successFactor;
  }

  public void setSuccessFactor(Double successFactor) {
    this.successFactor = successFactor;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RoleSuccessCriteriaEntity that = (RoleSuccessCriteriaEntity) o;

    if (requestId != null ? !requestId.equals(that.requestId) : that.requestId != null) return false;
    if (role != null ? !role.equals(that.role) : that.role != null) return false;
    if (stageId != null ? !stageId.equals(that.stageId) : that.stageId != null) return false;
    if (successFactor != null ? !successFactor.equals(that.successFactor) : that.successFactor != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = requestId != null ? requestId.hashCode() : 0;
    result = 31 * result + (stageId != null ? stageId.hashCode() : 0);
    result = 31 * result + (role != null ? role.hashCode() : 0);
    result = 31 * result + (successFactor != null ? successFactor.hashCode() : 0);
    return result;
  }

  public StageEntity getStage() {
    return stage;
  }

  public void setStage(StageEntity stage) {
    this.stage = stage;
  }
}
