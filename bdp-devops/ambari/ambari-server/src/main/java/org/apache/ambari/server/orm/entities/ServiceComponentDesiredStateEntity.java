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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;

import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.State;
import org.apache.commons.lang.builder.EqualsBuilder;

@Entity
@Table(
    name = "servicecomponentdesiredstate",
    uniqueConstraints = @UniqueConstraint(
        name = "unq_scdesiredstate_name",
        columnNames = { "component_name", "service_name", "cluster_id" }) )
@TableGenerator(
    name = "servicecomponentdesiredstate_id_generator",
    table = "ambari_sequences",
    pkColumnName = "sequence_name",
    valueColumnName = "sequence_value",
    pkColumnValue = "servicecomponentdesiredstate_id_seq",
    initialValue = 0)
@NamedQueries({
 @NamedQuery(
    name = "ServiceComponentDesiredStateEntity.findByName",
    query = "SELECT scds FROM ServiceComponentDesiredStateEntity scds WHERE scds.clusterId = :clusterId AND scds.serviceName = :serviceName AND scds.componentName = :componentName") })
public class ServiceComponentDesiredStateEntity {

  @Id
  @Column(name = "id", nullable = false, insertable = true, updatable = false)
  @GeneratedValue(
      strategy = GenerationType.TABLE,
      generator = "servicecomponentdesiredstate_id_generator")
  private Long id;

  @Column(name = "cluster_id", nullable = false, insertable = false, updatable = false, length = 10)
  private Long clusterId;

  @Column(name = "service_name", nullable = false, insertable = false, updatable = false)
  private String serviceName;

  @Column(name = "component_name", nullable = false, insertable = true, updatable = true)
  private String componentName;

  @Column(name = "desired_state", nullable = false, insertable = true, updatable = true)
  @Enumerated(EnumType.STRING)
  private State desiredState = State.INIT;

  @Column(name = "recovery_enabled", nullable = false, insertable = true, updatable = true)
  private Integer recoveryEnabled = 0;

  @Column(name = "repo_state", nullable = false, insertable = true, updatable = true)
  @Enumerated(EnumType.STRING)
  private RepositoryVersionState repoState = RepositoryVersionState.NOT_REQUIRED;

  /**
   * Unidirectional one-to-one association to {@link RepositoryVersionEntity}
   */
  @OneToOne
  @JoinColumn(
      name = "desired_repo_version_id",
      unique = false,
      nullable = false,
      insertable = true,
      updatable = true)
  private RepositoryVersionEntity desiredRepositoryVersion;

  @ManyToOne
  @JoinColumns({@javax.persistence.JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false), @JoinColumn(name = "service_name", referencedColumnName = "service_name", nullable = false)})
  private ClusterServiceEntity clusterServiceEntity;

  @OneToMany(mappedBy = "serviceComponentDesiredStateEntity")
  private Collection<HostComponentStateEntity> hostComponentStateEntities;

  @OneToMany(mappedBy = "serviceComponentDesiredStateEntity")
  private Collection<HostComponentDesiredStateEntity> hostComponentDesiredStateEntities;

  @OneToMany(mappedBy = "m_serviceComponentDesiredStateEntity", cascade = { CascadeType.ALL })
  private Collection<ServiceComponentVersionEntity> serviceComponentVersions;

  public Long getId() {
    return id;
  }

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

  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public State getDesiredState() {
    return desiredState;
  }

  public void setDesiredState(State desiredState) {
    this.desiredState = desiredState;
  }

  public RepositoryVersionEntity getDesiredRepositoryVersion() {
    return desiredRepositoryVersion;
  }

  public void setDesiredRepositoryVersion(RepositoryVersionEntity desiredRepositoryVersion) {
    this.desiredRepositoryVersion = desiredRepositoryVersion;
  }

  public StackEntity getDesiredStack() {
    return desiredRepositoryVersion.getStack();
  }

  public String getDesiredVersion() {
    return desiredRepositoryVersion.getVersion();
  }

  /**
   * @param versionEntity the version to add
   */
  public void addVersion(ServiceComponentVersionEntity versionEntity) {
    if (null == serviceComponentVersions) {
      serviceComponentVersions = new ArrayList<>();
    }

    serviceComponentVersions.add(versionEntity);
    versionEntity.setServiceComponentDesiredState(this);
  }

  /**
   * @return the collection of versions for the component
   */
  public Collection<ServiceComponentVersionEntity> getVersions() {
    return serviceComponentVersions;
  }


  public boolean isRecoveryEnabled() {
    return recoveryEnabled != 0;
  }

  public void setRecoveryEnabled(boolean recoveryEnabled) {
    this.recoveryEnabled = (recoveryEnabled == false) ? 0 : 1;
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

    ServiceComponentDesiredStateEntity that = (ServiceComponentDesiredStateEntity) o;
    EqualsBuilder equalsBuilder = new EqualsBuilder();
    equalsBuilder.append(id, that.id);
    equalsBuilder.append(clusterId, that.clusterId);
    equalsBuilder.append(componentName, that.componentName);
    equalsBuilder.append(desiredState, that.desiredState);
    equalsBuilder.append(serviceName, that.serviceName);
    equalsBuilder.append(desiredRepositoryVersion, that.desiredRepositoryVersion);

    return equalsBuilder.isEquals();
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, clusterId, serviceName, componentName, desiredState,
        desiredRepositoryVersion);
  }

  public ClusterServiceEntity getClusterServiceEntity() {
    return clusterServiceEntity;
  }

  public void setClusterServiceEntity(ClusterServiceEntity clusterServiceEntity) {
    this.clusterServiceEntity = clusterServiceEntity;
  }

  public Collection<HostComponentStateEntity> getHostComponentStateEntities() {
    return hostComponentStateEntities;
  }

  public void setHostComponentStateEntities(Collection<HostComponentStateEntity> hostComponentStateEntities) {
    this.hostComponentStateEntities = hostComponentStateEntities;
  }

  public Collection<HostComponentDesiredStateEntity> getHostComponentDesiredStateEntities() {
    return hostComponentDesiredStateEntities;
  }

  public void setHostComponentDesiredStateEntities(Collection<HostComponentDesiredStateEntity> hostComponentDesiredStateEntities) {
    this.hostComponentDesiredStateEntities = hostComponentDesiredStateEntities;
  }

  /**
   * @param state the repository state for {@link #getDesiredVersion()}
   */
  public void setRepositoryState(RepositoryVersionState state) {
    repoState = state;
  }

  /**
   * @return the state of the repository for {@link #getDesiredVersion()}
   */
  public RepositoryVersionState getRepositoryState() {
    return repoState;
  }

}
