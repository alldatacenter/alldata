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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.stack.upgrade.Task;
import org.apache.commons.lang.StringUtils;

import com.google.common.base.MoreObjects;

/**
 * Aggregates all upgrade tasks for a HostComponent into one wrapper.
 */
public class TaskWrapper {

  private String service;
  private String component;
  private Set<String> hosts; // all the hosts that all the tasks must run
  private Map<String, String> params;
  /* FIXME a TaskWrapper really should be wrapping ONLY ONE task */
  private List<Task> tasks; // all the tasks defined for the hostcomponent
  private Set<String> timeoutKeys = new HashSet<>();

  /**
   * @param s the service name for the task
   * @param c the component name for the task
   * @param hosts the set of hosts that the task is for
   * @param task a single task
   */
  public TaskWrapper(String s, String c, Set<String> hosts, Task task) {
    this(s, c, hosts, null, task);
  }


  /**
   * @param s the service name for the tasks
   * @param c the component name for the tasks
   * @param hosts the set of hosts that the tasks are for
   * @param params additional command parameters
   * @param tasks an array of tasks as a convenience
   */
  public TaskWrapper(String s, String c, Set<String> hosts, Map<String, String> params, Task... tasks) {
    this(s, c, hosts, params, Arrays.asList(tasks));
  }


  /**
   * @param s the service name for the tasks
   * @param c the component name for the tasks
   * @param hosts the set of hosts for the
   * @param tasks the list of tasks
   */
  public TaskWrapper(String s, String c, Set<String> hosts, Map<String, String> params, List<Task> tasks) {
    service = s;
    component = c;

    this.hosts = hosts;
    this.params = (params == null) ? new HashMap<>() : params;
    this.tasks = tasks;

    // !!! FIXME there should only be one task
    for (Task task : tasks) {
      if (StringUtils.isNotBlank(task.timeoutConfig)) {
        timeoutKeys.add(task.timeoutConfig);
      }
    }
  }

  /**
   * @return the additional command parameters.
   */
  public Map<String, String> getParams() {
    return params;
  }

  /**
   * @return the tasks associated with this wrapper
   */
  public List<Task> getTasks() {
    return tasks;
  }

  /**
   * @return the hosts associated with this wrapper
   */
  public Set<String> getHosts() {
    return hosts;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("service", service)
        .add("component", component)
        .add("tasks", tasks)
        .add("hosts", hosts)
        .omitNullValues().toString();
  }

  /**
   * @return the service name
   */
  public String getService() {
    return service;
  }

  /**
   * @return the component name
   */
  public String getComponent() {
    return component;
  }

  /**
   * @return true if any task is sequential, otherwise, return false.
   */
  public boolean isAnyTaskSequential() {
    for (Task t : getTasks()) {
      if (t.isSequential) {
        return true;
      }
    }

    return false;
  }


  /**
   * @return the timeout keys for all the tasks in this wrapper.
   */
  public Set<String> getTimeoutKeys() {
    return timeoutKeys;
  }

}
