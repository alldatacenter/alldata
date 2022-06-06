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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.ambari.server.stack.upgrade.orchestrate.StageWrapper;

/**
 * Used to represent an execution that should occur on an agent.
 * An equivalent class exists in the python server-side, called ExecuteTask in ru_execute_tasks.py
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="execute")
public class ExecuteTask extends Task {

  @XmlTransient
  private Task.Type type = Task.Type.EXECUTE;

  /**
   * The hosts to run the task on. Default to running on
   * {@link ExecuteHostType#ALL}.
   */
  @XmlAttribute
  public ExecuteHostType hosts = ExecuteHostType.ALL;

  /**
   * Similar to a command, but instead it is a call to invoke the script (using its relative path).
   * THe script and function elements are used together, and are invoked with additional environment variables.
   * If both a (script, function) and command are defined, only the (script, function) will be executed.
   * The service is already specified as part of the group.
   */
  @XmlElement(name="script")
  public String script;

  /**
   * Function name to call in the {@see script} element.
   */
  @XmlElement(name="function")
  public String function;

  public static final String actionVerb = "Executing";

  @Override
  public Task.Type getType() {
    return type;
  }

  @Override
  public StageWrapper.Type getStageWrapperType() {
    return StageWrapper.Type.UPGRADE_TASKS;
  }

  @Override
  public String getActionVerb() {
    return actionVerb;
  }
}
