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
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

@Entity
@Table(name = "topology_host_info")
@TableGenerator(name = "topology_host_info_id_generator", table = "ambari_sequences",
  pkColumnName = "sequence_name", valueColumnName = "sequence_value",
  pkColumnValue = "topology_host_info_id_seq", initialValue = 0)
public class TopologyHostInfoEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "topology_host_info_id_generator")
  @Column(name = "id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "fqdn", length = 255)
  private String fqdn;

  @OneToOne
  @JoinColumn(name="host_id")
  private HostEntity hostEntity;

  @Column(name = "host_count", length = 10)
  private Integer hostCount;

  @Column(name = "predicate", length = 2048)
  private String predicate;

  @ManyToOne
  @JoinColumn(name = "group_id", referencedColumnName = "id", nullable = false)
  private TopologyHostGroupEntity topologyHostGroupEntity;

  @Column(name = "rack_info", length = 255)
  private String rackInfo;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getGroupId() {
    return topologyHostGroupEntity.getId();
  }

  public String getFqdn() {
    return fqdn;
  }

  public void setFqdn(String fqdn) {
    this.fqdn = fqdn;
  }

  public Integer getHostCount() {
    return hostCount;
  }

  public void setHostCount(Integer hostCount) {
    this.hostCount = hostCount;
  }

  public String getPredicate() {
    return predicate;
  }

  public void setPredicate(String predicate) {
    this.predicate = predicate;
  }

  public TopologyHostGroupEntity getTopologyHostGroupEntity() {
    return topologyHostGroupEntity;
  }

  public void setTopologyHostGroupEntity(TopologyHostGroupEntity topologyHostGroupEntity) {
    this.topologyHostGroupEntity = topologyHostGroupEntity;
  }

  public String getRackInfo() {
    return rackInfo;
  }

  public void setRackInfo(String rackInfo) {
    this.rackInfo = rackInfo;
  }

  public HostEntity getHostEntity() {
    return hostEntity;
  }

  public void setHostEntity(HostEntity hostEntity) {
    this.hostEntity = hostEntity;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TopologyHostInfoEntity that = (TopologyHostInfoEntity) o;

    if (!id.equals(that.id)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
