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

import static org.apache.commons.lang.StringUtils.defaultString;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;

import org.apache.ambari.server.state.HostState;

import java.util.Objects;

@javax.persistence.Table(name = "hoststate")
@Entity
@NamedQueries({
    @NamedQuery(name = "hostStateByHostId", query =
        "SELECT hostState FROM HostStateEntity hostState " +
            "WHERE hostState.hostId=:hostId"),
})
public class HostStateEntity {
  
  @javax.persistence.Column(name = "host_id", nullable = false, insertable = false, updatable = false)
  @Id
  private Long hostId;

  @Column(name = "available_mem", nullable = false, insertable = true, updatable = true)
  @Basic
  private Long availableMem = 0L;

  @javax.persistence.Column(name = "time_in_state", nullable = false, insertable = true, updatable = true)
  @Basic
  private Long timeInState = 0L;

  @Column(name = "health_status", insertable = true, updatable = true)
  @Basic
  private String healthStatus;

  @Column(name = "agent_version", insertable = true, updatable = true)
  @Basic
  private String agentVersion = "";

  @Column(name = "current_state", nullable = false, insertable = true, updatable = true)
  @Enumerated(value = EnumType.STRING)
  private HostState currentState = HostState.INIT;
  
  @Column(name="maintenance_state", nullable = true, insertable = true, updatable = true)
  private String maintenanceState = null;
  

  @OneToOne
  @JoinColumn(name = "host_id", referencedColumnName = "host_id", nullable = false)
  private HostEntity hostEntity;

  public Long getHostId() {
    return hostId;
  }

  public void setHostId(Long hostId) {
    this.hostId = hostId;
  }

  public Long getAvailableMem() {
    return availableMem;
  }

  public void setAvailableMem(Long availableMem) {
    this.availableMem = availableMem;
  }

  public Long getTimeInState() {
    return timeInState;
  }

  public void setTimeInState(Long timeInState) {
    this.timeInState = timeInState;
  }

  public String getHealthStatus() {
    return healthStatus;
  }

  public void setHealthStatus(String healthStatus) {
    this.healthStatus = healthStatus;
  }

  public String getAgentVersion() {
    return defaultString(agentVersion);
  }

  public void setAgentVersion(String agentVersion) {
    this.agentVersion = agentVersion;
  }

  public HostState getCurrentState() {
    return currentState;
  }

  public void setCurrentState(HostState currentState) {
    this.currentState = currentState;
  }

  public String getMaintenanceState() {
    return maintenanceState;
  }  
  
  public void setMaintenanceState(String state) {
    maintenanceState = state;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HostStateEntity that = (HostStateEntity) o;
    return Objects.equals(hostId, that.hostId) &&
            Objects.equals(availableMem, that.availableMem) &&
            Objects.equals(timeInState, that.timeInState) &&
            Objects.equals(agentVersion, that.agentVersion) &&
            currentState == that.currentState;
  }

  @Override
  public int hashCode() {
    return Objects.hash(hostId, availableMem, timeInState, agentVersion, currentState);
  }

  public HostEntity getHostEntity() {
    return hostEntity;
  }

  public void setHostEntity(HostEntity hostEntity) {
    this.hostEntity = hostEntity;
  }

}
