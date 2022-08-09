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

package org.apache.ambari.server.view;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.dao.RemoteAmbariClusterDAO;
import org.apache.ambari.server.orm.entities.RemoteAmbariClusterEntity;
import org.apache.ambari.server.orm.entities.RemoteAmbariClusterServiceEntity;
import org.apache.ambari.view.AmbariHttpException;

/**
 * Registry for Remote Ambari Cluster
 */
@Singleton
public class RemoteAmbariClusterRegistry {

  private ConcurrentHashMap<Long,RemoteAmbariCluster> clusterMap = new ConcurrentHashMap<>();

  @Inject
  private RemoteAmbariClusterDAO remoteAmbariClusterDAO;

  @Inject
  private Configuration configuration;

  public RemoteAmbariCluster get(Long clusterId) throws MalformedURLException, ClusterNotFoundException {
    RemoteAmbariCluster remoteAmbariCluster = clusterMap.get(clusterId);
    if (remoteAmbariCluster == null) {
      RemoteAmbariCluster cluster = getCluster(clusterId);
      RemoteAmbariCluster oldCluster = clusterMap.putIfAbsent(clusterId, cluster);
      if (oldCluster == null) remoteAmbariCluster = cluster;
      else remoteAmbariCluster = oldCluster;
    }
    return remoteAmbariCluster;
  }


  private RemoteAmbariCluster getCluster(Long clusterId) throws MalformedURLException, ClusterNotFoundException {
    RemoteAmbariClusterEntity remoteAmbariClusterEntity = remoteAmbariClusterDAO.findById(clusterId);
    if (remoteAmbariClusterEntity == null) {
      throw new ClusterNotFoundException(clusterId);
    }
    RemoteAmbariCluster remoteAmbariCluster = new RemoteAmbariCluster(remoteAmbariClusterEntity, configuration);
    return remoteAmbariCluster;
  }

  /**
   * Update the remote cluster properties
   *
   * @param entity
   */
  public void update(RemoteAmbariClusterEntity entity) {
    remoteAmbariClusterDAO.update(entity);
    clusterMap.remove(entity.getId());
  }

  /**
   * Remove the cluster entity from registry and database
   *
   * @param entity
   */
  public void delete(RemoteAmbariClusterEntity entity) {
    remoteAmbariClusterDAO.delete(entity);
    clusterMap.remove(entity.getId());
  }

  /**
   * Save Remote Cluster Entity after setting services.
   *
   * @param entity
   * @param update
   * @throws IOException
   * @throws AmbariHttpException
   */
  public void saveOrUpdate(RemoteAmbariClusterEntity entity, boolean update) throws IOException, AmbariHttpException {

    RemoteAmbariCluster cluster = new RemoteAmbariCluster(entity, configuration);
    Set<String> services = cluster.getServices();

    if (!cluster.isAmbariOrClusterAdmin()) {
      throw new AmbariException("User must be Ambari or Cluster Adminstrator.");
    }

    Collection<RemoteAmbariClusterServiceEntity> serviceEntities = new ArrayList<>();

    for (String service : services) {
      RemoteAmbariClusterServiceEntity serviceEntity = new RemoteAmbariClusterServiceEntity();
      serviceEntity.setServiceName(service);
      serviceEntity.setCluster(entity);
      serviceEntities.add(serviceEntity);
    }

    entity.setServices(serviceEntities);

    if (update) {
      update(entity);
    } else {
      remoteAmbariClusterDAO.save(entity);
    }
  }

}
