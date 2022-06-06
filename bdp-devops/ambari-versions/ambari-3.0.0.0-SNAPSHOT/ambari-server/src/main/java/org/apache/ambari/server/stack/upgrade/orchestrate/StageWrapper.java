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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.stack.upgrade.Task;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.gson.Gson;

/**
 *
 */
public class StageWrapper {

  private static final Logger LOG = LoggerFactory.getLogger(StageWrapper.class);

  private static Gson gson = new Gson();
  private String text;
  private Type type;
  private Map<String, String> params;
  private List<TaskWrapper> tasks;

  /**
   * Wrapper for a stage that encapsulates its text and tasks.
   * @param type Type of stage
   * @param text Text to display
   * @param tasks List of tasks to add to the stage
   */
  public StageWrapper(Type type, String text, TaskWrapper... tasks) {
    this(type, text, null, Arrays.asList(tasks));
  }

  /**
   * Wrapper for a stage that encapsulates its text, params, and tasks.
   * @param type Type of stage
   * @param text Text to display
   * @param params Command parameters
   * @param tasks List of tasks to add to the stage
   */
  public StageWrapper(Type type, String text, Map<String, String> params, TaskWrapper... tasks) {
    this(type, text, params, Arrays.asList(tasks));
  }

  /**
   * Wrapper for a stage that encapsulates its text, params, and tasks.
   * @param type Type of stage
   * @param text Text to display
   * @param params Command parameters
   * @param tasks List of tasks to add to the stage
   */
  public StageWrapper(Type type, String text, Map<String, String> params, List<TaskWrapper> tasks) {
    this.type = type;
    this.text = text;
    this.params = (params == null ? Collections.emptyMap() : params);
    this.tasks = tasks;
  }

  /**
   * Gets the hosts json.
   */
  public String getHostsJson() {
    return gson.toJson(getHosts());
  }

  /**
   * Gets the tasks json.
   */
  public String getTasksJson() {
    List<Task> realTasks = new ArrayList<>();
    for (TaskWrapper tw : tasks) {
      realTasks.addAll(tw.getTasks());
    }

    return gson.toJson(realTasks);
  }

  /**
   * @return the set of hosts for all tasks
   */
  public Set<String> getHosts() {
    Set<String> hosts = new HashSet<>();
    for (TaskWrapper tw : tasks) {
      hosts.addAll(tw.getHosts());
    }

    return hosts;
  }

  /**
   * @return the additional command parameters
   */
  public Map<String, String> getParams() {
    return params;
  }

  /**
   * @return the wrapped tasks for this stage
   */
  public List<TaskWrapper> getTasks() {
    return tasks;
  }

  /**
   * @return the text for this stage
   */
  public String getText() {
    return text;
  }

  /**
   * @param newText the new text for the stage
   */
  public void setText(String newText) {
    text = newText;
  }

  /**
   * Gets the type of stage.  All tasks defined for the stage execute this type.
   * @return the type
   */
  public Type getType() {
    return type;
  }

  /**
   * Indicates the type of wrapper.
   */
  public enum Type {
    SERVER_SIDE_ACTION,
    RESTART,
    UPGRADE_TASKS,
    SERVICE_CHECK,
    STOP,
    START,
    CONFIGURE, 
    REGENERATE_KEYTABS;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("type", type)
        .add("text",text)
        .omitNullValues().toString();
  }

  /**
   * Gets the maximum timeout for any task that this {@code StageWrapper} encapsulates.  TaskWrappers
   * are homogeneous across the stage, but timeouts are defined in Upgrade Packs
   * at the task, so each one should be checked individually.
   *
   * <p>
   * WARNING:  This method relies on incorrect assumptions about {@link StageWrapper}s and the {@link TaskWrapper}s
   * that are contained in them.  Orchestration is currently forcing a StageWrapper to have only one TaskWrapper,
   * even though they could have many per the code.
   *
   * In addition, a TaskWrapper should have a one-to-one reference with the Task it contains.  That will be
   * fixed in a future release.
   * </p>
   *
   * @param configuration the configuration instance.  StageWrappers are not injectable, so pass
   *                      this in.
   * @return the maximum timeout, or the default agent execution timeout if none are found.  Never {@code null}.
   */
  public Short getMaxTimeout(Configuration configuration) {

    Set<String> timeoutKeys = new HashSet<>();

    // !!! FIXME a TaskWrapper should have only one task.
    for (TaskWrapper wrapper : tasks) {
      timeoutKeys.addAll(wrapper.getTimeoutKeys());
    }

    Short defaultTimeout = Short.valueOf(configuration.getDefaultAgentTaskTimeout(false));

    if (CollectionUtils.isEmpty(timeoutKeys)) {
      return defaultTimeout;
    }

    Short timeout = null;

    for (String key : timeoutKeys) {
      String configValue = configuration.getProperty(key);

      if (StringUtils.isNotBlank(configValue)) {
        try {
          Short configTimeout = Short.valueOf(configValue);

          if (null == timeout || configTimeout > timeout) {
            timeout = configTimeout;
          }

        } catch (Exception e) {
          LOG.warn("Could not parse {}/{} to a timeout value", key, configValue);
        }
      } else {
        LOG.warn("Configuration {} not found to compute timeout", key);
      }
    }

    return null == timeout ? defaultTimeout : timeout;
  }
}
