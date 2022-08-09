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


import java.util.Map;

import org.apache.ambari.server.agent.RecoveryConfig;
import org.apache.ambari.server.state.BlueprintProvisioningState;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class HostLevelParamsCluster {

  @JsonProperty("recoveryConfig")
  private RecoveryConfig recoveryConfig;

  @JsonProperty("blueprint_provisioning_state")
  private Map<String, BlueprintProvisioningState> blueprintProvisioningState;

  public HostLevelParamsCluster(RecoveryConfig recoveryConfig, Map<String, BlueprintProvisioningState> blueprintProvisioningState) {
    this.recoveryConfig = recoveryConfig;
    this.blueprintProvisioningState = blueprintProvisioningState;
  }

  public RecoveryConfig getRecoveryConfig() {
    return recoveryConfig;
  }

  public Map<String, BlueprintProvisioningState> getBlueprintProvisioningState() {
    return blueprintProvisioningState;
  }
}
