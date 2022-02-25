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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.apache.ambari.server.state.RepositoryVersionState;

/**
 * The {@link ServiceComponentVersionEntity} class is used to represent the
 * association of a component and repository version.
 */
@Entity
@Table(name = "servicecomponent_version")
@TableGenerator(
    name = "servicecomponent_version_id_generator",
    table = "ambari_sequences",
    pkColumnName = "sequence_name",
    valueColumnName = "sequence_value",
    pkColumnValue = "servicecomponent_version_id_seq",
    initialValue = 0)
@NamedQueries({
  @NamedQuery(
    name = "ServiceComponentVersionEntity.findByComponent",
    query = "SELECT version FROM ServiceComponentVersionEntity version WHERE " +
      "version.m_serviceComponentDesiredStateEntity.clusterId = :clusterId AND " +
      "version.m_serviceComponentDesiredStateEntity.serviceName = :serviceName AND " +
      "version.m_serviceComponentDesiredStateEntity.componentName = :componentName"),
  @NamedQuery(
    name = "ServiceComponentVersionEntity.findByComponentAndVersion",
    query = "SELECT version FROM ServiceComponentVersionEntity version WHERE " +
        "version.m_serviceComponentDesiredStateEntity.clusterId = :clusterId AND " +
        "version.m_serviceComponentDesiredStateEntity.serviceName = :serviceName AND " +
        "version.m_serviceComponentDesiredStateEntity.componentName = :componentName AND " +
        "version.m_repositoryVersion.version = :repoVersion")
})

public class ServiceComponentVersionEntity {

  @Id
  @GeneratedValue(
      strategy = GenerationType.TABLE,
      generator = "servicecomponent_version_id_generator")
  @Column(name = "id", nullable = false, updatable = false)
  private long m_id;

  @ManyToOne(optional = false, cascade = { CascadeType.MERGE })
  @JoinColumn(name = "component_id", referencedColumnName = "id", nullable = false)
  private ServiceComponentDesiredStateEntity m_serviceComponentDesiredStateEntity;

  @ManyToOne
  @JoinColumn(name  = "repo_version_id", referencedColumnName = "repo_version_id", nullable = false)
  private RepositoryVersionEntity m_repositoryVersion;

  @Column(name = "state", nullable = false, insertable = true, updatable = true)
  @Enumerated(value = EnumType.STRING)
  private RepositoryVersionState m_state = RepositoryVersionState.CURRENT;

  @Column(name = "user_name", nullable = false, insertable=true, updatable=true)
  private String userName;


  /**
   * @return the associated component
   */
  public ServiceComponentDesiredStateEntity getServiceComponentDesiredState() {
    return m_serviceComponentDesiredStateEntity;
  }

  /**
   * @param serviceComponentDesiredStateEntity  the associated component (not {@code null})
   */
  public void setServiceComponentDesiredState(ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity) {
    m_serviceComponentDesiredStateEntity = serviceComponentDesiredStateEntity;
  }

  /**
   * @param repositoryVersion the repository
   */
  public void setRepositoryVersion(RepositoryVersionEntity repositoryVersion) {
    m_repositoryVersion = repositoryVersion;
  }

  /**
   * @return the repository
   */
  public RepositoryVersionEntity getRepositoryVersion() {
    return m_repositoryVersion;
  }

  /**
   * @return the id
   */
  public long getId() {
    return m_id;
  }

  /**
   * @return the state of the repository
   */
  public RepositoryVersionState getState() {
    return m_state;
  }

  /**
   * @param state the state of the repository
   */
  public void setState(RepositoryVersionState state) {
    m_state = state;
  }

  /**
   * @return the user name
   */
  public String getUserName() {
    return userName;
  }

  /**
   * @param name  the user name
   */
  public void setUserName(String name) {
    userName = name;
  }

  @Override
  public int hashCode() {
    return Objects.hash(m_id, m_repositoryVersion, m_serviceComponentDesiredStateEntity, m_state);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final ServiceComponentVersionEntity other = (ServiceComponentVersionEntity) obj;

    return Objects.equals(m_id, other.m_id)
        && Objects.equals(m_repositoryVersion, other.m_repositoryVersion)
        && Objects.equals(m_serviceComponentDesiredStateEntity, other.m_serviceComponentDesiredStateEntity)
        && Objects.equals(m_state, other.m_state);
  }

}
