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
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class HostComponentDesiredStateDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;

  @Inject
  HostDAO hostDAO;

  @Inject
  DaoUtils daoUtils;

  @RequiresSession
  public HostComponentDesiredStateEntity findById(long id) {
    return entityManagerProvider.get().find(HostComponentDesiredStateEntity.class, id);
  }

  @RequiresSession
  public List<HostComponentDesiredStateEntity> findAll() {
    final TypedQuery<HostComponentDesiredStateEntity> query = entityManagerProvider.get().createNamedQuery("HostComponentDesiredStateEntity.findAll", HostComponentDesiredStateEntity.class);
    try {
      return query.getResultList();
    } catch (NoResultException ignored) {
    }
    return null;
  }

  /**
   * Retrieve the single Host Component Desired State for the given unique service, component, and host.
   *
   * @param serviceName Service Name
   * @param componentName Component Name
   * @param hostName Host Name
   * @return Return all of the Host Component States that match the criteria.
   */
  @RequiresSession
  public HostComponentDesiredStateEntity findByServiceComponentAndHost(String serviceName, String componentName, String hostName) {
    final TypedQuery<HostComponentDesiredStateEntity> query = entityManagerProvider.get().createNamedQuery("HostComponentDesiredStateEntity.findByServiceComponentAndHost", HostComponentDesiredStateEntity.class);
    query.setParameter("serviceName", serviceName);
    query.setParameter("componentName", componentName);
    query.setParameter("hostName", hostName);

    return daoUtils.selectSingle(query);
  }

  /**
   * Retrieve the single Host Component Desired State for the given unique cluster, service, component, and host.
   *
   * @param clusterId Cluster ID
   * @param serviceName Service Name
   * @param componentName Component Name
   * @param hostId Host ID
   * @return Return the Host Component Desired State entity that match the criteria.
   */
  @RequiresSession
  public HostComponentDesiredStateEntity findByIndex(Long clusterId, String serviceName,
                                                     String componentName, Long hostId) {
    final TypedQuery<HostComponentDesiredStateEntity> query = entityManagerProvider.get()
      .createNamedQuery("HostComponentDesiredStateEntity.findByIndexAndHost", HostComponentDesiredStateEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("serviceName", serviceName);
    query.setParameter("componentName", componentName);
    query.setParameter("hostId", hostId);

    return daoUtils.selectSingle(query);
  }

  /**
   * Retrieve the single Host Component Desired State for the given unique cluster, service, component, and host.
   *
   * @param clusterId Cluster ID
   * @param serviceName Service Name
   * @param componentName Component Name
   * @return Return the Host Component Desired State entity that match the criteria.
   */
  @RequiresSession
  public List<HostComponentDesiredStateEntity> findByIndex(Long clusterId, String serviceName,
                                                     String componentName) {
    final TypedQuery<HostComponentDesiredStateEntity> query = entityManagerProvider.get()
      .createNamedQuery("HostComponentDesiredStateEntity.findByIndex", HostComponentDesiredStateEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("serviceName", serviceName);
    query.setParameter("componentName", componentName);

    return daoUtils.selectList(query);
  }

  @RequiresSession
  public List<HostComponentDesiredStateEntity> findByHostsAndCluster(Collection<Long> hostIds, Long clusterId) {
    final TypedQuery<HostComponentDesiredStateEntity> query = entityManagerProvider.get()
      .createNamedQuery("HostComponentDesiredStateEntity.findByHostsAndCluster", HostComponentDesiredStateEntity.class);

    query.setParameter("hostIds", hostIds);
    query.setParameter("clusterId", clusterId);

    return daoUtils.selectList(query);
  }

  @Transactional
  public void refresh(HostComponentDesiredStateEntity hostComponentDesiredStateEntity) {
    entityManagerProvider.get().refresh(hostComponentDesiredStateEntity);
  }

  @Transactional
  public void create(HostComponentDesiredStateEntity hostComponentDesiredStateEntity) {
    entityManagerProvider.get().persist(hostComponentDesiredStateEntity);
  }

  @Transactional
  public HostComponentDesiredStateEntity merge(HostComponentDesiredStateEntity hostComponentDesiredStateEntity) {
    return entityManagerProvider.get().merge(hostComponentDesiredStateEntity);
  }

  @Transactional
  public void remove(HostComponentDesiredStateEntity hostComponentDesiredStateEntity) {
    HostEntity hostEntity = hostComponentDesiredStateEntity.getHostEntity();

    if (hostEntity == null) {
      throw new IllegalStateException(String.format("Missing hostEntity for host id %1d",
              hostComponentDesiredStateEntity.getHostId()));
    }

    hostEntity.removeHostComponentDesiredStateEntity(hostComponentDesiredStateEntity);

    entityManagerProvider.get().remove(hostComponentDesiredStateEntity);
    hostDAO.merge(hostEntity);
  }

  @Transactional
  public void removeId(long id) {
    remove(findById(id));
  }

}
