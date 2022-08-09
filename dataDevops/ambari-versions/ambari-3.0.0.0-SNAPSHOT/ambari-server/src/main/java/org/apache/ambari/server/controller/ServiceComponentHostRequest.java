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



public class ServiceComponentHostRequest {

  private String clusterName; // REF
  private String serviceName;
  private String componentName;
  private String hostname;
  private String publicHostname;
  private String state;
  private String desiredState; // CREATE/UPDATE
  private String desiredStackId; // UPDATE
  private String staleConfig; // GET - predicate
  private String adminState; // GET - predicate
  private String maintenanceState; // UPDATE
  
  public ServiceComponentHostRequest(String clusterName,
                                     String serviceName,
                                     String componentName,
                                     String hostname,
                                     String desiredState) {
    super();
    this.clusterName = clusterName;
    this.serviceName = serviceName;
    this.componentName = componentName;
    this.hostname = hostname;
    this.desiredState = desiredState;
  }

  /**
   * @return the serviceName
   */
  public String getServiceName() {
    return serviceName;
  }

  /**
   * @param serviceName the serviceName to set
   */
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  /**
   * @return the componentName
   */
  public String getComponentName() {
    return componentName;
  }

  /**
   * @param componentName the componentName to set
   */
  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  /**
   * @return the hostname
   */
  public String getHostname() {
    return hostname;
  }

  /**
   * @param hostname the hostname to set
   */
  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  /**
   * @return the desiredState
   */
  public String getDesiredState() {
    return desiredState;
  }

  /**
   * @param desiredState the desiredState to set
   */
  public void setDesiredState(String desiredState) {
    this.desiredState = desiredState;
  }

  /**
   * @return the state
   */
  public String getState() {
    return state;
  }

  /**
   * @param state the State to set
   */
  public void setState(String state) {
    this.state = state;
  }

  /**
   * @return the clusterName
   */
  public String getClusterName() {
    return clusterName;
  }

  /**
   * @param clusterName the clusterName to set
   */
  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  /**
   * @param staleConfig whether the config is stale
   */
  public void setStaleConfig(String staleConfig) {
    this.staleConfig = staleConfig;
  }

  /**
   * @return Stale config indicator
   */
  public String getStaleConfig() {
    return staleConfig;
  }

  /**
   * @param adminState the adminState to use as predicate
   */
  public void setAdminState(String adminState) {
    this.adminState = adminState;
  }

  /**
   * @return the admin state of the component
   */
  public String getAdminState() {
    return adminState;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{" + " clusterName=").append(clusterName)
      .append(", serviceName=").append(serviceName)
      .append(", componentName=").append(componentName)
      .append(", hostname=").append(hostname)
      .append(", publicHostname=").append(publicHostname)
      .append(", desiredState=").append(desiredState)
      .append(", state=").append(state)
      .append(", desiredStackId=").append(desiredStackId)
      .append(", staleConfig=").append(staleConfig)
      .append(", adminState=").append(adminState)
      .append(", maintenanceState=").append(maintenanceState)
      .append("}");
    return sb.toString();
  }

  /**
   * @param state the maintenance state
   */
  public void setMaintenanceState(String state) {
    maintenanceState = state;
  }
  
  /**
   * @return the maintenance state
   */
  public String getMaintenanceState() {
    return maintenanceState;
  }

  public String getPublicHostname() {
    return publicHostname;
  }

  public void setPublicHostname(String publicHostname) {
    this.publicHostname = publicHostname;
  }
}
