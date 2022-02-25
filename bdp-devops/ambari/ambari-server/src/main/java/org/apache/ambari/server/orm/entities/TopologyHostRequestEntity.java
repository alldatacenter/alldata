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

import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.apache.ambari.server.actionmanager.HostRoleStatus;

@Entity
@Table(name = "topology_host_request")
@NamedQueries({
  @NamedQuery(name = "TopologyHostRequestEntity.removeByIds", query = "DELETE FROM TopologyHostRequestEntity topologyHostRequest WHERE topologyHostRequest.id IN :hostRequestIds")
})
public class TopologyHostRequestEntity {
  @Id
//  @GeneratedValue(strategy = GenerationType.TABLE, generator = "topology_host_request_id_generator")
  @Column(name = "id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "stage_id", length = 10, nullable = false)
  private Long stageId;

  @Column(name = "host_name", length = 255)
  private String hostName;

  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private HostRoleStatus status;

  @Column(name = "status_message")
  private String statusMessage;

  @ManyToOne
  @JoinColumn(name = "logical_request_id", referencedColumnName = "id", nullable = false)
  private TopologyLogicalRequestEntity topologyLogicalRequestEntity;

  @ManyToOne
  @JoinColumn(name = "group_id", referencedColumnName = "id", nullable = false)
  private TopologyHostGroupEntity topologyHostGroupEntity;

  @OneToMany(mappedBy = "topologyHostRequestEntity", cascade = CascadeType.ALL, orphanRemoval = true)
  private Collection<TopologyHostTaskEntity> topologyHostTaskEntities;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getLogicalRequestId() {
    return topologyLogicalRequestEntity != null ? topologyLogicalRequestEntity.getTopologyRequestId() : null;
  }

  public Long getHostGroupId() {
    return topologyHostGroupEntity.getId();
  }

  public Long getStageId() {
    return stageId;
  }

  public void setStageId(Long stageId) {
    this.stageId = stageId;
  }

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public HostRoleStatus getStatus() {
    return status;
  }

  public void setStatus(HostRoleStatus status) {
    this.status = status;
  }

  public String getStatusMessage() {
    return statusMessage;
  }

  public void setStatusMessage(String statusMessage) {
    this.statusMessage = statusMessage;
  }

  public TopologyLogicalRequestEntity getTopologyLogicalRequestEntity() {
    return topologyLogicalRequestEntity;
  }

  public void setTopologyLogicalRequestEntity(TopologyLogicalRequestEntity topologyLogicalRequestEntity) {
    this.topologyLogicalRequestEntity = topologyLogicalRequestEntity;
  }

  public TopologyHostGroupEntity getTopologyHostGroupEntity() {
    return topologyHostGroupEntity;
  }

  public void setTopologyHostGroupEntity(TopologyHostGroupEntity topologyHostGroupEntity) {
    this.topologyHostGroupEntity = topologyHostGroupEntity;
  }

  public Collection<TopologyHostTaskEntity> getTopologyHostTaskEntities() {
    return topologyHostTaskEntities;
  }

  public void setTopologyHostTaskEntities(Collection<TopologyHostTaskEntity> topologyHostTaskEntities) {
    this.topologyHostTaskEntities = topologyHostTaskEntities;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TopologyHostRequestEntity that = (TopologyHostRequestEntity) o;

    if (!id.equals(that.id)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
