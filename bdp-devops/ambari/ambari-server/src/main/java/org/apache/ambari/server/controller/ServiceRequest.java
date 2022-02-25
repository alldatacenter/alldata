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

import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;

import io.swagger.annotations.ApiModelProperty;

public class ServiceRequest {

  private String clusterName; // REF
  private String serviceName; // GET/CREATE/UPDATE/DELETE
  private String desiredState; // CREATE/UPDATE
  private String maintenanceState; // UPDATE
  private String credentialStoreEnabled; // CREATE/UPDATE/GET
  private String credentialStoreSupported; //GET

  private Long desiredRepositoryVersionId;
  /**
   * Short-lived object that gets set while validating a request
   */
  private RepositoryVersionEntity resolvedRepository;

  public ServiceRequest(String clusterName, String serviceName,
      Long desiredRepositoryVersionId, String desiredState) {
    this(clusterName, serviceName, desiredRepositoryVersionId, desiredState, null);
  }

  public ServiceRequest(String clusterName, String serviceName,
      Long desiredRepositoryVersionId, String desiredState, String credentialStoreEnabled) {
    this.clusterName = clusterName;
    this.serviceName = serviceName;
    this.desiredState = desiredState;

    this.desiredRepositoryVersionId = desiredRepositoryVersionId;

    this.credentialStoreEnabled = credentialStoreEnabled;
    // Credential store supported cannot be changed after
    // creation since it comes from the stack definition.
    // We can update credential store enabled alone.
  }

  /**
   * @return the serviceName
   */
  @ApiModelProperty(name = "service_name")
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
   * @return the desiredState
   */
  @ApiModelProperty(name = "state")
  public String getDesiredState() {
    return desiredState;
  }

  /**
   * @param desiredState the desiredState to set
   */
  public void setDesiredState(String desiredState) {
    this.desiredState = desiredState;
  }

  public Long getDesiredRepositoryVersionId() {
    return desiredRepositoryVersionId;
  }

  /**
   * @return the clusterName
   */
  @ApiModelProperty(name = "cluster_name")
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
   * @param state the new maintenance state
   */
  public void setMaintenanceState(String state) {
    maintenanceState = state;
  }

  /**
   * @return the maintenance state
   */
  @ApiModelProperty(name = "maintenance_state")
  public String getMaintenanceState() {
    return maintenanceState;
  }

  /**
   * @return credential store enabled
   */
  @ApiModelProperty(name = "credential_store_enabled")
  public String getCredentialStoreEnabled() {
    return credentialStoreEnabled;
  }


  /**
   * @return credential store supported
   */
  public String getCredentialStoreSupported() {
    return credentialStoreSupported;
  }

  /**
   * @param credentialStoreEnabled the new credential store enabled
   */
  public void setCredentialStoreEnabled(String credentialStoreEnabled) {
    this.credentialStoreEnabled = credentialStoreEnabled;
  }

  /**
   * @param credentialStoreSupported the new credential store supported
   */
  @ApiModelProperty(name = "credential_store_supporteds")
  public void setCredentialStoreSupported(String credentialStoreSupported) {
    this.credentialStoreSupported = credentialStoreSupported;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("clusterName=").append(clusterName)
      .append(", serviceName=").append(serviceName)
      .append(", desiredState=").append(desiredState)
      .append(", credentialStoreEnabled=").append(credentialStoreEnabled)
      .append(", credentialStoreSupported=").append(credentialStoreSupported);
    return sb.toString();
  }

  /**
   * @param repositoryVersion
   */
  public void setResolvedRepository(RepositoryVersionEntity repositoryVersion) {
    resolvedRepository = repositoryVersion;
  }

  public RepositoryVersionEntity getResolvedRepository() {
    return resolvedRepository;
  }
}
