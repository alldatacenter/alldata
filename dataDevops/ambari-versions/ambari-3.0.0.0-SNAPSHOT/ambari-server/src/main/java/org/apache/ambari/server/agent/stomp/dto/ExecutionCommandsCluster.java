/**
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
package org.apache.ambari.server.agent.stomp.dto;

import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.agent.CancelCommand;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ExecutionCommandsCluster {

  @JsonProperty("commands")
  private List<org.apache.ambari.server.agent.ExecutionCommand> executionCommands = new ArrayList<>();

  @JsonProperty("cancelCommands")
  private List<CancelCommand> cancelCommands = new ArrayList<>();

  public ExecutionCommandsCluster(List<org.apache.ambari.server.agent.ExecutionCommand> executionCommands, List<CancelCommand> cancelCommands) {
    this.executionCommands = executionCommands;
    this.cancelCommands = cancelCommands;
  }

  public List<org.apache.ambari.server.agent.ExecutionCommand> getExecutionCommands() {
    return executionCommands;
  }

  public void setExecutionCommands(List<org.apache.ambari.server.agent.ExecutionCommand> executionCommands) {
    this.executionCommands = executionCommands;
  }

  public List<CancelCommand> getCancelCommands() {
    return cancelCommands;
  }

  public void setCancelCommands(List<CancelCommand> cancelCommands) {
    this.cancelCommands = cancelCommands;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ExecutionCommandsCluster that = (ExecutionCommandsCluster) o;

    if (executionCommands != null ? !executionCommands.equals(that.executionCommands) : that.executionCommands != null)
      return false;
    return cancelCommands != null ? cancelCommands.equals(that.cancelCommands) : that.cancelCommands == null;
  }

  @Override
  public int hashCode() {
    int result = executionCommands != null ? executionCommands.hashCode() : 0;
    result = 31 * result + (cancelCommands != null ? cancelCommands.hashCode() : 0);
    return result;
  }
}
