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

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

@Entity
@Table(name = "topology_hostgroup")
@NamedQueries({
  @NamedQuery(name = "TopologyHostGroupEntity.findByRequestIdAndName",
    query = "SELECT req FROM TopologyHostGroupEntity req WHERE req.topologyRequestEntity.id = :requestId AND req.name = :name")
})
@TableGenerator(name = "topology_host_group_id_generator", table = "ambari_sequences",
  pkColumnName = "sequence_name", valueColumnName = "sequence_value",
  pkColumnValue = "topology_host_group_id_seq", initialValue = 0)
public class TopologyHostGroupEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "topology_host_group_id_generator")
  @Column(name = "id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "name", nullable = false, updatable = false)
  private String name;

  @Column(name = "group_properties")
  @Basic(fetch = FetchType.LAZY)
  @Lob
  private String groupProperties;

  @Column(name = "group_attributes")
  @Basic(fetch = FetchType.LAZY)
  @Lob
  private String groupAttributes;

  @ManyToOne
  @JoinColumn(name = "request_id", referencedColumnName = "id", nullable = false)
  private TopologyRequestEntity topologyRequestEntity;

  @OneToMany(mappedBy = "topologyHostGroupEntity", cascade = CascadeType.ALL)
  private Collection<TopologyHostInfoEntity> topologyHostInfoEntities;

  @OneToMany(mappedBy = "topologyHostGroupEntity", cascade = CascadeType.ALL)
  private Collection<TopologyHostRequestEntity> topologyHostRequestEntities;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getGroupProperties() {
    return groupProperties;
  }

  public void setGroupProperties(String groupProperties) {
    this.groupProperties = groupProperties;
  }

  public String getGroupAttributes() {
    return groupAttributes;
  }

  public void setGroupAttributes(String groupAttributes) {
    this.groupAttributes = groupAttributes;
  }

  public Long getRequestId() {
    return topologyRequestEntity != null ? topologyRequestEntity.getId() : null;
  }

  public TopologyRequestEntity getTopologyRequestEntity() {
    return topologyRequestEntity;
  }

  public void setTopologyRequestEntity(TopologyRequestEntity topologyRequestEntity) {
    this.topologyRequestEntity = topologyRequestEntity;
  }

  public Collection<TopologyHostInfoEntity> getTopologyHostInfoEntities() {
    return topologyHostInfoEntities;
  }

  public void setTopologyHostInfoEntities(Collection<TopologyHostInfoEntity> topologyHostInfoEntities) {
    this.topologyHostInfoEntities = topologyHostInfoEntities;
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

    TopologyHostGroupEntity that = (TopologyHostGroupEntity) o;

    if (!name.equals(that.name)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }
}
