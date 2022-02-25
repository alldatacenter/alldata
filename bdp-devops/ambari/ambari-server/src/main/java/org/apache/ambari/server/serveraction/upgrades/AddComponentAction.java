/**
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
package org.apache.ambari.server.serveraction.upgrades;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.events.listeners.upgrade.StackVersionListener;
import org.apache.ambari.server.stack.MasterHostResolver;
import org.apache.ambari.server.stack.upgrade.AddComponentTask;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContext;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.State;

import com.google.gson.Gson;

/**
 * The {@link AddComponentAction} is used to add a component during an upgrade.
 */
public class AddComponentAction extends AbstractUpgradeServerAction {

  /**
   * {@inheritDoc}
   */
  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
      throws AmbariException, InterruptedException {

    Map<String, String> commandParameters = getCommandParameters();
    if (null == commandParameters || commandParameters.isEmpty()) {
      return createCommandReport(0, HostRoleStatus.FAILED, "{}", "",
          "Unable to add a new component to the cluster as there is no information on what to add.");
    }

    String clusterName = commandParameters.get("clusterName");
    Cluster cluster = getClusters().getCluster(clusterName);
    UpgradeContext upgradeContext = getUpgradeContext(cluster);

    // guard against downgrade until there is such a thing as removal of a
    // component on downgrade
    if (upgradeContext.isDowngradeAllowed() || upgradeContext.isPatchRevert()) {
      return createCommandReport(0, HostRoleStatus.SKIPPED_FAILED, "{}", "",
          "Unable to add a component during an upgrade which can be downgraded.");
    }

    String serializedJson = commandParameters.get(
        AddComponentTask.PARAMETER_SERIALIZED_ADD_COMPONENT_TASK);

    Gson gson = getGson();
    AddComponentTask task = gson.fromJson(serializedJson, AddComponentTask.class);

    final Service service;
    try {
      service = cluster.getService(task.service);
    } catch (ServiceNotFoundException snfe) {
      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", "",
          String.format(
              "%s was not installed in this cluster since %s is not an installed service.",
              task.component, task.service));
    }


    // build the list of candidate hosts
    Collection<Host> candidates = MasterHostResolver.getCandidateHosts(cluster, task.hosts,
        task.hostService, task.hostComponent);

    if (candidates.isEmpty()) {
      return createCommandReport(0, HostRoleStatus.FAILED, "{}", "", String.format(
          "Unable to add a new component to the cluster as there are no hosts which contain %s's %s",
          task.hostService, task.hostComponent));
    }

    // create the component if it doesn't exist in the service yet
    ServiceComponent serviceComponent;
    try {
      serviceComponent = service.getServiceComponent(task.component);
    } catch (ServiceComponentNotFoundException scnfe) {
      serviceComponent = service.addServiceComponent(task.component);
      serviceComponent.setDesiredState(State.INSTALLED);
    }

    StringBuilder buffer = new StringBuilder(String.format(
        "Successfully added %s's %s to the cluster", task.service, task.component)).append(
            System.lineSeparator());

    Map<String, ServiceComponentHost> existingSCHs = serviceComponent.getServiceComponentHosts();

    for (Host host : candidates) {
      if (existingSCHs.containsKey(host.getHostName())) {
        buffer.append("  ")
        .append(host.getHostName())
        .append(": ")
        .append("Already Installed")
        .append(System.lineSeparator());

        continue;
      }

      ServiceComponentHost sch = serviceComponent.addServiceComponentHost(host.getHostName());
      sch.setDesiredState(State.INSTALLED);
      sch.setState(State.INSTALLED);

      // for now, this is the easiest way to fire a topology event which
      // refreshes the information about the cluster (needed for restart
      // commands)
      sch.setVersion(StackVersionListener.UNKNOWN_VERSION);

      buffer.append("  ")
        .append(host.getHostName())
        .append(": ")
        .append("Installed")
        .append(System.lineSeparator());
    }

    Set<String> sortedHosts = candidates.stream().map(host -> host.getHostName()).collect(
        Collectors.toCollection(() -> new TreeSet<>()));

    Map<String, Object> structureOutMap = new LinkedHashMap<>();
    structureOutMap.put("service", task.service);
    structureOutMap.put("component", task.component);
    structureOutMap.put("hosts", sortedHosts);

    return createCommandReport(0, HostRoleStatus.COMPLETED, gson.toJson(structureOutMap),
        buffer.toString(), "");
  }
}
