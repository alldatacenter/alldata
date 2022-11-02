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
import javax.persistence.UniqueConstraint;

import org.apache.ambari.server.state.RepositoryVersionState;

@Entity
@Table(
    name = "host_version",
    uniqueConstraints = @UniqueConstraint(
        name = "UQ_host_repo",
        columnNames = { "host_id", "repo_version_id" }))
@TableGenerator(
    name = "host_version_id_generator",
    table = "ambari_sequences",
    pkColumnName = "sequence_name",
    valueColumnName = "sequence_value",
    pkColumnValue = "host_version_id_seq",
    initialValue = 0)
@NamedQueries({
    @NamedQuery(name = "hostVersionByClusterAndStackAndVersion", query =
        "SELECT hostVersion FROM HostVersionEntity hostVersion JOIN hostVersion.hostEntity host JOIN host.clusterEntities clusters " +
            "WHERE clusters.clusterName=:clusterName AND hostVersion.repositoryVersion.stack.stackName=:stackName AND hostVersion.repositoryVersion.stack.stackVersion=:stackVersion AND hostVersion.repositoryVersion.version=:version"),

    @NamedQuery(name = "hostVersionByClusterAndHostname", query =
        "SELECT hostVersion FROM HostVersionEntity hostVersion JOIN hostVersion.hostEntity host JOIN host.clusterEntities clusters " +
            "WHERE clusters.clusterName=:clusterName AND hostVersion.hostEntity.hostName=:hostName"),

    @NamedQuery(name = "hostVersionByHostname", query =
        "SELECT hostVersion FROM HostVersionEntity hostVersion JOIN hostVersion.hostEntity host " +
            "WHERE hostVersion.hostEntity.hostName=:hostName"),

    @NamedQuery(
        name = "findByClusterAndState",
        query = "SELECT hostVersion FROM HostVersionEntity hostVersion JOIN hostVersion.hostEntity host JOIN host.clusterEntities clusters "
            + "WHERE clusters.clusterName=:clusterName AND hostVersion.state=:state"),

    @NamedQuery(
        name = "findByCluster",
        query = "SELECT hostVersion FROM HostVersionEntity hostVersion JOIN hostVersion.hostEntity host JOIN host.clusterEntities clusters "
            + "WHERE clusters.clusterName=:clusterName"),

    @NamedQuery(name = "hostVersionByClusterHostnameAndState", query =
        "SELECT hostVersion FROM HostVersionEntity hostVersion JOIN hostVersion.hostEntity host JOIN host.clusterEntities clusters " +
            "WHERE clusters.clusterName=:clusterName AND hostVersion.hostEntity.hostName=:hostName AND hostVersion.state=:state"),

    @NamedQuery(name = "hostVersionByClusterStackVersionAndHostname", query =
        "SELECT hostVersion FROM HostVersionEntity hostVersion JOIN hostVersion.hostEntity host JOIN host.clusterEntities clusters " +
            "WHERE clusters.clusterName=:clusterName AND hostVersion.repositoryVersion.stack.stackName=:stackName AND hostVersion.repositoryVersion.stack.stackVersion=:stackVersion AND hostVersion.repositoryVersion.version=:version AND " +
            "hostVersion.hostEntity.hostName=:hostName"),

    @NamedQuery(
        name = "findHostVersionByClusterAndRepository",
        query = "SELECT hostVersion FROM HostVersionEntity hostVersion JOIN hostVersion.hostEntity host JOIN host.clusterEntities clusters "
            + "WHERE clusters.clusterId = :clusterId AND hostVersion.repositoryVersion = :repositoryVersion"),

    @NamedQuery(
        name = "hostVersionByRepositoryAndStates",
        query = "SELECT hostVersion FROM HostVersionEntity hostVersion WHERE hostVersion.repositoryVersion = :repositoryVersion AND hostVersion.state IN :states"),

    @NamedQuery(
        name = "findByHostAndRepository",
        query = "SELECT hostVersion FROM HostVersionEntity hostVersion WHERE hostVersion.hostEntity = :host AND hostVersion.repositoryVersion = :repositoryVersion")

})
public class HostVersionEntity {

  @Id
  @Column(name = "id", nullable = false, insertable = true, updatable = false)
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "host_version_id_generator")
  private Long id;

  @ManyToOne
  @JoinColumn(name = "repo_version_id", referencedColumnName = "repo_version_id", nullable = false)
  private RepositoryVersionEntity repositoryVersion;

  @Column(name = "host_id", nullable=false, insertable = false, updatable = false)
  private Long hostId;

  @ManyToOne
  @JoinColumn(name = "host_id", referencedColumnName = "host_id", nullable = false)
  private HostEntity hostEntity;

  @Column(name = "state", nullable = false, insertable = true, updatable = true)
  @Enumerated(value = EnumType.STRING)
  private RepositoryVersionState state;

  /**
   * Empty constructor is needed by unit tests.
   */
  public HostVersionEntity() {
  }

  /**
   * When using this constructor, you should also call setHostEntity(). Otherwise
   * you will have persistence errors when persisting the instance.
   */
  public HostVersionEntity(HostEntity hostEntity, RepositoryVersionEntity repositoryVersion, RepositoryVersionState state) {
    this.hostEntity = hostEntity;
    this.repositoryVersion = repositoryVersion;
    this.state = state;
  }

  /**
   * This constructor is mainly used by the unit tests in order to construct an object without the id.
   */
  public HostVersionEntity(HostVersionEntity other) {
    hostEntity = other.hostEntity;
    repositoryVersion = other.repositoryVersion;
    state = other.state;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getHostName() {
    return hostEntity != null ? hostEntity.getHostName() : null;
  }

  public HostEntity getHostEntity() {
    return hostEntity;
  }

  public void setHostEntity(HostEntity hostEntity) {
    this.hostEntity = hostEntity;
  }

  public RepositoryVersionState getState() {
    return state;
  }

  public void setState(RepositoryVersionState state) {
    this.state = state;
  }

  public RepositoryVersionEntity getRepositoryVersion() {
    return repositoryVersion;
  }

  public void setRepositoryVersion(RepositoryVersionEntity repositoryVersion) {
    this.repositoryVersion = repositoryVersion;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((hostEntity == null) ? 0 : hostEntity.hashCode());
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + ((repositoryVersion == null) ? 0 : repositoryVersion.hashCode());
    result = prime * result + ((state == null) ? 0 : state.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }

    HostVersionEntity other = (HostVersionEntity) obj;
    if (!Objects.equals(id, other.id)) {
      return false;
    }
    if (hostEntity != null ? !hostEntity.equals(other.hostEntity) : other.hostEntity != null) {
      return false;
    }
    if (repositoryVersion != null ? !repositoryVersion.equals(other.repositoryVersion) : other.repositoryVersion != null) {
      return false;
    }
    if (state != other.state) {
      return false;
    }
    return true;
  }

  public Long getHostId() {
    return hostId;
  }

  public void setHostId(Long hostId) {
    this.hostId = hostId;
  }
}
