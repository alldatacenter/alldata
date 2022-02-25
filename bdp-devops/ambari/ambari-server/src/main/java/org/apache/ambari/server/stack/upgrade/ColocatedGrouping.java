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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.ambari.server.stack.HostsType;
import org.apache.ambari.server.stack.upgrade.UpgradePack.ProcessingComponent;
import org.apache.ambari.server.stack.upgrade.orchestrate.StageWrapper;
import org.apache.ambari.server.stack.upgrade.orchestrate.StageWrapper.Type;
import org.apache.ambari.server.stack.upgrade.orchestrate.StageWrapperBuilder;
import org.apache.ambari.server.stack.upgrade.orchestrate.TaskWrapper;
import org.apache.ambari.server.stack.upgrade.orchestrate.TaskWrapperBuilder;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContext;
import org.apache.ambari.server.utils.SetUtils;
import org.apache.ambari.spi.upgrade.OrchestrationOptions;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Used for co-located services grouped together.
 */
@XmlType(name="colocated")
public class ColocatedGrouping extends Grouping {

  private static final Logger LOG = LoggerFactory.getLogger(ColocatedGrouping.class);

  @XmlElement(name="batch")
  public Batch batch;


  /**
   * {@inheritDoc}
   */
  @Override
  public StageWrapperBuilder getBuilder() {
    return new MultiHomedBuilder(this, batch, performServiceCheck, parallelScheduler);
  }

  private static class MultiHomedBuilder extends StageWrapperBuilder {

    private Batch m_batch;
    private boolean m_serviceCheck = true;

    // lists of tasks
    private List<TaskProxy> initialBatch = new LinkedList<>();
    private List<TaskProxy> finalBatches = new LinkedList<>();


    private MultiHomedBuilder(Grouping grouping, Batch batch,
        boolean serviceCheck, ParallelScheduler parallel) {
      super(grouping);

      m_batch = batch;
      m_serviceCheck = serviceCheck;
    }

    @Override
    public void add(UpgradeContext context, HostsType hostsType, String service,
        boolean clientOnly, ProcessingComponent pc, Map<String, String> params) {

      // !!! the percent is the number of co-located items that should be run
      // before pausing. This value has no bearing on how many should be in a single stage.
      int count = Double.valueOf(Math.ceil(
          (double) m_batch.percent / 100 * hostsType.getHosts().size())).intValue();

      LinkedHashSet<String> first = new LinkedHashSet<>();
      LinkedHashSet<String> remaining = new LinkedHashSet<>();
      hostsType.getHosts().stream().forEach(hostName -> {
        if (first.size() < count) {
          first.add(hostName);
        } else {
          remaining.add(hostName);
        }
      });

      // !!! resolve tasks only once
      List<Task> preTasks = resolveTasks(context, true, pc);
      Task task = resolveTask(context, pc);
      List<Task> postTasks = resolveTasks(context, false, pc);
      AtomicBoolean processInitial = new AtomicBoolean(true);

      int parallelCount = -1;
      OrchestrationOptions options = context.getOrchestrationOptions();
      if (null != options) {
        parallelCount = context.getOrchestrationOptions().getConcurrencyCount(
            context.getCluster().buildClusterInformation(), service, pc.name);
      }

      if (parallelCount < 1) {
        parallelCount = getParallelHostCount(context, 1);
      }

      // !!! stupid effective-final check
      final int hostCount = parallelCount;

      Stream.of(first, remaining).forEach(hosts -> {

        List<TaskProxy> targetList = processInitial.get() ? initialBatch : finalBatches;

        List<Set<String>> hostSplit = SetUtils.split(hosts, hostCount);

        hostSplit.forEach(hostSet -> {

          List<Task> tasks = preTasks;

          TaskProxy proxy;
          if (CollectionUtils.isNotEmpty(preTasks)) {
            // Our assumption is that all of the tasks in the StageWrapper are of
            // the same type.
            StageWrapper.Type type = preTasks.get(0).getStageWrapperType();

            proxy = new TaskProxy();
            proxy.clientOnly = clientOnly;
            proxy.message = getStageText("Preparing",
                context.getComponentDisplay(service, pc.name), hostSet);
            proxy.tasks.addAll(TaskWrapperBuilder.getTaskList(service, pc.name,
                HostsType.normal(new LinkedHashSet<>(hostSet)), tasks, params));
            proxy.service = service;
            proxy.component = pc.name;
            proxy.type = type;

            targetList.add(proxy);
          }

          if (null != task && RestartTask.class.isInstance(task)) {
            proxy = new TaskProxy();
            proxy.clientOnly = clientOnly;
            proxy.tasks.add(new TaskWrapper(service, pc.name, hostSet, params, task));
            proxy.restart = true;
            proxy.service = service;
            proxy.component = pc.name;
            proxy.type = Type.RESTART;
            proxy.message = getStageText("Restarting",
                context.getComponentDisplay(service, pc.name), hostSet);

            targetList.add(proxy);
          }

          tasks = postTasks;
          if (CollectionUtils.isNotEmpty(preTasks)) {
            // Our assumption is that all of the tasks in the StageWrapper are of
            // the same type.
            StageWrapper.Type type = preTasks.get(0).getStageWrapperType();

            proxy = new TaskProxy();
            proxy.clientOnly = clientOnly;
            proxy.message = getStageText("Completing",
                context.getComponentDisplay(service, pc.name), hostSet);
            proxy.tasks.addAll(TaskWrapperBuilder.getTaskList(service, pc.name,
                HostsType.normal(new LinkedHashSet<>(hostSet)), tasks, params));
            proxy.service = service;
            proxy.component = pc.name;
            proxy.type = type;

            targetList.add(proxy);
          }
        });

        processInitial.set(false);
      });

    }


    /**
     * {@inheritDoc}
     */
    @Override
    public List<StageWrapper> build(UpgradeContext upgradeContext, List<StageWrapper> stageWrappers) {

      final List<Task> visitedServerSideTasks = new ArrayList<>();

      // !!! predicate to ensure server-side tasks are executed once only per grouping
      Predicate<Task> predicate = new Predicate<Task>() {
        @Override
        public boolean apply(Task input) {
          if (visitedServerSideTasks.contains(input)) {
            return false;
          }

          if (input.getType().isServerAction()) {
            visitedServerSideTasks.add(input);
          }

          return true;
        };
      };

      List<StageWrapper> results = new ArrayList<>(stageWrappers);

      if (LOG.isDebugEnabled()) {
        LOG.debug("RU initial: {}", initialBatch);
        LOG.debug("RU final: {}", finalBatches);
      }

      List<StageWrapper> befores = fromProxies(upgradeContext.getDirection(), initialBatch, predicate);
      results.addAll(befores);

      if (!befores.isEmpty()) {

        ManualTask task = new ManualTask();
        task.summary = m_batch.summary;
        List<String> messages = new ArrayList<>();
        messages.add(m_batch.message);
        task.messages = messages;
        formatFirstBatch(upgradeContext, task, befores);

        StageWrapper wrapper = new StageWrapper(
            StageWrapper.Type.SERVER_SIDE_ACTION,
            "Validate Partial " + upgradeContext.getDirection().getText(true),
            new TaskWrapper(null, null, Collections.emptySet(), task));

        results.add(wrapper);
      }

      results.addAll(fromProxies(upgradeContext.getDirection(), finalBatches, predicate));

      return results;
    }

    private List<StageWrapper> fromProxies(Direction direction,
        List<TaskProxy> proxies, Predicate<Task> predicate) {

      List<StageWrapper> results = new ArrayList<>();

      Set<String> serviceChecks = new HashSet<>();


      proxies.forEach(proxy -> {
        StageWrapper wrapper = null;
        List<StageWrapper> execwrappers = new ArrayList<>();

        if (!proxy.clientOnly) {
          serviceChecks.add(proxy.service);
        }

        if (!proxy.restart) {
          TaskWrapper[] tasks = proxy.getTasksArray(predicate);

          if (LOG.isDebugEnabled()) {
            for (TaskWrapper tw : tasks) {
              LOG.debug("{}", tw);
            }
          }

          if (ArrayUtils.isNotEmpty(tasks)) {
            wrapper = new StageWrapper(proxy.type, proxy.message, tasks);
          }
        } else {
          TaskWrapper[] tasks = proxy.getTasksArray(null);

          if (LOG.isDebugEnabled()) {
            for (TaskWrapper tw : tasks) {
              LOG.debug("{}", tw);
            }
          }
          // !!! TODO check parallelism values
          execwrappers.add(new StageWrapper(StageWrapper.Type.RESTART, proxy.message, tasks));
        }

        if (null != wrapper) {
          results.add(wrapper);
        }

        if (execwrappers.size() > 0) {
          results.addAll(execwrappers);
        }
      });

      if (direction.isUpgrade() && m_serviceCheck && serviceChecks.size() > 0) {
        // !!! add the service check task
        List<TaskWrapper> tasks = new ArrayList<>();
        Set<String> displays = new HashSet<>();
        for (String service : serviceChecks) {
          tasks.add(new TaskWrapper(service, "", Collections.emptySet(), new ServiceCheckTask()));
          displays.add(service);
        }

        StageWrapper wrapper = new StageWrapper(
            StageWrapper.Type.SERVICE_CHECK,
            "Service Check " + StringUtils.join(displays, ", "),
            tasks.toArray(new TaskWrapper[tasks.size()]));

        results.add(wrapper);
      }

      return results;
    }

    /**
     * Formats the first batch's text and adds json for use if needed.
     * @param ctx       the upgrade context to load component display names
     * @param task      the manual task representing the verification message
     * @param wrappers  the list of stage wrappers
     */
    private void formatFirstBatch(UpgradeContext ctx, ManualTask task, List<StageWrapper> wrappers) {
      Set<String> names = new LinkedHashSet<>();
      Map<String, Set<String>> compLocations = new HashMap<>();

      for (StageWrapper sw : wrappers) {
        for (TaskWrapper tw : sw.getTasks()) {
          if (StringUtils.isNotEmpty(tw.getService()) &&
              StringUtils.isNotBlank(tw.getComponent())) {

            for (String host : tw.getHosts()) {
              if (!compLocations.containsKey(host)) {
                compLocations.put(host, new HashSet<>());
              }
              compLocations.get(host).add(tw.getComponent());
            }

            names.add(ctx.getComponentDisplay(
                tw.getService(), tw.getComponent()));
          }
        }
      }

      for(int i = 0; i < task.messages.size(); i++){
        String message = task.messages.get(i);
        // !!! add the display names to the message, if needed
        if (message.contains("{{components}}")) {
          StringBuilder sb = new StringBuilder();

          List<String> compNames = new ArrayList<>(names);

          if (compNames.size() == 1) {
            sb.append(compNames.get(0));
          } else if (names.size() > 1) {
            String last = compNames.remove(compNames.size() - 1);
            sb.append(StringUtils.join(compNames, ", "));
            sb.append(" and ").append(last);
          }

          message = message.replace("{{components}}", sb.toString());

          //Add the updated message back to the message list.
          task.messages.set(i, message);
        }
      }

      // !!! build the structured out to attach to the manual task
      JsonArray arr = new JsonArray();
      for (Entry<String, Set<String>> entry : compLocations.entrySet()) {
        JsonObject obj = new JsonObject();
        obj.addProperty("host_name", entry.getKey());

        JsonArray comps = new JsonArray();
        for (String comp : entry.getValue()) {
          comps.add(new JsonPrimitive(comp));
        }
        obj.add("components", comps);

        arr.add(obj);
      }

      JsonObject master = new JsonObject();
      master.add("topology", arr);

      task.structuredOut = master.toString();
    }
  }

  /**
   * Represents all the tasks that need to be run for a host
   */
  private static class TaskProxy {
    private boolean restart = false;
    private String service;
    private String component;
    private String message;
    private Type type;
    private boolean clientOnly = false;
    private List<TaskWrapper> tasks = new ArrayList<>();

    @Override
    public String toString() {
      String s = "";
      for (TaskWrapper t : tasks) {
        s += component + "/" + t.getTasks() + " ";
      }

      return s;
    }

    /**
     * Get the task wrappers for this proxy.  Server-side tasks cannot be executed more than
     * one time per grouping.
     * @param predicate the predicate to determine if a server-side task has already been added to a wrapper.
     * @return the wrappers for a stage
     */
    private TaskWrapper[] getTasksArray(Predicate<Task> predicate) {
      if (null == predicate) {
        return tasks.toArray(new TaskWrapper[tasks.size()]);
      }

      List<TaskWrapper> interim = new ArrayList<>();

      for (TaskWrapper wrapper : tasks) {
        Collection<Task> filtered = Collections2.filter(wrapper.getTasks(), predicate);

        if (CollectionUtils.isNotEmpty(filtered)) {
          interim.add(wrapper);
        }
      }

      return interim.toArray(new TaskWrapper[interim.size()]);
    }
  }

}
