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

import static org.apache.commons.lang.StringUtils.defaultString;

import java.util.Collection;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.State;

@Table(name = "clusters")
@NamedQueries({
    @NamedQuery(name = "clusterByName", query =
        "SELECT cluster " +
            "FROM ClusterEntity cluster " +
            "WHERE cluster.clusterName=:clusterName"),
    @NamedQuery(name = "allClusters", query =
        "SELECT clusters " +
            "FROM ClusterEntity clusters"),
    @NamedQuery(name = "clusterByResourceId", query =
        "SELECT cluster " +
            "FROM ClusterEntity cluster " +
            "WHERE cluster.resource.id=:resourceId")
})
@Entity
@TableGenerator(name = "cluster_id_generator",
    table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value"
    , pkColumnValue = "cluster_id_seq"
    , initialValue = 1
)
public class ClusterEntity {

  @Id
  @Column(name = "cluster_id", nullable = false, insertable = true, updatable = true)
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "cluster_id_generator")
  private Long clusterId;

  @Basic
  @Column(name = "cluster_name", nullable = false, insertable = true,
      updatable = true, unique = true, length = 100)
  private String clusterName;

  @Basic
  @Enumerated(value = EnumType.STRING)
  @Column(name = "provisioning_state", insertable = true, updatable = true)
  private State provisioningState = State.INIT;

  @Basic
  @Enumerated(value = EnumType.STRING)
  @Column(name = "security_type", nullable = false, insertable = true, updatable = true)
  private SecurityType securityType = SecurityType.NONE;

  @Basic
  @Column(name = "desired_cluster_state", insertable = true, updatable = true)
  private String desiredClusterState = "";

  @Basic
  @Column(name = "cluster_info", insertable = true, updatable = true)
  private String clusterInfo = "";

  /**
   * Unidirectional one-to-one association to {@link StackEntity}
   */
  @OneToOne
  @JoinColumn(name = "desired_stack_id", unique = false, nullable = false, insertable = true, updatable = true)
  private StackEntity desiredStack;

  @OneToMany(mappedBy = "clusterEntity")
  private Collection<ClusterServiceEntity> clusterServiceEntities;

  @OneToOne(mappedBy = "clusterEntity", cascade = CascadeType.REMOVE)
  private ClusterStateEntity clusterStateEntity;

  @ManyToMany(mappedBy = "clusterEntities")
  private Collection<HostEntity> hostEntities;

  @OneToMany(mappedBy = "clusterEntity", cascade = CascadeType.ALL)
  private Collection<ClusterConfigEntity> configEntities;

  @OneToMany(mappedBy = "clusterEntity", cascade = CascadeType.ALL)
  private Collection<ConfigGroupEntity> configGroupEntities;

  @OneToMany(mappedBy = "clusterEntity", cascade = CascadeType.ALL)
  private Collection<RequestScheduleEntity> requestScheduleEntities;

  @OneToMany(mappedBy = "clusterEntity", cascade = CascadeType.REMOVE)
  private Collection<ServiceConfigEntity> serviceConfigEntities;

  @OneToMany(mappedBy = "clusterEntity", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
  private Collection<AlertDefinitionEntity> alertDefinitionEntities;

  @OneToMany(mappedBy = "clusterEntity", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
  private Collection<WidgetEntity> widgetEntities;

  @OneToMany(mappedBy = "clusterEntity", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
  private Collection<WidgetLayoutEntity> widgetLayoutEntities;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumns({
      @JoinColumn(name = "resource_id", referencedColumnName = "resource_id", nullable = false)
  })
  private ResourceEntity resource;

  @Basic
  @Column(name = "upgrade_id", nullable = true, insertable = false, updatable = false)
  private Long upgradeId;

  /**
   * {@code null} when there is no upgrade/downgrade in progress.
   */
  @OneToOne(cascade = CascadeType.REMOVE)
  @JoinColumn(
      name = "upgrade_id",
      referencedColumnName = "upgrade_id",
      nullable = true,
      insertable = false,
      updatable = true)
  private UpgradeEntity upgradeEntity = null;

  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getDesiredClusterState() {
    return defaultString(desiredClusterState);
  }

  public void setDesiredClusterState(String desiredClusterState) {
    this.desiredClusterState = desiredClusterState;
  }

  public String getClusterInfo() {
    return defaultString(clusterInfo);
  }

  public void setClusterInfo(String clusterInfo) {
    this.clusterInfo = clusterInfo;
  }

  public StackEntity getDesiredStack() {
    return desiredStack;
  }

  public void setDesiredStack(StackEntity desiredStack) {
    this.desiredStack = desiredStack;
  }

  /**
   * Gets whether the cluster is still initializing or has finished with its
   * deployment requests.
   *
   * @return either {@link State#INIT} or {@link State#INSTALLED},
   * never {@code null}.
   */
  public State getProvisioningState(){
    return provisioningState;
  }

  /**
   * Sets whether the cluster is still initializing or has finished with its
   * deployment requests.
   *
   * @param provisioningState either {@link State#INIT} or
   * {@link State#INSTALLED}, never {@code null}.
   */
  public void setProvisioningState(State provisioningState){
    this.provisioningState = provisioningState;
  }

  /**
   * Gets this ClusterEntity's security type.
   *
   * @return the current SecurityType
   */
  public SecurityType getSecurityType() {
    return securityType;
  }

  /**
   * Set's this ClusterEntity's security type
   *
   * @param securityType the new SecurityType
   */
  public void setSecurityType(SecurityType securityType) {
    this.securityType = securityType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ClusterEntity that = (ClusterEntity) o;

    if (!clusterId.equals(that.clusterId)) {
      return false;
    }
    if (!clusterName.equals(that.clusterName)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = null == clusterId ? 0 : clusterId.hashCode();
    result = 31 * result + clusterName.hashCode();
    return result;
  }

  public Collection<ClusterServiceEntity> getClusterServiceEntities() {
    return clusterServiceEntities;
  }

  public void setClusterServiceEntities(Collection<ClusterServiceEntity> clusterServiceEntities) {
    this.clusterServiceEntities = clusterServiceEntities;
  }

  public ClusterStateEntity getClusterStateEntity() {
    return clusterStateEntity;
  }

  public void setClusterStateEntity(ClusterStateEntity clusterStateEntity) {
    this.clusterStateEntity = clusterStateEntity;
  }

  public Collection<HostEntity> getHostEntities() {
    return hostEntities;
  }

  public void setHostEntities(Collection<HostEntity> hostEntities) {
    this.hostEntities = hostEntities;
  }

  public Collection<ClusterConfigEntity> getClusterConfigEntities() {
    return configEntities;
  }

  public void setClusterConfigEntities(Collection<ClusterConfigEntity> entities) {
    configEntities = entities;
  }

  public Collection<ConfigGroupEntity> getConfigGroupEntities() {
    return configGroupEntities;
  }

  public void setConfigGroupEntities(Collection<ConfigGroupEntity> configGroupEntities) {
    this.configGroupEntities = configGroupEntities;
  }

  public Collection<RequestScheduleEntity> getRequestScheduleEntities() {
    return requestScheduleEntities;
  }

  public void setRequestScheduleEntities(Collection<RequestScheduleEntity> requestScheduleEntities) {
    this.requestScheduleEntities = requestScheduleEntities;
  }

  public Collection<ServiceConfigEntity> getServiceConfigEntities() {
    return serviceConfigEntities;
  }

  public void setServiceConfigEntities(Collection<ServiceConfigEntity> serviceConfigEntities) {
    this.serviceConfigEntities = serviceConfigEntities;
  }

  public Collection<AlertDefinitionEntity> getAlertDefinitionEntities() {
    return alertDefinitionEntities;
  }

  /**
   * Get the admin resource entity.
   *
   * @return the resource entity
   */
  public ResourceEntity getResource() {
    return resource;
  }

  /**
   * Set the admin resource entity.
   *
   * @param resource  the resource entity
   */
  public void setResource(ResourceEntity resource) {
    this.resource = resource;
  }

  public UpgradeEntity getUpgradeEntity() {
    return upgradeEntity;
  }

  public void setUpgradeEntity(UpgradeEntity upgradeEntity) {
    this.upgradeEntity = upgradeEntity;
  }
}
