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
package org.apache.ambari.server.controller.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.orm.dao.AlertHostSummaryDTO;
import org.apache.ambari.server.orm.dao.AlertSummaryDTO;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Property provider that adds alert summary information to endpoints.
 */
@StaticallyInject
public class AlertSummaryPropertyProvider extends BaseProvider implements PropertyProvider {

  private final static Logger LOG = LoggerFactory.getLogger(AlertSummaryPropertyProvider.class);

  /**
   * The property ID for a summary of all cluster-wide alerts.
   */
  private final static String ALERTS_SUMMARY = "alerts_summary";

  /**
   * The property ID for a summary of all cluster-wide alerts that have a host
   * associated with them.
   */
  private final static String ALERTS_SUMMARY_HOSTS = "alerts_summary_hosts";

  @Inject
  private static Provider<Clusters> s_clusters = null;

  @Inject
  private static AlertsDAO s_dao = null;

  private Resource.Type m_resourceType = null;
  private String m_clusterPropertyId = null;
  private String m_typeIdPropertyId = null;

  /**
   * Constructor.
   *
   * @param type
   * @param clusterPropertyId
   * @param typeIdPropertyId
   */
  AlertSummaryPropertyProvider(Resource.Type type,
      String clusterPropertyId, String typeIdPropertyId) {
    super(ImmutableSet.of(ALERTS_SUMMARY, ALERTS_SUMMARY_HOSTS));
    m_resourceType = type;
    m_clusterPropertyId = clusterPropertyId;
    m_typeIdPropertyId = typeIdPropertyId;
  }

  @Override
  public Set<Resource> populateResources(Set<Resource> resources,
      Request request, Predicate predicate) throws SystemException {
    Set<String> propertyIds = getRequestPropertyIds(request, predicate);

    try {
      // Optimization:
      // Some information can be determined more efficiently when requested in bulk 
      // for an entire cluster at once. 
      // For Example:
      //   (1) Cluster level alert-status counts
      //   (2) Per host alert-status counts
      // These can be determined in 1 SQL call per cluster, and results used multiple times.
      Map<Long, Map<String, AlertSummaryDTO>> perHostSummaryMap = new HashMap<>();
      Map<Long, AlertHostSummaryDTO> hostsSummaryMap = new HashMap<>();
      Map<String, Cluster> resourcesClusterMap = new HashMap<>();
      for (Resource res : resources) {
        String clusterName = (String) res.getPropertyValue(m_clusterPropertyId);
        if (clusterName == null || resourcesClusterMap.containsKey(clusterName)) {
          continue;
        }
        Cluster cluster = s_clusters.get().getCluster(clusterName);
        resourcesClusterMap.put(clusterName, cluster);
      }
      for (Cluster cluster : resourcesClusterMap.values()) {
        long clusterId = cluster.getClusterId();
        switch (m_resourceType.getInternalType()) {
          case Cluster:
            // only make the calculation if asked
            if (BaseProvider.isPropertyRequested(ALERTS_SUMMARY_HOSTS, propertyIds)) {
              hostsSummaryMap.put(clusterId, s_dao.findCurrentHostCounts(clusterId));
            }
            break;
          case Host:
            if (resources.size() > 1) {
              // More efficient to get information for all hosts in 1 call
              Map<String, AlertSummaryDTO> perHostCounts = s_dao.findCurrentPerHostCounts(clusterId);
              perHostSummaryMap.put(clusterId, perHostCounts);
            }
            break;
          default:
            break;
        }
      }

      for (Resource res : resources) {
        populateResource(res, propertyIds, perHostSummaryMap, hostsSummaryMap);
      }
    } catch (AmbariException e) {
      LOG.error("Could not load built-in alerts - Executor exception ({})",
          e.getMessage());
    }
    return resources;
  }

  private void populateResource(Resource resource, Set<String> requestedIds, Map<Long, Map<String, 
      AlertSummaryDTO>> perHostSummaryMap, Map<Long, AlertHostSummaryDTO> hostsSummaryMap) throws AmbariException {

    AlertSummaryDTO summary = null;
    AlertHostSummaryDTO hostSummary = null;

    String clusterName = (String) resource.getPropertyValue(m_clusterPropertyId);

    if (null == clusterName) {
      return;
    }

    String typeId = null == m_typeIdPropertyId ? null : (String) resource.getPropertyValue(m_typeIdPropertyId);
    Cluster cluster = s_clusters.get().getCluster(clusterName);

    switch (m_resourceType.getInternalType()) {
      case Cluster:
        long clusterId = cluster.getClusterId();

        // only make the calculation if asked
        if (BaseProvider.isPropertyRequested(ALERTS_SUMMARY, requestedIds)) {
          summary = s_dao.findCurrentCounts(cluster.getClusterId(), null, null);
        }

        // only make the calculation if asked
        if (BaseProvider.isPropertyRequested(ALERTS_SUMMARY_HOSTS,
            requestedIds)) {
          if (hostsSummaryMap.containsKey(cluster.getClusterId())) {
            hostSummary = hostsSummaryMap.get(cluster.getClusterId());
          } else {
            hostSummary = s_dao.findCurrentHostCounts(clusterId);
          }
        }

        break;
      case Service:
        summary = s_dao.findCurrentCounts(cluster.getClusterId(), typeId, null);
        break;
      case Host:
      if (perHostSummaryMap.containsKey(cluster.getClusterId()) && 
          perHostSummaryMap.get(cluster.getClusterId()).containsKey(typeId)) {
        summary = perHostSummaryMap.get(cluster.getClusterId()).get(typeId);
      } else {
        summary = s_dao.findCurrentCounts(cluster.getClusterId(), null, typeId);
      }
        break;
      default:
        break;
    }

    // all alerts in the cluster, in summary count form
    if (null != summary) {
      Map<String, Integer> map = new HashMap<>();
      map.put(AlertState.OK.name(), Integer.valueOf(summary.getOkCount()));
      map.put(AlertState.WARNING.name(), Integer.valueOf(summary.getWarningCount()));
      map.put(AlertState.CRITICAL.name(), Integer.valueOf(summary.getCriticalCount()));
      map.put(AlertState.UNKNOWN.name(), Integer.valueOf(summary.getUnknownCount()));
      map.put("MAINTENANCE", Integer.valueOf(summary.getMaintenanceCount()));
      setResourceProperty(resource, ALERTS_SUMMARY, map, requestedIds);
    }

    // the summary of hosts with warning or critical alerts
    if (null != hostSummary) {
      Map<AlertState, Integer> map = new HashMap<>();
      map.put(AlertState.OK, Integer.valueOf(hostSummary.getOkCount()));
      map.put(AlertState.WARNING, Integer.valueOf(hostSummary.getWarningCount()));
      map.put(AlertState.CRITICAL, Integer.valueOf(hostSummary.getCriticalCount()));
      map.put(AlertState.UNKNOWN, Integer.valueOf(hostSummary.getUnknownCount()));
      setResourceProperty(resource, ALERTS_SUMMARY_HOSTS, map, requestedIds);
    }

  }

  @Override
  public Set<String> checkPropertyIds(Set<String> propertyIds) {
    Set<String> rejects = new HashSet<>();

    for (String id : propertyIds) {
      if (!id.startsWith(ALERTS_SUMMARY)) {
        rejects.add(id);
      }
    }

    return rejects;
  }

}
