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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

@Entity
@Table(name = "topology_logical_task")
@TableGenerator(name = "topology_logical_task_id_generator", table = "ambari_sequences",
  pkColumnName = "sequence_name", valueColumnName = "sequence_value",
  pkColumnValue = "topology_logical_task_id_seq", initialValue = 0)
@NamedQueries({
  @NamedQuery(name = "TopologyLogicalTaskEntity.findHostTaskIdsByPhysicalTaskIds", query = "SELECT DISTINCT logicaltask.hostTaskId from TopologyLogicalTaskEntity logicaltask WHERE logicaltask.physicalTaskId IN :physicalTaskIds"),
  @NamedQuery(name = "TopologyLogicalTaskEntity.removeByPhysicalTaskIds", query = "DELETE FROM TopologyLogicalTaskEntity logicaltask WHERE logicaltask.physicalTaskId IN :taskIds")
})
public class TopologyLogicalTaskEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "topology_logical_task_id_generator")
  @Column(name = "id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "component", length = 255)
  private String componentName;

  @Column(name = "host_task_id", nullable = false, insertable = false, updatable = false)
  private Long hostTaskId;

  @Column(name = "physical_task_id", nullable = false, insertable = false, updatable = false)
  private Long physicalTaskId;

  @ManyToOne
  @JoinColumn(name = "host_task_id", referencedColumnName = "id", nullable = false)
  private TopologyHostTaskEntity topologyHostTaskEntity;

  @OneToOne
  @JoinColumn(name = "physical_task_id", referencedColumnName = "task_id", nullable = false)
  private HostRoleCommandEntity hostRoleCommandEntity;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getPhysicalTaskId() {
    return hostRoleCommandEntity != null ? hostRoleCommandEntity.getTaskId() : null;
  }

  public void setPhysicalTaskId(Long physicalTaskId) {
    this.physicalTaskId = physicalTaskId;
  }

  public void setHostTaskId(Long hostTaskId) {
    this.hostTaskId = hostTaskId;
  }

  public Long getHostTaskId() {
    return hostTaskId;
  }

  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public TopologyHostTaskEntity getTopologyHostTaskEntity() {
    return topologyHostTaskEntity;
  }

  public void setTopologyHostTaskEntity(TopologyHostTaskEntity topologyHostTaskEntity) {
    this.topologyHostTaskEntity = topologyHostTaskEntity;
  }

  public HostRoleCommandEntity getHostRoleCommandEntity() {
    return hostRoleCommandEntity;
  }

  public void setHostRoleCommandEntity(HostRoleCommandEntity hostRoleCommandEntity) {
    this.hostRoleCommandEntity = hostRoleCommandEntity;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TopologyLogicalTaskEntity that = (TopologyLogicalTaskEntity) o;

    if (!id.equals(that.id)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
