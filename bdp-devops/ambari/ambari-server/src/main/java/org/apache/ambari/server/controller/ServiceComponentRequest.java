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


public class ServiceComponentRequest {

  private String clusterName; // REF

  private String serviceName; // GET/CREATE/UPDATE/DELETE

  private String componentName; // GET/CREATE/UPDATE/DELETE

  private String desiredState; // CREATE/UPDATE

  private String componentCategory;

  private String recoveryEnabled; // CREATE/UPDATE

  public ServiceComponentRequest(String clusterName, String serviceName,
                                 String componentName, String desiredState) {
    this(clusterName, serviceName, componentName, desiredState, null, null);
  }

  public ServiceComponentRequest(String clusterName, String serviceName,
                                 String componentName, String desiredState,
                                 String recoveryEnabled) {
    this(clusterName, serviceName, componentName, desiredState, recoveryEnabled, null);
  }

  public ServiceComponentRequest(String clusterName,
                                 String serviceName, String componentName,
                                 String desiredState, String recoveryEnabled,
                                 String componentCategory) {
    this.clusterName = clusterName;
    this.serviceName = serviceName;
    this.componentName = componentName;
    this.desiredState = desiredState;
    this.recoveryEnabled = recoveryEnabled;
    this.componentCategory = componentCategory;
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
   * @return recoveryEnabled
   */
  public String getRecoveryEnabled() {
    return recoveryEnabled;
  }

  /**
   * @param recoveryEnabled the recoveryEnabled value to set.
   */
  public void setRecoveryEnabled(String recoveryEnabled) {
    this.recoveryEnabled = recoveryEnabled;
  }

  public String getComponentCategory() {
    return componentCategory;
  }

  public void setComponentCategory(String componentCategory) {
    this.componentCategory = componentCategory;
  }

  @Override
  public String toString() {
    return String.format("[clusterName=%s, serviceName=%s, componentName=%s, desiredState=%s, recoveryEnabled=%s, componentCategory=%s]",
        clusterName, serviceName, clusterName, desiredState, recoveryEnabled, componentCategory);
  }
}
