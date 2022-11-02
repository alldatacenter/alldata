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

@Table(name = "configgroup")
@Entity
@NamedQueries({
  @NamedQuery(name = "configGroupByName", query =
    "SELECT configgroup " +
      "FROM ConfigGroupEntity configgroup " +
      "WHERE configgroup.groupName=:groupName"),
  @NamedQuery(name = "allConfigGroups", query =
    "SELECT configgroup " +
      "FROM ConfigGroupEntity configgroup"),
  @NamedQuery(name = "configGroupsByTag", query =
    "SELECT configgroup FROM ConfigGroupEntity configgroup " +
    "WHERE configgroup.tag=:tagName")
})
@TableGenerator(name = "configgroup_id_generator",
  table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value"
  , pkColumnValue = "configgroup_id_seq"
  , initialValue = 1
)
public class ConfigGroupEntity {
  @Id
  @Column(name = "group_id", nullable = false, insertable = true, updatable = true)
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "configgroup_id_generator")
  private Long groupId;

  @Column(name = "cluster_id", insertable = false, updatable = false, nullable = false)
  private Long clusterId;

  @Column(name = "group_name", nullable = false, unique = true, updatable = true)
  private String groupName;

  @Column(name = "tag", nullable = false)
  private String tag;

  @Column(name = "description")
  private String description;

  @Column(name = "create_timestamp", nullable=false, insertable=true, updatable=false)
  private long timestamp;

  @Column(name = "service_name")
  private String serviceName;

  @ManyToOne
  @JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false)
  private ClusterEntity clusterEntity;

  @OneToMany(mappedBy = "configGroupEntity", cascade = CascadeType.ALL)
  private Collection<ConfigGroupHostMappingEntity> configGroupHostMappingEntities;

  @OneToMany(mappedBy = "configGroupEntity", cascade = CascadeType.ALL)
  private Collection<ConfigGroupConfigMappingEntity> configGroupConfigMappingEntities;

  public Long getGroupId() {
    return groupId;
  }

  public void setGroupId(Long groupId) {
    this.groupId = groupId;
  }

  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public ClusterEntity getClusterEntity() {
    return clusterEntity;
  }

  public void setClusterEntity(ClusterEntity clusterEntity) {
    this.clusterEntity = clusterEntity;
  }

  public Collection<ConfigGroupHostMappingEntity> getConfigGroupHostMappingEntities() {
    return configGroupHostMappingEntities;
  }

  public void setConfigGroupHostMappingEntities(Collection<ConfigGroupHostMappingEntity> configGroupHostMappingEntities) {
    this.configGroupHostMappingEntities = configGroupHostMappingEntities;
  }

  public Collection<ConfigGroupConfigMappingEntity> getConfigGroupConfigMappingEntities() {
    return configGroupConfigMappingEntities;
  }

  public void setConfigGroupConfigMappingEntities(Collection<ConfigGroupConfigMappingEntity> configGroupConfigMappingEntities) {
    this.configGroupConfigMappingEntities = configGroupConfigMappingEntities;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ConfigGroupEntity that = (ConfigGroupEntity) o;

    if (!clusterId.equals(that.clusterId)) return false;
    if (!groupId.equals(that.groupId)) return false;
    if (!groupName.equals(that.groupName)) return false;
    if (!tag.equals(that.tag)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = groupId.hashCode();
    result = 31 * result + clusterId.hashCode();
    result = 31 * result + groupName.hashCode();
    result = 31 * result + tag.hashCode();
    return result;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }
}
