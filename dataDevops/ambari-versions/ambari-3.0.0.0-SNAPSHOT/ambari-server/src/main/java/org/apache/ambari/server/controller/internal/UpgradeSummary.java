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
package org.apache.ambari.server.controller.internal;

import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;

/**
 * Represents a detailed summary of an upgrade, including the most recent failed task in order to bubble up errors.
 */
public class UpgradeSummary {
  private String displayText;
  private Long requestId;
  private Long stageId;
  private Long taskId;
  private String hostName;
  private HostRoleCommandEntity failedTask;

  public UpgradeSummary(String displayText, Long requestId, Long stageId, Long taskId, String hostName, HostRoleCommandEntity failedTask) {
    this.displayText = displayText;
    this.requestId = requestId;
    this.stageId = stageId;
    this.taskId = taskId;
    this.hostName = hostName;
    this.failedTask = failedTask;
  }

  public Long getStageId() {
    return stageId;
  }

  public Long getTaskId() {
    return taskId;
  }

  public UpgradeSummary(HostRoleCommandEntity hrc) {
    this("", hrc.getRequestId(), hrc.getStageId(), hrc.getTaskId(), hrc.getHostName(), hrc);

    // Construct a message to display on the UI.
    displayText = "Failed";
    if (hrc.getCommandDetail() != null) {
      displayText += " calling " + hrc.getCommandDetail();
    }
    if (hrc.getHostName() != null) {
      displayText += " on host " + hrc.getHostName();
    }
  }

  /**
   * Get the error message to display.
   */
  public String getDisplayText() {
    return this.displayText;
  }

  /**
   * Get the failed task if it exists.
   * @return The failed task if it exists, otherwise null.
   */
  public HostRoleCommandEntity getFailedTask() {
    return failedTask;
  }
}
