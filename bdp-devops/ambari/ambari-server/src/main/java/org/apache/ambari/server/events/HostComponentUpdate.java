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

import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.State;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class HostComponentUpdate {

  private Long clusterId;
  private String serviceName;
  private String hostName;
  private String componentName;
  private State currentState;
  private State previousState;
  private MaintenanceState maintenanceState;
  private Boolean staleConfigs;

  private HostComponentUpdate(Long clusterId, String serviceName, String hostName, String componentName,
                             State currentState, State previousState, MaintenanceState maintenanceState,
                             Boolean staleConfigs) {
    this.clusterId = clusterId;
    this.serviceName = serviceName;
    this.hostName = hostName;
    this.componentName = componentName;
    this.currentState = currentState;
    this.previousState = previousState;
    this.maintenanceState = maintenanceState;
    this.staleConfigs = staleConfigs;
  }

  public static HostComponentUpdate createHostComponentStatusUpdate(HostComponentStateEntity stateEntity, State previousState) {
    HostComponentUpdate hostComponentUpdate = new HostComponentUpdate(stateEntity.getClusterId(),
        stateEntity.getServiceName(), stateEntity.getHostEntity().getHostName(), stateEntity.getComponentName(),
        stateEntity.getCurrentState(), previousState, null, null);
    return hostComponentUpdate;
  }

  public static HostComponentUpdate createHostComponentMaintenanceStatusUpdate(Long clusterId, String serviceName,
                                                                               String hostName, String componentName,
                                                                               MaintenanceState maintenanceState) {
    HostComponentUpdate hostComponentUpdate = new HostComponentUpdate(clusterId, serviceName, hostName,
        componentName, null, null, maintenanceState, null);
    return hostComponentUpdate;
  }

  public static HostComponentUpdate createHostComponentStaleConfigsStatusUpdate(Long clusterId, String serviceName,
                                                                               String hostName, String componentName,
                                                                               Boolean isStaleConfig) {
    HostComponentUpdate hostComponentUpdate = new HostComponentUpdate(clusterId, serviceName, hostName,
        componentName, null, null, null, isStaleConfig);
    return hostComponentUpdate;
  }

  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public State getCurrentState() {
    return currentState;
  }

  public void setCurrentState(State currentState) {
    this.currentState = currentState;
  }

  public State getPreviousState() {
    return previousState;
  }

  public void setPreviousState(State previousState) {
    this.previousState = previousState;
  }

  public MaintenanceState getMaintenanceState() {
    return maintenanceState;
  }

  public void setMaintenanceState(MaintenanceState maintenanceState) {
    this.maintenanceState = maintenanceState;
  }

  public Boolean getStaleConfigs() {
    return staleConfigs;
  }

  public void setStaleConfigs(Boolean staleConfigs) {
    this.staleConfigs = staleConfigs;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HostComponentUpdate that = (HostComponentUpdate) o;

    if (clusterId != null ? !clusterId.equals(that.clusterId) : that.clusterId != null) return false;
    if (serviceName != null ? !serviceName.equals(that.serviceName) : that.serviceName != null) return false;
    if (hostName != null ? !hostName.equals(that.hostName) : that.hostName != null) return false;
    if (componentName != null ? !componentName.equals(that.componentName) : that.componentName != null) return false;
    if (currentState != that.currentState) return false;
    if (previousState != that.previousState) return false;
    if (maintenanceState != that.maintenanceState) return false;
    return staleConfigs != null ? staleConfigs.equals(that.staleConfigs) : that.staleConfigs == null;
  }

  @Override
  public int hashCode() {
    int result = clusterId != null ? clusterId.hashCode() : 0;
    result = 31 * result + (serviceName != null ? serviceName.hashCode() : 0);
    result = 31 * result + (hostName != null ? hostName.hashCode() : 0);
    result = 31 * result + (componentName != null ? componentName.hashCode() : 0);
    result = 31 * result + (currentState != null ? currentState.hashCode() : 0);
    result = 31 * result + (previousState != null ? previousState.hashCode() : 0);
    result = 31 * result + (maintenanceState != null ? maintenanceState.hashCode() : 0);
    result = 31 * result + (staleConfigs != null ? staleConfigs.hashCode() : 0);
    return result;
  }
}
