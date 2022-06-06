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

import java.util.Objects;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.State;
import org.apache.commons.lang.builder.EqualsBuilder;

@javax.persistence.IdClass(ServiceDesiredStateEntityPK.class)
@javax.persistence.Table(name = "servicedesiredstate")
@Entity
public class ServiceDesiredStateEntity {

  @Id
  @Column(name = "cluster_id", nullable = false, insertable = false, updatable = false, length = 10)  
  private Long clusterId;

  @Id
  @Column(name = "service_name", nullable = false, insertable = false, updatable = false)
  private String serviceName;

  @Column(name = "desired_state", nullable = false, insertable = true, updatable = true)
  @Enumerated(value = EnumType.STRING)
  private State desiredState = State.INIT;

  @Basic
  @Column(name = "desired_host_role_mapping", nullable = false, insertable = true, updatable = true, length = 10)  
  private int desiredHostRoleMapping = 0;

  @Column(name = "maintenance_state", nullable = false, insertable = true, updatable = true)
  @Enumerated(value = EnumType.STRING)
  private MaintenanceState maintenanceState = MaintenanceState.OFF;

  @Column(name = "credential_store_enabled", nullable = false, insertable = true, updatable = true)
  private short credentialStoreEnabled = 0;

  @OneToOne
  @javax.persistence.JoinColumns(
      {
          @JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false),
          @JoinColumn(name = "service_name", referencedColumnName = "service_name", nullable = false)
      })
  private ClusterServiceEntity clusterServiceEntity;

  /**
   * The desired repository that the service should be on.
   */
  @ManyToOne
  @JoinColumn(name = "desired_repo_version_id", unique = false, nullable = false, insertable = true, updatable = true)
  private RepositoryVersionEntity desiredRepositoryVersion;

  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public State getDesiredState() {
    return desiredState;
  }

  public void setDesiredState(State desiredState) {
    this.desiredState = desiredState;
  }

  public int getDesiredHostRoleMapping() {
    return desiredHostRoleMapping;
  }

  public void setDesiredHostRoleMapping(int desiredHostRoleMapping) {
    this.desiredHostRoleMapping = desiredHostRoleMapping;
  }

  public StackEntity getDesiredStack() {
    return desiredRepositoryVersion.getStack();
  }

  public MaintenanceState getMaintenanceState() {
    return maintenanceState;
  }

  public void setMaintenanceState(MaintenanceState state) {
    maintenanceState = state;
  }

  /**
   * Gets a value indicating if credential store use is enabled or not.
   *
   * @return true or false
   */
  public boolean isCredentialStoreEnabled() {
    return credentialStoreEnabled != 0;
  }

  /**
   * Sets a value indicating if credential store use is enabled or not.
   *
   * @param credentialStoreEnabled
   */
  public void setCredentialStoreEnabled(boolean credentialStoreEnabled) {
    this.credentialStoreEnabled = (short)((credentialStoreEnabled == false) ? 0 : 1);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ServiceDesiredStateEntity that = (ServiceDesiredStateEntity) o;
    EqualsBuilder equalsBuilder = new EqualsBuilder();
    equalsBuilder.append(clusterId, that.clusterId);
    equalsBuilder.append(desiredState, that.desiredState);
    equalsBuilder.append(desiredHostRoleMapping, that.desiredHostRoleMapping);
    equalsBuilder.append(serviceName, that.serviceName);
    equalsBuilder.append(desiredRepositoryVersion, that.desiredRepositoryVersion);

    return equalsBuilder.isEquals();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(clusterId, serviceName, desiredState, desiredHostRoleMapping,
        desiredRepositoryVersion);
  }

  public ClusterServiceEntity getClusterServiceEntity() {
    return clusterServiceEntity;
  }

  public void setClusterServiceEntity(ClusterServiceEntity clusterServiceEntity) {
    this.clusterServiceEntity = clusterServiceEntity;
  }

  /**
   * Gets the desired repository version.
   *
   * @return the desired repository (never {@code null}).
   */
  public RepositoryVersionEntity getDesiredRepositoryVersion() {
    return desiredRepositoryVersion;
  }

  /**
   * Sets the desired repository for this service.
   *
   * @param desiredRepositoryVersion
   *          the desired repository (not {@code null}).
   */
  public void setDesiredRepositoryVersion(RepositoryVersionEntity desiredRepositoryVersion) {
    this.desiredRepositoryVersion = desiredRepositoryVersion;
  }

}
