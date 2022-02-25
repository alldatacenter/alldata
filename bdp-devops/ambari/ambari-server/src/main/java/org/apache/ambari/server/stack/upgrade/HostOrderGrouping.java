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
package org.apache.ambari.server.stack.upgrade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlType;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.metadata.RoleCommandOrder;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.stack.HostsType;
import org.apache.ambari.server.stack.upgrade.UpgradePack.ProcessingComponent;
import org.apache.ambari.server.stack.upgrade.orchestrate.StageWrapper;
import org.apache.ambari.server.stack.upgrade.orchestrate.StageWrapperBuilder;
import org.apache.ambari.server.stack.upgrade.orchestrate.TaskWrapper;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContext;
import org.apache.ambari.server.stageplanner.RoleGraph;
import org.apache.ambari.server.stageplanner.RoleGraphFactory;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

/**
 * Marker group for Host-Ordered upgrades.
 */
@XmlType(name="host-order")
public class HostOrderGrouping extends Grouping {
  private static final String TYPE = "type";
  private static final String HOST = "host";
  private static final Logger LOG = LoggerFactory.getLogger(HostOrderGrouping.class);

  /**
   * Contains the ordered actions to schedule for this grouping.
   */
  private List<HostOrderItem> m_hostOrderItems;

  /**
   * Constructor
   */
  public HostOrderGrouping() {
  }

  /**
   * Sets the {@link HostOrderItem}s on this grouping.
   *
   * @param hostOrderItems
   */
  public void setHostOrderItems(List<HostOrderItem> hostOrderItems) {
    m_hostOrderItems = hostOrderItems;
  }

  @Override
  public StageWrapperBuilder getBuilder() {
    return new HostBuilder(this);
  }

  /**
   * Builder for host upgrades.
   */
  private static class HostBuilder extends StageWrapperBuilder {
    private final List<HostOrderItem> hostOrderItems;

    /**
     * @param grouping the grouping
     */
    protected HostBuilder(HostOrderGrouping grouping) {
      super(grouping);
      hostOrderItems = grouping.m_hostOrderItems;
    }

    @Override
    public void add(UpgradeContext upgradeContext, HostsType hostsType, String service,
        boolean clientOnly, ProcessingComponent pc, Map<String, String> params) {
      // !!! NOOP, this called when there are services in the group, and there
      // are none for host-ordered.
    }

    @Override
    public List<StageWrapper> build(UpgradeContext upgradeContext,
        List<StageWrapper> stageWrappers) {

      List<StageWrapper> wrappers = new ArrayList<>(stageWrappers);

      for (HostOrderItem orderItem : hostOrderItems) {
        switch (orderItem.getType()) {
          case HOST_UPGRADE:
            wrappers.addAll(buildHosts(upgradeContext, orderItem.getActionItems()));
            break;
          case SERVICE_CHECK:
            wrappers.addAll(buildServiceChecks(upgradeContext, orderItem.getActionItems()));
            break;
        }
      }

      return wrappers;
    }

    /**
     * Builds the stages for each host which typically consist of a STOP, a
     * manual wait, and a START. The starting of components can be a single
     * stage or may consist of several stages if the host components have
     * dependencies on each other.
     *
     * @param upgradeContext
     *          the context
     * @param hosts
     *          the list of hostnames
     * @return the wrappers for a host
     */
    private List<StageWrapper> buildHosts(UpgradeContext upgradeContext, List<String> hosts) {
      if (CollectionUtils.isEmpty(hosts)) {
        return Collections.emptyList();
      }

      Cluster cluster = upgradeContext.getCluster();
      List<StageWrapper> wrappers = new ArrayList<>();

      HostRoleCommandFactory hrcFactory = upgradeContext.getHostRoleCommandFactory();

      // get a role command order instance that we can adjust for HOU since HOU
      // may use a different ordering than normal start operations
      RoleCommandOrder roleCommandOrder = getRoleCommandOrderForUpgrade(cluster);

      for (String hostName : hosts) {
        // initialize the collection for all stop tasks for every component on
        // the host
        List<TaskWrapper> stopTasks = new ArrayList<>();

        // initialize the collection which will be passed into the RoleGraph for
        // ordering
        Map<String, Map<String, HostRoleCommand>> restartCommandsForHost = new HashMap<>();
        Map<String, HostRoleCommand> restartCommandsByRole = new HashMap<>();
        restartCommandsForHost.put(hostName, restartCommandsByRole);

        // iterating over every host component, build the commands
        for (ServiceComponentHost sch : cluster.getServiceComponentHosts(hostName)) {
          if (!isVersionAdvertised(upgradeContext, sch)) {
            continue;
          }

          HostsType hostsType = upgradeContext.getResolver().getMasterAndHosts(
              sch.getServiceName(), sch.getServiceComponentName());

          // !!! if the hosts do not contain the current one, that means the component
          // either doesn't exist or the downgrade is to the current target version.
          // hostsType better not be null either, but check anyway
          if (null != hostsType && !hostsType.getHosts().contains(hostName)) {
            RepositoryVersionEntity targetRepositoryVersion = upgradeContext.getTargetRepositoryVersion(
                sch.getServiceName());

            LOG.warn("Host {} could not be orchestrated. Either there are no components for {}/{} " +
                "or the target version {} is already current.",
                hostName, sch.getServiceName(), sch.getServiceComponentName(),
                targetRepositoryVersion.getVersion());

            continue;
          }

          // create a STOP task for this host component
          if (!sch.isClientComponent()) {
            stopTasks.add(new TaskWrapper(sch.getServiceName(), sch.getServiceComponentName(),
                Collections.singleton(hostName), new StopTask()));
          }

          // generate a placeholder HRC that can be used to generate the
          // dependency graph - we must use START here since that's what the
          // role command order is defined with - each of these will turn into a
          // RESTART when we create the wrappers later on
          Role role = Role.valueOf(sch.getServiceComponentName());
          HostRoleCommand hostRoleCommand = hrcFactory.create(hostName, role, null,
              RoleCommand.START);

          // add the newly created HRC RESTART
          restartCommandsByRole.put(role.name(), hostRoleCommand);
        }

        // short circuit and move to the next host if there are no commands
        if (stopTasks.isEmpty() && restartCommandsByRole.isEmpty()) {
          LOG.info("There were no {} commands generated for {}",
              upgradeContext.getDirection().getText(false), hostName);

          continue;
        }

        // now process the HRCs created so that we can create the appropriate
        // stage/task wrappers for the RESTARTs
        RoleGraphFactory roleGraphFactory = upgradeContext.getRoleGraphFactory();
        RoleGraph roleGraph = roleGraphFactory.createNew(roleCommandOrder);
        List<Map<String, List<HostRoleCommand>>> stages = roleGraph.getOrderedHostRoleCommands(
            restartCommandsForHost);

        // initialize the list of stage wrappers
        List<StageWrapper> stageWrappers = new ArrayList<>();

        // for every stage, create a stage wrapper around the tasks
        int phaseCounter = 1;
        for (Map<String, List<HostRoleCommand>> stage : stages) {
          List<HostRoleCommand> stageCommandsForHost = stage.get(hostName);
          String stageTitle = String.format("Starting components on %s (phase %d)", hostName,
              phaseCounter++);

          // create task wrappers
          List<TaskWrapper> taskWrappers = new ArrayList<>();
          for (HostRoleCommand command : stageCommandsForHost) {
            StackId stackId = upgradeContext.getRepositoryVersion().getStackId();
            String componentName = command.getRole().name();

            String serviceName = null;

            try {
              AmbariMetaInfo ambariMetaInfo = upgradeContext.getAmbariMetaInfo();
              serviceName = ambariMetaInfo.getComponentToService(stackId.getStackName(),
                  stackId.getStackVersion(), componentName);
            } catch (AmbariException ambariException) {
              LOG.error("Unable to lookup service by component {} for stack {}-{}", componentName,
                  stackId.getStackName(), stackId.getStackVersion());
            }

            TaskWrapper taskWrapper = new TaskWrapper(serviceName, componentName,
                Collections.singleton(hostName), new RestartTask());

            taskWrappers.add(taskWrapper);
          }

          if (!taskWrappers.isEmpty()) {
            StageWrapper startWrapper = new StageWrapper(StageWrapper.Type.RESTART, stageTitle,
                taskWrappers.toArray(new TaskWrapper[taskWrappers.size()]));

            stageWrappers.add(startWrapper);
          }
        }

        // create the manual task between the STOP and START stages
        ManualTask mt = new ManualTask();
        String message = String.format("Please acknowledge that host %s has been prepared.", hostName);
        mt.messages.add(message);

        JsonObject structuredOut = new JsonObject();
        structuredOut.addProperty(TYPE, HostOrderItem.HostOrderActionType.HOST_UPGRADE.toString());
        structuredOut.addProperty(HOST, hostName);
        mt.structuredOut = structuredOut.toString();

        // build the single STOP stage, but only if there are components to
        // stop; client-only hosts have no components which need stopping
        if (!stopTasks.isEmpty()) {
          StageWrapper stopWrapper = new StageWrapper(StageWrapper.Type.STOP,
              String.format("Stop on %s", hostName),
              stopTasks.toArray(new TaskWrapper[stopTasks.size()]));

          wrappers.add(stopWrapper);
        }

        StageWrapper manualWrapper = new StageWrapper(StageWrapper.Type.SERVER_SIDE_ACTION, "Manual Confirmation",
            new TaskWrapper(null, null, Collections.emptySet(), mt));

        wrappers.add(manualWrapper);

        // !!! TODO install_packages for hdp and conf-select changes.  Hopefully these will no-op.
        wrappers.addAll(stageWrappers);
      }

      return wrappers;
    }

    /**
     * @param upgradeContext  the context
     * @return  the wrappers for a host
     */
    private List<StageWrapper> buildServiceChecks(UpgradeContext upgradeContext, List<String> serviceChecks) {
      if (CollectionUtils.isEmpty(serviceChecks)) {
        return Collections.emptyList();
      }

      List<StageWrapper> wrappers = new ArrayList<>();

      Cluster cluster = upgradeContext.getCluster();

      for (String serviceName : serviceChecks) {
        boolean hasService = false;
        try {
          cluster.getService(serviceName);
          hasService = true;
        } catch (Exception e) {
          LOG.warn("Service {} not found to orchestrate", serviceName);
        }

        if (!hasService) {
          continue;
        }

        StageWrapper wrapper = new StageWrapper(StageWrapper.Type.SERVICE_CHECK,
            String.format("Service Check %s", upgradeContext.getServiceDisplay(serviceName)),
            new TaskWrapper(serviceName, "", Collections.emptySet(), new ServiceCheckTask()));

        wrappers.add(wrapper);
      }

      return wrappers;
    }


    /**
     * @param upgradeContext  the context
     * @param sch             the host component
     * @return                {@code true} if the host component advertises its version
     */
    private boolean isVersionAdvertised(UpgradeContext upgradeContext, ServiceComponentHost sch) {
      RepositoryVersionEntity targetRepositoryVersion = upgradeContext.getTargetRepositoryVersion(
          sch.getServiceName());

      StackId targetStack = targetRepositoryVersion.getStackId();

      try {
        ComponentInfo component = upgradeContext.getAmbariMetaInfo().getComponent(
            targetStack.getStackName(), targetStack.getStackVersion(),
            sch.getServiceName(), sch.getServiceComponentName());

        return component.isVersionAdvertised();
      } catch (AmbariException e) {
       LOG.warn("Could not determine if {}/{}/{} could be upgraded; returning false",
           targetStack, sch.getServiceName(), sch.getServiceComponentName(), e);
       return false;
      }
    }

    /**
     * Gets a {@link RoleCommandOrder} instance initialized with
     * {@code host_ordered_upgrade} overrides.
     *
     * @param cluster
     *          the cluster to get the {@link RoleCommandOrder} instance for.
     * @return the order of commands for the cluster
     */
    private RoleCommandOrder getRoleCommandOrderForUpgrade(Cluster cluster) {
      RoleCommandOrder roleCommandOrder = cluster.getRoleCommandOrder();

      try {
        roleCommandOrder = (RoleCommandOrder) roleCommandOrder.clone();
      } catch (CloneNotSupportedException cloneNotSupportedException) {
        LOG.warn("Unable to clone role command order and apply overrides for this upgrade",
            cloneNotSupportedException);
      }

      LinkedHashSet<String> sectionKeys = roleCommandOrder.getSectionKeys();
      sectionKeys.add("host_ordered_upgrade");

      roleCommandOrder.initialize(cluster, sectionKeys);
      return roleCommandOrder;
    }
  }
}
