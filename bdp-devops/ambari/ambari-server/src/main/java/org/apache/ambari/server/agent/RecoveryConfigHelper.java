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

package org.apache.ambari.server.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.events.ClusterConfigChangedEvent;
import org.apache.ambari.server.events.MaintenanceModeEvent;
import org.apache.ambari.server.events.ServiceComponentInstalledEvent;
import org.apache.ambari.server.events.ServiceComponentRecoveryChangedEvent;
import org.apache.ambari.server.events.ServiceComponentUninstalledEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.commons.lang.StringUtils;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class RecoveryConfigHelper {
  /**
   * Recovery related configuration
   */
  public static final String RECOVERY_ENABLED_KEY = "recovery_enabled";
  public static final String RECOVERY_TYPE_KEY = "recovery_type";
  public static final String RECOVERY_TYPE_DEFAULT = "AUTO_START";
  public static final String RECOVERY_LIFETIME_MAX_COUNT_KEY = "recovery_lifetime_max_count";
  public static final String RECOVERY_LIFETIME_MAX_COUNT_DEFAULT = "12";
  public static final String RECOVERY_MAX_COUNT_KEY = "recovery_max_count";
  public static final String RECOVERY_MAX_COUNT_DEFAULT = "6";
  public static final String RECOVERY_WINDOW_IN_MIN_KEY = "recovery_window_in_minutes";
  public static final String RECOVERY_WINDOW_IN_MIN_DEFAULT = "60";
  public static final String RECOVERY_RETRY_GAP_KEY = "recovery_retry_interval";
  public static final String RECOVERY_RETRY_GAP_DEFAULT = "5";

  @Inject
  private Clusters clusters;

  /**
   * Cluster --> Host --> Timestamp
   */
  private ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> timestampMap;

  @Inject
  public RecoveryConfigHelper(AmbariEventPublisher eventPublisher) {
    eventPublisher.register(this);
    timestampMap = new ConcurrentHashMap<>();
  }

  public RecoveryConfig getDefaultRecoveryConfig()
      throws AmbariException {
    return getRecoveryConfig(null, null);
  }

  public RecoveryConfig getRecoveryConfig(String clusterName, String hostname)
      throws AmbariException {
    long now = System.currentTimeMillis();

    if (StringUtils.isNotEmpty(clusterName)) {
      // Insert or update timestamp for cluster::host
      ConcurrentHashMap<String, Long>  hostTimestamp = timestampMap.get(clusterName);
      if (hostTimestamp == null) {
        hostTimestamp = new ConcurrentHashMap<>();
        timestampMap.put(clusterName, hostTimestamp);
      }

      if (StringUtils.isNotEmpty(hostname)) {
        hostTimestamp.put(hostname, now);
      }
    }

    AutoStartConfig autoStartConfig = new AutoStartConfig(clusterName);

    RecoveryConfig recoveryConfig = new RecoveryConfig(autoStartConfig.getEnabledComponents(hostname));
    return recoveryConfig;
  }

  /**
   * Computes if the recovery configuration was updated since the last time it was sent to the agent.
   *
   * @param clusterName - Name of the cluster which the host belongs to.
   * @param hostname - Host name from agent.
   * @param recoveryTimestamp - Time when the recovery configuration was last sent to the agent. Agent
   *                          stores this value and sends it during each heartbeat. -1 if agent was
   *                          restarted or configuration was not sent to the agent since it started.
   * @return
   */
  public boolean isConfigStale(String clusterName, String hostname, long recoveryTimestamp) {
    // Look up the last updated timestamp for the clusterName-->hostname-->timestamp if
    // it is available. If found, compare it with the timestamp from the agent. It the timestamp
    // is different from the timestamp sent by the agent, the recovery config on the agent
    // side is stale and should be sent to the agent during this heartbeat.

    if (StringUtils.isEmpty(clusterName)) {
      throw new IllegalArgumentException("clusterName cannot be empty or null.");
    }

    if (StringUtils.isEmpty(hostname)) {
      throw new IllegalArgumentException("hostname cannot be empty or null.");
    }

    ConcurrentHashMap<String, Long> hostTimestamp = timestampMap.get(clusterName);
    if (hostTimestamp == null) {
      return true;
    }

    Long timestamp = hostTimestamp.get(hostname);

    /*
     * An agent that did not get the configuration during registration because it
     * was not yet a part of a cluster but now is will not have an entry.
     */
    if (timestamp == null) {
      return true;
    }

    if (timestamp.longValue() != recoveryTimestamp) {
      return true;
    }

    return false;
  }

  /**
   * Maintenance mode of a host, service or service component host changed.
   * @param event
   * @throws AmbariException
   */
  @Subscribe
  @AllowConcurrentEvents
  public void handleMaintenanceModeEvent(MaintenanceModeEvent event)
      throws AmbariException {
    if (event.getHost() != null) {
      /*
       * If any one component in the host is recovery enabled,
       * invalidate the host timestamp.
       */
      Cluster cluster = clusters.getCluster(event.getClusterId());
      if (cluster == null) {
        return;
      }

      Host host = event.getHost();
      List<ServiceComponentHost> scHosts = cluster.getServiceComponentHosts(host.getHostName());
      for (ServiceComponentHost sch : scHosts) {
        if (sch.isRecoveryEnabled()) {
          invalidateRecoveryTimestamp(sch.getClusterName(), sch.getHostName());
          break;
        }
      }
    }
    else if (event.getService() != null) {
      /*
       * Simply invalidate all the hosts in the cluster.
       * The recovery config will be sent to all the hosts
       * even if some of the hosts do not have components
       * in recovery mode.
       * Looping through all the hosts and its components
       * to determine which host to send the recovery config
       * may not be efficient.
       */
      Service service = event.getService();
      invalidateRecoveryTimestamp(service.getCluster().getClusterName(), null);
    }
    else if (event.getServiceComponentHost() != null) {
      ServiceComponentHost sch = event.getServiceComponentHost();

      if (sch.isRecoveryEnabled()) {
        invalidateRecoveryTimestamp(sch.getClusterName(), sch.getHostName());
      }
    }
  }

  /**
   * A service component was installed on a host.
   * @param event
   * @throws AmbariException
   */
  @Subscribe
  @AllowConcurrentEvents
  public void handleServiceComponentInstalledEvent(ServiceComponentInstalledEvent event)
      throws AmbariException {
    if (event.isRecoveryEnabled()) {
      Cluster cluster = clusters.getClusterById(event.getClusterId());

      if (cluster != null) {
        invalidateRecoveryTimestamp(cluster.getClusterName(), event.getHostName());
      }
    }
  }

  /**
   * A service component was uninstalled from a host.
   * @param event
   * @throws AmbariException
   */
  @Subscribe
  @AllowConcurrentEvents
  public void handleServiceComponentUninstalledEvent(ServiceComponentUninstalledEvent event)
      throws AmbariException {
    if (event.isRecoveryEnabled()) {
      Cluster cluster = clusters.getClusterById(event.getClusterId());

      if (cluster != null) {
        invalidateRecoveryTimestamp(cluster.getClusterName(), event.getHostName());
      }
    }
  }

  /**
   * Recovery enabled was turned on or off.
   * @param event
   */
  @Subscribe
  @AllowConcurrentEvents
  public void handleServiceComponentRecoveryChangedEvent(ServiceComponentRecoveryChangedEvent event) {
    invalidateRecoveryTimestamp(event.getClusterName(), null);
  }

  /**
   * Cluster-env configuration changed.
   * @param event
   */
  @Subscribe
  @AllowConcurrentEvents
  public void handleClusterEnvConfigChangedEvent(ClusterConfigChangedEvent event) {
    if (StringUtils.equals(event.getConfigType(), ConfigHelper.CLUSTER_ENV)) {
      invalidateRecoveryTimestamp(event.getClusterName(), null);
    }
  }

  private void invalidateRecoveryTimestamp(String clusterName, String hostname) {
    if (StringUtils.isNotEmpty(clusterName)) {
      ConcurrentHashMap<String, Long> hostTimestamp = timestampMap.get(clusterName);
      if (hostTimestamp != null) {
        if (StringUtils.isNotEmpty(hostname)) {
          // Clear the time stamp for the specified host in this cluster
          hostTimestamp.put(hostname, 0L);
        }
        else {
          // Clear the time stamp for all hosts in this cluster
          for(Map.Entry<String, Long> hostEntry : hostTimestamp.entrySet()) {
            hostEntry.setValue(0L);
          }
        }
      }
    }
  }

  /**
   * Helper class to get auto start configuration
   */
  class AutoStartConfig {
    private Cluster cluster;
    private Map<String, String> configProperties;

    public AutoStartConfig(String clusterName)
        throws AmbariException {
      if (StringUtils.isNotEmpty(clusterName)) {
        cluster = clusters.getCluster(clusterName);
      }

      if (cluster != null) {
        Config config = cluster.getDesiredConfigByType(getConfigType());
        if (config != null) {
          configProperties = config.getProperties();
        }
      }

      if (configProperties == null) {
        configProperties = new HashMap<>();
      }
    }
    /**
     * Get a list of enabled components for the specified host and cluster. Filter by
     * Maintenance Mode OFF, so that agent does not auto start components that are in
     * maintenance mode.
     * @return
     */
    private List<RecoveryConfigComponent> getEnabledComponents(String hostname) throws AmbariException {
      List<RecoveryConfigComponent> enabledComponents = new ArrayList<>();

      if (cluster == null) {
        return enabledComponents;
      }

      Host host = clusters.getHost(hostname);
      if (host == null) {
        return enabledComponents;
      }

      // if host is in maintenance mode then ignore all the components for auto start
      if (host.getMaintenanceState(cluster.getClusterId()) == MaintenanceState.ON) {
        return enabledComponents;
      }

      List<ServiceComponentHost> scHosts = cluster.getServiceComponentHosts(hostname);

      for (ServiceComponentHost sch : scHosts) {
        if (sch.isRecoveryEnabled()) {
          Service service = cluster.getService(sch.getServiceName());

          // service should not be in maintenance mode
          if (service.getMaintenanceState() == MaintenanceState.OFF) {
            // Keep the components that are not in maintenance mode.
            if (sch.getMaintenanceState() == MaintenanceState.OFF) {
              enabledComponents.add(new RecoveryConfigComponent(sch));
            }
          }
        }
      }

      return enabledComponents;
    }

    /**
     * The configuration type name.
     * @return
     */
    private String getConfigType() {
      return "cluster-env";
    }

    /**
     * Get a value indicating whether the cluster supports recovery.
     *
     * @return True or false.
     */
    private boolean isRecoveryEnabled() {
      return Boolean.parseBoolean(getProperty(RECOVERY_ENABLED_KEY, "false"));
    }

    /**
     * Get the property value for the specified key. If not present, return default value.
     * @param key The key for which property value is required.
     * @param defaultValue Default value to return if key is not found.
     * @return
     */
    private String getProperty(String key, String defaultValue) {
      if (configProperties.containsKey(key)) {
        return configProperties.get(key);
      }

      return defaultValue;
    }
  }
}
