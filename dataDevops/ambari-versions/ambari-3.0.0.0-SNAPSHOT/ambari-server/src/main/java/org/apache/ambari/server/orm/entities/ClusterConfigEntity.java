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
import java.util.Objects;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang.builder.EqualsBuilder;

import com.google.common.base.MoreObjects;

@Entity
@Table(name = "clusterconfig",
  uniqueConstraints = {@UniqueConstraint(name = "UQ_config_type_tag", columnNames = {"cluster_id", "type_name", "version_tag"}),
    @UniqueConstraint(name = "UQ_config_type_version", columnNames = {"cluster_id", "type_name", "version"})})
@TableGenerator(name = "config_id_generator",
  table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value"
  , pkColumnValue = "config_id_seq"
  , initialValue = 1
)
@NamedQueries({
    @NamedQuery(
        name = "ClusterConfigEntity.findNextConfigVersion",
        query = "SELECT COALESCE(MAX(clusterConfig.version),0) + 1 as nextVersion FROM ClusterConfigEntity clusterConfig WHERE clusterConfig.type=:configType AND clusterConfig.clusterId=:clusterId"),
    @NamedQuery(
        name = "ClusterConfigEntity.findAllConfigsByStack",
        query = "SELECT clusterConfig FROM ClusterConfigEntity clusterConfig WHERE clusterConfig.clusterId=:clusterId AND clusterConfig.stack=:stack"),
    @NamedQuery(
        name = "ClusterConfigEntity.findLatestConfigsByStack",
        query = "SELECT clusterConfig FROM ClusterConfigEntity clusterConfig WHERE clusterConfig.clusterId = :clusterId AND clusterConfig.stack = :stack AND clusterConfig.selectedTimestamp = (SELECT MAX(clusterConfig2.selectedTimestamp) FROM ClusterConfigEntity clusterConfig2 WHERE clusterConfig2.clusterId=:clusterId AND clusterConfig2.stack=:stack AND clusterConfig2.type = clusterConfig.type)"),
    @NamedQuery(
        name = "ClusterConfigEntity.findLatestConfigsByStackWithTypes",
        query = "SELECT clusterConfig FROM ClusterConfigEntity clusterConfig WHERE clusterConfig.type IN :types AND clusterConfig.clusterId = :clusterId AND clusterConfig.stack = :stack AND clusterConfig.selectedTimestamp = (SELECT MAX(clusterConfig2.selectedTimestamp) FROM ClusterConfigEntity clusterConfig2 WHERE clusterConfig2.clusterId=:clusterId AND clusterConfig2.stack=:stack AND clusterConfig2.type = clusterConfig.type)"),
    @NamedQuery(
        name = "ClusterConfigEntity.findNotMappedClusterConfigsToService",
        query = "SELECT clusterConfig FROM ClusterConfigEntity clusterConfig WHERE clusterConfig.serviceConfigEntities IS EMPTY AND clusterConfig.type != 'cluster-env'"),
    @NamedQuery(
        name = "ClusterConfigEntity.findEnabledConfigsByStack",
        query = "SELECT config FROM ClusterConfigEntity config WHERE config.clusterId = :clusterId AND config.selected = 1 AND config.stack = :stack"),
    @NamedQuery(
        name = "ClusterConfigEntity.findEnabledConfigByType",
        query = "SELECT config FROM ClusterConfigEntity config WHERE config.clusterId = :clusterId AND config.selected = 1 and config.type = :type"),
    @NamedQuery(
        name = "ClusterConfigEntity.findEnabledConfigsByTypes",
        query = "SELECT config FROM ClusterConfigEntity config WHERE config.clusterId = :clusterId AND config.selected = 1 and config.type in :types"),
    @NamedQuery(
        name = "ClusterConfigEntity.findEnabledConfigs",
        query = "SELECT config FROM ClusterConfigEntity config WHERE config.clusterId = :clusterId AND config.selected = 1") })

public class ClusterConfigEntity {

  @Id
  @Column(name = "config_id")
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "config_id_generator")
  private Long configId;

  @Column(name = "cluster_id", nullable = false, insertable = false, updatable = false, length = 10)
  private Long clusterId;

  @Column(name = "type_name")
  private String type;

  @Column(name = "version")
  private Long version;

  @Column(name = "version_tag")
  private String tag;

  @Column(name = "selected", insertable = true, updatable = true, nullable = false)
  private int selected = 0;

  @Basic(fetch = FetchType.LAZY)
  @Column(name = "config_data", nullable = false, insertable = true)
  @Lob
  private String configJson;

  @Basic(fetch = FetchType.LAZY)
  @Column(name = "config_attributes", nullable = true, insertable = true)
  @Lob
  private String configAttributesJson;

  @Column(name = "create_timestamp", nullable = false, insertable = true, updatable = false)
  private long timestamp;

  /**
   * The most recent time that this configuration was marked as
   * {@link #selected}. This is useful when configruations are being reverted
   * since a reversion does not create a new instance. Another configuration may
   * technically be newer via its creation date ({@link #timestamp}), however
   * that does not indicate it was the most recently enabled configuration.
   */
  @Column(name = "selected_timestamp", nullable = false, insertable = true, updatable = true)
  private long selectedTimestamp = 0;

  @ManyToOne
  @JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false)
  private ClusterEntity clusterEntity;

  @OneToMany(mappedBy = "clusterConfigEntity")
  private Collection<ConfigGroupConfigMappingEntity> configGroupConfigMappingEntities;

  @ManyToMany(mappedBy = "clusterConfigEntities")
  private Collection<ServiceConfigEntity> serviceConfigEntities;

  @Column(name = "unmapped", nullable = false, insertable = true, updatable = true)
  private short unmapped = 0;

  /**
   * Unidirectional one-to-one association to {@link StackEntity}
   */
  @OneToOne
  @JoinColumn(name = "stack_id", unique = false, nullable = false, insertable = true, updatable = true)
  private StackEntity stack;

  public boolean isUnmapped() {
    return unmapped != 0;
  }

  public void setUnmapped(boolean unmapped) {
    this.unmapped  = (short)(unmapped ? 1 : 0);
  }

  public Long getConfigId() {
    return configId;
  }

  public void setConfigId(Long configId) {
    this.configId = configId;
  }

  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  public String getType() {
    return type;
  }

  public void setType(String typeName) {
    type = typeName;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String versionTag) {
    tag = versionTag;
  }

  public String getData() {
    return configJson;
  }

  public void setData(String data) {
    configJson = data;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long stamp) {
    timestamp = stamp;
  }

  public long getSelectedTimestamp() {
    return selectedTimestamp;
  }

  public String getAttributes() {
    return configAttributesJson;
  }

  public void setAttributes(String attributes) {
    configAttributesJson = attributes;
  }

  /**
   * Gets the cluster configuration's stack.
   *
   * @return the stack.
   */
  public StackEntity getStack() {
    return stack;
  }

  /**
   * Sets the cluster configuration's stack.
   *
   * @param stack
   *          the stack to set for the cluster config (not {@code null}).
   */
  public void setStack(StackEntity stack) {
    this.stack = stack;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }

    if (object == null || getClass() != object.getClass()) {
      return false;
    }

    ClusterConfigEntity that = (ClusterConfigEntity) object;
    EqualsBuilder equalsBuilder = new EqualsBuilder();

    equalsBuilder.append(configId, that.configId);
    equalsBuilder.append(clusterId, that.clusterId);
    equalsBuilder.append(type, that.type);
    equalsBuilder.append(tag, that.tag);
    equalsBuilder.append(stack, that.stack);

    return equalsBuilder.isEquals();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(configId, clusterId, type, tag, stack);
  }

  public ClusterEntity getClusterEntity() {
    return clusterEntity;
  }

  public void setClusterEntity(ClusterEntity clusterEntity) {
    this.clusterEntity = clusterEntity;
  }

  public Collection<ConfigGroupConfigMappingEntity> getConfigGroupConfigMappingEntities() {
    return configGroupConfigMappingEntities;
  }

  public void setConfigGroupConfigMappingEntities(Collection<ConfigGroupConfigMappingEntity> configGroupConfigMappingEntities) {
    this.configGroupConfigMappingEntities = configGroupConfigMappingEntities;
  }


  public Collection<ServiceConfigEntity> getServiceConfigEntities() {
    return serviceConfigEntities;
  }

  public void setServiceConfigEntities(Collection<ServiceConfigEntity> serviceConfigEntities) {
    this.serviceConfigEntities = serviceConfigEntities;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("clusterId", clusterId)
      .add("type", type)
      .add("version", version)
      .add("tag", tag)
      .add("selected", selected == 1)
      .add("selectedTimeStamp", selectedTimestamp)
      .add("created", timestamp)
      .toString();
  }

  public boolean isSelected() {
    return selected == 1;
  }

  public void setSelected(boolean selected) {
    this.selected = selected ? 1 : 0;

    // if this config is being selected, then also update the selected timestamp
    if (selected) {
      selectedTimestamp = System.currentTimeMillis();
    }
  }

}
