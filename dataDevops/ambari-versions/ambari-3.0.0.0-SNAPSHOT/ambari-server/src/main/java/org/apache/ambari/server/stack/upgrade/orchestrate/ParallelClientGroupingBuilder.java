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
package org.apache.ambari.server.stack.upgrade.orchestrate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.stack.HostsType;
import org.apache.ambari.server.stack.upgrade.ExecuteHostType;
import org.apache.ambari.server.stack.upgrade.ExecuteTask;
import org.apache.ambari.server.stack.upgrade.Grouping;
import org.apache.ambari.server.stack.upgrade.ParallelClientGrouping;
import org.apache.ambari.server.stack.upgrade.ServiceCheckGrouping.ServiceCheckStageWrapper;
import org.apache.ambari.server.stack.upgrade.Task;
import org.apache.ambari.server.stack.upgrade.Task.Type;
import org.apache.ambari.server.stack.upgrade.UpgradePack.ProcessingComponent;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.collections.CollectionUtils;

/**
 * Responsible for building the stages for {@link ParallelClientGrouping}
 */
public class ParallelClientGroupingBuilder extends StageWrapperBuilder {

  private Map<String, HostHolder> serviceToHostMap = new HashMap<>();

  /**
   * @param grouping
   */
  public ParallelClientGroupingBuilder(Grouping grouping) {
    super(grouping);
  }

  @Override
  public void add(UpgradeContext upgradeContext, HostsType hostsType, String service,
      boolean clientOnly, ProcessingComponent pc, Map<String, String> params) {

    if (null == hostsType || CollectionUtils.isEmpty(hostsType.getHosts())) {
      return;
    }

    Task task = resolveTask(upgradeContext, pc);
    // !!! better not
    if (null == task) {
      return;
    }

    Iterator<String> hostIterator = hostsType.getHosts().iterator();
    HostHolder holder = new HostHolder();
    holder.m_firstHost = hostIterator.next();

    while (hostIterator.hasNext()) {
      holder.m_remainingHosts.add(hostIterator.next());
    }

    holder.m_component = pc.name;
    holder.m_tasks = Collections.singletonList(task);
    holder.m_preTasks = resolveTasks(upgradeContext, true, pc);
    holder.m_postTasks = resolveTasks(upgradeContext, false, pc);

    serviceToHostMap.put(service, holder);
  }

  @Override
  public List<StageWrapper> build(UpgradeContext upgradeContext, List<StageWrapper> stageWrappers) {

    if (0 == serviceToHostMap.size()) {
      return stageWrappers;
    }

    List<StageWrapper> starterUpgrades = new ArrayList<>();
    List<StageWrapper> finisherUpgrades = new ArrayList<>();

    // !!! create a stage wrapper for the first host tasks
    // !!! create a stage wrapper for the service check on the first host
    // !!! create a stage wrapper for the remaining hosts

    // !!! when building stages, the pre- and post- tasks should be run sequentially,
    // which means they should be in their own stages.  that may seem counter-intuitive,
    // but the parallelism is across hosts, not stages.

    serviceToHostMap.forEach((service, holder) -> {

      // !!! pre-tasks for the first host
      starterUpgrades.addAll(buildStageWrappers(upgradeContext, service, holder,
          holder.m_preTasks, true, "Preparing"));

      // !!! upgrades for the first host
      starterUpgrades.addAll(buildStageWrappers(upgradeContext, service, holder,
          holder.m_tasks, true, "Upgrading"));

      // !!! post tasks for the first host
      starterUpgrades.addAll(buildStageWrappers(upgradeContext, service, holder,
          holder.m_postTasks, true, "Completing"));

      // !!! service check for the first host
      StageWrapper serviceCheck = new ServiceCheckStageWrapper(service,
          upgradeContext.getServiceDisplay(service), false, holder.m_firstHost);
      starterUpgrades.add(serviceCheck);

      // !!! pre-tasks for remaining hosts
      finisherUpgrades.addAll(buildStageWrappers(upgradeContext, service, holder,
          holder.m_preTasks, false, "Prepare Remaining"));

      // !!! upgrades for the remaining hosts
      finisherUpgrades.addAll(buildStageWrappers(upgradeContext, service, holder,
          holder.m_tasks, false, "Upgrade Remaining"));

      // !!! post tasks for the remaining hosts
      finisherUpgrades.addAll(buildStageWrappers(upgradeContext, service, holder,
          holder.m_postTasks, false, "Complete Remaining"));
    });

    List<StageWrapper> results = new ArrayList<>(stageWrappers);

    results.addAll(starterUpgrades);
    results.addAll(finisherUpgrades);

    return results;
  }

  /**
   * Builds the stages for the components
   * @param upgradeContext
   *          the upgrade context
   * @param service
   *          the service name
   * @param holder
   *          the holder of component and hosts
   * @param tasks
   *          the tasks to wrap
   * @param firstHost
   *          {@code true} if wrapping for the first host
   * @param prefix
   *          the text prefix for the stage
   * @return
   *          the list of stage wrappers
   */
  private List<StageWrapper> buildStageWrappers(UpgradeContext upgradeContext, String service,
      HostHolder holder, List<Task> tasks, boolean firstHost, String prefix) {

    if (CollectionUtils.isEmpty(tasks)) {
      return Collections.emptyList();
    }

    Set<String> hosts = firstHost ? Collections.singleton(holder.m_firstHost) :
      holder.m_remainingHosts;

    String component = holder.m_component;
    String componentDisplay = upgradeContext.getComponentDisplay(service, component);
    String text = getStageText(prefix, componentDisplay, hosts);

    List<TaskWrapper> wrappers = buildTaskWrappers(service, component, tasks, hosts, firstHost);

    List<StageWrapper> results = new ArrayList<>();

    wrappers.forEach(task -> {
      // !!! there should only be one task per wrapper, so this assumption is ok for now
      StageWrapper.Type type = task.getTasks().get(0).getStageWrapperType();

      StageWrapper stage = new StageWrapper(type, text, new HashMap<>(),
          Collections.singletonList(task));
      results.add(stage);
    });

    return results;
  }

  /**
   * Build the wrappers for the tasks.
   *
   * @param service
   *          the service name
   * @param component
   *          the component name
   * @param tasks
   *          the tasks to wrap
   * @param hosts
   *          the hosts where the tasks should run
   * @param firstHost
   *          {@code true} if these wrappers are for the first host
   * @return
   *        the list if task wrappers
   */
  private List<TaskWrapper> buildTaskWrappers(String service, String component,
      List<Task> tasks, Set<String> hosts, boolean firstHost) {

    List<TaskWrapper> results = new ArrayList<>();

    tasks.forEach(task -> {
      // server side actions should run only run once (the first host)
      if (task.getType().isServerAction()) {
        if (firstHost) {
          String ambariServerHostname = StageUtils.getHostName();
          results.add(new TaskWrapper(service, component, Collections.singleton(ambariServerHostname), task));
        }
        return;
      }

      // FIXME how to handle master-only types

      // !!! the first host has already run tasks that are singular
      if (!firstHost && task.getType() == Type.EXECUTE) {
        ExecuteTask et = (ExecuteTask) task;

        // !!! singular types have already run when firstHost is true
        if (et.hosts != ExecuteHostType.ALL) {
          return;
        }
      }

      results.add(new TaskWrapper(service, component, hosts, task));
    });

    return results;
  }

  /**
   * Temporary holder for building stage wrappers
   */
  private static class HostHolder {
    private String m_component;
    private String m_firstHost;
    private Set<String> m_remainingHosts = new HashSet<>();
    // !!! there will only ever be one, but code is cleaner this way
    private List<Task> m_tasks;
    private List<Task> m_preTasks;
    private List<Task> m_postTasks;
  }

}
