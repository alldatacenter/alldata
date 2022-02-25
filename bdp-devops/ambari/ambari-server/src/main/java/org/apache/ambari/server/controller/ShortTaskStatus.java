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

package org.apache.ambari.server.controller;

import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.lang.StringUtils;

public class ShortTaskStatus {
  protected long requestId;
  protected long taskId;
  protected long stageId;
  protected String hostName;
  protected String role;
  protected String command;
  protected String status;
  protected String customCommandName;
  protected String outputLog;
  protected String errorLog;

  public ShortTaskStatus() {
  }

  public ShortTaskStatus(int taskId, long stageId, String hostName, String role, String command, String status,
                         String customCommandName, String outputLog, String errorLog) {
    this.taskId = taskId;
    this.stageId = stageId;
    this.hostName = translateHostName(hostName);
    this.role = role;
    this.command = command;
    this.status = status;
    this.customCommandName = customCommandName;
    this.outputLog = outputLog;
    this.errorLog = errorLog;
  }

  public ShortTaskStatus(HostRoleCommand hostRoleCommand) {
    this.taskId = hostRoleCommand.getTaskId();
    this.stageId = hostRoleCommand.getStageId();
    this.command = hostRoleCommand.getRoleCommand().toString();
    this.hostName = translateHostName(hostRoleCommand.getHostName());
    this.role = hostRoleCommand.getRole().toString();
    this.status = hostRoleCommand.getStatus().toString();
    this.customCommandName = hostRoleCommand.getCustomCommandName();
    this.outputLog = hostRoleCommand.getOutputLog();
    this.errorLog = hostRoleCommand.getErrorLog();
  }

  public void setRequestId(long requestId) {
    this.requestId = requestId;
  }

  public long getRequestId() {
    return requestId;
  }

  public String getCustomCommandName() {
    return customCommandName;
  }

  public void setCustomCommandName(String customCommandName) {
    this.customCommandName = customCommandName;
  }

  public long getTaskId() {
    return taskId;
  }

  public void setTaskId(long taskId) {
    this.taskId = taskId;
  }

  public long getStageId() {
    return stageId;
  }

  public void setStageId(long stageId) {
    this.stageId = stageId;
  }

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = translateHostName(hostName);
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getCommand() {
    return command;
  }

  public void setCommand(String command) {
    this.command = command;
  }

  public String getOutputLog() {
    return outputLog;
  }

  public void setOutputLog(String outputLog) {
    this.outputLog = outputLog;
  }

  public String getErrorLog() {
    return errorLog;
  }

  public void setErrorLog(String errorLog) {
    this.errorLog = errorLog;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ShortTaskStatusDump ")
      .append(", stageId=").append(stageId)
      .append(", taskId=").append(taskId)
      .append(", hostname=").append(hostName)
      .append(", role=").append(role)
      .append(", command=").append(command)
      .append(", status=").append(status)
      .append(", outputLog=").append(outputLog)
      .append(", errorLog=").append(errorLog);
    return sb.toString();
  }

  /**
   * If the hostname is null (or empty), returns the hostname of the Ambari Server; else returns the
   * supplied hostname value.
   *
   * @param hostName a hostname
   * @return the hostname of the Ambari Server if the hostname is null (or empty); else supplied hostname value
   */
  private String translateHostName(String hostName) {
    // if the hostname in the command is null, replace it with the hostname of the Ambari Server
    // This is because commands (to be) handled by the Ambari Server have a null value for its
    // host designation.
    return (StringUtils.isEmpty(hostName))
        ? StageUtils.getHostName()
        : hostName;
  }
}
