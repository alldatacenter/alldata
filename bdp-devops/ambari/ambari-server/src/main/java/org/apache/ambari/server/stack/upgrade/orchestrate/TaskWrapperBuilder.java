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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.stack.HostsType;
import org.apache.ambari.server.stack.upgrade.ExecuteHostType;
import org.apache.ambari.server.stack.upgrade.ExecuteTask;
import org.apache.ambari.server.stack.upgrade.Task;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Generates a collection of tasks that need to run on a set of hosts during an upgarde.
 */
public class TaskWrapperBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(TaskWrapperBuilder.class);

  /**
   * Creates a collection of task wrappers based on the set of hosts they are allowed to run on
   * by analyzing the "hosts" attribute of any ExecuteTask objects.
   *
   * @param service the service name for the tasks
   * @param component the component name for the tasks
   * @param hostsType the collection of sets along with their status
   * @param tasks collection of tasks
   * @param params additional parameters
   *
   * @return the task wrappers, one for each task that is passed with {@code tasks}
   */
  public static List<TaskWrapper> getTaskList(String service, String component, HostsType hostsType, List<Task> tasks, Map<String, String> params) {
    // Ok if Ambari Server is not part of the cluster hosts since this is only used in the calculation of how many batches
    // to create.
    String ambariServerHostname = StageUtils.getHostName();

    List<TaskWrapper> collection = new ArrayList<>();
    for (Task t : tasks) {

      // only add the server-side task if there are actual hosts for the service/component
      if (t.getType().isServerAction() && CollectionUtils.isNotEmpty(hostsType.getHosts())) {
        collection.add(new TaskWrapper(service, component, Collections.singleton(ambariServerHostname), params, t));
        continue;
      }

      if (t.getType().equals(Task.Type.EXECUTE)) {
        ExecuteTask et = (ExecuteTask) t;
        if (et.hosts == ExecuteHostType.MASTER) {
          if (hostsType.hasMasters()) {
            collection.add(new TaskWrapper(service, component, hostsType.getMasters(), params, t));
            continue;
          } else {
            LOG.error(MessageFormat.format("Found an Execute task for {0} and {1} meant to run on a master but could not find any masters to run on. Skipping this task.", service, component));
            continue;
          }
        }
        // Pick a random host.
        if (et.hosts == ExecuteHostType.ANY) {
          if (!hostsType.getHosts().isEmpty()) {
            collection.add(new TaskWrapper(service, component, Collections.singleton(hostsType.getHosts().iterator().next()), params, t));
            continue;
          } else {
            LOG.error(MessageFormat.format("Found an Execute task for {0} and {1} meant to run on any host but could not find host to run on. Skipping this task.", service, component));
            continue;
          }
        }

        // Pick the first host sorted alphabetically (case insensitive).
        if (et.hosts == ExecuteHostType.FIRST) {
          if (!hostsType.getHosts().isEmpty()) {
            List<String> sortedHosts = new ArrayList<>(hostsType.getHosts());
            Collections.sort(sortedHosts, String.CASE_INSENSITIVE_ORDER);
            collection.add(new TaskWrapper(service, component, Collections.singleton(sortedHosts.get(0)), params, t));
            continue;
          } else {
            LOG.error(MessageFormat.format("Found an Execute task for {0} and {1} meant to run on the first host sorted alphabetically but could not find host to run on. Skipping this task.", service, component));
            continue;
          }
        }
        // Otherwise, meant to run on ALL hosts.
      }

      collection.add(new TaskWrapper(service, component, hostsType.getHosts(), params, t));
    }

    return collection;
  }

  /**
   * Given a collection of tasks, get the union of the hosts.
   * @param tasks Collection of tasks
   * @return Returns the union of the hosts scheduled to perform the tasks.
   */
  public static Set<String> getEffectiveHosts(List<TaskWrapper> tasks) {
    Set<String> effectiveHosts = new HashSet<>();
    for(TaskWrapper t : tasks) {
      effectiveHosts.addAll(t.getHosts());
    }
    return effectiveHosts;
  }
}
