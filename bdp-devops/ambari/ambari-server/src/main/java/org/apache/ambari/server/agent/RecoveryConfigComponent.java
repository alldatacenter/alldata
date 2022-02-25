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

import java.util.Objects;

import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.State;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

/**
 * Holder for component
 */
public class RecoveryConfigComponent{

  @SerializedName("component_name")
  @JsonProperty("component_name")
  private String componentName;

  @SerializedName("service_name")
  @JsonProperty("service_name")
  private String serviceName;

  @SerializedName("desired_state")
  @JsonProperty("desired_state")
  private String desiredState;

  /**
   * Creates new instance of {@link RecoveryConfigComponent}
   * @param componentName name of the component
   * @param desiredState desired desiredState of the component
   */
  public RecoveryConfigComponent(String componentName, String serviceName, State desiredState){
    this.setComponentName(componentName);
    this.setServiceName(serviceName);
    this.setDesiredState(desiredState);
  }

  /**
   * Creates {@link RecoveryConfigComponent} instance from initialized {@link ServiceComponentHost}
   */
  public RecoveryConfigComponent(ServiceComponentHost sch) {
    this(sch.getServiceComponentName(), sch.getServiceName(), sch.getDesiredState());
  }

  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public State getDesiredState() {
    return State.valueOf(desiredState);
  }


  public void setDesiredState(State state) {
    this.desiredState = state.toString();
  }

  @Override
  public String toString(){
    StringBuilder sb = new StringBuilder("{")
      .append("componentName=").append(componentName)
      .append(", serviceName=").append(serviceName)
      .append(", desiredState=").append(desiredState)
      .append("}");
    return sb.toString();
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  @Override
  public boolean equals(Object o){
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final RecoveryConfigComponent that = (RecoveryConfigComponent) o;
    return Objects.equals(componentName, that.componentName) &&
      Objects.equals(serviceName, that.serviceName) &&
      Objects.equals(desiredState, that.desiredState);
  }

  @Override
  public int hashCode(){
    return Objects.hash(componentName, serviceName, desiredState);
  }
}
