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
package org.apache.ambari.server.topology.tasks;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.events.ClusterConfigFinishedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.security.authorization.internal.RunWithInternalSecurityContext;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.topology.AsyncCallableService;
import org.apache.ambari.server.topology.ClusterConfigurationRequest;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.HostGroupInfo;
import org.apache.ambari.server.topology.TopologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class ConfigureClusterTask implements Callable<Boolean> {

  private static final long DEFAULT_TIMEOUT = TimeUnit.MINUTES.toMillis(30);
  private static final long REPEAT_DELAY = TimeUnit.SECONDS.toMillis(1);
  private static final String TIMEOUT_PROPERTY_NAME = "cluster_configure_task_timeout";
  private static final Logger LOG = LoggerFactory.getLogger(ConfigureClusterTask.class);

  private final ClusterConfigurationRequest configRequest;
  private final ClusterTopology topology;
  private final AmbariEventPublisher ambariEventPublisher;
  private final Map<String, Integer> previousHostCounts = Maps.newHashMap();
  private final Set<String> missingHostGroups = Sets.newHashSet();

  @AssistedInject
  public ConfigureClusterTask(@Assisted ClusterTopology topology, @Assisted ClusterConfigurationRequest configRequest,
                              @Assisted AmbariEventPublisher ambariEventPublisher) {
    this.configRequest = configRequest;
    this.topology = topology;
    this.ambariEventPublisher = ambariEventPublisher;
  }

  @Override
  @RunWithInternalSecurityContext(token = TopologyManager.INTERNAL_AUTH_TOKEN)
  public Boolean call() throws Exception {
    LOG.debug("Entering");

    Collection<String> requiredHostGroups = getTopologyRequiredHostGroups();

    if (!areHostGroupsResolved(requiredHostGroups)) {
      String msg = "Some host groups require more hosts, cluster configuration cannot begin";
      LOG.info(msg);
      throw new AsyncCallableService.RetryTaskSilently(msg);
    }

    LOG.info("All required host groups are complete, cluster configuration can now begin");
    configRequest.process();
    LOG.info("Cluster configuration finished successfully");

    notifyListeners();

    LOG.debug("Exiting");
    return true;
  }

  public long getTimeout() {
    long timeout = DEFAULT_TIMEOUT;

    String timeoutStr = topology.getConfiguration().getPropertyValue(ConfigHelper.CLUSTER_ENV, TIMEOUT_PROPERTY_NAME);
    if (timeoutStr != null) {
      try {
        timeout = Long.parseLong(timeoutStr);
        LOG.info("Using custom timeout: {} ms", timeout);
      } catch (NumberFormatException e) {
        // use default
      }
    }

    return timeout;
  }

  public long getRepeatDelay() {
    return REPEAT_DELAY;
  }

  /**
   * Return the set of host group names which are required for configuration topology resolution.
   */
  private Collection<String> getTopologyRequiredHostGroups() {
    try {
      return configRequest.getRequiredHostGroups();
    } catch (RuntimeException e) {
      // just log error and allow config topology update
      LOG.error("Could not determine required host groups", e);
      return Collections.emptyList();
    }
  }

  /**
   * Determine if all hosts for the given set of required host groups are known.
   *
   * @param requiredHostGroups set of required host groups
   * @return true if all required host groups are resolved
   */
  private boolean areHostGroupsResolved(Collection<String> requiredHostGroups) {
    boolean allHostGroupsResolved = true;
    Map<String, HostGroupInfo> hostGroupInfo = topology.getHostGroupInfo();
    for (String hostGroup : requiredHostGroups) {
      HostGroupInfo groupInfo = hostGroupInfo.get(hostGroup);
      if (groupInfo == null) {
        allHostGroupsResolved = false;
        if (missingHostGroups.add(hostGroup)) {
          LOG.warn("Host group '{}' is missing from cluster creation request", hostGroup);
        }
      } else {
        int actualHostCount = groupInfo.getHostNames().size();
        int requestedHostCount = groupInfo.getRequestedHostCount();
        boolean hostGroupReady = actualHostCount >= requestedHostCount;
        allHostGroupsResolved &= hostGroupReady;

        Integer previousHostCount = previousHostCounts.put(hostGroup, actualHostCount);
        if (previousHostCount == null || previousHostCount != actualHostCount) {
          if (hostGroupReady) {
            LOG.info("Host group '{}' resolved, requires {} hosts and {} are available",
              groupInfo.getHostGroupName(), requestedHostCount, actualHostCount
            );
          } else {
            LOG.info("Host group '{}' pending, requires {} hosts, but only {} are available",
              groupInfo.getHostGroupName(), requestedHostCount, actualHostCount
            );
          }
        }
      }
    }

    return allHostGroupsResolved;
  }

  private void notifyListeners() throws AmbariException {
    long clusterId = topology.getClusterId();
    String clusterName = topology.getAmbariContext().getClusterName(clusterId);
    ambariEventPublisher.publish(new ClusterConfigFinishedEvent(clusterId, clusterName));
  }

}
