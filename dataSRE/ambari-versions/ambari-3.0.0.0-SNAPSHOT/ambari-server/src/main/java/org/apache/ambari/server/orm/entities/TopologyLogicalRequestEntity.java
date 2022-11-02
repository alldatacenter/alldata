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
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "topology_logical_request")
@NamedQueries({
  @NamedQuery(name = "TopologyLogicalRequestEntity.findRequestIds", query = "SELECT DISTINCT t.topologyLogicalRequestEntity.topologyRequestId from TopologyHostRequestEntity t WHERE t.id IN :ids")
})
public class TopologyLogicalRequestEntity {
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "request_id", nullable = false, insertable = false, updatable = false, length = 10)
  private Long topologyRequestId;

  @Column(name = "description", length = 1024, nullable = false)
  private String description;

  @OneToOne
  @JoinColumn(name = "request_id", referencedColumnName = "id", nullable = false)
  private TopologyRequestEntity topologyRequestEntity;

  @OneToMany(mappedBy = "topologyLogicalRequestEntity", cascade = CascadeType.ALL)
  private Collection<TopologyHostRequestEntity> topologyHostRequestEntities;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getTopologyRequestId() {
    return topologyRequestId;
  }

  public void setTopologyRequestId(Long topologyRequestId) {
    this.topologyRequestId = topologyRequestId;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public TopologyRequestEntity getTopologyRequestEntity() {
    return topologyRequestEntity;
  }

  public void setTopologyRequestEntity(TopologyRequestEntity topologyRequestEntity) {
    this.topologyRequestEntity = topologyRequestEntity;
  }

  public Collection<TopologyHostRequestEntity> getTopologyHostRequestEntities() {
    return topologyHostRequestEntities;
  }

  public void setTopologyHostRequestEntities(Collection<TopologyHostRequestEntity> topologyHostRequestEntities) {
    this.topologyHostRequestEntities = topologyHostRequestEntities;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TopologyLogicalRequestEntity that = (TopologyLogicalRequestEntity) o;

    if (!id.equals(that.id)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
