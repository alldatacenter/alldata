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

package org.apache.ambari.server.events;

import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.State;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Contains info about service update. This update will be sent to all subscribed recipients.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceUpdateEvent extends STOMPEvent {

  @JsonProperty("cluster_name")
  private String clusterName;

  @JsonProperty("maintenance_state")
  private MaintenanceState maintenanceState;

  @JsonProperty("service_name")
  private String serviceName;

  @JsonProperty("state")
  private State state;

  @JsonIgnore
  private boolean stateChanged = false;

  public ServiceUpdateEvent(String clusterName, MaintenanceState maintenanceState, String serviceName, State state,
                            boolean stateChanged) {
    super(Type.SERVICE);
    this.clusterName = clusterName;
    this.maintenanceState = maintenanceState;
    this.serviceName = serviceName;
    this.state = state;
    this.stateChanged = stateChanged;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public MaintenanceState getMaintenanceState() {
    return maintenanceState;
  }

  public void setMaintenanceState(MaintenanceState maintenanceState) {
    this.maintenanceState = maintenanceState;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public State getState() {
    return state;
  }

  public void setState(State state) {
    this.state = state;
  }

  public boolean isStateChanged() {
    return stateChanged;
  }

  public void setStateChanged(boolean stateChanged) {
    this.stateChanged = stateChanged;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ServiceUpdateEvent that = (ServiceUpdateEvent) o;

    if (clusterName != null ? !clusterName.equals(that.clusterName) : that.clusterName != null) return false;
    return serviceName != null ? serviceName.equals(that.serviceName) : that.serviceName == null;
  }

  @Override
  public int hashCode() {
    int result = clusterName != null ? clusterName.hashCode() : 0;
    result = 31 * result + (serviceName != null ? serviceName.hashCode() : 0);
    return result;
  }
}
