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

package org.apache.ambari.server.orm.dao;

import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.StackId;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * The {@link org.apache.ambari.server.orm.dao.HostVersionDAO} class manages the {@link org.apache.ambari.server.orm.entities.HostVersionEntity}
 * instances associated with a host. Each host can have multiple stack versions in {@link org.apache.ambari.server.state.RepositoryVersionState#INSTALLED}
 * which are installed, exactly one stack version that is either {@link org.apache.ambari.server.state.RepositoryVersionState#CURRENT} or
 * {@link org.apache.ambari.server.state.RepositoryVersionState#INSTALLING}.
 */
@Singleton
public class HostVersionDAO extends CrudDAO<HostVersionEntity, Long> {
  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  /**
   * Constructor.
   */
  public HostVersionDAO() {
    super(HostVersionEntity.class);
  }

  /**
   * Construct a Host Version. Additionally this will update parent connection relations without
   * forcing refresh of parent entity
   * @param entity entity to create
   */
  @Override
  @Transactional
  public void create(HostVersionEntity entity) throws IllegalArgumentException{
    // check if repository version is not missing, to avoid NPE
    if (entity.getRepositoryVersion() == null) {
      throw new IllegalArgumentException("RepositoryVersion argument is not set for the entity");
    }

    super.create(entity);
    entity.getRepositoryVersion().updateHostVersionEntityRelation(entity);
  }

  /**
   * Retrieve all of the host versions for the given cluster name, stack name,
   * and stack version.
   *
   * @param clusterName
   *          Cluster name
   * @param stackId
   *          Stack (e.g., HDP-2.2)
   * @param version
   *          Stack version (e.g., 2.2.0.1-995)
   * @return Return all of the host versions that match the criteria.
   */
  @RequiresSession
  public List<HostVersionEntity> findByClusterStackAndVersion(
      String clusterName, StackId stackId, String version) {
    final TypedQuery<HostVersionEntity> query = entityManagerProvider.get().createNamedQuery("hostVersionByClusterAndStackAndVersion", HostVersionEntity.class);
    query.setParameter("clusterName", clusterName);
    query.setParameter("stackName", stackId.getStackName());
    query.setParameter("stackVersion", stackId.getStackVersion());
    query.setParameter("version", version);

    return daoUtils.selectList(query);
  }

  /**
   * Retrieve all of the host versions for the given host name across all clusters.
   *
   * @param hostName FQDN of host
   * @return Return all of the host versions that match the criteria.
   */
  @RequiresSession
  public List<HostVersionEntity> findByHost(String hostName) {
    final TypedQuery<HostVersionEntity> query = entityManagerProvider.get()
        .createNamedQuery("hostVersionByHostname", HostVersionEntity.class);
    query.setParameter("hostName", hostName);

    return daoUtils.selectList(query);
  }

  /**
   * Retrieve all of the host versions for the given cluster name and host name.
   *
   * @param clusterName Cluster name
   * @param hostName FQDN of host
   * @return Return all of the host versions that match the criteria.
   */
  @RequiresSession
  public List<HostVersionEntity> findByClusterAndHost(String  clusterName, String hostName) {
    final TypedQuery<HostVersionEntity> query = entityManagerProvider.get()
        .createNamedQuery("hostVersionByClusterAndHostname", HostVersionEntity.class);
    query.setParameter("clusterName", clusterName);
    query.setParameter("hostName", hostName);

    return daoUtils.selectList(query);
  }

  /**
   * Retrieve all of the cluster versions for the given cluster name.
   *
   * @param clusterName Cluster name
   * @return Return all of the host versions that match the criteria.
   */
  @RequiresSession
  public List<HostVersionEntity> findByCluster(String  clusterName) {
    final TypedQuery<HostVersionEntity> query = entityManagerProvider.get()
        .createNamedQuery("findByCluster", HostVersionEntity.class);
    query.setParameter("clusterName", clusterName);

    return daoUtils.selectList(query);
  }

  /**
   * Retrieve all of the host versions for the given cluster name, and state. <br/>
   * @param clusterName Cluster name
   * @param state repository version state
   * @return Return all of the host versions that match the criteria.
   */
  @RequiresSession
  public List<HostVersionEntity> findByClusterAndState(String clusterName, RepositoryVersionState state) {
    final TypedQuery<HostVersionEntity> query = entityManagerProvider.get()
        .createNamedQuery("findByClusterAndState", HostVersionEntity.class);
    query.setParameter("clusterName", clusterName);
    query.setParameter("state", state);

    return daoUtils.selectList(query);  
  }
  
  /**
   * Retrieve all of the host versions for the given cluster name, host name, and state. <br/>
   * @param clusterName Cluster name
   * @param hostName FQDN of host
   * @param state repository version state
   * @return Return all of the host versions that match the criteria.
   */
  @RequiresSession
  public List<HostVersionEntity> findByClusterHostAndState(String  clusterName, String hostName, RepositoryVersionState state) {
    final TypedQuery<HostVersionEntity> query = entityManagerProvider.get()
        .createNamedQuery("hostVersionByClusterHostnameAndState", HostVersionEntity.class);
    query.setParameter("clusterName", clusterName);
    query.setParameter("hostName", hostName);
    query.setParameter("state", state);

    return daoUtils.selectList(query);
  }

  /**
   * Retrieve the single host version for the given cluster, stack name, stack
   * version, and host name. <br/>
   * This query is slow and not suitable for frequent use.
   *
   * @param clusterName
   *          Cluster name
   * @param stackId
   *          Stack ID (e.g., HDP-2.2)
   * @param version
   *          Stack version (e.g., 2.2.0.1-995)
   * @param hostName
   *          FQDN of host
   * @return Returns the single host version that matches the criteria.
   */
  @RequiresSession
  public HostVersionEntity findByClusterStackVersionAndHost(String clusterName,
      StackId stackId, String version, String hostName) {

    final TypedQuery<HostVersionEntity> query = entityManagerProvider.get()
        .createNamedQuery("hostVersionByClusterStackVersionAndHostname", HostVersionEntity.class);
    query.setParameter("clusterName", clusterName);
    query.setParameter("stackName", stackId.getStackName());
    query.setParameter("stackVersion", stackId.getStackVersion());
    query.setParameter("version", version);
    query.setParameter("hostName", hostName);

    return daoUtils.selectSingle(query);
  }

  /**
   * Gets all host version entities assocaited with the specified cluster and
   * repository.
   *
   * @param clusterId
   *          the cluster ID.
   * @param repositoryVersion
   *          the repository (not {@code null}).
   * @return the host versions.
   */
  @RequiresSession
  public List<HostVersionEntity> findHostVersionByClusterAndRepository(long clusterId,
      RepositoryVersionEntity repositoryVersion) {
    TypedQuery<HostVersionEntity> query = entityManagerProvider.get().createNamedQuery(
        "findHostVersionByClusterAndRepository", HostVersionEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("repositoryVersion", repositoryVersion);

    return daoUtils.selectList(query);
  }

  /**
   * Gets all host version entities that are of the given states
   *
   * @param repositoryVersion
   *          the repository (not {@code null})
   * @param states
   *          the states
   * @return the host versions
   */
  @RequiresSession
  public List<HostVersionEntity> findByRepositoryAndStates(RepositoryVersionEntity repositoryVersion,
      Collection<RepositoryVersionState> states) {

    TypedQuery<HostVersionEntity> query = entityManagerProvider.get().createNamedQuery(
        "hostVersionByRepositoryAndStates", HostVersionEntity.class);

    query.setParameter("repositoryVersion", repositoryVersion);
    query.setParameter("states", states);

    return daoUtils.selectList(query);
  }

  /**
   * Gets the {@link HostVersionEntity} associted with the specified host and
   * repository.
   *
   * @param host
   * @param repositoryVersion
   * @return
   */
  @RequiresSession
  public HostVersionEntity findHostVersionByHostAndRepository(HostEntity host,
      RepositoryVersionEntity repositoryVersion) {
    TypedQuery<HostVersionEntity> query = entityManagerProvider.get().createNamedQuery(
        "findByHostAndRepository", HostVersionEntity.class);

    query.setParameter("host", host);
    query.setParameter("repositoryVersion", repositoryVersion);

    return daoUtils.selectOne(query);
  }

  @Transactional
  public void removeByHostName(String hostName) {
    Collection<HostVersionEntity> hostVersions = findByHost(hostName);
    this.remove(hostVersions);
  }

}
