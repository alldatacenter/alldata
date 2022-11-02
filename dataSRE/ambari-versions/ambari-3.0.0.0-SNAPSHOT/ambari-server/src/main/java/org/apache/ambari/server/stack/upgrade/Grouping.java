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

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.stack.HostsType;
import org.apache.ambari.server.stack.upgrade.UpgradePack.OrderService;
import org.apache.ambari.server.stack.upgrade.UpgradePack.ProcessingComponent;
import org.apache.ambari.server.stack.upgrade.orchestrate.StageWrapper;
import org.apache.ambari.server.stack.upgrade.orchestrate.StageWrapperBuilder;
import org.apache.ambari.server.stack.upgrade.orchestrate.TaskWrapper;
import org.apache.ambari.server.stack.upgrade.orchestrate.TaskWrapperBuilder;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContext;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.utils.SetUtils;
import org.apache.commons.lang.StringUtils;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.google.common.base.MoreObjects;

/**
 *
 */
@XmlSeeAlso(value = { ColocatedGrouping.class, ClusterGrouping.class,
    UpdateStackGrouping.class, ServiceCheckGrouping.class, RestartGrouping.class,
    StartGrouping.class, StopGrouping.class, HostOrderGrouping.class, ParallelClientGrouping.class })
public class Grouping {

  private static final String RACKS_YAML_KEY_NAME = "racks";
  private static final String HOSTS_YAML_KEY_NAME = "hosts";
  private static final String HOST_GROUPS_YAML_KEY_NAME = "hostGroups";

  @XmlAttribute(name="name")
  public String name;

  @XmlAttribute(name="title")
  public String title;

  @XmlElement(name="add-after-group")
  public String addAfterGroup;

  @XmlElement(name="add-after-group-entry")
  public String addAfterGroupEntry;

  @XmlElement(name="skippable", defaultValue="false")
  public boolean skippable = false;

  @XmlElement(name = "supports-auto-skip-failure", defaultValue = "true")
  public boolean supportsAutoSkipOnFailure = true;

  @XmlElement(name="allow-retry", defaultValue="true")
  public boolean allowRetry = true;

  @XmlElement(name="service")
  public List<UpgradePack.OrderService> services = new ArrayList<>();

  @XmlElement(name="service-check", defaultValue="true")
  public boolean performServiceCheck = true;

  @XmlElement(name="direction")
  public Direction intendedDirection = null;

  @XmlElement(name="parallel-scheduler")
  public ParallelScheduler parallelScheduler;

  @XmlElement(name="scope")
  public UpgradeScope scope = UpgradeScope.ANY;

  /**
   * A condition element with can prevent this entire group from being scheduled
   * in the upgrade.
   */
  @XmlElement(name = "condition")
  public Condition condition;

  /**
   * @return {@code true} when the grouping is used to upgrade services and that it is
   * appropriate to run service checks after orchestration.
   */
  public final boolean isProcessingGroup() {
    return serviceCheckAfterProcessing();
  }

  /**
   * Overridable function to indicate if full service checks can be run
   */
  protected boolean serviceCheckAfterProcessing() {
    return true;
  }

  /**
   * Gets the default builder.
   */
  public StageWrapperBuilder getBuilder() {
    return new DefaultBuilder(this, performServiceCheck);
  }

  private static class DefaultBuilder extends StageWrapperBuilder {

    private List<StageWrapper> m_stages = new ArrayList<>();
    private Set<String> m_servicesToCheck = new HashSet<>();
    private boolean m_serviceCheck = true;

    private DefaultBuilder(Grouping grouping, boolean serviceCheck) {
      super(grouping);
      m_serviceCheck = serviceCheck;
    }

    /**
     * Add stages where the restart stages are ordered
     * E.g., preupgrade, restart hosts(0), ..., restart hosts(n-1), postupgrade
     * @param context the context
     * @param hostsType the order collection of hosts, which may have a master and secondary
     * @param service the service name
     * @param pc the ProcessingComponent derived from the upgrade pack.
     * @param params additional parameters
     */
    @Override
    public void add(UpgradeContext context, HostsType hostsType, String service,
       boolean clientOnly, ProcessingComponent pc, Map<String, String> params) {

      // Construct the pre tasks during Upgrade/Downgrade direction.
      // Buckets are grouped by the type, e.g., bucket of all Execute tasks, or all Configure tasks.
      List<TaskBucket> buckets = buckets(resolveTasks(context, true, pc));
      for (TaskBucket bucket : buckets) {
        // The TaskWrappers take into account if a task is meant to run on all, any, or master.
        // A TaskWrapper may contain multiple tasks, but typically only one, and they all run on the same set of hosts.
        // Generate a task wrapper for every task in the bucket
        List<TaskWrapper> preTasks = TaskWrapperBuilder.getTaskList(service, pc.name, hostsType, bucket.tasks, params);
        List<List<TaskWrapper>> organizedTasks = organizeTaskWrappersBySyncRules(preTasks);
        for (List<TaskWrapper> tasks : organizedTasks) {
          addTasksToStageInBatches(tasks, "Preparing", context, service, pc, params);
        }
      }

      // Add the processing component
      Task t = resolveTask(context, pc);
      if (null != t) {
        TaskWrapper tw = new TaskWrapper(service, pc.name, hostsType.getHosts(), params, Collections.singletonList(t));
        addTasksToStageInBatches(Collections.singletonList(tw), t.getActionVerb(), context, service, pc, params);
      }

      // Construct the post tasks during Upgrade/Downgrade direction.
      buckets = buckets(resolveTasks(context, false, pc));
      for (TaskBucket bucket : buckets) {
        List<TaskWrapper> postTasks = TaskWrapperBuilder.getTaskList(service, pc.name, hostsType, bucket.tasks, params);
        List<List<TaskWrapper>> organizedTasks = organizeTaskWrappersBySyncRules(postTasks);
        for (List<TaskWrapper> tasks : organizedTasks) {
          addTasksToStageInBatches(tasks, "Completing", context, service, pc, params);
        }
      }

      // Potentially add a service check
      if (m_serviceCheck && !clientOnly) {
        m_servicesToCheck.add(service);
      }
    }

    /**
     * Split a list of TaskWrappers into a list of lists where a TaskWrapper that has any task with isSequential == true
     * must be a singleton in its own list.
     * @param tasks List of TaskWrappers to analyze
     * @return List of list of TaskWrappers, where each outer list is a separate stage.
     */
    private List<List<TaskWrapper>> organizeTaskWrappersBySyncRules(List<TaskWrapper> tasks) {
      List<List<TaskWrapper>> groupedTasks = new ArrayList<>();

      List<TaskWrapper> subTasks = new ArrayList<>();
      for (TaskWrapper tw : tasks) {
        // If an of this TaskWrapper's tasks must be on its own stage, write out the previous subtasks if possible into one complete stage.
        if (tw.isAnyTaskSequential()) {
          if (!subTasks.isEmpty()) {
            groupedTasks.add(subTasks);
            subTasks = new ArrayList<>();
          }
          groupedTasks.add(Collections.singletonList(tw));
        } else {
          subTasks.add(tw);
        }
      }

      if (!subTasks.isEmpty()) {
        groupedTasks.add(subTasks);
      }

      return groupedTasks;
    }

    /**
     * Helper function to analyze a ProcessingComponent and add its task to stages, depending on the batch size.
     * @param tasks Collection of tasks for this stage
     * @param verb Verb string to use in the title of the task
     * @param ctx Upgrade Context
     * @param service Service
     * @param pc Processing Component
     * @param params Params to add to the stage.
     */
    private void addTasksToStageInBatches(List<TaskWrapper> tasks, String verb, UpgradeContext ctx, String service,
                                          ProcessingComponent pc, Map<String, String> params) {
      if (tasks == null || tasks.isEmpty() || tasks.get(0).getTasks() == null || tasks.get(0).getTasks().isEmpty()) {
        return;
      }

      // Our assumption is that all of the tasks in the StageWrapper are of the same type.
      StageWrapper.Type type = tasks.get(0).getTasks().get(0).getStageWrapperType();

      // Expand some of the TaskWrappers into multiple based on the batch size.
      for (TaskWrapper tw : tasks) {
        List<Set<String>> hostSets = null;

        int parallel = getParallelHostCount(ctx, 1);
        final String rackYamlFile =
                ctx.getResolver().getValueFromDesiredConfigurations(ConfigHelper.CLUSTER_ENV, "rack_yaml_file_path");
        if (StringUtils.isNotEmpty(rackYamlFile)) {
          // If rack to hosts mapping yaml file path is present in cluster-env property: rack_yaml_file_path,
          // host sets will be formed based on rack i.e. based on parallel value, hosts present on same rack will
          // be part of the same batch. This is useful when we want to avoid possibility of single rack failure
          Map<String, Set<String>> hostsByRack = organizeHostsByRack(tw.getHosts(), rackYamlFile);
          List<Set<String>> hostSetsForRack;
          for (String rack : hostsByRack.keySet()) {
            hostSetsForRack = SetUtils.split(hostsByRack.get(rack), parallel);
            if (hostSets == null) {
              hostSets = hostSetsForRack;
            } else {
              hostSets.addAll(hostSetsForRack);
            }
          }
        } else {
          hostSets = SetUtils.split(tw.getHosts(), parallel);
        }
        int numBatchesNeeded = hostSets.size();
        int batchNum = 0;
        for (Set<String> hostSubset : hostSets) {
          batchNum++;

          String stageText = getStageText(verb, ctx.getComponentDisplay(service, pc.name), hostSubset, batchNum, numBatchesNeeded);

          StageWrapper stage = new StageWrapper(
              type,
              stageText,
              params,
              new TaskWrapper(service, pc.name, hostSubset, params, tw.getTasks()));
          m_stages.add(stage);
        }
      }
    }

    /**
     * Utility method to organize and return Rack to Hosts mapping for given rack yaml file
     *
     * @param hosts        : All hosts that are part of current group
     * @param rackYamlFile : file path for yaml containing rack to hosts mapping
     *                        e.g ambari-server/src/examples/rack_hosts.yaml
     * @return
     */
    private Map<String, Set<String>> organizeHostsByRack(Set<String> hosts, String rackYamlFile) {
      try {
        Map<String, String> hostToRackMap = getHostToRackMap(rackYamlFile);
        Map<String, Set<String>> rackToHostsMap = new HashMap<>();
        for (String host : hosts) {
          if (hostToRackMap.containsKey(host)) {
            String rack = hostToRackMap.get(host);
            if (!rackToHostsMap.containsKey(rack)) {
              rackToHostsMap.put(rack, new HashSet<>());
            }
            rackToHostsMap.get(rack).add(host);
          } else {
            throw new RuntimeException(String.format("Rack mapping is not present for host name: %s", host));
          }
        }
        return rackToHostsMap;
      } catch (Exception e) {
        throw new RuntimeException(
                String.format("Failed to generate Rack to Hosts mapping. filePath: %s", rackYamlFile), e);
      }
    }

    private static Map<String, String> getHostToRackMap(String rackYamlFile)
            throws IOException {
      YamlReader yamlReader = new YamlReader(new FileReader(rackYamlFile));
      Map rackHostsMap;
      try {
        rackHostsMap = (Map) yamlReader.read();
      } finally {
        yamlReader.close();
      }
      Map racks = (Map) rackHostsMap.get(RACKS_YAML_KEY_NAME);
      Map<String, String> hostToRackMap = new HashMap<>();
      for (Map.Entry entry : (Set<Map.Entry>) racks.entrySet()) {
        Map rackInfoMap = (Map) entry.getValue();
        String rackName = (String) entry.getKey();
        if (rackInfoMap.containsKey(HOSTS_YAML_KEY_NAME)) {
          List<String> hostList = (List<String>) rackInfoMap.get(HOSTS_YAML_KEY_NAME);
          for (String host : hostList) {
            hostToRackMap.put(host, rackName);
          }
        }
        if (rackInfoMap.containsKey(HOST_GROUPS_YAML_KEY_NAME)) {
          List<Map> hostGroups = (List<Map>) rackInfoMap.get(HOST_GROUPS_YAML_KEY_NAME);
          for (Map hostGroup : hostGroups) {
            if (hostGroup.containsKey(HOSTS_YAML_KEY_NAME)) {
              List<String> hostList = (List<String>) hostGroup.get(HOSTS_YAML_KEY_NAME);
              for (String host : hostList) {
                hostToRackMap.put(host, rackName);
              }
            }
          }
        }
      }
      return hostToRackMap;
    }

    /**
     * Determine if service checks need to be ran after the stages.
     * @param upgradeContext the upgrade context
     * @return Return the stages, which may potentially be followed by service checks.
     */
    @Override
    public List<StageWrapper> build(UpgradeContext upgradeContext,
        List<StageWrapper> stageWrappers) {

      // insert all pre-processed stage wrappers first
      if (!stageWrappers.isEmpty()) {
        m_stages.addAll(0, stageWrappers);
      }

      List<TaskWrapper> tasks = new ArrayList<>();
      List<String> displays = new ArrayList<>();
      for (String service : m_servicesToCheck) {
        tasks.add(new TaskWrapper(
            service, "", Collections.emptySet(), new ServiceCheckTask()));

        displays.add(upgradeContext.getServiceDisplay(service));
      }

      if (upgradeContext.getDirection().isUpgrade() && m_serviceCheck
          && m_servicesToCheck.size() > 0) {

        StageWrapper wrapper = new StageWrapper(StageWrapper.Type.SERVICE_CHECK,
            "Service Check " + StringUtils.join(displays, ", "), tasks.toArray(new TaskWrapper[0]));

        m_stages.add(wrapper);
      }

      return m_stages;
    }
  }

  /**
   * Group all like-typed tasks together.  When they change, create a new type.
   */
  private static List<TaskBucket> buckets(List<Task> tasks) {
    if (null == tasks || tasks.isEmpty()) {
      return Collections.emptyList();
    }

    List<TaskBucket> holders = new ArrayList<>();

    TaskBucket current = null;

    int i = 0;
    for (Task t : tasks) {
      if (i == 0) {
        current = new TaskBucket(t);
        holders.add(current);
      } else if (i > 0 && t.getType() != tasks.get(i-1).getType()) {
        current = new TaskBucket(t);
        holders.add(current);
      } else {
        current.tasks.add(t);
      }

      i++;
    }

    return holders;
  }

  private static class TaskBucket {
    private StageWrapper.Type type;
    private List<Task> tasks = new ArrayList<>();
    private TaskBucket(Task initial) {
      switch (initial.getType()) {
        case CONFIGURE:
        case CREATE_AND_CONFIGURE:
        case SERVER_ACTION:
        case MANUAL:
        case ADD_COMPONENT:
          type = StageWrapper.Type.SERVER_SIDE_ACTION;
          break;
        case EXECUTE:
          type = StageWrapper.Type.UPGRADE_TASKS;
          break;
        case CONFIGURE_FUNCTION:
          type = StageWrapper.Type.CONFIGURE;
          break;
        case RESTART:
          type = StageWrapper.Type.RESTART;
          break;
        case START:
          type = StageWrapper.Type.START;
          break;
        case STOP:
          type = StageWrapper.Type.STOP;
          break;
        case SERVICE_CHECK:
          type = StageWrapper.Type.SERVICE_CHECK;
          break;
        case REGENERATE_KEYTABS:
          type = StageWrapper.Type.REGENERATE_KEYTABS;
          break;
      }
      tasks.add(initial);
    }
  }

  /**
   * Merge the services of all the child groups, with the current services.
   * Keeping the order specified by the group.
   */
  public void merge(Iterator<Grouping> iterator) throws AmbariException {
    Map<String, List<OrderService>> skippedServices = new HashMap<>();
    while (iterator.hasNext()) {
      Grouping group = iterator.next();

      boolean added = addGroupingServices(group.services, group.addAfterGroupEntry);
      if (added) {
        addSkippedServices(skippedServices, group.services);
      } else {
        // store these services until later
        if (skippedServices.containsKey(group.addAfterGroupEntry)) {
          List<OrderService> tmp = skippedServices.get(group.addAfterGroupEntry);
          tmp.addAll(group.services);
        } else {
          skippedServices.put(group.addAfterGroupEntry, group.services);
        }
      }
    }
  }

  /**
   * Merge the services to add after a particular service name
   */
  private boolean addGroupingServices(List<OrderService> servicesToAdd, String after) {
    if (after == null) {
      services.addAll(servicesToAdd);
      return true;
    }
    else {
      // Check the current services, if the "after" service is there then add these
      for (int index = services.size() - 1; index >= 0; index--) {
        OrderService service = services.get(index);
        if (service.serviceName.equals(after)) {
          services.addAll(index + 1, servicesToAdd);
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Adds services which were previously skipped, if the service they are supposed
   * to come after has been added.
   */
  private void addSkippedServices(Map<String, List<OrderService>> skippedServices, List<OrderService> servicesJustAdded) {
    for (OrderService service : servicesJustAdded) {
      if (skippedServices.containsKey(service.serviceName)) {
        List<OrderService> servicesToAdd = skippedServices.remove(service.serviceName);
        addGroupingServices(servicesToAdd, service.serviceName);
        addSkippedServices(skippedServices, servicesToAdd);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("name", name).toString();
  }
}
