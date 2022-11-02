/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.events;

import java.util.Objects;

import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Single host role command update info. This update will be sent to all subscribed recipients.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NamedTaskUpdateEvent extends STOMPEvent {

  private Long id;
  private Long requestId;
  private String hostName;
  private Long endTime;
  private HostRoleStatus status;
  private String errorLog;
  private String outLog;
  private String stderr;
  private String stdout;

  @JsonProperty("structured_out")
  private String structuredOut;

  public NamedTaskUpdateEvent(Long id, Long requestId, String hostName, Long endTime, HostRoleStatus status,
                              String errorLog, String outLog, String stderr, String stdout, String structuredOut) {
    super(Type.NAMEDTASK);
    this.id = id;
    this.requestId = requestId;
    this.hostName = hostName;
    this.endTime = endTime;
    this.status = status;
    this.errorLog = errorLog;
    this.outLog = outLog;
    this.stderr = stderr;
    this.stdout = stdout;
    this.structuredOut = structuredOut;
  }

  public NamedTaskUpdateEvent(HostRoleCommand hostRoleCommand) {
    this(hostRoleCommand.getTaskId(), hostRoleCommand.getRequestId(), hostRoleCommand.getHostName(),
        hostRoleCommand.getEndTime(), hostRoleCommand.getStatus(), hostRoleCommand.getErrorLog(),
        hostRoleCommand.getOutputLog(), hostRoleCommand.getStderr(), hostRoleCommand.getStdout(),
        hostRoleCommand.getStructuredOut());
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getRequestId() {
    return requestId;
  }

  public void setRequestId(Long requestId) {
    this.requestId = requestId;
  }

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public Long getEndTime() {
    return endTime;
  }

  public void setEndTime(Long endTime) {
    this.endTime = endTime;
  }

  public HostRoleStatus getStatus() {
    return status;
  }

  public void setStatus(HostRoleStatus status) {
    this.status = status;
  }

  public String getErrorLog() {
    return errorLog;
  }

  public void setErrorLog(String errorLog) {
    this.errorLog = errorLog;
  }

  public String getOutLog() {
    return outLog;
  }

  public void setOutLog(String outLog) {
    this.outLog = outLog;
  }

  public String getStderr() {
    return stderr;
  }

  public void setStderr(String stderr) {
    this.stderr = stderr;
  }

  public String getStdout() {
    return stdout;
  }

  public void setStdout(String stdout) {
    this.stdout = stdout;
  }

  public String getStructuredOut() {
    return structuredOut;
  }

  public void setStructuredOut(String structuredOut) {
    this.structuredOut = structuredOut;
  }

  @Override
  public String completeDestination(String destination) {
    return destination + "/" + id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    NamedTaskUpdateEvent that = (NamedTaskUpdateEvent) o;
    return Objects.equals(id, that.id) &&
        Objects.equals(requestId, that.requestId) &&
        Objects.equals(hostName, that.hostName) &&
        Objects.equals(endTime, that.endTime) &&
        status == that.status &&
        Objects.equals(errorLog, that.errorLog) &&
        Objects.equals(outLog, that.outLog) &&
        Objects.equals(stderr, that.stderr) &&
        Objects.equals(stdout, that.stdout) &&
        Objects.equals(structuredOut, that.structuredOut);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, requestId, hostName, endTime, status, errorLog, outLog, stderr, stdout, structuredOut);
  }
}
