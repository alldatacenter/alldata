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
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@IdClass(ConfigGroupHostMappingEntityPK.class)
@Entity
@Table(name = "configgrouphostmapping")
@NamedQueries({
  @NamedQuery(name = "groupsByHost", query =
  "SELECT confighosts FROM ConfigGroupHostMappingEntity confighosts " +
    "WHERE confighosts.hostEntity.hostName=:hostname"),
  @NamedQuery(name = "hostsByGroup", query =
  "SELECT confighosts FROM ConfigGroupHostMappingEntity confighosts " +
    "WHERE confighosts.configGroupId=:groupId")
})
public class ConfigGroupHostMappingEntity {

  @Id
  @Column(name = "config_group_id", nullable = false, insertable = true, updatable = true)
  private Long configGroupId;

  @Id
  @Column(name = "host_id", nullable = false, insertable = true, updatable = true)
  private Long hostId;

  @ManyToOne
  @JoinColumns({
    @JoinColumn(name = "host_id", referencedColumnName = "host_id", nullable = false, insertable = false, updatable = false) })
  private HostEntity hostEntity;

  @ManyToOne
  @JoinColumns({
    @JoinColumn(name = "config_group_id", referencedColumnName = "group_id", nullable = false, insertable = false, updatable = false) })
  private ConfigGroupEntity configGroupEntity;

  public Long getConfigGroupId() {
    return configGroupId;
  }

  public void setConfigGroupId(Long configGroupId) {
    this.configGroupId = configGroupId;
  }

  public Long getHostId() {
    return hostId;
  }

  public void setHostId(Long hostId) {
    this.hostId = hostId;
  }

  public String getHostname() {
    return hostEntity != null ? hostEntity.getHostName() : null;
  }

  public HostEntity getHostEntity() {
    return hostEntity;
  }

  public void setHostEntity(HostEntity hostEntity) {
    this.hostEntity = hostEntity;
  }

  public ConfigGroupEntity getConfigGroupEntity() {
    return configGroupEntity;
  }

  public void setConfigGroupEntity(ConfigGroupEntity configGroupEntity) {
    this.configGroupEntity = configGroupEntity;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ConfigGroupHostMappingEntity that = (ConfigGroupHostMappingEntity) o;

    if (!configGroupId.equals(that.configGroupId)) return false;
    if (!hostEntity.equals(that.hostEntity)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = configGroupId.hashCode();
    result = 31 * result + hostEntity.hashCode();
    return result;
  }
}
