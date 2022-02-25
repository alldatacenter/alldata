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
package org.apache.ambari.server.events;

import org.apache.ambari.server.agent.CommandReport;

/**
 * The {@link ActionFinalReportReceivedEvent} is fired when a
 * command report action is received. Event is fired only if command state
 * is COMPLETED/FAILED/ABORTED.
 */
public final class ActionFinalReportReceivedEvent extends AmbariEvent {

  private Long clusterId;
  private String hostname;
  private CommandReport commandReport;
  private String role;
  private Boolean emulated;

  /**
   * Constructor.
   *
   * @param clusterId (beware, may be null if action is not bound to cluster)
   * @param hostname host that is an origin for a command report
   * @param report full command report (may be null if action has been cancelled)
   * @param emulated true, if event was generated without actually receiving
   * data from agent (e.g. if we did not perform action, or action timed out,
   * but we want to trigger event listener anyway). More loose checks against
   * data will be performed in this case.
   */
  public ActionFinalReportReceivedEvent(Long clusterId, String hostname,
                                        CommandReport report,
                                        Boolean emulated) {
    super(AmbariEventType.ACTION_EXECUTION_FINISHED);
    this.clusterId = clusterId;
    this.hostname = hostname;
    this.commandReport = report;
    if (report.getRole() != null) {
      this.role = report.getRole();
    } else {
      this.role = null;
    }
    this.emulated = emulated;
  }

  public Long getClusterId() {
    return clusterId;
  }

  public String getHostname() {
    return hostname;
  }

  public CommandReport getCommandReport() {
    return commandReport;
  }

  public String getRole() {
    return role;
  }

  public Boolean isEmulated() {
    return emulated;
  }

  @Override
  public String toString() {
    return "ActionFinalReportReceivedEvent{" +
            "clusterId=" + clusterId +
            ", hostname='" + hostname + '\'' +
            ", commandReportStatus=" + commandReport.getStatus() +
            ", commandReportRole=" + role +
            '}';
  }
}
