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

@Entity
@Table(name = "confgroupclusterconfigmapping")
@IdClass(ConfigGroupConfigMappingEntityPK.class)
@NamedQueries({
  @NamedQuery(name = "configsByGroup", query =
  "SELECT configs FROM ConfigGroupConfigMappingEntity configs " +
    "WHERE configs.configGroupId=:groupId")
})
public class ConfigGroupConfigMappingEntity {
  @Id
  @Column(name = "config_group_id", nullable = false, insertable = true, updatable = true)
  private Long configGroupId;

  @Id
  @Column(name = "cluster_id", nullable = false, insertable = true, updatable = false)
  private Long clusterId;

  @Id
  @Column(name = "config_type", nullable = false, insertable = true, updatable = false)
  private String configType;

  @Column(name = "version_tag", nullable = false, insertable = true, updatable = false)
  private String versionTag;

  @Column(name = "create_timestamp", nullable = false, insertable = true, updatable = true)
  private Long timestamp;

  @ManyToOne
  @JoinColumns({
    @JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false, insertable = false, updatable = false),
    @JoinColumn(name = "config_type", referencedColumnName = "type_name", nullable = false, insertable = false, updatable = false),
    @JoinColumn(name = "version_tag", referencedColumnName = "version_tag", nullable = false, insertable = false, updatable = false)
  })
  private ClusterConfigEntity clusterConfigEntity;

  @ManyToOne
  @JoinColumns({
    @JoinColumn(name = "config_group_id", referencedColumnName = "group_id", nullable = false, insertable = false, updatable = false)})
  private ConfigGroupEntity configGroupEntity;

  public Long getConfigGroupId() {
    return configGroupId;
  }

  public void setConfigGroupId(Long configGroupId) {
    this.configGroupId = configGroupId;
  }

  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  public String getConfigType() {
    return configType;
  }

  public void setConfigType(String configType) {
    this.configType = configType;
  }

  public String getVersionTag() {
    return versionTag;
  }

  public void setVersionTag(String versionTag) {
    this.versionTag = versionTag;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
  }

  public ClusterConfigEntity getClusterConfigEntity() {
    return clusterConfigEntity;
  }

  public void setClusterConfigEntity(ClusterConfigEntity clusterConfigEntity) {
    this.clusterConfigEntity = clusterConfigEntity;
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

    ConfigGroupConfigMappingEntity that = (ConfigGroupConfigMappingEntity) o;

    if (!clusterId.equals(that.clusterId)) return false;
    if (!configGroupId.equals(that.configGroupId)) return false;
    if (!configType.equals(that.configType)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = configGroupId.hashCode();
    result = 31 * result + clusterId.hashCode();
    result = 31 * result + configType.hashCode();
    return result;
  }
}
