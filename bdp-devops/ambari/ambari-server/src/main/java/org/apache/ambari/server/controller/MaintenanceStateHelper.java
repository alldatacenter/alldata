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
package org.apache.ambari.server.controller;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.controller.internal.RequestOperationLevel;
import org.apache.ambari.server.controller.internal.RequestResourceFilter;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

/**
 * Used to help manage maintenance state checks.
 */
@Singleton
public class MaintenanceStateHelper {
  private static final Logger LOG = LoggerFactory.getLogger(MaintenanceStateHelper.class);

  @Inject
  private Clusters clusters;

  @Inject
  public MaintenanceStateHelper(Injector injector) {
    injector.injectMembers(this);
  }

  /**
   * @param cluster
   *          cluster for request
   * @param levelObj
   *          operation level (can be null)
   * @param reqFilter
   *          request resource filter for operation (can't be null)
   * @param serviceName
   *          service name (can be null)
   * @param componentName
   *          component name (can be null)
   * @param hostname
   *          host name (can't be null)
   * @return true if operation may be performed on a given target (based on
   *         target's maintenance status).
   */
  public boolean isOperationAllowed(Cluster cluster,
      RequestOperationLevel levelObj, RequestResourceFilter reqFilter,
      String serviceName, String componentName, String hostname)
      throws AmbariException {
    Resource.Type level;
    if (levelObj == null) {
      level = guessOperationLevel(reqFilter);
    } else {
      level = levelObj.getLevel();
    }
    return isOperationAllowed(cluster, level, serviceName, componentName,
        hostname);
  }

  /**
   * @param cluster
   *          cluster for request
   * @param level
   *          operation level (can't be null)
   * @param serviceName
   *          service name (can be null)
   * @param componentName
   *          component name (can be null)
   * @param hostname
   *          host name (can't be null)
   * @return true if operation may be performed on a given target (based on
   *         target's maintenance status).
   */
  boolean isOperationAllowed(Cluster cluster, Resource.Type level,
      String serviceName, String componentName, String hostname)
      throws AmbariException {
    if (serviceName != null && !serviceName.isEmpty()) {
      Service service = cluster.getService(serviceName);
      if (componentName != null && !componentName.isEmpty()) {
        ServiceComponentHost sch = service.getServiceComponent(componentName).getServiceComponentHost(
            hostname);
        return isOperationAllowed(level, sch);
      } else { // Only service name is defined
        return isOperationAllowed(level, service);
      }
    } else { // Service is not defined, using host
      Host host = clusters.getHost(hostname);
      return isOperationAllowed(host, cluster.getClusterId(), level);
    }
  }

  /**
   * @param level
   *          operation level
   * @param service
   *          the service
   * @return true if operation may be performed on a given target (based on
   *         target's maintenance status).
   */
  public boolean isOperationAllowed(Resource.Type level, Service service)
      throws AmbariException {
    if (level == Resource.Type.Cluster) {
      return service.getMaintenanceState() == MaintenanceState.OFF;
    } else {
      return true;
    }
  }

  /**
   * @param host
   *          the service
   * @param clusterId
   *          cluster the host belongs to
   * @param level
   *          operation level for request
   * @return true if operation may be performed on a given target (based on
   *         target's maintenance status).
   */
  public boolean isOperationAllowed(Host host, long clusterId,
      Resource.Type level) throws AmbariException {
    if (level == Resource.Type.Cluster) {
      return host.getMaintenanceState(clusterId) == MaintenanceState.OFF;
    } else {
      return true;
    }
  }

  /**
   * Get effective state of HostComponent
   *
   * @param sch
   *          the host component
   * @param host
   *          the host
   * @return the maintenance state
   * @throws AmbariException
   */
  public MaintenanceState getEffectiveState(ServiceComponentHost sch, Host host)
      throws AmbariException {
    Cluster cluster = clusters.getCluster(sch.getClusterName());
    Service service = cluster.getService(sch.getServiceName());

    if (null == host) {
      throw new HostNotFoundException(cluster.getClusterName(),
          sch.getHostName());
    }

    return getEffectiveState(cluster.getClusterId(), service, host, sch);
  }

  /**
   * Gets the effective state for a HostComponent
   *
   * @param sch
   *          the host component
   * @return the maintenance state
   * @throws AmbariException
   */
  public MaintenanceState getEffectiveState(ServiceComponentHost sch)
      throws AmbariException {
    Host host = clusters.getHost(sch.getHostName());
    return getEffectiveState(sch, host);
  }

  /**
   * @param clusterId
   *          the cluster id
   * @param service
   *          the service
   * @param host
   *          the host
   * @param sch
   *          the host component
   * @return the effective maintenance state
   */
  private MaintenanceState getEffectiveState(long clusterId, Service service,
      Host host, ServiceComponentHost sch) {

    MaintenanceState schState = sch.getMaintenanceState();
    if (MaintenanceState.ON == schState) {
      return MaintenanceState.ON;
    }

    MaintenanceState serviceState = service.getMaintenanceState();
    MaintenanceState hostState = host.getMaintenanceState(clusterId);

    if (MaintenanceState.OFF != serviceState
        && MaintenanceState.OFF != hostState) {
      return MaintenanceState.IMPLIED_FROM_SERVICE_AND_HOST;
    }

    if (MaintenanceState.OFF != serviceState) {
      return MaintenanceState.IMPLIED_FROM_SERVICE;
    }

    if (MaintenanceState.OFF != hostState) {
      return MaintenanceState.IMPLIED_FROM_HOST;
    }

    return schState;
  }

  /**
   * Gets the effective maintenance state for a given alert. This will determine
   * whether the alert needs to report that it is in maintenance mode.
   *
   * @param clusterId
   *          the ID of the cluster for the alert.
   * @param alert
   *          the alert (not {@code null}).
   * @return {@link MaintenanceState#OFF} if the host/service/component all
   *         report they are not in maintenance mode;
   *         {@link MaintenanceState#ON} otherwise.
   * @throws AmbariException
   */
  public MaintenanceState getEffectiveState(long clusterId, Alert alert) throws AmbariException {
    String serviceName = alert.getService();
    String componentName = alert.getComponent();
    String hostName = alert.getHostName();

    if (null == serviceName && null == hostName) {
      LOG.warn("Unable to determine maintenance state for an alert without a service or host");
      return MaintenanceState.OFF;
    }

    // always start with the host first; we always have a host unless this is an
    // aggregate alert (service-level alert)
    if (StringUtils.isNotBlank(hostName)) {
      Host host = clusters.getHost(hostName);
      if (host.getMaintenanceState(clusterId) != MaintenanceState.OFF) {
        return MaintenanceState.ON;
      }
    }

    // the AMBARI service is not a real service; it's never in MM
    if( StringUtils.equals(RootService.AMBARI.name(), serviceName)){
      return MaintenanceState.OFF;
    }

    // the host is not in MM, move onto the service
    Cluster cluster = clusters.getClusterById(clusterId);
    Service service = cluster.getService(serviceName);
    if( service.getMaintenanceState() != MaintenanceState.OFF ){
      return MaintenanceState.ON;
    }

    if( StringUtils.isNotBlank(componentName)){
      ServiceComponent serviceComponent = service.getServiceComponent(componentName);
      ServiceComponentHost serviceComponentHost = serviceComponent.getServiceComponentHost(hostName);

      if (serviceComponentHost.getMaintenanceState() != MaintenanceState.OFF) {
        return MaintenanceState.ON;
      }
    }

    // MM is off at the host, service, and component level; return OFF
    return MaintenanceState.OFF;
  }

  /**
   * @param clusters
   *          the collection of clusters
   * @param cluster
   *          the specific cluster to check
   * @return a property map of all host components that are in a maintenance
   *         state (either {@link MaintenanceState#ON} or
   *         {@link MaintenanceState#IMPLIED_FROM_HOST} or
   *         {@link MaintenanceState#IMPLIED_FROM_SERVICE} or
   *         {@link MaintenanceState#IMPLIED_FROM_SERVICE_AND_HOST})
   */
  public Set<Map<String, String>> getMaintenanceHostComponents(
      Clusters clusters, Cluster cluster) throws AmbariException {
    Set<Map<String, String>> set = new HashSet<>();

    Map<String, Host> hosts = clusters.getHostsForCluster(cluster.getClusterName());

    for (Service service : cluster.getServices().values()) {
      for (ServiceComponent sc : service.getServiceComponents().values()) {
        if (sc.isClientComponent()) {
          continue;
        }

        for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
          Host host = hosts.get(sch.getHostName());

          if (MaintenanceState.OFF != getEffectiveState(cluster.getClusterId(),
              service, host, sch)) {
            Map<String, String> map = new HashMap<>();
            map.put("host", sch.getHostName());
            map.put("service", sch.getServiceName());
            map.put("component", sch.getServiceComponentName());
            set.add(map);
          }
        }
      }
    }

    return set;
  }

  /**
   * Determine based on the requesting Resource level and the state of the
   * operand whether to allow operations on it.
   *
   * @param operationLevel
   *          Request Source: {CLUSTER, SERVICE, HOSTCOMPONENT, HOST}
   * @param sch
   *          HostComponent which is the operand of the operation
   * @return
   * @throws AmbariException
   */
  public boolean isOperationAllowed(Resource.Type operationLevel,
      ServiceComponentHost sch) throws AmbariException {
    MaintenanceState maintenanceState = getEffectiveState(sch);

    switch (operationLevel.getInternalType()) {
      case Cluster:
        if (maintenanceState.equals(MaintenanceState.OFF)) {
          return true;
        }
        break;
      case Service:
        if (maintenanceState.equals(MaintenanceState.IMPLIED_FROM_SERVICE)
            || maintenanceState.equals(MaintenanceState.OFF)) {
          return true;
        }
        break;
      case Host:
        if (maintenanceState.equals(MaintenanceState.IMPLIED_FROM_HOST)
            || maintenanceState.equals(MaintenanceState.OFF)) {
          return true;
        }
        break;
      case HostComponent: {
        return true;
      }
      default:
        LOG.warn("Unsupported Resource type, type = " + operationLevel);
        break;
    }
    return false;
  }

  /**
   * Fallback logic for guessing operation level from request resource filter.
   * It is used when operation level is not defined explicitly.
   */
  public Resource.Type guessOperationLevel(RequestResourceFilter filter) {
    Resource.Type result;
    if (filter == null) {
      // CLUSTER can be assumed to be the default level if no ResourceFilter
      // is specified, because for any other resource operation
      // a ResourceFilter is mandatory.
      result = Resource.Type.Cluster;
    } else {
      boolean serviceDefined = filter.getServiceName() != null;
      boolean componentDefined = filter.getComponentName() != null;
      boolean hostsDefined = filter.getHostNames() != null
          && filter.getHostNames().size() > 0;

      if (hostsDefined & componentDefined) {
        result = Resource.Type.HostComponent;
      } else if (!serviceDefined & hostsDefined) {
        result = Resource.Type.Host;
      } else if (serviceDefined & !hostsDefined) {
        // This option also includes resource filters
        // that target service on few hosts
        result = Resource.Type.Service;
      } else { // Absolute fallback
        // Cluster level should be a good option for any unsure cases
        // Cluster-level actions only
        result = Resource.Type.Cluster;
      }
    }
    return result;
  }

  /**
   * Removes from a set all hosts that match a given condition.
   *
   * @param candidateHosts
   *          source set that should to be modified
   * @param condition
   *          condition
   * @return all hosts that have been removed from a candidateHosts
   */
  public Set<String> filterHostsInMaintenanceState(Set<String> candidateHosts,
      HostPredicate condition) throws AmbariException {
    // Filter hosts that are in MS
    Set<String> removedHosts = new HashSet<>();
    for (String hostname : candidateHosts) {
      if (condition.shouldHostBeRemoved(hostname)) {
        removedHosts.add(hostname);
      }
    }
    candidateHosts.removeAll(removedHosts);
    return removedHosts;
  }

  /**
   * The {@link HostPredicate} interaface is used as a way to pass a custom
   * implementation into one of the many
   * {@link MaintenanceStateHelper#isOperationAllowed(org.apache.ambari.server.controller.spi.Resource.Type, Service)}
   * methods.
   */
  public interface HostPredicate {
    /**
     * Gets whether the specified host should not be included in a result set.
     *
     * @param hostname
     *          the host (not {@code null}).
     * @return {@code true} to indicate that the specified host is not a valid
     *         choice, {@code false} otherwise.
     * @throws AmbariException
     */
    boolean shouldHostBeRemoved(String hostname) throws AmbariException;
  }
}
