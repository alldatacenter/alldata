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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.stack.HostsType;
import org.apache.ambari.server.stack.upgrade.UpgradePack.ProcessingComponent;
import org.apache.ambari.server.stack.upgrade.orchestrate.StageWrapper;
import org.apache.ambari.server.stack.upgrade.orchestrate.StageWrapperBuilder;
import org.apache.ambari.server.stack.upgrade.orchestrate.TaskWrapper;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContext;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to represent cluster-based operations.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="cluster")
public class ClusterGrouping extends Grouping {
  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(ClusterGrouping.class);

  /**
   * Stages against a Service and Component, or the Server, that doesn't need a Processing Component.
   */
  @XmlElement(name="execute-stage")
  public List<ExecuteStage> executionStages;

  @Override
  public ClusterBuilder getBuilder() {
    return new ClusterBuilder(this);
  }

  @Override
  protected boolean serviceCheckAfterProcessing() {
    return false;
  }

  public class ClusterBuilder extends StageWrapperBuilder {

    /**
     * Constructor.
     *
     * @param grouping
     *          the upgrade/downgrade grouping (not {@code null}).
     */
    private ClusterBuilder(Grouping grouping) {
      super(grouping);
    }

    @Override
    public void add(UpgradeContext ctx, HostsType hostsType, String service,
        boolean clientOnly, ProcessingComponent pc, Map<String, String> params) {
      // !!! no-op in this case
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<StageWrapper> build(UpgradeContext upgradeContext,
        List<StageWrapper> stageWrappers) {

      if (null == executionStages) {
        return stageWrappers;
      }

      List<StageWrapper> results = new ArrayList<>(stageWrappers);

      for (ExecuteStage execution : executionStages) {
        if (null != execution.intendedDirection
            && execution.intendedDirection != upgradeContext.getDirection()) {
          continue;
        }

        // if there is a condition on the group, evaluate it and skip scheduling
        // of this group if the condition has not been satisfied
        if (null != execution.condition && !execution.condition.isSatisfied(upgradeContext)) {
          LOG.info("Skipping {} while building upgrade orchestration due to {}", execution,
              execution.condition);

          continue;
        }

        // only schedule this stage if its service is part of the upgrade
        if (StringUtils.isNotBlank(execution.service)) {
          if (!upgradeContext.isServiceSupported(execution.service)) {
            continue;
          }
        }

        // tasks can have their own condition, so check that too
        if (null != execution.task.condition
            && !execution.task.condition.isSatisfied(upgradeContext)) {
          LOG.info("Skipping {} while building upgrade orchestration due to {}", execution,
              execution.task.condition);

          continue;
        }

        Task task = execution.task;

        StageWrapper wrapper = null;

        switch (task.getType()) {
          case MANUAL:
          case SERVER_ACTION:
          case CONFIGURE:
          case ADD_COMPONENT:
            wrapper = getServerActionStageWrapper(upgradeContext, execution);
            break;

          case EXECUTE:
            wrapper = getExecuteStageWrapper(upgradeContext, execution);
            break;

          case REGENERATE_KEYTABS:
            wrapper = getRegenerateKeytabsWrapper(upgradeContext, execution);
            break;

          default:
            break;
        }

        if (null != wrapper) {
          results.add(wrapper);
        }
      }

      return results;
    }
  }

  /**
   * Return a Stage Wrapper for a server side action that runs on the server.
   * @param ctx Upgrade Context
   * @param execution Execution Stage
   * @return Returns a Stage Wrapper
   */
  private StageWrapper getServerActionStageWrapper(UpgradeContext ctx, ExecuteStage execution) {

    String service   = execution.service;
    String component = execution.component;
    Task task        = execution.task;

    Set<String> realHosts = Collections.emptySet();

    if (StringUtils.isNotEmpty(service) && StringUtils.isNotEmpty(component)) {
      HostsType hosts = ctx.getResolver().getMasterAndHosts(service, component);

      if (null == hosts || hosts.getHosts().isEmpty()) {
        return null;
      } else {
        realHosts = new LinkedHashSet<>(hosts.getHosts());
      }
    }

    if (Task.Type.MANUAL == task.getType()) {
      return new StageWrapper(
          StageWrapper.Type.SERVER_SIDE_ACTION,
          execution.title,
          new TaskWrapper(service, component, realHosts, task));
    } else {
      return new StageWrapper(
          StageWrapper.Type.SERVER_SIDE_ACTION,
          execution.title,
          new TaskWrapper(null, null, Collections.emptySet(), task));
    }
  }

  /**
   * Return a {@link StageWrapper} for regeneration of keytabs.
   *
   * @param ctx
   *          Upgrade Context
   * @param execution
   *          Execution Stage
   * @return a {@link StageWrapper} which will regenerate keytabs on all hosts.
   */
  private StageWrapper getRegenerateKeytabsWrapper(UpgradeContext ctx, ExecuteStage execution) {
    Task task = execution.task;
    HostsType hosts = HostsType.healthy(ctx.getCluster());

    return new StageWrapper(StageWrapper.Type.REGENERATE_KEYTABS, execution.title,
        new TaskWrapper(null, null, hosts.getHosts(), task));
  }

  /**
   * Return a Stage Wrapper for a task meant to execute code, typically on
   * Ambari Server.
   *
   * @param ctx
   *          Upgrade Context
   * @param execution
   *          Execution Stage
   * @return Returns a Stage Wrapper, or null if a valid one could not be
   *         created.
   */
  private StageWrapper getExecuteStageWrapper(UpgradeContext ctx, ExecuteStage execution) {
    String service   = execution.service;
    String component = execution.component;
    ExecuteTask et = (ExecuteTask) execution.task;

    if (StringUtils.isNotBlank(service) && StringUtils.isNotBlank(component)) {

      // !!! if the context is not scoped for the execute-stage, bail
      if (!ctx.isScoped(execution.scope)) {
        return null;
      }

      // !!! if the context is targeted and does not include the service, bail
      if (!ctx.isServiceSupported(service)) {
        return null;
      }

      // !!! FUTURE: check for component

      HostsType hosts = ctx.getResolver().getMasterAndHosts(service, component);

      if (hosts != null) {

        Set<String> realHosts = new LinkedHashSet<>(hosts.getHosts());
        if (ExecuteHostType.MASTER == et.hosts && hosts.hasMasters()) {
          realHosts = hosts.getMasters();
        }

        // Pick a random host.
        if (ExecuteHostType.ANY == et.hosts && !hosts.getHosts().isEmpty()) {
          realHosts = Collections.singleton(hosts.getHosts().iterator().next());
        }

        // Pick the first host sorted alphabetically (case insensitive)
        if (ExecuteHostType.FIRST == et.hosts && !hosts.getHighAvailabilityHosts().isEmpty()) {
          List<String> sortedHosts = new ArrayList<>(hosts.getHosts());
          Collections.sort(sortedHosts, String.CASE_INSENSITIVE_ORDER);
          realHosts = Collections.singleton(sortedHosts.get(0));
        }

        // !!! cannot execute against empty hosts (safety net)
        if (realHosts.isEmpty()) {
          return null;
        }

        return new StageWrapper(
            StageWrapper.Type.UPGRADE_TASKS,
            execution.title,
            new TaskWrapper(service, component, realHosts, et));
      }
    } else if (null == service && null == component) {
      // no service and no component will distributed the task to all healthy
      // hosts not in maintenance mode
      Cluster cluster = ctx.getCluster();
      Set<String> hostNames = new HashSet<>();
      for (Host host : ctx.getCluster().getHosts()) {
        MaintenanceState maintenanceState = host.getMaintenanceState(cluster.getClusterId());
        if (maintenanceState == MaintenanceState.OFF) {
          hostNames.add(host.getHostName());
        }
      }

      return new StageWrapper(
          StageWrapper.Type.UPGRADE_TASKS,
          execution.title,
          new TaskWrapper(service, component, hostNames, et));
    }
    return null;
  }

  /**
   * Attempts to merge the given cluster groupings.  This merges the execute stages
   * in an order specific manner.
   */
  @Override
  public void merge(Iterator<Grouping> iterator) throws AmbariException {
    if (executionStages == null) {
      executionStages = new ArrayList<>();
    }
    Map<String, List<ExecuteStage>> skippedStages = new HashMap<>();
    while (iterator.hasNext()) {
      Grouping next = iterator.next();
      if (!(next instanceof ClusterGrouping)) {
        throw new AmbariException("Invalid group type " + next.getClass().getSimpleName() + " expected cluster group");
      }
      ClusterGrouping clusterGroup = (ClusterGrouping) next;

      boolean added = addGroupingStages(clusterGroup.executionStages, clusterGroup.addAfterGroupEntry);
      if (added) {
        addSkippedStages(skippedStages, clusterGroup.executionStages);
      }
      else {
        // store these services until later
        if (skippedStages.containsKey(next.addAfterGroupEntry)) {
          List<ExecuteStage> tmp = skippedStages.get(clusterGroup.addAfterGroupEntry);
          tmp.addAll(clusterGroup.executionStages);
        }
        else {
          skippedStages.put(clusterGroup.addAfterGroupEntry, clusterGroup.executionStages);
        }
      }
    }
  }

  /**
   * Adds the given stages if the stage they are supposed to come after has been added.
   */
  private boolean addGroupingStages(List<ExecuteStage> stagesToAdd, String after) {
    if (after == null) {
      executionStages.addAll(stagesToAdd);
      return true;
    }
    else {
      // Check the current stages, if the "after" stage is there then add these
      for (int index = executionStages.size() - 1; index >= 0; index--) {
        ExecuteStage stage = executionStages.get(index);
        if ((stage.service != null && stage.service.equals(after)) || stage.title.equals(after)) {
          executionStages.addAll(index + 1, stagesToAdd);
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Adds the skipped stages if the stage they are supposed to come after has been added.
   */
  private void addSkippedStages(Map<String, List<ExecuteStage>> skippedStages, List<ExecuteStage> stagesJustAdded) {
    for (ExecuteStage stage : stagesJustAdded) {
      if (skippedStages.containsKey(stage.service)) {
        List<ExecuteStage> stagesToAdd = skippedStages.remove(stage.service);
        addGroupingStages(stagesToAdd, stage.service);
        addSkippedStages(skippedStages, stagesToAdd);
      }
    }
  }
}
