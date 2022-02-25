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
package org.apache.ambari.server.agent;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.annotations.SerializedName;

/**
 * Command to report the status of a list of services in roles.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CancelCommand extends AgentCommand {

  public CancelCommand() {
    super(AgentCommandType.CANCEL_COMMAND);
  }

  @SerializedName("target_task_id")
  @com.fasterxml.jackson.annotation.JsonProperty("target_task_id")
  private long targetTaskId;

  @SerializedName("reason")
  @com.fasterxml.jackson.annotation.JsonProperty("reason")
  private String reason;

  public long getTargetTaskId() {
    return targetTaskId;
  }

  public void setTargetTaskId(long targetTaskId) {
    this.targetTaskId = targetTaskId;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CancelCommand that = (CancelCommand) o;

    if (targetTaskId != that.targetTaskId) return false;
    return reason != null ? reason.equals(that.reason) : that.reason == null;
  }

  @Override
  public int hashCode() {
    int result = (int) (targetTaskId ^ (targetTaskId >>> 32));
    result = 31 * result + (reason != null ? reason.hashCode() : 0);
    return result;
  }
}
