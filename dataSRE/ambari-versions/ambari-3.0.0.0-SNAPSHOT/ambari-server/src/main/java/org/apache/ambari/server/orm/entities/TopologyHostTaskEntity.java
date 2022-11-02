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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

@Entity
@Table(name = "topology_host_task")
@TableGenerator(name = "topology_host_task_id_generator", table = "ambari_sequences",
  pkColumnName = "sequence_name", valueColumnName = "sequence_value",
  pkColumnValue = "topology_host_task_id_seq", initialValue = 0)
@NamedQueries({
  @NamedQuery(name = "TopologyHostTaskEntity.findByHostRequest",
      query = "SELECT req FROM TopologyHostTaskEntity req WHERE req.topologyHostRequestEntity.id = :hostRequestId"),
  @NamedQuery(name = "TopologyLogicalTaskEntity.findHostRequestIdsByHostTaskIds",
      query = "SELECT DISTINCT tht.hostRequestId from TopologyHostTaskEntity tht WHERE tht.id IN :hostTaskIds"),
  @NamedQuery(name = "TopologyHostTaskEntity.removeByTaskIds",
      query = "DELETE FROM TopologyHostTaskEntity tht WHERE tht.id IN :hostTaskIds")
})
public class TopologyHostTaskEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "topology_host_task_id_generator")
  @Column(name = "id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "type", length = 255, nullable = false)
  private String type;

  @Column(name = "host_request_id", nullable = false, insertable = false, updatable = false)
  private Long hostRequestId;

  @ManyToOne
  @JoinColumn(name = "host_request_id", referencedColumnName = "id", nullable = false)
  private TopologyHostRequestEntity topologyHostRequestEntity;

  @OneToMany(mappedBy = "topologyHostTaskEntity", cascade = CascadeType.ALL)
  private Collection<TopologyLogicalTaskEntity> topologyLogicalTaskEntities;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getHostRequestId() {
    return hostRequestId;
  }

  public void setHostRequestId(Long hostRequestId) {
    this.hostRequestId = hostRequestId;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public TopologyHostRequestEntity getTopologyHostRequestEntity() {
    return topologyHostRequestEntity;
  }

  public void setTopologyHostRequestEntity(TopologyHostRequestEntity topologyHostRequestEntity) {
    this.topologyHostRequestEntity = topologyHostRequestEntity;
  }

  public Collection<TopologyLogicalTaskEntity> getTopologyLogicalTaskEntities() {
    return topologyLogicalTaskEntities;
  }

  public void setTopologyLogicalTaskEntities(Collection<TopologyLogicalTaskEntity> topologyLogicalTaskEntities) {
    this.topologyLogicalTaskEntities = topologyLogicalTaskEntities;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TopologyHostTaskEntity that = (TopologyHostTaskEntity) o;

    if (!id.equals(that.id)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
