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

import java.util.Collections;
import java.util.Map;

import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.audit.event.AuditEvent;
import org.apache.ambari.server.utils.StageUtils;

import com.google.gson.Gson;
import com.google.inject.Inject;

/**
 * AbstractServerAction is an abstract implementation of a ServerAction.
 * <p/>
 * This abstract implementation provides common facilities for all ServerActions, such as
 * maintaining the ExecutionCommand and HostRoleCommand properties. It also provides a convenient
 * way to generate CommandReports for reporting status.
 */
public abstract class AbstractServerAction implements ServerAction {
  /**
   * The ExecutionCommand containing data related to this ServerAction implementation
   */
  private ExecutionCommand executionCommand = null;

  /**
   * The HostRoleCommand containing data related to this ServerAction implementation
   */
  private HostRoleCommand hostRoleCommand = null;

  /**
   * The ActionLog that used to log execution progress of ServerAction
   */
  protected ActionLog actionLog = new ActionLog();

  @Inject
  private AuditLogger auditLogger;

  /**
   * Used to deserialized JSON.
   */
  @Inject
  protected Gson gson;

  @Override
  public ExecutionCommand getExecutionCommand() {
    return executionCommand;
  }

  @Override
  public void setExecutionCommand(ExecutionCommand executionCommand) {
    this.executionCommand = executionCommand;
  }

  @Override
  public HostRoleCommand getHostRoleCommand() {
    return hostRoleCommand;
  }

  @Override
  public void setHostRoleCommand(HostRoleCommand hostRoleCommand) {
    this.hostRoleCommand = hostRoleCommand;
  }

  /**
   * @return a command report with 0 exit code and COMPLETED HostRoleStatus
   */
  protected CommandReport createCompletedCommandReport() {
    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
  }

  /**
   * Creates a CommandReport used to report back to Ambari the status of this ServerAction.
   *
   * @param exitCode      an integer value declaring the exit code for this action - 0 typically
   *                      indicates success.
   * @param status        a HostRoleStatus indicating the status of this action
   * @param structuredOut a String containing the (typically) JSON-formatted data representing the
   *                      output from this action (this data is stored in the database, along with
   *                      the command status)
   * @param stdout        A string containing the data from the standard out stream (this data is stored in
   *                      the database, along with the command status)
   * @param stderr        A string containing the data from the standard error stream (this data is stored
   *                      in the database, along with the command status)
   * @return the generated CommandReport, or null if the HostRoleCommand or ExecutionCommand
   * properties are missing
   */
  protected CommandReport createCommandReport(int exitCode, HostRoleStatus status, String structuredOut,
                                              String stdout, String stderr) {
    CommandReport report = null;

    if (hostRoleCommand != null) {
      if (executionCommand == null) {
        ExecutionCommandWrapper wrapper = hostRoleCommand.getExecutionCommandWrapper();

        if (wrapper != null) {
          executionCommand = wrapper.getExecutionCommand();
        }
      }

      if (executionCommand != null) {
        RoleCommand roleCommand = executionCommand.getRoleCommand();

        report = new CommandReport();

        report.setActionId(StageUtils.getActionId(hostRoleCommand.getRequestId(), hostRoleCommand.getStageId()));
        report.setClusterId(executionCommand.getClusterId());
        //report.setConfigurationTags(executionCommand.getConfigurationTags());
        report.setRole(executionCommand.getRole());
        report.setRoleCommand((roleCommand == null) ? null : roleCommand.toString());
        report.setServiceName(executionCommand.getServiceName());
        report.setTaskId(executionCommand.getTaskId());

        report.setStructuredOut(structuredOut);
        report.setStdErr((stderr == null) ? "" : stderr);
        report.setStdOut((stdout == null) ? "" : stdout);
        report.setStatus((status == null) ? null : status.toString());
        report.setExitCode(exitCode);
      }
    }

    return report;
  }

  /**
   * Returns the command parameters value from the ExecutionCommand
   * <p/>
   * The returned map should be assumed to be read-only.
   *
   * @return the (assumed read-only) command parameters value from the ExecutionCommand
   */
  protected Map<String, String> getCommandParameters() {
    if (executionCommand == null) {
      return Collections.emptyMap();
    } else {
      return executionCommand.getCommandParams();
    }
  }

  /**
   * Attempts to safely retrieve a property with the specified name from the this action's relevant
   * command parameters Map.
   *
   * @param propertyName a String declaring the name of the item from commandParameters to retrieve
   * @return the value of the requested property, or null if not found or set
   */
  protected String getCommandParameterValue(String propertyName) {
    Map<String, String> commandParameters = getCommandParameters();
    return (commandParameters == null) ? null : commandParameters.get(propertyName);
  }

  protected void auditLog(AuditEvent ae) {
    auditLogger.log(ae);
  }
}
