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

package org.apache.ambari.server.metadata;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

/**
 * RoleCommandOrderProvider which caches RoleCommandOrder objects for a cluster to avoid the cost of construction of
 * RoleCommandOrder objects each time.
 */
@Singleton
public class CachedRoleCommandOrderProvider implements RoleCommandOrderProvider {

  private static final Logger LOG = LoggerFactory.getLogger(CachedRoleCommandOrderProvider.class);

  @Inject
  private Injector injector;

  @Inject
  private Clusters clusters;

  private Map<Integer, RoleCommandOrder> rcoMap = new ConcurrentHashMap<>();

  @Inject
  public CachedRoleCommandOrderProvider() {
  }

  @Override
  public RoleCommandOrder getRoleCommandOrder(Long clusterId) {
    Cluster cluster = null;
    try {
      cluster = clusters.getCluster(clusterId);
      return getRoleCommandOrder(cluster);
    } catch (AmbariException e) {
      return null;
    }

  }

  @Override
  public RoleCommandOrder getRoleCommandOrder(Cluster cluster) {
    boolean hasGLUSTERFS = false;
    boolean isNameNodeHAEnabled = false;
    boolean isResourceManagerHAEnabled = false;

    try {
      if (cluster != null && cluster.getService("GLUSTERFS") != null) {
        hasGLUSTERFS = true;
      }
    } catch (AmbariException ignored) {
    }

    try {
      if (cluster != null &&
        cluster.getService("HDFS") != null &&
        cluster.getService("HDFS").getServiceComponent("JOURNALNODE") != null) {
        isNameNodeHAEnabled = true;
      }
    } catch (AmbariException ignored) {
    }

    try {
      if (cluster != null &&
        cluster.getService("YARN") != null &&
        cluster.getService("YARN").getServiceComponent("RESOURCEMANAGER").getServiceComponentHosts().size() > 1) {
        isResourceManagerHAEnabled = true;
      }
    } catch (AmbariException ignored) {
    }

    int clusterCacheId = new HashCodeBuilder()
      .append(cluster != null ? cluster.getClusterId() : -1)
      .append(hasGLUSTERFS)
      .append(isNameNodeHAEnabled)
      .append(isResourceManagerHAEnabled)
      .toHashCode();

    RoleCommandOrder rco = rcoMap.get(clusterCacheId);
    if (rco == null) {
      rco = injector.getInstance(RoleCommandOrder.class);

      LinkedHashSet<String> sectionKeys = new LinkedHashSet<>();

      if (hasGLUSTERFS) {
        sectionKeys.add(RoleCommandOrder.GLUSTERFS_DEPS_KEY);
      } else {
        sectionKeys.add(RoleCommandOrder.NO_GLUSTERFS_DEPS_KEY);
      }

      if (isNameNodeHAEnabled) {
        sectionKeys.add(RoleCommandOrder.NAMENODE_HA_DEPS_KEY);
      }

      if (isResourceManagerHAEnabled) {
        sectionKeys.add(RoleCommandOrder.RESOURCEMANAGER_HA_DEPS_KEY);
      }

      rco.initialize(cluster, sectionKeys);
      rcoMap.put(clusterCacheId, rco);
    }
    return rco;
  }

  /**
   * Clear all entries - used after an upgrade
   */
  public void clearRoleCommandOrderCache() {
    rcoMap.clear();
  }
}
