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

package org.apache.ambari.server.serveraction;

import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;

/**
 * ServerAction is an interface to be implemented by all server-based actions/tasks.
 */
public interface ServerAction {

  String ACTION_NAME = "ACTION_NAME";
  String ACTION_USER_NAME = "ACTION_USER_NAME";

  /**
   * The name of the class that the server action is going to delegate to. This
   * is used in cases were a server action is being told to run another
   * non-server action, such as during an upgrade.
   */
  String WRAPPED_CLASS_NAME = "WRAPPED_CLASS_NAME";

  /**
   * The default timeout (in seconds) to use for potentially long running tasks such as creating
   * Kerberos principals and generating Kerberos keytab files
   */
  int DEFAULT_LONG_RUNNING_TASK_TIMEOUT_SECONDS = 36000;


  /**
   * Gets the ExecutionCommand property of this ServerAction.
   *
   * @return the ExecutionCommand property of this ServerAction
   */
  ExecutionCommand getExecutionCommand();

  /**
   * Sets the ExecutionCommand property of this ServerAction.
   * <p/>
   * This property is expected to be set by the creator of this ServerAction before calling execute.
   *
   * @param command the ExecutionCommand data to set
   */
  void setExecutionCommand(ExecutionCommand command);


  /**
   * Gets the HostRoleCommand property of this ServerAction.
   *
   * @return the HostRoleCommand property of this ServerAction
   */
  HostRoleCommand getHostRoleCommand();

  /**
   * Sets the HostRoleCommand property of this ServerAction.
   * <p/>
   * This property is expected to be set by the creator of this ServerAction before calling execute.
   *
   * @param hostRoleCommand the HostRoleCommand data to set
   */
  void setHostRoleCommand(HostRoleCommand hostRoleCommand);

  /**
   * Executes this ServerAction
   * <p/>
   * This is typically called by the ServerActionExecutor in it's own thread, but there is no
   * guarantee that this is the case.  It is expected that the ExecutionCommand and HostRoleCommand
   * properties are set before calling this method.
   *
   * @param requestSharedDataContext a Map to be used a shared data among all ServerActions related
   *                                 to a given request
   * @return a CommandReport declaring the status of the task
   * @throws AmbariException
   * @throws InterruptedException
   */
  CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
      throws AmbariException, InterruptedException;
}
